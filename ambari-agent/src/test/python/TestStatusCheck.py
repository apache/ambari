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
import tempfile
import shutil
import os
from unittest import TestCase
from ambari_agent.StatusCheck import StatusCheck
import subprocess
import signal
from shell import shellRunner


MAPPING_FILE_NAME='map.dict'

COMPONENT_LIVE = 'LIVE_COMPONENT'
COMPONENT_LIVE_PID = 'live_comp.pid'
COMPONENT_LIVE_CMD='''
while [ 1==1 ]
do
   echo ok
done
'''

COMPONENT_DEAD = 'DEAD_COMPONENT'
COMPONENT_DEAD_PID = 'dead_comp.pid'
DEAD_PID=0


class TestStatusCheck(TestCase):

  def setUp(self):
    self.tmpdir = tempfile.mkdtemp()
    self.tmpdict = tempfile.NamedTemporaryFile(dir=self.tmpdir)
    self.tmpdict = open(self.tmpdir + os.sep + MAPPING_FILE_NAME, 'w')

    self.sh = shellRunner()
    
    #Launch eternal process
    p = subprocess.Popen([COMPONENT_LIVE_CMD], stdout=subprocess.PIPE, 
                         stderr=subprocess.PIPE, shell=True, close_fds=True)

    #Write mapping for pid files for both live and dead process
    self.tmpdict.write(COMPONENT_LIVE + '=' + COMPONENT_LIVE_PID + os.linesep)
    self.tmpdict.write(COMPONENT_DEAD + '=' + COMPONENT_DEAD_PID + os.linesep)
    self.tmpdict.close()

    #Write pid of live process to file
    live_pid_file = open(self.tmpdir + os.sep + COMPONENT_LIVE_PID, 'w')
    self.live_pid = p.pid
    live_pid_file.write(str(self.live_pid))
    live_pid_file.close()

    #Write pid of dead process to file
    dead_pid_file = open(self.tmpdir + os.sep + COMPONENT_DEAD_PID, 'w')
    dead_pid_file.write(str(DEAD_PID))
    dead_pid_file.close()

    #Init status checker
    self.statusCheck = StatusCheck(self.tmpdir, self.tmpdict.name)

  # Ensure that status checker throws exceptions on invalid params
  def test_exceptions(self):
    self.assertRaises(ValueError,StatusCheck,"tmp","tmp")
    self.assertRaises(IOError, StatusCheck,self.tmpdir,"tmp")

  # Ensure that status checker return True for running process
  def test_live(self):
    status = self.statusCheck.getStatus(COMPONENT_LIVE)
    self.assertEqual(status, True)

  # Ensure that status checker return False for dead process
  def test_dead(self):
    status = self.statusCheck.getStatus(COMPONENT_DEAD)
    self.assertEqual(status, False)

  def tearDown(self):
    os.kill(self.live_pid, signal.SIGKILL)
    shutil.rmtree(self.tmpdir)
