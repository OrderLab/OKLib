
# Detailed instructions for reproducing

This README is for Artifact Evaluation only, which describes detailed
instructions to reproduce experiments we describe in the paper. Before you
start this part, we assume you already went through the procedures in the main
README file.

It is required that you have a 5-node cluster to reproduce these experiments.
The hardware and software dependencies remain same as we described in the main
`README.md` file. 

This is a project about distributed systems, thus the following steps can be 
tedious. We appreciate your patience.

Table of Contents
=================
* [0. Install and configure dependencies](#0-install-and-configure-dependencies)
   * [Install and test tmux  (~5 min)](#install-and-test-tmux--5-min)
   * [Install other dependencies (~3min)](#install-other-dependencies-3min)
* [1. Clone ZooKeeper, HDFS, and modify config  (~10 min)](#1-clone-zookeeper-hdfs-and-modify-config--10-min)
   * [Compile OathKeeper (~1 min)](#compile-oathkeeper-1-min)
   * [Compile Protobuf for HDFS and Add Environment Variable (~5min)](#compile-protobuf-for-hdfs-and-add-environment-variable-5min)
* [2. Reproduce Sec 9.1 Generation Overview &amp;&amp; Sec 9.3 Performance (Offline)](#2-reproduce-sec-91-generation-overview--sec-93-performance-offline)
   * [Modify config](#modify-config)
   * [Execute (~15 h)](#execute-15-h)
* [3. Reproduce Sec 9.2 Checking Newer Violations](#3-reproduce-sec-92-checking-newer-violations)
   * [3.1 Detect new ones (Online)](#31-detect-new-ones-online)
      * [ZK-1496](#zk-1496)
      * [ZK-1667](#zk-1667)
      * [ZK-3546](#zk-3546)
      * [HDFS-14699](#hdfs-14699)
      * [HDFS-14633](#hdfs-14633)
      * [HDFS-14317](#hdfs-14317)
   * [3.2 Crosscheck experiment (ZooKeeper only, Offline)](#32-crosscheck-experiment-zookeeper-only-offline)
      * [Modify config](#modify-config-1)
      * [Generate traces (~4 h)](#generate-traces-4-h)
      * [Run comparison (~30 min)](#run-comparison-30-min)
* [4. Reproduce Sec 9.4 Runtime Overhead &amp;&amp; 9.5 Rule Activation and False Positive (Online)](#4-reproduce-sec-94-runtime-overhead--95-rule-activation-and-false-positive-online)
   * [4.1 ZooKeeper](#41-zookeeper)
      * [Install benchmark and configure (~1 min)](#install-benchmark-and-configure-1-min)
      * [Checkout Zookeeper version and compile (~3 min)](#checkout-zookeeper-version-and-compile-3-min)
      * [Install runtime and rules (~1 min)](#install-runtime-and-rules-1-min)
      * [Start workload benchmark (~3 min)](#start-workload-benchmark-3-min)
      * [Collect results](#collect-results)
      * [Clean up](#clean-up)
      * [Repeat for baseline result](#repeat-for-baseline-result)
   * [4.2 HDFS](#42-hdfs)
      * [Checkout and compile (~2 min)](#checkout-and-compile-2-min)
      * [Install runtime and rules (~1 min)](#install-runtime-and-rules-1-min-1)
      * [Start workload benchmark (~3 min)](#start-workload-benchmark-3-min-1)
      * [Clean up (~1 min)](#clean-up-1-min)
      * [Repeat for baseline result](#repeat-for-baseline-result-1)

# 0. Install and configure dependencies 

## Install and test tmux  (~5 min)

We strongly recommend using tmux (a terminal multiplexer) to perform operations and monitor output in parallel. (Another alternative is to use pssh to broadcast commands to all machines) We demonstrate examples with tmux.

We assume the hostnames of 5 nodes are NODE1, NODE2, NODE3, NODE4 and NODE5. Please replace the hostnames in the commands in the following sections with yours.

on NODE1:

```bash
sudo apt-get update
sudo apt install tmux
wget https://gist.githubusercontent.com/mcfatealan/8e6df9ea87981406523e3b9470cf10b3/raw/1a2678d22f762dbb58ef7cf1cb855e6b3102bde9/smux.py
wget https://gist.githubusercontent.com/mcfatealan/7ff5bfe55a5facb035ccafc7a43be846/raw/37f254c2f2087a94d64a9a2e0bf24b3b39acf219/tmux_input
```

Edit the downloaded `tmux_input` file and replace all hostnames with your own cluster hostnames. For example:
```
#tmux_input
ssh NODE1 # should be your own hostname
```

Make sure you can ssh to other nodes from NODE1.

Then run
```
python smux.py tmux_input
```

If success, you should be able to see screen like this:

![image](https://i.imgur.com/gKJ8yS8.png)

Then you can press `ctrl+b` and type `:set synchronize-panes on` and press enter. Now your input is in parallel (to exit this mode, change `on` to be `off`). 

> :checkered_flag: Unless specified otherwise, all commands in the following sections need to execute across all nodes. Also we suggest at this step you make sure all nodes can ssh to each other which will be later used to execute remote scripts.


## Install other dependencies (~3min)

Running the experiments require additional dependencies (golang-go for zk benchmark):

```bash
sudo apt-get update
sudo apt-get -y git maven ant vim openjdk-8-jdk golang-go gnuplot
sudo update-alternatives --set java $(sudo update-alternatives --list java | grep "java-8")
```

# 1. Clone ZooKeeper, HDFS, and modify config  (~10 min)

Here we essentially repeat the first three steps in `Functional` guide but
instead on a cluster of nodes. Also we extend evaluation to another system
HDFS.

```bash
git clone git@github.com:apache/zookeeper.git
git clone git@github.com:apache/hadoop.git
```

Modify `conf/samples/zk-3.6.1.properties` and change this line to be the absolute path to Zookeeper root dir, for example

```ini
system_dir_path=/home/chang/zookeeper/
```

Modify `conf/samples/hdfs-3.2.1.properties` and change this line to be the absolute path to hadoop root dir, for example

```ini
system_dir_path=/home/chang/hadoop/
```

## Compile OathKeeper (~1 min)

```bash
cd OathKeeper && mvn package
```

## Compile Protobuf for HDFS and Add Environment Variable (~5min)

```
cd hadoop
wget https://gist.githubusercontent.com/mcfatealan/e450b26d60390b353f86fd465c120096/raw/855f78279100d50104e38aafebd682c2f3aa9a1e/build_protoc.sh && sudo chmod 755 ./build_protoc.sh && ./build_protoc.sh
```

add to .bashrc
```
export PROTOC_HOME=[PATH_TO_HADOOP]/protoc/2.5.0
export HADOOP_PROTOC_PATH=$PROTOC_HOME/dist/bin/protoc
export PATH=$PROTOC_HOME/dist/bin/:$PATH
```


```bash
source ~/.bashrc
```

This is important step for evaluating on HDFS. If later you see compilation errors `protoc not found` when compiling HDFS, it is usually due to this is not set properly. 

# 2. Reproduce Sec 9.1 Generation Overview && Sec 9.3 Performance (Offline)

This experiment reproduces the trace generation, rule inference and verification phase on 8 cases of Zookeeper and 10 cases of HDFS. In paper we list the total rule number (Sec 9.1) and execution time (Sec 9.3). Note that since all three phases are non-deterministic execution, the absolute numbers may vary upon each execution. The execution time depends on hardware performance.

## Modify config

For ZooKeeper, modify `conf/samples/zk-3.6.1.properties` 

```ini
ticket_collection_path=${ok_dir}/conf/samples/zk-collections-basic
```

For HDFS, modify `conf/samples/hdfs-3.2.1.properties` 

```ini
ticket_collection_path=${ok_dir}/conf/samples/hdfs-collections-basic
```

## Execute (~15 h)

```bash
nohup bash ./run_engine.sh runall_foreach conf/samples/zk-3.6.1.properties &> ./log_zk && bash ./run_engine.sh runall_foreach conf/samples/hdfs-3.2.1.properties  &> ./log_hdfs &
```

We suggest using `nohup` to run as background process as this takes significant long time. If target system compilation fails, try restarting the execution commands.

One way to speed up is, to divide the files in `${ok_dir}/conf/samples/zk-collections-basic` and `${ok_dir}/conf/samples/hdfs-collections-basic` in each node. For example, instead of executing for 18 cases on each, keep a few tickets on one node and delete all others. After the execution is over, merge all `inv_verify_output` in all nodes. 

This procedure is very computation-intensive and memory-intensive. If the memory resource is limited, it can take very long time due to heavy GC. To avoid one stuck job blocks other indefinitely, we set a timout threshold of 4 hours.

If you encounter problems when executing and want to kill the process to cleanup, you can use `misc/scripts/kill.sh`.

Use the following commands to highlight results from log:

```bash
sed -n -e '/^\[Profiler\]/p' -e '/^Dumped/p' ./log_zk
sed -n -e '/^\[Profiler\]/p' -e '/^Dumped/p' ./log_hdfs
```

The results should be kept for later use:
```bash
cp -r inv_verify_output inv_verify_output_bk
```


# 3. Reproduce Sec 9.2 Checking Newer Violations
## 3.1 Detect new ones (Online)

This experiment reproduces the runtime detection part (Sec 9.2) result. The steps are similar to what we described in the previous part: install reproducing hooks, install OathKeeper library, start the instance, trigger the failure, check the logs.
We automate the reproducing steps as convenient as we can. 

The detection relies on the generated invariants from last section.

### ZK-1496

This case we already covered in the `Getting Started` part. No need to repeat the execution.

### ZK-1667
In the paper draft we acknowledged that our tool did not infer useful rules that can detect this case.

### ZK-3546

Please refer to [experiments/notes/ZK-3546.md](experiments/notes/ZK-3546.md)

### HDFS-14699

Please refer to [experiments/notes/HDFS-14317.md](experiments/notes/HDFS-14317.md)

### HDFS-14633

Please refer to [experiments/notes/HDFS-14633.md](experiments/notes/HDFS-14633.md)

### HDFS-14317

Please refer to [experiments/notes/HDFS-14317.md](experiments/notes/HDFS-14317.md)

## 3.2 Crosscheck experiment (ZooKeeper only, Offline)

This experiment reproduces the crosschecking result between 22 ZooKeeper cases.

### Modify config

Modify `conf/samples/zk-3.6.1.properties`: 

```ini
ticket_collection_path=${ok_dir}/conf/samples/zk-collections-cc
```

### Generate traces (~4 h)

Crosschecking is based on generated traces. We need to run traces first.

```bash
./run_engine.sh runall_foreach conf/samples/zk-3.6.1.properties
```


### Run comparison (~30 min)

```bash
./run_engine.sh crosscheckall_dynamic conf/samples/zk-3.6.1.properties conf/samples/zk-collections-cc ./inv_verify_output ./trace_output/ &> log_crosscompare
```

Generated result is in `inv_checktrace_output`. For each case if `ZK-1208` can detect `ZK-1496`, it would generate a marker file `ZK-1208-ZK-1496_detected`, if not that is `ZK-1208-ZK-1496_undetected`.

Run this command to plot a simple version of `Figure 11` in the paper draft.

```bash
sed '/Total detected/!d' log_crosscompare | sed -e 's/Total detected ratio: \(.*\)\/23.*/\1/' | gnuplot -p -e 'set terminal png;plot "/dev/stdin"' > Figure11_simple.png
```

# 4. Reproduce Sec 9.4 Runtime Overhead && 9.5 Rule Activation and False Positive (Online)

This experiment reproduces the result of other metrics during OathKeeper runtime detection. Note that the overhead is directly affected by the number of loaded invariants (the more rules loaded , the throughput is worse).

## 4.1 ZooKeeper

### Install benchmark and configure (~1 min)

Assume you put OathKeeper root at `~/OathKeeper`
```bash
cd ~/OathKeeper
mkdir -p ~/go/src/github.com/samuel && cd ~/go/src/github.com/samuel && cp -r ~/OathKeeper/experiments/bench/go-zookeeper ./ && cd ~/OathKeeper && cp -r experiments/bench/zkbench/ ~/go/src/ && cd ~/go/src/zkbench/ && go build
```

Create a new config called `zkbench/bench_perf.conf`:
```conf
namespace = zkTest
requests = 3000
clients = 15
same_key = false
key_size_bytes = 8
value_size_bytes = 16
type = cmd
cleanup = false

# enable random access
# percents do not have to add up to 1.0
random_access = false
read_percent = 0.4
write_percent = 0.6
runs = 5

# ZooKeeper ensemble, need to change!!
server.0=NODE5:2181
```

For zookeeper cluster configuration, refer to same settings in ZK-3546 [Modify config](experiments/notes/ZK-3546.md#modify-config).

### Checkout Zookeeper version and compile (~3 min)

```bash
cd zookeeper
git stash && git checkout release-3.6.1
mvn clean package -DskipTests
```

### Install runtime and rules (~1 min)

```bash
cd OathKeeper
./run_engine.sh install conf/samples/zk-3.6.1.properties zookeeper
rm -rf inv_prod_input && mkdir inv_prod_input && cp -r inv_verify_output_bk/ZK-* inv_prod_input/
```

### Start workload benchmark (~3 min)

Run on all nodes (the script would automatically start the instances):

```bash
experiments/run/zookeeper/throughput.sh [path_to_OathKeeper] [path_to_ZooKeeper] NODE5 ~/go/src/zkbench/
```

### Collect results
For performance, see logs in `~/go/src/zkbench/zkresult-[date]-summary.dat`, example:

```
1,MIXED,5,3000,0,3191706,181254,11187329,977819,9.575119909s,313.312003,2022-04-14T00:25:45.418795Z,1392:200:198:200:194:200:203:200:198:15
```

`313.312003` is the throughput on this client.

For active ratio, see logs in `zookeeper/logs/zookeeper-*.out`, for example:

```
Checking finished, succCount:.. failCount: .. inactiveCount: ..
```

Active ratio = 1-inactiveCount/(succCount+failCount+inactiveCount)

### Clean up
```bash
cd zookeeper
bin/zkServer.sh stop
rm -r /tmp/zookeeper/version-2/
```

### Repeat for baseline result
Repeat above steps but do not install OathKeeper and rules

## 4.2 HDFS

### Checkout and compile (~2 min)
We assume you already configured in HDFS-14699 and will reuse the scripts and config in HDFS-14699.

```bash
experiments/reproduce/HDFS-14699/install_HDFS-14699.sh [path_to_OathKeeper] [path_to_HADOOP]
experiments/run/hdfs/cleanup.sh [path_to_OathKeeper] [path_to_HADOOP] NODE1
```


### Install runtime and rules (~1 min) 

```bash
./run_engine.sh install conf/samples/hdfs-3.2.1.properties hdfs
rm -rf inv_prod_input && mkdir inv_prod_input && cp -r inv_verify_output_bk/HDFS-* inv_prod_input/
```


### Start workload benchmark (~3 min)

Run on all nodes:
```bash
experiments/run/hdfs/cleanup.sh [path_to_OathKeeper] [path_to_HADOOP] NODE1
```

only run NODE1

```bash
experiments/run/hdfs/throughput.sh [path_to_OathKeeper] [path_to_HADOOP]
```

The result would be printed out in the terminal like:

```
Job started: 0
Job ended: ...
The createWrite job took 90 seconds.
The job recorded 0 exceptions.
```

If so, the throughput is 16000/90=177.77 op/s.

(Sometimes you might encounter benchmark fails to connect, retry cleaning and benchmark usually resolves the problem.)

(If you encounter issues of low resource exceptions and HDFS cluster is forced to enter safe mode, try to reduce benchmark size in the file `OathKeeper/experiments/run/hdfs/throughput.sh` or provide more resources.)

### Clean up (~1 min)

```bash
experiments/run/hdfs/cleanup.sh ~/OathKeeper/ ~/hadoop/ NODE1
```

### Repeat for baseline result
Repeat above steps but do not install OathKeeper and rules
