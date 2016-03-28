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


from resource_management.libraries.script.script import Script
from resource_management.libraries.resources.hdfs_resource import HdfsResource
from resource_management.libraries.functions import conf_select
from resource_management.libraries.functions import stack_select
from resource_management.libraries.functions import format
from resource_management.libraries.functions.copy_tarball import copy_to_hdfs
from resource_management.libraries.functions.get_stack_version import get_stack_version
from resource_management.libraries.functions.check_process_status import check_process_status
from resource_management.libraries.functions.version import compare_versions, format_stack_version
from resource_management.libraries.functions.security_commons import build_expectations, \
    cached_kinit_executor, get_params_from_filesystem, validate_security_config_properties, \
    FILE_TYPE_XML
from resource_management.core.resources.system import File, Execute
from resource_management.core import shell
from subprocess import call
from ambari_commons import OSCheck, OSConst
from setup_ranger_hive import setup_ranger_hive
from ambari_commons.os_family_impl import OsFamilyImpl
from ambari_commons.constants import UPGRADE_TYPE_ROLLING
from resource_management.core.logger import Logger
from hive_service_interactive import hive_service_interactive, stop_llap, start_llap
from hive_interactive import hive_interactive

class HiveServerInteractive(Script):
    def install(self, env):
      import params
      self.install_packages(env)

    def configure(self, env):
      import params
      env.set_params(params)
      hive_interactive(name='hiveserver2')

@OsFamilyImpl(os_family=OSConst.WINSRV_FAMILY)
class HiveServerWindows(HiveServerInteractive):
    def start(self, env):
      pass

    def stop(self, env):
      pass

    def status(self, env):
      pass


@OsFamilyImpl(os_family=OsFamilyImpl.DEFAULT)
class HiveServerDefault(HiveServerInteractive):
    def get_stack_to_component(self):
      return {"HDP": "hive-server2-hive2"}

    def start(self, env, upgrade_type=None):
      import params
      env.set_params(params)
      self.configure(env)

      # TODO : We need have conditional [re]start of LLAP once "status check command" for LLAP is ready.
      # Check status and based on that decide on [re]starting.

      # Start LLAP before Hive Server Interactive start
      status = start_llap(self)
      if status:
        # TODO : test the workability of Ranger and Hive2 during upgrade
        # setup_ranger_hive(upgrade_type=upgrade_type)

        hive_service_interactive('hiveserver2', action='start', upgrade_type=upgrade_type)
      else:
        Logger.info("Skipping start of Hive Server Interactive due to LLAP start issue.")

    def stop(self, env, upgrade_type=None):
      import params
      env.set_params(params)

      # Stop Hive Interactive Server first
      # TODO : Upgrade check comes here.
      hive_service_interactive('hiveserver2', action = 'stop')

      stop_llap(self)

    def status(self, env):
      import status_params
      env.set_params(status_params)
      pid_file = format("{hive_pid_dir}/{hive_interactive_pid}")

      # Recursively check all existing gmetad pid files
      check_process_status(pid_file)
      # TODO : Check the LLAP app status as well.

    def pre_upgrade_restart(self, env, upgrade_type=None):
      # TODO: Make sure, the tez_hive2 is upgraded, while writing the upgrade code.
      pass

    def security_status(self, env):
      pass


if __name__ == "__main__":
    HiveServerInteractive().execute()