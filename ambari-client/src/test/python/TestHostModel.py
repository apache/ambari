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
import logging

from mock.mock import MagicMock, patch
from HttpClientInvoker import HttpClientInvoker

from ambari_client.ambari_api import  AmbariClient

import unittest

class TestHostModel(unittest.TestCase):

  def setUp(self):
    http_client_logger = logging.getLogger()
    http_client_logger.info('Running test:' + self.id())

  def create_host(self, http_client_mock = MagicMock()):
    http_client_mock.invoke.side_effect = HttpClientInvoker.http_client_invoke_side_effects
    client = AmbariClient("localhost", 8080, "admin", "admin", version=1, client=http_client_mock)
    cluster = client.get_cluster('test1')
    host = cluster.get_host('myhost')
    return host

  def test_get_host_components(self):
    http_client_mock = MagicMock()

    expected_path = '//clusters/test1/hosts/myhost/host_components?fields=HostRoles/state'

    host = self.create_host(http_client_mock)
    host_components = host.get_host_components()

    self.assertEqual(host_components[0].component_name,"DATANODE")
    self.assertEqual(host_components[0].state,"STARTED")
    self.assertEqual(host_components[3].component_name,"HBASE_MASTER")
    self.assertEqual(host_components[3].state,"STARTED")
    http_client_mock.invoke.assert_called_with('GET', expected_path, headers=None, payload=None)

  def test_get_host_component(self):
    http_client_mock = MagicMock()

    expected_path = '//clusters/test1/hosts/myhost/host_components/DATANODE'

    host =  self.create_host(http_client_mock)
    component = host.get_host_component("DATANODE")

    self.assertEqual(component.component_name,"DATANODE")
    self.assertEqual(component.state,"STARTED")
    self.assertEqual(component.host_name,"myhost")

    http_client_mock.invoke.assert_called_with('GET', expected_path, headers=None, payload=None)

  def test_assign_role(self):
    http_client_mock = MagicMock()

    expected_path = '//clusters/test1/hosts?Hosts/host_name=myhost'
    expected_payload = {'host_components': [{'HostRoles': {'component_name': 'GANGLIA_SERVER'}}]}

    host =  self.create_host(http_client_mock)
    status = host.assign_role("GANGLIA_SERVER")

    self.assertTrue(status.status, 201)
    http_client_mock.invoke.assert_called_with('POST', expected_path, headers=None, payload=expected_payload)
