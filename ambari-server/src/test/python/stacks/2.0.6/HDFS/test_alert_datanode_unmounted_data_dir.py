#!/usr/bin/env python

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

# System imports
import os
import sys
import logging

from mock.mock import patch

# Local imports
from stacks.utils.RMFTestCase import *
import resource_management.libraries.functions.file_system

COMMON_SERVICES_ALERTS_DIR = "HDFS/2.1.0.2.0/package/alerts"
DATA_DIR_MOUNT_HIST_FILE_PATH = "/var/lib/ambari-agent/data/datanode/dfs_data_dir_mount.hist"

file_path = os.path.dirname(os.path.abspath(__file__))
file_path = os.path.dirname(os.path.dirname(os.path.dirname(os.path.dirname(os.path.dirname(file_path)))))
file_path = os.path.join(file_path, "main", "resources", "common-services", COMMON_SERVICES_ALERTS_DIR)

RESULT_STATE_OK = "OK"
RESULT_STATE_WARNING = "WARNING"
RESULT_STATE_CRITICAL = "CRITICAL"
RESULT_STATE_UNKNOWN = "UNKNOWN"

class TestAlertDataNodeUnmountedDataDir(RMFTestCase):

  def setUp(self):
    """
    Import the class under test.
    Because the class is present in a different folder, append its dir to the system path.
    Also, shorten the import name and make it a global so the test functions can access it.
    :return:
    """
    self.logger = logging.getLogger()
    sys.path.append(file_path)
    global alert
    import alert_datanode_unmounted_data_dir as alert

  @patch("resource_management.libraries.functions.file_system.get_and_cache_mount_points")
  def test_missing_configs(self, get_and_cache_mount_points_mock):
    """
    Check that the status is UNKNOWN when configs are missing.
    """
    configs = {}
    [status, messages] = alert.execute(configurations=configs)
    self.assertEqual(status, RESULT_STATE_UNKNOWN)
    self.assertTrue(messages is not None and len(messages) == 1)
    self.assertTrue('is a required parameter for the script' in messages[0])

    configs = {
      "{{hdfs-site/dfs.datanode.data.dir}}": ""
    }
    [status, messages] = alert.execute(configurations=configs)
    self.assertNotEqual(status, RESULT_STATE_UNKNOWN)

  @patch("resource_management.libraries.functions.file_system.get_and_cache_mount_points")
  @patch("resource_management.libraries.functions.file_system.get_mount_point_for_dir")
  @patch("os.path.exists")
  @patch("os.path.isdir")
  def test_mount_history_file_does_not_exist(self, is_dir_mock, exists_mock, get_mount_mock, get_and_cache_mount_points_mock):
    """
    Test that the status is WARNING when the data dirs are mounted on root, but the mount history file
    does not exist.
    """
    configs = {
      "{{hdfs-site/dfs.datanode.data.dir}}": "/grid/0/data"
    }

    # Mock calls
    exists_mock.return_value = False
    is_dir_mock.return_value = True
    get_mount_mock.return_value = "/"

    [status, messages] = alert.execute(configurations=configs)
    self.assertEqual(status, RESULT_STATE_WARNING)
    self.assertTrue(messages is not None and len(messages) == 1)
    self.assertTrue("{0} was not found".format(DATA_DIR_MOUNT_HIST_FILE_PATH) in messages[0])

  @patch("resource_management.libraries.functions.file_system.get_and_cache_mount_points")
  @patch("resource_management.libraries.functions.mounted_dirs_helper.get_dir_to_mount_from_file")
  @patch("resource_management.libraries.functions.file_system.get_mount_point_for_dir")
  @patch("os.path.exists")
  @patch("os.path.isdir")
  def test_all_dirs_on_root(self, is_dir_mock, exists_mock, get_mount_mock, get_data_dir_to_mount_from_file_mock, get_and_cache_mount_points_mock):
    """
    Test that the status is OK when all drives are mounted on the root partition
    and this coincides with the expected values.
    """
    configs = {
      "{{hdfs-site/dfs.datanode.data.dir}}": "/grid/0/data,/grid/1/data,/grid/2/data"
    }

    # Mock calls
    exists_mock.return_value = True
    is_dir_mock.return_value = True
    get_mount_mock.return_value = "/"
    get_data_dir_to_mount_from_file_mock.return_value = {"/grid/0/data": "/",
                                                         "/grid/1/data": "/",
                                                         "/grid/2/data": "/"}

    [status, messages] = alert.execute(configurations=configs)
    self.assertEqual(status, RESULT_STATE_OK)
    self.assertTrue(messages is not None and len(messages) == 1)
    self.assertTrue("The following data dir(s) are valid" in messages[0])

  @patch("resource_management.libraries.functions.file_system.get_and_cache_mount_points")
  @patch("resource_management.libraries.functions.mounted_dirs_helper.get_dir_to_mount_from_file")
  @patch("resource_management.libraries.functions.file_system.get_mount_point_for_dir")
  @patch("os.path.exists")
  @patch("os.path.isdir")
  def test_match_expected(self, is_dir_mock, exists_mock, get_mount_mock, get_data_dir_to_mount_from_file_mock, get_and_cache_mount_points_mock):
    """
    Test that the status is OK when the mount points match the expected values.
    """
    configs = {
      "{{hdfs-site/dfs.datanode.data.dir}}": "/grid/0/data,/grid/1/data,/grid/2/data"
    }

    # Mock calls
    exists_mock.return_value = True
    is_dir_mock.return_value = True
    get_mount_mock.side_effect = ["/device1", "/device2", "/"]
    get_data_dir_to_mount_from_file_mock.return_value = {"/grid/0/data": "/device1",
                                                         "/grid/1/data": "/device2",
                                                         "/grid/2/data": "/"}

    [status, messages] = alert.execute(configurations=configs)
    self.assertEqual(status, RESULT_STATE_OK)
    self.assertTrue(messages is not None and len(messages) == 1)
    self.assertTrue("The following data dir(s) are valid" in messages[0])

  @patch("resource_management.libraries.functions.file_system.get_and_cache_mount_points")
  @patch("resource_management.libraries.functions.mounted_dirs_helper.get_dir_to_mount_from_file")
  @patch("resource_management.libraries.functions.file_system.get_mount_point_for_dir")
  @patch("os.path.exists")
  @patch("os.path.isdir")
  def test_critical_one_root_one_mounted(self, is_dir_mock, exists_mock, get_mount_mock, get_data_dir_to_mount_from_file_mock, get_and_cache_mount_points_mock):
    """
    Test that the status is CRITICAL when the history file is missing
    and at least one data dir is on a mount and at least one data dir is on the root partition.
    """
    configs = {
      "{{hdfs-site/dfs.datanode.data.dir}}": "/grid/0/data,/grid/1/data,/grid/2/data,/grid/3/data"
    }

    # Mock calls
    exists_mock.return_value = False
    is_dir_mock.return_value = True
    # The first 2 data dirs will report an error.
    get_mount_mock.side_effect = ["/", "/", "/device1", "/device2"]

    [status, messages] = alert.execute(configurations=configs)
    self.assertEqual(status, RESULT_STATE_CRITICAL)
    self.assertTrue(messages is not None and len(messages) == 1)
    self.assertTrue("Detected at least one data dir on a mount point, but these are writing to the root partition:\n/grid/0/data\n/grid/1/data" in messages[0])

  @patch("resource_management.libraries.functions.file_system.get_and_cache_mount_points")
  @patch("resource_management.libraries.functions.mounted_dirs_helper.get_dir_to_mount_from_file")
  @patch("resource_management.libraries.functions.file_system.get_mount_point_for_dir")
  @patch("os.path.exists")
  @patch("os.path.isdir")
  def test_critical_unmounted(self, is_dir_mock, exists_mock, get_mount_mock, get_data_dir_to_mount_from_file_mock, get_and_cache_mount_points_mock):
    """
    Test that the status is CRITICAL when the history file exists and one of the dirs
    became unmounted.
    """
    configs = {
      "{{hdfs-site/dfs.datanode.data.dir}}": "/grid/0/data,/grid/1/data,/grid/2/data,/grid/3/data"
    }

    # Mock calls
    exists_mock.return_value = True
    is_dir_mock.return_value = True
    get_mount_mock.side_effect = ["/", "/", "/device3", "/device4"]
    get_data_dir_to_mount_from_file_mock.return_value = {"/grid/0/data": "/",        # remained on /
                                                         "/grid/1/data": "/device2", # became unmounted
                                                         "/grid/2/data": "/",        # became mounted
                                                         "/grid/3/data": "/device4"} # remained mounted

    [status, messages] = alert.execute(configurations=configs)
    self.assertEqual(status, RESULT_STATE_CRITICAL)
    self.assertTrue(messages is not None and len(messages) == 1)
    self.assertTrue("Detected data dir(s) that became unmounted and are now writing to the root partition:\n/grid/1/data" in messages[0])


  @patch("resource_management.libraries.functions.file_system.get_and_cache_mount_points")
  @patch("resource_management.libraries.functions.mounted_dirs_helper.get_dir_to_mount_from_file")
  @patch("resource_management.libraries.functions.file_system.get_mount_point_for_dir")
  @patch("os.path.exists")
  @patch("os.path.isdir")
  def test_file_uri_and_meta_tags(self, is_dir_mock, exists_mock, get_mount_mock, get_data_dir_to_mount_from_file_mock, get_and_cache_mount_points_mock):
    """
    Test that the status is OK when the locations include file:// schemes and meta tags.
    """
    configs = {
      "{{hdfs-site/dfs.datanode.data.dir}}":"[SSD]file:///grid/0/data"
    }

    # Mock calls
    exists_mock.return_value = True
    is_dir_mock.return_value = True
    get_mount_mock.return_value = "/"
    get_data_dir_to_mount_from_file_mock.return_value = {"/grid/0/data":"/"}

    [status, messages] = alert.execute(configurations = configs)
    self.assertEqual(status, RESULT_STATE_OK)
    self.assertTrue(messages is not None and len(messages) == 1)
    self.assertEqual("The following data dir(s) are valid:\n/grid/0/data", messages[0])