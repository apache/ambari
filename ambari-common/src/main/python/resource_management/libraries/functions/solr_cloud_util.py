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

from contextlib import nullcontext

import json
import os
import random
import re
from ambari_commons.constants import AMBARI_SUDO_BINARY
from jinja2 import Environment as JinjaEnvironment
from resource_management.core.exceptions import ExecutionFailed, Fail
from resource_management.core.logger import Logger
from resource_management.core.resources.system import Directory, Execute, File
from resource_management.core.utils import PasswordString
from resource_management.libraries.functions import get_kinit_path
from resource_management.libraries.functions.format import format
from resource_management.libraries.functions.private_kerberos_cache import (
  PrivateKerberosCache,
)
from resource_management.libraries.script.script import Script

__all__ = [
  "upload_configuration_to_zk",
  "create_collection",
  "set_cluster_prop",
  "setup_kerberos_plugin",
  "create_znode",
  "check_znode",
  "secure_solr_znode",
  "secure_znode",
  "setup_solr_client",
  "add_solr_roles",
  "post_json_to_solr",
]


_CLI_NAME_PATTERN = re.compile(r"[A-Za-z0-9_][A-Za-z0-9_.-]*", re.ASCII)
_HOST_PATTERN = re.compile(
  r"[A-Za-z0-9](?:[A-Za-z0-9.-]{0,251}[A-Za-z0-9])?", re.ASCII
)
_ZNODE_PATTERN = re.compile(
  r"/(?:[A-Za-z0-9_.-]+(?:/[A-Za-z0-9_.-]+)*)?", re.ASCII
)


def _safe_cli_name(value, name):
  if not isinstance(value, str) or _CLI_NAME_PATTERN.fullmatch(value) is None:
    raise Fail(f"{name} contains invalid characters")
  return value


def _safe_absolute_path(value, name):
  if (
    not isinstance(value, str)
    or not os.path.isabs(value)
    or value == "/"
    or value.startswith("//")
    or os.path.normpath(value) != value
    or any(ord(character) < 32 for character in value)
  ):
    raise Fail(f"{name} must be a safe absolute path")
  return value


def _safe_text(value, name):
  if (
    not isinstance(value, str)
    or not value
    or any(ord(character) < 32 for character in value)
  ):
    raise Fail(f"{name} must be non-empty text without control characters")
  return value


def _safe_host(value, name):
  if (
    not isinstance(value, str)
    or _HOST_PATTERN.fullmatch(value) is None
    or ".." in value
  ):
    raise Fail(f"{name} is not a valid host name")
  return value.lower()


def _safe_znode(value):
  if not isinstance(value, str):
    raise Fail("Solr znode is invalid")
  normalized = value.rstrip("/") or "/"
  if _ZNODE_PATTERN.fullmatch(normalized) is None:
    raise Fail("Solr znode is invalid")
  return normalized


def _safe_zookeeper_quorum(value):
  if not isinstance(value, str) or not value:
    raise Fail("ZooKeeper quorum is required")
  endpoints = []
  for endpoint in value.split(","):
    host, separator, port = endpoint.rpartition(":")
    if not separator:
      raise Fail(f"ZooKeeper endpoint {endpoint!r} must include a port")
    normalized_host = _safe_host(host, "ZooKeeper host")
    normalized_port = _positive_int(port, "ZooKeeper port", maximum=65535)
    endpoints.append(f"{normalized_host}:{normalized_port}")
  return ",".join(endpoints)


def _as_bool(value, name):
  if isinstance(value, bool):
    return value
  if isinstance(value, str) and value.lower() in ("true", "false"):
    return value.lower() == "true"
  raise Fail(f"{name} must be true or false")


def _positive_int(value, name, maximum=None):
  if isinstance(value, bool):
    raise Fail(f"{name} must be an integer")
  try:
    parsed = int(value)
  except (TypeError, ValueError) as error:
    raise Fail(f"{name} must be an integer") from error
  if parsed <= 0 or (maximum is not None and parsed > maximum):
    upper_bound = f" and at most {maximum}" if maximum is not None else ""
    raise Fail(f"{name} must be positive{upper_bound}")
  return parsed


def __create_solr_cloud_cli_prefix(
  zookeeper_quorum,
  solr_znode,
  java64_home,
  java_opts=None,
  jaas_file=None,
  separated_znode=False,
):
  _safe_absolute_path(java64_home, "Java home")
  zookeeper_quorum = _safe_zookeeper_quorum(zookeeper_quorum)
  solr_znode = _safe_znode(solr_znode)
  command = [AMBARI_SUDO_BINARY, f"JAVA_HOME={java64_home}"]
  if java_opts is not None:
    command.append(f"INFRA_SOLR_CLI_OPTS={_safe_text(str(java_opts), 'Java options')}")
  command.extend(
    (
      "/usr/lib/ambari-infra-solr-client/solrCloudCli.sh",
      "--zookeeper-connect-string",
      zookeeper_quorum if separated_znode else f"{zookeeper_quorum}{solr_znode}",
    )
  )
  if separated_znode:
    command.extend(("--znode", solr_znode))

  if jaas_file:
    command.extend(("--jaas-file", _safe_absolute_path(jaas_file, "JAAS file")))

  return tuple(command)


def __append_flags_if_exists(command, flagsDict):
  command = list(command)
  for key, value in flagsDict.items():
    if value is not None:
      command.extend((key, value))
  return tuple(command)


def upload_configuration_to_zk(
  zookeeper_quorum,
  solr_znode,
  config_set,
  config_set_dir,
  tmp_dir,
  java64_home,
  retry=5,
  interval=10,
  solrconfig_content=None,
  jaas_file=None,
  java_opts=None,
):
  """
  Upload configuration set to zookeeper with solrCloudCli.sh
  At first, it tries to download an existing configuration set into a temporary
  location. If it does not exist, upload the config_set_dir content instead.
  """
  _safe_cli_name(config_set, "Solr configuration set")
  _safe_absolute_path(config_set_dir, "Solr configuration directory")
  _safe_absolute_path(tmp_dir, "Solr temporary directory")
  retry = _positive_int(retry, "Solr configuration retry count")
  interval = _positive_int(interval, "Solr configuration retry interval")
  random_num = str(random.random()).replace(".", "")
  tmp_config_set_dir = os.path.join(
    tmp_dir, f"solr_config_{config_set}_{random_num}"
  )
  solr_cli_prefix = __create_solr_cloud_cli_prefix(
    zookeeper_quorum, solr_znode, java64_home, java_opts, jaas_file
  )
  common_options = (
    "--config-set",
    config_set,
    "--retry",
    str(retry),
    "--interval",
    str(interval),
  )
  config_exists = True
  try:
    Execute(solr_cli_prefix + ("--check-config",) + common_options)
  except ExecutionFailed:
    config_exists = False

  try:
    if config_exists:
      Execute(
        solr_cli_prefix
        + ("--download-config", "--config-dir", tmp_config_set_dir)
        + common_options
      )
      if solrconfig_content is not None:
        File(
          os.path.join(tmp_config_set_dir, "solrconfig.xml"),
          content=solrconfig_content,
          mode=0o640,
        )
      Execute(
        solr_cli_prefix
        + ("--upload-config", "--config-dir", tmp_config_set_dir)
        + common_options
      )
    else:
      Execute(
        solr_cli_prefix
        + ("--upload-config", "--config-dir", config_set_dir)
        + common_options
      )
  finally:
    Directory(tmp_config_set_dir, action="delete")


def create_collection(
  zookeeper_quorum,
  solr_znode,
  collection,
  config_set,
  java64_home,
  shards=1,
  replication_factor=1,
  max_shards=1,
  retry=5,
  interval=10,
  implicitRouting=False,
  router_name=None,
  router_field=None,
  jaas_file=None,
  key_store_location=None,
  key_store_password=None,
  key_store_type=None,
  trust_store_location=None,
  trust_store_password=None,
  trust_store_type=None,
  java_opts=None,
):
  """
  Create Solr collection based on a configuration set in zookeeper.
  Calling this method again with a higher shard or maximum-shard count asks the
  CLI tool to add shards after a new Solr Cloud instance joins the cluster.

  If you would like to add shards later to a collection, then use implicit routing, e.g.:
  router_name = "implicit", router_field = "_router_field_"
  """
  solr_cli_prefix = __create_solr_cloud_cli_prefix(
    zookeeper_quorum, solr_znode, java64_home, java_opts, jaas_file
  )

  shards = _positive_int(shards, "Solr shard count")
  replication_factor = _positive_int(
    replication_factor, "Solr replication factor"
  )
  max_shards = _positive_int(max_shards, "Solr maximum shard count")
  retry = _positive_int(retry, "Solr collection retry count")
  interval = _positive_int(interval, "Solr collection retry interval")
  if max_shards == 1:  # If max shards is not specified, use this strategy.
    max_shards = replication_factor * shards

  _safe_cli_name(collection, "Solr collection")
  _safe_cli_name(config_set, "Solr configuration set")
  create_collection_cmd = solr_cli_prefix + (
    "--create-collection",
    "--collection",
    collection,
    "--config-set",
    config_set,
    "--shards",
    str(shards),
    "--replication",
    str(replication_factor),
    "--max-shards",
    str(max_shards),
    "--retry",
    str(retry),
    "--interval",
    str(interval),
  )

  implicitRouting = _as_bool(implicitRouting, "Solr implicit routing setting")
  if implicitRouting:
    create_collection_cmd += ("--implicit-routing",)
  if router_name is not None:
    _safe_cli_name(router_name, "Solr router name")
  if router_field is not None:
    _safe_cli_name(router_field, "Solr router field")
  for path, name in (
    (key_store_location, "Solr key store"),
    (trust_store_location, "Solr trust store"),
  ):
    if path is not None:
      _safe_absolute_path(path, name)
  appendableDict = {}
  appendableDict["--router-name"] = router_name
  appendableDict["--router-field"] = router_field
  appendableDict["--key-store-location"] = key_store_location
  appendableDict["--key-store-password"] = (
    None if key_store_password is None else PasswordString(key_store_password)
  )
  appendableDict["--key-store-type"] = key_store_type
  appendableDict["--trust-store-location"] = trust_store_location
  appendableDict["--trust-store-password"] = (
    None if trust_store_password is None else PasswordString(trust_store_password)
  )
  appendableDict["--trust-store-type"] = trust_store_type
  create_collection_cmd = __append_flags_if_exists(
    create_collection_cmd, appendableDict
  )
  Execute(create_collection_cmd)


def check_znode(
  zookeeper_quorum,
  solr_znode,
  java64_home,
  retry=5,
  interval=10,
  java_opts=None,
  jaas_file=None,
):
  """
  Check znode exists or not, throws exception if does not accessible.
  """
  solr_cli_prefix = __create_solr_cloud_cli_prefix(
    zookeeper_quorum, solr_znode, java64_home, java_opts, jaas_file, True
  )
  check_znode_cmd = solr_cli_prefix + (
    "--check-znode",
    "--retry",
    str(_positive_int(retry, "Solr znode retry count")),
    "--interval",
    str(_positive_int(interval, "Solr znode retry interval")),
  )
  Execute(check_znode_cmd)


def create_znode(
  zookeeper_quorum,
  solr_znode,
  java64_home,
  retry=5,
  interval=10,
  java_opts=None,
  jaas_file=None,
):
  """
  Create znode if does not exists, throws exception if zookeeper is not accessible.
  """
  solr_cli_prefix = __create_solr_cloud_cli_prefix(
    zookeeper_quorum, solr_znode, java64_home, java_opts, jaas_file, True
  )
  create_znode_cmd = solr_cli_prefix + (
    "--create-znode",
    "--retry",
    str(_positive_int(retry, "Solr znode retry count")),
    "--interval",
    str(_positive_int(interval, "Solr znode retry interval")),
  )
  Execute(create_znode_cmd)


def setup_kerberos_plugin(
  zookeeper_quorum,
  solr_znode,
  java64_home,
  secure=False,
  security_json_location=None,
  jaas_file=None,
  java_opts=None,
):
  """
  Set the Kerberos plugin in security.json, or clear it when security is off.
  """
  solr_cli_prefix = __create_solr_cloud_cli_prefix(
    zookeeper_quorum, solr_znode, java64_home, java_opts, jaas_file, True
  )
  setup_kerberos_plugin_cmd = solr_cli_prefix + ("--setup-kerberos-plugin",)
  secure = _as_bool(secure, "Solr Kerberos plugin setting")
  if secure:
    if jaas_file is None or security_json_location is None:
      raise Fail("JAAS and security.json files are required for secure Solr")
    setup_kerberos_plugin_cmd += (
      "--secure",
      "--security-json-location",
      _safe_absolute_path(security_json_location, "security.json file"),
    )
  Execute(setup_kerberos_plugin_cmd)


def set_cluster_prop(
  zookeeper_quorum,
  solr_znode,
  prop_name,
  prop_value,
  java64_home,
  jaas_file=None,
  java_opts=None,
):
  """
  Set a cluster property on the Solr znode in clusterprops.json
  """
  solr_cli_prefix = __create_solr_cloud_cli_prefix(
    zookeeper_quorum, solr_znode, java64_home, java_opts, jaas_file
  )
  _safe_cli_name(prop_name, "Solr cluster property name")
  _safe_text(str(prop_value), "Solr cluster property value")
  set_cluster_prop_cmd = solr_cli_prefix + (
    "--cluster-prop",
    "--property-name",
    str(prop_name),
    "--property-value",
    str(prop_value),
  )
  Execute(set_cluster_prop_cmd)


def secure_znode(
  config,
  zookeeper_quorum,
  solr_znode,
  jaas_file,
  java64_home,
  sasl_users=None,
  retry=5,
  interval=10,
  java_opts=None,
):
  """
  Secure znode, set a list of sasl users acl to 'cdrwa', and set acl to 'r' only for the world.
  Add infra-solr user by default if its available.
  """
  solr_cli_prefix = __create_solr_cloud_cli_prefix(
    zookeeper_quorum, solr_znode, java64_home, java_opts, jaas_file, True
  )
  sasl_users = list(sasl_users or ())
  if "infra-solr-env" in config["configurations"]:
    sasl_users.append(
      __get_name_from_principal(
        config["configurations"]["infra-solr-env"]["infra_solr_kerberos_principal"]
      )
    )
  sasl_users_str = ",".join(
    _safe_cli_name(__get_name_from_principal(x), "Solr SASL user")
    for x in dict.fromkeys(sasl_users)
  )
  secure_znode_cmd = solr_cli_prefix + (
    "--secure-znode",
    "--sasl-users",
    sasl_users_str,
    "--retry",
    str(_positive_int(retry, "Solr znode retry count")),
    "--interval",
    str(_positive_int(interval, "Solr znode retry interval")),
  )
  Execute(secure_znode_cmd)


def secure_solr_znode(
  zookeeper_quorum,
  solr_znode,
  jaas_file,
  java64_home,
  sasl_users_str="",
  java_opts=None,
):
  """
  Secure the Solr znode for its service user while retaining the required
  world-read collection and configuration ACLs.
  sasl_users_str: comma separated sasl users
  """
  solr_cli_prefix = __create_solr_cloud_cli_prefix(
    zookeeper_quorum, solr_znode, java64_home, java_opts, jaas_file, True
  )
  secure_solr_znode_cmd = solr_cli_prefix + (
    "--secure-solr-znode",
    "--sasl-users",
    _safe_text(str(sasl_users_str), "Solr SASL users"),
  )
  Execute(secure_solr_znode_cmd)


def default_config(config, name, default_value):
  subdicts = [_f for _f in name.split("/") if _f]
  if not config:
    return default_value
  for x in subdicts:
    if x in config:
      config = config[x]
    else:
      return default_value
  return config


def setup_solr_client(
  config,
  custom_log4j=True,
  custom_log_location=None,
  log4jcontent=None,
  user=None,
  group=None,
):
  solr_client_dir = "/usr/lib/ambari-infra-solr-client"
  solr_client_log_dir = (
    default_config(
      config,
      "/configurations/infra-solr-client-log4j/infra_solr_client_log_dir",
      "/var/log/ambari-infra-solr-client",
    )
    if custom_log_location is None
    else custom_log_location
  )
  _safe_absolute_path(solr_client_log_dir, "Solr client log directory")
  solr_client_log = os.path.join(solr_client_log_dir, "solr-client.log")
  solr_client_log_maxfilesize = _positive_int(
    default_config(
      config,
      "configurations/infra-solr-client-log4j/infra_client_log_maxfilesize",
      80,
    ),
    "Solr client maximum log size",
  )
  solr_client_log_maxbackupindex = _positive_int(
    default_config(
      config,
      "configurations/infra-solr-client-log4j/infra_client_log_maxbackupindex",
      60,
    ),
    "Solr client backup count",
  )

  directory_options = {
    "mode": 0o750,
    "create_parents": True,
  }
  log_options = {"mode": 0o640}
  if user is not None:
    directory_options["owner"] = _safe_cli_name(user, "Solr client user")
    log_options["owner"] = user
  if group is not None:
    directory_options["group"] = _safe_cli_name(group, "Solr client group")
    log_options["group"] = group
  Directory(solr_client_log_dir, **directory_options)

  if custom_log4j:
    # Use custom log4j content only when Infra Solr is not installed.
    solr_client_log4j_content = (
      config["configurations"]["infra-solr-client-log4j"]["content"]
      if log4jcontent is None
      else log4jcontent
    )
    context = {
      "solr_client_log": solr_client_log,
      "solr_client_log_maxfilesize": solr_client_log_maxfilesize,
      "solr_client_log_maxbackupindex": solr_client_log_maxbackupindex,
    }
    template = JinjaEnvironment(
      line_statement_prefix="%", variable_start_string="{{", variable_end_string="}}"
    ).from_string(solr_client_log4j_content)

    File(
      os.path.join(solr_client_dir, "log4j.properties"),
      content=template.render(context),
      owner="root",
      group="root",
      mode=0o644,
    )
  else:
    File(
      os.path.join(solr_client_dir, "log4j.properties"),
      owner="root",
      group="root",
      mode=0o644,
    )

  File(solr_client_log, **log_options)


def post_json_to_solr(
  host,
  port,
  collection,
  endpoint,
  payload,
  user,
  group=None,
  use_ssl=False,
  security_enabled=False,
  kinit_path=None,
  keytab=None,
  principal=None,
  tries=3,
  try_sleep=5,
):
  """POST JSON to a Solr collection with optional private Kerberos state."""
  host = _safe_host(host, "Solr host")
  port = _positive_int(port, "Solr port", maximum=65535)
  collection = _safe_cli_name(collection, "Solr collection")
  endpoint = _safe_cli_name(endpoint, "Solr API endpoint")
  user = _safe_cli_name(user, "Solr API user")
  if group is not None:
    _safe_cli_name(group, "Solr API group")
  use_ssl = _as_bool(use_ssl, "Solr SSL setting")
  security_enabled = _as_bool(security_enabled, "Solr security setting")
  try:
    request_body = json.dumps(payload)
  except (TypeError, ValueError) as error:
    raise Fail("Solr API payload must be JSON serializable") from error

  cache_context = nullcontext(None)
  if security_enabled:
    cache_context = PrivateKerberosCache(
      user,
      group,
      Script.get_tmp_dir(),
      "ambari-solr-api-",
    )
    _safe_absolute_path(kinit_path, "kinit executable")
    _safe_absolute_path(keytab, "Solr API keytab")
    _safe_text(principal, "Solr API principal")

  protocol = "https" if use_ssl else "http"
  url = f"{protocol}://{host}:{port}/solr/{collection}/{endpoint}"
  with cache_context as cache:
    environment = None
    if cache is not None:
      cache.kinit(kinit_path, keytab, principal)
      environment = cache.environment
    command = [
      "/usr/bin/curl",
      "--disable",
      "--fail",
      "--silent",
      "--show-error",
      "--connect-timeout",
      "10",
      "--max-time",
      "30",
      "--request",
      "POST",
      "--header",
      "Content-Type: application/json",
      "--data-binary",
      request_body,
      "--output",
      "/dev/null",
    ]
    if security_enabled:
      command.extend(("--negotiate", "--user", ":"))
    command.extend(("--url", url))
    options = {}
    if environment is not None:
      options["environment"] = environment
    Execute(
      tuple(command),
      tries=_positive_int(tries, "Solr API request tries"),
      try_sleep=_positive_int(try_sleep, "Solr API retry interval"),
      user=user,
      timeout=35,
      logoutput=True,
      **options,
    )


def __get_name_from_principal(principal):
  if not principal:  # return if empty
    return principal
  slash_split = principal.split("/")
  if len(slash_split) == 2:
    return slash_split[0]
  else:
    at_split = principal.split("@")
    return at_split[0]


def __remove_host_from_principal(principal, realm):
  if not realm:
    raise Fail("Realm parameter is missing")
  if not principal:
    raise Fail("Principal parameter is missing")
  _safe_text(realm, "Kerberos realm")
  username = __get_name_from_principal(principal)
  at_split = principal.split("@")
  if len(at_split) == 2:
    realm = at_split[1]
  return format("{username}@{realm}")


def __get_random_solr_host(actual_host, solr_hosts=None):
  """
  Prefer the local Infra Solr host, which supports blueprint installs.
  If there is only one solr host on the cluster, use that.
  """
  solr_hosts = list(solr_hosts or ())
  if not solr_hosts:
    raise Fail("Solr hosts parameter is empty")
  if len(solr_hosts) == 1:
    return solr_hosts[0]
  if actual_host in solr_hosts:
    return actual_host
  else:
    return random.choice(solr_hosts)


def add_solr_roles(
  config, roles=None, new_service_principals=None, tries=30, try_sleep=10
):
  """
  Set role mappings for service principals through the Solr authorization API.
  Skip this operation when security.json is managed manually or customized.
  """
  solr_hosts = default_config(config, "/clusterHostInfo/infra_solr_hosts", [])
  roles = list(roles or ())
  new_service_principals = list(new_service_principals or ())
  security_enabled = _as_bool(
    config["configurations"]["cluster-env"]["security_enabled"],
    "Cluster security setting",
  )
  solr_ssl_enabled = _as_bool(
    default_config(
      config, "configurations/infra-solr-env/infra_solr_ssl_enabled", False
    ),
    "Infra Solr SSL setting",
  )
  solr_port = _positive_int(
    default_config(config, "configurations/infra-solr-env/infra_solr_port", 8886),
    "Infra Solr port",
    maximum=65535,
  )
  kinit_path_local = get_kinit_path(
    default_config(config, "/configurations/kerberos-env/executable_search_paths", None)
  )
  infra_solr_custom_security_json_content = None
  infra_solr_security_manually_managed = False
  if "infra-solr-security-json" in config["configurations"]:
    infra_solr_custom_security_json_content = config["configurations"][
      "infra-solr-security-json"
    ]["content"]
    infra_solr_security_manually_managed = _as_bool(
      config["configurations"]["infra-solr-security-json"][
        "infra_solr_security_manually_managed"
      ],
      "Infra Solr security management setting",
    )

  Logger.info(
    format(
      "Adding {roles} roles to {new_service_principals} if infra-solr is installed."
    )
  )
  if infra_solr_security_manually_managed:
    Logger.info("security.json file is manually managed, skip adding roles...")
  elif (
    infra_solr_custom_security_json_content
    and str(infra_solr_custom_security_json_content).strip()
  ):
    Logger.info(
      "Custom security.json is not empty for infra-solr, skip adding roles..."
    )
  elif (
    security_enabled
    and "infra-solr-env" in config["configurations"]
    and solr_hosts is not None
    and len(solr_hosts) > 0
  ):
    solr_protocol = "https" if solr_ssl_enabled else "http"
    hostname = config["agentLevelParams"]["hostname"].lower()
    solr_host = _safe_host(
      __get_random_solr_host(hostname, solr_hosts), "Infra Solr host"
    )
    solr_url = (
      f"{solr_protocol}://{solr_host}:{solr_port}/solr/admin/authorization"
    )
    solr_user = _safe_cli_name(
      config["configurations"]["infra-solr-env"]["infra_solr_user"],
      "Infra Solr user",
    )
    solr_user_keytab = _safe_absolute_path(
      config["configurations"]["infra-solr-env"]["infra_solr_kerberos_keytab"],
      "Infra Solr keytab",
    )
    solr_user_principal = _safe_text(
      config["configurations"]["infra-solr-env"][
        "infra_solr_kerberos_principal"
      ].replace("_HOST", hostname),
      "Infra Solr principal",
    )

    if len(new_service_principals) > 0:
      if not roles:
        raise Fail("At least one Infra Solr role is required")
      new_service_users = []
      kerberos_realm = config["configurations"]["kerberos-env"]["realm"]
      for new_service_user in new_service_principals:
        new_service_users.append(
          __remove_host_from_principal(
            _safe_text(new_service_user, "Service principal"), kerberos_realm
          )
        )
      validated_roles = [_safe_cli_name(role, "Infra Solr role") for role in roles]
      user_role_map = {
        new_service_user: validated_roles for new_service_user in new_service_users
      }

      Logger.info(
        format(
          "New service users after removing fully qualified names: {new_service_users}"
        )
      )

      set_user_role_json = json.dumps({"set-user-role": user_role_map})
      group = default_config(config, "/configurations/cluster-env/user_group", None)
      if group is not None:
        _safe_cli_name(group, "Infra Solr group")

      with PrivateKerberosCache(
        solr_user,
        group,
        Script.get_tmp_dir(),
        "ambari-infra-solr-role-",
      ) as cache:
        cache.kinit(kinit_path_local, solr_user_keytab, solr_user_principal)
        Execute(
          (
            "/usr/bin/curl",
            "--disable",
            "--fail",
            "--silent",
            "--show-error",
            "--connect-timeout",
            "10",
            "--max-time",
            "30",
            "--negotiate",
            "--user",
            ":",
            "--request",
            "POST",
            "--header",
            "Content-Type: application/json",
            "--data-binary",
            set_user_role_json,
            "--output",
            "/dev/null",
            "--url",
            solr_url,
          ),
          tries=_positive_int(tries, "Infra Solr role update tries"),
          try_sleep=_positive_int(
            try_sleep, "Infra Solr role update retry interval"
          ),
          user=solr_user,
          environment=cache.environment,
          timeout=35,
          logoutput=True,
        )
