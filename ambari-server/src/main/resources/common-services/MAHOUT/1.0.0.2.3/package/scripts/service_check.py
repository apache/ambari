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

from resource_management import *
from resource_management.libraries.functions.dynamic_variable_interpretation import copy_tarballs_to_hdfs

class MahoutServiceCheck(Script):
  def service_check(self, env):
    import params
    env.set_params(params)

    create_input_dir_cmd = format("fs -mkdir /user/{smokeuser}/mahoutsmokeinput")
    copy_file_to_hdfs_cmd = format("fs -put {tmp_dir}/sample-mahout-test.txt /user/{smokeuser}/mahoutsmokeinput/")
    mahout_command = format("mahout seqdirectory --input /user/{smokeuser}/mahoutsmokeinput/sample-mahout-test.txt "
                            "--output /user/{smokeuser}/mahoutsmokeoutput/ --charset utf-8")
    test_command = format("fs -test -e /user/{smokeuser}/mahoutsmokeoutput/_SUCCESS")
    remove_output_input_dirs_cmd = format("fs -rm -r -f /user/{smokeuser}/mahoutsmokeoutput "
                                          "/user/{smokeuser}/mahoutsmokeinput")

    ExecuteHadoop( remove_output_input_dirs_cmd,
                   tries = 3,
                   try_sleep = 5,
                   user = params.smokeuser,
                   conf_dir = params.hadoop_conf_dir,
                   # for kinit run
                   keytab = params.smoke_user_keytab,
                   principal = params.smokeuser_principal,
                   security_enabled = params.security_enabled,
                   kinit_path_local = params.kinit_path_local,
                   bin_dir = params.hadoop_bin_dir
                   )

    ExecuteHadoop( create_input_dir_cmd,
                 tries = 3,
                 try_sleep = 5,
                 user = params.smokeuser,
                 conf_dir = params.hadoop_conf_dir,
                 bin_dir = params.hadoop_bin_dir
    )

    File( format("{tmp_dir}/sample-mahout-test.txt"),
        content = "Test text which will be converted to sequence file.",
        mode = 0755
    )

    ExecuteHadoop( copy_file_to_hdfs_cmd,
                   tries = 3,
                   try_sleep = 5,
                   user = params.smokeuser,
                   conf_dir = params.hadoop_conf_dir,
                   bin_dir = params.hadoop_bin_dir
    )

    Execute( mahout_command,
             tries = 3,
             try_sleep = 5,
             environment={'HADOOP_HOME': params.hadoop_home,'HADOOP_CONF_DIR': params.hadoop_conf_dir,
                          'MAHOUT_HOME': params.mahout_home,'JAVA_HOME': params.java64_home},
             path = format('/usr/sbin:/sbin:/usr/local/bin:/bin:/usr/bin'),
             user = params.smokeuser
    )

    ExecuteHadoop( test_command,
                   tries = 10,
                   try_sleep = 6,
                   user = params.smokeuser,
                   conf_dir = params.hadoop_conf_dir,
                   bin_dir = params.hadoop_bin_dir
    )


if __name__ == "__main__":
  MahoutServiceCheck().execute()


