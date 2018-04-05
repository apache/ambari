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

sdir="`dirname \"$0\"`"
: ${1:?"argument is missing: (start|stop|create_collection)"}
command="$1"

function start_containers() {
  check_env_files
  kill_containers
  pushd $sdir/../
  local AMBARI_SOLR_MANAGER_LOCATION=$(pwd)
  echo $AMBARI_SOLR_MANAGER_LOCATION
  cd $AMBARI_SOLR_MANAGER_LOCATION/docker
  echo "Start containers ..."
  docker-compose up -d
  popd
  echo "Containers started"
}

function check_env_files() {
  local count=0;

  check_env_file .env setup_env
  count=$((count + $?));
  check_env_file Profile setup_profile
  count=$((count + $?));

  if [[ "$count" -gt 0 ]]
  then
    echo "Exit"
    exit;
  fi
}

function check_env_file() {
  if [ -f "$sdir/$1" ];
  then
    echo "$1 file exists"
    return 0;
  else
    echo "$1 file does not exist, Creating a new one..."
    $2
    echo "$1 file has been created. Check it out before starting Ambari Infra Manager. ($sdir/$1)"
    return 1;
  fi
}

function setup_env() {
  pushd $sdir/../../
  local AMBARI_LOCATION=$(pwd)
  popd
  local docker_ip=$(get_docker_ip)
  cat << EOF > $sdir/.env
DOCKERIP=$docker_ip
MAVEN_REPOSITORY_LOCATION=$HOME/.m2
AMBARI_LOCATION=$AMBARI_LOCATION

ZOOKEEPER_VERSION=3.4.10
ZOOKEEPER_CONNECTION_STRING=zookeeper:2181

SOLR_VERSION=7.2.1
EOF
}

function get_docker_ip() {
  local ip=$(ifconfig en0 | grep inet | awk '$1=="inet" {print $2}')
  echo $ip
}

function setup_profile() {
  cat << EOF > $sdir/Profile
EOF
}

function kill_containers() {
  pushd $sdir/../
  local AMBARI_SOLR_MANAGER_LOCATION=$(pwd)
  echo "Try to remove containers if exists ..."
  echo $AMBARI_SOLR_MANAGER_LOCATION
  cd $AMBARI_SOLR_MANAGER_LOCATION/docker
  docker-compose rm -f -s solr
  docker-compose rm -f -s zookeeper
  popd
}

function create_collection() {
  pushd $sdir/../
  local AMBARI_SOLR_MANAGER_LOCATION=$(pwd)
  cd $AMBARI_SOLR_MANAGER_LOCATION/docker
  docker exec docker_solr_1 solr create_collection -force -c hadoop_logs -d /usr/lib/ambari-infra-solr/server/solr/configsets/hadoop_logs/conf -n hadoop_logs_conf
  docker exec docker_solr_1 solr create_collection -force -c audit_logs -d /usr/lib/ambari-infra-solr/server/solr/configsets/audit_logs/conf -n audit_logs_conf
  popd
}

case $command in
  "start")
     start_containers
     ;;
  "create_collection")
     create_collection
     ;;
  "stop")
     kill_containers
     ;;
   *)
   echo "Available commands: (start|stop|create_collection)"
   ;;
esac
