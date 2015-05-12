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

import sys
from resource_management.core.logger import Logger
from resource_management.core.resources.system import Execute

# hdp-select set oozie-server 2.2.0.0-1234
TEMPLATE = "hdp-select set {0} {1}"

def select(component, version):
  """
  Executes hdp-select on the specific component and version. Some global
  variables that are imported via params/status_params/params_linux will need
  to be recalcuated after the hdp-select. However, python does not re-import
  existing modules. The only way to ensure that the configuration variables are
  recalculated is to call reload(...) on each module that has global parameters.
  After invoking hdp-select, this function will also reload params, status_params,
  and params_linux.
  :param component: the hdp-select component, such as oozie-server
  :param version: the version to set the component to, such as 2.2.0.0-1234
  """
  command = TEMPLATE.format(component, version)
  Execute(command)

  # don't trust the ordering of modules:
  # 1) status_params
  # 2) params_linux
  # 3) params
  modules = sys.modules
  param_modules = "status_params", "params_linux", "params"
  for moduleName in param_modules:
    if moduleName in modules:
      module = modules.get(moduleName)
      reload(module)
      Logger.info("After hdp-select {0}, reloaded module {1}".format(component, moduleName))
