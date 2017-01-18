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

"""
The naming convention for ServiceAdvisor subclasses depends on whether they are
in common-services or are part of the stack version's services.

In common-services, the naming convention is <service_name><service_version>ServiceAdvisor.
In the stack, the naming convention is <stack_name><stack_version><service_name>ServiceAdvisor.

Unlike the StackAdvisor, the ServiceAdvisor does NOT provide any inheritance.
If you want to use inheritance to augment a previous version of a service's
advisor you can use the following code to dynamically load the previous advisor.
Some changes will be need to provide the correct path and class names.

  SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
  PARENT_DIR = os.path.join(SCRIPT_DIR, '../<old_version>')
  PARENT_FILE = os.path.join(PARENT_DIR, 'service_advisor.py')

  try:
    with open(PARENT_FILE, 'rb') as fp:
      service_advisor = imp.load_module('service_advisor', fp, PARENT_FILE, ('.py', 'rb', imp.PY_SOURCE))
  except Exception as e:
    traceback.print_exc()
    print "Failed to load parent"

  class <NewServiceAdvisorClassName>(service_advisor.<OldServiceAdvisorClassName>)

where the NewServiceAdvisorClassName and OldServiceAdvisorClassName follow the naming
convention listed above.

For examples see: common-services/HAWQ/2.0.0/service_advisor.py
and common-services/PXF/3.0.0/service_advisor.py
"""
from stack_advisor import DefaultStackAdvisor

class ServiceAdvisor(DefaultStackAdvisor):
  """
  Abstract class implemented by all service advisors.
  """


  def colocateService(self, hostsComponentsMap, serviceComponents):
    """
    Populate hostsComponentsMap with key = hostname and value = [{"name": "COMP_NAME_1"}, {"name": "COMP_NAME_2"}, ...]
    of services that must be co-hosted and on which host they should be present.
    :param hostsComponentsMap: Map from hostname to list of [{"name": "COMP_NAME_1"}, {"name": "COMP_NAME_2"}, ...]
    present on on that host.
    :param serviceComponents: Mapping of components

    If any components of the service should be colocated with other services,
    this is where you should set up that layout.  Example:

      # colocate HAWQSEGMENT with DATANODE, if no hosts have been allocated for HAWQSEGMENT
      hawqSegment = [component for component in serviceComponents if component["StackServiceComponents"]["component_name"] == "HAWQSEGMENT"][0]
      if not self.isComponentHostsPopulated(hawqSegment):
        for hostName in hostsComponentsMap.keys():
          hostComponents = hostsComponentsMap[hostName]
          if {"name": "DATANODE"} in hostComponents and {"name": "HAWQSEGMENT"} not in hostComponents:
            hostsComponentsMap[hostName].append( { "name": "HAWQSEGMENT" } )
          if {"name": "DATANODE"} not in hostComponents and {"name": "HAWQSEGMENT"} in hostComponents:
            hostComponents.remove({"name": "HAWQSEGMENT"})
    """
    pass

  def getServiceConfigurationRecommendations(self, configurations, clusterSummary, services, hosts):
    """
    Any configuration recommendations for the service should be defined in this function.
    This should be similar to any of the recommendXXXXConfigurations functions in the stack_advisor.py
    such as recommendYARNConfigurations().
    """
    pass

  def getServiceComponentLayoutValidations(self, services, hosts):
    """
    Returns an array of Validation objects about issues with the hostnames to which components are assigned.
    This should detect validation issues which are different than those the stack_advisor.py detects.
    The default validations are in stack_advisor.py getComponentLayoutValidations function.
    """
    return []

  def getServiceConfigurationsValidationItems(self, configurations, recommendedDefaults, services, hosts):
    """
    Any configuration validations for the service should be defined in this function.
    This should be similar to any of the validateXXXXConfigurations functions in the stack_advisor.py
    such as validateHDFSConfigurations.
    """
    return []
