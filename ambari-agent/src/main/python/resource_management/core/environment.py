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

__all__ = ["Environment"]

import os
import shutil
import time
from datetime import datetime

from resource_management.core import shell
from resource_management.core.exceptions import Fail
from resource_management.core.providers import find_provider
from resource_management.core.utils import AttributeDictionary
from resource_management.core.system import System
from resource_management.core.logger import Logger


class Environment(object):
  _instances = []

  def __init__(self, basedir=None, test_mode=False):
    """
    @param basedir: basedir/files, basedir/templates are the places where templates / static files
    are looked up
    @param test_mode: if this is enabled, resources won't be executed until manualy running env.run().
    """
    self.reset(basedir, test_mode)

  def reset(self, basedir, test_mode):
    self.system = System.get_instance()
    self.config = AttributeDictionary()
    self.resources = {}
    self.resource_list = []
    self.delayed_actions = set()
    self.test_mode = test_mode
    self.update_config({
      # current time
      'date': datetime.now(),
      # backups here files which were rewritten while executing File resource
      'backup.path': '/tmp/resource_management/backup',
      # prefix for this files 
      'backup.prefix': datetime.now().strftime("%Y%m%d%H%M%S"),
      # dir where templates,failes dirs are 
      'basedir': basedir, 
      # variables, which can be used in templates
      'params': {},
    })

  def backup_file(self, path):
    if self.config.backup:
      if not os.path.exists(self.config.backup.path):
        os.makedirs(self.config.backup.path, 0700)
      new_name = self.config.backup.prefix + path.replace('/', '-')
      backup_path = os.path.join(self.config.backup.path, new_name)
      Logger.info("backing up %s to %s" % (path, backup_path))
      shutil.copy(path, backup_path)

  def update_config(self, attributes, overwrite=True):
    for key, value in attributes.items():
      attr = self.config
      path = key.split('.')
      for pth in path[:-1]:
        if pth not in attr:
          attr[pth] = AttributeDictionary()
        attr = attr[pth]
      if overwrite or path[-1] not in attr:
        attr[path[-1]] = value
        
  def set_params(self, arg):
    """
    @param arg: is a dictionary of configurations, or a module with the configurations
    """
    if isinstance(arg, dict):
      variables = arg
    else:
      variables = dict((var, getattr(arg, var)) for var in dir(arg))
    
    for variable, value in variables.iteritems():
      # don't include system variables, methods, classes, modules
      if not variable.startswith("__") and \
          not hasattr(value, '__call__')and \
          not hasattr(value, '__file__'):
        self.config.params[variable] = value
        
  def run_action(self, resource, action):
    Logger.debug("Performing action %s on %s" % (action, resource))

    provider_class = find_provider(self, resource.__class__.__name__,
                                   resource.provider)
    provider = provider_class(resource)
    try:
      provider_action = getattr(provider, 'action_%s' % action)
    except AttributeError:
      raise Fail("%r does not implement action %s" % (provider, action))
    provider_action()

  def _check_condition(self, cond):
    if hasattr(cond, '__call__'):
      return cond()

    if isinstance(cond, basestring):
      ret, out = shell.call(cond)
      return ret == 0

    raise Exception("Unknown condition type %r" % cond) 
    
  def run(self):
    with self:
      # Run resource actions
      while self.resource_list:
        resource = self.resource_list.pop(0)
        Logger.info_resource(resource)
        
        if resource.initial_wait:
          time.sleep(resource.initial_wait)

        if resource.not_if is not None and self._check_condition(
          resource.not_if):
          Logger.info("Skipping %s due to not_if" % resource)
          continue

        if resource.only_if is not None and not self._check_condition(
          resource.only_if):
          Logger.info("Skipping %s due to only_if" % resource)
          continue

        for action in resource.action:
          if not resource.ignore_failures:
            self.run_action(resource, action)
          else:
            try:
              self.run_action(resource, action)
            except Exception as ex:
              Logger.info("Skipping failure of %s due to ignore_failures. Failure reason: %s" % (resource, str(ex)))
              pass

      # Run delayed actions
      while self.delayed_actions:
        action, resource = self.delayed_actions.pop()
        self.run_action(resource, action)

  @classmethod
  def get_instance(cls):
    return cls._instances[-1]
  
  @classmethod
  def get_instance_copy(cls):
    """
    Copy only configurations, but not resources execution state
    """
    old_instance = cls.get_instance()
    new_instance = Environment()
    new_instance.config = old_instance.config.copy()
    
    return new_instance

  def __enter__(self):
    self.__class__._instances.append(self)
    return self

  def __exit__(self, exc_type, exc_val, exc_tb):
    self.__class__._instances.pop()
    return False

  def __getstate__(self):
    return dict(
      config=self.config,
      resources=self.resources,
      resource_list=self.resource_list,
      delayed_actions=self.delayed_actions,
    )

  def __setstate__(self, state):
    self.__init__()
    self.config = state['config']
    self.resources = state['resources']
    self.resource_list = state['resource_list']
    self.delayed_actions = state['delayed_actions']
