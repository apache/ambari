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
from resource_management.core.exceptions import Fail
from resource_management.core.resources.system import Execute
from resource_management.libraries.functions.default import default
from resource_management.libraries.functions.get_hdp_version import get_hdp_version
from resource_management.libraries.script.script import Script

# hdp-select set oozie-server 2.2.0.0-1234
TEMPLATE = ('hdp-select', 'set')

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

# mapping of service check to hdp-select component
SERVICE_CHECK_DIRECTORY_MAP = {
  "HDFS_SERVICE_CHECK" : "hadoop-client",
  "TEZ_SERVICE_CHECK" : "hadoop-client",
  "PIG_SERVICE_CHECK" : "hadoop-client",
  "HIVE_SERVICE_CHECK" : "hadoop-client",
  "OOZIE_SERVICE_CHECK" : "hadoop-client",
  "MAHOUT_SERVICE_CHECK" : "mahout-client"
}

# /usr/hdp/current/hadoop-client/[bin|sbin|libexec|lib]
# /usr/hdp/2.3.0.0-1234/hadoop/[bin|sbin|libexec|lib]
HADOOP_DIR_TEMPLATE = "/usr/hdp/{0}/{1}/{2}"

# /usr/hdp/current/hadoop-client
# /usr/hdp/2.3.0.0-1234/hadoop
HADOOP_HOME_DIR_TEMPLATE = "/usr/hdp/{0}/{1}"

HADOOP_DIR_DEFAULTS = {
  "home": "/usr/lib/hadoop",
  "libexec": "/usr/lib/hadoop/libexec",
  "sbin": "/usr/lib/hadoop/sbin",
  "bin": "/usr/bin",
  "lib": "/usr/lib/hadoop/lib"
}

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
  command = TEMPLATE + (component, version)
  Execute(command, sudo=True)

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


def get_role_component_current_hdp_version():
  """
  Gets the current HDP version of the component that this role command is for.
  :return:  the current HDP version of the specified component or None
  """
  hdp_select_component = None
  role = default("/role", "")
  role_command =  default("/roleCommand", "")

  if role in SERVER_ROLE_DIRECTORY_MAP:
    hdp_select_component = SERVER_ROLE_DIRECTORY_MAP[role]
  elif role_command == "SERVICE_CHECK" and role in SERVICE_CHECK_DIRECTORY_MAP:
    hdp_select_component = SERVICE_CHECK_DIRECTORY_MAP[role]

  if hdp_select_component is None:
    return None

  current_hdp_version = get_hdp_version(hdp_select_component)

  if current_hdp_version is None:
    Logger.warning("Unable to determine hdp-select version for {0}".format(
      hdp_select_component))
  else:
    Logger.info("{0} is currently at version {1}".format(
      hdp_select_component, current_hdp_version))

  return current_hdp_version


def get_hadoop_dir(target, force_latest_on_upgrade=False, upgrade_stack_only=False):
  """
  Return the hadoop shared directory in the following override order
  1. Use default for 2.1 and lower
  2. If 2.2 and higher, use /usr/hdp/current/hadoop-client/{target}
  3. If 2.2 and higher AND for an upgrade, use /usr/hdp/<version>/hadoop/{target}.
  However, if the upgrade has not yet invoked hdp-select, return the current
  version of the component.
  :target: the target directory
  :force_latest_on_upgrade: if True, then this will return the "current" directory
  without the HDP version built into the path, such as /usr/hdp/current/hadoop-client
  :upgrade_stack_only: if True, provides upgrade stack target if present and not current
  """

  if not target in HADOOP_DIR_DEFAULTS:
    raise Fail("Target {0} not defined".format(target))

  hadoop_dir = HADOOP_DIR_DEFAULTS[target]

  if Script.is_hdp_stack_greater_or_equal("2.2"):
    # home uses a different template
    if target == "home":
      hadoop_dir = HADOOP_HOME_DIR_TEMPLATE.format("current", "hadoop-client")
    else:
      hadoop_dir = HADOOP_DIR_TEMPLATE.format("current", "hadoop-client", target)

    # if we are not forcing "current" for HDP 2.2, then attempt to determine
    # if the exact version needs to be returned in the directory
    if not force_latest_on_upgrade:
      stack_info = _get_upgrade_stack()

      if stack_info is not None:
        stack_version = stack_info[1]

        # determine if hdp-select has been run and if not, then use the current
        # hdp version until this component is upgraded
        current_hdp_version = get_role_component_current_hdp_version()
        if current_hdp_version is not None and stack_version != current_hdp_version and not upgrade_stack_only:
          stack_version = current_hdp_version

        if target == "home":
          # home uses a different template
          hadoop_dir = HADOOP_HOME_DIR_TEMPLATE.format(stack_version, "hadoop")
        else:
          hadoop_dir = HADOOP_DIR_TEMPLATE.format(stack_version, "hadoop", target)

  return hadoop_dir


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
