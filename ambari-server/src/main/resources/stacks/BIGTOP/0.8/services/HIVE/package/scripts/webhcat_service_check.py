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

def webhcat_service_check():
  import params
  File(format("{tmp_dir}/templetonSmoke.sh"),
       content= StaticFile('templetonSmoke.sh'),
       mode=0755
  )

  cmd = format("{tmp_dir}/templetonSmoke.sh {webhcat_server_host[0]} {smokeuser} {smokeuser_keytab}"
               " {security_param} {kinit_path_local}",
               smokeuser_keytab=params.smoke_user_keytab if params.security_enabled else "no_keytab")

  Execute(cmd,
          tries=3,
          try_sleep=5,
          path='/usr/sbin:/sbin:/usr/local/bin:/bin:/usr/bin',
          logoutput=True)



