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


# Local Imports
from hive import create_hive_hdfs_dirs


# Ambari Commons & Resource Management Imports
from resource_management.core.resources.system import Execute
from resource_management.libraries.functions.private_kerberos_cache import (
  PrivateKerberosCache,
)
from resource_management.libraries.script import Script


class HivePostUpgrade(Script):
  def move_tables(self, env):
    import params

    env.set_params(params)

    create_hive_hdfs_dirs()

    cache_context = nullcontext(None)
    if params.security_enabled:
      cache_context = PrivateKerberosCache(
        params.hdfs_user,
        params.user_group,
        params.tmp_dir,
        "ambari-hive-managed-migration-",
      )

    with cache_context as cache:
      environment = {"JAVA_HOME": params.java64_home}
      if cache is not None:
        cache.kinit(
          params.kinit_path_local,
          params.hdfs_user_keytab,
          params.hdfs_principal_name,
        )
        environment = cache.merge_environment(environment)
      Execute(
        (
          f"{params.hive_bin_dir}/hive",
          "--config",
          params.hive_conf_dir,
          "--service",
          "strictmanagedmigration",
          "--hiveconf",
          "hive.strict.managed.tables=true",
          "-m",
          "automatic",
          "--modifyManagedTables",
          "--oldWarehouseRoot",
          "/apps/hive/warehouse",
        ),
        environment=environment,
        user=params.hdfs_user,
        timeout=1200,
      )


if __name__ == "__main__":
  HivePostUpgrade().execute()
