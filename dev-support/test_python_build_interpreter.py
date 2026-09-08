#!/usr/bin/env python3

# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

import os
from pathlib import Path
import subprocess
import tempfile
import unittest


LAUNCHER = Path(__file__).with_name("ambari-python-build")
INSTALLER = LAUNCHER.parents[1] / "install-ambari-python.sh"


class PythonBuildInterpreterTest(unittest.TestCase):
  @staticmethod
  def _write_candidate(path, supported):
    path.write_text(
      "#!/bin/sh\n"
      "if [ \"$1\" = -c ]; then\n"
      f"  exit {0 if supported else 1}\n"
      "fi\n"
      "printf '%s\\n' \"$*\"\n",
      encoding="utf-8",
    )
    path.chmod(0o755)

  def test_explicit_supported_interpreter_is_used(self):
    with tempfile.TemporaryDirectory() as directory:
      candidate = Path(directory) / "custom-python"
      self._write_candidate(candidate, supported=True)
      environment = os.environ.copy()
      environment["AMBARI_BUILD_PYTHON"] = str(candidate)

      result = subprocess.run(
        [str(LAUNCHER), "-m", "build"],
        check=False,
        capture_output=True,
        text=True,
        env=environment,
      )

    self.assertEqual(0, result.returncode, result.stderr)
    self.assertEqual("-m build\n", result.stdout)

  def test_unsupported_interpreters_fail_with_override_guidance(self):
    with tempfile.TemporaryDirectory() as directory:
      path = Path(directory)
      (path / "bash").symlink_to("/bin/bash")
      self._write_candidate(path / "python3.10", supported=False)
      environment = {
        "AMBARI_BUILD_PYTHON": str(path / "python3.10"),
        "PATH": directory,
      }

      result = subprocess.run(
        [str(LAUNCHER), "-m", "build"],
        check=False,
        capture_output=True,
        text=True,
        env=environment,
      )

    self.assertEqual(1, result.returncode)
    self.assertIn("CPython 3.10+", result.stderr)
    self.assertIn("AMBARI_BUILD_PYTHON", result.stderr)

  def test_runtime_venv_does_not_require_global_pip(self):
    with tempfile.TemporaryDirectory() as directory:
      path = Path(directory)
      interpreter = path / "runtime-python"
      interpreter.write_text(
        "#!/bin/sh\n"
        "if [ \"$1\" = -m ] && [ \"$2\" = pip ]; then exit 88; fi\n"
        f"exec {os.fsdecode(os.fsencode(os.sys.executable))!r} \"$@\"\n",
        encoding="utf-8",
      )
      interpreter.chmod(0o755)
      runtime_venv = path / "runtime-venv"
      script = (
        f"source {str(INSTALLER)!r}\n"
        f"AMBARI_PYTHON={str(interpreter)!r}\n"
        f"RUNTIME_VENV={str(runtime_venv)!r}\n"
        'RUNTIME_PYTHON="$RUNTIME_VENV/bin/python"\n'
        "prepare_runtime_environment\n"
        '"$RUNTIME_PYTHON" -m pip --version\n'
      )

      global_pip = subprocess.run(
        [str(interpreter), "-m", "pip", "--version"], check=False
      )
      result = subprocess.run(
        ["bash", "-c", script],
        check=False,
        capture_output=True,
        text=True,
      )

    self.assertEqual(88, global_pip.returncode)
    self.assertEqual(0, result.returncode, result.stderr)
    self.assertIn("pip ", result.stdout)

  def test_wheel_install_uses_runtime_venv(self):
    with tempfile.TemporaryDirectory() as directory:
      path = Path(directory)
      (path / "dist").mkdir()
      (path / "dist/ambari_python-1-py3-none-any.whl").touch()
      record = path / "runtime-arguments"
      runtime_python = path / "runtime-python"
      runtime_python.write_text(
        f"#!/bin/sh\nprintf '%s\\n' \"$*\" > {str(record)!r}\n",
        encoding="utf-8",
      )
      runtime_python.chmod(0o755)
      script = (
        f"source {str(INSTALLER)!r}\n"
        f"SCRIPT_DIR={str(path)!r}\n"
        f"RUNTIME_PYTHON={str(runtime_python)!r}\n"
        "generate_site_packages ambari_python-1-py3-none-any.whl\n"
      )

      result = subprocess.run(
        ["bash", "-c", script],
        check=False,
        capture_output=True,
        text=True,
      )

      arguments = record.read_text(encoding="utf-8")

    self.assertEqual(0, result.returncode, result.stderr)
    self.assertIn("-m pip install", arguments)
    self.assertIn("ambari_python-1-py3-none-any.whl", arguments)

  def test_clean_removes_stale_distribution_wheels(self):
    with tempfile.TemporaryDirectory() as directory:
      path = Path(directory)
      dist = path / "dist"
      dist.mkdir()
      (dist / "ambari_python-stale-py3-none-any.whl").touch()
      script = (
        f"source {str(INSTALLER)!r}\n"
        f"SCRIPT_DIR={str(path)!r}\n"
        f"BUILD_VENV={str(path / 'build-venv')!r}\n"
        f"RUNTIME_VENV={str(path / 'runtime-venv')!r}\n"
        "clean\n"
      )

      result = subprocess.run(
        ["bash", "-c", script],
        check=False,
        capture_output=True,
        text=True,
      )

      exists_after_clean = dist.exists()

    self.assertEqual(0, result.returncode, result.stderr)
    self.assertFalse(exists_after_clean)


if __name__ == "__main__":
  unittest.main()
