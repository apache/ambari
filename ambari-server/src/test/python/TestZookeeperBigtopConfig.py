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
import json
from pathlib import Path
import sys
from types import ModuleType
import unittest
from unittest.mock import patch
from xml.etree import ElementTree
from jinja2 import Environment as JinjaEnvironment, StrictUndefined
from resource_management.core.environment import Environment
from resource_management.core.logger import Logger


ZOOKEEPER = (
  Path(__file__).resolve().parents[2]
  / "main/resources/stacks/BIGTOP/3.2.0/services/ZOOKEEPER"
)
ZOOKEEPER_33 = ZOOKEEPER.parents[2] / "3.3.0/services/ZOOKEEPER"
ZOOKEEPER_34 = ZOOKEEPER.parents[2] / "3.4.0/services/ZOOKEEPER"
SCRIPTS = ZOOKEEPER / "package/scripts"


def load_module(module_name, path):
  spec = importlib.util.spec_from_file_location(module_name, path)
  module = importlib.util.module_from_spec(spec)
  spec.loader.exec_module(module)
  return module


def params_module(**values):
  module = ModuleType("params")
  for name, value in values.items():
    setattr(module, name, value)
  return module


ZOOKEEPER_CONFIG = load_module(
  "bigtop_zookeeper_config", SCRIPTS / "zookeeper.py"
)


class TestZookeeperConfiguration(unittest.TestCase):
  def setUp(self):
    Logger.initialize_logger()
    self._environment = Environment(str(ZOOKEEPER / "package"), test_mode=True)
    self._environment.__enter__()

  def tearDown(self):
    self._environment.__exit__(None, None, None)

  def test_server_configuration_has_stable_ownership_and_modes(self):
    params = params_module(
      config_dir="/etc/zookeeper/conf",
      zk_user="zookeeper",
      user_group="hadoop",
      zk_env_sh_template="export JAVA_HOME={{java64_home_shell}}",
      zk_pid_dir="/var/run/zookeeper",
      zk_log_dir="/var/log/zookeeper",
      zk_data_dir="/hadoop/zookeeper",
      hostname="zk1",
      zookeeper_hosts=("zk1", "zk2", "zk3"),
      log4j_props="log4j.rootLogger=INFO, CONSOLE",
      security_enabled=True,
    )
    self._environment.set_params(params)
    with patch.dict(sys.modules, {"params": params}), \
      patch.object(ZOOKEEPER_CONFIG, "Directory") as directory, \
      patch.object(ZOOKEEPER_CONFIG, "File") as file_resource:
      ZOOKEEPER_CONFIG.zookeeper(type="server")

    myid_call = next(
      resource_call
      for resource_call in file_resource.call_args_list
      if resource_call.args[0] == "/hadoop/zookeeper/myid"
    )
    self.assertEqual("1\n", myid_call.kwargs["content"])
    self.assertEqual(0o640, myid_call.kwargs["mode"])
    self.assertEqual("zookeeper", myid_call.kwargs["owner"])
    data_call = next(
      resource_call
      for resource_call in directory.call_args_list
      if resource_call.args[0] == "/hadoop/zookeeper"
    )
    self.assertEqual(0o750, data_call.kwargs["mode"])
    jaas_modes = [
      resource_call.kwargs["mode"]
      for resource_call in file_resource.call_args_list
      if resource_call.args[0].endswith("jaas.conf")
    ]
    self.assertEqual([0o640, 0o640], jaas_modes)

  def test_client_does_not_create_server_state_directories(self):
    params = params_module(
      config_dir="/etc/zookeeper/conf",
      zk_user="zookeeper",
      user_group="hadoop",
      zk_env_sh_template="export JAVA_HOME={{java64_home_shell}}",
      zk_pid_dir="/var/run/zookeeper",
      zk_log_dir="/var/log/zookeeper",
      log4j_props=None,
      security_enabled=False,
    )
    self._environment.set_params(params)
    with patch.dict(sys.modules, {"params": params}), \
      patch.object(ZOOKEEPER_CONFIG, "Directory") as directory, \
      patch.object(ZOOKEEPER_CONFIG, "File"), \
      patch.object(ZOOKEEPER_CONFIG.sudo, "path_exists", return_value=False):
      ZOOKEEPER_CONFIG.zookeeper(type="client")

    created_directories = {
      resource_call.args[0] for resource_call in directory.call_args_list
    }
    self.assertEqual({"/etc/zookeeper/conf"}, created_directories)


class TestZookeeperStackMetadata(unittest.TestCase):
  def test_versions_preserve_base_and_overlay_history(self):
    versions = []
    for service_dir in (ZOOKEEPER, ZOOKEEPER_33, ZOOKEEPER_34):
      root = ElementTree.parse(service_dir / "metainfo.xml").getroot()
      versions.append(root.findtext("./services/service/version"))
    self.assertEqual(["3.5.9-2", "3.7.2-1", "3.8.4-1"], versions)

  def test_vendored_upstream_shells_are_not_packaged(self):
    package_files = ZOOKEEPER / "package/files"
    for file_name in ("zkEnv.sh", "zkServer.sh", "zkService.sh", "zkSmoke.sh"):
      with self.subTest(file_name=file_name):
        self.assertFalse((package_files / file_name).exists())

  def test_templates_use_deterministic_and_structured_values(self):
    zoo_template = (ZOOKEEPER / "package/templates/zoo.cfg.j2").read_text()
    input_template = (
      ZOOKEEPER / "package/templates/input.config-zookeeper.json.j2"
    ).read_text()
    params_source = (SCRIPTS / "params_linux.py").read_text()
    self.assertIn("zoo_cfg_properties_map|dictsort", zoo_template)
    self.assertIn('"path":{{zk_log_path_json}}', input_template)
    rendered = JinjaEnvironment(undefined=StrictUndefined).from_string(
      input_template
    ).render(zk_log_path_json=json.dumps('/var/log/zookeeper/zookeeper*.log'))
    self.assertEqual(
      "/var/log/zookeeper/zookeeper*.log",
      json.loads(rendered)["input"][0]["path"],
    )
    self.assertNotIn("default('/configurations", input_template)
    hook_params = (
      ZOOKEEPER.parents[4] / "stack-hooks/after-INSTALL/scripts/params.py"
    ).read_text(encoding="utf-8")
    self.assertIn("zk_log_path_json = json.dumps", hook_params)
    self.assertIn('"/configurations/zookeeper-env/zk_log_dir"', hook_params)
    self.assertIn(
      'zk_server_heapsize = f"-Xmx{zk_server_heap_mb}m"', params_source
    )

  def test_rpm_and_deb_package_names_are_declared(self):
    root = ElementTree.parse(ZOOKEEPER / "metainfo.xml").getroot()
    packages = {
      os_specific.findtext("osFamily"): os_specific.findtext(
        "./packages/package/name"
      )
      for os_specific in root.findall("./services/service/osSpecifics/osSpecific")
    }
    self.assertEqual(
      "zookeeper_${stack_version}",
      packages["redhat8,redhat9,openeuler22"],
    )
    self.assertEqual(
      "zookeeper-${stack_version}",
      packages["ubuntu22"],
    )


if __name__ == "__main__":
  unittest.main()
