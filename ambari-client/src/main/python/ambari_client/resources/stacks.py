#
#  Licensed to the Apache Software Foundation (ASF) under one
#  or more contributor license agreements.  See the NOTICE file
#  distributed with this work for additional information
#  regarding copyright ownership.  The ASF licenses this file
#  to you under the Apache License, Version 2.0 (the
#  "License"); you may not use this file except in compliance
#  with the License.  You may obtain a copy of the License at
# 
#      http://www.apache.org/licenses/LICENSE-2.0
# 
#  Unless required by applicable law or agreed to in writing, software
#  distributed under the License is distributed on an "AS IS" BASIS,
#  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#  See the License for the specific language governing permissions and
#  limitations under the License.

from ambari_client.model import   stack , component

__docformat__ = "epytext"


def _get_config(root_resource, version, service_name):
  """
  Get service configurations from stack
  @param version: The HDP version.
  @param service_name: service name
  @return: A ConfigModel object
  """
  return stack._get_configuration_from_stack(root_resource, version, service_name)


def _get_components(root_resource, version, service_name):
  """
  Get service components from stack
  @param version: The HDP version.
  @param service_name: service name
  @return: A ComponentModel object
  """
  return stack._get_components_from_stack(root_resource, version, service_name)

