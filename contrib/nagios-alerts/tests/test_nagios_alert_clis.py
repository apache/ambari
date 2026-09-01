#!/usr/bin/env python3

# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

import base64
from contextlib import redirect_stdout
import io
import json
from pathlib import Path
import runpy
import sys
import tempfile
import unittest
import urllib.error
from unittest.mock import MagicMock, patch


PLUGINS_DIR = Path(__file__).resolve().parents[1] / "plugins"
AMBARI_ALERTS = PLUGINS_DIR / "ambari_alerts.py"
GENERATE_OBJECTS = PLUGINS_DIR / "generate_nagios_objects.py"


def response_with_json(payload):
  response = MagicMock()
  response.read.return_value = json.dumps(payload).encode("utf-8")
  response.__enter__.return_value = response
  return response


class TestAmbariAlertsCli(unittest.TestCase):
  def test_reads_python3_http_response_and_maps_alert_state_to_exit_code(self):
    password = base64.b64encode(b"secret").decode("ascii")
    response = response_with_json(
      {"items": [{"Alert": {"state": "OK", "text": "All checks passed"}}]}
    )
    stdout = io.StringIO()

    with patch.object(
        sys,
        "argv",
        [
          str(AMBARI_ALERTS),
          "ambari.example.com",
          "8080",
          "cluster-a",
          "http",
          "admin",
          password,
          "NAMENODE_PROCESS",
        ],
      ), \
      patch("urllib.request.urlopen", return_value=response) as urlopen, \
      redirect_stdout(stdout), \
      self.assertRaises(SystemExit) as raised:
      runpy.run_path(str(AMBARI_ALERTS), run_name="__main__")

    self.assertEqual(0, raised.exception.code)
    self.assertEqual("All checks passed\n", stdout.getvalue())
    request = urlopen.call_args.args[0]
    self.assertEqual(
      "http://ambari.example.com:8080/api/v1/clusters/cluster-a/alerts"
      "?fields=Alert/label,Alert/service_name,Alert/name,Alert/text,Alert/state"
      "&Alert/name=NAMENODE_PROCESS",
      request.full_url,
    )
    self.assertEqual("Basic YWRtaW46c2VjcmV0", request.get_header("Authorization"))
    urlopen.assert_called_once_with(request, timeout=20)

  def test_network_failure_returns_unknown(self):
    password = base64.b64encode(b"secret").decode("ascii")
    stdout = io.StringIO()

    with patch.object(
        sys,
        "argv",
        [
          str(AMBARI_ALERTS),
          "ambari.example.com",
          "8080",
          "cluster-a",
          "http",
          "admin",
          password,
          "NAMENODE_PROCESS",
        ],
      ), \
      patch(
        "urllib.request.urlopen",
        side_effect=urllib.error.URLError("network unavailable"),
      ), \
      redirect_stdout(stdout), \
      self.assertRaises(SystemExit) as raised:
      runpy.run_path(str(AMBARI_ALERTS), run_name="__main__")

    self.assertEqual(3, raised.exception.code)
    self.assertIn("Unable to retrieve alert info", stdout.getvalue())
    self.assertIn("network unavailable", stdout.getvalue())


class TestGenerateNagiosObjectsCli(unittest.TestCase):
  def test_fetches_alerts_and_writes_python3_text_output(self):
    response = response_with_json(
      {
        "items": [
          {
            "Alert": {
              "service_name": "HDFS",
              "label": "NameNode process",
              "name": "NAMENODE_PROCESS",
              "host_name": "worker-1.example.com",
            }
          }
        ]
      }
    )

    with tempfile.TemporaryDirectory() as directory:
      localhost_cfg = Path(directory) / "localhost.cfg"
      commands_cfg = Path(directory) / "commands.cfg"
      answers = [
        "ambari.example.com",
        "8080",
        "cluster-a",
        "false",
        "admin",
        "secret",
        "/usr/lib/nagios/plugins/ambari_alerts.py",
        str(localhost_cfg),
        str(commands_cfg),
      ]

      with patch("builtins.input", side_effect=answers), \
        patch("urllib.request.urlopen", return_value=response) as urlopen:
        runpy.run_path(str(GENERATE_OBJECTS), run_name="__main__")

      request = urlopen.call_args.args[0]
      self.assertEqual(
        "http://ambari.example.com:8080/api/v1/clusters/cluster-a/alerts"
        "?fields=Alert/label,Alert/service_name,Alert/name,Alert/text,Alert/state",
        request.full_url,
      )
      self.assertEqual("Basic YWRtaW46c2VjcmV0", request.get_header("Authorization"))
      urlopen.assert_called_once_with(request, timeout=20)
      self.assertIn("host_name                       worker-1.example.com", localhost_cfg.read_text(encoding="utf-8"))
      self.assertIn("servicegroups                   AMBARI,HDFS", localhost_cfg.read_text(encoding="utf-8"))
      self.assertIn("command_name    check_ambari_alert", commands_cfg.read_text(encoding="utf-8"))

  def test_network_failure_exits_before_writing_configuration(self):
    stdout = io.StringIO()
    answers = [
      "ambari.example.com",
      "8080",
      "cluster-a",
      "true",
      "admin",
      "secret",
    ]

    with patch("builtins.input", side_effect=answers), \
      patch(
        "urllib.request.urlopen",
        side_effect=urllib.error.URLError("network unavailable"),
      ), \
      redirect_stdout(stdout), \
      self.assertRaises(SystemExit) as raised:
      runpy.run_path(str(GENERATE_OBJECTS), run_name="__main__")

    self.assertEqual(1, raised.exception.code)
    self.assertIn("Error during Ambari Alerts data fetch", stdout.getvalue())
    self.assertIn("network unavailable", stdout.getvalue())


if __name__ == "__main__":
  unittest.main()
