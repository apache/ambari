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
from resource_management.core.exceptions import Fail
from resource_management.libraries.functions.private_kerberos_cache import (
  PrivateKerberosCache,
)


def webhcat_service_check():
  import params

  if not params.webhcat_server_host:
    raise Fail("WebHCat has no configured host")
  host = params.webhcat_server_host[0]
  base_url = f"http://{host}:{params.templeton_port}/templeton/v1"

  cache_context = nullcontext(None)
  if params.security_enabled:
    cache_context = PrivateKerberosCache(
      params.smokeuser,
      params.user_group,
      params.tmp_dir,
      "ambari-hive-webhcat-check-",
    )

  with cache_context as cache:
    environment = {"no_proxy": host}
    if cache is not None:
      cache.kinit(
        params.kinit_path_local,
        params.smoke_user_keytab,
        params.smokeuser_principal,
      )
      environment = cache.merge_environment(environment)

    common = ["curl", "--fail", "--silent", "--show-error"]
    if params.security_enabled:
      common.extend(("--negotiate", "-u", ":"))

    shell.checked_call(
      tuple(
        common
        + [
          f"{base_url}/status",
          "--get",
          "--data-urlencode",
          f"user.name={params.smokeuser}",
        ]
      ),
      user=params.smokeuser,
      env=environment,
      timeout=60,
      tries=3,
      try_sleep=5,
    )
    shell.checked_call(
      tuple(
        common
        + [
          f"{base_url}/ddl",
          "--data-urlencode",
          f"user.name={params.smokeuser}",
          "--data-urlencode",
          "exec=show databases",
        ]
      ),
      user=params.smokeuser,
      env=environment,
      timeout=60,
      tries=3,
      try_sleep=5,
    )
