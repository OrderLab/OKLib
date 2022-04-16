#run this scripts on namenode
cd $2
version=$(perl -ne 'print and last if s/.*<version>(.*)<\/version>.*/\1/;' < pom.xml)
$1/experiments/run/hdfs/start.sh $1 $2
cd hadoop-dist/target/hadoop-${version}/
$1/experiments/reproduce/HDFS-14633/HDFS-14633.sh

