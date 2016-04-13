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
from resource_management.core.exceptions import Fail

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


def setup_passwordless_ssh():
  """
  Exchanges ssh keys to setup passwordless ssh for the hawq_user between the HAWQ Master and the HAWQ Segment nodes
  """
  import params

  failed_hosts = []
  for host in params.hawq_all_hosts:
    try:
      utils.exec_hawq_operation("ssh-exkeys", format('-h {hawq_host} -p {hawq_password!p}', hawq_host=host, hawq_password=params.hawq_password))
    except:
      failed_hosts.append(host)

  failed_hosts_cnt = len(failed_hosts)
  if failed_hosts_cnt > 0:
    DEBUG_HELP_MSG = "Please verify the logs below to debug the cause of failure."
    if failed_hosts_cnt == len(params.hawq_all_hosts):
      raise Fail("Setting up passwordless ssh failed for all the HAWQ hosts. {0}".format(DEBUG_HELP_MSG))
    else:
      Logger.error("**WARNING**: Setting up passwordless ssh failed with the hosts below, proceeding with HAWQ Master start:\n{0}\n\n{1}".format("\n".join(failed_hosts), DEBUG_HELP_MSG))


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

