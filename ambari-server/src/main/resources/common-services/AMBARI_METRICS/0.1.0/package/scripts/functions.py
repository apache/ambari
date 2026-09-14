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

def calc_xmn_from_xms(heapsize_str, xmn_percent, xmn_max):
  """
  @param heapsize: str (e.g 1000m)
  @param xmn_percent: float (e.g 0.2)
  @param xmn_max: integer (e.g 512)
  """
  heapsize = int(re.search('\d+', str(heapsize_str)).group(0))
  heapsize_unit = re.search('\D+', str(heapsize_str)).group(0)

  xmn_val = int(math.floor(heapsize*xmn_percent))
  xmn_val -= xmn_val % 8
  
  result_xmn_val = xmn_max if xmn_val > xmn_max else xmn_val
  return str(result_xmn_val) + heapsize_unit

def trim_heap_property(property, m_suffix = "m"):
  if property and property.endswith(m_suffix):
    property = property[:-1]
  return property

def check_append_heap_property(property, m_suffix = "m"):
  if property and not property.endswith(m_suffix):
    property += m_suffix
  return property