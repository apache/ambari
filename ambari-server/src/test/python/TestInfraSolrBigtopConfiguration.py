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
from pathlib import Path
import sys
from types import ModuleType
import unittest
from unittest.mock import MagicMock, patch
from xml.etree import ElementTree

from resource_management.core.exceptions import Fail


SERVICE = (
  Path(__file__).resolve().parents[2]
  / "main/resources/common-services/AMBARI_INFRA_SOLR/3.0.0"
)
SCRIPTS = SERVICE / "package/scripts"


def load_module(name, path, dependencies=None):
  spec = importlib.util.spec_from_file_location(name, path)
  module = importlib.util.module_from_spec(spec)
  with patch.dict(sys.modules, dependencies or {}):
    spec.loader.exec_module(module)
  return module


def params_module(**values):
  module = ModuleType("params")
  for name, value in values.items():
    setattr(module, name, value)
  return module


UTILS = load_module("bigtop_infra_solr_config_utils", SCRIPTS / "infra_solr_utils.py")
CHECK = load_module(
  "bigtop_infra_solr_check",
  SCRIPTS / "service_check.py",
  {"infra_solr_utils": UTILS},
)
ADVISOR = load_module(
  "bigtop_infra_solr_advisor", SERVICE / "service_advisor.py"
)


class TestInfraSolrServiceCheck(unittest.TestCase):
  def test_synchronous_commands_use_process_group_timeouts(self):
    for script_path in SCRIPTS.glob("*.py"):
      tree = ast.parse(script_path.read_text(encoding="utf-8"))
      for node in ast.walk(tree):
        if not (
          isinstance(node, ast.Call)
          and isinstance(node.func, ast.Name)
          and node.func.id == "Execute"
        ):
          continue
        keywords = {keyword.arg: keyword.value for keyword in node.keywords}
        self.assertIn("timeout", keywords, f"{script_path.name}:{node.lineno}")
        strategy = keywords.get("timeout_kill_strategy")
        self.assertIsInstance(strategy, ast.Attribute)
        self.assertEqual("KILL_PROCESS_GROUP", strategy.attr)

  def test_secure_check_uses_private_cache_and_falls_back_across_hosts(self):
    params = params_module(
      infra_solr_hosts=("solr1.example.com", "solr2.example.com"),
      infra_solr_port=8886,
      infra_solr_ssl_enabled=False,
      security_enabled=True,
      kinit_path_local="/usr/bin/kinit",
      smoke_user_keytab="/etc/security/keytabs/smoke.keytab",
      smokeuser_principal="ambari-qa@EXAMPLE.COM",
      smokeuser="ambari-qa",
      user_group="hadoop",
      tmp_dir="/var/lib/ambari-agent/tmp",
    )
    cache = MagicMock()
    cache.environment = {"KRB5CCNAME": "FILE:/private/krb5cc"}
    context = MagicMock()
    context.__enter__.return_value = cache
    context.__exit__.return_value = False
    with patch.dict(sys.modules, {"params": params}), \
      patch.object(UTILS, "validate_executable"), \
      patch.object(UTILS, "validate_keytab"), \
      patch.object(UTILS, "validate_principal"), \
      patch.object(CHECK, "PrivateKerberosCache", return_value=context) as factory, \
      patch.object(CHECK, "Execute", side_effect=(Fail("first"), None)) as execute:
      CHECK.InfraServiceCheck().service_check(MagicMock())
    factory.assert_called_once()
    cache.kinit.assert_called_once()
    self.assertEqual(2, execute.call_count)
    first_url = execute.call_args_list[0].args[0][-1]
    second_url = execute.call_args_list[1].args[0][-1]
    self.assertIn("solr1.example.com", first_url)
    self.assertIn("solr2.example.com", second_url)
    self.assertEqual(
      "FILE:/private/krb5cc",
      execute.call_args_list[1].kwargs["environment"]["KRB5CCNAME"],
    )
    self.assertNotIn("--insecure", execute.call_args_list[0].args[0])
    context.__exit__.assert_called_once()

  def test_all_hosts_failure_reports_each_server(self):
    params = params_module(
      infra_solr_hosts=("solr1.example.com", "solr2.example.com"),
      infra_solr_port=8886,
      infra_solr_ssl_enabled=True,
      security_enabled=False,
      smokeuser="ambari-qa",
    )
    with patch.dict(sys.modules, {"params": params}), \
      patch.object(UTILS, "validate_executable"), \
      patch.object(CHECK, "Execute", side_effect=Fail("unavailable")) as execute, \
      self.assertRaisesRegex(Fail, "all Infra Solr servers") as raised:
      CHECK.InfraServiceCheck().service_check(MagicMock())
    self.assertEqual(2, execute.call_count)
    self.assertIn("solr1.example.com", str(raised.exception))
    self.assertIn("solr2.example.com", str(raised.exception))
    self.assertNotIn("--insecure", execute.call_args_list[0].args[0])

  def test_missing_hosts_fail_before_any_network_request(self):
    params = params_module(infra_solr_hosts=())
    with patch.dict(sys.modules, {"params": params}), \
      patch.object(CHECK, "Execute") as execute, \
      self.assertRaisesRegex(Fail, "at least one"):
      CHECK.InfraServiceCheck().service_check(MagicMock())
    execute.assert_not_called()


class TestInfraSolrMetadata(unittest.TestCase):
  def test_bigtop_packages_and_os_families_match_current_contract(self):
    root = ElementTree.parse(SERVICE / "metainfo.xml")
    packages = {
      element.text for element in root.findall(".//package/name")
    }
    self.assertEqual(
      {"ambari-infra-solr", "ambari-infra-solr-client"}, packages
    )
    os_families = {element.text for element in root.findall(".//osFamily")}
    self.assertEqual(
      {
        "redhat8,redhat9,openeuler22",
        "ubuntu22",
      },
      os_families,
    )

  def test_obsolete_migration_and_package_upgrade_entries_are_gone(self):
    metadata = (SERVICE / "metainfo.xml").read_text(encoding="utf-8")
    for command in (
      "BACKUP",
      "RESTORE",
      "MIGRATE",
      "UPGRADE_SOLR_INSTANCE",
      "UPGRADE_SOLR_CLIENT",
    ):
      self.assertNotIn(f"<name>{command}</name>", metadata)
    for obsolete_file in ("collection.py", "command_commons.py", "migrate.py"):
      self.assertFalse((SCRIPTS / obsolete_file).exists())
    self.assertFalse(
      (SERVICE / "package/templates/input.config-ambari-infra.json.j2").exists()
    )

  def test_security_template_is_bigtop_only_and_has_no_anonymous_admin_read(self):
    security_template = (
      SERVICE / "package/templates/infra-solr-security.json.j2"
    ).read_text(encoding="utf-8")
    self.assertNotIn("at" + "las", security_template.lower())
    self.assertNotIn("log" + "search", security_template.lower())
    self.assertNotIn('"role" :null', security_template)


class TestInfraSolrAdvisor(unittest.TestCase):
  def setUp(self):
    self.validator = object.__new__(ADVISOR.AmbariInfraSolrValidator)

  def environment(self, **overrides):
    properties = {
      "infra_solr_port": "8886",
      "infra_solr_jmx_port": "18886",
      "infra_solr_minmem": "1024",
      "infra_solr_maxmem": "2048",
      "infra_solr_java_stack_size": "1",
      "infra_solr_user_nofile_limit": "128000",
      "infra_solr_user_nproc_limit": "65536",
      "infra_solr_datadir": "/var/lib/ambari-infra-solr/data",
      "infra_solr_log_dir": "/var/log/ambari-infra-solr",
      "infra_solr_pid_dir": "/var/run/ambari-infra-solr",
      "infra_solr_znode": "/infra-solr",
      "infra_solr_user": "infra-solr",
      "infra_solr_extra_java_opts": "-Dcustom.option=true",
      "infra_solr_ssl_enabled": "false",
      "infra_solr_jmx_enabled": "false",
      "infra_solr_zookeeper_external_enabled": "false",
      "infra_solr_keystore_location": "/etc/security/serverKeys/solr.jks",
      "infra_solr_truststore_location": "/etc/security/serverKeys/trust.jks",
    }
    properties.update(overrides)
    return properties

  def test_environment_accepts_current_contract(self):
    self.assertEqual(
      [],
      self.validator.validate_environment(
        self.environment(), {}, {}, {}, {}
      ),
    )

  def test_environment_rejects_root_znode_and_managed_java_override(self):
    problems = self.validator.validate_environment(
      self.environment(
        infra_solr_znode="/",
        infra_solr_extra_java_opts="-Djetty.port=9999",
      ),
      {},
      {},
      {},
      {},
    )
    self.assertEqual(
      {"infra_solr_znode", "infra_solr_extra_java_opts"},
      {problem["config-name"] for problem in problems},
    )

  def test_security_rejects_invalid_roles_and_duplicate_audit_users(self):
    problems = self.validator.validate_security(
      {
        "infra_solr_role_ranger_admin": "bad role",
        "infra_solr_role_ranger_audit": "ranger_audit_user",
        "infra_solr_role_dev": "dev",
        "infra_solr_ranger_audit_service_users": "hive,hive",
        "infra_solr_security_manually_managed": "false",
      },
      {},
      {},
      {},
      {},
    )
    self.assertEqual(
      {"infra_solr_role_dev", "infra_solr_ranger_audit_service_users"},
      {problem["config-name"] for problem in problems},
    )


if __name__ == "__main__":
  unittest.main()
