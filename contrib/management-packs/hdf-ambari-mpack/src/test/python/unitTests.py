#!/usr/bin/env python3

# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

import os
from pathlib import Path
import signal
import sys
import unittest


TEST_TIMEOUT_SECONDS = int(os.environ.get("AMBARI_TEST_TIMEOUT_SECONDS", "300"))


class TestTimeoutError(TimeoutError):
  pass


class TimeoutTextTestResult(unittest.TextTestResult):
  def startTest(self, test):
    super().startTest(test)
    if hasattr(signal, "SIGALRM") and TEST_TIMEOUT_SECONDS > 0:
      self._active_test = test
      signal.signal(signal.SIGALRM, self._raise_timeout)
      signal.alarm(TEST_TIMEOUT_SECONDS)

  def _raise_timeout(self, signum, frame):
    raise TestTimeoutError(
      f"Test exceeded {TEST_TIMEOUT_SECONDS} seconds: {self._active_test.id()}"
    )

  def stopTest(self, test):
    if hasattr(signal, "SIGALRM"):
      signal.alarm(0)
    super().stopTest(test)


def main():
  test_directory = Path(__file__).resolve().parent
  suite = unittest.defaultTestLoader.discover(
    str(test_directory), pattern="Test*.py", top_level_dir=str(test_directory)
  )
  collected = suite.countTestCases()
  sys.stderr.write(f"Collected {collected} Python unit tests\n")
  if collected == 0:
    sys.stderr.write("ERROR: Python unit test discovery collected zero tests\n")
    return 1
  result = unittest.TextTestRunner(
    verbosity=2, resultclass=TimeoutTextTestResult
  ).run(suite)
  return 0 if result.wasSuccessful() else 1


if __name__ == "__main__":
  sys.exit(main())
