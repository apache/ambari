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
from mock.mock import patch
from subprocess import Popen
import sys
setup_agent = __import__('setupAgent')

class TestSetupAgent(TestCase):

  @patch("sys.exit")
  @patch.object(setup_agent, 'is_suse')
  @patch.object(setup_agent, 'runAgent')
  @patch.object(setup_agent, 'configureAgent')
  @patch.object(setup_agent, 'installAgentSuse')
  @patch.object(setup_agent, 'checkServerReachability')
  @patch.object(setup_agent, 'checkAgentPackageAvailabilitySuse')
  @patch.object(setup_agent, 'checkAgentPackageAvailability')
  @patch.object(setup_agent, 'findNearestAgentPackageVersionSuse')
  @patch.object(setup_agent, 'findNearestAgentPackageVersion')
  def test_checkServerReachability(self, findNearestAgentPackageVersion_method,
                                                       findNearestAgentPackageVersionSuse_method,
                                                       checkAgentPackageAvailability_method,
                                                       checkAgentPackageAvailabilitySuse_method,
                                                       checkServerReachability_method,
                                                       installAgentSuse_method,
                                                       configureAgent_method,
                                                       runAgent_method,
                                                       is_suse_method,
                                                       exit_mock):
    
    checkServerReachability_method.return_value = {

    }
    
    checkAgentPackageAvailabilitySuse_method.return_value = {
     "exitstatus" : 0
    }
    
    findNearestAgentPackageVersionSuse_method.return_value = {
     "exitstatus" : 0,
     "log": ["1.1.1", ""]
    }
     
    installAgentSuse_method.return_value = {}
    configureAgent_method.return_value = {}
    runAgent_method.return_value = 0
    
        
    setup_agent.main(("/root/","password","1.1.1","8080"))
    self.assertTrue(checkServerReachability_method.called)
    pass


  @patch.object(setup_agent, 'is_suse')
  @patch.object(setup_agent, 'checkAgentPackageAvailabilitySuse')
  @patch.object(setup_agent, 'checkAgentPackageAvailability')
  @patch.object(setup_agent, 'findNearestAgentPackageVersionSuse')
  @patch.object(setup_agent, 'findNearestAgentPackageVersion')
  def test_returned_optimal_version_is_initial_on_suse(self, findNearestAgentPackageVersion_method,
                                                       findNearestAgentPackageVersionSuse_method,
                                                       checkAgentPackageAvailability_method,
                                                       checkAgentPackageAvailabilitySuse_method,
                                                       is_suse_method):
    is_suse_method.return_value = True
    checkAgentPackageAvailabilitySuse_method.return_value = {
      "exitstatus" : 0
    }

    projectVersion = "1.1.1"
    result_version = setup_agent.getOptimalVersion(projectVersion)

    self.assertTrue(checkAgentPackageAvailabilitySuse_method.called)
    self.assertFalse(checkAgentPackageAvailability_method.called)
    self.assertFalse(findNearestAgentPackageVersionSuse_method.called)
    self.assertFalse(findNearestAgentPackageVersion_method.called)
    self.assertTrue(result_version == projectVersion)
    pass

  @patch.object(setup_agent, 'is_suse')
  @patch.object(setup_agent, 'checkAgentPackageAvailabilitySuse')
  @patch.object(setup_agent, 'checkAgentPackageAvailability')
  @patch.object(setup_agent, 'findNearestAgentPackageVersionSuse')
  @patch.object(setup_agent, 'findNearestAgentPackageVersion')
  def test_returned_optimal_version_is_nearest_on_suse(self, findNearestAgentPackageVersion_method,
                                                       findNearestAgentPackageVersionSuse_method,
                                                       checkAgentPackageAvailability_method,
                                                       checkAgentPackageAvailabilitySuse_method,
                                                       is_suse_method):
    is_suse_method.return_value = True
    checkAgentPackageAvailabilitySuse_method.return_value = {
      "exitstatus" : 1
    }
    projectVersion = "1.1.1"
    nearest_version = projectVersion + ".1"
    findNearestAgentPackageVersionSuse_method.return_value = {
      "exitstatus" : 0,
      "log": [nearest_version, ""]
    }

    result_version = setup_agent.getOptimalVersion(projectVersion)

    self.assertTrue(checkAgentPackageAvailabilitySuse_method.called)
    self.assertFalse(checkAgentPackageAvailability_method.called)
    self.assertTrue(findNearestAgentPackageVersionSuse_method.called)
    self.assertFalse(findNearestAgentPackageVersion_method.called)
    self.assertTrue(result_version == nearest_version)
    pass

  @patch.object(setup_agent, 'is_suse')
  @patch.object(setup_agent, 'checkAgentPackageAvailabilitySuse')
  @patch.object(setup_agent, 'checkAgentPackageAvailability')
  @patch.object(setup_agent, 'findNearestAgentPackageVersionSuse')
  @patch.object(setup_agent, 'findNearestAgentPackageVersion')
  def test_returned_optimal_version_is_default_on_suse(self, findNearestAgentPackageVersion_method,
                                                       findNearestAgentPackageVersionSuse_method,
                                                       checkAgentPackageAvailability_method,
                                                       checkAgentPackageAvailabilitySuse_method,
                                                       is_suse_method):
    is_suse_method.return_value = True
    checkAgentPackageAvailabilitySuse_method.return_value = {
      "exitstatus" : 1
    }
    findNearestAgentPackageVersionSuse_method.return_value = {
      "exitstatus" : 0,
      "log": ["", ""]
    }

    projectVersion = "1.1.1"
    result_version = setup_agent.getOptimalVersion(projectVersion)

    self.assertTrue(checkAgentPackageAvailabilitySuse_method.called)
    self.assertFalse(checkAgentPackageAvailability_method.called)
    self.assertTrue(findNearestAgentPackageVersionSuse_method.called)
    self.assertFalse(findNearestAgentPackageVersion_method.called)
    self.assertTrue(result_version == "")
    pass

  @patch.object(setup_agent, 'is_suse')
  @patch.object(setup_agent, 'checkAgentPackageAvailabilitySuse')
  @patch.object(setup_agent, 'checkAgentPackageAvailability')
  @patch.object(setup_agent, 'findNearestAgentPackageVersionSuse')
  @patch.object(setup_agent, 'findNearestAgentPackageVersion')
  def test_returned_optimal_version_is_initial(self, findNearestAgentPackageVersion_method,
                                               findNearestAgentPackageVersionSuse_method,
                                               checkAgentPackageAvailability_method,
                                               checkAgentPackageAvailabilitySuse_method,
                                               is_suse_method):
    is_suse_method.return_value = False
    checkAgentPackageAvailability_method.return_value = {
      "exitstatus" : 0
    }

    projectVersion = "1.1.1"
    result_version = setup_agent.getOptimalVersion(projectVersion)

    self.assertFalse(checkAgentPackageAvailabilitySuse_method.called)
    self.assertTrue(checkAgentPackageAvailability_method.called)
    self.assertFalse(findNearestAgentPackageVersionSuse_method.called)
    self.assertFalse(findNearestAgentPackageVersion_method.called)
    self.assertTrue(result_version == projectVersion)
    pass

  @patch.object(setup_agent, 'is_suse')
  @patch.object(setup_agent, 'checkAgentPackageAvailabilitySuse')
  @patch.object(setup_agent, 'checkAgentPackageAvailability')
  @patch.object(setup_agent, 'findNearestAgentPackageVersionSuse')
  @patch.object(setup_agent, 'findNearestAgentPackageVersion')
  def test_returned_optimal_version_is_nearest(self, findNearestAgentPackageVersion_method,
                                               findNearestAgentPackageVersionSuse_method,
                                               checkAgentPackageAvailability_method,
                                               checkAgentPackageAvailabilitySuse_method,
                                               is_suse_method):
    is_suse_method.return_value = False
    checkAgentPackageAvailability_method.return_value = {
      "exitstatus" : 1
    }

    projectVersion = "1.1.1"
    nearest_version = projectVersion + ".1"
    findNearestAgentPackageVersion_method.return_value = {
      "exitstatus" : 0,
      "log": [nearest_version, ""]
    }

    result_version = setup_agent.getOptimalVersion(projectVersion)

    self.assertFalse(checkAgentPackageAvailabilitySuse_method.called)
    self.assertTrue(checkAgentPackageAvailability_method.called)
    self.assertFalse(findNearestAgentPackageVersionSuse_method.called)
    self.assertTrue(findNearestAgentPackageVersion_method.called)
    self.assertTrue(result_version == nearest_version)
    pass

  @patch.object(setup_agent, 'is_suse')
  @patch.object(setup_agent, 'checkAgentPackageAvailabilitySuse')
  @patch.object(setup_agent, 'checkAgentPackageAvailability')
  @patch.object(setup_agent, 'findNearestAgentPackageVersionSuse')
  @patch.object(setup_agent, 'findNearestAgentPackageVersion')
  def test_returned_optimal_version_is_default(self, findNearestAgentPackageVersion_method,
                                               findNearestAgentPackageVersionSuse_method,
                                               checkAgentPackageAvailability_method,
                                               checkAgentPackageAvailabilitySuse_method,
                                               is_suse_method):
    is_suse_method.return_value = False
    checkAgentPackageAvailability_method.return_value = {
      "exitstatus" : 1
    }
    findNearestAgentPackageVersion_method.return_value = {
      "exitstatus" : 0,
      "log": ["", ""]
    }

    projectVersion = "1.1.1"
    result_version = setup_agent.getOptimalVersion(projectVersion)

    self.assertFalse(checkAgentPackageAvailabilitySuse_method.called)
    self.assertTrue(checkAgentPackageAvailability_method.called)
    self.assertFalse(findNearestAgentPackageVersionSuse_method.called)
    self.assertTrue(findNearestAgentPackageVersion_method.called)
    self.assertTrue(result_version == "")
    pass