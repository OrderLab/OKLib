#run this scripts on all nodes 
#this script is hardcoded, need to be paramerized
HP_HOME () {
  version=$(perl -ne 'print and last if s/.*<version>(.*)<\/version>.*/\1/;' < $1/pom.xml)
  cd $1/hadoop-dist/target/hadoop-${version}
}

if [ "$(hostname -s)" = $3 ]; then
  HP_HOME $2
  sbin/stop-dfs.sh
  kill -9 `jps | grep "JournalNode" | cut -d " " -f 1`
else
  kill -9 `jps | grep "JournalNode" | cut -d " " -f 1`
  sleep 10

fi

kill -9 `jps | grep "MainWrapper" | cut -d " " -f 1`
kill -9 `jps | grep "DataNode" | cut -d " " -f 1`
kill -9 `jps | grep "NameNode" | cut -d " " -f 1`
kill -9 `jps | grep "SecondaryNameNode" | cut -d " " -f 1`

rm -rf /tmp/hdfs/
mkdir -p /tmp/hdfs/vol1/nn
mkdir -p /tmp/hdfs/vol1/dn
mkdir -p /tmp/hdfs/vol2/nn
mkdir -p /tmp/hdfs/vol2/dn
mkdir -p /tmp/hdfs/vol3/nn
mkdir -p /tmp/hdfs/vol3/dn
chmod 755 /tmp/hdfs/vol1/nn
chmod 755 /tmp/hdfs/vol1/dn
chmod 755 /tmp/hdfs/vol2/nn
chmod 755 /tmp/hdfs/vol2/dn
chmod 755 /tmp/hdfs/vol3/nn
chmod 755 /tmp/hdfs/vol3/dn

cd ~
rm fault.HDFS-*
