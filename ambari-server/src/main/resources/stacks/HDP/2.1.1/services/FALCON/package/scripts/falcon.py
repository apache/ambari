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

  if type == 'client':
    if action == 'config':
      File(params.falcon_conf_dir + '/client.properties',
           content=Template('client.properties.j2'),
           mode=0644)
  elif type == 'server':
    if action == 'config':
      if params.store_uri[0:4] == "hdfs":
        params.HdfsDirectory(params.store_uri,
                             action="create",
                             owner=params.falcon_user,
                             mode=0755
        )
      Directory(params.falcon_local_dir,
                owner=params.falcon_user,
                recursive=True
      )
      Directory(params.falcon_data_dir,
                owner=params.falcon_user,
                recursive=True
      )
      File(params.falcon_conf_dir + '/runtime.properties',
           content=Template('runtime.properties.j2'),
           mode=0644
      )
      File(params.falcon_conf_dir + '/startup.properties',
           content=Template('startup.properties.j2'),
           mode=0644
      )
    if action == 'start':
      Execute(format('env JAVA_HOME={java_home} FALCON_LOG_DIR=/var/log/falcon '
                     'FALCON_PID_DIR=/var/run/falcon FALCON_DATA_DIR={falcon_data_dir} '
                     '{falcon_home}/bin/falcon-start -port {falcon_port}'),
              user=params.falcon_user
      )
    if action == 'stop':
      Execute(format('env JAVA_HOME={java_home} FALCON_LOG_DIR=/var/log/falcon '
                     'FALCON_PID_DIR=/var/run/falcon FALCON_DATA_DIR={falcon_data_dir} '
                     '{falcon_home}/bin/falcon-stop'),
              user=params.falcon_user
      )
