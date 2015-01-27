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
from resource_management import *

config  = Script.get_config()
tmp_dir = Script.get_tmp_dir()

hdp_stack_version         = str(config['hostLevelParams']['stack_version'])
hdp_stack_version         = format_hdp_stack_version(hdp_stack_version)
stack_is_hdp22_or_further = hdp_stack_version != "" and compare_versions(hdp_stack_version, '2.2') >= 0

if stack_is_hdp22_or_further:
	ranger_home    = '/usr/hdp/current/ranger-admin'
	ranger_conf    = '/etc/ranger/admin/conf'
	ranger_stop    = '/usr/bin/ranger-admin-stop'
	ranger_start   = '/usr/bin/ranger-admin-start'
	usersync_home  = '/usr/hdp/current/ranger-usersync'
	usersync_start = '/usr/bin/ranger-usersync-start'
	usersync_stop  = '/usr/bin/ranger-usersync-stop'
else:
	pass

java_home = config['hostLevelParams']['java_home']
unix_user  = default("/configurations/ranger-env/ranger_user", "ranger")
unix_group = default("/configurations/ranger-env/ranger_group", "ranger")

ambari_server_hostname = config['clusterHostInfo']['ambari_server_host'][0]

jdk_location = config['hostLevelParams']['jdk_location']
java_share_dir = '/usr/share/java'
jdbc_jar_name = "mysql-connector-java.jar"

downloaded_custom_connector = format("{tmp_dir}/{jdbc_jar_name}")

driver_curl_source = format("{jdk_location}/{jdbc_jar_name}")
driver_curl_target = format("{java_share_dir}/{jdbc_jar_name}")