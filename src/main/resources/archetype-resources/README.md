# ${artifactId}

Generated Spark + Delta template that can be built against multiple runtimes.

## Build / test

This template uses a `runtime` property to select the target runtime.

- Set the default at generation time with `-Druntime=...`.
- Override at build time with `mvn -Druntime=...`.

- `fabric13` (Spark 3.5.5 / Delta 3.2.0 / Scala 2.12.18 / Java 11)
- `fabric20` (Spark 4.0.0 / Delta 4.0.0 / Scala 2.13.16 / Java 21)
- `databricks180` (Spark 4.0.0 / Delta 4.0.1 / Scala 2.13.16 / Java 21)
- `synapse34` (Spark 3.4.1 / Delta 2.4.0 / Scala 2.12.17 / Java 11)

Examples:

```bash
mvn -B -Druntime=fabric13 test
mvn -B -Druntime=synapse34 test
```

## Maven settings (optional)

This project includes an optional `.mvn/settings.xml` that references Maven Central, Sonatype public, and an (optional) GitHub Packages repository.

- Use it explicitly: `mvn -s .mvn/settings.xml test`
- GitHub Packages requires credentials via environment variables (example):
  - `GITHUB_ACTOR`
  - `GITHUB_TOKEN` (token needs `read:packages` for local use)

Do not commit credentials into the project.

## Source sets

This template generates all runtime-specific source roots and uses Maven profiles to compile only the selected runtime.

- Common sources: `src/main/scala`
- Runtime sources: `src/main/scala-fabric13`, `src/main/scala-fabric20`, `src/main/scala-databricks180`, `src/main/scala-synapse34`

The common code depends on a runtime-provided `SparkRuntime` object.
