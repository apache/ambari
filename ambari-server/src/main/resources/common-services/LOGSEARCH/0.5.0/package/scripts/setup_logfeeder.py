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

from resource_management.core.resources.system import Directory, File
from resource_management.libraries.functions.format import format
from resource_management.core.source import InlineTemplate, Template


def setup_logfeeder():
  import params

  Directory([params.logfeeder_log_dir, params.logfeeder_pid_dir, params.logfeeder_dir,
             params.logsearch_logfeeder_conf, params.logfeeder_checkpoint_folder],
            mode=0755,
            cd_access='a',
            owner=params.logfeeder_user,
            group=params.logfeeder_group,
            create_parents=True
            )

  File(params.logfeeder_log,
       mode=0644,
       owner=params.logfeeder_user,
       group=params.logfeeder_group,
       content=''
       )

  File(format("{logsearch_logfeeder_conf}/logfeeder.properties"),
       content=Template("logfeeder.properties.j2"),
       owner=params.logfeeder_user
       )

  File(format("{logsearch_logfeeder_conf}/logfeeder-env.sh"),
       content=InlineTemplate(params.logfeeder_env_content),
       owner=params.logfeeder_user
       )

  File(format("{logsearch_logfeeder_conf}/log4j.xml"),
       content=InlineTemplate(params.logfeeder_log4j_content),
       owner=params.logfeeder_user
       )

  File(format("{logsearch_logfeeder_conf}/grok-patterns"),
       content=Template("grok-patterns.j2"),
       owner=params.logfeeder_user,
       encoding="utf-8"
       )

  for file_name in params.logfeeder_config_file_names:
    File(format("{logsearch_logfeeder_conf}/" + file_name),
         content=Template(file_name + ".j2"),
         owner=params.logfeeder_user
         )
