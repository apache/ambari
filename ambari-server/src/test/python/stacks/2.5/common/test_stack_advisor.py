'''
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
'''

import json
import os
import socket
from unittest import TestCase
from mock.mock import patch, MagicMock

class TestHDP25StackAdvisor(TestCase):

  def setUp(self):
    import imp
    self.maxDiff = None
    self.testDirectory = os.path.dirname(os.path.abspath(__file__))
    stackAdvisorPath = os.path.join(self.testDirectory, '../../../../../main/resources/stacks/stack_advisor.py')
    hdp206StackAdvisorPath = os.path.join(self.testDirectory, '../../../../../main/resources/stacks/HDP/2.0.6/services/stack_advisor.py')
    hdp21StackAdvisorPath = os.path.join(self.testDirectory, '../../../../../main/resources/stacks/HDP/2.1/services/stack_advisor.py')
    hdp22StackAdvisorPath = os.path.join(self.testDirectory, '../../../../../main/resources/stacks/HDP/2.2/services/stack_advisor.py')
    hdp23StackAdvisorPath = os.path.join(self.testDirectory, '../../../../../main/resources/stacks/HDP/2.3/services/stack_advisor.py')
    hdp24StackAdvisorPath = os.path.join(self.testDirectory, '../../../../../main/resources/stacks/HDP/2.4/services/stack_advisor.py')
    hdp25StackAdvisorPath = os.path.join(self.testDirectory, '../../../../../main/resources/stacks/HDP/2.5/services/stack_advisor.py')
    hdp25StackAdvisorClassName = 'HDP25StackAdvisor'

    with open(stackAdvisorPath, 'rb') as fp:
      imp.load_module('stack_advisor', fp, stackAdvisorPath, ('.py', 'rb', imp.PY_SOURCE))
    with open(hdp206StackAdvisorPath, 'rb') as fp:
      imp.load_module('stack_advisor_impl', fp, hdp206StackAdvisorPath, ('.py', 'rb', imp.PY_SOURCE))
    with open(hdp21StackAdvisorPath, 'rb') as fp:
      imp.load_module('stack_advisor_impl', fp, hdp21StackAdvisorPath, ('.py', 'rb', imp.PY_SOURCE))
    with open(hdp22StackAdvisorPath, 'rb') as fp:
      imp.load_module('stack_advisor_impl', fp, hdp22StackAdvisorPath, ('.py', 'rb', imp.PY_SOURCE))
    with open(hdp23StackAdvisorPath, 'rb') as fp:
      imp.load_module('stack_advisor_impl', fp, hdp23StackAdvisorPath, ('.py', 'rb', imp.PY_SOURCE))
    with open(hdp24StackAdvisorPath, 'rb') as fp:
      imp.load_module('stack_advisor_impl', fp, hdp24StackAdvisorPath, ('.py', 'rb', imp.PY_SOURCE))
    with open(hdp25StackAdvisorPath, 'rb') as fp:
      stack_advisor_impl = imp.load_module('stack_advisor_impl', fp, hdp25StackAdvisorPath, ('.py', 'rb', imp.PY_SOURCE))
    clazz = getattr(stack_advisor_impl, hdp25StackAdvisorClassName)
    self.stackAdvisor = clazz()

    # substitute method in the instance
    self.get_system_min_uid_real = self.stackAdvisor.get_system_min_uid
    self.stackAdvisor.get_system_min_uid = self.get_system_min_uid_magic

  def load_json(self, filename):
    file = os.path.join(self.testDirectory, filename)
    with open(file, 'rb') as f:
      data = json.load(f)
    return data

  def prepareHosts(self, hostsNames):
    hosts = { "items": [] }
    for hostName in hostsNames:
      nextHost = {"Hosts":{"host_name" : hostName}}
      hosts["items"].append(nextHost)
    return hosts

  @patch('__builtin__.open')
  @patch('os.path.exists')
  def get_system_min_uid_magic(self, exists_mock, open_mock):
    class MagicFile(object):
      def read(self):
        return """
        #test line UID_MIN 200
        UID_MIN 500
        """

      def __exit__(self, exc_type, exc_val, exc_tb):
        pass

      def __enter__(self):
        return self

    exists_mock.return_value = True
    open_mock.return_value = MagicFile()
    return self.get_system_min_uid_real()


  def __getHosts(self, componentsList, componentName):
    return [component["StackServiceComponents"] for component in componentsList if component["StackServiceComponents"]["component_name"] == componentName][0]


  def test_getComponentLayoutValidations_one_hsi_host(self):

    hosts = self.load_json("host-3-hosts.json")
    services = self.load_json("services-normal-his-2-hosts.json")

    validations = self.stackAdvisor.getComponentLayoutValidations(services, hosts)
    expected = {'component-name': 'HIVE_SERVER_INTERACTIVE', 'message': 'Between 0 and 1 HiveServer2 Interactive components should be installed in cluster.', 'type': 'host-component', 'level': 'ERROR'}
    self.assertEquals(validations[0], expected)


  def test_validateYarnConfigurations(self):
    properties = {'enable_hive_interactive': 'true',
                  'hive_server_interactive_host': 'c6401.ambari.apache.org',
                  'hive.tez.container.size': '2048'}
    recommendedDefaults = {'enable_hive_interactive': 'true',
                           "hive_server_interactive_host": "c6401.ambari.apache.org"}
    configurations = {
      "hive-interactive-env": {
        "properties": {'enable_hive_interactive': 'true', "hive_server_interactive_host": "c6401.ambari.apache.org"}
      },
      "hive-site": {
        "properties": {"hive.security.authorization.enabled": "true", 'hive.tez.java.opts': '-server -Djava.net.preferIPv4Stack=true'}
      },
      "hive-env": {
        "properties": {"hive_security_authorization": "None"}
      },
      "yarn-site": {
        "properties": {"yarn.resourcemanager.work-preserving-recovery.enabled": "false"}
      }
    }
    services = self.load_json("services-normal-his-valid.json")

    res_expected = [
      {'config-type': 'yarn-site', 'message': 'While enabling HIVE_SERVER_INTERACTIVE it is recommended that you enable work preserving restart in YARN.', 'type': 'configuration', 'config-name': 'yarn.resourcemanager.work-preserving-recovery.enabled', 'level': 'WARN'}
    ]
    res = self.stackAdvisor.validateYarnConfigurations(properties, recommendedDefaults, configurations, services, {})
    self.assertEquals(res, res_expected)
    pass

  def test_validateHiveConfigurations(self):
    properties = {'enable_hive_interactive': 'true',
                  'hive_server_interactive_host': 'c6401.ambari.apache.org',
                  'hive.tez.container.size': '2048'}
    recommendedDefaults = {'enable_hive_interactive': 'true',
                           "hive_server_interactive_host": "c6401.ambari.apache.org"}
    configurations = {
      "hive-interactive-env": {
        "properties": {'enable_hive_interactive': 'true', 'hive_server_interactive_host': 'c6401.ambari.apache.org'}
      },
      "hive-site": {
        "properties": {"hive.security.authorization.enabled": "true", 'hive.tez.java.opts': '-server -Djava.net.preferIPv4Stack=true'}
      },
      "hive-env": {
        "properties": {"hive_security_authorization": "None"}
      },
      "yarn-site": {
        "properties": {"yarn.resourcemanager.work-preserving-recovery.enabled": "true"}
      }
    }
    configurations2 = {
      "hive-interactive-env": {
        "properties": {'enable_hive_interactive': 'false'}
      },
      "hive-site": {
        "properties": {"hive.security.authorization.enabled": "true", 'hive.tez.java.opts': '-server -Djava.net.preferIPv4Stack=true'}
      },
      "hive-env": {
        "properties": {"hive_security_authorization": "None"}
      },
      "yarn-site": {
        "properties": {"yarn.resourcemanager.work-preserving-recovery.enabled": "true"}
      }
    }
    configurations3 = {
      "hive-interactive-env": {
        "properties": {'enable_hive_interactive': 'true', "hive_server_interactive_host": "c6402.ambari.apache.org"}
      },
      "hive-site": {
        "properties": {"hive.security.authorization.enabled": "true", 'hive.tez.java.opts': '-server -Djava.net.preferIPv4Stack=true'}
      },
      "hive-env": {
        "properties": {"hive_security_authorization": "None"}
      },
      "yarn-site": {
        "properties": {"yarn.resourcemanager.work-preserving-recovery.enabled": "true"}
      }
    }
    services = self.load_json("services-normal-his-valid.json")

    res_expected = [
    ]
    # the above error is not what we are checking for - just to keep test happy without having to test
    res = self.stackAdvisor.validateHiveInteractiveEnvConfigurations(properties, recommendedDefaults, configurations, services, {})
    self.assertEquals(res, res_expected)

    res_expected = [
      {'config-type': 'hive-interactive-env', 'message': 'HIVE_SERVER_INTERACTIVE requires enable_hive_interactive in hive-interactive-env set to true.', 'type': 'configuration', 'config-name': 'enable_hive_interactive', 'level': 'ERROR'},
      {'config-type': 'hive-interactive-env', 'message': 'HIVE_SERVER_INTERACTIVE requires hive_server_interactive_host in hive-interactive-env set to its host name.', 'type': 'configuration', 'config-name': 'hive_server_interactive_host', 'level': 'ERROR'}
    ]
    res = self.stackAdvisor.validateHiveInteractiveEnvConfigurations(properties, recommendedDefaults, configurations2, services, {})
    self.assertEquals(res, res_expected)

    res_expected = [
      {'config-type': 'hive-interactive-env', 'message': 'HIVE_SERVER_INTERACTIVE requires hive_server_interactive_host in hive-interactive-env set to its host name.', 'type': 'configuration', 'config-name': 'hive_server_interactive_host', 'level': 'ERROR'}
    ]
    res = self.stackAdvisor.validateHiveInteractiveEnvConfigurations(properties, recommendedDefaults, configurations3, services, {})
    self.assertEquals(res, res_expected)
    pass



  def test_recommendYARNConfigurations(self):
    ################ Setting up Inputs. #########################
    hosts = {
      "items": [
        {
          "Hosts": {
            "cpu_count": 6,
            "total_mem": 50331648,
            "disk_info": [
              {"mountpoint": "/"},
              {"mountpoint": "/dev/shm"},
              {"mountpoint": "/vagrant"},
              {"mountpoint": "/"},
              {"mountpoint": "/dev/shm"},
              {"mountpoint": "/vagrant"}
            ],
            "public_host_name": "c6401.ambari.apache.org",
            "host_name": "c6401.ambari.apache.org"
          }
        }
      ]
    }

    clusterData = {
      "cpu": 4,
      "mapMemory": 3000,
      "amMemory": 2000,
      "reduceMemory": 2056,
      "containers": 3,
      "ramPerContainer": 256
    }

    # Services 1: YARN service with : (1). 'capacity scheduler' having only 'default' queue,
    # (2). 'enable_hive_interactive' is ON, and (3). 'llap_queue_capacity' input is 0.
    services_1 = {
      "services": [{
        "StackServices": {
          "service_name": "YARN",
        },
        "Versions": {
          "stack_version": "2.5"
        },
        "components": [
          {
            "StackServiceComponents": {
              "component_name": "NODEMANAGER",
              "hostnames": ["c6401.ambari.apache.org"]
            },
          }
        ]
      }, {
          "href": "/api/v1/stacks/HDP/versions/2.5/services/HIVE",
          "StackServices": {
            "service_name": "HIVE",
            "service_version": "1.2.1.2.5",
            "stack_name": "HDP",
            "stack_version": "2.5"
          },
          "components": [
            {
              "href": "/api/v1/stacks/HDP/versions/2.5/services/HIVE/components/HIVE_SERVER_INTERACTIVE",
              "StackServiceComponents": {
                "advertise_version": "true",
                "bulk_commands_display_name": "",
                "bulk_commands_master_component_name": "",
                "cardinality": "0-1",
                "component_category": "MASTER",
                "component_name": "HIVE_SERVER_INTERACTIVE",
                "custom_commands": ["RESTART_LLAP"],
                "decommission_allowed": "false",
                "display_name": "HiveServer2 Interactive",
                "has_bulk_commands_definition": "false",
                "is_client": "false",
                "is_master": "true",
                "reassign_allowed": "false",
                "recovery_enabled": "false",
                "service_name": "HIVE",
                "stack_name": "HDP",
                "stack_version": "2.5",
                "hostnames": ["c6401.ambari.apache.org"]
              },
              "dependencies": []
            }
          ]
        }
      ],
      "changed-configurations": [
        {
          u'old_value': u'',
          u'type': u'',
          u'name': u''
        }
      ],
      "configurations": {
        "capacity-scheduler": {
          "properties": {
            "capacity-scheduler": "yarn.scheduler.capacity.root.queues=default\n"
                                  "yarn.scheduler.capacity.root.default.user-limit-factor=1\n"
                                  "yarn.scheduler.capacity.root.default.state=RUNNING\n"
                                  "yarn.scheduler.capacity.root.default.maximum-capacity=100\n"
                                  "yarn.scheduler.capacity.root.default.capacity=100\n"
                                  "yarn.scheduler.capacity.root.default.acl_submit_applications=*\n"
                                  "yarn.scheduler.capacity.root.capacity=100\n"
                                  "yarn.scheduler.capacity.root.acl_administer_queue=*\n"
                                  "yarn.scheduler.capacity.root.accessible-node-labels=*\n"
                                  "yarn.scheduler.capacity.node-locality-delay=40\n"
                                  "yarn.scheduler.capacity.maximum-applications=10000\n"
                                  "yarn.scheduler.capacity.maximum-am-resource-percent=1\n"
                                  "yarn.scheduler.capacity.queue-mappings-override.enable=false\n"
          }
        },
        "hive-interactive-env":
          {
            'properties': {
              'enable_hive_interactive': 'true',
              'llap_queue_capacity':'0'
            }
        },
        "yarn-site": {
          "properties": {
            "yarn.scheduler.minimum-allocation-mb": "682",
            "yarn.nodemanager.resource.memory-mb": "10240"
          }
        },
        "hive-interactive-site":
          {
            'properties': {
              'hive.llap.daemon.queue.name':'default'
            }
          },
        "tez-interactive-site": {
          "properties": {
            "tez.am.resource.memory.mb": "341"
          }
        },
        "hive-env":
          {
            'properties': {
              'hive_user': 'hive'
            }
        },
        "hive-site":
          {
            'properties': {
              'hive.tez.container.size': '341'
            }
        },
      }
    }

    # Services 2: YARN service with : (1). 'capacity scheduler' having only 'default' queue,
    # (2). 'enable_hive_interactive' is OFF, and (3). 'llap_queue_capacity' input is 0.
    services_2 = {
      "services": [{
        "StackServices": {
          "service_name": "YARN",
        },
        "Versions": {
          "stack_version": "2.5"
        },
        "components": [
          {
            "StackServiceComponents": {
              "component_name": "NODEMANAGER",
              "hostnames": ["c6401.ambari.apache.org"]
            }
          }
        ]
      }, {
        "href": "/api/v1/stacks/HDP/versions/2.5/services/HIVE",
        "StackServices": {
          "service_name": "HIVE",
          "service_version": "1.2.1.2.5",
          "stack_name": "HDP",
          "stack_version": "2.5"
        },
        "components": [
          {
            "href": "/api/v1/stacks/HDP/versions/2.5/services/HIVE/components/HIVE_SERVER_INTERACTIVE",
            "StackServiceComponents": {
              "advertise_version": "true",
              "bulk_commands_display_name": "",
              "bulk_commands_master_component_name": "",
              "cardinality": "0-1",
              "component_category": "MASTER",
              "component_name": "HIVE_SERVER_INTERACTIVE",
              "custom_commands": ["RESTART_LLAP"],
              "decommission_allowed": "false",
              "display_name": "HiveServer2 Interactive",
              "has_bulk_commands_definition": "false",
              "is_client": "false",
              "is_master": "true",
              "reassign_allowed": "false",
              "recovery_enabled": "false",
              "service_name": "HIVE",
              "stack_name": "HDP",
              "stack_version": "2.5",
              "hostnames": ["c6401.ambari.apache.org"]
            },
            "dependencies": []
          }
        ]
      }
      ],
      "changed-configurations": [
        {
          u'old_value': u'',
          u'type': u'',
          u'name': u''
        }
      ],
      "configurations": {
        "capacity-scheduler": {
          "properties": {
            "capacity-scheduler": "yarn.scheduler.capacity.root.queues=default\n"
                                  "yarn.scheduler.capacity.root.default.user-limit-factor=1\n"
                                  "yarn.scheduler.capacity.root.default.state=RUNNING\n"
                                  "yarn.scheduler.capacity.root.default.maximum-capacity=100\n"
                                  "yarn.scheduler.capacity.root.default.capacity=100\n"
                                  "yarn.scheduler.capacity.root.default.acl_submit_applications=*\n"
                                  "yarn.scheduler.capacity.root.capacity=100\n"
                                  "yarn.scheduler.capacity.root.acl_administer_queue=*\n"
                                  "yarn.scheduler.capacity.root.accessible-node-labels=*\n"
                                  "yarn.scheduler.capacity.node-locality-delay=40\n"
                                  "yarn.scheduler.capacity.maximum-applications=10000\n"
                                  "yarn.scheduler.capacity.maximum-am-resource-percent=1\n"
                                  "yarn.scheduler.capacity.queue-mappings-override.enable=false\n"
          }
        },
        "hive-interactive-env":
          {
            'properties': {
              'enable_hive_interactive': 'false',
              'llap_queue_capacity':'0'
            }
          },
        "yarn-site": {
          "properties": {
            "yarn.scheduler.minimum-allocation-mb": "682",
            "yarn.nodemanager.resource.memory-mb": "2048"
          }
        },
        "tez-interactive-site": {
          "properties": {
            "tez.am.resource.memory.mb": "341"
          }
        },
        "hive-env":
          {
            'properties': {
              'hive_user': 'hive'
            }
          }
      }
    }

    # Services 3: YARN service with : (1). 'capacity scheduler' having only 'default' queue,
    # (2). 'enable_hive_interactive' is ON, and (3). 'llap_queue_capacity' input is 30.
    services_3 = {
      "services": [{
        "StackServices": {
          "service_name": "YARN",
        },
        "Versions": {
          "stack_version": "2.5"
        },
        "components": [
          {
            "StackServiceComponents": {
              "component_name": "NODEMANAGER",
              "hostnames": ["c6401.ambari.apache.org"]
            }
          }
        ]
      }, {
        "href": "/api/v1/stacks/HDP/versions/2.5/services/HIVE",
        "StackServices": {
          "service_name": "HIVE",
          "service_version": "1.2.1.2.5",
          "stack_name": "HDP",
          "stack_version": "2.5"
        },
        "components": [
          {
            "href": "/api/v1/stacks/HDP/versions/2.5/services/HIVE/components/HIVE_SERVER_INTERACTIVE",
            "StackServiceComponents": {
              "advertise_version": "true",
              "bulk_commands_display_name": "",
              "bulk_commands_master_component_name": "",
              "cardinality": "0-1",
              "component_category": "MASTER",
              "component_name": "HIVE_SERVER_INTERACTIVE",
              "custom_commands": ["RESTART_LLAP"],
              "decommission_allowed": "false",
              "display_name": "HiveServer2 Interactive",
              "has_bulk_commands_definition": "false",
              "is_client": "false",
              "is_master": "true",
              "reassign_allowed": "false",
              "recovery_enabled": "false",
              "service_name": "HIVE",
              "stack_name": "HDP",
              "stack_version": "2.5",
              "hostnames": ["c6401.ambari.apache.org"]
            },
            "dependencies": []
          }
        ]
      }
      ],
      "changed-configurations": [
        {
          u'old_value': u'',
          u'type': u'',
          u'name': u''
        }
      ],
      "configurations": {
        "capacity-scheduler": {
          "properties": {
            "capacity-scheduler": "yarn.scheduler.capacity.root.queues=default\n"
                                  "yarn.scheduler.capacity.root.default.user-limit-factor=1\n"
                                  "yarn.scheduler.capacity.root.default.state=RUNNING\n"
                                  "yarn.scheduler.capacity.root.default.maximum-capacity=100\n"
                                  "yarn.scheduler.capacity.root.default.capacity=100\n"
                                  "yarn.scheduler.capacity.root.default.acl_submit_applications=*\n"
                                  "yarn.scheduler.capacity.root.capacity=100\n"
                                  "yarn.scheduler.capacity.root.acl_administer_queue=*\n"
                                  "yarn.scheduler.capacity.root.accessible-node-labels=*\n"
                                  "yarn.scheduler.capacity.node-locality-delay=40\n"
                                  "yarn.scheduler.capacity.maximum-applications=10000\n"
                                  "yarn.scheduler.capacity.maximum-am-resource-percent=1\n"
                                  "yarn.scheduler.capacity.queue-mappings-override.enable=false\n"
          }
        },
        "hive-interactive-env":
          {
            'properties': {
              'enable_hive_interactive': 'true',
              'llap_queue_capacity':'40'
            }
          },
        "yarn-site": {
          "properties": {
            "yarn.scheduler.minimum-allocation-mb": "682",
            "yarn.nodemanager.resource.memory-mb": "2048"
          }
        },
        "hive-interactive-site":
          {
            'properties': {
              'hive.llap.daemon.queue.name':'default',
            }
          },
        "tez-interactive-site": {
          "properties": {
            "tez.am.resource.memory.mb": "341"
          }
        },
        "hive-env":
          {
            'properties': {
              'hive_user': 'hive'
            }
          },
        "hive-site":
          {
            'properties': {
              'hive.tez.container.size': '341'
            }
          },
      }
    }

    # Services 4: YARN service with : (1). 'capacity scheduler' having 'llap' and 'default' queue at root level and
    # (2). 'enable_hive_interactive' is OFF
    services_4 = {
      "services": [{
        "StackServices": {
          "service_name": "YARN",
        },
        "Versions": {
          "stack_version": "2.5"
        },
        "components": [
          {
            "StackServiceComponents": {
              "component_name": "NODEMANAGER",
              "hostnames": ["c6401.ambari.apache.org"]
            }
          }
        ]
      }, {
        "href": "/api/v1/stacks/HDP/versions/2.5/services/HIVE",
        "StackServices": {
          "service_name": "HIVE",
          "service_version": "1.2.1.2.5",
          "stack_name": "HDP",
          "stack_version": "2.5"
        },
        "components": [
          {
            "href": "/api/v1/stacks/HDP/versions/2.5/services/HIVE/components/HIVE_SERVER_INTERACTIVE",
            "StackServiceComponents": {
              "advertise_version": "true",
              "bulk_commands_display_name": "",
              "bulk_commands_master_component_name": "",
              "cardinality": "0-1",
              "component_category": "MASTER",
              "component_name": "HIVE_SERVER_INTERACTIVE",
              "custom_commands": ["RESTART_LLAP"],
              "decommission_allowed": "false",
              "display_name": "HiveServer2 Interactive",
              "has_bulk_commands_definition": "false",
              "is_client": "false",
              "is_master": "true",
              "reassign_allowed": "false",
              "recovery_enabled": "false",
              "service_name": "HIVE",
              "stack_name": "HDP",
              "stack_version": "2.5",
              "hostnames": ["c6401.ambari.apache.org"]
            },
            "dependencies": []
          }
        ]
      }
      ],
      "changed-configurations": [
        {
          u'old_value': u'',
          u'type': u'',
          u'name': u''
        }
      ],
      "configurations": {
        "capacity-scheduler": {
          "properties": {
            "capacity-scheduler": "yarn.scheduler.capacity.root.accessible-node-labels=*\n"
                                  "yarn.scheduler.capacity.root.capacity=100\n"
                                  "yarn.scheduler.capacity.root.queues=default,llap\n"
                                  "yarn.scheduler.capacity.maximum-applications=10000\n"
                                  "yarn.scheduler.capacity.root.default.user-limit-factor=1\n"
                                  "yarn.scheduler.capacity.root.default.state=RUNNING\n"
                                  "yarn.scheduler.capacity.maximum-am-resource-percent=1\n"
                                  "yarn.scheduler.capacity.root.default.acl_submit_applications=*\n"
                                  "yarn.scheduler.capacity.root.default.capacity=80\n"
                                  "yarn.scheduler.capacity.root.acl_administer_queue=*\n"
                                  "yarn.scheduler.capacity.node-locality-delay=40\n"
                                  "yarn.scheduler.capacity.queue-mappings-override.enable=false\n"
                                  "yarn.scheduler.capacity.root.llap.user-limit-factor=1\n"
                                  "yarn.scheduler.capacity.root.llap.state=RUNNING\n"
                                  "yarn.scheduler.capacity.root.llap.ordering-policy=fifo\n"
                                  "yarn.scheduler.capacity.root.llap.minimum-user-limit-percent=100\n"
                                  "yarn.scheduler.capacity.root.llap.maximum-capacity=20\n"
                                  "yarn.scheduler.capacity.root.default.maximum-capacity=100\n"
                                  "yarn.scheduler.capacity.root.llap.capacity=20\n"
                                  "yarn.scheduler.capacity.root.llap.acl_submit_applications=hive\n"
                                  "yarn.scheduler.capacity.root.llap.acl_administer_queue=hive\n"
                                  "yarn.scheduler.capacity.root.llap.maximum-am-resource-percent=1\n"
          }
        },
        "hive-interactive-env":
          {
            'properties': {
              'enable_hive_interactive': 'false'
            }
          },
        "hive-interactive-site":
          {
            'properties': {
              'hive.llap.daemon.queue.name':'default'
            }
          },
        "yarn-site": {
          "properties": {
            "yarn.scheduler.minimum-allocation-mb": "682",
            "yarn.nodemanager.resource.memory-mb": "2048"
          },
        "tez-interactive-site": {
            "properties": {
              "tez.am.resource.memory.mb": "341"
            }
          },
        },
        "hive-env":
          {
            'properties': {
              'hive_user': 'hive'
            }
          }
      }
    }

    # Services 5: YARN service with : (1). 'capacity scheduler' having 'llap' and 'default' queue at root level and
    # (2). 'enable_hive_interactive' is ON
    services_5 = {
      "services": [{
        "StackServices": {
          "service_name": "YARN",
        },
        "Versions": {
          "stack_version": "2.5"
        },
        "components": [
          {
            "StackServiceComponents": {
              "component_name": "NODEMANAGER",
              "hostnames": ["c6401.ambari.apache.org"]
            }
          }
        ]
      }, {
        "href": "/api/v1/stacks/HDP/versions/2.5/services/HIVE",
        "StackServices": {
          "service_name": "HIVE",
          "service_version": "1.2.1.2.5",
          "stack_name": "HDP",
          "stack_version": "2.5"
        },
        "components": [
          {
            "href": "/api/v1/stacks/HDP/versions/2.5/services/HIVE/components/HIVE_SERVER_INTERACTIVE",
            "StackServiceComponents": {
              "advertise_version": "true",
              "bulk_commands_display_name": "",
              "bulk_commands_master_component_name": "",
              "cardinality": "0-1",
              "component_category": "MASTER",
              "component_name": "HIVE_SERVER_INTERACTIVE",
              "custom_commands": ["RESTART_LLAP"],
              "decommission_allowed": "false",
              "display_name": "HiveServer2 Interactive",
              "has_bulk_commands_definition": "false",
              "is_client": "false",
              "is_master": "true",
              "reassign_allowed": "false",
              "recovery_enabled": "false",
              "service_name": "HIVE",
              "stack_name": "HDP",
              "stack_version": "2.5",
              "hostnames": ["c6401.ambari.apache.org"]
            },
            "dependencies": []
          }
        ]
      }
      ],
      "changed-configurations": [
        {
          u'old_value': u'0',
          u'type': u'hive-interactive-env',
          u'name': u'llap_queue_capacity'
        }
      ],
      "configurations": {
        "capacity-scheduler": {
          "properties": {
            "capacity-scheduler": 'yarn.scheduler.capacity.root.default.maximum-capacity=60\n'
                                  'yarn.scheduler.capacity.root.accessible-node-labels=*\n'
                                  'yarn.scheduler.capacity.root.capacity=100\n'
                                  'yarn.scheduler.capacity.root.queues=default,llap\n'
                                  'yarn.scheduler.capacity.maximum-applications=10000\n'
                                  'yarn.scheduler.capacity.root.default.user-limit-factor=1\n'
                                  'yarn.scheduler.capacity.root.default.state=RUNNING\n'
                                  'yarn.scheduler.capacity.maximum-am-resource-percent=1\n'
                                  'yarn.scheduler.capacity.root.default.acl_submit_applications=*\n'
                                  'yarn.scheduler.capacity.root.default.capacity=60\n'
                                  'yarn.scheduler.capacity.root.acl_administer_queue=*\n'
                                  'yarn.scheduler.capacity.node-locality-delay=40\n'
                                  'yarn.scheduler.capacity.queue-mappings-override.enable=false\n'
                                  'yarn.scheduler.capacity.root.llap.user-limit-factor=1\n'
                                  'yarn.scheduler.capacity.root.llap.state=RUNNING\n'
                                  'yarn.scheduler.capacity.root.llap.ordering-policy=fifo\n'
                                  'yarn.scheduler.capacity.root.llap.minimum-user-limit-percent=100\n'
                                  'yarn.scheduler.capacity.root.llap.maximum-capacity=40\n'
                                  'yarn.scheduler.capacity.root.llap.capacity=40\n'
                                  'yarn.scheduler.capacity.root.llap.acl_submit_applications=hive\n'
                                  'yarn.scheduler.capacity.root.llap.acl_administer_queue=hive\n'
                                  'yarn.scheduler.capacity.root.llap.maximum-am-resource-percent=1'

          }
        },
        "hive-interactive-env":
          {
            'properties': {
              'enable_hive_interactive': 'true',
              'llap_queue_capacity':'0'
            }
          },
        "hive-interactive-site":
          {
            'properties': {
              'hive.llap.daemon.queue.name': 'llap'
            }
          },
        "tez-interactive-site": {
          "properties": {
            "tez.am.resource.memory.mb": "341"
          }
        },
        "hive-env":
          {
            'properties': {
              'hive_user': 'hive'
            }
          },
        "yarn-site": {
          "properties": {
            "yarn.scheduler.minimum-allocation-mb": "682",
            "yarn.nodemanager.resource.memory-mb": "2048"
          }
        }
      }
    }

    # Services 6: YARN service with : (1). 'capacity scheduler' having more than 2 queues and
    # (2). 'enable_hive_interactive' is OFF
    services_6= {
      "services": [{
        "StackServices": {
          "service_name": "YARN",
        },
        "Versions": {
          "stack_version": "2.5"
        },
        "components": [
          {
            "StackServiceComponents": {
              "component_name": "NODEMANAGER",
              "hostnames": ["c6401.ambari.apache.org"]
            }
          }
        ]
      }, {
        "href": "/api/v1/stacks/HDP/versions/2.5/services/HIVE",
        "StackServices": {
          "service_name": "HIVE",
          "service_version": "1.2.1.2.5",
          "stack_name": "HDP",
          "stack_version": "2.5"
        },
        "components": [
          {
            "href": "/api/v1/stacks/HDP/versions/2.5/services/HIVE/components/HIVE_SERVER_INTERACTIVE",
            "StackServiceComponents": {
              "advertise_version": "true",
              "bulk_commands_display_name": "",
              "bulk_commands_master_component_name": "",
              "cardinality": "0-1",
              "component_category": "MASTER",
              "component_name": "HIVE_SERVER_INTERACTIVE",
              "custom_commands": ["RESTART_LLAP"],
              "decommission_allowed": "false",
              "display_name": "HiveServer2 Interactive",
              "has_bulk_commands_definition": "false",
              "is_client": "false",
              "is_master": "true",
              "reassign_allowed": "false",
              "recovery_enabled": "false",
              "service_name": "HIVE",
              "stack_name": "HDP",
              "stack_version": "2.5",
              "hostnames": ["c6401.ambari.apache.org"]
            },
            "dependencies": []
          }
        ]
      }
      ],
      "changed-configurations": [
        {
          u'old_value': u'',
          u'type': u'',
          u'name': u''
        }
      ],
      "configurations": {
        "capacity-scheduler": {
          "properties": {
            "capacity-scheduler": "yarn.scheduler.capacity.maximum-am-resource-percent=0.2\n"
                                  "yarn.scheduler.capacity.maximum-applications=10000\n"
                                  "yarn.scheduler.capacity.node-locality-delay=40\n"
                                  "yarn.scheduler.capacity.queue-mappings-override.enable=false\n"
                                  "yarn.scheduler.capacity.resource-calculator=org.apache.hadoop.yarn.util.resource.DefaultResourceCalculator\n"
                                  "yarn.scheduler.capacity.root.accessible-node-labels=*\n"
                                  "yarn.scheduler.capacity.root.acl_administer_queue=*\n"
                                  "yarn.scheduler.capacity.root.capacity=100\n"
                                  "yarn.scheduler.capacity.root.default.a.a1.acl_administer_queue=*\n"
                                  "yarn.scheduler.capacity.root.default.a.a1.acl_submit_applications=*\n"
                                  "yarn.scheduler.capacity.root.default.a.a1.capacity=75\n"
                                  "yarn.scheduler.capacity.root.default.a.a1.maximum-capacity=100\n"
                                  "yarn.scheduler.capacity.root.default.a.a1.minimum-user-limit-percent=100\n"
                                  "yarn.scheduler.capacity.root.default.a.a1.ordering-policy=fifo\n"
                                  "yarn.scheduler.capacity.root.default.a.a1.state=RUNNING\n"
                                  "yarn.scheduler.capacity.root.default.a.a1.user-limit-factor=1\n"
                                  "yarn.scheduler.capacity.root.default.a.a2.acl_administer_queue=*\n"
                                  "yarn.scheduler.capacity.root.default.a.a2.acl_submit_applications=*\n"
                                  "yarn.scheduler.capacity.root.default.a.a2.capacity=25\n"
                                  "yarn.scheduler.capacity.root.default.a.a2.maximum-capacity=25\n"
                                  "yarn.scheduler.capacity.root.default.a.a2.minimum-user-limit-percent=100\n"
                                  "yarn.scheduler.capacity.root.default.a.a2.ordering-policy=fifo\n"
                                  "yarn.scheduler.capacity.root.default.a.a2.state=RUNNING\n"
                                  "yarn.scheduler.capacity.root.default.a.a2.user-limit-factor=1\n"
                                  "yarn.scheduler.capacity.root.default.a.acl_administer_queue=*\n"
                                  "yarn.scheduler.capacity.root.default.a.acl_submit_applications=*\n"
                                  "yarn.scheduler.capacity.root.default.a.capacity=50\n"
                                  "yarn.scheduler.capacity.root.default.a.maximum-capacity=100\n"
                                  "yarn.scheduler.capacity.root.default.a.minimum-user-limit-percent=100\n"
                                  "yarn.scheduler.capacity.root.default.a.ordering-policy=fifo\n"
                                  "yarn.scheduler.capacity.root.default.a.queues=a1,a2\n"
                                  "yarn.scheduler.capacity.root.default.a.state=RUNNING\n"
                                  "yarn.scheduler.capacity.root.default.a.user-limit-factor=1\n"
                                  "yarn.scheduler.capacity.root.default.acl_submit_applications=*\n"
                                  "yarn.scheduler.capacity.root.default.b.acl_administer_queue=*\n"
                                  "yarn.scheduler.capacity.root.default.b.acl_submit_applications=*\n"
                                  "yarn.scheduler.capacity.root.default.b.capacity=50\n"
                                  "yarn.scheduler.capacity.root.default.b.maximum-capacity=50\n"
                                  "yarn.scheduler.capacity.root.default.b.minimum-user-limit-percent=100\n"
                                  "yarn.scheduler.capacity.root.default.b.ordering-policy=fifo\n"
                                  "yarn.scheduler.capacity.root.default.b.state=RUNNING\n"
                                  "yarn.scheduler.capacity.root.default.b.user-limit-factor=1\n"
                                  "yarn.scheduler.capacity.root.default.capacity=100\n"
                                  "yarn.scheduler.capacity.root.default.maximum-capacity=100\n"
                                  "yarn.scheduler.capacity.root.default.queues=a,b\n"
                                  "yarn.scheduler.capacity.root.default.state=RUNNING\n"
                                  "yarn.scheduler.capacity.root.default.user-limit-factor=1\n"
                                  "yarn.scheduler.capacity.root.queues=default"
          }
        },
        "hive-interactive-env":
          {
            'properties': {
              'enable_hive_interactive': 'false',
              'llap_queue_capacity':'0'
            }
          },
        "tez-interactive-site": {
          "properties": {
            "tez.am.resource.memory.mb": "341"
          }
        },
        "hive-env":
          {
            'properties': {
              'hive_user': 'hive'
            }
          }
      }
    }

    # Services 7: YARN service with 'capacity scheduler' empty and (2). 'enable_hive_interactive' is OFF
    services_7= {
      "services": [{
        "StackServices": {
          "service_name": "YARN",
        },
        "Versions": {
          "stack_version": "2.5"
        },
        "components": [
          {
            "StackServiceComponents": {
              "component_name": "NODEMANAGER",
              "hostnames": ["c6401.ambari.apache.org"]
            }
          }
        ]
      }, {
        "href": "/api/v1/stacks/HDP/versions/2.5/services/HIVE",
        "StackServices": {
          "service_name": "HIVE",
          "service_version": "1.2.1.2.5",
          "stack_name": "HDP",
          "stack_version": "2.5"
        },
        "components": [
          {
            "href": "/api/v1/stacks/HDP/versions/2.5/services/HIVE/components/HIVE_SERVER_INTERACTIVE",
            "StackServiceComponents": {
              "advertise_version": "true",
              "bulk_commands_display_name": "",
              "bulk_commands_master_component_name": "",
              "cardinality": "0-1",
              "component_category": "MASTER",
              "component_name": "HIVE_SERVER_INTERACTIVE",
              "custom_commands": ["RESTART_LLAP"],
              "decommission_allowed": "false",
              "display_name": "HiveServer2 Interactive",
              "has_bulk_commands_definition": "false",
              "is_client": "false",
              "is_master": "true",
              "reassign_allowed": "false",
              "recovery_enabled": "false",
              "service_name": "HIVE",
              "stack_name": "HDP",
              "stack_version": "2.5",
              "hostnames": ["c6401.ambari.apache.org"]
            },
            "dependencies": []
          }
        ]
      }
      ],
      "changed-configurations": [
        {
          u'old_value': u'',
          u'type': u'',
          u'name': u''
        }
      ],
      "configurations": {
        "capacity-scheduler": {
          "properties": {
          }
        },
        "hive-interactive-env":
          {
            'properties': {
              'enable_hive_interactive': 'false',
              'llap_queue_capacity':'0'
            }
          },
        "hive-env":
          {
            'properties': {
              'hive_user': 'hive'
            }
          }
      }
    }

    # Services 8: YARN service with : (1). malformed 'capacity scheduler' and (2). 'enable_hive_interactive' is OFF
    services_8= {
      "services": [{
        "StackServices": {
          "service_name": "YARN",
        },
        "Versions": {
          "stack_version": "2.5"
        },
        "components": [
          {
            "StackServiceComponents": {
              "component_name": "NODEMANAGER",
              "hostnames": ["c6401.ambari.apache.org"]
            }
          }
        ]
      }, {
        "href": "/api/v1/stacks/HDP/versions/2.5/services/HIVE",
        "StackServices": {
          "service_name": "HIVE",
          "service_version": "1.2.1.2.5",
          "stack_name": "HDP",
          "stack_version": "2.5"
        },
        "components": [
          {
            "href": "/api/v1/stacks/HDP/versions/2.5/services/HIVE/components/HIVE_SERVER_INTERACTIVE",
            "StackServiceComponents": {
              "advertise_version": "true",
              "bulk_commands_display_name": "",
              "bulk_commands_master_component_name": "",
              "cardinality": "0-1",
              "component_category": "MASTER",
              "component_name": "HIVE_SERVER_INTERACTIVE",
              "custom_commands": ["RESTART_LLAP"],
              "decommission_allowed": "false",
              "display_name": "HiveServer2 Interactive",
              "has_bulk_commands_definition": "false",
              "is_client": "false",
              "is_master": "true",
              "reassign_allowed": "false",
              "recovery_enabled": "false",
              "service_name": "HIVE",
              "stack_name": "HDP",
              "stack_version": "2.5",
              "hostnames": ["c6401.ambari.apache.org"]
            },
            "dependencies": []
          }
        ]
      }
      ],
      "changed-configurations": [
        {
          u'old_value': u'',
          u'type': u'',
          u'name': u''
        }
      ],
      "configurations": {
        "capacity-scheduler": {
          "properties": {
            "capacity-scheduler": "yarn.scheduler.capacity.root.default.a.a1.acl_submit_applications=*\n"
                                  "yarn.scheduler.capacity.root.default.a.a1.capacity=75\n"
                                  "yarn.scheduler.capacity.root.default.a.a1.maximum-capacity=100\n"
          }
        },
        "hive-interactive-env":
          {
            'properties': {
              'enable_hive_interactive': 'false',
              'llap_queue_capacity':'0'
            }
          },
        "hive-env":
          {
            'properties': {
              'hive_user': 'hive'
            }
          }
      }
    }

    # Services 9: YARN service with : (1). 'capacity scheduler' having 'llap' and 'default' queue at root level and
    # (2). 'llap' queue state is STOPPED and sized 0 % and (3). 'enable_hive_interactive' is OFF
    services_9 = {
      "services": [{
        "StackServices": {
          "service_name": "YARN",
        },
        "Versions": {
          "stack_version": "2.5"
        },
        "components": [
          {
            "StackServiceComponents": {
              "component_name": "NODEMANAGER",
              "hostnames": ["c6401.ambari.apache.org"]
            }
          }
        ]
      }, {
        "href": "/api/v1/stacks/HDP/versions/2.5/services/HIVE",
        "StackServices": {
          "service_name": "HIVE",
          "service_version": "1.2.1.2.5",
          "stack_name": "HDP",
          "stack_version": "2.5"
        },
        "components": [
          {
            "href": "/api/v1/stacks/HDP/versions/2.5/services/HIVE/components/HIVE_SERVER_INTERACTIVE",
            "StackServiceComponents": {
              "advertise_version": "true",
              "bulk_commands_display_name": "",
              "bulk_commands_master_component_name": "",
              "cardinality": "0-1",
              "component_category": "MASTER",
              "component_name": "HIVE_SERVER_INTERACTIVE",
              "custom_commands": ["RESTART_LLAP"],
              "decommission_allowed": "false",
              "display_name": "HiveServer2 Interactive",
              "has_bulk_commands_definition": "false",
              "is_client": "false",
              "is_master": "true",
              "reassign_allowed": "false",
              "recovery_enabled": "false",
              "service_name": "HIVE",
              "stack_name": "HDP",
              "stack_version": "2.5",
              "hostnames": ["c6401.ambari.apache.org"]
            },
            "dependencies": []
          }
        ]
      }
      ],
      "changed-configurations": [
        {
          u'old_value': u'',
          u'type': u'',
          u'name': u''
        }
      ],
      "configurations": {
        "capacity-scheduler": {
          "properties": {
            "capacity-scheduler": "yarn.scheduler.capacity.root.accessible-node-labels=*\n"
                                  "yarn.scheduler.capacity.root.capacity=100\n"
                                  "yarn.scheduler.capacity.root.queues=default,llap\n"
                                  "yarn.scheduler.capacity.maximum-applications=10000\n"
                                  "yarn.scheduler.capacity.root.default.user-limit-factor=1\n"
                                  "yarn.scheduler.capacity.root.default.state=RUNNING\n"
                                  "yarn.scheduler.capacity.maximum-am-resource-percent=1\n"
                                  "yarn.scheduler.capacity.root.default.acl_submit_applications=*\n"
                                  "yarn.scheduler.capacity.root.default.capacity=100\n"
                                  "yarn.scheduler.capacity.root.acl_administer_queue=*\n"
                                  "yarn.scheduler.capacity.node-locality-delay=40\n"
                                  "yarn.scheduler.capacity.queue-mappings-override.enable=false\n"
                                  "yarn.scheduler.capacity.root.llap.user-limit-factor=1\n"
                                  "yarn.scheduler.capacity.root.llap.state=STOPPED\n"
                                  "yarn.scheduler.capacity.root.llap.ordering-policy=fifo\n"
                                  "yarn.scheduler.capacity.root.llap.minimum-user-limit-percent=100\n"
                                  "yarn.scheduler.capacity.root.llap.maximum-capacity=0\n"
                                  "yarn.scheduler.capacity.root.default.maximum-capacity=100\n"
                                  "yarn.scheduler.capacity.root.llap.capacity=0\n"
                                  "yarn.scheduler.capacity.root.llap.acl_submit_applications=hive\n"
                                  "yarn.scheduler.capacity.root.llap.acl_administer_queue=hive\n"
                                  "yarn.scheduler.capacity.root.llap.maximum-am-resource-percent=1\n"
          }
        },
        "hive-interactive-env":
          {
            'properties': {
              'enable_hive_interactive': 'false'
            }
          },
        "hive-env":
          {
            'properties': {
              'hive_user': 'hive'
            }
          }
      }
    }

    # Services 10: YARN service with : (1). 'capacity scheduler' having 'llap' and 'default' queue at root level and
    # (2). 'llap' queue state is STOPPED and sized 0 % and (3). 'enable_hive_interactive' is ON
    services_10 = {
      "services": [{
        "StackServices": {
          "service_name": "YARN",
        },
        "Versions": {
          "stack_version": "2.5"
        },
        "components": [
          {
            "StackServiceComponents": {
              "component_name": "NODEMANAGER",
              "hostnames": ["c6401.ambari.apache.org"]
            }
          }
        ]
      }, {
        "href": "/api/v1/stacks/HDP/versions/2.5/services/HIVE",
        "StackServices": {
          "service_name": "HIVE",
          "service_version": "1.2.1.2.5",
          "stack_name": "HDP",
          "stack_version": "2.5"
        },
        "components": [
          {
            "href": "/api/v1/stacks/HDP/versions/2.5/services/HIVE/components/HIVE_SERVER_INTERACTIVE",
            "StackServiceComponents": {
              "advertise_version": "true",
              "bulk_commands_display_name": "",
              "bulk_commands_master_component_name": "",
              "cardinality": "0-1",
              "component_category": "MASTER",
              "component_name": "HIVE_SERVER_INTERACTIVE",
              "custom_commands": ["RESTART_LLAP"],
              "decommission_allowed": "false",
              "display_name": "HiveServer2 Interactive",
              "has_bulk_commands_definition": "false",
              "is_client": "false",
              "is_master": "true",
              "reassign_allowed": "false",
              "recovery_enabled": "false",
              "service_name": "HIVE",
              "stack_name": "HDP",
              "stack_version": "2.5",
              "hostnames": ["c6401.ambari.apache.org"]
            },
            "dependencies": []
          }
        ]
      }
      ],
      "changed-configurations": [
        {
          u'old_value': u'off',
          u'type': u'hive-interactive-env',
          u'name': u'enable_hive_interactive'
        }
      ],
      "configurations": {
        "capacity-scheduler": {
          "properties": {
            "capacity-scheduler": "yarn.scheduler.capacity.root.accessible-node-labels=*\n"
                                  "yarn.scheduler.capacity.root.capacity=100\n"
                                  "yarn.scheduler.capacity.root.queues=default,llap\n"
                                  "yarn.scheduler.capacity.maximum-applications=10000\n"
                                  "yarn.scheduler.capacity.root.default.user-limit-factor=1\n"
                                  "yarn.scheduler.capacity.root.default.state=RUNNING\n"
                                  "yarn.scheduler.capacity.maximum-am-resource-percent=1\n"
                                  "yarn.scheduler.capacity.root.default.acl_submit_applications=*\n"
                                  "yarn.scheduler.capacity.root.default.capacity=100\n"
                                  "yarn.scheduler.capacity.root.acl_administer_queue=*\n"
                                  "yarn.scheduler.capacity.node-locality-delay=40\n"
                                  "yarn.scheduler.capacity.queue-mappings-override.enable=false\n"
                                  "yarn.scheduler.capacity.root.llap.user-limit-factor=1\n"
                                  "yarn.scheduler.capacity.root.llap.state=STOPPED\n"
                                  "yarn.scheduler.capacity.root.llap.ordering-policy=fifo\n"
                                  "yarn.scheduler.capacity.root.llap.minimum-user-limit-percent=100\n"
                                  "yarn.scheduler.capacity.root.llap.maximum-capacity=0\n"
                                  "yarn.scheduler.capacity.root.default.maximum-capacity=100\n"
                                  "yarn.scheduler.capacity.root.llap.capacity=0\n"
                                  "yarn.scheduler.capacity.root.llap.acl_submit_applications=hive\n"
                                  "yarn.scheduler.capacity.root.llap.acl_administer_queue=hive\n"
                                  "yarn.scheduler.capacity.root.llap.maximum-am-resource-percent=1\n"
          }
        },
        "hive-interactive-env":
          {
            'properties': {
              'enable_hive_interactive': 'true',
              'llap_queue_capacity':'0'
            }
          },
        "yarn-site": {
          "properties": {
            "yarn.scheduler.minimum-allocation-mb": "341",
            "yarn.nodemanager.resource.memory-mb": "20000",
            "yarn.nodemanager.resource.cpu-vcores": '1'
          }
        },
        "hive-interactive-site":
          {
            'properties': {
              'hive.llap.daemon.queue.name':'llap',
              'hive.server2.tez.sessions.per.default.queue' : '1'
            }
          },
        "tez-interactive-site": {
          "properties": {
            "tez.am.resource.memory.mb": "341"
          }
        },
        "hive-env":
          {
            'properties': {
              'hive_user': 'hive'
            }
          },
        "hive-site":
          {
            'properties': {
              'hive.tez.container.size': '341'
            }
          },
      }
    }
    # Services 11: YARN service with : (1). 'capacity scheduler' having 'llap' and 'default' queue at root level and
    # (2). 'llap' queue state is STOPPED and sized 40 % and (3). 'enable_hive_interactive' is ON
    services_11 = {
      "services": [{
        "StackServices": {
          "service_name": "YARN",
        },
        "Versions": {
          "stack_version": "2.5"
        },
        "components": [
          {
            "StackServiceComponents": {
              "component_name": "NODEMANAGER",
              "hostnames": ["c6401.ambari.apache.org"]
            }
          }
        ]
      }, {
        "href": "/api/v1/stacks/HDP/versions/2.5/services/HIVE",
        "StackServices": {
          "service_name": "HIVE",
          "service_version": "1.2.1.2.5",
          "stack_name": "HDP",
          "stack_version": "2.5"
        },
        "components": [
          {
            "href": "/api/v1/stacks/HDP/versions/2.5/services/HIVE/components/HIVE_SERVER_INTERACTIVE",
            "StackServiceComponents": {
              "advertise_version": "true",
              "bulk_commands_display_name": "",
              "bulk_commands_master_component_name": "",
              "cardinality": "0-1",
              "component_category": "MASTER",
              "component_name": "HIVE_SERVER_INTERACTIVE",
              "custom_commands": ["RESTART_LLAP"],
              "decommission_allowed": "false",
              "display_name": "HiveServer2 Interactive",
              "has_bulk_commands_definition": "false",
              "is_client": "false",
              "is_master": "true",
              "reassign_allowed": "false",
              "recovery_enabled": "false",
              "service_name": "HIVE",
              "stack_name": "HDP",
              "stack_version": "2.5",
              "hostnames": ["c6401.ambari.apache.org"]
            },
            "dependencies": []
          }
        ]
      }
      ],
      "changed-configurations": [
        {
          u'old_value': u'',
          u'type': u'',
          u'name': u''
        }
      ],
      "configurations": {
        "capacity-scheduler": {
          "properties": {
            "capacity-scheduler": "yarn.scheduler.capacity.root.accessible-node-labels=*\n"
                                  "yarn.scheduler.capacity.root.capacity=100\n"
                                  "yarn.scheduler.capacity.root.queues=default,llap\n"
                                  "yarn.scheduler.capacity.maximum-applications=10000\n"
                                  "yarn.scheduler.capacity.root.default.user-limit-factor=1\n"
                                  "yarn.scheduler.capacity.root.default.state=RUNNING\n"
                                  "yarn.scheduler.capacity.maximum-am-resource-percent=1\n"
                                  "yarn.scheduler.capacity.root.default.acl_submit_applications=*\n"
                                  "yarn.scheduler.capacity.root.default.capacity=80\n"
                                  "yarn.scheduler.capacity.root.acl_administer_queue=*\n"
                                  "yarn.scheduler.capacity.node-locality-delay=40\n"
                                  "yarn.scheduler.capacity.queue-mappings-override.enable=false\n"
                                  "yarn.scheduler.capacity.root.llap.user-limit-factor=1\n"
                                  "yarn.scheduler.capacity.root.llap.state=RUNNING\n"
                                  "yarn.scheduler.capacity.root.llap.ordering-policy=fifo\n"
                                  "yarn.scheduler.capacity.root.llap.minimum-user-limit-percent=100\n"
                                  "yarn.scheduler.capacity.root.llap.maximum-capacity=20\n"
                                  "yarn.scheduler.capacity.root.default.maximum-capacity=80\n"
                                  "yarn.scheduler.capacity.root.llap.capacity=20\n"
                                  "yarn.scheduler.capacity.root.llap.acl_submit_applications=hive\n"
                                  "yarn.scheduler.capacity.root.llap.acl_administer_queue=hive\n"
                                  "yarn.scheduler.capacity.root.llap.maximum-am-resource-percent=1\n"
          }
        },
        "hive-interactive-env":
          {
            'properties': {
              'enable_hive_interactive': 'true',
              'llap_queue_capacity':'40'
            }
          },
        "hive-interactive-site":
          {
            'properties': {
              'hive.llap.daemon.queue.name':'llap'
            }
          },
        "tez-interactive-site": {
          "properties": {
            "tez.am.resource.memory.mb": "341"
          }
        },
        "yarn-site": {
          "properties": {
            "yarn.scheduler.minimum-allocation-mb": "341",
            "yarn.nodemanager.resource.memory-mb": "20000",
            "yarn.nodemanager.resource.cpu-vcores": '1'
          }
        },
        "hive-env":
          {
            'properties': {
              'hive_user': 'hive'
            }
          },
        "hive-site":
          {
            'properties': {
              'hive.tez.container.size': '341'
            }
          },
      }
    }



    # Services 12: YARN service with : (1). 'capacity scheduler' having more than 2 queues out of which one queue
    # is named 'llap' and (2). 'enable_hive_interactive' is OFF
    services_12= {
      "services": [{
        "StackServices": {
          "service_name": "YARN",
        },
        "Versions": {
          "stack_version": "2.5"
        },
        "components": [
          {
            "StackServiceComponents": {
              "component_name": "NODEMANAGER",
              "hostnames": ["c6401.ambari.apache.org"]
            }
          }
        ]
      }, {
        "href": "/api/v1/stacks/HDP/versions/2.5/services/HIVE",
        "StackServices": {
          "service_name": "HIVE",
          "service_version": "1.2.1.2.5",
          "stack_name": "HDP",
          "stack_version": "2.5"
        },
        "components": [
          {
            "href": "/api/v1/stacks/HDP/versions/2.5/services/HIVE/components/HIVE_SERVER_INTERACTIVE",
            "StackServiceComponents": {
              "advertise_version": "true",
              "bulk_commands_display_name": "",
              "bulk_commands_master_component_name": "",
              "cardinality": "0-1",
              "component_category": "MASTER",
              "component_name": "HIVE_SERVER_INTERACTIVE",
              "custom_commands": ["RESTART_LLAP"],
              "decommission_allowed": "false",
              "display_name": "HiveServer2 Interactive",
              "has_bulk_commands_definition": "false",
              "is_client": "false",
              "is_master": "true",
              "reassign_allowed": "false",
              "recovery_enabled": "false",
              "service_name": "HIVE",
              "stack_name": "HDP",
              "stack_version": "2.5",
              "hostnames": ["c6401.ambari.apache.org"]
            },
            "dependencies": []
          }
        ]
      }
      ],
      "changed-configurations": [
        {
          u'old_value': u'',
          u'type': u'',
          u'name': u''
        }
      ],
      "configurations": {
        "capacity-scheduler": {
          "properties": {
            "capacity-scheduler": "yarn.scheduler.capacity.maximum-am-resource-percent=0.2\n"
                                  "yarn.scheduler.capacity.maximum-applications=10000\n"
                                  "yarn.scheduler.capacity.node-locality-delay=40\n"
                                  "yarn.scheduler.capacity.queue-mappings-override.enable=false\n"
                                  "yarn.scheduler.capacity.resource-calculator=org.apache.hadoop.yarn.util.resource.DefaultResourceCalculator\n"
                                  "yarn.scheduler.capacity.root.accessible-node-labels=*\n"
                                  "yarn.scheduler.capacity.root.acl_administer_queue=*\n"
                                  "yarn.scheduler.capacity.root.capacity=100\n"
                                  "yarn.scheduler.capacity.root.default.a.a1.acl_administer_queue=*\n"
                                  "yarn.scheduler.capacity.root.default.a.a1.acl_submit_applications=*\n"
                                  "yarn.scheduler.capacity.root.default.a.a1.capacity=75\n"
                                  "yarn.scheduler.capacity.root.default.a.a1.maximum-capacity=100\n"
                                  "yarn.scheduler.capacity.root.default.a.a1.minimum-user-limit-percent=100\n"
                                  "yarn.scheduler.capacity.root.default.a.a1.ordering-policy=fifo\n"
                                  "yarn.scheduler.capacity.root.default.a.a1.state=RUNNING\n"
                                  "yarn.scheduler.capacity.root.default.a.a1.user-limit-factor=1\n"
                                  "yarn.scheduler.capacity.root.default.a.llap.acl_administer_queue=*\n"
                                  "yarn.scheduler.capacity.root.default.a.llap.acl_submit_applications=*\n"
                                  "yarn.scheduler.capacity.root.default.a.llap.capacity=25\n"
                                  "yarn.scheduler.capacity.root.default.a.llap.maximum-capacity=25\n"
                                  "yarn.scheduler.capacity.root.default.a.llap.minimum-user-limit-percent=100\n"
                                  "yarn.scheduler.capacity.root.default.a.llap.ordering-policy=fifo\n"
                                  "yarn.scheduler.capacity.root.default.a.llap.state=RUNNING\n"
                                  "yarn.scheduler.capacity.root.default.a.llap.user-limit-factor=1\n"
                                  "yarn.scheduler.capacity.root.default.a.acl_administer_queue=*\n"
                                  "yarn.scheduler.capacity.root.default.a.acl_submit_applications=*\n"
                                  "yarn.scheduler.capacity.root.default.a.capacity=50\n"
                                  "yarn.scheduler.capacity.root.default.a.maximum-capacity=100\n"
                                  "yarn.scheduler.capacity.root.default.a.minimum-user-limit-percent=100\n"
                                  "yarn.scheduler.capacity.root.default.a.ordering-policy=fifo\n"
                                  "yarn.scheduler.capacity.root.default.a.queues=a1,llap\n"
                                  "yarn.scheduler.capacity.root.default.a.state=RUNNING\n"
                                  "yarn.scheduler.capacity.root.default.a.user-limit-factor=1\n"
                                  "yarn.scheduler.capacity.root.default.acl_submit_applications=*\n"
                                  "yarn.scheduler.capacity.root.default.b.acl_administer_queue=*\n"
                                  "yarn.scheduler.capacity.root.default.b.acl_submit_applications=*\n"
                                  "yarn.scheduler.capacity.root.default.b.capacity=50\n"
                                  "yarn.scheduler.capacity.root.default.b.maximum-capacity=50\n"
                                  "yarn.scheduler.capacity.root.default.b.minimum-user-limit-percent=100\n"
                                  "yarn.scheduler.capacity.root.default.b.ordering-policy=fifo\n"
                                  "yarn.scheduler.capacity.root.default.b.state=RUNNING\n"
                                  "yarn.scheduler.capacity.root.default.b.user-limit-factor=1\n"
                                  "yarn.scheduler.capacity.root.default.capacity=100\n"
                                  "yarn.scheduler.capacity.root.default.maximum-capacity=100\n"
                                  "yarn.scheduler.capacity.root.default.queues=a,b\n"
                                  "yarn.scheduler.capacity.root.default.state=RUNNING\n"
                                  "yarn.scheduler.capacity.root.default.user-limit-factor=1\n"
                                  "yarn.scheduler.capacity.root.queues=default"
          }
        },
        "hive-interactive-env":
          {
            'properties': {
              'enable_hive_interactive': 'false',
              'llap_queue_capacity':'0'
            }
          },
        "hive-env":
          {
            'properties': {
              'hive_user': 'hive'
            }
          }
      }
    }


    # Services 13: YARN service with : (1). 'capacity scheduler' having 'llap' and 'default' queue at root level and
    # (2). 'enable_hive_interactive' is ON and (3). configuration change detected for 'llap_queue_capacity'
    services_13 = {
      "services": [{
        "StackServices": {
          "service_name": "YARN",
        },
        "Versions": {
          "stack_version": "2.5"
        },
        "components": [
          {
            "StackServiceComponents": {
              "component_name": "NODEMANAGER",
              "hostnames": ["c6401.ambari.apache.org"]
            }
          }
        ]
      }, {
        "href": "/api/v1/stacks/HDP/versions/2.5/services/HIVE",
        "StackServices": {
          "service_name": "HIVE",
          "service_version": "1.2.1.2.5",
          "stack_name": "HDP",
          "stack_version": "2.5"
        },
        "components": [
          {
            "href": "/api/v1/stacks/HDP/versions/2.5/services/HIVE/components/HIVE_SERVER_INTERACTIVE",
            "StackServiceComponents": {
              "advertise_version": "true",
              "bulk_commands_display_name": "",
              "bulk_commands_master_component_name": "",
              "cardinality": "0-1",
              "component_category": "MASTER",
              "component_name": "HIVE_SERVER_INTERACTIVE",
              "custom_commands": ["RESTART_LLAP"],
              "decommission_allowed": "false",
              "display_name": "HiveServer2 Interactive",
              "has_bulk_commands_definition": "false",
              "is_client": "false",
              "is_master": "true",
              "reassign_allowed": "false",
              "recovery_enabled": "false",
              "service_name": "HIVE",
              "stack_name": "HDP",
              "stack_version": "2.5",
              "hostnames": ["c6401.ambari.apache.org"]
            },
            "dependencies": []
          },
          {
            "StackServiceComponents": {
              "advertise_version": "true",
              "cardinality": "1+",
              "component_category": "SLAVE",
              "component_name": "NODEMANAGER",
              "display_name": "NodeManager",
              "is_client": "false",
              "is_master": "false",
              "hostnames": [
                "c6403.ambari.apache.org"
              ]
            },
            "dependencies": []
          },
        ]
      }
      ],
      "changed-configurations": [
        {
          u'old_value': u'0',
          u'type': u'hive-interactive-env',
          u'name': u'llap_queue_capacity'
        }
      ],
      "configurations": {
        "capacity-scheduler": {
          "properties": {
            "capacity-scheduler": 'yarn.scheduler.capacity.root.default.maximum-capacity=60\n'
                                  'yarn.scheduler.capacity.root.accessible-node-labels=*\n'
                                  'yarn.scheduler.capacity.root.capacity=100\n'
                                  'yarn.scheduler.capacity.root.queues=default,llap\n'
                                  'yarn.scheduler.capacity.maximum-applications=10000\n'
                                  'yarn.scheduler.capacity.root.default.user-limit-factor=1\n'
                                  'yarn.scheduler.capacity.root.default.state=RUNNING\n'
                                  'yarn.scheduler.capacity.maximum-am-resource-percent=1\n'
                                  'yarn.scheduler.capacity.root.default.acl_submit_applications=*\n'
                                  'yarn.scheduler.capacity.root.default.capacity=60\n'
                                  'yarn.scheduler.capacity.root.acl_administer_queue=*\n'
                                  'yarn.scheduler.capacity.node-locality-delay=40\n'
                                  'yarn.scheduler.capacity.queue-mappings-override.enable=false\n'
                                  'yarn.scheduler.capacity.root.llap.user-limit-factor=1\n'
                                  'yarn.scheduler.capacity.root.llap.state=RUNNING\n'
                                  'yarn.scheduler.capacity.root.llap.ordering-policy=fifo\n'
                                  'yarn.scheduler.capacity.root.llap.minimum-user-limit-percent=100\n'
                                  'yarn.scheduler.capacity.root.llap.maximum-capacity=40\n'
                                  'yarn.scheduler.capacity.root.llap.capacity=40\n'
                                  'yarn.scheduler.capacity.root.llap.acl_submit_applications=hive\n'
                                  'yarn.scheduler.capacity.root.llap.acl_administer_queue=hive\n'
                                  'yarn.scheduler.capacity.root.llap.maximum-am-resource-percent=1'

          }
        },
        "hive-interactive-env":
          {
            'properties': {
              'enable_hive_interactive': 'true',
              'llap_queue_capacity':'41'
            }
          },
        "hive-interactive-site":
          {
            'properties': {
              'hive.llap.daemon.queue.name': 'llap',
              'hive.server2.tez.sessions.per.default.queue': '1'
            }
          },
        "hive-env":
          {
            'properties': {
              'hive_user': 'hive'
            }
          },
        "yarn-site": {
          "properties": {
            "yarn.scheduler.minimum-allocation-mb": "341",
            "yarn.nodemanager.resource.memory-mb": "4096",
            "yarn.nodemanager.resource.cpu-vcores": '1'
          }
        },
        "tez-interactive-site": {
          "properties": {
            "tez.am.resource.memory.mb": "341"
          }
        },
        "hive-site":
          {
            'properties': {
              'hive.tez.container.size': '341'
            }
          },
      }
    }


    # Services 14: YARN service with : (1). 'capacity scheduler' having more than 2 queues and
    # (2). 'enable_hive_interactive' is ON and (3). configuration change detected for 'llap_queue_capacity'
    services_14 = {
      "services": [{
        "StackServices": {
          "service_name": "YARN",
        },
        "Versions": {
          "stack_version": "2.5"
        },
        "components": [
          {
            "StackServiceComponents": {
              "component_name": "NODEMANAGER",
              "hostnames": ["c6401.ambari.apache.org"]
            }
          }
        ]
      }, {
        "href": "/api/v1/stacks/HDP/versions/2.5/services/HIVE",
        "StackServices": {
          "service_name": "HIVE",
          "service_version": "1.2.1.2.5",
          "stack_name": "HDP",
          "stack_version": "2.5"
        },
        "components": [
          {
            "href": "/api/v1/stacks/HDP/versions/2.5/services/HIVE/components/HIVE_SERVER_INTERACTIVE",
            "StackServiceComponents": {
              "advertise_version": "true",
              "bulk_commands_display_name": "",
              "bulk_commands_master_component_name": "",
              "cardinality": "0-1",
              "component_category": "MASTER",
              "component_name": "HIVE_SERVER_INTERACTIVE",
              "custom_commands": ["RESTART_LLAP"],
              "decommission_allowed": "false",
              "display_name": "HiveServer2 Interactive",
              "has_bulk_commands_definition": "false",
              "is_client": "false",
              "is_master": "true",
              "reassign_allowed": "false",
              "recovery_enabled": "false",
              "service_name": "HIVE",
              "stack_name": "HDP",
              "stack_version": "2.5",
              "hostnames": ["c6401.ambari.apache.org"]
            },
            "dependencies": []
          },
          {
            "StackServiceComponents": {
              "advertise_version": "true",
              "cardinality": "1+",
              "component_category": "SLAVE",
              "component_name": "NODEMANAGER",
              "display_name": "NodeManager",
              "is_client": "false",
              "is_master": "false",
              "hostnames": [
                "c6403.ambari.apache.org"
              ]
            },
            "dependencies": []
          },
        ]
      }
      ],
      "changed-configurations": [
        {
          u'old_value': u'0',
          u'type': u'hive-interactive-env',
          u'name': u'llap_queue_capacity'
        }
      ],
      "configurations": {
        "capacity-scheduler": {
          "properties": {
            "capacity-scheduler": "yarn.scheduler.capacity.maximum-am-resource-percent=0.2\n"
                                  "yarn.scheduler.capacity.maximum-applications=10000\n"
                                  "yarn.scheduler.capacity.node-locality-delay=40\n"
                                  "yarn.scheduler.capacity.queue-mappings-override.enable=false\n"
                                  "yarn.scheduler.capacity.resource-calculator=org.apache.hadoop.yarn.util.resource.DefaultResourceCalculator\n"
                                  "yarn.scheduler.capacity.root.accessible-node-labels=*\n"
                                  "yarn.scheduler.capacity.root.acl_administer_queue=*\n"
                                  "yarn.scheduler.capacity.root.capacity=100\n"
                                  "yarn.scheduler.capacity.root.default.a.a1.acl_administer_queue=*\n"
                                  "yarn.scheduler.capacity.root.default.a.a1.acl_submit_applications=*\n"
                                  "yarn.scheduler.capacity.root.default.a.a1.capacity=75\n"
                                  "yarn.scheduler.capacity.root.default.a.a1.maximum-capacity=100\n"
                                  "yarn.scheduler.capacity.root.default.a.a1.minimum-user-limit-percent=100\n"
                                  "yarn.scheduler.capacity.root.default.a.a1.ordering-policy=fifo\n"
                                  "yarn.scheduler.capacity.root.default.a.a1.state=RUNNING\n"
                                  "yarn.scheduler.capacity.root.default.a.a1.user-limit-factor=1\n"
                                  "yarn.scheduler.capacity.root.default.a.llap.acl_administer_queue=*\n"
                                  "yarn.scheduler.capacity.root.default.a.llap.acl_submit_applications=*\n"
                                  "yarn.scheduler.capacity.root.default.a.llap.capacity=25\n"
                                  "yarn.scheduler.capacity.root.default.a.llap.maximum-capacity=25\n"
                                  "yarn.scheduler.capacity.root.default.a.llap.minimum-user-limit-percent=100\n"
                                  "yarn.scheduler.capacity.root.default.a.llap.ordering-policy=fifo\n"
                                  "yarn.scheduler.capacity.root.default.a.llap.state=RUNNING\n"
                                  "yarn.scheduler.capacity.root.default.a.llap.user-limit-factor=1\n"
                                  "yarn.scheduler.capacity.root.default.a.acl_administer_queue=*\n"
                                  "yarn.scheduler.capacity.root.default.a.acl_submit_applications=*\n"
                                  "yarn.scheduler.capacity.root.default.a.capacity=50\n"
                                  "yarn.scheduler.capacity.root.default.a.maximum-capacity=100\n"
                                  "yarn.scheduler.capacity.root.default.a.minimum-user-limit-percent=100\n"
                                  "yarn.scheduler.capacity.root.default.a.ordering-policy=fifo\n"
                                  "yarn.scheduler.capacity.root.default.a.queues=a1,llap\n"
                                  "yarn.scheduler.capacity.root.default.a.state=RUNNING\n"
                                  "yarn.scheduler.capacity.root.default.a.user-limit-factor=1\n"
                                  "yarn.scheduler.capacity.root.default.acl_submit_applications=*\n"
                                  "yarn.scheduler.capacity.root.default.b.acl_administer_queue=*\n"
                                  "yarn.scheduler.capacity.root.default.b.acl_submit_applications=*\n"
                                  "yarn.scheduler.capacity.root.default.b.capacity=50\n"
                                  "yarn.scheduler.capacity.root.default.b.maximum-capacity=50\n"
                                  "yarn.scheduler.capacity.root.default.b.minimum-user-limit-percent=100\n"
                                  "yarn.scheduler.capacity.root.default.b.ordering-policy=fifo\n"
                                  "yarn.scheduler.capacity.root.default.b.state=RUNNING\n"
                                  "yarn.scheduler.capacity.root.default.b.user-limit-factor=1\n"
                                  "yarn.scheduler.capacity.root.default.capacity=100\n"
                                  "yarn.scheduler.capacity.root.default.maximum-capacity=100\n"
                                  "yarn.scheduler.capacity.root.default.queues=a,b\n"
                                  "yarn.scheduler.capacity.root.default.state=RUNNING\n"
                                  "yarn.scheduler.capacity.root.default.user-limit-factor=1\n"
                                  "yarn.scheduler.capacity.root.queues=default"

          }
        },
        "hive-interactive-env":
          {
            'properties': {
              'enable_hive_interactive': 'true',
              'llap_queue_capacity':'40'
            }
          },
        "hive-interactive-site":
          {
            'properties': {
              'hive.llap.daemon.queue.name': 'llap',
              'hive.server2.tez.sessions.per.default.queue': '1'
            }
          },
        "hive-env":
          {
            'properties': {
              'hive_user': 'hive'
            }
          },
        "yarn-site": {
          "properties": {
            "yarn.scheduler.minimum-allocation-mb": "341",
            "yarn.nodemanager.resource.memory-mb": "4096",
            "yarn.nodemanager.resource.cpu-vcores": '1'
          }
        },
        "tez-interactive-site": {
          "properties": {
            "tez.am.resource.memory.mb": "341"
          }
        },
        "hive-site":
          {
            'properties': {
              'hive.tez.container.size': '341'
            }
          },
      }
    }



    # Services 15: YARN service with : (1). 'capacity scheduler' having 'llap' (state:stopped) and 'default' queue at
    # root level and (2). 'enable_hive_interactive' is ON and (3). 'hive.llap.daemon.queue.name' == 'default'
    services_15 = {
      "services": [{
        "StackServices": {
          "service_name": "YARN",
        },
        "Versions": {
          "stack_version": "2.5"
        },
        "components": [
          {
            "StackServiceComponents": {
              "component_name": "NODEMANAGER",
              "hostnames": ["c6401.ambari.apache.org"]
            }
          }
        ]
      }, {
        "href": "/api/v1/stacks/HDP/versions/2.5/services/HIVE",
        "StackServices": {
          "service_name": "HIVE",
          "service_version": "1.2.1.2.5",
          "stack_name": "HDP",
          "stack_version": "2.5"
        },
        "components": [
          {
            "href": "/api/v1/stacks/HDP/versions/2.5/services/HIVE/components/HIVE_SERVER_INTERACTIVE",
            "StackServiceComponents": {
              "advertise_version": "true",
              "bulk_commands_display_name": "",
              "bulk_commands_master_component_name": "",
              "cardinality": "0-1",
              "component_category": "MASTER",
              "component_name": "HIVE_SERVER_INTERACTIVE",
              "custom_commands": ["RESTART_LLAP"],
              "decommission_allowed": "false",
              "display_name": "HiveServer2 Interactive",
              "has_bulk_commands_definition": "false",
              "is_client": "false",
              "is_master": "true",
              "reassign_allowed": "false",
              "recovery_enabled": "false",
              "service_name": "HIVE",
              "stack_name": "HDP",
              "stack_version": "2.5",
              "hostnames": ["c6401.ambari.apache.org"]
            },
            "dependencies": []
          },
          {
            "StackServiceComponents": {
              "advertise_version": "true",
              "cardinality": "1+",
              "component_category": "SLAVE",
              "component_name": "NODEMANAGER",
              "display_name": "NodeManager",
              "is_client": "false",
              "is_master": "false",
              "hostnames": [
                "c6403.ambari.apache.org"
              ]
            },
            "dependencies": []
          },
        ]
      }
      ],
      "changed-configurations": [
        {
          u'old_value': u'0',
          u'type': u'hive-interactive-env',
          u'name': u'llap_queue_capacity'
        }
      ],
      "configurations": {
        "capacity-scheduler": {
          "properties": {
            "capacity-scheduler": 'yarn.scheduler.capacity.root.default.maximum-capacity=60\n'
                                  'yarn.scheduler.capacity.root.accessible-node-labels=*\n'
                                  'yarn.scheduler.capacity.root.capacity=100\n'
                                  'yarn.scheduler.capacity.root.queues=default,llap\n'
                                  'yarn.scheduler.capacity.maximum-applications=10000\n'
                                  'yarn.scheduler.capacity.root.default.user-limit-factor=1\n'
                                  'yarn.scheduler.capacity.root.default.state=RUNNING\n'
                                  'yarn.scheduler.capacity.maximum-am-resource-percent=1\n'
                                  'yarn.scheduler.capacity.root.default.acl_submit_applications=*\n'
                                  'yarn.scheduler.capacity.root.default.capacity=60\n'
                                  'yarn.scheduler.capacity.root.acl_administer_queue=*\n'
                                  'yarn.scheduler.capacity.node-locality-delay=40\n'
                                  'yarn.scheduler.capacity.queue-mappings-override.enable=false\n'
                                  'yarn.scheduler.capacity.root.llap.user-limit-factor=1\n'
                                  'yarn.scheduler.capacity.root.llap.state=STOPPED\n'
                                  'yarn.scheduler.capacity.root.llap.ordering-policy=fifo\n'
                                  'yarn.scheduler.capacity.root.llap.minimum-user-limit-percent=100\n'
                                  'yarn.scheduler.capacity.root.llap.maximum-capacity=40\n'
                                  'yarn.scheduler.capacity.root.llap.capacity=40\n'
                                  'yarn.scheduler.capacity.root.llap.acl_submit_applications=hive\n'
                                  'yarn.scheduler.capacity.root.llap.acl_administer_queue=hive\n'
                                  'yarn.scheduler.capacity.root.llap.maximum-am-resource-percent=1'

          }
        },
        "hive-interactive-env":
          {
            'properties': {
              'enable_hive_interactive': 'true',
              'llap_queue_capacity':'40'
            }
          },
        "hive-interactive-site":
          {
            'properties': {
              'hive.llap.daemon.queue.name': 'default',
              'hive.server2.tez.sessions.per.default.queue': '1'
            }
          },
        "hive-env":
          {
            'properties': {
              'hive_user': 'hive'
            }
          },
        "yarn-site": {
          "properties": {
            "yarn.scheduler.minimum-allocation-mb": "341",
            "yarn.nodemanager.resource.memory-mb": "4096",
            "yarn.nodemanager.resource.cpu-vcores": '1'
          }
        },
        "tez-interactive-site": {
          "properties": {
            "tez.am.resource.memory.mb": "341"
          }
        },
        "hive-site":
          {
            'properties': {
              'hive.tez.container.size': '341'
            }
          },
      }
    }



    # Services 16: YARN service with : (1). 'capacity scheduler' having 'llap' and 'default' queue at root level and
    # (2). 'enable_hive_interactive' is ON and (3). configuration change detected for 'enable_hive_interactive'
    services_16 = {
      "services": [{
        "StackServices": {
          "service_name": "YARN",
        },
        "Versions": {
          "stack_version": "2.5"
        },
        "components": [
          {
            "StackServiceComponents": {
              "component_name": "NODEMANAGER",
              "hostnames": ["c6401.ambari.apache.org"]
            }
          }
        ]
      }, {
        "href": "/api/v1/stacks/HDP/versions/2.5/services/HIVE",
        "StackServices": {
          "service_name": "HIVE",
          "service_version": "1.2.1.2.5",
          "stack_name": "HDP",
          "stack_version": "2.5"
        },
        "components": [
          {
            "href": "/api/v1/stacks/HDP/versions/2.5/services/HIVE/components/HIVE_SERVER_INTERACTIVE",
            "StackServiceComponents": {
              "advertise_version": "true",
              "bulk_commands_display_name": "",
              "bulk_commands_master_component_name": "",
              "cardinality": "0-1",
              "component_category": "MASTER",
              "component_name": "HIVE_SERVER_INTERACTIVE",
              "custom_commands": ["RESTART_LLAP"],
              "decommission_allowed": "false",
              "display_name": "HiveServer2 Interactive",
              "has_bulk_commands_definition": "false",
              "is_client": "false",
              "is_master": "true",
              "reassign_allowed": "false",
              "recovery_enabled": "false",
              "service_name": "HIVE",
              "stack_name": "HDP",
              "stack_version": "2.5",
              "hostnames": ["c6401.ambari.apache.org"]
            },
            "dependencies": []
          },
          {
            "StackServiceComponents": {
              "advertise_version": "true",
              "cardinality": "1+",
              "component_category": "SLAVE",
              "component_name": "NODEMANAGER",
              "display_name": "NodeManager",
              "is_client": "false",
              "is_master": "false",
              "hostnames": [
                "c6403.ambari.apache.org"
              ]
            },
            "dependencies": []
          },
        ]
      }
      ],
      "changed-configurations": [
        {
          u'old_value': u'true',
          u'type': u'hive-interactive-env',
          u'name': u'enable_hive_interactive'
        }
      ],
      "configurations": {
        "capacity-scheduler": {
          "properties": {
            "capacity-scheduler": 'yarn.scheduler.capacity.root.default.maximum-capacity=60\n'
                                  'yarn.scheduler.capacity.root.accessible-node-labels=*\n'
                                  'yarn.scheduler.capacity.root.capacity=100\n'
                                  'yarn.scheduler.capacity.root.queues=default,llap\n'
                                  'yarn.scheduler.capacity.maximum-applications=10000\n'
                                  'yarn.scheduler.capacity.root.default.user-limit-factor=1\n'
                                  'yarn.scheduler.capacity.root.default.state=RUNNING\n'
                                  'yarn.scheduler.capacity.maximum-am-resource-percent=1\n'
                                  'yarn.scheduler.capacity.root.default.acl_submit_applications=*\n'
                                  'yarn.scheduler.capacity.root.default.capacity=60\n'
                                  'yarn.scheduler.capacity.root.acl_administer_queue=*\n'
                                  'yarn.scheduler.capacity.node-locality-delay=40\n'
                                  'yarn.scheduler.capacity.queue-mappings-override.enable=false\n'
                                  'yarn.scheduler.capacity.root.llap.user-limit-factor=1\n'
                                  'yarn.scheduler.capacity.root.llap.state=RUNNING\n'
                                  'yarn.scheduler.capacity.root.llap.ordering-policy=fifo\n'
                                  'yarn.scheduler.capacity.root.llap.minimum-user-limit-percent=100\n'
                                  'yarn.scheduler.capacity.root.llap.maximum-capacity=40\n'
                                  'yarn.scheduler.capacity.root.llap.capacity=40\n'
                                  'yarn.scheduler.capacity.root.llap.acl_submit_applications=hive\n'
                                  'yarn.scheduler.capacity.root.llap.acl_administer_queue=hive\n'
                                  'yarn.scheduler.capacity.root.llap.maximum-am-resource-percent=1'

          }
        },
        "hive-interactive-env":
          {
            'properties': {
              'enable_hive_interactive': 'true',
              'llap_queue_capacity':'41'
            }
          },
        "hive-interactive-site":
          {
            'properties': {
              'hive.llap.daemon.queue.name': 'llap',
              'hive.server2.tez.sessions.per.default.queue': '1'
            }
          },
        "hive-env":
          {
            'properties': {
              'hive_user': 'hive'
            }
          },
        "yarn-site": {
          "properties": {
            "yarn.scheduler.minimum-allocation-mb": "341",
            "yarn.nodemanager.resource.memory-mb": "4096",
            "yarn.nodemanager.resource.cpu-vcores": '1'
          }
        },
        "tez-interactive-site": {
          "properties": {
            "tez.am.resource.memory.mb": "341"
          }
        },
        "hive-site":
          {
            'properties': {
              'hive.tez.container.size': '341'
            }
          },
      }
    }


    # Services 17: YARN service with : (1). 'capacity scheduler' having 'llap' and 'default' queue at root level and
    # (2). 'enable_hive_interactive' is OFF and (3). configuration change detected for 'enable_hive_interactive'
    services_17 = {
      "services": [{
        "StackServices": {
          "service_name": "YARN",
        },
        "Versions": {
          "stack_version": "2.5"
        },
        "components": [
          {
            "StackServiceComponents": {
              "component_name": "NODEMANAGER",
              "hostnames": ["c6401.ambari.apache.org"]
            }
          }
        ]
      }, {
        "href": "/api/v1/stacks/HDP/versions/2.5/services/HIVE",
        "StackServices": {
          "service_name": "HIVE",
          "service_version": "1.2.1.2.5",
          "stack_name": "HDP",
          "stack_version": "2.5"
        },
        "components": [
          {
            "href": "/api/v1/stacks/HDP/versions/2.5/services/HIVE/components/HIVE_SERVER_INTERACTIVE",
            "StackServiceComponents": {
              "advertise_version": "true",
              "bulk_commands_display_name": "",
              "bulk_commands_master_component_name": "",
              "cardinality": "0-1",
              "component_category": "MASTER",
              "component_name": "HIVE_SERVER_INTERACTIVE",
              "custom_commands": ["RESTART_LLAP"],
              "decommission_allowed": "false",
              "display_name": "HiveServer2 Interactive",
              "has_bulk_commands_definition": "false",
              "is_client": "false",
              "is_master": "true",
              "reassign_allowed": "false",
              "recovery_enabled": "false",
              "service_name": "HIVE",
              "stack_name": "HDP",
              "stack_version": "2.5",
              "hostnames": ["c6401.ambari.apache.org"]
            },
            "dependencies": []
          },
          {
            "StackServiceComponents": {
              "advertise_version": "true",
              "cardinality": "1+",
              "component_category": "SLAVE",
              "component_name": "NODEMANAGER",
              "display_name": "NodeManager",
              "is_client": "false",
              "is_master": "false",
              "hostnames": [
                "c6403.ambari.apache.org"
              ]
            },
            "dependencies": []
          },
        ]
      }
      ],
      "changed-configurations": [
        {
          u'old_value': u'false',
          u'type': u'hive-interactive-env',
          u'name': u'enable_hive_interactive'
        }
      ],
      "configurations": {
        "capacity-scheduler": {
          "properties": {
            "capacity-scheduler": 'yarn.scheduler.capacity.root.default.maximum-capacity=60\n'
                                  'yarn.scheduler.capacity.root.accessible-node-labels=*\n'
                                  'yarn.scheduler.capacity.root.capacity=100\n'
                                  'yarn.scheduler.capacity.root.queues=default,llap\n'
                                  'yarn.scheduler.capacity.maximum-applications=10000\n'
                                  'yarn.scheduler.capacity.root.default.user-limit-factor=1\n'
                                  'yarn.scheduler.capacity.root.default.state=RUNNING\n'
                                  'yarn.scheduler.capacity.maximum-am-resource-percent=1\n'
                                  'yarn.scheduler.capacity.root.default.acl_submit_applications=*\n'
                                  'yarn.scheduler.capacity.root.default.capacity=60\n'
                                  'yarn.scheduler.capacity.root.acl_administer_queue=*\n'
                                  'yarn.scheduler.capacity.node-locality-delay=40\n'
                                  'yarn.scheduler.capacity.queue-mappings-override.enable=false\n'
                                  'yarn.scheduler.capacity.root.llap.user-limit-factor=1\n'
                                  'yarn.scheduler.capacity.root.llap.state=RUNNING\n'
                                  'yarn.scheduler.capacity.root.llap.ordering-policy=fifo\n'
                                  'yarn.scheduler.capacity.root.llap.minimum-user-limit-percent=100\n'
                                  'yarn.scheduler.capacity.root.llap.maximum-capacity=40\n'
                                  'yarn.scheduler.capacity.root.llap.capacity=40\n'
                                  'yarn.scheduler.capacity.root.llap.acl_submit_applications=hive\n'
                                  'yarn.scheduler.capacity.root.llap.acl_administer_queue=hive\n'
                                  'yarn.scheduler.capacity.root.llap.maximum-am-resource-percent=1'

          }
        },
        "hive-interactive-env":
          {
            'properties': {
              'enable_hive_interactive': 'false',
              'llap_queue_capacity':'40'
            }
          },
        "hive-interactive-site":
          {
            'properties': {
              'hive.llap.daemon.queue.name': 'llap',
              'hive.server2.tez.sessions.per.default.queue': '1'
            }
          },
        "hive-env":
          {
            'properties': {
              'hive_user': 'hive'
            }
          },
        "yarn-site": {
          "properties": {
            "yarn.scheduler.minimum-allocation-mb": "341",
            "yarn.nodemanager.resource.memory-mb": "4096",
            "yarn.nodemanager.resource.cpu-vcores": '1'
          }
        },
        "tez-interactive-site": {
          "properties": {
            "tez.am.resource.memory.mb": "341"
          }
        }
      }
    }



    # Services 18: YARN service with : (1). 'capacity scheduler' having 'llap' and 'default' queue at root level and
    # (2). 'enable_hive_interactive' is OFF and (3). configuration change NOT detected for 'enable_hive_interactive'
    services_18 = {
      "services": [{
        "StackServices": {
          "service_name": "YARN",
        },
        "Versions": {
          "stack_version": "2.5"
        },
        "components": [
          {
            "StackServiceComponents": {
              "component_name": "NODEMANAGER",
              "hostnames": ["c6401.ambari.apache.org"]
            }
          }
        ]
      }, {
        "href": "/api/v1/stacks/HDP/versions/2.5/services/HIVE",
        "StackServices": {
          "service_name": "HIVE",
          "service_version": "1.2.1.2.5",
          "stack_name": "HDP",
          "stack_version": "2.5"
        },
        "components": [
          {
            "href": "/api/v1/stacks/HDP/versions/2.5/services/HIVE/components/HIVE_SERVER_INTERACTIVE",
            "StackServiceComponents": {
              "advertise_version": "true",
              "bulk_commands_display_name": "",
              "bulk_commands_master_component_name": "",
              "cardinality": "0-1",
              "component_category": "MASTER",
              "component_name": "HIVE_SERVER_INTERACTIVE",
              "custom_commands": ["RESTART_LLAP"],
              "decommission_allowed": "false",
              "display_name": "HiveServer2 Interactive",
              "has_bulk_commands_definition": "false",
              "is_client": "false",
              "is_master": "true",
              "reassign_allowed": "false",
              "recovery_enabled": "false",
              "service_name": "HIVE",
              "stack_name": "HDP",
              "stack_version": "2.5",
              "hostnames": ["c6401.ambari.apache.org"]
            },
            "dependencies": []
          },
          {
            "StackServiceComponents": {
              "advertise_version": "true",
              "cardinality": "1+",
              "component_category": "SLAVE",
              "component_name": "NODEMANAGER",
              "display_name": "NodeManager",
              "is_client": "false",
              "is_master": "false",
              "hostnames": [
                "c6403.ambari.apache.org"
              ]
            },
            "dependencies": []
          },
        ]
      }
      ],
      "configurations": {
        "capacity-scheduler": {
          "properties": {
            "capacity-scheduler": 'yarn.scheduler.capacity.root.default.maximum-capacity=60\n'
                                  'yarn.scheduler.capacity.root.accessible-node-labels=*\n'
                                  'yarn.scheduler.capacity.root.capacity=100\n'
                                  'yarn.scheduler.capacity.root.queues=default,llap\n'
                                  'yarn.scheduler.capacity.maximum-applications=10000\n'
                                  'yarn.scheduler.capacity.root.default.user-limit-factor=1\n'
                                  'yarn.scheduler.capacity.root.default.state=RUNNING\n'
                                  'yarn.scheduler.capacity.maximum-am-resource-percent=1\n'
                                  'yarn.scheduler.capacity.root.default.acl_submit_applications=*\n'
                                  'yarn.scheduler.capacity.root.default.capacity=60\n'
                                  'yarn.scheduler.capacity.root.acl_administer_queue=*\n'
                                  'yarn.scheduler.capacity.node-locality-delay=40\n'
                                  'yarn.scheduler.capacity.queue-mappings-override.enable=false\n'
                                  'yarn.scheduler.capacity.root.llap.user-limit-factor=1\n'
                                  'yarn.scheduler.capacity.root.llap.state=STOPPED\n'
                                  'yarn.scheduler.capacity.root.llap.ordering-policy=fifo\n'
                                  'yarn.scheduler.capacity.root.llap.minimum-user-limit-percent=100\n'
                                  'yarn.scheduler.capacity.root.llap.maximum-capacity=40\n'
                                  'yarn.scheduler.capacity.root.llap.capacity=40\n'
                                  'yarn.scheduler.capacity.root.llap.acl_submit_applications=hive\n'
                                  'yarn.scheduler.capacity.root.llap.acl_administer_queue=hive\n'
                                  'yarn.scheduler.capacity.root.llap.maximum-am-resource-percent=1'

          }
        },
        "hive-interactive-env":
          {
            'properties': {
              'enable_hive_interactive': 'false',
              'llap_queue_capacity':'40'
            }
          },
        "hive-interactive-site":
          {
            'properties': {
              'hive.llap.daemon.queue.name': 'llap',
              'hive.server2.tez.sessions.per.default.queue': '1'
            }
          },
        "hive-env":
          {
            'properties': {
              'hive_user': 'hive'
            }
          },
        "yarn-site": {
          "properties": {
            "yarn.scheduler.minimum-allocation-mb": "341",
            "yarn.nodemanager.resource.memory-mb": "4096",
            "yarn.nodemanager.resource.cpu-vcores": '1'
          }
        },
        "tez-interactive-site": {
          "properties": {
            "tez.am.resource.memory.mb": "341"
          }
        }
      }
    }


    # Services 19: YARN service with : (1). 'capacity scheduler' having 'llap' and 'default' queue at root level and
    # (2). 'enable_hive_interactive' is ON and (3). configuration change detected for 'enable_hive_interactive'
    services_19 = {
      "services": [{
        "StackServices": {
          "service_name": "YARN",
        },
        "Versions": {
          "stack_version": "2.5"
        },
        "components": [
          {
            "StackServiceComponents": {
              "component_name": "NODEMANAGER",
              "hostnames": ["c6401.ambari.apache.org"]
            }
          }
        ]
      }, {
        "href": "/api/v1/stacks/HDP/versions/2.5/services/HIVE",
        "StackServices": {
          "service_name": "HIVE",
          "service_version": "1.2.1.2.5",
          "stack_name": "HDP",
          "stack_version": "2.5"
        },
        "components": [
          {
            "href": "/api/v1/stacks/HDP/versions/2.5/services/HIVE/components/HIVE_SERVER_INTERACTIVE",
            "StackServiceComponents": {
              "advertise_version": "true",
              "bulk_commands_display_name": "",
              "bulk_commands_master_component_name": "",
              "cardinality": "0-1",
              "component_category": "MASTER",
              "component_name": "HIVE_SERVER_INTERACTIVE",
              "custom_commands": ["RESTART_LLAP"],
              "decommission_allowed": "false",
              "display_name": "HiveServer2 Interactive",
              "has_bulk_commands_definition": "false",
              "is_client": "false",
              "is_master": "true",
              "reassign_allowed": "false",
              "recovery_enabled": "false",
              "service_name": "HIVE",
              "stack_name": "HDP",
              "stack_version": "2.5",
              "hostnames": ["c6401.ambari.apache.org"]
            },
            "dependencies": []
          },
          {
            "StackServiceComponents": {
              "advertise_version": "true",
              "cardinality": "1+",
              "component_category": "SLAVE",
              "component_name": "NODEMANAGER",
              "display_name": "NodeManager",
              "is_client": "false",
              "is_master": "false",
              "hostnames": [
                "c6403.ambari.apache.org"
              ]
            },
            "dependencies": []
          },
        ]
      }
      ],
      "changed-configurations": [
        {
          u'old_value': u'2',
          u'type': u'hive-interactive-env',
          u'name': u'num_llap_nodes'
        }
      ],
      "configurations": {
        "capacity-scheduler": {
          "properties": {
            "capacity-scheduler": 'yarn.scheduler.capacity.root.default.maximum-capacity=60\n'
                                  'yarn.scheduler.capacity.root.accessible-node-labels=*\n'
                                  'yarn.scheduler.capacity.root.capacity=100\n'
                                  'yarn.scheduler.capacity.root.queues=default,llap\n'
                                  'yarn.scheduler.capacity.maximum-applications=10000\n'
                                  'yarn.scheduler.capacity.root.default.user-limit-factor=1\n'
                                  'yarn.scheduler.capacity.root.default.state=RUNNING\n'
                                  'yarn.scheduler.capacity.maximum-am-resource-percent=1\n'
                                  'yarn.scheduler.capacity.root.default.acl_submit_applications=*\n'
                                  'yarn.scheduler.capacity.root.default.capacity=60\n'
                                  'yarn.scheduler.capacity.root.acl_administer_queue=*\n'
                                  'yarn.scheduler.capacity.node-locality-delay=40\n'
                                  'yarn.scheduler.capacity.queue-mappings-override.enable=false\n'
                                  'yarn.scheduler.capacity.root.llap.user-limit-factor=1\n'
                                  'yarn.scheduler.capacity.root.llap.state=RUNNING\n'
                                  'yarn.scheduler.capacity.root.llap.ordering-policy=fifo\n'
                                  'yarn.scheduler.capacity.root.llap.minimum-user-limit-percent=100\n'
                                  'yarn.scheduler.capacity.root.llap.maximum-capacity=40\n'
                                  'yarn.scheduler.capacity.root.llap.capacity=40\n'
                                  'yarn.scheduler.capacity.root.llap.acl_submit_applications=hive\n'
                                  'yarn.scheduler.capacity.root.llap.acl_administer_queue=hive\n'
                                  'yarn.scheduler.capacity.root.llap.maximum-am-resource-percent=1'

          }
        },
        "hive-interactive-env":
          {
            'properties': {
              'enable_hive_interactive': 'true',
              'llap_queue_capacity':'41',
              'num_llap_nodes': 1
            }
          },
        "hive-interactive-site":
          {
            'properties': {
              'hive.llap.daemon.queue.name': 'llap',
              'hive.server2.tez.sessions.per.default.queue': '1',

            }
          },
        "hive-env":
          {
            'properties': {
              'hive_user': 'hive'
            }
          },
        "yarn-site": {
          "properties": {
            "yarn.scheduler.minimum-allocation-mb": "341",
            "yarn.nodemanager.resource.memory-mb": "4096",
            "yarn.nodemanager.resource.cpu-vcores": '1'
          }
        },
        "tez-interactive-site": {
          "properties": {
            "tez.am.resource.memory.mb": "341"
          }
        },
        "hive-site":
          {
            'properties': {
              'hive.tez.container.size': '341'
            }
          },
      }
    }


    # Services 20: YARN service with : (1). 'capacity scheduler' having 'llap' and 'default' queue at root level and
    # (2). 'enable_hive_interactive' is ON and (3). configuration change detected for 'llap_queue_capacity'
    # yarn.nodemanager.resource.memory-mb": "24576"
    services_20 = {
      "services": [{
        "StackServices": {
          "service_name": "YARN",
        },
        "Versions": {
          "stack_version": "2.5"
        },
        "components": [
          {
            "StackServiceComponents": {
              "component_name": "NODEMANAGER",
              "hostnames": ["c6401.ambari.apache.org"]
            }
          }
        ]
      }, {
        "href": "/api/v1/stacks/HDP/versions/2.5/services/HIVE",
        "StackServices": {
          "service_name": "HIVE",
          "service_version": "1.2.1.2.5",
          "stack_name": "HDP",
          "stack_version": "2.5"
        },
        "components": [
          {
            "href": "/api/v1/stacks/HDP/versions/2.5/services/HIVE/components/HIVE_SERVER_INTERACTIVE",
            "StackServiceComponents": {
              "advertise_version": "true",
              "bulk_commands_display_name": "",
              "bulk_commands_master_component_name": "",
              "cardinality": "0-1",
              "component_category": "MASTER",
              "component_name": "HIVE_SERVER_INTERACTIVE",
              "custom_commands": ["RESTART_LLAP"],
              "decommission_allowed": "false",
              "display_name": "HiveServer2 Interactive",
              "has_bulk_commands_definition": "false",
              "is_client": "false",
              "is_master": "true",
              "reassign_allowed": "false",
              "recovery_enabled": "false",
              "service_name": "HIVE",
              "stack_name": "HDP",
              "stack_version": "2.5",
              "hostnames": ["c6401.ambari.apache.org"]
            },
            "dependencies": []
          },
          {
            "StackServiceComponents": {
              "advertise_version": "true",
              "cardinality": "1+",
              "component_category": "SLAVE",
              "component_name": "NODEMANAGER",
              "display_name": "NodeManager",
              "is_client": "false",
              "is_master": "false",
              "hostnames": [
                "c6403.ambari.apache.org"
              ]
            },
            "dependencies": []
          },
        ]
      }
      ],
      "changed-configurations": [
        {
          u'old_value': u'25',
          u'type': u'hive-interactive-env',
          u'name': u'llap_queue_capacity'
        }
      ],
      "configurations": {
        "capacity-scheduler": {
          "properties": {
            "capacity-scheduler": 'yarn.scheduler.capacity.root.default.maximum-capacity=60\n'
                                  'yarn.scheduler.capacity.root.accessible-node-labels=*\n'
                                  'yarn.scheduler.capacity.root.capacity=100\n'
                                  'yarn.scheduler.capacity.root.queues=default,llap\n'
                                  'yarn.scheduler.capacity.maximum-applications=10000\n'
                                  'yarn.scheduler.capacity.root.default.user-limit-factor=1\n'
                                  'yarn.scheduler.capacity.root.default.state=RUNNING\n'
                                  'yarn.scheduler.capacity.maximum-am-resource-percent=1\n'
                                  'yarn.scheduler.capacity.root.default.acl_submit_applications=*\n'
                                  'yarn.scheduler.capacity.root.default.capacity=60\n'
                                  'yarn.scheduler.capacity.root.acl_administer_queue=*\n'
                                  'yarn.scheduler.capacity.node-locality-delay=40\n'
                                  'yarn.scheduler.capacity.queue-mappings-override.enable=false\n'
                                  'yarn.scheduler.capacity.root.llap.user-limit-factor=1\n'
                                  'yarn.scheduler.capacity.root.llap.state=RUNNING\n'
                                  'yarn.scheduler.capacity.root.llap.ordering-policy=fifo\n'
                                  'yarn.scheduler.capacity.root.llap.minimum-user-limit-percent=100\n'
                                  'yarn.scheduler.capacity.root.llap.maximum-capacity=40\n'
                                  'yarn.scheduler.capacity.root.llap.capacity=40\n'
                                  'yarn.scheduler.capacity.root.llap.acl_submit_applications=hive\n'
                                  'yarn.scheduler.capacity.root.llap.acl_administer_queue=hive\n'
                                  'yarn.scheduler.capacity.root.llap.maximum-am-resource-percent=1'

          }
        },
        "hive-interactive-env":
          {
            'properties': {
              'enable_hive_interactive': 'true',
              'llap_queue_capacity':'60'
            }
          },
        "hive-interactive-site":
          {
            'properties': {
              'hive.llap.daemon.queue.name': 'llap',
              'hive.server2.tez.sessions.per.default.queue': '1'
            }
          },
        "hive-env":
          {
            'properties': {
              'hive_user': 'hive'
            }
          },
        "yarn-site": {
          "properties": {
            "yarn.scheduler.minimum-allocation-mb": "682",
            "yarn.nodemanager.resource.memory-mb": "24576",
            "yarn.nodemanager.resource.cpu-vcores": '2'
          }
        },
        "tez-interactive-site": {
          "properties": {
            "tez.am.resource.memory.mb": "341"
          }
        },
        "hive-site":
          {
            'properties': {
              'hive.tez.container.size': '341'
            }
          },
      }
    }






    # Expected config outputs.

    # Expected capacity-scheduler with 'llap' (size:20) and 'default' queue at root level.
    expected_capacity_scheduler_llap_queue_size_20 = {
      "properties": {
        "capacity-scheduler": 'yarn.scheduler.capacity.root.default.maximum-capacity=80\n'
                              'yarn.scheduler.capacity.root.accessible-node-labels=*\n'
                              'yarn.scheduler.capacity.root.capacity=100\n'
                              'yarn.scheduler.capacity.root.queues=default,llap\n'
                              'yarn.scheduler.capacity.maximum-applications=10000\n'
                              'yarn.scheduler.capacity.root.default.user-limit-factor=1\n'
                              'yarn.scheduler.capacity.root.default.state=RUNNING\n'
                              'yarn.scheduler.capacity.maximum-am-resource-percent=1\n'
                              'yarn.scheduler.capacity.root.default.acl_submit_applications=*\n'
                              'yarn.scheduler.capacity.root.default.capacity=80\n'
                              'yarn.scheduler.capacity.root.acl_administer_queue=*\n'
                              'yarn.scheduler.capacity.node-locality-delay=40\n'
                              'yarn.scheduler.capacity.queue-mappings-override.enable=false\n'
                              'yarn.scheduler.capacity.root.llap.user-limit-factor=1\n'
                              'yarn.scheduler.capacity.root.llap.state=RUNNING\n'
                              'yarn.scheduler.capacity.root.llap.ordering-policy=fifo\n'
                              'yarn.scheduler.capacity.root.llap.minimum-user-limit-percent=100\n'
                              'yarn.scheduler.capacity.root.llap.maximum-capacity=20\n'
                              'yarn.scheduler.capacity.root.llap.capacity=20\n'
                              'yarn.scheduler.capacity.root.llap.acl_submit_applications=hive\n'
                              'yarn.scheduler.capacity.root.llap.acl_administer_queue=hive\n'
                              'yarn.scheduler.capacity.root.llap.maximum-am-resource-percent=1'

      }
    }

    # Expected capacity-scheduler with 'llap' (size:40) and 'default' queue at root level.
    expected_capacity_scheduler_llap_queue_size_40 = {
      "properties": {
        "capacity-scheduler": 'yarn.scheduler.capacity.root.default.maximum-capacity=60\n'
                              'yarn.scheduler.capacity.root.accessible-node-labels=*\n'
                              'yarn.scheduler.capacity.root.capacity=100\n'
                              'yarn.scheduler.capacity.root.queues=default,llap\n'
                              'yarn.scheduler.capacity.maximum-applications=10000\n'
                              'yarn.scheduler.capacity.root.default.user-limit-factor=1\n'
                              'yarn.scheduler.capacity.root.default.state=RUNNING\n'
                              'yarn.scheduler.capacity.maximum-am-resource-percent=1\n'
                              'yarn.scheduler.capacity.root.default.acl_submit_applications=*\n'
                              'yarn.scheduler.capacity.root.default.capacity=60\n'
                              'yarn.scheduler.capacity.root.acl_administer_queue=*\n'
                              'yarn.scheduler.capacity.node-locality-delay=40\n'
                              'yarn.scheduler.capacity.queue-mappings-override.enable=false\n'
                              'yarn.scheduler.capacity.root.llap.user-limit-factor=1\n'
                              'yarn.scheduler.capacity.root.llap.state=RUNNING\n'
                              'yarn.scheduler.capacity.root.llap.ordering-policy=fifo\n'
                              'yarn.scheduler.capacity.root.llap.minimum-user-limit-percent=100\n'
                              'yarn.scheduler.capacity.root.llap.maximum-capacity=40\n'
                              'yarn.scheduler.capacity.root.llap.capacity=40\n'
                              'yarn.scheduler.capacity.root.llap.acl_submit_applications=hive\n'
                              'yarn.scheduler.capacity.root.llap.acl_administer_queue=hive\n'
                              'yarn.scheduler.capacity.root.llap.maximum-am-resource-percent=1'

      }
    }

    # Expected capacity-scheduler with 'llap' state = STOPPED, cap = 0 % and 'default' queue cap to 100%.
    expected_capacity_scheduler_llap_Stopped_size_0 = {
      "properties": {
        "capacity-scheduler": 'yarn.scheduler.capacity.root.default.maximum-capacity=100\n'
                              'yarn.scheduler.capacity.root.accessible-node-labels=*\n'
                              'yarn.scheduler.capacity.root.capacity=100\n'
                              'yarn.scheduler.capacity.root.queues=default,llap\n'
                              'yarn.scheduler.capacity.maximum-applications=10000\n'
                              'yarn.scheduler.capacity.root.default.user-limit-factor=1\n'
                              'yarn.scheduler.capacity.root.default.state=RUNNING\n'
                              'yarn.scheduler.capacity.maximum-am-resource-percent=1\n'
                              'yarn.scheduler.capacity.root.default.acl_submit_applications=*\n'
                              'yarn.scheduler.capacity.root.default.capacity=100\n'
                              'yarn.scheduler.capacity.root.acl_administer_queue=*\n'
                              'yarn.scheduler.capacity.node-locality-delay=40\n'
                              'yarn.scheduler.capacity.queue-mappings-override.enable=false\n'
                              'yarn.scheduler.capacity.root.llap.user-limit-factor=1\n'
                              'yarn.scheduler.capacity.root.llap.state=STOPPED\n'
                              'yarn.scheduler.capacity.root.llap.ordering-policy=fifo\n'
                              'yarn.scheduler.capacity.root.llap.minimum-user-limit-percent=100\n'
                              'yarn.scheduler.capacity.root.llap.maximum-capacity=0\n'
                              'yarn.scheduler.capacity.root.llap.capacity=0\n'
                              'yarn.scheduler.capacity.root.llap.acl_submit_applications=hive\n'
                              'yarn.scheduler.capacity.root.llap.acl_administer_queue=hive\n'
                              'yarn.scheduler.capacity.root.llap.maximum-am-resource-percent=1'

      }
    }

    # Expected capacity-scheduler with only 'default' queue.
    expected_capacity_scheduler_with_default_queue_only = {
      "properties": {
        "capacity-scheduler": 'yarn.scheduler.capacity.root.accessible-node-labels=*\n'
                              'yarn.scheduler.capacity.maximum-am-resource-percent=1\n'
                              'yarn.scheduler.capacity.root.capacity=100\n'
                              'yarn.scheduler.capacity.root.default.state=RUNNING\n'
                              'yarn.scheduler.capacity.node-locality-delay=40\n'
                              'yarn.scheduler.capacity.root.queues=default\n'
                              'yarn.scheduler.capacity.maximum-applications=10000\n'
                              'yarn.scheduler.capacity.root.default.user-limit-factor=1\n'
                              'yarn.scheduler.capacity.root.acl_administer_queue=*\n'
                              'yarn.scheduler.capacity.root.default.acl_submit_applications=*\n'
                              'yarn.scheduler.capacity.root.default.capacity=100\n'
                              "yarn.scheduler.capacity.root.default.maximum-capacity=100\n"
                              'yarn.scheduler.capacity.queue-mappings-override.enable=false\n'
      }
    }

    # Expected capacity-scheduler as empty.
    expected_capacity_scheduler_empty = {
      "properties": {
      }
    }

    # Expected 'hive_interactive_site' with (1). 'hive.llap.daemon.queue.name' set to 'llap' queue, and
    # (2). 'hive.llap.daemon.queue.name' property_attributes set to : default and llap.
    expected_hive_interactive_site_llap = {
      "hive-interactive-site": {
        "properties": {
          "hive.llap.daemon.queue.name": "llap"
        },
        "property_attributes": {
          "hive.llap.daemon.queue.name": {
            "entries": [
              {
                "value": "default",
                "label": "default"
              },
              {
                "value": "llap",
                "label": "llap"
              }
            ]
          }
        }
      }
    }

    # Expected 'hive_interactive_site' with (1). 'hive.llap.daemon.queue.name' set to 'default' queue, and
    # (2). 'hive.llap.daemon.queue.name' property_attributes set to : default.
    expected_hive_interactive_site_default = {
      "hive-interactive-site": {
        "properties": {
          "hive.llap.daemon.queue.name": "default"
        },
        "property_attributes": {
          "hive.llap.daemon.queue.name": {
            "entries": [
              {
                "value": "default",
                "label": "default"
              }
            ]
          }
        }
      }
    }

    # Expected 'hive_interactive_site' when no modifications are done.
    expected_hive_interactive_site_empty = {
      "hive-interactive-site": {
        "properties": {
        }
      }
    }

    # Expected 'hive_interactive_site' when no modifications are done.
    expected_hive_interactive_env_empty = {
      "hive-interactive-env": {
        "properties": {
        }
      }
    }

    expected_hive_interactive_site_only_memory = {
      "hive-interactive-site": {
        "properties": {
          'hive.llap.daemon.yarn.container.mb': '341'
        }
      }
    }

    # Expected 'hive_interactive_env' with 'llap_queue_capacity' set to 20.
    expected_llap_queue_capacity_20 = '20'

    # Expected 'hive_interactive_env' with 'llap_queue_capacity' set to 40.
    expected_llap_queue_capacity_40 = '40'


    # expected vals.
    expected_visibility_false = {'visible': 'false'}
    expected_visibility_true = {'visible': 'true'}
    expected_llap_queue_attributes_min_25_visible_true = {'maximum': '100', 'minimum': '25', 'visible': 'true'}
    expected_llap_queue_attributes_min_21_visible_true = {'maximum': '100', 'minimum': '21', 'visible': 'true'}



    #################### Tests #####################



    # Test 1 : (1). Only default queue exists in capacity-scheduler (2). enable_hive_interactive' is 'On' and
    # 'llap_queue_capacity is 0.
    configurations = {
    }
    self.stackAdvisor.recommendYARNConfigurations(configurations, clusterData, services_1, hosts)
    # Check output
    self.assertEquals(configurations['hive-interactive-site']['properties']['hive.llap.daemon.queue.name'],
                      expected_hive_interactive_site_llap['hive-interactive-site']['properties']['hive.llap.daemon.queue.name'])
    self.assertEquals(configurations['hive-interactive-site']['property_attributes']['hive.llap.daemon.queue.name'],
                      expected_hive_interactive_site_llap['hive-interactive-site']['property_attributes']['hive.llap.daemon.queue.name'])
    self.assertEquals(configurations['hive-interactive-env']['properties']['llap_queue_capacity'],
                      expected_llap_queue_capacity_20)

    cap_sched_output_dict = convertToDict(configurations['capacity-scheduler']['properties']['capacity-scheduler'])
    cap_sched_expected_dict = convertToDict(expected_capacity_scheduler_llap_queue_size_20['properties']['capacity-scheduler'])
    self.assertEqual(cap_sched_output_dict, cap_sched_expected_dict)



    # Test 2: (1). Only default queue exists in capacity-scheduler (2). enable_hive_interactive' is 'On' and
    # 'llap_queue_capacity is 40.
    configurations = {
    }

    self.stackAdvisor.recommendYARNConfigurations(configurations, clusterData, services_3, hosts)

    # Check output
    self.assertEquals(configurations['hive-interactive-site']['properties']['hive.llap.daemon.queue.name'],
                      expected_hive_interactive_site_llap['hive-interactive-site']['properties']['hive.llap.daemon.queue.name'])
    self.assertEquals(configurations['hive-interactive-site']['property_attributes']['hive.llap.daemon.queue.name'],
                      expected_hive_interactive_site_llap['hive-interactive-site']['property_attributes']['hive.llap.daemon.queue.name'])
    self.assertTrue('llap_queue_capacity' not in configurations['hive-interactive-env']['properties'])

    cap_sched_output_dict = convertToDict(configurations['capacity-scheduler']['properties']['capacity-scheduler'])
    cap_sched_expected_dict = convertToDict(expected_capacity_scheduler_llap_queue_size_40['properties']['capacity-scheduler'])
    self.assertEqual(cap_sched_output_dict, cap_sched_expected_dict)


    # Test 3: (1). 'llap' (0%) and 'default' (100%) queues exists at leaf level in capacity-scheduler
    #         (2). llap is state = STOPPED, (3). llap_queue_capacity = 0, and (4). enable_hive_interactive' is 'ON'.
    #         Expected : llap queue state = RUNNING, llap_queue_capacity = 20
    configurations = {
    }

    self.stackAdvisor.recommendYARNConfigurations(configurations, clusterData, services_10, hosts)

    # Check output
    self.assertEquals(configurations['hive-interactive-site']['properties']['hive.llap.daemon.queue.name'],
                      expected_hive_interactive_site_llap['hive-interactive-site']['properties']['hive.llap.daemon.queue.name'])
    self.assertEquals(configurations['hive-interactive-site']['property_attributes']['hive.llap.daemon.queue.name'],
                      expected_hive_interactive_site_llap['hive-interactive-site']['property_attributes']['hive.llap.daemon.queue.name'])
    self.assertEquals(configurations['hive-interactive-env']['properties']['llap_queue_capacity'],
                      expected_llap_queue_capacity_20)

    cap_sched_output_dict = convertToDict(configurations['capacity-scheduler']['properties']['capacity-scheduler'])
    cap_sched_expected_dict = convertToDict(expected_capacity_scheduler_llap_queue_size_20['properties']['capacity-scheduler'])
    self.assertEqual(cap_sched_output_dict, cap_sched_expected_dict)



    # Test 4: (1). 'llap' (20%) and 'default' (80%) queues exists at leaf level in capacity-scheduler
    #         (2). llap is state = STOPPED, (3). llap_queue_capacity = 40, and (4). enable_hive_interactive' is 'ON'.
    #         Expected : llap state goes RUNNING.
    configurations = {
    }

    self.stackAdvisor.recommendYARNConfigurations(configurations, clusterData, services_11, hosts)

    # Check output
    self.assertEquals(configurations['hive-interactive-site']['properties']['hive.llap.daemon.queue.name'],
                      expected_hive_interactive_site_llap['hive-interactive-site']['properties']['hive.llap.daemon.queue.name'])
    self.assertEquals(configurations['hive-interactive-site']['property_attributes']['hive.llap.daemon.queue.name'],
                      expected_hive_interactive_site_llap['hive-interactive-site']['property_attributes']['hive.llap.daemon.queue.name'])
    self.assertTrue('llap_queue_capacity' not in configurations['hive-interactive-env']['properties'])

    cap_sched_output_dict = convertToDict(configurations['capacity-scheduler']['properties']['capacity-scheduler'])
    cap_sched_expected_dict = convertToDict(expected_capacity_scheduler_llap_queue_size_40['properties']['capacity-scheduler'])
    self.assertEqual(cap_sched_output_dict, cap_sched_expected_dict)




    # Test 5: (1). 'llap' (20%) and 'default' (60%) queues exists at leaf level in capacity-scheduler
    #         (2). llap is state = RUNNING, (3). llap_queue_capacity = 40, and (4). enable_hive_interactive' is 'ON'.
    #         Expected : Existing llap queue's capacity in capacity-scheduler set to 40.
    configurations = {
    }

    self.stackAdvisor.recommendYARNConfigurations(configurations, clusterData, services_11, hosts)

    # Check output
    self.assertEquals(configurations['hive-interactive-site']['properties']['hive.llap.daemon.queue.name'],
                      expected_hive_interactive_site_llap['hive-interactive-site']['properties']['hive.llap.daemon.queue.name'])
    self.assertEquals(configurations['hive-interactive-site']['property_attributes']['hive.llap.daemon.queue.name'],
                      expected_hive_interactive_site_llap['hive-interactive-site']['property_attributes']['hive.llap.daemon.queue.name'])
    self.assertTrue('llap_queue_capacity' not in configurations['hive-interactive-env']['properties'])

    cap_sched_output_dict = convertToDict(configurations['capacity-scheduler']['properties']['capacity-scheduler'])
    cap_sched_expected_dict = convertToDict(expected_capacity_scheduler_llap_queue_size_40['properties']['capacity-scheduler'])
    self.assertEqual(cap_sched_output_dict, cap_sched_expected_dict)



    # Test 6: (1). Only default queue exists in capacity-scheduler (2). enable_hive_interactive' is 'Off' and
    #         'llap_queue_capacity is 0.
    #         Expected : No changes
    configurations = {
    }

    self.stackAdvisor.recommendYARNConfigurations(configurations, clusterData, services_2, hosts)

    # Check output
    self.assertTrue('hive.llap.daemon.queue.name' not in configurations['hive-interactive-site']['properties'])
    self.assertTrue('property_attributes' not in configurations['hive-interactive-site'])
    self.assertTrue('hive-interactive-env' not in configurations)
    self.assertEquals(configurations['capacity-scheduler']['properties'],expected_capacity_scheduler_empty['properties'])



    # Test 7: (1). 'default' and 'llap' (State : RUNNING) queue exists at root level in capacity-scheduler, and
    #         (2). enable_hive_interactive' is 'off'.
    #         Expected : 'default' queue set to Size 100, 'llap' queue state set to STOPPED and sized to 0.
    configurations = {
    }

    self.stackAdvisor.recommendYARNConfigurations(configurations, clusterData, services_4, hosts)

    # Check output
    self.assertEquals(configurations['hive-interactive-site']['properties']['hive.llap.daemon.queue.name'],
                      expected_hive_interactive_site_default['hive-interactive-site']['properties']['hive.llap.daemon.queue.name'])
    self.assertFalse('property_attributes' in configurations['hive-interactive-site'])
    self.assertFalse('hive-interactive-env' in configurations)

    cap_sched_output_dict = convertToDict(configurations['capacity-scheduler']['properties']['capacity-scheduler'])
    cap_sched_expected_dict = convertToDict(expected_capacity_scheduler_llap_Stopped_size_0['properties']['capacity-scheduler'])
    self.assertEqual(cap_sched_output_dict, cap_sched_expected_dict)



    # Test 8: (1). More than 2 queues at leaf level exists in capacity-scheduler (no queue is named 'llap')
    #         (2). enable_hive_interactive' is 'off'.
    #         Expected : No changes.
    configurations = {
    }

    self.stackAdvisor.recommendYARNConfigurations(configurations, clusterData, services_6, hosts)

    # Check output
    self.assertEquals(configurations['hive-interactive-site']['properties'],
                      expected_hive_interactive_site_empty['hive-interactive-site']['properties'])
    self.assertEquals(configurations['capacity-scheduler']['properties'],
                      expected_capacity_scheduler_empty['properties'])
    self.assertFalse('hive-interactive-env' in configurations)



    # Test 9: (1). More than 2 queues at leaf level exists in capacity-scheduler (one queue is named 'llap')
    #         (2). enable_hive_interactive' is 'off'.
    #         Expected : No changes.
    configurations = {
    }

    self.stackAdvisor.recommendYARNConfigurations(configurations, clusterData, services_12, hosts)

    # Check output
    self.assertEquals(configurations['hive-interactive-site']['properties'],
                      expected_hive_interactive_site_empty['hive-interactive-site']['properties'])
    self.assertEquals(configurations['capacity-scheduler']['properties'],
                      expected_capacity_scheduler_empty['properties'])
    self.assertFalse('hive-interactive-env' in configurations)



    # Test 10: (1). 'llap' (Cap: 0%, State: STOPPED) and 'default' (100%) queues exists at leaf level
    #               in capacity-scheduler
    #          (2). enable_hive_interactive' is 'off'.
    #          Expected : No changes.
    configurations = {
    }

    self.stackAdvisor.recommendYARNConfigurations(configurations, clusterData, services_9, hosts)

    # Check output
    self.assertEquals(configurations['hive-interactive-site']['properties'],
                      expected_hive_interactive_site_empty['hive-interactive-site']['properties'])
    self.assertEquals(configurations['capacity-scheduler']['properties'],
                      expected_capacity_scheduler_empty['properties'])
    self.assertFalse('hive-interactive-env' in configurations)




    # Test 11: capacity-scheduler not present as input in services.
    #         Expected : No changes.
    configurations = {
    }
    self.stackAdvisor.recommendYARNConfigurations(configurations, clusterData, services_7, hosts)

    # Check output
    self.assertEquals(configurations['capacity-scheduler']['properties'],
                      expected_capacity_scheduler_empty['properties'])
    self.assertFalse('hive-interactive-env' in configurations)
    self.assertEquals(configurations['hive-interactive-site']['properties'],
                      expected_hive_interactive_site_empty['hive-interactive-site']['properties'])



    # Test 12: capacity-scheduler malformed as input in services.
    #         Expected : No changes.
    configurations = {
    }
    self.stackAdvisor.recommendYARNConfigurations(configurations, clusterData, services_8, hosts)

    # Check output
    self.assertEquals(configurations['capacity-scheduler']['properties'],
                      expected_capacity_scheduler_empty['properties'])
    self.assertFalse('hive-interactive-env' in configurations)
    self.assertEquals(configurations['hive-interactive-site']['properties'],
                      expected_hive_interactive_site_empty['hive-interactive-site']['properties'])




    # Test 13: (1). 'default' and 'llap' (State : RUNNING) queue exists at root level in capacity-scheduler, and
    #         (2). enable_hive_interactive' is 'on' and (3). configuration change detected for 'llap_queue_capacity'
    #         Expected : Configurations values recommended for llap related configs.
    configurations = {
    }

    # expected vals.
    yarn_cont_mb_attributes = {'minimum': '682', 'maximum': '682'}
    llap_io_mem_size_attributes = {'minimum': '0', 'maximum': '341'}
    llap_num_executors_attributes = {'minimum': '1', 'maximum': '1'}
    num_llap_nodes_attributes = {'minimum': '1', 'maximum': '1'}


    self.stackAdvisor.recommendYARNConfigurations(configurations, clusterData, services_13, hosts)

    self.assertEqual(configurations['hive-interactive-site']['properties']['hive.llap.daemon.yarn.container.mb'], '682')
    self.assertEquals(configurations['hive-interactive-site']['property_attributes']['hive.llap.daemon.yarn.container.mb'], yarn_cont_mb_attributes)

    self.assertEqual(configurations['hive-interactive-site']['properties']['hive.llap.daemon.num.executors'], '1')
    self.assertEquals(configurations['hive-interactive-site']['property_attributes']['hive.llap.daemon.num.executors'], llap_num_executors_attributes)

    self.assertEqual(configurations['hive-interactive-site']['properties']['hive.llap.io.memory.size'], '341')
    self.assertEquals(configurations['hive-interactive-site']['property_attributes']['hive.llap.io.memory.size'], llap_io_mem_size_attributes)

    self.assertEqual(configurations['hive-interactive-env']['properties']['num_llap_nodes'], '1')
    self.assertEquals(configurations['hive-interactive-env']['property_attributes']['num_llap_nodes'], num_llap_nodes_attributes)

    self.assertEqual(configurations['hive-interactive-env']['properties']['llap_heap_size'], '272')

    self.assertEqual(configurations['hive-interactive-env']['properties']['slider_am_container_mb'], '341')
    self.assertEquals(configurations['hive-interactive-env']['property_attributes']['llap_queue_capacity'],
                      expected_llap_queue_attributes_min_25_visible_true)



    # Test 14: (1). 'default' and 'llap' (State : RUNNING) queue exists at root level in capacity-scheduler, and
    #         (2). enable_hive_interactive' is 'on' and (3). configuration change detected for 'enable_hive_interactive'
    #         Expected : Configurations values recommended for llap related configs.
    configurations = {
    }

    # expected vals.
    yarn_cont_mb_attributes = {'minimum': '682', 'maximum': '682'}
    llap_io_mem_size_attributes = {'minimum': '0', 'maximum': '341'}
    llap_num_executors_attributes = {'minimum': '1', 'maximum': '1'}
    num_llap_nodes_attributes = {'minimum': '1', 'maximum': '1'}



    self.stackAdvisor.recommendYARNConfigurations(configurations, clusterData, services_16, hosts)


    self.assertEqual(configurations['hive-interactive-site']['properties']['hive.llap.daemon.yarn.container.mb'], '682')
    self.assertEquals(configurations['hive-interactive-site']['property_attributes']['hive.llap.daemon.yarn.container.mb'], yarn_cont_mb_attributes)

    self.assertEqual(configurations['hive-interactive-site']['properties']['hive.llap.daemon.num.executors'], '1')
    self.assertEquals(configurations['hive-interactive-site']['property_attributes']['hive.llap.daemon.num.executors'], llap_num_executors_attributes)

    self.assertEqual(configurations['hive-interactive-site']['properties']['hive.llap.io.memory.size'], '341')
    self.assertEquals(configurations['hive-interactive-site']['property_attributes']['hive.llap.io.memory.size'], llap_io_mem_size_attributes)

    self.assertEqual(configurations['hive-interactive-env']['properties']['num_llap_nodes'], '1')
    self.assertEquals(configurations['hive-interactive-env']['property_attributes']['num_llap_nodes'], num_llap_nodes_attributes)

    self.assertEqual(configurations['hive-interactive-env']['properties']['llap_heap_size'], '272')

    self.assertEqual(configurations['hive-interactive-env']['properties']['slider_am_container_mb'], '341')
    self.assertEquals(configurations['hive-interactive-env']['property_attributes']['llap_queue_capacity'],
                      expected_llap_queue_attributes_min_25_visible_true)



    # Test 15: (1). 'default' and 'llap' (State : RUNNING) queue exists at root level in capacity-scheduler, and
    #         (2). enable_hive_interactive' is 'OFF' and (3). configuration change detected for 'enable_hive_interactive'
    #         Expected : Configurations values recommended for llap related configs.
    configurations = {
    }

    self.stackAdvisor.recommendYARNConfigurations(configurations, clusterData, services_17, hosts)

    cap_sched_output_dict = convertToDict(configurations['capacity-scheduler']['properties']['capacity-scheduler'])
    cap_sched_expected_dict = convertToDict(expected_capacity_scheduler_llap_Stopped_size_0['properties']['capacity-scheduler'])
    self.assertEqual(cap_sched_output_dict, cap_sched_expected_dict)

    self.assertEquals(configurations['hive-interactive-site']['properties'],
                      expected_hive_interactive_site_default['hive-interactive-site']['properties'])
    self.assertTrue('hive-interactive-env' not in configurations)

    self.assertTrue('property_attributes' not in configurations)


    # Test 16: (1). 'default' and 'llap' (State : RUNNING) queue exists at root level in capacity-scheduler, and
    #         (2). enable_hive_interactive' is 'OFF' and (3). configuration change NOT detected for 'enable_hive_interactive'
    #         Expected : No changes.
    configurations = {
    }
    self.stackAdvisor.recommendYARNConfigurations(configurations, clusterData, services_18, hosts)

    self.assertEquals(configurations['capacity-scheduler']['properties'],
                      expected_capacity_scheduler_empty['properties'])
    self.assertEquals(configurations['hive-interactive-site']['properties'],
                      expected_hive_interactive_site_empty['hive-interactive-site']['properties'])
    self.assertTrue('hive-interactive-env' not in configurations)

    self.assertTrue('property_attributes' not in configurations)


    # Test 17: (1). 'default' and 'llap' (State : RUNNING) queue exists at root level in capacity-scheduler, and
    #         (2). enable_hive_interactive' is 'on' and (3). configuration change detected for 'enable_hive_interactive'
    #         Expected : Configurations values recommended for llap related configs.
    configurations = {
    }

    # expected vals.
    yarn_cont_mb_visibility = {'minimum': '682', 'maximum': '682'}
    llap_io_mem_size_visibility = {'minimum': '0', 'maximum': '341'}
    llap_num_executors_visibility = {'minimum': '1', 'maximum': '1'}



    self.stackAdvisor.recommendYARNConfigurations(configurations, clusterData, services_19, hosts)

    self.assertEqual(configurations['hive-interactive-site']['properties']['hive.llap.daemon.yarn.container.mb'], '682')
    self.assertEquals(configurations['hive-interactive-site']['property_attributes']['hive.llap.daemon.yarn.container.mb'], yarn_cont_mb_visibility)

    self.assertEqual(configurations['hive-interactive-site']['properties']['hive.llap.daemon.num.executors'], '1')
    self.assertEquals(configurations['hive-interactive-site']['property_attributes']['hive.llap.daemon.num.executors'], llap_num_executors_visibility)

    self.assertEqual(configurations['hive-interactive-site']['properties']['hive.llap.io.memory.size'], '341')
    self.assertEquals(configurations['hive-interactive-site']['property_attributes']['hive.llap.io.memory.size'], llap_io_mem_size_visibility)

    self.assertTrue('num_llap_nodes' not in configurations['hive-interactive-env']['properties'])

    self.assertEqual(configurations['hive-interactive-env']['properties']['llap_heap_size'], '272')

    self.assertEqual(configurations['hive-interactive-env']['properties']['slider_am_container_mb'], '341')

    self.assertEquals(configurations['hive-interactive-env']['property_attributes']['llap_queue_capacity'],
                      expected_llap_queue_attributes_min_25_visible_true)





    # Test 18: (1). 'default' and 'llap' (State : RUNNING) queue exists at root level in capacity-scheduler, and
    #         (2). enable_hive_interactive' is 'on' and (3). configuration change detected for 'llap_queue_capacity'
    #         Expected : Configurations values recommended for llap related configs.
    configurations = {
    }

    # expected vals.
    yarn_cont_mb_visibility = {'minimum': '12958', 'maximum': '12958'}
    llap_io_mem_size_visibility = {'minimum': '0', 'maximum': '12617'}
    llap_num_executors_visibility = {'minimum': '1', 'maximum': '8'}
    num_llap_nodes_visibility = {'minimum': '1', 'maximum': '1'}



    self.stackAdvisor.recommendYARNConfigurations(configurations, clusterData, services_20, hosts)

    self.assertEqual(configurations['hive-interactive-site']['properties']['hive.llap.daemon.yarn.container.mb'], '12958')
    self.assertEquals(configurations['hive-interactive-site']['property_attributes']['hive.llap.daemon.yarn.container.mb'], yarn_cont_mb_visibility)

    self.assertEqual(configurations['hive-interactive-site']['properties']['hive.llap.daemon.num.executors'], '2')
    self.assertEquals(configurations['hive-interactive-site']['property_attributes']['hive.llap.daemon.num.executors'], llap_num_executors_visibility)

    self.assertEqual(configurations['hive-interactive-site']['properties']['hive.llap.io.memory.size'], '12276')
    self.assertEquals(configurations['hive-interactive-site']['property_attributes']['hive.llap.io.memory.size'], llap_io_mem_size_visibility)

    self.assertEqual(configurations['hive-interactive-env']['properties']['num_llap_nodes'], '1')
    self.assertEquals(configurations['hive-interactive-env']['property_attributes']['num_llap_nodes'], num_llap_nodes_visibility)

    self.assertEqual(configurations['hive-interactive-env']['properties']['llap_heap_size'], '545')

    self.assertEqual(configurations['hive-interactive-env']['properties']['slider_am_container_mb'], '682')

    self.assertEquals(configurations['hive-interactive-env']['property_attributes']['llap_queue_capacity'],
                      expected_llap_queue_attributes_min_21_visible_true)




    # Test 19: (1). 'default' and 'llap' (State : RUNNING) queue exists at root level in capacity-scheduler, and
    #         (2). enable_hive_interactive' is 'on' and (3). More than 2 queues exist.
    #
    configurations = {
    }



    self.stackAdvisor.recommendYARNConfigurations(configurations, clusterData, services_14, hosts)

    self.assertEquals(configurations['capacity-scheduler']['properties'],
                      expected_capacity_scheduler_empty['properties'])
    self.assertEquals(configurations['hive-interactive-site']['properties'],
                      expected_hive_interactive_site_empty['hive-interactive-site']['properties'])
    self.assertEquals(configurations['hive-interactive-env']['properties'],
                      expected_hive_interactive_env_empty['hive-interactive-env']['properties'])
    self.assertEquals(configurations['hive-interactive-env']['property_attributes']['llap_queue_capacity'],
                      expected_visibility_true)



    # Test 20: YARN service with : (1). 'capacity scheduler' having 'llap' (state:stopped) and 'default' queue at
    # root level and (2). 'enable_hive_interactive' is ON and (3). 'hive.llap.daemon.queue.name' == 'default'
    #
    configurations = {
    }

    self.stackAdvisor.recommendYARNConfigurations(configurations, clusterData, services_15, hosts)
    self.assertEquals(configurations['capacity-scheduler']['properties'],
                      expected_capacity_scheduler_empty['properties'])

    self.assertEquals(configurations['hive-interactive-site']['properties'],
                      expected_hive_interactive_site_only_memory['hive-interactive-site']['properties'])

    self.assertEquals(configurations['hive-interactive-env']['properties'],
                      expected_hive_interactive_env_empty['hive-interactive-env']['properties'])

    self.assertEquals(configurations['hive-interactive-env']['property_attributes']['llap_queue_capacity'], expected_visibility_false)


  def test_recommendAtlasConfigurations(self):
    self.maxDiff = None
    configurations = {
      "application-properties": {
        "properties": {
          "atlas.graph.index.search.solr.zookeeper-url": ""
        }
      },
      "logsearch-solr-env": {
        "properties": {
          "logsearch_solr_znode": "/logsearch"
        }
      }
    }
    clusterData = {
      "cpu": 4,
      "mapMemory": 3000,
      "amMemory": 2000,
      "reduceMemory": 2056,
      "containers": 3,
      "ramPerContainer": 256
    }
    expected = {
      'application-properties': {
        'properties': {
          'atlas.graph.index.search.solr.zookeeper-url': '{{zookeeper_quorum}}/logsearch',
        }
      },
      "logsearch-solr-env": {
        "properties": {
          "logsearch_solr_znode": "/logsearch"
        }
      }
    }
    services = {
      "services": [
        {
          "href": "/api/v1/stacks/HDP/versions/2.2/services/ATLAS",
          "StackServices": {
            "service_name": "LOGSEARCH",
            "service_version": "2.6.0.2.2",
            "stack_name": "HDP",
            "stack_version": "2.3"
          },
          "components": [
            {
              "StackServiceComponents": {
                "advertise_version": "false",
                "cardinality": "1",
                "component_category": "MASTER",
                "component_name": "LOGSEARCH_SOLR",
                "display_name": "solr",
                "is_client": "false",
                "is_master": "true",
                "hostnames": []
              },
              "dependencies": []
            }
          ]
        },
      ],
      "configurations": {
        "application-properties": {
          "properties": {
            "atlas.graph.index.search.solr.zookeeper-url": ""
          }
        },
        "logsearch-solr-env": {
          "properties": {
            "logsearch_solr_znode": "/logsearch"
          }
        }
      },
      "changed-configurations": [ ]

    }
    hosts = {
      "items" : [
        {
          "href" : "/api/v1/hosts/c6401.ambari.apache.org",
          "Hosts" : {
            "cpu_count" : 1,
            "host_name" : "c6401.ambari.apache.org",
            "os_arch" : "x86_64",
            "os_type" : "centos6",
            "ph_cpu_count" : 1,
            "public_host_name" : "c6401.ambari.apache.org",
            "rack_info" : "/default-rack",
            "total_mem" : 1922680
          }
        }
      ]
    }

    self.stackAdvisor.recommendAtlasConfigurations(configurations, clusterData, services, hosts)
    self.assertEquals(configurations, expected)

    services['ambari-server-properties'] = {'java.home': '/usr/jdk64/jdk1.7.3_23'}
    self.stackAdvisor.recommendAtlasConfigurations(configurations, clusterData, services, hosts)
    self.assertEquals(configurations, expected)
    
"""
Helper method to convert string of key-values to dict.
"""
def convertToDict(properties):
  capacitySchedulerProperties = dict()
  properties = str(properties).split('\n')
  if properties:
    for property in properties:
      key, sep, value = property.partition("=")
      if key:
        capacitySchedulerProperties[key] = value
  return capacitySchedulerProperties
