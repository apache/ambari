"""
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements. See the NOTICE file
distributed with this work for additional information
regarding copyright ownership. The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License. You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
"""

import copy
import hashlib
import json
import tempfile
import unittest
import uuid
from unittest.mock import Mock
from pathlib import Path

from ambari_mpack.builder import build, bundle, validate
from ambari_mpack.client import Client
from ambari_mpack.contract import ContractError, canonical, compare_versions, validate_manifest


def manifest():
    return {"schema_version": 1, "type": "full-release", "name": "nginx", "version": "1.0.0.0",
            "artifacts": [{"name": "service", "type": "extension-definitions", "source_dir": "extensions"}]}


def operation():
    return {"schema_version": 1, "id": str(uuid.uuid4()), "plan_id": str(uuid.uuid4()),
            "plan_digest": "a" * 64, "phase": "SUCCEEDED", "generation": 1,
            "effective_snapshot": "b" * 64, "hooks": {}, "hook_history": [], "error_code": None}


class ContractTests(unittest.TestCase):
    def test_wait_tracks_durable_cancellation_until_its_result(self):
        value = operation()
        value.update(phase="CANCELLING", effective_snapshot=None)
        completed = dict(value, phase="CANCELLED")
        client = object.__new__(Client)
        client.operation = Mock(return_value=completed)
        self.assertEqual("CANCELLED", client.wait(value, interval=0)["phase"])
        client.operation.assert_called_once_with(value["id"])

    def test_service_plan_rejects_foreign_selection_and_destination(self):
        client = object.__new__(Client)
        selected = "a" * 64
        result = {"schema_version": 1, "id": str(uuid.uuid4()), "digest": "b" * 64,
                  "deployment": {"cluster_id": 7, "service_ids": [selected], "service_names": ["EXAMPLE"]}}
        client.request = Mock(return_value=result)
        self.assertEqual(result, client.service_plan([selected], 7))
        result["deployment"]["service_ids"] = ["c" * 64]
        with self.assertRaises(ContractError):
            client.service_plan([selected], 7)
        result["deployment"]["service_ids"] = [selected]
        result["deployment"]["cluster_id"] = 8
        with self.assertRaises(ContractError):
            client.service_plan([selected], 7)

    def test_import_uses_every_bundle_member_without_binding_or_activation(self):
        from ambari_mpack.cli import parser, _run_mutation
        args = parser().parse_args(["import", "store.bundle.tar.gz", "--dry-run"])
        client = Mock()
        client.capabilities.return_value = {}
        client.upload.return_value = {"members": [{"archive_digest": "a" * 64}, {"archive_digest": "b" * 64}]}
        client.plan.side_effect = lambda mutation: {"schema_version": 1, "mutation": mutation}
        result = _run_mutation(client, args)
        self.assertEqual("IMPORT", result["mutation"]["action"])
        self.assertEqual(["a" * 64, "b" * 64], result["mutation"]["archive_digests"])
        self.assertFalse(result["mutation"]["activate"])
        self.assertEqual([], result["mutation"]["bindings"])
        client.submit.assert_not_called()

    def test_strict_schema_and_numeric_versions(self):
        self.assertEqual(1, compare_versions("1.10", "1.9"))
        self.assertEqual(0, compare_versions("1", "1.0.0"))
        self.assertEqual("nginx", validate_manifest(canonical(manifest()))["name"])
        for value in ("01", "1.2alpha", "1..2", "1.0\n"):
            with self.assertRaises(ContractError):
                compare_versions(value, "1")

    def test_duplicate_keys_noise_and_unknown_fields_are_not_results(self):
        for value in ('{"a":1,"a":2}', 'diagnostic noise\n{}', '{} {}'):
            with self.assertRaises(ContractError):
                validate_manifest(value)
        value = manifest()
        value["unexpected"] = True
        with self.assertRaises(ContractError):
            validate_manifest(canonical(value))

    def test_rejects_bool_schema_and_fractional_hook_timeout(self):
        value = manifest()
        value["schema_version"] = True
        with self.assertRaises(ContractError):
            validate_manifest(canonical(value))
        value["schema_version"] = 1
        value["hooks"] = [{"name": "after-install", "type": "python", "script": "hooks/run.py",
                           "timeout_seconds": 1.0, "idempotent": True}]
        with self.assertRaises(ContractError):
            validate_manifest(canonical(value))

    def test_rejects_traversal_overlap_and_self_dependencies(self):
        for source in ("../escape", "/escape", "x//y", "x/../y", "x\\y"):
            value = manifest()
            value["artifacts"][0]["source_dir"] = source
            with self.assertRaises(ContractError):
                validate_manifest(canonical(value))
        value = manifest()
        value["artifacts"].append({"name": "nested", "type": "service-definitions", "source_dir": "extensions/x"})
        with self.assertRaises(ContractError):
            validate_manifest(canonical(value))
        value = manifest()
        value["dependencies"] = [{"name": "nginx", "version": "1.0"}]
        with self.assertRaises(ContractError):
            validate_manifest(canonical(value))

    def test_success_requires_verified_snapshot_and_hook_lineage(self):
        value = operation()
        Client.validate_operation(value)
        del value["effective_snapshot"]
        with self.assertRaises(ContractError):
            Client.validate_operation(value)
        value = operation()
        value["hooks"] = {"x": {"schema_version": 1, "operation_id": str(uuid.uuid4()),
                               "plan_digest": value["plan_digest"], "state": "APPLIED",
                               "attempt": 1, "effect_state": "APPLIED", "observations": {"resource": "x"}}}
        with self.assertRaises(ContractError):
            Client.validate_operation(value)

    def test_success_rejects_unknown_effects_and_missing_attempts(self):
        value = operation()
        receipt = {"schema_version": 1, "operation_id": value["id"], "plan_digest": value["plan_digest"],
                   "state": "APPLIED", "attempt": 1, "effect_state": "UNKNOWN", "observations": {"x": 1}}
        value["hooks"] = {"x": receipt}
        with self.assertRaises(ContractError):
            Client.validate_operation(value)
        receipt["effect_state"] = "APPLIED"
        receipt.pop("attempt")
        with self.assertRaises(ContractError):
            Client.validate_operation(value)

    def test_recovery_states_are_not_reinterpreted_as_success(self):
        value = operation()
        value.update(phase="RECOVERY_REQUIRED", effective_snapshot=None, error_code="INVALID_RECEIPT")
        self.assertEqual("RECOVERY_REQUIRED", Client.validate_operation(value)["phase"])

    def test_result_for_foreign_operation_is_rejected(self):
        client = object.__new__(Client)
        client.request = lambda *_args, **_kwargs: operation()
        with self.assertRaises(ContractError):
            client.operation(str(uuid.uuid4()))

    def test_result_for_foreign_plan_is_rejected(self):
        client = object.__new__(Client)
        client.request = lambda *_args, **_kwargs: operation()
        with self.assertRaises(ContractError):
            client.submit({"id": str(uuid.uuid4()), "digest": "c" * 64}, "key")

    def test_cannot_put_credentials_in_server_url(self):
        with self.assertRaises(ContractError):
            Client("https://user:credential@localhost", "user", "password")


class BuilderTests(unittest.TestCase):
    def make_pack(self, root):
        pack = root / "pack"
        path = pack / "extensions" / "NGINX" / "1.0" / "services" / "NGINX" / "package"
        path.mkdir(parents=True)
        (path / "script.py").write_text("value = 1\n", encoding="utf-8")
        (pack / "mpack.json").write_text(canonical(manifest()), encoding="utf-8")
        return pack

    def test_build_is_deterministic_and_bundle_keeps_member_identity(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            pack = self.make_pack(root)
            first = build(pack, root / "dist-a")
            second = build(pack, root / "dist-b")
            self.assertEqual(first["digest"], second["digest"])
            result = bundle([first], root / "dist", "reference")
            self.assertEqual(result["digest"], hashlib.sha256(Path(result["path"]).read_bytes()).hexdigest())

    def test_builder_refuses_to_overwrite_different_release_content(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            pack = self.make_pack(root)
            build(pack, root / "dist")
            (pack / "extensions/NGINX/1.0/services/NGINX/package/script.py").write_text("value = 2\n")
            with self.assertRaises(ContractError):
                build(pack, root / "dist")

    def test_builder_rejects_file_link_outside_package(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            pack = self.make_pack(root)
            source = root / "outside"
            source.write_text("outside")
            (pack / "extensions/foreign").symlink_to(source)
            with self.assertRaises(ContractError):
                validate(pack)


if __name__ == "__main__":
    unittest.main()
