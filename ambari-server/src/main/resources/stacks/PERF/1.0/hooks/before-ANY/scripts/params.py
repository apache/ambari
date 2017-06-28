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
import collections
import ambari_simplejson as json
from resource_management.libraries.script import Script
from resource_management.libraries.functions import default
from resource_management.libraries.functions.expect import expect
from ambari_commons.constants import AMBARI_SUDO_BINARY

config = Script.get_config()
tmp_dir = Script.get_tmp_dir()

group_list = json.loads(config['hostLevelParams']['group_list'])
user_list = json.loads(config['hostLevelParams']['user_list'])

user_group = config['configurations']['cluster-env']['user_group']
user_to_gid_dict = collections.defaultdict(lambda:user_group)
user_to_groups_dict = collections.defaultdict(lambda:[user_group])

jdk_name = default("/hostLevelParams/jdk_name", None)
java_home = config['hostLevelParams']['java_home']
artifact_dir = format("{tmp_dir}/AMBARI-artifacts/")
jdk_location = config['hostLevelParams']['jdk_location']
java_version = expect("/hostLevelParams/java_version", int)

ambari_java_home = default("/commandParams/ambari_java_home", None)
ambari_jdk_name = default("/commandParams/ambari_jdk_name", None)

service_name = config["serviceName"]
component_name = config["role"]
sudo = AMBARI_SUDO_BINARY