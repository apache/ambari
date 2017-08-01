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
from resource_management import *
from resource_management.libraries.functions import stack_select
from hive import hive
from ambari_commons.os_family_impl import OsFamilyImpl
from ambari_commons import OSConst

class HiveClient(Script):
  def install(self, env):
    import params
    self.install_packages(env)
    self.configure(env)

  def status(self, env):
    raise ClientComponentHasNoStatus()

  def configure(self, env):
    import params
    env.set_params(params)
    hive(name='client')


@OsFamilyImpl(os_family=OsFamilyImpl.DEFAULT)
class HiveClientDefault(HiveClient):
  def pre_upgrade_restart(self, env, upgrade_type=None):
    import params
    env.set_params(params)

    if params.version and compare_versions(format_stack_version(params.version), '4.1.0.0') >= 0:
      stack_select.select_packages(params.version)

  def pre_upgrade_restart(self, env, upgrade_type=None):
    """
    Execute hdp-select before reconfiguring this client to the new HDP version.

    :param env:
    :param upgrade_type:
    :return:
    """
    Logger.info("Executing Hive HCat Client Stack Upgrade pre-restart")

    import params
    env.set_params(params)

    # this function should not execute if the version can't be determined or
    # is not at least HDP 2.2.0.0
    if not params.version or compare_versions(params.version, "2.2", format=True) < 0:
      return

    # HCat client doesn't have a first-class entry in hdp-select. Since clients always
    # update after daemons, this ensures that the hcat directories are correct on hosts
    # which do not include the WebHCat daemon
    stack_select.select_packages(params.version)

if __name__ == "__main__":
  HiveClient().execute()
