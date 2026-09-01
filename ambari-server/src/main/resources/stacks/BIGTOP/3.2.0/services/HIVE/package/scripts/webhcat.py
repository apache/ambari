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

import os.path
from resource_management.core.resources.system import Directory, File
from resource_management.core.source import InlineTemplate
from resource_management.libraries.resources.xml_config import XmlConfig
from resource_management.libraries.functions.format import format
from resource_management.libraries.functions.setup_atlas_hook import (
  setup_atlas_hook,
)
from ambari_commons.constants import SERVICE


def webhcat():
  import params

  Directory(
    params.templeton_pid_dir,
    owner=params.webhcat_user,
    mode=0o2750,
    group=params.user_group,
    create_parents=True,
  )

  Directory(
    params.templeton_log_dir,
    owner=params.webhcat_user,
    mode=0o750,
    group=params.user_group,
    create_parents=True,
  )

  Directory(
    params.webhcat_conf_dir,
    create_parents=True,
    owner="root",
    group=params.user_group,
    mode=0o755,
  )

  # Replace _HOST with hostname in relevant principal-related properties
  webhcat_site = params.config["configurations"]["webhcat-site"].copy()
  for prop_name in ["templeton.hive.properties", "templeton.kerberos.principal"]:
    if prop_name in webhcat_site:
      webhcat_site[prop_name] = webhcat_site[prop_name].replace(
        "_HOST", params.hostname
      )

  XmlConfig(
    "webhcat-site.xml",
    conf_dir=params.webhcat_conf_dir,
    configurations=webhcat_site,
    configuration_attributes=params.config["configurationAttributes"]["webhcat-site"],
    owner="root",
    group=params.user_group,
    mode=0o640,
  )

  File(
    format("{webhcat_conf_dir}/webhcat-env.sh"),
    owner="root",
    group=params.user_group,
    content=InlineTemplate(params.webhcat_env_sh_template),
    mode=0o640,
  )

  log4j_webhcat_filename = "webhcat-log4j.properties"
  if params.log4j_webhcat_props is not None:
    File(
      format("{webhcat_conf_dir}/{log4j_webhcat_filename}"),
      mode=0o644,
      group=params.user_group,
      owner="root",
      content=InlineTemplate(params.log4j_webhcat_props),
    )
  # Generate atlas-application.properties.xml file
  if params.enable_atlas_hook:
    # WebHCat uses a different config dir than the rest of the daemons in Hive.
    atlas_hook_filepath = os.path.join(
      params.webhcat_conf_dir, params.atlas_hook_filename
    )
    setup_atlas_hook(
      SERVICE.HIVE,
      params.hive_atlas_application_properties,
      atlas_hook_filepath,
      "root",
      params.user_group,
    )
