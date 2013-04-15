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
    pssh = PSSH(["hostname"], "root", "sshKeyFile", "command", "bootdir")
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

  def test_return_error_message_for_missing_sudo_package(self):
    Popen.communicate = lambda self: ("", "")
    SCP.writeLogToFile = lambda self, logFilePath: None
    SSH.writeLogToFile = lambda self, logFilePath: None
    SSH.writeDoneToFile = lambda self, doneFilePath, returncode: None
    bootstrap = BootStrap(["hostname"], "root", "sshKeyFile", "scriptDir", "bootdir", "setupAgentFile", "ambariServer", "centos6")
    ret = bootstrap.checkSudoPackage()
    self.assertTrue("Error: Sudo command is not available. Please install the sudo command." in bootstrap.statuses["hostname"]["log"])

  def test_copy_and_delete_password_file_methods_are_called_for_user_with_password(self):
    Popen.communicate = lambda self: ("", "")
    SCP.writeLogToFile = lambda self, logFilePath: None
    SSH.writeLogToFile = lambda self, logFilePath: None
    SSH.writeDoneToFile = lambda self, doneFilePath, returncode: None
    BootStrap.createDoneFiles = lambda self: None

    def side_effect(self):
      self.copyPasswordFile_called = True
      self.hostlist_to_remove_password_file = ["hostname"]
      return 0
    BootStrap.copyPasswordFile = side_effect

    deletePasswordFile = MagicMock()
    deletePasswordFile.return_value = 0
    BootStrap.deletePasswordFile = deletePasswordFile

    changePasswordFileModeOnHost = MagicMock()
    changePasswordFileModeOnHost.return_value = 0
    BootStrap.changePasswordFileModeOnHost = changePasswordFileModeOnHost

    os.environ[AMBARI_PASSPHRASE_VAR_NAME] = ""
    bootstrap = BootStrap(["hostname"], "user", "sshKeyFile", "scriptDir", "bootdir", "setupAgentFile", "ambariServer", "centos6", "passwordFile")
    ret = bootstrap.run()
    self.assertTrue(bootstrap.copyPasswordFile_called)
    self.assertTrue(deletePasswordFile.called)
    self.assertTrue(changePasswordFileModeOnHost.called)

  def test_copy_and_delete_password_file_methods_are_not_called_for_passwordless_user(self):
    Popen.communicate = lambda self: ("", "")
    SCP.writeLogToFile = lambda self, logFilePath: None
    SSH.writeLogToFile = lambda self, logFilePath: None
    SSH.writeDoneToFile = lambda self, doneFilePath, returncode: None
    BootStrap.createDoneFiles = lambda self: None

    def side_effect(self):
      self.copyPasswordFile_called = True
      self.hostlist_to_remove_password_file = ["hostname"]
      return 0
    BootStrap.copyPasswordFile = side_effect

    deletePasswordFile = MagicMock()
    deletePasswordFile.return_value = 0
    BootStrap.deletePasswordFile = deletePasswordFile

    changePasswordFileModeOnHost = MagicMock()
    changePasswordFileModeOnHost.return_value = 0
    BootStrap.changePasswordFileModeOnHost = changePasswordFileModeOnHost

    os.environ[AMBARI_PASSPHRASE_VAR_NAME] = ""
    bootstrap = BootStrap(["hostname"], "user", "sshKeyFile", "scriptDir", "bootdir", "setupAgentFile", "ambariServer", "centos6")
    bootstrap.copyPasswordFile_called = False
    ret = bootstrap.run()
    self.assertFalse(bootstrap.copyPasswordFile_called)
    self.assertFalse(deletePasswordFile.called)
    self.assertFalse(changePasswordFileModeOnHost.called)

  def test_commands_with_password_are_called_for_user_with_password(self):
    def communicate(self, input=None, timeout=None):
      self.returncode = 0
      return ("", "")
    Popen.communicate = communicate
    SCP.writeLogToFile = lambda self, logFilePath: None
    SSH.writeLogToFile = lambda self, logFilePath: None
    SSH.writeDoneToFile = lambda self, doneFilePath, returncode: None
    BootStrap.createDoneFiles = lambda self: None

    getRunSetupWithPasswordCommand = MagicMock()
    getRunSetupWithPasswordCommand.return_value = ""
    BootStrap.getRunSetupWithPasswordCommand = getRunSetupWithPasswordCommand

    getMoveRepoFileWithPasswordCommand = MagicMock()
    getMoveRepoFileWithPasswordCommand.return_value = ""
    BootStrap.getMoveRepoFileWithPasswordCommand = getMoveRepoFileWithPasswordCommand

    os.environ[AMBARI_PASSPHRASE_VAR_NAME] = ""
    bootstrap = BootStrap(["hostname"], "user", "sshKeyFile", "scriptDir", "bootdir", "setupAgentFile", "ambariServer", "centos6", "passwordFile")
    ret = bootstrap.run()
    self.assertTrue(getRunSetupWithPasswordCommand.called)
    self.assertTrue(getMoveRepoFileWithPasswordCommand.called)

  def test_commands_without_password_are_called_for_passwordless_user(self):
    Popen.communicate = lambda self: ("", "")
    SCP.writeLogToFile = lambda self, logFilePath: None
    SSH.writeLogToFile = lambda self, logFilePath: None
    SSH.writeDoneToFile = lambda self, doneFilePath, returncode: None
    BootStrap.createDoneFiles = lambda self: None

    getRunSetupWithoutPasswordCommand = MagicMock()
    getRunSetupWithoutPasswordCommand.return_value = ""
    BootStrap.getRunSetupWithoutPasswordCommand = getRunSetupWithoutPasswordCommand

    getMoveRepoFileWithoutPasswordCommand = MagicMock()
    getMoveRepoFileWithoutPasswordCommand.return_value = ""
    BootStrap.getMoveRepoFileWithoutPasswordCommand = getMoveRepoFileWithoutPasswordCommand

    os.environ[AMBARI_PASSPHRASE_VAR_NAME] = ""
    bootstrap = BootStrap(["hostname"], "user", "sshKeyFile", "scriptDir", "bootdir", "setupAgentFile", "ambariServer", "centos6")
    ret = bootstrap.run()
    self.assertTrue(getRunSetupWithoutPasswordCommand.called)
    self.assertTrue(getMoveRepoFileWithoutPasswordCommand.called)


  @patch.object(BootStrap, "runSetupAgent")
  @patch.object(BootStrap, "copyNeededFiles")
  @patch.object(BootStrap, "checkSudoPackage")
  @patch.object(BootStrap, "runOsCheckScript")
  @patch.object(BootStrap, "copyOsCheckScript")
  def test_os_check_performed(self, copyOsCheckScript_method,
                              runOsCheckScript_method, checkSudoPackage_method,
                              copyNeededFiles_method, runSetupAgent_method):
    BootStrap.createDoneFiles = lambda self: None

    getRunSetupWithoutPasswordCommand = MagicMock()
    getRunSetupWithoutPasswordCommand.return_value = ""
    BootStrap.getRunSetupWithoutPasswordCommand = getRunSetupWithoutPasswordCommand

    getMoveRepoFileWithoutPasswordCommand = MagicMock()
    getMoveRepoFileWithoutPasswordCommand.return_value = ""
    BootStrap.getMoveRepoFileWithoutPasswordCommand = getMoveRepoFileWithoutPasswordCommand

    copyOsCheckScript_method.return_value = 0
    runOsCheckScript_method.return_value = 0
    checkSudoPackage_method.return_value = 0
    copyNeededFiles_method.return_value = 0
    runSetupAgent_method.return_value = 0

    BootStrap.copyOsCheckScript = copyOsCheckScript_method
    BootStrap.runOsCheckScript = runOsCheckScript_method
    BootStrap.checkSudoPackage = checkSudoPackage_method
    BootStrap.copyNeededFiles = copyNeededFiles_method
    BootStrap.runSetupAgent = runSetupAgent_method

    os.environ[AMBARI_PASSPHRASE_VAR_NAME] = ""
    bootstrap = BootStrap(["hostname"], "user", "sshKeyFile", "scriptDir",
                          "bootdir", "setupAgentFile", "ambariServer",
                          "centos6")
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
                          "centos6")
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
                          "centos6")
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
                          "centos6")
    bootstrap.statuses = good_stats
    bootstrap.runOsCheckScript()

    self.assertTrue(run_method.called)
    self.assertTrue(getstatus_method.called)
    self.assertTrue("hostname" not in bootstrap.successive_hostlist)
    pass