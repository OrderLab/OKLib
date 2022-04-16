#run this scripts on namenode 
HP_HOME () {
  version=$(perl -ne 'print and last if s/.*<version>(.*)<\/version>.*/\1/;' < $1/pom.xml)
  cd $1/hadoop-dist/target/hadoop-${version}
}
HP_HOME $2
bin/hdfs namenode -format orderlab
sbin/start-dfs.sh
echo "sleep 10 sec to wait for instances start up"
sleep 10
bin/hadoop org.apache.hadoop.hdfs.NNBenchWithoutMR -operation createWrite -baseDir /benchmarks -numFiles 100 -blocksPerFile 160 -bytesPerBlock 1048576
