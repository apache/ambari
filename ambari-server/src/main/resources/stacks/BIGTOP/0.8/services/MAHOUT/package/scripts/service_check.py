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
from resource_management import *

class MahoutServiceCheck(Script):
  def service_check(self, env):
    import params
    env.set_params(params)

    # prepare the input file content
    input_file = os.path.join("/tmp", "mahout_input.csv")
    input_file_content = """\
1,101,5.0
1,102,5.0
1,103,2.5
2,101,2.0
2,102,2.5
2,103,5.0
2,104,2.0
3,101,2.5
3,104,4.0
3,105,4.5
3,107,5.0"""

    File(input_file, content=input_file_content, mode=644)

    # create the log dir for the smoke user
    Directory(os.path.join(params.hadoop_log_dir, params.smokeuser),
	            owner = params.smokeuser,
              group = params.user_group,
              mode = 755)

    # transfer the input file to hdfs
    recommenderdata_dir = "recommenderdata"
    recommenderoutput_dir = "recommenderoutput"
    cleanup_cmd = format("fs -rm -r {recommenderdata_dir} {recommenderoutput_dir} temp")
    #cleanup put below to handle retries; if retrying there wil be a stale file that needs cleanup; exit code is fn of second command
    create_file_cmd = format("{cleanup_cmd}; hadoop fs -put {input_file} {recommenderdata_dir}") #TODO: inconsistent that second command needs hadoop

    test_cmd_cat = "mahout cat /etc/passwd"
    test_cmd_recommendation = format("mahout recommenditembased --input {recommenderdata_dir} --output {recommenderoutput_dir} -s SIMILARITY_COOCCURRENCE")

    ExecuteHadoop(create_file_cmd,
      tries     = 3,
      try_sleep = 5,
      user      = params.smokeuser,
      conf_dir = params.hadoop_conf_dir,
      # for kinit run
      keytab = params.smoke_user_keytab,
      security_enabled = params.security_enabled,
      kinit_path_local = params.kinit_path_local,
      logoutput = True
    )

    Execute(test_cmd_cat,
      tries     = 3,
      try_sleep = 5,
      path      = '/usr/sbin:/sbin:/usr/local/bin:/bin:/usr/bin',
      user      = params.smokeuser,
      logoutput = True
    )

    Execute(test_cmd_recommendation,
      tries     = 3,
      try_sleep = 5,
      path      = '/usr/sbin:/sbin:/usr/local/bin:/bin:/usr/bin',
      user      = params.smokeuser,
      logoutput = True,
    )

if __name__ == "__main__":
  MahoutServiceCheck().execute()
