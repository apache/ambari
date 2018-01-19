# !/usr/bin/env python

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

from resource_management.core.logger import Logger
from resource_management.libraries.functions import settings
from resource_management.libraries.script import Script

from unittest import TestCase

Logger.initialize_logger()

class TestSettings(TestCase):


  ###### For get_setting_type_entries()

  def test_entries_for_nonexistent_setting_type_is_none(self):
    Script.config = TestSettings._get_simple_command()

    self.assertTrue(settings.get_setting_type_entries('/non_existing_setting_type', None) is None)
    self.assertTrue(settings.get_setting_type_entries('/non_existing_setting_type', 'non_existing_key') is None)
    self.assertTrue(settings.get_setting_type_entries('/non_existing_setting_type', set(['non_existing_key'])) is None)


  def test_entries_for_unsupported_setting_type_is_none(self):
    Script.config = TestSettings._get_simple_command()

    self.assertTrue(settings.get_setting_type_entries('/agentConfigParams', None) is None)
    self.assertTrue(settings.get_setting_type_entries('/agentConfigParams', 'agent') is None)
    self.assertTrue(settings.get_setting_type_entries('/agentConfigParams', set(['agent'])) is None)


  def test_entries_for_supported_type(self):
    Script.config = TestSettings._get_simple_command()

    # For stackSettings
    self.assertEquals(Script.config['stackSettings'], settings.get_setting_type_entries(settings.STACK_SETTINGS_TYPE, None))
    self.assertEquals(Script.config['stackSettings'], settings.get_setting_type_entries(settings.STACK_SETTINGS_TYPE))

    # For clusterSettings
    self.assertEquals(Script.config['clusterSettings'], settings.get_setting_type_entries(settings.CLUSTER_SETTINGS_TYPE, None))
    self.assertEquals(Script.config['clusterSettings'], settings.get_setting_type_entries(settings.CLUSTER_SETTINGS_TYPE))


  def test_full_subset_of_entries_for_supported_type(self):
    Script.config = TestSettings._get_simple_command()

    # For stackSettings
    stackSettings = Script.config['stackSettings']
    self.assertEquals(stackSettings, settings.get_setting_type_entries(settings.STACK_SETTINGS_TYPE, set(stackSettings.keys())))
    self.assertEquals(stackSettings, settings.get_setting_type_entries(settings.STACK_SETTINGS_TYPE, frozenset(stackSettings.keys())))
    self.assertEquals(stackSettings, settings.get_setting_type_entries(settings.STACK_SETTINGS_TYPE, tuple(stackSettings.keys())))
    self.assertEquals(stackSettings, settings.get_setting_type_entries(settings.STACK_SETTINGS_TYPE, list(stackSettings.keys())))

    # For clusterSettings
    clusterSettings = Script.config['clusterSettings']
    self.assertEquals(clusterSettings, settings.get_setting_type_entries(settings.CLUSTER_SETTINGS_TYPE, set(clusterSettings.keys())))
    self.assertEquals(clusterSettings, settings.get_setting_type_entries(settings.CLUSTER_SETTINGS_TYPE, frozenset(clusterSettings.keys())))
    self.assertEquals(clusterSettings, settings.get_setting_type_entries(settings.CLUSTER_SETTINGS_TYPE, tuple(clusterSettings.keys())))
    self.assertEquals(clusterSettings, settings.get_setting_type_entries(settings.CLUSTER_SETTINGS_TYPE, list(clusterSettings.keys())))

  def test_real_subset_of_entries_for_supported_type(self):
    Script.config = TestSettings._get_simple_command()

    # For stackSettings
    self.assertEquals({'stack_name':'HDP'}, settings.get_setting_type_entries(settings.STACK_SETTINGS_TYPE, set(['stack_name'])))
    self.assertEquals({'stack_name':'HDP'}, settings.get_setting_type_entries(settings.STACK_SETTINGS_TYPE, frozenset(['stack_name'])))
    self.assertEquals({'stack_name':'HDP'}, settings.get_setting_type_entries(settings.STACK_SETTINGS_TYPE, tuple(['stack_name'])))
    self.assertEquals({'stack_name':'HDP'}, settings.get_setting_type_entries(settings.STACK_SETTINGS_TYPE, list(['stack_name'])))
    self.assertEquals({'stack_name': 'HDP', 'stack_version': '2.4'}, settings.get_setting_type_entries(settings.STACK_SETTINGS_TYPE, list(['stack_name','stack_name', 'stack_version'])))

    # For clusterSettings
    self.assertEquals({'smokeuser': 'ambari-qa'}, settings.get_setting_type_entries(settings.CLUSTER_SETTINGS_TYPE, set(['smokeuser'])))
    self.assertEquals({'smokeuser': 'ambari-qa'}, settings.get_setting_type_entries(settings.CLUSTER_SETTINGS_TYPE, frozenset(['smokeuser'])))
    self.assertEquals({'smokeuser': 'ambari-qa'}, settings.get_setting_type_entries(settings.CLUSTER_SETTINGS_TYPE, tuple(['smokeuser'])))
    self.assertEquals({'smokeuser': 'ambari-qa'}, settings.get_setting_type_entries(settings.CLUSTER_SETTINGS_TYPE, list(['smokeuser'])))
    self.assertEquals({'recovery_type': 'AUTO_START', 'smokeuser': 'ambari-qa'}, settings.get_setting_type_entries(settings.CLUSTER_SETTINGS_TYPE, list(['smokeuser', 'recovery_type', 'smokeuser'])))

  def test_empty_subset_of_entries_for_supported_type(self):
    Script.config = TestSettings._get_simple_command()

    # For stackSettings
    self.assertEquals({}, settings.get_setting_type_entries(settings.STACK_SETTINGS_TYPE, set(['non_existing_key'])))
    self.assertEquals({}, settings.get_setting_type_entries(settings.STACK_SETTINGS_TYPE, frozenset(['non_existing_key'])))
    self.assertEquals({}, settings.get_setting_type_entries(settings.STACK_SETTINGS_TYPE, tuple(['non_existing_key'])))
    self.assertEquals({}, settings.get_setting_type_entries(settings.STACK_SETTINGS_TYPE, list(['non_existing_key'])))

    # For clusterSettings
    self.assertEquals({}, settings.get_setting_type_entries(settings.CLUSTER_SETTINGS_TYPE, set(['non_existing_key'])))
    self.assertEquals({}, settings.get_setting_type_entries(settings.CLUSTER_SETTINGS_TYPE, frozenset(['non_existing_key'])))
    self.assertEquals({}, settings.get_setting_type_entries(settings.CLUSTER_SETTINGS_TYPE, tuple(['non_existing_key'])))
    self.assertEquals({}, settings.get_setting_type_entries(settings.CLUSTER_SETTINGS_TYPE, list(['non_existing_key'])))

  def test_empty_string_entries_for_supported_type(self):
    Script.config = TestSettings._get_simple_command()

    # For stackSettings
    self.assertEquals({}, settings.get_setting_type_entries(settings.STACK_SETTINGS_TYPE, set([""])))
    self.assertEquals({}, settings.get_setting_type_entries(settings.STACK_SETTINGS_TYPE, set(["", ""])))
    self.assertEquals({}, settings.get_setting_type_entries(settings.STACK_SETTINGS_TYPE, frozenset(["", ""])))
    self.assertEquals({}, settings.get_setting_type_entries(settings.STACK_SETTINGS_TYPE, tuple(["", ""])))
    self.assertEquals({}, settings.get_setting_type_entries(settings.STACK_SETTINGS_TYPE, list(["", ""])))

    # For clusterSettings
    self.assertEquals({}, settings.get_setting_type_entries(settings.CLUSTER_SETTINGS_TYPE, set([""])))
    self.assertEquals({}, settings.get_setting_type_entries(settings.CLUSTER_SETTINGS_TYPE, set(["", ""])))
    self.assertEquals({}, settings.get_setting_type_entries(settings.CLUSTER_SETTINGS_TYPE, frozenset(["", ""])))
    self.assertEquals({}, settings.get_setting_type_entries(settings.CLUSTER_SETTINGS_TYPE, tuple(["", ""])))
    self.assertEquals({}, settings.get_setting_type_entries(settings.CLUSTER_SETTINGS_TYPE, list(["", ""])))

  ###### For get_setting_value()

  def test_value_of_nonexistent_setting_type_is_none(self):
    Script.config = TestSettings._get_simple_command()

    self.assertTrue(settings.get_setting_value('/non_existing_setting_type', None) is None)
    self.assertTrue(settings.get_setting_value('/non_existing_setting_type', 'non_existing_key') is None)


  def test_value_of_unsupported_setting_type_is_none(self):
    Script.config = TestSettings._get_simple_command()

    self.assertTrue(settings.get_setting_value('/agentConfigParams', None) is None)
    self.assertTrue(settings.get_setting_value('/agentConfigParams', 'non_existing_key') is None)
    self.assertTrue(settings.get_setting_value('/agentConfigParams', 'agent') is None)
    self.assertTrue(settings.get_setting_value('/agentConfigParams', set(['agent'])) is None)
    self.assertTrue(settings.get_setting_value('/agentConfigParams', frozenset(['agent'])) is None)
    self.assertTrue(settings.get_setting_value('/agentConfigParams', tuple(['agent'])) is None)
    self.assertTrue(settings.get_setting_value('/agentConfigParams', list(['agent'])) is None)


  def test_value_of_supported_setting_type_non_existent_name(self):
    Script.config = TestSettings._get_simple_command()

    # For stackSettings
    self.assertTrue(settings.get_setting_value(settings.STACK_SETTINGS_TYPE, 'non_existing_key') is None)

    # For clusterSettings
    self.assertTrue(settings.get_setting_value(settings.CLUSTER_SETTINGS_TYPE, 'non_existing_key') is None)

  def test_value_of_supported_setting_type_empy_string_name(self):
    Script.config = TestSettings._get_simple_command()

    # For stackSettings
    self.assertTrue(settings.get_setting_value(settings.STACK_SETTINGS_TYPE, "") is None)

    # For clusterSettings
    self.assertTrue(settings.get_setting_value(settings.CLUSTER_SETTINGS_TYPE, "") is None)

  def test_value_of_supported_setting_type_setting_name_none(self):
    Script.config = TestSettings._get_simple_command()

    # For stackSettings
    self.assertTrue(settings.get_setting_value(settings.STACK_SETTINGS_TYPE, None) is None)

    # For clusterSettings
    self.assertTrue(settings.get_setting_value(settings.CLUSTER_SETTINGS_TYPE, None) is None)

  def test_value_of_supported_setting_type_and_name(self):
    Script.config = TestSettings._get_simple_command()

    # For stackSettings
    stack_settings = Script.config['stackSettings']
    for k in stack_settings.keys():
      self.assertEquals(stack_settings[k], settings.get_setting_value(settings.STACK_SETTINGS_TYPE, k))

    # For clusterSettings
    cluster_settings = Script.config['clusterSettings']
    for k in cluster_settings.keys():
      self.assertEquals(cluster_settings[k], settings.get_setting_value(settings.CLUSTER_SETTINGS_TYPE, k))


  @staticmethod
  def _get_simple_command():
    """
    A simple command with stackSettings, clusterSettings, and some other data.
    """
    return {
      "stackSettings": {
        "stack_name": "HDP",
        "stack_version": "2.4",
      },
      "clusterSettings": {
        "recovery_enabled": "false",
        "smokeuser": "ambari-qa",
        "recovery_type": "AUTO_START",
        "user_group": "hadoop",
      },
      "agentConfigParams": {
        "agent": {
          "parallel_execution": 1,
        }
      },
      "localComponents": [
        "INFRA_SOLR_CLIENT",
        "MYSQL_SERVER",
        "SECONDARY_NAMENODE",
      ]
    }
