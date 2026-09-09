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

from contextlib import contextmanager
import fcntl
import hashlib
import json
import os
import re
import secrets
import stat
import uuid
from urllib.parse import urlsplit

from resource_management.core.exceptions import Fail


PROTOCOL_VERSION = 1
JOURNAL_SCHEMA_VERSION = 1
BUNDLE_SCHEMA_VERSION = 2
ZOOKEEPER_PARENT = "/ambari-managed-hbase"
ZOOKEEPER_LEDGER_PARENT = ZOOKEEPER_PARENT + "/.bindings"
ZOOKEEPER_HBASE_CHILD = "hbase"
HDFS_BASE = "/apps/ambari-managed/hbase"

PREPARE_BINDING_JOURNAL = "PREPARE_BINDING_JOURNAL"
INITIALIZE_BINDING_JOURNAL = "INITIALIZE_BINDING_JOURNAL"
PROVISION_HDFS_NAMESPACE = "PROVISION_HDFS_NAMESPACE"
PROVISION_ZOOKEEPER_NAMESPACE = "PROVISION_ZOOKEEPER_NAMESPACE"
INVALIDATE_BINDING_EPOCH = "INVALIDATE_BINDING_EPOCH"
PREPARE_HDFS_CONSUMER = "PREPARE_HDFS_CONSUMER"
PREPARE_ZOOKEEPER_CONSUMER = "PREPARE_ZOOKEEPER_CONSUMER"
VERIFY_HDFS_CONSUMER = "VERIFY_HDFS_CONSUMER"
VERIFY_ZOOKEEPER_CONSUMER = "VERIFY_ZOOKEEPER_CONSUMER"

_HASH = re.compile(r"sha256:[0-9a-f]{64}\Z", re.ASCII)
_POSITIVE_INTEGER = re.compile(r"[1-9][0-9]*\Z", re.ASCII)
_MAX_SIGNED_LONG = (1 << 63) - 1
_SAFE_SERVICE = re.compile(r"(?:HDFS|ZOOKEEPER)\Z", re.ASCII)
_SAFE_PACKAGE = re.compile(r"[A-Za-z0-9][A-Za-z0-9+._~-]{0,127}\Z", re.ASCII)
_SOFTWARE_VERSION = re.compile(
  r"[0-9]+(?:\.[0-9]+){1,3}(?:[-+][A-Za-z0-9][A-Za-z0-9._-]*)?\Z", re.ASCII
)
_SAFE_IDENTITY = re.compile(r"[A-Za-z_][A-Za-z0-9._-]{0,127}\Z", re.ASCII)
_KERBEROS_REALM = re.compile(r"[A-Za-z0-9][A-Za-z0-9._-]{0,254}\Z", re.ASCII)
_HA_TOKEN = re.compile(r"[A-Za-z0-9][A-Za-z0-9.-]*\Z", re.ASCII)
_STANDARD_HA_PROVIDER = (
  "org.apache.hadoop.hdfs.server.namenode.ha.ConfiguredFailoverProxyProvider"
)
_MAX_CLIENT_CONFIG_BYTES = 32 * 1024
_MAX_CONSUMER_RULE_BYTES = 24 * 1024
_MAX_CONSUMER_RULE_COUNT = 256
_AUTH_TO_LOCAL = "hadoop.security.auth_to_local"
_CORE_SITE_KEYS = {
  "fs.defaultFS",
  "hadoop.rpc.protection",
  "hadoop.security.authentication",
  "hadoop.security.authorization",
  "ipc.client.connect.max.retries",
  "ipc.client.connect.retry.interval",
  "ipc.client.fallback-to-simple-auth-allowed",
}
_HDFS_SITE_KEYS = {
  "dfs.block.access.token.enable",
  "dfs.client.failover.connection.retries",
  "dfs.client.failover.connection.retries.on.timeouts",
  "dfs.client.socket-timeout",
  "dfs.client.use.datanode.hostname",
  "dfs.data.transfer.protection",
  "dfs.datanode.kerberos.principal",
  "dfs.encrypt.data.transfer",
  "dfs.namenode.kerberos.principal",
  "dfs.nameservices",
}
_ZOOKEEPER_KEYS = {
  "hbase.zookeeper.property.clientPort",
  "hbase.zookeeper.quorum",
  "zookeeper.sasl.client",
  "zookeeper.sasl.client.username",
  "zookeeper.sasl.clientconfig",
  "zookeeper.znode.parent",
}
_COMMON_PARAMETERS = {
  "provider.cluster.id",
  "provider.fingerprint",
  "provider.service",
  "security.mode",
  "snapshot.fingerprint",
}
_ACTION_HOST = {"provider.action.host.id"}
_CONSUMER_SECURITY_PROOFS = {
  "consumer.mapping.profile.fingerprint",
  "consumer.mapping.proof.fingerprint",
  "consumer.mapping.rules.fingerprint",
  "provider.security.pair.proof.fingerprint",
  "provider.security.policy.fingerprint",
}
_CONSUMER_PREPARATION_PARAMETERS = {
  "client.config.json",
  "client.config.fingerprint",
  "client.package.name",
  "client.software.kind",
  "client.software.semantic.version",
  "consumer.user",
  "host.id",
  "identity.fingerprint",
}
_STRICT_VERIFICATION_PARAMETERS = {
  "client.package.version",
  "preparation.observation.fingerprint",
  "preparation.observation.id",
  "preparation.request.hash",
}
_HDFS_CONSUMER_PARAMETERS = {
  "expected.default.fs",
  "expected.directory.mode",
  "expected.namespace.root.uri",
  "expected.namespace.wal.uri",
  "expected.owner.group",
}
_ZOOKEEPER_CONSUMER_PARAMETERS = {
  "expected.namespace.container.znode",
  "expected.namespace.parent.znode",
  "expected.namespace.znode",
}
_PARAMETERS = {
  PREPARE_BINDING_JOURNAL: _COMMON_PARAMETERS | _ACTION_HOST,
  INITIALIZE_BINDING_JOURNAL: _COMMON_PARAMETERS
  | _ACTION_HOST
  | {
    "initialization.authorization.id",
    "initialization.challenge",
    "initialization.prepare.request.hash",
  },
  PROVISION_HDFS_NAMESPACE: _COMMON_PARAMETERS
  | _ACTION_HOST
  | {
    "directory.mode",
    "namespace.root.uri",
    "namespace.wal.uri",
    "owner.group",
    "owner.user",
  },
  PROVISION_ZOOKEEPER_NAMESPACE: _COMMON_PARAMETERS
  | _ACTION_HOST
  | {
    "expected.client.port",
    "expected.quorum",
    "namespace.container.znode",
    "namespace.parent.znode",
    "namespace.znode",
    "owner.user",
    "parent.acl.policy",
    "subtree.acl.policy",
  },
  INVALIDATE_BINDING_EPOCH: _COMMON_PARAMETERS | _ACTION_HOST,
  PREPARE_HDFS_CONSUMER: _COMMON_PARAMETERS
  | _CONSUMER_PREPARATION_PARAMETERS
  | _HDFS_CONSUMER_PARAMETERS,
  PREPARE_ZOOKEEPER_CONSUMER: _COMMON_PARAMETERS
  | _CONSUMER_PREPARATION_PARAMETERS
  | _ZOOKEEPER_CONSUMER_PARAMETERS,
  VERIFY_HDFS_CONSUMER: _COMMON_PARAMETERS
  | _CONSUMER_PREPARATION_PARAMETERS
  | _STRICT_VERIFICATION_PARAMETERS
  | _HDFS_CONSUMER_PARAMETERS,
  VERIFY_ZOOKEEPER_CONSUMER: _COMMON_PARAMETERS
  | _CONSUMER_PREPARATION_PARAMETERS
  | _STRICT_VERIFICATION_PARAMETERS
  | _ZOOKEEPER_CONSUMER_PARAMETERS,
}
_OPTIONAL_PARAMETERS = {
  PROVISION_ZOOKEEPER_NAMESPACE: {
    "consumer.zk.sasl.id",
    "namespace.ledger.znode",
  },
  PREPARE_HDFS_CONSUMER: _CONSUMER_SECURITY_PROOFS
  | {"consumer.kerberos.principal.pattern"},
  PREPARE_ZOOKEEPER_CONSUMER: _CONSUMER_SECURITY_PROOFS
  | {
    "consumer.kerberos.principal.pattern",
    "expected.consumer.zk.sasl.id",
  },
  VERIFY_HDFS_CONSUMER: _CONSUMER_SECURITY_PROOFS
  | {"consumer.kerberos.principal.pattern"},
  VERIFY_ZOOKEEPER_CONSUMER: _CONSUMER_SECURITY_PROOFS
  | {
    "consumer.kerberos.principal.pattern",
    "expected.consumer.zk.sasl.id",
  },
}
_PROVIDER_COMMANDS = {
  PREPARE_BINDING_JOURNAL,
  INITIALIZE_BINDING_JOURNAL,
  PROVISION_HDFS_NAMESPACE,
  PROVISION_ZOOKEEPER_NAMESPACE,
  INVALIDATE_BINDING_EPOCH,
}


class ManagedDependencyFailure(Fail):
  def __init__(self, code, message, facts=None):
    self.code = code
    self.sanitized_message = message
    self.facts = dict(facts or {})
    super().__init__(f"{code}: {message}")


class ManagedDependencyCommand:
  def __init__(self, name, envelope, parameters):
    self.name = name
    self.envelope = dict(envelope)
    self.parameters = dict(parameters)

  @property
  def binding_id(self):
    return self.envelope["bindingId"]

  @property
  def operation_id(self):
    return self.envelope["operationId"]

  @property
  def epoch(self):
    return self.envelope["epoch"]

  @property
  def snapshot_version(self):
    return self.envelope["snapshotVersion"]

  @property
  def request_hash(self):
    return self.envelope["immutableRequestHash"]

  @property
  def provider_service(self):
    return self.parameters["provider.service"]

  @property
  def action_host_id(self):
    return self.parameters.get("provider.action.host.id")


class ManagedDependencyCommandBundle:
  def __init__(
    self,
    schema_version,
    host_id,
    consumer_user,
    identity_fingerprint,
    commands,
    preparation_binding_ids,
    immutable_bundle_hash,
  ):
    self.schema_version = schema_version
    self.host_id = host_id
    self.consumer_user = consumer_user
    self.identity_fingerprint = identity_fingerprint
    self.commands = tuple(commands)
    self.preparation_binding_ids = tuple(preparation_binding_ids)
    self.immutable_bundle_hash = immutable_bundle_hash

  @property
  def preparation_commands(self):
    selected = set(self.preparation_binding_ids)
    return tuple(command for command in self.commands if command.binding_id in selected)

  def command_for(self, provider_service):
    for command in self.commands:
      if command.provider_service == provider_service:
        return command
    return None


def parse_managed_dependency_command(value, expected_name=None):
  if not isinstance(value, dict) or set(value) != {"name", "envelope", "parameters"}:
    _fail_invalid("Command must contain only name, envelope, and parameters")
  name = value["name"]
  if name not in _PARAMETERS or expected_name is not None and name != expected_name:
    _fail_invalid("Command name is missing, unexpected, or unsupported")
  envelope = value["envelope"]
  expected_envelope = {
    "bindingId",
    "epoch",
    "immutableRequestHash",
    "operationId",
    "protocolVersion",
    "snapshotVersion",
  }
  if not isinstance(envelope, dict) or set(envelope) != expected_envelope:
    _fail_invalid("Command envelope contains missing or unknown fields")
  parameters = value["parameters"]
  required_parameters = _PARAMETERS[name]
  allowed_parameters = required_parameters | _OPTIONAL_PARAMETERS.get(name, set())
  if (
    not isinstance(parameters, dict)
    or not required_parameters.issubset(parameters)
    or not set(parameters).issubset(allowed_parameters)
  ):
    _fail_invalid("Command parameters contain missing or unknown fields")
  if any(not isinstance(key, str) or not isinstance(item, str) for key, item in parameters.items()):
    _fail_invalid("Command parameter names and values must be strings")

  if (
    type(envelope["protocolVersion"]) is not int
    or envelope["protocolVersion"] != PROTOCOL_VERSION
  ):
    _fail_invalid("Unsupported protocol version")
  envelope = dict(envelope)
  envelope["bindingId"] = _canonical_uuid(envelope["bindingId"], "bindingId")
  envelope["operationId"] = _canonical_uuid(envelope["operationId"], "operationId")
  envelope["epoch"] = _positive_integer(envelope["epoch"], "epoch")
  envelope["snapshotVersion"] = _positive_integer(
    envelope["snapshotVersion"], "snapshotVersion"
  )
  _require_hash(envelope["immutableRequestHash"], "immutableRequestHash")
  _positive_integer(parameters["provider.cluster.id"], "provider.cluster.id")
  _require_hash(parameters["provider.fingerprint"], "provider.fingerprint")
  _require_hash(parameters["snapshot.fingerprint"], "snapshot.fingerprint")
  if parameters["security.mode"] not in {"INSECURE", "KERBEROS"}:
    _fail_invalid("security.mode is unsupported")
  if _SAFE_SERVICE.fullmatch(parameters["provider.service"]) is None:
    _fail_invalid("provider.service is unsupported")

  if name in _PROVIDER_COMMANDS:
    _positive_integer(parameters["provider.action.host.id"], "provider.action.host.id")
  if name in {PREPARE_BINDING_JOURNAL, INITIALIZE_BINDING_JOURNAL}:
    pass
  elif name in {
    PROVISION_HDFS_NAMESPACE,
    PREPARE_HDFS_CONSUMER,
    VERIFY_HDFS_CONSUMER,
  }:
    if parameters["provider.service"] != "HDFS":
      _fail_invalid("The HDFS command requires provider.service=HDFS")
    root_field = (
      "namespace.root.uri"
      if name == PROVISION_HDFS_NAMESPACE
      else "expected.namespace.root.uri"
    )
    wal_field = (
      "namespace.wal.uri"
      if name == PROVISION_HDFS_NAMESPACE
      else "expected.namespace.wal.uri"
    )
    authority = _validate_hdfs_namespaces(
      envelope["bindingId"], parameters[root_field], parameters[wal_field]
    )
    if name in {PREPARE_HDFS_CONSUMER, VERIFY_HDFS_CONSUMER}:
      default_fs = _parse_hdfs_uri(parameters["expected.default.fs"], "expected.default.fs")
      if default_fs.path or default_fs.netloc != authority:
        _fail_invalid("expected.default.fs does not match the namespace authority")
  elif name in {
    PROVISION_ZOOKEEPER_NAMESPACE,
    PREPARE_ZOOKEEPER_CONSUMER,
    VERIFY_ZOOKEEPER_CONSUMER,
  }:
    if parameters["provider.service"] != "ZOOKEEPER":
      _fail_invalid("The ZooKeeper command requires provider.service=ZOOKEEPER")
    container_field = (
      "namespace.container.znode"
      if name == PROVISION_ZOOKEEPER_NAMESPACE
      else "expected.namespace.container.znode"
    )
    znode_field = (
      "namespace.znode" if name == PROVISION_ZOOKEEPER_NAMESPACE
      else "expected.namespace.znode"
    )
    expected_container = f"{ZOOKEEPER_PARENT}/{envelope['bindingId']}"
    if (
      parameters[container_field] != expected_container
      or parameters[znode_field] != f"{expected_container}/{ZOOKEEPER_HBASE_CHILD}"
      or name in {PREPARE_ZOOKEEPER_CONSUMER, VERIFY_ZOOKEEPER_CONSUMER}
      and parameters["expected.namespace.parent.znode"] != ZOOKEEPER_PARENT
    ):
      _fail_invalid("ZooKeeper container or HBase root does not match the binding UUID")

  if name == INITIALIZE_BINDING_JOURNAL:
    _canonical_uuid(parameters["initialization.authorization.id"], "initialization.authorization.id")
    _canonical_uuid(parameters["initialization.challenge"], "initialization.challenge")
    _require_hash(
      parameters["initialization.prepare.request.hash"],
      "initialization.prepare.request.hash",
    )
  if name == PROVISION_HDFS_NAMESPACE:
    _validate_hdfs_ownership(parameters)
  if name == PROVISION_ZOOKEEPER_NAMESPACE:
    _validate_zookeeper_policies(parameters, envelope["bindingId"])
  consumer_commands = {
    PREPARE_HDFS_CONSUMER,
    PREPARE_ZOOKEEPER_CONSUMER,
    VERIFY_HDFS_CONSUMER,
    VERIFY_ZOOKEEPER_CONSUMER,
  }
  if name in consumer_commands:
    hdfs = name in {PREPARE_HDFS_CONSUMER, VERIFY_HDFS_CONSUMER}
    verification = name in {VERIFY_HDFS_CONSUMER, VERIFY_ZOOKEEPER_CONSUMER}
    _positive_integer(parameters["host.id"], "host.id")
    if _SAFE_PACKAGE.fullmatch(parameters["client.package.name"]) is None:
      _fail_invalid("client.package.name is not a bounded operating-system package name")
    expected_kind = "HADOOP_CLIENT" if hdfs else "HBASE_CLIENT"
    if parameters["client.software.kind"] != expected_kind:
      _fail_invalid("client.software.kind does not match the managed dependency type")
    _require_software_version(parameters["client.software.semantic.version"])
    if verification:
      _require_package_version(
        parameters["client.package.version"], "client.package.version"
      )
      _canonical_uuid(
        parameters["preparation.observation.id"], "preparation.observation.id"
      )
      _require_hash(
        parameters["preparation.request.hash"], "preparation.request.hash"
      )
      _require_hash(
        parameters["preparation.observation.fingerprint"],
        "preparation.observation.fingerprint",
      )
    _require_nonblank(parameters["consumer.user"], "consumer.user")
    _require_hash(parameters["identity.fingerprint"], "identity.fingerprint")
    _require_hash(parameters["client.config.fingerprint"], "client.config.fingerprint")
    secure = parameters["security.mode"] == "KERBEROS"
    if secure:
      _require_principal_pattern(
        parameters.get("consumer.kerberos.principal.pattern"),
        parameters["consumer.user"],
      )
      if not _CONSUMER_SECURITY_PROOFS.issubset(parameters):
        _fail_invalid("Kerberos consumer command is missing typed security proofs")
      for field in _CONSUMER_SECURITY_PROOFS:
        _require_hash(parameters[field], field)
    elif (
      "consumer.kerberos.principal.pattern" in parameters
      or not _CONSUMER_SECURITY_PROOFS.isdisjoint(parameters)
    ):
      _fail_invalid("Insecure consumer command carries Kerberos security proofs")
    client_config = _parse_client_config(parameters["client.config.json"])
    if parameters["client.config.fingerprint"] != client_config_fingerprint(client_config):
      _fail_invalid("client.config.fingerprint does not match client.config.json")
    if hdfs:
      expected_group = _require_nonblank(
        parameters["expected.owner.group"], "expected.owner.group"
      )
      if parameters["expected.directory.mode"] != "0700" and not (
        parameters["expected.directory.mode"] == "0750" and expected_group != "hadoop"
      ):
        _fail_invalid("Expected HDFS ownership is not private per consumer")
      if (
        set(client_config["coreSite"])
        - (_CORE_SITE_KEYS | ({_AUTH_TO_LOCAL} if secure else set()))
        or set(client_config["hdfsSite"])
        - _allowed_hdfs_client_keys(client_config["hdfsSite"])
        or client_config["zooKeeperClient"]
        or client_config["coreSite"].get("fs.defaultFS")
        != parameters["expected.default.fs"]
      ):
        _fail_invalid("HDFS client config is outside the approved snapshot")
      _validate_hdfs_client_values(
        parameters["security.mode"],
        client_config["coreSite"],
        client_config["hdfsSite"],
      )
      consumer_rules = client_config["coreSite"].get(_AUTH_TO_LOCAL)
      if secure:
        if (
          consumer_rules is None
          or parameters["consumer.mapping.rules.fingerprint"]
          != consumer_mapping_rules_fingerprint(consumer_rules)
        ):
          _fail_invalid(
            "Consumer auth-to-local rules do not match their approved fingerprint"
          )
      elif consumer_rules is not None:
        _fail_invalid(
          "Insecure HDFS client config carries consumer auth-to-local rules"
        )
    elif (
      client_config["coreSite"]
      or client_config["hdfsSite"]
      or set(client_config["zooKeeperClient"]) - _ZOOKEEPER_KEYS
      or client_config["zooKeeperClient"].get("zookeeper.znode.parent")
      != parameters["expected.namespace.znode"]
    ):
      _fail_invalid("ZooKeeper client config is outside the approved snapshot")
    else:
      _validate_zookeeper_client_values(
        parameters["security.mode"], client_config["zooKeeperClient"]
      )
      sasl_id = parameters.get("expected.consumer.zk.sasl.id")
      if secure:
        _require_identity(sasl_id, "expected.consumer.zk.sasl.id")
        if sasl_id != parameters["consumer.user"]:
          _fail_invalid("Expected ZooKeeper SASL identity differs from consumer.user")
      elif sasl_id is not None:
        _fail_invalid("Insecure ZooKeeper consumer command carries a SASL identity")

  command = ManagedDependencyCommand(name, envelope, parameters)
  if command.request_hash != managed_dependency_request_hash(command):
    _fail_invalid("immutableRequestHash does not match the command content")
  return command


def parse_managed_dependency_bundle(value):
  base_fields = {
    "schemaVersion",
    "hostId",
    "consumerUser",
    "identityFingerprint",
    "commands",
    "immutableBundleHash",
  }
  if not isinstance(value, dict) or not base_fields.issubset(value):
    _fail_invalid("Managed dependency bundle contains missing or unknown fields")
  schema_version = value["schemaVersion"]
  fields = base_fields | ({"preparationBindingIds"} if schema_version == BUNDLE_SCHEMA_VERSION else set())
  if set(value) != fields:
    _fail_invalid("Managed dependency bundle contains missing or unknown fields")
  if type(schema_version) is not int or schema_version not in {1, BUNDLE_SCHEMA_VERSION}:
    _fail_invalid("Managed dependency bundle schemaVersion is unsupported")
  host_id = _positive_integer(value["hostId"], "hostId")
  consumer_user = _require_nonblank(value["consumerUser"], "consumerUser")
  identity_fingerprint = value["identityFingerprint"]
  _require_hash(identity_fingerprint, "identityFingerprint")
  raw_commands = value["commands"]
  if not isinstance(raw_commands, list) or not 1 <= len(raw_commands) <= 2:
    _fail_invalid("Managed dependency bundle requires one or two PREPARE commands")
  commands = tuple(parse_managed_dependency_command(item) for item in raw_commands)
  services = set()
  binding_ids = set()
  security_modes = set()
  mapping_profiles = set()
  for command in commands:
    if command.name not in {PREPARE_HDFS_CONSUMER, PREPARE_ZOOKEEPER_CONSUMER}:
      _fail_invalid("Managed dependency bundles contain only consumer PREPARE commands")
    if command.provider_service in services or command.binding_id in binding_ids:
      _fail_invalid("Managed dependency bundle contains a duplicate type or binding UUID")
    services.add(command.provider_service)
    binding_ids.add(command.binding_id)
    security_modes.add(command.parameters["security.mode"])
    mapping_profile = command.parameters.get("consumer.mapping.profile.fingerprint")
    if mapping_profile is not None:
      mapping_profiles.add(mapping_profile)
    if (
      command.parameters["host.id"] != str(host_id)
      or command.parameters["consumer.user"] != consumer_user
      or command.parameters["identity.fingerprint"] != identity_fingerprint
    ):
      _fail_invalid("Managed dependency bundle commands target different hosts or identities")
  if (
    len(security_modes) != 1
    or len(mapping_profiles) > 1
    or "KERBEROS" in security_modes
    and len(mapping_profiles) != 1
    or "INSECURE" in security_modes
    and mapping_profiles
  ):
    _fail_invalid("Managed dependency bundle commands use different security profiles")
  command_binding_ids = {command.binding_id for command in commands}
  if schema_version == 1:
    preparation_binding_ids = tuple(sorted(command_binding_ids))
  else:
    raw_preparation_ids = value["preparationBindingIds"]
    if not isinstance(raw_preparation_ids, list):
      _fail_invalid("preparationBindingIds must be a list")
    try:
      preparation_binding_ids = tuple(sorted(
        _canonical_uuid(item, "preparationBindingIds") for item in raw_preparation_ids
      ))
    except (TypeError, ValueError):
      _fail_invalid("preparationBindingIds must contain UUID strings")
    if len(set(preparation_binding_ids)) != len(preparation_binding_ids):
      _fail_invalid("preparationBindingIds must be duplicate-free")
    if not set(preparation_binding_ids).issubset(command_binding_ids):
      _fail_invalid("preparationBindingIds must be a subset of command binding IDs")
  bundle_hash = value["immutableBundleHash"]
  _require_hash(bundle_hash, "immutableBundleHash")
  if bundle_hash != managed_dependency_bundle_hash(
    schema_version,
    host_id,
    consumer_user,
    identity_fingerprint,
    commands,
    preparation_binding_ids,
  ):
    _fail_invalid("immutableBundleHash does not match the selected commands")
  return ManagedDependencyCommandBundle(
    schema_version,
    host_id,
    consumer_user,
    identity_fingerprint,
    sorted(commands, key=lambda command: command.provider_service),
    preparation_binding_ids,
    bundle_hash,
  )


def managed_dependency_bundle_hash(
  schema_version, host_id, consumer_user, identity_fingerprint, commands,
  preparation_binding_ids=None
):
  canonical = []
  _append_java_strings(
    canonical,
    "managed-dependency-command-bundle",
    str(schema_version),
    str(host_id),
    consumer_user,
    identity_fingerprint,
  )
  for command in sorted(commands, key=lambda item: item.provider_service):
    _append_java_strings(
      canonical,
      command.provider_service,
      command.binding_id,
      str(command.epoch),
      str(command.snapshot_version),
      command.request_hash,
    )
  if schema_version >= BUNDLE_SCHEMA_VERSION:
    ids = sorted(
      command.binding_id for command in commands
    ) if preparation_binding_ids is None else sorted(preparation_binding_ids)
    _append_java_strings(canonical, "preparation-binding-ids", str(len(ids)))
    for binding_id in ids:
      _append_java_strings(canonical, binding_id)
  return "sha256:" + hashlib.sha256("".join(canonical).encode("utf-8")).hexdigest()


def managed_dependency_request_hash(command):
  canonical = []
  _append_java_strings(
    canonical,
    command.name,
    str(command.envelope["protocolVersion"]),
    command.binding_id,
    command.operation_id,
    str(command.epoch),
    str(command.snapshot_version),
  )
  for key in sorted(command.parameters):
    _append_java_strings(canonical, key, command.parameters[key])
  return "sha256:" + hashlib.sha256("".join(canonical).encode("utf-8")).hexdigest()


def client_config_fingerprint(client_config):
  canonical = json.dumps(
    client_config, sort_keys=True, separators=(",", ":"), ensure_ascii=False
  )
  return "sha256:" + hashlib.sha256(canonical.encode("utf-8")).hexdigest()


def consumer_mapping_rules_fingerprint(rules):
  rules = _require_consumer_mapping_rules(rules)
  canonical = []
  _append_java_strings(
    canonical,
    "managed-hdfs-auth-to-local",
    "1",
    "consumer-local-rules",
    rules.encode("utf-8").hex(),
  )
  return "sha256:" + hashlib.sha256("".join(canonical).encode("utf-8")).hexdigest()


class ManagedDependencyJournal:
  def __init__(self, state_root, expected_uid=None):
    if not isinstance(state_root, str) or not os.path.isabs(state_root):
      raise ManagedDependencyFailure(
        "DEPENDENCY_FENCING_UNCERTAIN", "Journal root must be an absolute path"
      )
    self.state_root = state_root
    self.expected_uid = os.geteuid() if expected_uid is None else expected_uid

  def prepare(self, command):
    self._require_command(command, PREPARE_BINDING_JOURNAL)
    with self._locked_store(command) as store:
      if store.exists(store.journal_name):
        raise ManagedDependencyFailure(
          "DEPENDENCY_FENCING_UNCERTAIN",
          "Binding journal already exists; initialization cannot be repeated",
        )
      pending = store.read_optional(store.pending_name)
      if pending is not None:
        self._validate_pending(command, pending)
        return {"initialization.challenge": pending["challenge"]}
      challenge = str(uuid.UUID(bytes=secrets.token_bytes(16)))
      pending = {
        "schemaVersion": JOURNAL_SCHEMA_VERSION,
        "state": "CHALLENGED",
        "bindingId": command.binding_id,
        "providerClusterId": command.parameters["provider.cluster.id"],
        "providerService": command.provider_service,
        "actionHostId": command.action_host_id,
        "epoch": command.epoch,
        "snapshotVersion": command.snapshot_version,
        "prepareOperationId": command.operation_id,
        "prepareRequestHash": command.request_hash,
        "challenge": challenge,
      }
      store.write(store.pending_name, pending)
      return {"initialization.challenge": challenge}

  def initialize(self, command):
    self._require_command(command, INITIALIZE_BINDING_JOURNAL)
    with self._locked_store(command) as store:
      journal = store.read_optional(store.journal_name)
      if journal is not None:
        self._validate_journal_identity(command, journal)
        if (
          journal["lastOperationId"] == command.operation_id
          and journal["lastRequestHash"] == command.request_hash
          and journal["status"] == "INITIALIZED"
        ):
          return self._initialization_facts(command)
        raise ManagedDependencyFailure(
          "DEPENDENCY_FENCING_UNCERTAIN",
          "Binding journal was already initialized by another operation",
        )
      pending = store.read_optional(store.pending_name)
      if pending is None:
        raise ManagedDependencyFailure(
          "DEPENDENCY_FENCING_UNCERTAIN",
          "Initialization requires the matching local challenge record",
        )
      if pending.get("status") == "INITIALIZED":
        self._validate_journal_identity(command, pending)
        if (
          pending["lastOperationId"] == command.operation_id
          and pending["lastRequestHash"] == command.request_hash
          and pending["initializationChallenge"]
          == command.parameters["initialization.challenge"]
          and pending["prepareRequestHash"]
          == command.parameters["initialization.prepare.request.hash"]
          and pending["initializationAuthorizationId"]
          == command.parameters["initialization.authorization.id"]
        ):
          store.rename(store.pending_name, store.journal_name)
          return dict(pending["resultFacts"])
        raise ManagedDependencyFailure(
          "DEPENDENCY_FENCING_UNCERTAIN",
          "Authorized journal initialization does not match this operation",
        )
      self._validate_pending_identity(command, pending)
      if (
        pending["challenge"] != command.parameters["initialization.challenge"]
        or pending["prepareRequestHash"]
        != command.parameters["initialization.prepare.request.hash"]
      ):
        raise ManagedDependencyFailure(
          "DEPENDENCY_FENCING_UNCERTAIN",
          "Initialization authorization does not match the local challenge",
        )
      journal = {
        "schemaVersion": JOURNAL_SCHEMA_VERSION,
        "bindingId": command.binding_id,
        "providerClusterId": command.parameters["provider.cluster.id"],
        "providerService": command.provider_service,
        "actionHostId": command.action_host_id,
        "highestEpoch": command.epoch,
        "snapshotVersion": command.snapshot_version,
        "lastCommandName": command.name,
        "lastOperationId": command.operation_id,
        "lastRequestHash": command.request_hash,
        "initializationAuthorizationId": command.parameters[
          "initialization.authorization.id"
        ],
        "initializationChallenge": command.parameters["initialization.challenge"],
        "prepareRequestHash": command.parameters["initialization.prepare.request.hash"],
        "status": "INITIALIZED",
        "tombstoned": False,
        "completedSteps": [],
        "resultFacts": self._initialization_facts(command),
      }
      store.write(store.pending_name, journal)
      store.rename(store.pending_name, store.journal_name)
      return dict(journal["resultFacts"])

  def execute(self, command, mutation):
    if command.name not in {
      PROVISION_HDFS_NAMESPACE,
      PROVISION_ZOOKEEPER_NAMESPACE,
      INVALIDATE_BINDING_EPOCH,
    }:
      _fail_invalid("Journal execution requires a provider mutation command")
    with self._locked_store(command) as store:
      journal = store.read_optional(store.journal_name)
      if journal is None:
        raise ManagedDependencyFailure(
          "DEPENDENCY_FENCING_UNCERTAIN",
          "Provider mutation requires a previously initialized binding journal",
        )
      self._validate_journal_identity(command, journal)
      same_operation = journal["lastOperationId"] == command.operation_id
      if same_operation and journal["lastRequestHash"] != command.request_hash:
        raise ManagedDependencyFailure(
          "DEPENDENCY_OPERATION_STALE",
          "Operation UUID was reused with different immutable content",
        )
      if journal["tombstoned"]:
        if (
          command.name == INVALIDATE_BINDING_EPOCH
          and same_operation
          and journal["status"] == "SUCCEEDED"
        ):
          return dict(journal["resultFacts"])
        raise ManagedDependencyFailure(
          "DEPENDENCY_OPERATION_STALE", "Provider command epoch is retired"
        )
      if command.epoch < journal["highestEpoch"]:
        raise ManagedDependencyFailure(
          "DEPENDENCY_OPERATION_STALE", "Provider command epoch is retired"
        )
      if same_operation and journal["status"] == "SUCCEEDED":
        return dict(journal["resultFacts"])
      if not same_operation and (
        command.epoch == journal["highestEpoch"] and journal["status"] != "INITIALIZED"
      ):
        raise ManagedDependencyFailure(
          "DEPENDENCY_OPERATION_STALE",
          "A different operation already owns this binding epoch",
        )

      if not same_operation:
        journal["completedSteps"] = []
        journal["resultFacts"] = {}
      journal.update(
        {
          "highestEpoch": command.epoch,
          "snapshotVersion": command.snapshot_version,
          "lastCommandName": command.name,
          "lastOperationId": command.operation_id,
          "lastRequestHash": command.request_hash,
          "status": "RUNNING",
        }
      )
      store.write(store.journal_name, journal)
      progress = _JournalProgress(store, journal)
      try:
        result = mutation(progress)
        if not isinstance(result, dict):
          raise ManagedDependencyFailure(
            "DEPENDENCY_COMMAND_INVALID", "Provider mutation returned invalid facts"
          )
        journal["resultFacts"] = dict(result)
        journal["status"] = "SUCCEEDED"
        if command.name == INVALIDATE_BINDING_EPOCH:
          journal["tombstoned"] = True
        store.write(store.journal_name, journal)
        return dict(result)
      except ManagedDependencyFailure:
        journal["status"] = "FAILED"
        store.write(store.journal_name, journal)
        raise
      except Exception as error:
        journal["status"] = "FAILED"
        store.write(store.journal_name, journal)
        raise ManagedDependencyFailure(
          "DEPENDENCY_PROVIDER_ACTION_FAILED",
          "Provider mutation failed; inspect the protected agent log",
        ) from error

  @contextmanager
  def _locked_store(self, command):
    store = _JournalStore(
      self.state_root, command.provider_service, command.binding_id, self.expected_uid
    )
    with store.locked():
      yield store

  @staticmethod
  def _require_command(command, expected_name):
    if not isinstance(command, ManagedDependencyCommand) or command.name != expected_name:
      _fail_invalid(f"Expected {expected_name}")

  @staticmethod
  def _validate_pending(command, pending):
    ManagedDependencyJournal._validate_pending_identity(command, pending)
    if (
      pending["prepareOperationId"] != command.operation_id
      or pending["prepareRequestHash"] != command.request_hash
    ):
      raise ManagedDependencyFailure(
        "DEPENDENCY_FENCING_UNCERTAIN",
        "Another journal challenge already exists for this binding",
      )

  @staticmethod
  def _validate_pending_identity(command, pending):
    required = {
      "actionHostId",
      "bindingId",
      "challenge",
      "epoch",
      "prepareOperationId",
      "prepareRequestHash",
      "providerClusterId",
      "providerService",
      "schemaVersion",
      "snapshotVersion",
      "state",
    }
    if set(pending) != required or pending.get("state") != "CHALLENGED":
      raise ManagedDependencyFailure(
        "DEPENDENCY_FENCING_UNCERTAIN", "Local journal challenge is corrupt"
      )
    try:
      if type(pending["schemaVersion"]) is not int:
        raise ValueError("invalid pending schema version")
      _canonical_uuid(pending["challenge"], "initialization.challenge")
      _canonical_uuid(pending["prepareOperationId"], "prepareOperationId")
      _require_hash(pending["prepareRequestHash"], "prepareRequestHash")
      _positive_integer(pending["providerClusterId"], "providerClusterId")
      _positive_integer(pending["actionHostId"], "actionHostId")
      if (
        type(pending["epoch"]) is not int
        or type(pending["snapshotVersion"]) is not int
        or not 0 < pending["epoch"] <= _MAX_SIGNED_LONG
        or not 0 < pending["snapshotVersion"] <= _MAX_SIGNED_LONG
      ):
        raise ValueError("invalid pending numeric field")
    except (ManagedDependencyFailure, ValueError) as error:
      raise ManagedDependencyFailure(
        "DEPENDENCY_FENCING_UNCERTAIN", "Local journal challenge is corrupt"
      ) from error
    expected = {
      "schemaVersion": JOURNAL_SCHEMA_VERSION,
      "bindingId": command.binding_id,
      "providerClusterId": command.parameters["provider.cluster.id"],
      "providerService": command.provider_service,
      "actionHostId": command.action_host_id,
      "epoch": command.epoch,
      "snapshotVersion": command.snapshot_version,
    }
    if any(pending.get(key) != value for key, value in expected.items()):
      raise ManagedDependencyFailure(
        "DEPENDENCY_FENCING_UNCERTAIN",
        "Local journal challenge does not match this binding and pinned host",
      )

  @staticmethod
  def _validate_journal_identity(command, journal):
    required = {
      "actionHostId",
      "bindingId",
      "completedSteps",
      "highestEpoch",
      "initializationAuthorizationId",
      "initializationChallenge",
      "lastCommandName",
      "lastOperationId",
      "lastRequestHash",
      "prepareRequestHash",
      "providerClusterId",
      "providerService",
      "resultFacts",
      "schemaVersion",
      "snapshotVersion",
      "status",
      "tombstoned",
    }
    if (
      set(journal) != required
      or type(journal.get("schemaVersion")) is not int
      or journal.get("status") not in {"INITIALIZED", "RUNNING", "SUCCEEDED", "FAILED"}
      or type(journal.get("highestEpoch")) is not int
      or not 0 < journal.get("highestEpoch", 0) <= _MAX_SIGNED_LONG
      or type(journal.get("snapshotVersion")) is not int
      or not 0 < journal.get("snapshotVersion", 0) <= _MAX_SIGNED_LONG
      or type(journal.get("tombstoned")) is not bool
      or not isinstance(journal.get("completedSteps"), list)
      or not isinstance(journal.get("resultFacts"), dict)
    ):
      raise ManagedDependencyFailure(
        "DEPENDENCY_FENCING_UNCERTAIN", "Binding journal is structurally invalid"
      )
    try:
      _canonical_uuid(
        journal["initializationAuthorizationId"], "initializationAuthorizationId"
      )
      _canonical_uuid(
        journal["initializationChallenge"], "initializationChallenge"
      )
      _canonical_uuid(journal["lastOperationId"], "lastOperationId")
      _require_hash(journal["prepareRequestHash"], "prepareRequestHash")
      _require_hash(journal["lastRequestHash"], "lastRequestHash")
      _positive_integer(journal["providerClusterId"], "providerClusterId")
      _positive_integer(journal["actionHostId"], "actionHostId")
      if journal["lastCommandName"] not in _PROVIDER_COMMANDS:
        raise ValueError("invalid last command")
      if (
        any(
          not isinstance(step, str) or not step or len(step) > 64
          for step in journal["completedSteps"]
        )
        or len(set(journal["completedSteps"])) != len(journal["completedSteps"])
        or any(
          not isinstance(key, str) or not isinstance(value, str)
          for key, value in journal["resultFacts"].items()
        )
      ):
        raise ValueError("invalid journal progress or facts")
    except (ManagedDependencyFailure, ValueError) as error:
      raise ManagedDependencyFailure(
        "DEPENDENCY_FENCING_UNCERTAIN", "Binding journal is structurally invalid"
      ) from error
    expected = {
      "schemaVersion": JOURNAL_SCHEMA_VERSION,
      "bindingId": command.binding_id,
      "providerClusterId": command.parameters["provider.cluster.id"],
      "providerService": command.provider_service,
      "actionHostId": command.action_host_id,
    }
    if any(journal.get(key) != value for key, value in expected.items()):
      raise ManagedDependencyFailure(
        "DEPENDENCY_FENCING_UNCERTAIN",
        "Binding journal identity or pinned action host does not match",
      )

  @staticmethod
  def _initialization_facts(command):
    return {
      "initialization.authorization.id": command.parameters[
        "initialization.authorization.id"
      ],
      "journal.initialized": "true",
      "provider.action.host.id": command.action_host_id,
    }


class _JournalProgress:
  def __init__(self, store, journal):
    self.store = store
    self.journal = journal

  @property
  def completed_steps(self):
    return tuple(self.journal["completedSteps"])

  def checkpoint(self, step):
    if not isinstance(step, str) or not step or len(step) > 64:
      _fail_invalid("Journal checkpoint name is invalid")
    if step not in self.journal["completedSteps"]:
      self.journal["completedSteps"].append(step)
      self.store.write(self.store.journal_name, self.journal)


class _JournalStore:
  def __init__(self, root, service, binding_id, expected_uid):
    self.root = root
    self.expected_uid = expected_uid
    prefix = f"{service.lower()}-{binding_id}"
    self.lock_name = prefix + ".lock"
    self.pending_name = prefix + ".pending.json"
    self.journal_name = prefix + ".journal.json"
    self.directory_descriptor = None
    self.lock_descriptor = None

  @contextmanager
  def locked(self):
    self._open_root()
    try:
      self.lock_descriptor = self._open_secure(
        self.lock_name, os.O_RDWR | os.O_CREAT, 0o600
      )
      fcntl.flock(self.lock_descriptor, fcntl.LOCK_EX)
      yield self
    finally:
      if self.lock_descriptor is not None:
        try:
          fcntl.flock(self.lock_descriptor, fcntl.LOCK_UN)
        finally:
          os.close(self.lock_descriptor)
          self.lock_descriptor = None
      if self.directory_descriptor is not None:
        os.close(self.directory_descriptor)
        self.directory_descriptor = None

  def exists(self, name):
    try:
      file_stat = os.stat(name, dir_fd=self.directory_descriptor, follow_symlinks=False)
    except FileNotFoundError:
      return False
    self._validate_file_stat(file_stat)
    return True

  def read_optional(self, name):
    try:
      descriptor = self._open_secure(name, os.O_RDONLY)
    except FileNotFoundError:
      return None
    try:
      chunks = []
      remaining = 65537
      while remaining:
        chunk = os.read(descriptor, min(remaining, 8192))
        if not chunk:
          break
        chunks.append(chunk)
        remaining -= len(chunk)
      if remaining == 0:
        raise ValueError("journal record exceeds 64 KiB")
      value = json.loads(b"".join(chunks).decode("utf-8"))
      if not isinstance(value, dict):
        raise ValueError("journal record is not an object")
      return value
    except (UnicodeDecodeError, ValueError, json.JSONDecodeError) as error:
      raise ManagedDependencyFailure(
        "DEPENDENCY_FENCING_UNCERTAIN",
        "Binding journal is unreadable or corrupt",
      ) from error
    finally:
      os.close(descriptor)

  def write(self, name, value):
    encoded = (json.dumps(value, sort_keys=True, separators=(",", ":")) + "\n").encode(
      "utf-8"
    )
    if len(encoded) > 65536:
      raise ManagedDependencyFailure(
        "DEPENDENCY_FENCING_UNCERTAIN", "Binding journal exceeds 64 KiB"
      )
    temporary_name = f".{name}.{secrets.token_hex(12)}.tmp"
    descriptor = None
    try:
      descriptor = self._open_secure(
        temporary_name, os.O_WRONLY | os.O_CREAT | os.O_EXCL, 0o600
      )
      view = memoryview(encoded)
      while view:
        written = os.write(descriptor, view)
        if written <= 0:
          raise OSError("journal write made no progress")
        view = view[written:]
      os.fsync(descriptor)
      os.close(descriptor)
      descriptor = None
      os.replace(
        temporary_name,
        name,
        src_dir_fd=self.directory_descriptor,
        dst_dir_fd=self.directory_descriptor,
      )
      os.fsync(self.directory_descriptor)
    finally:
      if descriptor is not None:
        os.close(descriptor)
      try:
        os.unlink(temporary_name, dir_fd=self.directory_descriptor)
      except FileNotFoundError:
        pass

  def rename(self, source, destination):
    os.replace(
      source,
      destination,
      src_dir_fd=self.directory_descriptor,
      dst_dir_fd=self.directory_descriptor,
    )
    os.fsync(self.directory_descriptor)

  def _open_root(self):
    parent, name = os.path.split(self.root)
    parent_descriptor = None
    try:
      flags = os.O_RDONLY | os.O_DIRECTORY | os.O_CLOEXEC
      if not hasattr(os, "O_NOFOLLOW"):
        raise ManagedDependencyFailure(
          "DEPENDENCY_FENCING_UNCERTAIN", "Journal storage requires O_NOFOLLOW"
        )
      parent_descriptor = os.open(parent, flags | os.O_NOFOLLOW)
      parent_stat = os.fstat(parent_descriptor)
      if (
        not stat.S_ISDIR(parent_stat.st_mode)
        or parent_stat.st_uid != self.expected_uid
        or stat.S_IMODE(parent_stat.st_mode) & 0o022
      ):
        raise ManagedDependencyFailure(
          "DEPENDENCY_FENCING_UNCERTAIN",
          "Journal parent must be owned by the agent user and not group/world writable",
        )
      try:
        os.mkdir(name, 0o700, dir_fd=parent_descriptor)
        os.fsync(parent_descriptor)
      except FileExistsError:
        pass
      self.directory_descriptor = os.open(
        name, flags | os.O_NOFOLLOW, dir_fd=parent_descriptor
      )
    except ManagedDependencyFailure:
      raise
    except OSError as error:
      raise ManagedDependencyFailure(
        "DEPENDENCY_FENCING_UNCERTAIN", "Journal root is missing or unsafe"
      ) from error
    finally:
      if parent_descriptor is not None:
        os.close(parent_descriptor)
    root_stat = os.fstat(self.directory_descriptor)
    if (
      not stat.S_ISDIR(root_stat.st_mode)
      or root_stat.st_uid != self.expected_uid
      or stat.S_IMODE(root_stat.st_mode) != 0o700
    ):
      os.close(self.directory_descriptor)
      self.directory_descriptor = None
      raise ManagedDependencyFailure(
        "DEPENDENCY_FENCING_UNCERTAIN",
        "Journal root must be owned by the agent user with mode 0700",
      )

  def _open_secure(self, name, flags, mode=0o600):
    try:
      descriptor = os.open(
        name,
        flags | os.O_CLOEXEC | os.O_NOFOLLOW,
        mode,
        dir_fd=self.directory_descriptor,
      )
    except FileNotFoundError:
      raise
    except OSError as error:
      raise ManagedDependencyFailure(
        "DEPENDENCY_FENCING_UNCERTAIN", "Journal or lock path is unsafe"
      ) from error
    try:
      self._validate_file_stat(os.fstat(descriptor))
      return descriptor
    except BaseException:
      os.close(descriptor)
      raise

  def _validate_file_stat(self, file_stat):
    if (
      not stat.S_ISREG(file_stat.st_mode)
      or file_stat.st_uid != self.expected_uid
      or stat.S_IMODE(file_stat.st_mode) != 0o600
      or file_stat.st_nlink != 1
    ):
      raise ManagedDependencyFailure(
        "DEPENDENCY_FENCING_UNCERTAIN",
        "Journal or lock file ownership, mode, or link count is unsafe",
      )


def _parse_client_config(value):
  try:
    encoded_size = len(value.encode("utf-8")) if isinstance(value, str) else -1
  except UnicodeEncodeError as error:
    raise ManagedDependencyFailure(
      "DEPENDENCY_COMMAND_INVALID", "client.config.json contains invalid Unicode"
    ) from error
  if encoded_size < 0 or encoded_size > _MAX_CLIENT_CONFIG_BYTES:
    _fail_invalid("client.config.json is missing or exceeds the size limit")
  _require_json_nesting(value, 2)
  try:
    parsed = json.loads(value)
  except (TypeError, ValueError, UnicodeDecodeError, RecursionError, json.JSONDecodeError) as error:
    raise ManagedDependencyFailure(
      "DEPENDENCY_COMMAND_INVALID", "client.config.json is not valid JSON"
    ) from error
  expected = {"coreSite", "hdfsSite", "zooKeeperClient"}
  if not isinstance(parsed, dict) or set(parsed) != expected:
    _fail_invalid("client.config.json must contain exactly the approved config maps")
  for field in expected:
    config = parsed[field]
    if (
      not isinstance(config, dict)
      or len(config) > 64
      or any(
        not isinstance(key, str)
        or not isinstance(item, str)
        or len(key) > 256
        or len(item.encode("utf-8"))
        > (_MAX_CONSUMER_RULE_BYTES if field == "coreSite" and key == _AUTH_TO_LOCAL else 4096)
        for key, item in config.items()
      )
    ):
      _fail_invalid(f"{field} must contain only bounded string properties")
  canonical = json.dumps(parsed, sort_keys=True, separators=(",", ":"), ensure_ascii=False)
  if value != canonical:
    _fail_invalid("client.config.json must use canonical sorted compact JSON")
  return parsed


def _require_consumer_mapping_rules(value):
  try:
    encoded = value.encode("utf-8") if isinstance(value, str) else b""
  except UnicodeEncodeError as error:
    raise ManagedDependencyFailure(
      "DEPENDENCY_COMMAND_INVALID", "Consumer auth-to-local rules contain invalid Unicode"
    ) from error
  rules = value.split("\n") if isinstance(value, str) else []
  if (
    not encoded
    or len(encoded) > _MAX_CONSUMER_RULE_BYTES
    or "\r" in value
    or value.startswith("\n")
    or value.endswith("\n")
    or len(rules) > _MAX_CONSUMER_RULE_COUNT
    or any(not rule for rule in rules)
  ):
    _fail_invalid("Consumer auth-to-local rules are empty, noncanonical, or too large")
  return value


def _require_json_nesting(value, maximum_depth):
  depth = 0
  quoted = False
  escaped = False
  for character in value:
    if quoted:
      if escaped:
        escaped = False
      elif character == "\\":
        escaped = True
      elif character == '"':
        quoted = False
    elif character == '"':
      quoted = True
    elif character in "[{":
      depth += 1
      if depth > maximum_depth:
        _fail_invalid("client.config.json exceeds the supported nesting depth")
    elif character in "]}":
      depth -= 1


def _allowed_hdfs_client_keys(config):
  name_service = config.get("dfs.nameservices")
  if name_service is None:
    return _HDFS_SITE_KEYS
  if _HA_TOKEN.fullmatch(name_service) is None:
    _fail_invalid("dfs.nameservices must identify one approved HA nameservice")
  ids_key = f"dfs.ha.namenodes.{name_service}"
  name_node_ids = config.get(ids_key, "").split(",")
  if len(name_node_ids) < 2 or any(
    _HA_TOKEN.fullmatch(name_node_id) is None for name_node_id in name_node_ids
  ):
    _fail_invalid("HA client config contains invalid NameNode IDs")
  dynamic = {ids_key}
  dynamic.update(
    f"dfs.namenode.rpc-address.{name_service}.{name_node_id}"
    for name_node_id in name_node_ids
  )
  provider_key = f"dfs.client.failover.proxy.provider.{name_service}"
  dynamic.add(provider_key)
  if (
    not dynamic.issubset(config)
    or config.get(provider_key) != _STANDARD_HA_PROVIDER
  ):
    _fail_invalid("HA client config is incomplete or uses an unsupported failover provider")
  return _HDFS_SITE_KEYS | dynamic


def _validate_hdfs_client_values(security_mode, core_site, hdfs_site):
  _validate_enum(
    core_site,
    "hadoop.security.authentication",
    {"simple", "kerberos"},
  )
  _validate_enum(
    core_site,
    "hadoop.rpc.protection",
    {"authentication", "integrity", "privacy"},
  )
  _validate_boolean(core_site, "hadoop.security.authorization")
  _validate_boolean(core_site, "ipc.client.fallback-to-simple-auth-allowed")
  _validate_integer(core_site, "ipc.client.connect.max.retries", 0, 50)
  _validate_integer(core_site, "ipc.client.connect.retry.interval", 100, 60_000)
  _validate_boolean(hdfs_site, "dfs.block.access.token.enable")
  _validate_integer(hdfs_site, "dfs.client.failover.connection.retries", 0, 100)
  _validate_integer(
    hdfs_site, "dfs.client.failover.connection.retries.on.timeouts", 0, 100
  )
  _validate_integer(hdfs_site, "dfs.client.socket-timeout", 1_000, 600_000)
  _validate_boolean(hdfs_site, "dfs.client.use.datanode.hostname")
  _validate_boolean(hdfs_site, "dfs.encrypt.data.transfer")
  protection = hdfs_site.get("dfs.data.transfer.protection")
  if protection is not None and any(
    value.strip() not in {"authentication", "integrity", "privacy"}
    for value in protection.split(",")
  ):
    _fail_invalid("dfs.data.transfer.protection contains an unsupported value")
  if security_mode == "KERBEROS":
    if (
      core_site.get("hadoop.security.authentication") != "kerberos"
      or core_site.get("ipc.client.fallback-to-simple-auth-allowed") != "false"
      or not hdfs_site.get("dfs.namenode.kerberos.principal")
      or not hdfs_site.get("dfs.datanode.kerberos.principal")
    ):
      _fail_invalid(
        "Secure HDFS client config requires Kerberos principals and no simple fallback"
      )
  elif core_site.get("hadoop.security.authentication") == "kerberos":
    _fail_invalid("Insecure HDFS client config cannot enable Kerberos")


def _validate_zookeeper_client_values(security_mode, config):
  _validate_boolean(config, "zookeeper.sasl.client")
  context = config.get("zookeeper.sasl.clientconfig")
  if context is not None and context != "Client":
    _fail_invalid("zookeeper.sasl.clientconfig must use Client")
  service = config.get("zookeeper.sasl.client.username")
  if service is not None and _SAFE_IDENTITY.fullmatch(service) is None:
    _fail_invalid("zookeeper.sasl.client.username is invalid")
  if security_mode == "KERBEROS" and (
    config.get("zookeeper.sasl.client") != "true"
    or not service
    or context != "Client"
  ):
    _fail_invalid("Secure ZooKeeper client config requires exact SASL settings")
  if security_mode == "INSECURE" and config.get("zookeeper.sasl.client") == "true":
    _fail_invalid("Insecure ZooKeeper client config cannot enable SASL")


def _validate_enum(values, key, allowed):
  value = values.get(key)
  if value is not None and value not in allowed:
    _fail_invalid(f"{key} has an unsupported value")


def _validate_boolean(values, key):
  _validate_enum(values, key, {"true", "false"})


def _validate_integer(values, key, minimum, maximum):
  value = values.get(key)
  if value is None:
    return
  if not value.isdigit() or str(int(value)) != value or not minimum <= int(value) <= maximum:
    _fail_invalid(f"{key} must be a canonical integer in the supported range")


def _validate_hdfs_ownership(parameters):
  _require_nonblank(parameters["owner.user"], "owner.user")
  group = _require_nonblank(parameters["owner.group"], "owner.group")
  mode = parameters["directory.mode"]
  if mode != "0700" and not (mode == "0750" and group != "hadoop"):
    _fail_invalid("HDFS ownership must enforce private per-consumer authority")


def _validate_zookeeper_policies(parameters, binding_id):
  _require_nonblank(parameters["owner.user"], "owner.user")
  if parameters["namespace.parent.znode"] != ZOOKEEPER_PARENT:
    _fail_invalid("ZooKeeper parent is not reserved for managed HBase")
  client_port = _positive_integer(parameters["expected.client.port"], "expected.client.port")
  if client_port > 65535:
    _fail_invalid("expected.client.port must be between 1 and 65535")
  quorum = _require_nonblank(parameters["expected.quorum"], "expected.quorum")
  hosts = quorum.split(",")
  if (
    any(_HA_TOKEN.fullmatch(host) is None for host in hosts)
    or len(hosts) != len(set(hosts))
    or hosts != sorted(hosts)
  ):
    _fail_invalid("expected.quorum must contain canonical unique sorted hosts")
  secure = parameters["security.mode"] == "KERBEROS"
  expected_parent = "PROVIDER_ADMIN_CREATE_DELETE" if secure else "INSECURE_PROVIDER_PREPARED"
  expected_subtree = "SASL_CONSUMER_PRIVATE" if secure else "INSECURE_BINDING_SCOPED"
  if (
    parameters["parent.acl.policy"] != expected_parent
    or parameters["subtree.acl.policy"] != expected_subtree
  ):
    _fail_invalid("ZooKeeper ACL policies do not match security.mode")
  sasl_id = parameters.get("consumer.zk.sasl.id")
  ledger = parameters.get("namespace.ledger.znode")
  if secure:
    _require_identity(sasl_id, "consumer.zk.sasl.id")
    if (
      sasl_id != parameters["owner.user"]
      or ledger != f"{ZOOKEEPER_LEDGER_PARENT}/{binding_id}"
    ):
      _fail_invalid("Secure ZooKeeper handoff identity or ledger is invalid")
  elif sasl_id is not None or ledger is not None:
    _fail_invalid("Insecure ZooKeeper commands carry secure handoff fields")


def _validate_hdfs_namespaces(binding_id, root_value, wal_value):
  root = _parse_hdfs_uri(root_value, "namespace.root.uri")
  wal = _parse_hdfs_uri(wal_value, "namespace.wal.uri")
  expected = f"{HDFS_BASE}/{binding_id}"
  if root.path != expected + "/root" or wal.path != expected + "/wal":
    _fail_invalid("HDFS namespaces do not match the binding UUID")
  if root.netloc != wal.netloc:
    _fail_invalid("HDFS root and WAL authorities differ")
  return root.netloc


def _parse_hdfs_uri(value, field):
  value = _require_nonblank(value, field)
  if "%" in value or "\\" in value or any(character.isspace() for character in value):
    _fail_invalid(f"{field} is not a normalized HDFS URI")
  try:
    parsed = urlsplit(value)
    port = parsed.port
  except ValueError as error:
    raise ManagedDependencyFailure(
      "DEPENDENCY_COMMAND_INVALID", f"{field} is not a valid HDFS URI"
    ) from error
  if (
    parsed.scheme != "hdfs"
    or not parsed.netloc
    or parsed.username is not None
    or parsed.password is not None
    or parsed.query
    or parsed.fragment
    or any(segment in {".", ".."} for segment in parsed.path.split("/"))
    or port is not None and not 0 < port <= 65535
  ):
    _fail_invalid(f"{field} is not a normalized credential-free HDFS URI")
  return parsed


def _append_java_strings(target, *values):
  for value in values:
    if not isinstance(value, str):
      _fail_invalid("Canonical request hash fields must be strings")
    try:
      java_length = len(value.encode("utf-16-be")) // 2
      value.encode("utf-8")
    except UnicodeEncodeError as error:
      raise ManagedDependencyFailure(
        "DEPENDENCY_COMMAND_INVALID", "Command contains an invalid Unicode value"
      ) from error
    target.append(f"{java_length}:{value}")


def _canonical_uuid(value, field):
  if not isinstance(value, str):
    _fail_invalid(f"{field} must be a canonical UUID string")
  try:
    parsed = uuid.UUID(value)
  except (ValueError, AttributeError) as error:
    raise ManagedDependencyFailure(
      "DEPENDENCY_COMMAND_INVALID", f"{field} must be a canonical UUID string"
    ) from error
  canonical = str(parsed)
  if value != canonical:
    _fail_invalid(f"{field} must be a canonical lower-case UUID string")
  return canonical


def _positive_integer(value, field):
  if isinstance(value, bool):
    _fail_invalid(f"{field} must be a positive integer")
  if isinstance(value, int):
    if value <= 0 or value > _MAX_SIGNED_LONG:
      _fail_invalid(f"{field} must be a positive integer")
    return value
  if not isinstance(value, str) or _POSITIVE_INTEGER.fullmatch(value) is None:
    _fail_invalid(f"{field} must be a positive integer")
  result = int(value)
  if result > _MAX_SIGNED_LONG:
    _fail_invalid(f"{field} exceeds the supported integer range")
  return result


def _require_hash(value, field):
  if not isinstance(value, str) or _HASH.fullmatch(value) is None:
    _fail_invalid(f"{field} must use sha256:<64 lower-case hex>")
  return value


def _require_nonblank(value, field):
  if not isinstance(value, str) or not value or value != value.strip():
    _fail_invalid(f"{field} must be a nonblank normalized string")
  return value


def _require_package_version(value, field):
  _require_nonblank(value, field)
  if len(value) > 512 or any(ord(character) < 32 or ord(character) == 127 for character in value):
    _fail_invalid(f"{field} is not a bounded single-line package-manager version")
  return value


def _require_software_version(value):
  _require_nonblank(value, "client.software.semantic.version")
  if len(value) > 128 or _SOFTWARE_VERSION.fullmatch(value) is None:
    _fail_invalid("client.software.semantic.version is not a supported semantic version")
  return value


def _require_identity(value, field):
  _require_nonblank(value, field)
  if _SAFE_IDENTITY.fullmatch(value) is None:
    _fail_invalid(f"{field} must be a bounded simple authorization identity")
  return value


def _require_principal_pattern(value, consumer_user):
  _require_nonblank(value, "consumer.kerberos.principal.pattern")
  _require_identity(consumer_user, "consumer.user")
  prefix = f"{consumer_user}/_HOST@"
  realm = value[len(prefix) :] if value.startswith(prefix) else ""
  if len(value) > 512 or _KERBEROS_REALM.fullmatch(realm) is None:
    _fail_invalid(
      "consumer.kerberos.principal.pattern must match consumer.user/_HOST@REALM"
    )
  return value


def _fail_invalid(message):
  raise ManagedDependencyFailure("DEPENDENCY_COMMAND_INVALID", message)
