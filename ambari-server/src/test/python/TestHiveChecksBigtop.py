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

from resource_management.core.exceptions import Fail
from resource_management.core.utils import PasswordString


SCRIPTS = (
  Path(__file__).resolve().parents[2]
  / "main/resources/stacks/BIGTOP/3.2.0/services/HIVE/package/scripts"
)
ALERTS = SCRIPTS.parent / "alerts"


def load_script(module_name, filename):
  spec = importlib.util.spec_from_file_location(module_name, SCRIPTS / filename)
  module = importlib.util.module_from_spec(spec)
  sys.modules[module_name] = module
  spec.loader.exec_module(module)
  return module


HCAT_CHECK = load_script("hcat_service_check", "hcat_service_check.py")
WEBHCAT_CHECK = load_script("webhcat_service_check", "webhcat_service_check.py")
SERVICE_CHECK = load_script("bigtop_hive_service_check", "service_check.py")
MYSQL_SERVICE = load_script("mysql_service", "mysql_service.py")
MYSQL_USERS = load_script("mysql_users", "mysql_users.py")
MYSQL_UTILS = load_script("bigtop_hive_mysql_utils", "mysql_utils.py")
RANGER_SETUP = load_script("bigtop_hive_ranger_setup", "setup_ranger_hive.py")


def load_alert(module_name, filename):
  spec = importlib.util.spec_from_file_location(module_name, ALERTS / filename)
  module = importlib.util.module_from_spec(spec)
  spec.loader.exec_module(module)
  return module


THRIFT_ALERT = load_alert("bigtop_hive_thrift_alert", "alert_hive_thrift_port.py")
METASTORE_ALERT = load_alert(
  "bigtop_hive_metastore_alert", "alert_hive_metastore.py"
)


def module_with(**values):
  module = ModuleType("test_params")
  for name, value in values.items():
    setattr(module, name, value)
  return module


class CacheContext:
  def __init__(self):
    self.cache = MagicMock()
    self.cache.environment = {"KRB5CCNAME": "FILE:/private/cache"}
    self.cache.merge_environment.side_effect = lambda value: {
      **value,
      **self.cache.environment,
    }

  def __enter__(self):
    return self.cache

  def __exit__(self, *_):
    return False


class TestHiveServerCheck(unittest.TestCase):
  def params(self, authentication="KERBEROS"):
    return module_with(
      hive_server_hosts=["hs2.example.test"],
      hive_server_port=10000,
      hive_server2_authentication=authentication,
      hive_server_principal="hive/_HOST@EXAMPLE.TEST",
      hive_transport_mode="binary",
      hive_http_endpoint="cliservice",
      hive_ssl=False,
      hive_ssl_keystore_path=None,
      hive_ssl_keystore_password=None,
      hive_ldap_user="ldap;user",
      hive_ldap_passwd="secret;$(id)",
      hive_pam_username="pam user",
      hive_pam_password="pam secret",
      hive_custom_username="custom user",
      hive_custom_password="custom secret",
      hive_bin_dir="/usr/lib/hive/bin",
      hive_user="hive",
      smokeuser="ambari-qa",
      smoke_user_keytab="/etc/security/keytabs/smoke;keytab",
      smokeuser_principal="ambari-qa/host@EXAMPLE.TEST",
      user_group="hadoop",
      tmp_dir="/var/lib/ambari-agent/tmp",
      kinit_path_local="/usr/bin/kinit;$(id)",
      execute_path="/usr/bin",
    )

  def test_beeline_ldap_credentials_remain_individual_argv_tokens(self):
    command = SERVICE_CHECK._beeline_command(self.params("LDAP"), "host;$(id)")
    self.assertIsInstance(command, tuple)
    self.assertEqual("jdbc:hive2://host;$(id):10000/;transportMode=binary", command[2])
    self.assertEqual("ldap;user", command[4])
    self.assertIsInstance(command[6], PasswordString)
    self.assertEqual("secret;$(id)", str(command[6]))
    self.assertEqual(("-e", "show databases"), command[-2:])

  def test_kerberos_check_uses_one_private_cache_for_all_endpoints(self):
    params = self.params()
    context = CacheContext()
    check = object.__new__(SERVICE_CHECK.HiveServiceCheck)
    with patch.object(SERVICE_CHECK, "PrivateKerberosCache", return_value=context) as cache, \
      patch.object(SERVICE_CHECK.shell, "checked_call", return_value=(0, "ok")) as execute:
      check.check_hive_server(params)

    cache.assert_called_once_with(
      "ambari-qa",
      "hadoop",
      "/var/lib/ambari-agent/tmp",
      "ambari-hive-service-check-",
    )
    context.cache.kinit.assert_called_once_with(
      "/usr/bin/kinit;$(id)",
      "/etc/security/keytabs/smoke;keytab",
      "ambari-qa/host@EXAMPLE.TEST",
    )
    self.assertEqual(
      {"KRB5CCNAME": "FILE:/private/cache"}, execute.call_args.kwargs["env"]
    )
    self.assertIsInstance(execute.call_args.args[0], tuple)

  def test_custom_authentication_credentials_remain_individual_argv_tokens(self):
    command = SERVICE_CHECK._beeline_command(self.params("CUSTOM"), "host;$(id)")
    self.assertEqual(("-n", "custom user"), command[3:5])
    self.assertEqual("-p", command[5])
    self.assertIsInstance(command[6], PasswordString)
    self.assertEqual("custom secret", str(command[6]))

  def test_password_authentication_rejects_missing_credentials(self):
    params = self.params("PAM")
    params.hive_pam_password = ""
    with self.assertRaisesRegex(Fail, "PAM service checks require"):
      SERVICE_CHECK._beeline_command(params, "host.example.test")


class TestHCatAndWebHCatChecks(unittest.TestCase):
  def test_hcat_cleanup_runs_when_hdfs_validation_fails(self):
    params = module_with(
      purge_tables="true",
      hive_apps_whs_dir="/warehouse/tablespace/managed/hive",
      security_enabled=False,
      smokeuser="ambari-qa",
      hdfs_user="hdfs",
      user_group="hadoop",
      tmp_dir="/tmp",
      smoke_user_keytab="unused",
      smokeuser_principal="unused",
      hdfs_user_keytab="unused",
      hdfs_principal_name="unused",
      kinit_path_local="/usr/bin/kinit",
    )
    with patch.dict(sys.modules, {"params": params}), \
      patch.object(HCAT_CHECK, "get_unique_id_and_date", return_value="1-2"), \
      patch.object(HCAT_CHECK, "_run_hcat") as run_hcat, \
      patch.object(HCAT_CHECK, "_check_hdfs_path", side_effect=Fail("missing")):
      with self.assertRaisesRegex(Fail, "missing"):
        HCAT_CHECK.hcat_service_check()

    self.assertEqual(2, run_hcat.call_count)
    self.assertIn("create table ambari_hcat_smoke_1_2", run_hcat.call_args_list[0].args[1])
    self.assertEqual(
      "drop table if exists ambari_hcat_smoke_1_2 PURGE",
      run_hcat.call_args_list[1].args[1],
    )

  def test_hcat_cleanup_failure_does_not_hide_primary_failure(self):
    params = module_with(
      purge_tables="false",
      hive_apps_whs_dir="/warehouse/tablespace/managed/hive",
      security_enabled=False,
      smokeuser="ambari-qa",
      hdfs_user="hdfs",
      user_group="hadoop",
      tmp_dir="/tmp",
      smoke_user_keytab="unused",
      smokeuser_principal="unused",
      hdfs_user_keytab="unused",
      hdfs_principal_name="unused",
      kinit_path_local="/usr/bin/kinit",
    )
    with patch.dict(sys.modules, {"params": params}), \
      patch.object(HCAT_CHECK, "get_unique_id_and_date", return_value="1-2"), \
      patch.object(
        HCAT_CHECK,
        "_run_hcat",
        side_effect=(Fail("create failed"), Fail("cleanup failed")),
      ) as run_hcat, \
      patch.object(HCAT_CHECK.Logger, "error") as log_error:
      with self.assertRaisesRegex(Fail, "create failed"):
        HCAT_CHECK.hcat_service_check()

    self.assertEqual(2, run_hcat.call_count)
    log_error.assert_called_once()

  def test_secure_webhcat_check_passes_private_cache_to_structured_curl(self):
    params = module_with(
      webhcat_server_host=["webhcat.example.test"],
      templeton_port=50111,
      security_enabled=True,
      smokeuser="ambari-qa",
      user_group="hadoop",
      tmp_dir="/tmp",
      kinit_path_local="/usr/bin/kinit",
      smoke_user_keytab="/etc/security/keytabs/smoke.keytab",
      smokeuser_principal="ambari-qa@EXAMPLE.TEST",
    )
    context = CacheContext()
    with patch.dict(sys.modules, {"params": params}), \
      patch.object(WEBHCAT_CHECK, "PrivateKerberosCache", return_value=context), \
      patch.object(WEBHCAT_CHECK.shell, "checked_call") as execute:
      WEBHCAT_CHECK.webhcat_service_check()

    self.assertEqual(2, execute.call_count)
    for invocation in execute.call_args_list:
      command = invocation.args[0]
      self.assertIsInstance(command, tuple)
      self.assertEqual("curl", command[0])
      self.assertIn("--negotiate", command)
      self.assertEqual(
        "FILE:/private/cache", invocation.kwargs["env"]["KRB5CCNAME"]
      )


class TestBundledMariaDbWorkflow(unittest.TestCase):
  def test_database_setup_uses_structured_mysql_command_and_hides_password(self):
    params = module_with(
      hive_metastore_user_name="hive",
      hive_db_schema_name="hive",
      hive_metastore_user_passwd="secret;$(id)",
    )
    with patch.dict(sys.modules, {"params": params}), \
      patch.object(MYSQL_USERS, "get_daemon_name", return_value="mariadb"), \
      patch.object(MYSQL_USERS.shell, "call", return_value=(1, "stopped")), \
      patch.object(MYSQL_USERS, "Execute") as execute:
      MYSQL_USERS.mysql_adduser()

    mysql_command = execute.call_args_list[1].args[0]
    self.assertEqual(("mysql", "-u", "root", "-e"), mysql_command[:4])
    self.assertIsInstance(mysql_command[4], PasswordString)
    self.assertIn("`hive`.*", str(mysql_command[4]))
    self.assertEqual(
      [
        call(("service", "mariadb", "start"), sudo=True, logoutput=True),
        execute.call_args_list[1],
        call(("service", "mariadb", "stop"), sudo=True, logoutput=True),
      ],
      execute.call_args_list,
    )

  def test_database_setup_preserves_an_already_running_mariadb(self):
    params = module_with(
      hive_metastore_user_name="hive",
      hive_db_schema_name="hive",
      hive_metastore_user_passwd="secret",
    )
    with patch.dict(sys.modules, {"params": params}), \
      patch.object(MYSQL_USERS, "get_daemon_name", return_value="mariadb"), \
      patch.object(MYSQL_USERS.shell, "call", return_value=(0, "running")), \
      patch.object(MYSQL_USERS, "Execute") as execute:
      MYSQL_USERS.mysql_adduser()

    self.assertEqual(2, execute.call_count)
    self.assertEqual(
      ("service", "mariadb", "restart"),
      execute.call_args_list[0].args[0],
    )
    self.assertEqual(
      ("mysql", "-u", "root", "-e"),
      execute.call_args_list[1].args[0][:4],
    )

  def test_database_configuration_uses_root_owned_mariadb_drop_in(self):
    params = module_with(mysql_conf_dir="/etc/my.cnf.d")
    with patch.dict(sys.modules, {"params": params}), \
      patch.object(MYSQL_UTILS, "Directory") as directory, \
      patch.object(MYSQL_UTILS, "File") as file_resource, \
      patch.object(MYSQL_UTILS.mysql_users, "mysql_adduser") as add_user:
      MYSQL_UTILS.mysql_configure()

    directory.assert_called_once_with(
      "/etc/my.cnf.d",
      owner="root",
      group="root",
      mode=0o755,
      create_parents=True,
    )
    self.assertEqual(
      "/etc/my.cnf.d/99-ambari-hive.cnf",
      file_resource.call_args.args[0],
    )
    self.assertEqual(0o644, file_resource.call_args.kwargs["mode"])
    add_user.assert_called_once_with()

  def test_database_identifiers_reject_sql_metacharacters(self):
    with self.assertRaisesRegex(Fail, "unsupported characters"):
      MYSQL_USERS._sql_identifier("hive'; DROP DATABASE hive", "user")


class TestRangerSetupFailure(unittest.TestCase):
  def test_audit_directory_failure_is_not_suppressed(self):
    params = module_with(
      enable_ranger_hive=True,
      retryAble=False,
      xa_audit_hdfs_is_enabled=True,
      hdfs_user="hdfs",
      hive_user="hive",
      user_group="hadoop",
      HdfsResource=MagicMock(side_effect=Fail("HDFS unavailable")),
    )
    with patch.dict(sys.modules, {"params": params}):
      with self.assertRaisesRegex(Fail, "HDFS unavailable"):
        RANGER_SETUP.setup_ranger_hive()


class TestHiveThriftAlert(unittest.TestCase):
  def test_invalid_port_is_reported_as_unknown(self):
    result, labels = THRIFT_ALERT.execute(
      {THRIFT_ALERT.HIVE_SERVER_THRIFT_PORT_KEY: "not-a-port"},
      {},
      "hive.example.test",
    )

    self.assertEqual("UNKNOWN", result)
    self.assertIn("Invalid HiveServer2 alert configuration", labels[0])

  def test_unknown_transport_is_rejected_before_beeline(self):
    with patch.object(THRIFT_ALERT.shell, "checked_call") as execute:
      result, labels = THRIFT_ALERT.execute(
        {THRIFT_ALERT.HIVE_SERVER_TRANSPORT_MODE_KEY: "legacy"},
        {},
        "hive.example.test",
      )

    self.assertEqual("UNKNOWN", result)
    self.assertIn("Unsupported HiveServer2 transport mode", labels[0])
    execute.assert_not_called()


class TestHiveMetastoreAlert(unittest.TestCase):
  def test_invalid_timeout_is_reported_as_unknown(self):
    result, labels = METASTORE_ALERT.execute(
      {METASTORE_ALERT.HIVE_METASTORE_URIS_KEY: "thrift://hive.example.test:9083"},
      {METASTORE_ALERT.CHECK_COMMAND_TIMEOUT_KEY: "not-a-timeout"},
      "hive.example.test",
    )

    self.assertEqual("UNKNOWN", result)
    self.assertIn("Invalid Hive metastore alert timeout", labels[0])


if __name__ == "__main__":
  unittest.main()
