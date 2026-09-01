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
import os
import shlex
import sys
import unittest
import xml.etree.ElementTree as ET
from pathlib import Path
from unittest.mock import patch

from jinja2 import Environment as JinjaEnvironment, StrictUndefined
from resource_management.core.shell import quote_bash_args


ALLUXIO_SCRIPTS = (
  Path(__file__).resolve().parents[2]
  / "main/resources/stacks/BIGTOP/3.2.0/services/ALLUXIO/package/scripts"
)
ALLUXIO_SERVICE = ALLUXIO_SCRIPTS.parents[1]
ALLUXIO_33_SERVICE = ALLUXIO_SERVICE.parents[2] / "3.3.0/services/ALLUXIO"


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
    )
    assignments = {
      line.split("=", 1)[0]: line.split("=", 1)[1]
      for line in rendered.splitlines()
      if line.startswith(("JAVA_HOME=", "ALLUXIO_NATIVE_LIBRARY_OPTION="))
    }

    self.assertNotIn("/usr/hdp/", template_text)
    self.assertNotIn("{{java_home}}", template_text)
    self.assertNotIn("{{hadoop_home}}", template_text)
    self.assertEqual([java_home], shlex.split(assignments["JAVA_HOME"]))
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
      "alluxio_authentication",
      "alluxio_data_dir",
      "alluxio_kerberos_keytab",
      "alluxio_kerberos_principal",
      "alluxio_journal_dir",
      "alluxio_master_rpc_port",
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
    self.assertEqual({"resolve_master_metastore_dir"}, functions)

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
      "alluxio_authentication": "KERBEROS" if security_enabled else "SIMPLE",
      "alluxio_master_host": "master.example.com",
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
        alluxio_kerberos_keytab="/etc/security/keytabs/alluxio.keytab",
        alluxio_kerberos_principal="alluxio/host@EXAMPLE.COM",
      )
    return variables

  def test_insecure_template_renders_without_kerberos_or_level1(self):
    rendered = JinjaEnvironment(undefined=StrictUndefined).from_string(
      self._template_text()
    ).render(**self._variables(False))

    self.assertNotIn("kerberos.client.keytab.file", rendered)
    self.assertNotIn("kerberos.client.principal", rendered)
    self.assertNotIn("tieredstore.level1.alias=HDD", rendered)
    self.assertNotIn("hdd_dirs", rendered)
    self.assertIn(
      "alluxio.master.journal.folder=/var/lib/alluxio/journal", rendered
    )
    self.assertIn(
      "alluxio.underfs.hdfs.configuration=/etc/hadoop/conf/core-site.xml:"
      "/etc/hadoop/conf/hdfs-site.xml",
      rendered,
    )

  def test_secure_template_renders_guarded_kerberos_properties(self):
    rendered = JinjaEnvironment(undefined=StrictUndefined).from_string(
      self._template_text()
    ).render(**self._variables(True))

    self.assertIn(
      "kerberos.client.keytab.file=/etc/security/keytabs/alluxio.keytab",
      rendered,
    )
    self.assertIn(
      "kerberos.client.principal=alluxio/host@EXAMPLE.COM", rendered
    )
    self.assertIn(
      "alluxio.master.journal.folder=/var/lib/alluxio/journal", rendered
    )
    self.assertNotIn("tieredstore.level1.alias=HDD", rendered)


class TestAlluxioVersionContract(unittest.TestCase):
  def test_stack_33_overlay_matches_bigtop_alluxio_296_release_1(self):
    base_path = ALLUXIO_SERVICE / "metainfo.xml"
    overlay_path = ALLUXIO_33_SERVICE / "metainfo.xml"
    base_versions = [
      element.text for element in ET.parse(base_path).findall(".//version")
    ]
    overlay_source = overlay_path.read_text(encoding="utf-8")
    overlay_root = ET.parse(overlay_path).getroot()

    self.assertEqual(["2.9.3"], base_versions)
    self.assertEqual("ALLUXIO", overlay_root.findtext(".//service/name"))
    self.assertEqual("2.9.6-1", overlay_root.findtext(".//service/version"))
    self.assertNotIn("2.9.3", overlay_source)


class TestAlluxioServiceAdvisor(unittest.TestCase):
  def test_parent_load_failure_propagates_original_error(self):
    advisor_path = ALLUXIO_SERVICE / "service_advisor.py"
    parent_path = ALLUXIO_SERVICE.parents[3] / "service_advisor.py"
    spec = importlib.util.spec_from_file_location(
      "bigtop_alluxio_service_advisor_failure", advisor_path
    )
    advisor_module = importlib.util.module_from_spec(spec)

    with (
      patch.dict(os.environ, {"BASE_SERVICE_ADVISOR": str(parent_path)}),
      patch(
        "ambari_commons.import_utils.load_module",
        side_effect=RuntimeError("parent load failed"),
      ),
    ):
      with self.assertRaisesRegex(RuntimeError, "parent load failed"):
        spec.loader.exec_module(advisor_module)

  def test_legacy_recommender_is_removed(self):
    advisor_source = (ALLUXIO_SERVICE / "service_advisor.py").read_text(
      encoding="utf-8"
    )

    self.assertNotIn("HDP", advisor_source)
    self.assertNotIn("AlluxioRecommender", advisor_source)


if __name__ == "__main__":
  unittest.main()
