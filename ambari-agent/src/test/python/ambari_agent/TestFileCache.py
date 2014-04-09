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
import configparser
import os

import pprint

from unittest import TestCase
import threading
import tempfile
import time
from threading import Thread

from FileCache import FileCache, CachingException
from AmbariConfig import AmbariConfig
from mock.mock import MagicMock, patch
import io
import sys
import shutil


class TestFileCache(TestCase):

  def setUp(self):
    # disable stdout
    out = io.StringIO()
    sys.stdout = out
    # generate sample config
    tmpdir = tempfile.gettempdir()
    self.config = configparser.RawConfigParser()
    self.config.add_section('agent')
    self.config.set('agent', 'prefix', tmpdir)
    self.config.set('agent', 'cache_dir', "/var/lib/ambari-agent/cache")
    self.config.set('agent', 'tolerate_download_failures', "true")


  def test_reset(self):
    fileCache = FileCache(self.config)
    fileCache.uptodate_paths.append('dummy-path')
    fileCache.reset()
    self.assertFalse(fileCache.uptodate_paths)


  @patch.object(FileCache, "provide_directory")
  def test_get_service_base_dir(self, provide_directory_mock):
    provide_directory_mock.return_value = "dummy value"
    fileCache = FileCache(self.config)
    command = {
      'commandParams' : {
        'service_package_folder' : 'HDP/2.1.1/services/ZOOKEEPER/package'
      }
    }
    res = fileCache.get_service_base_dir(command, "server_url_pref")
    self.assertEqual(
      pprint.pformat(provide_directory_mock.call_args_list[0][0]),
      "('/var/lib/ambari-agent/cache',\n "
      "'stacks/HDP/2.1.1/services/ZOOKEEPER/package',\n"
      " 'server_url_pref')")
    self.assertEqual(res, "dummy value")


  @patch.object(FileCache, "provide_directory")
  def test_get_hook_base_dir(self, provide_directory_mock):
    fileCache = FileCache(self.config)
    # Check missing parameter
    command = {
      'commandParams' : {
      }
    }
    base = fileCache.get_hook_base_dir(command, "server_url_pref")
    self.assertEqual(base, None)
    self.assertFalse(provide_directory_mock.called)

    # Check existing dir case
    command = {
      'commandParams' : {
        'hooks_folder' : 'HDP/2.1.1/hooks'
      }
    }
    provide_directory_mock.return_value = "dummy value"
    fileCache = FileCache(self.config)
    res = fileCache.get_hook_base_dir(command, "server_url_pref")
    self.assertEqual(
      pprint.pformat(provide_directory_mock.call_args_list[0][0]),
      "('/var/lib/ambari-agent/cache', "
      "'stacks/HDP/2.1.1/hooks', "
      "'server_url_pref')")
    self.assertEqual(res, "dummy value")


  @patch.object(FileCache, "provide_directory")
  def test_get_custom_actions_base_dir(self, provide_directory_mock):
    provide_directory_mock.return_value = "dummy value"
    fileCache = FileCache(self.config)
    res = fileCache.get_custom_actions_base_dir("server_url_pref")
    self.assertEqual(
      pprint.pformat(provide_directory_mock.call_args_list[0][0]),
      "('/var/lib/ambari-agent/cache', 'custom_actions', 'server_url_pref')")
    self.assertEqual(res, "dummy value")


  @patch.object(FileCache, "build_download_url")
  @patch.object(FileCache, "fetch_url")
  @patch.object(FileCache, "read_hash_sum")
  @patch.object(FileCache, "invalidate_directory")
  @patch.object(FileCache, "unpack_archive")
  @patch.object(FileCache, "write_hash_sum")
  def test_provide_directory(self, write_hash_sum_mock, unpack_archive_mock,
                             invalidate_directory_mock,
                             read_hash_sum_mock, fetch_url_mock,
                             build_download_url_mock):
    build_download_url_mock.return_value = "http://dummy-url/"
    HASH1 = "hash1"
    membuffer = MagicMock()
    membuffer.getvalue.return_value.strip.return_value = HASH1
    fileCache = FileCache(self.config)

    # Test uptodate dirs after start
    self.assertFalse(fileCache.uptodate_paths)

    # Test initial downloading (when dir does not exist)
    fetch_url_mock.return_value = membuffer
    read_hash_sum_mock.return_value = "hash2"
    res = fileCache.provide_directory("cache_path", "subdirectory",
                                      "server_url_prefix")
    self.assertTrue(invalidate_directory_mock.called)
    self.assertTrue(write_hash_sum_mock.called)
    self.assertEqual(fetch_url_mock.call_count, 2)
    self.assertEqual(pprint.pformat(fileCache.uptodate_paths),
                     "['cache_path/subdirectory']")
    self.assertEqual(res, 'cache_path/subdirectory')

    fetch_url_mock.reset_mock()
    write_hash_sum_mock.reset_mock()
    invalidate_directory_mock.reset_mock()
    unpack_archive_mock.reset_mock()

    # Test cache invalidation when local hash does not differ
    fetch_url_mock.return_value = membuffer
    read_hash_sum_mock.return_value = HASH1
    fileCache.reset()

    res = fileCache.provide_directory("cache_path", "subdirectory",
                                      "server_url_prefix")
    self.assertFalse(invalidate_directory_mock.called)
    self.assertFalse(write_hash_sum_mock.called)
    self.assertEqual(fetch_url_mock.call_count, 1)
    self.assertEqual(pprint.pformat(fileCache.uptodate_paths),
                      "['cache_path/subdirectory']")
    self.assertEqual(res, 'cache_path/subdirectory')

    fetch_url_mock.reset_mock()
    write_hash_sum_mock.reset_mock()
    invalidate_directory_mock.reset_mock()
    unpack_archive_mock.reset_mock()

    # Test execution path when path is up-to date (already checked)
    res = fileCache.provide_directory("cache_path", "subdirectory",
                                      "server_url_prefix")
    self.assertFalse(invalidate_directory_mock.called)
    self.assertFalse(write_hash_sum_mock.called)
    self.assertEqual(fetch_url_mock.call_count, 0)
    self.assertEqual(pprint.pformat(fileCache.uptodate_paths),
                      "['cache_path/subdirectory']")
    self.assertEqual(res, 'cache_path/subdirectory')

    # Check exception handling when tolerance is disabled
    self.config.set('agent', 'tolerate_download_failures', "false")
    fetch_url_mock.side_effect = self.caching_exc_side_effect
    fileCache = FileCache(self.config)
    try:
      fileCache.provide_directory("cache_path", "subdirectory",
                                  "server_url_prefix")
      self.fail('CachingException not thrown')
    except CachingException:
      pass # Expected
    except Exception as e:
      self.fail('Unexpected exception thrown:' + str(e))

    # Check that unexpected exceptions are still propagated when
    # tolerance is enabled
    self.config.set('agent', 'tolerate_download_failures', "false")
    fetch_url_mock.side_effect = self.exc_side_effect
    fileCache = FileCache(self.config)
    try:
      fileCache.provide_directory("cache_path", "subdirectory",
                                  "server_url_prefix")
      self.fail('Exception not thrown')
    except Exception:
      pass # Expected


    # Check exception handling when tolerance is enabled
    self.config.set('agent', 'tolerate_download_failures', "true")
    fetch_url_mock.side_effect = self.caching_exc_side_effect
    fileCache = FileCache(self.config)
    res = fileCache.provide_directory("cache_path", "subdirectory",
                                  "server_url_prefix")
    self.assertEqual(res, 'cache_path/subdirectory')


  def test_build_download_url(self):
    fileCache = FileCache(self.config)
    url = fileCache.build_download_url('http://localhost:8080/resources/',
                                       'stacks/HDP/2.1.1/hooks', 'archive.zip')
    self.assertEqual(url,
        'http://localhost:8080/resources//stacks/HDP/2.1.1/hooks/archive.zip')


  @patch("urllib2.urlopen")
  def test_fetch_url(self, urlopen_mock):
    fileCache = FileCache(self.config)
    remote_url = "http://dummy-url/"
    # Test normal download
    test_str = 'abc' * 100000 # Very long string
    test_string_io = io.StringIO(test_str)
    test_buffer = MagicMock()
    test_buffer.read.side_effect = test_string_io.read
    urlopen_mock.return_value = test_buffer

    memory_buffer = fileCache.fetch_url(remote_url)

    self.assertEqual(memory_buffer.getvalue(), test_str)
    self.assertEqual(test_buffer.read.call_count, 20) # depends on buffer size
    # Test exception handling
    test_buffer.read.side_effect = self.exc_side_effect
    try:
      fileCache.fetch_url(remote_url)
      self.fail('CachingException not thrown')
    except CachingException:
      pass # Expected
    except Exception as e:
      self.fail('Unexpected exception thrown:' + str(e))


  def test_read_write_hash_sum(self):
    tmpdir = tempfile.mkdtemp()
    dummyhash = "DUMMY_HASH"
    fileCache = FileCache(self.config)
    fileCache.write_hash_sum(tmpdir, dummyhash)
    newhash = fileCache.read_hash_sum(tmpdir)
    self.assertEqual(newhash, dummyhash)
    shutil.rmtree(tmpdir)
    # Test read of not existing file
    newhash = fileCache.read_hash_sum(tmpdir)
    self.assertEqual(newhash, None)
    # Test write to not existing file
    with patch("__builtin__.open") as open_mock:
      open_mock.side_effect = self.exc_side_effect
      try:
        fileCache.write_hash_sum(tmpdir, dummyhash)
        self.fail('CachingException not thrown')
      except CachingException:
        pass # Expected
      except Exception as e:
        self.fail('Unexpected exception thrown:' + str(e))


  @patch("os.path.isfile")
  @patch("os.path.isdir")
  @patch("os.unlink")
  @patch("shutil.rmtree")
  @patch("os.makedirs")
  def test_invalidate_directory(self, makedirs_mock, rmtree_mock,
                                unlink_mock, isdir_mock, isfile_mock):
    fileCache = FileCache(self.config)
    # Test execution flow if path points to file
    isfile_mock.return_value = True
    isdir_mock.return_value = False

    fileCache.invalidate_directory("dummy-dir")

    self.assertTrue(unlink_mock.called)
    self.assertFalse(rmtree_mock.called)
    self.assertTrue(makedirs_mock.called)

    unlink_mock.reset_mock()
    rmtree_mock.reset_mock()
    makedirs_mock.reset_mock()

    # Test execution flow if path points to dir
    isfile_mock.return_value = False
    isdir_mock.return_value = True

    fileCache.invalidate_directory("dummy-dir")

    self.assertFalse(unlink_mock.called)
    self.assertTrue(rmtree_mock.called)
    self.assertTrue(makedirs_mock.called)

    unlink_mock.reset_mock()
    rmtree_mock.reset_mock()
    makedirs_mock.reset_mock()

    # Test exception handling
    makedirs_mock.side_effect = self.exc_side_effect
    try:
      fileCache.invalidate_directory("dummy-dir")
      self.fail('CachingException not thrown')
    except CachingException:
      pass # Expected
    except Exception as e:
      self.fail('Unexpected exception thrown:' + str(e))


  def test_unpack_archive(self):
    tmpdir = tempfile.mkdtemp()
    dummy_archive = os.path.join("ambari_agent", "dummy_files",
                                 "dummy_archive.zip")
    # Test normal flow
    with open(dummy_archive, "r") as f:
      data = f.read(os.path.getsize(dummy_archive))
      membuf = io.StringIO(data)

    fileCache = FileCache(self.config)
    fileCache.unpack_archive(membuf, tmpdir)
    # Count summary size of unpacked files:
    total_size = 0
    total_files = 0
    total_dirs = 0
    for dirpath, dirnames, filenames in os.walk(tmpdir):
      total_dirs += 1
      for f in filenames:
        fp = os.path.join(dirpath, f)
        total_size += os.path.getsize(fp)
        total_files += 1
    self.assertEqual(total_size, 51258)
    self.assertEqual(total_files, 28)
    self.assertEqual(total_dirs, 8)
    shutil.rmtree(tmpdir)

    # Test exception handling
    with patch("os.path.isdir") as isdir_mock:
      isdir_mock.side_effect = self.exc_side_effect
      try:
        fileCache.unpack_archive(membuf, tmpdir)
        self.fail('CachingException not thrown')
      except CachingException:
        pass # Expected
      except Exception as e:
        self.fail('Unexpected exception thrown:' + str(e))


  def tearDown(self):
    # enable stdout
    sys.stdout = sys.__stdout__


  def exc_side_effect(self, *a):
    raise Exception("horrible_exc")

  def caching_exc_side_effect(self, *a):
    raise CachingException("horrible_caching_exc")