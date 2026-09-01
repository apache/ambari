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

from resource_management.core.exceptions import Fail


MEMORY_PATTERN = re.compile(r"\s*([0-9]+)\s*([kmg]?)\s*", re.IGNORECASE)
MEMORY_UNIT_MIB = {
  "": 1,
  "k": 1 / 1024,
  "m": 1,
  "g": 1024,
}


def as_bool(value):
  if isinstance(value, bool):
    return value
  return str(value or "").strip().lower() in ("1", "true", "yes")


def calc_xmn_from_xms(heapsize_str, xmn_percent, xmn_max):
  """
  @param heapsize_str: str (e.g '1000m')
  @param xmn_percent: float (e.g 0.2)
  @param xmn_max: integer (e.g 512)
  """
  match = MEMORY_PATTERN.fullmatch(str(heapsize_str))
  if match is None:
    raise Fail(f"Invalid heap size: {heapsize_str}")
  heapsize = int(match.group(1))
  heapsize_unit = (match.group(2) or "m").lower()
  try:
    ratio = float(xmn_percent)
    maximum_mib = int(xmn_max)
  except (TypeError, ValueError) as error:
    raise Fail("Invalid young generation ratio or maximum") from error
  if heapsize <= 0 or not 0 < ratio <= 1 or maximum_mib <= 0:
    raise Fail("Heap size, young generation ratio, and maximum must be positive")
  maximum_mib -= maximum_mib % 8
  if maximum_mib <= 0:
    raise Fail("Young generation maximum must be at least 8 MiB")
  heapsize_mib = heapsize * MEMORY_UNIT_MIB[heapsize_unit]
  xmn_mib = int(math.floor(heapsize_mib * ratio))
  xmn_mib -= xmn_mib % 8
  xmn_mib = min(xmn_mib, maximum_mib)
  if xmn_mib <= 0:
    raise Fail("Calculated young generation size must be at least 8 MiB")

  return f"{xmn_mib}m"


def ensure_unit_for_memory(memory_size):
  match = MEMORY_PATTERN.fullmatch(str(memory_size))
  if match is None or int(match.group(1)) <= 0:
    raise Fail(f"Invalid memory size: {memory_size}")
  return f"{match.group(1)}{(match.group(2) or 'm').lower()}"
