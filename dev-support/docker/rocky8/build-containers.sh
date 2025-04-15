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
if [[ -z $(docker ps -a --format "table {{.Names}}" | grep "ambari-rpm-build") ]];then
  docker run -it -d --name ambari-rpm-build --privileged=true -e "container=docker" \
    -v /sys/fs/cgroup:/sys/fs/cgroup:ro -v $PWD/../../../:/opt/ambari/ \
    -w /opt/ambari \
    ambari/develop:trunk-rocky-8
else
  docker start ambari-rpm-build
fi

echo -e "\033[32mCompiling ambari\033[0m"
docker exec ambari-rpm-build bash -c "mvn clean install rpm:rpm -DskipTests -Drat.skip=true"
docker stop ambari-rpm-build

echo -e "\033[32mCreating network ambari\033[0m"
docker network create --driver bridge ambari

echo -e "\033[32mCreating container ambari-server\033[0m"
docker run -d -p 5005:5005 -p 8080:8080 -p 8440:8440 -p 8441:8441 -p 8670:8670 --name ambari-server --hostname ambari-server --network ambari --privileged -e "container=docker" -v /sys/fs/cgroup:/sys/fs/cgroup:ro ambari/develop:trunk-rocky-8 /usr/sbin/init
docker cp ../../../ambari-server/target/rpm/ambari-server/RPMS/x86_64/ambari-server*.rpm ambari-server:/root/ambari-server.rpm
docker cp ../../../ambari-agent/target/rpm/ambari-agent/RPMS/x86_64/ambari-agent*.rpm ambari-server:/root/ambari-agent.rpm
SERVER_PUB_KEY=`docker exec ambari-server /bin/cat /root/.ssh/id_rsa.pub`
docker exec ambari-server bash -c "yum -y install /root/ambari-server.rpm"
docker exec ambari-server bash -c "yum -y install /root/ambari-agent.rpm"
docker exec ambari-server bash -c "chmod a+x /etc/init.d/ambari-agent"
docker exec ambari-server bash -c "ambari-agent start"
docker exec ambari-server bash -c "echo '$SERVER_PUB_KEY' > /root/.ssh/authorized_keys"
# Install a wrapper to workaround systemctl on docker
docker exec ambari-server bash -c "wget https://raw.githubusercontent.com/gdraheim/docker-systemctl-replacement/master/files/docker/systemctl3.py -O /usr/local/bin/systemctl"
docker exec ambari-server bash -c "chmod a+x /usr/local/bin/systemctl"
docker exec ambari-server systemctl enable sshd
docker exec ambari-server systemctl start sshd

echo -e "\033[32mSetting up mariadb-server\033[0m"
docker run --name ambari-mariadb --network ambari -e  MYSQL_ROOT_PASSWORD=root -p 3306:3306 -d docker.io/library/mariadb:10.4
sleep 10
docker exec ambari-mariadb bash -c "mysql -proot -e \"UPDATE mysql.user SET Password = PASSWORD('root') WHERE User = 'root'\""
docker exec ambari-mariadb bash -c "mysql -proot -e \"GRANT ALL PRIVILEGES ON *.* TO 'root'@'%' IDENTIFIED BY 'root' WITH GRANT OPTION\""
docker exec ambari-mariadb bash -c "mysql -proot -e \"DROP USER ''@'localhost'\""
docker exec ambari-mariadb bash -c "mysql -proot -e \"DROP USER ''@'ambari-server'\""
docker exec ambari-mariadb bash -c "mysql -proot -e \"DROP DATABASE test\""
docker exec ambari-mariadb bash -c "mysql -proot -e \"CREATE DATABASE ambari\""
docker exec ambari-server bash -c "mysql -proot -h ambari-mariadb --database=ambari -e  \"source /var/lib/ambari-server/resources/Ambari-DDL-MySQL-CREATE.sql\""

docker exec ambari-mariadb bash -c "mysql -proot -e \"CREATE USER 'hive'@'%' IDENTIFIED BY 'hive'\""
docker exec ambari-mariadb bash -c "mysql -proot -e \"GRANT ALL PRIVILEGES ON *.* TO 'hive'@'%' IDENTIFIED BY 'hive'\""
docker exec ambari-mariadb bash -c "mysql -proot -e \"CREATE DATABASE hive\""

docker exec ambari-mariadb bash -c "mysql -proot -e \"FLUSH PRIVILEGES\""

echo -e "\033[32mSetting up ambari-server\033[0m"
docker exec ambari-server bash -c "ambari-server setup --jdbc-db=mysql --jdbc-driver=/usr/share/java/mysql-connector-java.jar"
docker exec ambari-server bash -c "ambari-server setup --java-home=/usr/lib/jvm/java-1.8.0 --ambari-java-home=/usr/lib/jvm/java-17 --database=mysql --databasehost=ambari-mariadb --databaseport=3306 --databasename=ambari --databaseusername=root --databasepassword=root -s"

echo -e "\033[32mCreating container ambari-agent-01\033[0m"
docker run -d -p 9995:9995 --name ambari-agent-01 --hostname ambari-agent-01 --network ambari --privileged -e "container=docker" -v /sys/fs/cgroup:/sys/fs/cgroup:ro ambari/develop:trunk-rocky-8 /usr/sbin/init
docker cp ../../../ambari-agent/target/rpm/ambari-agent/RPMS/x86_64/ambari-agent*.rpm ambari-agent-01:/root/ambari-agent.rpm
docker exec ambari-agent-01 bash -c "yum -y install /root/ambari-agent.rpm"
docker exec ambari-agent-01 bash -c "chmod a+x /etc/init.d/ambari-agent"
docker exec ambari-agent-01 bash -c "ambari-agent start"
docker exec ambari-agent-01 bash -c "echo '$SERVER_PUB_KEY' > /root/.ssh/authorized_keys"
docker exec ambari-agent-01 bash -c "wget https://raw.githubusercontent.com/gdraheim/docker-systemctl-replacement/master/files/docker/systemctl3.py -O /usr/local/bin/systemctl"
docker exec ambari-agent-01 bash -c "chmod a+x /usr/local/bin/systemctl"
docker exec ambari-agent-01 systemctl enable sshd
docker exec ambari-agent-01 systemctl start sshd

echo -e "\033[32mCreating container ambari-agent-02\033[0m"
docker run -d -p 8088:8088 --name ambari-agent-02 --hostname ambari-agent-02 --network ambari --privileged -e "container=docker" -v /sys/fs/cgroup:/sys/fs/cgroup:ro ambari/develop:trunk-rocky-8 /usr/sbin/init
docker cp ../../../ambari-agent/target/rpm/ambari-agent/RPMS/x86_64/ambari-agent*.rpm ambari-agent-02:/root/ambari-agent.rpm
docker exec ambari-agent-02 bash -c "yum -y install /root/ambari-agent.rpm"
docker exec ambari-agent-02 bash -c "chmod a+x /etc/init.d/ambari-agent"
docker exec ambari-agent-02 bash -c "ambari-agent start"
docker exec ambari-agent-02 bash -c "echo '$SERVER_PUB_KEY' > /root/.ssh/authorized_keys"
docker exec ambari-agent-02 bash -c "wget https://raw.githubusercontent.com/gdraheim/docker-systemctl-replacement/master/files/docker/systemctl3.py -O /usr/local/bin/systemctl"
docker exec ambari-agent-02 bash -c "chmod a+x /usr/local/bin/systemctl"
docker exec ambari-agent-02 systemctl enable sshd
docker exec ambari-agent-02 systemctl start sshd

echo -e "\033[32mConfiguring hosts file\033[0m"
AMBARI_SERVER_IP=`docker inspect -f '{{range .NetworkSettings.Networks}}{{.IPAddress}}{{end}}' ambari-server`
AMBARI_AGENT_01_IP=`docker inspect -f '{{range .NetworkSettings.Networks}}{{.IPAddress}}{{end}}' ambari-agent-01`
AMBARI_AGENT_02_IP=`docker inspect -f '{{range .NetworkSettings.Networks}}{{.IPAddress}}{{end}}' ambari-agent-02`
docker exec ambari-server bash -c "echo '$AMBARI_AGENT_01_IP      ambari-agent-01' >> /etc/hosts"
docker exec ambari-server bash -c "echo '$AMBARI_AGENT_02_IP      ambari-agent-02' >> /etc/hosts"
docker exec ambari-agent-01 bash -c "echo '$AMBARI_SERVER_IP      ambari-server' >> /etc/hosts"
docker exec ambari-agent-01 bash -c "echo '$AMBARI_AGENT_02_IP      ambari-agent-02' >> /etc/hosts"
docker exec ambari-agent-02 bash -c "echo '$AMBARI_SERVER_IP      ambari-server' >> /etc/hosts"
docker exec ambari-agent-02 bash -c "echo '$AMBARI_AGENT_01_IP      ambari-agent-01' >> /etc/hosts"


echo -e "\033[32mConfiguring Kerberos\033[0m"
docker cp ./krb5.conf ambari-server:/etc/krb5.conf
docker cp ./krb5.conf ambari-agent-01:/etc/krb5.conf
docker cp ./krb5.conf ambari-agent-02:/etc/krb5.conf
docker exec ambari-server bash -c "echo -e \"admin\nadmin\" | kdb5_util create -s -r EXAMPLE.COM"
docker exec ambari-server bash -c "echo -e \"admin\nadmin\" | kadmin.local -q \"addprinc admin/admin\""
docker exec ambari-server bash -c "/usr/local/bin/systemctl start krb5kdc"
docker exec ambari-server bash -c "/usr/local/bin/systemctl enable krb5kdc"
docker exec ambari-server bash -c "/usr/local/bin/systemctl start kadmin"
docker exec ambari-server bash -c "/usr/local/bin/systemctl enable kadmin"

echo -e "\033[32mSynchronize Chrony\033[0m"
docker exec ambari-server bash -c "/usr/local/bin/systemctl enable chronyd; /usr/local/bin/systemctl start chronyd; chronyc tracking"
docker exec ambari-agent-01 bash -c "/usr/local/bin/systemctl enable chronyd; /usr/local/bin/systemctl start chronyd; chronyc tracking"
docker exec ambari-agent-02 bash -c "/usr/local/bin/systemctl enable chronyd; /usr/local/bin/systemctl start chronyd; chronyc tracking"

docker exec ambari-server bash -c "ambari-server restart --debug"

echo -e "\033[32mPrint Ambari Server RSA Private Key\033[0m"
docker exec ambari-server bash -c "cat ~/.ssh/id_rsa"

# KDC HOST: ambari-server
# REALM NAME: EXAMPLE.COM
# ADMIN PRINCIPAL: admin/admin@EXAMPLE.COM
# ADMIN PASSWORD: admin

# MySQL HOST: ambari-server
# MySQL PORT: 3306
# DATABASE NAME: hive
# DATABASE USER NAME: hive
# DATABASE PASSWORD: hive
