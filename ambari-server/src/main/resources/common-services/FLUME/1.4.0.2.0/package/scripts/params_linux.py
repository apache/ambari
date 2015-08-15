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
from resource_management.libraries.functions import default
from resource_management.libraries.script.script import Script

# server configurations
config = Script.get_config()

# upgrade params
stack_name = default("/hostLevelParams/stack_name", None)
upgrade_direction = default("/commandParams/upgrade_direction", Direction.UPGRADE)
stack_version_unformatted = str(config['hostLevelParams']['stack_version'])

flume_conf_dir = '/etc/flume/conf'
if Script.is_hdp_stack_greater_or_equal("2.2"):
  flume_conf_dir = '/usr/hdp/current/flume-server/conf'

flume_user = 'flume'
flume_group = 'flume'
if 'flume-env' in config['configurations'] and 'flume_user' in config['configurations']['flume-env']:
  flume_user = config['configurations']['flume-env']['flume_user']