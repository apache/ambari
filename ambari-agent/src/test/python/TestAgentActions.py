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
import os, errno, getpass
from ambari_agent.ActionQueue import ActionQueue
from ambari_agent.AmbariConfig import AmbariConfig
from ambari_agent.FileUtil import getFilePath
from ambari_agent import shell
from ambari_agent.shell import serverTracker
import time

class TestAgentActions(TestCase):
  def test_installAndConfigAction(self):
    action={'id' : 'tttt'}
    actionQueue = ActionQueue(AmbariConfig().getConfig())
    path = actionQueue.getInstallFilename(action['id'])
    configFile = {
      "data"       : "test",
      "owner"      : os.getuid(),
      "group"      : os.getgid() ,
      "permission" : 0700,
      "path"       : path,
      "umask"      : 022
    }

    #note that the command in the action is just a listing of the path created
    #we just want to ensure that 'ls' can run on the data file (in the actual world
    #this 'ls' would be a puppet or a chef command that would work on a data
    #file
    path=getFilePath(action,path)
    action = { 
      'id' : 'tttt',
      'kind' : 'INSTALL_AND_CONFIG_ACTION',
      'workDirComponent' : 'abc-hdfs',
      'file' : configFile,
      'clusterDefinitionRevision' : 12,
      'command' : ['/bin/ls',path]
    }
    result = { }
    actionQueue = ActionQueue(AmbariConfig().getConfig())
    result = actionQueue.installAndConfigAction(action)
    cmdResult = result['commandResult']
    self.assertEqual(cmdResult['exitCode'], 0, "installAndConfigAction test failed. Returned %d " % cmdResult['exitCode'])
    self.assertEqual(cmdResult['output'], path + "\n", "installAndConfigAction test failed Returned %s " % cmdResult['output'])

  def test_startAndStopAction(self):
    command = {'script' : 'import os,sys,time\ni = 0\nwhile (i < 1000):\n  print "testhello"\n  sys.stdout.flush()\n  time.sleep(1)\n  i+=1',
               'param' : ''}
    action={'id' : 'ttt',
            'kind' : 'START_ACTION',
            'clusterId' : 'foobar',
            'clusterDefinitionRevision' : 1,
            'component' : 'foocomponent',
            'role' : 'foorole',
            'command' : command,
            'user' : getpass.getuser()
    }
    
    actionQueue = ActionQueue(AmbariConfig().getConfig())
    result = actionQueue.startAction(action)
    cmdResult = result['commandResult']
    self.assertEqual(cmdResult['exitCode'], 0, "starting a process failed")
    shell = actionQueue.getshellinstance()
    key = shell.getServerKey(action['clusterId'],action['clusterDefinitionRevision'],
                       action['component'],action['role'])
    keyPresent = True
    if not key in serverTracker:
      keyPresent = False
    self.assertEqual(keyPresent, True, "Key not present")
    plauncher = serverTracker[key]
    self.assertTrue(plauncher.getpid() > 0, "Pid less than 0!")
    time.sleep(5)
    shell.stopProcess(key)
    keyPresent = False
    if key in serverTracker:
      keyPresent = True
    self.assertEqual(keyPresent, False, "Key present")
    processexists = True
    try:
      os.kill(serverTracker[key].getpid(),0)
    except:
      processexists = False
    self.assertEqual(processexists, False, "Process still exists!")
    self.assertTrue("testhello" in plauncher.out, "Output doesn't match!")
