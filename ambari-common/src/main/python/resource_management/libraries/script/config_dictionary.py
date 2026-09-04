#!/usr/bin/env python3

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

from resource_management.core.exceptions import Fail
from resource_management.core.encryption import ensure_decrypted

IMMUTABLE_MESSAGE = """Configuration dictionary is immutable!

For adding dynamic properties to xml files please use {{varible_from_params}} substitutions.
Lookup xml files for {{ for examples. 
"""


class ConfigDictionary(dict):
  """
  Immutable config dictionary
  """

  def __init__(self, dictionary):
    """
    Recursively turn dict to ConfigDictionary
    """
    for k, v in dictionary.items():
      if isinstance(v, dict):
        dictionary[k] = ConfigDictionary(v)

    super(ConfigDictionary, self).__init__(dictionary)

  def __setitem__(self, name, value):
    raise Fail(IMMUTABLE_MESSAGE)

  def __getitem__(self, name):
    """
    - use Python types
    - enable lazy failure for unknown configs.
    """
    try:
      value = super(ConfigDictionary, self).__getitem__(name)
    except KeyError:
      return UnknownConfiguration(name)

    value = ensure_decrypted(value)
    if isinstance(value, bytes):
      value = value.decode('utf-8')

    if value == "true":
      value = True
    elif value == "false":
      value = False

    return value


class UnknownConfiguration(object):
  """
  Lazy failing for unknown config keys,
  supports nested attribute/item access like Python 2,
  but fails when used as a real value.
  Supports when accessed via the subscript operator ([]), attribute access (.),
  type conversion (str(), int()), or boolean context. It can be extended in future to support other use cases.
  """

  def __init__(self, name):
    self.name = name

  def __getitem__(self, key):
    # Keep chaining returning self for nested keys
    return self

  def __getattr__(self, attr):
    # Keep chaining returning self for nested attrs
    if attr.startswith('__') and attr.endswith('__'):
      # Allow special Python internals to avoid breaking introspection
      raise AttributeError(attr)
    return self

  # When used in boolean context, raise error
  def __bool__(self):
    # raise Fail(
    #    f"Configuration parameter '{self.name}' was not found in configurations dictionary!"
    # )
    return False

  __nonzero__ = __bool__  # Python 2 compatibility

  # When converted to string, raise error
  def __str__(self):
    raise Fail(
      f"Configuration parameter '{self.name}' was not found in configurations dictionary!"
    )

  def __repr__(self):
    return f"<UnknownConfiguration {self.name}>"

  # When called as a function, raise error
  def __call__(self, *args, **kwargs):
    raise Fail(
      f"Configuration parameter '{self.name}' was not found in configurations dictionary!"
    )

  # Add other conversions as needed (int, float, etc.):

  def __int__(self):
    raise Fail(
      f"Configuration parameter '{self.name}' was not found in configurations dictionary!"
    )

  def __float__(self):
    raise Fail(
      f"Configuration parameter '{self.name}' was not found in configurations dictionary!"
    )

  def __contains__(self, item):
    return False