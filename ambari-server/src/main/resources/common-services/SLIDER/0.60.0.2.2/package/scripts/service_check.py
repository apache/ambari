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

from resource_management import *
from resource_management.libraries.functions.copy_tarball import copy_to_hdfs
from ambari_commons import OSConst
from ambari_commons.os_family_impl import OsFamilyFuncImpl, OsFamilyImpl

class SliderServiceCheck(Script):

  @OsFamilyFuncImpl(os_family=OSConst.WINSRV_FAMILY)
  def service_check(self, env):
    import params
    env.set_params(params)
    smoke_cmd = os.path.join(params.hdp_root, "Run-SmokeTests.cmd")
    service = "SLIDER"
    Execute(format("cmd /C {smoke_cmd} {service}"), logoutput=True, user=params.hdfs_user)

  @OsFamilyFuncImpl(os_family=OsFamilyImpl.DEFAULT)
  def service_check(self, env):
    import params
    env.set_params(params)
    
    if Script.is_hdp_stack_greater_or_equal("2.2"):
      copy_to_hdfs("slider", params.user_group, params.hdfs_user, host_sys_prepped=params.host_sys_prepped)
    
    smokeuser_kinit_cmd = format(
      "{kinit_path_local} -kt {smokeuser_keytab} {smokeuser_principal};") if params.security_enabled else ""

    servicecheckcmd = format("{smokeuser_kinit_cmd} {slider_cmd} list")

    Execute(servicecheckcmd,
            tries=3,
            try_sleep=5,
            user=params.smokeuser,
            logoutput=True
    )


if __name__ == "__main__":
  SliderServiceCheck().execute()
  
