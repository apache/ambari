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
import re
from contextlib import nullcontext

from resource_management.core import shell
from resource_management.core.exceptions import Fail
from resource_management.core.resources.system import Execute
from resource_management.libraries.functions.private_kerberos_cache import (
  PrivateKerberosCache,
)


def deregister():
  """Remove the running HiveServer2 instance from ZooKeeper before an upgrade."""
  import params

  cache_context = nullcontext(None)
  if params.security_enabled:
    cache_context = PrivateKerberosCache(
      params.hive_user,
      params.user_group,
      params.tmp_dir,
      "ambari-hive-deregister-",
    )

  with cache_context as cache:
    environment = None
    if cache is not None:
      cache.kinit(
        params.kinit_path_local,
        params.hive_server2_keytab,
        params.hive_principal,
      )
      environment = cache.environment

    current_version = _get_current_hiveserver_version(params, environment)
    Execute(
      (
        os.path.join(params.hive_bin_dir, "hive"),
        "--config",
        params.hive_conf_dir,
        "--service",
        "hiveserver2",
        "--deregister",
        current_version,
      ),
      user=params.hive_user,
      environment=environment,
      timeout=120,
      tries=1,
    )


def _get_current_hiveserver_version(params, environment=None):
  try:
    _, output = shell.checked_call(
      (os.path.join(params.hive_bin_dir, "hive"), "--version"),
      user=params.hive_user,
      env=environment,
      path=params.execute_path,
      timeout=60,
    )
  except Exception as exception:
    raise Fail(
      "Unable to execute hive --version to retrieve the HiveServer2 version"
    ) from exception

  match = re.search(r"^Hive ([0-9]+\.[0-9]+\.[^\s]+)", output, re.MULTILINE)
  if match is None:
    raise Fail(f"Unable to parse the HiveServer2 version from output: {output!r}")
  return match.group(1)
