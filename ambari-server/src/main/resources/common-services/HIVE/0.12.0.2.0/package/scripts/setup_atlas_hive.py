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
from resource_management.libraries.functions.format import format
from ambari_commons import OSCheck

def setup_atlas_hive(configuration_directory=None):
  import params

  if params.has_atlas:
    if configuration_directory is None:
      configuration_directory = format("{hive_config_dir}")

    if not params.host_sys_prepped:
      Package(params.atlas_ubuntu_plugin_package if OSCheck.is_ubuntu_family() else params.atlas_plugin_package, # FIXME HACK: install the package during RESTART/START when install_packages is not triggered.
              retry_on_repo_unavailability=params.agent_stack_retry_on_unavailability, retry_count=params.agent_stack_retry_count)

    PropertiesFile(format('{configuration_directory}/client.properties'),
                   properties = params.atlas_client_props,
                   owner = params.hive_user,
                   group = params.user_group,
                   mode = 0644)
