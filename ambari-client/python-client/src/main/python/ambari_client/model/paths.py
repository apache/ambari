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

CLUSTERS_PATH = "/clusters"
CLUSTERS_CONFIG_PATH = "/clusters/%s"
CLUSTER_HOSTS_PATH = "/clusters/%s/hosts"
CLUSTER_HOST_PATH = "/clusters/%s/hosts/%s"
CLUSTER_START_ALL_SERVICES = "/clusters/%s/services?ServiceInfo/state=INSTALLED"
CLUSTER_STOP_ALL_SERVICES = "/clusters/%s/services?ServiceInfo"
CLUSTER_REQUESTS_PATH = "/clusters/%s/requests"

SERVICES_PATH = "/clusters/%s/services"
SERVICE_PATH = "/clusters/%s/services/%s"
SERVICE_CREATE_PATH = "/clusters/%s/services/?ServiceInfo/service_name=%s"
SERVICE_COMPONENTS_PATH = "/clusters/%s/services/%s/components?fields=*"
SERVICE_COMPONENT_PATH = "/clusters/%s/services/%s/components/%s"


HOST_PATH = "/hosts/%s"
HOSTS_PATH = "/hosts"
HOSTS_CREATE_PATH = "/clusters/%s/hosts"
HOSTS_COMPONENTS_PATH = "/clusters/%s/hosts/%s/host_components?fields=HostRoles/state"
HOSTS_COMPONENT_PATH = "/clusters/%s/hosts/%s/host_components/%s"
HOSTS_ASSIGN_ROLE = "/clusters/%s/hosts?Hosts/host_name=%s"

BOOTSTRAP_PATH = "/bootstrap"
REQUEST_STATUS_PATH = "/clusters/%s/requests/%s?fields=tasks/Tasks/status"
REQUEST_PATH = "clusters/%s/requests/%s"
REQUEST_N_PATH = "clusters/%s/requests?to=end&page_size=%s&fields=Requests"

CONFIGURATION_PATH = "/clusters/%s/configurations?type=%s&tag=%s"
CONFIGURATION_ALL_PATH = "/clusters/%s/configurations?type=%s"
CREATE_CONFIGURATION_PATH = "/clusters/%s/configurations"
UPDATE_CONFIGURATION_PATH = "/clusters/%s"

STACKS_PATH = "/stacks2"

STACKS_VERSIONS_PATH = "stacks2/HDP/versions"
STACKS_OS_REPO_PATH = "stacks2/HDP/versions/%s/operatingSystems/%s/repositories/HDP-%s"
STACK_SERVICES_COMPONENTS_PATH = "/stacks2/HDP/versions/%s/stackServices/%s/serviceComponents?fields=*"
STACK_SERVICES_CONFIG_PATH = "/stacks2/HDP/versions/%s/stackServices/%s/configurations?fields=*"

TASKS_PATH = "clusters/%s/requests/%s/tasks?fields=*"

BLUEPRINT_ALL_PATH = "blueprints?fields=*"
BLUEPRINT_PATH = "blueprints/%s"
BLUEPRINT_CLUSTER_PATH = "clusters/%s?format=blueprint"
