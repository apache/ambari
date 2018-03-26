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
from resource_management.core.logger import Logger
from resource_management.libraries.execution_command import execution_command

import sys
import json

command_data_file = "command.json"

class TestExecutionCommand(TestCase):

  def setUp(self):
    Logger.initialize_logger()
    try:
      with open(command_data_file) as f:
        self.__execution_command = execution_command.ExecutionCommand(json.load(f))
    except IOError:
      Logger.error("Can not read json file with command parameters: ")
      sys.exit(1)

  def test_get_module_name(self):
    module_name = self.__execution_command.get_module_name()
    self.assertEquals(module_name, "ZOOKEEPER")

  def test_get_module_configs(self):
    module_configs = self.__execution_command.get_module_configs()
    self.assertNotEquals(module_configs, None)
    zookeeper_client_port = module_configs.get_property_value("zookeeper", "zoo.cfg", "clientPort")
    self.assertEquals(int(zookeeper_client_port), 2181)
    zookeeper_client_port_fake = module_configs.get_property_value("zookeeper", "zoo.cfg", "clientPort1")
    self.assertEquals(zookeeper_client_port_fake, None)
    zookeeper_client_port_default_value = module_configs.get_property_value("zookeeper", "zoo.cfg", "clientPort1", 1111)
    self.assertEquals(int(zookeeper_client_port_default_value), 1111)
    zookeeper_empty_value = module_configs.get_property_value("zookeeper", "zoo_fake", "", {})
    self.assertEquals(zookeeper_empty_value, {})
    zookeeper_log_max_backup_size = module_configs.get_property_value('zookeeper', 'zookeeper-log4j',
                                                                      'zookeeper_log_max_backup_size', 10)
    self.assertEquals(zookeeper_log_max_backup_size, 10)

  def test_get_stack_name(self):
    stack_name = self.__execution_command.get_stack_name()
    self.assertEquals(stack_name, "HDPCORE")