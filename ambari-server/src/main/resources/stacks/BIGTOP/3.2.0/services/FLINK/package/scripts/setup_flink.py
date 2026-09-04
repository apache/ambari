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

from resource_management.core.exceptions import Fail
from resource_management.core.resources.system import Directory, File
from resource_management.core.source import InlineTemplate
from ambari_commons.os_family_impl import OsFamilyFuncImpl, OsFamilyImpl


@OsFamilyFuncImpl(os_family=OsFamilyImpl.DEFAULT)
def setup_flink(env, component, upgrade_type=None, action=None):
  import params

  if component not in ("client", "historyserver"):
    raise Fail(f"Unsupported Flink component: {component}")

  Directory(params.flink_etc_dir, owner="root", group="root", mode=0o755)
  Directory(
    params.flink_config_dir,
    owner="root",
    group=params.user_group,
    mode=0o750,
    create_parents=True,
  )
  Directory(
    params.flink_cli_log_dir,
    owner="root",
    group=params.user_group,
    mode=0o1770,
  )

  if component == "historyserver":
    Directory(
      params.flink_pid_dir,
      owner=params.flink_user,
      group=params.user_group,
      mode=0o750,
      create_parents=True,
    )
    Directory(
      params.flink_log_dir,
      owner=params.flink_user,
      group=params.user_group,
      mode=0o750,
      create_parents=True,
    )

  if component == "historyserver" and action == "config":
    params.HdfsResource(
      params.flink_hdfs_user_dir,
      type="directory",
      action="create_on_execute",
      owner=params.flink_user,
      group=params.user_group,
      mode=0o750,
    )

    params.HdfsResource(None, action="execute")

  # Flink 1.20 still prioritizes this legacy flat file, which is also valid for 1.15.
  File(os.path.join(params.flink_config_dir, "config.yaml"), action="delete")
  flink_conf_file_path = os.path.join(params.flink_config_dir, "flink-conf.yaml")
  File(
    flink_conf_file_path,
    owner="root",
    group=params.user_group,
    content=InlineTemplate(params.flink_conf_template),
    mode=0o640,
  )

  # create log4j.properties in /etc/conf dir
  File(
    os.path.join(params.flink_config_dir, "log4j.properties"),
    owner="root",
    group=params.user_group,
    content=params.flink_log4j_properties,
    mode=0o644,
  )

  # create log4j-cli.properties in /etc/conf dir
  File(
    os.path.join(params.flink_config_dir, "log4j-cli.properties"),
    owner="root",
    group=params.user_group,
    content=params.flink_log4j_cli_properties,
    mode=0o644,
  )

  # create log4j-console.properties in /etc/conf dir
  File(
    os.path.join(params.flink_config_dir, "log4j-console.properties"),
    owner="root",
    group=params.user_group,
    content=params.flink_log4j_console_properties,
    mode=0o644,
  )

  # create log4j-session.properties in /etc/conf dir
  File(
    os.path.join(params.flink_config_dir, "log4j-session.properties"),
    owner="root",
    group=params.user_group,
    content=params.flink_log4j_session_properties,
    mode=0o644,
  )
