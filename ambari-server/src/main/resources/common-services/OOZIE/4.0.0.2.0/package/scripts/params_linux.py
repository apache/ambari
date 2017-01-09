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
from ambari_commons.constants import AMBARI_SUDO_BINARY
from resource_management.libraries.functions import format
from resource_management.libraries.functions import conf_select, stack_select
from resource_management.libraries.functions.constants import StackFeature
from resource_management.libraries.functions.stack_features import check_stack_feature
from resource_management.libraries.functions.default import default
from resource_management.libraries.functions import get_kinit_path
from resource_management.libraries.functions import get_port_from_url
from resource_management.libraries.functions.get_not_managed_resources import get_not_managed_resources
from resource_management.libraries.functions.setup_atlas_hook import has_atlas_in_cluster
from resource_management.libraries.script.script import Script
from resource_management.libraries.functions.get_lzo_packages import get_lzo_packages
from resource_management.libraries.functions.expect import expect
from resource_management.libraries.resources.hdfs_resource import HdfsResource
from resource_management.libraries.functions.get_architecture import get_architecture

from urlparse import urlparse

import status_params
import os
import re

# server configurations
config = Script.get_config()
tmp_dir = Script.get_tmp_dir()
sudo = AMBARI_SUDO_BINARY

architecture = get_architecture()


# Needed since this writes out the Atlas Hive Hook config file.
cluster_name = config['clusterName']

hostname = config["hostname"]

# New Cluster Stack Version that is defined during the RESTART of a Rolling Upgrade
version = default("/commandParams/version", None)
stack_name = status_params.stack_name
stack_name_uppercase = stack_name.upper()
upgrade_direction = default("/commandParams/upgrade_direction", None)
agent_stack_retry_on_unavailability = config['hostLevelParams']['agent_stack_retry_on_unavailability']
agent_stack_retry_count = expect("/hostLevelParams/agent_stack_retry_count", int)

stack_root = status_params.stack_root
stack_version_unformatted =  status_params.stack_version_unformatted
stack_version_formatted =  status_params.stack_version_formatted

hadoop_conf_dir = conf_select.get_hadoop_conf_dir()
hadoop_bin_dir = stack_select.get_hadoop_dir("bin")
hadoop_lib_home = stack_select.get_hadoop_dir("lib")

#hadoop params
if stack_version_formatted and check_stack_feature(StackFeature.ROLLING_UPGRADE,stack_version_formatted):
  stack_version = None
  upgrade_stack = stack_select._get_upgrade_stack()
  if upgrade_stack is not None and len(upgrade_stack) == 2 and upgrade_stack[1] is not None:
    stack_version = upgrade_stack[1]

  # oozie-server or oozie-client, depending on role
  oozie_root = status_params.component_directory

  # using the correct oozie root dir, format the correct location
  oozie_lib_dir = format("{stack_root}/current/{oozie_root}")
  oozie_setup_sh = format("{stack_root}/current/{oozie_root}/bin/oozie-setup.sh")
  oozie_webapps_dir = format("{stack_root}/current/{oozie_root}/oozie-server/webapps")
  oozie_webapps_conf_dir = format("{stack_root}/current/{oozie_root}/oozie-server/conf")
  oozie_libext_dir = format("{stack_root}/current/{oozie_root}/libext")
  oozie_server_dir = format("{stack_root}/current/{oozie_root}/oozie-server")
  oozie_shared_lib = format("{stack_root}/current/{oozie_root}/share")
  oozie_home = format("{stack_root}/current/{oozie_root}")
  oozie_bin_dir = format("{stack_root}/current/{oozie_root}/bin")
  oozie_examples_regex = format("{stack_root}/current/{oozie_root}/doc")

  # set the falcon home for copying JARs; if in an upgrade, then use the version of falcon that
  # matches the version of oozie
  falcon_home = format("{stack_root}/current/falcon-client")
  if stack_version is not None:
    falcon_home = '{0}/{1}/falcon'.format(stack_root, stack_version)

  conf_dir = format("{stack_root}/current/{oozie_root}/conf")
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
smoke_hdfs_user_mode = 0770
service_check_queue_name = default('/configurations/yarn-env/service_check.queue.name', 'default')

# This config actually contains {oozie_user}
oozie_admin_users = format(config['configurations']['oozie-env']['oozie_admin_users'])

user_group = config['configurations']['cluster-env']['user_group']
jdk_location = config['hostLevelParams']['jdk_location']
check_db_connection_jar_name = "DBConnectionVerification.jar"
check_db_connection_jar = format("/usr/lib/ambari-agent/{check_db_connection_jar_name}")
oozie_tmp_dir = default("configurations/oozie-env/oozie_tmp_dir", "/var/tmp/oozie")
oozie_hdfs_user_dir = format("/user/{oozie_user}")
oozie_pid_dir = status_params.oozie_pid_dir
pid_file = status_params.pid_file
hadoop_jar_location = "/usr/lib/hadoop/"
java_share_dir = "/usr/share/java"
ext_js_file = "ext-2.2.zip"
ext_js_path = format("/usr/share/{stack_name_uppercase}-oozie/{ext_js_file}")
security_enabled = config['configurations']['cluster-env']['security_enabled']
oozie_heapsize = config['configurations']['oozie-env']['oozie_heapsize']
oozie_permsize = config['configurations']['oozie-env']['oozie_permsize']

limits_conf_dir = "/etc/security/limits.d"

oozie_user_nofile_limit = default('/configurations/oozie-env/oozie_user_nofile_limit', 32000)
oozie_user_nproc_limit = default('/configurations/oozie-env/oozie_user_nproc_limit', 16000)

kinit_path_local = get_kinit_path(default('/configurations/kerberos-env/executable_search_paths', None))
oozie_service_keytab = config['configurations']['oozie-site']['oozie.service.HadoopAccessorService.keytab.file']
oozie_principal = config['configurations']['oozie-site']['oozie.service.HadoopAccessorService.kerberos.principal']
http_principal = config['configurations']['oozie-site']['oozie.authentication.kerberos.principal']
oozie_site = config['configurations']['oozie-site']
# Need this for yarn.nodemanager.recovery.dir in yarn-site
yarn_log_dir_prefix = config['configurations']['yarn-env']['yarn_log_dir_prefix']
yarn_resourcemanager_address = config['configurations']['yarn-site']['yarn.resourcemanager.address']

if security_enabled:
  oozie_site = dict(config['configurations']['oozie-site'])

  # If a user-supplied oozie.ha.authentication.kerberos.principal property exists in oozie-site,
  # use it to replace the existing oozie.authentication.kerberos.principal value. This is to ensure
  # that any special principal name needed for HA is used rather than the Ambari-generated value
  if "oozie.ha.authentication.kerberos.principal" in oozie_site:
    oozie_site['oozie.authentication.kerberos.principal'] = oozie_site['oozie.ha.authentication.kerberos.principal']
    http_principal = oozie_site['oozie.authentication.kerberos.principal']

  # If a user-supplied oozie.ha.authentication.kerberos.keytab property exists in oozie-site,
  # use it to replace the existing oozie.authentication.kerberos.keytab value. This is to ensure
  # that any special keytab file needed for HA is used rather than the Ambari-generated value
  if "oozie.ha.authentication.kerberos.keytab" in oozie_site:
    oozie_site['oozie.authentication.kerberos.keytab'] = oozie_site['oozie.ha.authentication.kerberos.keytab']

  if stack_version_formatted and check_stack_feature(StackFeature.OOZIE_HOST_KERBEROS, stack_version_formatted):
    #older versions of oozie have problems when using _HOST in principal
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
if 'export OOZIE_HTTPS_PORT' in oozie_env_sh_template or 'oozie.https.port' in config['configurations']['oozie-site'] or 'oozie.https.keystore.file' in config['configurations']['oozie-site'] or 'oozie.https.keystore.pass' in config['configurations']['oozie-site']:
  oozie_secure = '-secure'
else:
  oozie_secure = ''

https_port = None
# try to get https port form oozie-env content
for line in oozie_env_sh_template.splitlines():
  result = re.match(r"export\s+OOZIE_HTTPS_PORT=(\d+)", line)
  if result is not None:
    https_port = result.group(1)
# or from oozie-site.xml
if https_port is None and 'oozie.https.port' in config['configurations']['oozie-site']:
  https_port = config['configurations']['oozie-site']['oozie.https.port']

oozie_base_url = config['configurations']['oozie-site']['oozie.base.url']

service_check_job_name = default("/configurations/oozie-env/service_check_job_name", "no-op")

# construct proper url for https
if https_port is not None:
  parsed_url = urlparse(oozie_base_url)
  oozie_base_url = oozie_base_url.replace(parsed_url.scheme, "https")
  if parsed_url.port is None:
    oozie_base_url.replace(parsed_url.hostname, ":".join([parsed_url.hostname, str(https_port)]))
  else:
    oozie_base_url = oozie_base_url.replace(str(parsed_url.port), str(https_port))

oozie_setup_sh_current = oozie_setup_sh

hdfs_site = config['configurations']['hdfs-site']
fs_root = config['configurations']['core-site']['fs.defaultFS']

if stack_version_formatted and check_stack_feature(StackFeature.OOZIE_SETUP_SHARED_LIB, stack_version_formatted):
  put_shared_lib_to_hdfs_cmd = format("{oozie_setup_sh} sharelib create -fs {fs_root} -locallib {oozie_shared_lib}")
  # for older  
else: 
  put_shared_lib_to_hdfs_cmd = format("hadoop --config {hadoop_conf_dir} dfs -put {oozie_shared_lib} {oozie_hdfs_user_dir}")

default_connectors_map = { "com.microsoft.sqlserver.jdbc.SQLServerDriver":"sqljdbc4.jar",
                           "com.mysql.jdbc.Driver":"mysql-connector-java.jar",
                           "org.postgresql.Driver":"postgresql-jdbc.jar",
                           "oracle.jdbc.driver.OracleDriver":"ojdbc.jar",
                           "sap.jdbc4.sqlanywhere.IDriver":"sajdbc4.jar"}

jdbc_driver_name = default("/configurations/oozie-site/oozie.service.JPAService.jdbc.driver", "")
# NOT SURE THAT IT'S A GOOD IDEA TO USE PATH TO CLASS IN DRIVER, MAYBE IT WILL BE BETTER TO USE DB TYPE.
# BECAUSE PATH TO CLASSES COULD BE CHANGED
sqla_db_used = False
previous_jdbc_jar_name = None
if jdbc_driver_name == "com.microsoft.sqlserver.jdbc.SQLServerDriver":
  jdbc_driver_jar = default("/hostLevelParams/custom_mssql_jdbc_name", None)
  previous_jdbc_jar_name = default("/hostLevelParams/previous_custom_mssql_jdbc_name", None)
elif jdbc_driver_name == "com.mysql.jdbc.Driver":
  jdbc_driver_jar = default("/hostLevelParams/custom_mysql_jdbc_name", None)
  previous_jdbc_jar_name = default("/hostLevelParams/previous_custom_mysql_jdbc_name", None)
elif jdbc_driver_name == "org.postgresql.Driver":
  jdbc_driver_jar = format("{oozie_home}/libserver/postgresql-9.0-801.jdbc4.jar")  #oozie using it's own postgres jdbc
  previous_jdbc_jar_name = None
elif jdbc_driver_name == "oracle.jdbc.driver.OracleDriver":
  jdbc_driver_jar = default("/hostLevelParams/custom_oracle_jdbc_name", None)
  previous_jdbc_jar_name = default("/hostLevelParams/previous_custom_oracle_jdbc_name", None)
elif jdbc_driver_name == "sap.jdbc4.sqlanywhere.IDriver":
  jdbc_driver_jar = default("/hostLevelParams/custom_sqlanywhere_jdbc_name", None)
  previous_jdbc_jar_name = default("/hostLevelParams/previous_custom_sqlanywhere_jdbc_name", None)
  sqla_db_used = True
else:
  jdbc_driver_jar = ""
  jdbc_symlink_name = ""
  previous_jdbc_jar_name = None

default("/hostLevelParams/custom_sqlanywhere_jdbc_name", None)
driver_curl_source = format("{jdk_location}/{jdbc_driver_jar}")
downloaded_custom_connector = format("{tmp_dir}/{jdbc_driver_jar}")
if jdbc_driver_name == "org.postgresql.Driver":
  target = jdbc_driver_jar
  previous_jdbc_jar = None
else:
  target = format("{oozie_libext_dir}/{jdbc_driver_jar}")
  previous_jdbc_jar = format("{oozie_libext_dir}/{previous_jdbc_jar_name}")

#constants for type2 jdbc
jdbc_libs_dir = format("{oozie_libext_dir}/native/lib64")
lib_dir_available = os.path.exists(jdbc_libs_dir)

if sqla_db_used:
  jars_path_in_archive = format("{tmp_dir}/sqla-client-jdbc/java/*")
  libs_path_in_archive = format("{tmp_dir}/sqla-client-jdbc/native/lib64/*")
  downloaded_custom_connector = format("{tmp_dir}/{jdbc_driver_jar}")

hdfs_share_dir = format("{oozie_hdfs_user_dir}/share")
ambari_server_hostname = config['clusterHostInfo']['ambari_server_host'][0]
falcon_host = default("/clusterHostInfo/falcon_server_hosts", [])
has_falcon_host = not len(falcon_host)  == 0

oozie_server_hostnames = default("/clusterHostInfo/oozie_server", [])
oozie_server_hostnames = sorted(oozie_server_hostnames)

oozie_log_maxhistory = default('configurations/oozie-log4j/oozie_log_maxhistory',720)

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

dfs_type = default("/commandParams/dfs_type", "")


########################################################
############# Atlas related params #####################
########################################################
#region Atlas Hooks needed by Hive on Oozie
hive_atlas_application_properties = default('/configurations/hive-atlas-application.properties', {})

if has_atlas_in_cluster():
  atlas_hook_filename = default('/configurations/atlas-env/metadata_conf_file', 'atlas-application.properties')
#endregion

import functools
#create partial functions with common arguments for every HdfsResource call
#to create/delete hdfs directory/file/copyfromlocal we need to call params.HdfsResource in code
HdfsResource = functools.partial(
  HdfsResource,
  user=hdfs_user,
  hdfs_resource_ignore_file = "/var/lib/ambari-agent/data/.hdfs_resource_ignore",
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

is_webhdfs_enabled = config['configurations']['hdfs-site']['dfs.webhdfs.enabled']

# The logic for LZO also exists in HDFS' params.py
io_compression_codecs = default("/configurations/core-site/io.compression.codecs", None)
lzo_enabled = io_compression_codecs is not None and "com.hadoop.compression.lzo" in io_compression_codecs.lower()

all_lzo_packages = get_lzo_packages(stack_version_unformatted)
