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

from resource_management.libraries.script.script import Script
from resource_management.libraries.functions.format import format
from resource_management.core.resources.system import Execute, File
from resource_management.core.source import StaticFile
from ambari_commons import OSConst
from ambari_commons.os_family_impl import OsFamilyFuncImpl, OsFamilyImpl

class ZookeeperServiceCheck(Script):
  pass

@OsFamilyImpl(os_family=OsFamilyImpl.DEFAULT)
class ZookeeperServiceCheckLinux(ZookeeperServiceCheck):
  def service_check(self, env):
    import params
    env.set_params(params)

    File(params.zk_smoke_out,
         action="delete"
    )

    File(format("{tmp_dir}/zkSmoke.sh"),
         mode=0755,
         content=StaticFile('zkSmoke.sh')
    )

    if params.security_enabled:
      smokeUserKeytab=params.smoke_user_keytab
      smokeUserPrincipal=params.smokeuser_principal
    else:
      smokeUserKeytab= "no_keytab"
      smokeUserPrincipal="no_principal"


    cmd_quorum = format("{tmp_dir}/zkSmoke.sh {zk_cli_shell} {smokeuser} {config_dir} {client_port} "
                  "{security_enabled} {kinit_path_local} {smokeUserKeytab} {smokeUserPrincipal} {zk_smoke_out}")

    Execute(cmd_quorum,
            tries=3,
            try_sleep=5,
            path='/usr/sbin:/sbin:/usr/local/bin:/bin:/usr/bin',
            logoutput=True
    )

@OsFamilyImpl(os_family=OSConst.WINSRV_FAMILY)
class ZookeeperServiceCheckWindows(ZookeeperServiceCheck):
  def service_check(self, env):
    import params
    env.set_params(params)

    smoke_cmd = os.path.join(params.stack_root,"Run-SmokeTests.cmd")
    service = "Zookeeper"
    Execute(format("cmd /C {smoke_cmd} {service}"), user=params.zk_user, logoutput=True, tries=3, try_sleep=20)

if __name__ == "__main__":
  ZookeeperServiceCheck().execute()
