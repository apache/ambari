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

# Python imports
from ambari_commons import import_utils
import os
import inspect
import ipaddress
import socket
import math
import re
from math import floor

# Local imports


SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
STACKS_DIR = os.path.join(SCRIPT_DIR, "../../../../../stacks/")
PARENT_FILE = os.path.join(STACKS_DIR, "service_advisor.py")

if "BASE_SERVICE_ADVISOR" in os.environ:
  PARENT_FILE = os.environ["BASE_SERVICE_ADVISOR"]
with open(PARENT_FILE, "rb") as fp:
  service_advisor = import_utils.load_module(
    "service_advisor", fp, PARENT_FILE, (".py", "rb", import_utils.PY_SOURCE)
  )


def _effective_site_properties(configurations, services, site_name):
  effective = {}
  service_configurations = (services or {}).get("configurations", {})
  existing_site = service_configurations.get(site_name, {})
  existing_properties = existing_site.get("properties", {})
  if isinstance(existing_properties, dict):
    effective.update(existing_properties)
  proposed_site = (configurations or {}).get(site_name, {})
  proposed_properties = proposed_site.get("properties", {})
  if isinstance(proposed_properties, dict):
    effective.update(proposed_properties)
  return effective


def _parse_positive_integer(value, name):
  if isinstance(value, bool):
    raise ValueError(f"{name} must be a positive integer")
  value_text = str(value).strip()
  if re.fullmatch(r"[0-9]+", value_text) is None or int(value_text) < 1:
    raise ValueError(f"{name} must be a positive integer")
  return int(value_text)


def _parse_boolean(value, name):
  if isinstance(value, bool):
    return value
  if isinstance(value, str):
    normalized = value.strip().lower()
    if normalized == "true":
      return True
    if normalized == "false":
      return False
  raise ValueError(f"{name} must be true or false")


def _parse_yes_no(value, name):
  if isinstance(value, str):
    normalized = value.strip().lower()
    if normalized in ("yes", "no"):
      return normalized
  raise ValueError(f"{name} must be Yes or No")


def _parse_unix_name(value, name):
  if (
    not isinstance(value, str)
    or re.fullmatch(r"[A-Za-z_][A-Za-z0-9_.-]*", value) is None
  ):
    raise ValueError(f"{name} must be a valid Unix user name")
  return value


def _timeline_service_v2_enabled(yarn_site):
  enabled = _parse_boolean(
    yarn_site.get("yarn.timeline-service.enabled", False),
    "yarn-site/yarn.timeline-service.enabled",
  )
  if not enabled:
    return False
  selected = yarn_site.get("yarn.timeline-service.versions")
  if not isinstance(selected, str) or not selected.strip():
    selected = yarn_site.get("yarn.timeline-service.version")
  if not isinstance(selected, str) or not selected.strip():
    raise ValueError("An enabled YARN timeline service requires a version")
  normalized_versions = []
  for raw_version in selected.split(","):
    normalized = raw_version.strip().lower()
    if normalized.endswith("f"):
      normalized = normalized[:-1]
    if normalized not in ("1.5", "2.0"):
      raise ValueError(f"Unsupported YARN timeline service version: {raw_version!r}")
    normalized_versions.append(normalized)
  return "2.0" in normalized_versions


def _parse_webapp_address(value, name):
  if not isinstance(value, str) or not value or value != value.strip():
    raise ValueError(f"{name} must be a host and port")
  if any(character.isspace() or ord(character) < 32 for character in value):
    raise ValueError(f"{name} must not contain whitespace")
  if value.startswith("["):
    closing_bracket = value.find("]")
    if closing_bracket < 2 or value[closing_bracket + 1 : closing_bracket + 2] != ":":
      raise ValueError(f"{name} must contain a bracketed IPv6 host and port")
    host = value[1:closing_bracket]
    if "%" in host:
      raise ValueError(f"{name} must not use a scoped IPv6 host")
    try:
      ipaddress.IPv6Address(host)
    except ipaddress.AddressValueError as error:
      raise ValueError(f"{name} contains an invalid IPv6 host") from error
    port = value[closing_bracket + 2 :]
  else:
    host, separator, port = value.rpartition(":")
    if not separator or not host or ":" in host:
      raise ValueError(f"{name} must contain a host and port")
    if re.fullmatch(r"[0-9.]+", host):
      try:
        ipaddress.IPv4Address(host)
      except ipaddress.AddressValueError as error:
        raise ValueError(f"{name} contains an invalid IPv4 host") from error
    elif re.fullmatch(
      r"(?=.{1,253}\.?\Z)(?:[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}"
      r"[A-Za-z0-9])?\.)*[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}"
      r"[A-Za-z0-9])?\.?",
      host,
      re.ASCII,
    ) is None:
      raise ValueError(f"{name} contains an invalid hostname")
  if re.fullmatch(r"[0-9]+", port) is None or not 1 <= int(port) <= 65535:
    raise ValueError(f"{name} port must be from 1 through 65535")
  return value


def _parse_cpu_percentage(value):
  if isinstance(value, bool):
    raise ValueError("YARN physical CPU limit must be from 1 through 100")
  value_text = str(value).strip()
  if re.fullmatch(r"(?:0|[1-9][0-9]*)(?:\.[0-9]+)?", value_text) is None:
    raise ValueError("YARN physical CPU limit must be from 1 through 100")
  percentage = float(value_text)
  if not math.isfinite(percentage) or percentage < 1 or percentage > 100:
    raise ValueError("YARN physical CPU limit must be from 1 through 100")
  return percentage


def _effective_kerberos_enabled(configurations, services, site_names):
  return any(
    str(
      _effective_site_properties(configurations, services, site_name).get(
        "hadoop.security.authentication", ""
      )
    ).strip().lower()
    == "kerberos"
    for site_name in site_names
  )


def _split_config_list(value):
  if not isinstance(value, str):
    return []
  return [entry.strip() for entry in value.split(",") if entry.strip()]


def _parse_capacity_scheduler_properties(site_properties):
  if not isinstance(site_properties, dict):
    return {}, True
  serialized = site_properties.get("capacity-scheduler")
  if serialized is not None and str(serialized).strip().lower() != "null":
    parsed = {}
    for line in str(serialized).splitlines():
      if not line.strip():
        continue
      key, separator, value = line.partition("=")
      if not separator or not key.strip():
        raise ValueError("Invalid serialized capacity-scheduler property")
      parsed[key.strip()] = value
    return parsed, False
  return {
    key: value
    for key, value in site_properties.items()
    if key != "capacity-scheduler"
  }, True


def _effective_capacity_scheduler_properties(configurations, services):
  existing_site = (services or {}).get("configurations", {}).get(
    "capacity-scheduler", {}
  )
  existing, existing_as_pairs = _parse_capacity_scheduler_properties(
    existing_site.get("properties", {})
  )
  proposed_site = (configurations or {}).get("capacity-scheduler")
  if not isinstance(proposed_site, dict):
    return existing, existing_as_pairs
  proposed_properties = proposed_site.get("properties", {})
  proposed, proposed_as_pairs = _parse_capacity_scheduler_properties(
    proposed_properties
  )
  if not proposed_as_pairs:
    return proposed, False
  effective = dict(existing)
  effective.update(proposed)
  return effective, existing_as_pairs


def _replace_address_host(address, hostname):
  if not isinstance(address, str) or not address.strip():
    return None
  value = address.strip()
  if value.startswith("["):
    closing_bracket = value.find("]")
    if closing_bracket < 1 or value[closing_bracket + 1 : closing_bracket + 2] != ":":
      return None
    port = value[closing_bracket + 2 :]
  else:
    _, separator, port = value.rpartition(":")
    if not separator:
      return None
  try:
    port_number = int(port)
  except (TypeError, ValueError):
    return None
  if port_number < 1 or port_number > 65535:
    return None
  rendered_host = f"[{hostname}]" if ":" in hostname else hostname
  return f"{rendered_host}:{port_number}"


def _recommended_log_server_url(configurations, services):
  yarn_site = _effective_site_properties(configurations, services, "yarn-site")
  if "yarn.log.server.web-service.url" not in yarn_site:
    return None
  policy_contract = {
    "HTTP_ONLY": ("http", "yarn.timeline-service.webapp.address"),
    "HTTPS_ONLY": ("https", "yarn.timeline-service.webapp.https.address"),
  }
  policy = yarn_site.get("yarn.http.policy")
  if policy not in policy_contract:
    return None
  scheme, address_property = policy_contract[policy]
  address = yarn_site.get(address_property)
  try:
    address = _parse_webapp_address(address, f"yarn-site/{address_property}")
  except ValueError:
    return None
  return f"{scheme}://{address}/ws/v1/applicationhistory"


class YARNServiceAdvisor(service_advisor.ServiceAdvisor):
  def __init__(self, *args, **kwargs):
    self.as_super = super(YARNServiceAdvisor, self)
    self.as_super.__init__(*args, **kwargs)

    self.initialize_logger("YARNServiceAdvisor")

    self.CLUSTER_CREATE_OPERATION = "ClusterCreate"

    self.heap_size_properties = {}
    self.notValuableComponents.add("APP_TIMELINE_SERVER")
    self.componentLayoutSchemes.update(
      {
        "APP_TIMELINE_SERVER": {31: 1, "else": 2},
      }
    )

  def getServiceComponentLayoutValidations(self, services, hosts):
    """
    Get a list of errors.
    Must be overridden in child class.
    """
    self.logger.info(
      "Class: %s, Method: %s. Validating Service Component Layout."
      % (self.__class__.__name__, inspect.stack()[0][3])
    )

    return self.getServiceComponentCardinalityValidations(services, hosts, "YARN")

  def getServiceConfigurationRecommendations(
    self, configurations, clusterData, services, hosts
  ):
    """
    Entry point.
    Must be overridden in child class.
    """
    self.logger.info(
      "Class: %s, Method: %s. Recommending Service Configurations."
      % (self.__class__.__name__, inspect.stack()[0][3])
    )

    # Apply the BIGTOP recommendation phases in dependency order.
    recommender = YARNRecommender()

    if "forced-configurations" not in services:
      services["forced-configurations"] = []

    # YARN
    recommender.recommendBigtopBaseConfigurations(
      configurations, clusterData, services, hosts
    )
    recommender.recommendBigtopSchedulerConfigurations(
      configurations, clusterData, services, hosts
    )
    recommender.recommendBigtopAuthorizationConfigurations(
      configurations, clusterData, services, hosts
    )
    recommender.recommendBigtopRuntimeConfigurations(
      configurations, clusterData, services, hosts
    )
    recommender.recommendBigtopServiceIntegrations(
      configurations, clusterData, services, hosts
    )
    recommender.recommendConfigurationsForSSO(
      configurations, clusterData, services, hosts
    )

  def getServiceConfigurationRecommendationsForSSO(
    self, configurations, clusterData, services, hosts
  ):
    """
    Entry point.
    Must be overridden in child class.
    """
    recommender = YARNRecommender()
    recommender.recommendConfigurationsForSSO(
      configurations, clusterData, services, hosts
    )

  def getServiceConfigurationsValidationItems(
    self, configurations, recommendedDefaults, services, hosts
  ):
    """
    Entry point.
    Validate configurations for the service. Return a list of errors.
    The code for this function should be the same for each Service Advisor.
    """
    self.logger.info(
      "Class: %s, Method: %s. Validating Configurations."
      % (self.__class__.__name__, inspect.stack()[0][3])
    )

    validator = YARNValidator()
    # Calls the methods of the validator using arguments,
    # method(siteProperties, siteRecommendations, configurations, services, hosts)
    return validator.validateListOfConfigUsingMethod(
      configurations, recommendedDefaults, services, hosts, validator.validators
    )

  @staticmethod
  def isKerberosEnabled(services, configurations):
    """
    Determines if security is enabled by testing the value of core-site/hadoop.security.authentication enabled.
    If the property exists and is equal to "kerberos", then is it enabled; otherwise is it assumed to be
    disabled.

    :type services: dict
    :param services: the dictionary containing the existing configuration values
    :type configurations: dict
    :param configurations: the dictionary containing the updated configuration values
    :rtype: bool
    :return: True or False
    """
    return _effective_kerberos_enabled(
      configurations, services, ("core-site",)
    )


class MAPREDUCE2ServiceAdvisor(service_advisor.ServiceAdvisor):
  def __init__(self, *args, **kwargs):
    self.as_super = super(MAPREDUCE2ServiceAdvisor, self)
    self.as_super.__init__(*args, **kwargs)

    self.heap_size_properties = {}
    self.componentLayoutSchemes.update(
      {
        "HISTORYSERVER": {31: 1, "else": 2},
      }
    )

  def getServiceComponentLayoutValidations(self, services, hosts):
    """
    Get a list of errors.
    Must be overridden in child class.
    """
    self.logger.info(
      "Class: %s, Method: %s. Validating Service Component Layout."
      % (self.__class__.__name__, inspect.stack()[0][3])
    )

    return self.getServiceComponentCardinalityValidations(services, hosts, "MAPREDUCE2")

  def getServiceConfigurationRecommendations(
    self, configurations, clusterData, services, hosts
  ):
    """
    Entry point.
    Must be overridden in child class.
    """
    self.logger.info(
      "Class: %s, Method: %s. Recommending Service Configurations."
      % (self.__class__.__name__, inspect.stack()[0][3])
    )

    # Apply the BIGTOP recommendation phases in dependency order.
    recommender = MAPREDUCE2Recommender()
    recommender.recommendBigtopMapReduceConfigurations(
      configurations, clusterData, services, hosts
    )
    recommender.recommendConfigurationsForSSO(
      configurations, clusterData, services, hosts
    )

  def getServiceConfigurationRecommendationsForSSO(
    self, configurations, clusterData, services, hosts
  ):
    """
    Entry point.
    Must be overridden in child class.
    """
    recommender = MAPREDUCE2Recommender()
    recommender.recommendConfigurationsForSSO(
      configurations, clusterData, services, hosts
    )

  def getServiceConfigurationsValidationItems(
    self, configurations, recommendedDefaults, services, hosts
  ):
    """
    Entry point.
    Validate configurations for the service. Return a list of errors.
    The code for this function should be the same for each Service Advisor.
    """
    self.logger.info(
      "Class: %s, Method: %s. Validating Configurations."
      % (self.__class__.__name__, inspect.stack()[0][3])
    )

    validator = MAPREDUCE2Validator()
    # Calls the methods of the validator using arguments,
    # method(siteProperties, siteRecommendations, configurations, services, hosts)
    return validator.validateListOfConfigUsingMethod(
      configurations, recommendedDefaults, services, hosts, validator.validators
    )


class YARNRecommender(service_advisor.ServiceAdvisor):
  """
  YARN Recommender suggests properties when adding the service for the first time or modifying configs via the UI.
  """

  def __init__(self, *args, **kwargs):
    self.as_super = super(YARNRecommender, self)
    self.as_super.__init__(*args, **kwargs)

  def recommendBigtopBaseConfigurations(
    self, configurations, clusterData, services, hosts
  ):
    """Recommend the base YARN resource and directory configuration."""
    self.logger.info(
      "Class: %s, Method: %s. Recommending Service Configurations."
      % (self.__class__.__name__, inspect.stack()[0][3])
    )

    putYarnProperty = self.putProperty(configurations, "yarn-site", services)
    putYarnPropertyAttribute = self.putPropertyAttribute(configurations, "yarn-site")
    putYarnEnvProperty = self.putProperty(configurations, "yarn-env", services)
    putContainerExecutorProperty = self.putProperty(
      configurations, "container-executor", services
    )

    self.calculateYarnAllocationSizes(configurations, services, hosts)

    putContainerExecutorProperty("min_user_id", self.get_system_min_uid())

    yarn_mount_properties = [
      ("yarn.nodemanager.local-dirs", "NODEMANAGER", "/hadoop/yarn/local", "multi"),
      ("yarn.nodemanager.log-dirs", "NODEMANAGER", "/hadoop/yarn/log", "multi"),
      (
        "yarn.timeline-service.leveldb-timeline-store.path",
        "APP_TIMELINE_SERVER",
        "/hadoop/yarn/timeline",
        "single",
      ),
      (
        "yarn.timeline-service.leveldb-state-store.path",
        "APP_TIMELINE_SERVER",
        "/hadoop/yarn/timeline",
        "single",
      ),
    ]

    self.updateMountProperties(
      "yarn-site", yarn_mount_properties, configurations, services, hosts
    )

    sc_queue_name = self.recommendYarnQueue(
      services, "yarn-env", "service_check.queue.name"
    )
    if sc_queue_name is not None:
      putYarnEnvProperty("service_check.queue.name", sc_queue_name)

    cluster_env = _effective_site_properties(configurations, services, "cluster-env")
    containerExecutorGroup = str(cluster_env.get("user_group", "hadoop")).strip()
    if not re.fullmatch(r"[A-Za-z_][A-Za-z0-9_.-]*", containerExecutorGroup):
      raise ValueError("cluster-env/user_group must be a valid group name")
    putYarnProperty(
      "yarn.nodemanager.linux-container-executor.group", containerExecutorGroup
    )

    servicesList = [
      service["StackServices"]["service_name"]
      for service in services.get("services", [])
    ]
    if "TEZ" in servicesList:
      ambari_user = self.getAmbariUser(services)
      ambariHostName = socket.getfqdn()
      putYarnProperty(
        f"yarn.timeline-service.http-authentication.proxyuser.{ambari_user}.hosts",
        ambariHostName,
      )
      putYarnProperty(
        f"yarn.timeline-service.http-authentication.proxyuser.{ambari_user}.groups", "*"
      )
      old_ambari_user = self.getOldAmbariUser(services)
      if old_ambari_user is not None:
        putYarnPropertyAttribute(
          f"yarn.timeline-service.http-authentication.proxyuser.{old_ambari_user}.hosts",
          "delete",
          "true",
        )
        putYarnPropertyAttribute(
          f"yarn.timeline-service.http-authentication.proxyuser.{old_ambari_user}.groups",
          "delete",
          "true",
        )

  def recommendBigtopSchedulerConfigurations(
    self, configurations, clusterData, services, hosts
  ):
    capacity_scheduler_properties, received_as_key_value_pair = (
      _effective_capacity_scheduler_properties(configurations, services)
    )
    putYarnProperty = self.putProperty(configurations, "yarn-site", services)
    putYarnEnvProperty = self.putProperty(configurations, "yarn-env", services)
    putCapScheProperty = self.putProperty(
      configurations, "capacity-scheduler", services
    )
    cluster_cpu_count = _parse_positive_integer(clusterData["cpu"], "cluster CPU")
    putYarnProperty("yarn.nodemanager.resource.cpu-vcores", cluster_cpu_count)
    putYarnProperty("yarn.scheduler.minimum-allocation-vcores", 1)
    putYarnProperty(
      "yarn.scheduler.maximum-allocation-vcores",
      configurations["yarn-site"]["properties"]["yarn.nodemanager.resource.cpu-vcores"],
    )
    # Property Attributes
    putYarnPropertyAttribute = self.putPropertyAttribute(configurations, "yarn-site")
    nodeManagerHost = self.getHostWithComponent("YARN", "NODEMANAGER", services, hosts)
    if nodeManagerHost is not None:
      cpuPercentageLimit = 80.0
      yarn_site = _effective_site_properties(configurations, services, "yarn-site")
      if "yarn.nodemanager.resource.percentage-physical-cpu-limit" in yarn_site:
        cpuPercentageLimit = _parse_cpu_percentage(
          yarn_site["yarn.nodemanager.resource.percentage-physical-cpu-limit"]
        )
      host_cpu_count = _parse_positive_integer(
        nodeManagerHost["Hosts"]["cpu_count"], "NodeManager CPU count"
      )
      host_total_memory = _parse_positive_integer(
        nodeManagerHost["Hosts"]["total_mem"], "NodeManager total memory"
      )
      cpuLimit = max(
        1,
        int(floor(host_cpu_count * (cpuPercentageLimit / 100.0))),
      )
      putYarnProperty("yarn.nodemanager.resource.cpu-vcores", str(cpuLimit))
      putYarnProperty(
        "yarn.scheduler.maximum-allocation-vcores",
        configurations["yarn-site"]["properties"][
          "yarn.nodemanager.resource.cpu-vcores"
        ],
      )
      putYarnPropertyAttribute(
        "yarn.nodemanager.resource.memory-mb",
        "maximum",
        int(host_total_memory / 1024),
      )  # total_mem in kb
      putYarnPropertyAttribute(
        "yarn.nodemanager.resource.cpu-vcores",
        "maximum",
        host_cpu_count * 2,
      )
      putYarnPropertyAttribute(
        "yarn.scheduler.minimum-allocation-vcores",
        "maximum",
        configurations["yarn-site"]["properties"][
          "yarn.nodemanager.resource.cpu-vcores"
        ],
      )
      putYarnPropertyAttribute(
        "yarn.scheduler.maximum-allocation-vcores",
        "maximum",
        configurations["yarn-site"]["properties"][
          "yarn.nodemanager.resource.cpu-vcores"
        ],
      )

      kerberos_authentication_enabled = YARNServiceAdvisor.isKerberosEnabled(
        services, configurations
      )
      if kerberos_authentication_enabled:
        putYarnProperty(
          "yarn.nodemanager.container-executor.class",
          "org.apache.hadoop.yarn.server.nodemanager.LinuxContainerExecutor",
        )

      container_executor = _effective_site_properties(
        configurations, services, "container-executor"
      )
      gpu_module_enabled = _parse_boolean(
        container_executor.get("gpu_module_enabled", False),
        "container-executor/gpu_module_enabled",
      )
      yarn_env = _effective_site_properties(configurations, services, "yarn-env")
      yarn_cgroups_enabled = _parse_boolean(
        yarn_env.get("yarn_cgroups_enabled", False),
        "yarn-env/yarn_cgroups_enabled",
      )
      cluster_env = _effective_site_properties(
        configurations, services, "cluster-env"
      )
      containerExecutorGroup = str(
        cluster_env.get("user_group", "hadoop")
      ).strip()
      if not re.fullmatch(r"[A-Za-z_][A-Za-z0-9_.-]*", containerExecutorGroup):
        raise ValueError("cluster-env/user_group must be a valid group name")

      if gpu_module_enabled:
        putYarnEnvProperty("yarn_cgroups_enabled", "true")
        yarn_cgroups_enabled = True

      if yarn_cgroups_enabled or self.has_multiple_resource_types(
        configurations, services
      ):
        # ResourceCalculator must switch to DominantResourceCalculator when more than resource types are involved
        # If capacity-scheduler configs are received as one concatenated string, we deposit the changed configs back as
        # one concatenated string.
        updated_cap_sched_configs_str = ""
        if not received_as_key_value_pair:
          for prop, val in capacity_scheduler_properties.items():
            if prop == "yarn.scheduler.capacity.resource-calculator":
              updated_cap_sched_configs_str = (
                updated_cap_sched_configs_str
                + prop
                + "=org.apache.hadoop.yarn.util.resource.DominantResourceCalculator\n"
              )
            elif prop.startswith("yarn.") and ".resource-calculator" not in prop:
              updated_cap_sched_configs_str = (
                updated_cap_sched_configs_str + prop + "=" + val + "\n"
              )
          putCapScheProperty("capacity-scheduler", updated_cap_sched_configs_str)
          self.logger.info(
            "Updated 'capacity-scheduler' configs as one concatenated string."
          )
        else:
          # If capacity-scheduler configs are received as a  dictionary (generally 1st time), we deposit the changed
          # values back as dictionary itself.
          # Update existing configs in 'capacity-scheduler'.
          for prop, val in capacity_scheduler_properties.items():
            if prop == "yarn.scheduler.capacity.resource-calculator":
              putCapScheProperty(
                prop, "org.apache.hadoop.yarn.util.resource.DominantResourceCalculator"
              )
            elif prop.startswith("yarn.") and ".resource-calculator" not in prop:
              putCapScheProperty(prop, val)
          self.logger.info("Updated 'capacity-scheduler' configs as a dictionary.")
      else:
        # only one resource involved in resource-type, reset resource-calculator to default
        # If capacity-scheduler configs are received as one concatenated string, we deposit the changed configs back as
        # one concatenated string.
        updated_cap_sched_configs_str = ""
        if not received_as_key_value_pair:
          for prop, val in capacity_scheduler_properties.items():
            if prop == "yarn.scheduler.capacity.resource-calculator":
              updated_cap_sched_configs_str = (
                updated_cap_sched_configs_str
                + prop
                + "=org.apache.hadoop.yarn.util.resource.DefaultResourceCalculator\n"
              )
            elif prop.startswith("yarn.") and ".resource-calculator" not in prop:
              updated_cap_sched_configs_str = (
                updated_cap_sched_configs_str + prop + "=" + val + "\n"
              )
          putCapScheProperty("capacity-scheduler", updated_cap_sched_configs_str)
          self.logger.info(
            "Updated 'capacity-scheduler' configs as one concatenated string."
          )
        else:
          # If capacity-scheduler configs are received as a  dictionary (generally 1st time), we deposit the changed
          # values back as dictionary itself.
          # Update existing configs in 'capacity-scheduler'.
          for prop, val in capacity_scheduler_properties.items():
            if prop == "yarn.scheduler.capacity.resource-calculator":
              putCapScheProperty(
                prop, "org.apache.hadoop.yarn.util.resource.DefaultResourceCalculator"
              )
            elif prop.startswith("yarn.") and ".resource-calculator" not in prop:
              putCapScheProperty(prop, val)
          self.logger.info("Updated 'capacity-scheduler' configs as a dictionary.")

      if yarn_cgroups_enabled:
        putYarnProperty(
          "yarn.nodemanager.container-executor.class",
          "org.apache.hadoop.yarn.server.nodemanager.LinuxContainerExecutor",
        )
        putYarnProperty(
          "yarn.nodemanager.linux-container-executor.group", containerExecutorGroup
        )
        putYarnProperty("yarn.nodemanager.resource.cpu.enabled", "true")
        putYarnProperty(
          "yarn.nodemanager.linux-container-executor.cgroups.hierarchy", "/yarn"
        )
        putYarnPropertyAttribute(
          "yarn.nodemanager.linux-container-executor.resources-handler.class",
          "delete",
          "true",
        )
        putYarnProperty(
          "yarn.nodemanager.linux-container-executor.cgroups.mount", "false"
        )
        putYarnProperty(
          "yarn.nodemanager.linux-container-executor.cgroups.mount-path",
          "/sys/fs/cgroup",
        )
      else:
        if not kerberos_authentication_enabled:
          putYarnProperty(
            "yarn.nodemanager.container-executor.class",
            "org.apache.hadoop.yarn.server.nodemanager.DefaultContainerExecutor",
          )
        putYarnPropertyAttribute(
          "yarn.nodemanager.linux-container-executor.resources-handler.class",
          "delete",
          "true",
        )
        putYarnProperty("yarn.nodemanager.resource.cpu.enabled", "false")
        putYarnPropertyAttribute(
          "yarn.nodemanager.linux-container-executor.cgroups.hierarchy",
          "delete",
          "true",
        )
        putYarnPropertyAttribute(
          "yarn.nodemanager.linux-container-executor.cgroups.mount", "delete", "true"
        )
        putYarnPropertyAttribute(
          "yarn.nodemanager.linux-container-executor.cgroups.mount-path",
          "delete",
          "true",
        )

  def has_multiple_resource_types(self, configurations, services):
    resource_types = _effective_site_properties(
      configurations, services, "resource-types"
    )
    return bool(_split_config_list(resource_types.get("yarn.resource-types")))

  def recommendBigtopAuthorizationConfigurations(
    self, configurations, clusterData, services, hosts
  ):
    putYarnSiteProperty = self.putProperty(configurations, "yarn-site", services)
    putYarnSitePropertyAttributes = self.putPropertyAttribute(
      configurations, "yarn-site"
    )

    ranger_env = _effective_site_properties(configurations, services, "ranger-env")
    ranger_plugin = _effective_site_properties(
      configurations, services, "ranger-yarn-plugin-properties"
    )
    ranger_env_value = ranger_env.get("ranger-yarn-plugin-enabled")
    if ranger_env_value is not None:
      ranger_env_enabled = _parse_yes_no(
        ranger_env_value, "ranger-env/ranger-yarn-plugin-enabled"
      )
      putYarnRangerPluginProperty = self.putProperty(
        configurations, "ranger-yarn-plugin-properties", services
      )
      putYarnRangerPluginProperty(
        "ranger-yarn-plugin-enabled", ranger_env_enabled.title()
      )
      ranger_plugin_enabled = ranger_env_enabled
    else:
      ranger_plugin_enabled = _parse_yes_no(
        ranger_plugin.get("ranger-yarn-plugin-enabled", "no"),
        "ranger-yarn-plugin-properties/ranger-yarn-plugin-enabled",
      )

    if ranger_plugin_enabled == "yes":
      putYarnSiteProperty("yarn.acl.enable", "true")
      putYarnSiteProperty(
        "yarn.authorization-provider",
        "org.apache.ranger.authorization.yarn.authorizer.RangerYarnAuthorizer",
      )
    else:
      putYarnSitePropertyAttributes("yarn.authorization-provider", "delete", "true")

  def recommendBigtopRuntimeConfigurations(
    self, configurations, clusterData, services, hosts
  ):
    putYarnSiteProperty = self.putProperty(configurations, "yarn-site", services)
    putYarnEnvProperty = self.putProperty(configurations, "yarn-env", services)
    putResTypsProperty = self.putProperty(configurations, "resource-types", services)
    putCapScheProperty = self.putProperty(
      configurations, "capacity-scheduler", services
    )
    putCanExecProperty = self.putProperty(
      configurations, "container-executor", services
    )

    yarn_site = _effective_site_properties(configurations, services, "yarn-site")
    if "yarn.resourcemanager.scheduler.monitor.enable" in yarn_site:
      scheduler_monitor_enabled = yarn_site[
        "yarn.resourcemanager.scheduler.monitor.enable"
      ]
      if _parse_boolean(
        scheduler_monitor_enabled,
        "yarn-site/yarn.resourcemanager.scheduler.monitor.enable",
      ):
        putYarnSiteProperty(
          "yarn.scheduler.capacity.ordering-policy.priority-utilization.underutilized-preemption.enabled",
          "true",
        )
      else:
        putYarnSiteProperty(
          "yarn.scheduler.capacity.ordering-policy.priority-utilization.underutilized-preemption.enabled",
          "false",
        )

    # calculate total_preemption_per_round
    total_preemption_per_round = self.calculate_total_preemption_per_round(hosts)
    putYarnSiteProperty(
      "yarn.resourcemanager.monitor.capacity.preemption.total_preemption_per_round",
      total_preemption_per_round,
    )

    yarn_env = _effective_site_properties(configurations, services, "yarn-env")
    yarn_user = yarn_env.get("yarn_user", "yarn")
    ranger_yarn = _effective_site_properties(
      configurations, services, "ranger-yarn-plugin-properties"
    )
    ranger_yarn_plugin_enabled = (
      str(ranger_yarn.get("ranger-yarn-plugin-enabled", "")).strip().lower()
      == "yes"
    )

    webservice_url = _recommended_log_server_url(configurations, services)
    if webservice_url is not None:
      putYarnSiteProperty("yarn.log.server.web-service.url", webservice_url)

    if (
      ranger_yarn_plugin_enabled and "REPOSITORY_CONFIG_USERNAME" in ranger_yarn
    ):
      self.logger.info("Setting Yarn Repo user for Ranger.")
      putRangerYarnPluginProperty = self.putProperty(
        configurations, "ranger-yarn-plugin-properties", services
      )
      putRangerYarnPluginProperty("REPOSITORY_CONFIG_USERNAME", yarn_user)
    else:
      self.logger.info("Not setting Yarn Repo user for Ranger.")

    yarn_timeline_app_cache_size = None
    host_mem = self.get_host_memory_mb(hosts)
    # Check if 'yarn.timeline-service.entity-group-fs-store.app-cache-size' in changed configs.
    changed_configs_has_ats_cache_size = self.isConfigPropertiesChanged(
      services,
      "yarn-site",
      ["yarn.timeline-service.entity-group-fs-store.app-cache-size"],
      False,
    )
    # Check if it's : 1. 'apptimelineserver_heapsize' changed detected in changed-configurations)
    # OR 2. cluster initialization (changed-configurations is empty in this case)
    if changed_configs_has_ats_cache_size:
      yarn_timeline_app_cache_size = self.read_yarn_apptimelineserver_cache_size(
        configurations, services
      )
    elif not services.get("changed-configurations"):
      # Fetch host memory from 1st host, to be used for ATS config calculations below.
      if host_mem is not None:
        yarn_timeline_app_cache_size = self.calculate_yarn_apptimelineserver_cache_size(
          host_mem
        )
        putYarnSiteProperty(
          "yarn.timeline-service.entity-group-fs-store.app-cache-size",
          yarn_timeline_app_cache_size,
        )
        self.logger.info(
          "Updated YARN config 'yarn.timeline-service.entity-group-fs-store.app-cache-size' as : {0}, "
          "using 'host_mem' = {1}".format(yarn_timeline_app_cache_size, host_mem)
        )
      else:
        self.logger.info(
          "Couldn't update YARN config 'yarn.timeline-service.entity-group-fs-store.app-cache-size' as "
          "'host_mem' read = {0}".format(host_mem)
        )

    if yarn_timeline_app_cache_size is not None and host_mem is not None:
      # Calculation for 'ats_heapsize' is in MB.
      ats_heapsize = self.calculate_yarn_apptimelineserver_heapsize(
        host_mem, yarn_timeline_app_cache_size
      )
      putYarnEnvProperty("apptimelineserver_heapsize", ats_heapsize)  # Value in MB
      self.logger.info(
        f"Updated YARN config 'apptimelineserver_heapsize' as : {ats_heapsize}, "
      )

    resource_types = _effective_site_properties(
      configurations, services, "resource-types"
    )
    yarn_env = _effective_site_properties(configurations, services, "yarn-env")
    container_executor = _effective_site_properties(
      configurations, services, "container-executor"
    )
    yarn_site = _effective_site_properties(configurations, services, "yarn-site")

    restyps_list = _split_config_list(resource_types.get("yarn.resource-types"))
    gpu_module_enabled = container_executor.get("gpu_module_enabled")
    docker_module_enabled = container_executor.get("docker_module_enabled")
    allow_dev_list = _split_config_list(
      container_executor.get("docker_allowed_devices")
    )
    allow_vol_drive_list = _split_config_list(
      container_executor.get("docker_allowed_volume-drivers")
    )
    allow_romounts_list = _split_config_list(
      container_executor.get("docker_allowed_ro-mounts")
    )
    rp_gpu_agd_list = _split_config_list(
      yarn_site.get("yarn.nodemanager.resource-plugins.gpu.allowed-gpu-devices")
    )
    rp_gpu_dp_list = _split_config_list(
      yarn_site.get("yarn.nodemanager.resource-plugins.gpu.docker-plugin")
    )
    rp_gpu_dp_nv1_ep_list = _split_config_list(
      yarn_site.get(
        "yarn.nodemanager.resource-plugins.gpu.docker-plugin."
        "nvidia-docker-v1.endpoint"
      )
    )

    self.logger.info(f"Effective YARN resource types: {restyps_list}")
    self.logger.info(
      f"Effective YARN cgroup state: {yarn_env.get('yarn_cgroups_enabled')}"
    )

    if _parse_boolean(
      False if gpu_module_enabled is None else gpu_module_enabled,
      "container-executor/gpu_module_enabled",
    ):
      # put yarn.io/gpu if it is absent in resource-types.xml
      if "yarn.resource-types" in resource_types:
        if "yarn.io/gpu" in restyps_list:
          self.logger.info("GPU types already in resource-types.")
        else:
          restyps_list.append("yarn.io/gpu")
          yarn_restyps = ",".join(str(x) for x in restyps_list)
          putResTypsProperty("yarn.resource-types", yarn_restyps)
      # auto fill gpu related property values in yarn-site
      yarn_restyps = ",".join(str(x) for x in restyps_list)
      putYarnSiteProperty("yarn.nodemanager.resource-plugins", yarn_restyps)

      if "auto" in rp_gpu_agd_list:
        self.logger.info(
          "allowed gpu devices already in resource-plugins.gpu.allowed-gpu-devices"
        )
      else:
        rp_gpu_agd_list.append("auto")
        rp_gpu_agd = ",".join(str(x) for x in rp_gpu_agd_list)
        putYarnSiteProperty(
          "yarn.nodemanager.resource-plugins.gpu.allowed-gpu-devices", rp_gpu_agd
        )

      # yarn_hierarchy should always have same value of yarn.nodemanager.linux-container-executor.cgroups.hierarchy
      putCanExecProperty("yarn_hierarchy", "/yarn")
      # cgroup_root should always have same value of yarn.nodemanager.linux-container-executor.cgroups.mount-path
      putCanExecProperty("cgroup_root", "/sys/fs/cgroup")

      if _parse_boolean(
        False if docker_module_enabled is None else docker_module_enabled,
        "container-executor/docker_module_enabled",
      ):
        if "nvidia-docker-v1" in rp_gpu_dp_list:
          self.logger.info(
            "nvidia gpu docker plugin already in resource-plugins.gpu.docker-plugin"
          )
        else:
          rp_gpu_dp_list.append("nvidia-docker-v1")
          rp_gpu_dp = ",".join(str(x) for x in rp_gpu_dp_list)
          putYarnSiteProperty(
            "yarn.nodemanager.resource-plugins.gpu.docker-plugin", rp_gpu_dp
          )

        if "http://localhost:3476/v1.0/docker/cli" in rp_gpu_dp_nv1_ep_list:
          self.logger.info(
            "nvidia gpu docker plugin endpoint already in resource-plugins.gpu.docker-plugin.nvidia-docker-v1.endpoint"
          )
        else:
          rp_gpu_dp_nv1_ep_list.append("http://localhost:3476/v1.0/docker/cli")
          rp_gpu_dp_nv1_ep = ",".join(str(x) for x in rp_gpu_dp_nv1_ep_list)
          putYarnSiteProperty(
            "yarn.nodemanager.resource-plugins.gpu.docker-plugin.nvidia-docker-v1.endpoint",
            rp_gpu_dp_nv1_ep,
          )

        # add gpu related devices if it is absent in docker section
        if "regex:^/dev/nvidia.*$" in allow_dev_list:
          self.logger.info("gpu related devices already in docker.allowed.devices.")
        else:
          allow_dev_list.append("regex:^/dev/nvidia.*$")
          docker_allow_dev = ",".join(str(x) for x in allow_dev_list)
          putCanExecProperty("docker_allowed_devices", docker_allow_dev)

        # add nvidia-docker if it is absent in docker allowed volume-drivers
        if "nvidia-docker" in allow_vol_drive_list:
          self.logger.info("nvidia-docker already in docker_allowed_volume-drivers.")
        else:
          allow_vol_drive_list.append("nvidia-docker")
          docker_allow_vol_drive = ",".join(str(x) for x in allow_vol_drive_list)
          putCanExecProperty("docker_allowed_volume-drivers", docker_allow_vol_drive)

        # add nvidia_driver_<version> if it is absent in docker allowed ro-mounts
        if "regex:^nvidia_driver_.*$" in allow_romounts_list:
          self.logger.info("nvidia_driver_<version> already in allow_romounts_list.")
        else:
          allow_romounts_list.append("regex:^nvidia_driver_.*$")
          docker_allow_romounts = ",".join(str(x) for x in allow_romounts_list)
          putCanExecProperty("docker_allowed_ro-mounts", docker_allow_romounts)

      # gpu_module_enabled is true and docker_module_enabled is false
      else:
        # revert gpu docker related settings only
        # yarn-site
        if "nvidia-docker-v1" in rp_gpu_dp_list:
          rp_gpu_dp_list.remove("nvidia-docker-v1")
          rp_gpu_dp = ",".join(str(x) for x in rp_gpu_dp_list)
          putYarnSiteProperty(
            "yarn.nodemanager.resource-plugins.gpu.docker-plugin", rp_gpu_dp
          )

        if "http://localhost:3476/v1.0/docker/cli" in rp_gpu_dp_nv1_ep_list:
          rp_gpu_dp_nv1_ep_list.remove("http://localhost:3476/v1.0/docker/cli")
          rp_gpu_dp_nv1_ep = ",".join(str(x) for x in rp_gpu_dp_nv1_ep_list)
          putYarnSiteProperty(
            "yarn.nodemanager.resource-plugins.gpu.docker-plugin.nvidia-docker-v1.endpoint",
            rp_gpu_dp_nv1_ep,
          )

        # container-executor
        if "regex:^/dev/nvidia.*$" in allow_dev_list:
          allow_dev_list.remove("regex:^/dev/nvidia.*$")
          docker_allow_dev = ",".join(str(x) for x in allow_dev_list)
          putCanExecProperty("docker_allowed_devices", docker_allow_dev)

        if "nvidia-docker" in allow_vol_drive_list:
          allow_vol_drive_list.remove("nvidia-docker")
          docker_allow_vol_drive = ",".join(str(x) for x in allow_vol_drive_list)
          putCanExecProperty("docker_allowed_volume-drivers", docker_allow_vol_drive)

        if "regex:^nvidia_driver_.*$" in allow_romounts_list:
          allow_romounts_list.remove("regex:^nvidia_driver_.*$")
          docker_allow_romounts = ",".join(str(x) for x in allow_romounts_list)
          putCanExecProperty("docker_allowed_ro-mounts", docker_allow_romounts)

    # gpu_module_enabled is false, we will revert all gpu settings no matter
    # docker_module_enabled is true or false.
    else:
      # auto revert gpu related property values when gpu is disabled
      # revert gpu types from resource-types.xml
      if "yarn.io/gpu" in restyps_list:
        restyps_list.remove("yarn.io/gpu")
        yarn_restyps = ",".join(str(x) for x in restyps_list)
        putResTypsProperty("yarn.resource-types", yarn_restyps)

      if "auto" in rp_gpu_agd_list:
        rp_gpu_agd_list.remove("auto")
        rp_gpu_agd = ",".join(str(x) for x in rp_gpu_agd_list)
        putYarnSiteProperty(
          "yarn.nodemanager.resource-plugins.gpu.allowed-gpu-devices", rp_gpu_agd
        )

      if "nvidia-docker-v1" in rp_gpu_dp_list:
        rp_gpu_dp_list.remove("nvidia-docker-v1")
        rp_gpu_dp = ",".join(str(x) for x in rp_gpu_dp_list)
        putYarnSiteProperty(
          "yarn.nodemanager.resource-plugins.gpu.docker-plugin", rp_gpu_dp
        )

      if "http://localhost:3476/v1.0/docker/cli" in rp_gpu_dp_nv1_ep_list:
        rp_gpu_dp_nv1_ep_list.remove("http://localhost:3476/v1.0/docker/cli")
        rp_gpu_dp_nv1_ep = ",".join(str(x) for x in rp_gpu_dp_nv1_ep_list)
        putYarnSiteProperty(
          "yarn.nodemanager.resource-plugins.gpu.docker-plugin.nvidia-docker-v1.endpoint",
          rp_gpu_dp_nv1_ep,
        )

      # yarn_hierarchy should always have same value of yarn.nodemanager.linux-container-executor.cgroups.hierarchy
      putCanExecProperty("yarn_hierarchy", "")
      # cgroup_root should always have same value of yarn.nodemanager.linux-container-executor.cgroups.mount-path
      putCanExecProperty("cgroup_root", "")

      # revert docker related settings from docker section
      if "regex:^/dev/nvidia.*$" in allow_dev_list:
        allow_dev_list.remove("regex:^/dev/nvidia.*$")
        docker_allow_dev = ",".join(str(x) for x in allow_dev_list)
        putCanExecProperty("docker_allowed_devices", docker_allow_dev)

      if "nvidia-docker" in allow_vol_drive_list:
        allow_vol_drive_list.remove("nvidia-docker")
        docker_allow_vol_drive = ",".join(str(x) for x in allow_vol_drive_list)
        putCanExecProperty("docker_allowed_volume-drivers", docker_allow_vol_drive)

      if "regex:^nvidia_driver_.*$" in allow_romounts_list:
        allow_romounts_list.remove("regex:^nvidia_driver_.*$")
        docker_allow_romounts = ",".join(str(x) for x in allow_romounts_list)
        putCanExecProperty("docker_allowed_ro-mounts", docker_allow_romounts)

  @staticmethod
  def calculate_total_preemption_per_round(hosts):
    host_count = len((hosts or {}).get("items", []))
    return str(round(max(1.0 / host_count, 0.1), 2)) if host_count else "0.1"

  def recommendBigtopServiceIntegrations(
    self, configurations, clusterData, services, hosts
  ):
    putYarnSiteProperty = self.putProperty(configurations, "yarn-site", services)
    putCapSchedProperty = self.putProperty(
      configurations, "capacity-scheduler", services
    )

    self.update_timeline_reader_address(
      configurations, services, "yarn.timeline-service.reader.webapp.address"
    )
    self.update_timeline_reader_address(
      configurations, services, "yarn.timeline-service.reader.webapp.https.address"
    )

    hive_env_properties = _effective_site_properties(
      configurations, services, "hive-env"
    )
    cap_sched_properties, received_as_key_value_pair = (
      _effective_capacity_scheduler_properties(configurations, services)
    )
    if (
      hive_env_properties
      and "hive_user" in hive_env_properties
      and cap_sched_properties
      and "yarn.scheduler.capacity.root.acl_administer_queue" in cap_sched_properties
    ):
      hive_user = _parse_unix_name(
        hive_env_properties["hive_user"], "hive-env/hive_user"
      )
      acl_administer_queue = cap_sched_properties[
        "yarn.scheduler.capacity.root.acl_administer_queue"
      ]
      acl_administer_queue_items = acl_administer_queue.split(",")
      if not (
        "*" in acl_administer_queue_items or hive_user in acl_administer_queue_items
      ):
        if not received_as_key_value_pair:
          updated_cap_sched_configs_str = ""
          for prop, val in cap_sched_properties.items():
            if prop == "yarn.scheduler.capacity.root.acl_administer_queue":
              updated_cap_sched_configs_str = (
                updated_cap_sched_configs_str
                + prop
                + "="
                + acl_administer_queue
                + ","
                + hive_user
                + "\n"
              )
            elif prop:
              updated_cap_sched_configs_str = (
                updated_cap_sched_configs_str + prop + "=" + val + "\n"
              )

          putCapSchedProperty("capacity-scheduler", updated_cap_sched_configs_str)
        else:
          putCapSchedProperty(
            "yarn.scheduler.capacity.root.acl_administer_queue",
            acl_administer_queue + "," + hive_user,
          )

    cap_sched_properties, received_as_key_value_pair = (
      _effective_capacity_scheduler_properties(configurations, services)
    )
    spark_env_properties = _effective_site_properties(
      configurations, services, "spark-env"
    )
    if (
      spark_env_properties
      and "spark_user" in spark_env_properties
      and cap_sched_properties
      and "yarn.scheduler.capacity.root.acl_administer_queue" in cap_sched_properties
    ):
      spark_user = _parse_unix_name(
        spark_env_properties["spark_user"], "spark-env/spark_user"
      )
      acl_administer_queue = cap_sched_properties[
        "yarn.scheduler.capacity.root.acl_administer_queue"
      ]
      acl_administer_queue_items = acl_administer_queue.split(",")
      if not (
        "*" in acl_administer_queue_items or spark_user in acl_administer_queue_items
      ):
        if not received_as_key_value_pair:
          updated_cap_sched_configs_str = ""
          for prop, val in cap_sched_properties.items():
            if prop == "yarn.scheduler.capacity.root.acl_administer_queue":
              updated_cap_sched_configs_str = (
                updated_cap_sched_configs_str
                + prop
                + "="
                + acl_administer_queue
                + ","
                + spark_user
                + "\n"
              )
            elif prop:
              updated_cap_sched_configs_str = (
                updated_cap_sched_configs_str + prop + "=" + val + "\n"
              )

          putCapSchedProperty("capacity-scheduler", updated_cap_sched_configs_str)
        else:
          putCapSchedProperty(
            "yarn.scheduler.capacity.root.acl_administer_queue",
            acl_administer_queue + "," + spark_user,
          )

    # auto detect whether system service launch is required or not
    # Set is_hbase_system_service_launch flag based on number of NM and cluster capacity.
    # (1). if each NM capacity is greater than 10GB and cluster capacity greater than 50GB
    yarn_hbase_env = _effective_site_properties(
      configurations, services, "yarn-hbase-env"
    )
    if "is_hbase_system_service_launch" in yarn_hbase_env:
      putYarnHBaseEnv = self.putProperty(configurations, "yarn-hbase-env", services)
      uses_external_hbase = any(
        _parse_boolean(
          yarn_hbase_env.get(property_name, False),
          f"yarn-hbase-env/{property_name}",
        )
        for property_name in ("use_external_hbase", "hbase_within_cluster")
      )
      if uses_external_hbase:
        putYarnHBaseEnv("is_hbase_system_service_launch", "false")
        return

      yarn_site = _effective_site_properties(
        configurations, services, "yarn-site"
      )
      if not _timeline_service_v2_enabled(yarn_site):
        putYarnHBaseEnv("is_hbase_system_service_launch", "false")
        return
      timeline_reader_hosts = self.getHostsForComponent(
        services, "YARN", "TIMELINE_READER"
      )
      if not timeline_reader_hosts:
        putYarnHBaseEnv("is_hbase_system_service_launch", "false")
        return

      node_manager_host_list = self.getHostsForComponent(
        services, "YARN", "NODEMANAGER"
      )
      node_manager_cnt = len(node_manager_host_list)
      yarn_nm_mem_in_mb = self.get_yarn_nm_mem_in_mb(services, configurations)
      total_cluster_capacity = node_manager_cnt * yarn_nm_mem_in_mb
      if yarn_nm_mem_in_mb >= 10240 and total_cluster_capacity >= 51200:
        putYarnHBaseEnv("is_hbase_system_service_launch", "true")
      # Do not set to false in else

  def recommendConfigurationsForSSO(self, configurations, clusterData, services, hosts):
    ambari_configuration = self.get_ambari_configuration(services)
    ambari_sso_details = (
      ambari_configuration.get_ambari_sso_details() if ambari_configuration else None
    )

    if ambari_sso_details and ambari_sso_details.is_managing_services():
      putYarnSiteProperty = self.putProperty(configurations, "yarn-site", services)

      # If SSO should be enabled for this service
      if ambari_sso_details.should_enable_sso("YARN"):
        if self.is_kerberos_enabled(configurations, services):
          putYarnSiteProperty(
            "hadoop.http.authentication.type",
            "org.apache.hadoop.security.authentication.server.JWTRedirectAuthenticationHandler",
          )
          putYarnSiteProperty(
            "hadoop.http.authentication.authentication.provider.url",
            ambari_sso_details.get_sso_provider_url(),
          )
          putYarnSiteProperty(
            "hadoop.http.authentication.public.key.pem",
            ambari_sso_details.get_sso_provider_certificate(False, True),
          )
        else:
          # Since Kerberos is not enabled, we can not enable SSO
          self.logger.warning(
            "Enabling SSO integration for Yarn requires Kerberos, Since Kerberos is not enabled, SSO integration is not being recommended."
          )
          putYarnSiteProperty("hadoop.http.authentication.type", "simple")

      # If SSO should be disabled for this service
      elif ambari_sso_details.should_disable_sso("YARN"):
        if self.is_kerberos_enabled(configurations, services):
          putYarnSiteProperty("hadoop.http.authentication.type", "kerberos")
        else:
          putYarnSiteProperty("hadoop.http.authentication.type", "simple")

  def is_kerberos_enabled(self, configurations, services):
    return _effective_kerberos_enabled(
      configurations, services, ("yarn-site", "core-site")
    )

  """
  Calculate YARN config 'apptimelineserver_heapsize' in MB.
  """

  def calculate_yarn_apptimelineserver_heapsize(
    self, host_mem, yarn_timeline_app_cache_size
  ):
    try:
      host_mem = float(host_mem)
      yarn_timeline_app_cache_size = int(
        str(yarn_timeline_app_cache_size).strip()
      )
    except (TypeError, ValueError) as error:
      raise ValueError("ATS host memory and application cache size must be numeric") from error
    if (
      not math.isfinite(host_mem)
      or host_mem <= 0
      or yarn_timeline_app_cache_size <= 0
    ):
      raise ValueError("ATS host memory and application cache size must be positive")

    if host_mem < 4096:
      ats_heapsize = 1024
    else:
      ats_heapsize = int(
        min(math.floor(host_mem / 2), int(yarn_timeline_app_cache_size) * 500 + 3072)
      )
    return ats_heapsize

  """
  Calculates for YARN config 'yarn.timeline-service.entity-group-fs-store.app-cache-size', based on YARN's NodeManager size.
  """

  def calculate_yarn_apptimelineserver_cache_size(self, host_mem):
    try:
      host_mem = float(host_mem)
    except (TypeError, ValueError) as error:
      raise ValueError("ATS host memory must be numeric") from error
    if not math.isfinite(host_mem) or host_mem <= 0:
      raise ValueError("ATS host memory must be positive")

    if host_mem < 4096:
      yarn_timeline_app_cache_size = 3
    elif host_mem >= 4096 and host_mem < 8192:
      yarn_timeline_app_cache_size = 7
    elif host_mem >= 8192:
      yarn_timeline_app_cache_size = 10
    self.logger.info(
      f"Calculated and returning 'yarn_timeline_app_cache_size' : {yarn_timeline_app_cache_size}"
    )
    return yarn_timeline_app_cache_size

  def get_host_memory_mb(self, hosts):
    """Return the first valid host memory value converted from KB to MB."""
    for host in (hosts or {}).get("items", []):
      total_mem = (host.get("Hosts") or {}).get("total_mem")
      try:
        total_mem = float(total_mem)
      except (TypeError, ValueError):
        continue
      if math.isfinite(total_mem) and total_mem > 0:
        return total_mem / 1024.0
    self.logger.warning("No valid host memory is available for ATS recommendations")
    return None

  """
  Reads YARN config 'yarn.timeline-service.entity-group-fs-store.app-cache-size'.
  """

  def read_yarn_apptimelineserver_cache_size(self, configurations, services):
    """
    :type services dict
    :rtype str
    """
    yarn_ats_app_cache_size = None
    yarn_ats_app_cache_size_config = (
      "yarn.timeline-service.entity-group-fs-store.app-cache-size"
    )
    yarn_site_in_services = _effective_site_properties(
      configurations, services, "yarn-site"
    )

    if (
      yarn_site_in_services and yarn_ats_app_cache_size_config in yarn_site_in_services
    ):
      raw_cache_size = yarn_site_in_services[yarn_ats_app_cache_size_config]
      try:
        yarn_ats_app_cache_size = int(str(raw_cache_size).strip())
      except (TypeError, ValueError) as error:
        raise ValueError(
          f"{yarn_ats_app_cache_size_config} must be a positive integer"
        ) from error
      if yarn_ats_app_cache_size <= 0:
        raise ValueError(
          f"{yarn_ats_app_cache_size_config} must be a positive integer"
        )
      self.logger.info(
        f"'{yarn_ats_app_cache_size_config}' read from services as: {yarn_ats_app_cache_size}"
      )

    if not yarn_ats_app_cache_size:
      self.logger.error(
        f"'{yarn_ats_app_cache_size_config}' was not found in the services"
      )

    return yarn_ats_app_cache_size

  def update_timeline_reader_address(self, configurations, services, property_name):
    putYarnProperty = self.putProperty(configurations, "yarn-site", services)
    yarn_site = _effective_site_properties(configurations, services, "yarn-site")
    if yarn_site and property_name in yarn_site:
      timeline_hosts = self.getHostsForComponent(services, "YARN", "TIMELINE_READER")
      old_address = yarn_site[property_name]
      if len(timeline_hosts) == 1:
        new_address = _replace_address_host(old_address, timeline_hosts[0])
        if old_address != new_address:
          if new_address is None:
            raise ValueError(f"Invalid YARN timeline reader address: {old_address}")
          putYarnProperty(property_name, new_address)
          self.logger.info(f"Updated YARN config {property_name} to {new_address}")

  def get_yarn_nm_mem_in_mb(self, services, configurations):
    """Return the effective NodeManager resource memory in MB."""
    yarn_site = _effective_site_properties(configurations, services, "yarn-site")
    value = yarn_site.get("yarn.nodemanager.resource.memory-mb")
    try:
      yarn_nm_mem_in_mb = float(value)
    except (TypeError, ValueError) as error:
      raise ValueError(
        "yarn.nodemanager.resource.memory-mb must be numeric"
      ) from error
    if not math.isfinite(yarn_nm_mem_in_mb) or yarn_nm_mem_in_mb <= 0.0:
      raise ValueError("yarn.nodemanager.resource.memory-mb must be positive")
    return yarn_nm_mem_in_mb


class MAPREDUCE2Recommender(YARNRecommender):
  """
  MAPREDUCE2 Recommender suggests properties when adding the service for the first time or modifying configs via the UI.
  """

  def __init__(self, *args, **kwargs):
    self.as_super = super(MAPREDUCE2Recommender, self)
    self.as_super.__init__(*args, **kwargs)

  def recommendBigtopMapReduceConfigurations(
    self, configurations, clusterData, services, hosts
  ):
    putMapredProperty = self.putProperty(configurations, "mapred-site", services)
    yarn_site = dict(self.getServicesSiteProperties(services, "yarn-site") or {})
    yarn_site.update(
      (configurations.get("yarn-site") or {}).get("properties") or {}
    )
    required_yarn_properties = (
      "yarn.scheduler.minimum-allocation-mb",
      "yarn.scheduler.maximum-allocation-mb",
    )
    missing_yarn_properties = [
      property_name
      for property_name in required_yarn_properties
      if property_name not in yarn_site
    ]
    if missing_yarn_properties:
      raise ValueError(
        "MAPREDUCE2 recommendations require YARN allocation properties: "
        + ", ".join(missing_yarn_properties)
      )
    yarn_min_allocation = int(yarn_site[required_yarn_properties[0]])
    yarn_configured_max = int(yarn_site[required_yarn_properties[1]])
    if yarn_min_allocation <= 0 or yarn_configured_max < yarn_min_allocation:
      raise ValueError("YARN allocation bounds must be positive and ordered")
    ram_per_container = int(clusterData["ramPerContainer"])
    if ram_per_container <= 0:
      raise ValueError("ramPerContainer must be positive")

    mapred_mounts = [
      ("mapred.local.dir", ["NODEMANAGER"], "/hadoop/mapred", "multi")
    ]
    self.updateMountProperties(
      "mapred-site", mapred_mounts, configurations, services, hosts
    )

    nodemanager_min_ram = 1048576  # 1TB in MB
    if "referenceNodeManagerHost" in clusterData:
      reference_total_mem = float(
        clusterData["referenceNodeManagerHost"]["total_mem"]
      )
      if not math.isfinite(reference_total_mem) or reference_total_mem <= 0:
        raise ValueError("referenceNodeManagerHost.total_mem must be positive")
      nodemanager_min_ram = min(reference_total_mem / 1024, nodemanager_min_ram)

    am_memory = min(
      max(ram_per_container, yarn_min_allocation), yarn_configured_max
    )
    putMapredProperty("yarn.app.mapreduce.am.resource.mb", am_memory)
    putMapredProperty(
      "yarn.app.mapreduce.am.command-opts",
      f"-Xmx{int(0.8 * am_memory)}m",
    )
    servicesList = [
      service["StackServices"]["service_name"]
      for service in services.get("services", [])
    ]
    min_mapreduce_map_memory_mb = 0
    min_mapreduce_reduce_memory_mb = 0
    min_mapreduce_map_java_opts = 0
    if ("PIG" in servicesList) and clusterData.get("totalAvailableRam", 0) >= 4096:
      min_mapreduce_map_memory_mb = 1536
      min_mapreduce_reduce_memory_mb = 1536
      min_mapreduce_map_java_opts = 1024

    map_memory = min(
      yarn_configured_max,
      max(min_mapreduce_map_memory_mb, ram_per_container, yarn_min_allocation),
    )
    reduce_memory = min(
      yarn_configured_max,
      max(
        min_mapreduce_reduce_memory_mb,
        yarn_min_allocation,
        min(2 * ram_per_container, int(nodemanager_min_ram)),
      ),
    )
    putMapredProperty("mapreduce.map.memory.mb", map_memory)
    putMapredProperty("mapreduce.reduce.memory.mb", reduce_memory)

    mapredMapXmx = int(0.8 * map_memory)
    putMapredProperty(
      "mapreduce.map.java.opts",
      "-Xmx" + str(max(min_mapreduce_map_java_opts, mapredMapXmx)) + "m",
    )
    putMapredProperty(
      "mapreduce.reduce.java.opts",
      f"-Xmx{int(0.8 * reduce_memory)}m",
    )
    putMapredProperty(
      "mapreduce.task.io.sort.mb", str(min(int(0.7 * mapredMapXmx), 2047))
    )
    # Property Attributes
    putMapredPropertyAttribute = self.putPropertyAttribute(
      configurations, "mapred-site"
    )
    yarnMinAllocationSize = yarn_min_allocation
    yarnMaxAllocationSize = min(30 * yarn_min_allocation, yarn_configured_max)
    putMapredPropertyAttribute(
      "mapreduce.map.memory.mb", "maximum", yarnMaxAllocationSize
    )
    putMapredPropertyAttribute(
      "mapreduce.map.memory.mb", "minimum", yarnMinAllocationSize
    )
    putMapredPropertyAttribute(
      "mapreduce.reduce.memory.mb", "maximum", yarnMaxAllocationSize
    )
    putMapredPropertyAttribute(
      "mapreduce.reduce.memory.mb", "minimum", yarnMinAllocationSize
    )
    putMapredPropertyAttribute(
      "yarn.app.mapreduce.am.resource.mb", "maximum", yarnMaxAllocationSize
    )
    putMapredPropertyAttribute(
      "yarn.app.mapreduce.am.resource.mb", "minimum", yarnMinAllocationSize
    )
    # Hadoop MR limitation
    putMapredPropertyAttribute("mapreduce.task.io.sort.mb", "maximum", "2047")

    mr_queue = self.recommendYarnQueue(
      services, "mapred-site", "mapreduce.job.queuename"
    )
    if mr_queue is not None:
      putMapredProperty("mapreduce.job.queuename", mr_queue)

  def recommendConfigurationsForSSO(self, configurations, clusterData, services, hosts):
    ambari_configuration = self.get_ambari_configuration(services)
    ambari_sso_details = (
      ambari_configuration.get_ambari_sso_details() if ambari_configuration else None
    )

    if ambari_sso_details and ambari_sso_details.is_managing_services():
      putMapRedSiteProperty = self.putProperty(configurations, "mapred-site", services)

      # If SSO should be enabled for this service
      if ambari_sso_details.should_enable_sso("MAPREDUCE2"):
        if self.is_kerberos_enabled(configurations, services):
          putMapRedSiteProperty(
            "hadoop.http.authentication.type",
            "org.apache.hadoop.security.authentication.server.JWTRedirectAuthenticationHandler",
          )
          putMapRedSiteProperty(
            "hadoop.http.authentication.authentication.provider.url",
            ambari_sso_details.get_sso_provider_url(),
          )
          putMapRedSiteProperty(
            "hadoop.http.authentication.public.key.pem",
            ambari_sso_details.get_sso_provider_certificate(False, True),
          )
        else:
          # Since Kerberos is not enabled, we can not enable SSO
          self.logger.warning(
            "Enabling SSO integration for MapReduce requires Kerberos, Since Kerberos is not enabled, SSO integration is not being recommended."
          )
          putMapRedSiteProperty("hadoop.http.authentication.type", "simple")

      # If SSO should be disabled for this service
      elif ambari_sso_details.should_disable_sso("MAPREDUCE2"):
        if self.is_kerberos_enabled(configurations, services):
          putMapRedSiteProperty("hadoop.http.authentication.type", "kerberos")
        else:
          putMapRedSiteProperty("hadoop.http.authentication.type", "simple")

  def is_kerberos_enabled(self, configurations, services):
    return _effective_kerberos_enabled(
      configurations, services, ("mapred-site", "core-site")
    )


class YARNValidator(service_advisor.ServiceAdvisor):
  """
  YARN Validator checks the correctness of properties whenever the service is first added or the user attempts to
  change configs via the UI.
  """

  def __init__(self, *args, **kwargs):
    self.as_super = super(YARNValidator, self)
    self.as_super.__init__(*args, **kwargs)

    self.validators = [
      ("yarn-site", self.validateYarnResourceConfigurations),
      ("yarn-site", self.validateYarnRuntimeConfigurations),
      ("yarn-env", self.validateYarnServiceCheckConfigurations),
      ("yarn-env", self.validateYarnCgroupConfigurations),
      (
        "ranger-yarn-plugin-properties",
        self.validateYarnRangerConfigurations,
      ),
    ]

  def validateYarnResourceConfigurations(
    self, properties, recommendedDefaults, configurations, services, hosts
  ):
    """Validate YARN resource configuration."""
    clusterEnv = _effective_site_properties(configurations, services, "cluster-env")
    validationItems = [
      {
        "config-name": "yarn.nodemanager.resource.memory-mb",
        "item": self.validatorLessThenDefaultValue(
          properties, recommendedDefaults, "yarn.nodemanager.resource.memory-mb"
        ),
      },
      {
        "config-name": "yarn.scheduler.minimum-allocation-mb",
        "item": self.validatorLessThenDefaultValue(
          properties, recommendedDefaults, "yarn.scheduler.minimum-allocation-mb"
        ),
      },
      {
        "config-name": "yarn.nodemanager.linux-container-executor.group",
        "item": self.validatorEqualsPropertyItem(
          properties,
          "yarn.nodemanager.linux-container-executor.group",
          clusterEnv,
          "user_group",
        ),
      },
      {
        "config-name": "yarn.scheduler.maximum-allocation-mb",
        "item": self.validatorLessThenDefaultValue(
          properties, recommendedDefaults, "yarn.scheduler.maximum-allocation-mb"
        ),
      },
    ]
    return self.toConfigurationValidationProblems(validationItems, "yarn-site")

  def validateYarnRuntimeConfigurations(
    self, properties, recommendedDefaults, configurations, services, hosts
  ):
    validationItems = []
    policy = properties.get("yarn.http.policy")
    policy_contract = {
      "HTTP_ONLY": ("yarn.timeline-service.webapp.address", "http"),
      "HTTPS_ONLY": ("yarn.timeline-service.webapp.https.address", "https"),
    }
    if policy not in policy_contract:
      validationItems.append(
        {
          "config-name": "yarn.http.policy",
          "item": self.getErrorItem(
            "yarn.http.policy must be HTTP_ONLY or HTTPS_ONLY"
          ),
        }
      )
    else:
      address_property, scheme = policy_contract[policy]
      webapp_address = properties.get(address_property)
      try:
        webapp_address = _parse_webapp_address(webapp_address, address_property)
      except ValueError as error:
        validationItems.append(
          {
            "config-name": address_property,
            "item": self.getErrorItem(str(error)),
          }
        )
      else:
        expected_url = f"{scheme}://{webapp_address}/ws/v1/applicationhistory"
        if properties.get("yarn.log.server.web-service.url") != expected_url:
          validationItems.append(
            {
              "config-name": "yarn.log.server.web-service.url",
              "item": self.getWarnItem(f"Value should be {expected_url}"),
            }
          )

    container_executor = dict(
      self.getServicesSiteProperties(services, "container-executor") or {}
    )
    container_executor.update(
      self.getSiteProperties(configurations, "container-executor") or {}
    )
    yarn_hierarchy = container_executor.get("yarn_hierarchy")
    yarn_cgroup_hierarchy = properties.get(
      "yarn.nodemanager.linux-container-executor.cgroups.hierarchy"
    )
    if (
      yarn_hierarchy is not None
      and yarn_cgroup_hierarchy is not None
      and yarn_hierarchy != yarn_cgroup_hierarchy
    ):
      validationItems.append(
        {
          "config-name": "yarn.nodemanager.linux-container-executor.cgroups.hierarchy",
          "item": self.getWarnItem(
            "yarn.nodemanager.linux-container-executor.cgroups.hierarchy and "
            "container-executor/yarn_hierarchy must match"
          ),
        }
      )

    return self.toConfigurationValidationProblems(validationItems, "yarn-site")

  def validateYarnServiceCheckConfigurations(
    self, properties, recommendedDefaults, configurations, services, hosts
  ):
    """Validate the YARN service-check queue configuration."""
    validationItems = [
      {
        "config-name": "service_check.queue.name",
        "item": self.validatorYarnQueue(
          properties, recommendedDefaults, "service_check.queue.name", services
        ),
      }
    ]
    return self.toConfigurationValidationProblems(validationItems, "yarn-env")

  def validateYarnCgroupConfigurations(
    self, properties, recommendedDefaults, configurations, services, hosts
  ):
    """Validate YARN cgroup configuration."""
    validationItems = []
    if "yarn_cgroups_enabled" in properties:
      configured_value = properties["yarn_cgroups_enabled"]
      if isinstance(configured_value, bool):
        yarn_cgroups_enabled = configured_value
      elif isinstance(configured_value, str) and configured_value.strip().lower() in (
        "true",
        "false",
      ):
        yarn_cgroups_enabled = configured_value.strip().lower() == "true"
      else:
        yarn_cgroups_enabled = False
        validationItems.append(
          {
            "config-name": "yarn_cgroups_enabled",
            "item": self.getWarnItem(
              "yarn_cgroups_enabled must be a boolean value"
            ),
          }
        )
      core_site_properties = _effective_site_properties(
        configurations, services, "core-site"
      )
      security_enabled = False
      authentication = str(
        core_site_properties.get("hadoop.security.authentication", "")
      ).strip().lower()
      authorization = core_site_properties.get("hadoop.security.authorization", False)
      if isinstance(authorization, str):
        authorization = authorization.strip().lower() == "true"
      security_enabled = authentication == "kerberos" and authorization is True
      if not security_enabled and yarn_cgroups_enabled:
        validationItems.append(
          {
            "config-name": "yarn_cgroups_enabled",
            "item": self.getWarnItem(
              "CPU Isolation should only be enabled if security is enabled"
            ),
          }
        )
    validationProblems = self.toConfigurationValidationProblems(
      validationItems, "yarn-env"
    )
    return validationProblems

  def validateYarnRangerConfigurations(
    self, properties, recommendedDefaults, configurations, services, hosts
  ):
    """Validate Ranger YARN plugin configuration."""
    validationItems = []
    ranger_plugin_properties = _effective_site_properties(
      configurations, services, "ranger-yarn-plugin-properties"
    )
    ranger_env = _effective_site_properties(configurations, services, "ranger-env")
    ranger_plugin_value = ranger_plugin_properties.get(
      "ranger-yarn-plugin-enabled", "No"
    )
    ranger_env_value = ranger_env.get("ranger-yarn-plugin-enabled", "No")
    try:
      ranger_plugin_enabled = _parse_yes_no(
        ranger_plugin_value,
        "ranger-yarn-plugin-properties/ranger-yarn-plugin-enabled",
      )
    except ValueError as error:
      ranger_plugin_enabled = None
      validationItems.append(
        {
          "config-name": "ranger-yarn-plugin-enabled",
          "item": self.getErrorItem(str(error)),
        }
      )
    try:
      ranger_env_enabled = _parse_yes_no(
        ranger_env_value, "ranger-env/ranger-yarn-plugin-enabled"
      )
    except ValueError as error:
      ranger_env_enabled = None
      validationItems.append(
        {
          "config-name": "ranger-yarn-plugin-enabled",
          "item": self.getErrorItem(str(error)),
        }
      )
    if ranger_plugin_enabled:
      repository_password = ranger_plugin_properties.get(
        "REPOSITORY_CONFIG_PASSWORD"
      )
      if not isinstance(repository_password, str) or not repository_password.strip():
        validationItems.append(
          {
            "config-name": "REPOSITORY_CONFIG_PASSWORD",
            "item": self.getErrorItem(
              "Ranger YARN repository config password must not be empty when "
              "the plugin is enabled"
            ),
          }
        )
    if (
      ranger_plugin_enabled is not None
      and ranger_env_enabled is not None
      and ranger_plugin_enabled != ranger_env_enabled
    ):
      validationItems.append(
        {
          "config-name": "ranger-yarn-plugin-enabled",
          "item": self.getWarnItem(
            "ranger-yarn-plugin-properties/ranger-yarn-plugin-enabled must "
            "match ranger-env/ranger-yarn-plugin-enabled"
          ),
        }
      )
    return self.toConfigurationValidationProblems(
      validationItems, "ranger-yarn-plugin-properties"
    )


class MAPREDUCE2Validator(service_advisor.ServiceAdvisor):
  """
  MapReduce Validator checks properties when the service is added or configured.
  """

  def __init__(self, *args, **kwargs):
    self.as_super = super(MAPREDUCE2Validator, self)
    self.as_super.__init__(*args, **kwargs)

    self.validators = [
      ("mapred-site", self.validateMapReduceResourceConfigurations),
      ("mapred-site", self.validateMapReduceRuntimeConfigurations),
    ]

  def validateMapReduceResourceConfigurations(
    self, properties, recommendedDefaults, configurations, services, hosts
  ):
    """Validate MapReduce resource configuration."""
    validationItems = [
      {
        "config-name": "mapreduce.map.java.opts",
        "item": self.validateXmxValue(
          properties, recommendedDefaults, "mapreduce.map.java.opts"
        ),
      },
      {
        "config-name": "mapreduce.reduce.java.opts",
        "item": self.validateXmxValue(
          properties, recommendedDefaults, "mapreduce.reduce.java.opts"
        ),
      },
      {
        "config-name": "mapreduce.task.io.sort.mb",
        "item": self.validatorLessThenDefaultValue(
          properties, recommendedDefaults, "mapreduce.task.io.sort.mb"
        ),
      },
      {
        "config-name": "mapreduce.map.memory.mb",
        "item": self.validatorLessThenDefaultValue(
          properties, recommendedDefaults, "mapreduce.map.memory.mb"
        ),
      },
      {
        "config-name": "mapreduce.reduce.memory.mb",
        "item": self.validatorLessThenDefaultValue(
          properties, recommendedDefaults, "mapreduce.reduce.memory.mb"
        ),
      },
      {
        "config-name": "yarn.app.mapreduce.am.resource.mb",
        "item": self.validatorLessThenDefaultValue(
          properties, recommendedDefaults, "yarn.app.mapreduce.am.resource.mb"
        ),
      },
      {
        "config-name": "yarn.app.mapreduce.am.command-opts",
        "item": self.validateXmxValue(
          properties, recommendedDefaults, "yarn.app.mapreduce.am.command-opts"
        ),
      },
      {
        "config-name": "mapreduce.job.queuename",
        "item": self.validatorYarnQueue(
          properties, recommendedDefaults, "mapreduce.job.queuename", services
        ),
      },
    ]
    return self.toConfigurationValidationProblems(validationItems, "mapred-site")

  def validateMapReduceRuntimeConfigurations(
    self, properties, recommendedDefaults, configurations, services, hosts
  ):
    """Validate MapReduce runtime configuration."""
    validationItems = []

    if (
      "mapreduce.map.java.opts" in properties
      and "mapreduce.map.memory.mb" in properties
      and self.checkXmxValueFormat(properties["mapreduce.map.java.opts"])
    ):
      mapreduceMapJavaOpts = self.formatXmxSizeToBytes(
        self.getXmxSize(properties["mapreduce.map.java.opts"])
      ) / (1024.0 * 1024)
      mapreduceMapMemoryMb = self.to_number(properties["mapreduce.map.memory.mb"])
      if mapreduceMapJavaOpts > mapreduceMapMemoryMb:
        validationItems.append(
          {
            "config-name": "mapreduce.map.java.opts",
            "item": self.getWarnItem(
              f"mapreduce.map.java.opts Xmx should be less than mapreduce.map.memory.mb ({mapreduceMapMemoryMb})"
            ),
          }
        )

    if (
      "mapreduce.reduce.java.opts" in properties
      and "mapreduce.reduce.memory.mb" in properties
      and self.checkXmxValueFormat(properties["mapreduce.reduce.java.opts"])
    ):
      mapreduceReduceJavaOpts = self.formatXmxSizeToBytes(
        self.getXmxSize(properties["mapreduce.reduce.java.opts"])
      ) / (1024.0 * 1024)
      mapreduceReduceMemoryMb = self.to_number(properties["mapreduce.reduce.memory.mb"])
      if mapreduceReduceJavaOpts > mapreduceReduceMemoryMb:
        validationItems.append(
          {
            "config-name": "mapreduce.reduce.java.opts",
            "item": self.getWarnItem(
              f"mapreduce.reduce.java.opts Xmx should be less than mapreduce.reduce.memory.mb ({mapreduceReduceMemoryMb})"
            ),
          }
        )

    if (
      "yarn.app.mapreduce.am.command-opts" in properties
      and "yarn.app.mapreduce.am.resource.mb" in properties
      and self.checkXmxValueFormat(properties["yarn.app.mapreduce.am.command-opts"])
    ):
      yarnAppMapreduceAmCommandOpts = self.formatXmxSizeToBytes(
        self.getXmxSize(properties["yarn.app.mapreduce.am.command-opts"])
      ) / (1024.0 * 1024)
      yarnAppMapreduceAmResourceMb = self.to_number(
        properties["yarn.app.mapreduce.am.resource.mb"]
      )
      if yarnAppMapreduceAmCommandOpts > yarnAppMapreduceAmResourceMb:
        validationItems.append(
          {
            "config-name": "yarn.app.mapreduce.am.command-opts",
            "item": self.getWarnItem(
              f"yarn.app.mapreduce.am.command-opts Xmx should be less than yarn.app.mapreduce.am.resource.mb ({yarnAppMapreduceAmResourceMb})"
            ),
          }
        )

    return self.toConfigurationValidationProblems(validationItems, "mapred-site")
