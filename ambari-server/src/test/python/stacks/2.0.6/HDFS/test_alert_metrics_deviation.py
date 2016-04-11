#!/usr/bin/env python

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

# System imports
import os
import sys

from mock.mock import patch, MagicMock

# Local imports
from stacks.utils.RMFTestCase import *

COMMON_SERVICES_ALERTS_DIR = "HDFS/2.1.0.2.0/package/alerts"

file_path = os.path.dirname(os.path.abspath(__file__))
file_path = os.path.dirname(os.path.dirname(os.path.dirname(os.path.dirname(os.path.dirname(file_path)))))
file_path = os.path.join(file_path, "main", "resources", "common-services", COMMON_SERVICES_ALERTS_DIR)

RESULT_STATE_OK = "OK"
RESULT_STATE_WARNING = "WARNING"
RESULT_STATE_CRITICAL = "CRITICAL"
RESULT_STATE_UNKNOWN = "UNKNOWN"
RESULT_STATE_SKIPPED = "SKIPPED"

class TestAlertMetricsDeviation(RMFTestCase):

  def setUp(self):
    """
    Import the class under test.
    Because the class is present in a different folder, append its dir to the system path.
    Also, shorten the import name and make it a global so the test functions can access it.
    :return:
    """
    sys.path.append(file_path)
    global alert
    import alert_metrics_deviation as alert
    global configs
    configs = {
      '{{hdfs-site/dfs.namenode.https-address}}': 'c6401.ambari.apache.org:50470',
      '{{hdfs-site/dfs.http.policy}}': 'HTTP_ONLY',
      '{{ams-site/timeline.metrics.service.webapp.address}}': 'c6401.ambari.apache.org:6188',
      '{{hdfs-site/dfs.namenode.http-address}}': 'c6401.ambari.apache.org:50070',
      '{{cluster-env/security_enabled}}': 'false',
      '{{cluster-env/smokeuser}}': 'ambari-qa',
      '{{hdfs-site}}': {
        'dfs.datanode.address': '0.0.0.0:50010',
        'dfs.namenode.rpc-address': 'c6401.ambari.apache.org:8020',
        'dfs.namenode.https-address': 'c6401.ambari.apache.org:50470',
        'dfs.namenode.http-address': 'c6401.ambari.apache.org:50070',
        'dfs.datanode.https.address': '0.0.0.0:50475',
        'dfs.namenode.secondary.http-address': 'c6401.ambari.apache.org:50090',
        'dfs.datanode.http.address': '0.0.0.0:50075',
        'dfs.http.policy': 'HTTP_ONLY',
        'dfs.journalnode.https-address': '0.0.0.0:8481',
        'dfs.journalnode.http-address': '0.0.0.0:8480'
      }
    }
    global parameters
    parameters = {
      'metric.deviation.warning.threshold': 100.0,
      'mergeHaMetrics': 'false',
      'interval': 60.0,
      'metric.deviation.critical.threshold': 200.0,
      'appId': 'NAMENODE',
      'minimumValue': 30.0,
      'kerberos.kinit.timer': 14400000L,
      'metricName': 'metric1'
    }
  
  def test_missing_configs(self):
    """
    Check that the status is UNKNOWN when configs are missing.
    """
    configs = {}
    [status, messages] = alert.execute(configurations=configs)
    self.assertEqual(status, RESULT_STATE_UNKNOWN)
    self.assertTrue(messages is not None and len(messages) == 1)
    self.assertTrue('is a required parameter for the script' in messages[0])

  @patch("httplib.HTTPConnection")
  def test_alert(self, conn_mock):
    ca_connection = MagicMock()
    response = MagicMock()
    response.status = 200
    ca_connection.getresponse.return_value = response
    conn_mock.return_value = ca_connection
    response.read.return_value = '{"metrics":[{"metricname":"metric1","metrics":{"1459966360838":1,"1459966370838":3}}]}'

    # OK, but no datapoints above the minimum threshold
    [status, messages] = alert.execute(configurations=configs, parameters=parameters)
    self.assertEqual(status, RESULT_STATE_OK)
    self.assertTrue(messages is not None and len(messages) == 1)
    self.assertEquals('No datapoints found above the minimum threshold of 30 seconds',messages[0])

    # Unable to calculate the standard deviation for 1 data point
    response.read.return_value = '{"metrics":[{"metricname":"metric1","metrics":{"1459966360838":40000}}]}'
    [status, messages] = alert.execute(configurations=configs, parameters=parameters)
    self.assertEqual(status, RESULT_STATE_SKIPPED)
    self.assertTrue(messages is not None and len(messages) == 1)
    self.assertEquals('Unable to calculate the standard deviation for 1 datapoints', messages[0])

    # OK
    response.read.return_value = '{"metrics":[{"metricname":"metric1","metrics":{"1459966360838":40000,"1459966370838":50000}}]}'
    [status, messages] = alert.execute(configurations=configs, parameters=parameters)
    self.assertEqual(status, RESULT_STATE_OK)
    self.assertTrue(messages is not None and len(messages) == 1)
    self.assertTrue('OK. Percentage standard deviation value is' in messages[0])

    # Warning
    response.read.return_value = '{"metrics":[{"metricname":"metric1","metrics":{"1459966360838":40000,"1459966370838":1000000}}]}'
    [status, messages] = alert.execute(configurations=configs, parameters=parameters)
    self.assertEqual(status, RESULT_STATE_WARNING)
    self.assertTrue(messages is not None and len(messages) == 1)
    self.assertTrue('WARNING. Percentage standard deviation' in messages[0])

    # HTTP request to AMS failed
    response.read.return_value = ''
    response.status = 501
    [status, messages] = alert.execute(configurations=configs, parameters=parameters)
    self.assertEqual(status, RESULT_STATE_UNKNOWN)
    self.assertTrue(messages is not None and len(messages) == 1)
    self.assertEquals('Unable to retrieve metrics from AMS.', messages[0])
