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

import json
import os

from resource_management.core.exceptions import Fail
from resource_management.libraries.functions.default import default


def resolve_java_home(service_name, component_name=None, default_home=None):
  """Resolve a validated per-service Java home override.

  ``cluster-env/java_home_overrides`` is intentionally explicit: the default
  Ambari Java home remains unchanged unless an operator supplies a mapping for
  a known service or component.  This lets old BIGTOP services such as Hive
  3.1 run with the pre-installed Java 8 runtime while Hadoop runs on Java 17.
  """
  raw_overrides = default("/configurations/cluster-env/java_home_overrides", "{}")
  if raw_overrides in (None, ""):
    return default_home
  if isinstance(raw_overrides, str):
    try:
      overrides = json.loads(raw_overrides)
    except (TypeError, ValueError) as error:
      raise Fail("cluster-env/java_home_overrides must be valid JSON") from error
  else:
    overrides = raw_overrides
  if not isinstance(overrides, dict):
    raise Fail("cluster-env/java_home_overrides must be a JSON object")

  selected = None
  for key in (component_name, service_name):
    if key and key in overrides:
      selected = overrides[key]
      break
  if selected is None:
    return default_home
  if not isinstance(selected, dict):
    raise Fail(f"Java override for {key} must be an object")
  home = selected.get("home")
  if (
    not isinstance(home, str)
    or not home
    or home != home.strip()
    or not os.path.isabs(home)
    or os.path.normpath(home) != home
    or home == os.sep
    or not os.path.isfile(os.path.join(home, "bin", "java"))
  ):
    raise Fail(f"Java override for {key} must point to an installed Java home")
  version = selected.get("version")
  if version is not None and (
    isinstance(version, bool) or not isinstance(version, int) or version < 1
  ):
    raise Fail(f"Java override version for {key} must be a positive integer")
  return home
