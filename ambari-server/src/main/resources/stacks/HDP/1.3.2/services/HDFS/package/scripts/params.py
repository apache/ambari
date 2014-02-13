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
import status_params
import os

config = Script.get_config()

if System.get_instance().os_type == "oraclelinux":
  ulimit_cmd = ''
else:
  ulimit_cmd = "ulimit -c unlimited; "

#security params
_authentication = config['configurations']['core-site']['hadoop.security.authentication']
security_enabled = ( not is_empty(_authentication) and _authentication == 'kerberos')
dfs_journalnode_keytab_file = config['configurations']['hdfs-site']['dfs.journalnode.keytab.file']
dfs_web_authentication_kerberos_keytab = config['configurations']['hdfs-site']['dfs.journalnode.keytab.file']
dfs_secondary_namenode_keytab_file =  config['configurations']['hdfs-site']['dfs.secondary.namenode.keytab.file']
dfs_datanode_keytab_file =  config['configurations']['hdfs-site']['dfs.datanode.keytab.file']
dfs_namenode_keytab_file =  config['configurations']['hdfs-site']['dfs.namenode.keytab.file']
smoke_user_keytab = config['configurations']['global']['smokeuser_keytab']
hdfs_user_keytab = config['configurations']['global']['hdfs_user_keytab']

dfs_datanode_kerberos_principal = config['configurations']['hdfs-site']['dfs.datanode.kerberos.principal']
dfs_journalnode_kerberos_principal = config['configurations']['hdfs-site']['dfs.journalnode.kerberos.principal']
dfs_secondary_namenode_kerberos_internal_spnego_principal = config['configurations']['hdfs-site']['dfs.secondary.namenode.kerberos.internal.spnego.principal']
dfs_namenode_kerberos_principal = config['configurations']['hdfs-site']['dfs.namenode.kerberos.principal']
dfs_web_authentication_kerberos_principal = config['configurations']['hdfs-site']['dfs.web.authentication.kerberos.principal']
dfs_secondary_namenode_kerberos_principal = config['configurations']['hdfs-site']['dfs.secondary.namenode.kerberos.principal']
dfs_journalnode_kerberos_internal_spnego_principal = config['configurations']['hdfs-site']['dfs.journalnode.kerberos.internal.spnego.principal']

#exclude file
hdfs_exclude_file = default("/clusterHostInfo/decom_dn_hosts", [])
exclude_file_path = config['configurations']['hdfs-site']['dfs.hosts.exclude']
update_exclude_file_only = config['commandParams']['update_exclude_file_only']

kinit_path_local = functions.get_kinit_path([default("kinit_path_local",None), "/usr/bin", "/usr/kerberos/bin", "/usr/sbin"])
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
nm_host = default("/clusterHostInfo/nm_host", [])
ganglia_server_hosts = default("/clusterHostInfo/ganglia_server_host", [])
journalnode_hosts = default("/clusterHostInfo/journalnode_hosts", [])
zkfc_hosts = default("/clusterHostInfo/zkfc_hosts", [])

has_ganglia_server = not len(ganglia_server_hosts) == 0
has_namenodes = not len(namenode_host) == 0
has_jobtracker = not len(jtnode_host) == 0
has_resourcemanager = not len(rm_host) == 0
has_histroryserver = not len(hs_host) == 0
has_hbase_masters = not len(hbase_master_hosts) == 0
has_slaves = not len(slave_hosts) == 0
has_nagios = not len(hagios_server_hosts) == 0
has_oozie_server = not len(oozie_servers)  == 0
has_hcat_server_host = not len(hcat_server_hosts)  == 0
has_hive_server_host = not len(hive_server_host)  == 0
has_journalnode_hosts = not len(journalnode_hosts)  == 0
has_zkfc_hosts = not len(zkfc_hosts)  == 0


is_namenode_master = hostname in namenode_host
is_jtnode_master = hostname in jtnode_host
is_rmnode_master = hostname in rm_host
is_hsnode_master = hostname in hs_host
is_hbase_master = hostname in hbase_master_hosts
is_slave = hostname in slave_hosts

if has_ganglia_server:
  ganglia_server_host = ganglia_server_hosts[0]

#users and groups
yarn_user = config['configurations']['global']['yarn_user']
hbase_user = config['configurations']['global']['hbase_user']
nagios_user = config['configurations']['global']['nagios_user']
oozie_user = config['configurations']['global']['oozie_user']
webhcat_user = config['configurations']['global']['hcat_user']
hcat_user = config['configurations']['global']['hcat_user']
hive_user = config['configurations']['global']['hive_user']
smoke_user =  config['configurations']['global']['smokeuser']
mapred_user = config['configurations']['global']['mapred_user']
hdfs_user = status_params.hdfs_user

user_group = config['configurations']['global']['user_group']
proxyuser_group =  config['configurations']['global']['proxyuser_group']
nagios_group = config['configurations']['global']['nagios_group']
smoke_user_group = "users"

#hadoop params
hadoop_conf_dir = "/etc/hadoop/conf"
hadoop_pid_dir_prefix = status_params.hadoop_pid_dir_prefix
hadoop_bin = "/usr/lib/hadoop/bin"

hdfs_log_dir_prefix = config['configurations']['global']['hdfs_log_dir_prefix']

dfs_domain_socket_path = "/var/lib/hadoop-hdfs/dn_socket"
dfs_domain_socket_dir = os.path.dirname(dfs_domain_socket_path)

hadoop_libexec_dir = "/usr/lib/hadoop/libexec"

jn_edits_dir = config['configurations']['hdfs-site']['dfs.journalnode.edits.dir']

dfs_name_dir = config['configurations']['hdfs-site']['dfs.name.dir']

namenode_dirs_created_stub_dir = format("{hdfs_log_dir_prefix}/{hdfs_user}")
namenode_dirs_stub_filename = "namenode_dirs_created"

smoke_hdfs_user_dir = format("/user/{smoke_user}")
smoke_hdfs_user_mode = 0770

namenode_formatted_mark_dir = format("{hadoop_pid_dir_prefix}/hdfs/namenode/formatted/")

fs_checkpoint_dir = config['configurations']['core-site']['fs.checkpoint.dir']

dfs_data_dir = config['configurations']['hdfs-site']['dfs.data.dir']
#for create_hdfs_directory
hostname = config["hostname"]
hadoop_conf_dir = "/etc/hadoop/conf"
hdfs_user_keytab = config['configurations']['global']['hdfs_user_keytab']
hdfs_user = config['configurations']['global']['hdfs_user']
kinit_path_local = functions.get_kinit_path([default("kinit_path_local",None), "/usr/bin", "/usr/kerberos/bin", "/usr/sbin"])
import functools
#create partial functions with common arguments for every HdfsDirectory call
#to create hdfs directory we need to call params.HdfsDirectory in code
HdfsDirectory = functools.partial(
  HdfsDirectory,
  conf_dir=hadoop_conf_dir,
  hdfs_user=hdfs_user,
  security_enabled = security_enabled,
  keytab = hdfs_user_keytab,
  kinit_path_local = kinit_path_local
)



