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

HADOOP_LINK_NAME="/usr/lib/ambari-metrics-hadoop-sink/ambari-metrics-hadoop-sink.jar"
HADOOP_SINK_JAR="/usr/lib/ambari-metrics-hadoop-sink/${hadoop.sink.jar}"

FLUME_LINK_NAME="/usr/lib/flume/lib/ambari-metrics-flume-sink.jar"
FLUME_SINK_JAR="/usr/lib/flume/lib/${flume.sink.jar}"

KAFKA_LINK_NAME="/usr/lib/ambari-metrics-kafka-sink/ambari-metrics-kafka-sink.jar"
KAFKA_SINK_JAR="/usr/lib/ambari-metrics-kafka-sink/${kafka.sink.jar}"

#link for storm jar not required with current loading
#STORM_SINK_JAR="/usr/lib/storm/lib/${storm.sink.jar}"
#STORM_LINK_NAME="/usr/lib/storm/lib/ambari-metrics-storm-sink.jar"

JARS=(${HADOOP_SINK_JAR} ${FLUME_SINK_JAR} ${KAFKA_SINK_JAR})
LINKS=(${HADOOP_LINK_NAME} ${FLUME_LINK_NAME} ${KAFKA_LINK_NAME})

for index in ${!LINKS[*]}
do
  rm -f ${LINKS[$index]} ; ln -s ${JARS[$index]} ${LINKS[$index]}
done
