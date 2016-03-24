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

import sys
from resource_management.core.source import InlineTemplate
from resource_management.core.logger import Logger
from resource_management.libraries.functions.format import format
from resource_management.libraries.functions.default import default

import utils
import common
import hawq_constants

def __setup_master_specific_conf_files():
  """
  Sets up config files only applicable for HAWQ Master and Standby nodes
  """
  import params

  params.File(hawq_constants.hawq_check_file,
              content=params.hawq_check_content)

  params.File(hawq_constants.hawq_slaves_file,
              content=InlineTemplate("{% for host in hawqsegment_hosts %}{{host}}\n{% endfor %}"))

  params.File(hawq_constants.hawq_hosts_file,
              content=InlineTemplate("{% for host in hawq_all_hosts %}{{host}}\n{% endfor %}"))


def setup_passwordless_ssh():
  """
  Exchanges ssh keys to setup passwordless ssh for the hawq_user between the HAWQ Master and the HAWQ Segment nodes
  """
  import params
  utils.exec_hawq_operation("ssh-exkeys", format('-f {hawq_hosts_file} -p {hawq_password!p}', hawq_hosts_file=hawq_constants.hawq_hosts_file, hawq_password=params.hawq_password))


def configure_master():
  """
  Configures the master node after rpm install
  """
  import params
  common.setup_user()
  common.setup_common_configurations()
  __setup_master_specific_conf_files()
  common.create_master_dir(params.hawq_master_dir)
  common.create_temp_dirs(params.hawq_master_temp_dirs)
  __check_dfs_truncate_enforced()

def __setup_hdfs_dirs():
  """
  Creates the required HDFS directories for HAWQ if they don't exist
  or sets proper owner/mode if directory exists
  """
  import params

  data_dir_owner = hawq_constants.hawq_user_secured if params.security_enabled else hawq_constants.hawq_user

  params.HdfsResource(params.hawq_hdfs_data_dir,
                        type="directory",
                        action="create_on_execute",
                        owner=data_dir_owner,
                        group=hawq_constants.hawq_group,
                        recursive_chown = True,
                        mode=0755)
  params.HdfsResource(None, action="execute")

def __check_dfs_truncate_enforced():
  """
  If enforce_hdfs_truncate is set to True:
    throw an ERROR, HAWQMASTER or HAWQSTANDBY start should fail
  Else:
    throw a WARNING,
  """
  import custom_params

  DFS_ALLOW_TRUNCATE_EXCEPTION_MESSAGE = "dfs.allow.truncate property in hdfs-site.xml configuration file should be set to True. Please review HAWQ installation guide for more information."

  # Check if dfs.allow.truncate exists in hdfs-site.xml and throw appropriate exception if not set to True
  dfs_allow_truncate = default('/configurations/hdfs-site/dfs.allow.truncate', None)

  if dfs_allow_truncate is None or str(dfs_allow_truncate).lower() != 'true':
    if custom_params.enforce_hdfs_truncate:
      Logger.error("**ERROR**: {0}".format(DFS_ALLOW_TRUNCATE_EXCEPTION_MESSAGE))
      sys.exit(1)
    else:
      Logger.warning("**WARNING**: {0}".format(DFS_ALLOW_TRUNCATE_EXCEPTION_MESSAGE))
