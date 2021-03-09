@echo off

echo RUNNING..
cd target
dir /s/b *.jar > name.txt
set jar_name=<name.txt
del name.txt
java -jar %jar_name%
echo DONE
