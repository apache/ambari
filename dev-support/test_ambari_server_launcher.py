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

import os
from pathlib import Path
import re
import shutil
import stat
import subprocess
import tempfile
import unittest


REPOSITORY = Path(__file__).resolve().parents[1]
LAUNCHER = REPOSITORY / "ambari-server/sbin/ambari-server"


class AmbariServerLauncherTest(unittest.TestCase):
  def setUp(self):
    self.temporary_directory = tempfile.TemporaryDirectory()
    self.base = Path(self.temporary_directory.name)

  def tearDown(self):
    self.temporary_directory.cleanup()

  def _write_executable(self, path, content):
    path.write_text(content, encoding="utf-8")
    path.chmod(path.stat().st_mode | stat.S_IXUSR)

  def _run(self, passphrase=None, existing_environment=None, fail_command=None):
    root = Path(tempfile.mkdtemp(dir=self.base))
    launcher = root / "ambari-server"
    source = LAUNCHER.read_text(encoding="utf-8")
    source = source.replace('VERSION="${ambariFullVersion}"', 'VERSION="test"')
    source = source.replace('HASH="${buildNumber}"', 'HASH="test"')
    source = source.replace(
      '\nROOT="/"\n', '\nROOT="${AMBARI_TEST_ROOT}"\n', 1
    )
    self._write_executable(launcher, source)

    library = root / "usr/lib/ambari-server/lib"
    library.mkdir(parents=True)
    (library / "_probe.cpython-39-x86_64-linux-gnu.so").touch()

    fake_bin = root / "fake-bin"
    fake_bin.mkdir()
    capture = root / "captured-passphrase"
    fake_python = fake_bin / "python"
    self._write_executable(
      fake_python,
      "#!/bin/sh\n"
      "if [ \"${1:-}\" = \"-c\" ]; then\n"
      "  case \"${2:-}\" in *'print('* ) echo 3.9.25 ;; esac\n"
      "  exit 0\n"
      "fi\n"
      "printf '%s' \"${AMBARI_PASSPHRASE:-}\" > \"$AMBARI_TEST_CAPTURE\"\n",
    )
    self._write_executable(fake_bin / "sudo", "#!/bin/sh\nexit 0\n")
    for command in ("chmod", "mkdir", "mktemp", "mv", "openssl", "sed"):
      executable = shutil.which(command)
      self.assertIsNotNone(executable)
      self._write_executable(
        fake_bin / command,
        "#!/bin/sh\n"
        f"if [ \"${{AMBARI_TEST_FAIL_COMMAND:-}}\" = \"{command}\" ]; then exit 1; fi\n"
        f'exec "{executable}" "$@"\n',
      )

    environment_path = root / "var/lib/ambari-server/ambari-env.sh"
    if existing_environment is not None:
      environment_path.parent.mkdir(parents=True)
      environment_path.write_text(existing_environment, encoding="utf-8")

    environment = os.environ.copy()
    environment.pop("AMBARI_PASSPHRASE", None)
    environment.update(
      {
        "AMBARI_TEST_CAPTURE": str(capture),
        "AMBARI_TEST_FAIL_COMMAND": fail_command or "",
        "AMBARI_TEST_ROOT": str(root),
        "PATH": f"{fake_bin}:{environment['PATH']}",
        "PYTHON": str(fake_python),
      }
    )
    if passphrase is not None:
      environment["AMBARI_PASSPHRASE"] = passphrase
    result = subprocess.run(
      ["bash", str(launcher), "status"],
      check=False,
      capture_output=True,
      env=environment,
      text=True,
    )
    return result, environment_path, capture

  def test_missing_secret_is_generated_persisted_and_used(self):
    result, environment_path, capture = self._run()

    self.assertEqual(0, result.returncode, result.stderr)
    match = re.search(
      r"^AMBARI_PASSPHRASE=([0-9a-f]{64})$",
      environment_path.read_text(encoding="utf-8"),
      re.MULTILINE,
    )
    self.assertIsNotNone(match)
    self.assertEqual(match.group(1), capture.read_text(encoding="utf-8"))
    self.assertEqual(0o600, stat.S_IMODE(environment_path.stat().st_mode))

  def test_explicit_secret_overrides_environment_file_without_rewriting_it(self):
    original = "KEEP=1\nAMBARI_PASSPHRASE=DEV\n"
    result, environment_path, capture = self._run(
      passphrase="explicit-secret", existing_environment=original
    )

    self.assertEqual(0, result.returncode, result.stderr)
    self.assertEqual(original, environment_path.read_text(encoding="utf-8"))
    self.assertEqual("explicit-secret", capture.read_text(encoding="utf-8"))
    self.assertEqual(0o600, stat.S_IMODE(environment_path.stat().st_mode))

  def test_secret_persistence_command_failures_abort_startup(self):
    for command in ("openssl", "mkdir", "mktemp", "sed", "chmod", "mv"):
      with self.subTest(command=command):
        existing = "AMBARI_PASSPHRASE=DEV\n" if command in {"sed", "chmod", "mv"} else None
        result, _, capture = self._run(
          existing_environment=existing, fail_command=command
        )
        self.assertNotEqual(0, result.returncode)
        self.assertFalse(capture.exists())


if __name__ == "__main__":
  unittest.main()
