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
import json
import collections

config = Script.get_config()
tmp_dir = Script.get_tmp_dir()

#java params
artifact_dir = format("{tmp_dir}/AMBARI-artifacts/")
jdk_name = default("/hostLevelParams/jdk_name", None) # None when jdk is already installed by user
jce_policy_zip = default("/hostLevelParams/jce_name", None) # None when jdk is already installed by user
jce_location = config['hostLevelParams']['jdk_location']
jdk_location = config['hostLevelParams']['jdk_location']
java_home = config['hostLevelParams']['java_home']
if System.get_instance().os_family == "suse":
  jsvc_path = "/usr/lib/bigtop-utils"
else:
  jsvc_path = "/usr/libexec/bigtop-utils"
#security params
security_enabled = config['configurations']['cluster-env']['security_enabled']
#hadoop params
hadoop_conf_dir = "/etc/hadoop/conf"

#hadoop-env.sh

java_home = config['hostLevelParams']['java_home']
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

dtnode_heapsize = config['configurations']['hadoop-env']['dtnode_heapsize']
mapred_pid_dir_prefix = default("/configurations/hadoop-env/mapred_pid_dir_prefix","/var/run/hadoop-mapreduce")
mapreduce_libs_path = "/usr/lib/hadoop-mapreduce/*"
hadoop_libexec_dir = "/usr/lib/hadoop/libexec"
mapred_log_dir_prefix = "/var/log/hadoop-mapreduce"

hdfs_log_dir_prefix = config['configurations']['hadoop-env']['hdfs_log_dir_prefix']
hadoop_pid_dir_prefix = config['configurations']['hadoop-env']['hadoop_pid_dir_prefix']

#users and groups
hbase_user = config['configurations']['hbase-env']['hbase_user']
nagios_user = config['configurations']['nagios-env']['nagios_user']
smoke_user =  config['configurations']['cluster-env']['smokeuser']
gmetad_user = config['configurations']['ganglia-env']["gmetad_user"]
gmond_user = config['configurations']['ganglia-env']["gmond_user"]

user_group = config['configurations']['cluster-env']['user_group']
proxyuser_group =  default("/configurations/hadoop-env/proxyuser_group","users")
nagios_group = config['configurations']['nagios-env']['nagios_group']

#hosts
hostname = config["hostname"]
ambari_server_hostname = config['clusterHostInfo']['ambari_server_host'][0]
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

has_sqoop_client = 'sqoop-env' in config['configurations']
has_resourcemanager = not len(rm_host) == 0
has_namenode = not len(namenode_host) == 0
has_jt = not len(jtnode_host) == 0
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

hbase_tmp_dir = config['configurations']['hbase-site']['hbase.tmp.dir']
ignore_groupsusers_create = default("/configurations/cluster-env/ignore_groupsusers_create", False)

smoke_user_dirs = format("/tmp/hadoop-{smoke_user},/tmp/hsperfdata_{smoke_user},/home/{smoke_user},/tmp/{smoke_user},/tmp/sqoop-{smoke_user}")
if has_hbase_masters:
  hbase_user_dirs = format("/home/{hbase_user},/tmp/{hbase_user},/usr/bin/{hbase_user},/var/log/{hbase_user},{hbase_tmp_dir}")
#repo params
repo_info = config['hostLevelParams']['repo_info']
service_repo_info = default("/hostLevelParams/service_repo_info",None)

user_to_groups_dict = collections.defaultdict(lambda:[user_group])
user_to_groups_dict[smoke_user] = [proxyuser_group]
if has_ganglia_server:
  user_to_groups_dict[gmond_user] = [gmond_user]
  user_to_groups_dict[gmetad_user] = [gmetad_user]

user_to_gid_dict = collections.defaultdict(lambda:user_group)
if has_nagios:
  user_to_gid_dict[nagios_user] = nagios_group

user_list = json.loads(config['hostLevelParams']['user_list'])
group_list = json.loads(config['hostLevelParams']['group_list'])
