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
from resource_management.libraries.functions.stack_features import get_stack_feature_version
from resource_management.libraries.script import Script
from resource_management.core.exceptions import Fail
from unittest import TestCase

Logger.initialize_logger()

class TestStackFeature(TestCase):
  def test_get_stack_feature_version_missing_params(self):
    try:
      stack_feature_version = get_stack_feature_version({})
      self.assertEqual("2.3.0.0-1234", stack_feature_version)
      self.fail("Expected an exception when there are required parameters missing from the dictionary")
    except Fail:
      pass

  def test_get_stack_feature_version_for_install_command(self):
    """
    Tests the stack feature version calculated during an install command on a new cluster
    :return:
    """
    command_json = TestStackFeature._get_cluster_install_command_json()
    Script.config = command_json

    stack_feature_version = get_stack_feature_version(command_json)
    self.assertEqual("2.4", stack_feature_version)


  def test_get_stack_feature_version_for_upgrade_restart(self):
    """
    Tests the stack feature version calculated during a restart command in an upgrade.
    :return:
    """
    command_json = TestStackFeature._get_cluster_upgrade_restart_json()
    Script.config = command_json

    stack_feature_version = get_stack_feature_version(command_json)
    self.assertEqual("2.5.9.9-9999", stack_feature_version)


  def test_get_stack_feature_version_for_downgrade_restart(self):
    """
    Tests the stack feature version calculated during a restart command in a downgrade.
    :return:
    """
    command_json = TestStackFeature._get_cluster_downgrade_restart_json()
    Script.config = command_json

    stack_feature_version = get_stack_feature_version(command_json)
    self.assertEqual("2.4.0.0-1234", stack_feature_version)


  def test_get_stack_feature_version_for_downgrade_stop(self):
    """
    Tests the stack feature version calculated during a STOP command in a downgrade.
    :return:
    """
    command_json = TestStackFeature._get_cluster_downgrade_stop_json()
    Script.config = command_json

    stack_feature_version = get_stack_feature_version(command_json)
    self.assertEqual("2.5.9.9-9999", stack_feature_version)

    command_json = TestStackFeature._get_cluster_downgrade_stop_custom_command_json()
    Script.config = command_json

    stack_feature_version = get_stack_feature_version(command_json)
    self.assertEqual("2.5.9.9-9999", stack_feature_version)

  @staticmethod
  def _get_cluster_install_command_json():
    """
    Install command JSON with no upgrade and no version information.
    :return:
    """
    return {
      "roleCommand": "ACTIONEXECUTE",
      "hostLevelParams": {
        "stack_name": "HDP",
        "stack_version": "2.4",
      },
      "commandParams": {
        "command_timeout": "1800",
        "script_type": "PYTHON",
        "script": "install_packages.py"
      }
    }

  @staticmethod
  def _get_cluster_upgrade_restart_json():
    """
    A restart command during an upgrade.
    :return:
    """
    return {
      "roleCommand":"ACTIONEXECUTE",
      "hostLevelParams": {
        "stack_name": "HDP",
        "stack_version": "2.4",
        "current_version":  "2.4.0.0-1234"
      },
      "commandParams": {
        "original_stack": "2.4",
        "target_stack": "2.5",
        "upgrade_direction": "upgrade",
        "version": "2.5.9.9-9999"
      }
    }

  @staticmethod
  def _get_cluster_downgrade_restart_json():
    """
    A restart command during a downgrade.
    :return:
    """
    return {
      "roleCommand":"ACTIONEXECUTE",
      "hostLevelParams":{
        "stack_name":"HDP",
        "stack_version":"2.4",
        "current_version":"2.4.0.0-1234"
      },
      "commandParams":{
        "original_stack":"2.4",
        "target_stack":"2.5",
        "upgrade_direction":"downgrade",
        "version":"2.4.0.0-1234",
        "downgrade_from_version": "2.5.9.9-9999"
      }
    }


  @staticmethod
  def _get_cluster_downgrade_stop_json():
    """
    A STOP command during a downgrade.
    :return:
    """
    return {
      "roleCommand":"STOP",
      "hostLevelParams":{
        "stack_name":"HDP",
        "stack_version":"2.5",
        "current_version":"2.4.0.0-1234"
      },
      "commandParams":{
        "original_stack":"2.4",
        "target_stack":"2.5",
        "upgrade_direction":"downgrade",
        "version":"2.4.0.0-1234",
        "downgrade_from_version":"2.5.9.9-9999"
      }
    }

  @staticmethod
  def _get_cluster_downgrade_stop_custom_command_json():
    """
    A STOP command during a downgrade.
    :return:
    """
    return {
      "roleCommand":"CUSTOM_COMMAND",
      "hostLevelParams":{
        "stack_name":"HDP",
        "stack_version":"2.5",
        "current_version":"2.4.0.0-1234",
        "custom_command":"STOP"
      },
      "commandParams":{
        "original_stack":"2.4",
        "target_stack":"2.5",
        "upgrade_direction":"downgrade",
        "version":"2.4.0.0-1234",
        "downgrade_from_version":"2.5.9.9-9999"
      }
    }