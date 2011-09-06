#!/bin/sh

bin=`dirname "$0"`
bin=`cd "$bin"; pwd`

. "$bin"/hms-config.sh

sleep 30
transmission-remote -t 1 -r
transmission-remote --exit
result=$?
echo $result > ${HMS_HOME}/var/tmp/tracker
