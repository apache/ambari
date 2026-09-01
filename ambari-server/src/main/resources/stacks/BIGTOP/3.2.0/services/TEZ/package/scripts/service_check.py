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

from ambari_commons.os_family_impl import OsFamilyImpl
from resource_management.core.exceptions import Fail
from resource_management.core.resources.system import File
from resource_management.libraries.functions import StackFeature
from resource_management.libraries.functions.copy_tarball import copy_to_hdfs
from resource_management.libraries.functions.private_kerberos_cache import (
  PrivateKerberosCache,
)
from resource_management.libraries.functions.stack_features import check_stack_feature
from resource_management.libraries.resources.execute_hadoop import ExecuteHadoop
from resource_management.libraries.script import Script

import tez_utils


class TezServiceCheck(Script):
  """OS-independent Tez service check entry point."""


@OsFamilyImpl(os_family=OsFamilyImpl.DEFAULT)
class TezServiceCheckLinux(TezServiceCheck):
  def service_check(self, env):
    import params

    env.set_params(params)

    path_to_tez_jar = tez_utils.find_unique_examples_jar(
      params.tez_examples_jar_pattern
    )
    check_id = uuid.uuid4().hex
    hdfs_root = f"/tmp/ambari-tez-service-check-{check_id}"
    hdfs_input = f"{hdfs_root}/input/sample-tez-test"
    hdfs_output = f"{hdfs_root}/output"
    local_input = os.path.join(params.tmp_dir, f"tez-service-check-{check_id}.txt")

    if params.security_enabled:
      tez_utils.validate_keytab(params.hdfs_user_keytab, "HDFS service keytab")
      tez_utils.validate_keytab(params.smoke_user_keytab, "Tez smoke keytab")

    File(local_input, content="foo\nbar\nfoo\nbar\nfoo", mode=0o644)
    operation_error = None
    try:
      params.HdfsResource(
        f"{hdfs_root}/input",
        action="create_on_execute",
        type="directory",
        owner=params.smokeuser,
      )
      params.HdfsResource(
        hdfs_input,
        action="create_on_execute",
        type="file",
        owner=params.smokeuser,
        source=local_input,
      )

      if params.stack_version_formatted and check_stack_feature(
        StackFeature.ROLLING_UPGRADE, params.stack_version_formatted
      ):
        tarball_ready = copy_to_hdfs(
          "tez",
          params.user_group,
          params.hdfs_user,
          skip=params.sysprep_skip_copy_tarballs_hdfs,
        )
      else:
        params.HdfsResource(
          params.tez_lib_base_dir_path,
          type="directory",
          action="create_on_execute",
          owner=params.hdfs_user,
          mode=0o555,
        )
        params.HdfsResource(
          params.tez_lib_uris,
          action="create_on_execute",
          type="file",
          owner=params.hdfs_user,
          group=params.user_group,
          mode=0o444,
          source=os.path.join(params.tez_home, "lib", "tez.tar.gz"),
        )
        tarball_ready = True

      if not tarball_ready:
        raise Fail("Could not stage the Tez runtime archive in HDFS")
      params.HdfsResource(None, action="execute")

      if params.security_enabled:
        cache_context = PrivateKerberosCache(
          params.smokeuser,
          params.user_group,
          prefix="ambari-tez-service-check-",
        )
      else:
        cache_context = nullcontext(None)

      with cache_context as kerberos_cache:
        command_environment = (
          kerberos_cache.environment if kerberos_cache is not None else {}
        )
        if kerberos_cache is not None:
          kerberos_cache.kinit(
            params.kinit_path_local,
            params.smoke_user_keytab,
            params.smokeuser_principal,
            timeout=60,
          )

        ExecuteHadoop(
          ("jar", path_to_tez_jar, "orderedwordcount", hdfs_input, hdfs_output),
          tries=3,
          try_sleep=5,
          user=params.smokeuser,
          conf_dir=params.hadoop_conf_dir,
          bin_dir=params.hadoop_bin_dir,
          environment=command_environment,
        )
        ExecuteHadoop(
          ("fs", "-test", "-e", f"{hdfs_output}/_SUCCESS"),
          tries=10,
          try_sleep=6,
          user=params.smokeuser,
          conf_dir=params.hadoop_conf_dir,
          bin_dir=params.hadoop_bin_dir,
          environment=command_environment,
        )
    except Exception as error:
      operation_error = error
      raise
    finally:
      try:
        params.HdfsResource(
          hdfs_root,
          action="delete_on_execute",
          type="directory",
        )
        params.HdfsResource(None, action="execute")
      except Exception as cleanup_error:
        if operation_error is not None:
          raise Fail(
            f"Tez service check failed: {operation_error}; "
            f"HDFS cleanup also failed: {cleanup_error}"
          ) from operation_error
        raise
      finally:
        File(local_input, action="delete")


if __name__ == "__main__":
  TezServiceCheck().execute()
