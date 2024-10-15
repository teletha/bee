@echo off
setlocal enabledelayedexpansion
set "version=0.56.6"
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
java -javaagent:%bee% -cp %bee% bee.Bee %*