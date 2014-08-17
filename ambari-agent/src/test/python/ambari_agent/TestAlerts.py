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
from ambari_agent.AlertSchedulerHandler import AlertSchedulerHandler
from ambari_agent.apscheduler.scheduler import Scheduler
from ambari_agent.alerts.port_alert import PortAlert
from mock.mock import patch
from unittest import TestCase

class TestAlerts(TestCase):

  def setUp(self):
    pass

  def tearDown(self):
    sys.stdout == sys.__stdout__

  @patch.object(Scheduler, "add_interval_job")
  def test_build(self, aps_add_interval_job_mock):
    test_file_path = os.path.join('ambari_agent', 'dummy_files', 'alert_definitions.json')

    ash = AlertSchedulerHandler(test_file_path)

    self.assertTrue(aps_add_interval_job_mock.called)

  def test_port_alert(self):
    json = { "name": "namenode_process",
      "service": "HDFS",
      "component": "NAMENODE",
      "label": "NameNode process",
      "interval": 6,
      "scope": "host",
      "source": {
        "type": "PORT",
        "uri": "http://c6401.ambari.apache.org:50070",
        "default_port": 50070,
        "reporting": {
          "ok": {
            "text": "TCP OK - {0:.4f} response time on port {1}"
          },
          "critical": {
            "text": "Could not load process info: {0}"
          }
        }
      }
    }

    pa = PortAlert(json, json['source'])
    self.assertEquals(6, pa.interval())

    res = pa.collect()
    
    pass

