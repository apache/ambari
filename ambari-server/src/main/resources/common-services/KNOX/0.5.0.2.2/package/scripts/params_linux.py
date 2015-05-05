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
from resource_management.libraries.functions.version import format_hdp_stack_version
from resource_management.libraries.functions.default import default
from resource_management.libraries.script.script import Script

# server configurations
config = Script.get_config()

knox_master_secret_path = '/var/lib/knox/data/security/master'
knox_cert_store_path = '/var/lib/knox/data/security/keystores/gateway.jks'

knox_user = default("/configurations/knox-env/knox_user", "knox")
stack_version_unformatted = str(config['hostLevelParams']['stack_version'])
hdp_stack_version = format_hdp_stack_version(stack_version_unformatted)

# default parameters
knox_bin = '/usr/bin/gateway'
knox_conf_dir = '/etc/knox/conf'
ldap_bin = '/usr/lib/knox/bin/ldap.sh'
knox_client_bin = '/usr/lib/knox/bin/knoxcli.sh'

# HDP 2.2+ parameters
if Script.is_hdp_stack_greater_or_equal("2.2"):
  knox_bin = '/usr/hdp/current/knox-server/bin/gateway.sh'
  knox_conf_dir = '/usr/hdp/current/knox-server/conf'
  ldap_bin = '/usr/hdp/current/knox-server/bin/ldap.sh'
  knox_client_bin = '/usr/hdp/current/knox-server/bin/knoxcli.sh'

knox_group = default("/configurations/knox-env/knox_group", "knox")
mode = 0644

# server configurations
knox_data_dir = '/var/lib/knox/data'
knox_logs_dir = '/var/log/knox'
