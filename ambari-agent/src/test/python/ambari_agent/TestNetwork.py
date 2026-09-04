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

import ssl
from unittest import TestCase
from unittest.mock import MagicMock, patch

from ambari_commons.inet_utils import create_ssl_context
from ambari_commons.network import (
  HTTPSConnectionWithCustomSslVersion,
  build_url_opener,
)


class TestNetwork(TestCase):
  def test_tls_context_accepts_legacy_tls12_as_secure_minimum(self):
    context = create_ssl_context("PROTOCOL_TLSv1_2")

    self.assertEqual(ssl.PROTOCOL_TLS_CLIENT, context.protocol)
    self.assertEqual(ssl.TLSVersion.TLSv1_2, context.minimum_version)
    self.assertEqual(ssl.CERT_REQUIRED, context.verify_mode)
    self.assertTrue(context.check_hostname)

  def test_tls_context_rejects_insecure_or_unknown_protocols(self):
    for protocol in ("PROTOCOL_SSLv23", "PROTOCOL_TLSv1", "unknown"):
      with self.subTest(protocol=protocol):
        with self.assertRaisesRegex(ValueError, "Unsupported TLS client protocol"):
          create_ssl_context(protocol)

  @patch("ambari_commons.network.create_ssl_context")
  def test_https_connection_uses_explicit_verified_context(self, context_factory):
    context = MagicMock()
    context_factory.return_value = context

    connection = HTTPSConnectionWithCustomSslVersion(
      "server.example", 8443, ssl.PROTOCOL_TLS_CLIENT
    )

    context_factory.assert_called_once_with(ssl.PROTOCOL_TLS_CLIENT, None)
    self.assertIs(connection._context, context)

  @patch("ambari_commons.network.urllib.request.build_opener")
  def test_request_scoped_opener_disables_proxy_and_uses_tls_context(
    self, build_opener
  ):
    context = MagicMock()

    opener = build_url_opener(True, context)

    self.assertIs(opener, build_opener.return_value)
    handlers = build_opener.call_args.args
    self.assertIsInstance(handlers[0], __import__("urllib").request.ProxyHandler)
    self.assertEqual({}, handlers[0].proxies)
    self.assertIsInstance(handlers[1], __import__("urllib").request.HTTPSHandler)
    self.assertIs(context, handlers[1]._context)
