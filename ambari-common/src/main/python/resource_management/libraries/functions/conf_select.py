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

import os
import version
import hdp_select

from resource_management.core import shell
from resource_management.libraries.script.script import Script
from resource_management.core.logger import Logger

PACKAGE_DIRS = {
  "accumulo": {
    "conf_dir": "/etc/accumulo/conf",
    "current_dir": "/usr/hdp/current/accumulo-client/conf"
  },
  "falcon": {
    "conf_dir": "/etc/falcon/conf",
    "current_dir": "/usr/hdp/current/falcon-client/conf"
  },
  "hadoop": {
    "conf_dir": "/etc/hadoop/conf",
    "current_dir": "/usr/hdp/current/hadoop-client/conf"
  },
  "hbase": {
    "conf_dir": "/etc/hbase/conf",
    "current_dir": "/usr/hdp/current/hbase-client/conf"
  },
  "hive": {
    "conf_dir": "/etc/hive/conf",
    "current_dir": "/usr/hdp/current/hive-client/conf"
  },
  "kafka": {
    "conf_dir": "/etc/kafka/conf",
    "current_dir": "/usr/hdp/current/kafka-broker/conf"
  },
  "knox": {
    "conf_dir": "/etc/knox/conf",
    "current_dir": "/usr/hdp/current/knox-server/conf"
  },
  "mahout": {
    "conf_dir": "/etc/mahout/conf",
    "current_dir": "/usr/hdp/current/mahout-client/conf"
  },
  "oozie": {
    "conf_dir": "/etc/oozie/conf",
    "current_dir": "/usr/hdp/current/oozie-client/conf"
  },
  "phoenix": {
    "conf_dir": "/etc/phoenix/conf",
    "current_dir": "/usr/hdp/current/phoenix-client/conf"
  },
  "ranger-admin": {
    "conf_dir": "/etc/ranger/admin/conf",
    "current_dir": "/usr/hdp/current/ranger-admin/conf"
  },
  "ranger-kms": {
    "conf_dir": "/etc/ranger/kms/conf",
    "current_dir": "/usr/hdp/current/ranger-kms/conf"
  },
  "ranger-usersync": {
    "conf_dir": "/etc/ranger/kms/usersync",
    "current_dir": "/usr/hdp/current/ranger-usersync/conf"
  },
  "slider": {
    "conf_dir": "/etc/slider/conf",
    "current_dir": "/usr/hdp/current/slider-client/conf"
  },
  "spark": {
    "conf_dir": "/etc/spark/conf",
    "current_dir": "/usr/hdp/current/spark-client/conf"
  },
  "sqoop": {
    "conf_dir": "/etc/sqoop/conf",
    "current_dir": "/usr/hdp/current/sqoop-client/conf"
  },
  "storm": {
    "conf_dir": "/etc/storm/conf",
    "current_dir": "/usr/hdp/current/storm-client/conf"
  },
  "tez": {
    "conf_dir": "/etc/tez/conf",
    "current_dir": "/usr/hdp/current/tez-client/conf"
  },
  "zookeeper": {
    "conf_dir": "/etc/zookeeper/conf",
    "current_dir": "/usr/hdp/current/zookeeper-client/conf"
  }
}

TEMPLATE = "conf-select {0} --package {1} --stack-version {2} --conf-version 0"

def _valid(stack_name, package, ver):
  if stack_name != "HDP":
    return False

  if version.compare_versions(version.format_hdp_stack_version(ver), "2.3.0.0") < 0:
    return False

  return True


def create(stack_name, package, version, dry_run = False):
  """
  Creates a config version for the specified package
  :stack_name: the name of the stack
  :package: the name of the package, as-used by conf-select
  :version: the version number to create
  """

  if not _valid(stack_name, package, version):
    return

  command = "dry-run-create" if dry_run else "create-conf-dir"

  code, stdout = shell.call(TEMPLATE.format(command, package, version), logoutput=False, quiet=True)

  return stdout


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


def create_config_links(stack_id, stack_version):
  """
  Creates config links
  stack_id:  stack id, ie HDP-2.3
  stack_version:  version to set, ie 2.3.0.0-1234
  """

  if stack_id is None:
    Logger.info("Cannot create config links when stack_id is not defined")
    return

  args = stack_id.upper().split('-')
  if len(args) != 2:
    Logger.info("Unrecognized stack id {0}".format(stack_id))
    return

  if args[0] != "HDP":
    Logger.info("Unrecognized stack name {0}".format(args[0]))

  if version.compare_versions(version.format_hdp_stack_version(args[1]), "2.3.0.0") < 0:
    Logger.info("Cannot link configs unless HDP-2.3 or higher")
    return

  for k, v in PACKAGE_DIRS.iteritems():
    if os.path.exists(v['conf_dir']):
      new_conf_dir = create(args[0], k, stack_version, dry_run = True)

      if not os.path.exists(new_conf_dir):
        Logger.info("Creating conf {0} for {1}".format(new_conf_dir, k))
        select(args[0], k, stack_version)

