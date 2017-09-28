#!/usr/bin/env ambari-python-wrap
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
import imp
import os
import traceback
import re
import socket
import fnmatch


from resource_management.core.logger import Logger

SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
STACKS_DIR = os.path.join(SCRIPT_DIR, '../../../stacks/')
PARENT_FILE = os.path.join(STACKS_DIR, 'service_advisor.py')

try:
  with open(PARENT_FILE, 'rb') as fp:
    service_advisor = imp.load_module('service_advisor', fp, PARENT_FILE, ('.py', 'rb', imp.PY_SOURCE))
except Exception as e:
  traceback.print_exc()
  print "Failed to load parent"

class KafkaServiceAdvisor(service_advisor.ServiceAdvisor):

  def __init__(self, *args, **kwargs):
    self.as_super = super(KafkaServiceAdvisor, self)
    self.as_super.__init__(*args, **kwargs)

    # Always call these methods
    self.modifyMastersWithMultipleInstances()
    self.modifyCardinalitiesDict()
    self.modifyHeapSizeProperties()
    self.modifyNotValuableComponents()
    self.modifyComponentsNotPreferableOnServer()
    self.modifyComponentLayoutSchemes()

  def modifyMastersWithMultipleInstances(self):
    """
    Modify the set of masters with multiple instances.
    Must be overriden in child class.
    """
    # Nothing to do
    pass

  def modifyCardinalitiesDict(self):
    """
    Modify the dictionary of cardinalities.
    Must be overriden in child class.
    """
    # Nothing to do
    pass

  def modifyHeapSizeProperties(self):
    """
    Modify the dictionary of heap size properties.
    Must be overriden in child class.
    """
    pass

  def modifyNotValuableComponents(self):
    """
    Modify the set of components whose host assignment is based on other services.
    Must be overriden in child class.
    """
    # Nothing to do
    pass

  def modifyComponentsNotPreferableOnServer(self):
    """
    Modify the set of components that are not preferable on the server.
    Must be overriden in child class.
    """
    # Nothing to do
    pass

  def modifyComponentLayoutSchemes(self):
    """
    Modify layout scheme dictionaries for components.
    The scheme dictionary basically maps the number of hosts to
    host index where component should exist.
    Must be overriden in child class.
    """
    # Nothing to do
    pass

  def getServiceComponentLayoutValidations(self, services, hosts):
    """
    Get a list of errors.
    Must be overriden in child class.
    """

    return []

  def getServiceConfigurationRecommendations(self, configurations, clusterData, services, hosts):
    """
    Entry point.
    Must be overriden in child class.
    """
    #Logger.info("Class: %s, Method: %s. Recommending Service Configurations." %
    #            (self.__class__.__name__, inspect.stack()[0][3]))

    recommender = KafkaRecommender()
    recommender.recommendKafkaConfigurationsFromHDP22(configurations, clusterData, services, hosts)
    recommender.recommendKAFKAConfigurationsFromHDP23(configurations, clusterData, services, hosts)
    recommender.recommendKAFKAConfigurationsFromHDP26(configurations, clusterData, services, hosts)


  def getServiceConfigurationsValidationItems(self, configurations, recommendedDefaults, services, hosts):
    """
    Entry point.
    Validate configurations for the service. Return a list of errors.
    The code for this function should be the same for each Service Advisor.
    """
    #Logger.info("Class: %s, Method: %s. Validating Configurations." %
    #            (self.__class__.__name__, inspect.stack()[0][3]))

    validator = KafkaValidator()
    # Calls the methods of the validator using arguments,
    # method(siteProperties, siteRecommendations, configurations, services, hosts)
    return validator.validateListOfConfigUsingMethod(configurations, recommendedDefaults, services, hosts, validator.validators)



class KafkaRecommender(service_advisor.ServiceAdvisor):
  """
  Kafka Recommender suggests properties when adding the service for the first time or modifying configs via the UI.
  """

  def __init__(self, *args, **kwargs):
    self.as_super = super(KafkaRecommender, self)
    self.as_super.__init__(*args, **kwargs)



  def recommendKafkaConfigurationsFromHDP22(self, configurations, clusterData, services, hosts):
    kafka_mounts = [
      ("log.dirs", "KAFKA_BROKER", "/kafka-logs", "multi")
    ]

    self.updateMountProperties("kafka-broker", kafka_mounts, configurations, services, hosts)


  def recommendKAFKAConfigurationsFromHDP23(self, configurations, clusterData, services, hosts):

    servicesList = [service["StackServices"]["service_name"] for service in services["services"]]
    kafka_broker = self.getServicesSiteProperties(services, "kafka-broker")

    security_enabled = self.isSecurityEnabled(services)

    putKafkaBrokerProperty = self.putProperty(configurations, "kafka-broker", services)
    putKafkaLog4jProperty = self.putProperty(configurations, "kafka-log4j", services)
    putKafkaBrokerAttributes = self.putPropertyAttribute(configurations, "kafka-broker")

    if security_enabled:
      kafka_env = slef.getServicesSiteProperties(services, "kafka-env")
      kafka_user = kafka_env.get('kafka_user') if kafka_env is not None else None

      if kafka_user is not None:
        kafka_super_users = kafka_broker.get('super.users') if kafka_broker is not None else None

        # kafka_super_super_users is expected to be formatted as:  User:user1;User:user2
        if kafka_super_users is not None and kafka_super_users != '':
          # Parse kafka_super_users to get a set of unique user names and rebuild the property value
          user_names = set()
          user_names.add(kafka_user)
          for match in re.findall('User:([^;]*)', kafka_super_users):
            user_names.add(match)
          kafka_super_users = 'User:' + ";User:".join(user_names)
        else:
          kafka_super_users = 'User:' + kafka_user

        putKafkaBrokerProperty("super.users", kafka_super_users)

      putKafkaBrokerProperty("principal.to.local.class", "kafka.security.auth.KerberosPrincipalToLocal")
      putKafkaBrokerProperty("security.inter.broker.protocol", "PLAINTEXTSASL")
      putKafkaBrokerProperty("zookeeper.set.acl", "true")

    else:  # not security_enabled
      # remove unneeded properties
      putKafkaBrokerAttributes('super.users', 'delete', 'true')
      putKafkaBrokerAttributes('principal.to.local.class', 'delete', 'true')
      putKafkaBrokerAttributes('security.inter.broker.protocol', 'delete', 'true')

    # Update ranger-kafka-plugin-properties/ranger-kafka-plugin-enabled to match ranger-env/ranger-kafka-plugin-enabled
    if "ranger-env" in services["configurations"] \
      and "ranger-kafka-plugin-properties" in services["configurations"] \
      and "ranger-kafka-plugin-enabled" in services["configurations"]["ranger-env"]["properties"]:
      putKafkaRangerPluginProperty = self.putProperty(configurations, "ranger-kafka-plugin-properties", services)
      ranger_kafka_plugin_enabled = services["configurations"]["ranger-env"]["properties"]["ranger-kafka-plugin-enabled"]
      putKafkaRangerPluginProperty("ranger-kafka-plugin-enabled", ranger_kafka_plugin_enabled)


    ranger_plugin_enabled = False
    # Only if the RANGER service is installed....
    if "RANGER" in servicesList:
      # If ranger-kafka-plugin-properties/ranger-kafka-plugin-enabled,
      # determine if the Ranger/Kafka plug-in enabled enabled or not
      if 'ranger-kafka-plugin-properties' in configurations and \
                      'ranger-kafka-plugin-enabled' in configurations['ranger-kafka-plugin-properties']['properties']:
        ranger_plugin_enabled = configurations['ranger-kafka-plugin-properties']['properties']['ranger-kafka-plugin-enabled'].lower() == 'yes'
      # If ranger-kafka-plugin-properties/ranger-kafka-plugin-enabled was not changed,
      # determine if the Ranger/Kafka plug-in enabled enabled or not
      elif 'ranger-kafka-plugin-properties' in services['configurations'] and \
                      'ranger-kafka-plugin-enabled' in services['configurations']['ranger-kafka-plugin-properties']['properties']:
        ranger_plugin_enabled = services['configurations']['ranger-kafka-plugin-properties']['properties']['ranger-kafka-plugin-enabled'].lower() == 'yes'

    # Determine the value for kafka-broker/authorizer.class.name
    if ranger_plugin_enabled:
      # If the Ranger plugin for Kafka is enabled, set authorizer.class.name to
      # "org.apache.ranger.authorization.kafka.authorizer.RangerKafkaAuthorizer" whether Kerberos is
      # enabled or not.
      putKafkaBrokerProperty("authorizer.class.name", 'org.apache.ranger.authorization.kafka.authorizer.RangerKafkaAuthorizer')
    elif security_enabled:
      putKafkaBrokerProperty("authorizer.class.name", 'kafka.security.auth.SimpleAclAuthorizer')
    else:
      putKafkaBrokerAttributes('authorizer.class.name', 'delete', 'true')

    #If AMS is part of Services, use the KafkaTimelineMetricsReporter for metric reporting. Default is ''.
    if "AMBARI_METRICS" in servicesList:
      putKafkaBrokerProperty('kafka.metrics.reporters', 'org.apache.hadoop.metrics2.sink.kafka.KafkaTimelineMetricsReporter')

    if ranger_plugin_enabled:
      kafkaLog4jRangerLines = [{
                                 "name": "log4j.appender.rangerAppender",
                                 "value": "org.apache.log4j.DailyRollingFileAppender"
                               },
                               {
                                 "name": "log4j.appender.rangerAppender.DatePattern",
                                 "value": "'.'yyyy-MM-dd-HH"
                               },
                               {
                                 "name": "log4j.appender.rangerAppender.File",
                                 "value": "${kafka.logs.dir}/ranger_kafka.log"
                               },
                               {
                                 "name": "log4j.appender.rangerAppender.layout",
                                 "value": "org.apache.log4j.PatternLayout"
                               },
                               {
                                 "name": "log4j.appender.rangerAppender.layout.ConversionPattern",
                                 "value": "%d{ISO8601} %p [%t] %C{6} (%F:%L) - %m%n"
                               },
                               {
                                 "name": "log4j.logger.org.apache.ranger",
                                 "value": "INFO, rangerAppender"
                               }]

      # change kafka-log4j when ranger plugin is installed
      if 'kafka-log4j' in services['configurations'] and 'content' in services['configurations']['kafka-log4j']['properties']:
        kafkaLog4jContent = services['configurations']['kafka-log4j']['properties']['content']
        for item in range(len(kafkaLog4jRangerLines)):
          if kafkaLog4jRangerLines[item]["name"] not in kafkaLog4jContent:
            kafkaLog4jContent+= '\n' + kafkaLog4jRangerLines[item]["name"] + '=' + kafkaLog4jRangerLines[item]["value"]
        putKafkaLog4jProperty("content",kafkaLog4jContent)

      zookeeper_host_port = self.getZKHostPortString(services)
      if zookeeper_host_port:
        putRangerKafkaPluginProperty = self.putProperty(configurations, 'ranger-kafka-plugin-properties', services)
        putRangerKafkaPluginProperty('zookeeper.connect', zookeeper_host_port)


  def recommendKAFKAConfigurationsFromHDP26(self, configurations, clusterData, services, hosts):
    if 'kafka-env' in services['configurations'] and 'kafka_user' in services['configurations']['kafka-env']['properties']:
      kafka_user = services['configurations']['kafka-env']['properties']['kafka_user']
    else:
      kafka_user = "kafka"

    if 'ranger-kafka-plugin-properties' in configurations and  'ranger-kafka-plugin-enabled' in configurations['ranger-kafka-plugin-properties']['properties']:
      ranger_kafka_plugin_enabled = (configurations['ranger-kafka-plugin-properties']['properties']['ranger-kafka-plugin-enabled'].lower() == 'Yes'.lower())
    elif 'ranger-kafka-plugin-properties' in services['configurations'] and 'ranger-kafka-plugin-enabled' in services['configurations']['ranger-kafka-plugin-properties']['properties']:
      ranger_kafka_plugin_enabled = (services['configurations']['ranger-kafka-plugin-properties']['properties']['ranger-kafka-plugin-enabled'].lower() == 'Yes'.lower())
    else:
      ranger_kafka_plugin_enabled = False

    if ranger_kafka_plugin_enabled and 'ranger-kafka-plugin-properties' in services['configurations'] and 'REPOSITORY_CONFIG_USERNAME' in services['configurations']['ranger-kafka-plugin-properties']['properties']:
      self.logger.info("Setting Kafka Repo user for Ranger.")
      putRangerKafkaPluginProperty = self.putProperty(configurations, "ranger-kafka-plugin-properties", services)
      putRangerKafkaPluginProperty("REPOSITORY_CONFIG_USERNAME",kafka_user)
    else:
      self.logger.info("Not setting Kafka Repo user for Ranger.")


class KafkaValidator(service_advisor.ServiceAdvisor):
  """
  Kafka Validator checks the correctness of properties whenever the service is first added or the user attempts to
  change configs via the UI.
  """

  def __init__(self, *args, **kwargs):
    self.as_super = super(KafkaValidator, self)
    self.as_super.__init__(*args, **kwargs)

    self.validators = [("ranger-kafka-plugin-properties", self.validateKafkaRangerPluginConfigurationsFromHDP22),
                       ("kafka-broker", self.validateKAFKAConfigurationsFromHDP23)]

  def validateKafkaRangerPluginConfigurationsFromHDP22(self, properties, recommendedDefaults, configurations, services, hosts):
    validationItems = []
    ranger_plugin_properties = self.getSiteProperties(configurations, "ranger-kafka-plugin-properties")
    ranger_plugin_enabled = ranger_plugin_properties['ranger-kafka-plugin-enabled'] if ranger_plugin_properties else 'No'
    servicesList = [service["StackServices"]["service_name"] for service in services["services"]]
    security_enabled = self.isSecurityEnabled(services)
    if 'RANGER' in servicesList and ranger_plugin_enabled.lower() == 'yes':
      # ranger-hdfs-plugin must be enabled in ranger-env
      ranger_env = self.getServicesSiteProperties(services, 'ranger-env')
      if not ranger_env or not 'ranger-kafka-plugin-enabled' in ranger_env or \
                      ranger_env['ranger-kafka-plugin-enabled'].lower() != 'yes':
        validationItems.append({"config-name": 'ranger-kafka-plugin-enabled',
                                "item": self.getWarnItem(
                                  "ranger-kafka-plugin-properties/ranger-kafka-plugin-enabled must correspond ranger-env/ranger-kafka-plugin-enabled")})

    if ("RANGER" in servicesList) and (ranger_plugin_enabled.lower() == 'yes') and not security_enabled:
      validationItems.append({"config-name": "ranger-kafka-plugin-enabled",
                              "item": self.getWarnItem(
                                "Ranger Kafka plugin should not be enabled in non-kerberos environment.")})
    return self.toConfigurationValidationProblems(validationItems, "ranger-kafka-plugin-properties")


  def validateKAFKAConfigurationsFromHDP23(self, properties, recommendedDefaults, configurations, services, hosts):
    kafka_broker = properties
    validationItems = []
    servicesList = [service["StackServices"]["service_name"] for service in services["services"]]

    #Adding Ranger Plugin logic here
    ranger_plugin_properties = self.getSiteProperties(configurations, "ranger-kafka-plugin-properties")
    ranger_plugin_enabled = ranger_plugin_properties['ranger-kafka-plugin-enabled'] if ranger_plugin_properties else 'No'
    prop_name = 'authorizer.class.name'
    prop_val = "org.apache.ranger.authorization.kafka.authorizer.RangerKafkaAuthorizer"
    if ("RANGER" in servicesList) and (ranger_plugin_enabled.lower() == 'Yes'.lower()):
      if kafka_broker[prop_name] != prop_val:
        validationItems.append({"config-name": prop_name,
                                "item": self.getWarnItem(
                                  "If Ranger Kafka Plugin is enabled." \
                                  "{0} needs to be set to {1}".format(prop_name,prop_val))})

    if 'KERBEROS' in servicesList and 'security.inter.broker.protocol' in properties:
      interBrokerValue = properties['security.inter.broker.protocol']
      prop_name = 'listeners'
      prop_value =  properties[prop_name]
      if interBrokerValue and interBrokerValue not in prop_value:
        validationItems.append({"config-name": "listeners",
                                "item": self.getWarnItem("If kerberos is enabled " \
                                                         "{0}  need to contain {1} as one of " \
                                                         "the protocol".format(prop_name, interBrokerValue))})


    return self.toConfigurationValidationProblems(validationItems, "kafka-broker")



