#!/usr/bin/env python2.6
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

from resource_management import *
import status_params

# server configurations
config = Script.get_config()

webhcat_user = config['configurations']['global']['webhcat_user']
download_url = config['configurations']['global']['apache_artifacts_download_url']

if config['hostLevelParams']['stack_version'] == '2.1.1':
  config_dir = '/etc/hive-webhcat/conf'
  webhcat_bin_dir = '/usr/lib/hive-hcatalog/sbin'
else:
  config_dir = '/etc/hcatalog/conf'
  webhcat_bin_dir = '/usr/lib/hcatalog/sbin'

templeton_log_dir = config['configurations']['global']['hcat_log_dir']
templeton_pid_dir = status_params.templeton_pid_dir

pid_file = status_params.pid_file

hadoop_conf_dir = config['configurations']['webhcat-site']['templeton.hadoop.conf.dir']
templeton_jar = config['configurations']['webhcat-site']['templeton.jar']

hadoop_home = '/usr'
user_group = config['configurations']['global']['user_group']

webhcat_server_host = config['clusterHostInfo']['webhcat_server_host']

webhcat_apps_dir = "/apps/webhcat"
smoke_user_keytab = config['configurations']['global']['smokeuser_keytab']
smokeuser = config['configurations']['global']['smokeuser']
security_enabled = config['configurations']['global']['security_enabled']
kinit_path_local = functions.get_kinit_path([default("kinit_path_local",None), "/usr/bin", "/usr/kerberos/bin", "/usr/sbin"])
