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
from resource_management.core.logger import Logger
from resource_management.core.exceptions import Fail
from resource_management.core import shell


def get_hdp_version(package_name):
  """
  @param package_name, name of the package, from which, function will try to get hdp version
  """
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
    raise Fail('Failed to get extracted version')

  return hdp_version
