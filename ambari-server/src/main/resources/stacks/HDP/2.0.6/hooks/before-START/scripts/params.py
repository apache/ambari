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

#java params
artifact_dir = "/tmp/HDP-artifacts/"
jdk_name = default("/hostLevelParams/jdk_name", None) # None when jdk is already installed by user
jce_policy_zip = default("/hostLevelParams/jce_name", None) # None when jdk is already installed by user
jce_location = config['hostLevelParams']['jdk_location']
jdk_location = config['hostLevelParams']['jdk_location']
#security params
_authentication = config['configurations']['core-site']['hadoop.security.authentication']
security_enabled = ( not is_empty(_authentication) and _authentication == 'kerberos')

#users and groups
mapred_user = config['configurations']['global']['mapred_user']
hdfs_user = config['configurations']['global']['hdfs_user']
yarn_user = config['configurations']['global']['yarn_user']

user_group = config['configurations']['global']['user_group']
mapred_tt_group = default("/configurations/mapred-site/mapreduce.tasktracker.group", user_group)

#snmp
snmp_conf_dir = "/etc/snmp/"
snmp_source = "0.0.0.0/0"
snmp_community = "hadoop"

#hosts
hostname = config["hostname"]
rm_host = default("/clusterHostInfo/rm_host", [])
slave_hosts = default("/clusterHostInfo/slave_hosts", [])
hagios_server_hosts = default("/clusterHostInfo/nagios_server_host", [])
oozie_servers = default("/clusterHostInfo/oozie_server", [])
hcat_server_hosts = default("/clusterHostInfo/webhcat_server_host", [])
hive_server_host =  default("/clusterHostInfo/hive_server_host", [])
hbase_master_hosts = default("/clusterHostInfo/hbase_master_hosts", [])
hs_host = default("/clusterHostInfo/hs_host", [])
jtnode_host = default("/clusterHostInfo/jtnode_host", [])
namenode_host = default("/clusterHostInfo/namenode_host", [])
zk_hosts = default("/clusterHostInfo/zookeeper_hosts", [])
ganglia_server_hosts = default("/clusterHostInfo/ganglia_server_host", [])

has_resourcemanager = not len(rm_host) == 0
has_slaves = not len(slave_hosts) == 0
has_nagios = not len(hagios_server_hosts) == 0
has_oozie_server = not len(oozie_servers)  == 0
has_hcat_server_host = not len(hcat_server_hosts)  == 0
has_hive_server_host = not len(hive_server_host)  == 0
has_hbase_masters = not len(hbase_master_hosts) == 0
has_zk_host = not len(zk_hosts) == 0
has_ganglia_server = not len(ganglia_server_hosts) == 0

is_namenode_master = hostname in namenode_host
is_jtnode_master = hostname in jtnode_host
is_rmnode_master = hostname in rm_host
is_hsnode_master = hostname in hs_host
is_hbase_master = hostname in hbase_master_hosts
is_slave = hostname in slave_hosts
if has_ganglia_server:
  ganglia_server_host = ganglia_server_hosts[0]
#hadoop params
hadoop_tmp_dir = format("/tmp/hadoop-{hdfs_user}")
hadoop_lib_home = "/usr/lib/hadoop/lib"
hadoop_conf_dir = "/etc/hadoop/conf"
hadoop_pid_dir_prefix = config['configurations']['global']['hadoop_pid_dir_prefix']
hadoop_home = "/usr"
hadoop_bin = "/usr/lib/hadoop/sbin"

task_log4j_properties_location = os.path.join(hadoop_conf_dir, "task-log4j.properties")
limits_conf_dir = "/etc/security/limits.d"

hdfs_log_dir_prefix = config['configurations']['global']['hdfs_log_dir_prefix']
hbase_tmp_dir = config['configurations']['hbase-site']['hbase.tmp.dir']
#db params
server_db_name = config['hostLevelParams']['db_name']
db_driver_filename = config['hostLevelParams']['db_driver_filename']
oracle_driver_url = config['hostLevelParams']['oracle_jdbc_url']
mysql_driver_url = config['hostLevelParams']['mysql_jdbc_url']

ambari_db_rca_url = config['hostLevelParams']['ambari_db_rca_url'][0]
ambari_db_rca_driver = config['hostLevelParams']['ambari_db_rca_driver'][0]
ambari_db_rca_username = config['hostLevelParams']['ambari_db_rca_username'][0]
ambari_db_rca_password = config['hostLevelParams']['ambari_db_rca_password'][0]

if 'rca_enabled' in config['configurations']['global']:
  rca_enabled =  config['configurations']['global']['rca_enabled']
else:
  rca_enabled = False
rca_disabled_prefix = "###"
if rca_enabled == True:
  rca_prefix = ""
else:
  rca_prefix = rca_disabled_prefix

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

#taskcontroller.cfg

mapred_local_dir = "/tmp/hadoop-mapred/mapred/local"

#log4j.properties

yarn_log_dir_prefix = default("yarn_log_dir_prefix","/var/log/hadoop-yarn")

dfs_hosts = default('/configurations/hdfs-site/dfs.hosts', None)

#log4j.properties
if (('hdfs-log4j' in config['configurations']) and ('content' in config['configurations']['hdfs-log4j'])):
  log4j_props = config['configurations']['hdfs-log4j']['content']
  if (('yarn-log4j' in config['configurations']) and ('content' in config['configurations']['yarn-log4j'])):
    log4j_props += config['configurations']['yarn-log4j']['content']
else:
  log4j_props = None
