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
from ambari_agent.LiveStatus import LiveStatus
from ambari_agent.AmbariConfig import AmbariConfig
import socket
import os, sys, StringIO

class TestLiveStatus(TestCase):

  def setUp(self):
    # disable stdout
    out = StringIO.StringIO()
    sys.stdout = out


  def tearDown(self):
    # enable stdout
    sys.stdout = sys.__stdout__

  def test_build(self):
    for component in LiveStatus.COMPONENTS:
      config = AmbariConfig().getConfig()
      config.set('agent', 'prefix', "dummy_files")
      livestatus = LiveStatus('', component['serviceName'], component['componentName'], {}, config)
      livestatus.versionsHandler.versionsFilePath = os.path.join("dummy_files","dummy_current_stack")
      result = livestatus.build()
      print "LiveStatus of {0}: {1}".format(component['serviceName'], str(result))
      self.assertEquals(len(result) > 0, True, 'Livestatus should not be empty')
      if component['componentName'] == 'GANGLIA_SERVER':
        self.assertEquals(result['stackVersion'],'{"stackName":"HDP","stackVersion":"1.2.2"}',
                      'Livestatus should contain component stack version')
  
