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

from resource_management.core.resources.system import Execute
from resource_management.libraries.functions.format import format

def supervisord_service(component_name, action):
  Execute(format("supervisorctl {action} storm-{component_name}"),
    wait_for_finish=False
  )

def supervisord_check_status(component_name):
  try:
    Execute(format("supervisorctl status storm-{component_name} | grep RUNNING"))
  except Fail:
    raise ComponentIsNotRunning() 
