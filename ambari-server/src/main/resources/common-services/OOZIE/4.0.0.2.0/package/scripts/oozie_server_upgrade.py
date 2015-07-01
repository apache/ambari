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
from resource_management.libraries.functions import hdp_select
from resource_management.libraries.functions import format_hdp_stack_version
from resource_management.libraries.functions import tar_archive
from resource_management.libraries.script.script import Script
from resource_management.core.resources import File
from resource_management.core.source import DownloadSource

BACKUP_TEMP_DIR = "oozie-upgrade-backup"
BACKUP_CONF_ARCHIVE = "oozie-conf-backup.tar"

class OozieUpgrade(Script):

  @staticmethod
  def backup_configuration():
    """
    Backs up the oozie configuration as part of the upgrade process.
    :return:
    """
    Logger.info('Backing up Oozie configuration directory before upgrade...')
    directoryMappings = OozieUpgrade._get_directory_mappings()

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


  @staticmethod
  def restore_configuration():
    """
    Restores the configuration backups to their proper locations after an
    upgrade has completed.
    :return:
    """
    Logger.info('Restoring Oozie configuration directory after upgrade...')
    directoryMappings = OozieUpgrade._get_directory_mappings()

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


  @staticmethod
  def prepare_libext_directory():
    """
    Creates /usr/hdp/current/oozie/libext-customer and recursively sets
    777 permissions on it and its parents.
    Also, downloads jdbc driver and provides other staff
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

    # Redownload jdbc driver to a new current location
    if params.jdbc_driver_name=="com.mysql.jdbc.Driver" or \
      params.jdbc_driver_name == "com.microsoft.sqlserver.jdbc.SQLServerDriver" or \
      params.jdbc_driver_name=="oracle.jdbc.driver.OracleDriver":

      File(params.downloaded_custom_connector,
        content = DownloadSource(params.driver_curl_source))

      Execute(('cp', '--remove-destination', params.downloaded_custom_connector, params.target),
        #creates=params.target, TODO: uncomment after ranger_hive_plugin will not provide jdbc
        path=["/bin", "/usr/bin/"], sudo = True)

      File(params.target, owner = params.oozie_user, group = params.user_group)


  @staticmethod
  def prepare_warfile():
    """
    Invokes the 'prepare-war' command in Oozie in order to create the WAR.
    This method will also perform a kinit if necessary.
    :return:
    """
    import params

    # get the kerberos token if necessary to execute commands as oozie
    if params.security_enabled:
      oozie_principal_with_host = params.oozie_principal.replace("_HOST", params.hostname)
      command = format("{kinit_path_local} -kt {oozie_keytab} {oozie_principal_with_host}")
      Execute(command, user=params.oozie_user, logoutput=True)

    # prepare the oozie WAR
    command = format("{oozie_setup_sh} prepare-war {oozie_secure} -d {oozie_libext_customer_dir}")
    return_code, oozie_output = shell.call(command, user=params.oozie_user,
      logoutput=False, quiet=False)

    # set it to "" in to prevent a possible iteration issue
    if oozie_output is None:
      oozie_output = ""

    if return_code != 0 or "New Oozie WAR file with added".lower() not in oozie_output.lower():
      message = "Unexpected Oozie WAR preparation output {0}".format(oozie_output)
      Logger.error(message)
      raise Fail(message)


  def upgrade_oozie_database_and_sharelib(self, env):
    """
    Performs the creation and upload of the sharelib and the upgrade of the
    database. This method will also perform a kinit if necessary.
    It is run before the upgrade of oozie begins exactly once as part of the
    upgrade orchestration.

    Since this runs before the upgrade has occurred, it should not use any
    "current" directories since they will still be pointing to the older
    version of Oozie. Instead, it should use versioned directories to ensure
    that the commands running are from the oozie version about to be upgraded to.
    :return:
    """
    import params
    env.set_params(params)

    # get the kerberos token if necessary to execute commands as oozie
    if params.security_enabled:
      oozie_principal_with_host = params.oozie_principal.replace("_HOST", params.hostname)
      command = format("{kinit_path_local} -kt {oozie_keytab} {oozie_principal_with_host}")
      Execute(command, user=params.oozie_user, logoutput=True)

    upgrade_stack = hdp_select._get_upgrade_stack()
    if upgrade_stack is None or len(upgrade_stack) < 2 or upgrade_stack[1] is None:
      raise Fail("Unable to determine the stack that is being upgraded to or downgraded to.")

    stack_version = upgrade_stack[1]

    # upgrade oozie DB
    Logger.info('Upgrading the Oozie database...')
    database_upgrade_command = "/usr/hdp/{0}/oozie/bin/ooziedb.sh upgrade -run".format(stack_version)
    Execute(database_upgrade_command, user=params.oozie_user, logoutput=True)

    # install new sharelib to HDFS
    self.create_sharelib(env)


  def create_sharelib(self, env):
    """
    Performs the creation and upload of the sharelib.
    This method will also perform a kinit if necessary.
    It is run before the upgrade of oozie begins exactly once as part of the
    upgrade orchestration.

    Since this runs before the upgrade has occurred, it should not use any
    "current" directories since they will still be pointing to the older
    version of Oozie. Instead, it should use versioned directories to ensure
    that the commands running are from the oozie version about to be upgraded to.
    :param env:
    :return:
    """
    import params
    env.set_params(params)

    Logger.info('Creating a new sharelib and uploading it to HDFS...')

    # ensure the oozie directory exists for the sharelib
    params.HdfsResource(format("{oozie_hdfs_user_dir}/share"),
      action = "create_on_execute",
      type = "directory",
      owner = "oozie",
      group = "hadoop",
      mode = 0755,
      recursive_chmod = True)

    params.HdfsResource(None, action = "execute")

    upgrade_stack = hdp_select._get_upgrade_stack()
    if upgrade_stack is None or upgrade_stack[1] is None:
      raise Fail("Unable to determine the stack that is being upgraded to or downgraded to.")

    stack_version = upgrade_stack[1]

    # install new sharelib to HDFS
    sharelib_command = "/usr/hdp/{0}/oozie/bin/oozie-setup.sh sharelib create -fs {1}".format(
      stack_version, params.fs_root)

    Execute(sharelib_command, user=params.oozie_user, logoutput=True)


  @staticmethod
  def _get_directory_mappings():
    """
    Gets a dictionary of directory to archive name that represents the
    directories that need to be backed up and their output tarball archive targets
    :return:  the dictionary of directory to tarball mappings
    """
    import params

    # the trailing "/" is important here so as to not include the "conf" folder itself
    return { params.conf_dir + "/" : BACKUP_CONF_ARCHIVE }


if __name__ == "__main__":
  OozieUpgrade().execute()
