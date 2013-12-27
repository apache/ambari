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

class TestServiceModel(unittest.TestCase):

  def setUp(self):
    http_client_logger = logging.getLogger()
    http_client_logger.info('Running test:' + self.id())

  def create_service(self, http_client_mock = MagicMock()):
    http_client_mock.invoke.side_effect = HttpClientInvoker.http_client_invoke_side_effects
    client = AmbariClient("localhost", 8080, "admin", "admin", version=1, client=http_client_mock)
    cluster = client.get_cluster('test1')
    service = cluster.get_service('GANGLIA')
    return service

  def test_start(self):
    http_client_mock = MagicMock()

    expected_path = '//clusters/test1/services/GANGLIA'
    expected_payload = {'ServiceInfo': {'state': 'STARTED'}}

    service = self.create_service(http_client_mock)
    status = service.start()

    self.assertEqual(status.get_request_path(), 'clusters/test1/requests/19')
    http_client_mock.invoke.assert_called_with('PUT', expected_path, headers=None, payload=expected_payload)

  def test_stop(self):
    http_client_mock = MagicMock()

    expected_path = '//clusters/test1/services/GANGLIA'
    expected_payload = {"ServiceInfo": {"state": "INSTALLED"}}

    service = self.create_service(http_client_mock)
    status = service.stop()

    self.assertEqual(status.get_request_path(), 'clusters/test1/requests/19')
    http_client_mock.invoke.assert_called_with('PUT', expected_path, headers=None, payload=expected_payload)

  def test_install(self):
    http_client_mock = MagicMock()

    expected_path = '//clusters/test1/services/GANGLIA'
    expected_payload = {"ServiceInfo": {"state": "INSTALLED"}}

    service = self.create_service(http_client_mock)
    status = service.install()

    self.assertEqual(status.get_request_path(), 'clusters/test1/requests/19')
    http_client_mock.invoke.assert_called_with('PUT', expected_path, headers=None, payload=expected_payload)

  def test_get_service_components(self):
    http_client_mock = MagicMock()

    expected_path = '//clusters/test1/services/GANGLIA/components?fields=*'

    service = self.create_service(http_client_mock)
    components = service.get_service_components()

    self.assertEqual(components[0].component_name, "GANGLIA_MONITOR")
    self.assertEqual(components[0].state, "STARTED")
    self.assertEqual(components[1].component_name, "GANGLIA_SERVER")
    self.assertEqual(components[1].state, "INSTALLED")

    http_client_mock.invoke.assert_called_with('GET', expected_path, headers=None, payload=None)

  def test_get_service_component(self):
    http_client_mock = MagicMock()

    expected_path = '//clusters/test1/services/GANGLIA/components/GANGLIA_SERVER'

    service = self.create_service(http_client_mock)
    component = service.get_service_component("GANGLIA_SERVER")

    self.assertEqual(component.component_name, "GANGLIA_SERVER")
    self.assertEqual(component.service_name, "GANGLIA")
    self.assertEqual(component.state, "STARTED")

    http_client_mock.invoke.assert_called_with('GET', expected_path, headers=None, payload=None)
