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
import glob
import os
import sys
from random import shuffle

TEST_MASK = 'Test*.py'

def main():

  pwd = os.path.dirname(__file__)
  if pwd:
    global TEST_MASK
    TEST_MASK = pwd + os.sep + TEST_MASK

  tests = glob.glob(TEST_MASK)
  shuffle(tests)
  modules = [os.path.basename(s)[:-3] for s in tests]
  suites = [unittest.defaultTestLoader.loadTestsFromName(name) for name in
    modules]
  testSuite = unittest.TestSuite(suites)

  textRunner = unittest.TextTestRunner(verbosity=2).run(testSuite)
  return 0 if textRunner.wasSuccessful() else 1


if __name__ == "__main__":
  sys.exit(main())

