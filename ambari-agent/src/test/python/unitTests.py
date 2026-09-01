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

"""
SAMPLE USAGE:

python3 unitTests.py
python3 unitTests.py NameOfFile.py
python3 unitTests.py NameOfFileWithoutExtension  (this will append .* to the end, so it can match other file names too)

prepend _ to test file name(s) and run "python3 unitTests.py": execute only
  test files whose name begins with _ (useful for quick debug)

SETUP:
To run in Linux from command line,
cd to this same directory. Then make sure PYTHONPATH is correct.

export PYTHONPATH=$PYTHONPATH:$(pwd)/ambari-agent/src/test/python:
$(pwd)/ambari-common/src/test/python:
$(pwd)/ambari-agent/src/test/python/ambari_agent:
$(pwd)/ambari-common/src/main/python:
$(pwd)/ambari-server/src/main/resources/common-services/HDFS/2.1.0.2.0/package/files:
$(pwd)/ambari-server/src/test/python:
$(pwd)/ambari-agent/src/test/python/resource_management
"""

import re
import unittest
import fnmatch
import os
import signal
import sys
from os.path import isdir
import logging
from resource_management.core.logger import Logger
# TODO Add an option to randomize the tests' execution
# from random import shuffle

LOG_FILE_NAME = "tests.log"
SELECTED_PREFIX = "_"
PY_EXT = ".py"
TEST_TIMEOUT_SECONDS = int(os.environ.get("AMBARI_TEST_TIMEOUT_SECONDS", "300"))
CORE_TEST_MINIMUMS = {
  "TestHostInfo": 15,
  "TestCommandStatusDict": 5,
  "TestClusterConfigurationCache": 7,
  "TestCustomServiceOrchestrator": 9,
  "TestMain": 11,
  "TestAgentStompResponses": 3,
  "TestNetUtil": 5,
}


class TestTimeoutError(TimeoutError):
  pass


class TimeoutTextTestResult(unittest.TextTestResult):
  def startTest(self, test):
    super().startTest(test)
    if hasattr(signal, "SIGALRM") and TEST_TIMEOUT_SECONDS > 0:
      signal.signal(
        signal.SIGALRM,
        lambda signum, frame: (_ for _ in ()).throw(
          TestTimeoutError(
            f"Test exceeded {TEST_TIMEOUT_SECONDS} seconds: {test.id()}"
          )
        ),
      )
      signal.alarm(TEST_TIMEOUT_SECONDS)

  def stopTest(self, test):
    if hasattr(signal, "SIGALRM"):
      signal.alarm(0)
    super().stopTest(test)


class TestAgent(unittest.TestSuite):
  def run(self, result):
    run = unittest.TestSuite.run
    run(self, result)
    return result


def parent_dir(path):
  if isdir(path):
    if path.endswith(os.sep):
      path = os.path.dirname(path)
    parent_dir = os.path.dirname(path)
  else:
    parent_dir = os.path.dirname(os.path.dirname(path))

  return parent_dir


def get_test_files(path, mask=None, recursive=True):
  """
  Returns test files for path recursively
  """
  # Must convert mask so it can match a file
  if mask and mask != "" and not mask.endswith("*"):
    mask = mask + "*"

  file_list = []
  directory_items = os.listdir(path)

  for item in directory_items:
    add_to_pythonpath = False
    p = os.path.join(path, item)
    if os.path.isfile(p):
      if (
        mask is not None
        and fnmatch.fnmatch(item, mask)
        or mask is None
        and re.search(r"^_?[Tt]est.*\.py$", item)
      ):
        add_to_pythonpath = True
        file_list.append(p)
    elif os.path.isdir(p):
      if recursive:
        file_list.extend(get_test_files(p, mask=mask))
    if add_to_pythonpath:
      sys.path.append(path)

  return file_list


def all_tests_suite(custom_test_mask):
  test_mask = custom_test_mask if custom_test_mask else None

  src_dir = os.getcwd()
  files_list = get_test_files(src_dir, mask=test_mask)

  # TODO Add an option to randomize the tests' execution
  # shuffle(files_list)
  suites = []

  logger.info(
    "------------------------TESTS LIST:-------------------------------------"
  )
  # If test with special name exists, run only this test
  selected_test = None
  for test_path in files_list:
    file_name = os.path.basename(test_path)
    if (
      file_name.endswith(PY_EXT)
      and not file_name == __file__
      and file_name.startswith(SELECTED_PREFIX)
    ):
      logger.info("Running only selected test " + str(file_name))
      selected_test = file_name
  if selected_test is not None:
    selected_paths = [
      test_path
      for test_path in files_list
      if os.path.basename(test_path) == selected_test
    ]
  else:
    selected_paths = files_list
    for test_path in files_list:
      file_name = os.path.basename(test_path)
      if file_name.endswith(PY_EXT) and not file_name == __file__:
        logger.info(test_path)
  logger.info(
    "------------------------------------------------------------------------"
  )

  loader = unittest.TestLoader()
  for test_path in sorted(selected_paths):
    test_directory = os.path.dirname(test_path)
    module_name = os.path.splitext(os.path.basename(test_path))[0]
    original_sys_path = sys.path[:]
    try:
      sys.path[:] = [path for path in sys.path if path != test_directory]
      sys.path.insert(0, test_directory)
      sys.modules.pop(module_name, None)
      suites.append(
        loader.discover(
          start_dir=test_directory,
          pattern=os.path.basename(test_path),
          top_level_dir=test_directory,
        )
      )
    finally:
      sys.modules.pop(module_name, None)
      sys.path[:] = original_sys_path
  return unittest.TestSuite(suites)


def iter_tests(suite):
  for test in suite:
    if isinstance(test, unittest.TestSuite):
      yield from iter_tests(test)
    else:
      yield test


def validate_core_test_collection(suite, test_mask):
  selected_minimums = CORE_TEST_MINIMUMS
  if test_mask:
    pattern = test_mask if test_mask.endswith("*") else test_mask + "*"
    selected_minimums = {
      module: minimum
      for module, minimum in CORE_TEST_MINIMUMS.items()
      if fnmatch.fnmatch(module + PY_EXT, pattern)
    }

  collected_by_module = {module: 0 for module in selected_minimums}
  for test in iter_tests(suite):
    test_id_parts = test.id().split(".")
    for module in collected_by_module:
      if module in test_id_parts:
        collected_by_module[module] += 1

  failures = [
    f"{module}: collected {collected_by_module[module]}, expected at least {minimum}"
    for module, minimum in selected_minimums.items()
    if collected_by_module[module] < minimum
  ]
  if failures:
    logger.error("Core Python test collection guard failed: %s", "; ".join(failures))
    return False
  return True


def main():
  test_mask = None
  if len(sys.argv) >= 2:
    test_mask = sys.argv[1]

  logger.info(
    "------------------------------------------------------------------------"
  )
  logger.info("PYTHON AGENT TESTS")
  logger.info(
    "------------------------------------------------------------------------"
  )
  runner = unittest.TextTestRunner(
    verbosity=2,
    stream=sys.stdout,
    resultclass=TimeoutTextTestResult,
  )
  suite = all_tests_suite(test_mask)
  collected = suite.countTestCases()
  logger.info("Collected %s Python unit tests", collected)
  if collected == 0:
    logger.error("Python unit test discovery collected zero tests")
    return 1
  if not validate_core_test_collection(suite, test_mask):
    return 1
  status = runner.run(suite).wasSuccessful()

  if not status:
    logger.error(
      "-----------------------------------------------------------------------"
    )
    logger.error("Python unit tests failed")
    logger.error("Find detailed logs in " + LOG_FILE_NAME)
    logger.error(
      "-----------------------------------------------------------------------"
    )
    return 1
  else:
    logger.info(
      "------------------------------------------------------------------------"
    )
    logger.info("Python unit tests finished successfully")
    logger.info(
      "------------------------------------------------------------------------"
    )
  return 0


if __name__ == "__main__":
  pwd = os.path.abspath(__file__)
  ambari_agent_dir = os.path.dirname(
    os.path.dirname(os.path.dirname(os.path.dirname(pwd)))
  )
  src_dir = os.path.dirname(ambari_agent_dir)
  ambari_common_dir = os.path.join(src_dir, "ambari-common")

  sys.path.insert(0, os.path.join(ambari_agent_dir, "src", "main", "python"))
  sys.path.insert(
    0, os.path.join(ambari_agent_dir, "src", "main", "python", "ambari_agent")
  )
  sys.path.insert(0, os.path.join(ambari_common_dir, "src", "main", "python"))
  sys.path.insert(0, os.path.join(ambari_common_dir, "src", "test", "python"))

  logger = logging.getLogger()
  logger.setLevel(logging.INFO)
  formatter = logging.Formatter("[%(levelname)s] %(message)s")
  src_dir = os.getcwd()
  target_dir = parent_dir(parent_dir(parent_dir(src_dir))) + os.sep + "target"
  if not os.path.exists(target_dir):
    os.mkdir(target_dir)
  path = target_dir + os.sep + LOG_FILE_NAME
  file = open(path, "w")
  consoleLog = logging.StreamHandler(file)
  consoleLog.setFormatter(formatter)
  logger.addHandler(consoleLog)
  Logger.initialize_logger(logging_level=logging.WARNING)

  sys.exit(main())
