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

Ambari Agent

"""

from resource_management.libraries.script.script import Script
from resource_management.libraries.resources.hdfs_resource import HdfsResource
from resource_management.libraries.functions import conf_select
from resource_management.libraries.functions import stack_select
from resource_management.libraries.functions.format import format
from resource_management.libraries.functions.version import format_stack_version
from resource_management.libraries.functions.default import default
from resource_management.libraries.functions import get_kinit_path

# server configurations
config = Script.get_config()
tmp_dir = Script.get_tmp_dir()

stack_name = default("/hostLevelParams/stack_name", None)

stack_version_unformatted = str(config['hostLevelParams']['stack_version'])
iop_stack_version = format_stack_version(stack_version_unformatted)

# New Cluster Stack Version that is defined during the RESTART of a Rolling Upgrade
version = default("/commandParams/version", None)

titan_user = config['configurations']['titan-env']['titan_user']
user_group = config['configurations']['cluster-env']['user_group']
titan_bin_dir = '/usr/iop/current/titan-client/bin'


smokeuser = config['configurations']['cluster-env']['smokeuser']
smokeuser_principal = config['configurations']['cluster-env']['smokeuser_principal_name']

security_enabled = config['configurations']['cluster-env']['security_enabled']
smoke_user_keytab = config['configurations']['cluster-env']['smokeuser_keytab']
kinit_path_local = get_kinit_path(default('/configurations/kerberos-env/executable_search_paths', None))

# titan configurations
titan_conf_dir = "/usr/iop/current/titan-client/conf"
titan_hbase_solr_props = config['configurations']['titan-hbase-solr']['content']
titan_env_props = config['configurations']['titan-env']['content']
log4j_console_props = config['configurations']['titan-log4j']['content']

# not supporting 32 bit jdk.
java64_home = config['hostLevelParams']['java_home']
hadoop_config_dir = '/etc/hadoop/conf'
hbase_config_dir = '/etc/hbase/conf'

# Titan required 'storage.hostname' which is hbase cluster in IOP 4.2.
# The host name should be zooKeeper quorum
storage_hosts = config['clusterHostInfo']['zookeeper_hosts']
storage_host_list = []
for hostname in storage_hosts:
  storage_host_list.append(hostname)
storage_host = ",".join(storage_host_list)
hbase_zookeeper_parent = config['configurations']['hbase-site']['zookeeper.znode.parent']

# Solr cloud host
solr_hosts = config['clusterHostInfo']['solr_hosts']
solr_host_list = []
for hostname in solr_hosts:
  solr_host_list.append(hostname)
solr_host = ",".join(solr_host_list)
solr_server_host = solr_hosts[0]

# Titan client, it does not work right now, there is no 'titan_host' in 'clusterHostInfo'
# It will return "Configuration parameter 'titan_host' was not found in configurations dictionary!"
# So here is a known issue as task 118900, will install titan and solr on same node right now.
# titan_host = config['clusterHostInfo']['titan_host']
titan_host = solr_server_host

# Conf directory and jar should be copy to solr site
titan_dir = format('/usr/iop/current/titan-client')
titan_ext_dir = format('/usr/iop/current/titan-client/ext')
titan_solr_conf_dir = format('/usr/iop/current/titan-client/conf/solr')
titan_solr_jar_file = format('/usr/iop/current/titan-client/lib/jts-1.13.jar')

titan_solr_hdfs_dir = "/apps/titan"
titan_solr_hdfs_conf_dir = "/apps/titan/conf"
titan_solr_hdfs_jar = "/apps/titan/jts-1.13.jar"
titan_tmp_dir = format('{tmp_dir}/titan')
titan_solr_dir = format('{titan_tmp_dir}/solr_installed')
configuration_tags = config['configurationTags']
hdfs_user = config['configurations']['hadoop-env']['hdfs_user']
titan_hdfs_mode = 0775

#for create_hdfs_directory
security_enabled = config['configurations']['cluster-env']['security_enabled']
hdfs_user_keytab = config['configurations']['hadoop-env']['hdfs_user_keytab']
kinit_path_local = get_kinit_path()
hadoop_bin_dir = stack_select.get_hadoop_dir("bin")
hadoop_conf_dir = conf_select.get_hadoop_conf_dir()
hdfs_site = config['configurations']['hdfs-site']
hdfs_principal_name = default('/configurations/hadoop-env/hdfs_principal_name', 'missing_principal').replace("_HOST", hostname)
default_fs = config['configurations']['core-site']['fs.defaultFS']

import functools
#to create hdfs directory we need to call params.HdfsResource in code
HdfsResource = functools.partial(
  HdfsResource,
  user = hdfs_user,
  security_enabled = security_enabled,
  keytab = hdfs_user_keytab,
  kinit_path_local = kinit_path_local,
  hadoop_bin_dir = hadoop_bin_dir,
  hadoop_conf_dir = hadoop_conf_dir,
  principal_name = hdfs_principal_name,
  hdfs_site = hdfs_site,
  default_fs = default_fs
)

