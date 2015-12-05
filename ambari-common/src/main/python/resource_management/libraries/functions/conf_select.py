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
  :stack_name: the name of the stack
  :package: the name of the package, as-used by conf-select
  :version: the version number to create
  """

  if not _valid(stack_name, package, version):
    return

  command = "dry-run-create" if dry_run else "create-conf-dir"

  code, stdout, stderr = shell.call(get_cmd(command, package, version), logoutput=False, quiet=False, sudo=True, stderr = subprocess.PIPE)

  # conf-select can set more than one directory
  # per package, so return that list, especially for dry_run
  dirs = []
  if 0 == code and stdout is not None: # just be sure we have a stdout
    for line in stdout.splitlines():
      dirs.append(line.rstrip('\n'))

  # take care of permissions
  if not code and stdout and command == "create-conf-dir":
    for d in dirs:
      Directory(d,
          mode=0755,
          cd_access='a',
          recursive=True)

  return dirs


def select(stack_name, package, version, try_create=True):
  """
  Selects a config version for the specified package.
  :stack_name: the name of the stack
  :package: the name of the package, as-used by conf-select
  :version: the version number to create
  :try_create: optional argument to attempt to create the directory before setting it
  """
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

      # if /etc/<component>/conf is not a symlink, we need to change it
      if not os.path.islink(conf_dir):
        # if it exists, try to back it up
        if os.path.exists(conf_dir):
          parent_directory = os.path.dirname(conf_dir)
          conf_install_dir = os.path.join(parent_directory, "conf.backup")

          Execute(("cp", "-R", "-p", conf_dir, conf_install_dir),
            not_if = format("test -e {conf_install_dir}"), sudo = True)

          Directory(conf_dir, action="delete")

        Link(conf_dir, to = current_dir)


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

  if Script.is_hdp_stack_greater_or_equal("2.2"):
    hadoop_conf_dir = "/usr/hdp/current/hadoop-client/conf"

    stack_info = hdp_select._get_upgrade_stack()

    # if upgrading to >= HDP 2.3
    if stack_info is not None and Script.is_hdp_stack_greater_or_equal("2.3"):
      stack_name = stack_info[0]
      stack_version = stack_info[1]

      # determine if hdp-select has been run and if not, then use the current
      # hdp version until this component is upgraded
      if not force_latest_on_upgrade:
        current_hdp_version = hdp_select.get_role_component_current_hdp_version()
        if current_hdp_version is not None and stack_version != current_hdp_version:
          stack_version = current_hdp_version

      # only change the hadoop_conf_dir path, don't conf-select this older version
      hadoop_conf_dir = "/usr/hdp/{0}/hadoop/conf".format(stack_version)

      # ensure the new HDP stack is conf-selected, but only if it exists
      # there are cases where hadoop might not be installed, such as on a host with only ZK
      if os.path.exists(hadoop_conf_dir):
        select(stack_name, "hadoop", stack_version)

  return hadoop_conf_dir


def convert_conf_directories_to_symlinks(package, version, dirs, skip_existing_links=True, link_to_conf_install=False):

  """
  Assumes HDP 2.3+, moves around directories and creates the conf symlink for the given package.
  If the package does not exist, then no work is performed.

  - Creates a /etc/<component>/conf.backup directory
  - Copies all configs from /etc/<component>/conf to conf.backup
  - Removes /etc/<component>/conf
  - Creates /etc/<component>/<version>/0
  - Creates /usr/hdp/current/<component>-client/conf -> /etc/<component>/<version>/0
  - Links /etc/<component>/conf to <something>
  -- /etc/<component>/conf -> /usr/hdp/current/[component]-client/conf
  -- /etc/<component>/conf -> /etc/<component>/conf.backup

  :param package: the package to create symlinks for (zookeeper, falcon, etc)
  :param version: the version number to use with conf-select (2.3.0.0-1234)
  :param dirs: the directories associated with the package (from PACKAGE_DIRS)
  :param skip_existing_links: True to not do any work if already a symlink
  :param link_to_conf_install:
  """
  bad_dirs = []
  for dir_def in dirs:
    if not os.path.exists(dir_def['conf_dir']):
      bad_dirs.append(dir_def['conf_dir'])

  if len(bad_dirs) > 0:
    Logger.info("Skipping {0} as it does not exist.".format(",".join(bad_dirs)))
    return

  # existing links should be skipped since we assume there's no work to do
  if skip_existing_links:
    bad_dirs = []
    for dir_def in dirs:
      # check if conf is a link already
      old_conf = dir_def['conf_dir']
      if os.path.islink(old_conf):
        Logger.info("{0} is already link to {1}".format(old_conf, os.path.realpath(old_conf)))
        bad_dirs.append(old_conf)

  if len(bad_dirs) > 0:
    return

  # make backup dir and copy everything in case configure() was called after install()
  for dir_def in dirs:
    old_conf = dir_def['conf_dir']
    old_parent = os.path.abspath(os.path.join(old_conf, os.pardir))
    conf_install_dir = os.path.join(old_parent, "conf.backup")
    Execute(("cp", "-R", "-p", old_conf, conf_install_dir),
      not_if = format("test -e {conf_install_dir}"), sudo = True)

  # we're already in the HDP stack
  versioned_confs = create("HDP", package, version, dry_run = True)

  Logger.info("New conf directories: {0}".format(", ".join(versioned_confs)))

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


  # make /usr/hdp/[version]/[component]/conf point to the versioned config.
  # /usr/hdp/current is already set
  try:
    select("HDP", package, version)

    # no more references to /etc/[component]/conf
    for dir_def in dirs:
      new_symlink = dir_def['conf_dir']

      # remove new_symlink to pave the way, but only if it's a directory
      if not os.path.islink(new_symlink):
        Directory(new_symlink, action="delete")

      # link /etc/[component]/conf -> /usr/hdp/current/[component]-client/conf
      if link_to_conf_install:
        Link(new_symlink, to = conf_install_dir)
      else:
        Link(new_symlink, to = dir_def['current_dir'])
  except Exception, e:
    Logger.warning("Could not select the directory: {0}".format(e.message))

  # should conf.backup be removed?

