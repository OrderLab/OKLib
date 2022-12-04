#!/bin/bash
#This is the running script for tool interfaces

#constants
single_command_timeout_threshold=4h

ok_dir=$(cd "$(dirname "${BASH_SOURCE-$0}")"; pwd)

echo "Setting tool root dir: "${ok_dir}

if [[ -z "$JAVA_HOME" ]]; then
  export JAVA_HOME=$(readlink -f /usr/bin/javac | sed "s:/bin/javac::")
  echo "Warning: JAVA_HOME env is not set, inferring it to be $JAVA_HOME"
fi

# check if we are running this scripts under OathKeeper dir, if not, abort
# we check by scanning if we can find this run_engine script
# meanwhile, note that when running test cases we always use target system dir as working dir
if [ -f "${ok_dir}/run_engine.sh" ]; then
    echo "The tool is running under OathKeeper dir"
else
    echo "[ERROR] cannot detect run_engine.sh under root dir, please run this script under OathKeeper root dir"
    echo "Abort."
    exit
fi

ok_lib="${ok_dir}/target/OathKeeper-1.0-SNAPSHOT-jar-with-dependencies.jar"
log4j_conf="${ok_dir}/conf/log4j.properties"
#this should be consistent with dir in codes
inv_out="${ok_dir}/inv_infer_output"
trace_out="${ok_dir}/trace_output"
check_out="${ok_dir}/inv_checktrace_output"

banner (){
  echo "                                                                                                      "
  echo " ██████╗  █████╗ ████████╗██╗  ██╗██╗  ██╗███████╗███████╗██████╗ ███████╗██████╗      ██╗    ██████╗ "
  echo "██╔═══██╗██╔══██╗╚══██╔══╝██║  ██║██║ ██╔╝██╔════╝██╔════╝██╔══██╗██╔════╝██╔══██╗    ███║   ██╔═████╗"
  echo "██║   ██║███████║   ██║   ███████║█████╔╝ █████╗  █████╗  ██████╔╝█████╗  ██████╔╝    ╚██║   ██║██╔██║"
  echo "██║   ██║██╔══██║   ██║   ██╔══██║██╔═██╗ ██╔══╝  ██╔══╝  ██╔═══╝ ██╔══╝  ██╔══██╗     ██║   ████╔╝██║"
  echo "╚██████╔╝██║  ██║   ██║   ██║  ██║██║  ██╗███████╗███████╗██║     ███████╗██║  ██║     ██║██╗╚██████╔╝"
  echo " ╚═════╝ ╚═╝  ╚═╝   ╚═╝   ╚═╝  ╚═╝╚═╝  ╚═╝╚══════╝╚══════╝╚═╝     ╚══════╝╚═╝  ╚═╝     ╚═╝╚═╝ ╚═════╝ "
  echo ""

}

usage (){
  echo "\tusage: ./run_engine.sh build"
  echo "\tusage: ./run_engine.sh gentrace_testonly conf_file_path test_name"
  echo "\tusage: ./run_engine.sh gentrace conf_file_path ticket_property_file"
  echo "\tusage: ./run_engine.sh gentrace_foreach conf_file_path"
  echo "\tusage: ./run_engine.sh infer conf_file_path ticket_property_file [template_version]"
  echo "\tusage: ./run_engine.sh infer_foreach conf_file_path"
  echo "\tusage: ./run_engine.sh verify conf_file_path ticket_property_file"
  echo "\tusage: ./run_engine.sh verify_foreach conf_file_path"
  echo "\tusage: ./run_engine.sh verify_mergeonly conf_file_path ticket_property_file output_dir_path"
  echo "\tusage: ./run_engine.sh verify_baremetal conf_file_path"
  echo "\tusage: ./run_engine.sh runall conf_file_path ticket_property_file"
  echo "\tusage: ./run_engine.sh runall_foreach conf_file_path"
  echo "\tusage: ./run_engine.sh sample conf_file_path input_dir_path keyword1 keyword2 ..."
  echo "\tusage: ./run_engine.sh stat conf_file_path input_dir_path"
  echo "\tusage: ./run_engine.sh optimize conf_file_path input_dir_path"
  echo "\tusage: ./run_engine.sh eval_completeness conf_file_path input_dir_path"
  echo "\tusage: ./run_engine.sh compare conf_file_path inv_file_1 inv_file_2"
  echo "\tusage: ./run_engine.sh compareall conf_file_path inv_folder inv_file_format"
  echo "\tusage: ./run_engine.sh check_trace conf_file_path verify/detect inv_file trace_file1;trace_file2;.. output_file_name"
  echo "\tusage: ./run_engine.sh crosscheck conf_file_path ticket_file_1 ticket_file_2 inv_dir traces_dir"
  echo "\tusage: ./run_engine.sh crosscheckall conf_file_path ticket_dir_for_inv ticket_dir_for_traces inv_dir traces_dir"
  echo "\tusage: ./run_engine.sh checkout conf_file_path ticket_property_file"
  echo "\tusage: ./run_engine.sh checkoutall conf_file_path"
  echo "\tusage: ./run_engine.sh clean"
  echo "\tusage: ./run_engine.sh jdepend conf_file_path  <directory> [directory2 [directory 3] ..."
  echo "\tusage: ./run_engine.sh install conf_file_path"
}

gentrace_test () {
    cd ${system_dir_path} || return
    echo "full_class_path: ${full_class_path}"
    echo "gentrace_test:"
    echo "java -cp ${full_class_path} \
     -Xmx8g \
     -Dok.testname=${test_name} -Dok.invmode=dump -Dok.patchstate=${patchstate} \
     -Dok.conf=${conf_file_realpath} -Dok.filediff="${diff_file_list}" -Dlog4j.configuration=${log4j_conf} \
      -Dok.ok_root_abs_path=${ok_dir} -Dok.target_system_abs_path=${system_dir_path} \
      -Dok.test_trace_prefix=${test_trace_prefix} \
      -Dok.ticket_id=${ticket_id} oathkeeper.engine.tester.TestEngine"
    timeout ${single_command_timeout_threshold} java -cp ${full_class_path} \
     -Xmx8g \
     -Dok.testname=${test_name} -Dok.invmode=dump -Dok.patchstate=${patchstate} \
     -Dok.conf=${conf_file_realpath} -Dok.filediff="${diff_file_list}" -Dlog4j.configuration=${log4j_conf} \
      -Dok.ok_root_abs_path=${ok_dir} -Dok.target_system_abs_path=${system_dir_path} \
      -Dok.test_trace_prefix=${test_trace_prefix} \
      -Dok.ticket_id=${ticket_id} oathkeeper.engine.tester.TestEngine

    if [ $? -eq 124 ]
    then
      echo "[ERROR] timeout, threshold is " ${single_command_timeout_threshold}
    fi
}

gentrace () {
  ticket_file_path=${file}
  ticket_id=$(basename $file)
  ticket_id="${ticket_id%.*}"
  checkout

  #recalculate once in case this case customize
  #full_class_path=${test_classes_dir_path}:${java_class_path}:${ok_lib}
  #put forward ok_lib to avoid library conflictions
  full_class_path=${ok_lib}:${test_classes_dir_path}:${java_class_path}

  #save all test class
  cp -r ${test_classes_dir_path} ./tmp-test-classes
  #get patched traces
  cd ${ok_dir} || return
  patchstate="patched"
  gentrace_test
  cd ${system_dir_path} || return
  git reset --hard # remove any changes
  #if a fix contains multiple commits, use this to specify first commit
  if [[ -n ${first_commit_id} ]]
  then
    git checkout -f ${first_commit_id}
  fi
  first_commit_id=
  git checkout -f HEAD~1
  eval ${compile_test_cmd}
  #put back all test class
  cp -r ./tmp-test-classes/* ${test_classes_dir_path}
  rm -rf ./tmp-test-classes/
  cd ${ok_dir} || return
  patchstate="unpatched"
  gentrace_test
  cd ${system_dir_path} || return
  git reset --hard # remove any changes
}

gentrace_foreach()
{
    cd ${ok_dir} || return
    for file in ${ticket_collection_path}/*.properties
    do
      timing gentrace "gentrace" ${file}
    done

}

infer ()
{
    ticket_id=$(basename $ticket_file_path)
    ticket_id="${ticket_id%.*}"

    echo "java -cp ${full_class_path} -Dok.conf=${conf_file_realpath} \
     -Dok.ok_root_abs_path=${ok_dir} -Dok.target_system_abs_path=${system_dir_path} \
     -Dok.ticket_id=${ticket_id} \
     -Dok.template_version=${template_version} \
     oathkeeper.engine.InferEngine ${test_trace_prefix}"
    timeout ${single_command_timeout_threshold} java -cp ${full_class_path} -Dok.conf=${conf_file_realpath} \
     -Dok.ok_root_abs_path=${ok_dir} -Dok.target_system_abs_path=${system_dir_path} \
     -Dok.ticket_id=${ticket_id} \
     -Dok.template_version=${template_version} \
     oathkeeper.engine.InferEngine ${test_trace_prefix}

    if [ $? -eq 124 ]
    then
      echo "[ERROR] timeout, threshold is " ${single_command_timeout_threshold}
    fi
}

infer_foreach ()
{
    cd ${ok_dir} || return
    for file in ${ticket_collection_path}/*.properties
    do
      source ${file}
      ticket_file_path=${file}
      timing infer "infer" ${file}
    done
}

verify ()
{
    ticket_id=$(basename $ticket_file_path)
    ticket_id="${ticket_id%.*}"

    checkout
    cd ${system_dir_path} || return

    #recalculate once in case this case customize
    full_class_path=${test_classes_dir_path}:${java_class_path}:${ok_lib}

    echo "java -cp ${full_class_path} -Dok.invmode=${invmode} -Dok.invfile=${test_name} -Dok.patchstate=patched \
     -Dok.conf=${conf_file_realpath} -Dlog4j.configuration=${log4j_conf} \
      -Dok.ok_root_abs_path=${ok_dir} -Dok.target_system_abs_path=${system_dir_path} \
      -Dok.ticket_id=${ticket_id} -Dok.verify_test_package=${verify_test_package} \
      oathkeeper.engine.tester.TestEngine"
    timeout ${single_command_timeout_threshold} java -cp ${full_class_path} -Dok.invmode=${invmode} -Dok.invfile=${test_name} -Dok.patchstate=patched \
     -Dok.conf=${conf_file_realpath} -Dlog4j.configuration=${log4j_conf} \
      -Dok.ok_root_abs_path=${ok_dir} -Dok.target_system_abs_path=${system_dir_path} \
      -Dok.ticket_id=${ticket_id} -Dok.verify_test_package=${verify_test_package} \
      oathkeeper.engine.tester.TestEngine

    if [ $? -eq 124 ]
    then
      echo "[ERROR] timeout, threshold is " ${single_command_timeout_threshold}
    fi
}

verify_foreach()
{
    cd ${ok_dir} || return
    for file in ${ticket_collection_path}/*.properties
    do
      source ${file}
      ticket_file_path=${file}
      invmode="verify"
      timing verify "verify" ${file}
    done
}

verify_baremetal()
{
    cd ${ok_dir} || return
    for file in ${ticket_collection_path}/*.properties
    do
      source ${file}
      ticket_file_path=${file}
      invmode="verify_baremetal"
      timing verify "verify_baremetal" ${file}
    done
}

verify_mergeonly ()
{
    ticket_id=$(basename $ticket_file_path)
    ticket_id="${ticket_id%.*}"

    java -cp ${full_class_path} \
    -Dok.ok_root_abs_path=${ok_dir} -Dok.target_system_abs_path=${system_dir_path} \
    -Dok.ticket_id=${ticket_id} \
    oathkeeper.tool.InvMerger
}

check_trace ()
{
    java -cp ${full_class_path} -Dok.conf=${conf_file_realpath} \
    -Dok.ok_root_abs_path=${ok_dir} -Dok.target_system_abs_path=${system_dir_path} \
    oathkeeper.tool.InvChecker ${check_trace_mode} ${invfile} ${tracefile} ${output_file_name} ifDebugEnabled=${ifDebugEnabled}
}

compare ()
{
    java -cp ${full_class_path} \
     -Dok.ok_root_abs_path=${ok_dir} -Dok.target_system_abs_path=${system_dir_path} \
     oathkeeper.tool.InvComparator ${file1} ${file2}
}

search ()
{
    java -cp ${full_class_path} \
     -Dok.ok_root_abs_path=${ok_dir} -Dok.target_system_abs_path=${system_dir_path} \
     oathkeeper.tool.InvSearcher "$@"
}

crosscheck ()
{
    inv_provider_ticket_id=$(basename ${inv_provider_ticket_file})
    inv_provider_ticket_id="${inv_provider_ticket_id%.*}"
    trace_provider_ticket_id=$(basename ${trace_provider_ticket_file})
    trace_provider_ticket_id="${trace_provider_ticket_id%.*}"

    invfile=${inv_dir}/${inv_provider_ticket_id}/verified_invs
    output_file_name=${inv_provider_ticket_id}-${trace_provider_ticket_id}

    tracefile=""
    for tfile in ${traces_dir}/${trace_provider_ticket_id}/${test_trace_prefix}*\_patched
    do
        tracefile="${tracefile};${tfile}"
    done

    ifDebugEnabled=false
    check_trace_mode=detect
    check_trace
}

checkout ()
{
    clean_state
    cd ${ok_dir} || return

    #clean saved value by reloading the default setting,
    source ${conf_file_path}

    source ${ticket_file_path}
    cd ${system_dir_path} || return
    git checkout -f ${commit_id}
    diff_file_list=$(git diff-tree --no-commit-id --name-only -r ${commit_id})
    eval ${compile_test_cmd}
}

checkout_light ()
{
    clean_state
    cd ${ok_dir} || return

    #clean saved value by reloading the default setting,
    source ${conf_file_path}

    source ${ticket_file_path}
    cd ${system_dir_path} || return
    git checkout -f ${commit_id}
}

crosscheckall_merge ()
{
    for i in $(seq 1 $((${lst_size}-1)))
    do
        echo divide by ${i}
        total_trace_ticket_count=0
        detected_trace_ticket_count=0

        for trace_provider_ticket_file in "${ticket_lst[@]:${i}}"
        do
            total_trace_ticket_count=$((total_trace_ticket_count+1))
            trace_provider_ticket_id=$(basename ${trace_provider_ticket_file})
            trace_provider_ticket_id="${trace_provider_ticket_id%.*}"

            for inv_provider_ticket_file in "${ticket_lst[@]:0:${i}}"
            do

                inv_provider_ticket_id=$(basename ${inv_provider_ticket_file})
                inv_provider_ticket_id="${inv_provider_ticket_id%.*}"

                if ls ${check_out}/${inv_provider_ticket_id}-${trace_provider_ticket_id}\_detected 1> /dev/null 2>&1; then
                    detected_trace_ticket_count=$((detected_trace_ticket_count+1))
                    break
                fi
            done
        done

        echo "Total detected ratio: $((${detected_trace_ticket_count}+${i}))/$((${total_trace_ticket_count}+${i})) for ${i}"
    done
}

clean_state ()
{
  cd ${system_dir_path} || return
  echo "clean states now for repo: ${system_dir_path}"

  git reset --hard > /dev/null 2>&1

  #do a check again see if it's really clean
      #we sometimes ran into unstaged changes that cannot be removed, it seems to be issues from this:
  # https://stackoverflow.com/questions/18536863/git-refuses-to-reset-discard-files
  if ! git diff-files --quiet --ignore-submodules --
  then
    echo "ran into unstaged files still, do a thorough cleanup"

    git rm --cached -r . > /dev/null 2>&1
    git reset --hard > /dev/null 2>&1
    git add . > /dev/null 2>&1
    git commit -m "[OathKeeper] Normalize line endings" > /dev/null 2>&1
  fi
}

jdepend ()
{
  java -cp lib/jdepend-2.10.jar jdepend.xmlui.JDepend -file report.xml "${jdepend_dir_list}"
}

timing ()
{
    echo "Start $2 for $3"

    SECONDS=0
    $1
    duration=$SECONDS
    dt=$(date '+%d/%m/%Y %H:%M:%S');
    echo "$dt"
    echo "[Profiler] $2 for $3 spent ${duration} seconds"
}

#sometimes directly adding in .bashrc not work
add_protoc_for_hdfs()
{

  if [[ ${conf_file_path} == *"hdfs"* ]]
then
  export PROTOC_HOME=${system_dir_path}/protoc/2.5.0
  export HADOOP_PROTOC_PATH=$PROTOC_HOME/dist/bin/protoc
  export PATH=$PROTOC_HOME/dist/bin/:$PATH
fi
}

banner

if [[ $1 == "build" ]]
then
    echo "building the OathKeeper tool now"
    mvn clean package -DskipTests
    exit 0
fi

if [[ $1 == "help" ]]
then
    usage
    exit 0
fi

conf_file_path=$2
conf_file_realpath=$(realpath ${conf_file_path})
source ${conf_file_path}
full_class_path=${test_classes_dir_path}:${java_class_path}:${ok_lib}

if [[ $1 == "test" ]]
then
    echo "(deprecated now)"
    echo "run test engine now"
    echo "example:"
    printf "\t./run_engine.sh test \"/Users/McfateAlan/zookeeper/zookeeper-server/target/classes/:/Users/McfateAlan/zookeeper/zookeeper-server/target/test-classes:/Users/McfateAlan/zookeeper/zookeeper-server/target/lib/*\" org.apache.zookeeper.test.WatcherTest ZK-137 patched conf/samples/zk-3.6.1.properties"
    java -cp $2:target/OathKeeper-1.0-SNAPSHOT-jar-with-dependencies.jar -Dok.testname=$3 -Dok.invmode=dump -Dok.patchid=$4 -Dok.patchstate=$5 -Dok.conf=$6 -Dlog4j.configuration=${log4j_conf} oathkeeper.engine.tester.TestEngine
elif [[ $1 == "gentrace_testonly" ]]
then
    echo "run test engine to generate traces for single test"
    echo "this is mostly useful when you are inspecting a single test"
    test_name=$3
    patchstate="patched"
    timing gentrace_test "gentrace_test" ${test_name}
elif [[ $1 == "gentrace" ]]
then
    echo "run test engine to generate traces for single properties file now"
    file=$3
    timing gentrace "gentrace" ${file}
elif [[ $1 == "gentrace_foreach" ]]
then
    echo "run test engine to generate traces now"
    gentrace_foreach
elif [[ $1 == "infer" ]]
then
    ticket_file_path=$3
    template_version=$4
    source ${ticket_file_path}
    echo "infer invs now"
    timing infer "infer" ${ticket_file_path}
elif [[ $1 == "infer_foreach" ]]
then
    echo "infer invs now for all tickets"
    infer_foreach
elif [[ $1 == "verify" ]]
then
    ticket_file_path=$3
    invmode="verify"
    timing verify "verify" ${ticket_file_path}
elif [[ $1 == "verify_foreach" ]]
then
    verify_foreach
elif [[ $1 == "verify_mergeonly" ]]
then
    echo "verify generated invs from intermediate file"
    echo "this is just step 2)"
    ticket_file_path=$3
    output_dir=$4
    timing verify_mergeonly "verify_mergeonly" ""
elif [[ $1 == "verify_baremetal" ]]
then
    echo "this mode is similar to verify_foreach, but instead it would not do instrumenting but run tests"
    verify_baremetal
elif [[ $1 == "check_trace" ]]
then
    echo "check_trace_mode: detect/verify"
    echo "verify is the simpler usage, just apply inv on traces and output the result, pass or not"
    echo "detect is actually wrapper on top of basic verify functions, it would check on both patched and unpatched traces,"
    echo "only pass on patched traces and fail on unpatched traces, we would call it detect successfully"
    check_trace_mode=$3
    invfile=$4
    tracefile=$5
    output_file_name=$6
    timing check_trace "check_trace" "${invfile} and ${tracefile}"
elif [[ $1 == "sample" ]]
then
    echo "get some partial subsets of all invs based on different percentages"
    java -cp ${full_class_path} -Dok.conf=$2 -Dok.ok_root_abs_path=${ok_dir} oathkeeper.tool.InvSplitter ${@:3}
elif [[ $1 == "stat" ]]
then
    echo "show stats of invs"
    java -cp ${full_class_path} -Dok.conf=$2 -Dok.ok_root_abs_path=${ok_dir} oathkeeper.tool.InvStatPrinter $3
elif [[ $1 == "optimize" ]]
then
    echo "optimize of invs"
    java -cp ${full_class_path} -Dok.conf=$2 -Dok.ok_root_abs_path=${ok_dir} oathkeeper.tool.InvOptimizer $3
elif [[ $1 == "eval_completeness" ]]
then
    echo "eval the completeness of generated invs, we want to know whether invs (especially after some optimization policies)"
    echo "can still detect the failures in our pool"

    echo "this is essentially crosscheck, however, specific to a few tickets"

    echo "in total there are three steps:"
    echo "1) generate invs"
    echo "  e.g. ./run_engine.sh gentrace conf/samples/zk-3.6.1.properties conf/samples/zk-collections/ZK-1208.properties"
    echo "  e.g. ./run_engine.sh infer conf/samples/zk-3.6.1.properties conf/samples/zk-collections/ZK-1208.properties"
    echo "  e.g. ./run_engine.sh verify conf/samples/zk-3.6.1.properties conf/samples/zk-collections/ZK-1208.properties"
    echo "2) generate traces for target case"
    echo "  e.g. ./run_engine.sh gentrace conf/samples/zk-3.6.1.properties conf/samples/zk-collections/ZK-1496.properties"
    echo "3) try to detect using check_trace function"
    check_trace_mode=detect
    invfile=$4
    tracefile=$5
    output_file_name=$6
    timing check_trace "check_trace" "${invfile} and ${tracefile}"

    
elif [[ $1 == "compare" ]]
then
    echo "compare invs from two inv files"
    file1=$3
    file2=$4
    timing compare "compare" "${file1} and ${file2}"
elif [[ $1 == "compareall" ]]
then
    echo "layout: inv_dir/HBASE-xxx/inv_file_format"
    echo "inv_file_format could be verified_invs or *"
    echo "compare invs from all pairs of inv files in inv dir"
    inv_dir=$3
    inv_file_format=$4
    for file1 in ${inv_dir}/**/${inv_file_format}
    do
        for file2 in ${inv_dir}/**/${inv_file_format}
        do
          if [ ${file1} \< ${file2} ]
           then
             #echo "${file1} and ${file2}"
            timing compare "compare" "${file1} and ${file2}"
          fi
        done
    done
elif [[ $1 == "crosscheck" ]]
then
    echo "crosscheck invs against another tracefile for another inv, this is based on tickets"
    echo "example: crosscheck sys.properties ZK-1.properties ZK-2.properties inv_dir traces_dir"
    echo "we assume the following layout:"
    echo "inv_dir/ZK-XXX/verified_invs"
    echo "traces_dir/ZK-XXX/xxxx_patched"
    echo "traces_dir/ZK-XXX/xxxx_unpatched"
    inv_provider_ticket_file=$3
    source ${inv_provider_ticket_file}
    trace_provider_ticket_file=$4
    source ${trace_provider_ticket_file}

    inv_dir=$5
    traces_dir=$6

    timing crosscheck "crosscheck" "${inv_provider_ticket_file} and ${trace_provider_ticket_file}"
elif [[ $1 == "crosscheckall" ]]
then
    echo "compare invs from all pairs of inv files in inv dir"
    echo "example: crosscheckall sys.properties inv_ticket_dir trace_ticket_dir inv_dir traces_dir"
    echo "we assume the following layout:"
    echo "inv_ticket_dir/ZK-XXX.properties"
    echo "trace_ticket_dir/ZK-XXX.properties"
    inv_dir=$5
    traces_dir=$6

    for inv_provider_ticket_file in $3/*.properties
    do
        for trace_provider_ticket_file in $4/*.properties
        do
            timing crosscheck "crosscheck" "${inv_provider_ticket_file} and ${trace_provider_ticket_file}"
        done
    done

    total_trace_ticket_count=0
    detected_trace_ticket_count=0

    for trace_provider_ticket_file in $4/*.properties
    do
        total_trace_ticket_count=$((total_trace_ticket_count+1))
        trace_provider_ticket_id=$(basename ${trace_provider_ticket_file})
        trace_provider_ticket_id="${trace_provider_ticket_id%.*}"
        if ls ${check_out}/*${trace_provider_ticket_id}*\_detected 1> /dev/null 2>&1; then
            detected_trace_ticket_count=$((detected_trace_ticket_count+1))
        fi
    done

    echo "Total detected ratio: ${detected_trace_ticket_count}/${total_trace_ticket_count}"
elif [[ $1 == "crosscheckall_dynamic" ]]
then
    echo "compare invs from all pairs of inv files in inv dir, vary size"
    echo "example: crosscheckall sys.properties all_ticket_dir inv_dir traces_dir"
    echo "we assume the following layout:"
    echo "all_ticket_dir/ZK-XXX.properties"
    all_ticket_dir=$3
    inv_dir=$4
    traces_dir=$5

    ticket_lst=(${all_ticket_dir}/*.properties)
    lst_size=${#ticket_lst[@]}
    echo total ${lst_size} tickets
    for inv_provider_ticket_file in ${all_ticket_dir}/*.properties
    do
        echo inv_provider_ticket_file: ${inv_provider_ticket_file}
        for trace_provider_ticket_file in ${all_ticket_dir}/*.properties
        do
            if [[ "${inv_provider_ticket_file}" == "${trace_provider_ticket_file}" ]]
            then
              continue
            fi

            echo trace_provider_ticket_file: ${trace_provider_ticket_file}
            timing crosscheck "crosscheck" "${inv_provider_ticket_file} and ${trace_provider_ticket_file}"
        done
    done

    crosscheckall_merge
elif [[ $1 == "crosscheckall_merge" ]]
then
    all_ticket_dir=$3
    inv_dir=$4
    traces_dir=$5

    ticket_lst=(${all_ticket_dir}/*.properties)
    lst_size=${#ticket_lst[@]}
    echo total ${lst_size} tickets

    crosscheckall_merge


elif [[ $1 == "search" ]]
then
    echo "search keywords in inv list"
    search "${@:3}"
elif [[ $1 == "checkout" ]]
then
    echo "checkout the target system version after a particular fix, this is for verify use"
    ticket_file_path=$3
    checkout
elif [[ $1 == "checkoutall" ]]
then
    echo "checkout the target system version after a particular fix, this is for verify use"
    for file in ${ticket_collection_path}/*.properties
    do
      source ${file}
      ticket_file_path=${file}
      timing checkout "checkout" ${ticket_file_path}
    done
elif [[ $1 == "install" ]]
then
    echo "install tool to target system startup scripts and classpath"
    echo "essentially 1) add jar to classpath 2) add jvm flags 3) replace main class to our wrapper"
    str="s/OK_DIR_MACRO/${ok_dir//\//\\/}/g and s/SYS_DIR_MACRO/${system_dir_path//\//\\/}/g and s/CONF_PATH_MACRO/${conf_file_realpath//\//\\/}/g"
    echo ${str}
    cd ${system_dir_path} || return
    if [[ $3 == "zookeeper" ]]
    then
        perl -p -e "${str}" ${ok_dir}/conf/samples/zk-patches/install_zk-3.6.1.patch > tmp.patch
        cat tmp.patch
        git apply tmp.patch
        rm tmp.patch
    elif [[ $3 == "hdfs" ]]
    then
        perl -p -e "${str}" ${ok_dir}/conf/samples/hdfs-patches/install_hdfs.patch > tmp.patch
        version=$(perl -ne 'print and last if s/.*<version>(.*)<\/version>.*/\1/;' < pom.xml)
        cat tmp.patch
        git apply tmp.patch
        rm tmp.patch
        cp hadoop-common-project/hadoop-common/src/main/bin/hadoop-functions.sh hadoop-dist/target/hadoop-${version}/libexec/
    elif [[ $3 == "hdfs_14514" ]]
    then
        perl -p -e "${str}" ${ok_dir}/conf/samples/hdfs-patches/install_hdfs_14514.patch > tmp.patch
        version=$(perl -ne 'print and last if s/.*<version>(.*)<\/version>.*/\1/;' < pom.xml)
        cat tmp.patch
        git apply tmp.patch
        rm tmp.patch
        cp hadoop-hdfs-project/hadoop-hdfs/src/main/bin/hdfs hadoop-dist/target/hadoop-${version}/bin/
    else
        echo "[ERROR] missing legal preset system name"
        return
    fi
    echo "successfully install for target system"
elif [[ $1 == "clean" ]]
then
    echo "clean state of target system after abortion of execution"
    clean_state
elif [[ $1 == "jdepend" ]]
then
    echo "analyze package dependencies of target system"
    jdepend_dir_list="${@:3}"
    jdepend
elif [[ $1 == "runall" ]]
then
    echo "run test engine to generate traces for single properties file now"
    file=$3
    ticket_file_path=${file}
    timing gentrace "gentrace" ${file}
    source ${file}
    timing infer "infer" ${file}
    invmode="verify"
    timing verify "verify" ${file}
elif [[ $1 == "runall_foreach" ]]
then
    add_protoc_for_hdfs

    clean_state
    gentrace_foreach
    clean_state
    infer_foreach
    clean_state
    verify_foreach
    #clean_state
    #verify_baremetal
elif [[ $1 == "checkoutall_forhbase_dev" ]]
then
    echo "(dev only)"
    echo "this is a helper function for testing use and should be removed"
    echo "for hbase we want to copy out the cached_classpath for each version"
    mkdir -p "${ok_dir}/HBASE_CACHE_CLASSPATH_DIR"
    for file in ${ticket_collection_path}/*.properties
    do
      source ${file}
      ticket_file_path=${file}
      timing checkout "checkout" ${ticket_file_path}
      ticket_id=$(basename $ticket_file_path)
      ticket_id="${ticket_id%.*}"
      mkdir -p "${ok_dir}/HBASE_CACHE_CLASSPATH_DIR/${ticket_id}"
      cp "${system_dir_path}/hbase-build-configuration/target/cached_classpath.txt" "${ok_dir}/HBASE_CACHE_CLASSPATH_DIR/${ticket_id}"
      cp "${system_dir_path}/target/cached_classpath.txt" "${ok_dir}/HBASE_CACHE_CLASSPATH_DIR/${ticket_id}"
    done
elif [[ $1 == "genpatchall_forhbase_dev" ]]
then
    echo "(dev only)"
    echo "this is a helper function for testing use and should be removed"
    echo "for hbase we want to generate a patch to disable findbugs for each version"
    mkdir -p "${ok_dir}/HBASE_PATCH_DIR"
    for file in ${ticket_collection_path}/*.properties
    do
      source ${file}
      ticket_file_path=${file}
      timing checkout_light "checkout_light" ${ticket_file_path}
      ticket_id=$(basename $ticket_file_path)
      ticket_id="${ticket_id%.*}"
      cp ${ok_dir}/misc/scripts/hbase/gen_patch.py ${system_dir_path}
      cd ${system_dir_path} || return
      python ${system_dir_path}/gen_patch.py
      rm ${system_dir_path}/pom.xml
      mv ${system_dir_path}/pom2.xml ${system_dir_path}/pom.xml
      git diff > "${ok_dir}/HBASE_PATCH_DIR/${ticket_id}.patch"
    done
fi

