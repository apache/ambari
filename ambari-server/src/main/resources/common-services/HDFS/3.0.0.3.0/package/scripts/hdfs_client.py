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
from resource_management.libraries.functions import stack_select
from resource_management.libraries.functions.constants import StackFeature
from resource_management.libraries.functions.stack_features import check_stack_feature
from resource_management.libraries.functions.security_commons import build_expectations, \
  cached_kinit_executor, get_params_from_filesystem, validate_security_config_properties, \
  FILE_TYPE_XML
from hdfs import hdfs
from ambari_commons.os_family_impl import OsFamilyImpl
from ambari_commons import OSConst
from resource_management.core.exceptions import ClientComponentHasNoStatus

class HdfsClient(Script):

  def install(self, env):
    import params
    env.set_params(params)
    self.install_packages(env)
    self.configure(env)

  def configure(self, env):
    import params
    env.set_params(params)
    hdfs()

  def save_configs(self, env):
    import params
    env.set_params(params)
    hdfs()

  def start(self, env, upgrade_type=None):
    import params
    env.set_params(params)

  def stop(self, env, upgrade_type=None):
    import params
    env.set_params(params)

  def status(self, env):
    raise ClientComponentHasNoStatus()

@OsFamilyImpl(os_family=OsFamilyImpl.DEFAULT)
class HdfsClientDefault(HdfsClient):

  def pre_upgrade_restart(self, env, upgrade_type=None):
    import params
    env.set_params(params)
    if params.version and check_stack_feature(StackFeature.ROLLING_UPGRADE, params.version):
      stack_select.select_packages(params.version)

@OsFamilyImpl(os_family=OSConst.WINSRV_FAMILY)
class HdfsClientWindows(HdfsClient):
  def install(self, env):
    import install_params
    self.install_packages(env)
    self.configure(env)

if __name__ == "__main__":
  HdfsClient().execute()
