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

    # setup for 'test_recommendYARNConfigurations'
    self.hosts = {
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
          },
        }, {
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
            "public_host_name": "c6402.ambari.apache.org",
            "host_name": "c6402.ambari.apache.org"
          },
        }, {
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
            "public_host_name": "c6403.ambari.apache.org",
            "host_name": "c6403.ambari.apache.org"
          },
        }, {
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
            "public_host_name": "c6404.ambari.apache.org",
            "host_name": "c6404.ambari.apache.org"
          },
        }, {
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
            "public_host_name": "c6405.ambari.apache.org",
            "host_name": "c6405.ambari.apache.org"
          },
        }
      ]
    }


    # setup for 'test_recommendYARNConfigurations'
    self.hosts_9_total = {
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
          },
        }, {
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
            "public_host_name": "c6402.ambari.apache.org",
            "host_name": "c6402.ambari.apache.org"
          },
        }, {
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
            "public_host_name": "c6403.ambari.apache.org",
            "host_name": "c6403.ambari.apache.org"
          },
        }, {
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
            "public_host_name": "c6404.ambari.apache.org",
            "host_name": "c6404.ambari.apache.org"
          },
        }, {
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
            "public_host_name": "c6405.ambari.apache.org",
            "host_name": "c6405.ambari.apache.org"
          },
        }, {
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
            "public_host_name": "c6406.ambari.apache.org",
            "host_name": "c6406.ambari.apache.org"
          },
        }, {
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
            "public_host_name": "c6407.ambari.apache.org",
            "host_name": "c6407.ambari.apache.org"
          },
        }, {
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
            "public_host_name": "c6408.ambari.apache.org",
            "host_name": "c6408.ambari.apache.org"
          },
        }, {
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
            "public_host_name": "c6409.ambari.apache.org",
            "host_name": "c6409.ambari.apache.org"
          },
        }
      ]
    }

    # Expected config outputs.

    # Expected capacity-scheduler with 'llap' (size:20) and 'default' queue at root level.
    self.expected_capacity_scheduler_llap_queue_size_20 = {
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
                              'yarn.scheduler.capacity.root.ordering-policy=priority-utilization\n'
                              'yarn.scheduler.capacity.root.llap.user-limit-factor=1\n'
                              'yarn.scheduler.capacity.root.llap.state=RUNNING\n'
                              'yarn.scheduler.capacity.root.llap.ordering-policy=fifo\n'
                              'yarn.scheduler.capacity.root.llap.priority=10\n'
                              'yarn.scheduler.capacity.root.llap.minimum-user-limit-percent=100\n'

                              'yarn.scheduler.capacity.root.llap.maximum-capacity=20\n'
                              'yarn.scheduler.capacity.root.llap.capacity=20\n'
                              'yarn.scheduler.capacity.root.llap.acl_submit_applications=hive\n'
                              'yarn.scheduler.capacity.root.llap.acl_administer_queue=hive\n'
                              'yarn.scheduler.capacity.root.llap.maximum-am-resource-percent=1'

      }
    }

    # Expected capacity-scheduler with 'llap' (size:40) and 'default' queue at root level.
    self.expected_capacity_scheduler_llap_queue_size_40 = {
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
                              'yarn.scheduler.capacity.root.ordering-policy=priority-utilization\n'
                              'yarn.scheduler.capacity.root.llap.user-limit-factor=1\n'
                              'yarn.scheduler.capacity.root.llap.state=RUNNING\n'
                              'yarn.scheduler.capacity.root.llap.ordering-policy=fifo\n'
                              'yarn.scheduler.capacity.root.llap.priority=10\n'
                              'yarn.scheduler.capacity.root.llap.minimum-user-limit-percent=100\n'
                              'yarn.scheduler.capacity.root.llap.maximum-capacity=40\n'
                              'yarn.scheduler.capacity.root.llap.capacity=40\n'
                              'yarn.scheduler.capacity.root.llap.acl_submit_applications=hive\n'
                              'yarn.scheduler.capacity.root.llap.acl_administer_queue=hive\n'
                              'yarn.scheduler.capacity.root.llap.maximum-am-resource-percent=1'

      }
    }

    # Expected capacity-scheduler with 'llap' state = STOPPED, cap = 0 % and 'default' queue cap to 100%.
    self.expected_capacity_scheduler_llap_Stopped_size_0 = {
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
                              'yarn.scheduler.capacity.root.llap.priority=10\n'
                              'yarn.scheduler.capacity.root.llap.minimum-user-limit-percent=100\n'
                              'yarn.scheduler.capacity.root.llap.maximum-capacity=0\n'
                              'yarn.scheduler.capacity.root.llap.capacity=0\n'
                              'yarn.scheduler.capacity.root.llap.acl_submit_applications=hive\n'
                              'yarn.scheduler.capacity.root.llap.acl_administer_queue=hive\n'
                              'yarn.scheduler.capacity.root.llap.maximum-am-resource-percent=1'

      }
    }

    # Expected capacity-scheduler with only 'default' queue.
    self.expected_capacity_scheduler_with_default_queue_only = {
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
    self.expected_capacity_scheduler_empty = {
      "properties": {
      }
    }

    # Expected 'hive_interactive_site' with (1). 'hive.llap.daemon.queue.name' set to 'llap' queue, and
    # (2). 'hive.llap.daemon.queue.name' property_attributes set to : default and llap.
    self.expected_hive_interactive_site_llap = {
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


    # Expected 'hive_interactive_site' with 'hive.llap.daemon.queue.name' property_attributes set to : 'a1', 'b' and llap.
    self.expected_hive_interactive_site_prop_attr_as_a1_b_llap = {
      "hive-interactive-site": {
        "property_attributes": {
          "hive.llap.daemon.queue.name": {
            "entries": [
              {
                "value": "a1",
                "label": "a1"
              },
              {
                "value": "b",
                "label": "b"
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
    self.expected_hive_interactive_site_default = {
      "hive-interactive-site": {
        "properties": {
          "hive.server2.tez.default.queues": "default",
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
    self.expected_hive_interactive_site_empty = {
      "hive-interactive-site": {
        "properties": {
        }
      }
    }

    # Expected 'hive_interactive_site' when no modifications are done.
    self.expected_hive_interactive_env_empty = {
      "hive-interactive-env": {
        "properties": {
        }
      }
    }

    self.expected_hive_interactive_site_only_memory = {
      "hive-interactive-site": {
        "properties": {
          'hive.llap.daemon.yarn.container.mb': '341'
        }
      }
    }

    # Expected 'hive_interactive_env' with 'llap_queue_capacity' set to 20.
    self.expected_llap_queue_capacity_20 = '20'

    # Expected 'hive_interactive_env' with 'llap_queue_capacity' set to 40.
    self.expected_llap_queue_capacity_40 = '40'


    # expected vals.
    self.expected_visibility_false = {'visible': 'false'}
    self.expected_visibility_true = {'visible': 'true'}

  def test_recommendSPARKConfigurations_SecurityEnabledZeppelinInstalled(self):
    configurations = {
      "cluster-env": {
        "properties": {
          "security_enabled": "true",
        }
      },
      "livy-conf": {
        "properties": {
          "livy.property1": "value1"
        }
      },
      "zeppelin-env": {
        "properties": {
          "zeppelin.server.kerberos.principal": "zeppelin_user@REALM"
        }
      }
    }
    services = {"configurations": configurations}
    services['services'] = [
      {
        "StackServices": {
          "service_name": "SPARK"
        },
      }
    ]
    clusterData = {
      "cpu": 4,
      "containers": 5,
      "ramPerContainer": 256,
      "yarnMinContainerSize": 256
    }
    expected = {
      "cluster-env": {
        "properties": {
          "security_enabled": "true",
        }
      },
      "livy-conf": {
        "properties": {
          "livy.superusers": "zeppelin_user",
          "livy.property1": "value1"
        }
      },
      "zeppelin-env": {
        "properties": {
          "zeppelin.server.kerberos.principal": "zeppelin_user@REALM"
        }
      }
    }

    self.stackAdvisor.recommendSparkConfigurations(configurations, clusterData, services, None)
    self.assertEquals(configurations, expected)

  def test_recommendSPARKConfigurations_SecurityNotEnabledZeppelinInstalled(self):
    configurations = {
      "cluster-env": {
        "properties": {
          "security_enabled": "false",
        }
      },
      "livy-conf": {
        "properties": {
        }
      },
      "zeppelin-env": {
        "properties": {
        }
      }
    }
    services = {"configurations": configurations}
    services['services'] = [
      {
        "StackServices": {
          "service_name": "SPARK"
        },
      }
    ]
    clusterData = {
      "cpu": 4,
      "containers": 5,
      "ramPerContainer": 256,
      "yarnMinContainerSize": 256
    }
    expected = {
      "cluster-env": {
        "properties": {
          "security_enabled": "false",
        }
      },
      "livy-conf": {
        "properties": {
        }
      },
      "zeppelin-env": {
        "properties": {
        }
      }
    }

    self.stackAdvisor.recommendSparkConfigurations(configurations, clusterData, services, None)
    self.assertEquals(configurations, expected)

  def test_recommendSPARKConfigurations_SecurityEnabledZeppelinInstalledExistingValue(self):
    configurations = {
      "cluster-env": {
        "properties": {
          "security_enabled": "true",
        }
      },
      "livy-conf": {
        "properties": {
          "livy.superusers": "livy_user"
        }
      },
      "zeppelin-env": {
        "properties": {
          "zeppelin.server.kerberos.principal": "zeppelin_user@REALM"
        }
      }
    }
    services = {"configurations": configurations}
    services['services'] = [
      {
        "StackServices": {
          "service_name": "SPARK"
        },
      }
    ]
    clusterData = {
      "cpu": 4,
      "containers": 5,
      "ramPerContainer": 256,
      "yarnMinContainerSize": 256
    }
    expected = {
      "cluster-env": {
        "properties": {
          "security_enabled": "true",
        }
      },
      "livy-conf": {
        "properties": {
          "livy.superusers": "livy_user,zeppelin_user"
        }
      },
      "zeppelin-env": {
        "properties": {
          "zeppelin.server.kerberos.principal": "zeppelin_user@REALM"
        }
      }
    }

    self.stackAdvisor.recommendSparkConfigurations(configurations, clusterData, services, None)
    self.assertEquals(configurations, expected)

  def test_recommendSPARKConfigurations_SecurityEnabledZeppelinNotInstalled(self):
    configurations = {
      "cluster-env": {
        "properties": {
          "security_enabled": "true",
        }
      },
      "livy-conf": {
        "properties": {
        }
      }
    }
    services = {"configurations": configurations}
    services['services'] = [
      {
        "StackServices": {
          "service_name": "SPARK"
        },
      }
    ]
    clusterData = {
      "cpu": 4,
      "containers": 5,
      "ramPerContainer": 256,
      "yarnMinContainerSize": 256
    }
    expected = {
      "cluster-env": {
        "properties": {
          "security_enabled": "true",
        }
      },
      "livy-conf": {
        "properties": {
        }
      }
    }

    self.stackAdvisor.recommendSparkConfigurations(configurations, clusterData, services, None)
    self.assertEquals(configurations, expected)

  def test_recommendSPARK2Configurations(self):
    configurations = {}
    services = {"configurations": configurations}
    services['services'] = [
      {
        "StackServices": {
          "service_name": "SPARK2"
        },
      }
    ]
    clusterData = {
      "cpu": 4,
      "containers": 5,
      "ramPerContainer": 256,
      "yarnMinContainerSize": 256
    }
    expected = {
      "spark2-defaults": {
        "properties": {
          "spark.yarn.queue": "default"
        }
      },
      "spark2-thrift-sparkconf": {
        "properties": {
          "spark.yarn.queue": "default"
        }
      }
    }

    self.stackAdvisor.recommendSpark2Configurations(configurations, clusterData, services, None)
    self.assertEquals(configurations, expected)

  def load_json(self, filename):
    file = os.path.join(self.testDirectory, filename)
    with open(file, 'rb') as f:
      data = json.load(f)
    return data

  def prepareNHosts(self, host_count):
    names = []
    for i in range(0, host_count):
      names.append("hostname" + str(i))
    return self.prepareHosts(names)

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

  def test_getCardinalitiesDict(self):
    hosts = self.prepareNHosts(5)
    actual = self.stackAdvisor.getCardinalitiesDict(hosts)
    expected = {'ZOOKEEPER_SERVER': {'min': 3}, 'HBASE_MASTER': {'min': 1}, 'METRICS_COLLECTOR': {'min': 1}}
    self.assertEquals(actual, expected)

    hosts = self.prepareNHosts(1001)
    actual = self.stackAdvisor.getCardinalitiesDict(hosts)
    expected = {'ZOOKEEPER_SERVER': {'min': 3}, 'HBASE_MASTER': {'min': 1}, 'METRICS_COLLECTOR': {'min': 2}}
    self.assertEquals(actual, expected)

  def test_getComponentLayoutValidations_one_hsi_host(self):

    hosts = self.load_json("host-3-hosts.json")
    services = self.load_json("services-normal-his-2-hosts.json")

    validations = self.stackAdvisor.getComponentLayoutValidations(services, hosts)
    expected = {'component-name': 'HIVE_SERVER_INTERACTIVE',
                'message': 'You have selected 2 HiveServer2 Interactive components. Please consider that between 0 and 1 HiveServer2 Interactive components should be installed in cluster.',
                'type': 'host-component',
                'level': 'ERROR'}
    self.assertEquals(validations[0], expected)


  def test_validateYARNConfigurations(self):
    properties = {'enable_hive_interactive': 'true',
                  'hive.tez.container.size': '2048', "yarn.nodemanager.linux-container-executor.group": "hadoop"}
    recommendedDefaults = {'enable_hive_interactive': 'true',
                           "yarn.nodemanager.linux-container-executor.group": "hadoop"}
    configurations = {
      "hive-interactive-env": {
        "properties": {'enable_hive_interactive': 'true'}
      },
      "hive-site": {
        "properties": {"hive.security.authorization.enabled": "true", 'hive.tez.java.opts': '-server -Djava.net.preferIPv4Stack=true'}
      },
      "hive-env": {
        "properties": {"hive_security_authorization": "None"}
      },
      "yarn-site": {
        "properties": {"yarn.resourcemanager.work-preserving-recovery.enabled": "false"}
      },
      "cluster-env": {
        "properties": {
          "user_group": "hadoop",
        }
      }
    }
    services = self.load_json("services-normal-his-valid.json")

    res_expected = [
      {'config-type': 'yarn-site', 'message': 'While enabling HIVE_SERVER_INTERACTIVE it is recommended that you enable work preserving restart in YARN.', 'type': 'configuration', 'config-name': 'yarn.resourcemanager.work-preserving-recovery.enabled', 'level': 'WARN'}
    ]
    res = self.stackAdvisor.validateYARNConfigurations(properties, recommendedDefaults, configurations, services, {})
    self.assertEquals(res, res_expected)
    pass

  def test_validateHiveInteractiveEnvConfigurations(self):
    properties = {'enable_hive_interactive': 'true',
                  'hive.tez.container.size': '2048'}
    recommendedDefaults = {'enable_hive_interactive': 'true'}
    configurations = {
      "hive-interactive-env": {
        "properties": {'enable_hive_interactive': 'true'}
      },
      "hive-site": {
        "properties": {"hive.security.authorization.enabled": "true", 'hive.tez.java.opts': '-server -Djava.net.preferIPv4Stack=true'}
      },
      "hive-env": {
        "properties": {"hive_security_authorization": "None"}
      },
      "yarn-site": {
        "properties": {"yarn.resourcemanager.work-preserving-recovery.enabled": "true",
                       "yarn.resourcemanager.scheduler.monitor.enable": "false"}
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
        "properties": {"yarn.resourcemanager.work-preserving-recovery.enabled": "true",
                       "yarn.resourcemanager.scheduler.monitor.enable": "true"}
      }
    }
    services = self.load_json("services-normal-his-valid.json")

    # Checks for WARN message that 'yarn.resourcemanager.scheduler.monitor.enable' should be true.
    res_expected = [
      {'config-type': 'hive-interactive-env', 'message': "When enabling LLAP, set 'yarn.resourcemanager.scheduler.monitor.enable' to true to ensure that LLAP gets the full allocated capacity.", 'type': 'configuration', 'config-name': 'enable_hive_interactive', 'level': 'WARN'}
    ]
    # the above error is not what we are checking for - just to keep test happy without having to test
    res = self.stackAdvisor.validateHiveInteractiveEnvConfigurations(properties, recommendedDefaults, configurations, services, {})
    self.assertEquals(res, res_expected)

    # (1). Checks for ERROR message for 'enable_hive_interactive' to be true.
    # (2). Further, no message regarding 'yarn.resourcemanager.scheduler.monitor.enable' as it is true already.
    res_expected = [
      {'config-type': 'hive-interactive-env', 'message': 'HIVE_SERVER_INTERACTIVE requires enable_hive_interactive in hive-interactive-env set to true.', 'type': 'configuration', 'config-name': 'enable_hive_interactive', 'level': 'ERROR'}
    ]
    res = self.stackAdvisor.validateHiveInteractiveEnvConfigurations(properties, recommendedDefaults, configurations2, services, {})
    self.assertEquals(res, res_expected)
    pass


  """
  Tests validations for Hive Server Interactive site.
  """
  def test_validateHiveInteractiveSiteConfigurations(self):
    # Performing setup

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
            "public_host_name": "c6402.ambari.apache.org",
            "host_name": "c6402.ambari.apache.org"
          },
        }
      ]
    }


    # Test A : When selected queue capacity is < than the minimum required for LLAP app to run
    # Expected : Error telling about the current size compared to minimum required size.
    services1 = self.load_json("services-normal-his-valid.json")
    res_expected1 = [
      {'config-type': 'hive-interactive-site', 'message': "Selected queue 'llap' capacity (49.0%) is less than minimum required "
        "capacity (50%) for LLAP app to run", 'type': 'configuration', 'config-name': 'hive.llap.daemon.queue.name', 'level': 'ERROR'},
    ]
    res1 = self.stackAdvisor.validateHiveInteractiveSiteConfigurations({}, {}, {}, services1, hosts)
    self.assertEquals(res1, res_expected1)



    # Test B : When selected queue capacity is < than the minimum required for LLAP app to run
    # and selected queue current state is "STOPPED".
    # Expected : 1. Error telling about the current size compared to minimum required size.
    #            2. Error telling about current state can't be STOPPED. Expected : RUNNING.
    #            3. Error telling about config 'hive.server2.enable.doAs' to be false at all times.
    #            4. Error telling about config 'hive.server2.tez.sessions.per.default.queue' that its consuming more
    #               than 50% of queue capacity for LLAP.
    services2 = self.load_json("services-normal-his-2-hosts.json")
    res_expected2 = [
      {'config-type': 'hive-interactive-site', 'message': "Selected queue 'llap' capacity (49.0%) is less than minimum required "
                                                          "capacity (50%) for LLAP app to run", 'type': 'configuration', 'config-name': 'hive.llap.daemon.queue.name', 'level': 'ERROR'},
      {'config-type': 'hive-interactive-site', 'message': "Selected queue 'llap' current state is : 'STOPPED'. It is required to be in "
                                                          "'RUNNING' state for LLAP to run", 'type': 'configuration', 'config-name': 'hive.llap.daemon.queue.name', 'level': 'ERROR'},
      {'config-type': 'hive-interactive-site', 'message': "Value should be set to 'false' for Hive2.", 'type': 'configuration', 'config-name': 'hive.server2.enable.doAs', 'level': 'ERROR'},
      {'config-type': 'hive-interactive-site', 'message': " Reducing the 'Maximum Total Concurrent Queries' (value: 32) is advisable as it is consuming more than 50% of 'llap' queue for "
                                                          "LLAP.", 'type': 'configuration', 'config-name': 'hive.server2.tez.sessions.per.default.queue', 'level': 'WARN'}
    ]
    res2 = self.stackAdvisor.validateHiveInteractiveSiteConfigurations({}, {}, {}, services2, hosts)
    self.assertEquals(res2, res_expected2)


    # Test C : When selected queue capacity is >= the minimum required for LLAP app to run, using the passed in configurations.
    # Expected : No error.
    configurations = {
      "yarn-site": {
        "properties": {
          "yarn.nodemanager.resource.memory-mb":"10240",
          "yarn.scheduler.minimum-allocation-mb":"341"
        }
      },
      "hive-site": {
        "properties": {
          "hive.tez.container.size" : "341",
          "tez.am.resource.memory.mb" : "341"
        }
      },
      "tez-site": {
        "properties": {
          "tez.am.resource.memory.mb" : "341"
        }
      }
    }
    res_expected3 = []
    res3 = self.stackAdvisor.validateHiveInteractiveSiteConfigurations({}, {}, configurations, services1, hosts)
    self.assertEquals(res3, res_expected3)


    # Test D : When remaining available capacity is less than 512M (designated for running service checks), using the passed in configurations.
    # Expected : WARN error as 'Service checks may not run as remaining available capacity is less than 512M'.
    # With current configs passed in, also getting selected queue capacity is < than the minimum required for LLAP app to run.
    configurations = {
      "yarn-site": {
        "properties": {
          "yarn.nodemanager.resource.memory-mb":"512",
          "yarn.scheduler.minimum-allocation-mb":"341"
        }
      },
      "hive-site": {
        "properties": {
          "hive.tez.container.size" : "341",
          "tez.am.resource.memory.mb" : "341"
        }
      },
      "tez-site": {
        "properties": {
          "tez.am.resource.memory.mb" : "341"
        }
      },
    }
    res_expected4 = [
      {'config-type': 'hive-interactive-site', 'message': "Selected queue 'llap' capacity (49.0%) is less than minimum required capacity (200%) for LLAP app to run",
       'type': 'configuration', 'config-name': 'hive.llap.daemon.queue.name', 'level': 'ERROR'},
      {'config-type': 'hive-interactive-site', 'message': "Capacity used by 'llap' queue is '250.88'. Service checks may not run as remaining available capacity "
                                                          "(261.12) in cluster is less than 512 MB.", 'type': 'configuration', 'config-name': 'hive.llap.daemon.queue.name', 'level': 'WARN'}]
    res4 = self.stackAdvisor.validateHiveInteractiveSiteConfigurations({}, {}, configurations, services1, hosts)

    self.assertEquals(res4, res_expected4)
    pass




  # Test 1 : (1). 'default' and 'llap' (State : RUNNING) queue exists at root level in capacity-scheduler and
  #         'capacity-scheduler' configs are passed-in as single "/n" separated string , and
  #         (2). enable_hive_interactive' is 'OFF' and (3). configuration change detected for 'enable_hive_interactive'
  #         Expected : 'llap' queue state becomes STOPPED, 'default' becomes 100%.
  def test_recommendYARNConfigurations_llap_configs_not_updated_1(self):

    services = {
      "services": [{
                     "StackServices": {
                       "service_name": "TEZ"
                     }
                   },
                   {
                     "StackServices": {
                       "service_name": "SPARK"
                     }
                   },
                   {
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
        "cluster-env": {
          "properties": {
            "stack_root": "{\"HDP\":\"/usr/hdp\"}",
            "stack_name": "HDP"
          },
        },
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
                                  'yarn.scheduler.capacity.root.ordering-policy=priority-utilization\n'
                                  'yarn.scheduler.capacity.root.llap.user-limit-factor=1\n'
                                  'yarn.scheduler.capacity.root.llap.state=RUNNING\n'
                                  'yarn.scheduler.capacity.root.llap.ordering-policy=fifo\n'
                                  'yarn.scheduler.capacity.root.llap.priority=10\n'
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
              'num_llap_nodes': '1'
            }
          },
        "hive-interactive-site":
          {
            'properties': {
              'hive.llap.daemon.queue.name': 'llap',
              'hive.server2.tez.sessions.per.default.queue': '1',
              "hive.tez.container.size": "341"
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
      },
      "ambari-server-properties": {"ambari-server.user":"ambari_user"}
    }

    clusterData = {
      "cpu": 4,
      "mapMemory": 30000,
      "amMemory": 20000,
      "reduceMemory": 20560,
      "containers": 30,
      "ramPerContainer": 512,
      "referenceNodeManagerHost" : {
        "total_mem" : 10240 * 1024
      },
      "yarnMinContainerSize": 512
    }


    configurations = {
    }

    self.stackAdvisor.recommendYARNConfigurations(configurations, clusterData, services, self.hosts)

    # Outputs

    cap_sched_output_dict = convertToDict(configurations['capacity-scheduler']['properties']['capacity-scheduler'])
    cap_sched_expected_dict = convertToDict(self.expected_capacity_scheduler_llap_Stopped_size_0['properties']['capacity-scheduler'])
    self.assertEqual(cap_sched_output_dict, cap_sched_expected_dict)

    self.assertEquals(configurations['hive-interactive-site']['properties']['hive.server2.tez.default.queues'], 'default')
    self.assertEquals(configurations['hive-interactive-site']['properties']['hive.llap.daemon.queue.name'], 'default')
    self.assertEquals(configurations['yarn-site']['properties']['yarn.timeline-service.entity-group-fs-store.group-id-plugin-classes'],
                      'org.apache.tez.dag.history.logging.ats.TimelineCachePluginImpl,org.apache.spark.deploy.history.yarn.plugin.SparkATSPlugin')
    self.assertEquals(configurations['yarn-site']['properties']['yarn.timeline-service.entity-group-fs-store.group-id-plugin-classpath'], '/usr/hdp/{{spark_version}}/spark/hdpLib/*')
    self.assertTrue('hive-interactive-env' not in configurations)
    self.assertTrue('property_attributes' not in configurations)




  # Test 2 : (1). 'default' and 'llap' (State : RUNNING) queue exists at root level in capacity-scheduler, and
  #         'capacity-scheduler' configs are passed-in as single "/n" separated string  and
  #         (2). enable_hive_interactive' is 'OFF' and (3). configuration change NOT detected for 'enable_hive_interactive'
  #         Expected : No changes.
  def test_recommendYARNConfigurations_llap_configs_not_updated_2(self):

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
      "changed-configurations": [
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
                                  'yarn.scheduler.capacity.root.llap.priority=10\n'
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

    clusterData = {
      "cpu": 4,
      "mapMemory": 30000,
      "amMemory": 20000,
      "reduceMemory": 20560,
      "containers": 30,
      "ramPerContainer": 512,
      "referenceNodeManagerHost" : {
        "total_mem" : 10240 * 1024
      },
      "yarnMinContainerSize": 512
    }

    configurations = {
    }
    self.stackAdvisor.recommendYARNConfigurations(configurations, clusterData, services_18, self.hosts)

    self.assertEquals(configurations['capacity-scheduler']['properties'],
                      self.expected_capacity_scheduler_empty['properties'])
    self.assertEquals(configurations['hive-interactive-site']['properties'],
                      self.expected_hive_interactive_site_empty['hive-interactive-site']['properties'])
    self.assertTrue('hive-interactive-env' not in configurations)

    self.assertTrue('property_attributes' not in configurations)



  ####################### 'One Node Manager' cluster - tests for calculating llap configs ################


  # Test 3: (1). 'default' and 'llap' (State : RUNNING) queue exists at root level in capacity-scheduler and
  #         'capacity-scheduler' configs are passed-in as single "/n" separated string , and
  #         (2). enable_hive_interactive' is 'on' and (3). configuration change detected for 'num_llap_nodes'
  #         Expected : Configurations values recommended for llap related configs.
  def test_recommendYARNConfigurations_one_node_manager_llap_configs_updated_1(self):

    services = {
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
                                  'yarn.scheduler.capacity.root.default.capacity=80\n'
                                  'yarn.scheduler.capacity.root.acl_administer_queue=*\n'
                                  'yarn.scheduler.capacity.node-locality-delay=40\n'
                                  'yarn.scheduler.capacity.queue-mappings-override.enable=false\n'
                                  'yarn.scheduler.capacity.root.ordering-policy=priority-utilization\n'
                                  'yarn.scheduler.capacity.root.llap.user-limit-factor=1\n'
                                  'yarn.scheduler.capacity.root.llap.state=RUNNING\n'
                                  'yarn.scheduler.capacity.root.llap.ordering-policy=fifo\n'
                                  'yarn.scheduler.capacity.root.llap.priority=10\n'
                                  'yarn.scheduler.capacity.root.llap.minimum-user-limit-percent=100\n'
                                  'yarn.scheduler.capacity.root.llap.maximum-capacity=20\n'
                                  'yarn.scheduler.capacity.root.llap.capacity=20\n'
                                  'yarn.scheduler.capacity.root.llap.acl_submit_applications=hive\n'
                                  'yarn.scheduler.capacity.root.llap.acl_administer_queue=hive\n'
                                  'yarn.scheduler.capacity.root.llap.maximum-am-resource-percent=1'

          }
        },
        "hive-interactive-env":
          {
            'properties': {
              'enable_hive_interactive': 'true',
              'num_llap_nodes':'1',
              'num_llap_nodes_for_llap_daemons': '1'
            }
          },
        "hive-interactive-site":
          {
            'properties': {
              'hive.llap.daemon.queue.name': 'llap',
              'hive.server2.tez.sessions.per.default.queue': '1',
              'hive.tez.container.size':'1024'
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
            "yarn.scheduler.minimum-allocation-mb": "512",
            "yarn.nodemanager.resource.memory-mb": "10240",
            "yarn.nodemanager.resource.cpu-vcores": '1'
          }
        },
        "tez-interactive-site": {
          "properties": {
            "tez.am.resource.memory.mb": "512"
          }
        },
        "hive-site":
          {
            'properties': {
              'hive.tez.container.size': '512'
            }
          },
      }
    }


    clusterData = {
      "cpu": 4,
      "mapMemory": 30000,
      "amMemory": 20000,
      "reduceMemory": 20560,
      "containers": 30,
      "ramPerContainer": 512,
      "referenceNodeManagerHost" : {
        "total_mem" : 10240 * 1024
      },
      "yarnMinContainerSize": 512
    }



    configurations = {
    }
    self.stackAdvisor.recommendYARNConfigurations(configurations, clusterData, services, self.hosts)

    self.assertEqual(configurations['hive-interactive-site']['properties']['hive.server2.tez.sessions.per.default.queue'], '1')
    self.assertEquals(configurations['hive-interactive-site']['property_attributes']['hive.server2.tez.sessions.per.default.queue'], {'minimum': '1', 'maximum': '4'})

    self.assertTrue(configurations['hive-interactive-env']['properties']['num_llap_nodes_for_llap_daemons'], 3)
    self.assertTrue('num_llap_nodes' not in configurations['hive-interactive-env']['properties'])

    self.assertEqual(configurations['hive-interactive-site']['properties']['hive.llap.daemon.yarn.container.mb'], '9216')

    self.assertEqual(configurations['hive-interactive-site']['properties']['hive.llap.daemon.num.executors'], '1')
    self.assertEqual(configurations['hive-interactive-site']['properties']['hive.llap.io.threadpool.size'], '1')

    self.assertEqual(configurations['hive-interactive-site']['properties']['hive.llap.io.memory.size'], '8192')
    self.assertEqual(configurations['hive-interactive-site']['properties']['hive.llap.io.enabled'], 'true')

    self.assertEqual(configurations['hive-interactive-env']['properties']['llap_heap_size'], '819')
    self.assertEqual(configurations['hive-interactive-env']['properties']['hive_heapsize'], '2048')
    self.assertEqual(configurations['hive-interactive-env']['property_attributes']['num_llap_nodes'], {'read_only': 'false', 'minimum': '1', 'maximum': '1'})

    self.assertEqual(configurations['hive-interactive-env']['properties']['slider_am_container_mb'], '512')
    self.assertEqual(configurations['hive-interactive-site']['properties']['hive.server2.tez.default.queues'], 'llap')
    self.assertEqual(configurations['hive-interactive-site']['properties']['hive.auto.convert.join.noconditionaltask.size'], '286261248')

    self.assertTrue('tez.am.resource.memory.mb' not in configurations['tez-interactive-site']['properties'])
    self.assertEquals(configurations['hive-interactive-site']['property_attributes']['hive.llap.daemon.queue.name'], {'entries': [{'value': 'default', 'label': 'default'}, {'value': 'llap', 'label': 'llap'}]})



  # Test 4: (1). 'default' and 'llap' (State : RUNNING) queue exists at root level in capacity-scheduler, and
  #         'capacity-scheduler' configs are passed-in as single "/n" separated string  and
  #         (2). enable_hive_interactive' is 'on' and (3). configuration change detected for 'hive.server2.tez.sessions.per.default.queue'
  #         (3). Selected queue in 'hive.llap.daemon.queue.name' is 'default'.
  #         Expected : Configurations values recommended for llap related configs.
  def test_recommendYARNConfigurations_one_node_manager_llap_configs_updated_2(self):

    # Services 16: YARN service with : (1). 'capacity scheduler' having 'llap' and 'default' queue at root level and
    # (2). 'enable_hive_interactive' is ON and (3). configuration change detected for 'enable_hive_interactive'
    services = {
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
          u'type': u'hive-interactive-site',
          u'name': u'hive.server2.tez.sessions.per.default.queue'
        }
      ],
      "configurations": {
        "capacity-scheduler": {
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
                                  'yarn.scheduler.capacity.root.ordering-policy=priority-utilization\n'
                                  'yarn.scheduler.capacity.root.llap.user-limit-factor=1\n'
                                  'yarn.scheduler.capacity.root.llap.state=RUNNING\n'
                                  'yarn.scheduler.capacity.root.llap.ordering-policy=fifo\n'
                                  'yarn.scheduler.capacity.root.llap.priority=10\n'
                                  'yarn.scheduler.capacity.root.llap.minimum-user-limit-percent=100\n'
                                  'yarn.scheduler.capacity.root.llap.maximum-capacity=0\n'
                                  'yarn.scheduler.capacity.root.llap.capacity=0\n'
                                  'yarn.scheduler.capacity.root.llap.acl_submit_applications=hive\n'
                                  'yarn.scheduler.capacity.root.llap.acl_administer_queue=hive\n'
                                  'yarn.scheduler.capacity.root.llap.maximum-am-resource-percent=1'

          }
        },
        "hive-interactive-env":
          {
            'properties': {
              'enable_hive_interactive': 'true',
              'num_llap_nodes':'1',
              'num_llap_nodes_for_llap_daemons': '1'
            }
          },
        "hive-interactive-site":
          {
            'properties': {
              'hive.llap.daemon.queue.name': 'default',
              'hive.server2.tez.sessions.per.default.queue': '1',
              'hive.tez.container.size':'1024'
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
            "yarn.nodemanager.resource.memory-mb": "10240",
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


    clusterData = {
      "cpu": 4,
      "mapMemory": 30000,
      "amMemory": 20000,
      "reduceMemory": 20560,
      "containers": 30,
      "ramPerContainer": 512,
      "referenceNodeManagerHost" : {
        "total_mem" : 10240 * 2048
      },
      "yarnMinContainerSize": 512
    }


    configurations = {
    }

    self.stackAdvisor.recommendYARNConfigurations(configurations, clusterData, services, self.hosts)

    self.assertEquals(configurations['hive-interactive-site']['property_attributes']['hive.server2.tez.sessions.per.default.queue'], {'maximum': '3'})

    self.assertTrue(configurations['hive-interactive-env']['properties']['num_llap_nodes'], 3)
    self.assertTrue(configurations['hive-interactive-env']['properties']['num_llap_nodes_for_llap_daemons'], 3)

    self.assertEqual(configurations['hive-interactive-site']['properties']['hive.llap.daemon.yarn.container.mb'], '9207')

    self.assertEqual(configurations['hive-interactive-site']['properties']['hive.llap.daemon.num.executors'], '1')
    self.assertEqual(configurations['hive-interactive-site']['properties']['hive.llap.io.threadpool.size'], '1')

    self.assertEqual(configurations['hive-interactive-site']['properties']['hive.llap.io.memory.size'], '8183')
    self.assertEqual(configurations['hive-interactive-site']['properties']['hive.llap.io.enabled'], 'true')

    self.assertEqual(configurations['hive-interactive-env']['properties']['llap_heap_size'], '819')
    self.assertEqual(configurations['hive-interactive-env']['properties']['hive_heapsize'], '2048')
    self.assertEqual(configurations['hive-interactive-env']['property_attributes']['num_llap_nodes'], {'read_only': 'true', 'minimum': '1', 'maximum': '1'})

    self.assertEqual(configurations['hive-interactive-env']['properties']['slider_am_container_mb'], '682')
    self.assertEqual(configurations['hive-interactive-site']['properties']['hive.auto.convert.join.noconditionaltask.size'], '286261248')

    self.assertTrue('tez.am.resource.memory.mb' not in configurations['tez-interactive-site']['properties'])
    self.assertEquals(configurations['hive-interactive-site']['property_attributes']['hive.llap.daemon.queue.name'], {'entries': [{'value': 'default', 'label': 'default'}, {'value': 'llap', 'label': 'llap'}]})



  # Test 5: (1). 'default' and 'llap' (State : RUNNING) queue exists at root level in capacity-scheduler, and
  #         'capacity-scheduler' configs are passed-in as single "/n" separated string  and
  #         (2). enable_hive_interactive' is 'on' and (3). configuration change detected for 'hive.server2.tez.sessions.per.default.queue'
  #         Expected : Configurations values recommended for llap related configs.
  def test_recommendYARNConfigurations_one_node_manager_llap_configs_updated_3(self):
    services = {
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
          u'old_value': u'1',
          u'type': u'hive-interactive-site',
          u'name': u'hive.server2.tez.sessions.per.default.queue'
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
                                  'yarn.scheduler.capacity.root.ordering-policy=priority-utilization\n'
                                  'yarn.scheduler.capacity.root.llap.user-limit-factor=1\n'
                                  'yarn.scheduler.capacity.root.llap.state=RUNNING\n'
                                  'yarn.scheduler.capacity.root.llap.ordering-policy=fifo\n'
                                  'yarn.scheduler.capacity.root.llap.priority=10\n'
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
              'num_llap_nodes': 1,
              'num_llap_nodes_for_llap_daemons': '1'
            }
          },
        "hive-interactive-site":
          {
            'properties': {
              'hive.llap.daemon.queue.name': 'llap',
              'hive.server2.tez.sessions.per.default.queue': '2',
              'hive.tez.container.size':'2048'
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
            "yarn.scheduler.minimum-allocation-mb": "1024",
            "yarn.nodemanager.resource.memory-mb": "51200",
            "yarn.nodemanager.resource.cpu-vcores": '1'
          }
        },
        "tez-interactive-site": {
          "properties": {
            "tez.am.resource.memory.mb": "1024"
          }
        },
        "hive-site":
          {
            'properties': {
              'hive.tez.container.size': '1024'
            }
          },
      }
    }



    clusterData = {
      "cpu": 4,
      "mapMemory": 30000,
      "amMemory": 20000,
      "reduceMemory": 20560,
      "containers": 30,
      "ramPerContainer": 1024,
      "referenceNodeManagerHost" : {
        "total_mem" : 51200 * 1024
      },
      "yarnMinContainerSize": 1024
    }

    configurations = {
    }


    self.stackAdvisor.recommendYARNConfigurations(configurations, clusterData, services, self.hosts)

    self.assertEquals(configurations['hive-interactive-site']['property_attributes']['hive.server2.tez.sessions.per.default.queue'], {'maximum': '4'})

    self.assertTrue(configurations['hive-interactive-env']['properties']['num_llap_nodes_for_llap_daemons'], 3)
    self.assertTrue('num_llap_nodes' not in configurations['hive-interactive-env']['properties'])

    self.assertEqual(configurations['hive-interactive-site']['properties']['hive.llap.daemon.yarn.container.mb'], '48128')

    self.assertEqual(configurations['hive-interactive-site']['properties']['hive.llap.daemon.num.executors'], '1')
    self.assertEqual(configurations['hive-interactive-site']['properties']['hive.llap.io.threadpool.size'], '1')

    self.assertEqual(configurations['hive-interactive-site']['properties']['hive.llap.io.memory.size'], '46080')
    self.assertEqual(configurations['hive-interactive-site']['properties']['hive.llap.io.enabled'], 'true')

    self.assertEqual(configurations['hive-interactive-env']['properties']['llap_heap_size'], '1638')
    self.assertEqual(configurations['hive-interactive-env']['properties']['hive_heapsize'], '2048')
    self.assertEqual(configurations['hive-interactive-env']['property_attributes']['num_llap_nodes'], {'read_only': 'false', 'minimum': '1', 'maximum': '1'})

    self.assertEqual(configurations['hive-interactive-env']['properties']['slider_am_container_mb'], '1024')
    self.assertEqual(configurations['hive-interactive-site']['properties']['hive.server2.tez.default.queues'], 'llap')
    self.assertEqual(configurations['hive-interactive-site']['properties']['hive.auto.convert.join.noconditionaltask.size'], '572522496')

    self.assertTrue('tez.am.resource.memory.mb' not in configurations['tez-interactive-site']['properties'])
    self.assertEquals(configurations['hive-interactive-site']['property_attributes']['hive.llap.daemon.queue.name'], {'entries': [{'value': 'default', 'label': 'default'}, {'value': 'llap', 'label': 'llap'}]})




  ####################### 'Three Node Managers' cluster - tests for calculating llap configs ################


  # Test 6: (1). 'default' and 'llap' (State : RUNNING) queue exists at root level in capacity-scheduler, and
  #          'capacity-scheduler' configs are passed-in as single "/n" separated string  and
  #          (2). enable_hive_interactive' is 'on' and (3). configuration change detected for 'num_llap_nodes'
  #         Expected : Configurations values recommended for llap related configs.
  def test_recommendYARNConfigurations_three_node_manager_llap_configs_updated_1(self):
    # Services 20: YARN service with : (1). 'capacity scheduler' having 'llap' and 'default' queue at root level and
    # (2). 'enable_hive_interactive' is ON and (3). configuration change detected for 'llap_queue_capacity'
    # 3 node managers and yarn.nodemanager.resource.memory-mb": "40960"
    services = {
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
              "hostnames": ["c6401.ambari.apache.org", "c6402.ambari.apache.org", "c6403.ambari.apache.org"]
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
                "c6401.ambari.apache.org"
              ]
            },
            "dependencies": []
          },
        ]
      }
      ],
      "changed-configurations": [
        {
          u'old_value': u'1',
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
                                  'yarn.scheduler.capacity.root.ordering-policy=priority-utilization\n'
                                  'yarn.scheduler.capacity.root.llap.user-limit-factor=1\n'
                                  'yarn.scheduler.capacity.root.llap.state=RUNNING\n'
                                  'yarn.scheduler.capacity.root.llap.ordering-policy=fifo\n'
                                  'yarn.scheduler.capacity.root.llap.priority=10\n'
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
              'num_llap_nodes':'3',
              'num_llap_nodes_for_llap_daemons': '1'
            }
          },
        "hive-interactive-site":
          {
            'properties': {
              'hive.llap.daemon.queue.name': 'llap',
              'hive.server2.tez.sessions.per.default.queue': '1',
              'hive.tez.container.size':'2048'
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
            "yarn.scheduler.minimum-allocation-mb": "2048",
            "yarn.nodemanager.resource.memory-mb": "40960",
            "yarn.nodemanager.resource.cpu-vcores": '4'
          }
        },
        "tez-interactive-site": {
          "properties": {
            "tez.am.resource.memory.mb": "1024"
          }
        },
        "hive-site":
          {
            'properties': {
              'hive.tez.container.size': '1024'
            }
          },
      }
    }



    clusterData = {
      "cpu": 4,
      "mapMemory": 30000,
      "amMemory": 20000,
      "reduceMemory": 20560,
      "containers": 30,
      "ramPerContainer": 2048,
      "referenceNodeManagerHost" : {
        "total_mem" : 40960 * 1024
      },
      "yarnMinContainerSize": 1024
    }

    configurations = {
    }
    self.stackAdvisor.recommendYARNConfigurations(configurations, clusterData, services, self.hosts)


    self.assertEqual(configurations['hive-interactive-site']['properties']['hive.server2.tez.sessions.per.default.queue'], '1')
    self.assertEquals(configurations['hive-interactive-site']['property_attributes']['hive.server2.tez.sessions.per.default.queue'], {'maximum': '4', 'minimum': '1'})

    self.assertTrue(configurations['hive-interactive-env']['properties']['num_llap_nodes_for_llap_daemons'], 3)
    self.assertTrue('num_llap_nodes' not in configurations['hive-interactive-env']['properties'])

    self.assertEqual(configurations['hive-interactive-site']['properties']['hive.llap.daemon.yarn.container.mb'], '38912')

    self.assertEqual(configurations['hive-interactive-site']['properties']['hive.llap.daemon.num.executors'], '4')
    self.assertEqual(configurations['hive-interactive-site']['properties']['hive.llap.io.threadpool.size'], '4')

    self.assertEqual(configurations['hive-interactive-site']['properties']['hive.llap.io.memory.size'], '30720')
    self.assertEqual(configurations['hive-interactive-site']['properties']['hive.llap.io.enabled'], 'true')

    self.assertEqual(configurations['hive-interactive-env']['properties']['llap_heap_size'], '6553')
    self.assertEqual(configurations['hive-interactive-env']['properties']['hive_heapsize'], '2048')
    self.assertEqual(configurations['hive-interactive-env']['property_attributes']['num_llap_nodes'], {'maximum': '3', 'minimum': '1', 'read_only': 'false'})

    self.assertEqual(configurations['hive-interactive-env']['properties']['slider_am_container_mb'], '2048')
    self.assertEqual(configurations['hive-interactive-site']['properties']['hive.server2.tez.default.queues'], 'llap')
    self.assertEqual(configurations['hive-interactive-site']['properties']['hive.auto.convert.join.noconditionaltask.size'], '572522496')

    self.assertTrue('tez.am.resource.memory.mb' not in configurations['tez-interactive-site']['properties'])
    self.assertEquals(configurations['hive-interactive-site']['property_attributes']['hive.llap.daemon.queue.name'], {'entries': [{'value': 'default', 'label': 'default'}, {'value': 'llap', 'label': 'llap'}]})




  # Test 7: (1). 'default' and 'llap' (State : RUNNING) queue exists at root level in capacity-scheduler, and
  #          'capacity-scheduler' configs are passed-in as single "/n" separated string  and
  #          (2). enable_hive_interactive' is 'on' and (3). configuration change detected for 'enable_hive_interactive'
  #         Expected : Configurations values recommended for llap related configs.
  def test_recommendYARNConfigurations_three_node_manager_llap_configs_updated_2(self):
    # 3 node managers and yarn.nodemanager.resource.memory-mb": "12288"
    services = {
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
              "hostnames": ["c6401.ambari.apache.org", "c6402.ambari.apache.org", "c6403.ambari.apache.org"]
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
                "c6401.ambari.apache.org"
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
                                  'yarn.scheduler.capacity.root.ordering-policy=priority-utilization\n'
                                  'yarn.scheduler.capacity.root.llap.user-limit-factor=1\n'
                                  'yarn.scheduler.capacity.root.llap.state=RUNNING\n'
                                  'yarn.scheduler.capacity.root.llap.ordering-policy=fifo\n'
                                  'yarn.scheduler.capacity.root.llap.priority=10\n'
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
              'num_llap_nodes':'3',
              'num_llap_nodes_for_llap_daemons': '1',
             }
          },
        "hive-interactive-site":
          {
            'properties': {
              'hive.llap.daemon.queue.name': 'llap',
              'hive.server2.tez.sessions.per.default.queue': '1',
              'hive.tez.container.size':'2048'
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
            "yarn.nodemanager.resource.memory-mb": "12288",
            "yarn.nodemanager.resource.cpu-vcores": '3'
          }
        },
        "tez-interactive-site": {
          "properties": {
            "tez.am.resource.memory.mb": "1024"
          }
        },
        "hive-site":
          {
            'properties': {
              'hive.tez.container.size': '1024'
            }
          },
      }
    }


    clusterData = {
      "cpu": 4,
      "mapMemory": 30000,
      "amMemory": 20000,
      "reduceMemory": 20560,
      "containers": 30,
      "ramPerContainer": 341,
      "referenceNodeManagerHost" : {
        "total_mem" : 12288 * 1024
      },
      "yarnMinContainerSize": 341
    }


    configurations = {
    }

    self.stackAdvisor.recommendYARNConfigurations(configurations, clusterData, services, self.hosts)
    self.assertEqual(configurations['hive-interactive-site']['properties']['hive.server2.tez.sessions.per.default.queue'], '1')
    self.assertEquals(configurations['hive-interactive-site']['property_attributes']['hive.server2.tez.sessions.per.default.queue'], {'minimum': '1', 'maximum': '4'})
    self.assertEqual(configurations['capacity-scheduler']['properties'], {'capacity-scheduler': 'yarn.scheduler.capacity.root.accessible-node-labels=*\nyarn.scheduler.capacity.maximum-am-resource-percent=1\nyarn.scheduler.capacity.node-locality-delay=40\nyarn.scheduler.capacity.root.capacity=100\nyarn.scheduler.capacity.root.default.state=RUNNING\nyarn.scheduler.capacity.root.default.maximum-capacity=66.0\nyarn.scheduler.capacity.root.queues=default,llap\nyarn.scheduler.capacity.maximum-applications=10000\nyarn.scheduler.capacity.root.default.user-limit-factor=1\nyarn.scheduler.capacity.root.acl_administer_queue=*\nyarn.scheduler.capacity.root.default.acl_submit_applications=*\nyarn.scheduler.capacity.root.default.capacity=66.0\nyarn.scheduler.capacity.queue-mappings-override.enable=false\nyarn.scheduler.capacity.root.ordering-policy=priority-utilization\nyarn.scheduler.capacity.root.llap.user-limit-factor=1\nyarn.scheduler.capacity.root.llap.state=RUNNING\nyarn.scheduler.capacity.root.llap.ordering-policy=fifo\nyarn.scheduler.capacity.root.llap.priority=10\nyarn.scheduler.capacity.root.llap.minimum-user-limit-percent=100\nyarn.scheduler.capacity.root.llap.maximum-capacity=34.0\nyarn.scheduler.capacity.root.llap.capacity=34.0\nyarn.scheduler.capacity.root.llap.acl_submit_applications=hive\nyarn.scheduler.capacity.root.llap.acl_administer_queue=hive\nyarn.scheduler.capacity.root.llap.maximum-am-resource-percent=1'})

    self.assertTrue(configurations['hive-interactive-env']['properties']['num_llap_nodes_for_llap_daemons'], 3)
    self.assertTrue('num_llap_nodes' not in configurations['hive-interactive-env']['properties'])

    self.assertTrue(configurations['hive-interactive-env']['properties']['num_llap_nodes_for_llap_daemons'], 3)
    self.assertEqual(configurations['hive-interactive-site']['properties']['hive.llap.daemon.yarn.container.mb'], '10230')

    self.assertEqual(configurations['hive-interactive-site']['properties']['hive.llap.daemon.num.executors'], '3')
    self.assertEqual(configurations['hive-interactive-site']['properties']['hive.llap.io.threadpool.size'], '3')

    self.assertEqual(configurations['hive-interactive-site']['properties']['hive.llap.io.memory.size'], '1014')
    self.assertEqual(configurations['hive-interactive-site']['properties']['hive.llap.io.enabled'], 'true')

    self.assertEqual(configurations['hive-interactive-env']['properties']['llap_heap_size'], '7372')
    self.assertEqual(configurations['hive-interactive-env']['properties']['hive_heapsize'], '2048')
    self.assertEqual(configurations['hive-interactive-env']['property_attributes']['num_llap_nodes'], {'maximum': '3', 'minimum': '1', 'read_only': 'false'})

    self.assertEqual(configurations['hive-interactive-env']['properties']['slider_am_container_mb'], '682')
    self.assertEqual(configurations['hive-interactive-site']['properties']['hive.server2.tez.default.queues'], 'llap')
    self.assertEqual(configurations['hive-interactive-site']['properties']['hive.auto.convert.join.noconditionaltask.size'], '858783744')

    self.assertEqual(configurations['tez-interactive-site']['properties']['tez.am.resource.memory.mb'], '1364')
    self.assertEquals(configurations['hive-interactive-site']['property_attributes']['hive.llap.daemon.queue.name'], {'entries': [{'value': 'default', 'label': 'default'}, {'value': 'llap', 'label': 'llap'}]})


  # Test 8: (1). 'default' and 'llap' (State : RUNNING) queue exists at root level in capacity-scheduler, and
  #          'capacity-scheduler' configs are passed-in as single "/n" separated string  and
  #          (2). enable_hive_interactive' is 'on' and (3). configuration change detected for 'enable_hive_interactive'
  #
  #         Small configuration test with 3 nodes - 'yarn.nodemanager.resource.memory-mb' : 2046 and 'yarn.scheduler.minimum-allocation-mb' : 682, representing a small GCE cluster.
  #
  #         Expected : Configurations values recommended for llap related configs.
  def test_recommendYARNConfigurations_three_node_manager_llap_configs_updated_3(self):
    # 3 node managers and yarn.nodemanager.resource.memory-mb": "12288"
    services = {
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
              "hostnames": ["c6401.ambari.apache.org", "c6402.ambari.apache.org", "c6403.ambari.apache.org"]
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
                "c6401.ambari.apache.org"
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
                                  'yarn.scheduler.capacity.root.ordering-policy=priority-utilization\n'
                                  'yarn.scheduler.capacity.root.llap.user-limit-factor=1\n'
                                  'yarn.scheduler.capacity.root.llap.state=RUNNING\n'
                                  'yarn.scheduler.capacity.root.llap.ordering-policy=fifo\n'
                                  'yarn.scheduler.capacity.root.llap.priority=10\n'
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
              'num_llap_nodes':'3',
            }
          },
        "hive-interactive-site":
          {
            'properties': {
              'hive.llap.daemon.queue.name': 'llap',
              'hive.server2.tez.sessions.per.default.queue': '1',
              'hive.tez.container.size':'682'
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
            "yarn.nodemanager.resource.memory-mb": "2046",
            "yarn.nodemanager.resource.cpu-vcores": '3'
          }
        },
        "tez-interactive-site": {
          "properties": {
            "tez.am.resource.memory.mb": "682"
          }
        },
        "hive-site":
          {
            'properties': {
              'hive.tez.container.size': '1024'
            }
          },
      }
    }


    clusterData = {
      "cpu": 4,
      "mapMemory": 30000,
      "amMemory": 20000,
      "reduceMemory": 20560,
      "containers": 30,
      "ramPerContainer": 341,
      "referenceNodeManagerHost" : {
        "total_mem" : 12288 * 1024
      },
      "yarnMinContainerSize": 341
    }


    configurations = {
    }

    self.stackAdvisor.recommendYARNConfigurations(configurations, clusterData, services, self.hosts)

    self.assertEqual(configurations['hive-interactive-site']['properties']['hive.server2.tez.sessions.per.default.queue'], '1')
    self.assertEquals(configurations['hive-interactive-site']['property_attributes']['hive.server2.tez.sessions.per.default.queue'], {'minimum': '1', 'maximum': '3.0'})
    self.assertEqual(configurations['capacity-scheduler']['properties'], {'capacity-scheduler': 'yarn.scheduler.capacity.root.accessible-node-labels=*\nyarn.scheduler.capacity.maximum-am-resource-percent=1\nyarn.scheduler.capacity.node-locality-delay=40\nyarn.scheduler.capacity.root.capacity=100\nyarn.scheduler.capacity.root.default.state=RUNNING\nyarn.scheduler.capacity.root.default.maximum-capacity=66.0\nyarn.scheduler.capacity.root.queues=default,llap\nyarn.scheduler.capacity.maximum-applications=10000\nyarn.scheduler.capacity.root.default.user-limit-factor=1\nyarn.scheduler.capacity.root.acl_administer_queue=*\nyarn.scheduler.capacity.root.default.acl_submit_applications=*\nyarn.scheduler.capacity.root.default.capacity=66.0\nyarn.scheduler.capacity.queue-mappings-override.enable=false\nyarn.scheduler.capacity.root.llap.user-limit-factor=1\nyarn.scheduler.capacity.root.llap.state=RUNNING\nyarn.scheduler.capacity.root.llap.ordering-policy=fifo\nyarn.scheduler.capacity.root.llap.minimum-user-limit-percent=100\nyarn.scheduler.capacity.root.llap.maximum-capacity=34.0\nyarn.scheduler.capacity.root.llap.capacity=34.0\nyarn.scheduler.capacity.root.llap.acl_submit_applications=hive\nyarn.scheduler.capacity.root.llap.acl_administer_queue=hive\nyarn.scheduler.capacity.root.llap.maximum-am-resource-percent=1'})

    self.assertTrue(configurations['hive-interactive-env']['properties']['num_llap_nodes_for_llap_daemons'], 3)
    self.assertTrue('num_llap_nodes' not in configurations['hive-interactive-env']['properties'])

    self.assertEqual(configurations['hive-interactive-site']['properties']['hive.llap.daemon.yarn.container.mb'], '682')

    self.assertEqual(configurations['hive-interactive-site']['properties']['hive.llap.daemon.num.executors'], '1')
    self.assertEqual(configurations['hive-interactive-site']['properties']['hive.llap.io.threadpool.size'], '1')

    self.assertEqual(configurations['hive-interactive-site']['properties']['hive.llap.io.memory.size'], '0')
    self.assertEqual(configurations['hive-interactive-site']['properties']['hive.llap.io.enabled'], 'false')

    self.assertEqual(configurations['hive-interactive-env']['properties']['llap_heap_size'], '545')
    self.assertEqual(configurations['hive-interactive-env']['properties']['hive_heapsize'], '2048')
    self.assertEqual(configurations['hive-interactive-env']['property_attributes']['num_llap_nodes'], {'maximum': '3', 'minimum': '1', 'read_only': 'false'})

    self.assertEqual(configurations['hive-interactive-env']['properties']['slider_am_container_mb'], '682')
    self.assertEqual(configurations['hive-interactive-site']['properties']['hive.server2.tez.default.queues'], 'llap')
    self.assertEqual(configurations['hive-interactive-site']['properties']['hive.auto.convert.join.noconditionaltask.size'], '189792256')

    self.assertEqual(configurations['tez-interactive-site']['properties']['tez.am.resource.memory.mb'], '682')
    self.assertEquals(configurations['hive-interactive-site']['property_attributes']['hive.llap.daemon.queue.name'], {'entries': [{'value': 'default', 'label': 'default'}, {'value': 'llap', 'label': 'llap'}]})



  # Test 9: (1). 'default' and 'llap' (State : RUNNING) queue exists at root level in capacity-scheduler, and
  #         'capacity-scheduler' configs are passed-in as single "/n" separated string  and
  #          (2). enable_hive_interactive' is 'on' and (3). configuration change detected for 'hive.server2.tez.sessions.per.default.queue'
  #         Expected : Configurations values recommended for llap related configs.
  def test_recommendYARNConfigurations_three_node_manager_llap_configs_updated_3(self):
    # 3 node managers and yarn.nodemanager.resource.memory-mb": "204800"
    services = {
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
              "hostnames": ["c6401.ambari.apache.org", "c6402.ambari.apache.org", "c6403.ambari.apache.org"]
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
                "c6401.ambari.apache.org"
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
          u'type': u'hive-interactive-site',
          u'name': u'hive.server2.tez.sessions.per.default.queue'
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
              'num_llap_nodes':'3',
              'num_llap_nodes_for_llap_daemons': '1'
            }
          },
        "hive-interactive-site":
          {
            'properties': {
              'hive.llap.daemon.queue.name': 'llap',
              'hive.server2.tez.sessions.per.default.queue': '1',
              'hive.tez.container.size':'2048'
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
            "yarn.scheduler.minimum-allocation-mb": "2048",
            "yarn.nodemanager.resource.memory-mb": "204800",
            "yarn.nodemanager.resource.cpu-vcores": '3'
          }
        },
        "tez-interactive-site": {
          "properties": {
            "tez.am.resource.memory.mb": "1024"
          }
        },
        "hive-site":
          {
            'properties': {
              'hive.tez.container.size': '1024'
            }
          },
      }
    }



    clusterData = {
      "cpu": 4,
      "mapMemory": 30000,
      "amMemory": 20000,
      "reduceMemory": 20560,
      "containers": 30,
      "ramPerContainer": 2048,
      "referenceNodeManagerHost" : {
        "total_mem" : 204800 * 1024
      },
      "yarnMinContainerSize": 1024
    }

    configurations = {
    }


    self.stackAdvisor.recommendYARNConfigurations(configurations, clusterData, services, self.hosts)

    self.assertEqual(configurations['capacity-scheduler']['properties'], {'capacity-scheduler': 'yarn.scheduler.capacity.root.accessible-node-labels=*\nyarn.scheduler.capacity.maximum-am-resource-percent=1\nyarn.scheduler.capacity.node-locality-delay=40\nyarn.scheduler.capacity.root.capacity=100\nyarn.scheduler.capacity.root.default.state=RUNNING\nyarn.scheduler.capacity.root.default.maximum-capacity=0.0\nyarn.scheduler.capacity.root.queues=default,llap\nyarn.scheduler.capacity.maximum-applications=10000\nyarn.scheduler.capacity.root.default.user-limit-factor=1\nyarn.scheduler.capacity.root.acl_administer_queue=*\nyarn.scheduler.capacity.root.default.acl_submit_applications=*\nyarn.scheduler.capacity.root.default.capacity=0.0\nyarn.scheduler.capacity.queue-mappings-override.enable=false\nyarn.scheduler.capacity.root.ordering-policy=priority-utilization\nyarn.scheduler.capacity.root.llap.user-limit-factor=1\nyarn.scheduler.capacity.root.llap.state=RUNNING\nyarn.scheduler.capacity.root.llap.ordering-policy=fifo\nyarn.scheduler.capacity.root.llap.priority=10\nyarn.scheduler.capacity.root.llap.minimum-user-limit-percent=100\nyarn.scheduler.capacity.root.llap.maximum-capacity=100.0\nyarn.scheduler.capacity.root.llap.capacity=100.0\nyarn.scheduler.capacity.root.llap.acl_submit_applications=hive\nyarn.scheduler.capacity.root.llap.acl_administer_queue=hive\nyarn.scheduler.capacity.root.llap.maximum-am-resource-percent=1'})
    self.assertEquals(configurations['hive-interactive-site']['property_attributes']['hive.server2.tez.sessions.per.default.queue'], {'maximum': '4'})

    self.assertTrue(configurations['hive-interactive-env']['properties']['num_llap_nodes_for_llap_daemons'], 3)
    self.assertTrue('num_llap_nodes' not in configurations['hive-interactive-env']['properties'])
    self.assertEqual(configurations['hive-interactive-site']['properties']['hive.llap.daemon.yarn.container.mb'], '202752')

    self.assertEqual(configurations['hive-interactive-site']['properties']['hive.llap.daemon.num.executors'], '3')
    self.assertEqual(configurations['hive-interactive-site']['properties']['hive.llap.io.threadpool.size'], '3')

    self.assertEqual(configurations['hive-interactive-site']['properties']['hive.llap.io.memory.size'], '196608')
    self.assertEqual(configurations['hive-interactive-site']['properties']['hive.llap.io.enabled'], 'true')

    self.assertEqual(configurations['hive-interactive-env']['properties']['llap_heap_size'], '4915')
    self.assertEqual(configurations['hive-interactive-env']['properties']['hive_heapsize'], '2048')
    self.assertEqual(configurations['hive-interactive-env']['property_attributes']['num_llap_nodes'], {'maximum': '3', 'minimum': '1', 'read_only': 'false'})

    self.assertEqual(configurations['hive-interactive-env']['properties']['slider_am_container_mb'], '2048')
    self.assertEqual(configurations['hive-interactive-site']['properties']['hive.server2.tez.default.queues'], 'llap')
    self.assertEqual(configurations['hive-interactive-site']['properties']['hive.auto.convert.join.noconditionaltask.size'], '572522496')

    self.assertTrue('tez.am.resource.memory.mb' not in configurations['tez-interactive-site']['properties'])
    self.assertEquals(configurations['hive-interactive-site']['property_attributes']['hive.llap.daemon.queue.name'], {'entries': [{'value': 'default', 'label': 'default'}, {'value': 'llap', 'label': 'llap'}]})







  ####################### 'Five Node Managers' cluster - tests for calculating llap configs ################


  # Test 10: (1). 'default' and 'llap' (State : RUNNING) queue exists at root level in capacity-scheduler, and
  #          'capacity-scheduler' configs are passed-in as single "/n" separated string  and
  #          (2). enable_hive_interactive' is 'on' and (3). configuration change detected for 'num_llap_nodes'
  #         Expected : Configurations values recommended for llap related configs.
  def test_recommendYARNConfigurations_five_node_manager_llap_configs_updated_1(self):
    services = {
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
              "hostnames": ["c6401.ambari.apache.org", "c6402.ambari.apache.org", "c6403.ambari.apache.org", "c6404.ambari.apache.org", "c6405.ambari.apache.org"]
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
                "c6401.ambari.apache.org"
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
                                  'yarn.scheduler.capacity.root.ordering-policy=priority-utilization\n'
                                  'yarn.scheduler.capacity.root.llap.user-limit-factor=1\n'
                                  'yarn.scheduler.capacity.root.llap.state=RUNNING\n'
                                  'yarn.scheduler.capacity.root.llap.ordering-policy=fifo\n'
                                  'yarn.scheduler.capacity.root.llap.priority=10\n'
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
              'num_llap_nodes':'5'
            }
          },
        "hive-interactive-site":
          {
            'properties': {
              'hive.llap.daemon.queue.name': 'llap',
              'hive.server2.tez.sessions.per.default.queue': '1',
              'hive.tez.container.size':'2048'
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
            "yarn.scheduler.minimum-allocation-mb": "3072",
            "yarn.nodemanager.resource.memory-mb": "40960",
            "yarn.nodemanager.resource.cpu-vcores": '4'
          }
        },
        "tez-interactive-site": {
          "properties": {
            "tez.am.resource.memory.mb": "1024"
          }
        },
        "hive-site":
          {
            'properties': {
              'hive.tez.container.size': '1024'
            }
          },
      }
    }


    clusterData = {
      "cpu": 4,
      "mapMemory": 30000,
      "amMemory": 20000,
      "reduceMemory": 20560,
      "containers": 30,
      "ramPerContainer": 3072,
      "referenceNodeManagerHost" : {
        "total_mem" : 40960 * 1024
      },
      "yarnMinContainerSize": 1024
    }


    configurations = {
    }


    self.stackAdvisor.recommendYARNConfigurations(configurations, clusterData, services, self.hosts)

    self.assertEqual(configurations['capacity-scheduler']['properties'], {'capacity-scheduler': 'yarn.scheduler.capacity.root.accessible-node-labels=*\nyarn.scheduler.capacity.maximum-am-resource-percent=1\nyarn.scheduler.capacity.node-locality-delay=40\nyarn.scheduler.capacity.root.capacity=100\nyarn.scheduler.capacity.root.default.state=RUNNING\nyarn.scheduler.capacity.root.default.maximum-capacity=2.0\nyarn.scheduler.capacity.root.queues=default,llap\nyarn.scheduler.capacity.maximum-applications=10000\nyarn.scheduler.capacity.root.default.user-limit-factor=1\nyarn.scheduler.capacity.root.acl_administer_queue=*\nyarn.scheduler.capacity.root.default.acl_submit_applications=*\nyarn.scheduler.capacity.root.default.capacity=2.0\nyarn.scheduler.capacity.queue-mappings-override.enable=false\nyarn.scheduler.capacity.root.ordering-policy=priority-utilization\nyarn.scheduler.capacity.root.llap.user-limit-factor=1\nyarn.scheduler.capacity.root.llap.state=RUNNING\nyarn.scheduler.capacity.root.llap.ordering-policy=fifo\nyarn.scheduler.capacity.root.llap.priority=10\nyarn.scheduler.capacity.root.llap.minimum-user-limit-percent=100\nyarn.scheduler.capacity.root.llap.maximum-capacity=98.0\nyarn.scheduler.capacity.root.llap.capacity=98.0\nyarn.scheduler.capacity.root.llap.acl_submit_applications=hive\nyarn.scheduler.capacity.root.llap.acl_administer_queue=hive\nyarn.scheduler.capacity.root.llap.maximum-am-resource-percent=1'})
    self.assertEqual(configurations['hive-interactive-site']['properties']['hive.server2.tez.sessions.per.default.queue'], '1')
    self.assertEquals(configurations['hive-interactive-site']['property_attributes']['hive.server2.tez.sessions.per.default.queue'], {'maximum': '4', 'minimum': '1'})

    self.assertTrue('num_llap_nodes_for_llap_daemons' not in configurations['hive-interactive-env']['properties'])
    self.assertTrue('num_llap_nodes' not in configurations['hive-interactive-env']['properties'])

    self.assertEqual(configurations['hive-interactive-site']['properties']['hive.llap.daemon.yarn.container.mb'], '36864')

    self.assertEqual(configurations['hive-interactive-site']['properties']['hive.llap.daemon.num.executors'], '4')
    self.assertEqual(configurations['hive-interactive-site']['properties']['hive.llap.io.threadpool.size'], '4')

    self.assertEqual(configurations['hive-interactive-site']['properties']['hive.llap.io.memory.size'], '28672')
    self.assertEqual(configurations['hive-interactive-site']['properties']['hive.llap.io.enabled'], 'true')

    self.assertEqual(configurations['hive-interactive-env']['properties']['llap_heap_size'], '6553')
    self.assertEqual(configurations['hive-interactive-env']['properties']['hive_heapsize'], '2048')
    self.assertEqual(configurations['hive-interactive-env']['property_attributes']['num_llap_nodes'], {'maximum': '5', 'minimum': '1', 'read_only': 'false'})

    self.assertEqual(configurations['hive-interactive-env']['properties']['slider_am_container_mb'], '3072')
    self.assertEqual(configurations['hive-interactive-site']['properties']['hive.server2.tez.default.queues'], 'llap')
    self.assertEqual(configurations['hive-interactive-site']['properties']['hive.auto.convert.join.noconditionaltask.size'], '572522496')

    self.assertTrue('tez.am.resource.memory.mb' not in configurations['tez-interactive-site']['properties'])
    self.assertEquals(configurations['hive-interactive-site']['property_attributes']['hive.llap.daemon.queue.name'], {'entries': [{'value': 'default', 'label': 'default'}, {'value': 'llap', 'label': 'llap'}]})



  # Test 11: (1). 'default' and 'llap' (State : RUNNING) queue exists at root level in capacity-scheduler, and
  #          'capacity-scheduler' configs are passed-in as single "/n" separated string  and
  #          (2). enable_hive_interactive' is 'on' and (3). configuration change detected for 'enable_hive_interactive'
  #         Expected : Configurations values recommended for llap related configs.
  def test_recommendYARNConfigurations_five_node_manager_llap_configs_updated_2(self):

    # 3 node managers and yarn.nodemanager.resource.memory-mb": "12288"
    services = {
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
              "hostnames": ["c6401.ambari.apache.org", "c6402.ambari.apache.org", "c6403.ambari.apache.org", "c6404.ambari.apache.org", "c6405.ambari.apache.org"]
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
                "c6401.ambari.apache.org"
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
                                  'yarn.scheduler.capacity.root.ordering-policy=priority-utilization\n'
                                  'yarn.scheduler.capacity.root.llap.user-limit-factor=1\n'
                                  'yarn.scheduler.capacity.root.llap.state=RUNNING\n'
                                  'yarn.scheduler.capacity.root.llap.ordering-policy=fifo\n'
                                  'yarn.scheduler.capacity.root.llap.priority=10\n'
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
              'num_llap_nodes':'5'
            }
          },
        "hive-interactive-site":
          {
            'properties': {
              'hive.llap.daemon.queue.name': 'llap',
              'hive.server2.tez.sessions.per.default.queue': '1',
              'hive.tez.container.size':'2048'
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
            "yarn.nodemanager.resource.memory-mb": "204800",
            "yarn.nodemanager.resource.cpu-vcores": '10'
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

    clusterData = {
      "cpu": 4,
      "mapMemory": 30000,
      "amMemory": 20000,
      "reduceMemory": 20560,
      "containers": 30,
      "ramPerContainer": 341,
      "referenceNodeManagerHost" : {
        "total_mem" : 204800 * 1024
      },
      "yarnMinContainerSize": 341
    }


    configurations = {
    }


    self.stackAdvisor.recommendYARNConfigurations(configurations, clusterData, services, self.hosts)

    self.assertEqual(configurations['capacity-scheduler']['properties'], {'capacity-scheduler': 'yarn.scheduler.capacity.root.accessible-node-labels=*\nyarn.scheduler.capacity.maximum-am-resource-percent=1\nyarn.scheduler.capacity.node-locality-delay=40\nyarn.scheduler.capacity.root.capacity=100\nyarn.scheduler.capacity.root.default.state=RUNNING\nyarn.scheduler.capacity.root.default.maximum-capacity=80.0\nyarn.scheduler.capacity.root.queues=default,llap\nyarn.scheduler.capacity.maximum-applications=10000\nyarn.scheduler.capacity.root.default.user-limit-factor=1\nyarn.scheduler.capacity.root.acl_administer_queue=*\nyarn.scheduler.capacity.root.default.acl_submit_applications=*\nyarn.scheduler.capacity.root.default.capacity=80.0\nyarn.scheduler.capacity.queue-mappings-override.enable=false\nyarn.scheduler.capacity.root.ordering-policy=priority-utilization\nyarn.scheduler.capacity.root.llap.user-limit-factor=1\nyarn.scheduler.capacity.root.llap.state=RUNNING\nyarn.scheduler.capacity.root.llap.ordering-policy=fifo\nyarn.scheduler.capacity.root.llap.priority=10\nyarn.scheduler.capacity.root.llap.minimum-user-limit-percent=100\nyarn.scheduler.capacity.root.llap.maximum-capacity=20.0\nyarn.scheduler.capacity.root.llap.capacity=20.0\nyarn.scheduler.capacity.root.llap.acl_submit_applications=hive\nyarn.scheduler.capacity.root.llap.acl_administer_queue=hive\nyarn.scheduler.capacity.root.llap.maximum-am-resource-percent=1'})
    self.assertEqual(configurations['hive-interactive-site']['properties']['hive.server2.tez.sessions.per.default.queue'], '1')
    self.assertEquals(configurations['hive-interactive-site']['property_attributes']['hive.server2.tez.sessions.per.default.queue'], {'maximum': '4', 'minimum': '1'})

    self.assertTrue('num_llap_nodes_for_llap_daemons' not in configurations['hive-interactive-env']['properties'])
    self.assertTrue('num_llap_nodes' not in configurations['hive-interactive-env']['properties'])

    self.assertEqual(configurations['hive-interactive-site']['properties']['hive.llap.daemon.yarn.container.mb'], '199485')

    self.assertEqual(configurations['hive-interactive-site']['properties']['hive.llap.daemon.num.executors'], '10')
    self.assertEqual(configurations['hive-interactive-site']['properties']['hive.llap.io.threadpool.size'], '10')

    self.assertEqual(configurations['hive-interactive-site']['properties']['hive.llap.io.memory.size'], '158525')
    self.assertEqual(configurations['hive-interactive-site']['properties']['hive.llap.io.enabled'], 'true')

    self.assertEqual(configurations['hive-interactive-env']['properties']['llap_heap_size'], '34816')
    self.assertEqual(configurations['hive-interactive-env']['properties']['hive_heapsize'], '2048')
    self.assertEqual(configurations['hive-interactive-env']['property_attributes']['num_llap_nodes'], {'maximum': '5', 'minimum': '1', 'read_only': 'false'})

    self.assertEqual(configurations['hive-interactive-env']['properties']['slider_am_container_mb'], '682')
    self.assertEqual(configurations['hive-interactive-site']['properties']['hive.server2.tez.default.queues'], 'llap')
    self.assertEqual(configurations['hive-interactive-site']['properties']['hive.auto.convert.join.noconditionaltask.size'], '1145044992')

    self.assertEqual(configurations['tez-interactive-site']['properties']['tez.am.resource.memory.mb'], '4433')
    self.assertEquals(configurations['hive-interactive-site']['property_attributes']['hive.llap.daemon.queue.name'], {'entries': [{'value': 'default', 'label': 'default'}, {'value': 'llap', 'label': 'llap'}]})



  # Test 12: (1). 'default' and 'llap' (State : RUNNING) queue exists at root level in capacity-scheduler, and
  #          'capacity-scheduler' configs are passed-in as single "/n" separated string  and
  #          (2). enable_hive_interactive' is 'on' and (3). configuration change detected for 'hive.server2.tez.sessions.per.default.queue'
  #         Expected : Configurations values recommended for llap related configs.
  def test_recommendYARNConfigurations_five_node_manager_llap_configs_updated_3(self):
    # 3 node managers and yarn.nodemanager.resource.memory-mb": "204800"
    services = {
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
              "hostnames": ["c6401.ambari.apache.org", "c6402.ambari.apache.org", "c6403.ambari.apache.org", "c6404.ambari.apache.org", "c6405.ambari.apache.org"]
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
                "c6401.ambari.apache.org"
              ]
            },
            "dependencies": []
          },
        ]
      }
      ],
      "changed-configurations": [
        {
          u'old_value': u'3',
          u'type': u'hive-interactive-site',
          u'name': u'hive.server2.tez.sessions.per.default.queue'
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
                                  'yarn.scheduler.capacity.root.ordering-policy=priority-utilization\n'
                                  'yarn.scheduler.capacity.root.llap.user-limit-factor=1\n'
                                  'yarn.scheduler.capacity.root.llap.state=RUNNING\n'
                                  'yarn.scheduler.capacity.root.llap.ordering-policy=fifo\n'
                                  'yarn.scheduler.capacity.root.llap.priority=10\n'
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
              'num_llap_nodes':'5'
            }
          },
        "hive-interactive-site":
          {
            'properties': {
              'hive.llap.daemon.queue.name': 'llap',
              'hive.server2.tez.sessions.per.default.queue': '1',
              'hive.tez.container.size':'2048'
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
            "yarn.scheduler.minimum-allocation-mb": "2048",
            "yarn.nodemanager.resource.memory-mb": "204800",
            "yarn.nodemanager.resource.cpu-vcores": '3'
          }
        },
        "tez-interactive-site": {
          "properties": {
            "tez.am.resource.memory.mb": "1024"
          }
        },
        "hive-site":
          {
            'properties': {
              'hive.tez.container.size': '1024'
            }
          },
      }
    }

    clusterData = {
      "cpu": 4,
      "mapMemory": 30000,
      "amMemory": 20000,
      "reduceMemory": 20560,
      "containers": 3,
      "ramPerContainer": 82240,
      "referenceNodeManagerHost" : {
        "total_mem" : 204800 * 1024
      },
      "yarnMinContainerSize": 1024
    }

    configurations = {
    }

    self.stackAdvisor.recommendYARNConfigurations(configurations, clusterData, services, self.hosts)

    self.assertEqual(configurations['capacity-scheduler']['properties'], {'capacity-scheduler': 'yarn.scheduler.capacity.root.accessible-node-labels=*\nyarn.scheduler.capacity.maximum-am-resource-percent=1\nyarn.scheduler.capacity.node-locality-delay=40\nyarn.scheduler.capacity.root.capacity=100\nyarn.scheduler.capacity.root.default.state=RUNNING\nyarn.scheduler.capacity.root.default.maximum-capacity=0.0\nyarn.scheduler.capacity.root.queues=default,llap\nyarn.scheduler.capacity.maximum-applications=10000\nyarn.scheduler.capacity.root.default.user-limit-factor=1\nyarn.scheduler.capacity.root.acl_administer_queue=*\nyarn.scheduler.capacity.root.default.acl_submit_applications=*\nyarn.scheduler.capacity.root.default.capacity=0.0\nyarn.scheduler.capacity.queue-mappings-override.enable=false\nyarn.scheduler.capacity.root.ordering-policy=priority-utilization\nyarn.scheduler.capacity.root.llap.user-limit-factor=1\nyarn.scheduler.capacity.root.llap.state=RUNNING\nyarn.scheduler.capacity.root.llap.ordering-policy=fifo\nyarn.scheduler.capacity.root.llap.priority=10\nyarn.scheduler.capacity.root.llap.minimum-user-limit-percent=100\nyarn.scheduler.capacity.root.llap.maximum-capacity=100.0\nyarn.scheduler.capacity.root.llap.capacity=100.0\nyarn.scheduler.capacity.root.llap.acl_submit_applications=hive\nyarn.scheduler.capacity.root.llap.acl_administer_queue=hive\nyarn.scheduler.capacity.root.llap.maximum-am-resource-percent=1'})
    self.assertEquals(configurations['hive-interactive-site']['property_attributes']['hive.server2.tez.sessions.per.default.queue'], {'maximum': '4'})

    self.assertTrue('num_llap_nodes_for_llap_daemons' not in configurations['hive-interactive-env']['properties'])
    self.assertTrue('num_llap_nodes' not in configurations['hive-interactive-env']['properties'])

    self.assertEqual(configurations['hive-interactive-site']['properties']['hive.llap.daemon.yarn.container.mb'], '202752')

    self.assertEqual(configurations['hive-interactive-site']['properties']['hive.llap.daemon.num.executors'], '3')
    self.assertEqual(configurations['hive-interactive-site']['properties']['hive.llap.io.threadpool.size'], '3')

    self.assertEqual(configurations['hive-interactive-site']['properties']['hive.llap.io.memory.size'], '196608')
    self.assertEqual(configurations['hive-interactive-site']['properties']['hive.llap.io.enabled'], 'true')

    self.assertEqual(configurations['hive-interactive-env']['properties']['llap_heap_size'], '4915')
    self.assertEqual(configurations['hive-interactive-env']['properties']['hive_heapsize'], '2048')
    self.assertEqual(configurations['hive-interactive-env']['property_attributes']['num_llap_nodes'], {'maximum': '5', 'minimum': '1', 'read_only': 'false'})

    self.assertEqual(configurations['hive-interactive-env']['properties']['slider_am_container_mb'], '2048')
    self.assertEqual(configurations['hive-interactive-site']['properties']['hive.server2.tez.default.queues'], 'llap')
    self.assertEqual(configurations['hive-interactive-site']['properties']['hive.auto.convert.join.noconditionaltask.size'], '572522496')

    self.assertTrue('tez.am.resource.memory.mb' not in configurations['tez-interactive-site']['properties'])
    self.assertEquals(configurations['hive-interactive-site']['property_attributes']['hive.llap.daemon.queue.name'], {'entries': [{'value': 'default', 'label': 'default'}, {'value': 'llap', 'label': 'llap'}]})









  # Test 13 (1). 'default' and 'llap' (State : RUNNING) queue exists at root level in capacity-scheduler, and
  #          'capacity-scheduler' configs are passed-in as dictionary and
  #          services['configurations']["capacity-scheduler"]["properties"]["capacity-scheduler"] is set to value "null"  and
  #          (2). enable_hive_interactive' is 'on' and (3). configuration change detected for 'hive.server2.tez.sessions.per.default.queue'
  #         Expected : Configurations values recommended for llap related configs.
  def test_recommendYARNConfigurations_five_node_manager_llap_configs_updated_5(self):
    # 3 node managers and yarn.nodemanager.resource.memory-mb": "204800"
    services = {
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
              "hostnames": ["c6401.ambari.apache.org", "c6402.ambari.apache.org", "c6403.ambari.apache.org", "c6404.ambari.apache.org", "c6405.ambari.apache.org"]
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
                "c6401.ambari.apache.org"
              ]
            },
            "dependencies": []
          },
        ]
      }
      ],
      "changed-configurations": [
        {
          u'old_value': u'3',
          u'type': u'hive-interactive-site',
          u'name': u'hive.server2.tez.sessions.per.default.queue'
        }
      ],
      "configurations": {
        "capacity-scheduler" : {
          "properties" : {
            "capacity-scheduler" : "null",
            "yarn.scheduler.capacity.root.accessible-node-labels" : "*",
            "yarn.scheduler.capacity.maximum-am-resource-percent" : "1",
            "yarn.scheduler.capacity.root.acl_administer_queue" : "*",
            'yarn.scheduler.capacity.queue-mappings-override.enable' : 'false',
            "yarn.scheduler.capacity.root.default.capacity" : "100",
            "yarn.scheduler.capacity.root.default.user-limit-factor" : "1",
            "yarn.scheduler.capacity.root.queues" : "default",
            "yarn.scheduler.capacity.root.capacity" : "100",
            "yarn.scheduler.capacity.root.default.acl_submit_applications" : "*",
            "yarn.scheduler.capacity.root.default.maximum-capacity" : "100",
            "yarn.scheduler.capacity.node-locality-delay" : "40",
            "yarn.scheduler.capacity.maximum-applications" : "10000",
            "yarn.scheduler.capacity.root.default.state" : "RUNNING"
          }
        },
        "hive-interactive-env":
          {
            'properties': {
              'enable_hive_interactive': 'true',
              'llap_queue_capacity':'50'
            }
          },
        "hive-interactive-site":
          {
            'properties': {
              'hive.llap.daemon.queue.name': 'default',
              'hive.server2.tez.sessions.per.default.queue': '1',
              'hive.tez.container.size':'2048'
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
            "yarn.scheduler.minimum-allocation-mb": "2048",
            "yarn.nodemanager.resource.memory-mb": "204800",
            "yarn.nodemanager.resource.cpu-vcores": '3'
          }
        },
        "tez-interactive-site": {
          "properties": {
            "tez.am.resource.memory.mb": "1024"
          }
        },
        "hive-site":
          {
            'properties': {
              'hive.tez.container.size': '1024'
            }
          },
      }
    }

    clusterData = {
      "cpu": 4,
      "mapMemory": 30000,
      "amMemory": 20000,
      "reduceMemory": 20560,
      "containers": 3,
      "ramPerContainer": 82240,
      "referenceNodeManagerHost" : {
        "total_mem" : 328960 * 1024
      },
      "yarnMinContainerSize": 1024
    }


    configurations = {
    }
    self.stackAdvisor.recommendYARNConfigurations(configurations, clusterData, services, self.hosts)

    self.assertTrue('capacity-scheduler' not in configurations)
    self.assertEquals(configurations['hive-interactive-site']['property_attributes']['hive.server2.tez.sessions.per.default.queue'], {'maximum': '4'})

    self.assertTrue(configurations['hive-interactive-env']['properties']['num_llap_nodes'], 3)
    self.assertTrue('num_llap_nodes_for_llap_daemons' not in configurations['hive-interactive-env']['properties'])

    self.assertEqual(configurations['hive-interactive-site']['properties']['hive.llap.daemon.yarn.container.mb'], '202752')

    self.assertEqual(configurations['hive-interactive-site']['properties']['hive.llap.daemon.num.executors'], '3')
    self.assertEqual(configurations['hive-interactive-site']['properties']['hive.llap.io.threadpool.size'], '3')

    self.assertEqual(configurations['hive-interactive-site']['properties']['hive.llap.io.memory.size'], '196608')
    self.assertEqual(configurations['hive-interactive-site']['properties']['hive.llap.io.enabled'], 'true')

    self.assertEqual(configurations['hive-interactive-env']['properties']['llap_heap_size'], '4915')
    self.assertEqual(configurations['hive-interactive-env']['properties']['hive_heapsize'], '2048')
    self.assertEqual(configurations['hive-interactive-env']['property_attributes']['num_llap_nodes'], {'maximum': '5', 'minimum': '1', 'read_only': 'true'})

    self.assertEqual(configurations['hive-interactive-env']['properties']['slider_am_container_mb'], '2048')
    self.assertEqual(configurations['hive-interactive-site']['properties']['hive.auto.convert.join.noconditionaltask.size'], '572522496')

    self.assertTrue('tez.am.resource.memory.mb' not in configurations['tez-interactive-site']['properties'])
    self.assertEquals(configurations['hive-interactive-site']['property_attributes']['hive.llap.daemon.queue.name'], {'entries': [{'value': 'default', 'label': 'default'}]})





  # Test 14: (1). Multiple queue exist at various depths in capacity-scheduler, and 'capacity-scheduler' configs are
  #               passed-in as dictionary and services['configurations']["capacity-scheduler"]["properties"]["capacity-scheduler"]
  #               is set to value "null"  and
  #          (2). Selected queue in 'hive.llap.daemon.queue.name' is 'default.b'
  #          (3). enable_hive_interactive' is 'on' and (3). configuration change detected for 'hive.server2.tez.sessions.per.default.queue'
  #         Expected : Configurations values recommended for llap related configs.
  def test_recommendYARNConfigurations_five_node_manager_llap_configs_updated_6(self):
    services= {
      "services": [{
        "StackServices": {
          "service_name": "YARN",
        },
        "Versions": {
          "parent_stack_version": "2.4",
          "stack_name": "HDP",
          "stack_version": "2.5",
          "stack_hierarchy": {
            "stack_name": "HDP",
            "stack_versions": ["2.4", "2.3", "2.2", "2.1", "2.0.6"]
          }
        },
        "components": [
          {
            "StackServiceComponents": {
              "component_name": "NODEMANAGER",
              "hostnames": ["c6401.ambari.apache.org", "c6402.ambari.apache.org", "c6403.ambari.apache.org", "c6404.ambari.apache.org", "c6405.ambari.apache.org"]
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
                "c6401.ambari.apache.org"
              ]
            },
            "dependencies": []
          },
        ]
      }
      ],
      "changed-configurations": [ ],
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
                                  "yarn.scheduler.capacity.root.default.a.capacity=20\n"
                                  "yarn.scheduler.capacity.root.default.a.maximum-capacity=0\n"
                                  "yarn.scheduler.capacity.root.default.a.minimum-user-limit-percent=100\n"
                                  "yarn.scheduler.capacity.root.default.a.ordering-policy=fifo\n"
                                  "yarn.scheduler.capacity.root.default.a.queues=a1,llap\n"
                                  "yarn.scheduler.capacity.root.default.a.state=RUNNING\n"
                                  "yarn.scheduler.capacity.root.default.a.user-limit-factor=1\n"
                                  "yarn.scheduler.capacity.root.default.acl_submit_applications=*\n"
                                  "yarn.scheduler.capacity.root.default.b.acl_administer_queue=*\n"
                                  "yarn.scheduler.capacity.root.default.b.acl_submit_applications=*\n"
                                  "yarn.scheduler.capacity.root.default.b.capacity=80\n"
                                  "yarn.scheduler.capacity.root.default.b.maximum-capacity=100\n"
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
              'num_llap_nodes':'2'
            }
          },
        "hive-interactive-site":
          {
            'properties': {
              'hive.llap.daemon.queue.name': 'b',
              'hive.server2.tez.sessions.per.default.queue': '1',
              'hive.tez.container.size':'2048'
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
            "yarn.scheduler.minimum-allocation-mb": "2048",
            "yarn.nodemanager.resource.memory-mb": "204800",
            "yarn.nodemanager.resource.cpu-vcores": '3'
          }
        },
        "tez-interactive-site": {
          "properties": {
            "tez.am.resource.memory.mb": "1024"
          }
        },
        "hive-site":
          {
            'properties': {
              'hive.tez.container.size': '1024'
            }
          },
      }
    }

    clusterData = {
      "cpu": 4,
      "mapMemory": 30000,
      "amMemory": 20000,
      "reduceMemory": 20560,
      "containers": 30,
      "ramPerContainer": 512,
      "referenceNodeManagerHost" : {
        "total_mem" : 10240 * 1024
      },
      "yarnMinContainerSize": 512
    }

    configurations = {
    }
    self.stackAdvisor.recommendYARNConfigurations(configurations, clusterData, services, self.hosts)


    self.assertTrue('capacity-scheduler' not in configurations)

    self.assertEqual(configurations['hive-interactive-site']['properties']['hive.server2.tez.sessions.per.default.queue'], '1')
    self.assertEquals(configurations['hive-interactive-site']['property_attributes']['hive.server2.tez.sessions.per.default.queue'], {'maximum': '4', 'minimum': '1'})

    self.assertTrue(configurations['hive-interactive-env']['properties']['num_llap_nodes'], 3)
    self.assertTrue('num_llap_nodes_for_llap_daemons' not in configurations['hive-interactive-env']['properties'])

    self.assertEqual(configurations['hive-interactive-site']['properties']['hive.llap.daemon.yarn.container.mb'], '204288')

    self.assertEqual(configurations['hive-interactive-site']['properties']['hive.llap.daemon.num.executors'], '3')
    self.assertEqual(configurations['hive-interactive-site']['properties']['hive.llap.io.threadpool.size'], '3')

    self.assertEqual(configurations['hive-interactive-site']['properties']['hive.llap.io.memory.size'], '198144')
    self.assertEqual(configurations['hive-interactive-site']['properties']['hive.llap.io.enabled'], 'true')

    self.assertEqual(configurations['hive-interactive-env']['properties']['llap_heap_size'], '4915')
    self.assertEqual(configurations['hive-interactive-env']['properties']['hive_heapsize'], '2048')
    self.assertEqual(configurations['hive-interactive-env']['property_attributes']['num_llap_nodes'], {'maximum': '5', 'minimum': '1', 'read_only': 'true'})

    self.assertEqual(configurations['hive-interactive-env']['properties']['slider_am_container_mb'], '512')
    self.assertEqual(configurations['hive-interactive-site']['properties']['hive.auto.convert.join.noconditionaltask.size'], '572522496')

    self.assertTrue('tez.am.resource.memory.mb' not in configurations['tez-interactive-site']['properties'])
    self.assertEquals(configurations['hive-interactive-site']['property_attributes']['hive.llap.daemon.queue.name'], {'entries': [{'value': 'a1', 'label': 'a1'}, {'value': 'b', 'label': 'b'}, {'value': 'llap', 'label': 'llap'}]})




  # Test 15: (1). Multiple queue exist at various depths in capacity-scheduler, and 'capacity-scheduler' configs are
  #               passed-in as dictionary and services['configurations']["capacity-scheduler"]["properties"]["capacity-scheduler"]
  #               is set to value "null"  and
  #          (2). Selected queue in 'hive.llap.daemon.queue.name' is 'default.b' and is in STOPPED state
  #          (3). enable_hive_interactive' is 'on' and (3). configuration change detected for 'hive.server2.tez.sessions.per.default.queue'
  #         Expected : No calculations for llap related configs.
  def test_recommendYARNConfigurations_five_node_manager_llap_configs_updated_7(self):
    services= {
      "services": [{
        "StackServices": {
          "service_name": "YARN",
        },
        "Versions": {
          "parent_stack_version": "2.4",
          "stack_name": "HDP",
          "stack_version": "2.5",
          "stack_hierarchy": {
            "stack_name": "HDP",
            "stack_versions": ["2.4", "2.3", "2.2", "2.1", "2.0.6"]
          }
        },
        "components": [
          {
            "StackServiceComponents": {
              "component_name": "NODEMANAGER",
              "hostnames": ["c6401.ambari.apache.org", "c6402.ambari.apache.org", "c6403.ambari.apache.org", "c6404.ambari.apache.org", "c6405.ambari.apache.org"]
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
                "c6401.ambari.apache.org"
              ]
            },
            "dependencies": []
          },
        ]
      }
      ],
      "changed-configurations": [
        {
          u'old_value': u'3',
          u'type': u'hive-interactive-site',
          u'name': u'hive.server2.tez.sessions.per.default.queue'
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
                                  "yarn.scheduler.capacity.root.default.a.capacity=0\n"
                                  "yarn.scheduler.capacity.root.default.a.maximum-capacity=0\n"
                                  "yarn.scheduler.capacity.root.default.a.minimum-user-limit-percent=100\n"
                                  "yarn.scheduler.capacity.root.default.a.ordering-policy=fifo\n"
                                  "yarn.scheduler.capacity.root.default.a.queues=a1,llap\n"
                                  "yarn.scheduler.capacity.root.default.a.state=RUNNING\n"
                                  "yarn.scheduler.capacity.root.default.a.user-limit-factor=1\n"
                                  "yarn.scheduler.capacity.root.default.acl_submit_applications=*\n"
                                  "yarn.scheduler.capacity.root.default.b.acl_administer_queue=*\n"
                                  "yarn.scheduler.capacity.root.default.b.acl_submit_applications=*\n"
                                  "yarn.scheduler.capacity.root.default.b.capacity=100\n"
                                  "yarn.scheduler.capacity.root.default.b.maximum-capacity=100\n"
                                  "yarn.scheduler.capacity.root.default.b.minimum-user-limit-percent=100\n"
                                  "yarn.scheduler.capacity.root.default.b.ordering-policy=fifo\n"
                                  "yarn.scheduler.capacity.root.default.b.state=STOPPED\n"
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
              'num_llap_nodes_for_llap_daemons': '1'
            }
          },
        "hive-interactive-site":
          {
            'properties': {
              'hive.llap.daemon.queue.name': 'default.b',
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
            "yarn.scheduler.minimum-allocation-mb": "2048",
            "yarn.nodemanager.resource.memory-mb": "204800",
            "yarn.nodemanager.resource.cpu-vcores": '3'
          }
        },
        "tez-site": {
          "properties": {
            "tez.am.resource.memory.mb": "1024"
          }
        },
        "hive-site":
          {
            'properties': {
              'hive.tez.container.size': '1024'
            }
          },
      }
    }

    clusterData = {
      "cpu": 4,
      "mapMemory": 30000,
      "amMemory": 20000,
      "reduceMemory": 20560,
      "containers": 30,
      "ramPerContainer": 512,
      "referenceNodeManagerHost" : {
        "total_mem" : 10240 * 1024
      },
      "yarnMinContainerSize": 512
    }

    configurations = {
    }
    self.stackAdvisor.recommendYARNConfigurations(configurations, clusterData, services, self.hosts)

    self.assertEqual(configurations['hive-interactive-env']['properties']['num_llap_nodes'], '0')
    self.assertTrue(configurations['hive-interactive-env']['properties']['num_llap_nodes_for_llap_daemons'], 0)


    self.assertEqual(configurations['hive-interactive-site']['properties']['hive.llap.daemon.yarn.container.mb'], '2048')

    self.assertEqual(configurations['hive-interactive-site']['properties']['hive.llap.daemon.num.executors'], '0')
    self.assertEqual(configurations['hive-interactive-site']['properties']['hive.llap.io.threadpool.size'], '0')

    self.assertEqual(configurations['hive-interactive-site']['properties']['hive.llap.io.memory.size'], '0')
    self.assertTrue('hive.llap.io.enabled' not in configurations['hive-interactive-site']['properties'])

    self.assertEqual(configurations['hive-interactive-env']['properties']['llap_heap_size'], '0')

    self.assertEqual(configurations['hive-interactive-env']['properties']['slider_am_container_mb'], '1024')
    self.assertEquals(configurations['hive-interactive-env']['property_attributes']['num_llap_nodes'],
                      {'maximum': '5', 'minimum': '1', 'read_only': 'true'})

    self.assertTrue('tez.am.resource.memory.mb' not in configurations['tez-interactive-site']['properties'])




  ####################### 'Nine Node Managers' cluster - tests for calculating llap configs ################



  # Test 16 (1). 'default' and 'llap' (State : RUNNING) queue exists at root level in capacity-scheduler, and
  #          'capacity-scheduler' configs are passed-in as dictionary and
  #          services['configurations']["capacity-scheduler"]["properties"]["capacity-scheduler"] is set to value "null"  and
  #          (2). enable_hive_interactive' is 'on' and (3). configuration change detected for 'hive.server2.tez.sessions.per.default.queue'
  #         Expected : Configurations values recommended for llap related configs.
  def test_recommendYARNConfigurations_nine_node_manager_llap_configs_updated_1(self):
    # 9 node managers and yarn.nodemanager.resource.memory-mb": "204800"
    services = {
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
              "hostnames": ["c6401.ambari.apache.org", "c6402.ambari.apache.org", "c6403.ambari.apache.org",
                            "c6404.ambari.apache.org", "c6405.ambari.apache.org", "c6406.ambari.apache.org",
                            "c6407.ambari.apache.org", "c6408.ambari.apache.org", "c6409.ambari.apache.org"]
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
                "c6401.ambari.apache.org"
              ]
            },
            "dependencies": []
          },
        ]
      }
      ],
      "changed-configurations": [
        {
          u'old_value': u'3',
          u'type': u'hive-interactive-site',
          u'name': u'hive.server2.tez.sessions.per.default.queue'
        }
      ],
      "configurations": {
        "capacity-scheduler" : {
          "properties" : {
            "capacity-scheduler" : "null",
            "yarn.scheduler.capacity.root.accessible-node-labels" : "*",
            "yarn.scheduler.capacity.maximum-am-resource-percent" : "1",
            "yarn.scheduler.capacity.root.acl_administer_queue" : "*",
            'yarn.scheduler.capacity.queue-mappings-override.enable' : 'false',
            "yarn.scheduler.capacity.root.default.capacity" : "100",
            "yarn.scheduler.capacity.root.default.user-limit-factor" : "1",
            "yarn.scheduler.capacity.root.queues" : "default",
            "yarn.scheduler.capacity.root.capacity" : "100",
            "yarn.scheduler.capacity.root.default.acl_submit_applications" : "*",
            "yarn.scheduler.capacity.root.default.maximum-capacity" : "100",
            "yarn.scheduler.capacity.node-locality-delay" : "40",
            "yarn.scheduler.capacity.maximum-applications" : "10000",
            "yarn.scheduler.capacity.root.default.state" : "RUNNING"
          }
        },
        "hive-interactive-env":
          {
            'properties': {
              'enable_hive_interactive': 'true',
              'llap_queue_capacity':'50'
            }
          },
        "hive-interactive-site":
          {
            'properties': {
              'hive.llap.daemon.queue.name': 'default',
              'hive.server2.tez.sessions.per.default.queue': '4',
              'hive.tez.container.size':'4096'
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
            "yarn.scheduler.minimum-allocation-mb": "1024",
            "yarn.nodemanager.resource.memory-mb": "212992",
            "yarn.nodemanager.resource.cpu-vcores": '25'
          }
        },
        "tez-interactive-site": {
          "properties": {
            "tez.am.resource.memory.mb": "4096"
          }
        },
        "hive-site":
          {
            'properties': {
              'hive.tez.container.size': '1024'
            }
          },
      }
    }

    clusterData = {
      "cpu": 4,
      "mapMemory": 30000,
      "amMemory": 20000,
      "reduceMemory": 20560,
      "containers": 3,
      "ramPerContainer": 82240,
      "referenceNodeManagerHost" : {
        "total_mem" : 328960 * 1024
      },
      "yarnMinContainerSize": 1024
    }

    configurations = {
    }

    # Tests based on concurrency (hive.server2.tez.sessions.per.default.queue)  config changes

    ###################################################################
    #  Test A: 'hive.server2.tez.sessions.per.default.queue' set to = 4
    ###################################################################

    # Test
    self.stackAdvisor.recommendYARNConfigurations(configurations, clusterData, services, self.hosts_9_total)
    self.assertTrue('capacity-scheduler' not in configurations)
    self.assertEquals(configurations['hive-interactive-site']['property_attributes']['hive.server2.tez.sessions.per.default.queue'], {'maximum': '22'})

    self.assertTrue(configurations['hive-interactive-env']['properties']['num_llap_nodes'], 3)
    self.assertTrue('num_llap_nodes_for_llap_daemons' not in configurations['hive-interactive-env']['properties'])

    self.assertEqual(configurations['hive-interactive-site']['properties']['hive.llap.daemon.yarn.container.mb'], '208896')

    self.assertEqual(configurations['hive-interactive-site']['properties']['hive.llap.daemon.num.executors'], '25')
    self.assertEqual(configurations['hive-interactive-site']['properties']['hive.llap.io.threadpool.size'], '25')

    self.assertEqual(configurations['hive-interactive-site']['properties']['hive.llap.io.memory.size'], '106496')
    self.assertEqual(configurations['hive-interactive-site']['properties']['hive.llap.io.enabled'], 'true')

    self.assertEqual(configurations['hive-interactive-env']['properties']['llap_heap_size'], '96256')
    self.assertEqual(configurations['hive-interactive-env']['properties']['hive_heapsize'], '2048')
    self.assertEqual(configurations['hive-interactive-env']['property_attributes']['num_llap_nodes'], {'maximum': '9', 'minimum': '1', 'read_only': 'true'})

    self.assertEqual(configurations['hive-interactive-env']['properties']['slider_am_container_mb'], '1024')
    self.assertEqual(configurations['hive-interactive-site']['properties']['hive.auto.convert.join.noconditionaltask.size'], '1145044992')

    self.assertTrue('tez.am.resource.memory.mb' not in configurations['tez-interactive-site']['properties'])
    self.assertEquals(configurations['hive-interactive-site']['property_attributes']['hive.llap.daemon.queue.name'], {'entries': [{'value': 'default', 'label': 'default'}]})


    ##################################################################
    # Test B: 'hive.server2.tez.sessions.per.default.queue' set to = 9
    ##################################################################
    # Set the config
    services['configurations']['hive-interactive-site']['properties']['hive.server2.tez.sessions.per.default.queue'] = 9

    # Test
    self.stackAdvisor.recommendYARNConfigurations(configurations, clusterData, services, self.hosts_9_total)
    self.assertTrue('capacity-scheduler' not in configurations)
    self.assertEquals(configurations['hive-interactive-site']['property_attributes']['hive.server2.tez.sessions.per.default.queue'], {'maximum': '22'})

    self.assertTrue(configurations['hive-interactive-env']['properties']['num_llap_nodes'], 3)
    self.assertTrue('num_llap_nodes_for_llap_daemons' not in configurations['hive-interactive-env']['properties'])

    self.assertEqual(configurations['hive-interactive-site']['properties']['hive.llap.daemon.yarn.container.mb'], '207872')

    self.assertEqual(configurations['hive-interactive-site']['properties']['hive.llap.daemon.num.executors'], '25')
    self.assertEqual(configurations['hive-interactive-site']['properties']['hive.llap.io.threadpool.size'], '25')

    self.assertEqual(configurations['hive-interactive-site']['properties']['hive.llap.io.memory.size'], '105472')
    self.assertEqual(configurations['hive-interactive-site']['properties']['hive.llap.io.enabled'], 'true')

    self.assertEqual(configurations['hive-interactive-env']['properties']['llap_heap_size'], '96256')
    self.assertEqual(configurations['hive-interactive-env']['properties']['hive_heapsize'], '3600')
    self.assertEqual(configurations['hive-interactive-env']['property_attributes']['num_llap_nodes'], {'maximum': '9', 'minimum': '1', 'read_only': 'true'})

    self.assertEqual(configurations['hive-interactive-env']['properties']['slider_am_container_mb'], '1024')
    self.assertEqual(configurations['hive-interactive-site']['properties']['hive.auto.convert.join.noconditionaltask.size'], '1145044992')

    self.assertTrue('tez.am.resource.memory.mb' not in configurations['tez-interactive-site']['properties'])
    self.assertEquals(configurations['hive-interactive-site']['property_attributes']['hive.llap.daemon.queue.name'], {'entries': [{'value': 'default', 'label': 'default'}]})


    ###################################################################
    # Test C: 'hive.server2.tez.sessions.per.default.queue' set to = 10
    ###################################################################
    # Set the config
    services['configurations']['hive-interactive-site']['properties']['hive.server2.tez.sessions.per.default.queue'] = 10

    # Test
    self.stackAdvisor.recommendYARNConfigurations(configurations, clusterData, services, self.hosts_9_total)
    self.assertTrue('capacity-scheduler' not in configurations)
    self.assertEquals(configurations['hive-interactive-site']['property_attributes']['hive.server2.tez.sessions.per.default.queue'], {'maximum': '22'})

    self.assertTrue(configurations['hive-interactive-env']['properties']['num_llap_nodes'], 3)
    self.assertTrue('num_llap_nodes_for_llap_daemons' not in configurations['hive-interactive-env']['properties'])

    self.assertEqual(configurations['hive-interactive-site']['properties']['hive.llap.daemon.yarn.container.mb'], '204800')

    self.assertEqual(configurations['hive-interactive-site']['properties']['hive.llap.daemon.num.executors'], '25')
    self.assertEqual(configurations['hive-interactive-site']['properties']['hive.llap.io.threadpool.size'], '25')

    self.assertEqual(configurations['hive-interactive-site']['properties']['hive.llap.io.memory.size'], '102400')
    self.assertEqual(configurations['hive-interactive-site']['properties']['hive.llap.io.enabled'], 'true')

    self.assertEqual(configurations['hive-interactive-env']['properties']['llap_heap_size'], '96256')
    self.assertEqual(configurations['hive-interactive-env']['properties']['hive_heapsize'], '4000')
    self.assertEqual(configurations['hive-interactive-env']['property_attributes']['num_llap_nodes'], {'maximum': '9', 'minimum': '1', 'read_only': 'true'})

    self.assertEqual(configurations['hive-interactive-env']['properties']['slider_am_container_mb'], '1024')
    self.assertEqual(configurations['hive-interactive-site']['properties']['hive.auto.convert.join.noconditionaltask.size'], '1145044992')

    self.assertTrue('tez.am.resource.memory.mb' not in configurations['tez-interactive-site']['properties'])
    self.assertEquals(configurations['hive-interactive-site']['property_attributes']['hive.llap.daemon.queue.name'], {'entries': [{'value': 'default', 'label': 'default'}]})





  # Test 16: (1). only 'default' queue exists at root level in capacity-scheduler, and
  #          'capacity-scheduler' configs are passed-in as single "/n" separated string  and
  #         Expected : 'hive.llap.daemon.queue.name' property attributes getting set with current YARN leaf queues.
  #                    'hive.server2.tez.default.queues' value getting set to value of 'hive.llap.daemon.queue.name' (llap).
  def test_recommendHIVEConfigurations_for_llap_queue_prop_attributes_1(self):
    services = {
      "Versions": {
        "parent_stack_version": "2.4",
        "stack_name": "HDP",
        "stack_version": "2.5",
        "stack_hierarchy": {
          "stack_name": "HDP",
          "stack_versions": ["2.4", "2.3", "2.2", "2.1", "2.0.6"]
        }
      },
      "services": [{
        "StackServices": {
          "service_name": "YARN",
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
                "c6401.ambari.apache.org"
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
              'num_llap_nodes' : '1'
            }
          },
        "hive-interactive-site":
          {
            'properties': {
              'hive.llap.daemon.queue.name': 'llap',
              'hive.server2.tez.sessions.per.default.queue': '1',
              'hive.llap.daemon.num.executors' : '1',
              'hive.llap.daemon.yarn.container.mb' : '10240',
              'hive.llap.io.memory.size' : '512',
              'hive.tez.container.size' : '1024'
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
            "yarn.scheduler.minimum-allocation-mb": "2048",
            "yarn.nodemanager.resource.memory-mb": "204800",
            "yarn.nodemanager.resource.cpu-vcores": '3'
          }
        },
        "tez-interactive-site": {
          "properties": {
            "tez.am.resource.memory.mb": "1024"
          }
        },
        "hive-site":
          {
            'properties': {
              'hive.tez.container.size': '1024'
            }
          },
        "tez-site": {
          "properties": {
            "tez.am.resource.memory.mb": "1024",
            "tez.runtime.sorter.class": "LEGACY"
          }
        },
      }
    }


    clusterData = {
      "cpu": 4,
      "mapMemory": 30000,
      "amMemory": 20000,
      "reduceMemory": 20560,
      "containers": 3,
      "ramPerContainer": 82240,
      "referenceNodeManagerHost" : {
        "total_mem" : 328960 * 1024
      },
      "yarnMinContainerSize": 1024
    }

    configurations = {
    }

    self.stackAdvisor.recommendHIVEConfigurations(configurations, clusterData, services, self.hosts)


    self.assertEquals(configurations['hive-interactive-site']['properties']['hive.llap.daemon.queue.name'],
                      self.expected_hive_interactive_site_llap['hive-interactive-site']['properties']['hive.llap.daemon.queue.name'])
    self.assertEquals(configurations['hive-interactive-site']['properties']['hive.server2.tez.default.queues'], 'llap')
    self.assertEquals(configurations['hive-interactive-env']['property_attributes']['num_llap_nodes'],
                      {'maximum': '1', 'minimum': '1', 'read_only': 'false'})
    self.assertEqual(configurations['hive-interactive-site']['properties']['hive.llap.io.threadpool.size'], '3')

    self.assertEqual(configurations['hive-interactive-site']['properties']['hive.llap.io.memory.size'], '186368')
    self.assertEqual(configurations['hive-interactive-env']['properties']['llap_heap_size'], '9830')
    self.assertEqual(configurations['tez-interactive-site']['properties']['tez.runtime.io.sort.mb'], '1092')
    self.assertEquals(configurations['tez-interactive-site']['property_attributes']['tez.runtime.io.sort.mb'], {'maximum': '1800'})





  # Test 17: (1). only 'default' queue exists at root level in capacity-scheduler, and
  #          'capacity-scheduler' configs are passed-in as single "/n" separated string  and
  #         change in 'hive.llap.daemon.queue.name' value detected.
  #         Expected : 'hive.llap.daemon.queue.name' property attributes getting set with current YARN leaf queues.
  #                    'hive.server2.tez.default.queues' value getting set to value of 'hive.llap.daemon.queue.name' (default).
  def test_recommendHIVEConfigurations_for_llap_queue_prop_attributes_3(self):
    services = {
      "Versions": {
        "parent_stack_version": "2.4",
        "stack_name": "HDP",
        "stack_version": "2.5",
        "stack_hierarchy": {
          "stack_name": "HDP",
          "stack_versions": ["2.4", "2.3", "2.2", "2.1", "2.0.6"]
        }
      },
      "services": [{
        "StackServices": {
          "service_name": "YARN",
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
                "c6401.ambari.apache.org"
              ]
            },
            "dependencies": []
          },
        ]
      }
      ],
      "changed-configurations": [
        {
          u'old_value': u'llap',
          u'type': u'hive-interactive-site',
          u'name': u'hive.llap.daemon.queue.name'
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
              'num_llap_nodes': '1',
              'num_llap_nodes_for_llap_daemons': '0'
            }
          },
        "hive-interactive-site":
          {
            'properties': {
              'hive.llap.daemon.queue.name': 'default',
              'hive.server2.tez.sessions.per.default.queue': '1',
              'hive.llap.daemon.num.executors' : '1',
              'hive.llap.daemon.yarn.container.mb' : '4096',
              'hive.llap.io.memory.size' : '512',
              'hive.tez.container.size':'2048'
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
            "yarn.scheduler.minimum-allocation-mb": "2048",
            "yarn.nodemanager.resource.memory-mb": "204800",
            "yarn.nodemanager.resource.cpu-vcores": '3'
          }
        },
        "tez-interactive-site": {
          "properties": {
            "tez.am.resource.memory.mb": "1024"
          }
        },
        "hive-site":
          {
            'properties': {
              'hive.tez.container.size': '1024'
            }
          },
      }
    }


    clusterData = {
      "cpu": 4,
      "mapMemory": 30000,
      "amMemory": 20000,
      "reduceMemory": 20560,
      "containers": 3,
      "ramPerContainer": 82240,
      "referenceNodeManagerHost" : {
        "total_mem" : 328960 * 1024
      },
      "yarnMinContainerSize": 1024
    }

    configurations = {
    }
    self.stackAdvisor.recommendHIVEConfigurations(configurations, clusterData, services, self.hosts)


    self.assertTrue('hive.llap.daemon.queue.name' not in configurations['hive-interactive-site']['properties'])
    self.assertEqual(configurations['hive-interactive-site']['properties']['hive.server2.tez.sessions.per.default.queue'], '1')
    self.assertEquals(configurations['hive-interactive-site']['property_attributes']['hive.server2.tez.sessions.per.default.queue'], {'maximum': '4', 'minimum': '1'})

    self.assertTrue(configurations['hive-interactive-env']['properties']['num_llap_nodes'], 1)
    self.assertTrue(configurations['hive-interactive-env']['properties']['num_llap_nodes_for_llap_daemons'], 1)

    self.assertEqual(configurations['hive-interactive-site']['properties']['hive.llap.daemon.yarn.container.mb'], '200704')

    self.assertEqual(configurations['hive-interactive-site']['properties']['hive.llap.daemon.num.executors'], '3')
    self.assertEqual(configurations['hive-interactive-site']['properties']['hive.llap.io.threadpool.size'], '3')

    self.assertEqual(configurations['hive-interactive-site']['properties']['hive.llap.io.memory.size'], '194560')
    self.assertEqual(configurations['hive-interactive-site']['properties']['hive.llap.io.enabled'], 'true')

    self.assertEqual(configurations['hive-interactive-env']['properties']['llap_heap_size'], '4915')
    self.assertEqual(configurations['hive-interactive-env']['properties']['hive_heapsize'], '2048')
    self.assertEqual(configurations['hive-interactive-env']['property_attributes']['num_llap_nodes'], {'maximum': '1', 'minimum': '1', 'read_only': 'true'})

    self.assertEqual(configurations['hive-interactive-env']['properties']['slider_am_container_mb'], '2048')
    self.assertEqual(configurations['hive-interactive-site']['properties']['hive.server2.tez.default.queues'], 'default')
    self.assertEqual(configurations['hive-interactive-site']['properties']['hive.auto.convert.join.noconditionaltask.size'], '572522496')

    self.assertTrue('tez.am.resource.memory.mb' not in configurations['tez-interactive-site']['properties'])
    self.assertEquals(configurations['hive-interactive-site']['property_attributes']['hive.llap.daemon.queue.name'], {'entries': [{'value': 'default', 'label': 'default'}]})



  # Test 18: capacity-scheduler malformed as input in services.
  #         Expected : No changes.
  def test_recommendYARNConfigurations_no_update_to_llap_queue_7(self):
    services= {
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


    clusterData = {
      "cpu": 4,
      "mapMemory": 30000,
      "amMemory": 20000,
      "reduceMemory": 20560,
      "containers": 30,
      "ramPerContainer": 512,
      "referenceNodeManagerHost" : {
        "total_mem" : 10240 * 1024
      },
      "yarnMinContainerSize": 512
    }

    configurations = {
    }
    self.stackAdvisor.recommendYARNConfigurations(configurations, clusterData, services, self.hosts)

    # Check output
    self.assertEquals(configurations['capacity-scheduler']['properties'],
                      self.expected_capacity_scheduler_empty['properties'])
    self.assertFalse('hive-interactive-env' in configurations)
    self.assertEquals(configurations['hive-interactive-site']['properties'],
                      self.expected_hive_interactive_site_empty['hive-interactive-site']['properties'])



  def test_recommendAtlasConfigurations(self):
    self.maxDiff = None
    configurations = {
      "application-properties": {
        "properties": {
          "atlas.graph.index.search.solr.zookeeper-url": "",
          "atlas.audit.hbase.zookeeper.quorum": "",
          "atlas.graph.storage.hostname": "",
          "atlas.kafka.bootstrap.servers": "",
          "atlas.kafka.zookeeper.connect": ""
        }
      },
      "infra-solr-env": {
        "properties": {
          "infra_solr_znode": "/infra-solr"
        }
      },
      'ranger-atlas-plugin-properties': {
        'properties': {
          'ranger-atlas-plugin-enabled':'No'
        }
      }
    }
    clusterData = {
      "cpu": 4,
      "mapMemory": 3000,
      "amMemory": 2000,
      "reduceMemory": 2056,
      "containers": 3,
      "ramPerContainer": 256,
      "yarnMinContainerSize": 256
    }
    expected = {
      'application-properties': {
        'properties': {
          'atlas.graph.index.search.solr.zookeeper-url': 'c6401.ambari.apache.org:2181/infra-solr',
          "atlas.audit.hbase.zookeeper.quorum": "c6401.ambari.apache.org",
          "atlas.graph.storage.hostname": "c6401.ambari.apache.org",
          "atlas.kafka.bootstrap.servers": "c6401.ambari.apache.org:6667",
          "atlas.kafka.zookeeper.connect": "c6401.ambari.apache.org",
          "atlas.authorizer.impl": "ranger",
          "atlas.rest.address": "http://c6401.ambari.apache.org:21000"
        }
      },
      "infra-solr-env": {
        "properties": {
          "infra_solr_znode": "/infra-solr"
        }
      },
      'ranger-atlas-plugin-properties': {
        'properties': {
          'ranger-atlas-plugin-enabled':'Yes'
        }
      },
      'atlas-env': {'properties': {}}
    }
    services = {
      "Versions": {
        "parent_stack_version": "2.4",
        "stack_name": "HDP",
        "stack_version": "2.5",
        "stack_hierarchy": {
          "stack_name": "HDP",
          "stack_versions": ["2.4", "2.3", "2.2", "2.1", "2.0.6"]
        }
      },
      "services": [
        {
          "href": "/api/v1/stacks/HDP/versions/2.2/services/AMBARI_INFRA",
          "StackServices": {
            "service_name": "AMBARI_INFRA",
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
                "component_name": "INFRA_SOLR",
                "display_name": "solr",
                "is_client": "false",
                "is_master": "true",
                "hostnames": ["c6401.ambari.apache.org"]
              },
              "dependencies": []
            }
          ]
        },
        {
          "href": "/api/v1/stacks/HDP/versions/2.2/services/ZOOKEEPER",
          "StackServices": {
            "service_name": "ZOOKEEPER",
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
                "component_name": "ZOOKEEPER_SERVER",
                "display_name": "zk",
                "is_client": "false",
                "is_master": "true",
                "hostnames": ["c6401.ambari.apache.org"]
              },
              "dependencies": []
            }
          ]
        },
        {
          "href": "/api/v1/stacks/HDP/versions/2.2/services/HBASE",
          "StackServices": {
            "service_name": "HBASE",
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
                "component_name": "HBASE_MASTER",
                "display_name": "zk",
                "is_client": "false",
                "is_master": "true",
                "hostnames": ["c6401.ambari.apache.org"]
              },
              "dependencies": []
            }
          ]
        },
        {
          "href": "/api/v1/stacks/HDP/versions/2.2/services/ATLAS",
          "StackServices": {
            "service_name": "ATLAS",
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
                "component_name": "ATLAS_SERVER",
                "display_name": "atlas",
                "is_client": "false",
                "is_master": "true",
                "hostnames": ["c6401.ambari.apache.org"]
              },
              "dependencies": []
            }
          ]
        },
        {
          "href": "/api/v1/stacks/HDP/versions/2.2/services/KAFKA",
          "StackServices": {
            "service_name": "KAFKA",
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
                "component_name": "KAFKA_BROKER",
                "display_name": "atlas",
                "is_client": "false",
                "is_master": "true",
                "hostnames": ["c6401.ambari.apache.org"]
              },
              "dependencies": []
            }
          ]
        },
      ],
      "configurations": {
        "application-properties": {
          "properties": {
            "atlas.graph.index.search.solr.zookeeper-url": "",
            "atlas.audit.hbase.zookeeper.quorum": "",
            "atlas.graph.storage.hostname": "",
            "atlas.kafka.bootstrap.servers": "",
            "atlas.kafka.zookeeper.connect": ""
          }
        },
        "infra-solr-env": {
          "properties": {
            "infra_solr_znode": "/infra-solr"
          }
        },
        "hbase-site": {
          "properties": {
            "hbase.zookeeper.quorum": "c6401.ambari.apache.org"
          }
        },
        "kafka-broker": {
          "properties": {
            "zookeeper.connect": "c6401.ambari.apache.org",
            "port": "6667",
            "listeners": "PLAINTEXT://localhost:6667"
          }
        },
        'ranger-atlas-plugin-properties': {
          'properties': {
            'ranger-atlas-plugin-enabled':'No'
          }
        },
        "cluster-env": {
          "properties": {
            "security_enabled": "false"
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
    # test for Ranger Atlas plugin disabled
    self.assertEquals(configurations['application-properties']['properties']['atlas.authorizer.impl'], 'simple', 'Test atlas.authorizer.impl with Ranger Atlas plugin is disabled ')

    configurations['ranger-atlas-plugin-properties']['properties']['ranger-atlas-plugin-enabled'] = 'Yes'
    # configurations['application-properties']['properties']['atlas.authorizer.impl'] =  'ranger'
    self.stackAdvisor.recommendAtlasConfigurations(configurations,clusterData,services,hosts)
    self.assertEquals(configurations, expected)

    services['ambari-server-properties'] = {'java.home': '/usr/jdk64/jdk1.7.3_23'}
    self.stackAdvisor.recommendAtlasConfigurations(configurations, clusterData, services, hosts)
    self.assertEquals(configurations, expected)

    services['configurations']['kafka-broker']['properties']['listeners'] = '  PLAINTEXT://localhost:5522  ,  PLAINTEXTSASL://localhost:2255   '
    expected['application-properties']['properties']['atlas.kafka.bootstrap.servers'] = 'c6401.ambari.apache.org:5522'
    self.stackAdvisor.recommendAtlasConfigurations(configurations, clusterData, services, hosts)
    self.assertEquals(configurations, expected)
    services['configurations']['cluster-env']['properties']['security_enabled']='true'
    services['configurations']['kafka-broker']['properties']['listeners'] = '  PLAINTEXT://localhost:5522  ,  PLAINTEXTSASL://localhost:2266   '
    expected['application-properties']['properties']['atlas.kafka.bootstrap.servers'] = 'c6401.ambari.apache.org:2266'
    self.stackAdvisor.recommendAtlasConfigurations(configurations, clusterData, services, hosts)
    self.assertEquals(configurations, expected)
    services['configurations']['kafka-broker']['properties']['listeners'] = '  SASL_PLAINTEXT://localhost:2233   , PLAINTEXT://localhost:5577  '
    expected['application-properties']['properties']['atlas.kafka.bootstrap.servers'] = 'c6401.ambari.apache.org:2233'
    self.stackAdvisor.recommendAtlasConfigurations(configurations, clusterData, services, hosts)
    self.assertEquals(configurations, expected)

    services['configurations']['cluster-env']['properties']['security_enabled']='false'
    expected['application-properties']['properties']['atlas.kafka.bootstrap.servers'] = 'c6401.ambari.apache.org:5577'
    self.stackAdvisor.recommendAtlasConfigurations(configurations, clusterData, services, hosts)
    self.assertEquals(configurations, expected)


  def test_validationAtlasConfigs(self):
    servicesInfo = [
      {
        "name": "ATLAS",
        "components": []
      }
    ]
    services = self.prepareServices(servicesInfo)
    services["configurations"] = {"application-properties": {"properties": {"atlas.graph.storage.backend": "hbase",
                                                                            "atlas.authentication.method.ldap.type": "",
                                                                            "atlas.graph.index.search.backend": "",
                                                                            "atlas.kafka.bootstrap.servers": "",
                                                                            "atlas.kafka.zookeeper.connect": "",
                                                                            "atlas.graph.storage.hostname": "",
                                                                            "atlas.audit.hbase.zookeeper.quorum": "",
                                                                            "atlas.authentication.method.ldap": "false"
                                                                            }}}
    hosts = self.prepareHosts([])
    result = self.stackAdvisor.validateConfigurations(services, hosts)
    expectedItems = [
      {'message': 'If KAFKA is not installed then the Kafka bootstrap servers configuration must be specified.',
       'level': 'ERROR'},
      {'message': 'If KAFKA is not installed then the Kafka zookeeper quorum configuration must be specified.',
       'level': 'ERROR'},
      {
        'message': 'Atlas is not configured to use the HBase installed in this cluster. If you would like Atlas to use another HBase instance, please configure this property and HBASE_CONF_DIR variable in atlas-env appropriately.',
        'level': 'ERROR'},
      {'message': 'If HBASE is not installed then the audit hbase zookeeper quorum configuration must be specified.',
       'level': 'ERROR'}
    ]
    self.assertValidationResult(expectedItems, result)
    services["configurations"]["hbase-site"] = {"properties": {"hbase.zookeeper.quorum": "h1",}}
    services["configurations"]["application-properties"]["properties"]["atlas.kafka.bootstrap.servers"] = "test"
    services["configurations"]["application-properties"]["properties"]["atlas.kafka.zookeeper.connect"] = "test"
    expectedItems = [
      {'message': 'If HBASE is not installed then the hbase zookeeper quorum configuration must be specified.',
       'level': 'ERROR'},
      {'message': 'If HBASE is not installed then the audit hbase zookeeper quorum configuration must be specified.',
       'level': 'ERROR'}
    ]
    result = self.stackAdvisor.validateConfigurations(services, hosts)
    self.assertValidationResult(expectedItems, result)
    services["configurations"]["application-properties"]["properties"]["atlas.graph.storage.hostname"] = "h1"
    expectedItems = [
      {
        'message': 'Atlas is configured to use the HBase installed in this cluster. If you would like Atlas to use another HBase instance, please configure this property and HBASE_CONF_DIR variable in atlas-env appropriately.',
        'level': 'WARN'},
      {'message': 'If HBASE is not installed then the audit hbase zookeeper quorum configuration must be specified.',
       'level': 'ERROR'}
    ]
    result = self.stackAdvisor.validateConfigurations(services, hosts)
    self.assertValidationResult(expectedItems, result)

  def test_phoenixQueryServerSecureConfigsAppendProxyuser(self):
    self.maxDiff = None
    phoenix_query_server_hosts = ["c6401.ambari.apache.org", "c6402.ambari.apache.org"]
    # Starting configuration
    configurations = {
      "cluster-env": {
        "properties": {
          "security_enabled": "true"
        }
      },
      "core-site": {
        "properties": {
          "hadoop.proxyuser.HTTP.hosts": "c6401.ambari.apache.org",
        }
      },
      "hbase-site": {
        "properties": {}
      }
    }
    # Expected configuration after the recommendation
    expected_configuration = {
      "cluster-env": {
        "properties": {
          "security_enabled": "true"
        }
      },
      "core-site": {
        "properties": {
          "hadoop.proxyuser.HTTP.hosts": "c6401.ambari.apache.org,c6402.ambari.apache.org",
        }
      },
      "hbase-site": {
        "properties": {
          "hbase.master.ui.readonly": "true"
        }
      }
    }

    clusterData = {
      "hbaseRam": 4096,
    }
    services = {
      "Versions": {
        "parent_stack_version": "2.4",
        "stack_name": "HDP",
        "stack_version": "2.5",
        "stack_hierarchy": {
          "stack_name": "HDP",
          "stack_versions": ["2.4", "2.3", "2.2", "2.1", "2.0.6"]
        }
      },
      "services": [
        {
          "href": "/api/v1/stacks/HDP/versions/2.4/services/HBASE",
          "StackServices": {
            "service_name": "HBASE",
          },
          "Versions": {
            "stack_version": "2.5"
          },
          "components": [
            {
              "StackServiceComponents": {
                "component_name": "PHOENIX_QUERY_SERVER",
                "hostnames": phoenix_query_server_hosts
              }
            }
          ]
        },
        {
          "StackServices": {
            "service_name": "KERBEROS",
          },
        },
      ],
      "configurations": configurations,
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
        }, {
          "href" : "/api/v1/hosts/c6402.ambari.apache.org",
          "Hosts" : {
            "cpu_count" : 1,
            "host_name" : "c6402.ambari.apache.org",
            "os_arch" : "x86_64",
            "os_type" : "centos6",
            "ph_cpu_count" : 1,
            "public_host_name" : "c6402.ambari.apache.org",
            "rack_info" : "/default-rack",
            "total_mem" : 1922680
          }
        }, {
          "href" : "/api/v1/hosts/c6403.ambari.apache.org",
          "Hosts" : {
            "cpu_count" : 1,
            "host_name" : "c6403.ambari.apache.org",
            "os_arch" : "x86_64",
            "os_type" : "centos6",
            "ph_cpu_count" : 1,
            "public_host_name" : "c6403.ambari.apache.org",
            "rack_info" : "/default-rack",
            "total_mem" : 1922680
          }
        }
      ]
    }

    self.stackAdvisor.recommendHBASEConfigurations(configurations, clusterData, services, hosts)

    self.assertTrue('core-site' in configurations)
    self.assertTrue('properties' in configurations['core-site'])
    # Avoid an unnecessary sort in the stack advisor, sort here for easy comparison
    actualHosts = configurations['core-site']['properties']['hadoop.proxyuser.HTTP.hosts']
    expectedHosts = expected_configuration['core-site']['properties']['hadoop.proxyuser.HTTP.hosts']
    self.assertEquals(splitAndSort(actualHosts), splitAndSort(expectedHosts))
    # Do a simple check for hbase-site
    self.assertTrue('hbase-site' in configurations)
    self.assertTrue('properties' in configurations['hbase-site'])
    self.assertEquals(configurations['hbase-site']['properties']['hbase.master.ui.readonly'],
        expected_configuration['hbase-site']['properties']['hbase.master.ui.readonly'])

  def test_phoenixQueryServerSecureConfigsNoProxyuser(self):
    self.maxDiff = None
    phoenix_query_server_hosts = ["c6401.ambari.apache.org"]
    # Starting configuration
    configurations = {
      "cluster-env": {
        "properties": {
          "security_enabled": "true"
        }
      },
      "core-site": {
        "properties": {}
      },
      "hbase-site": {
        "properties": {}
      }
    }
    # Expected configuration after the recommendation
    expected_configuration = {
      "cluster-env": {
        "properties": {
          "security_enabled": "true"
        }
      },
      "core-site": {
        "properties": {
          "hadoop.proxyuser.HTTP.hosts": "c6401.ambari.apache.org",
        }
      },
      "hbase-site": {
        "properties": {
          "hbase.master.ui.readonly": "true"
        }
      }
    }

    clusterData = {
      "hbaseRam": 4096,
    }
    services = {
      "Versions": {
        "parent_stack_version": "2.4",
        "stack_name": "HDP",
        "stack_version": "2.5",
        "stack_hierarchy": {
          "stack_name": "HDP",
          "stack_versions": ["2.4", "2.3", "2.2", "2.1", "2.0.6"]
        }
      },
      "services": [
        {
          "href": "/api/v1/stacks/HDP/versions/2.4/services/HBASE",
          "StackServices": {
            "service_name": "HBASE",
          },
          "Versions": {
            "stack_version": "2.5"
          },
          "components": [
            {
              "StackServiceComponents": {
                "component_name": "PHOENIX_QUERY_SERVER",
                "hostnames": phoenix_query_server_hosts
              }
            }
          ]
        },
        {
          "StackServices": {
            "service_name": "KERBEROS",
          },
        },
      ],
      "configurations": configurations,
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
        }, {
          "href" : "/api/v1/hosts/c6402.ambari.apache.org",
          "Hosts" : {
            "cpu_count" : 1,
            "host_name" : "c6402.ambari.apache.org",
            "os_arch" : "x86_64",
            "os_type" : "centos6",
            "ph_cpu_count" : 1,
            "public_host_name" : "c6402.ambari.apache.org",
            "rack_info" : "/default-rack",
            "total_mem" : 1922680
          }
        }, {
          "href" : "/api/v1/hosts/c6403.ambari.apache.org",
          "Hosts" : {
            "cpu_count" : 1,
            "host_name" : "c6403.ambari.apache.org",
            "os_arch" : "x86_64",
            "os_type" : "centos6",
            "ph_cpu_count" : 1,
            "public_host_name" : "c6403.ambari.apache.org",
            "rack_info" : "/default-rack",
            "total_mem" : 1922680
          }
        }
      ]
    }

    self.stackAdvisor.recommendHBASEConfigurations(configurations, clusterData, services, hosts)

    self.assertTrue('core-site' in configurations)
    self.assertTrue('properties' in configurations['core-site'])
    self.assertTrue('hadoop.proxyuser.HTTP.hosts' in configurations['core-site']['properties'])
    # Avoid an unnecessary sort in the stack advisor, sort here for easy comparison
    actualHosts = configurations['core-site']['properties']['hadoop.proxyuser.HTTP.hosts']
    expectedHosts = expected_configuration['core-site']['properties']['hadoop.proxyuser.HTTP.hosts']
    self.assertEquals(splitAndSort(actualHosts), splitAndSort(expectedHosts))
    # Do a simple check for hbase-site
    self.assertTrue('hbase-site' in configurations)
    self.assertTrue('properties' in configurations['hbase-site'])
    self.assertEquals(configurations['hbase-site']['properties']['hbase.master.ui.readonly'],
        expected_configuration['hbase-site']['properties']['hbase.master.ui.readonly'])

  def test_phoenixQueryServerSecureConfigsAppendProxyuser(self):
    self.maxDiff = None
    phoenix_query_server_hosts = ["c6402.ambari.apache.org"]
    # Starting configuration
    configurations = {
      "cluster-env": {
        "properties": {
          "security_enabled": "true"
        }
      },
      "core-site": {
        "properties": {
          "hadoop.proxyuser.HTTP.hosts": "c6401.ambari.apache.org",
        }
      },
      "hbase-site": {
        "properties": {}
      }
    }
    # Expected configuration after the recommendation
    expected_configuration = {
      "cluster-env": {
        "properties": {
          "security_enabled": "true"
        }
      },
      "core-site": {
        "properties": {
          "hadoop.proxyuser.HTTP.hosts": "c6401.ambari.apache.org,c6402.ambari.apache.org",
        }
      },
      "hbase-site": {
        "properties": {
          "hbase.master.ui.readonly": "true"
        }
      }
    }

    clusterData = {
      "hbaseRam": 4096,
    }
    services = {
      "Versions": {
        "parent_stack_version": "2.4",
        "stack_name": "HDP",
        "stack_version": "2.5",
        "stack_hierarchy": {
          "stack_name": "HDP",
          "stack_versions": ["2.4", "2.3", "2.2", "2.1", "2.0.6"]
        }
      },
      "services": [
        {
          "href": "/api/v1/stacks/HDP/versions/2.4/services/HBASE",
          "StackServices": {
            "service_name": "HBASE",
          },
          "Versions": {
            "stack_version": "2.5"
          },
          "components": [
            {
              "StackServiceComponents": {
                "component_name": "PHOENIX_QUERY_SERVER",
                "hostnames": phoenix_query_server_hosts
              }
            }
          ]
        },
        {
          "StackServices": {
            "service_name": "KERBEROS",
          },
        },
      ],
      "configurations": configurations,
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
        }, {
          "href" : "/api/v1/hosts/c6402.ambari.apache.org",
          "Hosts" : {
            "cpu_count" : 1,
            "host_name" : "c6402.ambari.apache.org",
            "os_arch" : "x86_64",
            "os_type" : "centos6",
            "ph_cpu_count" : 1,
            "public_host_name" : "c6402.ambari.apache.org",
            "rack_info" : "/default-rack",
            "total_mem" : 1922680
          }
        }, {
          "href" : "/api/v1/hosts/c6403.ambari.apache.org",
          "Hosts" : {
            "cpu_count" : 1,
            "host_name" : "c6403.ambari.apache.org",
            "os_arch" : "x86_64",
            "os_type" : "centos6",
            "ph_cpu_count" : 1,
            "public_host_name" : "c6403.ambari.apache.org",
            "rack_info" : "/default-rack",
            "total_mem" : 1922680
          }
        }
      ]
    }

    self.stackAdvisor.recommendHBASEConfigurations(configurations, clusterData, services, hosts)

    self.assertTrue('core-site' in configurations)
    self.assertTrue('properties' in configurations['core-site'])
    # Avoid an unnecessary sort in the stack advisor, sort here for easy comparison
    actualHosts = configurations['core-site']['properties']['hadoop.proxyuser.HTTP.hosts']
    expectedHosts = expected_configuration['core-site']['properties']['hadoop.proxyuser.HTTP.hosts']
    self.assertEquals(splitAndSort(actualHosts), splitAndSort(expectedHosts))
    # Do a simple check for hbase-site
    self.assertTrue('hbase-site' in configurations)
    self.assertTrue('properties' in configurations['hbase-site'])
    self.assertEquals(configurations['hbase-site']['properties']['hbase.master.ui.readonly'],
        expected_configuration['hbase-site']['properties']['hbase.master.ui.readonly'])

  def test_validationYARNServicecheckQueueName(self):
    servicesInfo = [
      {
        "name": "YARN",
        "components": []
      }
    ]
    services = self.prepareServices(servicesInfo)
    services["configurations"] = {"yarn-env":{"properties":{"service_check.queue.name": "default"}},
                                  "capacity-scheduler":{"properties":{
                                    "capacity-scheduler": "yarn.scheduler.capacity.root.queues=ndfqueue,leaf\n" +
                                                          "yarn.scheduler.capacity.root.ndfqueue.queues=ndfqueue1,ndfqueue2\n"}}}
    services["changed-configurations"]= []


    hosts = self.prepareHosts([])
    result = self.stackAdvisor.validateConfigurations(services, hosts)
    expectedItems = [
      {'message': 'Queue is not exist or not corresponds to existing YARN leaf queue', 'level': 'ERROR'}
    ]
    self.assertValidationResult(expectedItems, result)
    services["configurations"]["yarn-env"]["properties"]["service_check.queue.name"] = "ndfqueue2"
    expectedItems = []
    result = self.stackAdvisor.validateConfigurations(services, hosts)
    self.assertValidationResult(expectedItems, result)
    services["configurations"]["yarn-env"]["properties"]["service_check.queue.name"] = "leaf"
    expectedItems = []
    result = self.stackAdvisor.validateConfigurations(services, hosts)
    self.assertValidationResult(expectedItems, result)

  def test_recommendYARNQueueConfigurations(self):
    configurations = {"yarn-env":{"properties":{"service_check.queue.name": "default"}},
                      "capacity-scheduler":{"properties":{
                        "capacity-scheduler": "yarn.scheduler.capacity.root.queues=ndfqueue\n" +
                                              "yarn.scheduler.capacity.root.ndfqueue.queues=ndfqueue1,ndfqueue2\n"}}}
    services = {
      "Versions": {
        "parent_stack_version": "2.4",
        "stack_name": "HDP",
        "stack_version": "2.5",
        "stack_hierarchy": {
          "stack_name": "HDP",
          "stack_versions": ["2.4", "2.3", "2.2", "2.1", "2.0.6"]
        }
      },
      "changed-configurations": [
      ],
      "configurations": configurations,
      "services": [],
      "ambari-server-properties": {}}
    clusterData = {
      "containers" : 5,
      "ramPerContainer": 256,
      "mapMemory": 567,
      "reduceMemory": 345.6666666666666,
      "amMemory": 123.54,
      "cpu": 4,
      "referenceNodeManagerHost" : {
        "total_mem" : 328960 * 1024
      },
      "yarnMinContainerSize": 256
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

    self.stackAdvisor.recommendYARNConfigurations(configurations, clusterData, services, hosts)
    self.stackAdvisor.recommendMapReduce2Configurations(configurations, clusterData, services, hosts)
    self.stackAdvisor.recommendHIVEConfigurations(configurations, clusterData, services, hosts)
    self.stackAdvisor.recommendTezConfigurations(configurations, clusterData, services, hosts)
    self.assertEquals(configurations["yarn-env"]["properties"]["service_check.queue.name"], "ndfqueue2")
    self.assertEquals(configurations["mapred-site"]["properties"]["mapreduce.job.queuename"], "ndfqueue2")
    self.assertEquals(configurations["webhcat-site"]["properties"]["templeton.hadoop.queue.name"], "ndfqueue2")
    self.assertEquals(configurations["tez-site"]["properties"]["tez.queue.name"], "ndfqueue2")

  def assertValidationResult(self, expectedItems, result):
    actualItems = []
    for item in result["items"]:
      next = {"message": item["message"], "level": item["level"]}
      try:
        next["host"] = item["host"]
      except KeyError, err:
        pass
      actualItems.append(next)
    self.checkEqual(expectedItems, actualItems)

  def checkEqual(self, l1, l2):
    if not len(l1) == len(l2) or not sorted(l1) == sorted(l2):
      raise AssertionError("list1={0}, list2={1}".format(l1, l2))

  def prepareServices(self, servicesInfo):
    services = { "Versions" : { "stack_name" : "HDP", "stack_version" : "2.5" } }
    services["services"] = []

    for serviceInfo in servicesInfo:
      nextService = {"StackServices":{"service_name" : serviceInfo["name"]}}
      nextService["components"] = []
      for component in serviceInfo["components"]:
        nextComponent = {
          "StackServiceComponents": {
            "component_name": component["name"],
            "cardinality": component["cardinality"],
            "component_category": component["category"],
            "is_master": component["is_master"]
          }
        }
        try:
          nextComponent["StackServiceComponents"]["hostnames"] = component["hostnames"]
        except KeyError:
          nextComponent["StackServiceComponents"]["hostnames"] = []
        try:
          nextComponent["StackServiceComponents"]["display_name"] = component["display_name"]
        except KeyError:
          nextComponent["StackServiceComponents"]["display_name"] = component["name"]
        nextService["components"].append(nextComponent)
      services["services"].append(nextService)

    return services

  def test_phoenixQueryServerNoChangesWithUnsecure(self):
    self.maxDiff = None
    phoenix_query_server_hosts = ["c6402.ambari.apache.org"]
    # Starting configuration
    configurations = {
      "cluster-env": {
        "properties": {}
      },
      "core-site": {
        "properties": {}
      },
      "hbase-site": {
        "properties": {}
      }
    }

    clusterData = {
      "hbaseRam": 4096,
    }
    services = {
      "Versions": {
        "parent_stack_version": "2.4",
        "stack_name": "HDP",
        "stack_version": "2.5",
        "stack_hierarchy": {
          "stack_name": "HDP",
          "stack_versions": ["2.4", "2.3", "2.2", "2.1", "2.0.6"]
        }
      },
      "services": [
        {
          "href": "/api/v1/stacks/HDP/versions/2.4/services/HBASE",
          "StackServices": {
            "service_name": "HBASE",
          },
          "Versions": {
            "stack_version": "2.5"
          },
          "components": [
            {
              "StackServiceComponents": {
                "component_name": "PHOENIX_QUERY_SERVER",
                "hostnames": phoenix_query_server_hosts
              }
            }
          ]
        },
      ],
      "configurations": configurations,
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
        }, {
          "href" : "/api/v1/hosts/c6402.ambari.apache.org",
          "Hosts" : {
            "cpu_count" : 1,
            "host_name" : "c6402.ambari.apache.org",
            "os_arch" : "x86_64",
            "os_type" : "centos6",
            "ph_cpu_count" : 1,
            "public_host_name" : "c6402.ambari.apache.org",
            "rack_info" : "/default-rack",
            "total_mem" : 1922680
          }
        }, {
          "href" : "/api/v1/hosts/c6403.ambari.apache.org",
          "Hosts" : {
            "cpu_count" : 1,
            "host_name" : "c6403.ambari.apache.org",
            "os_arch" : "x86_64",
            "os_type" : "centos6",
            "ph_cpu_count" : 1,
            "public_host_name" : "c6403.ambari.apache.org",
            "rack_info" : "/default-rack",
            "total_mem" : 1922680
          }
        }
      ]
    }

    self.stackAdvisor.recommendHBASEConfigurations(configurations, clusterData, services, hosts)

    self.assertTrue('core-site' in configurations)
    self.assertTrue('properties' in configurations['core-site'])
    # Should have no updates for core-site for unsecure
    self.assertFalse('hadoop.proxuser.HTTP.hosts' in configurations['core-site']['properties'])
    # Should have no update to hbase-site for unsecure
    self.assertTrue('hbase-site' in configurations)
    self.assertTrue('properties' in configurations['hbase-site'])
    self.assertFalse('hbase.master.ui.readonly' in configurations['hbase-site']['properties']['hbase.master.ui.readonly'])

  def test_obtainPhoenixQueryServerHosts(self):
    self.maxDiff = None
    phoenix_query_server_hosts = ["c6402.ambari.apache.org"]
    # Starting configuration
    configurations = {
      "cluster-env": {
        "properties": {
          "security_enabled": "true"
        }
      },
      "core-site": {
        "properties": {
          "hadoop.proxyuser.HTTP.hosts": "c6401.ambari.apache.org",
        }
      },
      "hbase-site": {
        "properties": {}
      }
    }
    # Expected configuration after the recommendation
    expected_configuration = {
      "cluster-env": {
        "properties": {
          "security_enabled": "true"
        }
      },
      "core-site": {
        "properties": {
          "hadoop.proxyuser.HTTP.hosts": "c6401.ambari.apache.org,c6402.ambari.apache.org",
        }
      },
      "hbase-site": {
        "properties": {
          "hbase.master.ui.readonly": "true"
        }
      }
    }

    clusterData = {
      "hbaseRam": 4096,
    }
    services = {
      "Versions": {
        "parent_stack_version": "2.4",
        "stack_name": "HDP",
        "stack_version": "2.5",
        "stack_hierarchy": {
          "stack_name": "HDP",
          "stack_versions": ["2.4", "2.3", "2.2", "2.1", "2.0.6"]
        }
      },
      "services": [
        {
          "href": "/api/v1/stacks/HDP/versions/2.4/services/HBASE",
          "StackServices": {
            "service_name": "HBASE",
          },
          "Versions": {
            "stack_version": "2.5"
          },
          "components": [
            {
              "StackServiceComponents": {
                "component_name": "PHOENIX_QUERY_SERVER",
                "hostnames": phoenix_query_server_hosts
              }
            }
          ]
        },
      ],
      "configurations": configurations,
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
        }, {
          "href" : "/api/v1/hosts/c6402.ambari.apache.org",
          "Hosts" : {
            "cpu_count" : 1,
            "host_name" : "c6402.ambari.apache.org",
            "os_arch" : "x86_64",
            "os_type" : "centos6",
            "ph_cpu_count" : 1,
            "public_host_name" : "c6402.ambari.apache.org",
            "rack_info" : "/default-rack",
            "total_mem" : 1922680
          }
        }, {
          "href" : "/api/v1/hosts/c6403.ambari.apache.org",
          "Hosts" : {
            "cpu_count" : 1,
            "host_name" : "c6403.ambari.apache.org",
            "os_arch" : "x86_64",
            "os_type" : "centos6",
            "ph_cpu_count" : 1,
            "public_host_name" : "c6403.ambari.apache.org",
            "rack_info" : "/default-rack",
            "total_mem" : 1922680
          }
        }
      ]
    }

    self.assertEquals(self.stackAdvisor.get_phoenix_query_server_hosts(services, hosts),
        phoenix_query_server_hosts)

    phoenix_query_server_hosts = []
    services['services'][0]['components'][0]['StackServiceComponents']['hostnames'] = phoenix_query_server_hosts

    self.assertEquals(self.stackAdvisor.get_phoenix_query_server_hosts(services, hosts),
        phoenix_query_server_hosts)

  def test_recommendStormConfigurations(self):
    configurations = {}
    clusterData = {}
    services = {
      "services":
        [
          {
            "StackServices": {
              "service_name" : "STORM",
              "service_version" : "1.0.1.0.0"
            }
          },
          {
            "StackServices": {
              "service_name": "RANGER",
              "service_version": "0.6.0"

            },
            "components": [
              {
                "StackServiceComponents": {
                  "component_name": "RANGER_ADMIN",
                  "hostnames": ["host1"]
                }
              }
            ]
          }
        ],
      "Versions": {
        "stack_version": "2.5"
      },
      "configurations": {
        "storm-site": {
          "properties": {
            "nimbus.authorizer" : "org.apache.storm.security.auth.authorizer.SimpleACLAuthorizer",
            "nimbus.impersonation.acl" :"{{{storm_bare_jaas_principal}} : {hosts: ['*'], groups: ['*']}}"
            },
          "property_attributes": {}
        },
        "storm-env": {
          "properties":{
            "storm_principal_name": "storm_user@ECAMPLE.COM"
          },
        },
        "ranger-storm-plugin-properties": {
          "properties": {
            "ranger-storm-plugin-enabled": "No"
          }
        }
      }
    }

    # Test nimbus.authorizer with Ranger Storm plugin disabled in non-kerberos environment
    self.stackAdvisor.recommendStormConfigurations(configurations, clusterData, services, None)
    self.assertEquals(configurations['storm-site']['property_attributes']['nimbus.authorizer'], {'delete': 'true'}, "Test nimbus.authorizer with Ranger Storm plugin disabled in non-kerberos environment")
    self.assertEquals(configurations['storm-site']['properties']['storm.cluster.metrics.consumer.register'], 'null')
    self.assertEquals(configurations['storm-site']['properties']['topology.metrics.consumer.register'], 'null')

    services = {
      "services":
        [
          {
            "StackServices": {
              "service_name" : "STORM",
              "service_version" : "1.0.1.0.0"
            }
          },
          {
            "StackServices": {
              "service_name": "RANGER",
              "service_version": "0.6.0"

            },
            "components": [
              {
                "StackServiceComponents": {
                  "component_name": "RANGER_ADMIN",
                  "hostnames": ["host1"]
                }
              }
            ]
          },
          {
            "StackServices": {
              "service_name": "AMBARI_METRICS"
            },
            "components": [{
              "StackServiceComponents": {
                "component_name": "METRICS_COLLECTOR",
                "hostnames": ["host1"]
              }

            }, {
              "StackServiceComponents": {
                "component_name": "METRICS_MONITOR",
                "hostnames": ["host1"]
              }

            }]
          }
        ],
      "Versions": {
        "stack_version": "2.5"
      },
      "configurations": {
        "cluster-env": {
          "properties": {
            "security_enabled" : "true"
          }
        },
        "storm-site": {
          "properties": {
            "nimbus.authorizer" : "org.apache.storm.security.auth.authorizer.SimpleACLAuthorizer",
            "nimbus.impersonation.acl" :"{{{storm_bare_jaas_principal}} : {hosts: ['*'], groups: ['*']}}"
          },
          "property_attributes": {}
        },
        "storm-env": {
          "properties":{
            "storm_principal_name": "storm_user@ECAMPLE.COM"
          },
        },
        "ranger-storm-plugin-properties": {
          "properties": {
            "ranger-storm-plugin-enabled": "No"
          }
        }
      }
    }

    # Test nimbus.authorizer with Ranger Storm plugin enabled in non-kerberos environment
    configurations['storm-site']['properties'] = {}
    configurations['storm-site']['property_attributes'] = {}
    services['configurations']['ranger-storm-plugin-properties']['properties']['ranger-storm-plugin-enabled'] = 'Yes'
    services['configurations']['cluster-env']['properties']['security_enabled'] = 'false'
    self.stackAdvisor.recommendStormConfigurations(configurations, clusterData, services, None)
    self.assertEquals(configurations['storm-site']['property_attributes']['nimbus.authorizer'], {'delete': 'true'}, "Test nimbus.authorizer with Ranger Storm plugin enabled in non-kerberos environment")
    self.assertEquals(configurations['storm-site']['properties']['storm.cluster.metrics.consumer.register'], '[{"class": "org.apache.hadoop.metrics2.sink.storm.StormTimelineMetricsReporter"}]')
    self.assertEquals(configurations['storm-site']['properties']['topology.metrics.consumer.register'], '[{"class": "org.apache.hadoop.metrics2.sink.storm.StormTimelineMetricsSink", '
                                                                                                      '"parallelism.hint": 1, '
                                                                                                      '"whitelist": ["kafkaOffset\\\..+/", "__complete-latency", "__process-latency", '
                                                                                                      '"__execute-latency", '
                                                                                                      '"__receive\\\.population$", "__sendqueue\\\.population$", "__execute-count", "__emit-count", '
                                                                                                      '"__ack-count", "__fail-count", "memory/heap\\\.usedBytes$", "memory/nonHeap\\\.usedBytes$", '
                                                                                                      '"GC/.+\\\.count$", "GC/.+\\\.timeMs$"]}]')

    # Test nimbus.authorizer with Ranger Storm plugin being enabled in kerberos environment
    configurations['storm-site']['properties'] = {}
    configurations['storm-site']['property_attributes'] = {}
    services['configurations']['storm-site']['properties']['nimbus.authorizer'] = ''
    services['configurations']['ranger-storm-plugin-properties']['properties']['ranger-storm-plugin-enabled'] = 'Yes'
    services['configurations']['storm-site']['properties']['storm.zookeeper.superACL'] = 'sasl:{{storm_bare_jaas_principal}}'
    services['configurations']['cluster-env']['properties']['security_enabled'] = 'true'
    self.stackAdvisor.recommendStormConfigurations(configurations, clusterData, services, None)
    self.assertEquals(configurations['storm-site']['properties']['nimbus.authorizer'], 'org.apache.ranger.authorization.storm.authorizer.RangerStormAuthorizer', "Test nimbus.authorizer with Ranger Storm plugin enabled in kerberos environment")

    # Test nimbus.authorizer with Ranger Storm plugin being disabled in kerberos environment
    configurations['storm-site']['properties'] = {}
    configurations['storm-site']['property_attributes'] = {}
    services['configurations']['ranger-storm-plugin-properties']['properties']['ranger-storm-plugin-enabled'] = 'No'
    services['configurations']['storm-site']['properties']['storm.zookeeper.superACL'] = 'sasl:{{storm_bare_jaas_principal}}'
    services['configurations']['storm-site']['properties']['nimbus.authorizer'] = 'org.apache.ranger.authorization.storm.authorizer.RangerStormAuthorizer'
    services['configurations']['cluster-env']['properties']['security_enabled'] = 'true'
    self.stackAdvisor.recommendStormConfigurations(configurations, clusterData, services, None)
    self.assertEquals(configurations['storm-site']['properties']['nimbus.authorizer'], 'org.apache.storm.security.auth.authorizer.SimpleACLAuthorizer', "Test nimbus.authorizer with Ranger Storm plugin being disabled in kerberos environment")

  def test_validateSpark2Defaults(self):
    properties = {}
    recommendedDefaults = {
      "spark.yarn.queue": "default",
    }
    configurations = {}
    services = {
      "services":
        [
          {
            "StackServices": {
              "service_name": "SPARK"
            }
          }
        ]
    }

    res_expected = []

    res = self.stackAdvisor.validateSpark2Defaults(properties, recommendedDefaults, configurations, services, {})
    self.assertEquals(res_expected, res)


  def test_recommendOozieConfigurations_noFalconServer(self):
    configurations = {}
    clusterData = {
      "components" : []
    }
    services = {
      "services": [
        {
          "StackServices": {
            "service_name": "OOZIE"
          }, "components": []
        },
        ],
      "configurations": configurations,
      "forced-configurations": []
    }
    expected = {
      "oozie-site": {"properties":{}, 'property_attributes':
        {'oozie.service.ELService.ext.functions.workflow': {'delete': 'true'},
         'oozie.service.ELService.ext.functions.coord-job-submit-instances': {'delete': 'true'},
         'oozie.service.ELService.ext.functions.coord-action-start': {'delete': 'true'},
         'oozie.service.ELService.ext.functions.coord-job-submit-data': {'delete': 'true'},
         'oozie.service.ELService.ext.functions.coord-sla-submit': {'delete': 'true'},
         'oozie.service.ELService.ext.functions.coord-action-create': {'delete': 'true'},
         'oozie.service.ELService.ext.functions.coord-action-create-inst': {'delete': 'true'},
         'oozie.service.ELService.ext.functions.coord-sla-create': {'delete': 'true'},
         'oozie.service.HadoopAccessorService.supported.filesystems': {'delete': 'true'}}},
      "oozie-env": {"properties":{}}
    }

    self.stackAdvisor.recommendOozieConfigurations(configurations, clusterData, services, None)
    self.assertEquals(configurations, expected)


  def test_recommendOozieConfigurations_withFalconServer(self):
    configurations = {
      "falcon-env" : {
        "properties" : {
          "falcon_user" : "falcon"
        }
      }
    }

    services = {
      "services": [
        {
          "StackServices": {
            "service_name": "FALCON"
          }, "components": []
        },
      ],
      "configurations": configurations,
      "forced-configurations": []
    }

    clusterData = {
      "components" : ["FALCON_SERVER"]
    }
    expected = {
      "oozie-site": {
        "properties": {
          "oozie.service.ELService.ext.functions.coord-action-create" : 'now=org.apache.oozie.extensions.OozieELExtensions#ph2_now, \
                            today=org.apache.oozie.extensions.OozieELExtensions#ph2_today, \
                            yesterday=org.apache.oozie.extensions.OozieELExtensions#ph2_yesterday, \
                            currentWeek=org.apache.oozie.extensions.OozieELExtensions#ph2_currentWeek, \
                            lastWeek=org.apache.oozie.extensions.OozieELExtensions#ph2_lastWeek, \
                            currentMonth=org.apache.oozie.extensions.OozieELExtensions#ph2_currentMonth, \
                            lastMonth=org.apache.oozie.extensions.OozieELExtensions#ph2_lastMonth, \
                            currentYear=org.apache.oozie.extensions.OozieELExtensions#ph2_currentYear, \
                            lastYear=org.apache.oozie.extensions.OozieELExtensions#ph2_lastYear, \
                            latest=org.apache.oozie.coord.CoordELFunctions#ph2_coord_latest_echo, \
                            future=org.apache.oozie.coord.CoordELFunctions#ph2_coord_future_echo, \
                            formatTime=org.apache.oozie.coord.CoordELFunctions#ph2_coord_formatTime, \
                            user=org.apache.oozie.coord.CoordELFunctions#coord_user',
          "oozie.service.ELService.ext.functions.coord-action-create-inst" : 'now=org.apache.oozie.extensions.OozieELExtensions#ph2_now_inst, \
                            today=org.apache.oozie.extensions.OozieELExtensions#ph2_today_inst, \
                            yesterday=org.apache.oozie.extensions.OozieELExtensions#ph2_yesterday_inst, \
                            currentWeek=org.apache.oozie.extensions.OozieELExtensions#ph2_currentWeek_inst, \
                            lastWeek=org.apache.oozie.extensions.OozieELExtensions#ph2_lastWeek_inst, \
                            currentMonth=org.apache.oozie.extensions.OozieELExtensions#ph2_currentMonth_inst, \
                            lastMonth=org.apache.oozie.extensions.OozieELExtensions#ph2_lastMonth_inst, \
                            currentYear=org.apache.oozie.extensions.OozieELExtensions#ph2_currentYear_inst, \
                            lastYear=org.apache.oozie.extensions.OozieELExtensions#ph2_lastYear_inst, \
                            latest=org.apache.oozie.coord.CoordELFunctions#ph2_coord_latest_echo, \
                            future=org.apache.oozie.coord.CoordELFunctions#ph2_coord_future_echo, \
                            formatTime=org.apache.oozie.coord.CoordELFunctions#ph2_coord_formatTime, \
                            user=org.apache.oozie.coord.CoordELFunctions#coord_user',
          "oozie.service.ELService.ext.functions.coord-action-start" : 'now=org.apache.oozie.extensions.OozieELExtensions#ph2_now, \
                            today=org.apache.oozie.extensions.OozieELExtensions#ph2_today, \
                            yesterday=org.apache.oozie.extensions.OozieELExtensions#ph2_yesterday, \
                            currentWeek=org.apache.oozie.extensions.OozieELExtensions#ph2_currentWeek, \
                            lastWeek=org.apache.oozie.extensions.OozieELExtensions#ph2_lastWeek, \
                            currentMonth=org.apache.oozie.extensions.OozieELExtensions#ph2_currentMonth, \
                            lastMonth=org.apache.oozie.extensions.OozieELExtensions#ph2_lastMonth, \
                            currentYear=org.apache.oozie.extensions.OozieELExtensions#ph2_currentYear, \
                            lastYear=org.apache.oozie.extensions.OozieELExtensions#ph2_lastYear, \
                            latest=org.apache.oozie.coord.CoordELFunctions#ph3_coord_latest, \
                            future=org.apache.oozie.coord.CoordELFunctions#ph3_coord_future, \
                            dataIn=org.apache.oozie.extensions.OozieELExtensions#ph3_dataIn, \
                            instanceTime=org.apache.oozie.coord.CoordELFunctions#ph3_coord_nominalTime, \
                            dateOffset=org.apache.oozie.coord.CoordELFunctions#ph3_coord_dateOffset, \
                            formatTime=org.apache.oozie.coord.CoordELFunctions#ph3_coord_formatTime, \
                            user=org.apache.oozie.coord.CoordELFunctions#coord_user',
          "oozie.service.ELService.ext.functions.coord-job-submit-data" : 'now=org.apache.oozie.extensions.OozieELExtensions#ph1_now_echo, \
                            today=org.apache.oozie.extensions.OozieELExtensions#ph1_today_echo, \
                            yesterday=org.apache.oozie.extensions.OozieELExtensions#ph1_yesterday_echo, \
                            currentWeek=org.apache.oozie.extensions.OozieELExtensions#ph1_currentWeek_echo, \
                            lastWeek=org.apache.oozie.extensions.OozieELExtensions#ph1_lastWeek_echo, \
                            currentMonth=org.apache.oozie.extensions.OozieELExtensions#ph1_currentMonth_echo, \
                            lastMonth=org.apache.oozie.extensions.OozieELExtensions#ph1_lastMonth_echo, \
                            currentYear=org.apache.oozie.extensions.OozieELExtensions#ph1_currentYear_echo, \
                            lastYear=org.apache.oozie.extensions.OozieELExtensions#ph1_lastYear_echo, \
                            dataIn=org.apache.oozie.extensions.OozieELExtensions#ph1_dataIn_echo, \
                            instanceTime=org.apache.oozie.coord.CoordELFunctions#ph1_coord_nominalTime_echo_wrap, \
                            formatTime=org.apache.oozie.coord.CoordELFunctions#ph1_coord_formatTime_echo, \
                            dateOffset=org.apache.oozie.coord.CoordELFunctions#ph1_coord_dateOffset_echo, \
                            user=org.apache.oozie.coord.CoordELFunctions#coord_user',
          "oozie.service.ELService.ext.functions.coord-job-submit-instances" : 'now=org.apache.oozie.extensions.OozieELExtensions#ph1_now_echo, \
                            today=org.apache.oozie.extensions.OozieELExtensions#ph1_today_echo, \
                            yesterday=org.apache.oozie.extensions.OozieELExtensions#ph1_yesterday_echo,\
                            currentWeek=org.apache.oozie.extensions.OozieELExtensions#ph1_currentWeek_echo, \
                            lastWeek=org.apache.oozie.extensions.OozieELExtensions#ph1_lastWeek_echo, \
                            currentMonth=org.apache.oozie.extensions.OozieELExtensions#ph1_currentMonth_echo, \
                            lastMonth=org.apache.oozie.extensions.OozieELExtensions#ph1_lastMonth_echo, \
                            currentYear=org.apache.oozie.extensions.OozieELExtensions#ph1_currentYear_echo, \
                            lastYear=org.apache.oozie.extensions.OozieELExtensions#ph1_lastYear_echo, \
                            formatTime=org.apache.oozie.coord.CoordELFunctions#ph1_coord_formatTime_echo, \
                            latest=org.apache.oozie.coord.CoordELFunctions#ph2_coord_latest_echo, \
                            future=org.apache.oozie.coord.CoordELFunctions#ph2_coord_future_echo',
          "oozie.service.ELService.ext.functions.coord-sla-create" : 'instanceTime=org.apache.oozie.coord.CoordELFunctions#ph2_coord_nominalTime, \
                            user=org.apache.oozie.coord.CoordELFunctions#coord_user',
          "oozie.service.ELService.ext.functions.coord-sla-submit" : 'instanceTime=org.apache.oozie.coord.CoordELFunctions#ph1_coord_nominalTime_echo_fixed, \
                            user=org.apache.oozie.coord.CoordELFunctions#coord_user',
          'oozie.service.ELService.ext.functions.workflow' : 'now=org.apache.oozie.extensions.OozieELExtensions#ph1_now_echo, \
                            today=org.apache.oozie.extensions.OozieELExtensions#ph1_today_echo, \
                            yesterday=org.apache.oozie.extensions.OozieELExtensions#ph1_yesterday_echo, \
                            currentMonth=org.apache.oozie.extensions.OozieELExtensions#ph1_currentMonth_echo, \
                            lastMonth=org.apache.oozie.extensions.OozieELExtensions#ph1_lastMonth_echo, \
                            currentYear=org.apache.oozie.extensions.OozieELExtensions#ph1_currentYear_echo, \
                            lastYear=org.apache.oozie.extensions.OozieELExtensions#ph1_lastYear_echo, \
                            formatTime=org.apache.oozie.coord.CoordELFunctions#ph1_coord_formatTime_echo, \
                            latest=org.apache.oozie.coord.CoordELFunctions#ph2_coord_latest_echo, \
                            future=org.apache.oozie.coord.CoordELFunctions#ph2_coord_future_echo',
          "oozie.service.HadoopAccessorService.supported.filesystems" : "*",
          "oozie.services.ext": "org.apache.oozie.service.JMSAccessorService," +
                                "org.apache.oozie.service.PartitionDependencyManagerService," +
                                "org.apache.oozie.service.HCatAccessorService",
          "oozie.service.ProxyUserService.proxyuser.falcon.groups" : "*",
          "oozie.service.ProxyUserService.proxyuser.falcon.hosts" : "*"
        }
      },
      "falcon-env" : {
        "properties" : {
          "falcon_user" : "falcon"
        }
      },
      "oozie-env": {
        "properties": {'oozie_admin_users': 'oozie, oozie-admin,falcon'}
      }
    }

    self.stackAdvisor.recommendOozieConfigurations(configurations, clusterData, services, None)
    self.assertEquals(configurations, expected)


"""
Given a comma-separated string, split the items, sort them, and re-join the elements
back into a comma-separated string
"""
def splitAndSort(s):
  l = s.split(',')
  l.sort()
  return ','.join(l)

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
