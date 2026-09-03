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

from resource_management.libraries.script.script import Script
from resource_management.core.resources.system import Execute, Directory, File, Link
from resource_management.core.signal_utils import TerminateStrategy
from resource_management.core.resources import Package
from resource_management.core.source import Template
from resource_management.core.resources.service import ServiceConfig
from resource_management.libraries.resources.xml_config import XmlConfig

from resource_management.core.exceptions import Fail
from resource_management.core.logger import Logger
from resource_management.libraries.functions.format import format
import os
from ambari_commons.os_family_impl import OsFamilyFuncImpl, OsFamilyImpl
from ambari_commons import OSConst
from resource_management.libraries.functions.lzo_utils import install_lzo_if_needed
from hdfs_kerberos import hdfs_kerberos_environment


@OsFamilyFuncImpl(os_family=OsFamilyImpl.DEFAULT)
def hdfs(name=None):
  import params

  if params.create_lib_snappy_symlinks:
    install_snappy()

  # On some OS this folder could be not exists, so we will create it before pushing there files
  Directory(params.limits_conf_dir, create_parents=True, owner="root", group="root")

  File(
    os.path.join(params.limits_conf_dir, "hdfs.conf"),
    owner="root",
    group="root",
    mode=0o644,
    content=Template("hdfs.conf.j2"),
  )

  if params.security_enabled:
    File(
      os.path.join(params.hadoop_conf_dir, "hdfs_dn_jaas.conf"),
      owner=params.hdfs_user,
      group=params.user_group,
      mode=0o640,
      content=Template("hdfs_dn_jaas.conf.j2"),
    )
    File(
      os.path.join(params.hadoop_conf_dir, "hdfs_nn_jaas.conf"),
      owner=params.hdfs_user,
      group=params.user_group,
      mode=0o640,
      content=Template("hdfs_nn_jaas.conf.j2"),
    )
    if params.dfs_ha_enabled:
      File(
        os.path.join(params.hadoop_conf_dir, "hdfs_jn_jaas.conf"),
        owner=params.hdfs_user,
        group=params.user_group,
        mode=0o640,
        content=Template("hdfs_jn_jaas.conf.j2"),
      )

    tc_mode = 0o644
    tc_owner = "root"
  else:
    tc_mode = None
    tc_owner = params.hdfs_user

  if "hadoop-policy" in params.config["configurations"]:
    XmlConfig(
      "hadoop-policy.xml",
      conf_dir=params.hadoop_conf_dir,
      configurations=params.config["configurations"]["hadoop-policy"],
      configuration_attributes=params.config["configurationAttributes"][
        "hadoop-policy"
      ],
      owner=params.hdfs_user,
      group=params.user_group,
    )

  if "ssl-client" in params.config["configurations"]:
    XmlConfig(
      "ssl-client.xml",
      conf_dir=params.hadoop_conf_dir,
      configurations=params.config["configurations"]["ssl-client"],
      configuration_attributes=params.config["configurationAttributes"]["ssl-client"],
      owner=params.hdfs_user,
      group=params.user_group,
    )

    Directory(
      params.hadoop_conf_secure_dir,
      create_parents=True,
      owner="root",
      group=params.user_group,
      cd_access="a",
    )

    XmlConfig(
      "ssl-client.xml",
      conf_dir=params.hadoop_conf_secure_dir,
      configurations=params.config["configurations"]["ssl-client"],
      configuration_attributes=params.config["configurationAttributes"]["ssl-client"],
      owner=params.hdfs_user,
      group=params.user_group,
    )

  if "ssl-server" in params.config["configurations"]:
    XmlConfig(
      "ssl-server.xml",
      conf_dir=params.hadoop_conf_dir,
      configurations=params.config["configurations"]["ssl-server"],
      configuration_attributes=params.config["configurationAttributes"]["ssl-server"],
      owner=params.hdfs_user,
      group=params.user_group,
    )

  XmlConfig(
    "hdfs-site.xml",
    conf_dir=params.hadoop_conf_dir,
    configurations=params.config["configurations"]["hdfs-site"],
    configuration_attributes=params.config["configurationAttributes"]["hdfs-site"],
    owner=params.hdfs_user,
    group=params.user_group,
  )

  XmlConfig(
    "hdfs-rbf-site.xml",
    conf_dir=params.hadoop_conf_dir,
    configurations=params.config["configurations"]["hdfs-rbf-site"],
    configuration_attributes=params.config["configurationAttributes"]["hdfs-rbf-site"],
    mode=0o644,
    owner=params.hdfs_user,
    group=params.user_group,
  )

  XmlConfig(
    "core-site.xml",
    conf_dir=params.hadoop_conf_dir,
    configurations=params.config["configurations"]["core-site"],
    configuration_attributes=params.config["configurationAttributes"]["core-site"],
    owner=params.hdfs_user,
    group=params.user_group,
    mode=0o644,
  )

  File(
    os.path.join(params.hadoop_conf_dir, "slaves"),
    owner=tc_owner,
    content=Template("slaves.j2"),
  )

  install_lzo_if_needed()


def install_snappy():
  import params

  Directory(
    [params.so_target_dir_x86, params.so_target_dir_x64],
    create_parents=True,
  )
  Link(
    params.so_target_x86,
    to=params.so_src_x86,
  )
  Link(
    params.so_target_x64,
    to=params.so_src_x64,
  )


class ConfigStatusParser:
  def __init__(self):
    self.reconfig_successful = False

  def handle_new_line(self, line, is_stderr):
    if is_stderr:
      return

    if line.startswith("SUCCESS: Changed property"):
      self.reconfig_successful = True

    Logger.info(f"[reconfig] {line}")


@OsFamilyFuncImpl(os_family=OsFamilyImpl.DEFAULT)
def reconfig(componentName, componentAddress):
  import params

  if componentName not in {"namenode", "datanode", "router"}:
    raise Fail(f"Unsupported HDFS reconfiguration component: {componentName!r}")
  if (
    not isinstance(componentAddress, str)
    or componentAddress != componentAddress.strip()
    or not componentAddress
  ):
    raise Fail(f"Invalid HDFS reconfiguration address: {componentAddress!r}")

  with hdfs_kerberos_environment(
    params,
    "ambari-hdfs-reconfigure-",
    keytab=params.nn_keytab if params.security_enabled else None,
    principal=params.nn_principal_name if params.security_enabled else None,
  ) as command_environment:
    nn_reconfig_cmd = (
      "hdfs",
      "--config",
      params.hadoop_conf_dir,
      "dfsadmin",
      "-reconfig",
      componentName,
      componentAddress,
      "start",
    )

    Execute(
      nn_reconfig_cmd,
      user=params.hdfs_user,
      logoutput=True,
      path=[params.hadoop_bin_dir],
      environment=command_environment,
      timeout=120,
      timeout_kill_strategy=TerminateStrategy.KILL_PROCESS_GROUP,
    )

    nn_reconfig_cmd = (
      "hdfs",
      "--config",
      params.hadoop_conf_dir,
      "dfsadmin",
      "-reconfig",
      componentName,
      componentAddress,
      "status",
    )
    config_status_parser = ConfigStatusParser()
    Execute(
      nn_reconfig_cmd,
      user=params.hdfs_user,
      logoutput=False,
      path=[params.hadoop_bin_dir],
      on_new_line=config_status_parser.handle_new_line,
      environment=command_environment,
      timeout=120,
      timeout_kill_strategy=TerminateStrategy.KILL_PROCESS_GROUP,
    )

  if not config_status_parser.reconfig_successful:
    Logger.info("Reconfiguration failed")
    raise Fail("Reconfiguration failed!")

  Logger.info("Reconfiguration successfully completed.")
