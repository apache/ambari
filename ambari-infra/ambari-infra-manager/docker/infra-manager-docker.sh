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

function build_infra_manager_container() {
  pushd $sdir
  docker build -t ambari-infra-manager:v1.0 .
  popd
}

function build_infra_manager_project() {
  pushd $sdir/../
  mvn clean package -DskipTests
  popd
}

function kill_infra_manager_container() {
  echo "Try to remove infra manager container if exists ..."
  docker rm -f infra-manager
}

function start_infra_manager_container() {
 echo "Start infra manager container ..."
 pushd $sdir/../
 local AMBARI_INFRA_MANAGER_LOCATION=$(pwd)
 popd
 kill_infra_manager_container
 docker run -d --name infra_manager --hostname infra-manager.apache.org \
   -v $AMBARI_INFRA_MANAGER_LOCATION/target/package:/root/ambari-infra-manager -p 61890:61890 -p 5007:5007 \
   ambari-infra-manager:v1.0
  ip_address=$(docker inspect --format '{{ .NetworkSettings.IPAddress }}' infra_manager)
  echo "Ambari Infra Manager container started on $ip_address (for Mac OSX route to boot2docker/docker-machine VM address, e.g.: 'sudo route add -net 172.17.0.0/16 192.168.59.103')"
  echo "You can follow Log Search logs with 'docker logs -f infra-manager' command"
}

case $command in
  "build-and-run")
     build_infra_manager_project
     build_infra_manager_container
     start_infra_manager_container
     ;;
  "build")
     build_infra_manager_project
     start_infra_manager_container
     ;;
  "build-docker-and-run")
     build_infra_manager_container
     start_infra_manager_container
     ;;
  "build-mvn-and-run")
     build_infra_manager_project
     build_infra_manager_container
     ;;
  "build-docker-only")
     build_infra_manager_container
     ;;
  "build-mvn-only")
     build_infra_manager_project
     ;;
  "start")
     start_infra_manager_container
     ;;
  "stop")
     kill_infra_manager_container
     ;;
   *)
   echo "Available commands: (start|stop|build-and-run|build|build-docker-and-run|build-mvn-and-run|build-docker-only|build-mvn-only)"
   ;;
esac