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

import imp
import os
from unittest import TestCase

class TestSPARKServiceAdvisor(TestCase):
  testDirectory = os.path.dirname(os.path.abspath(__file__))
  stack_advisor_path = os.path.join(testDirectory, '../../../../../main/resources/stacks/stack_advisor.py')
  with open(stack_advisor_path, 'rb') as fp:
    imp.load_module('stack_advisor', fp, stack_advisor_path, ('.py', 'rb', imp.PY_SOURCE))

  serviceAdvisorPath = '../../../../../main/resources/common-services/SPARK/2.2.0/service_advisor.py'
  sparkServiceAdvisorPath = os.path.join(testDirectory, serviceAdvisorPath)
  with open(sparkServiceAdvisorPath, 'rb') as fp:
    service_advisor_impl = imp.load_module('service_advisor_impl', fp, sparkServiceAdvisorPath, ('.py', 'rb', imp.PY_SOURCE))

  def setUp(self):
    serviceAdvisorClass = getattr(self.service_advisor_impl, 'SparkServiceAdvisor')
    self.serviceAdvisor = serviceAdvisorClass()

  def test_recommendSPARKConfigurations_SecurityEnabledZeppelinInstalled(self):
    configurations = {
      "cluster-env": {
        "properties": {
          "security_enabled": "true",
        }
      },
      "livy-conf": {
        "properties": {
          "livy.property1": "value1"
        }
      },
      "zeppelin-env": {
        "properties": {
          "zeppelin.server.kerberos.principal": "zeppelin_user@REALM"
        }
      }
    }
    services = {"configurations": configurations}
    services['services'] = [
      {
        "StackServices": {
          "service_name": "SPARK"
        },
      }
    ]
    expected = {
      "cluster-env": {
        "properties": {
          "security_enabled": "true",
        }
      },
      "livy-conf": {
        "properties": {
          "livy.superusers": "zeppelin_user",
          "livy.property1": "value1"
        }
      },
      "spark-defaults": {
        "properties": {
          "spark.yarn.queue": "default"
        }
      },
      "spark-thrift-sparkconf": {
        "properties": {
          "spark.yarn.queue": "default"
        }
      },
      "zeppelin-env": {
        "properties": {
          "zeppelin.server.kerberos.principal": "zeppelin_user@REALM"
        }
      }
    }

    self.serviceAdvisor.getServiceConfigurationRecommendations(configurations, None, services, None)
    self.assertEquals(configurations, expected)

  def test_recommendSPARKConfigurations_SecurityNotEnabledZeppelinInstalled(self):
    configurations = {
      "cluster-env": {
        "properties": {
          "security_enabled": "false",
        }
      },
      "livy-conf": {
        "properties": {
        }
      },
      "zeppelin-env": {
        "properties": {
        }
      }
    }
    services = {"configurations": configurations}
    services['services'] = [
      {
        "StackServices": {
          "service_name": "SPARK"
        },
      }
    ]
    expected = {
      "cluster-env": {
        "properties": {
          "security_enabled": "false",
        }
      },
      "livy-conf": {
        "properties": {
        }
      },
      "spark-defaults": {
        "properties": {
          "spark.yarn.queue": "default"
        }
      },
      "spark-thrift-sparkconf": {
        "properties": {
          "spark.yarn.queue": "default"
        }
      },
      "zeppelin-env": {
        "properties": {
        }
      }
    }

    self.serviceAdvisor.getServiceConfigurationRecommendations(configurations, None, services, None)
    self.assertEquals(configurations, expected)

  def test_recommendSPARKConfigurations_SecurityEnabledZeppelinInstalledExistingValue(self):
    configurations = {
      "cluster-env": {
        "properties": {
          "security_enabled": "true",
        }
      },
      "livy-conf": {
        "properties": {
          "livy.superusers": "livy_user"
        }
      },
      "spark-defaults": {
        "properties": {
          "spark.yarn.queue": "default"
        }
      },
      "spark-thrift-sparkconf": {
        "properties": {
          "spark.yarn.queue": "default"
        }
      },
      "zeppelin-env": {
        "properties": {
          "zeppelin.server.kerberos.principal": "zeppelin_user@REALM"
        }
      }
    }
    services = {"configurations": configurations}
    services['services'] = [
      {
        "StackServices": {
          "service_name": "SPARK"
        },
      }
    ]
    expected = {
      "cluster-env": {
        "properties": {
          "security_enabled": "true",
        }
      },
      "livy-conf": {
        "properties": {
          "livy.superusers": "livy_user,zeppelin_user"
        }
      },
      "spark-defaults": {
        "properties": {
          "spark.yarn.queue": "default"
        }
      },
      "spark-thrift-sparkconf": {
        "properties": {
          "spark.yarn.queue": "default"
        }
      },
      "zeppelin-env": {
        "properties": {
          "zeppelin.server.kerberos.principal": "zeppelin_user@REALM"
        }
      }
    }

    self.serviceAdvisor.getServiceConfigurationRecommendations(configurations, None, services, None)
    self.assertEquals(configurations, expected)

  def test_recommendSPARKConfigurations_SecurityEnabledZeppelinNotInstalled(self):
    configurations = {
      "cluster-env": {
        "properties": {
          "security_enabled": "true",
        }
      },
      "livy-conf": {
        "properties": {
          "livy.property1" : "value1"
        }
      }
    }

    services = {"configurations": configurations}
    services['services'] = [
      {
        "StackServices": {
          "service_name": "SPARK"
        },
      }
    ]
    expected = {
      "cluster-env": {
        "properties": {
          "security_enabled": "true",
        }
      },
      "livy-conf": {
        "properties": {
          "livy.property1" : "value1"
        }
      },
      "spark-defaults": {
        "properties": {
          "spark.yarn.queue": "default"
        }
      },
      "spark-thrift-sparkconf": {
        "properties": {
          "spark.yarn.queue": "default"
        }
      }
    }

    self.serviceAdvisor.getServiceConfigurationRecommendations(configurations, None, services, None)
    self.assertEquals(configurations, expected)

  def test_recommendSPARK2Configurations(self):
    configurations = {}
    services = {"configurations": configurations}
    services['services'] = [
      {
        "StackServices": {
          "service_name": "SPARK2"
        },
      }
    ]
    expected = {
      "spark-defaults": {
        "properties": {
          "spark.yarn.queue": "default"
        }
      },
      "spark-thrift-sparkconf": {
        "properties": {
          "spark.yarn.queue": "default"
        }
      }
    }

    self.serviceAdvisor.getServiceConfigurationRecommendations(configurations, None, services, None)
    self.assertEquals(configurations, expected)

