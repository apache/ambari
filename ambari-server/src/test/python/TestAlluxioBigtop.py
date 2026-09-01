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
import sys
import unittest
from pathlib import Path
from types import ModuleType
from unittest.mock import MagicMock, call, patch

from resource_management.core.exceptions import ComponentIsNotRunning, Fail
from resource_management.libraries.functions import safe_process


ALLUXIO_SCRIPTS = (
  Path(__file__).resolve().parents[2]
  / "main/resources/stacks/BIGTOP/3.2.0/services/ALLUXIO/package/scripts"
)
MASTER_PROCESS_CLASS = "alluxio.master.AlluxioMaster"
WORKER_PROCESS_CLASS = "alluxio.worker.AlluxioWorker"


def load_script(module_name, filename):
  spec = importlib.util.spec_from_file_location(
    module_name, ALLUXIO_SCRIPTS / filename
  )
  module = importlib.util.module_from_spec(spec)
  sys.modules[module_name] = module
  spec.loader.exec_module(module)
  return module


ALLUXIO_UTILS = load_script("alluxio_utils", "alluxio_utils.py")
ALLUXIO_MASTER = load_script("bigtop_alluxio_master", "master.py")
ALLUXIO_WORKER = load_script("bigtop_alluxio_worker", "worker.py")
ALLUXIO_SERVICE_CHECK = load_script(
  "bigtop_alluxio_service_check", "service_check.py"
)


def params_module(**values):
  module = ModuleType("params")
  for name, value in values.items():
    setattr(module, name, value)
  return module


def process_identity(process_class, pid=123, start_time=456):
  return safe_process.ProcessIdentity(
    pid, 1001, start_time, ("java", process_class)
  )


def master_params(security_enabled=False, **overrides):
  values = {
    "alluxio_group": "hadoop",
    "alluxio_master_pid_file": "/run/alluxio/alluxio-master.pid",
    "alluxio_master_process_class": MASTER_PROCESS_CLASS,
    "alluxio_master_start_cmd": ("alluxio-start.sh", "-a", "-N", "master"),
    "alluxio_user": "alluxio",
    "java_home": "/usr/lib/jvm/java",
    "security_enabled": security_enabled,
  }
  values.update(overrides)
  return params_module(**values)


def worker_params(security_enabled=False, **overrides):
  values = {
    "alluxio_group": "hadoop",
    "alluxio_user": "alluxio",
    "alluxio_worker_mount_cmd": ("alluxio-mount.sh", "Mount"),
    "alluxio_worker_pid_file": "/run/alluxio/alluxio-worker.pid",
    "alluxio_worker_process_class": WORKER_PROCESS_CLASS,
    "alluxio_worker_start_cmd": (
      "alluxio-start.sh",
      "-a",
      "-N",
      "worker",
      "NoMount",
    ),
    "java_home": "/usr/lib/jvm/java",
    "security_enabled": security_enabled,
  }
  values.update(overrides)
  return params_module(**values)


def component_cases():
  return (
    (
      "master",
      ALLUXIO_MASTER,
      ALLUXIO_MASTER.AlluxioMaster,
      master_params(),
      "/run/alluxio/alluxio-master.pid",
      MASTER_PROCESS_CLASS,
      [
        call(
          ("alluxio-start.sh", "-a", "-N", "master"),
          user="alluxio",
          environment={"JAVA_HOME": "/usr/lib/jvm/java"},
          timeout=60,
        )
      ],
    ),
    (
      "worker",
      ALLUXIO_WORKER,
      ALLUXIO_WORKER.AlluxioWorker,
      worker_params(),
      "/run/alluxio/alluxio-worker.pid",
      WORKER_PROCESS_CLASS,
      [
        call(("alluxio-mount.sh", "Mount"), sudo=True, timeout=60),
        call(
          ("alluxio-start.sh", "-a", "-N", "worker", "NoMount"),
          user="alluxio",
          environment={"JAVA_HOME": "/usr/lib/jvm/java"},
          timeout=60,
        ),
      ],
    ),
  )


class TestAlluxioLifecycle(unittest.TestCase):
  def test_start_rejects_invalid_existing_pid_before_any_cli(self):
    failures = (
      "does not contain one positive integer",
      "must not be a symbolic link",
      "owner does not match",
      "command line does not match",
      "stale process",
    )
    for failure in failures:
      stale = failure == "stale process"
      process_validation = stale or failure in (
        "owner does not match",
        "command line does not match",
      )
      for name, module, service_class, params, _, _, _ in component_cases():
        service = service_class()
        with self.subTest(component=name, failure=failure):
          with patch.dict(sys.modules, {"params": params}), \
            patch.object(service, "configure"), \
            patch.object(module, "Execute") as execute, \
            patch.object(
              module.safe_process,
              "read_pid",
              side_effect=None if process_validation else Fail(failure),
              return_value=123 if process_validation else None,
            ), \
            patch.object(
              module.safe_process,
              "read_running_process",
              side_effect=(
                Fail(failure) if process_validation and not stale else None
              ),
              return_value=None,
            ) as read_running, \
            patch.object(
              module.safe_process, "discover_running_process"
            ) as discover:
            with self.assertRaisesRegex(Fail, failure):
              service.start(MagicMock())

          execute.assert_not_called()
          discover.assert_not_called()
          if process_validation:
            read_running.assert_called_once()
          else:
            read_running.assert_not_called()

  def test_start_is_idempotent_for_a_valid_pid_file(self):
    for name, module, service_class, params, _, process_class, _ in component_cases():
      service = service_class()
      identity = process_identity(process_class)
      with self.subTest(component=name):
        with patch.dict(sys.modules, {"params": params}), \
          patch.object(service, "configure"), \
          patch.object(module, "Execute") as execute, \
          patch.object(module.safe_process, "read_pid", return_value=123), \
          patch.object(
            module.safe_process, "read_running_process", return_value=identity
          ), \
          patch.object(
            module.safe_process, "discover_running_process", return_value=identity
          ) as discover, \
          patch.object(
            module.safe_process, "create_pid_file_for_identity"
          ) as create_pid:
          service.start(MagicMock())

        execute.assert_not_called()
        discover.assert_not_called()
        create_pid.assert_not_called()

  def test_start_recovers_a_missing_pid_file_for_a_unique_process(self):
    for (
      name,
      module,
      service_class,
      params,
      pid_file,
      process_class,
      _,
    ) in component_cases():
      service = service_class()
      identity = process_identity(process_class)
      with self.subTest(component=name):
        with patch.dict(sys.modules, {"params": params}), \
          patch.object(service, "configure"), \
          patch.object(module, "Execute") as execute, \
          patch.object(module.safe_process, "read_pid", return_value=None), \
          patch.object(
            module.safe_process, "discover_running_process", return_value=identity
          ) as discover, \
          patch.object(
            module.safe_process, "create_pid_file_for_identity"
          ) as create_pid, \
          patch.object(
            module.safe_process, "wait_for_discovered_process"
          ) as wait_for_process:
          service.start(MagicMock())

        execute.assert_not_called()
        discover.assert_called_once_with("alluxio", process_class)
        create_pid.assert_called_once_with(
          pid_file,
          identity,
          "alluxio",
          process_class,
          "alluxio",
          "hadoop",
          mode=0o640,
        )
        wait_for_process.assert_not_called()

  def test_start_does_not_run_cli_when_discovery_or_pid_recovery_fails(self):
    for failure_stage in ("discovery", "recovery"):
      for name, module, service_class, params, _, process_class, _ in component_cases():
        service = service_class()
        identity = process_identity(process_class)
        with self.subTest(component=name, failure_stage=failure_stage):
          with patch.dict(sys.modules, {"params": params}), \
            patch.object(service, "configure"), \
            patch.object(module, "Execute") as execute, \
            patch.object(module.safe_process, "read_pid", return_value=None), \
            patch.object(
              module.safe_process,
              "discover_running_process",
              side_effect=(
                Fail("ambiguous process discovery")
                if failure_stage == "discovery"
                else None
              ),
              return_value=identity,
            ), \
            patch.object(
              module.safe_process,
              "create_pid_file_for_identity",
              side_effect=(
                Fail("PID file was created concurrently")
                if failure_stage == "recovery"
                else None
              ),
            ) as create_pid:
            expected = (
              "ambiguous process discovery"
              if failure_stage == "discovery"
              else "PID file was created concurrently"
            )
            with self.assertRaisesRegex(Fail, expected):
              service.start(MagicMock())

          execute.assert_not_called()
          if failure_stage == "discovery":
            create_pid.assert_not_called()

  def test_start_uses_safe_identity_and_writes_a_missing_pid_file(self):
    for (
      name,
      module,
      service_class,
      params,
      pid_file,
      process_class,
      execute_calls,
    ) in component_cases():
      service = service_class()
      identity = process_identity(process_class)
      with self.subTest(component=name):
        with patch.dict(sys.modules, {"params": params}), \
          patch.object(service, "configure"), \
          patch.object(module, "Execute") as execute, \
          patch.object(
            module.safe_process,
            "read_running_process",
            return_value=identity,
          ) as read_running, \
          patch.object(
            module.safe_process, "discover_running_process", return_value=None
          ) as discover, \
          patch.object(
            module.safe_process,
            "wait_for_discovered_process",
            return_value=identity,
          ) as wait_for_process, \
          patch.object(
            module.safe_process, "read_pid", side_effect=(None, None)
          ), \
          patch.object(
            module.safe_process, "create_pid_file_for_identity"
          ) as create_pid:
          service.start(MagicMock())

        self.assertEqual(execute_calls, execute.call_args_list)
        discover.assert_called_once_with("alluxio", process_class)
        wait_for_process.assert_called_once_with(
          "alluxio", process_class, attempts=60, sleep_seconds=1
        )
        create_pid.assert_called_once_with(
          pid_file,
          identity,
          "alluxio",
          process_class,
          "alluxio",
          "hadoop",
          mode=0o640,
        )
        read_running.assert_called_once_with(pid_file, "alluxio", process_class)

  def test_start_rejects_pid_file_creation_and_process_races(self):
    failures = ("PID file was created concurrently", "Process 123 disappeared")
    for failure in failures:
      for name, module, service_class, params, _, process_class, _ in component_cases():
        service = service_class()
        identity = process_identity(process_class)
        with self.subTest(component=name, failure=failure):
          with patch.dict(sys.modules, {"params": params}), \
            patch.object(service, "configure"), \
            patch.object(module, "Execute"), \
            patch.object(
              module.safe_process, "read_running_process", return_value=None
            ), \
            patch.object(
              module.safe_process, "discover_running_process", return_value=None
            ), \
            patch.object(
              module.safe_process,
              "wait_for_discovered_process",
              return_value=identity,
            ), \
            patch.object(module.safe_process, "read_pid", side_effect=(None, None)), \
            patch.object(
              module.safe_process,
              "create_pid_file_for_identity",
              side_effect=Fail(failure),
            ) as create_pid:
            with self.assertRaisesRegex(Fail, failure):
              service.start(MagicMock())

          create_pid.assert_called_once()

  def test_start_rejects_identity_change_after_pid_file_write(self):
    for name, module, service_class, params, _, process_class, _ in component_cases():
      service = service_class()
      identity = process_identity(process_class)
      replacement = process_identity(process_class, start_time=999)
      with self.subTest(component=name):
        with patch.dict(sys.modules, {"params": params}), \
          patch.object(service, "configure"), \
          patch.object(module, "Execute"), \
          patch.object(
            module.safe_process,
            "read_running_process",
            return_value=replacement,
          ), \
          patch.object(
            module.safe_process, "discover_running_process", return_value=None
          ), \
          patch.object(
            module.safe_process,
            "wait_for_discovered_process",
            return_value=identity,
          ), \
          patch.object(module.safe_process, "read_pid", side_effect=(None, None)), \
          patch.object(module.safe_process, "create_pid_file_for_identity") as create_pid:
          with self.assertRaisesRegex(Fail, "process changed"):
            service.start(MagicMock())

        create_pid.assert_called_once()

  def test_start_and_wait_failures_propagate_without_false_success(self):
    for name, module, service_class, params, _, _, _ in component_cases():
      for failure_stage in ("command", "wait"):
        service = service_class()
        execute_effect = (
          Fail("command failed")
          if name == "master"
          else (None, Fail("command failed"))
        )
        with self.subTest(component=name, failure_stage=failure_stage):
          with patch.dict(sys.modules, {"params": params}), \
            patch.object(service, "configure"), \
            patch.object(
              module,
              "Execute",
              side_effect=execute_effect if failure_stage == "command" else None,
            ), \
            patch.object(module, "File") as file_resource, \
            patch.object(module.safe_process, "read_pid", return_value=None), \
            patch.object(
              module.safe_process, "read_running_process", return_value=None
            ), \
            patch.object(
              module.safe_process, "discover_running_process", return_value=None
            ), \
            patch.object(
              module.safe_process,
              "wait_for_discovered_process",
              side_effect=Fail("wait failed") if failure_stage == "wait" else None,
            ) as wait_for_process, \
            patch.object(module, "rollback_started_process") as rollback:
            with self.assertRaisesRegex(Fail, f"{failure_stage} failed"):
              service.start(MagicMock())

          file_resource.assert_not_called()
          rollback.assert_called_once_with(
            params.alluxio_master_pid_file
            if name == "master"
            else params.alluxio_worker_pid_file,
            "alluxio",
            params.alluxio_master_process_class
            if name == "master"
            else params.alluxio_worker_process_class,
          )
          if failure_stage == "command":
            wait_for_process.assert_not_called()

  def test_start_preserves_original_failure_when_rollback_fails(self):
    for name, module, service_class, params, _, _, _ in component_cases():
      service = service_class()
      execute_effect = (
        Fail("command failed")
        if name == "master"
        else (None, Fail("command failed"))
      )
      with self.subTest(component=name):
        with patch.dict(sys.modules, {"params": params}), \
          patch.object(service, "configure"), \
          patch.object(module, "Execute", side_effect=execute_effect), \
          patch.object(module.safe_process, "read_pid", return_value=None), \
          patch.object(
            module.safe_process, "discover_running_process", return_value=None
          ), \
          patch.object(
            module,
            "rollback_started_process",
            side_effect=Fail("cleanup failed"),
          ), \
          patch.object(module.Logger, "warning") as warning:
          with self.assertRaisesRegex(Fail, "command failed"):
            service.start(MagicMock())

        warning.assert_called_once()

  def test_rollback_terminates_only_the_discovered_identity(self):
    identity = process_identity(MASTER_PROCESS_CLASS)
    with patch.object(
      ALLUXIO_UTILS.safe_process,
      "discover_running_process",
      return_value=identity,
    ) as discover, \
      patch.object(ALLUXIO_UTILS.safe_process, "terminate_process") as terminate, \
      patch.object(ALLUXIO_UTILS.safe_process, "read_pid", return_value=123), \
      patch.object(
        ALLUXIO_UTILS.safe_process, "remove_pid_file_if_stopped"
      ) as remove_pid:
      ALLUXIO_UTILS.rollback_started_process(
        "/run/alluxio/alluxio-master.pid", "alluxio", MASTER_PROCESS_CLASS
      )

    discover.assert_called_once_with("alluxio", MASTER_PROCESS_CLASS)
    terminate.assert_called_once_with(identity, "alluxio", MASTER_PROCESS_CLASS)
    remove_pid.assert_called_once_with(
      "/run/alluxio/alluxio-master.pid",
      123,
      "alluxio",
      MASTER_PROCESS_CLASS,
    )

  def test_stop_is_idempotent_only_when_pid_and_discovery_are_missing(self):
    for name, module, service_class, params, _, _, _ in component_cases():
      service = service_class()
      with self.subTest(component=name):
        with patch.dict(sys.modules, {"params": params}), \
          patch.object(service, "configure"), \
          patch.object(module.safe_process, "read_pid", return_value=None), \
          patch.object(module.safe_process, "read_running_process", return_value=None), \
          patch.object(
            module.safe_process, "discover_running_process", return_value=None
          ), \
          patch.object(module.safe_process, "terminate_process") as terminate, \
          patch.object(
            module.safe_process, "remove_pid_file_if_stopped"
          ) as remove_pid:
          service.stop(MagicMock())

        terminate.assert_not_called()
        remove_pid.assert_not_called()

  def test_stop_never_reconfigures_files_or_accesses_hdfs(self):
    for name, module, service_class, params, _, _, _ in component_cases():
      service = service_class()
      params.HdfsResource = MagicMock()
      with self.subTest(component=name):
        with patch.dict(sys.modules, {"params": params}), \
          patch.object(service, "configure") as configure, \
          patch.object(module.safe_process, "read_pid", return_value=None), \
          patch.object(
            module.safe_process, "discover_running_process", return_value=None
          ):
          service.stop(MagicMock())

        configure.assert_not_called()
        params.HdfsResource.assert_not_called()

  def test_stop_signals_only_the_captured_identity_and_safely_removes_pid(self):
    for name, module, service_class, params, pid_file, process_class, _ in component_cases():
      for discovered in (False, True):
        service = service_class()
        identity = process_identity(process_class)
        with self.subTest(component=name, discovered=discovered):
          with patch.dict(sys.modules, {"params": params}), \
            patch.object(service, "configure"), \
            patch.object(
              module.safe_process,
              "read_pid",
              return_value=None if discovered else 123,
            ), \
            patch.object(
              module.safe_process,
              "read_running_process",
              return_value=None if discovered else identity,
            ), \
            patch.object(
              module.safe_process,
              "discover_running_process",
              return_value=identity if discovered else None,
            ) as discover, \
            patch.object(module.safe_process, "terminate_process") as terminate, \
            patch.object(
              module.safe_process, "remove_pid_file_if_stopped"
            ) as remove_pid:
            service.stop(MagicMock())

          terminate.assert_called_once_with(identity, "alluxio", process_class)
          remove_pid.assert_called_once_with(
            pid_file, 123, "alluxio", process_class
          )
          if discovered:
            discover.assert_called_once_with("alluxio", process_class)
          else:
            discover.assert_not_called()

  def test_stop_preserves_pid_file_on_validation_signal_or_cleanup_failure(self):
    failures = ("owner mismatch", "start time changed", "PID file replaced")
    for failure in failures:
      for name, module, service_class, params, _, process_class, _ in component_cases():
        service = service_class()
        identity = process_identity(process_class)
        with self.subTest(component=name, failure=failure):
          read_failure = failure == "owner mismatch"
          terminate_failure = failure == "start time changed"
          with patch.dict(sys.modules, {"params": params}), \
            patch.object(service, "configure"), \
            patch.object(module.safe_process, "read_pid", return_value=123), \
            patch.object(
              module.safe_process,
              "read_running_process",
              side_effect=Fail(failure) if read_failure else None,
              return_value=None if read_failure else identity,
            ), \
            patch.object(module.safe_process, "discover_running_process") as discover, \
            patch.object(
              module.safe_process,
              "terminate_process",
              side_effect=Fail(failure) if terminate_failure else None,
            ) as terminate, \
            patch.object(
              module.safe_process,
              "remove_pid_file_if_stopped",
              side_effect=(
                Fail(failure)
                if not read_failure and not terminate_failure
                else None
              ),
            ) as remove_pid:
            with self.assertRaisesRegex(Fail, failure):
              service.stop(MagicMock())

          discover.assert_not_called()
          if read_failure:
            terminate.assert_not_called()
          if read_failure or terminate_failure:
            remove_pid.assert_not_called()

  def test_status_uses_strict_pid_or_read_only_discovery(self):
    for name, module, service_class, params, _, process_class, _ in component_cases():
      identity = process_identity(process_class)
      for from_pid_file in (True, False):
        with self.subTest(component=name, from_pid_file=from_pid_file):
          with patch.dict(sys.modules, {"params": params}), \
            patch.object(
              module.safe_process,
              "read_pid",
              return_value=123 if from_pid_file else None,
            ), \
            patch.object(
              module.safe_process,
              "read_running_process",
              return_value=identity if from_pid_file else None,
            ) as read_running, \
            patch.object(
              module.safe_process,
              "discover_running_process",
              return_value=None if from_pid_file else identity,
            ) as discover, \
            patch.object(module, "File") as file_resource:
            service_class().status(MagicMock())

          if from_pid_file:
            read_running.assert_called_once_with(
              params.alluxio_master_pid_file
              if name == "master"
              else params.alluxio_worker_pid_file,
              "alluxio",
              process_class,
            )
            discover.assert_not_called()
          else:
            read_running.assert_not_called()
            discover.assert_called_once_with("alluxio", process_class)
          file_resource.assert_not_called()

  def test_status_reports_missing_and_preserves_validation_failures(self):
    for name, module, service_class, params, _, _, _ in component_cases():
      for invalid in (False, True):
        with self.subTest(component=name, invalid=invalid):
          with patch.dict(sys.modules, {"params": params}), \
            patch.object(
              module.safe_process,
              "read_pid",
              side_effect=Fail("invalid PID") if invalid else None,
              return_value=None,
            ), \
            patch.object(module.safe_process, "read_running_process"), \
            patch.object(
              module.safe_process, "discover_running_process", return_value=None
            ) as discover:
            expected = Fail if invalid else ComponentIsNotRunning
            with self.assertRaises(expected):
              service_class().status(MagicMock())

          if invalid:
            discover.assert_not_called()

  def test_status_propagates_process_identity_failures(self):
    for name, module, service_class, params, _, _, _ in component_cases():
      with self.subTest(component=name):
        with patch.dict(sys.modules, {"params": params}), \
          patch.object(module.safe_process, "read_pid", return_value=123), \
          patch.object(
            module.safe_process,
            "read_running_process",
            side_effect=Fail("process identity changed"),
          ), \
          patch.object(
            module.safe_process, "discover_running_process"
          ) as discover:
          with self.assertRaisesRegex(Fail, "process identity changed"):
            service_class().status(MagicMock())

        discover.assert_not_called()


class TestAlluxioKerberosStartup(unittest.TestCase):
  KINIT = "/usr/bin/kinit"
  KEYTAB = "/etc/security/alluxio.keytab;touch /tmp/keytab-injection"
  PRINCIPAL = "alluxio/host@REALM;touch /tmp/principal-injection"

  def _run_secure_start(self, module, service_class, params, process_class):
    identity = process_identity(process_class)
    service = service_class()
    with patch.dict(sys.modules, {"params": params}), \
      patch.object(service, "configure"), \
      patch.object(module, "Execute") as execute, \
      patch.object(
        module.safe_process,
        "read_running_process",
        return_value=identity,
      ), \
      patch.object(module.safe_process, "discover_running_process", return_value=None), \
      patch.object(
        module.safe_process, "wait_for_discovered_process", return_value=identity
      ), \
      patch.object(module.safe_process, "read_pid", side_effect=(None, None)), \
      patch.object(module.safe_process, "create_pid_file_for_identity"):
      service.start(MagicMock())
    return execute.call_args_list

  def test_secure_kinit_keeps_metacharacters_in_single_argv(self):
    common = {
      "alluxio_service_kerberos_keytab": self.KEYTAB,
      "kinit_path_local": self.KINIT,
      "kinit_principal": self.PRINCIPAL,
    }
    master_calls = self._run_secure_start(
      ALLUXIO_MASTER,
      ALLUXIO_MASTER.AlluxioMaster,
      master_params(True, **common),
      MASTER_PROCESS_CLASS,
    )
    worker_calls = self._run_secure_start(
      ALLUXIO_WORKER,
      ALLUXIO_WORKER.AlluxioWorker,
      worker_params(True, **common),
      WORKER_PROCESS_CLASS,
    )
    kinit_call = call(
      (self.KINIT, "-kt", self.KEYTAB, self.PRINCIPAL),
      user="alluxio",
      timeout=30,
    )

    self.assertEqual(kinit_call, master_calls[0])
    self.assertEqual(kinit_call, worker_calls[1])

  def test_insecure_start_does_not_run_kinit_or_format_master(self):
    for _, module, service_class, params, _, process_class, expected in component_cases():
      calls = self._run_secure_start(
        module, service_class, params, process_class
      )
      self.assertEqual(expected, calls)

  def test_kinit_failure_prevents_service_start(self):
    common = {
      "alluxio_service_kerberos_keytab": self.KEYTAB,
      "kinit_path_local": self.KINIT,
      "kinit_principal": self.PRINCIPAL,
    }
    cases = (
      (
        ALLUXIO_MASTER,
        ALLUXIO_MASTER.AlluxioMaster,
        master_params(True, **common),
        Fail("kinit failed"),
        1,
      ),
      (
        ALLUXIO_WORKER,
        ALLUXIO_WORKER.AlluxioWorker,
        worker_params(True, **common),
        (None, Fail("kinit failed")),
        2,
      ),
    )
    for module, service_class, params, execute_effect, expected_calls in cases:
      service = service_class()
      with patch.dict(sys.modules, {"params": params}), \
        patch.object(service, "configure"), \
        patch.object(module, "Execute", side_effect=execute_effect) as execute, \
        patch.object(module.safe_process, "read_pid", return_value=None), \
        patch.object(module.safe_process, "read_running_process", return_value=None), \
        patch.object(
          module.safe_process, "discover_running_process", return_value=None
        ), \
        patch.object(
          module.safe_process, "wait_for_discovered_process"
        ) as wait_for_process:
        with self.assertRaisesRegex(Fail, "kinit failed"):
          service.start(MagicMock())

      self.assertEqual(expected_calls, execute.call_count)
      wait_for_process.assert_not_called()


class TestAlluxioConfiguration(unittest.TestCase):
  @staticmethod
  def _params(hdfs_resource=None):
    return params_module(
      HdfsResource=hdfs_resource or MagicMock(),
      alluxio_conf_dir="/usr/bigtop/current/alluxio/conf",
      alluxio_env_sh="env",
      alluxio_group="hadoop",
      alluxio_hdfs_user_dir="/user/alluxio",
      alluxio_log4j2_properties="log4j",
      alluxio_metrics_properties="metrics",
      alluxio_log_dir="/var/log/alluxio",
      alluxio_master_metastore_dir="/var/lib/alluxio/metastore",
      alluxio_pid_dir="/run/alluxio",
      alluxio_site_properties="site",
      alluxio_user="alluxio",
      alluxio_workers_str="worker1",
      alluxio_masters_str="master1",
      underfs_hdfs_addr="hdfs://namenode/alluxio",
    )

  def _configure(self, service_module, service_class, params):
    with patch.dict(sys.modules, {"params": params}), \
      patch.object(service_module, "Directory") as directory, \
      patch.object(service_module, "File") as file_resource, \
      patch.object(
        service_module, "InlineTemplate", side_effect=lambda value: value
      ), \
      patch.object(service_module, "Template", return_value="template"), \
      patch.object(service_module, "format", return_value="config-file"):
      service_class().configure(MagicMock())
    return directory, file_resource

  def _assert_local_permissions(
    self, service_module, service_class, creates_metastore
  ):
    params = self._params()
    directory, file_resource = self._configure(
      service_module, service_class, params
    )

    self.assertIn(
      call(
        ["/var/log/alluxio", "/var/log/alluxio/user"],
        owner="alluxio",
        group="hadoop",
        mode=0o770,
        create_parents=True,
      ),
      directory.call_args_list,
    )
    all_directory_paths = {
      path
      for directory_call in directory.call_args_list
      for path in (
        directory_call.args[0]
        if isinstance(directory_call.args[0], (list, tuple))
        else (directory_call.args[0],)
      )
    }
    for directory_call in directory.call_args_list:
      self.assertEqual(0, directory_call.kwargs["mode"] & 0o007)
    self.assertNotIn("/var/lib/alluxio/journal", all_directory_paths)
    if creates_metastore:
      self.assertIn("/var/lib/alluxio/metastore", all_directory_paths)
    else:
      self.assertNotIn("/var/lib/alluxio/metastore", all_directory_paths)

    self.assertEqual(6, file_resource.call_count)
    self.assertIn(
      "/usr/bigtop/current/alluxio/conf/metrics.properties",
      {str(file_call.args[0]) for file_call in file_resource.call_args_list},
    )
    for file_call in file_resource.call_args_list:
      self.assertEqual("root", file_call.kwargs["owner"])
      self.assertEqual("hadoop", file_call.kwargs["group"])
      expected_mode = (
        0o640 if str(file_call.args[0]).endswith("alluxio-env.sh") else 0o644
      )
      self.assertEqual(expected_mode, file_call.kwargs["mode"])

  def test_master_flushes_queued_hdfs_directories_in_order(self):
    hdfs_resource = MagicMock()
    params = self._params(hdfs_resource)
    self._configure(ALLUXIO_MASTER, ALLUXIO_MASTER.AlluxioMaster, params)

    self.assertEqual(
      [
        call(
          "/user/alluxio",
          type="directory",
          action="create_on_execute",
          owner="alluxio",
          mode=0o755,
        ),
        call(
          "hdfs://namenode/alluxio",
          type="directory",
          action="create_on_execute",
          owner="alluxio",
          mode=0o755,
        ),
        call(None, action="execute"),
      ],
      hdfs_resource.call_args_list,
    )

  def test_master_propagates_hdfs_flush_failure(self):
    hdfs_resource = MagicMock(
      side_effect=(None, None, Fail("HDFS flush failed"))
    )
    params = self._params(hdfs_resource)

    with self.assertRaisesRegex(Fail, "HDFS flush failed"):
      self._configure(ALLUXIO_MASTER, ALLUXIO_MASTER.AlluxioMaster, params)
    self.assertEqual(call(None, action="execute"), hdfs_resource.call_args_list[-1])

  def test_master_local_permissions_exclude_other_access(self):
    self._assert_local_permissions(
      ALLUXIO_MASTER, ALLUXIO_MASTER.AlluxioMaster, creates_metastore=True
    )

  def test_worker_local_permissions_exclude_other_access(self):
    self._assert_local_permissions(
      ALLUXIO_WORKER, ALLUXIO_WORKER.AlluxioWorker, creates_metastore=False
    )


class TestAlluxioServiceCheck(unittest.TestCase):
  def test_product_scripts_use_explicit_imports_and_preserve_journal(self):
    for script_name in ("master.py", "worker.py"):
      source = (ALLUXIO_SCRIPTS / script_name).read_text(encoding="utf-8")
      with self.subTest(script=script_name):
        self.assertNotIn("from resource_management import *", source)
        self.assertIn(
          "from resource_management.core.resources.system import ", source
        )
        self.assertIn("from resource_management.core.source import ", source)
        self.assertNotIn("alluxio_journal_dir", source)
        self.assertNotIn("formatMaster", source)
        self.assertNotIn('action="delete"', source)

  def test_worker_configures_root_owned_inputs_before_sudo_mount(self):
    source = (ALLUXIO_SCRIPTS / "worker.py").read_text(encoding="utf-8")
    self.assertLess(
      source.index("self.configure(env)"),
      source.index(
        "Execute(params.alluxio_worker_mount_cmd, sudo=True, timeout=60)"
      ),
    )

  def test_service_check_asf_header_is_undamaged(self):
    source = (ALLUXIO_SCRIPTS / "service_check.py").read_text(encoding="utf-8")
    self.assertIn(
      "Unless required by applicable law or agreed to in writing", source
    )
    self.assertNotIn("law or agree in writing", source)

  def test_check_uses_tuple_argv_and_bounded_timeout(self):
    command = (
      "/usr/lib/alluxio/bin/alluxio;touch /tmp/path-injection",
      "runTests;touch /tmp/argument-injection",
    )
    params = params_module(alluxio_test_cmd=command, alluxio_user="alluxio")
    with patch.dict(sys.modules, {"params": params}), \
      patch.object(ALLUXIO_SERVICE_CHECK, "Execute") as execute:
      ALLUXIO_SERVICE_CHECK.AlluxioServiceCheck().service_check(MagicMock())

    execute.assert_called_once_with(command, user="alluxio", timeout=300)

  def test_check_propagates_nonzero_and_timeout_failures(self):
    params = params_module(
      alluxio_test_cmd=("/usr/lib/alluxio/bin/alluxio", "runTests"),
      alluxio_user="alluxio",
    )
    for failure in ("service check failed", "service check timed out"):
      with self.subTest(failure=failure):
        with patch.dict(sys.modules, {"params": params}), \
          patch.object(
            ALLUXIO_SERVICE_CHECK, "Execute", side_effect=Fail(failure)
          ):
          with self.assertRaisesRegex(Fail, failure):
            ALLUXIO_SERVICE_CHECK.AlluxioServiceCheck().service_check(
              MagicMock()
            )


if __name__ == "__main__":
  unittest.main()
