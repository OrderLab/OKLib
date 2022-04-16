#run this scripts on all nodes
#this script is hardcoded, need to be paramerized
cd $2
bin/zkServer.sh stop
rm -rf /tmp/zookeeper/version-2
sleep 2

bin/zkServer.sh start
sleep 2
bin/zkServer.sh status

