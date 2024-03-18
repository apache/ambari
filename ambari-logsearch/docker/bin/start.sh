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
LOGSEARCH_SERVER_PATH=$AMBARI_PATH/ambari-logsearch/ambari-logsearch-server/target/package
LOGFEEDER_PATH=$AMBARI_PATH/ambari-logsearch/ambari-logsearch-logfeeder/target/package
SOLR_LOCATION=/root/solr-$SOLR_VERSION.tgz
SOLR_SERVER_LOCATION=/root/solr-$SOLR_VERSION
ZKCLI=$SOLR_SERVER_LOCATION/server/scripts/cloud-scripts/zkcli.sh
ZK_CONNECT_STRING=${ZK_CONNECT_STRING:-"localhost:9983"}
COMPONENT=${COMPONENT:-"ALL"}

command="$1"

function build_all() {
  echo "build all"
  cd $AMBARI_PATH/ambari-logsearch && mvn clean package -DskipTests && mvn -pl ambari-logsearch-logfeeder clean package -DskipTests
}

function set_custom_zookeeper_address() {
  local file_to_update=${1:?"usage: <filename_to_update>"}
  local zk_connect_string="$ZK_CONNECT_STRING"
  if [ "$zk_connect_string" != "localhost:9983" ] ; then
    sed -i "s|localhost:9983|$zk_connect_string|g" $file_to_update
  fi
}

function create_logfeeder_configs() {
  mkdir /root/config/logfeeder
  cp -r /root/test-config/logfeeder/* /root/config/logfeeder/
  set_custom_zookeeper_address /root/config/logfeeder/logfeeder.properties
  set_custom_zookeeper_address /root/config/logfeeder/shipper-conf/output.config.json
}

function create_logsearch_configs() {
  mkdir -p /root/config/logsearch
  cp /root/test-config/logsearch/log4j.xml /root/config/logsearch/
  cp /root/test-config/logsearch/logsearch-env.sh /root/config/logsearch/
  cp $LOGSEARCH_SERVER_PATH/conf/user_pass.json /root/config/logsearch/user_pass.json
  if [ "$LOGSEARCH_HTTPS_ENABLED" == "true" ]
  then
    cp /root/test-config/logsearch/logsearch-https.properties /root/config/logsearch/logsearch.properties
  else
    cp /root/test-config/logsearch/logsearch.properties /root/config/logsearch/logsearch.properties
  fi

  if [ "$KNOX" == "true"  ]
  then
    cp /root/test-config/logsearch/logsearch-sso.properties /root/config/logsearch/logsearch.properties
  fi

  set_custom_zookeeper_address /root/config/logsearch/logsearch.properties
}

function create_solr_configs() {
  mkdir /root/config/solr
  cp /root/test-config/solr/log4j.properties /root/config/solr/
  cp /root/test-config/solr/zoo.cfg /root/config/solr/
  cp /root/test-config/solr/solr.xml /root/config/solr/
  if [ "$LOGSEARCH_SOLR_SSL_ENABLED" == "true" ]
  then
    cp /root/test-config/solr/solr-env-ssl.sh /root/config/solr/solr-env.sh
  else
    cp /root/test-config/solr/solr-env.sh /root/config/solr/solr-env.sh
  fi
}

function create_configs() {
  create_solr_configs
  create_logfeeder_configs
  create_logsearch_configs
}

function generate_keys() {
  if [ "$GENERATE_KEYSTORE_AT_START" == "true" ]
  then
    IP=`hostname --ip-address`
    echo "generating stores for IP: $IP"
    mkdir -p /etc/ambari-logsearch-portal/conf/keys/
    keytool -genkeypair -alias logsearch -keyalg RSA -keysize 2048 -keypass bigdata -storepass bigdata -validity 9999 -keystore /etc/ambari-logsearch-portal/conf/keys/logsearch.jks  -ext SAN=DNS:localhost,IP:127.0.0.1,IP:$IP -dname "CN=Common Name, OU=Organizational Unit, O=Organization, L=Location, ST=State, C=Country" -rfc
  fi
}

function start_solr_d() {
  echo "Starting Solr..."
  /root/solr-$SOLR_VERSION/bin/solr start -cloud -s /root/logsearch_solr_index/data -verbose -force
  touch /var/log/ambari-logsearch-solr/solr.log

  if [ "$LOGSEARCH_SOLR_SSL_ENABLED" == "true"  ]
  then
    echo "Setting urlScheme as https and restarting solr..."
    $ZKCLI -zkhost localhost:9983 -cmd clusterprop -name urlScheme -val https
    /root/solr-$SOLR_VERSION/bin/solr stop
    /root/solr-$SOLR_VERSION/bin/solr start -cloud -s /root/logsearch_solr_index/data -verbose -force
  fi
}

function start_logsearch() {
  $LOGSEARCH_SERVER_PATH/bin/logsearch.sh start -f
}

function start_logsearch_d() {
  $LOGSEARCH_SERVER_PATH/bin/logsearch.sh start
  touch /var/log/ambari-logsearch-portal/logsearch-app.log
}

function start_logfeeder() {
  $LOGFEEDER_PATH/bin/logfeeder.sh start -f
}


function start_logfeeder_d() {
  $LOGFEEDER_PATH/bin/logfeeder.sh start
  touch /var/log/ambari-logsearch-logfeeder/logsearch-logfeeder.log
}

function start_selenium_server_d() {
  nohup java -jar /root/selenium-server-standalone.jar > /var/log/selenium-test.log &
}

function start_ldap_d() {
  if [ "$KNOX" == "true"  ]
  then
    echo "KNOX is enabled. Starting Demo LDAP."
    su knox -c "/ldap.sh"
  else
    echo "KNOX is not enabled. Skip Starting Demo LDAP."
  fi
}

function start_knox_d() {
  if [ "$KNOX" == "true"  ]
  then
    echo "KNOX is enabled. Starting Demo KNOX gateway."
    su knox -c "/gateway.sh"
  else
    echo "KNOX is not enabled. Skip Starting KNOX gateway."
  fi
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
    "selenium")
      tail -f /var/log/selenium-test.log
     ;;
     "knox")
      tail -f --retry /knox/logs/gateway.log
     ;;
     "ldap")
      tail -f --retry /knox/logs/ldap.log
     ;;
     *)
      tail -f /var/log/ambari-logsearch-portal/logsearch-app.log
     ;;
  esac
}

function main() {
  component=${COMPONENT:-"ALL"}
  case $component in
    "solr")
      create_solr_configs
      echo "Start Solr only ..."
      export COMPONENT_LOG="solr"
      generate_keys
      start_solr_d
      log
     ;;
    "logfeeder")
      create_logfeeder_configs
      echo "Start Log Feeder only ..."
      export COMPONENT_LOG="logfeeder"
      generate_keys
      start_logfeeder
     ;;
    "logsearch")
      create_logsearch_configs
      echo "Start Log Search only ..."
      export COMPONENT_LOG="logsearch"
      generate_keys
      start_logsearch
      log
     ;;
     "knox")
      echo "Start KNOX only ..."
      export COMPONENT_LOG="knox"
      export KNOX="true"
      start_knox_d
      log
     ;;
     "ldap")
      echo "Start Demo LDAP only ..."
      export COMPONENT_LOG="ldap"
      export KNOX="true"
      start_ldap_d
      log
     ;;
     *)
      create_configs
      generate_keys
      start_selenium_server_d
      start_solr_d
      start_logfeeder_d
      start_ldap_d
      start_knox_d
      start_logsearch_d
      log
     ;;
  esac
}

main
