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

LOGSEARCH_SCRIPT_LINK_NAME="/usr/bin/logsearch"
LOGSEARCH_SCRIPT_SOURCE="/usr/lib/ambari-logsearch-portal/bin/logsearch.sh"

LOGSEARCH_ETC_FOLDER="/etc/ambari-logsearch-portal"
LOGSEARCH_CONF_LINK="$LOGSEARCH_ETC_FOLDER/conf"
LOGSEARCH_CONF_SOURCE="/usr/lib/ambari-logsearch-portal/conf"

mkdir -p $LOGSEARCH_ETC_FOLDER

ln -s $LOGSEARCH_SCRIPT_SOURCE $LOGSEARCH_SCRIPT_LINK_NAME
#ln -s $LOGSEARCH_CONF_SOURCE $LOGSEARCH_CONF_LINK

# handle old keys folder

LOGSEARCH_CONF_BACKUP="/usr/lib/ambari-logsearch-portal/conf-old"

if [ -d "$LOGSEARCH_CONF_BACKUP" ]; then
  if [ -d "$LOGSEARCH_CONF_BACKUP/keys" ]; then
    cp -r $LOGSEARCH_CONF_BACKUP/keys $LOGSEARCH_CONF_SOURCE
  fi
fi