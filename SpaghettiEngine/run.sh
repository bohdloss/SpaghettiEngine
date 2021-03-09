echo RUNNING..
cd target
jar_name="$(find . -name "*.jar")"
chmod +x $jar_name
java -jar "$jar_name"
echo DONE
