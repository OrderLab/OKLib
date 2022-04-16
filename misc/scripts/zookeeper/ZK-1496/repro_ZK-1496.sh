#original bug case takes a cluster to trigger, we did minor modifications to reproduce without custom clients
cd  $2
bin/zkCli.sh <<EOF2
ls /
EOF2
  sleep 8
  echo dump | nc localhost 2181
  echo "[OathKeeper] repro finished, you should see ephemeral still exists after session expired"

