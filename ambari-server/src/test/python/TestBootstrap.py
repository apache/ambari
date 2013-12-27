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
import tempfile
import pprint

from bootstrap import PBootstrap, Bootstrap, SharedState, HostLog, SCP, SSH
from unittest import TestCase
from subprocess import Popen
from bootstrap import AMBARI_PASSPHRASE_VAR_NAME
from mock.mock import MagicMock, call
from mock.mock import patch
from mock.mock import create_autospec


class TestBootstrap(TestCase):

  def setUp(self):
    logging.basicConfig(level=logging.ERROR)


  def test_getRemoteName(self):
    shared_state = SharedState("root", "sshkey_file", "scriptDir", "bootdir",
                      "setupAgentFile", "ambariServer", "centos6", None, "8440")
    res = bootstrap_obj = Bootstrap("hostname", shared_state)
    utime1 = 1234
    utime2 = 12345
    bootstrap_obj.getUtime = MagicMock(return_value=utime1)
    remote1 = bootstrap_obj.getRemoteName("/tmp/setupAgent.sh")
    self.assertEquals(remote1, "/tmp/setupAgent{0}.sh".format(utime1))

    bootstrap_obj.getUtime.return_value=utime2
    remote1 = bootstrap_obj.getRemoteName("/tmp/setupAgent.sh")
    self.assertEquals(remote1, "/tmp/setupAgent{0}.sh".format(utime1))

    remote2 = bootstrap_obj.getRemoteName("/tmp/host_pass")
    self.assertEquals(remote2, "/tmp/host_pass{0}".format(utime2))


  # TODO: Test bootstrap timeout

  # TODO: test_return_error_message_for_missing_sudo_package

  def test_getAmbariPort(self):
    shared_state = SharedState("root", "sshkey_file", "scriptDir", "bootdir",
                               "setupAgentFile", "ambariServer", "centos6",
                               None, "8440")
    bootstrap_obj = Bootstrap("hostname", shared_state)
    self.assertEquals(bootstrap_obj.getAmbariPort(),"8440")
    shared_state.server_port = None
    bootstrap_obj = Bootstrap("hostname", shared_state)
    self.assertEquals(bootstrap_obj.getAmbariPort(),"null")


  @patch.object(subprocess, "Popen")
  @patch("sys.stderr")
  @patch("sys.exit")
  @patch.object(PBootstrap, "run")
  @patch("os.path.dirname")
  @patch("os.path.realpath")
  def test_bootstrap_main(self, dirname_mock, realpath_mock, run_mock, exit_mock, stderr_mock, subprocess_Popen_mock):
    bootstrap.main(["bootstrap.py", "hostname,hostname2", "/tmp/bootstrap", "root", "sshkey_file", "setupAgent.py", "ambariServer", \
                    "centos6", "1.1.1", "8440", "passwordfile"])
    self.assertTrue(run_mock.called)
    run_mock.reset_mock()
    bootstrap.main(["bootstrap.py", "hostname,hostname2", "/tmp/bootstrap", "root", "sshkey_file", "setupAgent.py", "ambariServer", \
                    "centos6", "1.1.1", "8440", None])
    self.assertTrue(run_mock.called)
    run_mock.reset_mock()
    def side_effect(retcode):
      raise Exception(retcode, "sys.exit")
    exit_mock.side_effect = side_effect
    try:
      bootstrap.main(["bootstrap.py","hostname,hostname2", "/tmp/bootstrap"])
      self.fail("sys.exit(2)")
    except Exception:
    # Expected
      pass
    self.assertTrue(exit_mock.called)


  @patch("os.environ")
  def test_getRunSetupWithPasswordCommand(self, environ_mock):
    shared_state = SharedState("root", "sshkey_file", "scriptDir", "bootdir",
                               "setupAgentFile", "ambariServer", "centos6",
                               None, "8440")
    environ_mock.__getitem__.return_value = "TEST_PASSPHRASE"
    bootstrap_obj = Bootstrap("hostname", shared_state)
    utime = 1234
    bootstrap_obj.getUtime = MagicMock(return_value=utime)
    ret = bootstrap_obj.getRunSetupWithPasswordCommand("hostname")
    expected = "sudo -S python /tmp/setupAgent{0}.py hostname TEST_PASSPHRASE " \
               "ambariServer  8440 < /tmp/host_pass{0}".format(utime)
    self.assertEquals(ret, expected)


  def test_generateRandomFileName(self):
    shared_state = SharedState("root", "sshkey_file", "scriptDir", "bootdir",
                               "setupAgentFile", "ambariServer", "centos6",
                               None, "8440")
    bootstrap_obj = Bootstrap("hostname", shared_state)
    self.assertTrue(bootstrap_obj.generateRandomFileName(None) == bootstrap_obj.getUtime())


  @patch("os.path.isfile")
  @patch("__builtin__.open")
  def test_is_suse(self, open_mock, isfile_mock):
    shared_state = SharedState("root", "sshkey_file", "scriptDir", "bootdir",
                               "setupAgentFile", "ambariServer", "centos6",
                               None, "8440")
    bootstrap_obj = Bootstrap("hostname", shared_state)
    isfile_mock.return_value = True
    f = open_mock.return_value
    f.read.return_value = " suse  "
    self.assertTrue(bootstrap_obj.is_suse())


  @patch.object(Bootstrap, "is_suse")
  def test_getRepoDir(self, is_suse_mock):
    shared_state = SharedState("root", "sshkey_file", "scriptDir", "bootdir",
                               "setupAgentFile", "ambariServer", "centos6",
                               None, "8440")
    bootstrap_obj = Bootstrap("hostname", shared_state)
    # Suse
    is_suse_mock.return_value = True
    res = bootstrap_obj.getRepoDir()
    self.assertEquals(res, "/etc/zypp/repos.d")
    # non-Suse
    is_suse_mock.return_value = False
    res = bootstrap_obj.getRepoDir()
    self.assertEquals(res, "/etc/yum.repos.d")

  def test_getSetupScript(self):
    shared_state = SharedState("root", "sshkey_file", "scriptDir", "bootdir",
                               "setupAgentFile", "ambariServer", "centos6",
                               None, "8440")
    bootstrap_obj = Bootstrap("hostname", shared_state)
    self.assertEquals(bootstrap_obj.shared_state.script_dir, "scriptDir")


  def test_run_setup_agent_command_ends_with_project_version(self):
    os.environ[AMBARI_PASSPHRASE_VAR_NAME] = ""
    version = "1.1.1"
    shared_state = SharedState("root", "sshkey_file", "scriptDir", "bootdir",
                               "setupAgentFile", "ambariServer", "centos6",
                               version, "8440")
    bootstrap_obj = Bootstrap("hostname", shared_state)
    runSetupCommand = bootstrap_obj.getRunSetupCommand("hostname")
    self.assertTrue(runSetupCommand.endswith(version + " 8440"))


  def test_agent_setup_command_without_project_version(self):
    os.environ[AMBARI_PASSPHRASE_VAR_NAME] = ""
    version = None
    shared_state = SharedState("root", "sshkey_file", "scriptDir", "bootdir",
                               "setupAgentFile", "ambariServer", "centos6",
                               version, "8440")
    bootstrap_obj = Bootstrap("hostname", shared_state)
    runSetupCommand = bootstrap_obj.getRunSetupCommand("hostname")
    self.assertTrue(runSetupCommand.endswith(" 8440"))


  # TODO: test_os_check_fail_fails_bootstrap_execution


  def test_host_log(self):
    tmp_file, tmp_filename  = tempfile.mkstemp()
    dummy_log = HostLog(tmp_filename)
    # First write to log
    dummy_log.write("a\nb\nc")
    # Read it
    with open(tmp_filename) as f:
      s = f.read()
      etalon = "a\nb\nc\n"
      self.assertEquals(s, etalon)
    # Next write
    dummy_log.write("Yet another string")
    # Read it
    with open(tmp_filename) as f:
      s = f.read()
      etalon = "a\nb\nc\nYet another string\n"
      self.assertEquals(s, etalon)
    # Should not append line end if it already exists
    dummy_log.write("line break->\n")
    # Read it
    with open(tmp_filename) as f:
      s = f.read()
      etalon = "a\nb\nc\nYet another string\nline break->\n"
      self.assertEquals(s, etalon)
    # Cleanup
    os.unlink(tmp_filename)


  @patch("subprocess.Popen")
  def test_SCP(self, popenMock):
    params = SharedState("root", "sshkey_file", "scriptDir", "bootdir",
                                  "setupAgentFile", "ambariServer", "centos6",
                                  "1.2.1", "8440")
    host_log_mock = MagicMock()
    log = {'text': ""}
    def write_side_effect(text):
      log['text'] = log['text'] + text

    host_log_mock.write.side_effect = write_side_effect
    scp = SCP(params.user, params.sshkey_file, "dummy-host", "src/file",
              "dst/file", params.bootdir, host_log_mock)
    log_sample = "log_sample"
    error_sample = "error_sample"
    # Successful run
    process = MagicMock()
    popenMock.return_value = process
    process.communicate.return_value = (log_sample, error_sample)
    process.returncode = 0

    retcode = scp.run()

    self.assertTrue(popenMock.called)
    self.assertTrue(log_sample in log['text'])
    self.assertTrue(error_sample in log['text'])
    command_str = str(popenMock.call_args[0][0])
    self.assertEquals(command_str, "['scp', '-o', 'ConnectTimeout=60', '-o', "
        "'BatchMode=yes', '-o', 'StrictHostKeyChecking=no', '-i', 'sshkey_file',"
        " 'src/file', 'root@dummy-host:dst/file']")
    self.assertEqual(retcode, 0)

    log['text'] = ""
    #unsuccessfull run
    process.returncode = 1

    retcode = scp.run()

    self.assertTrue(log_sample in log['text'])
    self.assertTrue(error_sample in log['text'])
    self.assertEqual(retcode, 1)


  @patch("subprocess.Popen")
  def test_SSH(self, popenMock):
    params = SharedState("root", "sshkey_file", "scriptDir", "bootdir",
                                  "setupAgentFile", "ambariServer", "centos6",
                                  "1.2.1", "8440")
    host_log_mock = MagicMock()
    log = {'text': ""}
    def write_side_effect(text):
      log['text'] = log['text'] + text

    host_log_mock.write.side_effect = write_side_effect
    ssh = SSH(params.user, params.sshkey_file, "dummy-host", "dummy-command",
              params.bootdir, host_log_mock)
    log_sample = "log_sample"
    error_sample = "error_sample"
    # Successful run
    process = MagicMock()
    popenMock.return_value = process
    process.communicate.return_value = (log_sample, error_sample)
    process.returncode = 0

    retcode = ssh.run()

    self.assertTrue(popenMock.called)
    self.assertTrue(log_sample in log['text'])
    self.assertTrue(error_sample in log['text'])
    command_str = str(popenMock.call_args[0][0])
    self.assertEquals(command_str, "['ssh', '-o', 'ConnectTimeOut=60', '-o', "
            "'StrictHostKeyChecking=no', '-o', 'BatchMode=yes', '-tt', '-i', "
            "'sshkey_file', 'root@dummy-host', 'dummy-command']")
    self.assertEqual(retcode, 0)

    log['text'] = ""
    #unsuccessfull run
    process.returncode = 1

    retcode = ssh.run()

    self.assertTrue(log_sample in log['text'])
    self.assertTrue(error_sample in log['text'])
    self.assertEqual(retcode, 1)

    log['text'] = ""
    # unsuccessful run with error message
    process.returncode = 1

    dummy_error_message = "dummy_error_message"
    ssh = SSH(params.user, params.sshkey_file, "dummy-host", "dummy-command",
              params.bootdir, host_log_mock, errorMessage= dummy_error_message)
    retcode = ssh.run()

    self.assertTrue(log_sample in log['text'])
    self.assertTrue(error_sample in log['text'])
    self.assertTrue(dummy_error_message in log['text'])
    self.assertEqual(retcode, 1)


  def test_getOsCheckScript(self):
    shared_state = SharedState("root", "sshkey_file", "scriptDir", "bootdir",
                               "setupAgentFile", "ambariServer", "centos6",
                               None, "8440")
    bootstrap_obj = Bootstrap("hostname", shared_state)
    ocs = bootstrap_obj.getOsCheckScript()
    self.assertEquals(ocs, "scriptDir/os_type_check.sh")


  @patch.object(Bootstrap, "getRemoteName")
  def test_getOsCheckScriptRemoteLocation(self, getRemoteName_mock):
    shared_state = SharedState("root", "sshkey_file", "scriptDir", "bootdir",
                               "setupAgentFile", "ambariServer", "centos6",
                               None, "8440")
    bootstrap_obj = Bootstrap("hostname", shared_state)
    v = "/tmp/os_type_check1374259902.sh"
    getRemoteName_mock.return_value = v
    ocs = bootstrap_obj.getOsCheckScriptRemoteLocation()
    self.assertEquals(ocs, v)


  @patch.object(Bootstrap, "is_suse")
  def test_getRepoFile(self, is_suse_mock):
    shared_state = SharedState("root", "sshkey_file", "scriptDir", "bootdir",
                               "setupAgentFile", "ambariServer", "centos6",
                               None, "8440")
    bootstrap_obj = Bootstrap("hostname", shared_state)
    is_suse_mock.return_value = False
    rf = bootstrap_obj.getRepoFile()
    self.assertEquals(rf, "/etc/yum.repos.d/ambari.repo")


  @patch.object(Bootstrap, "getOsCheckScript")
  @patch.object(Bootstrap, "getOsCheckScriptRemoteLocation")
  @patch.object(SCP, "__init__")
  @patch.object(SCP, "run")
  @patch.object(HostLog, "write")
  def test_copyOsCheckScript(self, write_mock, run_mock, init_mock,
                    getOsCheckScriptRemoteLocation_mock, getOsCheckScript_mock):
    shared_state = SharedState("root", "sshkey_file", "scriptDir", "bootdir",
                               "setupAgentFile", "ambariServer", "centos6",
                               None, "8440")
    bootstrap_obj = Bootstrap("hostname", shared_state)
    getOsCheckScript_mock.return_value = "OsCheckScript"
    getOsCheckScriptRemoteLocation_mock.return_value = "OsCheckScriptRemoteLocation"
    expected = 42
    init_mock.return_value = None
    run_mock.return_value = expected
    res = bootstrap_obj.copyOsCheckScript()
    self.assertEquals(res, expected)
    input_file = str(init_mock.call_args[0][3])
    remote_file = str(init_mock.call_args[0][4])
    self.assertEqual(input_file, "OsCheckScript")
    self.assertEqual(remote_file, "OsCheckScriptRemoteLocation")


  @patch.object(Bootstrap, "getRemoteName")
  @patch.object(Bootstrap, "hasPassword")
  def test_getRepoFile(self, hasPassword_mock, getRemoteName_mock):
    shared_state = SharedState("root", "sshkey_file", "scriptDir", "bootdir",
                               "setupAgentFile", "ambariServer", "centos6",
                               None, "8440")
    bootstrap_obj = Bootstrap("hostname", shared_state)
    # Without password
    hasPassword_mock.return_value = False
    getRemoteName_mock.return_value = "RemoteName"
    rf = bootstrap_obj.getMoveRepoFileCommand("target")
    self.assertEquals(rf, "sudo mv RemoteName target/ambari.repo")
    # With password
    hasPassword_mock.return_value = True
    getRemoteName_mock.return_value = "RemoteName"
    rf = bootstrap_obj.getMoveRepoFileCommand("target")
    self.assertEquals(rf, "sudo -S mv RemoteName target/ambari.repo < RemoteName")


  @patch.object(Bootstrap, "getMoveRepoFileCommand")
  @patch.object(Bootstrap, "getRepoDir")
  @patch.object(Bootstrap, "getRepoFile")
  @patch.object(Bootstrap, "getRemoteName")
  @patch.object(SCP, "__init__")
  @patch.object(SCP, "run")
  @patch.object(SSH, "__init__")
  @patch.object(SSH, "run")
  @patch.object(HostLog, "write")
  def test_copyNeededFiles(self, write_mock, ssh_run_mock, ssh_init_mock,
                           scp_run_mock, scp_init_mock,
                           getRemoteName_mock, getRepoFile_mock, getRepoDir,
                           getMoveRepoFileCommand):
    shared_state = SharedState("root", "sshkey_file", "scriptDir", "bootdir",
                               "setupAgentFile", "ambariServer", "centos6",
                               None, "8440")
    bootstrap_obj = Bootstrap("hostname", shared_state)
    getMoveRepoFileCommand.return_value = "MoveRepoFileCommand"
    getRepoDir.return_value  = "RepoDir"
    getRemoteName_mock.return_value = "RemoteName"
    getRepoFile_mock.return_value = "RepoFile"
    expected1 = 42
    expected2 = 17
    expected3 = 1
    scp_init_mock.return_value = None
    ssh_init_mock.return_value = None
    # Testing max retcode return
    scp_run_mock.side_effect = [expected1, expected3]
    ssh_run_mock.side_effect = [expected2]
    res = bootstrap_obj.copyNeededFiles()
    self.assertEquals(res, expected1)
    input_file = str(scp_init_mock.call_args[0][3])
    remote_file = str(scp_init_mock.call_args[0][4])
    self.assertEqual(input_file, "setupAgentFile")
    self.assertEqual(remote_file, "RemoteName")
    command = str(ssh_init_mock.call_args[0][3])
    self.assertEqual(command, "MoveRepoFileCommand")
    # Another order
    expected1 = 0
    expected2 = 17
    expected3 = 1
    scp_run_mock.side_effect = [expected1, expected3]
    ssh_run_mock.side_effect = [expected2]
    res = bootstrap_obj.copyNeededFiles()
    self.assertEquals(res, expected2)
    # yet another order
    expected1 = 33
    expected2 = 17
    expected3 = 42
    scp_run_mock.side_effect = [expected1, expected3]
    ssh_run_mock.side_effect = [expected2]
    res = bootstrap_obj.copyNeededFiles()
    self.assertEquals(res, expected3)


  @patch.object(Bootstrap, "getOsCheckScriptRemoteLocation")
  @patch.object(SSH, "__init__")
  @patch.object(SSH, "run")
  @patch.object(HostLog, "write")
  def test_runOsCheckScript(self, write_mock, run_mock,
                            init_mock, getOsCheckScriptRemoteLocation_mock):
    shared_state = SharedState("root", "sshkey_file", "scriptDir", "bootdir",
                               "setupAgentFile", "ambariServer", "centos6",
                               None, "8440")
    bootstrap_obj = Bootstrap("hostname", shared_state)
    getOsCheckScriptRemoteLocation_mock.return_value = "OsCheckScriptRemoteLocation"
    expected = 42
    init_mock.return_value = None
    run_mock.return_value = expected
    res = bootstrap_obj.runOsCheckScript()
    self.assertEquals(res, expected)
    command = str(init_mock.call_args[0][3])
    self.assertEqual(command, "chmod a+x OsCheckScriptRemoteLocation &&"
                              " OsCheckScriptRemoteLocation centos6")


  @patch.object(SSH, "__init__")
  @patch.object(Bootstrap, "getRunSetupCommand")
  @patch.object(SSH, "run")
  @patch.object(HostLog, "write")
  def test_runSetupAgent(self, write_mock, run_mock,
                         getRunSetupCommand_mock, init_mock):
    shared_state = SharedState("root", "sshkey_file", "scriptDir", "bootdir",
                               "setupAgentFile", "ambariServer", "centos6",
                               None, "8440")
    bootstrap_obj = Bootstrap("hostname", shared_state)
    getRunSetupCommand_mock.return_value = "RunSetupCommand"
    expected = 42
    init_mock.return_value = None
    run_mock.return_value = expected
    res = bootstrap_obj.runSetupAgent()
    self.assertEquals(res, expected)
    command = str(init_mock.call_args[0][3])
    self.assertEqual(command, "RunSetupCommand")


  @patch.object(Bootstrap, "hasPassword")
  @patch.object(Bootstrap, "getRunSetupWithPasswordCommand")
  @patch.object(Bootstrap, "getRunSetupWithoutPasswordCommand")
  def test_getRunSetupCommand(self, getRunSetupWithoutPasswordCommand_mock,
                              getRunSetupWithPasswordCommand_mock,
                              hasPassword_mock):
    shared_state = SharedState("root", "sshkey_file", "scriptDir", "bootdir",
                               "setupAgentFile", "ambariServer", "centos6",
                               None, "8440")
    bootstrap_obj = Bootstrap("hostname", shared_state)
    # With password
    hasPassword_mock.return_value = True
    getRunSetupWithPasswordCommand_mock.return_value = "RunSetupWithPasswordCommand"
    getRunSetupWithoutPasswordCommand_mock.return_value = "RunSetupWithoutPasswordCommand"
    res = bootstrap_obj.getRunSetupCommand("dummy-host")
    self.assertEqual(res, "RunSetupWithPasswordCommand")
    # Without password
    hasPassword_mock.return_value = False
    res = bootstrap_obj.getRunSetupCommand("dummy-host")
    self.assertEqual(res, "RunSetupWithoutPasswordCommand")


  @patch.object(HostLog, "write")
  def test_createDoneFile(self, write_mock):
    tmp_dir = tempfile.gettempdir()
    shared_state = SharedState("root", "sshkey_file", "scriptDir", tmp_dir,
                               "setupAgentFile", "ambariServer", "centos6",
                               None, "8440")
    bootstrap_obj = Bootstrap("hostname", shared_state)
    done_file = os.path.join(tmp_dir, "hostname.done")
    expected = 42
    bootstrap_obj.createDoneFile(expected)
    with open(done_file) as df:
      res = df.read()
      self.assertEqual(res, str(expected))
    os.unlink(done_file)

  @patch.object(SSH, "__init__")
  @patch.object(SSH, "run")
  @patch.object(HostLog, "write")
  def test_checkSudoPackage(self, write_mock, run_mock, init_mock):
    shared_state = SharedState("root", "sshkey_file", "scriptDir", "bootdir",
                               "setupAgentFile", "ambariServer", "centos6",
                               None, "8440")
    bootstrap_obj = Bootstrap("hostname", shared_state)
    expected = 42
    init_mock.return_value = None
    run_mock.return_value = expected
    res = bootstrap_obj.checkSudoPackage()
    self.assertEquals(res, expected)
    command = str(init_mock.call_args[0][3])
    self.assertEqual(command, "rpm -qa | grep sudo")


  @patch.object(SSH, "__init__")
  @patch.object(SSH, "run")
  @patch.object(HostLog, "write")
  @patch.object(Bootstrap, "getPasswordFile")
  def test_deletePasswordFile(self, getPasswordFile_mock, write_mock, run_mock,
                              init_mock):
    shared_state = SharedState("root", "sshkey_file", "scriptDir", "bootdir",
                               "setupAgentFile", "ambariServer", "centos6",
                               None, "8440")
    bootstrap_obj = Bootstrap("hostname", shared_state)
    expected = 42
    getPasswordFile_mock.return_value = "PasswordFile"
    init_mock.return_value = None
    run_mock.return_value = expected
    res = bootstrap_obj.deletePasswordFile()
    self.assertEquals(res, expected)
    command = str(init_mock.call_args[0][3])
    self.assertEqual(command, "rm PasswordFile")


  @patch.object(Bootstrap, "getPasswordFile")
  @patch.object(SCP, "__init__")
  @patch.object(SCP, "run")
  @patch.object(SSH, "__init__")
  @patch.object(SSH, "run")
  @patch.object(HostLog, "write")
  def test_copyPasswordFile(self, write_mock, ssh_run_mock,
                            ssh_init_mock, scp_run_mock,
                            scp_init_mock, getPasswordFile_mock):
    shared_state = SharedState("root", "sshkey_file", "scriptDir", "bootdir",
                               "setupAgentFile", "ambariServer", "centos6",
                               None, "8440", password_file="PasswordFile")
    bootstrap_obj = Bootstrap("hostname", shared_state)
    getPasswordFile_mock.return_value = "PasswordFile"
     # Testing max retcode return
    expected1 = 42
    expected2 = 17
    scp_init_mock.return_value = None
    scp_run_mock.return_value = expected1
    ssh_init_mock.return_value = None
    ssh_run_mock.return_value = expected2
    res = bootstrap_obj.copyPasswordFile()
    self.assertEquals(res, expected1)
    input_file = str(scp_init_mock.call_args[0][3])
    remote_file = str(scp_init_mock.call_args[0][4])
    self.assertEqual(input_file, "PasswordFile")
    self.assertEqual(remote_file, "PasswordFile")
    command = str(ssh_init_mock.call_args[0][3])
    self.assertEqual(command, "chmod 600 PasswordFile")
    # Another order
    expected1 = 0
    expected2 = 17
    scp_run_mock.return_value = expected1
    ssh_run_mock.return_value = expected2


  @patch.object(SSH, "__init__")
  @patch.object(SSH, "run")
  @patch.object(HostLog, "write")
  @patch.object(Bootstrap, "getPasswordFile")
  def test_changePasswordFileModeOnHost(self, getPasswordFile_mock, write_mock,
                                        run_mock, init_mock):
    shared_state = SharedState("root", "sshkey_file", "scriptDir", "bootdir",
                               "setupAgentFile", "ambariServer", "centos6",
                               None, "8440")
    bootstrap_obj = Bootstrap("hostname", shared_state)
    expected = 42
    getPasswordFile_mock.return_value = "PasswordFile"
    init_mock.return_value = None
    run_mock.return_value = expected
    res = bootstrap_obj.changePasswordFileModeOnHost()
    self.assertEquals(res, expected)
    command = str(init_mock.call_args[0][3])
    self.assertEqual(command, "chmod 600 PasswordFile")


  @patch.object(HostLog, "write")
  def test_try_to_execute(self, write_mock):
    expected = 43
    shared_state = SharedState("root", "sshkey_file", "scriptDir", "bootdir",
                               "setupAgentFile", "ambariServer", "centos6",
                               None, "8440")
    bootstrap_obj = Bootstrap("hostname", shared_state)
    # Normal case
    ret = bootstrap_obj.try_to_execute(lambda : expected)
    self.assertEqual(ret, expected)
    self.assertFalse(write_mock.called)

    write_mock.reset_mock()
    # Exception scenario
    def act():
      raise IOError()
    ret = bootstrap_obj.try_to_execute(act)
    self.assertEqual(ret, 177)
    self.assertTrue(write_mock.called)


  @patch.object(Bootstrap, "try_to_execute")
  @patch.object(Bootstrap, "hasPassword")
  @patch.object(Bootstrap, "createDoneFile")
  @patch.object(HostLog, "write")
  @patch("logging.warn")
  @patch("logging.error")
  def test_run(self, error_mock, warn_mock, write_mock, createDoneFile_mock,
               hasPassword_mock, try_to_execute_mock):
    shared_state = SharedState("root", "sshkey_file", "scriptDir", "bootdir",
                               "setupAgentFile", "ambariServer", "centos6",
                               None, "8440")
    bootstrap_obj = Bootstrap("hostname", shared_state)
    # Testing workflow without password
    bootstrap_obj.copied_password_file = False
    hasPassword_mock.return_value = False
    try_to_execute_mock.return_value = 0
    bootstrap_obj.run()
    self.assertEqual(try_to_execute_mock.call_count, 5) # <- Adjust if changed
    self.assertTrue(createDoneFile_mock.called)
    self.assertEqual(bootstrap_obj.getStatus()["return_code"], 0)

    try_to_execute_mock.reset_mock()
    createDoneFile_mock.reset_mock()
    # Testing workflow with password
    bootstrap_obj.copied_password_file = True
    hasPassword_mock.return_value = True
    try_to_execute_mock.return_value = 0
    bootstrap_obj.run()
    self.assertEqual(try_to_execute_mock.call_count, 8) # <- Adjust if changed
    self.assertTrue(createDoneFile_mock.called)
    self.assertEqual(bootstrap_obj.getStatus()["return_code"], 0)

    error_mock.reset_mock()
    write_mock.reset_mock()
    try_to_execute_mock.reset_mock()
    createDoneFile_mock.reset_mock()
    # Testing workflow when some action failed before copying password
    bootstrap_obj.copied_password_file = False
    hasPassword_mock.return_value = False
    try_to_execute_mock.side_effect = [0, 1]
    bootstrap_obj.run()
    self.assertEqual(try_to_execute_mock.call_count, 2) # <- Adjust if changed
    self.assertTrue("ERROR" in error_mock.call_args[0][0])
    self.assertTrue("ERROR" in write_mock.call_args[0][0])
    self.assertTrue(createDoneFile_mock.called)
    self.assertEqual(bootstrap_obj.getStatus()["return_code"], 1)

    try_to_execute_mock.reset_mock()
    createDoneFile_mock.reset_mock()
    # Testing workflow when some action failed after copying password
    bootstrap_obj.copied_password_file = True
    hasPassword_mock.return_value = True
    try_to_execute_mock.side_effect = [0, 42, 0]
    bootstrap_obj.run()
    self.assertEqual(try_to_execute_mock.call_count, 3) # <- Adjust if changed
    self.assertTrue(createDoneFile_mock.called)
    self.assertEqual(bootstrap_obj.getStatus()["return_code"], 42)

    error_mock.reset_mock()
    write_mock.reset_mock()
    try_to_execute_mock.reset_mock()
    createDoneFile_mock.reset_mock()
    # Testing workflow when some action failed after copying password and
    # removing password failed too
    bootstrap_obj.copied_password_file = True
    hasPassword_mock.return_value = True
    try_to_execute_mock.side_effect = [0, 17, 19]
    bootstrap_obj.run()
    self.assertEqual(try_to_execute_mock.call_count, 3) # <- Adjust if changed
    self.assertTrue("ERROR" in write_mock.call_args_list[0][0][0])
    self.assertTrue("ERROR" in error_mock.call_args[0][0])
    self.assertTrue("WARNING" in write_mock.call_args_list[1][0][0])
    self.assertTrue("WARNING" in warn_mock.call_args[0][0])
    self.assertTrue(createDoneFile_mock.called)
    self.assertEqual(bootstrap_obj.getStatus()["return_code"], 17)


  @patch.object(Bootstrap, "createDoneFile")
  @patch.object(HostLog, "write")
  def test_interruptBootstrap(self, write_mock, createDoneFile_mock):
    shared_state = SharedState("root", "sshkey_file", "scriptDir", "bootdir",
                               "setupAgentFile", "ambariServer", "centos6",
                               None, "8440")
    bootstrap_obj = Bootstrap("hostname", shared_state)
    bootstrap_obj.interruptBootstrap()
    self.assertTrue(createDoneFile_mock.called)


  @patch("time.sleep")
  @patch("time.time")
  @patch("logging.warn")
  @patch("logging.info")
  @patch.object(Bootstrap, "start")
  @patch.object(Bootstrap, "interruptBootstrap")
  @patch.object(Bootstrap, "getStatus")
  def test_PBootstrap(self, getStatus_mock, interruptBootstrap_mock, start_mock,
                      info_mock, warn_mock, time_mock, sleep_mock):
    shared_state = SharedState("root", "sshkey_file", "scriptDir", "bootdir",
                               "setupAgentFile", "ambariServer", "centos6",
                               None, "8440")
    n = 180
    time = 100500
    time_mock.return_value = time
    hosts = []
    for i in range(0, n):
      hosts.append("host" + str(i))
    # Testing normal case
    getStatus_mock.return_value = {"return_code": 0,
                                   "start_time": time + 999}
    pbootstrap_obj = PBootstrap(hosts, shared_state)
    pbootstrap_obj.run()
    self.assertEqual(start_mock.call_count, n)
    self.assertEqual(interruptBootstrap_mock.call_count, 0)

    start_mock.reset_mock()
    getStatus_mock.reset_mock()
    # Testing case of timeout
    def fake_return_code_generator():
      call_number = 0
      while True:
        call_number += 1
        if call_number % 5 != 0:   # ~80% of hosts finish successfully
          yield 0
        else:
          yield None

    def fake_start_time_generator():
      while True:
        yield time - bootstrap.HOST_BOOTSTRAP_TIMEOUT - 1

    return_code_generator = fake_return_code_generator()
    start_time_generator = fake_start_time_generator()

    def status_get_item_mock(item):
      if item == "return_code":
        return return_code_generator.next()
      elif item == "start_time":
        return start_time_generator.next()

    dict_mock = MagicMock()
    dict_mock.__getitem__.side_effect = status_get_item_mock
    getStatus_mock.return_value = dict_mock

    pbootstrap_obj.run()
    self.assertEqual(start_mock.call_count, n)
    self.assertEqual(interruptBootstrap_mock.call_count, n / 5)

