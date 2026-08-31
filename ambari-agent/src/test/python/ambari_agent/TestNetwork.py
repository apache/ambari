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

from ambari_commons.network import HTTPSConnectionWithCustomSslVersion


class TestNetwork(TestCase):
  @patch("ambari_commons.network.ssl.SSLContext")
  def test_https_connection_uses_explicit_unverified_context(self, context_factory):
    context = MagicMock()
    context_factory.return_value = context

    connection = HTTPSConnectionWithCustomSslVersion(
      "server.example", 8443, ssl.PROTOCOL_TLS_CLIENT
    )

    context_factory.assert_called_once_with(ssl.PROTOCOL_TLS_CLIENT)
    self.assertFalse(context.check_hostname)
    self.assertEqual(context.verify_mode, ssl.CERT_NONE)
    self.assertIs(connection._context, context)
