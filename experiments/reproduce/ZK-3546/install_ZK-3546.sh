#run this scripts on all nodes
cd $2
./run_engine.sh clean conf/samples/zk-3.6.1.properties
git checkout 26aee2228451257f3b0b5093bc0c101822e06bc8
git apply $1/experiments/reproduce/ZK-3546/hook_ZK-3546.patch
ant clean compile

