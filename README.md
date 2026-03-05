# spark-initializr

Website: <https://www.initializr.app/>

Maven archetype that generates a minimal Spark + Delta app template compatible with:

| Runtime | Spark Version | Delta Version | Scala Version | JDK Version |
| --- | ---: | ---: | ---: | ---: |
| Microsoft Fabric Runtime 1.3 | 3.5.x | 3.2.x | 2.12 | 11 |
| Microsoft Fabric Runtime 2.0 | 4.0.x | 4.0.x | 2.13 | 21 |
| Databricks Runtime 18.0 | 4.0.0 | 4.0.1 | 2.13 | 21 |
| Azure Synapse Spark 3.4 | 3.4.1 | 2.4.0 | 2.12 | 11 |

## Release

See [RELEASE.md](RELEASE.md) for the official release workflow order and flow diagram.

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md) for development notes, archetype integration tests (`src/it/**`), and how to add a new runtime.

## Quick start

### Option 1 (Preferred): Generate from Maven Central

The archetype is published to Maven Central:

- <https://central.sonatype.com/artifact/io.github.yaravind/spark-initializr/overview>

Generate a project:

```bash
mvn -B archetype:generate \
  -DarchetypeGroupId=io.github.yaravind \
  -DarchetypeArtifactId=spark-initializr \
  -DarchetypeVersion=1.0.0 \
  -Druntime=fabric13 \
  -DgroupId=com.example \
  -DartifactId=my-spark-app \
  -Dversion=0.1.0-SNAPSHOT \
  -Dpackage=com.example.app
```

Build using the default runtime you selected during generation:

```bash
cd my-spark-app
mvn -B test
```

Override the runtime at build time (without regenerating):

```bash
mvn -B -Druntime=fabric20 test
```

### Option 2 (Advanced): Build and install locally

Build/install the archetype locally.

Use this option when:

- you are developing/modifying the archetype itself
- you need an unreleased change (not yet published to Maven Central)
- you are iterating quickly on templates and want to test immediately
- you are offline or Maven Central access is restricted

```bash
mvn -B -U clean verify
```

Generate a project:

```bash
mvn -B archetype:generate \
  -DarchetypeGroupId=io.github.yaravind \
  -DarchetypeArtifactId=spark-initializr \
  -DarchetypeVersion=0.1.0-SNAPSHOT \
  -DgroupId=com.example \
  -DartifactId=my-spark-app \
  -Dversion=0.1.0-SNAPSHOT \
  -Dpackage=com.example.app
```

Build using the default runtime configured in the generated project:

```bash
cd my-spark-app
mvn -B -Druntime=fabric13 test
```

Supported values for `runtime`: `fabric13`, `fabric20`, `databricks180`, `synapse34`.

## Step-by-step: usage by runtime

### 0) Prerequisites

- Maven 3.9+
- JDK per runtime:
  - `fabric13`: Java 11+
  - `fabric20`: Java 21+
  - `databricks180`: Java 21+
  - `synapse34`: Java 11+

### Option 1 (Preferred): Use the published archetype from Maven Central

From any folder, generate a new project:

```bash
mvn -B archetype:generate \
  -DarchetypeGroupId=io.github.yaravind \
  -DarchetypeArtifactId=spark-initializr \
  -DarchetypeVersion=0.1.0 \
  -Druntime=fabric13 \
  -DgroupId=com.example \
  -DartifactId=my-data-product \
  -Dversion=1.0.0-SNAPSHOT \
  -Dpackage=com.mydataproduct.example
```

Log

```console
Developer % mvn -B archetype:generate \
  -DarchetypeGroupId=io.github.yaravind \
  -DarchetypeArtifactId=spark-initializr \
  -DarchetypeVersion=0.1.0 \
  -Druntime=fabric13 \
  -DgroupId=com.example \
  -DartifactId=my-data-product \
  -Dversion=1.0.0-SNAPSHOT \
  -Dpackage=com.mydataproduct.example \
  -s /Users/TestUser/.m2/settings.xml

[INFO] Scanning for projects...
[INFO] 
[INFO] ------------------< org.apache.maven:standalone-pom >-------------------
[INFO] Building Maven Stub Project (No POM) 1
[INFO] --------------------------------[ pom ]---------------------------------
[INFO] 
[INFO] >>> archetype:3.4.1:generate (default-cli) > generate-sources @ standalone-pom >>>
[INFO] 
[INFO] <<< archetype:3.4.1:generate (default-cli) < generate-sources @ standalone-pom <<<
[INFO] 
[INFO] 
[INFO] --- archetype:3.4.1:generate (default-cli) @ standalone-pom ---
[INFO] Generating project in Batch mode
[INFO] Archetype repository not defined. Using the one from [io.github.yaravind:spark-initializr:0.1.0-SNAPSHOT] found in catalog local
[INFO] ----------------------------------------------------------------------------
[INFO] Using following parameters for creating project from Archetype: spark-initializr:0.1.0
[INFO] ----------------------------------------------------------------------------
[INFO] Parameter: groupId, Value: com.example
[INFO] Parameter: artifactId, Value: my-data-product
[INFO] Parameter: version, Value: 1.0.0-SNAPSHOT
[INFO] Parameter: package, Value: com.mydataproduct.example
[INFO] Parameter: packageInPathFormat, Value: com/mydataproduct/example
[INFO] Parameter: package, Value: com.mydataproduct.example
[INFO] Parameter: groupId, Value: com.example
[INFO] Parameter: runtime, Value: fabric13
[INFO] Parameter: artifactId, Value: my-data-product
[INFO] Parameter: version, Value: 1.0.0-SNAPSHOT
[WARNING] Don't override file /Users/TestUser/Developer/my-data-product/pom.xml
[INFO] Project created from Archetype in dir: /Users/TestUser/Developer/my-data-product
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  1.275 s
[INFO] Finished at: 2026-03-04T13:50:42-05:00
[INFO] ------------------------------------------------------------------------
Developer %
```

Then:

```bash
cd my-spark-app
```

### Option 2 (clone + install locally): Clone and install the archetype locally

Use this option when:

- you are developing/modifying the archetype itself
- you need an unreleased change (not yet published to Maven Central)
- you are iterating quickly on templates and want to test immediately
- you are offline or Maven Central access is restricted

Clone this repo and install it to your local Maven repository:

```bash
git clone https://github.com/yaravind/spark-initializr.git
cd spark-initializr
mvn -B -U clean install
```

Then, from any folder, generate a new project (using the locally installed version):

```bash
mvn -B archetype:generate \
  -DarchetypeGroupId=io.github.yaravind \
  -DarchetypeArtifactId=spark-initializr \
  -DarchetypeVersion=0.1.0-SNAPSHOT \
  -DgroupId=com.example \
  -DartifactId=my-spark-app \
  -Dversion=0.1.0-SNAPSHOT \
  -Dpackage=com.example.app \
  -s settings.xml
```

Then:

```bash
cd my-spark-app
```

### Build / test for a target runtime

The generated app contains:

- common sources: `src/main/scala`
- runtime sources: `src/main/scala-<runtime>`

Pick a runtime at build time with `-Druntime=...`.

#### Fabric Runtime 1.3 (`fabric13`)

```bash
mvn -B -Druntime=fabric13 test
mvn -B -Druntime=fabric13 package
```

#### Fabric Runtime 2.0 (`fabric20`)

Make sure you are using Java 21+.

```bash
mvn -B -Druntime=fabric20 test
mvn -B -Druntime=fabric20 package
```

#### Databricks Runtime 18.0 (`databricks180`)

Make sure you are using Java 21+.

```bash
mvn -B -Druntime=databricks180 test
mvn -B -Druntime=databricks180 package
```

#### Synapse Spark 3.4 (`synapse34`)

```bash
mvn -B -Druntime=synapse34 test
mvn -B -Druntime=synapse34 package
```

### Notes

- Spark/Delta dependencies in the generated app are `provided` (they’re supplied by the cluster runtime). The `*-all.jar` produced by the shade plugin is mainly useful for bundling your own dependencies, not Spark itself.

## License

Licensed under the Apache License, Version 2.0 (Apache-2.0).

Provided on an "AS IS" basis, without warranties or conditions of any kind, and the author(s) are not liable for any damages arising from its use.
