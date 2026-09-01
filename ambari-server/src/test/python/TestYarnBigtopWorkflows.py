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
import io
import os
from pathlib import Path
import pwd
import stat
import sys
import tarfile
import tempfile
from types import ModuleType, SimpleNamespace
import unittest
from unittest.mock import ANY, MagicMock, call, patch

from resource_management.core.exceptions import Fail


YARN = (
  Path(__file__).resolve().parents[2]
  / "main/resources/stacks/BIGTOP/3.2.0/services/YARN"
)
SCRIPTS = YARN / "package/scripts"
ALERTS = YARN / "package/alerts"


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


YARN_PROCESS_UTILS = load_module(
  "yarn_process_utils", SCRIPTS / "yarn_process_utils.py"
)
YARN_SERVICE_CHECK = load_module(
  "bigtop_yarn_service_check",
  SCRIPTS / "service_check.py",
  {"yarn_process_utils": YARN_PROCESS_UTILS},
)
MAPRED_SERVICE_CHECK = load_module(
  "bigtop_mapred_service_check",
  SCRIPTS / "mapred_service_check.py",
  {"yarn_process_utils": YARN_PROCESS_UTILS},
)
HBASE_SERVICE = load_module(
  "bigtop_yarn_hbase_service",
  SCRIPTS / "hbase_service.py",
  {"yarn_process_utils": YARN_PROCESS_UTILS},
)
YARN_CONFIG = load_module(
  "bigtop_yarn_config",
  SCRIPTS / "yarn.py",
  {"hbase_service": HBASE_SERVICE},
)
RANGER_YARN = load_module(
  "bigtop_setup_ranger_yarn", SCRIPTS / "setup_ranger_yarn.py"
)

_TIMELINE_YARN = ModuleType("yarn")
_TIMELINE_YARN.yarn = MagicMock()
_TIMELINE_YARN._validated_resource_manager_host_files = (
  YARN_CONFIG._validated_resource_manager_host_files
)
_TIMELINE_SERVICE = ModuleType("service")
_TIMELINE_SERVICE.service = MagicMock()
_TIMELINE_HBASE = ModuleType("hbase_service")
_TIMELINE_HBASE.hbase = MagicMock()
_TIMELINE_HBASE.configure_hbase = MagicMock()
_TIMELINE_HBASE.rollback_hbase_roles = MagicMock()
TIMELINE_READER = load_module(
  "bigtop_yarn_timeline_reader",
  SCRIPTS / "timelinereader.py",
  {
    "yarn": _TIMELINE_YARN,
    "service": _TIMELINE_SERVICE,
    "hbase_service": _TIMELINE_HBASE,
    "yarn_process_utils": YARN_PROCESS_UTILS,
  },
)
_RM_RANGER = ModuleType("setup_ranger_yarn")
_RM_RANGER.setup_ranger_yarn = MagicMock()
RESOURCE_MANAGER = load_module(
  "bigtop_yarn_resource_manager",
  SCRIPTS / "resourcemanager.py",
  {
    "yarn": _TIMELINE_YARN,
    "service": _TIMELINE_SERVICE,
    "setup_ranger_yarn": _RM_RANGER,
    "yarn_process_utils": YARN_PROCESS_UTILS,
  },
)
NODEMANAGER_HEALTH_ALERT = load_module(
  "bigtop_yarn_nodemanager_health_alert", ALERTS / "alert_nodemanager_health.py"
)
NODEMANAGERS_SUMMARY_ALERT = load_module(
  "bigtop_yarn_nodemanagers_summary_alert",
  ALERTS / "alert_nodemanagers_summary.py",
)
ATS_HBASE_ALERT = load_module(
  "bigtop_yarn_ats_hbase_alert", ALERTS / "alert_ats_hbase.py"
)
NODEMANAGER_UPGRADE = load_module(
  "bigtop_yarn_nodemanager_upgrade", SCRIPTS / "nodemanager_upgrade.py"
)
YARN_ADVISOR = load_module("bigtop_yarn_service_advisor", YARN / "service_advisor.py")


class TestYarnComponentWorkflows(unittest.TestCase):
  def test_timeline_reader_stops_before_embedded_hbase(self):
    params = params_module(
      use_external_hbase=False,
      is_hbase_system_service_launch=False,
    )
    events = []
    with patch.dict(sys.modules, {"params": params}), \
      patch.object(
        TIMELINE_READER,
        "service",
        side_effect=lambda *args, **kwargs: events.append(("reader", kwargs)),
      ), \
      patch.object(
        TIMELINE_READER,
        "hbase",
        side_effect=lambda *args, **kwargs: events.append(("hbase", kwargs)),
      ):
      TIMELINE_READER.ApplicationTimelineReader().stop(MagicMock())
    self.assertEqual(["reader", "hbase"], [event[0] for event in events])

  def test_timeline_reader_stop_failure_still_stops_embedded_hbase(self):
    params = params_module(
      use_external_hbase=False,
      is_hbase_system_service_launch=False,
    )
    with patch.dict(sys.modules, {"params": params}), \
      patch.object(TIMELINE_READER, "service", side_effect=Fail("reader stop failed")), \
      patch.object(TIMELINE_READER, "hbase") as hbase:
      with self.assertRaisesRegex(RuntimeError, "reader stop failed"):
        TIMELINE_READER.ApplicationTimelineReader().stop(MagicMock())
    hbase.assert_called_once_with(action="stop")

  def test_timeline_reader_stop_reports_reader_and_hbase_failures(self):
    params = params_module(
      use_external_hbase=False,
      is_hbase_system_service_launch=False,
    )
    with patch.dict(sys.modules, {"params": params}), \
      patch.object(TIMELINE_READER, "service", side_effect=Fail("reader failed")), \
      patch.object(TIMELINE_READER, "hbase", side_effect=Fail("hbase failed")):
      with self.assertRaisesRegex(
        RuntimeError, "reader failed.*hbase failed"
      ) as raised:
        TIMELINE_READER.ApplicationTimelineReader().stop(MagicMock())
    self.assertIsInstance(raised.exception.__cause__, Fail)

  def test_timeline_reader_start_failure_rolls_back_only_new_hbase_roles(self):
    params = params_module(
      use_external_hbase=False,
      is_hbase_system_service_launch=False,
    )
    reader = TIMELINE_READER.ApplicationTimelineReader()
    env = MagicMock()
    with patch.dict(sys.modules, {"params": params}), \
      patch.object(reader, "configure"), \
      patch.object(
        TIMELINE_READER,
        "hbase",
        return_value=("master", "regionserver"),
      ), \
      patch.object(
        TIMELINE_READER,
        "service",
        side_effect=Fail("reader start failed"),
      ), \
      patch.object(
        TIMELINE_READER, "rollback_hbase_roles", return_value=[]
      ) as rollback:
      with self.assertRaisesRegex(Fail, "reader start failed"):
        reader.start(env)
    rollback.assert_called_once_with(("master", "regionserver"))

  def test_secure_nodemanager_upgrade_requires_complete_credentials(self):
    params = params_module(
      security_enabled=True,
      nodemanager_principal_name="",
      nodemanager_keytab="",
      yarn_user="yarn",
      user_group="hadoop",
      yarn_log_dir="/var/log/hadoop-yarn",
    )
    with patch.dict(sys.modules, {"params": params}), \
      patch.object(NODEMANAGER_UPGRADE, "PrivateKerberosCache") as cache, \
      patch.object(NODEMANAGER_UPGRADE, "show_logs"):
      with self.assertRaisesRegex(Fail, "principal and keytab are required"):
        NODEMANAGER_UPGRADE.post_upgrade_check()
    cache.assert_not_called()

  def test_nodemanager_upgrade_matches_exact_running_node_id(self):
    output = """Total Nodes:2
Node-Id Node-State Node-Http-Address Number-of-Running-Containers
nm10.example:45454 RUNNING nm10.example:8042 0
[2001:db8::10]:45454 RUNNING [2001:db8::10]:8042 0
"""
    running = NODEMANAGER_UPGRADE._running_node_ids(output)
    self.assertFalse(
      any(
        NODEMANAGER_UPGRADE._node_matches(
          node_id, {"nm1.example", "192.0.2.1"}, "45454"
        )
        for node_id in running
      )
    )
    self.assertTrue(
      any(
        NODEMANAGER_UPGRADE._node_matches(
          node_id, {"nm10.example", "192.0.2.10"}, "45454"
        )
        for node_id in running
      )
    )
    self.assertEqual(
      ("2001:db8::10", "45454"),
      NODEMANAGER_UPGRADE._split_node_id("[2001:db8::10]:45454"),
    )

  def test_nodemanager_dns_aliases_support_ipv6_and_optional_dns(self):
    with patch.object(
      NODEMANAGER_UPGRADE.socket,
      "getaddrinfo",
      return_value=[
        (2, 1, 6, "", ("192.0.2.10", 0)),
        (10, 1, 6, "", ("2001:db8::10", 0, 0, 0)),
      ],
    ):
      self.assertEqual(
        {"192.0.2.10", "2001:db8::10"},
        NODEMANAGER_UPGRADE._resolve_host_aliases("nm.example"),
      )
    with patch.object(
      NODEMANAGER_UPGRADE.socket,
      "getaddrinfo",
      side_effect=NODEMANAGER_UPGRADE.socket.gaierror("temporary failure"),
    ):
      self.assertEqual(set(), NODEMANAGER_UPGRADE._resolve_host_aliases("nm.example"))

  def test_secure_refresh_nodes_uses_private_cache_and_timeout(self):
    params = params_module(
      yarn_user="yarn",
      hadoop_conf_dir="/etc/hadoop/conf",
      stack_root="/usr/bigtop",
      user_group="hadoop",
      exclude_file_path="/etc/hadoop/conf/yarn.exclude",
      include_hosts=False,
      net_topology_mapping_data_file_path="/etc/hadoop/conf/topology.data",
      hdfs_user="hdfs",
      update_files_only=False,
      yarn_container_bin="/usr/lib/hadoop-yarn/bin",
      execute_path="/usr/bin:/bin",
      security_enabled=True,
      kinit_path_local="/usr/bin/kinit",
      rm_keytab="/etc/security/keytabs/rm.keytab",
      rm_principal_name="rm/host@REALM",
    )
    cache = MagicMock()
    cache.merge_environment.return_value = {
      "PATH": "/usr/bin:/bin",
      "KRB5CCNAME": "FILE:/tmp/private/krb5cc",
    }
    cache_context = MagicMock()
    cache_context.__enter__.return_value = cache
    with patch.dict(sys.modules, {"params": params}), \
      patch.object(RESOURCE_MANAGER, "File"), \
      patch.object(
        RESOURCE_MANAGER,
        "_validated_resource_manager_host_files",
        return_value=("/etc/hadoop/conf/yarn.exclude", None),
      ), \
      patch.object(
        RESOURCE_MANAGER, "PrivateKerberosCache", return_value=cache_context
      ), \
      patch.object(RESOURCE_MANAGER, "Execute") as execute:
      RESOURCE_MANAGER.ResourcemanagerDefault().decommission(MagicMock())
    execute.assert_called_once_with(
      (
        "/usr/lib/hadoop-yarn/bin/yarn",
        "--config",
        "/etc/hadoop/conf",
        "rmadmin",
        "-refreshNodes",
      ),
      environment={
        "PATH": "/usr/bin:/bin",
        "KRB5CCNAME": "FILE:/tmp/private/krb5cc",
      },
      user="yarn",
      timeout=60,
      timeout_kill_strategy=RESOURCE_MANAGER.TerminateStrategy.KILL_PROCESS_GROUP,
    )

  def test_disable_security_uses_bounded_structured_zookeeper_migrations(self):
    params = params_module(
      stack_supports_zk_security=True,
      rm_zk_address="zk1.example:2181,zk2.example:2181/yarn",
      ambari_java_exec="/usr/bin/java",
      ambari_java_home="/usr/lib/jvm/java-17",
      yarn_jaas_file="/etc/hadoop/conf/yarn jaas.conf",
      yarn_user="yarn",
      rm_zk_znode="/rmstore;$(id)",
      hadoop_registry_zk_root="/registry",
      rm_zk_failover_znode="/leader-election",
    )
    with patch.dict(sys.modules, {"params": params}), \
      patch(
        "resource_management.core.resources.zkmigrator.Execute"
      ) as execute:
      RESOURCE_MANAGER.ResourcemanagerDefault().disable_security(MagicMock())

    self.assertEqual(3, execute.call_count)
    for invocation in execute.call_args_list:
      self.assertIsInstance(invocation.args[0], tuple)
      self.assertEqual(60, invocation.kwargs["timeout"])
      self.assertEqual(
        RESOURCE_MANAGER.TerminateStrategy.KILL_PROCESS_GROUP,
        invocation.kwargs["timeout_kill_strategy"],
      )

  def test_disable_security_rejects_root_or_traversing_znodes(self):
    for unsafe_znode in ("/", "relative", "/rmstore/../other"):
      params = params_module(
        stack_supports_zk_security=True,
        rm_zk_address="zk.example:2181",
        ambari_java_exec="/usr/bin/java",
        ambari_java_home="/usr/lib/jvm/java-17",
        yarn_jaas_file="/etc/hadoop/conf/yarn_jaas.conf",
        yarn_user="yarn",
        rm_zk_znode=unsafe_znode,
        hadoop_registry_zk_root="/registry",
        rm_zk_failover_znode="/leader-election",
      )
      with self.subTest(znode=unsafe_znode), \
        patch.dict(sys.modules, {"params": params}), \
        patch(
          "resource_management.core.resources.zkmigrator.Execute"
        ) as execute:
        with self.assertRaises(Fail):
          RESOURCE_MANAGER.ResourcemanagerDefault().disable_security(
            MagicMock()
          )
      execute.assert_not_called()

  def test_hdfs_directory_fallback_is_bounded_and_credential_scoped(self):
    params = params_module(
      hdfs_site={},
      is_webhdfs_enabled=YARN_FUNCTIONS.parse_boolean("false"),
      dfs_type="HDFS",
      hadoop_bin_dir="/usr/lib/hadoop/bin",
      hadoop_conf_dir="/etc/hadoop/conf",
      hdfs_user="hdfs",
    )
    environment = {"KRB5CCNAME": "FILE:/tmp/private/krb5cc"}
    with patch.dict(sys.modules, {"params": params}), \
      patch.object(
        RESOURCE_MANAGER.namenode_ha_utils, "get_nameservices", return_value=[]
      ), \
      patch.object(
        RESOURCE_MANAGER.WebHDFSUtil, "is_webhdfs_available", return_value=False
      ), \
      patch.object(RESOURCE_MANAGER.shell, "call", return_value=(0, "")) as call:
      RESOURCE_MANAGER.ResourcemanagerDefault().wait_for_dfs_directory_created(
        "/ats/active", [], environment
      )
    self.assertEqual("hdfs", call.call_args.args[0][0].rsplit("/", 1)[-1])
    self.assertEqual(environment, call.call_args.kwargs["env"])
    self.assertEqual(30, call.call_args.kwargs["timeout"])
    RESOURCE_MANAGER.WebHDFSUtil.is_webhdfs_available.assert_called_once_with(
      False, "HDFS"
    )


class TestYarnAdvisorContract(unittest.TestCase):
  def test_missing_parent_advisor_preserves_file_error(self):
    missing = "/definitely-missing/bigtop-service-advisor.py"
    with patch.dict(os.environ, {"BASE_SERVICE_ADVISOR": missing}):
      with self.assertRaisesRegex(FileNotFoundError, missing):
        load_module("bigtop_yarn_missing_parent", YARN / "service_advisor.py")

  def test_advisor_contains_only_bigtop_service_contracts(self):
    source = (YARN / "service_advisor.py").read_text(encoding="utf-8")
    kerberos = (YARN / "kerberos.json").read_text(encoding="utf-8")
    for residue in (
      "HDP",
      "HIVE_SERVER_INTERACTIVE",
      "hive-interactive",
      "enable_hive_interactive",
      "LLAP",
      "llap",
      "SPARK2",
    ):
      self.assertNotIn(residue, source)
      self.assertNotIn(residue, kerberos)

  def test_ats_keytabs_are_scoped_to_runtime_components(self):
    descriptor = json.loads((YARN / "kerberos.json").read_text(encoding="utf-8"))
    yarn_service = next(
      service for service in descriptor["services"] if service["name"] == "YARN"
    )
    ats_identities = {
      "yarn_ats",
      "yarn_ats_hbase_master",
      "yarn_ats_hbase_regionserver",
    }
    self.assertTrue(
      ats_identities.isdisjoint(
        identity["name"] for identity in yarn_service["identities"]
      )
    )
    components = {
      component["name"]: component for component in yarn_service["components"]
    }
    def predicate_matches(predicate, values):
      if predicate is None:
        return True
      if "equals" in predicate:
        key, expected = predicate["equals"]
        return values.get(key) == expected
      if "and" in predicate:
        self.assertEqual(2, len(predicate["and"]))
        return all(predicate_matches(item, values) for item in predicate["and"])
      self.fail(f"unsupported descriptor predicate: {predicate}")

    def selected_ats_identities(component_name, values):
      return {
        identity["name"]
        for identity in components[component_name]["identities"]
        if identity["name"] in ats_identities
        and predicate_matches(identity.get("when"), values)
      }

    base_values = {
      "configurations/yarn-hbase-env/is_hbase_system_service_launch": "false",
      "configurations/yarn-hbase-env/use_external_hbase": "false",
      "configurations/yarn-hbase-env/hbase_within_cluster": "false",
    }
    deployment_matrix = (
      ("embedded", {}, ats_identities, set()),
      (
        "external",
        {"configurations/yarn-hbase-env/use_external_hbase": "true"},
        {"yarn_ats"},
        set(),
      ),
      (
        "cluster-hbase",
        {"configurations/yarn-hbase-env/hbase_within_cluster": "true"},
        {"yarn_ats"},
        set(),
      ),
      (
        "system-service",
        {
          "configurations/yarn-hbase-env/is_hbase_system_service_launch": "true"
        },
        {"yarn_ats"},
        ats_identities,
      ),
    )
    for mode, overrides, timeline_expected, nodemanager_expected in deployment_matrix:
      values = dict(base_values)
      values.update(overrides)
      with self.subTest(mode=mode):
        self.assertEqual(
          timeline_expected,
          selected_ats_identities("TIMELINE_READER", values),
        )
        self.assertEqual(
          nodemanager_expected,
          selected_ats_identities("NODEMANAGER", values),
        )

    resource_manager_text = json.dumps(
      components["RESOURCEMANAGER"]["identities"]
    )
    self.assertNotIn("yarn_ats", resource_manager_text)
    for component_name in (
      "APP_TIMELINE_SERVER",
      "YARN_REGISTRY_DNS",
    ):
      identity_text = json.dumps(components[component_name]["identities"])
      self.assertNotIn("yarn_ats_hbase", identity_text)

    registry_accounts = yarn_service["configurations"][0]["yarn-site"][
      "hadoop.registry.system.accounts"
    ]
    for optional_service in ("MAPREDUCE2", "HIVE", "SPARK"):
      self.assertNotIn(f"${{principals/{optional_service}/", registry_accounts)
    self.assertNotIn("${principals/YARN/APP_TIMELINE_SERVER", registry_accounts)
    self.assertIn("${yarn-env/yarn_user}", registry_accounts)
    self.assertIn("${hadoop-env/hdfs_user}", registry_accounts)

  def test_service_validation_entry_points_use_their_own_validators(self):
    cases = (
      (YARN_ADVISOR.YARNServiceAdvisor, "YARNValidator"),
      (YARN_ADVISOR.MAPREDUCE2ServiceAdvisor, "MAPREDUCE2Validator"),
    )
    for advisor_class, validator_name in cases:
      with self.subTest(advisor=advisor_class.__name__):
        advisor = object.__new__(advisor_class)
        advisor.logger = MagicMock()
        validator = MagicMock()
        validator.validators = [("site", MagicMock())]
        validator.validateListOfConfigUsingMethod.return_value = [validator_name]
        with patch.object(YARN_ADVISOR, validator_name, return_value=validator):
          result = advisor.getServiceConfigurationsValidationItems(
            {}, {}, {}, {}
          )
        self.assertEqual([validator_name], result)
        validator.validateListOfConfigUsingMethod.assert_called_once_with(
          {}, {}, {}, {}, validator.validators
        )

  def test_yarn_runtime_validator_uses_proposed_http_policy_matrix(self):
    validator = object.__new__(YARN_ADVISOR.YARNValidator)
    validator.logger = MagicMock()
    existing_services = {
      "configurations": {
        "yarn-site": {
          "properties": {
            "yarn.http.policy": "HTTP_ONLY",
            "yarn.timeline-service.webapp.address": "old.example:8188",
            "yarn.log.server.web-service.url": "http://old.invalid",
          }
        }
      }
    }
    cases = (
      {
        "yarn.http.policy": "HTTP_ONLY",
        "yarn.timeline-service.webapp.address": "new.example:8188",
        "yarn.log.server.web-service.url": (
          "http://new.example:8188/ws/v1/applicationhistory"
        ),
      },
      {
        "yarn.http.policy": "HTTPS_ONLY",
        "yarn.timeline-service.webapp.https.address": "new.example:8190",
        "yarn.log.server.web-service.url": (
          "https://new.example:8190/ws/v1/applicationhistory"
        ),
      },
    )
    with patch.object(validator, "getErrorItem", side_effect=lambda message: message), \
      patch.object(validator, "getServicesSiteProperties", return_value=None), \
      patch.object(validator, "getSiteProperties", return_value=None), \
      patch.object(
        validator,
        "toConfigurationValidationProblems",
        side_effect=lambda items, _: items,
      ):
      for properties in cases:
        with self.subTest(policy=properties["yarn.http.policy"]):
          self.assertEqual(
            [],
            validator.validateYarnRuntimeConfigurations(
              properties, {}, {}, existing_services, {}
            ),
          )
      for invalid_address in (
        "",
        "host",
        "host:0",
        "host:65536",
        "user@host:8188",
        "host/path:8188",
        "host?query:8188",
        "2001:db8::1:8188",
        "[fe80::1%eth0]:8188",
      ):
        with self.subTest(invalid_address=invalid_address):
          problems = validator.validateYarnRuntimeConfigurations(
            {
              "yarn.http.policy": "HTTP_ONLY",
              "yarn.timeline-service.webapp.address": invalid_address,
            },
            {},
            {},
            existing_services,
            {},
          )
          self.assertEqual(
            "yarn.timeline-service.webapp.address",
            problems[0]["config-name"],
          )

  def test_log_server_recommendation_uses_proposed_values_and_never_writes_empty(self):
    services = {
      "configurations": {
        "yarn-site": {
          "properties": {
            "yarn.http.policy": "HTTP_ONLY",
            "yarn.timeline-service.webapp.address": "old.example:8188",
            "yarn.log.server.web-service.url": "http://old.invalid",
          }
        }
      }
    }
    proposed = {
      "yarn-site": {
        "properties": {
          "yarn.http.policy": "HTTPS_ONLY",
          "yarn.timeline-service.webapp.https.address": "new.example:8190",
        }
      }
    }
    self.assertEqual(
      "https://new.example:8190/ws/v1/applicationhistory",
      YARN_ADVISOR._recommended_log_server_url(proposed, services),
    )
    for properties in (
      {"yarn.http.policy": "HTTPS_ONLY"},
      {"yarn.http.policy": "HTTP_AND_HTTPS"},
      {
        "yarn.http.policy": "HTTPS_ONLY",
        "yarn.timeline-service.webapp.https.address": " ",
      },
      {
        "yarn.http.policy": "HTTP_ONLY",
        "yarn.timeline-service.webapp.address": "user@host:8188",
      },
      {
        "yarn.http.policy": "HTTP_ONLY",
        "yarn.timeline-service.webapp.address": "2001:db8::1:8188",
      },
    ):
      with self.subTest(properties=properties):
        missing_services = {
          "configurations": {
            "yarn-site": {
              "properties": {
                "yarn.log.server.web-service.url": "http://old.invalid",
                **properties,
              }
            }
          }
        }
        self.assertIsNone(
          YARN_ADVISOR._recommended_log_server_url({}, missing_services)
        )

  def test_proposed_runtime_values_override_existing_gpu_and_cgroup_state(self):
    services = {
      "configurations": {
        "container-executor": {"properties": {"gpu_module_enabled": "true"}},
        "yarn-env": {"properties": {"yarn_cgroups_enabled": "true"}},
        "resource-types": {"properties": {"yarn.resource-types": "yarn.io/gpu"}},
      }
    }
    proposed = {
      "container-executor": {"properties": {"gpu_module_enabled": "false"}},
      "yarn-env": {"properties": {"yarn_cgroups_enabled": "false"}},
      "resource-types": {"properties": {"yarn.resource-types": ""}},
    }
    self.assertEqual(
      "false",
      YARN_ADVISOR._effective_site_properties(
        proposed, services, "container-executor"
      )["gpu_module_enabled"],
    )
    self.assertEqual(
      "false",
      YARN_ADVISOR._effective_site_properties(proposed, services, "yarn-env")[
        "yarn_cgroups_enabled"
      ],
    )
    recommender = YARN_ADVISOR.YARNRecommender()
    self.assertFalse(recommender.has_multiple_resource_types(proposed, services))

  def test_resource_validator_uses_existing_cluster_group_for_partial_update(self):
    validator = object.__new__(YARN_ADVISOR.YARNValidator)
    validator.logger = MagicMock()
    services = {
      "configurations": {
        "cluster-env": {"properties": {"user_group": "analytics"}}
      }
    }
    with patch.object(
        validator,
        "validatorLessThenDefaultValue",
        return_value=None,
      ), \
      patch.object(
        validator,
        "validatorEqualsPropertyItem",
        return_value=None,
      ) as equals, \
      patch.object(
        validator,
        "toConfigurationValidationProblems",
        return_value=[],
      ):
      validator.validateYarnResourceConfigurations(
        {
          "yarn.nodemanager.linux-container-executor.group": "analytics",
        },
        {},
        {"yarn-site": {"properties": {}}},
        services,
        {},
      )
    self.assertEqual("analytics", equals.call_args.args[2]["user_group"])
    self.assertEqual("user_group", equals.call_args.args[3])

  def test_scheduler_gpu_proposal_enables_cgroups_with_proposed_group(self):
    recommender = YARN_ADVISOR.YARNRecommender()
    configurations = {
      "container-executor": {"properties": {"gpu_module_enabled": "true"}},
      "cluster-env": {"properties": {"user_group": "analytics"}},
    }

    services = {
      "configurations": {
        "container-executor": {"properties": {"gpu_module_enabled": "false"}},
        "cluster-env": {"properties": {"user_group": "hadoop"}},
        "yarn-env": {"properties": {"yarn_cgroups_enabled": "false"}},
        "capacity-scheduler": {
          "properties": {
            "yarn.scheduler.capacity.resource-calculator": (
              "org.apache.hadoop.yarn.util.resource.DefaultResourceCalculator"
            )
          }
        },
      },
      "services": [],
    }

    def put_property(_, config_type, __):
      def update(name, value):
        configurations.setdefault(config_type, {}).setdefault("properties", {})[
          name
        ] = value

      return update

    with patch.object(recommender, "putProperty", side_effect=put_property), \
      patch.object(
        recommender, "putPropertyAttribute", return_value=MagicMock()
      ) as attribute_factory, \
      patch.object(
        recommender,
        "getHostWithComponent",
        return_value={
          "Hosts": {"cpu_count": "8", "total_mem": str(32 * 1024 * 1024)}
        },
      ), \
      patch.object(
        YARN_ADVISOR.YARNServiceAdvisor,
        "isKerberosEnabled",
        return_value=False,
      ):
      recommender.recommendBigtopSchedulerConfigurations(
        configurations, {"cpu": 8}, services, {"items": []}
      )

    yarn_properties = configurations["yarn-site"]["properties"]
    self.assertEqual("analytics", yarn_properties[
      "yarn.nodemanager.linux-container-executor.group"
    ])
    self.assertEqual(
      "true", configurations["yarn-env"]["properties"]["yarn_cgroups_enabled"]
    )
    self.assertIn(
      "DominantResourceCalculator",
      configurations["capacity-scheduler"]["properties"][
        "yarn.scheduler.capacity.resource-calculator"
      ],
    )
    attribute_factory.return_value.assert_any_call(
      "yarn.nodemanager.resource.cpu-vcores", "maximum", 16
    )

  def test_cpu_recommendation_numeric_contracts_fail_closed(self):
    self.assertEqual(80.0, YARN_ADVISOR._parse_cpu_percentage("80"))
    self.assertEqual(
      16, YARN_ADVISOR._parse_positive_integer("16", "NodeManager CPU count")
    )
    self.assertIs(
      True, YARN_ADVISOR._parse_boolean(" true ", "GPU module switch")
    )
    self.assertEqual(
      "no", YARN_ADVISOR._parse_yes_no(" No ", "Ranger plugin switch")
    )
    for invalid in (None, "bad", "nan", "inf", "1_0", 0, -1, 101, True):
      with self.subTest(percentage=invalid):
        with self.assertRaisesRegex(ValueError, "1 through 100"):
          YARN_ADVISOR._parse_cpu_percentage(invalid)
    for invalid in (None, "bad", "nan", 0, -1, 1.5, True):
      with self.subTest(cpu_count=invalid):
        with self.assertRaisesRegex(ValueError, "positive integer"):
          YARN_ADVISOR._parse_positive_integer(
            invalid, "NodeManager CPU count"
          )
    for invalid in (None, "", "yes", 1):
      with self.subTest(boolean=invalid):
        with self.assertRaisesRegex(ValueError, "true or false"):
          YARN_ADVISOR._parse_boolean(invalid, "GPU module switch")
    for invalid in (None, "", "true", 1):
      with self.subTest(yes_no=invalid):
        with self.assertRaisesRegex(ValueError, "Yes or No"):
          YARN_ADVISOR._parse_yes_no(invalid, "Ranger plugin switch")

  def test_proposed_node_manager_memory_wins_with_changed_configurations(self):
    recommender = YARN_ADVISOR.YARNRecommender()
    services = {
      "changed-configurations": ["yarn-site/yarn.nodemanager.resource.memory-mb"],
      "configurations": {
        "yarn-site": {
          "properties": {"yarn.nodemanager.resource.memory-mb": "4096"}
        }
      },
    }
    proposed = {
      "yarn-site": {
        "properties": {"yarn.nodemanager.resource.memory-mb": "12288"}
      }
    }
    self.assertEqual(
      12288.0, recommender.get_yarn_nm_mem_in_mb(services, proposed)
    )

  def test_proposed_ats_cache_and_reader_port_override_existing_values(self):
    recommender = YARN_ADVISOR.YARNRecommender()
    services = {
      "configurations": {
        "yarn-site": {
          "properties": {
            "yarn.timeline-service.entity-group-fs-store.app-cache-size": "3",
            "yarn.timeline-service.reader.webapp.address": "old.example:8198",
          }
        }
      }
    }
    configurations = {
      "yarn-site": {
        "properties": {
          "yarn.timeline-service.entity-group-fs-store.app-cache-size": "9",
          "yarn.timeline-service.reader.webapp.address": "proposed.example:9198",
        }
      }
    }
    self.assertEqual(
      9,
      recommender.read_yarn_apptimelineserver_cache_size(
        configurations, services
      ),
    )
    updates = []
    with patch.object(
        recommender,
        "putProperty",
        return_value=lambda name, value: updates.append((name, value)),
      ), \
      patch.object(
        recommender,
        "getHostsForComponent",
        return_value=["reader.example"],
      ):
      recommender.update_timeline_reader_address(
        configurations,
        services,
        "yarn.timeline-service.reader.webapp.address",
      )
    self.assertEqual(
      [("yarn.timeline-service.reader.webapp.address", "reader.example:9198")],
      updates,
    )
    self.assertEqual(
      "[2001:db8::2]:9198",
      YARN_ADVISOR._replace_address_host("[2001:db8::1]:9198", "2001:db8::2"),
    )

  def test_proposed_simple_authentication_disables_existing_kerberos(self):
    services = {
      "configurations": {
        "core-site": {
          "properties": {"hadoop.security.authentication": " Kerberos "}
        }
      }
    }
    proposed = {
      "core-site": {"properties": {"hadoop.security.authentication": " SIMPLE "}}
    }
    for advisor_class in (
      YARN_ADVISOR.YARNRecommender,
      YARN_ADVISOR.MAPREDUCE2Recommender,
    ):
      with self.subTest(advisor=advisor_class.__name__):
        self.assertFalse(
          advisor_class().is_kerberos_enabled(proposed, services)
        )

  def test_yarn_runtime_validator_reports_missing_and_cross_site_values(self):
    validator = object.__new__(YARN_ADVISOR.YARNValidator)
    validator.logger = MagicMock()
    with patch.object(validator, "getErrorItem", side_effect=lambda message: message), \
      patch.object(validator, "getWarnItem", side_effect=lambda message: message), \
      patch.object(validator, "getServicesSiteProperties", return_value=None), \
      patch.object(
        validator,
        "getSiteProperties",
        return_value={"yarn_hierarchy": "/configured"},
      ), \
      patch.object(
        validator,
        "toConfigurationValidationProblems",
        side_effect=lambda items, _: items,
      ):
      missing_policy = validator.validateYarnRuntimeConfigurations(
        {}, {}, {}, {}, {}
      )
      missing_address = validator.validateYarnRuntimeConfigurations(
        {"yarn.http.policy": "HTTPS_ONLY"}, {}, {}, {}, {}
      )
      hierarchy_mismatch = validator.validateYarnRuntimeConfigurations(
        {
          "yarn.http.policy": "HTTP_ONLY",
          "yarn.timeline-service.webapp.address": "ats:8188",
          "yarn.log.server.web-service.url": (
            "http://ats:8188/ws/v1/applicationhistory"
          ),
          "yarn.nodemanager.linux-container-executor.cgroups.hierarchy": "/yarn",
        },
        {},
        {},
        {},
        {},
      )
    self.assertEqual("yarn.http.policy", missing_policy[0]["config-name"])
    self.assertEqual(
      "yarn.timeline-service.webapp.https.address",
      missing_address[0]["config-name"],
    )
    self.assertEqual(
      "yarn.nodemanager.linux-container-executor.cgroups.hierarchy",
      hierarchy_mismatch[0]["config-name"],
    )

  def test_yarn_cgroup_validator_handles_partial_and_typed_configuration(self):
    validator = object.__new__(YARN_ADVISOR.YARNValidator)
    validator.logger = MagicMock()
    with patch.object(validator, "getWarnItem", side_effect=lambda message: message), \
      patch.object(
        validator,
        "toConfigurationValidationProblems",
        side_effect=lambda items, _: items,
      ):
      cases = (
        (True, {}, 1),
        ("true", {"hadoop.security.authentication": "kerberos"}, 1),
        (
          True,
          {
            "hadoop.security.authentication": "KERBEROS",
            "hadoop.security.authorization": True,
          },
          0,
        ),
        (False, {}, 0),
        ([], {}, 1),
      )
      for configured_value, core_site, warning_count in cases:
        with self.subTest(value=configured_value, core_site=core_site):
          problems = validator.validateYarnCgroupConfigurations(
            {"yarn_cgroups_enabled": configured_value},
            {},
            {"core-site": {"properties": core_site}},
            {},
            {},
          )
          self.assertEqual(warning_count, len(problems))

  def test_yarn_cgroup_validator_uses_existing_security_for_partial_proposal(self):
    validator = object.__new__(YARN_ADVISOR.YARNValidator)
    validator.logger = MagicMock()
    services = {
      "configurations": {
        "core-site": {
          "properties": {
            "hadoop.security.authentication": "KERBEROS",
            "hadoop.security.authorization": "true",
          }
        }
      }
    }
    configurations = {
      "yarn-env": {"properties": {"yarn_cgroups_enabled": "true"}}
    }
    with patch.object(
      validator,
      "toConfigurationValidationProblems",
      side_effect=lambda items, _: items,
    ):
      self.assertEqual(
        [],
        validator.validateYarnCgroupConfigurations(
          {"yarn_cgroups_enabled": "true"},
          {},
          configurations,
          services,
          {},
        ),
      )

  def test_ranger_recommendation_and_validation_use_proposed_switch(self):
    recommender = YARN_ADVISOR.YARNRecommender()
    for old_value, proposed_value in (("Yes", "No"), ("No", "Yes")):
      services = {
        "configurations": {
          "ranger-env": {
            "properties": {"ranger-yarn-plugin-enabled": old_value}
          },
          "ranger-yarn-plugin-properties": {
            "properties": {"ranger-yarn-plugin-enabled": old_value}
          },
        }
      }
      configurations = {
        "ranger-env": {
          "properties": {"ranger-yarn-plugin-enabled": proposed_value}
        }
      }
      updates = []

      def put_property(_, config_type, __):
        return lambda name, value: updates.append((config_type, name, value))

      with self.subTest(old=old_value, proposed=proposed_value), \
        patch.object(recommender, "putProperty", side_effect=put_property), \
        patch.object(
          recommender, "putPropertyAttribute", return_value=MagicMock()
        ):
        recommender.recommendBigtopAuthorizationConfigurations(
          configurations, {}, services, {}
        )
      self.assertIn(
        (
          "ranger-yarn-plugin-properties",
          "ranger-yarn-plugin-enabled",
          proposed_value,
        ),
        updates,
      )

      validator = object.__new__(YARN_ADVISOR.YARNValidator)
      validator.logger = MagicMock()
      validator_configurations = {
        **configurations,
        "ranger-yarn-plugin-properties": {
          "properties": {"ranger-yarn-plugin-enabled": proposed_value}
        },
      }
      with patch.object(
        validator,
        "toConfigurationValidationProblems",
        side_effect=lambda items, _: items,
      ):
        self.assertEqual(
          [],
          validator.validateYarnRangerConfigurations(
            {}, {}, validator_configurations, services, {}
          ),
        )

    validator = object.__new__(YARN_ADVISOR.YARNValidator)
    validator.logger = MagicMock()
    invalid = {
      "ranger-env": {
        "properties": {"ranger-yarn-plugin-enabled": "garbage"}
      },
      "ranger-yarn-plugin-properties": {
        "properties": {"ranger-yarn-plugin-enabled": "garbage"}
      },
    }
    with patch.object(validator, "getErrorItem", side_effect=lambda message: message), \
      patch.object(
        validator,
        "toConfigurationValidationProblems",
        side_effect=lambda items, _: items,
      ):
      problems = validator.validateYarnRangerConfigurations(
        {}, {}, invalid, {}, {}
      )
    self.assertEqual(2, len(problems))
    self.assertTrue(all("Yes or No" in problem["item"] for problem in problems))

  def test_ats_memory_recommendations_use_host_memory_in_mb(self):
    recommender = YARN_ADVISOR.YARNRecommender()
    four_gb = recommender.get_host_memory_mb(
      {"items": [{"Hosts": {"total_mem": 4096 * 1024}}]}
    )
    sixteen_gb = recommender.get_host_memory_mb(
      {"items": [{"Hosts": {"total_mem": 16384 * 1024}}]}
    )
    self.assertEqual(4096, four_gb)
    self.assertEqual(7, recommender.calculate_yarn_apptimelineserver_cache_size(four_gb))
    self.assertEqual(
      2048, recommender.calculate_yarn_apptimelineserver_heapsize(four_gb, 7)
    )
    self.assertEqual(16384, sixteen_gb)
    self.assertEqual(
      8072, recommender.calculate_yarn_apptimelineserver_heapsize(sixteen_gb, 10)
    )
    self.assertIsNone(recommender.get_host_memory_mb({"items": []}))
    with self.assertRaises(ValueError):
      recommender.calculate_yarn_apptimelineserver_cache_size(float("nan"))

  def test_invalid_ats_cache_size_is_rejected(self):
    recommender = YARN_ADVISOR.YARNRecommender()
    with self.assertRaisesRegex(ValueError, "must be a positive integer"):
      recommender.read_yarn_apptimelineserver_cache_size(
        {
          "yarn-site": {
            "properties": {
              "yarn.timeline-service.entity-group-fs-store.app-cache-size": "invalid"
            }
          }
        },
        {},
      )

  def test_empty_host_inventory_has_deterministic_preemption_default(self):
    recommender = YARN_ADVISOR.YARNRecommender()
    self.assertEqual("0.1", recommender.calculate_total_preemption_per_round({}))
    self.assertEqual(
      "0.1", recommender.calculate_total_preemption_per_round({"items": []})
    )
    self.assertEqual(
      "0.5",
      recommender.calculate_total_preemption_per_round(
        {"items": [{"Hosts": {}}, {"Hosts": {}}]}
      ),
    )

  def test_mapreduce_recommendations_write_each_runtime_value_once(self):
    recommender = YARN_ADVISOR.MAPREDUCE2Recommender()
    updates = []
    attributes = []
    mounts = []
    configurations = {
      "yarn-site": {
        "properties": {
          "yarn.scheduler.minimum-allocation-mb": "512",
          "yarn.scheduler.maximum-allocation-mb": "8192",
        }
      },
      "mapred-site": {"properties": {}},
    }
    services = {"configurations": {}, "services": []}

    def put_property(_, config_type, __):
      def update(name, value):
        updates.append((config_type, name, value))
        configurations.setdefault(config_type, {}).setdefault("properties", {})[
          name
        ] = value

      return update

    with patch.object(recommender, "putProperty", side_effect=put_property), \
      patch.object(
        recommender,
        "putPropertyAttribute",
        return_value=lambda name, attribute, value: attributes.append(
          (name, attribute, value)
        ),
      ), \
      patch.object(
        recommender,
        "updateMountProperties",
        side_effect=lambda *args: mounts.append(args),
      ), \
      patch.object(recommender, "recommendYarnQueue", return_value="default"), \
      patch.object(recommender, "getServicesSiteProperties", return_value=None):
      recommender.recommendBigtopMapReduceConfigurations(
        configurations,
        {"ramPerContainer": 1024, "totalAvailableRam": 8192},
        services,
        {"items": []},
      )

    runtime_names = [
      name for config_type, name, _ in updates if config_type == "mapred-site"
    ]
    self.assertEqual(len(runtime_names), len(set(runtime_names)))
    self.assertEqual("default", configurations["mapred-site"]["properties"]["mapreduce.job.queuename"])
    self.assertEqual("NODEMANAGER", mounts[0][1][0][1][0])
    self.assertIn(("mapreduce.map.memory.mb", "maximum", 8192), attributes)

  def test_spark_user_is_added_to_capacity_admins_exactly_once(self):
    recommender = YARN_ADVISOR.YARNRecommender()
    updates = []

    def put_property(_, config_type, __):
      return lambda name, value: updates.append((config_type, name, value))

    services = {
      "configurations": {
        "capacity-scheduler": {
          "properties": {
            "yarn.scheduler.capacity.root.acl_administer_queue": "yarn"
          }
        },
        "spark-env": {"properties": {"spark_user": "spark"}},
      },
      "changed-configurations": [],
    }
    with patch.object(recommender, "putProperty", side_effect=put_property), \
      patch.object(recommender, "update_timeline_reader_address"):
      recommender.recommendBigtopServiceIntegrations({}, {}, services, {})

    self.assertEqual(
      [
        (
          "capacity-scheduler",
          "yarn.scheduler.capacity.root.acl_administer_queue",
          "yarn,spark",
        )
      ],
      updates,
    )

  def test_serialized_capacity_scheduler_preserves_prior_phase_updates(self):
    recommender = YARN_ADVISOR.YARNRecommender()
    configurations = {}
    services = {
      "configurations": {
        "capacity-scheduler": {
          "properties": {
            "capacity-scheduler": (
              "yarn.scheduler.capacity.root.acl_administer_queue=yarn\n"
              "yarn.scheduler.capacity.resource-calculator="
              "org.apache.hadoop.yarn.util.resource.DominantResourceCalculator\n"
            )
          }
        },
        "hive-env": {"properties": {"hive_user": "hive"}},
        "spark-env": {"properties": {"spark_user": "spark"}},
      },
      "changed-configurations": [],
    }

    def put_property(_, config_type, __):
      def update(name, value):
        configurations.setdefault(config_type, {}).setdefault("properties", {})[
          name
        ] = value

      return update

    with patch.object(recommender, "putProperty", side_effect=put_property), \
      patch.object(recommender, "update_timeline_reader_address"):
      recommender.recommendBigtopServiceIntegrations(
        configurations, {}, services, {}
      )

    effective, received_as_pairs = (
      YARN_ADVISOR._effective_capacity_scheduler_properties(
        configurations, services
      )
    )
    self.assertFalse(received_as_pairs)
    self.assertEqual(
      "yarn,hive,spark",
      effective["yarn.scheduler.capacity.root.acl_administer_queue"],
    )

  def test_external_hbase_disables_embedded_system_service(self):
    recommender = YARN_ADVISOR.YARNRecommender()
    updates = []
    services = {
      "configurations": {
        "yarn-hbase-env": {
          "properties": {
            "is_hbase_system_service_launch": "true",
            "use_external_hbase": "false",
            "hbase_within_cluster": "false",
          }
        }
      }
    }

    def put_property(_, config_type, __):
      return lambda name, value: updates.append((config_type, name, value))

    with patch.object(recommender, "getServicesSiteProperties", return_value=None), \
      patch.object(
        recommender, "getCapacitySchedulerProperties", return_value=({}, True)
      ), \
      patch.object(recommender, "putProperty", side_effect=put_property), \
      patch.object(recommender, "update_timeline_reader_address"), \
      patch.object(recommender, "getHostsForComponent") as get_hosts:
      recommender.recommendBigtopServiceIntegrations(
        {
          "yarn-hbase-env": {
            "properties": {"use_external_hbase": "true"}
          }
        },
        {},
        services,
        {},
      )
      with self.assertRaisesRegex(ValueError, "must be true or false"):
        recommender.recommendBigtopServiceIntegrations(
          {
            "yarn-hbase-env": {
              "properties": {"use_external_hbase": "yes"}
            }
          },
          {},
          services,
          {},
        )

    self.assertEqual(
      [("yarn-hbase-env", "is_hbase_system_service_launch", "false")], updates
    )
    get_hosts.assert_not_called()

  def test_system_service_recommendation_requires_timeline_v2_placement(self):
    recommender = YARN_ADVISOR.YARNRecommender()
    updates = []
    services = {
      "configurations": {
        "yarn-hbase-env": {
          "properties": {
            "is_hbase_system_service_launch": "true",
            "use_external_hbase": "false",
            "hbase_within_cluster": "false",
          }
        },
        "yarn-site": {
          "properties": {
            "yarn.timeline-service.enabled": "true",
            "yarn.timeline-service.versions": "2.0f",
          }
        },
      }
    }

    def put_property(_, config_type, __):
      return lambda name, value: updates.append((config_type, name, value))

    with patch.object(recommender, "getServicesSiteProperties", return_value=None), \
      patch.object(
        recommender, "getCapacitySchedulerProperties", return_value=({}, True)
      ), \
      patch.object(recommender, "putProperty", side_effect=put_property), \
      patch.object(recommender, "update_timeline_reader_address"), \
      patch.object(recommender, "getHostsForComponent", return_value=[]):
      recommender.recommendBigtopServiceIntegrations({}, {}, services, {})

    self.assertIn(
      ("yarn-hbase-env", "is_hbase_system_service_launch", "false"),
      updates,
    )

  def test_service_acl_integration_rejects_untrusted_user_names_in_both_formats(self):
    recommender = YARN_ADVISOR.YARNRecommender()
    cases = (
      (
        "hive-env",
        "hive_user",
        "hive,attacker",
        {
          "yarn.scheduler.capacity.root.acl_administer_queue": "yarn"
        },
      ),
      (
        "spark-env",
        "spark_user",
        "spark\nnext.property=admin",
        {
          "capacity-scheduler": (
            "yarn.scheduler.capacity.root.acl_administer_queue=yarn\n"
          )
        },
      ),
    )
    for site_name, property_name, value, capacity_properties in cases:
      services = {
        "configurations": {
          site_name: {"properties": {property_name: value}},
          "capacity-scheduler": {"properties": capacity_properties},
        }
      }
      with self.subTest(site=site_name), \
        patch.object(recommender, "putProperty", return_value=MagicMock()), \
        patch.object(recommender, "update_timeline_reader_address"):
        with self.assertRaisesRegex(ValueError, "valid Unix user name"):
          recommender.recommendBigtopServiceIntegrations(
            {}, {}, services, {}
          )


class TestYarnAlertContracts(unittest.TestCase):
  def test_alert_boolean_configuration_fails_closed(self):
    for alert in (NODEMANAGER_HEALTH_ALERT, NODEMANAGERS_SUMMARY_ALERT):
      for value in ("yes", 1, ""):
        with self.subTest(alert=alert.__name__, value=value):
          result, labels = alert.execute(
            {alert.SECURITY_ENABLED_KEY: value}, {}, "host.example"
          )
          self.assertEqual("UNKNOWN", result)
          self.assertIn("must be true or false", labels[0])

    alert = ATS_HBASE_ALERT
    base = {
      alert.USE_EXTERNAL_HBASE_KEY: "false",
      alert.ATS_HBASE_SYSTEM_SERVICE_LAUNCH_KEY: "true",
      alert.SECURITY_ENABLED_KEY: "false",
      alert.STACK_ROOT: "/usr/bigtop",
    }
    for key in (
      alert.USE_EXTERNAL_HBASE_KEY,
      alert.ATS_HBASE_SYSTEM_SERVICE_LAUNCH_KEY,
      alert.SECURITY_ENABLED_KEY,
    ):
      for value in ("yes", 1, ""):
        configurations = dict(base)
        configurations[key] = value
        with self.subTest(key=key, value=value):
          result, labels = alert.execute(configurations, {}, "host.example")
          self.assertEqual("CRITICAL", result)
          self.assertIn("must be true or false", labels[0])

  def test_nodemanager_alerts_fail_closed_for_policy_and_address_mismatch(self):
    for alert in (NODEMANAGER_HEALTH_ALERT, NODEMANAGERS_SUMMARY_ALERT):
      with self.subTest(alert=alert.__name__, case="invalid policy"):
        result, _ = alert.execute(
          {
            alert.YARN_HTTP_POLICY_KEY: "HTTP_AND_HTTPS",
            alert.NODEMANAGER_HTTP_ADDRESS_KEY: "rm.example:8088",
          },
          {},
          "host.example",
        )
        self.assertEqual("UNKNOWN", result)
      with self.subTest(alert=alert.__name__, case="missing HTTPS address"):
        result, _ = alert.execute(
          {
            alert.YARN_HTTP_POLICY_KEY: "HTTPS_ONLY",
            alert.NODEMANAGER_HTTP_ADDRESS_KEY: "rm.example:8088",
          },
          {},
          "host.example",
        )
        self.assertEqual("UNKNOWN", result)
      for address in (
        "host:",
        "host:0",
        "host:65536",
        "host:+53",
        "host:1_000",
        "host: 8042",
        "2001:db8::1:8042",
      ):
        with self.subTest(alert=alert.__name__, address=address):
          result, _ = alert.execute(
            {
              alert.YARN_HTTP_POLICY_KEY: "HTTP_ONLY",
              alert.NODEMANAGER_HTTP_ADDRESS_KEY: address,
            },
            {},
            "host.example",
          )
          self.assertEqual("UNKNOWN", result)

  def test_nodemanager_health_uses_the_address_for_the_effective_policy(self):
    alert = NODEMANAGER_HEALTH_ALERT
    for policy, address_key, address, scheme in (
      ("HTTP_ONLY", alert.NODEMANAGER_HTTP_ADDRESS_KEY, "nm:8042", "http"),
      ("HTTPS_ONLY", alert.NODEMANAGER_HTTPS_ADDRESS_KEY, "nm:8044", "https"),
    ):
      response = MagicMock()
      response.read.return_value = (
        b'{"nodeInfo":{"nodeHealthy":true,"healthReport":""}}'
      )
      with self.subTest(policy=policy), \
        patch.object(alert.urllib.request, "urlopen", return_value=response) as open_url:
        result, _ = alert.execute(
          {alert.YARN_HTTP_POLICY_KEY: policy, address_key: address},
          {},
          "nm.example",
        )
        self.assertEqual("OK", result)
        self.assertTrue(open_url.call_args.args[0].startswith(f"{scheme}://"))

  def test_nodemanager_summary_uses_the_address_for_the_effective_policy(self):
    alert = NODEMANAGERS_SUMMARY_ALERT
    for policy, address_key, address, scheme in (
      ("HTTP_ONLY", alert.NODEMANAGER_HTTP_ADDRESS_KEY, "rm:8088", "http"),
      ("HTTPS_ONLY", alert.NODEMANAGER_HTTPS_ADDRESS_KEY, "rm:8090", "https"),
    ):
      with self.subTest(policy=policy), \
        patch.object(alert, "get_value_from_jmx", return_value="[]") as get_jmx:
        result, _ = alert.execute(
          {alert.YARN_HTTP_POLICY_KEY: policy, address_key: address},
          {},
          "rm.example",
        )
        self.assertEqual("OK", result)
        self.assertTrue(get_jmx.call_args.args[0].startswith(f"{scheme}://"))

  def test_nodemanager_alerts_reject_non_finite_or_invalid_timeouts(self):
    for alert in (NODEMANAGER_HEALTH_ALERT, NODEMANAGERS_SUMMARY_ALERT):
      for timeout in ("invalid", "nan", "inf", 0, -1):
        with self.subTest(alert=alert.__name__, timeout=timeout):
          result, _ = alert.execute({}, {alert.CONNECTION_TIMEOUT_KEY: timeout}, "host")
          self.assertEqual("CRITICAL", result)

  def test_secure_nodemanager_health_uses_smoke_credentials_and_timeout(self):
    alert = NODEMANAGER_HEALTH_ALERT
    configurations = {
      alert.SECURITY_ENABLED_KEY: "true",
      alert.KERBEROS_KEYTAB: "/etc/security/keytabs/smokeuser.headless.keytab",
      alert.KERBEROS_PRINCIPAL: "ambari-qa-cluster@REALM",
      alert.SMOKEUSER_KEY: "ambari-qa",
      alert.NODEMANAGER_HTTP_ADDRESS_KEY: "0.0.0.0:8042",
    }
    with patch.object(
        alert.Environment,
        "get_instance",
        return_value=SimpleNamespace(tmp_dir="/tmp"),
      ), \
      patch.object(
        alert,
        "curl_krb_request",
        return_value=('{"nodeInfo":{"nodeHealthy":true,"healthReport":""}}', None, 0.1),
      ) as curl:
      result, _ = alert.execute(
        configurations, {alert.CONNECTION_TIMEOUT_KEY: 0.1}, "nm.example"
      )
    self.assertEqual("OK", result)
    self.assertEqual(0.1, curl.call_args.kwargs["connection_timeout"])
    self.assertEqual(
      "/etc/security/keytabs/smokeuser.headless.keytab", curl.call_args.args[1]
    )
    self.assertEqual("ambari-qa-cluster@REALM", curl.call_args.args[2])
    self.assertEqual("ambari-qa", curl.call_args.args[8])
    self.assertNotIn(
      "{{yarn-site/yarn.nodemanager.webapp.spnego-keytab-file}}",
      alert.get_tokens(),
    )

  def test_secure_nodemanager_summary_uses_cluster_smoke_credentials(self):
    alert = NODEMANAGERS_SUMMARY_ALERT
    configurations = {
      alert.SECURITY_ENABLED_KEY: "true",
      alert.KERBEROS_KEYTAB: "/etc/security/keytabs/smokeuser.headless.keytab",
      alert.KERBEROS_PRINCIPAL: "ambari-qa-cluster@REALM",
      alert.SMOKEUSER_KEY: "ambari-qa",
      alert.NODEMANAGER_HTTP_ADDRESS_KEY: "rm.example:8088",
    }
    with patch.object(
        alert.Environment,
        "get_instance",
        return_value=SimpleNamespace(tmp_dir="/tmp"),
      ), \
      patch.object(
        alert,
        "curl_krb_request",
        return_value=(
          '{"beans":[{"LiveNodeManagers":"[]"}]}',
          None,
          0.1,
        ),
      ) as curl:
      result, _ = alert.execute(configurations, {}, "standalone-rm.example")

    self.assertEqual("OK", result)
    self.assertEqual(
      "/etc/security/keytabs/smokeuser.headless.keytab", curl.call_args.args[1]
    )
    self.assertEqual("ambari-qa-cluster@REALM", curl.call_args.args[2])
    self.assertNotIn(
      "{{yarn-site/yarn.nodemanager.webapp.spnego-keytab-file}}",
      alert.get_tokens(),
    )

  def test_secure_nodemanager_summary_fails_closed_without_smoke_credentials(self):
    alert = NODEMANAGERS_SUMMARY_ALERT
    base = {
      alert.SECURITY_ENABLED_KEY: "true",
      alert.KERBEROS_KEYTAB: "/etc/security/keytabs/smokeuser.headless.keytab",
      alert.KERBEROS_PRINCIPAL: "ambari-qa-cluster@REALM",
      alert.SMOKEUSER_KEY: "ambari-qa",
      alert.NODEMANAGER_HTTP_ADDRESS_KEY: "rm.example:8088",
    }
    for missing in (
      alert.KERBEROS_KEYTAB,
      alert.KERBEROS_PRINCIPAL,
      alert.SMOKEUSER_KEY,
    ):
      configurations = dict(base)
      configurations.pop(missing)
      with self.subTest(missing=missing):
        result, _ = alert.execute(configurations, {}, "standalone-rm.example")
        self.assertEqual("CRITICAL", result)

  def test_nodemanager_invalid_json_always_closes_http_response(self):
    alert = NODEMANAGER_HEALTH_ALERT
    response = MagicMock()
    response.read.return_value = b"not json"
    with patch.object(alert.urllib.request, "urlopen", return_value=response):
      result, _ = alert.execute(
        {alert.NODEMANAGER_HTTP_ADDRESS_KEY: "0.0.0.0:8042"},
        {},
        "nm.example",
      )
    self.assertEqual("CRITICAL", result)
    response.close.assert_called_once_with()

  def test_secure_ats_hbase_uses_private_cache_and_validates_response(self):
    alert = ATS_HBASE_ALERT
    configurations = {
      alert.USE_EXTERNAL_HBASE_KEY: "false",
      alert.ATS_HBASE_SYSTEM_SERVICE_LAUNCH_KEY: "true",
      alert.SECURITY_ENABLED_KEY: "true",
      alert.ATS_HBASE_USER_KEY: "yarn-ats",
      alert.ATS_PRINCIPAL_KEY: "yarn-ats-headless@REALM",
      alert.ATS_KEYTAB_KEY: "/etc/security/keytabs/yarn-ats.headless.keytab",
      alert.STACK_ROOT: json.dumps({"BIGTOP": "/custom/bigtop"}),
    }
    cache = MagicMock()
    cache.environment = {"KRB5CCNAME": "FILE:/tmp/private/krb5cc"}
    cache_context = MagicMock()
    cache_context.__enter__.return_value = cache
    with patch.object(alert, "get_kinit_path", return_value="/usr/bin/kinit"), \
      patch.object(
        alert,
        "resolve_yarn_executable",
        return_value="/custom/bigtop/current/hadoop-yarn-client/bin/yarn",
      ), \
      patch.object(alert, "PrivateKerberosCache", return_value=cache_context), \
      patch.object(
        alert,
        "get_ats_hbase_status",
        return_value='noise {"state":"STABLE"}',
      ) as status:
      result, _ = alert.execute(configurations, {}, "rm.example")
    self.assertEqual("OK", result)
    cache.kinit.assert_called_once_with(
      "/usr/bin/kinit",
      "/etc/security/keytabs/yarn-ats.headless.keytab",
      "yarn-ats-headless@REALM",
      timeout=10,
    )
    self.assertIn("{{yarn-env/yarn_ats_principal_name}}", alert.get_tokens())
    self.assertIn("{{yarn-env/yarn_ats_user_keytab}}", alert.get_tokens())
    self.assertEqual(cache.environment, status.call_args.args[3])
    self.assertEqual(
      "/custom/bigtop/current/hadoop-yarn-client/bin/yarn",
      status.call_args.args[0],
    )

  def test_ats_hbase_short_circuits_external_and_fails_missing_credentials(self):
    alert = ATS_HBASE_ALERT
    self.assertEqual(
      "OK",
      alert.execute({alert.USE_EXTERNAL_HBASE_KEY: "true"}, {}, "host")[0],
    )
    configurations = {
      alert.USE_EXTERNAL_HBASE_KEY: "false",
      alert.ATS_HBASE_SYSTEM_SERVICE_LAUNCH_KEY: "true",
      alert.SECURITY_ENABLED_KEY: "true",
    }
    result, labels = alert.execute(configurations, {}, "host")
    self.assertEqual("CRITICAL", result)
    self.assertIn("principal and keytab are required", labels[0])

  def test_ats_hbase_rejects_invalid_timeout_and_json(self):
    alert = ATS_HBASE_ALERT
    configurations = {
      alert.USE_EXTERNAL_HBASE_KEY: "false",
      alert.ATS_HBASE_SYSTEM_SERVICE_LAUNCH_KEY: "true",
      alert.SECURITY_ENABLED_KEY: "false",
      alert.STACK_ROOT: "/usr/bigtop",
    }
    for timeout in ("nan", "inf", 0, -1):
      with self.subTest(timeout=timeout):
        result, _ = alert.execute(
          configurations, {alert.CHECK_COMMAND_TIMEOUT_KEY: timeout}, "host"
        )
        self.assertEqual("CRITICAL", result)
    with patch.object(
        alert,
        "resolve_yarn_executable",
        return_value="/usr/bigtop/current/hadoop-yarn-client/bin/yarn",
      ), \
      patch.object(alert, "get_ats_hbase_status", return_value="not json"):
      result, labels = alert.execute(configurations, {}, "host")
    self.assertEqual("CRITICAL", result)
    self.assertIn("Could not find a JSON object", labels[0])

  def test_ats_hbase_stack_root_parser_rejects_unsafe_or_wrong_stack(self):
    alert = ATS_HBASE_ALERT
    self.assertEqual("/usr/bigtop", alert.resolve_stack_root("/usr/bigtop"))
    self.assertEqual(
      "/custom/bigtop",
      alert.resolve_stack_root(json.dumps({"BIGTOP": "/custom/bigtop"})),
    )
    for stack_root in (
      None,
      "",
      "/",
      "relative",
      "/usr/bigtop/",
      "/usr/bigtop/../bigtop",
      json.dumps({"HDP": "/usr/hdp"}),
    ):
      with self.subTest(stack_root=stack_root):
        with self.assertRaises(Fail):
          alert.resolve_stack_root(stack_root)

  def test_ats_hbase_command_requires_a_trusted_executable_chain(self):
    alert = ATS_HBASE_ALERT
    executable = "/usr/bigtop/current/hadoop-yarn-client/bin/yarn"

    def metadata(path, unsafe_path=None, unsafe_mode=None, unsafe_uid=None):
      is_final = path == executable
      mode = stat.S_IFREG | 0o755 if is_final else stat.S_IFDIR | 0o755
      uid = 0
      if path == unsafe_path:
        mode = unsafe_mode if unsafe_mode is not None else mode
        uid = unsafe_uid if unsafe_uid is not None else uid
      return SimpleNamespace(st_mode=mode, st_uid=uid)

    with patch.object(alert.os, "lstat", side_effect=metadata), \
      patch.object(alert.os.path, "realpath", return_value=executable):
      self.assertEqual(executable, alert.resolve_yarn_executable("/usr/bigtop"))

    unsafe_cases = (
      ("/usr/bigtop", stat.S_IFDIR | 0o777, 0),
      ("/usr/bigtop/current", stat.S_IFLNK | 0o777, 1001),
      (executable, stat.S_IFDIR | 0o755, 0),
      (executable, stat.S_IFREG | 0o777, 0),
      (executable, stat.S_IFREG | 0o755, 1001),
      (executable, stat.S_IFREG | 0o644, 0),
    )
    for unsafe_path, unsafe_mode, unsafe_uid in unsafe_cases:
      def unsafe_metadata(path):
        return metadata(path, unsafe_path, unsafe_mode, unsafe_uid)

      with self.subTest(path=unsafe_path, mode=unsafe_mode, uid=unsafe_uid), \
        patch.object(alert.os, "lstat", side_effect=unsafe_metadata), \
        patch.object(alert.os.path, "realpath", return_value=executable):
        with self.assertRaises(Fail):
          alert.resolve_yarn_executable("/usr/bigtop")

    def replaced_metadata(path):
      if path == "/tmp":
        return SimpleNamespace(st_mode=stat.S_IFDIR | 0o1777, st_uid=0)
      if path == "/tmp/replaced/yarn":
        return SimpleNamespace(st_mode=stat.S_IFREG | 0o755, st_uid=0)
      return metadata(path)

    with patch.object(alert.os, "lstat", side_effect=replaced_metadata), \
      patch.object(
        alert.os.path, "realpath", return_value="/tmp/replaced/yarn"
      ):
      with self.assertRaisesRegex(Fail, "root-owned and non-writable"):
        alert.resolve_yarn_executable("/usr/bigtop")


class TestYarnServiceCheck(unittest.TestCase):
  def _params(self, security_enabled=False):
    return params_module(
      HdfsResource=MagicMock(),
      smokeuser="ambari-qa",
      user_group="hadoop",
      smoke_hdfs_user_mode=0o770,
      hadoop_yarn_home="/usr/lib/hadoop-yarn",
      security_enabled=security_enabled,
      kinit_path_local="/usr/bin/kinit",
      smoke_user_keytab="/etc/security/keytabs/smoke user.keytab",
      smokeuser_principal="ambari-qa/host@REALM;$(id)",
      yarn_container_bin="/usr/lib/hadoop-yarn/bin",
      hadoop_conf_dir="/etc/hadoop/conf",
      number_of_nm=2,
      service_check_queue_name="default;$(id)",
      execute_path="/usr/bin:/bin",
    )

  def test_unsafe_smoke_users_fail_before_hdfs_path_construction(self):
    for smokeuser in ("", "../hdfs", "user/name", " user", "user\nname"):
      params = self._params()
      params.smokeuser = smokeuser
      with self.subTest(smokeuser=smokeuser), \
        patch.dict(sys.modules, {"params": params}), \
        patch.object(YARN_SERVICE_CHECK, "Execute") as execute:
        with self.assertRaisesRegex(Fail, "single safe path segment"):
          YARN_SERVICE_CHECK.ServiceCheckDefault().service_check(MagicMock())
        params.HdfsResource.assert_not_called()
        execute.assert_not_called()

  def test_missing_distributed_shell_jar_fails_before_execution(self):
    params = self._params()
    with patch.dict(sys.modules, {"params": params}), \
      patch.object(YARN_SERVICE_CHECK.os.path, "isfile", return_value=False), \
      patch.object(YARN_SERVICE_CHECK, "Execute") as execute:
      with self.assertRaisesRegex(Fail, "distributed shell jar is missing"):
        YARN_SERVICE_CHECK.ServiceCheckDefault().service_check(MagicMock())
    execute.assert_not_called()

  def test_distributed_shell_uses_structured_argv_and_bounded_timeout(self):
    params = self._params()
    with patch.dict(sys.modules, {"params": params}), \
      patch.object(YARN_SERVICE_CHECK.os.path, "isfile", return_value=True), \
      patch.object(YARN_SERVICE_CHECK, "Execute") as execute:
      YARN_SERVICE_CHECK.ServiceCheckDefault().service_check(MagicMock())

    command = execute.call_args.args[0]
    self.assertIsInstance(command, tuple)
    self.assertEqual("/usr/lib/hadoop-yarn/bin/yarn", command[0])
    self.assertIn("hadoop-yarn-applications-distributedshell.jar", command[-5])
    self.assertEqual("default;$(id)", command[-1])
    self.assertEqual(330, execute.call_args.kwargs["timeout"])

  def test_failed_structured_kinit_prevents_service_command(self):
    params = self._params(security_enabled=True)
    cache = MagicMock()
    cache_context = MagicMock()
    cache_context.__enter__.return_value = cache
    cache.kinit.side_effect = Fail("kinit failed")
    with patch.dict(sys.modules, {"params": params}), \
      patch.object(YARN_SERVICE_CHECK.os.path, "isfile", return_value=True), \
      patch.object(
        YARN_SERVICE_CHECK,
        "PrivateKerberosCache",
        return_value=cache_context,
      ), \
      patch.object(YARN_SERVICE_CHECK, "Execute") as execute:
      with self.assertRaisesRegex(Fail, "kinit failed"):
        YARN_SERVICE_CHECK.ServiceCheckDefault().service_check(MagicMock())

    execute.assert_not_called()
    cache.kinit.assert_called_once_with(
      "/usr/bin/kinit",
      "/etc/security/keytabs/smoke user.keytab",
      "ambari-qa/host@REALM;$(id)",
    )


class TestMapReduceServiceCheck(unittest.TestCase):
  def _params(self):
    return params_module(
      HdfsResource=MagicMock(),
      hadoop_mapred_home="/usr/lib/hadoop-mapreduce",
      tmp_dir="/var/lib/ambari-agent/tmp",
      smokeuser="ambari-qa",
      user_group="hadoop",
      smoke_hdfs_user_mode=0o770,
      dfs_type="HDFS",
      security_enabled=False,
      kinit_path_local="/usr/bin/kinit",
      smoke_user_keytab="/etc/security/keytabs/smoke.keytab",
      smokeuser_principal="ambari-qa@REALM",
      hadoop_bin_dir="/usr/bin",
      hadoop_conf_dir="/etc/hadoop/conf",
    )

  def test_unsafe_smoke_users_fail_before_cleanup_path_construction(self):
    for smokeuser in ("", "../hdfs", "user/name", " user", "user\x00name"):
      params = self._params()
      params.smokeuser = smokeuser
      with self.subTest(smokeuser=smokeuser), \
        patch.dict(sys.modules, {"params": params}), \
        patch.object(MAPRED_SERVICE_CHECK, "File") as file_resource:
        with self.assertRaisesRegex(Fail, "single safe path segment"):
          MAPRED_SERVICE_CHECK.MapReduce2ServiceCheckDefault().service_check(
            MagicMock()
          )
        params.HdfsResource.assert_not_called()
        file_resource.assert_not_called()

  def test_missing_examples_jar_fails_before_temp_file_creation(self):
    params = self._params()
    with patch.dict(sys.modules, {"params": params}), \
      patch.object(MAPRED_SERVICE_CHECK.os.path, "isfile", return_value=False), \
      patch.object(MAPRED_SERVICE_CHECK, "File") as file_resource:
      with self.assertRaisesRegex(Fail, "examples jar is missing"):
        MAPRED_SERVICE_CHECK.MapReduce2ServiceCheckDefault().service_check(
          MagicMock()
        )
    file_resource.assert_not_called()

  def test_concurrent_checks_use_unique_private_paths_and_structured_commands(self):
    params = self._params()
    run_ids = (SimpleNamespace(hex="first"), SimpleNamespace(hex="second"))
    local_files = []
    hadoop_calls = []
    with patch.dict(sys.modules, {"params": params}), \
      patch.object(MAPRED_SERVICE_CHECK.os.path, "isfile", return_value=True), \
      patch.object(
        MAPRED_SERVICE_CHECK.uuid, "uuid4", side_effect=run_ids
      ), \
      patch.object(
        MAPRED_SERVICE_CHECK,
        "File",
        side_effect=lambda path, **kwargs: local_files.append((path, kwargs)),
      ), \
      patch.object(
        MAPRED_SERVICE_CHECK,
        "Execute",
        side_effect=lambda command, **kwargs: hadoop_calls.append(
          (command, kwargs)
        ),
      ):
      checker = MAPRED_SERVICE_CHECK.MapReduce2ServiceCheckDefault()
      checker.service_check(MagicMock())
      checker.service_check(MagicMock())

    created_files = [path for path, options in local_files if options.get("content")]
    self.assertEqual(2, len(set(created_files)))
    self.assertTrue(
      all(
        options["replace"] is False
        for _, options in local_files
        if options.get("content")
      )
    )
    jar_commands = [
      (command, options)
      for command, options in hadoop_calls
      if "jar" in command
    ]
    self.assertEqual(2, len(jar_commands))
    self.assertTrue(all(isinstance(command, tuple) for command, _ in jar_commands))
    self.assertIn("ambari-mapreduce-smoke-first", jar_commands[0][0][-2])
    self.assertIn("ambari-mapreduce-smoke-second", jar_commands[1][0][-2])
    self.assertTrue(all(options["timeout"] == 330 for _, options in jar_commands))
    self.assertTrue(
      all(
        options["timeout_kill_strategy"]
        == MAPRED_SERVICE_CHECK.TerminateStrategy.KILL_PROCESS_GROUP
        for _, options in hadoop_calls
      )
    )
    result_checks = [
      options for command, options in hadoop_calls if "-test" in command
    ]
    self.assertTrue(all(options["timeout"] == 60 for options in result_checks))

  def test_primary_failure_and_cleanup_failure_are_both_reported(self):
    params = self._params()
    params.HdfsResource.side_effect = (
      None,
      None,
      None,
      None,
      Fail("HDFS cleanup failed"),
    )
    with patch.dict(sys.modules, {"params": params}), \
      patch.object(MAPRED_SERVICE_CHECK.os.path, "isfile", return_value=True), \
      patch.object(MAPRED_SERVICE_CHECK, "File"), \
      patch.object(
        MAPRED_SERVICE_CHECK,
        "Execute",
        side_effect=Fail("wordcount failed"),
      ):
      with self.assertRaisesRegex(
        RuntimeError, "wordcount failed.*HDFS cleanup failed"
      ) as raised:
        MAPRED_SERVICE_CHECK.MapReduce2ServiceCheckDefault().service_check(
          MagicMock()
        )
    self.assertIsInstance(raised.exception.__cause__, Fail)


class TestAtsHBasePackage(unittest.TestCase):
  def _params(self, version="3.3.6-1"):
    return params_module(
      version=version,
      yarn_hbase_user_tmp=(
        f"/var/lib/ambari-agent/yarn-ats-hbase/{version}"
        if isinstance(version, str)
        else "/var/lib/ambari-agent/yarn-ats-hbase/missing"
      ),
      yarn_hbase_user="yarn-ats",
      user_group="hadoop",
      stack_root="/usr/bigtop",
    )

  def test_invalid_or_missing_stack_version_fails_before_path_access(self):
    for version in (None, "", "../current", "3.3.6;$(id)"):
      with self.subTest(version=version):
        with patch.dict(sys.modules, {"params": self._params(version)}), \
          patch.object(HBASE_SERVICE.sudo, "path_islink") as path_islink:
          with self.assertRaisesRegex(Fail, "valid stack version"):
            HBASE_SERVICE.create_hbase_package()
        path_islink.assert_not_called()

  def test_symlink_package_directory_is_rejected(self):
    with patch.dict(sys.modules, {"params": self._params()}), \
      patch.object(HBASE_SERVICE.sudo, "path_lexists", return_value=True), \
      patch.object(HBASE_SERVICE.sudo, "path_islink", return_value=True), \
      patch.object(
        HBASE_SERVICE.sudo,
        "lstat",
        return_value=SimpleNamespace(st_mode=stat.S_IFDIR | 0o755, st_uid=0),
      ), \
      patch.object(HBASE_SERVICE, "Directory") as directory:
      with self.assertRaisesRegex(Fail, "symbolic link"):
        HBASE_SERVICE.create_hbase_package()
    directory.assert_not_called()

  def test_archive_directory_rejects_untrusted_existing_parent_chain(self):
    archive_dir = "/var/lib/ambari-agent/yarn-ats-hbase/3.3.6-1"
    for unsafe_path, mode, uid in (
      ("/var/lib/ambari-agent", stat.S_IFDIR | 0o775, 0),
      ("/var/lib/ambari-agent/yarn-ats-hbase", stat.S_IFDIR | 0o755, 1001),
      (archive_dir, stat.S_IFREG | 0o644, 0),
    ):
      def metadata(path):
        if path == unsafe_path:
          return SimpleNamespace(st_mode=mode, st_uid=uid)
        return SimpleNamespace(st_mode=stat.S_IFDIR | 0o755, st_uid=0)

      with self.subTest(path=unsafe_path, mode=mode, uid=uid), \
        patch.object(HBASE_SERVICE.sudo, "path_lexists", return_value=True), \
        patch.object(HBASE_SERVICE.sudo, "path_islink", return_value=False), \
        patch.object(HBASE_SERVICE.sudo, "lstat", side_effect=metadata):
        with self.assertRaises(Fail):
          HBASE_SERVICE._validate_archive_directory(archive_dir, "3.3.6-1")

  def test_package_source_snapshot_rejects_untrusted_entries_and_replacement(self):
    version_lib = "/usr/bigtop/3.3.6-1/usr/lib"
    source_root = f"{version_lib}/hbase"
    package_jar = f"{source_root}/lib/hbase.jar"

    def metadata(path, unsafe_path=None, unsafe_mode=None, unsafe_uid=None, ino=1):
      is_file = path == package_jar
      mode = stat.S_IFREG | 0o444 if is_file else stat.S_IFDIR | 0o755
      uid = 0
      if path == unsafe_path:
        mode = unsafe_mode if unsafe_mode is not None else mode
        uid = unsafe_uid if unsafe_uid is not None else uid
      return SimpleNamespace(
        st_mode=mode,
        st_uid=uid,
        st_gid=0,
        st_dev=1,
        st_ino=ino,
        st_size=10,
        st_mtime_ns=20,
      )

    walk_result = [
      (source_root, ["lib"], []),
      (f"{source_root}/lib", [], ["hbase.jar"]),
    ]
    with patch.object(HBASE_SERVICE.os, "walk", return_value=walk_result), \
      patch.object(HBASE_SERVICE.sudo, "lstat", side_effect=metadata):
      snapshot = HBASE_SERVICE._snapshot_package_sources(
        (source_root,), version_lib
      )
    self.assertIn(package_jar, snapshot)

    unsafe_cases = (
      ("/usr/bigtop", stat.S_IFDIR | 0o777, 0),
      (source_root, stat.S_IFLNK | 0o777, 0),
      (package_jar, stat.S_IFREG | 0o666, 0),
      (package_jar, stat.S_IFREG | 0o444, 1001),
    )
    for unsafe_path, unsafe_mode, unsafe_uid in unsafe_cases:
      def unsafe_metadata(path):
        return metadata(path, unsafe_path, unsafe_mode, unsafe_uid)

      with self.subTest(path=unsafe_path, mode=unsafe_mode, uid=unsafe_uid), \
        patch.object(HBASE_SERVICE.os, "walk", return_value=walk_result), \
        patch.object(HBASE_SERVICE.sudo, "lstat", side_effect=unsafe_metadata):
        with self.assertRaises(Fail):
          HBASE_SERVICE._snapshot_package_sources((source_root,), version_lib)

    def replacement_metadata(path):
      return metadata(path, ino=2 if path == package_jar else 1)

    with patch.object(HBASE_SERVICE.os, "walk", return_value=walk_result), \
      patch.object(
        HBASE_SERVICE.sudo, "lstat", side_effect=replacement_metadata
      ):
      with self.assertRaisesRegex(Fail, "changed while creating"):
        HBASE_SERVICE._require_same_package_sources(
          snapshot, (source_root,), version_lib
        )

  def test_staged_runtime_symlink_is_unlinked_without_directory_delete(self):
    with patch.object(HBASE_SERVICE.sudo, "path_lexists", return_value=True), \
      patch.object(HBASE_SERVICE.sudo, "path_islink", return_value=True), \
      patch.object(HBASE_SERVICE.sudo, "unlink") as unlink, \
      patch.object(HBASE_SERVICE, "Directory") as directory:
      HBASE_SERVICE._delete_staged_directory_or_link("/staging/hbase/conf")
    unlink.assert_called_once_with("/staging/hbase/conf")
    directory.assert_not_called()

  def test_staged_runtime_broken_symlink_is_also_unlinked(self):
    with patch.object(HBASE_SERVICE.sudo, "path_lexists", return_value=True), \
      patch.object(HBASE_SERVICE.sudo, "path_islink", return_value=True), \
      patch.object(HBASE_SERVICE.sudo, "path_exists", return_value=False), \
      patch.object(HBASE_SERVICE.sudo, "unlink") as unlink:
      HBASE_SERVICE._delete_staged_directory_or_link("/staging/hbase/conf")
    unlink.assert_called_once_with("/staging/hbase/conf")

  def test_staged_runtime_real_directory_uses_directory_resource(self):
    with patch.object(HBASE_SERVICE.sudo, "path_lexists", return_value=True), \
      patch.object(HBASE_SERVICE.sudo, "path_islink", return_value=False), \
      patch.object(HBASE_SERVICE.sudo, "path_isdir", return_value=True), \
      patch.object(HBASE_SERVICE.sudo, "unlink") as unlink, \
      patch.object(HBASE_SERVICE, "Directory") as directory:
      HBASE_SERVICE._delete_staged_directory_or_link("/staging/hbase/logs")
    directory.assert_called_once_with("/staging/hbase/logs", action="delete")
    unlink.assert_not_called()

  def test_staged_runtime_regular_file_fails_closed(self):
    with patch.object(HBASE_SERVICE.sudo, "path_lexists", return_value=True), \
      patch.object(HBASE_SERVICE.sudo, "path_islink", return_value=False), \
      patch.object(HBASE_SERVICE.sudo, "path_isdir", return_value=False):
      with self.assertRaisesRegex(Fail, "unexpected staged HBase path type"):
        HBASE_SERVICE._delete_staged_directory_or_link("/staging/hbase/pids")

  def test_existing_archive_has_owner_and_mode_normalized(self):
    params = self._params()
    archive = f"{params.yarn_hbase_user_tmp}/hbase.tar.gz"

    def path_islink(path):
      return False

    with patch.dict(sys.modules, {"params": params}), \
      patch.object(HBASE_SERVICE.sudo, "path_islink", side_effect=path_islink), \
      patch.object(HBASE_SERVICE.sudo, "path_lexists", return_value=True), \
      patch.object(HBASE_SERVICE.sudo, "path_exists", return_value=True), \
      patch.object(HBASE_SERVICE.sudo, "path_isdir", return_value=True), \
      patch.object(HBASE_SERVICE.sudo, "path_isfile", return_value=True), \
      patch.object(
        HBASE_SERVICE.sudo,
        "lstat",
        side_effect=lambda path: SimpleNamespace(
          st_mode=(
            stat.S_IFREG | 0o444
            if path == archive
            else stat.S_IFDIR | 0o755
          ),
          st_uid=0,
          st_dev=1,
          st_ino=2,
          st_nlink=1,
        ),
      ), \
      patch.object(HBASE_SERVICE, "Directory"), \
      patch.object(HBASE_SERVICE, "File") as file_resource, \
      patch.object(HBASE_SERVICE, "_validate_tar_archive"):
      HBASE_SERVICE.create_hbase_package()

    file_resource.assert_called_once_with(
      archive,
      owner="root",
      group="root",
      mode=0o444,
    )

  def test_existing_archive_rejects_nonroot_or_writable_source(self):
    archive = "/var/lib/ambari-agent/yarn-ats-hbase/3.3.6-1/hbase.tar.gz"
    for mode, uid in (
      (stat.S_IFREG | 0o666, 0),
      (stat.S_IFREG | 0o444, 1001),
    ):
      with self.subTest(mode=mode, uid=uid), \
        patch.object(
          HBASE_SERVICE.sudo,
          "lstat",
          return_value=SimpleNamespace(
            st_mode=mode,
            st_uid=uid,
            st_dev=1,
            st_ino=2,
            st_nlink=1,
          ),
        ):
        with self.assertRaisesRegex(Fail, "root-owned and non-writable"):
          HBASE_SERVICE._regular_file_identity(archive, "Existing HBase archive")

  def test_corrupt_or_partial_archive_is_rejected(self):
    with tempfile.TemporaryDirectory() as temp_dir:
      corrupt = Path(temp_dir) / "corrupt.tar.gz"
      corrupt.write_bytes(b"not a tar archive")
      with self.assertRaisesRegex(Fail, "not a readable gzip tar"):
        HBASE_SERVICE._validate_tar_archive(
          str(corrupt),
          ("hadoop", "hbase"),
          ("hadoop/bin/hadoop", "hbase/bin/hbase"),
        )

  def test_archive_rejects_duplicate_or_invalid_required_members(self):
    required = ("hadoop/bin/hadoop", "hbase/lib/hbase.jar")
    cases = (
      ("duplicate", "duplicate member"),
      ("symlink", "not a regular file"),
      ("non_executable", "not executable"),
    )
    with tempfile.TemporaryDirectory() as temp_dir:
      for case, message in cases:
        with self.subTest(case=case):
          archive_path = Path(temp_dir) / f"{case}.tar.gz"
          with tarfile.open(archive_path, "w:gz") as archive:
            hadoop = tarfile.TarInfo("hadoop/bin/hadoop")
            hadoop.mode = 0o755 if case != "non_executable" else 0o644
            content = b"#!/bin/sh\n"
            if case == "symlink":
              hadoop.type = tarfile.SYMTYPE
              hadoop.linkname = "../lib/hadoop"
              archive.addfile(hadoop)
            else:
              hadoop.size = len(content)
              archive.addfile(hadoop, io.BytesIO(content))
            if case == "duplicate":
              duplicate = tarfile.TarInfo("hadoop/bin/hadoop")
              duplicate.mode = 0o755
              duplicate.size = len(content)
              archive.addfile(duplicate, io.BytesIO(content))
            jar = tarfile.TarInfo("hbase/lib/hbase.jar")
            jar.size = 1
            archive.addfile(jar, io.BytesIO(b"x"))

          with self.assertRaisesRegex(Fail, message):
            HBASE_SERVICE._validate_tar_archive(
              str(archive_path), ("hadoop", "hbase"), required
            )

      partial = Path(temp_dir) / "partial.tar.gz"
      with tarfile.open(partial, "w:gz") as archive:
        content = b"#!/bin/sh\n"
        member = tarfile.TarInfo("hbase/bin/hbase")
        member.size = len(content)
        archive.addfile(member, io.BytesIO(content))
      with self.assertRaisesRegex(Fail, "unexpected top-level entries"):
        HBASE_SERVICE._validate_tar_archive(
          str(partial),
          ("hadoop", "hbase"),
          ("hadoop/bin/hadoop", "hbase/bin/hbase"),
        )

  def test_concurrent_publisher_archive_is_validated_before_reuse(self):
    params = self._params()
    archive = f"{params.yarn_hbase_user_tmp}/hbase.tar.gz"
    with patch.dict(sys.modules, {"params": params}), \
      patch.object(HBASE_SERVICE.sudo, "path_islink", return_value=False), \
      patch.object(HBASE_SERVICE.sudo, "path_lexists", return_value=True), \
      patch.object(
        HBASE_SERVICE.sudo,
        "path_exists",
        side_effect=lambda path: path != archive,
      ), \
      patch.object(HBASE_SERVICE.sudo, "path_isdir", return_value=True), \
      patch.object(HBASE_SERVICE.sudo, "path_isfile", return_value=True), \
      patch.object(
        HBASE_SERVICE.sudo,
        "lstat",
        side_effect=lambda path: SimpleNamespace(
          st_mode=(
            stat.S_IFREG | 0o444
            if path == archive
            or path.endswith(("hbase.tar.gz", ".jar"))
            or "/bin/" in path
            else stat.S_IFDIR | 0o755
          ),
          st_uid=0,
          st_dev=1,
          st_ino=2 if path == archive else 3,
          st_nlink=1,
        ),
      ), \
      patch.object(
        HBASE_SERVICE.sudo,
        "link_exclusive",
        side_effect=OSError("already published"),
      ), \
      patch.object(HBASE_SERVICE.glob, "glob", return_value=["zookeeper.jar"]), \
      patch.object(HBASE_SERVICE, "_snapshot_package_sources", return_value={}), \
      patch.object(HBASE_SERVICE, "_require_same_package_sources"), \
      patch.object(HBASE_SERVICE, "Directory"), \
      patch.object(HBASE_SERVICE, "File"), \
      patch.object(HBASE_SERVICE, "Execute"), \
      patch.object(HBASE_SERVICE, "_validate_tar_archive") as validate:
      HBASE_SERVICE.create_hbase_package()

    self.assertIn("/.hbase-package-", validate.call_args_list[0].args[0])
    self.assertEqual(archive, validate.call_args_list[1].args[0])
    self.assertEqual(
      HBASE_SERVICE._hadoop_archive_required_members("3.3.6-1"),
      validate.call_args_list[1].args[2],
    )

  def test_archive_identity_change_is_rejected_and_rollback_is_inode_scoped(self):
    expected = SimpleNamespace(
      st_mode=stat.S_IFREG | 0o444,
      st_uid=0,
      st_dev=1,
      st_ino=2,
      st_nlink=2,
    )
    replacement = SimpleNamespace(
      st_mode=stat.S_IFREG | 0o444,
      st_uid=0,
      st_dev=1,
      st_ino=3,
      st_nlink=1,
    )
    with patch.object(HBASE_SERVICE.sudo, "lstat", return_value=replacement):
      with self.assertRaisesRegex(Fail, "changed while it was being published"):
        HBASE_SERVICE._require_same_file(
          "/staging/hbase.tar.gz", expected, "Staged HBase archive"
        )

    with patch.object(HBASE_SERVICE.sudo, "path_lexists", return_value=True), \
      patch.object(HBASE_SERVICE.sudo, "lstat", return_value=replacement), \
      patch.object(HBASE_SERVICE.sudo, "unlink") as unlink:
      self.assertFalse(
        HBASE_SERVICE._unlink_if_same_file("/archive/hbase.tar.gz", expected)
      )
    unlink.assert_not_called()

  def test_source_contains_only_structured_copy_and_tar_invocations(self):
    source = (SCRIPTS / "hbase_service.py").read_text(encoding="utf-8")
    self.assertNotIn("yarn_hbase_package_preparation", source)
    self.assertNotIn("rm -rf", source)
    self.assertNotIn("cp -", source)
    self.assertNotIn("mapreduce.tar.gz", source)
    self.assertNotIn("tar\", \"-xzf", source)
    self.assertIn('("cp", "-R", "--preserve=mode,timestamps,links"', source)
    self.assertIn('(\"hadoop*.jar\",)', source)
    self.assertEqual(3, source.count("_replace_external_zookeeper_links("))
    self.assertIn('"share", "hadoop", "mapreduce"', source)
    self.assertIn('"share", "hadoop", "yarn", "timelineservice"', source)
    self.assertIn("sudo.link_exclusive(staged_archive, archive_path)", source)
    self.assertIn('owner="root",\n      group="root",\n      mode=0o700', source)
    self.assertNotIn("user=params.yarn_hbase_user", source[source.index("def create_hbase_package"):source.index("def copy_hbase_package_to_hdfs")])

  def test_external_zookeeper_links_are_replaced_with_versioned_package_jars(self):
    destination = "/staging/hadoop/share/hadoop/common/lib"
    installed_links = (
      f"{destination}/zookeeper-3.7.2.jar",
      f"{destination}/zookeeper-jute-3.7.2.jar",
    )
    with patch.object(HBASE_SERVICE.glob, "glob", return_value=installed_links), \
      patch.object(HBASE_SERVICE, "File") as file_resource, \
      patch.object(HBASE_SERVICE, "_copy_matching_files") as copy_files:
      HBASE_SERVICE._replace_external_zookeeper_links(
        "/usr/bigtop/3.3.6-1/usr/lib/zookeeper", destination
      )

    self.assertEqual(
      [call(path, action="delete") for path in installed_links],
      file_resource.call_args_list,
    )
    self.assertEqual(
      [
        call(
          "/usr/bigtop/3.3.6-1/usr/lib/zookeeper/zookeeper-[0-9]*.jar",
          destination,
        ),
        call(
          "/usr/bigtop/3.3.6-1/usr/lib/zookeeper/zookeeper-jute-[0-9]*.jar",
          destination,
        ),
      ],
      copy_files.call_args_list,
    )

  def test_archive_contract_requires_all_bigtop_split_hadoop_components(self):
    required = HBASE_SERVICE._hadoop_archive_required_members("3.3.6-1")
    self.assertIn("hadoop/bin/hadoop", required)
    self.assertIn("hadoop/bin/hdfs", required)
    self.assertIn("hadoop/bin/mapred", required)
    self.assertIn("hadoop/bin/yarn", required)
    self.assertIn(
      "hadoop/share/hadoop/mapreduce/hadoop-mapreduce-client-core-3.3.6.jar",
      required,
    )

  def test_secure_table_creation_uses_one_private_cache_for_both_commands(self):
    params = params_module(
      security_enabled=True,
      yarn_hbase_user="yarn-ats",
      user_group="hadoop",
      yarn_hbase_executable="/usr/lib/hbase/bin/hbase",
      yarn_hbase_conf_dir="/etc/hadoop/conf/embedded-yarn-ats-hbase",
      yarn_hbase_schema_creator_args=("org.apache.SchemaCreator",),
      yarn_hbase_classpath_prefix="/usr/lib/hadoop-yarn/timelineservice/*",
      kinit_path_local="/usr/bin/kinit",
      yarn_ats_user_keytab="/etc/security/keytabs/yarn-ats.keytab",
      yarn_ats_principal_name="yarn-ats/host@REALM",
      yarn_hbase_log_dir="/var/log/hadoop-yarn/embedded-yarn-ats-hbase",
    )
    cache = MagicMock()
    cache.cache_dir = "/tmp/private/cache"
    cache.merge_environment.return_value = {
      "HBASE_CLASSPATH_PREFIX": params.yarn_hbase_classpath_prefix,
      "KRB5CCNAME": "FILE:/tmp/private/krb5cc",
    }
    cache_context = MagicMock()
    cache_context.__enter__.return_value = cache
    with patch.dict(sys.modules, {"params": params}), \
      patch.object(HBASE_SERVICE, "PrivateKerberosCache", return_value=cache_context), \
      patch.object(HBASE_SERVICE, "File") as file_resource, \
      patch.object(HBASE_SERVICE, "Execute") as execute:
      HBASE_SERVICE.createTables()

    cache.kinit.assert_called_once_with(
      "/usr/bin/kinit",
      "/etc/security/keytabs/yarn-ats.keytab",
      "yarn-ats/host@REALM",
    )
    file_resource.assert_called_once_with(
      "/tmp/private/cache/hbase_grant_permissions.rb",
      owner="yarn-ats",
      group="hadoop",
      mode=0o600,
      content=ANY,
    )
    hbase_calls = [entry for entry in execute.call_args_list if entry.args[0][0] != "sleep"]
    self.assertEqual(2, len(hbase_calls))
    self.assertTrue(
      all(
        entry.kwargs["environment"]["KRB5CCNAME"]
        == "FILE:/tmp/private/krb5cc"
        for entry in hbase_calls
      )
    )
    self.assertEqual(
      "/tmp/private/cache/hbase_grant_permissions.rb", hbase_calls[1].args[0][-1]
    )


class TestYarnFilesystemSafety(unittest.TestCase):
  def test_local_directory_rejects_noncanonical_dot_segments(self):
    for path in (
      "/data/yarn/../ssh",
      "/data/./yarn",
      "/data//yarn",
      "/data/yarn/",
      " /data/yarn",
    ):
      with self.subTest(path=path):
        with self.assertRaisesRegex(Fail, "normalized absolute path"):
          YARN_CONFIG._validate_local_service_directory(path, "runtime path")

  def test_daemon_directory_rejects_existing_directory_owned_by_another_user(self):
    directory_metadata = SimpleNamespace(st_mode=stat.S_IFDIR | 0o755)
    with patch.object(
        YARN_CONFIG.pwd,
        "getpwnam",
        return_value=SimpleNamespace(pw_uid=1001),
      ), \
      patch.object(YARN_CONFIG.sudo, "path_lexists", return_value=True), \
      patch.object(YARN_CONFIG.sudo, "path_islink", return_value=False), \
      patch.object(YARN_CONFIG.sudo, "lstat", return_value=directory_metadata), \
      patch.object(
        YARN_CONFIG.sudo,
        "stat",
        return_value=SimpleNamespace(st_uid=0, st_mode=0o755),
      ), \
      patch.object(YARN_CONFIG, "Directory") as directory:
      with self.assertRaisesRegex(Fail, "must already be owned by yarn"):
        YARN_CONFIG._daemon_owned_directory(
          "/etc/ssh", "NodeManager local directory", "yarn", "hadoop"
        )
    directory.assert_not_called()

  def test_daemon_directory_creates_only_below_trusted_root_parent(self):
    def path_exists(path):
      return path == "/data"

    with patch.object(
        YARN_CONFIG.pwd,
        "getpwnam",
        return_value=SimpleNamespace(pw_uid=1001),
      ), \
      patch.object(YARN_CONFIG.sudo, "path_lexists", side_effect=path_exists), \
      patch.object(YARN_CONFIG.sudo, "path_islink", return_value=False), \
      patch.object(
        YARN_CONFIG.sudo,
        "lstat",
        return_value=SimpleNamespace(st_mode=stat.S_IFDIR | 0o755),
      ), \
      patch.object(
        YARN_CONFIG.sudo,
        "stat",
        return_value=SimpleNamespace(st_uid=0, st_mode=0o755),
      ), \
      patch.object(YARN_CONFIG, "Directory") as directory:
      YARN_CONFIG._daemon_owned_directory(
        "/data/yarn/local", "NodeManager local directory", "yarn", "hadoop"
      )
    directory.assert_called_once_with(
      "/data/yarn/local",
      owner="yarn",
      group="hadoop",
      create_parents=True,
      mode=0o755,
      cd_access="a",
    )

  def test_existing_daemon_directory_is_validated_without_reconfiguration(self):
    def metadata(path):
      owner = 1001 if path == "/data/yarn/local" else 0
      return SimpleNamespace(st_uid=owner, st_mode=0o755)

    with patch.object(
        YARN_CONFIG.pwd,
        "getpwnam",
        return_value=SimpleNamespace(pw_uid=1001),
      ), \
      patch.object(YARN_CONFIG.sudo, "path_lexists", return_value=True), \
      patch.object(YARN_CONFIG.sudo, "path_islink", return_value=False), \
      patch.object(
        YARN_CONFIG.sudo,
        "lstat",
        return_value=SimpleNamespace(st_mode=stat.S_IFDIR | 0o755),
      ), \
      patch.object(YARN_CONFIG.sudo, "stat", side_effect=metadata), \
      patch.object(YARN_CONFIG, "Directory") as directory:
      self.assertEqual(
        "/data/yarn/local",
        YARN_CONFIG._daemon_owned_directory(
          "/data/yarn/local", "NodeManager local directory", "yarn", "hadoop"
        ),
      )
    directory.assert_not_called()

  def test_missing_daemon_directory_rejects_daemon_owned_or_writable_parent(self):
    for metadata in (
      SimpleNamespace(st_uid=1001, st_mode=0o755),
      SimpleNamespace(st_uid=0, st_mode=0o777),
    ):
      with self.subTest(metadata=metadata), \
        patch.object(
          YARN_CONFIG.pwd,
          "getpwnam",
          return_value=SimpleNamespace(pw_uid=1001),
        ), \
        patch.object(
          YARN_CONFIG.sudo,
          "path_lexists",
          side_effect=lambda path: path in ("/data", "/data/yarn"),
        ), \
        patch.object(YARN_CONFIG.sudo, "path_islink", return_value=False), \
        patch.object(
          YARN_CONFIG.sudo,
          "lstat",
          return_value=SimpleNamespace(st_mode=stat.S_IFDIR | 0o755),
        ), \
        patch.object(
          YARN_CONFIG.sudo,
          "stat",
          side_effect=lambda path: metadata
          if path == "/data/yarn"
          else SimpleNamespace(st_uid=0, st_mode=0o755),
        ), \
        patch.object(YARN_CONFIG, "Directory") as directory:
        with self.assertRaisesRegex(Fail, "parent must"):
          YARN_CONFIG._daemon_owned_directory(
            "/data/yarn/local", "NodeManager local directory", "yarn", "hadoop"
          )
      directory.assert_not_called()

  def test_resourcemanager_does_not_precreate_daemon_job_summary_log(self):
    yarn_source = (SCRIPTS / "yarn.py").read_text(encoding="utf-8")
    params_source = (SCRIPTS / "params_linux.py").read_text(encoding="utf-8")
    self.assertNotIn("yarn_job_summary_log", yarn_source)
    self.assertNotIn("job_summary_log", params_source)

  def test_runtime_prefixes_remain_root_owned(self):
    source = (SCRIPTS / "yarn.py").read_text(encoding="utf-8")
    runtime_block = source[source.index("for path, label, allowed_roots in") :]
    runtime_block = runtime_block[: runtime_block.index("if manages_embedded_hbase")]
    self.assertEqual(2, runtime_block.count("_root_owned_directory("))
    self.assertIn('(\"/run\", \"/var/run\")', runtime_block)
    self.assertIn('(\"/var/log\",)', runtime_block)
    self.assertIn("privileged_registry_pid_dir", runtime_block)
    self.assertIn("mode=0o750", runtime_block)

  def test_existing_safe_root_directory_is_not_reconfigured(self):
    directory_metadata = SimpleNamespace(st_mode=stat.S_IFDIR | 0o750)
    with patch.object(YARN_CONFIG.sudo, "path_lexists", return_value=True), \
      patch.object(YARN_CONFIG.sudo, "path_islink", return_value=False), \
      patch.object(YARN_CONFIG.sudo, "lstat", return_value=directory_metadata), \
      patch.object(
        YARN_CONFIG.sudo,
        "stat",
        return_value=SimpleNamespace(st_uid=0, st_mode=0o750),
      ), \
      patch.object(YARN_CONFIG, "Directory") as directory:
      self.assertEqual(
        "/var/log/hadoop-yarn",
        YARN_CONFIG._root_owned_directory(
          "/var/log/hadoop-yarn", "YARN log prefix", ("/var/log",)
        ),
      )
    directory.assert_not_called()

  def test_root_directory_rejects_unsafe_location_or_parent(self):
    with self.assertRaisesRegex(Fail, "allowed roots"):
      YARN_CONFIG._root_owned_directory(
        "/etc/ssh", "YARN PID prefix", ("/run", "/var/run")
      )

    directory_metadata = SimpleNamespace(st_mode=stat.S_IFDIR | 0o755)
    for metadata in (
      SimpleNamespace(st_uid=1001, st_mode=0o755),
      SimpleNamespace(st_uid=0, st_mode=0o777),
    ):
      with self.subTest(metadata=metadata), \
        patch.object(YARN_CONFIG.sudo, "path_lexists", return_value=True), \
        patch.object(YARN_CONFIG.sudo, "path_islink", return_value=False), \
        patch.object(YARN_CONFIG.sudo, "lstat", return_value=directory_metadata), \
        patch.object(YARN_CONFIG.sudo, "stat", return_value=metadata), \
        patch.object(YARN_CONFIG, "Directory") as directory:
        with self.assertRaisesRegex(Fail, "root-owned and non-writable"):
          YARN_CONFIG._root_owned_directory(
            "/run/hadoop-yarn", "YARN PID prefix", ("/run",)
          )
      directory.assert_not_called()

  def test_local_directory_allows_only_root_managed_var_run_alias(self):
    with patch.object(
        YARN_CONFIG.sudo,
        "path_lexists",
        side_effect=lambda path: path == "/var/run",
      ), \
      patch.object(
        YARN_CONFIG.sudo,
        "path_islink",
        side_effect=lambda path: path == "/var/run",
      ), \
      patch.object(YARN_CONFIG.sudo, "readlink", return_value="../run"), \
      patch.object(
        YARN_CONFIG.sudo, "stat", return_value=SimpleNamespace(st_uid=0)
      ):
      self.assertEqual(
        "/var/run/hadoop-yarn",
        YARN_CONFIG._validate_local_service_directory(
          "/var/run/hadoop-yarn", "yarn_pid_dir_prefix"
        ),
      )

    with patch.object(
        YARN_CONFIG.sudo,
        "path_lexists",
        side_effect=lambda path: path == "/var/run",
      ), \
      patch.object(
        YARN_CONFIG.sudo,
        "path_islink",
        side_effect=lambda path: path == "/var/run",
      ), \
      patch.object(YARN_CONFIG.sudo, "readlink", return_value="../tmp/run"), \
      patch.object(
        YARN_CONFIG.sudo, "stat", return_value=SimpleNamespace(st_uid=1000)
      ):
      with self.assertRaisesRegex(Fail, "symbolic link"):
        YARN_CONFIG._validate_local_service_directory(
          "/var/run/hadoop-yarn", "yarn_pid_dir_prefix"
        )

  def test_node_manager_cleanup_rejects_unsafe_paths_and_symlinks(self):
    for path in ("", "relative/path", "/", "/tmp", "/var"):
      with self.subTest(path=path):
        with self.assertRaises(Fail):
          YARN_CONFIG._validate_local_service_directory(path, "local dirs")

    with patch.object(
        YARN_CONFIG.sudo,
        "path_lexists",
        side_effect=lambda path: path == "/data/link",
      ), \
      patch.object(YARN_CONFIG.sudo, "path_islink", return_value=True):
      with self.assertRaisesRegex(Fail, "symbolic link"):
        YARN_CONFIG._validate_local_service_directory(
          "/data/link/yarn", "local dirs"
        )

  def test_node_manager_cleanup_normalizes_and_drops_empty_entries(self):
    with patch.object(YARN_CONFIG.sudo, "path_lexists", return_value=False):
      self.assertEqual(
        ["/data/yarn/local", "/data/yarn/other"],
        YARN_CONFIG._configured_node_manager_directories(
          [" /data/yarn/local ", "", "   ", "/data/yarn/other"],
          "local dirs",
        ),
      )

  def test_node_manager_cleanup_does_not_follow_symlinks(self):
    with tempfile.TemporaryDirectory() as temporary_directory:
      root = Path(temporary_directory) / "nm-local"
      outside = Path(temporary_directory) / "outside"
      root.mkdir()
      outside.mkdir()
      protected = outside / "keep"
      protected.write_text("keep", encoding="utf-8")
      (root / "external").symlink_to(outside, target_is_directory=True)
      nested = root / "nested"
      nested.mkdir()
      (nested / "data").write_text("remove", encoding="utf-8")

      YARN_CONFIG._remove_local_tree_safely(
        str(root),
        "NodeManager local directory",
        pwd.getpwuid(os.getuid()).pw_name,
      )

      self.assertFalse(root.exists())
      self.assertEqual("keep", protected.read_text(encoding="utf-8"))

  def test_node_manager_cleanup_requires_the_configured_service_owner(self):
    with tempfile.TemporaryDirectory() as temporary_directory:
      target = Path(temporary_directory) / "nm-local"
      target.mkdir()
      (target / "data").write_text("keep", encoding="utf-8")
      configured_owner = SimpleNamespace(pw_uid=os.getuid() + 1)
      with patch.object(
        YARN_CONFIG.pwd, "getpwnam", return_value=configured_owner
      ):
        with self.assertRaisesRegex(Fail, "must be owned"):
          YARN_CONFIG._remove_local_tree_safely(
            str(target), "NodeManager local directory", "yarn"
          )
      self.assertTrue((target / "data").is_file())

  def test_node_manager_cleanup_is_idempotent_when_target_is_missing(self):
    with tempfile.TemporaryDirectory() as temporary_directory:
      missing = Path(temporary_directory) / "missing" / "nm-local"
      YARN_CONFIG._remove_local_tree_safely(
        str(missing),
        "NodeManager local directory",
        pwd.getpwuid(os.getuid()).pw_name,
      )

  def test_node_manager_security_marker_rejects_unsafe_state(self):
    marker_dir = "/var/lib/ambari-agent/data/yarn"
    marker_file = f"{marker_dir}/nm_security_enabled"

    def metadata(path, unsafe_path=None, unsafe_mode=None, unsafe_uid=None):
      is_file = path == marker_file
      mode = stat.S_IFREG | 0o640 if is_file else stat.S_IFDIR | 0o755
      uid = 0
      if path == unsafe_path:
        mode = unsafe_mode if unsafe_mode is not None else mode
        uid = unsafe_uid if unsafe_uid is not None else uid
      return SimpleNamespace(st_mode=mode, st_uid=uid)

    with patch.object(YARN_CONFIG.sudo, "path_lexists", return_value=True), \
      patch.object(YARN_CONFIG.sudo, "lstat", side_effect=metadata), \
      patch.object(YARN_CONFIG.sudo, "stat", side_effect=metadata):
      self.assertTrue(
        YARN_CONFIG._current_nm_security_state(
          marker_dir,
          marker_file,
          "/var/lib/hadoop-yarn/nm_security_enabled",
        )[0]
      )

    unsafe_cases = (
      (marker_file, stat.S_IFLNK | 0o777, 0),
      (marker_file, stat.S_IFREG | 0o666, 0),
      (marker_file, stat.S_IFREG | 0o640, 1001),
      (marker_dir, stat.S_IFLNK | 0o777, 0),
      (marker_dir, stat.S_IFDIR | 0o777, 0),
    )
    for unsafe_path, unsafe_mode, unsafe_uid in unsafe_cases:
      def unsafe_metadata(path):
        return metadata(path, unsafe_path, unsafe_mode, unsafe_uid)

      with self.subTest(path=unsafe_path, mode=unsafe_mode, uid=unsafe_uid), \
        patch.object(YARN_CONFIG.sudo, "path_lexists", return_value=True), \
        patch.object(YARN_CONFIG.sudo, "lstat", side_effect=unsafe_metadata), \
        patch.object(YARN_CONFIG.sudo, "stat", side_effect=unsafe_metadata):
        with self.assertRaises(Fail):
          YARN_CONFIG._current_nm_security_state(
            marker_dir,
            marker_file,
            "/var/lib/hadoop-yarn/nm_security_enabled",
          )

    with self.assertRaisesRegex(Fail, "Ambari Agent state directory"):
      YARN_CONFIG._current_nm_security_state(
        "/var/lib/hadoop-yarn",
        "/var/lib/hadoop-yarn/nm_security_enabled",
        "/var/lib/hadoop-yarn/nm_security_enabled",
      )

  def test_node_manager_security_marker_migrates_trusted_legacy_state(self):
    marker_dir = "/var/lib/ambari-agent/data/yarn"
    marker_file = f"{marker_dir}/nm_security_enabled"
    legacy_marker = "/var/lib/hadoop-yarn/nm_security_enabled"

    def path_exists(path):
      return path != marker_file

    def trusted_metadata(path):
      return SimpleNamespace(
        st_mode=(stat.S_IFREG | 0o640)
        if path == legacy_marker
        else (stat.S_IFDIR | 0o755),
        st_uid=0,
      )

    with patch.object(YARN_CONFIG.sudo, "path_lexists", side_effect=path_exists), \
      patch.object(YARN_CONFIG.sudo, "lstat", side_effect=trusted_metadata):
      self.assertEqual(
        (True, True),
        YARN_CONFIG._current_nm_security_state(
          marker_dir, marker_file, legacy_marker
        ),
      )

    with patch.object(YARN_CONFIG.sudo, "path_lexists", return_value=False):
      self.assertEqual(
        (False, False),
        YARN_CONFIG._current_nm_security_state(
          marker_dir, marker_file, legacy_marker
        ),
      )

    for unsafe_mode, unsafe_uid in (
      (stat.S_IFLNK | 0o777, 0),
      (stat.S_IFREG | 0o666, 0),
      (stat.S_IFREG | 0o640, 1001),
    ):
      def unsafe_metadata(path):
        if path == legacy_marker:
          return SimpleNamespace(st_mode=unsafe_mode, st_uid=unsafe_uid)
        return SimpleNamespace(st_mode=stat.S_IFDIR | 0o755, st_uid=0)

      with self.subTest(mode=unsafe_mode, uid=unsafe_uid), \
        patch.object(YARN_CONFIG.sudo, "path_lexists", side_effect=path_exists), \
        patch.object(YARN_CONFIG.sudo, "lstat", side_effect=unsafe_metadata):
        with self.assertRaises(Fail):
          YARN_CONFIG._current_nm_security_state(
            marker_dir, marker_file, legacy_marker
          )

  def test_node_manager_security_toggle_uses_root_owned_marker_contract(self):
    source = (SCRIPTS / "yarn.py").read_text(encoding="utf-8")
    setup_block = source[source.index("def setup_nodemanager()") :]
    setup_block = setup_block[: setup_block.index("def create_log_dir")]
    self.assertIn("_current_nm_security_state", setup_block)
    self.assertIn("current_nm_security_state != params.security_enabled", setup_block)
    self.assertIn("migrated_legacy_marker", setup_block)
    self.assertLess(
      setup_block.index('content="NodeManager security mode enabled"'),
      setup_block.index('File(params.legacy_nm_security_marker, action="delete")'),
    )
    self.assertIn("_remove_local_tree_safely(\n        cleanup_dir", setup_block)
    self.assertIn('_remove_local_tree_safely(\n        recovery_dir', setup_block)
    self.assertGreaterEqual(setup_block.count("params.yarn_user"), 2)
    self.assertIn('owner="root"', setup_block)
    self.assertIn("mode=0o640", setup_block)
    self.assertNotIn("params.toggle_nm_security", setup_block)

  def test_resource_manager_host_files_reject_unsafe_targets(self):
    config_dir = "/etc/hadoop/conf"
    stack_root = "/usr/bigtop"

    def trusted_metadata(path):
      is_file = path.endswith(("yarn.exclude", "yarn.include"))
      return SimpleNamespace(
        st_mode=(stat.S_IFREG | 0o644) if is_file else (stat.S_IFDIR | 0o755),
        st_uid=0,
      )

    with patch.object(
        YARN_CONFIG.sudo,
        "path_lexists",
        side_effect=lambda path: not path.endswith(("yarn.exclude", "yarn.include")),
      ), \
      patch.object(YARN_CONFIG.sudo, "path_islink", return_value=False), \
      patch.object(YARN_CONFIG.sudo, "lstat", side_effect=trusted_metadata), \
      patch.object(YARN_CONFIG.os.path, "realpath", return_value=config_dir):
      self.assertEqual(
        ("/etc/hadoop/conf/yarn.exclude", "/etc/hadoop/conf/yarn.include"),
        YARN_CONFIG._validated_resource_manager_host_files(
          "/etc/hadoop/conf/yarn.exclude",
          config_dir,
          stack_root,
          "/etc/hadoop/conf/yarn.include",
        ),
      )
      for path in (
        "relative/yarn.exclude",
        "/etc/shadow",
        "/etc/ssh/yarn.exclude",
        "/etc/hadoop/conf/nested/yarn.exclude",
      ):
        with self.subTest(path=path):
          with self.assertRaises(Fail):
            YARN_CONFIG._validated_resource_manager_host_files(
              path, config_dir, stack_root
            )

    with patch.object(YARN_CONFIG.sudo, "path_lexists", return_value=True), \
      patch.object(
        YARN_CONFIG.sudo,
        "path_islink",
        side_effect=lambda path: path == "/etc/hadoop/conf/yarn.exclude",
      ), \
      patch.object(
        YARN_CONFIG.sudo,
        "lstat",
        side_effect=lambda path: SimpleNamespace(
          st_mode=(
            stat.S_IFLNK | 0o777
            if path == "/etc/hadoop/conf/yarn.exclude"
            else stat.S_IFDIR | 0o755
          ),
          st_uid=0,
        ),
      ), \
      patch.object(YARN_CONFIG.os.path, "realpath", return_value=config_dir):
      with self.assertRaisesRegex(Fail, "regular file"):
        YARN_CONFIG._validated_resource_manager_host_files(
          "/etc/hadoop/conf/yarn.exclude", config_dir, stack_root
        )

    for unsafe_path, mode, uid in (
      ("/etc/hadoop/conf", stat.S_IFDIR | 0o777, 0),
      ("/etc/hadoop/conf", stat.S_IFDIR | 0o755, 1001),
      ("/etc/hadoop/conf/yarn.exclude", stat.S_IFREG | 0o666, 0),
      ("/etc/hadoop/conf/yarn.exclude", stat.S_IFREG | 0o644, 1001),
    ):
      def unsafe_metadata(path):
        if path == unsafe_path:
          return SimpleNamespace(st_mode=mode, st_uid=uid)
        return trusted_metadata(path)

      with self.subTest(path=unsafe_path, mode=mode, uid=uid), \
        patch.object(YARN_CONFIG.sudo, "path_lexists", return_value=True), \
        patch.object(YARN_CONFIG.sudo, "path_islink", return_value=False), \
        patch.object(YARN_CONFIG.sudo, "lstat", side_effect=unsafe_metadata), \
        patch.object(YARN_CONFIG.os.path, "realpath", return_value=config_dir):
        with self.assertRaisesRegex(Fail, "root-owned and non-writable"):
          YARN_CONFIG._validated_resource_manager_host_files(
            "/etc/hadoop/conf/yarn.exclude", config_dir, stack_root
          )

  def test_resource_manager_host_files_accept_trusted_conf_select_symlink(self):
    config_dir = "/etc/hadoop/conf"
    stack_root = "/usr/bigtop"
    resolved = "/usr/bigtop/3.3.6-1/usr/lib/hadoop/etc/hadoop"

    def metadata(path):
      return SimpleNamespace(
        st_mode=(
          stat.S_IFLNK | 0o777 if path == config_dir else stat.S_IFDIR | 0o755
        ),
        st_uid=0,
      )

    with patch.object(
        YARN_CONFIG.sudo,
        "path_lexists",
        side_effect=lambda path: not path.endswith("yarn.exclude"),
      ), \
      patch.object(
        YARN_CONFIG.sudo,
        "path_islink",
        side_effect=lambda path: path == config_dir,
      ), \
      patch.object(YARN_CONFIG.sudo, "lstat", side_effect=metadata), \
      patch.object(
        YARN_CONFIG.os.path,
        "realpath",
        side_effect=lambda path: (
          resolved
          if path
          in (config_dir, "/usr/bigtop/current/hadoop-client/conf")
          else path
        ),
      ):
      self.assertEqual(
        ("/etc/hadoop/conf/yarn.exclude", None),
        YARN_CONFIG._validated_resource_manager_host_files(
          "/etc/hadoop/conf/yarn.exclude", config_dir, stack_root
        ),
      )

    with patch.object(YARN_CONFIG.sudo, "path_lexists", return_value=True), \
      patch.object(
        YARN_CONFIG.sudo,
        "path_islink",
        side_effect=lambda path: path == config_dir,
      ), \
      patch.object(YARN_CONFIG.sudo, "lstat", side_effect=metadata), \
      patch.object(
        YARN_CONFIG.os.path,
        "realpath",
        side_effect=lambda path: (
          "/tmp/untrusted" if path == config_dir else resolved
        ),
      ):
      with self.assertRaisesRegex(Fail, "BIGTOP-managed"):
        YARN_CONFIG._validated_resource_manager_host_files(
          "/etc/hadoop/conf/yarn.exclude", config_dir, stack_root
        )

  def test_resource_manager_generated_host_and_topology_files_are_root_owned(self):
    yarn_source = (SCRIPTS / "yarn.py").read_text(encoding="utf-8")
    setup_block = yarn_source[yarn_source.index("def setup_resourcemanager()") :]
    setup_block = setup_block[: setup_block.index("def setup_ats()")]
    self.assertEqual(2, setup_block.count('owner="root"'))
    self.assertEqual(2, setup_block.count("mode=0o644"))
    self.assertNotIn("owner=params.yarn_user", setup_block)

    rm_source = (SCRIPTS / "resourcemanager.py").read_text(encoding="utf-8")
    decommission_block = rm_source[rm_source.index("def decommission") :]
    decommission_block = decommission_block[
      : decommission_block.index("def disable_security")
    ]
    self.assertEqual(3, decommission_block.count('owner="root"'))
    self.assertEqual(3, decommission_block.count("mode=0o644"))
    self.assertNotIn("owner=yarn_user", decommission_block)
    self.assertNotIn("owner=params.hdfs_user", decommission_block)

  def test_node_labels_reject_unsafe_hdfs_roots(self):
    for path in ("", "relative/labels", "/", "/labels"):
      with self.subTest(path=path):
        with self.assertRaises(Fail):
          YARN_CONFIG._validate_hdfs_directory(
            path, "yarn.node-labels.fs-store.root-dir"
          )
    self.assertEqual(
      "/system/yarn/node-labels",
      YARN_CONFIG._validate_hdfs_directory(
        "/system/yarn/node-labels", "yarn.node-labels.fs-store.root-dir"
      ),
    )
    source = (SCRIPTS / "yarn.py").read_text(encoding="utf-8")
    setup_rm = source[source.index("def setup_resourcemanager()") :]
    setup_rm = setup_rm[: setup_rm.index("def setup_ats()")]
    self.assertIn("validate_hdfs_directory", setup_rm)

  def test_hdfs_directory_validation_supports_safe_uris_and_rejects_roots(self):
    for path in (
      "/",
      "/top-level",
      "/safe/../escaped",
      "relative/path",
      "hdfs://nameservice/",
      "hdfs://user@nameservice/safe/path",
      "hdfs://nameservice/safe/%2e%2e/escaped",
      "hdfs://nameservice/safe%2fescaped/path",
      "hdfs://name%40service/safe/path",
      "/safe/%2e%2e/escaped",
      "/safe/path?recursive=true",
      "/safe/path#fragment",
      "/safe/path\nchild",
      "/safe\\path/child",
      "file:///safe/path",
    ):
      with self.subTest(path=path):
        with self.assertRaises(Fail):
          HBASE_SERVICE.validate_hdfs_directory(
            path, "configured HDFS directory", allow_uri=True
          )
    self.assertEqual(
      "hdfs://nameservice/safe/path",
      HBASE_SERVICE.validate_hdfs_directory(
        "hdfs://nameservice//safe/path",
        "configured HDFS directory",
        allow_uri=True,
      ),
    )
    self.assertEqual(
      "viewfs:///safe/path",
      HBASE_SERVICE.validate_hdfs_directory(
        "viewfs:///safe/path", "configured HDFS directory", allow_uri=True
      ),
    )

  def test_historyserver_and_nodemanager_state_paths_are_hardened(self):
    source = (SCRIPTS / "yarn.py").read_text(encoding="utf-8")
    history_block = source[source.index("def setup_historyserver()") :]
    history_block = history_block[: history_block.index("def setup_nodemanager()")]
    self.assertEqual(3, history_block.count("validate_hdfs_directory"))
    self.assertNotIn('"/mapred"', history_block)
    self.assertNotIn('"/mapred/system"', history_block)
    self.assertIn('owner="root"', source[source.index("def setup_nodemanager()") :])
    log_block = source[source.index("def create_log_dir") :]
    log_block = log_block[: log_block.index("def create_local_dir")]
    self.assertIn("mode=0o755", log_block)
    self.assertNotIn("mode=0o775", log_block)

  def test_ats_hdfs_resources_never_change_parent_permissions_recursively(self):
    params = params_module(
      HdfsResource=MagicMock(),
      hdfs_tmp_dir="/tmp",
      hdfs_user="hdfs",
      yarn_user="yarn",
      user_group="hadoop",
    )
    YARN_CONFIG._create_ats_hdfs_directory(params, "/ats/done", 0o700)
    YARN_CONFIG._create_ats_hdfs_directory(params, "/tmp/ats-active", 0o1777)

    for resource in params.HdfsResource.call_args_list:
      self.assertNotIn("change_permissions_for_parents", resource.kwargs)
    params.HdfsResource.assert_any_call(
      "/ats",
      type="directory",
      action="create_on_execute",
      owner="yarn",
      group="hadoop",
      mode=0o755,
    )
    params.HdfsResource.assert_any_call(
      "/tmp",
      type="directory",
      action="create_on_execute",
      owner="hdfs",
      group="hadoop",
      mode=0o1777,
    )
    params.hdfs_tmp_dir = "/"
    with self.assertRaisesRegex(Fail, "unsafe HDFS directory"):
      YARN_CONFIG._create_ats_hdfs_directory(params, "/tmp/ats-active", 0o1777)

  def test_historyserver_does_not_manage_ats_entity_history(self):
    source = (SCRIPTS / "yarn.py").read_text(encoding="utf-8")
    params_source = (SCRIPTS / "params_linux.py").read_text(encoding="utf-8")
    self.assertNotIn("entity_file_history_directory", source)
    self.assertNotIn("entity_file_history_directory", params_source)
    history_block = source[source.index("def setup_historyserver()") :]
    history_block = history_block[: history_block.index("def setup_nodemanager()")]
    self.assertIn("_validate_local_service_directory", history_block)
    self.assertNotIn("recursive_ownership", history_block)

  def test_ats_leveldb_paths_are_validated_and_created_once(self):
    source = (SCRIPTS / "yarn.py").read_text(encoding="utf-8")
    self.assertNotIn("yarn_timeline_service_leveldb_state_store_path", source)
    self.assertEqual(1, source.count("params.ats_leveldb_state_store_dir"))
    ats_block = source[source.index("def setup_ats()") :]
    ats_block = ats_block[: ats_block.index("def create_log_dir")]
    self.assertEqual(2, ats_block.count("_validate_local_service_directory"))

  def test_ranger_yarn_audit_resources_do_not_modify_existing_trees(self):
    params = params_module(
      HdfsResource=MagicMock(),
      hdfs_user="hdfs",
      yarn_user="yarn",
    )
    RANGER_YARN._create_ranger_yarn_audit_dirs(params)

    self.assertEqual(3, params.HdfsResource.call_count)
    for resource in params.HdfsResource.call_args_list:
      self.assertNotIn("recursive_chmod", resource.kwargs)
      self.assertNotIn("recursive_chown", resource.kwargs)


class TestYarnSystemServicePublication(unittest.TestCase):
  def _params(self):
    return params_module(
      version="3.3.6-1",
      rm_ha_enabled=False,
      rm_ha_id=None,
      security_enabled=False,
      yarn_hbase_conf_dir="/etc/hadoop/conf/embedded-yarn-ats-hbase",
      yarn_hbase_user="yarn-ats",
      yarn_user="yarn",
      user_group="hadoop",
      yarn_system_service_dir="/services",
      yarn_system_service_launch_mode="sync",
      HdfsResource=MagicMock(),
      yarn_hbase_user_home="/user/yarn-ats",
      yarn_hbase_user_version_home="/user/yarn-ats/3.3.6-1",
      has_metric_collector=False,
      yarn_hbase_hdfs_root_dir="/apps/hbase/data",
      yarn_service_app_hdfs_path="/bigtop/apps/3.3.6-1/yarn",
      yarn_hbase_app_hdfs_path="/bigtop/apps/3.3.6-1/hbase",
      hdfs_user="hdfs",
      hadoop_yarn_home="/usr/bigtop/3.3.6-1/usr/lib/hadoop-yarn",
    )

  def test_system_service_rejects_unsafe_dynamic_paths_before_mutation(self):
    cases = (
      ("version", "../current"),
      ("yarn_hbase_user", "../hdfs"),
      ("rm_ha_id", "../../rm1"),
      ("yarn_system_service_launch_mode", "rolling"),
      ("yarn_system_service_dir", "/"),
      ("yarn_hbase_hdfs_root_dir", "hdfs://nameservice/"),
    )
    for attribute, value in cases:
      params = self._params()
      setattr(params, attribute, value)
      with self.subTest(attribute=attribute, value=value), \
        patch.dict(sys.modules, {"params": params}), \
        patch.object(YARN_CONFIG, "File") as file_resource:
        with self.assertRaises(Fail):
          YARN_CONFIG.setup_system_services("/etc/hadoop/conf")
      file_resource.assert_not_called()

  def test_atsv2_consumers_fail_only_when_the_disabled_component_is_invoked(self):
    params = params_module(
      atsv2_backend_enabled=False,
      use_external_hbase=False,
      hbase_within_cluster=False,
      is_hbase_system_service_launch=False,
    )
    with patch.dict(sys.modules, {"params": params}):
      self.assertIsNone(
        YARN_CONFIG.setup_atsv2_backend("resourcemanager", "/etc/hadoop/conf")
      )
      with self.assertRaisesRegex(Fail, "TIMELINE_READER requires"):
        YARN_CONFIG.setup_atsv2_backend(
          "apptimelinereader", "/etc/hadoop/conf"
        )

  def test_system_service_preserves_valid_hdfs_uri_authority(self):
    params = self._params()
    params.yarn_system_service_dir = "hdfs://nameservice/services"
    with patch.dict(sys.modules, {"params": params}), \
      patch.object(YARN_CONFIG, "setup_atsv2_hbase_files"), \
      patch.object(YARN_CONFIG, "File"), \
      patch.object(YARN_CONFIG, "Execute"), \
      patch.object(YARN_CONFIG, "create_hbase_package"), \
      patch.object(YARN_CONFIG, "copy_hbase_package_to_hdfs"):
      YARN_CONFIG.setup_system_services("/etc/hadoop/conf")
    params.HdfsResource.assert_any_call(
      "hdfs://nameservice/services/sync/yarn-ats",
      type="directory",
      action="create_on_execute",
      owner="yarn",
      group="hadoop",
    )

  def test_ha_system_service_requires_local_id_and_isolated_paths(self):
    params = self._params()
    params.rm_ha_enabled = True
    with patch.dict(sys.modules, {"params": params}), \
      patch.object(YARN_CONFIG, "File") as file_resource:
      with self.assertRaisesRegex(Fail, "must match exactly one HA ID"):
        YARN_CONFIG.setup_system_services("/etc/hadoop/conf")
    file_resource.assert_not_called()

    params.rm_ha_id = "rm0"
    with patch.dict(sys.modules, {"params": params}), \
      patch.object(YARN_CONFIG, "File") as file_resource:
      with self.assertRaisesRegex(Fail, "isolated"):
        YARN_CONFIG.setup_system_services("/etc/hadoop/conf")
    file_resource.assert_not_called()

    params.yarn_hbase_app_hdfs_path += "/rm0"
    with patch.dict(sys.modules, {"params": params}), \
      patch.object(YARN_CONFIG, "setup_atsv2_hbase_files"), \
      patch.object(YARN_CONFIG, "File"), \
      patch.object(YARN_CONFIG, "Execute"), \
      patch.object(YARN_CONFIG, "create_hbase_package"), \
      patch.object(YARN_CONFIG, "copy_hbase_package_to_hdfs"):
      YARN_CONFIG.setup_system_services("/etc/hadoop/conf")

  def test_framework_directory_and_hbase_archive_are_published_before_manifest(self):
    params = self._params()
    events = []

    def hdfs_resource(path, **kwargs):
      events.append(("hdfs", path, kwargs))

    params.HdfsResource.side_effect = hdfs_resource

    def execute(command, **kwargs):
      events.append(("execute", command, kwargs))

    with patch.dict(sys.modules, {"params": params}), \
      patch.object(YARN_CONFIG, "setup_atsv2_hbase_files"), \
      patch.object(YARN_CONFIG, "File"), \
      patch.object(YARN_CONFIG, "Execute", side_effect=execute), \
      patch.object(
        YARN_CONFIG,
        "create_hbase_package",
        side_effect=lambda: events.append(("create_archive",)),
      ), \
      patch.object(
        YARN_CONFIG,
        "copy_hbase_package_to_hdfs",
        side_effect=lambda: events.append(("copy_archive",)),
      ):
      YARN_CONFIG.setup_system_services("/etc/hadoop/conf")

    framework_index = next(
      index
      for index, event in enumerate(events)
      if event[0] == "hdfs" and event[1] == "/bigtop/apps/3.3.6-1/yarn"
    )
    create_index = events.index(("create_archive",))
    copy_index = events.index(("copy_archive",))
    manifest_index = next(
      index
      for index, event in enumerate(events)
      if len(event) > 1 and event[1].endswith("hbase.yarnfile")
    )
    self.assertLess(framework_index, create_index)
    self.assertLess(create_index, copy_index)
    self.assertLess(copy_index, manifest_index)
    self.assertEqual(("hdfs", None, {"action": "execute"}), events[-1])
    framework_directory = next(
      event
      for event in events
      if event[0] == "hdfs"
      and event[1] == "/bigtop/apps/3.3.6-1/yarn"
    )
    self.assertEqual("yarn", framework_directory[2]["owner"])
    self.assertEqual("hadoop", framework_directory[2]["group"])
    self.assertEqual(0o755, framework_directory[2]["mode"])
    self.assertFalse(any(event[0] == "execute" for event in events))

  def test_configure_never_overwrites_the_fast_launch_dependency_archive(self):
    params = self._params()
    with patch.dict(sys.modules, {"params": params}), \
      patch.object(YARN_CONFIG, "setup_atsv2_hbase_files"), \
      patch.object(YARN_CONFIG, "File"), \
      patch.object(YARN_CONFIG, "Execute") as execute, \
      patch.object(YARN_CONFIG, "create_hbase_package"), \
      patch.object(YARN_CONFIG, "copy_hbase_package_to_hdfs"):
      YARN_CONFIG.setup_system_services("/etc/hadoop/conf")
    execute.assert_not_called()
    source = (SCRIPTS / "yarn.py").read_text(encoding="utf-8")
    self.assertNotIn("enableFastLaunch", source)

  def test_secure_system_service_publication_protects_sensitive_files(self):
    params = self._params()
    params.security_enabled = True
    params.kinit_path_local = "/usr/bin/kinit"
    params.rm_keytab = "/etc/security/keytabs/rm.keytab"
    params.rm_principal_name = "rm/host@REALM"
    params.yarn_hbase_grant_permissions_file = (
      "/etc/hadoop/conf/embedded-yarn-ats-hbase/hbase_grant_permissions.rb"
    )
    with patch.dict(sys.modules, {"params": params}), \
      patch.object(YARN_CONFIG, "setup_atsv2_hbase_files"), \
      patch.object(YARN_CONFIG, "File") as file_resource, \
      patch.object(YARN_CONFIG, "Execute") as execute, \
      patch.object(YARN_CONFIG, "create_hbase_package"), \
      patch.object(YARN_CONFIG, "copy_hbase_package_to_hdfs"):
      YARN_CONFIG.setup_system_services("/etc/hadoop/conf")

    execute.assert_not_called()
    manifest = next(
      entry
      for entry in file_resource.call_args_list
      if entry.args[0].endswith("hbase.yarnfile")
    )
    self.assertEqual(0o644, manifest.kwargs["mode"])
    grant_upload = next(
      entry
      for entry in params.HdfsResource.call_args_list
      if entry.args
      and entry.args[0].endswith("hbase_grant_permissions.rb")
    )
    self.assertEqual(0o600, grant_upload.kwargs["mode"])
    published_manifest = next(
      entry
      for entry in params.HdfsResource.call_args_list
      if entry.args and entry.args[0].endswith("hbase.yarnfile")
    )
    self.assertEqual(0o644, published_manifest.kwargs["mode"])


if __name__ == "__main__":
  unittest.main()
