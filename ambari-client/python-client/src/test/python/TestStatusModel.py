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

from ambari_client.model.status import StatusModel
from mock.mock import MagicMock
from HttpClientInvoker import HttpClientInvoker

from ambari_client.ambari_api import AmbariClient
import unittest


class TestStatusModel(unittest.TestCase):

  def setUp(self):
    http_client_logger = logging.getLogger()
    http_client_logger.info('Running test:' + self.id())

  def create_service(self, http_client_mock=MagicMock()):
    http_client_mock.invoke.side_effect = HttpClientInvoker.http_client_invoke_side_effects
    client = AmbariClient("localhost", 8080, "admin", "admin", version=1, client=http_client_mock)
    cluster = client.get_cluster('test1')
    service = cluster.get_service('GANGLIA')
    return service

  def create_client(self, http_client_mock=MagicMock()):
    http_client_mock.invoke.side_effect = HttpClientInvoker.http_client_invoke_side_effects
    client = AmbariClient("localhost", 8080, "admin", "admin", version=1, client=http_client_mock)
    return client

  def test_get_request_path(self):
    http_client_mock = MagicMock()

    expected_payload = {'ServiceInfo': {'state': 'INSTALLED'}}
    expected_path = '//clusters/test1/services/GANGLIA'
    expected_request_path = 'clusters/test1/requests/19'

    service = self.create_service(http_client_mock)
    status = service.stop()

    self.assertEqual(status.get_request_path(), expected_request_path)
    http_client_mock.invoke.assert_called_with('PUT', expected_path, headers=None, payload=expected_payload)

  def test_is_error(self):
    error_model = StatusModel(None, 400)
    ok_model = StatusModel(None, 201)

    self.assertTrue(error_model.is_error())
    self.assertFalse(ok_model.is_error())

  def ADisabledtest_get_bootstrap_path(self):
    http_client_mock = MagicMock()

    ssh_key = 'abc!@#$%^&*()_:"|<>?[];\'\\./'
    host_list = ['dev05.hortonworks.com', 'dev06.hortonworks.com']
    ssh_user = 'root'

    expected_path = '//bootstrap'
    expected_headers = {'Content-Type': 'application/json'}
    expected_request = {'user': ssh_user, 'hosts': str(host_list), 'verbose': True, 'sshKey': ssh_key}
    expected_bootstrap_path = '/bootstrap/5'
    client = self.create_client(http_client_mock)
    resp = client.bootstrap_hosts(host_list, ssh_key, ssh_user)

    self.assertEqual(resp.get_bootstrap_path(), expected_bootstrap_path)
    http_client_mock.invoke.assert_called_with('POST', expected_path, headers=expected_headers, payload=expected_request)
