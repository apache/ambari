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

import io
from ambari_commons import import_utils
import os
import sys
import json
from unittest.mock import patch, MagicMock
from unittest import TestCase
import urllib.request, urllib.parse, urllib.error


def get_configs():
  test_directory = os.path.dirname(os.path.abspath(__file__))
  configs_path = os.path.join(test_directory, "../../main/resources/scripts/configs.py")
  with open(configs_path, "rb") as fp:
    return import_utils.load_module("configs", fp, configs_path, (".py", "rb", import_utils.PY_SOURCE))


configs = get_configs()


class TestConfigs(TestCase):
  def setUp(self):
    out = io.StringIO()
    sys.stdout = out

  def tearDown(self):
    sys.stdout = sys.__stdout__

  def get_url_open_side_effect(self, response_mapping):
    def urlopen_side_effect(request, *args, **kwargs):
      response = MagicMock()
      request_type = request.get_method()
      request_url = request.get_full_url()
      if request_type in response_mapping:
        req_type = response_mapping[request_type]
        type_responses = req_type.get("body", None)
        if type_responses is not None:
          response_data = type_responses[request_url]
          response.read.side_effect = [response_data]
        request_check = req_type.get("request_assertion", None)
        if request_check is not None:
          request_body = json.loads(request.data)
          request_check.get(request_url, lambda x: None)(request_body)
      return response

    return urlopen_side_effect

  @patch("urllib.request.urlopen")
  @patch.object(configs.ssl, "create_default_context")
  def test_api_accessor_uses_ca_and_timeout(self, context_mock, urlopen_mock):
    response = MagicMock()
    response.read.return_value = b"{}"
    urlopen_mock.return_value = response
    context = context_mock.return_value

    accessor = configs.api_accessor(
      "server.example", "user", "password", "https", "8443", ca_cert="ca.pem"
    )
    accessor("/api/v1/clusters")

    context_mock.assert_called_once_with(cafile="ca.pem")
    urlopen_mock.assert_called_once_with(
      urlopen_mock.call_args.args[0], context=context, timeout=30
    )
    response.close.assert_called_once_with()

  @patch("urllib.request.urlopen")
  def test_api_accessor_unsafe_mode_is_explicitly_unverified(self, urlopen_mock):
    response = MagicMock()
    response.read.return_value = b"{}"
    urlopen_mock.return_value = response

    accessor = configs.api_accessor(
      "server.example", "user", "password", "https", "8443", unsafe=True
    )
    accessor("/api/v1/clusters")

    context = urlopen_mock.call_args.kwargs["context"]
    self.assertEqual(context.verify_mode, configs.ssl.CERT_NONE)
    self.assertFalse(context.check_hostname)

  @patch.object(configs, "output_to_file")
  @patch("urllib.request.urlopen")
  def test_get_config_to_file(self, urlopen_method, to_file_method):
    response_mapping = {
      "GET": {
        "body": {
          "http://localhost:8081/api/v1/clusters/cluster1?fields=Clusters/desired_configs": '{"Clusters":{"desired_configs":{"hdfs-site":{"tag":"version1"}}}}',
          "http://localhost:8081/api/v1/clusters/cluster1/configurations?type=hdfs-site&tag=version1": '{"items":[{"properties":{"config1": "value1", "config2": "value2"}}]}',
        }
      }
    }

    def config_assertion(config):
      self.assertEqual(config["properties"], {"config1": "value1", "config2": "value2"})

    urlopen_method.side_effect = self.get_url_open_side_effect(response_mapping)
    to_file_method.return_value = config_assertion
    sys.argv = [
      "configs.py",
      "-u",
      "user",
      "-p",
      "password",
      "-t",
      "8081",
      "-s",
      "http",
      "-a",
      "get",
      "-l",
      "localhost",
      "-n",
      "cluster1",
      "-c",
      "hdfs-site",
    ]
    configs.main()

  @patch.object(configs, "output_to_file")
  @patch("urllib.request.urlopen")
  def test_update_specific_config(self, urlopen_method, to_file_method):
    response_mapping = {
      "GET": {
        "body": {
          "https://localhost:8081/api/v1/clusters/cluster1?fields=Clusters/desired_configs": '{"Clusters":{"desired_configs":{"hdfs-site":{"tag":"version1"}}}}',
          "https://localhost:8081/api/v1/clusters/cluster1/configurations?type=hdfs-site&tag=version1": '{"items":[{"properties":{"config1": "value1", "config2": "value2"}}]}',
        }
      },
      "PUT": {
        "request_assertion": {
          "https://localhost:8081/api/v1/clusters/cluster1": lambda request_body: self.assertEqual(
            request_body["Clusters"]["desired_configs"]["properties"],
            {"config1": "value3", "config2": "value2"},
          )
        }
      },
    }
    urlopen_method.side_effect = self.get_url_open_side_effect(response_mapping)
    sys.argv = [
      "configs.py",
      "-u",
      "user",
      "-p",
      "password",
      "-t",
      "8081",
      "-s",
      "https",
      "-a",
      "set",
      "-l",
      "localhost",
      "-n",
      "cluster1",
      "-c",
      "hdfs-site",
      "-k",
      "config1",
      "-v",
      "value3",
    ]
    configs.main()

  @patch.object(configs, "output_to_file")
  @patch("urllib.request.urlopen")
  def test_update_from_file(self, urlopen_method, to_file_method):
    response_mapping = {
      "GET": {
        "body": {
          "https://localhost:8081/api/v1/clusters/cluster1?fields=Clusters/desired_configs": '{"Clusters":{"desired_configs":{"hdfs-site":{"tag":"version1"}}}}',
          "https://localhost:8081/api/v1/clusters/cluster1/configurations?type=hdfs-site&tag=version1": '{"items":[{"properties":{"config1": "value1", "config2": "value2"}}]}',
        }
      },
      "PUT": {
        "request_assertion": {
          "https://localhost:8081/api/v1/clusters/cluster1": lambda request_body: self.assertEqual(
            request_body["Clusters"]["desired_configs"]["properties"],
            {"config1": "value3", "config2": "value2"},
          )
        }
      },
    }
    urlopen_method.side_effect = self.get_url_open_side_effect(response_mapping)
    sys.argv = [
      "configs.py",
      "-u",
      "user",
      "-p",
      "password",
      "-t",
      "8081",
      "-s",
      "https",
      "-a",
      "set",
      "-l",
      "localhost",
      "-n",
      "cluster1",
      "-c",
      "hdfs-site",
      "-k",
      "config1",
      "-v",
      "value3",
    ]
    configs.main()

  @patch.object(configs, "output_to_file")
  @patch("urllib.request.urlopen")
  def test_update_specific_config_with_attributes(self, urlopen_method, to_file_method):
    def update_check(request_body):
      self.assertEqual(
        request_body["Clusters"]["desired_configs"]["properties"],
        {"config1": "value4", "config2": "value2", "config3": "value3"},
      )
      self.assertEqual(
        request_body["Clusters"]["desired_configs"]["properties_attributes"],
        {"final": {"config1": "true", "config3": "true"}},
      )

    response_mapping = {
      "GET": {
        "body": {
          "https://localhost:8081/api/v1/clusters/cluster2?fields=Clusters/desired_configs": '{"Clusters":{"desired_configs":{"hdfs-site":{"tag":"version12"}}}}',
          "https://localhost:8081/api/v1/clusters/cluster2/configurations?type=hdfs-site&tag=version12": '{"items":[{"properties":{"config1": "value1", "config2": "value2", "config3": "value3"}, "properties_attributes":{"final":{"config1": "true", "config3": "true"}}}]}',
        }
      },
      "PUT": {
        "request_assertion": {
          "https://localhost:8081/api/v1/clusters/cluster2": update_check
        }
      },
    }
    urlopen_method.side_effect = self.get_url_open_side_effect(response_mapping)
    sys.argv = [
      "configs.py",
      "-u",
      "user",
      "-p",
      "password",
      "-t",
      "8081",
      "-s",
      "https",
      "-a",
      "set",
      "-l",
      "localhost",
      "-n",
      "cluster2",
      "-c",
      "hdfs-site",
      "-k",
      "config1",
      "-v",
      "value4",
    ]
    configs.main()

  @patch.object(configs, "output_to_file")
  @patch("urllib.request.urlopen")
  def test_delete_config(self, urlopen_method, to_file_method):
    response_mapping = {
      "GET": {
        "body": {
          "https://localhost:8081/api/v1/clusters/cluster1?fields=Clusters/desired_configs": '{"Clusters":{"desired_configs":{"hdfs-site":{"tag":"version1"}}}}',
          "https://localhost:8081/api/v1/clusters/cluster1/configurations?type=hdfs-site&tag=version1": '{"items":[{"properties":{"config1": "value1", "config2": "value2"}}]}',
        }
      },
      "PUT": {
        "request_assertion": {
          "https://localhost:8081/api/v1/clusters/cluster1": lambda request_body: self.assertEqual(
            request_body["Clusters"]["desired_configs"]["properties"],
            {"config2": "value2"},
          )
        }
      },
    }
    urlopen_method.side_effect = self.get_url_open_side_effect(response_mapping)

    sys.argv = [
      "configs.py",
      "-u",
      "user",
      "-p",
      "password",
      "-t",
      "8081",
      "-s",
      "https",
      "-a",
      "delete",
      "-l",
      "localhost",
      "-n",
      "cluster1",
      "-c",
      "hdfs-site",
      "-k",
      "config1",
    ]
    configs.main()

  @patch.object(configs, "output_to_file")
  @patch("urllib.request.urlopen")
  def test_delete_config_with_attributes(self, urlopen_method, to_file_method):
    def delete_check(request_body):
      self.assertEqual(
        request_body["Clusters"]["desired_configs"]["properties"],
        {"config2": "value2", "config3": "value3"},
      )
      self.assertEqual(
        request_body["Clusters"]["desired_configs"]["properties_attributes"],
        {"final": {"config3": "true"}},
      )

    response_mapping = {
      "GET": {
        "body": {
          "https://localhost:8081/api/v1/clusters/cluster2?fields=Clusters/desired_configs": '{"Clusters":{"desired_configs":{"hdfs-site":{"tag":"version12"}}}}',
          "https://localhost:8081/api/v1/clusters/cluster2/configurations?type=hdfs-site&tag=version12": '{"items":[{"properties":{"config1": "value1", "config2": "value2", "config3": "value3"}, "properties_attributes":{"final":{"config1": "true", "config3": "true"}}}]}',
        }
      },
      "PUT": {
        "request_assertion": {
          "https://localhost:8081/api/v1/clusters/cluster2": delete_check
        }
      },
    }
    urlopen_method.side_effect = self.get_url_open_side_effect(response_mapping)
    sys.argv = [
      "configs.py",
      "-u",
      "user",
      "-p",
      "password",
      "-t",
      "8081",
      "-s",
      "https",
      "-a",
      "delete",
      "-l",
      "localhost",
      "-n",
      "cluster2",
      "-c",
      "hdfs-site",
      "-k",
      "config1",
    ]
    configs.main()

  @patch.object(configs, "output_to_file")
  @patch("urllib.request.urlopen")
  def test_set_properties_from_xml(self, urlopen_method, to_file_method):
    response_mapping = {
      "GET": {
        "body": {
          "https://localhost:8081/api/v1/clusters/cluster1?fields=Clusters/desired_configs": '{"Clusters":{"desired_configs":{"hdfs-site":{"tag":"version1"}}}}',
          "https://localhost:8081/api/v1/clusters/cluster1/configurations?type=hdfs-site&tag=version1": '{"items":[{"properties":{"config3": "value3", "config4": "value4"}}]}',
        }
      },
      "PUT": {
        "request_assertion": {
          "https://localhost:8081/api/v1/clusters/cluster1": lambda request_body: self.assertEqual(
            request_body["Clusters"]["desired_configs"]["properties"],
            {"config1": "value1", "config2": "value2"},
          )
        }
      },
    }
    urlopen_method.side_effect = self.get_url_open_side_effect(response_mapping)

    test_directory = os.path.dirname(os.path.abspath(__file__))
    configs_path = os.path.join(test_directory, "../resources/TestConfigs-content.xml")

    sys.argv = [
      "configs.py",
      "-u",
      "user",
      "-p",
      "password",
      "-t",
      "8081",
      "-s",
      "https",
      "-a",
      "set",
      "-l",
      "localhost",
      "-n",
      "cluster1",
      "-c",
      "hdfs-site",
      "-f",
      configs_path,
    ]
    configs.main()
