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
from resource_management.libraries.functions.get_bare_principal import get_bare_principal

SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
STACKS_DIR = os.path.join(SCRIPT_DIR, '../../../stacks/')
PARENT_FILE = os.path.join(STACKS_DIR, 'service_advisor.py')

try:
  with open(PARENT_FILE, 'rb') as fp:
    service_advisor = imp.load_module('service_advisor', fp, PARENT_FILE, ('.py', 'rb', imp.PY_SOURCE))
except Exception as e:
  traceback.print_exc()
  print "Failed to load parent"

class StormServiceAdvisor(service_advisor.ServiceAdvisor):

  def __init__(self, *args, **kwargs):
    self.as_super = super(StormServiceAdvisor, self)
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

    recommender = StormRecommender()
    recommender.recommendStormConfigurationsFromHDP206(configurations, clusterData, services, hosts)
    recommender.recommendStormConfigurationsFromHDP21(configurations, clusterData, services, hosts)
    recommender.recommendStormConfigurationsFromHDP22(configurations, clusterData, services, hosts)
    recommender.recommendStormConfigurationsFromHDP23(configurations, clusterData, services, hosts)
    recommender.recommendStormConfigurationsFromHDP25(configurations, clusterData, services, hosts)



  def getServiceConfigurationsValidationItems(self, configurations, recommendedDefaults, services, hosts):
    """
    Entry point.
    Validate configurations for the service. Return a list of errors.
    The code for this function should be the same for each Service Advisor.
    """
    #Logger.info("Class: %s, Method: %s. Validating Configurations." %
    #            (self.__class__.__name__, inspect.stack()[0][3]))

    validator = StormValidator()
    # Calls the methods of the validator using arguments,
    # method(siteProperties, siteRecommendations, configurations, services, hosts)
    return validator.validateListOfConfigUsingMethod(configurations, recommendedDefaults, services, hosts, validator.validators)



class StormRecommender(service_advisor.ServiceAdvisor):
  """
  Storm Recommender suggests properties when adding the service for the first time or modifying configs via the UI.
  """

  def __init__(self, *args, **kwargs):
    self.as_super = super(StormRecommender, self)
    self.as_super.__init__(*args, **kwargs)


  def recommendStormConfigurationsFromHDP206(self, configurations, clusterData, services, hosts):
    putStormSiteProperty = self.putProperty(configurations, "storm-site", services)
    servicesList = [service["StackServices"]["service_name"] for service in services["services"]]
    # Storm AMS integration
    if 'AMBARI_METRICS' in servicesList:
      putStormSiteProperty('metrics.reporter.register', 'org.apache.hadoop.metrics2.sink.storm.StormTimelineMetricsReporter')


  def recommendStormConfigurationsFromHDP21(self, configurations, clusterData, services, hosts):
    storm_mounts = [
      ("storm.local.dir", ["NODEMANAGER", "NIMBUS"], "/hadoop/storm", "single")
    ]

    self.updateMountProperties("storm-site", storm_mounts, configurations, services, hosts)


  def recommendStormConfigurationsFromHDP22(self, configurations, clusterData, services, hosts):
    putStormSiteProperty = self.putProperty(configurations, "storm-site", services)
    putStormSiteAttributes = self.putPropertyAttribute(configurations, "storm-site")
    storm_site = self.getServicesSiteProperties(services, "storm-site")
    security_enabled = self.isSecurityEnabled(services)
    if "ranger-env" in services["configurations"] and "ranger-storm-plugin-properties" in services["configurations"] and \
        "ranger-storm-plugin-enabled" in services["configurations"]["ranger-env"]["properties"]:
      putStormRangerPluginProperty = self.putProperty(configurations, "ranger-storm-plugin-properties", services)
      rangerEnvStormPluginProperty = services["configurations"]["ranger-env"]["properties"]["ranger-storm-plugin-enabled"]
      putStormRangerPluginProperty("ranger-storm-plugin-enabled", rangerEnvStormPluginProperty)

    rangerPluginEnabled = ''
    if 'ranger-storm-plugin-properties' in configurations and 'ranger-storm-plugin-enabled' in  configurations['ranger-storm-plugin-properties']['properties']:
      rangerPluginEnabled = configurations['ranger-storm-plugin-properties']['properties']['ranger-storm-plugin-enabled']
    elif 'ranger-storm-plugin-properties' in services['configurations'] and 'ranger-storm-plugin-enabled' in services['configurations']['ranger-storm-plugin-properties']['properties']:
      rangerPluginEnabled = services['configurations']['ranger-storm-plugin-properties']['properties']['ranger-storm-plugin-enabled']

    nonRangerClass = 'backtype.storm.security.auth.authorizer.SimpleACLAuthorizer'
    servicesList = [service["StackServices"]["service_name"] for service in services["services"]]
    rangerServiceVersion=''
    if 'RANGER' in servicesList:
      rangerServiceVersion = [service['StackServices']['service_version'] for service in services["services"] if service['StackServices']['service_name'] == 'RANGER'][0]

    if rangerServiceVersion and rangerServiceVersion == '0.4.0':
      rangerClass = 'com.xasecure.authorization.storm.authorizer.XaSecureStormAuthorizer'
    else:
      rangerClass = 'org.apache.ranger.authorization.storm.authorizer.RangerStormAuthorizer'
    # Cluster is kerberized
    if security_enabled:
      if rangerPluginEnabled and (rangerPluginEnabled.lower() == 'Yes'.lower()):
        putStormSiteProperty('nimbus.authorizer',rangerClass)
      else:
        putStormSiteProperty('nimbus.authorizer', nonRangerClass)
    else:
      putStormSiteAttributes('nimbus.authorizer', 'delete', 'true')


  def recommendStormConfigurationsFromHDP23(self, configurations, clusterData, services, hosts):
    putStormStartupProperty = self.putProperty(configurations, "storm-site", services)
    putStormEnvProperty = self.putProperty(configurations, "storm-env", services)
    servicesList = [service["StackServices"]["service_name"] for service in services["services"]]

    if "storm-site" in services["configurations"]:
      # atlas
      notifier_plugin_property = "storm.topology.submission.notifier.plugin.class"
      if notifier_plugin_property in services["configurations"]["storm-site"]["properties"] and \
         services["configurations"]["storm-site"]["properties"][notifier_plugin_property] is not None:

        notifier_plugin_value = services["configurations"]["storm-site"]["properties"][notifier_plugin_property]
      else:
        notifier_plugin_value = " "

      atlas_is_present = "ATLAS" in servicesList
      atlas_hook_class = "org.apache.atlas.storm.hook.StormAtlasHook"
      atlas_hook_is_set = atlas_hook_class in notifier_plugin_value
      enable_atlas_hook = False
      enable_external_atlas_for_storm = False

      if 'storm-atlas-application.properties' in services['configurations'] and 'enable.external.atlas.for.storm' in services['configurations']['storm-atlas-application.properties']['properties']:
        enable_external_atlas_for_storm = services['configurations']['storm-atlas-application.properties']['properties']['enable.external.atlas.for.storm'].lower() == "true"

      if atlas_is_present:
        putStormEnvProperty("storm.atlas.hook", "true")
      elif enable_external_atlas_for_storm:
        putStormEnvProperty("storm.atlas.hook", "true")
      else:
        putStormEnvProperty("storm.atlas.hook", "false")

      if 'storm-env' in configurations and 'storm.atlas.hook' in configurations['storm-env']['properties']:
        enable_atlas_hook = configurations['storm-env']['properties']['storm.atlas.hook'] == "true"
      elif 'storm-env' in services['configurations'] and 'storm.atlas.hook' in services['configurations']['storm-env']['properties']:
        enable_atlas_hook = services['configurations']['storm-env']['properties']['storm.atlas.hook'] == "true"

      if enable_atlas_hook and not atlas_hook_is_set:
        notifier_plugin_value = atlas_hook_class if notifier_plugin_value == " " else ",".join([notifier_plugin_value, atlas_hook_class])

      if not enable_atlas_hook and atlas_hook_is_set:
        application_classes = [item for item in notifier_plugin_value.split(",") if item != atlas_hook_class and item != " "]
        notifier_plugin_value = ",".join(application_classes) if application_classes else " "

      if notifier_plugin_value.strip() != "":
        putStormStartupProperty(notifier_plugin_property, notifier_plugin_value)
      else:
        putStormStartupPropertyAttribute = self.putPropertyAttribute(configurations, "storm-site")
        putStormStartupPropertyAttribute(notifier_plugin_property, 'delete', 'true')



  def recommendStormConfigurationsFromHDP25(self, configurations, clusterData, services, hosts):
    storm_site = self.getServicesSiteProperties(services, "storm-site")
    storm_env = self.getServicesSiteProperties(services, "storm-env")
    putStormSiteProperty = self.putProperty(configurations, "storm-site", services)
    putStormSiteAttributes = self.putPropertyAttribute(configurations, "storm-site")
    security_enabled = self.isSecurityEnabled(services)

    if storm_env and storm_site:
      if security_enabled:
        _storm_principal_name = storm_env['storm_principal_name'] if 'storm_principal_name' in storm_env else None
        storm_bare_jaas_principal = get_bare_principal(_storm_principal_name)
        if 'nimbus.impersonation.acl' in storm_site:
          storm_nimbus_impersonation_acl = storm_site["nimbus.impersonation.acl"]
          storm_nimbus_impersonation_acl.replace('{{storm_bare_jaas_principal}}', storm_bare_jaas_principal)
          putStormSiteProperty('nimbus.impersonation.acl', storm_nimbus_impersonation_acl)
      else:
        if 'nimbus.impersonation.acl' in storm_site:
          putStormSiteAttributes('nimbus.impersonation.acl', 'delete', 'true')
        if 'nimbus.impersonation.authorizer' in storm_site:
          putStormSiteAttributes('nimbus.impersonation.authorizer', 'delete', 'true')

    rangerPluginEnabled = ''
    if 'ranger-storm-plugin-properties' in configurations and 'ranger-storm-plugin-enabled' in  configurations['ranger-storm-plugin-properties']['properties']:
      rangerPluginEnabled = configurations['ranger-storm-plugin-properties']['properties']['ranger-storm-plugin-enabled']
    elif 'ranger-storm-plugin-properties' in services['configurations'] and 'ranger-storm-plugin-enabled' in services['configurations']['ranger-storm-plugin-properties']['properties']:
      rangerPluginEnabled = services['configurations']['ranger-storm-plugin-properties']['properties']['ranger-storm-plugin-enabled']

    storm_authorizer_class = 'org.apache.storm.security.auth.authorizer.SimpleACLAuthorizer'
    ranger_authorizer_class = 'org.apache.ranger.authorization.storm.authorizer.RangerStormAuthorizer'
    # Cluster is kerberized
    if security_enabled:
      if rangerPluginEnabled and (rangerPluginEnabled.lower() == 'Yes'.lower()):
        putStormSiteProperty('nimbus.authorizer',ranger_authorizer_class)
      else:
        putStormSiteProperty('nimbus.authorizer', storm_authorizer_class)
    else:
      putStormSiteAttributes('nimbus.authorizer', 'delete', 'true')

    servicesList = [service["StackServices"]["service_name"] for service in services["services"]]
    # Storm AMS integration
    if 'AMBARI_METRICS' in servicesList:
      putStormSiteProperty('storm.cluster.metrics.consumer.register', '[{"class": "org.apache.hadoop.metrics2.sink.storm.StormTimelineMetricsReporter"}]')
      putStormSiteProperty('topology.metrics.consumer.register',
                           '[{"class": "org.apache.hadoop.metrics2.sink.storm.StormTimelineMetricsSink", '
                           '"parallelism.hint": 1, '
                           '"whitelist": ["kafkaOffset\\\..+/", "__complete-latency", "__process-latency", '
                           '"__receive\\\.population$", "__sendqueue\\\.population$", "__execute-count", "__emit-count", '
                           '"__ack-count", "__fail-count", "memory/heap\\\.usedBytes$", "memory/nonHeap\\\.usedBytes$", '
                           '"GC/.+\\\.count$", "GC/.+\\\.timeMs$"]}]')
    else:
      putStormSiteProperty('storm.cluster.metrics.consumer.register', 'null')
      putStormSiteProperty('topology.metrics.consumer.register', 'null')


class StormValidator(service_advisor.ServiceAdvisor):
  """
  Storm Validator checks the correctness of properties whenever the service is first added or the user attempts to
  change configs via the UI.
  """

  def __init__(self, *args, **kwargs):
    self.as_super = super(StormValidator, self)
    self.as_super.__init__(*args, **kwargs)

    self.validators = [("storm-site", self.validateStormConfigurationsFromHDP206),
                       ("ranger-storm-plugin-properties", self.validateStormRangerPluginConfigurationsFromHDP22),
                       ("storm-site", self.validateStormConfigurationsFromHDP25)]



  def validateStormConfigurationsFromHDP206(self, properties, recommendedDefaults, configurations, services, hosts):
    validationItems = []
    servicesList = [service["StackServices"]["service_name"] for service in services["services"]]
    # Storm AMS integration
    if 'AMBARI_METRICS' in servicesList and "metrics.reporter.register" in properties and \
      "org.apache.hadoop.metrics2.sink.storm.StormTimelineMetricsReporter" not in properties.get("metrics.reporter.register"):

      validationItems.append({"config-name": 'metrics.reporter.register',
                              "item": self.getWarnItem(
                                "Should be set to org.apache.hadoop.metrics2.sink.storm.StormTimelineMetricsReporter to report the metrics to Ambari Metrics service.")})

    return self.toConfigurationValidationProblems(validationItems, "storm-site")


  def validateStormRangerPluginConfigurationsFromHDP22(self, properties, recommendedDefaults, configurations, services, hosts):
    validationItems = []
    ranger_plugin_properties = self.getSiteProperties(configurations, "ranger-storm-plugin-properties")
    ranger_plugin_enabled = ranger_plugin_properties['ranger-storm-plugin-enabled'] if ranger_plugin_properties else 'No'
    servicesList = [service["StackServices"]["service_name"] for service in services["services"]]
    security_enabled = self.isSecurityEnabled(services)
    if 'RANGER' in servicesList and ranger_plugin_enabled.lower() == 'yes':
      # ranger-hdfs-plugin must be enabled in ranger-env
      ranger_env = self.getServicesSiteProperties(services, 'ranger-env')
      if not ranger_env or not 'ranger-storm-plugin-enabled' in ranger_env or \
          ranger_env['ranger-storm-plugin-enabled'].lower() != 'yes':
        validationItems.append({"config-name": 'ranger-storm-plugin-enabled',
                                "item": self.getWarnItem(
                                  "ranger-storm-plugin-properties/ranger-storm-plugin-enabled must correspond ranger-env/ranger-storm-plugin-enabled")})
    if ("RANGER" in servicesList) and (ranger_plugin_enabled.lower() == 'Yes'.lower()) and not security_enabled:
      validationItems.append({"config-name": "ranger-storm-plugin-enabled",
                              "item": self.getWarnItem(
                                "Ranger Storm plugin should not be enabled in non-kerberos environment.")})

    return self.toConfigurationValidationProblems(validationItems, "ranger-storm-plugin-properties")


  def validateStormConfigurationsFromHDP25(self, properties, recommendedDefaults, configurations, services, hosts):
    validationItems = []

    servicesList = [service["StackServices"]["service_name"] for service in services["services"]]
    # Storm AMS integration
    if 'AMBARI_METRICS' in servicesList:
      if "storm.cluster.metrics.consumer.register" in properties and \
          'null' in properties.get("storm.cluster.metrics.consumer.register"):

        validationItems.append({"config-name": 'storm.cluster.metrics.consumer.register',
                              "item": self.getWarnItem(
                                "Should be set to recommended value to report metrics to Ambari Metrics service.")})

      if "topology.metrics.consumer.register" in properties and \
          'null' in properties.get("topology.metrics.consumer.register"):

        validationItems.append({"config-name": 'topology.metrics.consumer.register',
                                "item": self.getWarnItem(
                                  "Should be set to recommended value to report metrics to Ambari Metrics service.")})

    return self.toConfigurationValidationProblems(validationItems, "storm-site")