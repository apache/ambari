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

from resource_management.libraries.functions.default import default
from resource_management import *
from resource_management.libraries.functions import conf_select
from resource_management.libraries.functions import stack_select
from resource_management.libraries.functions.version import format_stack_version
from spark import *
import status_params


# a map of the Ambari role to the component name
# for use with /usr/iop/current/<component>
SERVER_ROLE_DIRECTORY_MAP = {
  'SPARK_JOBHISTORYSERVER' : 'spark-historyserver',
  'SPARK_CLIENT' : 'spark-client',
  'SPARK_THRIFTSERVER' : 'spark-thriftserver'
}
upgrade_direction = default("/commandParams/upgrade_direction", None)

component_directory = Script.get_component_from_role(SERVER_ROLE_DIRECTORY_MAP, "SPARK_CLIENT")

config = Script.get_config()
tmp_dir = Script.get_tmp_dir()

# New Cluster Stack Version that is defined during the RESTART of a Rolling Upgrade
version = default("/commandParams/version", None)
stack_name = default("/hostLevelParams/stack_name", None)
iop_full_version = format_stack_version(version)

hadoop_home = "/usr/iop/current/hadoop-client"
spark_conf = format("/usr/iop/current/{component_directory}/conf")
spark_log_dir = config['configurations']['spark-env']['spark_log_dir']
spark_pid_dir = status_params.spark_pid_dir
spark_role_root = "spark-client"

command_role = default("/role", "")

if command_role == "SPARK_CLIENT":
  spark_role_root = "spark-client"
elif command_role == "SPARK_JOBHISTORYSERVER":
  spark_role_root = "spark-historyserver"
elif command_role == "SPARK_THRIFTSERVER":
  spark_role_root = "spark-thriftserver"

spark_home = format("/usr/iop/current/{spark_role_root}")
if not os.path.exists(spark_home):
  os.symlink('/usr/iop/current/spark', spark_home)

java_home = config['hostLevelParams']['java_home']

spark_user = status_params.spark_user
hive_user = status_params.hive_user
spark_group = status_params.spark_group
user_group = status_params.user_group

spark_hdfs_user_dir = format("/user/{spark_user}")
spark_hdfs_user_mode = 0755
spark_eventlog_dir_mode = 01777
spark_jar_hdfs_dir = "/iop/apps/" + str(iop_full_version) + "/spark/jars"
spark_jar_hdfs_dir_mode = 0755
spark_jar_file_mode = 0444
spark_jar_src_dir = "/usr/iop/current/spark-historyserver/lib"
spark_jar_src_file = "spark-assembly.jar"

spark_history_server_pid_file = status_params.spark_history_server_pid_file
spark_thrift_server_pid_file = status_params.spark_thrift_server_pid_file

spark_history_server_start = format("{spark_home}/sbin/start-history-server.sh")
spark_history_server_stop = format("{spark_home}/sbin/stop-history-server.sh")

spark_thrift_server_start = format("{spark_home}/sbin/start-thriftserver.sh")
spark_thrift_server_stop = format("{spark_home}/sbin/stop-thriftserver.sh")

spark_submit_cmd = format("{spark_home}/bin/spark-submit")
spark_smoke_example = "org.apache.spark.examples.SparkPi"
spark_service_check_cmd = format(
  "{spark_submit_cmd} --class {spark_smoke_example}  --master yarn-cluster  --num-executors 1 --driver-memory 256m  --executor-memory 256m   --executor-cores 1  {spark_home}/lib/spark-examples*.jar 1")

spark_jobhistoryserver_hosts = default("/clusterHostInfo/spark_jobhistoryserver_hosts", [])
spark_thriftserver_hosts = default("/clusterHostInfo/spark_thriftserver_hosts", [])
namenode_hosts = default("/clusterHostInfo/namenode_host", [])
has_namenode = not len(namenode_hosts) == 0

if len(spark_jobhistoryserver_hosts) > 0:
  spark_history_server_host = spark_jobhistoryserver_hosts[0]
else:
  spark_history_server_host = "localhost"

if len(spark_thriftserver_hosts) > 0:
  spark_thrift_server_host = spark_thriftserver_hosts[0]
else:
  spark_thrift_server_host = "localhost"
# spark-defaults params
if has_namenode: 
  namenode_host = str(namenode_hosts[0])
else:
  namenode_host = "localhost"

hadoop_fs_defaultfs = config['configurations']['core-site']['fs.defaultFS']
spark_eventlog_dir_default=hadoop_fs_defaultfs + config['configurations']['spark-defaults']['spark.eventLog.dir']
spark_yarn_jar_default=hadoop_fs_defaultfs + '/iop/apps/' + str(iop_full_version) + '/spark/jars/spark-assembly.jar'

spark_yarn_applicationMaster_waitTries = default(
  "/configurations/spark-defaults/spark.yarn.applicationMaster.waitTries", '10')
spark_yarn_submit_file_replication = default("/configurations/spark-defaults/spark.yarn.submit.file.replication", '3')
spark_yarn_preserve_staging_files = default("/configurations/spark-defaults/spark.yarn.preserve.staging.files", "false")
spark_yarn_scheduler_heartbeat_interval = default(
  "/configurations/spark-defaults/spark.yarn.scheduler.heartbeat.interval-ms", "5000")
spark_yarn_queue = default("/configurations/spark-defaults/spark.yarn.queue", "default")
spark_yarn_containerLauncherMaxThreads = default(
  "/configurations/spark-defaults/spark.yarn.containerLauncherMaxThreads", "25")
spark_yarn_max_executor_failures = default("/configurations/spark-defaults/spark.yarn.max.executor.failures", "3")
spark_yarn_executor_memoryOverhead = default("/configurations/spark-defaults/spark.yarn.executor.memoryOverhead", "384")
spark_yarn_driver_memoryOverhead = default("/configurations/spark-defaults/spark.yarn.driver.memoryOverhead", "384")
spark_history_ui_port = default("/configurations/spark-defaults/spark.history.ui.port", "18080")
spark_thriftserver_port = default("/configurations/spark-env/spark_thriftserver_port", "10015")
spark_eventlog_enabled = default("/configurations/spark-defaults/spark.eventLog.enabled", "true")
spark_eventlog_dir = default("/configurations/spark-defaults/spark.eventLog.dir", spark_eventlog_dir_default)
spark_yarn_jar = default("/configurations/spark-defaults/spark.yarn.jar", spark_yarn_jar_default)
spark_thriftserver_ui_port = 4039

# add the properties that cannot be configured thru UI
spark_conf_properties_map = dict(config['configurations']['spark-defaults'])
spark_conf_properties_map["spark.yarn.historyServer.address"] = spark_history_server_host + ":" + str(spark_history_ui_port)
spark_conf_properties_map["spark.yarn.jar"] = spark_yarn_jar
spark_conf_properties_map["spark.eventLog.dir"] = spark_eventlog_dir_default

spark_env_sh = config['configurations']['spark-env']['content']
spark_log4j = config['configurations']['spark-log4j']['content']
#spark_metrics_properties = config['configurations']['spark-metrics-properties']['content']
spark_javaopts_properties = config['configurations']['spark-javaopts-properties']['content']
hive_server_host = default("/clusterHostInfo/hive_server_host", [])
is_hive_installed = not len(hive_server_host) == 0

spark_driver_extraJavaOptions = str(config['configurations']['spark-defaults']['spark.driver.extraJavaOptions'])
if spark_driver_extraJavaOptions.find('-Diop.version') == -1:
  spark_driver_extraJavaOptions = spark_driver_extraJavaOptions + ' -Diop.version=' + str(iop_full_version)

spark_yarn_am_extraJavaOptions = str(config['configurations']['spark-defaults']['spark.yarn.am.extraJavaOptions'])
if spark_yarn_am_extraJavaOptions.find('-Diop.version') == -1:
  spark_yarn_am_extraJavaOptions = spark_yarn_am_extraJavaOptions + ' -Diop.version=' + str(iop_full_version)

spark_javaopts_properties = str(spark_javaopts_properties)
if spark_javaopts_properties.find('-Diop.version') == -1:
  spark_javaopts_properties = spark_javaopts_properties+ ' -Diop.version=' + str(iop_full_version)

security_enabled = config['configurations']['cluster-env']['security_enabled']
kinit_path_local = functions.get_kinit_path()
spark_kerberos_keytab =  config['configurations']['spark-defaults']['spark.history.kerberos.keytab']
spark_kerberos_principal =  config['configurations']['spark-defaults']['spark.history.kerberos.principal']
smokeuser = config['configurations']['cluster-env']['smokeuser']
smoke_user_keytab = config['configurations']['cluster-env']['smokeuser_keytab']
smokeuser_principal = config['configurations']['cluster-env']['smokeuser_principal_name']
if security_enabled:
  spark_principal = spark_kerberos_principal.replace('_HOST',spark_history_server_host.lower())
# for create_hdfs_directory

# To create hdfs directory
hadoop_conf_dir = conf_select.get_hadoop_conf_dir()
hadoop_bin_dir = stack_select.get_hadoop_dir("bin")

hostname = config["hostname"]
hdfs_user_keytab = config['configurations']['hadoop-env']['hdfs_user_keytab']
hdfs_user = config['configurations']['hadoop-env']['hdfs_user']
hdfs_principal_name = config['configurations']['hadoop-env']['hdfs_principal_name']
kinit_path_local = functions.get_kinit_path()

hdfs_site = config['configurations']['hdfs-site']
default_fs = config['configurations']['core-site']['fs.defaultFS']

# Hiveserver 2 properties
hive_server2_authentication = config['configurations']['hive-site']['hive.server2.authentication']
hive_transport_mode = config['configurations']['hive-site']['hive.server2.transport.mode']
hive_server_principal = config['configurations']['hive-site']['hive.server2.authentication.kerberos.principal']
hive_http_endpoint = config['configurations']['hive-site']['hive.server2.thrift.http.path']
hive_ssl = config['configurations']['hive-site']['hive.server2.use.SSL']
if hive_ssl:
  hive_ssl_keystore_path = str(config['configurations']['hive-site']['hive.server2.keystore.path'])
  hive_ssl_keystore_password = str(config['configurations']['hive-site']['hive.server2.keystore.password'])
else:
  hive_ssl_keystore_path = None
  hive_ssl_keystore_password = None

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
