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
import socket

from resource_management.core.exceptions import Fail
from resource_management.core.logger import Logger
from resource_management.core.resources.system import Directory, File
from resource_management.core.source import InlineTemplate, Template
from resource_management.libraries.functions.default import default
from resource_management.libraries.functions.generate_logfeeder_input_config import (
  generate_logfeeder_input_config,
)
from resource_management.libraries.resources.properties_file import PropertiesFile
from resource_management.libraries.resources.xml_config import XmlConfig

import spark_utils


def setup_spark(env, component_type, upgrade_type=None, action=None):
  import params

  if component_type not in ("client", "historyserver", "thriftserver"):
    raise Fail(f"Unsupported Spark component type: {component_type}")
  is_server = component_type in ("historyserver", "thriftserver")
  if is_server:
    Directory(
      params.spark_pid_dir,
      owner=params.spark_user,
      group=params.user_group,
      mode=0o750,
      create_parents=True,
    )
    Directory(
      params.spark_log_dir,
      owner=params.spark_user,
      group=params.user_group,
      mode=0o750,
      create_parents=True,
    )

  if is_server and action == "config":
    if component_type == "historyserver":
      Directory(
        params.spark_lib_dir,
        owner=params.spark_user,
        group=params.user_group,
        mode=0o750,
        create_parents=True,
      )
      Directory(
        params.spark_history_store_path,
        owner=params.spark_user,
        group=params.user_group,
        mode=0o750,
        create_parents=True,
      )
    params.HdfsResource(
      params.spark_hdfs_user_dir,
      type="directory",
      action="create_on_execute",
      owner=params.spark_user,
      group=params.user_group,
      mode=0o750,
    )
    params.HdfsResource(None, action="execute")
    generate_logfeeder_input_config(
      "", Template("input.config-spark.json.j2", extra_imports=[default])
    )

  spark_defaults = spark_utils.validate_properties(dict(params.spark_defaults), "Spark defaults")
  spark_defaults.pop("history.server.spnego.kerberos.principal", None)
  spark_defaults.pop("history.server.spnego.keytab.file", None)
  if params.security_enabled:
    spark_defaults["spark.history.kerberos.principal"] = spark_defaults[
      "spark.history.kerberos.principal"
    ].replace("_HOST", socket.getfqdn().lower())
    spark_defaults["spark.kerberos.principal"] = spark_defaults[
      "spark.kerberos.principal"
    ].replace("_HOST", socket.getfqdn().lower())
  if not params.spark_warehouse_dir:
    spark_defaults.pop("spark.sql.warehouse.dir", None)
  if not params.is_hive_installed:
    spark_defaults.pop("spark.sql.hive.metastore.version", None)
    spark_defaults.pop("spark.sql.hive.metastore.jars", None)

  PropertiesFile(
    os.path.join(params.spark_conf_dir, "spark-defaults.conf"),
    properties=spark_defaults,
    key_value_delimiter=" ",
    owner=params.spark_user,
    group=params.user_group,
    mode=0o640,
  )
  File(
    os.path.join(params.spark_conf_dir, "spark-env.sh"),
    owner=params.spark_user,
    group=params.user_group,
    content=InlineTemplate(params.spark_env_sh),
    mode=0o640,
  )
  File(
    os.path.join(params.spark_conf_dir, "log4j2.properties"),
    owner=params.spark_user,
    group=params.user_group,
    content=params.spark_log4j2_properties,
    mode=0o644,
  )
  File(
    os.path.join(params.spark_conf_dir, "metrics.properties"),
    owner=params.spark_user,
    group=params.user_group,
    content=params.spark_metrics_properties,
    mode=0o644,
  )

  if params.is_hive_installed:
    XmlConfig(
      "hive-site.xml",
      conf_dir=params.spark_conf_dir,
      configurations=params.spark_hive_properties,
      owner=params.spark_user,
      group=params.user_group,
      mode=0o640,
    )

  File(
    os.path.join(params.spark_conf_dir, "spark-thrift-fairscheduler.xml"),
    owner=params.spark_user,
    group=params.user_group,
    mode=0o644,
    content=InlineTemplate(params.spark_thrift_fairscheduler_content),
  )

  if component_type == "client":
    Logger.info("Configured Spark client")
