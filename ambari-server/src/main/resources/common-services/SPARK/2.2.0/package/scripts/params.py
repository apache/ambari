#!/usr/bin/python
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

import socket

import status_params
from ambari_commons.constants import AMBARI_SUDO_BINARY
from resource_management.libraries.functions.stack_features import check_stack_feature
from resource_management.libraries.functions.constants import StackFeature
from resource_management.libraries.functions import conf_select, stack_select
from resource_management.libraries.functions.version import format_stack_version
from resource_management.libraries.functions.copy_tarball import get_sysprep_skip_copy_tarballs_hdfs
from resource_management.libraries.functions.format import format
from resource_management.libraries.functions.default import default
from resource_management.libraries.functions import get_kinit_path
from resource_management.libraries.functions.get_not_managed_resources import get_not_managed_resources
from resource_management.libraries.resources.hdfs_resource import HdfsResource
from resource_management.libraries.script.script import Script

# a map of the Ambari role to the component name
# for use with <stack-root>/current/<component>
# TODO, change to "spark" and "livy" after RPM switches the name
SERVER_ROLE_DIRECTORY_MAP = {
  'SPARK_JOBHISTORYSERVER' : 'spark2-historyserver',
  'SPARK_CLIENT' : 'spark2-client',
  'SPARK_THRIFTSERVER' : 'spark2-thriftserver',
  'LIVY_SERVER' : 'livy2-server',
  'LIVY_CLIENT' : 'livy2-client'

}

sudo = AMBARI_SUDO_BINARY
component_directory = Script.get_component_from_role(SERVER_ROLE_DIRECTORY_MAP, "SPARK_CLIENT")

config = Script.get_config()
tmp_dir = Script.get_tmp_dir()

stack_name = status_params.stack_name
stack_root = Script.get_stack_root()
stack_version_unformatted = config['hostLevelParams']['stack_version']
stack_version_formatted = format_stack_version(stack_version_unformatted)

sysprep_skip_copy_tarballs_hdfs = get_sysprep_skip_copy_tarballs_hdfs()

# New Cluster Stack Version that is defined during the RESTART of a Stack Upgrade
version = default("/commandParams/version", None)

# TODO, change to "spark" after RPM switches the name
spark_conf = '/etc/spark2/conf'
hadoop_conf_dir = conf_select.get_hadoop_conf_dir()
hadoop_bin_dir = stack_select.get_hadoop_dir("bin")

if stack_version_formatted and check_stack_feature(StackFeature.ROLLING_UPGRADE, stack_version_formatted):
  hadoop_home = stack_select.get_hadoop_dir("home")
  spark_conf = format("{stack_root}/current/{component_directory}/conf")
  spark_log_dir = config['configurations']['spark-env']['spark_log_dir']
  spark_pid_dir = status_params.spark_pid_dir
  spark_home = format("{stack_root}/current/{component_directory}")

spark_daemon_memory = config['configurations']['spark-env']['spark_daemon_memory']
spark_thrift_server_conf_file = spark_conf + "/spark-thrift-sparkconf.conf"
java_home = config['hostLevelParams']['java_home']

hdfs_user = config['configurations']['hadoop-env']['hdfs_user']
hdfs_principal_name = config['configurations']['hadoop-env']['hdfs_principal_name']
hdfs_user_keytab = config['configurations']['hadoop-env']['hdfs_user_keytab']
user_group = config['configurations']['cluster-env']['user_group']

spark_user = status_params.spark_user
hive_user = status_params.hive_user
spark_group = status_params.spark_group
user_group = status_params.user_group
spark_hdfs_user_dir = format("/user/{spark_user}")
spark_history_dir = default('/configurations/spark-defaults/spark.history.fs.logDirectory', "hdfs:///spark-history")

spark_history_server_pid_file = status_params.spark_history_server_pid_file
spark_thrift_server_pid_file = status_params.spark_thrift_server_pid_file

spark_history_server_start = format("{spark_home}/sbin/start-history-server.sh")
spark_history_server_stop = format("{spark_home}/sbin/stop-history-server.sh")

spark_thrift_server_start = format("{spark_home}/sbin/start-thriftserver.sh")
spark_thrift_server_stop = format("{spark_home}/sbin/stop-thriftserver.sh")
spark_hadoop_lib_native = format("{stack_root}/current/hadoop-client/lib/native:{stack_root}/current/hadoop-client/lib/native/Linux-amd64-64")

run_example_cmd = format("{spark_home}/bin/run-example")
spark_smoke_example = "SparkPi"
spark_service_check_cmd = format(
  "{run_example_cmd} --master yarn --deploy-mode cluster --num-executors 1 --driver-memory 256m --executor-memory 256m --executor-cores 1 {spark_smoke_example} 1")

spark_jobhistoryserver_hosts = default("/clusterHostInfo/spark_jobhistoryserver_hosts", [])

if len(spark_jobhistoryserver_hosts) > 0:
  spark_history_server_host = spark_jobhistoryserver_hosts[0]
else:
  spark_history_server_host = "localhost"

# spark-defaults params
ui_ssl_enabled = default("configurations/spark-defaults/spark.ssl.enabled", False)

spark_yarn_historyServer_address = default(spark_history_server_host, "localhost")
spark_history_scheme = "http"
spark_history_ui_port = config['configurations']['spark-defaults']['spark.history.ui.port']

if ui_ssl_enabled:
  spark_history_ui_port = str(int(spark_history_ui_port) + 400)
  spark_history_scheme = "https"


spark_env_sh = config['configurations']['spark-env']['content']
spark_log4j_properties = config['configurations']['spark-log4j-properties']['content']
spark_metrics_properties = config['configurations']['spark-metrics-properties']['content']

hive_server_host = default("/clusterHostInfo/hive_server_host", [])
is_hive_installed = not len(hive_server_host) == 0

security_enabled = config['configurations']['cluster-env']['security_enabled']
kinit_path_local = get_kinit_path(default('/configurations/kerberos-env/executable_search_paths', None))
spark_kerberos_keytab =  config['configurations']['spark-defaults']['spark.history.kerberos.keytab']
spark_kerberos_principal =  config['configurations']['spark-defaults']['spark.history.kerberos.principal']
smoke_user_keytab = config['configurations']['cluster-env']['smokeuser_keytab']
smokeuser_principal =  config['configurations']['cluster-env']['smokeuser_principal_name']

spark_thriftserver_hosts = default("/clusterHostInfo/spark_thriftserver_hosts", [])
has_spark_thriftserver = not len(spark_thriftserver_hosts) == 0

# hive-site params
spark_hive_properties = {
  'hive.metastore.uris': default('/configurations/hive-site/hive.metastore.uris', '')
}

# security settings
if security_enabled:
  spark_principal = spark_kerberos_principal.replace('_HOST',spark_history_server_host.lower())

  if is_hive_installed:
    spark_hive_properties.update({
      'hive.metastore.sasl.enabled': str(config['configurations']['hive-site']['hive.metastore.sasl.enabled']).lower(),
      'hive.metastore.kerberos.keytab.file': config['configurations']['hive-site']['hive.metastore.kerberos.keytab.file'],
      'hive.server2.authentication.spnego.principal': config['configurations']['hive-site']['hive.server2.authentication.spnego.principal'],
      'hive.server2.authentication.spnego.keytab': config['configurations']['hive-site']['hive.server2.authentication.spnego.keytab'],
      'hive.metastore.kerberos.principal': config['configurations']['hive-site']['hive.metastore.kerberos.principal'],
      'hive.server2.authentication.kerberos.principal': config['configurations']['hive-site']['hive.server2.authentication.kerberos.principal'],
      'hive.server2.authentication.kerberos.keytab': config['configurations']['hive-site']['hive.server2.authentication.kerberos.keytab'],
      'hive.server2.authentication': config['configurations']['hive-site']['hive.server2.authentication'],
    })

    hive_kerberos_keytab = config['configurations']['hive-site']['hive.server2.authentication.kerberos.keytab']
    hive_kerberos_principal = config['configurations']['hive-site']['hive.server2.authentication.kerberos.principal'].replace('_HOST', socket.getfqdn().lower())

# thrift server support - available on HDP 2.3 or higher
spark_thrift_sparkconf = None
spark_thrift_cmd_opts_properties = ''
spark_thrift_fairscheduler_content = None
spark_thrift_master = "yarn-client"
if 'nm_hosts' in config['clusterHostInfo'] and len(config['clusterHostInfo']['nm_hosts']) == 1:
  # use local mode when there's only one nodemanager
  spark_thrift_master = "local[4]"

if has_spark_thriftserver and 'spark-thrift-sparkconf' in config['configurations']:
  spark_thrift_sparkconf = config['configurations']['spark-thrift-sparkconf']
  spark_thrift_cmd_opts_properties = config['configurations']['spark-env']['spark_thrift_cmd_opts']
  if is_hive_installed:
    # update default metastore client properties (async wait for metastore component) it is useful in case of
    # blueprint provisioning when hive-metastore and spark-thriftserver is not on the same host.
    spark_hive_properties.update({
      'hive.metastore.client.socket.timeout' : config['configurations']['hive-site']['hive.metastore.client.socket.timeout']
    })
    spark_hive_properties.update(config['configurations']['spark-hive-site-override'])

  if 'spark-thrift-fairscheduler' in config['configurations'] and 'fairscheduler_content' in config['configurations']['spark-thrift-fairscheduler']:
    spark_thrift_fairscheduler_content = config['configurations']['spark-thrift-fairscheduler']['fairscheduler_content']

default_fs = config['configurations']['core-site']['fs.defaultFS']
hdfs_site = config['configurations']['hdfs-site']
hdfs_resource_ignore_file = "/var/lib/ambari-agent/data/.hdfs_resource_ignore"

ats_host = set(default("/clusterHostInfo/app_timeline_server_hosts", []))
has_ats = len(ats_host) > 0

dfs_type = default("/commandParams/dfs_type", "")

# livy related config

# livy for spark2 is only supported from HDP 2.6
has_livyserver = False

if stack_version_formatted and check_stack_feature(StackFeature.SPARK_LIVY, stack_version_formatted) and "livy-env" in config['configurations']:
  livy_component_directory = Script.get_component_from_role(SERVER_ROLE_DIRECTORY_MAP, "LIVY_SERVER")
  livy_conf = format("{stack_root}/current/{livy_component_directory}/conf")
  livy_log_dir = config['configurations']['livy-env']['livy_log_dir']
  livy_pid_dir = status_params.livy_pid_dir
  livy_home = format("{stack_root}/current/{livy_component_directory}")
  livy_user = status_params.livy_user
  livy_group = status_params.livy_group
  user_group = status_params.user_group
  livy_hdfs_user_dir = format("/user/{livy_user}")
  livy_server_pid_file = status_params.livy_server_pid_file
  livy_recovery_dir = default("/configurations/livy-conf/livy.server.recovery.state-store.url", "/livy-recovery")

  livy_server_start = format("{livy_home}/bin/livy-server start")
  livy_server_stop = format("{livy_home}/bin/livy-server stop")
  livy_logs_dir = format("{livy_home}/logs")

  livy_env_sh = config['configurations']['livy-env']['content']
  livy_log4j_properties = config['configurations']['livy-log4j-properties']['content']
  livy_spark_blacklist_properties = config['configurations']['livy-spark-blacklist']['content']

  if 'livy.server.kerberos.keytab' in config['configurations']['livy-conf']:
    livy_kerberos_keytab =  config['configurations']['livy-conf']['livy.server.kerberos.keytab']
  else:
    livy_kerberos_keytab =  config['configurations']['livy-conf']['livy.server.launch.kerberos.keytab']
  if 'livy.server.kerberos.principal' in config['configurations']['livy-conf']:
    livy_kerberos_principal = config['configurations']['livy-conf']['livy.server.kerberos.principal']
  else:
    livy_kerberos_principal = config['configurations']['livy-conf']['livy.server.launch.kerberos.principal']

  livy_livyserver_hosts = default("/clusterHostInfo/livy_server_hosts", [])

  # ats 1.5 properties
  entity_groupfs_active_dir = config['configurations']['yarn-site']['yarn.timeline-service.entity-group-fs-store.active-dir']
  entity_groupfs_active_dir_mode = 01777
  entity_groupfs_store_dir = config['configurations']['yarn-site']['yarn.timeline-service.entity-group-fs-store.done-dir']
  entity_groupfs_store_dir_mode = 0700
  is_webhdfs_enabled = hdfs_site['dfs.webhdfs.enabled']

  if len(livy_livyserver_hosts) > 0:
    has_livyserver = True
    if security_enabled:
      livy_principal = livy_kerberos_principal.replace('_HOST', config['hostname'].lower())

  livy_livyserver_port = default('configurations/livy-conf/livy.server.port',8999)


import functools
#create partial functions with common arguments for every HdfsResource call
#to create/delete hdfs directory/file/copyfromlocal we need to call params.HdfsResource in code
HdfsResource = functools.partial(
  HdfsResource,
  user=hdfs_user,
  hdfs_resource_ignore_file = hdfs_resource_ignore_file,
  security_enabled = security_enabled,
  keytab = hdfs_user_keytab,
  kinit_path_local = kinit_path_local,
  hadoop_bin_dir = hadoop_bin_dir,
  hadoop_conf_dir = hadoop_conf_dir,
  principal_name = hdfs_principal_name,
  hdfs_site = hdfs_site,
  default_fs = default_fs,
  immutable_paths = get_not_managed_resources(),
  dfs_type = dfs_type
)

