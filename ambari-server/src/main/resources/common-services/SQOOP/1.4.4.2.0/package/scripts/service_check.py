#!/usr/bin/env python
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
from resource_management.core.resources import Execute
from resource_management.libraries.functions.default import default
from resource_management.libraries.functions import format
from ambari_commons.os_family_impl import OsFamilyImpl
from ambari_commons import OSConst
import os

class SqoopServiceCheck(Script):
  pass

@OsFamilyImpl(os_family=OsFamilyImpl.DEFAULT)
class SqoopServiceCheckDefault(SqoopServiceCheck):

  def get_component_name(self):
    return "sqoop-server"

  def service_check(self, env):
    import params
    env.set_params(params)
    if params.security_enabled:
      Execute(format("{kinit_path_local}  -kt {smoke_user_keytab} {smokeuser_principal}"),
              user = params.smokeuser,
      )
    Execute("sqoop version",
            user = params.smokeuser,
            path = params.sqoop_bin_dir,
            logoutput = True
    )

@OsFamilyImpl(os_family=OSConst.WINSRV_FAMILY)
class SqoopServiceCheckWindows(SqoopServiceCheck):
  def service_check(self, env):
    import params
    env.set_params(params)
    smoke_cmd = os.path.join(params.stack_root,"Run-SmokeTests.cmd")
    service = "SQOOP"
    Execute(format("cmd /C {smoke_cmd} {service}"), logoutput=True)

if __name__ == "__main__":
  SqoopServiceCheck().execute()
