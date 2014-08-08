#!/usr/bin/env python
"""
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

Ambari Agent

"""
from resource_management import Script
from ambari_agent import shell

class CancelBackgroundTaskCommand(Script):
  def actionexecute(self, env):
    config = Script.get_config()

    cancel_command_pid = config['commandParams']['cancel_command_pid'] if config['commandParams'].has_key('cancel_command_pid') else None
    cancel_task_id = config['commandParams']['cancel_task_id']
    if cancel_command_pid == None:
      print "Nothing to cancel: there is no any task running with given taskId = '%s'" % cancel_task_id
    else:
      cancel_policy = config['commandParams']['cancel_policy']
      print "Send Kill to process pid = %s for task = %s with policy %s" % (cancel_command_pid, cancel_task_id, cancel_policy)
  
      shell.kill_process_with_children(cancel_command_pid)
      print "Process pid = %s for task = %s has been killed successfully" % (cancel_command_pid, cancel_task_id)
    
if __name__ == "__main__":
  CancelBackgroundTaskCommand().execute()
