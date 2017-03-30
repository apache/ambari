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



SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
STACKS_DIR = os.path.join(SCRIPT_DIR, '../../../stacks/')
PARENT_FILE = os.path.join(STACKS_DIR, 'service_advisor.py')

try:
  with open(PARENT_FILE, 'rb') as fp:
    service_advisor = imp.load_module('service_advisor', fp, PARENT_FILE, ('.py', 'rb', imp.PY_SOURCE))
except Exception as e:
  traceback.print_exc()
  print "Failed to load parent"

class TezServiceAdvisor(service_advisor.ServiceAdvisor):

  def __init__(self, *args, **kwargs):
    self.as_super = super(TezServiceAdvisor, self)
    self.as_super.__init__(*args, **kwargs)

    self.initialize_logger("TezServiceAdvisor")

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
    self.logger.info("Class: %s, Method: %s. Recommending Service Configurations." %
                (self.__class__.__name__, inspect.stack()[0][3]))

    recommender = TezRecommender()
    recommender.recommendTezConfigurationsFromHDP21(configurations, clusterData, services, hosts)
    recommender.recommendTezConfigurationsFromHDP22(configurations, clusterData, services, hosts)
    recommender.recommendTezConfigurationsFromHDP23(configurations, clusterData, services, hosts)
    recommender.recommendTezConfigurationsFromHDP26(configurations, clusterData, services, hosts)


  def getServiceConfigurationsValidationItems(self, configurations, recommendedDefaults, services, hosts):
    """
    Entry point.
    Validate configurations for the service. Return a list of errors.
    The code for this function should be the same for each Service Advisor.
    """
    self.logger.info("Class: %s, Method: %s. Validating Configurations." %
                (self.__class__.__name__, inspect.stack()[0][3]))

    validator = TezValidator()
    # Calls the methods of the validator using arguments,
    # method(siteProperties, siteRecommendations, configurations, services, hosts)
    return validator.validateListOfConfigUsingMethod(configurations, recommendedDefaults, services, hosts, validator.validators)



class TezRecommender(service_advisor.ServiceAdvisor):
  """
  Tez Recommender suggests properties when adding the service for the first time or modifying configs via the UI.
  """

  def __init__(self, *args, **kwargs):
    self.as_super = super(TezRecommender, self)
    self.as_super.__init__(*args, **kwargs)

  def recommendTezConfigurationsFromHDP21(self, configurations, clusterData, services, hosts):
    putTezProperty = self.putProperty(configurations, "tez-site")
    putTezProperty("tez.am.resource.memory.mb", int(clusterData['amMemory']))
    putTezProperty("tez.am.java.opts",
                   "-server -Xmx" + str(int(0.8 * clusterData["amMemory"]))
                   + "m -Djava.net.preferIPv4Stack=true -XX:+UseNUMA -XX:+UseParallelGC")
    recommended_tez_queue = self.recommendYarnQueue(services, "tez-site", "tez.queue.name")
    if recommended_tez_queue is not None:
      putTezProperty("tez.queue.name", recommended_tez_queue)


  def recommendTezConfigurationsFromHDP22(self, configurations, clusterData, services, hosts):
    if not "yarn-site" in configurations:
      self.calculateYarnAllocationSizes(configurations, services, hosts)
      #properties below should be always present as they are provided in HDP206 stack advisor
    yarnMaxAllocationSize = min(30 * int(configurations["yarn-site"]["properties"]["yarn.scheduler.minimum-allocation-mb"]), int(configurations["yarn-site"]["properties"]["yarn.scheduler.maximum-allocation-mb"]))

    putTezProperty = self.putProperty(configurations, "tez-site", services)
    putTezProperty("tez.am.resource.memory.mb", min(int(configurations["yarn-site"]["properties"]["yarn.scheduler.maximum-allocation-mb"]), int(clusterData['amMemory']) * 2 if int(clusterData['amMemory']) < 3072 else int(clusterData['amMemory'])))

    taskResourceMemory = clusterData['mapMemory'] if clusterData['mapMemory'] > 2048 else int(clusterData['reduceMemory'])
    taskResourceMemory = min(clusterData['containers'] * clusterData['ramPerContainer'], taskResourceMemory, yarnMaxAllocationSize)
    putTezProperty("tez.task.resource.memory.mb", min(int(configurations["yarn-site"]["properties"]["yarn.scheduler.maximum-allocation-mb"]), taskResourceMemory))
    taskResourceMemory = int(configurations["tez-site"]["properties"]["tez.task.resource.memory.mb"])
    putTezProperty("tez.runtime.io.sort.mb", min(int(taskResourceMemory * 0.4), 2047))
    putTezProperty("tez.runtime.unordered.output.buffer.size-mb", int(taskResourceMemory * 0.075))
    putTezProperty("tez.session.am.dag.submit.timeout.secs", "600")

    tez_queue = self.recommendYarnQueue(services, "tez-site", "tez.queue.name")
    if tez_queue is not None:
      putTezProperty("tez.queue.name", tez_queue)

    serverProperties = services["ambari-server-properties"]
    latest_tez_jar_version = None

    server_host = socket.getfqdn()
    for host in hosts["items"]:
      if server_host == host["Hosts"]["host_name"]:
        server_host = host["Hosts"]["public_host_name"]
    server_port = '8080'
    server_protocol = 'http'
    views_dir = '/var/lib/ambari-server/resources/views/'

    has_tez_view = False
    if serverProperties:
      if 'client.api.port' in serverProperties:
        server_port = serverProperties['client.api.port']
      if 'views.dir' in serverProperties:
        views_dir = serverProperties['views.dir']
      if 'api.ssl' in serverProperties:
        if serverProperties['api.ssl'].lower() == 'true':
          server_protocol = 'https'

      views_work_dir = os.path.join(views_dir, 'work')

      if os.path.exists(views_work_dir) and os.path.isdir(views_work_dir):
        for file in os.listdir(views_work_dir):
          if fnmatch.fnmatch(file, 'TEZ{*}'):
            has_tez_view = True # now used just to verify if the tez view exists
          pass
        pass
      pass
    pass

    if has_tez_view:
      tez_url = '{0}://{1}:{2}/#/main/view/TEZ/tez_cluster_instance'.format(server_protocol, server_host, server_port)
      putTezProperty("tez.tez-ui.history-url.base", tez_url)
    pass


  def recommendTezConfigurationsFromHDP23(self, configurations, clusterData, services, hosts):

    putTezProperty = self.putProperty(configurations, "tez-site")

    if "HIVE" in self.getServiceNames(services):
      if not "hive-site" in configurations:
        self.recommendHIVEConfigurations(configurations, clusterData, services, hosts)

      if "hive-site" in configurations and "hive.tez.container.size" in configurations["hive-site"]["properties"]:
        putTezProperty("tez.task.resource.memory.mb", configurations["hive-site"]["properties"]["hive.tez.container.size"])

    # remove 2gb limit for tez.runtime.io.sort.mb
    # in HDP 2.3 "tez.runtime.sorter.class" is set by default to PIPELINED, in other case comment calculation code below
    taskResourceMemory = int(configurations["tez-site"]["properties"]["tez.task.resource.memory.mb"])
    # fit io.sort.mb into tenured regions
    putTezProperty("tez.runtime.io.sort.mb", int(taskResourceMemory * 0.8 * 0.33))

    if "tez-site" in services["configurations"] and "tez.runtime.sorter.class" in services["configurations"]["tez-site"]["properties"]:
      if services["configurations"]["tez-site"]["properties"]["tez.runtime.sorter.class"] == "LEGACY":
        putTezAttribute = self.putPropertyAttribute(configurations, "tez-site")
        putTezAttribute("tez.runtime.io.sort.mb", "maximum", 1800)
    pass

    serverProperties = services["ambari-server-properties"]
    latest_tez_jar_version = None

    server_host = socket.getfqdn()
    for host in hosts["items"]:
      if server_host == host["Hosts"]["host_name"]:
        server_host = host["Hosts"]["public_host_name"]
    server_port = '8080'
    server_protocol = 'http'
    views_dir = '/var/lib/ambari-server/resources/views/'

    has_tez_view = False
    if serverProperties:
      if 'client.api.port' in serverProperties:
        server_port = serverProperties['client.api.port']
      if 'views.dir' in serverProperties:
        views_dir = serverProperties['views.dir']
      if 'api.ssl' in serverProperties:
        if serverProperties['api.ssl'].lower() == 'true':
          server_protocol = 'https'

      views_work_dir = os.path.join(views_dir, 'work')

      if os.path.exists(views_work_dir) and os.path.isdir(views_work_dir):
        for file in os.listdir(views_work_dir):
          if fnmatch.fnmatch(file, 'TEZ{*}'):
            has_tez_view = True # now used just to verify if the tez view exists
          pass
        pass
      pass
    pass

    if has_tez_view:
      tez_url = '{0}://{1}:{2}/#/main/view/TEZ/tez_cluster_instance'.format(server_protocol, server_host, server_port)
      putTezProperty("tez.tez-ui.history-url.base", tez_url)
    pass

    # TEZ JVM options
    jvmGCParams = "-XX:+UseParallelGC"
    if "ambari-server-properties" in services and "java.home" in services["ambari-server-properties"]:
      # JDK8 needs different parameters
      match = re.match(".*\/jdk(1\.\d+)[\-\_\.][^/]*$", services["ambari-server-properties"]["java.home"])
      if match and len(match.groups()) > 0:
        # Is version >= 1.8
        versionSplits = re.split("\.", match.group(1))
        if versionSplits and len(versionSplits) > 1 and int(versionSplits[0]) > 0 and int(versionSplits[1]) > 7:
          jvmGCParams = "-XX:+UseG1GC -XX:+ResizeTLAB"
      # Note: Same calculation is done in 2.6/stack_advisor::recommendTezConfigurations() for 'tez.task.launch.cmd-opts',
    # and along with it, are appended heap dump opts. If something changes here, make sure to change it in 2.6 stack.
    putTezProperty('tez.am.launch.cmd-opts', "-XX:+PrintGCDetails -verbose:gc -XX:+PrintGCTimeStamps -XX:+UseNUMA " + jvmGCParams)
    putTezProperty('tez.task.launch.cmd-opts', "-XX:+PrintGCDetails -verbose:gc -XX:+PrintGCTimeStamps -XX:+UseNUMA " + jvmGCParams)


  def recommendTezConfigurationsFromHDP26(self, configurations, clusterData, services, hosts):
    putTezProperty = self.putProperty(configurations, "tez-site")

    # TEZ JVM options
    jvmGCParams = "-XX:+UseParallelGC"
    if "ambari-server-properties" in services and "java.home" in services["ambari-server-properties"]:
      # JDK8 needs different parameters
      match = re.match(".*\/jdk(1\.\d+)[\-\_\.][^/]*$", services["ambari-server-properties"]["java.home"])
      if match and len(match.groups()) > 0:
        # Is version >= 1.8
        versionSplits = re.split("\.", match.group(1))
        if versionSplits and len(versionSplits) > 1 and int(versionSplits[0]) > 0 and int(versionSplits[1]) > 7:
          jvmGCParams = "-XX:+UseG1GC -XX:+ResizeTLAB"
    tez_jvm_opts = "-XX:+PrintGCDetails -verbose:gc -XX:+PrintGCTimeStamps -XX:+UseNUMA "
    # Append 'jvmGCParams' and 'Heap Dump related option' (({{heap_dump_opts}}) Expanded while writing the
    # configurations at start/restart time).
    tez_jvm_updated_opts = tez_jvm_opts + jvmGCParams + "{{heap_dump_opts}}"
    putTezProperty('tez.am.launch.cmd-opts', tez_jvm_updated_opts)
    putTezProperty('tez.task.launch.cmd-opts', tez_jvm_updated_opts)
    self.logger.info("Updated 'tez-site' config 'tez.task.launch.cmd-opts' and 'tez.am.launch.cmd-opts' as "
                ": {0}".format(tez_jvm_updated_opts))


class TezValidator(service_advisor.ServiceAdvisor):
  """
  Tez Validator checks the correctness of properties whenever the service is first added or the user attempts to
  change configs via the UI.
  """

  def __init__(self, *args, **kwargs):
    self.as_super = super(TezValidator, self)
    self.as_super.__init__(*args, **kwargs)

    self.validators = [("tez-site", self.validateTezConfigurationsFromHDP21),
                       ("tez-site", self.validateTezConfigurationsFromHDP22)]


  def validateTezConfigurationsFromHDP21(self, properties, recommendedDefaults, configurations, services, hosts):
    validationItems = [ {"config-name": 'tez.am.resource.memory.mb', "item": self.validatorLessThenDefaultValue(properties, recommendedDefaults, 'tez.am.resource.memory.mb')},
                        {"config-name": 'tez.am.java.opts', "item": self.validateXmxValue(properties, recommendedDefaults, 'tez.am.java.opts')},
                        {"config-name": 'tez.queue.name', "item": self.validatorYarnQueue(properties, recommendedDefaults, 'tez.queue.name', services)} ]
    return self.toConfigurationValidationProblems(validationItems, "tez-site")


  def validateTezConfigurationsFromHDP22(self, properties, recommendedDefaults, configurations, services, hosts):
    validationItems = [ {"config-name": 'tez.am.resource.memory.mb', "item": self.validatorLessThenDefaultValue(properties, recommendedDefaults, 'tez.am.resource.memory.mb')},
                        {"config-name": 'tez.task.resource.memory.mb', "item": self.validatorLessThenDefaultValue(properties, recommendedDefaults, 'tez.task.resource.memory.mb')},
                        {"config-name": 'tez.runtime.io.sort.mb', "item": self.validatorLessThenDefaultValue(properties, recommendedDefaults, 'tez.runtime.io.sort.mb')},
                        {"config-name": 'tez.runtime.unordered.output.buffer.size-mb', "item": self.validatorLessThenDefaultValue(properties, recommendedDefaults, 'tez.runtime.unordered.output.buffer.size-mb')},
                        {"config-name": 'tez.queue.name', "item": self.validatorYarnQueue(properties, recommendedDefaults, 'tez.queue.name', services)} ]
    if "tez.tez-ui.history-url.base" in recommendedDefaults:
      validationItems.append({"config-name": 'tez.tez-ui.history-url.base', "item": self.validatorEqualsToRecommendedItem(properties, recommendedDefaults, 'tez.tez-ui.history-url.base')})

    tez_site = properties
    prop_name1 = 'tez.am.resource.memory.mb'
    prop_name2 = 'tez.task.resource.memory.mb'
    yarnSiteProperties = self.getSiteProperties(configurations, "yarn-site")
    if yarnSiteProperties:
      yarnMaxAllocationSize = min(30 * int(configurations["yarn-site"]["properties"]["yarn.scheduler.minimum-allocation-mb"]),int(configurations["yarn-site"]["properties"]["yarn.scheduler.maximum-allocation-mb"]))
      if int(tez_site[prop_name1]) > yarnMaxAllocationSize:
        validationItems.append({"config-name": prop_name1,
                                "item": self.getWarnItem(
                                  "{0} should be less than YARN max allocation size ({1})".format(prop_name1, yarnMaxAllocationSize))})
      if int(tez_site[prop_name2]) > yarnMaxAllocationSize:
        validationItems.append({"config-name": prop_name2,
                                "item": self.getWarnItem(
                                  "{0} should be less than YARN max allocation size ({1})".format(prop_name2, yarnMaxAllocationSize))})

    return self.toConfigurationValidationProblems(validationItems, "tez-site")



