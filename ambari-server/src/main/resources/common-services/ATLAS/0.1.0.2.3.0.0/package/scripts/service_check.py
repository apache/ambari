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
import httplib
import socket
import time
from resource_management import Script, Logger, ComponentIsNotRunning, Fail
from resource_management.libraries.functions import format


class AtlasServiceCheck(Script):
  ATLAS_CONNECT_TRIES = 5
  ATLAS_CONNECT_TIMEOUT = 10

  def service_check(self, env):
    import params

    env.set_params(params)

    for i in xrange(0, self.ATLAS_CONNECT_TRIES):
      try:
        conn = httplib.HTTPConnection(params.metadata_host,
                                      int(params.metadata_port))
        conn.request("GET", format("http://{params.metadata_host}:{params.metadata_port}/"))
      except (httplib.HTTPException, socket.error) as ex:
        if i < self.ATLAS_CONNECT_TRIES - 1:
          time.sleep(self.ATLAS_CONNECT_TIMEOUT)
          Logger.info("Connection failed. Next retry in %s seconds."
                      % (self.ATLAS_CONNECT_TIMEOUT))
          continue
        else:
          raise Fail("Service check has failed.")

    resp = conn.getresponse()
    if resp.status == 200 :
      Logger.info('Atlas server up and running')
    else:
      Logger.debug('Atlas server not running')
      raise ComponentIsNotRunning()

if __name__ == "__main__":
  AtlasServiceCheck().execute()
