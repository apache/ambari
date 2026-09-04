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

Ambari Agent

"""

import os
import uuid
from contextlib import nullcontext

from resource_management.libraries.script.script import Script
from resource_management.core.exceptions import Fail
from resource_management.core.logger import Logger
from resource_management.core.resources.system import Execute, File
from resource_management.core.signal_utils import TerminateStrategy
from ambari_commons.os_family_impl import OsFamilyImpl
from resource_management.libraries.functions.private_kerberos_cache import (
  PrivateKerberosCache,
)

import yarn_process_utils


class MapReduce2ServiceCheck(Script):
  def service_check(self, env):
    pass


@OsFamilyImpl(os_family=OsFamilyImpl.DEFAULT)
class MapReduce2ServiceCheckDefault(MapReduce2ServiceCheck):
  def service_check(self, env):
    import params

    env.set_params(params)

    smokeuser = yarn_process_utils.validate_path_segment(
      params.smokeuser, "MapReduce service-check user"
    )

    run_id = uuid.uuid4().hex
    jar_path = os.path.join(
      params.hadoop_mapred_home, "hadoop-mapreduce-examples.jar"
    )
    if not os.path.isfile(jar_path):
      raise Fail(f"MapReduce examples jar is missing: {jar_path}")
    local_input_file = os.path.join(
      params.tmp_dir, f"ambari-mapreduce-smoke-{run_id}.txt"
    )
    smoke_root = f"/user/{smokeuser}/ambari-mapreduce-smoke-{run_id}"
    input_file = f"{smoke_root}/input"
    output_file = f"{smoke_root}/output"

    params.HdfsResource(
      f"/user/{smokeuser}",
      type="directory",
      action="create_on_execute",
      owner=smokeuser,
      mode=params.smoke_hdfs_user_mode,
    )

    File(
      local_input_file,
      content="Ambari MapReduce service check\n",
      owner=smokeuser,
      mode=0o600,
      replace=False,
    )
    primary_error = None
    try:
      params.HdfsResource(
        input_file,
        action="create_on_execute",
        type="file",
        source=local_input_file,
        owner=smokeuser,
        mode=0o600,
        dfs_type=params.dfs_type,
      )
      params.HdfsResource(None, action="execute")

      cache_context = (
        PrivateKerberosCache(
          smokeuser,
          params.user_group,
          temp_dir=params.tmp_dir,
          prefix="ambari-mapreduce-check-",
        )
        if params.security_enabled
        else nullcontext(None)
      )
      with cache_context as kerberos_cache:
        command_environment = {}
        if kerberos_cache is not None:
          kerberos_cache.kinit(
            params.kinit_path_local,
            params.smoke_user_keytab,
            params.smokeuser_principal,
          )
          command_environment = kerberos_cache.environment

        hadoop_command = (
          os.path.join(params.hadoop_bin_dir, "hadoop"),
          "--config",
          params.hadoop_conf_dir,
        )
        Execute(
          hadoop_command + ("jar", jar_path, "wordcount", input_file, output_file),
          user=smokeuser,
          environment=command_environment,
          logoutput=True,
          timeout=330,
          timeout_kill_strategy=TerminateStrategy.KILL_PROCESS_GROUP,
        )

        Execute(
          hadoop_command + ("fs", "-test", "-e", output_file),
          user=smokeuser,
          environment=command_environment,
          timeout=60,
          timeout_kill_strategy=TerminateStrategy.KILL_PROCESS_GROUP,
        )
    except Exception as error:
      primary_error = error
      raise
    finally:
      cleanup_errors = []
      try:
        params.HdfsResource(
          smoke_root,
          action="delete_on_execute",
          type="directory",
          dfs_type=params.dfs_type,
        )
        params.HdfsResource(None, action="execute")
      except Exception as error:
        cleanup_errors.append(f"HDFS cleanup failed: {error}")
      try:
        File(local_input_file, action="delete")
      except Exception as error:
        cleanup_errors.append(f"local cleanup failed: {error}")
      if cleanup_errors:
        cleanup_message = "; ".join(cleanup_errors)
        if primary_error is None:
          raise RuntimeError(cleanup_message)
        Logger.warning(
          f"Could not clean MapReduce service-check resources: {cleanup_message}"
        )


if __name__ == "__main__":
  MapReduce2ServiceCheck().execute()
