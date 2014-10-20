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

import json
import os
import time
import imp
from unittest import TestCase
from mock.mock import patch

class TestHDP22StackAdvisor(TestCase):

  def instantiate_stack_advisor(self, testDirectory, base_stack_advisor_path):
    hdp_206_stack_advisor_path = os.path.join(testDirectory, '../../../../../main/resources/stacks/HDP/2.0.6/services/stack_advisor.py')
    hdp_21_stack_advisor_path = os.path.join(testDirectory, '../../../../../main/resources/stacks/HDP/2.1/services/stack_advisor.py')
    hdp_22_stack_advisor_path = os.path.join(testDirectory, '../../../../../main/resources/stacks/HDP/2.2/services/stack_advisor.py')
    hdp_206_stack_advisor_classname = 'HDP22StackAdvisor'
    with open(base_stack_advisor_path, 'rb') as fp:
      imp.load_module('stack_advisor', fp, base_stack_advisor_path, ('.py', 'rb', imp.PY_SOURCE))
    with open(hdp_206_stack_advisor_path, 'rb') as fp:
      imp.load_module('stack_advisor_impl', fp, hdp_206_stack_advisor_path, ('.py', 'rb', imp.PY_SOURCE))
    with open(hdp_21_stack_advisor_path, 'rb') as fp:
      imp.load_module('stack_advisor_impl', fp, hdp_21_stack_advisor_path, ('.py', 'rb', imp.PY_SOURCE))
    with open(hdp_22_stack_advisor_path, 'rb') as fp:
      stack_advisor_impl = imp.load_module('stack_advisor_impl', fp, hdp_22_stack_advisor_path, ('.py', 'rb', imp.PY_SOURCE))
    clazz = getattr(stack_advisor_impl, hdp_206_stack_advisor_classname)
    return clazz()

  @patch('socket.getfqdn')
  def test_performance(self, getfqdn_method):
    getfqdn_method.side_effect = lambda host='perf400-a-1.c.pramod-thangali.internal': host
    testDirectory = os.path.dirname(os.path.abspath(__file__))
    old_stack_advisor_path = os.path.join(testDirectory, '../../../../../test/resources/stacks/old_stack_advisor.py')
    current_stack_advisor_path = os.path.join(testDirectory, '../../../../../main/resources/stacks/stack_advisor.py')

    for folder_name in ['1', '2']:
      stack_advisor_old = self.instantiate_stack_advisor(testDirectory, old_stack_advisor_path)
      services = json.load(open(os.path.join(testDirectory, folder_name + '/services.json')))
      hosts = json.load(open(os.path.join(testDirectory, folder_name + '/hosts.json')))
      start = time.time()
      recommendation_old = stack_advisor_old.recommendComponentLayout(services, hosts)
      time_taken = time.time() - start
      print "time taken by old stack_advisor.py = " + str(time_taken)

      stack_advisor = self.instantiate_stack_advisor(testDirectory, current_stack_advisor_path)
      start = time.time()
      recommendation = stack_advisor.recommendComponentLayout(services, hosts)
      time_taken = time.time() - start
      print "time taken by current stack_advisor.py = " + str(time_taken)

      self.assertEquals(recommendation, recommendation_old,
                        "current stack_advisor gives different results running on folder '" + folder_name + "'")


