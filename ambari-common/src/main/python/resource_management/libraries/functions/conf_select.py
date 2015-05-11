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

__all__ = ["select", "create", "get_hadoop_conf_dir", "get_hadoop_dir"]

import version
from resource_management.core import shell
from resource_management.core.exceptions import Fail
from resource_management.libraries.script.script import Script

TEMPLATE = "conf-select {0} --package {1} --stack-version {2} --conf-version 0"
HADOOP_DIR_TEMPLATE = "/usr/hdp/{0}/{1}/{2}"
HADOOP_DIR_DEFAULTS = {
  "libexec": "/usr/lib/hadoop/libexec",
  "sbin": "/usr/lib/hadoop/sbin",
  "bin": "/usr/bin",
  "lib": "/usr/lib/hadoop/lib"
}

def _valid(stack_name, package, ver):
  if stack_name != "HDP":
    return False

  if version.compare_versions(version.format_hdp_stack_version(ver), "2.3.0.0") < 0:
    return False

  return True

def _is_upgrade():
  from resource_management.libraries.functions.default import default
  direction = default("/commandParams/upgrade_direction", None)
  stack_name = default("/hostLevelParams/stack_name", None)
  ver = default("/commandParams/version", None)

  if direction and stack_name and ver:
    return (stack_name, ver)

  return None

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

  hadoop_conf_dir = "/etc/hadoop/conf"

  if Script.is_hdp_stack_greater_or_equal("2.2"):
    hadoop_conf_dir = "/usr/hdp/current/hadoop-client/conf"

    res = _is_upgrade()

    if res is not None and Script.is_hdp_stack_greater_or_equal("2.3"):
      select(res[0], "hadoop", res[1])
      hadoop_conf_dir = "/usr/hdp/{0}/hadoop/conf".format(res[1])

  return hadoop_conf_dir

def get_hadoop_dir(target):
  """
  Return the hadoop shared directory in the following override order
  1. Use default for 2.1 and lower
  2. If 2.2 and higher, use /usr/hdp/current/hadoop-client/{target}
  3. If 2.2 and higher AND for an upgrade, use /usr/hdp/<version>/hadoop/{target}
  :target: the target directory
  """

  if not target in HADOOP_DIR_DEFAULTS:
    raise Fail("Target {0} not defined".format(target))

  hadoop_dir = HADOOP_DIR_DEFAULTS[target]

  if Script.is_hdp_stack_greater_or_equal("2.2"):
    hadoop_dir = HADOOP_DIR_TEMPLATE.format("current", "hadoop-client", target)

    res = _is_upgrade()

    if res is not None:
      hadoop_dir = HADOOP_DIR_TEMPLATE.format(res[1], "hadoop", target)

  return hadoop_dir
    
