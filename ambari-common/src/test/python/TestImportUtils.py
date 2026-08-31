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
import sys
import tempfile
from unittest import TestCase

from ambari_commons import import_utils


class TestImportUtils(TestCase):
  def test_load_source_registers_and_executes_module(self):
    module_name = "ambari_test_dynamic_module"
    with tempfile.NamedTemporaryFile(mode="w", suffix=".py", delete=False) as source:
      source.write("VALUE = 42\n")
      source_path = source.name

    try:
      module = import_utils.load_source(module_name, source_path)
      self.assertEqual(42, module.VALUE)
      self.assertIs(module, sys.modules[module_name])
    finally:
      sys.modules.pop(module_name, None)
      os.unlink(source_path)

  def test_new_module_creates_named_module(self):
    module = import_utils.new_module("ambari_test_empty_module")
    self.assertEqual("ambari_test_empty_module", module.__name__)

  def test_load_source_reuses_namespace_for_inheritance(self):
    module_name = "ambari_test_inherited_module"
    source_paths = []
    try:
      for content in ("class Parent: pass\n", "class Child(Parent): pass\n"):
        with tempfile.NamedTemporaryFile(
          mode="w", suffix=".py", delete=False
        ) as source:
          source.write(content)
          source_paths.append(source.name)

      parent_module = import_utils.load_source(module_name, source_paths[0])
      child_module = import_utils.load_source(module_name, source_paths[1])

      self.assertIs(parent_module, child_module)
      self.assertTrue(issubclass(child_module.Child, child_module.Parent))
    finally:
      sys.modules.pop(module_name, None)
      for source_path in source_paths:
        os.unlink(source_path)
