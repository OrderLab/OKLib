#run this scripts on all nodes
cd $2
bin/zkServer.sh stop
rm -rf /tmp/zookeeper/version-2
sleep 2

bin/zkServer.sh start
sleep 2
bin/zkServer.sh status

$1/experiments/reproduce/ZK-3546/ZK-3546.sh $3

