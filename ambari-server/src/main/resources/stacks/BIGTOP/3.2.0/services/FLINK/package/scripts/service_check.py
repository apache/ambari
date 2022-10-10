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

import os

from resource_management.libraries.functions.format import format
from resource_management.core.resources import Execute
from resource_management.libraries.script import Script
from resource_management.core.resources.system import Directory

class FlinkServiceCheck(Script):
  def service_check(self, env):
    import params
    env.set_params(params)

    if params.security_enabled:
       flink_kinit_cmd = format("{kinit_path_local} -kt {smoke_user_keytab} {smokeuser_principal}; ")
       Execute(flink_kinit_cmd, user=params.smokeuser)

    job_cmd_opts= "-m yarn-cluster -yD classloader.check-leaked-classloader=false "
    run_flink_wordcount_job = format("export HADOOP_CLASSPATH=`hadoop classpath`;{flink_bin_dir}/flink run {job_cmd_opts} {flink_bin_dir}/../examples/batch/WordCount.jar")

    Execute(run_flink_wordcount_job,
      logoutput=True,
      environment={'JAVA_HOME':params.java_home,'HADOOP_CONF_DIR': params.hadoop_conf_dir},
      user=params.smokeuser)
            
if __name__ == "__main__":
  FlinkServiceCheck().execute()
