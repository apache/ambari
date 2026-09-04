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

import os
import hashlib
import json

import pprint

from unittest import TestCase
import threading
import tempfile
import time
from threading import Thread

from ambari_agent.FileCache import FileCache, CachingException
from ambari_agent.AmbariConfig import AmbariConfig
from unittest.mock import MagicMock, patch
import io
import sys
import shutil
import zipfile


class TestFileCache(TestCase):
  def setUp(self):
    # disable stdout
    out = io.StringIO()
    sys.stdout = out
    # generate sample config
    tmpdir = tempfile.gettempdir()
    self.config = AmbariConfig()
    self.config.set("agent", "prefix", tmpdir)
    self.config.set("agent", "cache_dir", "/var/lib/ambari-agent/cache")
    self.config.set("agent", "tolerate_download_failures", "true")
    self.config.add_section(AmbariConfig.AMBARI_PROPERTIES_CATEGORY)
    self.config.set(
      AmbariConfig.AMBARI_PROPERTIES_CATEGORY,
      FileCache.ENABLE_AUTO_AGENT_CACHE_UPDATE_KEY,
      "true",
    )
    self.config.get_server_ssl_context = MagicMock()

  def test_reset(self):
    fileCache = FileCache(self.config)
    fileCache.uptodate_paths["dummy-path"] = "0" * 64
    fileCache.reset()
    self.assertFalse(fileCache.uptodate_paths)

  @patch.object(FileCache, "provide_directory")
  def test_get_service_base_dir(self, provide_directory_mock):
    provide_directory_mock.return_value = "dummy value"
    fileCache = FileCache(self.config)
    command = {
      "commandParams": {
        "service_package_folder": os.path.join(
          "stacks", "HDP", "2.1.1", "services", "ZOOKEEPER", "package"
        )
      },
      "ambariLevelParams": {"jdk_location": "server_url_pref"},
    }
    res = fileCache.get_service_base_dir(command)
    provide_directory_mock.assert_called_once_with(
      "/var/lib/ambari-agent/cache",
      os.path.join("stacks", "HDP", "2.1.1", "services", "ZOOKEEPER", "package"),
      "server_url_pref",
      None,
    )
    self.assertEqual(res, "dummy value")

  @patch.object(FileCache, "provide_directory")
  def test_get_hook_base_dir(self, provide_directory_mock):
    fileCache = FileCache(self.config)
    # Check missing parameter
    command = {
      "clusterLevelParams": {},
      "ambariLevelParams": {"jdk_location": "server_url_pref"},
    }
    base = fileCache.get_hook_base_dir(command)
    self.assertEqual(base, None)
    self.assertFalse(provide_directory_mock.called)

    # Check existing dir case
    command = {
      "clusterLevelParams": {"hooks_folder": "stack-hooks"},
      "ambariLevelParams": {"jdk_location": "server_url_pref"},
    }
    provide_directory_mock.return_value = "dummy value"
    fileCache = FileCache(self.config)
    res = fileCache.get_hook_base_dir(command)
    provide_directory_mock.assert_called_once_with(
      "/var/lib/ambari-agent/cache", "stack-hooks", "server_url_pref", None
    )
    self.assertEqual(res, "dummy value")

  @patch.object(FileCache, "provide_directory")
  def test_get_custom_actions_base_dir(self, provide_directory_mock):
    provide_directory_mock.return_value = "dummy value"
    fileCache = FileCache(self.config)
    res = fileCache.get_custom_actions_base_dir(
      {"ambariLevelParams": {"jdk_location": "server_url_pref"}}
    )
    provide_directory_mock.assert_called_once_with(
      "/var/lib/ambari-agent/cache", "custom_actions", "server_url_pref", None
    )
    self.assertEqual(res, "dummy value")

  @patch.object(FileCache, "provide_directory")
  def test_get_custom_resources_subdir(self, provide_directory_mock):
    provide_directory_mock.return_value = "dummy value"
    fileCache = FileCache(self.config)
    command = {
      "commandParams": {"custom_folder": "dashboards"},
      "ambariLevelParams": {"jdk_location": "server_url_pref"},
    }

    res = fileCache.get_custom_resources_subdir(command)
    provide_directory_mock.assert_called_once_with(
      "/var/lib/ambari-agent/cache", "dashboards", "server_url_pref", None
    )
    self.assertEqual(res, "dummy value")

  @patch.object(FileCache, "build_download_url")
  def test_provide_directory_no_update(self, build_download_url_mock):
    try:
      self.config.set(
        AmbariConfig.AMBARI_PROPERTIES_CATEGORY,
        FileCache.ENABLE_AUTO_AGENT_CACHE_UPDATE_KEY,
        "false",
      )
      fileCache = FileCache(self.config)

      # Test uptodate dirs after start
      with tempfile.TemporaryDirectory() as cache_root:
        path = os.path.join(cache_root, "subdirectory")
        os.makedirs(path)
        res = fileCache.provide_directory(
          cache_root, "subdirectory", "https://server/resources"
        )
        self.assertEqual(res, path)
        self.assertFalse(build_download_url_mock.called)
    finally:
      self.config.set(
        AmbariConfig.AMBARI_PROPERTIES_CATEGORY,
        FileCache.ENABLE_AUTO_AGENT_CACHE_UPDATE_KEY,
        "true",
      )
    pass

  @patch.object(FileCache, "build_download_url")
  @patch.object(FileCache, "fetch_url")
  @patch.object(FileCache, "read_hash_sum")
  @patch.object(FileCache, "replace_directory")
  def test_provide_directory(
    self,
    replace_directory_mock,
    read_hash_sum_mock,
    fetch_url_mock,
    build_download_url_mock,
  ):
    build_download_url_mock.return_value = "http://dummy-url/"
    HASH1 = "hash1"
    membuffer = MagicMock()
    membuffer.getvalue.return_value.strip.return_value = HASH1
    fileCache = FileCache(self.config)

    # Test uptodate dirs after start
    self.assertFalse(fileCache.uptodate_paths)
    path = os.path.abspath(os.path.join("cache_path", "subdirectory"))
    # Test initial downloading (when dir does not exist)
    fetch_url_mock.return_value = membuffer
    read_hash_sum_mock.return_value = "hash2"
    res = fileCache.provide_directory(
      "cache_path", "subdirectory", "server_url_prefix"
    )
    replace_directory_mock.assert_called_once_with(membuffer, path, HASH1, None)
    self.assertEqual(fetch_url_mock.call_count, 2)
    self.assertEqual(fileCache.uptodate_paths, {})
    self.assertEqual(res, path)

    fetch_url_mock.reset_mock()
    replace_directory_mock.reset_mock()

    # Test cache invalidation when local hash does not differ
    fetch_url_mock.return_value = membuffer
    read_hash_sum_mock.return_value = HASH1
    fileCache.reset()

    res = fileCache.provide_directory(
      "cache_path", "subdirectory", "server_url_prefix"
    )
    self.assertFalse(replace_directory_mock.called)
    self.assertEqual(fetch_url_mock.call_count, 1)

    self.assertEqual(fileCache.uptodate_paths, {})
    self.assertEqual(res, path)

    fetch_url_mock.reset_mock()
    replace_directory_mock.reset_mock()

    # Test execution path when path is up-to date (already checked)
    res = fileCache.provide_directory("cache_path", "subdirectory", "server_url_prefix")
    self.assertFalse(replace_directory_mock.called)
    self.assertEqual(fetch_url_mock.call_count, 1)
    self.assertEqual(fileCache.uptodate_paths, {})
    self.assertEqual(res, path)

    # Check exception handling when tolerance is disabled
    self.config.set("agent", "tolerate_download_failures", "false")
    fetch_url_mock.side_effect = self.caching_exc_side_effect
    fileCache = FileCache(self.config)
    try:
      fileCache.provide_directory("cache_path", "subdirectory", "server_url_prefix")
      self.fail("CachingException not thrown")
    except CachingException:
      pass  # Expected
    except Exception as e:
      self.fail("Unexpected exception thrown:" + str(e))

    # Check that unexpected exceptions are still propagated when
    # tolerance is enabled
    self.config.set("agent", "tolerate_download_failures", "true")
    fetch_url_mock.side_effect = self.exc_side_effect
    fileCache = FileCache(self.config)
    try:
      fileCache.provide_directory("cache_path", "subdirectory", "server_url_prefix")
      self.fail("Exception not thrown")
    except Exception:
      pass  # Expected

    # Check exception handling when tolerance is enabled
    self.config.set("agent", "tolerate_download_failures", "true")
    fetch_url_mock.side_effect = self.caching_exc_side_effect
    fileCache = FileCache(self.config)
    with self.assertRaises(CachingException):
      fileCache.provide_directory("cache_path", "subdirectory", "server_url_prefix")

    with tempfile.TemporaryDirectory() as existing_cache_root:
      existing_path = os.path.join(existing_cache_root, "subdirectory")
      os.makedirs(existing_path)
      with open(
        os.path.join(existing_path, FileCache.ARCHIVE_DIGEST_FILE),
        "w",
        encoding="ascii",
      ) as stream:
        stream.write("a" * 64)
      res = fileCache.provide_directory(
        existing_cache_root, "subdirectory", "https://server/resources"
      )
      self.assertEqual(res, existing_path)

    # Test empty archive
    fetch_url_mock.reset_mock()
    build_download_url_mock.reset_mock()
    read_hash_sum_mock.reset_mock()
    replace_directory_mock.reset_mock()
    fileCache.reset()

    fetch_url_mock.side_effect = None
    membuffer_empty = MagicMock()
    membuffer_empty.getvalue.return_value.strip.return_value = ""
    fetch_url_mock.return_value = membuffer_empty  # Remote hash and content
    read_hash_sum_mock.return_value = "hash2"  # Local hash

    with self.assertRaisesRegex(CachingException, "archive is empty"):
      fileCache.provide_directory(
        "cache_path", "subdirectory", "server_url_prefix"
      )
    self.assertTrue(
      fetch_url_mock.return_value.strip() != read_hash_sum_mock.return_value.strip()
    )
    self.assertEqual(build_download_url_mock.call_count, 2)
    self.assertEqual(fetch_url_mock.call_count, 2)
    self.assertFalse(replace_directory_mock.called)
    self.assertFalse(fileCache.uptodate_paths)
    with self.assertRaisesRegex(CachingException, "archive is empty"):
      fileCache.provide_directory(
        "cache_path", "subdirectory", "server_url_prefix"
      )
    self.assertEqual(build_download_url_mock.call_count, 4)
    self.assertEqual(fetch_url_mock.call_count, 4)
    pass

  def test_build_download_url(self):
    fileCache = FileCache(self.config)
    url = fileCache.build_download_url(
      "http://localhost:8080/resources", "stacks/HDP/2.1.1/hooks", "archive.zip"
    )
    self.assertEqual(
      url, "http://localhost:8080/resources/stacks/HDP/2.1.1/hooks/archive.zip"
    )

  def test_http_archive_requires_digest_from_trusted_command(self):
    file_cache = FileCache(self.config)
    file_cache.fetch_url = MagicMock()

    with self.assertRaisesRegex(CachingException, "trusted digest"):
      file_cache.provide_directory(
        tempfile.gettempdir(), "service/package", "http://server/resources"
      )

    file_cache.fetch_url.assert_not_called()

  def test_http_archive_is_verified_before_cache_replacement(self):
    file_cache = FileCache(self.config)
    archive = io.BytesIO()
    with zipfile.ZipFile(archive, "w") as zip_archive:
      zip_archive.writestr("scripts/service.py", "print('trusted')")
    archive_bytes = archive.getvalue()
    expected_digest = hashlib.sha256(archive_bytes).hexdigest()
    trusted_digests = json.dumps({"service/package": expected_digest})
    command = {
      "commandParams": {"service_package_folder": "service/package"},
      "ambariLevelParams": {
        "jdk_location": "http://server/resources",
        FileCache.RESOURCE_ARCHIVE_DIGESTS_KEY: trusted_digests,
      },
    }
    file_cache.cache_dir = tempfile.mkdtemp()
    file_cache.fetch_url = MagicMock(
      side_effect=[io.BytesIO(b"resource-generation"), io.BytesIO(archive_bytes)]
    )
    try:
      provided_path = file_cache.get_service_base_dir(command)
      with open(
        os.path.join(provided_path, FileCache.ARCHIVE_DIGEST_FILE),
        encoding="ascii",
      ) as stream:
        self.assertEqual(expected_digest, stream.read())
      with open(os.path.join(provided_path, "scripts", "service.py")) as stream:
        self.assertEqual("print('trusted')", stream.read())
    finally:
      shutil.rmtree(file_cache.cache_dir)

  def test_archive_digest_mismatch_preserves_existing_cache(self):
    file_cache = FileCache(self.config)
    archive = io.BytesIO()
    with zipfile.ZipFile(archive, "w") as zip_archive:
      zip_archive.writestr("value", "untrusted")
    archive.seek(0)

    with tempfile.TemporaryDirectory() as parent_directory:
      target_directory = os.path.join(parent_directory, "cache")
      os.makedirs(target_directory)
      value_path = os.path.join(target_directory, "value")
      with open(value_path, "w") as stream:
        stream.write("trusted-old")

      with self.assertRaisesRegex(CachingException, "does not match"):
        file_cache.replace_directory(
          archive, target_directory, b"new-hash", "0" * 64
        )

      with open(value_path) as stream:
        self.assertEqual("trusted-old", stream.read())

  def test_trusted_generation_change_revalidates_uptodate_path(self):
    file_cache = FileCache(self.config)
    first_archive = self.create_archive_bytes("first")
    second_archive = self.create_archive_bytes("second")
    first_digest = hashlib.sha256(first_archive).hexdigest()
    second_digest = hashlib.sha256(second_archive).hexdigest()

    with tempfile.TemporaryDirectory() as cache_root:
      file_cache.fetch_url = MagicMock(
        side_effect=[io.BytesIO(b"hash-1"), io.BytesIO(first_archive)]
      )
      path = file_cache.provide_directory(
        cache_root,
        "service/package",
        "http://server/resources",
        first_digest,
      )
      self.assertEqual(first_digest, file_cache.uptodate_paths[path])

      file_cache.fetch_url = MagicMock(
        side_effect=[io.BytesIO(b"hash-2"), io.BytesIO(second_archive)]
      )
      file_cache.provide_directory(
        cache_root,
        "service/package",
        "http://server/resources",
        second_digest,
      )
      with open(os.path.join(path, "value")) as stream:
        self.assertEqual("second", stream.read())

  def test_waiter_retries_after_different_generation_failure(self):
    file_cache = FileCache(self.config)
    archive_bytes = self.create_archive_bytes("legacy-https")

    with tempfile.TemporaryDirectory() as cache_root:
      full_path = os.path.join(cache_root, "service", "package")
      state = {
        "event": threading.Event(),
        "error": CachingException("new generation failed"),
        "expected_archive_digest": "1" * 64,
      }
      file_cache.currently_providing[full_path] = state
      file_cache.fetch_url = MagicMock(
        side_effect=[io.BytesIO(b"hash"), io.BytesIO(archive_bytes)]
      )
      waiter_started = threading.Event()
      original_wait = state["event"].wait

      def wait_for_leader(timeout):
        waiter_started.set()
        return original_wait(timeout)

      state["event"].wait = wait_for_leader

      def finish_other_generation():
        self.assertTrue(waiter_started.wait(1))
        with file_cache.currently_providing_dict_lock:
          state["event"].set()
          del file_cache.currently_providing[full_path]

      finisher = Thread(target=finish_other_generation)
      finisher.start()
      try:
        self.assertEqual(
          full_path,
          file_cache.provide_directory(
            cache_root, "service/package", "https://server/resources"
          ),
        )
      finally:
        finisher.join(1)
      self.assertTrue(os.path.isdir(full_path))

  def test_empty_archive_fallback_requires_current_trusted_digest(self):
    file_cache = FileCache(self.config)
    expected_digest = "2" * 64

    with tempfile.TemporaryDirectory() as cache_root:
      full_path = os.path.join(cache_root, "service", "package")
      os.makedirs(full_path)
      with open(
        os.path.join(full_path, FileCache.ARCHIVE_DIGEST_FILE),
        "w",
        encoding="ascii",
      ) as stream:
        stream.write(expected_digest)
      file_cache.fetch_url = MagicMock(
        side_effect=[io.BytesIO(b"new-hash"), io.BytesIO(b"")]
      )

      self.assertEqual(
        full_path,
        file_cache.provide_directory(
          cache_root,
          "service/package",
          "http://server/resources",
          expected_digest,
        ),
      )

      file_cache.reset()
      file_cache.fetch_url = MagicMock(
        side_effect=[io.BytesIO(b"newer-hash"), io.BytesIO(b"")]
      )
      with self.assertRaisesRegex(CachingException, "archive is empty"):
        file_cache.provide_directory(
          cache_root,
          "service/package",
          "http://server/resources",
          "3" * 64,
        )

  def test_disabled_auto_update_requires_existing_matching_cache(self):
    self.config.set(
      AmbariConfig.AMBARI_PROPERTIES_CATEGORY,
      FileCache.ENABLE_AUTO_AGENT_CACHE_UPDATE_KEY,
      "false",
    )
    file_cache = FileCache(self.config)
    expected_digest = "4" * 64
    try:
      with tempfile.TemporaryDirectory() as cache_root:
        with self.assertRaisesRegex(CachingException, "does not exist"):
          file_cache.provide_directory(
            cache_root,
            "service/package",
            "http://server/resources",
            expected_digest,
          )

        full_path = os.path.join(cache_root, "service", "package")
        os.makedirs(full_path)
        with open(
          os.path.join(full_path, FileCache.ARCHIVE_DIGEST_FILE),
          "w",
          encoding="ascii",
        ) as stream:
          stream.write(expected_digest)
        self.assertEqual(
          full_path,
          file_cache.provide_directory(
            cache_root,
            "service/package",
            "http://server/resources",
            expected_digest,
          ),
        )
    finally:
      self.config.set(
        AmbariConfig.AMBARI_PROPERTIES_CATEGORY,
        FileCache.ENABLE_AUTO_AGENT_CACHE_UPDATE_KEY,
        "true",
      )

  @staticmethod
  def create_archive_bytes(value):
    archive = io.BytesIO()
    with zipfile.ZipFile(archive, "w") as zip_archive:
      zip_archive.writestr("value", value)
    return archive.getvalue()

  @patch("ambari_agent.FileCache.build_url_opener")
  def test_fetch_url(self, build_url_opener):
    fileCache = FileCache(self.config)
    remote_url = "http://dummy-url/"
    # Test normal download
    test_str = b"abc" * 100000  # Very long string
    test_string_io = io.BytesIO(test_str)
    test_buffer = MagicMock()
    test_buffer.read.side_effect = test_string_io.read
    build_url_opener.return_value.open.return_value.__enter__.return_value = test_buffer

    memory_buffer = fileCache.fetch_url(remote_url)

    self.assertEqual(memory_buffer.getvalue(), test_str)
    self.assertEqual(test_buffer.read.call_count, 20)  # depends on buffer size
    # Test exception handling
    test_buffer.read.side_effect = self.exc_side_effect
    try:
      fileCache.fetch_url(remote_url)
      self.fail("CachingException not thrown")
    except CachingException:
      pass  # Expected
    except Exception as e:
      self.fail("Unexpected exception thrown:" + str(e))

  @patch("ambari_agent.FileCache.build_url_opener")
  def test_fetch_url_applies_proxy_and_tls_policy_per_request(
    self, build_url_opener
  ):
    self.config.set("network", "use_system_proxy_settings", "false")
    ssl_context = self.config.get_server_ssl_context.return_value
    response = MagicMock()
    response.read.return_value = b""
    response.geturl.return_value = "https://server.example/resources/archive.zip"
    build_url_opener.return_value.open.return_value.__enter__.return_value = response

    FileCache(self.config).fetch_url("https://server.example/resources/archive.zip")

    build_url_opener.assert_called_once_with(True, ssl_context)

  @patch("ambari_agent.FileCache.build_url_opener")
  def test_fetch_url_rejects_https_redirect_to_http(self, build_url_opener):
    response = MagicMock()
    response.geturl.return_value = "http://server.example/resources/archive.zip"
    build_url_opener.return_value.open.return_value.__enter__.return_value = response

    with self.assertRaisesRegex(CachingException, "insecure URL"):
      FileCache(self.config).fetch_url(
        "https://server.example/resources/archive.zip"
      )

    response.read.assert_not_called()

  def test_read_write_hash_sum(self):
    tmpdir = tempfile.mkdtemp()
    dummyhash = b"DUMMY_HASH"
    fileCache = FileCache(self.config)
    fileCache.write_hash_sum(tmpdir, dummyhash)
    newhash = fileCache.read_hash_sum(tmpdir)
    self.assertEqual(newhash, dummyhash)
    shutil.rmtree(tmpdir)
    # Test read of not existing file
    newhash = fileCache.read_hash_sum(tmpdir)
    self.assertEqual(newhash, None)
    # Test write to not existing file
    with patch("builtins.open") as open_mock:
      open_mock.side_effect = self.exc_side_effect
      try:
        fileCache.write_hash_sum(tmpdir, dummyhash)
        self.fail("CachingException not thrown")
      except CachingException:
        pass  # Expected
      except Exception as e:
        self.fail("Unexpected exception thrown:" + str(e))

  def test_provide_directory_propagates_failure_to_concurrent_waiter(self):
    self.config.set("agent", "tolerate_download_failures", "false")
    file_cache = FileCache(self.config)
    fetch_started = threading.Event()
    release_fetch = threading.Event()
    errors = []

    def fail_fetch(_url):
      fetch_started.set()
      release_fetch.wait(2)
      raise CachingException("download failed")

    file_cache.fetch_url = MagicMock(side_effect=fail_fetch)

    def provide():
      try:
        file_cache.provide_directory(
          tempfile.gettempdir(), "shared-cache", "https://server/resources"
        )
      except Exception as error:
        errors.append(error)

    leader = Thread(target=provide)
    waiter = Thread(target=provide)
    leader.start()
    self.assertTrue(fetch_started.wait(1))
    provision_event = file_cache.currently_providing[
      os.path.join(tempfile.gettempdir(), "shared-cache")
    ]["event"]
    waiter_waiting = threading.Event()
    original_wait = provision_event.wait

    def wait_for_leader(timeout=None):
      waiter_waiting.set()
      return original_wait(timeout)

    provision_event.wait = wait_for_leader
    waiter.start()
    try:
      self.assertTrue(waiter_waiting.wait(1))
    finally:
      release_fetch.set()
      leader.join(2)
      waiter.join(2)

    self.assertFalse(leader.is_alive())
    self.assertFalse(waiter.is_alive())
    self.assertEqual(len(errors), 2)
    self.assertTrue(all(isinstance(error, CachingException) for error in errors))
    self.assertEqual(file_cache.currently_providing, {})

  def test_provide_directory_times_out_waiting_for_stalled_leader(self):
    file_cache = FileCache(self.config)
    file_cache.PROVISION_WAIT_TIMEOUT = 0.01
    full_path = os.path.join(tempfile.gettempdir(), "stalled-cache")
    file_cache.currently_providing[full_path] = {
      "event": threading.Event(),
      "error": None,
    }

    with self.assertRaisesRegex(CachingException, "Timed out"):
      file_cache.provide_directory(
        tempfile.gettempdir(), "stalled-cache", "https://server/resources"
      )

  def test_cache_subdirectory_cannot_escape_cache_root(self):
    file_cache = FileCache(self.config)
    with tempfile.TemporaryDirectory() as cache_root:
      with self.assertRaisesRegex(CachingException, "must be relative"):
        file_cache.provide_directory(cache_root, "/tmp/escape", "unused")
      with self.assertRaisesRegex(CachingException, "escapes the cache root"):
        file_cache.provide_directory(cache_root, "../escape", "unused")

  def test_unpack_archive_rejects_traversal_and_symlink(self):
    file_cache = FileCache(self.config)
    with tempfile.TemporaryDirectory() as target_directory:
      traversal_archive = io.BytesIO()
      with zipfile.ZipFile(traversal_archive, "w") as archive:
        archive.writestr("../escape", "bad")
      traversal_archive.seek(0)
      with self.assertRaisesRegex(CachingException, "unsafe path"):
        file_cache.unpack_archive(traversal_archive, target_directory)

      symlink_archive = io.BytesIO()
      with zipfile.ZipFile(symlink_archive, "w") as archive:
        symlink = zipfile.ZipInfo("link")
        symlink.create_system = 3
        symlink.external_attr = 0o120777 << 16
        archive.writestr(symlink, "target")
      symlink_archive.seek(0)
      with self.assertRaisesRegex(CachingException, "unsupported symlink"):
        file_cache.unpack_archive(symlink_archive, target_directory)

  def test_replace_directory_restores_previous_cache_on_install_failure(self):
    file_cache = FileCache(self.config)
    archive = io.BytesIO()
    with zipfile.ZipFile(archive, "w") as zip_archive:
      zip_archive.writestr("value", "new")
    archive.seek(0)

    with tempfile.TemporaryDirectory() as parent_directory:
      target_directory = os.path.join(parent_directory, "cache")
      os.makedirs(target_directory)
      with open(os.path.join(target_directory, "value"), "w") as stream:
        stream.write("old")
      real_replace = os.replace

      def fail_staging_install(source, destination):
        if "-staging-" in source and destination == target_directory:
          raise OSError("install failed")
        return real_replace(source, destination)

      with patch("ambari_agent.FileCache.os.replace", side_effect=fail_staging_install):
        with self.assertRaisesRegex(CachingException, "install failed"):
          file_cache.replace_directory(archive, target_directory, b"new-hash")

      with open(os.path.join(target_directory, "value")) as stream:
        self.assertEqual(stream.read(), "old")
      self.assertFalse(
        any("-previous-" in name for name in os.listdir(parent_directory))
      )

  def test_replace_directory_preserves_backup_when_rollback_fails(self):
    file_cache = FileCache(self.config)
    archive = io.BytesIO()
    with zipfile.ZipFile(archive, "w") as zip_archive:
      zip_archive.writestr("value", "new")
    archive.seek(0)

    with tempfile.TemporaryDirectory() as parent_directory:
      target_directory = os.path.join(parent_directory, "cache")
      os.makedirs(target_directory)
      with open(os.path.join(target_directory, "value"), "w") as stream:
        stream.write("old")
      real_replace = os.replace

      def fail_install_and_restore(source, destination):
        if destination == target_directory:
          raise OSError("destination unavailable")
        return real_replace(source, destination)

      with patch(
        "ambari_agent.FileCache.os.replace", side_effect=fail_install_and_restore
      ):
        with self.assertRaisesRegex(CachingException, "destination unavailable"):
          file_cache.replace_directory(archive, target_directory, b"new-hash")

      backups = [
        name for name in os.listdir(parent_directory) if "-previous-" in name
      ]
      self.assertEqual(len(backups), 1)
      with open(os.path.join(parent_directory, backups[0], "value")) as stream:
        self.assertEqual(stream.read(), "old")

  def test_replace_directory_restores_previous_cache_on_commit_fsync_failure(self):
    file_cache = FileCache(self.config)
    archive = io.BytesIO()
    with zipfile.ZipFile(archive, "w") as zip_archive:
      zip_archive.writestr("value", "new")
    archive.seek(0)

    with tempfile.TemporaryDirectory() as parent_directory:
      target_directory = os.path.join(parent_directory, "cache")
      os.makedirs(target_directory)
      with open(os.path.join(target_directory, "value"), "w") as stream:
        stream.write("old")
      real_fsync_directory = file_cache.fsync_directory
      commit_fsync_attempted = False

      def fail_first_parent_fsync(directory):
        nonlocal commit_fsync_attempted
        if not commit_fsync_attempted:
          commit_fsync_attempted = True
          raise OSError("directory fsync failed")
        return real_fsync_directory(directory)

      file_cache.fsync_directory = MagicMock(side_effect=fail_first_parent_fsync)
      with self.assertRaisesRegex(CachingException, "directory fsync failed"):
        file_cache.replace_directory(archive, target_directory, b"new-hash")

      with open(os.path.join(target_directory, "value")) as stream:
        self.assertEqual("old", stream.read())
      self.assertFalse(
        any(
          marker in name
          for name in os.listdir(parent_directory)
          for marker in ("-previous-", "-failed-", "-staging-")
        )
      )

  @patch("os.path.exists")
  @patch("os.path.isfile")
  @patch("os.path.isdir")
  @patch("os.unlink")
  @patch("shutil.rmtree")
  @patch("os.makedirs")
  def test_invalidate_directory(
    self, makedirs_mock, rmtree_mock, unlink_mock, isdir_mock, isfile_mock, exists_mock
  ):
    fileCache = FileCache(self.config)
    # Test execution flow if path points to file
    isfile_mock.return_value = True
    isdir_mock.return_value = False
    exists_mock.return_value = True

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
    exists_mock.return_value = True

    fileCache.invalidate_directory("dummy-dir")

    self.assertFalse(unlink_mock.called)
    self.assertTrue(rmtree_mock.called)
    self.assertTrue(makedirs_mock.called)

    unlink_mock.reset_mock()
    rmtree_mock.reset_mock()
    makedirs_mock.reset_mock()

    # Test execution flow if path points nowhere
    isfile_mock.return_value = False
    isdir_mock.return_value = False
    exists_mock.return_value = False

    fileCache.invalidate_directory("dummy-dir")

    self.assertFalse(unlink_mock.called)
    self.assertFalse(rmtree_mock.called)
    self.assertTrue(makedirs_mock.called)

    unlink_mock.reset_mock()
    rmtree_mock.reset_mock()
    makedirs_mock.reset_mock()

    # Test exception handling
    makedirs_mock.side_effect = self.exc_side_effect
    try:
      fileCache.invalidate_directory("dummy-dir")
      self.fail("CachingException not thrown")
    except CachingException:
      pass  # Expected
    except Exception as e:
      self.fail("Unexpected exception thrown:" + str(e))

  def test_unpack_archive(self):
    tmpdir = tempfile.mkdtemp()
    dummy_archive_name = os.path.join(
      "ambari_agent", "dummy_files", "dummy_archive.zip"
    )
    archive_file = open(dummy_archive_name, "rb")
    fileCache = FileCache(self.config)
    fileCache.unpack_archive(archive_file, tmpdir)
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
        fileCache.unpack_archive(archive_file, tmpdir)
        self.fail("CachingException not thrown")
      except CachingException:
        pass  # Expected
      except Exception as e:
        self.fail("Unexpected exception thrown:" + str(e))

  def tearDown(self):
    # enable stdout
    sys.stdout = sys.__stdout__

  def exc_side_effect(self, *a):
    raise Exception("horrible_exc")

  def caching_exc_side_effect(self, *a):
    raise CachingException("horrible_caching_exc")
