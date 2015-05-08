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

__all__ = ["select", "create"]

import version
from resource_management.core import shell
from resource_management.libraries.script.script import Script

TEMPLATE = "conf-select {0} --package {1} --stack-version {2} --conf-version 0"

def _valid(stack_name, package, ver):
  if stack_name != "HDP":
    return False

  if version.compare_versions(version.format_hdp_stack_version(ver), "2.3.0.0") < 0:
    return False

  return True

def create(stack_name, package, version):
  """
  Creates a config version for the specified package
  :stack_name: the name of the stack
  :package: the name of the package, as-used by conf-select
  :version: the version number to create
  """

  if not _valid(stack_name, package, version):
    return

  shell.call(TEMPLATE.format("create-conf-dir", package, version), logoutput=False, quiet=True)

def select(stack_name, package, version, try_create=True):
  """
  Selects a config version for the specified package.  Currently only works if the version is
  for HDP-2.3 or higher
  :stack_name: the name of the stack
  :package: the name of the package, as-used by conf-select
  :version: the version number to create
  :try_create: optional argument to attempt to create the directory before setting it
  """

  if not _valid(stack_name, package, version):
    return

  if try_create:
    create(stack_name, package, version)

  shell.call(TEMPLATE.format("set-conf-dir", package, version), logoutput=False, quiet=False)

def get_hadoop_conf_dir():
  """
  Gets the shared hadoop conf directory using:
  1.  Start with /etc/hadoop/conf
  2.  When the stack is greater than HDP-2.2, use /usr/hdp/current/hadoop-client/conf
  3.  Only when doing a RU and HDP-2.3 or higher, use the value as computed
      by conf-select.  This is in the form /usr/hdp/VERSION/hadoop/conf to make sure
      the configs are written in the correct place
  """

  config = Script.get_config()
  hadoop_conf_dir = "/etc/hadoop/conf"

  if Script.is_hdp_stack_greater_or_equal("2.2"):
    from resource_management.libraries.functions.default import default

    hadoop_conf_dir = "/usr/hdp/current/hadoop-client/conf"

    direction = default("/commandParams/upgrade_direction", None)
    ver = default("/commandParams/version", None)
    stack_name = default("/hostLevelParams/stack_name", None)

    if direction and ver and stack_name and Script.is_hdp_stack_greater_or_equal("2.3"):
      select(stack_name, "hadoop", ver)
      hadoop_conf_dir = "/usr/hdp/{0}/hadoop/conf".format(ver)

  return hadoop_conf_dir

    
