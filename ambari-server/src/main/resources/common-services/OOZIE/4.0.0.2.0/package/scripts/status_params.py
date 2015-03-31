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
from ambari_commons import OSCheck

config = Script.get_config()

if OSCheck.is_windows_family():
  # windows service mapping
  oozie_server_win_service_name = "oozieservice"
else:
  oozie_pid_dir = config['configurations']['oozie-env']['oozie_pid_dir']
  pid_file = format("{oozie_pid_dir}/oozie.pid")

  security_enabled = config['configurations']['cluster-env']['security_enabled']
  kinit_path_local = functions.get_kinit_path()
  conf_dir = "/etc/oozie/conf"
  tmp_dir = Script.get_tmp_dir()
  oozie_user = config['configurations']['oozie-env']['oozie_user']
  hostname = config["hostname"]
