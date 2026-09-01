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
from resource_management.core.exceptions import ExecutionFailed, Fail
from resource_management.core.logger import Logger
from resource_management.core.resources.system import Execute, Directory, File
from resource_management.core.source import InlineTemplate, Template
from resource_management.libraries.functions import solr_cloud_util
from resource_management.libraries.functions.format import format


def setup_solr(name=None):
  import params

  if name == "server":
    Directory(
      [
        params.solr_log_dir,
        params.solr_piddir,
        params.solr_datadir,
        params.solr_data_resources_dir,
      ],
      mode=0o755,
      cd_access="a",
      create_parents=True,
      owner=params.solr_user,
      group=params.user_group,
    )

    Directory(
      params.solr_conf,
      mode=0o750,
      cd_access="a",
      owner="root",
      group=params.user_group,
      create_parents=True,
    )

    File(
      format("{solr_conf}/solr-env.sh"),
      content=InlineTemplate(params.solr_env_content),
      mode=0o600,
      owner=params.solr_user,
      group=params.user_group,
    )

    File(
      format("{solr_datadir}/solr.xml"),
      content=InlineTemplate(params.solr_xml_content),
      owner=params.solr_user,
      group=params.user_group,
    )

    File(
      format("{solr_conf}/log4j2.xml"),
      content=InlineTemplate(params.solr_log4j_content),
      owner=params.solr_user,
      group=params.user_group,
    )

    custom_security_json_location = format("{solr_conf}/custom-security.json")
    File(
      custom_security_json_location,
      content=InlineTemplate(params.solr_security_json_content),
      owner=params.solr_user,
      group=params.user_group,
      mode=0o640,
    )

    if params.security_enabled:
      File(
        format("{solr_jaas_file}"),
        content=Template("solr_jaas.conf.j2"),
        owner=params.solr_user,
        group=params.user_group,
        mode=0o600,
      )

      File(
        format("{solr_conf}/security.json"),
        content=Template("solr-security.json.j2"),
        owner=params.solr_user,
        group=params.user_group,
        mode=0o640,
      )
    if os.path.exists(params.limits_conf_dir):
      File(
        os.path.join(params.limits_conf_dir, "solr.conf"),
        owner="root",
        group="root",
        mode=0o644,
        content=Template("solr.conf.j2"),
      )

  elif name == "client":
    solr_cloud_util.setup_solr_client(params.config)

  else:
    raise Fail("Nor client or server were selected to install.")


def setup_solr_znode_env():
  create_solr_znode()


def create_solr_znode():
  import params

  create_argv = [
    f"{params.solr_bindir}/solr",
    "zk",
    "mkroot",
    str(params.solr_znode),
    "-z",
    str(params.zookeeper_quorum),
  ]

  try:
    Execute(
      tuple(create_argv),
      environment={"SOLR_INCLUDE": f"{params.solr_conf}/solr-env.sh"},
      user=params.solr_user,
      logoutput=True,
    )
  except ExecutionFailed as error:
    if f"NodeExists for {params.solr_znode}" in str(error):
      Logger.info(f"Node {params.solr_znode} already exists.")
    else:
      raise
