#
#/**
# * Copyright 2007 The Apache Software Foundation
# *
# * Licensed to the Apache Software Foundation (ASF) under one
# * or more contributor license agreements.  See the NOTICE file
# * distributed with this work for additional information
# * regarding copyright ownership.  The ASF licenses this file
# * to you under the Apache License, Version 2.0 (the
# * "License"); you may not use this file except in compliance
# * with the License.  You may obtain a copy of the License at
# *
# *     http://www.apache.org/licenses/LICENSE-2.0
# *
# * Unless required by applicable law or agreed to in writing, software
# * distributed under the License is distributed on an "AS IS" BASIS,
# * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# * See the License for the specific language governing permissions and
# * limitations under the License.
# */

# Set environment variables here.

# The java implementation to use.  Java 1.6 required.
# export JAVA_HOME=/usr/java/default/

# Extra Java CLASSPATH elements.  Optional.
# export HMS_CLASSPATH=

# The maximum amount of heap to use, in MB. Default is 64.
export HMS_AGENT_HEAPSIZE=64

# Extra Java runtime options.
# Below are what we set by default.  May only work with SUN JVM.
# For more on why as well as other possible settings,
# see http://wiki.apache.org/hadoop/PerformanceTuning
# export HMS_OPTS="-ea -XX:+UseConcMarkSweepGC -XX:+CMSIncrementalMode"

# Uncomment below to enable java garbage collection logging.
# export HMS_OPTS="$HMS_OPTS -verbose:gc -XX:+PrintGCDetails -XX:+PrintGCDateStamps -Xloggc:$HMS_HOME/logs/gc-hms-agent.log" 

# Uncomment and adjust to enable JMX exporting
# See jmxremote.password and jmxremote.access in $JRE_HOME/lib/management to configure remote password access.
# More details at: http://java.sun.com/javase/6/docs/technotes/guides/management/agent.html
#
# export HMS_JMX_BASE="-Dcom.sun.management.jmxremote.ssl=false -Dcom.sun.management.jmxremote.authenticate=false"

# Where log files are stored.  $HMS_HOME/logs by default.
# export HMS_LOG_DIR=${HMS_HOME}/logs

# A string representing this instance of hms agent. $USER by default.
# export HMS_IDENT_STRING=agent

# The scheduling priority for daemon processes.  See 'man nice'.
# export HMS_NICENESS=10

# The directory where pid files are stored. /tmp by default.
# export HMS_PID_DIR=/var/run/

