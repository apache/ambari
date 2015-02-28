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
import socket
import sys

from ambari_agent.AlertSchedulerHandler import AlertSchedulerHandler
from ambari_agent.alerts.collector import AlertCollector
from ambari_agent.alerts.base_alert import BaseAlert
from ambari_agent.alerts.metric_alert import MetricAlert
from ambari_agent.alerts.port_alert import PortAlert
from ambari_agent.alerts.script_alert import ScriptAlert
from ambari_agent.alerts.web_alert import WebAlert
from ambari_agent.apscheduler.scheduler import Scheduler
from ambari_agent.ClusterConfiguration import ClusterConfiguration

from collections import namedtuple
from mock.mock import MagicMock, patch
from unittest import TestCase

class TestAlerts(TestCase):

  def setUp(self):
    # save original open() method for later use
    self.original_open = open

  def tearDown(self):
    sys.stdout == sys.__stdout__


  @patch.object(Scheduler, "add_interval_job")
  @patch.object(Scheduler, "start")
  def test_start(self, aps_add_interval_job_mock, aps_start_mock):
    test_file_path = os.path.join('ambari_agent', 'dummy_files')
    test_stack_path = os.path.join('ambari_agent', 'dummy_files')
    test_common_services_path = os.path.join('ambari_agent', 'dummy_files')
    test_host_scripts_path = os.path.join('ambari_agent', 'dummy_files')

    cluster_configuration = self.__get_cluster_configuration()

    ash = AlertSchedulerHandler(test_file_path, test_stack_path,
      test_common_services_path, test_host_scripts_path, cluster_configuration,
      None)

    ash.start()

    self.assertTrue(aps_add_interval_job_mock.called)
    self.assertTrue(aps_start_mock.called)

  @patch('time.time')
  @patch.object(socket.socket,"connect")
  def test_port_alert(self, socket_connect_mock, time_mock):
    definition_json = { "name": "namenode_process",
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
            "text": "(Unit Tests) TCP OK - {0:.4f} response time on port {1}"
          },
          "warning": {
            "text": "(Unit Tests) TCP WARN - {0:.4f} response time on port {1}",
            "value": 1.5
          },
          "critical": {
            "text": "(Unit Tests) Could not load process info: {0}",
            "value": 5.0
          }
        }
      }
    }

    configuration = { 'hdfs-site' : { 'my-key': 'value1' } }

    collector = AlertCollector()
    cluster_configuration = self.__get_cluster_configuration()
    self.__update_cluster_configuration(cluster_configuration, configuration)

    # called 3x with 3 calls per alert
    # - 900ms and then a time.time() for the date from base_alert
    # - 2000ms and then a time.time() for the date from base_alert
    # - socket.timeout to simulate a timeout and then a time.time() for the date from base_alert
    time_mock.side_effect = [0,900,336283000000,
      0,2000,336283100000,
      socket.timeout,336283200000]

    alert = PortAlert(definition_json, definition_json['source'])
    alert.set_helpers(collector, cluster_configuration)
    alert.set_cluster("c1", "c6401.ambari.apache.org")
    self.assertEquals(6, alert.interval())

    # 900ms is OK
    alert.collect()
    alerts = collector.alerts()
    self.assertEquals(0, len(collector.alerts()))
    self.assertEquals('OK', alerts[0]['state'])

    # 2000ms is WARNING
    alert.collect()
    alerts = collector.alerts()
    self.assertEquals(0, len(collector.alerts()))
    self.assertEquals('WARNING', alerts[0]['state'])

    # throws a socket.timeout exception, causes a CRITICAL
    alert.collect()
    alerts = collector.alerts()
    self.assertEquals(0, len(collector.alerts()))
    self.assertEquals('CRITICAL', alerts[0]['state'])


  @patch.object(socket.socket,"connect")
  def test_port_alert_complex_uri(self, socket_connect_mock):
    definition_json = { "name": "namenode_process",
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
            "text": "(Unit Tests) TCP OK - {0:.4f} response time on port {1}"
          },
          "critical": {
            "text": "(Unit Tests) Could not load process info: {0}"
          }
        }
      }
    }

    configuration = {'hdfs-site' :
      { 'my-key': 'c6401.ambari.apache.org:2181,c6402.ambari.apache.org:2181,c6403.ambari.apache.org:2181'}
    }

    collector = AlertCollector()
    cluster_configuration = self.__get_cluster_configuration()
    self.__update_cluster_configuration(cluster_configuration, configuration)

    alert = PortAlert(definition_json, definition_json['source'])
    alert.set_helpers(collector, cluster_configuration)
    alert.set_cluster("c1", "c6402.ambari.apache.org")

    # use a URI that has commas to verify that we properly parse it
    alert.set_helpers(collector, cluster_configuration)
    alert.set_cluster("c1", "c6401.ambari.apache.org")
    self.assertEquals(6, alert.interval())

    alert.collect()
    
    alerts = collector.alerts()
    self.assertEquals(0, len(collector.alerts()))
    
    self.assertEquals('OK', alerts[0]['state'])
    self.assertTrue('(Unit Tests)' in alerts[0]['text'])
    self.assertTrue('response time on port 2181' in alerts[0]['text'])


  def test_port_alert_no_sub(self):
    definition_json = { "name": "namenode_process",
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
            "text": "(Unit Tests) TCP OK - {0:.4f} response time on port {1}"
          },
          "critical": {
            "text": "(Unit Tests) Could not load process info: {0}"
          }
        }
      }
    }

    cluster_configuration = self.__get_cluster_configuration()

    alert = PortAlert(definition_json, definition_json['source'])
    alert.set_helpers(AlertCollector(), cluster_configuration)
    alert.set_cluster("c1", "c6401.ambari.apache.org")

    self.assertEquals('http://c6401.ambari.apache.org', alert.uri)

    alert.collect()


  def test_script_alert(self):
    definition_json = {
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
    definition_json['source']['stacks_directory'] = os.path.join('ambari_agent', 'dummy_files')
    definition_json['source']['common_services_directory'] = os.path.join('ambari_agent', 'common-services')
    definition_json['source']['host_scripts_directory'] = os.path.join('ambari_agent', 'host_scripts')

    configuration = {'foo-site' :
      { 'bar': 'rendered-bar', 'baz' : 'rendered-baz' }
    }

    collector = AlertCollector()
    cluster_configuration = self.__get_cluster_configuration()
    self.__update_cluster_configuration(cluster_configuration, configuration)

    alert = ScriptAlert(definition_json, definition_json['source'], MagicMock())
    alert.set_helpers(collector, cluster_configuration )
    alert.set_cluster("c1", "c6401.ambari.apache.org")
    
    self.assertEquals(definition_json['source']['path'], alert.path)
    self.assertEquals(definition_json['source']['stacks_directory'], alert.stacks_dir)
    self.assertEquals(definition_json['source']['common_services_directory'], alert.common_services_dir)
    self.assertEquals(definition_json['source']['host_scripts_directory'], alert.host_scripts_dir)

    alert.collect()

    alerts = collector.alerts()
    self.assertEquals(0, len(collector.alerts()))

    self.assertEquals('WARNING', alerts[0]['state'])
    self.assertEquals('bar is rendered-bar, baz is rendered-baz', alerts[0]['text'])


  @patch.object(MetricAlert, "_load_jmx")
  def test_metric_alert(self, ma_load_jmx_mock):
    definition_json = {
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
            "text": "(Unit Tests) ok_arr: {0} {1} {2}",
          },
          "warning": {
            "text": "",
            "value": 13
          },
          "critical": {
            "text": "(Unit Tests) crit_arr: {0} {1} {2}",
            "value": 72
          }
        }
      }
    }

    ma_load_jmx_mock.return_value = [1, 3]

    configuration = {'hdfs-site' :
      { 'dfs.datanode.http.address': '1.2.3.4:80'}
    }

    collector = AlertCollector()
    cluster_configuration = self.__get_cluster_configuration()
    self.__update_cluster_configuration(cluster_configuration, configuration)

    alert = MetricAlert(definition_json, definition_json['source'])
    alert.set_helpers(collector, cluster_configuration)
    alert.set_cluster("c1", "c6401.ambari.apache.org")

    alert.collect()
    
    alerts = collector.alerts()
    self.assertEquals(0, len(collector.alerts()))
    
    self.assertEquals('CRITICAL', alerts[0]['state'])
    self.assertEquals('(Unit Tests) crit_arr: 1 3 223', alerts[0]['text'])

    del definition_json['source']['jmx']['value']
    collector = AlertCollector()

    alert = MetricAlert(definition_json, definition_json['source'])
    alert.set_helpers(collector, cluster_configuration)
    alert.set_cluster("c1", "c6401.ambari.apache.org")

    alert.collect()

    alerts = collector.alerts()
    self.assertEquals(0, len(collector.alerts()))

    self.assertEquals('OK', alerts[0]['state'])
    self.assertEquals('(Unit Tests) ok_arr: 1 3 None', alerts[0]['text'])


  @patch.object(MetricAlert, "_load_jmx")
  def test_alert_uri_structure(self, ma_load_jmx_mock):
    definition_json = {
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
            "text": "(Unit Tests) ok_arr: {0} {1} {2}",
          },
          "warning": {
            "text": "",
            "value": 10
          },
          "critical": {
            "text": "(Unit Tests) crit_arr: {0} {1} {2}",
            "value": 20
          }
        }
      }
    }

    ma_load_jmx_mock.return_value = [1,1]
    
    # run the alert without specifying any keys; an exception should be thrown
    # indicating that there was no URI and the result is UNKNOWN
    collector = AlertCollector()
    cluster_configuration = self.__get_cluster_configuration()
    alert = MetricAlert(definition_json, definition_json['source'])
    alert.set_helpers(collector, cluster_configuration)
    alert.set_cluster("c1", "c6401.ambari.apache.org")
    alert.collect()

    self.assertEquals('UNKNOWN', collector.alerts()[0]['state'])

    # set properties that make no sense wihtout the main URI properties
    configuration = {'hdfs-site' :
      { 'dfs.http.policy' : 'HTTP_ONLY'}
    }

    collector = AlertCollector()
    cluster_configuration = self.__get_cluster_configuration()
    self.__update_cluster_configuration(cluster_configuration, configuration)

    alert = MetricAlert(definition_json, definition_json['source'])
    alert.set_helpers(collector, cluster_configuration)
    alert.set_cluster("c1", "c6401.ambari.apache.org")
    alert.collect()
    
    self.assertEquals('UNKNOWN', collector.alerts()[0]['state'])
    
    # set an actual property key (http)
    configuration = {'hdfs-site' :
      { 'dfs.http.policy' : 'HTTP_ONLY', 'dfs.datanode.http.address' : '1.2.3.4:80' }
    }

    self.__update_cluster_configuration(cluster_configuration, configuration)

    collector = AlertCollector()
    alert = MetricAlert(definition_json, definition_json['source'])
    alert.set_helpers(collector, cluster_configuration)
    alert.set_cluster("c1", "c6401.ambari.apache.org")
    alert.collect()
    
    self.assertEquals('OK', collector.alerts()[0]['state'])
    
    # set an actual property key (https)
    configuration = {'hdfs-site' :
      { 'dfs.http.policy' : 'HTTP_ONLY', 'dfs.datanode.https.address' : '1.2.3.4:443' }
    }

    self.__update_cluster_configuration(cluster_configuration, configuration)

    collector = AlertCollector()
    alert = MetricAlert(definition_json, definition_json['source'])
    alert.set_helpers(collector, cluster_configuration)
    alert.set_cluster("c1", "c6401.ambari.apache.org")
    alert.collect()
    
    self.assertEquals('OK', collector.alerts()[0]['state'])    

    # set both (http and https)
    configuration = {'hdfs-site' :
      { 'dfs.http.policy' : 'HTTP_ONLY',
        'dfs.datanode.http.address' : '1.2.3.4:80',
        'dfs.datanode.https.address' : '1.2.3.4:443' }
    }

    self.__update_cluster_configuration(cluster_configuration, configuration)

    collector = AlertCollector()
    alert = MetricAlert(definition_json, definition_json['source'])
    alert.set_helpers(collector, cluster_configuration)
    alert.set_cluster("c1", "c6401.ambari.apache.org")
    alert.collect()
    
    self.assertEquals('OK', collector.alerts()[0]['state'])    


  @patch.object(WebAlert, "_make_web_request")
  def test_web_alert(self, wa_make_web_request_mock):
    definition_json = {
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
            "text": "(Unit Tests) ok: {0}",
          },
          "warning": {
            "text": "(Unit Tests) warning: {0}",
          },
          "critical": {
            "text": "(Unit Tests) critical: {1}. {3}",
          }
        }
      }
    }

    WebResponse = namedtuple('WebResponse', 'status_code time_millis error_msg')
    wa_make_web_request_mock.return_value = WebResponse(200,1.234,None)

    # run the alert and check HTTP 200    
    configuration = {'hdfs-site' :
      { 'dfs.datanode.http.address' : '1.2.3.4:80' }
    }

    collector = AlertCollector()
    cluster_configuration = self.__get_cluster_configuration()
    self.__update_cluster_configuration(cluster_configuration, configuration)

    alert = WebAlert(definition_json, definition_json['source'], None)
    alert.set_helpers(collector, cluster_configuration)
    alert.set_cluster("c1", "c6401.ambari.apache.org")
    alert.collect()

    alerts = collector.alerts()
    self.assertEquals(0, len(collector.alerts()))

    self.assertEquals('(Unit Tests) ok: 200', alerts[0]['text'])
    self.assertEquals('OK', alerts[0]['state'])

    # run the alert and check HTTP 500


    wa_make_web_request_mock.return_value = WebResponse(500,1.234,None)
    collector = AlertCollector()
    alert = WebAlert(definition_json, definition_json['source'], None)
    alert.set_helpers(collector, cluster_configuration)
    alert.set_cluster("c1", "c6401.ambari.apache.org")
    alert.collect()
    
    alerts = collector.alerts()
    self.assertEquals(0, len(collector.alerts()))
    
    self.assertEquals('WARNING', alerts[0]['state'])
    self.assertEquals('(Unit Tests) warning: 500', alerts[0]['text'])

    # run the alert and check critical
    wa_make_web_request_mock.return_value = WebResponse(0,0,'error message')
     
    collector = AlertCollector()
    alert = WebAlert(definition_json, definition_json['source'], None)
    alert.set_helpers(collector, cluster_configuration)
    alert.set_cluster("c1", "c6401.ambari.apache.org")
    alert.collect()
    
    alerts = collector.alerts()
    self.assertEquals(0, len(collector.alerts()))    
    
    # http assertion indicating that we properly determined non-SSL
    self.assertEquals('CRITICAL', alerts[0]['state'])
    self.assertEquals('(Unit Tests) critical: http://1.2.3.4:80. error message', alerts[0]['text'])

    configuration = {'hdfs-site' :
      { 'dfs.http.policy' : 'HTTPS_ONLY',
        'dfs.datanode.http.address' : '1.2.3.4:80',
        'dfs.datanode.https.address' : '1.2.3.4:443' }
    }

    self.__update_cluster_configuration(cluster_configuration, configuration)

    collector = AlertCollector()
    alert = WebAlert(definition_json, definition_json['source'], None)
    alert.set_helpers(collector, cluster_configuration)
    alert.set_cluster("c1", "c6401.ambari.apache.org")

    alert.collect()
    
    alerts = collector.alerts()
    self.assertEquals(0, len(collector.alerts()))    
    
    # SSL assertion
    self.assertEquals('CRITICAL', alerts[0]['state'])
    self.assertEquals('(Unit Tests) critical: https://1.2.3.4:443. error message', alerts[0]['text'])

  def test_reschedule(self):
    test_file_path = os.path.join('ambari_agent', 'dummy_files')
    test_stack_path = os.path.join('ambari_agent', 'dummy_files')
    test_common_services_path = os.path.join('ambari_agent', 'dummy_files')
    test_host_scripts_path = os.path.join('ambari_agent', 'dummy_files')

    cluster_configuration = self.__get_cluster_configuration()

    ash = AlertSchedulerHandler(test_file_path, test_stack_path,
      test_common_services_path, test_host_scripts_path, cluster_configuration,
      None)

    ash.start()

    self.assertEquals(1, ash.get_job_count())
    ash.reschedule()
    self.assertEquals(1, ash.get_job_count())


  def test_alert_collector_purge(self):
    definition_json = { "name": "namenode_process",
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
            "text": "(Unit Tests) TCP OK - {0:.4f} response time on port {1}"
          },
          "critical": {
            "text": "(Unit Tests) Could not load process info: {0}"
          }
        }
      }
    }

    configuration = {'hdfs-site' :
      { 'my-key': 'value1' }
    }

    collector = AlertCollector()
    cluster_configuration = self.__get_cluster_configuration()
    self.__update_cluster_configuration(cluster_configuration, configuration)

    alert = PortAlert(definition_json, definition_json['source'])
    alert.set_helpers(collector, cluster_configuration)
    alert.set_cluster("c1", "c6401.ambari.apache.org")
    self.assertEquals(6, alert.interval())

    res = alert.collect()

    alerts = collector.alerts()
    self.assertEquals(0, len(collector.alerts()))

    self.assertTrue(alerts[0] is not None)
    self.assertEquals('CRITICAL', alerts[0]['state'])

    collector.remove_by_uuid('c1f73191-4481-4435-8dae-fd380e4c0be1')
    self.assertEquals(0,len(collector.alerts()))


  def test_disabled_definitions(self):
    test_file_path = os.path.join('ambari_agent', 'dummy_files')
    test_stack_path = os.path.join('ambari_agent', 'dummy_files')
    test_common_services_path = os.path.join('ambari_agent', 'dummy_files')
    test_host_scripts_path = os.path.join('ambari_agent', 'dummy_files')

    cluster_configuration = self.__get_cluster_configuration()

    ash = AlertSchedulerHandler(test_file_path, test_stack_path,
      test_common_services_path, test_host_scripts_path, cluster_configuration,
      None)

    ash.start()

    self.assertEquals(1, ash.get_job_count())

    definition_json = { "name": "namenode_process",
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
            "text": "(Unit Tests) TCP OK - {0:.4f} response time on port {1}"
          },
          "critical": {
            "text": "(Unit Tests) Could not load process info: {0}"
          }
        }
      }
    }

    alert = PortAlert(definition_json, definition_json['source'])
    ash.schedule_definition(alert)

    self.assertEquals(2, ash.get_job_count())

    definition_json['enabled'] = False
    alert = PortAlert(definition_json, definition_json['source'])
    ash.schedule_definition(alert)

    # verify disabled alert not scheduled
    self.assertEquals(2, ash.get_job_count())

    definition_json['enabled'] = True
    pa = PortAlert(definition_json, definition_json['source'])
    ash.schedule_definition(pa)

    # verify enabled alert was scheduled
    self.assertEquals(3, ash.get_job_count())

  def test_immediate_alert(self):
    test_file_path = os.path.join('ambari_agent', 'dummy_files')
    test_stack_path = os.path.join('ambari_agent', 'dummy_files')
    test_common_services_path = os.path.join('ambari_agent', 'dummy_files')
    test_host_scripts_path = os.path.join('ambari_agent', 'dummy_files')

    cluster_configuration = self.__get_cluster_configuration()
    ash = AlertSchedulerHandler(test_file_path, test_stack_path,
      test_common_services_path, test_host_scripts_path, cluster_configuration,
      None)

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
                "text": "(Unit Tests) TCP OK - {0:.4f} response time on port {1}"
              },
              "critical": {
                "text": "(Unit Tests) Could not load process info: {0}"
              }
            }
          }
        }
      } ]

    # execute the alert immediately and verify that the collector has the result
    ash.execute_alert(execution_commands)
    self.assertEquals(1, len(ash._collector.alerts()))


  def test_skipped_alert(self):
    definition_json = {
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
    definition_json['source']['stacks_directory'] = os.path.join('ambari_agent', 'dummy_files')
    definition_json['source']['common_services_directory'] = os.path.join('ambari_agent', 'common-services')
    definition_json['source']['host_scripts_directory'] = os.path.join('ambari_agent', 'host_scripts')

    configuration = {'foo-site' :
      { 'skip': 'true' }
    }

    collector = AlertCollector()
    cluster_configuration = self.__get_cluster_configuration()
    self.__update_cluster_configuration(cluster_configuration, configuration)

    alert = ScriptAlert(definition_json, definition_json['source'], None)

    # instruct the test alert script to be skipped
    alert.set_helpers(collector, cluster_configuration )
    alert.set_cluster("c1", "c6401.ambari.apache.org")

    self.assertEquals(definition_json['source']['path'], alert.path)
    self.assertEquals(definition_json['source']['stacks_directory'], alert.stacks_dir)
    self.assertEquals(definition_json['source']['common_services_directory'], alert.common_services_dir)
    self.assertEquals(definition_json['source']['host_scripts_directory'], alert.host_scripts_dir)

    # ensure that it was skipped
    self.assertEquals(0,len(collector.alerts()))


  def test_default_reporting_text(self):
    definition_json = {
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

    alert = ScriptAlert(definition_json, definition_json['source'], None)
    self.assertEquals(alert._get_reporting_text(alert.RESULT_OK), '{0}')
    self.assertEquals(alert._get_reporting_text(alert.RESULT_WARNING), '{0}')
    self.assertEquals(alert._get_reporting_text(alert.RESULT_CRITICAL), '{0}')

    definition_json['source']['type'] = 'PORT'
    alert = PortAlert(definition_json, definition_json['source'])
    self.assertEquals(alert._get_reporting_text(alert.RESULT_OK), 'TCP OK - {0:.4f} response on port {1}')
    self.assertEquals(alert._get_reporting_text(alert.RESULT_WARNING), 'TCP OK - {0:.4f} response on port {1}')
    self.assertEquals(alert._get_reporting_text(alert.RESULT_CRITICAL), 'Connection failed: {0} to {1}:{2}')

    definition_json['source']['type'] = 'WEB'
    alert = WebAlert(definition_json, definition_json['source'], None)
    self.assertEquals(alert._get_reporting_text(alert.RESULT_OK), 'HTTP {0} response in {2:.4f} seconds')
    self.assertEquals(alert._get_reporting_text(alert.RESULT_WARNING), 'HTTP {0} response in {2:.4f} seconds')
    self.assertEquals(alert._get_reporting_text(alert.RESULT_CRITICAL), 'Connection failed to {1}')

    definition_json['source']['type'] = 'METRIC'
    alert = MetricAlert(definition_json, definition_json['source'])
    self.assertEquals(alert._get_reporting_text(alert.RESULT_OK), '{0}')
    self.assertEquals(alert._get_reporting_text(alert.RESULT_WARNING), '{0}')
    self.assertEquals(alert._get_reporting_text(alert.RESULT_CRITICAL), '{0}')


  def test_configuration_updates(self):
    definition_json = {
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
    definition_json['source']['stacks_directory'] = os.path.join('ambari_agent', 'dummy_files')
    definition_json['source']['common_services_directory'] = os.path.join('ambari_agent', 'common-services')
    definition_json['source']['host_scripts_directory'] = os.path.join('ambari_agent', 'host_scripts')

    configuration = {'foo-site' :
      { 'bar': 'rendered-bar', 'baz' : 'rendered-baz' }
    }

    # populate the configuration cache with the initial configs
    collector = AlertCollector()
    cluster_configuration = self.__get_cluster_configuration()
    self.__update_cluster_configuration(cluster_configuration, configuration)

    # run the alert and verify the output
    alert = ScriptAlert(definition_json, definition_json['source'], MagicMock())
    alert.set_helpers(collector, cluster_configuration )
    alert.set_cluster("c1", "c6401.ambari.apache.org")
    alert.collect()

    alerts = collector.alerts()
    self.assertEquals(0, len(collector.alerts()))

    self.assertEquals('WARNING', alerts[0]['state'])
    self.assertEquals('bar is rendered-bar, baz is rendered-baz', alerts[0]['text'])

    # now update only the configs and run the same alert again and check
    # for different output
    configuration = {'foo-site' :
      { 'bar': 'rendered-bar2', 'baz' : 'rendered-baz2' }
    }

    # populate the configuration cache with the initial configs
    self.__update_cluster_configuration(cluster_configuration, configuration)

    alert.collect()

    alerts = collector.alerts()
    self.assertEquals(0, len(collector.alerts()))

    self.assertEquals('WARNING', alerts[0]['state'])
    self.assertEquals('bar is rendered-bar2, baz is rendered-baz2', alerts[0]['text'])


  def test_uri_structure_parsing(self):
    uri_structure = {
      "http": "{{hdfs-site/dfs.namenode.http.address}}",
      "https": "{{hdfs-site/dfs.namenode.https.address}}",
      "https_property": "{{hdfs-site/dfs.http.policy}}",
      "https_property_value": "HTTPS_ONLY",
      "high_availability": {
        "nameservice": "{{hdfs-site/dfs.nameservices}}",
        "alias_key" : "{{hdfs-site/dfs.ha.namenodes.{{ha-nameservice}}}}",
        "http_pattern" : "{{hdfs-site/dfs.namenode.http-address.{{ha-nameservice}}.{{alias}}}}",
        "https_pattern" : "{{hdfs-site/dfs.namenode.https-address.{{ha-nameservice}}.{{alias}}}}"
      }
    }

    configuration = {'hdfs-site' :
      { 'dfs.namenode.http.address' : '1.2.3.4:80',
        'dfs.namenode.https.address' : '1.2.3.4:443' }
    }

    collector = AlertCollector()
    cluster_configuration = self.__get_cluster_configuration()
    self.__update_cluster_configuration(cluster_configuration, configuration)

    alert = MockAlert()
    alert.set_helpers(collector, cluster_configuration)
    alert.set_cluster("c1", "c6401.ambari.apache.org")
    uri_keys = alert._lookup_uri_property_keys(uri_structure)
    self.assertFalse(alert._check_uri_ssl_property(uri_keys))

    uri = alert._get_uri_from_structure(uri_keys)
    self.assertEqual( '1.2.3.4:80', uri.uri )
    self.assertEqual( False, uri.is_ssl_enabled )

    configuration = {'hdfs-site' :
      { 'dfs.http.policy' : 'HTTP_ONLY',
        'dfs.namenode.http.address' : '1.2.3.4:80',
        'dfs.namenode.https.address' : '1.2.3.4:443' }
    }

    self.__update_cluster_configuration(cluster_configuration, configuration)
    uri_keys = alert._lookup_uri_property_keys(uri_structure)
    self.assertFalse(alert._check_uri_ssl_property(uri_keys))

    uri = alert._get_uri_from_structure(uri_keys)
    self.assertEqual( '1.2.3.4:80', uri.uri )
    self.assertEqual( False, uri.is_ssl_enabled )

    # switch to SSL
    configuration = {'hdfs-site' :
      { 'dfs.http.policy' : 'HTTPS_ONLY',
        'dfs.namenode.http.address' : '1.2.3.4:80',
        'dfs.namenode.https.address' : '1.2.3.4:443' }
    }

    self.__update_cluster_configuration(cluster_configuration, configuration)
    uri_keys = alert._lookup_uri_property_keys(uri_structure)
    self.assertTrue(alert._check_uri_ssl_property(uri_keys))

    uri = alert._get_uri_from_structure(uri_keys)
    self.assertEqual( '1.2.3.4:443', uri.uri )
    self.assertEqual( True, uri.is_ssl_enabled )

    # test HA
    configuration = {'hdfs-site' :
      { 'dfs.http.policy' : 'HTTP_ONLY',
        'dfs.namenode.http.address' : '1.2.3.4:80',
        'dfs.namenode.https.address' : '1.2.3.4:443',
        'dfs.nameservices' : 'c1ha',
        'dfs.ha.namenodes.c1ha' : 'nn1, nn2',
        'dfs.namenode.http-address.c1ha.nn1' : 'c6401.ambari.apache.org:8080',
        'dfs.namenode.http-address.c1ha.nn2' : 'c6402.ambari.apache.org:8080',
      }
    }

    self.__update_cluster_configuration(cluster_configuration, configuration)
    uri_keys = alert._lookup_uri_property_keys(uri_structure)
    self.assertFalse(alert._check_uri_ssl_property(uri_keys))

    uri = alert._get_uri_from_structure(uri_keys)
    self.assertEqual( 'c6401.ambari.apache.org:8080', uri.uri )
    self.assertEqual( False, uri.is_ssl_enabled )

    # test HA SSL
    configuration = {'hdfs-site' :
      { 'dfs.http.policy' : 'HTTPS_ONLY',
        'dfs.namenode.http.address' : '1.2.3.4:80',
        'dfs.namenode.https.address' : '1.2.3.4:443',
        'dfs.nameservices' : 'c1ha',
        'dfs.ha.namenodes.c1ha' : 'nn1, nn2',
        'dfs.namenode.http-address.c1ha.nn1' : 'c6401.ambari.apache.org:8080',
        'dfs.namenode.http-address.c1ha.nn2' : 'c6402.ambari.apache.org:8080',
        'dfs.namenode.https-address.c1ha.nn1' : 'c6401.ambari.apache.org:8443',
        'dfs.namenode.https-address.c1ha.nn2' : 'c6402.ambari.apache.org:8443',
      }
    }

    self.__update_cluster_configuration(cluster_configuration, configuration)
    uri_keys = alert._lookup_uri_property_keys(uri_structure)
    self.assertTrue(alert._check_uri_ssl_property(uri_keys))

    uri = alert._get_uri_from_structure(uri_keys)
    self.assertEqual( 'c6401.ambari.apache.org:8443', uri.uri )
    self.assertEqual( True, uri.is_ssl_enabled )



  def __get_cluster_configuration(self):
    """
    Gets an instance of the cluster cache where the file read and write
    operations have been mocked out
    :return:
    """
    with patch("__builtin__.open") as open_mock:
      open_mock.side_effect = self.open_side_effect
      cluster_configuration = ClusterConfiguration("")
      return cluster_configuration


  def __update_cluster_configuration(self, cluster_configuration, configuration):
    """
    Updates the configuration cache, using as mock file as the disk based
    cache so that a file is not created during tests
    :return:
    """
    with patch("__builtin__.open") as open_mock:
      open_mock.side_effect = self.open_side_effect
      cluster_configuration._update_configurations("c1", configuration)


  def open_side_effect(self, file, mode):
    if mode == 'w':
      file_mock = MagicMock()
      return file_mock
    else:
      return self.original_open(file, mode)


class MockAlert(BaseAlert):
  """
  Mock class for testing
  """
  def __init__(self):
    super(MockAlert, self).__init__(None, None)

  def get_name(self):
    return "mock_alert"
