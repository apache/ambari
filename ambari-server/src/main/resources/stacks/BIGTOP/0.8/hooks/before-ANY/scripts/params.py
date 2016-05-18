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
import collections
import json

config = Script.get_config()
tmp_dir = Script.get_tmp_dir()

artifact_dir = format("{tmp_dir}/AMBARI-artifacts/")
jce_policy_zip = default("/hostLevelParams/jce_name", None) # None when jdk is already installed by user
jce_location = config['hostLevelParams']['jdk_location']
jdk_name = default("/hostLevelParams/jdk_name", None)
java_home = config['hostLevelParams']['java_home']

ambari_server_hostname = config['clusterHostInfo']['ambari_server_host'][0]

#RPM versioning support
rpm_version = default("/configurations/cluster-env/rpm_version", None)

#hadoop params
if rpm_version:
  mapreduce_libs_path = "/usr/bigtop/current/hadoop-mapreduce-client/*"
  hadoop_libexec_dir = "/usr/bigtop/current/hadoop-client/libexec"
  hadoop_home = "/usr/bigtop/current/hadoop-client"
else:
  mapreduce_libs_path = "/usr/lib/hadoop-mapreduce/*"
  hadoop_libexec_dir = "/usr/lib/hadoop/libexec"
  hadoop_home = "/usr/lib/hadoop"

hadoop_conf_dir = "/etc/hadoop/conf"
hadoop_conf_empty_dir = "/etc/hadoop/conf.empty"
versioned_stack_root = '/usr/bigtop/current'
#security params
security_enabled = config['configurations']['cluster-env']['security_enabled']

#hadoop params
hdfs_log_dir_prefix = config['configurations']['hadoop-env']['hdfs_log_dir_prefix']
hadoop_pid_dir_prefix = config['configurations']['hadoop-env']['hadoop_pid_dir_prefix']
hadoop_root_logger = config['configurations']['hadoop-env']['hadoop_root_logger']

if str(config['hostLevelParams']['stack_version']).startswith('2.0') and System.get_instance().os_family != "suse":
  # deprecated rhel jsvc_path
  jsvc_path = "/usr/libexec/bigtop-utils"
else:
  jsvc_path = "/usr/lib/bigtop-utils"

hadoop_heapsize = config['configurations']['hadoop-env']['hadoop_heapsize']
namenode_heapsize = config['configurations']['hadoop-env']['namenode_heapsize']
namenode_opt_newsize =  config['configurations']['hadoop-env']['namenode_opt_newsize']
namenode_opt_maxnewsize =  config['configurations']['hadoop-env']['namenode_opt_maxnewsize']

jtnode_opt_newsize = "200m"
jtnode_opt_maxnewsize = "200m"
jtnode_heapsize =  "1024m"
ttnode_heapsize = "1024m"

dtnode_heapsize = config['configurations']['hadoop-env']['dtnode_heapsize']
mapred_pid_dir_prefix = default("/configurations/mapred-env/mapred_pid_dir_prefix","/var/run/hadoop-mapreduce")
mapred_log_dir_prefix = default("/configurations/mapred-env/mapred_log_dir_prefix","/var/log/hadoop-mapreduce")
hadoop_env_sh_template = config['configurations']['hadoop-env']['content']

#users and groups
hdfs_user = config['configurations']['hadoop-env']['hdfs_user']
hbase_user = config['configurations']['hbase-env']['hbase_user']
nagios_user = config['configurations']['nagios-env']['nagios_user']
smoke_user =  config['configurations']['cluster-env']['smokeuser']
gmetad_user = config['configurations']['ganglia-env']["gmetad_user"]
gmond_user = config['configurations']['ganglia-env']["gmond_user"]
tez_user = config['configurations']['tez-env']["tez_user"]
oozie_user = config['configurations']['oozie-env']["oozie_user"]

user_group = config['configurations']['cluster-env']['user_group']

hagios_server_hosts = default("/clusterHostInfo/nagios_server_host", [])
ganglia_server_hosts = default("/clusterHostInfo/ganglia_server_host", [])
namenode_host = default("/clusterHostInfo/namenode_host", [])
hbase_master_hosts = default("/clusterHostInfo/hbase_master_hosts", [])
oozie_servers = default("/clusterHostInfo/oozie_server", [])

has_namenode = not len(namenode_host) == 0
has_nagios = not len(hagios_server_hosts) == 0
has_ganglia_server = not len(ganglia_server_hosts) == 0
has_tez = 'tez-site' in config['configurations']
has_hbase_masters = not len(hbase_master_hosts) == 0
has_oozie_server = not len(oozie_servers) == 0

hbase_tmp_dir = config['configurations']['hbase-site']['hbase.tmp.dir']

proxyuser_group = default("/configurations/hadoop-env/proxyuser_group","users")
nagios_group = config['configurations']['nagios-env']['nagios_group']

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
if has_tez:
  user_to_groups_dict[tez_user] = [proxyuser_group]
if has_oozie_server:
  user_to_groups_dict[oozie_user] = [proxyuser_group]

user_to_gid_dict = collections.defaultdict(lambda:user_group)
if has_nagios:
  user_to_gid_dict[nagios_user] = nagios_group

user_list = json.loads(config['hostLevelParams']['user_list'])
group_list = json.loads(config['hostLevelParams']['group_list'])
