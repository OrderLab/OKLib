#!/bin/bash

if [ $# -lt 2 ]; then
    echo "Usage: $0 [OathKeeper folder] [system folder]"
    exit 1
fi

set -x

rm -rf /tmp/hdfs-14514-repro
mkdir -p /tmp/hdfs-14514-repro

cd $2
version=$(perl -ne 'print and last if s/.*<version>(.*)<\/version>.*/\1/;' < pom.xml)

dist=$2/hadoop-dist/target/hadoop-${version}
cd $dist

yes | bin/hdfs namenode -format
sleep 3
sbin/start-dfs.sh
sleep 3
sbin/kms.sh start
sleep 3

bin/hadoop key create reprokey
bin/hdfs dfs -mkdir /dataenc
bin/hdfs crypto -createZone -keyName reprokey -path /dataenc
bin/hdfs dfsadmin -allowSnapshot /dataenc

pushd $1/experiments/reproduce/HDFS-14514

classpath="$dist/etc/hadoop:$dist/share/hadoop/common/lib/*:$dist/share/hadoop/common/*:$dist/share/hadoop/hdfs:$dist/share/hadoop/hdfs/lib/*:."

javac -classpath $classpath Repro.java && java -classpath $classpath Repro

# to cleanup, run: bin/hdfs dfs -deleteSnapshot /dataenc snap1; bin/hdfs dfs -rm /dataenc/a.txt

popd

sbin/stop-dfs.sh
sleep 3
sbin/kms.sh stop
sleep 3
jps
