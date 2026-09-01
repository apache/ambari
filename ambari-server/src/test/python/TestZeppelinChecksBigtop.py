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
import json
from pathlib import Path
import sys
from types import ModuleType
import unittest
from unittest.mock import MagicMock, patch

from resource_management.core.exceptions import Fail


ZEPPELIN = (
  Path(__file__).resolve().parents[2]
  / "main/resources/stacks/BIGTOP/3.2.0/services/ZEPPELIN"
)
SCRIPTS = ZEPPELIN / "package/scripts"


def load_module(name, path):
  spec = importlib.util.spec_from_file_location(name, path)
  module = importlib.util.module_from_spec(spec)
  spec.loader.exec_module(module)
  return module


def params_module(**values):
  module = ModuleType("params")
  for name, value in values.items():
    setattr(module, name, value)
  return module


SERVICE_CHECK = load_module("bigtop_zeppelin_check", SCRIPTS / "service_check.py")
ALERT = load_module(
  "bigtop_zeppelin_alert", SCRIPTS / "alerts/alert_check_zeppelin.py"
)


class TestZeppelinServiceCheck(unittest.TestCase):
  def _params(self, secure=False):
    return params_module(
      is_ui_ssl_enabled=secure,
      zeppelin_host="zeppelin.example.com",
      zeppelin_port=9995 if secure else 9996,
      security_enabled=secure,
      smoke_user="ambari-qa",
      user_group="hadoop",
      kinit_path_local="/usr/bin/kinit;$(id)",
      smoke_user_keytab="/etc/security/key tabs/smoke;$(id)",
      smokeuser_principal="smoke@REALM;$(id)",
    )

  def test_url_supports_ipv6_and_rejects_injection(self):
    self.assertEqual(
      "https://[2001:db8::1]:9995/api/version",
      SERVICE_CHECK.build_zeppelin_url(True, "2001:db8::1", 9995),
    )
    with self.assertRaises(Fail):
      SERVICE_CHECK.build_zeppelin_url(False, "host/path;$(id)", 9995)
    with self.assertRaises(Fail):
      SERVICE_CHECK.build_zeppelin_url(False, "host", 70000)

  def test_insecure_check_uses_structured_verified_request(self):
    params = self._params()
    with patch.dict(sys.modules, {"params": params}), \
      patch.object(SERVICE_CHECK, "Execute") as execute:
      SERVICE_CHECK.ZeppelinServiceCheck().service_check(MagicMock())

    command = execute.call_args.args[0]
    self.assertIsInstance(command, tuple)
    self.assertIn("--fail", command)
    self.assertNotIn("-k", command)
    self.assertNotIn("--negotiate", command)
    self.assertEqual(
      "http://zeppelin.example.com:9996/api/version", command[-1]
    )
    self.assertEqual(20, execute.call_args.kwargs["timeout"])
    self.assertEqual(3, execute.call_args.kwargs["tries"])

  def test_secure_check_uses_private_cache_and_preserves_argv(self):
    params = self._params(secure=True)
    cache = MagicMock()
    cache.environment = {"KRB5CCNAME": "FILE:/tmp/zeppelin-check/krb5cc"}
    context = MagicMock()
    context.__enter__.return_value = cache
    with patch.dict(sys.modules, {"params": params}), \
      patch.object(
        SERVICE_CHECK, "PrivateKerberosCache", return_value=context
      ) as cache_factory, \
      patch.object(SERVICE_CHECK, "Execute") as execute:
      SERVICE_CHECK.ZeppelinServiceCheck().service_check(MagicMock())

    cache_factory.assert_called_once_with(
      "ambari-qa", "hadoop", prefix="ambari-zeppelin-service-check-"
    )
    cache.kinit.assert_called_once_with(
      "/usr/bin/kinit;$(id)",
      "/etc/security/key tabs/smoke;$(id)",
      "smoke@REALM;$(id)",
      timeout=30,
    )
    command = execute.call_args.args[0]
    self.assertIn("--negotiate", command)
    self.assertNotIn("-k", command)
    self.assertEqual(cache.environment, execute.call_args.kwargs["environment"])

  def test_secure_check_without_credentials_fails_closed(self):
    params = self._params(secure=True)
    params.smoke_user_keytab = None
    with patch.dict(sys.modules, {"params": params}):
      with self.assertRaisesRegex(Fail, "principal and keytab"):
        SERVICE_CHECK.ZeppelinServiceCheck().service_check(MagicMock())


class TestZeppelinAlert(unittest.TestCase):
  def _config(self, secure=False):
    return {
      ALERT.ZEPPELIN_USER_KEY: "zeppelin",
      ALERT.ZEPPELIN_GROUP_KEY: "zeppelin",
      ALERT.UI_SSL_ENABLED_KEY: str(secure).lower(),
      ALERT.SECURITY_ENABLED_KEY: str(secure).lower(),
      ALERT.ZEPPELIN_PORT_KEY: "9996",
      ALERT.ZEPPELIN_PORT_SSL_KEY: "9995",
      ALERT.ZEPPELIN_KEYTAB_KEY: "/etc/security/key tabs/zeppelin;$(id)",
      ALERT.ZEPPELIN_PRINCIPAL_KEY: "zeppelin/_HOST@REALM;$(id)",
      ALERT.KERBEROS_EXECUTABLE_SEARCH_PATHS_KEY: "/usr/bin",
    }

  def test_alert_definition_has_bounded_timeout(self):
    alert = json.loads((ZEPPELIN / "alerts.json").read_text(encoding="utf-8"))
    parameters = alert["ZEPPELIN"]["ZEPPELIN_SERVER"][0]["source"]["parameters"]
    self.assertEqual("check.command.timeout", parameters[0]["name"])

  def test_insecure_alert_uses_structured_verified_request(self):
    with patch.object(ALERT, "Execute") as execute, \
      patch.object(ALERT.time, "monotonic", side_effect=(10.0, 10.25)):
      result = ALERT.execute(
        self._config(), {"check.command.timeout": 12}, "zeppelin.example.com"
      )

    self.assertEqual("OK", result[0])
    command = execute.call_args.args[0]
    self.assertIsInstance(command, tuple)
    self.assertNotIn("-k", command)
    self.assertNotIn("--negotiate", command)
    self.assertEqual(
      "http://zeppelin.example.com:9996/api/version", command[-1]
    )

  def test_secure_alert_uses_private_cache(self):
    cache = MagicMock()
    cache.environment = {"KRB5CCNAME": "FILE:/tmp/zeppelin-alert/krb5cc"}
    context = MagicMock()
    context.__enter__.return_value = cache
    with patch.object(ALERT, "PrivateKerberosCache", return_value=context), \
      patch.object(ALERT, "get_kinit_path", return_value="/usr/bin/kinit;$(id)"), \
      patch.object(ALERT, "Execute") as execute:
      result = ALERT.execute(
        self._config(secure=True), {}, "zeppelin.example.com"
      )

    self.assertEqual("OK", result[0])
    cache.kinit.assert_called_once_with(
      "/usr/bin/kinit;$(id)",
      "/etc/security/key tabs/zeppelin;$(id)",
      "zeppelin/zeppelin.example.com@REALM;$(id)",
      timeout=30,
    )
    self.assertIn("--negotiate", execute.call_args.args[0])
    self.assertEqual(cache.environment, execute.call_args.kwargs["environment"])

  def test_invalid_configuration_is_unknown_and_request_failure_is_critical(self):
    invalid = self._config()
    invalid[ALERT.UI_SSL_ENABLED_KEY] = "maybe"
    self.assertEqual("UNKNOWN", ALERT.execute(invalid, {}, "host")[0])

    with patch.object(ALERT, "Execute", side_effect=Fail("HTTP 503")):
      result = ALERT.execute(self._config(), {}, "host")
    self.assertEqual("CRITICAL", result[0])
    self.assertIn("HTTP 503", result[1][0])


if __name__ == "__main__":
  unittest.main()
