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
from types import ModuleType
import unittest
from unittest.mock import MagicMock, patch

from resource_management.core.exceptions import Fail
from resource_management.libraries.functions import safe_process


SERVICE = (
  Path(__file__).resolve().parents[2]
  / "main/resources/common-services/AMBARI_INFRA_SOLR/3.0.0"
)
SCRIPTS = SERVICE / "package/scripts"
PID_FILE = "/var/run/ambari-infra-solr/solr-8886.pid"
DATA_DIR = "/var/lib/ambari-infra-solr/data"
TOKENS = (
  "-Djetty.port=8886",
  f"-Dsolr.solr.home={DATA_DIR}",
  "-jar",
  "start.jar",
)
IDENTITY = safe_process.ProcessIdentity(
  123,
  1001,
  456,
  ("/usr/bin/java", *TOKENS),
)


def load_module(name, path, dependencies=None):
  spec = importlib.util.spec_from_file_location(name, path)
  module = importlib.util.module_from_spec(spec)
  with patch.dict(sys.modules, dependencies or {}):
    spec.loader.exec_module(module)
  return module


def params_module(**values):
  module = ModuleType("params")
  for name, value in values.items():
    setattr(module, name, value)
  return module


UTILS = load_module("bigtop_infra_solr_utils", SCRIPTS / "infra_solr_utils.py")
PROCESS = load_module(
  "bigtop_infra_solr_process",
  SCRIPTS / "infra_solr_process.py",
  {"infra_solr_utils": UTILS},
)
SETUP_SCRIPT = load_module(
  "bigtop_infra_solr_runtime_setup",
  SCRIPTS / "setup_infra_solr.py",
  {"infra_solr_utils": UTILS},
)
SETUP = ModuleType("setup_infra_solr")
SETUP.setup_infra_solr = MagicMock()
SETUP.setup_solr_znode_env = MagicMock()
SERVICE_SCRIPT = load_module(
  "bigtop_infra_solr_service",
  SCRIPTS / "infra_solr.py",
  {
    "infra_solr_process": PROCESS,
    "infra_solr_utils": UTILS,
    "setup_infra_solr": SETUP,
  },
)


class TestInfraSolrUtilities(unittest.TestCase):
  def test_bigtop_paths_and_zookeeper_values_fail_closed(self):
    self.assertEqual("3.3.0", UTILS.validate_bigtop_stack("BIGTOP", "3.3.0"))
    self.assertEqual("/infra-solr", UTILS.validate_znode("/infra-solr/"))
    self.assertEqual(
      "zk1.example.com:2181,zk2.example.com:2181",
      UTILS.validate_zookeeper_quorum(
        "zk1.example.com:2181,zk2.example.com:2181"
      ),
    )
    with self.assertRaises(Fail):
      UTILS.validate_bigtop_stack("OTHER", "3.3.0")
    with self.assertRaises(Fail):
      UTILS.validate_znode("/")
    for path_value in ("relative", "/tmp/../etc", "/tmp/with space", "/usr/lib"):
      with self.subTest(path_value=path_value), self.assertRaises(Fail):
        UTILS.validate_service_directory(path_value, "directory")
    with self.assertRaisesRegex(Fail, "managed argument"):
      UTILS.validate_extra_java_options("-Djetty.port=9999")
    for options in ("-Dcustom='two words'", "-Dcustom=*"):
      with self.subTest(options=options), self.assertRaisesRegex(Fail, "unsafe"):
        UTILS.validate_extra_java_options(options)

  def test_runtime_requires_java17_without_legacy_gc_contracts(self):
    params_source = (SCRIPTS / "params.py").read_text(encoding="utf-8")
    config_source = (SERVICE / "configuration/infra-solr-env.xml").read_text(
      encoding="utf-8"
    )
    solr_xml_source = (SERVICE / "properties/solr.xml.j2").read_text(
      encoding="utf-8"
    )
    self.assertIn("java_version < 17", params_source)
    self.assertIn("requires at least one server host", params_source)
    self.assertIn("server hosts must be unique", params_source)
    for obsolete in ("ConcMarkSweep", "UseParNewGC", "ParNew", "CMS-"):
      self.assertNotIn(obsolete, config_source)
      self.assertNotIn(obsolete, solr_xml_source)


class TestInfraSolrConfiguration(unittest.TestCase):
  def test_server_files_use_restricted_non_executable_modes(self):
    params = params_module(
      infra_solr_log_dir="/var/log/ambari-infra-solr",
      infra_solr_piddir="/var/run/ambari-infra-solr",
      infra_solr_datadir="/var/lib/ambari-infra-solr/data",
      infra_solr_data_resources_dir="/var/lib/ambari-infra-solr/data/resources",
      infra_solr_user="infra-solr",
      user_group="hadoop",
      infra_solr_conf="/etc/ambari-infra-solr/conf",
      infra_solr_include="/etc/ambari-infra-solr/conf/infra-solr-env.sh",
      solr_env_content="SOLR_PORT=8886",
      solr_xml_content="<solr/>",
      solr_log4j_content="<Configuration/>",
      infra_solr_security_json_content="",
      infra_solr_ssl_enabled=False,
      security_enabled=True,
      infra_solr_kerberos_keytab="/etc/security/keytabs/infra.keytab",
      infra_solr_web_kerberos_keytab="/etc/security/keytabs/spnego.keytab",
      infra_solr_jaas_file="/etc/ambari-infra-solr/conf/infra_solr_jaas.conf",
      limits_conf_dir="/etc/security/limits.d",
    )
    with (
      patch.dict(sys.modules, {"params": params}),
      patch.object(SETUP_SCRIPT, "Directory"),
      patch.object(SETUP_SCRIPT, "File") as file_resource,
      patch.object(UTILS, "validate_keytab"),
      patch.object(SETUP_SCRIPT.os.path, "exists", return_value=False),
    ):
      SETUP_SCRIPT.setup_infra_solr("server")
    files = {call.args[0]: call.kwargs for call in file_resource.call_args_list}
    self.assertEqual(0o600, files[params.infra_solr_include]["mode"])
    self.assertEqual("infra-solr", files[params.infra_solr_include]["owner"])
    solr_xml = files[f"{params.infra_solr_datadir}/solr.xml"]
    self.assertEqual(("infra-solr", 0o600), (solr_xml["owner"], solr_xml["mode"]))
    self.assertEqual(
      0o644, files[f"{params.infra_solr_conf}/log4j2.xml"]["mode"]
    )
    jaas = files[params.infra_solr_jaas_file]
    self.assertEqual(("infra-solr", 0o400), (jaas["owner"], jaas["mode"]))
    security = files[f"{params.infra_solr_conf}/security.json"]
    self.assertEqual(
      ("root", "root", 0o600),
      (security["owner"], security["group"], security["mode"]),
    )


class TestInfraSolrProcess(unittest.TestCase):
  def test_process_identity_uses_exact_port_home_and_start_jar_tokens(self):
    self.assertEqual(TOKENS, PROCESS.process_tokens(8886, DATA_DIR))
    with self.assertRaises(Fail):
      PROCESS.validate_pid_file(
        "/var/run/ambari-infra-solr/unrelated.pid", 8886
      )

  def test_missing_pid_is_uniquely_recovered_and_published_0640(self):
    with (
      patch.object(safe_process, "read_pid", return_value=None),
      patch.object(
        safe_process, "discover_running_process", return_value=IDENTITY
      ) as discover,
      patch.object(
        safe_process, "create_pid_file_for_identity", return_value=IDENTITY
      ) as publish,
    ):
      recovered = PROCESS.read_or_recover_process(
        PID_FILE, "infra-solr", "hadoop", 8886, DATA_DIR
      )
    self.assertIs(IDENTITY, recovered)
    discover.assert_called_once_with("infra-solr", TOKENS)
    publish.assert_called_once_with(
      PID_FILE,
      IDENTITY,
      expected_user="infra-solr",
      expected_cmdline=TOKENS,
      owner="infra-solr",
      group="hadoop",
      mode=0o640,
    )

  def test_ambiguous_recovery_fails_without_signaling(self):
    with (
      patch.object(safe_process, "read_pid", return_value=None),
      patch.object(
        safe_process,
        "discover_running_process",
        side_effect=Fail("ambiguous process discovery"),
      ),
      patch.object(safe_process, "terminate_process") as terminate,
      self.assertRaisesRegex(Fail, "ambiguous"),
    ):
      PROCESS.stop_process(
        PID_FILE, "infra-solr", "hadoop", 8886, DATA_DIR
      )
    terminate.assert_not_called()

  def test_stop_uses_identity_safe_term_wait_kill_contract(self):
    with (
      patch.object(PROCESS, "read_or_recover_process", return_value=IDENTITY),
      patch.object(safe_process, "terminate_process") as terminate,
      patch.object(safe_process, "remove_pid_file_if_stopped") as remove,
    ):
      self.assertTrue(
        PROCESS.stop_process(
          PID_FILE, "infra-solr", "hadoop", 8886, DATA_DIR
        )
      )
    terminate.assert_called_once_with(
      IDENTITY,
      "infra-solr",
      TOKENS,
      term_wait_attempts=30,
      term_wait_sleep=1,
      kill_wait_attempts=10,
      kill_wait_sleep=1,
    )
    remove.assert_called_once_with(
      PID_FILE,
      123,
      expected_user="infra-solr",
      expected_cmdline=TOKENS,
    )

  def test_launcher_pid_is_secured_through_nofollow_identity_contract(self):
    with (
      patch.object(
        safe_process, "wait_for_discovered_process", return_value=IDENTITY
      ),
      patch.object(safe_process, "read_pid", return_value=IDENTITY.pid),
      patch.object(
        safe_process, "secure_pid_file_for_identity", return_value=IDENTITY
      ) as secure,
    ):
      published = PROCESS.wait_for_started_process(
        PID_FILE, "infra-solr", "hadoop", 8886, DATA_DIR
      )

    self.assertIs(IDENTITY, published)
    secure.assert_called_once_with(
      PID_FILE,
      IDENTITY,
      expected_user="infra-solr",
      expected_cmdline=TOKENS,
      owner="infra-solr",
      group="hadoop",
      mode=0o640,
    )

  def test_launcher_pid_mismatch_fails_before_permission_change(self):
    with (
      patch.object(
        safe_process, "wait_for_discovered_process", return_value=IDENTITY
      ),
      patch.object(safe_process, "read_pid", return_value=999),
      patch.object(safe_process, "secure_pid_file_for_identity") as secure,
      self.assertRaisesRegex(Fail, "contains 999"),
    ):
      PROCESS.wait_for_started_process(
        PID_FILE, "infra-solr", "hadoop", 8886, DATA_DIR
      )
    secure.assert_not_called()


class TestInfraSolrLifecycle(unittest.TestCase):
  def service_params(self):
    return params_module(
      infra_solr_pidfile=PID_FILE,
      infra_solr_user="infra-solr",
      user_group="hadoop",
      infra_solr_port=8886,
      infra_solr_datadir=DATA_DIR,
      infra_solr_log_dir="/var/log/ambari-infra-solr",
      infra_solr_znode="/infra-solr",
      zk_quorum="zk1.example.com:2181",
      security_enabled=False,
      solr_executable="/usr/lib/ambari-infra-solr/bin/solr",
      infra_solr_include="/etc/ambari-infra-solr/conf/infra-solr-env.sh",
    )

  def test_start_is_idempotent(self):
    params = self.service_params()
    with (
      patch.dict(sys.modules, {"params": params}),
      patch.object(PROCESS, "read_or_recover_process", return_value=IDENTITY),
      patch.object(SERVICE_SCRIPT, "Execute") as execute,
      patch.object(SERVICE_SCRIPT, "setup_infra_solr"),
      patch.object(SERVICE_SCRIPT, "setup_solr_znode_env"),
    ):
      SERVICE_SCRIPT.InfraSolr().start(MagicMock())
    execute.assert_not_called()

  def test_start_uses_structured_solr_811_cloud_contract(self):
    params = self.service_params()
    with (
      patch.dict(sys.modules, {"params": params}),
      patch.object(PROCESS, "read_or_recover_process", return_value=None),
      patch.object(PROCESS, "wait_for_started_process", return_value=IDENTITY),
      patch.object(UTILS, "validate_executable"),
      patch.object(SERVICE_SCRIPT, "Execute") as execute,
      patch.object(SERVICE_SCRIPT, "setup_infra_solr"),
      patch.object(SERVICE_SCRIPT, "setup_solr_znode_env"),
    ):
      SERVICE_SCRIPT.InfraSolr().start(MagicMock())
    command = execute.call_args.args[0]
    self.assertEqual("/usr/lib/ambari-infra-solr/bin/solr", command[0])
    self.assertEqual(("start", "-cloud", "-noprompt"), command[1:4])
    self.assertEqual(("-p", "8886", "-s", DATA_DIR), command[4:8])
    self.assertEqual(("-z", "zk1.example.com:2181/infra-solr"), command[8:])

  def test_start_failure_attempts_only_identity_safe_cleanup(self):
    params = self.service_params()
    with (
      patch.dict(sys.modules, {"params": params}),
      patch.object(PROCESS, "read_or_recover_process", return_value=None),
      patch.object(PROCESS, "wait_for_started_process", side_effect=Fail("start")),
      patch.object(PROCESS, "stop_process", return_value=True) as stop,
      patch.object(UTILS, "validate_executable"),
      patch.object(SERVICE_SCRIPT, "Execute"),
      patch.object(SERVICE_SCRIPT, "setup_infra_solr"),
      patch.object(SERVICE_SCRIPT, "setup_solr_znode_env"),
      patch.object(SERVICE_SCRIPT, "show_logs"),
      self.assertRaisesRegex(Fail, "start"),
    ):
      SERVICE_SCRIPT.InfraSolr().start(MagicMock())
    stop.assert_called_once_with(
      PID_FILE, "infra-solr", "hadoop", 8886, DATA_DIR
    )


if __name__ == "__main__":
  unittest.main()
