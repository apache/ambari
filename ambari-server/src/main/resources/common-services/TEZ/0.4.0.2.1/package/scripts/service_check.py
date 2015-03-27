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
from resource_management.libraries.functions.version import compare_versions
from resource_management.libraries.functions.dynamic_variable_interpretation import copy_tarballs_to_hdfs
from ambari_commons import OSConst
from ambari_commons.os_family_impl import OsFamilyImpl

class TezServiceCheck(Script):
  pass

@OsFamilyImpl(os_family=OsFamilyImpl.DEFAULT)
class TezServiceCheckLinux(TezServiceCheck):
  def service_check(self, env):
    import params
    env.set_params(params)

    if params.hdp_stack_version != "" and compare_versions(params.hdp_stack_version, '2.2') >= 0:
      hdp_version = functions.get_hdp_version("hadoop-client")

    path_to_tez_jar = format(params.path_to_tez_examples_jar)
    copy_test_file_to_hdfs_cmd =  format("fs -put {tmp_dir}/sample-tez-test /tmp/tezsmokeinput/")
    create_input_dir_cmd = format("fs -mkdir /tmp/tezsmokeinput")
    wordcount_command = format("jar {path_to_tez_jar} orderedwordcount "
                               "/tmp/tezsmokeinput/sample-tez-test /tmp/tezsmokeoutput/")
    test_command = format("fs -test -e /tmp/tezsmokeoutput/_SUCCESS")
    remove_output_input_dirs_cmd = "fs -rm -r -f /tmp/tezsmokeinput /tmp/tezsmokeoutput"


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

    File( format("{tmp_dir}/sample-tez-test"),
          content = "foo\nbar\nfoo\nbar\nfoo",
          mode = 0755
    )

    ExecuteHadoop( copy_test_file_to_hdfs_cmd,
                   tries = 3,
                   try_sleep = 5,
                   user = params.smokeuser,
                   conf_dir = params.hadoop_conf_dir,
                   bin_dir = params.hadoop_bin_dir
    )

    ExecuteHadoop( wordcount_command,
                   tries = 3,
                   try_sleep = 5,
                   user = params.smokeuser,
                   conf_dir = params.hadoop_conf_dir,
                   bin_dir = params.hadoop_bin_dir
    )

    ExecuteHadoop( test_command,
                   tries = 10,
                   try_sleep = 6,
                   user = params.smokeuser,
                   conf_dir = params.hadoop_conf_dir,
                   bin_dir = params.hadoop_bin_dir
    )


@OsFamilyImpl(os_family=OSConst.WINSRV_FAMILY)
class TezServiceCheckWindows(TezServiceCheck):
  def service_check(self, env):
    import params
    env.set_params(params)
    smoke_cmd = os.path.join(params.hdp_root,"Run-SmokeTests.cmd")
    service = "TEZ"
    Execute(format("cmd /C {smoke_cmd} {service}"), logoutput=True, user=params.tez_user)


if __name__ == "__main__":
  TezServiceCheck().execute()

