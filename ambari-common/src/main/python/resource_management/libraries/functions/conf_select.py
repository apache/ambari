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

__all__ = ["get_hadoop_conf_dir"]

# Python Imports
import os

# Local Imports
from resource_management.libraries.script.script import Script
from resource_management.core.logger import Logger
from resource_management.libraries.functions import component_version
from resource_management.libraries.functions.default import default
from resource_management.core import sudo
from resource_management.libraries.functions.stack_features import check_stack_feature
from resource_management.libraries.functions import StackFeature
from resource_management.libraries.functions.decorator import deprecated

@deprecated(comment = "The conf-select tools are no longer used. In order to get the hadoop conf "
                      "directory, the mpack_manager_helper module should be used to get the conf "
                      "directory of HADOOP_CLIENTS for the component's service group.")
def get_hadoop_conf_dir():
  """
  Return the hadoop shared conf directory which should be used for the command's component. The
  directory including the component's version is tried first, but if that doesn't exist,
  this will fallback to using "current".
  """
  stack_root = Script.get_stack_root()
  stack_version = Script.get_stack_version()

  hadoop_conf_dir = os.path.join(os.path.sep, "etc", "hadoop", "conf")
  if check_stack_feature(StackFeature.CONFIG_VERSIONING, stack_version):
    # read the desired version from the component map and use that for building the hadoop home
    version = component_version.get_component_repository_version()
    if version is None:
      version = default("/commandParams/version", None)

    hadoop_conf_dir = os.path.join(stack_root, str(version), "hadoop", "conf")
    if version is None or sudo.path_isdir(hadoop_conf_dir) is False:
      hadoop_conf_dir = os.path.join(stack_root, "current", "hadoop-client", "conf")

    Logger.info("Using hadoop conf dir: {0}".format(hadoop_conf_dir))

  return hadoop_conf_dir

