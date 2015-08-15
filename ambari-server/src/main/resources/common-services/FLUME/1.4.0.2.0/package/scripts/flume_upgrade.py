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
import shutil
import tarfile
import tempfile

from resource_management.core.logger import Logger
from resource_management.core.exceptions import Fail
from resource_management.core.resources.system import Execute
from resource_management.libraries.functions import tar_archive

BACKUP_TEMP_DIR = "flume-upgrade-backup"
BACKUP_CONF_DIR_ARCHIVE = "flume-conf-backup.tar"

def post_stop_backup():
  """
  Backs up the flume config, config dir, file/spillable channels as part of the
  upgrade process.
  :return:
  """
  Logger.info('Backing up Flume data and configuration before upgrade...')
  directoryMappings = _get_directory_mappings()

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


def pre_start_restore():
  """
  Restores the flume config, config dir, file/spillable channels to their proper locations
  after an upgrade has completed.
  :return:
  """
  Logger.info('Restoring Flume data and configuration after upgrade...')
  directoryMappings = _get_directory_mappings()

  for directory in directoryMappings:
    archive = os.path.join(tempfile.gettempdir(), BACKUP_TEMP_DIR,
      directoryMappings[directory])

    if os.path.isfile(archive):
      tar_archive.untar_archive(archive, directory)

    # cleanup
    if os.path.exists(os.path.join(tempfile.gettempdir(), BACKUP_TEMP_DIR)):
      shutil.rmtree(os.path.join(tempfile.gettempdir(), BACKUP_TEMP_DIR))


def _get_directory_mappings():
  """
  Gets a dictionary of directory to archive name that represents the
  directories that need to be backed up and their output tarball archive targets
  :return:  the dictionary of directory to tarball mappings
  """
  import params

  return { params.flume_conf_dir : BACKUP_CONF_DIR_ARCHIVE}
