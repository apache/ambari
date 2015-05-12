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
from resource_management.core.logger import Logger
from resource_management.libraries.functions.default import default
from resource_management.libraries.functions.get_hdp_version import get_hdp_version
from resource_management.libraries.script.script import Script

# a mapping of Ambari server role to hdp-select component name for all
# non-clients
SERVER_ROLE_DIRECTORY_MAP = {
  'ACCUMULO_MASTER' : 'accumulo-master',
  'ACCUMULO_MONITOR' : 'accumulo-monitor',
  'ACCUMULO_GC' : 'accumulo-gc',
  'ACCUMULO_TRACER' : 'accumulo-tracer',
  'ACCUMULO_TSERVER' : 'accumulo-tablet',
  'ATLAS_SERVER' : 'atlas-server',
  'FLUME_HANDLER' : 'flume-server',
  'FALCON_SERVER' : 'falcon-server',
  'NAMENODE' : 'hadoop-hdfs-namenode',
  'DATANODE' : 'hadoop-hdfs-datanode',
  'SECONDARY_NAMENODE' : 'hadoop-hdfs-secondarynamenode',
  'NFS_GATEWAY' : 'hadoop-hdfs-nfs3',
  'JOURNALNODE' : 'hadoop-hdfs-journalnode',
  'HBASE_MASTER' : 'hbase-master',
  'HBASE_REGIONSERVER' : 'hbase-regionserver',
  'HIVE_METASTORE' : 'hive-metastore',
  'HIVE_SERVER' : 'hive-server2',
  'WEBHCAT_SERVER' : 'hive-webhcat',
  'KAFKA_BROKER' : 'kafka-broker',
  'KNOX_GATEWAY' : 'knox-server',
  'OOZIE_SERVER' : 'oozie-server',
  'RANGER_ADMIN' : 'ranger-admin',
  'RANGER_USERSYNC' : 'ranger-usersync',
  'SPARK_JOBHISTORYSERVER' : 'spark-historyserver',
  'NIMBUS' : 'storm-nimbus',
  'SUPERVISOR' : 'storm-supervisor',
  'HISTORYSERVER' : 'hadoop-mapreduce-historyserver',
  'APP_TIMELINE_SERVER' : 'hadoop-yarn-timelineserver',
  'NODEMANAGER' : 'hadoop-yarn-nodemanager',
  'RESOURCEMANAGER' : 'hadoop-yarn-resourcemanager',
  'ZOOKEEPER_SERVER' : 'zookeeper-server'
}

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

def _get_upgrade_stack():
  """
  Gets the stack name and stack version if an upgrade is currently in progress.
  :return:  the stack name and stack version as a tuple, or None if an
  upgrade is not in progress.
  """
  from resource_management.libraries.functions.default import default
  direction = default("/commandParams/upgrade_direction", None)
  stack_name = default("/hostLevelParams/stack_name", None)
  stack_version = default("/commandParams/version", None)

  if direction and stack_name and stack_version:
    return (stack_name, stack_version)

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

    stack_info = _get_upgrade_stack()

    if stack_info is not None and Script.is_hdp_stack_greater_or_equal("2.3"):
      stack_name = stack_info[0]
      stack_version = stack_info[1]

      select(stack_name, "hadoop", stack_version)
      hadoop_conf_dir = "/usr/hdp/{0}/hadoop/conf".format(stack_version)

  return hadoop_conf_dir

def get_hadoop_dir(target):
  """
  Return the hadoop shared directory in the following override order
  1. Use default for 2.1 and lower
  2. If 2.2 and higher, use /usr/hdp/current/hadoop-client/{target}
  3. If 2.2 and higher AND for an upgrade, use /usr/hdp/<version>/hadoop/{target}.
  However, if the upgrade has not yet invoked hdp-select, return the current
  version of the component.
  :target: the target directory
  """

  if not target in HADOOP_DIR_DEFAULTS:
    raise Fail("Target {0} not defined".format(target))

  hadoop_dir = HADOOP_DIR_DEFAULTS[target]

  if Script.is_hdp_stack_greater_or_equal("2.2"):
    hadoop_dir = HADOOP_DIR_TEMPLATE.format("current", "hadoop-client", target)

    stack_info = _get_upgrade_stack()

    if stack_info is not None:
      stack_version = stack_info[1]

      # determine if hdp-select has been run and if not, then use the current
      # hdp version until this component is upgraded
      current_hdp_version = get_role_component_current_hdp_version()
      if current_hdp_version is not None and stack_version != current_hdp_version:
        stack_version = current_hdp_version

      hadoop_dir = HADOOP_DIR_TEMPLATE.format(stack_version, "hadoop", target)

  return hadoop_dir


def get_role_component_current_hdp_version():
  """
  Gets the current HDP version of the component that this role command is for.
  :return:  the current HDP version of the specified component or None
  """
  command_role = default("/role", "")
  if command_role in SERVER_ROLE_DIRECTORY_MAP:
    hdp_select_component = SERVER_ROLE_DIRECTORY_MAP[command_role]
    current_hdp_version = get_hdp_version(hdp_select_component)

    Logger.info("{0} is currently at version {1}".format(
      hdp_select_component, current_hdp_version))
    
    return current_hdp_version

  return None
