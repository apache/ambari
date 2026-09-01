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

from unittest import TestCase
import os
import tempfile

from resource_management.libraries.functions import get_port_from_url
from resource_management.core.exceptions import Fail
from resource_management.libraries.providers.hdfs_resource import HdfsResourceProvider


class TestLibraryFunctions(TestCase):
  def test_hdfs_ignore_list_reads_python3_text_and_skips_blank_lines(self):
    with tempfile.TemporaryDirectory() as temporary_directory:
      ignore_file = os.path.join(temporary_directory, "ignored-resources")
      with open(ignore_file, "w", encoding="utf-8") as stream:
        stream.write("hdfs://namenode/path one\n\n/other//path\n")

      self.assertEqual(
        ["/path%20one", "/other/path"],
        HdfsResourceProvider.get_ignored_resources_list(ignore_file),
      )

  def test_get_port_from_url(self):
    self.assertEqual("", get_port_from_url(None))
    self.assertEqual("", get_port_from_url(""))
    self.assertEqual("8080", get_port_from_url("protocol://host:8080"))
    self.assertEqual("8080", get_port_from_url("protocol://host:8080/"))
    self.assertEqual("8080", get_port_from_url("host:8080"))
    self.assertEqual("8080", get_port_from_url("host:8080/"))
    self.assertEqual("8080", get_port_from_url("host:8080/dots_in_url8888:"))
    self.assertEqual("8080", get_port_from_url("protocol://host:8080/dots_in_url8888:"))
    self.assertEqual("8080", get_port_from_url("127.0.0.1:8080"))
    self.assertEqual("8042", get_port_from_url("8042"))
    self.assertRaises(Fail, get_port_from_url, "http://host/no_port")
    self.assertRaises(Fail, get_port_from_url, "127.0.0.1:808080")
