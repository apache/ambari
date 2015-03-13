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
import glob
import os
import shutil
import tarfile
import tempfile

from resource_management.core import shell
from resource_management.core.logger import Logger
from resource_management.core.exceptions import Fail
from resource_management.core.resources.system import Execute
from resource_management.libraries.functions import Direction
from resource_management.libraries.functions import format
from resource_management.libraries.functions import compare_versions
from resource_management.libraries.functions import format_hdp_stack_version

BACKUP_TEMP_DIR = "oozie-upgrade-backup"
BACKUP_CONF_ARCHIVE = "oozie-conf-backup.tar"


def backup_configuration():
  """
  Backs up the oozie configuration as part of the upgrade process.
  :return:
  """
  Logger.info('Backing up Oozie configuration directory before upgrade...')
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

    tarball = None
    try:
      tarball = tarfile.open(archive, "w")
      tarball.add(directory, arcname=os.path.basename(directory))
    finally:
      if tarball:
        tarball.close()


def restore_configuration():
  """
  Restores the configuration backups to their proper locations after an
  upgrade has completed.
  :return:
  """
  Logger.info('Restoring Oozie configuration directory after upgrade...')
  directoryMappings = _get_directory_mappings()

  for directory in directoryMappings:
    archive = os.path.join(tempfile.gettempdir(), BACKUP_TEMP_DIR,
      directoryMappings[directory])

    if not os.path.isfile(archive):
      raise Fail("Unable to restore missing backup archive {0}".format(archive))

    Logger.info('Extracting {0} to {1}'.format(archive, directory))

    tarball = None
    try:
      tarball = tarfile.open(archive, "r")
      tarball.extractall(directory)
    finally:
      if tarball:
        tarball.close()

  # cleanup
  shutil.rmtree(os.path.join(tempfile.gettempdir(), BACKUP_TEMP_DIR))


def prepare_libext_directory():
  """
  Creates /usr/hdp/current/oozie/libext-customer and recursively sets
  777 permissions on it and its parents.
  :return:
  """
  import params

  # some versions of HDP don't need the lzo compression libraries
  target_version_needs_compression_libraries = compare_versions(
    format_hdp_stack_version(params.version), '2.2.1.0') >= 0

  if not os.path.isdir(params.oozie_libext_customer_dir):
    os.makedirs(params.oozie_libext_customer_dir, 0o777)

  # ensure that it's rwx for all
  os.chmod(params.oozie_libext_customer_dir, 0o777)

  # get all hadooplzo* JAR files
  # hdp-select set hadoop-client has not run yet, therefore we cannot use
  # /usr/hdp/current/hadoop-client ; we must use params.version directly
  # however, this only works when upgrading beyond 2.2.0.0; don't do this
  # for downgrade to 2.2.0.0 since hadoop-lzo will not be present
  # This can also be called during a Downgrade.
  # When a version is Intalled, it is responsible for downloading the hadoop-lzo packages
  # if lzo is enabled.
  if params.lzo_enabled and (params.upgrade_direction == Direction.UPGRADE or target_version_needs_compression_libraries):
    hadoop_lzo_pattern = 'hadoop-lzo*.jar'
    hadoop_client_new_lib_dir = format("/usr/hdp/{version}/hadoop/lib")

    files = glob.iglob(os.path.join(hadoop_client_new_lib_dir, hadoop_lzo_pattern))
    if not files:
      raise Fail("There are no files at {0} matching {1}".format(
        hadoop_client_new_lib_dir, hadoop_lzo_pattern))

    # copy files into libext
    files_copied = False
    for file in files:
      if os.path.isfile(file):
        Logger.info("Copying {0} to {1}".format(str(file), params.oozie_libext_customer_dir))
        shutil.copy2(file, params.oozie_libext_customer_dir)
        files_copied = True

    if not files_copied:
      raise Fail("There are no files at {0} matching {1}".format(
        hadoop_client_new_lib_dir, hadoop_lzo_pattern))

  # copy ext ZIP to customer dir
  oozie_ext_zip_file = '/usr/share/HDP-oozie/ext-2.2.zip'
  if not os.path.isfile(oozie_ext_zip_file):
    raise Fail("Unable to copy {0} because it does not exist".format(oozie_ext_zip_file))

  Logger.info("Copying {0} to {1}".format(oozie_ext_zip_file, params.oozie_libext_customer_dir))
  shutil.copy2(oozie_ext_zip_file, params.oozie_libext_customer_dir)


def upgrade_oozie():
  """
  Performs the upgrade of the oozie WAR file and database.
  :return:
  """
  import params

  # get the kerberos token if necessary to execute commands as oozie
  if params.security_enabled:
    oozie_principal_with_host = params.oozie_principal.replace("_HOST", params.hostname)
    command = format("{kinit_path_local} -kt {oozie_keytab} {oozie_principal_with_host}")
    Execute(command, user=params.oozie_user)

  # ensure that HDFS is prepared to receive the new sharelib
  command = format("hdfs dfs -chown oozie:hadoop {oozie_hdfs_user_dir}/share")
  Execute(command, user=params.oozie_user)

  command = format("hdfs dfs -chmod -R 755 {oozie_hdfs_user_dir}/share")
  Execute(command, user=params.oozie_user)

  # upgrade oozie DB
  command = format("{oozie_home}/bin/ooziedb.sh upgrade -run")
  Execute(command, user=params.oozie_user)

  # prepare the oozie WAR
  command = format("{oozie_setup_sh} prepare-war -d {oozie_libext_customer_dir}")
  return_code, oozie_output = shell.call(command)

  if return_code != 0 or "New Oozie WAR file with added" not in oozie_output:
    message = "Unexpected Oozie WAR preparation output {0}".format(oozie_output)
    Logger.error(message)
    raise Fail(message)

  # install new sharelib to HDFS
  command = format("{oozie_setup_sh} sharelib create -fs {fs_root}")
  Execute(command, user=params.oozie_user)


def _get_directory_mappings():
  """
  Gets a dictionary of directory to archive name that represents the
  directories that need to be backed up and their output tarball archive targets
  :return:  the dictionary of directory to tarball mappings
  """
  import params

  return { params.conf_dir : BACKUP_CONF_ARCHIVE }
