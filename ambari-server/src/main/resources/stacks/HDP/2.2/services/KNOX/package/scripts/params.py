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

from resource_management.libraries.functions.version import format_hdp_stack_version, compare_versions
from resource_management import *
import status_params

config = Script.get_config()
tmp_dir = Script.get_tmp_dir()

hdp_stack_version = str(config['hostLevelParams']['stack_version'])
hdp_stack_version = format_hdp_stack_version(hdp_stack_version)
stack_is_hdp22_or_further = hdp_stack_version != "" and compare_versions(hdp_stack_version, '2.2') >= 0

if stack_is_hdp22_or_further:
  knox_bin = '/usr/hdp/current/knox-server/bin/gateway.sh'
  ldap_bin = '/usr/hdp/current/knox-server/bin/ldap.sh'
  knox_client_bin = '/usr/hdp/current/knox-server/bin/knoxcli.sh'
else:
  knox_bin = '/usr/bin/gateway'
  ldap_bin = '/usr/lib/knox/bin/ldap.sh'
  knox_client_bin = '/usr/lib/knox/bin/knoxcli.sh'

namenode_hosts = default("/clusterHostInfo/namenode_host", None)
if type(namenode_hosts) is list:
    namenode_host = namenode_hosts[0]
else:
    namenode_host = namenode_hosts

has_namenode = not namenode_host == None
namenode_http_port = "50070"
namenode_rpc_port = "8020"

if has_namenode:
    if 'dfs.namenode.http-address' in config['configurations']['hdfs-site']:
        namenode_http_port = get_port_from_url(config['configurations']['hdfs-site']['dfs.namenode.http-address'])
    if 'dfs.namenode.rpc-address' in config['configurations']['hdfs-site']:
        namenode_rpc_port = get_port_from_url(config['configurations']['hdfs-site']['dfs.namenode.rpc-address'])

rm_hosts = default("/clusterHostInfo/rm_host", None)
if type(rm_hosts) is list:
    rm_host = rm_hosts[0]
else:
    rm_host = rm_hosts
has_rm = not rm_host == None

jt_rpc_port = "8050"
rm_port = "8080"

if has_rm:
    if 'yarn.resourcemanager.address' in config['configurations']['yarn-site']:
        jt_rpc_port = get_port_from_url(config['configurations']['yarn-site']['yarn.resourcemanager.address'])

    if 'yarn.resourcemanager.webapp.address' in config['configurations']['yarn-site']:
        rm_port = get_port_from_url(config['configurations']['yarn-site']['yarn.resourcemanager.webapp.address'])

hive_http_port = default('/configurations/hive-site/hive.server2.thrift.http.port', "10001")
hive_http_path = default('/configurations/hive-site/hive.server2.thrift.http.path', "cliservice")
hive_server_hosts = default("/clusterHostInfo/hive_server_host", None)
if type(hive_server_hosts) is list:
    hive_server_host = hive_server_hosts[0]
else:
    hive_server_host = hive_server_hosts

templeton_port = default('/configurations/webhcat-site/templeton.port', "50111")
webhcat_server_hosts = default("/clusterHostInfo/webhcat_server_host", None)
if type(webhcat_server_hosts) is list:
    webhcat_server_host = webhcat_server_hosts[0]
else:
    webhcat_server_host = webhcat_server_hosts

hbase_master_port = default('/configurations/hbase-site/hbase.rest.port', "8080")
hbase_master_hosts = default("/clusterHostInfo/hbase_master_hosts", None)
if type(hbase_master_hosts) is list:
    hbase_master_host = hbase_master_hosts[0]
else:
    hbase_master_host = hbase_master_hosts

oozie_server_hosts = default("/clusterHostInfo/oozie_server", None)
if type(oozie_server_hosts) is list:
    oozie_server_host = oozie_server_hosts[0]
else:
    oozie_server_host = oozie_server_hosts

has_oozie = not oozie_server_host == None
oozie_server_port = "11000"

if has_oozie:
    if 'oozie.base.url' in config['configurations']['oozie-site']:
        oozie_server_port = get_port_from_url(config['configurations']['oozie-site']['oozie.base.url'])


# server configurations
knox_conf_dir = '/etc/knox/conf'
knox_data_dir = '/var/lib/knox/data'
knox_logs_dir = '/var/log/knox'
knox_pid_dir = status_params.knox_pid_dir
knox_user = default("/configurations/knox-env/knox_user", "knox")
knox_group = default("/configurations/knox-env/knox_group", "knox")
knox_pid_file = status_params.knox_pid_file
ldap_pid_file = status_params.ldap_pid_file
knox_master_secret = config['configurations']['knox-env']['knox_master_secret']
knox_master_secret_path = '/var/lib/knox/data/security/master'
knox_cert_store_path = '/var/lib/knox/data/security/keystores/gateway.jks'
knox_host_name = config['clusterHostInfo']['knox_gateway_hosts'][0]
knox_host_name_in_cluster = config['hostname']
knox_host_port = config['configurations']['gateway-site']['gateway.port']
topology_template = config['configurations']['topology']['content']
gateway_log4j = config['configurations']['gateway-log4j']['content']
ldap_log4j = config['configurations']['ldap-log4j']['content']
users_ldif = config['configurations']['users-ldif']['content']
java_home = config['hostLevelParams']['java_home']
security_enabled = config['configurations']['cluster-env']['security_enabled']
smokeuser = config['configurations']['cluster-env']['smokeuser']
smoke_user_keytab = config['configurations']['cluster-env']['smokeuser_keytab']
kinit_path_local = functions.get_kinit_path(["/usr/bin", "/usr/kerberos/bin", "/usr/sbin"])
if security_enabled:
  knox_keytab_path = config['configurations']['knox-env']['knox_keytab_path']
  _hostname_lowercase = config['hostname'].lower()
  knox_principal_name = config['configurations']['knox-env']['knox_principal_name'].replace('_HOST',_hostname_lowercase)


