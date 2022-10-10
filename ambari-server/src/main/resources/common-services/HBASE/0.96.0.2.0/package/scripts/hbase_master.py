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

from resource_management.core.resources.system import Execute, File
from resource_management.core.source import StaticFile
from resource_management.libraries.script.script import Script
from resource_management.libraries.functions.format import format
from resource_management.libraries.functions.check_process_status import check_process_status
from resource_management.libraries.functions.security_commons import build_expectations, \
  cached_kinit_executor, get_params_from_filesystem, validate_security_config_properties, \
  FILE_TYPE_XML
from hbase import hbase
from hbase_service import hbase_service
from hbase_decommission import hbase_decommission
import upgrade
from setup_ranger_hbase import setup_ranger_hbase
from ambari_commons import OSCheck, OSConst
from ambari_commons.os_family_impl import OsFamilyImpl


class HbaseMaster(Script):
  def configure(self, env):
    import params
    env.set_params(params)
    hbase(name='master')

  def install(self, env):
    import params
    env.set_params(params)
    self.install_packages(env)

  def decommission(self, env):
    import params
    env.set_params(params)
    hbase_decommission(env)


@OsFamilyImpl(os_family=OSConst.WINSRV_FAMILY)
class HbaseMasterWindows(HbaseMaster):
  def start(self, env):
    import status_params
    self.configure(env)
    Service(status_params.hbase_master_win_service_name, action="start")

  def stop(self, env):
    import status_params
    env.set_params(status_params)
    Service(status_params.hbase_master_win_service_name, action="stop")

  def status(self, env):
    import status_params
    env.set_params(status_params)
    check_windows_service_status(status_params.hbase_master_win_service_name)

  def stop_replication(self, env):
    import params
    env.set_params(params)
    File(params.hbase_replication, content=StaticFile("hbase_replication.rb"), owner=params.hbase_user, mode="f")
    remove_replication_values = "remove {0}".format(params.hbase_replication_peers)
    replication_cmd = format(
      "cmd /c {hbase_executable} org.jruby.Main {hbase_replication} " + remove_replication_values)
    Execute(replication_cmd, user=params.hbase_user, logoutput=True)

  def update_replication(self, env):
    import params
    env.set_params(params)
    File(params.hbase_replication, content=StaticFile("hbase_replication.rb"), owner=params.hbase_user, mode="f")
    update_replication_values = "update {0} {1}".format(params.hbase_replication_peers,
                                                        params.hbase_replication_cluster_keys)
    update_replication_cmd = format(
      "cmd /c {hbase_executable} org.jruby.Main {hbase_replication} " + update_replication_values)
    Execute(update_replication_cmd, user=params.hbase_user, logoutput=True)


@OsFamilyImpl(os_family=OsFamilyImpl.DEFAULT)
class HbaseMasterDefault(HbaseMaster):
  def pre_upgrade_restart(self, env, upgrade_type=None):
    import params
    env.set_params(params)
    upgrade.prestart(env)

  def start(self, env, upgrade_type=None):
    import params
    env.set_params(params)
    self.configure(env) # for security
    setup_ranger_hbase(upgrade_type=upgrade_type, service_name="hbase-master")
    hbase_service('master', action = 'start')
    
  def stop(self, env, upgrade_type=None):
    import params
    env.set_params(params)
    hbase_service('master', action = 'stop')

  def status(self, env):
    import status_params
    env.set_params(status_params)

    check_process_status(status_params.hbase_master_pid_file)

  def stop_replication(self, env, upgrade_type=None):
    import params
    env.set_params(params)
    File(params.hbase_replication, content=StaticFile("hbase_replication.rb"), owner=params.hbase_user, mode=0755)
    remove_replication_values = "remove {0}".format(params.hbase_replication_peers)
    replication_cmd = format(
      "{kinit_cmd} {hbase_cmd} --config {hbase_conf_dir} {master_security_config} org.jruby.Main {hbase_replication} "
      + remove_replication_values)
    Execute(replication_cmd, user=params.hbase_user, logoutput=True)

  def update_replication(self, env, upgrade_type=None):
    import params
    env.set_params(params)
    File(params.hbase_replication, content=StaticFile("hbase_replication.rb"), owner=params.hbase_user, mode=0755)
    update_replication_values = "update {0} {1}".format(params.hbase_replication_peers,
                                                        params.hbase_replication_cluster_keys)
    update_replication_cmd = format(
      "{kinit_cmd} {hbase_cmd} --config {hbase_conf_dir} {master_security_config} org.jruby.Main {hbase_replication} "
      + update_replication_values)
    Execute(update_replication_cmd, user=params.hbase_user, logoutput=True)

  def get_log_folder(self):
    import params
    return params.log_dir
  
  def get_user(self):
    import params
    return params.hbase_user

  def get_pid_files(self):
    import status_params
    return [status_params.hbase_master_pid_file]

if __name__ == "__main__":
  HbaseMaster().execute()
