#!/usr/bin/env bash
#
#
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#   http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.
#
#

mysqldservice=$1
mysqldbuser=$2
mysqldbpasswd=$3
userhost=$4

sudo service $mysqldservice start
echo "Adding user $mysqldbuser@% and removing users with empty name"
sudo su mysql -s /bin/bash - -c "mysql -u root -e \"CREATE USER '$mysqldbuser'@'%' IDENTIFIED BY '$mysqldbpasswd';\""
sudo su mysql -s /bin/bash - -c "mysql -u root -e \"GRANT ALL PRIVILEGES ON *.* TO '$mysqldbuser'@'%';\""
sudo su mysql -s /bin/bash - -c "mysql -u root -e \"DELETE FROM mysql.user WHERE user='';\""
sudo su mysql -s /bin/bash - -c "mysql -u root -e \"flush privileges;\""

sudo service $mysqldservice stop
