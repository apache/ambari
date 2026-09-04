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

import os
import re
import time

import javaproperties

# Apache License Header
ASF_LICENSE_HEADER = """
# Copyright 2011 The Apache Software Foundation
#
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
"""


# Ambari compatibility facade around the official javaproperties package.
class Properties(object):
  def __init__(self, props=None):
    self._props = {}
    self._origprops = {}
    self._keymap = {}

    self.bspacere = re.compile(r"\\(?!\s$)")

  def process_pair(self, key, value):
    """
    Adds or overrides the property with the given key.
    """
    oldkey = key
    oldvalue = value
    keyparts = self.bspacere.split(key)
    strippable = False
    lastpart = keyparts[-1]
    if lastpart.find("\\ ") != -1:
      keyparts[-1] = lastpart.replace("\\", "")
    elif lastpart and lastpart[-1] == " ":
      strippable = True
    key = "".join(keyparts)
    if strippable:
      key = key.strip()
      oldkey = oldkey.strip()
    oldvalue = self.unescape(oldvalue)
    value = self.unescape(value)
    self._store_pair(key, value, oldkey, oldvalue)

  def _store_pair(self, key, value, oldkey, oldvalue):
    self._props[key] = None if value is None else value.strip()
    if key in self._keymap:
      oldkey = self._keymap.get(key)
      self._origprops[oldkey] = None if oldvalue is None else oldvalue.strip()
    else:
      self._origprops[oldkey] = None if oldvalue is None else oldvalue.strip()
      self._keymap[key] = oldkey

  def unescape(self, value):
    newvalue = value
    if value is not None:
      newvalue = value.replace(r"\:", ":")
      newvalue = newvalue.replace(r"\=", "=")
    return newvalue

  def removeOldProp(self, key):
    if key in self._origprops:
      del self._origprops[key]
    pass

  def removeProp(self, key):
    if key in self._props:
      del self._props[key]
    pass

  def load(self, stream):
    if stream.mode != "r":
      raise ValueError("Stream should be opened in read-only mode!")
    try:
      self.fileName = os.path.abspath(stream.name)
      for key, value in javaproperties.load(stream, object_pairs_hook=list):
        self._store_pair(key, value, key, value)
    except IOError:
      raise

  def get_property(self, key):
    return self._props.get(key, "")

  def propertyNames(self):
    return self._props.keys()

  def getPropertyDict(self):
    return self._props

  def __getitem__(self, name):
    return self.get_property(name)

  def __getattr__(self, name):
    try:
      return self.__dict__[name]
    except KeyError:
      if hasattr(self._props, name):
        return getattr(self._props, name)
      else:
        raise NotImplementedError(f"The method '{name}' is not implemented.")

  def __contains__(self, key):
    return key in self._props

  def sort_props(self):
    tmp_props = {}
    for key in sorted(self._props.keys()):
      tmp_props[key] = self._props[key]
    self._props = tmp_props
    pass

  def sort_origprops(self):
    tmp_props = self._origprops.copy()
    self._origprops.clear()
    for key in sorted(tmp_props.keys()):
      self._origprops[key] = tmp_props[key]
    pass

  def store(self, out, header=""):
    """Write the properties list to the stream 'out' along
    with the optional 'header'
    This function will attempt to close the file handler once it's done.
    """
    if out.mode[0] != "w":
      raise ValueError("Steam should be opened in write mode!")
    try:
      out.write("".join(("#", ASF_LICENSE_HEADER, "\n")))
      out.write("".join(("#", header, "\n")))
      # Write timestamp
      tstamp = time.strftime("%a %b %d %H:%M:%S %Z %Y", time.localtime())
      out.write("".join(("#", tstamp, "\n")))
      javaproperties.dump(
        ((prop, val) for prop, val in self._origprops.items() if val is not None),
        out,
        timestamp=False,
        ensure_ascii=False,
      )
    except IOError:
      raise
    finally:
      if out:
        out.close()

  def store_ordered(self, out, header=""):
    """Write the properties list to the stream 'out' along
    with the optional 'header'"""
    if out.mode[0] != "w":
      raise ValueError("Steam should be opened in write mode!")
    try:
      out.write("".join(("#", ASF_LICENSE_HEADER, "\n")))
      out.write("".join(("#", header, "\n")))
      # Write timestamp
      tstamp = time.strftime("%a %b %d %H:%M:%S %Z %Y", time.localtime())
      out.write("".join(("#", tstamp, "\n")))
      javaproperties.dump(
        ((prop, val) for prop, val in self._origprops.items() if val is not None),
        out,
        timestamp=False,
        sort_keys=True,
        ensure_ascii=False,
      )
    except IOError:
      raise
    finally:
      if out:
        out.close()
