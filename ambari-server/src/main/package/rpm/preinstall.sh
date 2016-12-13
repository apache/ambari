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

ROOT=`echo "${RPM_INSTALL_PREFIX}" | sed 's|/$||g'`

STACKS_FOLDER="${ROOT}/var/lib/ambari-server/resources/stacks"
STACKS_FOLDER_OLD="${ROOT}/var/lib/ambari-server/resources/stacks_$(date '+%d_%m_%y_%H_%M').old"

COMMON_SERVICES_FOLDER=${ROOT}"/var/lib/ambari-server/resources/common-services"
COMMON_SERVICES_FOLDER_OLD="${ROOT}/var/lib/ambari-server/resources/common-services_$(date '+%d_%m_%y_%H_%M').old"

AMBARI_PROPERTIES="${ROOT}/etc/ambari-server/conf/ambari.properties"
AMBARI_PROPERTIES_OLD="$AMBARI_PROPERTIES.rpmsave"

AMBARI_ENV="${ROOT}/var/lib/ambari-server/ambari-env.sh"
AMBARI_ENV_OLD="$AMBARI_ENV.rpmsave"

AMBARI_KRB_JAAS_LOGIN_FILE="${ROOT}/etc/ambari-server/conf/krb5JAASLogin.conf"
AMBARI_KRB_JAAS_LOGIN_FILE_OLD="$AMBARI_KRB_JAAS_LOGIN_FILE.rpmsave"

AMBARI_VIEWS_FOLDER="${ROOT}/var/lib/ambari-server/resources/views"
AMBARI_VIEWS_BACKUP_FOLDER="$AMBARI_VIEWS_FOLDER/backups"

AMBARI_SERVER_JAR_FILES="/usr/lib/ambari-server/ambari-server-*.jar"
AMBARI_SERVER_JAR_FILES_BACKUP_FOLDER="/usr/lib/ambari-server-backups"
SERVER_CONF_SAVE="${ROOT}/etc/ambari-server/conf.save"
SERVER_CONF_SAVE_BACKUP="${ROOT}/etc/ambari-server/conf_$(date '+%d_%m_%y_%H_%M').save"

if [ -d "$SERVER_CONF_SAVE" ]
then
    mv "$SERVER_CONF_SAVE" "$SERVER_CONF_SAVE_BACKUP"
fi

if [ -f "$AMBARI_PROPERTIES" ]
then
    cp -n "$AMBARI_PROPERTIES" "$AMBARI_PROPERTIES_OLD"
fi

if [ -f "$AMBARI_ENV" ]
then
    cp -n "$AMBARI_ENV" "$AMBARI_ENV_OLD"
fi

if [ -f "$AMBARI_KRB_JAAS_LOGIN_FILE" ]
then
    cp -n "$AMBARI_KRB_JAAS_LOGIN_FILE" "$AMBARI_KRB_JAAS_LOGIN_FILE_OLD"
fi

if [ -d "$STACKS_FOLDER" ]
then
    mv -f "$STACKS_FOLDER" "$STACKS_FOLDER_OLD"
fi

if [ -d "$COMMON_SERVICES_FOLDER" ]
then
    mv -f "$COMMON_SERVICES_FOLDER" "$COMMON_SERVICES_FOLDER_OLD"
fi

if [ ! -d "$AMBARI_VIEWS_BACKUP_FOLDER" ] && [ -d "$AMBARI_VIEWS_FOLDER" ]
then
    mkdir "$AMBARI_VIEWS_BACKUP_FOLDER"
fi

ls $AMBARI_VIEWS_FOLDER/*.jar > /dev/null 2>&1
JARS_EXIST="$?"
if [ -d "$AMBARI_VIEWS_FOLDER" ] && [ -d "$AMBARI_VIEWS_BACKUP_FOLDER" ] && [ "$JARS_EXIST" -eq 0 ]
then
    cp -u $AMBARI_VIEWS_FOLDER/*.jar $AMBARI_VIEWS_BACKUP_FOLDER/
fi

for f in $AMBARI_SERVER_JAR_FILES;
do
    if [ -f "$f" ]
    then
        if [ ! -d "$AMBARI_SERVER_JAR_FILES_BACKUP_FOLDER" ]
        then
            mkdir -p "$AMBARI_SERVER_JAR_FILES_BACKUP_FOLDER"
        fi
        mv -f $f $AMBARI_SERVER_JAR_FILES_BACKUP_FOLDER/
    fi
done

exit 0
