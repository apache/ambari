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

import os
import sys
import json
import time

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

class TestAlertCheckpointTime(RMFTestCase):
  def setUp(self):
    """
    Import the class under test.
    Because the class is present in a different folder, append its dir to the system path.
    Also, shorten the import name and make it a global so the test functions can access it.
    :return:
    """
    sys.path.append(file_path)
    global alert

    import alert_checkpoint_time as alert
    global configs
    configs = { "{{hdfs-site}}" : {'dfs.namenode.http-address' : 'c6401.ambari.apache.org:50470',
                                   'dfs.http.policy': 'HTTP_ONLY',
                                   'dfs.namenode.checkpoint.period': 100,
                                   'security_enabled': 'false',
                                   'dfs.namenode.checkpoint.txns': 100},
      '{{hdfs-site/dfs.namenode.http-address}}': 'c6401.ambari.apache.org:50470',
      '{{hdfs-site/dfs.http.policy}}': 'HTTP_ONLY',
      '{{hdfs-site/dfs.namenode.checkpoint.period}}': 100,
      '{{cluster-env/security_enabled}}': 'false',
      '{{hdfs-site/dfs.namenode.checkpoint.txns}}': 100,
    }
    global parameters
    parameters = {
      'connection.timeout': 200.0,
      'checkpoint.time.warning.threshold': 2.0,
      'checkpoint.time.critical.threshold': 4.0,
      'checkpoint.txns.multiplier.warning.threshold': 2.0,
      'checkpoint.txns.multiplier.critical.threshold': 4.0
    }

  @patch("urllib2.urlopen")
  def test_uncommitted_txn_exceeded(self, urlopen_mock):
    current_time = int(round(time.time() * 1000))

    jmx_output = { 'beans' : [
      { "LastCheckpointTime" : current_time - 3600000,
        "JournalTransactionInfo" : "{\"MostRecentCheckpointTxId\":\"2000\","
                                 "\"LastAppliedOrWrittenTxId\":\"3000\"}" }
    ] }

    response = MagicMock()
    response.read.return_value = json.dumps(jmx_output)
    urlopen_mock.return_value = response

    [status, messages] = alert.execute(configurations=configs, parameters=parameters, host_name="c6401.ambari.apache.org")

    self.assertEqual(status, RESULT_STATE_CRITICAL)
    self.assertEqual(messages[0], 'Last Checkpoint: [1 hours, 0 minutes, 1000 transactions]')