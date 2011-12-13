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
import os, errno
from ambari_agent.ActionQueue import ActionQueue
from ambari_agent.AmbariConfig import AmbariConfig
from ambari_agent.FileUtil import getFilePath

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
    print ("path : " + path)
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
    print(result)
    self.assertEqual(result['exitCode'], 0, "installAndConfigAction test failed. Returned %d " % result['exitCode'])
    self.assertEqual(result['output'], path + "\n", "installAndConfigAction test failed Returned %s " % result['output'])
