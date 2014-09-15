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

_all__ = ["ExecuteHadoop"]
from resource_management.core.base import Resource, ForcedListArgument, ResourceArgument, BooleanArgument

class ExecuteHadoop(Resource):
  action = ForcedListArgument(default="run")
  command = ResourceArgument(default=lambda obj: obj.name)
  kinit_override = BooleanArgument(default=False)
  tries = ResourceArgument(default=1)
  try_sleep = ResourceArgument(default=0) # seconds
  user = ResourceArgument()
  logoutput = BooleanArgument(default=False)
  principal = ResourceArgument(default=lambda obj: obj.user)
  bin_dir = ResourceArgument() # appended to $PATH
  
  conf_dir = ResourceArgument()
  
  security_enabled = BooleanArgument(default=False)
  keytab = ResourceArgument()
  kinit_path_local = ResourceArgument()
  
  actions = Resource.actions + ["run"]
  
