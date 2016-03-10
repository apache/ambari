#!/usr/bin/env python
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

import functools
import os
import re

from resource_management.libraries.functions import conf_select
from resource_management.libraries.functions import get_kinit_path
from resource_management.libraries.functions import stack_select
from resource_management.libraries.functions.default import default
from resource_management.libraries.functions.get_stack_version import get_stack_version
from resource_management.libraries.functions.version import format_stack_version
from resource_management.libraries.resources.hdfs_resource import HdfsResource
from resource_management.libraries.script.script import Script


def get_port_from_url(address):
  if not (address is None):
    return address.split(':')[-1]
  else:
    return address


# server configurations
config = Script.get_config()

# e.g. /var/lib/ambari-agent/cache/stacks/HDP/2.2/services/zeppelin-stack/package
service_packagedir = os.path.realpath(__file__).split('/scripts')[0]

zeppelin_dirname = 'zeppelin-server/lib'

install_dir = '/usr/hdp/current'
executor_mem = config['configurations']['zeppelin-env']['zeppelin.executor.mem']
executor_instances = config['configurations']['zeppelin-env'][
  'zeppelin.executor.instances']

spark_jar_dir = config['configurations']['zeppelin-env']['zeppelin.spark.jar.dir']
spark_jar = format("{spark_jar_dir}/zeppelin-spark-0.5.5-SNAPSHOT.jar")
setup_view = True
temp_file = config['configurations']['zeppelin-env']['zeppelin.temp.file']
spark_home = "/usr/hdp/current/spark-client/"

try:
  fline = open(spark_home + "/RELEASE").readline().rstrip()
  spark_version = re.search('Spark (\d\.\d).+', fline).group(1)
except:
  pass

# params from zeppelin-config
zeppelin_port = str(config['configurations']['zeppelin-config']['zeppelin.server.port'])

# params from zeppelin-env
zeppelin_user = config['configurations']['zeppelin-env']['zeppelin_user']
zeppelin_group = config['configurations']['zeppelin-env']['zeppelin_group']
zeppelin_log_dir = config['configurations']['zeppelin-env']['zeppelin_log_dir']
zeppelin_pid_dir = config['configurations']['zeppelin-env']['zeppelin_pid_dir']
zeppelin_log_file = os.path.join(zeppelin_log_dir, 'zeppelin-setup.log')
zeppelin_hdfs_user_dir = format("/user/{zeppelin_user}")

zeppelin_dir = os.path.join(*[install_dir, zeppelin_dirname])
conf_dir = os.path.join(*[install_dir, zeppelin_dirname, 'conf'])
notebook_dir = os.path.join(*[install_dir, zeppelin_dirname, 'notebook'])

# zeppelin-env.sh
zeppelin_env_content = config['configurations']['zeppelin-env']['content']

# detect configs
master_configs = config['clusterHostInfo']
java64_home = config['hostLevelParams']['java_home']
ambari_host = str(master_configs['ambari_server_host'][0])
zeppelin_host = str(master_configs['zeppelin_master_hosts'][0])

# detect HS2 details, if installed

if 'hive_server_host' in master_configs and len(master_configs['hive_server_host']) != 0:
  hive_server_host = str(master_configs['hive_server_host'][0])
  hive_metastore_host = str(master_configs['hive_metastore_host'][0])
  hive_metastore_port = str(
    get_port_from_url(config['configurations']['hive-site']['hive.metastore.uris']))
  hive_server_port = str(config['configurations']['hive-site']['hive.server2.thrift.http.port'])
else:
  hive_server_host = None
  hive_metastore_host = '0.0.0.0'
  hive_metastore_port = None
  hive_server_port = None

# detect hbase details if installed
if 'hbase_master_hosts' in master_configs and 'hbase-site' in config['configurations']:
  zookeeper_znode_parent = config['configurations']['hbase-site']['zookeeper.znode.parent']
  hbase_zookeeper_quorum = config['configurations']['hbase-site']['hbase.zookeeper.quorum']
else:
  zookeeper_znode_parent = None
  hbase_zookeeper_quorum = None

# detect spark queue
if 'spark.yarn.queue' in config['configurations']['spark-defaults']:
  spark_queue = config['configurations']['spark-defaults']['spark.yarn.queue']
else:
  spark_queue = 'default'

# e.g. 2.3
stack_version_unformatted = str(config['hostLevelParams']['stack_version'])

# e.g. 2.3.0.0
hdp_stack_version = format_stack_version(stack_version_unformatted)

# e.g. 2.3.0.0-2130
full_version = default("/commandParams/version", None)
hdp_version = full_version

spark_client_version = get_stack_version('spark-client')

hdfs_user = config['configurations']['hadoop-env']['hdfs_user']
security_enabled = config['configurations']['cluster-env']['security_enabled']
hdfs_user_keytab = config['configurations']['hadoop-env']['hdfs_user_keytab']
kinit_path_local = get_kinit_path(default('/configurations/kerberos-env/executable_search_paths', None))
hadoop_bin_dir = stack_select.get_hadoop_dir("bin")
hadoop_conf_dir = conf_select.get_hadoop_conf_dir()
hdfs_principal_name = config['configurations']['hadoop-env']['hdfs_principal_name']
hdfs_site = config['configurations']['hdfs-site']
default_fs = config['configurations']['core-site']['fs.defaultFS']

# create partial functions with common arguments for every HdfsResource call
# to create hdfs directory we need to call params.HdfsResource in code
HdfsResource = functools.partial(
  HdfsResource,
  user=hdfs_user,
  hdfs_resource_ignore_file="/var/lib/ambari-agent/data/.hdfs_resource_ignore",
  security_enabled=security_enabled,
  keytab=hdfs_user_keytab,
  kinit_path_local=kinit_path_local,
  hadoop_bin_dir=hadoop_bin_dir,
  hadoop_conf_dir=hadoop_conf_dir,
  principal_name=hdfs_principal_name,
  hdfs_site=hdfs_site,
  default_fs=default_fs
)
