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
# Python Imports
import os

# Local Imports
from resource_management.core.resources.system import Directory, File
from resource_management.libraries.functions import lzo_utils
from resource_management.libraries.resources.xml_config import XmlConfig
from resource_management.core.source import InlineTemplate
from ambari_commons import OSConst
from ambari_commons.os_family_impl import OsFamilyFuncImpl, OsFamilyImpl

@OsFamilyFuncImpl(os_family=OsFamilyImpl.DEFAULT)
def tez(config_dir):
  """
  Write out tez-site.xml and tez-env.sh to the config directory.
  :param config_dir: Which config directory to save configs to, which is different during rolling upgrade.
  """
  import params

  # ensure that matching LZO libraries are installed for Tez
  lzo_utils.install_lzo_if_needed()

  Directory(params.tez_etc_dir, mode=0755)

  Directory(config_dir,
            owner = params.tez_user,
            group = params.user_group,
            create_parents = True)

  XmlConfig( "tez-site.xml",
             conf_dir = config_dir,
             configurations = params.config['configurations']['tez-site'],
             configuration_attributes=params.config['configuration_attributes']['tez-site'],
             owner = params.tez_user,
             group = params.user_group,
             mode = 0664)

  tez_env_file_path = os.path.join(config_dir, "tez-env.sh")
  File(tez_env_file_path,
       owner=params.tez_user,
       content=InlineTemplate(params.tez_env_sh_template),
       mode=0555)


@OsFamilyFuncImpl(os_family=OSConst.WINSRV_FAMILY)
def tez(config_dir):
  """
  Write out tez-site.xml and tez-env.sh to the config directory.
  :param config_dir: Directory to write configs to.
  """
  import params
  XmlConfig("tez-site.xml",
             conf_dir=config_dir,
             configurations=params.config['configurations']['tez-site'],
             owner=params.tez_user,
             mode="f",
             configuration_attributes=params.config['configuration_attributes']['tez-site']
  )

