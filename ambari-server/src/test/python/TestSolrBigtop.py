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

import ast
import importlib.util
import json
import os
from pathlib import Path
import sys
from types import ModuleType, SimpleNamespace
import unittest
from unittest.mock import MagicMock, mock_open, patch
from xml.etree import ElementTree

from ambari_commons import import_utils
from jinja2 import Environment as JinjaEnvironment, StrictUndefined
from resource_management.core.exceptions import (
  ExecutionFailed,
  Fail,
)
from resource_management.core.shell import quote_bash_args


SOLR = (
  Path(__file__).resolve().parents[2]
  / "main/resources/stacks/BIGTOP/3.2.0/services/SOLR"
)
SCRIPTS = SOLR / "package/scripts"
SOLR_33 = SOLR.parents[2] / "3.3.0/services/SOLR"
SOLR_34 = SOLR.parents[2] / "3.4.0/services/SOLR"
PID_DIR = "/var/run/solr"
SOLR_HOME = "/var/lib/solr/data"


def dependency_module(name, **attributes):
  module = ModuleType(name)
  for attribute, value in attributes.items():
    setattr(module, attribute, value)
  return module


def load_module(module_name, path, dependencies=None):
  spec = importlib.util.spec_from_file_location(module_name, path)
  module = importlib.util.module_from_spec(spec)
  with patch.dict(sys.modules, dependencies or {}):
    spec.loader.exec_module(module)
  return module


SETUP_SOLR_SCRIPT = load_module("bigtop_setup_solr_script", SCRIPTS / "setup_solr.py")
SERVICE_CHECK_SCRIPT = load_module(
  "bigtop_solr_service_check", SCRIPTS / "service_check.py"
)
SOLR_UTILS = load_module("bigtop_solr_utils", SCRIPTS / "solr_utils.py")


def params_module(**values):
  return dependency_module("params", **values)


class TestSolrSetupAndServiceCheck(unittest.TestCase):
  def test_znode_creation_uses_structured_argv_with_metacharacters(self):
    params = params_module(
      solr_bindir="/opt/solr/bin;$(id)",
      solr_znode="/solr;$(id)",
      zookeeper_quorum="zk1:2181,zk2:2181;$(id)",
      security_enabled=True,
      solr_kerberos_name_rules="RULE:[1:$1@$0](.*);$(id)",
      solr_conf="/etc/solr/conf",
      solr_user="solr",
    )
    with patch.dict(sys.modules, {"params": params}), \
      patch.object(SETUP_SOLR_SCRIPT, "Execute") as execute:
      SETUP_SOLR_SCRIPT.create_solr_znode()

    execute.assert_called_once_with(
      (
        "/opt/solr/bin;$(id)/solr",
        "zk",
        "mkroot",
        "/solr;$(id)",
        "-z",
        "zk1:2181,zk2:2181;$(id)",
      ),
      environment={"SOLR_INCLUDE": "/etc/solr/conf/solr-env.sh"},
      user="solr",
      logoutput=True,
    )

  def test_znode_creation_ignores_only_matching_node_exists(self):
    params = params_module(
      solr_bindir="/opt/solr/bin",
      solr_znode="/solr",
      zookeeper_quorum="zk1:2181",
      security_enabled=False,
      solr_conf="/etc/solr/conf",
      solr_user="solr",
    )
    node_exists = ExecutionFailed("NodeExists for /solr", 1, "")
    unrelated = ExecutionFailed("ZooKeeper connection failed", 1, "")
    with patch.dict(sys.modules, {"params": params}), \
      patch.object(SETUP_SOLR_SCRIPT, "Execute", side_effect=node_exists), \
      patch.object(SETUP_SOLR_SCRIPT.Logger, "info") as logger:
      SETUP_SOLR_SCRIPT.create_solr_znode()
    logger.assert_called_once_with("Node /solr already exists.")

    with patch.dict(sys.modules, {"params": params}), \
      patch.object(SETUP_SOLR_SCRIPT, "Execute", side_effect=unrelated):
      with self.assertRaises(ExecutionFailed) as raised:
        SETUP_SOLR_SCRIPT.create_solr_znode()
    self.assertIs(unrelated, raised.exception)

  def test_secure_znode_setup_uploads_security_json_and_applies_acl(self):
    params = params_module(
      solr_bindir="/usr/lib/solr/bin",
      solr_znode="/solr",
      zookeeper_quorum="zk1:2181,zk2:2181",
      security_enabled=True,
      solr_security_manually_managed=False,
      solr_security_json_content="",
      solr_conf="/etc/solr/conf",
      solr_user="solr",
      ambari_java_exec="/usr/lib/jvm/java-17/bin/java",
      ambari_java_home="/usr/lib/jvm/java-17",
      solr_jaas_file="/etc/solr/conf/solr_jaas.conf",
      solr_kerberos_service_user="solr",
    )
    with patch.dict(sys.modules, {"params": params}), \
      patch.object(SETUP_SOLR_SCRIPT, "Execute") as execute, \
      patch.object(SETUP_SOLR_SCRIPT, "ZkMigrator") as migrator:
      SETUP_SOLR_SCRIPT.setup_solr_znode_env()

    self.assertEqual(2, execute.call_count)
    self.assertEqual(
      (
        "/usr/lib/solr/bin/solr",
        "zk",
        "cp",
        "file:/etc/solr/conf/security.json",
        "zk:/security.json",
        "-z",
        "zk1:2181,zk2:2181/solr",
      ),
      execute.call_args_list[1].args[0],
    )
    migrator.return_value.set_acls.assert_called_once_with(
      "/solr", "sasl:solr:cdrwa"
    )

  def test_manual_security_management_skips_zookeeper_security_upload(self):
    params = params_module(
      solr_bindir="/usr/lib/solr/bin",
      solr_znode="/solr",
      zookeeper_quorum="zk1:2181",
      security_enabled=True,
      solr_security_manually_managed=True,
      solr_conf="/etc/solr/conf",
      solr_user="solr",
      ambari_java_exec="/usr/lib/jvm/java-17/bin/java",
      ambari_java_home="/usr/lib/jvm/java-17",
      solr_jaas_file="/etc/solr/conf/solr_jaas.conf",
      solr_kerberos_service_user="solr",
    )
    with patch.dict(sys.modules, {"params": params}), \
      patch.object(SETUP_SOLR_SCRIPT, "Execute") as execute, \
      patch.object(SETUP_SOLR_SCRIPT, "ZkMigrator") as migrator:
      SETUP_SOLR_SCRIPT.setup_solr_znode_env()

    self.assertEqual(1, execute.call_count)
    migrator.return_value.set_acls.assert_called_once()

  def test_insecure_znode_setup_does_not_hide_security_removal_failure(self):
    params = params_module(
      solr_bindir="/usr/lib/solr/bin",
      solr_znode="/solr",
      zookeeper_quorum="zk1:2181",
      security_enabled=False,
      solr_security_manually_managed=False,
      solr_conf="/etc/solr/conf",
      solr_user="solr",
    )
    removal_failure = ExecutionFailed("ZooKeeper connection failed", 1, "")
    with patch.dict(sys.modules, {"params": params}), \
      patch.object(
        SETUP_SOLR_SCRIPT,
        "Execute",
        side_effect=(None, removal_failure),
      ):
      with self.assertRaises(ExecutionFailed) as raised:
        SETUP_SOLR_SCRIPT.setup_solr_znode_env()
    self.assertIs(removal_failure, raised.exception)

  def test_setup_preserves_package_tree_and_protects_sensitive_configs(self):
    params = params_module(
      solr_log_dir="/var/log/solr",
      solr_piddir=PID_DIR,
      solr_datadir=SOLR_HOME,
      solr_data_resources_dir=f"{SOLR_HOME}/resources",
      solr_user="solr",
      user_group="hadoop",
      solr_conf="/etc/solr/conf",
      solr_env_content="env template",
      solr_xml_content="solr xml",
      solr_log4j_content="log4j",
      solr_security_json_content="security",
      security_enabled=True,
      solr_jaas_file="/etc/solr/conf/solr_jaas.conf",
      limits_conf_dir="/missing/limits.d",
    )

    def render(value):
      return value.format(
        solr_conf=params.solr_conf,
        solr_datadir=params.solr_datadir,
        solr_jaas_file=params.solr_jaas_file,
      )

    with patch.dict(sys.modules, {"params": params}), \
      patch.object(SETUP_SOLR_SCRIPT, "Directory") as directory, \
      patch.object(SETUP_SOLR_SCRIPT, "File") as file_resource, \
      patch.object(SETUP_SOLR_SCRIPT, "InlineTemplate"), \
      patch.object(SETUP_SOLR_SCRIPT, "Template"), \
      patch.object(SETUP_SOLR_SCRIPT, "format", side_effect=render), \
      patch.object(SETUP_SOLR_SCRIPT.os.path, "exists", return_value=False):
      SETUP_SOLR_SCRIPT.setup_solr("server")

    self.assertEqual(2, directory.call_count)
    runtime_call, config_call = directory.call_args_list
    self.assertEqual(
      [
        "/var/log/solr",
        PID_DIR,
        SOLR_HOME,
        f"{SOLR_HOME}/resources",
      ],
      runtime_call.args[0],
    )
    self.assertEqual("solr", runtime_call.kwargs["owner"])
    self.assertEqual("/etc/solr/conf", config_call.args[0])
    self.assertEqual("root", config_call.kwargs["owner"])
    self.assertFalse(
      any(call_args.kwargs.get("recursive_ownership") for call_args in directory.call_args_list)
    )
    self.assertFalse(
      any("/usr/lib/solr" in str(call_args) for call_args in directory.call_args_list)
    )

    files = {call_args.args[0]: call_args.kwargs for call_args in file_resource.call_args_list}
    self.assertEqual(0o600, files["/etc/solr/conf/solr-env.sh"]["mode"])
    self.assertEqual("solr", files["/etc/solr/conf/solr-env.sh"]["owner"])
    self.assertEqual(0o600, files["/etc/solr/conf/solr_jaas.conf"]["mode"])

  def _service_params(self, secure):
    return params_module(
      config={"configurations": {"solr-env": {}}},
      solr_hosts=["host.example;$(id)"],
      solr_ssl_enabled=secure,
      solr_port="8983;$(id)",
      security_enabled=secure,
      kinit_path_local="/usr/bin/kinit;$(id)",
      smoke_user_keytab="/tmp/smoke keytab;$(id)",
      smokeuser_principal="smoke/host@REALM;$(id)",
      smokeuser="ambari-qa",
      user_group="hadoop",
    )

  def test_service_check_uses_verified_insecure_health_endpoint(self):
    params = self._service_params(False)
    service = SERVICE_CHECK_SCRIPT.ServiceCheck()
    env = MagicMock()
    with patch.dict(sys.modules, {"params": params}), \
      patch.object(SERVICE_CHECK_SCRIPT, "Execute") as execute:
      service.service_check(env)

    command = execute.call_args.args[0]
    self.assertIsInstance(command, tuple)
    self.assertEqual(
      "http://host.example;$(id):8983;$(id)/solr/admin/info/system?wt=json",
      command[-1],
    )
    self.assertIn("--fail", command)
    self.assertEqual("--disable", command[1])
    self.assertIn("--max-time", command)
    self.assertNotIn("--negotiate", command)
    self.assertNotIn("-k", command)
    self.assertNotIn("#/", command[-1])

  def test_secure_service_check_separates_kinit_and_curl_argv(self):
    params = self._service_params(True)
    service = SERVICE_CHECK_SCRIPT.ServiceCheck()
    with patch.dict(sys.modules, {"params": params}), \
      patch.object(
        SERVICE_CHECK_SCRIPT.uuid,
        "uuid4",
        return_value=SimpleNamespace(hex="cache-one"),
      ), \
      patch.object(SERVICE_CHECK_SCRIPT.os, "mkdir") as mkdir, \
      patch.object(SERVICE_CHECK_SCRIPT, "Directory") as directory, \
      patch.object(SERVICE_CHECK_SCRIPT, "Execute") as execute, \
      patch.object(SERVICE_CHECK_SCRIPT, "File") as file_resource:
      service.service_check(MagicMock())

    ccache_dir = "/tmp/ambari-solr-service-check-cache-one"
    ccache_path = f"{ccache_dir}/krb5cc"
    kerberos_environment = {"KRB5CCNAME": f"FILE:{ccache_path}"}
    mkdir.assert_called_once_with(ccache_dir, 0o700)
    self.assertEqual(
      [
        ((ccache_dir,), {"owner": "ambari-qa", "group": "hadoop", "mode": 0o700}),
        ((ccache_dir,), {"action": "delete"}),
      ],
      [(call.args, call.kwargs) for call in directory.call_args_list],
    )
    self.assertEqual(2, execute.call_count)
    self.assertEqual(
      (
        "/usr/bin/kinit;$(id)",
        "-c",
        f"FILE:{ccache_path}",
        "-kt",
        "/tmp/smoke keytab;$(id)",
        "smoke/host@REALM;$(id)",
      ),
      execute.call_args_list[0].args[0],
    )
    self.assertIs(
      execute.call_args_list[0].kwargs["environment"],
      execute.call_args_list[1].kwargs["environment"],
    )
    self.assertEqual(
      kerberos_environment,
      execute.call_args_list[0].kwargs["environment"],
    )
    curl_command = execute.call_args_list[1].args[0]
    self.assertIn("--negotiate", curl_command)
    self.assertIn(("--user", ":"), tuple(zip(curl_command, curl_command[1:])))
    self.assertTrue(curl_command[-1].startswith("https://"))
    self.assertNotIn("-k", curl_command)
    file_resource.assert_called_once_with(ccache_path, action="delete")

  def test_secure_service_checks_use_unique_private_caches(self):
    params = self._service_params(True)
    service = SERVICE_CHECK_SCRIPT.ServiceCheck()
    with patch.dict(sys.modules, {"params": params}), \
      patch.object(
        SERVICE_CHECK_SCRIPT.uuid,
        "uuid4",
        side_effect=(
          SimpleNamespace(hex="cache-one"),
          SimpleNamespace(hex="cache-two"),
        ),
      ), \
      patch.object(SERVICE_CHECK_SCRIPT.os, "mkdir") as mkdir, \
      patch.object(SERVICE_CHECK_SCRIPT, "Directory") as directory, \
      patch.object(SERVICE_CHECK_SCRIPT, "Execute") as execute, \
      patch.object(SERVICE_CHECK_SCRIPT, "File") as file_resource:
      service.service_check(MagicMock())
      service.service_check(MagicMock())

    first_environment = execute.call_args_list[0].kwargs["environment"]
    second_environment = execute.call_args_list[2].kwargs["environment"]
    self.assertNotEqual(first_environment, second_environment)
    self.assertEqual(
      [
        "/tmp/ambari-solr-service-check-cache-one/krb5cc",
        "/tmp/ambari-solr-service-check-cache-two/krb5cc",
      ],
      [call_args.args[0] for call_args in file_resource.call_args_list],
    )
    self.assertEqual(2, mkdir.call_count)
    self.assertEqual(4, directory.call_count)

  def test_service_check_propagates_kinit_and_curl_failures(self):
    params = self._service_params(True)
    service = SERVICE_CHECK_SCRIPT.ServiceCheck()
    for side_effect, expected_calls in (
      (Fail("kinit failed"), 1),
      ((None, Fail("curl failed")), 2),
    ):
      with self.subTest(expected_calls=expected_calls):
        with patch.dict(sys.modules, {"params": params}), \
          patch.object(
            SERVICE_CHECK_SCRIPT.uuid,
            "uuid4",
            return_value=SimpleNamespace(hex="failed-cache"),
          ), \
          patch.object(SERVICE_CHECK_SCRIPT.os, "mkdir"), \
          patch.object(SERVICE_CHECK_SCRIPT, "Directory") as directory, \
          patch.object(
            SERVICE_CHECK_SCRIPT, "Execute", side_effect=side_effect
          ) as execute, \
          patch.object(SERVICE_CHECK_SCRIPT, "File") as file_resource:
          with self.assertRaises(Fail):
            service.service_check(MagicMock())
        self.assertEqual(expected_calls, execute.call_count)
        file_resource.assert_called_once_with(
          "/tmp/ambari-solr-service-check-failed-cache/krb5cc",
          action="delete",
        )
        self.assertEqual("delete", directory.call_args_list[-1].kwargs["action"])

  def test_private_cache_creation_collision_and_failure_are_fail_closed(self):
    params = self._service_params(True)
    for create_error in (
      FileExistsError("collision"),
      OSError("read-only filesystem"),
    ):
      with self.subTest(create_error=create_error):
        with patch.dict(sys.modules, {"params": params}), \
          patch.object(
            SERVICE_CHECK_SCRIPT.uuid,
            "uuid4",
            return_value=SimpleNamespace(hex="collision"),
          ), \
          patch.object(
            SERVICE_CHECK_SCRIPT.os, "mkdir", side_effect=create_error
          ), \
          patch.object(SERVICE_CHECK_SCRIPT, "Directory") as directory, \
          patch.object(SERVICE_CHECK_SCRIPT, "Execute") as execute:
          with self.assertRaises(Fail):
            SERVICE_CHECK_SCRIPT.ServiceCheck().service_check(MagicMock())
        directory.assert_not_called()
        execute.assert_not_called()

  def test_private_cache_ownership_failure_rolls_back_created_directory(self):
    params = self._service_params(True)
    cache_dir = "/tmp/ambari-solr-service-check-ownership-failure"
    with patch.dict(sys.modules, {"params": params}), \
      patch.object(
        SERVICE_CHECK_SCRIPT.uuid,
        "uuid4",
        return_value=SimpleNamespace(hex="ownership-failure"),
      ), \
      patch.object(SERVICE_CHECK_SCRIPT.os, "mkdir") as mkdir, \
      patch.object(
        SERVICE_CHECK_SCRIPT,
        "Directory",
        side_effect=(Fail("could not set ownership"), None),
      ) as directory, \
      patch.object(SERVICE_CHECK_SCRIPT, "Execute") as execute:
      with self.assertRaisesRegex(Fail, "could not set ownership"):
        SERVICE_CHECK_SCRIPT.ServiceCheck().service_check(MagicMock())

    mkdir.assert_called_once_with(cache_dir, 0o700)
    self.assertEqual(2, directory.call_count)
    directory.assert_called_with(cache_dir, action="delete")
    execute.assert_not_called()

  def test_ownership_and_rollback_failures_preserve_ownership_as_cause(self):
    params = self._service_params(True)
    ownership_error = Fail("ownership setup failed")
    rollback_error = Fail("rollback delete failed")
    with patch.dict(sys.modules, {"params": params}), \
      patch.object(
        SERVICE_CHECK_SCRIPT.uuid,
        "uuid4",
        return_value=SimpleNamespace(hex="double-failure"),
      ), \
      patch.object(SERVICE_CHECK_SCRIPT.os, "mkdir"), \
      patch.object(
        SERVICE_CHECK_SCRIPT,
        "Directory",
        side_effect=(ownership_error, rollback_error),
      ), \
      patch.object(SERVICE_CHECK_SCRIPT, "Execute") as execute:
      with self.assertRaisesRegex(
        Fail,
        "ownership setup failed; rollback also failed: rollback delete failed",
      ) as raised:
        SERVICE_CHECK_SCRIPT.ServiceCheck().service_check(MagicMock())

    self.assertIs(ownership_error, raised.exception.__cause__)
    execute.assert_not_called()

  def test_private_cache_cleanup_failures_are_reported_after_full_cleanup(self):
    params = self._service_params(True)
    cleanup_cases = (
      (Fail("cache delete failed"), (None, None)),
      (None, (None, Fail("directory delete failed"))),
    )
    for file_error, directory_effects in cleanup_cases:
      with self.subTest(
        file_error=file_error,
        directory_effects=directory_effects,
      ):
        with patch.dict(sys.modules, {"params": params}), \
          patch.object(
            SERVICE_CHECK_SCRIPT.uuid,
            "uuid4",
            return_value=SimpleNamespace(hex="cleanup-failure"),
          ), \
          patch.object(SERVICE_CHECK_SCRIPT.os, "mkdir"), \
          patch.object(
            SERVICE_CHECK_SCRIPT,
            "Directory",
            side_effect=directory_effects,
          ) as directory, \
          patch.object(SERVICE_CHECK_SCRIPT, "Execute"), \
          patch.object(
            SERVICE_CHECK_SCRIPT,
            "File",
            side_effect=file_error,
          ) as file_resource:
          with self.assertRaisesRegex(Fail, "Could not remove Kerberos cache"):
            SERVICE_CHECK_SCRIPT.ServiceCheck().service_check(MagicMock())
        file_resource.assert_called_once()
        self.assertEqual(2, directory.call_count)

  def test_primary_service_check_failure_is_preserved_when_cleanup_also_fails(self):
    params = self._service_params(True)
    cases = (
      (
        Fail("kinit primary failure"),
        Fail("cache file cleanup failure"),
        (None, None),
        1,
        "cache file cleanup failure",
      ),
      (
        (None, Fail("curl primary failure")),
        None,
        (None, Fail("cache directory cleanup failure")),
        2,
        "cache directory cleanup failure",
      ),
    )
    for (
      execute_effect,
      file_effect,
      directory_effects,
      expected_calls,
      cleanup_message,
    ) in cases:
      with self.subTest(expected_calls=expected_calls):
        primary_error = (
          execute_effect
          if isinstance(execute_effect, Fail)
          else execute_effect[1]
        )
        with patch.dict(sys.modules, {"params": params}), \
          patch.object(
            SERVICE_CHECK_SCRIPT.uuid,
            "uuid4",
            return_value=SimpleNamespace(hex="primary-and-cleanup"),
          ), \
          patch.object(SERVICE_CHECK_SCRIPT.os, "mkdir"), \
          patch.object(
            SERVICE_CHECK_SCRIPT,
            "Directory",
            side_effect=directory_effects,
          ), \
          patch.object(
            SERVICE_CHECK_SCRIPT,
            "Execute",
            side_effect=execute_effect,
          ) as execute, \
          patch.object(
            SERVICE_CHECK_SCRIPT,
            "File",
            side_effect=file_effect,
          ):
          with self.assertRaisesRegex(
            Fail,
            f"Solr service check failed: {primary_error}; "
            f"Kerberos cache cleanup also failed: .*{cleanup_message}",
          ) as raised:
            SERVICE_CHECK_SCRIPT.ServiceCheck().service_check(MagicMock())

        self.assertIs(primary_error, raised.exception.__cause__)
        self.assertEqual(expected_calls, execute.call_count)

  def test_service_check_fails_when_service_configuration_or_hosts_are_missing(self):
    for configurations, hosts in (
      ({}, ["host.example"]),
      ({"solr-env": {}}, None),
      ({"solr-env": {}}, []),
    ):
      with self.subTest(configurations=configurations, hosts=hosts):
        params = params_module(
          config={"configurations": configurations},
          solr_hosts=hosts,
        )
        with patch.dict(sys.modules, {"params": params}), \
          patch.object(SERVICE_CHECK_SCRIPT, "Execute") as execute, \
          patch.object(SERVICE_CHECK_SCRIPT, "File") as file_resource:
          with self.assertRaises(Fail):
            SERVICE_CHECK_SCRIPT.ServiceCheck().service_check(MagicMock())
        execute.assert_not_called()
        file_resource.assert_not_called()

  def test_solr_env_template_quotes_shell_metacharacters(self):
    payload = "value;$(touch /tmp/not-created) `id` 'quoted'"
    quoted = quote_bash_args(payload)
    assignments = {
      name: quoted
      for name in (
        "solr_java_home_assignment",
        "solr_java_mem_assignment",
        "solr_java_stack_size_assignment",
        "solr_gc_log_opts_assignment",
        "solr_gc_tune_assignment",
        "solr_zk_host_assignment",
        "solr_jmx_enabled_assignment",
        "solr_jmx_port_assignment",
        "solr_rmi_hostname_assignment",
        "solr_extra_java_opts_assignment",
        "solr_piddir_assignment",
        "solr_datadir_assignment",
        "solr_log4j_props_assignment",
        "solr_log_dir_assignment",
        "solr_port_assignment",
        "solr_keystore_location_assignment",
        "solr_keystore_password_assignment",
        "solr_truststore_location_assignment",
        "solr_truststore_password_assignment",
        "solr_jaas_file_assignment",
        "solr_web_kerberos_keytab_assignment",
        "solr_web_kerberos_principal_assignment",
        "solr_hdfs_kerberos_option_assignment",
        "solr_kerberos_name_rules_assignment",
        "zk_security_opts_assignment",
      )
    }
    template = JinjaEnvironment(undefined=StrictUndefined).from_string(
      (SOLR / "properties/solr-env.sh.j2").read_text(encoding="utf-8")
    )
    rendered = template.render(
      **assignments,
      solr_extra_java_opts=payload,
      solr_ssl_enabled=True,
      security_enabled=True,
    )

    for variable in (
      "ZK_HOST",
      "SOLR_HOST",
      "SOLR_PID_DIR",
      "SOLR_HOME",
      "SOLR_SSL_KEY_STORE_PASSWORD",
      "SOLR_SSL_TRUST_STORE_PASSWORD",
      "SOLR_KERB_KEYTAB",
      "SOLR_KERB_PRINCIPAL",
      "SOLR_KERBEROS_NAME_RULES_OPTION",
      "SOLR_EXTRA_JAVA_OPTS",
    ):
      self.assertIn(f"{variable}={quoted}", rendered)
      self.assertNotIn(f"{variable}={payload}", rendered)


class TestSolrSourceContracts(unittest.TestCase):
  def test_security_users_are_derived_only_from_present_bigtop_services(self):
    key = "xasecure.audit.jaas.Client.option.principal"
    users = SOLR_UTILS.configured_principal_users(
      {
        "ranger-hdfs-audit": {key: "nn/host@EXAMPLE.COM"},
        "ranger-kafka-audit": {key: "kafka@EXAMPLE.COM"},
        "ranger-knox-audit": {key: "knox@EXAMPLE.COM"},
      },
      ("ranger-hdfs-audit", "ranger-kafka-audit"),
      key,
    )
    self.assertEqual(("nn", "kafka"), users)

  def test_security_user_parser_rejects_injection_and_duplicates(self):
    with self.assertRaisesRegex(Fail, "valid user"):
      SOLR_UTILS.parse_users('hive,evil"user', "audit users")
    with self.assertRaisesRegex(Fail, "duplicate"):
      SOLR_UTILS.parse_users("hive,hive", "audit users")

  def test_custom_security_json_must_be_one_valid_object(self):
    self.assertEqual(
      '{"authentication": {}}',
      SOLR_UTILS.validate_json_object(
        '{"authentication": {}}', "custom security.json"
      ),
    )
    with self.assertRaisesRegex(Fail, "valid JSON"):
      SOLR_UTILS.validate_json_object("{invalid", "custom security.json")
    with self.assertRaisesRegex(Fail, "one JSON object"):
      SOLR_UTILS.validate_json_object("[]", "custom security.json")

  def test_zookeeper_quorum_accepts_ipv6_and_rejects_invalid_ports(self):
    self.assertEqual(
      "zk1.example:2181,[2001:db8::1]:2181",
      SOLR_UTILS.validate_zookeeper_quorum(
        "ZK1.EXAMPLE:2181,[2001:DB8::1]:2181"
      ),
    )
    with self.assertRaisesRegex(Fail, "between 1 and 65535"):
      SOLR_UTILS.validate_zookeeper_quorum("zk1.example:70000")

  def test_security_template_is_valid_without_optional_ranger_integrations(self):
    template = JinjaEnvironment(undefined=StrictUndefined).from_string(
      (SOLR / "package/templates/solr-security.json.j2").read_text(
        encoding="utf-8"
      )
    )
    rendered = template.render(
      solr_kerberos_service_user="solr",
      kerberos_realm="EXAMPLE.COM",
      solr_ranger_audit_service_users=(),
      ranger_admin_kerberos_service_user=None,
      solr_role_ranger_admin="ranger_admin_user",
      solr_role_ranger_audit="ranger_audit_user",
      solr_role_dev="dev",
      ranger_solr_collection_name="ranger_audits",
    )
    document = json.loads(rendered)
    self.assertEqual(
      {"solr@EXAMPLE.COM": "admin"},
      document["authorization"]["user-role"],
    )
    self.assertNotIn("atlas", rendered.lower())
    self.assertNotIn("logsearch", rendered.lower())
    self.assertNotIn('"role" :null', rendered)

  def test_params_imports_expect_for_required_java_version(self):
    source = (SCRIPTS / "params.py").read_text(encoding="utf-8")
    tree = ast.parse(source)
    expect_imports = {
      alias.name
      for node in ast.walk(tree)
      if isinstance(node, ast.ImportFrom)
      and node.module == "resource_management.libraries.functions.expect"
      for alias in node.names
    }
    self.assertIn("expect", expect_imports)

    java_version = next(
      node.value
      for node in ast.walk(tree)
      if isinstance(node, ast.Assign)
      and any(
        isinstance(target, ast.Name) and target.id == "java_version"
        for target in node.targets
      )
    )
    self.assertEqual("expect", java_version.func.id)
    self.assertEqual("/ambariLevelParams/java_version", java_version.args[0].value)
    self.assertEqual("int", java_version.args[1].id)

  def test_solr_has_no_phantom_infra_client_or_default_tls_password(self):
    setup_source = (SCRIPTS / "setup_solr.py").read_text(encoding="utf-8")
    environment = (SOLR / "configuration/solr-env.xml").read_text(
      encoding="utf-8"
    )
    self.assertNotIn("setup_solr_client", setup_source)
    self.assertNotIn('name == "client"', setup_source)
    self.assertNotIn("infra.solr", environment)
    self.assertNotIn("<value>bigdata</value>", environment)

  def test_metainfo_matches_bigtop_solr_package_contract(self):
    base_metainfo_path = SOLR / "metainfo.xml"
    override_metainfo_path = SOLR_33 / "metainfo.xml"
    stack_metainfo_path = SOLR_33.parents[1] / "metainfo.xml"
    base_service = ElementTree.parse(base_metainfo_path).find("./services/service")
    override_source = override_metainfo_path.read_text(encoding="utf-8")
    override_service = ElementTree.parse(override_metainfo_path).find(
      "./services/service"
    )

    self.assertEqual(
      "3.2.0", ElementTree.parse(stack_metainfo_path).findtext("extends")
    )
    self.assertEqual("8.11.2-1", base_service.findtext("version"))
    self.assertEqual("8.11.3-2", override_service.findtext("version"))
    packages_by_os = {
      os_specific.findtext("osFamily"): [
        package.findtext("name")
        for package in os_specific.findall("./packages/package")
      ]
      for os_specific in base_service.findall("./osSpecifics/osSpecific")
    }
    self.assertEqual(
      {
        "redhat8,redhat9,openeuler22": ["solr_${stack_version}", "curl"],
        "ubuntu22": ["solr-${stack_version}", "curl"],
      },
      packages_by_os,
    )
    self.assertEqual(
      "8.11.3-2",
      ElementTree.parse(SOLR_34 / "metainfo.xml").findtext(
        "./services/service/version"
      ),
    )
    self.assertEqual(
      "scripts/solr.py",
      base_service.findtext("./components/component/commandScript/script"),
    )
    self.assertNotIn("9.1.1", override_source)
    stack_packages = json.loads(
      (SOLR.parents[1] / "properties/stack_packages.json").read_text(
        encoding="utf-8"
      )
    )
    solr_packages = stack_packages["BIGTOP"]["stack-select"]["SOLR"][
      "SOLR_SERVER"
    ]
    self.assertEqual("solr-server", solr_packages["STACK-SELECT-PACKAGE"])
    for command_type in ("INSTALL", "PATCH", "STANDARD"):
      self.assertEqual(["solr"], solr_packages[command_type])

  def test_service_descriptors_have_no_phantom_solr_client(self):
    kerberos = json.loads((SOLR / "kerberos.json").read_text(encoding="utf-8"))
    components = kerberos["services"][0]["components"]
    self.assertEqual(["SOLR_SERVER"], [item["name"] for item in components])
    directories_theme = (SOLR / "themes/directories.json").read_text(
      encoding="utf-8"
    )
    self.assertNotIn("SOLR_CLIENT", directories_theme)
    self.assertNotIn("solr-client", directories_theme)
    command_order = json.loads(
      (SOLR / "role_command_order.json").read_text(encoding="utf-8")
    )["general_deps"]
    self.assertEqual(
      ["ZOOKEEPER_SERVER-START"], command_order["SOLR_SERVER-START"]
    )
    self.assertEqual(
      ["SOLR_SERVER-START"],
      command_order["SOLR_SERVICE_CHECK-SERVICE_CHECK"],
    )
    self.assertNotIn("SOLR-START", command_order)

  def test_service_advisor_parent_load_failure_preserves_original_cause(self):
    advisor_path = SOLR / "service_advisor.py"
    with patch.dict(os.environ, {"BASE_SERVICE_ADVISOR": "/missing/base-advisor.py"}), \
      patch("builtins.open", mock_open(read_data=b"")), \
      patch.object(
        import_utils, "load_module", side_effect=ValueError("invalid parent")
      ):
      with self.assertRaisesRegex(
        RuntimeError, "Failed to load parent service advisor /missing/base-advisor.py"
      ) as raised:
        load_module("bigtop_solr_broken_advisor", advisor_path)

    self.assertIsInstance(raised.exception.__cause__, ValueError)

  def test_service_advisor_rejects_unsafe_runtime_and_security_values(self):
    advisor_path = SOLR / "service_advisor.py"
    parent_path = SOLR.parents[3] / "service_advisor.py"
    with patch.dict(os.environ, {"BASE_SERVICE_ADVISOR": str(parent_path)}):
      advisor = load_module("bigtop_solr_advisor_validation", advisor_path)
    validator = object.__new__(advisor.SolrValidator)

    environment = {
      "solr_port": "8983",
      "solr_jmx_port": "8983",
      "solr_minmem": "2048",
      "solr_maxmem": "1024",
      "solr_java_stack_size": "1",
      "solr_user_nofile_limit": "128000",
      "solr_user_nproc_limit": "65536",
      "solr_datadir": "/",
      "solr_log_dir": "/var/log/solr",
      "solr_pid_dir": "/var/run/solr",
      "solr_znode": "/",
      "solr_ssl_enabled": "false",
      "solr_jmx_enabled": "invalid",
      "solr_zookeeper_external_enabled": "false",
      "solr_user": "solr",
      "solr_keystore_location": "/etc/security/serverKeys/solr.jks",
      "solr_truststore_location": "/etc/security/serverKeys/trust.jks",
    }
    problems = validator.validate_environment(environment, {}, {}, {}, {})
    self.assertTrue(
      {
        "solr_jmx_port",
        "solr_minmem",
        "solr_datadir",
        "solr_znode",
        "solr_jmx_enabled",
      }.issubset({problem["config-name"] for problem in problems})
    )

    security = {
      "solr_role_ranger_admin": "duplicate",
      "solr_role_ranger_audit": "duplicate",
      "solr_role_dev": "dev",
      "solr_ranger_audit_service_users": "hive,knox",
      "solr_security_manually_managed": "false",
    }
    problems = validator.validate_security(security, {}, {}, {}, {})
    self.assertEqual(
      {"solr_role_dev", "solr_ranger_audit_service_users"},
      {problem["config-name"] for problem in problems},
    )

if __name__ == "__main__":
  unittest.main()
