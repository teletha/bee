# Changelog

## [0.40.0](https://github.com/teletha/bee/compare/v0.39.1...v0.40.0) (2024-01-05)


### Features

* update maven-resolver ([abb37aa](https://github.com/teletha/bee/commit/abb37aae3b5b1807dbc0e07755edc50b98bc46a2))


### Bug Fixes

* dependency injection ([8f1101c](https://github.com/teletha/bee/commit/8f1101c97797e8ef4ff0b4bba84f6b7ff24e5314))
* update conjure ([6237320](https://github.com/teletha/bee/commit/62373201b5a3edaeb335c6ef77380cea72abe40f))
* update license ([9dd399d](https://github.com/teletha/bee/commit/9dd399d71324b6e485604932df1e8ac87260b883))
* update maven-resolver ([565fe85](https://github.com/teletha/bee/commit/565fe850ae965be51f146a38a0d6ba174c1bd85b))
* use temporal local repository in test ([acf9412](https://github.com/teletha/bee/commit/acf941236510364a643158b952ec87d3b5fcb8d2))

## [0.39.1](https://github.com/teletha/bee/compare/v0.39.0...v0.39.1) (2023-08-26)


### Bug Fixes

* update sinobu ([f45b8a4](https://github.com/teletha/bee/commit/f45b8a435e697174eaae28f17dde550f1c1cc2aa))

## [0.39.0](https://github.com/teletha/bee/compare/v0.38.0...v0.39.0) (2023-06-06)


### Features

* Use latest version resolver when conflicts libraries. ([36c4ae1](https://github.com/teletha/bee/commit/36c4ae1b9650ec1aade2e5e0f5847359083c1b5e))


### Bug Fixes

* Change location of the downloadable exewrap. ([7e62957](https://github.com/teletha/bee/commit/7e62957c10c365fac58ccdc4a4b4e1944a687b32))
* Jar:source task should not delete resources. ([1581231](https://github.com/teletha/bee/commit/15812317a8f1d76996af50dd6cb4032b0b9c938c))
* JLink can sync the last modified date. ([246596b](https://github.com/teletha/bee/commit/246596bbc8cf7a55b4036a11cba64586d46b3100))
* Use the latest slf4j. ([29f1b53](https://github.com/teletha/bee/commit/29f1b53d7fa22a53e74adcd7a19a4258511886c6))

## [0.38.0](https://github.com/teletha/bee/compare/v0.37.0...v0.38.0) (2023-05-18)


### Features

* Exe task can archive additional resources. ([6e10499](https://github.com/teletha/bee/commit/6e10499f1d05f9fd3628393ac11787d0a71aab02))


### Bug Fixes

* exe task must have built-in modules. ([6ae86a1](https://github.com/teletha/bee/commit/6ae86a1cad85ca56cc1fac41c3a5c115a1d653c5))
* Exe task should hold java.exe too. ([932218e](https://github.com/teletha/bee/commit/932218e09c28056b02d6ee69b407335b732033f1))
* Ignore error on module finding process. ([5d1b375](https://github.com/teletha/bee/commit/5d1b3750e5a40e04f25e7aecc9e48108362807d7))
* Update exewrap. ([ad0509c](https://github.com/teletha/bee/commit/ad0509c0a03679036864e04aa71d886ba1581aad))

## [0.37.0](https://github.com/teletha/bee/compare/v0.36.0...v0.37.0) (2023-03-02)


### Features

* NetTransporter implements peek method. ([f112d18](https://github.com/teletha/bee/commit/f112d184dba88e170daac8c6c828df728f68f0d3))


### Bug Fixes

* dependency task shows classifier ([6e66798](https://github.com/teletha/bee/commit/6e6679864ee16b23a0d2dd5256126b51dec4d883))
* HEAD is not implemented in Java17. ([684f0c1](https://github.com/teletha/bee/commit/684f0c1072ce90caa0f436e0690f43480fb998f0))
* update sinobu ([667889c](https://github.com/teletha/bee/commit/667889ce732edd949b5d11861be54f8f2539835d))

## [0.36.0](https://github.com/teletha/bee/compare/v0.35.0...v0.36.0) (2023-01-06)


### Features

* Add dependency task. ([947212f](https://github.com/teletha/bee/commit/947212f700a9e0a11647cdb0ce49d9ed42994a7e))


### Bug Fixes

* Ignore HTTP error status. ([4701cf4](https://github.com/teletha/bee/commit/4701cf48eefe8edf82e3a7f3f628b17b3adde3f5))

## [0.35.0](https://github.com/teletha/bee/compare/v0.34.1...v0.35.0) (2023-01-05)


### Features

* Replace maven-resolver-transport-http ([99f08ee](https://github.com/teletha/bee/commit/99f08eec3c36137cb8be1eb7b1cc90c6c3061b87))
* resolve conflict by nearest-latest-strategy ([606b55e](https://github.com/teletha/bee/commit/606b55e26adf085d1e2779de533dcf92117bbe20))


### Bug Fixes

* format test output ([4ff0316](https://github.com/teletha/bee/commit/4ff0316b06c31c062fe0012d4d8da8a8a3c4440e))
* update javaparser ([219f408](https://github.com/teletha/bee/commit/219f4086ac5c1f0c25b06837e255d881ae0de701))

## [0.34.1](https://github.com/teletha/bee/compare/v0.34.0...v0.34.1) (2022-12-27)


### Bug Fixes

* Don't load this compiled project. ([6049675](https://github.com/teletha/bee/commit/6049675617757954617520d64d1123efbdefc7a6))

## [0.34.0](https://github.com/teletha/bee/compare/v0.33.2...v0.34.0) (2022-12-25)


### Features

* Latest version means snapshoted version. ([464574a](https://github.com/teletha/bee/commit/464574af3f27f5b4609fbe946d446b7e2794e808))

## [0.33.2](https://github.com/teletha/bee/compare/v0.33.1...v0.33.2) (2022-12-19)


### Bug Fixes

* revert maven-resolver-provider because of its bug ([727eeac](https://github.com/teletha/bee/commit/727eeac017f0e30b6c608f543cf902ec5632b7a7))
* update sinobu and javadng ([caa0f54](https://github.com/teletha/bee/commit/caa0f54b14bd05edccf0689443d79774da6d2a3a))

## [0.33.1](https://github.com/teletha/bee/compare/v0.33.0...v0.33.1) (2022-12-19)


### Bug Fixes

* update sinobu ([6dc3cc5](https://github.com/teletha/bee/commit/6dc3cc519b5dee7ba4ca37171d4293d253c3dee7))

## [0.33.0](https://github.com/teletha/bee/compare/v0.32.2...v0.33.0) (2022-12-18)


### Features

* Update required java version to 17 ([cd875b8](https://github.com/teletha/bee/commit/cd875b8cc1db9c369a600e600c601e861de1161b))

### [0.32.2](https://www.github.com/teletha/bee/compare/v0.32.1...v0.32.2) (2022-11-29)


### Bug Fixes

* update javadng ([336731d](https://www.github.com/teletha/bee/commit/336731dbd776fd32717419390fe945396b32a490))

### [0.32.1](https://www.github.com/teletha/bee/compare/v0.32.0...v0.32.1) (2022-11-29)


### Bug Fixes

* Can't load project related classes. ([413bde3](https://www.github.com/teletha/bee/commit/413bde3ae291f7a749e8ab7a11f5f8cbed0687be))
* CI task outputs the invalid code. ([fb6c454](https://www.github.com/teletha/bee/commit/fb6c4545207b2c3bdfdc5a93eece406bef58bfe6))

## [0.32.0](https://www.github.com/teletha/bee/compare/v0.31.0...v0.32.0) (2022-11-29)


### Features

* Add snippet. ([9a6ecda](https://www.github.com/teletha/bee/commit/9a6ecda4b124d2f68b55ca1d25189976f7864286))
* Update maven and aether api. ([565d57d](https://www.github.com/teletha/bee/commit/565d57d53cebafda0eeed611fc56068a6b220579))


### Bug Fixes

* NPE on CI task. ([5c5cfbf](https://www.github.com/teletha/bee/commit/5c5cfbf4ddc9280933e142312e3668f1b1ea38ea))
* Update github actions. ([63fdb99](https://www.github.com/teletha/bee/commit/63fdb99a1e6bad9fc24d12ab3f57be2ee93c9d56))
* update readme on ci ([4bfc326](https://www.github.com/teletha/bee/commit/4bfc326283788cd14bcef6d841d293fc55bd96a2))
* Update sinobu. ([c91dd50](https://www.github.com/teletha/bee/commit/c91dd5019afb35583d56e4a229fd901b7b897009))

## [0.31.0](https://www.github.com/teletha/bee/compare/v0.30.1...v0.31.0) (2022-06-30)


### Features

* Eclipse task resolves sources in parallel. ([91eeead](https://www.github.com/teletha/bee/commit/91eeead9d9d050fbcbecff8ee7689e5d68a8dd31))
* Exe task can build custom JRE. ([6f24e8b](https://www.github.com/teletha/bee/commit/6f24e8bafe0c0ffa3873bd98826a7d68181ad190))
* Exe task generates custom JRE automatically. ([26cf5fd](https://www.github.com/teletha/bee/commit/26cf5fdb2c84cb2e1d2ce1a34bcd295f7e0ade07))
* Exe task requires the passing tests. ([00b1bd4](https://www.github.com/teletha/bee/commit/00b1bd4e00d6bc73b50a23a6398b629ef7abfca3))


### Bug Fixes

* Exe task generates 64bit application only. ([2536e12](https://www.github.com/teletha/bee/commit/2536e127fc7714d650ad52f1bedadcc2dc3e2264))
* Exe task ignore uncaught exception. ([eae6f84](https://www.github.com/teletha/bee/commit/eae6f8475cb92ed65f698c79c3b8932f4e397a59))
* Exe task is broken. ([b3e41a4](https://www.github.com/teletha/bee/commit/b3e41a4d9e278e6a9b6bf41c09c597f39bd9e1bb))
* JavaCompilerTest was failed. ([e9e61e7](https://www.github.com/teletha/bee/commit/e9e61e74899dfde790c2ad20b93e097ab38433ff))
* Reduce message on license task. ([5230c6e](https://www.github.com/teletha/bee/commit/5230c6efddc7eea4f3d817162690c300d811ee6d))
* update aether resolver ([0a17783](https://www.github.com/teletha/bee/commit/0a17783695bb634c1e9f7755c4f71d0eb55d95b7))

### [0.30.1](https://www.github.com/teletha/bee/compare/v0.30.0...v0.30.1) (2022-01-22)


### Bug Fixes

* Test task is broken. ([b7960d2](https://www.github.com/teletha/bee/commit/b7960d28c56491924ff73a60c1941c6be6dc8a82))

## [0.30.0](https://www.github.com/teletha/bee/compare/v0.29.0...v0.30.0) (2022-01-22)


### Features

* Add Project#associate to store the project related info. ([f966de1](https://www.github.com/teletha/bee/commit/f966de10ee89717d9dad913f1410ecc2ae377c50))
* All configurable value on tasks are non-static protected field. ([d1753be](https://www.github.com/teletha/bee/commit/d1753be3dd2353918ab2642b9fb6d653fdb46e7b))
* The command execution results are cached for each project. ([384d94a](https://www.github.com/teletha/bee/commit/384d94a0fcec9e1cd750cb0a7a1258002d54da11))


### Bug Fixes

* Jar task can't include resources when modified jar. ([0a15269](https://www.github.com/teletha/bee/commit/0a15269db8c0eff89c1c4d42d0eae4dbcfeaa384))
* Jar task can't pack non-class resources when modified jar. ([536bcca](https://www.github.com/teletha/bee/commit/536bcca817e9e5bd72075133b97733d7f9d51da9))
* Project#getAnnotationProcessor must return all dependencies. ([4476081](https://www.github.com/teletha/bee/commit/4476081c755d7a347c75c8e16af38d2e50a41c82))
* Update the generated bytecode version to 16. ([46ede28](https://www.github.com/teletha/bee/commit/46ede28feaea1afb7e0823730a8098730c9837f6))

## [0.29.0](https://www.github.com/teletha/bee/compare/v0.28.2...v0.29.0) (2022-01-20)


### Features

* Add --skip (-x) option to skip any task execution. ([5a88114](https://www.github.com/teletha/bee/commit/5a88114f86a13b03179b498371f73e4ae6ab35f7))
* Commandline option can define system property like maven. ([1ca135c](https://www.github.com/teletha/bee/commit/1ca135cd960d1b54d9ed06a026f809d87a98407d))
* Enable multilined commandline interface. ([ec30c24](https://www.github.com/teletha/bee/commit/ec30c246f70bc0de9786e05a1705274175e8a03f))


### Bug Fixes

* Avoid NPE. ([a273b79](https://www.github.com/teletha/bee/commit/a273b795c67ebc97076819d4c74c3926b65d836f))
* Bee option is aware of environment variable. ([151fe78](https://www.github.com/teletha/bee/commit/151fe78ab42f40b961c76f8dcc6d0651cd6ade58))
* Update exewrap version. ([a571c99](https://www.github.com/teletha/bee/commit/a571c9984a837f4b5c770effc514b76efb2cf96b))

### [0.28.2](https://www.github.com/teletha/bee/compare/v0.28.1...v0.28.2) (2022-01-18)


### Bug Fixes

* Don't load target classes. ([dd323ce](https://www.github.com/teletha/bee/commit/dd323cee12340763ca4a1dbdcf1d6c9bf59001df))

### [0.28.1](https://www.github.com/teletha/bee/compare/v0.28.0...v0.28.1) (2022-01-18)


### Bug Fixes

* Auto detected repository uri is invalid. ([ea4400a](https://www.github.com/teletha/bee/commit/ea4400a2bc11bc5503fa83e2b23c0f4787951190))

## [0.28.0](https://www.github.com/teletha/bee/compare/v0.27.0...v0.28.0) (2022-01-18)


### Features

* Add --help (-h) option. ([9aecd59](https://www.github.com/teletha/bee/commit/9aecd5943c20e11636df6136028d80f5440da944))
* Add --quiet (-q) and --debug (-d) options. ([6b6a5ed](https://www.github.com/teletha/bee/commit/6b6a5ed4da0a3769bba5a4c2d4fc9c8879c4bd76))
* Add new tasks [help:task] and [help:option]. ([d91d38c](https://www.github.com/teletha/bee/commit/d91d38c463bbfde2886bd0ec60ed23d339fbc820))
* Drop Notation. ([2979c75](https://www.github.com/teletha/bee/commit/2979c75dd43cf907377a80fd35778a4710f847ff))


### Bug Fixes

* Dependency resolving is aware of no-cache option. ([01a0415](https://www.github.com/teletha/bee/commit/01a041525f73f8fb4bff74cf40088c9c41c93c7b))

## [0.27.0](https://www.github.com/teletha/bee/compare/v0.26.1...v0.27.0) (2022-01-17)


### Features

* Add --nocache (-n) option. ([26f82ab](https://www.github.com/teletha/bee/commit/26f82ab81a625b7c30cf0264d889312fcbf523d5))
* Add --offline (-o) option. ([46e98e5](https://www.github.com/teletha/bee/commit/46e98e5c2510cbccefec50e80c70ec6fc0cc313a))
* Add --version (-v) option. ([e740649](https://www.github.com/teletha/bee/commit/e7406494ea3695bdfc3b90a536b5e9d6e11fc423))

### [0.26.1](https://www.github.com/teletha/bee/compare/v0.26.0...v0.26.1) (2022-01-17)


### Bug Fixes

* Don't compute frame when version modification. ([5be54a9](https://www.github.com/teletha/bee/commit/5be54a9f5fca6025e115ed91202aa1553d9b31e2))
* Drop unknown vcs. ([db2c7ac](https://www.github.com/teletha/bee/commit/db2c7accadc180f727aed82dfb7d3b6aac13cf7c))

## [0.26.0](https://www.github.com/teletha/bee/compare/v0.25.1...v0.26.0) (2022-01-17)


### Features

* Add JavaCompiler#setCompileAll. ([776fcdd](https://www.github.com/teletha/bee/commit/776fcdd90ede69eb5edc9f19f1329ae164667ad1))
* Add profiler. ([b4678ac](https://www.github.com/teletha/bee/commit/b4678ac92d7aeb5f62bacb66cc0d33ca79ecd017))
* Add profiling option (-p or -profiling). ([717c16d](https://www.github.com/teletha/bee/commit/717c16d7862eb054d67db26b3d0b8ee7663a0400))
* Drop configuration for test class version. ([bd0fa88](https://www.github.com/teletha/bee/commit/bd0fa88734c45f96e1cfa9cc5eb564c58e158cb4))
* Parallel and fast artifact downloading. ([c937977](https://www.github.com/teletha/bee/commit/c937977a5b65432c6217531bdb2eb0224075970b))
* Provide fast DependencyCollector. ([f6152f8](https://www.github.com/teletha/bee/commit/f6152f86febd97f65061c19506192e8f4fb4a064))


### Bug Fixes

* Compile task is aware of java version setting. ([56e5ba2](https://www.github.com/teletha/bee/commit/56e5ba222fc4e363ca1169a600c9e49ba12d0f2f))
* Parallel dependency resolver. ([6d1a774](https://www.github.com/teletha/bee/commit/6d1a774d13a0ed857668cbc26118da9688362466))
* Parallel metadata downloading. ([5e604f2](https://www.github.com/teletha/bee/commit/5e604f28e702b63e2d76de7e915ee2384b6eb20e))
* Parse clipboard data as URL. ([fc1a8b6](https://www.github.com/teletha/bee/commit/fc1a8b6d0eb3e4b394f7f179498132e806bea15f))

### [0.25.1](https://www.github.com/teletha/bee/compare/v0.25.0...v0.25.1) (2022-01-14)


### Bug Fixes

* Merged jar must use modified version classes. ([16e8e64](https://www.github.com/teletha/bee/commit/16e8e648284d9f56223756caf69137822bf5fc5b))

## [0.25.0](https://www.github.com/teletha/bee/compare/v0.24.0...v0.25.0) (2022-01-14)


### Features

* Detect version control system automatically. ([0fcc463](https://www.github.com/teletha/bee/commit/0fcc4639c4d951d6572ec9f7ea7cee5573736b6d))


### Bug Fixes

* Update to Java17. ([39bc968](https://www.github.com/teletha/bee/commit/39bc968ca2e3e79c7f5e47c1a9eadabf85af3c98))

## [0.24.0](https://www.github.com/teletha/bee/compare/v0.23.1...v0.24.0) (2022-01-05)


### Features

* Detect version control system automatically when generate project. ([5e8700a](https://www.github.com/teletha/bee/commit/5e8700afbc6cbaffd705b671827cfc783403dc19))
* Disambiguate the command name. ([b9687f7](https://www.github.com/teletha/bee/commit/b9687f796e914eaa8e2b2e0091bd84258a3caa2c))
* Disambiguate the task name. ([c9fc3d3](https://www.github.com/teletha/bee/commit/c9fc3d35c7af962a5b11c376fce3c8a31e5646fc))
* Fetch dependencies in parallel. ([184eeee](https://www.github.com/teletha/bee/commit/184eeee50dadbfdece422e49130de9a5c7e3b4b5))


### Bug Fixes

* Doc:site task will not fail when there is no source code. ([1fd7d4d](https://www.github.com/teletha/bee/commit/1fd7d4d88226cec4eecbb5d7f12096516a93e224))
* Document will not fail when there is no soruce code. ([6d2919a](https://www.github.com/teletha/bee/commit/6d2919a0809056031d185fcb93852877a956d2fc))
* Enhance env:select. ([a40c255](https://www.github.com/teletha/bee/commit/a40c25566ff7f80a6476c43f23feb076e2b9b6b9))
* Env is broken in windows. ([4d8f996](https://www.github.com/teletha/bee/commit/4d8f99620f7345bcb6ede47a2da4903b6a13bf4b))
* Make fetching dependency more faster. ([70afc6a](https://www.github.com/teletha/bee/commit/70afc6a681d6cb4f4c959fe2e60bb6511040a6e8))

### [0.23.1](https://www.github.com/teletha/bee/compare/v0.23.0...v0.23.1) (2022-01-01)


### Bug Fixes

* Eclipse compiler always use -preserveAllLocals. ([b14e971](https://www.github.com/teletha/bee/commit/b14e9710e5f42b50b0460f57e243d9123144c028))

## [0.23.0](https://www.github.com/teletha/bee/compare/v0.22.0...v0.23.0) (2022-01-01)


### Features

* Add Ensure utility. ([81c4553](https://www.github.com/teletha/bee/commit/81c45535979bf76184940846b62703a99d693c14))
* Add Project#licenser. ([c56edc4](https://www.github.com/teletha/bee/commit/c56edc4e987fac2662b07cf9eb3837632950bef2))
* Compile task supports eclipse compiler for java. ([8ca0c8d](https://www.github.com/teletha/bee/commit/8ca0c8d12db9cdda9365debdefec894c368d8974))
* Support eclipse compiler for java. ([7102e42](https://www.github.com/teletha/bee/commit/7102e42516d96906c343d7d8107df303bddc6569))


### Bug Fixes

* If dependency doesn't exist, README outputs the appropriate text. ([77148e7](https://www.github.com/teletha/bee/commit/77148e7d9280911e10e8f2cd86274326988226c8))
* Project#toDefinition outputs license declaration. ([3a022a4](https://www.github.com/teletha/bee/commit/3a022a4faf77a32221bd0d51f98fd6a1c16b8cb7))
* Update maven-resolver. ([f4c67fd](https://www.github.com/teletha/bee/commit/f4c67fd284b6ec76258c731c9cd26f1b197cd988))

## [0.22.0](https://www.github.com/teletha/bee/compare/v0.21.0...v0.22.0) (2021-12-17)


### Features

* Generate README.md on CI task. ([7686e47](https://www.github.com/teletha/bee/commit/7686e47af9296d722dc164d979a9ff0c31ccd36b))
* License can generate the full license text without link. ([c420d02](https://www.github.com/teletha/bee/commit/c420d029bd7cd7cbd02015b7c25d2635631fce11))


### Bug Fixes

* Add license. ([f4f7b29](https://www.github.com/teletha/bee/commit/f4f7b2929e777c95cd4c0ff4805abc8f9b2a7994))

## [0.21.0](https://www.github.com/teletha/bee/compare/v0.20.0...v0.21.0) (2021-12-16)


### Features

* New installer. ([914c335](https://www.github.com/teletha/bee/commit/914c335456f86eb288e4340c57e36f79a9b4728c))
* Task#makeFile detect shell script and use line feed automatically ([f0c91c7](https://www.github.com/teletha/bee/commit/f0c91c7572c6892a30a23cb59a570926e7043972))


### Bug Fixes

* Env task always clean up old envronment. ([d698388](https://www.github.com/teletha/bee/commit/d698388e43bdfbb53768634d883a978784d0da93))
* Failed to rebuild the wrapper when launched from the wrapper. ([b5acf74](https://www.github.com/teletha/bee/commit/b5acf74e14361282d39aaac5bbff17e92fd61f7c))
* Format .gitignore correctly. ([63d5e35](https://www.github.com/teletha/bee/commit/63d5e3504a8cb3d8255d39ac79ad0f1f67e2925a))
* IDE task needs pom file no longer. ([78cf6d8](https://www.github.com/teletha/bee/commit/78cf6d84b94da721f9f8d1fb408c0abd9d095dc6))
* Inputs#hyphenize accepts abbriviation. ([b3c73ed](https://www.github.com/teletha/bee/commit/b3c73edba2acfc5e5699031ab298175ff358bcf8))
* Install task should execute test. ([f425876](https://www.github.com/teletha/bee/commit/f4258760fb14bd8e4811d6f7f1029c2b658c2fe3))
* Normalize output of .gitignore and version.txt ([7de5aeb](https://www.github.com/teletha/bee/commit/7de5aeb8e64b1d2a7e332f5d73acfbee7dc09ef7))

## [0.20.0](https://www.github.com/teletha/bee/compare/v0.19.0...v0.20.0) (2021-12-13)


### Features

* Drop automatic pom synchronization. ([558a3af](https://www.github.com/teletha/bee/commit/558a3af9a32716aa77195316c8e97f135534c881))
* Drop Project#getLibrary, use #asLibraray instead. ([4980760](https://www.github.com/teletha/bee/commit/4980760a3a2c6cb99aaafe98854618760892b599))


### Bug Fixes

* CI/CD action on github uses maven cache no longer. ([ab74135](https://www.github.com/teletha/bee/commit/ab74135930dcb824dec31574dbaa5cc4d4b9e7db))
* Remove pom. ([77af54d](https://www.github.com/teletha/bee/commit/77af54df0b5429cf491dfef5dff13907185b4daf))

## [0.19.0](https://www.github.com/teletha/bee/compare/v0.18.1...v0.19.0) (2021-12-12)


### Features

* Add env:local forces to use the local installed bee. ([9db13f7](https://www.github.com/teletha/bee/commit/9db13f709548e07c3ad313e672abe72d797a685b))
* Support multiple copyright. ([32953fd](https://www.github.com/teletha/bee/commit/32953fd851f0fe61b25a904b8e51e91ab9d627dd))


### Bug Fixes

* Bee project can use the latest version on its build process. ([c77d627](https://www.github.com/teletha/bee/commit/c77d6279bf02975aac4cb8ac42dae292f244b8dc))
* Enhance help:version info. ([ce56ac8](https://www.github.com/teletha/bee/commit/ce56ac8a92018e2ee701acd004d8f049b5ab530d))
* env:clean must delete the snapshot jar. ([8b48b72](https://www.github.com/teletha/bee/commit/8b48b729300fe139b98ce232e69b46379b0e0557))
* File related task throws NPE by null input. ([3f56313](https://www.github.com/teletha/bee/commit/3f56313663d8c65b28b9fee2b6f9a6655cd70b21))
* Illegal line separator. ([1ebba05](https://www.github.com/teletha/bee/commit/1ebba0572cf5f0289507463107090f1a481b55cb))
* Task#copyFile ensures that the input file exists. ([5c1db60](https://www.github.com/teletha/bee/commit/5c1db608b6bcedfaa4a2bbfa8cbf86c2e457ff13))
* Task#makeFile throws NPE by null input. ([5d48a96](https://www.github.com/teletha/bee/commit/5d48a96a003939c6978054bbcaa78185c3250c50))

### [0.18.1](https://www.github.com/teletha/bee/compare/v0.18.0...v0.18.1) (2021-12-09)


### Bug Fixes

* Repository#collectDependency includes the processing project. ([0ebc7fc](https://www.github.com/teletha/bee/commit/0ebc7fc87d3734126f43e8f0d7b0d4ad40f44ea2))

## [0.18.0](https://www.github.com/teletha/bee/compare/v0.17.3...v0.18.0) (2021-12-08)


### Features

* Add task [env:stable] and [env:latest]. ([909586c](https://www.github.com/teletha/bee/commit/909586c576cbf6b3d8b9da378367622291738a20))
* Add Task#checkFile. ([621ab82](https://www.github.com/teletha/bee/commit/621ab82a80a021a2c83216654ee27f64da587ed8))
* Github supports the creating license.txt ([d510a6f](https://www.github.com/teletha/bee/commit/d510a6fe71b10d91663efb0ca4640ba031c8219e))


### Bug Fixes

* Eclipse make the moduled classpath when the project is moduled. ([61c5367](https://www.github.com/teletha/bee/commit/61c536759a69e08b46970c35fe8dc1c8f012fa73))
* NPE when no license definition. ([47e46b1](https://www.github.com/teletha/bee/commit/47e46b1840dea12af72db6c673b2c64194634c2d))
* The [clean:all] task excludes built jar set. ([b528716](https://www.github.com/teletha/bee/commit/b528716fff6d7f56c16c2fc98c4269cff1f3eea8))
* Track deleting directory. ([09fa03f](https://www.github.com/teletha/bee/commit/09fa03f645994c3da73554f994266d896ae9f906))

### [0.17.3](https://www.github.com/teletha/bee/compare/v0.17.2...v0.17.3) (2021-12-04)


### Bug Fixes

* build.yml ([4d04313](https://www.github.com/teletha/bee/commit/4d04313c24b8a15b28bcfa1100c805b511e78c1e))
* update psychopath ([c3ade7f](https://www.github.com/teletha/bee/commit/c3ade7fe279a6fa12d0b073755e26e7a60d17b13))

### [0.17.2](https://www.github.com/teletha/bee/compare/v0.17.1...v0.17.2) (2021-12-03)


### Bug Fixes

* Invalid shell script. ([75765a6](https://www.github.com/teletha/bee/commit/75765a62cf3355853b82e20e44ed09f5f670e695))

### [0.17.1](https://www.github.com/teletha/bee/compare/v0.17.0...v0.17.1) (2021-12-03)


### Bug Fixes

* Add version sufix to donwloaded jar. ([90d17d0](https://www.github.com/teletha/bee/commit/90d17d081a86f21255845c70181dc8ffe3ef3f37))
* change build order ([c1ba4bc](https://www.github.com/teletha/bee/commit/c1ba4bca76f4423cf89089176e7b1c15c8823169))

## [0.17.0](https://www.github.com/teletha/bee/compare/v0.16.0...v0.17.0) (2021-12-03)


### Features

* Disabe trace message in eclipse platform. ([dfe8474](https://www.github.com/teletha/bee/commit/dfe84742485c76a6c78061480562466edb6ed7af))
* Merge CI/CD action. ([bce1ab7](https://www.github.com/teletha/bee/commit/bce1ab779cbf1dac33cd79277ab69d4b08a36c67))
* Throwing TaskCancel will skip the processing task. ([d0fd7bb](https://www.github.com/teletha/bee/commit/d0fd7bb5e8b471d5c0b1d2e9a48bbac081ce4ac2))


### Bug Fixes

* Format CUI message. ([e099830](https://www.github.com/teletha/bee/commit/e099830b3dc948394b6592aba5328243777be5a6))

## [0.16.0](https://www.github.com/teletha/bee/compare/v0.15.1...v0.16.0) (2021-11-30)


### Features

* Support module on eclipse task. ([6c15942](https://www.github.com/teletha/bee/commit/6c159420190d13de302e01101280a06e44c19757))


### Bug Fixes

* Change installed jar's name. ([8cd022b](https://www.github.com/teletha/bee/commit/8cd022b8d7c4fb85bbb3ad76e00e1be54535f769))
* Update maven model. ([0eefb76](https://www.github.com/teletha/bee/commit/0eefb76e4bc11e1903ea64696cb8f4ba5042b643))

### [0.15.1](https://www.github.com/teletha/bee/compare/v0.15.0...v0.15.1) (2021-11-19)


### Bug Fixes

* Enahance CI/CD process. ([aed8c77](https://www.github.com/teletha/bee/commit/aed8c770fcb4dbb011e1642b8750a48ba1567840))

## [0.15.0](https://www.github.com/teletha/bee/compare/v0.14.0...v0.15.0) (2021-11-19)


### Features

* Add clean task. ([a8633cb](https://www.github.com/teletha/bee/commit/a8633cb25fc6c5dcd8b00085f198a051057cff65))
* JavaCompiler generates the debug info by default. ([02d66a5](https://www.github.com/teletha/bee/commit/02d66a522ed3b84c06afbb337839e10268accd9a))


### Bug Fixes

* JavaCompiler API is chainable. ([5fdf0a9](https://www.github.com/teletha/bee/commit/5fdf0a9cef384bb008d9cb08d675054fc5f53e73))
* Optimize message on javac. ([2e02639](https://www.github.com/teletha/bee/commit/2e026394a314819a72c37db0eeefe07ce3abacec))

## [0.14.0](https://www.github.com/teletha/bee/compare/v0.13.0...v0.14.0) (2021-11-18)


### Features

* Drop Bee.DisableANSI option. ([afbae6b](https://www.github.com/teletha/bee/commit/afbae6b1357ea5ec5ed7559f2ece9beefd02f003))


### Bug Fixes

* BeeInstaller doesn't delete release versions. ([2d57954](https://www.github.com/teletha/bee/commit/2d579546b8db0dc3952499376fa2131a6cf8ff53))
* Compiling order in test task. ([4ae82bb](https://www.github.com/teletha/bee/commit/4ae82bb5f82e96d763babf86ecd88e36ac1eca27))
* Compiling order in test task. ([e11616d](https://www.github.com/teletha/bee/commit/e11616d449053d7005ed988d2e32c93498e5fcb0))
* Compiling order. ([163ddac](https://www.github.com/teletha/bee/commit/163ddacb9529a66f786d885aba73e9b131b20825))
* Detect the build environment automatically. ([afbae6b](https://www.github.com/teletha/bee/commit/afbae6b1357ea5ec5ed7559f2ece9beefd02f003))
* JitPack build process uses the specified Java version. ([1a9bee5](https://www.github.com/teletha/bee/commit/1a9bee547505aacba10fb9383b45aaf178f21acb))
* Platforma scans the normalized path. ([be8909a](https://www.github.com/teletha/bee/commit/be8909a27c0e126b2a18283bad3790d2a7f24d1a))
* Rename from Env#clear to #clean. ([7b901a6](https://www.github.com/teletha/bee/commit/7b901a6ea5c3c0bddf26f788e743dab80a3a4f3b))

## [0.13.0](https://www.github.com/teletha/bee/compare/v0.12.0...v0.13.0) (2021-11-14)


### Features

* Add Bee.DisableANSI option. ([40be54b](https://www.github.com/teletha/bee/commit/40be54b3fe17525f5ceeb8217022ebf899390bdc))


### Bug Fixes

* Disable ANSI escape code when JitPack building. ([46b95e7](https://www.github.com/teletha/bee/commit/46b95e794582b982c125cc3e9ed94e20acb25a67))
* Support maven repository detection on JitPack. ([22d71ef](https://www.github.com/teletha/bee/commit/22d71ef55181486d5fdd4738d6412166cda12d8f))

## [0.12.0](https://www.github.com/teletha/bee/compare/v0.11.0...v0.12.0) (2021-11-14)


### Features

* Add env task to generate the local bee environment. ([47ce617](https://www.github.com/teletha/bee/commit/47ce6177212f6027a0bdaef6410180169421a558))
* Bee#execute returns status code. ([e518e66](https://www.github.com/teletha/bee/commit/e518e66beee89be7ebd2651d80441ed49a7efdfd))
* Ci task provides jitpack command. ([e518e66](https://www.github.com/teletha/bee/commit/e518e66beee89be7ebd2651d80441ed49a7efdfd))

## [0.11.0](https://www.github.com/Teletha/bee/compare/v0.10.0...v0.11.0) (2021-11-10)


### Features

* Remove @Grab, use Require with lazy dependency resolution. ([6eb2936](https://www.github.com/Teletha/bee/commit/6eb2936e844d8d1f830c5702b80469e8370b06c2))
* Remove Task#require(String...) ([aae42b5](https://www.github.com/Teletha/bee/commit/aae42b51926cab1f2bfa3bf33100bd7f8a66b4d1))


### Bug Fixes

* Add all dependencies as Javadoc classpath. ([571cb34](https://www.github.com/Teletha/bee/commit/571cb34256e410a1acf4f1aa6feeb33097282641))
* Check duplication when dynamic class loading. ([796a4cc](https://www.github.com/Teletha/bee/commit/796a4cc4daea047baf7072755a16d20c58dfdb7f))
* CI can cache dependency. ([b67c01c](https://www.github.com/Teletha/bee/commit/b67c01c5fb2f84f1a8764264df6b9b78804dad29))
* doc:site indicates invalid sample directory ([71397b8](https://www.github.com/Teletha/bee/commit/71397b8d0c04eb72ffa77c292399edd213f25744))
* doc:site supports javadoc for JDK ([eb9d40d](https://www.github.com/Teletha/bee/commit/eb9d40de68aec811549b98ad43a71aeec6e581af))
* doc:site task supports sample and document ([3d27c16](https://www.github.com/Teletha/bee/commit/3d27c16c40ef154cf184873ca6ae79509efc9699))
* Don't execute IDE task when project initialization phase. ([5e307c8](https://www.github.com/Teletha/bee/commit/5e307c8960a286bcf964fd114069f24d28b59352))
* Format fail message. ([c5edd98](https://www.github.com/Teletha/bee/commit/c5edd9824280c30b3cb26a417a010e6e9626997a))
* Generate javadoc with project's dependency. ([88f9dc9](https://www.github.com/Teletha/bee/commit/88f9dc9d0017ca8e03b980d21d628cff7b10747d))
* Handles transitive dependencies more accurately. ([85483fe](https://www.github.com/Teletha/bee/commit/85483fe29092a2a5d0f7db796be7bb7370bf8cd5))
* Hide private class. ([181c632](https://www.github.com/Teletha/bee/commit/181c6321c294ff4e548327b0b6f8a72a63ddfb2f))
* Installer supports shell script. ([c0f8fee](https://www.github.com/Teletha/bee/commit/c0f8fee8eb52e1898f5708cb7cb805ebe3dd8de2))
* Jitpack requires the JVM for source version. ([a41e1d8](https://www.github.com/Teletha/bee/commit/a41e1d8ab7ef0d5318001a716b2345e73bdb08be))
* Repository#require resolves the specified library. ([eaa7b45](https://www.github.com/Teletha/bee/commit/eaa7b45bca8aa1f5dd9d5e538dcab08d95d7d616))
* update javadng ([4f8e10f](https://www.github.com/Teletha/bee/commit/4f8e10fa44f58cec3f20efd6f639aba75ab9a738))
* Update javadng. ([aacdc3f](https://www.github.com/Teletha/bee/commit/aacdc3f98517a48a2b43d3f8646e411b99b76564))
* Update javadng. ([77026e9](https://www.github.com/Teletha/bee/commit/77026e9c9a7c1140ced5440b0ed65b97f60ef097))
* Update javadng. ([a13bd23](https://www.github.com/Teletha/bee/commit/a13bd239905b16e365960793175f4c267d414b90))
* Update javadng. ([56c0e39](https://www.github.com/Teletha/bee/commit/56c0e393961bbc1e628c3ee28ca2a1e119d02f37))
* Update javadng. ([407cba0](https://www.github.com/Teletha/bee/commit/407cba050bc183b73530f9ef3ab139f19a2089ee))
* Update javadoc link. ([f86589a](https://www.github.com/Teletha/bee/commit/f86589aec1d265dde62a50f5f593c5148bebb418))

## [0.10.0](https://www.github.com/Teletha/bee/compare/v0.9.0...v0.10.0) (2021-10-28)


### Features

* Add Inputs#formatAsSize and #observerFor. ([a89471c](https://www.github.com/Teletha/bee/commit/a89471c6546f7af6995877bc62366afa9fd428c8))
* Add Inputs#formatAsSize(long, boolean). ([174a6af](https://www.github.com/Teletha/bee/commit/174a6af8f8b97e166c870f9f18d0d1b83b6663dc))
* Add UserInterface#trace and #debug. ([482e780](https://www.github.com/Teletha/bee/commit/482e7800a609eb3bc357e713d59c408dc8c998f8))
* Drop byte-buddy-agent, use BeeLoader instead. ([6550153](https://www.github.com/Teletha/bee/commit/6550153ec04f3292badef2ce4d6b8b0d41520432))
* Update lambok. ([9881c33](https://www.github.com/Teletha/bee/commit/9881c33a7680e86d7f9f97666af16ef0845682b2))


### Bug Fixes

* Colorize the console output. ([09fd2a3](https://www.github.com/Teletha/bee/commit/09fd2a3863e10271c5cf31939260afda6868f480))
* MemoizedTask is broken by user-customed task. ([45a40a7](https://www.github.com/Teletha/bee/commit/45a40a713de1ece266157dc63572b0d3ff48af9f))
* Rename from UserInterface#talk to #info. ([43bb082](https://www.github.com/Teletha/bee/commit/43bb08249665e0919e1d7c1b0d26a7213a52c3f6))
* UserInterface depends on CLI behavior. ([fc6b358](https://www.github.com/Teletha/bee/commit/fc6b3586b20fc384a4ffe5ac7b164f53a09040ea))

## [0.9.0](https://www.github.com/Teletha/bee/compare/v0.8.0...v0.9.0) (2021-10-25)


### Features

* Add test report on CI. ([81955ee](https://www.github.com/Teletha/bee/commit/81955ee5de5b6c1c33bd50a8045ded1f474c37b7))
* Jar task support minify of class files. ([a79cb46](https://www.github.com/Teletha/bee/commit/a79cb46ed37c0433302b63ce6e1caf64c9fb14c7))
* Support annotation processor on maven. ([7053921](https://www.github.com/Teletha/bee/commit/7053921fcfb2f632570d5d13c78be51893e1dab7))
* Update junit. ([adb7924](https://www.github.com/Teletha/bee/commit/adb7924a335eca01b3539e1870ddfe2764294022))
* Update maven resolver. ([06442bf](https://www.github.com/Teletha/bee/commit/06442bf2d777be49838c1aae40ca9b395e469465))
* Update psychopath. ([25dac78](https://www.github.com/Teletha/bee/commit/25dac78c967e95508842a4a1e8a61bcc59ffc953))


### Bug Fixes

* Compacting log. ([7a895ac](https://www.github.com/Teletha/bee/commit/7a895ac9f6056fa7b5e860a4090bd2e505b3c8d7))
* Format test result output on console. ([e39e628](https://www.github.com/Teletha/bee/commit/e39e628ee6fd622e5595a8f4ec13d22673a0a8fd))
* Integrate CI and Git task. ([4e6b32c](https://www.github.com/Teletha/bee/commit/4e6b32c123188d41c165fd621ee9d018380a486b))
* Jar task shows the detailed compress log. ([fa9f77f](https://www.github.com/Teletha/bee/commit/fa9f77f968c4cbcf313b2cb8358738341cb40d09))
* Reconfigure various components. ([3fc6637](https://www.github.com/Teletha/bee/commit/3fc6637bb3281867ef4f3ac67bc01bd0ce78adc8))
* Remove enable-preview option. ([98e9036](https://www.github.com/Teletha/bee/commit/98e9036435236d33f720e912007b3136c96e4f08))
* Test ignores the failed test. ([ca5caa9](https://www.github.com/Teletha/bee/commit/ca5caa99b93c65c4e4fca41a7186d0446b1e29b4))
* Test reporter is not thread-safe. ([c5ac0b6](https://www.github.com/Teletha/bee/commit/c5ac0b671c760398dd55c1eef151b04a5da371d3))
* Test task can detect the existence of test class. ([454f24c](https://www.github.com/Teletha/bee/commit/454f24c4a2512921bf3e12ec2a6dad0d0d528731))
* Update CI process. ([587f31c](https://www.github.com/Teletha/bee/commit/587f31c7c874e992f4c94238f0bc67cc719618c7))
* Update maven resolver. ([fc2df5b](https://www.github.com/Teletha/bee/commit/fc2df5b6fec7f507d5c9fdd2eaee3fe406a7bca5))
* Update maven-resolver. ([3b48c40](https://www.github.com/Teletha/bee/commit/3b48c405a2ff61acda9bb67972ff0dd9eb82b2f4))
* Update version. ([33a2f74](https://www.github.com/Teletha/bee/commit/33a2f7409442661a51b1e130905e6cea85974762))

## [0.8.0](https://www.github.com/Teletha/bee/compare/v0.7.0...v0.8.0) (2021-03-28)


### Features

* Add check command on compile task. ([dec2637](https://www.github.com/Teletha/bee/commit/dec2637e8fdda9188570ae747ec3146b99ae7935))
* Add CI/CD task (for github). ([148d146](https://www.github.com/Teletha/bee/commit/148d146bfc82e856733cada9c6dadb5899101a99))
* Enable CI on github. ([06354df](https://www.github.com/Teletha/bee/commit/06354df76b4d92e111bf583971e7933fc8899668))
* IDE task builds pom file synchronously. ([7537381](https://www.github.com/Teletha/bee/commit/753738134e9184f8fdf8dd7069001f2dbc80d653))
* Jar task supports Java16 on version modification. ([d1ce3ea](https://www.github.com/Teletha/bee/commit/d1ce3ea8ca2e4c91411c3bc320aaff0eff64b5ee))
* Provide version control system model. ([adea252](https://www.github.com/Teletha/bee/commit/adea252a1454feaa78f7c3fcc756486c3801684f))
* Terminates all tasks when a test fails. ([e4910a9](https://www.github.com/Teletha/bee/commit/e4910a9dbc1b9bd52e923c2333ac745b63b8d1b8))
* The dependency Grab annotation like Groovy. ([6280a12](https://www.github.com/Teletha/bee/commit/6280a123eba3333dc8ae7d64b461f89d79ea8aa0))
* Use release option on maven compiler plugin. ([7ef2a0d](https://www.github.com/Teletha/bee/commit/7ef2a0d24e9e689cbbea38b0086f48515c7003e5))
* When executing any command, if the project definition has been ([d820b49](https://www.github.com/Teletha/bee/commit/d820b49a9660f50253ee29d0608bc62cdfefe196))


### Bug Fixes

* Build environment restricts the upper bound of java version. ([38cc547](https://www.github.com/Teletha/bee/commit/38cc547b966a961ca6c788f30471112d3b2fa979))
* CI/CD task support JitPack. ([99ff1ce](https://www.github.com/Teletha/bee/commit/99ff1ce1ea02a43987e16a0551185061a5632998))
* Compile at java 11. ([e0b6a8b](https://www.github.com/Teletha/bee/commit/e0b6a8b69a1915b71867a1459248c95b5c253cc1))
* Dependency resolution fails when library is referred from compile ([520b6f7](https://www.github.com/Teletha/bee/commit/520b6f72935099bf3dc04cc48a24ac5f999488f4))
* Disable logging of Junit. ([e148573](https://www.github.com/Teletha/bee/commit/e14857382089965cfdef35c7b2d196376945f024))
* Drop preview support. ([b95f3b0](https://www.github.com/Teletha/bee/commit/b95f3b05565fa991ce017ecc04b8532478cac678))
* Inputs#ref trim its contents. ([86e3415](https://www.github.com/Teletha/bee/commit/86e3415b481c6fbc9e205538c6562ed65a1ae1e9))
* Integrate Inputs and DebugHelper. ([eabe63e](https://www.github.com/Teletha/bee/commit/eabe63ed53236a916d88d3859469166b9410ba5e))
* Jar file is ignored. ([7aef28e](https://www.github.com/Teletha/bee/commit/7aef28e89006ee2315fd598907d056b7eb39f3fa))
* Licesen is class now and remove StandardLicense. ([3d91a52](https://www.github.com/Teletha/bee/commit/3d91a524f89d6b7f1ceca2afba78fd8ebfe939ef))
* Make code compilable by javac. ([5905a48](https://www.github.com/Teletha/bee/commit/5905a48e7ea21b5d9433ceab1b77bd2028e64fe2))
* NotationTest fails on non-windows platform. ([fd20177](https://www.github.com/Teletha/bee/commit/fd2017740f682c0344ca620c421341d38d7d53ea))
* Overridden commands in subclasses were not being recognized. ([d820b49](https://www.github.com/Teletha/bee/commit/d820b49a9660f50253ee29d0608bc62cdfefe196))
* POM uses the ranged dependency version. ([92faf58](https://www.github.com/Teletha/bee/commit/92faf5804ba6c6d8aedd21032299ada717b3dec6))
* Public bee related projects should refer the version.txt. ([2190d36](https://www.github.com/Teletha/bee/commit/2190d36ea4aa0978350002ecafb87fb9c559c9a0))
* Remove PGP. ([11c50ff](https://www.github.com/Teletha/bee/commit/11c50ff9751720066d916f4bd409e4c3e6cbeb4e))
* Remove Task#readResource, use text-block instead. ([06354df](https://www.github.com/Teletha/bee/commit/06354df76b4d92e111bf583971e7933fc8899668))
* Remove unused class. ([148d146](https://www.github.com/Teletha/bee/commit/148d146bfc82e856733cada9c6dadb5899101a99))
* Simplify a failure message. ([dc107c8](https://www.github.com/Teletha/bee/commit/dc107c86940367ae4a7528090714f107364493d4))
* The bee related project' version is corrupted. ([7537381](https://www.github.com/Teletha/bee/commit/753738134e9184f8fdf8dd7069001f2dbc80d653))
* Update pom. ([b99df4e](https://www.github.com/Teletha/bee/commit/b99df4e0bcaace3c9a2e974179010d65c601e015))
* Use java15 on memoized task. ([8402e86](https://www.github.com/Teletha/bee/commit/8402e86fb8fe2f73d442094b6ac170b97921bff0))

## [0.7.0](https://www.github.com/Teletha/bee/compare/0.1.8...v0.7.0) (2021-03-23)


### Features

* Compiler target version of test classes is configurable. ([1735bc3](https://www.github.com/Teletha/bee/commit/1735bc3fdda996934f4d8225a6f981cf21b61d8d))
* Enable release-please-action when you use github. ([e392965](https://www.github.com/Teletha/bee/commit/e392965177691dc2d939f2c5dcb98b055c85698a))
* Enable release-please-action when you use github. ([a75ff91](https://www.github.com/Teletha/bee/commit/a75ff91b0cd98243d6ab3ef2de0e469975819b96))
* Inputs#ref retrieve the file's live contents. ([9f23e93](https://www.github.com/Teletha/bee/commit/9f23e93a1f631d2285e1ab892f5ed6305c13aadd))
* Project#product accepts CharSequence. ([4a61c3a](https://www.github.com/Teletha/bee/commit/4a61c3ac5664c38c2cb385a4dca052fbdc9c27bb))


### Bug Fixes

* Enabel in offline mode. ([979f13b](https://www.github.com/Teletha/bee/commit/979f13b8e861fbbb4e0a25679955de5c03658f3a))
* Exe task is broken. ([f1d7c60](https://www.github.com/Teletha/bee/commit/f1d7c60da29bb3c2764368e86354a0309d7d9119))
* FindMain throws StackOverFlowError. ([fbf65bb](https://www.github.com/Teletha/bee/commit/fbf65bb1d88ee31bea8497487d80c442fd1d56c8))
* Library can recognize classifier. ([525d318](https://www.github.com/Teletha/bee/commit/525d318babfa09e737b55d15c1be790d91f116e8))
* Load same package twice. ([7dd09ce](https://www.github.com/Teletha/bee/commit/7dd09ce8a0c5c7cab06d06fcf312a3c1eb5c6f53))
* Memoized task is defined duplicately at parallel task execution. ([bd1a83a](https://www.github.com/Teletha/bee/commit/bd1a83a90401e6d81982e0264dbb88bc16a246be))
* Process is broken. ([24fd067](https://www.github.com/Teletha/bee/commit/24fd0675beacc1490c4e7ac3a55134568b62395b))
* Release-please-action should execution on main brunch. ([bd1a83a](https://www.github.com/Teletha/bee/commit/bd1a83a90401e6d81982e0264dbb88bc16a246be))
* Task#readResource can't read resource in jar. ([d99ed7c](https://www.github.com/Teletha/bee/commit/d99ed7cc642be00eed54ed61a1602d12f77f700a))
* Task#readResource fails by relative path. ([bd1a83a](https://www.github.com/Teletha/bee/commit/bd1a83a90401e6d81982e0264dbb88bc16a246be))
* Test scope accepts compile scope. ([77978bf](https://www.github.com/Teletha/bee/commit/77978bfd35ada2eff627f722d4f4f1550f13b9d5))
* Use ASM directly instead of byte-buddy. ([2a88f0b](https://www.github.com/Teletha/bee/commit/2a88f0b2dbebf94781aaab1caaaf5d79de5048c4))
