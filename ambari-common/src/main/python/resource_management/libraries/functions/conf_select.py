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

__all__ = ["select", "create", "get_hadoop_conf_dir", "get_hadoop_dir", "get_package_dirs"]

# Python Imports
import copy
import os
import subprocess

# Local Imports
import version
import stack_select
from resource_management.core import shell
from resource_management.libraries.functions.format import format
from resource_management.libraries.script.script import Script
from resource_management.core.logger import Logger
from resource_management.core.resources.system import Directory
from resource_management.core.resources.system import Execute
from resource_management.core.resources.system import Link
from resource_management.libraries.functions.default import default
from resource_management.libraries.functions import stack_tools
from resource_management.core.exceptions import Fail
from resource_management.core.shell import as_sudo
from resource_management.libraries.functions.stack_features import check_stack_feature
from resource_management.libraries.functions import StackFeature

STACK_ROOT_PATTERN = "{{ stack_root }}"

_PACKAGE_DIRS = {
  "atlas": [
    {
      "conf_dir": "/etc/atlas/conf",
      "current_dir": "{0}/current/atlas-client/conf".format(STACK_ROOT_PATTERN)
    }
  ],
  "accumulo": [
    {
      "conf_dir": "/etc/accumulo/conf",
      "current_dir": "{0}/current/accumulo-client/conf".format(STACK_ROOT_PATTERN)
    }
  ],
  "falcon": [
    {
      "conf_dir": "/etc/falcon/conf",
      "current_dir": "{0}/current/falcon-client/conf".format(STACK_ROOT_PATTERN)
    }
  ],
  "hadoop": [
    {
      "conf_dir": "/etc/hadoop/conf",
      "current_dir": "{0}/current/hadoop-client/conf".format(STACK_ROOT_PATTERN)
    }
  ],
  "hbase": [
    {
      "conf_dir": "/etc/hbase/conf",
      "current_dir": "{0}/current/hbase-client/conf".format(STACK_ROOT_PATTERN)
    }
  ],
  "hive": [
    {
      "conf_dir": "/etc/hive/conf",
      "current_dir": "{0}/current/hive-client/conf".format(STACK_ROOT_PATTERN)
    }
  ],
  "hive2": [
    {
      "conf_dir": "/etc/hive2/conf",
      "current_dir": "{0}/current/hive-server2-hive2/conf".format(STACK_ROOT_PATTERN)
    }
  ],
  "kafka": [
    {
      "conf_dir": "/etc/kafka/conf",
      "current_dir": "{0}/current/kafka-broker/conf".format(STACK_ROOT_PATTERN)
    }
  ],
  "knox": [
    {
      "conf_dir": "/etc/knox/conf",
      "current_dir": "{0}/current/knox-server/conf".format(STACK_ROOT_PATTERN)
    }
  ],
  "mahout": [
    {
      "conf_dir": "/etc/mahout/conf",
      "current_dir": "{0}/current/mahout-client/conf".format(STACK_ROOT_PATTERN)
    }
  ],
  "nifi": [
    {
      "conf_dir": "/etc/nifi/conf",
      "current_dir": "{0}/current/nifi/conf".format(STACK_ROOT_PATTERN)
    }
  ],
  "oozie": [
    {
      "conf_dir": "/etc/oozie/conf",
      "current_dir": "{0}/current/oozie-client/conf".format(STACK_ROOT_PATTERN)
    }
  ],
  "phoenix": [
    {
      "conf_dir": "/etc/phoenix/conf",
      "current_dir": "{0}/current/phoenix-client/conf".format(STACK_ROOT_PATTERN)
    }
  ],
  "ranger-admin": [
    {
      "conf_dir": "/etc/ranger/admin/conf",
      "current_dir": "{0}/current/ranger-admin/conf".format(STACK_ROOT_PATTERN)
    }
  ],
  "ranger-tagsync": [
    {
      "conf_dir": "/etc/ranger/tagsync/conf",
      "current_dir": "{0}/current/ranger-tagsync/conf".format(STACK_ROOT_PATTERN)
    }
  ],
  "ranger-kms": [
    {
      "conf_dir": "/etc/ranger/kms/conf",
      "current_dir": "{0}/current/ranger-kms/conf".format(STACK_ROOT_PATTERN)
    }
  ],
  "ranger-usersync": [
    {
      "conf_dir": "/etc/ranger/usersync/conf",
      "current_dir": "{0}/current/ranger-usersync/conf".format(STACK_ROOT_PATTERN)
    }
  ],
  "slider": [
    {
      "conf_dir": "/etc/slider/conf",
      "current_dir": "{0}/current/slider-client/conf".format(STACK_ROOT_PATTERN)
    }
  ],
  "spark": [
    {
      "conf_dir": "/etc/spark/conf",
      "current_dir": "{0}/current/spark-client/conf".format(STACK_ROOT_PATTERN)
    }
  ],
  "zeppelin": [
    {
      "conf_dir": "/etc/zeppelin/conf",
      "current_dir": "{0}/current/zeppelin-server/conf".format(STACK_ROOT_PATTERN)
    }
  ],
  "spark2": [
    {
      "conf_dir": "/etc/spark2/conf",
      "current_dir": "{0}/current/spark2-client/conf".format(STACK_ROOT_PATTERN)
    }
  ],
  "sqoop": [
    {
      "conf_dir": "/etc/sqoop/conf",
      "current_dir": "{0}/current/sqoop-client/conf".format(STACK_ROOT_PATTERN)
    }
  ],
  "storm": [
    {
      "conf_dir": "/etc/storm/conf",
      "current_dir": "{0}/current/storm-client/conf".format(STACK_ROOT_PATTERN)
    }
  ],
  "tez": [
    {
      "conf_dir": "/etc/tez/conf",
      "current_dir": "{0}/current/tez-client/conf".format(STACK_ROOT_PATTERN)
    }
  ],
  "zookeeper": [
    {
      "conf_dir": "/etc/zookeeper/conf",
      "current_dir": "{0}/current/zookeeper-client/conf".format(STACK_ROOT_PATTERN)
    }
  ],
  "pig": [
    {
      "conf_dir": "/etc/pig/conf",
      "current_dir": "{0}/current/pig-client/conf".format(STACK_ROOT_PATTERN)
    }
  ],
  "flume": [
    {
      "conf_dir": "/etc/flume/conf",
      "current_dir": "{0}/current/flume-server/conf".format(STACK_ROOT_PATTERN)
    }
  ],
  "storm-slider-client": [
    {
      "conf_dir": "/etc/storm-slider-client/conf",
      "current_dir": "{0}/current/storm-slider-client/conf".format(STACK_ROOT_PATTERN)
    }
  ],
  "hive-hcatalog": [
    {
      "conf_dir": "/etc/hive-webhcat/conf",
      "prefix": "/etc/hive-webhcat",
      "current_dir": "{0}/current/hive-webhcat/etc/webhcat".format(STACK_ROOT_PATTERN)
    },
    {
      "conf_dir": "/etc/hive-hcatalog/conf",
      "prefix": "/etc/hive-hcatalog",
      "current_dir": "{0}/current/hive-webhcat/etc/hcatalog".format(STACK_ROOT_PATTERN)
    }
  ]
}

DIRECTORY_TYPE_BACKUP = "backup"
DIRECTORY_TYPE_CURRENT = "current"

def _get_cmd(command, package, version):
  conf_selector_path = stack_tools.get_stack_tool_path(stack_tools.CONF_SELECTOR_NAME)
  return ('ambari-python-wrap', conf_selector_path, command, '--package', package, '--stack-version', version, '--conf-version', '0')

def _valid(stack_name, package, ver):
  return (ver and check_stack_feature(StackFeature.CONFIG_VERSIONING, ver))

def get_package_dirs():
  """
  Get package dir mappings
  :return:
  """
  stack_root = Script.get_stack_root()
  package_dirs = copy.deepcopy(_PACKAGE_DIRS)
  for package_name, directories in package_dirs.iteritems():
    for dir in directories:
      current_dir = dir['current_dir']
      current_dir = current_dir.replace(STACK_ROOT_PATTERN, stack_root)
      dir['current_dir'] = current_dir
  return package_dirs

def create(stack_name, package, version, dry_run = False):
  """
  Creates a config version for the specified package
  :param stack_name: the name of the stack
  :param package: the name of the package, as-used by <conf-selector-tool>
  :param version: the version number to create
  :param dry_run: False to create the versioned config directory, True to only return what would be created
  :return List of directories created
  """
  Logger.info("Checking if need to create versioned conf dir /etc/{0}/{1}/0".format(package, version))
  if not _valid(stack_name, package, version):
    Logger.info("Will not create it since parameters are not valid.")
    return []

  command = "dry-run-create" if dry_run else "create-conf-dir"

  code, stdout, stderr = shell.call(_get_cmd(command, package, version), logoutput=False, quiet=False, sudo=True, stderr = subprocess.PIPE)

  # <conf-selector-tool> can set more than one directory
  # per package, so return that list, especially for dry_run
  # > <conf-selector-tool> dry-run-create --package hive-hcatalog --stack-version 2.4.0.0-169 0
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
      Directory(directory, mode=0755, cd_access='a', create_parents=True)

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
  :param package: the name of the package, as-used by <conf-selector-tool>
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

    shell.checked_call(_get_cmd("set-conf-dir", package, version), logoutput=False, quiet=False, sudo=True)

    # for consistency sake, we must ensure that the /etc/<component>/conf symlink exists and
    # points to <stack-root>/current/<component>/conf - this is because some people still prefer to
    # use /etc/<component>/conf even though <stack-root> is the "future"
    package_dirs = get_package_dirs()
    if package in package_dirs:
      Logger.info("Ensuring that {0} has the correct symlink structure".format(package))

      directory_list = package_dirs[package]
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
            if package in ["atlas", ]:
              #HACK for Atlas
              '''
              In the case of Atlas, the Hive RPM installs /usr/$stack/$version/atlas with some partial packages that
              contain Hive hooks, while the Atlas RPM is responsible for installing the full content.

              If the user does not have Atlas currently installed on their stack, then /usr/$stack/current/atlas-client
              will be a broken symlink, and we should not create the
              symlink /etc/atlas/conf -> /usr/$stack/current/atlas-client/conf .
              If we mistakenly create this symlink, then when the user performs an EU/RU and then adds Atlas service
              then the Atlas RPM will not be able to copy its artifacts into /etc/atlas/conf directory and therefore
              prevent Ambari from by copying those unmanaged contents into /etc/atlas/$version/0
              '''
              component_list = default("/localComponents", [])
              if "ATLAS_SERVER" in component_list or "ATLAS_CLIENT" in component_list:
                Logger.info("Atlas is installed on this host.")
                parent_dir = os.path.dirname(current_dir)
                if os.path.exists(parent_dir):
                  Link(conf_dir, to=current_dir)
                else:
                  Logger.info("Will not create symlink from {0} to {1} because the destination's parent dir does not exist.".format(conf_dir, current_dir))
              else:
                Logger.info("Will not create symlink from {0} to {1} because Atlas is not installed on this host.".format(conf_dir, current_dir))
            else:
              # Normal path for other packages
              Link(conf_dir, to=current_dir)

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
  2.  When the stack is greater than HDP-2.2, use <stack-root>/current/hadoop-client/conf
  3.  Only when doing a RU and HDP-2.3 or higher, use the value as computed
      by <conf-selector-tool>.  This is in the form <stack-root>/VERSION/hadoop/conf to make sure
      the configs are written in the correct place. However, if the component itself has
      not yet been upgraded, it should use the hadoop configs from the prior version.
      This will perform an <stack-selector-tool> status to determine which version to use.
  :param force_latest_on_upgrade:  if True, then force the returned path to always
  be that of the upgrade target version, even if <stack-selector-tool> has not been called. This
  is primarily used by hooks like before-ANY to ensure that hadoop environment
  configurations are written to the correct location since they are written out
  before the <stack-selector-tool>/<conf-selector-tool> would have been called.
  """
  hadoop_conf_dir = "/etc/hadoop/conf"
  stack_name = None
  stack_root = Script.get_stack_root()
  stack_version = Script.get_stack_version()
  version = None
  allow_setting_conf_select_symlink = False

  if not Script.in_stack_upgrade():
    # During normal operation, the HDP stack must be 2.3 or higher
    if stack_version and check_stack_feature(StackFeature.ROLLING_UPGRADE, stack_version):
      hadoop_conf_dir = os.path.join(stack_root, "current", "hadoop-client", "conf")

    if stack_version and check_stack_feature(StackFeature.CONFIG_VERSIONING, stack_version):
      hadoop_conf_dir = os.path.join(stack_root, "current", "hadoop-client", "conf")
      stack_name = default("/hostLevelParams/stack_name", None)
      version = default("/commandParams/version", None)

      if not os.path.islink(hadoop_conf_dir) and stack_name and version:
        version = str(version)
        allow_setting_conf_select_symlink = True
  else:
    # During an upgrade/downgrade, which can be a Rolling or Express Upgrade, need to calculate it based on the version
    '''
    Whenever upgrading to HDP 2.2, or downgrading back to 2.2, need to use /etc/hadoop/conf
    Whenever upgrading to HDP 2.3, or downgrading back to 2.3, need to use a versioned hadoop conf dir

    Type__|_Source_|_Target_|_Direction_____________|_Comment_____________________________________________________________
    Normal|        | 2.2    |                       | Use /etc/hadoop/conf
    Normal|        | 2.3    |                       | Use /etc/hadoop/conf, which should be a symlink to <stack-root>/current/hadoop-client/conf
    EU    | 2.1    | 2.3    | Upgrade               | Use versioned <stack-root>/current/hadoop-client/conf
          |        |        | No Downgrade Allowed  | Invalid
    EU/RU | 2.2    | 2.2.*  | Any                   | Use <stack-root>/current/hadoop-client/conf
    EU/RU | 2.2    | 2.3    | Upgrade               | Use <stack-root>/$version/hadoop/conf, which should be a symlink destination
          |        |        | Downgrade             | Use <stack-root>/current/hadoop-client/conf
    EU/RU | 2.3    | 2.3.*  | Any                   | Use <stack-root>/$version/hadoop/conf, which should be a symlink destination
    '''

    # The "stack_version" is the desired stack, e.g., 2.2 or 2.3
    # In an RU, it is always the desired stack, and doesn't change even during the Downgrade!
    # In an RU Downgrade from HDP 2.3 to 2.2, the first thing we do is 
    # rm /etc/[component]/conf and then mv /etc/[component]/conf.backup /etc/[component]/conf
    if stack_version and check_stack_feature(StackFeature.ROLLING_UPGRADE, stack_version):
      hadoop_conf_dir = os.path.join(stack_root, "current", "hadoop-client", "conf")

      # This contains the "version", including the build number, that is actually used during a stack upgrade and
      # is the version upgrading/downgrading to.
      stack_info = stack_select._get_upgrade_stack()

      if stack_info is not None:
        stack_name = stack_info[0]
        version = stack_info[1]
      else:
        raise Fail("Unable to get parameter 'version'")
      
      Logger.info("In the middle of a stack upgrade/downgrade for Stack {0} and destination version {1}, determining which hadoop conf dir to use.".format(stack_name, version))
      # This is the version either upgrading or downgrading to.
      if version and check_stack_feature(StackFeature.CONFIG_VERSIONING, version):
        # Determine if <stack-selector-tool> has been run and if not, then use the current
        # hdp version until this component is upgraded.
        if not force_latest_on_upgrade:
          current_stack_version = stack_select.get_role_component_current_stack_version()
          if current_stack_version is not None and version != current_stack_version:
            version = current_stack_version
            stack_selector_name = stack_tools.get_stack_tool_name(stack_tools.STACK_SELECTOR_NAME)
            Logger.info("{0} has not yet been called to update the symlink for this component, "
                        "keep using version {1}".format(stack_selector_name, current_stack_version))

        # Only change the hadoop_conf_dir path, don't <conf-selector-tool> this older version
        hadoop_conf_dir = os.path.join(stack_root, version, "hadoop", "conf")
        Logger.info("Hadoop conf dir: {0}".format(hadoop_conf_dir))

        allow_setting_conf_select_symlink = True

  if allow_setting_conf_select_symlink:
    # If not in the middle of an upgrade and on HDP 2.3 or higher, or if
    # upgrading stack to version 2.3.0.0 or higher (which may be upgrade or downgrade), then consider setting the
    # symlink for /etc/hadoop/conf.
    # If a host does not have any HDFS or YARN components (e.g., only ZK), then it will not contain /etc/hadoop/conf
    # Therefore, any calls to <conf-selector-tool> will fail.
    # For that reason, if the hadoop conf directory exists, then make sure it is set.
    if os.path.exists(hadoop_conf_dir):
      conf_selector_name = stack_tools.get_stack_tool_name(stack_tools.CONF_SELECTOR_NAME)
      Logger.info("The hadoop conf dir {0} exists, will call {1} on it for version {2}".format(
              hadoop_conf_dir, conf_selector_name, version))
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
  - Creates /etc/<component>/<version>/0 via <conf-selector-tool>
  - <stack-root>/current/<component>-client/conf -> /etc/<component>/<version>/0 via <conf-selector-tool>
  - Links /etc/<component>/conf to <something> depending on function paramter
  -- /etc/<component>/conf -> <stack-root>/current/[component]-client/conf (usually)
  -- /etc/<component>/conf -> /etc/<component>/conf.backup (only when supporting < HDP 2.3)

  :param package: the package to create symlinks for (zookeeper, falcon, etc)
  :param version: the version number to use with <conf-selector-tool> (2.3.0.0-1234)
  :param dirs: the directories associated with the package (from get_package_dirs())
  :param skip_existing_links: True to not do any work if already a symlink
  :param link_to: link to "current" or "backup"
  """
  # lack of enums makes this possible - we need to know what to link to
  if link_to not in [DIRECTORY_TYPE_CURRENT, DIRECTORY_TYPE_BACKUP]:
    raise Fail("Unsupported 'link_to' argument. Could not link package {0}".format(package))

  stack_name = Script.get_stack_name()
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
  for dir_def in dirs:
    old_conf = dir_def['conf_dir']
    backup_dir = _get_backup_conf_directory(old_conf)
    Logger.info("Backing up {0} to {1} if destination doesn't exist already.".format(old_conf, backup_dir))
    Execute(("cp", "-R", "-p", old_conf, backup_dir),
      not_if = format("test -e {backup_dir}"), sudo = True)

  # we're already in the HDP stack
  # Create the versioned /etc/[component]/[version]/0 folder.
  # The component must be installed on the host.
  versioned_confs = create(stack_name, package, version, dry_run = True)

  Logger.info("Package {0} will have new conf directories: {1}".format(package, ", ".join(versioned_confs)))

  need_dirs = []
  for d in versioned_confs:
    if not os.path.exists(d):
      need_dirs.append(d)

  if len(need_dirs) > 0:
    create(stack_name, package, version)

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


  # <stack-root>/current/[component] is already set to to the correct version, e.g., <stack-root>/[version]/[component]
  
  select(stack_name, package, version, ignore_errors = True)

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

      old_conf = dir_def['conf_dir']
      backup_dir = _get_backup_conf_directory(old_conf)
      # link /etc/[component]/conf -> /etc/[component]/conf.backup
      # or
      # link /etc/[component]/conf -> <stack-root>/current/[component]-client/conf
      if link_to == DIRECTORY_TYPE_BACKUP:
        Link(new_symlink, to=backup_dir)
      else:
        Link(new_symlink, to=dir_def['current_dir'])

        #HACK
        if package in ["atlas", ]:
          Logger.info("Seeding the new conf symlink {0} from the old backup directory {1} in case any "
                      "unmanaged artifacts are needed.".format(new_symlink, backup_dir))
          # If /etc/[component]/conf.backup exists, then copy any artifacts not managed by Ambari to the new symlink target
          # Be careful not to clobber any existing files.
          Execute(as_sudo(["cp", "-R", "--no-clobber", os.path.join(backup_dir, "*"), new_symlink], auto_escape=False),
                  only_if=format("test -e {new_symlink}"))
  except Exception, e:
    Logger.warning("Could not change symlink for package {0} to point to {1} directory. Error: {2}".format(package, link_to, e))


def _seed_new_configuration_directories(package, created_directories):
  """
  Copies any files from the "current" configuration directory to the directories which were
  newly created with <conf-selector-tool>. This function helps ensure that files which are not tracked
  by Ambari will be available after performing a stack upgrade. Although old configurations
  will be copied as well, they will be overwritten when the components are writing out their
  configs after upgrade during their restart.

  This function will catch all errors, logging them, but not raising an exception. This is to
  prevent problems here from stopping and otherwise healthy upgrade.

  :param package: the <conf-selector-tool> package name
  :param created_directories: a list of directories that <conf-selector-tool> said it created
  :return: None
  """
  package_dirs = get_package_dirs()
  if package not in package_dirs:
    Logger.warning("Unable to seed newly created configuration directories for {0} because it is an unknown component".format(package))
    return

  # seed the directories with any existing configurations
  # this allows files which are not tracked by Ambari to be available after an upgrade
  Logger.info("Seeding versioned configuration directories for {0}".format(package))
  expected_directories = package_dirs[package]

  try:
    # if the expected directories don't match those created, we can't seed them
    if len(created_directories) != len(expected_directories):
      Logger.warning("The known configuration directories for {0} do not match those created by conf-select: {1}".format(
        package, str(created_directories)))

      return

    # short circuit for a simple 1:1 mapping
    if len(expected_directories) == 1:
      # <stack-root>/current/component/conf
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
