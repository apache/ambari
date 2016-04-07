#!/usr/bin/env python
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

Ambari Agent

"""

import fcntl

from resource_management.core.logger import Logger

class FileBasedProcessLock(object):
  """A file descriptor based lock for interprocess locking.
  The lock is automatically released when process dies.

  WARNING: Do not use this lock for synchronization between threads.
  Multiple threads in a same process can simultaneously acquire this lock.
  It should be used only for locking between processes.
  """

  def __init__(self, lock_file_path):
    """
    :param lock_file_path: The path to the file used for locking
    """
    self.lock_file_name = lock_file_path
    self.lock_file = None

  def blocking_lock(self):
    """
    Creates the lock file if it doesn't exist.
    Waits to acquire an exclusive lock on the lock file descriptor.
    """
    Logger.info("Trying to acquire a lock on {0}".format(self.lock_file_name))
    if self.lock_file is None or self.lock_file.closed:
      self.lock_file = open(self.lock_file_name, 'a')
    fcntl.lockf(self.lock_file, fcntl.LOCK_EX)
    Logger.info("Acquired the lock on {0}".format(self.lock_file_name))

  def unlock(self):
    """
    Unlocks the lock file descriptor.
    """
    Logger.info("Releasing the lock on {0}".format(self.lock_file_name))
    fcntl.lockf(self.lock_file, fcntl.LOCK_UN)
    try:
      if self.lock_file is not None:
        self.lock_file.close()
        self.lock_file = None
    except IOError:
      pass

  def __enter__(self):
    self.blocking_lock()
    return None

  def __exit__(self, exc_type, exc_val, exc_tb):
    self.unlock()
    return False