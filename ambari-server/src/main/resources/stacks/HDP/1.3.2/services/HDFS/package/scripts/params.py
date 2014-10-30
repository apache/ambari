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
import itertools

config = Script.get_config()
tmp_dir = Script.get_tmp_dir()

ulimit_cmd = "ulimit -c unlimited; "

#security params
security_enabled = config['configurations']['cluster-env']['security_enabled']
smoke_user_keytab = config['configurations']['cluster-env']['smokeuser_keytab']
hdfs_user_keytab = config['configurations']['hadoop-env']['hdfs_user_keytab']

#exclude file
hdfs_exclude_file = default("/clusterHostInfo/decom_dn_hosts", [])
exclude_file_path = config['configurations']['hdfs-site']['dfs.hosts.exclude']
update_exclude_file_only = config['commandParams']['update_exclude_file_only']

kinit_path_local = functions.get_kinit_path(["/usr/bin", "/usr/kerberos/bin", "/usr/sbin"])
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
hbase_user = config['configurations']['hbase-env']['hbase_user']
nagios_user = config['configurations']['nagios-env']['nagios_user']
oozie_user = config['configurations']['oozie-env']['oozie_user']
webhcat_user = config['configurations']['hive-env']['hcat_user']
hcat_user = config['configurations']['hive-env']['hcat_user']
hive_user = config['configurations']['hive-env']['hive_user']
smoke_user =  config['configurations']['cluster-env']['smokeuser']
mapred_user = config['configurations']['mapred-env']['mapred_user']
hdfs_user = status_params.hdfs_user

user_group = config['configurations']['cluster-env']['user_group']
proxyuser_group =  config['configurations']['hadoop-env']['proxyuser_group']
nagios_group = config['configurations']['nagios-env']['nagios_group']

#hadoop params
hadoop_conf_dir = "/etc/hadoop/conf"
hadoop_pid_dir_prefix = status_params.hadoop_pid_dir_prefix
hadoop_bin = "/usr/lib/hadoop/bin"

hdfs_log_dir_prefix = config['configurations']['hadoop-env']['hdfs_log_dir_prefix']
hadoop_root_logger = config['configurations']['hadoop-env']['hadoop_root_logger']

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
data_dir_mount_file = config['configurations']['hadoop-env']['dfs.datanode.data.dir.mount.file']

#for create_hdfs_directory
hostname = config["hostname"]
hadoop_conf_dir = "/etc/hadoop/conf"
hdfs_user_keytab = config['configurations']['hadoop-env']['hdfs_user_keytab']
hdfs_user = config['configurations']['hadoop-env']['hdfs_user']
hdfs_principal_name = config['configurations']['hadoop-env']['hdfs_principal_name']
kinit_path_local = functions.get_kinit_path(["/usr/bin", "/usr/kerberos/bin", "/usr/sbin"])
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
limits_conf_dir = "/etc/security/limits.d"

io_compression_codecs = config['configurations']['core-site']['io.compression.codecs']
lzo_enabled = "com.hadoop.compression.lzo" in io_compression_codecs

lzo_packages_to_family = {
  "any": ["hadoop-lzo"],
  "redhat": ["lzo", "hadoop-lzo-native"],
  "suse": ["lzo", "hadoop-lzo-native"],
  "ubuntu": ["liblzo2-2"]
}
lzo_packages_for_current_host = lzo_packages_to_family['any'] + lzo_packages_to_family[System.get_instance().os_family]
all_lzo_packages = set(itertools.chain(*lzo_packages_to_family.values()))
 
exclude_packages = []
if not lzo_enabled:
  exclude_packages += all_lzo_packages

java_home = config['hostLevelParams']['java_home']
#hadoop params

hadoop_conf_empty_dir = "/etc/hadoop/conf.empty"
hadoop_env_sh_template = config['configurations']['hadoop-env']['content']

#hadoop-env.sh
if System.get_instance().os_family == "suse":
  jsvc_path = "/usr/lib/bigtop-utils"
else:
  jsvc_path = "/usr/libexec/bigtop-utils"
hadoop_heapsize = config['configurations']['hadoop-env']['hadoop_heapsize']
namenode_heapsize = config['configurations']['hadoop-env']['namenode_heapsize']
namenode_opt_newsize = config['configurations']['hadoop-env']['namenode_opt_newsize']
namenode_opt_maxnewsize = config['configurations']['hadoop-env']['namenode_opt_maxnewsize']
namenode_opt_permsize = format_jvm_option("/configurations/hadoop-env/namenode_opt_permsize","128m")
namenode_opt_maxpermsize = format_jvm_option("/configurations/hadoop-env/namenode_opt_maxpermsize","256m")

jtnode_opt_newsize = default("/configurations/mapred-env/jtnode_opt_newsize","200m")
jtnode_opt_maxnewsize = default("/configurations/mapred-env/jtnode_opt_maxnewsize","200m")
jtnode_heapsize =  default("/configurations/mapred-env/jtnode_heapsize","1024m")
ttnode_heapsize = default("/configurations/mapred-env/ttnode_heapsize","1024m")

dtnode_heapsize = config['configurations']['hadoop-env']['dtnode_heapsize']

mapred_pid_dir_prefix = default("/configurations/hadoop-env/mapred_pid_dir_prefix","/var/run/hadoop-mapreduce")
mapreduce_libs_path = "/usr/lib/hadoop-mapreduce/*"

rca_enabled = False
if 'mapred-env' in config['configurations']:
  rca_enabled =  config['configurations']['mapred-env']['rca_enabled']

ambari_db_rca_url = config['hostLevelParams']['ambari_db_rca_url']
ambari_db_rca_driver = config['hostLevelParams']['ambari_db_rca_driver']
ambari_db_rca_username = config['hostLevelParams']['ambari_db_rca_username']
ambari_db_rca_password = config['hostLevelParams']['ambari_db_rca_password']

rca_properties = ''
if rca_enabled and 'mapreduce-log4j' in config['configurations'] \
  and 'rca_properties' in config['configurations']['mapred-env']:
  rca_properties = format(config['configurations']['mapred-env']['rca_properties'])