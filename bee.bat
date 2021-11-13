@echo off

SET bee="%JAVA_HOME%\lib\bee\bee-0.11.0.jar"

if not exist %bee% (
	curl -sL -o %bee% https://github.com/Teletha/bee/blob/master/bee-0.10.0.jar?raw=true 
)

java -Dfile.encoding=UTF-8 -javaagent:%bee% -cp %bee% bee.Bee %*
