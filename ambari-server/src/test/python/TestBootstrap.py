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

import bootstrap
import time
import subprocess
import os
import logging

from bootstrap import SCP
from bootstrap import PSCP
from bootstrap import SSH
from bootstrap import PSSH
from bootstrap import BootStrap
from unittest import TestCase
from subprocess import Popen
from bootstrap import AMBARI_PASSPHRASE_VAR_NAME
from mock.mock import MagicMock, call
from mock.mock import patch
from mock.mock import create_autospec

class TestBootstrap(TestCase):

  def setUp(self):
    logging.basicConfig(level=logging.ERROR)

  #Timout is specified in bootstrap.HOST_BOOTSTRAP_TIMEOUT, default is 300 seconds
  def test_return_failed_status_for_hanging_ssh_threads_after_timeout(self):
    bootstrap.HOST_BOOTSTRAP_TIMEOUT = 1
    forever_hanging_timeout = 5
    SSH.run = lambda self: time.sleep(forever_hanging_timeout)
    pssh = PSSH(["hostname"], "root", "sshKeyFile", "bootdir", command="command")
    self.assertTrue(pssh.ret == {})
    starttime = time.time()
    pssh.run()
    self.assertTrue(pssh.ret != {})
    self.assertTrue(time.time() - starttime < forever_hanging_timeout)
    self.assertTrue(pssh.ret["hostname"]["log"] == "FAILED")
    self.assertTrue(pssh.ret["hostname"]["exitstatus"] == -1)

  #Timout is specified in bootstrap.HOST_BOOTSTRAP_TIMEOUT, default is 300 seconds
  def test_return_failed_status_for_hanging_scp_threads_after_timeout(self):
    bootstrap.HOST_BOOTSTRAP_TIMEOUT = 1
    forever_hanging_timeout = 5
    SCP.run = lambda self: time.sleep(forever_hanging_timeout)
    pscp = PSCP(["hostname"], "root", "sshKeyFile", "inputfile", "remote", "bootdir")
    self.assertTrue(pscp.ret == {})
    starttime = time.time()
    pscp.run()
    self.assertTrue(pscp.ret != {})
    self.assertTrue(time.time() - starttime < forever_hanging_timeout)
    self.assertTrue(pscp.ret["hostname"]["log"] == "FAILED")
    self.assertTrue(pscp.ret["hostname"]["exitstatus"] == -1)

  @patch.object(SCP, "writeLogToFile")
  @patch.object(SSH, "writeLogToFile")
  @patch.object(Popen, "communicate")
  def test_return_error_message_for_missing_sudo_package(self, communicate_method,
                                                         SSH_writeLogToFile_method,
                                                         SCP_writeLogToFile_method):
    SCP_writeLogToFile_method.return_value = None
    SSH_writeLogToFile_method.return_value = None
    communicate_method.return_value = ("", "")
    bootstrap = BootStrap(["hostname"], "root", "sshKeyFile", "scriptDir", "bootdir", "setupAgentFile", "ambariServer", "centos6", None, "8440")
    bootstrap.statuses = {
      "hostname" : {
        "exitstatus" : 0,
        "log" : ""
      }
    }
    ret = bootstrap.checkSudoPackage()
    self.assertTrue("Error: Sudo command is not available. Please install the sudo command." in bootstrap.statuses["hostname"]["log"])

  @patch.object(SCP, "writeLogToFile")
  @patch.object(SSH, "writeLogToFile")
  @patch.object(Popen, "communicate")
  @patch.object(BootStrap, "createDoneFiles")
  @patch.object(BootStrap, "deletePasswordFile")
  @patch.object(BootStrap, "changePasswordFileModeOnHost")
  def test_copy_and_delete_password_file_methods_are_called_for_user_with_password(self,
                                                                                   changePasswordFileModeOnHost_method,
                                                                                   deletePasswordFile_method,
                                                                                   createDoneFiles_method,
                                                                                   communicate_method,
                                                                                   SSH_writeLogToFile_method,
                                                                                   SCP_writeLogToFile_method):
    SCP_writeLogToFile_method.return_value = None
    SSH_writeLogToFile_method.return_value = None
    communicate_method.return_value = ("", "")
    createDoneFiles_method.return_value = None

    deletePasswordFile_method.return_value = 0

    changePasswordFileModeOnHost_method.return_value = 0

    os.environ[AMBARI_PASSPHRASE_VAR_NAME] = ""
    bootstrap = BootStrap(["hostname"], "user", "sshKeyFile", "scriptDir", "bootdir", "setupAgentFile", "ambariServer", "centos6", None, "8440", "passwordFile")
    def side_effect():
      bootstrap.copyPasswordFile_called = True
      bootstrap.hostlist_to_remove_password_file = ["hostname"]
      return 0
    bootstrap.copyPasswordFile = side_effect
    ret = bootstrap.run()
    self.assertTrue(bootstrap.copyPasswordFile_called)
    self.assertTrue(deletePasswordFile_method.called)
    self.assertTrue(changePasswordFileModeOnHost_method.called)

  @patch.object(SCP, "writeLogToFile")
  @patch.object(SSH, "writeLogToFile")
  @patch.object(Popen, "communicate")
  @patch.object(BootStrap, "createDoneFiles")
  @patch.object(BootStrap, "deletePasswordFile")
  @patch.object(BootStrap, "changePasswordFileModeOnHost")
  def test_copy_and_delete_password_file_methods_are_not_called_for_passwordless_user(self,
                                                                                      changePasswordFileModeOnHost_method,
                                                                                      deletePasswordFile_method,
                                                                                      createDoneFiles_method,
                                                                                      communicate_method,
                                                                                      SSH_writeLogToFile_method,
                                                                                      SCP_writeLogToFile_method):
    SCP_writeLogToFile_method.return_value = None
    SSH_writeLogToFile_method.return_value = None
    communicate_method.return_value = ("", "")
    createDoneFiles_method.return_value = None

    deletePasswordFile_method.return_value = 0
    changePasswordFileModeOnHost_method.return_value = 0

    os.environ[AMBARI_PASSPHRASE_VAR_NAME] = ""
    bootstrap = BootStrap(["hostname"], "user", "sshKeyFile", "scriptDir", "bootdir", "setupAgentFile", "ambariServer", "centos6", None, "8440")
    bootstrap.copyPasswordFile_called = False
    def side_effect():
      bootstrap.copyPasswordFile_called = True
      bootstrap.hostlist_to_remove_password_file = ["hostname"]
      return 0
    bootstrap.copyPasswordFile = side_effect
    ret = bootstrap.run()
    self.assertFalse(bootstrap.copyPasswordFile_called)
    self.assertFalse(deletePasswordFile_method.called)
    self.assertFalse(changePasswordFileModeOnHost_method.called)

  @patch.object(SCP, "writeLogToFile")
  @patch.object(SSH, "writeLogToFile")
  @patch.object(Popen, "communicate")
  @patch.object(BootStrap, "createDoneFiles")
  @patch.object(BootStrap, "getRunSetupWithPasswordCommand")
  @patch.object(BootStrap, "getMoveRepoFileWithPasswordCommand")
  def test_commands_with_password_are_called_for_user_with_password(self, getMoveRepoFileWithPasswordCommand_method,
                                                                    getRunSetupWithPasswordCommand_method,
                                                                    createDoneFiles_method,
                                                                    communicate_method,
                                                                    SSH_writeLogToFile_method,
                                                                    SCP_writeLogToFile_method):
    SCP_writeLogToFile_method.return_value = None
    SSH_writeLogToFile_method.return_value = None
    communicate_method.return_value = ("", "")
    createDoneFiles_method.return_value = None

    getRunSetupWithPasswordCommand_method.return_value = ""
    getMoveRepoFileWithPasswordCommand_method.return_value = ""

    os.environ[AMBARI_PASSPHRASE_VAR_NAME] = ""
    hosts = ["hostname"]
    bootstrap = BootStrap(["hostname"], "user", "sshKeyFile", "scriptDir", "bootdir", "setupAgentFile", "ambariServer", "centos6", None, "8440", "passwordFile")
    bootstrap.successive_hostlist = hosts
    bootstrap.copyOsCheckScript()
    bootstrap.successive_hostlist = hosts
    bootstrap.copyNeededFiles()
    bootstrap.successive_hostlist = hosts
    bootstrap.runSetupAgent()
    self.assertTrue(getRunSetupWithPasswordCommand_method.called)
    self.assertTrue(getMoveRepoFileWithPasswordCommand_method.called)

  @patch.object(SCP, "writeLogToFile")
  @patch.object(SSH, "writeLogToFile")
  @patch.object(Popen, "communicate")
  @patch.object(BootStrap, "createDoneFiles")
  @patch.object(BootStrap, "getRunSetupWithoutPasswordCommand")
  @patch.object(BootStrap, "getMoveRepoFileWithoutPasswordCommand")
  def test_commands_without_password_are_called_for_passwordless_user(self, getMoveRepoFileWithoutPasswordCommand_method,
                                                                      getRunSetupWithoutPasswordCommand_method,
                                                                      createDoneFiles_method,
                                                                      communicate_method,
                                                                      SSH_writeLogToFile_method,
                                                                      SCP_writeLogToFile_method):
    SCP_writeLogToFile_method.return_value = None
    SSH_writeLogToFile_method.return_value = None
    communicate_method.return_value = ("", "")
    createDoneFiles_method.return_value = None

    getRunSetupWithoutPasswordCommand_method.return_value = ""
    getMoveRepoFileWithoutPasswordCommand_method.return_value = ""

    os.environ[AMBARI_PASSPHRASE_VAR_NAME] = ""
    hosts = ["hostname"]
    bootstrap = BootStrap(["hostname"], "user", "sshKeyFile", "scriptDir", "bootdir", "setupAgentFile", "ambariServer", "centos6", None, "8440")
    bootstrap.successive_hostlist = hosts
    bootstrap.copyOsCheckScript()
    bootstrap.successive_hostlist = hosts
    bootstrap.copyNeededFiles()
    bootstrap.successive_hostlist = hosts
    bootstrap.runSetupAgent()
    self.assertTrue(getRunSetupWithoutPasswordCommand_method.called)
    self.assertTrue(getMoveRepoFileWithoutPasswordCommand_method.called)


  @patch.object(BootStrap, "runSetupAgent")
  @patch.object(BootStrap, "copyNeededFiles")
  @patch.object(BootStrap, "checkSudoPackage")
  @patch.object(BootStrap, "runOsCheckScript")
  @patch.object(BootStrap, "copyOsCheckScript")
  @patch.object(BootStrap, "createDoneFiles")
  def test_os_check_performed(self, createDoneFiles_method, copyOsCheckScript_method,
                              runOsCheckScript_method, checkSudoPackage_method,
                              copyNeededFiles_method, runSetupAgent_method):
    createDoneFiles_method.return_value = None

    copyOsCheckScript_method.return_value = 0
    runOsCheckScript_method.return_value = 0
    checkSudoPackage_method.return_value = 0
    copyNeededFiles_method.return_value = 0
    runSetupAgent_method.return_value = 0

    os.environ[AMBARI_PASSPHRASE_VAR_NAME] = ""
    bootstrap = BootStrap(["hostname"], "user", "sshKeyFile", "scriptDir",
                          "bootdir", "setupAgentFile", "ambariServer",
                          "centos6", None, "8440")
    ret = bootstrap.run()
    self.assertTrue(copyOsCheckScript_method.called)
    self.assertTrue(runOsCheckScript_method.called)
    self.assertTrue(ret == 0)

  @patch.object(PSCP, "run")
  @patch.object(PSCP, "getstatus")
  def test_copyOsCheckScript(self, getstatus_method, run_method):
    getstatus_method.return_value = {
      "hostname" : {
        "exitstatus" : 0,
        "log" : ""
      }
    }
    os.environ[AMBARI_PASSPHRASE_VAR_NAME] = ""
    bootstrap = BootStrap(["hostname"], "user", "sshKeyFile", "scriptDir",
                          "bootdir", "setupAgentFile", "ambariServer",
                          "centos6", None, "8440")
    res = bootstrap.copyOsCheckScript()
    self.assertTrue(run_method.called)
    self.assertTrue(getstatus_method.called)
    self.assertTrue(res == 0)
    pass

  @patch.object(PSSH, "run")
  @patch.object(PSSH, "getstatus")
  def test_runOsCheckScript_success(self, getstatus_method, run_method):
    good_stats = {
      "hostname" : {
        "exitstatus" : 0,
        "log" : ""
      }
    }
    getstatus_method.return_value = good_stats
    os.environ[AMBARI_PASSPHRASE_VAR_NAME] = ""
    bootstrap = BootStrap(["hostname"], "user", "sshKeyFile", "scriptDir",
                          "bootdir", "setupAgentFile", "ambariServer",
                          "centos6", None, "8440")
    bootstrap.statuses = good_stats
    bootstrap.runOsCheckScript()

    self.assertTrue(run_method.called)
    self.assertTrue(getstatus_method.called)
    self.assertTrue("hostname" in bootstrap.successive_hostlist)
    pass

  @patch.object(PSSH, "run")
  @patch.object(PSSH, "getstatus")
  def test_runOsCheckScript_fail(self, getstatus_method, run_method):
    good_stats = {
      "hostname" : {
        "exitstatus" : 1,
        "log" : ""
      }
    }
    getstatus_method.return_value = good_stats
    os.environ[AMBARI_PASSPHRASE_VAR_NAME] = ""
    bootstrap = BootStrap(["hostname"], "user", "sshKeyFile", "scriptDir",
                          "bootdir", "setupAgentFile", "ambariServer",
                          "centos6", None, "8440")
    bootstrap.statuses = good_stats
    bootstrap.runOsCheckScript()

    self.assertTrue(run_method.called)
    self.assertTrue(getstatus_method.called)
    self.assertTrue("hostname" not in bootstrap.successive_hostlist)
    pass

  @patch.object(SCP, "writeLogToFile")
  @patch.object(SSH, "writeLogToFile")
  @patch.object(Popen, "communicate")
  @patch.object(BootStrap, "createDoneFiles")
  def test_run_setup_agent_command_ends_with_project_version(self, createDoneFiles_method,
                                                             communicate_method,
                                                             SSH_writeLogToFile_method,
                                                             SCP_writeLogToFile_method):
    SCP_writeLogToFile_method.return_value = None
    SSH_writeLogToFile_method.return_value = None
    communicate_method.return_value = ("", "")
    createDoneFiles_method.return_value = None

    os.environ[AMBARI_PASSPHRASE_VAR_NAME] = ""
    version = "1.1.1"
    bootstrap = BootStrap(["hostname"], "user", "sshKeyFile", "scriptDir", "bootdir", "setupAgentFile", "ambariServer", "centos6", version, "8440")
    runSetupCommand = bootstrap.getRunSetupCommand("hostname")
    self.assertTrue(runSetupCommand.endswith(version + " 8440"))


  @patch.object(SCP, "writeLogToFile")
  @patch.object(SSH, "writeLogToFile")
  @patch.object(Popen, "communicate")
  @patch.object(BootStrap, "createDoneFiles")
  def test_agent_setup_command_without_project_version(self, createDoneFiles_method,
                                                       communicate_method,
                                                       SSH_writeLogToFile_method,
                                                       SCP_writeLogToFile_method):
    SCP_writeLogToFile_method.return_value = None
    SSH_writeLogToFile_method.return_value = None
    communicate_method.return_value = ("", "")
    createDoneFiles_method.return_value = None

    os.environ[AMBARI_PASSPHRASE_VAR_NAME] = ""
    version = None
    bootstrap = BootStrap(["hostname"], "user", "sshKeyFile", "scriptDir", "bootdir", "setupAgentFile", "ambariServer", "centos6", version, "8440")
    runSetupCommand = bootstrap.getRunSetupCommand("hostname")
    self.assertTrue(runSetupCommand.endswith("8440"))



  @patch.object(BootStrap, "createDoneFiles")
  @patch.object(PSCP, "getstatus")
  @patch.object(PSCP, "run")
  @patch.object(PSSH, "getstatus")
  @patch.object(PSSH, "run")
  def test_os_check_fail_fails_bootstrap_execution(self,
      pssh_run_method, pssh_getstatus_method,
      pscp_run_method, pscp_getstatus_method, createDoneFiles_method):

    c6hstr = "cent6host"
    c5hstr = "cent5host"

    def pscp_statuses():
      yield { # copyOsCheckScript call
          c6hstr : {
            "exitstatus" : 0,
            "log" : ""
          },
          c5hstr : {
            "exitstatus" : 0,
            "log" : ""
          },
        }
      while True:   # Next calls
        d = {}
        for host in bootstrap.successive_hostlist:
          d[host] = {
            "exitstatus" : 0,
            "log" : ""
          }
        yield d

    def pssh_statuses():
      yield { # runOsCheckScript call
          c6hstr : {
            "exitstatus" : 0,
            "log" : ""
          },
          c5hstr : {
            "exitstatus" : 1,
            "log" : ""
          },
        }
      while True:   # Next calls
        d = {}
        for host in bootstrap.successive_hostlist:
          d[host] = {
            "exitstatus" : 0,
            "log" : ""
          }
        yield d

    pscp_getstatus_method.side_effect = pscp_statuses().next
    pssh_getstatus_method.side_effect = pssh_statuses().next


    os.environ[AMBARI_PASSPHRASE_VAR_NAME] = ""
    bootstrap = BootStrap([c6hstr, c5hstr], "user", "sshKeyFile", "scriptDir",
                          "bootdir", "setupAgentFile", "ambariServer",
                          "centos6", None, "8440")
    ret = bootstrap.run()

    self.assertTrue(c5hstr not in bootstrap.successive_hostlist)
    self.assertTrue(c6hstr in bootstrap.successive_hostlist)
    self.assertTrue(pssh_run_method.call_count >= 2)
    self.assertTrue(pssh_getstatus_method.call_count >= 2)
    self.assertTrue(ret == 1)


  def test_PSSH_constructor_argument_validation(self):
    dummy_command = "command"
    dummy_dict = {
      'hostname1' : 'c1',
      'hostname2' : 'c2',
      'hostname3' : 'c3'
    }

    # No any command arguments defined
    try:
      pssh = PSSH(["hostname"], "root", "sshKeyFile", "bootdir")
      self.fail("Should raise exception")
    except Exception, err:
      # Expected
      pass

    # Both command arguments defined
    try:
      pssh = PSSH(["hostname"], "root", "sshKeyFile", "bootdir", command = dummy_command, perHostCommands = dummy_dict)
      self.fail("Should raise exception")
    except Exception, err:
      # Expected
      pass

    # Invalid arguments: command dictionary has commands not for all hosts
    inv_dict = dict(dummy_dict)
    del inv_dict["hostname1"]
    try:
      pssh = PSSH(["hostname1", "hostname2", "hostname3"], "root", "sshKeyFile", "bootdir", perHostCommands=inv_dict)
      self.fail("Should raise exception")
    except Exception, err:
      # Expected
      pass

    # Invalid arguments:  command dictionary instead of command
    try:
      pssh = PSSH(["hostname"], "root", "sshKeyFile", "bootdir", command = dummy_dict)
      self.fail("Should raise exception")
    except Exception, err:
      # Expected
      pass

    # Invalid arguments: single command instead of command dictionary
    try:
      pssh = PSSH(["hostname"], "root", "sshKeyFile", "bootdir", perHostCommands = dummy_command)
      self.fail("Should raise exception")
    except Exception, err:
      # Expected
      pass

    # Valid arguments: command passed
    pssh = PSSH(["hostname"], "root", "sshKeyFile", "bootdir", command = dummy_command)

    # Valid arguments: command dictionary passed
    pssh = PSSH(["hostname1", "hostname2", "hostname3"], "root", "sshKeyFile", "bootdir", perHostCommands=dummy_dict)


