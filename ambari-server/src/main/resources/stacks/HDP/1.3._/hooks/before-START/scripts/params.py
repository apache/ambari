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
java_home = "/usr/jdk64/jdk1.6.0_31"
artifact_dir = "/tmp/HDP-artifacts/"
jdk_bin = "jdk-6u31-linux-x64.bin"
jce_policy_zip = "jce_policy-6.zip"
jce_location = config['hostLevelParams']['jdk_location']
jdk_location = config['hostLevelParams']['jdk_location']
#security params
security_enabled = config['configurations']['global']['security_enabled']
dfs_journalnode_keytab_file = config['configurations']['hdfs-site']['dfs.journalnode.keytab.file']
dfs_web_authentication_kerberos_keytab = config['configurations']['hdfs-site']['dfs.journalnode.keytab.file']
dfs_secondary_namenode_keytab_file =  config['configurations']['hdfs-site']['fs.secondary.namenode.keytab.file']
dfs_datanode_keytab_file =  config['configurations']['hdfs-site']['dfs.datanode.keytab.file']
dfs_namenode_keytab_file =  config['configurations']['hdfs-site']['dfs.namenode.keytab.file']

dfs_datanode_kerberos_principal = config['configurations']['hdfs-site']['dfs.datanode.kerberos.principal']
dfs_journalnode_kerberos_principal = config['configurations']['hdfs-site']['dfs.journalnode.kerberos.principal']
dfs_secondary_namenode_kerberos_internal_spnego_principal = config['configurations']['hdfs-site']['dfs.secondary.namenode.kerberos.internal.spnego.principal']
dfs_namenode_kerberos_principal = config['configurations']['hdfs-site']['dfs.namenode.kerberos.principal']
dfs_web_authentication_kerberos_principal = config['configurations']['hdfs-site']['dfs.web.authentication.kerberos.principal']
dfs_secondary_namenode_kerberos_principal = config['configurations']['hdfs-site']['dfs.secondary.namenode.kerberos.principal']
dfs_journalnode_kerberos_internal_spnego_principal = config['configurations']['hdfs-site']['dfs.journalnode.kerberos.internal.spnego.principal']

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
hadoop_bin = "/usr/lib/hadoop/bin"

task_log4j_properties_location = os.path.join(hadoop_conf_dir, "task-log4j.properties")
limits_conf_dir = "/etc/security/limits.d"

hdfs_log_dir_prefix = config['configurations']['global']['hdfs_log_dir_prefix']
hbase_tmp_dir = config['configurations']['hbase-site']['hbase.tmp.dir']
#db params
server_db_name = config['hostLevelParams']['db_name']
db_driver_filename = config['hostLevelParams']['db_driver_filename']
oracle_driver_url = config['hostLevelParams']['oracle_jdbc_url']
mysql_driver_url = config['hostLevelParams']['mysql_jdbc_url']

ambari_db_rca_url = config['hostLevelParams']['ambari_db_rca_url']
ambari_db_rca_driver = config['hostLevelParams']['ambari_db_rca_driver']
ambari_db_rca_username = config['hostLevelParams']['ambari_db_rca_username']
ambari_db_rca_password = config['hostLevelParams']['ambari_db_rca_password']

rca_enabled = config['configurations']['global']['rca_enabled']
rca_disabled_prefix = "###"
if rca_enabled == True:
  rca_prefix = ""
else:
  rca_prefix = rca_disabled_prefix

#hadoop-env.sh
java_home = config['configurations']['global']['java64_home']
if System.get_instance().platform == "suse":
  jsvc_path = "/usr/lib/bigtop-utils"
else:
  jsvc_path = "/usr/libexec/bigtop-utils"

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

#exclude file
exlude_file_path = config['configurations']['hdfs-site']['dfs.hosts.exclude']
if 'hdfs-exclude-file' in config['configurations']:
  if 'datanodes' in config['configurations']['hdfs-exclude-file']:
    hdfs_exclude_file = config['configurations']['hdfs-exclude-file']['datanodes'].split(",")
  else:
    hdfs_exclude_file = []
else:
  hdfs_exclude_file = []

#hdfs ha properties
dfs_ha_enabled = False
dfs_ha_nameservices = default("/configurations/hdfs-site/dfs.nameservices", None)
dfs_ha_namenode_ids = default(format("hdfs-site/dfs.ha.namenodes.{dfs_ha_nameservices}"), None)
if dfs_ha_namenode_ids:
  dfs_ha_namenode_ids_array_len = len(dfs_ha_namenode_ids.split(","))
  if dfs_ha_namenode_ids_array_len > 1:
    dfs_ha_enabled = True

if dfs_ha_enabled:
  for nn_id in dfs_ha_namenode_ids:
    nn_host = config['configurations']['hdfs-site'][format('dfs.namenode.rpc-address.{dfs_ha_nameservices}.{nn_id}')]
    if hostname in nn_host:
      namenode_id = nn_id
  namenode_id = None

dfs_hosts = default('/configurations/hdfs-site/dfs.hosts', None)