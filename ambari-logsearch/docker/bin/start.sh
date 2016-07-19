#!/bin/bash
# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License

AMBARI_PATH=/root/ambari
LOGSEARCH_SERVER_PATH=$AMBARI_PATH/ambari-logsearch/ambari-logsearch-portal/target/package
LOGFEEDER_PATH=$AMBARI_PATH/ambari-logsearch/ambari-logsearch-logfeeder/target/package
SOLR_LOCATION=/root/solr-$SOLR_VERSION.tgz
SOLR_SERVER_LOCATION=/root/solr-$SOLR_VERSION
ZKCLI=$SOLR_SERVER_LOCATION/server/scripts/cloud-scripts/zkcli.sh

command="$1"

function build_all() {
  echo "build all"
  cd $AMBARI_PATH/ambari-logsearch && mvn clean package -DskipTests && mvn -pl ambari-logsearch-logfeeder clean package -DskipTests
}

function start_solr() {
  echo "Starting Solr..."
  /root/solr-$SOLR_VERSION/bin/solr start -cloud -s /root/logsearch_solr_index/data -verbose
  touch /var/log/ambari-logsearch-solr/solr.log
}

function start_logsearch() {
  echo "Upload configuration sets ..."
  $ZKCLI  -zkhost localhost:9983 -cmd upconfig -confdir $LOGSEARCH_SERVER_PATH/solr_configsets/audit_logs/conf -confname audit_logs
  $ZKCLI  -zkhost localhost:9983 -cmd upconfig -confdir $LOGSEARCH_SERVER_PATH/solr_configsets/hadoop_logs/conf -confname hadoop_logs
  $ZKCLI  -zkhost localhost:9983 -cmd upconfig -confdir $LOGSEARCH_SERVER_PATH/solr_configsets/history/conf -confname history

  cp $LOGSEARCH_CONFIG_LOCATION/logsearch.properties /root/ambari/ambari-logsearch/ambari-logsearch-portal/target/package/classes/logsearch.properties
  cp $LOGSEARCH_CONFIG_LOCATION/log4j.xml /root/ambari/ambari-logsearch/ambari-logsearch-portal/target/package/classes/logsearch.properties
  $LOGSEARCH_SERVER_PATH/run.sh
  touch /var/log/ambari-logsearch-portal/logsearch-app.log
}

function start_logfeeder() {
  cp $LOGFEEDER_CONFIG_LOCATION/logfeeder.properties /root/ambari/ambari-logsearch/ambari-logsearch-logfeeder/target/package/classes/logfeeder.properties
  cp $LOGFEEDER_CONFIG_LOCATION/log4j.xml /root/ambari/ambari-logsearch/ambari-logsearch-logfeeder/target/package/classes/log4j.xml
  $LOGFEEDER_PATH/run.sh
  touch /var/log/ambari-logsearch-logfeeder/logsearch-logfeeder.log
}

function log() {
  component_log=${COMPONENT_LOG:-"logsearch"}
  case $component_log in
    "logfeeder")
      tail -f /var/log/ambari-logsearch-logfeeder/logsearch-logfeeder.log
     ;;
    "solr")
      tail -f /var/log/ambari-logsearch-solr/solr.log
     ;;
     *)
      tail -f /var/log/ambari-logsearch-portal/logsearch-app.log
     ;;
  esac
}

start_solr
start_logsearch
start_logfeeder
log



