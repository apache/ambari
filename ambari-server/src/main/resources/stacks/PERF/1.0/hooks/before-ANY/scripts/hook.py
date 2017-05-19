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

from resource_management.libraries.script import Hook
from shared_initialization import *

class BeforeAnyHook(Hook):

  def hook(self, env):
    print "Before Any Hook"
    import params
    env.set_params(params)

    #For AMS.
    if params.service_name == 'AMBARI_METRICS':
      setup_users()
      if params.component_name == 'METRICS_COLLECTOR':
        setup_java()

if __name__ == "__main__":
  BeforeAnyHook().execute()
