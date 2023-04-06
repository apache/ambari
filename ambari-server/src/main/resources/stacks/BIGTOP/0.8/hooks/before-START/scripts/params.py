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

#RPM versioning support
rpm_version = default("/configurations/cluster-env/rpm_version", None)

#hadoop params
if rpm_version:
  mapreduce_libs_path = "/usr/bigtop/current/hadoop-mapreduce-client/*"
  hadoop_libexec_dir = "/usr/bigtop/current/hadoop-client/libexec"
  hadoop_lib_home = "/usr/bigtop/current/hadoop-client/lib"
  hadoop_bin = "/usr/bigtop/current/hadoop-client/sbin"
  hadoop_home = '/usr/bigtop/current/hadoop-client'
else:
  mapreduce_libs_path = "/usr/lib/hadoop-mapreduce/*"
  hadoop_libexec_dir = "/usr/lib/hadoop/libexec"
  hadoop_lib_home = "/usr/lib/hadoop/lib"
  hadoop_bin = "/usr/lib/hadoop/sbin"
  hadoop_home = '/usr'

hadoop_conf_dir = "/etc/hadoop/conf"
#security params
security_enabled = config['configurations']['cluster-env']['security_enabled']

#users and groups
mapred_user = config['configurations']['mapred-env']['mapred_user']
hdfs_user = config['configurations']['hadoop-env']['hdfs_user']
yarn_user = config['configurations']['yarn-env']['yarn_user']

user_group = config['configurations']['cluster-env']['user_group']

#hosts
hostname = config["hostname"]
ambari_server_hostname = config['clusterHostInfo']['ambari_server_host'][0]
rm_host = default("/clusterHostInfo/rm_host", [])
slave_hosts = default("/clusterHostInfo/slave_hosts", [])
oozie_servers = default("/clusterHostInfo/oozie_server", [])
hcat_server_hosts = default("/clusterHostInfo/webhcat_server_host", [])
hive_server_host =  default("/clusterHostInfo/hive_server_host", [])
hbase_master_hosts = default("/clusterHostInfo/hbase_master_hosts", [])
hs_host = default("/clusterHostInfo/hs_host", [])
jtnode_host = default("/clusterHostInfo/jtnode_host", [])
namenode_host = default("/clusterHostInfo/namenode_host", [])
zk_hosts = default("/clusterHostInfo/zookeeper_server_hosts", [])
ganglia_server_hosts = default("/clusterHostInfo/ganglia_server_host", [])

has_namenode = not len(namenode_host) == 0
has_resourcemanager = not len(rm_host) == 0
has_slaves = not len(slave_hosts) == 0
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

if has_namenode:
  hadoop_tmp_dir = format("/tmp/hadoop-{hdfs_user}")
hadoop_pid_dir_prefix = config['configurations']['hadoop-env']['hadoop_pid_dir_prefix']

task_log4j_properties_location = os.path.join(hadoop_conf_dir, "task-log4j.properties")

hdfs_log_dir_prefix = config['configurations']['hadoop-env']['hdfs_log_dir_prefix']
hbase_tmp_dir = config['configurations']['hbase-site']['hbase.tmp.dir']
#db params
server_db_name = config['hostLevelParams']['db_name']
db_driver_filename = config['hostLevelParams']['db_driver_filename']
oracle_driver_url = config['hostLevelParams']['oracle_jdbc_url']
mysql_driver_url = config['hostLevelParams']['mysql_jdbc_url']
ambari_server_resources = config['ambariLevelParams']['jdk_location']
oracle_driver_symlink_url = format("{ambari_server_resources}oracle-jdbc-driver.jar")
mysql_driver_symlink_url = format("{ambari_server_resources}mysql-jdbc-driver.jar")

ambari_db_rca_url = config['hostLevelParams']['ambari_db_rca_url'][0]
ambari_db_rca_driver = config['hostLevelParams']['ambari_db_rca_driver'][0]
ambari_db_rca_username = config['hostLevelParams']['ambari_db_rca_username'][0]
ambari_db_rca_password = config['hostLevelParams']['ambari_db_rca_password'][0]

if has_namenode and 'rca_enabled' in config['configurations']['hadoop-env']:
  rca_enabled =  config['configurations']['hadoop-env']['rca_enabled']
else:
  rca_enabled = False
rca_disabled_prefix = "###"
if rca_enabled == True:
  rca_prefix = ""
else:
  rca_prefix = rca_disabled_prefix

#hadoop-env.sh
java_home = config['ambariLevelParams']['java_home']

if str(config['clusterLevelParams']['stack_version']).startswith('2.0') and System.get_instance().os_family != "suse":
  # deprecated rhel jsvc_path
  jsvc_path = "/usr/libexec/bigtop-utils"
else:
  jsvc_path = "/usr/lib/bigtop-utils"

hadoop_heapsize = config['configurations']['hadoop-env']['hadoop_heapsize']
namenode_heapsize = config['configurations']['hadoop-env']['namenode_heapsize']
namenode_opt_newsize = config['configurations']['hadoop-env']['namenode_opt_newsize']
namenode_opt_maxnewsize = config['configurations']['hadoop-env']['namenode_opt_maxnewsize']
namenode_opt_permsize = format_jvm_option("/configurations/hadoop-env/namenode_opt_permsize","128m")
namenode_opt_maxpermsize = format_jvm_option("/configurations/hadoop-env/namenode_opt_maxpermsize","256m")

jtnode_opt_newsize = "200m"
jtnode_opt_maxnewsize = "200m"
jtnode_heapsize =  "1024m"
ttnode_heapsize = "1024m"

dtnode_heapsize = config['configurations']['hadoop-env']['dtnode_heapsize']
mapred_pid_dir_prefix = default("/configurations/mapred-env/mapred_pid_dir_prefix","/var/run/hadoop-mapreduce")
mapred_log_dir_prefix = default("/configurations/mapred-env/mapred_log_dir_prefix","/var/log/hadoop-mapreduce")

#log4j.properties

yarn_log_dir_prefix = default("/configurations/yarn-env/yarn_log_dir_prefix","/var/log/hadoop-yarn")

dfs_hosts = default('/configurations/hdfs-site/dfs.hosts', None)

#log4j.properties
if (('hdfs-log4j' in config['configurations']) and ('content' in config['configurations']['hdfs-log4j'])):
  log4j_props = config['configurations']['hdfs-log4j']['content']
  if (('yarn-log4j' in config['configurations']) and ('content' in config['configurations']['yarn-log4j'])):
    log4j_props += config['configurations']['yarn-log4j']['content']
else:
  log4j_props = None
