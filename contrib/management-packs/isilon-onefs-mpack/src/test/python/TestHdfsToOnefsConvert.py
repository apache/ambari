#!/usr/bin/env python3
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

import importlib.util
import json
import os
import tempfile
from unittest import TestCase
from unittest.mock import MagicMock, patch


MODULE_PATH = os.path.abspath(os.path.join(
  os.path.dirname(__file__), '..', '..', 'main', 'tools',
  'hdfs_to_onefs_convert.py'))
SPEC = importlib.util.spec_from_file_location('hdfs_to_onefs_convert', MODULE_PATH)
converter = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(converter)


class TestHttpCompatibility(TestCase):
  def test_basic_auth_encodes_utf8_credentials(self):
    request = MagicMock()

    converter.BasicAuth('user', 'pass').authenticate(request)

    request.add_header.assert_called_once_with(
      'Authorization', 'Basic dXNlcjpwYXNz')

  def test_json_request_is_encoded_and_url_parameters_are_escaped(self):
    client = converter.RestClient(
      converter.Url('https://example.test/api'),
      converter.BasicAuth('user', 'pass'),
      request_transformer=json.dumps)

    request, _ = client._request(
      converter.Url('items').query_params({'name': 'a value'}),
      'POST', {'enabled': True})

    self.assertEqual('POST', request.get_method())
    self.assertEqual(b'{"enabled": true}', request.data)
    self.assertIn('name=a+value', request.full_url)

  @patch.object(converter.ssl, 'create_default_context')
  def test_https_uses_verified_context_and_explicit_ca(self, create_context):
    converter.SslContext('/tmp/ambari-ca.pem').build('https://example.test')

    create_context.assert_called_once_with(cafile='/tmp/ambari-ca.pem')
    self.assertIsNone(converter.SslContext().build('http://example.test'))


class TestAsyncResult(TestCase):
  def _result(self, statuses):
    client = MagicMock()
    client.get.side_effect = [
      (200, {'Requests': {'request_status': status}}) for status in statuses
    ]
    return converter.AsyncResult(client, {
      'Requests': {'status': 'PENDING', 'id': 7},
      'href': '/api/v1/requests/7',
    })

  def test_wait_returns_completed_status(self):
    result = self._result(['PENDING', 'COMPLETED'])

    with patch.object(converter.time, 'sleep'):
      self.assertEqual('COMPLETED', result.wait(timeout=1))

  def test_wait_raises_for_failed_request(self):
    result = self._result(['FAILED'])

    with self.assertRaises(converter.OperationFailed):
      result.wait(timeout=1)

  def test_wait_times_out(self):
    result = self._result(['PENDING'])

    with patch.object(converter.time, 'monotonic', side_effect=[0, 1]):
      with self.assertRaises(converter.OperationFailed):
        result.wait(timeout=1, poll_interval=0)


class TestFsStorage(TestCase):
  def test_round_trip_uses_json(self):
    with tempfile.TemporaryDirectory() as directory:
      storage = converter.FsStorage(directory)

      storage.save('core-site', {'key': ['value']})

      self.assertEqual({'key': ['value']}, storage.load('core-site'))

  def test_rejects_path_traversal(self):
    with tempfile.TemporaryDirectory() as directory:
      storage = converter.FsStorage(directory)

      with self.assertRaises(ValueError):
        storage.save('../outside', {})

  def test_does_not_execute_legacy_repr_payload(self):
    with tempfile.TemporaryDirectory() as directory:
      payload = os.path.join(directory, 'saved-core-site.json')
      with open(payload, 'w', encoding='utf-8') as stream:
        stream.write("__import__('os').system('false')")

      with self.assertRaises(json.JSONDecodeError):
        converter.FsStorage(directory).load('core-site')
