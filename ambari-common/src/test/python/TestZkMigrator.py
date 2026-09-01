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

import unittest
from unittest.mock import patch

from resource_management.core.exceptions import Fail
from resource_management.core.resources.zkmigrator import ZkMigrator
from resource_management.core.signal_utils import TerminateStrategy


class TestZkMigrator(unittest.TestCase):
  def _migrator(self, connection="zk1.example:2181,zk2.example:2181/yarn"):
    return ZkMigrator(
      connection,
      "/usr/bin/java",
      "/usr/lib/jvm/java-17",
      "/etc/hadoop/conf/yarn jaas.conf",
      "yarn",
    )

  def test_acl_command_is_structured_and_bounded(self):
    migrator = self._migrator()
    with patch(
      "resource_management.core.resources.zkmigrator.Execute"
    ) as execute:
      migrator.set_acls("/rmstore;$(id)", "world:anyone:crdwa")

    command = execute.call_args.args[0]
    self.assertIsInstance(command, tuple)
    self.assertIn("/rmstore;$(id)", command)
    self.assertEqual(60, execute.call_args.kwargs["timeout"])
    self.assertEqual(
      TerminateStrategy.KILL_PROCESS_GROUP,
      execute.call_args.kwargs["timeout_kill_strategy"],
    )

  def test_delete_command_preserves_arguments_and_failure_propagation(self):
    migrator = self._migrator()
    with patch(
      "resource_management.core.resources.zkmigrator.Execute",
      side_effect=Fail("migration failed"),
    ) as execute:
      with self.assertRaisesRegex(Fail, "migration failed"):
        migrator.delete_node("/leader-election")
    self.assertEqual("-delete", execute.call_args.args[0][-1])

  def test_root_relative_and_traversal_znodes_are_rejected(self):
    migrator = self._migrator()
    for znode in ("", "/", "relative", "/rmstore/../other", "/a//b"):
      with self.subTest(znode=znode):
        with self.assertRaises(Fail):
          migrator.delete_node(znode)

  def test_invalid_connection_strings_are_rejected(self):
    for connection in (
      "",
      "zk1",
      "zk1:0",
      "zk1:65536",
      "zk1:+2181",
      "zk1:2181,",
      "zk1:2181/../other",
      "zk1:2181;touch /tmp/injected",
    ):
      with self.subTest(connection=connection):
        with self.assertRaises(Fail):
          self._migrator(connection)


if __name__ == "__main__":
  unittest.main()
