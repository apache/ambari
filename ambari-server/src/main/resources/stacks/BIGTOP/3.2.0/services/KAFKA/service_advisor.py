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

# Python imports
from ambari_commons import import_utils
import os
import re

SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
STACKS_DIR = os.path.join(SCRIPT_DIR, "../../../../../stacks/")
PARENT_FILE = os.path.join(STACKS_DIR, "service_advisor.py")

if "BASE_SERVICE_ADVISOR" in os.environ:
  PARENT_FILE = os.environ["BASE_SERVICE_ADVISOR"]
with open(PARENT_FILE, "rb") as fp:
  service_advisor = import_utils.load_module(
    "service_advisor", fp, PARENT_FILE, (".py", "rb", import_utils.PY_SOURCE)
  )


def _service_names(services):
  return {
    service.get("StackServices", {}).get("service_name")
    for service in (services or {}).get("services", [])
  }


def _kafka_broker_count(services):
  for service in (services or {}).get("services", []):
    if service.get("StackServices", {}).get("service_name") != "KAFKA":
      continue
    for component in service.get("components", []):
      component_info = component.get("StackServiceComponents", {})
      if component_info.get("component_name") == "KAFKA_BROKER":
        return len(component_info.get("hostnames") or [])
  return 0


def _normalize_security_protocol(value):
  return str(value or "").upper().replace("PLAINTEXTSASL", "SASL_PLAINTEXT")


def _listener_protocols(properties):
  protocol_map = {}
  for mapping in properties.get("listener.security.protocol.map", "").split(","):
    name, separator, protocol = mapping.partition(":")
    if separator:
      protocol_map[name.strip().upper()] = _normalize_security_protocol(protocol)

  protocols = set()
  for listener in properties.get("listeners", "").split(","):
    name, separator, _ = listener.strip().partition("://")
    if separator:
      name = name.upper()
      protocols.add(protocol_map.get(name, _normalize_security_protocol(name)))
  return protocols


def _inter_broker_protocol(properties):
  listener_name = str(properties.get("inter.broker.listener.name", "")).upper()
  if listener_name:
    protocol_map = {}
    for mapping in properties.get("listener.security.protocol.map", "").split(","):
      name, separator, protocol = mapping.partition(":")
      if separator:
        protocol_map[name.strip().upper()] = _normalize_security_protocol(protocol)
    return protocol_map.get(
      listener_name, _normalize_security_protocol(listener_name)
    )
  return _normalize_security_protocol(
    properties.get("security.inter.broker.protocol", "PLAINTEXT")
  )


class KafkaServiceAdvisor(service_advisor.ServiceAdvisor):
  def getServiceComponentLayoutValidations(self, services, hosts):
    """
    Get a list of errors.
    Must be overriden in child class.
    """

    return self.getServiceComponentCardinalityValidations(services, hosts, "KAFKA")

  def getServiceConfigurationRecommendations(
    self, configurations, clusterData, services, hosts
  ):
    """
    Entry point.
    Must be overriden in child class.
    """
    # Logger.info("Class: %s, Method: %s. Recommending Service Configurations." %
    #            (self.__class__.__name__, inspect.stack()[0][3]))

    recommender = KafkaRecommender()
    recommender.recommendKafkaStorage(
      configurations, clusterData, services, hosts
    )
    recommender.recommendKafkaSecurity(
      configurations, clusterData, services, hosts
    )
    recommender.recommendRangerRepositoryUser(
      configurations, clusterData, services, hosts
    )
    recommender.recommendReplicationFactor(
      configurations, clusterData, services, hosts
    )

  def getServiceConfigurationsValidationItems(
    self, configurations, recommendedDefaults, services, hosts
  ):
    """
    Entry point.
    Validate configurations for the service. Return a list of errors.
    The code for this function should be the same for each Service Advisor.
    """
    # Logger.info("Class: %s, Method: %s. Validating Configurations." %
    #            (self.__class__.__name__, inspect.stack()[0][3]))

    validator = KafkaValidator()
    # Calls the methods of the validator using arguments,
    # method(siteProperties, siteRecommendations, configurations, services, hosts)
    return validator.validateListOfConfigUsingMethod(
      configurations, recommendedDefaults, services, hosts, validator.validators
    )

  @staticmethod
  def isKerberosEnabled(services, configurations):
    """
    Determine if Kerberos is enabled for Kafka.

    Kafka uses Kerberos when its inter-broker protocol is SASL-based.

    The value of this property is first tested in the updated configurations (configurations) then
    tested in the current configuration set (services)

    :type services: dict
    :param services: the dictionary containing the existing configuration values
    :type configurations: dict
    :param configurations: the dictionary containing the updated configuration values
    :rtype: bool
    :return: True or False
    """

    current = (
      services.get("configurations", {})
      .get("kafka-broker", {})
      .get("properties", {})
      if services
      else {}
    )
    updated = (
      configurations.get("kafka-broker", {}).get("properties", {})
      if configurations
      else {}
    )
    effective = dict(current)
    effective.update(updated)
    protocol = _inter_broker_protocol(effective)
    mechanism = str(
      updated.get(
        "sasl.mechanism.inter.broker.protocol",
        current.get("sasl.mechanism.inter.broker.protocol", "GSSAPI"),
      )
      or ""
    ).upper()
    return protocol in ("SASL_PLAINTEXT", "SASL_SSL") and mechanism == "GSSAPI"


class KafkaRecommender(service_advisor.ServiceAdvisor):
  """
  Kafka Recommender suggests properties when adding the service for the first time or modifying configs via the UI.
  """

  def __init__(self, *args, **kwargs):
    self.as_super = super(KafkaRecommender, self)
    self.as_super.__init__(*args, **kwargs)

  def recommendKafkaStorage(
    self, configurations, clusterData, services, hosts
  ):
    kafka_mounts = [("log.dirs", "KAFKA_BROKER", "/kafka-logs", "multi")]

    self.updateMountProperties(
      "kafka-broker", kafka_mounts, configurations, services, hosts
    )

  def recommendKafkaSecurity(
    self, configurations, clusterData, services, hosts
  ):
    kafka_broker = self.getServicesSiteProperties(services, "kafka-broker")
    kafka_env = self.getServicesSiteProperties(services, "kafka-env")

    if not kafka_env:  # Kafka check not required
      return

    security_enabled = KafkaServiceAdvisor.isKerberosEnabled(services, configurations)

    putKafkaBrokerProperty = self.putProperty(configurations, "kafka-broker", services)
    putKafkaLog4jProperty = self.putProperty(configurations, "kafka-log4j", services)
    putKafkaBrokerAttributes = self.putPropertyAttribute(configurations, "kafka-broker")

    if security_enabled:
      self.update_listeners_to_sasl(
        services, configurations, putKafkaBrokerProperty
      )

      kafka_user = kafka_env.get("kafka_user")

      if kafka_user is not None:
        kafka_super_users = (
          kafka_broker.get("super.users") if kafka_broker is not None else None
        )

        # kafka_super_super_users is expected to be formatted as:  User:user1;User:user2
        if kafka_super_users is not None and kafka_super_users != "":
          # Parse kafka_super_users to get a set of unique user names and rebuild the property value
          user_names = set()
          user_names.add(kafka_user)
          for match in re.findall(r"(?i)User:([^;]*)", kafka_super_users):
            if match:
              user_names.add(match)
          kafka_super_users = "User:" + ";User:".join(sorted(user_names))
        else:
          kafka_super_users = "User:" + kafka_user

        putKafkaBrokerProperty("super.users", kafka_super_users)

      putKafkaBrokerProperty(
        "principal.to.local.class", "kafka.security.auth.KerberosPrincipalToLocal"
      )
      putKafkaBrokerProperty("zookeeper.set.acl", "true")

    else:  # not security_enabled
      # remove unneeded properties
      putKafkaBrokerAttributes("super.users", "delete", "true")
      putKafkaBrokerAttributes("principal.to.local.class", "delete", "true")

    # Update ranger-kafka-plugin-properties/ranger-kafka-plugin-enabled to match ranger-env/ranger-kafka-plugin-enabled
    if (
      "ranger-env" in services["configurations"]
      and "ranger-kafka-plugin-properties" in services["configurations"]
      and "ranger-kafka-plugin-enabled"
      in services["configurations"]["ranger-env"]["properties"]
    ):
      putKafkaRangerPluginProperty = self.putProperty(
        configurations, "ranger-kafka-plugin-properties", services
      )
      ranger_kafka_plugin_enabled = services["configurations"]["ranger-env"][
        "properties"
      ]["ranger-kafka-plugin-enabled"]
      putKafkaRangerPluginProperty(
        "ranger-kafka-plugin-enabled", ranger_kafka_plugin_enabled
      )

    ranger_plugin_enabled = False
    if (
      "ranger-kafka-plugin-properties" in configurations
      and "ranger-kafka-plugin-enabled"
      in configurations["ranger-kafka-plugin-properties"]["properties"]
    ):
      ranger_plugin_enabled = (
        configurations["ranger-kafka-plugin-properties"]["properties"][
          "ranger-kafka-plugin-enabled"
        ].lower()
        == "yes"
      )
    elif (
      "ranger-kafka-plugin-properties" in services["configurations"]
      and "ranger-kafka-plugin-enabled"
      in services["configurations"]["ranger-kafka-plugin-properties"]["properties"]
    ):
      ranger_plugin_enabled = (
        services["configurations"]["ranger-kafka-plugin-properties"]["properties"][
          "ranger-kafka-plugin-enabled"
        ].lower()
        == "yes"
      )

    # Determine the value for kafka-broker/authorizer.class.name
    if ranger_plugin_enabled:
      # If the Ranger plugin for Kafka is enabled, set authorizer.class.name to
      # "org.apache.ranger.authorization.kafka.authorizer.RangerKafkaAuthorizer" whether Kerberos is
      # enabled or not.
      putKafkaBrokerProperty(
        "authorizer.class.name",
        "org.apache.ranger.authorization.kafka.authorizer.RangerKafkaAuthorizer",
      )
    elif security_enabled:
      putKafkaBrokerProperty(
        "authorizer.class.name", "kafka.security.authorizer.AclAuthorizer"
      )
    else:
      putKafkaBrokerAttributes("authorizer.class.name", "delete", "true")

    if ranger_plugin_enabled:
      kafkaLog4jRangerLines = [
        {
          "name": "log4j.appender.rangerAppender",
          "value": "org.apache.log4j.DailyRollingFileAppender",
        },
        {
          "name": "log4j.appender.rangerAppender.DatePattern",
          "value": "'.'yyyy-MM-dd-HH",
        },
        {
          "name": "log4j.appender.rangerAppender.File",
          "value": "${kafka.logs.dir}/ranger_kafka.log",
        },
        {
          "name": "log4j.appender.rangerAppender.layout",
          "value": "org.apache.log4j.PatternLayout",
        },
        {
          "name": "log4j.appender.rangerAppender.layout.ConversionPattern",
          "value": "%d{ISO8601} %p [%t] %C{6} (%F:%L) - %m%n",
        },
        {"name": "log4j.logger.org.apache.ranger", "value": "INFO, rangerAppender"},
      ]

      # change kafka-log4j when ranger plugin is installed
      if (
        "kafka-log4j" in services["configurations"]
        and "content" in services["configurations"]["kafka-log4j"]["properties"]
      ):
        kafkaLog4jContent = services["configurations"]["kafka-log4j"]["properties"][
          "content"
        ]
        for item in range(len(kafkaLog4jRangerLines)):
          if kafkaLog4jRangerLines[item]["name"] not in kafkaLog4jContent:
            kafkaLog4jContent += (
              "\n"
              + kafkaLog4jRangerLines[item]["name"]
              + "="
              + kafkaLog4jRangerLines[item]["value"]
            )
        putKafkaLog4jProperty("content", kafkaLog4jContent)

      zookeeper_host_port = self.getZKHostPortString(services)
      if zookeeper_host_port:
        putRangerKafkaPluginProperty = self.putProperty(
          configurations, "ranger-kafka-plugin-properties", services
        )
        putRangerKafkaPluginProperty("zookeeper.connect", zookeeper_host_port)

  def update_listeners_to_sasl(
    self, services, configurations, putKafkaBrokerProperty
  ):
    current = (
      services.get("configurations", {})
      .get("kafka-broker", {})
      .get("properties", {})
    )
    updated = configurations.get("kafka-broker", {}).get("properties", {})
    listeners = updated.get(
      "listeners", current.get("listeners")
    )
    if not listeners:
      raise ValueError("kafka-broker/listeners must be configured")
    listeners = re.sub(r"(^|\b)PLAINTEXT://", "SASL_PLAINTEXT://", listeners)
    listeners = re.sub(
      r"(^|\b)PLAINTEXTSASL://", "SASL_PLAINTEXT://", listeners
    )
    listeners = re.sub(r"(^|\b)SSL://", "SASL_SSL://", listeners)
    putKafkaBrokerProperty("listeners", listeners)

    listener_map = updated.get(
      "listener.security.protocol.map",
      current.get("listener.security.protocol.map", ""),
    )
    if listener_map:
      updated_mappings = []
      for mapping in listener_map.split(","):
        name, separator, protocol = mapping.partition(":")
        normalized = _normalize_security_protocol(protocol)
        if normalized == "PLAINTEXT":
          normalized = "SASL_PLAINTEXT"
        elif normalized == "SSL":
          normalized = "SASL_SSL"
        updated_mappings.append(
          f"{name.strip()}:{normalized}" if separator else mapping
        )
      putKafkaBrokerProperty(
        "listener.security.protocol.map", ",".join(updated_mappings)
      )

  def recommendRangerRepositoryUser(
    self, configurations, clusterData, services, hosts
  ):
    if (
      "kafka-env" in services["configurations"]
      and "kafka_user" in services["configurations"]["kafka-env"]["properties"]
    ):
      kafka_user = services["configurations"]["kafka-env"]["properties"]["kafka_user"]
    else:
      kafka_user = "kafka"

    if (
      "ranger-kafka-plugin-properties" in configurations
      and "ranger-kafka-plugin-enabled"
      in configurations["ranger-kafka-plugin-properties"]["properties"]
    ):
      ranger_kafka_plugin_enabled = (
        configurations["ranger-kafka-plugin-properties"]["properties"][
          "ranger-kafka-plugin-enabled"
        ].lower()
        == "Yes".lower()
      )
    elif (
      "ranger-kafka-plugin-properties" in services["configurations"]
      and "ranger-kafka-plugin-enabled"
      in services["configurations"]["ranger-kafka-plugin-properties"]["properties"]
    ):
      ranger_kafka_plugin_enabled = (
        services["configurations"]["ranger-kafka-plugin-properties"]["properties"][
          "ranger-kafka-plugin-enabled"
        ].lower()
        == "Yes".lower()
      )
    else:
      ranger_kafka_plugin_enabled = False

    if (
      ranger_kafka_plugin_enabled
      and "ranger-kafka-plugin-properties" in services["configurations"]
      and "REPOSITORY_CONFIG_USERNAME"
      in services["configurations"]["ranger-kafka-plugin-properties"]["properties"]
    ):
      self.logger.info("Setting Kafka Repo user for Ranger.")
      putRangerKafkaPluginProperty = self.putProperty(
        configurations, "ranger-kafka-plugin-properties", services
      )
      putRangerKafkaPluginProperty("REPOSITORY_CONFIG_USERNAME", kafka_user)
    else:
      self.logger.info("Not setting Kafka Repo user for Ranger.")

  def recommendReplicationFactor(
    self, configurations, clusterData, services, hosts
  ):
    num_kafka_brokers = _kafka_broker_count(services)
    if num_kafka_brokers:
      putKafkaBrokerProperty = self.putProperty(
        configurations, "kafka-broker", services
      )
      putKafkaBrokerProperty(
        "offsets.topic.replication.factor", str(min(3, num_kafka_brokers))
      )


class KafkaValidator(service_advisor.ServiceAdvisor):
  """
  Kafka Validator checks the correctness of properties whenever the service is first added or the user attempts to
  change configs via the UI.
  """

  def __init__(self, *args, **kwargs):
    self.as_super = super(KafkaValidator, self)
    self.as_super.__init__(*args, **kwargs)

    self.validators = [
      (
        "ranger-kafka-plugin-properties",
        self.validateRangerPlugin,
      ),
      ("kafka-broker", self.validateKafka),
    ]

  def validateRangerPlugin(
    self, properties, recommendedDefaults, configurations, services, hosts
  ):
    validationItems = []
    ranger_plugin_properties = self.getSiteProperties(
      configurations, "ranger-kafka-plugin-properties"
    )
    ranger_plugin_enabled = (
      ranger_plugin_properties["ranger-kafka-plugin-enabled"]
      if ranger_plugin_properties
      else "No"
    )
    servicesList = _service_names(services)
    security_enabled = KafkaServiceAdvisor.isKerberosEnabled(services, configurations)
    if ranger_plugin_enabled.lower() == "yes":
      repository_password = ranger_plugin_properties.get(
        "REPOSITORY_CONFIG_PASSWORD"
      )
      if not isinstance(repository_password, str) or not repository_password.strip():
        validationItems.append(
          {
            "config-name": "REPOSITORY_CONFIG_PASSWORD",
            "item": self.getErrorItem(
              "Ranger Kafka repository config password must not be empty when "
              "the plugin is enabled"
            ),
          }
        )
    if "RANGER" in servicesList and ranger_plugin_enabled.lower() == "yes":
      # ranger-hdfs-plugin must be enabled in ranger-env
      ranger_env = self.getServicesSiteProperties(services, "ranger-env")
      if (
        not ranger_env
        or not "ranger-kafka-plugin-enabled" in ranger_env
        or ranger_env["ranger-kafka-plugin-enabled"].lower() != "yes"
      ):
        validationItems.append(
          {
            "config-name": "ranger-kafka-plugin-enabled",
            "item": self.getWarnItem(
              "ranger-kafka-plugin-properties/ranger-kafka-plugin-enabled must correspond ranger-env/ranger-kafka-plugin-enabled"
            ),
          }
        )

    if ranger_plugin_enabled.lower() == "yes" and not security_enabled:
      validationItems.append(
        {
          "config-name": "ranger-kafka-plugin-enabled",
          "item": self.getWarnItem(
            "Ranger Kafka plugin should not be enabled in non-kerberos environment."
          ),
        }
      )
    return self.toConfigurationValidationProblems(
      validationItems, "ranger-kafka-plugin-properties"
    )

  def validateKafka(
    self, properties, recommendedDefaults, configurations, services, hosts
  ):
    kafka_broker = properties
    validationItems = []
    servicesList = _service_names(services)

    # Adding Ranger Plugin logic here
    ranger_plugin_properties = self.getSiteProperties(
      configurations, "ranger-kafka-plugin-properties"
    )
    ranger_plugin_enabled = (
      ranger_plugin_properties["ranger-kafka-plugin-enabled"]
      if ranger_plugin_properties
      else "No"
    )
    prop_name = "authorizer.class.name"
    prop_val = "org.apache.ranger.authorization.kafka.authorizer.RangerKafkaAuthorizer"
    if ranger_plugin_enabled.lower() == "yes":
      if not kafka_broker or kafka_broker.get(prop_name) != prop_val:
        validationItems.append(
          {
            "config-name": prop_name,
            "item": self.getWarnItem(
              "If Ranger Kafka Plugin is enabled." "{0} needs to be set to {1}".format(
                prop_name, prop_val
              )
            ),
          }
        )

    kafka_broker_properties = (
      self.getSiteProperties(configurations, "kafka-broker") or {}
    )
    num_kafka_brokers = _kafka_broker_count(services)
    if num_kafka_brokers:
      replication_factor = kafka_broker_properties.get(
        "offsets.topic.replication.factor"
      )
      try:
        replication_factor_value = int(replication_factor)
      except (TypeError, ValueError):
        validationItems.append(
          {
            "config-name": "offsets.topic.replication.factor",
            "item": self.getErrorItem(
              "offsets.topic.replication.factor must be a positive integer."
            ),
          }
        )
      else:
        if replication_factor_value < 1 or replication_factor_value > num_kafka_brokers:
          validationItems.append(
            {
              "config-name": "offsets.topic.replication.factor",
              "item": self.getErrorItem(
                "offsets.topic.replication.factor={0} must be between 1 and "
                "the number of Kafka brokers={1}.".format(
                  replication_factor,
                  num_kafka_brokers,
                )
              ),
            }
          )

    if "KERBEROS" in servicesList:
      interBrokerValue = _inter_broker_protocol(properties)
      prop_name = "listeners"
      if interBrokerValue and interBrokerValue not in _listener_protocols(properties):
        validationItems.append(
          {
            "config-name": "listeners",
            "item": self.getWarnItem(
              "If kerberos is enabled "
              "{0}  need to contain {1} as one of "
              "the protocol".format(prop_name, interBrokerValue)
            ),
          }
        )

    return self.toConfigurationValidationProblems(validationItems, "kafka-broker")
