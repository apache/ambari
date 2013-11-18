#!/usr/bin/env python2.6
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

__all__ = ["Resource", "ResourceArgument", "ForcedListArgument",
           "BooleanArgument"]

import logging
from resource_management.core.exceptions import Fail, InvalidArgument
from resource_management.core.environment import Environment

class ResourceArgument(object):
  def __init__(self, default=None, required=False, allow_override=False):
    self.required = False # Prevents the initial validate from failing
    if hasattr(default, '__call__'):
      self.default = default
    else:
      self.default = self.validate(default)
    self.required = required
    self.allow_override = allow_override

  def validate(self, value):
    if self.required and value is None:
      raise InvalidArgument("Required argument %s missing" % self.name)
    return value


class ForcedListArgument(ResourceArgument):
  def validate(self, value):
    value = super(ForcedListArgument, self).validate(value)
    if not isinstance(value, (tuple, list)):
      value = [value]
    return value


class BooleanArgument(ResourceArgument):
  def validate(self, value):
    value = super(BooleanArgument, self).validate(value)
    if not value in (True, False):
      raise InvalidArgument(
        "Expected a boolean for %s received %r" % (self.name, value))
    return value


class Accessor(object):
  def __init__(self, name):
    self.name = name

  def __get__(self, obj, cls):
    try:
      return obj.arguments[self.name]
    except KeyError:
      val = obj._arguments[self.name].default
      if hasattr(val, '__call__'):
        val = val(obj)
      return val

  def __set__(self, obj, value):
    obj.arguments[self.name] = obj._arguments[self.name].validate(value)


class ResourceMetaclass(type):
  # def __new__(cls, name, bases, attrs):
  #     super_new = super(ResourceMetaclass, cls).__new__
  #     return super_new(cls, name, bases, attrs)

  def __init__(mcs, _name, bases, attrs):
    mcs._arguments = getattr(bases[0], '_arguments', {}).copy()
    for key, value in list(attrs.items()):
      if isinstance(value, ResourceArgument):
        value.name = key
        mcs._arguments[key] = value
        setattr(mcs, key, Accessor(key))
  
  
class Resource(object):
  __metaclass__ = ResourceMetaclass

  log = logging.getLogger("resource_management.resource")
  is_updated = False

  action = ForcedListArgument(default="nothing")
  ignore_failures = BooleanArgument(default=False)
  notifies = ResourceArgument(default=[]) # this is not supported/recommended
  subscribes = ResourceArgument(default=[]) # this is not supported/recommended
  not_if = ResourceArgument() # pass command e.g. not_if = ('ls','/root/jdk')
  only_if = ResourceArgument() # pass command
  initial_wait = ResourceArgument() # in seconds

  actions = ["nothing"]
  
  def __new__(cls, name, env=None, provider=None, **kwargs):
    if isinstance(name, list):
      while len(name) != 1:
        cls(name.pop(0), env, provider, **kwargs)
        
      name = name[0]
    
    env = env or Environment.get_instance()
    provider = provider or getattr(cls, 'provider', None)
    
    r_type = cls.__name__
    if r_type not in env.resources:
      env.resources[r_type] = {}
    if name not in env.resources[r_type]:
      obj = super(Resource, cls).__new__(cls)
      env.resources[r_type][name] = obj
      env.resource_list.append(obj)
      return obj

    obj = env.resources[r_type][name]
    if obj.provider != provider:
      raise Fail("Duplicate resource %r with a different provider %r != %r" % (
      obj, provider, obj.provider))

    obj.override(**kwargs)
    return obj

  def __init__(self, name, env=None, provider=None, **kwargs):
    if isinstance(name, list):
      name = name.pop(0)
    
    if hasattr(self, 'name'):
      return

    self.env = env or Environment.get_instance()
    self.name = name
     
    self.provider = provider or getattr(self, 'provider', None)

    self.arguments = {}
    for key, value in kwargs.items():
      try:
        arg = self._arguments[key]
      except KeyError:
        raise Fail("%s received unsupported argument %s" % (self, key))
      else:
        try:
          self.arguments[key] = arg.validate(value)
        except InvalidArgument, exc:
          raise InvalidArgument("%s %s" % (self, exc))

    Resource.log.debug("New resource %s: %s" % (self, self.arguments))
    self.subscriptions = {'immediate': set(), 'delayed': set()}

    for sub in self.subscribes:
      if len(sub) == 2:
        action, res = sub
        immediate = False
      else:
        action, res, immediate = sub

      res.subscribe(action, self, immediate)

    for sub in self.notifies:
      self.subscribe(*sub)

    self.validate()

  def validate(self):
    pass

  def subscribe(self, action, resource, immediate=False):
    imm = "immediate" if immediate else "delayed"
    sub = (action, resource)
    self.subscriptions[imm].add(sub)

  def updated(self):
    self.is_updated = True

  def override(self, **kwargs):
    for key, value in kwargs.items():
      try:
        arg = self._arguments[key]
      except KeyError:
        raise Fail("%s received unsupported argument %s" % (self, key))
      else:
        if value != self.arguments.get(key):
          if not arg.allow_override:
            raise Fail(
              "%s doesn't allow overriding argument '%s'" % (self, key))

          try:
            self.arguments[key] = arg.validate(value)
          except InvalidArgument, exc:
            raise InvalidArgument("%s %s" % (self, exc))
    self.validate()

  def __repr__(self):
    return "%s['%s']" % (self.__class__.__name__, self.name)

  def __unicode__(self):
    return u"%s['%s']" % (self.__class__.__name__, self.name)

  def __getstate__(self):
    return dict(
      name=self.name,
      provider=self.provider,
      arguments=self.arguments,
      subscriptions=self.subscriptions,
      subscribes=self.subscribes,
      notifies=self.notifies,
      env=self.env,
    )

  def __setstate__(self, state):
    self.name = state['name']
    self.provider = state['provider']
    self.arguments = state['arguments']
    self.subscriptions = state['subscriptions']
    self.subscribes = state['subscribes']
    self.notifies = state['notifies']
    self.env = state['env']

    Resource.log = logging.getLogger("resource_management.resource")

    self.validate()
