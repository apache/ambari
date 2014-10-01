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

from resource_management import *
import status_params

# server configurations
config = Script.get_config()
tmp_dir = Script.get_tmp_dir()

hcat_user = config['configurations']['hive-env']['hcat_user']
webhcat_user = config['configurations']['hive-env']['webhcat_user']

if str(config['hostLevelParams']['stack_version']).startswith('2.0'):
  config_dir = '/etc/hcatalog/conf'
  webhcat_bin_dir = '/usr/lib/hcatalog/sbin'
# for newer versions
else:
  config_dir = '/etc/hive-webhcat/conf'
  webhcat_bin_dir = '/usr/lib/hive-hcatalog/sbin'

webhcat_env_sh_template = config['configurations']['webhcat-env']['content']
templeton_log_dir = config['configurations']['hive-env']['hcat_log_dir']
templeton_pid_dir = status_params.templeton_pid_dir

pid_file = status_params.pid_file

hadoop_conf_dir = config['configurations']['webhcat-site']['templeton.hadoop.conf.dir']
templeton_jar = config['configurations']['webhcat-site']['templeton.jar']

hadoop_home = '/usr/lib/hadoop'
user_group = config['configurations']['cluster-env']['user_group']

webhcat_server_host = config['clusterHostInfo']['webhcat_server_host']

webhcat_apps_dir = "/apps/webhcat"
smoke_user_keytab = config['configurations']['cluster-env']['smokeuser_keytab']
smokeuser = config['configurations']['cluster-env']['smokeuser']
security_enabled = config['configurations']['cluster-env']['security_enabled']
kinit_path_local = functions.get_kinit_path(["/usr/bin", "/usr/kerberos/bin", "/usr/sbin"])

hcat_hdfs_user_dir = format("/user/{hcat_user}")
hcat_hdfs_user_mode = 0755
webhcat_hdfs_user_dir = format("/user/{webhcat_user}")
webhcat_hdfs_user_mode = 0755
webhcat_apps_dir = "/apps/webhcat"
#for create_hdfs_directory
hostname = config["hostname"]
hadoop_conf_dir = "/etc/hadoop/conf"
security_param = "true" if security_enabled else "false"
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
