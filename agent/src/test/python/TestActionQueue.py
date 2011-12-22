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
from ambari_agent.ActionQueue import ActionQueue
from ambari_agent.AmbariConfig import AmbariConfig
from ambari_agent.FileUtil import getFilePath
import os, errno, time

class TestActionQueue(TestCase):
  def test_ActionQueueStartStop(self):
    actionQueue = ActionQueue(AmbariConfig().getConfig())
    actionQueue.start()
    actionQueue.stop()
    actionQueue.join()
    self.assertEqual(actionQueue.stopped(), True, 'Action queue is not stopped.') 

  def test_RetryAction(self):
    action={'id' : 'tttt'}
    config = AmbariConfig().getConfig()
    actionQueue = ActionQueue(config)
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
    badAction = {
      'id' : 'tttt',
      'kind' : 'INSTALL_AND_CONFIG_ACTION',
      'workDirComponent' : 'abc-hdfs',
      'file' : configFile,
      'clusterDefinitionRevision' : 12,
      'command' : ['/bin/ls',"/foo/bar/badPath1234"]
    }
    path=getFilePath(action,path)
    goodAction = {
      'id' : 'tttt',
      'kind' : 'INSTALL_AND_CONFIG_ACTION',
      'workDirComponent' : 'abc-hdfs',
      'file' : configFile,
      'clusterDefinitionRevision' : 12,
      'command' : ['/bin/ls',path]
    }
    actionQueue.start()
    response = {'actions' : [badAction,goodAction]}
    actionQueue.maxRetries = 2
    actionQueue.sleepInterval = 1
    result = actionQueue.put(response)
    time.sleep(5)
    actionQueue.stop()
    actionQueue.join()
    results = actionQueue.result()
    self.assertEqual(len(results), 2, 'Number of results is not 2.')
    result = results[0]
    maxretries = config.get('command', 'maxretries')
    self.assertEqual(int(result['retryActionCount']), 
                     int(maxretries),
                     "Number of retries is %d and not %d" % 
                     (int(result['retryActionCount']), int(str(maxretries))))
    result = results[1]
    self.assertEqual(int(result['retryActionCount']), 
                     1,
                     "Number of retries is %d and not %d" % 
                     (int(result['retryActionCount']), 1))        
