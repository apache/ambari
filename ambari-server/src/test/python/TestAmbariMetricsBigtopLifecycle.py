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
from unittest.mock import MagicMock, call, patch

from resource_management.core.exceptions import Fail
from resource_management.libraries.functions import safe_process


SERVICE = (
  Path(__file__).resolve().parents[2]
  / "main/resources/common-services/AMBARI_METRICS/3.0.0"
)
SCRIPTS = SERVICE / "package/scripts"
IDENTITY = safe_process.ProcessIdentity(
  123,
  1001,
  456,
  (
    "/usr/bin/java",
    "-Dproc_ams-metrics-collector",
    "org.apache.ambari.metrics.AMSApplicationServer",
  ),
)


def load_module(module_name, path, dependencies=None):
  spec = importlib.util.spec_from_file_location(module_name, path)
  module = importlib.util.module_from_spec(spec)
  with patch.dict(sys.modules, dependencies or {}):
    spec.loader.exec_module(module)
  return module


METRICS_PROCESS = load_module(
  "ambari_metrics_process", SCRIPTS / "metrics_process.py"
)
HBASE_SERVICE = load_module(
  "ambari_metrics_hbase_service",
  SCRIPTS / "hbase_service.py",
  {"metrics_process": METRICS_PROCESS},
)
HBASE_SERVICE_MODULE = ModuleType("hbase_service")
HBASE_SERVICE_MODULE.hbase_service = MagicMock()
AMS_SERVICE = load_module(
  "ambari_metrics_ams_service",
  SCRIPTS / "ams_service.py",
  {
    "hbase_service": HBASE_SERVICE_MODULE,
    "metrics_process": METRICS_PROCESS,
  },
)
STATUS_MODULE = ModuleType("status")
STATUS_MODULE.check_service_status = MagicMock()
AMS_CONFIG_MODULE = ModuleType("ams")
AMS_CONFIG_MODULE.ams = MagicMock()
GRAFANA = load_module(
  "ambari_metrics_grafana",
  SCRIPTS / "metrics_grafana.py",
  {
    "ams": AMS_CONFIG_MODULE,
    "metrics_process": METRICS_PROCESS,
    "status": STATUS_MODULE,
  },
)


class TestAmbariMetricsProcessIdentity(unittest.TestCase):
  def test_process_tokens_match_bigtop_ambari_metrics_300_layout(self):
    self.assertEqual(
      (
        "-Dproc_ams-metrics-collector",
        "org.apache.ambari.metrics.AMSApplicationServer",
      ),
      METRICS_PROCESS.ams_process_tokens("collector"),
    )
    self.assertEqual(
      ("/usr/lib/python3.9/site-packages/resource_monitoring/main.py",),
      METRICS_PROCESS.ams_process_tokens("monitor"),
    )
    self.assertEqual(
      ("org.apache.hadoop.hbase.master.HMaster",),
      METRICS_PROCESS.hbase_process_tokens("master"),
    )

  def test_valid_pid_identity_is_reused_without_discovery(self):
    with patch.object(safe_process, "read_pid", return_value=123), \
      patch.object(safe_process, "inspect_process", return_value=IDENTITY), \
      patch.object(safe_process, "is_process_running", return_value=True), \
      patch.object(
        safe_process, "secure_pid_file_for_identity", return_value=IDENTITY
      ) as secure, \
      patch.object(safe_process, "discover_running_process") as discover:
      result = METRICS_PROCESS.read_or_discover_ams_process(
        "/var/run/ambari-metrics-collector/ambari-metrics-collector.pid",
        "ams",
        "hadoop",
        "collector",
      )
    self.assertIs(IDENTITY, result)
    secure.assert_called_once_with(
      "/var/run/ambari-metrics-collector/ambari-metrics-collector.pid",
      IDENTITY,
      "ams",
      METRICS_PROCESS.ams_process_tokens("collector"),
      owner="ams",
      group="hadoop",
      mode=0o640,
    )
    discover.assert_not_called()

  def test_stale_pid_is_removed_before_unique_process_recovery(self):
    with patch.object(safe_process, "read_pid", return_value=123), \
      patch.object(safe_process, "inspect_process", return_value=None), \
      patch.object(safe_process, "remove_pid_file_if_stopped") as remove, \
      patch.object(safe_process, "discover_running_process", return_value=IDENTITY), \
      patch.object(
        safe_process, "create_pid_file_for_identity", return_value=IDENTITY
      ) as create:
      result = METRICS_PROCESS.read_or_discover_ams_process(
        "/var/run/ambari-metrics-collector/ambari-metrics-collector.pid",
        "ams",
        "hadoop",
        "collector",
      )
    self.assertIs(IDENTITY, result)
    remove.assert_called_once()
    self.assertEqual(0o640, create.call_args.kwargs["mode"])

  def test_mismatched_pid_identity_is_not_replaced_by_process_discovery(self):
    with patch.object(safe_process, "read_pid", return_value=123), \
      patch.object(
        safe_process,
        "inspect_process",
        side_effect=Fail("pid belongs to another process"),
      ), \
      patch.object(safe_process, "discover_running_process") as discover:
      with self.assertRaises(Fail):
        METRICS_PROCESS.read_or_discover_ams_process(
          "/var/run/ambari-metrics-collector/ambari-metrics-collector.pid",
          "ams",
          "hadoop",
          "collector",
        )
    discover.assert_not_called()

  def test_stop_pins_identity_then_uses_term_wait_kill_contract(self):
    with patch.object(
        METRICS_PROCESS,
        "read_or_discover_ams_process",
        return_value=IDENTITY,
      ), \
      patch.object(safe_process, "terminate_process") as terminate, \
      patch.object(safe_process, "remove_pid_file_if_stopped") as remove:
      self.assertTrue(
        METRICS_PROCESS.stop_ams_process(
          "/var/run/ambari-metrics-collector/ambari-metrics-collector.pid",
          "ams",
          "hadoop",
          "collector",
        )
      )
    terminate.assert_called_once_with(
      IDENTITY,
      "ams",
      METRICS_PROCESS.ams_process_tokens("collector"),
      term_wait_attempts=15,
      term_wait_sleep=1,
      kill_wait_attempts=10,
      kill_wait_sleep=1,
    )
    remove.assert_called_once()


class TestAmbariMetricsLifecycleRollback(unittest.TestCase):
  def test_hbase_start_uses_structured_command_and_waits_for_identity(self):
    params = SimpleNamespace(
      hbase_pid_dir="/var/run/ambari-metrics-collector",
      hbase_user="ams",
      user_group="hadoop",
      daemon_script="/usr/lib/ams-hbase/bin/hbase-daemon.sh",
      hbase_conf_dir="/etc/ams-hbase/conf",
      java64_home="/usr/lib/jvm/java-17",
      hbase_log_dir="/var/log/ambari-metrics-collector",
    )
    with patch.dict(sys.modules, {"params": params}), \
      patch.object(HBASE_SERVICE, "read_or_discover_hbase_process", return_value=None), \
      patch.object(HBASE_SERVICE, "Execute") as execute, \
      patch.object(HBASE_SERVICE, "wait_for_hbase_process") as wait:
      self.assertTrue(HBASE_SERVICE.hbase_service("master", "start"))
    execute.assert_called_once_with(
      (
        "/usr/lib/ams-hbase/bin/hbase-daemon.sh",
        "--config",
        "/etc/ams-hbase/conf",
        "start",
        "master",
      ),
      user="ams",
      environment={"JAVA_HOME": "/usr/lib/jvm/java-17"},
      timeout=60,
    )
    wait.assert_called_once()

  def test_collector_failure_rolls_back_only_new_distributed_hbase_roles(self):
    params = SimpleNamespace(
      ams_collector_script="/usr/sbin/ambari-metrics-collector",
      ams_collector_conf_dir="/etc/ambari-metrics-collector/conf",
      ams_collector_pid_dir="/var/run/ambari-metrics-collector",
      ams_collector_log_dir="/var/log/ambari-metrics-collector",
      ams_user="ams",
      user_group="hadoop",
      java64_home="/usr/lib/jvm/java-17",
      is_hbase_distributed=True,
      security_enabled=False,
    )
    HBASE_SERVICE_MODULE.hbase_service.reset_mock()
    HBASE_SERVICE_MODULE.hbase_service.side_effect = [True, False, True]
    with patch.dict(sys.modules, {"params": params}), \
      patch.object(AMS_SERVICE, "read_or_discover_ams_process", return_value=None), \
      patch.object(AMS_SERVICE, "Execute", side_effect=Fail("start failed")), \
      patch.object(AMS_SERVICE, "stop_ams_process", return_value=False), \
      patch.object(AMS_SERVICE, "show_logs"):
      with self.assertRaises(Fail):
        AMS_SERVICE.ams_service("collector", "start")
    self.assertEqual(
      [
        call("master", action="start"),
        call("regionserver", action="start"),
        call("master", action="stop"),
      ],
      HBASE_SERVICE_MODULE.hbase_service.call_args_list,
    )

  def test_monitor_start_enforces_python_392_before_structured_start(self):
    params = SimpleNamespace(
      ams_monitor_script="/usr/sbin/ambari-metrics-monitor",
      ams_monitor_conf_dir="/etc/ambari-metrics-monitor/conf",
      ams_monitor_pid_dir="/var/run/ambari-metrics-monitor",
      ams_monitor_log_dir="/var/log/ambari-metrics-monitor",
      ams_user="ams",
      user_group="hadoop",
      java64_home="/usr/lib/jvm/java-17",
      python_binary="/usr/bin/python3.9",
      security_enabled=False,
    )
    with patch.dict(sys.modules, {"params": params}), \
      patch.object(AMS_SERVICE, "read_or_discover_ams_process", return_value=None), \
      patch.object(AMS_SERVICE, "Execute") as execute, \
      patch.object(AMS_SERVICE, "wait_for_ams_process"):
      AMS_SERVICE.ams_service("monitor", "start")
    self.assertEqual(
      (
        "/usr/bin/python3.9",
        "-c",
        "import sys; raise SystemExit(0 if sys.version_info >= (3, 9, 2) else 1)",
      ),
      execute.call_args_list[0].args[0],
    )
    self.assertEqual(
      (
        "/usr/sbin/ambari-metrics-monitor",
        "--config",
        "/etc/ambari-metrics-monitor/conf",
        "start",
      ),
      execute.call_args_list[1].args[0],
    )

  def test_monitor_stop_without_optional_kerberos_identity_is_idempotent(self):
    params = SimpleNamespace(
      ams_monitor_script="/usr/sbin/ambari-metrics-monitor",
      ams_monitor_conf_dir="/etc/ambari-metrics-monitor/conf",
      ams_monitor_pid_dir="/var/run/ambari-metrics-monitor",
      ams_monitor_log_dir="/var/log/ambari-metrics-monitor",
      ams_user="ams",
      user_group="hadoop",
      security_enabled=True,
      monitor_kinit_cmd="",
    )
    with patch.dict(sys.modules, {"params": params}), \
      patch.object(AMS_SERVICE, "stop_ams_process", return_value=False), \
      patch.object(AMS_SERVICE, "File") as file_resource:
      AMS_SERVICE.ams_service("monitor", "stop")
    file_resource.assert_not_called()

  def test_hbase_start_failure_stops_discovered_partial_process(self):
    params = SimpleNamespace(
      hbase_pid_dir="/var/run/ambari-metrics-collector",
      hbase_user="ams",
      user_group="hadoop",
      daemon_script="/usr/lib/ams-hbase/bin/hbase-daemon.sh",
      hbase_conf_dir="/etc/ams-hbase/conf",
      java64_home="/usr/lib/jvm/java-17",
      hbase_log_dir="/var/log/ambari-metrics-collector",
      hbase_regionserver_shutdown_timeout=30,
    )
    with patch.dict(sys.modules, {"params": params}), \
      patch.object(HBASE_SERVICE, "read_or_discover_hbase_process", return_value=None), \
      patch.object(HBASE_SERVICE, "Execute"), \
      patch.object(
        HBASE_SERVICE, "wait_for_hbase_process", side_effect=Fail("not running")
      ), \
      patch.object(HBASE_SERVICE, "stop_hbase_process", return_value=True) as stop, \
      patch.object(HBASE_SERVICE, "show_logs"):
      with self.assertRaises(Fail):
        HBASE_SERVICE.hbase_service("master", "start")
    stop.assert_called_once_with(
      "/var/run/ambari-metrics-collector/hbase-ams-master.pid",
      "ams",
      "hadoop",
      "master",
      wait_attempts=30,
    )

  def test_grafana_command_failure_rolls_back_a_partially_started_process(self):
    params = SimpleNamespace(
      ams_grafana_host="grafana.example",
      ams_grafana_admin_pwd="secret",
      grafana_pid_file="/var/run/ambari-metrics-grafana/grafana-server.pid",
      ams_user="ams",
      user_group="hadoop",
      ams_grafana_script="/usr/sbin/ambari-metrics-grafana",
      java64_home="/usr/lib/jvm/java-17",
      ams_grafana_log_dir="/var/log/ambari-metrics-grafana",
    )
    with patch.dict(sys.modules, {"params": params}), \
      patch.object(GRAFANA, "read_or_discover_ams_process", return_value=None), \
      patch.object(GRAFANA, "Execute", side_effect=Fail("start timed out")), \
      patch.object(GRAFANA, "stop_ams_process", return_value=True) as stop, \
      patch.object(GRAFANA, "show_logs"):
      with self.assertRaises(Fail):
        GRAFANA.AmsGrafana().start(MagicMock())
    stop.assert_called_once_with(
      "/var/run/ambari-metrics-grafana/grafana-server.pid",
      "ams",
      "hadoop",
      "grafana",
    )


if __name__ == "__main__":
  unittest.main()
