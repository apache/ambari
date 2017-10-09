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

import imp
import json
import os
from unittest import TestCase

from mock.mock import patch, MagicMock


class TestAMBARI_METRICS010ServiceAdvisor(TestCase):

  testDirectory = os.path.dirname(os.path.abspath(__file__))
  stack_advisor_path = os.path.join(testDirectory, '../../../../main/resources/stacks/stack_advisor.py')
  with open(stack_advisor_path, 'rb') as fp:
    imp.load_module('stack_advisor', fp, stack_advisor_path, ('.py', 'rb', imp.PY_SOURCE))

  serviceAdvisorPath = '../../../../main/resources/common-services/AMBARI_METRICS/0.1.0/service_advisor.py'
  ambariMetrics010ServiceAdvisorPath = os.path.join(testDirectory, serviceAdvisorPath)
  with open(ambariMetrics010ServiceAdvisorPath, 'rb') as fp:
    service_advisor_impl = imp.load_module('service_advisor_impl', fp, ambariMetrics010ServiceAdvisorPath, ('.py', 'rb', imp.PY_SOURCE))

  def setUp(self):
    serviceAdvisorClass = getattr(self.service_advisor_impl, 'AMBARI_METRICSServiceAdvisor')
    self.serviceAdvisor = serviceAdvisorClass()


  def test_recommendAmsConfigurations(self):
    configurations = {
      "hadoop-env": {
        "properties": {
          "hdfs_user": "hdfs",
          "proxyuser_group": "users"
        }
      }
    }

    hosts = {
      "items": [
        {
          "href": "/api/v1/hosts/host1",
          "Hosts": {
            "cpu_count": 1,
            "host_name": "c6401.ambari.apache.org",
            "os_arch": "x86_64",
            "os_type": "centos6",
            "ph_cpu_count": 1,
            "public_host_name": "public.c6401.ambari.apache.org",
            "rack_info": "/default-rack",
            "total_mem": 2097152,
            "disk_info": [{
              "size": '80000000',
              "mountpoint": "/"
            }]
          }
        },
        {
          "href": "/api/v1/hosts/host2",
          "Hosts": {
            "cpu_count": 1,
            "host_name": "c6402.ambari.apache.org",
            "os_arch": "x86_64",
            "os_type": "centos6",
            "ph_cpu_count": 1,
            "public_host_name": "public.c6402.ambari.apache.org",
            "rack_info": "/default-rack",
            "total_mem": 1048576,
            "disk_info": [{
              "size": '800000000',
              "mountpoint": "/"
            }]
          }
        }
      ]}


    services1 = {
      "services": [
        {
          "StackServices": {
            "service_name": "HDFS"
          }, "components": [
          {
            "StackServiceComponents": {
              "component_name": "NAMENODE",
              "hostnames": ["c6401.ambari.apache.org"]
            }
          }
        ]
        },
        {
          "StackServices": {
            "service_name": "AMBARI_METRICS"
          },
          "components": [
            {
              "StackServiceComponents": {
                "component_name": "METRICS_COLLECTOR",
                "hostnames": ["c6401.ambari.apache.org", "c6402.ambari.apache.org"]
              }
            }, {
              "StackServiceComponents": {
                "component_name": "METRICS_MONITOR",
                "hostnames": ["c6401.ambari.apache.org", "c6402.ambari.apache.org"]
              }
            }
          ]
        }],
      "configurations": configurations,
      "ambari-server-properties": {"ambari-server.user":"ambari_user"}
    }

    clusterData = {
      "totalAvailableRam": 2048
    }

    expected = {'ams-env': {'properties': {'metrics_collector_heapsize': '512'}},
                  'ams-grafana-env': {'properties': {},
                                                             'property_attributes': {'metrics_grafana_password': {'visible': 'false'}}},
                  'ams-hbase-env': {'properties': {'hbase_log_dir': '/var/log/ambari-metrics-collector',
                                                                                       'hbase_master_heapsize': '512',
                                                                                       'hbase_master_xmn_size': '102',
                                                                                       'hbase_regionserver_heapsize': '1024',
                                                                                       'regionserver_xmn_size': '128'}},
                  'ams-hbase-site': {'properties': {'hbase.cluster.distributed': 'true',
                                                                                         'hbase.hregion.memstore.flush.size': '134217728',
                                                                                         'hbase.regionserver.global.memstore.lowerLimit': '0.3',
                                                                                         'hbase.regionserver.global.memstore.upperLimit': '0.35',
                                                                                         'hbase.rootdir': '/user/ams/hbase',
                                                                                         'hbase.tmp.dir': '/var/lib/ambari-metrics-collector/hbase-tmp',
                                                                                         'hbase.zookeeper.property.clientPort': '2181',
                                                                                         'hfile.block.cache.size': '0.3'}},
                  'ams-site': {'properties': {'timeline.metrics.cache.commit.interval': '10',
                                                                             'timeline.metrics.cache.size': '100',
                                                                             'timeline.metrics.cluster.aggregate.splitpoints': 'master.Balancer.BalancerCluster_95th_percentile',
                                                                             'timeline.metrics.host.aggregate.splitpoints': 'master.Balancer.BalancerCluster_95th_percentile',
                                                                             'timeline.metrics.service.handler.thread.count': '20',
                                                                             'timeline.metrics.service.operation.mode': 'distributed',
                                                                             'timeline.metrics.service.watcher.disabled': 'true',
                                                                             'timeline.metrics.service.webapp.address': '0.0.0.0:6188'}},
                  'hadoop-env': {'properties': {'hdfs_user': 'hdfs',
                                                                                 'proxyuser_group': 'users'}}}

    self.serviceAdvisor.getServiceConfigurationRecommendations(configurations, clusterData, services1, hosts)
    self.assertEquals(configurations, expected)

    services1 = {
      "services": [
        {
          "StackServices": {
            "service_name": "HDFS"
          }, "components": [
          {
            "StackServiceComponents": {
              "component_name": "NAMENODE",
              "hostnames": ["c6401.ambari.apache.org"]
            }
          }
        ]
        },
        {
          "StackServices": {
            "service_name": "AMBARI_METRICS"
          },
          "components": [
            {
              "StackServiceComponents": {
                "component_name": "METRICS_COLLECTOR",
                "hostnames": ["c6401.ambari.apache.org"]
              }
            }, {
              "StackServiceComponents": {
                "component_name": "METRICS_MONITOR",
                "hostnames": ["c6401.ambari.apache.org", "c6402.ambari.apache.org"]
              }
            }
          ]
        }],
      "configurations": configurations,
      "ambari-server-properties": {"ambari-server.user":"ambari_user"}
    }
    expected = {'ams-env': {'properties': {'metrics_collector_heapsize': '512'}},
                  'ams-grafana-env': {'properties': {},
                                                             'property_attributes': {'metrics_grafana_password': {'visible': 'false'}}},
                  'ams-hbase-env': {'properties': {'hbase_log_dir': '/var/log/ambari-metrics-collector',
                                                                                       'hbase_master_heapsize': '512',
                                                                                       'hbase_master_xmn_size': '102',
                                                                                       'hbase_regionserver_heapsize': '1024',
                                                                                       'regionserver_xmn_size': '128'}},
                  'ams-hbase-site': {'properties': {'hbase.cluster.distributed': 'true',
                                                                                         'hbase.hregion.memstore.flush.size': '134217728',
                                                                                         'hbase.regionserver.global.memstore.lowerLimit': '0.3',
                                                                                         'hbase.regionserver.global.memstore.upperLimit': '0.35',
                                                                                         'hbase.rootdir': '/user/ams/hbase',
                                                                                         'hbase.tmp.dir': '/var/lib/ambari-metrics-collector/hbase-tmp',
                                                                                         'hbase.zookeeper.property.clientPort': '2181',
                                                                                         'hfile.block.cache.size': '0.3',
                                                                                         'phoenix.coprocessor.maxMetaDataCacheSize': '20480000'}},
                  'ams-site': {'properties': {'timeline.metrics.cache.commit.interval': '10',
                                                                             'timeline.metrics.cache.size': '100',
                                                                             'timeline.metrics.cluster.aggregate.splitpoints': 'master.Balancer.BalancerCluster_95th_percentile',
                                                                             'timeline.metrics.host.aggregate.splitpoints': 'master.Balancer.BalancerCluster_95th_percentile',
                                                                             'timeline.metrics.service.handler.thread.count': '20',
                                                                             'timeline.metrics.service.operation.mode': 'distributed',
                                                                             'timeline.metrics.service.watcher.disabled': 'true',
                                                                             'timeline.metrics.service.webapp.address': '0.0.0.0:6188'}},
                  'hadoop-env': {'properties': {'hdfs_user': 'hdfs',
                                                                                 'proxyuser_group': 'users'}}}
    self.serviceAdvisor.getServiceConfigurationRecommendations(configurations, clusterData, services1, hosts)
    self.assertEquals(configurations, expected)


  def test_validateAmsSiteConfigurations(self):
    configurations = {
      "hdfs-site": {
        "properties": {
          'dfs.datanode.data.dir': "/hadoop/data"
        }
      },
      "core-site": {
        "properties": {
          "fs.defaultFS": "hdfs://c6401.ambari.apache.org:8020"
        }
      },
      "ams-site": {
        "properties": {
          "timeline.metrics.service.operation.mode": "embedded"
        }
      }
    }
    recommendedDefaults = {
      'hbase.rootdir': 'file:///var/lib/ambari-metrics-collector/hbase',
      'hbase.tmp.dir': '/var/lib/ambari-metrics-collector/hbase',
      'hbase.cluster.distributed': 'false'
    }
    properties = {
      'hbase.rootdir': 'file:///var/lib/ambari-metrics-collector/hbase',
      'hbase.tmp.dir' : '/var/lib/ambari-metrics-collector/hbase',
      'hbase.cluster.distributed': 'false',
      'timeline.metrics.service.operation.mode' : 'embedded'
    }
    host1 = {
      "href" : "/api/v1/hosts/host1",
      "Hosts" : {
        "cpu_count" : 1,
        "host_name" : "host1",
        "os_arch" : "x86_64",
        "os_type" : "centos6",
        "ph_cpu_count" : 1,
        "public_host_name" : "host1",
        "rack_info" : "/default-rack",
        "total_mem" : 2097152,
        "disk_info": [
          {
            "available": str(15<<30), # 15 GB
            "type": "ext4",
            "mountpoint": "/"
          }
        ]
      }
    }
    host2 = {
      "href" : "/api/v1/hosts/host2",
      "Hosts" : {
        "cpu_count" : 1,
        "host_name" : "host2",
        "os_arch" : "x86_64",
        "os_type" : "centos6",
        "ph_cpu_count" : 1,
        "public_host_name" : "host2",
        "rack_info" : "/default-rack",
        "total_mem" : 2097152,
        "disk_info": [
          {
            "available": str(15<<30), # 15 GB
            "type": "ext4",
            "mountpoint": "/"
          }
        ]
      }
    }

    hosts = {
      "items" : [
        host1, host2
      ]
    }

    services = {
      "services":  [
        {
          "StackServices": {
            "service_name": "AMBARI_METRICS"
          },
          "components": [
            {
              "StackServiceComponents": {
                "component_name": "METRICS_COLLECTOR",
                "hostnames": ["host1", "host2"]
              }
            }, {
              "StackServiceComponents": {
                "component_name": "METRICS_MONITOR",
                "hostnames": ["host1", "host2"]
              }
            }
          ]
        },
        {
          "StackServices": {
            "service_name": "HDFS"
          },
          "components": [
            {
              "StackServiceComponents": {
                "component_name": "DATANODE",
                "hostnames": ["host1"]
              }
            }
          ]
        }
      ],
      "configurations": configurations
    }
    # only 1 partition, enough disk space, no warnings
    res = self.serviceAdvisor.getAMBARI_METRICSValidator().validateAmsSiteConfigurationsFromHDP206(properties, recommendedDefaults, configurations, services, hosts)
    expected = [{'config-name': 'timeline.metrics.service.operation.mode',
                    'config-type': 'ams-site',
                    'level': 'ERROR',
                    'message': "Correct value should be 'distributed' for clusters with more then 1 Metrics collector",
                    'type': 'configuration'}]
    self.assertEquals(res, expected)


    services = {
      "services":  [
        {
          "StackServices": {
            "service_name": "AMBARI_METRICS"
          },
          "components": [
            {
              "StackServiceComponents": {
                "component_name": "METRICS_COLLECTOR",
                "hostnames": ["host1"]
              }
            }, {
              "StackServiceComponents": {
                "component_name": "METRICS_MONITOR",
                "hostnames": ["host1"]
              }
            }
          ]
        },
        {
          "StackServices": {
            "service_name": "HDFS"
          },
          "components": [
            {
              "StackServiceComponents": {
                "component_name": "DATANODE",
                "hostnames": ["host1"]
              }
            }
          ]
        }
      ],
      "configurations": configurations
    }
    res = self.serviceAdvisor.getAMBARI_METRICSValidator().validateAmsSiteConfigurationsFromHDP206(properties, recommendedDefaults, configurations, services, hosts)
    expected = []
    self.assertEquals(res, expected)

  def test_validateAmsHbaseSiteConfigurations(self):
    configurations = {
      "hdfs-site": {
        "properties": {
          'dfs.datanode.data.dir': "/hadoop/data"
        }
      },
      "core-site": {
        "properties": {
          "fs.defaultFS": "hdfs://c6401.ambari.apache.org:8020"
        }
      },
      "ams-site": {
        "properties": {
          "timeline.metrics.service.operation.mode": "embedded"
        }
      }
    }

    recommendedDefaults = {
      'hbase.rootdir': 'file:///var/lib/ambari-metrics-collector/hbase',
      'hbase.tmp.dir': '/var/lib/ambari-metrics-collector/hbase',
      'hbase.cluster.distributed': 'false'
    }
    properties = {
      'hbase.rootdir': 'file:///var/lib/ambari-metrics-collector/hbase',
      'hbase.tmp.dir' : '/var/lib/ambari-metrics-collector/hbase',
      'hbase.cluster.distributed': 'false'
    }
    host = {
      "href" : "/api/v1/hosts/host1",
      "Hosts" : {
        "cpu_count" : 1,
        "host_name" : "host1",
        "os_arch" : "x86_64",
        "os_type" : "centos6",
        "ph_cpu_count" : 1,
        "public_host_name" : "host1",
        "rack_info" : "/default-rack",
        "total_mem" : 2097152,
        "disk_info": [
          {
            "available": str(15<<30), # 15 GB
            "type": "ext4",
            "mountpoint": "/"
          }
        ]
      }
    }

    hosts = {
      "items" : [
        host
      ]
    }

    services = {
      "services":  [
        {
          "StackServices": {
            "service_name": "AMBARI_METRICS"
          },
          "components": [
            {
              "StackServiceComponents": {
                "component_name": "METRICS_COLLECTOR",
                "hostnames": ["host1"]
              }
            }, {
              "StackServiceComponents": {
                "component_name": "METRICS_MONITOR",
                "hostnames": ["host1"]
              }
            }
          ]
        },
        {
          "StackServices": {
            "service_name": "HDFS"
          },
          "components": [
            {
              "StackServiceComponents": {
                "component_name": "DATANODE",
                "hostnames": ["host1"]
              }
            }
          ]
        }
      ],
      "configurations": configurations
    }

    # only 1 partition, enough disk space, no warnings
    res = self.serviceAdvisor.getAMBARI_METRICSValidator().validateAmsHbaseSiteConfigurationsFromHDP206(properties, recommendedDefaults, configurations, services, hosts)
    expected = []
    self.assertEquals(res, expected)


    # 1 partition, no enough disk space
    host['Hosts']['disk_info'] = [
      {
        "available" : '1',
        "type" : "ext4",
        "mountpoint" : "/"
      }
    ]
    res = self.serviceAdvisor.getAMBARI_METRICSValidator().validateAmsHbaseSiteConfigurationsFromHDP206(properties, recommendedDefaults, configurations, services, hosts)
    expected = [
      {'config-name': 'hbase.rootdir',
       'config-type': 'ams-hbase-site',
       'level': 'WARN',
       'message': 'Ambari Metrics disk space requirements not met. '
                  '\nRecommended disk space for partition / is 10G',
       'type': 'configuration'
      }
    ]
    self.assertEquals(res, expected)

    # 2 partitions
    host['Hosts']['disk_info'] = [
      {
        "available": str(15<<30), # 15 GB
        "type" : "ext4",
        "mountpoint" : "/grid/0"
      },
      {
        "available" : str(15<<30), # 15 GB
        "type" : "ext4",
        "mountpoint" : "/"
      }
    ]
    recommendedDefaults = {
      'hbase.rootdir': 'file:///grid/0/var/lib/ambari-metrics-collector/hbase',
      'hbase.tmp.dir': '/var/lib/ambari-metrics-collector/hbase',
      'hbase.cluster.distributed': 'false'
    }
    properties = {
      'hbase.rootdir': 'file:///grid/0/var/lib/ambari-metrics-collector/hbase',
      'hbase.tmp.dir' : '/var/lib/ambari-metrics-collector/hbase',
      'hbase.cluster.distributed': 'false'
    }
    res = self.serviceAdvisor.getAMBARI_METRICSValidator().validateAmsHbaseSiteConfigurationsFromHDP206(properties, recommendedDefaults, configurations, services, hosts)
    expected = []
    self.assertEquals(res, expected)

    # dfs.dir & hbase.rootdir crosscheck + root partition + hbase.rootdir == hbase.tmp.dir warnings
    properties = {
      'hbase.rootdir': 'file:///var/lib/ambari-metrics-collector/hbase',
      'hbase.tmp.dir' : '/var/lib/ambari-metrics-collector/hbase',
      'hbase.cluster.distributed': 'false'
    }

    res = self.serviceAdvisor.getAMBARI_METRICSValidator().validateAmsHbaseSiteConfigurationsFromHDP206(properties, recommendedDefaults, configurations, services, hosts)
    expected = [
      {
        'config-name': 'hbase.rootdir',
        'config-type': 'ams-hbase-site',
        'level': 'WARN',
        'message': 'It is not recommended to use root partition for hbase.rootdir',
        'type': 'configuration'
      },
      {
        'config-name': 'hbase.tmp.dir',
        'config-type': 'ams-hbase-site',
        'level': 'WARN',
        'message': 'Consider not using / partition for storing metrics temporary data. '
                   '/ partition is already used as hbase.rootdir to store metrics data',
        'type': 'configuration'
      },
      {
        'config-name': 'hbase.rootdir',
        'config-type': 'ams-hbase-site',
        'level': 'WARN',
        'message': 'Consider not using / partition for storing metrics data. '
                   '/ is already used by datanode to store HDFS data',
        'type': 'configuration'
      }
    ]
    self.assertEquals(res, expected)

    # incorrect hbase.rootdir in distributed mode
    properties = {
      'hbase.rootdir': 'file:///grid/0/var/lib/ambari-metrics-collector/hbase',
      'hbase.tmp.dir' : '/var/lib/ambari-metrics-collector/hbase',
      'hbase.cluster.distributed': 'false'
    }
    configurations['ams-site']['properties']['timeline.metrics.service.operation.mode'] = 'distributed'
    res = self.serviceAdvisor.getAMBARI_METRICSValidator().validateAmsHbaseSiteConfigurationsFromHDP206(properties, recommendedDefaults, configurations, services, hosts)
    expected = [
      {
        'config-name': 'hbase.rootdir',
        'config-type': 'ams-hbase-site',
        'level': 'WARN',
        'message': 'In distributed mode hbase.rootdir should point to HDFS.',
        'type': 'configuration'
      },
      {
        'config-name': 'hbase.cluster.distributed',
        'config-type': 'ams-hbase-site',
        'level': 'ERROR',
        'message': 'hbase.cluster.distributed property should be set to true for distributed mode',
        'type': 'configuration'
      }
    ]
    self.assertEquals(res, expected)