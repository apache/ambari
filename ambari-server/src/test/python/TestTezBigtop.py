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
import os
from pathlib import Path
import sys
from types import ModuleType, SimpleNamespace
import unittest
from unittest.mock import MagicMock, call, patch
from xml.etree import ElementTree

from resource_management.core.exceptions import Fail
from resource_management.core.environment import Environment
from resource_management.core.logger import Logger


TEZ = (
  Path(__file__).resolve().parents[2]
  / "main/resources/stacks/BIGTOP/3.2.0/services/TEZ"
)
TEZ_33 = TEZ.parents[2] / "3.3.0/services/TEZ"
TEZ_34 = TEZ.parents[2] / "3.4.0/services/TEZ"
SCRIPTS = TEZ / "package/scripts"


def load_module(module_name, path, dependencies=None):
  spec = importlib.util.spec_from_file_location(module_name, path)
  module = importlib.util.module_from_spec(spec)
  with patch.dict(sys.modules, dependencies or {}):
    spec.loader.exec_module(module)
  return module


def params_module(**values):
  module = ModuleType("params")
  for name, value in values.items():
    setattr(module, name, value)
  return module


TEZ_UTILS = load_module("bigtop_tez_utils", SCRIPTS / "tez_utils.py")
TEZ_CONFIG = load_module(
  "bigtop_tez_config", SCRIPTS / "tez.py", {"tez_utils": TEZ_UTILS}
)
TEZ_CLIENT = load_module(
  "bigtop_tez_client", SCRIPTS / "tez_client.py", {"tez": TEZ_CONFIG}
)
TEZ_SERVICE_CHECK = load_module(
  "bigtop_tez_service_check",
  SCRIPTS / "service_check.py",
  {"tez_utils": TEZ_UTILS},
)


class TestTezUtils(unittest.TestCase):
  def test_boolean_stack_identity_and_paths_fail_closed(self):
    self.assertTrue(TEZ_UTILS.as_bool("true", "flag"))
    self.assertFalse(TEZ_UTILS.as_bool(False, "flag"))
    self.assertEqual("3.3.0", TEZ_UTILS.validate_bigtop_stack("BIGTOP", "3.3.0"))

    for value in (1, "yes", ""):
      with self.subTest(value=value), self.assertRaises(Fail):
        TEZ_UTILS.as_bool(value, "flag")
    with self.assertRaises(Fail):
      TEZ_UTILS.validate_bigtop_stack("OTHER", "3.3.0")
    for path in (
      "relative/path",
      "/tmp/../etc",
      "/tmp/with space",
      "/tmp/$(id)",
      "/tmp/path,INJECTED=value",
    ):
      with self.subTest(path=path), self.assertRaises(Fail):
        TEZ_UTILS.validate_absolute_path(path, "path")

  def test_examples_jar_must_be_one_regular_non_symlink_file(self):
    pattern = "/usr/lib/tez/tez-examples*.jar"
    with patch.object(TEZ_UTILS.glob, "glob", return_value=["/one.jar"]), \
      patch.object(TEZ_UTILS.sudo, "path_isfile", return_value=True), \
      patch.object(TEZ_UTILS.sudo, "path_islink", return_value=False):
      self.assertEqual("/one.jar", TEZ_UTILS.find_unique_examples_jar(pattern))

    with patch.object(
        TEZ_UTILS.glob, "glob", return_value=["/one.jar", "/two.jar"]
      ), \
      patch.object(TEZ_UTILS.sudo, "path_isfile", return_value=True), \
      patch.object(TEZ_UTILS.sudo, "path_islink", return_value=False), \
      self.assertRaisesRegex(Fail, "exactly one"):
      TEZ_UTILS.find_unique_examples_jar(pattern)

    with patch.object(TEZ_UTILS.glob, "glob", return_value=["/link.jar"]), \
      patch.object(TEZ_UTILS.sudo, "path_isfile", return_value=True), \
      patch.object(TEZ_UTILS.sudo, "path_islink", return_value=True), \
      self.assertRaisesRegex(Fail, "found 0"):
      TEZ_UTILS.find_unique_examples_jar(pattern)

  def test_keytab_validation_rejects_missing_and_symlink_credentials(self):
    keytab = "/etc/security/keytabs/smoke.keytab"
    with patch.object(TEZ_UTILS.sudo, "path_lexists", return_value=True), \
      patch.object(TEZ_UTILS.sudo, "path_isfile", return_value=True), \
      patch.object(TEZ_UTILS.sudo, "path_islink", return_value=False):
      self.assertEqual(
        keytab, TEZ_UTILS.validate_keytab(keytab, "Tez smoke keytab")
      )

    with patch.object(TEZ_UTILS.sudo, "path_lexists", return_value=True), \
      patch.object(TEZ_UTILS.sudo, "path_isfile", return_value=True), \
      patch.object(TEZ_UTILS.sudo, "path_islink", return_value=True), \
      self.assertRaisesRegex(Fail, "non-symlink"):
      TEZ_UTILS.validate_keytab(keytab, "Tez smoke keytab")

    with patch.object(TEZ_UTILS.sudo, "path_lexists", return_value=False), \
      self.assertRaisesRegex(Fail, "does not exist"):
      TEZ_UTILS.validate_keytab(keytab, "Tez smoke keytab")

    with self.assertRaisesRegex(Fail, "must end with .keytab"):
      TEZ_UTILS.validate_keytab(
        "/etc/security/keytabs/smoke.txt", "Tez smoke keytab"
      )


class TestTezConfiguration(unittest.TestCase):
  def setUp(self):
    Logger.initialize_logger()
    self._environment = Environment(str(TEZ / "package"), test_mode=True)
    self._environment.__enter__()

  def tearDown(self):
    self._environment.__exit__(None, None, None)
  def test_managed_configuration_has_stable_ownership_and_modes(self):
    params = params_module(
      tez_conf_dir="/etc/tez/conf",
      user_group="hadoop",
      tez_site_config={"tez.lib.uris": "/bigtop/apps/3.3.0/tez/tez.tar.gz"},
      config={},
      tez_env_sh_template="export JAVA_HOME={{java64_home}}",
    )
    self._environment.set_params(params)
    with patch.dict(sys.modules, {"params": params}), \
      patch.object(TEZ_CONFIG, "Directory") as directory, \
      patch.object(TEZ_CONFIG, "XmlConfig") as xml_config, \
      patch.object(TEZ_CONFIG, "File") as file_resource:
      TEZ_CONFIG.tez("/etc/tez/conf")

    directory.assert_called_once_with(
      "/etc/tez/conf",
      owner="root",
      group="hadoop",
      mode=0o755,
      create_parents=True,
    )
    self.assertEqual(0o644, xml_config.call_args.kwargs["mode"])
    self.assertEqual("root", xml_config.call_args.kwargs["owner"])
    self.assertEqual("hadoop", xml_config.call_args.kwargs["group"])
    self.assertEqual(0o644, file_resource.call_args.kwargs["mode"])
    self.assertEqual("root", file_resource.call_args.kwargs["owner"])
    self.assertEqual("hadoop", file_resource.call_args.kwargs["group"])

  def test_client_install_stages_runtime_dependencies_before_configuration(self):
    params = params_module(tez_conf_dir="/etc/tez/conf")
    client = object.__new__(TEZ_CLIENT.TezClientLinux)
    client.install_packages = MagicMock()
    client.configure = MagicMock()
    env = MagicMock()
    with patch.dict(sys.modules, {"params": params}), \
      patch.object(TEZ_CLIENT.lzo_utils, "install_lzo_if_needed") as install_lzo:
      client.install(env)

    client.install_packages.assert_called_once_with(env)
    install_lzo.assert_called_once_with()
    client.configure.assert_called_once_with(env, config_dir="/etc/tez/conf")


class TestTezServiceCheck(unittest.TestCase):
  @staticmethod
  def _params(security_enabled=False):
    return params_module(
      tez_examples_jar_pattern="/usr/lib/tez/tez-examples*.jar",
      tmp_dir="/var/lib/ambari-agent/tmp",
      smokeuser="ambari-qa",
      user_group="hadoop",
      hdfs_user="hdfs",
      stack_version_formatted="3.3.0",
      sysprep_skip_copy_tarballs_hdfs=False,
      tez_lib_base_dir_path="/bigtop/apps/3.3.0/tez",
      tez_lib_uris="/bigtop/apps/3.3.0/tez/tez.tar.gz",
      tez_home="/usr/bigtop/current/tez-client",
      security_enabled=security_enabled,
      smoke_user_keytab="/etc/security/keytabs/smoke.keytab",
      hdfs_user_keytab="/etc/security/keytabs/hdfs.keytab",
      smokeuser_principal="ambari-qa@EXAMPLE.COM",
      kinit_path_local="/usr/bin/kinit",
      hadoop_conf_dir="/etc/hadoop/conf",
      hadoop_bin_dir="/usr/bin",
      HdfsResource=MagicMock(),
    )

  def test_nonsecure_check_uses_unique_structured_commands_and_cleans_up(self):
    params = self._params()
    service_check = object.__new__(TEZ_SERVICE_CHECK.TezServiceCheckLinux)
    uuid_value = SimpleNamespace(hex="unique-check")
    with patch.dict(sys.modules, {"params": params}), \
      patch.object(TEZ_SERVICE_CHECK.uuid, "uuid4", return_value=uuid_value), \
      patch.object(
        TEZ_SERVICE_CHECK.tez_utils,
        "find_unique_examples_jar",
        return_value="/usr/lib/tez/tez-examples-0.10.2.jar",
      ), \
      patch.object(TEZ_SERVICE_CHECK, "check_stack_feature", return_value=False), \
      patch.object(TEZ_SERVICE_CHECK, "File") as file_resource, \
      patch.object(TEZ_SERVICE_CHECK, "Execute") as execute:
      service_check.service_check(MagicMock())

    run_command = execute.call_args_list[0]
    self.assertEqual(
      ("/usr/bin/hadoop", "--config", "/etc/hadoop/conf"),
      run_command.args[0][:3],
    )
    self.assertEqual("jar", run_command.args[0][3])
    self.assertEqual("orderedwordcount", run_command.args[0][5])
    self.assertIn("unique-check", run_command.args[0][6])
    self.assertEqual({}, run_command.kwargs["environment"])
    self.assertEqual(330, run_command.kwargs["timeout"])
    success_command = execute.call_args_list[1]
    self.assertEqual(("fs", "-test", "-e"), success_command.args[0][3:6])
    self.assertIn("unique-check", success_command.args[0][6])
    self.assertEqual(60, success_command.kwargs["timeout"])
    file_resource.assert_has_calls(
      [
        call(
          "/var/lib/ambari-agent/tmp/tez-service-check-unique-check.txt",
          content="foo\nbar\nfoo\nbar\nfoo",
          mode=0o644,
        ),
        call(
          "/var/lib/ambari-agent/tmp/tez-service-check-unique-check.txt",
          action="delete",
        ),
      ]
    )
    self.assertEqual(
      "delete_on_execute",
      params.HdfsResource.call_args_list[-2].kwargs["action"],
    )
    self.assertEqual(
      "execute", params.HdfsResource.call_args_list[-1].kwargs["action"]
    )
    hdfs_calls = {
      item.args[0]: item for item in params.HdfsResource.call_args_list
    }
    runtime_directory = hdfs_calls["/bigtop/apps/3.3.0/tez"]
    self.assertEqual("hdfs", runtime_directory.kwargs["owner"])
    self.assertEqual(0o555, runtime_directory.kwargs["mode"])
    runtime_archive = hdfs_calls["/bigtop/apps/3.3.0/tez/tez.tar.gz"]
    self.assertEqual("hdfs", runtime_archive.kwargs["owner"])
    self.assertEqual("hadoop", runtime_archive.kwargs["group"])
    self.assertEqual(0o444, runtime_archive.kwargs["mode"])

  def test_secure_check_uses_private_cache_for_kinit_and_hadoop(self):
    params = self._params(security_enabled=True)
    service_check = object.__new__(TEZ_SERVICE_CHECK.TezServiceCheckLinux)
    kerberos_cache = MagicMock()
    kerberos_cache.environment = {"KRB5CCNAME": "FILE:/private/cache"}
    cache_context = MagicMock()
    cache_context.__enter__.return_value = kerberos_cache
    with patch.dict(sys.modules, {"params": params}), \
      patch.object(
        TEZ_SERVICE_CHECK.tez_utils,
        "find_unique_examples_jar",
        return_value="/usr/lib/tez/tez-examples-0.10.2.jar",
      ), \
      patch.object(TEZ_SERVICE_CHECK.tez_utils, "validate_keytab") as validate, \
      patch.object(TEZ_SERVICE_CHECK, "check_stack_feature", return_value=False), \
      patch.object(
        TEZ_SERVICE_CHECK, "PrivateKerberosCache", return_value=cache_context
      ) as cache_factory, \
      patch.object(TEZ_SERVICE_CHECK, "File"), \
      patch.object(TEZ_SERVICE_CHECK, "Execute") as execute:
      service_check.service_check(MagicMock())

    self.assertEqual(
      [
        call("/etc/security/keytabs/hdfs.keytab", "HDFS service keytab"),
        call("/etc/security/keytabs/smoke.keytab", "Tez smoke keytab"),
      ],
      validate.call_args_list,
    )
    cache_factory.assert_called_once_with(
      "ambari-qa", "hadoop", prefix="ambari-tez-service-check-"
    )
    kerberos_cache.kinit.assert_called_once_with(
      "/usr/bin/kinit",
      "/etc/security/keytabs/smoke.keytab",
      "ambari-qa@EXAMPLE.COM",
      timeout=60,
    )
    for command_call in execute.call_args_list:
      self.assertEqual(
        {"KRB5CCNAME": "FILE:/private/cache"},
        command_call.kwargs["environment"],
      )
    cache_context.__exit__.assert_called_once()

  def test_tarball_failure_does_not_run_tez_and_still_cleans_both_inputs(self):
    params = self._params()
    service_check = object.__new__(TEZ_SERVICE_CHECK.TezServiceCheckLinux)
    with patch.dict(sys.modules, {"params": params}), \
      patch.object(
        TEZ_SERVICE_CHECK.tez_utils,
        "find_unique_examples_jar",
        return_value="/usr/lib/tez/tez-examples-0.10.2.jar",
      ), \
      patch.object(TEZ_SERVICE_CHECK, "check_stack_feature", return_value=True), \
      patch.object(TEZ_SERVICE_CHECK, "copy_to_hdfs", return_value=False), \
      patch.object(TEZ_SERVICE_CHECK, "File") as file_resource, \
      patch.object(TEZ_SERVICE_CHECK, "Execute") as execute, \
      self.assertRaisesRegex(Fail, "runtime archive"):
      service_check.service_check(MagicMock())

    execute.assert_not_called()
    self.assertEqual("delete", file_resource.call_args_list[-1].kwargs["action"])
    self.assertEqual(
      "delete_on_execute",
      params.HdfsResource.call_args_list[-2].kwargs["action"],
    )
    self.assertEqual(
      "execute", params.HdfsResource.call_args_list[-1].kwargs["action"]
    )

  def test_cleanup_failure_is_logged_without_replacing_operation_failure(self):
    params = self._params()

    def hdfs_resource(target, **options):
      if target is None and options.get("action") == "execute":
        raise RuntimeError("cleanup unavailable")

    params.HdfsResource.side_effect = hdfs_resource
    service_check = object.__new__(TEZ_SERVICE_CHECK.TezServiceCheckLinux)
    with patch.dict(sys.modules, {"params": params}), \
      patch.object(
        TEZ_SERVICE_CHECK.tez_utils,
        "find_unique_examples_jar",
        return_value="/usr/lib/tez/tez-examples-0.10.2.jar",
      ), \
      patch.object(TEZ_SERVICE_CHECK, "check_stack_feature", return_value=True), \
      patch.object(TEZ_SERVICE_CHECK, "copy_to_hdfs", return_value=False), \
      patch.object(TEZ_SERVICE_CHECK, "File") as file_resource, \
      patch.object(TEZ_SERVICE_CHECK.Logger, "warning") as warning, \
      self.assertRaisesRegex(Fail, "runtime archive"):
      service_check.service_check(MagicMock())

    self.assertEqual("delete", file_resource.call_args_list[-1].kwargs["action"])
    warning.assert_called_once()
    self.assertIn("cleanup unavailable", warning.call_args.args[0])


class TestTezAdvisorAndMetadata(unittest.TestCase):
  @classmethod
  def setUpClass(cls):
    cls.advisor = load_module(
      "bigtop_tez_service_advisor", TEZ / "service_advisor.py"
    )

  def test_recommendations_respect_yarn_and_hive_container_limits(self):
    recommender = object.__new__(self.advisor.TezRecommender)
    put_property = MagicMock()
    put_attribute = MagicMock()
    recommender.putProperty = MagicMock(return_value=put_property)
    recommender.putPropertyAttribute = MagicMock(return_value=put_attribute)
    recommender.recommendYarnQueue = MagicMock(return_value="default")
    recommender.calculateYarnAllocationSizes = MagicMock()
    configurations = {
      "yarn-site": {
        "properties": {
          "yarn.scheduler.minimum-allocation-mb": "1024",
          "yarn.scheduler.maximum-allocation-mb": "4096",
        }
      },
      "tez-site": {"properties": {"tez.runtime.sorter.class": "LEGACY"}},
    }
    services = {
      "configurations": {
        "hive-site": {"properties": {"hive.tez.container.size": "8192"}}
      }
    }

    recommender.recommendBigtopConfigurations(
      configurations,
      {
        "amMemory": 1024,
        "mapMemory": 8192,
        "reduceMemory": 2048,
        "containers": 2,
        "ramPerContainer": 2048,
      },
      services,
      {},
    )

    put_property.assert_any_call("tez.am.resource.memory.mb", 2048)
    put_property.assert_any_call("tez.task.resource.memory.mb", 4096)
    put_property.assert_any_call("tez.runtime.io.sort.mb", 1081)
    put_property.assert_any_call("tez.queue.name", "default")
    put_attribute.assert_called_once_with(
      "tez.runtime.io.sort.mb", "maximum", 1800
    )
    recommender.calculateYarnAllocationSizes.assert_not_called()

  def test_task_recommendation_does_not_exceed_one_container(self):
    recommender = object.__new__(self.advisor.TezRecommender)
    put_property = MagicMock()
    recommender.putProperty = MagicMock(return_value=put_property)
    recommender.putPropertyAttribute = MagicMock()
    recommender.recommendYarnQueue = MagicMock(return_value=None)
    recommender.calculateYarnAllocationSizes = MagicMock()
    configurations = {
      "yarn-site": {
        "properties": {
          "yarn.scheduler.minimum-allocation-mb": "1024",
          "yarn.scheduler.maximum-allocation-mb": "8192",
        }
      }
    }

    recommender.recommendBigtopConfigurations(
      configurations,
      {
        "amMemory": 2048,
        "mapMemory": 8192,
        "reduceMemory": 4096,
        "containers": 8,
        "ramPerContainer": 2048,
      },
      {},
      {},
    )

    put_property.assert_any_call("tez.task.resource.memory.mb", 2048)
    put_property.assert_any_call("tez.runtime.io.sort.mb", 540)

  def test_resource_parser_and_validator_reject_lossy_or_unsafe_values(self):
    for value in (1, " 2 ", "03"):
      with self.subTest(value=value):
        self.assertEqual(int(value), self.advisor._positive_int(value))
    for value in (True, 1.5, "1.5", "+1", "-1", "one", 0, None):
      with self.subTest(value=value):
        self.assertIsNone(self.advisor._positive_int(value))

    validator = object.__new__(self.advisor.TezValidator)
    validator.validatorLessThenDefaultValue = MagicMock(return_value=None)
    validator.validatorYarnQueue = MagicMock(return_value=None)
    validator.getErrorItem = lambda message: {
      "level": "ERROR",
      "message": message,
    }
    validator.getWarnItem = lambda message: {
      "level": "WARN",
      "message": message,
    }
    validator.toConfigurationValidationProblems = (
      lambda items, config_type: [item for item in items if item["item"]]
    )
    problems = validator.validateBigtopConfigurations(
      {
        "tez.am.resource.memory.mb": "1.5",
        "tez.task.resource.memory.mb": "1024",
        "tez.runtime.io.sort.mb": "1024",
        "tez.runtime.unordered.output.buffer.size-mb": "2048",
      },
      {},
      {},
      {
        "configurations": {
          "yarn-site": {
            "properties": {
              "yarn.scheduler.minimum-allocation-mb": "1024",
              "yarn.scheduler.maximum-allocation-mb": "4096",
            }
          }
        }
      },
      {},
    )
    error_names = [
      problem["config-name"]
      for problem in problems
      if problem["item"]["level"] == "ERROR"
    ]
    self.assertEqual(
      [
        "tez.am.resource.memory.mb",
        "tez.runtime.io.sort.mb",
        "tez.runtime.unordered.output.buffer.size-mb",
      ],
      error_names,
    )
    validated_names = [
      item.args[2]
      for item in validator.validatorLessThenDefaultValue.call_args_list
    ]
    self.assertNotIn("tez.am.resource.memory.mb", validated_names)

    problems = validator.validateBigtopConfigurations(
      {
        "tez.am.resource.memory.mb": "512",
        "tez.task.resource.memory.mb": "8192",
        "tez.runtime.io.sort.mb": "256",
        "tez.runtime.unordered.output.buffer.size-mb": "100",
      },
      {},
      {},
      {
        "configurations": {
          "yarn-site": {
            "properties": {
              "yarn.scheduler.minimum-allocation-mb": "1024",
              "yarn.scheduler.maximum-allocation-mb": "4096",
            }
          }
        }
      },
      {},
    )
    warning_names = [
      problem["config-name"]
      for problem in problems
      if problem["item"]["level"] == "WARN"
    ]
    self.assertEqual(
      ["tez.am.resource.memory.mb", "tez.task.resource.memory.mb"],
      warning_names,
    )

  def test_parent_load_failure_is_not_swallowed(self):
    advisor_path = TEZ / "service_advisor.py"
    spec = importlib.util.spec_from_file_location(
      "bigtop_tez_broken_parent_advisor", advisor_path
    )
    module = importlib.util.module_from_spec(spec)
    with patch.dict(os.environ, {"BASE_SERVICE_ADVISOR": "/missing/advisor.py"}), \
      patch("builtins.open", side_effect=OSError("parent unavailable")), \
      self.assertRaisesRegex(OSError, "parent unavailable"):
      spec.loader.exec_module(module)

  def test_metadata_preserves_history_and_removes_unrelated_theme(self):
    versions = [
      ElementTree.parse(path / "metainfo.xml").findtext(
        "./services/service/version"
      )
      for path in (TEZ, TEZ_33, TEZ_34)
    ]
    self.assertEqual(["0.10.1-1", "0.10.2-1", "0.10.4-1"], versions)
    self.assertIsNone(
      ElementTree.parse(TEZ / "metainfo.xml").find("./services/service/themes")
    )
    self.assertFalse((TEZ / "themes/directories.json").exists())
    self.assertFalse((SCRIPTS / "pre_upgrade.py").exists())

    metadata = ElementTree.parse(TEZ / "metainfo.xml").getroot()
    packages_by_os = {
      os_specific.findtext("osFamily"): [
        package.text for package in os_specific.findall("packages/package/name")
      ]
      for os_specific in metadata.findall("./services/service/osSpecifics/osSpecific")
    }
    self.assertEqual(
      ["tez_${stack_version}"],
      packages_by_os["redhat8,redhat9,openeuler22"],
    )
    self.assertEqual(["tez-${stack_version}"], packages_by_os["ubuntu22"])

  def test_java_17_options_and_config_ownership_contract_are_declared(self):
    root = ElementTree.parse(TEZ / "configuration/tez-site.xml").getroot()
    values = {
      property_node.findtext("name"): property_node.findtext("value")
      for property_node in root.findall("property")
    }
    for property_name in (
      "tez.am.launch.cmd-opts",
      "tez.task.launch.cmd-opts",
    ):
      self.assertIn("-Xlog:gc*", values[property_name])
      self.assertIn("-XX:+UseParallelGC", values[property_name])
      self.assertNotIn("PrintGCTimeStamps", values[property_name])
    self.assertNotIn("yarn.timeline-service.enabled", values)
    self.assertEqual("/tmp/${user.name}/tez/staging", values["tez.staging-dir"])
    self.assertEqual("{{tez_lib_uris}}", values["tez.lib.uris"])
    self.assertEqual("false", values["tez.use.cluster.hadoop-libs"])
    self.assertNotIn("tez.lib.uris.classpath", values)
    self.assertNotIn("tez.cluster.additional.classpath.prefix", values)
    self.assertNotIn("tez.am.tez-ui.history-url.template", values)
    self.assertNotIn("tez.tez-ui.history-url.base", values)

    env_root = ElementTree.parse(TEZ / "configuration/tez-env.xml").getroot()
    heap_dump = next(
      property_node
      for property_node in env_root.findall("property")
      if property_node.findtext("name") == "enable_heap_dump"
    )
    self.assertEqual("boolean", heap_dump.findtext("value-attributes/type"))
    template = next(
      property_node.findtext("value")
      for property_node in env_root.findall("property")
      if property_node.findtext("name") == "content"
    )
    for shell_value in (
      "{{tez_conf_dir_shell}}",
      "{{hadoop_home_shell}}",
      "{{java64_home_shell}}",
    ):
      self.assertIn(shell_value, template)

  def test_service_sources_have_no_shell_commands_or_obsolete_entry_points(self):
    source = "\n".join(
      path.read_text(encoding="utf-8") for path in SCRIPTS.glob("*.py")
    )
    for residue in (
      "shell.call",
      "shell.checked_call",
      "kinit_cmd",
      "tezsmoke",
      "subprocess",
      "os.system",
      "Popen",
    ):
      self.assertNotIn(residue, source)


if __name__ == "__main__":
  unittest.main()
