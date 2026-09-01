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
from unittest.mock import MagicMock, call, patch
import xml.etree.ElementTree as ET

from resource_management.core.exceptions import Fail


HDFS = (
  Path(__file__).resolve().parents[2]
  / "main/resources/stacks/BIGTOP/3.2.0/services/HDFS"
)
SCRIPTS = HDFS / "package/scripts"


def dependency_module(name, **attributes):
  module = ModuleType(name)
  for attribute, value in attributes.items():
    setattr(module, attribute, value)
  return module


def load_script(module_name, filename, dependencies=None):
  spec = importlib.util.spec_from_file_location(module_name, SCRIPTS / filename)
  module = importlib.util.module_from_spec(spec)
  with patch.dict(sys.modules, dependencies or {}):
    spec.loader.exec_module(module)
  return module


HDFS_PROCESS = load_script("bigtop_hdfs_process", "hdfs_process.py")

HDFS_UTILS = dependency_module(
  "utils",
  get_dfsadmin_base_command=MagicMock(return_value="hdfs dfsadmin"),
  set_up_zkfc_security=MagicMock(),
  service=MagicMock(),
  safe_zkfc_op=MagicMock(),
  is_previous_fs_image=MagicMock(return_value=False),
)
HDFS_NAMENODE = load_script(
  "bigtop_hdfs_namenode",
  "hdfs_namenode.py",
  {
    "hdfs_process": HDFS_PROCESS,
    "utils": HDFS_UTILS,
    "setup_ranger_hdfs": dependency_module(
      "setup_ranger_hdfs",
      setup_ranger_hdfs=MagicMock(),
      create_ranger_audit_hdfs_directories=MagicMock(),
    ),
    "namenode_upgrade": dependency_module("namenode_upgrade"),
  },
)

NAMENODE_UPGRADE = load_script(
  "bigtop_namenode_upgrade",
  "namenode_upgrade.py",
  {
    "utils": dependency_module(
      "utils", get_dfsadmin_base_command=MagicMock(return_value="hdfs dfsadmin")
    ),
    "namenode_ha_state": dependency_module(
      "namenode_ha_state", NamenodeHAState=MagicMock
    ),
  },
)

SERVICE_CHECK = load_script(
  "bigtop_hdfs_service_check",
  "service_check.py",
  {"hdfs_process": HDFS_PROCESS},
)
HDFS_RUNTIME_UTILS = load_script(
  "bigtop_hdfs_runtime_utils",
  "utils.py",
  {
    "hdfs_process": HDFS_PROCESS,
    "zkfc_slave": dependency_module("zkfc_slave", ZkfcSlaveDefault=MagicMock),
  },
)

NAMENODE = load_script(
  "bigtop_namenode",
  "namenode.py",
  {
    "namenode_upgrade": NAMENODE_UPGRADE,
    "hdfs_namenode": dependency_module(
      "hdfs_namenode",
      namenode=MagicMock(),
      wait_for_safemode_off=MagicMock(),
      refreshProxyUsers=MagicMock(),
      format_namenode=MagicMock(),
    ),
    "hdfs": dependency_module("hdfs", hdfs=MagicMock(), reconfig=MagicMock()),
    "hdfs_rebalance": dependency_module("hdfs_rebalance"),
    "utils": dependency_module(
      "utils",
      initiate_safe_zkfc_failover=MagicMock(),
      get_hdfs_binary=MagicMock(return_value="hdfs"),
      get_dfsadmin_base_command=MagicMock(return_value="hdfs dfsadmin"),
    ),
  },
)


def params_module(**values):
  return dependency_module("params", **values)


class TestHdfsBigtop(unittest.TestCase):
  def test_process_identity_uses_component_marker_and_hadoop_class(self):
    self.assertEqual(
      (
        "-Dproc_zkfc",
        "org.apache.hadoop.hdfs.tools.DFSZKFailoverController",
      ),
      HDFS_PROCESS.expected_cmdline("zkfc"),
    )
    self.assertEqual(
      (
        "-Dproc_datanode",
        "org.apache.hadoop.hdfs.server.datanode.SecureDataNodeStarter",
      ),
      HDFS_PROCESS.expected_cmdline("datanode", privileged=True),
    )
    with self.assertRaisesRegex(Fail, "Unsupported HDFS process"):
      HDFS_PROCESS.expected_cmdline("unknown")

  def test_process_recovery_rejects_wrong_pid_identity(self):
    with (
      patch.object(HDFS_PROCESS.safe_process, "read_pid", return_value=8123),
      patch.object(
        HDFS_PROCESS.safe_process,
        "read_running_process",
        side_effect=Fail("command line does not match"),
      ),
      patch.object(
        HDFS_PROCESS.safe_process, "remove_pid_file_if_stopped"
      ) as remove_pid,
    ):
      with self.assertRaisesRegex(Fail, "command line does not match"):
        HDFS_PROCESS.recover_running_process(
          "/run/hdfs.pid", "hdfs", "namenode"
        )

    remove_pid.assert_not_called()

  def test_service_start_does_not_duplicate_discovered_process(self):
    params = params_module(
      hadoop_pid_dir_prefix="/run/hadoop",
      hdfs_log_dir_prefix="/var/log/hadoop",
      hadoop_libexec_dir="/usr/lib/hadoop/libexec",
      hadoop_bin="/usr/lib/hadoop/sbin",
      hadoop_conf_dir="/etc/hadoop/conf",
      security_enabled=False,
      user_group="hadoop",
      hdfs_user="hdfs",
      root_user="root",
      root_group="root",
      ulimit_cmd="ulimit -c unlimited ; ",
    )
    identity = MagicMock()
    with (
      patch.dict(sys.modules, {"params": params}),
      patch.object(
        HDFS_RUNTIME_UTILS.hdfs_process,
        "recover_running_process",
        return_value=identity,
      ),
      patch.object(HDFS_RUNTIME_UTILS, "Execute") as execute,
    ):
      HDFS_RUNTIME_UTILS.service(
        action="start", name="namenode", user="hdfs"
      )

    execute.assert_not_called()

  def test_service_stop_never_signals_unvalidated_pid(self):
    params = params_module(
      hadoop_pid_dir_prefix="/run/hadoop",
      hdfs_log_dir_prefix="/var/log/hadoop",
      hadoop_libexec_dir="/usr/lib/hadoop/libexec",
      hadoop_bin="/usr/lib/hadoop/sbin",
      hadoop_conf_dir="/etc/hadoop/conf",
      security_enabled=False,
      user_group="hadoop",
      hdfs_user="hdfs",
      root_user="root",
      root_group="root",
      ulimit_cmd="ulimit -c unlimited ; ",
    )
    with (
      patch.dict(sys.modules, {"params": params}),
      patch.object(
        HDFS_RUNTIME_UTILS.hdfs_process,
        "recover_running_process",
        side_effect=Fail("owner does not match"),
      ),
      patch.object(HDFS_RUNTIME_UTILS, "Execute") as execute,
      patch.object(
        HDFS_RUNTIME_UTILS.hdfs_process, "terminate_process"
      ) as terminate,
    ):
      with self.assertRaisesRegex(Fail, "owner does not match"):
        HDFS_RUNTIME_UTILS.service(
          action="stop", name="namenode", user="hdfs"
        )

    execute.assert_not_called()
    terminate.assert_not_called()

  def test_service_stop_revalidates_identity_before_forced_signal(self):
    params = params_module(
      hadoop_pid_dir_prefix="/run/hadoop",
      hdfs_log_dir_prefix="/var/log/hadoop",
      hadoop_libexec_dir="/usr/lib/hadoop/libexec",
      hadoop_bin="/usr/lib/hadoop/sbin",
      hadoop_conf_dir="/etc/hadoop/conf",
      security_enabled=False,
      user_group="hadoop",
      hdfs_user="hdfs",
      root_user="root",
      root_group="root",
      ulimit_cmd="ulimit -c unlimited ; ",
    )
    identity = MagicMock(pid=8123)
    with (
      patch.dict(sys.modules, {"params": params}),
      patch.object(
        HDFS_RUNTIME_UTILS.hdfs_process,
        "recover_running_process",
        return_value=identity,
      ),
      patch.object(
        HDFS_RUNTIME_UTILS.hdfs_process,
        "wait_for_process_stopped",
        return_value=False,
      ),
      patch.object(
        HDFS_RUNTIME_UTILS.hdfs_process, "terminate_process"
      ) as terminate,
      patch.object(
        HDFS_RUNTIME_UTILS.hdfs_process, "remove_pid_file_if_stopped"
      ) as cleanup,
      patch.object(HDFS_RUNTIME_UTILS, "Execute"),
    ):
      HDFS_RUNTIME_UTILS.service(
        action="stop", name="namenode", user="hdfs"
      )

    terminate.assert_called_once_with(
      identity, "hdfs", "namenode", privileged=False
    )
    cleanup.assert_called_once_with(
      "/run/hadoop/hdfs/hadoop-hdfs-namenode.pid",
      identity,
      "hdfs",
      "namenode",
      privileged=False,
    )

  def test_hdfs_network_port_parser_rejects_partial_and_unsafe_values(self):
    self.assertEqual(9866, HDFS_RUNTIME_UTILS.get_port("0.0.0.0:9866"))
    self.assertEqual(9867, HDFS_RUNTIME_UTILS.get_port("https://[::1]:9867"))
    for address in (
      "host.example.com:9866 trailing",
      "host.example.com:9866/path",
      "host.example.com:70000",
      "host.example.com",
      "hdfs://host.example.com:9866",
      " user:9866",
      "user@host.example.com:9866",
    ):
      with self.subTest(address=address):
        with self.assertRaisesRegex(Fail, "Invalid HDFS network address"):
          HDFS_RUNTIME_UTILS.get_port(address)

  def test_metadata_matches_bigtop_hadoop_and_os_packages(self):
    root = ET.parse(HDFS / "metainfo.xml").getroot()
    service = root.find("./services/service")
    self.assertEqual("3.3.6-1", service.findtext("version"))

    packages_by_family = {}
    for os_specific in service.findall("./osSpecifics/osSpecific"):
      packages_by_family[os_specific.findtext("osFamily")] = [
        package.findtext("name")
        for package in os_specific.findall("./packages/package")
      ]

    rpm_packages = packages_by_family[
      "redhat7,redhat8,redhat9,openeuler22"
    ]
    self.assertIn("hadoop_${stack_version}-libhdfs", rpm_packages)
    self.assertNotIn("snappy-devel", rpm_packages)
    self.assertNotIn("libtirpc-devel", rpm_packages)
    self.assertEqual(
      [
        "hadoop-${stack_version}",
        "hadoop-${stack_version}-client",
        "libhdfs0",
        "ranger-${stack_version}-hdfs-plugin",
      ],
      packages_by_family["ubuntu22"],
    )

  def test_core_site_has_no_hortonworks_or_hdp_user_agent(self):
    core_site = (HDFS / "configuration/core-site.xml").read_text(
      encoding="utf-8"
    )
    self.assertNotIn("Hortonworks", core_site)
    self.assertNotIn("HDP", core_site)
    self.assertEqual(3, core_site.count("Apache Ambari"))

  def test_hadoop_env_uses_jdk17_runtime_options(self):
    hadoop_env = (HDFS / "configuration/hadoop-env.xml").read_text(
      encoding="utf-8"
    )
    self.assertIn("-XX:+UseG1GC", hadoop_env)
    self.assertIn("-Xlog:gc*:", hadoop_env)
    for obsolete in (
      "UseConcMarkSweepGC",
      "CMSInitiatingOccupancyFraction",
      "MaxPermSize",
      "HADOOP_JOBTRACKER_OPTS",
      "HADOOP_TASKTRACKER_OPTS",
      "namenode_opt_permsize",
      "namenode_opt_maxpermsize",
    ):
      with self.subTest(obsolete=obsolete):
        self.assertNotIn(obsolete, hadoop_env)

  def test_hdfs_params_do_not_require_unrelated_service_configs(self):
    params_source = (SCRIPTS / "params_linux.py").read_text(encoding="utf-8")
    for obsolete in (
      'config["configurations"]["falcon-env"]',
      'config["configurations"]["hbase-env"]',
      'config["configurations"]["hive-env"]',
      'config["configurations"]["oozie-env"]',
      'config["configurations"]["yarn-env"]',
      'default("/clusterHostInfo/ganglia_server_hosts"',
      'default("/clusterHostInfo/jtnode_hosts"',
    ):
      with self.subTest(obsolete=obsolete):
        self.assertNotIn(obsolete, params_source)
    for heap_property in (
      "namenode_heapsize",
      "namenode_opt_newsize",
      "namenode_opt_maxnewsize",
      "dtnode_heapsize",
    ):
      with self.subTest(heap_property=heap_property):
        self.assertIn(
          f'"/configurations/hadoop-env/{heap_property}"', params_source
        )

  def test_rolling_restart_timeout_uses_integer_ceiling(self):
    for timeout, expected_retries in ((1, 1), (29, 1), (30, 1), (31, 2)):
      with self.subTest(timeout=timeout):
        options = HDFS_NAMENODE.get_safemode_wait_options(True, str(timeout))
        self.assertEqual(30, options["afterwait_sleep"])
        self.assertEqual(expected_retries, options["retries"])
        self.assertIsInstance(options["retries"], int)
        self.assertEqual(30, options["sleep_seconds"])

  def test_rolling_restart_timeout_rejects_invalid_values(self):
    for timeout in ("not-a-number", "30.5", 0, -1, True):
      with self.subTest(timeout=timeout):
        with self.assertRaisesRegex(Fail, "must be a positive integer"):
          HDFS_NAMENODE.get_safemode_wait_options(True, timeout)

  def test_rolling_restart_without_timeout_uses_legacy_wait_defaults(self):
    for timeout in (None, "", "  "):
      with self.subTest(timeout=timeout):
        self.assertEqual(
          {}, HDFS_NAMENODE.get_safemode_wait_options(True, timeout)
        )

  def test_non_rolling_restart_does_not_consume_timeout(self):
    self.assertEqual(
      {}, HDFS_NAMENODE.get_safemode_wait_options(False, "not-a-number")
    )

  def test_shared_hdfs_tmp_directory_has_sticky_bit(self):
    hdfs_resource = MagicMock()
    params = params_module(
      HdfsResource=hdfs_resource,
      hdfs_tmp_dir="/tmp",
      hdfs_user="hdfs",
      smoke_hdfs_user_dir="/user/ambari-qa",
      smoke_user="ambari-qa",
      smoke_hdfs_user_mode=0o770,
    )

    with patch.dict(sys.modules, {"params": params}):
      HDFS_NAMENODE.create_hdfs_directories("nameservice1")

    self.assertEqual(
      call(
        "/tmp",
        type="directory",
        action="create_on_execute",
        owner="hdfs",
        mode=0o1777,
        nameservices=["nameservice1"],
      ),
      hdfs_resource.call_args_list[0],
    )

  def test_secure_journalnode_failure_preserves_shared_tmp_sticky_bit(self):
    hdfs_resource = MagicMock()
    params = params_module(
      hdfs_tmp_dir="/tmp",
      security_enabled=True,
      kinit_path_local="/usr/bin/kinit",
      hdfs_user_keytab="/etc/security/keytabs/hdfs.headless.keytab",
      hdfs_principal_name="hdfs@example.com",
      hdfs_user="hdfs",
      HdfsResource=hdfs_resource,
      has_journalnode_hosts=True,
      journalnode_hosts=["journalnode.example.com"],
      https_only=False,
      journalnode_port=8480,
      tmp_dir="/tmp",
      smoke_user_keytab="/etc/security/keytabs/smokeuser.headless.keytab",
      smokeuser_principal="ambari-qa@example.com",
      smoke_user="ambari-qa",
      is_namenode_master=False,
    )
    env = MagicMock()

    def render(template):
      return template.format(
        dir=params.hdfs_tmp_dir,
        unique="check-id",
        kinit_path_local=params.kinit_path_local,
        hdfs_user_keytab=params.hdfs_user_keytab,
        hdfs_principal_name=params.hdfs_principal_name,
        host=params.journalnode_hosts[0],
        journalnode_port=params.journalnode_port,
      )

    with (
      patch.dict(sys.modules, {"params": params}),
      patch.object(
        SERVICE_CHECK.functions, "get_unique_id_and_date", return_value="check-id"
      ),
      patch.object(SERVICE_CHECK, "Execute"),
      patch.object(SERVICE_CHECK, "format", side_effect=render),
      patch.object(
        SERVICE_CHECK,
        "curl_krb_request",
        return_value=(False, "connection refused", 1),
      ),
      patch.object(SERVICE_CHECK.Logger, "error") as logger_error,
    ):
      result = SERVICE_CHECK.HdfsServiceCheckDefault().service_check(env)

    self.assertEqual(1, result)
    self.assertEqual(
      call("/tmp", type="directory", action="create_on_execute", mode=0o1777),
      hdfs_resource.call_args_list[0],
    )
    logger_error.assert_called_once_with(
      "Cannot access WEB UI on: http://journalnode.example.com:8480. "
      "Error : connection refused"
    )

  def test_namenode_backup_failure_is_fail_closed(self):
    params = params_module(
      dfs_name_dir="/namenode",
      namenode_backup_dir="/backup",
      stack_version_unformatted="3.2.0",
    )

    with (
      patch.dict(sys.modules, {"params": params}),
      patch.object(
        NAMENODE_UPGRADE.os.path,
        "isdir",
        side_effect=lambda path: path == "/namenode/current",
      ),
      patch.object(NAMENODE_UPGRADE.os, "makedirs"),
      patch.object(NAMENODE_UPGRADE, "Execute", side_effect=OSError("disk full")),
      patch.object(NAMENODE_UPGRADE, "Directory") as cleanup_directory,
      patch.object(
        NAMENODE_UPGRADE, "get_unique_id_and_date", return_value="backup-id"
      ),
      patch.object(NAMENODE_UPGRADE.Logger, "info"),
      patch.object(NAMENODE_UPGRADE.Logger, "error") as logger_error,
    ):
      with self.assertRaisesRegex(Fail, "Could not backup the NameNode Name Dir"):
        NAMENODE_UPGRADE.prepare_upgrade_backup_namenode_dir()

    logger_error.assert_called_once()
    self.assertIn("/namenode/current", logger_error.call_args.args[0])
    cleanup_directory.assert_called_once_with(
      "/backup/3.2.0/namenode_backup-id_1/", action="delete"
    )

  def test_incomplete_backup_is_not_reused_on_same_identifier_retry(self):
    params = params_module(
      dfs_name_dir="/namenode",
      namenode_backup_dir="/backup",
      stack_version_unformatted="3.2.0",
    )
    backup_destination = "/backup/3.2.0/namenode_backup-id_1/"
    destination_state = {"exists": False}

    def create_destination(path):
      self.assertEqual(backup_destination, path)
      destination_state["exists"] = True

    with (
      patch.dict(sys.modules, {"params": params}),
      patch.object(
        NAMENODE_UPGRADE.os.path,
        "isdir",
        side_effect=lambda path: path == "/namenode/current",
      ),
      patch.object(
        NAMENODE_UPGRADE.os.path,
        "lexists",
        side_effect=lambda path: destination_state["exists"],
      ),
      patch.object(
        NAMENODE_UPGRADE.os, "makedirs", side_effect=create_destination
      ) as makedirs,
      patch.object(
        NAMENODE_UPGRADE, "Execute", side_effect=OSError("copy failed")
      ) as execute,
      patch.object(
        NAMENODE_UPGRADE,
        "Directory",
        side_effect=OSError("cleanup failed"),
      ) as cleanup_directory,
      patch.object(
        NAMENODE_UPGRADE, "get_unique_id_and_date", return_value="backup-id"
      ),
      patch.object(NAMENODE_UPGRADE.Logger, "info"),
      patch.object(NAMENODE_UPGRADE.Logger, "warning"),
      patch.object(NAMENODE_UPGRADE.Logger, "error"),
    ):
      for attempt in (1, 2):
        with self.subTest(attempt=attempt):
          with self.assertRaisesRegex(
            Fail, "Could not backup the NameNode Name Dir"
          ):
            NAMENODE_UPGRADE.prepare_upgrade_backup_namenode_dir()

    makedirs.assert_called_once_with(backup_destination)
    execute.assert_called_once_with(
      ("cp", "-ar", "/namenode/current", backup_destination), sudo=True
    )
    cleanup_directory.assert_called_once_with(backup_destination, action="delete")

  def test_backup_failure_stops_upgrade_before_finalize_and_marker(self):
    params = params_module(
      security_enabled=False,
      skip_namenode_save_namespace_express=False,
      skip_namenode_namedir_backup_express=False,
    )
    env = MagicMock()
    namenode = NAMENODE.NameNodeDefault()

    with (
      patch.dict(sys.modules, {"params": params}),
      patch.object(namenode, "get_hdfs_binary", return_value="hdfs"),
      patch.object(NAMENODE.Logger, "info"),
      patch.object(NAMENODE_UPGRADE, "prepare_upgrade_check_for_previous_dir"),
      patch.object(NAMENODE_UPGRADE, "prepare_upgrade_enter_safe_mode"),
      patch.object(NAMENODE_UPGRADE, "prepare_upgrade_save_namespace"),
      patch.object(
        NAMENODE_UPGRADE,
        "prepare_upgrade_backup_namenode_dir",
        side_effect=Fail("backup failed"),
      ) as backup,
      patch.object(
        NAMENODE_UPGRADE, "prepare_upgrade_finalize_previous_upgrades"
      ) as finalize,
      patch.object(NAMENODE_UPGRADE, "prepare_rolling_upgrade") as rolling_upgrade,
      patch.object(NAMENODE_UPGRADE, "create_upgrade_marker") as create_marker,
    ):
      with self.assertRaisesRegex(Fail, "backup failed"):
        namenode.prepare_express_upgrade(env)

    backup.assert_called_once_with()
    finalize.assert_not_called()
    rolling_upgrade.assert_not_called()
    create_marker.assert_not_called()

  def test_service_advisor_has_intact_asf_license_header(self):
    header = (HDFS / "service_advisor.py").read_text(encoding="utf-8")[:800]
    self.assertIn("distributed with this work for additional information", header)
    self.assertNotIn("disass HDFSRecommender", header)


if __name__ == "__main__":
  unittest.main()
