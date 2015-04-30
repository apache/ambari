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

from ambari_commons import OSCheck
from only_for_platform import get_platform, not_for_platform, only_for_platform, PLATFORM_WINDOWS, PLATFORM_LINUX
from mock.mock import MagicMock, patch, ANY, Mock

if get_platform() != PLATFORM_WINDOWS:
  os_distro_value = ('Suse','11','Final')
else:
  os_distro_value = ('win2012serverr2','6.3','WindowsServer')

with patch.object(OSCheck, "os_distribution", new = MagicMock(return_value = os_distro_value)):
#  from ambari_agent import NetUtil, security

#  if get_platform() != PLATFORM_WINDOWS:
#    from ambari_commons.shell import shellRunnerLinux

  setup_agent = __import__('setupAgent')

class TestSetupAgent(TestCase):

  @patch("sys.exit")
  @patch("socket.socket")
  def test_checkServerReachability(self, socket_mock, exit_mock):
    ret = setup_agent.checkServerReachability("localhost", 8080)
    self.assertTrue(socket_mock.called)

    s = socket_mock.return_value
    s.connect = MagicMock()
    def side_effect():
      raise Exception(1, "socket is closed")
    s.connect.side_effect = side_effect
    ret = setup_agent.checkServerReachability("localhost", 8080)
    self.assertTrue("exitstatus" in ret)
    self.assertEqual(ret["exitstatus"], 1)
    self.assertTrue("log" in ret)
    pass


  @patch.object(OSCheck, "os_distribution", new = MagicMock(return_value = os_distro_value))
  @patch.object(setup_agent, 'execOsCommand')
  def test_configureAgent(self, execOsCommand_mock):
    # Test if expected_hostname is passed
    hostname = "test.hst"
    setup_agent.configureAgent(hostname, "root")
    cmdStr = str(execOsCommand_mock.call_args_list[0][0])
    self.assertTrue(hostname in cmdStr)
    pass


  @patch.object(setup_agent, 'execOsCommand')
  @patch("os.environ")
  @patch("subprocess.Popen")
  @patch("time.sleep")
  def test_runAgent(self, sleep_mock, popen_mock, environ_mock, execOsCommand_mock):
    expected_hostname = "test.hst"
    passphrase = "passphrase"
    agent_status = MagicMock()
    agent_status.returncode = 0
    popen_mock.return_value = agent_status
    execOsCommand_mock.return_value = {'log': 'log', 'exitstatus': 0}
    # Test if expected_hostname is passed
    ret = setup_agent.runAgent(passphrase, expected_hostname, "root", False)
    cmdStr = str(popen_mock.call_args_list[0][0])
    self.assertTrue(expected_hostname in cmdStr)
    self.assertFalse('-v' in cmdStr)
    self.assertEqual(ret["exitstatus"], 0)
    self.assertTrue(sleep_mock.called)
    self.assertEqual(execOsCommand_mock.call_count, 1)

    execOsCommand_mock.reset_mock()
    popen_mock.reset_mock()
    sleep_mock.reset_mock()

    # Test if verbose=True
    ret = setup_agent.runAgent(passphrase, expected_hostname, "root", True)
    self.assertTrue(expected_hostname in cmdStr)
    cmdStr = str(popen_mock.call_args_list[0][0])
    self.assertTrue('-v' in cmdStr)
    self.assertEqual(ret["exitstatus"], 0)
    self.assertTrue(sleep_mock.called)
    self.assertEqual(execOsCommand_mock.call_count, 1)

    execOsCommand_mock.reset_mock()
    popen_mock.reset_mock()
    sleep_mock.reset_mock()

    # Key 'log' not found
    execOsCommand_mock.return_value = {'log': 'log', 'exitstatus': 1}
    ret = setup_agent.runAgent(passphrase, expected_hostname, "root", False)
    cmdStr = str(popen_mock.call_args_list[0][0])
    self.assertTrue(expected_hostname in cmdStr)
    self.assertEqual(ret["exitstatus"], 0)
    self.assertEqual(execOsCommand_mock.call_count, 3)

    execOsCommand_mock.reset_mock()
    popen_mock.reset_mock()

    # Retcode id not 0
    agent_status.returncode = 2
    execOsCommand_mock.return_value = {'log': 'log', 'exitstatus': 2}
    ret = setup_agent.runAgent(passphrase, expected_hostname, "root", False)
    cmdStr = str(popen_mock.call_args_list[0][0])
    self.assertTrue(expected_hostname in cmdStr)
    self.assertEqual(ret["exitstatus"], 2)
    execOsCommand_mock.reset_mock()
    pass

  @patch.object(OSCheck, "os_distribution", new = MagicMock(return_value = os_distro_value))
  @patch.object(setup_agent, 'getAvailableAgentPackageVersions')
  @patch('ambari_commons.OSCheck.is_suse_family')
  @patch('ambari_commons.OSCheck.is_ubuntu_family')
  @patch.object(setup_agent, 'findNearestAgentPackageVersion')
  def test_returned_optimal_version_is_initial_on_suse(self, findNearestAgentPackageVersion_method, is_ubuntu_family_method,
                                                       is_suse_family_method, getAvailableAgentPackageVersions_method):
    getAvailableAgentPackageVersions_method.return_value = {"exitstatus": 0, "log": "1.1.1"}
    is_suse_family_method.return_value = True
    is_ubuntu_family_method.return_value = False

    projectVersion = "1.1.1"
    result_version = setup_agent.getOptimalVersion(projectVersion)
    self.assertTrue(findNearestAgentPackageVersion_method.called)
    self.assertTrue(result_version["exitstatus"] == 1)
    pass

  @patch.object(OSCheck, "os_distribution", new = MagicMock(return_value = os_distro_value))
  @patch.object(setup_agent, 'getAvailableAgentPackageVersions')
  @patch('ambari_commons.OSCheck.is_suse_family')
  @patch('ambari_commons.OSCheck.is_ubuntu_family')
  @patch.object(setup_agent, 'findNearestAgentPackageVersion')
  def test_returned_optimal_version_is_initial_on_ubuntu(self, findNearestAgentPackageVersion_method, is_ubuntu_family_method,
                                                       is_suse_family_method, getAvailableAgentPackageVersions_method):
    getAvailableAgentPackageVersions_method.return_value = {"exitstatus": 0, "log": "1.1.1"}
    is_suse_family_method.return_value = False
    is_ubuntu_family_method.return_value = True

    projectVersion = "1.1.1"
    result_version = setup_agent.getOptimalVersion(projectVersion)
    self.assertTrue(findNearestAgentPackageVersion_method.called)
    self.assertTrue(result_version["exitstatus"] == 1)
    pass

  @patch.object(OSCheck, "os_distribution", new = MagicMock(return_value = os_distro_value))
  @patch('ambari_commons.OSCheck.is_suse_family')
  @patch('ambari_commons.OSCheck.is_ubuntu_family')
  @patch.object(setup_agent, 'findNearestAgentPackageVersion')
  def test_returned_optimal_version_is_nearest_on_suse(self, findNearestAgentPackageVersion_method,
                                                       is_ubuntu_family_method,
                                                       is_suse_family_method):
    is_suse_family_method.return_value = True
    is_ubuntu_family_method.return_value = False

    projectVersion = ""
    nearest_version = projectVersion + "1.1.1"
    findNearestAgentPackageVersion_method.return_value = {
      "exitstatus": 0,
      "log": [nearest_version, ""]
    }

    result_version = setup_agent.getOptimalVersion(projectVersion)
    self.assertTrue(findNearestAgentPackageVersion_method.called)
    self.assertTrue(result_version["exitstatus"] == 1)
    pass

  @patch.object(OSCheck, "os_distribution", new = MagicMock(return_value = os_distro_value))
  @patch('ambari_commons.OSCheck.is_suse_family')
  @patch('ambari_commons.OSCheck.is_ubuntu_family')
  @patch.object(setup_agent, 'findNearestAgentPackageVersion')
  def test_returned_optimal_version_is_nearest_on_ubuntu(self, findNearestAgentPackageVersion_method,
                                                       is_ubuntu_family_method,
                                                       is_suse_family_method):
    is_suse_family_method.return_value = False
    is_ubuntu_family_method.return_value = True

    projectVersion = ""
    nearest_version = projectVersion + "1.1.1"
    findNearestAgentPackageVersion_method.return_value = {
      "exitstatus": 0,
      "log": [nearest_version, ""]
    }

    result_version = setup_agent.getOptimalVersion(projectVersion)
    self.assertTrue(findNearestAgentPackageVersion_method.called)
    self.assertTrue(result_version["exitstatus"] == 1)
    pass

  @patch.object(OSCheck, "os_distribution", new = MagicMock(return_value = os_distro_value))
  @patch.object(setup_agent, 'getAvailableAgentPackageVersions')
  @patch('ambari_commons.OSCheck.is_suse_family')
  @patch('ambari_commons.OSCheck.is_ubuntu_family')
  @patch.object(setup_agent, 'findNearestAgentPackageVersion')
  def test_returned_optimal_version_is_initial(self, findNearestAgentPackageVersion_method,
                                               is_ubuntu_family_method,
                                               is_suse_family_method, getAvailableAgentPackageVersions_method):
    getAvailableAgentPackageVersions_method.return_value = {"exitstatus": 0, "log": "1.1.1"}
    is_suse_family_method.return_value = False
    is_ubuntu_family_method.return_value = False

    projectVersion = "1.1.1"
    result_version = setup_agent.getOptimalVersion(projectVersion)
    self.assertTrue(findNearestAgentPackageVersion_method.called)
    self.assertTrue(result_version["log"] == projectVersion)
    pass

  @patch.object(OSCheck, "os_distribution", new = MagicMock(return_value = os_distro_value))
  @patch.object(setup_agent, 'getAvailableAgentPackageVersions')
  @patch('ambari_commons.OSCheck.is_suse_family')
  @patch('ambari_commons.OSCheck.is_ubuntu_family')
  @patch.object(setup_agent, 'findNearestAgentPackageVersion')
  def test_returned_optimal_version_is_default(self, findNearestAgentPackageVersion_method,
                                               is_ubuntu_family_method,
                                               is_suse_family_method, getAvailableAgentPackageVersions_method):
    getAvailableAgentPackageVersions_method.return_value = {"exitstatus": 0, "log": "1.1.1"}
    is_suse_family_method.return_value = False
    is_ubuntu_family_method.return_value = False
    findNearestAgentPackageVersion_method.return_value = {
      "exitstatus": 0,
      "log": ["1.1.1.1", ""]
    }

    projectVersion = "1.1.1"
    result_version = setup_agent.getOptimalVersion(projectVersion)

    self.assertTrue(findNearestAgentPackageVersion_method.called)
    self.assertTrue(result_version["exitstatus"] == 1)
    pass

  @patch.object(OSCheck, "os_distribution", new = MagicMock(return_value = os_distro_value))
  @patch.object(subprocess, 'Popen')
  def test_execOsCommand(self, Popen_mock):
    self.assertFalse(setup_agent.execOsCommand("hostname -f") == None)

  @patch.object(OSCheck, "os_distribution", new = MagicMock(return_value = os_distro_value))
  @patch.object(setup_agent, 'checkVerbose')
  @patch.object(setup_agent, 'isAgentPackageAlreadyInstalled')
  @patch.object(setup_agent, 'runAgent')
  @patch.object(setup_agent, 'configureAgent')
  @patch.object(setup_agent, 'installAgent')
  @patch('ambari_commons.OSCheck.is_suse_family')
  @patch('ambari_commons.OSCheck.is_ubuntu_family')
  @patch.object(setup_agent, 'getOptimalVersion')
  @patch.object(setup_agent, 'checkServerReachability')
  @patch("sys.exit")
  @patch("os.path.dirname")
  @patch("os.path.realpath")
  def test_setup_agent_main(self, dirname_mock, realpath_mock, exit_mock, checkServerReachability_mock,
                            getOptimalVersion_mock, is_ubuntu_family_mock, is_suse_family_mock,
                            installAgent_mock, configureAgent_mock, runAgent_mock,
                            isAgentPackageAlreadyInstalled_mock, checkVerbose_mock):
    checkServerReachability_mock.return_value = {'log': 'log', 'exitstatus': 0}
    installAgent_mock.return_value = {'log': 'log', 'exitstatus': 0}
    configureAgent_mock.return_value = {'log': 'log', 'exitstatus': 0}
    runAgent_mock.return_value = {'log': 'log', 'exitstatus': 0}
    getOptimalVersion_mock.return_value = {'log': '1.1.2, 1.1.3, ', 'exitstatus': 1}
    ret = setup_agent.main(("setupAgent.py","agents_host","password", "server_hostname","1.1.1","8080"))
    self.assertTrue(checkVerbose_mock.called)
    self.assertFalse(exit_mock.called)
    self.assertTrue("exitstatus" in ret)
    self.assertEqual(ret["exitstatus"], 1)
    self.assertTrue(getOptimalVersion_mock.called)

    exit_mock.reset_mock()
    getOptimalVersion_mock.reset_mock()

    getOptimalVersion_mock.return_value = {'log': '1.1.1', 'exitstatus': 0}
    isAgentPackageAlreadyInstalled_mock.return_value = False
    is_suse_family_mock.return_value = True
    is_ubuntu_family_mock.return_value = False
    ret = setup_agent.main(("setupAgent.py","agents_host","password", "server_hostname","1.1.1","8080"))
    self.assertFalse(exit_mock.called)
    self.assertTrue(getOptimalVersion_mock.called)
    self.assertTrue(isAgentPackageAlreadyInstalled_mock.called)
    self.assertTrue(installAgent_mock.called)
    self.assertFalse(is_suse_family_mock.called)
    self.assertFalse(is_ubuntu_family_mock.called)
    self.assertTrue("exitstatus" in ret)
    self.assertEqual(ret["exitstatus"], 0)

    exit_mock.reset_mock()
    getOptimalVersion_mock.reset_mock()
    isAgentPackageAlreadyInstalled_mock.reset_mock()
    is_suse_family_mock.reset_mock()
    is_ubuntu_family_mock.reset_mock()
    installAgent_mock.reset_mock()

    getOptimalVersion_mock.return_value = {'log': '', 'exitstatus': 0}
    ret = setup_agent.main(("setupAgent.py","agents_host","password", "server_hostname","1.1.1","8080"))
    self.assertFalse(exit_mock.called)
    self.assertTrue(getOptimalVersion_mock.called)
    self.assertFalse(isAgentPackageAlreadyInstalled_mock.called)
    self.assertFalse(is_suse_family_mock.called)
    self.assertFalse(is_ubuntu_family_mock.called)
    self.assertTrue("exitstatus" in ret)
    self.assertEqual(ret["exitstatus"], 0)

    exit_mock.reset_mock()
    getOptimalVersion_mock.reset_mock()
    isAgentPackageAlreadyInstalled_mock.reset_mock()
    is_suse_family_mock.reset_mock()
    is_ubuntu_family_mock.reset_mock()
    installAgent_mock.reset_mock()

    is_suse_family_mock.return_value = False
    is_ubuntu_family_mock.return_value = False
    getOptimalVersion_mock.return_value = {'log': '1.1.1', 'exitstatus': 0}
    ret = setup_agent.main(("setupAgent.py","agents_host","password", "server_hostname","1.1.1","8080"))
    self.assertFalse(exit_mock.called)
    self.assertTrue(getOptimalVersion_mock.called)
    self.assertTrue(isAgentPackageAlreadyInstalled_mock.called)
    self.assertTrue(installAgent_mock.called)
    self.assertFalse(is_suse_family_mock.called)
    self.assertFalse(is_ubuntu_family_mock.called)
    self.assertTrue("exitstatus" in ret)
    self.assertEqual(ret["exitstatus"], 0)

    exit_mock.reset_mock()
    getOptimalVersion_mock.reset_mock()
    isAgentPackageAlreadyInstalled_mock.reset_mock()
    exit_mock.reset_mock()
    getOptimalVersion_mock.reset_mock()
    isAgentPackageAlreadyInstalled_mock.reset_mock()
    is_suse_family_mock.reset_mock()
    is_ubuntu_family_mock.reset_mock()
    installAgent_mock.reset_mock()

    ret = setup_agent.main(("setupAgent.py","agents_host","password", "server_hostname","{ambariVersion}","8080"))
    self.assertTrue(getOptimalVersion_mock.called)
    self.assertFalse(exit_mock.called)
    self.assertTrue("exitstatus" in ret)
    self.assertEqual(ret["exitstatus"], 0)

    exit_mock.reset_mock()
    getOptimalVersion_mock.reset_mock()
    ret = setup_agent.main(("setupAgent.py","agents_host","password", "server_hostname","null","8080"))
    self.assertFalse(exit_mock.called)
    self.assertTrue(getOptimalVersion_mock.called)
    self.assertTrue("exitstatus" in ret)
    self.assertEqual(ret["exitstatus"], 0)

    exit_mock.reset_mock()
    is_suse_family_mock.return_value = False
    is_ubuntu_family_mock.return_value = False
    ret = setup_agent.main(("setupAgent.py","agents_host","password", "server_hostname","null","null"))
    self.assertFalse(exit_mock.called)
    self.assertTrue("exitstatus" in ret)
    self.assertEqual(ret["exitstatus"], 0)

    exit_mock.reset_mock()
    #if "yum -y install --nogpgcheck ambari-agent" return not 0 result
    installAgent_mock.return_value = {'log': 'log', 'exitstatus': 1}
    ret = setup_agent.main(("setupAgent.py","agents_host","password", "server_hostname","1.1.1","8080"))
    self.assertFalse(exit_mock.called)
    self.assertTrue("exitstatus" in ret)
    self.assertEqual(ret["exitstatus"], 1)

    installAgent_mock.reset_mock()
    exit_mock.reset_mock()
    #if suse
    is_suse_family_mock.return_value = True
    is_ubuntu_family_mock.return_value = False
    #if "zypper install -y ambari-agent" return not 0 result
    installAgent_mock.return_value = {'log': 'log', 'exitstatus': 1}
    ret = setup_agent.main(("setupAgent.py","agents_host","password", "server_hostname","1.1.1","8080"))
    self.assertFalse(exit_mock.called)
    self.assertTrue("exitstatus" in ret)
    self.assertEqual(ret["exitstatus"], 1)

    exit_mock.reset_mock()
    #if ubuntu
    is_suse_family_mock.return_value = False
    is_ubuntu_family_mock.return_value = True

    installAgent_mock.return_value = {'log': 'log', 'exitstatus': 1}
    ret = setup_agent.main(("setupAgent.py","agents_host","password", "server_hostname","1.1.1","8080"))
    self.assertFalse(exit_mock.called)
    self.assertTrue("exitstatus" in ret)
    self.assertEqual(ret["exitstatus"], 1)
    pass

  @patch.object(setup_agent, 'execOsCommand')
  def test_findNearestAgentPackageVersion(self, execOsCommand_mock):
    setup_agent.findNearestAgentPackageVersion("1.1.1")
    self.assertTrue(execOsCommand_mock.called)
    execOsCommand_mock.reset_mock()
    setup_agent.findNearestAgentPackageVersion("")
    self.assertTrue(execOsCommand_mock.called)
    pass

  @patch.object(setup_agent, 'execOsCommand')
  def test_isAgentPackageAlreadyInstalled(self, execOsCommand_mock):
    execOsCommand_mock.return_value = {"exitstatus": 0, "log": "1.1.1"}
    self.assertTrue(setup_agent.isAgentPackageAlreadyInstalled("1.1.1"))
    self.assertTrue(execOsCommand_mock.called)
    execOsCommand_mock.reset_mock()
    execOsCommand_mock.return_value = {"exitstatus": 1, "log": "1.1.1"}
    self.assertFalse(setup_agent.isAgentPackageAlreadyInstalled("1.1.1"))
    self.assertTrue(execOsCommand_mock.called)
    pass

  @patch.object(setup_agent, 'execOsCommand')
  def test_getAvailableAgentPackageVersions(self, execOsCommand_mock):
    setup_agent.getAvailableAgentPackageVersions()
    self.assertTrue(execOsCommand_mock.called)
    pass

  @patch.object(OSCheck, "os_distribution", new = MagicMock(return_value = os_distro_value))
  @patch.object(setup_agent, 'execOsCommand')
  def test_installAgent(self, execOsCommand_mock):
    setup_agent.installAgent("1.1.1")
    self.assertTrue(execOsCommand_mock.called)
    pass
