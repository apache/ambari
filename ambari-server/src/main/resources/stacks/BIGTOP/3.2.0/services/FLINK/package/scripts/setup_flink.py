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
# Python Imports
import os

# Local Imports
from resource_management.core.resources.system import Directory, File,Link
from resource_management.core.source import InlineTemplate
from ambari_commons.os_family_impl import OsFamilyFuncImpl, OsFamilyImpl

@OsFamilyFuncImpl(os_family=OsFamilyImpl.DEFAULT)
def setup_flink(env, type, upgrade_type = None, action = None):
  import params

  Directory(params.flink_pid_dir,
            owner=params.flink_user,
            group=params.user_group,
            mode=0775,
            create_parents = True
  )

  Directory(params.flink_etc_dir, mode=0755)
  Directory(params.flink_config_dir,
            owner = params.flink_user,
            group = params.user_group,
            create_parents = True)

  Directory(params.flink_log_dir,mode=0767)
  Link(params.flink_dir + '/log',to=params.flink_log_dir)

  if type == 'historyserver' and action == 'config':
    params.HdfsResource(params.flink_hdfs_user_dir,
                     type="directory",
                     action="create_on_execute",
                     owner=params.flink_user,
                     mode=0775
    )

    params.HdfsResource(None, action="execute")

  flink_conf_file_path = os.path.join(params.flink_config_dir, "flink-conf.yaml")
  File(flink_conf_file_path,
       owner=params.flink_user,
       group = params.flink_group,
       content=InlineTemplate(params.flink_conf_template),
       mode=0755)

  #create log4j.properties in /etc/conf dir
  File(os.path.join(params.flink_config_dir, 'log4j.properties'),
       owner=params.flink_user,
       group=params.flink_group,
       content=params.flink_log4j_properties,
       mode=0644,
  )

  #create log4j-cli.properties in /etc/conf dir
  File(os.path.join(params.flink_config_dir, 'log4j-cli.properties'),
       owner=params.flink_user,
       group=params.flink_group,
       content=params.flink_log4j_cli_properties,
       mode=0644,
  )

  #create log4j-console.properties in /etc/conf dir
  File(os.path.join(params.flink_config_dir, 'log4j-console.properties'),
       owner=params.flink_user,
       group=params.flink_group,
       content=params.flink_log4j_console_properties,
       mode=0644,
  )

  #create log4j-session.properties in /etc/conf dir
  File(os.path.join(params.flink_config_dir, 'log4j-session.properties'),
       owner=params.flink_user,
       group=params.flink_group,
       content=params.flink_log4j_session_properties,
       mode=0644,
  )
