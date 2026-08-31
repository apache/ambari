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
import os
from unittest import TestCase


MODULE_PATH = os.path.abspath(
  os.path.join(
    os.path.dirname(__file__), "../../main/repo/install_ambari_tarball.py"
  )
)
SPEC = importlib.util.spec_from_file_location("install_ambari_tarball", MODULE_PATH)
install_ambari_tarball = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(install_ambari_tarball)


class TestInstallAmbariTarball(TestCase):
  def test_dependency_property_names_include_family_and_generic_fallbacks(self):
    self.assertEqual(
      install_ambari_tarball.dependency_property_names(
        "rpm.dependency.list", "redhat", "3"
      ),
      [
        "rpm.dependency.listredhat3",
        "rpm.dependency.listredhat2",
        "rpm.dependency.listredhat1",
        "rpm.dependency.listredhat",
        "rpm.dependency.list",
      ],
    )
