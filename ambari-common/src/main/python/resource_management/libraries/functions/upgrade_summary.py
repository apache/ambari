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
from resource_management.libraries.script.script import Script
from resource_management.libraries.functions.constants import Direction

UpgradeSummary = namedtuple("UpgradeSummary", "direction is_revert service_groups")

UpgradeServiceGroupSummary = namedtuple("UpgradeServiceGroupSummary", "type service_group_id  service_group_name source_mpack_id target_mpack_id source_stack target_stack source_mpack_version target_mpack_version services")
UpgradeServiceSummary = namedtuple("UpgradeServiceSummary", "service_name source_version target_version components")
UpgradeComponentSummary = namedtuple("UpgradeComponentSummary", "component_name source_version target_version")


def get_upgrade_summary():
  """
  Gets a summary of an upgrade in progress, including type, direction, orchestration and from/to
  versions.
  """
  config = Script.get_config()
  if "upgradeSummary" not in config or not config["upgradeSummary"]:
    return None

  upgrade_summary = config["upgradeSummary"]

  service_group_summary_dict = {}
  for service_group_name, service_group_summary_json in upgrade_summary["serviceGroups"].iteritems():
    service_summary_dict = {}

    service_group_summary = UpgradeServiceGroupSummary(type = service_group_summary_json["type"],
      service_group_id = service_group_summary_json["serviceGroupId"],
      service_group_name = service_group_summary_json["serviceGroupName"],
      source_mpack_id = service_group_summary_json["sourceMpackId"],
      target_mpack_id = service_group_summary_json["targetMpackId"],
      source_stack = service_group_summary_json["sourceStack"],
      target_stack = service_group_summary_json["targetStack"],
      source_mpack_version = service_group_summary_json["sourceMpackVersion"],
      target_mpack_version = service_group_summary_json["targetMpackVersion"],
      services = service_summary_dict)

    service_group_summary_dict[service_group_name] = service_group_summary

    for service_name, service_summary_json in service_group_summary_json["services"].iteritems():
      component_summary_dict = {}

      service_summary = UpgradeServiceSummary(service_name = service_name,
        source_version = service_summary_json["sourceVersion"],
        target_version = service_summary_json["targetVersion"], components = component_summary_dict)

      service_summary_dict[service_name] = service_summary

      for component_name, component_summary_json in service_summary_json["components"].iteritems():
        component_summary = UpgradeComponentSummary(component_name = component_name,
          source_version = component_summary_json["sourceVersion"],
          target_version = component_summary_json["targetVersion"])
        component_summary_dict[component_name] = component_summary

  return UpgradeSummary(direction=upgrade_summary["direction"],
    is_revert = upgrade_summary["isRevert"],
    service_groups = service_group_summary_dict)


def get_source_version(service_group_name = None, service_name = None, default_version=None):
  """
  Gets the source (from) version of a service participating in an upgrade. If there is no
  upgrade or the specific service is not participating, this will return None.
  :param service_group_name:  the service group name to check for, or None to extract it from the command
  :param service_name:  the service name to check for, or None to extract it from the command
  :param default_version: if the version of the service can't be calculated, this optional
  default value is returned
  :return:  the version that the service is upgrading from or None if there is no upgrade or
  the service is not included in the upgrade.
  """
  service_summary = _get_service_summary(service_group_name, service_name)
  if service_summary is None:
    return default_version

  return service_summary.source_version


def get_target_version(service_group_name = None, service_name = None, default_version=None):
  """
  Gets the target (to) version of a service participating in an upgrade. If there is no
  upgrade or the specific service is not participating, this will return None.
  :param service_group_name:  the service group name to check for, or None to extract it from the command
  :param service_name:  the service name to check for, or None to extract it from the command
  :param default_version: if the version of the service can't be calculated, this optional
  default value is returned
  :return:  the version that the service is upgrading to or None if there is no upgrade or
  the service is not included in the upgrade.
  """
  service_summary = _get_service_summary(service_group_name, service_name)
  if service_summary is None:
    return default_version

  return service_summary.target_version


def get_downgrade_from_version(service_group_name = None, service_name = None):
  """
  Gets the downgrade-from-version for the specificed service. If there is no downgrade or
  the service isn't participating in the downgrade, then this will return None
  :param service_group_name:  the service group, or optionally onmitted to infer it from the command.
  :param service_name:  the service, or optionally onmitted to infer it from the command.
  :return: the downgrade-from-version or None
  """
  upgrade_summary = get_upgrade_summary()
  if upgrade_summary is None:
    return None

  if Direction.DOWNGRADE.lower() != upgrade_summary.direction.lower():
    return None

  service_summary = _get_service_summary(service_group_name, service_name)
  if service_summary is None:
    return None

  return service_summary.source_version


def _get_service_group_summary(service_group_name):
  """
  Gets the service group summary for the upgrade/downgrade for the given service group, or None if
  the service group isn't participating.
  :param service_group_name the service group name
  :return:  the service group summary or None
  """
  upgrade_summary = get_upgrade_summary()
  if upgrade_summary is None:
    return None

  if service_group_name is None:
    execution_command = Script.get_execution_command()
    service_group_name = execution_command.get_servicegroup_name()

  service_group_summary = upgrade_summary.service_groups
  if service_group_name not in service_group_summary:
    return None

  return service_group_summary[service_group_name]


def _get_service_summary(service_group_name, service_name):
  """
  Gets the service summary for the upgrade/downgrade for the given service, or None if
  the service isn't participating.
  :param service_group_name the service group name
  :param service_name:  the service name
  :return:  the service summary or None
  """
  upgrade_summary = get_upgrade_summary()
  if upgrade_summary is None:
    return None

  execution_command = Script.get_execution_command()

  if service_group_name is None:
    service_group_name = execution_command.get_servicegroup_name()

  if service_name is None:
    service_name = execution_command.get_module_name()

  service_group_summary = _get_service_group_summary(service_group_name)
  if service_group_summary is None or service_name not in service_group_summary.services:
    return None

  return service_group_summary.services[service_name]
