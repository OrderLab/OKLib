#run this scripts on all nodes
cd $2 
bin/zkServer.sh stop
rm -rf /tmp/zookeeper/version-2
sleep 2

bin/zkServer.sh start
sleep 2
bin/zkServer.sh status

if [ "$(hostname -s)" = $3 ]; then
    cd $4
    ./zkbench -conf bench_perf.conf
fi

