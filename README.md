<p align="center">
    <a href="https://docs.oracle.com/en/java/javase/11/"><img src="https://img.shields.io/badge/Java-Release%2011-green"/></a>
    <span>&nbsp;</span>
    <a href="https://jitpack.io/#teletha/bee"><img src="https://img.shields.io/jitpack/v/github/teletha/bee?label=Repository&color=green"></a>
    <span>&nbsp;</span>
    <a href="https://teletha.github.io/bee"><img src="https://img.shields.io/website.svg?down_color=red&down_message=CLOSE&label=Official%20Site&up_color=green&up_message=OPEN&url=https%3A%2F%2Fteletha.github.io%2Fbee"></a>
</p>


## About The Project
Bee is an open source build automation tool that focuses on conventions, type safety and performance.
Project and build task definitions are written in Java, ensuring flexible extensibility for programmers.

#### Minimize settings
Use default values to minimize the number of items that need to be set as much as possible. Also, all settings are type-safe and complementary, so you don't have to search for minor settings in the documentation.

#### Fast execution
All tasks are executed in parallel, and all output is cached and reused.

#### Repository oriented
It recognizes source code and package repositories and automates the entire lifecycle from development to release.
<p align="right"><a href="#top">back to top</a></p>


## Prerequisites
Bee runs on all major operating systems and requires only [Java version 11](https://docs.oracle.com/en/java/javase/11/) or later to run.
To check, please run `java -version` from the command line interface. You should see something like this:
```
> java -version
openjdk version "16" 2021-03-16
OpenJDK Runtime Environment (build 16+36-2231)
OpenJDK 64-Bit Server VM (build 16+36-2231, mixed mode, sharing)
```
<p align="right"><a href="#top">back to top</a></p>

## Using in your build
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
    <version>0.22.0</version>
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
    implementation 'com.github.teletha:bee:0.22.0'
}
```
#### [SBT](https://www.scala-sbt.org/)
Add JitPack repository at the end of resolvers in your build.sbt:
```scala
resolvers += "jitpack" at "https://jitpack.io"
```
Add it into the libraryDependencies section like so:
```scala
libraryDependencies += "com.github.teletha" % "bee" % "0.22.0"
```
#### [Leiningen](https://leiningen.org/)
Add JitPack repository at the end of repositories in your project.clj:
```clj
:repositories [["jitpack" "https://jitpack.io"]]
```
Add it into the dependencies section like so:
```clj
:dependencies [[com.github.teletha/bee "0.22.0"]]
```
#### [Bee](https://teletha.github.io/bee)
Add it into your project definition class like so:
```java
require("com.github.teletha", "bee", "0.22.0");
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


## Built with
Bee depends on the following products on runtime.
* [asm-9.2](https://mvnrepository.com/artifact/org.ow2.asm/asm/9.2)
* [commons-lang3-3.8.1](https://mvnrepository.com/artifact/org.apache.commons/commons-lang3/3.8.1)
* [httpclient-4.5.13](https://mvnrepository.com/artifact/org.apache.httpcomponents/httpclient/4.5.13)
* [httpcore-4.4.14](https://mvnrepository.com/artifact/org.apache.httpcomponents/httpcore/4.4.14)
* [javax.annotation-api-1.3.2](https://mvnrepository.com/artifact/javax.annotation/javax.annotation-api/1.3.2)
* [javax.inject-1](https://mvnrepository.com/artifact/javax.inject/javax.inject/1)
* [jcl-over-slf4j-2.0.0-alpha5](https://mvnrepository.com/artifact/org.slf4j/jcl-over-slf4j/2.0.0-alpha5)
* [jul-to-slf4j-2.0.0-alpha5](https://mvnrepository.com/artifact/org.slf4j/jul-to-slf4j/2.0.0-alpha5)
* [maven-artifact-3.8.4](https://mvnrepository.com/artifact/org.apache.maven/maven-artifact/3.8.4)
* [maven-builder-support-3.8.4](https://mvnrepository.com/artifact/org.apache.maven/maven-builder-support/3.8.4)
* [maven-model-3.8.4](https://mvnrepository.com/artifact/org.apache.maven/maven-model/3.8.4)
* [maven-model-builder-3.8.4](https://mvnrepository.com/artifact/org.apache.maven/maven-model-builder/3.8.4)
* [maven-repository-metadata-3.8.4](https://mvnrepository.com/artifact/org.apache.maven/maven-repository-metadata/3.8.4)
* [maven-resolver-api-1.7.2](https://mvnrepository.com/artifact/org.apache.maven.resolver/maven-resolver-api/1.7.2)
* [maven-resolver-connector-basic-1.7.2](https://mvnrepository.com/artifact/org.apache.maven.resolver/maven-resolver-connector-basic/1.7.2)
* [maven-resolver-impl-1.7.2](https://mvnrepository.com/artifact/org.apache.maven.resolver/maven-resolver-impl/1.7.2)
* [maven-resolver-named-locks-1.7.2](https://mvnrepository.com/artifact/org.apache.maven.resolver/maven-resolver-named-locks/1.7.2)
* [maven-resolver-provider-3.8.4](https://mvnrepository.com/artifact/org.apache.maven/maven-resolver-provider/3.8.4)
* [maven-resolver-spi-1.7.2](https://mvnrepository.com/artifact/org.apache.maven.resolver/maven-resolver-spi/1.7.2)
* [maven-resolver-transport-http-1.7.2](https://mvnrepository.com/artifact/org.apache.maven.resolver/maven-resolver-transport-http/1.7.2)
* [maven-resolver-util-1.7.2](https://mvnrepository.com/artifact/org.apache.maven.resolver/maven-resolver-util/1.7.2)
* [plexus-interpolation-1.26](https://mvnrepository.com/artifact/org.codehaus.plexus/plexus-interpolation/1.26)
* [plexus-utils-3.3.0](https://mvnrepository.com/artifact/org.codehaus.plexus/plexus-utils/3.3.0)
* [psychopath-1.6.0](https://mvnrepository.com/artifact/com.github.teletha/psychopath/1.6.0)
* [sinobu-2.10.0](https://mvnrepository.com/artifact/com.github.teletha/sinobu/2.10.0)
* [slf4j-api-2.0.0-alpha5](https://mvnrepository.com/artifact/org.slf4j/slf4j-api/2.0.0-alpha5)
* [slf4j-nop-2.0.0-alpha5](https://mvnrepository.com/artifact/org.slf4j/slf4j-nop/2.0.0-alpha5)

Bee depends on the following products on test.
* [antibug-1.2.0](https://mvnrepository.com/artifact/com.github.teletha/antibug/1.2.0)
* [apiguardian-api-1.1.2](https://mvnrepository.com/artifact/org.apiguardian/apiguardian-api/1.1.2)
* [byte-buddy-1.12.5](https://mvnrepository.com/artifact/net.bytebuddy/byte-buddy/1.12.5)
* [byte-buddy-agent-1.12.5](https://mvnrepository.com/artifact/net.bytebuddy/byte-buddy-agent/1.12.5)
* [junit-jupiter-api-5.8.1](https://mvnrepository.com/artifact/org.junit.jupiter/junit-jupiter-api/5.8.1)
* [junit-jupiter-engine-5.8.1](https://mvnrepository.com/artifact/org.junit.jupiter/junit-jupiter-engine/5.8.1)
* [junit-jupiter-params-5.8.1](https://mvnrepository.com/artifact/org.junit.jupiter/junit-jupiter-params/5.8.1)
* [junit-platform-commons-1.8.1](https://mvnrepository.com/artifact/org.junit.platform/junit-platform-commons/1.8.1)
* [junit-platform-engine-1.8.1](https://mvnrepository.com/artifact/org.junit.platform/junit-platform-engine/1.8.1)
* [junit-platform-launcher-1.8.2](https://mvnrepository.com/artifact/org.junit.platform/junit-platform-launcher/1.8.2)
* [opentest4j-1.2.0](https://mvnrepository.com/artifact/org.opentest4j/opentest4j/1.2.0)
<p align="right"><a href="#top">back to top</a></p>


## License
Copyright (C) 2021 The BEE Development Team

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