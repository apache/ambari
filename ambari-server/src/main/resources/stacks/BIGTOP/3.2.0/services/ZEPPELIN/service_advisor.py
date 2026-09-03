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
import hashlib
import os
import re

SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
STACKS_DIR = os.path.join(SCRIPT_DIR, "../../../../../stacks/")
PARENT_FILE = os.path.join(STACKS_DIR, "service_advisor.py")

if "BASE_SERVICE_ADVISOR" in os.environ:
  PARENT_FILE = os.environ["BASE_SERVICE_ADVISOR"]
try:
  with open(PARENT_FILE, "rb") as fp:
    service_advisor = import_utils.load_module(
      "service_advisor", fp, PARENT_FILE, (".py", "rb", import_utils.PY_SOURCE)
    )
except Exception as error:
  raise RuntimeError(f"Failed to load parent service advisor {PARENT_FILE}") from error


_USER_PATTERN = re.compile(r"[A-Za-z_][A-Za-z0-9_.-]*\$?", re.ASCII)
_LEGACY_LOCAL_USER_DIGESTS = frozenset(
  (
    "0051e9a0197704a027623c2376b1758a2519a3f5a0b67a507144d7cb10bbc961",
    "e406380da87579e67e29e7d19a0c7aae1f73630774c5f4c0953bf709284e85a9",
    "78f23667e87d9f7fe5e5da4e80411a81f4477f8bb9b5bfdb2536c0145181bbb0",
    "7053e375d3d158c3899072fce62a53b7050f2052352957d3e691456d17edd12f",
  )
)


def _shiro_configuration_error(content):
  if not isinstance(content, str) or not content.strip():
    return "Zeppelin Shiro configuration must not be empty"
  if "\x00" in content:
    return "Zeppelin Shiro configuration contains a NUL character"

  section = None
  catch_all = None
  for raw_line in content.splitlines():
    line = raw_line.strip()
    if not line or line.startswith(("#", ";")):
      continue
    if line.startswith("[") and line.endswith("]"):
      section = line[1:-1].strip().lower()
      continue
    if section == "users" and "=" in line:
      normalized = re.sub(r"\s+", "", line)
      digest = hashlib.sha256(normalized.encode("utf-8")).hexdigest()
      if digest in _LEGACY_LOCAL_USER_DIGESTS:
        return "Remove the insecure packaged Zeppelin local accounts"
    if section == "urls" and "=" in line:
      path, rule = line.split("=", 1)
      if path.strip() == "/**":
        catch_all = rule.strip()

  if catch_all is None:
    return "Zeppelin Shiro configuration must define a /** authentication rule"
  filters = {
    item.strip().split("[", 1)[0]
    for item in catch_all.split(",")
    if item.strip()
  }
  if "anon" in filters or "authc" not in filters:
    return "Zeppelin Shiro /** rule must require authc and must not allow anon"
  return None


def _integer(value):
  if isinstance(value, bool):
    return None
  try:
    return int(value)
  except (TypeError, ValueError):
    return None


def _safe_directory(value):
  return (
    isinstance(value, str)
    and os.path.isabs(value)
    and value != "/"
    and not value.startswith("//")
    and os.path.normpath(value) == value
    and not any(ord(character) < 32 for character in value)
  )


def _safe_store_path(value):
  return (
    isinstance(value, str)
    and bool(value)
    and not value.startswith("//")
    and os.path.normpath(value) == value
    and not value.startswith("../")
    and not any(ord(character) < 32 for character in value)
  )


class ZeppelinServiceAdvisor(service_advisor.ServiceAdvisor):
  def __init__(self, *args, **kwargs):
    super().__init__(*args, **kwargs)
    self.initialize_logger("ZeppelinServiceAdvisor")
    self.mastersWithMultipleInstances.add("ZEPPELIN_SERVER")
    self.cardinalitiesDict["ZEPPELIN_SERVER"] = {"min": 1}

  def getServiceComponentLayoutValidations(self, services, hosts):
    """
    Get a list of errors.
    Must be overriden in child class.
    """

    return self.getServiceComponentCardinalityValidations(services, hosts, "ZEPPELIN")

  def getServiceConfigurationRecommendations(
    self, configurations, clusterData, services, hosts
  ):
    """
    Entry point.
    Must be overriden in child class.
    """
    # Logger.info("Class: %s, Method: %s. Recommending Service Configurations." %
    #            (self.__class__.__name__, inspect.stack()[0][3]))

    recommender = ZeppelinRecommender()
    recommender.recommendBigtopSecurityConfigurations(
      configurations, clusterData, services, hosts
    )
    recommender.recommendBigtopRuntimeConfigurations(
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
    # Logger.info("Class: %s, Method: %s. Validating Configurations." %
    #            (self.__class__.__name__, inspect.stack()[0][3]))

    validator = ZeppelinValidator()
    # Calls the methods of the validator using arguments,
    # method(siteProperties, siteRecommendations, configurations, services, hosts)
    return validator.validateListOfConfigUsingMethod(
      configurations, recommendedDefaults, services, hosts, validator.validators
    )

  @staticmethod
  def isKerberosEnabled(services, configurations):
    """
    Determine if Kerberos is enabled for Zeppelin.

    If zeppelin-env/zeppelin.kerberos.enabled exists and is set to "true", return True;
    otherwise return false.

    The value of this property is first tested in the updated configurations (configurations) then
    tested in the current configuration set (services)

    :type services: dict
    :param services: the dictionary containing the existing configuration values
    :type configurations: dict
    :param configurations: the dictionary containing the updated configuration values
    :rtype: bool
    :return: True or False
    """
    if (
      configurations
      and "zeppelin-env" in configurations
      and "zeppelin.kerberos.enabled" in configurations["zeppelin-env"]["properties"]
    ):
      return (
        str(
          configurations["zeppelin-env"]["properties"][
            "zeppelin.kerberos.enabled"
          ]
        ).strip().lower()
        == "true"
      )
    elif (
      services
      and "zeppelin-env" in services.get("configurations", {})
      and "zeppelin.kerberos.enabled"
      in services["configurations"]["zeppelin-env"]["properties"]
    ):
      return (
        str(
          services["configurations"]["zeppelin-env"]["properties"][
            "zeppelin.kerberos.enabled"
          ]
        ).strip().lower()
        == "true"
      )
    else:
      return False


class ZeppelinRecommender(service_advisor.ServiceAdvisor):
  """
  Zeppelin Recommender suggests properties when adding the service for the first time or modifying configs via the UI.
  """

  def __init__(self, *args, **kwargs):
    self.as_super = super(ZeppelinRecommender, self)
    self.as_super.__init__(*args, **kwargs)

  def recommendBigtopSecurityConfigurations(
    self, configurations, clusterData, services, hosts
  ):
    """
    :type configurations dict
    :type clusterData dict
    :type services dict
    :type hosts dict
    """
    self.__recommendLivySuperUsers(configurations, services)

    zeppelin_shiro_ini = self.getServicesSiteProperties(services, "zeppelin-shiro-ini")
    zeppelin_site = self.getServicesSiteProperties(services, "zeppelin-site")
    putZeppelinShiroIniProperty = self.putProperty(
      configurations, "zeppelin-shiro-ini", services
    )

    if zeppelin_shiro_ini and "shiro_ini_content" in zeppelin_shiro_ini:
      shiro_ini_content = zeppelin_shiro_ini["shiro_ini_content"]

      if (
        zeppelin_site
        and "zeppelin.ssl" in zeppelin_site
        and zeppelin_site["zeppelin.ssl"] == "true"
      ):
        shiro_ini_content = shiro_ini_content.replace(
          "#cookie.secure = true", "cookie.secure = true"
        )
        putZeppelinShiroIniProperty("shiro_ini_content", str(shiro_ini_content))

      else:
        if not "#cookie.secure = true" in shiro_ini_content:
          shiro_ini_content = shiro_ini_content.replace(
            "cookie.secure = true", "#cookie.secure = true"
          )
          putZeppelinShiroIniProperty("shiro_ini_content", str(shiro_ini_content))

  def recommendBigtopRuntimeConfigurations(
    self, configurations, clusterData, services, hosts
  ):
    """
    :type configurations dict
    :type clusterData dict
    :type services dict
    :type hosts dict
    """
    service_names = {
      service["StackServices"]["service_name"]
      for service in services.get("services", [])
    }
    service_configurations = services.get("configurations", {})
    spark_available = (
      "SPARK" in service_names and "spark-env" in service_configurations
    )
    zeppelin_env_properties = self.getServicesSiteProperties(services, "zeppelin-env")
    if not zeppelin_env_properties or "zeppelin_env_content" not in zeppelin_env_properties:
      return

    content = zeppelin_env_properties["zeppelin_env_content"]
    if not spark_available and "spark-atlas-connector" not in content:
      return

    result_list = []
    for line in content.splitlines():
      if "ZEPPELIN_INTP_CLASSPATH_OVERRIDES" in line:
        connector_paths = re.findall(
          r"/usr/[^/]+/current/spark-atlas-connector/\*", line
        )
        for path in connector_paths:
          line = line.replace(":" + path, "").replace(path + ":", "")
          line = line.replace(path, "")
      result_list.append(line)

    updated_content = "\n".join(result_list)
    if updated_content != content:
      putZeppelinEnvProperty = self.putProperty(
        configurations, "zeppelin-env", services
      )
      putZeppelinEnvProperty("zeppelin_env_content", updated_content)

  def __recommendLivySuperUsers(self, configurations, services):
    """
    If Kerberos is enabled AND Zeppelin is installed and Spark Livy Server is installed, then set
    livy-conf/livy.superusers to contain the Zeppelin principal name from
    zeppelin-site/zeppelin.server.kerberos.principal

    :param configurations:
    :param services:
    """
    if ZeppelinServiceAdvisor.isKerberosEnabled(services, configurations):
      zeppelin_site = self.getServicesSiteProperties(services, "zeppelin-site")

      if zeppelin_site and "zeppelin.server.kerberos.principal" in zeppelin_site:
        zeppelin_principal = zeppelin_site["zeppelin.server.kerberos.principal"]
        zeppelin_user = zeppelin_principal.split("@")[0] if zeppelin_principal else None

        if zeppelin_user:
          self.__conditionallyUpdateSuperUsers(
            "livy-conf", "livy.superusers", zeppelin_user, configurations, services
          )

  def __conditionallyUpdateSuperUsers(
    self, config_name, property_name, user_to_add, configurations, services
  ):
    config = self.getServicesSiteProperties(services, config_name)

    if config:
      superusers = config[property_name] if property_name in config else None

      # add the user to the set of users
      if superusers:
        _superusers = superusers.split(",")
        _superusers = [x.strip() for x in _superusers]
        _superusers = [
          _f for _f in _superusers if _f
        ]  # Removes empty string elements from array
      else:
        _superusers = []

      if user_to_add not in _superusers:
        _superusers.append(user_to_add)

        putProperty = self.putProperty(configurations, config_name, services)
        putProperty(property_name, ",".join(_superusers))


class ZeppelinValidator(service_advisor.ServiceAdvisor):
  """
  Zeppelin Validator checks the correctness of properties whenever the service is first added or the user attempts to
  change configs via the UI.
  """

  def __init__(self, *args, **kwargs):
    super().__init__(*args, **kwargs)
    self.validators = [
      ("zeppelin-site", self.validate_site),
      ("zeppelin-env", self.validate_environment),
      ("zeppelin-shiro-ini", self.validate_shiro),
    ]

  def validate_shiro(
    self, properties, recommendedDefaults, configurations, services, hosts
  ):
    error = _shiro_configuration_error(properties.get("shiro_ini_content"))
    items = []
    if error is not None:
      items.append(
        {
          "config-name": "shiro_ini_content",
          "item": self.getErrorItem(error),
        }
      )
    return self.toConfigurationValidationProblems(items, "zeppelin-shiro-ini")

  def validate_site(
    self, properties, recommendedDefaults, configurations, services, hosts
  ):
    items = []
    for name in ("zeppelin.server.port", "zeppelin.server.ssl.port"):
      port = _integer(properties.get(name))
      if port is None or not 0 < port <= 65535:
        items.append(
          {
            "config-name": name,
            "item": self.getErrorItem(f"{name} must be between 1 and 65535"),
          }
        )

    boolean_names = (
      "zeppelin.ssl",
      "zeppelin.ssl.client.auth",
      "zeppelin.notebook.homescreen.hide",
      "zeppelin.anonymous.allowed",
      "zeppelin.notebook.public",
      "zeppelin.interpreter.config.upgrade",
    )
    for name in boolean_names:
      if str(properties.get(name, "")).strip().lower() not in ("true", "false"):
        items.append(
          {
            "config-name": name,
            "item": self.getErrorItem(f"{name} must be true or false"),
          }
        )

    if str(properties.get("zeppelin.ssl", "false")).strip().lower() == "true":
      for name in ("zeppelin.ssl.keystore.path", "zeppelin.ssl.truststore.path"):
        if not _safe_store_path(properties.get(name)):
          items.append(
            {
              "config-name": name,
              "item": self.getErrorItem(f"{name} must be a safe path"),
            }
          )
      for name in (
        "zeppelin.ssl.keystore.password",
        "zeppelin.ssl.key.manager.password",
        "zeppelin.ssl.truststore.password",
      ):
        value = properties.get(name)
        if not isinstance(value, str) or not value or any(
          character in value for character in ("\x00", "\r", "\n")
        ):
          items.append(
            {
              "config-name": name,
              "item": self.getErrorItem(
                f"{name} is required when Zeppelin SSL is enabled"
              ),
            }
          )

    allowed_origins = str(
      properties.get("zeppelin.server.allowed.origins", "")
    ).strip()
    if not allowed_origins or allowed_origins == "*":
      items.append(
        {
          "config-name": "zeppelin.server.allowed.origins",
          "item": self.getWarnItem(
            "Zeppelin allowed origins must explicitly name trusted origins"
          ),
        }
      )
    return self.toConfigurationValidationProblems(items, "zeppelin-site")

  def validate_environment(
    self, properties, recommendedDefaults, configurations, services, hosts
  ):
    items = []
    for name in ("zeppelin_user", "zeppelin_group"):
      if _USER_PATTERN.fullmatch(str(properties.get(name, ""))) is None:
        items.append(
          {
            "config-name": name,
            "item": self.getErrorItem(f"{name} is invalid"),
          }
        )

    for name in (
      "zeppelin_pid_dir",
      "zeppelin_log_dir",
      "zeppelin_war_tempdir",
      "spark_home",
      "hbase_home",
      "hbase_conf_dir",
    ):
      if not _safe_directory(properties.get(name)):
        items.append(
          {
            "config-name": name,
            "item": self.getErrorItem(f"{name} must be a safe absolute path"),
          }
        )
    return self.toConfigurationValidationProblems(items, "zeppelin-env")
