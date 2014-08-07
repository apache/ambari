#!/usr/bin/env ambari-python-wrap
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

class StackAdvisor(object):

  def recommendComponentLayout(self, services, hosts):
    """Returns Services object with hostnames array populated for components"""
    pass

  def validateComponentLayout(self, services, hosts):
    """Returns array of Validation objects about issues with hostnames components assigned to"""
    pass

  def recommendConfigurations(self, services, hosts):
    """Returns Services object with configurations object populated"""
    pass

  def validateConfigurations(self, services, hosts):
    """Returns array of Validation objects about issues with configuration values provided in services"""
    pass

