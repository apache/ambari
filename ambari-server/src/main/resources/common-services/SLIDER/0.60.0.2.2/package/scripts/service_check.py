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
from resource_management.libraries.functions.copy_tarball import copy_to_hdfs
from resource_management.libraries.functions.constants import StackFeature
from resource_management.libraries.functions.stack_features import check_stack_feature
from resource_management.libraries.functions.format import format
from resource_management.core.resources.system import Execute
from ambari_commons.os_family_impl import OsFamilyFuncImpl, OsFamilyImpl
from ambari_commons import OSConst

class SliderServiceCheck(Script):

  @OsFamilyFuncImpl(os_family=OSConst.WINSRV_FAMILY)
  def service_check(self, env):
    import params
    env.set_params(params)
    smoke_cmd = os.path.join(params.stack_root, "Run-SmokeTests.cmd")
    service = "SLIDER"
    Execute(format("cmd /C {smoke_cmd} {service}"), logoutput=True, user=params.hdfs_user)

  @OsFamilyFuncImpl(os_family=OsFamilyImpl.DEFAULT)
  def service_check(self, env):
    import params
    env.set_params(params)

    if params.stack_version_formatted and check_stack_feature(StackFeature.COPY_TARBALL_TO_HDFS, params.stack_version_formatted):
      copy_to_hdfs("slider", params.user_group, params.hdfs_user, skip=params.sysprep_skip_copy_tarballs_hdfs)
    
    smokeuser_kinit_cmd = format(
      "{kinit_path_local} -kt {smokeuser_keytab} {smokeuser_principal};") if params.security_enabled else ""

    servicecheckcmd = format("{smokeuser_kinit_cmd} {slider_cmd} list")

    Execute(servicecheckcmd,
            tries=3,
            try_sleep=5,
            user=params.smokeuser,
            logoutput=True,
    )


if __name__ == "__main__":
  SliderServiceCheck().execute()
  
