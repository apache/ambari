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

import os
import glob
from urlparse import urlparse

from resource_management.libraries.script.script import Script
from resource_management.libraries.resources.hdfs_resource import HdfsResource
from resource_management.libraries.functions.copy_tarball import copy_to_hdfs
from resource_management.libraries.functions.version import compare_versions
from resource_management.core.resources.service import ServiceConfig
from resource_management.core.resources.system import File, Execute, Directory
from resource_management.core.source import StaticFile, Template, DownloadSource, InlineTemplate
from resource_management.core.shell import as_user
from resource_management.libraries.functions.is_empty import is_empty
from resource_management.libraries.resources.xml_config import XmlConfig
from resource_management.libraries.functions.format import format
from resource_management.core.exceptions import Fail
from resource_management.core.shell import as_sudo
from resource_management.core.shell import quote_bash_args
from resource_management.core.logger import Logger
from resource_management.core import utils

from ambari_commons.os_family_impl import OsFamilyFuncImpl, OsFamilyImpl
from ambari_commons import OSConst
from hive import fill_conf_dir, create_directory, jdbc_connector


@OsFamilyFuncImpl(os_family=OSConst.WINSRV_FAMILY)
def hive_interactive(name=None):
  pass

"""
Sets up the configs, jdbc connection and tarball copy to HDFS for Hive Server Interactive.
"""
@OsFamilyFuncImpl(os_family=OsFamilyImpl.DEFAULT)
def hive_interactive(name=None):
  import params

  # Copy Tarballs in HDFS.
  copy_to_hdfs("tez_hive2",
               params.user_group,
               params.hdfs_user,
               file_mode=params.tarballs_mode,
               host_sys_prepped=params.host_sys_prepped)

  copy_to_hdfs("hive2",
               params.user_group,
               params.hdfs_user,
               file_mode=params.tarballs_mode,
               host_sys_prepped=params.host_sys_prepped)

  params.HdfsResource(None, action="execute")

  Directory(params.hive_interactive_etc_dir_prefix,
            mode=0755
            )

  for conf_dir in params.hive_interactive_conf_dir:
    fill_conf_dir(conf_dir)

  '''
  As hive2/hive-site.xml only contains the new + the changed props compared to hive/hive-site.xml,
  we need to merge hive/hive-site.xml and hive2/hive-site.xml and store it in hive2/hive-site.xml.
  '''
  merged_hive_interactive_site = {}
  merged_hive_interactive_site.update(params.config['configurations']['hive-site'])
  merged_hive_interactive_site.update(params.config['configurations']['hive-interactive-site'])

  XmlConfig("hive-site.xml",
            conf_dir=params.hive_server_interactive_conf_dir,
            configurations=merged_hive_interactive_site,
            configuration_attributes=params.config['configuration_attributes']['hive-interactive-site'],
            owner=params.hive_user,
            group=params.user_group,
            mode=0644)

  XmlConfig("tez-site.xml",
             conf_dir = params.tez_interactive_config_dir,
             configurations = params.config['configurations']['tez-interactive-site'],
             configuration_attributes=params.config['configuration_attributes']['tez-interactive-site'],
             owner = params.tez_interactive_user,
             group = params.user_group,
             mode = 0664)

  File(format("{hive_server_interactive_conf_dir}/hive-env.sh"),
       owner=params.hive_user,
       group=params.user_group,
       content=InlineTemplate(params.hive_interactive_env_sh_template)
       )

  if not os.path.exists(params.target_hive_interactive):
    jdbc_connector()

  File(format("/usr/lib/ambari-agent/{check_db_connection_jar_name}"),
       content = DownloadSource(format("{jdk_location}{check_db_connection_jar_name}")),
       mode = 0644,
       )
  File(params.start_hiveserver2_interactive_path,
       mode=0755,
       content=Template(format('{start_hiveserver2_interactive_script}'))
       )

  create_directory(params.hive_pid_dir)
  create_directory(params.hive_log_dir)
  create_directory(params.hive_interactive_var_lib)