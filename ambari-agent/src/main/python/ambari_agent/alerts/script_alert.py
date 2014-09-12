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

import imp
import logging
import os
from alerts.base_alert import BaseAlert

logger = logging.getLogger()

class ScriptAlert(BaseAlert):
  def __init__(self, alert_meta, alert_source_meta):
    ''' ScriptAlert reporting structure is output from the script itself '''
    
    alert_source_meta['reporting'] = {
      'ok': { 'text': '{0}' },
      'warning': { 'text': '{0}' },
      'critical': { 'text': '{0}' },
      'unknown': { 'text': '{0}' }
    }
    
    super(ScriptAlert, self).__init__(alert_meta, alert_source_meta)
    
    self.path = None
    if 'path' in alert_source_meta:
      self.path = alert_source_meta['path']
      
    if 'stacks_dir' in alert_source_meta:
      self.stacks_dir = alert_source_meta['stacks_dir']
    
  def _collect(self):
    if self.path is None and self.stack_path is None:
      raise Exception("The attribute 'path' must be specified")

    path_to_script = self.path
    if not os.path.exists(self.path) and self.stacks_dir is not None:
      paths = self.path.split('/')
      path_to_script = os.path.join(self.stacks_dir, *paths)
      
    if not os.path.exists(path_to_script) or not os.path.isfile(path_to_script):
      raise Exception(
        "Resolved script '{0}' does not appear to be a script".format(
          path_to_script))

    if logger.isEnabledFor(logging.DEBUG):
      logger.debug("Executing script check {0}".format(path_to_script))

          
    if (path_to_script.endswith('.py')):
      cmd_module = imp.load_source(self._find_value('name'), path_to_script)
      return cmd_module.execute()
    else:
      return ((self.RESULT_UNKNOWN, ["could not execute script {0}".format(path_to_script)]))
