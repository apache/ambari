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
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

import ast
from pathlib import Path
from unittest import TestCase


REPOSITORY_ROOT = Path(__file__).resolve().parents[1]
MIGRATED_CALLERS = (
  "ambari-agent/src/main/python/ambari_agent/CustomServiceOrchestrator.py",
  "ambari-common/src/main/python/ambari_commons/credential_store_helper.py",
  "ambari-server/src/main/resources/stacks/BIGTOP/3.3.0/services/RANGER/"
  "package/scripts/setup_ranger_xml.py",
  "ambari-server/src/main/resources/stacks/BIGTOP/3.3.0/services/RANGER_KMS/"
  "package/scripts/kms.py",
)


class TestCredentialArgumentSafety(TestCase):
  def test_all_credential_store_callers_use_stdin_helper(self):
    for relative_path in MIGRATED_CALLERS:
      tree = ast.parse((REPOSITORY_ROOT / relative_path).read_text("utf-8"))
      string_literals = {
        node.value
        for node in ast.walk(tree)
        if isinstance(node, ast.Constant) and isinstance(node.value, str)
      }
      helper_calls = [
        node
        for node in ast.walk(tree)
        if isinstance(node, ast.Call)
        and isinstance(node.func, ast.Name)
        and node.func.id
        in {
          "create_credential_store_entry",
          "create_password_in_credential_store",
        }
      ]
      self.assertNotIn("-value", string_literals, relative_path)
      self.assertTrue(helper_calls, relative_path)

  def test_java17_helper_callers_use_ambari_java_home(self):
    caller_paths = (
      "ambari-agent/src/main/python/ambari_agent/CustomServiceOrchestrator.py",
      "ambari-server/src/main/resources/stacks/BIGTOP/3.3.0/services/RANGER/"
      "package/scripts/setup_ranger_xml.py",
      "ambari-server/src/main/resources/stacks/BIGTOP/3.3.0/services/RANGER_KMS/"
      "package/scripts/kms.py",
    )
    for relative_path in caller_paths:
      source = (REPOSITORY_ROOT / relative_path).read_text(encoding="utf-8")
      self.assertIn("ambari_java_home", source, relative_path)
