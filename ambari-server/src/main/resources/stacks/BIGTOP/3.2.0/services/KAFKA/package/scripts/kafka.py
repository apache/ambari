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

import json
import os

from resource_management.libraries.functions.version import format_stack_version
from resource_management.libraries.resources.properties_file import PropertiesFile
from resource_management.libraries.resources.template_config import TemplateConfig
from resource_management.core.resources.system import Directory, File
from resource_management.core.source import Template, InlineTemplate
from resource_management.libraries.functions.default import default
from resource_management.libraries.functions import format
from resource_management.libraries.functions.generate_logfeeder_input_config import (
  generate_logfeeder_input_config,
)
from resource_management.libraries.functions.stack_features import check_stack_feature
from resource_management.libraries.functions import StackFeature
import re
import kafka_client

from resource_management.core import sudo
from resource_management.core.exceptions import Fail
from resource_management.core.logger import Logger


def kafka(upgrade_type=None):
  import params

  ensure_base_directories()

  kafka_server_config = mutable_config_dict(
    params.config["configurations"]["kafka-broker"]
  )
  effective_version = (
    params.stack_version_formatted
    if upgrade_type is None
    else format_stack_version(params.version)
  )
  Logger.info(format("Effective stack version: {effective_version}"))

  if (
    effective_version is not None
    and effective_version != ""
    and check_stack_feature(StackFeature.KAFKA_LISTENERS, effective_version)
  ):
    listeners = kafka_server_config["listeners"].replace("localhost", params.hostname)
    raw_listeners = kafka_server_config.pop("raw.listeners", "")
    if raw_listeners.strip():
      listeners = ",".join((listeners, raw_listeners))
    kafka_server_config["listeners"] = listeners

    if params.kerberos_security_enabled and params.kafka_kerberos_enabled:
      Logger.info("Kafka kerberos security is enabled.")

      if "security.inter.broker.protocol" in kafka_server_config:
        inter_broker_protocol = kafka_server_config[
          "security.inter.broker.protocol"
        ]
        inter_broker_protocol = replace_sasl_related_config(
          inter_broker_protocol, True
        )
        kafka_server_config["security.inter.broker.protocol"] = (
          inter_broker_protocol
        )

      listeners = kafka_server_config["listeners"]
      listeners = kafka_client.sasl_listeners(listeners)
      kafka_server_config["listeners"] = listeners

      if "listener.security.protocol.map" in kafka_server_config:
        kafka_server_config["listener.security.protocol.map"] = (
          kafka_client.sasl_listener_protocol_map(
            kafka_server_config["listener.security.protocol.map"]
          )
        )

      if "advertised.listeners" not in kafka_server_config:
        kafka_server_config["advertised.listeners"] = listeners
      elif params.kafka_kerberos_merge_advertised_listeners:
        Logger.warning(
          "User defined advertised.listeners will replace matching Ambari-managed listener endpoints. To leave the value as is change kafka-env/kerberos_merge_advertised_listeners to false."
        )
        advertised_listeners = kafka_client.sasl_listeners(
          kafka_server_config["advertised.listeners"].replace(
            "localhost", params.hostname
          )
        )
        kafka_server_config["advertised.listeners"] = (
          kafka_client.merge_advertised_listeners(
            listeners, advertised_listeners
          )
        )
    elif "advertised.listeners" in kafka_server_config:
      advertised_listeners = kafka_server_config["advertised.listeners"].replace(
        "localhost", params.hostname
      )
      kafka_server_config["advertised.listeners"] = advertised_listeners

    effective_kafka_listeners = kafka_server_config["listeners"]
    Logger.info("Kafka listeners: " + effective_kafka_listeners)
    if "advertised.listeners" in kafka_server_config:
      effective_advertised_listeners = kafka_server_config["advertised.listeners"]
      Logger.info("Kafka advertised listeners: " + effective_advertised_listeners)
  else:
    kafka_server_config["host.name"] = params.hostname

  kafka_data_dirs = validate_data_directories(kafka_server_config.get("log.dirs"))
  ensure_directories_are_not_symlinks(kafka_data_dirs)

  rack = "/default-rack"
  if params.all_racks:
    if len(params.all_hosts) != len(params.all_racks):
      raise Fail("Kafka host and rack topology lists have different lengths")
    rack_by_host = dict(zip(params.all_hosts, params.all_racks))
    if params.hostname not in rack_by_host:
      raise Fail(f"Kafka host {params.hostname} is missing from rack topology")
    rack = rack_by_host[params.hostname]
  kafka_server_config["broker.rack"] = rack

  Directory(
    kafka_data_dirs,
    mode=0o750,
    owner=params.kafka_user,
    group=params.user_group,
    create_parents=True,
  )

  PropertiesFile(
    "server.properties",
    mode=0o640,
    dir=params.conf_dir,
    properties=kafka_server_config,
    owner=params.kafka_user,
    group=params.user_group,
  )

  PropertiesFile(
    "kafka-client.properties",
    mode=0o600,
    dir=params.conf_dir,
    properties=kafka_client.client_properties(
      kafka_server_config, params.kafka_bare_jaas_principal
    ),
    owner=params.kafka_user,
    group=params.user_group,
  )

  File(
    format("{conf_dir}/kafka-env.sh"),
    owner=params.kafka_user,
    group=params.user_group,
    mode=0o640,
    content=InlineTemplate(params.kafka_env_sh_template),
  )

  if params.log4j_props != None:
    File(
      format("{conf_dir}/log4j.properties"),
      mode=0o644,
      group=params.user_group,
      owner=params.kafka_user,
      content=InlineTemplate(params.log4j_props),
    )

  if params.kafka_jaas_enabled:
    if params.kafka_jaas_conf_template:
      File(
        format("{conf_dir}/kafka_jaas.conf"),
        owner=params.kafka_user,
        group=params.user_group,
        mode=0o600,
        content=InlineTemplate(params.kafka_jaas_conf_template),
      )
    else:
      TemplateConfig(
        format("{conf_dir}/kafka_jaas.conf"),
        owner=params.kafka_user,
        group=params.user_group,
        mode=0o600,
      )
    if params.kafka_client_jaas_conf_template:
      File(
        format("{conf_dir}/kafka_client_jaas.conf"),
        owner=params.kafka_user,
        group=params.user_group,
        mode=0o600,
        content=InlineTemplate(params.kafka_client_jaas_conf_template),
      )
    else:
      TemplateConfig(
        format("{conf_dir}/kafka_client_jaas.conf"),
        owner=params.kafka_user,
        group=params.user_group,
        mode=0o600,
      )
  else:
    File(format("{conf_dir}/kafka_jaas.conf"), action="delete")
    File(format("{conf_dir}/kafka_client_jaas.conf"), action="delete")

  # On some OS this folder could be not exists, so we will create it before pushing there files
  Directory(params.limits_conf_dir, create_parents=True, owner="root", group="root")

  File(
    os.path.join(params.limits_conf_dir, "kafka.conf"),
    owner="root",
    group="root",
    mode=0o644,
    content=Template("kafka.conf.j2"),
  )

  File(
    os.path.join(params.conf_dir, "tools-log4j.properties"),
    owner="root",
    group="root",
    mode=0o644,
    content=Template("tools-log4j.properties.j2"),
  )

  generate_logfeeder_input_config(
    "kafka",
    Template("input.config-kafka.json.j2", extra_imports=[default, json]),
  )


def replace_sasl_related_config(property, only_protocol=False):
  property = (
    re.sub(r"(^|\b)PLAINTEXTSASL", "SASL_PLAINTEXT", property)
    if only_protocol
    else re.sub(r"(^|\b)PLAINTEXTSASL://", "SASL_PLAINTEXT://", property)
  )
  property = (
    re.sub(r"(^|\b)PLAINTEXT", "SASL_PLAINTEXT", property)
    if only_protocol
    else re.sub(r"(^|\b)PLAINTEXT://", "SASL_PLAINTEXT://", property)
  )
  property = (
    re.sub(r"(^|\b)SSL", "SASL_SSL", property)
    if only_protocol
    else re.sub(r"(^|\b)SSL://", "SASL_SSL://", property)
  )
  return property


def mutable_config_dict(kafka_broker_config):
  kafka_server_config = {}
  for key, value in kafka_broker_config.items():
    kafka_server_config[key] = value
  return kafka_server_config


def validate_data_directories(value):
  if not value:
    raise Fail("Kafka log.dirs must contain at least one data directory")
  directories = []
  for item in value.split(","):
    path = item.strip()
    if not path:
      raise Fail("Kafka log.dirs contains an empty data directory")
    if not os.path.isabs(path) or os.path.normpath(path) != path or path == os.path.sep:
      raise Fail(f"Kafka data directory {path!r} is not a safe absolute path")
    protected_trees = ("/boot", "/dev", "/etc", "/proc", "/run", "/sys", "/usr")
    if any(path == root or path.startswith(root + os.path.sep) for root in protected_trees):
      raise Fail(f"Kafka data directory {path!r} is inside a protected system path")
    if path in (
      "/bin",
      "/data",
      "/home",
      "/lib",
      "/lib64",
      "/mnt",
      "/opt",
      "/sbin",
      "/srv",
      "/tmp",
      "/var",
      "/var/lib",
      "/var/log",
    ):
      raise Fail(f"Kafka data directory {path!r} is a protected system path")
    if path in directories:
      raise Fail(f"Kafka data directory {path!r} is configured more than once")
    directories.append(path)
  return directories


def ensure_directories_are_not_symlinks(directories):
  for path in directories:
    if sudo.path_lexists(path) and sudo.path_islink(path):
      raise Fail(f"Kafka directory {path!r} must not be a symbolic link")


def ensure_base_directories():
  import params

  directories = [params.kafka_log_dir, params.kafka_pid_dir, params.conf_dir]
  ensure_directories_are_not_symlinks(
    [params.kafka_log_dir, params.kafka_pid_dir]
  )
  Directory(
    directories,
    mode=0o750,
    owner=params.kafka_user,
    group=params.user_group,
    create_parents=True,
  )
