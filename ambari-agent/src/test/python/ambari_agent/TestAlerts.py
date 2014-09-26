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
from ambari_agent.alerts.collector import AlertCollector
from ambari_agent.alerts.metric_alert import MetricAlert
from ambari_agent.alerts.port_alert import PortAlert
from ambari_agent.alerts.script_alert import ScriptAlert
from ambari_agent.apscheduler.scheduler import Scheduler
from mock.mock import patch
from unittest import TestCase

class TestAlerts(TestCase):

  def setUp(self):
    pass

  def tearDown(self):
    sys.stdout == sys.__stdout__

  @patch.object(Scheduler, "add_interval_job")
  @patch.object(Scheduler, "start")
  def test_start(self, aps_add_interval_job_mock, aps_start_mock):
    test_file_path = os.path.join('ambari_agent', 'dummy_files')
    test_stack_path = os.path.join('ambari_agent', 'dummy_files')

    ash = AlertSchedulerHandler(test_file_path, test_stack_path)
    ash.start()

    self.assertTrue(aps_add_interval_job_mock.called)
    self.assertTrue(aps_start_mock.called)

  def test_port_alert(self):
    json = { "name": "namenode_process",
      "service": "HDFS",
      "component": "NAMENODE",
      "label": "NameNode process",
      "interval": 6,
      "scope": "host",
      "uuid": "c1f73191-4481-4435-8dae-fd380e4c0be1",
      "source": {
        "type": "PORT",
        "uri": "{{hdfs-site/my-key}}",
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

    collector = AlertCollector()

    pa = PortAlert(json, json['source'])
    pa.set_helpers(collector, {'hdfs-site/my-key': 'value1'})
    self.assertEquals(6, pa.interval())

    res = pa.collect()

  def test_port_alert_no_sub(self):
    json = { "name": "namenode_process",
      "service": "HDFS",
      "component": "NAMENODE",
      "label": "NameNode process",
      "interval": 6,
      "scope": "host",
      "uuid": "c1f73191-4481-4435-8dae-fd380e4c0be1",
      "source": {
        "type": "PORT",
        "uri": "http://c6401.ambari.apache.org",
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
    pa.set_helpers(AlertCollector(), '')
    self.assertEquals('http://c6401.ambari.apache.org', pa.uri)

    res = pa.collect()

  def test_script_alert(self):
    json = {
      "name": "namenode_process",
      "service": "HDFS",
      "component": "NAMENODE",
      "label": "NameNode process",
      "interval": 6,
      "scope": "host",
      "uuid": "c1f73191-4481-4435-8dae-fd380e4c0be1",
      "source": {
        "type": "SCRIPT",
        "path": "test_script.py",
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

    # normally set by AlertSchedulerHandler
    json['source']['stacks_dir'] = os.path.join('ambari_agent', 'dummy_files')

    collector = AlertCollector()
    sa = ScriptAlert(json, json['source'])
    sa.set_helpers(collector, '')
    self.assertEquals(json['source']['path'], sa.path)
    self.assertEquals(json['source']['stacks_dir'], sa.stacks_dir)

    sa.collect()

    self.assertEquals('WARNING', collector.alerts()[0]['state'])
    self.assertEquals('all is not well', collector.alerts()[0]['text'])
   
  @patch.object(MetricAlert, "_load_jmx")
  def test_metric_alert(self, ma_load_jmx_mock):
    json = {
      "name": "cpu_check",
      "service": "HDFS",
      "component": "NAMENODE",
      "label": "NameNode process",
      "interval": 6,
      "scope": "host",
      "uuid": "c1f73191-4481-4435-8dae-fd380e4c0be1",
      "source": {
        "type": "METRIC",
        "uri": "http://myurl:8633",
        "jmx": {
          "property_list": [
            "someJmxObject/value",
            "someOtherJmxObject/value"
          ],
          "value": "{0} * 100 + 123"
        },
        "reporting": {
          "ok": {
            "text": "ok_arr: {0} {1} {2}",
          },
          "warning": {
            "text": "",
            "value": 13
          },
          "critical": {
            "text": "crit_arr: {0} {1} {2}",
            "value": 72
          }
        }
      }
    }

    ma_load_jmx_mock.return_value = [1, 3]

    collector = AlertCollector()
    ma = MetricAlert(json, json['source'])
    ma.set_helpers(collector, '')
    ma.collect()

    self.assertEquals('CRITICAL', collector.alerts()[0]['state'])
    self.assertEquals('crit_arr: 1 3 223', collector.alerts()[0]['text'])

    del json['source']['jmx']['value']
    collector = AlertCollector()
    ma = MetricAlert(json, json['source'])
    ma.set_helpers(collector, '')
    ma.collect()

    self.assertEquals('OK', collector.alerts()[0]['state'])
    self.assertEquals('ok_arr: 1 3 None', collector.alerts()[0]['text'])
    
  def test_reschedule(self):
    test_file_path = os.path.join('ambari_agent', 'dummy_files')
    test_stack_path = os.path.join('ambari_agent', 'dummy_files')

    ash = AlertSchedulerHandler(test_file_path, test_stack_path)
    ash.start()
    ash.reschedule()
        
  
  def test_alert_collector_purge(self):
    json = { "name": "namenode_process",
      "service": "HDFS",
      "component": "NAMENODE",
      "label": "NameNode process",
      "interval": 6,
      "scope": "host",
      "uuid": "c1f73191-4481-4435-8dae-fd380e4c0be1",
      "source": {
        "type": "PORT",
        "uri": "{{hdfs-site/my-key}}",
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

    collector = AlertCollector()

    pa = PortAlert(json, json['source'])
    pa.set_helpers(collector, {'hdfs-site/my-key': 'value1'})
    self.assertEquals(6, pa.interval())

    res = pa.collect()
    
    self.assertIsNotNone(collector.alerts()[0])
    self.assertEquals('CRITICAL', collector.alerts()[0]['state'])
    
    collector.remove_by_uuid('c1f73191-4481-4435-8dae-fd380e4c0be1')
    self.assertEquals(0,len(collector.alerts()))
    
