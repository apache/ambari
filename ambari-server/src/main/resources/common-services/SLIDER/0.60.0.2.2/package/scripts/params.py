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

from resource_management.libraries.functions.version import format_hdp_stack_version, compare_versions
from resource_management.libraries.functions.default import default
from resource_management import *
from ambari_commons import OSCheck

if OSCheck.is_windows_family():
  from params_windows import *
else:
  from params_linux import *

# server configurations
config = Script.get_config()

stack_name = default("/hostLevelParams/stack_name", None)

# New Cluster Stack Version that is defined during the RESTART of a Rolling Upgrade
version = default("/commandParams/version", None)

stack_version_unformatted = str(config['hostLevelParams']['stack_version'])
hdp_stack_version = format_hdp_stack_version(stack_version_unformatted)

#hadoop params
if hdp_stack_version != "" and compare_versions(hdp_stack_version, '2.2') >= 0:
  slider_bin_dir = '/usr/hdp/current/slider-client/bin'
else:
  slider_bin_dir = "/usr/lib/slider/bin"

hadoop_conf_dir = "/etc/hadoop/conf"
smokeuser = config['configurations']['cluster-env']['smokeuser']
smokeuser_principal = config['configurations']['cluster-env']['smokeuser_principal_name']
security_enabled = config['configurations']['cluster-env']['security_enabled']
smokeuser_keytab = config['configurations']['cluster-env']['smokeuser_keytab']
kinit_path_local = functions.get_kinit_path()
slider_env_sh_template = config['configurations']['slider-env']['content']

java64_home = config['hostLevelParams']['java_home']
log4j_props = config['configurations']['slider-log4j']['content']
slider_cmd = format("{slider_bin_dir}/slider")
