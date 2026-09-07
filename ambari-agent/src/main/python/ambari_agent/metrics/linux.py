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

import ctypes
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
  "MemTotal": ("memory_total_bytes", "bytes"),
  "MemFree": ("memory_free_bytes", "bytes"),
  "MemAvailable": ("memory_available_bytes", "bytes"),
  "Buffers": ("memory_buffers_bytes", "bytes"),
  "Cached": ("memory_cached_bytes", "bytes"),
  "SwapCached": ("memory_swap_cached_bytes", "bytes"),
  "Active": ("memory_active_bytes", "bytes"),
  "Active(anon)": ("memory_active_anon_bytes", "bytes"),
  "Active(file)": ("memory_active_file_bytes", "bytes"),
  "Inactive": ("memory_inactive_bytes", "bytes"),
  "Inactive(anon)": ("memory_inactive_anon_bytes", "bytes"),
  "Inactive(file)": ("memory_inactive_file_bytes", "bytes"),
  "Unevictable": ("memory_unevictable_bytes", "bytes"),
  "Mlocked": ("memory_mlocked_bytes", "bytes"),
  "SwapTotal": ("memory_swap_total_bytes", "bytes"),
  "SwapFree": ("memory_swap_free_bytes", "bytes"),
  "Dirty": ("memory_dirty_bytes", "bytes"),
  "Writeback": ("memory_writeback_bytes", "bytes"),
  "AnonPages": ("memory_anon_pages_bytes", "bytes"),
  "Mapped": ("memory_mapped_bytes", "bytes"),
  "Shmem": ("memory_shmem_bytes", "bytes"),
  "Slab": ("memory_slab_bytes", "bytes"),
  "SReclaimable": ("memory_sreclaimable_bytes", "bytes"),
  "SUnreclaim": ("memory_sunreclaim_bytes", "bytes"),
  "KernelStack": ("memory_kernel_stack_bytes", "bytes"),
  "NFS_Unstable": ("memory_nfs_unstable_bytes", "bytes"),
  "Bounce": ("memory_bounce_bytes", "bytes"),
  "VmallocUsed": ("memory_vmalloc_used_bytes", "bytes"),
  "VmallocChunk": ("memory_vmalloc_chunk_bytes", "bytes"),
  "AnonHugePages": ("memory_anon_huge_pages_bytes", "bytes"),
  "HugePages_Total": ("memory_hugepages_total", "pages"),
  "HugePages_Free": ("memory_hugepages_free", "pages"),
  "HugePages_Rsvd": ("memory_hugepages_reserved", "pages"),
  "HugePages_Surp": ("memory_hugepages_surplus", "pages"),
  "Hugepagesize": ("memory_hugepages_size_bytes", "bytes"),
  "DirectMap4k": ("memory_direct_map_4k_bytes", "bytes"),
  "DirectMap2M": ("memory_direct_map_2m_bytes", "bytes"),
  "DirectMap1G": ("memory_direct_map_1g_bytes", "bytes"),
}
TCP_STATES = {
  "01": "established",
  "02": "syn_sent",
  "03": "syn_received",
  "04": "fin_wait1",
  "05": "fin_wait2",
  "06": "time_wait",
  "07": "close",
  "08": "close_wait",
  "09": "last_ack",
  "0A": "listen",
  "0B": "closing",
  "0C": "new_syn_received",
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

  def _read_optional(self, name):
    try:
      return self._read(name)
    except OSError:
      return None


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
    for source_name, (metric_name, unit) in MEMORY_FIELDS.items():
      if source_name not in values:
        continue
      family = MetricFamily(
        f"{NAMESPACE}_{metric_name}",
        f"Linux {source_name} memory value in {unit}.",
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
      if len(fields) >= 2 and fields[0] in (
        "btime",
        "ctxt",
        "intr",
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
      (
        "system_context_switches_total",
        "Total number of context switches since boot.",
        "counter",
        stat_values.get("ctxt", 0),
      ),
      (
        "system_interrupts_total",
        "Total number of interrupts serviced since boot.",
        "counter",
        stat_values.get("intr", 0),
      ),
    )

    families = []
    for name, help_text, metric_type, value in metrics:
      family = MetricFamily(f"{NAMESPACE}_{name}", help_text, metric_type)
      family.add_sample(value)
      families.append(family)

    optional_metrics = (
      (
        "file_descriptors_allocated",
        "Number of allocated file descriptors.",
        "gauge",
        self._first_int("sys/fs/file-nr"),
      ),
      (
        "file_descriptors_maximum",
        "System-wide maximum number of file descriptors.",
        "gauge",
        self._first_int("sys/fs/file-max"),
      ),
      (
        "entropy_available_bits",
        "Available kernel entropy in bits.",
        "gauge",
        self._first_int("sys/kernel/random/entropy_avail"),
      ),
      (
        "oom_kills_total",
        "Total number of processes killed by the out-of-memory killer.",
        "counter",
        self._vmstat_value("oom_kill"),
      ),
      (
        "conntrack_entries",
        "Number of currently tracked network connections.",
        "gauge",
        self._first_int("sys/net/netfilter/nf_conntrack_count"),
      ),
      (
        "conntrack_entries_limit",
        "Maximum number of tracked network connections.",
        "gauge",
        self._first_int("sys/net/netfilter/nf_conntrack_max"),
      ),
    )
    for name, help_text, metric_type, value in optional_metrics:
      if value is None:
        continue
      family = MetricFamily(f"{NAMESPACE}_{name}", help_text, metric_type)
      family.add_sample(value)
      families.append(family)

    tcp_connections = self._tcp_connections()
    if tcp_connections is not None:
      family = MetricFamily(
        f"{NAMESPACE}_tcp_connections",
        "Number of IPv4 and IPv6 TCP connections by state.",
        "gauge",
      )
      for state in TCP_STATES.values():
        family.add_sample(tcp_connections[state], {"state": state})
      families.append(family)

    info = MetricFamily(
      f"{NAMESPACE}_host_info",
      "Static information about the host running Ambari Agent.",
      "gauge",
    )
    info.add_sample(1, {"hostname": socket.getfqdn()})
    families.append(info)
    return families

  def _first_int(self, name):
    content = self._read_optional(name)
    if content is None:
      return None
    try:
      return int(content.split()[0])
    except (IndexError, ValueError):
      return None

  def _vmstat_value(self, key):
    content = self._read_optional("vmstat")
    if content is None:
      return None
    for line in content.splitlines():
      fields = line.split()
      if len(fields) == 2 and fields[0] == key:
        try:
          return int(fields[1])
        except ValueError:
          return None
    return None

  def _tcp_connections(self):
    counts = {state: 0 for state in TCP_STATES.values()}
    source_available = False
    for name in ("net/tcp", "net/tcp6"):
      content = self._read_optional(name)
      if content is None:
        continue
      source_available = True
      for line in content.splitlines()[1:]:
        fields = line.split()
        if len(fields) < 4:
          continue
        state = TCP_STATES.get(fields[3].upper())
        if state is not None:
          counts[state] += 1
    return counts if source_available else None


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
    readonly = _filesystem_family(
      "readonly", "Whether the filesystem is mounted read-only."
    )
    device_error = _filesystem_family(
      "device_error", "Whether an error occurred while reading filesystem statistics."
    )

    for line in self._read("self/mounts").splitlines():
      fields = line.split()
      if len(fields) < 3 or fields[2] in PSEUDO_FILESYSTEMS:
        continue

      device = _decode_mount_field(fields[0])
      mountpoint = _decode_mount_field(fields[1])
      filesystem_type = fields[2]
      labels = {
        "device": device,
        "fstype": filesystem_type,
        "mountpoint": mountpoint,
      }
      try:
        stats = self.statvfs(mountpoint)
      except OSError:
        logger.warning('Unable to read filesystem metrics for "%s"', mountpoint)
        device_error.add_sample(1, labels)
        continue
      device_error.add_sample(0, labels)
      size.add_sample(stats.f_blocks * stats.f_frsize, labels)
      free.add_sample(stats.f_bfree * stats.f_frsize, labels)
      available.add_sample(stats.f_bavail * stats.f_frsize, labels)
      files.add_sample(stats.f_files, labels)
      files_free.add_sample(stats.f_ffree, labels)
      readonly.add_sample(1 if getattr(stats, "f_flag", 0) & os.ST_RDONLY else 0, labels)

    return [size, free, available, files, files_free, readonly, device_error]


class DiskIoCollector(LinuxCollector):
  name = "disk_io"

  def collect(self):
    definitions = (
      ("disk_reads_completed_total", "Completed disk read operations.", "counter"),
      ("disk_reads_merged_total", "Merged disk read operations.", "counter"),
      ("disk_read_bytes_total", "Bytes read from disk.", "counter"),
      ("disk_read_time_seconds_total", "Seconds spent reading from disk.", "counter"),
      ("disk_writes_completed_total", "Completed disk write operations.", "counter"),
      ("disk_writes_merged_total", "Merged disk write operations.", "counter"),
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
        int(fields[4]),
        int(fields[5]) * 512,
        int(fields[6]) / 1000.0,
        int(fields[7]),
        int(fields[8]),
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


class SocketCollector(LinuxCollector):
  name = "sockets"

  def collect(self):
    values = {}
    for source in ("net/sockstat", "net/sockstat6"):
      content = self._read_optional(source)
      if content is None:
        continue
      for line in content.splitlines():
        if ":" not in line:
          continue
        protocol, raw_values = line.split(":", 1)
        fields = raw_values.split()
        for index in range(0, len(fields) - 1, 2):
          try:
            value = int(fields[index + 1])
          except ValueError:
            continue
          key = (protocol, fields[index])
          values[key] = values.get(key, 0) + value

    definitions = (
      ("socket_tcp_allocated", "TCP", "alloc", "Allocated TCP sockets."),
      ("socket_tcp_in_use", "TCP", "inuse", "TCP sockets currently in use."),
      ("socket_tcp_orphaned", "TCP", "orphan", "Orphaned TCP sockets."),
      ("socket_tcp_time_wait", "TCP", "tw", "TCP sockets in the TIME_WAIT state."),
      ("sockets_used", "sockets", "used", "Sockets currently in use."),
    )
    families = []
    for metric_name, protocol, field, help_text in definitions:
      value = values.get((protocol, field))
      if value is None:
        continue
      family = MetricFamily(f"{NAMESPACE}_{metric_name}", help_text, "gauge")
      family.add_sample(value)
      families.append(family)
    return families


class _Timeval(ctypes.Structure):
  _fields_ = [("tv_sec", ctypes.c_long), ("tv_usec", ctypes.c_long)]


class _Timex(ctypes.Structure):
  _fields_ = [
    ("modes", ctypes.c_uint),
    ("offset", ctypes.c_long),
    ("freq", ctypes.c_long),
    ("maxerror", ctypes.c_long),
    ("esterror", ctypes.c_long),
    ("status", ctypes.c_int),
    ("constant", ctypes.c_long),
    ("precision", ctypes.c_long),
    ("tolerance", ctypes.c_long),
    ("time", _Timeval),
    ("tick", ctypes.c_long),
    ("ppsfreq", ctypes.c_long),
    ("jitter", ctypes.c_long),
    ("shift", ctypes.c_int),
    ("stabil", ctypes.c_long),
    ("jitcnt", ctypes.c_long),
    ("calcnt", ctypes.c_long),
    ("errcnt", ctypes.c_long),
    ("stbcnt", ctypes.c_long),
    ("tai", ctypes.c_int),
    ("reserved", ctypes.c_int * 11),
  ]


class TimexCollector:
  name = "timex"
  STA_NANO = 0x2000

  def __init__(self, read_timex=None):
    self.read_timex = read_timex or self._read_timex

  def collect(self):
    family = MetricFamily(
      f"{NAMESPACE}_timex_offset_seconds",
      "Time offset between the system clock and the kernel clock discipline source.",
      "gauge",
    )
    family.add_sample(self.read_timex())
    return [family]

  def _read_timex(self):
    libc = ctypes.CDLL(None, use_errno=True)
    adjtimex = libc.adjtimex
    adjtimex.argtypes = [ctypes.POINTER(_Timex)]
    adjtimex.restype = ctypes.c_int
    value = _Timex()
    if adjtimex(ctypes.byref(value)) < 0:
      error = ctypes.get_errno()
      raise OSError(error, os.strerror(error))
    scale = 1e-9 if value.status & self.STA_NANO else 1e-6
    return value.offset * scale


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
    threads = 0

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
        stat_fields = stat[command_end + 2 :].split()
        if not stat_fields:
          counts["unknown"] += 1
        else:
          state = stat_fields[0]
          counts[self.PROCESS_STATES.get(state, "unknown")] += 1
        if len(stat_fields) > 17:
          try:
            threads += int(stat_fields[17])
          except ValueError:
            pass
      counts["total"] += 1

    family = MetricFamily(
      f"{NAMESPACE}_processes",
      "Number of processes in each Linux process state.",
      "gauge",
    )
    for state in sorted(counts):
      family.add_sample(counts[state], {"state": state})
    thread_family = MetricFamily(
      f"{NAMESPACE}_process_threads",
      "Total number of threads across processes visible to Ambari Agent.",
      "gauge",
    )
    thread_family.add_sample(threads)
    return [family, thread_family]


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
    SocketCollector(proc_root),
    ProcessCollector(proc_root),
    TimexCollector(),
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
