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
from resource_management.core.shell import as_sudo
from resource_management.core.logger import Logger
from resource_management.core.resources.system import Execute
from resource_management.libraries.functions.version import compare_versions
from resource_management.libraries.functions.format import format
from resource_management.libraries.functions import conf_select
from resource_management.libraries.resources.xml_config import XmlConfig
from resource_management.libraries.script import Script
from resource_management.core.resources.system import Directory


def setup_hdp_install_directory():
  # This is a name of marker file.
  SELECT_ALL_PERFORMED_MARKER = "/var/lib/ambari-agent/data/hdp-select-set-all.performed"
  import params
  if params.hdp_stack_version != "" and compare_versions(params.hdp_stack_version, '2.2') >= 0:
    Execute(as_sudo(['touch', SELECT_ALL_PERFORMED_MARKER]) + ' ; ' +
                   format('{sudo} /usr/bin/hdp-select set all `ambari-python-wrap /usr/bin/hdp-select versions | grep ^{stack_version_unformatted} | tail -1`'),
            only_if=format('ls -d /usr/hdp/{stack_version_unformatted}*'),   # If any HDP version is installed
            not_if=format("test -f {SELECT_ALL_PERFORMED_MARKER}")           # Do that only once (otherwise we break rolling upgrade logic)
    )

def setup_config():
  import params
  stackversion = params.stack_version_unformatted
  if params.has_namenode or stackversion.find('Gluster') >= 0:
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
    _link_configs(k, json_version, v['conf_dir'], v['current_dir'])

def _link_configs(package, version, old_conf, link_conf):
  """
  Link a specific package's configuration directory
  """

  if not os.path.exists(old_conf):
    Logger.debug("Skipping {0} as it does not exist.".format(old_conf))
    return

  # check if conf is a link to the target already
  if os.path.islink(old_conf):
    Logger.debug("{0} is already a link to {1}".format(old_conf, os.path.realpath(old_conf)))
    return

  # make backup dir and copy everything in case configure() was called after install()
  old_parent = os.path.abspath(os.path.join(old_conf, os.pardir))
  old_conf_copy = os.path.join(old_parent, "conf.install")
  if not os.path.exists(old_conf_copy):
    try:
      Execute(as_sudo(["cp", "-R", "-p", old_conf, old_conf_copy]), logoutput=True)
    except:
      pass

  versioned_conf = conf_select.create("HDP", package, version, dry_run = True)

  Logger.info("New conf directory is {0}".format(versioned_conf))

  # make new conf dir and copy everything in case configure() was called after install()
  if not os.path.exists(versioned_conf):
    conf_select.create("HDP", package, version)
    try:
      Execute(as_sudo(["cp", "-R", "-p", os.path.join(old_conf, "*"), versioned_conf], auto_escape=False),
        logoutput=True)
      Directory(versioned_conf,
                mode=0755,
                cd_access='a'
      )
    except:
      pass

  # make /usr/hdp/<version>/hadoop/conf point to the versioned config.
  # /usr/hdp/current is already set
  conf_select.select("HDP", package, version)

  # no more references to /etc/[component]/conf
  shutil.rmtree(old_conf, ignore_errors=True)

  # link /etc/[component]/conf -> /usr/hdp/current/[component]-client/conf
  os.symlink(link_conf, old_conf)
      
  # should conf.install be removed?

