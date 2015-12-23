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

from hcat import hcat
from ambari_commons import OSConst
from ambari_commons.os_family_impl import OsFamilyImpl
from resource_management.core.logger import Logger
from resource_management.core.exceptions import ClientComponentHasNoStatus
from resource_management.libraries.functions import hdp_select
from resource_management.libraries.functions.version import compare_versions
from resource_management.libraries.script.script import Script


class HCatClient(Script):
  def install(self, env):
    import params
    self.install_packages(env, exclude_packages=params.hive_exclude_packages)
    self.configure(env)

  def configure(self, env):
    import params
    env.set_params(params)
    hcat()

  def status(self, env):
    raise ClientComponentHasNoStatus()


@OsFamilyImpl(os_family=OSConst.WINSRV_FAMILY)
class HCatClientWindows(HCatClient):
  pass


@OsFamilyImpl(os_family=OsFamilyImpl.DEFAULT)
class HCatClientDefault(HCatClient):
  def get_stack_to_component(self):
    # HCat client doesn't have a first-class entry in hdp-select. Since clients always
    # update after daemons, this ensures that the hcat directories are correct on hosts
    # which do not include the WebHCat daemon
    return {"HDP": "hive-webhcat"}


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
    hdp_select.select("hive-webhcat", params.version)


if __name__ == "__main__":
  HCatClient().execute()
