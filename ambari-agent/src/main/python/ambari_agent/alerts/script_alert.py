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
"""

import imp
import logging
import os
from alerts.base_alert import BaseAlert
from symbol import parameters

logger = logging.getLogger()

class ScriptAlert(BaseAlert):
  def __init__(self, alert_meta, alert_source_meta):
    """ ScriptAlert reporting structure is output from the script itself """
    
    alert_source_meta['reporting'] = {
      'ok': { 'text': '{0}' },
      'warning': { 'text': '{0}' },
      'critical': { 'text': '{0}' },
      'unknown': { 'text': '{0}' }
    }
    
    super(ScriptAlert, self).__init__(alert_meta, alert_source_meta)
    
    self.path = None
    self.stacks_dir = None
    self.host_scripts_dir = None
    
    if 'path' in alert_source_meta:
      self.path = alert_source_meta['path']
      
    if 'stacks_directory' in alert_source_meta:
      self.stacks_dir = alert_source_meta['stacks_directory']
      
    if 'host_scripts_directory' in alert_source_meta:
      self.host_scripts_dir = alert_source_meta['host_scripts_directory']
      
    # execute the get_tokens() method so that this script correctly populates
    # its list of keys
    try:
      cmd_module = self._load_source()
      tokens = cmd_module.get_tokens()
        
      # for every token, populate the array keys that this alert will need
      if tokens is not None:
        for token in tokens:
          # append the key to the list of keys for this alert
          self._find_lookup_property(token)
    except:
      logger.exception("Unable to parameterize tokens for script {0}".format(self.path))
      pass
              
    
  def _collect(self):
    cmd_module = self._load_source()
    if cmd_module is not None:
      # convert the dictionary from 
      # {'foo-site/bar': 'baz'} into 
      # {'{{foo-site/bar}}': 'baz'}1
      parameters = {}
      for key in self.config_value_dict:
        parameters['{{' + key + '}}'] = self.config_value_dict[key]
      
      return cmd_module.execute(parameters)
    else:
      return ((self.RESULT_UNKNOWN, ["Unable to execute script {0}".format(self.path)]))
    

  def _load_source(self):
    if self.path is None and self.stack_path is None and self.host_scripts_dir is None:
      raise Exception("The attribute 'path' must be specified")

    paths = self.path.split('/')
    path_to_script = self.path
    
    # if the path doesn't exist and stacks dir is defined, try that
    if not os.path.exists(path_to_script) and self.stacks_dir is not None:      
      path_to_script = os.path.join(self.stacks_dir, *paths)

    # if the path doesn't exist and the host script dir is defined, try that
    if not os.path.exists(path_to_script) and self.host_scripts_dir is not None:
      path_to_script = os.path.join(self.host_scripts_dir, *paths)

    # if the path can't be evaluated, throw exception      
    if not os.path.exists(path_to_script) or not os.path.isfile(path_to_script):
      raise Exception(
        "Unable to find '{0}' as an absolute path or part of {1} or {2}".format(self.path,
          self.stacks_dir, self.host_scripts_dir))

    if logger.isEnabledFor(logging.DEBUG):
      logger.debug("Executing script check {0}".format(path_to_script))

          
    if (not path_to_script.endswith('.py')):
      logger.error("Unable to execute script {0}".format(path_to_script))
      return None
    
    return imp.load_source(self._find_value('name'), path_to_script)