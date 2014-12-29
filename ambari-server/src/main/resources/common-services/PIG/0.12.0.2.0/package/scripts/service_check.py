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

class PigServiceCheck(Script):
  def service_check(self, env):
    import params
    env.set_params(params)

    input_file = format('/user/{smokeuser}/passwd')
    output_dir = format('/user/{smokeuser}/pigsmoke.out')

    #cleanup put below to handle retries; if retrying there wil be a stale file that needs cleanup; exit code is fn of second command
    params.HdfsResource(output_dir,
                        type="directory",
                        action="delete_delayed",
                        user=params.smokeuser,
                        )
    params.HdfsResource(input_file,
                        type="file",
                        action="delete_delayed",
                        user=params.smokeuser,
                        )
    params.HdfsResource(input_file,
                        type="file",
                        source="/etc/passwd",
                        action="create_delayed",
                        user=params.smokeuser,
    )
    params.HdfsResource(None, action="execute")

    File( format("{tmp_dir}/pigSmoke.sh"),
      content = StaticFile("pigSmoke.sh"),
      mode = 0755
    )

    # check for Pig-on-M/R
    Execute( format("pig {tmp_dir}/pigSmoke.sh"),
      tries     = 3,
      try_sleep = 5,
      path      = format('{pig_bin_dir}:/usr/sbin:/sbin:/usr/local/bin:/bin:/usr/bin'),
      user      = params.smokeuser
    )

    test_cmd = format("fs -test -e {output_dir}")
    ExecuteHadoop( test_cmd,
      user = params.smokeuser,
      conf_dir = params.hadoop_conf_dir,
      bin_dir = params.hadoop_bin_dir
    )

    if params.hdp_stack_version != "" and compare_versions(params.hdp_stack_version, '2.2') >= 0:
      # cleanup results from previous test
      params.HdfsResource(output_dir,
                          type="directory",
                          action="delete_delayed",
                          user=params.smokeuser,
                          )
      params.HdfsResource(input_file,
                          type="file",
                          action="delete_delayed",
                          user=params.smokeuser,
                          )
      params.HdfsResource(input_file,
                          type="file",
                          source="/etc/passwd",
                          action="create_delayed",
                          user=params.smokeuser,
      )
      params.HdfsResource(InlineTemplate(params.tez_tar_destination).get_content(),
                          type="file",
                          action="create_delayed",
                          source=params.tez_tar_source,
                          group=params.user_group,
                          owner=params.hdfs_user
      )
      params.HdfsResource(None, action="execute")

      if params.security_enabled:
        kinit_cmd = format("{kinit_path_local} -kt {smoke_user_keytab} {smokeuser};")
        Execute(kinit_cmd,
                user=params.smokeuser
        )

      Execute( format("pig -x tez {tmp_dir}/pigSmoke.sh"),
        tries     = 3,
        try_sleep = 5,
        path      = format('{pig_bin_dir}:/usr/sbin:/sbin:/usr/local/bin:/bin:/usr/bin'),
        user      = params.smokeuser
      )

      ExecuteHadoop( test_cmd,
        user      = params.smokeuser,
        conf_dir = params.hadoop_conf_dir,
        bin_dir = params.hadoop_bin_dir
      )

if __name__ == "__main__":
  PigServiceCheck().execute()

