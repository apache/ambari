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

from resource_management.core.exceptions import Fail
from resource_management.core.logger import Logger
from resource_management.core.resources.system import Execute
from resource_management.libraries.functions.private_kerberos_cache import (
  PrivateKerberosCache,
)
from resource_management.libraries.script.script import Script

import spark_utils


CHECK_TIMEOUT_SECONDS = 60


def build_beeline_url(params, host):
  validated_host = spark_utils.validate_host(
    host, "Spark Thrift Server host"
  )
  url = (
    f"jdbc:hive2://{validated_host}:"
    f"{params.spark_thrift_port}/default"
  )
  options = []
  if params.security_enabled:
    principal = params.default_hive_kerberos_principal.replace("_HOST", host.lower())
    spark_utils.validate_principal(principal, "Spark Thrift Server principal")
    options.append(f"principal={principal}")
  options.append(f"transportMode={params.spark_transport_mode}")
  if params.spark_transport_mode == "http":
    options.append(f"httpPath={params.spark_thrift_endpoint}")
    if params.spark_thrift_ssl_enabled:
      options.append("ssl=true")
  return ";".join((url,) + tuple(options))


class SparkServiceCheck(Script):
  def service_check(self, env):
    import params

    env.set_params(params)
    if not params.has_history_server:
      raise Fail("Spark service check requires a History Server host")
    spark_utils.validate_executable("/usr/bin/curl", "curl executable")

    cache_context = nullcontext(None)
    if params.security_enabled:
      spark_utils.validate_executable(params.kinit_path_local, "kinit executable")
      spark_utils.validate_keytab(params.smoke_user_keytab, "Spark service-check keytab")
      cache_context = PrivateKerberosCache(
        params.smoke_user,
        params.user_group,
        temp_dir=params.tmp_dir,
        prefix="ambari-spark-service-check-",
      )

    with cache_context as cache:
      environment = {"JAVA_HOME": params.java_home, "SPARK_CONF_DIR": params.spark_conf_dir}
      if cache is not None:
        cache.kinit(
          params.kinit_path_local,
          params.smoke_user_keytab,
          params.smokeuser_principal,
          timeout=30,
        )
        environment = cache.merge_environment(environment)

      curl_command = [
        "/usr/bin/curl",
        "--disable",
        "--silent",
        "--show-error",
        "--fail",
        "--output",
        "/dev/null",
        "--connect-timeout",
        "10",
        "--max-time",
        str(CHECK_TIMEOUT_SECONDS),
      ]
      if params.security_enabled:
        curl_command.extend(("--negotiate", "--user", ":"))
      history_url = (
        f"{params.spark_history_scheme}://"
        f"{params.spark_history_server_host}:"
        f"{params.spark_history_ui_port}/"
      )
      curl_command.extend(("--url", history_url))
      Execute(
        tuple(curl_command),
        user=params.smoke_user,
        environment=environment,
        timeout=CHECK_TIMEOUT_SECONDS + 5,
        tries=5,
        try_sleep=3,
        logoutput=True,
      )

      if not params.has_spark_thriftserver:
        return
      spark_utils.validate_executable(params.spark_beeline, "Spark Beeline executable")
      failures = []
      for host in params.spark_thriftserver_hosts:
        try:
          Execute(
            (params.spark_beeline, "-u", build_beeline_url(params, host), "-e", "SELECT 1"),
            user=params.smoke_user,
            environment=environment,
            timeout=CHECK_TIMEOUT_SECONDS,
            logoutput=True,
          )
          return
        except Exception as error:
          failures.append(f"{host}: {error}")
          Logger.warning(f"Spark Thrift Server check failed for {host}: {error}")
      raise Fail("Connection to all Spark Thrift Servers failed: " + "; ".join(failures))


if __name__ == "__main__":
  SparkServiceCheck().execute()
