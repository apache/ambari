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

from resource_management.core.logger import Logger
from resource_management.core.exceptions import Fail
from resource_management.core.resources.system import Execute
from resource_management.core.resources.system import Directory
from resource_management.core.resources.system import File
from resource_management.libraries.functions import Direction
from resource_management.libraries.functions import format
from resource_management.libraries.functions import stack_select
from resource_management.libraries.functions import lzo_utils
from resource_management.libraries.functions.oozie_prepare_war import prepare_war
from resource_management.libraries.script.script import Script
from resource_management.libraries.functions import StackFeature
from resource_management.libraries.functions.stack_features import check_stack_feature

import oozie

BACKUP_TEMP_DIR = "oozie-upgrade-backup"
BACKUP_CONF_ARCHIVE = "oozie-conf-backup.tar"

class OozieUpgrade(Script):

  @staticmethod
  def prepare_libext_directory(upgrade_type=None):
    """
    Performs the following actions on libext:
      - creates <stack-root>/current/oozie/libext and recursively
      - set 777 permissions on it and its parents.
      - downloads JDBC driver JAR if needed
      - copies Falcon JAR for the Oozie WAR if needed
    """
    import params

    # some stack versions don't need the lzo compression libraries
    target_version_needs_compression_libraries = check_stack_feature(StackFeature.LZO,
      params.version_for_stack_feature_checks)

    # ensure the directory exists
    Directory(params.oozie_libext_dir, mode = 0o777)

    # get all hadooplzo* JAR files
    # <stack-selector-tool> set hadoop-client has not run yet, therefore we cannot use
    # <stack-root>/current/hadoop-client ; we must use params.version directly
    # however, this only works when upgrading beyond 2.2.0.0; don't do this
    # for downgrade to 2.2.0.0 since hadoop-lzo will not be present
    # This can also be called during a Downgrade.
    # When a version is Installed, it is responsible for downloading the hadoop-lzo packages
    # if lzo is enabled.
    if params.lzo_enabled and (params.upgrade_direction == Direction.UPGRADE or target_version_needs_compression_libraries):
      # ensure that the LZO files are installed for this version of Oozie
      lzo_utils.install_lzo_if_needed()

      hadoop_lzo_pattern = 'hadoop-lzo*.jar'
      hadoop_client_new_lib_dir = format("{stack_root}/{version}/hadoop/lib")

      files = glob.iglob(os.path.join(hadoop_client_new_lib_dir, hadoop_lzo_pattern))
      if not files:
        raise Fail("There are no files at {0} matching {1}".format(
          hadoop_client_new_lib_dir, hadoop_lzo_pattern))

      # copy files into libext
      files_copied = False
      for file in files:
        if os.path.isfile(file):
          Logger.info("Copying {0} to {1}".format(str(file), params.oozie_libext_dir))
          shutil.copy2(file, params.oozie_libext_dir)
          files_copied = True

      if not files_copied:
        raise Fail("There are no files at {0} matching {1}".format(
          hadoop_client_new_lib_dir, hadoop_lzo_pattern))

    # ExtJS is used to build a working Oozie Web UI - without it, Oozie will startup and work
    # but will not have a functioning user interface - Some stacks no longer ship ExtJS,
    # so it's optional now. On an upgrade, we should make sure that if it's not found, that's OK
    # However, if it is found on the system (from an earlier install) then it should be used
    extjs_included = check_stack_feature(StackFeature.OOZIE_EXTJS_INCLUDED, params.version_for_stack_feature_checks)

    # something like <stack-root>/current/oozie-server/libext/ext-2.2.zip
    oozie_ext_zip_target_path = os.path.join(params.oozie_libext_dir, params.ext_js_file)

    # Copy ext ZIP to libext dir
    # Default to /usr/share/$TARGETSTACK-oozie/ext-2.2.zip as the first path
    source_ext_zip_paths = oozie.get_oozie_ext_zip_source_paths(upgrade_type, params)

    found_at_least_one_oozie_ext_file = False

    # Copy the first oozie ext-2.2.zip file that is found.
    # This uses a list to handle the cases when migrating from some versions of BigInsights to HDP.
    if source_ext_zip_paths is not None:
      for source_ext_zip_path in source_ext_zip_paths:
        if os.path.isfile(source_ext_zip_path):
          found_at_least_one_oozie_ext_file = True
          Logger.info("Copying {0} to {1}".format(source_ext_zip_path, params.oozie_libext_dir))
          Execute(("cp", source_ext_zip_path, params.oozie_libext_dir), sudo=True)
          Execute(("chown", format("{oozie_user}:{user_group}"), oozie_ext_zip_target_path), sudo=True)
          File(oozie_ext_zip_target_path, mode=0o644)
          break

    # ExtJS was expected to the be on the system, but was not found
    if extjs_included and not found_at_least_one_oozie_ext_file:
      raise Fail("Unable to find any Oozie source extension files from the following paths {0}".format(source_ext_zip_paths))

    # ExtJS is not expected, so it's OK - just log a warning
    if not found_at_least_one_oozie_ext_file:
      Logger.warning("Unable to find ExtJS in any of the following paths. The Oozie UI will not be available. Source Paths: {0}".format(source_ext_zip_paths))

    # Redownload jdbc driver to a new current location
    oozie.download_database_library_if_needed()

    # get the upgrade version in the event that it's needed
    upgrade_stack = stack_select._get_upgrade_stack()
    if upgrade_stack is None or len(upgrade_stack) < 2 or upgrade_stack[1] is None:
      raise Fail("Unable to determine the stack that is being upgraded to or downgraded to.")

    stack_version = upgrade_stack[1]

    # copy the Falcon JAR if needed; falcon has not upgraded yet, so we must
    # use the versioned falcon directory
    if params.has_falcon_host:
      versioned_falcon_jar_directory = "{0}/{1}/falcon/oozie/ext/falcon-oozie-el-extension-*.jar".format(params.stack_root, stack_version)
      Logger.info("Copying {0} to {1}".format(versioned_falcon_jar_directory, params.oozie_libext_dir))

      Execute(format('{sudo} cp {versioned_falcon_jar_directory} {oozie_libext_dir}'))
      Execute(format('{sudo} chown {oozie_user}:{user_group} {oozie_libext_dir}/falcon-oozie-el-extension-*.jar'))


  @staticmethod
  def prepare_warfile():
    """
    Invokes the 'prepare-war' command in Oozie in order to create the WAR.
    The prepare-war command uses the input WAR from ${OOZIE_HOME}/oozie.war and
    outputs the prepared WAR to ${CATALINA_BASE}/webapps/oozie.war - because of this,
    both of these environment variables must point to the upgraded oozie-server path and
    not oozie-client since it was not yet updated.

    This method will also perform a kinit if necessary.
    :return:
    """
    import params

    # get the kerberos token if necessary to execute commands as oozie
    if params.security_enabled:
      oozie_principal_with_host = params.oozie_principal.replace("_HOST", params.hostname)
      command = format("{kinit_path_local} -kt {oozie_keytab} {oozie_principal_with_host}")
      Execute(command, user=params.oozie_user, logoutput=True)

    prepare_war(params)


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

    Logger.info("Will upgrade the Oozie database")

    # get the kerberos token if necessary to execute commands as oozie
    if params.security_enabled:
      oozie_principal_with_host = params.oozie_principal.replace("_HOST", params.hostname)
      command = format("{kinit_path_local} -kt {oozie_keytab} {oozie_principal_with_host}")
      Execute(command, user=params.oozie_user, logoutput=True)

    upgrade_stack = stack_select._get_upgrade_stack()
    if upgrade_stack is None or len(upgrade_stack) < 2 or upgrade_stack[1] is None:
      raise Fail("Unable to determine the stack that is being upgraded to or downgraded to.")

    stack_version = upgrade_stack[1]

    # upgrade oozie DB
    Logger.info(format('Upgrading the Oozie database, using version {stack_version}'))

    # the database upgrade requires the db driver JAR, but since we have
    # not yet run <stack-selector-tool> to upgrade the current points, we have to use
    # the versioned libext directory as the location[[-vufdtffr,
    versioned_libext_dir = "{0}/{1}/oozie/libext".format(params.stack_root, stack_version)
    oozie.download_database_library_if_needed(target_directory=versioned_libext_dir)

    database_upgrade_command = "{0}/{1}/oozie/bin/ooziedb.sh upgrade -run".format(params.stack_root, stack_version)
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
      owner = params.oozie_user,
      group = params.user_group,
      mode = 0o755,
      recursive_chmod = True)

    params.HdfsResource(None, action = "execute")

    upgrade_stack = stack_select._get_upgrade_stack()
    if upgrade_stack is None or upgrade_stack[1] is None:
      raise Fail("Unable to determine the stack that is being upgraded to or downgraded to.")

    stack_version = upgrade_stack[1]

    # install new sharelib to HDFS
    sharelib_command = "{0}/{1}/oozie/bin/oozie-setup.sh sharelib create -fs {2}".format(
      params.stack_root, stack_version, params.fs_root)

    Execute(sharelib_command, user=params.oozie_user, logoutput=True)

if __name__ == "__main__":
  OozieUpgrade().execute()
