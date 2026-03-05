# Plan: Multi-Runtime Spark Maven Archetype

You’ll create a new, standalone Maven archetype repo under `~/Developer` that generates a Spark application template compatible with four runtimes (Fabric 1.3, Fabric 2.0, Databricks Runtime 18.0, Synapse Spark 3.4).

Because Maven archetype metadata can’t truly “conditionally include” entire source trees based on a property, the most robust design is:

- Generate all runtime-specific source sets
- Use Maven profiles (driven by `-Druntime=...`) to compile/package only the selected runtime

This still satisfies the parameterization requirement, keeps the generated project deterministic, and makes CI/runtime-matrix testing straightforward.

## Steps

1. Extract and normalize runtime version baselines into a single mapping table.

    - Fabric 1.3: derive dependency-friendly versions from the “Target Runtime” table in [README.md](../../README.md) (use upstream versions like Spark `3.5.5`, Delta `3.2.0`, Scala `2.12.18`, Java `11` rather than Fabric build strings).
    - Fabric 2.0: use the confirmed baseline (Spark `4.0`, Delta `4.0`, Scala `2.13`, Java `21`); include a quick dependency-resolution check to confirm the exact Scala binary/patch that Spark 4.0 artifacts are published against.
    - Databricks: pin to Databricks Runtime 18.0. For Maven-based local compilation/tests, prefer a Spark/Delta pairing that is both (a) available on Maven Central and (b) binary-compatible (e.g., Spark `4.0.0` + Delta `4.0.1` + Scala `2.13.16` + Java `21`).
    - Synapse: source Spark/Delta/Scala/Java for Synapse Spark 3.4 from Microsoft’s Synapse Spark runtime release source (canonical).
    - Preflight check (do this early): confirm the Maven coordinates for Spark/Delta resolve for each runtime, especially Synapse where Delta artifacts differ.

2. Create the new archetype repo (standalone) with “builds green locally” guardrails.
   - Initialize git immediately; add a `.gitignore` that ignores `target/` and other build artifacts.
   - Set `packaging` to `maven-archetype` and add the required archetype packaging build extension early (so Maven recognizes the packaging).
   - Use Central-friendly coordinates (`groupId` like `io.github.<you>`), and adopt SemVer tags `vX.Y.Z`.
   - Keep “create/push GitHub repo” as optional until the local `mvn clean verify` is green.
3. Design the generated project template to be deterministic by default.
   - Mirror the build/test conventions from [pom.xml](../../pom.xml): `scala-maven-plugin` for compile, `scalatest-maven-plugin` for tests (this repo intentionally skips surefire).
   - Keep Spark/Delta/Scala dependencies `provided` (available for compilation/tests, but not packaged).
   - Default runtime behavior (confirmed): generated project builds without `-Druntime=...` by activating a single runtime profile via `activeByDefault`.
4. Implement runtime switching via Maven profiles + source sets (generate all sources; compile one).
   - Generated project includes a shared source folder plus four runtime folders (e.g., `fabric13`, `fabric20`, `databricks180`, `synapse34`).
   - Define allowed runtime values (exact strings): `fabric13`, `fabric20`, `databricks180`, `synapse34`.
   - Maven profiles `rt-fabric13`, `rt-fabric20`, `rt-databricks180`, `rt-synapse34`:
     - Set `spark.version`, `delta.version`, `scala.version`, and Java target via `maven.compiler.release` based on the mapping table.
     - Add sources/resources for the selected runtime only (via `build-helper-maven-plugin`).
       - Add main sources in `generate-sources` and test sources in `generate-test-sources`.
     - Enforce a minimum required JDK for the selected runtime (via `maven-enforcer-plugin`).
     - Pin plugin versions explicitly (avoid “works on my Maven” surprises).
   - Default vs override behavior:
     - `rt-fabric13` is `activeByDefault` so `mvn test` works without any flags.
     - Other runtime profiles activate by property (e.g., `runtime=fabric20`). In Maven, an `activeByDefault` profile is generally not active when another profile becomes active; still add validation (below) so typos don’t silently build the default.
     - Usage: `mvn -Druntime=fabric20 test` (same pattern for the other runtime values).
   - Runtime property validation:
     - Add a small validation profile activated when `runtime` is set, and fail fast if the value is not one of the allowed options.
     - This prevents typos like `-Druntime=fabirc13` silently falling back to the default runtime.

5. Handle Delta artifact differences explicitly.
   - Use per-runtime properties rather than hard-coding one Delta coordinate for all runtimes:
     - `delta.groupId` (usually `io.delta`)
     - `delta.artifactId` (runtime-specific)
     - `delta.version`
     - `scala.binary.version` (derived from `scala.version`, e.g., `2.12` or `2.13`)
   - Fabric/Databricks (typical): use `delta-spark_${scala.binary.version}` when that coordinate exists for the runtime’s Delta version.
   - Synapse guardrail (learned the hard way): if the expected `delta-spark_${scala.binary.version}` coordinate for the Synapse Delta version is not available from Maven Central, set `delta.artifactId=delta-core_${scala.binary.version}` in the Synapse runtime profile.
   - Dependency/scope rules:
     - Keep Spark/Delta/Scala as `provided` (compile and tests work; artifacts don’t end up packaged).
     - Avoid duplicating the same dependency multiple times across scopes (it can create “duplicate dependency” warnings and confusing compile classpaths).
     - Prefer a single declaration per dependency, and override only the properties inside runtime profiles.

6. Configure archetype metadata to include all runtime trees cleanly.
   - Template layout:
     - Keep runtime source trees in distinct folders under the template (e.g., `src/main/scala`, `src/main/scala-fabric13`, `src/main/scala-fabric20`, etc.).
     - Do not place all runtime sources under the same Maven default source root in the template; rely on Step 4’s `build-helper-maven-plugin` to select one runtime at build time.
   - In `archetype-metadata.xml`:
     - Include fileSets for common + all runtime source trees, and mark them as `filtered=true` so standard Maven properties are replaced.
     - Ensure the directory structure is preserved exactly (avoid includes/excludes that would flatten runtime folders).
     - Include `pom.xml` in a filtered fileset so coordinates/artifactId/name/description substitute correctly.
   - Filtering guardrails:
     - Treat non-text/binary resources as `filtered=false` so they are copied byte-for-byte.
     - If the template contains literal `${...}` sequences that should remain (rare, but possible in docs/examples), exclude them from filtering or escape them.
   - Sanity check:
     - Generate a project and confirm all four runtime folders exist on disk and no files are missing/merged.

7. Add integration tests that generate and build for each runtime.
   - Use Maven Invoker-based integration tests (`src/it/**`) to prove the archetype works end-to-end.
   - Contributor details (test structure, `postbuild.shared.groovy` + per-runtime wrapper `postbuild.groovy`, `mvn-settings.xml`, and adding new runtime cases): see `CONTRIBUTING.md`.
   - CI coverage:
     - Include Fabric 1.3 on JDK 11 and Fabric 2.0 on JDK 21.
     - Add Databricks and Synapse once their canonical JDK baselines are confirmed.

8. CI and publishing workflows.
   - Keep “CI” and “publish” as separate workflows.
   - CI workflow (always-on):
     - Trigger: PRs + pushes to main.
     - Runs: `mvn -B -U clean verify`.
     - Includes: runtime/JDK matrix that runs the Invoker ITs.
     - Constraint: never deploy from CI (no publishing side effects).
   - Publish workflow (manual):
     - Trigger: `workflow_dispatch` only.
     - Inputs:
       - `dryRun` (boolean) to validate the pipeline without pushing artifacts.
       - Optional version override (if you choose to support it), otherwise rely on tags.
     - Behavior:
       - Always runs `mvn -B -U clean verify` first.
       - If `dryRun=true`, skip all deploy steps.
       - If `dryRun=false`, deploy to the chosen package repository.
     - Auth/permissions:
       - GitHub Packages publish uses `GITHUB_TOKEN` with `packages: write`.
       - Maven Central publish (optional, later) requires Sonatype credentials + GPG material as secrets.

9. Documentation.
   - README includes:
     - Quickstart: one command to generate, one command to run tests.
     - Default runtime: clearly state which runtime is `activeByDefault` and why.
     - Per-runtime snippets:
       - Build/test: `mvn -Druntime=<runtime> test`.
       - Package (if supported): `mvn -Druntime=<runtime> package`.
     - Runtime table: runtime → Spark/Delta/Scala/Java baseline.
     - Troubleshooting (minimal): common failure modes like “wrong JDK for runtime” and how to fix.

## Verification

- Local: generate a project and run `mvn -Druntime=<runtime> test` for any runtimes supported by your installed JDKs (often the Java 11 runtimes).
- CI: rely on GitHub Actions to validate Java 21 runtimes (Fabric 2.0 + Databricks 18.0).
- CI: GitHub Actions matrix verifies all 4 runtimes with correct JDKs and confirms the archetype IT suite passes.
- Release dry-run: run the publish workflow with `dryRun=true` to validate packaging/signing/staging steps before the first real publish.

## Decisions

- Runtime baselines: Fabric 2.0 = Spark 4.0 / Delta 4.0 / Scala 2.13 / Java 21; Databricks 18.0 = Spark 4.0.0 / Delta 4.0.1 / Scala 2.13.16 / Java 21; Synapse = Spark 3.4 baseline.
- Coordinates: recommend `io.github.yaravind` as `groupId`.
- Runtime selection: archetype will generate all runtime source sets and profile-select at build time (reliable and CI-friendly).
- Default runtime: one runtime profile is `activeByDefault` so the generated project builds without `-Druntime=...`.
- Runtime override: `-Druntime=...` selects a different runtime; invalid values fail fast.
- Java targeting: generated project pins bytecode/API level per runtime via `maven.compiler.release`.
