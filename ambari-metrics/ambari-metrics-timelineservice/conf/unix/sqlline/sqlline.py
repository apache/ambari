#!/usr/bin/env python
############################################################################
#
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#
############################################################################

import os
import subprocess
import sys
import phoenix_utils
import atexit
import fnmatch

# Set JAVA_HOME or JDK_HOME before running sqlline.py
# Example: ./sqlline.py localhost:61181:/ams-hbase-unsecure

global childProc
childProc = None
def kill_child():
    if childProc is not None:
        childProc.terminate()
        childProc.kill()
        if os.name != 'nt':
            os.system("reset")
atexit.register(kill_child)

def find_java():
  try:
    java = os.environ.get('JAVA_HOME')
    if not java:
      java = os.environ.get('JDK_HOME')
  except:
    java = None
  if java and os.path.exists(java):
    if java.endswith('bin') or java.endswith('bin/'):
      return os.path.join(java, 'java')
    if java.find('/bin/java') == -1:
      return os.path.join(java, 'bin', 'java')
  else:
    return 'java'

#phoenix_utils.setPath()

if len(sys.argv) < 2:
    print "Zookeeper not specified. \nUsage: sqlline.py <zookeeper> \
<optional_sql_file> \nExample: \n 1. sqlline.py localhost:2181:/hbase \n 2. sqlline.py \
localhost:2181:/hbase ../examples/stock_symbol.sql"
    sys.exit()

sqlfile = ""

if len(sys.argv) > 2:
    sqlfile = "--run=" + phoenix_utils.shell_quote([sys.argv[2]])

colorSetting = "true"
# disable color setting for windows OS
if os.name == 'nt':
    colorSetting = "false"

# Set custom path vars
hbase_conf_dir = os.environ.get('HBASE_CONF_DIR', '/etc/ams-hbase/conf')
hadoop_conf = os.environ.get('HADOOP_CONF_DIR', '/etc/ams-hbase/conf')
hadoop_classpath = os.environ.get('HADOOP_CLASSPATH', '/usr/lib/ams-hbase/lib')

PATH_PREFIX = '/usr/lib/ambari-metrics-collector/'
if not os.path.exists(PATH_PREFIX):
  print 'Fatal Error: ' + PATH_PREFIX + ' not a valid path.'
  sys.exit(1)

current_dir = os.path.dirname(os.path.abspath(__file__))
phoenix_client_path = PATH_PREFIX + '*'

java_cmd = find_java() + ' -cp "' + phoenix_client_path + \
    os.pathsep + hadoop_conf + '" -Dlog4j.configuration=file:' + \
    os.path.join(current_dir, "log4j.properties") + \
    " sqlline.SqlLine -d org.apache.phoenix.jdbc.PhoenixDriver \
-u jdbc:phoenix:" + phoenix_utils.shell_quote([sys.argv[1]]) + \
    " -n none -p none --color=" + colorSetting + " --fastConnect=false --verbose=true \
--isolation=TRANSACTION_READ_COMMITTED " + sqlfile

print 'java command: %s' % str(java_cmd)

childProc = subprocess.Popen(java_cmd, shell=True)
#Wait for child process exit
(output, error) = childProc.communicate()
returncode = childProc.returncode
childProc = None
# Propagate Java return code to this script
if returncode is not None:
    sys.exit(returncode)
