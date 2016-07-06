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

__all__ = ["has_atlas_in_cluster", "setup_atlas_hook", "setup_atlas_jar_symlinks"]

# Python Imports
import os

# Local Imports
from resource_management.libraries.resources.properties_file import PropertiesFile
from resource_management.libraries.functions.format import format
from resource_management.libraries.functions.default import default
from resource_management.core.resources.system import Link
from resource_management.core.logger import Logger


def has_atlas_in_cluster():
  """
  Determine if Atlas is installed on the cluster.
  :return: True if Atlas is installed, otherwise false.
  """
  atlas_hosts = default('/clusterHostInfo/atlas_server_hosts', [])
  return len(atlas_hosts) > 0

def setup_atlas_hook(service_props, atlas_hook_filepath, owner, group):
  """
  Generate the atlas-application.properties.xml file by merging the service_props with the Atlas application-properties.
  :param service_props: Atlas configs specific to this service that must be merged.
  :param atlas_hook_filepath: Config file to write, e.g., /etc/falcon/conf/atlas-application.properties.xml
  :param owner: File owner
  :param group: File group
  """
  import params
  atlas_props = default('/configurations/application-properties', {})

  if has_atlas_in_cluster():
    merged_props = atlas_props.copy()
    merged_props.update(service_props)

    Logger.info(format("Generating Atlas Hook config file {atlas_hook_filepath}"))
    PropertiesFile(atlas_hook_filepath,
                   properties = merged_props,
                   owner = owner,
                   group = group,
                   mode = 0644)


def setup_atlas_jar_symlinks(hook_name, jar_source_dir):
  """
  If atlas is present on this host, then link the jars from
  {stack_root}/current/{hook_name}/lib/name_version.jar -> {jar_source_dir}/name_version.jar
  @param hook_name: one of sqoop, storm
  @param jar_source_dir: directory of where the symlinks need to be created from.
  """
  import params

  if has_atlas_in_cluster():
    atlas_home_dir = os.environ['METADATA_HOME_DIR'] if 'METADATA_HOME_DIR' in os.environ \
      else format("{stack_root}/current/atlas-server")

    # Will only exist if this host contains Atlas Server
    atlas_hook_dir = os.path.join(atlas_home_dir, "hook", hook_name)
    if os.path.exists(atlas_hook_dir):
      Logger.info("Atlas Server is present on this host, will symlink jars inside of %s to %s if not already done." %
                  (jar_source_dir, atlas_hook_dir))

      src_files = os.listdir(atlas_hook_dir)
      for file_name in src_files:
        atlas_hook_file_name = os.path.join(atlas_hook_dir, file_name)
        source_lib_file_name = os.path.join(jar_source_dir, file_name)
        if os.path.isfile(atlas_hook_file_name):
          Link(source_lib_file_name, to=atlas_hook_file_name)