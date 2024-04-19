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

from resource_management.libraries.script.script import Script
from resource_management.libraries.functions.check_process_status import check_process_status
from resource_management.libraries.functions.format import format
from resource_management.libraries.functions import stack_select
from resource_management.libraries.functions.constants import Direction
from resource_management.libraries.functions.security_commons import build_expectations
from resource_management.libraries.functions.security_commons import cached_kinit_executor
from resource_management.libraries.functions.security_commons import validate_security_config_properties
from resource_management.libraries.functions.security_commons import get_params_from_filesystem
from resource_management.libraries.functions.security_commons import FILE_TYPE_XML
from resource_management.libraries.functions.show_logs import show_logs
from resource_management.core.resources.system import File, Execute, Link
from resource_management.core.resources.service import Service
from resource_management.core.logger import Logger


from ambari_commons import OSConst, OSCheck
from ambari_commons.os_family_impl import OsFamilyImpl
from hdfs_dfsrouter import *


class DfsRouter(Script):
    def configure(self, env):
        import params
        env.set_params(params)
        dfsrouter(action="configure")

    def install(self, env):
        import params
        env.set_params(params)
        self.install_packages(env)

    def start(self, env, upgrade_type=None):
        import params
        env.set_params(params)
        self.configure(env)
        dfsrouter(action="start")

    def stop(self, env, upgrade_type=None):
        import params
        env.set_params(params)
        dfsrouter(action="stop")

    def status(self, env):
        import status_params
        env.set_params(status_params)
        dfsrouter(action="status")


if __name__ == '__main__':
    DfsRouter().execute()
