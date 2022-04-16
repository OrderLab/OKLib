#run this scripts on all nodes
#original version of HDFS-14317 can no longer compile, we have to compile a working version and modify based on that version
cd $1
bash ./run_engine.sh checkout conf/samples/hdfs-3.2.1.properties conf/samples/hdfs-collections/HDFS-14633.properties
cd $2
#this is actually modified on 14633
git apply $1/experiments/reproduce/HDFS-14317/hook_HDFS-14317.patch
mvn clean package -Pdist -DskipTests -Dmaven.javadoc.skip=true -Dtar
version=$(perl -ne 'print and last if s/.*<version>(.*)<\/version>.*/\1/;' < pom.xml)
cp $1/experiments/reproduce/HDFS-14317/core-site.xml hadoop-dist/target/hadoop-${version}/etc/hadoop/
cp $1/experiments/reproduce/HDFS-14317/hdfs-site.xml hadoop-dist/target/hadoop-${version}/etc/hadoop/
cp $1/experiments/reproduce/HDFS-14317/workers hadoop-dist/target/hadoop-${version}/etc/hadoop/
$1/experiments/misc/hdfs/editJAVAHOME.sh
