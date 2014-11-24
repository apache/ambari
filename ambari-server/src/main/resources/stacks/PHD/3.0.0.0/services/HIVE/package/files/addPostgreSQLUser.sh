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

postgresqlservice=$1
postgresqluser=$2
postgresqlpasswd=$3
postgresqldb=$4
postgresqlport=5432

# check if the database has already been created.
check_cmd="psql -U postgres -c \"select datname from pg_catalog.pg_database where datname = '$postgresqldb';\" | grep \"$postgresqldb\" > /dev/null"
eval $check_cmd
if [ $? -eq 0 ]; then
  echo "The database $postgresqldb has already been created. No change is made to the system."
  exit 0
else
  # We need to create a database here, because postgresql grants privileges on objects.
  echo "Creating database \"$postgresqldb\""
  psql -U postgres -p $postgresqlport -c "CREATE DATABASE \"$postgresqldb\";"

  echo "Adding user \"$postgresqluser\""
  psql -U postgres -p $postgresqlport -c "CREATE USER \"$postgresqluser\" WITH NOSUPERUSER NOCREATEDB NOCREATEROLE PASSWORD '$postgresqlpasswd';"
  psql -U postgres -p $postgresqlport -c "GRANT ALL PRIVILEGES ON DATABASE \"$postgresqldb\" TO \"$postgresqluser\";"
fi

