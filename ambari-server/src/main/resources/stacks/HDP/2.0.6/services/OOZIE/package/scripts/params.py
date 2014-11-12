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

from resource_management.libraries.functions.version import format_hdp_stack_version, compare_versions
from resource_management import *
import status_params
import itertools
import os

# server configurations
config = Script.get_config()
tmp_dir = Script.get_tmp_dir()

stack_version_unformatted = str(config['hostLevelParams']['stack_version'])
hdp_stack_version = format_hdp_stack_version(stack_version_unformatted)
stack_is_hdp22_or_further = hdp_stack_version != "" and compare_versions(hdp_stack_version, '2.2') >= 0

#hadoop params
if stack_is_hdp22_or_further:
  hadoop_bin_dir = "/usr/hdp/current/hadoop-client/bin"
  hadoop_lib_home = "/usr/hdp/current/hadoop-client/lib"
  oozie_lib_dir = "/usr/hdp/current/oozie-client/"
  oozie_setup_sh = "/usr/hdp/current/oozie-client/bin/oozie-setup.sh"
  oozie_webapps_dir = "/usr/hdp/current/oozie-client/oozie-server/webapps"
  oozie_webapps_conf_dir = "/usr/hdp/current/oozie-client/oozie-server/conf"
  oozie_libext_dir = "/usr/hdp/current/oozie-client/libext"
  oozie_server_dir = "/usr/hdp/current/oozie-client/oozie-server"
  oozie_shared_lib = "/usr/hdp/current/oozie-client/share"
  oozie_home = "/usr/hdp/current/oozie-client"
  oozie_bin_dir = "/usr/hdp/current/oozie-client/bin"
  falcon_home = '/usr/hdp/current/falcon-client'
else:
  hadoop_bin_dir = "/usr/bin"
  hadoop_lib_home = "/usr/lib/hadoop/lib"
  oozie_lib_dir = "/var/lib/oozie/"
  oozie_setup_sh = "/usr/lib/oozie/bin/oozie-setup.sh"
  oozie_webapps_dir = "/var/lib/oozie/oozie-server/webapps/"
  oozie_webapps_conf_dir = "/var/lib/oozie/oozie-server/conf"
  oozie_libext_dir = "/usr/lib/oozie/libext"
  oozie_server_dir = "/var/lib/oozie/oozie-server"
  oozie_shared_lib = "/usr/lib/oozie/share"
  oozie_home = "/usr/lib/oozie"
  oozie_bin_dir = "/usr/bin"
  falcon_home = '/usr/lib/falcon'

execute_path = oozie_bin_dir + os.pathsep + hadoop_bin_dir

hadoop_conf_dir = "/etc/hadoop/conf"
conf_dir = "/etc/oozie/conf"
hive_conf_dir = "/etc/oozie/conf/action-conf/hive"
oozie_user = config['configurations']['oozie-env']['oozie_user']
smokeuser = config['configurations']['cluster-env']['smokeuser']
user_group = config['configurations']['cluster-env']['user_group']
jdk_location = config['hostLevelParams']['jdk_location']
check_db_connection_jar_name = "DBConnectionVerification.jar"
check_db_connection_jar = format("/usr/lib/ambari-agent/{check_db_connection_jar_name}")
oozie_tmp_dir = "/var/tmp/oozie"
oozie_hdfs_user_dir = format("/user/{oozie_user}")
oozie_pid_dir = status_params.oozie_pid_dir
pid_file = status_params.pid_file
hadoop_jar_location = "/usr/lib/hadoop/"
# for HDP1 it's "/usr/share/HDP-oozie/ext.zip"
ext_js_path = "/usr/share/HDP-oozie/ext-2.2.zip"
security_enabled = config['configurations']['cluster-env']['security_enabled']
oozie_heapsize = config['configurations']['oozie-env']['oozie_heapsize']
oozie_permsize = config['configurations']['oozie-env']['oozie_permsize']

kinit_path_local = functions.get_kinit_path(["/usr/bin", "/usr/kerberos/bin", "/usr/sbin"])
oozie_service_keytab = config['configurations']['oozie-site']['oozie.service.HadoopAccessorService.keytab.file']
oozie_principal = config['configurations']['oozie-site']['oozie.service.HadoopAccessorService.kerberos.principal']
smokeuser_keytab = config['configurations']['cluster-env']['smokeuser_keytab']
oozie_keytab = config['configurations']['oozie-env']['oozie_keytab']
oozie_env_sh_template = config['configurations']['oozie-env']['content']

oracle_driver_jar_name = "ojdbc6.jar"

java_home = config['hostLevelParams']['java_home']
oozie_metastore_user_name = config['configurations']['oozie-site']['oozie.service.JPAService.jdbc.username']
oozie_metastore_user_passwd = default("/configurations/oozie-site/oozie.service.JPAService.jdbc.password","")
oozie_jdbc_connection_url = default("/configurations/oozie-site/oozie.service.JPAService.jdbc.url", "")
oozie_log_dir = config['configurations']['oozie-env']['oozie_log_dir']
oozie_data_dir = config['configurations']['oozie-env']['oozie_data_dir']
oozie_server_port = get_port_from_url(config['configurations']['oozie-site']['oozie.base.url'])
oozie_server_admin_port = config['configurations']['oozie-env']['oozie_admin_port']
fs_root = config['configurations']['core-site']['fs.defaultFS']

if str(hdp_stack_version).startswith('2.0') or str(hdp_stack_version).startswith('2.1'):
  put_shared_lib_to_hdfs_cmd = format("hadoop --config {hadoop_conf_dir} dfs -put {oozie_shared_lib} {oozie_hdfs_user_dir}")
# for newer
else:
  put_shared_lib_to_hdfs_cmd = format("{oozie_setup_sh} sharelib create -fs {fs_root} -locallib {oozie_shared_lib}")
  
jdbc_driver_name = default("/configurations/oozie-site/oozie.service.JPAService.jdbc.driver", "")

if jdbc_driver_name == "com.mysql.jdbc.Driver":
  jdbc_driver_jar = "/usr/share/java/mysql-connector-java.jar"
elif jdbc_driver_name == "org.postgresql.Driver":
  jdbc_driver_jar = format("{oozie_home}/libserver/postgresql-9.0-801.jdbc4.jar")
elif jdbc_driver_name == "oracle.jdbc.driver.OracleDriver":
  jdbc_driver_jar = "/usr/share/java/ojdbc6.jar"
else:
  jdbc_driver_jar = ""

hostname = config["hostname"]
ambari_server_hostname = config['clusterHostInfo']['ambari_server_host'][0]
falcon_host = default("/clusterHostInfo/falcon_server_hosts", [])
has_falcon_host = not len(falcon_host)  == 0

#oozie-log4j.properties
if (('oozie-log4j' in config['configurations']) and ('content' in config['configurations']['oozie-log4j'])):
  log4j_props = config['configurations']['oozie-log4j']['content']
else:
  log4j_props = None

oozie_hdfs_user_mode = 0775
hdfs_user_keytab = config['configurations']['hadoop-env']['hdfs_user_keytab']
hdfs_user = config['configurations']['hadoop-env']['hdfs_user']
hdfs_principal_name = config['configurations']['hadoop-env']['hdfs_principal_name']
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

#LZO support

io_compression_codecs = config['configurations']['core-site']['io.compression.codecs']
lzo_enabled = "com.hadoop.compression.lzo" in io_compression_codecs
# stack_is_hdp22_or_further
underscorred_version = stack_version_unformatted.replace('.', '_')
dashed_version = stack_version_unformatted.replace('.', '-')
lzo_packages_to_family = {
  "any": ["hadoop-lzo"],
  "redhat": ["lzo", "hadoop-lzo-native"],
  "suse": ["lzo", "hadoop-lzo-native"],
  "ubuntu": ["liblzo2-2"]
}

if stack_is_hdp22_or_further:
  lzo_packages_to_family["redhat"] += [format("hadooplzo_{underscorred_version}_*")]
  lzo_packages_to_family["suse"] += [format("hadooplzo_{underscorred_version}_*")]
  lzo_packages_to_family["ubuntu"] += [format("hadooplzo_{dashed_version}_*")]

lzo_packages_for_current_host = lzo_packages_to_family['any'] + lzo_packages_to_family[System.get_instance().os_family]
all_lzo_packages = set(itertools.chain(*lzo_packages_to_family.values()))

