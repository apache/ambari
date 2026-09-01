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

import importlib.util
from pathlib import Path
from types import SimpleNamespace
import unittest
from unittest.mock import patch
from xml.etree import ElementTree

from resource_management.core.exceptions import Fail


KMS = (
  Path(__file__).resolve().parents[2]
  / "main/resources/stacks/BIGTOP/3.3.0/services/RANGER_KMS"
)


def load_module(name, path):
  spec = importlib.util.spec_from_file_location(name, path)
  module = importlib.util.module_from_spec(spec)
  spec.loader.exec_module(module)
  return module


class TestRangerKmsBigtopRuntime(unittest.TestCase):
  def test_runtime_boolean_contract_is_strict(self):
    module = load_module("kms_utils_test", KMS / "package/scripts/kms_utils.py")
    self.assertTrue(module.strict_bool(" true ", "value"))
    self.assertFalse(module.strict_bool("FALSE", "value"))
    for invalid in ("yes", "1", "", None, 1):
      with self.assertRaises(Fail):
        module.strict_bool(invalid, "value")

  def test_ranger_service_url_is_validated_and_query_encoded(self):
    module = load_module("kms_url_test", KMS / "package/scripts/kms_utils.py")
    self.assertEqual(
      "https://ranger.example.com:6182/service/public/v2/api/service?"
      "serviceName=kms%26isEnabled%3Dfalse&serviceType=kms&isEnabled=true",
      module.ranger_service_api_url(
        "https://ranger.example.com:6182/",
        serviceName="kms&isEnabled=false",
        serviceType="kms",
        isEnabled="true",
      ),
    )
    for invalid_url in (
      "file:///etc/passwd",
      "https://user:password@ranger.example.com",
      "https://ranger.example.com/path?override=true",
    ):
      with self.subTest(invalid_url=invalid_url):
        with self.assertRaises(Fail):
          module.ranger_service_api_url(invalid_url)

  def test_lifecycle_uses_exact_process_identity(self):
    process_source = (KMS / "package/scripts/kms_process.py").read_text()
    self.assertIn("-Dproc_rangerkms", process_source)
    self.assertIn("safe_process.terminate_process", process_source)
    self.assertIn("safe_process.secure_pid_file_for_identity", process_source)
    self.assertIn("PID directory ownership or permissions are unsafe", process_source)

    lifecycle_sources = "\n".join(
      path.read_text() for path in (KMS / "package/scripts").glob("*.py")
    )
    for obsolete in ("ps -ef", "grep proc_ranger", "check_process_status"):
      self.assertNotIn(obsolete, lifecycle_sources)
    self.assertIn("rollback_started_process", lifecycle_sources)

  def test_lifecycle_reuses_only_an_exact_pid_identity(self):
    module = load_module(
      "kms_process_existing_test", KMS / "package/scripts/kms_process.py"
    )
    identity = SimpleNamespace(pid=4217)
    with patch.object(module, "_validate_pid_file"), \
      patch.object(module, "_validate_pid_directory"), \
      patch.object(
        module.safe_process,
        "read_running_process",
        return_value=identity,
      ) as read_running_process, \
      patch.object(module.safe_process, "discover_running_process") as discover:
      self.assertIs(
        identity,
        module.find_process(
          "/run/ranger_kms/rangerkms.pid", "kms", "kms"
        ),
      )

    read_running_process.assert_called_once_with(
      "/run/ranger_kms/rangerkms.pid",
      "kms",
      ("-Dproc_rangerkms",),
    )
    discover.assert_not_called()

  def test_lifecycle_rejects_a_launcher_pid_mismatch(self):
    module = load_module(
      "kms_process_launcher_test", KMS / "package/scripts/kms_process.py"
    )
    identity = SimpleNamespace(pid=4217)
    with patch.object(module, "_validate_pid_file"), \
      patch.object(module, "_validate_pid_directory"), \
      patch.object(
        module.safe_process,
        "wait_for_discovered_process",
        return_value=identity,
      ), \
      patch.object(module.safe_process, "read_pid", return_value=4218), \
      patch.object(module.safe_process, "secure_pid_file_for_identity") as secure, \
      self.assertRaisesRegex(Fail, "unexpected PID"):
      module.secure_started_process(
        "/run/ranger_kms/rangerkms.pid", "kms", "kms"
      )
    secure.assert_not_called()

  def test_lifecycle_stops_and_removes_only_the_matching_pid(self):
    module = load_module(
      "kms_process_stop_test", KMS / "package/scripts/kms_process.py"
    )
    identity = SimpleNamespace(pid=4217)
    with patch.object(module, "find_process", return_value=identity), \
      patch.object(module.safe_process, "terminate_process") as terminate, \
      patch.object(module.safe_process, "read_pid", return_value=4217), \
      patch.object(module.safe_process, "remove_pid_file_if_stopped") as remove:
      module.stop_process(
        "/run/ranger_kms/rangerkms.pid", "kms", "kms"
      )

    terminate.assert_called_once_with(
      identity,
      "kms",
      ("-Dproc_rangerkms",),
    )
    remove.assert_called_once_with(
      "/run/ranger_kms/rangerkms.pid",
      4217,
      "kms",
      ("-Dproc_rangerkms",),
    )

  def test_metadata_matches_bigtop_ranger_2_6_contract(self):
    root = ElementTree.parse(KMS / "metainfo.xml").getroot()
    self.assertEqual("2.6.0-1", root.findtext("./services/service/version"))
    os_families = {
      item.text
      for item in root.findall("./services/service/osSpecifics/osSpecific/osFamily")
    }
    self.assertIn("ubuntu22", os_families)
    self.assertTrue(any("redhat8" in item for item in os_families))
    self.assertFalse(any("redhat7" in item for item in os_families))

  def test_required_setup_failures_are_propagated(self):
    source = (KMS / "package/scripts/kms.py").read_text()
    self.assertIn("Could not create Ranger KMS HDFS audit directories", source)
    self.assertIn("Could not get or create the Ranger KMS policy service", source)
    self.assertNotIn("/etc/init.d", source)

  def test_java_patch_keeps_database_and_package_ownership_boundaries(self):
    source = (KMS / "package/scripts/kms.py").read_text()
    java_patch = source[
      source.index("def setup_java_patch") : source.index("def do_keystore_setup")
    ]
    self.assertIn("user=params.kms_user", java_patch)
    self.assertIn('owner="root", group="root"', java_patch)

  def test_runtime_configuration_directories_are_private(self):
    source = (KMS / "package/scripts/kms.py").read_text()
    self.assertGreaterEqual(source.count("mode=0o750"), 3)

  def test_advisor_has_no_distribution_specific_legacy_names(self):
    advisor = (KMS / "service_advisor.py").read_text()
    for obsolete in ("HDP", "SPARK2", "livy2-env", "livy2_user", "OOZIE", "oozie-env"):
      with self.subTest(obsolete=obsolete):
        self.assertNotIn(obsolete, advisor)
    for current_mapping in (
      '"service": "YARN",\n        "config-type": "yarn-env"',
      '"service": "HIVE",\n        "config-type": "hive-env"',
      '"service": "LIVY",\n        "config-type": "livy-env"',
      '"service": "RANGER",\n        "config-type": "ranger-env"',
    ):
      with self.subTest(current_mapping=current_mapping):
        self.assertIn(current_mapping, advisor)

  def test_advisor_rejects_invalid_runtime_contract_values(self):
    module = load_module("kms_advisor_test", KMS / "service_advisor.py")
    self.assertTrue(module._valid_port("9292"))
    for invalid_port in (None, "", "0", "65536", "9292;shutdown"):
      with self.subTest(invalid_port=invalid_port):
        self.assertFalse(module._valid_port(invalid_port))
    self.assertTrue(module._valid_identity("kms"))
    for invalid_identity in (None, "", "root", "KMS User", "kms;id"):
      with self.subTest(invalid_identity=invalid_identity):
        self.assertFalse(module._valid_identity(invalid_identity))
    self.assertTrue(module._safe_pid_directory("/var/run/ranger_kms"))
    for invalid_directory in (
      "/tmp/ranger_kms",
      "/var/run/../tmp/ranger_kms",
      "var/run/ranger_kms",
      "/",
    ):
      with self.subTest(invalid_directory=invalid_directory):
        self.assertFalse(module._safe_pid_directory(invalid_directory))

  def test_missing_jdbc_driver_fails_before_download(self):
    source = (KMS / "package/scripts/kms.py").read_text()
    connector = source[
      source.index("def copy_jdbc_connector") : source.index("def enable_kms_plugin")
    ]
    self.assertLess(
      connector.index("JDBC driver cannot be downloaded"),
      connector.index("DownloadSource(params.driver_curl_source)"),
    )
    self.assertNotIn("Logger.error(error_message)", connector)


if __name__ == "__main__":
  unittest.main()
