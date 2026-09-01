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

import importlib.util
import json
from pathlib import Path
import tempfile
from unittest import TestCase
from unittest.mock import MagicMock, patch
from xml.etree import ElementTree


MODULE_PATH = (
  Path(__file__).resolve().parents[2]
  / "main/python/urlinfo_processor/urlinfo_processor.py"
)
MODULE_SPEC = importlib.util.spec_from_file_location("urlinfo_processor", MODULE_PATH)
urlinfo_processor = importlib.util.module_from_spec(MODULE_SPEC)
MODULE_SPEC.loader.exec_module(urlinfo_processor)


class TestUrlInfoProcessor(TestCase):
  def test_replace_urls_writes_valid_python3_text_xml(self):
    with tempfile.TemporaryDirectory() as directory:
      root = Path(directory)
      repository_directory = root / "3.2/repos"
      repository_directory.mkdir(parents=True)
      repository_file = repository_directory / "repoinfo.xml"
      repository_file.write_text(
        """<reposinfo><os family="redhat7"><repo><repoid>BIGTOP-3.2</repoid>
<baseurl>https://old.example/repo</baseurl></repo></os></reposinfo>""",
        encoding="utf-8",
      )
      urlinfo_file = root / "urlinfo.json"
      urlinfo_file.write_text(
        json.dumps(
          {
            "BIGTOP-3.2": {
              "latest": {"centos7": "https://new.example/repo"}
            }
          }
        ),
        encoding="utf-8",
      )

      urlinfo_processor.replace_urls(str(root), str(urlinfo_file))

      tree = ElementTree.parse(repository_file)
      self.assertEqual(
        "https://new.example/repo", tree.findtext("./os/repo/baseurl")
      )
      self.assertTrue(
        repository_file.read_text(encoding="utf-8").startswith("<?xml version=")
      )

  @patch("urllib.request.urlopen")
  def test_remote_urlinfo_uses_bounded_request_and_utf8_json(self, urlopen_mock):
    response = MagicMock()
    response.read.return_value = b'{"BIGTOP-3.2": {"latest": {}}}'
    urlopen_mock.return_value.__enter__.return_value = response

    result = urlinfo_processor.get_json_content(
      "https://repository.example/urlinfo.json"
    )

    self.assertIn("BIGTOP-3.2", result)
    urlopen_mock.assert_called_once_with(
      "https://repository.example/urlinfo.json",
      timeout=urlinfo_processor.URL_OPEN_TIMEOUT_SECONDS,
    )

  def test_cli_requires_both_paths(self):
    with self.assertRaises(SystemExit) as exception:
      urlinfo_processor.main([])

    self.assertEqual(2, exception.exception.code)
