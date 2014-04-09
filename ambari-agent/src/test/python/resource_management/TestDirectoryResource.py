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
import shutil
from resource_management.core.system import System
from resource_management.core import Environment, Fail
from resource_management.core.resources import Directory

@patch.object(System, "os_family", new = 'redhat')
class TestFileResource(TestCase):
  
  @patch.object(os.path, "exists")
  @patch.object(os, "makedirs")
  @patch.object(os.path, "isdir")
  @patch.object(os, "stat")
  @patch.object(os,"chmod")
  @patch.object(os,"chown")
  @patch("resource_management.core.providers.system._coerce_uid")
  @patch("resource_management.core.providers.system._coerce_gid")
  def test_create_directory_recursive(self, _coerce_gid_mock, _coerce_uid_mock,
                                      os_chown_mock, os_chmod_mock, os_stat_mock,
                                      isdir_mock, os_makedirs_mock, 
                                      os_path_exists_mock):
    os_path_exists_mock.return_value = False
    isdir_mock.return_value = True
    _coerce_uid_mock.return_value = 66
    _coerce_gid_mock.return_value = 77
    os_stat_mock.return_value = type("", (), dict(st_mode=0o755, st_uid=0, st_gid=0))()
    
    with Environment('/') as env:
      Directory('/a/b/c/d',
           action='create',
           mode=0o777,
           owner="hdfs",
           group="hadoop",
           recursive=True
      )
      
    os_makedirs_mock.assert_called_with('/a/b/c/d', 0o777)
    os_chmod_mock.assert_called_with('/a/b/c/d', 0o777)
    os_chown_mock.assert_any_call('/a/b/c/d', 66, -1)
    os_chown_mock.assert_any_call('/a/b/c/d', -1, 77)
  
  @patch.object(os.path, "exists")
  @patch.object(os.path, "dirname")
  @patch.object(os.path, "isdir")
  @patch.object(os, "mkdir")
  @patch.object(os, "stat")
  @patch.object(os,"chmod")
  @patch.object(os,"chown")
  @patch("resource_management.core.providers.system._coerce_uid")
  @patch("resource_management.core.providers.system._coerce_gid")
  def test_create_directory_not_recursive(self, _coerce_gid_mock, _coerce_uid_mock,
                                      os_chown_mock, os_chmod_mock, os_stat_mock,
                                      mkdir_mock, isdir_mock, os_dirname_mock, 
                                      os_path_exists_mock):
    os_path_exists_mock.return_value = False
    os_dirname_mock.return_value = "/a/b/c"
    isdir_mock.return_value = True
    _coerce_uid_mock.return_value = 66
    _coerce_gid_mock.return_value = 77
    os_stat_mock.return_value = type("", (), dict(st_mode=0o755, st_uid=0, st_gid=0))()
    
    with Environment('/') as env:
      Directory('/a/b/c/d',
           action='create',
           mode=0o777,
           owner="hdfs",
           group="hadoop"
      )
      
    mkdir_mock.assert_called_with('/a/b/c/d', 0o777)
    os_chmod_mock.assert_called_with('/a/b/c/d', 0o777)
    os_chown_mock.assert_any_call('/a/b/c/d', 66, -1)
    os_chown_mock.assert_any_call('/a/b/c/d', -1, 77)
    
  @patch.object(os.path, "exists")
  @patch.object(os.path, "dirname")
  @patch.object(os.path, "isdir")
  def test_create_directory_failed_no_parent(self, isdir_mock, os_dirname_mock, 
                                      os_path_exists_mock):
    os_path_exists_mock.return_value = False
    os_dirname_mock.return_value = "/a/b/c"
    isdir_mock.return_value = False
    
    
    try:
      with Environment('/') as env:
        Directory('/a/b/c/d',
             action='create',
             mode=0o777,
             owner="hdfs",
             group="hadoop"
        )
      self.fail("Must fail because parent directory /a/b/c doesn't exist")
    except Fail as e:
      self.assertEqual("Applying Directory['/a/b/c/d'] failed, parent directory /a/b/c doesn't exist",
                       str(e))

  @patch.object(os.path, "exists")
  @patch.object(os.path, "isdir")
  def test_create_directory_path_is_file_or_line(self, isdir_mock, os_path_exists_mock):
    os_path_exists_mock.return_value = True
    isdir_mock.return_value = False
    
    try:
      with Environment('/') as env:
        Directory('/a/b/c/d',
             action='create',
             mode=0o777,
             owner="hdfs",
             group="hadoop"
        )
      self.fail("Must fail because file /a/b/c/d already exists")
    except Fail as e:
      self.assertEqual("Applying Directory['/a/b/c/d'] failed, file /a/b/c/d already exists",
                       str(e))
  
  @patch.object(shutil, "rmtree")
  @patch.object(os.path, "exists")
  @patch.object(os.path, "isdir")
  def test_delete_directory(self, isdir_mock, os_path_exists_mock, rmtree_mock):
    os_path_exists_mock.return_value = True
    isdir_mock.return_value = True
    
    with Environment('/') as env:
      Directory('/a/b/c/d',
           action='delete'
      )
      
    rmtree_mock.assert_called_with('/a/b/c/d')
    
  @patch.object(os.path, "exists")
  def test_delete_noexisting_directory(self, os_path_exists_mock):
    os_path_exists_mock.return_value = False
    
    with Environment('/') as env:
      Directory('/a/b/c/d',
           action='delete'
      )
  
  @patch.object(shutil, "rmtree")
  @patch.object(os.path, "exists")
  @patch.object(os.path, "isdir")
  def test_delete_directory_with_path_to_file(self, isdir_mock, os_path_exists_mock, rmtree_mock):
    os_path_exists_mock.return_value = True
    isdir_mock.return_value = False
    
    try:
      with Environment('/') as env:
        Directory('/a/b/c/d',
             action='delete'
        )
      self.fail("Must fail because /a/b/c/d is not a directory")
    except Fail as e:
      self.assertEqual("Applying Directory['/a/b/c/d'] failed, /a/b/c/d is not a directory",
                       str(e))