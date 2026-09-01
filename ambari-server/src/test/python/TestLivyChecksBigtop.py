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


LIVY = (
  Path(__file__).resolve().parents[2]
  / "main/resources/stacks/BIGTOP/3.2.0/services/LIVY"
)
SCRIPTS = LIVY / "package/scripts"


def load_module(module_name, path):
  spec = importlib.util.spec_from_file_location(module_name, path)
  module = importlib.util.module_from_spec(spec)
  spec.loader.exec_module(module)
  return module


def params_module(**values):
  module = ModuleType("params")
  for attribute, value in values.items():
    setattr(module, attribute, value)
  return module


SERVICE_CHECK = load_module(
  "bigtop_livy_service_check",
  SCRIPTS / "service_check.py",
)
ALERT = load_module(
  "bigtop_alert_livy_port", SCRIPTS / "alerts/alert_livy_port.py"
)


class TestLivyServiceCheckContract(unittest.TestCase):
  def _params(self, secure=False, hosts=None):
    return params_module(
      livy_livyserver_hosts=(
        hosts if hosts is not None else ["livy1.example.com"]
      ),
      security_enabled=secure,
      smoke_user_keytab="/etc/security/key tabs/smoke;$(id)",
      smokeuser_principal="smoke@REALM;$(id)",
      smoke_user="ambari-qa",
      user_group="hadoop",
      kinit_path_local="/usr/bin/kinit;$(id)",
      livy_http_scheme="https" if secure else "http",
      livy_livyserver_port=8999,
    )

  def test_url_validation_supports_ipv6_and_rejects_path_injection(self):
    self.assertEqual(
      "https://[2001:db8::1]:8999/sessions",
      SERVICE_CHECK.build_livy_url("https", "2001:db8::1", 8999),
    )
    with self.assertRaises(Fail):
      SERVICE_CHECK.build_livy_url("http", "host/path;$(id)", 8999)
    with self.assertRaises(Fail):
      SERVICE_CHECK.build_livy_url("ftp", "host", 8999)

  def test_insecure_check_uses_structured_curl_and_system_ca_policy(self):
    params = self._params()
    with patch.dict(sys.modules, {"params": params}), \
      patch.object(SERVICE_CHECK, "Execute") as execute:
      SERVICE_CHECK.LivyServiceCheck().service_check(MagicMock())

    command = execute.call_args.args[0]
    self.assertIsInstance(command, tuple)
    self.assertIn("--fail", command)
    self.assertIn("--disable", command)
    self.assertNotIn("-k", command)
    self.assertNotIn("--negotiate", command)
    self.assertEqual(
      "http://livy1.example.com:8999/sessions", command[-1]
    )
    self.assertEqual(20, execute.call_args.kwargs["timeout"])
    self.assertEqual(3, execute.call_args.kwargs["tries"])

  def test_secure_check_uses_unique_cache_and_separate_argv(self):
    params = self._params(secure=True)
    cache = MagicMock()
    cache.cache_name = "FILE:/tmp/livy-check/krb5cc"
    cache.environment = {"KRB5CCNAME": cache.cache_name}
    context = MagicMock()
    context.__enter__.return_value = cache
    with patch.dict(sys.modules, {"params": params}), \
      patch.object(
        SERVICE_CHECK, "PrivateKerberosCache", return_value=context
      ) as cache_factory, \
      patch.object(SERVICE_CHECK, "Execute") as execute:
      SERVICE_CHECK.LivyServiceCheck().service_check(MagicMock())

    cache_factory.assert_called_once_with(
      "ambari-qa", "hadoop", prefix="ambari-livy-service-check-"
    )
    cache.kinit.assert_called_once_with(
      "/usr/bin/kinit;$(id)",
      "/etc/security/key tabs/smoke;$(id)",
      "smoke@REALM;$(id)",
      timeout=30,
    )
    execute.assert_called_once()
    curl = execute.call_args
    self.assertIn("--negotiate", curl.args[0])
    self.assertNotIn("-k", curl.args[0])
    self.assertEqual(cache.environment, curl.kwargs["environment"])
    context.__exit__.assert_called_once()

  def test_check_continues_after_invalid_or_failed_host(self):
    params = self._params(hosts=["invalid/path", "livy2.example.com"])
    with patch.dict(sys.modules, {"params": params}), \
      patch.object(SERVICE_CHECK, "Execute") as execute:
      SERVICE_CHECK.LivyServiceCheck().service_check(MagicMock())

    execute.assert_called_once()
    self.assertEqual(
      "http://livy2.example.com:8999/sessions",
      execute.call_args.args[0][-1],
    )

  def test_missing_hosts_and_secure_credentials_fail_fast(self):
    for params in (
      self._params(hosts=[]),
      self._params(secure=True),
    ):
      if params.security_enabled:
        params.smoke_user_keytab = None
      with patch.dict(sys.modules, {"params": params}):
        with self.assertRaises(Fail):
          SERVICE_CHECK.LivyServiceCheck().service_check(MagicMock())

  def test_all_failed_hosts_raise_with_each_failure(self):
    params = self._params(hosts=["livy1.example.com", "livy2.example.com"])
    with patch.dict(sys.modules, {"params": params}), \
      patch.object(
        SERVICE_CHECK, "Execute", side_effect=Fail("HTTP 503")
      ) as execute:
      with self.assertRaisesRegex(
        Fail, "livy1.example.com.*livy2.example.com"
      ):
        SERVICE_CHECK.LivyServiceCheck().service_check(MagicMock())

    self.assertEqual(2, execute.call_count)


class TestLivyAlertContract(unittest.TestCase):
  def _config(self, secure=False):
    return {
      ALERT.LIVY_SERVER_HOST_KEY: "livy.example.com",
      ALERT.LIVY_SERVER_PORT_KEY: "8999",
      ALERT.SECURITY_ENABLED_KEY: str(secure).lower(),
      ALERT.SMOKEUSER_KEY: "ambari-qa",
      ALERT.USER_GROUP_KEY: "hadoop",
      ALERT.SMOKEUSER_KEYTAB_KEY: "/etc/security/key tabs/smoke;$(id)",
      ALERT.SMOKEUSER_PRINCIPAL_KEY: "smoke/_HOST@REALM;$(id)",
      ALERT.LIVY_KEYSTORE_KEY: "/etc/livy/livy.jks" if secure else "",
      ALERT.KERBEROS_EXECUTABLE_SEARCH_PATHS_KEY: "/usr/bin",
    }

  def test_tokens_include_group_and_kerberos_search_path(self):
    tokens = ALERT.get_tokens()
    self.assertIn(ALERT.USER_GROUP_KEY, tokens)
    self.assertIn(ALERT.KERBEROS_EXECUTABLE_SEARCH_PATHS_KEY, tokens)

  def test_alerts_are_owned_by_livy_service(self):
    alert_definitions = json.loads(
      (LIVY / "alerts.json").read_text(encoding="utf-8")
    )
    self.assertEqual(["LIVY"], list(alert_definitions))
    self.assertIn("LIVY_SERVER", alert_definitions["LIVY"])

  def test_insecure_alert_uses_structured_verified_http_request(self):
    with patch.object(ALERT, "Execute") as execute, \
      patch.object(ALERT.time, "monotonic", side_effect=(10.0, 10.25)):
      result = ALERT.execute(self._config(), {"check.command.timeout": 12})

    self.assertEqual("OK", result[0])
    command = execute.call_args.args[0]
    self.assertIsInstance(command, tuple)
    self.assertNotIn("-k", command)
    self.assertNotIn("--negotiate", command)
    self.assertEqual("http://livy.example.com:8999/sessions", command[-1])
    self.assertEqual(17.0, execute.call_args.kwargs["timeout"])

  def test_secure_alert_uses_private_cache_and_preserves_argv_boundaries(self):
    cache = MagicMock()
    cache.cache_name = "FILE:/tmp/livy-alert/krb5cc"
    cache.environment = {"KRB5CCNAME": cache.cache_name}
    context = MagicMock()
    context.__enter__.return_value = cache
    with patch.object(ALERT, "PrivateKerberosCache", return_value=context), \
      patch.object(ALERT, "get_kinit_path", return_value="/usr/bin/kinit;$(id)"), \
      patch.object(ALERT.socket, "getfqdn", return_value="agent.example.com"), \
      patch.object(ALERT, "Execute") as execute:
      result = ALERT.execute(
        self._config(secure=True), {"check.command.timeout": 10}
      )

    self.assertEqual("OK", result[0])
    cache.kinit.assert_called_once_with(
      "/usr/bin/kinit;$(id)",
      "/etc/security/key tabs/smoke;$(id)",
      "smoke/agent.example.com@REALM;$(id)",
      timeout=30,
    )
    execute.assert_called_once()
    curl = execute.call_args
    self.assertIn("--negotiate", curl.args[0])
    self.assertNotIn("-k", curl.args[0])
    self.assertEqual(cache.environment, curl.kwargs["environment"])

  def test_invalid_timeout_and_missing_secure_credentials_are_unknown(self):
    invalid_timeout = ALERT.execute(
      self._config(), {"check.command.timeout": 0}
    )
    secure_config = self._config(secure=True)
    secure_config.pop(ALERT.SMOKEUSER_KEYTAB_KEY)
    missing_credentials = ALERT.execute(secure_config, {})
    self.assertEqual("UNKNOWN", invalid_timeout[0])
    self.assertEqual("UNKNOWN", missing_credentials[0])

  def test_request_failure_is_critical(self):
    with patch.object(ALERT, "Execute", side_effect=Fail("HTTP 503")):
      result = ALERT.execute(self._config(), {})

    self.assertEqual("CRITICAL", result[0])
    self.assertIn("HTTP 503", result[1][0])


if __name__ == "__main__":
  unittest.main()
