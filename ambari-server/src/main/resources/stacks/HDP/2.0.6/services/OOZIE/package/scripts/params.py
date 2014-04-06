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

from resource_management import *
import status_params

# server configurations
config = Script.get_config()

oozie_user = config['configurations']['global']['oozie_user']
smokeuser = config['configurations']['global']['smokeuser']
conf_dir = "/etc/oozie/conf"
hadoop_conf_dir = "/etc/hadoop/conf"
user_group = config['configurations']['global']['user_group']
jdk_location = config['hostLevelParams']['jdk_location']
check_db_connection_jar_name = "DBConnectionVerification.jar"
check_db_connection_jar = format("/usr/lib/ambari-agent/{check_db_connection_jar_name}")
hadoop_prefix = "/usr"
oozie_tmp_dir = "/var/tmp/oozie"
oozie_hdfs_user_dir = format("/user/{oozie_user}")
oozie_pid_dir = status_params.oozie_pid_dir
pid_file = status_params.pid_file
hadoop_jar_location = "/usr/lib/hadoop/"
# for HDP1 it's "/usr/share/HDP-oozie/ext.zip"
ext_js_path = "/usr/share/HDP-oozie/ext-2.2.zip"
oozie_libext_dir = "/usr/lib/oozie/libext"
lzo_enabled = config['configurations']['global']['lzo_enabled']
_authentication = config['configurations']['core-site']['hadoop.security.authentication']
security_enabled = ( not is_empty(_authentication) and _authentication == 'kerberos')

kinit_path_local = functions.get_kinit_path([default("kinit_path_local",None), "/usr/bin", "/usr/kerberos/bin", "/usr/sbin"])
oozie_service_keytab = config['configurations']['oozie-site']['oozie.service.HadoopAccessorService.keytab.file']
oozie_principal = config['configurations']['oozie-site']['oozie.service.HadoopAccessorService.kerberos.principal']
smokeuser_keytab = config['configurations']['global']['smokeuser_keytab']
oozie_keytab = config['configurations']['global']['oozie_keytab']

oracle_driver_jar_name = "ojdbc6.jar"
java_share_dir = "/usr/share/java"

java_home = config['hostLevelParams']['java_home']
oozie_metastore_user_name = config['configurations']['oozie-site']['oozie.service.JPAService.jdbc.username']
oozie_metastore_user_passwd = default("/configurations/oozie-site/oozie.service.JPAService.jdbc.password","")
oozie_jdbc_connection_url = default("/configurations/oozie-site/oozie.service.JPAService.jdbc.url", "")
oozie_log_dir = config['configurations']['global']['oozie_log_dir']
oozie_data_dir = config['configurations']['global']['oozie_data_dir']
oozie_lib_dir = "/var/lib/oozie/"
oozie_webapps_dir = "/var/lib/oozie/oozie-server/webapps/"

jdbc_driver_name = default("/configurations/oozie-site/oozie.service.JPAService.jdbc.driver", "")

if jdbc_driver_name == "com.mysql.jdbc.Driver":
  jdbc_driver_jar = "/usr/share/java/mysql-connector-java.jar"
elif jdbc_driver_name == "org.postgresql.Driver":
  jdbc_driver_jar = "/usr/lib/oozie/libserver/postgresql-9.0-801.jdbc4.jar"
elif jdbc_driver_name == "oracle.jdbc.driver.OracleDriver":
  jdbc_driver_jar = "/usr/share/java/ojdbc6.jar"
else:
  jdbc_driver_jar = ""

hostname = config["hostname"]
falcon_host = default("/clusterHostInfo/falcon_server_hosts", [])
has_falcon_host = not len(falcon_host)  == 0
falcon_home = '/usr/lib/falcon'

#oozie-log4j.properties
if (('oozie-log4j' in config['configurations']) and ('content' in config['configurations']['oozie-log4j'])):
  log4j_props = config['configurations']['oozie-log4j']['content']
else:
  log4j_props = None

oozie_hdfs_user_dir = format("/user/{oozie_user}")
oozie_hdfs_user_mode = 0775
#for create_hdfs_directory
hostname = config["hostname"]
hadoop_conf_dir = "/etc/hadoop/conf"
hdfs_user_keytab = config['configurations']['global']['hdfs_user_keytab']
hdfs_user = config['configurations']['global']['hdfs_user']
kinit_path_local = functions.get_kinit_path([default("kinit_path_local",None), "/usr/bin", "/usr/kerberos/bin", "/usr/sbin"])
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
