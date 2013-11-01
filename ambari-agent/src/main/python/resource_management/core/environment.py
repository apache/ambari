#!/usr/bin/env python

__all__ = ["Environment","format"]

import logging
import sys
import os
import shutil
import time
from datetime import datetime
from string import Formatter

from resource_management.core import shell
from resource_management.core.exceptions import Fail
from resource_management.core.providers import find_provider
from resource_management.core.utils import AttributeDictionary, checked_unite
from resource_management.core.system import System


class Environment(object):
  _instances = []

  def __init__(self, basedir=None, params=None):
    """
    @param basedir: basedir/files, basedir/templates are the places where templates / static files
    are looked up
    @param params: configurations dictionary (this will be accessible in the templates)
    """
    self.log = logging.getLogger("resource_management")
    self.formatter = ConfigurationFormatter()
    self.reset(basedir, params)

  def reset(self, basedir, params):
    self.system = System.get_instance()
    self.config = AttributeDictionary()
    self.resources = {}
    self.resource_list = []
    self.delayed_actions = set()
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
      'params': params.copy(),
    })

  def backup_file(self, path):
    if self.config.backup:
      if not os.path.exists(self.config.backup.path):
        os.makedirs(self.config.backup.path, 0700)
      new_name = self.config.backup.prefix + path.replace('/', '-')
      backup_path = os.path.join(self.config.backup.path, new_name)
      self.log.info("backing up %s to %s" % (path, backup_path))
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
        
  def add_params(self, params):
    variables = [item for item in dir(params) if not item.startswith("__")]

    for variable in variables:
      value = getattr(params, variable)
      if not hasattr(value, '__call__'):
        if variable in self.config.params:
          raise Fail("Variable %s already exists in the resource management parameters" % variable)
        self.config.params[variable] = value
        
  def run_action(self, resource, action):
    self.log.debug("Performing action %s on %s" % (action, resource))

    provider_class = find_provider(self, resource.__class__.__name__,
                                   resource.provider)
    provider = provider_class(resource)
    try:
      provider_action = getattr(provider, 'action_%s' % action)
    except AttributeError:
      raise Fail("%r does not implement action %s" % (provider, action))
    provider_action()

    if resource.is_updated:
      for action, res in resource.subscriptions['immediate']:
        self.log.info(
          "%s sending %s action to %s (immediate)" % (resource, action, res))
        self.run_action(res, action)
      for action, res in resource.subscriptions['delayed']:
        self.log.info(
          "%s sending %s action to %s (delayed)" % (resource, action, res))
      self.delayed_actions |= resource.subscriptions['delayed']

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
      for resource in self.resource_list:
        self.log.debug("Running resource %r" % resource)
        
        if resource.initial_wait:
          time.sleep(resource.initial_wait)

        if resource.not_if is not None and self._check_condition(
          resource.not_if):
          self.log.debug("Skipping %s due to not_if" % resource)
          continue

        if resource.only_if is not None and not self._check_condition(
          resource.only_if):
          self.log.debug("Skipping %s due to only_if" % resource)
          continue

        for action in resource.action:
          if not resource.ignore_failures:
            self.run_action(resource, action)
          else:
            try:
              self.run_action(resource, action)
            except Exception:
                pass

      # Run delayed actions
      while self.delayed_actions:
        action, resource = self.delayed_actions.pop()
        self.run_action(resource, action)

  @classmethod
  def get_instance(cls):
    return cls._instances[-1]

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
    
class ConfigurationFormatter(Formatter):
  def format(self, format_string, *args, **kwargs):
    env = Environment.get_instance()
    variables = kwargs
    params = env.config.params
    
    result = checked_unite(variables, params)
    return self.vformat(format_string, args, result)
  
def format(format_string, *args, **kwargs):
  env = Environment.get_instance()
  variables = sys._getframe(1).f_locals
  
  result = checked_unite(kwargs, variables)
  result.pop("self", None) # self kwarg would result in an error
  return env.formatter.format(format_string, args, **result)
  
