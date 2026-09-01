#!/usr/bin/env python3
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

__all__ = ["copy_to_hdfs", "get_sysprep_skip_copy_tarballs_hdfs"]

import os

from resource_management.core.logger import Logger
from resource_management.libraries.functions import (
  stack_features,
  stack_select,
)
from resource_management.libraries.functions.default import default
from resource_management.libraries.script.script import Script

STACK_NAME_PATTERN = "{{ stack_name }}"
STACK_ROOT_PATTERN = "{{ stack_root }}"
STACK_VERSION_PATTERN = "{{ stack_version }}"
TARBALL_MAP = {
  "tez": {
    "dirs": (
      f"{STACK_ROOT_PATTERN}/{STACK_VERSION_PATTERN}/usr/lib/tez/lib/tez.tar.gz",
      f"/{STACK_NAME_PATTERN}/apps/{STACK_VERSION_PATTERN}/tez/tez.tar.gz",
    ),
    "service": "TEZ",
  },
}

SERVICE_TO_CONFIG_MAP = {"tez": "tez-env"}


def get_sysprep_skip_copy_tarballs_hdfs():
  host_sys_prepped = default("/ambariLevelParams/host_sys_prepped", False)

  # By default, copy the tarballs to HDFS. If the cluster is sysprepped, then set based on the config.
  sysprep_skip_copy_tarballs_hdfs = False
  if host_sys_prepped:
    sysprep_skip_copy_tarballs_hdfs = default(
      "/configurations/cluster-env/sysprep_skip_copy_tarballs_hdfs", False
    )
  return sysprep_skip_copy_tarballs_hdfs


def get_tarball_paths(
  name,
  use_upgrading_version_during_upgrade=True,
  custom_source_file=None,
  custom_dest_file=None,
):
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
    Logger.error(
      f"Cannot copy {str(name)} tarball to HDFS because stack name could not be determined."
    )
    return False, None, None

  normalized_name = name.lower() if isinstance(name, str) else None
  if normalized_name not in TARBALL_MAP:
    Logger.error(
      f"Cannot copy tarball to HDFS because {str(name)} is not supported in stack {str(stack_name)} for this operation."
    )
    return False, None, None

  service = TARBALL_MAP[normalized_name]["service"]

  stack_version = get_current_version(
    service=service,
    use_upgrading_version_during_upgrade=use_upgrading_version_during_upgrade,
  )
  if not stack_version:
    Logger.error(
      f"Cannot copy {str(name)} tarball to HDFS because stack version could not be determined."
    )
    return False, None, None

  stack_root = Script.get_stack_root()
  if not stack_root:
    Logger.error(
      f"Cannot copy {str(name)} tarball to HDFS because stack root could not be determined."
    )
    return False, None, None

  (source_file, dest_file) = TARBALL_MAP[normalized_name]["dirs"]

  if custom_source_file is not None:
    source_file = custom_source_file

  if custom_dest_file is not None:
    dest_file = custom_dest_file

  source_file = source_file.replace(STACK_NAME_PATTERN, stack_name.lower())
  dest_file = dest_file.replace(STACK_NAME_PATTERN, stack_name.lower())

  source_file = source_file.replace(STACK_ROOT_PATTERN, stack_root)
  dest_file = dest_file.replace(STACK_ROOT_PATTERN, stack_root)

  source_file = source_file.replace(STACK_VERSION_PATTERN, stack_version)
  dest_file = dest_file.replace(STACK_VERSION_PATTERN, stack_version)

  return True, source_file, dest_file


def get_current_version(service, use_upgrading_version_during_upgrade=True):
  """
  Get the effective version to use to copy the tarballs to.
  :param service: the service name when checking for an upgrade.
  :param use_upgrading_version_during_upgrade: True, except when the RU/EU hasn't started yet.
  :return: Version, or False if an error occurred.
  """

  from resource_management.libraries.functions import upgrade_summary

  # get the version for this command
  version = stack_features.get_stack_feature_version(Script.get_config())
  version = upgrade_summary.get_target_version(
    service_name=service, default_version=version
  )

  # if there is no upgrade, then use the command's version
  if not Script.in_stack_upgrade() or use_upgrading_version_during_upgrade:
    Logger.info(
      f"Tarball version was calculated as {version}. Use Command Version: {use_upgrading_version_during_upgrade}"
    )

    return version

  # we're in an upgrade and we need to use an older version
  current_version = stack_select.get_role_component_current_stack_version()
  current_version = upgrade_summary.get_source_version(
    service_name=service, default_version=current_version
  )

  if current_version is None:
    Logger.warning(
      "Unable to determine the current version of the component for this command; unable to copy the tarball"
    )
    return False

  return current_version


def copy_to_hdfs(
  name,
  user_group,
  owner,
  file_mode=0o444,
  custom_source_file=None,
  custom_dest_file=None,
  force_execute=False,
  use_upgrading_version_during_upgrade=True,
  replace_existing_files=False,
  skip=False,
  skip_component_check=False,
):
  """
  :param name: Tarball name. The BIGTOP runtime currently supports Tez.
  :param user_group: Group to own the directory.
  :param owner: File owner
  :param file_mode: File permission
  :param custom_source_file: Override the source file path
  :param custom_dest_file: Override the destination file path
  :param force_execute: If true, will execute the HDFS commands immediately, otherwise, will defer to the calling function.
  :param use_upgrading_version_during_upgrade: If true, will use the version going to during upgrade. Otherwise, use the CURRENT (source) version.
  :param skip: If true, tarballs will not be copied as the cluster deployment uses prepped VMs.
  :param skip_component_check: If true, will skip checking if a given component is installed on the node for a file under its dir to be copied.
                               This is in case the file is not mapped to a component but rather to a specific location (JDK jar, Ambari jar, etc).
  :return: Will return True if successful, otherwise, False.
  """
  import params

  Logger.info(f"Called copy_to_hdfs tarball: {name}")
  (success, source_file, dest_file) = get_tarball_paths(
    name, use_upgrading_version_during_upgrade, custom_source_file, custom_dest_file
  )

  if not success:
    Logger.error(
      f"Could not copy tarball {str(name)} due to a missing or incorrect parameter."
    )
    return False

  if skip:
    Logger.warning(
      f"Skipping copying {str(source_file)} to {str(dest_file)} for {str(name)} as it is a sys prepped host."
    )
    return True

  if not skip_component_check:
    # Check if service is installed on the cluster to check if a file can be copied into HDFS
    config_name = SERVICE_TO_CONFIG_MAP[name.lower()]
    config = default("/configurations/" + config_name, None)
    if config is None:
      Logger.info(
        f"{config_name} is not present on the cluster. Skip copying {source_file}"
      )
      return False

  Logger.info(f"Source file: {source_file} , Dest file in HDFS: {dest_file}")

  if not os.path.exists(source_file):
    Logger.error(
      f"WARNING. Cannot copy {str(name)} tarball because file does not exist: {str(source_file)} . "
      "It is possible that this component is not installed on this host."
    )
    return False

  # If the directory already exists, it is a NO-OP
  dest_dir = os.path.dirname(dest_file)
  params.HdfsResource(
    dest_dir, type="directory", action="create_on_execute", owner=owner, mode=0o555
  )

  # If the file already exists, it is a NO-OP
  params.HdfsResource(
    dest_file,
    type="file",
    action="create_on_execute",
    source=source_file,
    group=user_group,
    owner=owner,
    mode=file_mode,
    replace_existing_files=replace_existing_files,
  )
  Logger.info(
    f"Will attempt to copy {name} tarball from {source_file} to DFS at {dest_file}."
  )

  # For improved performance, force_execute should be False so that it is delayed and combined with other calls.
  # If still want to run the command now, set force_execute to True
  if force_execute:
    params.HdfsResource(None, action="execute")

  return True
