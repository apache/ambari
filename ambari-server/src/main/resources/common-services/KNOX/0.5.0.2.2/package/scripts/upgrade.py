
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
import tempfile

from resource_management.core.logger import Logger
from resource_management.core.exceptions import Fail
from resource_management.core.resources.system import Execute
from resource_management.libraries.functions import tar_archive
from resource_management.libraries.functions import format
from resource_management.libraries.functions.stack_features import check_stack_feature
from resource_management.libraries.functions import StackFeature
from resource_management.libraries.script.script import Script


BACKUP_TEMP_DIR = "knox-upgrade-backup"
BACKUP_DATA_ARCHIVE = "knox-data-backup.tar"
STACK_ROOT_DEFAULT = Script.get_stack_root()

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


def seed_current_data_directory():
  """
  HDP stack example:

  Knox uses "versioned" data directories in some stacks:
  /usr/hdp/2.2.0.0-1234/knox/data -> /var/lib/knox/data
  /usr/hdp/2.3.0.0-4567/knox/data -> /var/lib/knox/data-2.3.0.0-4567

  If the stack being upgraded to supports versioned data directories for Knox, then we should
  seed the data from the prior version. This is mainly because Knox keeps things like keystores
  in the data directory and if those aren't copied over then it will re-create self-signed
  versions. This side-effect behavior causes loss of service in clusters where Knox is using
  custom keystores.

  cp -R -p -f /usr/hdp/<old>/knox-server/data/. /usr/hdp/current/knox-server/data
  :return:
  """
  import params

  if params.version is None or params.upgrade_from_version is None:
    raise Fail("The source and target versions are required")

  if check_stack_feature(StackFeature.KNOX_VERSIONED_DATA_DIR, params.version):
    Logger.info("Seeding Knox data from prior version...")

    # <stack-root>/2.3.0.0-1234/knox/data/.
    source_data_dir = os.path.join(params.stack_root, params.upgrade_from_version, "knox", "data", ".")

    # <stack-root>/current/knox-server/data
    target_data_dir = os.path.join(params.stack_root, "current", "knox-server", "data")

    # recursive copy, overwriting, and preserving attributes
    Execute(("cp", "-R", "-p", "-f", source_data_dir, target_data_dir), sudo = True)


def _get_directory_mappings_during_upgrade():
  """
  Gets a dictionary of directory to archive name that represents the
  directories that need to be backed up and their output tarball archive targets
  :return:  the dictionary of directory to tarball mappings
  """
  import params

  # the data directory is always a symlink to the "correct" data directory in /var/lib/knox
  # such as /var/lib/knox/data or /var/lib/knox/data-2.4.0.0-1234
  knox_data_dir = STACK_ROOT_DEFAULT + '/current/knox-server/data'

  directories = { knox_data_dir: BACKUP_DATA_ARCHIVE }

  Logger.info(format("Knox directories to backup:\n{directories}"))
  return directories
