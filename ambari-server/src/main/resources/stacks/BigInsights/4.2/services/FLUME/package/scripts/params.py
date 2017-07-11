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
from resource_management.libraries.functions.constants import Direction
from resource_management.libraries.functions.default import default
from resource_management.libraries.functions import format
from resource_management.libraries.functions.version import format_stack_version
from resource_management.libraries.functions.default import default
from resource_management.libraries.script.script import Script

from params_linux import *

config = Script.get_config()

stack_name = default("/hostLevelParams/stack_name", None)
upgrade_direction = default("/commandParams/upgrade_direction", Direction.UPGRADE)
host_sys_prepped = default("/hostLevelParams/host_sys_prepped", False)

# New Cluster Stack Version that is defined during the RESTART of a Rolling Upgrade
version = default("/commandParams/version", None)

user_group = config['configurations']['cluster-env']['user_group']
proxyuser_group =  config['configurations']['hadoop-env']['proxyuser_group']

security_enabled = False

stack_version_unformatted = str(config['hostLevelParams']['stack_version'])
iop_stack_version = format_stack_version(stack_version_unformatted)

# hadoop default parameters
flume_bin = '/usr/iop/current/flume-server/bin/flume-ng'

# hadoop parameters for 2.2+
flume_hive_home = '/usr/iop/current/hive-metastore'
flume_hcat_home = '/usr/iop/current/hive-webhcat'

# flume agent runtime for management
flume_log_dir = config['configurations']['flume-env']['flume_log_dir']

# flume_run_dir is a new parameter added since Ambari 2.1 in IOP 4.1
# The default value must be the same as the value of "flume_run_dir" in params.py of Ambari 1.7 in IOP 4.0
# See ambari-server/src/main/resources/stacks/BigInsights/4.0/services/FLUME/package/scripts/params.py
FLUME_RUN_DIR_DEFAULT = '/var/run/flume'
if (('flume-env' in config['configurations']) and ('flume_run_dir' in config['configurations']['flume-env'])):
  flume_run_dir = config['configurations']['flume-env']['flume_run_dir']
else:
  flume_run_dir = FLUME_RUN_DIR_DEFAULT 

java_home = config['hostLevelParams']['java_home']
ambari_state_file = format("{flume_run_dir}/ambari-state.txt")
  
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
if config.has_key('hostname'):
  hostname = config['hostname']

ams_collector_hosts = default("/clusterHostInfo/metrics_collector_hosts", [])
has_metric_collector = not len(ams_collector_hosts) == 0
if has_metric_collector:
  metric_collector_host = ams_collector_hosts[0]
  metric_collector_port = default("/configurations/ams-site/timeline.metrics.service.webapp.address", "0.0.0.0:6188")
  if metric_collector_port and metric_collector_port.find(':') != -1:
    metric_collector_port = metric_collector_port.split(':')[1]
  pass
