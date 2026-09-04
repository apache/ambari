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

from unittest import TestCase
from unittest.mock import call, patch

from resource_management.core.exceptions import Fail
from resource_management.libraries.functions import setup_ranger_plugin_xml


class TestSetupRangerPluginXml(TestCase):
  def test_external_ranger_credentials_must_be_explicit_and_nonempty(self):
    credentials = {
      "external_admin_username": "external-admin",
      "external_admin_password": "secret-one",
      "external_ranger_admin_username": "service-account",
      "external_ranger_admin_password": "secret-two",
    }
    self.assertEqual(
      credentials,
      setup_ranger_plugin_xml.require_external_ranger_credentials(credentials),
    )
    for property_name in credentials:
      for invalid_value in (None, "", "   ", 1):
        with self.subTest(
          property_name=property_name, invalid_value=invalid_value
        ):
          invalid = dict(credentials)
          invalid[property_name] = invalid_value
          with self.assertRaisesRegex(Fail, property_name):
            setup_ranger_plugin_xml.require_external_ranger_credentials(invalid)

    for invalid_properties in (None, (), []):
      with self.subTest(invalid_properties=invalid_properties):
        with self.assertRaisesRegex(Fail, "must be a mapping"):
          setup_ranger_plugin_xml.require_external_ranger_credentials(
            invalid_properties
          )

  def test_ranger_repository_and_connector_paths_fail_closed(self):
    self.assertEqual(
      "service-1",
      setup_ranger_plugin_xml._require_safe_segment(
        "service-1", "Ranger repository name"
      ),
    )
    for value in ("../service", "/service", "service name", "service;run", ""):
      with self.subTest(segment=value):
        with self.assertRaises(Fail):
          setup_ranger_plugin_xml._require_safe_segment(
            value, "Ranger repository name"
          )

    self.assertEqual(
      "/tmp/driver.jar",
      setup_ranger_plugin_xml._require_safe_jar_path(
        "/tmp/driver.jar", "Ranger JDBC connector"
      ),
    )
    for path in (
      "relative.jar",
      "/tmp/../etc/driver.jar",
      "/tmp/driver;run.jar",
      "/tmp/driver.zip",
    ):
      with self.subTest(path=path):
        with self.assertRaises(Fail):
          setup_ranger_plugin_xml._require_safe_jar_path(
            path, "Ranger JDBC connector"
          )

  def test_keystore_uses_ambari_java_and_stdin_credential_helper(self):
    with patch.object(
      setup_ranger_plugin_xml, "create_password_in_credential_store"
    ) as create_mock, patch.object(setup_ranger_plugin_xml, "File") as file_mock:
      setup_ranger_plugin_xml.setup_ranger_plugin_keystore(
        True,
        "/etc/ranger/service/cred.jceks",
        "audit-secret",
        "trust-secret",
        "key-secret",
        "service-user",
        "service-group",
        "/ambari/java",
        cred_lib_path_override="/plugin/install/lib/*",
      )

    self.assertEqual(
      [
        call(
          "auditDBCred",
          "jceks://file/etc/ranger/service/cred.jceks",
          "/plugin/install/lib/*",
          "/ambari/java",
          None,
          "audit-secret",
        ),
        call(
          "sslKeyStore",
          "jceks://file/etc/ranger/service/cred.jceks",
          "/plugin/install/lib/*",
          "/ambari/java",
          None,
          "key-secret",
        ),
        call(
          "sslTrustStore",
          "jceks://file/etc/ranger/service/cred.jceks",
          "/plugin/install/lib/*",
          "/ambari/java",
          None,
          "trust-secret",
        ),
      ],
      create_mock.call_args_list,
    )
    self.assertEqual(2, file_mock.call_count)

  def test_keystore_failure_does_not_apply_file_permissions(self):
    with patch.object(
      setup_ranger_plugin_xml,
      "create_password_in_credential_store",
      side_effect=[None, Fail("write failed")],
    ) as create_mock, patch.object(setup_ranger_plugin_xml, "File") as file_mock:
      with self.assertRaisesRegex(Fail, "write failed"):
        setup_ranger_plugin_xml.setup_ranger_plugin_keystore(
          False,
          "/etc/ranger/service/cred.jceks",
          "unused-audit-secret",
          "trust-secret",
          "key-secret",
          "service-user",
          "service-group",
          "/ambari/java",
          plugin_home="/plugin",
        )

    self.assertEqual(2, create_mock.call_count)
    file_mock.assert_not_called()
