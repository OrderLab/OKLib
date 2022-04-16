#run this scripts on namenode
cd $2
version=$(perl -ne 'print and last if s/.*<version>(.*)<\/version>.*/\1/;' < pom.xml)
$1/experiments/run/hdfs/start.sh $1 $2
cd hadoop-dist/target/hadoop-${version}/
$1/experiments/reproduce/HDFS-14699/HDFS-14699.sh ${version} $1 $2 $3 $4 $5

