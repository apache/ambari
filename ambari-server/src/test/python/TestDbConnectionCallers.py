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

from pathlib import Path
from types import ModuleType, SimpleNamespace
import sys
from unittest import TestCase
from unittest.mock import patch

from ambari_commons import import_utils
from resource_management.core.exceptions import Fail


STACK_ROOT = (
  Path(__file__).resolve().parents[2]
  / "main/resources/stacks/BIGTOP"
)
HIVE_PID_STUB = ModuleType("hive_pid_utils")
HIVE_PID_STUB.is_pid_file_process_running = lambda *_args, **_kwargs: False
HIVE_PID_STUB.read_pid = lambda *_args, **_kwargs: None
HIVE_PID_STUB.terminate_process = lambda *_args, **_kwargs: None
with patch.dict(sys.modules, {"hive_pid_utils": HIVE_PID_STUB}):
  HIVE_SERVICE = import_utils.load_source(
    "bigtop_hive_db_connection",
    str(
      STACK_ROOT
      / "3.2.0/services/HIVE/package/scripts/hive_service.py"
    ),
  )
RANGER_SETUP = import_utils.load_source(
  "bigtop_ranger_db_connection",
  str(
    STACK_ROOT
    / "3.3.0/services/RANGER/package/scripts/setup_ranger_xml.py"
  ),
)
RANGER_KMS = import_utils.load_source(
  "bigtop_ranger_kms_db_connection",
  str(STACK_ROOT / "3.3.0/services/RANGER_KMS/package/scripts/kms.py"),
)


def formatter(params):
  values = vars(params).copy()
  values["params"] = params
  return lambda template: template.format(**values)


class TestDbConnectionCallers(TestCase):
  def test_hive_passes_database_secret_only_to_shared_helper(self):
    params = SimpleNamespace(
      jdbc_jar_name="postgresql.jar",
      check_db_connection_jar="/agent/DBConnectionVerification.jar",
      ambari_java_home="/usr/lib/jvm/ambari-java",
      hive_jdbc_connection_url="jdbc:postgresql://db/hive",
      hive_metastore_user_name="hive db user",
      hive_metastore_user_passwd="secret; never-in-argv",
      hive_jdbc_driver="org.postgresql.Driver",
      hive_log_dir="/var/log/hive",
      hive_user="hive",
    )
    failure = Fail("stop after DB verification")
    rendered = {
      "{check_db_connection_jar}": params.check_db_connection_jar,
      "{ambari_java_home}/bin/java": params.ambari_java_home + "/bin/java",
    }

    with patch.dict(sys.modules, {"params": params}), patch.object(
      HIVE_SERVICE, "format", side_effect=rendered.__getitem__
    ), patch.object(
      HIVE_SERVICE, "Execute"
    ) as execute, patch.object(
      HIVE_SERVICE, "show_logs"
    ) as show_logs, patch.object(
      HIVE_SERVICE, "verify_db_connection", side_effect=failure
    ) as verify:
      with self.assertRaisesRegex(Fail, "stop after DB verification"):
        HIVE_SERVICE.validate_connection(
          "/usr/share/java/postgresql.jar", "/usr/lib/hive/lib"
        )

    verify.assert_called_once_with(
      "/usr/lib/jvm/ambari-java/bin/java",
      "/agent/DBConnectionVerification.jar:/usr/share/java/postgresql.jar",
      "jdbc:postgresql://db/hive",
      "hive db user",
      "secret; never-in-argv",
      "org.postgresql.Driver",
      tries=5,
      try_sleep=10,
    )
    execute.assert_not_called()
    show_logs.assert_called_once_with("/var/log/hive", "hive")

  def test_ranger_admin_passes_database_secret_only_to_shared_helper(self):
    params = SimpleNamespace(
      ranger_home="/opt/ranger",
      ranger_conf="/etc/ranger/admin",
      unix_user="ranger",
      unix_group="ranger",
      check_db_connection_jar_name="DBConnectionVerification.jar",
      jdk_location="https://server/resources",
      check_db_connection_jar="/agent/DBConnectionVerification.jar",
      db_flavor="sqla",
      ld_lib_path="/opt/sqlanywhere/lib",
      ambari_java_home="/usr/lib/jvm/ambari-java",
      ranger_jdbc_connection_url="jdbc:sqlanywhere:ranger",
      ranger_db_user="ranger db user",
      ranger_db_password="secret; never-in-argv",
      ranger_jdbc_driver="sap.jdbc4.sqlanywhere.IDriver",
    )
    failure = Fail("stop after DB verification")

    with patch.dict(sys.modules, {"params": params}), patch.object(
      RANGER_SETUP, "Directory"
    ), patch.object(RANGER_SETUP, "File"), patch.object(
      RANGER_SETUP, "Template"
    ), patch.object(
      RANGER_SETUP, "DownloadSource"
    ), patch.object(RANGER_SETUP, "copy_jdbc_connector"), patch.object(
      RANGER_SETUP, "generate_logfeeder_input_config"
    ), patch.object(
      RANGER_SETUP, "format", side_effect=formatter(params)
    ), patch.object(
      RANGER_SETUP, "verify_db_connection", side_effect=failure
    ) as verify:
      with self.assertRaisesRegex(Fail, "stop after DB verification"):
        RANGER_SETUP.setup_ranger_admin(upgrade_type="")

    verify.assert_called_once_with(
      "/usr/lib/jvm/ambari-java/bin/java",
      "/agent/DBConnectionVerification.jar:/opt/ranger/ews/lib/sajdbc4.jar:/opt/ranger/ews/lib/*",
      "jdbc:sqlanywhere:ranger",
      "ranger db user",
      "secret; never-in-argv",
      "sap.jdbc4.sqlanywhere.IDriver",
      environment={"LD_LIBRARY_PATH": "/opt/sqlanywhere/lib"},
      tries=5,
      try_sleep=10,
    )

  def test_ranger_kms_passes_database_secret_only_to_shared_helper(self):
    params = SimpleNamespace(
      has_ranger_admin=True,
      kms_home="/opt/ranger-kms",
      kms_conf_dir="/etc/ranger/kms",
      kms_user="kms",
      kms_group="kms",
      check_db_connection_jar_name="DBConnectionVerification.jar",
      jdk_location="https://server/resources",
      check_db_connection_jar="/agent/DBConnectionVerification.jar",
      db_flavor="sqla",
      kms_lib_path="/opt/ranger-kms/ews/webapp/lib",
      ld_library_path="/opt/sqlanywhere/lib",
      ambari_java_home="/usr/lib/jvm/ambari-java",
      ranger_kms_jdbc_connection_url="jdbc:sqlanywhere:ranger-kms",
      db_user="kms db user",
      db_password="secret; never-in-argv",
      ranger_kms_jdbc_driver="sap.jdbc4.sqlanywhere.IDriver",
    )
    failure = Fail("stop after DB verification")

    with patch.dict(sys.modules, {"params": params}), patch.object(
      RANGER_KMS, "Directory"
    ), patch.object(RANGER_KMS, "File"), patch.object(
      RANGER_KMS, "DownloadSource"
    ), patch.object(
      RANGER_KMS, "copy_jdbc_connector"
    ), patch.object(
      RANGER_KMS, "format", side_effect=formatter(params)
    ), patch.object(
      RANGER_KMS, "verify_db_connection", side_effect=failure
    ) as verify:
      with self.assertRaisesRegex(Fail, "stop after DB verification"):
        RANGER_KMS.kms()

    verify.assert_called_once_with(
      "/usr/lib/jvm/ambari-java/bin/java",
      "/agent/DBConnectionVerification.jar:/opt/ranger-kms/ews/webapp/lib/sajdbc4.jar",
      "jdbc:sqlanywhere:ranger-kms",
      "kms db user",
      "secret; never-in-argv",
      "sap.jdbc4.sqlanywhere.IDriver",
      environment={"LD_LIBRARY_PATH": "/opt/sqlanywhere/lib"},
      tries=5,
      try_sleep=10,
    )
