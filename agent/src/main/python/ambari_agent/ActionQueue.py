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
import json
import os

logger = logging.getLogger()

class ActionQueue(threading.Thread):
  global q, r, clusterId, bluePrintName, bluePrintRevision, checkFile
  q = Queue.Queue()
  r = Queue.Queue()
  clusterId = 'unknown'
  bluePrintName = 'unknown'
  bluePrintRevision = 'unknown'
  checkFile = '/tmp/blueprint'

  def __init__(self):
    global clusterId, bluePrintName, bluePrintRevision, checkFile
    threading.Thread.__init__(self)
    if 'AMBARI_LOG_DIR' in os.environ:
      checkFile = os.environ['AMBARI_LOG_DIR']+"/blueprint"
    if os.path.exists(checkFile):
      f = open(checkFile, 'r')
      data = json.load(f)
      clusterId = data['clusterId']
      bluePrintName = data['bluePrintName']
      bluePrintRevision = data['bluePrintRevision']
      f.close()
    self.sh = shellRunner()

  def put(self, response):
    actions = response['actions']
    for action in actions:
      q.put(action)

  # dispatch action types
  def run(self):
    global clusterId, bluePrintName, bluePrintRevision
    while True:
      while not q.empty():
        action = q.get()
        switches = {
                     'START_ACTION': self.startAction,
                     'STOP_ACTION': self.stopAction,
                     'RUN_ACTION': self.runAction
                   }
        result = switches.get(action['kind'], self.unknownAction)(action)
        # Store the blue print check point file
        if clusterId!=action['clusterId'] or bluePrintName!=action['bluePrintName'] or bluePrintRevision!=action['bluePrintRevision']:
          clusterId = action['clusterId']
          bluePrintName = action['bluePrintName']
          bluePrintRevision = action['bluePrintRevision']
          output = { 
                     'clusterId' : clusterId,
                     'bluePrintName' : bluePrintName,
                     'bluePrintRevision' : bluePrintRevision
                   }
          data = json.dumps(output)
          info = ['ambari-write-file',os.getuid(),os.getgid(),'0700','/tmp/blueprint',data]
          writeFile(info)
        # Update the result
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
               'id'                : action['id'], 
               'clusterId'         : action['clusterId'],
               'kind'              : action['kind'], 
               'component'         : action['component'], 
               'role'              : action['role'],
               'bluePrintName'     : action['bluePrintName'],
               'bluePrintRevision' : action['bluePrintRevision']
             }
    self.sh.startProcess(action['component'], action['role'], action['commands'][0]['cmd'], action['user'])
    return result

  # Run stop action, stop a server process.
  def stopAction(self, action):
    result = { 
               'id'                : action['id'], 
               'kind'              : action['kind'], 
               'clusterId'         : action['clusterId'], 
               'component'         : action['component'],
               'role'              : action['role'],
               'bluePrintName'     : action['bluePrintName'],
               'bluePrintRevision' : action['bluePrintRevision']
             }
    self.sh.stopProcess(action['component'], action['role'], action['signal'])
    return result

  # Run commands action
  def runAction(self, action):
    result = { 
               'id'                : action['id'],
               'clusterId'         : action['clusterId'],
               'kind'              : action['kind'],
               'bluePrintName'     : action['bluePrintName'],
               'bluePrintRevision' : action['bluePrintRevision']
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
      else:
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

  # Report current clusterId
  def getClusterId(self):
    return clusterId

  # Report blue print name
  def getBluePrintName(self):
    return bluePrintName

  # Report blue print revision
  def getBluePrintRevision(self):
    return bluePrintRevision
