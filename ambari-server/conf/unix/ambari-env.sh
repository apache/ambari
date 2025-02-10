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
# Exit immediately if a command exits with a non-zero status
set -e
# Set Ambari passphrase
AMBARI_PASSPHRASE="DEV"

# Set JVM arguments for Ambari
AMBARI_JVM_ARGS="--add-opens java.base/java.lang=ALL-UNNAMED "
AMBARI_JVM_ARGS+="--add-opens java.base/java.util.regex=ALL-UNNAMED "
AMBARI_JVM_ARGS+="--add-opens java.base/java.util=ALL-UNNAMED "
AMBARI_JVM_ARGS+="--add-opens java.base/java.lang.reflect=ALL-UNNAMED "
AMBARI_JVM_ARGS+="-Xms512m -Xmx2048m "
AMBARI_JVM_ARGS+="-Djava.security.auth.login.config=$ROOT/etc/ambari-server/conf/krb5JAASLogin.conf "
AMBARI_JVM_ARGS+="-Djava.security.krb5.conf=/etc/krb5.conf "
AMBARI_JVM_ARGS+="-Djavax.security.auth.useSubjectCredsOnly=false "
AMBARI_JVM_ARGS+="-Dcom.sun.jndi.ldap.connect.pool.protocol=\"plain ssl\" "
AMBARI_JVM_ARGS+="-Dcom.sun.jndi.ldap.connect.pool.maxsize=20 "
AMBARI_JVM_ARGS+="-Dcom.sun.jndi.ldap.connect.pool.timeout=300000"
export AMBARI_JVM_ARGS

# Update PATH to include Ambari server directory
export PATH="$PATH:$ROOT/var/lib/ambari-server"

# Set Python path for Ambari server
export PYTHONPATH="/usr/lib/ambari-server/lib:$PYTHONPATH"

# Additional server classpath can be set using SERVER_CLASSPATH
# Uncomment the following line to add additional directories or jars
# export SERVER_CLASSPATH=/etc/hadoop/conf/secure