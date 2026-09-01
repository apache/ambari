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
from unittest.mock import MagicMock, patch

from resource_management.core.exceptions import ComponentIsNotRunning, Fail
from resource_management.libraries.functions import safe_process


KAFKA = (
  Path(__file__).resolve().parents[2]
  / "main/resources/stacks/BIGTOP/3.2.0/services/KAFKA"
)
SCRIPTS = KAFKA / "package/scripts"
PID_FILE = "/var/run/kafka/kafka.pid"
SERVER_PROPERTIES = "/etc/kafka/conf/server.properties"
TOKENS = ("kafka.Kafka", SERVER_PROPERTIES)
IDENTITY = safe_process.ProcessIdentity(
  123,
  1001,
  456,
  ("/usr/bin/java", "-Xmx1g", *TOKENS),
)


def dependency_module(name, **attributes):
  module = ModuleType(name)
  for attribute, value in attributes.items():
    setattr(module, attribute, value)
  return module


def load_module(module_name, path, dependencies=None):
  spec = importlib.util.spec_from_file_location(module_name, path)
  module = importlib.util.module_from_spec(spec)
  with patch.dict(sys.modules, dependencies or {}):
    spec.loader.exec_module(module)
  return module


KAFKA_PROCESS = load_module("bigtop_kafka_process", SCRIPTS / "kafka_process.py")
KAFKA_SETUP = dependency_module(
  "kafka", kafka=MagicMock(), ensure_base_directories=MagicMock()
)
RANGER_SETUP = dependency_module("setup_ranger_kafka", setup_ranger_kafka=MagicMock())
KAFKA_BROKER = load_module(
  "bigtop_kafka_broker",
  SCRIPTS / "kafka_broker.py",
  {
    "kafka": KAFKA_SETUP,
    "kafka_process": KAFKA_PROCESS,
    "setup_ranger_kafka": RANGER_SETUP,
  },
)


class TestKafkaProcessLifecycle(unittest.TestCase):
  def test_expected_identity_uses_official_kafka_341_main_class_and_config(self):
    self.assertEqual(TOKENS, KAFKA_PROCESS.expected_process_tokens(SERVER_PROPERTIES))
    for unsafe_path in (
      None,
      "",
      "relative.pid",
      "/kafka.pid",
      "/run/../kafka.pid",
      "/etc/kafka.pid",
      "/tmp/kafka.pid",
      "/var/run/kafka/other.pid",
    ):
      with self.subTest(unsafe_path=unsafe_path):
        with self.assertRaises(Fail):
          KAFKA_PROCESS.validate_pid_file(unsafe_path)

  def test_valid_pid_is_idempotent_without_discovery_or_rewrite(self):
    with (
      patch.object(safe_process, "read_pid", return_value=123),
      patch.object(
        safe_process, "read_running_process", return_value=IDENTITY
      ) as read_running,
      patch.object(safe_process, "discover_running_process") as discover,
      patch.object(safe_process, "create_pid_file_for_identity") as create,
    ):
      result = KAFKA_PROCESS.read_or_recover_process(
        PID_FILE, "kafka", "hadoop", SERVER_PROPERTIES
      )

    self.assertIs(IDENTITY, result)
    read_running.assert_called_once_with(PID_FILE, "kafka", TOKENS)
    discover.assert_not_called()
    create.assert_not_called()

  def test_pidless_process_is_uniquely_discovered_and_atomically_published(self):
    with (
      patch.object(safe_process, "read_pid", return_value=None),
      patch.object(
        safe_process, "discover_running_process", return_value=IDENTITY
      ) as discover,
      patch.object(
        safe_process,
        "create_pid_file_for_identity",
        return_value=IDENTITY,
      ) as create,
    ):
      result = KAFKA_PROCESS.read_or_recover_process(
        PID_FILE, "kafka", "hadoop", SERVER_PROPERTIES
      )

    self.assertIs(IDENTITY, result)
    discover.assert_called_once_with("kafka", TOKENS)
    create.assert_called_once_with(
      PID_FILE,
      IDENTITY,
      expected_user="kafka",
      expected_cmdline=TOKENS,
      owner="kafka",
      group="hadoop",
      mode=0o640,
    )

  def test_stale_pid_is_removed_before_discovery(self):
    with (
      patch.object(safe_process, "read_pid", return_value=123),
      patch.object(safe_process, "read_running_process", return_value=None),
      patch.object(
        safe_process, "remove_pid_file_if_stopped", return_value=True
      ) as remove,
      patch.object(safe_process, "discover_running_process", return_value=None),
    ):
      result = KAFKA_PROCESS.read_or_recover_process(
        PID_FILE, "kafka", "hadoop", SERVER_PROPERTIES
      )

    self.assertIsNone(result)
    remove.assert_called_once_with(
      PID_FILE,
      123,
      expected_user="kafka",
      expected_cmdline=TOKENS,
    )

  def test_invalid_pid_wrong_owner_and_ambiguous_discovery_fail_closed(self):
    failures = (
      ("read_pid", Fail("invalid PID file")),
      ("read_running_process", Fail("owner does not match")),
      ("discover_running_process", Fail("ambiguous process discovery")),
    )
    for failing_call, failure in failures:
      with self.subTest(failing_call=failing_call):
        read_pid_result = None if failing_call == "discover_running_process" else 123
        with (
          patch.object(
            safe_process,
            "read_pid",
            return_value=read_pid_result,
            side_effect=failure if failing_call == "read_pid" else None,
          ),
          patch.object(
            safe_process,
            "read_running_process",
            side_effect=failure if failing_call == "read_running_process" else None,
          ),
          patch.object(
            safe_process,
            "discover_running_process",
            side_effect=(
              failure if failing_call == "discover_running_process" else None
            ),
          ),
          patch.object(safe_process, "remove_pid_file_if_stopped") as remove,
          patch.object(safe_process, "create_pid_file_for_identity") as create,
        ):
          with self.assertRaises(Fail):
            KAFKA_PROCESS.read_or_recover_process(
              PID_FILE, "kafka", "hadoop", SERVER_PROPERTIES
            )
        if failing_call != "discover_running_process":
          remove.assert_not_called()
        create.assert_not_called()

  def test_concurrent_exact_pid_publication_is_accepted(self):
    with (
      patch.object(safe_process, "read_pid", return_value=None),
      patch.object(
        safe_process, "discover_running_process", return_value=IDENTITY
      ),
      patch.object(
        safe_process,
        "create_pid_file_for_identity",
        side_effect=Fail("created concurrently"),
      ),
      patch.object(
        safe_process, "read_running_process", return_value=IDENTITY
      ),
    ):
      result = KAFKA_PROCESS.read_or_recover_process(
        PID_FILE, "kafka", "hadoop", SERVER_PROPERTIES
      )

    self.assertIs(IDENTITY, result)

  def test_start_waits_for_unique_identity_then_publishes_0640_pid(self):
    with (
      patch.object(
        safe_process, "wait_for_discovered_process", return_value=IDENTITY
      ) as wait,
      patch.object(
        safe_process,
        "create_pid_file_for_identity",
        return_value=IDENTITY,
      ) as create,
    ):
      result = KAFKA_PROCESS.wait_for_started_process(
        PID_FILE,
        "kafka",
        "hadoop",
        SERVER_PROPERTIES,
        attempts=9,
        sleep_seconds=2,
      )

    self.assertIs(IDENTITY, result)
    wait.assert_called_once_with(
      "kafka", TOKENS, attempts=9, sleep_seconds=2
    )
    self.assertEqual(0o640, create.call_args.kwargs["mode"])

  def test_stop_pins_identity_then_uses_term_wait_kill_contract(self):
    with (
      patch.object(
        KAFKA_PROCESS, "read_or_recover_process", return_value=IDENTITY
      ),
      patch.object(safe_process, "terminate_process") as terminate,
      patch.object(safe_process, "remove_pid_file_if_stopped") as remove,
    ):
      result = KAFKA_PROCESS.stop_process(
        PID_FILE, "kafka", "hadoop", SERVER_PROPERTIES
      )

    self.assertTrue(result)
    terminate.assert_called_once_with(
      IDENTITY,
      "kafka",
      TOKENS,
      term_wait_attempts=120,
      term_wait_sleep=1,
      kill_wait_attempts=10,
      kill_wait_sleep=1,
    )
    remove.assert_called_once_with(
      PID_FILE,
      123,
      expected_user="kafka",
      expected_cmdline=TOKENS,
    )


class TestKafkaBrokerLifecycle(unittest.TestCase):
  def setUp(self):
    self.params = SimpleNamespace(
      is_supported_kafka_ranger=False,
      kafka_pid_file=PID_FILE,
      kafka_pid_dir="/var/run/kafka",
      kafka_user="kafka",
      user_group="hadoop",
      kafka_server_properties=SERVER_PROPERTIES,
      kafka_env_file="/etc/kafka/conf/kafka env.sh;$(id)",
      kafka_server_start="/usr/lib/kafka/bin/kafka-server-start.sh;$(id)",
      kafka_log_dir="/var/log/kafka",
      zookeeper_connect="zk1:2181;$(id)",
      secure_acls=True,
      kafka_security_migrator=(
        "/usr/lib/kafka/bin/zookeeper-security-migration.sh;$(id)"
      ),
      java64_home="/usr/lib/jvm/java-17;$(id)",
      kafka_kerberos_params=(
        "-Djava.security.auth.login.config=/etc/kafka/conf/kafka_jaas.conf"
      ),
    )
    self.env = MagicMock()
    self.broker = KAFKA_BROKER.KafkaBroker()

  def test_start_is_idempotent_when_verified_process_is_running(self):
    with (
      patch.dict(sys.modules, {"params": self.params}),
      patch.object(self.broker, "configure") as configure,
      patch.object(
        KAFKA_PROCESS, "read_or_recover_process", return_value=IDENTITY
      ),
      patch.object(KAFKA_BROKER, "Execute") as execute,
    ):
      self.broker.start(self.env)

    configure.assert_called_once_with(self.env, upgrade_type=None)
    execute.assert_not_called()

  def test_start_preserves_untrusted_paths_as_positional_arguments(self):
    with (
      patch.dict(sys.modules, {"params": self.params}),
      patch.object(self.broker, "configure"),
      patch.object(
        KAFKA_PROCESS, "read_or_recover_process", return_value=None
      ),
      patch.object(KAFKA_BROKER, "Execute") as execute,
      patch.object(
        KAFKA_PROCESS, "wait_for_started_process", return_value=IDENTITY
      ) as wait,
    ):
      self.broker.start(self.env)

    execute.assert_called_once_with(
      (
        "/bin/bash",
        "-c",
        'source "$1" && exec "$2" -daemon "$3"',
        "ambari-kafka-start",
        self.params.kafka_env_file,
        self.params.kafka_server_start,
        SERVER_PROPERTIES,
      ),
      user="kafka",
    )
    wait.assert_called_once_with(
      PID_FILE, "kafka", "hadoop", SERVER_PROPERTIES
    )

  def test_start_command_and_identity_failures_show_logs_and_propagate(self):
    for execute_error, wait_error in (
      (Fail("launcher failed"), None),
      (None, Fail("Kafka process was not discovered")),
    ):
      with self.subTest(execute_error=execute_error, wait_error=wait_error):
        with (
          patch.dict(sys.modules, {"params": self.params}),
          patch.object(self.broker, "configure"),
          patch.object(
            KAFKA_PROCESS, "read_or_recover_process", return_value=None
          ),
          patch.object(KAFKA_BROKER, "Execute", side_effect=execute_error),
          patch.object(
            KAFKA_PROCESS,
            "wait_for_started_process",
            side_effect=wait_error,
          ) as wait,
          patch.object(KAFKA_BROKER, "show_logs") as show_logs,
        ):
          with self.assertRaises(Fail):
            self.broker.start(self.env)
        show_logs.assert_called_once_with("/var/log/kafka", "kafka")
        if execute_error is not None:
          wait.assert_not_called()

  def test_stop_delegates_to_verified_process_contract(self):
    status = dependency_module(
      "status_params",
      kafka_pid_file=PID_FILE,
      kafka_user="kafka",
      user_group="hadoop",
      kafka_server_properties=SERVER_PROPERTIES,
    )
    with (
      patch.dict(sys.modules, {"status_params": status}),
      patch.object(KAFKA_PROCESS, "stop_process", return_value=True) as stop,
    ):
      self.broker.stop(self.env)

    stop.assert_called_once_with(
      PID_FILE, "kafka", "hadoop", SERVER_PROPERTIES
    )

  def test_status_recovers_pidless_process_and_maps_absence_to_not_running(self):
    status = dependency_module(
      "status_params",
      kafka_pid_file=PID_FILE,
      kafka_user="kafka",
      user_group="hadoop",
      kafka_server_properties=SERVER_PROPERTIES,
    )
    with (
      patch.dict(sys.modules, {"status_params": status}),
      patch.object(
        KAFKA_PROCESS,
        "read_or_recover_process",
        side_effect=(IDENTITY, None),
      ) as recover,
    ):
      self.broker.status(self.env)
      with self.assertRaises(ComponentIsNotRunning):
        self.broker.status(self.env)

    self.assertEqual(2, recover.call_count)

  def test_disable_security_uses_structured_migrator_arguments(self):
    with (
      patch.dict(sys.modules, {"params": self.params}),
      patch.object(KAFKA_BROKER, "Execute") as execute,
    ):
      self.broker.disable_security(self.env)

    execute.assert_called_once_with(
      (
        self.params.kafka_security_migrator,
        "--zookeeper.connect",
        self.params.zookeeper_connect,
        "--zookeeper.acl=unsecure",
      ),
      user="kafka",
      environment={
        "JAVA_HOME": self.params.java64_home,
        "KAFKA_OPTS": (
          "-Djavax.security.auth.useSubjectCredsOnly=false "
          + self.params.kafka_kerberos_params
        ),
      },
      logoutput=True,
      tries=3,
    )

  def test_disable_security_fails_before_migration_without_jaas(self):
    self.params.kafka_kerberos_params = ""
    with (
      patch.dict(sys.modules, {"params": self.params}),
      patch.object(KAFKA_BROKER, "Execute") as execute,
    ):
      with self.assertRaisesRegex(Fail, "JAAS"):
        self.broker.disable_security(self.env)
    execute.assert_not_called()


if __name__ == "__main__":
  unittest.main()
