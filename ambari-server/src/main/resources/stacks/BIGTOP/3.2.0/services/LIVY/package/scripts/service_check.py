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
from resource_management.core.logger import Logger
from resource_management.core.resources.system import Execute
from resource_management.libraries.functions.private_kerberos_cache import (
  PrivateKerberosCache,
)
from resource_management.libraries.script.script import Script


_HOST_PATTERN = re.compile(r"[A-Za-z0-9_.:-]+", re.ASCII)
CHECK_COMMAND_TIMEOUT_SECONDS = 15


def build_livy_url(scheme, host, port):
  if scheme not in ("http", "https"):
    raise Fail(f"Unsupported Livy HTTP scheme: {scheme}")

  host = str(host).strip()
  if host.startswith("[") and host.endswith("]"):
    host = host[1:-1]
  if not host or not _HOST_PATTERN.fullmatch(host):
    raise Fail(f"Invalid Livy server host: {host!r}")

  try:
    port = int(port)
  except (TypeError, ValueError) as error:
    raise Fail(f"Invalid Livy server port: {port!r}") from error
  if not 0 < port <= 65535:
    raise Fail(f"Invalid Livy server port: {port!r}")

  url_host = f"[{host}]" if ":" in host else host
  return f"{scheme}://{url_host}:{port}/sessions"


class LivyServiceCheck(Script):
  def service_check(self, env):
    import params

    env.set_params(params)

    if not params.livy_livyserver_hosts:
      raise Fail("Livy service check requires at least one server host")

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
          "Secure Livy service check requires a smoke user principal and keytab"
        )
      cache_context = PrivateKerberosCache(
        params.smoke_user,
        params.user_group,
        prefix="ambari-livy-service-check-",
      )

    with cache_context as kerberos_cache:
      kerberos_environment = None
      if kerberos_cache is not None:
        kerberos_environment = kerberos_cache.environment
        kerberos_cache.kinit(
          params.kinit_path_local,
          params.smoke_user_keytab,
          params.smokeuser_principal,
          timeout=30,
        )

      failures = []
      for livy_host in params.livy_livyserver_hosts:
        try:
          livy_url = build_livy_url(
            params.livy_http_scheme,
            livy_host,
            params.livy_livyserver_port,
          )
          curl_argv = [
            "curl",
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
            curl_argv.extend(("--negotiate", "--user", ":"))
          curl_argv.extend(("--url", livy_url))

          execute_options = {}
          if kerberos_environment is not None:
            execute_options["environment"] = kerberos_environment
          Execute(
            tuple(curl_argv),
            timeout=CHECK_COMMAND_TIMEOUT_SECONDS + 5,
            tries=3,
            try_sleep=1,
            logoutput=True,
            user=params.smoke_user,
            **execute_options,
          )
          return
        except Exception as error:
          failures.append(f"{livy_host}: {error}")
          Logger.warning(
            f"Livy service check failed for host {livy_host!r}: {error}"
          )

      raise Fail(
        "Connection to all Livy servers failed: " + "; ".join(failures)
      )


if __name__ == "__main__":
  LivyServiceCheck().execute()
