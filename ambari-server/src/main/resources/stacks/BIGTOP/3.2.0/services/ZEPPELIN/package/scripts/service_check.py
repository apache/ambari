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
import re

from resource_management.core.exceptions import Fail
from resource_management.core.resources.system import Execute
from resource_management.core.signal_utils import TerminateStrategy
from resource_management.libraries.functions.private_kerberos_cache import (
  PrivateKerberosCache,
)
from resource_management.libraries.script.script import Script


_HOST_PATTERN = re.compile(r"[A-Za-z0-9_.:-]+", re.ASCII)
CHECK_COMMAND_TIMEOUT_SECONDS = 15


def build_zeppelin_url(ssl_enabled, host, port):
  host = str(host).strip()
  if host.startswith("[") and host.endswith("]"):
    host = host[1:-1]
  if not host or not _HOST_PATTERN.fullmatch(host):
    raise Fail(f"Invalid Zeppelin server host: {host!r}")

  try:
    port = int(port)
  except (TypeError, ValueError) as error:
    raise Fail(f"Invalid Zeppelin server port: {port!r}") from error
  if not 0 < port <= 65535:
    raise Fail(f"Invalid Zeppelin server port: {port!r}")

  url_host = f"[{host}]" if ":" in host else host
  scheme = "https" if ssl_enabled else "http"
  return f"{scheme}://{url_host}:{port}/api/version"


class ZeppelinServiceCheck(Script):
  def service_check(self, env):
    import params

    env.set_params(params)
    url = build_zeppelin_url(
      params.is_ui_ssl_enabled, params.zeppelin_host, params.zeppelin_port
    )

    cache_context = nullcontext(None)
    if params.security_enabled:
      if not all(
        str(value or "").strip()
        for value in (
          params.smoke_user,
          params.user_group,
          params.kinit_path_local,
          params.smoke_user_keytab,
          params.smokeuser_principal,
        )
      ):
        raise Fail(
          "Secure Zeppelin service check requires a smoke user principal and keytab"
        )
      cache_context = PrivateKerberosCache(
        params.smoke_user,
        params.user_group,
        prefix="ambari-zeppelin-service-check-",
      )

    with cache_context as kerberos_cache:
      environment = None
      if kerberos_cache is not None:
        kerberos_cache.kinit(
          params.kinit_path_local,
          params.smoke_user_keytab,
          params.smokeuser_principal,
          timeout=30,
        )
        environment = kerberos_cache.environment

      command = [
        "/usr/bin/curl",
        "--disable",
        "--silent",
        "--show-error",
        "--fail",
        "--output",
        "/dev/null",
        "--connect-timeout",
        "5",
        "--max-time",
        str(CHECK_COMMAND_TIMEOUT_SECONDS),
      ]
      if params.security_enabled:
        command.extend(("--negotiate", "--user", ":"))
      command.extend(("--url", url))

      execute_options = {}
      if environment is not None:
        execute_options["environment"] = environment
      Execute(
        tuple(command),
        tries=3,
        try_sleep=1,
        timeout=CHECK_COMMAND_TIMEOUT_SECONDS + 5,
        timeout_kill_strategy=TerminateStrategy.KILL_PROCESS_GROUP,
        user=params.smoke_user,
        logoutput=True,
        **execute_options,
      )


if __name__ == "__main__":
  ZeppelinServiceCheck().execute()
