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

import logging
import logging.handlers
import Queue
import threading
from FileUtil import writeFile, createStructure, deleteStructure
from shell import shellRunner
import json
import os

logger = logging.getLogger()

class ActionQueue(threading.Thread):
  global q, r, clusterId, clusterDefinitionRevision
  q = Queue.Queue()
  r = Queue.Queue()
  clusterId = 'unknown'
  clusterDefinitionRevision = 0

  def __init__(self):
    global clusterId, clusterDefinitionRevision 
    threading.Thread.__init__(self)
    self.sh = shellRunner()

  def put(self, response):
    actions = response['actions']
    for action in actions:
      q.put(action)

  def run(self):
    global clusterId, clusterDefinitionRevision
    while True:
      while not q.empty():
        action = q.get()
        switches = {
                     'START_ACTION'            : self.startAction,
                     'STOP_ACTION'             : self.stopAction,
                     'RUN_ACTION'              : self.runAction,
                     'CREATE_STRUCTURE_ACTION' : self.createStructureAction,
                     'DELETE_STRUCTURE_ACTION' : self.deleteStructureAction,
                     'WRITE_FILE_ACTION'       : self.writeFileAction
                   }
        result = switches.get(action['kind'], self.unknownAction)(action)
        # Update the result
        r.put(result)

  # Store action result to agent response queue
  def result(self):
    result = []
    while not r.empty():
      result.append(r.get())
    return result

  # Generate default action response
  def genResult(self, action):
    result = { 
               'id'                        : action['id'],
               'clusterId'                 : action['clusterId'],
               'kind'                      : action['kind'],
               'clusterDefinitionRevision' : action['clusterDefinitionRevision'],
               'component'                 : action['component'],
               'role'                      : action['role']
             }
    return result

  # Run start action, start a server process and
  # track the liveness of the children process
  def startAction(self, action):
    result = self.genResult(action)
    return self.sh.startProcess(action['clusterId'],
      action['clusterDefinitionRevision'],
      action['component'], 
      action['role'], 
      action['command'], 
      action['user'], result)

  # Run stop action, stop a server process.
  def stopAction(self, action):
    result = self.genResult(action)
    return self.sh.stopProcess(action['clusterId'], 
      action['clusterDefinitionRevision'],
      action['component'],
      action['role'], 
      action['signal'], result)

  # Write file action
  def writeFileAction(self, action):
    result = self.genResult(action)
    return writeFile(action, result)

  # Run command action
  def runAction(self, action):
    result = self.genResult(action)
    return self.sh.runAction(action['clusterId'], 
      action['component'],
      action['role'],
      action['user'], 
      action['command'], 
      action['cleanUpCommand'], result)

  # Create directory structure for cluster
  def createStructureAction(self, action):
    result = self.genResult(action)
    result['exitCode'] = 0
    return createStructure(action, result)

  # Delete directory structure for cluster
  def deleteStructureAction(self, action):
    result = self.genResult(action)
    result['exitCode'] = 0
    return deleteStructure(action, result)

  # Handle unknown action
  def unknownAction(self, action):
    logger.error('Unknown action: %s' % action['id'])
    result = { 'id': action['id'] }
    return result

  # Discover agent idle state
  def isIdle(self):
    return q.empty()

