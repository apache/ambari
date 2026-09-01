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
from types import SimpleNamespace
import unittest
from unittest.mock import MagicMock, patch


SERVICE = Path(__file__).resolve().parents[2] / (
  "main/resources/common-services/AMBARI_METRICS/3.0.0"
)
SCRIPTS = SERVICE / "package/scripts"


def load_module(module_name, path):
  spec = importlib.util.spec_from_file_location(module_name, path)
  module = importlib.util.module_from_spec(spec)
  spec.loader.exec_module(module)
  return module


AMS = load_module("ambari_metrics_configuration", SCRIPTS / "ams.py")


class TestAmbariMetricsConfiguration(unittest.TestCase):
  def test_jks_password_is_passed_only_through_the_command_environment(self):
    params = SimpleNamespace(
      metric_truststore_ca_certs="ca.pem",
      metric_truststore_path="/etc/security/ams/truststore.jks",
      metric_truststore_type="jks",
      metric_truststore_password="do-not-log-this-secret",
      java64_home="/usr/lib/jvm/java-17",
      ams_user="ams",
      user_group="hadoop",
    )
    execute = MagicMock()
    with patch.dict(sys.modules, {"params": params}), \
      patch.object(AMS.os.path, "isfile", return_value=True), \
      patch.object(AMS.os.path, "islink", return_value=False), \
      patch("tempfile.mkdtemp", return_value="/tmp/ams-truststore-private"), \
      patch.object(AMS, "Execute", execute), \
      patch.object(AMS, "File"), \
      patch.object(AMS, "Directory") as directory, \
      patch.object(AMS, "StaticFile"):
      AMS.export_ca_certs("/etc/ambari-metrics-monitor/conf")

    self.assertEqual(2, execute.call_count)
    for command_call in execute.call_args_list:
      command = command_call.args[0]
      self.assertIsInstance(command, tuple)
      self.assertNotIn(params.metric_truststore_password, command)
      self.assertEqual(
        params.metric_truststore_password,
        command_call.kwargs["environment"]["AMS_TRUSTSTORE_PASSWORD"],
      )
    directory.assert_called_once_with(
      "/tmp/ams-truststore-private", action="delete"
    )


if __name__ == "__main__":
  unittest.main()
