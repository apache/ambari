'''
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
'''

from unittest import TestCase
import os


class TestVersion(TestCase):
  """
  Class that tests the method of the version.py file used to format and compare version numbers
  of both Ambari (which use 3 digits separated by dots) and stacks (which use 4 digits separated by dots).
  """
  def setUp(self):
    import imp

    self.test_directory = os.path.dirname(os.path.abspath(__file__))
    test_file_path = os.path.join(self.test_directory, '../../../../ambari-common/src/main/python/resource_management/libraries/functions/version.py')
    with open(test_file_path, 'rb') as fp:
        self.version_module = imp.load_module('version', fp, test_file_path, ('.py', 'rb', imp.PY_SOURCE))

  def test_format(self):
    l = [("2.2",   "2.2.0.0"),
         ("2.2.1", "2.2.1.0"),
         ("2.2.1.3", "2.2.1.3")]
    
    for input, expected in l:
      actual = self.version_module.format_hdp_stack_version(input)
      self.assertEqual(expected, actual)

    gluster_fs_actual = self.version_module.format_hdp_stack_version("GlusterFS")
    self.assertEqual("", gluster_fs_actual)

  def test_comparison(self):
    # All versions to compare, from 1.0.0.0 to 3.0.0.0, and only include elements that are a multiple of 7.
    versions = range(1000, 3000, 7)
    versions = [".".join(list(str(elem))) for elem in versions]

    for idx, x in enumerate(versions):
      for idy, y in enumerate(versions):
        # Expected value will either be -1, 0, 1, and it relies on the fact
        # that an increasing index implies a greater version number.
        expected_value = cmp(idx, idy)
        actual_value = self.version_module.compare_versions(x, y)
        self.assertEqual(expected_value, actual_value)

    # Try something fancier
    self.assertEqual(0, self.version_module.compare_versions("2.10", "2.10.0"))
    self.assertEqual(0, self.version_module.compare_versions("2.10", "2.10.0.0"))
    self.assertEqual(0, self.version_module.compare_versions("2.10.0", "2.10.0.0"))

    try:
      self.version_module.compare_versions("", "GlusterFS")
    except ValueError:
      pass
    else:
      self.fail("Did not raise exception")