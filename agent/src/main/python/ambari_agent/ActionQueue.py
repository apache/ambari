#!/usr/bin/env python

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

import logging
import logging.handlers
import Queue
import threading
from FileUtil import writeFile
from shell import shellRunner

logger = logging.getLogger()

class ActionQueue(threading.Thread):
  global q, r
  q = Queue.Queue()
  r = Queue.Queue()

  def __init__(self):
    threading.Thread.__init__(self)
    self.sh = shellRunner()

  def put(self, response):
    actions = response['actions']
    for action in actions:
      q.put(action)

  # dispatch action types
  def run(self):
    while True:
      while not q.empty():
        action = q.get()
        switches = {
                     'START_ACTION': self.startAction,
                     'STOP_ACTION': self.stopAction,
                     'RUN_ACTION': self.runAction
                   }
        result = switches.get(action['kind'], self.unknownAction)(action)
        r.put(result)

  def result(self):
    result = []
    while not r.empty():
      result.append(r.get())
    return result

  # Run start action, start a server process and
  # track the liveness of the children process
  def startAction(self, action):
    result = { 
               'id'         : action['id'], 
               'clusterId'  : action['clusterId'],
               'kind'       : action['kind'], 
               'serverName' : action['serverName'] 
             }
    self.sh.startProcess(action['serverName'], action['commands'][0]['cmd'], action['user'])
    return result

  # Run stop action, stop a server process.
  def stopAction(self, action):
    result = { 
               'id': action['id'], 
               'kind': action['kind'], 
               'clusterId': action['clusterId'], 
               'serverName': action['serverName']
             }
    self.sh.stopProcess(action['serverName'], action['signal'])
    return result

  # Run commands action
  def runAction(self, action):
    result = { 
               'id': action['id'],
               'clusterId' : action['clusterId'],
               'kind'       : action['kind']
             }
    return self.runCommands(action['commands'], action['cleanUpCommands'], result)

  # run commands
  def runCommands(self, commands, cleanUps, result):
    failure = False
    cmdResult = []
    for cmd in commands:
      script = cmd['cmd']
      if script[0]=="ambari-write-file":
        response = writeFile(script)
      else
        response = self.sh.run(script, cmd['user'])
      exitCode = response['exit_code']
      if exitCode==0:
        cmdResult.append({'exitCode':exitCode})
      else:
        failure=True
        cmdResult.append({'exitCode':exitCode, 'stdout':response['stdout'], 'stderr':response['stderr']})
    result['commandResults'] = cmdResult
    if(failure):
      cleanUpResult = []
      for cmd in cleanUps:
        script = cmd['cmd']
        response = self.sh.run(script, cmd['user'])
        exitCode = response['exit_code']
        if exitCode==0:
          cleanUpResult.append({'exitCode':exitCode})
        else:
          cleanUpResult.append({'exitCode':exitCode,'stdout':response['stdout'],'stderr':response['stderr']})
      result['cleanUpCommandResults'] = cleanUpResult
    return result

  # Handle unknown action
  def unknownAction(self, action):
    logger.error('Unknown action: %s' % action['id'])
    result = { 'id': action['id'] }
    return result

  # Discover agent idle state
  def isIdle(self):
    return q.empty()
