#!/usr/bin/env python3
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
from resource_management.libraries.resources.xml_config import XmlConfig
from resource_management.libraries.functions.format import format
from resource_management.core.source import InlineTemplate
from ambari_commons.os_family_impl import OsFamilyFuncImpl, OsFamilyImpl

import tez_utils


@OsFamilyFuncImpl(os_family=OsFamilyImpl.DEFAULT)
def tez(config_dir):
  """
  Write out tez-site.xml and tez-env.sh to the config directory.
  :param config_dir: Which config directory to save configs to, which is different during rolling upgrade.
  """
  import params

  if config_dir is None:
    config_dir = params.tez_conf_dir
  tez_utils.validate_absolute_path(config_dir, "Tez configuration directory")

  Directory(
    config_dir,
    owner=params.tez_user,
    group=params.user_group,
    mode=0o755,
    create_parents=True,
  )

  XmlConfig(
    "tez-site.xml",
    conf_dir=config_dir,
    configurations=params.tez_site_config,
    configuration_attributes=params.config.get("configurationAttributes", {}).get(
      "tez-site", {}
    ),
    owner=params.tez_user,
    group=params.user_group,
    mode=0o644,
  )

  tez_env_file_path = os.path.join(config_dir, "tez-env.sh")
  File(
    tez_env_file_path,
    owner=params.tez_user,
    group=params.user_group,
    content=InlineTemplate(params.tez_env_sh_template),
    mode=0o644,
  )
