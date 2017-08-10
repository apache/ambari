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


class TestLOGSEARCH050ServiceAdvisor(TestCase):

  testDirectory = os.path.dirname(os.path.abspath(__file__))
  stack_advisor_path = os.path.join(testDirectory, '../../../../main/resources/stacks/stack_advisor.py')
  with open(stack_advisor_path, 'rb') as fp:
    imp.load_module('stack_advisor', fp, stack_advisor_path, ('.py', 'rb', imp.PY_SOURCE))

  serviceAdvisorPath = '../../../../main/resources/common-services/LOGSEARCH/0.5.0/service_advisor.py'
  logserch050ServiceAdvisorPath = os.path.join(testDirectory, serviceAdvisorPath)
  with open(logserch050ServiceAdvisorPath, 'rb') as fp:
    service_advisor_impl = imp.load_module('service_advisor_impl', fp, logserch050ServiceAdvisorPath, ('.py', 'rb', imp.PY_SOURCE))

  def setUp(self):
    serviceAdvisorClass = getattr(self.service_advisor_impl, 'LogSearchServiceAdvisor')
    self.serviceAdvisor = serviceAdvisorClass()

  def test_recommendLogsearchConfiguration(self):
    configurations = {
      "logsearch-properties": {
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
      'logsearch-properties': {
        'properties': {}
      },
      'logfeeder-env': {
        'property_attributes': {
          'logfeeder_external_solr_kerberos_keytab': {'visible': 'false'},
          'logfeeder_external_solr_kerberos_principal': {'visible': 'false'}
        }
      },
      'logsearch-common-env': {
        'properties': {
          'logsearch_external_solr_kerberos_enabled': 'false'
        },
        'property_attributes': {
          'logsearch_external_solr_kerberos_enabled': {'visible': 'false'}
        }
      },
      'logsearch-env': {
        'property_attributes': {
          'logsearch_external_solr_kerberos_keytab': {'visible': 'false'},
          'logsearch_external_solr_kerberos_principal': {'visible': 'false'}
        }
      }
    }
    services = {
      "services": [
        {
          "href": "/api/v1/stacks/HDP/versions/2.3/services/AMBARI_INFRA",
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
                "display_name": "Infra Solr Instance",
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
        "logsearch-properties": {
          "properties": {
            "logsearch.collection.numshards" : "5",
            "logsearch.collection.replication.factor": "0"
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
    def return_c6401_hostname(services, service_name, component_name):
      return ["c6401.ambari.apache.org"]
    self.serviceAdvisor.getComponentHostNames = return_c6401_hostname
    self.serviceAdvisor.getServiceConfigurationRecommendations(configurations, clusterData, services, hosts)
    self.assertEquals(configurations, expected)
