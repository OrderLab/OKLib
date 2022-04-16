cd  $2
git stash
git checkout release-3.6.1
git apply $1/misc/scripts/zookeeper/ZK-1496/ZK-1496.patch
mvn install -DskipTests
