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

import re
import math

def calc_heap_memory(memorysize, heapmemory_factor):
  """
  @param memorysize_str: str (e.g '4096m')
  @param heapmemory_factor: float (e.g 0.8)
  """
  return int(math.floor(memorysize*heapmemory_factor))

def ensure_unit_for_memory(memory_size):
  memory_size_values = re.findall('\d+', str(memory_size))
  memory_size_unit = re.findall('\D+', str(memory_size))

  if len(memory_size_values) > 0:
    unit = 'm'
    if len(memory_size_unit) > 0:
      unit = memory_size_unit[0]
    if unit not in ['b', 'k', 'm', 'g', 't', 'p']:
      raise Exception("Memory size unit error. %s - wrong unit" % unit)
    return "%s%s" % (memory_size_values[0], unit)
  else:
    raise Exception('Memory size can not be calculated')
