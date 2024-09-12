@echo off
for /f "delims=" %%i in ('curl -SsL https://git.io/stable-bee') do set version=%%i

curl -#L -o bee-%version%.jar https://jitpack.io/com/github/teletha/bee/%version%/bee-%version%.jar
java -javaagent:bee-%version%.jar -cp bee-%version%.jar bee.BeeInstaller
