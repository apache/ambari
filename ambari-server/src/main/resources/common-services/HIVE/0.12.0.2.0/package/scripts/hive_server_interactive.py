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
from ambari_commons import OSCheck, OSConst
from setup_ranger_hive import setup_ranger_hive
from ambari_commons.os_family_impl import OsFamilyImpl
from ambari_commons.constants import UPGRADE_TYPE_ROLLING
from resource_management.core.logger import Logger

import hive_server_upgrade

class HiveServerInteractive(Script):
    def install(self, env):
      import params
      self.install_packages(env)

    def configure(self, env):
      pass

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
      pass

    def stop(self, env, upgrade_type=None):
      pass

    def status(self, env):
      pass

    def pre_upgrade_restart(self, env, upgrade_type=None):
      pass

    def security_status(self, env):
      pass


if __name__ == "__main__":
    HiveServerInteractive().execute()