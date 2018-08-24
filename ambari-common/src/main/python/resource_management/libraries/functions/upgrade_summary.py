#!/usr/bin/env python
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

from collections import namedtuple
from ambari_commons.constants import UPGRADE_TYPE_EXPRESS
from ambari_commons.constants import UPGRADE_TYPE_HOST_ORDERED
from ambari_commons.constants import UPGRADE_TYPE_ROLLING
from resource_management.libraries.functions.constants import Direction

UpgradeServiceGroupSummary = namedtuple("UpgradeServiceGroupSummary",
  ["type", "service_group_id", "service_group_name", "source_mpack_id", "target_mpack_id",
    "source_mpack_name", "target_mpack_name", "source_mpack_version", "target_mpack_version",
    "source_stack", "target_stack", "services"])

UpgradeServiceSummary = namedtuple("UpgradeServiceSummary", ["service_name", "source_version", "target_version", "components"])
UpgradeComponentSummary = namedtuple("UpgradeComponentSummary", ["component_name", "source_version", "target_version"])

__all__ = ["UpgradeSummary"]

class UpgradeSummary(object):
  """
  Represents the state of an upgrade or downgrade is one is in progress, including all service groups,
  services, components and their respective mpack source/target versions.
  """
  def __init__(self):
    from resource_management.libraries.script.script import Script

    config = Script.get_config()
    if "upgradeSummary" not in config or not config["upgradeSummary"]:
      self.is_upgrade_in_progress = False
      return

    self.is_upgrade_in_progress = True
    self.execution_command = Script.get_execution_command()
    upgrade_summary = config["upgradeSummary"]

    service_group_summary_dict = {}
    for service_group_name, service_group_summary_json in upgrade_summary["serviceGroups"].iteritems():
      service_summary_dict = {}

      service_group_summary = UpgradeServiceGroupSummary(type = service_group_summary_json["type"],
        service_group_id = service_group_summary_json["serviceGroupId"],
        service_group_name = service_group_summary_json["serviceGroupName"],
        source_mpack_id = service_group_summary_json["sourceMpackId"],
        target_mpack_id = service_group_summary_json["targetMpackId"],
        source_mpack_name =  service_group_summary_json["sourceMpackName"],
        target_mpack_name = service_group_summary_json["targetMpackName"],
        source_mpack_version = service_group_summary_json["sourceMpackVersion"],
        target_mpack_version = service_group_summary_json["targetMpackVersion"],
        source_stack = service_group_summary_json["sourceStack"],
        target_stack = service_group_summary_json["targetStack"],
        services = service_summary_dict)

      service_group_summary_dict[service_group_name] = service_group_summary

      for service_name, service_summary_json in service_group_summary_json["services"].iteritems():
        component_summary_dict = {}

        service_summary = UpgradeServiceSummary(service_name = service_name,
          source_version = service_summary_json["sourceVersion"],
          target_version = service_summary_json["targetVersion"],
          components = component_summary_dict)

        service_summary_dict[service_name] = service_summary

        for component_name, component_summary_json in service_summary_json["components"].iteritems():
          component_summary = UpgradeComponentSummary(component_name = component_name,
            source_version = component_summary_json["sourceVersion"],
            target_version = component_summary_json["targetVersion"])

          component_summary_dict[component_name] = component_summary

    self.direction = upgrade_summary["direction"]
    self.is_revert = upgrade_summary["isRevert"]
    self.service_groups = service_group_summary_dict


  def get_upgrade_type(self):
    """
    Gets the type of upgrade for the service group in the command.
    :return:  the type of upgrade or None
    """
    if not self.is_upgrade_in_progress:
      return None

    service_group_name = self.execution_command.get_servicegroup_name()

    service_group_summary = self.get_service_group_summary(service_group_name)
    if service_group_summary is None:
      return None

    if service_group_summary.type.lower() == "rolling_upgrade":
      return UPGRADE_TYPE_ROLLING
    elif service_group_summary.type.lower() == "express_upgrade":
      return UPGRADE_TYPE_EXPRESS
    elif service_group_summary.type.lower() == "host_ordered_upgrade":
      return UPGRADE_TYPE_HOST_ORDERED

    return None


  def get_downgrade_from_version(self, service_group_name = None, service_name = None):
    """
    Gets the downgrade-from-version for the specificed service. If there is no downgrade or
    the service isn't participating in the downgrade, then this will return None
    :param service_group_name:  the service group, or optionally onmitted to infer it from the command.
    :param service_name:  the service, or optionally onmitted to infer it from the command.
    :return: the downgrade-from-version or None
    """
    if Direction.DOWNGRADE.lower() != self.direction.lower():
      return None

    service_summary = self.get_service_summary(service_group_name, service_name)
    if service_summary is None:
      return None

    return service_summary.source_version


  def get_service_group_summary(self, service_group_name):
    """
    Gets the service group summary for the upgrade/downgrade for the given service group, or None if
    the service group isn't participating.
    :param service_group_name the service group name
    :return:  the service group summary or None
    """
    if service_group_name is None:
      service_group_name = self.execution_command.get_servicegroup_name()

    if service_group_name not in self.service_groups:
      return None

    return self.service_groups[service_group_name]


  def get_service_summary(self, service_group_name, service_name):
    """
    Gets the service summary for the upgrade/downgrade for the given service, or None if
    the service isn't participating.
    :param service_group_name the service group name
    :param service_name:  the service name
    :return:  the service summary or None
    """
    if service_group_name is None:
      service_group_name = self.execution_command.get_servicegroup_name()

    if service_name is None:
      service_name = self.execution_command.get_module_name()

    service_group_summary = self.get_service_group_summary(service_group_name)
    if service_group_summary is None or service_name not in service_group_summary.services:
      return None

    return service_group_summary.services[service_name]


  def get_service_source_version(self, service_group_name, service_name, default_version = None):
    """
    Gets the source version of the service (aka the module) during an upgrade. This will not
    return an mpack verison, but the specific module version instead.
    :param service_group_name: the service group name
    :param service_name: the service (module) name
    :param default_version:  the default version to return.
    :return:
    """
    service_summary = self.get_service_summary(service_group_name, service_name)
    if service_summary is None:
      return default_version

    return service_summary.source_version


  def get_service_target_version(self, service_group_name, service_name, default_version = None):
    """
    Gets the target version of the service (aka the module) during an upgrade. This will not
    return an mpack verison, but the specific module version instead.
    :param service_group_name: the service group name
    :param service_name: the service (module) name
    :param default_version:  the default version to return.
    :return:
    """
    service_summary = self.get_service_summary(service_group_name, service_name)
    if service_summary is None:
      return default_version

    return service_summary.target_version