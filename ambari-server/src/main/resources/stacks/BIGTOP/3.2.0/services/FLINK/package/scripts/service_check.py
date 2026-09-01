#!/usr/bin/env python3
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

from contextlib import nullcontext

from resource_management.core import shell
from resource_management.libraries.functions.private_kerberos_cache import (
  PrivateKerberosCache,
)
from resource_management.libraries.script import Script

import flink_utils


class FlinkServiceCheck(Script):
  def service_check(self, env):
    import params

    env.set_params(params)
    flink_utils.validate_executable(params.flink_cli, "Flink CLI")
    flink_utils.validate_regular_file(params.wordcount_jar, "Flink WordCount example")
    hadoop_classpath = flink_utils.resolve_hadoop_classpath(
      params.hadoop_executable,
      params.smokeuser,
      params.java_home,
    )

    cache_context = nullcontext(None)
    if params.security_enabled:
      flink_utils.validate_executable(params.kinit_path_local, "kinit executable")
      flink_utils.validate_keytab(
        params.smoke_user_keytab, "Flink service-check keytab"
      )
      cache_context = PrivateKerberosCache(
        params.smokeuser,
        params.user_group,
        temp_dir=params.tmp_dir,
        prefix="ambari-flink-service-check-",
      )

    with cache_context as kerberos_cache:
      command_environment = {
        "FLINK_CONF_DIR": params.flink_config_dir,
        "FLINK_LOG_DIR": params.flink_cli_log_dir,
        "HADOOP_CLASSPATH": hadoop_classpath,
        "HADOOP_CONF_DIR": params.hadoop_conf_dir,
        "JAVA_HOME": params.java_home,
      }
      if kerberos_cache is not None:
        command_environment = kerberos_cache.merge_environment(command_environment)
        kerberos_cache.kinit(
          params.kinit_path_local,
          params.smoke_user_keytab,
          params.smokeuser_principal,
          timeout=60,
        )

      shell.checked_call(
        (
          params.flink_cli,
          "run-application",
          "--target",
          "yarn-application",
          "-Dclassloader.check-leaked-classloader=false",
          params.wordcount_jar,
        ),
        user=params.smokeuser,
        env=command_environment,
        timeout=240,
        logoutput=True,
      )


if __name__ == "__main__":
  FlinkServiceCheck().execute()
