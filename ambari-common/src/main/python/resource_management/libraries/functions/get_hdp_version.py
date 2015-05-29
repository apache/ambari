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

Ambari Agent

"""
__all__ = ["get_hdp_version"]

import os
import re

from ambari_commons import OSConst
from ambari_commons.os_family_impl import OsFamilyFuncImpl, OsFamilyImpl

from resource_management.core.logger import Logger
from resource_management.core.exceptions import Fail
from resource_management.core import shell

@OsFamilyFuncImpl(OSConst.WINSRV_FAMILY)
def get_hdp_version(package_name):
  """
  @param package_name, name of the package, from which, function will try to get hdp version
  """
  try:
    component_home_dir = os.environ[package_name.upper() + "_HOME"]
  except KeyError:
    Logger.info('Skipping get_hdp_version since the component {0} is not yet available'.format(package_name))
    return None # lazy fail

  #As a rule, component_home_dir is of the form <hdp_root_dir>\[\]<component_versioned_subdir>[\]
  home_dir_split = os.path.split(component_home_dir)
  iSubdir = len(home_dir_split) - 1
  while not home_dir_split[iSubdir]:
    iSubdir -= 1

  #The component subdir is expected to be of the form <package_name>-<package_version>.<hdp_stack_version>
  # with package_version = #.#.# and hdp_stack_version=#.#.#.#-<build_number>
  match = re.findall('[0-9]+.[0-9]+.[0-9]+.[0-9]+-[0-9]+', home_dir_split[iSubdir])
  if not match:
    Logger.info('Failed to get extracted version for component {0}. Home dir not in expected format.'.format(package_name))
    return None # lazy fail

  return match[0]

@OsFamilyFuncImpl(OsFamilyImpl.DEFAULT)
def get_hdp_version(package_name):
  """
  @param package_name, name of the package, from which, function will try to get hdp version
  """
  
  if not os.path.exists("/usr/bin/hdp-select"):
    Logger.info('Skipping get_hdp_version since hdp-select is not yet available')
    return None # lazy fail
  
  try:
    command = 'hdp-select status ' + package_name
    return_code, hdp_output = shell.call(command, timeout=20)
  except Exception, e:
    Logger.error(str(e))
    raise Fail('Unable to execute hdp-select command to retrieve the version.')

  if return_code != 0:
    raise Fail(
      'Unable to determine the current version because of a non-zero return code of {0}'.format(str(return_code)))

  hdp_version = re.sub(package_name + ' - ', '', hdp_output)
  hdp_version = hdp_version.rstrip()
  match = re.match('[0-9]+.[0-9]+.[0-9]+.[0-9]+-[0-9]+', hdp_version)

  if match is None:
    Logger.info('Failed to get extracted version with hdp-select')
    return None # lazy fail

  return hdp_version
