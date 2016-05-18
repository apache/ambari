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

# This script is used to delete all the documents in Solr
host_port=localhost:8983
if [ $# -eq 1 ]; then
    host_port=$1
fi
set -x
curl http://${host_port}/solr/hadoop_logs/update --data '<delete><query>*:*</query></delete>' -H 'Content-type:text/xml; charset=utf-8'
curl http://${host_port}/solr/hadoop_logs/update --data '<commit/>' -H 'Content-type:text/xml; charset=utf-8'

curl http://${host_port}/solr/audit_logs/update --data '<delete><query>*:*</query></delete>' -H 'Content-type:text/xml; charset=utf-8'
curl http://${host_port}/solr/audit_logs/update --data '<commit/>' -H 'Content-type:text/xml; charset=utf-8'

