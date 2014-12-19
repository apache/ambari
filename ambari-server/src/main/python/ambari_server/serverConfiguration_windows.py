#!/usr/bin/env python

'''
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
'''

import string
import os

JDBC_USE_INTEGRATED_AUTH_PROPERTY = "server.jdbc.use.integrated.auth"

JDBC_RCA_USE_INTEGRATED_AUTH_PROPERTY = "server.jdbc.rca.use.integrated.auth"

JDBC_METRICS_USE_INTEGRATED_AUTH_PROPERTY = "scom.sink.db.use.integrated.auth"

METRICS_PERSISTENCE_TYPE_PROPERTY = "metrics.persistence.type"

JDBC_METRICS_DATABASE_PROPERTY = "scom.sink.db.database"
JDBC_METRICS_HOSTNAME_PROPERTY = "scom.sink.db.hostname"
JDBC_METRICS_PORT_PROPERTY = "scom.sink.db.port"
JDBC_METRICS_SCHEMA_PROPERTY = "scom.sink.db.schema"

JDBC_METRICS_DRIVER_PROPERTY = "scom.sink.db.driver"
JDBC_METRICS_URL_PROPERTY = "scom.sink.db.url"
JDBC_METRICS_USER_NAME_PROPERTY = "scom.sink.db.username"
JDBC_METRICS_PASSWORD_PROPERTY = "scom.sink.db.password"
JDBC_METRICS_PASSWORD_FILENAME = "scom_password.dat"

JDBC_METRICS_PASSWORD_ALIAS = "scom.db.password"

JAVA_SHARE_PATH = "/usr/share/java"
OUT_DIR = "\\var\\log\\ambari-server"
SERVER_OUT_FILE = OUT_DIR + "\\ambari-server.out"
SERVER_LOG_FILE = OUT_DIR + "\\ambari-server.log"
ROOT_FS_PATH = "\\"

JDK_INSTALL_DIR = "C:\\"
JDK_SEARCH_PATTERN = "j[2se|dk|re]*"
JAVA_EXE_SUBPATH = "bin\\java.exe"

# Configuration defaults
DEFAULT_CONF_DIR = "conf"
PID_DIR = "\\var\\run\\ambari-server"
DEFAULT_LIBS_DIR = "lib"

# ownership/permissions mapping
# path - permissions - user - group - recursive
# Rules are executed in the same order as they are listed
# {0} in user/group will be replaced by customized ambari-server username
# The permissions are icacls
NR_ADJUST_OWNERSHIP_LIST = [

  (OUT_DIR, "M", "{0}", True),  #0110-0100-0100 rw-r-r
  (OUT_DIR, "F", "{0}", False), #0111-0101-0101 rwx-rx-rx
  (PID_DIR, "M", "{0}", True),
  (PID_DIR, "F", "{0}", False),
  ("bootstrap", "F", "{0}", False),
  ("ambari-env.cmd", "F", "{0}", False),
  ("keystore", "M", "{0}", True),
  ("keystore", "F", "{0}", False),
  ("keystore\\db", "700", "{0}", False),
  ("keystore\\db\\newcerts", "700", "{0}", False),
  ("resources\\stacks", "755", "{0}", True),
  ("resources\\custom_actions", "755", "{0}", True),
  ("conf", "644", "{0}", True),
  ("conf", "755", "{0}", False),
  ("conf\\password.dat", "640", "{0}", False),
  # Also, conf\password.dat
  # is generated later at store_password_file
]

MASTER_KEY_FILE_PERMISSIONS = "600"
CREDENTIALS_STORE_FILE_PERMISSIONS = "600"
TRUST_STORE_LOCATION_PERMISSIONS = "600"

SCHEMA_UPGRADE_HELPER_CMD = "{0}" + os.sep + "bin" + os.sep + "java -cp {1}" + \
  os.pathsep + "{2} " + \
  "org.apache.ambari.server.upgrade.SchemaUpgradeHelper" + \
  " > " + SERVER_OUT_FILE + " 2>&1"

STACK_UPGRADE_HELPER_CMD = "{0}" + os.sep + "bin" + os.sep + "java -cp {1}" + \
                           os.pathsep + "{2} " + \
                           "org.apache.ambari.server.upgrade.StackUpgradeHelper" + \
                           " {3} {4} > " + SERVER_OUT_FILE + " 2>&1"
