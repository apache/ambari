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

from resource_management.libraries.resources.properties_file import PropertiesFile
from resource_management.core.resources.packaging import Package
from resource_management.core.resources.system import Link
from resource_management.libraries.functions.format import format
from ambari_commons import OSCheck

import os

def setup_atlas_sqoop():
  import params

  if params.has_atlas:

    if not params.host_sys_prepped:
      Package(params.atlas_ubuntu_plugin_package if OSCheck.is_ubuntu_family() else params.atlas_plugin_package,
              retry_on_repo_unavailability=params.agent_stack_retry_on_unavailability, retry_count=params.agent_stack_retry_count)

    PropertiesFile(format('{sqoop_conf_dir}/{atlas_conf_file}'),
                     properties = params.atlas_props,
                     owner = params.sqoop_user,
                     group = params.user_group,
                     mode = 0644)

    atlas_sqoop_hook_dir = os.path.join(params.atlas_home_dir, "hook", "sqoop")
    if os.path.exists(atlas_sqoop_hook_dir):

      src_files = os.listdir(atlas_sqoop_hook_dir)
      for file_name in src_files:
        atlas_sqoop_hook_file_name = os.path.join(atlas_sqoop_hook_dir, file_name)
        sqoop_lib_file_name = os.path.join(params.sqoop_lib, file_name)
        if (os.path.isfile(atlas_sqoop_hook_file_name)):
          Link(sqoop_lib_file_name, to = atlas_sqoop_hook_file_name)
