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
from resource_management.libraries.functions import StackFeature
from resource_management.libraries.functions import conf_select
from resource_management.libraries.functions import get_kinit_path
from resource_management.libraries.functions import stack_select
from resource_management.libraries.functions.default import default
from resource_management.libraries.functions.format import format
from resource_management.libraries.functions.get_stack_version import get_stack_version
from resource_management.libraries.functions.stack_features import check_stack_feature
from resource_management.libraries.functions.version import format_stack_version, get_major_version
from resource_management.libraries.resources.hdfs_resource import HdfsResource
from resource_management.libraries.script.script import Script

def get_port_from_url(address):
  if not (address is None):
    return address.split(':')[-1]
  else:
    return address

def extract_spark_version(spark_home):
  try:
    with open(spark_home + "/RELEASE") as fline:
      return re.search('Spark (\d\.\d).+', fline.readline().rstrip()).group(1)
  except:
    pass
  return None


# server configurations
config = Script.get_config()
# stack_root = Script.get_stack_root()
stack_root = "/usr/lib"

# e.g. /var/lib/ambari-agent/cache/stacks/HDP/2.2/services/zeppelin-stack/package
service_packagedir = os.path.realpath(__file__).split('/scripts')[0]

zeppelin_dirname = 'zeppelin-server'

install_dir = os.path.join(stack_root, "zeppelin")

security_enabled = config['configurations']['cluster-env']['security_enabled']

ui_ssl_enabled = config['configurations']['zeppelin-site']['zeppelin.ssl']
is_ui_ssl_enabled = str(ui_ssl_enabled).upper() == 'TRUE'

setup_view = True
temp_file = config['configurations']['zeppelin-env']['zeppelin.temp.file']

spark_home = config['configurations']['zeppelin-env']['spark_home']
spark_version = None
spark2_home = ""
spark2_version = None
if 'spark-defaults' in config['configurations']:
  spark_home = os.path.join(stack_root, "current", 'spark-client')
  spark_version = extract_spark_version(spark_home)
if 'spark2-defaults' in config['configurations']:
  spark2_home = os.path.join(stack_root, "current", 'spark2-client')
  spark2_version = extract_spark_version(spark2_home)

# New Cluster Stack Version that is defined during the RESTART of a Rolling Upgrade
version = default("/commandParams/version", None)
stack_name = default("/clusterLevelParams/stack_name", None)

# params from zeppelin-site
zeppelin_port = str(config['configurations']['zeppelin-site']['zeppelin.server.port'])
if is_ui_ssl_enabled:
  zeppelin_port = str(config['configurations']['zeppelin-site']['zeppelin.server.ssl.port'])
zeppelin_interpreter = None
if 'zeppelin.interpreter.group.order' in config['configurations']['zeppelin-site']:
  zeppelin_interpreter = str(config['configurations']['zeppelin-site']
                             ['zeppelin.interpreter.group.order']).split(",")

# params from zeppelin-env
zeppelin_user = config['configurations']['zeppelin-env']['zeppelin_user']
zeppelin_group = config['configurations']['zeppelin-env']['zeppelin_group']
zeppelin_log_dir = config['configurations']['zeppelin-env']['zeppelin_log_dir']
zeppelin_pid_dir = config['configurations']['zeppelin-env']['zeppelin_pid_dir']
zeppelin_war_tempdir = config['configurations']['zeppelin-env']['zeppelin_war_tempdir']
zeppelin_notebook_dir = config['configurations']['zeppelin-env']['zeppelin_notebook_dir']
local_notebook_dir = "/var/lib/zeppelin/notebook"

hbase_home = config['configurations']['zeppelin-env']['hbase_home']
hbase_conf_dir = config['configurations']['zeppelin-env']['hbase_conf_dir']

zeppelin_log_file = os.path.join(zeppelin_log_dir, 'zeppelin-setup.log')
zeppelin_hdfs_user_dir = format("/user/{zeppelin_user}")

zeppelin_dir = install_dir
conf_dir = "/etc/zeppelin/conf"
external_dependency_conf = "/etc/zeppelin/conf/external-dependency-conf"

conf_stored_in_hdfs = False
if 'zeppelin.config.fs.dir' in config['configurations']['zeppelin-site'] and \
  not config['configurations']['zeppelin-site']['zeppelin.config.fs.dir'].startswith('file://'):
  conf_stored_in_hdfs = True

# zeppelin-env.sh
zeppelin_env_content = config['configurations']['zeppelin-env']['zeppelin_env_content']

# shiro.ini
shiro_ini_content = config['configurations']['zeppelin-shiro-ini']['shiro_ini_content']

# log4j.properties
log4j_properties_content = config['configurations']['zeppelin-log4j-properties']['log4j_properties_content']

# detect configs
master_configs = config['clusterHostInfo']
java64_home = config['ambariLevelParams']['java_home']
ambari_host = str(config['ambariLevelParams']['ambari_server_host'])
zeppelin_host = str(master_configs['zeppelin_master_hosts'][0])

# detect HS2 details, if installed

hive_server_host = None
hive_metastore_host = '0.0.0.0'
hive_metastore_port = None
hive_server_port = None
hive_zookeeper_quorum = None
hive_server2_support_dynamic_service_discovery = None
is_hive_installed = False
hive_zookeeper_namespace = None
hive_interactive_zookeeper_namespace = None

if 'hive_server_hosts' in master_configs and len(master_configs['hive_server_hosts']) != 0:
  is_hive_installed = True
  spark_hive_properties = {
    'hive.metastore.uris': default('/configurations/hive-site/hive.metastore.uris', '')
  }
  hive_server_host = str(master_configs['hive_server_hosts'][0])
  hive_metastore_host = str(master_configs['hive_metastore_hosts'][0])
  hive_metastore_port = str(
    get_port_from_url(default('/configurations/hive-site/hive.metastore.uris', '')))
  hive_server_port = str(config['configurations']['hive-site']['hive.server2.thrift.http.port'])
  hive_zookeeper_quorum = config['configurations']['hive-site']['hive.zookeeper.quorum']
  hive_zookeeper_namespace = config['configurations']['hive-site']['hive.server2.zookeeper.namespace']
  hive_zookeeper_namespace = default('/configurations/hive-interactive-site/hive.server2.zookeeper.namespace', hive_zookeeper_namespace)
  hive_server2_support_dynamic_service_discovery = config['configurations']['hive-site']['hive.server2.support.dynamic.service.discovery']

discovery_mode = "zooKeeper"
hive_server_interactive_hosts = None
if 'hive_server_interactive_hosts' in master_configs and len(master_configs['hive_server_interactive_hosts']) != 0:
    if len(master_configs['hive_server_interactive_hosts']) > 1:
      discovery_mode = "zooKeeperHA"

    hive_server_interactive_hosts = str(master_configs['hive_server_interactive_hosts'][0])
    hive_interactive_zookeeper_namespace = config['configurations']['hive-interactive-site']['hive.server2.zookeeper.namespace']
    hive_server_port = str(config['configurations']['hive-site']['hive.server2.thrift.http.port'])
    hive_zookeeper_quorum = config['configurations']['hive-site']['hive.zookeeper.quorum']
    hive_server2_support_dynamic_service_discovery = config['configurations']['hive-site']['hive.server2.support.dynamic.service.discovery']

spark_thrift_server_hosts = None
spark_hive_thrift_port = None
spark_hive_principal = None
hive_principal = None
hive_transport_mode = None

if 'hive-site' in config['configurations']:
  if 'hive.server2.authentication.kerberos.principal' in config['configurations']['hive-site']:
    hive_principal = config['configurations']['hive-site']['hive.server2.authentication.kerberos.principal']
  if 'hive.server2.transport.mode' in config['configurations']['hive-site']:
    hive_transport_mode = config['configurations']['hive-site']['hive.server2.transport.mode']

spark2_transport_mode = hive_transport_mode
spark2_http_path = None
spark2_ssl = False
if 'spark2-hive-site-override' in config['configurations']:
  if 'hive.server2.transport.mode' in config['configurations']['spark2-hive-site-override']:
    spark2_transport_mode = config['configurations']['spark2-hive-site-override']['hive.server2.transport.mode']

  if 'hive.server2.http.endpoint' in config['configurations']['spark2-hive-site-override']:
    spark2_http_path = config['configurations']['spark2-hive-site-override']['hive.server2.http.endpoint']

  if 'hive.server2.use.SSL' in config['configurations']['spark2-hive-site-override']:
    spark2_ssl = default("configurations/spark2-hive-site-override/hive.server2.use.SSL", False)

if 'spark_thriftserver_hosts' in master_configs and len(master_configs['spark_thriftserver_hosts']) != 0:
  spark_thrift_server_hosts = str(master_configs['spark_thriftserver_hosts'][0])
  if config['configurations']['spark-hive-site-override']:
    spark_hive_thrift_port = config['configurations']['spark-hive-site-override']['hive.server2.thrift.port']

spark2_thrift_server_hosts = None
spark2_hive_thrift_port = None
spark2_hive_principal = None
if 'spark2_thriftserver_hosts' in master_configs and len(master_configs['spark2_thriftserver_hosts']) != 0:
  spark2_thrift_server_hosts = str(master_configs['spark2_thriftserver_hosts'][0])
  if config['configurations']['spark2-hive-site-override']:
    spark2_hive_thrift_port = config['configurations']['spark2-hive-site-override']['hive.server2.thrift.port']
    if 'hive.server2.authentication.kerberos.principal' in config['configurations']['spark2-hive-site-override']:
      spark2_hive_principal = config['configurations']['spark2-hive-site-override']['hive.server2.authentication.kerberos.principal']


# detect hbase details if installed
zookeeper_znode_parent = None
hbase_zookeeper_quorum = None
is_hbase_installed = False
if 'hbase_master_hosts' in master_configs and 'hbase-site' in config['configurations']:
  is_hbase_installed = True
  zookeeper_znode_parent = config['configurations']['hbase-site']['zookeeper.znode.parent']
  hbase_zookeeper_quorum = config['configurations']['hbase-site']['hbase.zookeeper.quorum']

# detect spark queue
if 'spark-defaults' in config['configurations'] and 'spark.yarn.queue' in config['configurations']['spark-defaults']:
  spark_queue = config['configurations']['spark-defaults']['spark.yarn.queue']
elif 'spark2-defaults' in config['configurations'] and 'spark.yarn.queue' in config['configurations']['spark2-defaults']:
  spark_queue = config['configurations']['spark2-defaults']['spark.yarn.queue']
else:
  spark_queue = 'default'

smoke_user = config['configurations']['cluster-env']['smokeuser']

if security_enabled:
  zeppelin_kerberos_keytab = config['configurations']['zeppelin-site']['zeppelin.server.kerberos.keytab']
  zeppelin_kerberos_principal = config['configurations']['zeppelin-site']['zeppelin.server.kerberos.principal']

  smoke_user_keytab = config['configurations']['cluster-env']['smokeuser_keytab']
  smokeuser_principal =  config['configurations']['cluster-env']['smokeuser_principal_name']

if 'zeppelin.interpreter.config.upgrade' in config['configurations']['zeppelin-site']:
  zeppelin_interpreter_config_upgrade = config['configurations']['zeppelin-site']['zeppelin.interpreter.config.upgrade']
else:
  zeppelin_interpreter_config_upgrade = False

exclude_interpreter_autoconfig = default("/configurations/zeppelin-site/exclude.interpreter.autoconfig", None)

# e.g. 2.3
stack_version_unformatted = config['clusterLevelParams']['stack_version']

# e.g. 2.3.0.0
stack_version_formatted = format_stack_version(stack_version_unformatted)
major_stack_version = get_major_version(stack_version_formatted)

# e.g. 2.3.0.0-2130
full_stack_version = default("/commandParams/version", None)

spark_client_version = get_stack_version('spark-client')

hbase_master_hosts = default("/clusterHostInfo/hbase_master_hosts", [])
livy_hosts = default("/clusterHostInfo/livy_server_hosts", [])
livy2_hosts = default("/clusterHostInfo/livy2_server_hosts", [])

livy_livyserver_host = None
livy_livyserver_port = None
livy_livyserver_protocol = 'http'
livy2_livyserver_host = None
livy2_livyserver_port = None
livy2_livyserver_protocol = 'http'
if stack_version_formatted and check_stack_feature(StackFeature.SPARK_LIVY, stack_version_formatted) and \
    len(livy_hosts) > 0:
  livy_livyserver_host = str(livy_hosts[0])
  livy_livyserver_port = config['configurations']['livy-conf']['livy.server.port']
  if 'livy.keystore' in config['configurations']['livy-conf']:
    livy_livyserver_protocol = 'https'

if stack_version_formatted and check_stack_feature(StackFeature.SPARK_LIVY2, stack_version_formatted) and \
    len(livy2_hosts) > 0:
  livy2_livyserver_host = str(livy2_hosts[0])
  livy2_livyserver_port = config['configurations']['livy2-conf']['livy.server.port']
  if 'livy.keystore' in config['configurations']['livy2-conf']:
    livy2_livyserver_protocol = 'https'

hdfs_user = config['configurations']['hadoop-env']['hdfs_user']
hdfs_user_keytab = config['configurations']['hadoop-env']['hdfs_user_keytab']
kinit_path_local = get_kinit_path(default('/configurations/kerberos-env/executable_search_paths', None))
hadoop_bin_dir = stack_select.get_hadoop_dir("bin")
hadoop_conf_dir = conf_select.get_hadoop_conf_dir()
hdfs_principal_name = config['configurations']['hadoop-env']['hdfs_principal_name']
hdfs_site = config['configurations']['hdfs-site']
default_fs = config['configurations']['core-site']['fs.defaultFS']
dfs_type = default("/clusterLevelParams/dfs_type", "")

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
  default_fs=default_fs,
  dfs_type = dfs_type
)

mount_table_xml_inclusion_file_full_path = None
mount_table_content = None
if 'viewfs-mount-table' in config['configurations']:
  xml_inclusion_file_name = 'viewfs-mount-table.xml'
  mount_table = config['configurations']['viewfs-mount-table']

  if 'content' in mount_table and mount_table['content'].strip():
    mount_table_xml_inclusion_file_full_path = os.path.join(external_dependency_conf, xml_inclusion_file_name)
    mount_table_content = mount_table['content']
