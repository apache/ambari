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

JAR_FILES_LEGACY_FOLDER="/usr/lib/ambari-metrics-sink-legacy"

HADOOP_SINK_LINK="/usr/lib/ambari-metrics-hadoop-sink/ambari-metrics-hadoop-sink.jar"

HADOOP_LEGACY_LINK_NAME="/usr/lib/ambari-metrics-hadoop-sink/ambari-metrics-hadoop-sink-legacy.jar"

if [ -f ${HADOOP_SINK_LINK} ]
then
    old_jar=$(readlink -f ${HADOOP_SINK_LINK})
    version_part=$(basename ${old_jar} | awk -F"-" '{print $7}')
    IFS=. version=(${version_part})
    unset IFS

    if [[ ${version[0]} -le 2 && ${version[1]} -lt 7 ]] # backup only required on upgrade from version < 2.7
    then
        if [ ! -d "$JAR_FILES_LEGACY_FOLDER" ]
        then
            mkdir -p "$JAR_FILES_LEGACY_FOLDER"
        fi
        echo "Backing up Ambari metrics hadoop sink jar ${old_jar} -> $JAR_FILES_LEGACY_FOLDER/"
        cp "${old_jar}" "${JAR_FILES_LEGACY_FOLDER}/"

        HADOOP_SINK_LEGACY_JAR="$JAR_FILES_LEGACY_FOLDER/$(basename ${old_jar})"
        echo "Creating symlink for backup jar $HADOOP_LEGACY_LINK_NAME -> $HADOOP_SINK_LEGACY_JAR"
        rm -f "${HADOOP_LEGACY_LINK_NAME}" ; ln -s "${HADOOP_SINK_LEGACY_JAR}" "${HADOOP_LEGACY_LINK_NAME}"
    fi
fi

exit 0
