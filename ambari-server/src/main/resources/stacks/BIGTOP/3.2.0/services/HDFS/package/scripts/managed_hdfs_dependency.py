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

from resource_management.libraries.functions.managed_dependency_client import observe_client
from urllib.parse import urlsplit, urlunsplit

from resource_management.core import shell
from resource_management.libraries.functions.managed_dependency import (
  INITIALIZE_BINDING_JOURNAL,
  INVALIDATE_BINDING_EPOCH,
  PREPARE_BINDING_JOURNAL,
  PROVISION_HDFS_NAMESPACE,
  ManagedDependencyFailure,
  ManagedDependencyJournal,
  parse_managed_dependency_command,
)
from resource_management.libraries.script.script import Script

from hdfs_kerberos import hdfs_kerberos_environment


JOURNAL_ROOT = "/var/lib/ambari-agent/data/managed-dependencies"
COMMAND_PARAMETER = "managed_dependency_command"
RESULT_KEY = "managedDependencyResult"
MAX_COMMAND_BYTES = 64 * 1024
HDFS_MANAGED_ROOT = "/apps/ambari-managed"
HDFS_HBASE_ROOT = HDFS_MANAGED_ROOT + "/hbase"
HDFS_MARKER_ROOT = HDFS_MANAGED_ROOT + "/.bindings"


def execute_managed_hdfs_command(script, env, expected_name):
  import params

  env.set_params(params)
  command = _load_command(expected_name)
  journal = ManagedDependencyJournal(JOURNAL_ROOT)
  try:
    _require_hdfs_provider(command)
    if expected_name == PREPARE_BINDING_JOURNAL:
      facts = journal.prepare(command)
    elif expected_name == INITIALIZE_BINDING_JOURNAL:
      facts = journal.initialize(command)
    elif expected_name == PROVISION_HDFS_NAMESPACE:
      provisioner = ManagedHdfsNamespaceProvisioner(params)
      facts = journal.execute(
        command, lambda progress: provisioner.provision(command, progress)
      )
    elif expected_name == INVALIDATE_BINDING_EPOCH:
      facts = journal.execute(
        command,
        lambda _progress: {
          "highest.accepted.epoch": str(command.epoch),
          "provider.action.host.id": command.action_host_id,
        },
      )
    else:
      raise ManagedDependencyFailure(
        "DEPENDENCY_COMMAND_INVALID", "Unsupported HDFS dependency action"
      )
    script.put_structured_out({RESULT_KEY: _result(command, "SUCCEEDED", facts)})
  except ManagedDependencyFailure as error:
    status = (
      "STALE_REJECTED"
      if error.code == "DEPENDENCY_OPERATION_STALE"
      else "FAILED"
    )
    script.put_structured_out(
      {
        RESULT_KEY: _result(
          command,
          status,
          error.facts,
          error.code,
          error.sanitized_message,
        )
      }
    )
    raise


def _load_command(expected_name):
  raw_value = Script.get_config().get("commandParams", {}).get(COMMAND_PARAMETER)
  if (
    not isinstance(raw_value, str)
    or not raw_value
    or len(raw_value.encode("utf-8")) > MAX_COMMAND_BYTES
  ):
    raise ManagedDependencyFailure(
      "DEPENDENCY_COMMAND_INVALID", "Managed dependency command is missing or too large"
    )
  try:
    payload = json.loads(raw_value)
  except (TypeError, ValueError, json.JSONDecodeError) as error:
    raise ManagedDependencyFailure(
      "DEPENDENCY_COMMAND_INVALID", "Managed dependency command is not valid JSON"
    ) from error
  return parse_managed_dependency_command(payload, expected_name)


def _result(command, status, facts, error_code=None, error_message=None):
  return {
    "commandName": command.name,
    "envelope": command.envelope,
    "status": status,
    "facts": dict(facts),
    "errorCode": error_code,
    "errorMessage": error_message,
  }


def _require_hdfs_provider(command):
  if command.provider_service != "HDFS":
    raise ManagedDependencyFailure(
      "DEPENDENCY_COMMAND_INVALID",
      "The HDFS dependency runner requires provider.service=HDFS",
    )


class ManagedHdfsNamespaceProvisioner:
  def __init__(self, params, call=shell.call):
    self.params = params
    self.call = call
    self.environment = None

  def provision(self, command, progress):
    self.command = command
    parameters = command.parameters
    root_uri = parameters["namespace.root.uri"]
    wal_uri = parameters["namespace.wal.uri"]
    binding_uri = self._uri(root_uri, HDFS_HBASE_ROOT + "/" + command.binding_id)
    managed_uri = self._uri(root_uri, HDFS_MANAGED_ROOT)
    hbase_uri = self._uri(root_uri, HDFS_HBASE_ROOT)
    marker_root_uri = self._uri(root_uri, HDFS_MARKER_ROOT)
    marker_uri = self._uri(
      root_uri, HDFS_MARKER_ROOT + "/" + command.binding_id + ".json"
    )
    marker = {
      "bindingId": command.binding_id,
      "directoryMode": parameters["directory.mode"],
      "namespaceRootUri": root_uri,
      "namespaceWalUri": wal_uri,
      "ownerGroup": parameters["owner.group"],
      "ownerUser": parameters["owner.user"],
      "providerClusterId": parameters["provider.cluster.id"],
      "providerService": parameters["provider.service"],
    }

    permissions_enabled = str(
      self.params.hdfs_site.get("dfs.permissions.enabled", "true")
    ).strip().lower()
    if permissions_enabled != "true":
      raise ManagedDependencyFailure(
        "DEPENDENCY_CLIENT_FEATURE_UNSUPPORTED",
        "Managed HDFS namespaces require dfs.permissions.enabled=true",
      )

    with hdfs_kerberos_environment(
      self.params,
      "ambari-managed-hdfs-",
      keytab=(
        self.params.hdfs_user_keytab if self.params.security_enabled else None
      ),
      principal=(
        self.params.hdfs_principal_name if self.params.security_enabled else None
      ),
    ) as self.environment:
      self._validate_provider_ancestor(self._uri(root_uri, "/"))
      apps_uri = self._uri(root_uri, "/apps")
      if self._stat(apps_uri) is None:
        self._mkdir(apps_uri, "0755")
      self._validate_provider_ancestor(apps_uri)
      self._ensure_provider_directory(managed_uri, "0711")
      self._ensure_provider_directory(hbase_uri, "0711")
      self._ensure_provider_directory(marker_root_uri, "0700")
      marker_exists = self._read_marker(marker_uri)
      if marker_exists is None:
        unexpected = [
          path
          for path in (binding_uri, root_uri, wal_uri)
          if self._stat(path) is not None
        ]
        if unexpected:
          raise ManagedDependencyFailure(
            "DEPENDENCY_NAMESPACE_CONFLICT",
            "Unmarked HDFS namespace paths already exist",
          )
        self._write_marker(marker_uri, marker)
        progress.checkpoint("provider-marker-created")
      elif marker_exists != marker:
        raise ManagedDependencyFailure(
          "DEPENDENCY_NAMESPACE_CONFLICT",
          "HDFS binding marker does not match the immutable request",
        )
      else:
        progress.checkpoint("provider-marker-verified")

      owner = parameters["owner.user"]
      group = parameters["owner.group"]
      mode = parameters["directory.mode"]
      for step, path in (
        ("binding-directory-ready", binding_uri),
        ("root-directory-ready", root_uri),
        ("wal-directory-ready", wal_uri),
      ):
        self._ensure_binding_directory(path, owner, group, mode)
        progress.checkpoint(step)

    return {
      "applied.snapshot.fingerprint": parameters["snapshot.fingerprint"],
      "directory.mode": mode,
      "namespace.root.exists": "true",
      "namespace.wal.exists": "true",
      "owner.group": group,
      "owner.user": owner,
      "provider.action.host.id": command.action_host_id,
    }

  @staticmethod
  def _uri(namespace_uri, path):
    parsed = urlsplit(namespace_uri)
    return urlunsplit((parsed.scheme, parsed.netloc, path, "", ""))

  def _ensure_provider_directory(self, path, mode):
    observed = self._stat(path)
    expected = ("directory", self.params.hdfs_user, mode)
    if observed is None:
      self._mkdir(path, mode)
      observed = self._stat(path)
    if observed is None or (observed[0], observed[1], observed[3]) != expected:
      raise ManagedDependencyFailure(
        "DEPENDENCY_NAMESPACE_CONFLICT",
        "Managed HDFS provider directory has unexpected ownership or mode",
      )
    self._validate_acl(path, mode)

  def _validate_provider_ancestor(self, path):
    observed = self._stat(path)
    if (
      observed is None
      or observed[0] != "directory"
      or observed[1] != self.params.hdfs_user
      or len(observed[3]) != 4
      or not observed[3].startswith("0")
    ):
      raise ManagedDependencyFailure(
        "DEPENDENCY_NAMESPACE_CONFLICT",
        "HDFS managed namespace ancestor is missing or not provider-admin owned",
      )
    try:
      mode = int(observed[3], 8)
    except ValueError as error:
      raise ManagedDependencyFailure(
        "DEPENDENCY_PROVIDER_ACTION_FAILED",
        "HDFS returned an invalid ancestor mode",
      ) from error
    if mode & 0o022:
      raise ManagedDependencyFailure(
        "DEPENDENCY_NAMESPACE_CONFLICT",
        "HDFS managed namespace ancestor grants group or other write authority",
      )
    self._validate_acl(path, observed[3])

  def _ensure_binding_directory(self, path, owner, group, mode):
    observed = self._stat(path)
    if observed is None:
      self._mkdir(path, "0700")
      observed = self._stat(path)
    expected = ("directory", owner, group, mode)
    if observed == expected:
      self._validate_acl(path, mode)
      return
    provider_owned_transition = (
      observed is not None
      and observed[0] == "directory"
      and observed[1] == self.params.hdfs_user
      and observed[3] == "0700"
    )
    consumer_owned_transition = observed == ("directory", owner, group, "0700")
    if not provider_owned_transition and not (
      mode == "0750" and consumer_owned_transition
    ):
      raise ManagedDependencyFailure(
        "DEPENDENCY_NAMESPACE_CONFLICT",
        "Managed HDFS binding directory has unexpected ownership or mode",
      )
    self._validate_acl(path, "0700")
    if not self._directory_is_empty(path):
      raise ManagedDependencyFailure(
        "DEPENDENCY_NAMESPACE_CONFLICT",
        "A partial managed HDFS binding directory contains unexpected data",
      )
    if provider_owned_transition:
      self._observe("SET_OWNER", path, owner=owner, group=group)
    if mode == "0750":
      self._observe("SET_PERMISSION", path, mode=0o750)
    observed = self._stat(path)
    if observed != expected:
      raise ManagedDependencyFailure(
        "DEPENDENCY_NAMESPACE_CONFLICT",
        "Managed HDFS binding directory did not reach the approved ownership",
      )
    self._validate_acl(path, mode)

  def _mkdir(self, path, mode):
    self._reject_default_acl(self._parent_uri(path))
    self._observe("MKDIR", path, mode=int(mode, 8))

  def _directory_is_empty(self, path):
    return self._observe("COUNT", path)["empty"]

  def _read_marker(self, path):
    observed = self._stat(path)
    if observed is None:
      return None
    if (
      observed[0] != "regular file"
      or observed[1] != self.params.hdfs_user
      or observed[3] != "0600"
    ):
      raise ManagedDependencyFailure(
        "DEPENDENCY_NAMESPACE_CONFLICT", "HDFS binding marker is not provider-private"
      )
    self._validate_acl(path, "0600")
    return self._observe("READ_MARKER", path)["marker"]

  def _write_marker(self, path, marker):
    self._observe("WRITE_MARKER", path, marker=marker)
    if self._read_marker(path) != marker:
      raise ManagedDependencyFailure(
        "DEPENDENCY_NAMESPACE_CONFLICT", "HDFS binding marker was not stored exactly"
      )

  def _stat(self, path):
    observed = self._observe("STAT", path)
    if not observed["exists"]:
      return None
    return ({"DIRECTORY": "directory", "FILE": "regular file", "SYMLINK": "symlink"}[observed["type"]],
            observed["owner"], observed["group"], format(observed["mode"], "04o"))

  @staticmethod
  def _parent_uri(path):
    parsed = urlsplit(path)
    parent_path = parsed.path.rsplit("/", 1)[0] or "/"
    return urlunsplit((parsed.scheme, parsed.netloc, parent_path, "", ""))

  def _validate_acl(self, path, mode):
    # Hadoop reports only extended entries here; base permissions come from FileStatus.
    if self._observe("ACL", path)["entries"]:
      raise ManagedDependencyFailure(
        "DEPENDENCY_NAMESPACE_CONFLICT",
        "Managed HDFS path has extended or default ACL entries",
      )

  def _reject_default_acl(self, path):
    if any(entry["scope"] == "DEFAULT" for entry in self._observe("ACL", path)["entries"]):
      raise ManagedDependencyFailure(
        "DEPENDENCY_NAMESPACE_CONFLICT",
        "HDFS parent has a default ACL that can override private child permissions",
      )

  def _observe(self, operation, path, **payload):
    result = observe_client(self.call, os.path.join(self.params.hadoop_bin_dir, "hdfs"),
      self.params.hadoop_conf_dir, self.command, operation, {"path": path, **payload},
      self.params.hdfs_user, self.environment, timeout=120,
      error_code="DEPENDENCY_PROVIDER_ACTION_FAILED")
    confirmation = {"MKDIR": "created", "SET_OWNER": "applied", "SET_PERMISSION": "applied",
                    "WRITE_MARKER": "written"}.get(operation)
    if confirmation and result[confirmation] is not True:
      raise ManagedDependencyFailure("DEPENDENCY_PROVIDER_ACTION_FAILED",
                                     "The client did not confirm the provider mutation")
    return result
