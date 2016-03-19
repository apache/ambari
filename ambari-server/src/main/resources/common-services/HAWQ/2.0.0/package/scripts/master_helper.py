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
from resource_management.core.resources.system import File, Execute
from resource_management.core.source import InlineTemplate
from resource_management.core.exceptions import Fail
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


def __setup_passwordless_ssh():
  """
  Exchanges ssh keys to setup passwordless ssh for the hawq_user between the HAWQ Master and the HAWQ Segment nodes
  """
  import params
  utils.exec_hawq_operation("ssh-exkeys", format('-f {hawq_hosts_file} -p {hawq_password!p}', hawq_hosts_file=hawq_constants.hawq_hosts_file, hawq_password=params.hawq_password))


def configure_master():
  """
  Configures the master node after rpm install
  """
  common.setup_user()
  common.setup_common_configurations()
  __setup_master_specific_conf_files()
  import params
  common.create_master_dir(params.hawq_master_dir)
  common.create_temp_dirs(params.hawq_master_temp_dirs)

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


def __init_active():
  """
  Initializes the active master
  """
  import params
  __setup_hdfs_dirs()
  utils.exec_hawq_operation(hawq_constants.INIT, "{0} -a -v".format(hawq_constants.MASTER))
  Logger.info("Active master {0} initialized".format(params.hostname))


def __init_standby():
  """
  Initializes the HAWQ Standby Master
  """
  import params
  utils.exec_hawq_operation(hawq_constants.INIT, "{0} -a -v".format(hawq_constants.STANDBY))
  Logger.info("Standby host {0} initialized".format(params.hostname))


def __get_component_name():
  """
  Identifies current node as either HAWQ Master or HAWQ Standby Master
  """
  return hawq_constants.MASTER if __is_active_master() else hawq_constants.STANDBY


def __start_local_master():
  """
  Starts HAWQ Master or HAWQ Standby Master component on the host
  """
  import params
  component_name = __get_component_name()

  __setup_hdfs_dirs()

  utils.exec_hawq_operation(
        hawq_constants.START,
        "{0} -a -v".format(component_name),
        not_if=utils.chk_hawq_process_status_cmd(params.hawq_master_address_port, component_name))
  Logger.info("Master {0} started".format(params.hostname))


def __is_local_initialized():
  """
  Checks if the local node has been initialized
  """
  import params
  return os.path.exists(os.path.join(params.hawq_master_dir, hawq_constants.postmaster_opts_filename))


def __get_standby_host():
  """
  Returns the name of the HAWQ Standby Master host from hawq-site.xml, or None if no standby is configured
  """
  standby_host = common.get_local_hawq_site_property("hawq_standby_address_host")
  return None if standby_host is None or standby_host.lower() == 'none' else standby_host


def __is_active_master():
  """
  Finds if this node is the active master
  """
  import params
  return params.hostname == common.get_local_hawq_site_property("hawq_master_address_host")


def __is_standby_host():
  """
  Finds if this node is the standby host
  """
  import params
  return params.hostname == common.get_local_hawq_site_property("hawq_standby_address_host")


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


def start_master():
  """
  Initializes HAWQ Master/Standby if not already done and starts them
  """
  import params

  if not params.hostname in [params.hawqmaster_host, params.hawqstandby_host]:
    Fail("Host should be either active Hawq master or Hawq standby.")

  __check_dfs_truncate_enforced()

  is_active_master = __is_active_master()
  __setup_passwordless_ssh()

  if __is_local_initialized():
    __start_local_master()
    return

  if is_active_master:
    __init_active()
  elif __is_standby_host():
    __init_standby()


def stop(mode=hawq_constants.FAST, component=None):
  """
  Stops the HAWQ Master/Standby, if component is cluster performs cluster level operation from master
  """
  import params
  component_name = component if component else __get_component_name()
  utils.exec_hawq_operation(
                hawq_constants.STOP,
                "{0} -M {1} -a -v".format(component_name, mode),
                only_if=utils.chk_hawq_process_status_cmd(params.hawq_master_address_port, component_name))
