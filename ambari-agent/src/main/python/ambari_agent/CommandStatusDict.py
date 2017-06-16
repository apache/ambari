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
import threading
import copy
from Grep import Grep

from ambari_agent import Constants

logger = logging.getLogger()

class CommandStatusDict():
  """
  Holds results for all commands that are being executed or have finished
  execution (but are not yet reported). Implementation is thread-safe.
  Dict format:
    task_id -> (command, cmd_report)
  """

  def __init__(self, initializer_module):
    """
    callback_action is called every time when status of some command is
    updated
    """
    self.current_state = {} # Contains all statuses
    self.lock = threading.RLock()
    self.initializer_module = initializer_module
    self.reported_reports = set()


  def put_command_status(self, command, new_report):
    """
    Stores new version of report for command (replaces previous)
    """
    key = command['taskId']
    with self.lock: # Synchronized
      self.current_state[key] = (command, new_report)
      self.reported_reports.discard(key)

    self.force_update_to_server([new_report])

  def force_update_to_server(self, reports):
    self.initializer_module.connection.send(message=reports, destination=Constants.COMMANDS_STATUS_REPORTS_ENDPOINT)

  def get_command_status(self, taskId):
    with self.lock:
      c = copy.copy(self.current_state[taskId][1])
    return c

  def generate_report(self):
    """
    Generates status reports about commands that are IN_PROGRESS, COMPLETE or
    FAILED. Statuses for COMPLETE or FAILED commands are forgotten after
    generation
    """
    self.generated_reports = []
    from ActionQueue import ActionQueue
    with self.lock: # Synchronized
      resultReports = []
      for key, item in self.current_state.items():
        command = item[0]
        report = item[1]
        if command ['commandType'] in [ActionQueue.EXECUTION_COMMAND, ActionQueue.BACKGROUND_EXECUTION_COMMAND]:
          if (report['status']) != ActionQueue.IN_PROGRESS_STATUS:
            resultReports.append(report)
            self.reported_reports.add(key)
          else:
            in_progress_report = self.generate_in_progress_report(command, report)
            resultReports.append(in_progress_report)
        elif command ['commandType'] in [ActionQueue.AUTO_EXECUTION_COMMAND]:
          logger.debug("AUTO_EXECUTION_COMMAND task deleted " + str(command['commandId']))
          self.reported_reports.add(key)
          pass
      return resultReports

  def clear_reported_reports(self):
    with self.lock:
      for key in self.reported_reports:
        del self.current_state[key]
      self.reported_reports = set()

  def generate_in_progress_report(self, command, report):
    """
    Reads stdout/stderr for IN_PROGRESS command from disk file
    and populates other fields of report.
    """
    from ActionQueue import ActionQueue
    try:
      tmpout = open(report['tmpout'], 'r').read()
      tmperr = open(report['tmperr'], 'r').read()
    except Exception, err:
      logger.warn(err)
      tmpout = '...'
      tmperr = '...'
    try:
      tmpstructuredout = open(report['structuredOut'], 'r').read()
    except Exception:
      tmpstructuredout = '{}'
    grep = Grep()
    output = grep.tail(tmpout, Grep.OUTPUT_LAST_LINES)
    inprogress = self.generate_report_template(command)
    inprogress.update({
      'stdout': output,
      'stderr': tmperr,
      'structuredOut': tmpstructuredout,
      'exitCode': 777,
      'status': ActionQueue.IN_PROGRESS_STATUS,
    })
    return inprogress


  def generate_report_template(self, command):
    """
    Generates stub dict for command.
    Other fields should be populated manually
    """
    stub = {
      'role': command['role'],
      'actionId': command['commandId'],
      'taskId': command['taskId'],
      'clusterName': command['clusterName'],
      'serviceName': command['serviceName'],
      'roleCommand': command['roleCommand']
    }
    return stub


