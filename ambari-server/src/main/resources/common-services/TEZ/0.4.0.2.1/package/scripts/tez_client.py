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

Ambari Agent

"""
import os
import urlparse

from ambari_commons import OSConst
from ambari_commons.inet_utils import download_file
from ambari_commons.os_family_impl import OsFamilyImpl
from ambari_commons.os_utils import copy_file, extract_path_component

from resource_management.core.exceptions import ClientComponentHasNoStatus
from resource_management.core.source import InlineTemplate
from resource_management.libraries.functions import conf_select
from resource_management.libraries.functions import stack_select
from resource_management.libraries.functions import StackFeature
from resource_management.libraries.functions.stack_features import check_stack_feature
from resource_management.libraries.functions.get_stack_version import get_stack_version
from resource_management.libraries.script.script import Script
from resource_management.libraries.functions.default import default

from tez import tez

class TezClient(Script):
  def configure(self, env):
    import params
    env.set_params(params)
    tez()

  def status(self, env):
    raise ClientComponentHasNoStatus()

@OsFamilyImpl(os_family=OsFamilyImpl.DEFAULT)
class TezClientLinux(TezClient):

  def get_component_name(self):
    return "hadoop-client"

  def pre_upgrade_restart(self, env, upgrade_type=None):
    import params
    env.set_params(params)

    if params.version and check_stack_feature(StackFeature.ROLLING_UPGRADE, params.version):
      conf_select.select(params.stack_name, "tez", params.version)
      conf_select.select(params.stack_name, "hadoop", params.version)
      stack_select.select("hadoop-client", params.version)

  def install(self, env):
    self.install_packages(env)
    self.configure(env)

@OsFamilyImpl(os_family=OSConst.WINSRV_FAMILY)
class TezClientWindows(TezClient):
  def install(self, env):
    import params
    if params.tez_home_dir is None:
      self.install_packages(env)
      params.refresh_tez_state_dependent_params()
    env.set_params(params)
    self._install_lzo_support_if_needed(params)
    self.configure(env)

  def _install_lzo_support_if_needed(self, params):
    hadoop_classpath_prefix = self._expand_hadoop_classpath_prefix(params.hadoop_classpath_prefix_template, params.config['configurations']['tez-site'])

    hadoop_lzo_dest_path = extract_path_component(hadoop_classpath_prefix, "hadoop-lzo-")
    if hadoop_lzo_dest_path:
      hadoop_lzo_file = os.path.split(hadoop_lzo_dest_path)[1]

      config = Script.get_config()
      file_url = urlparse.urljoin(config['hostLevelParams']['jdk_location'], hadoop_lzo_file)
      hadoop_lzo_dl_path = os.path.join(config["hostLevelParams"]["agentCacheDir"], hadoop_lzo_file)
      download_file(file_url, hadoop_lzo_dl_path)
      #This is for protection against configuration changes. It will infect every new destination with the lzo jar,
      # but since the classpath points to the jar directly we're getting away with it.
      if not os.path.exists(hadoop_lzo_dest_path):
        copy_file(hadoop_lzo_dl_path, hadoop_lzo_dest_path)

  def _expand_hadoop_classpath_prefix(self, hadoop_classpath_prefix_template, configurations):
    import resource_management

    hadoop_classpath_prefix_obj = InlineTemplate(hadoop_classpath_prefix_template, configurations_dict=configurations,
                                                 extra_imports=[resource_management, resource_management.core,
                                                                resource_management.core.source])
    hadoop_classpath_prefix = hadoop_classpath_prefix_obj.get_content()
    return hadoop_classpath_prefix

if __name__ == "__main__":
  TezClient().execute()
