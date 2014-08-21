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
import sys

def sqoop(type=None):
  import params
  Link(params.sqoop_lib + "/mysql-connector-java.jar",
       to = '/usr/share/java/mysql-connector-java.jar'
  ) 
  Directory(params.sqoop_conf_dir,
            owner = params.sqoop_user,
            group = params.user_group
  )
  
  File(format("{sqoop_conf_dir}/sqoop-env.sh"),
    owner=params.sqoop_user,
    content=InlineTemplate(params.sqoop_env_sh_template)
  )
  
  File (params.sqoop_conf_dir + "/sqoop-env-template.sh",
          owner = params.sqoop_user,
          group = params.user_group
  )
  File (params.sqoop_conf_dir + "/sqoop-site-template.xml",
         owner = params.sqoop_user,
         group = params.user_group
  )
  File (params.sqoop_conf_dir + "/sqoop-site.xml",
         owner = params.sqoop_user,
         group = params.user_group
  )
  pass

def sqoop_TemplateConfig(name, tag=None):
  import params
  TemplateConfig( format("{sqoop_conf_dir}/{name}"),
                  owner = params.sqoop_user,
                  template_tag = tag
  )
