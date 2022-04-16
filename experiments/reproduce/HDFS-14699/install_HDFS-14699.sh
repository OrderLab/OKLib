#run this scripts on all nodes
cd $1
bash ./run_engine.sh checkout conf/samples/hdfs-3.2.1.properties conf/samples/hdfs-collections/HDFS-14699.properties
cd $2
git checkout -f HEAD~1
git apply $1/experiments/reproduce/HDFS-14699/hook_HDFS-14699.patch
mvn clean package -Pdist -DskipTests -Dmaven.javadoc.skip=true -Dtar
version=$(perl -ne 'print and last if s/.*<version>(.*)<\/version>.*/\1/;' < pom.xml)
cp $1/experiments/reproduce/HDFS-14699/core-site.xml hadoop-dist/target/hadoop-${version}/etc/hadoop/
cp $1/experiments/reproduce/HDFS-14699/hdfs-site.xml hadoop-dist/target/hadoop-${version}/etc/hadoop/
cp $1/experiments/reproduce/HDFS-14699/workers hadoop-dist/target/hadoop-${version}/etc/hadoop/
$1/experiments/misc/hdfs/editJAVAHOME.sh
