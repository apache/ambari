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

import time
import subprocess
import os
import logging
import tempfile
import pprint
from xml.dom import minidom

from unittest import TestCase
from subprocess import Popen
from mock.mock import MagicMock, call
from mock.mock import patch
from mock.mock import create_autospec
from ambari_server.resourceFilesKeeper import ResourceFilesKeeper, KeeperException


class TestResourceFilesKeeper(TestCase):

  TEST_STACKS_DIR="../resources/stacks"

  # Stack that is not expected to change
  DUMMY_UNCHANGEABLE_STACK="../resources/TestAmbaryServer.samples/" \
                           "dummy_stack/HIVE/"

  DUMMY_ACTIVE_STACK="../resources/TestAmbaryServer.samples/" \
                           "active_stack/"

  DUMMY_INACTIVE_STACK="../resources/TestAmbaryServer.samples/" \
                     "inactive_stack/"

  DUMMY_UNCHANGEABLE_PACKAGE=os.path.join(DUMMY_UNCHANGEABLE_STACK,
                                    ResourceFilesKeeper.PACKAGE_DIR)

  DUMMY_UNCHANGEABLE_PACKAGE_HASH="4cd211c4d16b49b3546ecc9b7996698105fe10e4"
  DUMMY_HASH="dummy_hash"
  YA_HASH="yet_another_hash"
  SOME_PATH="some-path"

  def setUp(self):
    logging.basicConfig(level=logging.ERROR)


  @patch.object(ResourceFilesKeeper, "update_directory_archieves")
  def test_perform_housekeeping(self, update_directory_archieves_mock):
    resource_files_keeper = ResourceFilesKeeper("/dummy-path")
    resource_files_keeper.perform_housekeeping()
    update_directory_archieves_mock.assertCalled()


  @patch.object(ResourceFilesKeeper, "update_directory_archive")
  @patch.object(ResourceFilesKeeper, "list_stacks")
  @patch("os.path.abspath")
  def test_update_directory_archieves(self, abspath_mock,
                                      list_active_stacks_mock,
                                      update_directory_archive_mock):
    list_active_stacks_mock.return_value = [self.DUMMY_UNCHANGEABLE_STACK,
                                            self.DUMMY_UNCHANGEABLE_STACK,
                                            self.DUMMY_UNCHANGEABLE_STACK]
    abspath_mock.side_effect = lambda s : s
    resource_files_keeper = ResourceFilesKeeper(self.TEST_STACKS_DIR)
    resource_files_keeper.update_directory_archieves()
    self.assertEqual(pprint.pformat(
      update_directory_archive_mock.call_args_list),
            "[call('../resources/TestAmbaryServer.samples/"
            "dummy_stack/HIVE/package'),\n "
            "call('../resources/TestAmbaryServer.samples/"
            "dummy_stack/HIVE/package'),\n "
            "call('../resources/TestAmbaryServer.samples/"
            "dummy_stack/HIVE/package'),\n "
            "call('../resources/stacks/custom_actions')]")



  @patch("glob.glob")
  @patch("os.path.exists")
  def test_list_stacks(self, exists_mock, glob_mock):
    resource_files_keeper = ResourceFilesKeeper(self.SOME_PATH)
    # Test normal execution flow
    glob_mock.return_value = ["stack1", "stack2", "stack3"]
    exists_mock.side_effect = [True, False, True]
    res = resource_files_keeper.list_stacks(self.SOME_PATH)
    self.assertEqual(pprint.pformat(res), "['stack1', 'stack3']")

    # Test exception handling
    glob_mock.side_effect = self.keeper_exc_side_effect
    try:
      resource_files_keeper.list_stacks(self.SOME_PATH)
      self.fail('KeeperException not thrown')
    except KeeperException:
      pass # Expected
    except Exception as e:
      self.fail('Unexpected exception thrown:' + str(e))


  @patch.object(ResourceFilesKeeper, "count_hash_sum")
  @patch.object(ResourceFilesKeeper, "read_hash_sum")
  @patch.object(ResourceFilesKeeper, "zip_directory")
  @patch.object(ResourceFilesKeeper, "write_hash_sum")
  def test_update_directory_archive(self, write_hash_sum_mock,
                                    zip_directory_mock, read_hash_sum_mock,
                                    count_hash_sum_mock):
    # Test situation when there is no saved directory hash
    read_hash_sum_mock.return_value = None
    count_hash_sum_mock.return_value = self.YA_HASH
    resource_files_keeper = ResourceFilesKeeper(self.SOME_PATH)
    resource_files_keeper.update_directory_archive(self.SOME_PATH)
    self.assertTrue(read_hash_sum_mock.called)
    self.assertTrue(count_hash_sum_mock.called)
    self.assertTrue(zip_directory_mock.called)
    self.assertTrue(write_hash_sum_mock.called)

    read_hash_sum_mock.reset_mock()
    count_hash_sum_mock.reset_mock()
    zip_directory_mock.reset_mock()
    write_hash_sum_mock.reset_mock()

    # Test situation when saved directory hash == current hash
    read_hash_sum_mock.return_value = self.DUMMY_HASH
    count_hash_sum_mock.return_value = self.YA_HASH
    resource_files_keeper.update_directory_archive(self.SOME_PATH)
    self.assertTrue(read_hash_sum_mock.called)
    self.assertTrue(count_hash_sum_mock.called)
    self.assertTrue(zip_directory_mock.called)
    self.assertTrue(write_hash_sum_mock.called)

    read_hash_sum_mock.reset_mock()
    count_hash_sum_mock.reset_mock()
    zip_directory_mock.reset_mock()
    write_hash_sum_mock.reset_mock()

    # Test situation when saved directory hash == current hash
    read_hash_sum_mock.return_value = self.DUMMY_HASH
    count_hash_sum_mock.return_value = self.DUMMY_HASH
    resource_files_keeper.update_directory_archive(self.SOME_PATH)
    self.assertTrue(read_hash_sum_mock.called)
    self.assertTrue(count_hash_sum_mock.called)
    self.assertFalse(zip_directory_mock.called)
    self.assertFalse(write_hash_sum_mock.called)

    read_hash_sum_mock.reset_mock()
    count_hash_sum_mock.reset_mock()
    zip_directory_mock.reset_mock()
    write_hash_sum_mock.reset_mock()

    # Check that no saved hash file is created when zipping failed
    zip_directory_mock.side_effect = self.keeper_exc_side_effect
    read_hash_sum_mock.return_value = self.DUMMY_HASH
    count_hash_sum_mock.return_value = self.YA_HASH
    try:
      resource_files_keeper.update_directory_archive(self.SOME_PATH)
      self.fail('KeeperException not thrown')
    except KeeperException:
      pass # Expected
    except Exception as e:
      self.fail('Unexpected exception thrown:' + str(e))
    self.assertTrue(read_hash_sum_mock.called)
    self.assertTrue(count_hash_sum_mock.called)
    self.assertTrue(zip_directory_mock.called)
    self.assertFalse(write_hash_sum_mock.called)

    read_hash_sum_mock.reset_mock()
    count_hash_sum_mock.reset_mock()
    zip_directory_mock.reset_mock()
    write_hash_sum_mock.reset_mock()

    # Test nozip option
    read_hash_sum_mock.return_value = None
    count_hash_sum_mock.return_value = self.YA_HASH
    resource_files_keeper = ResourceFilesKeeper(self.SOME_PATH, nozip=True)
    resource_files_keeper.update_directory_archive(self.SOME_PATH)
    self.assertTrue(read_hash_sum_mock.called)
    self.assertTrue(count_hash_sum_mock.called)
    self.assertFalse(zip_directory_mock.called)
    self.assertTrue(write_hash_sum_mock.called)


  def test_count_hash_sum(self):
    # Test normal flow
    resource_files_keeper = ResourceFilesKeeper(self.DUMMY_UNCHANGEABLE_PACKAGE)
    test_dir = os.path.join(self.DUMMY_UNCHANGEABLE_PACKAGE)
    hash_sum = resource_files_keeper.count_hash_sum(test_dir)
    self.assertEqual(hash_sum, self.DUMMY_UNCHANGEABLE_PACKAGE_HASH)

    # Test exception handling
    with patch("__builtin__.open") as open_mock:
      open_mock.side_effect = self.exc_side_effect
      try:
        resource_files_keeper.count_hash_sum(test_dir)
        self.fail('KeeperException not thrown')
      except KeeperException:
        pass # Expected
      except Exception as e:
        self.fail('Unexpected exception thrown:' + str(e))


  def test_read_hash_sum(self):
    resource_files_keeper = ResourceFilesKeeper(self.DUMMY_UNCHANGEABLE_PACKAGE)
    hash_sum = resource_files_keeper.read_hash_sum(self.DUMMY_UNCHANGEABLE_PACKAGE)
    self.assertEqual(hash_sum, "dummy_hash")

    # Test exception handling
    # If file exists, should rethrow exception
    with patch("os.path.isfile") as isfile_mock:
      isfile_mock.return_value = True
      with patch("__builtin__.open") as open_mock:
        open_mock.side_effect = self.exc_side_effect
        try:
          resource_files_keeper.read_hash_sum("path-to-directory")
          self.fail('KeeperException not thrown')
        except KeeperException:
          pass # Expected
        except Exception as e:
          self.fail('Unexpected exception thrown:' + str(e))

    # Test exception handling
    # If file does not exist, should ignore exception
    with patch("os.path.isfile") as isfile_mock:
      isfile_mock.return_value = False
      with patch("__builtin__.open") as open_mock:
        open_mock.side_effect = self.exc_side_effect
        res = resource_files_keeper.read_hash_sum("path-to-directory")
        self.assertEqual(res, None)


  def test_write_hash_sum(self):
    NEW_HASH = "new_hash"
    resource_files_keeper = ResourceFilesKeeper(self.DUMMY_UNCHANGEABLE_PACKAGE)
    resource_files_keeper.write_hash_sum(
      self.DUMMY_UNCHANGEABLE_PACKAGE, NEW_HASH)
    hash_sum = resource_files_keeper.read_hash_sum(self.DUMMY_UNCHANGEABLE_PACKAGE)
    self.assertEqual(hash_sum, NEW_HASH)

    # Revert to previous value
    resource_files_keeper.write_hash_sum(
      self.DUMMY_UNCHANGEABLE_PACKAGE, self.DUMMY_HASH)
    hash_sum = resource_files_keeper.read_hash_sum(self.DUMMY_UNCHANGEABLE_PACKAGE)
    self.assertEqual(hash_sum, self.DUMMY_HASH)

    # Test exception handling
    with patch("__builtin__.open") as open_mock:
      open_mock.side_effect = self.exc_side_effect
      try:
        resource_files_keeper.write_hash_sum("path-to-directory", self.DUMMY_HASH)
        self.fail('KeeperException not thrown')
      except KeeperException:
        pass # Expected
      except Exception as e:
        self.fail('Unexpected exception thrown:' + str(e))


  def test_zip_directory(self):
    # Test normal flow
    resource_files_keeper = ResourceFilesKeeper(self.DUMMY_UNCHANGEABLE_PACKAGE)
    resource_files_keeper.zip_directory(self.DUMMY_UNCHANGEABLE_PACKAGE)
    arc_file = os.path.join(self.DUMMY_UNCHANGEABLE_PACKAGE,
                            ResourceFilesKeeper.ARCHIVE_NAME)
    # Arc file should not be empty
    arc_size=os.path.getsize(arc_file)
    self.assertTrue(40000 < arc_size < 50000)
    # After creating zip, count hash sum of dir (should not change)
    hash_val = resource_files_keeper.count_hash_sum(self.DUMMY_UNCHANGEABLE_PACKAGE)
    self.assertEqual(hash_val, self.DUMMY_UNCHANGEABLE_PACKAGE_HASH)
    # Remove arc file
    os.unlink(arc_file)

    # Test exception handling
    with patch("os.path.join") as join_mock:
      join_mock.side_effect = self.exc_side_effect
      try:
        resource_files_keeper.zip_directory("path-to-directory")
        self.fail('KeeperException not thrown')
      except KeeperException:
        pass # Expected
      except Exception as e:
        self.fail('Unexpected exception thrown:' + str(e))


  def test_is_ignored(self):
    resource_files_keeper = ResourceFilesKeeper(self.DUMMY_UNCHANGEABLE_PACKAGE)
    self.assertTrue(resource_files_keeper.is_ignored(".hash"))
    self.assertTrue(resource_files_keeper.is_ignored("archive.zip"))
    self.assertTrue(resource_files_keeper.is_ignored("dummy.pyc"))
    self.assertFalse(resource_files_keeper.is_ignored("dummy.py"))
    self.assertFalse(resource_files_keeper.is_ignored("1.sh"))


  def exc_side_effect(self, *a):
    raise Exception("horrible_exc")


  def keeper_exc_side_effect(self, *a):
    raise KeeperException("horrible_keeper_exc")
