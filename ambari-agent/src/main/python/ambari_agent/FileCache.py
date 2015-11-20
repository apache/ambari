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
import StringIO

import logging
import os
import shutil
import zipfile
import urllib2
import urllib
from AmbariConfig import AmbariConfig

logger = logging.getLogger()

class CachingException(Exception):
  pass

class FileCache():
  """
  Provides caching and lookup for service metadata files.
  If service metadata is not available at cache,
  downloads relevant files from the server.
  """

  CLUSTER_CONFIGURATION_CACHE_DIRECTORY="cluster_configuration"
  ALERTS_CACHE_DIRECTORY="alerts"
  RECOVERY_CACHE_DIRECTORY="recovery"
  STACKS_CACHE_DIRECTORY="stacks"
  COMMON_SERVICES_DIRECTORY="common-services"
  CUSTOM_ACTIONS_CACHE_DIRECTORY="custom_actions"
  HOST_SCRIPTS_CACHE_DIRECTORY="host_scripts"
  HASH_SUM_FILE=".hash"
  ARCHIVE_NAME="archive.zip"
  ENABLE_AUTO_AGENT_CACHE_UPDATE_KEY = "agent.auto.cache.update"

  BLOCK_SIZE=1024*16
  SOCKET_TIMEOUT=10

  def __init__(self, config):
    self.service_component_pool = {}
    self.config = config
    self.cache_dir = config.get('agent', 'cache_dir')
    # Defines whether command should fail when downloading scripts
    # from the server is not possible or agent should rollback to local copy
    self.tolerate_download_failures = \
          config.get('agent','tolerate_download_failures').lower() == 'true'
    self.reset()


  def reset(self):
    self.uptodate_paths = [] # Paths that already have been recently checked


  def get_service_base_dir(self, command, server_url_prefix):
    """
    Returns a base directory for service
    """
    service_subpath = command['commandParams']['service_package_folder']
    return self.provide_directory(self.cache_dir, service_subpath,
                                  server_url_prefix)


  def get_hook_base_dir(self, command, server_url_prefix):
    """
    Returns a base directory for hooks
    """
    try:
      hooks_subpath = command['commandParams']['hooks_folder']
    except KeyError:
      return None
    subpath = os.path.join(self.STACKS_CACHE_DIRECTORY, hooks_subpath)
    return self.provide_directory(self.cache_dir, subpath,
                                  server_url_prefix)


  def get_custom_actions_base_dir(self, server_url_prefix):
    """
    Returns a base directory for custom action scripts
    """
    return self.provide_directory(self.cache_dir,
                                  self.CUSTOM_ACTIONS_CACHE_DIRECTORY,
                                  server_url_prefix)


  def get_host_scripts_base_dir(self, server_url_prefix):
    """
    Returns a base directory for host scripts (host alerts, etc) which
    are scripts that are not part of the main agent code
    """
    return self.provide_directory(self.cache_dir,
                                  self.HOST_SCRIPTS_CACHE_DIRECTORY,
                                  server_url_prefix)


  def auto_cache_update_enabled(self):
    if self.config and \
        self.config.has_option(AmbariConfig.AMBARI_PROPERTIES_CATEGORY, FileCache.ENABLE_AUTO_AGENT_CACHE_UPDATE_KEY) and \
            self.config.get(AmbariConfig.AMBARI_PROPERTIES_CATEGORY, FileCache.ENABLE_AUTO_AGENT_CACHE_UPDATE_KEY).lower() == "false":
      return False
    return True

  def provide_directory(self, cache_path, subdirectory, server_url_prefix):
    """
    Ensures that directory at cache is up-to-date. Throws a CachingException
    if any problems occur
    Parameters;
      cache_path: full path to cache directory
      subdirectory: subpath inside cache
      server_url_prefix: url of "resources" folder at the server
    """

    full_path = os.path.join(cache_path, subdirectory)
    logger.debug("Trying to provide directory {0}".format(subdirectory))

    if not self.auto_cache_update_enabled():
      logger.debug("Auto cache update is disabled.")
      return full_path

    try:
      if full_path not in self.uptodate_paths:
        logger.debug("Checking if update is available for "
                     "directory {0}".format(full_path))
        # Need to check for updates at server
        remote_url = self.build_download_url(server_url_prefix,
                                             subdirectory, self.HASH_SUM_FILE)
        memory_buffer = self.fetch_url(remote_url)
        remote_hash = memory_buffer.getvalue().strip()
        local_hash = self.read_hash_sum(full_path)
        if not local_hash or local_hash != remote_hash:
          logger.debug("Updating directory {0}".format(full_path))
          download_url = self.build_download_url(server_url_prefix,
                                                 subdirectory, self.ARCHIVE_NAME)
          membuffer = self.fetch_url(download_url)
          self.invalidate_directory(full_path)
          self.unpack_archive(membuffer, full_path)
          self.write_hash_sum(full_path, remote_hash)
        # Finally consider cache directory up-to-date
        self.uptodate_paths.append(full_path)
    except CachingException, e:
      if self.tolerate_download_failures:
        # ignore
        logger.warn("Error occurred during cache update. "
                    "Error tolerate setting is set to true, so"
                    " ignoring this error and continuing with current cache. "
                    "Error details: {0}".format(str(e)))
      else:
        raise # we are not tolerant to exceptions, command execution will fail
    return full_path


  def build_download_url(self, server_url_prefix,
                         directory, filename):
    """
    Builds up a proper download url for file. Used for downloading files
    from the server.
    directory - relative path
    filename - file inside directory we are trying to fetch
    """
    return "{0}/{1}/{2}".format(server_url_prefix,
                                urllib.pathname2url(directory), filename)


  def fetch_url(self, url):
    """
    Fetches content on url to in-memory buffer and returns the resulting buffer.
    May throw exceptions because of various reasons
    """
    logger.debug("Trying to download {0}".format(url))
    try:
      memory_buffer = StringIO.StringIO()
      proxy_handler = urllib2.ProxyHandler({})
      opener = urllib2.build_opener(proxy_handler)
      u = opener.open(url, timeout=self.SOCKET_TIMEOUT)
      logger.debug("Connected with {0} with code {1}".format(u.geturl(),
                                                             u.getcode()))
      buff = u.read(self.BLOCK_SIZE)
      while buff:
        memory_buffer.write(buff)
        buff = u.read(self.BLOCK_SIZE)
        if not buff:
          break
      return memory_buffer
    except Exception, err:
      raise CachingException("Can not download file from"
                             " url {0} : {1}".format(url, str(err)))


  def read_hash_sum(self, directory):
    """
    Tries to read a hash sum from previously generated file. Returns string
    containing hash or None
    """
    hash_file = os.path.join(directory, self.HASH_SUM_FILE)
    try:
      with open(hash_file) as fh:
        return fh.readline().strip()
    except:
      return None # We don't care


  def write_hash_sum(self, directory, new_hash):
    """
    Tries to read a hash sum from previously generated file. Returns string
    containing hash or None
    """
    hash_file = os.path.join(directory, self.HASH_SUM_FILE)
    try:
      with open(hash_file, "w") as fh:
        fh.write(new_hash)
      os.chmod(hash_file, 0o666)
    except Exception, err:
      raise CachingException("Can not write to file {0} : {1}".format(hash_file,
                                                                 str(err)))


  def invalidate_directory(self, directory):
    """
    Recursively removes directory content (if any). Also, creates
    directory and any parent directories if needed. May throw exceptions
    on permission problems
    """
    logger.debug("Invalidating directory {0}".format(directory))
    try:
      if os.path.exists(directory):
        if os.path.isfile(directory): # It would be a strange situation
          os.unlink(directory)
        elif os.path.isdir(directory):
          shutil.rmtree(directory)
      # create directory itself and any parent directories
      os.makedirs(directory)
    except Exception, err:
      raise CachingException("Can not invalidate cache directory {0}: {1}",
                             directory, str(err))


  def unpack_archive(self, mem_buffer, target_directory):
    """
    Unpacks contents of in-memory buffer to file system.
    In-memory buffer is expected to contain a valid zip archive
    """
    try:
      zfile = zipfile.ZipFile(mem_buffer)
      for name in zfile.namelist():
        (dirname, filename) = os.path.split(name)
        concrete_dir=os.path.abspath(os.path.join(target_directory, dirname))
        if not os.path.isdir(concrete_dir):
          os.makedirs(concrete_dir)
        logger.debug("Unpacking file {0} to {1}".format(name, concrete_dir))
        if filename!='':
          zfile.extract(name, target_directory)
    except Exception, err:
      raise CachingException("Can not unpack zip file to "
                             "directory {0} : {1}".format(
                            target_directory, str(err)))
