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

from resource_management.libraries.script.script import Script
from resource_management.libraries.functions.format import format
from resource_management.core.logger import Logger  
from resource_management.core.resources.system import Execute
from resource_management.core.exceptions import ComponentIsNotRunning
from resource_management.core.exceptions import Fail

class AtlasServiceCheck(Script):

  def service_check(self, env):
    import params

    env.set_params(params)

    if params.security_enabled:
      Execute(format("{kinit_path_local} -kt {smokeuser_keytab} {smokeuser_principal}"),
              user=params.smoke_test_user)
    atlas_host_call_count = 0

    for atlas_host in params.atlas_hosts:
      if params.security_enabled:
        smoke_cmd = format('curl -k --negotiate -u : -b ~/cookiejar.txt -c ~/cookiejar.txt -s -o /dev/null -w "%{{http_code}}" {metadata_protocol}://{atlas_host}:{metadata_port}/')
      else:
        smoke_cmd = format('curl -k -s -o /dev/null -w "%{{http_code}}" {metadata_protocol}://{atlas_host}:{metadata_port}/')
      try:
        Execute(smoke_cmd , user=params.smoke_test_user, tries = 5,
              try_sleep = 10)
      except Exception as err:
        atlas_host_call_count =  atlas_host_call_count + 1
        Logger.error("ATLAS service check failed for host {0} with error {1}".format(atlas_host,err))
    if atlas_host_call_count == len(params.atlas_hosts):
      raise Fail("All instances of ATLAS METADATA SERVER are down.")


if __name__ == "__main__":
  AtlasServiceCheck().execute()
