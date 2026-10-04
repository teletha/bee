@echo off
setlocal enabledelayedexpansion
set "version=0.80.0"
set "bee=bee-%version%.far"

if not exist !bee! (
    if not "!JAVA_HOME!" == "" (
        set "bee=!JAVA_HOME!/lib/bee/bee-%version%.jar"
    ) else (
        for /f "delims=" %%i in ('where java') do (
            set "javaDir=%%~dpi"
            set "bee=!javaDir!/../lib/bee/bee-%version%.jar"
        )
    )

    if not exist !bee! (
        echo bee is not found locally, try to download it from network.
        curl -#L -o !bee! --create-dirs https://jitpack.io/com/github/teletha/bee/%version%/bee-%version%.jar
    )
)
java -XX:+TieredCompilation -XX:TieredStopAtLevel=1 -XX:AOTCache=bee.aot -XX:+IgnoreUnrecognizedVMOptions -cp %bee% bee.Bee %*
