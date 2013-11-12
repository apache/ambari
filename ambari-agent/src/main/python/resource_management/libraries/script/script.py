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
__all__ = ["Script"]

import sys
import json
import logging

from resource_management.core.environment import Environment
from resource_management.core.exceptions import Fail


class Script():
  """
  Executes a command for custom service. stdout and stderr are written to
  tmpoutfile and to tmperrfile respectively.
  """

  def execute(self):
    """
    Sets up logging;
    Parses command parameters and executes method relevant to command type
    """
    # set up logging (two separate loggers for stderr and stdout with different loglevels)
    logger = logging.getLogger('resource_management')
    logger.setLevel(logging.DEBUG)
    formatter = logging.Formatter('%(asctime)s - %(name)s - %(levelname)s - %(message)s')
    chout = logging.StreamHandler(sys.stdout)
    chout.setLevel(logging.DEBUG)
    chout.setFormatter(formatter)
    cherr = logging.StreamHandler(sys.stderr)
    cherr.setLevel(logging.ERROR)
    cherr.setFormatter(formatter)
    logger.addHandler(cherr)
    logger.addHandler(chout)
    # parse arguments
    if len(sys.argv) < 1+3:
      logger.error("Script expects at least 3 arguments")
      sys.exit(1)
    command_type = str.lower(sys.argv[1])
    # parse command parameters
    command_data_file = sys.argv[2]
    basedir = sys.argv[3]
    try:
      with open(command_data_file, "r") as f:
        pass
        Script.config = ConfigDictionary(json.load(f))
    except IOError:
      logger.exception("Can not read json file with command parameters: ")
      sys.exit(1)
    # Run class method mentioned by a command type
    self_methods = dir(self)
    if not command_type in self_methods:
      logger.error("Script {0} has not method '{1}'".format(sys.argv[0], command_type))
      sys.exit(1)
    method = getattr(self, command_type)
    try:
      with Environment(basedir) as env:
        method(env)
      env.run()
    except Fail:
      logger.exception("Got exception while executing method '{0}':".format(command_type))
      sys.exit(1)
      
  @staticmethod
  def get_config():
    return Script.config

  def fail_with_error(self, message):
    """
    Prints error message and exits with non-zero exit code
    """
    print("Error: " + message)
    sys.stderr.write("Error: " + message)
    sys.exit(1)

class ConfigDictionary(dict):
  """
  Immutable config dictionary
  """
  
  def __init__(self, dictionary):
    """
    Recursively turn dict to ConfigDictionary
    """
    for k, v in dictionary.iteritems():
      if isinstance(v, dict):
        dictionary[k] = ConfigDictionary(v)
        
    super(ConfigDictionary, self).__init__(dictionary)

  def __setitem__(self, name, value):
    raise Fail("Configuration dictionary is immutable!")

  def __getitem__(self, name):
    """
    Use Python types
    """
    value = super(ConfigDictionary, self).__getitem__(name)
    
    if value == "true":
      value = True
    elif value == "false":
      value = False
    else: 
      try:
        value = int(value)
      except (ValueError, TypeError):
        try:
          value =  float(value)
        except (ValueError, TypeError):
          pass
    
    return value