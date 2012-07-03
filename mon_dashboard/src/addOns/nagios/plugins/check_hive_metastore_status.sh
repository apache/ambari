#!/usr/bin/env bash
#The uri is of the form thrift://<hostname>:<port>
HOST=$1
PORT=$2
HCAT_URL=-Dhive.metastore.uris="thrift://$HOST:$PORT"
out=`hcat $HCAT_URL -e "show databases" 2>&1`
if [[ "$?" -ne 0 ]]; then 
  echo "CRITICAL: Error accessing hive-metaserver status [$out]";
  exit 2;
fi
echo "OK: Hive metaserver status OK";
exit 0;
