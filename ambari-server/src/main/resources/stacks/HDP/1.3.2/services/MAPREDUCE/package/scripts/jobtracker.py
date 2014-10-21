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
from resource_management import *

from mapreduce import mapreduce
from service import service

class Jobtracker(Script):
  def install(self, env):
    self.install_packages(env)

  def configure(self, env):
    import params
    env.set_params(params)
    mapreduce(name="jobtracker")

  def start(self, env):
    import params
    env.set_params(params)
    self.configure(env) # FOR SECURITY
    service('jobtracker',
            action='start'
    )

  def stop(self, env):
    import params
    env.set_params(params)

    service('jobtracker',
            action='stop'
    )

  def status(self, env):
    import status_params
    env.set_params(status_params)
    check_process_status(status_params.jobtracker_pid_file)
    pass

  def decommission(self, env):
    import params

    env.set_params(params)

    mapred_user = params.mapred_user
    conf_dir = params.conf_dir
    user_group = params.user_group

    if params.security_enabled:
      Execute(params.kinit_cmd,
         user = params.mapred_user)

    File(params.exclude_file_path,
         content=Template("exclude_hosts_list.j2"),
         owner=mapred_user,
         group=user_group
    )

    if params.update_exclude_file_only == False:
      ExecuteHadoop('mradmin -refreshNodes',
                  user=mapred_user,
                  conf_dir=conf_dir,
                  kinit_override=True)
      pass
    pass

if __name__ == "__main__":
  Jobtracker().execute()
