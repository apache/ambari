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

from stacks.utils.RMFTestCase import *
import os
import socket
import sys

from ambari_agent.AlertSchedulerHandler import AlertSchedulerHandler
from ambari_agent.alerts.collector import AlertCollector
from ambari_agent.alerts.metric_alert import MetricAlert
from ambari_agent.alerts.port_alert import PortAlert
from ambari_agent.alerts.script_alert import ScriptAlert
from ambari_agent.alerts.web_alert import WebAlert
from ambari_agent.apscheduler.scheduler import Scheduler

from collections import namedtuple
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
    test_host_scripts_path = os.path.join('ambari_agent', 'dummy_files')

    ash = AlertSchedulerHandler(test_file_path, test_stack_path, test_host_scripts_path)
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
      "enabled": True,
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
    pa.collect()


  @patch.object(socket.socket,"connect")
  def test_port_alert_complex_uri(self, socket_connect_mock):
    json = { "name": "namenode_process",
      "service": "HDFS",
      "component": "NAMENODE",
      "label": "NameNode process",
      "interval": 6,
      "scope": "host",
      "enabled": True,
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

    # use a URI that has commas to verify that we properly parse it
    pa.set_helpers(collector, {'hdfs-site/my-key': 'c6401.ambari.apache.org:2181,c6402.ambari.apache.org:2181,c6403.ambari.apache.org:2181'})
    pa.host_name = 'c6402.ambari.apache.org'
    self.assertEquals(6, pa.interval())

    pa.collect()
    
    alerts = collector.alerts()
    self.assertEquals(0, len(collector.alerts()))
    
    self.assertEquals('OK', alerts[0]['state'])
    self.assertTrue('response time on port 2181' in alerts[0]['text'])


  def test_port_alert_no_sub(self):
    json = { "name": "namenode_process",
      "service": "HDFS",
      "component": "NAMENODE",
      "label": "NameNode process",
      "interval": 6,
      "scope": "host",
      "enabled": True,
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

    pa.collect()


  def test_script_alert(self):
    json = {
      "name": "namenode_process",
      "service": "HDFS",
      "component": "NAMENODE",
      "label": "NameNode process",
      "interval": 6,
      "scope": "host",
      "enabled": True,
      "uuid": "c1f73191-4481-4435-8dae-fd380e4c0be1",
      "source": {
        "type": "SCRIPT",
        "path": "test_script.py",
      }
    }

    # normally set by AlertSchedulerHandler
    json['source']['stacks_directory'] = os.path.join('ambari_agent', 'dummy_files')
    json['source']['host_scripts_directory'] = os.path.join('ambari_agent', 'host_scripts')

    collector = AlertCollector()
    sa = ScriptAlert(json, json['source'])
    sa.set_helpers(collector, {'foo-site/bar': 'rendered-bar', 'foo-site/baz':'rendered-baz'} )
    self.assertEquals(json['source']['path'], sa.path)
    self.assertEquals(json['source']['stacks_directory'], sa.stacks_dir)
    self.assertEquals(json['source']['host_scripts_directory'], sa.host_scripts_dir)

    sa.collect()

    alerts = collector.alerts()
    self.assertEquals(0, len(collector.alerts()))

    self.assertEquals('WARNING', alerts[0]['state'])
    self.assertEquals('bar is rendered-bar, baz is rendered-baz', alerts[0]['text'])


  @patch.object(MetricAlert, "_load_jmx")
  def test_metric_alert(self, ma_load_jmx_mock):
    json = {
      "name": "cpu_check",
      "service": "HDFS",
      "component": "NAMENODE",
      "label": "NameNode process",
      "interval": 6,
      "scope": "host",
      "enabled": True,
      "uuid": "c1f73191-4481-4435-8dae-fd380e4c0be1",
      "source": {
        "type": "METRIC",
        "uri": {
          "http": "{{hdfs-site/dfs.datanode.http.address}}"
        },
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
    ma.set_helpers(collector, {'hdfs-site/dfs.datanode.http.address': '1.2.3.4:80'})
    ma.collect()
    
    alerts = collector.alerts()
    self.assertEquals(0, len(collector.alerts()))
    
    self.assertEquals('CRITICAL', alerts[0]['state'])
    self.assertEquals('crit_arr: 1 3 223', alerts[0]['text'])

    del json['source']['jmx']['value']
    collector = AlertCollector()
    ma = MetricAlert(json, json['source'])
    ma.set_helpers(collector, {'hdfs-site/dfs.datanode.http.address': '1.2.3.4:80'})
    ma.collect()

    alerts = collector.alerts()
    self.assertEquals(0, len(collector.alerts()))

    self.assertEquals('OK', alerts[0]['state'])
    self.assertEquals('ok_arr: 1 3 None', alerts[0]['text'])


  @patch.object(MetricAlert, "_load_jmx")
  def test_alert_uri_structure(self, ma_load_jmx_mock):
    json = {
      "name": "cpu_check",
      "service": "HDFS",
      "component": "NAMENODE",
      "label": "NameNode process",
      "interval": 6,
      "scope": "host",
      "enabled": True,
      "uuid": "c1f73191-4481-4435-8dae-fd380e4c0be1",
      "source": {
        "type": "METRIC",
        "uri": {
          "http": "{{hdfs-site/dfs.datanode.http.address}}",
          "https": "{{hdfs-site/dfs.datanode.https.address}}",
          "https_property": "{{hdfs-site/dfs.http.policy}}",
          "https_property_value": "HTTPS_ONLY"
        },
        "jmx": {
          "property_list": [
            "someJmxObject/value",
            "someOtherJmxObject/value"
          ],
          "value": "{0}"
        },
        "reporting": {
          "ok": {
            "text": "ok_arr: {0} {1} {2}",
          },
          "warning": {
            "text": "",
            "value": 10
          },
          "critical": {
            "text": "crit_arr: {0} {1} {2}",
            "value": 20
          }
        }
      }
    }

    ma_load_jmx_mock.return_value = [1,1]
    
    # run the alert without specifying any keys; an exception should be thrown
    # indicating that there was no URI and the result is UNKNOWN
    collector = AlertCollector()
    ma = MetricAlert(json, json['source'])
    ma.set_helpers(collector, '')
    ma.collect()

    self.assertEquals('UNKNOWN', collector.alerts()[0]['state'])

    # set 2 properties that make no sense wihtout the main URI properties 
    collector = AlertCollector()
    ma = MetricAlert(json, json['source'])
    ma.set_helpers(collector, {'hdfs-site/dfs.http.policy': 'HTTP_ONLY'})
    ma.collect()
    
    self.assertEquals('UNKNOWN', collector.alerts()[0]['state'])
    
    # set an actual property key (http)
    collector = AlertCollector()
    ma = MetricAlert(json, json['source'])
    ma.set_helpers(collector, {'hdfs-site/dfs.datanode.http.address': '1.2.3.4:80', 
        'hdfs-site/dfs.http.policy': 'HTTP_ONLY'})
    ma.collect()
    
    self.assertEquals('OK', collector.alerts()[0]['state'])
    
    # set an actual property key (https)
    collector = AlertCollector()
    ma = MetricAlert(json, json['source'])
    ma.set_helpers(collector, {'hdfs-site/dfs.datanode.https.address': '1.2.3.4:443', 
        'hdfs-site/dfs.http.policy': 'HTTP_ONLY'})
    ma.collect()
    
    self.assertEquals('OK', collector.alerts()[0]['state'])    

    # set both (http and https)
    collector = AlertCollector()
    ma = MetricAlert(json, json['source'])
    ma.set_helpers(collector, {'hdfs-site/dfs.datanode.http.address': '1.2.3.4:80', 
        'hdfs-site/dfs.datanode.https.address': '1.2.3.4:443', 
        'hdfs-site/dfs.http.policy': 'HTTP_ONLY'})
    ma.collect()
    
    self.assertEquals('OK', collector.alerts()[0]['state'])    


  @patch.object(WebAlert, "_make_web_request")
  def test_web_alert(self, wa_make_web_request_mock):
    json = {
      "name": "webalert_test",
      "service": "HDFS",
      "component": "DATANODE",
      "label": "WebAlert Test",
      "interval": 1,
      "scope": "HOST",
      "enabled": True,
      "uuid": "c1f73191-4481-4435-8dae-fd380e4c0be1",
      "source": {
        "type": "WEB",
        "uri": {
          "http": "{{hdfs-site/dfs.datanode.http.address}}",
          "https": "{{hdfs-site/dfs.datanode.https.address}}",
          "https_property": "{{hdfs-site/dfs.http.policy}}",
          "https_property_value": "HTTPS_ONLY"
        },
        "reporting": {
          "ok": {
            "text": "ok: {0}",
          },
          "warning": {
            "text": "warning: {0}",
          },
          "critical": {
            "text": "critical: {1}",
          }
        }
      }
    }

    WebResponse = namedtuple('WebResponse', 'status_code time_millis')
    wa_make_web_request_mock.return_value = WebResponse(200,1.234)

    # run the alert and check HTTP 200    
    collector = AlertCollector()
    alert = WebAlert(json, json['source'])
    alert.set_helpers(collector, {'hdfs-site/dfs.datanode.http.address': '1.2.3.4:80'})
    alert.collect()

    alerts = collector.alerts()
    self.assertEquals(0, len(collector.alerts()))

    self.assertEquals('OK', alerts[0]['state'])
    self.assertEquals('ok: 200', alerts[0]['text'])

    # run the alert and check HTTP 500
    wa_make_web_request_mock.return_value = WebResponse(500,1.234)
    collector = AlertCollector()
    alert = WebAlert(json, json['source'])
    alert.set_helpers(collector, {'hdfs-site/dfs.datanode.http.address': '1.2.3.4:80'})
    alert.collect()
    
    alerts = collector.alerts()
    self.assertEquals(0, len(collector.alerts()))
    
    self.assertEquals('WARNING', alerts[0]['state'])
    self.assertEquals('warning: 500', alerts[0]['text'])

    # run the alert and check critical
    wa_make_web_request_mock.return_value = WebResponse(0,0)
     
    collector = AlertCollector()
    alert = WebAlert(json, json['source'])
    alert.set_helpers(collector, {'hdfs-site/dfs.datanode.http.address': '1.2.3.4:80'})
    alert.collect()
    
    alerts = collector.alerts()
    self.assertEquals(0, len(collector.alerts()))    
    
    # http assertion indicating that we properly determined non-SSL
    self.assertEquals('CRITICAL', alerts[0]['state'])
    self.assertEquals('critical: http://1.2.3.4:80', alerts[0]['text'])
     
    collector = AlertCollector()
    alert = WebAlert(json, json['source'])
    alert.set_helpers(collector, {
        'hdfs-site/dfs.datanode.http.address': '1.2.3.4:80',
        'hdfs-site/dfs.datanode.https.address': '1.2.3.4:8443',
        'hdfs-site/dfs.http.policy': 'HTTPS_ONLY'})

    alert.collect()
    
    alerts = collector.alerts()
    self.assertEquals(0, len(collector.alerts()))    
    
    # SSL assertion
    self.assertEquals('CRITICAL', alerts[0]['state'])
    self.assertEquals('critical: https://1.2.3.4:8443', alerts[0]['text'])

  def test_reschedule(self):
    test_file_path = os.path.join('ambari_agent', 'dummy_files')
    test_stack_path = os.path.join('ambari_agent', 'dummy_files')
    test_host_scripts_path = os.path.join('ambari_agent', 'dummy_files')
    
    ash = AlertSchedulerHandler(test_file_path, test_stack_path, test_host_scripts_path)
    ash.start()

    self.assertEquals(1, ash.get_job_count())
    ash.reschedule()
    self.assertEquals(1, ash.get_job_count())


  def test_alert_collector_purge(self):
    json = { "name": "namenode_process",
      "service": "HDFS",
      "component": "NAMENODE",
      "label": "NameNode process",
      "interval": 6,
      "scope": "host",
      "enabled": True,
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

    alerts = collector.alerts()
    self.assertEquals(0, len(collector.alerts()))

    self.assertTrue(alerts[0] is not None)
    self.assertEquals('CRITICAL', alerts[0]['state'])

    collector.remove_by_uuid('c1f73191-4481-4435-8dae-fd380e4c0be1')
    self.assertEquals(0,len(collector.alerts()))


  def test_disabled_definitions(self):
    test_file_path = os.path.join('ambari_agent', 'dummy_files')
    test_stack_path = os.path.join('ambari_agent', 'dummy_files')
    test_host_scripts_path = os.path.join('ambari_agent', 'dummy_files')

    ash = AlertSchedulerHandler(test_file_path, test_stack_path, test_host_scripts_path)
    ash.start()

    self.assertEquals(1, ash.get_job_count())

    json = { "name": "namenode_process",
      "service": "HDFS",
      "component": "NAMENODE",
      "label": "NameNode process",
      "interval": 6,
      "scope": "host",
      "enabled": True,
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

    pa = PortAlert(json, json['source'])
    ash.schedule_definition(pa)

    self.assertEquals(2, ash.get_job_count())

    json['enabled'] = False
    pa = PortAlert(json, json['source'])
    ash.schedule_definition(pa)

    # verify disabled alert not scheduled
    self.assertEquals(2, ash.get_job_count())

    json['enabled'] = True
    pa = PortAlert(json, json['source'])
    ash.schedule_definition(pa)

    # verify enabled alert was scheduled
    self.assertEquals(3, ash.get_job_count())

  def test_immediate_alert(self):
    test_file_path = os.path.join('ambari_agent', 'dummy_files')
    test_stack_path = os.path.join('ambari_agent', 'dummy_files')
    test_host_scripts_path = os.path.join('ambari_agent', 'dummy_files')

    ash = AlertSchedulerHandler(test_file_path, test_stack_path, test_host_scripts_path)
    ash.start()

    self.assertEquals(1, ash.get_job_count())
    self.assertEquals(0, len(ash._collector.alerts()))

    execution_commands = [ {
        "clusterName": "c1",
        "hostName": "c6401.ambari.apache.org",
        "alertDefinition": {
          "name": "namenode_process",
          "service": "HDFS",
          "component": "NAMENODE",
          "label": "NameNode process",
          "interval": 6,
          "scope": "host",
          "enabled": True,
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
      } ]

    # execute the alert immediately and verify that the collector has the result
    ash.execute_alert(execution_commands)
    self.assertEquals(1, len(ash._collector.alerts()))


  def test_skipped_alert(self):
    json = {
      "name": "namenode_process",
      "service": "HDFS",
      "component": "NAMENODE",
      "label": "NameNode process",
      "interval": 6,
      "scope": "host",
      "enabled": True,
      "uuid": "c1f73191-4481-4435-8dae-fd380e4c0be1",
      "source": {
        "type": "SCRIPT",
        "path": "test_script.py",
      }
    }

    # normally set by AlertSchedulerHandler
    json['source']['stacks_directory'] = os.path.join('ambari_agent', 'dummy_files')
    json['source']['host_scripts_directory'] = os.path.join('ambari_agent', 'host_scripts')

    collector = AlertCollector()
    sa = ScriptAlert(json, json['source'])

    # instruct the test alert script to be skipped
    sa.set_helpers(collector, {'foo-site/skip': 'true'} )

    self.assertEquals(json['source']['path'], sa.path)
    self.assertEquals(json['source']['stacks_directory'], sa.stacks_dir)
    self.assertEquals(json['source']['host_scripts_directory'], sa.host_scripts_dir)

    # ensure that it was skipped
    self.assertEquals(0,len(collector.alerts()))
