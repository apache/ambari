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
import multiprocessing
import os
import shutil
import stat
import tempfile
import unittest
from copy import deepcopy
from unittest.mock import patch

from resource_management.libraries.functions import managed_dependency as dependency_module
from resource_management.libraries.functions.managed_dependency import (
  HDFS_BASE,
  INITIALIZE_BINDING_JOURNAL,
  INVALIDATE_BINDING_EPOCH,
  PREPARE_BINDING_JOURNAL,
  PROVISION_HDFS_NAMESPACE,
  PROVISION_ZOOKEEPER_NAMESPACE,
  PREPARE_HDFS_CONSUMER,
  PREPARE_ZOOKEEPER_CONSUMER,
  VERIFY_HDFS_CONSUMER,
  ManagedDependencyCommand,
  ManagedDependencyFailure,
  ManagedDependencyJournal,
  client_config_fingerprint,
  managed_dependency_bundle_hash,
  managed_dependency_request_hash,
  parse_managed_dependency_bundle,
  parse_managed_dependency_command,
)


BINDING_ID = "59646bd5-39eb-414e-a07e-f5469d369687"
PREPARE_OPERATION_ID = "43d3dad3-d0b0-4ba2-a9ae-2ccf7ab758f2"
INITIALIZE_OPERATION_ID = "cb5b2b96-ed50-47fb-aad0-628835ad72b5"
PROVISION_OPERATION_ID = "eeb6a351-b27a-4233-b7ae-86f2a4f5d149"
AUTHORIZATION_ID = "3db076d6-ff66-4460-90b5-5b7d2f2b81af"
INVALIDATE_OPERATION_ID = "dc023422-627b-452f-a0e3-ae536dcf42b4"
IDENTITY_HASH = "sha256:427bca797fe223f7a612abe0292533df9a97eda718746863a056e186736846bb"
PROVIDER_HASH = "sha256:540491df8aa26c85b4082bec6030c2ccaba50950c79f5dfc12208e1addb1d155"
SNAPSHOT_HASH = "sha256:2bff3497f87dd9a5f2cf9049c3dba3f4caa589e49ecfead1fff799790853fc15"


class TestManagedDependency(unittest.TestCase):
  def setUp(self):
    self.temporary_root = tempfile.mkdtemp(prefix="ambari-managed-dependency-")
    self.state_root = os.path.join(self.temporary_root, "journal")

  def tearDown(self):
    shutil.rmtree(self.temporary_root)

  def test_java_compatible_hash_uses_utf16_string_lengths(self):
    payload = command_payload(
      VERIFY_HDFS_CONSUMER,
      {
        "client.config.json": hdfs_client_config(),
        "client.config.fingerprint": hdfs_client_config_fingerprint(),
        "client.package.name": "hadoop_3_3_0_0_1-client",
        "client.package.version": "3.3.0-\u7248\u672c-\U0001f600",
        "client.software.kind": "HADOOP_CLIENT",
        "client.software.semantic.version": "3.3.0",
        "consumer.user": "hbase_mc_c1",
        "expected.default.fs": "hdfs://nn.example.test:8020",
        "expected.directory.mode": "0700",
        "expected.namespace.root.uri": hdfs_uri("root"),
        "expected.namespace.wal.uri": hdfs_uri("wal"),
        "expected.owner.group": "hbase_mc_c1",
        "host.id": "42",
        "identity.fingerprint": IDENTITY_HASH,
        "preparation.observation.fingerprint":
          "sha256:7dc1dd44b1a75f745d6a32ca91f69159096f314ca842d43c6371d27eb467b67e",
        "preparation.observation.id": "4be3c05a-b27c-44c7-a430-a0ff6bfddb28",
        "preparation.request.hash":
          "sha256:1bbb4aa3387295797ad351ea845e8864be93077934cc5a7d3e2e44c74d02ba45",
      },
      operation_id=PREPARE_OPERATION_ID,
      epoch=3,
    )

    self.assertEqual(
      "sha256:8dceee0e7453229fce1ded1bd641a6c6809995b9c87f648f120632eaafe317ca",
      payload["envelope"]["immutableRequestHash"],
    )
    parse_managed_dependency_command(payload, VERIFY_HDFS_CONSUMER)

  def test_bundle_hash_binds_exact_host_identity_and_prepare_command(self):
    payload = command_payload(
      PREPARE_HDFS_CONSUMER,
      hdfs_prepare_parameters(),
      operation_id=PREPARE_OPERATION_ID,
      epoch=3,
    )
    command = parse_managed_dependency_command(payload, PREPARE_HDFS_CONSUMER)
    bundle = {
      "schemaVersion": 1,
      "hostId": 42,
      "consumerUser": "hbase_mc_c1",
      "identityFingerprint": IDENTITY_HASH,
      "commands": [payload],
      "immutableBundleHash": managed_dependency_bundle_hash(
        1, 42, "hbase_mc_c1", IDENTITY_HASH, [command]
      ),
    }

    parsed = parse_managed_dependency_bundle(bundle)

    self.assertEqual(command.request_hash, parsed.commands[0].request_hash)
    bundle["hostId"] = 43
    with self.assertRaisesRegex(ManagedDependencyFailure, "DEPENDENCY_COMMAND_INVALID"):
      parse_managed_dependency_bundle(bundle)

    verify = command_payload(
      VERIFY_HDFS_CONSUMER,
      hdfs_verify_parameters(),
      operation_id=PREPARE_OPERATION_ID,
      epoch=3,
    )
    bundle["hostId"] = 42
    bundle["commands"] = [verify]
    verified = parse_managed_dependency_command(verify, VERIFY_HDFS_CONSUMER)
    bundle["immutableBundleHash"] = managed_dependency_bundle_hash(
      1, 42, "hbase_mc_c1", IDENTITY_HASH, [verified]
    )
    with self.assertRaisesRegex(ManagedDependencyFailure, "DEPENDENCY_COMMAND_INVALID"):
      parse_managed_dependency_bundle(bundle)

  def test_schema_two_subset_is_required_canonical_and_hash_bound(self):
    payload = command_payload(
      PREPARE_HDFS_CONSUMER,
      hdfs_prepare_parameters(),
      operation_id=PREPARE_OPERATION_ID,
      epoch=3,
    )
    command = parse_managed_dependency_command(payload, PREPARE_HDFS_CONSUMER)
    bundle = {
      "schemaVersion": 2,
      "hostId": 42,
      "consumerUser": "hbase_mc_c1",
      "identityFingerprint": IDENTITY_HASH,
      "commands": [payload],
      "preparationBindingIds": [BINDING_ID],
      "immutableBundleHash": managed_dependency_bundle_hash(
        2, 42, "hbase_mc_c1", IDENTITY_HASH, [command], [BINDING_ID]
      ),
    }

    parsed = parse_managed_dependency_bundle(bundle)
    self.assertEqual((BINDING_ID,), parsed.preparation_binding_ids)
    self.assertEqual(
      (BINDING_ID,), tuple(item.binding_id for item in parsed.preparation_commands)
    )
    self.assertIs(parsed.commands[0], parsed.preparation_commands[0])

    missing = deepcopy(bundle)
    del missing["preparationBindingIds"]
    with self.assertRaisesRegex(ManagedDependencyFailure, "DEPENDENCY_COMMAND_INVALID"):
      parse_managed_dependency_bundle(missing)

    unknown = deepcopy(bundle)
    unknown["unexpected"] = True
    with self.assertRaisesRegex(ManagedDependencyFailure, "DEPENDENCY_COMMAND_INVALID"):
      parse_managed_dependency_bundle(unknown)

    duplicate = deepcopy(bundle)
    duplicate["preparationBindingIds"] = [BINDING_ID, BINDING_ID]
    with self.assertRaisesRegex(ManagedDependencyFailure, "DEPENDENCY_COMMAND_INVALID"):
      parse_managed_dependency_bundle(duplicate)

    non_subset = deepcopy(bundle)
    non_subset["preparationBindingIds"] = ["f0000000-0000-4000-8000-000000000002"]
    non_subset["immutableBundleHash"] = managed_dependency_bundle_hash(
      2, 42, "hbase_mc_c1", IDENTITY_HASH, [command],
      non_subset["preparationBindingIds"],
    )
    with self.assertRaisesRegex(ManagedDependencyFailure, "DEPENDENCY_COMMAND_INVALID"):
      parse_managed_dependency_bundle(non_subset)

    tampered = deepcopy(bundle)
    tampered["immutableBundleHash"] = "sha256:" + "0" * 64
    with self.assertRaisesRegex(ManagedDependencyFailure, "DEPENDENCY_COMMAND_INVALID"):
      parse_managed_dependency_bundle(tampered)

    wrong_host = deepcopy(bundle)
    wrong_host["commands"][0]["parameters"]["host.id"] = "43"
    rehash(wrong_host["commands"][0])
    with self.assertRaisesRegex(ManagedDependencyFailure, "DEPENDENCY_COMMAND_INVALID"):
      parse_managed_dependency_bundle(wrong_host)

    wrong_identity = deepcopy(bundle)
    wrong_identity["commands"][0]["parameters"]["consumer.user"] = "other_user"
    rehash(wrong_identity["commands"][0])
    with self.assertRaisesRegex(ManagedDependencyFailure, "DEPENDENCY_COMMAND_INVALID"):
      parse_managed_dependency_bundle(wrong_identity)

  def test_schema_two_hash_vector_uses_canonical_string_uuid_ordering(self):
    low_bit = ManagedDependencyCommand(
      PREPARE_HDFS_CONSUMER,
      {
        "bindingId": "10000000-0000-4000-8000-000000000001",
        "operationId": PREPARE_OPERATION_ID,
        "epoch": 3,
        "snapshotVersion": 7,
        "immutableRequestHash": "sha256:1f9d4536627f840954773f5f6a5c4c446d6e092d241bc11b1dec0ae6a3c029f9",
      },
      {"provider.service": "HDFS"},
    )
    high_bit = ManagedDependencyCommand(
      PREPARE_ZOOKEEPER_CONSUMER,
      {
        "bindingId": "f0000000-0000-4000-8000-000000000002",
        "operationId": "f0000000-0000-4000-8000-000000000003",
        "epoch": 4,
        "snapshotVersion": 7,
        "immutableRequestHash": "sha256:2c7af9e4ed49692ed09859ab822bfb0f5d0f0e3080ebb608dc6635920f5417b0",
      },
      {"provider.service": "ZOOKEEPER"},
    )

    self.assertEqual(
      "sha256:1cdc7d2691963e519c4353d3d920b1cf2b7680f07e5239c5535388d99a4fa6a6",
      managed_dependency_bundle_hash(
        2,
        42,
        "hbase_mc_c1",
        IDENTITY_HASH,
        [high_bit, low_bit],
        [high_bit.binding_id, low_bit.binding_id],
      ),
    )
    self.assertEqual(
      "sha256:1cdc7d2691963e519c4353d3d920b1cf2b7680f07e5239c5535388d99a4fa6a6",
      managed_dependency_bundle_hash(
        2, 42, "hbase_mc_c1", IDENTITY_HASH, [high_bit, low_bit]
      ),
    )

  def test_client_software_and_observation_lineage_are_strictly_typed(self):
    payload = command_payload(
      VERIFY_HDFS_CONSUMER,
      hdfs_verify_parameters(),
      operation_id=PREPARE_OPERATION_ID,
      epoch=3,
    )
    payload["parameters"]["client.software.semantic.version"] = "3"
    rehash(payload)
    with self.assertRaisesRegex(ManagedDependencyFailure, "DEPENDENCY_COMMAND_INVALID"):
      parse_managed_dependency_command(payload, VERIFY_HDFS_CONSUMER)

    payload = command_payload(
      VERIFY_HDFS_CONSUMER,
      hdfs_verify_parameters(),
      operation_id=PREPARE_OPERATION_ID,
      epoch=3,
    )
    payload["parameters"]["preparation.observation.id"] = "not-a-uuid"
    rehash(payload)
    with self.assertRaisesRegex(ManagedDependencyFailure, "DEPENDENCY_COMMAND_INVALID"):
      parse_managed_dependency_command(payload, VERIFY_HDFS_CONSUMER)

    preparation = command_payload(
      PREPARE_HDFS_CONSUMER,
      hdfs_prepare_parameters(),
      operation_id=PREPARE_OPERATION_ID,
      epoch=3,
    )
    preparation["parameters"]["client.package.version"] = "untrusted-echo"
    rehash(preparation)
    with self.assertRaisesRegex(ManagedDependencyFailure, "DEPENDENCY_COMMAND_INVALID"):
      parse_managed_dependency_command(preparation, PREPARE_HDFS_CONSUMER)

  def test_traversal_and_unknown_fields_fail_even_with_recomputed_hash(self):
    payload = command_payload(
      PROVISION_HDFS_NAMESPACE,
      hdfs_provision_parameters(),
      operation_id=PROVISION_OPERATION_ID,
    )
    payload["parameters"]["namespace.root.uri"] = (
      f"hdfs://nn.example.test:8020{HDFS_BASE}/{BINDING_ID}/../../foreign/root"
    )
    rehash(payload)
    with self.assertRaisesRegex(ManagedDependencyFailure, "DEPENDENCY_COMMAND_INVALID"):
      parse_managed_dependency_command(payload, PROVISION_HDFS_NAMESPACE)

    payload = command_payload(
      PROVISION_HDFS_NAMESPACE,
      hdfs_provision_parameters(),
      operation_id=PROVISION_OPERATION_ID,
    )
    payload["parameters"]["arbitrary.path"] = "/user/hbase"
    rehash(payload)
    with self.assertRaisesRegex(ManagedDependencyFailure, "DEPENDENCY_COMMAND_INVALID"):
      parse_managed_dependency_command(payload, PROVISION_HDFS_NAMESPACE)

  def test_secure_zookeeper_handoff_fields_are_exact_and_hash_bound(self):
    container = f"/ambari-managed-hbase/{BINDING_ID}"
    payload = command_payload(
      PROVISION_ZOOKEEPER_NAMESPACE,
      {
        "consumer.zk.sasl.id": "hbase_mc_c1",
        "expected.client.port": "2181",
        "expected.quorum": "zk1.example.test,zk2.example.test",
        "namespace.container.znode": container,
        "namespace.ledger.znode": f"/ambari-managed-hbase/.bindings/{BINDING_ID}",
        "namespace.parent.znode": "/ambari-managed-hbase",
        "namespace.znode": container + "/hbase",
        "owner.user": "hbase_mc_c1",
        "parent.acl.policy": "PROVIDER_ADMIN_CREATE_DELETE",
        "provider.service": "ZOOKEEPER",
        "security.mode": "KERBEROS",
        "subtree.acl.policy": "SASL_CONSUMER_PRIVATE",
      },
      operation_id=PROVISION_OPERATION_ID,
    )

    parsed = parse_managed_dependency_command(
      payload, PROVISION_ZOOKEEPER_NAMESPACE
    )
    self.assertEqual("hbase_mc_c1", parsed.parameters["consumer.zk.sasl.id"])
    payload["parameters"]["namespace.ledger.znode"] += "-foreign"
    rehash(payload)
    with self.assertRaisesRegex(ManagedDependencyFailure, "DEPENDENCY_COMMAND_INVALID"):
      parse_managed_dependency_command(payload, PROVISION_ZOOKEEPER_NAMESPACE)

  def test_challenge_initialize_mutate_retry_and_lost_journal(self):
    journal = ManagedDependencyJournal(self.state_root)
    prepare = parsed_command(
      PREPARE_BINDING_JOURNAL,
      {},
      operation_id=PREPARE_OPERATION_ID,
    )
    challenge = journal.prepare(prepare)["initialization.challenge"]
    self.assertEqual(challenge, journal.prepare(prepare)["initialization.challenge"])

    initialize = parsed_command(
      INITIALIZE_BINDING_JOURNAL,
      {
        "initialization.authorization.id": AUTHORIZATION_ID,
        "initialization.challenge": challenge,
        "initialization.prepare.request.hash": prepare.request_hash,
      },
      operation_id=INITIALIZE_OPERATION_ID,
    )
    initialized = journal.initialize(initialize)
    self.assertEqual("true", initialized["journal.initialized"])
    self.assertEqual(initialized, journal.initialize(initialize))

    provision = parsed_command(
      PROVISION_HDFS_NAMESPACE,
      hdfs_provision_parameters(),
      operation_id=PROVISION_OPERATION_ID,
    )
    namespace = os.path.join(self.temporary_root, "provider-namespace")
    calls = []

    def first_provider_mutation(progress):
      calls.append("mutated")
      os.mkdir(namespace, 0o700)
      progress.checkpoint("root-created")
      return {
        "applied.snapshot.fingerprint": SNAPSHOT_HASH,
        "directory.mode": "0700",
        "namespace.root.exists": "true",
        "namespace.wal.exists": "true",
        "owner.group": "hbase_mc_c1",
        "owner.user": "hbase_mc_c1",
        "provider.action.host.id": "21",
      }

    facts = journal.execute(provision, first_provider_mutation)
    self.assertEqual("true", facts["namespace.root.exists"])
    self.assertEqual(0o700, stat.S_IMODE(os.stat(namespace).st_mode))
    self.assertEqual(facts, journal.execute(provision, lambda unused: self.fail("replayed")))
    self.assertEqual(["mutated"], calls)

    os.unlink(os.path.join(self.state_root, f"hdfs-{BINDING_ID}.journal.json"))
    replacement_challenge = journal.prepare(prepare)["initialization.challenge"]
    self.assertNotEqual(challenge, replacement_challenge)
    with self.assertRaisesRegex(ManagedDependencyFailure, "DEPENDENCY_FENCING_UNCERTAIN"):
      journal.initialize(initialize)
    with self.assertRaisesRegex(ManagedDependencyFailure, "DEPENDENCY_FENCING_UNCERTAIN"):
      journal.execute(provision, first_provider_mutation)

  def test_symlink_lock_file_is_rejected(self):
    os.mkdir(self.state_root, 0o700)
    target = os.path.join(self.temporary_root, "target")
    with open(target, "w", encoding="utf-8") as stream:
      stream.write("do not follow")
    os.symlink(target, os.path.join(self.state_root, f"hdfs-{BINDING_ID}.lock"))
    prepare = parsed_command(
      PREPARE_BINDING_JOURNAL,
      {},
      operation_id=PREPARE_OPERATION_ID,
    )

    with self.assertRaisesRegex(ManagedDependencyFailure, "DEPENDENCY_FENCING_UNCERTAIN"):
      ManagedDependencyJournal(self.state_root).prepare(prepare)
    with open(target, encoding="utf-8") as stream:
      self.assertEqual("do not follow", stream.read())

  def test_processes_serialize_on_one_binding_lock(self):
    prepare_payload = command_payload(
      PREPARE_BINDING_JOURNAL,
      {},
      operation_id=PREPARE_OPERATION_ID,
    )
    start = multiprocessing.Event()
    results = multiprocessing.Queue()
    processes = [
      multiprocessing.Process(
        target=prepare_in_process,
        args=(self.state_root, prepare_payload, start, results),
      )
      for unused in range(3)
    ]
    for process in processes:
      process.start()
    start.set()
    for process in processes:
      process.join(5)
      self.assertEqual(0, process.exitcode)

    challenges = [results.get(timeout=1) for unused in processes]
    self.assertEqual(1, len(set(challenges)))

  def test_exact_invalidation_retry_reconciles_after_tombstone(self):
    journal = self._initialized_journal()
    invalidate = parsed_command(
      INVALIDATE_BINDING_EPOCH,
      {},
      operation_id=INVALIDATE_OPERATION_ID,
      epoch=2,
    )
    expected = {
      "highest.accepted.epoch": "2",
      "provider.action.host.id": "21",
    }

    self.assertEqual(expected, journal.execute(invalidate, lambda unused: expected))
    self.assertEqual(
      expected,
      journal.execute(invalidate, lambda unused: self.fail("replayed invalidation")),
    )
    later_provision = parsed_command(
      PROVISION_HDFS_NAMESPACE,
      hdfs_provision_parameters(),
      operation_id="b262c96a-b214-4091-a3f8-951b734c6b86",
      epoch=3,
    )
    with self.assertRaisesRegex(
      ManagedDependencyFailure, "DEPENDENCY_OPERATION_STALE"
    ):
      journal.execute(later_provision, lambda unused: self.fail("retired mutation"))

  def test_initialize_recovers_crashes_before_and_after_pending_rewrite(self):
    journal = ManagedDependencyJournal(self.state_root)
    prepare = parsed_command(
      PREPARE_BINDING_JOURNAL, {}, operation_id=PREPARE_OPERATION_ID
    )
    challenge = journal.prepare(prepare)["initialization.challenge"]
    initialize = parsed_command(
      INITIALIZE_BINDING_JOURNAL,
      {
        "initialization.authorization.id": AUTHORIZATION_ID,
        "initialization.challenge": challenge,
        "initialization.prepare.request.hash": prepare.request_hash,
      },
      operation_id=INITIALIZE_OPERATION_ID,
    )
    original_write = dependency_module._JournalStore.write

    def fail_before_rewrite(store, name, value):
      if value.get("status") == "INITIALIZED":
        raise ManagedDependencyFailure(
          "DEPENDENCY_FENCING_UNCERTAIN", "injected pre-rewrite interruption"
        )
      return original_write(store, name, value)

    with patch.object(dependency_module._JournalStore, "write", fail_before_rewrite):
      with self.assertRaisesRegex(
        ManagedDependencyFailure, "DEPENDENCY_FENCING_UNCERTAIN"
      ):
        journal.initialize(initialize)

    def fail_before_rename(unused_store, unused_source, unused_destination):
      raise ManagedDependencyFailure(
        "DEPENDENCY_FENCING_UNCERTAIN", "injected pre-rename interruption"
      )

    with patch.object(dependency_module._JournalStore, "rename", fail_before_rename):
      with self.assertRaisesRegex(
        ManagedDependencyFailure, "DEPENDENCY_FENCING_UNCERTAIN"
      ):
        journal.initialize(initialize)

    facts = journal.initialize(initialize)
    self.assertEqual("true", facts["journal.initialized"])
    pending = os.path.join(self.state_root, f"hdfs-{BINDING_ID}.pending.json")
    final = os.path.join(self.state_root, f"hdfs-{BINDING_ID}.journal.json")
    self.assertFalse(os.path.exists(pending))
    self.assertTrue(os.path.exists(final))

  def test_scalar_bounds_and_corrupt_journal_identity_fail_closed(self):
    payload = command_payload(
      PREPARE_BINDING_JOURNAL, {}, operation_id=PREPARE_OPERATION_ID
    )
    payload["envelope"]["protocolVersion"] = True
    rehash(payload)
    with self.assertRaisesRegex(ManagedDependencyFailure, "DEPENDENCY_COMMAND_INVALID"):
      parse_managed_dependency_command(payload, PREPARE_BINDING_JOURNAL)

    payload = command_payload(
      PREPARE_BINDING_JOURNAL, {}, operation_id=PREPARE_OPERATION_ID
    )
    payload["envelope"]["epoch"] = 1 << 63
    rehash(payload)
    with self.assertRaisesRegex(ManagedDependencyFailure, "DEPENDENCY_COMMAND_INVALID"):
      parse_managed_dependency_command(payload, PREPARE_BINDING_JOURNAL)

    journal = self._initialized_journal()
    provision = parsed_command(
      PROVISION_HDFS_NAMESPACE,
      hdfs_provision_parameters(),
      operation_id=PROVISION_OPERATION_ID,
    )
    journal_path = os.path.join(
      self.state_root, f"hdfs-{BINDING_ID}.journal.json"
    )
    with open(journal_path, encoding="utf-8") as stream:
      original = json.load(stream)
    corruptions = {
      "highestEpoch": True,
      "initializationChallenge": "not-a-uuid",
      "lastRequestHash": "sha256:not-a-hash",
      "schemaVersion": True,
    }
    for field, value in corruptions.items():
      damaged = dict(original)
      damaged[field] = value
      with open(journal_path, "w", encoding="utf-8") as stream:
        json.dump(damaged, stream)
      os.chmod(journal_path, 0o600)
      with self.subTest(field=field), self.assertRaisesRegex(
        ManagedDependencyFailure, "DEPENDENCY_FENCING_UNCERTAIN"
      ):
        journal.execute(provision, lambda unused: self.fail("corrupt journal"))

  def test_mutation_lock_covers_remote_effect_and_durable_result(self):
    self._initialized_journal()
    effect_started = multiprocessing.Event()
    result_persisted = multiprocessing.Event()
    release_result = multiprocessing.Event()
    invalidation_attempted = multiprocessing.Event()
    invalidation_finished = multiprocessing.Event()
    results = multiprocessing.Queue()
    provision_payload = command_payload(
      PROVISION_HDFS_NAMESPACE,
      hdfs_provision_parameters(),
      operation_id=PROVISION_OPERATION_ID,
    )
    invalidate_payload = command_payload(
      INVALIDATE_BINDING_EPOCH,
      {},
      operation_id=INVALIDATE_OPERATION_ID,
      epoch=2,
    )
    provision_process = multiprocessing.Process(
      target=provision_with_delayed_result,
      args=(
        self.state_root,
        provision_payload,
        effect_started,
        result_persisted,
        release_result,
        results,
      ),
    )
    invalidate_process = multiprocessing.Process(
      target=invalidate_after_effect,
      args=(
        self.state_root,
        invalidate_payload,
        effect_started,
        invalidation_attempted,
        invalidation_finished,
        results,
      ),
    )
    provision_process.start()
    invalidate_process.start()
    try:
      self.assertTrue(result_persisted.wait(2))
      self.assertTrue(invalidation_attempted.wait(2))
      self.assertFalse(invalidation_finished.wait(0.2))
    finally:
      release_result.set()
      for process in (provision_process, invalidate_process):
        process.join(5)
    for process in (provision_process, invalidate_process):
      self.assertEqual(0, process.exitcode)
    self.assertEqual(
      {"provision", "invalidate"},
      {results.get(timeout=1), results.get(timeout=1)},
    )

  def _initialized_journal(self):
    journal = ManagedDependencyJournal(self.state_root)
    prepare = parsed_command(
      PREPARE_BINDING_JOURNAL, {}, operation_id=PREPARE_OPERATION_ID
    )
    challenge = journal.prepare(prepare)["initialization.challenge"]
    initialize = parsed_command(
      INITIALIZE_BINDING_JOURNAL,
      {
        "initialization.authorization.id": AUTHORIZATION_ID,
        "initialization.challenge": challenge,
        "initialization.prepare.request.hash": prepare.request_hash,
      },
      operation_id=INITIALIZE_OPERATION_ID,
    )
    journal.initialize(initialize)
    return journal


def prepare_in_process(state_root, payload, start, results):
  start.wait(2)
  command = parse_managed_dependency_command(payload, PREPARE_BINDING_JOURNAL)
  results.put(ManagedDependencyJournal(state_root).prepare(command)["initialization.challenge"])


def provision_with_delayed_result(
  state_root,
  payload,
  effect_started,
  result_persisted,
  release_result,
  results,
):
  command = parse_managed_dependency_command(payload, PROVISION_HDFS_NAMESPACE)
  journal = ManagedDependencyJournal(state_root)
  original_write = dependency_module._JournalStore.write

  def write_then_wait(store, name, value):
    original_write(store, name, value)
    if (
      value.get("lastCommandName") == PROVISION_HDFS_NAMESPACE
      and value.get("status") == "SUCCEEDED"
    ):
      result_persisted.set()
      release_result.wait(5)

  def mutation(unused_progress):
    effect_started.set()
    return {
      "applied.snapshot.fingerprint": SNAPSHOT_HASH,
      "directory.mode": "0700",
      "namespace.root.exists": "true",
      "namespace.wal.exists": "true",
      "owner.group": "hbase_mc_c1",
      "owner.user": "hbase_mc_c1",
      "provider.action.host.id": "21",
    }

  with patch.object(dependency_module._JournalStore, "write", write_then_wait):
    journal.execute(command, mutation)
  results.put("provision")


def invalidate_after_effect(
  state_root,
  payload,
  effect_started,
  attempted,
  finished,
  results,
):
  effect_started.wait(5)
  command = parse_managed_dependency_command(payload, INVALIDATE_BINDING_EPOCH)
  attempted.set()
  ManagedDependencyJournal(state_root).execute(
    command,
    lambda unused: {
      "highest.accepted.epoch": str(command.epoch),
      "provider.action.host.id": command.action_host_id,
    },
  )
  finished.set()
  results.put("invalidate")


def parsed_command(name, extra_parameters, operation_id, epoch=1):
  return parse_managed_dependency_command(
    command_payload(name, extra_parameters, operation_id, epoch), name
  )


def command_payload(name, extra_parameters, operation_id, epoch=1):
  parameters = {
    "provider.cluster.id": "2",
    "provider.fingerprint": PROVIDER_HASH,
    "provider.service": "HDFS",
    "security.mode": "INSECURE",
    "snapshot.fingerprint": SNAPSHOT_HASH,
  }
  if name in {
    PREPARE_BINDING_JOURNAL,
    INITIALIZE_BINDING_JOURNAL,
    PROVISION_HDFS_NAMESPACE,
    PROVISION_ZOOKEEPER_NAMESPACE,
    INVALIDATE_BINDING_EPOCH,
  }:
    parameters["provider.action.host.id"] = "21"
  parameters.update(extra_parameters)
  envelope = {
    "protocolVersion": 1,
    "bindingId": BINDING_ID,
    "operationId": operation_id,
    "epoch": epoch,
    "snapshotVersion": 7,
    "immutableRequestHash": "sha256:" + "0" * 64,
  }
  command = ManagedDependencyCommand(name, envelope, parameters)
  envelope["immutableRequestHash"] = managed_dependency_request_hash(command)
  return {"name": name, "envelope": envelope, "parameters": parameters}


def rehash(payload):
  command = ManagedDependencyCommand(
    payload["name"], payload["envelope"], payload["parameters"]
  )
  payload["envelope"]["immutableRequestHash"] = managed_dependency_request_hash(command)


def hdfs_provision_parameters():
  return {
    "directory.mode": "0700",
    "namespace.root.uri": hdfs_uri("root"),
    "namespace.wal.uri": hdfs_uri("wal"),
    "owner.group": "hbase_mc_c1",
    "owner.user": "hbase_mc_c1",
  }


def hdfs_verify_parameters():
  return {
    "client.config.json": hdfs_client_config(),
    "client.config.fingerprint": hdfs_client_config_fingerprint(),
    "client.package.name": "hadoop_3_3_0_0_1-client",
    "client.package.version": "3.3.0-1",
    "client.software.kind": "HADOOP_CLIENT",
    "client.software.semantic.version": "3.3.0",
    "consumer.user": "hbase_mc_c1",
    "expected.default.fs": "hdfs://nn.example.test:8020",
    "expected.directory.mode": "0700",
    "expected.namespace.root.uri": hdfs_uri("root"),
    "expected.namespace.wal.uri": hdfs_uri("wal"),
    "expected.owner.group": "hbase_mc_c1",
    "host.id": "42",
    "identity.fingerprint": IDENTITY_HASH,
    "preparation.observation.fingerprint": "sha256:" + "4" * 64,
    "preparation.observation.id": "f70e1c9a-437f-4651-aed7-61cfcc6d9e2c",
    "preparation.request.hash": "sha256:" + "5" * 64,
  }


def hdfs_prepare_parameters():
  strict = {
    "client.package.version",
    "preparation.observation.fingerprint",
    "preparation.observation.id",
    "preparation.request.hash",
  }
  return {
    key: value for key, value in hdfs_verify_parameters().items() if key not in strict
  }


def hdfs_client_config():
  return json.dumps(
    {
      "coreSite": {"fs.defaultFS": "hdfs://nn.example.test:8020"},
      "hdfsSite": {},
      "zooKeeperClient": {},
    },
    sort_keys=True,
    separators=(",", ":"),
  )


def hdfs_client_config_fingerprint():
  return client_config_fingerprint(json.loads(hdfs_client_config()))


def hdfs_uri(child):
  return f"hdfs://nn.example.test:8020{HDFS_BASE}/{BINDING_ID}/{child}"


if __name__ == "__main__":
  unittest.main()
