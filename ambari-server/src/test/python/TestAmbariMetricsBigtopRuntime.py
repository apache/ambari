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
import unittest

from resource_management.core.exceptions import Fail


SERVICE = (
  Path(__file__).resolve().parents[2]
  / "main/resources/common-services/AMBARI_METRICS/3.0.0"
)
SCRIPTS = SERVICE / "package/scripts"


def load_module(module_name, path):
  spec = importlib.util.spec_from_file_location(module_name, path)
  module = importlib.util.module_from_spec(spec)
  spec.loader.exec_module(module)
  return module


UTILS = load_module("ambari_metrics_utils", SCRIPTS / "metrics_utils.py")
FUNCTIONS = load_module("ambari_metrics_functions", SCRIPTS / "functions.py")


class TestAmbariMetricsRuntimeContract(unittest.TestCase):
  def test_boolean_parser_does_not_treat_false_string_as_true(self):
    self.assertTrue(UTILS.parse_bool("TRUE", "setting"))
    self.assertFalse(UTILS.parse_bool(" false ", "setting"))
    for value in (None, 1, "yes", "", "false;id"):
      with self.subTest(value=value):
        with self.assertRaises(Fail):
          UTILS.parse_bool(value, "setting")

  def test_ports_hosts_and_ipv6_endpoints_are_strict(self):
    self.assertEqual(("::1", 6188), UTILS.parse_host_port("[::1]:6188", "AMS"))
    self.assertEqual("[2001:db8::1]", UTILS.url_host("2001:db8::1"))
    self.assertEqual("collector.example", UTILS.url_host("collector.example"))
    for endpoint in (
      "collector",
      "collector:0",
      "collector:65536",
      "host;id:6188",
      "999.999.999.999:6188",
      "user@collector:6188",
    ):
      with self.subTest(endpoint=endpoint):
        with self.assertRaises(Fail):
          UTILS.parse_host_port(endpoint, "AMS")

  def test_service_paths_are_canonical_and_not_protected_roots(self):
    self.assertEqual(
      "/var/lib/ambari-metrics-collector/hbase",
      UTILS.local_filesystem_path(
        "file:///var/lib/ambari-metrics-collector/hbase", "HBase root"
      ),
    )
    for path in ("relative", "/", "/tmp", "/var/lib", "/var/run/../run/ams"):
      with self.subTest(path=path):
        with self.assertRaises(Fail):
          UTILS.validate_absolute_path(path, "service path")

  def test_jvm_arguments_and_classpaths_reject_shell_syntax(self):
    self.assertEqual(
      "-Xmx256m -Djava.io.tmpdir=/var/lib/ams/tmp",
      UTILS.validate_jvm_arguments(
        "-Xmx256m -Djava.io.tmpdir=/var/lib/ams/tmp", "JVM options"
      ),
    )
    self.assertEqual(
      "/usr/lib/ams/*:/opt/ams/lib",
      UTILS.validate_classpath(
        "/usr/lib/ams/*:/opt/ams/lib", "additional classpath"
      ),
    )
    for value in ("-Xmx256m;id", "$(id)", "-Dkey='value with spaces'"):
      with self.subTest(value=value):
        with self.assertRaises(Fail):
          UTILS.validate_jvm_arguments(value, "JVM options")

  def test_heap_parser_supports_python_3_and_rejects_partial_matches(self):
    self.assertEqual("1024m", FUNCTIONS.check_append_heap_property("1024"))
    self.assertEqual("200m", FUNCTIONS.calc_xmn_from_xms("1000m", 0.2, 512))
    for value in ("", "0", "1.5g", "512m;id", "512mb"):
      with self.subTest(value=value):
        with self.assertRaises(ValueError):
          FUNCTIONS.check_append_heap_property(value)


if __name__ == "__main__":
  unittest.main()
