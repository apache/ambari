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

from status_params import *

config = Script.get_config()

#RPM versioning support
rpm_version = default("/configurations/hadoop-env/rpm_version", None)

#hadoop params
if rpm_version is not None:
  hadoop_bin_dir = "/usr/hdp/current/hadoop/bin"
  falcon_webapp_dir = "/usr/hdp/current/falcon/webapp"
  falcon_home = "/usr/hdp/current/falcon"
else:
  hadoop_bin_dir = "/usr/bin"
  falcon_webapp_dir = '/var/lib/falcon/webapp'
  falcon_home = '/usr/lib/falcon'

hadoop_conf_dir = "/etc/hadoop/conf"
falcon_conf_dir = '/etc/falcon/conf'
oozie_user = config['configurations']['oozie-env']['oozie_user']
falcon_user = config['configurations']['falcon-env']['falcon_user']
smoke_user =  config['configurations']['cluster-env']['smokeuser']

user_group = config['configurations']['cluster-env']['user_group']
proxyuser_group =  config['configurations']['hadoop-env']['proxyuser_group']

java_home = config['hostLevelParams']['java_home']
falcon_local_dir = config['configurations']['falcon-env']['falcon_local_dir']
falcon_log_dir = config['configurations']['falcon-env']['falcon_log_dir']
store_uri = config['configurations']['falcon-startup.properties']['*.config.store.uri']

falcon_embeddedmq_data = config['configurations']['falcon-env']['falcon.embeddedmq.data']
falcon_embeddedmq_enabled = config['configurations']['falcon-env']['falcon.embeddedmq']
falcon_emeddedmq_port = config['configurations']['falcon-env']['falcon.emeddedmq.port']

falcon_host = config['clusterHostInfo']['falcon_server_hosts'][0]
falcon_port = config['configurations']['falcon-env']['falcon_port']
falcon_runtime_properties = config['configurations']['falcon-runtime.properties']
falcon_startup_properties = config['configurations']['falcon-startup.properties']
smokeuser_keytab = config['configurations']['cluster-env']['smokeuser_keytab']
falcon_env_sh_template = config['configurations']['falcon-env']['content']

flacon_apps_dir = '/apps/falcon'
#for create_hdfs_directory
security_enabled = config['configurations']['cluster-env']['security_enabled']
hostname = config["hostname"]
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
  kinit_path_local = kinit_path_local,
  bin_dir = hadoop_bin_dir
)
