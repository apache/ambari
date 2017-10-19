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

from ambari_commons.str_utils import string_set_equals
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

class AtlasServiceAdvisor(service_advisor.ServiceAdvisor):

  def __init__(self, *args, **kwargs):
    self.as_super = super(AtlasServiceAdvisor, self)
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
    self.heap_size_properties = {"ATLAS_SERVER":
                                   [{"config-name": "atlas-env",
                                     "property": "atlas_server_xmx",
                                     "default": "2048m"}]}

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

    recommender = AtlasRecommender()
    recommender.recommendAtlasConfigurationsFromHDP25(configurations, clusterData, services, hosts)
    recommender.recommendAtlasConfigurationsFromHDP26(configurations, clusterData, services, hosts)



  def getServiceConfigurationsValidationItems(self, configurations, recommendedDefaults, services, hosts):
    """
    Entry point.
    Validate configurations for the service. Return a list of errors.
    The code for this function should be the same for each Service Advisor.
    """
    #Logger.info("Class: %s, Method: %s. Validating Configurations." %
    #            (self.__class__.__name__, inspect.stack()[0][3]))

    validator = AtlasValidator()
    # Calls the methods of the validator using arguments,
    # method(siteProperties, siteRecommendations, configurations, services, hosts)
    return validator.validateListOfConfigUsingMethod(configurations, recommendedDefaults, services, hosts, validator.validators)



class AtlasRecommender(service_advisor.ServiceAdvisor):
  """
  Atlas Recommender suggests properties when adding the service for the first time or modifying configs via the UI.
  """

  def __init__(self, *args, **kwargs):
    self.as_super = super(AtlasRecommender, self)
    self.as_super.__init__(*args, **kwargs)


  def constructAtlasRestAddress(self, services, hosts):
    """
    :param services: Collection of services in the cluster with configs
    :param hosts: Collection of hosts in the cluster
    :return: The suggested property for atlas.rest.address if it is valid, otherwise, None
    """
    atlas_rest_address = None
    services_list = [service["StackServices"]["service_name"] for service in services["services"]]
    is_atlas_in_cluster = "ATLAS" in services_list

    atlas_server_hosts_info = self.getHostsWithComponent("ATLAS", "ATLAS_SERVER", services, hosts)
    if is_atlas_in_cluster and atlas_server_hosts_info and len(atlas_server_hosts_info) > 0:
      # Multiple Atlas Servers can exist, so sort by hostname to create deterministic csv
      atlas_host_names = [e['Hosts']['host_name'] for e in atlas_server_hosts_info]
      if len(atlas_host_names) > 1:
        atlas_host_names = sorted(atlas_host_names)

      scheme = "http"
      metadata_port = "21000"
      atlas_server_default_https_port = "21443"
      tls_enabled = "false"
      if 'application-properties' in services['configurations']:
        if 'atlas.enableTLS' in services['configurations']['application-properties']['properties']:
          tls_enabled = services['configurations']['application-properties']['properties']['atlas.enableTLS']
        if 'atlas.server.http.port' in services['configurations']['application-properties']['properties']:
          metadata_port = str(services['configurations']['application-properties']['properties']['atlas.server.http.port'])

        if str(tls_enabled).lower() == "true":
          scheme = "https"
          if 'atlas.server.https.port' in services['configurations']['application-properties']['properties']:
            metadata_port = str(services['configurations']['application-properties']['properties']['atlas.server.https.port'])
          else:
            metadata_port = atlas_server_default_https_port

      atlas_rest_address_list = ["{0}://{1}:{2}".format(scheme, hostname, metadata_port) for hostname in atlas_host_names]
      atlas_rest_address = ",".join(atlas_rest_address_list)
      self.logger.info("Constructing atlas.rest.address=%s" % atlas_rest_address)
    return atlas_rest_address

  def recommendAtlasConfigurationsFromHDP25(self, configurations, clusterData, services, hosts):
    putAtlasApplicationProperty = self.putProperty(configurations, "application-properties", services)
    putAtlasRangerPluginProperty = self.putProperty(configurations, "ranger-atlas-plugin-properties", services)
    putAtlasEnvProperty = self.putProperty(configurations, "atlas-env", services)

    servicesList = [service["StackServices"]["service_name"] for service in services["services"]]

    # Generate atlas.rest.address since the value is always computed
    atlas_rest_address = self.constructAtlasRestAddress(services, hosts)
    if atlas_rest_address is not None:
      putAtlasApplicationProperty("atlas.rest.address", atlas_rest_address)

    if "AMBARI_INFRA" in servicesList and 'infra-solr-env' in services['configurations']:
      if 'infra_solr_znode' in services['configurations']['infra-solr-env']['properties']:
        infra_solr_znode = services['configurations']['infra-solr-env']['properties']['infra_solr_znode']
      else:
        infra_solr_znode = None

      zookeeper_hosts = self.getHostNamesWithComponent("ZOOKEEPER", "ZOOKEEPER_SERVER", services)
      zookeeper_host_arr = []

      zookeeper_port = self.getZKPort(services)
      for i in range(len(zookeeper_hosts)):
        zookeeper_host = zookeeper_hosts[i] + ':' + zookeeper_port
        if infra_solr_znode is not None:
          zookeeper_host += infra_solr_znode
        zookeeper_host_arr.append(zookeeper_host)

      solr_zookeeper_url = ",".join(zookeeper_host_arr)

      putAtlasApplicationProperty('atlas.graph.index.search.solr.zookeeper-url', solr_zookeeper_url)
    else:
      putAtlasApplicationProperty('atlas.graph.index.search.solr.zookeeper-url', "")

    # Kafka section
    if "KAFKA" in servicesList and 'kafka-broker' in services['configurations']:
      kafka_hosts = self.getHostNamesWithComponent("KAFKA", "KAFKA_BROKER", services)

      if 'port' in services['configurations']['kafka-broker']['properties']:
        kafka_broker_port = services['configurations']['kafka-broker']['properties']['port']
      else:
        kafka_broker_port = '6667'

      if 'kafka-broker' in services['configurations'] and 'listeners' in services['configurations']['kafka-broker']['properties']:
        kafka_server_listeners = services['configurations']['kafka-broker']['properties']['listeners']
      else:
        kafka_server_listeners = 'PLAINTEXT://localhost:6667'

      security_enabled = self.isSecurityEnabled(services)

      if ',' in kafka_server_listeners and len(kafka_server_listeners.split(',')) > 1:
        for listener in kafka_server_listeners.split(','):
          listener = listener.strip().split(':')
          if len(listener) == 3:
            if 'SASL' in listener[0] and security_enabled:
              kafka_broker_port = listener[2]
              break
            elif  'SASL' not in listener[0] and not security_enabled:
              kafka_broker_port = listener[2]
      else:
        listener = kafka_server_listeners.strip().split(':')
        if len(listener) == 3:
          kafka_broker_port  = listener[2]

      kafka_host_arr = []
      for i in range(len(kafka_hosts)):
        kafka_host_arr.append(kafka_hosts[i] + ':' + kafka_broker_port)

      kafka_bootstrap_servers = ",".join(kafka_host_arr)

      if 'zookeeper.connect' in services['configurations']['kafka-broker']['properties']:
        kafka_zookeeper_connect = services['configurations']['kafka-broker']['properties']['zookeeper.connect']
      else:
        kafka_zookeeper_connect = None

      putAtlasApplicationProperty('atlas.kafka.bootstrap.servers', kafka_bootstrap_servers)
      putAtlasApplicationProperty('atlas.kafka.zookeeper.connect', kafka_zookeeper_connect)
    else:
      putAtlasApplicationProperty('atlas.kafka.bootstrap.servers', "")
      putAtlasApplicationProperty('atlas.kafka.zookeeper.connect', "")

    if "HBASE" in servicesList and 'hbase-site' in services['configurations']:
      if 'hbase.zookeeper.quorum' in services['configurations']['hbase-site']['properties']:
        hbase_zookeeper_quorum = services['configurations']['hbase-site']['properties']['hbase.zookeeper.quorum']
      else:
        hbase_zookeeper_quorum = ""

      putAtlasApplicationProperty('atlas.graph.storage.hostname', hbase_zookeeper_quorum)
      putAtlasApplicationProperty('atlas.audit.hbase.zookeeper.quorum', hbase_zookeeper_quorum)
    else:
      putAtlasApplicationProperty('atlas.graph.storage.hostname', "")
      putAtlasApplicationProperty('atlas.audit.hbase.zookeeper.quorum', "")

    if "ranger-env" in services["configurations"] and "ranger-atlas-plugin-properties" in services["configurations"] and \
        "ranger-atlas-plugin-enabled" in services["configurations"]["ranger-env"]["properties"]:
      ranger_atlas_plugin_enabled = services["configurations"]["ranger-env"]["properties"]["ranger-atlas-plugin-enabled"]
      putAtlasRangerPluginProperty('ranger-atlas-plugin-enabled', ranger_atlas_plugin_enabled)

    ranger_atlas_plugin_enabled = ''
    if 'ranger-atlas-plugin-properties' in configurations and 'ranger-atlas-plugin-enabled' in configurations['ranger-atlas-plugin-properties']['properties']:
      ranger_atlas_plugin_enabled = configurations['ranger-atlas-plugin-properties']['properties']['ranger-atlas-plugin-enabled']
    elif 'ranger-atlas-plugin-properties' in services['configurations'] and 'ranger-atlas-plugin-enabled' in services['configurations']['ranger-atlas-plugin-properties']['properties']:
      ranger_atlas_plugin_enabled = services['configurations']['ranger-atlas-plugin-properties']['properties']['ranger-atlas-plugin-enabled']

    if ranger_atlas_plugin_enabled and (ranger_atlas_plugin_enabled.lower() == 'Yes'.lower()):
      putAtlasApplicationProperty('atlas.authorizer.impl','ranger')
    else:
      putAtlasApplicationProperty('atlas.authorizer.impl','simple')

    #atlas server memory settings
    if 'atlas-env' in services['configurations']:
      atlas_server_metadata_size = 50000
      if 'atlas_server_metadata_size' in services['configurations']['atlas-env']['properties']:
        atlas_server_metadata_size = float(services['configurations']['atlas-env']['properties']['atlas_server_metadata_size'])

      atlas_server_xmx = 2048

      if 300000 <= atlas_server_metadata_size < 500000:
        atlas_server_xmx = 1024*5
      if 500000 <= atlas_server_metadata_size < 1000000:
        atlas_server_xmx = 1024*10
      if atlas_server_metadata_size >= 1000000:
        atlas_server_xmx = 1024*16

      atlas_server_max_new_size = (atlas_server_xmx / 100) * 30

      putAtlasEnvProperty("atlas_server_xmx", atlas_server_xmx)
      putAtlasEnvProperty("atlas_server_max_new_size", atlas_server_max_new_size)


  def recommendAtlasConfigurationsFromHDP26(self, configurations, clusterData, services, hosts):
    servicesList = [service["StackServices"]["service_name"] for service in services["services"]]
    putAtlasApplicationProperty = self.putProperty(configurations, "application-properties", services)

    knox_host = 'localhost'
    knox_port = '8443'
    if 'KNOX' in servicesList:
      knox_hosts = self.getComponentHostNames(services, "KNOX", "KNOX_GATEWAY")
      if len(knox_hosts) > 0:
        knox_hosts.sort()
        knox_host = knox_hosts[0]
      if 'gateway-site' in services['configurations'] and 'gateway.port' in services['configurations']["gateway-site"]["properties"]:
        knox_port = services['configurations']["gateway-site"]["properties"]['gateway.port']
      putAtlasApplicationProperty('atlas.sso.knox.providerurl', 'https://{0}:{1}/gateway/knoxsso/api/v1/websso'.format(knox_host, knox_port))




class AtlasValidator(service_advisor.ServiceAdvisor):
  """
  Atlas Validator checks the correctness of properties whenever the service is first added or the user attempts to
  change configs via the UI.
  """

  def __init__(self, *args, **kwargs):
    self.as_super = super(AtlasValidator, self)
    self.as_super.__init__(*args, **kwargs)

    self.validators = [("application-properties", self.validateAtlasConfigurationsFromHDP25)]



  def validateAtlasConfigurationsFromHDP25(self, properties, recommendedDefaults, configurations, services, hosts):
    application_properties = self.getSiteProperties(configurations, "application-properties")
    validationItems = []

    auth_type = application_properties['atlas.authentication.method.ldap.type']
    auth_ldap_enable = application_properties['atlas.authentication.method.ldap'].lower() == 'true'
    self.logger.info("Validating Atlas configs, authentication type: %s" % str(auth_type))

    # Required props
    ldap_props = {"atlas.authentication.method.ldap.url": "",
                  "atlas.authentication.method.ldap.userDNpattern": "uid=",
                  "atlas.authentication.method.ldap.groupSearchBase": "",
                  "atlas.authentication.method.ldap.groupSearchFilter": "",
                  "atlas.authentication.method.ldap.groupRoleAttribute": "cn",
                  "atlas.authentication.method.ldap.base.dn": "",
                  "atlas.authentication.method.ldap.bind.dn": "",
                  "atlas.authentication.method.ldap.bind.password": "",
                  "atlas.authentication.method.ldap.user.searchfilter": ""
    }
    ad_props = {"atlas.authentication.method.ldap.ad.domain": "",
                "atlas.authentication.method.ldap.ad.url": "",
                "atlas.authentication.method.ldap.ad.base.dn": "",
                "atlas.authentication.method.ldap.ad.bind.dn": "",
                "atlas.authentication.method.ldap.ad.bind.password": "",
                "atlas.authentication.method.ldap.ad.user.searchfilter": "(sAMAccountName={0})"
    }

    props_to_require = set()
    if auth_type.lower() == "ldap":
      props_to_require = set(ldap_props.keys())
    elif auth_type.lower() == "ad":
      props_to_require = set(ad_props.keys())
    elif auth_type.lower() == "none":
      pass

    if auth_ldap_enable:
      for prop in props_to_require:
        if prop not in application_properties or application_properties[prop] is None or application_properties[prop].strip() == "":
          validationItems.append({"config-name": prop,
                                  "item": self.getErrorItem("If authentication type is %s, this property is required." % auth_type)})

    if application_properties['atlas.graph.index.search.backend'] == 'solr5' and \
            not application_properties['atlas.graph.index.search.solr.zookeeper-url']:
      validationItems.append({"config-name": "atlas.graph.index.search.solr.zookeeper-url",
                              "item": self.getErrorItem(
                                "If AMBARI_INFRA is not installed then the SOLR zookeeper url configuration must be specified.")})

    if not application_properties['atlas.kafka.bootstrap.servers']:
      validationItems.append({"config-name": "atlas.kafka.bootstrap.servers",
                              "item": self.getErrorItem(
                                "If KAFKA is not installed then the Kafka bootstrap servers configuration must be specified.")})

    if not application_properties['atlas.kafka.zookeeper.connect']:
      validationItems.append({"config-name": "atlas.kafka.zookeeper.connect",
                              "item": self.getErrorItem(
                                "If KAFKA is not installed then the Kafka zookeeper quorum configuration must be specified.")})

    if application_properties['atlas.graph.storage.backend'] == 'hbase' and 'hbase-site' in services['configurations']:
      hbase_zookeeper_quorum = services['configurations']['hbase-site']['properties']['hbase.zookeeper.quorum']

      if not application_properties['atlas.graph.storage.hostname']:
        validationItems.append({"config-name": "atlas.graph.storage.hostname",
                                "item": self.getErrorItem(
                                  "If HBASE is not installed then the hbase zookeeper quorum configuration must be specified.")})
      elif string_set_equals(application_properties['atlas.graph.storage.hostname'], hbase_zookeeper_quorum):
        validationItems.append({"config-name": "atlas.graph.storage.hostname",
                                "item": self.getWarnItem(
                                  "Atlas is configured to use the HBase installed in this cluster. If you would like Atlas to use another HBase instance, please configure this property and HBASE_CONF_DIR variable in atlas-env appropriately.")})

      if not application_properties['atlas.audit.hbase.zookeeper.quorum']:
        validationItems.append({"config-name": "atlas.audit.hbase.zookeeper.quorum",
                                "item": self.getErrorItem(
                                  "If HBASE is not installed then the audit hbase zookeeper quorum configuration must be specified.")})

    elif application_properties['atlas.graph.storage.backend'] == 'hbase' and 'hbase-site' not in services[
      'configurations']:
      if not application_properties['atlas.graph.storage.hostname']:
        validationItems.append({"config-name": "atlas.graph.storage.hostname",
                                "item": self.getErrorItem(
                                  "Atlas is not configured to use the HBase installed in this cluster. If you would like Atlas to use another HBase instance, please configure this property and HBASE_CONF_DIR variable in atlas-env appropriately.")})
      if not application_properties['atlas.audit.hbase.zookeeper.quorum']:
        validationItems.append({"config-name": "atlas.audit.hbase.zookeeper.quorum",
                                "item": self.getErrorItem(
                                  "If HBASE is not installed then the audit hbase zookeeper quorum configuration must be specified.")})

    validationProblems = self.toConfigurationValidationProblems(validationItems, "application-properties")
    return validationProblems

