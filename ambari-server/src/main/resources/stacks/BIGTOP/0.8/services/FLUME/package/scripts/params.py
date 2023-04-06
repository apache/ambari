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

from resource_management import *

config = Script.get_config()

user_group = config['configurations']['cluster-env']['user_group']
proxyuser_group =  config['configurations']['hadoop-env']['proxyuser_group']

security_enabled = False

#RPM versioning support
rpm_version = default("/configurations/cluster-env/rpm_version", None)

#hadoop params
if rpm_version:
  flume_bin = '/usr/bigtop/current/flume-client/bin/flume-ng'
else:
  flume_bin = '/usr/bin/flume-ng'

flume_conf_dir = '/etc/flume/conf'
java_home = config['ambariLevelParams']['java_home']
flume_log_dir = '/var/log/flume'
flume_run_dir = '/var/run/flume'
flume_user = 'flume'
flume_group = 'flume'

if 'flume-env' in config['configurations'] and 'flume_user' in config['configurations']['flume-env']:
  flume_user = config['configurations']['flume-env']['flume_user']

if (('flume-conf' in config['configurations']) and('content' in config['configurations']['flume-conf'])):
  flume_conf_content = config['configurations']['flume-conf']['content']
else:
  flume_conf_content = None

if (('flume-log4j' in config['configurations']) and ('content' in config['configurations']['flume-log4j'])):
  flume_log4j_content = config['configurations']['flume-log4j']['content']
else:
  flume_log4j_content = None

targets = default('/commandParams/flume_handler', None)
flume_command_targets = [] if targets is None else targets.split(',')

flume_env_sh_template = config['configurations']['flume-env']['content']

ganglia_server_hosts = default('/clusterHostInfo/ganglia_server_host', [])
ganglia_server_host = None
if 0 != len(ganglia_server_hosts):
  ganglia_server_host = ganglia_server_hosts[0]

hostname = None
if 'hostname' in config:
  hostname = config['hostname']
