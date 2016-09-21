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
from ambari_commons.os_check import OSCheck
from resource_management.libraries.functions import format
from resource_management.libraries.functions import conf_select
from resource_management.libraries.functions.default import default
from resource_management.libraries.functions import get_kinit_path
from resource_management.libraries.script.script import Script
from resource_management.libraries.functions.copy_tarball import get_sysprep_skip_copy_tarballs_hdfs

if OSCheck.is_windows_family():
  from params_windows import *
else:
  from params_linux import *

# server configurations
config = Script.get_config()

stack_name = default("/hostLevelParams/stack_name", None)

# New Cluster Stack Version that is defined during the RESTART of a Stack Upgrade
version = default("/commandParams/version", None)
sysprep_skip_copy_tarballs_hdfs = get_sysprep_skip_copy_tarballs_hdfs()

#hadoop params
hadoop_conf_dir = conf_select.get_hadoop_conf_dir()

smokeuser = config['configurations']['cluster-env']['smokeuser']
smokeuser_principal = config['configurations']['cluster-env']['smokeuser_principal_name']
security_enabled = config['configurations']['cluster-env']['security_enabled']
smokeuser_keytab = config['configurations']['cluster-env']['smokeuser_keytab']
kinit_path_local = get_kinit_path(default('/configurations/kerberos-env/executable_search_paths', None))
slider_env_sh_template = config['configurations']['slider-env']['content']

java64_home = config['hostLevelParams']['java_home']
log4j_props = config['configurations']['slider-log4j']['content']
slider_cmd = format("{slider_bin_dir}/slider")
