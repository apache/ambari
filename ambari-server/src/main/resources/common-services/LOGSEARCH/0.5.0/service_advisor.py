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
import math


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

class LogSearchServiceAdvisor(service_advisor.ServiceAdvisor):

  def __init__(self, *args, **kwargs):
    self.as_super = super(LogSearchServiceAdvisor, self)
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
    putLogSearchProperty = self.putProperty(configurations, "logsearch-properties", services)
    putLogSearchAttribute = self.putPropertyAttribute(configurations, "logsearch-properties")
    putLogSearchCommonEnvProperty = self.putProperty(configurations, "logsearch-common-env", services)
    putLogSearchCommonEnvAttribute = self.putPropertyAttribute(configurations, "logsearch-common-env")
    putLogSearchEnvAttribute = self.putPropertyAttribute(configurations, "logsearch-env")
    putLogFeederEnvAttribute = self.putPropertyAttribute(configurations, "logfeeder-env")

    logSearchServerHosts = self.getComponentHostNames(services, "LOGSEARCH", "LOGSEARCH_SERVER")
    # if there is no Log Search server on the cluster, i.e. there is an external server
    if logSearchServerHosts is None or len(logSearchServerHosts) == 0:
      # hide logsearch specific attributes
      for key in services['configurations']['logsearch-env']['properties']:
        putLogSearchEnvAttribute(key, 'visible', 'false')
      for key in services['configurations']['logsearch-properties']['properties']:
        putLogSearchAttribute(key, 'visible', 'false')
      for key in services['configurations']['logsearch-audit_logs-solrconfig']['properties']:
        self.putPropertyAttribute(configurations, "logsearch-audit_logs-solrconfig")(key, 'visible', 'false')
      for key in services['configurations']['logsearch-service_logs-solrconfig']['properties']:
        self.putPropertyAttribute(configurations, "logsearch-service_logs-solrconfig")(key, 'visible', 'false')
      for key in services['configurations']['logsearch-log4j']['properties']:
        self.putPropertyAttribute(configurations, "logsearch-log4j")(key, 'visible', 'false')
      for key in services['configurations']['logsearch-admin-json']['properties']:
        self.putPropertyAttribute(configurations, "logsearch-admin-json")(key, 'visible', 'false')
    # if there is a Log Search server on the cluster
    else:
      infraSolrHosts = self.getComponentHostNames(services, "AMBARI_INFRA", "INFRA_SOLR")
      # if there is AMBARI_INFRA, calculate the min/max shards and recommendations based on the number of infra solr hosts
      if infraSolrHosts is not None and len(infraSolrHosts) > 0 and "logsearch-properties" in services["configurations"]:
        replicationReccomendFloat = math.log(len(infraSolrHosts), 5)
        recommendedReplicationFactor = int(1 + math.floor(replicationReccomendFloat))
        
        recommendedMinShards = len(infraSolrHosts)
        recommendedShards = 2 * len(infraSolrHosts)
        recommendedMaxShards = max(3 * len(infraSolrHosts), 5)
      # if there is no AMBARI_INFRA (i.e. external solr is used), use default values for min/max shards and recommendations
      else:
        recommendedReplicationFactor = 2
        
        recommendedMinShards = 1
        recommendedShards = 1
        recommendedMaxShards = 100
        
        putLogSearchCommonEnvProperty('logsearch_use_external_solr', 'true')
        
        # recommend number of shard
        putLogSearchAttribute('logsearch.collection.service.logs.numshards', 'minimum', recommendedMinShards)
        putLogSearchAttribute('logsearch.collection.service.logs.numshards', 'maximum', recommendedMaxShards)
        putLogSearchProperty("logsearch.collection.service.logs.numshards", recommendedShards)
      
        putLogSearchAttribute('logsearch.collection.audit.logs.numshards', 'minimum', recommendedMinShards)
        putLogSearchAttribute('logsearch.collection.audit.logs.numshards', 'maximum', recommendedMaxShards)
        putLogSearchProperty("logsearch.collection.audit.logs.numshards", recommendedShards)
        # recommend replication factor
        putLogSearchProperty("logsearch.collection.service.logs.replication.factor", recommendedReplicationFactor)
        putLogSearchProperty("logsearch.collection.audit.logs.replication.factor", recommendedReplicationFactor)
      
    kerberos_authentication_enabled = self.isSecurityEnabled(services)
    # if there is no kerberos enabled hide kerberor related properties
    if not kerberos_authentication_enabled:
       putLogSearchCommonEnvProperty('logsearch_external_solr_kerberos_enabled', 'false')
       putLogSearchCommonEnvAttribute('logsearch_external_solr_kerberos_enabled', 'visible', 'false')
       putLogSearchEnvAttribute('logsearch_external_solr_kerberos_keytab', 'visible', 'false')
       putLogSearchEnvAttribute('logsearch_external_solr_kerberos_principal', 'visible', 'false')
       putLogFeederEnvAttribute('logfeeder_external_solr_kerberos_keytab', 'visible', 'false')
       putLogFeederEnvAttribute('logfeeder_external_solr_kerberos_principal', 'visible', 'false')

  def getServiceConfigurationsValidationItems(self, configurations, recommendedDefaults, services, hosts):
    """
    Entry point.
    Validate configurations for the service. Return a list of errors.
    The code for this function should be the same for each Service Advisor.
    """
    #Logger.info("Class: %s, Method: %s. Validating Configurations." %
    #            (self.__class__.__name__, inspect.stack()[0][3]))

    return []
