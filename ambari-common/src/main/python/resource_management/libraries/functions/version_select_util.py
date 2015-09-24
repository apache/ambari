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
import os
import re
import tempfile

from resource_management.core.logger import Logger
from resource_management.core import shell


def get_component_version(stack_name, component_name):
  """
  For any stack name, returns the version currently installed for a given component.
  Because each stack name may have different logic, the input is a generic dictionary.
  :param stack_name: one of HDP, HDPWIN, BIGTOP, PHD, etc. usually retrieved from
  the command-#.json file's ["hostLevelParams"]["stack_name"]
  :param component_name: Component name as a string necessary to get the version
  :return: Returns a string if found, e.g., 2.2.1.0-2175, otherwise, returns None
  """
  version = None
  if stack_name is None or component_name is None:
    Logger.error("Could not determine component version because of the parameters is empty. " \
                 "stack_name: %s, component_name: %s" % (str(stack_name), str(component_name)))
    return version

  out = None
  code = -1
  if stack_name == "HDP":
    tmpfile = tempfile.NamedTemporaryFile()

    get_hdp_comp_version_cmd = ""
    try:
      # This is necessary because Ubuntu returns "stdin: is not a tty", see AMBARI-8088
      with open(tmpfile.name, 'r') as file:
        get_hdp_comp_version_cmd = '/usr/bin/hdp-select status %s > %s' % (component_name, tmpfile.name)
        code, stdoutdata = shell.call(get_hdp_comp_version_cmd)
        out = file.read()

      if code != 0 or out is None:
        raise Exception("Code is nonzero or output is empty")

      Logger.debug("Command: %s\nOutput: %s" % (get_hdp_comp_version_cmd, str(out)))
      matches = re.findall(r"([\d\.]+\-\d+)", out)
      version = matches[0] if matches and len(matches) > 0 else None
    except Exception, e:
      Logger.error("Could not determine HDP version for component %s by calling '%s'. Return Code: %s, Output: %s." %
                   (component_name, get_hdp_comp_version_cmd, str(code), str(out)))
  elif stack_name == "HDPWIN":
    pass
  elif stack_name == "GlusterFS":
    pass
  elif stack_name == "PHD":
    pass
  elif stack_name == "BIGTOP":
    pass
  else:
    Logger.error("Could not find a stack for stack name: %s" % str(stack_name))

  return version


def get_versions_from_stack_root(stack_root):
  """
  Given a stack install root (/usr/hdp), returns a list of stack versions currently installed.
  The list of installed stack versions is determined purely based on the stack version directories
  found in the stack install root.
  Because each stack name may have different logic, the input is a generic dictionary.
  :param stack_root: Stack install root directory
  :return: Returns list of installed stack versions
  """
  if stack_root is None or not os.path.exists(stack_root):
    return []

  installed_stack_versions = [f for f in os.listdir(stack_root) if os.path.isdir(os.path.join(stack_root, f))
                              and re.match("([\d\.]+(-\d+)?)", f)]
  return installed_stack_versions
