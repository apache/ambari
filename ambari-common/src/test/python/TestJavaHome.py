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

import os
import tempfile
import unittest
from unittest.mock import patch

from resource_management.core.exceptions import Fail
from resource_management.libraries.functions.java_home import resolve_java_home


class TestJavaHome(unittest.TestCase):
  def test_service_override_is_validated_and_selected(self):
    with tempfile.TemporaryDirectory() as home:
      os.makedirs(os.path.join(home, "bin"))
      open(os.path.join(home, "bin", "java"), "w", encoding="ascii").close()
      with patch(
        "resource_management.libraries.functions.java_home.default",
        return_value='{"HIVE": {"home": "' + home + '", "version": 8}}',
      ):
        self.assertEqual(home, resolve_java_home("HIVE", default_home="/java17"))

  def test_invalid_override_fails_closed(self):
    with patch(
      "resource_management.libraries.functions.java_home.default",
      return_value='{"HIVE": {"home": "/tmp/../java8"}}',
    ):
      with self.assertRaises(Fail):
        resolve_java_home("HIVE", default_home="/java17")

  def test_missing_override_keeps_default(self):
    with patch(
      "resource_management.libraries.functions.java_home.default",
      return_value="{}",
    ):
      self.assertEqual("/java17", resolve_java_home("HIVE", default_home="/java17"))
