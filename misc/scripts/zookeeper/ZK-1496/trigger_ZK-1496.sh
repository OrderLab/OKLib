#run this on single node
cd $2
bin/zkServer.sh stop

CONFFILE=$2/conf/zoo.cfg
if test -f "$CONFFILE"; then
    echo "$CONFFILE exists."
else
    cp $2/conf/zoo_sample.cfg $2/conf/zoo.cfg
    echo "4lw.commands.whitelist=*" >> $2/conf/zoo.cfg
fi

#rm -rf ~/fuser/zookeeper/version-2
sleep 2

bin/zkServer.sh start
sleep 2
bin/zkServer.sh status

$1/misc/scripts/zookeeper/ZK-1496/repro_ZK-1496.sh $1 $2



