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

from resource_management.core.resources import File
from resource_management.core.resources import Execute
from resource_management.core.resources import Directory
from resource_management.core.source import InlineTemplate
from resource_management.libraries.resources.template_config import TemplateConfig
from resource_management.libraries.functions.format import format
from resource_management.core.source import Template
from resource_management.libraries.functions import compare_versions
from yaml_utils import escape_yaml_propetry

def storm():
  import params

  Directory(params.log_dir,
            owner=params.storm_user,
            group=params.user_group,
            mode=0777,
            recursive=True
  )

  Directory([params.pid_dir, params.local_dir],
            owner=params.storm_user,
            group=params.user_group,
            recursive=True,
            cd_access="a",
  )

  Directory(params.conf_dir,
            group=params.user_group,
            recursive=True,
            cd_access="a",
  )

  File(format("{conf_dir}/config.yaml"),
       content=Template("config.yaml.j2"),
       owner=params.storm_user,
       group=params.user_group
  )

  configurations = params.config['configurations']['storm-site']
  
  File(format("{conf_dir}/storm.yaml"),
       content=Template(
                        "storm.yaml.j2", 
                         extra_imports=[escape_yaml_propetry], 
                        configurations = configurations),
       owner=params.storm_user,
       group=params.user_group
  )

  if params.has_metric_collector:
    File(format("{conf_dir}/storm-metrics2.properties"),
        owner=params.storm_user,
        group=params.user_group,
        content=Template("storm-metrics2.properties.j2")
    )

    Execute(format("{sudo} ln -s {metric_collector_sink_jar} {storm_lib_dir}/ambari-metrics-storm-sink.jar"),
            not_if=format("ls {storm_lib_dir}/ambari-metrics-storm-sink.jar"),
            only_if=format("ls {metric_collector_sink_jar}")
    )

  File(format("{conf_dir}/storm-env.sh"),
    owner=params.storm_user,
    content=InlineTemplate(params.storm_env_sh_template)
  )

  if params.security_enabled:
    TemplateConfig(format("{conf_dir}/storm_jaas.conf"),
                   owner=params.storm_user
    )
    if params.hdp_stack_version != "" and compare_versions(params.hdp_stack_version, '2.2') >= 0:
      TemplateConfig(format("{conf_dir}/client_jaas.conf"),
                     owner=params.storm_user
      )
      minRuid = configurations['_storm.min.ruid'] if configurations.has_key('_storm.min.ruid') else ''
      
      min_user_ruid = int(minRuid) if minRuid.isdigit() else _find_real_user_min_uid()
      
      File(format("{conf_dir}/worker-launcher.cfg"),
           content=Template("worker-launcher.cfg.j2", min_user_ruid = min_user_ruid),
           owner='root',
           group=params.user_group
      )


'''
Finds minimal real user UID
'''
def _find_real_user_min_uid():
  with open('/etc/login.defs') as f:
    for line in f:
      if line.strip().startswith('UID_MIN') and len(line.split()) == 2 and line.split()[1].isdigit():
        return int(line.split()[1])
  raise Fail("Unable to find UID_MIN in file /etc/login.defs. Expecting format e.g.: 'UID_MIN    500'")  
