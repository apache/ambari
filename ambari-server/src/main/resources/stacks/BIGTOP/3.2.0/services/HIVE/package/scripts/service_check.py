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

import os
import time
from contextlib import nullcontext

from hcat_service_check import hcat_service_check
from webhcat_service_check import webhcat_service_check
from resource_management.core import shell
from resource_management.core.exceptions import Fail
from resource_management.core.logger import Logger
from resource_management.core.signal_utils import TerminateStrategy
from resource_management.core.utils import PasswordString
from resource_management.libraries.functions.private_kerberos_cache import (
  PrivateKerberosCache,
)
from resource_management.libraries.script.script import Script


class HiveServiceCheck(Script):
  def service_check(self, env):
    import params

    env.set_params(params)
    self.check_hive_server(params)
    hcat_service_check()
    webhcat_service_check()

  def check_hive_server(self, params):
    addresses = params.hive_server_hosts
    if not addresses:
      raise Fail("HiveServer2 has no configured host")

    cache_context = nullcontext(None)
    if params.hive_server2_authentication == "KERBEROS":
      cache_context = PrivateKerberosCache(
        params.smokeuser,
        params.user_group,
        params.tmp_dir,
        "ambari-hive-service-check-",
      )

    with cache_context as cache:
      environment = None
      if cache is not None:
        cache.kinit(
          params.kinit_path_local,
          params.smoke_user_keytab,
          params.smokeuser_principal,
        )
        environment = cache.environment

      deadline = time.monotonic() + 290
      last_error = None
      while time.monotonic() < deadline:
        for address in addresses:
          try:
            shell.checked_call(
              _beeline_command(params, address),
              user=params.smokeuser,
              env=environment,
              path=params.execute_path,
              timeout=30,
              timeout_kill_strategy=TerminateStrategy.KILL_PROCESS_TREE,
            )
            Logger.info(
              f"Successfully connected to HiveServer2 at {address}:"
              f"{params.hive_server_port}"
            )
            return
          except Exception as exception:
            last_error = exception
        time.sleep(5)

    raise Fail(
      "HiveServer2 service check did not find a working endpoint within 290 "
      f"seconds: {last_error}"
    )


def _beeline_command(params, address):
  properties = [f"transportMode={params.hive_transport_mode}"]
  if params.hive_transport_mode == "http":
    properties.append(f"httpPath={params.hive_http_endpoint}")
  if params.hive_server2_authentication == "NOSASL":
    properties.append("auth=noSasl")
  elif params.hive_server2_authentication == "KERBEROS":
    properties.append(f"principal={params.hive_server_principal}")

  if params.hive_ssl:
    properties.extend(
      (
        "ssl=true",
        f"sslTrustStore={params.hive_ssl_keystore_path}",
        f"trustStorePassword={params.hive_ssl_keystore_password}",
      )
    )

  url = f"jdbc:hive2://{address}:{params.hive_server_port}/;" + ";".join(
    properties
  )
  command = [
    os.path.join(params.hive_bin_dir, "beeline"),
    "-u",
    PasswordString(url) if params.hive_ssl else url,
  ]
  authentication = params.hive_server2_authentication
  username = None
  password = None
  if authentication == "LDAP":
    username, password = params.hive_ldap_user, params.hive_ldap_passwd
  elif authentication == "PAM":
    username, password = params.hive_pam_username, params.hive_pam_password
  elif authentication == "CUSTOM":
    username, password = params.hive_custom_username, params.hive_custom_password

  if username is not None:
    if not str(username).strip() or not str(password):
      raise Fail(f"{authentication} service checks require a username and password")
    command.extend(("-n", username, "-p", PasswordString(password)))
  else:
    command.extend(("-n", params.hive_user))
  command.extend(("-e", "show databases"))
  return tuple(command)


if __name__ == "__main__":
  HiveServiceCheck().execute()
