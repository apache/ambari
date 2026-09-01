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

import ast
import importlib.util
import json
import os
import shlex
import sys
import unittest
import xml.etree.ElementTree as ET
from pathlib import Path
from unittest.mock import patch

from ambari_commons import import_utils
from jinja2 import Environment as JinjaEnvironment, StrictUndefined
from resource_management.core.shell import quote_bash_args


ALLUXIO_SCRIPTS = (
  Path(__file__).resolve().parents[2]
  / "main/resources/stacks/BIGTOP/3.2.0/services/ALLUXIO/package/scripts"
)
ALLUXIO_SERVICE = ALLUXIO_SCRIPTS.parents[1]
ALLUXIO_33_SERVICE = ALLUXIO_SERVICE.parents[2] / "3.3.0/services/ALLUXIO"
ALLUXIO_34_SERVICE = ALLUXIO_SERVICE.parents[2] / "3.4.0/services/ALLUXIO"
STACKS = ALLUXIO_SERVICE.parents[3]


def load_script(module_name, filename):
  spec = importlib.util.spec_from_file_location(
    module_name, ALLUXIO_SCRIPTS / filename
  )
  module = importlib.util.module_from_spec(spec)
  sys.modules[module_name] = module
  spec.loader.exec_module(module)
  return module


ALLUXIO_UTILS = load_script("bigtop_alluxio_utils_config", "alluxio_utils.py")


class TestAlluxioSourceContracts(unittest.TestCase):
  def test_persistent_data_defaults_use_packaged_var_lib_location(self):
    self.assertEqual(
      "/var/lib/alluxio/metastore",
      ALLUXIO_UTILS.resolve_master_metastore_dir({}, "/var/lib/alluxio"),
    )
    self.assertEqual(
      "/var/lib/alluxio/metastore",
      ALLUXIO_UTILS.resolve_master_metastore_dir(
        {"alluxio.master.metastore.dir": None},
        "/var/lib/alluxio",
      ),
    )
    self.assertEqual(
      "/data/alluxio/meta",
      ALLUXIO_UTILS.resolve_master_metastore_dir(
        {"alluxio.master.metastore.dir": "/data/alluxio/meta"},
        "/var/lib/alluxio",
      ),
    )

    root = ET.parse(
      ALLUXIO_SERVICE / "configuration/alluxio-site-properties.xml"
    ).getroot()
    configured_default = next(
      prop.findtext("value")
      for prop in root.findall("property")
      if prop.findtext("name") == "alluxio.master.metastore.dir"
    )
    self.assertEqual("/var/lib/alluxio/metastore", configured_default)

  def test_root_sourced_environment_quotes_java_and_native_library_paths(self):
    root = ET.parse(ALLUXIO_SERVICE / "configuration/alluxio-env.xml").getroot()
    template_text = next(
      prop.findtext("value")
      for prop in root.findall("property")
      if prop.findtext("name") == "content"
    )
    java_home = "/opt/java home;$(touch /tmp/java-injection)"
    hadoop_home = "/opt/hadoop home;$(touch /tmp/hadoop-injection)"
    native_option = "-Djava.library.path=" + os.path.join(
      hadoop_home, "lib", "native"
    )
    java_home_shell = quote_bash_args(java_home)
    native_option_shell = quote_bash_args(native_option)
    rendered = JinjaEnvironment(undefined=StrictUndefined).from_string(
      template_text
    ).render(
      java_home_shell=java_home_shell,
      alluxio_native_library_option_shell=native_option_shell,
      alluxio_log_dir_shell=quote_bash_args("/var/log/alluxio"),
    )
    assignments = {
      line.split("=", 1)[0]: line.split("=", 1)[1]
      for line in rendered.splitlines()
      if line.startswith(
        ("JAVA_HOME=", "ALLUXIO_LOGS_DIR=", "ALLUXIO_NATIVE_LIBRARY_OPTION=")
      )
    }

    self.assertNotIn("/usr/hdp/", template_text)
    self.assertNotIn("{{java_home}}", template_text)
    self.assertNotIn("{{hadoop_home}}", template_text)
    self.assertEqual([java_home], shlex.split(assignments["JAVA_HOME"]))
    self.assertEqual(
      ["/var/log/alluxio"], shlex.split(assignments["ALLUXIO_LOGS_DIR"])
    )
    self.assertEqual(
      [native_option], shlex.split(assignments["ALLUXIO_NATIVE_LIBRARY_OPTION"])
    )
    self.assertIn(
      'ALLUXIO_JAVA_OPTS+=" ${ALLUXIO_NATIVE_LIBRARY_OPTION}"', rendered
    )

  def test_params_cli_and_dead_code_contract(self):
    source = (ALLUXIO_SCRIPTS / "params.py").read_text(encoding="utf-8")
    tree = ast.parse(source)
    assignments = {
      target.id: node.value
      for node in tree.body
      if isinstance(node, ast.Assign)
      for target in node.targets
      if isinstance(target, ast.Name)
    }
    assigned_names = {
      target.id
      for node in ast.walk(tree)
      if isinstance(node, ast.Assign)
      for target in node.targets
      if isinstance(target, ast.Name)
    }
    imported_names = {
      alias.asname or alias.name
      for node in ast.walk(tree)
      if isinstance(node, (ast.Import, ast.ImportFrom))
      for alias in node.names
    }
    tuple_constants = {
      name: tuple(
        item.value
        for item in value.elts
        if isinstance(item, ast.Constant) and isinstance(item.value, str)
      )
      for name, value in assignments.items()
      if isinstance(value, ast.Tuple)
    }
    removed_names = {
      "alluxio_authentication",
      "alluxio_kerberos_keytab",
      "alluxio_kerberos_principal",
      "alluxio_master_format",
      "alluxio_master_metastore_formatted",
      "alluxio_master_pid_cmd",
      "alluxio_master_stop_cmd",
      "alluxio_worker_pid_cmd",
      "alluxio_worker_stop_cmd",
      "alluxio_work_dir",
      "cluster_name",
      "effective_version",
      "fqdn",
      "major_stack_version",
      "retryAble",
      "smoke_user",
      "smoke_user_keytab",
      "smokeuser_principal",
      "stack_name",
      "stack_version",
      "stack_version_formatted",
      "stack_version_unformatted",
      "sudo",
      "sysprep_skip_copy_tarballs_hdfs",
      "tmp_dir",
      "user_group",
      "version",
    }
    removed_imports = {
      "AMBARI_SUDO_BINARY",
      "format_stack_version",
      "get_current_version",
      "get_major_version",
      "get_sysprep_skip_copy_tarballs_hdfs",
      "socket",
      "urlparse",
    }
    required_names = {
      "alluxio_data_dir",
      "alluxio_journal_dir",
      "alluxio_master_rpc_port",
      "alluxio_master_embedded_journal_port",
      "alluxio_master_start_cmd",
      "alluxio_master_web_port",
      "alluxio_service_kerberos_keytab",
      "alluxio_service_kerberos_principal",
      "alluxio_test_cmd",
      "alluxio_worker_mount_cmd",
      "alluxio_worker_rpc_port",
      "alluxio_worker_start_cmd",
      "alluxio_worker_web_port",
      "alluxio_native_library_option_shell",
      "alluxio_log_dir_shell",
      "alluxio_metrics_properties",
      "config",
      "hadoop_conf_dir",
      "hadoop_home",
      "kinit_path_local",
      "kinit_principal",
      "java_home_shell",
      "security_enabled",
      "stack_root",
      "worker_mem",
    }

    self.assertTrue(removed_names.isdisjoint(assigned_names))
    self.assertTrue(removed_imports.isdisjoint(imported_names))
    self.assertTrue(required_names.issubset(assigned_names))
    self.assertIn("quote_bash_args", imported_names)
    module_assignments = [
      target.id
      for node in tree.body
      if isinstance(node, ast.Assign)
      for target in node.targets
      if isinstance(target, ast.Name)
    ]
    self.assertEqual(1, module_assignments.count("config"))
    self.assertEqual(1, module_assignments.count("stack_root"))
    self.assertEqual(
      1,
      sum(
        alias.name == "functools"
        for node in tree.body
        if isinstance(node, ast.Import)
        for alias in node.names
      ),
    )
    self.assertEqual(
      ("-a", "-N", "master"), tuple_constants["alluxio_master_start_cmd"]
    )
    self.assertEqual(
      ("-a", "-N", "worker", "NoMount"),
      tuple_constants["alluxio_worker_start_cmd"],
    )
    self.assertEqual(("runTests",), tuple_constants["alluxio_test_cmd"])
    self.assertNotIn("formatMaster", source)
    self.assertNotIn("alluxio-stop.sh", source)
    self.assertNotIn("ps -A", source)
    self.assertNotIn("alluxio_serive_kerberos_principal", source)
    self.assertNotIn("from resource_management import *", source)
    self.assertIn('alluxio_data_dir = "/var/lib/alluxio"', source)
    self.assertIn(
      'alluxio_journal_dir = os.path.join(alluxio_data_dir, "journal")', source
    )
    self.assertIn("java_home_shell = quote_bash_args(str(java_home))", source)
    self.assertIn(
      "alluxio_native_library_option_shell = quote_bash_args(", source
    )

  def test_alluxio_utils_contains_no_private_process_implementation(self):
    tree = ast.parse(
      (ALLUXIO_SCRIPTS / "alluxio_utils.py").read_text(encoding="utf-8")
    )
    functions = {
      node.name for node in tree.body if isinstance(node, ast.FunctionDef)
    }
    self.assertEqual(
      {
        "_safe_absolute_path",
        "resolve_master_metastore_dir",
        "resolve_underfs_address",
        "validate_data_size",
        "validate_directory_path",
        "validate_keytab_path",
        "validate_port",
        "validate_principal",
        "validate_service_account",
      },
      functions,
    )

  def test_runtime_values_are_validated_before_resource_creation(self):
    self.assertEqual(
      "/var/lib/alluxio/metastore",
      ALLUXIO_UTILS.resolve_master_metastore_dir({}, "/var/lib/alluxio"),
    )
    self.assertEqual(
      "alluxio", ALLUXIO_UTILS.validate_service_account("alluxio", "user")
    )
    self.assertEqual("19200", ALLUXIO_UTILS.validate_port(19200, "port"))
    self.assertEqual("19200", ALLUXIO_UTILS.validate_port(" 19200 ", "port"))
    self.assertEqual("512MB", ALLUXIO_UTILS.validate_data_size(" 512MB ", "size"))

    for value in (
      "/",
      "/etc",
      "/etc/alluxio",
      "/tmp/alluxio",
      "/var/log/../lib/alluxio",
      "relative/alluxio",
    ):
      with self.subTest(directory=value):
        with self.assertRaises(ValueError):
          ALLUXIO_UTILS.validate_directory_path(value, "directory")
    for value in ("root", "alluxio;id", "1alluxio", "alluxio\nroot"):
      with self.subTest(account=value):
        with self.assertRaises(ValueError):
          ALLUXIO_UTILS.validate_service_account(value, "account")
    for value in (True, 1.5, 0, 65536, "+1", "1.5", "1;id"):
      with self.subTest(port=value):
        with self.assertRaises(ValueError):
          ALLUXIO_UTILS.validate_port(value, "port")
    for value in ("0GB", "1.5GB", "1GB;id", "GB", None):
      with self.subTest(size=value):
        with self.assertRaises(ValueError):
          ALLUXIO_UTILS.validate_data_size(value, "size")

  def test_metastore_directory_rejects_unsafe_configured_paths(self):
    for configured_dir in ("/etc/alluxio", "/tmp/alluxio", "relative"):
      with self.subTest(configured_dir=configured_dir):
        with self.assertRaises(ValueError):
          ALLUXIO_UTILS.resolve_master_metastore_dir(
            {"alluxio.master.metastore.dir": configured_dir},
            "/var/lib/alluxio",
          )

  def test_underfs_address_uses_structured_hdfs_uri_and_rejects_unsafe_paths(self):
    self.assertEqual(
      "hdfs://nameservice1/alluxio/underFSStorage",
      ALLUXIO_UTILS.resolve_underfs_address(
        "hdfs://nameservice1", "/alluxio/underFSStorage"
      ),
    )
    self.assertEqual(
      "viewfs://cluster/base/alluxio",
      ALLUXIO_UTILS.resolve_underfs_address(
        "viewfs://cluster/base", "/alluxio"
      ),
    )
    for default_fs, path in (
      ("file:///", "/alluxio"),
      ("hdfs://nameservice1", "relative"),
      ("hdfs://nameservice1", "/alluxio/../other"),
      ("hdfs://nameservice1", "//other"),
    ):
      with self.subTest(default_fs=default_fs, path=path):
        with self.assertRaises(ValueError):
          ALLUXIO_UTILS.resolve_underfs_address(default_fs, path)

  def test_kerberos_values_reject_property_and_path_injection(self):
    self.assertEqual(
      "/etc/security/keytabs/alluxio.service.keytab",
      ALLUXIO_UTILS.validate_keytab_path(
        "/etc/security/keytabs/alluxio.service.keytab"
      ),
    )
    self.assertEqual(
      "alluxio/_HOST@EXAMPLE.COM",
      ALLUXIO_UTILS.validate_principal("alluxio/_HOST@EXAMPLE.COM"),
    )
    for value in (
      "relative.keytab",
      "/etc/security/keytabs/../other.keytab",
      "/etc/security/keytabs/alluxio.txt",
      "/etc/security/keytabs/alluxio.keytab\nproperty=value",
    ):
      with self.subTest(keytab=value):
        with self.assertRaises(ValueError):
          ALLUXIO_UTILS.validate_keytab_path(value)
    for value in (
      "alluxio/_HOST",
      "alluxio/_HOST@EXAMPLE.COM\nproperty=value",
      "alluxio/_HOST@EXAMPLE.COM;id",
    ):
      with self.subTest(principal=value):
        with self.assertRaises(ValueError):
          ALLUXIO_UTILS.validate_principal(value)


class TestAlluxioSiteTemplate(unittest.TestCase):
  @staticmethod
  def _template_text():
    root = ET.parse(
      ALLUXIO_SERVICE / "configuration/alluxio-site-properties.xml"
    ).getroot()
    return next(
      prop.findtext("value")
      for prop in root.findall("property")
      if prop.findtext("name") == "content"
    )

  @staticmethod
  def _variables(security_enabled):
    variables = {
      "alluxio_master_host": "master.example.com",
      "alluxio_master_embedded_journal_port": "19200",
      "alluxio_journal_dir": "/var/lib/alluxio/journal",
      "alluxio_master_metastore_dir": "/var/lib/alluxio/metastore",
      "alluxio_master_rpc_port": "19998",
      "alluxio_master_web_port": "19999",
      "alluxio_worker_rpc_port": "29998",
      "alluxio_worker_web_port": "30000",
      "hadoop_conf_dir": "/etc/hadoop/conf",
      "master_embedded_journal_addresses_config": "",
      "namenode_address": "hdfs://namenode.example.com:8020",
      "security_enabled": security_enabled,
      "underfs_hdfs_addr": "hdfs://namenode.example.com:8020/alluxio",
      "worker_mem": "1GB",
    }
    if security_enabled:
      variables.update(
        alluxio_service_kerberos_keytab=(
          "/etc/security/keytabs/alluxio.service.keytab"
        ),
        alluxio_service_kerberos_principal="alluxio/_HOST@EXAMPLE.COM",
      )
    return variables

  def test_insecure_template_renders_without_kerberos_or_level1(self):
    rendered = JinjaEnvironment(undefined=StrictUndefined).from_string(
      self._template_text()
    ).render(**self._variables(False))

    self.assertNotIn("alluxio.master.keytab.file", rendered)
    self.assertNotIn("alluxio.master.principal", rendered)
    self.assertNotIn("tieredstore.level1.alias=HDD", rendered)
    self.assertNotIn("hdd_dirs", rendered)
    self.assertIn(
      "alluxio.master.journal.folder=/var/lib/alluxio/journal", rendered
    )
    self.assertIn(
      "alluxio.master.mount.table.root.ufs="
      "hdfs://namenode.example.com:8020/alluxio",
      rendered,
    )
    self.assertNotIn("alluxio.underfs.address=", rendered)
    self.assertIn(
      "alluxio.underfs.hdfs.configuration=/etc/hadoop/conf/core-site.xml:"
      "/etc/hadoop/conf/hdfs-site.xml",
      rendered,
    )
    self.assertIn("alluxio.worker.ramdisk.size=1GB", rendered)
    self.assertIn("alluxio.master.embedded.journal.port=19200", rendered)

  def test_secure_template_renders_guarded_kerberos_properties(self):
    rendered = JinjaEnvironment(undefined=StrictUndefined).from_string(
      self._template_text()
    ).render(**self._variables(True))

    self.assertIn(
      "alluxio.master.keytab.file="
      "/etc/security/keytabs/alluxio.service.keytab",
      rendered,
    )
    self.assertIn(
      "alluxio.master.principal=alluxio/_HOST@EXAMPLE.COM", rendered
    )
    self.assertIn(
      "alluxio.worker.keytab.file="
      "/etc/security/keytabs/alluxio.service.keytab",
      rendered,
    )
    self.assertIn(
      "alluxio.worker.principal=alluxio/_HOST@EXAMPLE.COM", rendered
    )
    self.assertNotIn("alluxio.hadoop.security.authentication", rendered)
    self.assertNotIn("alluxio.security.underfs.hdfs", rendered)
    self.assertIn(
      "alluxio.master.journal.folder=/var/lib/alluxio/journal", rendered
    )
    self.assertNotIn("tieredstore.level1.alias=HDD", rendered)


class TestAlluxioVersionContract(unittest.TestCase):
  def test_stack_versions_and_packages_match_bigtop_releases(self):
    base_path = ALLUXIO_SERVICE / "metainfo.xml"
    overlay_path = ALLUXIO_33_SERVICE / "metainfo.xml"
    overlay_34_path = ALLUXIO_34_SERVICE / "metainfo.xml"
    roots = {
      "3.2.0": ET.parse(base_path).getroot(),
      "3.3.0": ET.parse(overlay_path).getroot(),
      "3.4.0": ET.parse(overlay_34_path).getroot(),
    }
    self.assertEqual("2.8.0-2", roots["3.2.0"].findtext(".//service/version"))
    self.assertEqual("2.9.6-1", roots["3.3.0"].findtext(".//service/version"))
    self.assertEqual("2.9.6-1", roots["3.4.0"].findtext(".//service/version"))

    expected_packages = {
      "3.2.0": {
        "redhat7,redhat8,redhat9,openeuler22": ["alluxio"],
        "debian10,debian11,ubuntu20,ubuntu22": ["alluxio"],
      },
      "3.3.0": {
        "redhat8,redhat9,openeuler22": ["alluxio_${stack_version}"],
        "debian10,debian11,ubuntu20,ubuntu22": ["alluxio-${stack_version}"],
      },
      "3.4.0": {
        "redhat8,redhat9,openeuler22": ["alluxio_${stack_version}"],
        "debian10,debian11,ubuntu20,ubuntu22": ["alluxio"],
      },
    }
    for stack_version, root in roots.items():
      packages = {
        node.findtext("osFamily"): [
          package.findtext("name")
          for package in node.findall("packages/package")
        ]
        for node in root.findall(".//osSpecifics/osSpecific")
      }
      self.assertEqual(expected_packages[stack_version], packages)

    stack_packages = json.loads(
      (STACKS / "BIGTOP/3.2.0/properties/stack_packages.json").read_text(
        encoding="utf-8"
      )
    )["stack-packages"]["ALLUXIO"]
    self.assertEqual({"ALLUXIO_MASTER", "ALLUXIO_WORKER"}, set(stack_packages))
    for component in stack_packages.values():
      self.assertEqual("alluxio", component["STACK-SELECT-PACKAGE"])

  def test_metadata_and_alerts_reference_consumed_configurations(self):
    root = ET.parse(ALLUXIO_SERVICE / "metainfo.xml").getroot()
    config_types = {
      node.text
      for node in root.findall(
        "./services/service/configuration-dependencies/config-type"
      )
    }
    self.assertTrue(
      {"core-site", "hdfs-site", "hadoop-env"}.issubset(config_types)
    )
    alerts = json.loads(
      (ALLUXIO_SERVICE / "alerts.json").read_text(encoding="utf-8")
    )
    alert_source = json.dumps(alerts)
    self.assertIn(
      "alluxio-site-properties/alluxio.master.rpc.port", alert_source
    )
    self.assertIn(
      "alluxio-site-properties/alluxio.worker.rpc.port", alert_source
    )
    self.assertNotIn("alluxio-log4j-properties/alluxio.master", alert_source)

    kerberos = json.loads(
      (ALLUXIO_SERVICE / "kerberos.json").read_text(encoding="utf-8")
    )
    identities = kerberos["services"][0]["identities"]
    self.assertEqual(
      ["alluxio_service_keytab"], [item["name"] for item in identities]
    )
    kerberos_source = json.dumps(kerberos)
    self.assertNotIn("alluxio.headless.keytab", kerberos_source)
    self.assertNotIn("alluxio-env/alluxio_keytab", kerberos_source)


class TestAlluxioServiceAdvisor(unittest.TestCase):
  @classmethod
  def setUpClass(cls):
    for module_name in ("ambari_configuration", "stack_advisor"):
      module_path = STACKS / f"{module_name}.py"
      with module_path.open("rb") as module_file:
        import_utils.load_module(
          module_name,
          module_file,
          str(module_path),
          (".py", "rb", import_utils.PY_SOURCE),
        )
    spec = importlib.util.spec_from_file_location(
      "bigtop_alluxio_service_advisor", ALLUXIO_SERVICE / "service_advisor.py"
    )
    cls.advisor_module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(cls.advisor_module)

  def test_parent_load_failure_propagates_original_error(self):
    advisor_path = ALLUXIO_SERVICE / "service_advisor.py"
    parent_path = ALLUXIO_SERVICE.parents[3] / "service_advisor.py"
    spec = importlib.util.spec_from_file_location(
      "bigtop_alluxio_service_advisor_failure", advisor_path
    )
    advisor_module = importlib.util.module_from_spec(spec)

    with patch.dict(os.environ, {"BASE_SERVICE_ADVISOR": str(parent_path)}), \
      patch(
        "ambari_commons.import_utils.load_module",
        side_effect=RuntimeError("parent load failed"),
      ):
      with self.assertRaisesRegex(RuntimeError, "parent load failed"):
        spec.loader.exec_module(advisor_module)

  def test_legacy_recommender_is_removed(self):
    advisor_source = (ALLUXIO_SERVICE / "service_advisor.py").read_text(
      encoding="utf-8"
    )

    self.assertNotIn("HDP", advisor_source)
    self.assertNotIn("AlluxioRecommender", advisor_source)

  def test_advisor_accepts_defaults_and_rejects_unsafe_runtime_values(self):
    validator = self.advisor_module.AlluxioValidator()
    for value in (1, " 2 ", "03"):
      with self.subTest(value=value):
        self.assertEqual(int(value), self.advisor_module._integer(value))
    for value in (True, 1.5, "1.5", "+1", "-1", "one", None):
      with self.subTest(value=value):
        self.assertIsNone(self.advisor_module._integer(value))

    site = {
      node.findtext("name"): node.findtext("value") or ""
      for node in ET.parse(
        ALLUXIO_SERVICE / "configuration/alluxio-site-properties.xml"
      ).getroot().findall("property")
    }
    self.assertEqual([], validator.validate_site(site, {}, {}, {}, {}))

    site["alluxio.master.rpc.port"] = "70000"
    site["alluxio.worker.web.port"] = site["alluxio.master.web.port"]
    site["alluxio.worker.memory"] = "0GB;touch /tmp/alluxio"
    site["alluxio.underfs.hdfs.address"] = "/alluxio/../other"
    failures = validator.validate_site(site, {}, {}, {}, {})
    failure_names = {failure["config-name"] for failure in failures}
    self.assertTrue(
      {
        "alluxio.master.rpc.port",
        "alluxio.worker.web.port",
        "alluxio.worker.memory",
        "alluxio.underfs.hdfs.address",
      }.issubset(failure_names)
    )

  def test_advisor_rejects_unsafe_users_and_shared_runtime_directories(self):
    validator = self.advisor_module.AlluxioValidator()
    failures = validator.validate_environment(
      {
        "alluxio_user": "alluxio;id",
        "alluxio_group": "hadoop",
        "alluxio_log_dir": "/var/run/alluxio",
        "alluxio_pid_dir": "/etc",
      },
      {},
      {},
      {},
      {},
    )
    failure_names = {failure["config-name"] for failure in failures}
    self.assertIn("alluxio_user", failure_names)
    self.assertIn("alluxio_pid_dir", failure_names)


if __name__ == "__main__":
  unittest.main()
