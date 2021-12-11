@echo off
setlocal enabledelayedexpansion
set "bee=bee.bin"
if not exist %bee% (
   set "bee=%JAVA_HOME%/lib/bee/bee-latest.jar"
    if not exist !bee! (
        echo bee is not found locally, try to download it from network.
        curl -#L -o $bee https://jitpack.io/com/github/teletha/bee/latest/bee-latest.jar
    )
)
java -javaagent:%bee% -cp %bee% bee.Bee %*
