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

import re

from resource_management.core.exceptions import Fail
from resource_management.core.logger import Logger
from resource_management.core.resources.system import Execute
from resource_management.core.signal_utils import TerminateStrategy


def validate_znode(znode):
  if not isinstance(znode, str) or not znode.startswith("/") or znode == "/":
    raise Fail("ZooKeeper znode must be an absolute non-root path")
  if (
    "\\" in znode
    or "//" in znode
    or any(character.isspace() or ord(character) < 32 for character in znode)
    or any(part in ("", ".", "..") for part in znode.split("/")[1:])
  ):
    raise Fail(f"ZooKeeper znode is unsafe: {znode!r}")
  return znode


def validate_connection_string(connection_string):
  if not isinstance(connection_string, str) or not connection_string:
    raise Fail("ZooKeeper connection string must not be empty")
  if any(character.isspace() or ord(character) < 32 for character in connection_string):
    raise Fail("ZooKeeper connection string must not contain whitespace")

  server_list, separator, chroot = connection_string.partition("/")
  if separator:
    validate_znode(f"/{chroot}")
  endpoints = server_list.split(",")
  if not endpoints or any(not endpoint for endpoint in endpoints):
    raise Fail("ZooKeeper connection string contains an empty endpoint")
  for endpoint in endpoints:
    if endpoint.startswith("["):
      closing_bracket = endpoint.find("]")
      if closing_bracket < 2 or endpoint[closing_bracket + 1 : closing_bracket + 2] != ":":
        raise Fail(f"Invalid ZooKeeper endpoint: {endpoint}")
      host = endpoint[1:closing_bracket]
      port = endpoint[closing_bracket + 2 :]
      if not re.fullmatch(r"[0-9A-Fa-f:.]+", host):
        raise Fail(f"Invalid ZooKeeper endpoint: {endpoint}")
    else:
      host, colon, port = endpoint.rpartition(":")
      if not colon or not re.fullmatch(r"[A-Za-z0-9_.-]+", host):
        raise Fail(f"Invalid ZooKeeper endpoint: {endpoint}")
    if not re.fullmatch(r"[0-9]+", port):
      raise Fail(f"Invalid ZooKeeper endpoint port: {endpoint}")
    port_number = int(port)
    if port_number < 1 or port_number > 65535:
      raise Fail(f"Invalid ZooKeeper endpoint port: {endpoint}")
  return connection_string


class ZkMigrator:
  def __init__(self, zk_host, java_exec, java_home, jaas_file, user):
    self.zk_host = validate_connection_string(zk_host)
    self.java_exec = java_exec
    self.java_home = java_home
    self.jaas_file = jaas_file
    self.user = user
    self.zkmigrator_jar = "/var/lib/ambari-agent/tools/zkmigrator.jar"

  def set_acls(self, znode, acl, tries=3):
    znode = validate_znode(znode)
    if not isinstance(acl, str) or not acl or any(
      character.isspace() or ord(character) < 32 for character in acl
    ):
      raise Fail("ZooKeeper ACL must be a non-empty value without whitespace")
    Logger.info(f"Setting ACL on znode {znode} to {acl}")
    Execute(
      self._acl_command(znode, acl),
      user=self.user,
      environment={"JAVA_HOME": self.java_home},
      logoutput=True,
      tries=tries,
      timeout=60,
      timeout_kill_strategy=TerminateStrategy.KILL_PROCESS_GROUP,
    )

  def delete_node(self, znode, tries=3):
    znode = validate_znode(znode)
    Logger.info(f"Removing znode {znode}")
    Execute(
      self._delete_command(znode),
      user=self.user,
      environment={"JAVA_HOME": self.java_home},
      logoutput=True,
      tries=tries,
      timeout=60,
      timeout_kill_strategy=TerminateStrategy.KILL_PROCESS_GROUP,
    )

  def _acl_command(self, znode, acl):
    return (
      self.java_exec,
      f"-Djava.security.auth.login.config={self.jaas_file}",
      "-jar",
      self.zkmigrator_jar,
      "-connection-string",
      self.zk_host,
      "-znode",
      znode,
      "-acl",
      acl,
    )

  def _delete_command(self, znode):
    return (
      self.java_exec,
      f"-Djava.security.auth.login.config={self.jaas_file}",
      "-jar",
      self.zkmigrator_jar,
      "-connection-string",
      self.zk_host,
      "-znode",
      znode,
      "-delete",
    )
