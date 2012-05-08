#!/usr/bin/env bash
# out='{"status":"ok","version":"v1"}<status_code:200>'
HOST=$1
PORT=$2
VERSION=$3
regex="^.*\"status\":\"ok\".*<status_code:200>$"
out=`curl http://$HOST:$PORT/templeton/$VERSION/status -w '<status_code:%{http_code}>' 2>&1`
if [[ $out =~ $regex ]]; then 
  echo "OK: Templeton server status [$out]";
  exit 0;
fi
echo "CRITICAL: Error accessing Templeton server, status [$out]";
exit 2;
