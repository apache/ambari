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
import urllib2

from resource_management.core.logger import Logger
from resource_management.core.exceptions import Fail
from resource_management.core.resources.system import Execute, File
from resource_management.core.source import StaticFile, Template
from resource_management.libraries.script.script import Script
from resource_management.libraries.functions.format import format
from ambari_commons.os_family_impl import OsFamilyFuncImpl, OsFamilyImpl
from ambari_commons import OSConst
import time

def webhcat_service_check():
  import params
  File(format("{tmp_dir}/templetonSmoke.sh"),
       content= StaticFile('templetonSmoke.sh'),
       mode=0755
  )

  if params.security_enabled:
    smokeuser_keytab=params.smoke_user_keytab
    smoke_user_principal=params.smokeuser_principal
  else:
    smokeuser_keytab= "no_keytab"
    smoke_user_principal="no_principal"
    
  unique_name = format("{smokeuser}.{timestamp}", timestamp = time.time())
  templeton_test_script = format("idtest.{unique_name}.pig")
  templeton_test_input = format("/tmp/idtest.{unique_name}.in")
  templeton_test_output = format("/tmp/idtest.{unique_name}.out")

  File(format("{tmp_dir}/{templeton_test_script}"),
       content = Template("templeton_smoke.pig.j2", templeton_test_input=templeton_test_input, templeton_test_output=templeton_test_output),
       owner=params.hdfs_user
  )
  
  params.HdfsResource(format("/tmp/{templeton_test_script}"),
                      action = "create_on_execute",
                      type = "file",
                      source = format("{tmp_dir}/{templeton_test_script}"),
                      owner = params.smokeuser
  )
  
  params.HdfsResource(templeton_test_input,
                      action = "create_on_execute",
                      type = "file",
                      source = "/etc/passwd",
                      owner = params.smokeuser
  )
  
  params.HdfsResource(None, action = "execute")

  cmd = format("{tmp_dir}/templetonSmoke.sh {webhcat_server_host[0]} {smokeuser} {templeton_port} {templeton_test_script} {has_pig} {smokeuser_keytab}"
               " {security_param} {kinit_path_local} {smoke_user_principal}"
               " {tmp_dir}")

  Execute(cmd,
          tries=3,
          try_sleep=5,
          path='/usr/sbin:/sbin:/usr/local/bin:/bin:/usr/bin',
          logoutput=True)



