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
from ambari_commons.constants import AMBARI_SUDO_BINARY
from resource_management.libraries.functions import format
from resource_management.libraries.functions import conf_select
from resource_management.libraries.functions import hdp_select
from resource_management.libraries.functions.version import format_hdp_stack_version
from resource_management.libraries.functions.default import default
from resource_management.libraries.functions import get_kinit_path
from resource_management.libraries.functions import get_port_from_url
from resource_management.libraries.script.script import Script

from resource_management.libraries.functions.get_lzo_packages import get_lzo_packages

import status_params
import os

# server configurations
config = Script.get_config()
tmp_dir = Script.get_tmp_dir()
sudo = AMBARI_SUDO_BINARY

hostname = config["hostname"]

# New Cluster Stack Version that is defined during the RESTART of a Rolling Upgrade
version = default("/commandParams/version", None)
stack_name = default("/hostLevelParams/stack_name", None)
upgrade_direction = default("/commandParams/upgrade_direction", None)

stack_version_unformatted = str(config['hostLevelParams']['stack_version'])
hdp_stack_version = format_hdp_stack_version(stack_version_unformatted)

hadoop_conf_dir = conf_select.get_hadoop_conf_dir()
hadoop_bin_dir = hdp_select.get_hadoop_dir("bin")
hadoop_lib_home = hdp_select.get_hadoop_dir("lib")

#hadoop params
if Script.is_hdp_stack_greater_or_equal("2.2"):
  # oozie-server or oozie-client, depending on role
  oozie_root = status_params.component_directory

  # using the correct oozie root dir, format the correct location
  oozie_lib_dir = format("/usr/hdp/current/{oozie_root}")
  oozie_setup_sh = format("/usr/hdp/current/{oozie_root}/bin/oozie-setup.sh")
  oozie_webapps_dir = format("/usr/hdp/current/{oozie_root}/oozie-server/webapps")
  oozie_webapps_conf_dir = format("/usr/hdp/current/{oozie_root}/oozie-server/conf")
  oozie_libext_dir = format("/usr/hdp/current/{oozie_root}/libext")
  oozie_libext_customer_dir = format("/usr/hdp/current/{oozie_root}/libext-customer")
  oozie_server_dir = format("/usr/hdp/current/{oozie_root}/oozie-server")
  oozie_shared_lib = format("/usr/hdp/current/{oozie_root}/share")
  oozie_home = format("/usr/hdp/current/{oozie_root}")
  oozie_bin_dir = format("/usr/hdp/current/{oozie_root}/bin")
  oozie_examples_regex = format("/usr/hdp/current/{oozie_root}/doc")
  falcon_home = '/usr/hdp/current/falcon-client'

  conf_dir = format("/usr/hdp/current/{oozie_root}/conf")
  hive_conf_dir = format("{conf_dir}/action-conf/hive")

else:
  oozie_lib_dir = "/var/lib/oozie"
  oozie_setup_sh = "/usr/lib/oozie/bin/oozie-setup.sh"
  oozie_webapps_dir = "/var/lib/oozie/oozie-server/webapps/"
  oozie_webapps_conf_dir = "/var/lib/oozie/oozie-server/conf"
  oozie_libext_dir = "/usr/lib/oozie/libext"
  oozie_server_dir = "/var/lib/oozie/oozie-server"
  oozie_shared_lib = "/usr/lib/oozie/share"
  oozie_home = "/usr/lib/oozie"
  oozie_bin_dir = "/usr/bin"
  falcon_home = '/usr/lib/falcon'
  conf_dir = "/etc/oozie/conf"
  hive_conf_dir = "/etc/oozie/conf/action-conf/hive"
  oozie_examples_regex = "/usr/share/doc/oozie-*"

execute_path = oozie_bin_dir + os.pathsep + hadoop_bin_dir

oozie_user = config['configurations']['oozie-env']['oozie_user']
smokeuser = config['configurations']['cluster-env']['smokeuser']
smokeuser_principal = config['configurations']['cluster-env']['smokeuser_principal_name']
oozie_admin_users = format(config['configurations']['oozie-env']['oozie_admin_users'])
user_group = config['configurations']['cluster-env']['user_group']
jdk_location = config['hostLevelParams']['jdk_location']
check_db_connection_jar_name = "DBConnectionVerification.jar"
check_db_connection_jar = format("/usr/lib/ambari-agent/{check_db_connection_jar_name}")
oozie_tmp_dir = "/var/tmp/oozie"
oozie_hdfs_user_dir = format("/user/{oozie_user}")
oozie_pid_dir = status_params.oozie_pid_dir
pid_file = status_params.pid_file
hadoop_jar_location = "/usr/lib/hadoop/"
java_share_dir = "/usr/share/java"
# for HDP1 it's "/usr/share/HDP-oozie/ext.zip"
ext_js_file = "ext-2.2.zip"
ext_js_path = format("/usr/share/HDP-oozie/{ext_js_file}")
security_enabled = config['configurations']['cluster-env']['security_enabled']
oozie_heapsize = config['configurations']['oozie-env']['oozie_heapsize']
oozie_permsize = config['configurations']['oozie-env']['oozie_permsize']

kinit_path_local = get_kinit_path(default('/configurations/kerberos-env/executable_search_paths', None))
oozie_service_keytab = config['configurations']['oozie-site']['oozie.service.HadoopAccessorService.keytab.file']
oozie_principal = config['configurations']['oozie-site']['oozie.service.HadoopAccessorService.kerberos.principal']
http_principal = config['configurations']['oozie-site']['oozie.authentication.kerberos.principal']
oozie_site = config['configurations']['oozie-site']

if security_enabled and Script.is_hdp_stack_less_than("2.2"):
  #older versions of oozie have problems when using _HOST in principal
  oozie_site = dict(config['configurations']['oozie-site'])
  oozie_site['oozie.service.HadoopAccessorService.kerberos.principal'] = \
    oozie_principal.replace('_HOST', hostname)
  oozie_site['oozie.authentication.kerberos.principal'] = \
    http_principal.replace('_HOST', hostname)

smokeuser_keytab = config['configurations']['cluster-env']['smokeuser_keytab']
oozie_keytab = default("/configurations/oozie-env/oozie_keytab", oozie_service_keytab)
oozie_env_sh_template = config['configurations']['oozie-env']['content']

oracle_driver_jar_name = "ojdbc6.jar"

oozie_metastore_user_name = config['configurations']['oozie-site']['oozie.service.JPAService.jdbc.username']
oozie_metastore_user_passwd = default("/configurations/oozie-site/oozie.service.JPAService.jdbc.password","")
oozie_jdbc_connection_url = default("/configurations/oozie-site/oozie.service.JPAService.jdbc.url", "")
oozie_log_dir = config['configurations']['oozie-env']['oozie_log_dir']
oozie_data_dir = config['configurations']['oozie-env']['oozie_data_dir']
oozie_server_port = get_port_from_url(config['configurations']['oozie-site']['oozie.base.url'])
oozie_server_admin_port = config['configurations']['oozie-env']['oozie_admin_port']
if 'oozie.https.port' in config['configurations']['oozie-site'] or 'oozie.https.keystore.file' in config['configurations']['oozie-site'] or 'oozie.https.keystore.pass' in config['configurations']['oozie-site']:
  oozie_secure = '-secure'
else:
  oozie_secure = ''

hdfs_site = config['configurations']['hdfs-site']
fs_root = config['configurations']['core-site']['fs.defaultFS']

if Script.is_hdp_stack_greater_or_equal("2.0") and Script.is_hdp_stack_less_than("2.2"):
  put_shared_lib_to_hdfs_cmd = format("hadoop --config {hadoop_conf_dir} dfs -put {oozie_shared_lib} {oozie_hdfs_user_dir}")
# for newer
else:
  put_shared_lib_to_hdfs_cmd = format("{oozie_setup_sh} sharelib create -fs {fs_root} -locallib {oozie_shared_lib}")

jdbc_driver_name = default("/configurations/oozie-site/oozie.service.JPAService.jdbc.driver", "")

if jdbc_driver_name == "com.microsoft.sqlserver.jdbc.SQLServerDriver":
  jdbc_driver_jar = "sqljdbc4.jar"
  jdbc_symlink_name = "mssql-jdbc-driver.jar"
elif jdbc_driver_name == "com.mysql.jdbc.Driver":
  jdbc_driver_jar = "mysql-connector-java.jar"
  jdbc_symlink_name = "mysql-jdbc-driver.jar"
elif jdbc_driver_name == "org.postgresql.Driver":
  jdbc_driver_jar = format("{oozie_home}/libserver/postgresql-9.0-801.jdbc4.jar")  #oozie using it's own postgres jdbc
  jdbc_symlink_name = "postgres-jdbc-driver.jar"
elif jdbc_driver_name == "oracle.jdbc.driver.OracleDriver":
  jdbc_driver_jar = "ojdbc.jar"
  jdbc_symlink_name = "oracle-jdbc-driver.jar"
else:
  jdbc_driver_jar = ""
  jdbc_symlink_name = ""

driver_curl_source = format("{jdk_location}/{jdbc_symlink_name}")
driver_curl_target = format("{java_share_dir}/{jdbc_driver_jar}")
downloaded_custom_connector = format("{tmp_dir}/{jdbc_driver_jar}")
if jdbc_driver_name == "org.postgresql.Driver":
  target = jdbc_driver_jar
else:
  target = format("{oozie_libext_dir}/{jdbc_driver_jar}")


hdfs_share_dir = "/user/oozie/share"
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

hdfs_site = config['configurations']['hdfs-site']
default_fs = config['configurations']['core-site']['fs.defaultFS']
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

is_webhdfs_enabled = config['configurations']['hdfs-site']['dfs.webhdfs.enabled']

# The logic for LZO also exists in HDFS' params.py
io_compression_codecs = default("/configurations/core-site/io.compression.codecs", None)
lzo_enabled = io_compression_codecs is not None and "com.hadoop.compression.lzo" in io_compression_codecs.lower()

all_lzo_packages = get_lzo_packages(stack_version_unformatted)
