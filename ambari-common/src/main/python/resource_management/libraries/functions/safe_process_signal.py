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

import argparse
import errno
import json
import os
import signal
import sys


def _read_identity(pid):
  process_dir = f"/proc/{pid}"
  try:
    uid = os.stat(process_dir).st_uid
    with open(f"{process_dir}/stat", "rb") as stat_file:
      stat_value = stat_file.read().decode("ascii")
    with open(f"{process_dir}/cmdline", "rb") as cmdline_file:
      cmdline_value = cmdline_file.read()
  except FileNotFoundError:
    return None

  command_end = stat_value.rfind(")")
  stat_fields = stat_value[command_end + 1 :].split() if command_end >= 0 else []
  if len(stat_fields) <= 19:
    raise RuntimeError(f"Process status for pid {pid} is malformed")
  process_state = stat_fields[0]
  if process_state in ("Z", "X"):
    return None
  start_time = int(stat_fields[19])
  argv = tuple(
    item.decode("utf-8", errors="surrogateescape")
    for item in cmdline_value.split(b"\0")
    if item
  )
  return uid, start_time, argv, process_state


def _validate_identity(pid, expected_uid, expected_start_time, expected_tokens):
  identity = _read_identity(pid)
  if identity is None:
    return None
  uid, start_time, argv, _ = identity
  if uid != expected_uid:
    raise RuntimeError(f"Refusing pid {pid}: process owner changed")
  if start_time != expected_start_time:
    raise RuntimeError(f"Refusing pid {pid}: PID was reused")
  if any(token not in argv for token in expected_tokens):
    raise RuntimeError(f"Refusing pid {pid}: process command changed")
  return identity


def _pidfd_supported():
  return callable(getattr(os, "pidfd_open", None)) and callable(
    getattr(signal, "pidfd_send_signal", None)
  )


def _signal_process(pid, expected_uid, expected_start_time, expected_tokens, sig):
  if _validate_identity(pid, expected_uid, expected_start_time, expected_tokens) is None:
    return "gone"

  if _pidfd_supported():
    try:
      pid_fd = os.pidfd_open(pid, 0)
    except OSError as error:
      if error.errno not in (errno.ENOSYS, errno.EINVAL):
        if not os.path.exists(f"/proc/{pid}"):
          return "gone"
        raise
    else:
      try:
        rechecked = _validate_identity(
          pid, expected_uid, expected_start_time, expected_tokens
        )
        if rechecked is None:
          return "gone"
        try:
          signal.pidfd_send_signal(pid_fd, sig, None, 0)
        except ProcessLookupError:
          return "gone"
        return "signaled-pidfd"
      finally:
        os.close(pid_fd)

  # Linux kernels without pidfd_open cannot pin an existing process. Keep the
  # compatibility path in one privileged process and minimize the check/signal gap.
  if _validate_identity(pid, expected_uid, expected_start_time, expected_tokens) is None:
    return "gone"
  try:
    os.kill(pid, sig)
  except ProcessLookupError:
    return "gone"
  return "signaled-legacy"


def _parse_args(argv):
  parser = argparse.ArgumentParser(
    description="Safely signal an Ambari service process"
  )
  parser.add_argument("--pid", required=True, type=int)
  parser.add_argument("--uid", required=True, type=int)
  parser.add_argument("--start-time", required=True, type=int)
  parser.add_argument(
    "--signal",
    required=True,
    type=int,
    choices=(signal.SIGTERM, signal.SIGKILL),
  )
  parser.add_argument("--tokens-json", required=True)
  args = parser.parse_args(argv)
  tokens = json.loads(args.tokens_json)
  if not isinstance(tokens, list) or not tokens or any(
    not isinstance(token, str) or not token for token in tokens
  ):
    parser.error("--tokens-json must contain a non-empty JSON string array")
  if args.pid <= 0 or args.uid < 0 or args.start_time <= 0:
    parser.error("pid, uid, and start time must be positive")
  args.tokens = tuple(tokens)
  return args


def main(argv=None):
  if os.geteuid() != 0:
    print("Safe process signaling helper must run as root", file=sys.stderr)
    return 1
  args = _parse_args(sys.argv[1:] if argv is None else argv)
  try:
    result = _signal_process(
      args.pid,
      args.uid,
      args.start_time,
      args.tokens,
      args.signal,
    )
  except Exception as error:
    print(str(error), file=sys.stderr)
    return 1
  print(result)
  return 0


if __name__ == "__main__":
  sys.exit(main())
