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

from resource_management import *

# server configurations
config = Script.get_config()

#RPM versioning support
rpm_version = default("/configurations/cluster-env/rpm_version", None)

#hadoop params
if rpm_version:
#  slider_conf_dir = '/usr/lib/current/slider/conf'
#  slider_bin_dir = '/usr/lib/current/slider/bin'
  slider_conf_dir = "/usr/lib/slider/conf"
  slider_bin_dir = "/usr/lib/slider/bin"
else:
  slider_conf_dir = "/usr/lib/slider/conf"
  slider_bin_dir = "/usr/lib/slider/bin"

hadoop_conf_dir = "/etc/hadoop/conf"
smokeuser = config['configurations']['cluster-env']['smokeuser']
security_enabled = config['configurations']['cluster-env']['security_enabled']
smokeuser_keytab = config['configurations']['cluster-env']['smokeuser_keytab']
kinit_path_local = functions.get_kinit_path(["/usr/bin", "/usr/kerberos/bin", "/usr/sbin"])

java64_home = config['hostLevelParams']['java_home']
log4j_props = config['configurations']['slider-log4j']['content']
slider_cmd = format("{slider_bin_dir}/slider")