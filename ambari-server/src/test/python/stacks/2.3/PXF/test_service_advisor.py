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

import os
from unittest import TestCase


class TestPXF300ServiceAdvisor(TestCase):

  def setUp(self):
    import imp
    self.testDirectory = os.path.dirname(os.path.abspath(__file__))
    stackAdvisorPath = os.path.join(self.testDirectory, '../../../../../main/resources/stacks/stack_advisor.py')
    pxf300ServiceAdvisorPath = os.path.join(self.testDirectory, '../../../../../main/resources/common-services/PXF/3.0.0/service_advisor.py')
    with open(stackAdvisorPath, 'rb') as fp:
      imp.load_module('stack_advisor', fp, stackAdvisorPath, ('.py', 'rb', imp.PY_SOURCE))
    with open(pxf300ServiceAdvisorPath, 'rb') as fp:
      service_advisor_impl = imp.load_module('service_advisor_impl', fp, pxf300ServiceAdvisorPath, ('.py', 'rb', imp.PY_SOURCE))
    serviceAdvisorClass = getattr(service_advisor_impl, 'PXF300ServiceAdvisor')
    self.serviceAdvisor = serviceAdvisorClass()

    self.PXF_PATH = "export HBASE_CLASSPATH=${HBASE_CLASSPATH}:/usr/lib/pxf/pxf-hbase.jar"

  def test_getServiceConfigurationRecommendations(self):
    services = {
      "configurations": {
        "hbase-env": {
          "properties": {
            "content": "# Some hbase-env content text"
          }
        }
      }
    }

    ## Test is PXF_PATH is being added to hbase-env content

    # Case 1: Test pxf-hbase.jar classpath line was added to content
    expected = "# Some hbase-env content text\n\n#Add pxf-hbase.jar to HBASE_CLASSPATH\n" + self.PXF_PATH
    self.serviceAdvisor.getServiceConfigurationRecommendations(services["configurations"], None, services, None)
    self.assertEquals(services["configurations"]["hbase-env"]["properties"]["content"], expected)

    # Case 2: Test pxf-hbase.jar classpath line is not added again if content already has it
    services["configurations"]["hbase-env"]["properties"]["content"] = self.PXF_PATH
    expected = self.PXF_PATH
    self.serviceAdvisor.getServiceConfigurationRecommendations(services["configurations"], None, services, None)
    self.assertEquals(services["configurations"]["hbase-env"]["properties"]["content"], expected)

  def test_getConfigurationsValidationItems(self):
    services = {
      "services": [
        {
          "StackServices": {
            "service_name": "PXF",
            "service_version": "2.0",
            "stack_name": "HDP",
            "stack_version": "2.3"
          }
        },
        {
          "StackServices": {
            "service_name": "HBASE",
            "service_version": "2.0",
            "stack_name": "HDP",
            "stack_version": "2.3"
          }
        }
      ],
      "configurations": {
        "hbase-env": {
          "properties": {
            "content": "# Some hbase-env content text"
          }
        }
      }
    }
    properties = services["configurations"]

    ## Test if PXF_PATH present in hbase-env content

    # Case 1: Generate warning item if PXF_PATH is not present in hbase-env
    expected = [
      {
        "config-type": "hbase-env",
        "message": "HBASE_CLASSPATH must contain the location of pxf-hbase.jar",
        "type": "configuration",
        "config-name": "content",
        "level": "WARN"
      }
    ]
    items = self.serviceAdvisor.getConfigurationsValidationItems(properties, properties, services, None)
    self.assertEquals(items, expected)

    # Case 2: No warning should be generated if PXF_PATH is present in hbase-env
    properties = services["configurations"]["hbase-env"]["properties"]["content"] = self.PXF_PATH
    items = self.serviceAdvisor.getConfigurationsValidationItems(properties, properties, services, None)
    self.assertEquals(items, [])