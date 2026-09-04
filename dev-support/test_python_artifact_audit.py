#!/usr/bin/env python3

# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.

import base64
import csv
import hashlib
import importlib.util
from pathlib import Path
import struct
import tempfile
import unittest


SCRIPT_PATH = Path(__file__).with_name("python_artifact_audit.py")
SPEC = importlib.util.spec_from_file_location("python_artifact_audit", SCRIPT_PATH)
artifact_audit = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(artifact_audit)
VALID_HASH = "--hash=sha256:" + "a" * 64
DECLARED_LICENSES = {
  "cffi": "MIT",
  "distro": "Apache License, Version 2.0",
  "stomp.py": "Apache-2.0",
}


class PythonArtifactAuditTest(unittest.TestCase):
  def setUp(self):
    self.temporary_directory = tempfile.TemporaryDirectory()
    self.base = Path(self.temporary_directory.name)
    self.root = self.base / "dependencies"
    self.root.mkdir()
    self.lock = self.base / "requirements.lock"
    self.license = self.base / "LICENSE.txt"
    self.license.write_text(
      "For distro and stomp.py and docopt and cffi\n"
      "Apache License Version 2.0\n"
      "BSD 3-Clause License\n"
      "Permission is hereby granted, free of charge\n"
      "MIT No Attribution License\n"
      "Python Software Foundation License Version 2\n",
      encoding="utf-8",
    )

  def tearDown(self):
    self.temporary_directory.cleanup()

  def _write_distribution(
    self,
    name,
    version,
    files=None,
    requirements=None,
    wheel_tag="py3-none-any",
    declared_license=None,
  ):
    files = files or {f"{name.replace('-', '_')}.py": b"value = True\n"}
    requirements = requirements or []
    dist_info = self.root / f"{name.replace('-', '_')}-{version}.dist-info"
    dist_info.mkdir()
    metadata = [
      "Metadata-Version: 2.4",
      f"Name: {name}",
      f"Version: {version}",
      "License: " + (declared_license or DECLARED_LICENSES[name]),
    ]
    metadata.extend(f"Requires-Dist: {requirement}" for requirement in requirements)
    metadata_path = dist_info / "METADATA"
    metadata_path.write_text("\n".join(metadata) + "\n\n", encoding="utf-8")
    if name == "cffi":
      license_path = dist_info / "licenses/LICENSE"
      license_path.parent.mkdir()
      license_path.write_text("MIT No Attribution\n", encoding="utf-8")
    wheel_path = dist_info / "WHEEL"
    wheel_path.write_text(
      "Wheel-Version: 1.0\n"
      "Generator: ambari-test\n"
      "Root-Is-Purelib: true\n"
      f"Tag: {wheel_tag}\n\n",
      encoding="utf-8",
    )
    for relative, content in files.items():
      path = self.root / relative
      path.parent.mkdir(parents=True, exist_ok=True)
      path.write_bytes(content)

    record_path = dist_info / "RECORD"
    recorded = list(files) + [
      metadata_path.relative_to(self.root).as_posix(),
      wheel_path.relative_to(self.root).as_posix(),
    ]
    if name == "cffi":
      recorded.append(license_path.relative_to(self.root).as_posix())
    with record_path.open("w", newline="", encoding="utf-8") as stream:
      writer = csv.writer(stream)
      for relative in recorded:
        content = (self.root / relative).read_bytes()
        digest = base64.urlsafe_b64encode(hashlib.sha256(content).digest())
        writer.writerow(
          [relative, "sha256=" + digest.rstrip(b"=").decode("ascii"), len(content)]
        )
      writer.writerow([record_path.relative_to(self.root).as_posix(), "", ""])

  def _audit(self, manifests=None):
    return artifact_audit.audit(
      self.root,
      [self.lock],
      self.license,
      "manylinux2014_x86_64",
      "3.9.2",
      "cp39",
      "3.0.0-SNAPSHOT",
      manifests,
    )

  def test_valid_tree_generates_complete_sbom(self):
    self.lock.write_text(f"distro==1.9.0 {VALID_HASH}\n", encoding="utf-8")
    self._write_distribution(
      "distro",
      "1.9.0",
      requirements=['colorama; sys_platform == "win32"'],
    )

    sbom = self._audit()

    self.assertEqual("ambari-python-runtime-sbom-v1", sbom["format"])
    self.assertEqual(["distro"], [item["name"] for item in sbom["components"]])

  def test_missing_required_distribution_is_rejected(self):
    self.lock.write_text(f"stomp.py==8.2.0 {VALID_HASH}\n", encoding="utf-8")
    self._write_distribution(
      "stomp.py", "8.2.0", requirements=["docopt (>=0.6.2,<0.7.0)"]
    )

    with self.assertRaisesRegex(artifact_audit.AuditError, "missing distribution docopt"):
      self._audit()

  def test_recorded_upstream_test_content_is_accepted(self):
    self.lock.write_text(f"distro==1.9.0 {VALID_HASH}\n", encoding="utf-8")
    self._write_distribution(
      "distro", "1.9.0", files={"distro/tests/fixture.py": b"pass\n"}
    )

    self._audit()

  def test_precompiled_optimized_bytecode_is_rejected(self):
    self.lock.write_text(f"distro==1.9.0 {VALID_HASH}\n", encoding="utf-8")
    self._write_distribution(
      "distro", "1.9.0", files={"distro/module.pyo": b"bytecode"}
    )

    with self.assertRaisesRegex(
      artifact_audit.AuditError, r"Precompiled Python bytecode.*module\.pyo"
    ):
      self._audit()

  def test_unreviewed_declared_license_is_rejected(self):
    self.lock.write_text(f"distro==1.9.0 {VALID_HASH}\n", encoding="utf-8")
    self._write_distribution("distro", "1.9.0", declared_license="MIT")

    with self.assertRaisesRegex(
      artifact_audit.AuditError, "declares unexpected license"
    ):
      self._audit()

  def test_root_license_name_without_license_text_is_rejected(self):
    self.lock.write_text(f"distro==1.9.0 {VALID_HASH}\n", encoding="utf-8")
    self._write_distribution("distro", "1.9.0")
    self.license.write_text("For distro\n", encoding="utf-8")

    with self.assertRaisesRegex(
      artifact_audit.AuditError, "Root LICENSE lacks Apache-2.0 text"
    ):
      self._audit()

  def test_wrong_cpython_abi_is_rejected(self):
    self.lock.write_text(f"cffi==2.0.0 {VALID_HASH}\n", encoding="utf-8")
    elf_header = bytearray(20)
    elf_header[:6] = b"\x7fELF\x02\x01"
    elf_header[18:20] = struct.pack("<H", 62)
    self._write_distribution(
      "cffi",
      "2.0.0",
      files={"_cffi_backend.cpython-312-x86_64-linux-gnu.so": bytes(elf_header)},
    )

    with self.assertRaisesRegex(artifact_audit.AuditError, "expected cp39"):
      self._audit()

  def test_incompatible_wheel_tag_is_rejected(self):
    self.lock.write_text(f"distro==1.9.0 {VALID_HASH}\n", encoding="utf-8")
    self._write_distribution(
      "distro",
      "1.9.0",
      wheel_tag="cp312-cp312-manylinux2014_x86_64",
    )

    with self.assertRaisesRegex(artifact_audit.AuditError, "incompatible with cp39"):
      self._audit()

  def test_older_stable_abi_wheel_tag_is_accepted(self):
    self.lock.write_text(f"cffi==2.0.0 {VALID_HASH}\n", encoding="utf-8")
    self._write_distribution(
      "cffi",
      "2.0.0",
      wheel_tag="cp37-abi3-manylinux_2_17_x86_64",
    )

    self._audit()

  def test_versioned_shared_library_with_wrong_architecture_is_rejected(self):
    self.lock.write_text(f"cffi==2.0.0 {VALID_HASH}\n", encoding="utf-8")
    elf_header = bytearray(20)
    elf_header[:6] = b"\x7fELF\x02\x01"
    elf_header[18:20] = struct.pack("<H", 183)
    self._write_distribution(
      "cffi", "2.0.0", files={"cffi.libs/libcrypto.so.3": bytes(elf_header)}
    )

    with self.assertRaisesRegex(artifact_audit.AuditError, "ELF machine 183"):
      self._audit()

  def test_duplicate_record_ownership_is_rejected(self):
    self.lock.write_text(
      f"distro==1.9.0 {VALID_HASH}\n"
      f"cffi==2.0.0 {VALID_HASH}\n",
      encoding="utf-8",
    )
    self._write_distribution("distro", "1.9.0", files={"shared.py": b"pass\n"})
    self._write_distribution("cffi", "2.0.0", files={"shared.py": b"pass\n"})

    with self.assertRaisesRegex(artifact_audit.AuditError, "multiple owners"):
      self._audit()

  def test_record_tampering_is_rejected(self):
    self.lock.write_text(f"distro==1.9.0 {VALID_HASH}\n", encoding="utf-8")
    self._write_distribution("distro", "1.9.0")
    (self.root / "distro.py").write_text("tampered = True\n", encoding="utf-8")

    with self.assertRaisesRegex(artifact_audit.AuditError, "RECORD (size|digest) mismatch"):
      self._audit()

  def test_lock_entry_without_hash_is_rejected(self):
    self.lock.write_text("distro==1.9.0\n", encoding="utf-8")
    self._write_distribution("distro", "1.9.0")

    with self.assertRaisesRegex(artifact_audit.AuditError, "no SHA-256 hash"):
      self._audit()

  def test_lock_entry_with_malformed_hash_is_rejected(self):
    self.lock.write_text(
      "distro==1.9.0 --hash=sha256:abc\n", encoding="utf-8"
    )
    self._write_distribution("distro", "1.9.0")

    with self.assertRaisesRegex(artifact_audit.AuditError, "no SHA-256 hash"):
      self._audit()

  def test_unpinned_lock_entry_is_rejected(self):
    self.lock.write_text(
      "distro>=1.9.0 " + "--hash=sha256:" + "a" * 64 + "\n",
      encoding="utf-8",
    )
    self._write_distribution("distro", "1.9.0")

    with self.assertRaisesRegex(artifact_audit.AuditError, "not pinned"):
      self._audit()

  def test_lock_marker_is_not_part_of_version(self):
    self.lock.write_text(
      "distro==1.9.0; python_version >= '3.9' \\\n"
      "    --hash=sha256:" + "a" * 64 + "\n",
      encoding="utf-8",
    )
    self._write_distribution("distro", "1.9.0")

    self._audit()

  def test_dangling_lock_continuation_is_rejected(self):
    self.lock.write_text("distro==1.9.0 \\\n", encoding="utf-8")
    self._write_distribution("distro", "1.9.0")

    with self.assertRaisesRegex(artifact_audit.AuditError, "Dangling"):
      self._audit()

  def test_unreachable_locked_distribution_is_rejected(self):
    self.lock.write_text(
      f"distro==1.9.0 {VALID_HASH}\n"
      f"cffi==2.0.0 {VALID_HASH}\n",
      encoding="utf-8",
    )
    self._write_distribution("distro", "1.9.0")
    self._write_distribution("cffi", "2.0.0")
    manifest = self.base / "requirements.in"
    manifest.write_text("distro==1.9.0\n", encoding="utf-8")

    with self.assertRaisesRegex(artifact_audit.AuditError, "not reachable.*cffi"):
      self._audit([manifest])

  def test_manifest_version_must_match_lock(self):
    self.lock.write_text(f"distro==1.9.0 {VALID_HASH}\n", encoding="utf-8")
    self._write_distribution("distro", "1.9.0")
    manifest = self.base / "requirements.in"
    manifest.write_text("distro==2.0.0\n", encoding="utf-8")

    with self.assertRaisesRegex(artifact_audit.AuditError, "differ from locks"):
      self._audit([manifest])


if __name__ == "__main__":
  unittest.main()
