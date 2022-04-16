#!/usr/bin/env bash
 bin/hdfs dfs -mkdir -p src
 bin/hdfs dfs -mkdir -p dst
bin/hdfs dfs -copyFromLocal share/hadoop/hdfs/hadoop-hdfs-client-3.3.0-SNAPSHOT.jar src/foo
bin/hdfs storagepolicies -setStoragePolicy -path dst -policy ALL_SSD
bin/hdfs dfsadmin -setSpaceQuota 10 -storageType SSD dst
bin/hdfs dfs -mv src/foo dst/foo
echo "if you see no errors this means the failure is reproduced"
