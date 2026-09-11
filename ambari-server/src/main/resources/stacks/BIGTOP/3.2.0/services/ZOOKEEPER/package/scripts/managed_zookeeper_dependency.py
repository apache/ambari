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
import grp
import pwd
import re
import stat

from resource_management.core import shell
from resource_management.core.signal_utils import TerminateStrategy
from resource_management.libraries.functions.managed_dependency import (
  INITIALIZE_BINDING_JOURNAL,
  INVALIDATE_BINDING_EPOCH,
  PREPARE_BINDING_JOURNAL,
  PROVISION_ZOOKEEPER_NAMESPACE,
  ManagedDependencyFailure,
  ManagedDependencyJournal,
  parse_managed_dependency_command,
)
from resource_management.libraries.functions.private_kerberos_cache import (
  PrivateKerberosCache,
)
from resource_management.libraries.script.script import Script


JOURNAL_ROOT = "/var/lib/ambari-agent/data/managed-dependencies"
COMMAND_PARAMETER = "managed_dependency_command"
RESULT_KEY = "managedDependencyResult"
MAX_COMMAND_BYTES = 64 * 1024
HELPER_JAR = "/var/lib/ambari-agent/tools/zkmigrator.jar"
HELPER_CLASS = "org.apache.ambari.tools.zk.ManagedDependencyZk"


def _credential_metadata_matches(metadata, expected_uid, expected_gid, expected_mode):
  return (
    stat.S_ISREG(metadata.st_mode)
    and metadata.st_uid == expected_uid
    and metadata.st_gid == expected_gid
    and stat.S_IMODE(metadata.st_mode) == expected_mode
  )


def execute_managed_zookeeper_command(script, env, expected_name):
  import params

  env.set_params(params)
  command = _load_command(expected_name)
  journal = ManagedDependencyJournal(JOURNAL_ROOT)
  try:
    _require_zookeeper_provider(command)
    if expected_name == PREPARE_BINDING_JOURNAL:
      facts = journal.prepare(command)
    elif expected_name == INITIALIZE_BINDING_JOURNAL:
      facts = journal.initialize(command)
    elif expected_name == PROVISION_ZOOKEEPER_NAMESPACE:
      provisioner = ManagedZooKeeperNamespaceProvisioner(params)
      facts = journal.execute(
        command, lambda progress: provisioner.provision(command, progress)
      )
    elif expected_name == INVALIDATE_BINDING_EPOCH:
      facts = journal.execute(
        command,
        lambda unused: {
          "highest.accepted.epoch": str(command.epoch),
          "provider.action.host.id": command.action_host_id,
        },
      )
    else:
      raise ManagedDependencyFailure(
        "DEPENDENCY_COMMAND_INVALID", "Unsupported ZooKeeper dependency action"
      )
    script.put_structured_out({RESULT_KEY: _result(command, "SUCCEEDED", facts)})
  except ManagedDependencyFailure as error:
    status = (
      "STALE_REJECTED"
      if error.code == "DEPENDENCY_OPERATION_STALE"
      else "RECONCILIATION_REQUIRED"
      if error.code == "DEPENDENCY_ZOOKEEPER_HANDOFF_RECONCILIATION_REQUIRED"
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


class ManagedZooKeeperNamespaceProvisioner:
  def __init__(self, params, call=shell.call):
    self.params = params
    self.call = call

  def provision(self, command, progress):
    expected_hosts = command.parameters["expected.quorum"].split(",")
    actual_hosts = sorted(set(self.params.zookeeper_hosts))
    if expected_hosts != actual_hosts or command.parameters["expected.client.port"] != str(
      self.params.client_port
    ):
      raise ManagedDependencyFailure(
        "DEPENDENCY_ZOOKEEPER_INVALID",
        "The provider ZooKeeper quorum does not match the approved snapshot",
      )
    connection = ",".join(
      f"{host}:{self.params.client_port}" for host in actual_hosts
    )
    if command.parameters["security.mode"] == "KERBEROS":
      return self._provision_secure(command, progress, connection)
    return self._provision_insecure(command, progress, connection)

  def _provision_insecure(self, command, progress, connection):
    try:
      code, output = self.call(
        (
          self.params.java_executable,
          "-cp",
          HELPER_JAR,
          HELPER_CLASS,
          "--operation",
          "PREPARE_INSECURE",
          "--connection-string",
          connection,
          "--parent",
          command.parameters["namespace.parent.znode"],
          "--container",
          command.parameters["namespace.container.znode"],
          "--znode",
          command.parameters["namespace.znode"],
          "--binding-id",
          command.binding_id,
          "--provider-cluster-id",
          command.parameters["provider.cluster.id"],
          "--owner-user",
          command.parameters["owner.user"],
        ),
        user=self.params.zk_user,
        env={"JAVA_HOME": self.params.java64_home},
        shell=False,
        quiet=True,
        timeout=60,
        timeout_kill_strategy=TerminateStrategy.KILL_PROCESS_GROUP,
      )
    except Exception as error:
      raise ManagedDependencyFailure(
        "DEPENDENCY_PROVIDER_ACTION_FAILED",
        "The bounded ZooKeeper provider helper could not complete",
      ) from error
    if code != 0:
      raise ManagedDependencyFailure(
        "DEPENDENCY_PROVIDER_ACTION_FAILED",
        "The provider ZooKeeper namespace preparation failed",
      )
    helper_facts = _parse_helper_result(
      output,
      {
        "connected": True,
        "namespaceExists": True,
        "parentAclPolicy": "INSECURE_PROVIDER_PREPARED",
        "subtreeAclPolicy": "INSECURE_BINDING_SCOPED",
      },
    )
    progress.checkpoint("zookeeper-namespace-prepared")
    return {
      "applied.snapshot.fingerprint": command.parameters["snapshot.fingerprint"],
      "namespace.znode.exists": str(helper_facts["namespaceExists"]).lower(),
      "owner.user": command.parameters["owner.user"],
      "parent.acl.policy": helper_facts["parentAclPolicy"],
      "provider.action.host.id": command.action_host_id,
      "subtree.acl.policy": helper_facts["subtreeAclPolicy"],
    }

  def _provision_secure(self, command, progress, connection):
    self._validate_secure_provider(command)
    facts = self._secure_handoff_facts(command)
    if "secure-container-create-acknowledged" in progress.completed_steps:
      facts["provider.handoff.acknowledged"] = "true"
      return facts
    try:
      with PrivateKerberosCache(
        self.params.zk_user,
        self.params.user_group,
        temp_dir=self.params.tmp_dir,
        prefix="ambari-managed-zookeeper-",
      ) as kerberos_cache:
        kerberos_cache.kinit(
          self.params.kinit_path_local,
          self.params.zk_keytab_path,
          self.params.zk_principal,
          timeout=30,
        )
        environment = kerberos_cache.merge_environment(
          {"JAVA_HOME": self.params.java64_home}
        )
        common = self._secure_helper_arguments(command, connection)
        if "secure-ledger-prepared" not in progress.completed_steps:
          self._call_exact_helper(
            common + ("--operation", "PREPARE_SECURE_LEDGER"),
            environment,
            {"connected": True, "ledgerPrepared": True},
          )
          progress.checkpoint("secure-ledger-prepared")
        if "secure-container-create-dispatched" not in progress.completed_steps:
          progress.checkpoint("secure-container-create-dispatched")
        result = self._call_helper(
          common + ("--operation", "CREATE_SECURE_CONTAINER"), environment
        )
        created = {"connected": True, "containerCreated": True, "nodeExists": False}
        exists = {"connected": True, "containerCreated": False, "nodeExists": True}
        if result == created:
          progress.checkpoint("secure-container-create-acknowledged")
          facts["provider.handoff.acknowledged"] = "true"
          return facts
        if result == exists:
          facts["provider.handoff.reconciliation.required"] = "true"
          raise ManagedDependencyFailure(
            "DEPENDENCY_ZOOKEEPER_HANDOFF_RECONCILIATION_REQUIRED",
            "The consumer must reconcile an unacknowledged ZooKeeper container handoff",
            facts,
          )
        raise ManagedDependencyFailure(
          "DEPENDENCY_PROVIDER_ACTION_FAILED",
          "The ZooKeeper helper returned an invalid secure handoff result",
        )
    except ManagedDependencyFailure:
      raise
    except Exception as error:
      raise ManagedDependencyFailure(
        "DEPENDENCY_PROVIDER_ACTION_FAILED",
        "The secure ZooKeeper provider handoff could not complete",
      ) from error

  def _secure_helper_arguments(self, command, connection):
    return (
      self.params.java_executable,
      "-Dzookeeper.sasl.client=true",
      "-Dzookeeper.sasl.clientconfig=Client",
      "-Dzookeeper.sasl.client.username=" + self.params.zk_principal_user,
      "-Djava.security.auth.login.config=" + self.params.zk_client_jaas_file,
      "-cp",
      HELPER_JAR,
      HELPER_CLASS,
      "--connection-string",
      connection,
      "--parent",
      command.parameters["namespace.parent.znode"],
      "--container",
      command.parameters["namespace.container.znode"],
      "--znode",
      command.parameters["namespace.znode"],
      "--ledger",
      command.parameters["namespace.ledger.znode"],
      "--binding-id",
      command.binding_id,
      "--provider-cluster-id",
      command.parameters["provider.cluster.id"],
      "--owner-user",
      command.parameters["owner.user"],
      "--provider-sasl-id",
      self.params.zk_principal_user,
      "--consumer-sasl-id",
      command.parameters["consumer.zk.sasl.id"],
    )

  def _call_exact_helper(self, arguments, environment, expected):
    result = self._call_helper(arguments, environment)
    if result != expected:
      raise ManagedDependencyFailure(
        "DEPENDENCY_PROVIDER_ACTION_FAILED",
        "The ZooKeeper helper did not return the exact expected secure evidence",
      )
    return result

  def _call_helper(self, arguments, environment):
    try:
      code, output = self.call(
        arguments,
        user=self.params.zk_user,
        env=environment,
        shell=False,
        quiet=True,
        timeout=60,
        timeout_kill_strategy=TerminateStrategy.KILL_PROCESS_GROUP,
      )
    except Exception as error:
      raise ManagedDependencyFailure(
        "DEPENDENCY_PROVIDER_ACTION_FAILED",
        "The bounded ZooKeeper provider helper could not complete",
      ) from error
    if code != 0:
      raise ManagedDependencyFailure(
        "DEPENDENCY_PROVIDER_ACTION_FAILED",
        "The secure ZooKeeper provider helper failed",
      )
    return _parse_single_helper_result(output)

  def _validate_secure_provider(self, command):
    config = self.params.zoo_cfg_properties_map
    if (
      not self.params.security_enabled
      or str(config.get("kerberos.removeHostFromPrincipal", "")).lower() != "true"
      or str(config.get("kerberos.removeRealmFromPrincipal", "")).lower() != "true"
      or config.get("security.auth_to_local", "DEFAULT") != "DEFAULT"
      or self.params.zk_principal_user == command.parameters["consumer.zk.sasl.id"]
    ):
      raise ManagedDependencyFailure(
        "DEPENDENCY_ZOOKEEPER_AUTHORIZATION_FAILED",
        "The provider ZooKeeper SASL mapping cannot enforce the approved handoff identities",
      )
    self._validate_provider_credential_material()

  def _validate_provider_credential_material(self):
    try:
      service = pwd.getpwnam(self.params.zk_user)
      service_group = grp.getgrnam(self.params.user_group)
      keytab = os.stat(self.params.zk_keytab_path, follow_symlinks=False)
      jaas = os.stat(self.params.zk_client_jaas_file, follow_symlinks=False)
      with open(self.params.zk_client_jaas_file, "r", encoding="utf-8") as stream:
        content = stream.read(32 * 1024 + 1)
    except (KeyError, OSError, TypeError) as error:
      raise ManagedDependencyFailure(
        "DEPENDENCY_ZOOKEEPER_AUTHORIZATION_FAILED",
        "The provider-local ZooKeeper credential material cannot be inspected",
      ) from error
    client_blocks = re.findall(r"(?:^|\n)\s*Client\s*\{(.*?)\};", content, re.DOTALL)
    compact = re.sub(r"\s+", "", client_blocks[0]) if len(client_blocks) == 1 else ""
    if (
      not _credential_metadata_matches(
        keytab, service.pw_uid, service_group.gr_gid, 0o400
      )
      or not _credential_metadata_matches(
        jaas, service.pw_uid, service_group.gr_gid, 0o640
      )
      or len(content.encode("utf-8")) > 32 * 1024
      or len(client_blocks) != 1
      or "useKeyTab=false" not in compact
      or "useTicketCache=true" not in compact
      or "doNotPrompt=true" not in compact
      or "useKeyTab=true" in compact
    ):
      raise ManagedDependencyFailure(
        "DEPENDENCY_ZOOKEEPER_AUTHORIZATION_FAILED",
        "The provider-local ZooKeeper credential material is unsafe or not ticket-cache based",
      )

  @staticmethod
  def _secure_handoff_facts(command):
    return {
      "applied.snapshot.fingerprint": command.parameters["snapshot.fingerprint"],
      "consumer.zk.sasl.id": command.parameters["consumer.zk.sasl.id"],
      "namespace.container.znode": command.parameters["namespace.container.znode"],
      "namespace.ledger.znode": command.parameters["namespace.ledger.znode"],
      "namespace.znode": command.parameters["namespace.znode"],
      "provider.action.host.id": command.action_host_id,
    }


def _parse_helper_result(output, expected):
  if not isinstance(output, str) or len(output.encode("utf-8")) > 64 * 1024:
    raise ManagedDependencyFailure(
      "DEPENDENCY_PROVIDER_ACTION_FAILED", "ZooKeeper helper output is missing or too large"
    )
  matches = []
  for line in output.splitlines():
    try:
      result = json.loads(line)
    except (TypeError, ValueError, json.JSONDecodeError):
      continue
    if result == expected:
      matches.append(result)
  if len(matches) != 1:
    raise ManagedDependencyFailure(
      "DEPENDENCY_PROVIDER_ACTION_FAILED",
      "ZooKeeper helper did not return the exact expected evidence",
    )
  return matches[0]


def _parse_single_helper_result(output):
  if not isinstance(output, str) or len(output.encode("utf-8")) > 64 * 1024:
    raise ManagedDependencyFailure(
      "DEPENDENCY_PROVIDER_ACTION_FAILED", "ZooKeeper helper output is missing or too large"
    )
  values = []
  for line in output.splitlines():
    try:
      value = json.loads(line)
    except (TypeError, ValueError, json.JSONDecodeError):
      continue
    if isinstance(value, dict):
      values.append(value)
  if len(values) != 1:
    raise ManagedDependencyFailure(
      "DEPENDENCY_PROVIDER_ACTION_FAILED", "ZooKeeper helper output is ambiguous"
    )
  return values[0]


def _load_command(expected_name):
  raw = Script.get_config().get("commandParams", {}).get(COMMAND_PARAMETER)
  if (
    not isinstance(raw, str)
    or not raw
    or len(raw.encode("utf-8")) > MAX_COMMAND_BYTES
  ):
    raise ManagedDependencyFailure(
      "DEPENDENCY_COMMAND_INVALID", "Managed dependency command is missing or too large"
    )
  try:
    payload = json.loads(raw)
  except (TypeError, ValueError, json.JSONDecodeError) as error:
    raise ManagedDependencyFailure(
      "DEPENDENCY_COMMAND_INVALID", "Managed dependency command is not valid JSON"
    ) from error
  return parse_managed_dependency_command(payload, expected_name)


def _require_zookeeper_provider(command):
  if command.provider_service != "ZOOKEEPER":
    raise ManagedDependencyFailure(
      "DEPENDENCY_COMMAND_INVALID",
      "The ZooKeeper dependency runner requires provider.service=ZOOKEEPER",
    )


def _result(command, status, facts, error_code=None, error_message=None):
  return {
    "commandName": command.name,
    "envelope": command.envelope,
    "status": status,
    "facts": dict(facts),
    "errorCode": error_code,
    "errorMessage": error_message,
  }
