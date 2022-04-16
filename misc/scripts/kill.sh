pkill -f "./run_engine.sh"
kill -9 `jps | grep "TestEngine" | cut -d " " -f 1`
kill -9 `jps | grep "InferEngine" | cut -d " " -f 1`
