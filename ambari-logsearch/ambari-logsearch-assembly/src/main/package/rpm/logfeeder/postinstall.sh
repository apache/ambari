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

LOGFEEDER_SCRIPT_LINK_NAME="/usr/bin/logfeeder"
LOGFEEDER_SCRIPT_SOURCE="/usr/lib/ambari-logsearch-logfeeder/bin/logfeeder.sh"

LOGFEEDER_CONF_LINK="/etc/ambari-logsearch-logfeeder/conf"
LOGFEEDER_CONF_SOURCE="/usr/lib/ambari-logsearch-logfeeder/conf"

ln -s $LOGFEEDER_SCRIPT_SOURCE $LOGFEEDER_SCRIPT_LINK_NAME
ln -s $LOGFEEDER_CONF_SOURCE $LOGFEEDER_CONF_LINK

# handle old checkpoint & keys folder

LOGFEEDER_CONF_BACKUP="/usr/lib/ambari-logsearch-logfeeder/conf-old"

if [ -d "$LOGFEEDER_CONF_BACKUP" ]; then
  if [ -d "$LOGFEEDER_CONF_BACKUP/keys" ]; then
    cp -r $LOGFEEDER_CONF_BACKUP/keys $LOGFEEDER_CONF_SOURCE
  fi
  if [ -d "$LOGFEEDER_CONF_BACKUP/checkpoints" ]; then
    cp -r $LOGFEEDER_CONF_BACKUP/checkpoints $LOGFEEDER_CONF_SOURCE
  fi
fi