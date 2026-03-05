# Prompt: Add a New Runtime (End-to-End)

You are working in the `spark-initializr` repository (a Maven archetype that generates Spark + Delta Scala projects). Your task is to add a **new runtime** end-to-end so contributors/users can build a generated app for that runtime and the repo’s integration tests validate it.

## What “adding a runtime” means in this repo

A runtime corresponds to:

- A Maven profile in the **generated project template** POM (`src/main/resources/archetype-resources/pom.xml`) that sets Spark/Delta/Scala/JDK baselines and selects the runtime-specific source tree.
- A runtime-specific source directory in the template (generated into the app).
- A Maven Invoker integration test case under `src/it/<runtimeId>/` that:
  - generates a sample project from the local archetype artifact
  - builds/tests it with `-Druntime=<runtimeId>`

## Inputs (ask these questions first)

Ask the user for the following before editing files:

1. **Runtime id** (string): the value used with `-Druntime=...` (examples: `fabric13`, `fabric20`, `databricks180`, `synapse34`).
2. **Spark baseline**: `spark.version` and the **Scala binary** it’s built for (`2.12` or `2.13`).
3. **Delta baseline**:
   - `delta.version`
   - `delta.artifactId` to use (typically `delta-spark_${scala.binary.version}`, sometimes `delta-core_${scala.binary.version}`).
4. **Scala patch version** (full): `scala.version` (example: `2.13.16`).
5. **Java baseline**:
   - `maven.compiler.release` (major, e.g. `11` or `21`)
   - `required.java.version` range to enforce a minimum (examples used here: `[11,)`, `[21,)`).
6. Whether this new runtime should be the **default** (i.e., `activeByDefault=true`) or not.

If the user doesn’t know exact versions, propose a sensible default and ask for confirmation.

## Constraints

- Keep changes minimal and consistent with existing patterns.
- Do not introduce new build systems, new modules, or extra UX.
- Do not change existing runtimes unless necessary.

## Implementation steps (perform in order)

### 1) Add runtime profile to the template POM

Edit `src/main/resources/archetype-resources/pom.xml`:

- Add a new `<profile>` under `<profiles>`:
  - `<id>` should be `rt-<runtimeId>`.
  - Activation:
    - If default runtime: `<activeByDefault>true</activeByDefault>`.
    - Otherwise: activate via property:
      - `<name>runtime</name>` / `<value><runtimeId></value>`.
  - Under `<properties>`, set:
    - `spark.version`
    - `delta.version`
    - `delta.artifactId`
    - `scala.binary.version`
    - `scala.version`
    - `maven.compiler.release`
    - `required.java.version`
  - Under `<build>`, add a `build-helper-maven-plugin` execution like existing runtimes to include runtime sources:
    - source path: `src/main/scala-<runtimeId>`

Notes:
- Keep `maven-compiler-plugin` using `<release>${maven.compiler.release}</release>` (already present).
- Keep the Maven Enforcer rule using `${required.java.version}`.

### 2) Add runtime-specific sources in the archetype template

Under `src/main/resources/archetype-resources/src/main/`:

- Create: `scala-<runtimeId>/`.
- Add the minimal runtime-specific entry point(s) mirroring existing runtimes.
  - Prefer copying an existing runtime source directory that is closest (same Scala binary / same major Spark generation).

Do not duplicate code already in `scala/`.

### 3) Add Maven Invoker integration test case

Create a new folder: `src/it/<runtimeId>/`.

Copy from an existing runtime folder (pick one with the closest required Java), then adjust:

- `runtime.properties`:
  - `runtime=<runtimeId>`
  - `requiredJava=<maven.compiler.release>`
- `invoker.properties`:
  - keep `invoker.goals = validate`
- `postbuild.groovy`:
  - keep it as a thin wrapper that evaluates the shared harness: `src/it/postbuild.shared.groovy`.
  - do not duplicate the full install/generate/test logic per runtime.
  - keep using `src/it/mvn-settings.xml`.

### 4) Update top-level docs

Edit `README.md`:

- Add the runtime to the runtime table (Spark/Delta/Scala/JDK).
- Add `<runtimeId>` to the “Supported values for runtime” list.
- If you added a new default runtime, ensure the README’s examples still make sense.

### 5) Validate locally

Run:

```bash
mvn -B -U clean verify
```

If the current JDK is too low for the runtime, the IT should self-skip (this behavior is implemented in `src/it/postbuild.shared.groovy`).

## Deliverables (must be present in the PR)

- Updated template profile in `src/main/resources/archetype-resources/pom.xml`.
- New template runtime sources under `src/main/resources/archetype-resources/src/main/scala-<runtimeId>/`.
- New IT folder `src/it/<runtimeId>/` with `invoker.properties`, `postbuild.groovy`, `runtime.properties`.
- Updated `README.md`.

## Non-goals

- Do not add CI workflows or publishing changes.
- Do not add new build plugins unless required for the runtime.
