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
import os
from resource_management import *
from ambari_commons import OSConst
from ambari_commons.os_family_impl import OsFamilyFuncImpl, OsFamilyImpl

@OsFamilyFuncImpl(os_family=OSConst.WINSRV_FAMILY)
def slider():
  import params

  slider_client_config = params.config['configurations']['slider-client'] if 'configurations' in params.config and 'slider-client' in params.config['configurations'] else {}

  XmlConfig("slider-client.xml",
            conf_dir=params.slider_conf_dir,
            configurations=slider_client_config
  )

  if (params.log4j_props != None):
    File(os.path.join(params.slider_conf_dir, "log4j.properties"),
         content=params.log4j_props
    )
  elif (os.path.exists(os.path.join(params.slider_conf_dir, "log4j.properties"))):
    File(os.path.join(params.slider_conf_dir, "log4j.properties"))


@OsFamilyFuncImpl(os_family=OsFamilyImpl.DEFAULT)
def slider():
  import params

  Directory(params.slider_conf_dir,
            recursive=True
  )

  slider_client_config = params.config['configurations']['slider-client'] if 'configurations' in params.config and 'slider-client' in params.config['configurations'] else {}

  XmlConfig("slider-client.xml",
            conf_dir=params.slider_conf_dir,
            configurations=slider_client_config,
            mode=0644
  )

  File(format("{slider_conf_dir}/slider-env.sh"),
       mode=0755,
       content=InlineTemplate(params.slider_env_sh_template)
  )

  Directory(params.storm_slider_conf_dir,
            recursive=True
  )

  File(format("{storm_slider_conf_dir}/storm-slider-env.sh"),
       mode=0755,
       content=Template('storm-slider-env.sh.j2')
  )

  if (params.log4j_props != None):
    File(format("{params.slider_conf_dir}/log4j.properties"),
         mode=0644,
         content=params.log4j_props
    )
  elif (os.path.exists(format("{params.slider_conf_dir}/log4j.properties"))):
    File(format("{params.slider_conf_dir}/log4j.properties"),
         mode=0644
    )
  if Script.is_hdp_stack_greater_or_equal("2.2"): 
    File(params.slider_tar_gz,
         owner=params.hdfs_user,
         group=params.user_group,
    )
