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

import logging
import os
import socket

from ambari_agent.metrics.core import MetricFamily


logger = logging.getLogger(__name__)
NAMESPACE = "ambari_agent"
CPU_MODES = (
  "user",
  "nice",
  "system",
  "idle",
  "iowait",
  "irq",
  "softirq",
  "steal",
)
MEMORY_FIELDS = {
  "MemTotal": "memory_total_bytes",
  "MemFree": "memory_free_bytes",
  "MemAvailable": "memory_available_bytes",
  "Buffers": "memory_buffers_bytes",
  "Cached": "memory_cached_bytes",
  "Active": "memory_active_bytes",
  "Inactive": "memory_inactive_bytes",
  "Slab": "memory_slab_bytes",
  "SwapTotal": "memory_swap_total_bytes",
  "SwapFree": "memory_swap_free_bytes",
}
PSEUDO_FILESYSTEMS = frozenset(
  (
    "autofs",
    "bpf",
    "cgroup",
    "cgroup2",
    "configfs",
    "debugfs",
    "devpts",
    "devtmpfs",
    "fusectl",
    "hugetlbfs",
    "mqueue",
    "proc",
    "pstore",
    "securityfs",
    "sysfs",
    "tracefs",
  )
)


class LinuxCollector:
  def __init__(self, proc_root="/proc"):
    self.proc_root = proc_root

  def _read(self, name):
    with open(os.path.join(self.proc_root, name), encoding="utf-8") as stream:
      return stream.read()


class CpuCollector(LinuxCollector):
  name = "cpu"

  def collect(self):
    family = MetricFamily(
      f"{NAMESPACE}_cpu_seconds_total",
      "Seconds the CPUs spent in each mode.",
      "counter",
    )
    ticks_per_second = float(os.sysconf("SC_CLK_TCK"))

    for line in self._read("stat").splitlines():
      fields = line.split()
      if not fields or not fields[0].startswith("cpu"):
        continue
      if fields[0] != "cpu" and not fields[0][3:].isdigit():
        continue

      values = [int(value) for value in fields[1:]]
      values.extend([0] * (10 - len(values)))
      values[0] -= values[8]
      values[1] -= values[9]
      cpu = "total" if fields[0] == "cpu" else fields[0][3:]
      for mode, ticks in zip(CPU_MODES, values):
        family.add_sample(ticks / ticks_per_second, {"cpu": cpu, "mode": mode})

    return [family]


class MemoryCollector(LinuxCollector):
  name = "memory"

  def collect(self):
    values = {}
    for line in self._read("meminfo").splitlines():
      key, raw_value = line.split(":", 1)
      if key not in MEMORY_FIELDS:
        continue
      fields = raw_value.split()
      multiplier = 1024 if len(fields) > 1 and fields[1] == "kB" else 1
      values[key] = int(fields[0]) * multiplier

    families = []
    for source_name, metric_name in MEMORY_FIELDS.items():
      if source_name not in values:
        continue
      family = MetricFamily(
        f"{NAMESPACE}_{metric_name}",
        f"Linux {source_name} memory value in bytes.",
        "gauge",
      )
      family.add_sample(values[source_name])
      families.append(family)
    return families


class SystemCollector(LinuxCollector):
  name = "system"

  def collect(self):
    load_values = [float(value) for value in self._read("loadavg").split()[:3]]
    uptime = float(self._read("uptime").split()[0])
    stat_values = {}
    for line in self._read("stat").splitlines():
      fields = line.split()
      if len(fields) == 2 and fields[0] in (
        "btime",
        "processes",
        "procs_running",
        "procs_blocked",
      ):
        stat_values[fields[0]] = int(fields[1])

    metrics = (
      ("system_load1", "One minute system load average.", "gauge", load_values[0]),
      ("system_load5", "Five minute system load average.", "gauge", load_values[1]),
      ("system_load15", "Fifteen minute system load average.", "gauge", load_values[2]),
      ("system_uptime_seconds", "System uptime in seconds.", "gauge", uptime),
      (
        "system_cpu_count",
        "Number of logical CPUs visible to the Agent.",
        "gauge",
        os.cpu_count() or 0,
      ),
      (
        "system_boot_time_seconds",
        "System boot time as Unix epoch seconds.",
        "gauge",
        stat_values.get("btime", 0),
      ),
      (
        "system_processes_forked_total",
        "Total number of processes forked since boot.",
        "counter",
        stat_values.get("processes", 0),
      ),
      (
        "system_processes_running",
        "Number of processes currently runnable.",
        "gauge",
        stat_values.get("procs_running", 0),
      ),
      (
        "system_processes_blocked",
        "Number of processes currently blocked on I/O.",
        "gauge",
        stat_values.get("procs_blocked", 0),
      ),
    )

    families = []
    for name, help_text, metric_type, value in metrics:
      family = MetricFamily(f"{NAMESPACE}_{name}", help_text, metric_type)
      family.add_sample(value)
      families.append(family)

    info = MetricFamily(
      f"{NAMESPACE}_host_info",
      "Static information about the host running Ambari Agent.",
      "gauge",
    )
    info.add_sample(1, {"hostname": socket.getfqdn()})
    families.append(info)
    return families


class FilesystemCollector(LinuxCollector):
  name = "filesystem"

  def __init__(self, proc_root="/proc", statvfs=None):
    super().__init__(proc_root)
    self.statvfs = statvfs or os.statvfs

  def collect(self):
    size = _filesystem_family("size_bytes", "Filesystem size in bytes.")
    free = _filesystem_family("free_bytes", "Filesystem free space in bytes.")
    available = _filesystem_family(
      "available_bytes",
      "Filesystem space available to unprivileged users in bytes.",
    )
    files = _filesystem_family("files", "Filesystem total file nodes.")
    files_free = _filesystem_family("files_free", "Filesystem free file nodes.")

    for line in self._read("self/mounts").splitlines():
      fields = line.split()
      if len(fields) < 3 or fields[2] in PSEUDO_FILESYSTEMS:
        continue

      device = _decode_mount_field(fields[0])
      mountpoint = _decode_mount_field(fields[1])
      filesystem_type = fields[2]
      try:
        stats = self.statvfs(mountpoint)
      except OSError:
        logger.warning('Unable to read filesystem metrics for "%s"', mountpoint)
        continue
      labels = {
        "device": device,
        "fstype": filesystem_type,
        "mountpoint": mountpoint,
      }
      size.add_sample(stats.f_blocks * stats.f_frsize, labels)
      free.add_sample(stats.f_bfree * stats.f_frsize, labels)
      available.add_sample(stats.f_bavail * stats.f_frsize, labels)
      files.add_sample(stats.f_files, labels)
      files_free.add_sample(stats.f_ffree, labels)

    return [size, free, available, files, files_free]


class DiskIoCollector(LinuxCollector):
  name = "disk_io"

  def collect(self):
    definitions = (
      ("disk_reads_completed_total", "Completed disk read operations.", "counter"),
      ("disk_read_bytes_total", "Bytes read from disk.", "counter"),
      ("disk_read_time_seconds_total", "Seconds spent reading from disk.", "counter"),
      ("disk_writes_completed_total", "Completed disk write operations.", "counter"),
      ("disk_written_bytes_total", "Bytes written to disk.", "counter"),
      ("disk_write_time_seconds_total", "Seconds spent writing to disk.", "counter"),
      ("disk_io_in_progress", "Disk I/O operations currently in progress.", "gauge"),
      ("disk_io_time_seconds_total", "Seconds spent doing disk I/O.", "counter"),
      (
        "disk_io_weighted_time_seconds_total",
        "Weighted seconds spent doing disk I/O.",
        "counter",
      ),
    )
    families = [
      MetricFamily(f"{NAMESPACE}_{name}", help_text, metric_type)
      for name, help_text, metric_type in definitions
    ]

    for line in self._read("diskstats").splitlines():
      fields = line.split()
      if len(fields) < 14:
        continue
      labels = {"device": fields[2]}
      values = (
        int(fields[3]),
        int(fields[5]) * 512,
        int(fields[6]) / 1000.0,
        int(fields[7]),
        int(fields[9]) * 512,
        int(fields[10]) / 1000.0,
        int(fields[11]),
        int(fields[12]) / 1000.0,
        int(fields[13]) / 1000.0,
      )
      for family, value in zip(families, values):
        family.add_sample(value, labels)

    return families


class NetworkCollector(LinuxCollector):
  name = "network"

  def collect(self):
    definitions = (
      ("network_receive_bytes_total", "Network bytes received."),
      ("network_receive_packets_total", "Network packets received."),
      ("network_receive_errors_total", "Network receive errors."),
      ("network_receive_dropped_total", "Network receive packets dropped."),
      ("network_transmit_bytes_total", "Network bytes transmitted."),
      ("network_transmit_packets_total", "Network packets transmitted."),
      ("network_transmit_errors_total", "Network transmit errors."),
      ("network_transmit_dropped_total", "Network transmit packets dropped."),
    )
    families = [
      MetricFamily(f"{NAMESPACE}_{name}", help_text, "counter")
      for name, help_text in definitions
    ]

    for line in self._read("net/dev").splitlines()[2:]:
      if ":" not in line:
        continue
      device, raw_values = line.split(":", 1)
      device = device.strip()
      if device == "lo":
        continue
      values = [int(value) for value in raw_values.split()]
      if len(values) < 16:
        continue
      selected = values[0:4] + values[8:12]
      for family, value in zip(families, selected):
        family.add_sample(value, {"device": device})

    return families


class ProcessCollector(LinuxCollector):
  name = "processes"

  PROCESS_STATES = {
    "R": "running",
    "S": "sleeping",
    "D": "blocked",
    "Z": "zombie",
    "T": "stopped",
    "t": "stopped",
    "X": "dead",
    "x": "dead",
    "I": "idle",
    "W": "paging",
  }

  def collect(self):
    counts = {state: 0 for state in set(self.PROCESS_STATES.values())}
    counts["unknown"] = 0
    counts["total"] = 0

    for entry in os.listdir(self.proc_root):
      if not entry.isdigit():
        continue
      try:
        stat = self._read(os.path.join(entry, "stat"))
      except OSError:
        continue

      command_end = stat.rfind(")")
      if command_end < 0 or len(stat) <= command_end + 2:
        counts["unknown"] += 1
      else:
        state = stat[command_end + 2]
        counts[self.PROCESS_STATES.get(state, "unknown")] += 1
      counts["total"] += 1

    family = MetricFamily(
      f"{NAMESPACE}_processes",
      "Number of processes in each Linux process state.",
      "gauge",
    )
    for state in sorted(counts):
      family.add_sample(counts[state], {"state": state})
    return [family]


def default_collectors(proc_root="/proc"):
  if not os.path.isdir(proc_root):
    return []
  return [
    CpuCollector(proc_root),
    MemoryCollector(proc_root),
    SystemCollector(proc_root),
    FilesystemCollector(proc_root),
    DiskIoCollector(proc_root),
    NetworkCollector(proc_root),
    ProcessCollector(proc_root),
  ]


def _filesystem_family(name, help_text):
  return MetricFamily(f"{NAMESPACE}_filesystem_{name}", help_text, "gauge")


def _decode_mount_field(value):
  return (
    value.replace("\\040", " ")
    .replace("\\011", "\t")
    .replace("\\012", "\n")
    .replace("\\134", "\\")
  )
