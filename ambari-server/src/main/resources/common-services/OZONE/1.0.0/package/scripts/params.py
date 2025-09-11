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

from resource_management.libraries.script.script import Script
from resource_management.libraries.functions import conf_select, stack_select
from resource_management.libraries.functions.constants import Direction
from resource_management.libraries.functions.version import format_stack_version
from resource_management.libraries.functions.format import format
from resource_management.libraries.functions.default import default
from resource_management.libraries.functions.get_kinit_path import get_kinit_path
from resource_management.libraries.functions.get_not_managed_resources import get_not_managed_resources
from resource_management.libraries.functions.is_empty import is_empty
import status_params

# server configurations
config = Script.get_config()
tmp_dir = Script.get_tmp_dir()

# users and groups
ozone_user = config['configurations']['ozone-env']['ozone_user']
user_group = config['configurations']['cluster-env']['user_group']

# java
java_home = config['hostLevelParams']['java_home']
java64_home = config['hostLevelParams']['java_home']

# hadoop
hadoop_bin_dir = stack_select.get_hadoop_dir("bin")
hadoop_conf_dir = conf_select.get_hadoop_conf_dir()
ozone_bin_dir = "/usr/ozone/bin"
ozone_home = "/usr/ozone"

# service directories
ozone_conf_dir = "/etc/ozone/conf"
ozone_log_dir = config['configurations']['ozone-env']['ozone_log_dir']
ozone_pid_dir = status_params.ozone_pid_dir

# pid files
ozone_manager_pid_file = format("{ozone_pid_dir}/ozone-manager.pid")
ozone_datanode_pid_file = format("{ozone_pid_dir}/ozone-datanode.pid")
ozone_scm_pid_file = format("{ozone_pid_dir}/ozone-scm.pid")

# configurations
ozone_site = config['configurations']['ozone-site']
ozone_env = config['configurations']['ozone-env']

# ozone service urls
ozone_manager_address = ozone_site.get('ozone.om.address', 'localhost:9862')
ozone_manager_http_address = ozone_site.get('ozone.om.http-address', 'localhost:9874')
ozone_scm_address = ozone_site.get('ozone.scm.names', 'localhost:9860')

# hostnames
hostname = config['hostname']
ambari_server_hostname = config['clusterHostInfo']['ambari_server_host'][0]

# security
security_enabled = config['configurations']['cluster-env']['security_enabled']
kinit_path_local = get_kinit_path(default('/configurations/kerberos-env/executable_search_paths', None))

if security_enabled:
  ozone_manager_keytab_path = config['configurations']['ozone-site']['ozone.om.kerberos.keytab.file']
  ozone_manager_kerberos_principal = config['configurations']['ozone-site']['ozone.om.kerberos.principal']
  ozone_datanode_keytab_path = config['configurations']['ozone-site']['ozone.datanode.kerberos.keytab.file']
  ozone_datanode_kerberos_principal = config['configurations']['ozone-site']['ozone.datanode.kerberos.principal']

# decommission related
exclude_file_path = format("{ozone_conf_dir}/dfs.exclude")
include_file_path = format("{ozone_conf_dir}/dfs.include")

# cluster info
cluster_name = config['clusterName']

# host info
all_hosts = default("/clusterHostInfo/all_hosts", [])
slave_hosts = default("/clusterHostInfo/slave_hosts", [])

# metric collector
if 'metrics_collector_hosts' in config['clusterHostInfo']:
  metrics_collector_hosts = config['clusterHostInfo']['metrics_collector_hosts']
  metrics_collector_port = str(get_port_from_url(config['configurations']['ams-site']['timeline.metrics.service.webapp.address']))
  pass_file_path = format("{ozone_conf_dir}/.pass")
else:
  metrics_collector_hosts = []

# stack version
stack_version_unformatted = config['hostLevelParams']['stack_version']
stack_version_formatted = format_stack_version(stack_version_unformatted)

# not managed identities
not_managed_hdfs_path_list = get_not_managed_resources()

# ranger
ranger_plugin_enabled = False
if 'ranger-ozone-plugin-properties' in config['configurations'] and 'ranger-ozone-plugin-enabled' in config['configurations']['ranger-ozone-plugin-properties']:
  ranger_plugin_enabled = config['configurations']['ranger-ozone-plugin-properties']['ranger-ozone-plugin-enabled'].lower() == 'true'

# jvm heap size
ozone_manager_heapsize = ozone_env.get('ozone_manager_heapsize', '1024m')
ozone_datanode_heapsize = ozone_env.get('ozone_datanode_heapsize', '1024m')
ozone_scm_heapsize = ozone_env.get('ozone_scm_heapsize', '1024m')

# log settings
ozone_manager_log_maxfilesize = default('/configurations/ozone-log4j/ozone_manager_log_maxfilesize', '256')
ozone_manager_log_maxbackupindex = default('/configurations/ozone-log4j/ozone_manager_log_maxbackupindex', '20')
ozone_datanode_log_maxfilesize = default('/configurations/ozone-log4j/ozone_datanode_log_maxfilesize', '256')
ozone_datanode_log_maxbackupindex = default('/configurations/ozone-log4j/ozone_datanode_log_maxbackupindex', '20')

# helper function
def get_port_from_url(address):
  if not is_empty(address):
    return address.split(':')[-1]
  else:
    return address
