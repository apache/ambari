
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
import os
import tarfile
import tempfile

from resource_management.core.logger import Logger
from resource_management.core.exceptions import Fail
from resource_management.libraries.functions import tar_archive
from resource_management.libraries.functions import format
from resource_management.libraries.functions import Direction
from resource_management.libraries.functions.version import compare_versions,format_hdp_stack_version


BACKUP_TEMP_DIR = "knox-upgrade-backup"
BACKUP_DATA_ARCHIVE = "knox-data-backup.tar"
BACKUP_CONF_ARCHIVE = "knox-conf-backup.tar"

def backup_data():
  """
  Backs up the knox data as part of the upgrade process.
  :return: Returns the path to the absolute backup directory.
  """
  Logger.info('Backing up Knox data directory before upgrade...')
  directoryMappings = _get_directory_mappings_during_upgrade()

  Logger.info("Directory mappings to backup: {0}".format(str(directoryMappings)))

  absolute_backup_dir = os.path.join(tempfile.gettempdir(), BACKUP_TEMP_DIR)
  if not os.path.isdir(absolute_backup_dir):
    os.makedirs(absolute_backup_dir)

  for directory in directoryMappings:
    if not os.path.isdir(directory):
      raise Fail("Unable to backup missing directory {0}".format(directory))

    archive = os.path.join(absolute_backup_dir, directoryMappings[directory])
    Logger.info('Compressing {0} to {1}'.format(directory, archive))

    if os.path.exists(archive):
      os.remove(archive)

    # backup the directory, following symlinks instead of including them
    tar_archive.archive_directory_dereference(archive, directory)

  return absolute_backup_dir


def _get_directory_mappings_during_upgrade():
  """
  Gets a dictionary of directory to archive name that represents the
  directories that need to be backed up and their output tarball archive targets
  :return:  the dictionary of directory to tarball mappings
  """
  import params

  # Must be performing an Upgrade
  if params.upgrade_direction is None or params.upgrade_direction != Direction.UPGRADE or \
          params.upgrade_from_version is None or params.upgrade_from_version == "":
    Logger.error("Function _get_directory_mappings_during_upgrade() can only be called during a Stack Upgrade in direction UPGRADE.")
    return {}

  # By default, use this for all stacks.
  knox_data_dir = '/var/lib/knox/data'

  if params.stack_name and params.stack_name.upper() == "HDP" and \
          compare_versions(format_hdp_stack_version(params.upgrade_from_version), "2.3.0.0") > 0:
    # Use the version that is being upgraded from.
    knox_data_dir = format('/usr/hdp/{upgrade_from_version}/knox/data')

  # the trailing "/" is important here so as to not include the "conf" folder itself
  directories = {knox_data_dir: BACKUP_DATA_ARCHIVE, params.knox_conf_dir + "/": BACKUP_CONF_ARCHIVE}

  Logger.info(format("Knox directories to backup:\n{directories}"))
  return directories

