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
import os

class AccumuloServiceCheck(Script):
  def service_check(self, env):
    import params
    env.set_params(params)

    smokeuser_kinit_cmd = format("{kinit_path_local} -kt {smoke_user_keytab} {smokeuser_principal};") if params.security_enabled else ""

    cmdfile = format("{exec_tmp_dir}/cmds")
    try:
      File(cmdfile,
           mode=0600,
           group=params.user_group,
           owner=params.smoke_test_user,
           content=InlineTemplate('createtable testtable\n'
                                  'insert row cf cq val\n'
                                  'scan\n'
                                  'flush -w\n'
                                  'scan\n'
                                  'deletetable -f testtable\n\n')
      )
      if params.security_enabled and params.has_secure_user_auth:
        cmd = format("{smokeuser_kinit_cmd} "
                        "{client_script} shell -f {cmdfile}")
      else:
        cmd = format("{client_script} shell -u {smoke_test_user} "
                        "-p {smoke_test_password} -f {cmdfile}")
      Execute(cmd,
              timeout=120,
              user=params.smoke_test_user,
              logoutput=True)
    finally:
      try_remove(cmdfile)

def try_remove(file):
  try:
    os.remove(file)
  except:
    pass

if __name__ == "__main__":
  AccumuloServiceCheck().execute()
