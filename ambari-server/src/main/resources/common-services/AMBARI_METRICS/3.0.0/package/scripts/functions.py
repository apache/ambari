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

import re
import math


def calc_xmn_from_xms(heapsize_str, xmn_percent, xmn_max):
  """
  @param heapsize: str (e.g 1000m)
  @param xmn_percent: float (e.g 0.2)
  @param xmn_max: integer (e.g 512)
  """
  match = re.fullmatch(r"([1-9][0-9]*)([mMgG])", str(heapsize_str).strip())
  if match is None:
    raise ValueError("Heap size must be a positive integer followed by m or g")
  heapsize = int(match.group(1))
  heapsize_unit = match.group(2).lower()

  xmn_val = int(math.floor(heapsize * xmn_percent))
  xmn_val -= xmn_val % 8

  result_xmn_val = xmn_max if xmn_val > xmn_max else xmn_val
  return str(result_xmn_val) + heapsize_unit


def trim_heap_property(property_value, m_suffix="m"):
  value = str(property_value).strip()
  suffix = re.escape(m_suffix)
  match = re.fullmatch(rf"([1-9][0-9]*)(?:{suffix})?", value, re.IGNORECASE)
  if match is None:
    raise ValueError("Heap value must be a positive integer with an optional suffix")
  return match.group(1)


def check_append_heap_property(property_value, m_suffix="m"):
  return trim_heap_property(property_value, m_suffix) + m_suffix
