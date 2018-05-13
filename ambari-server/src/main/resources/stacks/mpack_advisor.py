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

# Python Imports
import imp
import os
import random
import re
import socket
import string
import traceback
import json
import sys
import logging
from math import ceil, floor
from urlparse import urlparse

# Local imports
from ambari_configuration import AmbariConfiguration
from resource_management.libraries.functions.data_structure_utils import get_from_dict
from resource_management.core.exceptions import Fail

class MpackAdvisor(object):
  """
  Abstract mpack_advisor class implemented by default_mpack_advisor.

  Currently mpack advisor provide following abilities:
  - Recommend where services should be installed in cluster
  - Recommend configurations based on host hardware
  - Validate user selection of where services are installed on cluster
  - Validate user configuration values 

  Each of the above methods is passed in parameters about services and hosts involved as described below.

    @type mapck_instances: dictionary
    @param mpack_instances: Dictionary containing all information about mpack_instances with services selected by the user.
      Example: {
      "mpack_instances": [
        {
          "name": "HDPCORE",
          "type": "HDPCORE",
          "verson", "1.0.0.-b202",
          "service_instance": [
            {
              "name": "HDFS"
            },
            {
              "name": "ZK1",
              "type": "ZOOKEEPER"
            },
            {
              "name": "ZK2",
              "type": "ZOOKEEPER"
            },
            {
              "name": "HADOOP_CLIENTS"
            },
            {
              "name": "ZOOKEEPER_CLIENTS"
            }
            ...
          ]
        },
        ...
      ]
    @type hosts: dictionary
    @param hosts: Dictionary containing host list in this cluster
     Example: {
      "hosts": [
        "mpack-1.c.pramod-thangali.inernal",
        "mpack-2.c.pramod-thangali.inernal",
        "mpack-3.c.pramod-thangali.inernal",
      ]
     }

    Each of the methods can either return recommendations or validations.

    Recommendations are made in a Ambari Blueprints friendly format. 
    Validations are an array of validation objects.
  """

  def recommendComponentLayout(self):
    """
    Returns recommendation of which hosts various service components should be installed on.

    This function takes as input all details about services being installed, and hosts
    they are being installed into, to generate hostname assignments to various components
    of each service.

    @type services: dictionary
    @param services: Dictionary containing all information about services selected by the user.
    @type hosts: dictionary
    @param hosts: Dictionary containing all information about hosts in this cluster
    @rtype: dictionary
    @return: Layout recommendation of service components on cluster hosts in Ambari Blueprints friendly format. 
        Example: {
          "host_groups" : [
            {
              "name": "host-group-1",
              "components" : [
                {
                  "name" : "NAMENODE",
                  "mpack_instance": "HDPCORE",
                  "service_instance": "HDFS"
                },
                {
                  "name" : "ZOOKEEEPER_SERVER",
                  "mpack_instance": "HDPCORE",
                  "service_instance": "ZK1"
                },
                {
                  "name" : "ZOOKEEEPER_SERVER",
                  "mpack_instance": "HDPCORE",
                  "service_instance": "ZK2"
                },
                {
                  "name" : "HBASE_MASTER",
                  "mpack_instance": "ODS-DEFAULT",
                  "service_instance": "HBASE"
                }
              ]
            },
            {
              "name": "host-group-2",
              "components" : [
                {
                  "name" : "DATANODE",
                  "mpack_instance": "HDPCORE",
                  "service_instance": "HDFS"
                },
                {
                  "name" : "SECONDARY_NAMENODE",
                  "mpack_instance": "HDPCORE",
                  "service_instance": "HDFS"
                },
                {
                  "name" : "HBASE_MASTER",
                  "mpack_instance": "ODS-MKTG",
                  "service_instance": "HBASE"
                },
                {
                  "name" : "HBASE_MASTER",
                  "mpack_instance": "ODS-DEFAULT",
                  "service_instance": "HBASE"
                },
                {
                  "name" : "HADOOP_CLIENT",
                  "mpack_instance": "ODS-MKTG",
                  "service_instance": "HADOOP_CLIENTS",
                  "is_internal": "true"
                },
                {
                  "name" : "HBASE_REGIONSERVER",
                  "mpack_instance": "ODS",
                  "service_instance": "HBASE"
                }
              ]
            }
          ]
        },
        "blueprint_cluster_binding": {
          "host_groups": [
            {
              "name": "host-group-1",
              "hosts": [
                {
                  "fqdn": "c6401.ambari.apache.org"
                }
              ]
            },
            {
              "name": "host-group-2",
              "hosts": [
                {
                  "fqdn" : "c6402.ambari.apache.org"
                }
              ]
            }
          ]
        }
    """
    pass

  def validateComponentLayout(self):
    """
    Returns array of Validation issues with service component layout on hosts

    This function takes as input all details about services being installed along with
    hosts the components are being installed on (hostnames property is populated for 
    each component).  

    @type services: dictionary
    @param services: Dictionary containing information about services and host layout selected by the user.
    @type hosts: dictionary
    @param hosts: Dictionary containing all information about hosts in this cluster
    @rtype: dictionary
    @return: Dictionary containing array of validation items
        Example: {
          "items": [
            {
              "type" : "host-group",
              "level" : "ERROR",
              "message" : "NameNode and Secondary NameNode should not be hosted on the same machine",
              "component-name" : "NAMENODE",
              "host" : "c6401.ambari.apache.org" 
            },
            ...
          ]
        }  
    """
    pass

  def recommendConfigurations(self):
    """
    Returns recommendation of service configurations based on host-specific layout of components.

    This function takes as input all details about services being installed, and hosts
    they are being installed into, to recommend host-specific configurations.

    @type services: dictionary
    @param services: Dictionary containing all information about services and component layout selected by the user.
    @type hosts: dictionary
    @param hosts: Dictionary containing all information about hosts in this cluster
    @rtype: dictionary
    @return: Layout recommendation of service components on cluster hosts in Ambari Blueprints friendly format. 
        Example:
        "blueprint": {
          "mpack_instances": [
            {
              "name": "HDPCORE",
              "type": "HDPCORE",
              "version", "1.0.0.b202",
              "servuce_instances": [
                {
                  "name": "HDFS",
                  "type": "HDFS"
                },
                {
                  "name": "ZK1",
                  "type": "ZOOKEEPER"
                },
                {
                  "name": "ZK2",
                  "type": "ZOOKEEPER"
                },
                {
                  "name": "HADOOP_CLIENTS",
                  "type": "HADOOP_CLIENTS"
                },
                {
                  "name": "ZOOKEEPER_CLIENTS",
                  "type": "ZOOKEEPER_CLIENTS"
                }
              ]
            },
            {
              "name": "ODS-DEFAULT",
              "type": "ODS",
              "version": "1.0.0.-b37",
              "service_instances": [
                {
                  "name": "HBASE",
                  "type": "HBASE"
                },
                {
                  "name": "HBASE_CLIENTS",
                  "type": "HBASE_CLIENTS"
                }
              ]
            }
          ]
        }
    """
    pass

  def recommendConfigurationsForSSO(self, services, hosts):
    """
    Returns recommendation of SSO-related service configurations based on host-specific layout of components.

    This function takes as input all details about services being installed, and hosts
    they are being installed into, to recommend host-specific configurations.

    @type services: dictionary
    @param services: Dictionary containing all information about services and component layout selected by the user.
    @type hosts: dictionary
    @param hosts: Dictionary containing all information about hosts in this cluster
    @rtype: dictionary
    @return: Layout recommendation of service components on cluster hosts in Ambari Blueprints friendly format.
        Example: {
         "services": [
          "HIVE",
          "TEZ",
          "YARN"
         ],
         "recommendations": {
          "blueprint": {
           "host_groups": [],
           "configurations": {
            "yarn-site": {
             "properties": {
              "yarn.scheduler.minimum-allocation-mb": "682",
              "yarn.scheduler.maximum-allocation-mb": "2048",
              "yarn.nodemanager.resource.memory-mb": "2048"
             }
            },
            "tez-site": {
             "properties": {
              "tez.am.java.opts": "-server -Xmx546m -Djava.net.preferIPv4Stack=true -XX:+UseNUMA -XX:+UseParallelGC",
              "tez.am.resource.memory.mb": "682"
             }
            },
            "hive-site": {
             "properties": {
              "hive.tez.container.size": "682",
              "hive.tez.java.opts": "-server -Xmx546m -Djava.net.preferIPv4Stack=true -XX:NewRatio=8 -XX:+UseNUMA -XX:+UseParallelGC",
              "hive.auto.convert.join.noconditionaltask.size": "238026752"
             }
            }
           }
          },
          "blueprint_cluster_binding": {
           "host_groups": []
          }
         },
         "hosts": [
          "c6401.ambari.apache.org",
          "c6402.ambari.apache.org",
          "c6403.ambari.apache.org"
         ]
        }
    """
    pass

  def validateConfigurations(self):
    """"
    Returns array of Validation issues with configurations provided by user

    This function takes as input all details about services being installed along with
    configuration values entered by the user. These configurations can be validated against
    service requirements, or host hardware to generate validation issues. 

    @type services: dictionary
    @param services: Dictionary containing information about services and user configurations.
    @type hosts: dictionary
    @param hosts: Dictionary containing all information about hosts in this cluster
    @rtype: dictionary
    @return: Dictionary containing array of validation items
        Example: {
         "items": [
          {
           "config-type": "yarn-site", 
           "message": "Value is less than the recommended default of 682", 
           "type": "configuration", 
           "config-name": "yarn.scheduler.minimum-allocation-mb", 
           "level": "WARN"
          }
         ]
       }
    """
    pass

class ServiceInstance(object):
  """
  All the recommendaton and validation should be based on service instance
  """

  def __init__(self, name, serviceType, version, mpackName, mpackVersion, components, configurations):
    self.name = name
    self.type = serviceType
    self.version = version
    self.mpackName = mpackName
    self.mpackVersion = mpackVersion
    self.components = components if components else []
    self.configurations = configurations if configurations else []
    self.serviceAdvisor = None
    # Identify this serviceInstance object
    self.key = self.mpackName+"+"+self.name

  def __eq__(self, other):
    return self.key == other.key

  def getKey(self):
    return self.key

  def setServiceAdvisor(self, serviceAdvisor):
    self.serviceAdvisor = serviceAdvisor

  def getServiceAdvisor(self):
    return self.serviceAdvisor

  def getName(self):
    return self.name

  def getType(self):
    return self.type

  def getVersion(self):
    return self.version

  def getMpackName(self):
    return self.mpackName

  def getMpackVersion(self):
    return self.mpackVersion

  def getComponents(self):
    return self.components

  def getConfigurations(self):
    return self.configurations

class MpackAdvisorImpl(MpackAdvisor):

  CLUSTER_CREATE_OPERATION = "ClusterCreate"
  ADD_SERVICE_OPERATION = "AddService"
  EDIT_CONFIG_OPERATION = "EditConfig"
  RECOMMEND_ATTRIBUTE_OPERATION = "RecommendAttribute"
  OPERATION = "operation"
  OPERATION_DETAILS = "operation_details"

  ADVISOR_CONTEXT = "advisor_context"
  CALL_TYPE = "call_type"

  """
  mpack advisor implementation.
  """

  def __init__(self):
    super(MpackAdvisorImpl, self).__init__()
    self.services = None
    self.hostsList = None

    self.initialize_logger('MpackAdvisor')

    # Since potentially we have to support multiple mpack instances with multiple service instances,
    # the service instance in different mpack instance may have different service advisor, so we will
    # use following data structure to track the relationship among serviceInstance, mapckInstance and serviceAdvisor:
    # Dictionary that maps serviceInstance or componentName to serviceAdvisor
    # the key should be: mpackName+serviceInstanceName[+componentName], the value is serviceAdvisor
    self.mpackServiceInstancesDict = {}

    self.serviceInstancesSet = set()

    # Contains requested properties during 'recommend-configuration-dependencies' request.
    # It's empty during other requests.
    self.allRequestedProperties = None

    # Data structures that may be extended by Service Advisors with information specific to each Service 
    self.mastersWithMultipleInstances = set()
    self.notValuableComponents = set()
    self.notPreferableOnServerComponents = set()
    self.cardinalitiesDict = {}
    self.componentLayoutSchemes = {}

  def initialize_logger(self, name='MpackAdvisor', logging_level=logging.INFO, format='%(asctime)s %(levelname)s %(name)s %(funcName)s: - %(message)s'):
    # set up logging (two separate loggers for stderr and stdout with different loglevels)
    self.logger = logging.getLogger(name)
    self.logger.setLevel(logging_level)
    formatter = logging.Formatter(format)
    chout = logging.StreamHandler(sys.stdout)
    chout.setLevel(logging_level)
    chout.setFormatter(formatter)
    cherr = logging.StreamHandler(sys.stderr)
    cherr.setLevel(logging.ERROR)
    cherr.setFormatter(formatter)
    self.logger.handlers = []
    self.logger.addHandler(cherr)
    self.logger.addHandler(chout)

  def preprocessHostsAndServices(self, hosts, services):
    """
    This method is to parse hosts and services dictionaries and build active host list and service instances information
    :param hosts: list of hosts including active and maintenance hosts
    :param services: services dictionary includes mpack instances and service instances information
    :return: void
    """
    self.services = services
    self.hostsList = self.filterHostMounts(hosts)
    services = services.get("services")
    if not services:
      self.logger.info("No services section is found in services.json")
      return
    for service in services:
      stackService = service.get("StackServices")
      if not stackService:
        continue
      mpackName = stackService.get("stack_name")
      mpackVersion = stackService.get("stack_version")
      # In the future, there should have multiple service instances
      serviceInstanceName = stackService.get("service_name")
      serviceInstanceType = serviceInstanceName
      serviceInstanceVersion = stackService.get("service_version")
      components = service.get("components")
      configurations = stackService.get("configurations")
      serviceInstance = ServiceInstance(serviceInstanceName, serviceInstanceType, serviceInstanceVersion,
                                        mpackName, mpackVersion, components, configurations)
      self.serviceInstancesSet.add(serviceInstance)
      serviceInstanceList = self.mpackServiceInstancesDict.get(mpackName, [])
      serviceInstanceList.append(serviceInstance)
      self.mpackServiceInstancesDict[mpackName] = serviceInstanceList
      if not self.getServiceAdvisor(serviceInstance):
          serviceAdvisor = self.instantiateServiceAdvisor(stackService)
          if serviceAdvisor:
            serviceInstance.setServiceAdvisor(serviceAdvisor)

  def getServiceAdvisor(self, serviceInstance):
    return serviceInstance.getServiceAdvisor()

  def getServiceComponentLayoutValidations(self, services, hosts):
    """
    Get a list of errors.

    :param services: Dictionary of the form:
    {
      'changed-configurations': [],
      'Versions": {
        'parent_stack_version': '9.0',
        'stack_name': 'HDP',
        'stack_version': '9.0',
        'stack_hierarchy': {
          'stack_name': 'HDP',
          'stack_versions': ['8.0', '7.0', ..., '1.0']
        }
      },
      'ambari-server-properties': {'key': 'value', ...},
      'services': [
        {'StackServices': {
          'advisor_path': '/var/lib/ambari-server/resources/common-services/MYSERVICE/1.2.3/service_advisor.py',
          'service_version': '1.2.3',
          'stack_name': 'HDP',
          'service_name': 'MYSERVICE',
          'stack_version': '9.0',
          'advisor_name': 'MYSERVICEServiceAdvisor'
        },
        'components': [
          {'StackServiceComponents': {
            'stack_version': '9.0',
            'decommission_allowed': True|False,
            'display_name': 'My Service Display Name',
            'stack_name': 'HDP',
            'custom_commands': [],
            'component_category': 'CLIENT|MASTER|SLAVE',
            'advertise_version': True|False,
            'is_client': True|False,
            'is_master': False|False,
            'bulk_commands_display_name': '',
            'bulk_commands_master_component_name': '',
            'service_name': 'MYSERVICE',
            'has_bulk_commands_definition': True|False,
            'reassign_allowed': True|False,
            'recovery_enabled': True|False,
            'cardinality': '0+|1|1+',
            'hostnames': ['c6401.ambari.apache.org'],
            'component_name': 'MY_COMPONENT_NAME'
          },
          'dependencies': []
          },
          ...
          }],
          'configurations': [
            {
              'StackConfigurations':
              {
                'stack_name': 'HDP',
                'service_name': 'MYSERVICE',
                'stack_version': '9.0',
                'property_depends_on': [],
                'type': 'myservice-config.xml',
                'property_name': 'foo'
              },
              'dependencies': []
            },
            {
              'StackConfigurations': {
                'stack_name': 'HDP',
                'service_name': 'ZOOKEEPER',
                'stack_version':
                '2.6',
                'property_depends_on': [],
                'type': 'zoo.cfg.xml',
                'property_name': 'autopurge.snapRetainCount'
              },
              'dependencies': []
            }
            ...
         ]
        }
      ],
      'configurations': {}
    }

    :param hosts: Dictionary where hosts["items"] contains list of hosts on the cluster.
    E.g. of the form,
    {
      'items': [
        {
          'Hosts':
          {
            'host_name': 'c6401.ambari.apache.org',
            'public_host_name': 'c6401.ambari.apache.org',
            'ip': '192.168.64.101',
            'rack_info': '/default-rack',
            'os_type': 'centos6',
            'os_arch': 'x86_64',
            'cpu_count': 1,
            'ph_cpu_count': 1
            'host_state': 'HEALTHY',
            'total_mem': 2926196,
            'host_status': 'HEALTHY',
            'last_registration_time': 1481833146522,
            'os_family': 'redhat6',
            'last_heartbeat_time': 1481835051067,
            'recovery_summary': 'DISABLED',
            'host_health_report': '',
            'desired_configs': None,
            'disk_info': [
              {
                'available': '483608892',
                'used': '3304964',
                'percent': '1%',
                'device': '/dev/mapper/VolGroup-lv_root',
                'mountpoint': '/',
                'type': 'ext4',
                'size': '512971376'
              },
              ...
            ],
            'recovery_report': {
              'component_reports': [],
              'summary': 'DISABLED'
            },
            'last_agent_env': {
              'transparentHugePage': 'always',
              'hostHealth': {
                'agentTimeStampAtReporting': 1481835031135,
                'activeJavaProcs': [],
                'serverTimeStampAtReporting': 1481835031180,
                'liveServices': [{
                  'status': 'Healthy',
                  'name': 'ntpd',
                  'desc': ''
                }]
              },
              'umask': 18,
              'reverseLookup': True,
              'alternatives': [],
              'existingUsers': [],
              'firewallName': 'iptables',
              'stackFoldersAndFiles': [],
              'existingRepos': [],
              'installedPackages': [],
              'firewallRunning': False
            }
          }
        }
      ]
    }

    :return: List of errors
    """
    # To be overriden by subclass or Service Advisor
    raise Fail("Must be overriden by subclass or Service Advisor")

  def getActiveHosts(self, hosts):
    """ Filters the list of specified hosts object and returns
        a list of hosts which are not in maintenance mode. """
    hostsList = []
    if hosts:
      hostsList = [host['host_name'] for host in hosts
                   if not host.get('maintenance_state') or host.get('maintenance_state') == "OFF"]
    return hostsList

  def instantiateServiceAdvisor(self, stackService):
    """
    Load the Service Advisor for the given service name by finding the best class in the given file.
    :param serviceInstance: ServiceInstance object that contains a path to the advisor being requested.
    :return: The class name for the Service Advisor requested, or None if one could not be found.
    """
    os.environ["advisor"] = "mpack"
    serviceName = stackService.get("service_name")
    className = stackService.get("advisor_name")
    path = stackService.get("advisor_path")
    classNamePattern = re.compile("%s.*?ServiceAdvisor" % serviceName, re.IGNORECASE)

    if path and os.path.exists(path) and className:
      try:
        with open(path, 'rb') as fp:
          serviceAdvisor = imp.load_module('service_advisor_impl', fp, path, ('.py', 'rb', imp.PY_SOURCE))

          # Find the class name by reading from all of the available attributes of the python file.
          attributes = dir(serviceAdvisor)
          bestClassName = className
          for potentialClassName in attributes:
            if not potentialClassName.startswith("__"):
              m = classNamePattern.match(potentialClassName)
              if m:
                bestClassName = potentialClassName
                break

          if hasattr(serviceAdvisor, bestClassName):
            self.logger.info("ServiceAdvisor implementation for service {0} was loaded".format(serviceName))
            sa = getattr(serviceAdvisor, bestClassName)
            return sa()
          else:
            self.logger.error("Failed to load or create ServiceAdvisor implementation for service {0}: " \
                  "Expecting class name {1} but it was not found.".format(serviceName, bestClassName))
      except Exception as e:
        self.logger.exception("Failed to load or create ServiceAdvisor implementation for service {0}".format(serviceName))

    return None

  def recommendComponentLayout(self):
    """Returns Services object with hostnames array populated for components"""

    layoutRecommendations = self.createComponentLayoutRecommendations()

    serviceInstanceNames = map(lambda si: si.getName(), self.serviceInstancesSet)
    self.logger.info("Create recommendComponentLayout for {0}".format(serviceInstanceNames))
    hostsList = self.getActiveHosts([host["Hosts"] for host in self.hostsList["items"]])

    recommendations = {
      "hosts": hostsList,
      "services": serviceInstanceNames,
      "recommendations": layoutRecommendations
    }

    return recommendations

  def get_heap_size_properties(self):
    """
    Now for mulitple service instances scenario, so this default setting should apply to individual service instance
    Get dictionary of all of the components and a mapping to the heap-size configs, along with default values
    if the heap-size config could not be found. This is used in calculations for the total memory needed to run
    the cluster.
    :param services: Dictionary that contains all of the services being requested. This is used to find heap-size
    configs that have been delegated to Service Advisors to define.
    :return: Dictionary of mappings from component name to another dictionary of the heap-size configs.
    """

    default = {
      "NAMENODE":
        [{"config-name": "hadoop-env",
          "property": "namenode_heapsize",
          "default": "1024m"}],
      "SECONDARY_NAMENODE":
        [{"config-name": "hadoop-env",
          "property": "namenode_heapsize",
          "default": "1024m"}],
      "DATANODE":
        [{"config-name": "hadoop-env",
          "property": "dtnode_heapsize",
          "default": "1024m"}],
      "REGIONSERVER":
        [{"config-name": "hbase-env",
          "property": "hbase_regionserver_heapsize",
          "default": "1024m"}],
      "HBASE_MASTER":
        [{"config-name": "hbase-env",
          "property": "hbase_master_heapsize",
          "default": "1024m"}],
      "HIVE_CLIENT":
        [{"config-name": "hive-env",
          "property": "hive.client.heapsize",
          "default": "1024m"}],
      "HIVE_METASTORE":
        [{"config-name": "hive-env",
          "property": "hive.metastore.heapsize",
          "default": "1024m"}],
      "HIVE_SERVER":
        [{"config-name": "hive-env",
          "property": "hive.heapsize",
          "default": "1024m"}],
      "HISTORYSERVER":
        [{"config-name": "mapred-env",
          "property": "jobhistory_heapsize",
          "default": "1024m"}],
      "OOZIE_SERVER":
        [{"config-name": "oozie-env",
          "property": "oozie_heapsize",
          "default": "1024m"}],
      "RESOURCEMANAGER":
        [{"config-name": "yarn-env",
          "property": "resourcemanager_heapsize",
          "default": "1024m"}],
      "NODEMANAGER":
        [{"config-name": "yarn-env",
          "property": "nodemanager_heapsize",
          "default": "1024m"}],
      "APP_TIMELINE_SERVER":
        [{"config-name": "yarn-env",
          "property": "apptimelineserver_heapsize",
          "default": "1024m"}],
      "ZOOKEEPER_SERVER":
        [{"config-name": "zookeeper-env",
          "property": "zk_server_heapsize",
          "default": "1024m"}],
      "METRICS_COLLECTOR":
        [{"config-name": "ams-hbase-env",
          "property": "hbase_master_heapsize",
          "default": "1024m"},
         {"config-name": "ams-hbase-env",
          "property": "hbase_regionserver_heapsize",
          "default": "1024m"},
         {"config-name": "ams-env",
          "property": "metrics_collector_heapsize",
          "default": "512m"}],
      "ATLAS_SERVER":
        [{"config-name": "atlas-env",
          "property": "atlas_server_xmx",
          "default": "2048m"}],
      "LOGSEARCH_SERVER":
        [{"config-name": "logsearch-env",
          "property": "logsearch_app_max_memory",
          "default": "1024m"}],
      "LOGSEARCH_LOGFEEDER":
        [{"config-name": "logfeeder-env",
          "property": "logfeeder_max_mem",
          "default": "512m"}],
      "SPARK_JOBHISTORYSERVER":
        [{"config-name": "spark-env",
          "property": "spark_daemon_memory",
          "default": "1024m"}],
      "SPARK2_JOBHISTORYSERVER":
        [{"config-name": "spark2-env",
          "property": "spark_daemon_memory",
          "default": "1024m"}]
    }

    defaultHeapSizeDict = {}
    try:
      for mpackName, serviceInstancesList in self.mpackServiceInstancesDict.items():
        # Override any by reading from the Service Advisors
        newDefault = default.deepcopy()
        for serviceInstance in serviceInstancesList:
          serviceAdvisor = serviceInstance.getServiceAdvisor()
          # This seems confusing, but "self" may actually refer to the actual Service Advisor class that was loaded
          # as opposed to this class.
          advisor = serviceAdvisor if serviceAdvisor else self
          # TODO, switch this to a function instead of a property.
          if hasattr(advisor, "heap_size_properties"):
            # Override the values in "default" with those from the service advisor
            newDefault.update(advisor.heap_size_properties)
        defaultHeapSizeDict[mpackName] = newDefault
    except Exception, e:
      self.logger.exception()
    return default

  def createComponentLayoutRecommendations(self):
    """
    This method is to create service instance based component layout,
    At this time, we actually only support one serviceInstance/service
    :return: componentLayoutRecommendation dict
    """

    recommendations = {
      "blueprint": {
        "host_groups": [ ]
      },
      "blueprint_cluster_binding": {
        "host_groups": [ ]
      }
    }

    # for fast lookup
    hostsList = self.getActiveHosts([host["Hosts"] for host in self.hostsList["items"]])
    hostsSet = set(hostsList)

    hostsComponentsMap = {}
    for hostName in hostsList:
      if hostName not in hostsComponentsMap:
        hostsComponentsMap[hostName] = []

    #Sort the serviceInstances so that the dependent serviceInstances will be processed before those that depend on them.
    sortedServiceInstances = self.getServiceInstancesSortedByDependencies()
    #extend hostsComponentsMap' with MASTER components
    for serviceInstance in sortedServiceInstances:
      masterComponents = [component for component in serviceInstance.getComponents() if self.isMasterComponent(component)]
      serviceInstanceName = serviceInstance.getName()
      serviceAdvisor = serviceInstance.getServiceAdvisor()
      for component in masterComponents:
        componentName = component["StackServiceComponents"]["component_name"]
        advisor = serviceAdvisor if serviceAdvisor else self
        #Filter the hosts such that only hosts that meet the dependencies are included (if possible)
        filteredHosts = self.getFilteredHostsBasedOnDependencies(component, hostsList, hostsComponentsMap)
        hostsForComponent = advisor.getHostsForMasterComponent(component, filteredHosts)

        #extend 'hostsComponentsMap' with 'hostsForComponent'
        for hostName in hostsForComponent:
          if hostName in hostsSet:
            hostsComponentsMap[hostName].append({"name": componentName, "service_instance": serviceInstance.getName(),
                                                 "mpack_instance": serviceInstance.getMpackName()})

    #extend 'hostsComponentsMap' with Slave and Client Components
    componentsListList = [serviceInstance.getComponents() for serviceInstance in self.serviceInstancesSet]
    componentsList = [item for sublist in componentsListList for item in sublist]
    usedHostsListList = [component["StackServiceComponents"]["hostnames"] for component in componentsList if not self.isComponentNotValuable(component)]
    utilizedHosts = [item for sublist in usedHostsListList for item in sublist]
    freeHosts = [hostName for hostName in hostsList if hostName not in utilizedHosts]

    for serviceInstance in sortedServiceInstances:
      slaveClientComponents = [component for component in serviceInstance.getComponents()
                               if self.isSlaveComponent(component) or self.isClientComponent(component)]
      serviceAdvisor = serviceInstance.getServiceAdvisor()
      for component in slaveClientComponents:
        componentName = component["StackServiceComponents"]["component_name"]
        advisor = serviceAdvisor if serviceAdvisor else self
        #Filter the hosts and free hosts such that only hosts that meet the dependencies are included (if possible)
        filteredHosts = self.getFilteredHostsBasedOnDependencies(component, hostsList, hostsComponentsMap)
        filteredFreeHosts = self.filterList(freeHosts, filteredHosts)
        hostsForComponent = advisor.getHostsForSlaveComponent(component, filteredHosts, filteredFreeHosts)

        #extend 'hostsComponentsMap' with 'hostsForComponent'
        for hostName in hostsForComponent:
          if hostName not in hostsComponentsMap and hostName in hostsSet:
            hostsComponentsMap[hostName] = []
          if hostName in hostsSet:
            hostsComponentsMap[hostName].append({"name": componentName, "service_instance": serviceInstance.getName(),
                                                 "mpack_instance": serviceInstance.getMpackName()})

    #colocate custom services
    for serviceInstance in sortedServiceInstances:
      serviceAdvisor = serviceInstance.getServiceAdvisor()
      if serviceAdvisor:
        serviceInstanceComponents = [component for component in serviceInstance.getComponents()]
        serviceAdvisor.colocateService(hostsComponentsMap, serviceInstanceComponents)

    #prepare 'host-group's from 'hostsComponentsMap'
    host_groups = recommendations["blueprint"]["host_groups"]
    bindings = recommendations["blueprint_cluster_binding"]["host_groups"]
    index = 0
    for key in hostsComponentsMap.keys():
      index += 1
      host_group_name = "host-group-{0}".format(index)
      host_groups.append({"name": host_group_name, "components": hostsComponentsMap[key]})
      bindings.append({"name": host_group_name, "hosts": [{"fqdn": key}]})

    return recommendations

  def getHostsForMasterComponent(self, component, hosts):
    if self.isComponentHostsPopulated(component):
      return component["StackServiceComponents"]["hostnames"]

    if len(hosts) > 1 and self.isMasterComponentWithMultipleInstances(component):
      hostsCount = self.getMinComponentCount(component, self.hostsList)
      if hostsCount > 1: # get first 'hostsCount' available hosts
        hostsForComponent = []
        hostIndex = 0
        while hostsCount > len(hostsForComponent) and hostIndex < len(hosts):
          currentHost = hosts[hostIndex]
          if self.isHostSuitableForComponent(currentHost, component):
            hostsForComponent.append(currentHost)
          hostIndex += 1
        return hostsForComponent

    return [self.getHostForComponent(component, hosts)]

  def getHostsForSlaveComponent(self, component, hostsList, freeHosts):
    if component["StackServiceComponents"]["cardinality"] == "ALL":
      return hostsList

    if self.isComponentHostsPopulated(component):
      return component["StackServiceComponents"]["hostnames"]

    hostsForComponent = []
    componentName = component["StackServiceComponents"]["component_name"]
    if self.isSlaveComponent(component):
      cardinality = str(component["StackServiceComponents"]["cardinality"])
      hostsMin, hostsMax = self.parseCardinality(cardinality, len(hostsList))
      hostsMin, hostsMax = (0 if hostsMin is None else hostsMin, len(hostsList) if hostsMax is None else hostsMax)
      if self.isComponentUsingCardinalityForLayout(componentName) and cardinality:
        if hostsMin > len(hostsForComponent):
          hostsForComponent.extend(freeHosts[0:hostsMin-len(hostsForComponent)])

      else:
        hostsForComponent.extend(freeHosts)
        if not hostsForComponent:  # hostsForComponent is empty
          hostsForComponent = hostsList[-1:]
      hostsForComponent = list(set(hostsForComponent))  # removing duplicates
      if len(hostsForComponent) < hostsMin:
        hostsForComponent = list(set(hostsList))[0:hostsMin]
      elif len(hostsForComponent) > hostsMax:
        hostsForComponent = list(set(hostsList))[0:hostsMax]
    elif self.isClientComponent(component):
      hostsForComponent = freeHosts[0:1]
      if not hostsForComponent:  # hostsForComponent is empty
        hostsForComponent = hostsList[-1:]

    return hostsForComponent

  def getServiceInstancesSortedByDependencies(self):
    """
    Sorts the serviceInstances based on their dependencies.  This is limited to non-conditional host scope dependencies.
    ServiceInstances with no dependencies will go first.  ServiceInstances with dependencies will go after the services
    they are dependent on. If there are circular dependencies, the serviceInstances will go in the order in which they
    were processed.
    """
    processedServiceInstances = []
    sortedServiceInstances = []

    for serviceInstance in self.serviceInstancesSet:
      self.sortServiceInstancesByDependencies(serviceInstance, processedServiceInstances, sortedServiceInstances)

    return sortedServiceInstances

  def sortServiceInstancesByDependencies(self, serviceInstance, processedServiceInstances, sortedServiceInstances):
    """
    Sorts the serviceInstances based on their dependencies.  This is limited to non-conditional host scope dependencies.
    ServiceInstances with no dependencies will go first.  ServiceInstances with dependencies will go after the services
    they are dependent on. If there are circular dependencies, the serviceInstances will go in the order in which they
    were processed.
    """
    if serviceInstance is None or serviceInstance in processedServiceInstances:
      return

    processedServiceInstances.append(serviceInstance)

    for component in serviceInstance.getComponents():
      dependencies = component.get("dependencies", [])
      for dependency in dependencies:
        # accounts only for dependencies that are not conditional
        conditionsPresent =  "conditions" in dependency["Dependencies"] and dependency["Dependencies"]["conditions"]
        scope = "cluster" if "scope" not in dependency["Dependencies"] else dependency["Dependencies"]["scope"]
        if not conditionsPresent and scope == "host":
          componentName = component["StackServiceComponents"]["component_name"]
          requiredComponentName = dependency["Dependencies"]["component_name"]
          requiredServiceInstance = self.getRequiredServiceInstanceForComponentName(serviceInstance, requiredComponentName)
          self.sortServiceInstancesByDependencies(requiredServiceInstance, processedServiceInstances, sortedServiceInstances)

    sortedServiceInstances.append(serviceInstance)

  def getFilteredHostsBasedOnDependencies(self, component, hostsList, hostsComponentsMap):
    """
    Returns a list of hosts that only includes the ones which have all host scope dependencies already assigned to them.
    If an empty list would be returned, instead the full list of hosts are returned.
    In that case, we can't possibly return a valid recommended layout so we will at least return a fully filled layout.
    """
    removeHosts = []
    dependencies = component.get("dependencies", [])
    for dependency in dependencies:
      # accounts only for dependencies that are not conditional
      conditionsPresent = "conditions" in dependency["Dependencies"] and dependency["Dependencies"]["conditions"]
      if not conditionsPresent:
        componentName = component["StackServiceComponents"]["component_name"]
        requiredComponentName = dependency["Dependencies"]["component_name"]
        mpackName = component["StackServiceComponents"]["stack_name"]
        requiredComponent = self.getRequiredComponent(mpackName, requiredComponentName)

        # We only deal with "host" scope.
        if requiredComponent and requiredComponent.get("component_category") != "CLIENT":
          scope = "cluster" if "scope" not in dependency["Dependencies"] else dependency["Dependencies"]["scope"]
          if scope == "host":
            for host, hostComponents in hostsComponentsMap.iteritems():
              isRequiredIncluded = False
              for hostComponent in hostComponents:
                currentComponentName = hostComponent.get("name")
                if requiredComponentName == currentComponentName:
                  isRequiredIncluded = True
              if not isRequiredIncluded:
                removeHosts.append(host)

    filteredHostsList = []
    for host in hostsList:
      if host not in removeHosts:
        filteredHostsList.append(host)
    return filteredHostsList

  def filterList(self, list, filter):
    """
    Returns the union of the two lists passed in (list and filter params).
    """
    filteredList = []
    for item in list:
      if item in filter:
        filteredList.append(item)
    return filteredList

  def getRequiredServiceInstanceForComponentName(self, serviceInstance, componentName):
    """
    Return serviceInstance for requested component
    There are two cases: 1. the serviceInstance may be in the same mpackInstance as input serviceInstance
    2. the serviceInstance may be in another mpackInstance

    :type servieInstance ServiceInstance: the original serviceInstance
    :type componentName str: component name
    """
    rst = None
    for si in self.serviceInstancesSet:
      for component in si.getComponents():
        if self.getComponentName(component) == componentName:
          if si == serviceInstance:
            return si
          rst = si

    return rst

  def isComponentUsingCardinalityForLayout(self, componentName):
    return componentName in ['NFS_GATEWAY', 'PHOENIX_QUERY_SERVER', 'SPARK_THRIFTSERVER'] or \
           componentName in ['SPARK2_THRIFTSERVER', 'LIVY2_SERVER', 'LIVY_SERVER']

  def createValidationResponse(self, validationItems):
    """Returns array of Validation objects about issues with hostnames components assigned to"""
    validations = {
      "items": validationItems
    }

    return validations

  def validateComponentLayout(self):
    """Returns array of Validation objects about issues with hostnames components assigned to"""

    validationItems = self.getComponentLayoutValidations()
    return self.createValidationResponse(validationItems)

  def validateConfigurations(self):
    """Returns array of Validation objects about issues with hostnames components assigned to"""
    validationItems = self.getConfigurationsValidationItems()
    return self.createValidationResponse(validationItems)

  def getComponentLayoutValidations(self):
    items = []
    if not self.serviceInstancesSet:
      return items

    items.extend(self.validateRequiredComponentsPresent())

    for serviceInstance in self.serviceInstancesSet:
      serviceAdvisor = serviceInstance.getServiceAdvisor()
      if serviceAdvisor is not None:
        items.extend(serviceAdvisor.getServiceComponentLayoutValidations(self.services, self.hostsList))

    return items

  def validateRequiredComponentsPresent(self):
    """
    Returns validation items derived from component dependencies as specified in service metainfo.xml for all services
    :type services dict
    :rtype list
    """
    items = []
    for serviceInstance in self.serviceInstancesSet:
      for component in serviceInstance.getComponents():
        # Client components are not validated for the dependencies
        # Rather dependent client components are auto-deployed in both UI deployments and blueprint deployments
        if (self.isSlaveComponent(component) or self.isMasterComponent(component)) and \
          component["StackServiceComponents"]["hostnames"]:
          for dependency in component['dependencies']:
            # account for only dependencies that are not conditional
            conditionsPresent =  "conditions" in dependency["Dependencies"] and dependency["Dependencies"]["conditions"]
            if not conditionsPresent:
              requiredComponent = self.getRequiredComponent(serviceInstance.getMapckName(), dependency["Dependencies"]["component_name"])
              componentDisplayName = component["StackServiceComponents"]["display_name"]
              requiredComponentDisplayName = requiredComponent["display_name"] \
                                             if requiredComponent is not None else dependency["Dependencies"]["component_name"]
              requiredComponentHosts = requiredComponent["hostnames"] if requiredComponent is not None else []

              # Client dependencies are not included in validation
              # Client dependencies are auto-deployed in both UI deployements and blueprint deployments
              if (requiredComponent is None) or \
                (requiredComponent["component_category"] != "CLIENT"):
                scope = "cluster" if "scope" not in dependency["Dependencies"] else dependency["Dependencies"]["scope"]
                if scope == "host":
                  componentHosts = component["StackServiceComponents"]["hostnames"]
                  requiredComponentHostsAbsent = []
                  for componentHost in componentHosts:
                    if componentHost not in requiredComponentHosts:
                      requiredComponentHostsAbsent.append(componentHost)
                  if requiredComponentHostsAbsent:
                    message = "{0} requires {1} to be co-hosted on following host(s): {2}.".format(componentDisplayName,
                               requiredComponentDisplayName, ', '.join(requiredComponentHostsAbsent))
                    items.append({ "type": 'host-component', "level": 'ERROR', "message": message,
                                   "component-name": component["StackServiceComponents"]["component_name"],
                                   "mpack-name": serviceInstance.getMapckName(), "mpack-version": serviceInstance.getMapckVersion()})
                elif scope == "cluster" and not requiredComponentHosts:
                  message = "{0} requires {1} to be present in the cluster.".format(componentDisplayName, requiredComponentDisplayName)
                  items.append({ "type": 'host-component', "level": 'ERROR', "message": message, "component-name": component["StackServiceComponents"]["component_name"],
                                 "mpack-name": serviceInstance.getMapckName(), "mpack-version": serviceInstance.getMapckVersion()})
    return items

  def calculateYarnAllocationSizes(self, configurations, services, hosts):
    # initialize data
    components = [component["StackServiceComponents"]["component_name"]
                  for serviceInstance in self.serviceInstancesSet
                  for component in serviceInstance.getComponents()]
    putYarnProperty = self.putProperty(configurations, "yarn-site", services)
    putYarnPropertyAttribute = self.putPropertyAttribute(configurations, "yarn-site")

    # calculate memory properties and get cluster data dictionary with whole information
    clusterSummary = self.getConfigurationClusterSummary(hosts, services)

    # executing code from stack advisor HDP 206
    nodemanagerMinRam = 1048576 # 1TB in mb
    if "referenceNodeManagerHost" in clusterSummary:
      nodemanagerMinRam = min(clusterSummary["referenceNodeManagerHost"]["total_mem"]/1024, nodemanagerMinRam)

    callContext = self.getCallContext(services)
    putYarnProperty('yarn.nodemanager.resource.memory-mb', int(round(min(clusterSummary['containers'] * clusterSummary['ramPerContainer'], nodemanagerMinRam))))
    if 'recommendConfigurations' == callContext:
      putYarnProperty('yarn.nodemanager.resource.memory-mb', int(round(min(clusterSummary['containers'] * clusterSummary['ramPerContainer'], nodemanagerMinRam))))
    else:
      # read from the supplied config
      if "yarn-site" in services["configurations"] and "yarn.nodemanager.resource.memory-mb" in services["configurations"]["yarn-site"]["properties"]:
        putYarnProperty('yarn.nodemanager.resource.memory-mb', int(services["configurations"]["yarn-site"]["properties"]["yarn.nodemanager.resource.memory-mb"]))
      else:
        putYarnProperty('yarn.nodemanager.resource.memory-mb', int(round(min(clusterSummary['containers'] * clusterSummary['ramPerContainer'], nodemanagerMinRam))))
      pass
    pass

    putYarnProperty('yarn.scheduler.minimum-allocation-mb', int(clusterSummary['yarnMinContainerSize']))
    putYarnProperty('yarn.scheduler.maximum-allocation-mb', int(configurations["yarn-site"]["properties"]["yarn.nodemanager.resource.memory-mb"]))

    # executing code from stack advisor HDP 22
    nodeManagerHost = self.getHostWithComponent("YARN", "NODEMANAGER", services, hosts)
    if (nodeManagerHost is not None):
      if "yarn-site" in services["configurations"] and "yarn.nodemanager.resource.percentage-physical-cpu-limit" in services["configurations"]["yarn-site"]["properties"]:
        putYarnPropertyAttribute('yarn.scheduler.minimum-allocation-mb', 'maximum', configurations["yarn-site"]["properties"]["yarn.nodemanager.resource.memory-mb"])
        putYarnPropertyAttribute('yarn.scheduler.maximum-allocation-mb', 'maximum', configurations["yarn-site"]["properties"]["yarn.nodemanager.resource.memory-mb"])

  def getConfigurationClusterSummary(self, hosts, services):
    """
    Copied from HDP 2.0.6 so that it could be used by Service Advisors.
    :servicesList: a list of serviceInstances which may be a subset of whole serviceInstances installed in this cluster
    :hosts: a list of hosts within the cluster, it may be a subset of all hosts in this cluster
    :components:
    :services
    :return: Dictionary of memory and CPU attributes in the cluster
    """
    servicesList = [service["StackServices"]["service_name"] for service in services["services"]]
    hBaseInstalled = False
    if 'HBASE' in servicesList:
      hBaseInstalled = True

    components = [component["StackServiceComponents"]["component_name"]
                  for service in services["services"]
                  for component in service["components"]]

    cluster = {
      "cpu": 0,
      "disk": 0,
      "ram": 0,
      "hBaseInstalled": hBaseInstalled,
      "components": components
    }

    serviceInstances = [serviceInstance for serviceInstance in self.serviceInstancesSet if serviceInstance.getType() in servicesList]

    if len(hosts.get("items")) > 0:
      # nodeManagerHosts=[(host, componentCount),...]
      nodeManagerHosts = self.getHostsWithComponent("YARN", "NODEMANAGER", services, hosts)
      # NodeManager host with least memory is generally used in calculations as it will work in larger hosts.
      if nodeManagerHosts:
        nodeManagerHost = nodeManagerHosts[0];
        for nmHost in nodeManagerHosts:
          if nmHost[0]["Hosts"]["total_mem"]/nmHost[1] < nodeManagerHost[0]["Hosts"]["total_mem"]/nodeManagerHost[1]:
            nodeManagerHost = nmHost
        host = nodeManagerHost[0]["Hosts"]
        cluster["referenceNodeManagerHost"] = host
      else:
        host = hosts["items"][0]["Hosts"]
      cluster["referenceHost"] = host
      cluster["cpu"] = host["cpu_count"]
      cluster["disk"] = len(host["disk_info"])
      cluster["ram"] = int(host["total_mem"] / (1024 * 1024))

    ramRecommendations = [
      {"os":1, "hbase":1},
      {"os":2, "hbase":1},
      {"os":2, "hbase":2},
      {"os":4, "hbase":4},
      {"os":6, "hbase":8},
      {"os":8, "hbase":8},
      {"os":8, "hbase":8},
      {"os":12, "hbase":16},
      {"os":24, "hbase":24},
      {"os":32, "hbase":32},
      {"os":64, "hbase":32}
    ]
    index = {
      cluster["ram"] <= 4: 0,
      4 < cluster["ram"] <= 8: 1,
      8 < cluster["ram"] <= 16: 2,
      16 < cluster["ram"] <= 24: 3,
      24 < cluster["ram"] <= 48: 4,
      48 < cluster["ram"] <= 64: 5,
      64 < cluster["ram"] <= 72: 6,
      72 < cluster["ram"] <= 96: 7,
      96 < cluster["ram"] <= 128: 8,
      128 < cluster["ram"] <= 256: 9,
      256 < cluster["ram"]: 10
    }[1]


    cluster["reservedRam"] = ramRecommendations[index]["os"]
    cluster["hbaseRam"] = ramRecommendations[index]["hbase"]


    cluster["minContainerSize"] = {
      cluster["ram"] <= 3: 128,
      3 < cluster["ram"] <= 4: 256,
      4 < cluster["ram"] <= 8: 512,
      8 < cluster["ram"] <= 24: 1024,
      24 < cluster["ram"]: 2048
    }[1]

    totalAvailableRam = cluster["ram"] - cluster["reservedRam"]
    if cluster["hBaseInstalled"]:
      totalAvailableRam -= cluster["hbaseRam"]
    cluster["totalAvailableRam"] = max(512, totalAvailableRam * 1024)
    self.logger.info("Memory for YARN apps - cluster[totalAvailableRam]: " + str(cluster["totalAvailableRam"]))

    suggestedMinContainerRam = 1024   # new smaller value for YARN min container
    callContext = self.getCallContext(services)

    operation = self.getUserOperationContext(services, MpackAdvisorImpl.OPERATION)
    adding_yarn = self.isServiceBeingAdded(services, 'YARN')
    if operation:
      self.logger.info("user operation context : " + str(operation))

    if services:  # its never None but some unit tests pass it as None
      # If min container value is changed (user is changing it)
      # if its a validation call - just use what ever value is set
      # If its a recommend attribute call (when UI lands on a page)
      # If add service but YARN is not being added
      if self.getOldValue(services, "yarn-site", "yarn.scheduler.minimum-allocation-mb") or \
              'recommendConfigurations' != callContext or \
              operation == MpackAdvisorImpl.RECOMMEND_ATTRIBUTE_OPERATION or \
          (operation == MpackAdvisorImpl.ADD_SERVICE_OPERATION and not adding_yarn):

        self.logger.info("Full context: callContext = " + str(callContext) +
                    " and operation = " + str(operation) + " and adding YARN = " + str(adding_yarn) +
                    " and old value exists = " +
                    str(self.getOldValue(services, "yarn-site", "yarn.scheduler.minimum-allocation-mb")))

        '''yarn.scheduler.minimum-allocation-mb has changed - then pick this value up'''
        if "yarn-site" in services["configurations"] and \
                "yarn.scheduler.minimum-allocation-mb" in services["configurations"]["yarn-site"]["properties"] and \
            str(services["configurations"]["yarn-site"]["properties"]["yarn.scheduler.minimum-allocation-mb"]).isdigit():
          self.logger.info("Using user provided yarn.scheduler.minimum-allocation-mb = " +
                      str(services["configurations"]["yarn-site"]["properties"]["yarn.scheduler.minimum-allocation-mb"]))
          cluster["yarnMinContainerSize"] = int(services["configurations"]["yarn-site"]["properties"]["yarn.scheduler.minimum-allocation-mb"])
          self.logger.info("Minimum ram per container due to user input - cluster[yarnMinContainerSize]: " + str(cluster["yarnMinContainerSize"]))
          if cluster["yarnMinContainerSize"] > cluster["totalAvailableRam"]:
            cluster["yarnMinContainerSize"] = cluster["totalAvailableRam"]
            self.logger.info("Minimum ram per container after checking against limit - cluster[yarnMinContainerSize]: " + str(cluster["yarnMinContainerSize"]))
            pass
          cluster["minContainerSize"] = cluster["yarnMinContainerSize"]    # set to what user has suggested as YARN min container size
          suggestedMinContainerRam = cluster["yarnMinContainerSize"]
          pass
        pass
      pass


    '''containers = max(3, min (2*cores,min (1.8*DISKS,(Total available RAM) / MIN_CONTAINER_SIZE))))'''
    cluster["containers"] = int(round(max(3,
                                          min(2 * cluster["cpu"],
                                              min(ceil(1.8 * cluster["disk"]),
                                                  cluster["totalAvailableRam"] / cluster["minContainerSize"])))))
    self.logger.info("Containers per node - cluster[containers]: " + str(cluster["containers"]))

    if cluster["containers"] * cluster["minContainerSize"] > cluster["totalAvailableRam"]:
      cluster["containers"] = ceil(cluster["totalAvailableRam"] / cluster["minContainerSize"])
      self.logger.info("Modified number of containers based on provided value for yarn.scheduler.minimum-allocation-mb")
      pass

    cluster["ramPerContainer"] = int(abs(cluster["totalAvailableRam"] / cluster["containers"]))
    cluster["yarnMinContainerSize"] = min(suggestedMinContainerRam, cluster["ramPerContainer"])
    self.logger.info("Ram per containers before normalization - cluster[ramPerContainer]: " + str(cluster["ramPerContainer"]))

    '''If greater than cluster["yarnMinContainerSize"], value will be in multiples of cluster["yarnMinContainerSize"]'''
    if cluster["ramPerContainer"] > cluster["yarnMinContainerSize"]:
      cluster["ramPerContainer"] = int(cluster["ramPerContainer"] / cluster["yarnMinContainerSize"]) * cluster["yarnMinContainerSize"]


    cluster["mapMemory"] = int(cluster["ramPerContainer"])
    cluster["reduceMemory"] = cluster["ramPerContainer"]
    cluster["amMemory"] = max(cluster["mapMemory"], cluster["reduceMemory"])

    self.logger.info("Min container size - cluster[yarnMinContainerSize]: " + str(cluster["yarnMinContainerSize"]))
    self.logger.info("Available memory for map - cluster[mapMemory]: " + str(cluster["mapMemory"]))
    self.logger.info("Available memory for reduce - cluster[reduceMemory]: " + str(cluster["reduceMemory"]))
    self.logger.info("Available memory for am - cluster[amMemory]: " + str(cluster["amMemory"]))


    return cluster

  def getCallContext(self, services):
    if services:
      if MpackAdvisorImpl.ADVISOR_CONTEXT in services:
        self.logger.info("call type context : " + str(services[MpackAdvisorImpl.ADVISOR_CONTEXT]))
        return services[MpackAdvisorImpl.ADVISOR_CONTEXT][MpackAdvisorImpl.CALL_TYPE]
    return ""

  # if serviceName is being added
  def isServiceBeingAdded(self, services, serviceName):
    if services:
      if 'user-context' in services.keys():
        userContext = services["user-context"]
        if MpackAdvisorImpl.OPERATION in userContext and \
              'AddService' == userContext[MpackAdvisorImpl.OPERATION] and \
          MpackAdvisorImpl.OPERATION_DETAILS in userContext:
          if -1 != userContext["operation_details"].find(serviceName):
            return True
    return False

  def getUserOperationContext(self, services, contextName):
    if services:
      if 'user-context' in services.keys():
        userContext = services["user-context"]
        if contextName in userContext:
          return userContext[contextName]
    return None

  def get_system_min_uid(self):
    login_defs = '/etc/login.defs'
    uid_min_tag = 'UID_MIN'
    comment_tag = '#'
    uid_min = uid_default = '1000'
    uid = None

    if os.path.exists(login_defs):
      with open(login_defs, 'r') as f:
        data = f.read().split('\n')
        # look for uid_min_tag in file
        uid = filter(lambda x: uid_min_tag in x, data)
        # filter all lines, where uid_min_tag was found in comments
        uid = filter(lambda x: x.find(comment_tag) > x.find(uid_min_tag) or x.find(comment_tag) == -1, uid)

      if uid is not None and len(uid) > 0:
        uid = uid[0]
        comment = uid.find(comment_tag)
        tag = uid.find(uid_min_tag)
        if comment == -1:
          uid_tag = tag + len(uid_min_tag)
          uid_min = uid[uid_tag:].strip()
        elif comment > tag:
          uid_tag = tag + len(uid_min_tag)
          uid_min = uid[uid_tag:comment].strip()

    # check result for value
    try:
      int(uid_min)
    except ValueError:
      return uid_default

    return uid_min

  def validateClusterConfigurations(self):
    validationItems = []

    return self.toConfigurationValidationProblems(validationItems, "")

  def toConfigurationValidationProblems(self, validationProblems, siteName):
    """
    Encapsulate the validation item's fields of "level" and "message" for the given validation's config-name.
    :param validationProblems: List of validation problems
    :param siteName: Config type
    :return: List of configuration validation problems that include additional fields like the log level.
    """
    result = []
    for validationProblem in validationProblems:
      validationItem = validationProblem.get("item")
      if validationItem:
        problem = {"type": 'configuration', "level": validationItem["level"], "message": validationItem["message"],
                   "config-type": siteName, "config-name": validationProblem["config-name"] }
        result.append(problem)
    return result

  def validateServiceConfigurations(self, serviceName):

    return self.getServiceConfigurationValidators().get(serviceName)

  def getServiceConfigurationValidators(self):
    """
    TODO
    The validator call should forward to individual serviceAdvisor, I do not understand why it is implemented in services/StackAdvisor
    :return:
    """
    return {}

  def validateMinMax(self, items, recommendedDefaults, configurations):

    # required for casting to the proper numeric type before comparison
    def convertToNumber(number):
      try:
        return int(number)
      except ValueError:
        return float(number)

    for configName in configurations:
      validationItems = []
      if configName in recommendedDefaults and "property_attributes" in recommendedDefaults[configName]:
        for propertyName in recommendedDefaults[configName]["property_attributes"]:
          if propertyName in configurations[configName]["properties"]:
            if "maximum" in recommendedDefaults[configName]["property_attributes"][propertyName] and \
                propertyName in recommendedDefaults[configName]["properties"]:
              userValue = convertToNumber(configurations[configName]["properties"][propertyName])
              maxValue = convertToNumber(recommendedDefaults[configName]["property_attributes"][propertyName]["maximum"])
              if userValue > maxValue:
                validationItems.extend([{"config-name": propertyName, "item": self.getWarnItem("Value is greater than the recommended maximum of {0} ".format(maxValue))}])
            if "minimum" in recommendedDefaults[configName]["property_attributes"][propertyName] and \
                    propertyName in recommendedDefaults[configName]["properties"]:
              userValue = convertToNumber(configurations[configName]["properties"][propertyName])
              minValue = convertToNumber(recommendedDefaults[configName]["property_attributes"][propertyName]["minimum"])
              if userValue < minValue:
                validationItems.extend([{"config-name": propertyName, "item": self.getWarnItem("Value is less than the recommended minimum of {0} ".format(minValue))}])
      items.extend(self.toConfigurationValidationProblems(validationItems, configName))
    pass

  def getConfigurationsValidationItems(self):
    """Returns array of Validation objects about issues with configuration values provided in services"""
    items = []
    """
    This section of configuration is recommended configuration which is used for validation
    """
    configurations = self.services["configurations"]
    recommendations = self.recommendConfigurations()
    mpackInstances = recommendations["recommendations"]["blueprint"]["mpack_instances"]
    mpackInstancesDict = {"mpack_instances": []}
    items.append(mpackInstancesDict)
    mpackInstancesList = mpackInstancesDict.get("mpack_instances")
    for mpackInstance in mpackInstances:
      recommendedDefaults = mpackInstance["configurations"]
      mpackName = mpackInstance["name"]
      mpackVersion = mpackInstance["version"]
      mpackInstanceConfigurations = {}
      mpackItems = []
      mpackInstanceItem = {"name": mpackName, "version": mpackVersion, "items": mpackItems}
      mpackInstancesList.append(mpackInstanceItem)
      for serviceInstance in self.mpackServiceInstancesDict.get(mpackName):
        mpackItems.extend(self.getConfigurationsValidationItemsForService(configurations, recommendedDefaults, serviceInstance, self.services, self.hostsList))
      self.validateMinMax(mpackItems, recommendedDefaults, configurations)
    clusterWideItems = self.validateClusterConfigurations()
    items.append({"cluster": clusterWideItems})
    return items

  def validateListOfConfigUsingMethod(self, configurations, recommendedDefaults, services, hosts, validators):
    """
    Service Advisors can use this method to pass in a list of validators, each of which is a tuple of a
    a config type (string) and a function (pointer). Each validator is then executed.
    :param validators: List of tuples like [("hadoop-env", someFunctionPointer), ("hdfs-site", someFunctionPointer)]
    :return: List of validation errors
    """
    items = []
    for (configType, method) in validators:
      if configType in recommendedDefaults:
        siteProperties = self.getSiteProperties(configurations, configType)
        if siteProperties is not None:
          siteRecommendations = recommendedDefaults[configType]["properties"]
          self.logger.info("SiteName: %s, method: %s" % (configType, method.__name__))
          self.logger.info("Site properties: %s" % str(siteProperties))
          self.logger.info("Recommendations: %s" % str(siteRecommendations))
          validationItems = method(siteProperties, siteRecommendations, configurations, services, hosts)
          items.extend(validationItems)
    return items

  def validateConfigurationsForSite(self, configurations, recommendedDefaults, services, hosts, siteName, method):
    """
    Deprecated, please use validateListOfConfigUsingMethod
    :return: List of validation errors by calling the corresponding method.
    """
    if siteName in recommendedDefaults:
      siteProperties = self.getSiteProperties(configurations, siteName)
      if siteProperties is not None:
        siteRecommendations = recommendedDefaults[siteName]["properties"]
        self.logger.info("SiteName: %s, method: %s" % (siteName, method.__name__))
        self.logger.info("Site properties: %s" % str(siteProperties))
        self.logger.info("Recommendations: %s" % str(siteRecommendations))
        return method(siteProperties, siteRecommendations, configurations, services, hosts)
    return []

  def getConfigurationsValidationItemsForService(self, configurations, recommendedDefaults, serviceInstance, services, hosts):
    items = []
    serviceName = serviceInstance.getType()
    validator = self.validateServiceConfigurations(serviceName)
    if validator is not None:
      for siteName, method in validator.items():
        resultItems = self.validateConfigurationsForSite(configurations, recommendedDefaults, services, hosts, siteName, method)
        items.extend(resultItems)

    serviceAdvisor = serviceInstance.getServiceAdvisor()
    if serviceAdvisor is not None:
      items.extend(serviceAdvisor.getServiceConfigurationsValidationItems(configurations, recommendedDefaults, services, hosts))

    return items

  def recommendConfigGroupsConfigurations(self, recommendations):
    recommendations["recommendations"]["config-groups"] = []
    for configGroup in self.services["config-groups"]:

      # Override configuration with the config group values
      cgServices = self.services.copy()
      for configName in configGroup["configurations"].keys():
        if configName in cgServices["configurations"]:
          cgServices["configurations"][configName]["properties"].update(
            configGroup["configurations"][configName]['properties'])
        else:
          cgServices["configurations"][configName] = \
          configGroup["configurations"][configName]

      # Override hosts with the config group hosts
      cgHosts = {"items": [host for host in self.hostsList["items"] if
                           host["Hosts"]["host_name"] in configGroup["hosts"]]}

      # Override clusterSummary
      cgClusterSummary = self.getConfigurationClusterSummary(cgHosts, cgServices)

      configurations = {}

      # there can be dependencies between service recommendations which require special ordering
      # for now, make sure custom services (that have service advisors) run after standard ones
      servicesList = [cgServices["StackServices"]["service_name"] for service in cgServices["services"]]
      serviceInstances = [serviceInstance for serviceInstance in self.serviceInstancesSet if
                          serviceInstance.getType() in servicesList]
      serviceAdvisors = []
      for serviceInstance in serviceInstances:
        serviceName = serviceInstance.getType()
        calculation = self.getServiceConfigurationRecommenderForSSODict(serviceName) if isSSO \
          else self.getServiceConfigurationRecommender(serviceName)
        if calculation:
          calculation(configurations, cgClusterSummary, cgServices, cgHosts)
        else:
          serviceAdvisor = serviceInstance.getServiceAdvisor()
          if serviceAdvisor:
            serviceAdvisors.append(serviceAdvisor)
      for serviceAdvisor in serviceAdvisors:
        serviceAdvisor.getServiceConfigurationRecommendations(configurations, cgClusterSummary, cgServices, cgHosts)

      cgRecommendation = {
        "configurations": {},
        "dependent_configurations": {},
        "hosts": configGroup["hosts"]
      }

      recommendations["recommendations"]["config-groups"].append(
        cgRecommendation)

      # Parse results.
      for config in configurations.keys():
        cgRecommendation["configurations"][config] = {}
        cgRecommendation["dependent_configurations"][config] = {}
        # property + property_attributes
        for configElement in configurations[config].keys():
          cgRecommendation["configurations"][config][configElement] = {}
          cgRecommendation["dependent_configurations"][config][
            configElement] = {}
          for property, value in configurations[config][configElement].items():
            if config in configGroup["configurations"]:
              cgRecommendation["configurations"][config][configElement][
                property] = value
            else:
              cgRecommendation["dependent_configurations"][config][
                configElement][property] = value

  def getRecommendConfigurations(self, isSSO=False):

    recommendations = {
      "recommendations": {
        "blueprint": {
          "mpack_instances": []
        },
        "blueprint_cluster_binding": {
          "host_groups": []
        }
      }
    }

    # If recommendation for config groups
    if "config-groups" in self.services:
      self.recommendConfigGroupsConfigurations(recommendations)
    else:
      clusterSummary = self.getConfigurationClusterSummary(self.hostsList, self.services)
      mpackInstances = recommendations["recommendations"]["blueprint"]["mpack_instances"]
      for mpackInstanceName, serviceInstances in self.mpackServiceInstancesDict.items():
        mpackInstance = {}
        mpackInstance["name"] = mpackInstanceName
        mpackInstance["version"] = serviceInstances[0].getMpackVersion()
        mpackInstance["service_instances"] = [serviceInstance.getName() for serviceInstance in serviceInstances]
        mpackInstance["configurations"] = {}
        mpackInstance["hosts"] = []
        mpackInstances.append(mpackInstance)
        configurations = mpackInstance["configurations"]
        # there can be dependencies between service recommendations which require special ordering
        # for now, make sure custom services (that have service advisors) run after standard ones
        serviceAdvisors = []
        for serviceInstance in serviceInstances:
          serviceName = serviceInstance.getType()
          calculation = self.getServiceConfigurationRecommenderForSSODict(serviceName) if isSSO \
                        else self.getServiceConfigurationRecommender(serviceName)
          if calculation:
            # ???????
            calculation(configurations, clusterSummary, self.services, self.hostsList)
          else:
            serviceAdvisor = serviceInstance.getServiceAdvisor()
            if serviceAdvisor:
              serviceAdvisors.append(serviceAdvisor)
        for serviceAdvisor in serviceAdvisors:
          if isSSO:
            serviceAdvisor.getServiceConfigurationRecommendationsForSSO(configurations, clusterSummary, self.services, self.hostsList)
          else:
            serviceAdvisor.getServiceConfigurationRecommendations(configurations, clusterSummary, self.services, self.hostsList)


    return recommendations

  def recommendConfigurations(self):

    return self.getRecommendConfigurations()

  def recommendConfigurationsForSSO(self):

    getRecommendConfigurations(True)

  def getServiceConfigurationRecommender(self, service):
    return self.getServiceConfigurationRecommenderDict().get(service, None)

  def getServiceConfigurationRecommenderDict(self):
    return {}

  def getServiceConfigurationRecommenderForSSODict(self):
    return {}

  # Recommendation helper methods
  def isComponentHostsPopulated(self, component):
    hostnames = self.getComponentAttribute(component, "hostnames")
    if hostnames is not None:
      return len(hostnames) > 0
    return False

  def checkSiteProperties(self, siteProperties, *propertyNames):
    """
    Check if properties defined in site properties.
    :param siteProperties: config properties dict
    :param *propertyNames: property names to validate
    :returns: True if all properties defined, in other cases returns False
    """
    if siteProperties is None:
      return False
    for name in propertyNames:
      if not (name in siteProperties):
        return False
    return True

  def get_ambari_configuration(self, services):
    """
    Gets the AmbariConfiguration object that can be used to request details about
    the Ambari configuration. For example LDAP and SSO configurations

    :param services: the services structure containing the "ambari-server-configurations" block
    :return: an AmbariConfiguration
    """
    return AmbariConfiguration(services)

  def is_secured_cluster(self):
    """
    Detects if cluster is secured or not
    :type services dict
    :rtype bool
    """
    return self.services and "cluster-settings" in self.services["configurations"] and \
           "security_enabled" in self.services["configurations"]["cluster-settings"]["properties"] and \
           self.services["configurations"]["cluster-settings"]["properties"]["security_enabled"].lower() == "true"

  def getZKHostPortString(self, services, include_port=True):
    """
    Returns the comma delimited string of zookeeper server host with the configure port installed in a cluster
    Example: zk.host1.org:2181,zk.host2.org:2181,zk.host3.org:2181
    include_port boolean param -> If port is also needed.
    """
    servicesList = [service["StackServices"]["service_name"] for service in services["services"]]
    include_zookeeper = "ZOOKEEPER" in servicesList
    zookeeper_host_port = ''

    if include_zookeeper:
      zookeeper_hosts = self.getHostNamesWithComponent("ZOOKEEPER", "ZOOKEEPER_SERVER", services)
      zookeeper_host_port_arr = []

      if include_port:
        zookeeper_port = self.getZKPort(services)
        for i in range(len(zookeeper_hosts)):
          zookeeper_host_port_arr.append(zookeeper_hosts[i] + ':' + zookeeper_port)
      else:
        for i in range(len(zookeeper_hosts)):
          zookeeper_host_port_arr.append(zookeeper_hosts[i])

      zookeeper_host_port = ",".join(zookeeper_host_port_arr)
    return zookeeper_host_port

  def getZKPort(self, services):
    zookeeper_port = '2181'     #default port
    if 'zoo.cfg' in services['configurations'] and ('clientPort' in services['configurations']['zoo.cfg']['properties']):
      zookeeper_port = services['configurations']['zoo.cfg']['properties']['clientPort']
    return zookeeper_port

  def isClientComponent(self, component):
    return self.getComponentAttribute(component, "component_category") == 'CLIENT'

  def isSlaveComponent(self, component):
    return self.getComponentAttribute(component, "component_category") == 'SLAVE'

  def isMasterComponent(self, component):
    return self.getComponentAttribute(component, "is_master")

  def getRequiredComponent(self, mpackName, componentName):
    """
    Return Category for component

    :type componentName str
    :rtype dict
    """
    componentsListList = [serviceInstance.getComponents() for serviceInstance in self.serviceInstancesSet]
    componentsList = [item["StackServiceComponents"] for sublist in componentsListList for item in sublist]
    component = next((component for component in componentsList
                      if component["component_name"] == componentName and component["stack_name"] == mpackName), None)

    return component

  def getComponentAttribute(self, component, attribute):
    serviceInstanceComponent = component.get("StackServiceComponents")
    if not serviceInstanceComponent:
      return None
    return serviceInstanceComponent.get(attribute)

  def isLocalHost(self, hostName):
    return socket.getfqdn(hostName) == socket.getfqdn()

  def isMasterComponentWithMultipleInstances(self, component):
    componentName = self.getComponentName(component)
    masters = self.getMastersWithMultipleInstances()
    return componentName in masters

  def isComponentNotValuable(self, component):
    componentName = self.getComponentName(component)
    # here we use service because the set is populated by the service advisor
    service = self.getNotValuableComponents()
    return componentName in service

  def getMinComponentCount(self, component, hosts):
    componentName = self.getComponentName(component)
    return self.getComponentCardinality(componentName, hosts)["min"]

  # Helper dictionaries
  def getComponentCardinality(self, componentName, hosts):
    dict = self.getCardinalitiesDict(hosts)
    if componentName in dict:
      return dict[componentName]
    else:
      return {"min": 1, "max": 1}

  def isServiceDeployed(self, serviceInstances, serviceName):
    servicesInstancesList = [serviceInstance.getType() for serviceInstance in se]
    return serviceName in servicesList

  def getHostForComponent(self, component, hostsList):
    if len(hostsList) == 0:
      return None

    componentName = self.getComponentName(component)

    if len(hostsList) != 1:
      scheme = self.getComponentLayoutSchemes().get(componentName, None)
      if scheme is not None:
        hostIndex = next((index for key, index in scheme.iteritems() if isinstance(key, (int, long)) and len(hostsList) < key), scheme['else'])
      else:
        hostIndex = 0
      for host in hostsList[hostIndex:]:
        if self.isHostSuitableForComponent(host, component):
          return host
    return hostsList[0]

  def getComponentName(self, component):
    return self.getComponentAttribute(component, "component_name")

  def isHostSuitableForComponent(self, host, component):
    return not (self.getComponentName(component) in self.getNotPreferableOnServerComponents() and self.isLocalHost(host))

  def getMastersWithMultipleInstances(self):
    return self.mastersWithMultipleInstances

  def getNotValuableComponents(self):
    return self.notValuableComponents

  def getNotPreferableOnServerComponents(self):
    return self.notPreferableOnServerComponents

  def getCardinalitiesDict(self, hosts):
    return self.cardinalitiesDict

  def getComponentLayoutSchemes(self):
    """
    Provides layout scheme dictionaries for components.

    The scheme dictionary basically maps the number of hosts to
    host index where component should exist.
    """
    return self.componentLayoutSchemes

  def getWarnItem(self, message):
    """
    Utility method used for validation warnings.
    """
    return {"level": "WARN", "message": message}

  def getErrorItem(self, message):
    """
    Utility method used for validation errors.
    """
    return {"level": "ERROR", "message": message}

  def getNotApplicableItem(self, message):
    '''
    Creates report about validation error that can not be ignored. 
    UI should not allow the proceeding of work.
    :param message: error description.
    :return: report about error.
    '''
    return {"level": "NOT_APPLICABLE", "message": message}

  def getComponentHostNames(self, servicesDict, serviceName, componentName):
    for service in servicesDict["services"]:
      if service["StackServices"]["service_name"] == serviceName:
        for component in service['components']:
          if component["StackServiceComponents"]["component_name"] == componentName:
            return component["StackServiceComponents"]["hostnames"]

  def recommendConfigurationDependencies(self, services, hosts):
    self.allRequestedProperties = self.getAllRequestedProperties(services)
    result = self.recommendConfigurations()
    return self.filterResult(result, services)

  # returns recommendations only for changed and depended properties
  def filterResult(self, result, services):
    allRequestedProperties = self.getAllRequestedProperties(services)
    self.filterConfigs(result['recommendations']['blueprint']['configurations'], allRequestedProperties)
    if "config-groups" in services:
      for configGroup in result['recommendations']["config-groups"]:
        self.filterConfigs(configGroup["configurations"], allRequestedProperties)
        self.filterConfigs(configGroup["dependent_configurations"], allRequestedProperties)
    return result

  def filterConfigs(self, configs, requestedProperties):

    filteredConfigs = {}
    for type, names in configs.items():
      if 'properties' in names.keys():
        for name in names['properties']:
          if type in requestedProperties.keys() and \
                  name in requestedProperties[type]:
            if type not in filteredConfigs.keys():
              filteredConfigs[type] = {'properties': {}}
            filteredConfigs[type]['properties'][name] = \
              configs[type]['properties'][name]
      if 'property_attributes' in names.keys():
        for name in names['property_attributes']:
          if type in requestedProperties.keys() and \
                  name in requestedProperties[type]:
            if type not in filteredConfigs.keys():
              filteredConfigs[type] = {'property_attributes': {}}
            elif 'property_attributes' not in filteredConfigs[type].keys():
              filteredConfigs[type]['property_attributes'] = {}
            filteredConfigs[type]['property_attributes'][name] = \
              configs[type]['property_attributes'][name]
    configs.clear()
    configs.update(filteredConfigs)

  def getAllRequestedProperties(self, services):
    affectedConfigs = self.getAffectedConfigs(services)
    allRequestedProperties = {}
    for config in affectedConfigs:
      if config['type'] in allRequestedProperties:
        allRequestedProperties[config['type']].append(config['name'])
      else:
        allRequestedProperties[config['type']] = [config['name']]
    return allRequestedProperties

  def getAffectedConfigs(self, services):
    """returns properties dict including changed-configurations and depended-by configs"""
    changedConfigs = services['changed-configurations']
    changedConfigs = [{"type": entry["type"], "name": entry["name"]} for entry in changedConfigs]
    allDependencies = []

    for item in services['services']:
      allDependencies.extend(item['configurations'])

    dependencies = []

    size = -1
    while size != len(dependencies):
      size = len(dependencies)
      for config in allDependencies:
        property = {
          "type": re.sub('\.xml$', '', config['StackConfigurations']['type']),
          "name": config['StackConfigurations']['property_name']
        }
        if property in dependencies or property in changedConfigs:
          for dependedConfig in config['dependencies']:
            dependency = {
              "name": dependedConfig["StackConfigurationDependency"]["dependency_name"],
              "type": dependedConfig["StackConfigurationDependency"]["dependency_type"]
            }
            if dependency not in dependencies:
              dependencies.append(dependency)

    if "forced-configurations" in services and services["forced-configurations"] is not None:
      dependencies.extend(services["forced-configurations"])
    return  dependencies

  def versionCompare(self, version1, version2):
    def normalize(v):
      return [int(x) for x in re.sub(r'(\.0+)*$','', v).split(".")]
    return cmp(normalize(version1), normalize(version2))

  def getSiteProperties(self, configurations, siteName):
    siteConfig = configurations.get(siteName)
    if siteConfig is None:
      return None
    return siteConfig.get("properties")

  def getServicesSiteProperties(self, services, siteName):
    if not services:
      return {}

    configurations = services.get("configurations")
    if not configurations:
      return {}
    siteConfig = configurations.get(siteName)
    if siteConfig is None:
      return {}
    return siteConfig.get("properties")

  def putProperty(self, config, configType, services=None):
    userConfigs = {}
    changedConfigs = []
    # if services parameter, prefer values, set by user
    if services:
      if 'configurations' in services.keys():
        userConfigs = services['configurations']
      if 'changed-configurations' in services.keys():
        changedConfigs = services["changed-configurations"]

    if configType not in config:
      config[configType] = {}
    if"properties" not in config[configType]:
      config[configType]["properties"] = {}
    def appendProperty(key, value):
      # If property exists in changedConfigs, do not override, use user defined property
      if not self.isPropertyRequested(configType, key, changedConfigs) \
          and configType in userConfigs and key in userConfigs[configType]['properties']:
        config[configType]["properties"][key] = userConfigs[configType]['properties'][key]
      else:
        config[configType]["properties"][key] = str(value)
    return appendProperty

  def __isPropertyInChangedConfigs(self, configType, propertyName, changedConfigs):
    for changedConfig in changedConfigs:
      if changedConfig['type']==configType and changedConfig['name']==propertyName:
        return True
    return False

  def isPropertyRequested(self, configType, propertyName, changedConfigs):
    # When the property depends on more than one property, we need to recalculate it based on the actual values
    # of all related properties. But "changed-configurations" usually contains only one on the dependent on properties.
    # So allRequestedProperties is used to avoid recommendations of other properties that are not requested.
    # Calculations should use user provided values for all properties that we depend on, not only the one that
    # came in the "changed-configurations".
    if self.allRequestedProperties:
      return configType in self.allRequestedProperties and propertyName in self.allRequestedProperties[configType]
    else:
      return not self.__isPropertyInChangedConfigs(configType, propertyName, changedConfigs)

  def updateProperty(self, config, configType, services=None):
    userConfigs = {}
    changedConfigs = []
    # if services parameter, prefer values, set by user
    if services:
      if 'configurations' in services.keys():
        userConfigs = services['configurations']
      if 'changed-configurations' in services.keys():
        changedConfigs = services["changed-configurations"]

    if configType not in config:
      config[configType] = {}
    if "properties" not in config[configType]:
      config[configType]["properties"] = {}

    def updatePropertyWithCallback(key, value, callback):
      # If property exists in changedConfigs, do not override, use user defined property
      if self.__isPropertyInChangedConfigs(configType, key, changedConfigs):
        config[configType]["properties"][key] = userConfigs[configType]['properties'][key]
      else:
        # Give the callback an empty string if the mapping doesn't exist
        current_value = ""
        if key in config[configType]["properties"]:
          current_value = config[configType]["properties"][key]

        config[configType]["properties"][key] = callback(current_value, value)

    return updatePropertyWithCallback

  def putPropertyAttribute(self, config, configType):
    if configType not in config:
      config[configType] = {}
    def appendPropertyAttribute(key, attribute, attributeValue):
      if "property_attributes" not in config[configType]:
        config[configType]["property_attributes"] = {}
      if key not in config[configType]["property_attributes"]:
        config[configType]["property_attributes"][key] = {}
      config[configType]["property_attributes"][key][attribute] = attributeValue if isinstance(attributeValue, list) else str(attributeValue)
    return appendPropertyAttribute

  def getHosts(self, componentsList, componentName):
    """
    Returns the hosts which are running the given component.
    """
    hostNamesList = [component["hostnames"] for component in componentsList if component["component_name"] == componentName]
    return hostNamesList[0] if len(hostNamesList) > 0 else []

  def getServiceComponents(self, services, serviceName):
    """
    Return list of components for serviceName service

    :type services dict
    :type serviceName str
    :rtype list
    """
    components = []

    if not services or not serviceName:
      return components

    for service in services["services"]:
      if service["StackServices"]["service_name"] == serviceName:
        components.extend(service["components"])
        break

    return components

  def getHostsForComponent(self, services, serviceName, componentName):
    """
    Returns the host(s) on which a requested service's component is hosted.

    :argument services Configuration information for the cluster
    :argument serviceName Passed-in service in consideration
    :argument componentName Passed-in component in consideration

    :type services dict
    :type serviceName str
    :type componentName str
    :rtype list
    """
    hosts_for_component = []
    components = self.getServiceComponents(services, serviceName)

    for component in components:
      if component["StackServiceComponents"]["component_name"] == componentName:
        hosts_for_component.extend(component["StackServiceComponents"]["hostnames"])
        break

    return hosts_for_component

  def getMountPoints(self, hosts):
    """
    Return list of mounts available on the hosts

    :type hosts dict
    """
    mount_points = []

    for item in hosts["items"]:
      if "disk_info" in item["Hosts"]:
        mount_points.append(item["Hosts"]["disk_info"])

    return mount_points

  def getStackRoot(self):
    """
    Gets the stack root associated with the stack
    :param services: the services structure containing the current configurations
    :return: the stack root as specified in the config or /usr/hdp
    """
    cluster_settings = self.getServicesSiteProperties(self.services, "cluster-settings")
    stack_root = "/usr/hdp"
    if cluster_settings and "stack_root" in cluster_settings:
      stack_root_as_str = cluster_settings["stack_root"]
      stack_roots = json.loads(stack_root_as_str)
      if "stack_name" in cluster_settings:
        stack_name = cluster_settings["stack_name"]
        if stack_name in stack_roots:
          stack_root = stack_roots[stack_name]

    return stack_root

  def isSecurityEnabled(self, services):
    """
    back compatibilty
    :param services:
    :return:
    """
    return self.isSecurityEnabled()

  def isSecurityEnabled(self):
    """
    Determines if security is enabled by testing the value of cluster-settings/security enabled.

    If the property exists and is equal to "true", then is it enabled; otherwise is it assumed to be
    disabled.

    This is an alias for stacks.stack_advisor.MpackAdvisor#is_secured_cluster

    :return: true if security is enabled; otherwise false
    """
    return self.is_secured_cluster()

  def parseCardinality(self, cardinality, hostsCount):
    """
    Cardinality types: 1+, 1-2, 1, ALL
    @return: a tuple: (minHosts, maxHosts) or (None, None) if cardinality string is invalid
    """
    if not cardinality:
      return (None, None)

    if "+" in cardinality:
      return (int(cardinality[:-1]), int(hostsCount))
    elif "-" in cardinality:
      nums = cardinality.split("-")
      return (int(nums[0]), int(nums[1]))
    elif "ALL" == cardinality:
      return (int(hostsCount), int(hostsCount))
    elif cardinality.isdigit():
      return (int(cardinality),int(cardinality))

    return (None, None)

  def getServiceNames(self, services):
    return [service["StackServices"]["service_name"] for service in services["services"]]

  def filterHostMounts(self, hosts):
    """
    Filter mounts on the host using agent_mounts_ignore_list, by excluding and record with mount-point
     mentioned in agent_mounts_ignore_list.

    This function updates hosts dictionary

    Example:

      agent_mounts_ignore_list : "/run/secrets"

      Hosts record :

       "disk_info" : [
          {
              ...
            "mountpoint" : "/"
          },
          {
              ...
            "mountpoint" : "/run/secrets"
          }
        ]

      Result would be :

        "disk_info" : [
          {
              ...
            "mountpoint" : "/"
          }
        ]

    :type hosts dict
    :type services dict
    """
    if not self.services or "items" not in hosts:
      return hosts

    banned_filesystems = ["devtmpfs", "tmpfs", "vboxsf", "cdfs"]
    banned_mount_points = ["/etc/resolv.conf", "/etc/hostname", "/boot", "/mnt", "/tmp", "/run/secrets"]

    cluster_settings = self.getServicesSiteProperties(self.services, "cluster-settings")
    ignore_list = []

    if cluster_settings and "agent_mounts_ignore_list" in cluster_settings and cluster_settings["agent_mounts_ignore_list"].strip():
      ignore_list = [x.strip() for x in cluster_settings["agent_mounts_ignore_list"].strip().split(",")]

    ignore_list.extend(banned_mount_points)
    for host in hosts["items"]:
      if "Hosts" not in host and "disk_info" not in host["Hosts"]:
        continue

      host = host["Hosts"]
      disk_info = []

      for disk in host["disk_info"]:
        if disk["mountpoint"] not in ignore_list\
          and disk["type"].lower() not in banned_filesystems:
          disk_info.append(disk)

      host["disk_info"] = disk_info

    return hosts

  def __getSameHostMounts(self, hosts):
    """
    Return list of the mounts which are same and present on all hosts

    :type hosts dict
    :rtype list
    """
    if not hosts:
      return None

    hostMounts = self.getMountPoints(hosts)
    mounts = []
    for m in hostMounts:
      host_mounts = set([item["mountpoint"] for item in m])
      mounts = host_mounts if not mounts else mounts & host_mounts

    return sorted(mounts)

  def getMountPathVariations(self, initial_value, component_name, services, hosts):
    """
    Recommends best fitted mount by prefixing path with it.

    :return return list of paths with properly selected paths. If no recommendation possible,
     would be returned empty list

    :type initial_value str
    :type component_name str
    :type services dict
    :type hosts dict
    :rtype list
    """
    available_mounts = []

    if not initial_value:
      return available_mounts

    mounts = self.__getSameHostMounts(hosts)
    sep = "/"

    if not mounts:
      return available_mounts

    for mount in mounts:
      new_mount = initial_value if mount == "/" else os.path.join(mount + sep, initial_value.lstrip(sep))
      if new_mount not in available_mounts:
        available_mounts.append(new_mount)

    # no list transformations after filling the list, because this will cause item order change
    return available_mounts

  def getMountPathVariation(self, initial_value, component_name, services, hosts):
    """
    Recommends best fitted mount by prefixing path with it.

    :return return list of paths with properly selected paths. If no recommendation possible,
     would be returned empty list

    :type initial_value str
        :type component_name str
    :type services dict
    :type hosts dict
    :rtype str
    """
    try:
      return [self.getMountPathVariations(initial_value, component_name, services, hosts)[0]]
    except IndexError:
      return []

  def updateMountProperties(self, siteConfig, propertyDefinitions, configurations,  services, hosts):
    """
    Update properties according to recommendations for available mount-points

    propertyDefinitions is an array of set : property name, component name, initial value, recommendation type

     Where,

       property name - name of the property
       component name, name of the component to which belongs this property
       initial value - initial path
       recommendation type - could be "multi" or "single". This describes recommendation strategy, to use only one disk
        or use all available space on the host

    :type propertyDefinitions list
    :type siteConfig str
    :type configurations dict
    :type services dict
    :type hosts dict
    """

    props = self.getServicesSiteProperties(services, siteConfig)
    put_f = self.putProperty(configurations, siteConfig, services)

    for prop_item in propertyDefinitions:
      name, component, default_value, rc_type = prop_item
      recommendation = None

      if props is None or name not in props:
        if rc_type == "multi":
          recommendation = self.getMountPathVariations(default_value, component, services, hosts)
        else:
          recommendation = self.getMountPathVariation(default_value, component, services, hosts)
      elif props and name in props and props[name] == default_value:
        if rc_type == "multi":
          recommendation = self.getMountPathVariations(default_value, component, services, hosts)
        else:
          recommendation = self.getMountPathVariation(default_value, component, services, hosts)

      if recommendation:
        put_f(name, ",".join(recommendation))

  def getHostNamesWithComponent(self, serviceName, componentName, services):
    """
    Returns the list of hostnames on which service component is installed
    """
    if services is not None and serviceName in [service["StackServices"]["service_name"] for service in services["services"]]:
      service = [serviceEntry for serviceEntry in services["services"] if serviceEntry["StackServices"]["service_name"] == serviceName][0]
      components = [componentEntry for componentEntry in service["components"] if componentEntry["StackServiceComponents"]["component_name"] == componentName]
      if (len(components) > 0 and len(components[0]["StackServiceComponents"]["hostnames"]) > 0):
        componentHostnames = components[0]["StackServiceComponents"]["hostnames"]
        return componentHostnames
    return []

  def getHostsWithComponent(self, serviceName, componentName, services, hosts):
    """
    This method should get for a service with multiple instances, how many hosts have this component installed
    and how many same components running on a host
    :param serviceName:
    :param componentName:
    :param serviceInstances:
    :param hosts:
    :return:
    """
    if services is not None and hosts is not None and serviceName in [service["StackServices"]["service_name"] for service in services["services"]]:
      service = [serviceEntry for serviceEntry in services["services"] if serviceEntry["StackServices"]["service_name"] == serviceName][0]
      components = [componentEntry for componentEntry in service["components"] if componentEntry["StackServiceComponents"]["component_name"] == componentName]
      if (len(components) > 0 and len(components[0]["StackServiceComponents"]["hostnames"]) > 0):
        componentHostnames = components[0]["StackServiceComponents"]["hostnames"]
        componentHosts = [host for host in hosts["items"] if host["Hosts"]["host_name"] in componentHostnames]
        return componentHosts
    return []

  def getHostWithComponent(self, serviceName, componentName, services, hosts):
    componentHosts = self.getHostsWithComponent(serviceName, componentName, services, hosts)
    if (len(componentHosts) > 0):
      return componentHosts[0]
    return None

  def getHostComponentsByCategories(self, hostname, categories, services, hosts):
    components = []
    if services is not None and hosts is not None:
      for service in services["services"]:
        components.extend([componentEntry for componentEntry in service["components"]
                           if componentEntry["StackServiceComponents"]["component_category"] in categories
                           and hostname in componentEntry["StackServiceComponents"]["hostnames"]])
    return components

  def get_services_list(self, services):
    """
    Returns available services as list

    :type services dict
    :rtype list
    """
    if not services:
      return []

    return [service["StackServices"]["service_name"] for service in services["services"]]

  def get_service_component_meta(self, service, component, services):
    """
    Function retrieve service component meta information as dict from services.json
    If no service or component found, would be returned empty dict

    Return value example:
        "advertise_version" : true,
        "bulk_commands_display_name" : "",
        "bulk_commands_master_component_name" : "",
        "cardinality" : "1+",
        "component_category" : "CLIENT",
        "component_name" : "HBASE_CLIENT",
        "custom_commands" : [ ],
        "decommission_allowed" : false,
        "display_name" : "HBase Client",
        "has_bulk_commands_definition" : false,
        "is_client" : true,
        "is_master" : false,
        "reassign_allowed" : false,
        "recovery_enabled" : false,
        "service_name" : "HBASE",
        "stack_name" : "HDP",
        "stack_version" : "2.5",
        "hostnames" : [ "host1", "host2" ]

    :type service str
    :type component str
    :type services dict
    :rtype dict
    """
    __stack_services = "StackServices"
    __stack_service_components = "StackServiceComponents"

    if not services:
      return {}

    service_meta = [item for item in services["services"] if item[__stack_services]["service_name"] == service]
    if len(service_meta) == 0:
      return {}

    service_meta = service_meta[0]
    component_meta = [item for item in service_meta["components"] if item[__stack_service_components]["component_name"] == component]

    if len(component_meta) == 0:
      return {}

    return component_meta[0][__stack_service_components]

  #region HDFS
  def getHadoopProxyUsersValidationItems(self, properties, services, hosts, configurations):
    validationItems = []
    users = self.getHadoopProxyUsers(services, hosts, configurations)
    for user_name, user_properties in users.iteritems():
      props = ["hadoop.proxyuser.{0}.hosts".format(user_name)]
      if "propertyGroups" in user_properties:
        props.append("hadoop.proxyuser.{0}.groups".format(user_name))

      for prop in props:
        validationItems.append({"config-name": prop, "item": self.validatorNotEmpty(properties, prop)})

    return validationItems

  def getHadoopProxyUsers(self, services, hosts, configurations):
    """
    Gets Hadoop Proxy User recommendations based on the configuration that is provided by
    getServiceHadoopProxyUsersConfigurationDict.

    See getServiceHadoopProxyUsersConfigurationDict
    """
    servicesList = self.get_services_list(services)
    users = {}

    for serviceName, serviceUserComponents in self.getServiceHadoopProxyUsersConfigurationDict().iteritems():
      users.update(self._getHadoopProxyUsersForService(serviceName, serviceUserComponents, services, hosts, configurations))

    return users

  def getServiceHadoopProxyUsersConfigurationDict(self):
    """
    Returns a map that is used by 'getHadoopProxyUsers' to determine service
    user properties and related components and get proxyuser recommendations.
    This method can be overridden in stackadvisors for the further stacks to
    add additional services or change the previous logic.

    Example of the map format:
    {
      "serviceName": [
        ("configTypeName1", "userPropertyName1", {"propertyHosts": "*", "propertyGroups": "exact string value"})
        ("configTypeName2", "userPropertyName2", {"propertyHosts": ["COMPONENT1", "COMPONENT2", "COMPONENT3"], "propertyGroups": "*"}),
        ("configTypeName3", "userPropertyName3", {"propertyHosts": ["COMPONENT1", "COMPONENT2", "COMPONENT3"]}, filterFunction)
      ],
      "serviceName2": [
        ...
    }

    If the third element of a tuple is map that maps proxy property to it's value.
    The key could be either 'propertyHosts' or 'propertyGroups'. (Both are optional)
    If the map value is a string, then this string will be used for the proxyuser
    value (e.g. 'hadoop.proxyuser.{user}.hosts' = '*').
    Otherwise map value should be alist or a tuple with component names.
    All hosts with the provided components will be added
    to the property (e.g. 'hadoop.proxyuser.{user}.hosts' = 'host1,host2,host3')

    The forth element of the tuple is optional and if it's provided,
    it should be a function that takes two arguments: services and hosts.
    If it returns False, proxyusers for the tuple will not be added.
    """
    ALL_WILDCARD = "*"
    HOSTS_PROPERTY = "propertyHosts"
    GROUPS_PROPERTY = "propertyGroups"

    return {
      "HDFS":   [("hadoop-env", "hdfs_user", {HOSTS_PROPERTY: ALL_WILDCARD, GROUPS_PROPERTY: ALL_WILDCARD})],
      "OOZIE":  [("oozie-env", "oozie_user", {HOSTS_PROPERTY: ["OOZIE_SERVER"], GROUPS_PROPERTY: ALL_WILDCARD})],
      "HIVE":   [("hive-env", "hive_user", {HOSTS_PROPERTY: ["HIVE_SERVER", "HIVE_SERVER_INTERACTIVE"], GROUPS_PROPERTY: ALL_WILDCARD}),
                 ("hive-env", "webhcat_user", {HOSTS_PROPERTY: ["WEBHCAT_SERVER"], GROUPS_PROPERTY: ALL_WILDCARD})],
      "YARN":   [("yarn-env", "yarn_user", {HOSTS_PROPERTY: ["RESOURCEMANAGER"]}, lambda services, hosts: len(self.getHostsWithComponent("YARN", "RESOURCEMANAGER", self.services, self.hostsList)) > 1)],
      "FALCON": [("falcon-env", "falcon_user", {HOSTS_PROPERTY: ALL_WILDCARD, GROUPS_PROPERTY: ALL_WILDCARD})],
      "SPARK":  [("livy-env", "livy_user", {HOSTS_PROPERTY: ALL_WILDCARD, GROUPS_PROPERTY: ALL_WILDCARD})]
    }

  def _getHadoopProxyUsersForService(self, serviceName, serviceUserComponents, services, hosts, configurations):
    self.logger.info("Calculating Hadoop Proxy User recommendations for {0} service.".format(serviceName))
    servicesList = self.get_services_list(services)
    resultUsers = {}

    if serviceName in servicesList:
      usersComponents = {}
      for values in serviceUserComponents:

        # Filter, if 4th argument is present in the tuple
        filterFunction = values[3:]
        if filterFunction and not filterFunction[0](services, hosts):
          continue

        userNameConfig, userNameProperty, hostSelectorMap = values[:3]
        user = get_from_dict(services, ("configurations", userNameConfig, "properties", userNameProperty), None)
        if user:
          usersComponents[user] = (userNameConfig, userNameProperty, hostSelectorMap)

      for user, (userNameConfig, userNameProperty, hostSelectorMap) in usersComponents.iteritems():
        proxyUsers = {"config": userNameConfig, "propertyName": userNameProperty}
        for proxyPropertyName, hostSelector in hostSelectorMap.iteritems():
          componentHostNamesString = hostSelector if isinstance(hostSelector, basestring) else '*'
          if isinstance(hostSelector, (list, tuple)):
            _, componentHostNames = self.get_data_for_proxyuser(user, services, configurations) # preserve old values
            for component in hostSelector:
              componentHosts = self.getHostsWithComponent(serviceName, component, services, hosts)
              if componentHosts is not None:
                for componentHost in componentHosts:
                  componentHostName = componentHost["Hosts"]["host_name"]
                  componentHostNames.add(componentHostName)

            componentHostNamesString = ",".join(sorted(componentHostNames))
            self.logger.info("Host List for [service='{0}'; user='{1}'; components='{2}']: {3}".format(serviceName, user, ','.join(hostSelector), componentHostNamesString))

          if not proxyPropertyName in proxyUsers:
            proxyUsers[proxyPropertyName] = componentHostNamesString

        if not user in resultUsers:
          resultUsers[user] = proxyUsers

    return resultUsers

  def recommendHadoopProxyUsers(self, configurations, services, hosts):
    servicesList = self.get_services_list(services)

    if 'forced-configurations' not in services:
      services["forced-configurations"] = []

    putCoreSiteProperty = self.putProperty(configurations, "core-site", services)
    putCoreSitePropertyAttribute = self.putPropertyAttribute(configurations, "core-site")

    users = self.getHadoopProxyUsers(services, hosts, configurations)

    # Force dependencies for HIVE
    if "HIVE" in servicesList:
      hive_user = get_from_dict(services, ("configurations", "hive-env", "properties", "hive_user"), default_value=None)
      if hive_user and get_from_dict(users, (hive_user, "propertyHosts"), default_value=None):
        services["forced-configurations"].append({"type" : "core-site", "name" : "hadoop.proxyuser.{0}.hosts".format(hive_user)})

    for user_name, user_properties in users.iteritems():

      # Add properties "hadoop.proxyuser.*.hosts", "hadoop.proxyuser.*.groups" to core-site for all users
      self.put_proxyuser_value(user_name, user_properties["propertyHosts"], services=services, configurations=configurations, put_function=putCoreSiteProperty)
      self.logger.info("Updated hadoop.proxyuser.{0}.hosts as : {1}".format(user_name, user_properties["propertyHosts"]))
      if "propertyGroups" in user_properties:
        self.put_proxyuser_value(user_name, user_properties["propertyGroups"], is_groups=True, services=services, configurations=configurations, put_function=putCoreSiteProperty)

      # Remove old properties if user was renamed
      userOldValue = self.getOldValue(services, user_properties["config"], user_properties["propertyName"])
      if userOldValue is not None and userOldValue != user_name:
        putCoreSitePropertyAttribute("hadoop.proxyuser.{0}.hosts".format(userOldValue), 'delete', 'true')
        services["forced-configurations"].append({"type" : "core-site", "name" : "hadoop.proxyuser.{0}.hosts".format(userOldValue)})
        services["forced-configurations"].append({"type" : "core-site", "name" : "hadoop.proxyuser.{0}.hosts".format(user_name)})

        if "propertyGroups" in user_properties:
          putCoreSitePropertyAttribute("hadoop.proxyuser.{0}.groups".format(userOldValue), 'delete', 'true')
          services["forced-configurations"].append({"type" : "core-site", "name" : "hadoop.proxyuser.{0}.groups".format(userOldValue)})
          services["forced-configurations"].append({"type" : "core-site", "name" : "hadoop.proxyuser.{0}.groups".format(user_name)})

    self.recommendAmbariProxyUsersForHDFS(services, configurations, servicesList, putCoreSiteProperty, putCoreSitePropertyAttribute)

  def recommendAmbariProxyUsersForHDFS(self, services, configurations, servicesList, putCoreSiteProperty, putCoreSitePropertyAttribute):
    if "HDFS" in servicesList:
      ambari_user = self.getAmbariUser(services)
      ambariHostName = socket.getfqdn()
      self.put_proxyuser_value(ambari_user, ambariHostName, services=services, configurations=configurations, put_function=putCoreSiteProperty)
      self.put_proxyuser_value(ambari_user, "*", is_groups=True, services=services, configurations=configurations, put_function=putCoreSiteProperty)
      old_ambari_user = self.getOldAmbariUser(services)
      if old_ambari_user is not None:
        putCoreSitePropertyAttribute("hadoop.proxyuser.{0}.hosts".format(old_ambari_user), 'delete', 'true')
        putCoreSitePropertyAttribute("hadoop.proxyuser.{0}.groups".format(old_ambari_user), 'delete', 'true')

  def getAmbariUser(self, services):
    ambari_user = services['ambari-server-properties']['ambari-server.user']
    if "cluster-settings" in services["configurations"] \
        and "ambari_principal_name" in services["configurations"]["cluster-settings"]["properties"] \
        and "security_enabled" in services["configurations"]["cluster-settings"]["properties"] \
        and services["configurations"]["cluster-settings"]["properties"]["security_enabled"].lower() == "true":
      ambari_user = services["configurations"]["cluster-settings"]["properties"]["ambari_principal_name"]
      ambari_user = ambari_user.split('@')[0]
    return ambari_user

  def getOldAmbariUser(self, services):
    ambari_user = None
    if "cluster-settings" in services["configurations"]:
      if "security_enabled" in services["configurations"]["cluster-settings"]["properties"] \
          and services["configurations"]["cluster-settings"]["properties"]["security_enabled"].lower() == "true":
        ambari_user = services['ambari-server-properties']['ambari-server.user']
      elif "ambari_principal_name" in services["configurations"]["cluster-settings"]["properties"]:
        ambari_user = services["configurations"]["cluster-settings"]["properties"]["ambari_principal_name"]
        ambari_user = ambari_user.split('@')[0]
    return ambari_user
  #endregion

  #region Generic
  PROXYUSER_SPECIAL_RE = [r"\$\{(?:([\w\-\.]+)/)?([\w\-\.]+)(?:\s*\|\s*(.+?))?\}"]

  @classmethod
  def preserve_special_values(cls, value):
    """
    Replace matches of PROXYUSER_SPECIAL_RE with random strings.

    :param value: input string
    :return: result string and dictionary that contains mapping random string to original value
    """
    def gen_random_str():
      return ''.join(random.choice(string.digits + string.ascii_letters) for _ in range(20))

    result = value
    replacements_dict = {}
    for regexp in cls.PROXYUSER_SPECIAL_RE:
      for match in re.finditer(regexp, value):
        matched_string = match.string[match.start():match.end()]
        rand_str = gen_random_str()
        result = result.replace(matched_string, rand_str)
        replacements_dict[rand_str] = matched_string
    return result, replacements_dict

  @staticmethod
  def restore_special_values(data, replacement_dict):
    """
    Replace random strings in data set to their original values using replacement_dict.

    :param data:
    :param replacement_dict:
    :return:
    """
    for replacement, original in replacement_dict.iteritems():
      data.remove(replacement)
      data.add(original)

  def put_proxyuser_value(self, user_name, value, is_groups=False, services=None, configurations=None, put_function=None):
    is_wildcard_value, current_value = self.get_data_for_proxyuser(user_name, services, configurations, is_groups)
    result_value = "*"
    result_values_set = self.merge_proxyusers_values(current_value, value)
    if len(result_values_set) > 0:
      result_value = ",".join(sorted([val for val in result_values_set if val]))

    if is_groups:
      property_name = "hadoop.proxyuser.{0}.groups".format(user_name)
    else:
      property_name = "hadoop.proxyuser.{0}.hosts".format(user_name)

    put_function(property_name, result_value)

  def get_data_for_proxyuser(self, user_name, services, configurations, groups=False):
    """
    Returns values of proxyuser properties for given user. Properties can be
    hadoop.proxyuser.username.groups or hadoop.proxyuser.username.hosts

    :param user_name:
    :param services:
    :param configurations:
    :param groups: if true, will return values for group property, not hosts
    :return: tuple (wildcard_value, set[values]), where wildcard_value indicates if property value was *
    """
    if "core-site" in services["configurations"]:
      coreSite = services["configurations"]["core-site"]['properties']
    else:
      coreSite = {}
    if groups:
      property_name = "hadoop.proxyuser.{0}.groups".format(user_name)
    else:
      property_name = "hadoop.proxyuser.{0}.hosts".format(user_name)
    if property_name in coreSite:
      property_value = coreSite[property_name]
      if property_value == "*":
        return True, set()
      else:
        property_value, replacement_map = self.preserve_special_values(property_value)
        result_values = set([v.strip() for v in property_value.split(",")])
        if "core-site" in configurations:
          if property_name in configurations["core-site"]['properties']:
            additional_value, additional_replacement_map = self.preserve_special_values(
                configurations["core-site"]['properties'][property_name]
            )
            replacement_map.update(additional_replacement_map)
            result_values = result_values.union([v.strip() for v in additional_value.split(",")])
        self.restore_special_values(result_values, replacement_map)
        return False, result_values
    return False, set()

  def merge_proxyusers_values(self, first, second):
    result = set()
    def append(data):
      if isinstance(data, str) or isinstance(data, unicode):
        if data != "*":
          result.update(data.split(","))
      else:
        result.update(data)
    append(first)
    append(second)
    return result

  def getOldValue(self, services, configType, propertyName):
    if services:
      changedConfigs = services.get("changed-configurations")
      for changedConfig in changedConfigs:
        if changedConfig.get("type") == configType and changedConfig.get("name")== propertyName:
          return changedConfig.get("old_value")
    return None

  @classmethod
  def isSecurePort(cls, port):
    """
    Returns True if port is root-owned at *nix systems
    """
    if port is not None:
      return port < 1024
    else:
      return False

  @classmethod
  def getPort(cls, address):
    """
    Extracts port from the address like 0.0.0.0:1019
    """
    if address is None:
      return None
    m = re.search(r'(?:http(?:s)?://)?([\w\d.]*):(\d{1,5})', address)
    if m is not None:
      return int(m.group(2))
    else:
      return None
  #endregion

  #region Validators
  def validateXmxValue(self, properties, recommendedDefaults, propertyName):
    if not propertyName in properties:
      return self.getErrorItem("Value should be set")
    value = properties[propertyName]
    defaultValue = recommendedDefaults[propertyName]
    if defaultValue is None:
      return self.getErrorItem("Config's default value can't be null or undefined")
    if not self.checkXmxValueFormat(value) and self.checkXmxValueFormat(defaultValue):
      # Xmx is in the default-value but not the value, should be an error
      return self.getErrorItem('Invalid value format')
    if not self.checkXmxValueFormat(defaultValue):
      # if default value does not contain Xmx, then there is no point in validating existing value
      return None
    valueInt = self.formatXmxSizeToBytes(self.getXmxSize(value))
    defaultValueXmx = self.getXmxSize(defaultValue)
    defaultValueInt = self.formatXmxSizeToBytes(defaultValueXmx)
    if valueInt < defaultValueInt:
      return self.getWarnItem("Value is less than the recommended default of -Xmx" + defaultValueXmx)
    return None

  def validatorLessThenDefaultValue(self, properties, recommendedDefaults, propertyName):
    if propertyName not in recommendedDefaults:
      # If a property name exists in say hbase-env and hbase-site (which is allowed), then it will exist in the
      # "properties" dictionary, but not necessarily in the "recommendedDefaults" dictionary". In this case, ignore it.
      return None

    if not propertyName in properties:
      return self.getErrorItem("Value should be set")
    value = self.to_number(properties[propertyName])
    if value is None:
      return self.getErrorItem("Value should be integer")
    defaultValue = self.to_number(recommendedDefaults[propertyName])
    if defaultValue is None:
      return None
    if value < defaultValue:
      return self.getWarnItem("Value is less than the recommended default of {0}".format(defaultValue))
    return None

  def validatorGreaterThenDefaultValue(self, properties, recommendedDefaults, propertyName):
    if propertyName not in recommendedDefaults:
      # If a property name exists in say hbase-env and hbase-site (which is allowed), then it will exist in the
      # "properties" dictionary, but not necessarily in the "recommendedDefaults" dictionary". In this case, ignore it.
      return None

    if not propertyName in properties:
      return self.getErrorItem("Value should be set")
    value = self.to_number(properties[propertyName])
    if value is None:
      return self.getErrorItem("Value should be integer")
    defaultValue = self.to_number(recommendedDefaults[propertyName])
    if defaultValue is None:
      return None
    if value > defaultValue:
      return self.getWarnItem("Value is greater than the recommended default of {0}".format(defaultValue))
    return None

  def validatorEqualsPropertyItem(self, properties1, propertyName1,
                                  properties2, propertyName2,
                                  emptyAllowed=False):
    if not propertyName1 in properties1:
      return self.getErrorItem("Value should be set for %s" % propertyName1)
    if not propertyName2 in properties2:
      return self.getErrorItem("Value should be set for %s" % propertyName2)
    value1 = properties1.get(propertyName1)
    if value1 is None and not emptyAllowed:
      return self.getErrorItem("Empty value for %s" % propertyName1)
    value2 = properties2.get(propertyName2)
    if value2 is None and not emptyAllowed:
      return self.getErrorItem("Empty value for %s" % propertyName2)
    if value1 != value2:
      return self.getWarnItem("It is recommended to set equal values "
                              "for properties {0} and {1}".format(propertyName1, propertyName2))

    return None

  def validatorEqualsToRecommendedItem(self, properties, recommendedDefaults,
                                       propertyName):
    if not propertyName in properties:
      return self.getErrorItem("Value should be set for %s" % propertyName)
    value = properties.get(propertyName)
    if not propertyName in recommendedDefaults:
      return self.getErrorItem("Value should be recommended for %s" % propertyName)
    recommendedValue = recommendedDefaults.get(propertyName)
    if value != recommendedValue:
      return self.getWarnItem("It is recommended to set value {0} "
                              "for property {1}".format(recommendedValue, propertyName))
    return None

  def validatorNotEmpty(self, properties, propertyName):
    if not propertyName in properties:
      return self.getErrorItem("Value should be set for {0}".format(propertyName))
    value = properties.get(propertyName)
    if not value:
      return self.getWarnItem("Empty value for {0}".format(propertyName))
    return None

  def validatorNotRootFs(self, properties, recommendedDefaults, propertyName, hostInfo):
    if not propertyName in properties:
      return self.getErrorItem("Value should be set")
    dir = properties[propertyName]
    if not dir.startswith("file://") or dir == recommendedDefaults.get(propertyName):
      return None

    dir = re.sub("^file://", "", dir, count=1)
    mountPoints = []
    for mountPoint in hostInfo["disk_info"]:
      mountPoints.append(mountPoint["mountpoint"])
    mountPoint = MpackAdvisorImpl.getMountPointForDir(dir, mountPoints)

    if "/" == mountPoint and self.getPreferredMountPoints(hostInfo)[0] != mountPoint:
      return self.getWarnItem("It is not recommended to use root partition for {0}".format(propertyName))

    return None

  def validatorEnoughDiskSpace(self, properties, propertyName, hostInfo, reqiuredDiskSpace):
    if not propertyName in properties:
      return self.getErrorItem("Value should be set")
    dir = properties[propertyName]
    if not dir.startswith("file://"):
      return None

    dir = re.sub("^file://", "", dir, count=1)
    mountPoints = {}
    for mountPoint in hostInfo["disk_info"]:
      mountPoints[mountPoint["mountpoint"]] = self.to_number(mountPoint["available"])
    mountPoint = MpackAdvisorImpl.getMountPointForDir(dir, mountPoints.keys())

    if not mountPoints:
      return self.getErrorItem("No disk info found on host %s" % hostInfo["host_name"])

    if mountPoints[mountPoint] < reqiuredDiskSpace:
      msg = "Ambari Metrics disk space requirements not met. \n" \
            "Recommended disk space for partition {0} is {1}G"
      return self.getWarnItem(msg.format(mountPoint, reqiuredDiskSpace/1048576)) # in Gb
    return None

  @classmethod
  def is_valid_host_port_authority(cls, target):
    has_scheme = "://" in target
    if not has_scheme:
      target = "dummyscheme://"+target
    try:
      result = urlparse(target)
      if result.hostname is not None and result.port is not None:
        return True
    except ValueError:
      pass
    return False
  #endregion

  #region YARN and MAPREDUCE
  def validatorYarnQueue(self, properties, recommendedDefaults, propertyName, services):
    if propertyName not in properties:
      return None

    capacity_scheduler_properties, _ = self.getCapacitySchedulerProperties(services)
    leaf_queue_names = self.getAllYarnLeafQueues(capacity_scheduler_properties)
    queue_name = properties[propertyName]

    if len(leaf_queue_names) == 0:
      return None
    elif queue_name not in leaf_queue_names:
      return self.getErrorItem("Queue does not exist or correspond to an existing YARN leaf queue")

    return None

  def recommendYarnQueue(self, services, catalog_name=None, queue_property=None):
    old_queue_name = None

    if services and 'configurations' in services:
        configurations = services["configurations"]
        if catalog_name in configurations and queue_property in configurations[catalog_name]["properties"]:
          old_queue_name = configurations[catalog_name]["properties"][queue_property]

        capacity_scheduler_properties, _ = self.getCapacitySchedulerProperties(services)
        leaf_queues = sorted(self.getAllYarnLeafQueues(capacity_scheduler_properties))

        if leaf_queues and (old_queue_name is None or old_queue_name not in leaf_queues):
          return leaf_queues.pop()
        elif old_queue_name and old_queue_name in leaf_queues:
          return None
    return "default"

  def isConfigPropertiesChanged(self, services, config_type, config_names, all_exists=True):
    """
    Checks for the presence of passed-in configuration properties in a given config, if they are changed.
    Reads from services["changed-configurations"].

    :argument services: Configuration information for the cluster
    :argument config_type: Type of the configuration
    :argument config_names: Set of configuration properties to be checked if they are changed.
    :argument all_exists: If True: returns True only if all properties mentioned in 'config_names_set' we found
                            in services["changed-configurations"].
                            Otherwise, returns False.
                          If False: return True, if any of the properties mentioned in config_names_set we found in
                            services["changed-configurations"].
                            Otherwise, returns False.


    :type services: dict
    :type config_type: str
    :type config_names: list|set
    :type all_exists: bool
    """
    changedConfigs = services["changed-configurations"]
    changed_config_names_set = set([changedConfig['name'] for changedConfig in changedConfigs if changedConfig['type'] == config_type])
    config_names_set = set(config_names)

    configs_intersection = changed_config_names_set & config_names_set
    if all_exists and configs_intersection == config_names_set:
      return True
    elif not all_exists and len(configs_intersection) > 0:
      return True

    return False

  def getCapacitySchedulerProperties(self, services):
    """
    Returns the dictionary of configs for 'capacity-scheduler'.
    """
    capacity_scheduler_properties = dict()
    received_as_key_value_pair = True
    if "capacity-scheduler" in services['configurations']:
      if "capacity-scheduler" in services['configurations']["capacity-scheduler"]["properties"]:
        cap_sched_props_as_str = services['configurations']["capacity-scheduler"]["properties"]["capacity-scheduler"]
        if cap_sched_props_as_str:
          cap_sched_props_as_str = str(cap_sched_props_as_str).split('\n')
          if len(cap_sched_props_as_str) > 0 and cap_sched_props_as_str[0] != 'null':
            # Received confgs as one "\n" separated string
            for property in cap_sched_props_as_str:
              key, sep, value = property.partition("=")
              capacity_scheduler_properties[key] = value
            self.logger.info("'capacity-scheduler' configs is passed-in as a single '\\n' separated string. "
                        "count(services['configurations']['capacity-scheduler']['properties']['capacity-scheduler']) = "
                        "{0}".format(len(capacity_scheduler_properties)))
            received_as_key_value_pair = False
          else:
            self.logger.info("Passed-in services['configurations']['capacity-scheduler']['properties']['capacity-scheduler'] is 'null'.")
        else:
          self.logger.info("'capacity-scheduler' configs not passed-in as single '\\n' string in "
                      "services['configurations']['capacity-scheduler']['properties']['capacity-scheduler'].")
      if not capacity_scheduler_properties:
        # Received configs as a dictionary (Generally on 1st invocation).
        capacity_scheduler_properties = services['configurations']["capacity-scheduler"]["properties"]
        self.logger.info("'capacity-scheduler' configs is passed-in as a dictionary. "
                    "count(services['configurations']['capacity-scheduler']['properties']) = {0}".format(len(capacity_scheduler_properties)))
    else:
      self.logger.error("Couldn't retrieve 'capacity-scheduler' from services.")

    self.logger.info("Retrieved 'capacity-scheduler' received as dictionary : '{0}'. configs : {1}" \
                .format(received_as_key_value_pair, capacity_scheduler_properties.items()))
    return capacity_scheduler_properties, received_as_key_value_pair

  def getAllYarnLeafQueues(self, capacitySchedulerProperties):
    """
    Gets all YARN leaf queues.
    """
    config_list = capacitySchedulerProperties.keys()
    yarn_queues = None
    leafQueueNames = set()
    if 'yarn.scheduler.capacity.root.queues' in config_list:
      yarn_queues = capacitySchedulerProperties.get('yarn.scheduler.capacity.root.queues')

    if yarn_queues:
      toProcessQueues = yarn_queues.split(",")
      while len(toProcessQueues) > 0:
        queue = toProcessQueues.pop()
        queueKey = "yarn.scheduler.capacity.root." + queue + ".queues"
        if queueKey in capacitySchedulerProperties:
          # If parent queue, add children
          subQueues = capacitySchedulerProperties[queueKey].split(",")
          for subQueue in subQueues:
            toProcessQueues.append(queue + "." + subQueue)
        else:
          # Leaf queues
          # We only take the leaf queue name instead of the complete path, as leaf queue names are unique in YARN.
          # Eg: If YARN queues are like :
          #     (1). 'yarn.scheduler.capacity.root.a1.b1.c1.d1',
          #     (2). 'yarn.scheduler.capacity.root.a1.b1.c2',
          #     (3). 'yarn.scheduler.capacity.root.default,
          # Added leaf queues names are as : d1, c2 and default for the 3 leaf queues.
          leafQueuePathSplits = queue.split(".")
          if leafQueuePathSplits > 0:
            leafQueueName = leafQueuePathSplits[-1]
            leafQueueNames.add(leafQueueName)
    return leafQueueNames
  #endregion

  @classmethod
  def getMountPointForDir(cls, dir, mountPoints):
    """
    :param dir: Directory to check, even if it doesn't exist.
    :return: Returns the closest mount point as a string for the directory.
    if the "dir" variable is None, will return None.
    If the directory does not exist, will return "/".
    """
    bestMountFound = None
    if dir:
      dir = re.sub("^file://", "", dir, count=1).strip().lower()

      # If the path is "/hadoop/hdfs/data", then possible matches for mounts could be
      # "/", "/hadoop/hdfs", and "/hadoop/hdfs/data".
      # So take the one with the greatest number of segments.
      for mountPoint in mountPoints:
        # Ensure that the mount path and the dir path ends with "/"
        # The mount point "/hadoop" should not match with the path "/hadoop1"
        if os.path.join(dir, "").startswith(os.path.join(mountPoint, "")):
          if bestMountFound is None:
            bestMountFound = mountPoint
          elif os.path.join(bestMountFound, "").count(os.path.sep) < os.path.join(mountPoint, "").count(os.path.sep):
            bestMountFound = mountPoint

    return bestMountFound

  def validateMinMemorySetting(self, properties, defaultValue, propertyName):
    if not propertyName in properties:
      return self.getErrorItem("Value should be set")
    if defaultValue is None:
      return self.getErrorItem("Config's default value can't be null or undefined")

    value = properties[propertyName]
    if value is None:
      return self.getErrorItem("Value can't be null or undefined")
    try:
      valueInt = self.to_number(value)
      # TODO: generify for other use cases
      defaultValueInt = int(str(defaultValue).strip())
      if valueInt < defaultValueInt:
        return self.getWarnItem("Value is less than the minimum recommended default of -Xmx" + str(defaultValue))
    except:
      return None

    return None

  @classmethod
  def checkXmxValueFormat(cls, value):
    p = re.compile('-Xmx(\d+)(b|k|m|g|p|t|B|K|M|G|P|T)?')
    matches = p.findall(value)
    return len(matches) == 1

  @classmethod
  def getXmxSize(cls, value):
    p = re.compile("-Xmx(\d+)(.?)")
    result = p.findall(value)[0]
    if len(result) > 1:
      # result[1] - is a space or size formatter (b|k|m|g etc)
      return result[0] + result[1].lower()
    return result[0]

  @classmethod
  def formatXmxSizeToBytes(cls, value):
    value = value.lower()
    if len(value) == 0:
      return 0
    modifier = value[-1]

    if modifier == ' ' or modifier in "0123456789":
      modifier = 'b'
    m = {
      modifier == 'b': 1,
      modifier == 'k': 1024,
      modifier == 'm': 1024 * 1024,
      modifier == 'g': 1024 * 1024 * 1024,
      modifier == 't': 1024 * 1024 * 1024 * 1024,
      modifier == 'p': 1024 * 1024 * 1024 * 1024 * 1024
    }[1]
    return cls.to_number(value) * m

  @classmethod
  def to_number(cls, s):
    try:
      return int(re.sub("\D", "", s))
    except ValueError:
      return None
