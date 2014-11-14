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
from resource_management.libraries import functions
import sys
import os


class MapReduce2ServiceCheck(Script):
  def service_check(self, env):
    import params

    env.set_params(params)

    component_type = 'hs'
    if params.hadoop_ssl_enabled:
      component_address = params.hs_webui_address
    else:
      component_address = params.hs_webui_address

    validateStatusFileName = "validateYarnComponentStatus.py"
    validateStatusFilePath = os.path.join(os.path.dirname(params.hadoop_home), "temp", validateStatusFileName)
    python_executable = sys.executable
    validateStatusCmd = "{} {} {} -p {} -s {}".format(
      python_executable, validateStatusFilePath, component_type, component_address, params.hadoop_ssl_enabled)

    if params.security_enabled:
      kinit_cmd = "{} -kt {} {};".format(params.kinit_path_local, params.smoke_user_keytab, params.smokeuser)
      smoke_cmd = kinit_cmd + validateStatusCmd
    else:
      smoke_cmd = validateStatusCmd

    File(validateStatusFilePath,
         content=StaticFile(validateStatusFileName)
    )

    Execute(smoke_cmd,
            tries=3,
            try_sleep=5,
            logoutput=True
    )

    # hadoop_exe = os.path.join(params.hadoop_home, "bin", "hadoop")
    #
    # tested_file = os.path.join(params.hadoop_home, "bin", "hadoop.cmd")
    # jar_path = os.path.join(params.hadoop_mapred2_jar_location, params.hadoopMapredExamplesJarName)
    # input_file = format("/user/hadoop/mapredsmokeinput")
    # output_file = format("/user/hadoop/mapredsmokeoutput")
    # cleanup_cmd = format("cmd /C {hadoop_exe} fs -rm -r -f {output_file} {input_file}")
    # create_file_cmd = format("cmd /C {hadoop_exe} fs -put {tested_file} {input_file}")
    # run_wordcount_job = format("cmd /C {hadoop_exe} jar {jar_path} wordcount {input_file} {output_file}")
    # test_cmd = format("cmd /C {hadoop_exe} fs -test -e {output_file}")
    #
    # if params.security_enabled:
    #   kinit_cmd = "{0} -kt {1} {2};".format(kinit_path_local, smoke_user_keytab, smokeuser)
    #   Execute(kinit_cmd)
    #
    # Execute(cleanup_cmd,
    #         tries=1,
    #         try_sleep=5,
    #         logoutput=True,
    #         user=params.hdfs_user
    # )
    #
    # Execute(create_file_cmd,
    #         tries=1,
    #         try_sleep=5,
    #         logoutput=True,
    #         user=params.hdfs_user
    # )
    #
    # Execute(run_wordcount_job,
    #         tries=1,
    #         try_sleep=5,
    #         logoutput=True,
    #         user=params.hdfs_user
    # )
    #
    # Execute(test_cmd,
    #         logoutput=True,
    #         user=params.hdfs_user
    # )


if __name__ == "__main__":
  MapReduce2ServiceCheck().execute()
