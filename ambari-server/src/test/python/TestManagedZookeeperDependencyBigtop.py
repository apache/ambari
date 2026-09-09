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

import importlib.util
import grp
import os
from pathlib import Path
import pwd
import shutil
import stat
from types import SimpleNamespace
import tempfile
import unittest
from unittest.mock import MagicMock, patch

from resource_management.core.signal_utils import TerminateStrategy
from resource_management.libraries.functions.managed_dependency import (
  PROVISION_ZOOKEEPER_NAMESPACE,
  ManagedDependencyCommand,
  ManagedDependencyFailure,
  managed_dependency_request_hash,
  parse_managed_dependency_command,
)


SCRIPT = (
  Path(__file__).resolve().parents[2]
  / "main/resources/stacks/BIGTOP/3.2.0/services/ZOOKEEPER/package/scripts"
  / "managed_zookeeper_dependency.py"
)
SPEC = importlib.util.spec_from_file_location("managed_zookeeper_dependency", SCRIPT)
MODULE = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(MODULE)

BINDING_ID = "59646bd5-39eb-414e-a07e-f5469d369687"
OPERATION_ID = "eeb6a351-b27a-4233-b7ae-86f2a4f5d149"
HASH = "sha256:" + "a" * 64
SNAPSHOT_HASH = "sha256:" + "b" * 64


class TestManagedZookeeperDependencyBigtop(unittest.TestCase):
  def setUp(self):
    self.temporary = tempfile.mkdtemp(prefix="ambari-managed-zookeeper-")
    service = pwd.getpwuid(os.geteuid())
    service_group = grp.getgrgid(os.getegid())
    keytab = os.path.join(self.temporary, "zookeeper.service.keytab")
    client_jaas = os.path.join(self.temporary, "zookeeper_client_jaas.conf")
    with open(keytab, "wb") as stream:
      stream.write(b"test fixture, not a credential")
    os.chmod(keytab, 0o400)
    with open(client_jaas, "w", encoding="utf-8") as stream:
      stream.write(
        "Client { com.sun.security.auth.module.Krb5LoginModule required "
        "useKeyTab=false useTicketCache=true doNotPrompt=true renewTGT=true; };\n"
      )
    os.chmod(client_jaas, 0o640)
    self.params = SimpleNamespace(
      client_port=2181,
      java64_home="/usr/lib/jvm/java",
      java_executable="/usr/lib/jvm/java/bin/java",
      zk_user=service.pw_name,
      user_group=service_group.gr_name,
      tmp_dir=self.temporary,
      security_enabled=True,
      kinit_path_local="/usr/bin/kinit",
      zk_keytab_path=keytab,
      zk_principal="zookeeper/zk1.example.test@EXAMPLE.COM",
      zk_principal_user="zookeeper",
      zk_client_jaas_file=client_jaas,
      zoo_cfg_properties_map={
        "kerberos.removeHostFromPrincipal": "true",
        "kerberos.removeRealmFromPrincipal": "true",
      },
      zookeeper_hosts=("zk1.example.test", "zk2.example.test"),
    )
    self.progress = MagicMock()

  def tearDown(self):
    shutil.rmtree(self.temporary)

  def test_insecure_provider_preparation_uses_exact_nonrecursive_helper(self):
    calls = []

    def call(arguments, **options):
      calls.append((arguments, options))
      return 0, (
        '{"connected":true,"namespaceExists":true,'
        '"parentAclPolicy":"INSECURE_PROVIDER_PREPARED",'
        '"subtreeAclPolicy":"INSECURE_BINDING_SCOPED"}\n'
      )

    command = zookeeper_command()
    facts = MODULE.ManagedZooKeeperNamespaceProvisioner(
      self.params, call
    ).provision(command, self.progress)

    arguments, options = calls[0]
    self.assertEqual(MODULE.HELPER_CLASS, arguments[3])
    self.assertIn("zk1.example.test:2181,zk2.example.test:2181", arguments)
    self.assertNotIn("--delete", arguments)
    self.assertNotIn("--acl", arguments)
    self.assertEqual(self.params.zk_user, options["user"])
    self.assertFalse(options["shell"])
    self.assertEqual(TerminateStrategy.KILL_PROCESS_GROUP,
                     options["timeout_kill_strategy"])
    self.assertEqual("true", facts["namespace.znode.exists"])
    self.progress.checkpoint.assert_called_once_with("zookeeper-namespace-prepared")

  def test_quorum_mismatch_fails_before_helper_execution(self):
    call = MagicMock()
    provisioner = MODULE.ManagedZooKeeperNamespaceProvisioner(self.params, call)
    mismatch = zookeeper_command(expected_quorum="zk1.example.test")
    with self.assertRaisesRegex(ManagedDependencyFailure, "DEPENDENCY_ZOOKEEPER_INVALID"):
      provisioner.provision(mismatch, self.progress)
    call.assert_not_called()

  def test_runner_rejects_non_zookeeper_provider_and_untrusted_helper_output(self):
    foreign = SimpleNamespace(provider_service="HDFS")
    with self.assertRaisesRegex(ManagedDependencyFailure, "DEPENDENCY_COMMAND_INVALID"):
      MODULE._require_zookeeper_provider(foreign)

    provisioner = MODULE.ManagedZooKeeperNamespaceProvisioner(
      self.params, lambda unused_arguments, **unused_options: (0, '{"connected":true}')
    )
    with self.assertRaisesRegex(ManagedDependencyFailure, "DEPENDENCY_PROVIDER_ACTION_FAILED"):
      provisioner.provision(zookeeper_command(), self.progress)

    def timeout(unused_arguments, **unused_options):
      raise TimeoutError("raw command detail must not escape")

    with self.assertRaisesRegex(
      ManagedDependencyFailure, "DEPENDENCY_PROVIDER_ACTION_FAILED"
    ) as failure:
      MODULE.ManagedZooKeeperNamespaceProvisioner(self.params, timeout).provision(
        zookeeper_command(), self.progress
      )
    self.assertNotIn("raw command detail", failure.exception.sanitized_message)

  def test_secure_provider_journals_ledger_then_one_way_container_handoff(self):
    calls = []

    def call(arguments, **options):
      calls.append((arguments, options))
      operation = arguments[arguments.index("--operation") + 1]
      if operation == "PREPARE_SECURE_LEDGER":
        return 0, '{"connected":true,"ledgerPrepared":true}\n'
      return 0, (
        '{"connected":true,"containerCreated":true,"nodeExists":false}\n'
      )

    progress = MagicMock()
    progress.completed_steps = ()
    with patch.object(MODULE, "PrivateKerberosCache", FakeKerberosCache):
      facts = MODULE.ManagedZooKeeperNamespaceProvisioner(
        self.params, call
      ).provision(zookeeper_command(security_mode="KERBEROS"), progress)

    self.assertEqual(
      ["PREPARE_SECURE_LEDGER", "CREATE_SECURE_CONTAINER"],
      [arguments[arguments.index("--operation") + 1] for arguments, _ in calls],
    )
    self.assertEqual(
      [
        "secure-ledger-prepared",
        "secure-container-create-dispatched",
        "secure-container-create-acknowledged",
      ],
      [item.args[0] for item in progress.checkpoint.call_args_list],
    )
    self.assertEqual("true", facts["provider.handoff.acknowledged"])
    self.assertTrue(all(options["user"] == self.params.zk_user for _, options in calls))
    self.assertTrue(all("KRB5CCNAME" in options["environment"] for _, options in calls))

  def test_lost_secure_create_response_requires_consumer_reconciliation(self):
    def call(arguments, **unused_options):
      operation = arguments[arguments.index("--operation") + 1]
      if operation == "PREPARE_SECURE_LEDGER":
        return 0, '{"connected":true,"ledgerPrepared":true}\n'
      return 0, (
        '{"connected":true,"containerCreated":false,"nodeExists":true}\n'
      )

    progress = MagicMock()
    progress.completed_steps = ("secure-container-create-dispatched",)
    with patch.object(MODULE, "PrivateKerberosCache", FakeKerberosCache):
      with self.assertRaisesRegex(
        ManagedDependencyFailure,
        "DEPENDENCY_ZOOKEEPER_HANDOFF_RECONCILIATION_REQUIRED",
      ) as failure:
        MODULE.ManagedZooKeeperNamespaceProvisioner(self.params, call).provision(
          zookeeper_command(security_mode="KERBEROS"), progress
        )
    self.assertEqual(
      "true", failure.exception.facts["provider.handoff.reconciliation.required"]
    )

  def test_credential_metadata_accepts_non_root_service_ownership_only(self):
    service_uid = 12345 if os.geteuid() != 12345 else 12346
    service_gid = 23456
    keytab = SimpleNamespace(
      st_mode=stat.S_IFREG | 0o400, st_uid=service_uid, st_gid=service_gid
    )
    jaas = SimpleNamespace(
      st_mode=stat.S_IFREG | 0o640, st_uid=service_uid, st_gid=service_gid
    )

    self.assertTrue(
      MODULE._credential_metadata_matches(keytab, service_uid, service_gid, 0o400)
    )
    self.assertTrue(
      MODULE._credential_metadata_matches(jaas, service_uid, service_gid, 0o640)
    )
    self.assertFalse(
      MODULE._credential_metadata_matches(keytab, os.geteuid(), service_gid, 0o400)
    )
    self.assertFalse(
      MODULE._credential_metadata_matches(jaas, service_uid, service_gid, 0o600)
    )

  def test_provider_inspector_requires_service_owned_keytab_and_jaas(self):
    service_uid = 12345 if os.geteuid() != 12345 else 12346
    service_gid = 23456
    service = SimpleNamespace(pw_uid=service_uid)
    group = SimpleNamespace(gr_gid=service_gid)
    keytab = SimpleNamespace(
      st_mode=stat.S_IFREG | 0o400, st_uid=service_uid, st_gid=service_gid
    )
    jaas = SimpleNamespace(
      st_mode=stat.S_IFREG | 0o640, st_uid=service_uid, st_gid=service_gid
    )

    def metadata(path, **unused):
      return keytab if path == self.params.zk_keytab_path else jaas

    provisioner = MODULE.ManagedZooKeeperNamespaceProvisioner(self.params)
    with patch.object(MODULE.pwd, "getpwnam", return_value=service), patch.object(
      MODULE.grp, "getgrnam", return_value=group
    ), patch.object(MODULE.os, "stat", side_effect=metadata):
      provisioner._validate_provider_credential_material()

    runner_owned_keytab = SimpleNamespace(
      st_mode=stat.S_IFREG | 0o400,
      st_uid=os.geteuid(),
      st_gid=service_gid,
    )

    def unsafe_metadata(path, **unused):
      return runner_owned_keytab if path == self.params.zk_keytab_path else jaas

    with patch.object(MODULE.pwd, "getpwnam", return_value=service), patch.object(
      MODULE.grp, "getgrnam", return_value=group
    ), patch.object(MODULE.os, "stat", side_effect=unsafe_metadata):
      with self.assertRaisesRegex(
        ManagedDependencyFailure, "DEPENDENCY_ZOOKEEPER_AUTHORIZATION_FAILED"
      ):
        provisioner._validate_provider_credential_material()


def zookeeper_command(
  expected_quorum="zk1.example.test,zk2.example.test",
  security_mode="INSECURE",
  provider_service="ZOOKEEPER",
):
  parameters = {
    "expected.client.port": "2181",
    "expected.quorum": expected_quorum,
    "namespace.container.znode": "/ambari-managed-hbase/" + BINDING_ID,
    "namespace.parent.znode": "/ambari-managed-hbase",
    "namespace.znode": "/ambari-managed-hbase/" + BINDING_ID + "/hbase",
    "owner.user": "hbase_mc_c1",
    "parent.acl.policy": (
      "PROVIDER_ADMIN_CREATE_DELETE"
      if security_mode == "KERBEROS"
      else "INSECURE_PROVIDER_PREPARED"
    ),
    "provider.action.host.id": "21",
    "provider.cluster.id": "2",
    "provider.fingerprint": HASH,
    "provider.service": provider_service,
    "security.mode": security_mode,
    "snapshot.fingerprint": SNAPSHOT_HASH,
    "subtree.acl.policy": (
      "SASL_CONSUMER_PRIVATE"
      if security_mode == "KERBEROS"
      else "INSECURE_BINDING_SCOPED"
    ),
  }
  if security_mode == "KERBEROS":
    parameters["consumer.zk.sasl.id"] = "hbase_mc_c1"
    parameters["namespace.ledger.znode"] = (
      "/ambari-managed-hbase/.bindings/" + BINDING_ID
    )
  envelope = {
    "bindingId": BINDING_ID,
    "epoch": 1,
    "immutableRequestHash": "sha256:" + "0" * 64,
    "operationId": OPERATION_ID,
    "protocolVersion": 1,
    "snapshotVersion": 1,
  }
  raw = ManagedDependencyCommand(
    PROVISION_ZOOKEEPER_NAMESPACE, envelope, parameters
  )
  envelope["immutableRequestHash"] = managed_dependency_request_hash(raw)
  return parse_managed_dependency_command(
    {
      "name": PROVISION_ZOOKEEPER_NAMESPACE,
      "envelope": envelope,
      "parameters": parameters,
    },
    PROVISION_ZOOKEEPER_NAMESPACE,
  )


class FakeKerberosCache:
  def __init__(self, *unused_args, **unused_kwargs):
    self.environment = {"KRB5CCNAME": "FILE:/private/cache"}

  def __enter__(self):
    return self

  def __exit__(self, *unused_args):
    return False

  def kinit(self, *unused_args, **unused_kwargs):
    return None

  def merge_environment(self, environment):
    return {**environment, **self.environment}


if __name__ == "__main__":
  unittest.main()
