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

import json
import os
import socket
from unittest import TestCase
from mock.mock import patch, MagicMock


class TestHDP25StackAdvisor(TestCase):

  def setUp(self):
    import imp
    self.maxDiff = None
    self.testDirectory = os.path.dirname(os.path.abspath(__file__))
    stackAdvisorPath = os.path.join(self.testDirectory, '../../../../../main/resources/stacks/stack_advisor.py')
    hdp206StackAdvisorPath = os.path.join(self.testDirectory, '../../../../../main/resources/stacks/HDP/2.0.6/services/stack_advisor.py')
    hdp21StackAdvisorPath = os.path.join(self.testDirectory, '../../../../../main/resources/stacks/HDP/2.1/services/stack_advisor.py')
    hdp22StackAdvisorPath = os.path.join(self.testDirectory, '../../../../../main/resources/stacks/HDP/2.2/services/stack_advisor.py')
    hdp23StackAdvisorPath = os.path.join(self.testDirectory, '../../../../../main/resources/stacks/HDP/2.3/services/stack_advisor.py')
    hdp24StackAdvisorPath = os.path.join(self.testDirectory, '../../../../../main/resources/stacks/HDP/2.4/services/stack_advisor.py')
    hdp25StackAdvisorPath = os.path.join(self.testDirectory, '../../../../../main/resources/stacks/HDP/2.5/services/stack_advisor.py')
    hdp25StackAdvisorClassName = 'HDP25StackAdvisor'

    with open(stackAdvisorPath, 'rb') as fp:
      imp.load_module('stack_advisor', fp, stackAdvisorPath, ('.py', 'rb', imp.PY_SOURCE))
    with open(hdp206StackAdvisorPath, 'rb') as fp:
      imp.load_module('stack_advisor_impl', fp, hdp206StackAdvisorPath, ('.py', 'rb', imp.PY_SOURCE))
    with open(hdp21StackAdvisorPath, 'rb') as fp:
      imp.load_module('stack_advisor_impl', fp, hdp21StackAdvisorPath, ('.py', 'rb', imp.PY_SOURCE))
    with open(hdp22StackAdvisorPath, 'rb') as fp:
      imp.load_module('stack_advisor_impl', fp, hdp22StackAdvisorPath, ('.py', 'rb', imp.PY_SOURCE))
    with open(hdp23StackAdvisorPath, 'rb') as fp:
      stack_advisor_impl = imp.load_module('stack_advisor_impl', fp, hdp23StackAdvisorPath, ('.py', 'rb', imp.PY_SOURCE))
    with open(hdp24StackAdvisorPath, 'rb') as fp:
      stack_advisor_impl = imp.load_module('stack_advisor_impl', fp, hdp24StackAdvisorPath, ('.py', 'rb', imp.PY_SOURCE))
    with open(hdp25StackAdvisorPath, 'rb') as fp:
      stack_advisor_impl = imp.load_module('stack_advisor_impl', fp, hdp25StackAdvisorPath, ('.py', 'rb', imp.PY_SOURCE))
    clazz = getattr(stack_advisor_impl, hdp25StackAdvisorClassName)
    self.stackAdvisor = clazz()

    # substitute method in the instance
    self.get_system_min_uid_real = self.stackAdvisor.get_system_min_uid
    self.stackAdvisor.get_system_min_uid = self.get_system_min_uid_magic

  def load_json(self, filename):
    file = os.path.join(self.testDirectory, filename)
    with open(file, 'rb') as f:
      data = json.load(f)
    return data

  def prepareHosts(self, hostsNames):
    hosts = { "items": [] }
    for hostName in hostsNames:
      nextHost = {"Hosts":{"host_name" : hostName}}
      hosts["items"].append(nextHost)
    return hosts

  @patch('__builtin__.open')
  @patch('os.path.exists')
  def get_system_min_uid_magic(self, exists_mock, open_mock):
    class MagicFile(object):
      def read(self):
        return """
        #test line UID_MIN 200
        UID_MIN 500
        """

      def __exit__(self, exc_type, exc_val, exc_tb):
        pass

      def __enter__(self):
        return self

    exists_mock.return_value = True
    open_mock.return_value = MagicFile()
    return self.get_system_min_uid_real()


  def __getHosts(self, componentsList, componentName):
    return [component["StackServiceComponents"] for component in componentsList if component["StackServiceComponents"]["component_name"] == componentName][0]


  def test_getComponentLayoutValidations_one_hsi_host(self):

    hosts = self.load_json("host-3-hosts.json")
    services = self.load_json("services-normal-his-2-hosts.json")

    validations = self.stackAdvisor.getComponentLayoutValidations(services, hosts)
    expected = {'component-name': 'HIVE_SERVER_INTERACTIVE', 'message': 'Between 0 and 1 HiveServer2 Interactive components should be installed in cluster.', 'type': 'host-component', 'level': 'ERROR'}
    self.assertEquals(validations[0], expected)


  def test_validateHiveConfigurations(self):
    properties = {'enable_hive_interactive': 'true',
                  'hive_server_interactive_host': 'c6401.ambari.apache.org',
                  'hive.tez.container.size': '2048'}
    recommendedDefaults = {'enable_hive_interactive': 'true',
                           "hive_server_interactive_host": "c6401.ambari.apache.org"}
    configurations = {
      "hive-interactive-env": {
        "properties": {'enable_hive_interactive': 'true', 'hive_server_interactive_host': 'c6401.ambari.apache.org'}
      },
      "hive-site": {
        "properties": {"hive.security.authorization.enabled": "true", 'hive.tez.java.opts': '-server -Djava.net.preferIPv4Stack=true'}
      },
      "hive-env": {
        "properties": {"hive_security_authorization": "None"}
      }
    }
    configurations2 = {
      "hive-interactive-env": {
        "properties": {'enable_hive_interactive': 'false'}
      },
      "hive-site": {
        "properties": {"hive.security.authorization.enabled": "true", 'hive.tez.java.opts': '-server -Djava.net.preferIPv4Stack=true'}
      },
      "hive-env": {
        "properties": {"hive_security_authorization": "None"}
      }
    }
    configurations3 = {
      "hive-interactive-env": {
        "properties": {'enable_hive_interactive': 'true', "hive_server_interactive_host": "c6402.ambari.apache.org"}
      },
      "hive-site": {
        "properties": {"hive.security.authorization.enabled": "true", 'hive.tez.java.opts': '-server -Djava.net.preferIPv4Stack=true'}
      },
      "hive-env": {
        "properties": {"hive_security_authorization": "None"}
      }
    }
    services = self.load_json("services-normal-his-valid.json")

    res_expected = [
    ]
    # the above error is not what we are checking for - just to keep test happy without having to test
    res = self.stackAdvisor.validateHiveInteractiveEnvConfigurations(properties, recommendedDefaults, configurations, services, {})
    self.assertEquals(res, res_expected)

    res_expected = [
      {'config-type': 'hdfs-site', 'message': 'HIVE_SERVER_INTERACTIVE requires enable_hive_interactive in hive-interactive-env set to true.', 'type': 'configuration', 'config-name': 'enable_hive_interactive', 'level': 'WARN'},
      {'config-type': 'hdfs-site', 'message': 'HIVE_SERVER_INTERACTIVE requires hive_server_interactive_host in hive-interactive-env set to its host name.', 'type': 'configuration', 'config-name': 'hive_server_interactive_host', 'level': 'WARN'}
    ]
    res = self.stackAdvisor.validateHiveInteractiveEnvConfigurations(properties, recommendedDefaults, configurations2, services, {})
    self.assertEquals(res, res_expected)

    res_expected = [
      {'config-type': 'hdfs-site', 'message': 'HIVE_SERVER_INTERACTIVE requires hive_server_interactive_host in hive-interactive-env set to its host name.', 'type': 'configuration', 'config-name': 'hive_server_interactive_host', 'level': 'WARN'}
    ]
    res = self.stackAdvisor.validateHiveInteractiveEnvConfigurations(properties, recommendedDefaults, configurations3, services, {})
    self.assertEquals(res, res_expected)