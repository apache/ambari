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

from resource_management import Script
from resource_management.core.resources.system import Execute
from resource_management.core.logger import Logger
from resource_management.libraries.functions.default import default
from resource_management.core.source import InlineTemplate
from resource_management.libraries.functions import stack_select

import master_helper
import common
import hawq_constants
import utils

class HawqMaster(Script):
  """
  Contains the interface definitions for methods like install, 
  start, stop, status, etc. for the HAWQ Master
  """

  def install(self, env):
    self.install_packages(env)
    self.configure(env)


  def configure(self, env):
    import params
    env.set_params(params)
    env.set_params(hawq_constants)
    master_helper.configure_master()


  def start(self, env):
    import params
    self.configure(env)
    common.validate_configuration()
    exchange_ssh_keys = default('/configurations/hawq-env/hawq_ssh_exkeys', None)
    if exchange_ssh_keys is None or str(exchange_ssh_keys).lower() == 'false':
      Logger.info("Skipping ssh key exchange with HAWQ hosts as hawq_ssh_exkeys is either set to false or is not available in hawq-env.xml")
    else:
      master_helper.setup_passwordless_ssh()
    common.start_component(hawq_constants.MASTER, params.hawq_master_address_port, params.hawq_master_dir)


  def stop(self, env):
    common.stop_component(hawq_constants.MASTER, hawq_constants.FAST)


  def status(self, env):
    from hawqstatus import assert_component_running
    assert_component_running(hawq_constants.MASTER)


  def immediate_stop_hawq_service(self, env):
    common.stop_component(hawq_constants.CLUSTER, hawq_constants.IMMEDIATE)


  def hawq_clear_cache(self, env):
    import params
    from utils import exec_psql_cmd
    cmd = "SELECT gp_metadata_cache_clear()"
    Logger.info("Clearing HAWQ's HDFS Metadata cache ...")
    exec_psql_cmd(cmd, params.hawqmaster_host, params.hawq_master_address_port)


  def run_hawq_check(self, env):
    import params
    Logger.info("Executing HAWQ Check ...")
    params.File(hawq_constants.hawq_hosts_file, content=InlineTemplate("{% for host in hawq_all_hosts %}{{host}}\n{% endfor %}"))

    additional_flags = list()

    if params.dfs_nameservice:
      additional_flags.append("--hdfs-ha")

    if params.hawq_global_rm_type == "yarn":
      yarn_option = "--yarn-ha" if params.is_yarn_ha_enabled else "--yarn"
      additional_flags.append(yarn_option)

    if str(params.security_enabled).lower() == "true":
      additional_flags.append("--kerberos")

    utils.exec_hawq_operation(hawq_constants.CHECK,
                              "-f {1} --hadoop {2} --config {3} {4}".format(hawq_constants.hawq_greenplum_path_file,
                                                                            hawq_constants.hawq_hosts_file,
                                                                            stack_select.get_hadoop_dir('home'),
                                                                            hawq_constants.hawq_check_file,
                                                                            " ".join(additional_flags)),
                              host_name=params.hawqmaster_host)


  def resync_hawq_standby(self,env):
    import params
    Logger.info("HAWQ Standby Master Re-Sync started in fast mode...")
    utils.exec_hawq_operation(hawq_constants.INIT,
                              "{0} -n -a -v -M {1}".format(hawq_constants.STANDBY, hawq_constants.FAST),
                              host_name=params.hawqmaster_host)


  def remove_hawq_standby(self, env):
    import params
    Logger.info("Removing HAWQ Standby Master ...")
    utils.exec_hawq_operation(hawq_constants.INIT,
                              "{0} -a -v -r --ignore-bad-hosts".format(hawq_constants.STANDBY),
                              host_name=params.hawqmaster_host)


if __name__ == "__main__":
  HawqMaster().execute()
