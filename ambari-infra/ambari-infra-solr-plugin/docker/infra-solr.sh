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
: ${1:?"argument is missing: (build|build-docker|start|stop)"}
command="$1"

function build() {
  pushd $sdir/..
  mvn clean install
  popd

  pushd $sdir/../../ambari-infra-assembly
  mvn clean install -Dbuild-rpm
  popd

  build_docker
}

function build_docker() {
  pushd $sdir
  cp ././../../ambari-infra-assembly/target/rpm/ambari-infra-solr/RPMS/noarch/ambari-infra-solr-2.0.0.0-SNAPSHOT.noarch.rpm .
  docker build -t ambari-infra-solr:v1.0 .
  popd
}

function start() {
  pushd $sdir
  docker run -d -p 8983:8983 -p 5005:5005 --name infra-solr ambari-infra-solr:v1.0
  popd
}

function stop() {
  pushd $sdir
  docker kill infra-solr
  docker rm infra-solr
  popd
}

case $command in
  "build")
     build
     ;;
  "build-docker")
     build_docker
     ;;
  "start")
     start
     ;;
  "stop")
     stop
     ;;
   *)
   echo "Available commands: (build|build-docker|start|stop)"
   ;;
esac
