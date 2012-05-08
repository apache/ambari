#!/bin/sh

smoke_script=$1
smoke_user=$2
conf_dir=$3
export ZOOKEEPER_EXIT_CODE=0
zkhosts=` grep server  $conf_dir/zoo.cfg  | cut -f 2 -d '=' | cut -f 1 -d ':' | tr '\n' ' ' `
zk_node1=`echo $zkhosts | tr ' ' '\n' | head -n 1`  
echo "zk_node1=$zk_node1"
# Delete /zk_smoketest znode if exists
su - $smoke_user -c "source $conf_dir/zookeeper-env.sh ;  echo delete /zk_smoketest | ${smoke_script} -server $zk_node1:2181"  
# Create /zk_smoketest znode on one zookeeper server
su - $smoke_user -c "source $conf_dir/zookeeper-env.sh ; echo create /zk_smoketest smoke_data | ${smoke_script} -server $zk_node1:2181"

for i in $zkhosts ; do
  echo "Running test on host $i"
  # Verify the data associated with znode across all the nodes in the zookeeper quorum
  su - $smoke_user -c "source $conf_dir/zookeeper-env.sh ; echo 'get /zk_smoketest' | ${smoke_script} -server $i:2181"
  su - $smoke_user -c "source $conf_dir/zookeeper-env.sh ; echo 'ls /' | ${smoke_script} -server $i:2181"
  output=$(su - $smoke_user -c "source $conf_dir/zookeeper-env.sh ; echo 'get /zk_smoketest' | ${smoke_script} -server $i:2181")
  echo $output | grep smoke_data
  if [[ $? -ne 0 ]] ; then
    echo "Data associated with znode /zk_smoketests is not consistent on host $i"
    ((ZOOKEEPER_EXIT_CODE=$ZOOKEEPER_EXIT_CODE+1))
  fi
done

su - $zmoke_user -c "source $conf_dir/zookeeper-env.sh ; echo 'delete /zk_smoketest' | ${smoke_script} -server $zk_node1:2181"
if [[ "$ZOOKEEPER_EXIT_CODE" -ne "0" ]] ; then
  echo "Zookeeper Smoke Test: Failed" 
else
   echo "Zookeeper Smoke Test: Passed" 
fi
exit $ZOOKEEPER_EXIT_CODE
