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

from bootstrap import SCP
from bootstrap import PSCP
from bootstrap import SSH
from bootstrap import PSSH
from unittest import TestCase

class TestBootstrap(TestCase):

  #Timout is specified in bootstrap.HOST_BOOTSTRAP_TIMEOUT, default is 300 seconds
  def test_return_failed_status_for_hanging_ssh_threads_after_timeout(self):
    bootstrap.HOST_BOOTSTRAP_TIMEOUT = 1
    forever_hanging_timeout = 5
    SSH.run = lambda self: time.sleep(forever_hanging_timeout)
    pssh = PSSH(["hostname"], "sshKeyFile", "command", "bootdir")
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
    pscp = PSCP(["hostname"], "sshKeyFile", "inputfile", "remote", "bootdir")
    self.assertTrue(pscp.ret == {})
    starttime = time.time()
    pscp.run()
    self.assertTrue(pscp.ret != {})
    self.assertTrue(time.time() - starttime < forever_hanging_timeout)
    self.assertTrue(pscp.ret["hostname"]["log"] == "FAILED")
    self.assertTrue(pscp.ret["hostname"]["exitstatus"] == -1)
