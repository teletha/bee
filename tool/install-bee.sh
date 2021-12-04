# To run this script remotely type this in your shell
# 		curl -Ls https://git.io/install-bee | bash
version=$(curl -SsL https://git.io/stable-bee)
curl -#L -o bee-${version}.jar https://jitpack.io/com/github/teletha/bee/${version}/bee-${version}.jar
java -javaagent:bee-${version}.jar -cp bee-${version}.jar bee.BeeInstaller
