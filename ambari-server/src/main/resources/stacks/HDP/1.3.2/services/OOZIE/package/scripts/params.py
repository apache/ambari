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
tmp_dir = Script.get_tmp_dir()

ambari_server_hostname = config['clusterHostInfo']['ambari_server_host'][0]
oozie_user = config['configurations']['oozie-env']['oozie_user']
smokeuser = config['configurations']['cluster-env']['smokeuser']
conf_dir = "/etc/oozie/conf"
hadoop_conf_dir = "/etc/hadoop/conf"
user_group = config['configurations']['cluster-env']['user_group']
jdk_location = config['hostLevelParams']['jdk_location']
check_db_connection_jar_name = "DBConnectionVerification.jar"
check_db_connection_jar = format("/usr/lib/ambari-agent/{check_db_connection_jar_name}")
hadoop_prefix = "/usr"
oozie_tmp_dir = "/var/tmp/oozie"
oozie_hdfs_user_dir = format("/user/{oozie_user}")
oozie_pid_dir = status_params.oozie_pid_dir
pid_file = status_params.pid_file
hadoop_jar_location = "/usr/lib/hadoop/"
# for HDP2 it's "/usr/share/HDP-oozie/ext-2.2.zip"
ext_js_path = "/usr/share/HDP-oozie/ext.zip"
oozie_libext_dir = "/usr/lib/oozie/libext"
lzo_enabled = config['configurations']['mapred-env']['lzo_enabled']
security_enabled = config['configurations']['cluster-env']['security_enabled']

kinit_path_local = functions.get_kinit_path(["/usr/bin", "/usr/kerberos/bin", "/usr/sbin"])
oozie_service_keytab = config['configurations']['oozie-site']['oozie.service.HadoopAccessorService.keytab.file']
oozie_principal = config['configurations']['oozie-site']['oozie.service.HadoopAccessorService.kerberos.principal']
smokeuser_keytab = config['configurations']['cluster-env']['smokeuser_keytab']
oozie_keytab = config['configurations']['oozie-env']['oozie_keytab']

oracle_driver_jar_name = "ojdbc6.jar"
java_share_dir = "/usr/share/java"

java_home = config['hostLevelParams']['java_home']
oozie_metastore_user_name = config['configurations']['oozie-site']['oozie.service.JPAService.jdbc.username']
oozie_metastore_user_passwd = default("/configurations/oozie-site/oozie.service.JPAService.jdbc.password","")
oozie_jdbc_connection_url = default("/configurations/oozie-site/oozie.service.JPAService.jdbc.url", "")
oozie_log_dir = config['configurations']['oozie-env']['oozie_log_dir']
oozie_data_dir = config['configurations']['oozie-env']['oozie_data_dir']
oozie_server_port = get_port_from_url(config['configurations']['oozie-site']['oozie.base.url'])
oozie_server_admin_port = config['configurations']['oozie-env']['oozie_admin_port']
oozie_lib_dir = "/var/lib/oozie/"
oozie_webapps_dir = "/var/lib/oozie/oozie-server/webapps/"

jdbc_driver_name = default("/configurations/oozie-site/oozie.service.JPAService.jdbc.driver", "")
oozie_env_sh_template = config['configurations']['oozie-env']['content']

if jdbc_driver_name == "com.mysql.jdbc.Driver":
  jdbc_driver_jar = "/usr/share/java/mysql-connector-java.jar"
elif jdbc_driver_name == "org.postgresql.Driver":
  jdbc_driver_jar = "/usr/lib/oozie/libtools/postgresql-9.0-801.jdbc4.jar"
elif jdbc_driver_name == "oracle.jdbc.driver.OracleDriver":
  jdbc_driver_jar = "/usr/share/java/ojdbc6.jar"
else:
  jdbc_driver_jar = ""
  
if lzo_enabled or jdbc_driver_name:
  jar_option = "-jars"         
else:
  jar_option = ""
  
lzo_jar_suffix = '`LZO_JARS=($(find /usr/lib/hadoop/lib/ -name "hadoop-lzo-*")); echo ${LZO_JARS[0]}`' if lzo_enabled else ""
  
if lzo_enabled and jdbc_driver_name:
    jar_path = format("{lzo_jar_suffix}:{jdbc_driver_jar}")        
else:
    jar_path = "{lzo_jar_suffix}{jdbc_driver_jar}"

#oozie-log4j.properties
if (('oozie-log4j' in config['configurations']) and ('content' in config['configurations']['oozie-log4j'])):
  log4j_props = config['configurations']['oozie-log4j']['content']
else:
  log4j_props = None

#hdfs directories
oozie_hdfs_user_dir = format("/user/{oozie_user}")
oozie_hdfs_user_mode = 0775
#for create_hdfs_directory
hostname = config["hostname"]
hadoop_conf_dir = "/etc/hadoop/conf"
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
