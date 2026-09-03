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

import math
import os
from urllib.parse import urlparse

from ambari_commons import import_utils


SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
STACKS_DIR = os.path.join(SCRIPT_DIR, "../../../../../stacks/")
PARENT_FILE = os.environ.get(
  "BASE_SERVICE_ADVISOR", os.path.join(STACKS_DIR, "service_advisor.py")
)
with open(PARENT_FILE, "rb") as parent_file:
  service_advisor = import_utils.load_module(
    "service_advisor",
    parent_file,
    PARENT_FILE,
    (".py", "rb", import_utils.PY_SOURCE),
  )


RANGER_AUTHORIZER = (
  "org.apache.ranger.authorization.hive.authorizer."
  "RangerHiveAuthorizerFactory"
)
SQL_AUTHORIZER = (
  "org.apache.hadoop.hive.ql.security.authorization.plugin.sqlstd."
  "SQLStdHiveAuthorizerFactory"
)
SESSION_AUTHENTICATOR = (
  "org.apache.hadoop.hive.ql.security.SessionStateUserAuthenticator"
)
ATLAS_HOOK = "org.apache.atlas.hive.hook.HiveHook"
DATABASE_CONTRACTS = {
  "new mysql database": (
    "mysql",
    "org.mariadb.jdbc.Driver",
    "jdbc:mariadb",
  ),
  "existing mysql / mariadb database": (
    "mysql",
    "org.mariadb.jdbc.Driver",
    "jdbc:mariadb",
  ),
  "existing postgresql database": (
    "postgres",
    "org.postgresql.Driver",
    "jdbc:postgresql",
  ),
  "existing oracle database": (
    "oracle",
    "oracle.jdbc.driver.OracleDriver",
    "jdbc:oracle",
  ),
}


def as_bool(value):
  if isinstance(value, bool):
    return value
  return str(value or "").strip().lower() in ("1", "true", "yes")


def service_names(services):
  return {
    service["StackServices"]["service_name"]
    for service in services.get("services", [])
  }


def effective_property(configurations, services, config_type, name, default=None):
  for source in (configurations, services.get("configurations", {})):
    value = source.get(config_type, {}).get("properties", {}).get(name)
    if value is not None:
      return value
  return default


class HiveServiceAdvisor(service_advisor.ServiceAdvisor):
  def __init__(self, *args, **kwargs):
    super().__init__(*args, **kwargs)
    self.initialize_logger("HiveServiceAdvisor")
    self.componentLayoutSchemes.update(
      {
        "HIVE_SERVER": {6: 1, 31: 2, "else": 4},
        "HIVE_METASTORE": {6: 1, 31: 2, "else": 4},
      }
    )

  def getServiceComponentLayoutValidations(self, services, hosts):
    return self.getServiceComponentCardinalityValidations(
      services, hosts, "HIVE"
    )

  def getServiceConfigurationRecommendations(
    self, configurations, clusterData, services, hosts
  ):
    HiveRecommender().recommendHiveConfigurations(
      configurations, clusterData, services, hosts
    )

  def getServiceConfigurationsValidationItems(
    self, configurations, recommendedDefaults, services, hosts
  ):
    validator = HiveValidator()
    return validator.validateListOfConfigUsingMethod(
      configurations,
      recommendedDefaults,
      services,
      hosts,
      validator.validators,
    )

  @staticmethod
  def isKerberosEnabled(services, configurations):
    authentication = effective_property(
      configurations,
      services,
      "hive-site",
      "hive.server2.authentication",
      "NONE",
    )
    return str(authentication).strip().lower() == "kerberos"


class HiveRecommender(service_advisor.ServiceAdvisor):
  def recommendHiveConfigurations(
    self, configurations, clusterData, services, hosts
  ):
    put_hive_site = self.putProperty(
      configurations, "hive-site", services
    )
    put_hive_env = self.putProperty(configurations, "hive-env", services)
    put_hs2_site = self.putProperty(
      configurations, "hiveserver2-site", services
    )
    put_hs2_attribute = self.putPropertyAttribute(
      configurations, "hiveserver2-site"
    )
    put_ranger = self.putProperty(
      configurations, "ranger-hive-plugin-properties", services
    )

    self._recommend_database(
      configurations,
      services,
      hosts,
      put_hive_site,
      put_hive_env,
    )

    metastore_hosts = self.getHostsWithComponent(
      "HIVE", "HIVE_METASTORE", services, hosts
    )
    if metastore_hosts:
      metastore_port = self._metastore_port(configurations, services)
      metastore_uris = ",".join(
        f"thrift://{host['Hosts']['host_name']}:{metastore_port}"
        for host in metastore_hosts
      )
      put_hive_site("hive.metastore.uris", metastore_uris)

    hive_server_hosts = self.getHostsWithComponent(
      "HIVE", "HIVE_SERVER", services, hosts
    )
    if hive_server_hosts:
      host_memory_mib = min(
        int(host["Hosts"].get("total_mem", 0)) // 1024
        for host in hive_server_hosts
      )
      if host_memory_mib > 0:
        recommended_heap = max(512, min(4096, host_memory_mib // 8))
        put_hive_env("hive.heapsize", str(recommended_heap))
        put_hive_env("hive.metastore.heapsize", str(recommended_heap))

    cpu_count = max(1, int(clusterData.get("cpu", 1) or 1))
    put_hive_site(
      "hive.compactor.worker.threads", str(max(1, math.ceil(cpu_count / 8)))
    )
    put_hive_env("beeline_jdbc_url_default", "container")

    authorization = str(
      effective_property(
        configurations,
        services,
        "hive-env",
        "hive_security_authorization",
        "None",
      )
    ).strip().lower()
    ranger_enabled = authorization == "ranger"
    if ranger_enabled:
      put_hive_site("hive.server2.enable.doAs", "false")
      put_hs2_site("hive.security.authorization.enabled", "true")
      put_hs2_site("hive.security.authorization.manager", RANGER_AUTHORIZER)
      put_hs2_site("hive.security.authenticator.manager", SESSION_AUTHENTICATOR)
      restricted = self._restricted_properties(configurations, services)
      put_hs2_site("hive.conf.restricted.list", ",".join(restricted))
      put_ranger(
        "REPOSITORY_CONFIG_USERNAME",
        effective_property(
          configurations, services, "hive-env", "hive_user", "hive"
        ),
      )
    elif authorization == "sqlstdauth":
      put_hive_site("hive.server2.enable.doAs", "false")
      put_hs2_site("hive.security.authorization.enabled", "true")
      put_hs2_site("hive.security.authorization.manager", SQL_AUTHORIZER)
      put_hs2_site("hive.security.authenticator.manager", SESSION_AUTHENTICATOR)
    else:
      put_hive_site("hive.server2.enable.doAs", "true")
      put_hs2_site("hive.security.authorization.enabled", "false")
      for name in (
        "hive.security.authorization.manager",
        "hive.security.authenticator.manager",
      ):
        put_hs2_attribute(name, "delete", "true")

    self._recommend_atlas(configurations, services, put_hive_site, put_hive_env)

  def _recommend_database(
    self, configurations, services, hosts, put_hive_site, put_hive_env
  ):
    database = str(
      effective_property(
        configurations, services, "hive-env", "hive_database", ""
      )
    ).strip().lower()
    contract = DATABASE_CONTRACTS.get(database)
    if contract is None:
      return
    db_type, driver, _ = contract
    put_hive_env("hive_database_type", db_type)
    put_hive_site("javax.jdo.option.ConnectionDriverName", driver)

    if database != "new mysql database":
      return
    mysql_hosts = self.getHostsWithComponent(
      "HIVE", "MYSQL_SERVER", services, hosts
    ) or self.getHostsWithComponent("HIVE", "HIVE_SERVER", services, hosts)
    if not mysql_hosts:
      return
    schema = str(
      effective_property(
        configurations,
        services,
        "hive-site",
        "ambari.hive.db.schema.name",
        "hive",
      )
    ).strip()
    host = mysql_hosts[0]["Hosts"]["host_name"]
    put_hive_site(
      "javax.jdo.option.ConnectionURL",
      f"jdbc:mariadb://{host}/{schema}?createDatabaseIfNotExist=true",
    )

  def _metastore_port(self, configurations, services):
    configured_uris = effective_property(
      configurations, services, "hive-site", "hive.metastore.uris", ""
    )
    for uri in str(configured_uris).split(","):
      try:
        port = urlparse(uri.strip()).port
      except ValueError:
        continue
      if port is not None and 1 <= port <= 65535:
        return port
    return 9083

  def _restricted_properties(self, configurations, services):
    existing = effective_property(
      configurations,
      services,
      "hiveserver2-site",
      "hive.conf.restricted.list",
      "",
    )
    values = []
    for value in str(existing).split(","):
      value = value.strip()
      if value and value not in values:
        values.append(value)
    for required in (
      "hive.security.authorization.enabled",
      "hive.security.authorization.manager",
      "hive.security.authenticator.manager",
    ):
      if required not in values:
        values.append(required)
    return values

  def _recommend_atlas(
    self, configurations, services, put_hive_site, put_hive_env
  ):
    external_atlas = as_bool(
      effective_property(
        configurations,
        services,
        "hive-atlas-application.properties",
        "enable.external.atlas.for.hive",
        False,
      )
    )
    atlas_enabled = "ATLAS" in service_names(services) or external_atlas
    put_hive_env("hive.atlas.hook", str(atlas_enabled).lower())
    hooks = [
      value.strip()
      for value in str(
        effective_property(
          configurations,
          services,
          "hive-site",
          "hive.exec.post.hooks",
          "",
        )
      ).split(",")
      if value.strip()
    ]
    hooks = [hook for hook in hooks if hook != ATLAS_HOOK]
    if atlas_enabled:
      hooks.append(ATLAS_HOOK)
    put_hive_site("hive.exec.post.hooks", ",".join(hooks))


class HiveValidator(service_advisor.ServiceAdvisor):
  def __init__(self, *args, **kwargs):
    super().__init__(*args, **kwargs)
    self.validators = [
      ("hive-site", self.validateHiveSite),
      ("hive-env", self.validateHiveEnvironment),
      ("hiveserver2-site", self.validateHiveServer2Site),
      ("ranger-hive-plugin-properties", self.validateRangerHivePlugin),
    ]

  def validateHiveSite(
    self, properties, recommendedDefaults, configurations, services, hosts
  ):
    validation_items = []
    self._validate_port(
      properties, "hive.server2.thrift.port", validation_items
    )
    self._validate_port(
      properties, "hive.server2.thrift.http.port", validation_items
    )
    authentication = str(
      properties.get("hive.server2.authentication", "NONE")
    ).strip().upper()
    if authentication not in ("NONE", "NOSASL", "KERBEROS", "LDAP", "PAM", "CUSTOM"):
      validation_items.append(
        self._warning(
          "hive.server2.authentication",
          f"Unsupported HiveServer2 authentication mode: {authentication}",
        )
      )
    if authentication == "KERBEROS":
      for name in (
        "hive.server2.authentication.kerberos.principal",
        "hive.server2.authentication.kerberos.keytab",
      ):
        if not str(properties.get(name, "")).strip():
          validation_items.append(
            self._warning(name, f"{name} is required for Kerberos")
          )

    transport = str(
      properties.get("hive.server2.transport.mode", "binary")
    ).strip().lower()
    if transport not in ("binary", "http"):
      validation_items.append(
        self._warning(
          "hive.server2.transport.mode",
          "Hive 3.1.3 transport mode must be binary or http",
        )
      )

    if as_bool(properties.get("hive.server2.support.dynamic.service.discovery")):
      if not str(properties.get("hive.server2.zookeeper.namespace", "")).strip("/"):
        validation_items.append(
          self._warning(
            "hive.server2.zookeeper.namespace",
            "Dynamic service discovery requires a ZooKeeper namespace",
          )
        )

    metastore_uris = str(properties.get("hive.metastore.uris", "")).strip()
    if not metastore_uris:
      validation_items.append(
        self._warning(
          "hive.metastore.uris", "Hive Metastore Thrift URI is required"
        )
      )
    for uri in (item.strip() for item in metastore_uris.split(",") if item.strip()):
      parsed = urlparse(uri)
      try:
        port = parsed.port
      except ValueError:
        port = None
      if (
        parsed.scheme != "thrift"
        or not parsed.hostname
        or port is None
        or not 1 <= port <= 65535
      ):
        validation_items.append(
          self._warning(
            "hive.metastore.uris",
            f"Invalid Hive Metastore Thrift URI: {uri}",
          )
        )

    self._validate_positive_integer(
      properties, "hive.tez.container.size", validation_items
    )
    self._validate_database(properties, configurations, validation_items)
    yarn_site = self.getSiteProperties(configurations, "yarn-site") or {}
    try:
      tez_size = int(properties.get("hive.tez.container.size", 0))
      yarn_maximum = int(
        yarn_site.get("yarn.scheduler.maximum-allocation-mb", 0)
      )
      if yarn_maximum > 0 and tez_size > yarn_maximum:
        validation_items.append(
          self._warning(
            "hive.tez.container.size",
            "Hive Tez container size exceeds the YARN maximum allocation",
          )
        )
    except (TypeError, ValueError):
      pass
    return self.toConfigurationValidationProblems(
      validation_items, "hive-site"
    )

  def _validate_database(self, properties, configurations, validation_items):
    hive_env = self.getSiteProperties(configurations, "hive-env") or {}
    database = str(hive_env.get("hive_database", "")).strip().lower()
    if not database:
      return
    contract = DATABASE_CONTRACTS.get(database)
    if contract is None:
      validation_items.append(
        self._warning("hive_database", f"Unsupported Hive database: {database}")
      )
      return
    db_type, driver, protocol = contract
    if str(hive_env.get("hive_database_type", "")).lower() != db_type:
      validation_items.append(
        self._warning(
          "hive_database_type",
          f"{database} requires hive_database_type={db_type}",
        )
      )
    if properties.get("javax.jdo.option.ConnectionDriverName") != driver:
      validation_items.append(
        self._warning(
          "javax.jdo.option.ConnectionDriverName",
          f"{database} requires JDBC driver {driver}",
        )
      )
    connection_url = str(
      properties.get("javax.jdo.option.ConnectionURL", "")
    ).strip()
    if not connection_url.startswith(protocol + ":"):
      validation_items.append(
        self._warning(
          "javax.jdo.option.ConnectionURL",
          f"{database} requires a {protocol} JDBC URL",
        )
      )

  def validateHiveEnvironment(
    self, properties, recommendedDefaults, configurations, services, hosts
  ):
    validation_items = []
    for name in ("hive.heapsize", "hive.metastore.heapsize"):
      self._validate_positive_integer(properties, name, validation_items)
    if properties.get("beeline_jdbc_url_default", "container") != "container":
      validation_items.append(
        self._warning(
          "beeline_jdbc_url_default",
          "BIGTOP Hive exposes only the standard HiveServer2 endpoint",
        )
      )
    authorization = str(
      properties.get("hive_security_authorization", "None")
    ).strip().lower()
    if authorization not in ("none", "ranger", "sqlstdauth"):
      validation_items.append(
        self._warning(
          "hive_security_authorization",
          f"Unsupported Hive authorization mode: {authorization}",
        )
      )
    hive_site = self.getSiteProperties(configurations, "hive-site") or {}
    authentication = str(
      hive_site.get("hive.server2.authentication", "NONE")
    ).strip().upper()
    credential_properties = {
      "LDAP": ("alert_ldap_username", "alert_ldap_password"),
      "PAM": ("alert_pam_username", "alert_pam_password"),
      "CUSTOM": ("alert_custom_username", "alert_custom_password"),
    }.get(authentication, ())
    for name in credential_properties:
      if not str(properties.get(name, "")):
        validation_items.append(
          self._warning(
            name,
            f"{authentication} alerts and service checks require {name}",
          )
        )
    return self.toConfigurationValidationProblems(
      validation_items, "hive-env"
    )

  def validateHiveServer2Site(
    self, properties, recommendedDefaults, configurations, services, hosts
  ):
    validation_items = []
    hive_env = self.getSiteProperties(configurations, "hive-env") or {}
    authorization = str(
      hive_env.get("hive_security_authorization", "None")
    ).strip().lower()
    if authorization == "ranger":
      expected = {
        "hive.security.authorization.enabled": "true",
        "hive.security.authorization.manager": RANGER_AUTHORIZER,
        "hive.security.authenticator.manager": SESSION_AUTHENTICATOR,
      }
      for name, value in expected.items():
        if properties.get(name) != value:
          validation_items.append(
            self._warning(name, f"Ranger authorization requires {name}={value}")
          )
    return self.toConfigurationValidationProblems(
      validation_items, "hiveserver2-site"
    )

  def validateRangerHivePlugin(
    self, properties, recommendedDefaults, configurations, services, hosts
  ):
    authorization = str(
      effective_property(
        configurations,
        services,
        "hive-env",
        "hive_security_authorization",
        "None",
      )
    ).strip().lower()
    validation_items = []
    if authorization == "ranger" and not str(
      properties.get("REPOSITORY_CONFIG_PASSWORD", "")
    ).strip():
      validation_items.append(
        {
          "config-name": "REPOSITORY_CONFIG_PASSWORD",
          "item": self.getErrorItem(
            "REPOSITORY_CONFIG_PASSWORD must not be empty when Ranger Hive authorization is enabled"
          ),
        }
      )
    return self.toConfigurationValidationProblems(
      validation_items, "ranger-hive-plugin-properties"
    )

  def _validate_port(self, properties, name, validation_items):
    try:
      port = int(properties.get(name, 0))
    except (TypeError, ValueError):
      port = 0
    if not 1 <= port <= 65535:
      validation_items.append(
        self._warning(name, f"{name} must be between 1 and 65535")
      )

  def _validate_positive_integer(self, properties, name, validation_items):
    try:
      value = int(properties.get(name, 0))
    except (TypeError, ValueError):
      value = 0
    if value <= 0:
      validation_items.append(
        self._warning(name, f"{name} must be a positive integer")
      )

  def _warning(self, name, message):
    return {"config-name": name, "item": self.getWarnItem(message)}
