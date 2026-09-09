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

import grp
from contextlib import contextmanager
import fcntl
from functools import wraps
import hashlib
import json
import os
import pwd
import re
import secrets
import shutil
import stat
import tempfile
import xml.etree.ElementTree as ElementTree

from ambari_commons.repo_manager import ManagerFactory
from resource_management.core import shell
from resource_management.core.signal_utils import TerminateStrategy
from resource_management.libraries.functions.managed_dependency import (
  PREPARE_HDFS_CONSUMER,
  PREPARE_ZOOKEEPER_CONSUMER,
  VERIFY_HDFS_CONSUMER,
  VERIFY_ZOOKEEPER_CONSUMER,
  ManagedDependencyFailure,
  client_config_fingerprint,
  parse_managed_dependency_bundle,
  parse_managed_dependency_command,
)
from resource_management.libraries.functions.private_kerberos_cache import (
  PrivateKerberosCache,
)
from resource_management.libraries.script.script import Script


BUNDLE_PARAMETER = "managed_dependency_commands"
COMMAND_PARAMETER = "managed_dependency_command"
RESULT_KEY = "managedDependencyResult"
PREPARATION_RESULTS_KEY = "managedDependencyPreparationResults"
MAX_BUNDLE_BYTES = 96 * 1024
MAX_COMMAND_BYTES = 64 * 1024
PROFILE_ROOT = "/etc/hbase/conf-managed"
PACKAGE_CONFIG_ROOTS = ("/usr/bigtop", "/usr/hdp")
ZOOKEEPER_HELPER_JAR = "/var/lib/ambari-agent/tools/zkmigrator.jar"
ZOOKEEPER_HELPER_CLASS = "org.apache.ambari.tools.zk.ManagedDependencyZk"
PROBE_BYTES = 128 * 1024 + 1
COMMAND_TIMEOUT_SECONDS = 60
CLIENT_SOFTWARE_VERSION = re.compile(
  r"[0-9]+(?:\.[0-9]+){1,3}(?:[-+][A-Za-z0-9][A-Za-z0-9._-]*)?\Z",
  re.ASCII,
)
FINGERPRINT = re.compile(r"sha256:[0-9a-f]{64}\Z", re.ASCII)
SECURITY_MANIFEST_FIELDS = {
  "consumerMappingProfileFingerprint": "consumer.mapping.profile.fingerprint",
  "consumerMappingProofFingerprint": "consumer.mapping.proof.fingerprint",
  "consumerMappingRulesFingerprint": "consumer.mapping.rules.fingerprint",
  "providerPairProofFingerprint": "provider.security.pair.proof.fingerprint",
  "providerPolicyFingerprint": "provider.security.policy.fingerprint",
}


def _credential_metadata_matches(metadata, expected_uid, expected_gid, expected_mode):
  return (
    stat.S_ISREG(metadata.st_mode)
    and metadata.st_uid == expected_uid
    and metadata.st_gid == expected_gid
    and stat.S_IMODE(metadata.st_mode) == expected_mode
  )


def has_managed_dependency_selection(profile_root=PROFILE_ROOT):
  return os.path.lexists(os.path.join(profile_root, "active"))


def managed_dependency_requested(profile_root=PROFILE_ROOT):
  raw = Script.get_config().get("commandParams", {}).get(BUNDLE_PARAMETER)
  return (isinstance(raw, str) and bool(raw)) or has_managed_dependency_selection(
    profile_root
  )


def configure_managed_dependency_client(params, profile_root=PROFILE_ROOT):
  bundle = _load_bundle(required=has_managed_dependency_selection(profile_root))
  if bundle is None:
    return None
  if params.hbase_user != bundle.consumer_user:
    raise ManagedDependencyFailure(
      "DEPENDENCY_CLIENT_IDENTITY_MISMATCH",
      "The configured HBase service user does not match the approved identity",
    )
  store = ManagedHBaseClientConfigStore(
    profile_root, params.hbase_conf_dir, params.user_group
  )
  store.select(bundle, params.config["configurations"]["hbase-site"])
  return bundle


@contextmanager
def managed_hbase_configuration(params, profile_root=PROFILE_ROOT):
  bundle = _load_bundle(required=has_managed_dependency_selection(profile_root))
  if bundle is None:
    yield _ManagedHBaseConfiguration(None, None, None)
    return
  if params.hbase_user != bundle.consumer_user:
    raise ManagedDependencyFailure(
      "DEPENDENCY_CLIENT_IDENTITY_MISMATCH",
      "The configured HBase service user does not match the approved identity",
    )
  store = ManagedHBaseClientConfigStore(
    profile_root, params.hbase_conf_dir, params.user_group
  )
  hbase_site = params.config["configurations"]["hbase-site"]
  with store.protect_configuration(bundle, hbase_site):
    configuration = _ManagedHBaseConfiguration(bundle, store, hbase_site)
    yield configuration
    if not configuration.applied:
      raise ManagedDependencyFailure(
        "DEPENDENCY_CLIENT_CONFIG_MISMATCH",
        "The managed HBase configuration did not select its approved client profile",
      )


class _ManagedHBaseConfiguration:
  def __init__(self, bundle, store, hbase_site):
    self.bundle = bundle
    self.store = store
    self.hbase_site = hbase_site
    self.applied = False

  def apply(self):
    if self.applied:
      raise ManagedDependencyFailure(
        "DEPENDENCY_CLIENT_CONFIG_MISMATCH",
        "The managed HBase client profile was selected more than once",
      )
    self.applied = True
    if self.bundle is not None:
      self.store.select_locked(self.bundle, self.hbase_site)
    return self.bundle


def managed_hbase_configuration_guard(configure):
  @wraps(configure)
  def protected(name=None):
    import params

    with managed_hbase_configuration(params) as configuration:
      return configure(name, configuration)

  return protected


def execute_managed_hbase_verification(script, env, role, expected_name):
  import params
  from hbase import hbase

  env.set_params(params)
  command = _load_command(expected_name)
  try:
    bundle = _load_bundle(required=True)
    selected = bundle.command_for(command.provider_service)
    strict_only = {
      "client.package.version",
      "preparation.observation.fingerprint",
      "preparation.observation.id",
      "preparation.request.hash",
    }
    expected_preparation_parameters = {
      key: value for key, value in command.parameters.items() if key not in strict_only
    }
    if (
      selected is None
      or selected.binding_id != command.binding_id
      or selected.epoch != command.epoch
      or selected.snapshot_version != command.snapshot_version
      or selected.request_hash != command.parameters["preparation.request.hash"]
      or selected.parameters != expected_preparation_parameters
    ):
      raise ManagedDependencyFailure(
        "DEPENDENCY_COMMAND_INVALID",
        "The VERIFY command does not match the approved preparation profile and observation lineage",
      )
    hbase(name=role)
    verifier = ManagedHBaseConsumerVerifier(params, role=role)
    if expected_name == VERIFY_HDFS_CONSUMER:
      facts = verifier.verify_hdfs(command)
    elif expected_name == VERIFY_ZOOKEEPER_CONSUMER:
      facts = verifier.verify_zookeeper(command)
    else:
      raise ManagedDependencyFailure(
        "DEPENDENCY_COMMAND_INVALID", "Unsupported HBase dependency verification"
      )
    script.put_structured_out({RESULT_KEY: _result(command, "SUCCEEDED", facts)})
  except ManagedDependencyFailure as error:
    script.put_structured_out(
      {
        RESULT_KEY: _result(
          command,
          "FAILED",
          error.facts,
          error.code,
          error.sanitized_message,
        )
      }
    )
    raise


def report_managed_dependency_preparation(
  script,
  params,
  bundle,
  role,
  role_command=None,
  profile_root=PROFILE_ROOT,
  call=shell.call,
  package_manager=None,
):
  if bundle is None:
    return
  current_role_command = role_command or Script.get_config().get("roleCommand")
  if current_role_command not in {"INSTALL", "CONFIGURE"}:
    return
  verifier = ManagedHBaseConsumerVerifier(
    params,
    profile_root=profile_root,
    call=call,
    package_manager=package_manager,
    role=role,
  )
  results = []
  preparation_commands = bundle.preparation_commands
  if not preparation_commands:
    return
  current = preparation_commands[0]
  try:
    with verifier.store.protect_preparation(bundle):
      for current in preparation_commands:
        facts = verifier.observe_preparation_locked(current)
        results.append(_result(current, "SUCCEEDED", facts))
  except ManagedDependencyFailure as error:
    status = "STALE_REJECTED" if error.code == "DEPENDENCY_OPERATION_STALE" else "FAILED"
    results.append(
      _result(
        current,
        status,
        error.facts,
        error.code,
        error.sanitized_message,
      )
    )
    script.put_structured_out({PREPARATION_RESULTS_KEY: results})
    raise
  script.put_structured_out({PREPARATION_RESULTS_KEY: results})


def _security_manifest(command):
  if command.parameters["security.mode"] != "KERBEROS":
    return {}
  return {
    manifest_field: command.parameters[parameter]
    for manifest_field, parameter in SECURITY_MANIFEST_FIELDS.items()
  }


class ManagedHBaseClientConfigStore:
  def __init__(
    self,
    profile_root,
    hbase_conf_dir,
    group_name,
    expected_uid=None,
    package_config_roots=PACKAGE_CONFIG_ROOTS,
  ):
    self.profile_root = os.path.abspath(profile_root)
    self.hbase_conf_dir = os.path.abspath(hbase_conf_dir)
    self.expected_uid = os.geteuid() if expected_uid is None else expected_uid
    self.package_config_roots = tuple(package_config_roots)
    self.hbase_conf_write_dir = None
    try:
      self.group_id = grp.getgrnam(group_name).gr_gid
    except KeyError as error:
      raise ManagedDependencyFailure(
        "DEPENDENCY_CLIENT_IDENTITY_MISMATCH",
        "The configured HBase service group does not exist on this host",
      ) from error

  def select(self, bundle, hbase_site):
    with self.protect_configuration(bundle, hbase_site):
      return self.select_locked(bundle, hbase_site)

  @contextmanager
  def protect_configuration(self, bundle, hbase_site):
    self._validate_consumer_identity(bundle)
    configs = self._bundle_configs(bundle)
    self._validate_hbase_site(configs["managedHbaseSite"], hbase_site)
    self._ensure_root(require_hbase_conf=False)
    with self._selection_lock():
      self._validate_monotonic_selection(
        self._read_active_manifest(), self._manifest(bundle)
      )
      yield

  def select_locked(self, bundle, hbase_site):
    self._ensure_hbase_conf_directory()
    configs = self._bundle_configs(bundle)
    manifest = self._manifest(bundle)
    profile_name = bundle.immutable_bundle_hash.removeprefix("sha256:")
    profile_path = os.path.join(self.profile_root, "profiles", profile_name)
    expected_files = {
      "manifest.json": (
        json.dumps(manifest, sort_keys=True, separators=(",", ":")) + "\n"
      ).encode("utf-8"),
      "managed-hbase-site.xml": _xml_config(configs["managedHbaseSite"]),
    }
    if configs["coreSite"]:
      expected_files["core-site.xml"] = _xml_config(configs["coreSite"])
      expected_files["hdfs-site.xml"] = _xml_config(configs["hdfsSite"])
    self._ensure_profile(profile_path, expected_files)
    self._select_profile(profile_name, "core-site.xml" in expected_files)
    return profile_path

  @contextmanager
  def protect_verification(self, command):
    self._ensure_root(require_hbase_conf=True)
    with self._selection_lock():
      manifest = self._read_active_manifest()
      selected = (
        manifest.get("commands", {}).get(command.provider_service)
        if isinstance(manifest, dict)
        and isinstance(manifest.get("commands"), dict)
        else None
      )
      expected_security = _security_manifest(command)
      if (
        not isinstance(selected, dict)
        or not isinstance(manifest, dict)
        or manifest.get("schemaVersion") != (2 if expected_security else 1)
        or selected.get("bindingId") != command.binding_id
        or selected.get("epoch") != command.epoch
        or selected.get("snapshotVersion") != command.snapshot_version
        or selected.get("preparationRequestHash")
        != command.parameters["preparation.request.hash"]
        or selected.get("clientConfigFingerprint")
        != command.parameters["client.config.fingerprint"]
        or selected.get("clientPackageName")
        != command.parameters["client.package.name"]
        or selected.get("clientSoftwareKind")
        != command.parameters["client.software.kind"]
        or selected.get("clientSoftwareSemanticVersion")
        != command.parameters["client.software.semantic.version"]
        or any(selected.get(key) != value for key, value in expected_security.items())
      ):
        raise ManagedDependencyFailure(
          "DEPENDENCY_OPERATION_STALE",
          "The active managed HBase client profile does not match the verification command",
        )
      yield

  @contextmanager
  def protect_preparation(self, bundle):
    self._ensure_root(require_hbase_conf=True)
    with self._selection_lock():
      if self._read_active_manifest() != self._manifest(bundle):
        raise ManagedDependencyFailure(
          "DEPENDENCY_OPERATION_STALE",
          "The active managed HBase client profile does not match the preparation task",
        )
      yield

  def read_selected_config(self, provider_service):
    active = self._active_profile_path()
    if provider_service == "HDFS":
      return {
        "coreSite": _read_xml_config(os.path.join(active, "core-site.xml")),
        "hdfsSite": _read_xml_config(os.path.join(active, "hdfs-site.xml")),
        "zooKeeperClient": {},
      }
    return {
      "coreSite": {},
      "hdfsSite": {},
      "zooKeeperClient": {
        key: value
        for key, value in _read_xml_config(
          os.path.join(active, "managed-hbase-site.xml")
        ).items()
        if key.startswith("hbase.zookeeper.") or key.startswith("zookeeper.")
      },
    }

  def _validate_consumer_identity(self, bundle):
    try:
      pwd.getpwnam(bundle.consumer_user)
    except KeyError as error:
      raise ManagedDependencyFailure(
        "DEPENDENCY_CLIENT_IDENTITY_MISMATCH",
        "The approved HBase service user does not exist on this host",
      ) from error

  def _bundle_configs(self, bundle):
    result = {"coreSite": {}, "hdfsSite": {}, "managedHbaseSite": {}}
    hdfs = bundle.command_for("HDFS")
    if hdfs is not None:
      config = json.loads(hdfs.parameters["client.config.json"])
      result["coreSite"] = config["coreSite"]
      result["hdfsSite"] = config["hdfsSite"]
      result["managedHbaseSite"]["hbase.rootdir"] = hdfs.parameters[
        "expected.namespace.root.uri"
      ]
      result["managedHbaseSite"]["hbase.wal.dir"] = hdfs.parameters[
        "expected.namespace.wal.uri"
      ]
    zookeeper = bundle.command_for("ZOOKEEPER")
    if zookeeper is not None:
      config = json.loads(zookeeper.parameters["client.config.json"])
      result["managedHbaseSite"].update(config["zooKeeperClient"])
    return result

  def _validate_hbase_site(self, expected, actual):
    mismatched = [key for key, value in expected.items() if actual.get(key) != value]
    if mismatched:
      raise ManagedDependencyFailure(
        "DEPENDENCY_CLIENT_CONFIG_MISMATCH",
        "HBase dependency settings do not match the approved snapshot: "
        + ",".join(sorted(mismatched)),
      )

  def _ensure_root(self, require_hbase_conf):
    parent = os.path.dirname(self.profile_root)
    _require_directory(parent, self.expected_uid, None, 0o022)
    _ensure_exact_directory(self.profile_root, self.expected_uid, self.group_id, 0o750)
    _ensure_exact_directory(
      os.path.join(self.profile_root, "profiles"),
      self.expected_uid,
      self.group_id,
      0o750,
    )
    if os.path.lexists(self.hbase_conf_dir):
      self._ensure_hbase_conf_directory()
    elif require_hbase_conf:
      raise ManagedDependencyFailure(
        "DEPENDENCY_CLIENT_CONFIG_MISMATCH",
        "The package-managed HBase configuration directory is missing",
      )

  def _ensure_hbase_conf_directory(self):
    self.hbase_conf_write_dir = _resolve_package_config_directory(
      self.hbase_conf_dir, self.expected_uid, self.package_config_roots
    )

  def _manifest(self, bundle):
    commands = {}
    secure = False
    for command in bundle.commands:
      security = _security_manifest(command)
      secure = secure or bool(security)
      commands[command.provider_service] = {
        "bindingId": command.binding_id,
        "clientConfigFingerprint": command.parameters["client.config.fingerprint"],
        "clientPackageName": command.parameters["client.package.name"],
        "clientSoftwareKind": command.parameters["client.software.kind"],
        "clientSoftwareSemanticVersion": command.parameters[
          "client.software.semantic.version"
        ],
        "epoch": command.epoch,
        "preparationRequestHash": command.request_hash,
        "snapshotFingerprint": command.parameters["snapshot.fingerprint"],
        "snapshotVersion": command.snapshot_version,
        **security,
      }
    return {
      "schemaVersion": 2 if secure else 1,
      "bundleHash": bundle.immutable_bundle_hash,
      "hostId": bundle.host_id,
      "consumerUser": bundle.consumer_user,
      "identityFingerprint": bundle.identity_fingerprint,
      "commands": commands,
    }

  def _read_active_manifest(self):
    if not os.path.lexists(os.path.join(self.profile_root, "active")):
      return None
    return _read_json_file(
      os.path.join(self._active_profile_path(), "manifest.json"), 32 * 1024
    )

  def _validate_monotonic_selection(self, current, desired):
    if current is None:
      return
    if (
      not isinstance(current, dict)
      or current.get("schemaVersion") not in {1, 2}
      or current.get("schemaVersion") != desired["schemaVersion"]
      or not isinstance(current.get("commands"), dict)
      or current.get("hostId") != desired["hostId"]
      or current.get("consumerUser") != desired["consumerUser"]
      or current.get("identityFingerprint") != desired["identityFingerprint"]
    ):
      raise ManagedDependencyFailure(
        "DEPENDENCY_CLIENT_CONFIG_MISMATCH",
        "The active managed HBase client profile is invalid or belongs to another identity",
      )
    expected_command_fields = {
      "bindingId",
      "clientConfigFingerprint",
      "clientPackageName",
      "clientSoftwareKind",
      "clientSoftwareSemanticVersion",
      "epoch",
      "preparationRequestHash",
      "snapshotFingerprint",
      "snapshotVersion",
    }
    security_fields = (
      set(SECURITY_MANIFEST_FIELDS) if current["schemaVersion"] == 2 else set()
    )
    expected_command_fields |= security_fields
    for service, selected in current["commands"].items():
      candidate = desired["commands"].get(service)
      if (
        service not in {"HDFS", "ZOOKEEPER"}
        or not isinstance(selected, dict)
        or set(selected) != expected_command_fields
        or type(selected.get("epoch")) is not int
        or type(selected.get("snapshotVersion")) is not int
        or selected.get("epoch", 0) <= 0
        or selected.get("snapshotVersion", 0) <= 0
        or any(
          not isinstance(selected.get(field), str)
          or FINGERPRINT.fullmatch(selected[field]) is None
          for field in security_fields
        )
      ):
        raise ManagedDependencyFailure(
          "DEPENDENCY_CLIENT_CONFIG_MISMATCH",
          "The active managed HBase client manifest is structurally invalid",
        )
      if candidate is None or candidate["bindingId"] != selected["bindingId"]:
        raise ManagedDependencyFailure(
          "DEPENDENCY_OPERATION_STALE",
          "A managed dependency cannot disappear or change binding without explicit detach",
        )
      if candidate["epoch"] < selected["epoch"]:
        raise ManagedDependencyFailure(
          "DEPENDENCY_OPERATION_STALE",
          "The requested managed dependency epoch is older than the active profile",
        )
      if candidate["epoch"] == selected["epoch"]:
        if candidate["preparationRequestHash"] != selected["preparationRequestHash"]:
          raise ManagedDependencyFailure(
            "DEPENDENCY_OPERATION_STALE",
            "A managed dependency epoch may only replay its exact command",
          )
        continue
      if candidate["snapshotVersion"] < selected["snapshotVersion"]:
        raise ManagedDependencyFailure(
          "DEPENDENCY_OPERATION_STALE",
          "The requested managed dependency snapshot is older than the active profile",
        )
      if candidate["snapshotVersion"] == selected["snapshotVersion"]:
        immutable_config = (
          "clientConfigFingerprint",
          "clientPackageName",
          "clientSoftwareKind",
          "clientSoftwareSemanticVersion",
          "snapshotFingerprint",
        ) + tuple(sorted(security_fields))
        if any(candidate[field] != selected[field] for field in immutable_config):
          raise ManagedDependencyFailure(
            "DEPENDENCY_CLIENT_CONFIG_MISMATCH",
            "A snapshot version cannot change its approved client content",
          )

    if current["schemaVersion"] == 2 and len(
      {
        selected["consumerMappingProfileFingerprint"]
        for selected in current["commands"].values()
      }
    ) != 1:
      raise ManagedDependencyFailure(
        "DEPENDENCY_CLIENT_CONFIG_MISMATCH",
        "The active managed HBase client profile contains inconsistent mappings",
      )

  @contextmanager
  def _selection_lock(self):
    path = os.path.join(self.profile_root, ".selection.lock")
    try:
      descriptor = os.open(
        path,
        os.O_RDWR | os.O_CREAT | os.O_CLOEXEC | os.O_NOFOLLOW,
        0o600,
      )
    except OSError as error:
      raise ManagedDependencyFailure(
        "DEPENDENCY_CLIENT_CONFIG_MISMATCH",
        "The managed HBase client selection lock could not be opened safely",
      ) from error
    try:
      metadata = os.fstat(descriptor)
      if (
        not stat.S_ISREG(metadata.st_mode)
        or metadata.st_uid != self.expected_uid
        or stat.S_IMODE(metadata.st_mode) != 0o600
        or metadata.st_nlink != 1
      ):
        raise ManagedDependencyFailure(
          "DEPENDENCY_CLIENT_CONFIG_MISMATCH",
          "The managed HBase client selection lock has unsafe metadata",
        )
      fcntl.flock(descriptor, fcntl.LOCK_EX)
      yield
    finally:
      os.close(descriptor)

  def _ensure_profile(self, profile_path, expected_files):
    if os.path.lexists(profile_path):
      _require_directory(profile_path, self.expected_uid, self.group_id, None, 0o750)
      actual_names = set(os.listdir(profile_path))
      if actual_names != set(expected_files):
        self._profile_conflict()
      for name, content in expected_files.items():
        if _read_exact_file(
          os.path.join(profile_path, name), self.expected_uid, self.group_id
        ) != content:
          self._profile_conflict()
      return

    profiles_root = os.path.dirname(profile_path)
    temporary = tempfile.mkdtemp(prefix=".profile-", dir=profiles_root)
    os.chmod(temporary, 0o750)
    os.chown(temporary, self.expected_uid, self.group_id)
    try:
      for name, content in expected_files.items():
        _write_new_file(
          os.path.join(temporary, name), content, self.expected_uid, self.group_id
        )
      _fsync_directory(temporary)
      os.rename(temporary, profile_path)
      _fsync_directory(profiles_root)
    except FileExistsError:
      shutil.rmtree(temporary)
      self._ensure_profile(profile_path, expected_files)
    except BaseException:
      shutil.rmtree(temporary, ignore_errors=True)
      raise

  def _select_profile(self, profile_name, has_hdfs):
    relative = os.path.join("profiles", profile_name)
    temporary = os.path.join(self.profile_root, ".active-" + secrets.token_hex(12))
    os.symlink(relative, temporary)
    os.replace(temporary, os.path.join(self.profile_root, "active"))
    _fsync_directory(self.profile_root)
    if has_hdfs:
      for name in ("core-site.xml", "hdfs-site.xml"):
        destination = os.path.join(self.hbase_conf_write_dir, name)
        temporary = destination + ".managed-" + secrets.token_hex(12)
        os.symlink(os.path.join(self.profile_root, "active", name), temporary)
        os.replace(temporary, destination)
      _fsync_directory(self.hbase_conf_write_dir)

  def _active_profile_path(self):
    active = os.path.join(self.profile_root, "active")
    if not os.path.islink(active):
      raise ManagedDependencyFailure(
        "DEPENDENCY_CLIENT_CONFIG_MISMATCH",
        "The active managed HBase client profile link is missing or unsafe",
      )
    target = os.readlink(active)
    if os.path.dirname(target) != "profiles" or not _is_lower_hex(os.path.basename(target), 64):
      raise ManagedDependencyFailure(
        "DEPENDENCY_CLIENT_CONFIG_MISMATCH",
        "The active managed HBase client profile target is invalid",
      )
    profile = os.path.join(self.profile_root, target)
    _require_directory(profile, self.expected_uid, self.group_id, None, 0o750)
    return profile

  @staticmethod
  def _profile_conflict():
    raise ManagedDependencyFailure(
      "DEPENDENCY_CLIENT_CONFIG_MISMATCH",
      "An immutable managed HBase client profile does not match its content hash",
    )


class _ConsumerSecurity:
  def __init__(self, principal, jaas_file, environment):
    self.principal = principal
    self.jaas_file = jaas_file
    self.environment = dict(environment)


class ManagedHBaseConsumerVerifier:
  def __init__(
    self,
    params,
    profile_root=PROFILE_ROOT,
    call=shell.call,
    package_manager=None,
    role=None,
  ):
    self.params = params
    self.call = call
    self.store = ManagedHBaseClientConfigStore(
      profile_root, params.hbase_conf_dir, params.user_group
    )
    self.package_manager = package_manager or ManagerFactory.get()
    self.role = role

  def verify_hdfs(self, command):
    with self.store.protect_verification(command):
      self._verify_identity(command)
      with self._consumer_security(command) as security:
        package_version = self._verify_package(command)
        software_version = self._verify_client_software(command)
        config_fingerprint = self._verify_selected_config(command)
        parameters = command.parameters
        for uri in (
          parameters["expected.namespace.root.uri"],
          parameters["expected.namespace.wal.uri"],
        ):
          self._verify_hdfs_directory(command, uri, security.environment)
        self._probe_hdfs(command, security.environment)
      package_version = self._verify_package(command)
      software_version = self._verify_client_software(command)
      facts = {
        "applied.snapshot.fingerprint": parameters["snapshot.fingerprint"],
        "client.config.fingerprint": config_fingerprint,
        "client.package.name": parameters["client.package.name"],
        "client.package.version": package_version,
        "client.software.kind": parameters["client.software.kind"],
        "client.software.semantic.version": software_version,
        "datanode.read.write.verified": "true",
        "host.id": parameters["host.id"],
        "identity.fingerprint": parameters["identity.fingerprint"],
        "namenode.rpc.connected": "true",
      }
      if security.principal is not None:
        facts["consumer.kerberos.principal"] = security.principal
      return facts

  def verify_zookeeper(self, command):
    with self.store.protect_verification(command):
      self._verify_identity(command)
      with self._consumer_security(command) as security:
        package_version = self._verify_package(command)
        software_version = self._verify_client_software(command)
        config_fingerprint = self._verify_selected_config(command)
        config = json.loads(command.parameters["client.config.json"])[
          "zooKeeperClient"
        ]
        classpath = self._hbase_classpath(command, security.environment)
        connection = ",".join(
          f"{host}:{config['hbase.zookeeper.property.clientPort']}"
          for host in config["hbase.zookeeper.quorum"].split(",")
        )
        operation = "VERIFY_SECURE" if security.principal is not None else "VERIFY_INSECURE"
        arguments = [
          os.path.join(self.params.java64_home, "bin", "java"),
        ]
        if security.principal is not None:
          arguments.extend(
            (
              "-Dzookeeper.sasl.client=true",
              "-Dzookeeper.sasl.client.username="
              + config["zookeeper.sasl.client.username"],
              "-Dzookeeper.sasl.clientconfig=" + config["zookeeper.sasl.clientconfig"],
              "-Djava.security.auth.login.config=" + security.jaas_file,
            )
          )
        arguments.extend(
          (
            "-cp",
            classpath + os.pathsep + ZOOKEEPER_HELPER_JAR,
            ZOOKEEPER_HELPER_CLASS,
            "--operation",
            operation,
            "--connection-string",
            connection,
            "--parent",
            command.parameters["expected.namespace.parent.znode"],
            "--container",
            command.parameters["expected.namespace.container.znode"],
            "--znode",
            command.parameters["expected.namespace.znode"],
            "--binding-id",
            command.binding_id,
            "--provider-cluster-id",
            command.parameters["provider.cluster.id"],
            "--owner-user",
            command.parameters["consumer.user"],
            "--probe-id",
            command.operation_id,
        )
        )
        if security.principal is not None:
          arguments.extend(
            (
              "--consumer-sasl-id",
              command.parameters["expected.consumer.zk.sasl.id"],
            )
          )
        environment = {"JAVA_HOME": self.params.java64_home, **security.environment}
        try:
          code, output = self.call(
            tuple(arguments),
            user=command.parameters["consumer.user"],
            environment=environment,
            shell=False,
            quiet=True,
            timeout=COMMAND_TIMEOUT_SECONDS,
            timeout_kill_strategy=TerminateStrategy.KILL_PROCESS_GROUP,
          )
        except Exception as error:
          raise ManagedDependencyFailure(
            "DEPENDENCY_ZOOKEEPER_SESSION_FAILED",
            "The HBase ZooKeeper client helper could not complete within its bounded execution",
          ) from error
        if code != 0:
          raise ManagedDependencyFailure(
            "DEPENDENCY_ZOOKEEPER_SESSION_FAILED",
            "The HBase ZooKeeper client could not establish and verify a provider session",
          )
        expected = (
          {
            "connected": True,
            "consumerSaslId": command.parameters["expected.consumer.zk.sasl.id"],
            "hbaseZnodeVerified": True,
            "privateContainerVerified": True,
            "siblingAuthorityDenied": True,
          }
          if security.principal is not None
          else {"connected": True, "privateZnodeVerified": True}
        )
        _parse_zookeeper_helper_output(output, expected)
      package_version = self._verify_package(command)
      software_version = self._verify_client_software(command)
      facts = {
        "applied.snapshot.fingerprint": command.parameters["snapshot.fingerprint"],
        "client.config.fingerprint": config_fingerprint,
        "client.package.name": command.parameters["client.package.name"],
        "client.package.version": package_version,
        "client.software.kind": command.parameters["client.software.kind"],
        "client.software.semantic.version": software_version,
        "host.id": command.parameters["host.id"],
        "identity.fingerprint": command.parameters["identity.fingerprint"],
        "private.znode.verified": "true",
        "zookeeper.quorum.connected": "true",
      }
      if security.principal is not None:
        facts.update(
          {
            "consumer.kerberos.principal": security.principal,
            "consumer.zk.sasl.id": command.parameters["expected.consumer.zk.sasl.id"],
            "container.acl.verified": "true",
            "hbase.znode.verified": "true",
            "sibling.authority.denied": "true",
          }
        )
      return facts

  def observe_preparation_locked(self, command):
    if command.name not in {
      PREPARE_HDFS_CONSUMER,
      PREPARE_ZOOKEEPER_CONSUMER,
    }:
      raise ManagedDependencyFailure(
        "DEPENDENCY_COMMAND_INVALID",
        "Client preparation accepts only reserved PREPARE commands",
      )
    self._verify_identity(command)
    package_version = self._observe_package(command)
    software_version = self._verify_client_software(command)
    config_fingerprint = self._verify_selected_config(command)
    return {
      "applied.snapshot.fingerprint": command.parameters["snapshot.fingerprint"],
      "client.config.fingerprint": config_fingerprint,
      "client.package.name": command.parameters["client.package.name"],
      "client.package.version": package_version,
      "client.software.kind": command.parameters["client.software.kind"],
      "client.software.semantic.version": software_version,
      "consumer.user": command.parameters["consumer.user"],
      "host.id": command.parameters["host.id"],
      "identity.fingerprint": command.parameters["identity.fingerprint"],
    }

  def _verify_identity(self, command):
    expected = command.parameters["consumer.user"]
    if self.params.hbase_user != expected:
      raise ManagedDependencyFailure(
        "DEPENDENCY_CLIENT_IDENTITY_MISMATCH",
        "The configured HBase service user does not match the approved identity",
      )
    try:
      pwd.getpwnam(expected)
    except KeyError as error:
      raise ManagedDependencyFailure(
        "DEPENDENCY_CLIENT_IDENTITY_MISMATCH",
        "The approved HBase service user does not exist on this host",
      ) from error

  @contextmanager
  def _consumer_security(self, command):
    secure = command.parameters["security.mode"] == "KERBEROS"
    if bool(getattr(self.params, "security_enabled", False)) != secure:
      raise ManagedDependencyFailure(
        "DEPENDENCY_SECURITY_MISMATCH",
        "The HBase host security mode does not match the approved dependency snapshot",
      )
    if not secure:
      yield _ConsumerSecurity(None, None, {})
      return

    principal, keytab, jaas_file = self._role_kerberos_material(command)
    try:
      with PrivateKerberosCache(
        command.parameters["consumer.user"],
        self.params.user_group,
        temp_dir=self.params.tmp_dir,
        prefix="ambari-managed-hbase-",
      ) as kerberos_cache:
        kerberos_cache.kinit(
          self.params.kinit_path_local, keytab, principal, timeout=30
        )
        yield _ConsumerSecurity(
          principal, jaas_file, kerberos_cache.merge_environment()
        )
    except ManagedDependencyFailure:
      raise
    except Exception as error:
      raise ManagedDependencyFailure(
        "DEPENDENCY_CLIENT_IDENTITY_MISMATCH",
        "The approved HBase role credential could not establish a private Kerberos identity",
      ) from error

  def _role_kerberos_material(self, command):
    material = {
      "master": ("master_jaas_princ", "master_keytab_path"),
      "regionserver": ("regionserver_jaas_princ", "regionserver_keytab_path"),
      "thrift": ("thrift_jaas_princ", "thrift_keytab_path"),
    }.get(self.role)
    if material is None:
      raise ManagedDependencyFailure(
        "DEPENDENCY_CLIENT_IDENTITY_MISMATCH",
        "Secure managed dependency verification requires a daemon role identity",
      )
    principal = getattr(self.params, material[0], None)
    keytab = getattr(self.params, material[1], None)
    pattern = command.parameters["consumer.kerberos.principal.pattern"]
    expected_principal = pattern.replace("_HOST", self.params.hostname.lower())
    hbase_site = self.params.config["configurations"]["hbase-site"]
    superusers = {
      value.strip()
      for value in str(hbase_site.get("hbase.superuser", "")).split(",")
      if value.strip()
    }
    if (
      principal != expected_principal
      or principal.split("/", 1)[0] != command.parameters["consumer.user"]
      or hbase_site.get("hbase.security.authentication") != "kerberos"
      or superusers != {command.parameters["consumer.user"]}
    ):
      raise ManagedDependencyFailure(
        "DEPENDENCY_CLIENT_IDENTITY_MISMATCH",
        "The rendered HBase role principal or superuser policy differs from the approved identity",
      )
    jaas_file = os.path.join(self.params.hbase_conf_dir, f"hbase_{self.role}_jaas.conf")
    try:
      service = pwd.getpwnam(command.parameters["consumer.user"])
      service_group = grp.getgrnam(self.params.user_group)
      root = pwd.getpwnam("root")
    except (KeyError, TypeError) as error:
      raise ManagedDependencyFailure(
        "DEPENDENCY_CLIENT_IDENTITY_MISMATCH",
        "The approved HBase credential owners cannot be resolved",
      ) from error
    self._require_safe_credential(
      keytab, "keytab", service.pw_uid, service_group.gr_gid, 0o400
    )
    self._require_safe_credential(
      jaas_file, "JAAS file", root.pw_uid, service_group.gr_gid, 0o640
    )
    self._validate_role_jaas(jaas_file, keytab, principal)
    return principal, keytab, jaas_file

  @staticmethod
  def _require_safe_credential(path, label, expected_uid, expected_gid, expected_mode):
    if not isinstance(path, str) or not os.path.isabs(path):
      raise ManagedDependencyFailure(
        "DEPENDENCY_CLIENT_IDENTITY_MISMATCH",
        f"The approved HBase role {label} path is missing or not absolute",
      )
    try:
      metadata = os.stat(path, follow_symlinks=False)
    except (OSError, TypeError) as error:
      raise ManagedDependencyFailure(
        "DEPENDENCY_CLIENT_IDENTITY_MISMATCH",
        f"The approved HBase role {label} cannot be inspected",
      ) from error
    if (
      not _credential_metadata_matches(
        metadata, expected_uid, expected_gid, expected_mode
      )
    ):
      raise ManagedDependencyFailure(
        "DEPENDENCY_CLIENT_IDENTITY_MISMATCH",
        f"The approved HBase role {label} has unsafe ownership, type, or permissions",
      )

  @staticmethod
  def _validate_role_jaas(path, keytab, principal):
    try:
      with open(path, "r", encoding="utf-8") as stream:
        content = stream.read(32 * 1024 + 1)
    except OSError as error:
      raise ManagedDependencyFailure(
        "DEPENDENCY_CLIENT_IDENTITY_MISMATCH",
        "The rendered HBase role JAAS configuration cannot be read",
      ) from error
    client_blocks = re.findall(r"(?:^|\n)\s*Client\s*\{(.*?)\};", content, re.DOTALL)
    compact = re.sub(r"\s+", "", client_blocks[0]) if len(client_blocks) == 1 else ""
    if (
      len(content.encode("utf-8")) > 32 * 1024
      or len(client_blocks) != 1
      or "useKeyTab=true" not in compact
      or "useTicketCache=false" not in compact
      or "useTicketCache=true" in compact
      or f'keyTab="{keytab}"' not in compact
      or f'principal="{principal}"' not in compact
    ):
      raise ManagedDependencyFailure(
        "DEPENDENCY_CLIENT_IDENTITY_MISMATCH",
        "The rendered HBase role JAAS configuration does not bind the approved credential",
      )

  def _verify_package(self, command):
    observed = self._observe_package(command)
    if observed != command.parameters["client.package.version"]:
      raise ManagedDependencyFailure(
        "DEPENDENCY_CLIENT_PACKAGE_MISMATCH",
        "The installed managed dependency client package version does not match the observation",
      )
    return observed

  def _observe_package(self, command):
    name = command.parameters["client.package.name"]
    try:
      observed = self.package_manager.get_installed_package_version(name)
    except Exception as error:
      raise ManagedDependencyFailure(
        "DEPENDENCY_CLIENT_PACKAGE_MISMATCH",
        "The approved managed dependency client package could not be inspected",
      ) from error
    if (
      not isinstance(observed, str)
      or not observed
      or observed != observed.strip()
      or len(observed) > 512
      or any(ord(character) < 32 or ord(character) == 127 for character in observed)
    ):
      raise ManagedDependencyFailure(
        "DEPENDENCY_CLIENT_PACKAGE_MISMATCH",
        "The installed managed dependency client package did not provide a bounded version",
      )
    return observed

  def _verify_client_software(self, command):
    kind = command.parameters["client.software.kind"]
    if kind == "HADOOP_CLIENT":
      self.store._active_profile_path()
      arguments = (
        os.path.join(self.params.hadoop_bin_dir, "hdfs"),
        "--config",
        os.path.join(self.store.profile_root, "active"),
        "version",
      )
      label = "Hadoop"
    else:
      arguments = (
        self.params.hbase_cmd,
        "--config",
        self.params.hbase_conf_dir,
        "version",
      )
      label = "HBase"
    try:
      code, output = self.call(
        arguments,
        user=command.parameters["consumer.user"],
        environment={"JAVA_HOME": self.params.java64_home},
        shell=False,
        timeout=COMMAND_TIMEOUT_SECONDS,
        timeout_kill_strategy=TerminateStrategy.KILL_PROCESS_GROUP,
        quiet=True,
      )
    except Exception as error:
      raise ManagedDependencyFailure(
        "DEPENDENCY_CLIENT_PACKAGE_MISMATCH",
        "The installed managed dependency client software version could not be inspected",
      ) from error
    if not isinstance(output, str) or len(output.encode("utf-8")) > 64 * 1024:
      matches = []
    else:
      prefix = label + " "
      matches = [
        line[len(prefix) :]
        for line in output.splitlines()
        if line.startswith(prefix)
        and CLIENT_SOFTWARE_VERSION.fullmatch(line[len(prefix) :]) is not None
      ]
    if (
      code != 0
      or len(matches) != 1
      or matches[0] != command.parameters["client.software.semantic.version"]
    ):
      raise ManagedDependencyFailure(
        "DEPENDENCY_CLIENT_PACKAGE_MISMATCH",
        "The installed managed dependency client software version is incompatible",
      )
    return matches[0]

  def _verify_selected_config(self, command):
    actual = self.store.read_selected_config(command.provider_service)
    fingerprint = client_config_fingerprint(actual)
    if fingerprint != command.parameters["client.config.fingerprint"]:
      raise ManagedDependencyFailure(
        "DEPENDENCY_CLIENT_CONFIG_MISMATCH",
        "The selected managed dependency client configuration does not match the snapshot",
      )
    return fingerprint

  def _verify_hdfs_directory(self, command, uri, environment):
    code, output = self._hdfs(
      command,
      environment,
      "DEPENDENCY_NAMENODE_RPC_FAILED",
      "NameNode RPC could not inspect a managed HBase namespace",
      "dfs",
      "-stat",
      "%u:%g:%a",
      uri,
    )
    if code != 0:
      raise ManagedDependencyFailure(
        "DEPENDENCY_NAMENODE_RPC_FAILED",
        "NameNode RPC could not inspect a managed HBase namespace",
      )
    expected = ":".join(
      (
        command.parameters["consumer.user"],
        command.parameters["expected.owner.group"],
        command.parameters["expected.directory.mode"].lstrip("0"),
      )
    )
    if output.strip() != expected:
      raise ManagedDependencyFailure(
        "DEPENDENCY_AUTHORIZATION_FAILED",
        "Managed HBase namespace ownership or mode does not match the snapshot",
      )

  def _probe_hdfs(self, command, environment):
    root_uri = command.parameters["expected.namespace.root.uri"].rstrip("/")
    remote = root_uri + "/.ambari-managed-probe-" + command.operation_id
    expected = hashlib.sha256(command.request_hash.encode("utf-8")).digest()
    content = (expected * ((PROBE_BYTES // len(expected)) + 1))[:PROBE_BYTES]
    local_directory = tempfile.mkdtemp(prefix="ambari-hbase-probe-", dir=self.params.tmp_dir)
    account = pwd.getpwnam(command.parameters["consumer.user"])
    os.chown(local_directory, account.pw_uid, account.pw_gid)
    os.chmod(local_directory, 0o700)
    source = os.path.join(local_directory, "source")
    destination = os.path.join(local_directory, "destination")
    with open(source, "xb") as stream:
      stream.write(content)
      stream.flush()
      os.fsync(stream.fileno())
    os.chown(source, account.pw_uid, account.pw_gid)
    os.chmod(source, 0o600)
    failure = None
    try:
      exists, unused = self._hdfs(
        command,
        environment,
        "DEPENDENCY_NAMENODE_RPC_FAILED",
        "NameNode RPC could not inspect the bounded DataNode probe path",
        "dfs",
        "-test",
        "-e",
        remote,
      )
      if exists not in (0, 1):
        raise ManagedDependencyFailure(
          "DEPENDENCY_NAMENODE_RPC_FAILED",
          "NameNode RPC could not inspect the bounded DataNode probe path",
        )
      if exists == 1:
        code, unused = self._hdfs(
          command,
          environment,
          "DEPENDENCY_DATANODE_READ_WRITE_FAILED",
          "The nonempty managed HBase DataNode probe could not be written",
          "dfs",
          "-put",
          source,
          remote,
        )
        if code != 0:
          raise ManagedDependencyFailure(
            "DEPENDENCY_DATANODE_READ_WRITE_FAILED",
            "The nonempty managed HBase DataNode probe could not be written",
          )
      code, unused = self._hdfs(
        command,
        environment,
        "DEPENDENCY_DATANODE_READ_WRITE_FAILED",
        "The nonempty managed HBase DataNode probe could not be read",
        "dfs",
        "-get",
        remote,
        destination,
      )
      if code != 0:
        raise ManagedDependencyFailure(
          "DEPENDENCY_DATANODE_READ_WRITE_FAILED",
          "The nonempty managed HBase DataNode probe could not be read",
        )
      with open(destination, "rb") as stream:
        observed = stream.read(PROBE_BYTES + 1)
      if observed != content:
        raise ManagedDependencyFailure(
          "DEPENDENCY_DATA_INTEGRITY_FAILED",
          "The managed HBase DataNode probe content did not match",
        )
    except ManagedDependencyFailure as error:
      failure = error
    finally:
      code, unused = self._hdfs(
        command,
        environment,
        "DEPENDENCY_PROBE_CLEANUP_FAILED",
        "The bounded managed HBase DataNode probe could not be removed",
        "dfs",
        "-rm",
        "-f",
        remote,
      )
      shutil.rmtree(local_directory, ignore_errors=True)
      if code != 0:
        raise ManagedDependencyFailure(
          "DEPENDENCY_PROBE_CLEANUP_FAILED",
          "The bounded managed HBase DataNode probe could not be removed",
        )
    if failure is not None:
      raise failure

  def _hdfs(self, command, environment, error_code, error_message, *arguments):
    executable = os.path.join(self.params.hadoop_bin_dir, "hdfs")
    try:
      return self.call(
        (executable, "--config", os.path.join(self.store.profile_root, "active"))
        + arguments,
        user=command.parameters["consumer.user"],
        environment=environment,
        shell=False,
        timeout=COMMAND_TIMEOUT_SECONDS,
        timeout_kill_strategy=TerminateStrategy.KILL_PROCESS_GROUP,
        quiet=True,
      )
    except Exception as error:
      raise ManagedDependencyFailure(error_code, error_message) from error

  def _hbase_classpath(self, command, environment):
    try:
      code, output = self.call(
        (self.params.hbase_cmd, "--config", self.params.hbase_conf_dir, "classpath"),
        user=command.parameters["consumer.user"],
        environment=environment,
        shell=False,
        timeout=COMMAND_TIMEOUT_SECONDS,
        timeout_kill_strategy=TerminateStrategy.KILL_PROCESS_GROUP,
        quiet=True,
      )
    except Exception as error:
      raise ManagedDependencyFailure(
        "DEPENDENCY_CLIENT_PACKAGE_MISMATCH",
        "The installed HBase package could not provide its ZooKeeper client classpath",
      ) from error
    classpath = output.strip() if isinstance(output, str) else ""
    if (
      code != 0
      or not classpath
      or len(classpath.encode("utf-8")) > 64 * 1024
      or "\n" in classpath
      or "\r" in classpath
      or any(ord(character) < 32 for character in classpath)
      or any(
        not os.path.isabs(entry.rstrip("*"))
        for entry in classpath.split(os.pathsep)
      )
    ):
      raise ManagedDependencyFailure(
        "DEPENDENCY_CLIENT_PACKAGE_MISMATCH",
        "The installed HBase package did not provide a safe ZooKeeper client classpath",
      )
    return classpath


def _load_command(expected_name):
  raw = Script.get_config().get("commandParams", {}).get(COMMAND_PARAMETER)
  payload = _load_json(raw, MAX_COMMAND_BYTES, "Managed dependency command")
  return parse_managed_dependency_command(payload, expected_name)


def _load_bundle(required):
  raw = Script.get_config().get("commandParams", {}).get(BUNDLE_PARAMETER)
  if raw is None or raw == "":
    if required:
      raise ManagedDependencyFailure(
        "DEPENDENCY_SNAPSHOT_NOT_APPROVED",
        "An active managed HBase deployment requires an approved dependency command bundle",
      )
    return None
  return parse_managed_dependency_bundle(
    _load_json(raw, MAX_BUNDLE_BYTES, "Managed dependency command bundle")
  )


def _load_json(raw, maximum_bytes, label):
  if not isinstance(raw, str) or not raw or len(raw.encode("utf-8")) > maximum_bytes:
    raise ManagedDependencyFailure(
      "DEPENDENCY_COMMAND_INVALID", label + " is missing or too large"
    )
  try:
    return json.loads(raw)
  except (TypeError, ValueError, json.JSONDecodeError) as error:
    raise ManagedDependencyFailure(
      "DEPENDENCY_COMMAND_INVALID", label + " is not valid JSON"
    ) from error


def _result(command, status, facts, error_code=None, error_message=None):
  return {
    "commandName": command.name,
    "envelope": command.envelope,
    "status": status,
    "facts": dict(facts),
    "errorCode": error_code,
    "errorMessage": error_message,
  }


def _parse_zookeeper_helper_output(output, expected):
  if not isinstance(output, str) or len(output.encode("utf-8")) > 64 * 1024:
    raise ManagedDependencyFailure(
      "DEPENDENCY_ZOOKEEPER_SESSION_FAILED",
      "The HBase ZooKeeper client helper output is missing or too large",
    )
  matches = []
  for line in output.splitlines():
    try:
      value = json.loads(line)
    except (TypeError, ValueError, json.JSONDecodeError):
      continue
    if value == expected:
      matches.append(value)
  if len(matches) != 1:
    raise ManagedDependencyFailure(
      "DEPENDENCY_ZOOKEEPER_SESSION_FAILED",
      "The HBase ZooKeeper client helper did not return exact verification evidence",
    )
  return matches[0]


def _xml_config(properties):
  root = ElementTree.Element("configuration")
  for name, value in sorted(properties.items()):
    property_element = ElementTree.SubElement(root, "property")
    ElementTree.SubElement(property_element, "name").text = name
    ElementTree.SubElement(property_element, "value").text = value
  ElementTree.indent(root, space="  ")
  return ElementTree.tostring(root, encoding="utf-8", xml_declaration=True) + b"\n"


def _read_xml_config(path):
  content = _read_exact_file(path, os.geteuid(), None)
  try:
    root = ElementTree.fromstring(content)
  except ElementTree.ParseError as error:
    raise ManagedDependencyFailure(
      "DEPENDENCY_CLIENT_CONFIG_MISMATCH",
      "A selected managed dependency client XML file is invalid",
    ) from error
  if root.tag != "configuration":
    raise ManagedDependencyFailure(
      "DEPENDENCY_CLIENT_CONFIG_MISMATCH",
      "A selected managed dependency client XML root is invalid",
    )
  result = {}
  for item in root:
    if item.tag != "property" or len(item) != 2:
      raise ManagedDependencyFailure(
        "DEPENDENCY_CLIENT_CONFIG_MISMATCH",
        "A selected managed dependency client property is invalid",
      )
    name = item.findtext("name")
    value = item.findtext("value")
    if name is None or value is None or name in result:
      raise ManagedDependencyFailure(
        "DEPENDENCY_CLIENT_CONFIG_MISMATCH",
        "A selected managed dependency client property is missing or duplicated",
      )
    result[name] = value
  return result


def _resolve_package_config_directory(path, uid, allowed_roots):
  try:
    metadata = os.lstat(path)
  except OSError as error:
    raise ManagedDependencyFailure(
      "DEPENDENCY_CLIENT_CONFIG_MISMATCH",
      "The package-managed HBase configuration directory is missing",
    ) from error
  if stat.S_ISDIR(metadata.st_mode):
    _require_directory(path, uid, None, 0o022)
    return path
  if not stat.S_ISLNK(metadata.st_mode) or metadata.st_uid != uid:
    raise ManagedDependencyFailure(
      "DEPENDENCY_CLIENT_CONFIG_MISMATCH",
      "The package-managed HBase configuration path has an unsafe type or owner",
    )
  try:
    target = os.readlink(path)
    if not target or any(character.isspace() for character in target):
      raise ValueError("unsafe package configuration link")
    resolved = os.path.realpath(path)
    matching_root = next(
      root
      for root in (os.path.realpath(item) for item in allowed_roots)
      if os.path.commonpath((resolved, root)) == root
    )
  except (OSError, ValueError, StopIteration) as error:
    raise ManagedDependencyFailure(
      "DEPENDENCY_CLIENT_CONFIG_MISMATCH",
      "The HBase configuration link does not target an approved package tree",
    ) from error
  current = matching_root
  relative = os.path.relpath(resolved, matching_root)
  for segment in (() if relative == "." else relative.split(os.sep)):
    _require_directory(current, uid, None, 0o022)
    current = os.path.join(current, segment)
  _require_directory(resolved, uid, None, 0o022)
  return resolved


def _ensure_exact_directory(path, uid, gid, mode):
  created = False
  try:
    os.mkdir(path, mode)
    created = True
    os.chown(path, uid, gid)
  except FileExistsError:
    pass
  if created:
    os.chmod(path, mode)
  _require_directory(path, uid, gid, None, mode)


def _require_directory(path, uid, gid=None, forbidden_mode=None, exact_mode=None):
  try:
    metadata = os.lstat(path)
  except OSError as error:
    raise ManagedDependencyFailure(
      "DEPENDENCY_CLIENT_CONFIG_MISMATCH", "A managed client directory is missing"
    ) from error
  mode = stat.S_IMODE(metadata.st_mode)
  if (
    not stat.S_ISDIR(metadata.st_mode)
    or metadata.st_uid != uid
    or gid is not None and metadata.st_gid != gid
    or forbidden_mode is not None and mode & forbidden_mode
    or exact_mode is not None and mode != exact_mode
  ):
    raise ManagedDependencyFailure(
      "DEPENDENCY_CLIENT_CONFIG_MISMATCH",
      "A managed client directory has unsafe ownership or permissions",
    )


def _write_new_file(path, content, uid, gid):
  descriptor = os.open(
    path,
    os.O_WRONLY | os.O_CREAT | os.O_EXCL | os.O_CLOEXEC | os.O_NOFOLLOW,
    0o640,
  )
  try:
    os.fchown(descriptor, uid, gid)
    view = memoryview(content)
    while view:
      written = os.write(descriptor, view)
      if written <= 0:
        raise OSError("managed client config write made no progress")
      view = view[written:]
    os.fsync(descriptor)
  finally:
    os.close(descriptor)


def _read_exact_file(path, uid, gid):
  metadata = os.lstat(path)
  if (
    not stat.S_ISREG(metadata.st_mode)
    or metadata.st_uid != uid
    or gid is not None and metadata.st_gid != gid
    or stat.S_IMODE(metadata.st_mode) != 0o640
    or metadata.st_nlink != 1
    or metadata.st_size > 64 * 1024
  ):
    raise ManagedDependencyFailure(
      "DEPENDENCY_CLIENT_CONFIG_MISMATCH",
      "A managed client configuration file has unsafe metadata",
    )
  with open(path, "rb") as stream:
    return stream.read(64 * 1024 + 1)


def _read_json_file(path, maximum_bytes):
  content = _read_exact_file(path, os.geteuid(), None)
  if len(content) > maximum_bytes:
    raise ManagedDependencyFailure(
      "DEPENDENCY_CLIENT_CONFIG_MISMATCH", "A managed client manifest is too large"
    )
  try:
    value = json.loads(content)
  except (UnicodeDecodeError, ValueError, json.JSONDecodeError) as error:
    raise ManagedDependencyFailure(
      "DEPENDENCY_CLIENT_CONFIG_MISMATCH", "A managed client manifest is invalid"
    ) from error
  if not isinstance(value, dict):
    raise ManagedDependencyFailure(
      "DEPENDENCY_CLIENT_CONFIG_MISMATCH", "A managed client manifest is invalid"
    )
  return value


def _fsync_directory(path):
  descriptor = os.open(path, os.O_RDONLY | os.O_DIRECTORY | os.O_CLOEXEC | os.O_NOFOLLOW)
  try:
    os.fsync(descriptor)
  finally:
    os.close(descriptor)


def _is_lower_hex(value, length):
  return len(value) == length and all(character in "0123456789abcdef" for character in value)
