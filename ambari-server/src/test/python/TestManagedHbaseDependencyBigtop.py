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

import grp
import importlib.util
import json
import multiprocessing
import os
from pathlib import Path
import pwd
import shutil
import stat
import sys
from types import SimpleNamespace
import tempfile
import unittest
from unittest.mock import MagicMock, patch

from resource_management.libraries.functions.managed_dependency import (
  PREPARE_HDFS_CONSUMER,
  PREPARE_ZOOKEEPER_CONSUMER,
  VERIFY_HDFS_CONSUMER,
  VERIFY_ZOOKEEPER_CONSUMER,
  ManagedDependencyCommand,
  ManagedDependencyFailure,
  client_config_fingerprint,
  consumer_mapping_rules_fingerprint,
  managed_dependency_bundle_hash,
  managed_dependency_request_hash,
  parse_managed_dependency_bundle,
  parse_managed_dependency_command,
)


SCRIPT = (
  Path(__file__).resolve().parents[2]
  / "main/resources/stacks/BIGTOP/3.2.0/services/HBASE/package/scripts"
  / "managed_hbase_dependency.py"
)
SPEC = importlib.util.spec_from_file_location("managed_hbase_dependency", SCRIPT)
MODULE = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(MODULE)

CLIENT_SCRIPT = SCRIPT.with_name("hbase_client.py")
CLIENT_SPEC = importlib.util.spec_from_file_location(
  "managed_dependency_hbase_client", CLIENT_SCRIPT
)
CLIENT_MODULE = importlib.util.module_from_spec(CLIENT_SPEC)
with patch.dict(
  sys.modules,
  {
    "hbase": SimpleNamespace(hbase=MagicMock()),
    "upgrade": MagicMock(),
  },
):
  CLIENT_SPEC.loader.exec_module(CLIENT_MODULE)

HDFS_BINDING = "59646bd5-39eb-414e-a07e-f5469d369687"
ZK_BINDING = "997f43e1-9af8-4c74-97fb-1bb4d7a7f839"
HDFS_OPERATION = "43d3dad3-d0b0-4ba2-a9ae-2ccf7ab758f2"
ZK_OPERATION = "2d84a326-ceb9-420a-aa53-0a963ac63db0"
HDFS_VERIFY_OPERATION = "8f4b458d-291a-4d92-9e0c-d160d815a843"
ZK_VERIFY_OPERATION = "50db9cee-a8d7-4a99-97e9-cf2dfe186edf"
HDFS_OBSERVATION = "f70e1c9a-437f-4651-aed7-61cfcc6d9e2c"
ZK_OBSERVATION = "bed26df8-4d2f-4602-b4ba-9b7abdf88231"
HDFS_OBSERVATION_HASH = "sha256:" + "4" * 64
ZK_OBSERVATION_HASH = "sha256:" + "5" * 64
IDENTITY_HASH = "sha256:427bca797fe223f7a612abe0292533df9a97eda718746863a056e186736846bb"
PROVIDER_HASH = "sha256:540491df8aa26c85b4082bec6030c2ccaba50950c79f5dfc12208e1addb1d155"
SNAPSHOT_HASH = "sha256:2bff3497f87dd9a5f2cf9049c3dba3f4caa589e49ecfead1fff799790853fc15"
MAPPING_PROFILE_HASH = "sha256:8b8fcaee5c89ab3cd239eb26da714d4b587aef04639b0d96feedceb15b0344ef"
MAPPING_PROOF_HASH = "sha256:aadab72f4f85131ac69d8415ad39bbb59105829d8da73976ec61cfc7623a8b28"
HDFS_POLICY_HASH = "sha256:" + "6" * 64
HDFS_PAIR_HASH = "sha256:" + "8" * 64
ZOOKEEPER_POLICY_HASH = "sha256:" + "7" * 64
ZOOKEEPER_PAIR_HASH = "sha256:" + "8" * 64


class TestManagedHbaseDependencyBigtop(unittest.TestCase):
  def setUp(self):
    self.temporary = tempfile.mkdtemp(prefix="ambari-managed-hbase-")
    self.service_root = os.path.join(self.temporary, "etc", "hbase")
    self.profile_root = os.path.join(self.service_root, "conf-managed")
    self.hbase_conf = os.path.join(self.service_root, "conf")
    os.makedirs(self.hbase_conf, mode=0o755)
    os.chmod(self.service_root, 0o755)
    os.chmod(self.hbase_conf, 0o755)
    account = pwd.getpwuid(os.geteuid())
    self.user = account.pw_name
    self.group = grp.getgrgid(os.getegid()).gr_name
    self.params = SimpleNamespace(
      config={"configurations": {"hbase-site": hbase_site()}},
      hadoop_bin_dir="/usr/bin",
      hbase_cmd="/usr/lib/hbase/bin/hbase",
      hbase_conf_dir=self.hbase_conf,
      hbase_user=self.user,
      java64_home="/usr/lib/jvm/java",
      tmp_dir=self.temporary,
      user_group=self.group,
    )

  def tearDown(self):
    shutil.rmtree(self.temporary)

  def test_versioned_bundle_renders_exact_configs_and_rejects_stale_selection(self):
    bundle = parsed_bundle(self.user)
    store = MODULE.ManagedHBaseClientConfigStore(
      self.profile_root, self.hbase_conf, self.group
    )

    first_profile = store.select(bundle, hbase_site())
    retry_profile = store.select(bundle, hbase_site())

    self.assertEqual(first_profile, retry_profile)
    self.assertTrue(os.path.islink(os.path.join(self.profile_root, "active")))
    self.assertTrue(os.path.islink(os.path.join(self.hbase_conf, "core-site.xml")))
    with open(os.path.join(first_profile, "manifest.json"), encoding="utf-8") as stream:
      manifest = json.load(stream)
    self.assertEqual(1, manifest["schemaVersion"])
    self.assertTrue(
      all(
        set(command).isdisjoint(MODULE.SECURITY_MANIFEST_FIELDS)
        for command in manifest["commands"].values()
      )
    )
    self.assertEqual(
      hdfs_config(), store.read_selected_config("HDFS")
    )
    self.assertEqual(
      zookeeper_config(), store.read_selected_config("ZOOKEEPER")
    )

    updated = parsed_bundle(self.user, hdfs_snapshot_version=2)
    store.select(updated, hbase_site())
    with self.assertRaisesRegex(ManagedDependencyFailure, "DEPENDENCY_OPERATION_STALE"):
      store.select(bundle, hbase_site())

  def test_schema_two_renders_both_private_profiles_but_reports_only_active_binding(self):
    bundle = parsed_bundle(
      self.user, schema_version=2, preparation_binding_ids=[HDFS_BINDING]
    )
    store = MODULE.ManagedHBaseClientConfigStore(
      self.profile_root, self.hbase_conf, self.group
    )
    profile = store.select(bundle, hbase_site())

    self.assertTrue(os.path.isfile(os.path.join(profile, "managed-hbase-site.xml")))
    self.assertTrue(os.path.isfile(os.path.join(profile, "core-site.xml")))
    self.assertTrue(os.path.isfile(os.path.join(profile, "hdfs-site.xml")))
    with open(os.path.join(profile, "manifest.json"), encoding="utf-8") as stream:
      manifest = json.load(stream)
    self.assertEqual({"HDFS", "ZOOKEEPER"}, set(manifest["commands"]))
    self.assertEqual(zookeeper_config(), store.read_selected_config("ZOOKEEPER"))

    script = MagicMock()
    MODULE.report_managed_dependency_preparation(
      script,
      self.params,
      bundle,
      "master",
      role_command="INSTALL",
      profile_root=self.profile_root,
      call=lambda arguments, **options: (
        0,
        "Hadoop 3.3.0\n" if arguments[0].endswith("/hdfs") else "HBase 2.4.13\n",
      ),
      package_manager=FakePackageManager(),
    )

    results = script.put_structured_out.call_args.args[0][MODULE.PREPARATION_RESULTS_KEY]
    self.assertEqual([HDFS_BINDING], [result["envelope"]["bindingId"] for result in results])
    self.assertEqual(["SUCCEEDED"], [result["status"] for result in results])

  def test_schema_two_empty_reporting_subset_renders_without_result_payload(self):
    bundle = parsed_bundle(self.user, schema_version=2, preparation_binding_ids=[])
    store = MODULE.ManagedHBaseClientConfigStore(
      self.profile_root, self.hbase_conf, self.group
    )
    profile = store.select(bundle, hbase_site())
    self.assertTrue(os.path.isfile(os.path.join(profile, "managed-hbase-site.xml")))
    with open(os.path.join(profile, "manifest.json"), encoding="utf-8") as stream:
      manifest = json.load(stream)
    self.assertEqual({"HDFS", "ZOOKEEPER"}, set(manifest["commands"]))

    script = MagicMock()
    MODULE.report_managed_dependency_preparation(
      script,
      self.params,
      bundle,
      "master",
      role_command="CONFIGURE",
      profile_root=self.profile_root,
      package_manager=FakePackageManager(),
    )
    script.put_structured_out.assert_not_called()

  def test_schema_two_peer_and_node_stale_bundles_are_rejected_monotonically(self):
    initial = parsed_bundle(
      self.user, schema_version=2, preparation_binding_ids=[HDFS_BINDING]
    )
    store = MODULE.ManagedHBaseClientConfigStore(
      self.profile_root, self.hbase_conf, self.group
    )
    store.select(initial, hbase_site())
    newer_peer = parsed_bundle(
      self.user,
      schema_version=2,
      preparation_binding_ids=[HDFS_BINDING],
      zookeeper_epoch=2,
      zookeeper_operation="9ef36739-2f8d-4932-a07e-b181a1af22b9",
    )
    store.select(newer_peer, hbase_site())
    with self.assertRaisesRegex(ManagedDependencyFailure, "DEPENDENCY_OPERATION_STALE"):
      store.select(initial, hbase_site())
    wrong_node = parsed_bundle(
      self.user, schema_version=2, preparation_binding_ids=[HDFS_BINDING]
    )
    wrong_node.host_id = 43
    with self.assertRaisesRegex(
      ManagedDependencyFailure, "DEPENDENCY_CLIENT_CONFIG_MISMATCH"
    ):
      store.select(wrong_node, hbase_site())

  def test_active_failure_keeps_companion_profile_and_emits_no_companion_result(self):
    bundle = parsed_bundle(
      self.user, schema_version=2, preparation_binding_ids=[HDFS_BINDING]
    )
    store = MODULE.ManagedHBaseClientConfigStore(
      self.profile_root, self.hbase_conf, self.group
    )
    profile = store.select(bundle, hbase_site())
    script = MagicMock()
    with self.assertRaisesRegex(ManagedDependencyFailure, "DEPENDENCY_CLIENT_PACKAGE_MISMATCH"):
      MODULE.report_managed_dependency_preparation(
        script,
        self.params,
        bundle,
        "master",
        role_command="INSTALL",
        profile_root=self.profile_root,
        package_manager=SelectivePackageManager({"hadoop_3_3_0_0_1-client"}),
      )

    result = script.put_structured_out.call_args.args[0][MODULE.PREPARATION_RESULTS_KEY]
    self.assertEqual([HDFS_BINDING], [item["envelope"]["bindingId"] for item in result])
    self.assertEqual(["FAILED"], [item["status"] for item in result])
    with open(os.path.join(profile, "manifest.json"), encoding="utf-8") as stream:
      manifest = json.load(stream)
    self.assertEqual({"HDFS", "ZOOKEEPER"}, set(manifest["commands"]))
    self.assertEqual(zookeeper_config(), store.read_selected_config("ZOOKEEPER"))

  def test_two_failed_bindings_recover_independently_without_peer_observations(self):
    store = MODULE.ManagedHBaseClientConfigStore(
      self.profile_root, self.hbase_conf, self.group
    )
    hdfs_failed = parsed_bundle(
      self.user,
      schema_version=2,
      hdfs_epoch=1,
      preparation_binding_ids=[HDFS_BINDING],
    )
    store.select(hdfs_failed, hbase_site())
    hdfs_failure = MagicMock()
    with self.assertRaises(ManagedDependencyFailure):
      MODULE.report_managed_dependency_preparation(
        hdfs_failure, self.params, hdfs_failed, "master", role_command="INSTALL",
        profile_root=self.profile_root,
        package_manager=SelectivePackageManager({"hadoop_3_3_0_0_1-client"}),
      )
    self.assertEqual(
      [HDFS_BINDING],
      [item["envelope"]["bindingId"] for item in hdfs_failure.put_structured_out.call_args.args[0][
        MODULE.PREPARATION_RESULTS_KEY
      ]],
    )
    self.assertEqual(
      ["FAILED"],
      [item["status"] for item in hdfs_failure.put_structured_out.call_args.args[0][
        MODULE.PREPARATION_RESULTS_KEY
      ]],
    )

    zookeeper_failed = parsed_bundle(
      self.user,
      schema_version=2,
      zookeeper_epoch=2,
      zookeeper_operation="9ef36739-2f8d-4932-a07e-b181a1af22b9",
      preparation_binding_ids=[ZK_BINDING],
    )
    store.select(zookeeper_failed, hbase_site())
    zookeeper_failure = MagicMock()
    with self.assertRaises(ManagedDependencyFailure):
      MODULE.report_managed_dependency_preparation(
        zookeeper_failure, self.params, zookeeper_failed, "master", role_command="INSTALL",
        profile_root=self.profile_root,
        package_manager=SelectivePackageManager({"hbase_3_3_0_0_1"}),
      )
    self.assertEqual(
      [ZK_BINDING],
      [item["envelope"]["bindingId"] for item in zookeeper_failure.put_structured_out.call_args.args[0][
        MODULE.PREPARATION_RESULTS_KEY
      ]],
    )
    self.assertEqual(
      ["FAILED"],
      [item["status"] for item in zookeeper_failure.put_structured_out.call_args.args[0][
        MODULE.PREPARATION_RESULTS_KEY
      ]],
    )

    hdfs_recovered = parsed_bundle(
      self.user,
      schema_version=2,
      hdfs_epoch=2,
      hdfs_operation="150581ba-2b36-401e-a7cf-1a80dc7568b6",
      zookeeper_epoch=2,
      zookeeper_operation="9ef36739-2f8d-4932-a07e-b181a1af22b9",
      preparation_binding_ids=[HDFS_BINDING],
    )
    store.select(hdfs_recovered, hbase_site())
    hdfs_success = MagicMock()
    MODULE.report_managed_dependency_preparation(
      hdfs_success, self.params, hdfs_recovered, "master", role_command="INSTALL",
      profile_root=self.profile_root,
      call=lambda arguments, **options: (
        0,
        "Hadoop 3.3.0\n" if arguments[0].endswith("/hdfs") else "HBase 2.4.13\n",
      ),
      package_manager=FakePackageManager(),
    )
    self.assertEqual(
      [HDFS_BINDING],
      [item["envelope"]["bindingId"] for item in hdfs_success.put_structured_out.call_args.args[0][
        MODULE.PREPARATION_RESULTS_KEY
      ]],
    )
    self.assertEqual(
      ["SUCCEEDED"],
      [item["status"] for item in hdfs_success.put_structured_out.call_args.args[0][
        MODULE.PREPARATION_RESULTS_KEY
      ]],
    )

    zookeeper_recovered = parsed_bundle(
      self.user,
      schema_version=2,
      hdfs_epoch=2,
      hdfs_operation="150581ba-2b36-401e-a7cf-1a80dc7568b6",
      zookeeper_epoch=3,
      zookeeper_operation="50db9cee-a8d7-4a99-97e9-cf2dfe186edf",
      preparation_binding_ids=[ZK_BINDING],
    )
    store.select(zookeeper_recovered, hbase_site())
    zookeeper_success = MagicMock()
    MODULE.report_managed_dependency_preparation(
      zookeeper_success, self.params, zookeeper_recovered, "master", role_command="INSTALL",
      profile_root=self.profile_root,
      call=lambda arguments, **options: (
        0,
        "Hadoop 3.3.0\n" if arguments[0].endswith("/hdfs") else "HBase 2.4.13\n",
      ),
      package_manager=FakePackageManager(),
    )
    self.assertEqual(
      [ZK_BINDING],
      [item["envelope"]["bindingId"] for item in zookeeper_success.put_structured_out.call_args.args[0][
        MODULE.PREPARATION_RESULTS_KEY
      ]],
    )
    self.assertEqual(
      ["SUCCEEDED"],
      [item["status"] for item in zookeeper_success.put_structured_out.call_args.args[0][
        MODULE.PREPARATION_RESULTS_KEY
      ]],
    )

  def test_new_epoch_can_reverify_unchanged_snapshot_but_cannot_change_its_content(self):
    original = parsed_bundle(self.user)
    store = MODULE.ManagedHBaseClientConfigStore(
      self.profile_root, self.hbase_conf, self.group
    )
    store.select(original, hbase_site())

    reverify = parsed_bundle(
      self.user,
      hdfs_epoch=2,
      hdfs_operation="9ef36739-2f8d-4932-a07e-b181a1af22b9",
    )
    store.select(reverify, hbase_site())

    changed = parsed_bundle(
      self.user,
      hdfs_epoch=3,
      hdfs_operation="150581ba-2b36-401e-a7cf-1a80dc7568b6",
      hdfs_socket_timeout="120000",
      hdfs_snapshot_hash="sha256:" + "4" * 64,
    )
    with self.assertRaisesRegex(
      ManagedDependencyFailure, "DEPENDENCY_CLIENT_CONFIG_MISMATCH"
    ):
      store.select(changed, hbase_site())
    with self.assertRaisesRegex(ManagedDependencyFailure, "DEPENDENCY_OPERATION_STALE"):
      store.select(original, hbase_site())

  def test_secure_profile_persists_exact_security_lineage_and_rejects_mapping_change(self):
    bundle = parsed_bundle(self.user, security_mode="KERBEROS")
    store = MODULE.ManagedHBaseClientConfigStore(
      self.profile_root, self.hbase_conf, self.group
    )
    profile = store.select(bundle, hbase_site(secure=True, user=self.user))
    with open(os.path.join(profile, "manifest.json"), encoding="utf-8") as stream:
      manifest = json.load(stream)

    self.assertEqual(2, manifest["schemaVersion"])
    for service in ("HDFS", "ZOOKEEPER"):
      selected = manifest["commands"][service]
      self.assertEqual(
        MAPPING_PROFILE_HASH, selected["consumerMappingProfileFingerprint"]
      )
      self.assertEqual(
        MAPPING_PROOF_HASH, selected["consumerMappingProofFingerprint"]
      )
      self.assertIn("consumerMappingRulesFingerprint", selected)
      self.assertIn("providerPolicyFingerprint", selected)
      self.assertIn("providerPairProofFingerprint", selected)

    changed = parsed_bundle(
      self.user,
      security_mode="KERBEROS",
      hdfs_snapshot_version=1,
      hdfs_epoch=2,
      hdfs_operation="150581ba-2b36-401e-a7cf-1a80dc7568b6",
      mapping_profile_hash="sha256:" + "c" * 64,
    )
    with self.assertRaisesRegex(
      ManagedDependencyFailure, "DEPENDENCY_CLIENT_CONFIG_MISMATCH"
    ):
      store.select(changed, hbase_site(secure=True, user=self.user))

  def test_secure_parser_rejects_missing_proof_wrong_rules_and_bundle_profile_mismatch(self):
    payload = preparation_payload(
      "HDFS", self.user, HDFS_BINDING, HDFS_OPERATION, 1,
      security_mode="KERBEROS",
    )
    del payload["parameters"]["provider.security.policy.fingerprint"]
    with self.assertRaisesRegex(ManagedDependencyFailure, "DEPENDENCY_COMMAND_INVALID"):
      parse_managed_dependency_command(payload)

    command = parsed_bundle(self.user, security_mode="KERBEROS").command_for("HDFS")
    changed_config = json.loads(command.parameters["client.config.json"])
    changed_config["coreSite"]["hadoop.security.auth_to_local"] += (
      "\nRULE:[1:$1@$0](extra@EXAMPLE.COM)s/.*/extra/"
    )
    with self.assertRaisesRegex(ManagedDependencyFailure, "DEPENDENCY_COMMAND_INVALID"):
      command_with_parameters(
        command,
        {
          "client.config.json": json.dumps(
            changed_config, sort_keys=True, separators=(",", ":")
          ),
          "client.config.fingerprint": client_config_fingerprint(changed_config),
        },
      )

    with self.assertRaisesRegex(ManagedDependencyFailure, "DEPENDENCY_COMMAND_INVALID"):
      parsed_bundle(
        self.user,
        security_mode="KERBEROS",
        zookeeper_mapping_profile_hash="sha256:" + "d" * 64,
      )

    for malformed in (
      "RULE:[1:$1@$0](user@EXAMPLE.COM)s/.*/user/\r\nDEFAULT",
      "RULE:[1:$1@$0](user@EXAMPLE.COM)s/.*/user/\n\nDEFAULT",
      "x" * (24 * 1024 + 1),
    ):
      with self.assertRaisesRegex(
        ManagedDependencyFailure, "DEPENDENCY_COMMAND_INVALID"
      ):
        consumer_mapping_rules_fingerprint(malformed)

  def test_secure_transport_hash_vector_matches_java_contract(self):
    payload = preparation_payload(
      "HDFS",
      "hbase_mc_c1",
      HDFS_BINDING,
      HDFS_OPERATION,
      7,
      epoch=9,
      security_mode="KERBEROS",
      owner_group="hbase_mc_c1",
    )

    self.assertEqual(
      "sha256:630da583e3050d9c20820af4a6c3b7c22a4227f0574152d2eebfd2a2ea569344",
      payload["parameters"]["consumer.mapping.rules.fingerprint"],
    )
    self.assertEqual(
      "sha256:497cf0d53b9fc2e1fd7490ebb9f1dd56cf2d25d45831b088d4faf77cf27a713d",
      payload["parameters"]["client.config.fingerprint"],
    )
    self.assertEqual(
      "sha256:add05f0fbf9ca7951a938b5ed25e064aba9ca485494e3e978b008738dede770d",
      payload["envelope"]["immutableRequestHash"],
    )

  def test_profile_selection_lock_serializes_read_validate_and_switch(self):
    context = multiprocessing.get_context("fork")
    entered = context.Event()
    release = context.Event()
    second_started = context.Event()
    second_finished = context.Event()
    results = context.Queue()
    first = context.Process(
      target=_select_bundle_in_process,
      args=(
        self.profile_root,
        self.hbase_conf,
        self.group,
        parsed_bundle(self.user),
        entered,
        release,
        None,
        results,
      ),
    )
    second = context.Process(
      target=_select_bundle_in_process,
      args=(
        self.profile_root,
        self.hbase_conf,
        self.group,
        parsed_bundle(
          self.user,
          hdfs_snapshot_version=2,
          hdfs_epoch=2,
          hdfs_operation="9ef36739-2f8d-4932-a07e-b181a1af22b9",
        ),
        second_started,
        None,
        second_finished,
        results,
      ),
    )

    first.start()
    self.assertTrue(entered.wait(5))
    second.start()
    self.assertTrue(second_started.wait(5))
    self.assertFalse(second_finished.wait(0.2))
    release.set()
    first.join(5)
    second.join(5)

    self.assertEqual(0, first.exitcode)
    self.assertEqual(0, second.exitcode)
    self.assertEqual(["SUCCEEDED", "SUCCEEDED"], sorted((results.get(), results.get())))

  def test_delayed_stale_configure_cannot_overwrite_newer_live_configuration(self):
    initial = parsed_bundle(self.user)
    MODULE.ManagedHBaseClientConfigStore(
      self.profile_root, self.hbase_conf, self.group
    ).select(initial, hbase_site())
    context = multiprocessing.get_context("fork")
    newer_entered = context.Event()
    release_newer = context.Event()
    stale_started = context.Event()
    stale_finished = context.Event()
    results = context.Queue()
    live_file = os.path.join(self.hbase_conf, "hbase-site.xml")
    newer = context.Process(
      target=_configure_bundle_in_process,
      args=(
        self.profile_root,
        self.hbase_conf,
        self.group,
        parsed_bundle(
          self.user,
          hdfs_snapshot_version=2,
          hdfs_epoch=2,
          hdfs_operation="9ef36739-2f8d-4932-a07e-b181a1af22b9",
        ),
        live_file,
        b"newer",
        newer_entered,
        release_newer,
        None,
        results,
      ),
    )
    stale = context.Process(
      target=_configure_bundle_in_process,
      args=(
        self.profile_root,
        self.hbase_conf,
        self.group,
        initial,
        live_file,
        b"stale",
        stale_started,
        None,
        stale_finished,
        results,
      ),
    )

    newer.start()
    self.assertTrue(newer_entered.wait(5))
    stale.start()
    self.assertTrue(stale_started.wait(5))
    self.assertFalse(stale_finished.wait(0.2))
    release_newer.set()
    newer.join(5)
    stale.join(5)

    self.assertEqual(0, newer.exitcode)
    self.assertNotEqual(0, stale.exitcode)
    self.assertEqual(b"newer", Path(live_file).read_bytes())
    outcomes = sorted((results.get(), results.get()))
    self.assertIn("SUCCEEDED", outcomes)
    self.assertTrue(any("DEPENDENCY_OPERATION_STALE" in value for value in outcomes))

  def test_verification_pins_selected_profile_until_connectivity_probe_finishes(self):
    original = parsed_bundle(self.user)
    MODULE.ManagedHBaseClientConfigStore(
      self.profile_root, self.hbase_conf, self.group
    ).select(original, hbase_site())
    context = multiprocessing.get_context("fork")
    probe_entered = context.Event()
    release_probe = context.Event()
    update_started = context.Event()
    update_finished = context.Event()
    results = context.Queue()
    verifier = context.Process(
      target=_verify_hdfs_in_process,
      args=(
        self.params,
        self.profile_root,
        verification_for(original, "HDFS"),
        self.user,
        self.group,
        probe_entered,
        release_probe,
        results,
      ),
    )
    update = context.Process(
      target=_select_bundle_in_process,
      args=(
        self.profile_root,
        self.hbase_conf,
        self.group,
        parsed_bundle(
          self.user,
          hdfs_snapshot_version=2,
          hdfs_epoch=2,
          hdfs_operation="9ef36739-2f8d-4932-a07e-b181a1af22b9",
        ),
        update_started,
        None,
        update_finished,
        results,
      ),
    )

    verifier.start()
    self.assertTrue(probe_entered.wait(5))
    update.start()
    self.assertTrue(update_started.wait(5))
    self.assertFalse(update_finished.wait(0.2))
    release_probe.set()
    verifier.join(5)
    update.join(5)

    self.assertEqual(0, verifier.exitcode)
    self.assertEqual(0, update.exitcode)
    self.assertEqual(["SUCCEEDED", "SUCCEEDED"], sorted((results.get(), results.get())))

  def test_immutable_profile_conflict_and_missing_active_bundle_fail_closed(self):
    bundle = parsed_bundle(self.user)
    store = MODULE.ManagedHBaseClientConfigStore(
      self.profile_root, self.hbase_conf, self.group
    )
    profile = store.select(bundle, hbase_site())
    with open(os.path.join(profile, "core-site.xml"), "wb") as stream:
      stream.write(b"foreign")
    os.chmod(os.path.join(profile, "core-site.xml"), 0o640)

    with self.assertRaisesRegex(ManagedDependencyFailure, "DEPENDENCY_CLIENT_CONFIG_MISMATCH"):
      store.select(bundle, hbase_site())

    with patch.object(MODULE.Script, "get_config", return_value={"commandParams": {}}):
      with self.assertRaisesRegex(ManagedDependencyFailure, "DEPENDENCY_SNAPSHOT_NOT_APPROVED"):
        MODULE.configure_managed_dependency_client(self.params, self.profile_root)

  def test_no_bundle_and_no_active_marker_preserves_local_mode(self):
    with patch.object(MODULE.Script, "get_config", return_value={"commandParams": {}}):
      self.assertIsNone(
        MODULE.configure_managed_dependency_client(self.params, self.profile_root)
      )
    self.assertFalse(os.path.exists(self.profile_root))

  def test_install_reports_measured_preparation_and_start_does_not_reobserve(self):
    bundle = parsed_bundle(self.user)
    MODULE.ManagedHBaseClientConfigStore(
      self.profile_root, self.hbase_conf, self.group
    ).select(bundle, hbase_site())
    script = MagicMock()

    def version_call(arguments, **unused_options):
      if arguments[0].endswith("/hdfs"):
        return 0, "Hadoop 3.3.0\n"
      return 0, "HBase 2.4.13\n"

    MODULE.report_managed_dependency_preparation(
      script,
      self.params,
      bundle,
      "master",
      role_command="INSTALL",
      profile_root=self.profile_root,
      call=version_call,
      package_manager=FakePackageManager(),
    )

    output = script.put_structured_out.call_args.args[0]
    results = output[MODULE.PREPARATION_RESULTS_KEY]
    self.assertEqual(2, len(results))
    self.assertTrue(all(result["status"] == "SUCCEEDED" for result in results))
    self.assertEqual(
      {"HADOOP_CLIENT", "HBASE_CLIENT"},
      {result["facts"]["client.software.kind"] for result in results},
    )
    self.assertEqual(
      {"3.3.0-1", "2.4.13-1"},
      {result["facts"]["client.package.version"] for result in results},
    )

    start_script = MagicMock()
    start_call = MagicMock()
    package_manager = MagicMock()
    MODULE.report_managed_dependency_preparation(
      start_script,
      self.params,
      bundle,
      "master",
      role_command="START",
      profile_root=self.profile_root,
      call=start_call,
      package_manager=package_manager,
    )
    start_script.put_structured_out.assert_not_called()
    start_call.assert_not_called()
    package_manager.get_installed_package_version.assert_not_called()

  def test_hbase_client_install_reports_only_after_client_rendering(self):
    client = CLIENT_MODULE.HbaseClient()
    env = MagicMock()
    bundle = parsed_bundle(self.user)
    report = MagicMock()
    order = []
    report.side_effect = lambda *unused: order.append("report")

    with patch.dict(
      sys.modules,
      {"params": self.params, "managed_hbase_dependency": MODULE},
    ), patch.object(client, "install_packages"), patch.object(
      CLIENT_MODULE.upgrade, "select_phoenix_packages"
    ), patch.object(
      CLIENT_MODULE,
      "hbase",
      side_effect=lambda **unused: order.append("render") or bundle,
    ) as render, patch.object(
      MODULE, "report_managed_dependency_preparation", report
    ):
      client.install(env)

    render.assert_called_once_with(name="client")
    report.assert_called_once_with(client, self.params, bundle, "client")
    self.assertEqual(["render", "report"], order)

  def test_hbase_client_local_configure_preserves_none_bundle(self):
    client = CLIENT_MODULE.HbaseClient()
    env = MagicMock()
    report = MagicMock()

    with patch.dict(
      sys.modules,
      {"params": self.params, "managed_hbase_dependency": MODULE},
    ), patch.object(
      CLIENT_MODULE, "hbase", return_value=None
    ) as render, patch.object(
      MODULE, "report_managed_dependency_preparation", report
    ):
      client.configure(env)

    render.assert_called_once_with(name="client")
    report.assert_called_once_with(client, self.params, None, "client")

  def test_managed_preflight_allows_missing_conf_then_resolves_approved_package_link(self):
    package_root = os.path.join(self.temporary, "package-root")
    selected_conf = os.path.join(package_root, "current", "hbase-client", "conf")
    shutil.rmtree(self.hbase_conf)
    os.makedirs(selected_conf, mode=0o755)
    os.symlink(selected_conf, self.hbase_conf)
    store = MODULE.ManagedHBaseClientConfigStore(
      self.profile_root,
      self.hbase_conf,
      self.group,
      package_config_roots=(package_root,),
    )

    with store.protect_configuration(parsed_bundle(self.user), hbase_site()):
      store.select_locked(parsed_bundle(self.user), hbase_site())

    self.assertTrue(os.path.islink(os.path.join(selected_conf, "core-site.xml")))

  def test_managed_preflight_allows_package_to_create_missing_conf(self):
    shutil.rmtree(self.hbase_conf)
    store = MODULE.ManagedHBaseClientConfigStore(
      self.profile_root, self.hbase_conf, self.group
    )
    bundle = parsed_bundle(self.user)

    with store.protect_configuration(bundle, hbase_site()):
      os.makedirs(self.hbase_conf, mode=0o755)
      store.select_locked(bundle, hbase_site())

    self.assertTrue(os.path.islink(os.path.join(self.hbase_conf, "core-site.xml")))

  def test_hdfs_verification_uses_real_selected_profile_and_nonempty_probe(self):
    bundle = parsed_bundle(self.user)
    store = MODULE.ManagedHBaseClientConfigStore(
      self.profile_root, self.hbase_conf, self.group
    )
    store.select(bundle, hbase_site())
    hdfs = FakeHdfs(self.user, self.group)
    verifier = MODULE.ManagedHBaseConsumerVerifier(
      self.params,
      profile_root=self.profile_root,
      call=hdfs,
      package_manager=FakePackageManager(),
    )

    facts = verifier.verify_hdfs(verification_for(bundle, "HDFS"))

    self.assertEqual("true", facts["namenode.rpc.connected"])
    self.assertEqual("true", facts["datanode.read.write.verified"])
    self.assertEqual(MODULE.PROBE_BYTES, hdfs.upload_size)
    self.assertEqual({}, hdfs.remote_files)
    self.assertEqual(
      (
        os.path.join(self.params.hadoop_bin_dir, "hdfs"),
        "--config",
        os.path.join(self.profile_root, "active"),
        "version",
      ),
      hdfs.calls[0][0],
    )
    self.assertTrue(all(options["user"] == self.user for _, options in hdfs.calls))
    self.assertTrue(all(options["shell"] is False for _, options in hdfs.calls))

  def test_strict_verification_rejects_wrong_preparation_lineage_before_probe(self):
    bundle = parsed_bundle(self.user)
    MODULE.ManagedHBaseClientConfigStore(
      self.profile_root, self.hbase_conf, self.group
    ).select(bundle, hbase_site())
    command = command_with_parameters(
      verification_for(bundle, "HDFS"),
      {"preparation.request.hash": "sha256:" + "6" * 64},
    )
    call = MagicMock()

    with self.assertRaisesRegex(
      ManagedDependencyFailure, "DEPENDENCY_OPERATION_STALE"
    ):
      self._verifier(call).verify_hdfs(command)

    call.assert_not_called()

  def test_secure_verification_rejects_changed_mapping_lineage_before_probe(self):
    bundle = parsed_bundle(self.user, security_mode="KERBEROS")
    MODULE.ManagedHBaseClientConfigStore(
      self.profile_root, self.hbase_conf, self.group
    ).select(bundle, hbase_site(secure=True, user=self.user))
    command = command_with_parameters(
      verification_for(bundle, "HDFS"),
      {"consumer.mapping.proof.fingerprint": "sha256:" + "a" * 64},
    )
    call = MagicMock()

    with self.assertRaisesRegex(
      ManagedDependencyFailure, "DEPENDENCY_OPERATION_STALE"
    ):
      self._verifier(call).verify_hdfs(command)

    call.assert_not_called()

  def test_verification_rejects_non_object_active_manifest_as_stale(self):
    bundle = parsed_bundle(self.user)
    store = MODULE.ManagedHBaseClientConfigStore(
      self.profile_root, self.hbase_conf, self.group
    )
    profile = store.select(bundle, hbase_site())
    with open(os.path.join(profile, "manifest.json"), "w", encoding="utf-8") as stream:
      json.dump([], stream)
    call = MagicMock()

    with self.assertRaisesRegex(
      ManagedDependencyFailure, "DEPENDENCY_OPERATION_STALE"
    ):
      self._verifier(call).verify_hdfs(verification_for(bundle, "HDFS"))

    call.assert_not_called()

  def test_strict_verification_rechecks_package_and_software_after_probe(self):
    bundle = parsed_bundle(self.user)
    MODULE.ManagedHBaseClientConfigStore(
      self.profile_root, self.hbase_conf, self.group
    ).select(bundle, hbase_site())
    command = verification_for(bundle, "HDFS")

    with self.assertRaisesRegex(
      ManagedDependencyFailure, "DEPENDENCY_CLIENT_PACKAGE_MISMATCH"
    ):
      MODULE.ManagedHBaseConsumerVerifier(
        self.params,
        profile_root=self.profile_root,
        call=FakeHdfs(self.user, self.group),
        package_manager=ChangingPackageManager(),
      ).verify_hdfs(command)

    with self.assertRaisesRegex(
      ManagedDependencyFailure, "DEPENDENCY_CLIENT_PACKAGE_MISMATCH"
    ):
      MODULE.ManagedHBaseConsumerVerifier(
        self.params,
        profile_root=self.profile_root,
        call=FakeHdfs(self.user, self.group, change_software_version=True),
        package_manager=FakePackageManager(),
      ).verify_hdfs(command)

  def test_hdfs_reports_separate_namenode_integrity_and_cleanup_failures(self):
    bundle = parsed_bundle(self.user)
    MODULE.ManagedHBaseClientConfigStore(
      self.profile_root, self.hbase_conf, self.group
    ).select(bundle, hbase_site())
    command = verification_for(bundle, "HDFS")

    namenode = FakeHdfs(self.user, self.group, fail_stat=True)
    with self.assertRaisesRegex(ManagedDependencyFailure, "DEPENDENCY_NAMENODE_RPC_FAILED"):
      self._verifier(namenode).verify_hdfs(command)

    corrupt = FakeHdfs(self.user, self.group, corrupt_read=True)
    with self.assertRaisesRegex(ManagedDependencyFailure, "DEPENDENCY_DATA_INTEGRITY_FAILED"):
      self._verifier(corrupt).verify_hdfs(command)
    self.assertEqual({}, corrupt.remote_files)

    cleanup = FakeHdfs(self.user, self.group, fail_cleanup=True)
    with self.assertRaisesRegex(ManagedDependencyFailure, "DEPENDENCY_PROBE_CLEANUP_FAILED"):
      self._verifier(cleanup).verify_hdfs(command)

    def timeout(arguments, **unused_options):
      if arguments[-1] == "version":
        return 0, "Hadoop 3.3.0\n"
      raise TimeoutError("raw command detail must not escape")

    with self.assertRaisesRegex(ManagedDependencyFailure, "DEPENDENCY_NAMENODE_RPC_FAILED"):
      self._verifier(timeout).verify_hdfs(command)

  def test_zookeeper_verification_uses_hbase_client_classpath_and_no_admin_identity(self):
    bundle = parsed_bundle(self.user)
    MODULE.ManagedHBaseClientConfigStore(
      self.profile_root, self.hbase_conf, self.group
    ).select(bundle, hbase_site())
    calls = []

    def call(arguments, **options):
      calls.append((arguments, options))
      if arguments[-1] == "version":
        return 0, "HBase 2.4.13\n"
      if arguments[-1] == "classpath":
        return 0, "/usr/lib/hbase/lib/*"
      return 0, '{"connected":true,"privateZnodeVerified":true}\n'

    facts = MODULE.ManagedHBaseConsumerVerifier(
      self.params,
      profile_root=self.profile_root,
      call=call,
      package_manager=FakePackageManager(),
    ).verify_zookeeper(verification_for(bundle, "ZOOKEEPER"))

    self.assertEqual(
      (self.params.hbase_cmd, "--config", self.hbase_conf, "version"),
      calls[0][0],
    )
    java_arguments, options = calls[2]
    self.assertTrue(java_arguments[2].startswith("/usr/lib/hbase/lib/*"))
    self.assertTrue(java_arguments[2].endswith(MODULE.ZOOKEEPER_HELPER_JAR))
    self.assertEqual(self.user, options["user"])
    self.assertNotIn("--delete", java_arguments)
    self.assertEqual("true", facts["private.znode.verified"])

  def test_zookeeper_helper_timeout_is_sanitized(self):
    bundle = parsed_bundle(self.user)
    MODULE.ManagedHBaseClientConfigStore(
      self.profile_root, self.hbase_conf, self.group
    ).select(bundle, hbase_site())
    calls = 0

    def call(arguments, **unused_options):
      nonlocal calls
      calls += 1
      if arguments[-1] == "version":
        return 0, "HBase 2.4.13\n"
      if arguments[-1] == "classpath":
        return 0, "/usr/lib/hbase/lib/*"
      raise TimeoutError("raw command detail must not escape")

    verifier = MODULE.ManagedHBaseConsumerVerifier(
      self.params,
      profile_root=self.profile_root,
      call=call,
      package_manager=FakePackageManager(),
    )
    with self.assertRaisesRegex(
      ManagedDependencyFailure, "DEPENDENCY_ZOOKEEPER_SESSION_FAILED"
    ) as failure:
      verifier.verify_zookeeper(verification_for(bundle, "ZOOKEEPER"))
    self.assertNotIn("raw command detail", failure.exception.sanitized_message)

  def test_secure_hdfs_uses_exact_role_principal_and_private_ticket_cache(self):
    bundle = parsed_bundle(self.user, security_mode="KERBEROS")
    self._configure_secure_role()
    MODULE.ManagedHBaseClientConfigStore(
      self.profile_root, self.hbase_conf, self.group
    ).select(bundle, hbase_site(secure=True, user=self.user))
    hdfs = FakeHdfs(self.user, self.group)
    cache = FakeKerberosCache()
    verifier = MODULE.ManagedHBaseConsumerVerifier(
      self.params,
      profile_root=self.profile_root,
      call=hdfs,
      package_manager=FakePackageManager(),
      role="master",
    )

    with patch.object(MODULE, "PrivateKerberosCache", return_value=cache):
      facts = verifier.verify_hdfs(verification_for(bundle, "HDFS"))

    self.assertEqual(self.params.master_jaas_princ, facts["consumer.kerberos.principal"])
    self.assertEqual(
      (self.params.kinit_path_local, self.params.master_keytab_path, self.params.master_jaas_princ),
      cache.kinit_arguments,
    )
    self.assertTrue(
      all(options["environment"]["KRB5CCNAME"] == "FILE:/private/cache" for _, options in hdfs.calls)
    )

  def test_secure_zookeeper_proves_exact_sasl_acl_and_sibling_denial(self):
    bundle = parsed_bundle(self.user, security_mode="KERBEROS")
    self._configure_secure_role()
    MODULE.ManagedHBaseClientConfigStore(
      self.profile_root, self.hbase_conf, self.group
    ).select(bundle, hbase_site(secure=True, user=self.user))
    calls = []

    def call(arguments, **options):
      calls.append((arguments, options))
      if arguments[-1] == "version":
        return 0, "HBase 2.4.13\n"
      if arguments[-1] == "classpath":
        return 0, "/usr/lib/hbase/lib/*"
      return 0, json.dumps(
        {
          "connected": True,
          "consumerSaslId": self.user,
          "hbaseZnodeVerified": True,
          "privateContainerVerified": True,
          "siblingAuthorityDenied": True,
        }
      )

    with patch.object(MODULE, "PrivateKerberosCache", return_value=FakeKerberosCache()):
      facts = MODULE.ManagedHBaseConsumerVerifier(
        self.params,
        profile_root=self.profile_root,
        call=call,
        package_manager=FakePackageManager(),
        role="master",
      ).verify_zookeeper(verification_for(bundle, "ZOOKEEPER"))

    java_arguments, options = calls[2]
    self.assertIn("-Djava.security.auth.login.config=" + self.params.master_jaas_file,
                  java_arguments)
    self.assertEqual("VERIFY_SECURE", java_arguments[java_arguments.index("--operation") + 1])
    self.assertEqual(self.user,
                     java_arguments[java_arguments.index("--consumer-sasl-id") + 1])
    self.assertEqual("FILE:/private/cache", options["environment"]["KRB5CCNAME"])
    self.assertEqual("true", facts["container.acl.verified"])
    self.assertEqual("true", facts["sibling.authority.denied"])

  def test_secure_shared_superuser_policy_fails_before_network(self):
    bundle = parsed_bundle(self.user, security_mode="KERBEROS")
    self._configure_secure_role()
    self.params.config["configurations"]["hbase-site"]["hbase.superuser"] = (
      self.user + ",hbase"
    )
    MODULE.ManagedHBaseClientConfigStore(
      self.profile_root, self.hbase_conf, self.group
    ).select(bundle, hbase_site(secure=True, user=self.user))
    call = MagicMock()

    with self.assertRaisesRegex(
      ManagedDependencyFailure, "DEPENDENCY_CLIENT_IDENTITY_MISMATCH"
    ):
      MODULE.ManagedHBaseConsumerVerifier(
        self.params,
        profile_root=self.profile_root,
        call=call,
        package_manager=FakePackageManager(),
        role="master",
      ).verify_hdfs(verification_for(bundle, "HDFS"))
    call.assert_not_called()

  def test_credential_metadata_distinguishes_service_keytab_and_root_jaas(self):
    service_uid = 12345 if os.geteuid() != 12345 else 12346
    service_gid = 23456
    service_keytab = SimpleNamespace(
      st_mode=stat.S_IFREG | 0o400, st_uid=service_uid, st_gid=service_gid
    )
    root_jaas = SimpleNamespace(
      st_mode=stat.S_IFREG | 0o640, st_uid=0, st_gid=service_gid
    )

    self.assertTrue(
      MODULE._credential_metadata_matches(
        service_keytab, service_uid, service_gid, 0o400
      )
    )
    self.assertTrue(
      MODULE._credential_metadata_matches(root_jaas, 0, service_gid, 0o640)
    )
    self.assertFalse(
      MODULE._credential_metadata_matches(
        service_keytab, os.geteuid(), service_gid, 0o400
      )
    )
    self.assertFalse(
      MODULE._credential_metadata_matches(
        SimpleNamespace(
          st_mode=stat.S_IFREG | 0o640,
          st_uid=service_uid,
          st_gid=service_gid,
        ),
        service_uid,
        service_gid,
        0o400,
      )
    )

  def test_role_credential_resolution_requires_service_keytab_and_root_jaas(self):
    service_name = "hbase_managed_test"
    bundle = parsed_bundle(service_name, security_mode="KERBEROS")
    self._configure_secure_role(service_name)
    service_uid = 12345 if os.geteuid() != 12345 else 12346
    service_gid = 23456
    service = SimpleNamespace(pw_uid=service_uid)
    root = SimpleNamespace(pw_uid=0)
    group = SimpleNamespace(gr_gid=service_gid)
    keytab = SimpleNamespace(
      st_mode=stat.S_IFREG | 0o400, st_uid=service_uid, st_gid=service_gid
    )
    jaas = SimpleNamespace(
      st_mode=stat.S_IFREG | 0o640, st_uid=0, st_gid=service_gid
    )

    def account(name):
      return root if name == "root" else service

    def metadata(path, **unused):
      return keytab if path == self.params.master_keytab_path else jaas

    verifier = MODULE.ManagedHBaseConsumerVerifier(
      self.params,
      profile_root=self.profile_root,
      call=MagicMock(),
      package_manager=FakePackageManager(),
      role="master",
    )
    with patch.object(MODULE.pwd, "getpwnam", side_effect=account), patch.object(
      MODULE.grp, "getgrnam", return_value=group
    ), patch.object(MODULE.os, "stat", side_effect=metadata):
      principal, observed_keytab, observed_jaas = verifier._role_kerberos_material(
        verification_for(bundle, "HDFS")
      )
    self.assertEqual(self.params.master_jaas_princ, principal)
    self.assertEqual(self.params.master_keytab_path, observed_keytab)
    self.assertEqual(self.params.master_jaas_file, observed_jaas)

    runner_owned_keytab = SimpleNamespace(
      st_mode=stat.S_IFREG | 0o400,
      st_uid=os.geteuid(),
      st_gid=service_gid,
    )

    def unsafe_metadata(path, **unused):
      return runner_owned_keytab if path == self.params.master_keytab_path else jaas

    with patch.object(MODULE.pwd, "getpwnam", side_effect=account), patch.object(
      MODULE.grp, "getgrnam", return_value=group
    ), patch.object(MODULE.os, "stat", side_effect=unsafe_metadata):
      with self.assertRaisesRegex(
        ManagedDependencyFailure, "DEPENDENCY_CLIENT_IDENTITY_MISMATCH"
      ):
        verifier._role_kerberos_material(verification_for(bundle, "HDFS"))

  def _configure_secure_role(self, user=None):
    user = user or self.user
    principal = f"{user}/host.example.test@EXAMPLE.COM"
    keytab = os.path.join(self.temporary, "hbase-master.keytab")
    jaas = os.path.join(self.hbase_conf, "hbase_master_jaas.conf")
    with open(keytab, "wb") as stream:
      stream.write(b"test fixture, not a credential")
    os.chmod(keytab, 0o400)
    with open(jaas, "w", encoding="utf-8") as stream:
      stream.write(
        "Client { com.sun.security.auth.module.Krb5LoginModule required "
        f'useKeyTab=true useTicketCache=false keyTab="{keytab}" '
        f'principal="{principal}"; }};\n'
      )
    os.chmod(jaas, 0o640)
    self.params.security_enabled = True
    self.params.hbase_user = user
    self.params.hostname = "host.example.test"
    self.params.kinit_path_local = "/usr/bin/kinit"
    self.params.master_jaas_princ = principal
    self.params.master_keytab_path = keytab
    self.params.master_jaas_file = jaas
    self.params.config = {
      "configurations": {"hbase-site": hbase_site(secure=True, user=user)}
    }

  def _verifier(self, call):
    return MODULE.ManagedHBaseConsumerVerifier(
      self.params,
      profile_root=self.profile_root,
      call=call,
      package_manager=FakePackageManager(),
    )


class FakeKerberosCache:
  def __init__(self):
    self.kinit_arguments = None
    self.environment = {"KRB5CCNAME": "FILE:/private/cache"}

  def __enter__(self):
    return self

  def __exit__(self, *unused):
    return False

  def kinit(self, kinit_path, keytab, principal, timeout=None):
    self.kinit_arguments = (kinit_path, keytab, principal)

  def merge_environment(self, environment=None):
    return {**(environment or {}), **self.environment}


class FakePackageManager:
  def get_installed_package_version(self, package_name):
    if package_name.startswith("hadoop"):
      return "3.3.0-1"
    if package_name.startswith("hbase"):
      return "2.4.13-1"
    return None


class SelectivePackageManager(FakePackageManager):
  def __init__(self, failed_packages):
    self.failed_packages = set(failed_packages)

  def get_installed_package_version(self, package_name):
    if package_name in self.failed_packages:
      return None
    return super().get_installed_package_version(package_name)


class ChangingPackageManager(FakePackageManager):
  def __init__(self):
    self.calls = 0

  def get_installed_package_version(self, package_name):
    self.calls += 1
    if self.calls == 1:
      return super().get_installed_package_version(package_name)
    return "3.3.0-2"


class FakeHdfs:
  def __init__(
    self,
    user,
    group,
    fail_stat=False,
    corrupt_read=False,
    fail_cleanup=False,
    change_software_version=False,
  ):
    self.user = user
    self.group = group
    self.fail_stat = fail_stat
    self.corrupt_read = corrupt_read
    self.fail_cleanup = fail_cleanup
    self.calls = []
    self.remote_files = {}
    self.upload_size = 0
    self.change_software_version = change_software_version
    self.version_calls = 0

  def __call__(self, arguments, **options):
    self.calls.append((arguments, options))
    if arguments[-1] == "version":
      self.version_calls += 1
      version = "3.3.1" if self.change_software_version and self.version_calls > 1 else "3.3.0"
      return 0, "Hadoop " + version + "\n"
    operation = arguments[arguments.index("dfs") + 1 :]
    if operation[0] == "-stat":
      if self.fail_stat:
        return 1, ""
      return 0, f"{self.user}:{self.group}:700"
    if operation[:2] == ("-test", "-e"):
      return (0 if operation[2] in self.remote_files else 1), ""
    if operation[0] == "-put":
      with open(operation[1], "rb") as stream:
        self.remote_files[operation[2]] = stream.read()
      self.upload_size = len(self.remote_files[operation[2]])
      return 0, ""
    if operation[0] == "-get":
      content = self.remote_files[operation[1]]
      if self.corrupt_read:
        content = b"corrupt"
      with open(operation[2], "wb") as stream:
        stream.write(content)
      return 0, ""
    if operation[:2] == ("-rm", "-f"):
      if self.fail_cleanup:
        return 1, ""
      self.remote_files.pop(operation[2], None)
      return 0, ""
    raise AssertionError("unexpected HDFS invocation: " + repr(operation))


class _BlockingHdfs(FakeHdfs):
  def __init__(self, user, group, entered, release):
    super().__init__(user, group)
    self.entered = entered
    self.release = release
    self.blocked = False

  def __call__(self, arguments, **options):
    if arguments[-1] == "version":
      return super().__call__(arguments, **options)
    operation = arguments[arguments.index("dfs") + 1 :]
    if operation[0] == "-stat" and not self.blocked:
      self.blocked = True
      self.entered.set()
      if not self.release.wait(5):
        raise AssertionError("timed out waiting to release verification probe")
    return super().__call__(arguments, **options)


def parsed_bundle(
  user,
  hdfs_snapshot_version=1,
  hdfs_epoch=None,
  hdfs_operation=HDFS_OPERATION,
  hdfs_socket_timeout="60000",
  hdfs_snapshot_hash=SNAPSHOT_HASH,
  security_mode="INSECURE",
  mapping_profile_hash=MAPPING_PROFILE_HASH,
  zookeeper_mapping_profile_hash=None,
  schema_version=1,
  preparation_binding_ids=None,
  zookeeper_snapshot_version=1,
  zookeeper_epoch=None,
  zookeeper_operation=ZK_OPERATION,
):
  hdfs = preparation_payload(
    "HDFS",
    user,
    HDFS_BINDING,
    hdfs_operation,
    hdfs_snapshot_version,
    epoch=hdfs_epoch,
    socket_timeout=hdfs_socket_timeout,
    snapshot_hash=hdfs_snapshot_hash,
    security_mode=security_mode,
    mapping_profile_hash=mapping_profile_hash,
  )
  zookeeper = preparation_payload(
    "ZOOKEEPER", user, ZK_BINDING, zookeeper_operation, zookeeper_snapshot_version,
    epoch=zookeeper_epoch, security_mode=security_mode,
    mapping_profile_hash=(
      mapping_profile_hash
      if zookeeper_mapping_profile_hash is None
      else zookeeper_mapping_profile_hash
    ),
  )
  commands = [
    parse_managed_dependency_command(hdfs),
    parse_managed_dependency_command(zookeeper),
  ]
  value = {
    "schemaVersion": schema_version,
    "hostId": 42,
    "consumerUser": user,
    "identityFingerprint": IDENTITY_HASH,
    "commands": [hdfs, zookeeper],
    "immutableBundleHash": managed_dependency_bundle_hash(
      1, 42, user, IDENTITY_HASH, commands
    ),
  }
  if schema_version == 2:
    value["preparationBindingIds"] = (
      [HDFS_BINDING, ZK_BINDING]
      if preparation_binding_ids is None else list(preparation_binding_ids)
    )
    value["immutableBundleHash"] = managed_dependency_bundle_hash(
      schema_version,
      42,
      user,
      IDENTITY_HASH,
      commands,
      value["preparationBindingIds"],
    )
  return parse_managed_dependency_bundle(value)


def preparation_payload(
  service,
  user,
  binding_id,
  operation_id,
  snapshot_version,
  epoch=None,
  socket_timeout="60000",
  snapshot_hash=SNAPSHOT_HASH,
  security_mode="INSECURE",
  mapping_profile_hash=MAPPING_PROFILE_HASH,
  owner_group=None,
):
  client_config = (
    hdfs_config(socket_timeout, security_mode == "KERBEROS", user)
    if service == "HDFS"
    else zookeeper_config(security_mode == "KERBEROS")
  )
  parameters = {
    "client.config.fingerprint": client_config_fingerprint(client_config),
    "client.config.json": json.dumps(
      client_config, sort_keys=True, separators=(",", ":")
    ),
    "client.package.name": (
      "hadoop_3_3_0_0_1-client" if service == "HDFS" else "hbase_3_3_0_0_1"
    ),
    "client.software.kind": "HADOOP_CLIENT" if service == "HDFS" else "HBASE_CLIENT",
    "client.software.semantic.version": "3.3.0" if service == "HDFS" else "2.4.13",
    "consumer.user": user,
    "host.id": "42",
    "identity.fingerprint": IDENTITY_HASH,
    "provider.cluster.id": "2",
    "provider.fingerprint": PROVIDER_HASH,
    "provider.service": service,
    "security.mode": security_mode,
    "snapshot.fingerprint": snapshot_hash,
  }
  if security_mode == "KERBEROS":
    parameters["consumer.kerberos.principal.pattern"] = (
      f"{user}/_HOST@EXAMPLE.COM"
    )
    parameters.update(
      {
        "consumer.mapping.profile.fingerprint": mapping_profile_hash,
        "consumer.mapping.proof.fingerprint": MAPPING_PROOF_HASH,
        "consumer.mapping.rules.fingerprint": consumer_mapping_rules_fingerprint(
          consumer_rules(user)
        ),
        "provider.security.pair.proof.fingerprint": (
          HDFS_PAIR_HASH if service == "HDFS" else ZOOKEEPER_PAIR_HASH
        ),
        "provider.security.policy.fingerprint": (
          HDFS_POLICY_HASH if service == "HDFS" else ZOOKEEPER_POLICY_HASH
        ),
      }
    )
  name = PREPARE_HDFS_CONSUMER if service == "HDFS" else PREPARE_ZOOKEEPER_CONSUMER
  if service == "HDFS":
    parameters.update(
      {
        "expected.default.fs": "hdfs://nn.example.test:8020",
        "expected.directory.mode": "0700",
        "expected.namespace.root.uri": hdfs_uri(binding_id, "root"),
        "expected.namespace.wal.uri": hdfs_uri(binding_id, "wal"),
        "expected.owner.group": (
          grp.getgrgid(os.getegid()).gr_name if owner_group is None else owner_group
        ),
      }
    )
  else:
    parameters["expected.namespace.container.znode"] = (
      "/ambari-managed-hbase/" + binding_id
    )
    parameters["expected.namespace.parent.znode"] = "/ambari-managed-hbase"
    parameters["expected.namespace.znode"] = (
      "/ambari-managed-hbase/" + binding_id + "/hbase"
    )
    if security_mode == "KERBEROS":
      parameters["expected.consumer.zk.sasl.id"] = user
  envelope = {
    "bindingId": binding_id,
    "epoch": snapshot_version if epoch is None else epoch,
    "immutableRequestHash": "sha256:" + "0" * 64,
    "operationId": operation_id,
    "protocolVersion": 1,
    "snapshotVersion": snapshot_version,
  }
  command = ManagedDependencyCommand(name, envelope, parameters)
  envelope["immutableRequestHash"] = managed_dependency_request_hash(command)
  return {"name": name, "envelope": envelope, "parameters": parameters}


def verification_for(bundle, service):
  preparation = bundle.command_for(service)
  parameters = dict(preparation.parameters)
  if service == "HDFS":
    name = VERIFY_HDFS_CONSUMER
    operation_id = HDFS_VERIFY_OPERATION
    observation_id = HDFS_OBSERVATION
    observation_fingerprint = HDFS_OBSERVATION_HASH
    package_version = "3.3.0-1"
  else:
    name = VERIFY_ZOOKEEPER_CONSUMER
    operation_id = ZK_VERIFY_OPERATION
    observation_id = ZK_OBSERVATION
    observation_fingerprint = ZK_OBSERVATION_HASH
    package_version = "2.4.13-1"
  parameters.update(
    {
      "client.package.version": package_version,
      "preparation.observation.fingerprint": observation_fingerprint,
      "preparation.observation.id": observation_id,
      "preparation.request.hash": preparation.request_hash,
    }
  )
  envelope = {
    "bindingId": preparation.binding_id,
    "epoch": preparation.epoch,
    "immutableRequestHash": "sha256:" + "0" * 64,
    "operationId": operation_id,
    "protocolVersion": 1,
    "snapshotVersion": preparation.snapshot_version,
  }
  command = ManagedDependencyCommand(name, envelope, parameters)
  envelope["immutableRequestHash"] = managed_dependency_request_hash(command)
  return parse_managed_dependency_command(
    {"name": name, "envelope": envelope, "parameters": parameters}, name
  )


def command_with_parameters(command, updates):
  parameters = {**command.parameters, **updates}
  envelope = {**command.envelope, "immutableRequestHash": "sha256:" + "0" * 64}
  changed = ManagedDependencyCommand(command.name, envelope, parameters)
  envelope["immutableRequestHash"] = managed_dependency_request_hash(changed)
  return parse_managed_dependency_command(
    {"name": command.name, "envelope": envelope, "parameters": parameters},
    command.name,
  )


def hdfs_config(socket_timeout="60000", secure=False, user=None):
  core_site = {"fs.defaultFS": "hdfs://nn.example.test:8020"}
  hdfs_site = {"dfs.client.socket-timeout": socket_timeout}
  if secure:
    core_site.update(
      {
        "hadoop.security.authentication": "kerberos",
        "hadoop.security.auth_to_local": consumer_rules(user),
        "ipc.client.fallback-to-simple-auth-allowed": "false",
      }
    )
    hdfs_site.update(
      {
        "dfs.datanode.kerberos.principal": "dn/_HOST@EXAMPLE.COM",
        "dfs.namenode.kerberos.principal": "nn/_HOST@EXAMPLE.COM",
      }
    )
  return {
    "coreSite": core_site,
    "hdfsSite": hdfs_site,
    "zooKeeperClient": {},
  }


def consumer_rules(user):
  return "\n".join(
    (
      "RULE:[1:$1@$0](ambari-qa-plan@EXAMPLE.COM)s/.*/ambari-qa/",
      f"RULE:[1:$1@$0]({user}@EXAMPLE.COM)s/.*/{user}/",
      "RULE:[1:$1@$0](.*@EXAMPLE.COM)s/@.*//",
      f"RULE:[2:$1@$0]({user}@EXAMPLE.COM)s/.*/{user}/",
      "DEFAULT",
    )
  )


def zookeeper_config(secure=False):
  values = {
    "hbase.zookeeper.property.clientPort": "2181",
    "hbase.zookeeper.quorum": "zk1.example.test,zk2.example.test",
    "zookeeper.znode.parent": "/ambari-managed-hbase/" + ZK_BINDING + "/hbase",
  }
  if secure:
    values.update(
      {
        "zookeeper.sasl.client": "true",
        "zookeeper.sasl.client.username": "zookeeper",
        "zookeeper.sasl.clientconfig": "Client",
      }
    )
  return {
    "coreSite": {},
    "hdfsSite": {},
    "zooKeeperClient": values,
  }


def hbase_site(secure=False, user=None):
  result = {
    "hbase.rootdir": hdfs_uri(HDFS_BINDING, "root"),
    "hbase.wal.dir": hdfs_uri(HDFS_BINDING, "wal"),
    **zookeeper_config(secure)["zooKeeperClient"],
  }
  if secure:
    result["hbase.security.authentication"] = "kerberos"
    result["hbase.superuser"] = user
  return result


def hdfs_uri(binding_id, child):
  return (
    "hdfs://nn.example.test:8020/apps/ambari-managed/hbase/"
    + binding_id
    + "/"
    + child
  )


class _BlockingConfigStore(MODULE.ManagedHBaseClientConfigStore):
  def __init__(self, *args, entered, release, **kwargs):
    super().__init__(*args, **kwargs)
    self.entered = entered
    self.release = release

  def _ensure_profile(self, profile_path, expected_files):
    self.entered.set()
    if not self.release.wait(5):
      raise AssertionError("timed out waiting to release profile selection")
    return super()._ensure_profile(profile_path, expected_files)


def _select_bundle_in_process(
  profile_root,
  hbase_conf,
  group,
  bundle,
  started,
  release,
  finished,
  results,
):
  try:
    if release is None:
      store = MODULE.ManagedHBaseClientConfigStore(profile_root, hbase_conf, group)
      started.set()
    else:
      store = _BlockingConfigStore(
        profile_root,
        hbase_conf,
        group,
        entered=started,
        release=release,
      )
    store.select(bundle, hbase_site())
    results.put("SUCCEEDED")
  except BaseException as error:
    results.put(type(error).__name__ + ":" + str(error))
    raise
  finally:
    if finished is not None:
      finished.set()


def _configure_bundle_in_process(
  profile_root,
  hbase_conf,
  group,
  bundle,
  live_file,
  content,
  started,
  release,
  finished,
  results,
):
  try:
    store = MODULE.ManagedHBaseClientConfigStore(profile_root, hbase_conf, group)
    if release is None:
      started.set()
    with store.protect_configuration(bundle, hbase_site()):
      with open(live_file, "wb") as stream:
        stream.write(content)
        stream.flush()
        os.fsync(stream.fileno())
      if release is not None:
        started.set()
        if not release.wait(5):
          raise AssertionError("timed out waiting to release managed configuration")
      store.select_locked(bundle, hbase_site())
    results.put("SUCCEEDED")
  except BaseException as error:
    results.put(type(error).__name__ + ":" + str(error))
    raise
  finally:
    if finished is not None:
      finished.set()


def _verify_hdfs_in_process(
  params,
  profile_root,
  command,
  user,
  group,
  entered,
  release,
  results,
):
  try:
    verifier = MODULE.ManagedHBaseConsumerVerifier(
      params,
      profile_root=profile_root,
      call=_BlockingHdfs(user, group, entered, release),
      package_manager=FakePackageManager(),
    )
    verifier.verify_hdfs(command)
    results.put("SUCCEEDED")
  except BaseException as error:
    results.put(type(error).__name__ + ":" + str(error))
    raise


if __name__ == "__main__":
  unittest.main()
