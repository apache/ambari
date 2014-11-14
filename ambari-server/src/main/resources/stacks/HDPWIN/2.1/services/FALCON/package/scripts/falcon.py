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

def falcon():
  import params

  env = Environment.get_instance()
  # These 2 parameters are used in ../templates/client.properties.j2
  env.config.params["falcon_host"] = params.falcon_host
  env.config.params["falcon_port"] = params.falcon_port

  File(os.path.join(params.falcon_conf_dir, 'falcon-env.sh'),
       content=InlineTemplate(params.falcon_env_sh_template)
  )
  File(os.path.join(params.falcon_conf_dir, 'client.properties'),
       content=Template('client.properties.j2')
  )
  PropertiesFile(os.path.join(params.falcon_conf_dir, 'runtime.properties'),
                 properties=params.falcon_runtime_properties
  )
  PropertiesFile(os.path.join(params.falcon_conf_dir, 'startup.properties'),
                 properties=params.falcon_startup_properties
  )
