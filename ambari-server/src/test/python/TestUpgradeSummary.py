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
from resource_management.libraries.functions import upgrade_summary
from resource_management.libraries.script import Script
from unittest import TestCase

Logger.initialize_logger()

class TestUpgradeSummary(TestCase):

  def test_upgrade_summary(self):
    """
    Tests that simple upgrade information can be extracted from JSON
    :return:
    """
    command_json = TestUpgradeSummary._get_cluster_simple_upgrade_json()
    Script.config = command_json

    summary = upgrade_summary.get_upgrade_summary()
    self.assertEqual(False, summary.is_revert)
    self.assertEqual("UPGRADE", summary.direction)

    service_groups = summary.service_groups
    self.assertEqual("express_upgrade", service_groups["SG1"].type)

    services = service_groups["SG1"].services
    self.assertEqual("3.0.0.0-b1", services["HDFS"].source_version)
    self.assertEqual("3.1.0.0-b1", services["HDFS"].target_version)

    self.assertEqual("3.0.0.0-b1", upgrade_summary.get_source_version(service_group_name = "SG1", service_name = "HDFS"))
    self.assertEqual("3.1.0.0-b1", upgrade_summary.get_target_version(service_group_name = "SG1", service_name = "HDFS"))

    self.assertTrue(upgrade_summary.get_downgrade_from_version(service_group_name = "SG1", service_name="HDFS") is None)


  def test_get_downgrade_from_version(self):
    """
    Tests that simple downgrade returns the correct version
    :return:
    """
    command_json = TestUpgradeSummary._get_cluster_simple_downgrade_json()
    Script.config = command_json

    self.assertTrue(upgrade_summary.get_downgrade_from_version(service_group_name = "FOO", service_name =  "BAR") is None)
    self.assertEqual("3.1.0.0-b1", upgrade_summary.get_downgrade_from_version(service_group_name = "SG1", service_name =  "HDFS"))


  @staticmethod
  def _get_cluster_simple_upgrade_json():
    """
    A restart command during an upgrade.
    :return:
    """
    return {
      "roleCommand":"ACTIONEXECUTE",
      "upgradeSummary": {
        "serviceGroups":{
          "SG1":{
            "type":"express_upgrade",
            "serviceGroupId": 1,
            "serviceGroupName": "SG1",
            "sourceMpackId": 50,
            "targetMpackId": 100,
            "sourceStack": "HDPCORE-1.0",
            "targetStack": "HDPCORE-1.5",
            "sourceMpackVersion": "1.0.0.0-b1",
            "targetMpackVersion": "1.5.0.0-b1",
            "services":{
              "HDFS":{
                "serviceName": "HDFS",
                "sourceVersion":"3.0.0.0-b1",
                "targetVersion":"3.1.0.0-b1",
                "components": {
                  "componentName": "NAMENODE",
                  "sourceVersion": "3.0.0.0-b1",
                  "targetVersion":"3.1.0.0-b1",
                }
              }
            }
          }
        },
        "direction":"UPGRADE",
        "isRevert":False
      }
    }

  @staticmethod
  def _get_cluster_simple_downgrade_json():
    """
    A restart command during a downgrade.
    :return:
    """
    return {
      "roleCommand":"ACTIONEXECUTE",
      "upgradeSummary": {
        "serviceGroups":{
          "SG1":{
            "type": "express_upgrade",
            "serviceGroupId": 1,
            "serviceGroupName": "SG1",
            "sourceMpackId": 100,
            "targetMpackId": 50,
            "sourceStack": "HDPCORE-1.5",
            "targetStack": "HDPCORE-1.0",
            "sourceMpackVersion": "1.5.0.0-b1",
            "targetMpackVersion": "1.0.0.0-b1",
            "services":{
              "HDFS":{
                "serviceName": "HDFS",
                "sourceVersion":"3.1.0.0-b1",
                "targetVersion":"3.0.0.0-b1",
                "components": {
                }
              }
            }
          }
        },
        "direction": "DOWNGRADE",
        "isRevert": False
      }
    }
