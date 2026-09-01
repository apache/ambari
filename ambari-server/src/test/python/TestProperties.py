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
from unittest.mock import patch

import javaproperties

from ambari_server.properties import Properties


class TestProperties(TestCase):
  def test_load_uses_java_properties_escaping_and_continuations(self):
    content = (
      "# ignored comment\n"
      "! another ignored comment\n"
      "equals=value\n"
      "colon:value\n"
      "space value\n"
      "empty=\n"
      "escaped\\=key=escaped separator value\n"
      "escaped\\ key=line\\nvalue\n"
      "continued=one\\\n  two\n"
      "duplicate=first\n"
      "duplicate=last\n"
      "unicode=\\u4f60\\u597d\n"
      "literal-backslash=left\\\\:right\n"
    )
    path = self._write_file(content)
    try:
      properties = Properties()
      with open(path, "r", encoding="utf-8") as stream:
        properties.load(stream)

      self.assertEqual(properties.get_property("equals"), "value")
      self.assertEqual(properties.get_property("colon"), "value")
      self.assertEqual(properties.get_property("space"), "value")
      self.assertEqual(properties.get_property("empty"), "")
      self.assertEqual(
        properties.get_property("escaped=key"), "escaped separator value"
      )
      self.assertEqual(properties.get_property("escaped key"), "line\nvalue")
      self.assertEqual(properties.get_property("continued"), "onetwo")
      self.assertEqual(properties.get_property("duplicate"), "last")
      self.assertEqual(properties.get_property("unicode"), "\u4f60\u597d")
      self.assertEqual(properties.get_property("literal-backslash"), r"left\:right")
      self.assertEqual(
        list(properties.propertyNames()),
        [
          "equals",
          "colon",
          "space",
          "empty",
          "escaped=key",
          "escaped key",
          "continued",
          "duplicate",
          "unicode",
          "literal-backslash",
        ],
      )
    finally:
      os.unlink(path)

  def test_store_round_trips_through_official_parser(self):
    properties = Properties()
    properties.process_pair("key with spaces", "jdbc:postgresql://host/db?x=a=b")
    path = self._write_file("")
    try:
      with open(path, "w", encoding="utf-8") as stream:
        properties.store(stream, "Updated by test")

      with open(path, "r", encoding="utf-8") as stream:
        stored = javaproperties.load(stream)
      self.assertEqual(
        stored, {"key with spaces": "jdbc:postgresql://host/db?x=a=b"}
      )
    finally:
      os.unlink(path)

  def test_store_ordered_sorts_keys(self):
    properties = Properties()
    properties.process_pair("z.key", "last")
    properties.process_pair("a.key", "first")
    path = self._write_file("")
    try:
      with open(path, "w", encoding="utf-8") as stream:
        properties.store_ordered(stream)

      with open(path, "r", encoding="utf-8") as stream:
        stored_keys = [key for key, value in javaproperties.load(stream, list)]
      self.assertEqual(stored_keys, ["a.key", "z.key"])
    finally:
      os.unlink(path)

  def test_store_ordered_closes_stream_after_write_failure(self):
    properties = Properties()
    properties.process_pair("key", "value")
    path = self._write_file("")
    stream = open(path, "w", encoding="utf-8")
    try:
      with patch("javaproperties.dump", side_effect=IOError("write failed")):
        with self.assertRaisesRegex(IOError, "write failed"):
          properties.store_ordered(stream)

      self.assertTrue(stream.closed)
    finally:
      if not stream.closed:
        stream.close()
      os.unlink(path)

  @patch("ambari_server.properties.time.strftime", return_value="fixed timestamp")
  def test_store_preserves_ambari_headers_and_closes_stream(self, _):
    properties = Properties()
    properties.process_pair("key", "value")
    path = self._write_file("")
    stream = open(path, "w", encoding="utf-8")
    try:
      properties.store(stream, "Updated by compatibility test")

      self.assertTrue(stream.closed)
      with open(path, "r", encoding="utf-8") as stored:
        content = stored.read()
      self.assertIn("Apache License, Version 2.0", content)
      self.assertIn("#Updated by compatibility test", content)
      self.assertIn("#fixed timestamp", content)
      self.assertEqual({"key": "value"}, javaproperties.loads(content))
    finally:
      if not stream.closed:
        stream.close()
      os.unlink(path)

  @staticmethod
  def _write_file(content):
    descriptor, path = tempfile.mkstemp(prefix="ambari-properties-")
    with os.fdopen(descriptor, "w", encoding="utf-8") as stream:
      stream.write(content)
    return path
