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

from collections import namedtuple
from urllib.error import HTTPError
from urllib.request import urlopen
import os
import tempfile
import threading
import unittest
from unittest.mock import patch

from ambari_agent.AmbariConfig import AmbariConfig
from ambari_agent.metrics.core import CollectorRegistry, MetricFamily, render_metrics
from ambari_agent.metrics.linux import (
  CpuCollector,
  DiskIoCollector,
  FilesystemCollector,
  MemoryCollector,
  NetworkCollector,
  ProcessCollector,
  SystemCollector,
  _decode_mount_field,
  default_collectors,
)
from ambari_agent.metrics.server import PrometheusMetricsServer


class StaticCollector:
  name = "static"

  def collect(self):
    family = MetricFamily("test_metric", "A test metric.", "gauge")
    family.add_sample(7, {"source": "unit"})
    return [family]


class FailingCollector:
  name = "failing"

  def collect(self):
    raise RuntimeError("collection failed")


class TestPrometheusMetricCore(unittest.TestCase):
  def test_render_metrics_escapes_help_and_labels(self):
    family = MetricFamily("test_metric", "line one\nline two\\", "gauge")
    family.add_sample(1.5, {"z": 'quoted"', "a": "line\nslash\\"})

    rendered = render_metrics([family]).decode("utf-8")

    self.assertEqual(
      "# HELP test_metric line one\\nline two\\\\\n"
      "# TYPE test_metric gauge\n"
      'test_metric{a="line\\nslash\\\\",z="quoted\\""} 1.5\n',
      rendered,
    )

  def test_metric_and_label_names_are_validated(self):
    with self.assertRaises(ValueError):
      MetricFamily("bad-name", "Bad metric.", "gauge")

    family = MetricFamily("valid_metric", "Valid metric.", "gauge")
    with self.assertRaises(ValueError):
      family.add_sample(1, {"bad-label": "value"})

  def test_render_metrics_merges_compatible_families(self):
    first = MetricFamily("shared_metric", "Shared metric.", "gauge")
    first.add_sample(1, {"component": "first"})
    second = MetricFamily("shared_metric", "Shared metric.", "gauge")
    second.add_sample(2, {"component": "second"})

    rendered = render_metrics([first, second]).decode("utf-8")

    self.assertEqual(1, rendered.count("# HELP shared_metric"))
    self.assertIn('shared_metric{component="first"} 1', rendered)
    self.assertIn('shared_metric{component="second"} 2', rendered)

  def test_registry_isolates_collector_failures(self):
    clock_values = iter((1.0, 1.25, 2.0, 2.5))
    registry = CollectorRegistry(
      [StaticCollector(), FailingCollector()], clock=lambda: next(clock_values)
    )

    with self.assertLogs("ambari_agent.metrics.core", level="ERROR"):
      rendered = registry.render().decode("utf-8")

    self.assertIn('test_metric{source="unit"} 7', rendered)
    self.assertIn('ambari_agent_metrics_collector_up{collector="static"} 1', rendered)
    self.assertIn('ambari_agent_metrics_collector_up{collector="failing"} 0', rendered)
    self.assertIn(
      'ambari_agent_metrics_collector_duration_seconds{collector="failing"} 0.5',
      rendered,
    )


class TestLinuxCollectors(unittest.TestCase):
  def setUp(self):
    self.temp_dir = tempfile.TemporaryDirectory()
    self.proc_root = self.temp_dir.name
    self._write(
      "stat",
      "cpu  100 20 30 400 50 6 7 8 10 2\n"
      "cpu0 60 10 20 200 30 4 5 6 5 1\n"
      "intr 100 10 20\n"
      "ctxt 4321\n"
      "btime 1700000000\n"
      "processes 1234\n"
      "procs_running 3\n"
      "procs_blocked 2\n",
    )
    self._write(
      "meminfo",
      "MemTotal:       1000 kB\n"
      "MemFree:         100 kB\n"
      "MemAvailable:    600 kB\n"
      "Buffers:          20 kB\n"
      "Cached:          300 kB\n"
      "Active:          400 kB\n"
      "Inactive:        200 kB\n"
      "Slab:             50 kB\n"
      "SwapTotal:       500 kB\n"
      "SwapFree:        450 kB\n",
    )
    self._write("loadavg", "1.25 0.75 0.50 2/100 123\n")
    self._write("uptime", "3600.50 7200.00\n")
    self._write("sys/fs/file-nr", "256 0 9223372036854775807\n")
    self._write("sys/fs/file-max", "4096\n")
    self._write("sys/kernel/random/entropy_avail", "192\n")
    self._write("sys/net/netfilter/nf_conntrack_count", "23\n")
    self._write("sys/net/netfilter/nf_conntrack_max", "1024\n")
    self._write("vmstat", "pgfault 500\noom_kill 4\n")
    self._write(
      "net/tcp",
      "  sl  local_address rem_address   st tx_queue rx_queue\n"
      "   0: 0100007F:0016 00000000:0000 0A 00000000:00000000\n"
      "   1: 0100007F:1234 0100007F:5678 01 00000000:00000000\n",
    )
    self._write(
      "net/tcp6",
      "  sl  local_address rem_address   st tx_queue rx_queue\n"
      "   0: 00000000000000000000000000000000:0016 "
      "00000000000000000000000000000000:0000 0A 00000000:00000000\n"
      "   1: 00000000000000000000000000000001:1234 "
      "00000000000000000000000000000001:5678 06 00000000:00000000\n",
    )
    self._write(
      "diskstats",
      "8 0 sda 10 1 20 300 40 2 50 600 3 700 800\n",
    )
    self._write(
      "net/dev",
      "Inter-| Receive | Transmit\n"
      " face |bytes packets errs drop fifo frame compressed multicast|"
      "bytes packets errs drop fifo colls carrier compressed\n"
      "  lo: 1 2 3 4 0 0 0 0 5 6 7 8 0 0 0 0\n"
      "eth0: 100 20 3 4 0 0 0 0 200 30 5 6 0 0 0 0\n",
    )
    self._write(
      "self/mounts",
      "proc /proc proc rw 0 0\n"
      "/dev/sda1 /data\\040disk ext4 rw,relatime 0 0\n"
      "/dev/sdb1 /missing ext4 rw,relatime 0 0\n",
    )
    self._write(
      "1/stat", "1 (init process) S 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 5\n"
    )
    self._write(
      "2/stat", "2 (worker) R 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 3\n"
    )
    self._write("3/stat", "3 (exited) Z 0 0 0 0\n")

  def tearDown(self):
    self.temp_dir.cleanup()

  def _write(self, relative_path, content):
    path = os.path.join(self.proc_root, relative_path)
    directory = os.path.dirname(path)
    if not os.path.isdir(directory):
      os.makedirs(directory)
    with open(path, "w", encoding="utf-8") as stream:
      stream.write(content)

  def _families_by_name(self, collector):
    return {family.name: family for family in collector.collect()}

  def test_cpu_collector_exports_cumulative_seconds(self):
    with patch("ambari_agent.metrics.linux.os.sysconf", return_value=100):
      family = CpuCollector(self.proc_root).collect()[0]

    samples = {
      (sample.labels["cpu"], sample.labels["mode"]): sample.value
      for sample in family.samples
    }
    self.assertEqual(0.9, samples[("total", "user")])
    self.assertEqual(0.18, samples[("total", "nice")])
    self.assertEqual(4.0, samples[("total", "idle")])
    self.assertEqual(0.55, samples[("0", "user")])

  def test_memory_collector_converts_kibibytes_to_bytes(self):
    families = self._families_by_name(MemoryCollector(self.proc_root))

    self.assertEqual(
      1000 * 1024, families["ambari_agent_memory_total_bytes"].samples[0].value
    )
    self.assertEqual(
      600 * 1024,
      families["ambari_agent_memory_available_bytes"].samples[0].value,
    )

  def test_system_collector_exports_load_process_and_host_metrics(self):
    with patch("ambari_agent.metrics.linux.os.cpu_count", return_value=8), patch(
      "ambari_agent.metrics.linux.socket.getfqdn", return_value="host.example.com"
    ):
      families = self._families_by_name(SystemCollector(self.proc_root))

    self.assertEqual(1.25, families["ambari_agent_system_load1"].samples[0].value)
    self.assertEqual(
      1234,
      families["ambari_agent_system_processes_forked_total"].samples[0].value,
    )
    self.assertEqual(
      4321,
      families["ambari_agent_system_context_switches_total"].samples[0].value,
    )
    self.assertEqual(
      100, families["ambari_agent_system_interrupts_total"].samples[0].value
    )
    for metric_name in (
      "ambari_agent_system_context_switches_total",
      "ambari_agent_system_interrupts_total",
      "ambari_agent_file_descriptors_allocated",
      "ambari_agent_file_descriptors_maximum",
      "ambari_agent_entropy_available_bits",
      "ambari_agent_oom_kills_total",
      "ambari_agent_conntrack_entries",
      "ambari_agent_conntrack_entries_limit",
    ):
      self.assertEqual(1, len(families[metric_name].samples))
    self.assertEqual(
      256, families["ambari_agent_file_descriptors_allocated"].samples[0].value
    )
    self.assertEqual(
      4096, families["ambari_agent_file_descriptors_maximum"].samples[0].value
    )
    self.assertEqual(
      192, families["ambari_agent_entropy_available_bits"].samples[0].value
    )
    self.assertEqual(4, families["ambari_agent_oom_kills_total"].samples[0].value)
    self.assertEqual(23, families["ambari_agent_conntrack_entries"].samples[0].value)
    self.assertEqual(
      1024, families["ambari_agent_conntrack_entries_limit"].samples[0].value
    )
    tcp_samples = {
      sample.labels["state"]: sample.value
      for sample in families["ambari_agent_tcp_connections"].samples
    }
    self.assertEqual(1, tcp_samples["established"])
    self.assertEqual(2, tcp_samples["listen"])
    self.assertEqual(1, tcp_samples["time_wait"])
    self.assertEqual(0, tcp_samples["close_wait"])
    self.assertEqual(
      {"hostname": "host.example.com"},
      families["ambari_agent_host_info"].samples[0].labels,
    )

  def test_system_collector_ignores_missing_optional_kernel_files(self):
    for relative_path in (
      "sys/fs/file-nr",
      "sys/fs/file-max",
      "sys/kernel/random/entropy_avail",
      "sys/net/netfilter/nf_conntrack_count",
      "sys/net/netfilter/nf_conntrack_max",
      "vmstat",
      "net/tcp",
      "net/tcp6",
    ):
      os.remove(os.path.join(self.proc_root, relative_path))

    families = self._families_by_name(SystemCollector(self.proc_root))

    self.assertEqual(1.25, families["ambari_agent_system_load1"].samples[0].value)
    self.assertNotIn("ambari_agent_file_descriptors_allocated", families)
    self.assertNotIn("ambari_agent_tcp_connections", families)

  def test_filesystem_collector_skips_pseudo_filesystems(self):
    StatVfs = namedtuple(
      "StatVfs", "f_blocks f_frsize f_bfree f_bavail f_files f_ffree"
    )

    def statvfs(path):
      if path == "/data disk":
        return StatVfs(100, 4096, 40, 30, 50, 20)
      raise OSError("stale mount")

    collector = FilesystemCollector(
      self.proc_root,
      statvfs=statvfs,
    )

    with self.assertLogs("ambari_agent.metrics.linux", level="WARNING"):
      families = self._families_by_name(collector)

    sample = families["ambari_agent_filesystem_size_bytes"].samples[0]
    self.assertEqual(409600, sample.value)
    self.assertEqual("/data disk", sample.labels["mountpoint"])
    self.assertEqual(1, len(families["ambari_agent_filesystem_size_bytes"].samples))

  def test_disk_io_collector_exports_kernel_counters(self):
    families = self._families_by_name(DiskIoCollector(self.proc_root))

    self.assertEqual(
      10, families["ambari_agent_disk_reads_completed_total"].samples[0].value
    )
    self.assertEqual(
      20 * 512, families["ambari_agent_disk_read_bytes_total"].samples[0].value
    )
    self.assertEqual(
      0.6,
      families["ambari_agent_disk_write_time_seconds_total"].samples[0].value,
    )

  def test_network_collector_excludes_loopback(self):
    families = self._families_by_name(NetworkCollector(self.proc_root))

    sample = families["ambari_agent_network_receive_bytes_total"].samples[0]
    self.assertEqual(100, sample.value)
    self.assertEqual({"device": "eth0"}, sample.labels)
    self.assertEqual(
      200,
      families["ambari_agent_network_transmit_bytes_total"].samples[0].value,
    )

  def test_process_collector_exports_low_cardinality_state_counts(self):
    family, thread_family = ProcessCollector(self.proc_root).collect()
    samples = {sample.labels["state"]: sample.value for sample in family.samples}

    self.assertEqual(3, samples["total"])
    self.assertEqual(1, samples["running"])
    self.assertEqual(1, samples["sleeping"])
    self.assertEqual(1, samples["zombie"])
    self.assertEqual(0, samples["unknown"])
    self.assertEqual(8, thread_family.samples[0].value)

  def test_default_collectors_require_proc(self):
    self.assertEqual([], default_collectors(os.path.join(self.proc_root, "missing")))
    self.assertEqual(7, len(default_collectors(self.proc_root)))

  def test_mount_field_decoding(self):
    self.assertEqual("a b\tc\nd\\e", _decode_mount_field("a\\040b\\011c\\012d\\134e"))


class TestPrometheusMetricsServer(unittest.TestCase):
  def _initializer(self, enabled=True, port=0):
    class Config:
      prometheus_metrics_enabled = enabled
      prometheus_metrics_bind_address = "127.0.0.1"
      prometheus_metrics_port = port

    class Initializer:
      config = Config()
      stop_event = threading.Event()

    return Initializer()

  def test_ambari_config_defaults_to_enabled(self):
    config = AmbariConfig()

    self.assertTrue(config.prometheus_metrics_enabled)
    self.assertEqual("0.0.0.0", config.prometheus_metrics_bind_address)
    self.assertEqual(9101, config.prometheus_metrics_port)

    config.set("prometheus", "enabled", "false")
    self.assertFalse(config.prometheus_metrics_enabled)

    config.set("prometheus", "enabled", "YES")
    self.assertTrue(config.prometheus_metrics_enabled)

    config.set("prometheus", "port", "invalid")
    with self.assertLogs("ambari_agent.AmbariConfig", level="WARNING"):
      self.assertEqual(9101, config.prometheus_metrics_port)

    config.set("prometheus", "port", "70000")
    with self.assertLogs("ambari_agent.AmbariConfig", level="WARNING"):
      self.assertEqual(9101, config.prometheus_metrics_port)

  def test_server_exposes_metrics_health_and_not_found(self):
    server = PrometheusMetricsServer(
      self._initializer(), registry=CollectorRegistry([StaticCollector()])
    )
    server.start()
    self.assertTrue(server.ready_event.wait(2))
    self.assertIsNone(server.startup_error)
    port = server.server_address[1]

    try:
      with urlopen(f"http://127.0.0.1:{port}/metrics", timeout=2) as response:
        body = response.read().decode("utf-8")
        self.assertEqual(200, response.status)
        self.assertEqual(
          "text/plain; version=0.0.4; charset=utf-8",
          response.headers["Content-Type"],
        )
      self.assertIn('test_metric{source="unit"} 7', body)

      with urlopen(f"http://127.0.0.1:{port}/-/healthy", timeout=2) as response:
        self.assertEqual(200, response.status)

      with self.assertRaises(HTTPError) as context:
        urlopen(f"http://127.0.0.1:{port}/missing", timeout=2)
      self.assertEqual(404, context.exception.code)
    finally:
      server.stop()
      server.join(2)

    self.assertFalse(server.is_alive())

  def test_disabled_server_does_not_bind(self):
    server = PrometheusMetricsServer(self._initializer(enabled=False))

    server.start()
    server.join(2)

    self.assertFalse(server.is_alive())
    self.assertIsNone(server.server_address)
    self.assertIsNone(server.startup_error)

  def test_bind_failure_is_contained(self):
    class FailingServer:
      def __init__(self, address, handler):
        raise OSError("address unavailable")

    server = PrometheusMetricsServer(
      self._initializer(),
      registry=CollectorRegistry([StaticCollector()]),
      server_class=FailingServer,
    )

    with self.assertLogs("ambari_agent.metrics.server", level="ERROR"):
      server.start()
      self.assertTrue(server.ready_event.wait(2))
      server.join(2)

    self.assertIsInstance(server.startup_error, OSError)
    self.assertFalse(server.is_alive())


if __name__ == "__main__":
  unittest.main()
