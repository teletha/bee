@goto(){
  # This part is interpreted as a shell script in Linux.
  # To run this script remotely type this in your shell
  #
  # curl -Ls https://git.io/install-bee | bash
  #
  version=$(curl -SsL https://git.io/stable-bee)
  curl -#L -o bee-${version}.jar https://jitpack.io/com/github/teletha/bee/${version}/bee-${version}.jar
  java -javaagent:bee-${version}.jar -cp bee-${version}.jar bee.BeeInstaller
}

@goto $@
exit

:(){
@echo off
:: This part is interpreted as a bat script in Windows.
:: To run this script remotely type this in your shell
::
:: curl -Ls https://git.io/install-bee -o install.bat && install
::
for /f "delims=" %%i in ('curl -SsL https://git.io/stable-bee') do set version=%%i
curl -#L -o bee-%version%.jar https://jitpack.io/com/github/teletha/bee/%version%/bee-%version%.jar
java -javaagent:bee-%version%.jar -cp bee-%version%.jar bee.BeeInstaller

:: delete myself
start /b "" cmd /c del "%~f0"