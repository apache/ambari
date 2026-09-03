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
import sys
from types import ModuleType, SimpleNamespace
import unittest
from unittest.mock import MagicMock, patch

from resource_management.core.exceptions import Fail


SPARK = Path(__file__).resolve().parents[2] / "main/resources/stacks/BIGTOP/3.2.0/services/SPARK"
SCRIPTS = SPARK / "package/scripts"
ALERTS = SCRIPTS / "alerts"


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


SPARK_UTILS = load_module("bigtop_spark_utils_checks", SCRIPTS / "spark_utils.py")
SPARK_CHECK = load_module(
  "bigtop_spark_check",
  SCRIPTS / "service_check.py",
  {"spark_utils": SPARK_UTILS},
)
SPARK_ALERT = load_module(
  "bigtop_spark_thrift_alert",
  ALERTS / "alert_spark_thrift_port.py",
)


class TestSparkChecks(unittest.TestCase):
  def test_secure_check_uses_one_private_cache_for_history_and_thrift(self):
    params = params_module(
      security_enabled=True,
      kinit_path_local="/usr/bin/kinit",
      smoke_user_keytab="/etc/security/keytabs/smoke.keytab",
      smokeuser_principal="ambari-qa@EXAMPLE.COM",
      smoke_user="ambari-qa",
      user_group="hadoop",
      tmp_dir="/var/lib/ambari-agent/tmp",
      spark_beeline="/usr/bigtop/current/spark-client/bin/beeline",
      java_home="/usr/lib/jvm/java-17",
      spark_conf_dir="/etc/spark/conf",
      spark_history_scheme="https",
      spark_history_server_host="history.example.com",
      spark_history_ui_port=18081,
      has_history_server=True,
      has_spark_thriftserver=True,
      spark_thriftserver_hosts=["thrift.example.com"],
      spark_thrift_port=10016,
      default_hive_kerberos_principal="spark/_HOST@EXAMPLE.COM",
      spark_transport_mode="binary",
      spark_thrift_endpoint="cliservice",
      spark_thrift_ssl_enabled=False,
    )
    cache = MagicMock()
    cache.merge_environment.return_value = {"KRB5CCNAME": "FILE:/private/krb5cc"}
    context = MagicMock()
    context.__enter__.return_value = cache
    context.__exit__.return_value = False
    with patch.dict(sys.modules, {"params": params}), \
      patch.object(SPARK_UTILS, "validate_executable"), \
      patch.object(SPARK_UTILS, "validate_keytab"), \
      patch.object(
        SPARK_CHECK, "PrivateKerberosCache", return_value=context
      ) as factory, \
      patch.object(SPARK_CHECK, "Execute") as execute:
      script = SimpleNamespace(set_params=MagicMock())
      SPARK_CHECK.SparkServiceCheck().service_check(script)
    factory.assert_called_once()
    cache.kinit.assert_called_once()
    self.assertEqual("/usr/bin/curl", execute.call_args_list[0].args[0][0])
    self.assertEqual(
      "/usr/bigtop/current/spark-client/bin/beeline",
      execute.call_args_list[1].args[0][0],
    )
    thrift_environment = execute.call_args_list[1].kwargs["environment"]
    self.assertEqual("FILE:/private/krb5cc", thrift_environment["KRB5CCNAME"])

  def test_alert_stack_root_resolves_plain_and_json_contracts(self):
    self.assertEqual("/usr/bigtop", SPARK_ALERT._resolve_stack_root("/usr/bigtop"))
    self.assertEqual(
      "/opt/bigtop",
      SPARK_ALERT._resolve_stack_root('{"BIGTOP": "/opt/bigtop"}'),
    )

  def test_alert_stack_root_rejects_unsafe_values(self):
    for value in (None, "relative", "/", "/usr/../etc", '{"HDP": "/usr/hdp"}'):
      with self.subTest(value=value), self.assertRaises(ValueError):
        SPARK_ALERT._resolve_stack_root(value)

  def test_secure_beeline_url_requires_principal(self):
    params = params_module(
      security_enabled=True,
      spark_thrift_port=10016,
      default_hive_kerberos_principal=None,
      spark_transport_mode="binary",
    )
    with self.assertRaisesRegex(Fail, "principal is required"):
      SPARK_CHECK.build_beeline_url(params, "thrift.example.com")

  def test_alert_boolean_parser_rejects_unknown_values(self):
    self.assertTrue(SPARK_ALERT._boolean("true", "setting"))
    self.assertFalse(SPARK_ALERT._boolean("false", "setting"))
    with self.assertRaisesRegex(ValueError, "true or false"):
      SPARK_ALERT._boolean("enabled", "setting")


if __name__ == "__main__":
  unittest.main()
