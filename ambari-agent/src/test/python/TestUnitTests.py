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

import logging
import os
import sys
import tempfile
from unittest import TestCase

import unitTests


class TestUnitTests(TestCase):
  def test_discovery_isolates_duplicate_module_names(self):
    original_cwd = os.getcwd()
    original_logger = getattr(unitTests, "logger", None)
    original_sys_path = sys.path[:]
    try:
      with tempfile.TemporaryDirectory() as test_root:
        first_dir = os.path.join(test_root, "first")
        second_dir = os.path.join(test_root, "second")
        os.makedirs(first_dir)
        os.makedirs(second_dir)
        self._write_test(first_dir, "test_first")
        self._write_test(second_dir, "test_second")

        os.chdir(test_root)
        unitTests.logger = logging.getLogger(__name__)
        suite = unitTests.all_tests_suite(None)

      test_ids = sorted(test.id() for test in unitTests.iter_tests(suite))
      self.assertEqual(
        [
          "TestDuplicate.Case.test_first",
          "TestDuplicate.Case.test_second",
        ],
        test_ids,
      )
    finally:
      os.chdir(original_cwd)
      sys.path[:] = original_sys_path
      if original_logger is None:
        del unitTests.logger
      else:
        unitTests.logger = original_logger

  @staticmethod
  def _write_test(directory, method_name):
    with open(os.path.join(directory, "TestDuplicate.py"), "w") as test_file:
      test_file.write(
        "from unittest import TestCase\n\n"
        "class Case(TestCase):\n"
        f"  def {method_name}(self):\n"
        "    pass\n"
      )
