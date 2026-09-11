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

from contextlib import contextmanager
import json
import importlib.util
from pathlib import Path
import sys
from types import ModuleType, SimpleNamespace
import unittest
from unittest.mock import MagicMock, patch

from resource_management.core.signal_utils import TerminateStrategy
from resource_management.libraries.functions.managed_dependency import (
  PROVISION_HDFS_NAMESPACE,
  ManagedDependencyCommand,
  ManagedDependencyFailure,
  managed_dependency_request_hash,
  parse_managed_dependency_command,
)


HDFS = (
  Path(__file__).resolve().parents[2]
  / "main/resources/stacks/BIGTOP/3.2.0/services/HDFS"
)
SCRIPTS = HDFS / "package/scripts"
BINDING_ID = "59646bd5-39eb-414e-a07e-f5469d369687"
OPERATION_ID = "eeb6a351-b27a-4233-b7ae-86f2a4f5d149"
UPDATE_OPERATION_ID = "33149921-9b0f-493f-8a95-ff19029f13bf"
PROVIDER_HASH = "sha256:540491df8aa26c85b4082bec6030c2ccaba50950c79f5dfc12208e1addb1d155"
SNAPSHOT_HASH = "sha256:2bff3497f87dd9a5f2cf9049c3dba3f4caa589e49ecfead1fff799790853fc15"
AUTHORITY = "hdfs://nn.example.test:8020"
BASE = f"{AUTHORITY}/apps/ambari-managed/hbase/{BINDING_ID}"


def load_script(module_name, filename, dependencies=None):
  spec = importlib.util.spec_from_file_location(module_name, SCRIPTS / filename)
  module = importlib.util.module_from_spec(spec)
  with patch.dict(sys.modules, dependencies or {}):
    spec.loader.exec_module(module)
  return module


@contextmanager
def insecure_hdfs_environment(unused_params, unused_prefix, **unused_credentials):
  yield None


HDFS_KERBEROS = ModuleType("hdfs_kerberos")
HDFS_KERBEROS.hdfs_kerberos_environment = insecure_hdfs_environment
MANAGED_HDFS = load_script(
  "bigtop_managed_hdfs_dependency",
  "managed_hdfs_dependency.py",
  {"hdfs_kerberos": HDFS_KERBEROS},
)


class TestManagedHdfsDependencyBigtop(unittest.TestCase):
  def setUp(self):
    self.params = SimpleNamespace(
      hadoop_bin_dir="/usr/bin",
      hadoop_conf_dir="/etc/hadoop/conf",
      hdfs_principal_name=None,
      hdfs_site={"dfs.permissions.enabled": "true"},
      hdfs_user="hdfs",
      hdfs_user_keytab=None,
      security_enabled=False,
      tmp_dir="/tmp",
      user_group="hadoop",
    )
    self.hdfs = FakeHdfs()
    self.progress = MagicMock()

  def test_first_provision_and_exact_retry_use_provider_marker(self):
    command = hdfs_command()
    provisioner = MANAGED_HDFS.ManagedHdfsNamespaceProvisioner(
      self.params, self.hdfs
    )
    with self.subTest("structured provider observations"):
      facts = provisioner.provision(command, self.progress)
      mutations_after_first_attempt = tuple(self.hdfs.mutations)
      retry_facts = provisioner.provision(command, MagicMock())

    self.assertEqual(facts, retry_facts)
    self.assertEqual(mutations_after_first_attempt, tuple(self.hdfs.mutations))
    self.assertEqual("true", facts["namespace.root.exists"])
    self.assertEqual(
      ("directory", "hbase_mc_a1", "hbase_mc_a1", "0700"),
      self.hdfs.nodes[BASE + "/root"],
    )
    marker_path = (
      f"{AUTHORITY}/apps/ambari-managed/.bindings/{BINDING_ID}.json"
    )
    self.assertIn('"bindingId":"' + BINDING_ID + '"', self.hdfs.contents[marker_path])
    for unused_command, options in self.hdfs.calls:
      self.assertEqual("hdfs", options["user"])
      self.assertFalse(options["shell"])
      self.assertEqual(120, options["timeout"])
      self.assertEqual(
        TerminateStrategy.KILL_PROCESS_GROUP,
        options["timeout_kill_strategy"],
      )

  def test_unmarked_existing_binding_path_is_not_adopted(self):
    self.hdfs.nodes[BASE] = ("directory", "foreign", "foreign", "0755")
    command = hdfs_command()
    provisioner = MANAGED_HDFS.ManagedHdfsNamespaceProvisioner(
      self.params, self.hdfs
    )

    with self.assertRaisesRegex(
      ManagedDependencyFailure, "DEPENDENCY_NAMESPACE_CONFLICT"
    ):
      provisioner.provision(command, self.progress)

    self.assertEqual(
      ("directory", "foreign", "foreign", "0755"), self.hdfs.nodes[BASE]
    )
    self.assertFalse(
      any(
        arguments[0] == "SET_OWNER" and arguments[-1] == BASE
        for arguments in self.hdfs.mutations
      )
    )

  def test_fresh_hdfs_creates_the_missing_provider_owned_apps_parent(self):
    del self.hdfs.nodes[AUTHORITY + "/apps"]
    provisioner = MANAGED_HDFS.ManagedHdfsNamespaceProvisioner(self.params, self.hdfs)
    with self.subTest("structured provider observations"):
      provisioner.provision(hdfs_command(), self.progress)
    self.assertEqual(("directory", "hdfs", "hadoop", "0755"), self.hdfs.nodes[AUTHORITY + "/apps"])

  def test_snapshot_and_provider_fingerprint_updates_reuse_immutable_namespace(self):
    first = hdfs_command()
    provisioner = MANAGED_HDFS.ManagedHdfsNamespaceProvisioner(
      self.params, self.hdfs
    )
    with self.subTest("structured provider observations"):
      provisioner.provision(first, self.progress)
      mutations_after_first_attempt = tuple(self.hdfs.mutations)
      updated = hdfs_command(
        provider_hash="sha256:" + "a" * 64,
        snapshot_hash="sha256:" + "b" * 64,
        operation_id=UPDATE_OPERATION_ID,
        epoch=2,
        snapshot_version=2,
      )
      facts = provisioner.provision(updated, MagicMock())

    self.assertEqual("sha256:" + "b" * 64, facts["applied.snapshot.fingerprint"])
    self.assertEqual(mutations_after_first_attempt, tuple(self.hdfs.mutations))

  def test_named_or_default_acl_is_rejected_without_repair(self):
    command = hdfs_command(directory_mode="0750", owner_group="hbase_mc_group")
    provisioner = MANAGED_HDFS.ManagedHdfsNamespaceProvisioner(
      self.params, self.hdfs
    )
    with self.subTest("structured provider observations"):
      provisioner.provision(command, self.progress)
    self.hdfs.acls[BASE + "/wal"] = {
      "user::rwx",
      "user:other:r-x",
      "group::r-x",
      "mask::r-x",
      "other::---",
    }
    mutations_before_retry = tuple(self.hdfs.mutations)

    with self.assertRaisesRegex(
      ManagedDependencyFailure, "DEPENDENCY_NAMESPACE_CONFLICT"
    ):
      provisioner.provision(command, MagicMock())

    self.assertEqual(mutations_before_retry, tuple(self.hdfs.mutations))

  def test_parent_default_acl_and_disabled_permissions_fail_before_creation(self):
    provisioner = MANAGED_HDFS.ManagedHdfsNamespaceProvisioner(
      self.params, self.hdfs
    )
    apps = AUTHORITY + "/apps"
    self.hdfs.acls[apps] = acl_for_mode("0755") | {
      "default:user::rwx",
      "default:group::r-x",
      "default:other::---",
    }

    with self.assertRaisesRegex(
      ManagedDependencyFailure, "DEPENDENCY_NAMESPACE_CONFLICT"
    ):
      provisioner.provision(hdfs_command(), self.progress)
    self.assertNotIn(AUTHORITY + "/apps/ambari-managed", self.hdfs.nodes)
    self.params.hdfs_site["dfs.permissions.enabled"] = "false"
    self.hdfs.acls.pop(apps)
    with self.assertRaisesRegex(
      ManagedDependencyFailure, "DEPENDENCY_CLIENT_FEATURE_UNSUPPORTED"
    ):
      provisioner.provision(hdfs_command(), self.progress)

  def test_unsafe_ancestor_owner_mode_or_acl_blocks_all_managed_mutations(self):
    cases = (
      (
        AUTHORITY + "/apps",
        ("directory", "foreign", "hadoop", "0755"),
        None,
      ),
      (
        AUTHORITY + "/apps",
        ("directory", "hdfs", "hadoop", "0777"),
        None,
      ),
      (
        AUTHORITY + "/",
        ("directory", "hdfs", "hadoop", "0755"),
        {
          "user::rwx",
          "user:foreign:rwx",
          "group::r-x",
          "mask::rwx",
          "other::r-x",
        },
      ),
    )
    for path, metadata, acl in cases:
      hdfs = FakeHdfs()
      hdfs.nodes[path] = metadata
      if acl is not None:
        hdfs.acls[path] = acl
      provisioner = MANAGED_HDFS.ManagedHdfsNamespaceProvisioner(
        self.params, hdfs
      )
      with self.subTest(path=path, metadata=metadata), self.assertRaisesRegex(
        ManagedDependencyFailure, "DEPENDENCY_NAMESPACE_CONFLICT"
      ):
        provisioner.provision(hdfs_command(), MagicMock())
      self.assertEqual([], hdfs.mutations)

  def test_hdfs_runner_rejects_generic_command_for_zookeeper_provider(self):
    command = generic_provider_command("ZOOKEEPER")

    with self.assertRaisesRegex(
      ManagedDependencyFailure, "DEPENDENCY_COMMAND_INVALID"
    ):
      MANAGED_HDFS._require_hdfs_provider(command)

  def test_provider_command_timeout_is_sanitized(self):
    def timeout(unused_arguments, **unused_options):
      raise TimeoutError("raw provider command detail must not escape")

    provisioner = MANAGED_HDFS.ManagedHdfsNamespaceProvisioner(
      self.params, timeout
    )
    with self.assertRaisesRegex(
      ManagedDependencyFailure, "DEPENDENCY_PROVIDER_ACTION_FAILED"
    ) as failure:
      provisioner.provision(hdfs_command(), self.progress)
    self.assertNotIn("raw provider command detail", failure.exception.sanitized_message)

  def test_empty_mkdir_interruption_recovers_but_nonempty_partial_path_fails(self):
    command = hdfs_command()
    provisioner = MANAGED_HDFS.ManagedHdfsNamespaceProvisioner(
      self.params, self.hdfs
    )
    self.hdfs.fail_chown_once = BASE
    with self.subTest("structured provider observations"):
      with self.assertRaisesRegex(
        ManagedDependencyFailure, "DEPENDENCY_PROVIDER_ACTION_FAILED"
      ):
        provisioner.provision(command, self.progress)
      self.assertEqual("hdfs", self.hdfs.nodes[BASE][1])
      provisioner.provision(command, MagicMock())

    self.assertEqual("hbase_mc_a1", self.hdfs.nodes[BASE][1])
    self.hdfs.nodes[BASE + "/wal"] = (
      "directory",
      "hdfs",
      "hadoop",
      "0700",
    )
    self.hdfs.counts[BASE + "/wal"] = (1, 1, 20)
    with self.assertRaisesRegex(
      ManagedDependencyFailure, "DEPENDENCY_NAMESPACE_CONFLICT"
    ):
      provisioner.provision(command, MagicMock())


class FakeHdfs:
  def __init__(self):
    self.calls = []
    self.contents = {}
    self.acls = {}
    self.counts = {}
    self.fail_chown_once = None
    self.mutations = []
    self.nodes = {}
    self.nodes[AUTHORITY + "/"] = ("directory", "hdfs", "hadoop", "0755")
    self.nodes[AUTHORITY + "/apps"] = ("directory", "hdfs", "hadoop", "0755")

  def __call__(self, command, **options):
    self.calls.append((command, options))
    operation, identity, payload = command[-4], json.loads(command[-2]), json.loads(command[-1])
    path = payload["path"]
    response = {"schemaVersion": 1, "identity": identity, "operation": operation, "status": "SUCCEEDED"}
    if operation == "STAT":
      observed = self.nodes.get(path)
      result = {"exists": False} if observed is None else {
        "exists": True, "type": "DIRECTORY" if observed[0] == "directory" else "FILE",
        "owner": observed[1], "group": observed[2], "mode": int(observed[3], 8)}
    elif operation == "READ_MARKER":
      result = {"marker": json.loads(self.contents[path])}
    elif operation == "COUNT":
      result = {"empty": self.counts.get(path, (1, 0, 0)) == (1, 0, 0)}
    elif operation == "ACL":
      result = {"entries": []}
      entries = self.acls.get(path, acl_for_mode(self.nodes[path][3]))
      for entry in entries - acl_for_mode(self.nodes[path][3]):
        parts = entry.split(":")
        default = parts[0] == "default"
        if default:
          parts = parts[1:]
        result["entries"].append({"scope": "DEFAULT" if default else "ACCESS",
          "type": parts[0].upper(), "name": parts[1], "permission": 7})
    else:
      self.mutations.append((operation, path))
      if operation == "MKDIR":
        self.nodes[path] = ("directory", "hdfs", "hadoop", format(payload["mode"], "04o"))
        result = {"created": True}
      elif operation == "SET_OWNER":
        if self.fail_chown_once == path:
          self.fail_chown_once = None
          response.update(status="FAILED", errorCode="DEPENDENCY_CLIENT_OBSERVATION_FAILED")
          return 0, json.dumps(response), "diagnostic noise"
        file_type, _, _, mode = self.nodes[path]
        self.nodes[path] = (file_type, payload["owner"], payload["group"], mode)
        result = {"applied": True}
      elif operation == "SET_PERMISSION":
        file_type, owner, group, _ = self.nodes[path]
        self.nodes[path] = (file_type, owner, group, format(payload["mode"], "04o"))
        result = {"applied": True}
      elif operation == "WRITE_MARKER":
        self.nodes[path] = ("regular file", "hdfs", "hadoop", "0600")
        self.contents[path] = json.dumps(payload["marker"], separators=(",", ":"))
        result = {"written": True}
      else:
        raise AssertionError("Unexpected structured operation: " + operation)
    response["result"] = result
    return 0, json.dumps(response), "diagnostic noise"


def hdfs_command(
  provider_hash=PROVIDER_HASH,
  snapshot_hash=SNAPSHOT_HASH,
  directory_mode="0700",
  owner_group="hbase_mc_a1",
  operation_id=OPERATION_ID,
  epoch=1,
  snapshot_version=1,
):
  parameters = {
    "directory.mode": directory_mode,
    "namespace.root.uri": BASE + "/root",
    "namespace.wal.uri": BASE + "/wal",
    "owner.group": owner_group,
    "owner.user": "hbase_mc_a1",
    "provider.action.host.id": "21",
    "provider.cluster.id": "2",
    "provider.fingerprint": provider_hash,
    "provider.service": "HDFS",
    "security.mode": "INSECURE",
    "snapshot.fingerprint": snapshot_hash,
  }
  envelope = {
    "bindingId": BINDING_ID,
    "epoch": epoch,
    "immutableRequestHash": "sha256:" + "0" * 64,
    "operationId": operation_id,
    "protocolVersion": 1,
    "snapshotVersion": snapshot_version,
  }
  unhashed = ManagedDependencyCommand(PROVISION_HDFS_NAMESPACE, envelope, parameters)
  envelope["immutableRequestHash"] = managed_dependency_request_hash(unhashed)
  return parse_managed_dependency_command(
    {
      "name": PROVISION_HDFS_NAMESPACE,
      "envelope": envelope,
      "parameters": parameters,
    },
    PROVISION_HDFS_NAMESPACE,
  )


def acl_for_mode(mode):
  return {
    "user::" + permission_triplet(mode[1]),
    "group::" + permission_triplet(mode[2]),
    "other::" + permission_triplet(mode[3]),
  }


def generic_provider_command(provider_service):
  parameters = {
    "provider.action.host.id": "21",
    "provider.cluster.id": "2",
    "provider.fingerprint": PROVIDER_HASH,
    "provider.service": provider_service,
    "security.mode": "INSECURE",
    "snapshot.fingerprint": SNAPSHOT_HASH,
  }
  envelope = {
    "bindingId": BINDING_ID,
    "epoch": 1,
    "immutableRequestHash": "sha256:" + "0" * 64,
    "operationId": OPERATION_ID,
    "protocolVersion": 1,
    "snapshotVersion": 1,
  }
  unhashed = ManagedDependencyCommand("PREPARE_BINDING_JOURNAL", envelope, parameters)
  envelope["immutableRequestHash"] = managed_dependency_request_hash(unhashed)
  return parse_managed_dependency_command(
    {
      "name": "PREPARE_BINDING_JOURNAL",
      "envelope": envelope,
      "parameters": parameters,
    },
    "PREPARE_BINDING_JOURNAL",
  )


def permission_triplet(digit):
  value = int(digit, 8)
  return "".join(
    character if value & bit else "-"
    for character, bit in (("r", 4), ("w", 2), ("x", 1))
  )


if __name__ == "__main__":
  unittest.main()
