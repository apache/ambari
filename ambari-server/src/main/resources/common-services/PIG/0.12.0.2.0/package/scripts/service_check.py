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

import os

from resource_management.core.resources.system import Execute, File
from resource_management.core.source import InlineTemplate, StaticFile
from resource_management.libraries.functions.copy_tarball import copy_to_hdfs
from resource_management.libraries.functions.format import format
from resource_management.libraries.functions import StackFeature
from resource_management.libraries.functions.stack_features import check_stack_feature
from resource_management.libraries.resources.execute_hadoop import ExecuteHadoop
from resource_management.libraries.resources.hdfs_resource import HdfsResource
from resource_management.libraries.script.script import Script
from ambari_commons import OSConst
from ambari_commons.os_family_impl import OsFamilyFuncImpl, OsFamilyImpl

class PigServiceCheck(Script):
  pass

@OsFamilyImpl(os_family=OsFamilyImpl.DEFAULT)
class PigServiceCheckLinux(PigServiceCheck):
  def service_check(self, env):
    import params
    env.set_params(params)

    input_file = format('/user/{smokeuser}/passwd')
    output_dir = format('/user/{smokeuser}/pigsmoke.out')

    params.HdfsResource(format("/user/{smokeuser}"),
                        type="directory",
                        action="create_on_execute",
                        owner=params.smokeuser,
                        mode=params.smoke_hdfs_user_mode,
                        )

    params.HdfsResource(output_dir,
                        type="directory",
                        action="delete_on_execute",
                        owner=params.smokeuser,
                        )
    params.HdfsResource(input_file,
                        type="file",
                        source="/etc/passwd",
                        action="create_on_execute",
                        owner=params.smokeuser,
    )
    params.HdfsResource(None, action="execute")
 
    if params.security_enabled:
      kinit_cmd = format("{kinit_path_local} -kt {smoke_user_keytab} {smokeuser_principal};")
      Execute(kinit_cmd,
        user=params.smokeuser
      )

    File( format("{tmp_dir}/pigSmoke.sh"),
      content = StaticFile("pigSmoke.sh"),
      mode = 0o755
    )

    # check for Pig-on-M/R
    Execute( format("pig {tmp_dir}/pigSmoke.sh"),
      tries     = 3,
      try_sleep = 5,
      path      = format('{pig_bin_dir}:/usr/sbin:/sbin:/usr/local/bin:/bin:/usr/bin'),
      user      = params.smokeuser,
      logoutput = True
    )

    test_cmd = format("fs -test -e {output_dir}")
    ExecuteHadoop( test_cmd,
      user      = params.smokeuser,
      conf_dir = params.hadoop_conf_dir,
      bin_dir = params.hadoop_bin_dir
    )

    if params.stack_version_formatted and check_stack_feature(StackFeature.PIG_ON_TEZ, params.stack_version_formatted):
      # cleanup results from previous test
      params.HdfsResource(output_dir,
                          type="directory",
                          action="delete_on_execute",
                          owner=params.smokeuser,
      )
      params.HdfsResource(input_file,
                          type="file",
                          source="/etc/passwd",
                          action="create_on_execute",
                          owner=params.smokeuser,
      )

      # Check for Pig-on-Tez
      resource_created = copy_to_hdfs(
        "tez", params.user_group,
        params.hdfs_user,
        skip=params.sysprep_skip_copy_tarballs_hdfs)
      if resource_created:
        params.HdfsResource(None, action="execute")

      Execute(format("pig -x tez {tmp_dir}/pigSmoke.sh"),
        tries     = 3,
        try_sleep = 5,
        path      = format('{pig_bin_dir}:/usr/sbin:/sbin:/usr/local/bin:/bin:/usr/bin'),
        user      = params.smokeuser,
        logoutput = True
      )

      ExecuteHadoop(test_cmd,
        user      = params.smokeuser,
        conf_dir = params.hadoop_conf_dir,
        bin_dir = params.hadoop_bin_dir
      )

@OsFamilyImpl(os_family=OSConst.WINSRV_FAMILY)
class PigServiceCheckWindows(PigServiceCheck):
  def service_check(self, env):
    import params
    env.set_params(params)
    smoke_cmd = os.path.join(params.stack_root,"Run-SmokeTests.cmd")
    service = "PIG"
    Execute(format("cmd /C {smoke_cmd} {service}", smoke_cmd=smoke_cmd, service=service), logoutput=True, user=params.pig_user, timeout=300)

if __name__ == "__main__":
  PigServiceCheck().execute()

