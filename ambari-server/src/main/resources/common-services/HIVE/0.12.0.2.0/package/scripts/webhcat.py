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
import sys
import os.path

from resource_management import *
from resource_management.core.resources.system import Execute
from resource_management.libraries.functions.version import compare_versions


def webhcat(env):
  import params

  Directory(params.templeton_pid_dir,
            owner=params.webhcat_user,
            mode=0755,
            group=params.user_group,
            recursive=True)

  Directory(params.templeton_log_dir,
            owner=params.webhcat_user,
            mode=0755,
            group=params.user_group,
            recursive=True)

  Directory(params.config_dir,
            recursive=True,
            owner=params.webhcat_user,
            group=params.user_group)

  if params.security_enabled:
    kinit_if_needed = format("{kinit_path_local} -kt {hdfs_user_keytab} {hdfs_principal_name};")
  else:
    kinit_if_needed = ""

  if kinit_if_needed:
    Execute(kinit_if_needed,
            user=params.webhcat_user,
            path='/bin'
    )

  XmlConfig("webhcat-site.xml",
            conf_dir=params.config_dir,
            configurations=params.config['configurations']['webhcat-site'],
            configuration_attributes=params.config['configuration_attributes']['webhcat-site'],
            owner=params.webhcat_user,
            group=params.user_group,
            )

  File(format("{config_dir}/webhcat-env.sh"),
       owner=params.webhcat_user,
       group=params.user_group,
       content=InlineTemplate(params.webhcat_env_sh_template)
  )
