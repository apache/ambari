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

from ambari_commons.os_family_impl import OsFamilyFuncImpl, OsFamilyImpl
from resource_management.core import sudo
from resource_management.core.exceptions import Fail
from resource_management.core.resources.system import Directory, File
from resource_management.core.source import InlineTemplate, Template


@OsFamilyFuncImpl(os_family=OsFamilyImpl.DEFAULT)
def zookeeper(type=None, upgrade_type=None):
  import params

  if type not in ("client", "server"):
    raise Fail("ZooKeeper configuration type must be client or server")

  Directory(
    params.config_dir,
    owner=params.zk_user,
    group=params.user_group,
    mode=0o750,
    create_parents=True,
  )
  File(
    os.path.join(params.config_dir, "zookeeper-env.sh"),
    content=InlineTemplate(params.zk_env_sh_template),
    owner=params.zk_user,
    group=params.user_group,
    mode=0o640,
  )
  config_file("zoo.cfg", "zoo.cfg.j2")
  config_file("configuration.xsl", "configuration.xsl.j2")

  if type == "server":
    if params.hostname not in params.zookeeper_hosts:
      raise Fail("The ZooKeeper server host list does not contain the current host")
    Directory(
      params.zk_pid_dir,
      owner=params.zk_user,
      group=params.user_group,
      mode=0o750,
      create_parents=True,
    )
    Directory(
      params.zk_log_dir,
      owner=params.zk_user,
      group=params.user_group,
      mode=0o750,
      create_parents=True,
    )
    Directory(
      params.zk_data_dir,
      owner=params.zk_user,
      group=params.user_group,
      mode=0o750,
      create_parents=True,
      cd_access="a",
    )
    myid = str(params.zookeeper_hosts.index(params.hostname) + 1)
    File(
      os.path.join(params.zk_data_dir, "myid"),
      content=myid + "\n",
      owner=params.zk_user,
      group=params.user_group,
      mode=0o640,
    )

  log4j_file = os.path.join(params.config_dir, "log4j.properties")
  if params.log4j_props is not None:
    File(
      log4j_file,
      content=InlineTemplate(params.log4j_props),
      owner=params.zk_user,
      group=params.user_group,
      mode=0o644,
    )
  elif sudo.path_exists(log4j_file):
    File(
      log4j_file,
      owner=params.zk_user,
      group=params.user_group,
      mode=0o644,
    )

  server_jaas = os.path.join(params.config_dir, "zookeeper_jaas.conf")
  client_jaas = os.path.join(params.config_dir, "zookeeper_client_jaas.conf")
  if params.security_enabled:
    if type == "server":
      config_file("zookeeper_jaas.conf", "zookeeper_jaas.conf.j2", mode=0o640)
    config_file(
      "zookeeper_client_jaas.conf",
      "zookeeper_client_jaas.conf.j2",
      mode=0o640,
    )
  else:
    File(client_jaas, action="delete")
    if type == "server":
      File(server_jaas, action="delete")


def config_file(name, template_name, mode=0o644):
  import params

  File(
    os.path.join(params.config_dir, name),
    content=Template(template_name),
    owner=params.zk_user,
    group=params.user_group,
    mode=mode,
  )
