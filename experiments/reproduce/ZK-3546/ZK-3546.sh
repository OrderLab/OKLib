#run this scripts on all nodes

if [ "$(hostname -s)" = $1 ]; then
  echo "creating container node now..."
  bin/zkCli.sh <<EOF
  create -c /q1 11
EOF

 sleep 10

  bin/zkCli.sh <<EOF
 ls /
EOF

echo "if u see /q1 exists, that's successful."
else
  echo "I'm not leader, so exiting... You should not see this msg on zookeeper leader."
  
fi
