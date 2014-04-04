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

def falcon(type, action = None):
  import params
  if action == 'config':
    Directory(params.falcon_pid_dir,
              owner=params.falcon_user
    )
    Directory(params.falcon_log_dir,
              owner=params.falcon_user
    )
    Directory(params.falcon_webapp_dir,
              owner=params.falcon_user
    )
    Directory(params.falcon_home,
              owner=params.falcon_user
    )
    File(params.falcon_conf_dir + '/falcon-env.sh',
         content=Template('falcon-env.sh.j2')
    )
    File(params.falcon_conf_dir + '/client.properties',
         content=Template('client.properties.j2'),
         mode=0644
    )
    PropertiesFile(params.falcon_conf_dir + '/runtime.properties',
                   properties=params.falcon_runtime_properties,
                   mode=0644
    )
    PropertiesFile(params.falcon_conf_dir + '/startup.properties',
                   properties=params.falcon_startup_properties,
                   mode=0644
    )
  if type == 'server':
    if action == 'config':
      if params.store_uri[0:4] == "hdfs":
        params.HdfsDirectory(params.store_uri,
                             action="create_delayed",
                             owner=params.falcon_user,
                             mode=0755
        )
      params.HdfsDirectory(params.flacon_apps_dir,
                           action="create_delayed",
                           owner=params.falcon_user,
                           mode=0777#TODO change to proper mode
      )
      params.HdfsDirectory(None, action="create")
      Directory(params.falcon_local_dir,
                owner=params.falcon_user,
                recursive=True
      )
      if params.falcon_embeddedmq_enabled == True:
        Directory(params.falcon_embeddedmq_data,
                  owner=params.falcon_user,
                  recursive=True
        )

    if action == 'start':
      Execute(format('{falcon_home}/bin/falcon-start -port {falcon_port}'),
              user=params.falcon_user
      )
    if action == 'stop':
      Execute(format('{falcon_home}/bin/falcon-stop'),
              user=params.falcon_user
      )
      File(params.server_pid_file,
           action='delete'
      )
