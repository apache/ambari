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
import hdp_select

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


def get_hadoop_conf_dir(force_latest_on_upgrade=False):
  """
  Gets the shared hadoop conf directory using:
  1.  Start with /etc/hadoop/conf
  2.  When the stack is greater than HDP-2.2, use /usr/hdp/current/hadoop-client/conf
  3.  Only when doing a RU and HDP-2.3 or higher, use the value as computed
      by conf-select.  This is in the form /usr/hdp/VERSION/hadoop/conf to make sure
      the configs are written in the correct place. However, if the component itself has
      not yet been upgraded, it should use the hadoop configs from the prior version.
      This will perform an hdp-select status to determine which version to use.
  :param force_latest_on_upgrade:  if True, then force the returned path to always
  be that of the upgrade target version, even if hdp-select has not been called. This
  is primarily used by hooks like before-ANY to ensure that hadoop environment
  configurations are written to the correct location since they are written out
  before the hdp-select/conf-select would have been called.
  """
  hadoop_conf_dir = "/etc/hadoop/conf"

  if Script.is_hdp_stack_greater_or_equal("2.2"):
    hadoop_conf_dir = "/usr/hdp/current/hadoop-client/conf"

    stack_info = hdp_select._get_upgrade_stack()

    # if upgrading to >= HDP 2.3
    if stack_info is not None and Script.is_hdp_stack_greater_or_equal("2.3"):
      stack_name = stack_info[0]
      stack_version = stack_info[1]

      # ensure the new HDP stack is conf-selected
      select(stack_name, "hadoop", stack_version)

      # determine if hdp-select has been run and if not, then use the current
      # hdp version until this component is upgraded
      if not force_latest_on_upgrade:
        current_hdp_version = hdp_select.get_role_component_current_hdp_version()
        if current_hdp_version is not None and stack_version != current_hdp_version:
          stack_version = current_hdp_version

      # only change the hadoop_conf_dir path, don't conf-select this older version
      hadoop_conf_dir = "/usr/hdp/{0}/hadoop/conf".format(stack_version)

  return hadoop_conf_dir




