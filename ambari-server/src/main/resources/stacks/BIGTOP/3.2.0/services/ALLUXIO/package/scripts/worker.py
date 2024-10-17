#!/usr/bin/python
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

import os
from resource_management import *
import time

class AlluxioWorker(Script):
    def install(self, env):
        self.install_packages(env)

    def configure(self, env, upgrade_type=None, config_dir=None):
        import params
        env.set_params(params)
        

        Directory([params.alluxio_pid_dir, params.alluxio_journal_dir],
                  owner=params.alluxio_user,
                  group=params.alluxio_group,
                  mode=0o775,
                  create_parents = True
        )

        Directory([params.alluxio_log_dir, os.path.join(params.alluxio_log_dir, 'user')],
                  owner=params.alluxio_user,
                  group=params.alluxio_group,
                  mode=0o777,
                  create_parents = True
        )

        # create alluxio-site.properties in alluxio install dir
        File(os.path.join(params.alluxio_conf_dir, 'alluxio-site.properties'),
             owner=params.alluxio_user,
             group=params.alluxio_group,
             content=InlineTemplate(params.alluxio_site_properties),
             mode=0o644,
        )

        # create alluxio-env.sh in alluxio install dir
        File(os.path.join(params.alluxio_conf_dir, 'alluxio-env.sh'),
             owner=params.alluxio_user,
             group=params.alluxio_group,
             content=InlineTemplate(params.alluxio_env_sh),
             mode=0o775,
        )

        #create log4j2.properties alluxio install dir
        File(os.path.join(params.alluxio_conf_dir, 'log4j.properties'),
             owner=params.alluxio_user,
             group=params.alluxio_group,
             content=InlineTemplate(params.alluxio_log4j2_properties),
             mode=0o644,
        ) 

        # masters
        File(format("{alluxio_conf_dir}/masters"),
             owner=params.alluxio_user,
             group=params.alluxio_group,
             mode=0o644,
             content=Template('masters.j2', conf_dir=params.alluxio_conf_dir)
          )

        # workers
        File(format("{alluxio_conf_dir}/workers"),
             owner=params.alluxio_user,
             group=params.alluxio_group,
             mode=0o644,
             content=Template('workers.j2', conf_dir=params.alluxio_conf_dir)
          )
                  
    def start(self, env, upgrade_type=None):
        import params
        env.set_params(params)

        self.configure(env)
        
        Execute(("bash","-c",params.alluxio_worker_mount_cmd), sudo=True)

        Execute(params.alluxio_worker_start_cmd,user=params.alluxio_user,environment={'JAVA_HOME': params.java_home})

        Execute(params.alluxio_worker_pid_cmd,user=params.alluxio_user,environment={'JAVA_HOME': params.java_home})

    def stop(self, env, upgrade_type=None):
        import params
        env.set_params(params)
        self.configure(env)

        Execute(params.alluxio_worker_stop_cmd,user=params.alluxio_user,environment={'JAVA_HOME': params.java_home})

    def status(self, env):
        import params
        env.set_params(params)
        check_process_status(params.alluxio_worker_pid_file)

    def get_user(self):
        import params
        return params.alluxio_user

    def get_pid_files(self):
        import params
        return [params.alluxio_worker_pid_file]

if __name__ == "__main__":
    AlluxioWorker().execute()