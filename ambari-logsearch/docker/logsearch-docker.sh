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
: ${1:?"argument is missing: (start|stop|build-and-run|build|build-docker-and-run|build-mvn-and-run|build-docker-only|build-mvn-only)"}
command="$1"
shift

while getopts "bf" opt; do
  case $opt in
    b) # build backend only
      maven_build_options="-pl !ambari-logsearch-web"
      ;;
    f) # build frontend only
      maven_build_options="-pl ambari-logsearch-web"
      ;;
    \?)
      echo "Invalid option: -$OPTARG" >&2
      exit 1
      ;;
    :)
      echo "Option -$OPTARG requires an argument." >&2
      exit 1
      ;;
  esac
done

function build_logsearch_project() {
  pushd $sdir/../
  mvn clean package -DskipTests $maven_build_options
  popd
}

function build_logsearch_container() {
  pushd $sdir
  docker build -t ambari-logsearch:v1.0 .
  popd
}

function get_docker_ip() {
  local ip=$(ifconfig en0 | grep inet | awk '$1=="inet" {print $2}')
  echo $ip
}

function start_logsearch_container() {
  setup_env
  setup_profile
  kill_logsearch_container
  echo "Run Log Search container"
  docker-compose -f all.yml up -d
  ip_address=$(docker inspect --format '{{ .NetworkSettings.IPAddress }}' logsearch)
  echo "Log Search container started on $ip_address (for Mac OSX route to boot2docker/docker-machine VM address, e.g.: 'sudo route add -net 172.17.0.0/16 192.168.59.103')"
  echo "You can follow Log Search logs with 'docker logs -f logsearch' command"
}

function setup_profile() {
  if [ -f "$sdir/Profile" ];
  then
    echo "Profile file exists"
  else
    echo "Profile file does not exist, Creating a new one..."
    pushd $sdir/../../
    local AMBARI_LOCATION=$(pwd)
    popd
    cat << EOF > $sdir/Profile
COMPONENT=ALL
COMPONENT_LOG=logsearch
LOGFEEDER_DEBUG_SUSPEND=n
LOGSEARCH_DEBUG_SUSPEND=n
LOGSEARCH_HTTPS_ENABLED=false
LOGSEARCH_SOLR_SSL_ENABLED=false
GENERATE_KEYSTORE_AT_START=false
SOLR_HOST=solr
EOF
    echo "'Profile' file has been created. Check it out before starting Log Search. ($sdir/Profile)"
    exit
  fi;
}

function setup_env() {
  if [ -f "$sdir/.env" ];
  then
    echo ".env file exists"
  else
    echo ".env file does not exist, Creating a new one..."
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

SOLR_VERSION=6.6.2
EOF
    echo ".env file has been created. Check it out before starting Log Search. ($sdir/.env)"
    exit
  fi;
}

function kill_logsearch_container() {
  echo "Try to remove logsearch container if exists..."
  docker rm -f logsearch
}

case $command in
  "build-and-run")
     build_logsearch_project
     build_logsearch_container
     start_logsearch_container
     ;;
  "build")
     build_logsearch_project
     build_logsearch_container
     ;;
  "build-docker-and-run")
     build_logsearch_container
     start_logsearch_container
     ;;
  "build-mvn-and-run")
     build_logsearch_project
     start_logsearch_container
     ;;
  "build-docker-only")
     build_logsearch_container
     ;;
  "build-mvn-only")
     build_logsearch_project
     ;;
  "start")
     start_logsearch_container
     ;;
  "stop")
     kill_logsearch_container
     ;;
   *)
   echo "Available commands: (start|stop|build-and-run|build|build-docker-and-run|build-mvn-and-run|build-docker-only|build-mvn-only)"
   ;;
esac
