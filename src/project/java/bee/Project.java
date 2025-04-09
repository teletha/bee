/*
 * Copyright (C) 2025 The BEE Development Team
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          https://opensource.org/licenses/MIT
 */
package bee;

import static bee.api.License.*;

import javax.lang.model.SourceVersion;

import bee.task.FindMain;

public class Project extends bee.api.Project {

    {
        product(Bee.Tool.getGroup(), Bee.Tool.getProduct(), ref("version.txt"));
        license(MIT);

        require(SourceVersion.latest(), SourceVersion.RELEASE_24);

        // MAVEN REPOSITORY
        // Since 4.0.0-beta, Maven has become a super heavyweight library, with dependencies
        // on woodstox for XML parsing, bouncycastle for checksum(?) and apache-http-client
        // for HTTP communication.
        // Maven seems to thoroughly adhere to backward compatibility, so further version upgrades
        // are currently unnecessary.
        //
        // The use of alpha-8 causes a dependency on woodstox, so it is stopped at alpha-7.
        String version = "2.0.0-alpha-7";
        require("org.apache.maven", "maven-resolver-provider", "4.0.0-alpha-7");
        require("org.apache.maven.resolver", "maven-resolver-api", version);
        require("org.apache.maven.resolver", "maven-resolver-spi", version);
        require("org.apache.maven.resolver", "maven-resolver-util", version);
        require("org.apache.maven.resolver", "maven-resolver-impl", version);
        require("org.apache.maven.resolver", "maven-resolver-connector-basic", version);
        require("org.apache.maven.resolver", "maven-resolver-named-locks", version);

        // LOGGER
        require("org.slf4j", "slf4j-api", "[2.0,)");
        // require("org.slf4j", "slf4j-nop");
        // require("org.slf4j", "jul-to-slf4j");
        require("com.github.teletha", "conjure");

        // REQUIRED
        require("com.github.teletha", "sinobu");
        require("com.github.teletha", "psychopath");
        require("com.github.teletha", "auto483");

        // DYNAMICALLY ON RUNTIME
        require("org.junit.platform", "junit-platform-engine").atProvided();
        require("org.junit.platform", "junit-platform-launcher").atProvided();
        require("com.github.teletha", "javadng").atProvided();
        require("org.eclipse.jgit", "org.eclipse.jgit").atProvided();
        require("org.eclipse.jdt", "ecj").atProvided();
        require("io.github.classgraph", "classgraph").atProvided();
        // require("org.graalvm.polyglot", "polyglot").atProvided();
        // require("org.graalvm.polyglot", "java-community").atProvided().byPom();
        // require("org.graalvm.polyglot", "js-community").atProvided().byPom();
        // require("org.graalvm.polyglot", "python-community").atProvided().byPom();
        // require("org.graalvm.espresso", "java").atProvided().byPom();
        // require("org.graalvm.espresso", "espresso-runtime-resources-jdk21").atProvided();

        // TEST
        require("com.github.teletha", "antibug").atTest();

        unrequire("commons-codec", "commons-codec");
        unrequire("org.apache.maven", "plexus-utils");
        unrequire("org.eclipse.sisu", "org.eclipse.sisu.inject");
        unrequire("org.eclipse.sisu", "org.eclipse.sisu.plexus");
        unrequire("org.codehaus.plexus", "plexus-classworlds");
        unrequire("org.codehaus.plexus", "plexus-component-annotations");

        config(FindMain.class, task -> {
            task.main = BeeInstaller.class.getName();
        });

        describe("""
                Bee is a modern, open-source build automation tool focused on conventions, type safety, and performance.

                Tired of complex configurations and slow build times? Bee leverages the power and familiarity of Java to define projects and build tasks, offering a flexible and highly extensible experience for developers.

                ## ✨ Features
                #### ☕ Java-Defined Project
                Write your build logic directly in Java. Enjoy type safety, easy refactoring, powerful IDE support (completion, navigation), and seamless integration with your existing Java codebase. Forget complex DSLs or XML – stick with the language you know best.

                #### 🧩 Flexible Task System
                Easily add custom tasks or modify existing ones. Simply create/implement a Java interface extending `bee.Task` in your project and define/override command methods. Bee automatically discovers and prioritizes your project-specific tasks without complex configuration files.

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
                """);
    }
}