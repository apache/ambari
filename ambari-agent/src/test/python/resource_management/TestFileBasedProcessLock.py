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
import tempfile
import time
import shutil
from unittest import TestCase
from multiprocessing import Process
from only_for_platform import  not_for_platform, PLATFORM_WINDOWS
from resource_management.libraries.functions.file_based_process_lock import FileBasedProcessLock

class TestFileBasedProcessLock(TestCase):


  @not_for_platform(PLATFORM_WINDOWS)
  def test_file_based_lock(self):
    """
    Test BlockingLock using mkdir atomicity.
    """
    test_temp_dir = tempfile.mkdtemp(prefix="test_file_based_lock")
    try:
      indicator_dir = os.path.join(test_temp_dir, "indicator")
      lock_file = os.path.join(test_temp_dir, "lock")

      # Raises an exception if mkdir operation fails.
      # It indicates that more than one process acquired the lock.
      def dummy_task(index):
        with FileBasedProcessLock(lock_file):
          os.mkdir(indicator_dir)
          time.sleep(0.1)
          os.rmdir(indicator_dir)

      process_list = []
      for i in range(0, 3):
        p = Process(target=dummy_task, args=(i,))
        p.start()
        process_list.append(p)

      for p in process_list:
        p.join(2)
        self.assertEquals(p.exitcode, 0)

    finally:
      shutil.rmtree(test_temp_dir)

