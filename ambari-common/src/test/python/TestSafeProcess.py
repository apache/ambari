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

import signal
import unittest
from types import SimpleNamespace
from unittest.mock import call, mock_open, patch

from resource_management.core.exceptions import ComponentIsNotRunning, Fail
from resource_management.libraries.functions.check_process_status import (
  check_process_status,
)
from resource_management.libraries.functions import safe_process
from resource_management.libraries.functions import safe_process_signal


def process_stat(pid, start_time, state="S"):
  fields_after_command = [state] + ["0"] * 18 + [str(start_time)]
  return f"{pid} (java process) " + " ".join(fields_after_command)


class TestSafeProcess(unittest.TestCase):
  PID_FILE = "/run/service/service.pid"
  PID = 123
  USER = "service"
  CMDLINE = b"/usr/bin/java\0org.example.Service\0--daemon\0"
  FILE_STAT = SimpleNamespace(st_dev=10, st_ino=20, st_mode=0o100640)

  def _read_file(self, path, start_time=456):
    if path == self.PID_FILE:
      return f"{self.PID}\n".encode("ascii")
    if path == f"/proc/{self.PID}/stat":
      return process_stat(self.PID, start_time).encode("ascii")
    if path == f"/proc/{self.PID}/cmdline":
      return self.CMDLINE
    raise AssertionError(f"Unexpected read: {path}")

  def _process_patches(self, start_time=456):
    return (
      patch.object(safe_process.sudo, "path_islink", return_value=False),
      patch.object(safe_process.sudo, "path_exists", return_value=True),
      patch.object(safe_process.sudo, "path_isfile", return_value=True),
      patch.object(
        safe_process.sudo,
        "read_file",
        side_effect=lambda path: self._read_file(path, start_time),
      ),
      patch.object(
        safe_process.sudo, "stat", return_value=SimpleNamespace(st_uid=1001)
      ),
      patch.object(
        safe_process.pwd,
        "getpwnam",
        return_value=SimpleNamespace(pw_uid=1001),
      ),
      patch.object(safe_process.sudo, "kill", return_value=None),
    )

  def test_reads_and_validates_process_identity(self):
    path_islink, path_exists, path_isfile, read_file, stat, getpwnam, kill = (
      self._process_patches()
    )
    with path_islink, path_exists, path_isfile, read_file, stat, getpwnam, kill:
      identity = safe_process.read_running_process(
        self.PID_FILE, self.USER, ("org.example.Service", "--daemon")
      )

    self.assertEqual(self.PID, identity.pid)
    self.assertEqual(1001, identity.uid)
    self.assertEqual(456, identity.start_time)
    self.assertEqual("S", identity.state)
    self.assertEqual("/usr/bin/java org.example.Service --daemon", identity.cmdline)
    kill.assert_called_once_with(self.PID, 0)

  def test_rejects_invalid_pid_file_without_using_value(self):
    invalid_values = (b"0", b"-1", b"1 2", b"1; touch /tmp/injected", b"\xff")
    for value in invalid_values:
      with self.subTest(value=value):
        with (
          patch.object(safe_process.sudo, "path_exists", return_value=True),
          patch.object(safe_process.sudo, "path_islink", return_value=False),
          patch.object(safe_process.sudo, "path_isfile", return_value=True),
          patch.object(safe_process.sudo, "read_file", return_value=value),
          patch.object(safe_process.sudo, "kill") as kill,
        ):
          with self.assertRaises(Fail):
            safe_process.read_running_process(
              self.PID_FILE, self.USER, "org.example.Service"
            )
        kill.assert_not_called()

  def test_rejects_wrong_process_owner(self):
    patches = self._process_patches()
    with (
      patches[0],
      patches[1],
      patches[2],
      patches[3],
      patch.object(
        safe_process.sudo, "stat", return_value=SimpleNamespace(st_uid=1002)
      ),
      patches[5],
      patches[6] as kill,
    ):
      with self.assertRaisesRegex(Fail, "owner does not match"):
        safe_process.read_running_process(
          self.PID_FILE, self.USER, "org.example.Service"
        )
    kill.assert_not_called()

  def test_rejects_wrong_process_command(self):
    (
      path_islink,
      path_exists,
      path_isfile,
      read_file,
      stat,
      getpwnam,
      kill_patcher,
    ) = self._process_patches()
    with (
      path_islink,
      path_exists,
      path_isfile,
      read_file,
      stat,
      getpwnam,
      kill_patcher as kill,
    ):
      with self.assertRaisesRegex(Fail, "command line does not match"):
        safe_process.read_running_process(
          self.PID_FILE, self.USER, "org.example.OtherService"
        )
    kill.assert_not_called()

  def test_pid_reuse_is_not_reported_as_original_process(self):
    identity = safe_process.ProcessIdentity(
      self.PID, 1001, 456, ("/usr/bin/java", "org.example.Service")
    )
    replacement = safe_process.ProcessIdentity(
      self.PID, 1001, 789, ("/usr/bin/java", "org.example.Service")
    )
    with patch.object(
      safe_process, "inspect_process", return_value=replacement
    ), patch.object(safe_process.sudo, "kill") as kill:
      self.assertFalse(
        safe_process.is_process_running(
          self.PID, self.USER, "org.example.Service", identity=identity
        )
      )
    kill.assert_not_called()

  def test_terminate_uses_term_when_process_exits(self):
    identity = safe_process.ProcessIdentity(
      self.PID, 1001, 456, ("/usr/bin/java", "org.example.Service")
    )
    with (
      patch.object(safe_process, "inspect_process", return_value=identity),
      patch.object(
        safe_process.sudo, "signal_process", return_value="signaled-pidfd"
      ) as send_signal,
      patch.object(safe_process, "wait_for_process_stopped", return_value=True),
    ):
      safe_process.terminate_process(identity, self.USER, "org.example.Service")

    send_signal.assert_called_once_with(
      self.PID,
      1001,
      456,
      ("org.example.Service",),
      signal.SIGTERM.value,
    )

  def test_terminate_escalates_to_kill_and_confirms_exit(self):
    identity = safe_process.ProcessIdentity(
      self.PID, 1001, 456, ("/usr/bin/java", "org.example.Service")
    )
    with (
      patch.object(safe_process, "inspect_process", return_value=identity),
      patch.object(
        safe_process.sudo,
        "signal_process",
        side_effect=("signaled-pidfd", "signaled-pidfd"),
      ) as send_signal,
      patch.object(
        safe_process, "wait_for_process_stopped", side_effect=(False, True)
      ),
    ):
      safe_process.terminate_process(identity, self.USER, "org.example.Service")

    self.assertEqual(
      [
        call(
          self.PID,
          1001,
          456,
          ("org.example.Service",),
          signal.SIGTERM.value,
        ),
        call(
          self.PID,
          1001,
          456,
          ("org.example.Service",),
          signal.SIGKILL.value,
        ),
      ],
      send_signal.call_args_list,
    )

  def test_terminate_fails_when_process_survives_kill(self):
    identity = safe_process.ProcessIdentity(
      self.PID, 1001, 456, ("/usr/bin/java", "org.example.Service")
    )
    with (
      patch.object(safe_process, "inspect_process", return_value=identity),
      patch.object(
        safe_process.sudo, "signal_process", return_value="signaled-pidfd"
      ),
      patch.object(safe_process, "wait_for_process_stopped", return_value=False),
    ):
      with self.assertRaisesRegex(Fail, "did not stop"):
        safe_process.terminate_process(identity, self.USER, "org.example.Service")

  def test_terminate_rejects_reused_pid_without_sending_signal(self):
    identity = safe_process.ProcessIdentity(
      self.PID, 1001, 456, ("/usr/bin/java", "org.example.Service")
    )
    replacement = safe_process.ProcessIdentity(
      self.PID, 1001, 789, ("/usr/bin/java", "org.example.Service")
    )
    with (
      patch.object(safe_process, "inspect_process", return_value=replacement),
      patch.object(safe_process.sudo, "signal_process") as send_signal,
    ):
      with self.assertRaisesRegex(Fail, "PID was reused"):
        safe_process.terminate_process(
          identity, self.USER, "org.example.Service"
        )
    send_signal.assert_not_called()

  def test_sudo_signal_helper_uses_privileged_command_and_structured_tokens(self):
    with patch.object(
      safe_process.sudo.shell,
      "checked_call",
      return_value=(0, "signaled-pidfd"),
    ) as checked_call:
      result = safe_process.sudo.signal_process(
        self.PID,
        1001,
        456,
        ("-Dproc_service", "ServiceMain"),
        signal.SIGTERM.value,
      )

    self.assertEqual("signaled-pidfd", result)
    command = checked_call.call_args.args[0]
    self.assertEqual(safe_process.sudo.sys.executable, command[0])
    self.assertIn("safe_process_signal.py", command[1])
    self.assertEqual(
      '["-Dproc_service", "ServiceMain"]', command[-1]
    )
    self.assertTrue(checked_call.call_args.kwargs["sudo"])

  def test_privileged_signal_helper_uses_pidfd_when_available(self):
    identity = (1001, 456, ("/usr/bin/java", "ServiceMain"), "S")
    with (
      patch.object(
        safe_process_signal, "_validate_identity", return_value=identity
      ),
      patch.object(safe_process_signal, "_pidfd_supported", return_value=True),
      patch.object(safe_process_signal.os, "pidfd_open", return_value=11),
      patch.object(safe_process_signal.os, "close") as close,
      patch.object(
        safe_process_signal.signal, "pidfd_send_signal"
      ) as send_signal,
      patch.object(safe_process_signal.os, "kill") as legacy_kill,
    ):
      result = safe_process_signal._signal_process(
        self.PID, 1001, 456, ("ServiceMain",), signal.SIGTERM
      )

    self.assertEqual("signaled-pidfd", result)
    send_signal.assert_called_once_with(11, signal.SIGTERM, None, 0)
    close.assert_called_once_with(11)
    legacy_kill.assert_not_called()

  def test_privileged_signal_helper_keeps_legacy_kernel_compatibility(self):
    identity = (1001, 456, ("/usr/bin/java", "ServiceMain"), "S")
    with (
      patch.object(
        safe_process_signal, "_validate_identity", return_value=identity
      ) as validate,
      patch.object(safe_process_signal, "_pidfd_supported", return_value=False),
      patch.object(safe_process_signal.os, "kill") as legacy_kill,
    ):
      result = safe_process_signal._signal_process(
        self.PID, 1001, 456, ("ServiceMain",), signal.SIGTERM
      )

    self.assertEqual("signaled-legacy", result)
    self.assertEqual(2, validate.call_count)
    legacy_kill.assert_called_once_with(self.PID, signal.SIGTERM)

  def test_privileged_signal_helper_treats_zombie_as_gone(self):
    stat_handle = mock_open(
      read_data=process_stat(self.PID, 456, state="Z").encode("ascii")
    ).return_value
    cmdline_handle = mock_open(read_data=b"").return_value
    with (
      patch.object(
        safe_process_signal.os,
        "stat",
        return_value=SimpleNamespace(st_uid=1001),
      ),
      patch("builtins.open", side_effect=(stat_handle, cmdline_handle)),
      patch.object(safe_process_signal.os, "kill") as legacy_kill,
    ):
      result = safe_process_signal._signal_process(
        self.PID, 1001, 456, ("ServiceMain",), signal.SIGTERM
      )

    self.assertEqual("gone", result)
    legacy_kill.assert_not_called()

  def test_privileged_signal_helper_rejects_identity_change(self):
    with (
      patch.object(
        safe_process_signal,
        "_validate_identity",
        side_effect=RuntimeError("PID was reused"),
      ),
      patch.object(safe_process_signal.os, "kill") as legacy_kill,
    ):
      with self.assertRaisesRegex(RuntimeError, "PID was reused"):
        safe_process_signal._signal_process(
          self.PID, 1001, 456, ("ServiceMain",), signal.SIGTERM
        )
    legacy_kill.assert_not_called()

  def test_wait_for_stop_rejects_reused_pid(self):
    identity = safe_process.ProcessIdentity(
      self.PID, 1001, 456, ("/usr/bin/java", "org.example.Service")
    )
    replacement = safe_process.ProcessIdentity(
      self.PID, 1001, 789, ("/usr/bin/java", "org.example.Service")
    )
    with (
      patch.object(safe_process, "inspect_process", return_value=replacement),
      patch.object(safe_process, "is_process_running") as is_running,
    ):
      with self.assertRaisesRegex(Fail, "PID was reused"):
        safe_process.wait_for_process_stopped(
          identity, self.USER, "org.example.Service", 2, 0
        )
    is_running.assert_not_called()

  def test_strict_wait_treats_zombie_as_stopped(self):
    identity = safe_process.ProcessIdentity(
      self.PID, 1001, 456, ("/usr/bin/java", "org.example.Service")
    )
    zombie = safe_process.ProcessIdentity(self.PID, 1001, 456, (), "Z")
    with (
      patch.object(safe_process, "_read_process_identity", return_value=zombie),
      patch.object(safe_process.sudo, "kill") as kill,
    ):
      self.assertTrue(
        safe_process.wait_for_process_stopped(
          identity, self.USER, "org.example.Service", 2, 0
        )
      )
    kill.assert_not_called()

  def test_legacy_status_treats_zombie_as_not_running(self):
    zombie = safe_process.ProcessIdentity(self.PID, 1001, 456, (), "Z")
    with (
      patch.object(safe_process, "read_pid", return_value=self.PID),
      patch.object(safe_process, "_read_process_identity", return_value=zombie),
    ):
      with self.assertRaises(ComponentIsNotRunning):
        check_process_status(self.PID_FILE)

  def test_running_check_treats_nonroot_kill_failure_as_exit_when_proc_is_gone(self):
    identity = safe_process.ProcessIdentity(
      self.PID, 1001, 456, ("/usr/bin/java", "org.example.Service")
    )
    with (
      patch.object(safe_process, "inspect_process", return_value=identity),
      patch.object(safe_process.sudo, "kill", side_effect=OSError("kill failed")),
      patch.object(safe_process.sudo, "path_exists", return_value=False),
    ):
      self.assertFalse(
        safe_process.is_process_running(
          self.PID, self.USER, "org.example.Service", identity=identity
        )
      )

  def test_pid_file_change_blocks_cleanup(self):
    with patch.object(safe_process, "read_pid", return_value=999), patch.object(
      safe_process.sudo, "unlink"
    ) as unlink:
      with self.assertRaisesRegex(Fail, "pid changed"):
        safe_process.remove_pid_file_if_stopped(
          self.PID_FILE, self.PID, self.USER, "org.example.Service"
        )
    unlink.assert_not_called()

  def test_cleanup_refuses_running_process(self):
    with patch.object(
      safe_process, "read_pid", return_value=self.PID
    ), patch.object(
      safe_process, "is_process_running", return_value=True
    ), patch.object(safe_process.sudo, "unlink") as unlink:
      with self.assertRaisesRegex(Fail, "running process"):
        safe_process.remove_pid_file_if_stopped(
          self.PID_FILE, self.PID, self.USER, "org.example.Service"
        )
    unlink.assert_not_called()

  def test_rejects_symbolic_link_pid_file(self):
    with patch.object(
      safe_process.sudo, "path_islink", return_value=True
    ), patch.object(safe_process.sudo, "read_file") as read_file:
      with self.assertRaisesRegex(Fail, "symbolic link"):
        safe_process.read_pid(self.PID_FILE)
    read_file.assert_not_called()

  def test_rejects_directory_pid_file(self):
    with (
      patch.object(safe_process.sudo, "path_islink", return_value=False),
      patch.object(safe_process.sudo, "path_exists", return_value=True),
      patch.object(safe_process.sudo, "path_isfile", return_value=False),
      patch.object(safe_process.sudo, "read_file") as read_file,
    ):
      with self.assertRaisesRegex(Fail, "not a regular file"):
        safe_process.read_pid(self.PID_FILE)
    read_file.assert_not_called()

  def test_rejects_empty_and_non_text_pid_file(self):
    for value in (b"", object()):
      with self.subTest(value=value):
        with (
          patch.object(safe_process.sudo, "path_islink", return_value=False),
          patch.object(safe_process.sudo, "path_exists", return_value=True),
          patch.object(safe_process.sudo, "path_isfile", return_value=True),
          patch.object(safe_process.sudo, "read_file", return_value=value),
        ):
          with self.assertRaises(Fail):
            safe_process.read_pid(self.PID_FILE)

  def test_pid_file_read_failure_is_not_treated_as_missing(self):
    with (
      patch.object(safe_process.sudo, "path_islink", return_value=False),
      patch.object(safe_process.sudo, "path_exists", return_value=True),
      patch.object(safe_process.sudo, "path_isfile", return_value=True),
      patch.object(
        safe_process.sudo, "read_file", side_effect=OSError("permission denied")
      ),
    ):
      with self.assertRaisesRegex(Fail, "Could not read PID file"):
        safe_process.read_pid(self.PID_FILE)

  def test_command_token_prefix_and_suffix_do_not_match(self):
    for actual_token in ("org.example.ServiceEvil", "prefix.org.example.Service"):
      with self.subTest(actual_token=actual_token):
        identity = safe_process.ProcessIdentity(
          self.PID, 1001, 456, ("/usr/bin/java", actual_token)
        )
        with patch.object(
          safe_process, "_read_process_identity", return_value=identity
        ), patch.object(
          safe_process.pwd,
          "getpwnam",
          return_value=SimpleNamespace(pw_uid=1001),
        ):
          with self.assertRaisesRegex(Fail, "command line does not match"):
            safe_process.inspect_process(
              self.PID, self.USER, "org.example.Service"
            )

  def test_pid_file_replacement_before_unlink_blocks_cleanup(self):
    with (
      patch.object(safe_process, "read_pid", side_effect=(self.PID, 999)),
      patch.object(safe_process, "is_process_running", return_value=False),
      patch.object(safe_process.sudo, "unlink") as unlink,
    ):
      with self.assertRaisesRegex(Fail, "before cleanup"):
        safe_process.remove_pid_file_if_stopped(
          self.PID_FILE, self.PID, self.USER, "org.example.Service"
        )
    unlink.assert_not_called()

  def test_process_discovery_returns_unique_exact_match(self):
    matching = safe_process.ProcessIdentity(
      123, 1001, 456, ("/usr/bin/java", "-Dproc_service", "ServiceMain")
    )
    other_user = safe_process.ProcessIdentity(
      124, 1002, 457, ("/usr/bin/java", "-Dproc_service", "ServiceMain")
    )
    with (
      patch.object(safe_process.sudo, "listdir", return_value=("self", "123", "124")),
      patch.object(
        safe_process,
        "_read_process_identity",
        side_effect=lambda pid: {123: matching, 124: other_user}[pid],
      ),
      patch.object(
        safe_process.pwd,
        "getpwnam",
        return_value=SimpleNamespace(pw_uid=1001),
      ),
      patch.object(safe_process, "is_process_running", return_value=True),
    ):
      discovered = safe_process.discover_running_process(
        self.USER, ("-Dproc_service", "ServiceMain")
      )
    self.assertIs(matching, discovered)

  def test_process_discovery_returns_none_without_match(self):
    with (
      patch.object(safe_process.sudo, "listdir", return_value=("self",)),
      patch.object(
        safe_process.pwd,
        "getpwnam",
        return_value=SimpleNamespace(pw_uid=1001),
      ),
    ):
      self.assertIsNone(
        safe_process.discover_running_process(
          self.USER, ("-Dproc_service", "ServiceMain")
        )
      )

  def test_process_discovery_rejects_multiple_matches(self):
    identities = {
      pid: safe_process.ProcessIdentity(
        pid, 1001, 450 + pid, ("/usr/bin/java", "-Dproc_service", "ServiceMain")
      )
      for pid in (123, 124)
    }
    with (
      patch.object(safe_process.sudo, "listdir", return_value=("123", "124")),
      patch.object(
        safe_process,
        "_read_process_identity",
        side_effect=lambda pid: identities[pid],
      ),
      patch.object(
        safe_process.pwd,
        "getpwnam",
        return_value=SimpleNamespace(pw_uid=1001),
      ),
      patch.object(safe_process, "is_process_running", return_value=True),
    ):
      with self.assertRaisesRegex(Fail, "ambiguous process discovery"):
        safe_process.discover_running_process(
          self.USER, ("-Dproc_service", "ServiceMain")
        )

  def test_process_discovery_ignores_process_that_changes_during_recheck(self):
    identity = safe_process.ProcessIdentity(
      123, 1001, 456, ("/usr/bin/java", "-Dproc_service", "ServiceMain")
    )
    with (
      patch.object(safe_process.sudo, "listdir", return_value=("123",)),
      patch.object(safe_process, "_read_process_identity", return_value=identity),
      patch.object(
        safe_process.pwd,
        "getpwnam",
        return_value=SimpleNamespace(pw_uid=1001),
      ),
      patch.object(safe_process, "is_process_running", return_value=False),
    ):
      self.assertIsNone(
        safe_process.discover_running_process(
          self.USER, ("-Dproc_service", "ServiceMain")
        )
      )

  def test_wait_for_discovered_process_retries_until_process_appears(self):
    identity = safe_process.ProcessIdentity(
      self.PID, 1001, 456, ("/usr/bin/java", "ServiceMain")
    )
    with (
      patch.object(
        safe_process,
        "discover_running_process",
        side_effect=(None, None, identity),
      ) as discover,
      patch.object(safe_process.time, "sleep") as sleep,
    ):
      result = safe_process.wait_for_discovered_process(
        self.USER, "ServiceMain", attempts=3, sleep_seconds=2
      )

    self.assertIs(identity, result)
    self.assertEqual(3, discover.call_count)
    self.assertEqual([call(2), call(2)], sleep.call_args_list)

  def test_wait_for_discovered_process_propagates_validation_failure(self):
    with (
      patch.object(
        safe_process,
        "discover_running_process",
        side_effect=Fail("ambiguous process discovery"),
      ),
      patch.object(safe_process.time, "sleep") as sleep,
    ):
      with self.assertRaisesRegex(Fail, "ambiguous process discovery"):
        safe_process.wait_for_discovered_process(
          self.USER, "ServiceMain", attempts=3
        )
    sleep.assert_not_called()

  def test_wait_for_discovered_process_fails_after_timeout(self):
    with (
      patch.object(safe_process, "discover_running_process", return_value=None),
      patch.object(safe_process.time, "sleep") as sleep,
    ):
      with self.assertRaisesRegex(Fail, "was not discovered"):
        safe_process.wait_for_discovered_process(
          self.USER, "ServiceMain", attempts=2, sleep_seconds=3
        )
    sleep.assert_called_once_with(3)

  def test_create_pid_file_preserves_identity_and_file_attributes(self):
    identity = safe_process.ProcessIdentity(
      self.PID, 1001, 456, ("/usr/bin/java", "ServiceMain")
    )

    def path_exists(path):
      return path != self.PID_FILE

    with (
      patch.object(safe_process.sudo, "path_islink", return_value=False),
      patch.object(safe_process.sudo, "path_exists", side_effect=path_exists),
      patch.object(safe_process, "is_process_running", return_value=True),
      patch.object(safe_process, "File") as pid_file,
      patch.object(safe_process.sudo, "lstat", return_value=self.FILE_STAT),
      patch.object(safe_process.sudo, "link_exclusive") as link_exclusive,
      patch.object(safe_process.sudo, "unlink") as unlink,
      patch.object(
        safe_process, "read_running_process", return_value=identity
      ),
    ):
      result = safe_process.create_pid_file_for_identity(
        self.PID_FILE,
        identity,
        self.USER,
        "ServiceMain",
        owner=self.USER,
        group="service-group",
        mode=0o640,
      )

    self.assertIs(identity, result)
    temp_pid_file = pid_file.call_args.args[0]
    self.assertEqual("/run/service", safe_process.os.path.dirname(temp_pid_file))
    pid_file.assert_called_once_with(
      temp_pid_file,
      content=f"{self.PID}\n",
      owner=self.USER,
      group="service-group",
      mode=0o640,
      replace=False,
    )
    link_exclusive.assert_called_once_with(temp_pid_file, self.PID_FILE)
    unlink.assert_called_once_with(temp_pid_file)

  def test_create_pid_file_rejects_existing_or_broken_symlink(self):
    identity = safe_process.ProcessIdentity(self.PID, 1001, 456, ())
    with (
      patch.object(safe_process.sudo, "path_islink", return_value=True),
      patch.object(safe_process.sudo, "path_exists", return_value=False),
      patch.object(safe_process, "File") as pid_file,
    ):
      with self.assertRaisesRegex(Fail, "existing PID file"):
        safe_process.create_pid_file_for_identity(
          self.PID_FILE,
          identity,
          self.USER,
          "ServiceMain",
          self.USER,
          "service-group",
        )
    pid_file.assert_not_called()

  def test_create_pid_file_rejects_concurrent_destination(self):
    identity = safe_process.ProcessIdentity(self.PID, 1001, 456, ())
    destination_checks = iter((False, True))

    def path_exists(path):
      return True if path != self.PID_FILE else next(destination_checks)

    with (
      patch.object(safe_process.sudo, "path_islink", return_value=False),
      patch.object(safe_process.sudo, "path_exists", side_effect=path_exists),
      patch.object(safe_process, "is_process_running", return_value=True),
      patch.object(safe_process, "File"),
      patch.object(safe_process.sudo, "lstat", return_value=self.FILE_STAT),
      patch.object(safe_process.sudo, "link_exclusive") as link_exclusive,
      patch.object(safe_process.sudo, "unlink") as unlink,
    ):
      with self.assertRaisesRegex(Fail, "created concurrently"):
        safe_process.create_pid_file_for_identity(
          self.PID_FILE,
          identity,
          self.USER,
          "ServiceMain",
          self.USER,
          "service-group",
        )
    link_exclusive.assert_not_called()
    unlink.assert_called_once()

  def test_create_pid_file_cleans_temp_after_atomic_link_failure(self):
    identity = safe_process.ProcessIdentity(self.PID, 1001, 456, ())

    def path_exists(path):
      return path != self.PID_FILE

    with (
      patch.object(safe_process.sudo, "path_islink", return_value=False),
      patch.object(safe_process.sudo, "path_exists", side_effect=path_exists),
      patch.object(safe_process, "is_process_running", return_value=True),
      patch.object(safe_process, "File"),
      patch.object(safe_process.sudo, "lstat", return_value=self.FILE_STAT),
      patch.object(
        safe_process.sudo,
        "link_exclusive",
        side_effect=OSError("destination exists"),
      ),
      patch.object(safe_process.sudo, "unlink") as unlink,
    ):
      with self.assertRaisesRegex(Fail, "Could not create PID file"):
        safe_process.create_pid_file_for_identity(
          self.PID_FILE,
          identity,
          self.USER,
          "ServiceMain",
          self.USER,
          "service-group",
        )
    unlink.assert_called_once()

  def test_create_pid_file_fails_if_process_disappears(self):
    identity = safe_process.ProcessIdentity(self.PID, 1001, 456, ())
    with (
      patch.object(safe_process.sudo, "path_islink", return_value=False),
      patch.object(safe_process.sudo, "path_exists", return_value=False),
      patch.object(safe_process, "is_process_running", return_value=False),
      patch.object(safe_process, "File") as pid_file,
    ):
      with self.assertRaisesRegex(Fail, "disappeared before PID file creation"):
        safe_process.create_pid_file_for_identity(
          self.PID_FILE,
          identity,
          self.USER,
          "ServiceMain",
          self.USER,
          "service-group",
        )
    pid_file.assert_not_called()

  def test_create_pid_file_detects_identity_change_after_write(self):
    identity = safe_process.ProcessIdentity(self.PID, 1001, 456, ())
    replacement = safe_process.ProcessIdentity(self.PID, 1001, 789, ())

    published = {"value": False}

    def path_exists(path):
      return path != self.PID_FILE or published["value"]

    def link_exclusive(source, destination):
      published["value"] = True

    def unlink(path):
      if path == self.PID_FILE:
        published["value"] = False

    with (
      patch.object(safe_process.sudo, "path_islink", return_value=False),
      patch.object(safe_process.sudo, "path_exists", side_effect=path_exists),
      patch.object(safe_process, "is_process_running", return_value=True),
      patch.object(safe_process, "File"),
      patch.object(safe_process.sudo, "lstat", return_value=self.FILE_STAT),
      patch.object(
        safe_process.sudo, "link_exclusive", side_effect=link_exclusive
      ),
      patch.object(safe_process.sudo, "unlink", side_effect=unlink) as unlink_mock,
      patch.object(
        safe_process, "read_running_process", return_value=replacement
      ),
    ):
      with self.assertRaisesRegex(Fail, "identity changed"):
        safe_process.create_pid_file_for_identity(
          self.PID_FILE,
          identity,
          self.USER,
          "ServiceMain",
          self.USER,
          "service-group",
        )
    self.assertFalse(published["value"])
    unlink_mock.assert_any_call(self.PID_FILE)

  def test_create_pid_file_does_not_remove_concurrent_replacement_on_rollback(self):
    identity = safe_process.ProcessIdentity(self.PID, 1001, 456, ())
    replacement = safe_process.ProcessIdentity(self.PID, 1001, 789, ())
    replacement_stat = SimpleNamespace(st_dev=30, st_ino=40, st_mode=0o100640)
    published = {"value": False}

    def path_exists(path):
      return path != self.PID_FILE or published["value"]

    def link_exclusive(source, destination):
      published["value"] = True

    with (
      patch.object(safe_process.sudo, "path_islink", return_value=False),
      patch.object(safe_process.sudo, "path_exists", side_effect=path_exists),
      patch.object(safe_process, "is_process_running", return_value=True),
      patch.object(safe_process, "File"),
      patch.object(
        safe_process.sudo,
        "lstat",
        side_effect=(self.FILE_STAT, replacement_stat),
      ),
      patch.object(
        safe_process.sudo, "link_exclusive", side_effect=link_exclusive
      ),
      patch.object(safe_process.sudo, "unlink") as unlink,
      patch.object(
        safe_process, "read_running_process", return_value=replacement
      ),
    ):
      with self.assertRaisesRegex(Fail, "identity changed"):
        safe_process.create_pid_file_for_identity(
          self.PID_FILE,
          identity,
          self.USER,
          "ServiceMain",
          self.USER,
          "service-group",
        )
    self.assertNotIn(call(self.PID_FILE), unlink.call_args_list)

  def test_pid_file_replacement_with_symlink_blocks_cleanup(self):
    with (
      patch.object(safe_process, "read_pid", side_effect=(self.PID, self.PID)),
      patch.object(safe_process, "is_process_running", return_value=False),
      patch.object(safe_process.sudo, "path_islink", return_value=True),
      patch.object(safe_process.sudo, "unlink") as unlink,
    ):
      with self.assertRaisesRegex(Fail, "replaced PID file"):
        safe_process.remove_pid_file_if_stopped(
          self.PID_FILE, self.PID, self.USER, "ServiceMain"
        )
    unlink.assert_not_called()

  def test_identity_validation_failure_is_not_downgraded_to_stopped(self):
    with patch(
      "resource_management.libraries.functions.check_process_status."
      "read_running_process",
      side_effect=Fail("owner mismatch"),
    ):
      with self.assertRaisesRegex(Fail, "owner mismatch"):
        check_process_status(
          self.PID_FILE, self.USER, "org.example.Service"
        )

  def test_legacy_status_maps_invalid_pid_to_component_not_running(self):
    with patch(
      "resource_management.libraries.functions.check_process_status."
      "read_running_process",
      side_effect=Fail("invalid pid"),
    ):
      with self.assertRaises(ComponentIsNotRunning):
        check_process_status(self.PID_FILE)

  def test_legacy_status_accepts_running_process(self):
    identity = safe_process.ProcessIdentity(self.PID, 1001, 456, ())
    with patch(
      "resource_management.libraries.functions.check_process_status."
      "read_running_process",
      return_value=identity,
    ):
      self.assertIsNone(check_process_status(self.PID_FILE))

  def test_legacy_status_maps_missing_or_stale_pid_to_not_running(self):
    with patch(
      "resource_management.libraries.functions.check_process_status."
      "read_running_process",
      return_value=None,
    ):
      with self.assertRaises(ComponentIsNotRunning):
        check_process_status(self.PID_FILE)


if __name__ == "__main__":
  unittest.main()
