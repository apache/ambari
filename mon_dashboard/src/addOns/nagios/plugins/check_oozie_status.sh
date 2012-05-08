#!/usr/bin/env bash
# OOZIE_URL is of the form http://<hostname>:<port>/oozie
# OOZIE_URL: http://hortonworks-sandbox.localdomain:11000/oozie
HOST=$1
PORT=$2
JAVA_HOME=$3
OOZIE_URL="http://$HOST:$PORT/oozie"
export JAVA_HOME=$JAVA_HOME
out=`oozie admin -oozie ${OOZIE_URL} -status 2>&1`
if [[ "$?" -ne 0 ]]; then 
  echo "CRITICAL: Error accessing oozie server status [$out]";
  exit 2;
fi
echo "OK: Oozie server status [$out]";
exit 0;
