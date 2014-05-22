"""
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

"""

from resource_management import *
from resource_management.core.system import System
import os

config = Script.get_config()

#security params
_authentication = config['configurations']['core-site']['hadoop.security.authentication']
security_enabled = ( not is_empty(_authentication) and _authentication == 'kerberos')
#java params
java_home = config['hostLevelParams']['java_home']
#hadoop params
hadoop_conf_dir = "/etc/hadoop/conf"
hadoop_conf_empty_dir = "/etc/hadoop/conf.empty"
hdfs_log_dir_prefix = config['configurations']['global']['hdfs_log_dir_prefix']
hadoop_pid_dir_prefix = config['configurations']['global']['hadoop_pid_dir_prefix']

#hadoop-env.sh
java_home = config['hostLevelParams']['java_home']

if str(config['hostLevelParams']['stack_version']).startswith('2.0') and System.get_instance().os_family != "suse":
  # deprecated rhel jsvc_path
  jsvc_path = "/usr/libexec/bigtop-utils"
else:
  jsvc_path = "/usr/lib/bigtop-utils"

hadoop_heapsize = config['configurations']['global']['hadoop_heapsize']
namenode_heapsize = config['configurations']['global']['namenode_heapsize']
namenode_opt_newsize =  config['configurations']['global']['namenode_opt_newsize']
namenode_opt_maxnewsize =  config['configurations']['global']['namenode_opt_maxnewsize']

jtnode_opt_newsize = default("jtnode_opt_newsize","200m")
jtnode_opt_maxnewsize = default("jtnode_opt_maxnewsize","200m")
jtnode_heapsize =  default("jtnode_heapsize","1024m")
ttnode_heapsize = "1024m"

dtnode_heapsize = config['configurations']['global']['dtnode_heapsize']
mapred_pid_dir_prefix = default("mapred_pid_dir_prefix","/var/run/hadoop-mapreduce")
mapreduce_libs_path = "/usr/lib/hadoop-mapreduce/*"
hadoop_libexec_dir = "/usr/lib/hadoop/libexec"
mapred_log_dir_prefix = default("mapred_log_dir_prefix","/var/log/hadoop-mapreduce")

#users and groups
hdfs_user = config['configurations']['global']['hdfs_user']
user_group = config['configurations']['global']['user_group']