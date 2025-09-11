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

from resource_management.libraries.functions import format
from resource_management.libraries.script.script import Script
from resource_management.libraries.functions.default import default
from resource_management.libraries.functions.get_kinit_path import get_kinit_path

config = Script.get_config()

# users
ozone_user = config['configurations']['ozone-env']['ozone_user']

# pid directory
ozone_pid_dir = config['configurations']['ozone-env']['ozone_pid_dir']

# log directory
ozone_log_dir = config['configurations']['ozone-env']['ozone_log_dir']

# conf directory
ozone_conf_dir = "/etc/ozone/conf"

# pid files
ozone_manager_pid_file = format("{ozone_pid_dir}/ozone-manager.pid")
ozone_datanode_pid_file = format("{ozone_pid_dir}/ozone-datanode.pid")
ozone_scm_pid_file = format("{ozone_pid_dir}/ozone-scm.pid")

# security
security_enabled = config['configurations']['cluster-env']['security_enabled']
kinit_path_local = get_kinit_path(default('/configurations/kerberos-env/executable_search_paths', None))
tmp_dir = Script.get_tmp_dir()
hostname = config['hostname']

if security_enabled:
  ozone_manager_keytab_path = config['configurations']['ozone-site']['ozone.om.kerberos.keytab.file']
  ozone_manager_kerberos_principal = config['configurations']['ozone-site']['ozone.om.kerberos.principal']
  ozone_datanode_keytab_path = config['configurations']['ozone-site']['ozone.datanode.kerberos.keytab.file']
  ozone_datanode_kerberos_principal = config['configurations']['ozone-site']['ozone.datanode.kerberos.principal']
