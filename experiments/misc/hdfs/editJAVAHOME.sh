cd ~/hadoop
version=$(perl -ne 'print and last if s/.*<version>(.*)<\/version>.*/\1/;' < pom.xml)
sed -i 's/# export JAVA_HOME=/export JAVA_HOME=\/usr\/lib\/jvm\/java-1.8.0-openjdk-amd64\//g' hadoop-dist/target/hadoop-${version}/etc/hadoop/hadoop-env.sh