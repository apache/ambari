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
from unittest import TestCase

from takeover_config_merge import XmlParser, YamlParser


class TestTakeoverConfigMerge(TestCase):
  def test_yaml_parser_accepts_mapping(self):
    path = self._write_file("enabled: true\nworkers: 3\n")
    try:
      configurations, attributes = YamlParser().read_data_to_map(path)
      self.assertEqual(configurations, {"enabled": "True", "workers": "3"})
      self.assertIsNone(attributes)
    finally:
      os.unlink(path)

  def test_yaml_parser_rejects_unsafe_object_and_non_mapping(self):
    for content in ("!!python/object:builtins.object {}\n", "- one\n- two\n"):
      path = self._write_file(content)
      try:
        self.assertEqual(YamlParser().read_data_to_map(path), (None, None))
      finally:
        os.unlink(path)

  def test_xml_parser_uses_supported_element_iteration(self):
    path = self._write_file(
      "<configuration><property><name>key</name><value>value</value>"
      "<final>true</final></property></configuration>"
    )
    try:
      configurations, attributes = XmlParser().read_data_to_map(path)
      self.assertEqual(configurations, {"key": "value"})
      self.assertEqual(attributes, {"key": "true"})
    finally:
      os.unlink(path)

  @staticmethod
  def _write_file(content):
    descriptor, path = tempfile.mkstemp(prefix="ambari-takeover-")
    with os.fdopen(descriptor, "w", encoding="utf-8") as stream:
      stream.write(content)
    return path
