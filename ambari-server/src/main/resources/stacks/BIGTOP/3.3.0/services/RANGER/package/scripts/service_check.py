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
import os
from urllib.parse import urlsplit

from resource_management.core.exceptions import Fail
from resource_management.core.logger import Logger
from resource_management.core.resources.system import Execute
from resource_management.libraries.functions.private_kerberos_cache import (
  PrivateKerberosCache,
)
from resource_management.libraries.script import Script


CHECK_TIMEOUT_SECONDS = 20


class RangerServiceCheck(Script):
  def service_check(self, env):
    import params

    env.set_params(params)
    self.check_ranger_admin_service(params)

  def check_ranger_admin_service(self, params):
    if self.is_ru_rangeradmin_in_progress(params.upgrade_marker_file):
      Logger.info(
        "Ranger admin process not running - skipping as stack upgrade is in progress"
      )
      return

    ranger_external_url = self._validate_ranger_admin_url(params.ranger_external_url)
    cache_context = nullcontext(None)
    if params.security_enabled:
      ranger_admin_keytab = getattr(params, "ranger_admin_keytab", None)
      ranger_admin_principal = getattr(params, "ranger_admin_jaas_principal", None)
      credentials = (
        getattr(params, "unix_user", None),
        getattr(params, "unix_group", None),
        getattr(params, "kinit_path_local", None),
        ranger_admin_keytab,
        ranger_admin_principal,
      )
      if not all(str(value or "").strip() for value in credentials):
        raise Fail(
          "Secure Ranger Admin service check requires a service principal and keytab"
        )
      cache_context = PrivateKerberosCache(
        params.unix_user,
        params.unix_group,
        temp_dir=params.tmp_dir,
        prefix="ambari-ranger-admin-check-",
      )

    with cache_context as kerberos_cache:
      environment = None
      if kerberos_cache is not None:
        kerberos_cache.kinit(
          params.kinit_path_local,
          ranger_admin_keytab,
          ranger_admin_principal,
          timeout=30,
        )
        environment = kerberos_cache.environment

      command = [
        "/usr/bin/curl",
        "--disable",
        "--fail",
        "--silent",
        "--show-error",
        "--output",
        "/dev/null",
        "--connect-timeout",
        "5",
        "--max-time",
        str(CHECK_TIMEOUT_SECONDS),
      ]
      if params.security_enabled:
        command.extend(("--negotiate", "--user", ":"))
      command.extend(("--url", ranger_external_url + "/login.jsp"))
      execute_options = {}
      if environment is not None:
        execute_options["environment"] = environment
      Execute(
        tuple(command),
        tries=10,
        try_sleep=3,
        timeout=CHECK_TIMEOUT_SECONDS + 5,
        user=params.unix_user,
        logoutput=True,
        **execute_options,
      )

  @staticmethod
  def _validate_ranger_admin_url(ranger_external_url):
    try:
      parsed_url = urlsplit(ranger_external_url)
      hostname = parsed_url.hostname
      port = parsed_url.port
    except (TypeError, ValueError) as error:
      raise Fail("Ranger Admin URL is invalid") from error
    if (
      parsed_url.scheme not in ("http", "https")
      or not hostname
      or parsed_url.username is not None
      or parsed_url.password is not None
      or parsed_url.query
      or parsed_url.fragment
      or (port is not None and not 1 <= port <= 65535)
    ):
      raise Fail("Ranger Admin URL is invalid")
    return ranger_external_url.rstrip("/")

  def is_ru_rangeradmin_in_progress(self, upgrade_marker_file):
    return os.path.isfile(upgrade_marker_file)


if __name__ == "__main__":
  RangerServiceCheck().execute()
