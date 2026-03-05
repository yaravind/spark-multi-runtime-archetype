# Contributing

Thanks for contributing!

This repo is a Maven **archetype**. The key contributor workflow is validating that the archetype generates a project that builds for each supported runtime.

## Introduction

### What `src/it/` is (and why it exists)

`src/it/` is a Maven ecosystem convention for **integration test projects** (“ITs”). In this repo, each `src/it/<case>/` directory is a self-contained scenario that validates the archetype end-to-end.

Purpose:

- Keep test harness files out of the generated template (nothing under `src/it/` should ship into generated apps).
- Validate the generated output as a real Maven project (dependencies resolve, profiles activate correctly, and the build runs).

> This is why `src/it/` lives alongside the archetype project (not under `src/main/resources/archetype-resources/`).

How it’s used here:

| Step # | Step details | Methods in `postbuild.shared.groovy` |
| ---: | --- | --- |
| 1 | Maven Invoker discovers `src/it/**` and runs during the `verify` phase (configured in the root `pom.xml`). | N/A (this is `pom.xml` / Invoker configuration, not `postbuild.shared.groovy`) |
| 2 | Read `runtime.properties` to get `runtime=<id>` and `requiredJava=<major>`. | Inline script logic (loads `Properties`); no helper method |
| 3 | Skip the IT early if the current JDK is below `requiredJava`. | `parseJavaSpecVersion(...)` |
| 4 | Install the just-built archetype artifact into a test-local Maven repo (isolated via `-Dmaven.repo.local=...` and `src/it/mvn-settings.xml`). | `runCmd(...)` (runs `maven-install-plugin:install-file`) |
| 5 | Generate a sample project via `maven-archetype-plugin:generate` into a `work/` directory (e.g., `work/it-app-<id>/`). | `deleteRecursively(...)` (cleanup), `runCmd(...)` (runs `maven-archetype-plugin:generate`) |
| 6 | Run `mvn -Druntime=<id> test` in the generated project directory; this activates the matching runtime profile in the generated project `pom.xml`. | `runCmd(...)` |

#### How `src/it` tests work (Maven Invoker)

Each runtime folder (example: `src/it/fabric13/`) typically contains:

- `invoker.properties`
  - Usually sets `invoker.goals = validate`.
  - The real work happens in the post-build hook.
- `postbuild.groovy`
  - Thin wrapper that delegates to the shared implementation in `src/it/postbuild.shared.groovy`.
  - This is intentionally small to keep all IT behavior consistent across runtimes.
- `runtime.properties`
  - Defines `runtime=<id>` and `requiredJava=<major>`.
- `mvn-settings.xml` (shared at `src/it/mvn-settings.xml`)
  - A **Central-only** Maven settings file used by ITs.
  - Prevents user-level `~/.m2/settings.xml` mirrors/proxies from breaking ITs (common in corporate environments).

Shared harness file:

- `src/it/postbuild.shared.groovy`
  - Implements the common Invoker post-build flow (JDK gate, install archetype to a test-local repo, generate a sample app, run `mvn -Druntime=... test`).

### Maven Invoker Plugin

This repository uses the **Maven Invoker Plugin** to run end-to-end integration tests for the archetype.

Why Invoker?

- An archetype can “build successfully” while still generating a broken project (missing files, wrong filtering, bad dependency coordinates, incorrect profile activation, etc.).
- The Invoker plugin runs a set of real Maven builds from `src/it/**`, which lets us validate the full workflow: build the archetype → generate a sample project → run `mvn -Druntime=... test` in the generated project.

In short: Invoker is the guardrail that ensures changes to templates and runtime profiles keep producing a working project.

## Repository layout (what’s what)

- `src/main/resources/archetype-resources/`
  - The **template** copied into the generated project.
  - If a file is placed here, it ends up in every generated project.
- `src/it/`
  - **Integration tests** for the archetype using the Maven Invoker Plugin.
  - Each subdirectory is an end-to-end scenario (generate a project, then run Maven against it).

## Running the build + integration tests

Run:

```bash
mvn -B -U clean verify
```

Notes:

- `-B` (batch mode): non-interactive output suitable for CI/logs (no prompts).
- `verify` runs the Maven Invoker Plugin configured in the root `pom.xml`.
- Each `src/it/<case>` test is cloned to `target/it/` during execution.

> In this repository specifically, verify is important because the Maven Invoker Plugin is configured to run at verify, so this command will also execute the src/it/** integration tests (generate a project from the archetype and run mvn -Druntime=... test inside it).

## Adding a new runtime

A “runtime” here means: a set of Spark/Delta/Scala/JDK baselines + a Maven profile that selects the correct dependencies and sources.

Checklist:

1. Add a runtime profile to the generated project template POM

   - Edit `src/main/resources/archetype-resources/pom.xml`.
   - Add a new `<profile>` with:
     - activation via `-Druntime=<yourRuntimeId>`
     - properties: `spark.version`, `delta.version`, `delta.artifactId`, `scala.binary.version`, `scala.version`, `maven.compiler.release`, `required.java.version`
     - a `build-helper-maven-plugin` execution that adds `src/main/scala-<yourRuntimeId>` as a source root.

1. Add runtime-specific sources

    - Add `src/main/resources/archetype-resources/src/main/scala-<yourRuntimeId>/...`.
    - Keep shared code in `src/main/resources/archetype-resources/src/main/scala/`.

1. Add an integration test case under `src/it/`

   - Create a folder: `src/it/<yourRuntimeId>/`.
   - Copy an existing runtime’s `postbuild.groovy` wrapper (or create a similar wrapper that evaluates `src/it/postbuild.shared.groovy`).
   - Update `src/it/<yourRuntimeId>/runtime.properties`:
     - `runtime=<yourRuntimeId>`
     - `requiredJava=<major>`
   - Keep `invoker.properties` with `invoker.goals = validate`.

1. Document the runtime

    - Update the runtime table and supported `runtime` values in `README.md`.

That’s it — `mvn -B -U clean verify` should now exercise your new runtime end-to-end.
