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

import csv
import importlib.util
from pathlib import Path
import tempfile
import unittest


SCRIPT_PATH = Path(__file__).with_name("normalize_python_dependencies.py")
SPEC = importlib.util.spec_from_file_location("normalize_python_dependencies", SCRIPT_PATH)
normalizer = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(normalizer)


class NormalizePythonDependenciesTest(unittest.TestCase):
  def setUp(self):
    self.temporary_directory = tempfile.TemporaryDirectory()
    self.root = Path(self.temporary_directory.name)
    self.dist_info = self.root / "example-1.0.dist-info"
    self.dist_info.mkdir()
    (self.dist_info / "entry_points.txt").write_text(
      "[console_scripts]\nexample = example:main\n", encoding="utf-8"
    )
    self.bin_directory = self.root / "bin"
    self.bin_directory.mkdir()
    (self.bin_directory / "example").write_text("#!/usr/bin/python\n", encoding="utf-8")

  def tearDown(self):
    self.temporary_directory.cleanup()

  def _write_record(self, script_path="../../../bin/example"):
    with (self.dist_info / "RECORD").open("w", newline="", encoding="utf-8") as stream:
      csv.writer(stream).writerows(
        [
          [script_path, "sha256=script", "20"],
          ["example.py", "sha256=module", "12"],
          ["example-1.0.dist-info/RECORD", "", ""],
        ]
      )

  def test_declared_console_script_is_removed_from_tree_and_record(self):
    self._write_record()

    removed = normalizer.normalize(self.root)

    self.assertEqual(["example"], removed)
    self.assertFalse(self.bin_directory.exists())
    with (self.dist_info / "RECORD").open(newline="", encoding="utf-8") as stream:
      paths = [row[0] for row in csv.reader(stream)]
    self.assertEqual(
      ["example.py", "example-1.0.dist-info/RECORD"], paths
    )

  def test_root_relative_console_script_record_is_supported(self):
    self._write_record("bin/example")

    self.assertEqual(["example"], normalizer.normalize(self.root))
    self.assertFalse(self.bin_directory.exists())

  def test_undeclared_script_is_rejected_without_mutating_tree(self):
    self._write_record("../../../bin/not-declared")
    (self.bin_directory / "example").rename(self.bin_directory / "not-declared")

    with self.assertRaisesRegex(normalizer.NormalizationError, "undeclared"):
      normalizer.normalize(self.root)

    self.assertTrue((self.bin_directory / "not-declared").is_file())
    with (self.dist_info / "RECORD").open(encoding="utf-8") as stream:
      self.assertIn("../../../bin/not-declared", stream.read())

  def test_unowned_script_is_rejected_without_mutating_tree(self):
    self._write_record()
    (self.bin_directory / "unexpected").write_text("unexpected\n", encoding="utf-8")

    with self.assertRaisesRegex(normalizer.NormalizationError, "undeclared=unexpected"):
      normalizer.normalize(self.root)

    self.assertTrue((self.bin_directory / "example").is_file())
    self.assertTrue((self.bin_directory / "unexpected").is_file())


if __name__ == "__main__":
  unittest.main()
