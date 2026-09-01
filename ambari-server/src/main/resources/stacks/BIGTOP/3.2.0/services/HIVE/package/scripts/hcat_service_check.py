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
from resource_management.core.logger import Logger
from resource_management.libraries.functions import get_unique_id_and_date
from resource_management.libraries.functions.private_kerberos_cache import (
  PrivateKerberosCache,
)


def hcat_service_check():
  import params

  suffix = re.sub(r"[^A-Za-z0-9_]", "_", get_unique_id_and_date())
  table_name = f"ambari_hcat_smoke_{suffix}"
  purge = " PURGE" if params.purge_tables == "true" else ""
  output_path = f"{params.hive_apps_whs_dir.rstrip('/')}/{table_name}"

  smoke_cache_context = _cache_context(
    params,
    params.smokeuser,
    "ambari-hive-hcat-check-",
  )
  with smoke_cache_context as cache:
    environment = _initialize_cache(
      cache,
      params,
      params.smoke_user_keytab,
      params.smokeuser_principal,
    )
    try:
      _run_hcat(
        params,
        f"create table {table_name} (id int, name string) stored as rcfile",
        environment,
      )
      _check_hdfs_path(params, output_path)
    except Exception:
      try:
        _drop_smoke_table(params, table_name, purge, environment)
      except Exception as cleanup_error:
        Logger.error(
          f"Unable to clean up HCatalog smoke table {table_name}: {cleanup_error}"
        )
      raise
    else:
      _drop_smoke_table(params, table_name, purge, environment)


def _drop_smoke_table(params, table_name, purge, environment):
  _run_hcat(
    params,
    f"drop table if exists {table_name}{purge}",
    environment,
  )


def _run_hcat(params, statement, environment):
  shell.checked_call(
    (os.path.join(params.hive_hcatalog_home, "bin", "hcat"), "-e", statement),
    user=params.smokeuser,
    env={"JAVA_HOME": params.java64_home, **(environment or {})},
    path=params.execute_path,
    timeout=120,
    tries=3,
    try_sleep=5,
  )


def _check_hdfs_path(params, path):
  cache_context = _cache_context(
    params,
    params.hdfs_user,
    "ambari-hive-hcat-hdfs-check-",
  )
  with cache_context as cache:
    environment = _initialize_cache(
      cache,
      params,
      params.hdfs_user_keytab,
      params.hdfs_principal_name,
    )
    return_code, output = shell.call(
      ("hdfs", "dfs", "-test", "-e", path),
      user=params.hdfs_user,
      env=environment,
      path=params.execute_path,
      timeout=60,
      shell=False,
    )
    if return_code != 0:
      raise Fail(f"HCatalog did not create expected warehouse path {path}: {output}")


def _cache_context(params, user, prefix):
  if not params.security_enabled:
    return nullcontext(None)
  return PrivateKerberosCache(user, params.user_group, params.tmp_dir, prefix)


def _initialize_cache(cache, params, keytab, principal):
  if cache is None:
    return None
  cache.kinit(params.kinit_path_local, keytab, principal)
  return cache.environment
