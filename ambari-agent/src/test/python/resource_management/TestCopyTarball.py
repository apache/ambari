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

import inspect
import sys
from types import SimpleNamespace
from unittest import TestCase
from unittest.mock import Mock, patch

from resource_management.libraries.functions import copy_tarball


class TestCopyTarball(TestCase):
  def test_tez_uses_the_packaged_archive_without_legacy_staging(self):
    self.assertEqual({"tez"}, set(copy_tarball.TARBALL_MAP))
    self.assertEqual({"tez": "tez-env"}, copy_tarball.SERVICE_TO_CONFIG_MAP)
    tez_contract = copy_tarball.TARBALL_MAP["tez"]
    self.assertNotIn("prepare_function", tez_contract)
    self.assertEqual("TEZ", tez_contract["service"])
    self.assertEqual(
      (
        "{{ stack_root }}/{{ stack_version }}/usr/lib/tez/lib/tez.tar.gz",
        "/{{ stack_name }}/apps/{{ stack_version }}/tez/tez.tar.gz",
      ),
      tez_contract["dirs"],
    )

    source = inspect.getsource(copy_tarball)
    for obsolete in (
      "_prepare_tez_tarball",
      "_prepare_mapreduce_tarball",
      "tez-native-tarball-staging",
      "tez-native.tar.gz",
      "mapreduce-native-tarball-staging",
      "_get_single_version_from_stack_select",
      '"spark2"',
      '"tez_hive2"',
    ):
      with self.subTest(obsolete=obsolete):
        self.assertNotIn(obsolete, source)

  def test_tarball_paths_preserve_the_stack_root(self):
    with patch.object(copy_tarball.Script, "get_stack_name", return_value="BIGTOP"), \
      patch.object(
        copy_tarball.Script,
        "get_stack_root",
        return_value="/Opt/Bigtop",
      ), \
      patch.object(copy_tarball, "get_current_version", return_value="3.3.0"):
      success, source, destination = copy_tarball.get_tarball_paths("TEZ")

    self.assertTrue(success)
    self.assertEqual("/Opt/Bigtop/3.3.0/usr/lib/tez/lib/tez.tar.gz", source)
    self.assertEqual("/bigtop/apps/3.3.0/tez/tez.tar.gz", destination)

  def test_obsolete_tarball_name_is_rejected(self):
    with patch.object(copy_tarball.Script, "get_stack_name", return_value="BIGTOP"):
      self.assertEqual(
        (False, None, None), copy_tarball.get_tarball_paths("spark2")
      )

  def test_copy_honors_the_requested_file_mode(self):
    hdfs_resource = Mock()
    params = SimpleNamespace(HdfsResource=hdfs_resource)
    with patch.dict(sys.modules, {"params": params}), \
      patch.object(
        copy_tarball,
        "get_tarball_paths",
        return_value=(True, "/usr/lib/tez/lib/tez.tar.gz", "/bigtop/tez.tar.gz"),
      ), \
      patch.object(copy_tarball, "default", return_value={}), \
      patch.object(copy_tarball.os.path, "exists", return_value=True):
      copied = copy_tarball.copy_to_hdfs(
        "tez", "hadoop", "hdfs", file_mode=0o640
      )

    self.assertTrue(copied)
    self.assertEqual(0o640, hdfs_resource.call_args_list[1].kwargs["mode"])
