#!/bin/bash

if [ $# -lt 2 ]; then
    echo "Usage: $0 [OathKeeper folder] [system folder]"
    exit 1
fi

set -x

cd $1
./run_engine.sh clean conf/samples/hdfs-3.2.1.properties
cd $2
# parent of the fix commit cecba551aa3b03142830cfd58f548cb6b125ee7b
git checkout -f e3d4f6bfed493d6c2bf574fd0a7f104442f62218

# For native and for C API
# fix openssl breaking change
#git cherry-pick --no-commit 94ca52ae9ec0ae04854d726bf2ac1bc457b96a9c
# no longer used, we now use Java client API

# build
mvn clean package -Pdist -DskipTests -Dmaven.javadoc.skip=true -Dtar
#mvn package -Pdist -DskipTests -Dmaven.javadoc.skip=true -Dtar

# install configs
version=$(perl -ne 'print and last if s/.*<version>(.*)<\/version>.*/\1/;' < pom.xml)
cp $1/experiments/reproduce/HDFS-14514/core-site.xml hadoop-dist/target/hadoop-${version}/etc/hadoop/
cp $1/experiments/reproduce/HDFS-14514/hdfs-site.xml hadoop-dist/target/hadoop-${version}/etc/hadoop/
cp $1/experiments/reproduce/HDFS-14514/kms-site.xml hadoop-dist/target/hadoop-${version}/etc/hadoop/

# change JAVA_HOME
sed -i 's/^export JAVA_HOME=.*$/export JAVA_HOME=\/usr\/lib\/jvm\/java-1.8.0-openjdk-amd64\//g' hadoop-dist/target/hadoop-${version}/etc/hadoop/hadoop-env.sh
