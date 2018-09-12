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

import imp
import json
import os
from unittest import TestCase

class TestServiceAdvisor(TestCase):

  test_directory = os.path.dirname(os.path.abspath(__file__))

  def setUp(self):
    os.environ["advisor"] = "mpack"
    from service_advisor import ServiceAdvisor
    self.serviceAdvisor = ServiceAdvisor()

  def load_json(self, filename):
    file = os.path.join(self.test_directory, filename)
    with open(file, 'rb') as f:
      data = json.load(f)
    return data

  def test_getServiceComponentCardinalityValidations(self):
    """ Test getServiceComponentCardinalityValidations """
    services = self.load_json("hdfs.json")
    hosts = self.load_json("validation-hosts.json")

    validations = self.serviceAdvisor.getServiceComponentCardinalityValidations(services, hosts, "HDFS")
    self.assertEquals(len(validations), 1)
    expected = {
      "type": 'host-component',
      "level": 'ERROR',
      "component-name": 'DATANODE',
      "message": 'You have selected 0 DataNode components. Please consider that at least 1 DataNode components should be installed in cluster.'
    }
    self.assertEquals(validations[0], expected)

    validations = self.serviceAdvisor.getServiceComponentCardinalityValidations(services, hosts, "HBASE")
    self.assertEquals(len(validations), 0)

    services["services"][0]["components"][0]["StackServiceComponents"]["hostnames"].append("c7402.ambari.apache.org")

    validations = self.serviceAdvisor.getServiceComponentCardinalityValidations(services, hosts, "HDFS")
    self.assertEquals(len(validations), 0)
