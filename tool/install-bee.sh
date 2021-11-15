# To run this script remotely type this in your shell
# 		curl -Ls https://git.io/install-bee | bash
curl -#L -o bee.jar https://jitpack.io/com/github/teletha/bee/0.13.0/bee-0.13.0.jar
java -javaagent:bee.jar -cp bee.jar bee.BeeInstaller
