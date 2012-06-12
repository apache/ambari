#!/bin/sh

zkcli_script=$1
user=$2
conf_dir=$3
su - $user -c "source $conf_dir/zookeeper-env.sh ; echo 'ls /' | $zkcli_script"
