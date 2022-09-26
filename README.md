# Source code repository for the Oathkeeper project


## Overview

Oathkeeper is a runtime verification toolchain to detect silent semantic
violations in distributed systems. For a given system, Oathkeeper first
leverages the *old* silent semantic failures in this system to infer the
underlying essential semantic rules. It then enforces these rules at runtime
for systems to catch *new*, unseen semantic violations.

<img src="https://sysartifacts.github.io/images/usenix_available.svg" alt="drawing" width="100"/> <img src="https://sysartifacts.github.io/images/usenix_functional.svg" alt="drawing" width="100"/> <img src="https://sysartifacts.github.io/images/usenix_reproduced.svg" alt="drawing" width="100"/>

Table of Contents
=================
* [Requirements](#requirements)
* [Getting Started Instructions](#getting-started-instructions)
   * [0. Install dependencies](#0-install-dependencies)
   * [1. Clone the Oathkeeper repository](#1-clone-the-oathkeeper-repository)
   * [2. Build Oathkeeper (~1 min)](#2-build-oathkeeper-1-min)
   * [3. Get the target system (~2 min)](#3-get-the-target-system-2-min)
   * [4. Customize configurations to analyze target system](#4-customize-configurations-to-analyze-target-system)
      * [4.1 Target system config](#41-target-system-config)
      * [4.2 Test case config](#42-test-case-config)
   * [5. Execute tests and generate traces (~1 min)](#5-execute-tests-and-generate-traces-1-min)
   * [6. Infer rules from traces (~1 min)](#6-infer-rules-from-traces-1-min)
   * [7. Verify inferred rules (~20 min)](#7-verify-inferred-rules-20-min)
   * [8. Runtime detection](#8-runtime-detection)
      * [8.1 Inject failure triggers (~2 min)](#81-inject-failure-triggers-2-min)
      * [8.2 Install Oathkeeper runtime (~1 min)](#82-install-oathkeeper-runtime-1-min)
         * [8.2.1 Add dependency library to class path](#821-add-dependency-library-to-class-path)
         * [8.2.2 Modify startup scripts](#822-modify-startup-scripts)
      * [8.3 Load rules (~1 min)](#83-load-rules-1-min)
      * [8.4 Monitor detection results](#84-monitor-detection-results)
         * [8.4.1 Reproduce failures (~2 min)](#841-reproduce-failures-2-min)
         * [8.4.2 Start up the target system (~1 min)](#842-start-up-the-target-system-1-min)
         * [8.4.3 Check results (~1 min)](#843-check-results-1-min)
* [Detailed Instructions](#detailed-instructions)
* [Known Issues](#known-issues)
* [Publication](#publication)

## Requirements

* OS and JDK:
  - Oathkeeper is developed and tested under **Ubuntu 18.04** and **JDK 8**. 
  - Other systems and newer JDKs may also work. We tested a few functionalities on macOS Catalina (10.15.7) but the test is not complete. 

* Hardware: 
  - The basic workflow of Oathkeeper described in this README, which should satisfy the `Artifacts Functional` requirements, can be done in just one single node.
  - To reproduce the failures in our evaluated distributed systems and meet the `Results Reproduced` requirements, we recommend that you use a **cluster of 5 nodes**. 

* Git (>= 2.16.2, version control)
* Apache Maven (>= 3.6.3, for OathKeeper compilation)
* Apache Ant (>= 1.10.9, artifact testing only, for zookeeper compilation)
* JDK8 (openjdk recommended)

## Getting Started Instructions

For most instructions below we provide automated commands in scripts for a
couple of popular versions of two exercised systems ZooKeeper and HDFS. 
We use this flag :checkered_flag: to highlight automation scripts for Artifact
Evaluation.

There is roughly estimated execution time for each step. The real execution
time can vary depending on the machine performance and network bandwidth.

The total time estimated to go through the workflow below is around 35 minutes. 

### 0. Install dependencies

```bash
sudo apt-get update
sudo apt install git maven ant vim openjdk-8-jdk
sudo update-alternatives --set java $(sudo update-alternatives --list java | grep "java-8")
```

Make sure you set JDK to be openjdk-8. You should also set the `JAVA_HOME` 
environment variable properly (and add it to `.bashrc`):

```bash
export JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64
echo export JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64 >> ~/.bashrc
```

### 1. Clone the Oathkeeper repository

To clone from github:

```bash
git clone https://github.com/OrderLab/OathKeeper.git
cd OathKeeper
git submodule update --init --recursive
```

### 2. Build Oathkeeper (~1 min)

Oathkeeper uses Maven for project management.

To compile and run Oathkeeper tests:

```bash
cd OathKeeper && mvn package
```

You should see test passed:
```
Results :

Tests run: 21, Failures: 0, Errors: 0, Skipped: 0
```

and compilation succeeded.
```
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  4.170 s
[INFO] Finished at: 2022-03-27T16:12:18-04:00
[INFO] ------------------------------------------------------------------------
```

### 3. Get the target system (~2 min)

Clone the Git repository for the target system you want to infer rules from. We
use [Zookeeper](https://github.com/apache/zookeeper) as an example:

```bash
git clone git@github.com:apache/zookeeper.git
cd zookeeper && pwd
```
If you have problems with using SSH to clone, you can use HTTPS `git clone https://github.com/apache/zookeeper.git` instead.

Record the full path to the ZooKeeper repository (e.g. `/home/chang/zookeeper`),
which will be used as input in the next step.

No need to compile ZooKeeper at this stage. Oathkeeper will re-compile the 
target system during the experiment.

### 4. Customize configurations to analyze target system

#### 4.1 Target system config
> :checkered_flag: For Artifact Evaluation: you don't need to go through steps in this subsection as we already prepared related recipes under `conf/samples/`, you only need to modify the file `conf/samples/zk-3.6.1.properties` and change `system_dir_path` to be the absolute path to your target system. For example, if zookeeper is cloned to `/home/chang/zookeeper/`, you should set the config to be `system_dir_path=/home/chang/zookeeper/`. 

A configuration is needed to specify the basic information about the target system. 

```bash
vim conf/samples/zk-3.6.1.properties
```

A sample config file looks like:
```ini
#Required (user-specific):
system_dir_path=/home/chang/zookeeper/
ticket_collection_path=${ok_dir}/conf/samples/zk-collections

#Required (customized rule-related):
time_window_length_in_millis=5000
#select instrumentation range: strict-selective, relaxed-selective, specified_selective, full
gentrace_instrument_mode=strict-selective
verify_survivor_mode=true

#Required (system related):
java_class_path="${system_dir_path}/build/classes/:${system_dir_path}/build/lib/*"
test_classes_dir_path="${system_dir_path}/build/test/classes/"
system_package_prefix=org.apache.zookeeper
test_class_name_regex=.*Test$
compile_test_cmd="ant clean compile-test"

#Optional (testing-use):
verify_abort_after_three_test=false
force_instrument_nothing=false
force_track_no_states=false
force_disable_prod_checking=false
force_disable_enqueue_events=false
dump_suppress_inv_list_when_checking=false

#Optional:
instrument_state_fields=
instrument_class_allmethods=
exclude_class_list=
```
#### 4.2 Test case config

> :checkered_flag: For Artifact Evaluation: you can skip this subsection as well, since we already prepared the related recipes under `conf/samples/`.

Oathkeeper leverages regression tests to infer semantic rules. Users should
provide recipes about these test cases. Note that such test case configurations 
override the target system configurations.

For example, to perform analysis for [ZOOKEEPER-1754](https://issues.apache.org/jira/browse/ZOOKEEPER-1754). 

```bash
mkdir -p conf/samples/zk-collections/
vim conf/samples/zk-collections/ZK-1754.properties
```
and input
```ini
#commit that fixes the issue
commit_id=67dd6fc9df3d33e40570095b52cd6858621c3ae0
test_name=org.apache.zookeeper.test.ReadOnlyModeTest
test_trace_prefix=org.apache.zookeeper.test.ReadOnlyModeTest@testMultiTransaction
```

Some old versions of target systems may experience compilation issues (described in [Known Issues](#known-issues)), in such cases we provide a workaround by customizing the compilation test commands and apply a patch before the compiling commands. One example is [ZOOKEEPER-1208](https://issues.apache.org/jira/browse/ZOOKEEPER-1208).

```ini
#case specific
commit_id=fa9e821e91d5c007593f830dcc4553a3f05b1038
test_name=org.apache.zookeeper.test.SessionInvalidationTest
test_trace_prefix=org.apache.zookeeper.test.SessionInvalidationTest
compile_test_cmd="rm -f src/java/lib/ivy-2.2.0.jar && git apply ${ok_dir}/conf/samples/zk-patches/https.patch && ant clean compile-test"
```
### 5. Execute tests and generate traces (~1 min)

> :warning: WARNING: Before you execute this step, be aware that the automation scripts in Oathkeeper applies clean operations to the target system repo, such as `git reset --hard ` and `git rm --cached -r .`. This is to ensure the repo is clean when switching between versions.

For example, to generate traces from [ZOOKEEPER-1208](https://issues.apache.org/jira/browse/ZOOKEEPER-1208). Run following commands under OathKeeper root: 

```bash
./run_engine.sh gentrace conf/samples/zk-3.6.1.properties conf/samples/zk-collections/ZK-1208.properties
```

In the stdout you may notice some failure messages like:

```
There was 1 failure:
1) testCreateAfterCloseShouldFail(org.apache.zookeeper.test.SessionInvalidationTest)
junit.framework.AssertionFailedError: expected:<1> but was:<11>
	at junit.framework.Assert.fail(Assert.java:57)
	at junit.framework.Assert.failNotEquals(Assert.java:329)
	at junit.framework.Assert.assertEquals(Assert.java:78)
	at junit.framework.Assert.assertEquals(Assert.java:234)
	at junit.framework.Assert.assertEquals(Assert.java:241)
	at org.apache.zookeeper.test.SessionInvalidationTest.testCreateAfterCloseShouldFail(SessionInvalidationTest.java:101)
	at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
	at sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:62)
	at sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
	at java.lang.reflect.Method.invoke(Method.java:498)
	at org.junit.runners.model.FrameworkMethod$1.runReflectiveCall(FrameworkMethod.java:50)
	at org.junit.internal.runners.model.ReflectiveCallable.run(ReflectiveCallable.java:12)
	at org.junit.runners.model.FrameworkMethod.invokeExplosively(FrameworkMethod.java:47)
	at org.junit.internal.runners.statements.InvokeMethod.evaluate(InvokeMethod.java:17)
	at org.apache.zookeeper.JUnit4ZKTestRunner$LoggedInvokeMethod.evaluate(JUnit4ZKTestRunner.java:52)
	at org.junit.internal.runners.statements.RunBefores.evaluate(RunBefores.java:26)
	at org.junit.internal.runners.statements.RunAfters.evaluate(RunAfters.java:27)
	at org.junit.rules.TestWatchman$1.evaluate(TestWatchman.java:53)
	at org.junit.runners.ParentRunner.runLeaf(ParentRunner.java:325)
	at org.junit.runners.BlockJUnit4ClassRunner.runChild(BlockJUnit4ClassRunner.java:78)
	at org.junit.runners.BlockJUnit4ClassRunner.runChild(BlockJUnit4ClassRunner.java:57)
	at org.junit.runners.ParentRunner$3.run(ParentRunner.java:290)
	at org.junit.runners.ParentRunner$1.schedule(ParentRunner.java:71)
	at org.junit.runners.ParentRunner.runChildren(ParentRunner.java:288)
	at org.junit.runners.ParentRunner.access$000(ParentRunner.java:58)
	at org.junit.runners.ParentRunner$2.evaluate(ParentRunner.java:268)
	at org.junit.runners.ParentRunner.run(ParentRunner.java:363)
	at org.junit.runners.Suite.runChild(Suite.java:128)
	at org.junit.runners.Suite.runChild(Suite.java:27)
	at org.junit.runners.ParentRunner$3.run(ParentRunner.java:290)
	at org.junit.runners.ParentRunner$1.schedule(ParentRunner.java:71)
	at org.junit.runners.ParentRunner.runChildren(ParentRunner.java:288)
	at org.junit.runners.ParentRunner.access$000(ParentRunner.java:58)
	at org.junit.runners.ParentRunner$2.evaluate(ParentRunner.java:268)
	at org.junit.runners.ParentRunner.run(ParentRunner.java:363)
	at org.junit.runner.JUnitCore.run(JUnitCore.java:137)
	at org.junit.runner.JUnitCore.run(JUnitCore.java:115)
	at org.junit.runner.JUnitCore.run(JUnitCore.java:105)
	at org.junit.runner.JUnitCore.run(JUnitCore.java:94)
	at oathkeeper.engine.tester.TestEngine.execSingleTest(TestEngine.java:113)
	at oathkeeper.engine.tester.TestEngine.main(TestEngine.java:157)

FAILURES!!!
Tests run: 1,  Failures: 1
```

They are from JUnit and are expected, as Oathkeeper performs both patched runs and buggy runs. The test in the buggy run is doomed to fail.

You should notice some new files (`org.apache.zookeeper.test.SessionInvalidationTest@testCreateAfterCloseShouldFail_patched` and `org.apache.zookeeper.test.SessionInvalidationTest@testCreateAfterCloseShouldFail_unpatched`) under `./trace_output` after generation finished, which looks like:

```json
{
  "eventQueue": [
    {
      "type": "oathkeeper.runtime.event.OpTriggerEvent",
      "data": {
        "opName": "org.apache.zookeeper.server.persistence.FileTxnLog.setPreallocSize(long)",
        "system_timestamp": 1648416409252,
        "logical_timestamp": 1
      }
    },
    {
      "type": "oathkeeper.runtime.event.OpTriggerEvent",
      "data": {
        "opName": "org.apache.zookeeper.server.DataNode.addChild(java.lang.String)",
        "system_timestamp": 1648416409404,
        "logical_timestamp": 2
      }
    },
    {
      "type": "oathkeeper.runtime.event.OpTriggerEvent",
      "data": {
        "opName": "org.apache.zookeeper.server.DataNode.addChild(java.lang.String)",
        "system_timestamp": 1648416409404,
        "logical_timestamp": 3
...
```



### 6. Infer rules from traces (~1 min)

Then we infer rules. For example, to infer rules from [ZOOKEEPER-1208](https://issues.apache.org/jira/browse/ZOOKEEPER-1208).  

```bash
./run_engine.sh infer conf/samples/zk-3.6.1.properties conf/samples/zk-collections/ZK-1208.properties
```

The generated output is under `./inv_infer_output`.

```json
{
  "invariantList": [
    {
      "template": {
        "type": "oathkeeper.runtime.template.OpHappenBeforeOpTemplate",
        "data": {}
      },
      "context": {
        "left": {
          "type": "oathkeeper.runtime.event.OpTriggerEvent",
          "data": {
            "opName": "org.apache.zookeeper.server.ZKDatabase.getSessionWithTimeOuts()",
            "system_timestamp": 1648416409414,
            "logical_timestamp": 28
          }
        },
        "right": {
          "type": "oathkeeper.runtime.event.OpTriggerEvent",
          "data": {
            "opName": "org.apache.zookeeper.server.ZKDatabase.convertLong(java.lang.Long)",
            "system_timestamp": 1648416409491,
            "logical_timestamp": 1976
          }
        },
...

```
### 7. Verify inferred rules (~20 min)

Verifying inferred rules can be time-consuming as Oathkeeper needs to execute all
test cases in the target system and check all inferred rules from the last step. 

You can optionally speed up this step by turning on the survivor mode, which prevents
failed rules from loading and executing in other test cases. This optimization 
may pre-maturely kill some legitimate rule due to a bad/flaky test case.

```bash
./run_engine.sh verify conf/samples/zk-3.6.1.properties conf/samples/zk-collections/ZK-1208.properties
```

The progress is printed in stdout, such as 

```
Spawn test for 10/82
...
```

**Note:** Output like `javassist.CannotCompileException: by java.lang.ClassFormatError` is fine. In most cases, they are benign signals for redundant class transformation.

The generated output is under `./inv_verify_output`. They look similar to
results from last step, but the list size is greatly reduced. Note that the
inference and verification are neither deterministic process. It is common if
output numbers are different if re-executed. 

### 8. Runtime detection

#### 8.1 Inject failure triggers (~2 min)

This step is for artifact evaluation only. You can skip this step if you are
users to deploy the tool to production systems.

To test the effectiveness of the tool, we provide some failure reproducing
scripts. For basic functionality, we use ZK-1496 as example. In this example,
ZooKeeper the ephemeral node that expires is not properly cleaned. At this step
we instrument the ZooKeeper source codes to reproduce the failures later:

```bash
cd OathKeeper
./misc/scripts/zookeeper/ZK-1496/install_ZK-1496.sh [path_to_OathKeeper_root] [path_to_Zookeeper_root]
```

For example, 
```bash
cd OathKeeper
./misc/scripts/zookeeper/ZK-1496/install_ZK-1496.sh ~/OathKeeper ~/zookeeper
```

Retry if you encounter problems.

#### 8.2 Install Oathkeeper runtime (~1 min)

> :checkered_flag: For Artifact Evaluation: you can just execute and skip the remaining 8.2 section: 
> ```bash
> ./run_engine.sh install conf/samples/zk-3.6.1.properties zookeeper
> ```

##### 8.2.1 Add dependency library to class path

To invoke event recording and rule checking functionalities, Oathkeeper runtime
and related data structures need to be added, by either copying Oathkeeper
packed jar file to the class path of target system:

```bash
cp target/OathKeeper-1.0-SNAPSHOT-jar-with-dependencies.jar [system_class_path]
```

or modify class path to include library.

```bash
CLASSPATH="OK_DIR_MACRO/target/*:$CLASSPATH"
```

##### 8.2.2 Modify startup scripts

Many popular distributed systems use scripts to start instances. There are two needed changes.  

First, Oathkeeper needs to instrument classes of target system before they are
loaded, thus it must start before any other class. We use a utility class
called `MainWrapper` which replaces original Main class. The usage is simple:
just use `MainWrapper` as new main class and add original main class name to
the list of args. 

Second, some JVM flags need to be added:
* `-Dok.invmode=prod` 
* `-Dok.conf=CONF_PATH_MACRO`
* `-Dok.ok_root_abs_path=OK_DIR_MACRO`
* `-Dok.target_system_abs_path=SYS_DIR_MACRO`

For example, to modify for zookeeper 3.6.1, here is an sample on its `bin/zkServer.sh`
```patch
+    OKFLAGS="-Dok.invmode=prod -Dok.conf=CONF_PATH_MACRO -Dok.ok_root_abs_path=OK_DIR_MACRO -Dok.target_system_abs_path=SYS_DIR_MACRO"
     nohup "$JAVA" $ZOO_DATADIR_AUTOCREATE "-Dzookeeper.log.dir=${ZOO_LOG_DIR}" \
     "-Dzookeeper.log.file=${ZOO_LOG_FILE}" "-Dzookeeper.root.logger=${ZOO_LOG4J_PROP}" \
     -XX:+HeapDumpOnOutOfMemoryError -XX:OnOutOfMemoryError='kill -9 %p' \
-    -cp "$CLASSPATH" $JVMFLAGS $ZOOMAIN "$ZOOCFG" > "$_ZOO_DAEMON_OUT" 2>&1 < /dev/null &
+    -cp "$CLASSPATH" $JVMFLAGS $OKFLAGS oathkeeper.engine.MainWrapper $ZOOMAIN "$ZOOCFG" > "$_ZOO_DAEMON_OUT" 2>&1 < /dev/null &
```


#### 8.3 Load rules (~1 min)

Essentially copy verified rules to `{ok_dir}/inv_prod_input` so the Oathkeeper runtime would load them and check when the system is running.

```
cp -r inv_verify_output/ inv_prod_input/
```


#### 8.4 Monitor detection results

##### 8.4.1 Reproduce failures (~2 min)

This step is for artifact evaluation only. If you are users to deploy Oathkeeper 
to production systems, you should execute the next step instead.

To start a zookeeper instance and reproduce ZK-1496, run:

```bash
cd OathKeeper
./misc/scripts/zookeeper/ZK-1496/trigger_ZK-1496.sh [path_to_OathKeeper_root] [path_to_Zookeeper_root]
```

for example, 
```bash
cd OathKeeper
./misc/scripts/zookeeper/ZK-1496/trigger_ZK-1496.sh ~/OathKeeper ~/zookeeper
```

The scripts would display signals when reproducing finished. We speed up the
procedures in codes for evaluation convenience. Also note that to faithfully
mimic this case it requires special clients and a cluster, for evaluation
convenience we added minor code changes thus you may experience issues if
trying to directly connect to zk instance. You could use `echo dump | nc
localhost 2181` to observe the symptom (ephemeral node exists forever).

##### 8.4.2 Start up the target system (~1 min)

> :checkered_flag: For Artifact Evaluation: you should skip this step as the step 8.4.1 already started the system instances.

If you want to detect unknown failures in the deployed system, you can start the system as usual. If previous modifications to startup scripts are good, the system instance should work.

For example, to start zookeeper:

```bash
cp conf/zoo_sample.cfg conf/zoo.cfg
bin/zkServer.sh start
```

##### 8.4.3 Check results (~1 min)

The checking result will be printed to stdout (or redirected to logs depending on target system's log configuration, for example, zookeeper saves output to `zookeeper/logs/zookeeper-*.out`). If some invariant fails and report, the log would print failed invariants such as:

```
[...]ASSERT FAIL! #220
Invariant{template=oathkeeper.runtime.template.OpImplyOpTemplate, context=Context{left=OpTriggerEvent{opName='org.apache.zookeeper.server.SessionTrackerImpl.setSessionClosing(long)', system_timestamp=1648416409404, logical_timestamp=2}, right=OpTriggerEvent{opName='org.apache.zookeeper.server.SessionTrackerImpl.removeSession(long)', system_timestamp=1648416409404, logical_timestamp=7}
```

## Detailed Instructions

Please see the [README_detailed.md](README_detailed.md) for further instructions.

## Known Issues

* Oathkeeper needs to compile and execute old versions of target systems. Such old versions, in some cases, depend on libraries that are already deprecated and no longer accesible, causing the target system not directly compilable. One workaround is to provide interface for users to manually add a patch to modify the project compilation configuration (such as pom.xml) of target system to disable certain modules that blocks compilation.

* Rule inference and verification are memory-consuming process and could trigger a lot of GCs for if test execution trace is very long. We suggest using physical machines with larger memories. 

## Publication

> Chang Lou, Yuzhuo Jing, and Peng Huang. **Demystifying and Checking Silent Semantic Violations in Large Distributed Systems**. To appear in Proceedings of the 16th USENIX Symposium on Operating Systems Design and Implementation (OSDI '22), Carlsbad, CA, USA, July 2022.

## Acknowledgement

We very appreciate the reviewers of OSDI'22 Artifact Evaluation try out this tool and provide useful feedbacks.
