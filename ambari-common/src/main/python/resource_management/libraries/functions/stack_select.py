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

# Python Imports
import os
import sys
import re

# Local Imports
from resource_management.core.logger import Logger
from resource_management.core.exceptions import Fail
from resource_management.core.resources.system import Execute
from resource_management.libraries.functions.default import default
from resource_management.libraries.functions.get_stack_version import get_stack_version
from resource_management.libraries.functions.format import format
from resource_management.libraries.script.script import Script
from resource_management.libraries.functions import stack_tools
from resource_management.core.shell import call
from resource_management.libraries.functions.version import format_stack_version
from resource_management.libraries.functions.version_select_util import get_versions_from_stack_root
from resource_management.libraries.functions.stack_features import check_stack_feature
from resource_management.libraries.functions import StackFeature

STACK_SELECT_PREFIX = 'ambari-python-wrap'

# a mapping of Ambari server role to <stack-selector-tool> component name for all
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
  'HIVE_SERVER_INTERACTIVE' : 'hive-server2-hive2',
  'WEBHCAT_SERVER' : 'hive-webhcat',
  'KAFKA_BROKER' : 'kafka-broker',
  'KNOX_GATEWAY' : 'knox-server',
  'OOZIE_SERVER' : 'oozie-server',
  'RANGER_ADMIN' : 'ranger-admin',
  'RANGER_USERSYNC' : 'ranger-usersync',
  'RANGER_TAGSYNC' : 'ranger-tagsync',
  'RANGER_KMS' : 'ranger-kms',
  'SPARK_JOBHISTORYSERVER' : 'spark-historyserver',
  'SPARK_THRIFTSERVER' : 'spark-thriftserver',
  'NIMBUS' : 'storm-nimbus',
  'SUPERVISOR' : 'storm-supervisor',
  'HISTORYSERVER' : 'hadoop-mapreduce-historyserver',
  'APP_TIMELINE_SERVER' : 'hadoop-yarn-timelineserver',
  'NODEMANAGER' : 'hadoop-yarn-nodemanager',
  'RESOURCEMANAGER' : 'hadoop-yarn-resourcemanager',
  'ZOOKEEPER_SERVER' : 'zookeeper-server',

  # ZKFC is tied to NN since it doesn't have its own componnet in <stack-selector-tool> and there is
  # a requirement that the ZKFC is installed on each NN
  'ZKFC' : 'hadoop-hdfs-namenode'
}

# mapping of service check to <stack-selector-tool> component
SERVICE_CHECK_DIRECTORY_MAP = {
  "HDFS_SERVICE_CHECK" : "hadoop-client",
  "TEZ_SERVICE_CHECK" : "hadoop-client",
  "PIG_SERVICE_CHECK" : "hadoop-client",
  "HIVE_SERVICE_CHECK" : "hadoop-client",
  "OOZIE_SERVICE_CHECK" : "hadoop-client",
  "MAHOUT_SERVICE_CHECK" : "mahout-client",
  "MAPREDUCE2_SERVICE_CHECK" : "hadoop-client",
  "YARN_SERVICE_CHECK" : "hadoop-client",
  "SLIDER_SERVICE_CHECK" : "slider-client"
}

# <stack-root>/current/hadoop-client/[bin|sbin|libexec|lib]
# <stack-root>/2.3.0.0-1234/hadoop/[bin|sbin|libexec|lib]
HADOOP_DIR_TEMPLATE = "{0}/{1}/{2}/{3}"

# <stack-root>/current/hadoop-client
# <stack-root>/2.3.0.0-1234/hadoop
HADOOP_HOME_DIR_TEMPLATE = "{0}/{1}/{2}"

HADOOP_DIR_DEFAULTS = {
  "home": "/usr/lib/hadoop",
  "libexec": "/usr/lib/hadoop/libexec",
  "sbin": "/usr/lib/hadoop/sbin",
  "bin": "/usr/bin",
  "lib": "/usr/lib/hadoop/lib"
}

def select_all(version_to_select):
  """
  Executes <stack-selector-tool> on every component for the specified version. If the value passed in is a
  stack version such as "2.3", then this will find the latest installed version which
  could be "2.3.0.0-9999". If a version is specified instead, such as 2.3.0.0-1234, it will use
  that exact version.
  :param version_to_select: the version to <stack-selector-tool> on, such as "2.3" or "2.3.0.0-1234"
  """
  stack_root = Script.get_stack_root()
  (stack_selector_name, stack_selector_path, stack_selector_package) = stack_tools.get_stack_tool(stack_tools.STACK_SELECTOR_NAME)
  # it's an error, but it shouldn't really stop anything from working
  if version_to_select is None:
    Logger.error(format("Unable to execute {stack_selector_name} after installing because there was no version specified"))
    return

  Logger.info("Executing {0} set all on {1}".format(stack_selector_name, version_to_select))

  command = format('{sudo} {stack_selector_path} set all `ambari-python-wrap {stack_selector_path} versions | grep ^{version_to_select} | tail -1`')
  only_if_command = format('ls -d {stack_root}/{version_to_select}*')
  Execute(command, only_if = only_if_command)


def select(component, version):
  """
  Executes <stack-selector-tool> on the specific component and version. Some global
  variables that are imported via params/status_params/params_linux will need
  to be recalcuated after the <stack-selector-tool>. However, python does not re-import
  existing modules. The only way to ensure that the configuration variables are
  recalculated is to call reload(...) on each module that has global parameters.
  After invoking <stack-selector-tool>, this function will also reload params, status_params,
  and params_linux.
  :param component: the <stack-selector-tool> component, such as oozie-server. If "all", then all components
  will be updated.
  :param version: the version to set the component to, such as 2.2.0.0-1234
  """
  stack_selector_path = stack_tools.get_stack_tool_path(stack_tools.STACK_SELECTOR_NAME)
  command = (STACK_SELECT_PREFIX, stack_selector_path, "set", component, version)
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
      Logger.info("After {0}, reloaded module {1}".format(command, moduleName))


def get_role_component_current_stack_version():
  """
  Gets the current HDP version of the component that this role command is for.
  :return:  the current HDP version of the specified component or None
  """
  stack_select_component = None
  role = default("/role", "")
  role_command =  default("/roleCommand", "")
  stack_selector_name = stack_tools.get_stack_tool_name(stack_tools.STACK_SELECTOR_NAME)

  if role in SERVER_ROLE_DIRECTORY_MAP:
    stack_select_component = SERVER_ROLE_DIRECTORY_MAP[role]
  elif role_command == "SERVICE_CHECK" and role in SERVICE_CHECK_DIRECTORY_MAP:
    stack_select_component = SERVICE_CHECK_DIRECTORY_MAP[role]

  if stack_select_component is None:
    return None

  current_stack_version = get_stack_version(stack_select_component)

  if current_stack_version is None:
    Logger.warning("Unable to determine {0} version for {1}".format(
      stack_selector_name, stack_select_component))
  else:
    Logger.info("{0} is currently at version {1}".format(
      stack_select_component, current_stack_version))

  return current_stack_version


def get_hadoop_dir(target, force_latest_on_upgrade=False):
  """
  Return the hadoop shared directory in the following override order
  1. Use default for 2.1 and lower
  2. If 2.2 and higher, use <stack-root>/current/hadoop-client/{target}
  3. If 2.2 and higher AND for an upgrade, use <stack-root>/<version>/hadoop/{target}.
  However, if the upgrade has not yet invoked <stack-selector-tool>, return the current
  version of the component.
  :target: the target directory
  :force_latest_on_upgrade: if True, then this will return the "current" directory
  without the stack version built into the path, such as <stack-root>/current/hadoop-client
  """
  stack_root = Script.get_stack_root()
  stack_version = Script.get_stack_version()

  if not target in HADOOP_DIR_DEFAULTS:
    raise Fail("Target {0} not defined".format(target))

  hadoop_dir = HADOOP_DIR_DEFAULTS[target]

  formatted_stack_version = format_stack_version(stack_version)
  if formatted_stack_version and  check_stack_feature(StackFeature.ROLLING_UPGRADE, formatted_stack_version):
    # home uses a different template
    if target == "home":
      hadoop_dir = HADOOP_HOME_DIR_TEMPLATE.format(stack_root, "current", "hadoop-client")
    else:
      hadoop_dir = HADOOP_DIR_TEMPLATE.format(stack_root, "current", "hadoop-client", target)

    # if we are not forcing "current" for HDP 2.2, then attempt to determine
    # if the exact version needs to be returned in the directory
    if not force_latest_on_upgrade:
      stack_info = _get_upgrade_stack()

      if stack_info is not None:
        stack_version = stack_info[1]

        # determine if <stack-selector-tool> has been run and if not, then use the current
        # hdp version until this component is upgraded
        current_stack_version = get_role_component_current_stack_version()
        if current_stack_version is not None and stack_version != current_stack_version:
          stack_version = current_stack_version

        if target == "home":
          # home uses a different template
          hadoop_dir = HADOOP_HOME_DIR_TEMPLATE.format(stack_root, stack_version, "hadoop")
        else:
          hadoop_dir = HADOOP_DIR_TEMPLATE.format(stack_root, stack_version, "hadoop", target)

  return hadoop_dir

def get_hadoop_dir_for_stack_version(target, stack_version):
  """
  Return the hadoop shared directory for the provided stack version. This is necessary
  when folder paths of downgrade-source stack-version are needed after <stack-selector-tool>.
  :target: the target directory
  :stack_version: stack version to get hadoop dir for
  """

  stack_root = Script.get_stack_root()
  if not target in HADOOP_DIR_DEFAULTS:
    raise Fail("Target {0} not defined".format(target))

  hadoop_dir = HADOOP_DIR_DEFAULTS[target]

  formatted_stack_version = format_stack_version(stack_version)
  if formatted_stack_version and  check_stack_feature(StackFeature.ROLLING_UPGRADE, formatted_stack_version):
    # home uses a different template
    if target == "home":
      hadoop_dir = HADOOP_HOME_DIR_TEMPLATE.format(stack_root, stack_version, "hadoop")
    else:
      hadoop_dir = HADOOP_DIR_TEMPLATE.format(stack_root, stack_version, "hadoop", target)

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

def unsafe_get_stack_versions():
  """
  Gets list of stack versions installed on the host.
  By default a call to <stack-selector-tool> versions is made to get the list of installed stack versions.
  DO NOT use a fall-back since this function is called by alerts in order to find potential errors.
  :return: Returns a tuple of (exit code, output, list of installed stack versions).
  """
  stack_selector_path = stack_tools.get_stack_tool_path(stack_tools.STACK_SELECTOR_NAME)
  code, out = call((STACK_SELECT_PREFIX, stack_selector_path, 'versions'))
  versions = []
  if 0 == code:
    for line in out.splitlines():
      versions.append(line.rstrip('\n'))
  return (code, out, versions)

def get_stack_versions(stack_root):
  """
  Gets list of stack versions installed on the host.
  By default a call to <stack-selector-tool> versions is made to get the list of installed stack versions.
  As a fallback list of installed versions is collected from stack version directories in stack install root.
  :param stack_root: Stack install root
  :return: Returns list of installed stack versions.
  """
  stack_selector_path = stack_tools.get_stack_tool_path(stack_tools.STACK_SELECTOR_NAME)
  code, out = call((STACK_SELECT_PREFIX, stack_selector_path, 'versions'))
  versions = []
  if 0 == code:
    for line in out.splitlines():
      versions.append(line.rstrip('\n'))
  if not versions:
    versions = get_versions_from_stack_root(stack_root)
  return versions

def get_stack_version_before_install(component_name):
  """
  Works in the similar way to '<stack-selector-tool> status component',
  but also works for not yet installed packages.
  
  Note: won't work if doing initial install.
  """
  stack_root = Script.get_stack_root()
  component_dir = HADOOP_HOME_DIR_TEMPLATE.format(stack_root, "current", component_name)
  stack_selector_name = stack_tools.get_stack_tool_name(stack_tools.STACK_SELECTOR_NAME)
  if os.path.islink(component_dir):
    stack_version = os.path.basename(os.path.dirname(os.readlink(component_dir)))
    match = re.match('[0-9]+.[0-9]+.[0-9]+.[0-9]+-[0-9]+', stack_version)
    if match is None:
      Logger.info('Failed to get extracted version with {0} in method get_stack_version_before_install'.format(stack_selector_name))
      return None # lazy fail
    return stack_version
  else:
    return None
