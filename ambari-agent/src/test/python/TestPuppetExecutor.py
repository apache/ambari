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

from unittest import TestCase
from puppetExecutor import puppetExecutor
from pprint import pformat
import sys

class TestPuppetExecutor(TestCase):
  def test_build(self):
    puppetexecutor = puppetExecutor("/tmp", "/x", "/y", "/z")
    command = puppetexecutor.puppetCommand("site.pp")
    self.assertEquals("/x/bin/puppet", command[0], "puppet binary wrong")
    self.assertEquals("apply", command[1], "local apply called")
    self.assertEquals("--confdir=/tmp", command[2],"conf dir tmp")
    self.assertEquals("--detailed-exitcodes", command[3], "make sure output \
    correct")
    
    
    
