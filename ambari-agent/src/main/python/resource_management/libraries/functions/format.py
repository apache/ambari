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

__all__ = ["format"]
import sys
from string import Formatter
from resource_management.core.exceptions import Fail
from resource_management.core.utils import checked_unite
from resource_management.core.environment import Environment
from resource_management.core.logger import Logger


class ConfigurationFormatter(Formatter):
  def format(self, format_string, *args, **kwargs):
    env = Environment.get_instance()
    variables = kwargs
    params = env.config.params
    all_params = checked_unite(variables, params)
    
    self.convert_field = self.convert_field_protected
    result_protected = self.vformat(format_string, args, all_params)
    
    self.convert_field = self.convert_field_unprotected
    result_unprotected = self.vformat(format_string, args, all_params)
    
    if result_protected != result_unprotected:
      Logger.sensitive_strings[result_unprotected] = result_protected
      
    return result_unprotected
  
  def convert_field_unprotected(self, value, conversion):
    if conversion == 'p':
      return value
      
    return super(ConfigurationFormatter, self).convert_field(value, conversion)
  
  def convert_field_protected(self, value, conversion):
    """
    Enable masking sensitive information like
    passwords from logs via !p (password) format flag.
    """
    if conversion == 'p':
      return "[PROTECTED]"
      
    return super(ConfigurationFormatter, self).convert_field(value, conversion)
      
  
def format(format_string, *args, **kwargs):
  variables = sys._getframe(1).f_locals
  
  result = checked_unite(kwargs, variables)
  result.pop("self", None) # self kwarg would result in an error
  return ConfigurationFormatter().format(format_string, args, **result)
