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

from resource_management import *
from resource_management.libraries.script.config_dictionary import MutableConfigDictionary
from resource_management.libraries.functions.dynamic_variable_interpretation import interpret_dynamic_version_property


def tez():
  import params

  Directory(params.config_dir,
            owner = params.tez_user,
            group = params.user_group,
            recursive = True
  )

  mutable_configs = MutableConfigDictionary(params.config)
  tez_lib_uris = params.config['configurations']['tez-site']['tez.lib.uris']
  if tez_lib_uris:
    found_at_least_one_replacement, new_tez_lib_uris = interpret_dynamic_version_property(tez_lib_uris, "tez", ",")
    if found_at_least_one_replacement:
      mutable_configs['configurations']['tez-site']['tez.lib.uris'] = new_tez_lib_uris

  XmlConfig( "tez-site.xml",
             conf_dir = params.config_dir,
             configurations = mutable_configs['configurations']['tez-site'],
             configuration_attributes=params.config['configuration_attributes']['tez-site'],
             owner = params.tez_user,
             group = params.user_group,
             mode = 0664
  )

  File(format("{config_dir}/tez-env.sh"),
       owner=params.tez_user,
       content=InlineTemplate(params.tez_env_sh_template)
  )


def tez_TemplateConfig(name):
  import params

  if not isinstance(name, list):
    name = [name]

  for x in name:
    TemplateConfig(format("{config_dir}/{x}"),
                   owner = params.tez_user
    )

