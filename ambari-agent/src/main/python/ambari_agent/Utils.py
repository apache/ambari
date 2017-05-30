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
import threading
from functools import wraps

class BlockingDictionary():
  """
  A dictionary like class.
  Which allow putting an item. And retrieving it in blocking way (the caller is blocked until item is available).
  """
  def __init__(self, dictionary=None):
    self.dict = {} if dictionary is None else dictionary
    self.cv = threading.Condition()
    self.put_event = threading.Event()
    self.dict_lock = threading.RLock()

  def put(self, key, value):
    """
    Thread-safe put to dictionary.
    """
    with self.dict_lock:
      self.dict[key] = value
    self.put_event.set()

  def blocking_pop(self, key):
    """
    Block until a key in dictionary is available and than pop it.
    """
    with self.dict_lock:
      if key in self.dict:
        return self.dict.pop(key)

    while True:
      self.put_event.wait()
      self.put_event.clear()
      with self.dict_lock:
        if key in self.dict:
          return self.dict.pop(key)

  def __repr__(self):
    return self.dict.__repr__()

  def __str__(self):
    return self.dict.__str__()

class Utils(object):
  @staticmethod
  def make_immutable(value):
    if isinstance(value, dict):
      return ImmutableDictionary(value)
    if isinstance(value, (list, tuple)):
      return tuple([Utils.make_immutable(x) for x in value])

    return value

  @staticmethod
  def get_mutable_copy(param):
    if isinstance(param, dict):
      mutable_dict = {}

      for k, v in param.iteritems():
        mutable_dict[k] = Utils.get_mutable_copy(v)

      return mutable_dict
    elif isinstance(param, (list, tuple)):
      return [Utils.get_mutable_copy(x) for x in param]

    return param

class ImmutableDictionary(dict):
  def __init__(self, dictionary):
    """
    Recursively turn dict to ImmutableDictionary
    """
    for k, v in dictionary.iteritems():
        dictionary[k] = Utils.make_immutable(v)

    super(ImmutableDictionary, self).__init__(dictionary)

  def __getattr__(self, name):
    """
    Access to self['attribute'] as self.attribute
    """
    if name in self:
      return self[name]

    try:
      return self[name]
    except KeyError:
      raise AttributeError("'%s' object has no attribute '%s'" % (self.__class__.__name__, name))

def raise_immutable_error(*args, **kwargs):
  """
  PLEASE MAKE SURE YOU NEVER UPDATE CACHE on agent side. The cache should contain exactly the data received from server.
  Modifications on agent-side will lead to unnecessary cache sync every agent registration. Which is a big concern on perf clusters!
  Also immutability can lead to multithreading issues.
  """
  raise TypeError("The dictionary is immutable cannot change it")

ImmutableDictionary.__setitem__ = raise_immutable_error
ImmutableDictionary.__delitem__ = raise_immutable_error
ImmutableDictionary.clear = raise_immutable_error
ImmutableDictionary.pop = raise_immutable_error
ImmutableDictionary.update = raise_immutable_error


def lazy_property(undecorated):
  """
  Only run the function decorated once. Next time return cached value.
  """
  name = '_' + undecorated.__name__

  @property
  @wraps(undecorated)
  def decorated(self):
    try:
      return getattr(self, name)
    except AttributeError:
      v = undecorated(self)
      setattr(self, name, v)
      return v

  return decorated