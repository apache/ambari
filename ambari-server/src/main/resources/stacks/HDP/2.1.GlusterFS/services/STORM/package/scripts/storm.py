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
from yaml_config import yaml_config
import sys


def storm():
  import params

  Directory([params.log_dir, params.pid_dir, params.local_dir, params.conf_dir],
            owner=params.storm_user,
            group=params.user_group,
            recursive=True
  )

  File(format("{conf_dir}/config.yaml"),
       content=Template("config.yaml.j2"),
       owner=params.storm_user,
       group=params.user_group
  )

  yaml_config("storm.yaml",
              conf_dir=params.conf_dir,
              configurations=params.config['configurations']['storm-site'],
              owner=params.storm_user,
              group=params.user_group
  )

  TemplateConfig(format("{conf_dir}/storm-env.sh"),
                 owner=params.storm_user
  )

  if params.security_enabled:
    TemplateConfig(format("{conf_dir}/storm_jaas.conf"),
                   owner=params.storm_user
    )