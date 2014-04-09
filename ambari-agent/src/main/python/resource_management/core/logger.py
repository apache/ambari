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

__all__ = ["Logger"]
import logging
from resource_management.libraries.script.config_dictionary import UnknownConfiguration

class Logger:
  logger = logging.getLogger("resource_management")
  
  # unprotected_strings : protected_strings map
  sensitive_strings = {}
  
  @staticmethod
  def info(text):
    Logger.logger.info(Logger.get_protected_text(text))
  
  @staticmethod  
  def debug(text):
    Logger.logger.debug(Logger.get_protected_text(text))

  @staticmethod
  def info_resource(resource):
    Logger.info(Logger.get_protected_text(Logger._get_resource_repr(resource)))
  
  @staticmethod  
  def debug_resource(resource):
    Logger.debug(Logger.get_protected_text(Logger._get_resource_repr(resource)))
    
  @staticmethod
  def get_protected_text(text):
    """
    Replace passwords with [PROTECTED]
    """
    for unprotected_string, protected_string in list(Logger.sensitive_strings.items()):
      text = text.replace(unprotected_string, protected_string)
      
    return text
    
  @staticmethod  
  def _get_resource_repr(resource):
    MESSAGE_MAX_LEN = 256
    arguments_str = ""
    for x,y in list(resource.arguments.items()):
      
      # strip unicode 'u' sign
      if isinstance(y, str):
        # don't show long messages
        if len(y) > MESSAGE_MAX_LEN:
          y = '...'
        val = repr(y).lstrip('u')
      # don't show dicts of configurations
      # usually too long  
      elif isinstance(y, dict):
        val = "..."
      # for configs which didn't come
      elif isinstance(y, UnknownConfiguration):
        val = "[EMPTY]"
      # correctly output 'mode' (as they are octal values like 0755)
      elif y and x == 'mode':
        val = oct(y)
      else:
        val = repr(y)
      
      
      arguments_str += "'{0}': {1}, ".format(x, val)
      
    if arguments_str:  
      arguments_str = arguments_str[:-2]
    
    return "{0} {{{1}}}".format(resource, arguments_str)