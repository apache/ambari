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
import os
import logging
from unittest import TestCase
from mock.mock import Mock, MagicMock, patch

from resource_management.libraries.functions import dfs_datanode_helper
from resource_management.core.logger import Logger


class StubParams(object):
  """
  Dummy class to fake params where params.x performs a get on params.dict["x"]
  """
  def __init__(self):
    self.dict = {}

  def __getattr__(self, name):
    return self.dict[name]

  def __repr__(self):
    name = self.__class__.__name__
    mocks = set(dir(self))
    mocks = [x for x in mocks if not str(x).startswith("__")]   # Exclude private methods
    return "<StubParams: {0}; mocks: {1}>".format(name, str(mocks))


def fake_create_dir(directory, other):
  """
  Fake function used as function pointer.
  """
  print "Fake function to create directory {0}".format(directory)


class TestDatanodeHelper(TestCase):
  """
  Test the functionality of the dfs_datanode_helper.py
  """
  logger = logging.getLogger('TestDatanodeHelper')

  grid0 = "/grid/0/data"
  grid1 = "/grid/1/data"
  grid2 = "/grid/2/data"

  params = StubParams()
  params.data_dir_mount_file = "/var/lib/ambari-agent/data/datanode/dfs_data_dir_mount.hist"
  params.dfs_data_dir = "{0},{1},{2}".format(grid0, grid1, grid2)


  @patch.object(Logger, "info")
  @patch.object(Logger, "error")
  def test_normalized(self, log_error, log_info):
    """
    Test that the data dirs are normalized by removing leading and trailing whitespace, and case sensitive.
    """
    params = StubParams()
    params.data_dir_mount_file = "/var/lib/ambari-agent/data/datanode/dfs_data_dir_mount.hist"
    params.dfs_data_dir = "/grid/0/data  ,  /grid/1/data  ,/GRID/2/Data/"

    # Function under test
    dfs_datanode_helper.handle_dfs_data_dir(fake_create_dir, params, update_cache=False)

    for (name, args, kwargs) in log_info.mock_calls:
      print args[0]
    for (name, args, kwargs) in log_error.mock_calls:
      print args[0]

    log_info.assert_any_call("Forcefully creating directory: /grid/0/data")
    log_info.assert_any_call("Forcefully creating directory: /grid/1/data")
    log_info.assert_any_call("Forcefully creating directory: /GRID/2/Data/")

    self.assertEquals(0, log_error.call_count)

  @patch.object(Logger, "info")
  @patch.object(Logger, "error")
  @patch.object(dfs_datanode_helper, "get_data_dir_to_mount_from_file")
  @patch.object(dfs_datanode_helper, "get_mount_point_for_dir")
  @patch.object(os.path, "isdir")
  @patch.object(os.path, "exists")
  def test_grid_becomes_unmounted(self, mock_os_exists, mock_os_isdir, mock_get_mount_point, mock_get_data_dir_to_mount_from_file, log_error, log_info):
    """
    Test when grid2 becomes unmounted
    """
    mock_os_exists.return_value = True    # Indicate that history file exists

    # Initially, all grids were mounted
    mock_get_data_dir_to_mount_from_file.return_value = {self.grid0: "/dev0", self.grid1: "/dev1", self.grid2: "/dev2"}

    # Grid2 then becomes unmounted
    mock_get_mount_point.side_effect = ["/dev0", "/dev1", "/"] * 2
    mock_os_isdir.side_effect = [False, False, False] + [True, True, True]

    # Function under test
    dfs_datanode_helper.handle_dfs_data_dir(fake_create_dir, self.params, update_cache=False)

    for (name, args, kwargs) in log_info.mock_calls:
      print args[0]

    error_logs = []
    for (name, args, kwargs) in log_error.mock_calls:
      error_logs.append(args[0])    # this is a one-tuple
      #print args[0]
    error_msg = "".join(error_logs)

    self.assertEquals(1, log_error.call_count)
    self.assertTrue("Directory /grid/2/data does not exist and became unmounted from /dev2" in error_msg)

  @patch.object(Logger, "info")
  @patch.object(Logger, "error")
  @patch.object(dfs_datanode_helper, "get_data_dir_to_mount_from_file")
  @patch.object(dfs_datanode_helper, "get_mount_point_for_dir")
  @patch.object(os.path, "isdir")
  @patch.object(os.path, "exists")
  def test_grid_becomes_remounted(self, mock_os_exists, mock_os_isdir, mock_get_mount_point, mock_get_data_dir_to_mount_from_file, log_error, log_info):
    """
    Test when grid2 becomes remounted
    """
    mock_os_exists.return_value = True    # Indicate that history file exists

    # Initially, all grids were mounted
    mock_get_data_dir_to_mount_from_file.return_value = {self.grid0: "/dev0", self.grid1: "/dev1", self.grid2: "/"}

    # Grid2 then becomes remounted
    mock_get_mount_point.side_effect = ["/dev0", "/dev1", "/dev2"] * 2
    mock_os_isdir.side_effect = [False, False, False] + [True, True, True]

    # Function under test
    dfs_datanode_helper.handle_dfs_data_dir(fake_create_dir, self.params, update_cache=False)

    for (name, args, kwargs) in log_info.mock_calls:
      print args[0]

    for (name, args, kwargs) in log_error.mock_calls:
      print args[0]

    self.assertEquals(0, log_error.call_count)
