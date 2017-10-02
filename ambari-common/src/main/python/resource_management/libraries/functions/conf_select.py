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
import os
import subprocess
import ambari_simplejson as json

# Local Imports
import stack_select
from resource_management.core import shell
from resource_management.libraries.functions.format import format
from resource_management.libraries.script.script import Script
from resource_management.core.logger import Logger
from resource_management.core.resources.system import Directory
from resource_management.core.resources.system import Execute
from resource_management.core.resources.system import Link
from resource_management.libraries.functions import component_version
from resource_management.libraries.functions.default import default
from resource_management.libraries.functions import stack_tools
from resource_management.core.exceptions import Fail
from resource_management.core import sudo
from resource_management.core.shell import as_sudo
from resource_management.libraries.functions.stack_features import check_stack_feature
from resource_management.libraries.functions import StackFeature

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
  stack_name = default("/hostLevelParams/stack_name", None)
  if stack_name is None:
    raise Fail("The stack name is not present in the command. Packages for conf-select tool cannot be loaded.")

  stack_packages_config = default("/configurations/cluster-env/stack_packages", None)
  if stack_packages_config is None:
    raise Fail("The stack packages are not defined on the command. Unable to load packages for the conf-select tool")

  data = json.loads(stack_packages_config)

  if stack_name not in data:
    raise Fail(
      "Cannot find conf-select packages for the {0} stack".format(stack_name))

  conf_select_key = "conf-select"
  data = data[stack_name]
  if conf_select_key not in data:
    raise Fail(
      "There are no conf-select packages defined for this command for the {0} stack".format(stack_name))

  package_dirs = data[conf_select_key]

  stack_root = Script.get_stack_root()
  for package_name, directories in package_dirs.iteritems():
    for dir in directories:
      current_dir = dir['current_dir']
      current_dir =  current_dir.format(stack_root)
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



def get_hadoop_conf_dir():
  """
  Return the hadoop shared conf directory which should be used for the command's component. The
  directory including the component's version is tried first, but if that doesn't exist,
  this will fallback to using "current".
  """
  stack_root = Script.get_stack_root()
  stack_version = Script.get_stack_version()

  hadoop_conf_dir = os.path.join(os.path.sep, "etc", "hadoop", "conf")
  if check_stack_feature(StackFeature.CONFIG_VERSIONING, stack_version):
    # read the desired version from the component map and use that for building the hadoop home
    version = component_version.get_component_repository_version()
    if version is None:
      version = default("/commandParams/version", None)

    hadoop_conf_dir = os.path.join(stack_root, str(version), "hadoop", "conf")
    if version is None or sudo.path_isdir(hadoop_conf_dir) is False:
      hadoop_conf_dir = os.path.join(stack_root, "current", "hadoop-client", "conf")

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
    Execute(("cp", "-R", "-p", unicode(old_conf), unicode(backup_dir)),
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
