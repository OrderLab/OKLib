#run this scripts on all nodes
cd $2
version=$(perl -ne 'print and last if s/.*<version>(.*)<\/version>.*/\1/;' < pom.xml)

cd hadoop-dist/target/hadoop-${version}/
sbin/hadoop-daemons.sh start journalnode
#$3 is hostname of active namenode
if [ "$(hostname -s)" = $3 ]; then
  bin/hdfs namenode -format orderlab
  sleep 3
  $1/experiments/run/hdfs/start.sh $1 $2
  bin/hdfs haadmin -getAllServiceState
  bin/hdfs haadmin -transitionToStandby nn1
  bin/hdfs haadmin -transitionToActive nn2
  bin/hdfs haadmin -getAllServiceState
  touch ~/fault.HDFS-14317
  echo "checkout logs, if you see" $3 "stops generating "
  echo "  INFO org.apache.hadoop.hdfs.server.namenode.ha.EditLogTailer: Triggering log roll on remote NameNode"
  echo "that means reproducing succeed (you should probably check after more than 2min)"
#$4 is hostname of standby namenode
elif [ "$(hostname -s)" = $4 ]; then
  bin/hdfs namenode -format orderlab
fi



