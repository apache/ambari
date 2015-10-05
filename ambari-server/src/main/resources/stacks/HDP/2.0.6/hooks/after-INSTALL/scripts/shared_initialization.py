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

import ambari_simplejson as json
from resource_management.core.logger import Logger
from resource_management.core.resources.system import Directory, Link
from resource_management.core.resources.system import Execute
from resource_management.core.shell import as_sudo
from resource_management.libraries.functions import conf_select
from resource_management.libraries.functions.format import format
from resource_management.libraries.functions.version import compare_versions
from resource_management.libraries.resources.xml_config import XmlConfig
from resource_management.libraries.script import Script


def setup_hdp_install_directory():
  # This is a name of marker file.
  SELECT_ALL_PERFORMED_MARKER = "/var/lib/ambari-agent/data/hdp-select-set-all.performed"
  import params
  if params.hdp_stack_version != "" and compare_versions(params.hdp_stack_version, '2.2') >= 0:
    Execute(format('{sudo} /usr/bin/hdp-select set all `ambari-python-wrap /usr/bin/hdp-select versions | grep ^{stack_version_unformatted} | tail -1`') + ' && ' +
            as_sudo(['touch', SELECT_ALL_PERFORMED_MARKER]),
            only_if=format('ls -d /usr/hdp/{stack_version_unformatted}*'),   # If any HDP version is installed
            not_if=format("test -f {SELECT_ALL_PERFORMED_MARKER}")           # Do that only once (otherwise we break rolling upgrade logic)
    )

def setup_config():
  import params
  stackversion = params.stack_version_unformatted
  Logger.info("FS Type: {0}".format(params.dfs_type))
  if params.has_namenode or stackversion.find('Gluster') >= 0 or params.dfs_type == 'HCFS':
    # create core-site only if the hadoop config diretory exists
    XmlConfig("core-site.xml",
              conf_dir=params.hadoop_conf_dir,
              configurations=params.config['configurations']['core-site'],
              configuration_attributes=params.config['configuration_attributes']['core-site'],
              owner=params.hdfs_user,
              group=params.user_group,
              only_if=format("ls {hadoop_conf_dir}"))


def load_version(struct_out_file):
  """
  Load version from file.  Made a separate method for testing
  """
  json_version = None
  try:
    if os.path.exists(struct_out_file):
      with open(struct_out_file, 'r') as fp:
        json_info = json.load(fp)
        json_version = json_info['version']
  except:
    pass

  return json_version
  

def link_configs(struct_out_file):
  """
  Links configs, only on a fresh install of HDP-2.3 and higher
  """

  if not Script.is_hdp_stack_greater_or_equal("2.3"):
    Logger.info("Can only link configs for HDP-2.3 and higher.")
    return

  json_version = load_version(struct_out_file)

  if not json_version:
    Logger.info("Could not load 'version' from {0}".format(struct_out_file))
    return

  for k, v in conf_select.PACKAGE_DIRS.iteritems():
    _link_configs(k, json_version, v)

def _link_configs(package, version, dirs):
  """
  Link a specific package's configuration directory
  """
  bad_dirs = []
  for dir_def in dirs:
    if not os.path.exists(dir_def['conf_dir']):
      bad_dirs.append(dir_def['conf_dir'])

  if len(bad_dirs) > 0:
    Logger.debug("Skipping {0} as it does not exist.".format(",".join(bad_dirs)))
    return

  bad_dirs = []
  for dir_def in dirs:
    # check if conf is a link already
    old_conf = dir_def['conf_dir']
    if os.path.islink(old_conf):
      Logger.debug("{0} is a link to {1}".format(old_conf, os.path.realpath(old_conf)))
      bad_dirs.append(old_conf)

  if len(bad_dirs) > 0:
    return

  # make backup dir and copy everything in case configure() was called after install()
  for dir_def in dirs:
    old_conf = dir_def['conf_dir']
    old_parent = os.path.abspath(os.path.join(old_conf, os.pardir))
    old_conf_copy = os.path.join(old_parent, "conf.install")
    Execute(("cp", "-R", "-p", old_conf, old_conf_copy),
      not_if = format("test -e {old_conf_copy}"), sudo = True)

  # we're already in the HDP stack
  versioned_confs = conf_select.create("HDP", package, version, dry_run = True)

  Logger.info("New conf directories: {0}".format(", ".join(versioned_confs)))

  need_dirs = []
  for d in versioned_confs:
    if not os.path.exists(d):
      need_dirs.append(d)

  if len(need_dirs) > 0:
    conf_select.create("HDP", package, version)

    # find the matching definition and back it up (not the most efficient way) ONLY if there is more than one directory
    if len(dirs) > 1:
      for need_dir in need_dirs:
        for dir_def in dirs:
          if 'prefix' in dir_def and need_dir.startswith(dir_def['prefix']):
            old_conf = dir_def['conf_dir']
            versioned_conf = need_dir
            Execute(as_sudo(["cp", "-R", "-p", os.path.join(old_conf, "*"), versioned_conf], auto_escape=False),
              only_if = format("ls {old_conf}/*"))
    elif 1 == len(dirs) and 1 == len(need_dirs):
      old_conf = dirs[0]['conf_dir']
      versioned_conf = need_dirs[0]
      Execute(as_sudo(["cp", "-R", "-p", os.path.join(old_conf, "*"), versioned_conf], auto_escape=False),
        only_if = format("ls {old_conf}/*"))


  # make /usr/hdp/[version]/[component]/conf point to the versioned config.
  # /usr/hdp/current is already set
  try:
    conf_select.select("HDP", package, version)

    # no more references to /etc/[component]/conf
    for dir_def in dirs:
      Directory(dir_def['conf_dir'], action="delete")

      # link /etc/[component]/conf -> /usr/hdp/current/[component]-client/conf
      Link(dir_def['conf_dir'], to = dir_def['current_dir'])
  except Exception, e:
    Logger.warning("Could not select the directory: {0}".format(e.message))

  # should conf.install be removed?
