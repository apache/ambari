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
from resource_management.libraries.functions.format import format
import functions
from ambari_commons import OSCheck, OSConst
from ambari_commons.os_family_impl import OsFamilyImpl


class HbaseServiceCheck(Script):
  pass


@OsFamilyImpl(os_family=OSConst.WINSRV_FAMILY)
class HbaseServiceCheckWindows(HbaseServiceCheck):
  def service_check(self, env):
    import params
    env.set_params(params)
    smoke_cmd = os.path.join(params.hdp_root, "Run-SmokeTests.cmd")
    service = "HBASE"
    Execute(format("cmd /C {smoke_cmd} {service}"), user=params.hbase_user, logoutput=True)


@OsFamilyImpl(os_family=OsFamilyImpl.DEFAULT)
class HbaseServiceCheckDefault(HbaseServiceCheck):
  def service_check(self, env):
    import params
    env.set_params(params)
    
    output_file = "/apps/hbase/data/ambarismoketest"
    smokeuser_kinit_cmd = format("{kinit_path_local} -kt {smoke_user_keytab} {smokeuser_principal};") if params.security_enabled else ""
    hbase_servicecheck_file = format("{exec_tmp_dir}/hbase-smoke.sh")
  
    File( format("{exec_tmp_dir}/hbaseSmokeVerify.sh"),
      content = StaticFile("hbaseSmokeVerify.sh"),
      mode = 0755
    )
  
    File( hbase_servicecheck_file,
      mode = 0755,
      content = Template('hbase-smoke.sh.j2')
    )
    
    if params.security_enabled:    
      hbase_grant_premissions_file = format("{exec_tmp_dir}/hbase_grant_permissions.sh")
      grantprivelegecmd = format("{kinit_cmd} {hbase_cmd} shell {hbase_grant_premissions_file}")
  
      File( hbase_grant_premissions_file,
        owner   = params.hbase_user,
        group   = params.user_group,
        mode    = 0644,
        content = Template('hbase_grant_permissions.j2')
      )
      
      Execute( grantprivelegecmd,
        user = params.hbase_user,
      )

    servicecheckcmd = format("{smokeuser_kinit_cmd} {hbase_cmd} --config {hbase_conf_dir} shell {hbase_servicecheck_file}")
    smokeverifycmd = format("{exec_tmp_dir}/hbaseSmokeVerify.sh {hbase_conf_dir} {service_check_data} {hbase_cmd}")
  
    Execute(format("{servicecheckcmd} && {smokeverifycmd}"),
      tries     = 6,
      try_sleep = 5,
      user = params.smoke_test_user,
      logoutput = True
    )

if __name__ == "__main__":
  HbaseServiceCheck().execute()
  
