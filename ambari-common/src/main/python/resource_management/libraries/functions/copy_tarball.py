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

__all__ = ["copy_to_hdfs", ]

import os
import uuid
import tempfile
import re

from resource_management.libraries.script.script import Script
from resource_management.libraries.resources.hdfs_resource import HdfsResource
from resource_management.libraries.functions.default import default
from resource_management.core import shell
from resource_management.core.logger import Logger
from resource_management.libraries.functions import stack_tools

STACK_NAME_PATTERN = "{{ stack_name }}"
STACK_ROOT_PATTERN = "{{ stack_root }}"
STACK_VERSION_PATTERN = "{{ stack_version }}"

# TODO, in the future, each stack can define its own mapping of tarballs
# inside the stack definition directory in some sort of xml file.
# PLEASE DO NOT put this in cluster-env since it becomes much harder to change,
# especially since it is an attribute of a stack and becomes
# complicated to change during a Rolling/Express upgrade.
TARBALL_MAP = {
  "slider": ("{0}/{1}/slider/lib/slider.tar.gz".format(STACK_ROOT_PATTERN, STACK_VERSION_PATTERN),
             "/{0}/apps/{1}/slider/slider.tar.gz".format(STACK_NAME_PATTERN, STACK_VERSION_PATTERN)),
  "tez": ("{0}/{1}/tez/lib/tez.tar.gz".format(STACK_ROOT_PATTERN, STACK_VERSION_PATTERN),
          "/{0}/apps/{1}/tez/tez.tar.gz".format(STACK_NAME_PATTERN, STACK_VERSION_PATTERN)),
  "tez_hive2": ("{0}/{1}/tez_hive2/lib/tez.tar.gz".format(STACK_ROOT_PATTERN, STACK_VERSION_PATTERN),
          "/{0}/apps/{1}/tez_hive2/tez.tar.gz".format(STACK_NAME_PATTERN, STACK_VERSION_PATTERN)),
  "hive": ("{0}/{1}/hive/hive.tar.gz".format(STACK_ROOT_PATTERN, STACK_VERSION_PATTERN),
           "/{0}/apps/{1}/hive/hive.tar.gz".format(STACK_NAME_PATTERN, STACK_VERSION_PATTERN)),
  "pig": ("{0}/{1}/pig/pig.tar.gz".format(STACK_ROOT_PATTERN, STACK_VERSION_PATTERN),
          "/{0}/apps/{1}/pig/pig.tar.gz".format(STACK_NAME_PATTERN, STACK_VERSION_PATTERN)),
  "hadoop_streaming": ("{0}/{1}/hadoop-mapreduce/hadoop-streaming.jar".format(STACK_ROOT_PATTERN, STACK_VERSION_PATTERN),
                       "/{0}/apps/{1}/mapreduce/hadoop-streaming.jar".format(STACK_NAME_PATTERN, STACK_VERSION_PATTERN)),
  "sqoop": ("{0}/{1}/sqoop/sqoop.tar.gz".format(STACK_ROOT_PATTERN, STACK_VERSION_PATTERN),
            "/{0}/apps/{1}/sqoop/sqoop.tar.gz".format(STACK_NAME_PATTERN, STACK_VERSION_PATTERN)),
  "mapreduce": ("{0}/{1}/hadoop/mapreduce.tar.gz".format(STACK_ROOT_PATTERN, STACK_VERSION_PATTERN),
                "/{0}/apps/{1}/mapreduce/mapreduce.tar.gz".format(STACK_NAME_PATTERN, STACK_VERSION_PATTERN)),
  "spark": ("{0}/{1}/spark/lib/spark-{2}-assembly.jar".format(STACK_ROOT_PATTERN, STACK_VERSION_PATTERN, STACK_NAME_PATTERN),
            "/{0}/apps/{1}/spark/spark-{0}-assembly.jar".format(STACK_NAME_PATTERN, STACK_VERSION_PATTERN))
}


def _get_tarball_paths(name, use_upgrading_version_during_upgrade=True, custom_source_file=None, custom_dest_file=None):
  """
  For a given tarball name, get the source and destination paths to use.
  :param name: Tarball name
  :param use_upgrading_version_during_upgrade:
  :param custom_source_file: If specified, use this source path instead of the default one from the map.
  :param custom_dest_file: If specified, use this destination path instead of the default one from the map.
  :return: A tuple of (success status, source path, destination path)
  """
  stack_name = Script.get_stack_name()

  if not stack_name:
    Logger.error("Cannot copy {0} tarball to HDFS because stack name could not be determined.".format(str(name)))
    return (False, None, None)

  stack_version = _get_current_version(use_upgrading_version_during_upgrade)
  if not stack_version:
    Logger.error("Cannot copy {0} tarball to HDFS because stack version could be be determined.".format(str(name)))
    return (False, None, None)

  stack_root = Script.get_stack_root()
  if not stack_root:
    Logger.error("Cannot copy {0} tarball to HDFS because stack root could be be determined.".format(str(name)))
    return (False, None, None)

  if name is None or name.lower() not in TARBALL_MAP:
    Logger.error("Cannot copy tarball to HDFS because {0} is not supported in stack {1} for this operation.".format(str(name), str(stack_name)))
    return (False, None, None)
  (source_file, dest_file) = TARBALL_MAP[name.lower()]

  if custom_source_file is not None:
    source_file = custom_source_file

  if custom_dest_file is not None:
    dest_file = custom_dest_file

  source_file = source_file.replace(STACK_NAME_PATTERN, stack_name.lower())
  dest_file = dest_file.replace(STACK_NAME_PATTERN, stack_name.lower())

  source_file = source_file.replace(STACK_ROOT_PATTERN, stack_root.lower())
  dest_file = dest_file.replace(STACK_ROOT_PATTERN, stack_root.lower())

  source_file = source_file.replace(STACK_VERSION_PATTERN, stack_version)
  dest_file = dest_file.replace(STACK_VERSION_PATTERN, stack_version)

  return (True, source_file, dest_file)


def _get_current_version(use_upgrading_version_during_upgrade=True):
  """
  Get the effective version to use to copy the tarballs to.
  :param use_upgrading_version_during_upgrade: True, except when the RU/EU hasn't started yet.
  :return: Version, or False if an error occurred.
  """
  upgrade_direction = default("/commandParams/upgrade_direction", None)
  is_stack_upgrade = upgrade_direction is not None
  current_version = default("/hostLevelParams/current_version", None)
  Logger.info("Default version is {0}".format(current_version))
  if is_stack_upgrade:
    if use_upgrading_version_during_upgrade:
      # This is the version going to. In the case of a downgrade, it is the lower version.
      current_version = default("/commandParams/version", None)
      Logger.info("Because this is a Stack Upgrade, will use version {0}".format(current_version))
    else:
      Logger.info("This is a Stack Upgrade, but keep the version unchanged.")
  else:
    if current_version is None:
      # During normal operation, the first installation of services won't yet know about the version, so must rely
      # on <stack-selector> to get it.
      stack_version = _get_single_version_from_stack_select()
      if stack_version:
        Logger.info("Will use stack version {0}".format(stack_version))
        current_version = stack_version

  if current_version is None:
    message_suffix = "during stack %s" % str(upgrade_direction) if is_stack_upgrade else ""
    Logger.warning("Cannot copy tarball because unable to determine current version {0}.".format(message_suffix))
    return False

  return current_version


def _get_single_version_from_stack_select():
  """
  Call "<stack-selector> versions" and return the version string if only one version is available.
  :return: Returns a version string if successful, and None otherwise.
  """
  # Ubuntu returns: "stdin: is not a tty", as subprocess output, so must use a temporary file to store the output.
  tmpfile = tempfile.NamedTemporaryFile()
  tmp_dir = Script.get_tmp_dir()
  tmp_file = os.path.join(tmp_dir, "copy_tarball_out.txt")
  stack_version = None

  out = None
  stack_selector_path = stack_tools.get_stack_tool_path(stack_tools.STACK_SELECTOR_NAME)
  get_stack_versions_cmd = "{0} versions > {1}".format(stack_selector_path, tmp_file)
  try:
    code, stdoutdata = shell.call(get_stack_versions_cmd, logoutput=True)
    with open(tmp_file, 'r+') as file:
      out = file.read()
  except Exception, e:
    Logger.logger.exception("Could not parse output of {0}. Error: {1}".format(str(tmp_file), str(e)))
  finally:
    try:
      if os.path.exists(tmp_file):
        os.remove(tmp_file)
    except Exception, e:
      Logger.logger.exception("Could not remove file {0}. Error: {1}".format(str(tmp_file), str(e)))

  if code != 0 or out is None or out == "":
    Logger.error("Could not verify stack version by calling '{0}'. Return Code: {1}, Output: {2}.".format(get_stack_versions_cmd, str(code), str(out)))
    return None

  matches = re.findall(r"([\d\.]+\-\d+)", out)

  if matches and len(matches) == 1:
    stack_version = matches[0]
  elif matches and len(matches) > 1:
    Logger.error("Found multiple matches for stack version, cannot identify the correct one from: {0}".format(", ".join(matches)))

  return stack_version


def copy_to_hdfs(name, user_group, owner, file_mode=0444, custom_source_file=None, custom_dest_file=None, force_execute=False,
                 use_upgrading_version_during_upgrade=True, replace_existing_files=False, host_sys_prepped=False):
  """
  :param name: Tarball name, e.g., tez, hive, pig, sqoop.
  :param user_group: Group to own the directory.
  :param owner: File owner
  :param file_mode: File permission
  :param custom_source_file: Override the source file path
  :param custom_dest_file: Override the destination file path
  :param force_execute: If true, will execute the HDFS commands immediately, otherwise, will defer to the calling function.
  :param use_upgrading_version_during_upgrade: If true, will use the version going to during upgrade. Otherwise, use the CURRENT (source) version.
  :param host_sys_prepped: If true, tarballs will not be copied as the cluster deployment uses prepped VMs.
  :return: Will return True if successful, otherwise, False.
  """
  import params

  Logger.info("Called copy_to_hdfs tarball: {0}".format(name))
  (success, source_file, dest_file) = _get_tarball_paths(name, use_upgrading_version_during_upgrade,
                                                         custom_source_file, custom_dest_file)

  if not success:
    Logger.error("Could not copy tarball {0} due to a missing or incorrect parameter.".format(str(name)))
    return False

  if host_sys_prepped:
    Logger.warning("Skipping copying {0} to {1} for {2} as its a sys_prepped host.".format(str(source_file), str(dest_file), str(name)))
    return True

  Logger.info("Source file: {0} , Dest file in HDFS: {1}".format(source_file, dest_file))

  if not os.path.exists(source_file):
    Logger.error("WARNING. Cannot copy {0} tarball because file does not exist: {1} . "
                   "It is possible that this component is not installed on this host.".format(str(name), str(source_file)))
    return False

  # Because CopyFromLocal does not guarantee synchronization, it's possible for two processes to first attempt to
  # copy the file to a temporary location, then process 2 fails because the temporary file was already created by
  # process 1, so process 2 tries to clean up by deleting the temporary file, and then process 1
  # cannot finish the copy to the final destination, and both fail!
  # For this reason, the file name on the destination must be unique, and we then rename it to the intended value.
  # The rename operation is synchronized by the Namenode.

  #unique_string = str(uuid.uuid4())[:8]
  #temp_dest_file = dest_file + "." + unique_string

  # The logic above cannot be used until fast-hdfs-resource.jar supports the mv command, or it switches
  # to WebHDFS.


  # If the directory already exists, it is a NO-OP
  dest_dir = os.path.dirname(dest_file)
  params.HdfsResource(dest_dir,
                      type="directory",
                      action="create_on_execute",
                      owner=owner,
                      mode=0555
  )

  # If the file already exists, it is a NO-OP
  params.HdfsResource(dest_file,
                      type="file",
                      action="create_on_execute",
                      source=source_file,
                      group=user_group,
                      owner=owner,
                      mode=0444,
                      replace_existing_files=replace_existing_files,
  )
  Logger.info("Will attempt to copy {0} tarball from {1} to DFS at {2}.".format(name, source_file, dest_file))

  # For improved performance, force_execute should be False so that it is delayed and combined with other calls.
  # If still want to run the command now, set force_execute to True
  if force_execute:
    params.HdfsResource(None, action="execute")

  return True
