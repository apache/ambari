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

class TestStackAdvisorPerformance(TestCase):

  TIME_ALLOWED = 0.2 # somewhat arbitrary, based on test runs

  def setUp(self):
    self.testDirectory = os.path.dirname(os.path.abspath(__file__))

  def instantiate_stack_advisor(self):
    self.load_stack_advisor('main/resources/stacks/stack_advisor.py', 'stack_advisor')

    stack_advisors = (
      'main/resources/stacks/HDP/2.0.6/services/stack_advisor.py',
      'main/resources/stacks/HDP/2.1/services/stack_advisor.py',
      'main/resources/stacks/HDP/2.2/services/stack_advisor.py',
      'main/resources/stacks/HDP/2.3/services/stack_advisor.py',
      'main/resources/stacks/HDP/2.4/services/stack_advisor.py',
      'main/resources/stacks/HDP/2.5/services/stack_advisor.py',
      'main/resources/stacks/HDP/2.6/services/stack_advisor.py',
    )

    for filename in stack_advisors:
      stack_advisor_impl = self.load_stack_advisor(filename, 'stack_advisor_impl')

    current_stack_advisor_classname = 'HDP26StackAdvisor'
    clazz = getattr(stack_advisor_impl, current_stack_advisor_classname)
    return clazz()


  def load_stack_advisor(self, filename, module_name):
    path = os.path.join(self.testDirectory, '../../../../..', filename)
    with open(path, 'rb') as fp:
      return imp.load_module(module_name, fp, path, ('.py', 'rb', imp.PY_SOURCE))


  @patch('socket.getfqdn')
  def test_performance(self, getfqdn_method):
    getfqdn_method.side_effect = lambda host='perf400-a-1.c.pramod-thangali.internal': host

    for folder_name in ['1', '2']:
      with open(os.path.join(self.testDirectory, folder_name, 'services.json')) as fp:
        services = json.load(fp)
      with open(os.path.join(self.testDirectory, folder_name, 'hosts.json')) as fp:
        hosts = json.load(fp)

      stack_advisor = self.instantiate_stack_advisor()

      start = time.time()
      recommendation = stack_advisor.recommendComponentLayout(services, hosts)
      time_taken = time.time() - start
      print "Current stack advisor elapsed {0}, allowed {1}".format(time_taken, TestStackAdvisorPerformance.TIME_ALLOWED)

      self.assertTrue(time_taken < TestStackAdvisorPerformance.TIME_ALLOWED) # Python 2.7: assertLess


