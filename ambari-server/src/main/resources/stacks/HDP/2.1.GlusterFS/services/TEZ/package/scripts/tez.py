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

def tez():
  import params

  Directory(params.config_dir,
    owner = params.tez_user,
    group = params.user_group,
    create_parents = True
  )

  XmlConfig( "tez-site.xml",
            conf_dir = params.config_dir,
            configurations = params.config['configurations']['tez-site'],
            configuration_attributes=params.config['configurationAttributes']['tez-site'],
            owner = params.tez_user,
            group = params.user_group,
            mode = 0o664
  )

  tez_TemplateConfig( ['tez-env.sh'])


def tez_TemplateConfig(name):
  import params

  if not isinstance(name, list):
    name = [name]

  for x in name:
    TemplateConfig(format("{config_dir}/{x}"),
        owner = params.tez_user
    )

