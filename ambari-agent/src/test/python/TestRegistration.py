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
from ambari_agent.Register import Register
import socket
import os, pprint, json

class TestRegistration(TestCase):

  def test_registration_build(self):
    register = Register()
    data = register.build(1)
    #print ("Register: " + pprint.pformat(data))
    self.assertEquals(len(data['hardwareProfile']) > 0, True, "hardwareProfile should contain content")
    self.assertEquals(data['hostname'] != "", True, "hostname should not be empty")
    self.assertEquals(data['publicHostname'] != "", True, "publicHostname should not be empty")
    self.assertEquals(data['responseId'], 1)
    self.assertEquals(data['timestamp'] > 1353678475465L, True, "timestamp should not be empty")
    self.assertEquals(len(data), 5)
