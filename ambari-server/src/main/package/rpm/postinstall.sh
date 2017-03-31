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

# Warning: don't add changes to this script directly, please add changes to install-helper.sh.

INSTALL_HELPER="${RPM_INSTALL_PREFIX}/var/lib/ambari-server/install-helper.sh"

AMBARI_SERVER_KEYS_FOLDER="${ROOT}/var/lib/ambari-server/keys"
AMBARI_SERVER_KEYS_DB_FOLDER="${ROOT}/var/lib/ambari-server/keys/db"
AMBARI_SERVER_NEWCERTS_FOLDER="${ROOT}/var/lib/ambari-server/keys/db/newcerts"

case "$1" in
  1) # Action install
    if [ -f "$INSTALL_HELPER" ]; then
        $INSTALL_HELPER install
    fi
  ;;
  2) # Action upgrade
    if [ -f "$INSTALL_HELPER" ]; then
        $INSTALL_HELPER upgrade
    fi
  ;;
esac

if [ -d "$AMBARI_SERVER_KEYS_FOLDER" ]
then
    chmod 700 "$AMBARI_SERVER_KEYS_FOLDER"
    if [ -d "$AMBARI_SERVER_KEYS_DB_FOLDER" ]
    then
        chmod 700 "$AMBARI_SERVER_KEYS_DB_FOLDER"
        if [ -d "$AMBARI_SERVER_NEWCERTS_FOLDER" ]
        then
            chmod 700 "$AMBARI_SERVER_NEWCERTS_FOLDER"

        fi
    fi
fi

exit 0
