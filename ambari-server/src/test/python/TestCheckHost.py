# !/usr/bin/env python

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
import socket
from resource_management import Script,ConfigDictionary
from mock.mock import patch
from unittest import TestCase

check_host = __import__('check_host')
from check_host import CheckHost

class TestCheckHost(TestCase):
  
  @patch("socket.gethostbyname")
  @patch.object(Script, 'get_config')
  @patch("resource_management.libraries.script.Script.put_structured_out")
  def testHostResolution(self, structured_out_mock, mock_config, mock_socket):
    mock_socket.return_value = "192.168.1.1"    
    jsonFilePath = os.path.join("../resources/custom_actions", "check_host_ip_addresses.json")
    
    with open(jsonFilePath, "r") as jsonFile:
      jsonPayload = json.load(jsonFile)
 
    mock_config.return_value = ConfigDictionary(jsonPayload)
    
    checkHost = CheckHost()
    checkHost.actionexecute(None)
    
    # ensure the correct function was called
    self.assertTrue(structured_out_mock.called)
    structured_out_mock.assert_called_with({'host_resolution_check': 
      {'failures': [], 
       'message': 'All hosts resolved to an IP address.', 
       'failed_count': 0, 
       'success_count': 5, 
       'exit_code': '0'}})
    
    # try it now with errors
    mock_socket.side_effect = socket.error
    checkHost.actionexecute(None)
    
    structured_out_mock.assert_called_with({'host_resolution_check': 
      {'failures': [
                    {'cause': (), 'host': u'c6401.ambari.apache.org', 'type': 'FORWARD_LOOKUP'}, 
                    {'cause': (), 'host': u'c6402.ambari.apache.org', 'type': 'FORWARD_LOOKUP'}, 
                    {'cause': (), 'host': u'c6403.ambari.apache.org', 'type': 'FORWARD_LOOKUP'}, 
                    {'cause': (), 'host': u'foobar', 'type': 'FORWARD_LOOKUP'}, 
                    {'cause': (), 'host': u'!!!', 'type': 'FORWARD_LOOKUP'}], 
       'message': 'There were 5 host(s) that could not resolve to an IP address.', 
       'failed_count': 5, 'success_count': 0, 'exit_code': '0'}})