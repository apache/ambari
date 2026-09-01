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
import subprocess
import tempfile
import unittest
from pathlib import Path


PROJECT_ROOT = Path(__file__).resolve().parents[1]


class InstallHelperCleanupTest(unittest.TestCase):
  @staticmethod
  def _shell_function(path, name):
    lines = Path(path).read_text(encoding="utf-8").splitlines()
    start = next(
      index
      for index, line in enumerate(lines)
      if line.replace(" ", "") == f"{name}(){{"
    )
    end = next(index for index in range(start + 1, len(lines)) if lines[index] == "}")
    return "\n".join(lines[start : end + 1])

  def _verify_cleanup(self, component, interpreter):
    helper = PROJECT_ROOT / component / "conf/unix/install-helper.sh"
    unit = component

    with tempfile.TemporaryDirectory() as directory:
      root = Path(directory)
      dependency = root / "usr/lib" / unit / "lib/official_dependency"
      first_party = root / "usr/lib" / unit / "lib/ambari_source"
      dependency_cache = dependency / "__pycache__"
      first_party_cache = first_party / "__pycache__"
      dependency_cache.mkdir(parents=True)
      first_party_cache.mkdir(parents=True)
      dependency_bytecode = dependency_cache / "module.cpython-39.pyc"
      first_party_bytecode = first_party_cache / "module.cpython-39.pyo"
      dependency_source = dependency / "module.py"
      dependency_bytecode.write_bytes(b"bytecode")
      first_party_bytecode.write_bytes(b"bytecode")
      dependency_source.write_text("VALUE = 1\n", encoding="utf-8")

      environment = os.environ.copy()
      environment["RPM_INSTALL_PREFIX"] = directory
      result = subprocess.run(
        [interpreter, str(helper), "cleanup"],
        check=False,
        env=environment,
        stdout=subprocess.PIPE,
        stderr=subprocess.PIPE,
        text=True,
      )

      self.assertEqual(result.returncode, 0, result.stderr)
      self.assertFalse(dependency_bytecode.exists())
      self.assertFalse(first_party_bytecode.exists())
      self.assertTrue(dependency_source.is_file())

  def test_agent_cleans_bytecode_from_the_complete_library(self):
    self._verify_cleanup("ambari-agent", "sh")

  def test_server_cleans_bytecode_from_the_complete_library(self):
    self._verify_cleanup("ambari-server", "bash")

  def test_install_helpers_remove_only_named_obsolete_python_sources(self):
    for component, root_variable in (
      ("ambari-agent", "AMBARI_AGENT_ROOT_DIR"),
      ("ambari-server", "AMBARI_SERVER_ROOT_DIR"),
    ):
      with self.subTest(component=component), tempfile.TemporaryDirectory() as directory:
        component_root = Path(directory) / "usr/lib" / component
        library = component_root / "lib"
        obsolete = library / "ambari_jinja2"
        first_party = library / (
          "ambari_agent" if component == "ambari-agent" else "ambari_server"
        )
        upstream_docs = library / "official_dependency/docs"
        obsolete.mkdir(parents=True)
        first_party.mkdir(parents=True)
        upstream_docs.mkdir(parents=True)
        (obsolete / "legacy.py").write_text("legacy = True\n", encoding="utf-8")
        (first_party / "main.py").write_text("current = True\n", encoding="utf-8")
        (upstream_docs / "index.txt").write_text("docs\n", encoding="utf-8")

        helper = PROJECT_ROOT / component / "conf/unix/install-helper.sh"
        function = self._shell_function(helper, "clean_obsolete_python_sources")
        obsolete_paths = next(
          line
          for line in helper.read_text(encoding="utf-8").splitlines()
          if line.startswith("OBSOLETE_PYTHON_PATHS=")
        )
        result = subprocess.run(
          [
            "sh",
            "-c",
            f"{function}\n{root_variable}=\"$1\"; LOG_FILE=/dev/null; "
            f"{obsolete_paths}; clean_obsolete_python_sources",
            "sh",
            str(component_root),
          ],
          check=False,
          capture_output=True,
          text=True,
        )

        self.assertEqual(0, result.returncode, result.stderr)
        self.assertFalse(obsolete.exists())
        self.assertTrue((first_party / "main.py").is_file())
        self.assertTrue((upstream_docs / "index.txt").is_file())


if __name__ == "__main__":
  unittest.main()
