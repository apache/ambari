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

class MahoutServiceCheck(Script):
  def service_check(self, env):
    import params
    env.set_params(params)

    mahout_command = format("mahout seqdirectory --input /user/{smokeuser}/mahoutsmokeinput/sample-mahout-test.txt "
                            "--output /user/{smokeuser}/mahoutsmokeoutput/ --charset utf-8")
    test_command = format("fs -test -e /user/{smokeuser}/mahoutsmokeoutput/_SUCCESS")
    
    File( format("{tmp_dir}/sample-mahout-test.txt"),
        content = "Test text which will be converted to sequence file.",
        mode = 0755
    )
    
    params.HdfsResource(format("/user/{smokeuser}/mahoutsmokeoutput"),
                       action="delete_on_execute",
                       type="directory",
    )
    params.HdfsResource(format("/user/{smokeuser}/mahoutsmokeinput"),
                        action="create_on_execute",
                        type="directory",
                        owner=params.smokeuser,
    )
    params.HdfsResource(format("/user/{smokeuser}/mahoutsmokeinput/sample-mahout-test.txt"),
                        action="create_on_execute",
                        type="file",
                        owner=params.smokeuser,
                        source=format("{tmp_dir}/sample-mahout-test.txt")
    )
    params.HdfsResource(None, action="execute")

    if params.security_enabled:
      kinit_cmd = format("{kinit_path_local} -kt {smoke_user_keytab} {smokeuser_principal};")
      Execute(kinit_cmd,
              user=params.smokeuser)

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


