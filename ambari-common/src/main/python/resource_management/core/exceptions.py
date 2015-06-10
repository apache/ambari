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

__all__ = ["Fail", "ExecuteTimeoutException", "InvalidArgument", "ClientComponentHasNoStatus", "ComponentIsNotRunning"]

class Fail(Exception):
  pass

class ExecuteTimeoutException(Fail):
  pass

class InvalidArgument(Fail):
  pass

class ClientComponentHasNoStatus(Fail):
  """
  Thrown when status() method is called for a CLIENT component.
  The only valid status for CLIENT component is installed,
  that's why exception is thrown and later silently processed at script.py
  """
  pass

class ComponentIsNotRunning(Fail):
  """
  Thrown when status() method is called for a component (only
  in situations when component process is not running).
  Later exception is silently processed at script.py
  """
  pass
