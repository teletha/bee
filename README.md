<p align="center">
    <a href="https://docs.oracle.com/en/java/javase/24/"><img src="https://img.shields.io/badge/Java-Release%2024-green"/></a>
    <span>&nbsp;</span>
    <a href="https://jitpack.io/#teletha/bee"><img src="https://img.shields.io/jitpack/v/github/teletha/bee?label=Repository&color=green"></a>
    <span>&nbsp;</span>
    <a href="https://teletha.github.io/bee"><img src="https://img.shields.io/website.svg?down_color=red&down_message=CLOSE&label=Official%20Site&up_color=green&up_message=OPEN&url=https%3A%2F%2Fteletha.github.io%2Fbee"></a>
</p>

## Summary
Bee is a modern, open-source build automation tool focused on conventions, type safety, and performance.

Tired of complex configurations and slow build times? Bee leverages the power and familiarity of Java to define projects and build tasks, offering a flexible and highly extensible experience for developers.

## ✨ Features
#### ☕ Java-Defined Tasks
Write your build logic directly in Java. Enjoy type safety, easy refactoring, powerful IDE support (completion, navigation), and seamless integration with your existing Java codebase. Forget complex DSLs or XML – stick with the language you know best.

#### 🧩 Flexible Task System
Easily extend Bee with your own custom tasks or modify existing ones. Simply create an interface extending `bee.Task` within your project folder to add new tasks, defining commands with annotations – Bee discovers them automatically without complex descriptors. Furthermore, you can easily customize the behavior of built-in tasks by implementing their corresponding task interface in your project and overriding the command methods you wish to change. Bee prioritizes your custom implementations, giving you full control over the build process.

#### ✅ Convention over Configuration
Sensible defaults minimize the need for boilerplate configuration. Focus on your code, not the build tool setup. All settings are type-safe with IDE completion, eliminating guesswork and trips to the documentation for minor details.

#### 🚀 Fast Execution
Achieve significantly faster build times. Bee executes tasks in parallel whenever possible and utilizes intelligent caching to reuse outputs from previous runs, avoiding redundant work. Only build what's necessary, when it's necessary.

#### 🔗 Integrated Lifecycle
Bee understands source code and package repositories (like Maven/Gradle repositories). It automates the entire development lifecycle, from compiling and testing locally to packaging, publishing, and releasing your artifacts with straightforward commands.


## 📦 Installation
Get started with Bee quickly using our installation scripts. Open your terminal and run the appropriate command:

**Linux / macOS**
```bash
curl -Ls https://git.io/install-bee | bash
```

**Windows (Command Prompt or PowerShell)**
```cmd
curl -Ls https://git.io/install-bee -o install.bat && install
```

After installation is complete, verify that the tool was installed successfully by running:
```
bee -v
```
This should display the installed Bee version.


## 🚀 Available Tasks
Here is a list of the built-in tasks available in Bee. You can get more detailed help for each task, including its specific commands and configuration options, by running `bee [TaskName]:help` (e.g., `bee compile:help`).

### BUN
Installs and manages the Bun runtime.

| Command   | Description                 | Default |
| :-------- | :-------------------------- | :------: |
| `dev`     | Launch development server.  |         |
| `install` | Install bun.                |    ✅    |

### CI
Sets up Continuous Integration configurations.

| Command     | Description                                         | Default |
| :---------- | :-------------------------------------------------- | :------: |
| `github`    | Generate CI/CD configuration files for GitHub.    |         |
| `gitignore` | Generate `.gitignore` file.                         |         |
| `jitpack`   | Generate CI/CD configuration files for JitPack.   |         |
| `license`   | Generate license file.                            |         |
| `readme`    | Generate readme file.                             |         |
| `setup`     | Setup general CI/CD configurations.               |    ✅    |

### CLEAN
Cleans build output files.

| Command | Description             | Default |
| :------ | :---------------------- | :------: |
| `all`   | Clean output directory. |    ✅    |

### COMPILE
Compiles source code.

| Command   | Description                                        | Default |
| :-------- | :------------------------------------------------- | :------: |
| `check`   | Validate main and test sources for compilation errors. |         |
| `project` | Compile project definition sources and resources.  |         |
| `source`  | Compile main sources and resources.                |    ✅    |
| `test`    | Compile test sources and resources.                |         |

Configuration

| Option   | Description                                              | Type      | Default |
| :------- | :------------------------------------------------------- | :-------- | :------ |
| `useECJ` | Force use of the Eclipse Compiler for Java (ECJ).        | `boolean` | `false` |

### DEPENDENCY
Manages project dependencies.

| Command  | Description                                         | Default |
| :------- | :-------------------------------------------------- | :------: |
| `module` | Analyze and display required Java modules using jdeps. |         |
| `tree`   | Display the project dependency tree.                |    ✅    |

### DOC
Generates project documentation.

| Command   | Description                           | Default |
| :-------- | :------------------------------------ | :------: |
| `javadoc` | Generate project Javadoc.             |    ✅    |
| `site`    | Generate project site (including docs). |         |

### ECLIPSE
Manages Eclipse IDE project files.

| Command      | Description                                                              | Default |
| :----------- | :----------------------------------------------------------------------- | :------: |
| `create`     | Generate configuration files for Eclipse.                                |    ✅    |
| `delete`     | Delete configuration files for Eclipse.                                  |         |
| `live`       | Rewrite sibling Eclipse projects to use the current project directly.    |         |
| `repository` | Rewrite sibling Eclipse projects to use the current project in repository. |         |

### EXE
Creates a Windows executable launcher.

| Command | Description                                             | Default |
| :------ | :------------------------------------------------------ | :------: |
| `build` | Generate windows exe file which executes the main class. |    ✅    |

Configuration

| Option      | Description                        | Type             | Default    |
| :---------- | :--------------------------------- | :--------------- | :--------- |
| `icon`      | Location for the `.exe` file icon. | `Path`           | *(none)*   |
| `customJRE` | Embed a custom JRE.                | `boolean`        | `true`     |
| `resources` | Additional files/dirs to package.  | `Set<Location>`  | `[]`       |

### FIND-MAIN
Finds main and agent entry point classes.

| Command     | Description                                        | Default |
| :---------- | :------------------------------------------------- | :------: |
| `agentmain` | Find `agentmain` class for Java Agent (attach API). |         |
| `main`      | Find `main` class for the project.                 |    ✅    |
| `premain`   | Find `premain` class for Java Agent (startup).     |         |

Configuration

| Option      | Description                   | Type     | Default |
| :---------- | :---------------------------- | :------- | :------ |
| `main`      | Specify the main class FQCN.  | `String` | *(auto-detected)* |
| `premain`   | Specify the premain class FQCN. | `String` | *(none)* |
| `agentmain` | Specify the agentmain class FQCN.| `String` | *(none)* |

### HELP
Displays help information about Bee.

| Command   | Description                                         | Default |
| :-------- | :-------------------------------------------------- | :------: |
| `option`  | Display all available command-line options.         |         |
| `task`    | Display all available tasks (this list).            |    ✅    |
| `version` | Display version information for Bee, Java, and OS.  |         |
| `welcome` | Display the Bee welcome message.                    |         |

### IDE
Manages general IDE project files.

| Command  | Description                       | Default |
| :------- | :-------------------------------- | :------: |
| `create` | Generate configuration files.     |    ✅    |
| `delete` | Delete configuration files.       |         |

### INSTALL
Installs project artifacts into the local repository.

| Command   | Description                                    | Default |
| :-------- | :--------------------------------------------- | :------: |
| `jar`     | Install JAR file only into the local repository. |         |
| `project` | Install project into the local repository.     |    ✅    |

### INTELLIJ
Manages IntelliJ IDEA project files.

| Command  | Description                                | Default |
| :------- | :----------------------------------------- | :------: |
| `create` | Generate configuration files for IntelliJ IDEA. |    ✅    |
| `delete` | Delete configuration files for IntelliJ IDEA. |         |

### JAR
Packages project artifacts into JAR files.

| Command    | Description                                                     | Default |
| :--------- | :-------------------------------------------------------------- | :------: |
| `document` | Package generated Javadoc into a JAR file.                      |         |
| `merge`    | Create an executable JAR with all dependencies included (uber-jar). |         |
| `project`  | Package project definition classes and resources into a JAR file. |         |
| `source`   | Package main classes and resources into a JAR file.             |    ✅    |
| `test`     | Package test classes and resources into a JAR file.             |         |

Configuration

| Option            | Description                                   | Type       | Default    |
| :---------------- | :-------------------------------------------- | :--------- | :--------- |
| `removeDebugInfo` | Remove debug info (vars, params).             | `boolean`  | `false`    |
| `removeTraceInfo` | Remove trace info (source, lines).            | `boolean`  | `false`    |
| `packing`         | Configure resource handling in the main JAR.  | `Function` | *(identity)* |
| `merging`         | Configure resource handling when merging deps. | `Function` | *(identity)* |

### LICENSE
Manages license headers in source files.

| Command  | Description                     | Default |
| :------- | :------------------------------ | :------: |
| `update` | Write license header comment.   |    ✅    |

Configuration

| Option    | Description                        | Type               | Default    |
| :-------- | :--------------------------------- | :----------------- | :--------- |
| `exclude` | Specify files to exclude from updates. | `Predicate<File>` | *(none)*   |

### MAVEN
Provides Maven integration.

| Command | Description        | Default |
| :------ | :----------------- | :------: |
| `pom`   | Generate pom file. |    ✅    |

### NATIVE
Builds native executables using GraalVM Native Image.

| Command | Description                   | Default |
| :------ | :---------------------------- | :------: |
| `build` | Build native execution file.  |    ✅    |
| `run`   | Run the native executable.    |         |

Configuration

| Option      | Description                                | Type           | Default       |
| :---------- | :----------------------------------------- | :------------- | :------------ |
| `protocols` | Network protocols to include.              | `List<String>` | `[http, https]` |
| `resources` | Resource file patterns to include.         | `List<String>` | `[...]`       |
| `params`    | Additional parameters for Native Image builder. | `List<String>` | `[]`          |

### PROTOTYPE
Generates project skeletons.

| Command | Description                         | Default |
| :------ | :---------------------------------- | :------: |
| `java`  | Generate standard Java project skeleton. |    ✅    |

### TEST
Compiles and runs project tests.

| Command | Description         | Default |
| :------ | :------------------ | :------: |
| `test`  | Test product codes. |    ✅    |

Configuration

| Option    | Description                                     | Type           | Default |
| :-------- | :---------------------------------------------- | :------------- | :------ |
| `longRun` | Threshold (ms) to report long-running tests.    | `int`          | `1000`  |
| `java`    | Specify the JVM used for test execution.        | `Directory`    | *(system default)* |
| `params`  | Additional JVM parameters for test execution.   | `List<String>` | `[]`    |

### WRAPPER
Manages the Bee wrapper installation for the project.

| Command  | Description                                           | Default |
| :------- | :---------------------------------------------------- | :------: |
| `clean`  | Clean local bee environment installed by the wrapper. |         |
| `latest` | Set up wrapper to use the latest Bee release.         |         |
| `local`  | Set up wrapper to use a locally installed Bee version.  |         |
| `select` | Set up wrapper using a user-selected Bee version.     |         |
| `stable` | Set up wrapper to use the latest stable Bee release.  |    ✅    |
| `use`    | Set up wrapper to use a specific Bee version.         |         |

Configuration

| Option    | Description                  | Type     | Default |
| :-------- | :--------------------------- | :------- | :------ |
| `version` | Specify the Bee version for the wrapper. | `String` | *(latest stable)* |
<p align="right"><a href="#top">back to top</a></p>






## Prerequisites
Bee runs on all major operating systems and requires only [Java version 24](https://docs.oracle.com/en/java/javase/24/) or later to run.
To check, please run `java -version` on your terminal.
<p align="right"><a href="#top">back to top</a></p>

## Install
For any code snippet below, please substitute the version given with the version of Bee you wish to use.
#### [Maven](https://maven.apache.org/)
Add JitPack repository at the end of repositories element in your build.xml:
```xml
<repository>
    <id>jitpack.io</id>
    <url>https://jitpack.io</url>
</repository>
```
Add it into in the dependencies element like so:
```xml
<dependency>
    <groupId>com.github.teletha</groupId>
    <artifactId>bee</artifactId>
    <version>0.76.0</version>
</dependency>
```
#### [Gradle](https://gradle.org/)
Add JitPack repository at the end of repositories in your build.gradle:
```gradle
repositories {
    maven { url "https://jitpack.io" }
}
```
Add it into the dependencies section like so:
```gradle
dependencies {
    implementation 'com.github.teletha:bee:0.76.0'
}
```
#### [SBT](https://www.scala-sbt.org/)
Add JitPack repository at the end of resolvers in your build.sbt:
```scala
resolvers += "jitpack" at "https://jitpack.io"
```
Add it into the libraryDependencies section like so:
```scala
libraryDependencies += "com.github.teletha" % "bee" % "0.76.0"
```
#### [Leiningen](https://leiningen.org/)
Add JitPack repository at the end of repositories in your project().clj:
```clj
:repositories [["jitpack" "https://jitpack.io"]]
```
Add it into the dependencies section like so:
```clj
:dependencies [[com.github.teletha/bee "0.76.0"]]
```
#### [Bee](https://teletha.github.io/bee)
Add it into your project definition class like so:
```java
require("com.github.teletha", "bee", "0.76.0");
```
<p align="right"><a href="#top">back to top</a></p>


## Contributing
Contributions are what make the open source community such an amazing place to learn, inspire, and create. Any contributions you make are **greatly appreciated**.
If you have a suggestion that would make this better, please fork the repo and create a pull request. You can also simply open an issue with the tag "enhancement".
Don't forget to give the project a star! Thanks again!

1. Fork the Project
2. Create your Feature Branch (`git checkout -b feature/AmazingFeature`)
3. Commit your Changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the Branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

The overwhelming majority of changes to this project don't add new features at all. Optimizations, tests, documentation, refactorings -- these are all part of making this product meet the highest standards of code quality and usability.
Contributing improvements in these areas is much easier, and much less of a hassle, than contributing code for new features.

### Bug Reports
If you come across a bug, please file a bug report. Warning us of a bug is possibly the most valuable contribution you can make to Bee.
If you encounter a bug that hasn't already been filed, [please file a report](https://github.com/teletha/bee/issues/new) with an [SSCCE](http://sscce.org/) demonstrating the bug.
If you think something might be a bug, but you're not sure, ask on StackOverflow or on [bee-discuss](https://github.com/teletha/bee/discussions).
<p align="right"><a href="#top">back to top</a></p>


## Dependency
Bee depends on the following products on runtime.
* [auto483-1.0.0](https://mvnrepository.com/artifact/com.github.teletha/auto483/1.0.0)
* [conjure-1.2.1](https://mvnrepository.com/artifact/com.github.teletha/conjure/1.2.1)
* [javax.inject-1](https://mvnrepository.com/artifact/javax.inject/javax.inject/1)
* [maven-api-meta-4.0.0-alpha-7](https://mvnrepository.com/artifact/org.apache.maven/maven-api-meta/4.0.0-alpha-7)
* [maven-api-model-4.0.0-alpha-7](https://mvnrepository.com/artifact/org.apache.maven/maven-api-model/4.0.0-alpha-7)
* [maven-api-xml-4.0.0-alpha-7](https://mvnrepository.com/artifact/org.apache.maven/maven-api-xml/4.0.0-alpha-7)
* [maven-artifact-4.0.0-alpha-7](https://mvnrepository.com/artifact/org.apache.maven/maven-artifact/4.0.0-alpha-7)
* [maven-builder-support-4.0.0-alpha-7](https://mvnrepository.com/artifact/org.apache.maven/maven-builder-support/4.0.0-alpha-7)
* [maven-model-4.0.0-alpha-7](https://mvnrepository.com/artifact/org.apache.maven/maven-model/4.0.0-alpha-7)
* [maven-model-builder-4.0.0-alpha-7](https://mvnrepository.com/artifact/org.apache.maven/maven-model-builder/4.0.0-alpha-7)
* [maven-model-transform-4.0.0-alpha-7](https://mvnrepository.com/artifact/org.apache.maven/maven-model-transform/4.0.0-alpha-7)
* [maven-repository-metadata-4.0.0-alpha-7](https://mvnrepository.com/artifact/org.apache.maven/maven-repository-metadata/4.0.0-alpha-7)
* [maven-resolver-api-2.0.0-alpha-7](https://mvnrepository.com/artifact/org.apache.maven.resolver/maven-resolver-api/2.0.0-alpha-7)
* [maven-resolver-connector-basic-2.0.0-alpha-7](https://mvnrepository.com/artifact/org.apache.maven.resolver/maven-resolver-connector-basic/2.0.0-alpha-7)
* [maven-resolver-impl-2.0.0-alpha-7](https://mvnrepository.com/artifact/org.apache.maven.resolver/maven-resolver-impl/2.0.0-alpha-7)
* [maven-resolver-named-locks-2.0.0-alpha-7](https://mvnrepository.com/artifact/org.apache.maven.resolver/maven-resolver-named-locks/2.0.0-alpha-7)
* [maven-resolver-provider-4.0.0-alpha-7](https://mvnrepository.com/artifact/org.apache.maven/maven-resolver-provider/4.0.0-alpha-7)
* [maven-resolver-spi-2.0.0-alpha-7](https://mvnrepository.com/artifact/org.apache.maven.resolver/maven-resolver-spi/2.0.0-alpha-7)
* [maven-resolver-util-2.0.0-alpha-7](https://mvnrepository.com/artifact/org.apache.maven.resolver/maven-resolver-util/2.0.0-alpha-7)
* [maven-xml-impl-4.0.0-alpha-7](https://mvnrepository.com/artifact/org.apache.maven/maven-xml-impl/4.0.0-alpha-7)
* [plexus-interpolation-1.26](https://mvnrepository.com/artifact/org.codehaus.plexus/plexus-interpolation/1.26)
* [plexus-utils-4.0.0](https://mvnrepository.com/artifact/org.codehaus.plexus/plexus-utils/4.0.0)
* [plexus-xml-4.0.1](https://mvnrepository.com/artifact/org.codehaus.plexus/plexus-xml/4.0.1)
* [psychopath-2.2.0](https://mvnrepository.com/artifact/com.github.teletha/psychopath/2.2.0)
* [sinobu-4.6.1](https://mvnrepository.com/artifact/com.github.teletha/sinobu/4.6.1)
* [slf4j-api-2.1.0-alpha1](https://mvnrepository.com/artifact/org.slf4j/slf4j-api/2.1.0-alpha1)
<p align="right"><a href="#top">back to top</a></p>


## License
Copyright (C) 2025 The BEE Development Team

MIT License

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
<p align="right"><a href="#top">back to top</a></p>