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
from unittest import TestCase

from resource_management.libraries.functions import copy_tarball


class TestCopyTarball(TestCase):
  def test_tez_uses_the_packaged_archive_without_legacy_staging(self):
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
      "tez-native-tarball-staging",
      "tez-native.tar.gz",
    ):
      with self.subTest(obsolete=obsolete):
        self.assertNotIn(obsolete, source)
