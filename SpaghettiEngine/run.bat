@ECHO OFF

echo RUNNING..
cd target
dir /s/b *.jar > name.txt
set /p JARNAME=<name.txt
java -jar %JARNAME%
del name.txt
echo DONE
cd ..
