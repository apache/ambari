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

import unittest
import doctest
from os.path import dirname, split, isdir
import logging.handlers
import logging

LOG_FILE_NAME='tests.log'
SELECTED_PREFIX = "_"
PY_EXT='.py'

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


def all_tests_suite():


  src_dir = os.getcwd()
  files_list=os.listdir(src_dir)
  tests_list = []

  logger.info('------------------------TESTS LIST:-------------------------------------')
  # If test with special name exists, run only this test
  selected_test = None
  for file_name in files_list:
    if file_name.endswith(PY_EXT) and not file_name == __file__ and file_name.startswith(SELECTED_PREFIX):
      logger.info("Running only selected test " + str(file_name))
      selected_test = file_name
  if selected_test is not None:
      tests_list.append(selected_test.replace(PY_EXT, ''))
  else:
    for file_name in files_list:
      if file_name.endswith(PY_EXT) and not file_name == __file__:
        logger.info(file_name)
        tests_list.append(file_name.replace(PY_EXT, ''))
  logger.info('------------------------------------------------------------------------')

  suite = unittest.TestLoader().loadTestsFromNames(tests_list)
  return TestAgent([suite])

def main():

  logger.info('------------------------------------------------------------------------')
  logger.info('PYTHON AGENT TESTS')
  logger.info('------------------------------------------------------------------------')
  runner = unittest.TextTestRunner(verbosity=2, stream=sys.stdout)
  suite = all_tests_suite()
  result = runner.run(suite)
  for error in result.errors:
    logger.error('Failed test:' + error[0]._testMethodName + '\n' + error[1])
  for failure in result.failures:
    logger.error('Failed test:' + failure[0]._testMethodName + '\n' + failure[1])
  status = result.wasSuccessful()

  if not status:
    logger.error('-----------------------------------------------------------------------')
    logger.error('Python unit tests failed')
    logger.error('Find detailed logs in ' + path)
    logger.error('-----------------------------------------------------------------------')
    exit(1)
  else:
    logger.info('------------------------------------------------------------------------')
    logger.info('Python unit tests finished succesfully')
    logger.info('------------------------------------------------------------------------')

if __name__ == '__main__':
  import os
  import sys
  import io
  sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__)))))
  sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__)))) + os.sep + 'main' + os.sep + 'python')
  sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__)))) + os.sep + 'main' + os.sep + 'python' + os.sep + 'ambari_agent')
  logger = logging.getLogger()
  logger.setLevel(logging.INFO)
  formatter = logging.Formatter("[%(levelname)s] %(message)s")
  src_dir = os.getcwd()
  target_dir = parent_dir(parent_dir(parent_dir(src_dir))) + os.sep + 'target'
  if not os.path.exists(target_dir):
    os.mkdir(target_dir)
  path = target_dir + os.sep + LOG_FILE_NAME
  file=open(path, "w")
  consoleLog = logging.StreamHandler(file)
  consoleLog.setFormatter(formatter)
  logger.addHandler(consoleLog)
  main()
