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

__all__ = ["format_jvm_option", "format_jvm_option_value"]
from resource_management.libraries.script import Script
from resource_management.libraries.script.config_dictionary import UnknownConfiguration
from resource_management.core.logger import Logger
from resource_management.libraries.functions import *

def format_jvm_option(name, default_value):
  curr_dict = default(name, default_value)
  return format_jvm_option_value(curr_dict, default_value)


def format_jvm_option_value(value, default_value):
  if isinstance(value, (int, long)):
    curr_dict = str(value) + "m"
    return curr_dict
  elif isinstance(value, str):
    if value.strip() == "":
      return default_value
    elif value.strip() != "":
      if "m" in value:
        return value
      else:
        if isinstance(int(value), (int, long)):
          return str(int(value)) + "m"
        else:
          return default_value
  else:
    return default_value
