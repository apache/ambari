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

import sys

from resource_management.core import shell
from resource_management.core.resources.system import Execute
from resource_management.libraries.script.script import Script
from resource_management.libraries.functions.format import format
from resource_management.libraries.functions.check_process_status import check_process_status
from resource_management.libraries.functions.security_commons import build_expectations, \
  cached_kinit_executor, get_params_from_filesystem, validate_security_config_properties, \
  FILE_TYPE_XML

from ambari_commons import OSCheck, OSConst
from ambari_commons.constants import UPGRADE_TYPE_ROLLING
from ambari_commons.os_family_impl import OsFamilyImpl

from hbase import hbase
from hbase_service import hbase_service
import upgrade
from setup_ranger_hbase import setup_ranger_hbase
from hbase_decommission import hbase_decommission


class HbaseRegionServer(Script):
  def install(self, env):
    import params
    env.set_params(params)
    self.install_packages(env)

  def configure(self, env):
    import params
    env.set_params(params)
    hbase(name='regionserver')

  def decommission(self, env):
    print "Decommission not yet implemented!"

  def graceful_stop(self, env, upgrade_type=None):
    import params

    # Mark Draining ZNode
    params.hbase_drain_only = False
    params.hbase_excluded_hosts = params.hostname
    env.set_params(params)
    hbase_decommission(env)

    # Stop RegionServer
    hbase_service('regionserver', action='stop')

    # Remove from Draining ZNode to make host useable on restarting regionserver
    params.hbase_drain_only = True
    env.set_params(params)
    hbase_decommission(env)

  def graceful_start(self, env, upgrade_type=None):
    import params
    env.set_params(params)

    #Start RegionServer
    hbase_service('regionserver', action='start')

    # Load Regions back
    kinit_cmd = params.kinit_cmd_master
    host = params.hostname

    regionmover_cmd = format(
      "{kinit_cmd} {hbase_cmd} --config {hbase_conf_dir} {client_security_config} org.jruby.Main {region_mover} load {host}")

    Execute(regionmover_cmd,
            user=params.hbase_user,
            logoutput=True
            )



@OsFamilyImpl(os_family=OSConst.WINSRV_FAMILY)
class HbaseRegionServerWindows(HbaseRegionServer):
  def start(self, env):
    import status_params
    self.configure(env)
    Service(status_params.hbase_regionserver_win_service_name, action="start")

  def stop(self, env):
    import status_params
    env.set_params(status_params)
    Service(status_params.hbase_regionserver_win_service_name, action="stop")

  def status(self, env):
    import status_params
    env.set_params(status_params)
    check_windows_service_status(status_params.hbase_regionserver_win_service_name)



@OsFamilyImpl(os_family=OsFamilyImpl.DEFAULT)
class HbaseRegionServerDefault(HbaseRegionServer):
  def pre_upgrade_restart(self, env, upgrade_type=None):
    import params
    env.set_params(params)
    upgrade.prestart(env)

  def post_upgrade_restart(self, env, upgrade_type=None):
    import params
    env.set_params(params)
    upgrade.post_regionserver(env)

  def start(self, env, upgrade_type=None):
    import params
    env.set_params(params)
    self.configure(env) # for security
    setup_ranger_hbase(upgrade_type=upgrade_type, service_name="hbase-regionserver")

    if upgrade_type == UPGRADE_TYPE_ROLLING and len(params.rs_hosts) > 5:
      self.graceful_start(env)
    else:
      hbase_service('regionserver',
                    action='start'
                    )

  def stop(self, env, upgrade_type=None):
    import params
    env.set_params(params)

    if upgrade_type == UPGRADE_TYPE_ROLLING and len(params.rs_hosts) > 5:
      self.graceful_stop(env)
    else:
      hbase_service('regionserver',
                    action='stop'
                    )

  def status(self, env):
    import status_params
    env.set_params(status_params)

    check_process_status(status_params.regionserver_pid_file)

  def get_log_folder(self):
    import params
    return params.log_dir
  
  def get_user(self):
    import params
    return params.hbase_user

  def get_pid_files(self):
    import status_params
    return [status_params.regionserver_pid_file]

if __name__ == "__main__":
  HbaseRegionServer().execute()
