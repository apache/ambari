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
import importlib.util
from pathlib import Path
import shlex
import unittest
import xml.etree.ElementTree as ET
from unittest.mock import patch

from jinja2 import Environment as JinjaEnvironment, StrictUndefined
from resource_management.core.exceptions import Fail
from resource_management.core.shell import quote_bash_args
from resource_management.libraries.functions import package_conditions


STACKS = Path(__file__).resolve().parents[2] / "main/resources/stacks/BIGTOP"
YARN = STACKS / "3.2.0/services/YARN"
YARN_33 = STACKS / "3.3.0/services/YARN"
SCRIPTS = YARN / "package/scripts"
TEMPLATES = YARN / "package/templates"
PROPERTIES = YARN / "properties"


def load_module(module_name, path):
  spec = importlib.util.spec_from_file_location(module_name, path)
  module = importlib.util.module_from_spec(spec)
  spec.loader.exec_module(module)
  return module


YARN_FUNCTIONS = load_module("bigtop_yarn_functions", SCRIPTS / "functions.py")


def property_value(path, property_name):
  root = ET.parse(path).getroot()
  return next(
    prop.findtext("value")
    for prop in root.findall("property")
    if prop.findtext("name") == property_name
  )


def service_version(path, service_name):
  root = ET.parse(path).getroot()
  return next(
    service.findtext("version")
    for service in root.findall("./services/service")
    if service.findtext("name") == service_name
  )


class TestYarnRootShellContract(unittest.TestCase):
  def test_bigtop_stack_and_local_roots_fail_closed(self):
    self.assertEqual(
      "3.3.6-1", YARN_FUNCTIONS.validate_bigtop_stack("BIGTOP", "3.3.6-1")
    )
    self.assertEqual(
      "/usr/bigtop",
      YARN_FUNCTIONS.validate_absolute_path("/usr/bigtop", "BIGTOP stack root"),
    )
    for stack_name, version in (
      ("HDP", "3.3.6-1"),
      ("BIGTOP", ""),
      ("BIGTOP", "../3.3.6"),
    ):
      with self.subTest(stack_name=stack_name, version=version):
        with self.assertRaises(Fail):
          YARN_FUNCTIONS.validate_bigtop_stack(stack_name, version)
    for path in ("/", "relative/path", "/usr/bigtop/../hdp", "/usr/bigtop path"):
      with self.subTest(path=path):
        with self.assertRaises(Fail):
          YARN_FUNCTIONS.validate_absolute_path(path, "BIGTOP stack root")

    for path in ("/run/hadoop-yarn", "/var/run/hadoop-mapreduce"):
      self.assertEqual(
        path,
        YARN_FUNCTIONS.validate_runtime_directory_prefix(path, "PID prefix"),
      )
    for path in ("/tmp/yarn", "/var/lib/yarn", "/run/../tmp", "relative"):
      with self.subTest(runtime_prefix=path):
        with self.assertRaises(Fail):
          YARN_FUNCTIONS.validate_runtime_directory_prefix(path, "PID prefix")

  def test_resource_manager_ha_ids_follow_hadoop_trimmed_collection_contract(self):
    self.assertEqual(
      ("rm0", "rm1"), YARN_FUNCTIONS.parse_rm_ha_ids(" rm0, rm1 ")
    )
    self.assertEqual(
      "rm1",
      YARN_FUNCTIONS.resolve_local_rm_ha_id(
        {"rm0": "RM0.EXAMPLE.COM", "rm1": "RM1.EXAMPLE.COM."},
        " rm1.example.com ",
      ),
    )
    for value in ("rm0,,rm1", "rm0,rm0", "rm0,../rm1", 1):
      with self.subTest(value=value):
        with self.assertRaises(Fail):
          YARN_FUNCTIONS.parse_rm_ha_ids(value)
    for hosts, local in (
      ({"rm0": "rm0.example.com"}, "rm1.example.com"),
      ({"rm0": "rm.example.com", "rm1": "RM.EXAMPLE.COM."}, "rm.example.com"),
    ):
      with self.subTest(hosts=hosts, local=local):
        with self.assertRaises(Fail):
          YARN_FUNCTIONS.resolve_local_rm_ha_id(hosts, local)
    self.assertIsNone(
      YARN_FUNCTIONS.resolve_local_rm_ha_id(
        {"rm0": "rm0.example.com"},
        "nm.example.com",
        require_match=False,
      )
    )
    self.assertEqual(
      (), YARN_FUNCTIONS.validate_rm_ha_ids(False, "stale,,invalid")
    )
    self.assertEqual(
      ("rm0", "rm1"),
      YARN_FUNCTIONS.validate_rm_ha_ids(True, "rm0,rm1"),
    )
    for invalid in (None, "", "rm0"):
      with self.subTest(ha_ids=invalid):
        with self.assertRaisesRegex(Fail, "at least two"):
          YARN_FUNCTIONS.validate_rm_ha_ids(True, invalid)
    self.assertEqual(
      "rm0",
      YARN_FUNCTIONS.resolve_local_rm_ha_id(
        {"rm0": "rm0", "rm1": "rm1.example.com"},
        "rm0.example.com",
      ),
    )

  def test_ranger_uses_policy_selected_and_ha_aware_rm_address(self):
    addresses = {
      "rm0": "rm0.example:8088",
      "rm1": "rm1.example:8088",
    }
    self.assertEqual(
      "http://rm1.example:8088",
      "http://"
      + YARN_FUNCTIONS.select_rm_webapp_address(addresses, "rm1"),
    )
    self.assertEqual(
      "https://rm0.example:8090",
      "https://"
      + YARN_FUNCTIONS.select_rm_webapp_address(
        {"rm0": "rm0.example:8090", "rm1": "rm1.example:8090"},
        None,
      ),
    )
    for invalid in ({}, {"rm0": "rm0.example"}, {"rm0": "rm0.example:0"}):
      with self.subTest(invalid=invalid):
        with self.assertRaises(Fail):
          YARN_FUNCTIONS.select_rm_webapp_address(invalid)

    params_source = (SCRIPTS / "params_linux.py").read_text(encoding="utf-8")
    self.assertIn("yarn_rest_url = yarn_rm_address", params_source)
    self.assertNotIn(
      'yarn_rest_url = config["configurations"]["yarn-site"]',
      params_source,
    )

  def test_fast_launch_framework_path_is_stable_across_ha_roles(self):
    expected_framework_root = "/bigtop/apps/3.3.6-1/yarn"
    for role, rm_id in (
      ("rm0", "rm0"),
      ("rm1", "rm1"),
      ("nodemanager", None),
      ("client", None),
    ):
      with self.subTest(role=role):
        framework_root, hbase_root = YARN_FUNCTIONS.yarn_artifact_paths(
          "3.3.6-1", rm_id
        )
        self.assertEqual(expected_framework_root, framework_root)
        expected_hbase_suffix = f"/{rm_id}" if rm_id else "/hbase"
        self.assertTrue(hbase_root.endswith(expected_hbase_suffix))

  def test_component_version_is_required_only_for_timeline_v2_consumers(self):
    self.assertEqual(
      "3.3.6-1",
      YARN_FUNCTIONS.require_bigtop_component_version(
        "3.3.6-1", "YARN Timeline Service v2"
      ),
    )
    with self.assertRaisesRegex(Fail, "requires a resolved BIGTOP"):
      YARN_FUNCTIONS.require_bigtop_component_version(
        None, "YARN Timeline Service v2"
      )

    params_source = (SCRIPTS / "params_linux.py").read_text(encoding="utf-8")
    self.assertIn("version = get_current_component_version()", params_source)
    self.assertNotIn('version = default("/commandParams/version"', params_source)
    versioned_block = params_source[
      params_source.index("yarn_atsv2_hbase_versioned_home = None") :
    ]
    versioned_block = versioned_block[
      : versioned_block.index("yarn_hbase_log_dir =")
    ]
    self.assertIn("if atsv2_backend_enabled:", versioned_block)
    self.assertIn("require_bigtop_component_version", versioned_block)
    self.assertIn(
      "atsv2_backend_enabled = has_timeline_service_v2 and has_atsv2",
      params_source,
    )
    derived_paths = params_source[
      params_source.index("yarn_hbase_archive_id_json = None") :
    ]
    derived_paths = derived_paths[: derived_paths.index("java64_home_json =")]
    self.assertIn("if atsv2_backend_enabled:", derived_paths)
    self.assertNotIn(
      'f"{yarn_hbase_app_hdfs_path}',
      derived_paths.split("if atsv2_backend_enabled:", 1)[0],
    )

  def test_ranger_root_paths_use_safe_single_segment_names(self):
    for value in ("cluster_yarn", "custom.repo-1"):
      self.assertEqual(
        value, YARN_FUNCTIONS.validate_config_segment(value, "Ranger service")
      )
    for value in ("../outside", "/absolute", "name with space", "repo;touch", ""):
      with self.subTest(value=value):
        with self.assertRaises(Fail):
          YARN_FUNCTIONS.validate_config_segment(value, "Ranger service")

    self.assertEqual(
      "mariadb-java-client.jar",
      YARN_FUNCTIONS.validate_jar_file_name(
        "mariadb-java-client.jar", "Ranger JDBC JAR"
      ),
    )
    for value in ("../driver.jar", "/tmp/driver.jar", "driver;run.jar", "driver"):
      with self.subTest(value=value):
        with self.assertRaises(Fail):
          YARN_FUNCTIONS.validate_jar_file_name(value, "Ranger JDBC JAR")

    params_source = (SCRIPTS / "params_linux.py").read_text(encoding="utf-8")
    self.assertGreaterEqual(params_source.count("validate_config_segment("), 2)
    self.assertGreaterEqual(params_source.count("validate_jar_file_name("), 2)

  def test_boolean_config_values_are_parsed_without_string_truthiness(self):
    values = {
      True: True,
      False: False,
      "true": True,
      "false": False,
      "TRUE": True,
      "FALSE": False,
      " True ": True,
      " False ": False,
    }
    for value, expected in values.items():
      with self.subTest(value=value):
        self.assertIs(expected, YARN_FUNCTIONS.parse_boolean(value))
    for invalid in (None, "", "yes", 1):
      with self.subTest(invalid=invalid):
        with self.assertRaisesRegex(ValueError, "Expected a boolean"):
          YARN_FUNCTIONS.parse_boolean(invalid)

    params_source = (SCRIPTS / "params_linux.py").read_text(encoding="utf-8")
    self.assertIn(
      'is_webhdfs_enabled = parse_boolean(hdfs_site["dfs.webhdfs.enabled"])',
      params_source,
    )
    self.assertIn("rm_cross_origin_enabled = parse_boolean(", params_source)

  def test_registry_dns_port_requires_the_network_port_range(self):
    self.assertEqual(53, YARN_FUNCTIONS.parse_port("53", "Registry DNS port"))
    self.assertEqual(1053, YARN_FUNCTIONS.parse_port(1053, "Registry DNS port"))
    for invalid in (0, -1, 65536, "not-a-port", "+53", "1_053", 53.0, True):
      with self.subTest(value=invalid):
        with self.assertRaisesRegex(Fail, "1 through 65535"):
          YARN_FUNCTIONS.parse_port(invalid, "Registry DNS port")

  def test_yes_no_and_positive_numeric_contracts_fail_closed(self):
    self.assertIs(True, YARN_FUNCTIONS.parse_yes_no(" Yes ", "Ranger switch"))
    self.assertIs(False, YARN_FUNCTIONS.parse_yes_no("NO", "Ranger switch"))
    for invalid in (True, False, "true", "", None, 1):
      with self.subTest(yes_no=invalid):
        with self.assertRaises(Fail):
          YARN_FUNCTIONS.parse_yes_no(invalid, "Ranger switch")

    self.assertEqual(1, YARN_FUNCTIONS.parse_positive_int("1", "container count"))
    self.assertEqual(4096, YARN_FUNCTIONS.parse_positive_int(4096, "memory"))
    for invalid in (0, -1, "1.5", 1.0, True, "nan"):
      with self.subTest(positive_int=invalid):
        with self.assertRaises(Fail):
          YARN_FUNCTIONS.parse_positive_int(invalid, "container count")

    self.assertEqual(0.8, YARN_FUNCTIONS.parse_fraction("0.8", "heap factor"))
    for invalid in (0, -0.1, 1.01, "nan", "inf", True, None):
      with self.subTest(fraction=invalid):
        with self.assertRaises(Fail):
          YARN_FUNCTIONS.parse_fraction(invalid, "heap factor")

  def test_container_executor_values_are_strict_and_render_exactly(self):
    self.assertEqual(0, YARN_FUNCTIONS.parse_nonnegative_int("0", "minimum UID"))
    self.assertEqual(1000, YARN_FUNCTIONS.parse_nonnegative_int(1000, "minimum UID"))
    for invalid in (-1, "-1", "1.5", 1.5, True, "1_000", "$(id)"):
      with self.subTest(nonnegative_int=invalid):
        with self.assertRaises(Fail):
          YARN_FUNCTIONS.parse_nonnegative_int(invalid, "minimum UID")

    for invalid in ("line\nnext", "nul\0byte", "tab\tvalue", 1, None):
      with self.subTest(single_line=invalid):
        with self.assertRaises(Fail):
          YARN_FUNCTIONS.validate_single_line_value(invalid, "Docker value")

    self.assertEqual(
      "hadoop-users",
      YARN_FUNCTIONS.validate_unix_name("hadoop-users", "executor group"),
    )
    for invalid in ("", "bad/group", "bad group", "bad\nname", "1group"):
      with self.subTest(unix_name=invalid):
        with self.assertRaises(Fail):
          YARN_FUNCTIONS.validate_unix_name(invalid, "executor group")

    rendered = JinjaEnvironment(undefined=StrictUndefined).from_string(
      (PROPERTIES / "container-executor.cfg.j2").read_text(encoding="utf-8")
    ).render(
      nm_local_dirs="/hadoop/yarn/local",
      nm_log_dirs="/hadoop/yarn/log",
      yarn_executor_container_group="hadoop",
      min_user_id=1000,
      docker_module_enabled="false",
      docker_binary="/usr/bin/docker",
      docker_allowed_capabilities="CHOWN",
      docker_allowed_devices="",
      docker_allowed_networks="host",
      docker_allowed_ro_mounts="",
      docker_allowed_rw_mounts="",
      docker_privileged_containers_enabled="false",
      docker_trusted_registries="",
      docker_allowed_volume_drivers="",
      gpu_module_enabled="false",
      cgroup_root="/sys/fs/cgroup",
      yarn_hierarchy="yarn",
    )
    self.assertIn("min.user.id=1000", rendered)
    self.assertIn("  root=/sys/fs/cgroup\n  yarn-hierarchy=yarn\n", rendered)
    self.assertNotIn("yarn-hierarchy=yarn)", rendered)

    params_source = (SCRIPTS / "params_linux.py").read_text(encoding="utf-8")
    self.assertIn('container_executor_config["min_user_id"]', params_source)
    self.assertNotIn('yarn_env["min_user_id"]', params_source)
    advisor_source = (YARN / "service_advisor.py").read_text(encoding="utf-8")
    self.assertIn(
      'putContainerExecutorProperty("min_user_id", self.get_system_min_uid())',
      advisor_source,
    )

  def test_root_generated_limits_and_jaas_values_are_injection_safe(self):
    self.assertEqual(
      'svc/host@REALM\\\\name\\"quoted',
      YARN_FUNCTIONS.escape_java_quoted_string(
        'svc/host@REALM\\name"quoted', "Kerberos principal"
      ),
    )
    for invalid in ("", "principal\nnext", "keytab\0path", None, 1):
      with self.subTest(java_value=invalid):
        with self.assertRaises(Fail):
          YARN_FUNCTIONS.escape_java_quoted_string(
            invalid, "Kerberos JAAS value"
          )

    self.assertEqual(
      r"value\ with\ spaces\\and\:\=\#\!",
      YARN_FUNCTIONS.escape_java_properties_value(
        r"value with spaces\and:=#!", "Java properties value"
      ),
    )
    for invalid in ("line\nnext", "nul\0byte", None, 1):
      with self.subTest(properties_value=invalid):
        with self.assertRaises(Fail):
          YARN_FUNCTIONS.escape_java_properties_value(
            invalid, "Java properties value"
          )

    self.assertEqual(
      "file:///etc/security/keytabs/yarn.keytab",
      YARN_FUNCTIONS.local_file_uri(
        "/etc/security/keytabs/yarn.keytab", "ATS HBase keytab"
      ),
    )
    self.assertEqual(
      "file:///etc/key%20tabs/yarn%23service%3F.keytab",
      YARN_FUNCTIONS.local_file_uri(
        "/etc/key tabs/yarn#service?.keytab", "ATS HBase keytab"
      ),
    )
    for invalid in (
      "relative.keytab",
      "//host/keytab",
      "/etc/../root.keytab",
      "/etc/keytab\nnext",
      "/",
      None,
    ):
      with self.subTest(keytab_path=invalid):
        with self.assertRaises(Fail):
          YARN_FUNCTIONS.local_file_uri(invalid, "ATS HBase keytab")

    status_source = (SCRIPTS / "status_params.py").read_text(encoding="utf-8")
    for value_name in (
      "mapred_user",
      "yarn_user",
      "yarn_ats_user",
      "user_group",
    ):
      self.assertIn(f"{value_name} = validate_unix_name(", status_source)
    self.assertEqual(3, status_source.count("validate_runtime_directory_prefix("))

    params_source = (SCRIPTS / "params_linux.py").read_text(encoding="utf-8")
    for limit_name in (
      "yarn_user_nofile_limit",
      "yarn_user_nproc_limit",
      "mapred_user_nofile_limit",
      "mapred_user_nproc_limit",
    ):
      self.assertIn(f"{limit_name} = parse_positive_int(", params_source)

    for relative_path, property_names in (
      (
        "configuration/yarn-env.xml",
        ("yarn_user_nofile_limit", "yarn_user_nproc_limit"),
      ),
      (
        "configuration-mapred/mapred-env.xml",
        ("mapred_user_nofile_limit", "mapred_user_nproc_limit"),
      ),
    ):
      root = ET.parse(YARN / relative_path).getroot()
      for property_name in property_names:
        prop = next(
          entry
          for entry in root.findall("property")
          if entry.findtext("name") == property_name
        )
        self.assertEqual("int", prop.findtext("value-attributes/type"))
        self.assertEqual("1", prop.findtext("value-attributes/minimum"))

    jaas_templates = tuple(TEMPLATES.glob("*jaas.conf.j2"))
    self.assertEqual(7, len(jaas_templates))
    for template_path in jaas_templates:
      with self.subTest(template=template_path.name):
        template_source = template_path.read_text(encoding="utf-8")
        for line in template_source.splitlines():
          if "keyTab=" in line or "principal=" in line:
            self.assertIn("_jaas}}", line)

    yarn_source = (SCRIPTS / "yarn.py").read_text(encoding="utf-8")
    self.assertIn("if params.has_ats or params.has_atsv2:", yarn_source)

  def test_logfeeder_input_paths_are_encoded_as_json_values(self):
    configured_values = {
      "/configurations/yarn-env/yarn_log_dir_prefix": '/var/log/evil"\\\npath',
      "/configurations/yarn-env/yarn_user": 'yarn"\\user',
      "/configurations/mapred-env/mapred_log_dir_prefix": '/var/log/mapred"\\\npath',
      "/configurations/mapred-env/mapred_user": 'mapred"\\user',
    }

    def configured_default(path, fallback):
      return configured_values.get(path, fallback)

    environment = JinjaEnvironment(undefined=StrictUndefined)
    environment.globals.update(default=configured_default, json=json)
    yarn_document = json.loads(
      environment.from_string(
        (TEMPLATES / "input.config-yarn.json.j2").read_text(encoding="utf-8")
      ).render()
    )
    mapred_document = json.loads(
      environment.from_string(
        (TEMPLATES / "input.config-mapreduce2.json.j2").read_text(
          encoding="utf-8"
        )
      ).render()
    )
    self.assertEqual(4, len(yarn_document["input"]))
    self.assertEqual(1, len(mapred_document["input"]))
    self.assertEqual(
      configured_values["/configurations/yarn-env/yarn_log_dir_prefix"]
      + "/"
      + configured_values["/configurations/yarn-env/yarn_user"]
      + "/hadoop-"
      + configured_values["/configurations/yarn-env/yarn_user"]
      + "-nodemanager-*.log",
      yarn_document["input"][0]["path"],
    )
    self.assertEqual(
      configured_values[
        "/configurations/mapred-env/mapred_log_dir_prefix"
      ]
      + "/"
      + configured_values["/configurations/mapred-env/mapred_user"]
      + "/mapred-"
      + configured_values["/configurations/mapred-env/mapred_user"]
      + "-historyserver*.log",
      mapred_document["input"][0]["path"],
    )

    hook = (
      STACKS.parents[1]
      / "stack-hooks/after-INSTALL/scripts/shared_initialization.py"
    ).read_text(encoding="utf-8")
    self.assertIn("extra_imports=[default, json]", hook)

  def test_embedded_hbase_temp_directory_avoids_shared_tmp_by_default(self):
    configured = property_value(
      YARN / "configuration/yarn-hbase-site.xml", "hbase.tmp.dir"
    )
    self.assertEqual(
      "/var/lib/hadoop-yarn/embedded-yarn-ats-hbase/${user.name}/tmp",
      configured,
    )

  def test_service_address_port_supports_dns_and_bracketed_ipv6(self):
    self.assertEqual(
      8088,
      YARN_FUNCTIONS.parse_address_port("rm.example:8088", "RM address"),
    )
    self.assertEqual(
      8090,
      YARN_FUNCTIONS.parse_address_port("[2001:db8::1]:8090", "RM address"),
    )
    for invalid in (
      "",
      "rm.example",
      "rm.example:0",
      "2001:db8::1:8090",
      "[2001:db8::1]",
      "rm example:8088",
      "bad;host:8088",
      "999.999.999.999:8088",
      "[not-ip]:8088",
      "user@host:8088",
      "host/path:8088",
      "host?query:8088",
      "[fe80::1%eth0]:8088",
    ):
      with self.subTest(invalid=invalid):
        with self.assertRaises(Fail):
          YARN_FUNCTIONS.parse_address_port(invalid, "RM address")

  def test_zookeeper_quorum_requires_safe_unique_hosts(self):
    self.assertEqual(
      "zk1.example,10.0.0.2,[2001:db8::3]",
      YARN_FUNCTIONS.format_zookeeper_quorum(
        ["zk1.example", "10.0.0.2", "2001:db8::3"], "ZooKeeper quorum"
      ),
    )
    for invalid in (
      [],
      [""],
      ["zk1.example:2181"],
      ["bad host"],
      ["999.999.999.999"],
      ["zk1.example", "zk1.example"],
      ["ZK1.EXAMPLE", "zk1.example."],
      None,
    ):
      with self.subTest(invalid=invalid):
        with self.assertRaises(Fail):
          YARN_FUNCTIONS.format_zookeeper_quorum(invalid, "ZooKeeper quorum")

  def test_topology_and_decommission_inputs_are_validated_and_deterministic(self):
    all_hosts = YARN_FUNCTIONS.normalize_network_hosts(
      ["DN1.EXAMPLE", "nm1.example", "edge.example"], "all hosts"
    )
    all_ipv4 = YARN_FUNCTIONS.normalize_ipv4_addresses(
      ["10.0.0.1", "10.0.0.2", "10.0.0.3"], "all IPv4 addresses"
    )
    racks = YARN_FUNCTIONS.validate_rack_paths(
      ["/rack-a", "/zone-1/rack-b", "/rack-c"], "all racks"
    )
    self.assertEqual(
      (
        ("dn1.example", "10.0.0.1", "/rack-a"),
        ("nm1.example", "10.0.0.2", "/zone-1/rack-b"),
      ),
      YARN_FUNCTIONS.build_topology_mappings(
        all_hosts, all_ipv4, racks, ("nm1.example", "dn1.example")
      ),
    )
    self.assertEqual(
      ("nm1.example", "dn1.example"),
      YARN_FUNCTIONS.parse_network_host_csv(
        " nm1.example , DN1.EXAMPLE ", "decommissioned hosts"
      ),
    )
    self.assertEqual(
      (), YARN_FUNCTIONS.parse_network_host_csv("", "decommissioned hosts")
    )

    for invalid in (
      "host,,other",
      "host,host",
      "host\nother",
      "bad host",
      1,
    ):
      with self.subTest(decommissioned_hosts=invalid):
        with self.assertRaises(Fail):
          YARN_FUNCTIONS.parse_network_host_csv(invalid, "decommissioned hosts")
    for invalid in (["10.0.0.999"], ["10.0.0.1", "10.0.0.1"], [" 10.0.0.1"]):
      with self.subTest(ipv4=invalid):
        with self.assertRaises(Fail):
          YARN_FUNCTIONS.normalize_ipv4_addresses(invalid, "IPv4 addresses")
    for invalid in (["rack"], ["/rack\nnext"], ["/"], ["/rack bad"]):
      with self.subTest(racks=invalid):
        with self.assertRaises(Fail):
          YARN_FUNCTIONS.validate_rack_paths(invalid, "rack paths")
    for invalid_addresses, invalid_racks, service_hosts in (
      (("10.0.0.1",), racks, ()),
      (all_ipv4, ("/rack-a",), ()),
      (all_ipv4, racks, ("missing.example",)),
    ):
      with self.subTest(
        addresses=invalid_addresses,
        invalid_racks=invalid_racks,
        service_hosts=service_hosts,
      ):
        with self.assertRaises(Fail):
          YARN_FUNCTIONS.build_topology_mappings(
            all_hosts, invalid_addresses, invalid_racks, service_hosts
          )

    rendered = JinjaEnvironment(undefined=StrictUndefined).from_string(
      (TEMPLATES / "topology_mappings.data.j2").read_text(encoding="utf-8")
    ).render(
      topology_mappings=(
        ("dn1.example", "10.0.0.1", "/rack-a"),
        ("nm1.example", "10.0.0.2", "/rack-b"),
      )
    )
    self.assertLess(rendered.index("dn1.example"), rendered.index("nm1.example"))
    self.assertNotIn("all_racks[", rendered)

    params_source = (SCRIPTS / "params_linux.py").read_text(encoding="utf-8")
    self.assertIn(
      "tuple(host for host in nm_hosts if host not in excluded_host_set)",
      params_source,
    )

  def test_timeline_v2_versions_are_parsed_exactly(self):
    cases = (
      (True, "2.0f", "", True),
      (True, "1.5f", " 1.5f, 2.0f ", True),
      (True, "2.0f", "1.5f", False),
      (False, "invalid", "also-invalid", False),
    )
    for enabled, version, versions, expected in cases:
      with self.subTest(version=version, versions=versions):
        self.assertIs(
          expected,
          YARN_FUNCTIONS.timeline_service_v2_enabled(
            enabled, version, versions
          ),
        )
    for invalid in ("2", "2.0ff", "12.0f", "2.0-preview", "1.5f,,2.0f"):
      with self.subTest(invalid=invalid):
        with self.assertRaisesRegex(Fail, "Unsupported"):
          YARN_FUNCTIONS.timeline_service_v2_enabled(True, invalid, "")

  def test_memory_units_require_positive_integral_values(self):
    self.assertEqual("1024m", YARN_FUNCTIONS.ensure_unit_for_memory(1024))
    self.assertEqual("2g", YARN_FUNCTIONS.ensure_unit_for_memory(" 2G "))
    for invalid in (0, -1, "1.5g", "1gb", "12m garbage", "nan"):
      with self.subTest(invalid=invalid):
        with self.assertRaisesRegex(ValueError, "Invalid positive memory size"):
          YARN_FUNCTIONS.ensure_unit_for_memory(invalid)

  def test_root_sourced_environment_uses_quoted_values(self):
    template_text = property_value(YARN / "configuration/yarn-env.xml", "content")
    malicious = "/opt/path with spaces;$(touch /tmp/yarn-injection)"
    rendered = JinjaEnvironment(undefined=StrictUndefined).from_string(
      template_text
    ).render(
      security_enabled=True,
      has_ats=True,
      has_atsv2=True,
      has_registry_dns=True,
      hadoop_yarn_home_shell=quote_bash_args(malicious),
      yarn_log_dir_prefix_shell=quote_bash_args(malicious),
      yarn_pid_dir_prefix_shell=quote_bash_args(malicious),
      hadoop_libexec_dir_shell=quote_bash_args(malicious),
      java64_home_shell=quote_bash_args(malicious),
      hadoop_java_io_tmpdir_shell=quote_bash_args(malicious),
      yarn_heapsize_shell=quote_bash_args("1024;$(id)"),
      resourcemanager_heapsize_shell=quote_bash_args("2048;$(id)"),
      nodemanager_heapsize_shell=quote_bash_args("1024;$(id)"),
      apptimelineserver_heapsize_shell=quote_bash_args("1024;$(id)"),
      yarn_jaas_option_shell=quote_bash_args(f"-Djaas={malicious}"),
      yarn_ats_jaas_option_shell=quote_bash_args(f"-Dats={malicious}"),
      yarn_registry_dns_jaas_option_shell=quote_bash_args(
        f"-Dregistry={malicious}"
      ),
      yarn_nm_jaas_option_shell=quote_bash_args(f"-Dnm={malicious}"),
      rm_security_opts="configured",
      rm_security_opts_shell=quote_bash_args(f"-Dzk={malicious}"),
      registry_dns_needs_privileged_access=True,
      yarn_user_shell=quote_bash_args("yarn;$(id)"),
    )

    assignments = {}
    for line in rendered.splitlines():
      line = line.strip()
      if line.startswith("export "):
        line = line[len("export ") :]
      if "=" in line and not line.startswith(("if ", "HADOOP_OPTS=")):
        name, value = line.split("=", 1)
        if name.replace("_", "").isalnum():
          assignments[name] = value

    self.assertEqual([malicious], shlex.split(assignments["HADOOP_YARN_HOME"]))
    self.assertEqual([malicious], shlex.split(assignments["JAVA_HOME"]))
    self.assertEqual(
      [f"-Dregistry={malicious}"],
      shlex.split(assignments["YARN_REGISTRYDNS_OPTS"]),
    )
    self.assertEqual(
      ["yarn;$(id)"],
      shlex.split(assignments["YARN_REGISTRYDNS_SECURE_USER"]),
    )
    self.assertNotIn("{{java64_home}}", template_text)
    self.assertNotIn("{{yarn_pid_dir_prefix}}", template_text)

  def test_embedded_hbase_environment_uses_jdk17_gc_options(self):
    template_text = property_value(
      YARN / "configuration/yarn-hbase-env.xml", "content"
    )
    rendered = JinjaEnvironment(undefined=StrictUndefined).from_string(
      template_text
    ).render(
      java64_home_shell=quote_bash_args("/usr/lib/jvm/java-17"),
      yarn_hbase_conf_dir_shell=quote_bash_args(
        "/etc/hadoop/conf/embedded-yarn-ats-hbase"
      ),
      yarn_hbase_log_dir_shell=quote_bash_args(
        "/var/log/hadoop-yarn/embedded-yarn-ats-hbase"
      ),
      yarn_hbase_pid_dir_shell=quote_bash_args("/run/hadoop-yarn/yarn-ats"),
      java_version=17,
      yarn_hbase_java_io_tmpdir_shell=quote_bash_args("/tmp"),
      yarn_hbase_master_heapsize_shell=quote_bash_args("1024m"),
      yarn_hbase_regionserver_heapsize_shell=quote_bash_args("2048m"),
      security_enabled=False,
    )
    self.assertIn("-Xlog:gc*", rendered)
    self.assertIn("-XX:+UseG1GC", rendered)
    self.assertNotIn("UseConcMarkSweepGC", rendered)
    self.assertNotIn("CMSInitiatingOccupancyFraction", rendered)
    self.assertNotIn("PrintGCDateStamps", rendered)
    self.assertNotIn("-Xloggc", rendered)

  def test_embedded_hbase_and_mapred_shell_values_are_quoted(self):
    malicious = "/tmp/path with spaces;$(touch /tmp/yarn-hbase-injection)"
    hbase_template = property_value(
      YARN / "configuration/yarn-hbase-env.xml", "content"
    )
    rendered = JinjaEnvironment(undefined=StrictUndefined).from_string(
      hbase_template
    ).render(
      java64_home_shell=quote_bash_args(malicious),
      yarn_hbase_conf_dir_shell=quote_bash_args(malicious),
      yarn_hbase_log_dir_shell=quote_bash_args(malicious),
      yarn_hbase_pid_dir_shell=quote_bash_args(malicious),
      java_version=17,
      yarn_hbase_java_io_tmpdir_shell=quote_bash_args(malicious),
      yarn_hbase_master_heapsize_shell=quote_bash_args("1024m;$(id)"),
      yarn_hbase_regionserver_heapsize_shell=quote_bash_args("2048m;$(id)"),
      security_enabled=True,
      yarn_hbase_master_jaas_file_shell=quote_bash_args(malicious),
      yarn_hbase_regionserver_jaas_file_shell=quote_bash_args(malicious),
    )
    assignments = [
      line.strip()
      for line in rendered.splitlines()
      if line.strip().startswith(
        (
          "export JAVA_HOME=",
          "YARN_HBASE_CONF_DIR=",
          "YARN_HBASE_LOG_DIR=",
          "YARN_HBASE_PID_DIR=",
          "YARN_HBASE_JAVA_IO_TMPDIR=",
          "YARN_HBASE_MASTER_HEAPSIZE=",
          "YARN_HBASE_REGIONSERVER_HEAPSIZE=",
          "YARN_HBASE_MASTER_JAAS_FILE=",
          "YARN_HBASE_REGIONSERVER_JAAS_FILE=",
        )
      )
    ]
    self.assertEqual(9, len(assignments))
    for assignment in assignments:
      self.assertEqual(1 if not assignment.startswith("export ") else 2, len(shlex.split(assignment)))

    mapred_template = property_value(
      YARN / "configuration-mapred/mapred-env.xml", "content"
    )
    rendered_mapred = JinjaEnvironment(undefined=StrictUndefined).from_string(
      mapred_template
    ).render(
      jobhistory_heapsize_shell=quote_bash_args("900;$(id)"),
      mapred_log_dir_prefix_shell=quote_bash_args(malicious),
    )
    self.assertIn(quote_bash_args("900;$(id)"), rendered_mapred)
    self.assertIn(quote_bash_args(malicious), rendered_mapred)

  def test_agent_generated_configuration_is_root_owned_and_not_executable(self):
    source = (SCRIPTS / "yarn.py").read_text(encoding="utf-8")
    env_block = source[source.index('os.path.join(config_dir, "yarn-env.sh")') :]
    env_block = env_block[: env_block.index("content=InlineTemplate")]
    self.assertIn('owner="root"', env_block)
    self.assertIn("group=params.user_group", env_block)
    self.assertIn("mode=0o644", env_block)

    for config_name in (
      "core-site.xml",
      "hdfs-site.xml",
      "mapred-site.xml",
      "yarn-site.xml",
      "capacity-scheduler.xml",
      "resource-types.xml",
    ):
      with self.subTest(config=config_name):
        config_block = source[source.index(f'"{config_name}"') :]
        config_block = config_block[: config_block.index(")\n\n")]
        self.assertIn('owner="root"', config_block)
        self.assertIn("group=params.user_group", config_block)
        self.assertIn("mode=0o644", config_block)

    for jaas_name in (
      "yarn_jaas.conf",
      "yarn_ats_jaas.conf",
      "yarn_registry_dns_jaas.conf",
      "yarn_nm_jaas.conf",
      "mapred_jaas.conf",
      "yarn_hbase_master_jaas.conf",
      "yarn_hbase_regionserver_jaas.conf",
    ):
      with self.subTest(jaas=jaas_name):
        jaas_block = source[source.index(f'"{jaas_name}"') :]
        jaas_block = jaas_block[: jaas_block.index(")\n")]
        self.assertIn('owner="root"', jaas_block)
        self.assertIn("group=params.user_group", jaas_block)
        self.assertIn("mode=0o640", jaas_block)
    executor_block = source[
      source.index('format("{yarn_container_bin}/container-executor")') :
    ]
    executor_block = executor_block[: executor_block.index(")\n\n  File(")]
    self.assertIn('owner="root"', executor_block)
    self.assertIn("group=params.yarn_executor_container_group", executor_block)
    self.assertIn("mode=params.container_executor_mode", executor_block)
    container_block = source[
      source.index('os.path.join(config_dir, "container-executor.cfg")') :
    ]
    container_block = container_block[: container_block.index("content=InlineTemplate")]
    self.assertIn('owner="root"', container_block)
    self.assertIn('group="root"', container_block)
    self.assertIn("mode=0o400", container_block)

  def test_registry_dns_upgrade_selection_is_guarded(self):
    source = (SCRIPTS / "yarn_registry_dns.py").read_text(encoding="utf-8")
    upgrade_block = source[source.index("def pre_upgrade_restart") :]
    upgrade_block = upgrade_block[: upgrade_block.index("def status")]
    self.assertIn("params.version and check_stack_feature", upgrade_block)
    self.assertIn("StackFeature.ROLLING_UPGRADE", upgrade_block)
    self.assertEqual(1, upgrade_block.count("stack_select.select_packages"))


class TestYarnServiceJsonContract(unittest.TestCase):
  def _render(
    self,
    template_name,
    secure,
    has_metric_collector=True,
    system_tmpdir="/var/lib/yarn-ats/tmp",
  ):
    malicious = 'queue"},"injected":true,"x":"'
    values = {
      "yarn_hbase_service_queue_json": json.dumps(malicious),
      "yarn_hbase_archive_id_json": json.dumps("/apps/a\"b/hbase.tar.gz"),
      "yarn_service_framework_path_json": json.dumps("/apps/yarn/service.tar.gz"),
      "java64_home_json": json.dumps("/opt/java;$(id)"),
      "yarn_hbase_root_logger_json": json.dumps("INFO,RFA"),
      "yarn_hbase_system_service_opts_json": json.dumps(
        "-XX:+UseG1GC -XX:MaxGCPauseMillis=100 -XX:-ResizePLAB "
        "-XX:ErrorFile=${HBASE_LOG_DIR}/hs_err_pid%p.log "
        f"-Djava.io.tmpdir={system_tmpdir}"
      ),
      "yarn_hbase_log4j_source_json": json.dumps("/user/a/log4j.properties"),
      "yarn_hbase_site_source_json": json.dumps("/user/a/hbase-site.xml"),
      "yarn_hbase_policy_source_json": json.dumps("/user/a/hbase-policy.xml"),
      "yarn_hbase_grant_source_json": json.dumps(
        "/user/a/hbase_grant_permissions.rb"
      ),
      "yarn_hbase_core_site_source_json": json.dumps("/user/a/core-site.xml"),
      "yarn_hbase_metrics_source_json": json.dumps("/user/a/metrics.properties"),
      "yarn_hbase_master_containers": 1,
      "yarn_hbase_master_url_json": json.dumps(
        "http://${THIS_HOST}:16010/master-status"
      ),
      "yarn_hbase_master_cpu": 1,
      "yarn_hbase_master_memory_json": json.dumps("2048"),
      "yarn_hbase_master_opts_json": json.dumps("-Xms1024m -Xmx1024m"),
      "yarn_hbase_regionserver_containers": 2,
      "yarn_hbase_regionserver_url_json": json.dumps(
        "http://${THIS_HOST}:16030/rs-status"
      ),
      "yarn_hbase_regionserver_cpu": 1,
      "yarn_hbase_regionserver_memory_json": json.dumps("2048"),
      "yarn_hbase_regionserver_opts_json": json.dumps("-Xms1024m -Xmx1024m"),
      "yarn_hbase_client_containers": 1,
      "yarn_hbase_client_launch_command_json": json.dumps(
        '"$HBASE_HOME/bin/hbase" SchemaCreator && exec sleep infinity'
      ),
      "yarn_hbase_client_cpu": 1,
      "yarn_hbase_client_memory_json": json.dumps("1024"),
      "has_metric_collector": has_metric_collector,
    }
    if secure:
      values.update(
        yarn_ats_hbase_principal_json=json.dumps('yarn/host@REALM"'),
        yarn_ats_hbase_keytab_uri_json=json.dumps(
          "file:///etc/key%20tabs/yarn.keytab"
        ),
      )
    rendered = JinjaEnvironment(undefined=StrictUndefined).from_string(
      (TEMPLATES / template_name).read_text(encoding="utf-8")
    ).render(**values)
    return json.loads(rendered), malicious

  def test_unsecure_yarnfile_escapes_json_strings_and_keeps_numbers_typed(self):
    document, malicious = self._render("yarn_hbase_unsecure.yarnfile.j2", False)
    self.assertEqual(malicious, document["queue"])
    self.assertNotIn("injected", document)
    self.assertEqual(1, document["components"][0]["number_of_containers"])
    self.assertEqual(1, document["components"][0]["resource"]["cpus"])
    self.assertEqual("2048", document["components"][0]["resource"]["memory"])
    self.assertIn(
      "hadoop-metrics2-hbase.properties",
      [entry["dest_file"] for entry in document["configuration"]["files"]],
    )

  def test_yarnfile_omits_metrics_resource_without_collector(self):
    document, _ = self._render(
      "yarn_hbase_unsecure.yarnfile.j2", False, has_metric_collector=False
    )
    self.assertNotIn(
      "hadoop-metrics2-hbase.properties",
      [entry["dest_file"] for entry in document["configuration"]["files"]],
    )

  def test_yarnfile_uses_jdk17_gc_options(self):
    for template_name, secure in (
      ("yarn_hbase_unsecure.yarnfile.j2", False),
      ("yarn_hbase_secure.yarnfile.j2", True),
    ):
      with self.subTest(template=template_name):
        document, _ = self._render(template_name, secure)
        environment = document["configuration"]["env"]
        for home_variable in (
          "HADOOP_HOME",
          "HADOOP_COMMON_HOME",
          "HADOOP_HDFS_HOME",
          "HADOOP_MAPRED_HOME",
          "HADOOP_YARN_HOME",
        ):
          self.assertEqual("$PWD/lib/hadoop", environment[home_variable])
        self.assertEqual(
          "$PWD/lib/hadoop/libexec", environment["HADOOP_LIBEXEC_DIR"]
        )
        self.assertIn("-Xlog:gc*", environment["SERVER_GC_OPTS"])
        self.assertIn("-XX:+UseG1GC", environment["HBASE_OPTS"])
        self.assertIn(
          "-Djava.io.tmpdir=/var/lib/yarn-ats/tmp", environment["HBASE_OPTS"]
        )
        self.assertNotIn("ConcMarkSweep", json.dumps(document))
        self.assertNotIn("CMSInitiating", json.dumps(document))

  def test_yarnfile_json_escapes_custom_system_service_tmpdir(self):
    malicious = '/tmp/a";$(touch /tmp/not-executed)'
    for template_name, secure in (
      ("yarn_hbase_unsecure.yarnfile.j2", False),
      ("yarn_hbase_secure.yarnfile.j2", True),
    ):
      with self.subTest(template=template_name):
        document, _ = self._render(
          template_name, secure, system_tmpdir=malicious
        )
        self.assertIn(
          f"-Djava.io.tmpdir={malicious}",
          document["configuration"]["env"]["HBASE_OPTS"],
        )
        self.assertNotIn("injected", document)

  def test_secure_yarnfile_escapes_credentials_and_launch_command(self):
    document, _ = self._render("yarn_hbase_secure.yarnfile.j2", True)
    self.assertEqual('yarn/host@REALM"', document["kerberos_principal"]["principal_name"])
    self.assertEqual(
      "file:///etc/key tabs/yarn.keytab",
      document["kerberos_principal"]["keytab"],
    )
    client = next(
      component
      for component in document["components"]
      if component["name"] == "hbaseclient"
    )
    self.assertEqual(
      '"$HBASE_HOME/bin/hbase" SchemaCreator && exec sleep infinity',
      client["launch_command"],
    )
    files = document["configuration"]["files"]
    grant_file = next(
      entry for entry in files if entry["dest_file"] == "hbase_grant_permissions.rb"
    )
    self.assertEqual("STATIC", grant_file["type"])
    self.assertEqual("/user/a/hbase_grant_permissions.rb", grant_file["src_file"])


class TestYarnVersionAndResidue(unittest.TestCase):
  def test_configuration_property_names_are_unique(self):
    for config_directory in (
      YARN / "configuration",
      YARN / "configuration-mapred",
    ):
      for config_file in config_directory.glob("*.xml"):
        names = [
          prop.findtext("name")
          for prop in ET.parse(config_file).getroot().findall("property")
        ]
        duplicates = sorted({name for name in names if names.count(name) > 1})
        self.assertEqual([], duplicates, f"duplicate properties in {config_file}")

  def test_33_overlay_matches_bigtop_hadoop_version(self):
    for metainfo in (YARN / "metainfo.xml", YARN_33 / "metainfo.xml"):
      for service_name in ("YARN", "MAPREDUCE2"):
        with self.subTest(metainfo=metainfo, service=service_name):
          self.assertEqual(
            "3.3.6-1",
            service_version(metainfo, service_name),
          )

  def test_service_check_outer_timeouts_leave_cleanup_margin(self):
    root = ET.parse(YARN / "metainfo.xml").getroot()
    service_check_timeouts = [
      int(command.findtext("timeout"))
      for command in root.findall(".//commandScript")
      if command.findtext("script", "").endswith(
        ("/service_check.py", "/mapred_service_check.py")
      )
    ]
    self.assertEqual([600, 600], sorted(service_check_timeouts))
    self.assertTrue(all(timeout > 330 + 60 for timeout in service_check_timeouts))

  def test_obsolete_jar_installer_and_shell_archive_template_are_deleted(self):
    self.assertFalse((SCRIPTS / "install_jars.py").exists())
    self.assertFalse((TEMPLATES / "yarn_hbase_package_preparation.j2").exists())
    combined_source = "\n".join(
      path.read_text(encoding="utf-8") for path in SCRIPTS.glob("*.py")
    )
    self.assertNotIn("yarn-daemon.sh", combined_source)
    self.assertNotIn("mr-jobhistory-daemon.sh", combined_source)
    self.assertNotIn("grant_premissions", combined_source)
    self.assertNotIn("mapreduce.tar.gz", combined_source)
    self.assertNotIn("yarn_service_dep_source_path", combined_source)
    self.assertNotIn('copy_to_hdfs("tez"', combined_source)
    self.assertNotIn('copy_to_hdfs("mapreduce"', combined_source)
    self.assertNotIn('copy_to_hdfs("yarn"', combined_source)
    self.assertNotIn('config["configurations"]["tez-env"]', combined_source)
    self.assertNotIn('config["configurations"]["hbase-env"]["hbase_user"]', combined_source)

    params_source = (SCRIPTS / "params_linux.py").read_text(encoding="utf-8")
    self.assertEqual(4, params_source.count('"/configurations/yarn-hbase-log4j/'))
    self.assertNotIn('"configurations/yarn-hbase-log4j/', params_source)

    yarn_site_names = {
      prop.findtext("name")
      for prop in ET.parse(YARN / "configuration/yarn-site.xml").getroot().findall(
        "property"
      )
    }
    self.assertNotIn("yarn.service.framework.path", yarn_site_names)
    self.assertIn("yarn.system-metrics-publisher.enabled", yarn_site_names)
    self.assertIn(
      "yarn.rm.system-metrics-publisher.emit-container-events", yarn_site_names
    )
    self.assertIn("yarn.webapp.filter-entity-list-by-user", yarn_site_names)
    self.assertIn(
      "yarn.nodemanager.resourcemanager.connect.max-wait.ms", yarn_site_names
    )
    self.assertIn("yarn.resourcemanager.fs.state-store.num-retries", yarn_site_names)
    self.assertIn(
      "yarn.resourcemanager.fs.state-store.retry-interval-ms", yarn_site_names
    )
    self.assertIn("yarn.nodemanager.resource.cpu.enabled", yarn_site_names)
    for current_name in (
      "hadoop.zk.address",
      "hadoop.zk.acl",
      "hadoop.zk.retry-interval-ms",
      "hadoop.zk.num-retries",
      "hadoop.zk.timeout-ms",
    ):
      self.assertIn(current_name, yarn_site_names)
    self.assertIn(
      "yarn.nodemanager.resource-plugins.gpu.docker-plugin.nvidia-docker-v1.endpoint",
      yarn_site_names,
    )
    for obsolete_name in (
      "yarn.system-metricspublisher.enabled",
      "yarn.rm.system-metricspublisher.emit-container-events",
      "yarn.resourcemanager.display.per-user-apps",
      "yarn.nodemanager.resourcemanager.connect.wait.secs",
      "yarn.resourcemanager.fs.state-store.retry-policy-spec",
      "yarn.node-labels.fs-store.retry-policy-spec",
      "yarn.resourcemanager.system-metrics-publisher.enabled",
      "yarn.nodemanager.linux-container-executor.resources-handler.class",
      "yarn.nodemanager.resource-plugins.gpu.docker-plugin.nvidiadocker-v1.endpoint",
      "yarn.resourcemanager.zk-address",
      "yarn.resourcemanager.zk-acl",
      "yarn.resourcemanager.zk-retry-interval-ms",
      "yarn.resourcemanager.zk-num-retries",
      "yarn.resourcemanager.zk-timeout-ms",
    ):
      self.assertNotIn(obsolete_name, yarn_site_names)

    hbase_site_names = {
      prop.findtext("name")
      for prop in ET.parse(
        YARN / "configuration/yarn-hbase-site.xml"
      ).getroot().findall("property")
    }
    for obsolete_name in (
      "hbase.zookeeper.useMulti",
      "hbase.defaults.for.version.skip",
      "hbase.table.sanity.checks",
      "hbase.bucketcache.percentage.in.combinedcache",
    ):
      self.assertNotIn(obsolete_name, hbase_site_names)

    combined_yarn_source = "\n".join(
      path.read_text(encoding="utf-8")
      for path in YARN.rglob("*")
      if path.is_file() and path.suffix in {".py", ".j2", ".xml", ".json"}
    )
    for obsolete_contract in (
      "TASKTRACKER",
      "task-controller",
      "taskcontroller.cfg",
      "mapreduce.tasktracker.group",
      "CgroupsLCEResourcesHandler",
      "cgroups_test",
      "sampleValidator",
      '"/mapred/system"',
      "MAPR_SERVER_ROLE_DIRECTORY_MAP",
      "YARN_SERVER_ROLE_DIRECTORY_MAP",
      "sysprep_skip_copy_tarballs_hdfs",
      "namenode_hostname",
      "ats_leveldb_lock_file",
    ):
      self.assertNotIn(obsolete_contract, combined_yarn_source)

  def test_hbase_backend_modes_fail_closed(self):
    valid_modes = (
      (False, False, False, [], False),
      (False, True, False, [], False),
      (False, False, True, [], False),
      (True, False, True, ["hbase.example"], True),
    )
    for mode in valid_modes:
      with self.subTest(mode=mode):
        self.assertIsNone(YARN_FUNCTIONS.validate_hbase_backend_mode(*mode))

    invalid_modes = (
      (False, True, True, [], False),
      (True, True, True, ["hbase.example"], True),
      (True, False, False, ["hbase.example"], True),
      (True, False, True, [], True),
      (True, False, True, ["hbase.example"], False),
    )
    for mode in invalid_modes:
      with self.subTest(mode=mode):
        with self.assertRaises(Fail):
          YARN_FUNCTIONS.validate_hbase_backend_mode(*mode)

  def test_external_ranger_credentials_never_use_known_defaults(self):
    valid = {
      "external_admin_username": "external-admin",
      "external_admin_password": "secret-one",
      "external_ranger_admin_username": "service-account",
      "external_ranger_admin_password": "secret-two",
    }
    self.assertEqual(
      valid, YARN_FUNCTIONS.require_external_ranger_credentials(valid)
    )
    for missing_property in valid:
      with self.subTest(missing_property=missing_property):
        invalid = dict(valid)
        invalid[missing_property] = ""
        with self.assertRaisesRegex(Fail, missing_property):
          YARN_FUNCTIONS.require_external_ranger_credentials(invalid)
    params_source = (SCRIPTS / "params_linux.py").read_text(encoding="utf-8")
    self.assertNotIn('external_admin_username", "admin"', params_source)
    self.assertNotIn('"amb_ranger_admin"', params_source)

  def test_external_hbase_configuration_is_never_managed_by_yarn(self):
    source = (SCRIPTS / "yarn.py").read_text(encoding="utf-8")
    self.assertIn("manages_embedded_hbase = not params.use_external_hbase", source)
    self.assertIn("if manages_embedded_hbase:", source)
    self.assertIn("if manages_embedded_hbase and not isinstance(", source)

  def test_service_dependencies_match_backend_contracts(self):
    root = ET.parse(YARN / "metainfo.xml").getroot()
    yarn_service = next(
      service
      for service in root.findall("./services/service")
      if service.findtext("name") == "YARN"
    )
    required = {
      service.text for service in yarn_service.findall("./requiredServices/service")
    }
    dependencies = {
      dependency.findtext("name")
      for dependency in root.findall(".//dependency")
    }
    self.assertNotIn("MAPREDUCE2", required)
    self.assertNotIn("TEZ/TEZ_CLIENT", dependencies)
    self.assertNotIn("SLIDER/SLIDER", dependencies)
    timeline_reader = next(
      component
      for component in yarn_service.findall("./components/component")
      if component.findtext("name") == "TIMELINE_READER"
    )
    hbase_dependency = next(
      dependency
      for dependency in timeline_reader.findall("./dependencies/dependency")
      if dependency.findtext("name") == "HBASE/HBASE_CLIENT"
    )
    condition = hbase_dependency.find("./conditions/condition")
    self.assertEqual("yarn-hbase-env", condition.findtext("configType"))
    self.assertEqual("hbase_within_cluster", condition.findtext("property"))
    self.assertEqual("true", condition.findtext("propertyValue"))

  def test_yarn_component_packages_and_ats_hbase_install_matrix(self):
    stack_packages = json.loads(
      (STACKS / "3.2.0/properties/stack_packages.json").read_text(
        encoding="utf-8"
      )
    )["BIGTOP"]["stack-select"]["YARN"]
    expected_selectors = {
      "APP_TIMELINE_SERVER": "hadoop-yarn-timelineserver",
      "TIMELINE_READER": "hadoop-yarn-timelinereader",
      "YARN_REGISTRY_DNS": "hadoop-yarn-registrydns",
    }
    for component, selector in expected_selectors.items():
      with self.subTest(component=component):
        self.assertEqual(
          selector, stack_packages[component]["STACK-SELECT-PACKAGE"]
        )
        self.assertIn(selector, stack_packages[component]["INSTALL"])

    base = {
      "role": "install_packages",
      "localComponents": [],
      "configurations": {
        "yarn-hbase-env": {
          "is_hbase_system_service_launch": "false",
          "use_external_hbase": "false",
          "hbase_within_cluster": "false",
        }
      },
    }
    cases = (
      (["TIMELINE_READER"], {}, True),
      (["RESOURCEMANAGER"], {}, False),
      (
        ["RESOURCEMANAGER"],
        {"is_hbase_system_service_launch": "true"},
        True,
      ),
      (["TIMELINE_READER"], {"use_external_hbase": "true"}, False),
      (["TIMELINE_READER"], {"hbase_within_cluster": "true"}, False),
    )
    for local_components, overrides, expected in cases:
      config = json.loads(json.dumps(base))
      config["localComponents"] = local_components
      config["configurations"]["yarn-hbase-env"].update(overrides)
      with (
        self.subTest(components=local_components, overrides=overrides),
        patch.object(package_conditions.Script, "get_config", return_value=config),
      ):
        self.assertEqual(
          expected, package_conditions.should_install_yarn_ats_hbase()
        )

    for local_components, expected in (
      (["TIMELINE_READER"], True),
      (["RESOURCEMANAGER"], True),
      (["NODEMANAGER"], False),
    ):
      config = {
        "role": "install_packages",
        "localComponents": local_components,
        "configurations": {},
      }
      with (
        self.subTest(missing_config_components=local_components),
        patch.object(package_conditions.Script, "get_config", return_value=config),
      ):
        self.assertEqual(
          expected, package_conditions.should_install_yarn_ats_hbase()
        )

    invalid = json.loads(json.dumps(base))
    invalid["localComponents"] = ["TIMELINE_READER"]
    invalid["configurations"]["yarn-hbase-env"]["use_external_hbase"] = "yes"
    with patch.object(
      package_conditions.Script, "get_config", return_value=invalid
    ):
      with self.assertRaisesRegex(Fail, "use_external_hbase"):
        package_conditions.should_install_yarn_ats_hbase()


if __name__ == "__main__":
  unittest.main()
