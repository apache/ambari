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

import ast
from pathlib import Path
import re
import unittest


RESOURCES = Path(__file__).resolve().parents[2] / "main/resources"
SERVICE_ROOTS = (
  RESOURCES / "stacks/BIGTOP/3.2.0/services",
  RESOURCES / "stacks/BIGTOP/3.3.0/services/RANGER",
  RESOURCES / "stacks/BIGTOP/3.3.0/services/RANGER_KMS",
  RESOURCES / "common-services/AMBARI_INFRA_SOLR/3.0.0",
)
DIRECT_PROCESS_APIS = re.compile(
  r"\b(?:subprocess\.(?:Popen|run|call|check_call|check_output)|"
  r"os\.(?:system|popen|kill)|signal\.kill)\s*\("
)
OLD_PID_HELPERS = (
  "create_pid_file_for_identity",
  "secure_pid_file_for_identity",
)


def product_python_files():
  for root in SERVICE_ROOTS:
    yield from sorted(root.rglob("*.py"))


def call_name(node):
  if isinstance(node, ast.Name):
    return node.id
  if isinstance(node, ast.Attribute):
    prefix = call_name(node.value)
    return f"{prefix}.{node.attr}" if prefix else node.attr
  return ""


def is_false_constant(node):
  return isinstance(node, ast.Constant) and node.value is False


def is_process_group_strategy(node):
  return isinstance(node, ast.Attribute) and node.attr == "KILL_PROCESS_GROUP"


class TestBigtopServiceProcessContracts(unittest.TestCase):
  def test_product_python_is_parseable_and_avoids_direct_process_apis(self):
    direct_calls = []
    legacy_helpers = []
    for path in product_python_files():
      source = path.read_text(encoding="utf-8")
      ast.parse(source, filename=str(path))
      for match in DIRECT_PROCESS_APIS.finditer(source):
        direct_calls.append(f"{path.relative_to(RESOURCES)}:{match.start()}")
      for helper in OLD_PID_HELPERS:
        if helper in source:
          legacy_helpers.append(f"{path.relative_to(RESOURCES)}:{helper}")

    self.assertEqual([], direct_calls)
    self.assertEqual([], legacy_helpers)

  def test_synchronous_process_calls_are_bounded_by_process_group_timeout(self):
    violations = []
    for path in product_python_files():
      tree = ast.parse(path.read_text(encoding="utf-8"), filename=str(path))
      for node in ast.walk(tree):
        if not isinstance(node, ast.Call):
          continue
        name = call_name(node.func)
        if name not in (
          "Execute",
          "shell.call",
          "shell.checked_call",
          "get_user_call_output",
        ):
          continue
        keywords = {
          keyword.arg: keyword.value
          for keyword in node.keywords
          if keyword.arg is not None
        }
        if name == "Execute" and is_false_constant(
          keywords.get("wait_for_finish")
        ):
          continue
        if "timeout" not in keywords or not is_process_group_strategy(
          keywords.get("timeout_kill_strategy")
        ):
          violations.append(
            f"{path.relative_to(RESOURCES)}:{node.lineno}:{name}"
          )

    self.assertEqual([], violations)


if __name__ == "__main__":
  unittest.main()
