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
from resource_management.core.resources.system import Execute
from resource_management.core.shell import as_sudo
from resource_management.libraries.functions import conf_select
from resource_management.libraries.functions.format import format
from resource_management.libraries.functions.version import compare_versions
from resource_management.libraries.resources.xml_config import XmlConfig
from resource_management.libraries.script import Script

def setup_iop_install_directory():
  # This is a name of marker file.
  SELECT_ALL_PERFORMED_MARKER = "/var/lib/ambari-agent/data/iop-select-set-all.performed"

  import params
  if params.iop_stack_version != "" and compare_versions(params.iop_stack_version, '4.0') >= 0:
    Execute(as_sudo(['touch', SELECT_ALL_PERFORMED_MARKER]) + ' ; ' +
                   format('{sudo} /usr/bin/iop-select set all `ambari-python-wrap /usr/bin/iop-select versions | grep ^{stack_version_unformatted} | tail -1`'),
            only_if=format('ls -d /usr/iop/{stack_version_unformatted}*'),   # If any IOP version is installed
            not_if=format("test -f {SELECT_ALL_PERFORMED_MARKER}")           # Do that only once (otherwise we break rolling upgrade logic)
    )

def setup_config():
  import params
  if params.has_namenode:
    # create core-site only if the hadoop config directory exists
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
  Links configs, only on a fresh install of BigInsights-4.1 and higher
  """

  if not Script.is_stack_greater_or_equal("4.1"):
    Logger.info("Can only link configs for BigInsights-4.1 and higher.")
    return

  json_version = load_version(struct_out_file)

  if not json_version:
    Logger.info("Could not load 'version' from {0}".format(struct_out_file))
    return

  for k, v in conf_select.get_package_dirs().iteritems():
    conf_select.convert_conf_directories_to_symlinks(k, json_version, v)