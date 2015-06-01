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


import status_params

from setup_spark import *

from resource_management import *
import resource_management.libraries.functions
from resource_management.libraries.functions import conf_select
from resource_management.libraries.functions import hdp_select
from resource_management.libraries.functions import format
from resource_management.libraries.functions.version import format_hdp_stack_version
from resource_management.libraries.functions.default import default
from resource_management.libraries.functions import get_kinit_path

from resource_management.libraries.script.script import Script

# a map of the Ambari role to the component name
# for use with /usr/hdp/current/<component>
SERVER_ROLE_DIRECTORY_MAP = {
  'SPARK_JOBHISTORYSERVER' : 'spark-historyserver',
  'SPARK_CLIENT' : 'spark-client'
}

component_directory = Script.get_component_from_role(SERVER_ROLE_DIRECTORY_MAP, "SPARK_CLIENT")

config = Script.get_config()
tmp_dir = Script.get_tmp_dir()

stack_name = default("/hostLevelParams/stack_name", None)
stack_version_unformatted = str(config['hostLevelParams']['stack_version'])
hdp_stack_version = format_hdp_stack_version(stack_version_unformatted)
host_sys_prepped = default("/hostLevelParams/host_sys_prepped", False)

# New Cluster Stack Version that is defined during the RESTART of a Rolling Upgrade
version = default("/commandParams/version", None)

# TODO! FIXME! Version check is not working as of today :
#   $ yum list installed | grep hdp-select
#   hdp-select.noarch                            2.2.1.0-2340.el6           @HDP-2.2
# And hdp_stack_version returned from hostLevelParams/stack_version is : 2.2.0.0
# Commenting out for time being
#stack_is_hdp22_or_further = hdp_stack_version != "" and compare_versions(hdp_stack_version, '2.2.1.0') >= 0

spark_conf = '/etc/spark/conf'
hadoop_conf_dir = conf_select.get_hadoop_conf_dir()
hadoop_bin_dir = hdp_select.get_hadoop_dir("bin")

if Script.is_hdp_stack_greater_or_equal("2.2"):
  hadoop_home = hdp_select.get_hadoop_dir("home")
  spark_conf = format("/usr/hdp/current/{component_directory}/conf")
  spark_log_dir = config['configurations']['spark-env']['spark_log_dir']
  spark_pid_dir = status_params.spark_pid_dir
  spark_home = format("/usr/hdp/current/{component_directory}")


java_home = config['hostLevelParams']['java_home']

hdfs_user = config['configurations']['hadoop-env']['hdfs_user']
hdfs_principal_name = config['configurations']['hadoop-env']['hdfs_principal_name']
hdfs_user_keytab = config['configurations']['hadoop-env']['hdfs_user_keytab']

spark_user = status_params.spark_user
spark_group = status_params.spark_group
user_group = status_params.user_group
spark_hdfs_user_dir = format("/user/{spark_user}")
spark_history_server_pid_file = status_params.spark_history_server_pid_file

spark_history_server_start = format("{spark_home}/sbin/start-history-server.sh")
spark_history_server_stop = format("{spark_home}/sbin/stop-history-server.sh")

spark_submit_cmd = format("{spark_home}/bin/spark-submit")
spark_smoke_example = "org.apache.spark.examples.SparkPi"
spark_service_check_cmd = format(
  "{spark_submit_cmd} --class {spark_smoke_example}  --master yarn-cluster  --num-executors 1 --driver-memory 256m  --executor-memory 256m   --executor-cores 1  {spark_home}/lib/spark-examples*.jar 1")

spark_jobhistoryserver_hosts = default("/clusterHostInfo/spark_jobhistoryserver_hosts", [])

if len(spark_jobhistoryserver_hosts) > 0:
  spark_history_server_host = spark_jobhistoryserver_hosts[0]
else:
  spark_history_server_host = "localhost"

# spark-defaults params
spark_hive_sec_authorization_enabled = "false"
spark_yarn_historyServer_address = default(spark_history_server_host, "localhost")

spark_history_ui_port = config['configurations']['spark-defaults']['spark.history.ui.port']

spark_env_sh = config['configurations']['spark-env']['content']
spark_log4j_properties = config['configurations']['spark-log4j-properties']['content']
spark_metrics_properties = config['configurations']['spark-metrics-properties']['content']
spark_javaopts_properties = config['configurations']['spark-javaopts-properties']['content']

hive_server_host = default("/clusterHostInfo/hive_server_host", [])
is_hive_installed = not len(hive_server_host) == 0

hdp_full_version = functions.get_hdp_version('spark-client')

spark_driver_extraJavaOptions = str(config['configurations']['spark-defaults']['spark.driver.extraJavaOptions'])
if spark_driver_extraJavaOptions.find('-Dhdp.version') == -1:
  spark_driver_extraJavaOptions = spark_driver_extraJavaOptions + ' -Dhdp.version=' + str(hdp_full_version)

spark_yarn_am_extraJavaOptions = str(config['configurations']['spark-defaults']['spark.yarn.am.extraJavaOptions'])
if spark_yarn_am_extraJavaOptions.find('-Dhdp.version') == -1:
  spark_yarn_am_extraJavaOptions = spark_yarn_am_extraJavaOptions + ' -Dhdp.version=' + str(hdp_full_version)

spark_javaopts_properties = str(spark_javaopts_properties)
if spark_javaopts_properties.find('-Dhdp.version') == -1:
  spark_javaopts_properties = spark_javaopts_properties+ ' -Dhdp.version=' + str(hdp_full_version)

security_enabled = config['configurations']['cluster-env']['security_enabled']
kinit_path_local = get_kinit_path(default('/configurations/kerberos-env/executable_search_paths', None))
spark_kerberos_keytab =  config['configurations']['spark-defaults']['spark.history.kerberos.keytab']
spark_kerberos_principal =  config['configurations']['spark-defaults']['spark.history.kerberos.principal']

spark_hive_properties = {
  'hive.metastore.uris': config['configurations']['hive-site']['hive.metastore.uris']
}

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
      'hive.security.authorization.enabled': spark_hive_sec_authorization_enabled,
      'hive.server2.enable.doAs': str(config['configurations']['hive-site']['hive.server2.enable.doAs']).lower()
    })
  
default_fs = config['configurations']['core-site']['fs.defaultFS']
hdfs_site = config['configurations']['hdfs-site']


import functools
#create partial functions with common arguments for every HdfsResource call
#to create/delete hdfs directory/file/copyfromlocal we need to call params.HdfsResource in code
HdfsResource = functools.partial(
  HdfsResource,
  user=hdfs_user,
  security_enabled = security_enabled,
  keytab = hdfs_user_keytab,
  kinit_path_local = kinit_path_local,
  hadoop_bin_dir = hadoop_bin_dir,
  hadoop_conf_dir = hadoop_conf_dir,
  principal_name = hdfs_principal_name,
  hdfs_site = hdfs_site,
  default_fs = default_fs
 )
