#!/usr/bin/env bash
#just to trigger
bin/hdfs dfs -mkdir /zz
bin/hdfs dfsadmin -shutdownDatanode $5:9867
bin/hdfs dfsadmin -shutdownDatanode $6:9867
echo "sleep 60 seconds"
sleep 60
bin/hdfs dfsadmin -report
bin/hdfs dfs -mkdir /test
bin/hdfs ec -enablePolicy -policy XOR-2-1-1024k
bin/hdfs ec -setPolicy -path /test -policy XOR-2-1-1024k
bin/hdfs dfs -copyFromLocal share/hadoop/hdfs/hadoop-hdfs-3.3.0-SNAPSHOT.jar /test/foo
bin/hdfs fsck /test -files -blocks -replicaDetails
#echo "hurry! go to razor18 and restart datanode by:  bin/hdfs --daemon start datanode"
ssh $5 "cd $3/hadoop-dist/target/hadoop-$1/ && bin/hdfs --daemon start datanode"
sleep 10
bin/hdfs dfsadmin -shutdownDatanode $4:9867
echo "sleep for 300 sec"
sleep 300
bin/hdfs fsck /test -files -blocks -replicaDetails
echo "you should see live blocks only have two, while other one is redundant"
