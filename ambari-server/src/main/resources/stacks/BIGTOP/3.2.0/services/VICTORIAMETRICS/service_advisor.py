#!/usr/bin/env python3
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
import traceback


SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
STACKS_DIR = os.path.join(SCRIPT_DIR, "../../../../../stacks/")
PARENT_FILE = os.path.join(STACKS_DIR, "service_advisor.py")

try:
  if "BASE_SERVICE_ADVISOR" in os.environ:
    PARENT_FILE = os.environ["BASE_SERVICE_ADVISOR"]
  with open(PARENT_FILE, "rb") as fp:
    service_advisor = imp.load_module(
      "service_advisor", fp, PARENT_FILE, (".py", "rb", imp.PY_SOURCE)
    )
except Exception:
  traceback.print_exc()
  print("Failed to load parent")


class VictoriaMetricsServiceAdvisor(service_advisor.ServiceAdvisor):
  CORE_CLUSTER_COMPONENTS = ("VMSTORAGE", "VMINSERT", "VMSELECT")

  def getHostsForMasterComponent(self, services, hosts, component, hostsList):
    if self.isComponentHostsPopulated(component):
      return super(VictoriaMetricsServiceAdvisor, self).getHostsForMasterComponent(
        services, hosts, component, hostsList
      )

    component_name = self.getComponentName(component)
    mode = self._deployment_mode(services)
    if mode == "single" and component_name in ("VMINSERT", "VMSELECT"):
      return []
    if mode == "cluster" and component_name == "VICTORIAMETRICS_SERVER":
      return []
    return super(VictoriaMetricsServiceAdvisor, self).getHostsForMasterComponent(
      services, hosts, component, hostsList
    )

  def getHostsForSlaveComponent(
    self, services, hosts, component, hostsList, freeHosts
  ):
    if self.isComponentHostsPopulated(component):
      return super(VictoriaMetricsServiceAdvisor, self).getHostsForSlaveComponent(
        services, hosts, component, hostsList, freeHosts
      )

    if (
      self._deployment_mode(services) == "single"
      and self.getComponentName(component) == "VMSTORAGE"
    ):
      return []
    return super(VictoriaMetricsServiceAdvisor, self).getHostsForSlaveComponent(
      services, hosts, component, hostsList, freeHosts
    )

  def getServiceComponentLayoutValidations(self, services, hosts):
    items = self.getServiceComponentCardinalityValidations(
      services, hosts, "VICTORIAMETRICS"
    )
    component_counts = self._component_counts(services)
    properties = self.getServicesSiteProperties(services, "victoriametrics") or {}
    mode = properties.get("deployment_mode", "single")

    if mode == "single":
      if component_counts.get("VICTORIAMETRICS_SERVER", 0) != 1:
        items.append(
          self._layout_item(
            "VICTORIAMETRICS_SERVER",
            "Single-node mode requires exactly one VictoriaMetrics Server.",
          )
        )
      for component in self.CORE_CLUSTER_COMPONENTS:
        if component_counts.get(component, 0):
          items.append(
            self._layout_item(
              component,
              "{0} cannot be assigned in single-node mode.".format(component),
            )
          )
    elif mode == "cluster":
      if component_counts.get("VICTORIAMETRICS_SERVER", 0):
        items.append(
          self._layout_item(
            "VICTORIAMETRICS_SERVER",
            "VictoriaMetrics Server cannot be assigned in distributed mode.",
          )
        )
      for component in self.CORE_CLUSTER_COMPONENTS:
        if component_counts.get(component, 0) < 1:
          items.append(
            self._layout_item(
              component,
              "Distributed mode requires at least one {0} component.".format(
                component
              ),
            )
          )
      if component_counts.get("VMAUTH", 0) < 1:
        items.append(
          self._layout_item(
            "VMAUTH",
            "Deploy VMAUTH to provide a stable query and ingestion endpoint.",
            level="WARN",
          )
        )
    else:
      items.append(
        self._layout_item(
          "VICTORIAMETRICS_SERVER",
          "Unknown VictoriaMetrics deployment mode: {0}.".format(mode),
        )
      )
    return items

  def getServiceConfigurationRecommendations(
    self, configurations, clusterData, services, hosts
  ):
    pass

  def getServiceConfigurationsValidationItems(
    self, configurations, recommendedDefaults, services, hosts
  ):
    items = []
    core = self.getSiteProperties(configurations, "victoriametrics") or {}
    scrape = self.getSiteProperties(
      configurations, "victoriametrics-scrape"
    ) or {}
    auth = self.getSiteProperties(configurations, "victoriametrics-auth") or {}
    component_counts = self._component_counts(services)

    replication_factor = self._positive_int(
      items,
      "victoriametrics",
      "replication_factor",
      core.get("replication_factor", "1"),
    )
    storage_count = component_counts.get("VMSTORAGE", 0)
    if replication_factor and storage_count:
      if replication_factor > storage_count:
        self._config_error(
          items,
          "victoriametrics",
          "replication_factor",
          "Replication factor cannot exceed the number of VMSTORAGE hosts.",
        )
      elif storage_count < 2 * replication_factor - 1:
        self._config_warning(
          items,
          "victoriametrics",
          "replication_factor",
          "At least 2 * replication_factor - 1 VMSTORAGE hosts are recommended to preserve writes during failures.",
        )
      if replication_factor > 1 and not core.get("dedup_min_scrape_interval"):
        self._config_error(
          items,
          "victoriametrics",
          "dedup_min_scrape_interval",
          "A deduplication interval is required when replication is enabled.",
        )

    scrape_replication = self._positive_int(
      items,
      "victoriametrics-scrape",
      "vmagent_replication_factor",
      scrape.get("vmagent_replication_factor", "1"),
    )
    vmagent_count = component_counts.get("VMAGENT", 0)
    if scrape_replication:
      if scrape_replication > 2:
        self._config_error(
          items,
          "victoriametrics-scrape",
          "vmagent_replication_factor",
          "Scrape replication factor cannot exceed the Agent telemetry concurrency limit of 2.",
        )
      if vmagent_count and scrape_replication > vmagent_count:
        self._config_error(
          items,
          "victoriametrics-scrape",
          "vmagent_replication_factor",
          "Scrape replication factor cannot exceed the number of VMAGENT hosts.",
        )
      if scrape_replication > 1 and not str(
        scrape.get("scrape_interval", "30s")
      ).strip():
        self._config_error(
          items,
          "victoriametrics-scrape",
          "scrape_interval",
          "Scrape interval is required to deduplicate replicated scrapes.",
        )
      if scrape_replication > 1 and scrape.get("remote_write_url"):
        self._config_warning(
          items,
          "victoriametrics-scrape",
          "remote_write_url",
          "External remote-write storage must deduplicate samples at the configured scrape interval.",
        )

    managed_identity = str(
      scrape.get("managed_discovery_identity", "true")
    ).lower() == "true"
    if vmagent_count and not managed_identity:
      if not scrape.get("ambari_sd_username") or not scrape.get(
        "ambari_sd_password"
      ):
        self._config_error(
          items,
          "victoriametrics-scrape",
          "ambari_sd_password",
          "Manual discovery identity requires both an Ambari user and password.",
        )

    require_authentication = str(
      auth.get("require_authentication", "false")
    ).lower() == "true"
    if require_authentication and (
      not auth.get("api_username") or not auth.get("api_password")
    ):
      self._config_error(
        items,
        "victoriametrics-auth",
        "api_password",
        "VMAUTH authentication requires both an API user and password.",
      )
    return items

  @staticmethod
  def _component_counts(services):
    counts = {}
    for service in services.get("services", []):
      stack_service = service.get("StackServices", {})
      if stack_service.get("service_name") != "VICTORIAMETRICS":
        continue
      for component in service.get("components", []):
        data = component.get("StackServiceComponents", {})
        counts[data.get("component_name")] = len(data.get("hostnames") or [])
    return counts

  def _deployment_mode(self, services):
    properties = self.getServicesSiteProperties(services, "victoriametrics") or {}
    return properties.get("deployment_mode", "single")

  @staticmethod
  def _layout_item(component, message, level="ERROR"):
    return {
      "type": "host-component",
      "level": level,
      "message": message,
      "component-name": component,
    }

  def _positive_int(self, items, config_type, config_name, value):
    try:
      parsed = int(value)
      if parsed < 1:
        raise ValueError()
      return parsed
    except (TypeError, ValueError):
      self._config_error(
        items, config_type, config_name, "Value must be a positive integer."
      )
      return None

  def _config_error(self, items, config_type, config_name, message):
    items.extend(
      self.toConfigurationValidationProblems(
        [{"config-name": config_name, "item": self.getErrorItem(message)}],
        config_type,
      )
    )

  def _config_warning(self, items, config_type, config_name, message):
    items.extend(
      self.toConfigurationValidationProblems(
        [{"config-name": config_name, "item": self.getWarnItem(message)}],
        config_type,
      )
    )
