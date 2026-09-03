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

import json
import unittest
from unittest.mock import MagicMock, patch
from urllib.parse import parse_qs, urlsplit

from resource_management.libraries.functions.ranger_functions_v2 import (
  RangeradminV2,
)
from resource_management.core.logger import Logger


class TestRangerFunctionsV2(unittest.TestCase):
  def setUp(self):
    Logger.initialize_logger()
    self.client = RangeradminV2(
      url="https://ranger.example", skip_if_rangeradmin_down=False
    )

  def test_secure_repository_query_is_encoded(self):
    name = "cluster&isEnabled=false_kms"
    response = ["invalid", {}, {"name": "different"}, {"name": name}]
    with patch.object(
      self.client,
      "call_curl_request",
      return_value=(json.dumps(response), None, 0),
    ) as request:
      repository = self.client.get_repository_by_name_curl(
        "kms",
        "/etc/security/keytabs/kms.keytab",
        "kms/host@EXAMPLE.COM",
        name,
        "kms+plugin",
        "true",
        is_keyadmin=True,
      )

    self.assertEqual(name, repository["name"])
    query = parse_qs(urlsplit(request.call_args.args[3]).query)
    self.assertEqual([name], query["serviceName"])
    self.assertEqual(["kms+plugin"], query["serviceType"])
    self.assertEqual(["true"], query["isEnabled"])
    self.assertEqual(["keyadmin"], query["suser"])

  def test_http_repository_update_accepts_python3_bytes_response(self):
    result = MagicMock()
    result.getcode.return_value = 200
    result.read.return_value = b'{"name":"cluster/name"}'
    with patch(
      "resource_management.libraries.functions.ranger_functions_v2.openurl",
      return_value=result,
    ):
      response = self.client.update_repository_http(
        "kms",
        "cluster/name",
        {"name": "cluster/name"},
        "admin",
        "password",
      )

    self.assertEqual("cluster/name", response["name"])

  def test_http_repository_create_accepts_python3_bytes_response(self):
    result = MagicMock()
    result.getcode.return_value = 200
    result.read.return_value = b'{"name":"cluster"}'
    with patch(
      "resource_management.libraries.functions.ranger_functions_v2.openurl",
      return_value=result,
    ):
      response = self.client.create_repository_http(
        '{"name":"cluster"}', "admin:password"
      )

    self.assertEqual("cluster", response["name"])

  def test_repository_update_encodes_path_and_query(self):
    with patch.object(
      self.client,
      "call_curl_request",
      return_value=('{"name":"cluster/name"}', None, 0),
    ) as request:
      self.client.update_repository_curl(
        "kms",
        "cluster/name?disabled=true",
        {"name": "cluster/name"},
        "kms",
        "kms/host@EXAMPLE.COM",
        "/etc/security/keytabs/kms.keytab",
        force_rename=True,
      )

    requested_url = request.call_args.args[3]
    self.assertIn("/name/cluster%2Fname%3Fdisabled%3Dtrue", requested_url)
    self.assertEqual(
      ["true"], parse_qs(urlsplit(requested_url).query)["forceRename"]
    )


if __name__ == "__main__":
  unittest.main()
