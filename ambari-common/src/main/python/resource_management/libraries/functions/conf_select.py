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

__all__ = ["select", "create", "get_hadoop_conf_dir", "get_hadoop_dir"]

import os
import version
import hdp_select
import subprocess

from resource_management.core import shell
from resource_management.libraries.functions.format import format
from resource_management.libraries.script.script import Script
from resource_management.core.logger import Logger
from resource_management.core.resources.system import Directory
from resource_management.core.resources.system import Execute
from resource_management.core.resources.system import Link
from resource_management.libraries.functions.default import default
from resource_management.core.exceptions import Fail
from resource_management.libraries.functions.version import compare_versions, format_hdp_stack_version
from resource_management.core.shell import as_sudo


PACKAGE_DIRS = {
  "accumulo": [
    {
      "conf_dir": "/etc/accumulo/conf",
      "current_dir": "/usr/hdp/current/accumulo-client/conf"
    }
  ],
  "falcon": [
    {
      "conf_dir": "/etc/falcon/conf",
      "current_dir": "/usr/hdp/current/falcon-client/conf"
    }
  ],
  "hadoop": [
    {
      "conf_dir": "/etc/hadoop/conf",
      "current_dir": "/usr/hdp/current/hadoop-client/conf"
    }
  ],
  "hbase": [
    {
      "conf_dir": "/etc/hbase/conf",
      "current_dir": "/usr/hdp/current/hbase-client/conf"
    }
  ],
  "hive": [
    {
      "conf_dir": "/etc/hive/conf",
      "current_dir": "/usr/hdp/current/hive-client/conf"
    }
  ],
  "kafka": [
    {
      "conf_dir": "/etc/kafka/conf",
      "current_dir": "/usr/hdp/current/kafka-broker/conf"
    }
  ],
  "knox": [
    {
      "conf_dir": "/etc/knox/conf",
      "current_dir": "/usr/hdp/current/knox-server/conf"
    }
  ],
  "mahout": [
    {
      "conf_dir": "/etc/mahout/conf",
      "current_dir": "/usr/hdp/current/mahout-client/conf"
    }
  ],
  "oozie": [
    {
      "conf_dir": "/etc/oozie/conf",
      "current_dir": "/usr/hdp/current/oozie-client/conf"
    }
  ],
  "phoenix": [
    {
      "conf_dir": "/etc/phoenix/conf",
      "current_dir": "/usr/hdp/current/phoenix-client/conf"
    }
  ],
  "ranger-admin": [
    {
      "conf_dir": "/etc/ranger/admin/conf",
      "current_dir": "/usr/hdp/current/ranger-admin/conf"
    }
  ],
  "ranger-kms": [
    {
      "conf_dir": "/etc/ranger/kms/conf",
      "current_dir": "/usr/hdp/current/ranger-kms/conf"
    }
  ],
  "ranger-usersync": [
    {
      "conf_dir": "/etc/ranger/usersync/conf",
      "current_dir": "/usr/hdp/current/ranger-usersync/conf"
    }
  ],
  "slider": [
    {
      "conf_dir": "/etc/slider/conf",
      "current_dir": "/usr/hdp/current/slider-client/conf"
    }
  ],
  "spark": [
    {
      "conf_dir": "/etc/spark/conf",
      "current_dir": "/usr/hdp/current/spark-client/conf"
    }
  ],
  "sqoop": [
    {
      "conf_dir": "/etc/sqoop/conf",
      "current_dir": "/usr/hdp/current/sqoop-client/conf"
    }
  ],
  "storm": [
    {
      "conf_dir": "/etc/storm/conf",
      "current_dir": "/usr/hdp/current/storm-client/conf"
    }
  ],
  "tez": [
    {
      "conf_dir": "/etc/tez/conf",
      "current_dir": "/usr/hdp/current/tez-client/conf"
    }
  ],
  "zookeeper": [
    {
      "conf_dir": "/etc/zookeeper/conf",
      "current_dir": "/usr/hdp/current/zookeeper-client/conf"
    }
  ],
  "pig": [
    {
      "conf_dir": "/etc/pig/conf",
      "current_dir": "/usr/hdp/current/pig-client/conf"
    }
  ],
  "flume": [
    {
      "conf_dir": "/etc/flume/conf",
      "current_dir": "/usr/hdp/current/flume-server/conf"
    }
  ],
  "storm-slider-client": [
    {
      "conf_dir": "/etc/storm-slider-client/conf",
      "current_dir": "/usr/hdp/current/storm-slider-client/conf"
    }
  ],
  "hive-hcatalog": [
    {
      "conf_dir": "/etc/hive-webhcat/conf",
      "prefix": "/etc/hive-webhcat",
      "current_dir": "/usr/hdp/current/hive-webhcat/etc/webhcat"
    },
    {
      "conf_dir": "/etc/hive-hcatalog/conf",
      "prefix": "/etc/hive-hcatalog",
      "current_dir": "/usr/hdp/current/hive-webhcat/etc/hcatalog"
    }
  ]
}

DIRECTORY_TYPE_BACKUP = "backup"
DIRECTORY_TYPE_CURRENT = "current"

def get_cmd(command, package, version):
  return ('conf-select', command, '--package', package, '--stack-version', version, '--conf-version', '0')


def _valid(stack_name, package, ver):
  if stack_name != "HDP":
    return False

  if version.compare_versions(version.format_hdp_stack_version(ver), "2.3.0.0") < 0:
    return False

  return True


def create(stack_name, package, version, dry_run = False):
  """
  Creates a config version for the specified package
  :param stack_name: the name of the stack
  :param package: the name of the package, as-used by conf-select
  :param version: the version number to create
  :param dry_run: False to create the versioned config directory, True to only return what would be created
  :return List of directories created
  """
  Logger.info("Checking if need to create versioned conf dir /etc/{0}/{1}/0".format(package, version))
  if not _valid(stack_name, package, version):
    Logger.info("Will not create it since parameters are not valid.")
    return []

  command = "dry-run-create" if dry_run else "create-conf-dir"

  code, stdout, stderr = shell.call(get_cmd(command, package, version), logoutput=False, quiet=False, sudo=True, stderr = subprocess.PIPE)

  # conf-select can set more than one directory
  # per package, so return that list, especially for dry_run
  # > conf-select dry-run-create --package hive-hcatalog --stack-version 2.4.0.0-169 0
  # /etc/hive-webhcat/2.4.0.0-169/0
  # /etc/hive-hcatalog/2.4.0.0-169/0
  created_directories = []
  if 0 == code and stdout is not None: # just be sure we have a stdout
    for line in stdout.splitlines():
      created_directories.append(line.rstrip('\n'))

  # if directories were created, then do some post-processing
  if not code and stdout and not dry_run:
    # take care of permissions if directories were created
    for directory in created_directories:
      Directory(directory, mode=0755, cd_access='a', recursive=True)

    # seed the new directories with configurations from the old (current) directories
    _seed_new_configuration_directories(package, created_directories)

  return created_directories


def select(stack_name, package, version, try_create=True, ignore_errors=False):
  """
  Selects a config version for the specified package. If this detects that
  the stack supports configuration versioning but /etc/<component>/conf is a
  directory, then it will attempt to bootstrap the conf.backup directory and change
  /etc/<component>/conf into a symlink.

  :param stack_name: the name of the stack
  :param package: the name of the package, as-used by conf-select
  :param version: the version number to create
  :param try_create: optional argument to attempt to create the directory before setting it
  :param ignore_errors: optional argument to ignore any error and simply log a warning
  """
  try:
    # do nothing if the stack does not support versioned configurations
    if not _valid(stack_name, package, version):
      return

    if try_create:
      create(stack_name, package, version)

    shell.checked_call(get_cmd("set-conf-dir", package, version), logoutput=False, quiet=False, sudo=True)

    # for consistency sake, we must ensure that the /etc/<component>/conf symlink exists and
    # points to /usr/hdp/current/<component>/conf - this is because some people still prefer to
    # use /etc/<component>/conf even though /usr/hdp is the "future"
    if package in PACKAGE_DIRS:
      Logger.info("Ensuring that {0} has the correct symlink structure".format(package))

      directory_list = PACKAGE_DIRS[package]
      for directory_structure in directory_list:
        conf_dir = directory_structure["conf_dir"]
        current_dir = directory_structure["current_dir"]

        # if /etc/<component>/conf is missing or is not a symlink
        if not os.path.islink(conf_dir):
          # if /etc/<component>/conf is not a link and it exists, convert it to a symlink
          if os.path.exists(conf_dir):
            parent_directory = os.path.dirname(conf_dir)
            conf_backup_dir = os.path.join(parent_directory, "conf.backup")

            # create conf.backup and copy files to it (if it doesn't exist)
            Execute(("cp", "-R", "-p", conf_dir, conf_backup_dir),
              not_if = format("test -e {conf_backup_dir}"), sudo = True)

            # delete the old /etc/<component>/conf directory and link to the backup
            Directory(conf_dir, action="delete")
            Link(conf_dir, to = conf_backup_dir)
          else:
            # missing entirely
            # /etc/<component>/conf -> <stack-root>/current/<component>/conf
            Link(conf_dir, to = current_dir)

  except Exception, exception:
    if ignore_errors is True:
      Logger.warning("Could not select the directory for package {0}. Error: {1}".format(package,
        str(exception)))
    else:
      raise



def get_hadoop_conf_dir(force_latest_on_upgrade=False):
  """
  Gets the shared hadoop conf directory using:
  1.  Start with /etc/hadoop/conf
  2.  When the stack is greater than HDP-2.2, use /usr/hdp/current/hadoop-client/conf
  3.  Only when doing a RU and HDP-2.3 or higher, use the value as computed
      by conf-select.  This is in the form /usr/hdp/VERSION/hadoop/conf to make sure
      the configs are written in the correct place. However, if the component itself has
      not yet been upgraded, it should use the hadoop configs from the prior version.
      This will perform an hdp-select status to determine which version to use.
  :param force_latest_on_upgrade:  if True, then force the returned path to always
  be that of the upgrade target version, even if hdp-select has not been called. This
  is primarily used by hooks like before-ANY to ensure that hadoop environment
  configurations are written to the correct location since they are written out
  before the hdp-select/conf-select would have been called.
  """
  hadoop_conf_dir = "/etc/hadoop/conf"
  stack_name = None
  version = None
  allow_setting_conf_select_symlink = False

  if not Script.in_stack_upgrade():
    # During normal operation, the HDP stack must be 2.3 or higher
    if Script.is_hdp_stack_greater_or_equal("2.2"):
      hadoop_conf_dir = "/usr/hdp/current/hadoop-client/conf"

    if Script.is_hdp_stack_greater_or_equal("2.3"):
      hadoop_conf_dir = "/usr/hdp/current/hadoop-client/conf"
      stack_name = default("/hostLevelParams/stack_name", None)
      version = default("/commandParams/version", None)

      if stack_name and version:
        version = str(version)
        allow_setting_conf_select_symlink = True
  else:
    # During an upgrade/downgrade, which can be a Rolling or Express Upgrade, need to calculate it based on the version
    '''
    Whenever upgrading to HDP 2.2, or downgrading back to 2.2, need to use /etc/hadoop/conf
    Whenever upgrading to HDP 2.3, or downgrading back to 2.3, need to use a versioned hadoop conf dir

    Type__|_Source_|_Target_|_Direction_____________|_Comment_____________________________________________________________
    Normal|        | 2.2    |                       | Use /etc/hadoop/conf
    Normal|        | 2.3    |                       | Use /etc/hadoop/conf, which should be a symlink to /usr/hdp/current/hadoop-client/conf
    EU    | 2.1    | 2.3    | Upgrade               | Use versioned /usr/hdp/current/hadoop-client/conf
          |        |        | No Downgrade Allowed  | Invalid
    EU/RU | 2.2    | 2.2.*  | Any                   | Use /usr/hdp/current/hadoop-client/conf
    EU/RU | 2.2    | 2.3    | Upgrade               | Use /usr/hdp/$version/hadoop/conf, which should be a symlink destination
          |        |        | Downgrade             | Use /usr/hdp/current/hadoop-client/conf
    EU/RU | 2.3    | 2.3.*  | Any                   | Use /usr/hdp/$version/hadoop/conf, which should be a symlink destination
    '''

    # The method "is_hdp_stack_greater_or_equal" uses "stack_version" which is the desired stack, e.g., 2.2 or 2.3
    # In an RU, it is always the desired stack, and doesn't change even during the Downgrade!
    # In an RU Downgrade from HDP 2.3 to 2.2, the first thing we do is 
    # rm /etc/[component]/conf and then mv /etc/[component]/conf.backup /etc/[component]/conf
    if Script.is_hdp_stack_greater_or_equal("2.2"):
      hadoop_conf_dir = "/usr/hdp/current/hadoop-client/conf"

      # This contains the "version", including the build number, that is actually used during a stack upgrade and
      # is the version upgrading/downgrading to.
      stack_info = hdp_select._get_upgrade_stack()

      if stack_info is not None:
        stack_name = stack_info[0]
        version = stack_info[1]
      else:
        raise Fail("Unable to get parameter 'version'")
      
      Logger.info("In the middle of a stack upgrade/downgrade for Stack {0} and destination version {1}, determining which hadoop conf dir to use.".format(stack_name, version))
      # This is the version either upgrading or downgrading to.
      if compare_versions(format_hdp_stack_version(version), "2.3.0.0") >= 0:
        # Determine if hdp-select has been run and if not, then use the current
        # hdp version until this component is upgraded.
        if not force_latest_on_upgrade:
          current_hdp_version = hdp_select.get_role_component_current_hdp_version()
          if current_hdp_version is not None and version != current_hdp_version:
            version = current_hdp_version
            Logger.info("hdp-select has not yet been called to update the symlink for this component, keep using version {0}".format(current_hdp_version))

        # Only change the hadoop_conf_dir path, don't conf-select this older version
        hadoop_conf_dir = "/usr/hdp/{0}/hadoop/conf".format(version)
        Logger.info("Hadoop conf dir: {0}".format(hadoop_conf_dir))

        allow_setting_conf_select_symlink = True

  if allow_setting_conf_select_symlink:
    # If not in the middle of an upgrade and on HDP 2.3 or higher, or if
    # upgrading stack to version 2.3.0.0 or higher (which may be upgrade or downgrade), then consider setting the
    # symlink for /etc/hadoop/conf.
    # If a host does not have any HDFS or YARN components (e.g., only ZK), then it will not contain /etc/hadoop/conf
    # Therefore, any calls to conf-select will fail.
    # For that reason, if the hadoop conf directory exists, then make sure it is set.
    if os.path.exists(hadoop_conf_dir):
      Logger.info("The hadoop conf dir {0} exists, will call conf-select on it for version {1}".format(hadoop_conf_dir, version))
      select(stack_name, "hadoop", version)

  Logger.info("Using hadoop conf dir: {0}".format(hadoop_conf_dir))
  return hadoop_conf_dir


def convert_conf_directories_to_symlinks(package, version, dirs, skip_existing_links=True,
    link_to=DIRECTORY_TYPE_CURRENT):
  """
  Assumes HDP 2.3+, moves around directories and creates the conf symlink for the given package.
  If the package does not exist, then no work is performed.

  - Creates a /etc/<component>/conf.backup directory
  - Copies all configs from /etc/<component>/conf to conf.backup
  - Removes /etc/<component>/conf
  - Creates /etc/<component>/<version>/0 via conf-select
  - /usr/hdp/current/<component>-client/conf -> /etc/<component>/<version>/0 via conf-select
  - Links /etc/<component>/conf to <something> depending on function paramter
  -- /etc/<component>/conf -> /usr/hdp/current/[component]-client/conf (usually)
  -- /etc/<component>/conf -> /etc/<component>/conf.backup (only when supporting < HDP 2.3)

  :param package: the package to create symlinks for (zookeeper, falcon, etc)
  :param version: the version number to use with conf-select (2.3.0.0-1234)
  :param dirs: the directories associated with the package (from PACKAGE_DIRS)
  :param skip_existing_links: True to not do any work if already a symlink
  :param link_to: link to "current" or "backup"
  """
  # lack of enums makes this possible - we need to know what to link to
  if link_to not in [DIRECTORY_TYPE_CURRENT, DIRECTORY_TYPE_BACKUP]:
    raise Fail("Unsupported 'link_to' argument. Could not link package {0}".format(package))

  bad_dirs = []
  for dir_def in dirs:
    if not os.path.exists(dir_def['conf_dir']):
      bad_dirs.append(dir_def['conf_dir'])

  if len(bad_dirs) > 0:
    Logger.info("Skipping {0} as it does not exist.".format(",".join(bad_dirs)))
    return

  # existing links should be skipped since we assume there's no work to do
  # they should be checked against the correct target though
  if skip_existing_links:
    bad_dirs = []
    for dir_def in dirs:
      # check if conf is a link already
      old_conf = dir_def['conf_dir']
      if os.path.islink(old_conf):
        # it's already a link; make sure it's a link to where we want it
        if link_to == DIRECTORY_TYPE_BACKUP:
          target_conf_dir = _get_backup_conf_directory(old_conf)
        else:
          target_conf_dir = dir_def['current_dir']

        # the link isn't to the right spot; re-link it
        if os.readlink(old_conf) != target_conf_dir:
          Logger.info("Re-linking symlink {0} to {1}".format(old_conf, target_conf_dir))

          Link(old_conf, action = "delete")
          Link(old_conf, to = target_conf_dir)
        else:
          Logger.info("{0} is already linked to {1}".format(old_conf, os.path.realpath(old_conf)))

        bad_dirs.append(old_conf)

  if len(bad_dirs) > 0:
    return

  # make backup dir and copy everything in case configure() was called after install()
  backup_dir = None
  for dir_def in dirs:
    old_conf = dir_def['conf_dir']
    backup_dir = _get_backup_conf_directory(old_conf)
    Logger.info("Backing up {0} to {1} if destination doesn't exist already.".format(old_conf, backup_dir))
    Execute(("cp", "-R", "-p", old_conf, backup_dir),
      not_if = format("test -e {backup_dir}"), sudo = True)

  # we're already in the HDP stack
  # Create the versioned /etc/[component]/[version]/0 folder.
  # The component must be installed on the host.
  versioned_confs = create("HDP", package, version, dry_run = True)

  Logger.info("Package {0} will have new conf directories: {1}".format(package, ", ".join(versioned_confs)))

  need_dirs = []
  for d in versioned_confs:
    if not os.path.exists(d):
      need_dirs.append(d)

  if len(need_dirs) > 0:
    create("HDP", package, version)

    # find the matching definition and back it up (not the most efficient way) ONLY if there is more than one directory
    if len(dirs) > 1:
      for need_dir in need_dirs:
        for dir_def in dirs:
          if 'prefix' in dir_def and need_dir.startswith(dir_def['prefix']):
            old_conf = dir_def['conf_dir']
            versioned_conf = need_dir
            Execute(as_sudo(["cp", "-R", "-p", os.path.join(old_conf, "*"), versioned_conf], auto_escape=False),
              only_if = format("ls -d {old_conf}/*"))
    elif 1 == len(dirs) and 1 == len(need_dirs):
      old_conf = dirs[0]['conf_dir']
      versioned_conf = need_dirs[0]
      Execute(as_sudo(["cp", "-R", "-p", os.path.join(old_conf, "*"), versioned_conf], auto_escape=False),
        only_if = format("ls -d {old_conf}/*"))


  # /usr/hdp/current/[component] is already set to to the correct version, e.g., /usr/hdp/[version]/[component]
  
  select("HDP", package, version, ignore_errors = True)

  # Symlink /etc/[component]/conf to /etc/[component]/conf.backup
  try:
    # No more references to /etc/[component]/conf
    for dir_def in dirs:
      # E.g., /etc/[component]/conf
      new_symlink = dir_def['conf_dir']

      # Delete the existing directory/link so that linking will work
      if not os.path.islink(new_symlink):
        Directory(new_symlink, action = "delete")
      else:
        Link(new_symlink, action = "delete")

      # link /etc/[component]/conf -> /etc/[component]/conf.backup
      # or
      # link /etc/[component]/conf -> <stack-root>/current/[component]-client/conf
      if link_to == DIRECTORY_TYPE_BACKUP:
        Link(new_symlink, to = backup_dir)
      else:
        Link(new_symlink, to = dir_def['current_dir'])
  except Exception, e:
    Logger.warning("Could not change symlink for package {0} to point to {1} directory. Error: {2}".format(package, link_to, e))


def _seed_new_configuration_directories(package, created_directories):
  """
  Copies any files from the "current" configuration directory to the directories which were
  newly created with conf-select. This function helps ensure that files which are not tracked
  by Ambari will be available after performing a stack upgrade. Although old configurations
  will be copied as well, they will be overwritten when the components are writing out their
  configs after upgrade during their restart.

  This function will catch all errors, logging them, but not raising an exception. This is to
  prevent problems here from stopping and otherwise healthy upgrade.

  :param package: the conf-select package name
  :param created_directories: a list of directories that conf-select said it created
  :return: None
  """
  if package not in PACKAGE_DIRS:
    Logger.warning("Unable to seed newly created configuration directories for {0} because it is an unknown component".format(package))
    return

  # seed the directories with any existing configurations
  # this allows files which are not tracked by Ambari to be available after an upgrade
  Logger.info("Seeding versioned configuration directories for {0}".format(package))
  expected_directories = PACKAGE_DIRS[package]

  try:
    # if the expected directories don't match those created, we can't seed them
    if len(created_directories) != len(expected_directories):
      Logger.warning("The known configuration directories for {0} do not match those created by conf-select: {1}".format(
        package, str(created_directories)))

      return

    # short circuit for a simple 1:1 mapping
    if len(expected_directories) == 1:
      # /usr/hdp/current/component/conf
      # the current directory is the source of the seeded configurations;
      source_seed_directory = expected_directories[0]["current_dir"]
      target_seed_directory = created_directories[0]
      _copy_configurations(source_seed_directory, target_seed_directory)
    else:
      for created_directory in created_directories:
        for expected_directory_structure in expected_directories:
          prefix = expected_directory_structure.get("prefix", None)
          if prefix is not None and created_directory.startswith(prefix):
            source_seed_directory = expected_directory_structure["current_dir"]
            target_seed_directory = created_directory
            _copy_configurations(source_seed_directory, target_seed_directory)

  except Exception, e:
    Logger.warning("Unable to seed new configuration directories for {0}. {1}".format(package, str(e)))


def _copy_configurations(source_directory, target_directory):
  """
  Copies from the source directory to the target directory. If the source directory is a symlink
  then it will be followed (deferenced) but any other symlinks found to copy will not be. This
  will ensure that if the configuration directory itself is a symlink, then it's contents will be
  copied, preserving and children found which are also symlinks.

  :param source_directory:  the source directory to copy from
  :param target_directory:  the target directory to copy to
  :return: None
  """
  # append trailing slash so the cp command works correctly WRT recursion and symlinks
  source_directory = os.path.join(source_directory, "*")
  Execute(as_sudo(["cp", "-R", "-p", "-v", source_directory, target_directory], auto_escape = False),
    logoutput = True)

def _get_backup_conf_directory(old_conf):
  """
  Calculates the conf.backup absolute directory given the /etc/<component>/conf location.
  :param old_conf:  the old conf directory (ie /etc/<component>/conf)
  :return:  the conf.backup absolute directory
  """
  old_parent = os.path.abspath(os.path.join(old_conf, os.pardir))
  backup_dir = os.path.join(old_parent, "conf.backup")
  return backup_dir
