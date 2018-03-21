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
from resource_management.libraries.script.script import Script
from resource_management.libraries.functions import stack_select
from resource_management.libraries.functions.constants import StackFeature
from resource_management.libraries.functions.stack_features import check_stack_feature
from resource_management.core.logger import Logger
from resource_management.core.exceptions import ClientComponentHasNoStatus

from oozie import oozie
from oozie_service import oozie_service


class OozieClient(Script):

  def install(self, env):
    self.install_packages(env)
    self.configure(env)


  def configure(self, env):
    import params
    env.set_params(params)

    oozie(is_server=False)

  def status(self, env):
    raise ClientComponentHasNoStatus()


  def pre_upgrade_restart(self, env, upgrade_type=None):
    import params
    env.set_params(params)

    # this function should not execute if the version can't be determined or
    # the stack does not support rolling upgrade
    if not (params.version and check_stack_feature(StackFeature.ROLLING_UPGRADE, params.version)):
      return

    Logger.info("Executing Oozie Client Stack Upgrade pre-restart")
    stack_select.select_packages(params.version)

  # We substitute some configs (oozie.authentication.kerberos.principal) before generation (see oozie.py and params.py).
  # This function returns changed configs (it's used for config generation before config download)
  def generate_configs_get_xml_file_content(self, filename, dictionary):
    if dictionary == 'oozie-site':
      import params
      config = self.get_config()
      return {'configurations': params.oozie_site,
              'configurationAttributes': config['configurationAttributes'][dictionary]}
    else:
      return super(OozieClient, self).generate_configs_get_xml_file_content(filename, dictionary)

if __name__ == "__main__":
  OozieClient().execute()
