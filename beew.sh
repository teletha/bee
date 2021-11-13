#!bin/bash

bee=$JAVA_HOME/lib/bee-0.10.0.jar

if [ ! -d $bee ]; then
	curl -sL -o %bee% https://github.com/Teletha/bee/blob/master/bee-0.10.0.jar?raw=true 
fi

java -Dfile.encoding=UTF-8 -javaagent:%bee% -cp %bee% bee.Bee %*