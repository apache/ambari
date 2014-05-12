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

class ServiceCheck(Script):
  def service_check(self, env):
    import params
    env.set_params(params)

    jar_location = params.hadoop_jar_location
    input_file = 'mapredsmokeinput'
    output_file = "mapredsmokeoutput"

    cleanup_cmd = format("dfs -rmr {output_file} {input_file}")
    create_file_cmd = format("{cleanup_cmd} ; hadoop dfs -put /etc/passwd {input_file}")
    test_cmd = format("fs -test -e {output_file}")
    run_wordcount_job = format("jar {jar_location}/hadoop-examples.jar wordcount {input_file} {output_file}")

    if params.security_enabled:
      kinit_cmd = format("{kinit_path_local} -kt {smoke_user_keytab} {smokeuser};")

      Execute(kinit_cmd,
              user=params.smokeuser
      )

    ExecuteHadoop(create_file_cmd,
                  tries=1,
                  try_sleep=5,
                  user=params.smokeuser,
                  conf_dir=params.conf_dir
    )

    ExecuteHadoop(run_wordcount_job,
                  tries=1,
                  try_sleep=5,
                  user=params.smokeuser,
                  conf_dir=params.conf_dir,
                  logoutput=True
    )

    ExecuteHadoop(test_cmd,
                  user=params.smokeuser,
                  conf_dir=params.conf_dir
    )

if __name__ == "__main__":
  ServiceCheck().execute()
