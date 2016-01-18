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

import contextlib
import sys
import cStringIO
from functools import wraps
from resource_management.core.exceptions import Fail

PASSWORDS_HIDE_STRING = "[PROTECTED]"

class AttributeDictionary(object):
  def __init__(self, *args, **kwargs):
    d = kwargs
    if args:
      d = args[0]
    super(AttributeDictionary, self).__setattr__("_dict", d)

  def __setattr__(self, name, value):
    self[name] = value

  def __getattr__(self, name):
    if name in self.__dict__:
      return self.__dict__[name]
    try:
      return self[name]
    except KeyError:
      raise AttributeError("'%s' object has no attribute '%s'" % (self.__class__.__name__, name))

  def __setitem__(self, name, value):
    self._dict[name] = self._convert_value(value)

  def __getitem__(self, name):
    return self._convert_value(self._dict[name])

  def _convert_value(self, value):
    if isinstance(value, dict) and not isinstance(value, AttributeDictionary):
      return AttributeDictionary(value)
    return value

  def copy(self):
    return self.__class__(self._dict.copy())

  def update(self, *args, **kwargs):
    self._dict.update(*args, **kwargs)

  def items(self):
    return self._dict.items()
  
  def iteritems(self):
    return self._dict.iteritems()

  def values(self):
    return self._dict.values()

  def keys(self):
    return self._dict.keys()

  def pop(self, *args, **kwargs):
    return self._dict.pop(*args, **kwargs)

  def get(self, *args, **kwargs):
    return self._dict.get(*args, **kwargs)

  def __repr__(self):
    return self._dict.__repr__()

  def __unicode__(self):
    if isinstance(self._dict, str):
      return self._dict.__unicode__()
    else:
      return str(self._dict)

  def __str__(self):
    return self._dict.__str__()

  def __iter__(self):
    return self._dict.__iter__()

  def __getstate__(self):
    return self._dict

  def __setstate__(self, state):
    super(AttributeDictionary, self).__setattr__("_dict", state)
    
def checked_unite(dict1, dict2):
  for key in dict1:
    if key in dict2:
      if not dict2[key] is dict1[key]: # it's not a big deal if this is the same variable
        raise Fail("Variable '%s' already exists more than once as a variable/configuration/kwarg parameter. Cannot evaluate it." % key)
  
  result = dict1.copy()
  result.update(dict2)
  
  return result

@contextlib.contextmanager
def suppress_stdout():
  save_stdout = sys.stdout
  sys.stdout = cStringIO.StringIO()
  yield
  sys.stdout = save_stdout

class PasswordString(unicode):
  """
  Logger replaces this strings with [PROTECTED]
  """
  
  def __init__(self, value):
    self.value = value
    
  def __str__(self):
    return value
  
  def __repr__(self):
    return PASSWORDS_HIDE_STRING
  
def lazy_property(undecorated):
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
  