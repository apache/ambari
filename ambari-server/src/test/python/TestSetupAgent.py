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
import subprocess
from mock.mock import MagicMock
from unittest import TestCase
from mock.mock import patch
import sys
setup_agent = __import__('setupAgent')

class TestSetupAgent(TestCase):

  @patch("sys.exit")
  @patch("socket.socket")
  def test_checkServerReachability(self, socket_mock, exit_mock):
      setup_agent.checkServerReachability("localhost", 8080)
      self.assertTrue(socket_mock.called)
      s = socket_mock.return_value
      s.connect = MagicMock()
      def side_effect():
          raise Exception(1, "socket is closed")
      s.connect.side_effect = side_effect
      try:
          setup_agent.checkServerReachability("localhost", 8080)
          self.fail("Should throw exception because port is closed")
      except Exception:
      # Expected
          self.assertTrue(exit_mock.called)
          pass


  @patch.object(setup_agent, 'execOsCommand')
  def test_configureAgent(self, execOsCommand_mock):
    # Test if expected_hostname is passed
    hostname = "test.hst"
    setup_agent.configureAgent(hostname)
    cmdStr = str(execOsCommand_mock.call_args_list[0][0])
    self.assertTrue(hostname in cmdStr)


  @patch.object(setup_agent, 'execOsCommand')
  @patch("os.environ")
  @patch("subprocess.call")
  def test_runAgent(self, call_mock, environ_mock, execOsCommand_mock):
    expected_hostname = "test.hst"
    passphrase = "passphrase"
    call_mock.return_value = 0
    execOsCommand_mock.return_value = {'log': 'log', 'exitstatus': 0}
    # Test if expected_hostname is passed
    ret = setup_agent.runAgent(passphrase, expected_hostname)
    cmdStr = str(call_mock.call_args_list[0][0])
    self.assertTrue(expected_hostname in cmdStr)
    self.assertEqual(ret, 0)
    # Key 'log' not found
    execOsCommand_mock.return_value = None
    ret = setup_agent.runAgent(passphrase, expected_hostname)
    cmdStr = str(call_mock.call_args_list[0][0])
    self.assertTrue(expected_hostname in cmdStr)
    self.assertEqual(ret, 1)
    # Retcode id not 0
    execOsCommand_mock.return_value = {'log': 'log', 'exitstatus': 2}
    ret = setup_agent.runAgent(passphrase, expected_hostname)
    cmdStr = str(call_mock.call_args_list[0][0])
    self.assertTrue(expected_hostname in cmdStr)
    self.assertEqual(ret, 2)


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
    checkAgentPackageAvailabilitySuse_method.reset()
    checkAgentPackageAvailability_method.reset()
    findNearestAgentPackageVersionSuse_method.reset()
    findNearestAgentPackageVersion_method.reset()

    projectVersion = None
    result_version = None

    self.assertFalse(checkAgentPackageAvailabilitySuse_method.called)
    self.assertTrue(checkAgentPackageAvailability_method.called)
    self.assertFalse(findNearestAgentPackageVersionSuse_method.called)
    self.assertTrue(findNearestAgentPackageVersion_method.called)
    self.assertTrue(result_version == projectVersion)

    pass

  @patch.object(subprocess, 'Popen')
  def test_execOsCommand(self, Popen_mock):
    self.assertFalse(setup_agent.execOsCommand("hostname -f") == None)

  @patch("os.path.isfile")
  @patch("__builtin__.open")
  def test_is_suse(self, open_mock, isfile_mock):
    self.assertFalse(setup_agent.is_suse())
    isfile_mock.return_value = True
    f = open_mock.return_value
    f.read.return_value = " suse "
    self.assertTrue(setup_agent.is_suse())

  @patch.object(subprocess, 'Popen')
  def test_installAgentSuse(self, Popen_mock):
    self.assertFalse(setup_agent.installAgentSuse("1") == None)

  @patch.object(setup_agent, 'execOsCommand')
  def test_installPreReq(self, execOsCommand_mock):
    execOsCommand_mock.side_effect = [{"log": " epel "}, "hostname -f", {"log": " something "}, "hostname -f"]
    setup_agent.installPreReq()
    setup_agent.installPreReq()
    self.assertTrue(execOsCommand_mock.call_count == 4)


  @patch.object(setup_agent, 'runAgent')
  @patch.object(setup_agent, 'configureAgent')
  @patch.object(setup_agent, 'installAgent')
  @patch.object(setup_agent, 'installPreReq')
  @patch.object(setup_agent, 'installAgentSuse')
  @patch.object(setup_agent, 'is_suse')
  @patch.object(setup_agent, 'getOptimalVersion')
  @patch.object(setup_agent, 'checkServerReachability')
  @patch("sys.exit")
  @patch("os.path.dirname")
  @patch("os.path.realpath")
  def test_setup_agent_main(self, dirname_mock, realpath_mock, exit_mock, checkServerReachability_mock,
                            getOptimalVersion_mock, is_suse_mock, installAgentSuse_mock, installPreReq_mock,
                            installAgent_mock, configureAgent_mock, runAgent_mock):
    installPreReq_mock.return_value = {'log': 'log', 'exitstatus': 0}
    installAgent_mock.return_value = {'log': 'log', 'exitstatus': 0}
    installAgentSuse_mock.return_value = {'log': 'log', 'exitstatus': 0}
    runAgent_mock.return_value = 0
    setup_agent.main(("setupAgent.py","agents_host","password", "server_hostname","1.1.1","8080"))
    self.assertTrue(exit_mock.called)
    exit_mock.reset()
    setup_agent.main(("setupAgent.py","agents_host","password", "server_hostname","null","8080"))
    self.assertTrue(exit_mock.called)
    exit_mock.reset()
    is_suse_mock.return_value = False
    setup_agent.main(("setupAgent.py","agents_host","password", "server_hostname","null","null"))
    self.assertTrue(exit_mock.called)
    exit_mock.reset()
    def side_effect(retcode):
      raise Exception(retcode, "sys.exit")
    exit_mock.side_effect = side_effect
    #BUG-6769 Bootstrap does not fail on yum error
    #if "yum -y install epel-release" return not 0 result
    installPreReq_mock.return_value = {'log': 'log', 'exitstatus': 1}
    try:
        setup_agent.main(("setupAgent.py","agents_host","password", "server_hostname","1.1.1","8080"))
        self.fail("Should throw exception")
    except Exception:
        # Expected
        pass
    self.assertTrue(exit_mock.called)
    exit_mock.reset()
    #if "yum -y install --nogpgcheck ambari-agent" return not 0 result
    installPreReq_mock.return_value = {'log': 'log', 'exitstatus': 0}
    installAgent_mock.return_value = {'log': 'log', 'exitstatus': 1}
    try:
        setup_agent.main(("setupAgent.py","agents_host","password", "server_hostname","1.1.1","8080"))
        self.fail("Should throw exception")
    except Exception:
        # Expected
        pass
    self.assertTrue(exit_mock.called)
    exit_mock.reset()
    #if suse
    is_suse_mock.return_value = True
    #if "zypper install -y ambari-agent" return not 0 result
    installAgentSuse_mock.return_value = {'log': 'log', 'exitstatus': 1}
    try:
        setup_agent.main(("setupAgent.py","agents_host","password", "server_hostname","1.1.1","8080"))
        self.fail("Should throw exception")
    except Exception:
        # Expected
        pass
    self.assertTrue(exit_mock.called)


  @patch.object(setup_agent, 'execOsCommand')
  def test_checkAgentPackageAvailabilitySuse(self, execOsCommand_mock):
    setup_agent.checkAgentPackageAvailabilitySuse("1.1.1")
    self.assertTrue(execOsCommand_mock.called)

  @patch.object(setup_agent, 'execOsCommand')
  def test_checkAgentPackageAvailability(self, execOsCommand_mock):
      setup_agent.checkAgentPackageAvailability("1.1.1")
      self.assertTrue(execOsCommand_mock.called)

  @patch.object(setup_agent, 'execOsCommand')
  def test_findNearestAgentPackageVersionSuse(self, execOsCommand_mock):
      setup_agent.findNearestAgentPackageVersionSuse("1.1.1")
      self.assertTrue(execOsCommand_mock.called)

  @patch.object(setup_agent, 'execOsCommand')
  def test_findNearestAgentPackageVersion(self, execOsCommand_mock):
      setup_agent.findNearestAgentPackageVersion("1.1.1")
      self.assertTrue(execOsCommand_mock.called)

  @patch.object(setup_agent, 'execOsCommand')
  def test_installAgent(self, execOsCommand_mock):
    setup_agent.installAgent("1.1.1")
    self.assertTrue(execOsCommand_mock.called)