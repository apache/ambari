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

JAVA_SHARE_PATH = "/usr/share/java"
OUT_DIR = "/var/log/ambari-server"
SERVER_OUT_FILE = OUT_DIR + "/ambari-server.out"
SERVER_LOG_FILE = OUT_DIR + "/ambari-server.log"
ROOT_FS_PATH = "/"

# JDK
JDK_NAMES = ["jdk-7u45-linux-x64.tar.gz", "jdk-6u31-linux-x64.bin"]
DEFAULT_JDK16_LOCATION = "/usr/jdk64/jdk1.6.0_31"
JDK_INSTALL_DIR = "/usr/jdk64"
JDK_SEARCH_PATTERN = "jdk*"
JAVA_EXE_SUBPATH = "bin/java"

# Configuration defaults
DEFAULT_CONF_DIR = "/etc/ambari-server/conf"
PID_DIR = "/var/run/ambari-server"
DEFAULT_LIBS_DIR = "/usr/lib/ambari-server"

# ownership/permissions mapping
# path - permissions - user - group - recursive
# Rules are executed in the same order as they are listed
# {0} in user/group will be replaced by customized ambari-server username
NR_ADJUST_OWNERSHIP_LIST = [

  ("/var/log/ambari-server", "644", "{0}", True),
  ("/var/log/ambari-server", "755", "{0}", False),
  ("/var/run/ambari-server", "644", "{0}", True),
  ("/var/run/ambari-server", "755", "{0}", False),
  ("/var/run/ambari-server/bootstrap", "755", "{0}", False),
  ("/var/lib/ambari-server/ambari-env.sh", "700", "{0}", False),
  ("/var/lib/ambari-server/keys", "600", "{0}", True),
  ("/var/lib/ambari-server/keys", "700", "{0}", False),
  ("/var/lib/ambari-server/keys/db", "700", "{0}", False),
  ("/var/lib/ambari-server/keys/db/newcerts", "700", "{0}", False),
  ("/var/lib/ambari-server/keys/.ssh", "700", "{0}", False),
  ("/var/lib/ambari-server/resources/stacks/", "755", "{0}", True),
  ("/var/lib/ambari-server/resources/custom_actions/", "755", "{0}", True),
  ("/etc/ambari-server/conf", "644", "{0}", True),
  ("/etc/ambari-server/conf", "755", "{0}", False),
  ("/etc/ambari-server/conf/password.dat", "640", "{0}", False),
  # Also, /etc/ambari-server/conf/password.dat
  # is generated later at store_password_file
]

MASTER_KEY_FILE_PERMISSIONS = "600"
CREDENTIALS_STORE_FILE_PERMISSIONS = "600"
TRUST_STORE_LOCATION_PERMISSIONS = "600"
