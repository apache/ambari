#!/bin/sh
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

dbFile="/var/db/hmc/data/data.db"

if [[ "x" != "x$1" ]]; then
  dbFile=$1
fi

if [[ ! -f ${dbFile} ]]; then
  echo "DB file ${dbFile} does not exist";
  exit 1
fi

while true; do
  read -p "Are you really sure you want to wipe out the DB at ${dbFile}? (y/n)" yn  
  case $yn in
    [Yy]* ) break;;
    [Nn]* ) echo "User aborted script. Exiting without cleaning up DB"; exit 0;;
    * ) echo "Please answer y or n.";;
  esac
done

echo "Deleting data from DB ${dbFile}, restoring to clean state"

sqlite3 ${dbFile} "Delete FROM Clusters;"
sqlite3 ${dbFile} "DELETE FROM ServiceInfo;"
sqlite3 ${dbFile} "DELETE FROM ServiceComponentInfo;"
sqlite3 ${dbFile} "DELETE FROM ServiceConfig;"
sqlite3 ${dbFile} "DELETE FROM Hosts;"
sqlite3 ${dbFile} "DELETE FROM HostRoles;"
sqlite3 ${dbFile} "DELETE FROM HostRoleConfig;"
sqlite3 ${dbFile} "DELETE FROM ConfigHistory;"
sqlite3 ${dbFile} "DELETE FROM TransactionStatus;"
sqlite3 ${dbFile} "DELETE FROM SubTransactionStatus;"

exit 0;
