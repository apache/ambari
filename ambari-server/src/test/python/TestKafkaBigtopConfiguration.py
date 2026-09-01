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
import os
from pathlib import Path
import sys
from types import ModuleType, SimpleNamespace
import unittest
from unittest.mock import MagicMock, patch
from xml.etree import ElementTree

from resource_management.core.exceptions import Fail


KAFKA = (
  Path(__file__).resolve().parents[2]
  / "main/resources/stacks/BIGTOP/3.2.0/services/KAFKA"
)
KAFKA_33 = KAFKA.parents[2] / "3.3.0/services/KAFKA"
SCRIPTS = KAFKA / "package/scripts"


def dependency_module(name, **attributes):
  module = ModuleType(name)
  for attribute, value in attributes.items():
    setattr(module, attribute, value)
  return module


def load_module(module_name, path, dependencies=None):
  spec = importlib.util.spec_from_file_location(module_name, path)
  module = importlib.util.module_from_spec(spec)
  with patch.dict(sys.modules, dependencies or {}):
    spec.loader.exec_module(module)
  return module


KAFKA_CLIENT = load_module("bigtop_kafka_client", SCRIPTS / "kafka_client.py")
KAFKA_CONFIG = load_module(
  "bigtop_kafka_config",
  SCRIPTS / "kafka.py",
  {"kafka_client": KAFKA_CLIENT},
)
KAFKA_SERVICE_CHECK = load_module(
  "bigtop_kafka_service_check", SCRIPTS / "service_check.py"
)
KAFKA_UTILS = load_module("bigtop_kafka_utils", SCRIPTS / "utils.py")


class TestKafkaClientContract(unittest.TestCase):
  def test_selects_inter_broker_protocol_and_builds_ipv4_bootstrap_list(self):
    properties = {
      "listeners": "PLAINTEXT://:9092,SASL_PLAINTEXT://:9093",
      "security.inter.broker.protocol": "SASL_PLAINTEXT",
    }
    self.assertEqual(
      ("SASL_PLAINTEXT", "SASL_PLAINTEXT", 9093),
      KAFKA_CLIENT.select_listener(properties),
    )
    self.assertEqual(
      "broker1:9093,broker2:9093",
      KAFKA_CLIENT.bootstrap_servers(properties, ["broker1", "broker2"]),
    )

  def test_custom_listener_map_and_inter_broker_name_are_supported(self):
    properties = {
      "listeners": "EXTERNAL://:19092,INTERNAL://:19093",
      "listener.security.protocol.map": (
        "EXTERNAL:SSL,INTERNAL:SASL_PLAINTEXT"
      ),
      "inter.broker.listener.name": "INTERNAL",
    }
    self.assertEqual(
      ("INTERNAL", "SASL_PLAINTEXT", 19093),
      KAFKA_CLIENT.select_listener(properties),
    )
    self.assertEqual(
      "SASL_PLAINTEXT", KAFKA_CLIENT.inter_broker_protocol(properties)
    )

  def test_sasl_listener_migration_is_parsed_and_idempotent(self):
    self.assertEqual(
      "SASL_PLAINTEXT://:9092,SASL_SSL://[::]:9093,INTERNAL://host:9094",
      KAFKA_CLIENT.sasl_listeners(
        "PLAINTEXT://:9092,SASL_SSL://[::]:9093,INTERNAL://host:9094"
      ),
    )
    self.assertEqual(
      "SASL_PLAINTEXT://broker.example.com:19092,SASL_SSL://host:9093",
      KAFKA_CLIENT.merge_advertised_listeners(
        "SASL_PLAINTEXT://:9092,SASL_SSL://:9093",
        "SASL_PLAINTEXT://broker.example.com:19092",
      ),
    )
    with self.assertRaisesRegex(Fail, "no matching listener"):
      KAFKA_CLIENT.merge_advertised_listeners(
        "SASL_PLAINTEXT://:9092", "EXTERNAL://host:19092"
      )
    self.assertEqual(
      "INTERNAL:SASL_PLAINTEXT,EXTERNAL:SASL_SSL",
      KAFKA_CLIENT.sasl_listener_protocol_map(
        "INTERNAL:PLAINTEXT,EXTERNAL:SASL_SSL"
      ),
    )

  def test_ipv6_hosts_are_bracketed_without_changing_configured_port(self):
    properties = {"listeners": "PLAINTEXT://[::]:9092"}
    self.assertEqual(
      "[2001:db8::10]:9092,[2001:db8::11]:9092",
      KAFKA_CLIENT.bootstrap_servers(
        properties, ["2001:db8::10", "2001:db8::11"]
      ),
    )

  def test_invalid_listener_host_port_and_protocol_fail_closed(self):
    invalid_properties = (
      {"listeners": "PLAINTEXT://host:9092;$(id)"},
      {"listeners": "PLAINTEXT://host:0"},
      {"listeners": "PLAINTEXT://host:65536"},
      {"listeners": "PLAINTEXT://host;$(id):9092"},
      {"listeners": "INTERNAL://host:9092"},
      {
        "listeners": "INTERNAL://host:9092",
        "listener.security.protocol.map": "INTERNAL:UNKNOWN",
      },
      {
        "listeners": "PLAINTEXT://host:9092",
        "security.inter.broker.protocol": "SASL_SSL",
      },
    )
    for properties in invalid_properties:
      with self.subTest(properties=properties):
        with self.assertRaises(Fail):
          KAFKA_CLIENT.select_listener(properties)

    with self.assertRaises(Fail):
      KAFKA_CLIENT.bootstrap_servers(
        {"listeners": "PLAINTEXT://:9092"}, []
      )
    with self.assertRaises(Fail):
      KAFKA_CLIENT.bootstrap_servers(
        {"listeners": "PLAINTEXT://:9092"}, ["broker;$(id)"]
      )

  def test_client_properties_cover_kerberos_and_tls_without_client_key(self):
    properties = {
      "listeners": "SASL_SSL://:9093",
      "sasl.mechanism.inter.broker.protocol": "GSSAPI",
      "ssl.truststore.location": "/etc/security/kafka truststore.jks",
      "ssl.truststore.password": "secret;$(id)",
      "ssl.truststore.type": "JKS",
      "ssl.keystore.location": "/etc/security/broker-private.jks",
    }
    result = KAFKA_CLIENT.client_properties(properties, "kafka")
    self.assertEqual("SASL_SSL", result["security.protocol"])
    self.assertEqual("GSSAPI", result["sasl.mechanism"])
    self.assertEqual("kafka", result["sasl.kerberos.service.name"])
    self.assertEqual(
      "/etc/security/kafka truststore.jks", result["ssl.truststore.location"]
    )
    self.assertNotIn("ssl.keystore.location", result)

    with self.assertRaisesRegex(Fail, "service name"):
      KAFKA_CLIENT.client_properties(properties)


class TestKafkaConfigurationResources(unittest.TestCase):
  def _params(self, secure=False):
    protocol = "SASL_PLAINTEXT" if secure else "PLAINTEXT"
    return SimpleNamespace(
      config={
        "configurations": {
          "kafka-broker": {
            "listeners": f"{protocol}://localhost:9092",
            "log.dirs": "/data/kafka-a,/data/kafka-b",
          }
        }
      },
      stack_version_formatted="3.3.0",
      version=None,
      hostname="broker1.example.com",
      kerberos_security_enabled=secure,
      kafka_kerberos_enabled=secure,
      kafka_kerberos_merge_advertised_listeners=True,
      kafka_other_sasl_enabled=False,
      kafka_jaas_enabled=secure,
      has_metric_collector=False,
      all_hosts=["broker1.example.com"],
      all_racks=["/rack-a"],
      kafka_user="kafka",
      user_group="hadoop",
      conf_dir="/etc/kafka/conf",
      kafka_bare_jaas_principal="kafka" if secure else None,
      kafka_env_sh_template="export JAVA_HOME=...",
      log4j_props=None,
      kafka_jaas_conf_template="KafkaServer { ... };" if secure else None,
      kafka_client_jaas_conf_template="KafkaClient { ... };" if secure else None,
      limits_conf_dir="/etc/security/limits.d",
      kafka_log_dir="/var/log/kafka",
      kafka_pid_dir="/var/run/kafka",
    )

  def _configure(self, secure=False):
    params = self._params(secure)
    patches = (
      patch.dict(sys.modules, {"params": params}),
      patch.object(KAFKA_CONFIG, "check_stack_feature", return_value=True),
      patch.object(KAFKA_CONFIG, "Directory"),
      patch.object(KAFKA_CONFIG, "PropertiesFile"),
      patch.object(KAFKA_CONFIG, "File"),
      patch.object(KAFKA_CONFIG, "TemplateConfig"),
      patch.object(KAFKA_CONFIG, "InlineTemplate", side_effect=lambda value: value),
      patch.object(KAFKA_CONFIG, "Template"),
      patch.object(KAFKA_CONFIG, "generate_logfeeder_input_config"),
      patch.object(KAFKA_CONFIG.sudo, "path_lexists", return_value=False),
    )
    entered = [context.__enter__() for context in patches]
    try:
      KAFKA_CONFIG.kafka()
    finally:
      for context in reversed(patches):
        context.__exit__(None, None, None)
    return params, entered

  def test_server_client_and_data_resources_have_restrictive_permissions(self):
    params, entered = self._configure(secure=False)
    _, _, directory, properties_file, file_resource, *_ = entered

    data_call = next(
      call for call in directory.call_args_list
      if call.args and call.args[0] == ["/data/kafka-a", "/data/kafka-b"]
    )
    self.assertEqual(0o750, data_call.kwargs["mode"])
    self.assertNotIn("recursive_ownership", data_call.kwargs)

    server_call, client_call = properties_file.call_args_list
    self.assertEqual("server.properties", server_call.args[0])
    self.assertEqual(0o640, server_call.kwargs["mode"])
    self.assertEqual("/rack-a", server_call.kwargs["properties"]["broker.rack"])
    self.assertEqual("kafka-client.properties", client_call.args[0])
    self.assertEqual(0o600, client_call.kwargs["mode"])
    self.assertEqual(
      {"security.protocol": "PLAINTEXT"}, client_call.kwargs["properties"]
    )

    env_call = next(
      call for call in file_resource.call_args_list
      if call.args and call.args[0] == "/etc/kafka/conf/kafka-env.sh"
    )
    self.assertEqual("kafka", env_call.kwargs["owner"])
    self.assertEqual("hadoop", env_call.kwargs["group"])
    self.assertEqual(0o640, env_call.kwargs["mode"])
    deleted_paths = {
      call.args[0]
      for call in file_resource.call_args_list
      if call.kwargs.get("action") == "delete"
    }
    self.assertEqual(
      {
        "/etc/kafka/conf/kafka_jaas.conf",
        "/etc/kafka/conf/kafka_client_jaas.conf",
      },
      deleted_paths,
    )
    self.assertEqual("broker1.example.com", params.hostname)

  def test_secure_configuration_creates_broker_and_client_jaas_as_0600(self):
    _, entered = self._configure(secure=True)
    file_resource = entered[4]
    jaas_calls = [
      call for call in file_resource.call_args_list
      if call.args and call.args[0].endswith("jaas.conf")
    ]
    self.assertEqual(2, len(jaas_calls))
    for call in jaas_calls:
      self.assertEqual("kafka", call.kwargs["owner"])
      self.assertEqual("hadoop", call.kwargs["group"])
      self.assertEqual(0o600, call.kwargs["mode"])
      self.assertNotEqual("delete", call.kwargs.get("action"))

  def test_data_directory_and_rack_validation_fail_closed(self):
    for value in (
      None,
      "",
      "relative",
      "/",
      "/data/../tmp",
      "/data,/data",
      "/etc/kafka-data",
      "/run/kafka-data",
    ):
      with self.subTest(value=value):
        with self.assertRaises(Fail):
          KAFKA_CONFIG.validate_data_directories(value)
    self.assertEqual(
      ["/data/a", "/data/b"],
      KAFKA_CONFIG.validate_data_directories("/data/a, /data/b"),
    )

  def test_bare_principal_requires_a_complete_valid_principal(self):
    self.assertEqual("kafka", KAFKA_UTILS.get_bare_principal("kafka/_HOST@REALM"))
    self.assertEqual("kafka", KAFKA_UTILS.get_bare_principal("kafka@REALM"))
    self.assertIsNone(KAFKA_UTILS.get_bare_principal(None))
    self.assertIsNone(KAFKA_UTILS.get_bare_principal("kafka/@REALM"))


class TestKafkaServiceCheck(unittest.TestCase):
  def _params(self, kerberos=False, delete_topic=True):
    return SimpleNamespace(
      kafka_delete_topic_enable=delete_topic,
      kafka_service_check_uses_kerberos=kerberos,
      kafka_service_check_uses_sasl=kerberos,
      kerberos_security_enabled=kerberos,
      kafka_keytab_path="/etc/security/key tabs/kafka;$(id)" if kerberos else None,
      kafka_jaas_principal="kafka/broker@REALM;$(id)" if kerberos else None,
      kafka_user="kafka",
      user_group="hadoop",
      java64_home="/usr/lib/jvm/java-17;$(id)",
      kafka_log_dir="/var/log/kafka;$(id)",
      kafka_client_jaas_file="/etc/kafka/conf/client jaas;$(id)",
      kinit_path_local="/usr/bin/kinit;$(id)",
      kafka_topics="/usr/lib/kafka/bin/kafka-topics.sh;$(id)",
      kafka_bootstrap_servers="broker1:9092,broker2:9092;$(id)",
      kafka_client_properties="/etc/kafka/conf/client props;$(id)",
      kafka_service_check_timeout=60,
    )

  def test_check_uses_kafka_341_bootstrap_and_command_config_argv(self):
    params = self._params()
    results = (
      (0, ""),
      (0, "Topic: ambari_kafka_service_check_fixed PartitionCount: 1"),
      (0, ""),
      (0, ""),
    )
    with (
      patch.dict(sys.modules, {"params": params}),
      patch.object(
        KAFKA_SERVICE_CHECK.uuid,
        "uuid4",
        return_value=SimpleNamespace(hex="fixed"),
      ),
      patch.object(
        KAFKA_SERVICE_CHECK.shell, "checked_call", side_effect=results
      ) as checked_call,
    ):
      KAFKA_SERVICE_CHECK.ServiceCheck().service_check(MagicMock())

    self.assertEqual(4, checked_call.call_count)
    for call in checked_call.call_args_list:
      command = call.args[0]
      self.assertIsInstance(command, tuple)
      self.assertEqual("--bootstrap-server", command[1])
      self.assertEqual(params.kafka_bootstrap_servers, command[2])
      self.assertEqual("--command-config", command[3])
      self.assertEqual(params.kafka_client_properties, command[4])
      self.assertNotIn("--zookeeper", command)
    self.assertIn("--create", checked_call.call_args_list[0].args[0])
    self.assertIn("--under-replicated-partitions", checked_call.call_args_list[2].args[0])
    self.assertIn("--delete", checked_call.call_args_list[3].args[0])

  def test_describe_and_under_replicated_failures_still_delete_unique_topic(self):
    for primary_failure, results in (
      (
        Fail("describe failed"),
        ((0, ""), Fail("describe failed"), (0, "")),
      ),
      (
        Fail("under replicated"),
        (
          (0, ""),
          (0, "Topic: ambari_kafka_service_check_fixed PartitionCount: 1"),
          (0, "Topic: ambari_kafka_service_check_fixed Partition: 0"),
          (0, ""),
        ),
      ),
    ):
      with self.subTest(primary_failure=primary_failure):
        params = self._params()
        with (
          patch.dict(sys.modules, {"params": params}),
          patch.object(
            KAFKA_SERVICE_CHECK.uuid,
            "uuid4",
            return_value=SimpleNamespace(hex="fixed"),
          ),
          patch.object(
            KAFKA_SERVICE_CHECK.shell, "checked_call", side_effect=results
          ) as checked_call,
        ):
          with self.assertRaises(Fail):
            KAFKA_SERVICE_CHECK.ServiceCheck().service_check(MagicMock())
        self.assertIn("--delete", checked_call.call_args_list[-1].args[0])

  def test_cleanup_failure_does_not_mask_primary_but_fails_successful_check(self):
    primary = Fail("describe failed")
    cleanup = Fail("delete failed")
    params = self._params()
    with (
      patch.dict(sys.modules, {"params": params}),
      patch.object(
        KAFKA_SERVICE_CHECK.uuid,
        "uuid4",
        return_value=SimpleNamespace(hex="fixed"),
      ),
      patch.object(
        KAFKA_SERVICE_CHECK.shell,
        "checked_call",
        side_effect=((0, ""), primary, cleanup),
      ),
      patch.object(KAFKA_SERVICE_CHECK.Logger, "error") as log_error,
    ):
      with self.assertRaises(Fail) as raised:
        KAFKA_SERVICE_CHECK.ServiceCheck().service_check(MagicMock())
    self.assertIs(primary, raised.exception)
    log_error.assert_called_once()

    successful_results = (
      (0, ""),
      (0, "Topic: ambari_kafka_service_check_fixed PartitionCount: 1"),
      (0, ""),
      cleanup,
    )
    with (
      patch.dict(sys.modules, {"params": params}),
      patch.object(
        KAFKA_SERVICE_CHECK.uuid,
        "uuid4",
        return_value=SimpleNamespace(hex="fixed"),
      ),
      patch.object(
        KAFKA_SERVICE_CHECK.shell,
        "checked_call",
        side_effect=successful_results,
      ),
    ):
      with self.assertRaises(Fail) as raised:
        KAFKA_SERVICE_CHECK.ServiceCheck().service_check(MagicMock())
    self.assertIs(cleanup, raised.exception)

  def test_kerberos_check_uses_unique_private_cache_and_finally_context(self):
    params = self._params(kerberos=True, delete_topic=False)
    cache = MagicMock()
    cache.merge_environment.return_value = {
      "JAVA_HOME": params.java64_home,
      "LOG_DIR": params.kafka_log_dir,
      "KAFKA_OPTS": (
        "-Djava.security.auth.login.config=" + params.kafka_client_jaas_file
      ),
      "KRB5CCNAME": "FILE:/tmp/ambari-kafka/cache/krb5cc",
    }
    context = MagicMock()
    context.__enter__.return_value = cache
    results = (
      (0, ""),
      (0, "Topic: ambari_kafka_service_check PartitionCount: 1"),
      (0, ""),
    )
    with (
      patch.dict(sys.modules, {"params": params}),
      patch.object(
        KAFKA_SERVICE_CHECK, "PrivateKerberosCache", return_value=context
      ) as cache_factory,
      patch.object(
        KAFKA_SERVICE_CHECK.shell, "checked_call", side_effect=results
      ) as checked_call,
    ):
      KAFKA_SERVICE_CHECK.ServiceCheck().service_check(MagicMock())

    cache_factory.assert_called_once_with(
      "kafka", "hadoop", prefix="ambari-kafka-service-check-"
    )
    cache.kinit.assert_called_once_with(
      params.kinit_path_local,
      params.kafka_keytab_path,
      params.kafka_jaas_principal,
      timeout=60,
    )
    self.assertEqual(
      cache.merge_environment.return_value,
      checked_call.call_args.kwargs["env"],
    )
    context.__exit__.assert_called_once()

  def test_missing_kerberos_credentials_fail_before_cache_or_kafka_command(self):
    params = self._params(kerberos=True)
    params.kafka_keytab_path = None
    with (
      patch.dict(sys.modules, {"params": params}),
      patch.object(KAFKA_SERVICE_CHECK, "PrivateKerberosCache") as cache_factory,
      patch.object(KAFKA_SERVICE_CHECK.shell, "checked_call") as checked_call,
    ):
      with self.assertRaisesRegex(Fail, "credentials"):
        KAFKA_SERVICE_CHECK.ServiceCheck().service_check(MagicMock())
    cache_factory.assert_not_called()
    checked_call.assert_not_called()


class TestKafkaAdvisorAndMetadata(unittest.TestCase):
  @classmethod
  def setUpClass(cls):
    cls.advisor = load_module(
      "bigtop_kafka_service_advisor", KAFKA / "service_advisor.py"
    )

  def test_kerberos_detection_accepts_legacy_protocol_but_requires_gssapi(self):
    services = {
      "configurations": {
        "kafka-broker": {
          "properties": {
            "security.inter.broker.protocol": "PLAINTEXTSASL",
            "sasl.mechanism.inter.broker.protocol": "GSSAPI",
          }
        }
      }
    }
    self.assertTrue(
      self.advisor.KafkaServiceAdvisor.isKerberosEnabled(services, {})
    )
    updated = {
      "kafka-broker": {
        "properties": {"sasl.mechanism.inter.broker.protocol": "SCRAM-SHA-512"}
      }
    }
    self.assertFalse(
      self.advisor.KafkaServiceAdvisor.isKerberosEnabled(services, updated)
    )

    custom_listener = {
      "kafka-broker": {
        "properties": {
          "listeners": "INTERNAL://:19092",
          "listener.security.protocol.map": "INTERNAL:SASL_SSL",
          "inter.broker.listener.name": "INTERNAL",
          "sasl.mechanism.inter.broker.protocol": "GSSAPI",
        }
      }
    }
    self.assertTrue(
      self.advisor.KafkaServiceAdvisor.isKerberosEnabled({}, custom_listener)
    )

  def test_listener_recommendation_prefers_updates_and_migrates_custom_map(self):
    recommender = object.__new__(self.advisor.KafkaRecommender)
    services = {
      "configurations": {
        "kafka-broker": {"properties": {"listeners": "PLAINTEXT://:9092"}}
      }
    }
    configurations = {
      "kafka-broker": {
        "properties": {
          "listeners": "INTERNAL://:19092",
          "listener.security.protocol.map": "INTERNAL:PLAINTEXT",
        }
      }
    }
    put_property = MagicMock()
    recommender.update_listeners_to_sasl(
      services, configurations, put_property
    )
    put_property.assert_any_call("listeners", "INTERNAL://:19092")
    put_property.assert_any_call(
      "listener.security.protocol.map", "INTERNAL:SASL_PLAINTEXT"
    )

  def test_broker_count_uses_named_component_instead_of_array_position(self):
    services = {
      "services": [
        {
          "StackServices": {"service_name": "KAFKA"},
          "components": [
            {
              "StackServiceComponents": {
                "component_name": "KAFKA_CLIENT",
                "hostnames": [],
              }
            },
            {
              "StackServiceComponents": {
                "component_name": "KAFKA_BROKER",
                "hostnames": ["broker1", "broker2"],
              }
            },
          ],
        }
      ]
    }
    self.assertEqual(2, self.advisor._kafka_broker_count(services))

  def test_versions_preserve_32_history_and_match_bigtop_33_kafka_341(self):
    base = ElementTree.parse(KAFKA / "metainfo.xml").getroot()
    overlay = ElementTree.parse(KAFKA_33 / "metainfo.xml").getroot()
    self.assertEqual("2.8.1-2", base.findtext("./services/service/version"))
    self.assertEqual("3.4.1-1", overlay.findtext("./services/service/version"))

  def test_removed_kafka_341_properties_remain_deleted_for_upgrade_cleanup(self):
    root = ElementTree.parse(KAFKA / "configuration/kafka-broker.xml").getroot()
    properties = {
      node.findtext("name"): node for node in root.findall("property")
    }
    for name in (
      "controller.message.queue.size",
      "replica.lag.max.messages",
      "zookeeper.sync.time.ms",
    ):
      self.assertEqual("true", properties[name].findtext("deleted"))
      self.assertEqual("false", properties[name].find("on-ambari-upgrade").get("add"))
    self.assertNotIn("producer.metrics.enable", properties)
    self.assertEqual("10080", properties["offsets.retention.minutes"].findtext("value"))

  def test_kerberos_descriptor_uses_kafka_341_acl_authorizer(self):
    descriptor = json.loads((KAFKA / "kerberos.json").read_text(encoding="utf-8"))
    source = json.dumps(descriptor)
    self.assertIn("kafka.security.authorizer.AclAuthorizer", source)
    self.assertIn("User:${kafka-env/kafka_user}", source)
    self.assertNotIn("SimpleAclAuthorizer", source)

  def test_shell_template_quotes_configured_values(self):
    root = ElementTree.parse(KAFKA / "configuration/kafka-env.xml").getroot()
    content = next(
      node.findtext("value")
      for node in root.findall("property")
      if node.findtext("name") == "content"
    )
    for assignment in (
      "JAVA_HOME={{java64_home_shell}}",
      "PID_DIR={{kafka_pid_dir_shell}}",
      "LOG_DIR={{kafka_log_dir_shell}}",
      "KAFKA_OPTS={{kafka_opts_shell}}",
    ):
      self.assertIn(assignment, content)

  def test_json_and_jaas_templates_use_encoded_dynamic_values(self):
    logfeeder = (KAFKA / "package/templates/input.config-kafka.json.j2").read_text(
      encoding="utf-8"
    )
    self.assertEqual(5, logfeeder.count("json.dumps("))
    self.assertNotIn('"path":"{{default(', logfeeder)

    for path in (
      KAFKA / "configuration/kafka_jaas_conf.xml",
      KAFKA / "configuration/kafka_client_jaas_conf.xml",
      KAFKA / "package/templates/kafka_jaas.conf.j2",
      KAFKA / "package/templates/kafka_client_jaas.conf.j2",
    ):
      source = path.read_text(encoding="utf-8")
      self.assertIn("kafka_kerberos_credentials_enabled", source)
      self.assertNotIn('"{{kafka_keytab_path}}"', source)
      self.assertNotIn('"{{kafka_jaas_principal}}"', source)
      self.assertNotIn('"{{kafka_bare_jaas_principal}}"', source)

    client_template = (
      KAFKA / "package/templates/kafka_client_jaas.conf.j2"
    ).read_text(encoding="utf-8")
    self.assertNotIn("\nClient {", client_template)

  def test_historical_upgrade_entrypoint_and_unsafe_lifecycle_are_removed(self):
    self.assertFalse((SCRIPTS / "upgrade.py").exists())
    source = "\n".join(
      path.read_text(encoding="utf-8") for path in SCRIPTS.glob("*.py")
    )
    for obsolete in (
      "SimpleAclAuthorizer",
      "kafka-server-stop.sh",
      "kafka_start_cmd",
      "kafka_stop_cmd",
      "ps -p",
      "sudo.kill",
      "producer.metrics.enable",
    ):
      self.assertNotIn(obsolete, source)
    self.assertNotIn("--zookeeper", (SCRIPTS / "service_check.py").read_text())

  def test_ranger_files_and_hdfs_audit_directory_are_restrictive(self):
    source = (SCRIPTS / "setup_ranger_kafka.py").read_text(encoding="utf-8")
    self.assertIn("content=StaticFile(params.setup_ranger_env_sh_source)", source)
    self.assertIn("mode=0o640", source)
    self.assertIn('group=params.user_group,\n        mode=0o700', source)
    self.assertIn('os.path.join(params.conf_dir, "kafka-ranger-env.sh")', source)
    self.assertNotIn("recursive_ownership", source)
    self.assertNotIn("except:", source)

  def test_parent_advisor_loading_does_not_swallow_errors(self):
    advisor_path = KAFKA / "service_advisor.py"
    spec = importlib.util.spec_from_file_location(
      "bigtop_kafka_broken_parent_advisor", advisor_path
    )
    module = importlib.util.module_from_spec(spec)
    with (
      patch.dict(os.environ, {"BASE_SERVICE_ADVISOR": "/missing/advisor.py"}),
      patch("builtins.open", side_effect=OSError("parent unavailable")),
    ):
      with self.assertRaisesRegex(OSError, "parent unavailable"):
        spec.loader.exec_module(module)


if __name__ == "__main__":
  unittest.main()
