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

import math
import traceback

from ambari_commons.str_utils import string_set_equals
from resource_management.core.logger import Logger
from resource_management.core.exceptions import Fail
from resource_management.libraries.functions.get_bare_principal import get_bare_principal

try:
  from stack_advisor_24 import *
except ImportError:
  #Ignore ImportError
  print("stack_advisor_24 not found")

class HDP25StackAdvisor(HDP24StackAdvisor):

  def __init__(self):
    super(HDP25StackAdvisor, self).__init__()
    Logger.initialize_logger()
    self.HIVE_INTERACTIVE_SITE = 'hive-interactive-site'
    self.YARN_ROOT_DEFAULT_QUEUE_NAME = 'default'
    self.AMBARI_MANAGED_LLAP_QUEUE_NAME = 'llap'
    self.RANGER_TAGSYNC_SITE = 'ranger-tagsync-site';

  def recommendOozieConfigurations(self, configurations, clusterData, services, hosts):
    super(HDP25StackAdvisor,self).recommendOozieConfigurations(configurations, clusterData, services, hosts)
    putOozieEnvProperty = self.putProperty(configurations, "oozie-env", services)

    if not "oozie-env" in services["configurations"] :
      Logger.info("No oozie configurations available")
      return

    if not "FALCON_SERVER" in clusterData["components"] :
      Logger.info("Falcon is not part of the installation")
      return

    falconUser = 'falcon'

    if "falcon-env" in services["configurations"] :
      if "falcon_user" in services["configurations"]["falcon-env"]["properties"] :
        falconUser = services["configurations"]["falcon-env"]["properties"]["falcon_user"]
        Logger.info("Falcon user from configuration: %s " % falconUser)

    Logger.info("Falcon user : %s" % falconUser)

    oozieUser = 'oozie'

    if "oozie_user" \
      in services["configurations"]["oozie-env"]["properties"] :
      oozieUser = services["configurations"]["oozie-env"]["properties"]["oozie_user"]
      Logger.info("Oozie user from configuration %s" % oozieUser)

    Logger.info("Oozie user %s" % oozieUser)

    if "oozie_admin_users" \
            in services["configurations"]["oozie-env"]["properties"] :
      currentAdminUsers =  services["configurations"]["oozie-env"]["properties"]["oozie_admin_users"]
      Logger.info("Oozie admin users from configuration %s" % currentAdminUsers)
    else :
      currentAdminUsers = "{0}, oozie-admin".format(oozieUser)
      Logger.info("Setting default oozie admin users to %s" % currentAdminUsers)


    if falconUser in currentAdminUsers :
      Logger.info("Falcon user %s already member of  oozie admin users " % falconUser)
      return

    newAdminUsers = "{0},{1}".format(currentAdminUsers, falconUser)

    Logger.info("new oozie admin users : %s" % newAdminUsers)

    services["forced-configurations"].append({"type" : "oozie-env", "name" : "oozie_admin_users"})
    putOozieEnvProperty("oozie_admin_users", newAdminUsers)

  def createComponentLayoutRecommendations(self, services, hosts):
    parentComponentLayoutRecommendations = super(HDP25StackAdvisor, self).createComponentLayoutRecommendations(
      services, hosts)
    return parentComponentLayoutRecommendations

  def getComponentLayoutValidations(self, services, hosts):
    parentItems = super(HDP25StackAdvisor, self).getComponentLayoutValidations(services, hosts)
    childItems = []
    if self.HIVE_INTERACTIVE_SITE in services['configurations']:
      hsi_hosts = self.__getHostsForComponent(services, "HIVE", "HIVE_SERVER_INTERACTIVE")
      if len(hsi_hosts) > 1:
        message = "Only one host can install HIVE_SERVER_INTERACTIVE. "
        childItems.append(
          {"type": 'host-component', "level": 'ERROR', "message": message, "component-name": 'HIVE_SERVER_INTERACTIVE'})

    parentItems.extend(childItems)
    return parentItems

  def getServiceConfigurationValidators(self):
    parentValidators = super(HDP25StackAdvisor, self).getServiceConfigurationValidators()
    childValidators = {
      "ATLAS": {"application-properties": self.validateAtlasConfigurations},
      "HIVE": {"hive-interactive-env": self.validateHiveInteractiveEnvConfigurations,
               "hive-interactive-site": self.validateHiveInteractiveSiteConfigurations,
               "hive-env": self.validateHiveConfigurationsEnv},
      "YARN": {"yarn-site": self.validateYARNConfigurations},
      "RANGER": {"ranger-tagsync-site": self.validateRangerTagsyncConfigurations},
      "SPARK2": {"spark2-defaults": self.validateSpark2Defaults,
                 "spark2-thrift-sparkconf": self.validateSpark2ThriftSparkConf}
    }
    self.mergeValidators(parentValidators, childValidators)
    return parentValidators

  def validateAtlasConfigurations(self, properties, recommendedDefaults, configurations, services, hosts):
    application_properties = getSiteProperties(configurations, "application-properties")
    validationItems = []

    #<editor-fold desc="LDAP and AD">
    auth_type = application_properties['atlas.authentication.method.ldap.type']
    Logger.info("Validating Atlas configs, authentication type: %s" % str(auth_type))

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

    for prop in props_to_require:
      if prop not in application_properties or application_properties[prop] is None or application_properties[prop].strip() == "":
        validationItems.append({"config-name": prop,
                                "item": self.getErrorItem("If authentication type is %s, this property is required." % auth_type)})
    #</editor-fold>

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

  def validateSpark2Defaults(self, properties, recommendedDefaults, configurations, services, hosts):
    validationItems = [
      {
        "config-name": 'spark.yarn.queue',
        "item": self.validatorYarnQueue(properties, recommendedDefaults, 'spark.yarn.queue', services)
      }
    ]
    return self.toConfigurationValidationProblems(validationItems, "spark2-defaults")

  def validateSpark2ThriftSparkConf(self, properties, recommendedDefaults, configurations, services, hosts):
    validationItems = [
      {
        "config-name": 'spark.yarn.queue',
        "item": self.validatorYarnQueue(properties, recommendedDefaults, 'spark.yarn.queue', services)
      }
    ]
    return self.toConfigurationValidationProblems(validationItems, "spark2-thrift-sparkconf")

  def validateYarnConfigurations(self, properties, recommendedDefaults, configurations, services, hosts):
    parentValidationProblems = super(HDP25StackAdvisor, self).validateYARNConfigurations(properties, recommendedDefaults, configurations, services, hosts)
    yarn_site_properties = getSiteProperties(configurations, "yarn-site")
    servicesList = [service["StackServices"]["service_name"] for service in services["services"]]
    componentsListList = [service["components"] for service in services["services"]]
    componentsList = [item["StackServiceComponents"] for sublist in componentsListList for item in sublist]
    validationItems = []
    if self.HIVE_INTERACTIVE_SITE in services['configurations']:
      hsi_hosts = self.__getHostsForComponent(services, "HIVE", "HIVE_SERVER_INTERACTIVE")
      if len(hsi_hosts) > 0:
        # HIVE_SERVER_INTERACTIVE is mapped to a host
        if 'yarn.resourcemanager.work-preserving-recovery.enabled' not in yarn_site_properties or \
                'true' != yarn_site_properties['yarn.resourcemanager.work-preserving-recovery.enabled']:
          validationItems.append({"config-name": "yarn.resourcemanager.work-preserving-recovery.enabled",
                                      "item": self.getWarnItem(
                                        "While enabling HIVE_SERVER_INTERACTIVE it is recommended that you enable work preserving restart in YARN.")})

    validationProblems = self.toConfigurationValidationProblems(validationItems, "yarn-site")
    validationProblems.extend(parentValidationProblems)
    return validationProblems

  """
  Does the following validation checks for HIVE_SERVER_INTERACTIVE's hive-interactive-site configs.
      1. Queue selected in 'hive.llap.daemon.queue.name' config should be sized >= to minimum required to run LLAP
         and Hive2 app.
      2. Queue selected in 'hive.llap.daemon.queue.name' config state should not be 'STOPPED'.
      3. 'hive.server2.enable.doAs' config should be set to 'false' for Hive2.
      4. 'Maximum Total Concurrent Queries'(hive.server2.tez.sessions.per.default.queue) should not consume more that 50% of selected queue for LLAP.
      5. if 'llap' queue is selected, in order to run Service Checks, 'remaining available capacity' in cluster is atleast 512 MB.
  """
  def validateHiveInteractiveSiteConfigurations(self, properties, recommendedDefaults, configurations, services, hosts):
    validationItems = []
    hsi_hosts = self.__getHostsForComponent(services, "HIVE", "HIVE_SERVER_INTERACTIVE")
    curr_selected_queue_for_llap = None
    curr_selected_queue_for_llap_cap_perc =  None
    MIN_ASSUMED_CAP_REQUIRED_FOR_SERVICE_CHECKS = 512
    current_selected_queue_for_llap_cap =  None
    if self.HIVE_INTERACTIVE_SITE in services['configurations']:
      if len(hsi_hosts) > 0:
        # Get total cluster capacity
        node_manager_host_list = self.get_node_manager_hosts(services, hosts)
        node_manager_cnt = len(node_manager_host_list)
        yarn_nm_mem_in_mb = self.get_yarn_nm_mem_in_mb(services, configurations)
        total_cluster_capacity = node_manager_cnt * yarn_nm_mem_in_mb

        capacity_scheduler_properties, received_as_key_value_pair = self.getCapacitySchedulerProperties(services)
        if capacity_scheduler_properties:
          if self.HIVE_INTERACTIVE_SITE in services['configurations'] and \
              'hive.llap.daemon.queue.name' in services['configurations'][self.HIVE_INTERACTIVE_SITE]['properties']:
            curr_selected_queue_for_llap = services['configurations'][self.HIVE_INTERACTIVE_SITE]['properties']['hive.llap.daemon.queue.name']
            if curr_selected_queue_for_llap:
              current_selected_queue_for_llap_cap = self.__getSelectedQueueTotalCap(capacity_scheduler_properties,
                                                                                  curr_selected_queue_for_llap, total_cluster_capacity)
              if current_selected_queue_for_llap_cap:
                curr_selected_queue_for_llap_cap_perc = int(current_selected_queue_for_llap_cap * 100 / total_cluster_capacity)
                min_reqd_queue_cap_perc = self.min_queue_perc_reqd_for_llap_and_hive_app(services, hosts, configurations)

                # Validate that the selected queue in 'hive.llap.daemon.queue.name' should be sized >= to minimum required
                # to run LLAP and Hive2 app.
                if curr_selected_queue_for_llap_cap_perc < min_reqd_queue_cap_perc:
                  errMsg1 =  "Selected queue '{0}' capacity ({1}%) is less than minimum required capacity ({2}%) for LLAP " \
                            "app to run".format(curr_selected_queue_for_llap, curr_selected_queue_for_llap_cap_perc, min_reqd_queue_cap_perc)
                  validationItems.append({"config-name": "hive.llap.daemon.queue.name","item": self.getErrorItem(errMsg1)})
              else:
                Logger.error("Couldn't retrieve '{0}' queue's capacity from 'capacity-scheduler' while doing validation checks for "
                 "Hive Server Interactive.".format(curr_selected_queue_for_llap))

              # Validate that current selected queue in 'hive.llap.daemon.queue.name' state is not STOPPED.
              llap_selected_queue_state = self.__getQueueStateFromCapacityScheduler(capacity_scheduler_properties, curr_selected_queue_for_llap)
              if llap_selected_queue_state:
                if llap_selected_queue_state == "STOPPED":
                  errMsg2 =  "Selected queue '{0}' current state is : '{1}'. It is required to be in 'RUNNING' state for LLAP to run"\
                    .format(curr_selected_queue_for_llap, llap_selected_queue_state)
                  validationItems.append({"config-name": "hive.llap.daemon.queue.name","item": self.getErrorItem(errMsg2)})
              else:
                Logger.error("Couldn't retrieve '{0}' queue's state from 'capacity-scheduler' while doing validation checks for "
                             "Hive Server Interactive.".format(curr_selected_queue_for_llap))
            else:
              Logger.error("Couldn't retrieve current selection for 'hive.llap.daemon.queue.name' while doing validation "
                           "checks for Hive Server Interactive.")
          else:
            Logger.error("Couldn't retrieve 'hive.llap.daemon.queue.name' config from 'hive-interactive-site' while doing "
                         "validation checks for Hive Server Interactive.")
            pass
        else:
          Logger.error("Couldn't retrieve 'capacity-scheduler' properties while doing validation checks for Hive Server Interactive.")
          pass

      if self.HIVE_INTERACTIVE_SITE in services['configurations']:
        # Validate that 'hive.server2.enable.doAs' config is not set to 'true' for Hive2.
        if 'hive.server2.enable.doAs' in services['configurations'][self.HIVE_INTERACTIVE_SITE]['properties']:
          hive2_enable_do_as = services['configurations'][self.HIVE_INTERACTIVE_SITE]['properties']['hive.server2.enable.doAs']
          if hive2_enable_do_as == 'true':
            validationItems.append({"config-name": "hive.server2.enable.doAs","item": self.getErrorItem("Value should be set to 'false' for Hive2.")})

        # Validate that 'Maximum Total Concurrent Queries'(hive.server2.tez.sessions.per.default.queue) is not consuming more that
        # 50% of selected queue for LLAP.
        if current_selected_queue_for_llap_cap and 'hive.server2.tez.sessions.per.default.queue' in \
          services['configurations']['hive-interactive-site']['properties']:
          num_tez_sessions = services['configurations']['hive-interactive-site']['properties']['hive.server2.tez.sessions.per.default.queue']
          if num_tez_sessions:
            num_tez_sessions = long(num_tez_sessions)
            yarn_min_container_size = self.get_yarn_min_container_size(services, configurations)
            tez_am_container_size = self.calculate_tez_am_container_size(long(total_cluster_capacity))
            normalized_tez_am_container_size = self._normalizeUp(tez_am_container_size, yarn_min_container_size)
            llap_selected_queue_cap_remaining = current_selected_queue_for_llap_cap - (normalized_tez_am_container_size * num_tez_sessions)
            if llap_selected_queue_cap_remaining <= current_selected_queue_for_llap_cap/2:
              errMsg3 = " Reducing the 'Maximum Total Concurrent Queries' (value: {0}) is advisable as it is consuming more than 50% of " \
                        "'{1}' queue for LLAP.".format(num_tez_sessions, curr_selected_queue_for_llap)
              validationItems.append({"config-name": "hive.server2.tez.sessions.per.default.queue","item": self.getWarnItem(errMsg3)})

      # Validate that 'remaining available capacity' in cluster is atleast 512 MB, after 'llap' queue is selected,
      # in order to run Service Checks.
      if curr_selected_queue_for_llap and curr_selected_queue_for_llap_cap_perc and \
          curr_selected_queue_for_llap == self.AMBARI_MANAGED_LLAP_QUEUE_NAME:
        curr_selected_queue_for_llap_cap = float(curr_selected_queue_for_llap_cap_perc) / 100 * total_cluster_capacity
        available_cap_in_cluster = total_cluster_capacity - curr_selected_queue_for_llap_cap
        if available_cap_in_cluster < MIN_ASSUMED_CAP_REQUIRED_FOR_SERVICE_CHECKS:
          errMsg4 =  "Capacity used by '{0}' queue is '{1}'. Service checks may not run as remaining available capacity " \
                     "({2}) in cluster is less than 512 MB.".format(self.AMBARI_MANAGED_LLAP_QUEUE_NAME, curr_selected_queue_for_llap_cap, available_cap_in_cluster)
          validationItems.append({"config-name": "hive.llap.daemon.queue.name","item": self.getWarnItem(errMsg4)})

    validationProblems = self.toConfigurationValidationProblems(validationItems, "hive-interactive-site")
    return validationProblems

  def validateHiveConfigurationsEnv(self, properties, recommendedDefaults, configurations, services, hosts):
    parentValidationProblems = super(HDP25StackAdvisor, self).validateHiveConfigurationsEnv(properties, recommendedDefaults, configurations, services, hosts)
    hive_site_properties = self.getSiteProperties(configurations, "hive-site")
    hive_env_properties = self.getSiteProperties(configurations, "hive-env")
    validationItems = []

    if 'hive.server2.authentication' in hive_site_properties and hive_site_properties['hive.server2.authentication'] in ["LDAP","PAM"]:
      if 'alert_ldap_username' not in hive_env_properties or hive_env_properties['alert_ldap_username'] == "":
        validationItems.append({"config-name": "alert_ldap_username",
                                "item": self.getWarnItem(
                                  "Provide an user to be used for alerts. Hive authentication type LDAP and PAM requires valid credentials for the alerts.")})
      if 'alert_ldap_password' not in hive_env_properties or hive_env_properties['alert_ldap_password'] == "":
        validationItems.append({"config-name": "alert_ldap_password",
                                "item": self.getWarnItem(
                                  "Provide the password for the alert user. Hive authentication type LDAP and PAM requires valid credentials for the alerts.")})

    validationProblems = self.toConfigurationValidationProblems(validationItems, "hive-env")
    validationProblems.extend(parentValidationProblems)
    return validationProblems

  def validateHiveInteractiveEnvConfigurations(self, properties, recommendedDefaults, configurations, services, hosts):
    hive_site_env_properties = getSiteProperties(configurations, "hive-interactive-env")
    validationItems = []
    if self.HIVE_INTERACTIVE_SITE in services['configurations']:
      hsi_hosts = self.__getHostsForComponent(services, "HIVE", "HIVE_SERVER_INTERACTIVE")
      if len(hsi_hosts) > 0:
        # HIVE_SERVER_INTERACTIVE is mapped to a host
        if 'enable_hive_interactive' not in hive_site_env_properties or (
              'enable_hive_interactive' in hive_site_env_properties and hive_site_env_properties[
            'enable_hive_interactive'].lower() != 'true'):
          validationItems.append({"config-name": "enable_hive_interactive",
                                  "item": self.getErrorItem(
                                    "HIVE_SERVER_INTERACTIVE requires enable_hive_interactive in hive-interactive-env set to true.")})
        if 'hive_server_interactive_host' in hive_site_env_properties:
          hsi_host = hsi_hosts[0]
          if hive_site_env_properties['hive_server_interactive_host'].lower() != hsi_host.lower():
            validationItems.append({"config-name": "hive_server_interactive_host",
                                    "item": self.getErrorItem(
                                      "HIVE_SERVER_INTERACTIVE requires hive_server_interactive_host in hive-interactive-env set to its host name.")})
          pass
        if 'hive_server_interactive_host' not in hive_site_env_properties:
          validationItems.append({"config-name": "hive_server_interactive_host",
                                  "item": self.getErrorItem(
                                    "HIVE_SERVER_INTERACTIVE requires hive_server_interactive_host in hive-interactive-env set to its host name.")})
          pass

      else:
        # no  HIVE_SERVER_INTERACTIVE
        if 'enable_hive_interactive' in hive_site_env_properties and hive_site_env_properties[
          'enable_hive_interactive'].lower() != 'false':
          validationItems.append({"config-name": "enable_hive_interactive",
                                  "item": self.getErrorItem(
                                    "enable_hive_interactive in hive-interactive-env should be set to false.")})
          pass
        pass

    validationProblems = self.toConfigurationValidationProblems(validationItems, "hive-interactive-env")
    return validationProblems

  def getServiceConfigurationRecommenderDict(self):
    parentRecommendConfDict = super(HDP25StackAdvisor, self).getServiceConfigurationRecommenderDict()
    childRecommendConfDict = {
      "RANGER": self.recommendRangerConfigurations,
      "HBASE": self.recommendHBASEConfigurations,
      "HIVE": self.recommendHIVEConfigurations,
      "ATLAS": self.recommendAtlasConfigurations,
      "RANGER_KMS": self.recommendRangerKMSConfigurations,
      "STORM": self.recommendStormConfigurations,
      "OOZIE": self.recommendOozieConfigurations,
      "SPARK2": self.recommendSpark2Configurations
    }
    parentRecommendConfDict.update(childRecommendConfDict)
    return parentRecommendConfDict

  def recommendSpark2Configurations(self, configurations, clusterData, services, hosts):
    """
    :type configurations dict
    :type clusterData dict
    :type services dict
    :type hosts dict
    """
    putSparkProperty = self.putProperty(configurations, "spark2-defaults", services)
    putSparkThriftSparkConf = self.putProperty(configurations, "spark2-thrift-sparkconf", services)

    spark_queue = self.recommendYarnQueue(services, "spark2-defaults", "spark.yarn.queue")
    if spark_queue is not None:
      putSparkProperty("spark.yarn.queue", spark_queue)

    spart_thrift_queue = self.recommendYarnQueue(services, "spark2-thrift-sparkconf", "spark.yarn.queue")
    if spart_thrift_queue is not None:
      putSparkThriftSparkConf("spark.yarn.queue", spart_thrift_queue)

  def recommendStormConfigurations(self, configurations, clusterData, services, hosts):
    super(HDP25StackAdvisor, self).recommendStormConfigurations(configurations, clusterData, services, hosts)
    storm_site = getServicesSiteProperties(services, "storm-site")
    putStormSiteProperty = self.putProperty(configurations, "storm-site", services)
    putStormSiteAttributes = self.putPropertyAttribute(configurations, "storm-site")
    security_enabled = (storm_site is not None and "storm.zookeeper.superACL" in storm_site)
    
    if security_enabled:
      _storm_principal_name = services['configurations']['storm-env']['properties']['storm_principal_name']
      storm_bare_jaas_principal = get_bare_principal(_storm_principal_name)
      if 'nimbus.impersonation.acl' in storm_site:  
        storm_nimbus_impersonation_acl = storm_site["nimbus.impersonation.acl"]
        storm_nimbus_impersonation_acl.replace('{{storm_bare_jaas_principal}}', storm_bare_jaas_principal)
        putStormSiteProperty('nimbus.impersonation.acl', storm_nimbus_impersonation_acl)
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
      elif rangerPluginEnabled and (rangerPluginEnabled.lower() == 'No'.lower()) and (services["configurations"]["storm-site"]["properties"]["nimbus.authorizer"] == ranger_authorizer_class):
        putStormSiteProperty('nimbus.authorizer', storm_authorizer_class)
    else:
      putStormSiteAttributes('nimbus.authorizer', 'delete', 'true')

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
      Logger.info("Constructing atlas.rest.address=%s" % atlas_rest_address)
    return atlas_rest_address

  def recommendAtlasConfigurations(self, configurations, clusterData, services, hosts):
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
        atlas_server_metadata_size = int(services['configurations']['atlas-env']['properties']['atlas_server_metadata_size'])

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

  def recommendHBASEConfigurations(self, configurations, clusterData, services, hosts):
    super(HDP25StackAdvisor, self).recommendHBASEConfigurations(configurations, clusterData, services, hosts)
    putHbaseSiteProperty = self.putProperty(configurations, "hbase-site", services)
    appendCoreSiteProperty = self.updateProperty(configurations, "core-site", services)

    if "cluster-env" in services["configurations"] \
         and "security_enabled" in services["configurations"]["cluster-env"]["properties"] \
         and services["configurations"]["cluster-env"]["properties"]["security_enabled"].lower() == "true":
      # Set the master's UI to readonly
      putHbaseSiteProperty('hbase.master.ui.readonly', 'true')

      phoenix_query_server_hosts = self.get_phoenix_query_server_hosts(services, hosts)
      Logger.debug("Calculated Phoenix Query Server hosts: %s" % str(phoenix_query_server_hosts))
      if phoenix_query_server_hosts:
        Logger.debug("Attempting to update hadoop.proxyuser.HTTP.hosts with %s" % str(phoenix_query_server_hosts))
        # The PQS hosts we want to ensure are set
        new_value = ','.join(phoenix_query_server_hosts)
        # Compute the unique set of hosts for the property
        def updateCallback(originalValue, newValue):
          Logger.debug("Original hadoop.proxyuser.HTTP.hosts value %s, appending %s" % (originalValue, newValue))
          # Only update the original value if it's not whitespace only
          if originalValue and not originalValue.isspace():
            hosts = originalValue.split(',')
            # Add in the new hosts if we have some
            if newValue and not newValue.isspace():
              hosts.extend(newValue.split(','))
            # Return the combined (uniqued) list of hosts
            result = ','.join(set(hosts))
            Logger.debug("Setting final to %s" % result)
            return result
          else:
            Logger.debug("Setting final value to %s" % newValue)
            return newValue
        # Update the proxyuser setting, deferring to out callback to merge results together
        appendCoreSiteProperty('hadoop.proxyuser.HTTP.hosts', new_value, updateCallback)
      else:
        Logger.debug("No phoenix query server hosts to update")
    else:
      putHbaseSiteProperty('hbase.master.ui.readonly', 'false')

  """
  Returns the list of Phoenix Query Server host names, or None.
  """
  def get_phoenix_query_server_hosts(self, services, hosts):
    if len(hosts['items']) > 0:
      phoenix_query_server_hosts = self.getHostsWithComponent("HBASE", "PHOENIX_QUERY_SERVER", services, hosts)
      if phoenix_query_server_hosts is None:
        return []
      return [host['Hosts']['host_name'] for host in phoenix_query_server_hosts]

  def recommendHIVEConfigurations(self, configurations, clusterData, services, hosts):
    super(HDP25StackAdvisor, self).recommendHIVEConfigurations(configurations, clusterData, services, hosts)
    putHiveInteractiveEnvProperty = self.putProperty(configurations, "hive-interactive-env", services)
    putHiveInteractiveSiteProperty = self.putProperty(configurations, self.HIVE_INTERACTIVE_SITE, services)
    putHiveInteractiveEnvPropertyAttribute = self.putPropertyAttribute(configurations, "hive-interactive-env")

    # For 'Hive Server Interactive', if the component exists.
    hsi_hosts = self.__getHostsForComponent(services, "HIVE", "HIVE_SERVER_INTERACTIVE")
    if len(hsi_hosts) > 0:
      hsi_host = hsi_hosts[0]
      putHiveInteractiveEnvProperty('enable_hive_interactive', 'true')
      putHiveInteractiveEnvProperty('hive_server_interactive_host', hsi_host)

      # Update 'hive.llap.daemon.queue.name' property attributes if capacity scheduler is changed.
      if self.HIVE_INTERACTIVE_SITE in services['configurations']:
        if 'hive.llap.daemon.queue.name' in services['configurations'][self.HIVE_INTERACTIVE_SITE]['properties']:
          self.setLlapDaemonQueuePropAttributesAndCapSliderVisibility(services, configurations)

          # Update 'hive.server2.tez.default.queues' value
          hive_tez_default_queue = None
          if 'hive-interactive-site' in configurations and \
              'hive.llap.daemon.queue.name' in configurations[self.HIVE_INTERACTIVE_SITE]['properties']:
            hive_tez_default_queue = configurations[self.HIVE_INTERACTIVE_SITE]['properties']['hive.llap.daemon.queue.name']
            Logger.info("'hive.llap.daemon.queue.name' value from configurations : '{0}'".format(hive_tez_default_queue))
          if not hive_tez_default_queue:
            hive_tez_default_queue = services['configurations'][self.HIVE_INTERACTIVE_SITE]['properties']['hive.llap.daemon.queue.name']
            Logger.info("'hive.llap.daemon.queue.name' value from services : '{0}'".format(hive_tez_default_queue))
          if hive_tez_default_queue:
            putHiveInteractiveSiteProperty("hive.server2.tez.default.queues", hive_tez_default_queue)
            Logger.info("Updated 'hive.server2.tez.default.queues' config : '{0}'".format(hive_tez_default_queue))
    else:
      putHiveInteractiveEnvProperty('enable_hive_interactive', 'false')
      putHiveInteractiveEnvPropertyAttribute("llap_queue_capacity", "visible", "false")

    if self.HIVE_INTERACTIVE_SITE in services['configurations'] and \
        'hive.llap.zk.sm.connectionString' in services['configurations'][self.HIVE_INTERACTIVE_SITE]['properties']:
      # Fill the property 'hive.llap.zk.sm.connectionString' required by Hive Server Interactive (HiveServer2)
      zookeeper_host_port = self.getZKHostPortString(services)
      if zookeeper_host_port:
        putHiveInteractiveSiteProperty("hive.llap.zk.sm.connectionString", zookeeper_host_port)
    pass

  def recommendYARNConfigurations(self, configurations, clusterData, services, hosts):
    super(HDP25StackAdvisor, self).recommendYARNConfigurations(configurations, clusterData, services, hosts)

    # Queue 'llap' creation/removal logic (Used by Hive Interactive server and associated LLAP)
    if 'hive-interactive-env' in services['configurations'] and \
        'enable_hive_interactive' in services['configurations']['hive-interactive-env']['properties']:
      enable_hive_interactive = services['configurations']['hive-interactive-env']['properties']['enable_hive_interactive']
      LLAP_QUEUE_NAME = 'llap'

      # Hive Server interactive is already added or getting added
      if enable_hive_interactive == 'true':
        self.checkAndManageLlapQueue(services, configurations, hosts, LLAP_QUEUE_NAME)
        self.updateLlapConfigs(configurations, services, hosts, LLAP_QUEUE_NAME)
      else:  # When Hive Interactive Server is in 'off/removed' state.
        self.checkAndStopLlapQueue(services, configurations, LLAP_QUEUE_NAME)

    putYarnSiteProperty = self.putProperty(configurations, "yarn-site", services)
    stack_root = self.getStackRoot(services
    )

    timeline_plugin_classes_values = []
    timeline_plugin_classpath_values = []

    if self.__isServiceDeployed(services, "TEZ"):
      timeline_plugin_classes_values.append('org.apache.tez.dag.history.logging.ats.TimelineCachePluginImpl')

    if self.__isServiceDeployed(services, "SPARK"):
      timeline_plugin_classes_values.append('org.apache.spark.deploy.history.yarn.plugin.SparkATSPlugin')
      timeline_plugin_classpath_values.append(stack_root + "/{{spark_version}}/spark/hdpLib/*")

    putYarnSiteProperty('yarn.timeline-service.entity-group-fs-store.group-id-plugin-classes', ",".join(timeline_plugin_classes_values))
    putYarnSiteProperty('yarn.timeline-service.entity-group-fs-store.group-id-plugin-classpath', ":".join(timeline_plugin_classpath_values))

  """
  Entry point for updating Hive's 'LLAP app' configs namely : (1). num_llap_nodes (2). hive.llap.daemon.yarn.container.mb
  (3). hive.llap.daemon.num.executors (4). hive.llap.io.memory.size (5). llap_heap_size (6). slider_am_container_mb,
  and (7). hive.server2.tez.sessions.per.default.queue

    The trigger point for updating LLAP configs (mentioned above) is change in values of any of the following:
    (1). 'enable_hive_interactive' set to 'true' (2). 'llap_queue_capacity' (3). 'hive.server2.tez.sessions.per.default.queue'
    (4). Change in queue selection for config 'hive.llap.daemon.queue.name'.

    If change in value for 'llap_queue_capacity' or 'hive.server2.tez.sessions.per.default.queue' is detected, that config
    value is not calulated, but read and use in calculation for dependent configs.
  """
  def updateLlapConfigs(self, configurations, services, hosts, llap_queue_name):
    putHiveInteractiveSiteProperty = self.putProperty(configurations, self.HIVE_INTERACTIVE_SITE, services)
    putHiveInteractiveSitePropertyAttribute = self.putPropertyAttribute(configurations, self.HIVE_INTERACTIVE_SITE)

    putHiveInteractiveEnvProperty = self.putProperty(configurations, "hive-interactive-env", services)
    putHiveInteractiveEnvPropertyAttribute = self.putPropertyAttribute(configurations, "hive-interactive-env")

    putTezInteractiveSiteProperty = self.putProperty(configurations, "tez-interactive-site", services)

    llap_daemon_selected_queue_name = None
    llap_queue_selected_in_current_call = None
    LLAP_MAX_CONCURRENCY = 32 # Allow a max of 32 concurrency.

    # Update 'hive.llap.daemon.queue.name' prop combo entries and llap capacity slider visibility.
    self.setLlapDaemonQueuePropAttributesAndCapSliderVisibility(services, configurations)

    if not services["changed-configurations"]:
      read_llap_daemon_yarn_cont_mb = long(self.get_yarn_min_container_size(services, configurations))
      putHiveInteractiveSiteProperty('hive.llap.daemon.yarn.container.mb', read_llap_daemon_yarn_cont_mb)
      # initial memory setting to make sure hive.llap.daemon.yarn.container.mb >= yarn.scheduler.minimum-allocation-mb
      Logger.info("Adjusted 'hive.llap.daemon.yarn.container.mb' to yarn min container size as initial size "
                 "(" + str(self.get_yarn_min_container_size(services, configurations)) + " MB).")

    try:
      if self.HIVE_INTERACTIVE_SITE in services['configurations'] and \
          'hive.llap.daemon.queue.name' in services['configurations'][self.HIVE_INTERACTIVE_SITE]['properties']:
        llap_daemon_selected_queue_name =  services['configurations'][self.HIVE_INTERACTIVE_SITE]['properties']['hive.llap.daemon.queue.name']

      if 'hive.llap.daemon.queue.name' in configurations[self.HIVE_INTERACTIVE_SITE]['properties']:
        llap_queue_selected_in_current_call = configurations[self.HIVE_INTERACTIVE_SITE]['properties']['hive.llap.daemon.queue.name']

      # Update Visibility of 'llap_queue_capacity' slider.
      capacity_scheduler_properties, received_as_key_value_pair = self.getCapacitySchedulerProperties(services)
      if capacity_scheduler_properties:
        # Get all leaf queues.
        leafQueueNames = self.getAllYarnLeafQueues(capacity_scheduler_properties)
        if len(leafQueueNames) == 2 and \
          (llap_daemon_selected_queue_name != None and llap_daemon_selected_queue_name == llap_queue_name) or \
          (llap_queue_selected_in_current_call != None and llap_queue_selected_in_current_call == llap_queue_name):
            putHiveInteractiveEnvPropertyAttribute("llap_queue_capacity", "visible", "true")
            Logger.info("Selected YARN queue is '{0}'. Setting LLAP queue capacity slider visibility to 'True'".format(llap_queue_name))
        else:
          putHiveInteractiveEnvPropertyAttribute("llap_queue_capacity", "visible", "false")
          Logger.info("Queue selected for LLAP app is : '{0}'. Current YARN queues : {1}. Setting '{2}' queue capacity slider "
                      "visibility to 'False'.".format(llap_daemon_selected_queue_name, list(leafQueueNames), llap_queue_name))
        if llap_daemon_selected_queue_name:
          llap_selected_queue_state = self.__getQueueStateFromCapacityScheduler(capacity_scheduler_properties, llap_daemon_selected_queue_name)
          if llap_selected_queue_state == None or llap_selected_queue_state == "STOPPED":
            putHiveInteractiveEnvPropertyAttribute("llap_queue_capacity", "visible", "false")
            raise Fail("Selected LLAP app queue '{0}' current state is : '{1}'. Setting LLAP configs to default values "
                       "and 'llap' queue capacity slider visibility to 'False'."
                       .format(llap_daemon_selected_queue_name, llap_selected_queue_state))
        else:
          raise Fail("Retrieved LLAP app queue name is : '{0}'. Setting LLAP configs to default values."
                     .format(llap_daemon_selected_queue_name))
      else:
        Logger.error("Couldn't retrieve 'capacity-scheduler' properties while doing YARN queue adjustment for Hive Server Interactive."
                     " Not calculating LLAP configs.")
        return

      changed_configs_in_hive_int_env = None
      llap_concurrency_in_changed_configs = None
      llap_daemon_queue_in_changed_configs = None
      # Calculations are triggered only if there is change in any one of the following props :
      # 'llap_queue_capacity', 'enable_hive_interactive', 'hive.server2.tez.sessions.per.default.queue'
      # or 'hive.llap.daemon.queue.name' has change in value selection.
      # OR
      # services['changed-configurations'] is empty implying that this is the Blueprint call. (1st invocation)
      if 'changed-configurations' in services.keys():
        config_names_to_be_checked = set(['llap_queue_capacity', 'enable_hive_interactive'])
        changed_configs_in_hive_int_env = self.are_config_props_in_changed_configs(services, "hive-interactive-env",
                                                                                   config_names_to_be_checked, False)

        # Determine if there is change detected in "hive-interactive-site's" configs based on which we calculate llap configs.
        llap_concurrency_in_changed_configs = self.are_config_props_in_changed_configs(services, "hive-interactive-site",
                                                                                       set(['hive.server2.tez.sessions.per.default.queue']), False)
        llap_daemon_queue_in_changed_configs = self.are_config_props_in_changed_configs(services, "hive-interactive-site",
                                                                                       set(['hive.llap.daemon.queue.name']), False)

      if not changed_configs_in_hive_int_env and \
        not llap_concurrency_in_changed_configs and \
        not llap_daemon_queue_in_changed_configs and \
        services["changed-configurations"]:
        Logger.info("LLAP parameters not modified. Not adjusting LLAP configs.")
        Logger.info("Current 'changed-configuration' received is : {0}".format(services["changed-configurations"]))
        return

      node_manager_host_list = self.get_node_manager_hosts(services, hosts)
      node_manager_cnt = len(node_manager_host_list)
      yarn_nm_mem_in_mb = self.get_yarn_nm_mem_in_mb(services, configurations)
      total_cluster_capacity = node_manager_cnt * yarn_nm_mem_in_mb
      Logger.info("\n\nCalculated total_cluster_capacity : {0}, using following : node_manager_cnt : {1}, "
                  "yarn_nm_mem_in_mb : {2}".format(total_cluster_capacity, node_manager_cnt, yarn_nm_mem_in_mb))

      # Check which queue is selected in 'hive.llap.daemon.queue.name', to determine current queue capacity
      current_selected_queue_for_llap_cap = None
      yarn_root_queues = capacity_scheduler_properties.get("yarn.scheduler.capacity.root.queues")
      if llap_queue_selected_in_current_call == llap_queue_name \
        or llap_daemon_selected_queue_name == llap_queue_name \
        and (llap_queue_name in yarn_root_queues and len(leafQueueNames) == 2):
        current_selected_queue_for_llap_cap_perc = self.get_llap_cap_percent_slider(services, configurations)
        current_selected_queue_for_llap_cap = current_selected_queue_for_llap_cap_perc / 100 * total_cluster_capacity
      else:  # any queue other than 'llap'
        current_selected_queue_for_llap_cap = self.__getSelectedQueueTotalCap(capacity_scheduler_properties,
                                                                              llap_daemon_selected_queue_name, total_cluster_capacity)
      assert (current_selected_queue_for_llap_cap >= 1), "Current selected queue '{0}' capacity value : {1}. Expected value : >= 1" \
        .format(llap_daemon_selected_queue_name, current_selected_queue_for_llap_cap)
      yarn_min_container_size = self.get_yarn_min_container_size(services, configurations)
      tez_am_container_size = self.calculate_tez_am_container_size(long(total_cluster_capacity))
      normalized_tez_am_container_size = self._normalizeUp(tez_am_container_size, yarn_min_container_size)
      Logger.info("Calculated normalized_tez_am_container_size : {0}, using following : tez_am_container_size : {1}, "
                  "total_cluster_capacity : {2}".format(normalized_tez_am_container_size, tez_am_container_size,
                                                        total_cluster_capacity))
      normalized_selected_queue_for_llap_cap = long(self._normalizeDown(current_selected_queue_for_llap_cap, yarn_min_container_size))

      # Get calculated value for Slider AM container Size
      slider_am_container_size = self._normalizeUp(self.calculate_slider_am_size(yarn_min_container_size),
                                                   yarn_min_container_size)

      # Read 'hive.server2.tez.sessions.per.default.queue' prop if it's in changed-configs, else calculate it.
      if not llap_concurrency_in_changed_configs:
        # Calculate llap concurrency (i.e. Number of Tez AM's)
        llap_concurrency = float(normalized_selected_queue_for_llap_cap * 0.25 / normalized_tez_am_container_size)
        llap_concurrency = max(long(llap_concurrency), 1)
        Logger.info("Calculated llap_concurrency : {0}, using following : normalized_selected_queue_for_llap_cap : {1}, "
                    "normalized_tez_am_container_size : {2}".format(llap_concurrency, normalized_selected_queue_for_llap_cap,
                                                                    normalized_tez_am_container_size))
        # Limit 'llap_concurrency' to reach a max. of 32.
        if llap_concurrency > LLAP_MAX_CONCURRENCY:
          llap_concurrency = LLAP_MAX_CONCURRENCY
      else:
        # Read current value
        if 'hive.server2.tez.sessions.per.default.queue' in services['configurations'][self.HIVE_INTERACTIVE_SITE][
          'properties']:
          llap_concurrency = long(services['configurations'][self.HIVE_INTERACTIVE_SITE]['properties'][
                                    'hive.server2.tez.sessions.per.default.queue'])
          assert (
          llap_concurrency >= 1), "'hive.server2.tez.sessions.per.default.queue' current value : {0}. Expected value : >= 1" \
            .format(llap_concurrency)
        else:
          raise Fail(
            "Couldn't retrieve Hive Server interactive's 'hive.server2.tez.sessions.per.default.queue' config.")


      # Calculate 'total memory available for llap daemons' across cluster
      total_am_capacity_required = normalized_tez_am_container_size * llap_concurrency + slider_am_container_size
      cap_available_for_daemons = normalized_selected_queue_for_llap_cap - total_am_capacity_required
      Logger.info(
        "Calculated cap_available_for_daemons : {0}, using following : current_selected_queue_for_llap_cap : {1}, "
        "yarn_nm_mem_in_mb : {2}, total_cluster_capacity : {3}, normalized_selected_queue_for_llap_cap : {4}, normalized_tez_am_container_size"
        " : {5}, yarn_min_container_size : {6}, llap_concurrency : {7}, total_am_capacity_required : {8}"
        .format(cap_available_for_daemons, current_selected_queue_for_llap_cap, yarn_nm_mem_in_mb,
                total_cluster_capacity,
                normalized_selected_queue_for_llap_cap, normalized_tez_am_container_size, yarn_min_container_size, llap_concurrency,
                total_am_capacity_required))
      if cap_available_for_daemons < yarn_min_container_size:
        raise Fail(
          "'Capacity available for LLAP daemons'({0}) < 'YARN minimum container size'({1}). Invalid configuration detected. "
          "Increase LLAP queue size.".format(cap_available_for_daemons, yarn_min_container_size))



      # Calculate value for 'num_llap_nodes', an across cluster config.
      # Also, get calculated value for 'hive.llap.daemon.yarn.container.mb' based on 'num_llap_nodes' value, a per node config.
      num_llap_nodes_raw = cap_available_for_daemons / yarn_nm_mem_in_mb
      if num_llap_nodes_raw < 1.00:
        # Set the llap nodes to min. value of 1 and 'llap_container_size' to min. YARN allocation.
        num_llap_nodes = 1
        llap_container_size = self._normalizeUp(cap_available_for_daemons, yarn_min_container_size)
        Logger.info("Calculated llap_container_size : {0}, using following : cap_available_for_daemons : {1}, "
                    "yarn_min_container_size : {2}".format(llap_container_size, cap_available_for_daemons,
                                                           yarn_min_container_size))
      else:
        num_llap_nodes = math.floor(num_llap_nodes_raw)
        llap_container_size = self._normalizeDown(yarn_nm_mem_in_mb, yarn_min_container_size)
        Logger.info("Calculated llap_container_size : {0}, using following : yarn_nm_mem_in_mb : {1}, "
                    "yarn_min_container_size : {2}".format(llap_container_size, yarn_nm_mem_in_mb,
                                                           yarn_min_container_size))
      Logger.info(
        "Calculated num_llap_nodes : {0} using following : yarn_nm_mem_in_mb : {1}, cap_available_for_daemons : {2} " \
        .format(num_llap_nodes, yarn_nm_mem_in_mb, cap_available_for_daemons))


      # Calculate value for 'hive.llap.daemon.num.executors', a per node config.
      hive_tez_container_size = self.get_hive_tez_container_size(services, configurations)
      if 'yarn.nodemanager.resource.cpu-vcores' in services['configurations']['yarn-site']['properties']:
        cpu_per_nm_host = float(services['configurations']['yarn-site']['properties'][
                                  'yarn.nodemanager.resource.cpu-vcores'])
        assert (cpu_per_nm_host > 0), "'yarn.nodemanager.resource.cpu-vcores' current value : {0}. Expected value : > 0" \
          .format(cpu_per_nm_host)
      else:
        raise Fail("Couldn't retrieve YARN's 'yarn.nodemanager.resource.cpu-vcores' config.")

      num_executors_per_node_raw = math.floor(llap_container_size / hive_tez_container_size)
      num_executors_per_node = min(num_executors_per_node_raw, cpu_per_nm_host)
      Logger.info("calculated num_executors_per_node: {0}, using following :  hive_tez_container_size : {1}, "
                  "cpu_per_nm_host : {2}, num_executors_per_node_raw : {3}, llap_container_size : {4}"
                  .format(num_executors_per_node, hive_tez_container_size, cpu_per_nm_host, num_executors_per_node_raw,
                          llap_container_size))
      assert (num_executors_per_node >= 0), "'Number of executors per node' : {0}. Expected value : > 0".format(
        num_executors_per_node)

      total_mem_for_executors = num_executors_per_node * hive_tez_container_size

      # Calculate value for 'cache' (hive.llap.io.memory.size), a per node config.
      cache_size_per_node = llap_container_size - total_mem_for_executors
      Logger.info(
        "Calculated cache_size_per_node : {0} using following : hive_container_size : {1}, llap_container_size"
        " : {2}, num_executors_per_node : {3}"
        .format(cache_size_per_node, hive_tez_container_size, llap_container_size, num_executors_per_node))
      if cache_size_per_node < 0:  # Run with '0' cache.
        Logger.info(
          "Calculated 'cache_size_per_node' : {0}. Setting 'cache_size_per_node' to 0.".format(cache_size_per_node))
        cache_size_per_node = 0


      # Calculate value for prop 'llap_heap_size'
      llap_xmx = max(total_mem_for_executors * 0.8, total_mem_for_executors - self.get_llap_headroom_space(services, configurations))
      Logger.info("Calculated llap_app_heap_size : {0}, using following : hive_container_size : {1}, "
                  "total_mem_for_executors : {2}".format(llap_xmx, hive_tez_container_size, total_mem_for_executors))


      # Updating calculated configs.
      normalized_tez_am_container_size = long(normalized_tez_am_container_size)
      putTezInteractiveSiteProperty('tez.am.resource.memory.mb', normalized_tez_am_container_size)
      Logger.info("'Tez for Hive2' config 'tez.am.resource.memory.mb' updated. Current: {0}".format(
        normalized_tez_am_container_size))

      if not llap_concurrency_in_changed_configs:
        min_llap_concurrency = 1
        putHiveInteractiveSiteProperty('hive.server2.tez.sessions.per.default.queue', llap_concurrency)
        putHiveInteractiveSitePropertyAttribute('hive.server2.tez.sessions.per.default.queue', "minimum",
                                                min_llap_concurrency)
        putHiveInteractiveSitePropertyAttribute('hive.server2.tez.sessions.per.default.queue', "maximum",
                                                LLAP_MAX_CONCURRENCY)
        Logger.info(
          "Hive2 config 'hive.server2.tez.sessions.per.default.queue' updated. Min : {0}, Current: {1}, Max: {2}" \
          .format(min_llap_concurrency, llap_concurrency, LLAP_MAX_CONCURRENCY))

      num_llap_nodes = long(num_llap_nodes)

      putHiveInteractiveEnvProperty('num_llap_nodes', num_llap_nodes)
      Logger.info("LLAP config 'num_llap_nodes' updated. Current: {0}".format(num_llap_nodes))

      llap_container_size = long(llap_container_size)
      putHiveInteractiveSiteProperty('hive.llap.daemon.yarn.container.mb', llap_container_size)
      Logger.info("LLAP config 'hive.llap.daemon.yarn.container.mb' updated. Current: {0}".format(llap_container_size))

      num_executors_per_node = long(num_executors_per_node)
      putHiveInteractiveSiteProperty('hive.llap.daemon.num.executors', num_executors_per_node)
      Logger.info("LLAP config 'hive.llap.daemon.num.executors' updated. Current: {0}".format(num_executors_per_node))
      # 'hive.llap.io.threadpool.size' config value is to be set same as value calculated for
      # 'hive.llap.daemon.num.executors' at all times.
      putHiveInteractiveSiteProperty('hive.llap.io.threadpool.size', num_executors_per_node)
      Logger.info("LLAP config 'hive.llap.io.threadpool.size' updated. Current: {0}".format(num_executors_per_node))

      cache_size_per_node = long(cache_size_per_node)
      putHiveInteractiveSiteProperty('hive.llap.io.memory.size', cache_size_per_node)
      Logger.info("LLAP config 'hive.llap.io.memory.size' updated. Current: {0}".format(cache_size_per_node))
      llap_io_enabled = 'false'
      if cache_size_per_node >= 64:
        llap_io_enabled = 'true'

      putHiveInteractiveSiteProperty('hive.llap.io.enabled', llap_io_enabled)
      Logger.info("Hive2 config 'hive.llap.io.enabled' updated to '{0}' as part of "
                  "'hive.llap.io.memory.size' calculation.".format(llap_io_enabled))

      llap_xmx = long(llap_xmx)
      putHiveInteractiveEnvProperty('llap_heap_size', llap_xmx)
      Logger.info("LLAP config 'llap_heap_size' updated. Current: {0}".format(llap_xmx))

      slider_am_container_size = long(slider_am_container_size)
      putHiveInteractiveEnvProperty('slider_am_container_mb', slider_am_container_size)
      Logger.info("LLAP config 'slider_am_container_mb' updated. Current: {0}".format(slider_am_container_size))

    except Exception as e:
      # Set default values, if caught an Exception. The 'llap queue capacity' is left untouched, as it can be increased,
      # triggerring recalculation.
      Logger.info(e.message+" Skipping calculating LLAP configs. Setting them to minimum values.")
      traceback.print_exc()

      try:
        yarn_min_container_size = long(self.get_yarn_min_container_size(services, configurations))
        slider_am_container_size = long(self.calculate_slider_am_size(yarn_min_container_size))

        node_manager_host_list = self.get_node_manager_hosts(services, hosts)
        node_manager_cnt = len(node_manager_host_list)

        putHiveInteractiveSiteProperty('hive.server2.tez.sessions.per.default.queue', 1)
        putHiveInteractiveSitePropertyAttribute('hive.server2.tez.sessions.per.default.queue', "minimum", 1)
        putHiveInteractiveSitePropertyAttribute('hive.server2.tez.sessions.per.default.queue', "maximum", 32)

        putHiveInteractiveEnvProperty('num_llap_nodes', 0)
        putHiveInteractiveEnvPropertyAttribute('num_llap_nodes', "minimum", 1)
        putHiveInteractiveEnvPropertyAttribute('num_llap_nodes', "maximum", node_manager_cnt)

        putHiveInteractiveSiteProperty('hive.llap.daemon.yarn.container.mb', yarn_min_container_size)
        putHiveInteractiveSitePropertyAttribute('hive.llap.daemon.yarn.container.mb', "minimum", yarn_min_container_size)

        putHiveInteractiveSiteProperty('hive.llap.daemon.num.executors', 0)
        putHiveInteractiveSitePropertyAttribute('hive.llap.daemon.num.executors', "minimum", 1)

        putHiveInteractiveSiteProperty('hive.llap.io.threadpool.size', 0)

        putHiveInteractiveSiteProperty('hive.llap.io.threadpool.size', 0)

        putHiveInteractiveSiteProperty('hive.llap.io.memory.size', 0)

        putHiveInteractiveEnvProperty('llap_heap_size', 0)

        putHiveInteractiveEnvProperty('slider_am_container_mb', slider_am_container_size)

      except Exception as e:
        Logger.info("Problem setting minimum values for LLAP configs in Exception code.")
        traceback.print_exc()

  """
  Checks for the presence of passed-in configuration properties in a given config, if they are changed.
  Reads from services["changed-configurations"].
  Parameters:
     services: Configuration information for the cluster
     config_type : Type of the configuration
     config_names_set : Set of configuration properties to be checked if they are changed.
     all_exists: If True : returns True only if all properties mentioned in 'config_names_set' we found
                           in services["changed-configurations"].
                           Otherwise, returns False.
                 If False : return True, if any of the properties mentioned in config_names_set we found in
                           services["changed-configurations"].
                           Otherwise, returns False.
  """
  def are_config_props_in_changed_configs(self, services, config_type, config_names_set, all_exists):
    changedConfigs = services["changed-configurations"]
    changed_config_names_set = set()
    for changedConfig in changedConfigs:
      if changedConfig['type'] == config_type:
        changed_config_names_set.update([changedConfig['name']])

    if changed_config_names_set is None:
      return False
    else:
      configs_intersection = changed_config_names_set.intersection(config_names_set)
      if all_exists:
        if configs_intersection == config_names_set:
          return True
      else:
        if len(configs_intersection) > 0 :
          return True
    return False

  """
  Returns all the Node Manager configs in cluster.
  """
  def get_node_manager_hosts(self, services, hosts):
    if len(hosts["items"]) > 0:
      node_manager_hosts = self.getHostsWithComponent("YARN", "NODEMANAGER", services, hosts)
      assert (node_manager_hosts is not None), "Information about NODEMANAGER not found in cluster."
      return node_manager_hosts

  """
  Returns the current LLAP queue capacity percentage value. (llap_queue_capacity)
  """
  def get_llap_cap_percent_slider(self, services, configurations):
    llap_slider_cap_percentage = 0
    if 'llap_queue_capacity' in services['configurations']['hive-interactive-env']['properties']:
      llap_slider_cap_percentage = float(
        services['configurations']['hive-interactive-env']['properties']['llap_queue_capacity'])
      Logger.error("'llap_queue_capacity' not present in services['configurations']['hive-interactive-env']['properties'].")
    if llap_slider_cap_percentage <= 0 :
      if 'hive-interactive-env' in configurations and \
          'llap_queue_capacity' in configurations["hive-interactive-env"]["properties"]:
        llap_slider_cap_percentage = float(configurations["hive-interactive-env"]["properties"]["llap_queue_capacity"])
    assert (llap_slider_cap_percentage > 0), "'llap_queue_capacity' is set to : {0}. Should be > 0.".format(llap_slider_cap_percentage)
    return llap_slider_cap_percentage


  """
  Returns current value of number of LLAP nodes in cluster (num_llap_nodes)
  """
  def get_num_llap_nodes(self, services):
    if 'num_llap_nodes' in services['configurations']['hive-interactive-env']['properties']:
      num_llap_nodes = float(
        services['configurations']['hive-interactive-env']['properties']['num_llap_nodes'])
      assert (num_llap_nodes > 0), "Number of LLAP nodes read : {0}. Expected value : > 0".format(
        num_llap_nodes)
      return num_llap_nodes
    else:
      raise Fail("Couldn't retrieve Hive Server interactive's 'num_llap_nodes' config.")

  """
  Gets HIVE Tez container size (hive.tez.container.size). Takes into account if it has been calculated as part of current
  Stack Advisor invocation.
  """
  def get_hive_tez_container_size(self, services, configurations):
    hive_container_size = None
    # Check if 'hive.tez.container.size' is modified in current ST invocation.
    if 'hive-site' in configurations and 'hive.tez.container.size' in configurations['hive-site']['properties']:
      hive_container_size = float(configurations['hive-site']['properties']['hive.tez.container.size'])
      Logger.info("'hive.tez.container.size' read from configurations as : {0}".format(hive_container_size))

    if not hive_container_size:
      # Check if 'hive.tez.container.size' is input in services array.
      if 'hive.tez.container.size' in services['configurations']['hive-site']['properties']:
        hive_container_size = float(services['configurations']['hive-site']['properties']['hive.tez.container.size'])
        Logger.info("'hive.tez.container.size' read from services as : {0}".format(hive_container_size))
    if not hive_container_size:
      raise Fail("Couldn't retrieve Hive Server 'hive.tez.container.size' config.")

    assert (hive_container_size > 0), "'hive.tez.container.size' current value : {0}. Expected value : > 0".format(
          hive_container_size)

    return hive_container_size

  """
  Gets HIVE Server Interactive's 'llap_headroom_space' config. (Default value set to 6144 bytes).
  """
  def get_llap_headroom_space(self, services, configurations):
    llap_headroom_space = None
    # Check if 'llap_headroom_space' is modified in current SA invocation.
    if 'hive-interactive-env' in configurations and 'llap_headroom_space' in configurations['hive-interactive-env']['properties']:
      hive_container_size = float(configurations['hive-interactive-env']['properties']['llap_headroom_space'])
      Logger.info("'llap_headroom_space' read from configurations as : {0}".format(llap_headroom_space))

    if not llap_headroom_space:
      # Check if 'llap_headroom_space' is input in services array.
      if 'llap_headroom_space' in services['configurations']['hive-interactive-env']['properties']:
        llap_headroom_space = float(services['configurations']['hive-interactive-env']['properties']['llap_headroom_space'])
        Logger.info("'llap_headroom_space' read from services as : {0}".format(llap_headroom_space))
    if not llap_headroom_space or llap_headroom_space < 1:
      llap_headroom_space = 6144 # 6GB
      Logger.info("Couldn't read 'llap_headroom_space' from services or configurations. Returing default value : 6144 bytes")

    return llap_headroom_space

  """
  Gets YARN's minimum container size (yarn.scheduler.minimum-allocation-mb).
  Reads from:
    - configurations (if changed as part of current Stack Advisor invocation (output)), and services["changed-configurations"]
      is empty, else
    - services['configurations'] (input).

  services["changed-configurations"] would be empty if Stack Advisor call is made from Blueprints (1st invocation). Subsequent
  Stack Advisor calls will have it non-empty. We do this because in subsequent invocations, even if Stack Advsior calculates this
  value (configurations), it is finally not recommended, making 'input' value to survive.
  """
  def get_yarn_min_container_size(self, services, configurations):
    yarn_min_container_size = None
    # Check if services["changed-configurations"] is empty and 'yarn.scheduler.minimum-allocation-mb' is modified in current ST invocation.
    if not services["changed-configurations"] and \
      'yarn-site' in configurations and 'yarn.scheduler.minimum-allocation-mb' in configurations['yarn-site']['properties']:
      yarn_min_container_size = float(configurations['yarn-site']['properties']['yarn.scheduler.minimum-allocation-mb'])
      Logger.info("'yarn.scheduler.minimum-allocation-mb' read from configurations as : {0}".format(yarn_min_container_size))

    if not yarn_min_container_size:
      # Check if 'yarn.scheduler.minimum-allocation-mb' is input in services array.
      if 'yarn-site' in services['configurations'] and \
          'yarn.scheduler.minimum-allocation-mb' in services['configurations']['yarn-site']['properties']:
        yarn_min_container_size = float(services['configurations']['yarn-site']['properties']['yarn.scheduler.minimum-allocation-mb'])
        Logger.info("'yarn.scheduler.minimum-allocation-mb' read from services as : {0}".format(yarn_min_container_size))

    if not yarn_min_container_size:
      raise Fail("Couldn't retrieve YARN's 'yarn.scheduler.minimum-allocation-mb' config.")

    assert (yarn_min_container_size > 0), "'yarn.scheduler.minimum-allocation-mb' current value : {0}. " \
                                            "Expected value : > 0".format(yarn_min_container_size)
    return yarn_min_container_size

  """
  Calculates the Slider App Master size based on YARN's Minimum Container Size.
  """
  def calculate_slider_am_size(self, yarn_min_container_size):
    if yarn_min_container_size > 1024:
      return 1024
    if yarn_min_container_size >= 256 and yarn_min_container_size <= 1024:
      return yarn_min_container_size
    if yarn_min_container_size < 256:
      return 256

  """
  Gets YARN NodeManager memory in MB (yarn.nodemanager.resource.memory-mb).
  Reads from:
    - configurations (if changed as part of current Stack Advisor invocation (output)), and services["changed-configurations"]
      is empty, else
    - services['configurations'] (input).

  services["changed-configurations"] would be empty is Stack Advisor call if made from Blueprints (1st invocation). Subsequent
  Stack Advisor calls will have it non-empty. We do this because in subsequent invocations, even if Stack Advsior calculates this
  value (configurations), it is finally not recommended, making 'input' value to survive.
  """
  def get_yarn_nm_mem_in_mb(self, services, configurations):
    yarn_nm_mem_in_mb = None

    # Check if services["changed-configurations"] is empty and 'yarn.nodemanager.resource.memory-mb' is modified in current ST invocation.
    if not services["changed-configurations"] and\
        'yarn-site' in configurations and 'yarn.nodemanager.resource.memory-mb' in configurations['yarn-site']['properties']:
      yarn_nm_mem_in_mb = float(configurations['yarn-site']['properties']['yarn.nodemanager.resource.memory-mb'])
      Logger.info("'yarn.nodemanager.resource.memory-mb' read from configurations as : {0}".format(yarn_nm_mem_in_mb))

    if not yarn_nm_mem_in_mb:
      # Check if 'yarn.nodemanager.resource.memory-mb' is input in services array.
      if 'yarn-site' in services['configurations'] and \
          'yarn.nodemanager.resource.memory-mb' in services['configurations']['yarn-site']['properties']:
        yarn_nm_mem_in_mb = float(services['configurations']['yarn-site']['properties']['yarn.nodemanager.resource.memory-mb'])
        Logger.info("'yarn.nodemanager.resource.memory-mb' read from services as : {0}".format(yarn_nm_mem_in_mb))

    if not yarn_nm_mem_in_mb:
      raise Fail("Couldn't retrieve YARN's 'yarn.nodemanager.resource.memory-mb' config.")

    assert (yarn_nm_mem_in_mb > 0.0), "'yarn.nodemanager.resource.memory-mb' current value : {0}. " \
                                      "Expected value : > 0".format(yarn_nm_mem_in_mb)

    return yarn_nm_mem_in_mb

  """
  Determines Tez App Master container size (tez.am.resource.memory.mb) for tez_hive2/tez-site based on total cluster capacity.
  """
  def calculate_tez_am_container_size(self, total_cluster_capacity):
    if total_cluster_capacity is None or not isinstance(total_cluster_capacity, long):
      raise Fail ("Passed-in 'Total Cluster Capacity' is : '{0}'".format(total_cluster_capacity))

    if total_cluster_capacity <= 0:
      raise Fail ("Passed-in 'Total Cluster Capacity' ({0}) is Invalid.".format(total_cluster_capacity))
    if total_cluster_capacity <= 4096:
      return 256
    elif total_cluster_capacity > 4096 and total_cluster_capacity <= 73728:
      return 512
    elif total_cluster_capacity > 73728:
      return 1536


  """
  Calculate minimum queue capacity required in order to get LLAP and HIVE2 app into running state.
  """
  def min_queue_perc_reqd_for_llap_and_hive_app(self, services, hosts, configurations):
    # Get queue size if sized at 20%
    node_manager_hosts = self.get_node_manager_hosts(services, hosts)
    yarn_rm_mem_in_mb = self.get_yarn_nm_mem_in_mb(services, configurations)
    total_cluster_cap = len(node_manager_hosts) * yarn_rm_mem_in_mb
    total_queue_size_at_20_perc = 20.0 / 100 * total_cluster_cap

    # Calculate based on minimum size required by containers.
    yarn_min_container_size = self.get_yarn_min_container_size(services, configurations)
    slider_am_size = self.calculate_slider_am_size(yarn_min_container_size)
    hive_tez_container_size = self.get_hive_tez_container_size(services, configurations)
    tez_am_container_size = self.calculate_tez_am_container_size(long(total_cluster_cap))
    normalized_val = self._normalizeUp(slider_am_size, yarn_min_container_size) + self._normalizeUp\
      (hive_tez_container_size, yarn_min_container_size) + self._normalizeUp(tez_am_container_size, yarn_min_container_size)

    min_required = max(total_queue_size_at_20_perc, normalized_val)

    min_required_perc = min_required * 100 / total_cluster_cap
    Logger.info("Calculated 'min_queue_perc_required_in_cluster' : {0}% and 'min_queue_cap_required_in_cluster': {1} "
                "for LLAP and HIVE2 app using following : yarn_min_container_size : {2}, slider_am_size : {3}, hive_tez_container_size : {4}, "
                "hive_am_container_size : {5}, total_cluster_cap : {6}, yarn_rm_mem_in_mb : {7}"
                "".format(min_required_perc, min_required, yarn_min_container_size, slider_am_size, hive_tez_container_size,
                          tez_am_container_size, total_cluster_cap, yarn_rm_mem_in_mb))
    return int(math.ceil(min_required_perc))

  """
  Normalize down 'val2' with respect to 'val1'.
  """
  def _normalizeDown(self, val1, val2):
    tmp = math.floor(val1 / val2)
    if tmp < 1.00:
      return val1
    return tmp * val2

  """
  Normalize up 'val2' with respect to 'val1'.
  """
  def _normalizeUp(self, val1, val2):
    tmp = math.ceil(val1 / val2)
    return tmp * val2

  """
  Checks and (1). Creates 'llap' queue if only 'default' queue exist at leaf level and is consuming 100% capacity OR
             (2). Updates 'llap' queue capacity and state, if current selected queue is 'llap', and only 2 queues exist
                  at root level : 'default' and 'llap'.
  """
  def checkAndManageLlapQueue(self, services, configurations, hosts, llap_queue_name):
    Logger.info("Determining creation/adjustment of 'capacity-scheduler' for 'llap' queue.")
    putHiveInteractiveEnvProperty = self.putProperty(configurations, "hive-interactive-env", services)
    putHiveInteractiveSiteProperty = self.putProperty(configurations, self.HIVE_INTERACTIVE_SITE, services)
    putHiveInteractiveEnvPropertyAttribute = self.putPropertyAttribute(configurations, "hive-interactive-env")
    putCapSchedProperty = self.putProperty(configurations, "capacity-scheduler", services)
    leafQueueNames = None

    capacity_scheduler_properties, received_as_key_value_pair = self.getCapacitySchedulerProperties(services)
    if capacity_scheduler_properties:
      leafQueueNames = self.getAllYarnLeafQueues(capacity_scheduler_properties)
      # Get the llap Cluster percentage used for 'llap' Queue creation
      if 'llap_queue_capacity' in services['configurations']['hive-interactive-env']['properties']:
        llap_slider_cap_percentage = int(
          services['configurations']['hive-interactive-env']['properties']['llap_queue_capacity'])
        min_reqd_queue_cap_perc = self.min_queue_perc_reqd_for_llap_and_hive_app(services, hosts, configurations)
        if min_reqd_queue_cap_perc > 100:
          min_reqd_queue_cap_perc = 100
          Logger.info("Received 'Minimum Required LLAP queue capacity' : {0}% (out of bounds), adjusted it to : 100%".format(min_reqd_queue_cap_perc))

        # Adjust 'llap' queue capacity slider value to be minimum required if out of expected bounds.
        if llap_slider_cap_percentage <= 0 or llap_slider_cap_percentage > 100:
          Logger.info("Adjusting HIVE 'llap_queue_capacity' from {0}% (invalid size) to {1}%".format(llap_slider_cap_percentage, min_reqd_queue_cap_perc))
          putHiveInteractiveEnvProperty('llap_queue_capacity', min_reqd_queue_cap_perc)
          llap_slider_cap_percentage = min_reqd_queue_cap_perc
      else:
        Logger.error("Problem retrieving LLAP Queue Capacity. Skipping creating {0} queue".format(llap_queue_name))
        return

      cap_sched_config_keys = capacity_scheduler_properties.keys()

      yarn_default_queue_capacity = -1
      if 'yarn.scheduler.capacity.root.default.capacity' in cap_sched_config_keys:
        yarn_default_queue_capacity = capacity_scheduler_properties.get('yarn.scheduler.capacity.root.default.capacity')

      # Get 'llap' queue state
      currLlapQueueState = ''
      if 'yarn.scheduler.capacity.root.'+llap_queue_name+'.state' in cap_sched_config_keys:
        currLlapQueueState = capacity_scheduler_properties.get('yarn.scheduler.capacity.root.'+llap_queue_name+'.state')

      # Get 'llap' queue capacity
      currLlapQueueCap = -1
      if 'yarn.scheduler.capacity.root.'+llap_queue_name+'.capacity' in cap_sched_config_keys:
        currLlapQueueCap = int(float(capacity_scheduler_properties.get('yarn.scheduler.capacity.root.'+llap_queue_name+'.capacity')))

      if self.HIVE_INTERACTIVE_SITE in services['configurations'] and \
          'hive.llap.daemon.queue.name' in services['configurations'][self.HIVE_INTERACTIVE_SITE]['properties']:
        llap_daemon_selected_queue_name =  services['configurations'][self.HIVE_INTERACTIVE_SITE]['properties']['hive.llap.daemon.queue.name']
      else:
        Logger.error("Couldn't retrive 'hive.llap.daemon.queue.name' property. Skipping adjusting queues.")
        return
      updated_cap_sched_configs_str = ''

      enabled_hive_int_in_changed_configs = self.are_config_props_in_changed_configs(services, "hive-interactive-env",
                                                                                   set(['enable_hive_interactive']), False)
      """
      We create OR "modify 'llap' queue 'state and/or capacity' " based on below conditions:
       - if only 1 queue exists at root level and is 'default' queue and has 100% cap -> Create 'llap' queue,  OR
       - if 2 queues exists at root level ('llap' and 'default') :
           - Queue selected is 'llap' and state is STOPPED -> Modify 'llap' queue state to RUNNING, adjust capacity, OR
           - Queue selected is 'llap', state is RUNNING and 'llap_queue_capacity' prop != 'llap' queue current running capacity ->
              Modify 'llap' queue capacity to 'llap_queue_capacity'
      """
      if 'default' in leafQueueNames and \
        ((len(leafQueueNames) == 1 and int(yarn_default_queue_capacity) == 100) or \
        ((len(leafQueueNames) == 2 and llap_queue_name in leafQueueNames) and \
           ((currLlapQueueState == 'STOPPED' and enabled_hive_int_in_changed_configs) or (currLlapQueueState == 'RUNNING' and currLlapQueueCap != llap_slider_cap_percentage)))):
        adjusted_default_queue_cap = str(100 - llap_slider_cap_percentage)

        hive_user = '*'  # Open to all
        if 'hive_user' in services['configurations']['hive-env']['properties']:
          hive_user = services['configurations']['hive-env']['properties']['hive_user']

        llap_slider_cap_percentage = str(llap_slider_cap_percentage)

        # If capacity-scheduler configs are received as one concatenated string, we deposit the changed configs back as
        # one concatenated string.
        updated_cap_sched_configs_as_dict = False
        if not received_as_key_value_pair:
          for prop, val in capacity_scheduler_properties.items():
            if llap_queue_name not in prop:
              if prop == 'yarn.scheduler.capacity.root.queues':
                updated_cap_sched_configs_str = updated_cap_sched_configs_str \
                                            + prop + "=default,llap\n"
              elif prop == 'yarn.scheduler.capacity.root.default.capacity':
                updated_cap_sched_configs_str = updated_cap_sched_configs_str \
                                            + prop + "=" + adjusted_default_queue_cap + "\n"
              elif prop == 'yarn.scheduler.capacity.root.default.maximum-capacity':
                updated_cap_sched_configs_str = updated_cap_sched_configs_str \
                                            + prop + "=" + adjusted_default_queue_cap + "\n"
              elif prop.startswith('yarn.') and '.llap.' not in prop:
                updated_cap_sched_configs_str = updated_cap_sched_configs_str + prop + "=" + val + "\n"

          # Now, append the 'llap' queue related properties
          updated_cap_sched_configs_str = updated_cap_sched_configs_str \
                                      + "yarn.scheduler.capacity.root." + llap_queue_name + ".user-limit-factor=1\n" \
                                      + "yarn.scheduler.capacity.root." + llap_queue_name + ".state=RUNNING\n" \
                                      + "yarn.scheduler.capacity.root." + llap_queue_name + ".ordering-policy=fifo\n" \
                                      + "yarn.scheduler.capacity.root." + llap_queue_name + ".minimum-user-limit-percent=100\n" \
                                      + "yarn.scheduler.capacity.root." + llap_queue_name + ".maximum-capacity=" \
                                      + llap_slider_cap_percentage + "\n" \
                                      + "yarn.scheduler.capacity.root." + llap_queue_name + ".capacity=" \
                                      + llap_slider_cap_percentage + "\n" \
                                      + "yarn.scheduler.capacity.root." + llap_queue_name + ".acl_submit_applications=" \
                                      + hive_user + "\n" \
                                      + "yarn.scheduler.capacity.root." + llap_queue_name + ".acl_administer_queue=" \
                                      + hive_user + "\n" \
                                      + "yarn.scheduler.capacity.root." + llap_queue_name + ".maximum-am-resource-percent=1"

          putCapSchedProperty("capacity-scheduler", updated_cap_sched_configs_str)
          Logger.info("Updated 'capacity-scheduler' configs as one concatenated string.")
        else:
          # If capacity-scheduler configs are received as a  dictionary (generally 1st time), we deposit the changed
          # values back as dictionary itself.
          # Update existing configs in 'capacity-scheduler'.
          for prop, val in capacity_scheduler_properties.items():
            if llap_queue_name not in prop:
              if prop == 'yarn.scheduler.capacity.root.queues':
                putCapSchedProperty(prop, 'default,llap')
              elif prop == 'yarn.scheduler.capacity.root.default.capacity':
                putCapSchedProperty(prop, adjusted_default_queue_cap)
              elif prop == 'yarn.scheduler.capacity.root.default.maximum-capacity':
                putCapSchedProperty(prop, adjusted_default_queue_cap)
              elif prop.startswith('yarn.') and '.llap.' not in prop:
                putCapSchedProperty(prop, val)

          # Add new 'llap' queue related configs.
          putCapSchedProperty("yarn.scheduler.capacity.root." + llap_queue_name + ".user-limit-factor", "1")
          putCapSchedProperty("yarn.scheduler.capacity.root." + llap_queue_name + ".state", "RUNNING")
          putCapSchedProperty("yarn.scheduler.capacity.root." + llap_queue_name + ".ordering-policy", "fifo")
          putCapSchedProperty("yarn.scheduler.capacity.root." + llap_queue_name + ".minimum-user-limit-percent", "100")
          putCapSchedProperty("yarn.scheduler.capacity.root." + llap_queue_name + ".maximum-capacity", llap_slider_cap_percentage)
          putCapSchedProperty("yarn.scheduler.capacity.root." + llap_queue_name + ".capacity", llap_slider_cap_percentage)
          putCapSchedProperty("yarn.scheduler.capacity.root." + llap_queue_name + ".acl_submit_applications", hive_user)
          putCapSchedProperty("yarn.scheduler.capacity.root." + llap_queue_name + ".acl_administer_queue", hive_user)
          putCapSchedProperty("yarn.scheduler.capacity.root." + llap_queue_name + ".maximum-am-resource-percent", "1")


          Logger.info("Updated 'capacity-scheduler' configs as a dictionary.")
          updated_cap_sched_configs_as_dict = True

        if updated_cap_sched_configs_str or updated_cap_sched_configs_as_dict:
          if len(leafQueueNames) == 1: # 'llap' queue didn't exist before
            Logger.info("Created YARN Queue : '{0}' with capacity : {1}%. Adjusted 'default' queue capacity to : {2}%" \
                      .format(llap_queue_name, llap_slider_cap_percentage, adjusted_default_queue_cap))
          else: # Queue existed, only adjustments done.
            Logger.info("Adjusted YARN Queue : '{0}'. Current capacity : {1}%. State: RUNNING.".format(llap_queue_name, llap_slider_cap_percentage))
            Logger.info("Adjusted 'default' queue capacity to : {0}%".format(adjusted_default_queue_cap))

          # Update Hive 'hive.llap.daemon.queue.name' prop to use 'llap' queue.
          putHiveInteractiveSiteProperty('hive.llap.daemon.queue.name', llap_queue_name)
          putHiveInteractiveSiteProperty('hive.server2.tez.default.queues', llap_queue_name)
          putHiveInteractiveEnvPropertyAttribute('llap_queue_capacity', "minimum", min_reqd_queue_cap_perc)
          putHiveInteractiveEnvPropertyAttribute('llap_queue_capacity', "maximum", 100)

          # Update 'hive.llap.daemon.queue.name' prop combo entries and llap capacity slider visibility.
          self.setLlapDaemonQueuePropAttributesAndCapSliderVisibility(services, configurations)
      else:
        Logger.debug("Not creating/adjusting {0} queue. Current YARN queues : {1}".format(llap_queue_name, list(leafQueueNames)))
    else:
      Logger.error("Couldn't retrieve 'capacity-scheduler' properties while doing YARN queue adjustment for Hive Server Interactive.")

  """
  Checks and sees (1). If only two leaf queues exist at root level, namely: 'default' and 'llap',
              and (2). 'llap' is in RUNNING state.

  If yes, performs the following actions:   (1). 'llap' queue state set to STOPPED,
                                            (2). 'llap' queue capacity set to 0 %,
                                            (3). 'default' queue capacity set to 100 %
  """
  def checkAndStopLlapQueue(self, services, configurations, llap_queue_name):
    putCapSchedProperty = self.putProperty(configurations, "capacity-scheduler", services)
    putHiveInteractiveSiteProperty = self.putProperty(configurations, self.HIVE_INTERACTIVE_SITE, services)
    capacity_scheduler_properties, received_as_key_value_pair = self.getCapacitySchedulerProperties(services)
    updated_default_queue_configs = ''
    updated_llap_queue_configs = ''
    if capacity_scheduler_properties:
      # Get all leaf queues.
      leafQueueNames = self.getAllYarnLeafQueues(capacity_scheduler_properties)

      if len(leafQueueNames) == 2 and llap_queue_name in leafQueueNames and 'default' in leafQueueNames:
        # Get 'llap' queue state
        currLlapQueueState = 'STOPPED'
        if 'yarn.scheduler.capacity.root.'+llap_queue_name+'.state' in capacity_scheduler_properties.keys():
          currLlapQueueState = capacity_scheduler_properties.get('yarn.scheduler.capacity.root.'+llap_queue_name+'.state')
        else:
          Logger.error("{0} queue 'state' property not present in capacity scheduler. Skipping adjusting queues.".format(llap_queue_name))
          return
        if currLlapQueueState == 'RUNNING':
          DEFAULT_MAX_CAPACITY = '100'
          for prop, val in capacity_scheduler_properties.items():
            # Update 'default' related configs in 'updated_default_queue_configs'
            if llap_queue_name not in prop:
              if prop == 'yarn.scheduler.capacity.root.default.capacity':
                # Set 'default' capacity back to maximum val
                updated_default_queue_configs = updated_default_queue_configs \
                                            + prop + "="+DEFAULT_MAX_CAPACITY + "\n"
              elif prop == 'yarn.scheduler.capacity.root.default.maximum-capacity':
                # Set 'default' max. capacity back to maximum val
                updated_default_queue_configs = updated_default_queue_configs \
                                            + prop + "="+DEFAULT_MAX_CAPACITY + "\n"
              elif prop.startswith('yarn.'):
                updated_default_queue_configs = updated_default_queue_configs + prop + "=" + val + "\n"
            else: # Update 'llap' related configs in 'updated_llap_queue_configs'
              if prop == 'yarn.scheduler.capacity.root.'+llap_queue_name+'.state':
                updated_llap_queue_configs = updated_llap_queue_configs \
                                            + prop + "=STOPPED\n"
              elif prop == 'yarn.scheduler.capacity.root.'+llap_queue_name+'.capacity':
                updated_llap_queue_configs = updated_llap_queue_configs \
                                            + prop + "=0\n"
              elif prop == 'yarn.scheduler.capacity.root.'+llap_queue_name+'.maximum-capacity':
                updated_llap_queue_configs = updated_llap_queue_configs \
                                            + prop + "=0\n"
              elif prop.startswith('yarn.'):
                updated_llap_queue_configs = updated_llap_queue_configs + prop + "=" + val + "\n"
        else:
          Logger.debug("{0} queue state is : {1}. Skipping adjusting queues.".format(llap_queue_name, currLlapQueueState))
          return

        if updated_default_queue_configs and updated_llap_queue_configs:
          putCapSchedProperty("capacity-scheduler", updated_default_queue_configs+updated_llap_queue_configs)
          Logger.info("Changed YARN '{0}' queue state to 'STOPPED', and capacity to 0%. Adjusted 'default' queue capacity to : {1}%" \
            .format(llap_queue_name, DEFAULT_MAX_CAPACITY))

          # Update Hive 'hive.llap.daemon.queue.name' prop to use 'default' queue.
          putHiveInteractiveSiteProperty('hive.llap.daemon.queue.name', self.YARN_ROOT_DEFAULT_QUEUE_NAME)
          putHiveInteractiveSiteProperty('hive.server2.tez.default.queues', self.YARN_ROOT_DEFAULT_QUEUE_NAME)
      else:
        Logger.debug("Not removing '{0}' queue as number of Queues not equal to 2. Current YARN queues : {1}".format(llap_queue_name, list(leafQueueNames)))
    else:
      Logger.error("Couldn't retrieve 'capacity-scheduler' properties while doing YARN queue adjustment for Hive Server Interactive.")

  """
  Checks and sets the 'Hive Server Interactive' 'hive.llap.daemon.queue.name' config Property Attributes.  Takes into
  account that 'capacity-scheduler' may have changed (got updated) in current Stack Advisor invocation.

  Also, updates the 'llap_queue_capacity' slider visibility.
  """
  def setLlapDaemonQueuePropAttributesAndCapSliderVisibility(self, services, configurations):
    Logger.info("Determining 'hive.llap.daemon.queue.name' config Property Attributes.")
    putHiveInteractiveSitePropertyAttribute = self.putPropertyAttribute(configurations, self.HIVE_INTERACTIVE_SITE)
    putHiveInteractiveEnvPropertyAttribute = self.putPropertyAttribute(configurations, "hive-interactive-env")

    capacity_scheduler_properties = dict()

    # Read 'capacity-scheduler' from configurations if we modified and added recommendation to it, as part of current
    # StackAdvisor invocation.
    if "capacity-scheduler" in configurations:
        cap_sched_props_as_dict = configurations["capacity-scheduler"]["properties"]
        if 'capacity-scheduler' in cap_sched_props_as_dict:
          cap_sched_props_as_str = configurations['capacity-scheduler']['properties']['capacity-scheduler']
          if cap_sched_props_as_str:
            cap_sched_props_as_str = str(cap_sched_props_as_str).split('\n')
            if len(cap_sched_props_as_str) > 0 and cap_sched_props_as_str[0] != 'null':
              # Got 'capacity-scheduler' configs as one "\n" separated string
              for property in cap_sched_props_as_str:
                key, sep, value = property.partition("=")
                capacity_scheduler_properties[key] = value
              Logger.info("'capacity-scheduler' configs is set as a single '\\n' separated string in current invocation. "
                          "count(configurations['capacity-scheduler']['properties']['capacity-scheduler']) = "
                          "{0}".format(len(capacity_scheduler_properties)))
            else:
              Logger.info("Read configurations['capacity-scheduler']['properties']['capacity-scheduler'] is : {0}".format(cap_sched_props_as_str))
          else:
            Logger.info("configurations['capacity-scheduler']['properties']['capacity-scheduler'] : {0}.".format(cap_sched_props_as_str))

        # if 'capacity_scheduler_properties' is empty, implies we may have 'capacity-scheduler' configs as dictionary
        # in configurations, if 'capacity-scheduler' changed in current invocation.
        if not capacity_scheduler_properties:
          if isinstance(cap_sched_props_as_dict, dict) and len(cap_sched_props_as_dict) > 1:
            capacity_scheduler_properties = cap_sched_props_as_dict
            Logger.info("'capacity-scheduler' changed in current Stack Advisor invocation. Retrieved the configs as dictionary from configurations.")
          else:
            Logger.info("Read configurations['capacity-scheduler']['properties'] is : {0}".format(cap_sched_props_as_dict))
    else:
      Logger.info("'capacity-scheduler' not modified in the current Stack Advisor invocation.")


    # if 'capacity_scheduler_properties' is still empty, implies 'capacity_scheduler' wasn't change in current
    # SA invocation. Thus, read it from input : 'services'.
    if not capacity_scheduler_properties:
      capacity_scheduler_properties, received_as_key_value_pair = self.getCapacitySchedulerProperties(services)
      Logger.info("'capacity-scheduler' not changed in current Stack Advisor invocation. Retrieved the configs from services.")

    # Get set of current YARN leaf queues.
    leafQueueNames = self.getAllYarnLeafQueues(capacity_scheduler_properties)
    if leafQueueNames:
      leafQueues = [{"label": str(queueName), "value": queueName} for queueName in leafQueueNames]
      leafQueues = sorted(leafQueues, key=lambda q: q['value'])
      putHiveInteractiveSitePropertyAttribute("hive.llap.daemon.queue.name", "entries", leafQueues)
      Logger.info("'hive.llap.daemon.queue.name' config Property Attributes set to : {0}".format(leafQueues))

      # Update 'llap_queue_capacity' slider visibility to 'true' if current selected queue in 'hive.llap.daemon.queue.name'
      # is 'llap', else 'false'.
      llap_daemon_selected_queue_name = None
      llap_queue_selected_in_current_call =  None
      if self.HIVE_INTERACTIVE_SITE in services['configurations'] and \
          'hive.llap.daemon.queue.name' in services['configurations'][self.HIVE_INTERACTIVE_SITE]['properties']:
        llap_daemon_selected_queue_name =  services['configurations'][self.HIVE_INTERACTIVE_SITE]['properties']['hive.llap.daemon.queue.name']

      if self.HIVE_INTERACTIVE_SITE in configurations and \
          'hive.llap.daemon.queue.name' in configurations[self.HIVE_INTERACTIVE_SITE]['properties']:
        llap_queue_selected_in_current_call = configurations[self.HIVE_INTERACTIVE_SITE]['properties']['hive.llap.daemon.queue.name']

      # Check to see if only 2 queues exist at root level : 'default' and 'llap' and current selected queue in 'hive.llap.daemon.queue.name'
      # is 'llap'.
      if len(leafQueueNames) == 2 and \
        ((llap_daemon_selected_queue_name != None and llap_daemon_selected_queue_name == 'llap') or \
        (llap_queue_selected_in_current_call != None and llap_queue_selected_in_current_call == 'llap')):
        putHiveInteractiveEnvPropertyAttribute("llap_queue_capacity", "visible", "true")
        Logger.info("Setting LLAP queue capacity slider visibility to 'True'.")
      else:
        putHiveInteractiveEnvPropertyAttribute("llap_queue_capacity", "visible", "false")
        Logger.info("Setting LLAP queue capacity slider visibility to 'False'.")
    else:
      Logger.error("Problem retrieving YARN queues. Skipping updating HIVE Server Interactve "
                   "'hive.server2.tez.default.queues' property attributes.")

  """
  Retrieves the passed in queue's 'capacity' related key from Capacity Scheduler.
  """
  def __getQueueCapacityKeyFromCapacityScheduler(self, capacity_scheduler_properties, llap_daemon_selected_queue_name):
    # Identify the key which contains the capacity for 'llap_daemon_selected_queue_name'.
    cap_sched_keys = capacity_scheduler_properties.keys()
    llap_selected_queue_cap_key =  None
    current_selected_queue_for_llap_cap = None
    for key in cap_sched_keys:
      # Expected capacity prop key is of form : 'yarn.scheduler.capacity.<one or more queues in path separated by '.'>.[llap_daemon_selected_queue_name].capacity'
      if key.endswith(llap_daemon_selected_queue_name+".capacity"):
        llap_selected_queue_cap_key = key
        break;
    return llap_selected_queue_cap_key

  """
  Retrieves the passed in queue's 'state' from Capacity Scheduler.
  """
  def __getQueueStateFromCapacityScheduler(self, capacity_scheduler_properties, llap_daemon_selected_queue_name):
    # Identify the key which contains the state for 'llap_daemon_selected_queue_name'.
    cap_sched_keys = capacity_scheduler_properties.keys()
    llap_selected_queue_state_key =  None
    llap_selected_queue_state = None
    for key in cap_sched_keys:
      if key.endswith(llap_daemon_selected_queue_name+".state"):
        llap_selected_queue_state_key = key
        break;
    llap_selected_queue_state = capacity_scheduler_properties.get(llap_selected_queue_state_key)
    return llap_selected_queue_state

  """
  Calculates the total available capacity for the passed-in YARN queue of any level based on the percentages.
  """
  def __getSelectedQueueTotalCap(self, capacity_scheduler_properties, llap_daemon_selected_queue_name, total_cluster_capacity):
    Logger.info("Entered __getSelectedQueueTotalCap fn().")
    available_capacity = total_cluster_capacity
    queue_cap_key = self.__getQueueCapacityKeyFromCapacityScheduler(capacity_scheduler_properties, llap_daemon_selected_queue_name)
    if queue_cap_key:
      queue_cap_key = queue_cap_key.strip()
      if len(queue_cap_key) >= 34: # len('yarn.scheduler.capacity.<single letter queue name>.capacity') = 34
        # Expected capacity prop key is of form : 'yarn.scheduler.capacity.<one or more queues (path)>.capacity'
        queue_path = queue_cap_key[24:] # Strip from beginning 'yarn.scheduler.capacity.'
        queue_path = queue_path[0:-9] # Strip from end '.capacity'
        queues_list = queue_path.split('.')
        Logger.info("Queue list : {0}".format(queues_list))
        if queues_list:
          for queue in queues_list:
            queue_cap_key = self.__getQueueCapacityKeyFromCapacityScheduler(capacity_scheduler_properties, queue)
            queue_cap_perc = float(capacity_scheduler_properties.get(queue_cap_key))
            available_capacity = queue_cap_perc / 100 * available_capacity
            Logger.info("Total capacity available for queue {0} is : {1}".format(queue, available_capacity))
        else:
          raise Fail("Retrieved 'queue list' from capacity-scheduler is empty while doing '{0}' queue capacity caluclation."
                     .format(llap_daemon_selected_queue_name))
      else:
        raise Fail("Expected length for queue_cap_key(val:{0}) should be greater than atleast 34.".format(queue_cap_key))
    else:
      raise Fail("Couldn't retrieve {0}' queue capacity KEY from capacity-scheduler while doing capacity caluclation.".format(llap_daemon_selected_queue_name))
    # returns the capacity calculated for passed-in queue in 'llap_daemon_selected_queue_name'.
    return available_capacity

  def recommendRangerKMSConfigurations(self, configurations, clusterData, services, hosts):
    super(HDP25StackAdvisor, self).recommendRangerKMSConfigurations(configurations, clusterData, services, hosts)
    servicesList = [service["StackServices"]["service_name"] for service in services["services"]]
    putRangerKmsSiteProperty = self.putProperty(configurations, "kms-site", services)

    if 'ranger-env' in services['configurations'] and 'ranger_user' in services['configurations']['ranger-env']['properties']:
      rangerUser = services['configurations']['ranger-env']['properties']['ranger_user']

      if 'kms-site' in services['configurations'] and 'KERBEROS' in servicesList:
        putRangerKmsSiteProperty('hadoop.kms.proxyuser.{0}.groups'.format(rangerUser), '*')
        putRangerKmsSiteProperty('hadoop.kms.proxyuser.{0}.hosts'.format(rangerUser), '*')
        putRangerKmsSiteProperty('hadoop.kms.proxyuser.{0}.users'.format(rangerUser), '*')

  def recommendRangerConfigurations(self, configurations, clusterData, services, hosts):
    super(HDP25StackAdvisor, self).recommendRangerConfigurations(configurations, clusterData, services, hosts)
    servicesList = [service["StackServices"]["service_name"] for service in services["services"]]
    has_ranger_tagsync = False

    putTagsyncAppProperty = self.putProperty(configurations, "tagsync-application-properties", services)
    putTagsyncSiteProperty = self.putProperty(configurations, "ranger-tagsync-site", services)
    putRangerAdminProperty = self.putProperty(configurations, "ranger-admin-site", services)
    putRangerEnvProperty = self.putProperty(configurations, "ranger-env", services)
    if self.RANGER_TAGSYNC_SITE in services['configurations']:
      ranger_tagsync_host = self.__getHostsForComponent(services, "RANGER", "RANGER_TAGSYNC")
      has_ranger_tagsync = len(ranger_tagsync_host) > 0

    if 'ATLAS' in servicesList and has_ranger_tagsync:
      atlas_hosts = self.getHostNamesWithComponent("ATLAS", "ATLAS_SERVER", services)
      atlas_host = 'localhost' if len(atlas_hosts) == 0 else atlas_hosts[0]
      protocol = 'http'
      atlas_port = '21000'

      if 'application-properties' in services['configurations'] and 'atlas.enableTLS' in services['configurations']['application-properties']['properties'] \
        and services['configurations']['application-properties']['properties']['atlas.enableTLS'].lower() == 'true':
        protocol = 'https'
        if 'application-properties' in services['configurations'] and 'atlas.server.https.port' in services['configurations']['application-properties']['properties']:
          atlas_port = services['configurations']['application-properties']['properties']['atlas.server.https.port']
      else:
        protocol = 'http'
        if 'application-properties' in services['configurations'] and 'atlas.server.http.port' in services['configurations']['application-properties']['properties']:
          atlas_port = services['configurations']['application-properties']['properties']['atlas.server.http.port']

      atlas_rest_endpoint = '{0}://{1}:{2}'.format(protocol, atlas_host, atlas_port)

      putTagsyncSiteProperty('ranger.tagsync.source.atlas', 'true')
      putTagsyncSiteProperty('ranger.tagsync.source.atlasrest.endpoint', atlas_rest_endpoint)

    zookeeper_host_port = self.getZKHostPortString(services)
    #if zookeeper_host_port and has_ranger_tagsync:
      #putTagsyncAppProperty('atlas.kafka.zookeeper.connect', zookeeper_host_port)

    if 'KAFKA' in servicesList and has_ranger_tagsync:
      kafka_hosts = self.getHostNamesWithComponent("KAFKA", "KAFKA_BROKER", services)
      kafka_port = '6667'
      if 'kafka-broker' in services['configurations'] and (
          'port' in services['configurations']['kafka-broker']['properties']):
        kafka_port = services['configurations']['kafka-broker']['properties']['port']
      kafka_host_port = []
      for i in range(len(kafka_hosts)):
        kafka_host_port.append(kafka_hosts[i] + ':' + kafka_port)

      final_kafka_host = ",".join(kafka_host_port)
      #putTagsyncAppProperty('atlas.kafka.bootstrap.servers', final_kafka_host)

    is_solr_cloud_enabled = False
    if 'ranger-env' in services['configurations'] and 'is_solrCloud_enabled' in services['configurations']['ranger-env']['properties']:
      is_solr_cloud_enabled = services['configurations']['ranger-env']['properties']['is_solrCloud_enabled']  == 'true'

    is_external_solr_cloud_enabled = False
    if 'ranger-env' in services['configurations'] and 'is_external_solrCloud_enabled' in services['configurations']['ranger-env']['properties']:
      is_external_solr_cloud_enabled = services['configurations']['ranger-env']['properties']['is_external_solrCloud_enabled']  == 'true'

    ranger_audit_zk_port = ''

    if 'AMBARI_INFRA' in servicesList and zookeeper_host_port and is_solr_cloud_enabled and not is_external_solr_cloud_enabled:
      zookeeper_host_port = zookeeper_host_port.split(',')
      zookeeper_host_port.sort()
      zookeeper_host_port = ",".join(zookeeper_host_port)
      infra_solr_znode = '/infra-solr'

      if 'infra-solr-env' in services['configurations'] and \
        ('infra_solr_znode' in services['configurations']['infra-solr-env']['properties']):
        infra_solr_znode = services['configurations']['infra-solr-env']['properties']['infra_solr_znode']
        ranger_audit_zk_port = '{0}{1}'.format(zookeeper_host_port, infra_solr_znode)
      putRangerAdminProperty('ranger.audit.solr.zookeepers', ranger_audit_zk_port)
    elif zookeeper_host_port and is_solr_cloud_enabled and is_external_solr_cloud_enabled:
      ranger_audit_zk_port = '{0}/{1}'.format(zookeeper_host_port, 'ranger_audits')
      putRangerAdminProperty('ranger.audit.solr.zookeepers', ranger_audit_zk_port)
    else:
      putRangerAdminProperty('ranger.audit.solr.zookeepers', 'NONE')

    ranger_services = [
      {'service_name': 'HDFS', 'audit_file': 'ranger-hdfs-audit'},
      {'service_name': 'YARN', 'audit_file': 'ranger-yarn-audit'},
      {'service_name': 'HBASE', 'audit_file': 'ranger-hbase-audit'},
      {'service_name': 'HIVE', 'audit_file': 'ranger-hive-audit'},
      {'service_name': 'KNOX', 'audit_file': 'ranger-knox-audit'},
      {'service_name': 'KAFKA', 'audit_file': 'ranger-kafka-audit'},
      {'service_name': 'STORM', 'audit_file': 'ranger-storm-audit'},
      {'service_name': 'RANGER_KMS', 'audit_file': 'ranger-kms-audit'},
      {'service_name': 'ATLAS', 'audit_file': 'ranger-atlas-audit'}
    ]

    for item in range(len(ranger_services)):
      if ranger_services[item]['service_name'] in servicesList:
        component_audit_file =  ranger_services[item]['audit_file']
        if component_audit_file in services["configurations"]:
          ranger_audit_dict = [
            {'filename': 'ranger-admin-site', 'configname': 'ranger.audit.solr.urls', 'target_configname': 'xasecure.audit.destination.solr.urls'},
            {'filename': 'ranger-admin-site', 'configname': 'ranger.audit.solr.zookeepers', 'target_configname': 'xasecure.audit.destination.solr.zookeepers'}
          ]
          putRangerAuditProperty = self.putProperty(configurations, component_audit_file, services)

          for item in ranger_audit_dict:
            if item['filename'] in services["configurations"] and item['configname'] in  services["configurations"][item['filename']]["properties"]:
              if item['filename'] in configurations and item['configname'] in  configurations[item['filename']]["properties"]:
                rangerAuditProperty = configurations[item['filename']]["properties"][item['configname']]
              else:
                rangerAuditProperty = services["configurations"][item['filename']]["properties"][item['configname']]
              putRangerAuditProperty(item['target_configname'], rangerAuditProperty)

    if "HDFS" in servicesList:
      hdfs_user = None
      if "hadoop-env" in services["configurations"] and "hdfs_user" in services["configurations"]["hadoop-env"]["properties"]:
        hdfs_user = services["configurations"]["hadoop-env"]["properties"]["hdfs_user"]
        putRangerAdminProperty('ranger.kms.service.user.hdfs', hdfs_user)

    if "HIVE" in servicesList:
      hive_user = None
      if "hive-env" in services["configurations"] and "hive_user" in services["configurations"]["hive-env"]["properties"]:
        hive_user = services["configurations"]["hive-env"]["properties"]["hive_user"]
        putRangerAdminProperty('ranger.kms.service.user.hive', hive_user)

    ranger_plugins_serviceuser = [
      {'service_name': 'HDFS', 'file_name': 'hadoop-env', 'config_name': 'hdfs_user', 'target_configname': 'ranger.plugins.hdfs.serviceuser'},
      {'service_name': 'HIVE', 'file_name': 'hive-env', 'config_name': 'hive_user', 'target_configname': 'ranger.plugins.hive.serviceuser'},
      {'service_name': 'YARN', 'file_name': 'yarn-env', 'config_name': 'yarn_user', 'target_configname': 'ranger.plugins.yarn.serviceuser'},
      {'service_name': 'HBASE', 'file_name': 'hbase-env', 'config_name': 'hbase_user', 'target_configname': 'ranger.plugins.hbase.serviceuser'},
      {'service_name': 'KNOX', 'file_name': 'knox-env', 'config_name': 'knox_user', 'target_configname': 'ranger.plugins.knox.serviceuser'},
      {'service_name': 'STORM', 'file_name': 'storm-env', 'config_name': 'storm_user', 'target_configname': 'ranger.plugins.storm.serviceuser'},
      {'service_name': 'KAFKA', 'file_name': 'kafka-env', 'config_name': 'kafka_user', 'target_configname': 'ranger.plugins.kafka.serviceuser'},
      {'service_name': 'RANGER_KMS', 'file_name': 'kms-env', 'config_name': 'kms_user', 'target_configname': 'ranger.plugins.kms.serviceuser'},
      {'service_name': 'ATLAS', 'file_name': 'atlas-env', 'config_name': 'metadata_user', 'target_configname': 'ranger.plugins.atlas.serviceuser'}
    ]

    for item in range(len(ranger_plugins_serviceuser)):
      if ranger_plugins_serviceuser[item]['service_name'] in servicesList:
        file_name = ranger_plugins_serviceuser[item]['file_name']
        config_name = ranger_plugins_serviceuser[item]['config_name']
        target_configname = ranger_plugins_serviceuser[item]['target_configname']
        if file_name in services["configurations"] and config_name in services["configurations"][file_name]["properties"]:
          service_user = services["configurations"][file_name]["properties"][config_name]
          putRangerAdminProperty(target_configname, service_user)

    if "ATLAS" in servicesList:
      if "ranger-env" in services["configurations"]:
        putAtlasRangerAuditProperty = self.putProperty(configurations, 'ranger-atlas-audit', services)
        xasecure_audit_destination_hdfs = ''
        xasecure_audit_destination_hdfs_dir = ''
        xasecure_audit_destination_solr = ''
        if 'xasecure.audit.destination.hdfs' in configurations['ranger-env']['properties']:
          xasecure_audit_destination_hdfs = configurations['ranger-env']['properties']['xasecure.audit.destination.hdfs']
        else:
          xasecure_audit_destination_hdfs = services['configurations']['ranger-env']['properties']['xasecure.audit.destination.hdfs']

        if 'core-site' in services['configurations'] and ('fs.defaultFS' in services['configurations']['core-site']['properties']):
          xasecure_audit_destination_hdfs_dir = '{0}/{1}/{2}'.format(services['configurations']['core-site']['properties']['fs.defaultFS'] ,'ranger','audit')

        if 'xasecure.audit.destination.solr' in configurations['ranger-env']['properties']:
          xasecure_audit_destination_solr = configurations['ranger-env']['properties']['xasecure.audit.destination.solr']
        else:
          xasecure_audit_destination_solr = services['configurations']['ranger-env']['properties']['xasecure.audit.destination.solr']

        putAtlasRangerAuditProperty('xasecure.audit.destination.hdfs',xasecure_audit_destination_hdfs)
        putAtlasRangerAuditProperty('xasecure.audit.destination.hdfs.dir',xasecure_audit_destination_hdfs_dir)
        putAtlasRangerAuditProperty('xasecure.audit.destination.solr',xasecure_audit_destination_solr)

  def validateRangerTagsyncConfigurations(self, properties, recommendedDefaults, configurations, services, hosts):
    ranger_tagsync_properties = properties
    validationItems = []
    servicesList = [service["StackServices"]["service_name"] for service in services["services"]]

    has_atlas = False
    if "RANGER" in servicesList:
      has_atlas = not "ATLAS" in servicesList

      if has_atlas and 'ranger.tagsync.source.atlas' in ranger_tagsync_properties and \
        ranger_tagsync_properties['ranger.tagsync.source.atlas'].lower() == 'true':
        validationItems.append({"config-name": "ranger.tagsync.source.atlas",
                                  "item": self.getWarnItem(
                                    "Need to Install ATLAS service to set ranger.tagsync.source.atlas as true.")})

    return self.toConfigurationValidationProblems(validationItems, "ranger-tagsync-site")

  def __isServiceDeployed(self, services, serviceName):
    servicesList = [service["StackServices"]["service_name"] for service in services["services"]]
    return serviceName in servicesList

  """
  Returns the host(s) on which a requested service's component is hosted.
  Parameters :
    services : Configuration information for the cluster
    serviceName : Passed-in service in consideration
    componentName : Passed-in component in consideration
  """
  def __getHostsForComponent(self, services, serviceName, componentName):
    servicesList = [service["StackServices"]["service_name"] for service in services["services"]]
    componentsListList = [service["components"] for service in services["services"]]
    componentsList = [item["StackServiceComponents"] for sublist in componentsListList for item in sublist]
    hosts_for_component = []
    if serviceName in servicesList:
      hosts = [component["hostnames"] for component in componentsList if component["component_name"] == componentName]
      if len(hosts) > 0:
        hosts_for_component = hosts[0]
    return hosts_for_component

  def isComponentUsingCardinalityForLayout(self, componentName):
    return super(HDP25StackAdvisor, self).isComponentUsingCardinalityForLayout (componentName) or  componentName in ['SPARK2_THRIFTSERVER', 'LIVY_SERVER']
