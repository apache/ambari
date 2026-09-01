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

import io
import hashlib
import hmac
import json

import logging
import os
import shutil
import zipfile
import urllib.request, urllib.error, urllib.parse
import time
import threading
import tempfile

from ambari_commons.network import build_url_opener
from ambari_agent.Utils import execute_with_retries

logger = logging.getLogger()


class CachingException(Exception):
  pass


class FileCache:
  """
  Provides caching and lookup for service metadata files.
  If service metadata is not available at cache,
  downloads relevant files from the server.
  """

  CLUSTER_CACHE_DIRECTORY = "cluster_cache"
  ALERTS_CACHE_DIRECTORY = "alerts"
  RECOVERY_CACHE_DIRECTORY = "recovery"
  STACKS_CACHE_DIRECTORY = "stacks"
  COMMON_SERVICES_DIRECTORY = "common-services"
  CUSTOM_ACTIONS_CACHE_DIRECTORY = "custom_actions"
  EXTENSIONS_CACHE_DIRECTORY = "extensions"
  HOST_SCRIPTS_CACHE_DIRECTORY = "host_scripts"
  HASH_SUM_FILE = ".hash"
  ARCHIVE_NAME = "archive.zip"
  ARCHIVE_DIGEST_FILE = ".archive.sha256"
  RESOURCE_ARCHIVE_DIGESTS_KEY = "resource_archive_digests"
  ENABLE_AUTO_AGENT_CACHE_UPDATE_KEY = "agent.auto.cache.update"

  BLOCK_SIZE = 1024 * 16
  SOCKET_TIMEOUT = 10
  PROVISION_WAIT_TIMEOUT = 60

  def __init__(self, config):
    self.service_component_pool = {}
    self.config = config
    self.cache_dir = config.get("agent", "cache_dir")
    # Defines whether command should fail when downloading scripts
    # from the server is not possible or agent should rollback to local copy
    self.tolerate_download_failures = (
      config.get("agent", "tolerate_download_failures").lower() == "true"
    )
    self.currently_providing_dict_lock = threading.RLock()
    self.currently_providing = {}
    self.reset()

  def reset(self):
    # A path is reusable only for the exact trusted archive generation.
    self.uptodate_paths = {}

  def get_server_url_prefix(self, command):
    """
     Returns server url prefix if exists

    :type command dict
    """
    try:
      return command["ambariLevelParams"]["jdk_location"]
    except KeyError:
      return ""

  def get_trusted_archive_digest(self, command, subdirectory):
    raw_digests = command.get("commandParams", {}).get(
      self.RESOURCE_ARCHIVE_DIGESTS_KEY
    )
    if raw_digests is None:
      raw_digests = command.get("ambariLevelParams", {}).get(
        self.RESOURCE_ARCHIVE_DIGESTS_KEY
      )
    if raw_digests is None:
      return None
    try:
      digests = json.loads(raw_digests) if isinstance(raw_digests, str) else raw_digests
    except (TypeError, ValueError) as error:
      raise CachingException("Server resource archive digest metadata is invalid") from error
    if not isinstance(digests, dict):
      raise CachingException("Server resource archive digest metadata is not a map")

    normalized_subdirectory = subdirectory.replace(os.sep, "/")
    digest = digests.get(normalized_subdirectory)
    if digest is None:
      return None
    if not self.is_valid_archive_digest(digest):
      raise CachingException(
        f"Server resource archive digest is invalid for {subdirectory}"
      )
    return digest.lower()

  def get_service_base_dir(self, command):
    """
    Returns a base directory for service
    """
    if "service_package_folder" in command["commandParams"]:
      service_subpath = command["commandParams"]["service_package_folder"]
    else:
      service_subpath = command["serviceLevelParams"]["service_package_folder"]
    return self.provide_directory(
      self.cache_dir,
      service_subpath,
      self.get_server_url_prefix(command),
      self.get_trusted_archive_digest(command, service_subpath),
    )

  def get_hook_base_dir(self, command):
    """
    Returns a base directory for hooks
    """
    try:
      hooks_path = command["clusterLevelParams"]["hooks_folder"]
    except KeyError:
      return None
    return self.provide_directory(
      self.cache_dir,
      hooks_path,
      self.get_server_url_prefix(command),
      self.get_trusted_archive_digest(command, hooks_path),
    )

  def get_custom_actions_base_dir(self, command):
    """
    Returns a base directory for custom action scripts
    """
    return self.provide_directory(
      self.cache_dir,
      self.CUSTOM_ACTIONS_CACHE_DIRECTORY,
      self.get_server_url_prefix(command),
      self.get_trusted_archive_digest(command, self.CUSTOM_ACTIONS_CACHE_DIRECTORY),
    )

  def get_custom_resources_subdir(self, command):
    """
    Returns a custom directory which must be a subdirectory of the resources dir
    """
    try:
      custom_dir = command["commandParams"]["custom_folder"]
    except KeyError:
      return None

    return self.provide_directory(
      self.cache_dir,
      custom_dir,
      self.get_server_url_prefix(command),
      self.get_trusted_archive_digest(command, custom_dir),
    )

  def get_host_scripts_base_dir(self, command):
    """
    Returns a base directory for host scripts (host alerts, etc) which
    are scripts that are not part of the main agent code
    """
    return self.provide_directory(
      self.cache_dir,
      self.HOST_SCRIPTS_CACHE_DIRECTORY,
      self.get_server_url_prefix(command),
      self.get_trusted_archive_digest(command, self.HOST_SCRIPTS_CACHE_DIRECTORY),
    )

  def auto_cache_update_enabled(self):
    from ambari_agent.AmbariConfig import AmbariConfig

    if (
      self.config
      and self.config.has_option(
        AmbariConfig.AMBARI_PROPERTIES_CATEGORY,
        FileCache.ENABLE_AUTO_AGENT_CACHE_UPDATE_KEY,
      )
      and self.config.get(
        AmbariConfig.AMBARI_PROPERTIES_CATEGORY,
        FileCache.ENABLE_AUTO_AGENT_CACHE_UPDATE_KEY,
      ).lower()
      == "false"
    ):
      return False
    return True

  def provide_directory(
    self, cache_path, subdirectory, server_url_prefix, expected_archive_digest=None
  ):
    """
    Ensures that directory at cache is up-to-date. Throws a CachingException
    if any problems occur
    Parameters;
      cache_path: full path to cache directory
      subdirectory: subpath inside cache
      server_url_prefix: url of "resources" folder at the server
    """
    full_path = self.resolve_cache_path(cache_path, subdirectory)
    logger.debug(f"Trying to provide directory {subdirectory}")

    download_scheme = urllib.parse.urlparse(server_url_prefix).scheme.lower()
    if expected_archive_digest is not None and not self.is_valid_archive_digest(
      expected_archive_digest
    ):
      raise CachingException(f"Invalid trusted archive digest for {subdirectory}")
    if download_scheme == "http" and expected_archive_digest is None:
      raise CachingException(
        f"Refusing unauthenticated resource archive without a trusted digest: {subdirectory}"
      )

    if not self.auto_cache_update_enabled():
      if not os.path.isdir(full_path):
        raise CachingException(f"Cached resource directory does not exist: {full_path}")
      if expected_archive_digest is not None and not hmac.compare_digest(
        self.read_archive_digest(full_path) or "", expected_archive_digest
      ):
        raise CachingException(
          f"Cached resource archive does not match trusted metadata: {subdirectory}"
        )
      logger.debug("Auto cache update is disabled.")
      return full_path

    if expected_archive_digest is not None and hmac.compare_digest(
      self.uptodate_paths.get(full_path, ""), expected_archive_digest
    ) and hmac.compare_digest(
      self.read_archive_digest(full_path) or "", expected_archive_digest
    ):
      return full_path

    while True:
      provision_state = None
      with self.currently_providing_dict_lock:
        if full_path in self.currently_providing:
          provision_state = self.currently_providing[full_path]
        else:
          self.currently_providing[full_path] = {
            "event": threading.Event(),
            "error": None,
            "expected_archive_digest": expected_archive_digest,
          }

      if provision_state is None:
        break
      if not provision_state["event"].wait(self.PROVISION_WAIT_TIMEOUT):
        raise CachingException(
          f"Timed out waiting for concurrent cache update of {full_path}"
        )
      if provision_state["error"] is not None:
        if provision_state["expected_archive_digest"] == expected_archive_digest:
          raise provision_state["error"]
        continue
      if expected_archive_digest is None and os.path.isdir(full_path):
        return full_path
      if expected_archive_digest is not None and hmac.compare_digest(
        self.read_archive_digest(full_path) or "", expected_archive_digest
      ):
        return full_path

    try:
      logger.debug(f"Checking if update is available for directory {full_path}")
      # Need to check for updates at server
      remote_url = self.build_download_url(
        server_url_prefix, subdirectory, self.HASH_SUM_FILE
      )
      memory_buffer = self.fetch_url(remote_url)
      remote_hash = memory_buffer.getvalue().strip()
      local_hash = self.read_hash_sum(full_path)
      local_archive_digest = self.read_archive_digest(full_path)
      cache_is_current = bool(
        local_hash
        and local_hash == remote_hash
        and (
          expected_archive_digest is None
          or hmac.compare_digest(local_archive_digest or "", expected_archive_digest)
        )
      )
      if not cache_is_current:
        logger.debug(f"Updating directory {full_path}")
        download_url = self.build_download_url(
          server_url_prefix, subdirectory, self.ARCHIVE_NAME
        )
        membuffer = self.fetch_url(download_url)
        if not membuffer.getvalue().strip():
          raise CachingException(
            f"Downloaded archive is empty for {subdirectory}"
          )
        self.replace_directory(
          membuffer, full_path, remote_hash, expected_archive_digest
        )
        logger.info(f"Updated directory {full_path}")
        cache_is_current = True
      if cache_is_current and expected_archive_digest is not None:
        self.uptodate_paths[full_path] = expected_archive_digest
    except Exception as error:
      if (
        isinstance(error, CachingException)
        and self.tolerate_download_failures
        and os.path.isdir(full_path)
        and (
          (
            expected_archive_digest is not None
            and hmac.compare_digest(
              self.read_archive_digest(full_path) or "", expected_archive_digest
            )
          )
          or (
            download_scheme == "https"
            and expected_archive_digest is None
            and self.is_valid_archive_digest(self.read_archive_digest(full_path))
          )
        )
      ):
        # ignore
        if expected_archive_digest is not None:
          self.uptodate_paths[full_path] = expected_archive_digest
        logger.warning(
          "Error occurred during cache update. "
          "Error tolerate setting is set to true, so"
          " ignoring this error and continuing with current cache. "
          "Error details: {0}".format(str(error))
        )
      else:
        with self.currently_providing_dict_lock:
          self.currently_providing[full_path]["error"] = error
        raise
    finally:
      with self.currently_providing_dict_lock:
        self.currently_providing[full_path]["event"].set()
        del self.currently_providing[full_path]

    return full_path

  @staticmethod
  def resolve_cache_path(cache_path, subdirectory):
    if not isinstance(subdirectory, str) or not subdirectory:
      raise CachingException("Cache subdirectory must be a non-empty relative path")
    if os.path.isabs(subdirectory):
      raise CachingException(
        f"Cache subdirectory must be relative: {subdirectory}"
      )

    cache_root = os.path.realpath(os.path.abspath(cache_path))
    full_path = os.path.abspath(os.path.join(cache_root, subdirectory))
    resolved_path = os.path.realpath(full_path)
    try:
      is_contained = os.path.commonpath((cache_root, resolved_path)) == cache_root
    except ValueError:
      is_contained = False
    if not is_contained:
      raise CachingException(
        f"Cache subdirectory escapes the cache root: {subdirectory}"
      )
    return full_path

  def build_download_url(self, server_url_prefix, directory, filename):
    """
    Builds up a proper download url for file. Used for downloading files
    from the server.
    directory - relative path
    filename - file inside directory we are trying to fetch
    """
    return f"{server_url_prefix}/{urllib.request.pathname2url(directory)}/{filename}"

  def fetch_url(self, url):
    """
    Fetches content on url to in-memory buffer and returns the resulting buffer.
    May throw exceptions because of various reasons
    """
    logger.debug(f"Trying to download {url}")
    try:
      memory_buffer = io.BytesIO()
      ssl_context = (
        self.config.get_server_ssl_context()
        if urllib.parse.urlparse(url).scheme == "https"
        else None
      )
      opener = build_url_opener(
        not self.config.use_system_proxy_setting(), ssl_context
      )
      with opener.open(url, timeout=self.SOCKET_TIMEOUT) as response:
        final_url = response.geturl()
        if (
          urllib.parse.urlparse(url).scheme == "https"
          and urllib.parse.urlparse(final_url).scheme != "https"
        ):
          raise CachingException(
            f"Refusing HTTPS download redirected to an insecure URL: {final_url}"
          )
        logger.debug(
          f"Connected with {final_url} with code {response.getcode()}"
        )
        while True:
          buff = response.read(self.BLOCK_SIZE)
          if not buff:
            break
          memory_buffer.write(buff)
      return memory_buffer
    except Exception as err:
      raise CachingException(f"Can not download file from url {url} : {str(err)}")

  def read_hash_sum(self, directory):
    """
    Tries to read a hash sum from previously generated file. Returns string
    containing hash or None
    """
    hash_file = os.path.join(directory, self.HASH_SUM_FILE)
    try:
      with open(hash_file, "rb") as fh:
        return fh.readline().strip()
    except Exception:
      return None

  def read_archive_digest(self, directory):
    digest_file = os.path.join(directory, self.ARCHIVE_DIGEST_FILE)
    try:
      with open(digest_file, encoding="ascii") as stream:
        digest = stream.readline().strip().lower()
      return digest if self.is_valid_archive_digest(digest) else None
    except (OSError, UnicodeError):
      return None

  @staticmethod
  def is_valid_archive_digest(digest):
    return bool(
      isinstance(digest, str)
      and len(digest) == 64
      and all(character in "0123456789abcdefABCDEF" for character in digest)
    )

  def write_hash_sum(self, directory, new_hash):
    """
    Tries to read a hash sum from previously generated file. Returns string
    containing hash or None
    """
    hash_file = os.path.join(directory, self.HASH_SUM_FILE)
    try:
      with open(hash_file, "wb") as fh:
        fh.write(new_hash)
      os.chmod(hash_file, 0o644)
    except Exception as err:
      raise CachingException(f"Can not write to file {hash_file} : {str(err)}")

  def invalidate_directory(self, directory):
    """
    Recursively removes directory content (if any). Also, creates
    directory and any parent directories if needed. May throw exceptions
    on permission problems
    """
    CLEAN_DIRECTORY_TRIES = 5
    CLEAN_DIRECTORY_TRY_SLEEP = 0.25

    logger.debug(f"Invalidating directory {directory}")
    try:
      if os.path.exists(directory):
        if os.path.isfile(directory):  # It would be a strange situation
          os.unlink(directory)
        elif os.path.isdir(directory):
          """
          Execute shutil.rmtree(directory) multiple times.
          Reason: race condition, where a file (e.g. *.pyc) in deleted directory
          is created during function is running, causing it to fail.
          """
          execute_with_retries(
            CLEAN_DIRECTORY_TRIES,
            CLEAN_DIRECTORY_TRY_SLEEP,
            OSError,
            shutil.rmtree,
            directory,
          )
        # create directory itself and any parent directories
      os.makedirs(directory)
    except Exception as err:
      logger.exception(f"Can not invalidate cache directory {directory}")
      raise CachingException(
        f"Can not invalidate cache directory {directory}: {str(err)}"
      )

  def unpack_archive(self, mem_buffer, target_directory):
    """
    Unpacks contents of in-memory buffer to file system.
    In-memory buffer is expected to contain a valid zip archive
    """
    try:
      with zipfile.ZipFile(mem_buffer) as zfile:
        target_root = os.path.abspath(target_directory)
        for member in zfile.infolist():
          name = member.filename
          destination = os.path.abspath(os.path.join(target_root, name))
          if os.path.commonpath((target_root, destination)) != target_root:
            raise CachingException(f"Archive contains an unsafe path: {name}")
          if (member.external_attr >> 16) & 0o170000 == 0o120000:
            raise CachingException(
              f"Archive contains an unsupported symlink: {name}"
            )
          (dirname, filename) = os.path.split(name)
          concrete_dir = os.path.abspath(os.path.join(target_directory, dirname))
          if not os.path.isdir(concrete_dir):
            os.makedirs(concrete_dir)
          logger.debug(f"Unpacking file {name} to {concrete_dir}")
          if filename != "":
            zfile.extract(member, target_directory)
    except Exception as err:
      raise CachingException(
        f"Can not unpack zip file to directory {target_directory} : {str(err)}"
      )

  def replace_directory(
    self, mem_buffer, target_directory, remote_hash, expected_archive_digest=None
  ):
    archive_digest = hashlib.sha256(mem_buffer.getvalue()).hexdigest()
    if expected_archive_digest is not None and not hmac.compare_digest(
      archive_digest, expected_archive_digest
    ):
      raise CachingException(
        f"Downloaded resource archive digest does not match trusted metadata for {target_directory}"
      )

    parent_directory = os.path.dirname(target_directory)
    os.makedirs(parent_directory, exist_ok=True)
    staging_directory = tempfile.mkdtemp(
      prefix=f".{os.path.basename(target_directory)}-staging-",
      dir=parent_directory,
    )
    backup_directory = None
    failed_directory = None
    rollback_failed = False
    try:
      self.unpack_archive(mem_buffer, staging_directory)
      self.write_hash_sum(staging_directory, remote_hash)
      with open(
        os.path.join(staging_directory, self.ARCHIVE_DIGEST_FILE),
        "w",
        encoding="ascii",
      ) as stream:
        stream.write(archive_digest)
      self.fsync_directory_tree(staging_directory)

      if os.path.exists(target_directory):
        backup_directory = tempfile.mkdtemp(
          prefix=f".{os.path.basename(target_directory)}-previous-",
          dir=parent_directory,
        )
        os.rmdir(backup_directory)
        os.replace(target_directory, backup_directory)
      try:
        os.replace(staging_directory, target_directory)
        staging_directory = None
        self.fsync_directory(parent_directory)
      except Exception:
        try:
          if os.path.exists(target_directory):
            failed_directory = tempfile.mkdtemp(
              prefix=f".{os.path.basename(target_directory)}-failed-",
              dir=parent_directory,
            )
            os.rmdir(failed_directory)
            os.replace(target_directory, failed_directory)
          if backup_directory is not None:
            os.replace(backup_directory, target_directory)
            backup_directory = None
          self.fsync_directory(parent_directory)
        except Exception:
          rollback_failed = True
          logger.critical(
            "Failed to restore previous cache directory for %s; "
            "backup=%s failed_generation=%s",
            target_directory,
            backup_directory,
            failed_directory,
            exc_info=True,
          )
        raise

      if backup_directory is not None:
        previous_directory = backup_directory
        backup_directory = None
        shutil.rmtree(previous_directory, ignore_errors=True)
      try:
        self.fsync_directory(parent_directory)
      except OSError:
        logger.warning(
          "Failed to persist cleanup of the previous cache directory for %s",
          target_directory,
          exc_info=True,
        )
    except Exception as err:
      if isinstance(err, CachingException):
        raise
      raise CachingException(
        f"Can not atomically replace cache directory {target_directory}: {str(err)}"
      )
    finally:
      if staging_directory and os.path.exists(staging_directory):
        shutil.rmtree(staging_directory, ignore_errors=True)
      if failed_directory and os.path.exists(failed_directory) and not rollback_failed:
        shutil.rmtree(failed_directory, ignore_errors=True)
      if backup_directory and os.path.exists(backup_directory):
        logger.critical(
          "Preserving previous cache at %s after replacement failure for %s",
          backup_directory,
          target_directory,
        )

  @staticmethod
  def fsync_directory(directory):
    directory_fd = os.open(directory, os.O_RDONLY)
    try:
      os.fsync(directory_fd)
    finally:
      os.close(directory_fd)

  @staticmethod
  def fsync_directory_tree(directory):
    for root, _directories, files in os.walk(directory, topdown=False):
      for filename in files:
        file_descriptor = os.open(os.path.join(root, filename), os.O_RDONLY)
        try:
          os.fsync(file_descriptor)
        finally:
          os.close(file_descriptor)
      directory_descriptor = os.open(root, os.O_RDONLY)
      try:
        os.fsync(directory_descriptor)
      finally:
        os.close(directory_descriptor)
