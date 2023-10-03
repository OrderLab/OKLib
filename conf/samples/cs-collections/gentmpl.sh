#!/bin/bash

if [[ $# < 3 ]]; then
    echo Expected 3 arguments and 1 optional, found $#
    echo 'Usage example: gentmpl.sh 11803 bc9a0793944f7dd481646c4014d13b844439906c org.apache.cassandra.cql3.ReservedKeywordsTest [methodName]'
    exit 1
fi

ticket_id=$1
commit_id=$2
test_name=$3

if ![[ "$ticket_id" =~ ^CASSANDRA.*$ ]]; then
    ticket_id=CASSANDRA-${ticket_id}
fi

if [[ "$test_name" =~ .java$ ]]; then
    test_name=$(echo $test_name | perl -pe 's/.*(org.*).java/\1/ and s/\//./g')
fi

prefix=$test_name
if [[ $# > 3 ]]; then
    prefix="${prefix}@$4"
fi

cat > ${ticket_id}.properties <<EOF
# https://issues.apache.org/jira/browse/${ticket_id}
# https://github.com/apache/cassandra/commit/${commit_id}
commit_id=${commit_id}
test_name=${test_name}
test_trace_prefix=$prefix
compile_test_cmd="cp \${ok_dir}/conf/samples/cs-patches/build.properties.default . && git apply \${ok_dir}/conf/samples/cs-patches/repo.patch && ant clean build-test"
EOF

vi ${ticket_id}.properties
