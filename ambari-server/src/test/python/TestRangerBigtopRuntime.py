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
import grp
import os
from pathlib import Path
import pwd
import stat
import sys
import tempfile
from types import SimpleNamespace
import unittest
from unittest.mock import MagicMock, patch
from xml.etree import ElementTree

from resource_management.core.exceptions import Fail


SERVICES = (
  Path(__file__).resolve().parents[2]
  / "main/resources/stacks/BIGTOP/3.3.0/services"
)
RANGER = SERVICES / "RANGER"


def load_module(name, path):
  spec = importlib.util.spec_from_file_location(name, path)
  module = importlib.util.module_from_spec(spec)
  spec.loader.exec_module(module)
  return module


class TestRangerBigtopRuntime(unittest.TestCase):
  def test_synchronous_execute_calls_are_bounded_and_kill_process_groups(self):
    failures = []
    for path in sorted((RANGER / "package").rglob("*.py")):
      tree = ast.parse(path.read_text(), filename=str(path))
      for node in ast.walk(tree):
        if not (
          isinstance(node, ast.Call)
          and isinstance(node.func, ast.Name)
          and node.func.id == "Execute"
        ):
          continue
        keywords = {keyword.arg: keyword.value for keyword in node.keywords}
        wait_for_finish = keywords.get("wait_for_finish")
        if (
          isinstance(wait_for_finish, ast.Constant)
          and wait_for_finish.value is False
        ):
          continue
        strategy = keywords.get("timeout_kill_strategy")
        if "timeout" not in keywords or not (
          isinstance(strategy, ast.Attribute)
          and strategy.attr == "KILL_PROCESS_GROUP"
        ):
          failures.append(f"{path.relative_to(RANGER)}:{node.lineno}")

    self.assertEqual([], failures)

  def test_runtime_boolean_contract_is_strict(self):
    module = load_module(
      "ranger_utils_test", RANGER / "package/scripts/ranger_utils.py"
    )
    self.assertTrue(module.strict_bool(" true ", "value"))
    self.assertFalse(module.strict_bool("FALSE", "value"))
    for invalid in ("yes", "1", "", None, 1):
      with self.assertRaises(Fail):
        module.strict_bool(invalid, "value")

    self.assertTrue(module.strict_yes_no(" Yes ", "value"))
    self.assertFalse(module.strict_yes_no("NO", "value"))
    for invalid in ("true", "1", "", None, 1):
      with self.assertRaises(Fail):
        module.strict_yes_no(invalid, "value")

  def test_tagsync_atlas_password_must_be_explicit_and_nonempty(self):
    module = load_module(
      "ranger_utils_secret_test", RANGER / "package/scripts/ranger_utils.py"
    )
    self.assertEqual(
      "configured-secret",
      module.require_nonempty_secret(
        "configured-secret", "atlas-env/atlas.admin.password"
      ),
    )
    for invalid in (None, "", "   ", 1):
      with self.subTest(invalid=invalid), self.assertRaisesRegex(
        Fail, "atlas-env/atlas.admin.password"
      ):
        module.require_nonempty_secret(
          invalid, "atlas-env/atlas.admin.password"
        )

    params_source = (RANGER / "package/scripts/params.py").read_text(
      encoding="utf-8"
    )
    self.assertNotIn('atlas.admin.password", "admin"', params_source)
    self.assertIn("is_ranger_tagsync_host", params_source)

  def test_private_secret_file_is_private_and_removed_on_failure(self):
    module = load_module(
      "ranger_secret_test", RANGER / "package/scripts/ranger_utils.py"
    )
    owner = pwd.getpwuid(os.getuid()).pw_name
    group = grp.getgrgid(os.getgid()).gr_name
    with tempfile.TemporaryDirectory() as directory:
      secret_path = None
      with self.assertRaises(RuntimeError):
        with module.private_secret_file(directory, owner, group, "test-value") as path:
          secret_path = path
          path_stat = os.stat(path, follow_symlinks=False)
          self.assertTrue(stat.S_ISREG(path_stat.st_mode))
          self.assertEqual(0o600, stat.S_IMODE(path_stat.st_mode))
          raise RuntimeError("exercise cleanup")
      self.assertIsNotNone(secret_path)
      self.assertFalse(os.path.lexists(secret_path))

  def test_private_secret_file_rejects_unsafe_directory_and_oversize_payload(self):
    module = load_module(
      "ranger_secret_validation_test",
      RANGER / "package/scripts/ranger_utils.py",
    )
    owner = pwd.getpwuid(os.getuid()).pw_name
    group = grp.getgrgid(os.getgid()).gr_name
    with tempfile.TemporaryDirectory() as directory:
      os.chmod(directory, 0o777)
      with self.assertRaises(Fail):
        with module.private_secret_file(directory, owner, group, "test-value"):
          self.fail("unsafe directory was accepted")
      self.assertEqual([], os.listdir(directory))

      os.chmod(directory, 0o700)
      with self.assertRaises(Fail):
        with module.private_secret_file(directory, owner, group, "x" * 65537):
          self.fail("oversize secret was accepted")
      self.assertEqual([], os.listdir(directory))

  def test_private_secret_cleanup_does_not_replace_primary_error(self):
    module = load_module(
      "ranger_secret_primary_error_test",
      RANGER / "package/scripts/ranger_utils.py",
    )
    owner = pwd.getpwuid(os.getuid()).pw_name
    group = grp.getgrgid(os.getgid()).gr_name
    replacement_identity = SimpleNamespace(
      st_mode=stat.S_IFREG,
      st_dev=-1,
      st_ino=-1,
    )
    with tempfile.TemporaryDirectory() as directory, patch.object(
      module.Logger, "warning"
    ) as warning, patch.object(
      module.os, "lstat", return_value=replacement_identity
    ):
      with self.assertRaisesRegex(RuntimeError, "primary operation failed"):
        with module.private_secret_file(
          directory, owner, group, "test-value"
        ) as path:
          os.unlink(path)
          with open(path, "w", encoding="utf-8") as replacement:
            replacement.write("replacement")
          raise RuntimeError("primary operation failed")

    warning.assert_called_once()
    self.assertNotIn("test-value", warning.call_args.args[0])

  def test_credentials_do_not_cross_process_argv(self):
    setup_source = (RANGER / "package/scripts/setup_ranger_xml.py").read_text()
    self.assertIn('"-changepasswordfile"', setup_source)
    self.assertIn('"-storepass:file"', setup_source)
    self.assertIn('"-keypass:file"', setup_source)
    self.assertNotIn("-changepassword {", setup_source)
    self.assertNotIn("keystore_password!p", setup_source)
    self.assertNotIn("Unsupported special characters", setup_source)

  def test_service_check_verifies_tls_and_has_timeouts(self):
    source = (RANGER / "package/scripts/service_check.py").read_text()
    self.assertIn('"/usr/bin/curl"', source)
    self.assertIn('"--connect-timeout"', source)
    self.assertIn('"--max-time"', source)
    self.assertNotIn('"-k"', source)
    self.assertNotIn("| grep", source)

    alert = load_module(
      "ranger_alert_test",
      RANGER / "package/alerts/alert_ranger_admin_passwd_check.py",
    )
    with self.assertRaises(ValueError):
      alert._strict_bool("yes", "security_enabled")
    with self.assertRaises(ValueError):
      alert._validate_ranger_url("file:///etc/passwd")

  def test_secure_service_check_uses_private_cache_and_propagates_failure(self):
    module = load_module(
      "ranger_service_check_test", RANGER / "package/scripts/service_check.py"
    )
    params = SimpleNamespace(
      ranger_external_url="https://ranger.example.com:6182/",
      upgrade_marker_file="/nonexistent/ranger-upgrade-marker",
      security_enabled=True,
      unix_user="ranger",
      unix_group="ranger",
      tmp_dir="/var/run/ambari-agent/tmp",
      kinit_path_local="/usr/bin/kinit",
      ranger_admin_keytab="/etc/security/keytabs/rangeradmin.service.keytab",
      ranger_admin_jaas_principal="rangeradmin/ranger.example.com@EXAMPLE.COM",
    )
    cache = MagicMock()
    cache.environment = {"KRB5CCNAME": "FILE:/private/ranger/krb5cc"}
    cache.__enter__.return_value = cache
    cache.__exit__.return_value = False
    execute_error = Fail("Ranger Admin endpoint failed")

    with patch.dict(sys.modules, {"params": params}), \
      patch.object(module.os.path, "isfile", return_value=False), \
      patch.object(module, "PrivateKerberosCache", return_value=cache) as factory, \
      patch.object(module, "Execute", side_effect=execute_error) as execute, \
      self.assertRaisesRegex(Fail, "endpoint failed"):
      module.RangerServiceCheck().service_check(SimpleNamespace(set_params=MagicMock()))

    factory.assert_called_once_with(
      "ranger",
      "ranger",
      temp_dir="/var/run/ambari-agent/tmp",
      prefix="ambari-ranger-admin-check-",
    )
    cache.kinit.assert_called_once_with(
      "/usr/bin/kinit",
      "/etc/security/keytabs/rangeradmin.service.keytab",
      "rangeradmin/ranger.example.com@EXAMPLE.COM",
      timeout=30,
    )
    command = execute.call_args.args[0]
    self.assertIn("--negotiate", command)
    self.assertEqual("https://ranger.example.com:6182/login.jsp", command[-1])
    self.assertEqual(
      {"KRB5CCNAME": "FILE:/private/ranger/krb5cc"},
      execute.call_args.kwargs["environment"],
    )
    self.assertEqual("ranger", execute.call_args.kwargs["user"])
    self.assertIs(Fail, cache.__exit__.call_args.args[0])
    self.assertIs(execute_error, cache.__exit__.call_args.args[1])

  def test_secure_service_check_rejects_missing_credentials_before_request(self):
    module = load_module(
      "ranger_service_check_credentials_test",
      RANGER / "package/scripts/service_check.py",
    )
    params = SimpleNamespace(
      ranger_external_url="https://ranger.example.com:6182/",
      upgrade_marker_file="/nonexistent/ranger-upgrade-marker",
      security_enabled=True,
      unix_user="ranger",
      unix_group="ranger",
      tmp_dir="/var/run/ambari-agent/tmp",
      kinit_path_local="/usr/bin/kinit",
      ranger_admin_keytab="/etc/security/keytabs/rangeradmin.service.keytab",
    )

    with patch.object(module.os.path, "isfile", return_value=False), \
      patch.object(module, "PrivateKerberosCache") as factory, \
      patch.object(module, "Execute") as execute, \
      self.assertRaisesRegex(Fail, "service principal and keytab"):
      module.RangerServiceCheck().check_ranger_admin_service(params)

    factory.assert_not_called()
    execute.assert_not_called()

  def test_metadata_matches_bigtop_ranger_2_6_contract(self):
    root = ElementTree.parse(RANGER / "metainfo.xml").getroot()
    self.assertEqual("2.6.0-1", root.findtext("./services/service/version"))
    os_families = {
      item.text
      for item in root.findall("./services/service/osSpecifics/osSpecific/osFamily")
    }
    self.assertIn("ubuntu22", os_families)
    self.assertTrue(any("redhat8" in item for item in os_families))
    self.assertFalse(any("redhat7" in item for item in os_families))

    alerts = (RANGER / "alerts.json").read_text()
    self.assertIn("BIGTOP/3.3.0/services/RANGER/package/alerts", alerts)
    self.assertNotIn("BIGTOP/3.2.0/services/RANGER/package/alerts", alerts)

    override_root = ElementTree.parse(
      RANGER.parents[2] / "3.4.0/services/RANGER/metainfo.xml"
    ).getroot()
    self.assertEqual("2.6.0-1", override_root.findtext("./services/service/version"))

  def test_required_setup_failures_are_propagated(self):
    ranger_setup = (RANGER / "package/scripts/setup_ranger_xml.py").read_text()
    self.assertIn("Could not configure Solr for Ranger", ranger_setup)

  def test_runtime_configuration_directories_are_private(self):
    ranger_setup = (RANGER / "package/scripts/setup_ranger_xml.py").read_text()
    self.assertGreaterEqual(ranger_setup.count("mode=0o750"), 3)

if __name__ == "__main__":
  unittest.main()
