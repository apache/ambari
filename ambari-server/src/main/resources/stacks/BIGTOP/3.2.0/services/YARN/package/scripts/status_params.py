#!/usr/bin/env python3
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
import os
import pwd

from ambari_commons import OSCheck

from resource_management.libraries.script.script import Script
from resource_management.libraries import functions
from resource_management.libraries.functions import conf_select
from resource_management.libraries.functions import stack_select
from resource_management.libraries.functions import format
from resource_management.libraries.functions import get_kinit_path
from resource_management.libraries.functions import stack_select
from resource_management.libraries.functions.default import default

config = Script.get_config()
tmp_dir = Script.get_tmp_dir()
root_user = 'root'

mapred_user = config['configurations']['mapred-env']['mapred_user']
yarn_user = config['configurations']['yarn-env']['yarn_user']
yarn_ats_user = config['configurations']['yarn-env']['yarn_ats_user']
yarn_pid_dir_prefix = config['configurations']['yarn-env']['yarn_pid_dir_prefix']
mapred_pid_dir_prefix = config['configurations']['mapred-env']['mapred_pid_dir_prefix']
yarn_pid_dir = format("{yarn_pid_dir_prefix}/{yarn_user}")
mapred_pid_dir = format("{mapred_pid_dir_prefix}/{mapred_user}")

resourcemanager_pid_file = format("{yarn_pid_dir}/hadoop-{yarn_user}-resourcemanager.pid")
nodemanager_pid_file = format("{yarn_pid_dir}/hadoop-{yarn_user}-nodemanager.pid")
yarn_historyserver_pid_file_old = format("{yarn_pid_dir}/hadoop-{yarn_user}-historyserver.pid")
yarn_historyserver_pid_file = format("{yarn_pid_dir}/hadoop-{yarn_user}-timelineserver.pid")  # *-historyserver.pid is deprecated

# registry dns service
registry_dns_bind_port = int(config['configurations']['yarn-env']['registry.dns.bind-port'])
registry_dns_needs_privileged_access = True if registry_dns_bind_port < 1024 else False

yarn_registry_dns_pid_file = format("{yarn_pid_dir_prefix}/{yarn_user}/hadoop-{yarn_user}-registrydns.pid")
yarn_registry_dns_priv_pid_file = format("{yarn_pid_dir_prefix}/{root_user}/hadoop-{yarn_user}-{root_user}-registrydns.pid")

if registry_dns_needs_privileged_access:
    yarn_registry_dns_in_use_pid_file = yarn_registry_dns_priv_pid_file
else:
    yarn_registry_dns_in_use_pid_file = yarn_registry_dns_pid_file

mapred_historyserver_pid_file = format("{mapred_pid_dir}/hadoop-{mapred_user}-historyserver.pid")

yarn_timelinereader_pid_file = format("{yarn_pid_dir}/hadoop-{yarn_user}-timelinereader.pid")

hadoop_home = stack_select.get_hadoop_dir("home")
hadoop_conf_dir = conf_select.get_hadoop_conf_dir()

hostname = config['agentLevelParams']['hostname']
kinit_path_local = functions.get_kinit_path(default('/configurations/kerberos-env/executable_search_paths', None))
security_enabled = config['configurations']['cluster-env']['security_enabled']

stack_name = default("/clusterLevelParams/stack_name", None)

# ATSv2 backend properties
yarn_hbase_user = format("{yarn_ats_user}") #Use yarn_ats_user.
yarn_hbase_pid_dir_prefix = config['configurations']['yarn-hbase-env']['yarn_hbase_pid_dir_prefix']
yarn_hbase_pid_dir = format("{yarn_hbase_pid_dir_prefix}/{yarn_hbase_user}")