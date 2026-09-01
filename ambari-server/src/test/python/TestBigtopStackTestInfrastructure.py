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

import json
from pathlib import Path
from unittest import TestCase

from stacks.utils.RMFTestCase import RMFTestCase
from unitTests import discover_tests, get_base_test_directories, get_stack_name


SERVER_ROOT = Path(__file__).resolve().parents[2]
BIGTOP_PROPERTIES = SERVER_ROOT / "main/resources/stacks/BIGTOP/3.2.0/properties"
STACK_CONFIGS = SERVER_ROOT / "test/python/stacks/configs"
STACK_HOOKS = SERVER_ROOT / "main/resources/stack-hooks"


class TestBigtopStackTestInfrastructure(TestCase):
  def test_stack_root_tests_are_discovered(self):
    test_directories = {
      Path(path): message
      for path, message in get_base_test_directories(
        SERVER_ROOT.parent.parent / "ambari-common", SERVER_ROOT / "test/python"
      )
    }
    stack_tests = SERVER_ROOT / "test/python/stacks"

    self.assertIn(stack_tests, test_directories)
    suite = discover_tests(str(stack_tests), "[Tt]est*.py", recursive=False)
    self.assertGreater(suite.countTestCases(), 0)

  def test_runner_and_stack_properties_use_bigtop(self):
    self.assertEqual("BIGTOP", get_stack_name())

    for name, reader in (
      ("stack_tools.json", RMFTestCase.get_stack_tools),
      ("stack_features.json", RMFTestCase.get_stack_features),
      ("stack_packages.json", RMFTestCase.get_stack_packages),
    ):
      expected = (BIGTOP_PROPERTIES / name).read_text(encoding="utf-8")
      self.assertEqual(expected, reader())
      self.assertFalse((STACK_CONFIGS / name).exists())

  def test_stack_hook_fixtures_only_reference_bigtop_repositories(self):
    for name in ("default.json", "repository_file.json", "secured.json"):
      payload = json.loads((STACK_CONFIGS / name).read_text(encoding="utf-8"))
      self.assertEqual("BIGTOP", payload["clusterLevelParams"]["stack_name"])
      self.assertEqual("3.3.0", payload["clusterLevelParams"]["stack_version"])

      repository_files = [payload["repositoryFile"]]
      repository_files.extend(
        payload["hostLevelParams"]["hostRepositories"]["commandRepos"].values()
      )
      for repository_file in repository_files:
        self.assertEqual("BIGTOP", repository_file["stackName"])
        self.assertEqual("3.3.0", repository_file["repoVersion"])
        self.assertEqual(1, len(repository_file["repositories"]))
        repository = repository_file["repositories"][0]
        self.assertEqual("BIGTOP-3.3.0", repository["repoId"])
        self.assertTrue(repository["baseUrl"].startswith("https://"))

  def test_stack_hook_sources_and_fixtures_do_not_reference_hdp(self):
    files = list(STACK_CONFIGS.glob("*.json"))
    files.extend(STACK_HOOKS.rglob("*.py"))
    for path in files:
      self.assertNotIn("hdp", path.read_text(encoding="utf-8").lower(), path)
