#!/usr/bin/env python2.6

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


class TestAgent(unittest.TestSuite):
  def run(self, result):
    run = unittest.TestSuite.run
    run(self, result)
    return result

def all_tests_suite():
  suite = unittest.TestLoader().loadTestsFromNames([
    'TestHeartbeat',
    'TestHardware',
    'TestServerStatus',
    'TestFileUtil',
    'TestActionQueue',
    #'TestAmbariComponent',
    'TestAgentActions',
    'TestCertGeneration'
  ])
  return TestAgent([suite])

def main():

  logger.info('------------------------------------------------------------------------')
  logger.info('PYTHON AGENT TESTS')
  logger.info('------------------------------------------------------------------------')
  parent_dir = lambda x: split(x)[0] if isdir(x) else split(dirname(x))[0]
  src_dir = os.getcwd()
  target_dir = parent_dir(parent_dir(parent_dir(src_dir))) + os.sep + 'target'
  if not os.path.exists(target_dir):
    os.mkdir(target_dir)
  path = target_dir + os.sep + LOG_FILE_NAME
  file=open(path, "w")
  runner = unittest.TextTestRunner(stream=file)
  suite = all_tests_suite()

  status = runner.run(suite).wasSuccessful()

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
  consoleLog = logging.StreamHandler(sys.stdout)
  consoleLog.setFormatter(formatter)
  logger.addHandler(consoleLog)
  main()
