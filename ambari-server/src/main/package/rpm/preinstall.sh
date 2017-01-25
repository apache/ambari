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

MPACKS_FOLDER="${ROOT}/var/lib/ambari-server/resources/mpacks"
MPACKS_FOLDER_OLD=${ROOT}/var/lib/ambari-server/resources/mpacks_$(date '+%d_%m_%y_%H_%M').old

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
    echo "Backing up configs $SERVER_CONF_SAVE -> $SERVER_CONF_SAVE_BACKUP"
    mv "$SERVER_CONF_SAVE" "$SERVER_CONF_SAVE_BACKUP"
fi

if [ -f "$AMBARI_PROPERTIES" ]
then
    echo "Backing up Ambari properties $AMBARI_PROPERTIES -> $AMBARI_PROPERTIES_OLD"
    cp -n "$AMBARI_PROPERTIES" "$AMBARI_PROPERTIES_OLD"
fi

if [ -f "$AMBARI_ENV" ]
then
    echo "Backing up Ambari properties $AMBARI_ENV -> $AMBARI_ENV_OLD"
    cp -n "$AMBARI_ENV" "$AMBARI_ENV_OLD"
fi

if [ -f "$AMBARI_KRB_JAAS_LOGIN_FILE" ]
then
    echo "Backing up JAAS login file $AMBARI_KRB_JAAS_LOGIN_FILE -> $AMBARI_KRB_JAAS_LOGIN_FILE_OLD"
    cp -n "$AMBARI_KRB_JAAS_LOGIN_FILE" "$AMBARI_KRB_JAAS_LOGIN_FILE_OLD"
fi

if [ -d "$STACKS_FOLDER" ]
then
    echo "Backing up stacks directory $STACKS_FOLDER -> $STACKS_FOLDER_OLD"
    mv -f "$STACKS_FOLDER" "$STACKS_FOLDER_OLD"
fi

if [ -d "$COMMON_SERVICES_FOLDER" ]
then
    echo "Backing up common-services directory $COMMON_SERVICES_FOLDER -> $COMMON_SERVICES_FOLDER_OLD"
    mv -f "$COMMON_SERVICES_FOLDER" "$COMMON_SERVICES_FOLDER_OLD"
fi

if [ -d "$MPACKS_FOLDER" ]
then
    # Make a copy of mpacks folder
    if [ ! -d "$MPACKS_FOLDER_OLD" ]; then
        echo "Backing up mpacks directory $MPACKS_FOLDER -> $MPACKS_FOLDER_OLD"
        cp -R "$MPACKS_FOLDER" "$MPACKS_FOLDER_OLD"
    fi

    # Update symlinks in $STACKS_FOLDER_OLD to point to $MPACKS_FOLDER_OLD
    if [ -d "$STACKS_FOLDER_OLD" ]; then
        for link in $(find "$STACKS_FOLDER_OLD" -type l)
        do
            target=`readlink $link`
            if grep -q "$MPACKS_FOLDER/"<<<$target; then
                new_target="${target/$MPACKS_FOLDER/$MPACKS_FOLDER_OLD}"
                echo "Updating symlink $link -> $new_target"
                ln -snf $new_target $link
            fi
        done
    fi

    # Update symlinks in $COMMON_SERVICES_FOLDER_OLD to point to $MPACKS_FOLDER_OLD
    if [ -d "$COMMON_SERVICES_FOLDER_OLD" ]; then
    for link in $(find "$COMMON_SERVICES_FOLDER_OLD" -type l)
        do
            target=`readlink $link`
            if grep -q "$MPACKS_FOLDER/"<<<$target; then
                new_target="${target/$MPACKS_FOLDER/$MPACKS_FOLDER_OLD}"
                echo "Updating symlink $link -> $new_target"
                ln -snf $new_target $link
            fi
        done
    fi
fi

if [ ! -d "$AMBARI_VIEWS_BACKUP_FOLDER" ] && [ -d "$AMBARI_VIEWS_FOLDER" ]
then
    mkdir "$AMBARI_VIEWS_BACKUP_FOLDER"
fi

ls $AMBARI_VIEWS_FOLDER/*.jar > /dev/null 2>&1
JARS_EXIST="$?"
if [ -d "$AMBARI_VIEWS_FOLDER" ] && [ -d "$AMBARI_VIEWS_BACKUP_FOLDER" ] && [ "$JARS_EXIST" -eq 0 ]
then
    echo "Backing up Ambari view jars $AMBARI_VIEWS_FOLDER/*.jar -> $AMBARI_VIEWS_BACKUP_FOLDER/"
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
        echo "Backing up Ambari server jar $f -> $AMBARI_SERVER_JAR_FILES_BACKUP_FOLDER/"
        mv -f $f $AMBARI_SERVER_JAR_FILES_BACKUP_FOLDER/
    fi
done

exit 0
