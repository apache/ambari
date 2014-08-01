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
from mock.mock import patch, MagicMock
import os
import sys
from resource_management.core import Environment, Fail
from resource_management.core.resources import File
from resource_management.core.system import System
import resource_management.core.providers.system
import resource_management


@patch.object(System, "os_family", new = 'redhat')
class TestFileResource(TestCase):
  @patch.object(os.path, "dirname")
  @patch.object(os.path, "isdir")
  def test_action_create_dir_exist(self, isdir_mock, dirname_mock):
    """
    Tests if 'create' action fails when path is existent directory
    """
    isdir_mock.side_effect = [True, False]
    try:
      with Environment('/') as env:
        File('/existent_directory',
             action='create',
             mode=0777,
             content='file-content'
        )
      
      self.fail("Must fail when directory with name 'path' exist")
    except Fail as e:
      self.assertEqual("Applying File['/existent_directory'] failed, directory with name /existent_directory exists",
                       str(e))
    self.assertFalse(dirname_mock.called)

  @patch.object(os.path, "dirname")
  @patch.object(os.path, "isdir")
  def test_action_create_parent_dir_non_exist(self, isdir_mock, dirname_mock):
    """
    Tests if 'create' action fails when parent directory of path
    doesn't exist
    """
    isdir_mock.side_effect = [False, False]
    dirname_mock.return_value = "/non_existent_directory"
    try:
      with Environment('/') as env:
        File('/non_existent_directory/file',
             action='create',
             mode=0777,
             content='file-content'
        )
      
      self.fail('Must fail on non existent parent directory')
    except Fail as e:
      self.assertEqual(
        "Applying File['/non_existent_directory/file'] failed, parent directory /non_existent_directory doesn't exist",
        str(e))
    self.assertTrue(dirname_mock.called)

  @patch("resource_management.core.providers.system._ensure_metadata")
  @patch("__builtin__.open")
  @patch.object(os.path, "exists")
  @patch.object(os.path, "isdir")
  def test_action_create_non_existent_file(self, isdir_mock, exists_mock, open_mock, ensure_mock):
    """
    Tests if 'create' action create new non existent file and write proper data
    """
    isdir_mock.side_effect = [False, True]
    exists_mock.return_value = False
    new_file = MagicMock()
    open_mock.return_value = new_file
    with Environment('/') as env:
      File('/directory/file',
           action='create',
           mode=0777,
           content='file-content'
      )
    

    open_mock.assert_called_with('/directory/file', 'wb')
    new_file.__enter__().write.assert_called_with('file-content')
    self.assertEqual(open_mock.call_count, 1)
    ensure_mock.assert_called()


  @patch("resource_management.core.providers.system._ensure_metadata")
  @patch("__builtin__.open")
  @patch.object(os.path, "exists")
  @patch.object(os.path, "isdir")
  def test_action_create_replace(self, isdir_mock, exists_mock, open_mock, ensure_mock):
    """
    Tests if 'create' action rewrite existent file with new data
    """
    isdir_mock.side_effect = [False, True]
    old_file, new_file = MagicMock(), MagicMock()
    open_mock.side_effect = [old_file, new_file]
    old_file.read.return_value = 'old-content'
    exists_mock.return_value = True

    with Environment('/') as env:
      File('/directory/file',
           action='create',
           mode=0777,
           backup=False,
           content='new-content'
      )

    
    old_file.read.assert_called()
    new_file.__enter__().write.assert_called_with('new-content')
    ensure_mock.assert_called()
    self.assertEqual(open_mock.call_count, 2)
    open_mock.assert_any_call('/directory/file', 'rb')
    open_mock.assert_any_call('/directory/file', 'wb')


  @patch.object(os, "unlink")
  @patch.object(os.path, "exists")
  @patch.object(os.path, "isdir")
  def test_action_delete_is_directory(self, isdir_mock, exist_mock, unlink_mock):
    """
    Tests if 'delete' action fails when path is directory
    """
    isdir_mock.return_value = True

    try:
      with Environment('/') as env:
        File('/directory/file',
             action='delete',
             mode=0777,
             backup=False,
             content='new-content'
        )
      
      self.fail("Should fail when deleting directory")
    except Fail:
      pass

    self.assertEqual(isdir_mock.call_count, 1)
    self.assertEqual(exist_mock.call_count, 0)
    self.assertEqual(unlink_mock.call_count, 0)

  @patch.object(os, "unlink")
  @patch.object(os.path, "exists")
  @patch.object(os.path, "isdir")
  def test_action_delete(self, isdir_mock, exist_mock, unlink_mock):
    """
    Tests if 'delete' action removes file
    """
    isdir_mock.return_value = False

    with Environment('/') as env:
      File('/directory/file',
           action='delete',
           mode=0777,
           backup=False,
           content='new-content'
      )
    

    self.assertEqual(isdir_mock.call_count, 1)
    self.assertEqual(exist_mock.call_count, 1)
    self.assertEqual(unlink_mock.call_count, 1)


  @patch.object(os.path, "isdir")
  def test_attribute_path(self, isdir_mock):
    """
    Tests 'path' attribute
    """
    isdir_mock.side_effect = [True, False]

    try:
      with Environment('/') as env:
        File('/existent_directory',
             action='create',
             mode=0777,
             content='file-content'
        )
      
      self.fail("Must fail when directory with name 'path' exist")
    except Fail as e:
      pass

    isdir_mock.assert_called_with('/existent_directory')

  @patch.object(resource_management.core.Environment, "backup_file")
  @patch("resource_management.core.providers.system._ensure_metadata")
  @patch("__builtin__.open")
  @patch.object(os.path, "exists")
  @patch.object(os.path, "isdir")
  def test_attribute_backup(self, isdir_mock, exists_mock, open_mock, ensure_mock, backup_file_mock):
    """
    Tests 'backup' attribute
    """
    isdir_mock.side_effect = [False, True, False, True]
    open_mock.return_value = MagicMock()
    exists_mock.return_value = True

    with Environment('/') as env:
      File('/directory/file',
           action='create',
           mode=0777,
           backup=False,
           content='new-content'
      )
    

    self.assertEqual(backup_file_mock.call_count, 0)

    with Environment('/') as env:
      File('/directory/file',
           action='create',
           mode=0777,
           backup=True,
           content='new-content'
      )
    

    self.assertEqual(backup_file_mock.call_count, 1)
    backup_file_mock.assert_called_with('/directory/file')


  @patch("resource_management.core.providers.system._ensure_metadata")
  @patch("__builtin__.open")
  @patch.object(os.path, "exists")
  @patch.object(os.path, "isdir")
  def test_attribute_replace(self, isdir_mock, exists_mock, open_mock, ensure_mock):
    """
    Tests 'replace' attribute
    """
    isdir_mock.side_effect = [False, True]
    old_file, new_file = MagicMock(), MagicMock()
    open_mock.side_effect = [old_file, new_file]
    old_file.read.return_value = 'old-content'
    exists_mock.return_value = True

    with Environment('/') as env:
      File('/directory/file',
           action='create',
           mode=0777,
           backup=False,
           content='new-content',
           replace=False
      )

    
    old_file.read.assert_called()
    self.assertEqual(new_file.__enter__().write.call_count, 0)
    ensure_mock.assert_called()
    self.assertEqual(open_mock.call_count, 0)


  @patch("resource_management.core.providers.system._coerce_uid")
  @patch("resource_management.core.providers.system._coerce_gid")
  @patch.object(os, "chown")
  @patch.object(os, "chmod")
  @patch.object(os, "stat")
  @patch("__builtin__.open")
  @patch.object(os.path, "exists")
  @patch.object(os.path, "isdir")
  def test_ensure_metadata(self, isdir_mock, exists_mock, open_mock, stat_mock, chmod_mock, chown_mock, gid_mock,
                           uid_mock):
    """
    Tests if _ensure_metadata changes owner, usergroup and permissions of file to proper values
    """
    isdir_mock.side_effect = [False, True, False, True]
    exists_mock.return_value = False

    class stat():
      def __init__(self):
        self.st_mode = 0666
        self.st_uid = 1
        self.st_gid = 1

    stat_mock.return_value = stat()
    gid_mock.return_value = 0
    uid_mock.return_value = 0

    with Environment('/') as env:
      File('/directory/file',
           action='create',
           mode=0777,
           content='file-content',
           owner='root',
           group='hdfs'
      )
    

    open_mock.assert_called_with('/directory/file', 'wb')
    self.assertEqual(open_mock.call_count, 1)
    stat_mock.assert_called_with('/directory/file')
    self.assertEqual(chmod_mock.call_count, 1)
    self.assertEqual(chown_mock.call_count, 2)
    gid_mock.assert_called_once_with('hdfs')
    uid_mock.assert_called_once_with('root')

    chmod_mock.reset_mock()
    chown_mock.reset_mock()
    gid_mock.return_value = 1
    uid_mock.return_value = 1

    with Environment('/') as env:
      File('/directory/file',
           action='create',
           mode=0777,
           content='file-content',
           owner='root',
           group='hdfs'
      )
    

    self.assertEqual(chmod_mock.call_count, 1)
    self.assertEqual(chown_mock.call_count, 0)

  @patch("resource_management.core.providers.system._ensure_metadata")
  @patch("resource_management.core.providers.system.FileProvider._get_content")
  @patch("__builtin__.open")
  @patch.object(os.path, "exists")
  @patch.object(os.path, "isdir")
  def test_action_create_encoding(self, isdir_mock, exists_mock, open_mock, get_content_mock ,ensure_mock):

    isdir_mock.side_effect = [False, True]
    exists_mock.return_value = True
    content_mock = MagicMock()
    old_content_mock = MagicMock()
    get_content_mock.return_value = content_mock
    new_file = MagicMock()
    open_mock.return_value = new_file
    enter_file_mock = MagicMock()
    enter_file_mock.read = MagicMock(return_value=old_content_mock)
    new_file.__enter__ = MagicMock(return_value=enter_file_mock)
    with Environment('/') as env:
      File('/directory/file',
           action='create',
           mode=0777,
           content='file-content',
           encoding = "UTF-8"
      )


    open_mock.assert_called_with('/directory/file', 'wb')
    content_mock.encode.assert_called_with('UTF-8')
    old_content_mock.decode.assert_called_with('UTF-8')

