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
# limitations under the License.

echo -e "\033[32mStarting container ambari-rpm-build\033[0m"
if [ `docker inspect --format '{{.State.Running}}' ambari-rpm-build` == true ];then
  docker exec ambari-rpm-build bash -c "pkill -KILL -f maven"
else
  docker start ambari-rpm-build
fi

echo -e "\033[32mCompiling ambari\033[0m"
docker exec ambari-rpm-build bash -c "mvn clean install rpm:rpm -DskipTests -Drat.skip=true"
docker stop ambari-rpm-build

echo -e "\033[32mRestarting ambari-server\033[0m"
docker exec ambari-server bash -c "ambari-server stop"
docker exec ambari-server bash -c "ambari-agent stop"
docker exec ambari-server bash -c "yum -y remove ambari-server"
docker exec ambari-server bash -c "yum -y remove ambari-agent"
docker cp ../../../ambari-server/target/rpm/ambari-server/RPMS/x86_64/ambari-server*.rpm ambari-server:/root/ambari-server.rpm
docker cp ../../../ambari-agent/target/rpm/ambari-agent/RPMS/x86_64/ambari-agent*.rpm ambari-server:/root/ambari-agent.rpm
docker exec ambari-server bash -c "yum -y install /root/ambari-server.rpm"
docker exec ambari-server bash -c "yum -y install /root/ambari-agent.rpm"
docker exec ambari-server bash -c "ambari-server setup --jdbc-db=mysql --jdbc-driver=/usr/share/java/mysql-connector-java.jar"
docker exec ambari-server bash -c "ambari-server setup --java-home=/usr/lib/jvm/java --database=mysql --databasehost=localhost --databaseport=3306 --databasename=ambari --databaseusername=root --databasepassword=root -s"
docker exec ambari-server bash -c "ambari-server restart --debug"
docker exec ambari-server bash -c "ambari-agent restart"

echo -e "\033[32mRestarting ambari-agent-01\033[0m"
docker exec ambari-agent-01 bash -c "ambari-agent stop"
docker exec ambari-agent-01 bash -c "yum -y remove ambari-agent"
docker cp ../../../ambari-agent/target/rpm/ambari-agent/RPMS/x86_64/ambari-agent*.rpm ambari-agent-01:/root/ambari-agent.rpm
docker exec ambari-agent-01 bash -c "yum -y install /root/ambari-agent.rpm"
docker exec ambari-agent-01 bash -c "ambari-agent restart"

echo -e "\033[32mRestarting ambari-agent-02\033[0m"
docker exec ambari-agent-02 bash -c "ambari-agent stop"
docker exec ambari-agent-02 bash -c "yum -y remove ambari-agent"
docker cp ../../../ambari-agent/target/rpm/ambari-agent/RPMS/x86_64/ambari-agent*.rpm ambari-agent-02:/root/ambari-agent.rpm
docker exec ambari-agent-02 bash -c "yum -y install /root/ambari-agent.rpm"
docker exec ambari-agent-02 bash -c "ambari-agent restart"

echo -e "\033[32mDone!\033[0m"