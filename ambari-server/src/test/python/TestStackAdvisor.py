'''
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
'''

from unittest import TestCase
import os

class TestStackAdvisorInitialization(TestCase):

  def setUp(self):
    import imp

    self.test_directory = os.path.dirname(os.path.abspath(__file__))
    stack_advisor_path = os.path.join(self.test_directory, '../../main/resources/scripts/stack_advisor.py')
    with open(stack_advisor_path, 'rb') as fp:
        self.stack_advisor = imp.load_module( 'stack_advisor', fp, stack_advisor_path, ('.py', 'rb', imp.PY_SOURCE) )

  def test_stackAdvisorLoadedForNotHDPStack(self):
    path_template = os.path.join(self.test_directory, '../resources/stacks/{0}/{1}/services/stack_advisor.py')
    path_template_name = "STACK_ADVISOR_IMPL_PATH_TEMPLATE"
    setattr(self.stack_advisor, path_template_name, path_template)
    self.assertEquals(path_template, getattr(self.stack_advisor, path_template_name))
    instantiate_stack_advisor_method_name = 'instantiateStackAdvisor'
    instantiate_stack_advisor_method = getattr(self.stack_advisor, instantiate_stack_advisor_method_name)
    stack_advisor = instantiate_stack_advisor_method("XYZ", "1.0.1", ["1.0.0"])
    self.assertEquals("XYZ101StackAdvisor", stack_advisor.__class__.__name__)
    services = {"Versions":
                  {
                    "stack_name":"XYZ",
                    "stack_version":"1.0.1"
                  },
                "services":[
                  {
                    "StackServices":{
                      "service_name":"YARN"
                    },
                    "components":[
                      {
                        "StackServiceComponents": {
                          "component_name": "RESOURCEMANAGER"
                        }
                      },
                      {
                        "StackServiceComponents": {
                          "component_name": "APP_TIMELINE_SERVER"
                        }
                      },
                      {
                        "StackServiceComponents": {
                          "component_name":"YARN_CLIENT"
                        }
                      },
                      {
                        "StackServiceComponents": {
                          "component_name": "NODEMANAGER"
                        }
                      }
                    ]
                  }
                ]
    }
    hosts= {
      "items": [
        {"Hosts": {"host_name": "host1"}},
        {"Hosts": {"host_name": "host2"}}
      ]
    }
    config_recommendations = stack_advisor.recommendConfigurations(services, hosts)
    yarn_configs = config_recommendations["recommendations"]["blueprint"]["configurations"]["yarn-site"]["properties"]
    '''Check that value is populated from child class, not parent'''
    self.assertEquals("-Xmx101m", yarn_configs["yarn.nodemanager.resource.memory-mb"])

  def test_stackAdvisorSuperClassIsFoundAndReturnedAsDefaultImpl(self):
    instantiate_stack_advisor_method_name = 'instantiateStackAdvisor'
    instantiate_stack_advisor_method = getattr(self.stack_advisor, instantiate_stack_advisor_method_name)
    '''Not existent stack - to return default implementation'''
    default_stack_advisor = instantiate_stack_advisor_method("HDP1", "2.0.6", [])
    self.assertEquals("StackAdvisor", default_stack_advisor.__class__.__name__)

