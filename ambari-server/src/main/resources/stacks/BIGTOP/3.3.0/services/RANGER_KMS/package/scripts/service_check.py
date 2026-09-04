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
import ipaddress
import re

from resource_management.core.exceptions import Fail
from resource_management.core.resources.system import Execute
from resource_management.core.signal_utils import TerminateStrategy
from resource_management.libraries.functions.private_kerberos_cache import (
  PrivateKerberosCache,
)
from resource_management.libraries.script import Script

from kms_process import check_process


CHECK_TIMEOUT_SECONDS = 20
_HOSTNAME_PATTERN = re.compile(
  r"(?=.{1,253}\.?\Z)(?:[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}"
  r"[A-Za-z0-9])?\.)*[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}"
  r"[A-Za-z0-9])?\.?",
  re.ASCII,
)


def kms_service_url(scheme, host, port):
  if scheme not in ("http", "https"):
    raise Fail("Ranger KMS service-check scheme must be http or https")
  if isinstance(port, bool):
    raise Fail("Ranger KMS service-check port must be between 1 and 65535")
  port_text = str(port).strip()
  if re.fullmatch(r"[0-9]+", port_text) is None or not 1 <= int(port_text) <= 65535:
    raise Fail("Ranger KMS service-check port must be between 1 and 65535")
  if not isinstance(host, str) or not host or host != host.strip():
    raise Fail("Ranger KMS service-check host is invalid")
  if any(character.isspace() or ord(character) < 32 for character in host):
    raise Fail("Ranger KMS service-check host is invalid")

  authority = host
  if ":" in host:
    ipv6_host = host[1:-1] if host.startswith("[") and host.endswith("]") else host
    try:
      ipaddress.IPv6Address(ipv6_host)
    except ipaddress.AddressValueError as error:
      raise Fail("Ranger KMS service-check host is invalid") from error
    authority = f"[{ipv6_host}]"
  elif re.fullmatch(r"[0-9.]+", host):
    try:
      ipaddress.IPv4Address(host)
    except ipaddress.AddressValueError as error:
      raise Fail("Ranger KMS service-check host is invalid") from error
  elif _HOSTNAME_PATTERN.fullmatch(host) is None:
    raise Fail("Ranger KMS service-check host is invalid")

  return f"{scheme}://{authority}:{port_text}/kms/v1/keys/names"


class KmsServiceCheck(Script):
  def service_check(self, env):
    import params

    env.set_params(params)
    check_process(params.ranger_kms_pid_file, params.kms_user, params.kms_group)

    cache_context = nullcontext(None)
    if params.security_enabled:
      rangerkms_keytab = getattr(params, "rangerkms_keytab", None)
      rangerkms_principal = getattr(params, "rangerkms_principal", None)
      credentials = (
        getattr(params, "kms_user", None),
        getattr(params, "kms_group", None),
        getattr(params, "kinit_path_local", None),
        rangerkms_keytab,
        rangerkms_principal,
      )
      if not all(str(value or "").strip() for value in credentials):
        raise Fail(
          "Secure Ranger KMS service check requires a service principal and keytab"
        )
      cache_context = PrivateKerberosCache(
        params.kms_user,
        params.kms_group,
        temp_dir=params.tmp_dir,
        prefix="ambari-ranger-kms-check-",
      )

    with cache_context as kerberos_cache:
      environment = None
      if kerberos_cache is not None:
        kerberos_cache.kinit(
          params.kinit_path_local,
          rangerkms_keytab,
          rangerkms_principal,
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
      command.extend(
        (
          "--url",
          kms_service_url(params.url_scheme, params.current_host, params.kms_port),
        )
      )
      execute_options = {}
      if environment is not None:
        execute_options["environment"] = environment
      Execute(
        tuple(command),
        user=params.kms_user,
        tries=5,
        try_sleep=3,
        timeout=CHECK_TIMEOUT_SECONDS + 5,
        timeout_kill_strategy=TerminateStrategy.KILL_PROCESS_GROUP,
        logoutput=True,
        **execute_options,
      )


if __name__ == "__main__":
  KmsServiceCheck().execute()
