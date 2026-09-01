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

from pathlib import Path
import os
import shutil
import subprocess
import sys
import tarfile
import tempfile
import unittest


REPOSITORY = Path(__file__).resolve().parents[1]


class PythonSourceDistributionTest(unittest.TestCase):
  def _copy(self, source, destination):
    if source.is_dir():
      shutil.copytree(
        source,
        destination,
        ignore=shutil.ignore_patterns("__pycache__", "*.egg-info", "target"),
      )
    else:
      shutil.copy2(source, destination)

  def _build_sdist(self, source, destination):
    environment = os.environ.copy()
    environment["AMBARI_VERSION"] = "3.1.0.0.dev0"
    result = subprocess.run(
      [
        sys.executable,
        "-m",
        "build",
        "--sdist",
        "--no-isolation",
        "--outdir",
        str(destination),
        str(source),
      ],
      check=False,
      stdout=subprocess.PIPE,
      stderr=subprocess.PIPE,
      text=True,
      env=environment,
    )
    output = result.stdout + result.stderr
    self.assertEqual(0, result.returncode, output)
    self.assertNotIn("warning", output.lower(), output)

  def test_sdist_contains_runtime_data_without_legacy_vendor_artifacts(self):
    with tempfile.TemporaryDirectory() as temporary_directory:
      temporary_path = Path(temporary_directory)
      source = temporary_path / "source"
      source.mkdir()
      for relative in (
        "LICENSE.txt",
        "MANIFEST.in",
        "README.md",
        "pyproject.toml",
        "setup.py",
      ):
        self._copy(REPOSITORY / relative, source / relative)
      self._copy(REPOSITORY / "ambari-common", source / "ambari-common")
      destination = temporary_path / "dist"
      destination.mkdir()

      self._build_sdist(source, destination)

      archives = tuple(destination.glob("ambari_python-*.tar.gz"))
      self.assertEqual(1, len(archives))
      with tarfile.open(archives[0], "r:gz") as archive:
        members = {
          name.split("/", 1)[1]
          for name in archive.getnames()
          if "/" in name
        }

    required = {
      "ambari-common/src/main/python/ambari_commons/resources/os_family.json",
      "ambari-common/src/main/python/resource_management/core/files/killtree.sh",
      "ambari-common/src/main/python/pluggable_stack_definition/configs/ODP.json",
      "ambari-common/src/main/python/pluggable_stack_definition/resources/ODP/custom_stack_map.js",
      "ambari-common/src/main/python/pluggable_stack_definition/resources/PHD/custom-ui.less",
    }
    self.assertTrue(required.issubset(members), sorted(required - members))
    forbidden_fragments = (
      "/ambari_commons/libs/",
      "/ambari_jinja2/",
      "/ambari_simplejson/",
      "/ambari_stomp/",
      "/ambari_ws4py/",
      "/ambari_pbkdf2/",
      "/ambari_pyaes/",
    )
    self.assertFalse(
      any(fragment in f"/{name}" for name in members for fragment in forbidden_fragments)
    )

  def test_agent_sdist_uses_complete_pep517_metadata(self):
    with tempfile.TemporaryDirectory() as temporary_directory:
      temporary_path = Path(temporary_directory)
      source = temporary_path / "source"
      self._copy(REPOSITORY / "ambari-agent/src/main/python", source)
      destination = temporary_path / "dist"
      destination.mkdir()

      self._build_sdist(source, destination)

      archives = tuple(destination.glob("ambari_agent-*.tar.gz"))
      self.assertEqual(1, len(archives))
      with tarfile.open(archives[0], "r:gz") as archive:
        pkg_info = next(
          member for member in archive.getmembers() if member.name.endswith("/PKG-INFO")
        )
        metadata = archive.extractfile(pkg_info).read().decode("utf-8")

    self.assertIn("Requires-Python: >=3.9.2", metadata)
    for requirement in (
      "APScheduler==3.11.3",
      "cryptography==50.0.1",
      "distro==1.9.0",
      "Jinja2==3.1.6",
      "stomp.py==8.2.0",
      "websocket-client==1.9.0",
    ):
      self.assertIn(f"Requires-Dist: {requirement}", metadata)


if __name__ == "__main__":
  unittest.main()
