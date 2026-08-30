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

import importlib.util
import sys
import types


PY_SOURCE = 1


def load_source(name, path):
  """Load a Python source file using the behavior expected from imp.load_source."""
  spec = importlib.util.spec_from_file_location(name, path)
  if spec is None or spec.loader is None:
    raise ImportError(f"Cannot load module {name} from {path}")

  previous_module = sys.modules.get(name)
  previous_namespace = None
  if previous_module is None:
    module = importlib.util.module_from_spec(spec)
  else:
    module = previous_module
    previous_namespace = module.__dict__.copy()
    module.__file__ = path
    module.__loader__ = spec.loader
    module.__package__ = spec.parent
    module.__spec__ = spec

  sys.modules[name] = module
  try:
    spec.loader.exec_module(module)
  except Exception:
    if previous_module is None:
      sys.modules.pop(name, None)
    else:
      module.__dict__.clear()
      module.__dict__.update(previous_namespace)
    raise
  return module


def load_module(name, file, path, description):
  """Compatibility wrapper for the source-module form of imp.load_module."""
  del file, description
  return load_source(name, path)


def new_module(name):
  return types.ModuleType(name)


def get_magic():
  return importlib.util.MAGIC_NUMBER
