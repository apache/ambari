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
        from resource_management.libraries.script import Script
        Script.execution_command = self.__execution_command
        Script.module_configs = Script.get_module_configs()
        Script.stack_settings = Script.get_stack_settings()
        Script.cluster_settings = Script.get_cluster_settings()
    except IOError:
      Logger.error("Can not read json file with command parameters: ")
      sys.exit(1)

  def test_get_module_name(self):
    module_name = self.__execution_command.get_module_name()
    self.assertEquals(module_name, "ZOOKEEPER")

  def test_get_oozie_server_hosts(self):
    oozie_server = self.__execution_command.get_component_hosts('oozie_server')
    self.assertEqual(oozie_server, 'host2')

  def test_get_ganglia_server_hosts(self):
    ganglia_server_hosts = self.__execution_command.get_component_hosts('ganglia_server')
    self.assertEqual(ganglia_server_hosts, 'host1')

  def test_get_java_version(self):
    java_version = self.__execution_command.get_java_version()
    self.assertEqual(java_version, 8)

  def test_get_module_configs(self):
    module_configs = self.__execution_command.get_module_configs()
    self.assertNotEquals(module_configs, None)
    zookeeper_client_port = module_configs.get_property_value("zookeeper", "zoo.cfg", "clientPort")
    self.assertEquals(int(zookeeper_client_port), 2181)
    zookeeper_client_port_fake = module_configs.get_property_value("zookeeper", "zoo.cfg", "clientPort1")
    self.assertEquals(zookeeper_client_port_fake, None)
    zookeeper_client_port_default_value = module_configs.get_property_value("zookeeper", "zoo.cfg", "clientPort1", 1111)
    self.assertEquals(int(zookeeper_client_port_default_value), 1111)
    zookeeper_empty_value = module_configs.get_all_properties("zookeeper", "zoo_fake")
    self.assertEquals(zookeeper_empty_value, {})
    zookeeper_log_max_backup_size = module_configs.get_property_value('zookeeper', 'zookeeper-log4j',
                                                                      'zookeeper_log_max_backup_size', 10)
    self.assertEquals(zookeeper_log_max_backup_size, 10)
    properties = module_configs.get_properties("zookeeper", "zoo.cfg", ['clientPort', 'dataDir', 'fake'])
    self.assertEqual(int(properties.get('clientPort')), 2181)
    self.assertEqual(properties.get('fake'), None)

    sqoop = bool(module_configs.get_all_properties("zookeeper", 'sqoop-env'))
    self.assertFalse(sqoop)

  def test_access_to_module_configs(self):
    module_configs = self.__execution_command.get_module_configs()
    is_zoo_cfg_there = bool(module_configs.get_all_properties("zookeeper", "zoo.cfg"))
    self.assertTrue(is_zoo_cfg_there)
    zoo_cfg = module_configs.get_all_properties("zookeeper", "zoo.cfg")
    self.assertTrue(isinstance(zoo_cfg, dict))

  def test_null_value(self):
    versions = self.__execution_command.get_value("Versions")
    self.assertEqual(versions, None)
    versions = self.__execution_command.get_value("Versions", "1.1.1.a")
    self.assertEqual(versions, "1.1.1.a")
    module_configs = self.__execution_command.get_module_configs()
    version = module_configs.get_property_value("zookeeper", "zoo.cfg", "version")
    self.assertEqual(version, None)
    version = module_configs.get_property_value("zookeeper", "zoo.cfg", "version", "3.0.b")
    self.assertEqual(version, "3.0.b")

  def test_access_to_stack_settings(self):
    stack_settings = self.__execution_command.get_stack_settings()
    stack_name = stack_settings.get_mpack_name()
    self.assertEquals(stack_name, "HDPCORE")
    stack_version = stack_settings.get_mpack_version()
    self.assertEqual(stack_version, "1.0.0-b645")
    user_groups = stack_settings.get_user_groups()
    self.assertTrue("ambari-qa" in user_groups)
    group_list = stack_settings.get_group_list()
    self.assertTrue("users" in group_list)
    self.assertFalse("zookeeper" in group_list)
    stack_features = stack_settings.get_stack_features()
    self.assertTrue("snappy" in stack_features)
    stack_package = stack_settings.get_stack_packages()
    self.assertTrue("ACCUMULO" in stack_package)
    stack_tools = stack_settings.get_stack_tools()
    self.assertTrue("conf_selector" in stack_tools)

  def test_access_to_cluster_settings(self):
    cluster_settings = self.__execution_command.get_cluster_settings()
    security_enabled = cluster_settings.is_cluster_security_enabled()
    self.assertFalse(security_enabled)
    recovery_count = cluster_settings.get_recovery_max_count()
    self.assertEqual(recovery_count, 6)
    recovery_enabled = cluster_settings.check_recovery_enabled()
    self.assertTrue(recovery_enabled)
    recovery_type = cluster_settings.get_recovery_type()
    self.assertEqual(recovery_type, "AUTO_START")
    kerberos_domain = cluster_settings.get_kerberos_domain()
    self.assertEqual(kerberos_domain, "EXAMPLE.COM")
    smoke_user = cluster_settings.get_smokeuser()
    self.assertEqual(smoke_user, "ambari-qa")
    user_group = cluster_settings.get_user_group()
    self.assertEqual(user_group, "hadoop")
    suse_rhel_template = cluster_settings.get_repo_suse_rhel_template()
    self.assertTrue("if mirror_list" in suse_rhel_template)
    ubuntu_template = cluster_settings.get_repo_ubuntu_template()
    self.assertTrue("package_type" in ubuntu_template)
    override_uid = cluster_settings.check_override_uid()
    self.assertTrue(override_uid)
    skip_copy = cluster_settings.check_sysprep_skip_copy_fast_jar_hdfs()
    self.assertFalse(skip_copy)
    skip_setup_jce = cluster_settings.check_sysprep_skip_setup_jce()
    self.assertFalse(skip_setup_jce)
    ignored = cluster_settings.check_ignore_groupsusers_create()
    self.assertFalse(ignored)