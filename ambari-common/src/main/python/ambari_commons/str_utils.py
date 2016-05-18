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


def compress_backslashes(s):
  s1 = s
  while (-1 != s1.find('\\\\')):
    s1 = s1.replace('\\\\', '\\')
  return s1


def ensure_double_backslashes(s):
  s1 = compress_backslashes(s)
  s2 = s1.replace('\\', '\\\\')
  return s2


def cbool(obj):
  """
  Interprets an object as a boolean value.

  :rtype: bool
  """
  if isinstance(obj, str):
    obj = obj.strip().lower()
    if obj in ('true', 'yes', 'on', 'y', 't', '1'):
      return True
    if obj in ('false', 'no', 'off', 'n', 'f', '0'):
      return False
    raise ValueError('Unable to interpret value "%s" as boolean' % obj)
  return bool(obj)


def cint(obj):
  """
  Interprets an object as a integer value.
  :param obj:
  :return:
  """
  if isinstance(obj, str):
    obj = obj.strip().lower()
    try:
      return int(obj)
    except ValueError:
      raise ValueError('Unable to interpret value "%s" as integer' % obj)
  elif obj is None:
    return obj

  return int(obj)

