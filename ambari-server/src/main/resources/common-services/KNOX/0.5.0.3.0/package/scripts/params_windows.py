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
import os
from status_params import *

# server configurations
config = Script.get_config()

stack_root = None
knox_home = None
knox_conf_dir = None
knox_logs_dir = None
knox_bin = None
ldap_bin = None
knox_client_bin = None
knox_data_dir = None

knox_master_secret_path = None
knox_cert_store_path = None

try:
  stack_root = os.path.abspath(os.path.join(os.environ["HADOOP_HOME"],".."))
  knox_home = os.environ['KNOX_HOME']
  knox_conf_dir = os.environ['KNOX_CONF_DIR']
  knox_logs_dir = os.environ['KNOX_LOG_DIR']
  knox_bin = os.path.join(knox_home, 'bin', 'gateway.exe')
  ldap_bin = os.path.join(knox_home, 'bin', 'ldap.exe')
  knox_client_bin = os.path.join(knox_home, 'bin', 'knoxcli.cmd')
  knox_data_dir = os.path.join(knox_home, 'data')

  knox_master_secret_path = os.path.join(knox_data_dir, 'security', 'master')
  knox_cert_store_path = os.path.join(knox_data_dir, 'security', 'keystores', 'gateway.jks')
except:
  pass

knox_host_port = config['configurations']['gateway-site']['gateway.port']
knox_host_name = config['clusterHostInfo']['knox_gateway_hosts'][0]
knox_host_name_in_cluster = config['agentLevelParams']['hostname']
knox_master_secret = config['configurations']['knox-env']['knox_master_secret']
topology_template = config['configurations']['topology']['content']
admin_topology_template = default('/configurations/admin-topology/content', None)
knoxsso_topology_template = config['configurations']['knoxsso-topology']['content']
gateway_log4j = config['configurations']['gateway-log4j']['content']
security_enabled = config['configurations']['cluster-env']['security_enabled']
ldap_log4j = config['configurations']['ldap-log4j']['content']
users_ldif = config['configurations']['users-ldif']['content']

hadoop_user = config["configurations"]["cluster-env"]["hadoop.user.name"]
knox_user = hadoop_user
hdfs_user = hadoop_user
knox_group = None
mode = None
