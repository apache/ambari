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
import utils
import ambari_simplejson as json # simplejson is much faster comparing to Python 2.6 json module and has the same functions set.
import os
import re

from ambari_commons.os_check import OSCheck
from ambari_commons.str_utils import cbool, cint

from resource_management.libraries.functions import conf_select
from resource_management.libraries.functions import hdp_select
from resource_management.libraries.functions import format
from resource_management.libraries.functions.version import format_hdp_stack_version
from resource_management.libraries.functions.default import default
from resource_management.libraries.functions import get_klist_path
from resource_management.libraries.functions import get_kinit_path
from resource_management.libraries.functions.get_not_managed_resources import get_not_managed_resources
from resource_management.libraries.script.script import Script
from resource_management.libraries.resources.hdfs_resource import HdfsResource

from resource_management.libraries.functions.format_jvm_option import format_jvm_option
from resource_management.libraries.functions.get_lzo_packages import get_lzo_packages
from resource_management.libraries.functions.is_empty import is_empty


config = Script.get_config()
tmp_dir = Script.get_tmp_dir()

stack_name = default("/hostLevelParams/stack_name", None)
upgrade_direction = default("/commandParams/upgrade_direction", None)
stack_version_unformatted = str(config['hostLevelParams']['stack_version'])
hdp_stack_version = format_hdp_stack_version(stack_version_unformatted)
agent_stack_retry_on_unavailability = cbool(default("/hostLevelParams/agent_stack_retry_on_unavailability", None))
agent_stack_retry_count = cint(default("/hostLevelParams/agent_stack_retry_count", None))

# there is a stack upgrade which has not yet been finalized; it's currently suspended
upgrade_suspended = default("roleParams/upgrade_suspended", False)

# New Cluster Stack Version that is defined during the RESTART of a Stack Upgrade
version = default("/commandParams/version", None)

# The desired role is only available during a Non-Rolling Upgrade in HA.
# The server calculates which of the two NameNodes will be the active, and the other the standby since they
# are started using different commands.
desired_namenode_role = default("/commandParams/desired_namenode_role", None)


security_enabled = config['configurations']['cluster-env']['security_enabled']
hdfs_user = status_params.hdfs_user
root_user = "root"
hadoop_pid_dir_prefix = status_params.hadoop_pid_dir_prefix
namenode_pid_file = status_params.namenode_pid_file
zkfc_pid_file = status_params.zkfc_pid_file

# Some datanode settings
dfs_dn_addr = default('/configurations/hdfs-site/dfs.datanode.address', None)
dfs_dn_http_addr = default('/configurations/hdfs-site/dfs.datanode.http.address', None)
dfs_dn_https_addr = default('/configurations/hdfs-site/dfs.datanode.https.address', None)
dfs_http_policy = default('/configurations/hdfs-site/dfs.http.policy', None)
dfs_dn_ipc_address = config['configurations']['hdfs-site']['dfs.datanode.ipc.address']
secure_dn_ports_are_in_use = False

hdfs_tmp_dir = config['configurations']['hadoop-env']['hdfs_tmp_dir']

# hadoop default parameters
mapreduce_libs_path = "/usr/lib/hadoop-mapreduce/*"
hadoop_libexec_dir = hdp_select.get_hadoop_dir("libexec")
hadoop_bin = hdp_select.get_hadoop_dir("sbin")
hadoop_bin_dir = hdp_select.get_hadoop_dir("bin")
hadoop_home = hdp_select.get_hadoop_dir("home")
hadoop_secure_dn_user = hdfs_user
hadoop_conf_dir = conf_select.get_hadoop_conf_dir()
hadoop_conf_secure_dir = os.path.join(hadoop_conf_dir, "secure")
hadoop_lib_home = hdp_select.get_hadoop_dir("lib")

# hadoop parameters for 2.2+
if Script.is_hdp_stack_greater_or_equal("2.2"):
  mapreduce_libs_path = "/usr/hdp/current/hadoop-mapreduce-client/*"

  if not security_enabled:
    hadoop_secure_dn_user = '""'
  else:
    dfs_dn_port = utils.get_port(dfs_dn_addr)
    dfs_dn_http_port = utils.get_port(dfs_dn_http_addr)
    dfs_dn_https_port = utils.get_port(dfs_dn_https_addr)
    # We try to avoid inability to start datanode as a plain user due to usage of root-owned ports
    if dfs_http_policy == "HTTPS_ONLY":
      secure_dn_ports_are_in_use = utils.is_secure_port(dfs_dn_port) or utils.is_secure_port(dfs_dn_https_port)
    elif dfs_http_policy == "HTTP_AND_HTTPS":
      secure_dn_ports_are_in_use = utils.is_secure_port(dfs_dn_port) or utils.is_secure_port(dfs_dn_http_port) or utils.is_secure_port(dfs_dn_https_port)
    else:   # params.dfs_http_policy == "HTTP_ONLY" or not defined:
      secure_dn_ports_are_in_use = utils.is_secure_port(dfs_dn_port) or utils.is_secure_port(dfs_dn_http_port)
    if secure_dn_ports_are_in_use:
      hadoop_secure_dn_user = hdfs_user
    else:
      hadoop_secure_dn_user = '""'

ambari_libs_dir = "/var/lib/ambari-agent/lib"
limits_conf_dir = "/etc/security/limits.d"

hdfs_user_nofile_limit = default("/configurations/hadoop-env/hdfs_user_nofile_limit", "128000")
hdfs_user_nproc_limit = default("/configurations/hadoop-env/hdfs_user_nproc_limit", "65536")

create_lib_snappy_symlinks = not Script.is_hdp_stack_greater_or_equal("2.2")
jsvc_path = "/usr/lib/bigtop-utils"

execute_path = os.environ['PATH'] + os.pathsep + hadoop_bin_dir
ulimit_cmd = "ulimit -c unlimited ; "

snappy_so = "libsnappy.so"
so_target_dir_x86 = format("{hadoop_lib_home}/native/Linux-i386-32")
so_target_dir_x64 = format("{hadoop_lib_home}/native/Linux-amd64-64")
so_target_x86 = format("{so_target_dir_x86}/{snappy_so}")
so_target_x64 = format("{so_target_dir_x64}/{snappy_so}")
so_src_dir_x86 = format("{hadoop_home}/lib")
so_src_dir_x64 = format("{hadoop_home}/lib64")
so_src_x86 = format("{so_src_dir_x86}/{snappy_so}")
so_src_x64 = format("{so_src_dir_x64}/{snappy_so}")

#security params
smoke_user_keytab = config['configurations']['cluster-env']['smokeuser_keytab']
hdfs_user_keytab = config['configurations']['hadoop-env']['hdfs_user_keytab']
falcon_user = config['configurations']['falcon-env']['falcon_user']

#exclude file
hdfs_exclude_file = default("/clusterHostInfo/decom_dn_hosts", [])
exclude_file_path = config['configurations']['hdfs-site']['dfs.hosts.exclude']
update_exclude_file_only = default("/commandParams/update_exclude_file_only",False)
command_phase = default("/commandParams/phase","")

klist_path_local = get_klist_path(default('/configurations/kerberos-env/executable_search_paths', None))
kinit_path_local = get_kinit_path(default('/configurations/kerberos-env/executable_search_paths', None))
#hosts
hostname = config["hostname"]
rm_host = default("/clusterHostInfo/rm_host", [])
slave_hosts = default("/clusterHostInfo/slave_hosts", [])
oozie_servers = default("/clusterHostInfo/oozie_server", [])
hcat_server_hosts = default("/clusterHostInfo/webhcat_server_host", [])
hive_server_host =  default("/clusterHostInfo/hive_server_host", [])
hbase_master_hosts = default("/clusterHostInfo/hbase_master_hosts", [])
hs_host = default("/clusterHostInfo/hs_host", [])
jtnode_host = default("/clusterHostInfo/jtnode_host", [])
namenode_host = default("/clusterHostInfo/namenode_host", [])
nm_host = default("/clusterHostInfo/nm_host", [])
ganglia_server_hosts = default("/clusterHostInfo/ganglia_server_host", [])
journalnode_hosts = default("/clusterHostInfo/journalnode_hosts", [])
zkfc_hosts = default("/clusterHostInfo/zkfc_hosts", [])
falcon_host = default("/clusterHostInfo/falcon_server_hosts", [])

has_ganglia_server = not len(ganglia_server_hosts) == 0
has_namenodes = not len(namenode_host) == 0
has_jobtracker = not len(jtnode_host) == 0
has_resourcemanager = not len(rm_host) == 0
has_histroryserver = not len(hs_host) == 0
has_hbase_masters = not len(hbase_master_hosts) == 0
has_slaves = not len(slave_hosts) == 0
has_oozie_server = not len(oozie_servers)  == 0
has_hcat_server_host = not len(hcat_server_hosts)  == 0
has_hive_server_host = not len(hive_server_host)  == 0
has_journalnode_hosts = not len(journalnode_hosts)  == 0
has_zkfc_hosts = not len(zkfc_hosts)  == 0
has_falcon_host = not len(falcon_host)  == 0


is_namenode_master = hostname in namenode_host
is_jtnode_master = hostname in jtnode_host
is_rmnode_master = hostname in rm_host
is_hsnode_master = hostname in hs_host
is_hbase_master = hostname in hbase_master_hosts
is_slave = hostname in slave_hosts

if has_ganglia_server:
  ganglia_server_host = ganglia_server_hosts[0]

#users and groups
yarn_user = config['configurations']['yarn-env']['yarn_user']
hbase_user = config['configurations']['hbase-env']['hbase_user']
oozie_user = config['configurations']['oozie-env']['oozie_user']
webhcat_user = config['configurations']['hive-env']['hcat_user']
hcat_user = config['configurations']['hive-env']['hcat_user']
hive_user = config['configurations']['hive-env']['hive_user']
smoke_user =  config['configurations']['cluster-env']['smokeuser']
smokeuser_principal =  config['configurations']['cluster-env']['smokeuser_principal_name']
mapred_user = config['configurations']['mapred-env']['mapred_user']
hdfs_principal_name = default('/configurations/hadoop-env/hdfs_principal_name', None)

user_group = config['configurations']['cluster-env']['user_group']
root_group = "root"
proxyuser_group =  config['configurations']['hadoop-env']['proxyuser_group']

#hadoop params
hdfs_log_dir_prefix = config['configurations']['hadoop-env']['hdfs_log_dir_prefix']
hadoop_root_logger = config['configurations']['hadoop-env']['hadoop_root_logger']
nfs_file_dump_dir = config['configurations']['hdfs-site']['nfs.file.dump.dir']

dfs_domain_socket_path = config['configurations']['hdfs-site']['dfs.domain.socket.path']
dfs_domain_socket_dir = os.path.dirname(dfs_domain_socket_path)

jn_edits_dir = config['configurations']['hdfs-site']['dfs.journalnode.edits.dir']

dfs_name_dir = config['configurations']['hdfs-site']['dfs.namenode.name.dir']

namenode_dirs_created_stub_dir = format("{hdfs_log_dir_prefix}/{hdfs_user}")
namenode_dirs_stub_filename = "namenode_dirs_created"

smoke_hdfs_user_dir = format("/user/{smoke_user}")
smoke_hdfs_user_mode = 0770

hdfs_namenode_format_disabled = default("/configurations/cluster-env/hdfs_namenode_format_disabled", False)
hdfs_namenode_formatted_mark_suffix = "/namenode-formatted/"
namenode_formatted_old_mark_dirs = ["/var/run/hadoop/hdfs/namenode-formatted", 
  format("{hadoop_pid_dir_prefix}/hdfs/namenode/formatted"),
  "/var/lib/hdfs/namenode/formatted"]
dfs_name_dirs = dfs_name_dir.split(",")
namenode_formatted_mark_dirs = []
for dn_dir in dfs_name_dirs:
 tmp_mark_dir = format("{dn_dir}{hdfs_namenode_formatted_mark_suffix}")
 namenode_formatted_mark_dirs.append(tmp_mark_dir)

# Use the namenode RPC address if configured, otherwise, fallback to the default file system
namenode_address = None
if 'dfs.namenode.rpc-address' in config['configurations']['hdfs-site']:
  namenode_rpcaddress = config['configurations']['hdfs-site']['dfs.namenode.rpc-address']
  namenode_address = format("hdfs://{namenode_rpcaddress}")
else:
  namenode_address = config['configurations']['core-site']['fs.defaultFS']

fs_checkpoint_dirs = default("/configurations/hdfs-site/dfs.namenode.checkpoint.dir", "").split(',')

dfs_data_dir = config['configurations']['hdfs-site']['dfs.datanode.data.dir']
dfs_data_dir = ",".join([re.sub(r'^\[.+\]', '', dfs_dir.strip()) for dfs_dir in dfs_data_dir.split(",")])

data_dir_mount_file = "/var/lib/ambari-agent/data/datanode/dfs_data_dir_mount.hist"

# HDFS High Availability properties
dfs_ha_enabled = False
dfs_ha_nameservices = default("/configurations/hdfs-site/dfs.nameservices", None)
dfs_ha_namenode_ids = default(format("/configurations/hdfs-site/dfs.ha.namenodes.{dfs_ha_nameservices}"), None)
dfs_ha_automatic_failover_enabled = default("/configurations/hdfs-site/dfs.ha.automatic-failover.enabled", False)

# hostname of the active HDFS HA Namenode (only used when HA is enabled)
dfs_ha_namenode_active = default("/configurations/hadoop-env/dfs_ha_initial_namenode_active", None)
# hostname of the standby HDFS HA Namenode (only used when HA is enabled)
dfs_ha_namenode_standby = default("/configurations/hadoop-env/dfs_ha_initial_namenode_standby", None)

# Values for the current Host
namenode_id = None
namenode_rpc = None

dfs_ha_namemodes_ids_list = []
other_namenode_id = None

if dfs_ha_namenode_ids:
  dfs_ha_namemodes_ids_list = dfs_ha_namenode_ids.split(",")
  dfs_ha_namenode_ids_array_len = len(dfs_ha_namemodes_ids_list)
  if dfs_ha_namenode_ids_array_len > 1:
    dfs_ha_enabled = True
if dfs_ha_enabled:
  for nn_id in dfs_ha_namemodes_ids_list:
    nn_host = config['configurations']['hdfs-site'][format('dfs.namenode.rpc-address.{dfs_ha_nameservices}.{nn_id}')]
    if hostname in nn_host:
      namenode_id = nn_id
      namenode_rpc = nn_host
  # With HA enabled namenode_address is recomputed
  namenode_address = format('hdfs://{dfs_ha_nameservices}')

  # Calculate the namenode id of the other namenode. This is needed during RU to initiate an HA failover using ZKFC.
  if namenode_id is not None and len(dfs_ha_namemodes_ids_list) == 2:
    other_namenode_id = list(set(dfs_ha_namemodes_ids_list) - set([namenode_id]))[0]


if dfs_http_policy is not None and dfs_http_policy.upper() == "HTTPS_ONLY":
  https_only = True
  journalnode_address = default('/configurations/hdfs-site/dfs.journalnode.https-address', None)
else:
  https_only = False
  journalnode_address = default('/configurations/hdfs-site/dfs.journalnode.http-address', None)

if journalnode_address:
  journalnode_port = journalnode_address.split(":")[1]
  
  
if security_enabled:
  dn_principal_name = config['configurations']['hdfs-site']['dfs.datanode.kerberos.principal']
  dn_keytab = config['configurations']['hdfs-site']['dfs.datanode.keytab.file']
  dn_principal_name = dn_principal_name.replace('_HOST',hostname.lower())
  
  dn_kinit_cmd = format("{kinit_path_local} -kt {dn_keytab} {dn_principal_name};")
  
  nn_principal_name = config['configurations']['hdfs-site']['dfs.namenode.kerberos.principal']
  nn_keytab = config['configurations']['hdfs-site']['dfs.namenode.keytab.file']
  nn_principal_name = nn_principal_name.replace('_HOST',hostname.lower())
  
  nn_kinit_cmd = format("{kinit_path_local} -kt {nn_keytab} {nn_principal_name};")

  jn_principal_name = default("/configurations/hdfs-site/dfs.journalnode.kerberos.principal", None)
  if jn_principal_name:
    jn_principal_name = jn_principal_name.replace('_HOST', hostname.lower())
  jn_keytab = default("/configurations/hdfs-site/dfs.journalnode.keytab.file", None)
  hdfs_kinit_cmd = format("{kinit_path_local} -kt {hdfs_user_keytab} {hdfs_principal_name};")
else:
  dn_kinit_cmd = ""
  nn_kinit_cmd = ""
  hdfs_kinit_cmd = ""

hdfs_site = config['configurations']['hdfs-site']
default_fs = config['configurations']['core-site']['fs.defaultFS']

dfs_type = default("/commandParams/dfs_type", "")

import functools
#create partial functions with common arguments for every HdfsResource call
#to create/delete/copyfromlocal hdfs directories/files we need to call params.HdfsResource in code
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


# The logic for LZO also exists in OOZIE's params.py
io_compression_codecs = default("/configurations/core-site/io.compression.codecs", None)
lzo_enabled = io_compression_codecs is not None and "com.hadoop.compression.lzo" in io_compression_codecs.lower()
lzo_packages = get_lzo_packages(stack_version_unformatted)

exclude_packages = []
if not lzo_enabled:
  exclude_packages += lzo_packages
  
name_node_params = default("/commandParams/namenode", None)

java_home = config['hostLevelParams']['java_home']
java_version = int(config['hostLevelParams']['java_version'])

hadoop_heapsize = config['configurations']['hadoop-env']['hadoop_heapsize']
namenode_heapsize = config['configurations']['hadoop-env']['namenode_heapsize']
namenode_opt_newsize = config['configurations']['hadoop-env']['namenode_opt_newsize']
namenode_opt_maxnewsize = config['configurations']['hadoop-env']['namenode_opt_maxnewsize']
namenode_opt_permsize = format_jvm_option("/configurations/hadoop-env/namenode_opt_permsize","128m")
namenode_opt_maxpermsize = format_jvm_option("/configurations/hadoop-env/namenode_opt_maxpermsize","256m")

jtnode_opt_newsize = "200m"
jtnode_opt_maxnewsize = "200m"
jtnode_heapsize =  "1024m"
ttnode_heapsize = "1024m"

dtnode_heapsize = config['configurations']['hadoop-env']['dtnode_heapsize']
mapred_pid_dir_prefix = default("/configurations/mapred-env/mapred_pid_dir_prefix","/var/run/hadoop-mapreduce")
mapred_log_dir_prefix = default("/configurations/mapred-env/mapred_log_dir_prefix","/var/log/hadoop-mapreduce")

# ranger host
ranger_admin_hosts = default("/clusterHostInfo/ranger_admin_hosts", [])
has_ranger_admin = not len(ranger_admin_hosts) == 0
xml_configurations_supported = config['configurations']['ranger-env']['xml_configurations_supported']
ambari_server_hostname = config['clusterHostInfo']['ambari_server_host'][0]

#ranger hdfs properties
policymgr_mgr_url = config['configurations']['admin-properties']['policymgr_external_url']
sql_connector_jar = config['configurations']['admin-properties']['SQL_CONNECTOR_JAR']
xa_audit_db_name = config['configurations']['admin-properties']['audit_db_name']
xa_audit_db_user = config['configurations']['admin-properties']['audit_db_user']
xa_db_host = config['configurations']['admin-properties']['db_host']
repo_name = str(config['clusterName']) + '_hadoop'

hadoop_security_authentication = config['configurations']['core-site']['hadoop.security.authentication']
hadoop_security_authorization = config['configurations']['core-site']['hadoop.security.authorization']
fs_default_name = config['configurations']['core-site']['fs.defaultFS']
hadoop_security_auth_to_local = config['configurations']['core-site']['hadoop.security.auth_to_local']
hadoop_rpc_protection = config['configurations']['ranger-hdfs-plugin-properties']['hadoop.rpc.protection']
common_name_for_certificate = config['configurations']['ranger-hdfs-plugin-properties']['common.name.for.certificate']

repo_config_username = config['configurations']['ranger-hdfs-plugin-properties']['REPOSITORY_CONFIG_USERNAME']

if security_enabled:
  sn_principal_name = default("/configurations/hdfs-site/dfs.secondary.namenode.kerberos.principal", "nn/_HOST@EXAMPLE.COM")
  sn_principal_name = sn_principal_name.replace('_HOST',hostname.lower())

ranger_env = config['configurations']['ranger-env']
ranger_plugin_properties = config['configurations']['ranger-hdfs-plugin-properties']
policy_user = config['configurations']['ranger-hdfs-plugin-properties']['policy_user']

#For curl command in ranger plugin to get db connector
jdk_location = config['hostLevelParams']['jdk_location']
java_share_dir = '/usr/share/java'

is_https_enabled = config['configurations']['hdfs-site']['dfs.https.enable'] if \
  not is_empty(config['configurations']['hdfs-site']['dfs.https.enable']) else False

if has_ranger_admin:
  enable_ranger_hdfs = (config['configurations']['ranger-hdfs-plugin-properties']['ranger-hdfs-plugin-enabled'].lower() == 'yes')
  xa_audit_db_password = unicode(config['configurations']['admin-properties']['audit_db_password'])
  repo_config_password = unicode(config['configurations']['ranger-hdfs-plugin-properties']['REPOSITORY_CONFIG_PASSWORD'])
  xa_audit_db_flavor = (config['configurations']['admin-properties']['DB_FLAVOR']).lower()

  if xa_audit_db_flavor == 'mysql':
    jdbc_symlink_name = "mysql-jdbc-driver.jar"
    jdbc_jar_name = "mysql-connector-java.jar"
    audit_jdbc_url = format('jdbc:mysql://{xa_db_host}/{xa_audit_db_name}')
    jdbc_driver = "com.mysql.jdbc.Driver"
  elif xa_audit_db_flavor == 'oracle':
    jdbc_jar_name = "ojdbc6.jar"
    jdbc_symlink_name = "oracle-jdbc-driver.jar"
    colon_count = xa_db_host.count(':')
    if colon_count == 2 or colon_count == 0:
      audit_jdbc_url = format('jdbc:oracle:thin:@{xa_db_host}')
    else:
      audit_jdbc_url = format('jdbc:oracle:thin:@//{xa_db_host}')
    jdbc_driver = "oracle.jdbc.OracleDriver"
  elif xa_audit_db_flavor == 'postgres':
    jdbc_jar_name = "postgresql.jar"
    jdbc_symlink_name = "postgres-jdbc-driver.jar"
    audit_jdbc_url = format('jdbc:postgresql://{xa_db_host}/{xa_audit_db_name}')
    jdbc_driver = "org.postgresql.Driver"
  elif xa_audit_db_flavor == 'mssql':
    jdbc_jar_name = "sqljdbc4.jar"
    jdbc_symlink_name = "mssql-jdbc-driver.jar"
    audit_jdbc_url = format('jdbc:sqlserver://{xa_db_host};databaseName={xa_audit_db_name}')
    jdbc_driver = "com.microsoft.sqlserver.jdbc.SQLServerDriver"
  elif xa_audit_db_flavor == 'sqla':
    jdbc_jar_name = "sajdbc4.jar"
    jdbc_symlink_name = "sqlanywhere-jdbc-driver.tar.gz"
    audit_jdbc_url = format('jdbc:sqlanywhere:database={xa_audit_db_name};host={xa_db_host}')
    jdbc_driver = "sap.jdbc4.sqlanywhere.IDriver"

  downloaded_custom_connector = format("{tmp_dir}/{jdbc_jar_name}")
  driver_curl_source = format("{jdk_location}/{jdbc_symlink_name}")
  driver_curl_target = format("{hadoop_lib_home}/{jdbc_jar_name}")

  hdfs_ranger_plugin_config = {
    'username': repo_config_username,
    'password': repo_config_password,
    'hadoop.security.authentication': hadoop_security_authentication,
    'hadoop.security.authorization': hadoop_security_authorization,
    'fs.default.name': fs_default_name,
    'hadoop.security.auth_to_local': hadoop_security_auth_to_local,
    'hadoop.rpc.protection': hadoop_rpc_protection,
    'commonNameForCertificate': common_name_for_certificate,
    'dfs.datanode.kerberos.principal': dn_principal_name if security_enabled else '',
    'dfs.namenode.kerberos.principal': nn_principal_name if security_enabled else '',
    'dfs.secondary.namenode.kerberos.principal': sn_principal_name if security_enabled else ''
  }

  hdfs_ranger_plugin_repo = {
    'isActive': 'true',
    'config': json.dumps(hdfs_ranger_plugin_config),
    'description': 'hdfs repo',
    'name': repo_name,
    'repositoryType': 'hdfs',
    'assetType': '1'
  }
  
  ranger_audit_solr_urls = config['configurations']['ranger-admin-site']['ranger.audit.solr.urls']
  xa_audit_db_is_enabled = config['configurations']['ranger-hdfs-audit']['xasecure.audit.destination.db'] if xml_configurations_supported else None
  xa_audit_hdfs_is_enabled = config['configurations']['ranger-hdfs-audit']['xasecure.audit.destination.hdfs'] if xml_configurations_supported else None
  ssl_keystore_password = unicode(config['configurations']['ranger-hdfs-policymgr-ssl']['xasecure.policymgr.clientssl.keystore.password']) if xml_configurations_supported else None
  ssl_truststore_password = unicode(config['configurations']['ranger-hdfs-policymgr-ssl']['xasecure.policymgr.clientssl.truststore.password']) if xml_configurations_supported else None
  credential_file = format('/etc/ranger/{repo_name}/cred.jceks') if xml_configurations_supported else None

  #For SQLA explicitly disable audit to DB for Ranger
  if xa_audit_db_flavor == 'sqla':
    xa_audit_db_is_enabled = False
