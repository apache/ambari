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

import json
import os
import uuid

from ambari_commons.os_family_impl import OsFamilyImpl
from resource_management.core import shell
from resource_management.core.exceptions import Fail
from resource_management.core.resources.system import File
from resource_management.core.signal_utils import TerminateStrategy
from resource_management.libraries.functions.private_kerberos_cache import (
  PrivateKerberosCache,
)
from resource_management.libraries.script.script import Script


def ruby_quote(value):
  value = str(value)
  return "'" + value.replace("\\", "\\\\").replace("'", "\\'").replace(
    "\r", "\\r"
  ).replace("\n", "\\n") + "'"


def drop_table_commands(table):
  quoted_table = ruby_quote(table)
  return (
    f"if exists {quoted_table}\n"
    f"  disable {quoted_table} if is_enabled {quoted_table}\n"
    f"  drop {quoted_table}\n"
    "end\n"
  )


def read_write_commands(table, value, result_file, operation_id):
  # JRuby calls the installed HBase Java SDK. Shell output is diagnostic only.
  return (
    "require 'json'\n"
    "java_import org.apache.hadoop.hbase.HBaseConfiguration\n"
    "java_import org.apache.hadoop.hbase.TableName\n"
    "java_import org.apache.hadoop.hbase.client.ConnectionFactory\n"
    "java_import org.apache.hadoop.hbase.client.TableDescriptorBuilder\n"
    "java_import org.apache.hadoop.hbase.client.ColumnFamilyDescriptorBuilder\n"
    "java_import org.apache.hadoop.hbase.client.Put\n"
    "java_import org.apache.hadoop.hbase.client.Get\n"
    "java_import org.apache.hadoop.hbase.util.Bytes\n"
    "connection = ConnectionFactory.createConnection(HBaseConfiguration.create)\n"
    "admin = connection.getAdmin\n"
    f"name = TableName.valueOf({ruby_quote(table)})\n"
    "family = Bytes.toBytes('family')\n"
    "column = Bytes.toBytes('col01')\n"
    "row = Bytes.toBytes('row01')\n"
    f"expected = Bytes.toBytes({ruby_quote(value)})\n"
    "client = nil\n"
    "begin\n"
    "  descriptor = TableDescriptorBuilder.newBuilder(name)\n"
    "  descriptor.setColumnFamily(ColumnFamilyDescriptorBuilder.of(family))\n"
    "  admin.createTable(descriptor.build) unless admin.tableExists(name)\n"
    "  client = connection.getTable(name)\n"
    "  client.put(Put.new(row).addColumn(family, column, expected))\n"
    "  actual = client.get(Get.new(row).addColumn(family, column)).getValue(family, column)\n"
    "  raise 'HBase SDK read/write mismatch' unless java.util.Arrays.equals(expected, actual)\n"
    f"  receipt = {{'schemaVersion' => 1, 'operationId' => {ruby_quote(operation_id)}, "
    f"'table' => {ruby_quote(table)}, 'readWriteVerified' => true}}\n"
    f"  File.write({ruby_quote(result_file)}, JSON.generate(receipt))\n"
    "ensure\n"
    "  client.close unless client.nil?\n"
    "  admin.close\n"
    "  connection.close\n"
    "end\nexit\n"
  )


def run_hbase_shell(
  params,
  user,
  command_file,
  keytab=None,
  principal=None,
  prefix="ambari-hbase-check-",
  tries=1,
):
  command = (
    params.hbase_cmd,
    "--config",
    params.hbase_conf_dir,
    "shell",
    "-n",
    command_file,
  )
  if not params.security_enabled:
    return shell.checked_call(
      command,
      user=user,
      env={"HBASE_HOME": params.hbase_home},
      timeout=60,
      timeout_kill_strategy=TerminateStrategy.KILL_PROCESS_GROUP,
      tries=tries,
      try_sleep=5,
    )[1]

  if not all(str(value or "").strip() for value in (keytab, principal)):
    raise Fail("Secure HBase service check requires a principal and keytab")
  with PrivateKerberosCache(
    user,
    params.user_group,
    temp_dir=params.exec_tmp_dir,
    prefix=prefix,
  ) as kerberos_cache:
    kerberos_cache.kinit(
      params.kinit_path_local, keytab, principal, timeout=30
    )
    return shell.checked_call(
      command,
      user=user,
      env={**kerberos_cache.environment, "HBASE_HOME": params.hbase_home},
      timeout=60,
      timeout_kill_strategy=TerminateStrategy.KILL_PROCESS_GROUP,
      tries=tries,
      try_sleep=5,
    )[1]


class HbaseServiceCheck(Script):
  """Base HBase service-check entry point."""


@OsFamilyImpl(os_family=OsFamilyImpl.DEFAULT)
class HbaseServiceCheckDefault(HbaseServiceCheck):
  def service_check(self, env):
    import params

    env.set_params(params)
    unique_id = uuid.uuid4().hex
    table = "ambari_smoke_" + unique_id
    grant_file = os.path.join(
      params.exec_tmp_dir, f"hbase-grant-{unique_id}.hbase"
    )
    check_file = os.path.join(
      params.exec_tmp_dir, f"hbase-check-{unique_id}.hbase"
    )
    cleanup_file = os.path.join(
      params.exec_tmp_dir, f"hbase-cleanup-{unique_id}.hbase"
    )
    result_file = os.path.join(params.exec_tmp_dir, f"hbase-result-{unique_id}.json")
    command_files = (grant_file, check_file, cleanup_file, result_file)

    primary_error = None
    cleanup_ready = False
    try:
      File(
        grant_file,
        owner=params.hbase_user,
        group=params.user_group,
        mode=0o600,
        content=(
          f"grant {ruby_quote(params.smoke_test_user)}, "
          f"{ruby_quote(params.smokeuser_permissions)}\nexit\n"
        ),
      )
      File(
        check_file,
        owner=params.smoke_test_user,
        group=params.user_group,
        mode=0o600,
        content=read_write_commands(table, params.service_check_data, result_file, unique_id),
      )
      File(
        cleanup_file,
        owner=params.smoke_test_user,
        group=params.user_group,
        mode=0o600,
        content=drop_table_commands(table) + "exit\n",
      )
      File(result_file, owner=params.smoke_test_user, group=params.user_group,
           mode=0o600, content="{}")
      cleanup_ready = True

      if params.security_enabled:
        run_hbase_shell(
          params,
          params.hbase_user,
          grant_file,
          params.hbase_user_keytab,
          params.hbase_principal_name,
          prefix="ambari-hbase-grant-",
          tries=3,
        )

      run_hbase_shell(
        params,
        params.smoke_test_user,
        check_file,
        params.smoke_user_keytab,
        params.smokeuser_principal,
        prefix="ambari-hbase-smoke-",
        tries=3,
      )
      with open(result_file, "r", encoding="utf-8") as stream:
        content = stream.read(8193)
      try:
        result = json.loads(content) if len(content) <= 8192 else None
      except (TypeError, ValueError):
        result = None
      if (not isinstance(result, dict) or type(result.get("schemaVersion")) is not int
          or result != {"schemaVersion": 1, "operationId": unique_id,
                        "table": table, "readWriteVerified": True}
          or result.get("readWriteVerified") is not True):
        raise Fail("HBase service check has no valid client API read/write receipt")
    except Exception as error:
      primary_error = error

    cleanup_error = None
    try:
      if cleanup_ready:
        run_hbase_shell(
          params,
          params.smoke_test_user,
          cleanup_file,
          params.smoke_user_keytab,
          params.smokeuser_principal,
          prefix="ambari-hbase-smoke-cleanup-",
        )
    except Exception as error:
      cleanup_error = error
    finally:
      for command_file in command_files:
        File(command_file, action="delete")

    if primary_error is not None and cleanup_error is not None:
      raise Fail(
        f"HBase service check failed: {primary_error}; cleanup also failed: "
        f"{cleanup_error}"
      ) from primary_error
    if primary_error is not None:
      raise primary_error
    if cleanup_error is not None:
      raise Fail(f"HBase service check cleanup failed: {cleanup_error}") from cleanup_error


if __name__ == "__main__":
  HbaseServiceCheck().execute()
