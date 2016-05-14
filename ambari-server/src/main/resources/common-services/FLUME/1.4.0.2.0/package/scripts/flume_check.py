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
from resource_management.core.resources.system import Execute
from ambari_commons import OSConst
from ambari_commons.os_family_impl import OsFamilyFuncImpl, OsFamilyImpl

class FlumeServiceCheck(Script):

  @OsFamilyFuncImpl(os_family=OSConst.WINSRV_FAMILY)
  def service_check(self, env):
    import params
    env.set_params(params)
    smoke_cmd = os.path.join(params.stack_root,"Run-SmokeTests.cmd")
    service = "FLUME"
    Execute(format("cmd /C {smoke_cmd} {service}"), logoutput=True, user=params.hdfs_user)

  @OsFamilyFuncImpl(os_family=OsFamilyImpl.DEFAULT)
  def service_check(self, env):
    import params
    env.set_params(params)
    if params.security_enabled:
      principal_replaced = params.http_principal.replace("_HOST", params.hostname)
      Execute(format("{kinit_path_local} -kt {http_keytab} {principal_replaced}"),
              user=params.smoke_user)

    Execute(format('env JAVA_HOME={java_home} {flume_bin} version'),
            logoutput=True,
            tries = 3,
            try_sleep = 20)

if __name__ == "__main__":
  FlumeServiceCheck().execute()
