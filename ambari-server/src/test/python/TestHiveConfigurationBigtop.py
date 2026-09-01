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

import json
from pathlib import Path
import unittest
import xml.etree.ElementTree as ET


HIVE = (
  Path(__file__).resolve().parents[2]
  / "main/resources/stacks/BIGTOP/3.2.0/services/HIVE"
)
HIVE_33 = (
  Path(__file__).resolve().parents[2]
  / "main/resources/stacks/BIGTOP/3.3.0/services/HIVE"
)


def properties(path):
  root = ET.parse(path).getroot()
  result = {}
  for property_element in root.findall("property"):
    name = property_element.findtext("name")
    if name in result:
      raise AssertionError(f"Duplicate property {name} in {path}")
    result[name] = property_element.findtext("value") or ""
  return result


class TestHiveConfigurationContract(unittest.TestCase):
  def test_hive_and_webhcat_env_are_jdk17_safe_and_shell_quoted(self):
    hive_env = properties(HIVE / "configuration/hive-env.xml")
    content = hive_env["content"]
    self.assertIn("umask 0027", content)
    self.assertIn("{{hadoop_home_shell}}", content)
    self.assertIn("{{hive_home_shell}}", content)
    self.assertIn("-XX:+UseG1GC", content)
    self.assertIn("-Xlog:gc*", content)
    for obsolete in (
      "UseConcMarkSweepGC",
      "PrintGCDetails",
      "PrintGCTimeStamps",
      "MaxPermSize",
      "sqla_db_used",
      "jdbc_libs_dir",
    ):
      with self.subTest(obsolete=obsolete):
        self.assertNotIn(obsolete, content)

    webhcat_env = properties(HIVE / "configuration/webhcat-env.xml")["content"]
    self.assertIn("export WEBHCAT_PID_DIR={{templeton_pid_dir_shell}}", webhcat_env)
    self.assertIn("export PID_FILE={{webhcat_pid_file_shell}}", webhcat_env)
    self.assertIn("{{templeton_log_dir_shell}}", webhcat_env)

  def test_bigtop_packaging_paths_and_service_user_are_exact(self):
    hive_env = properties(HIVE / "configuration/hive-env.xml")
    self.assertEqual("hive", hive_env["webhcat_user"])
    self.assertEqual("/var/log/hive-hcatalog", hive_env["hcat_log_dir"])
    self.assertEqual("/var/lib/hive-hcatalog", hive_env["hcat_pid_dir"])
    self.assertNotIn("hive_ambari_database", hive_env)
    self.assertNotIn("hive_database_name", hive_env)
    for name in (
      "alert_ldap_username",
      "alert_ldap_password",
      "alert_pam_username",
      "alert_pam_password",
      "alert_custom_username",
      "alert_custom_password",
    ):
      self.assertIn(name, hive_env)

    webhcat_site = properties(HIVE / "configuration/webhcat-site.xml")
    self.assertEqual("/usr/bin/python3", webhcat_site["templeton.python"])
    self.assertEqual("/usr/bin/hcat", webhcat_site["templeton.hcat"])

    params_source = (HIVE / "package/scripts/params.py").read_text()
    self.assertIn('hive_home = os.path.join(stack_root, "current", "hive-client")', params_source)
    self.assertIn(
      'hive_hcatalog_home = os.path.join(stack_root, "current", "hive-webhcat")',
      params_source,
    )
    self.assertNotIn("/usr/lib/hive", params_source)
    self.assertNotIn("/usr/lib/zookeeper", params_source)
    self.assertNotIn("/usr/hdp", params_source)
    self.assertNotIn("/usr/lib/tez", params_source)
    self.assertNotIn("sap.jdbc4.sqlanywhere", params_source)
    for obsolete_tez_parameter in (
      "tez_local_api_jars",
      "tez_local_lib_jars",
      "tez_lib_uris",
      'tez_user = config["configurations"]["tez-env"]["tez_user"]',
    ):
      self.assertNotIn(obsolete_tez_parameter, params_source)

    hcat_source = (HIVE / "package/scripts/hcat.py").read_text()
    self.assertIn("update_credential_provider_path", hcat_source)
    hive_source = (HIVE / "package/scripts/hive.py").read_text()
    self.assertNotIn("mode=0o777", hive_source)
    self.assertIn("mode=0o1777", hive_source)
    self.assertNotIn('"-passWord"', hive_source)
    self.assertNotIn('"-userName"', hive_source)

    for relative_path in (
      "package/scripts/service_check.py",
      "package/alerts/alert_hive_thrift_port.py",
      "package/scripts/mysql_users.py",
    ):
      command_source = (HIVE / relative_path).read_text()
      self.assertNotIn("PasswordString", command_source)
      self.assertNotIn('"-p"', command_source)

  def test_default_database_uses_preinstalled_mariadb_connector(self):
    hive_site = properties(HIVE / "configuration/hive-site.xml")
    self.assertEqual(
      "org.mariadb.jdbc.Driver",
      hive_site["javax.jdo.option.ConnectionDriverName"],
    )
    self.assertTrue(
      hive_site["javax.jdo.option.ConnectionURL"].startswith("jdbc:mariadb://")
    )

    params_source = (HIVE / "package/scripts/params.py").read_text()
    hive_source = (HIVE / "package/scripts/hive.py").read_text()
    self.assertIn(
      'mariadb_jdbc_driver_jar = "/usr/share/java/mariadb-java-client.jar"',
      params_source,
    )
    self.assertIn("params.using_system_mariadb_driver", hive_source)
    self.assertIn("params.mariadb_jdbc_driver_jar", hive_source)
    marker_delete = 'File(SYS_DB_CREATED_FILE, action="delete")'
    self.assertIn(marker_delete, hive_source)
    self.assertLess(
      hive_source.index(marker_delete),
      hive_source.index('info_command = _schema_tool_command'),
    )
    self.assertIn("requires the MariaDB JDBC driver", params_source)
    self.assertNotIn("com.mysql.jdbc.Driver", params_source)
    self.assertNotIn("com.microsoft.sqlserver.jdbc.SQLServerDriver", params_source)
    self.assertNotIn("mysql-connector-java", params_source)
    self.assertIn(
      'doAs = as_bool(config["configurations"]["hive-site"]',
      params_source,
    )
    self.assertNotIn(
      'hive_site_config["hive.server2.enable.doAs"] =',
      params_source,
    )
    self.assertNotIn(
      "application-properties",
      (HIVE / "configuration/hive-env.xml").read_text(),
    )
    self.assertNotIn(
      "application-properties",
      (HIVE / "configuration/hive-site.xml").read_text(),
    )
    status_source = (HIVE / "package/scripts/status_params.py").read_text()
    self.assertIn("os.path.normpath", status_source)
    self.assertIn('"/lib/systemd/system/{0}.service"', status_source)
    self.assertIn('POSSIBLE_DAEMON_NAMES = ["mariadb", "mysqld", "mysql"]', status_source)

  def test_metadata_declares_real_configurations_and_bounded_check(self):
    metadata = ET.parse(HIVE / "metainfo.xml").getroot()
    dependencies = {
      node.text
      for node in metadata.findall(
        "./services/service/configuration-dependencies/config-type"
      )
    }
    self.assertTrue(
      {
        "hive-site",
        "hive-env",
        "hive-log4j2",
        "hive-exec-log4j2",
        "beeline-log4j2",
        "hivemetastore-site",
        "hiveserver2-site",
        "webhcat-site",
        "webhcat-env",
        "webhcat-log4j",
        "hcat-env",
        "hdfs-site",
        "hadoop-env",
        "yarn-site",
        "zookeeper-env",
      }.issubset(dependencies)
    )
    self.assertNotIn("hivemetastore-site.xml", dependencies)
    self.assertNotIn("druid-common", dependencies)
    self.assertNotIn("application.properties", dependencies)
    self.assertEqual(
      "900", metadata.findtext("./services/service/commandScript/timeout")
    )

  def test_no_interactive_llap_or_hdp_surface_remains(self):
    text_extensions = {".py", ".xml", ".json", ".j2", ".sh", ".sql"}
    source = "\n".join(
      path.read_text(errors="replace")
      for path in HIVE.rglob("*")
      if path.is_file() and path.suffix in text_extensions
    ).lower()
    for obsolete in (
      "hive_server_interactive",
      "hive-interactive-",
      "llap",
      "/usr/hdp",
      "hdp-",
      "hive_pid_utils",
    ):
      with self.subTest(obsolete=obsolete):
        self.assertNotIn(obsolete, source)

    obsolete_paths = (
      "package/files/hcatSmoke.sh",
      "package/files/templetonSmoke.sh",
      "package/files/addMysqlUser.sh",
      "package/etc/hive-schema-0.12.0.mysql.sql",
      "package/templates/startHiveserver2Interactive.sh.j2",
    )
    for relative_path in obsolete_paths:
      self.assertFalse((HIVE / relative_path).exists(), relative_path)

  def test_alerts_reference_only_declared_bigtop_components(self):
    alerts = json.loads((HIVE / "alerts.json").read_text())
    self.assertEqual(
      {"HIVE_METASTORE", "HIVE_SERVER"}, set(alerts["HIVE"])
    )
    metadata = ET.parse(HIVE / "metainfo.xml").getroot()
    declared = {
      node.text for node in metadata.findall(".//components/component/name")
    }
    self.assertTrue(set(alerts["HIVE"]).issubset(declared))

    logfeeder_template = HIVE / "package/templates/input.config-hive.json.j2"
    self.assertTrue(logfeeder_template.is_file())
    self.assertNotIn("interactive", logfeeder_template.read_text().lower())

  def test_bigtop_33_overlay_uses_the_bom_hive_version(self):
    metadata = ET.parse(HIVE_33 / "metainfo.xml").getroot()
    self.assertEqual("HIVE", metadata.findtext("./services/service/name"))
    self.assertEqual("3.1.3-1", metadata.findtext("./services/service/version"))


if __name__ == "__main__":
  unittest.main()
