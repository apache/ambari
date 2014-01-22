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
import Queue
import threading
import pprint
import os
import json
from AgentException import AgentException

logger = logging.getLogger()

class FileCache():
  """
  Provides caching and lookup for service metadata files.
  If service metadata is not available at cache,
  downloads relevant files from the server.
  """

  def __init__(self, config):
    self.service_component_pool = {}
    self.config = config
    self.cache_dir = config.get('agent', 'cache_dir')


  def get_service_base_dir(self, service_subpath):
    """
    Returns a base directory for service
    """
    service_base_dir = os.path.join(self.cache_dir, "stacks", service_subpath)
    if not os.path.isdir(service_base_dir):
      # TODO: Metadata downloading will be implemented at Phase 2
      # As of now, all stack definitions are packaged and distributed with
      # agent rpm
      message = "Service base dir not found at expected location {0}".\
        format(service_base_dir)
      raise AgentException(message)
    return service_base_dir


  def get_hook_base_dir(self, command):
    """
    Returns a base directory for hooks
    """
    try:
      hooks_subpath = command['commandParams']['hooks_folder']
    except KeyError:
      return None
    hook_base_path = os.path.join(self.cache_dir, "stacks", hooks_subpath)
    if not os.path.isdir(hook_base_path):
      # TODO: Metadata downloading will be implemented at Phase 2
      # As of now, all stack definitions are packaged and distributed with
      # agent rpm
      message = "Hook scripts dir for not found at " \
                "expected location {0}".format(hook_base_path)
      raise AgentException(message)
    return hook_base_path

