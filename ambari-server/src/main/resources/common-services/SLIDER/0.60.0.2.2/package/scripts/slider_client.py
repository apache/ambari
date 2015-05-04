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

from resource_management import *
from resource_management.libraries.functions import conf_select
from slider import slider
from ambari_commons import OSConst
from ambari_commons.os_family_impl import OsFamilyFuncImpl, OsFamilyImpl

class SliderClient(Script):

  @OsFamilyFuncImpl(os_family=OsFamilyImpl.DEFAULT)
  def get_stack_to_component(self):
    return {"HDP": "slider-client"}

  @OsFamilyFuncImpl(os_family=OsFamilyImpl.DEFAULT)
  def pre_rolling_restart(self, env):
    import params
    env.set_params(params)

    if params.version and compare_versions(format_hdp_stack_version(params.version), '2.2.0.0') >= 0:
      conf_select.select(params.stack_name, "slider", params.version)      
      Execute(format("hdp-select set slider-client {version}"))

      # also set all of the hadoop clients since slider client is upgraded as
      # part of the final "CLIENTS" group and we need to ensure that
      # hadoop-client is also set
      conf_select.select(params.stack_name, "hadoop", params.version)      
      Execute(format("hdp-select set hadoop-client {version}"))

  @OsFamilyFuncImpl(os_family=OsFamilyImpl.DEFAULT)
  def install(self, env):
    self.install_packages(env)
    self.configure(env)

  @OsFamilyFuncImpl(os_family=OSConst.WINSRV_FAMILY)
  def install(self, env):
    import params
    if params.slider_home is None:
      self.install_packages(env)
    self.configure(env)

  @OsFamilyFuncImpl(os_family=OsFamilyImpl.DEFAULT)
  def configure(self, env):
    import params
    env.set_params(params)
    slider()

  def status(self, env):
    raise ClientComponentHasNoStatus()


if __name__ == "__main__":
  SliderClient().execute()
