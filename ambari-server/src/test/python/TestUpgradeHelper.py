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


from mock.mock import MagicMock, call
from mock.mock import patch

from unittest import TestCase
import sys
import os
import unittest
import upgradeHelper
import json
import copy
from StringIO import StringIO


class UpgradeCatalogFactoryMock(upgradeHelper.UpgradeCatalogFactory):
  def __init__(self, data):
    self._load(data)

  def _load(self, data):
    fn = StringIO(data)
    with patch("__builtin__.open") as open_mock:
      open_mock.return_value = fn
      super(UpgradeCatalogFactoryMock, self)._load("")


class TestUpgradeHelper(TestCase):
  original_curl = None
  out = None
  catalog_from = "1.3"
  catalog_to = "2.2"
  catalog_cfg_type = "my type"
  required_service = "TEST"
  curl_response = "{}"
  test_catalog = """{
   "version": "1.0",
   "stacks": [
     {
       "name": "HDP",
       "old-version": "%s",
       "target-version": "%s",
       "options": {
         "config-types": {
           "%s": {
             "merged-copy": "yes"
           }
          }
       },
       "properties": {
         "%s": {
           "my property": {
             "value": "my value",
             "required-services": [\"%s\"]
           }
         }
       },
       "property-mapping": {
         "my replace property": "my property 2"
       }
     }
   ]
  }
  """

  def setUp(self):
    # replace original curl call to mock
    self.test_catalog = self.test_catalog % (self.catalog_from, self.catalog_to,
                                             self.catalog_cfg_type, self.catalog_cfg_type,
                                             self.required_service)

    self.original_curl = upgradeHelper.curl
    upgradeHelper.curl = self.magic_curl

    # mock logging methods
    upgradeHelper.logging.getLogger = MagicMock()
    upgradeHelper.logging.FileHandler = MagicMock()

    self.out = StringIO()
    sys.stdout = self.out

  def magic_curl(self, *args, **kwargs):
    resp = self.curl_response
    self.curl_response = "{}"
    if "parse" in kwargs and isinstance(resp, str) and kwargs["parse"] == True:
      resp = json.loads(resp)
    return resp

  def tearDown(self):
    sys.stdout = sys.__stdout__

  @patch("optparse.OptionParser")
  @patch("upgradeHelper.modify_configs")
  @patch("__builtin__.open")
  def test_ParseOptions(self, open_mock, modify_action_mock, option_parser_mock):
    class options(object):
      user = "test_user"
      hostname = "127.0.0.1"
      clustername = "test1"
      password = "test_password"
      upgrade_json = "catalog_file"
      from_stack = "0.0"
      to_stack = "1.3"
      logfile = "test.log"
      report = "report.txt"
      https = False
      port = "8080"
      warnings = []
      printonly = False

    args = ["update-configs"]
    modify_action_mock.return_value = MagicMock()
    test_mock = MagicMock()
    test_mock.parse_args = lambda: (options, args)
    option_parser_mock.return_value = test_mock

    upgradeHelper.main()

    self.assertEqual("8080", upgradeHelper.Options.API_PORT)
    self.assertEqual("http", upgradeHelper.Options.API_PROTOCOL)
    self.assertEqual(1, modify_action_mock.call_count)
    self.assertEqual({"user": options.user, "pass": options.password}, upgradeHelper.Options.API_TOKENS)
    self.assertEqual(options.clustername, upgradeHelper.Options.CLUSTER_NAME)

  def test_is_services_exists(self):
    old_services = upgradeHelper.Options.SERVICES

    upgradeHelper.Options.SERVICES = set(['TEST1', 'TEST2'])
    actual_result = upgradeHelper.is_services_exists(['TEST1'])

    # check for situation with two empty sets
    upgradeHelper.Options.SERVICES = set()
    actual_result_1 = upgradeHelper.is_services_exists([])

    upgradeHelper.Options.SERVICES = old_services

    self.assertEqual(True, actual_result)
    self.assertEqual(True, actual_result_1)


  @patch("__builtin__.open")
  @patch.object(os.path, "isfile")
  @patch("os.remove")
  def test_write_mapping(self, remove_mock, isfile_mock, open_mock):
    test_data = {
      "test_field": "test_value"
    }
    test_result = json.dumps(test_data)
    output = StringIO()
    isfile_mock.return_value = True
    open_mock.return_value = output

    # execute testing function
    upgradeHelper.write_mapping(test_data)

    self.assertEquals(1, isfile_mock.call_count)
    self.assertEquals(1, remove_mock.call_count)
    self.assertEquals(1, open_mock.call_count)

    # check for content
    self.assertEquals(test_result, output.getvalue())

  @patch("__builtin__.open")
  @patch.object(os.path, "isfile")
  def test_read_mapping(self, isfile_mock, open_mock):
    test_data = {
      "test_field": "test_value"
    }
    test_result = json.dumps(test_data)
    isfile_mock.return_value = True
    output = StringIO(test_result)
    open_mock.return_value = output

    # execute testing function
    actual_mapping = upgradeHelper.read_mapping()

    self.assertEquals(1, isfile_mock.call_count)
    self.assertEquals(1, open_mock.call_count)

    self.assertEquals(test_data, actual_mapping)

  @patch.object(upgradeHelper, "curl")
  @patch.object(upgradeHelper, "write_mapping")
  def test_get_mr1_mapping(self, write_mapping_mock, curl_mock):
    return_data = [
     {
      "host_components": [   # MAPREDUCE_CLIENT
        {
          "HostRoles": {
            "host_name": "test.host.vm"
           }
        }
      ]
     },
     {
      "host_components": [  # JOBTRACKER
        {
          "HostRoles": {
            "host_name": "test1.host.vm"
           }
        }
      ]
     },
     {
      "host_components": [  # TASKTRACKER
        {
          "HostRoles": {
            "host_name": "test2.host.vm"
           }
        }
      ]
     },
     {
      "host_components": [  # HISTORYSERVER
        {
          "HostRoles": {
            "host_name": "test3.host.vm"
           }
        }
      ]
     }
    ]
    expect_data = {
      "MAPREDUCE_CLIENT": ["test.host.vm"],
      "JOBTRACKER": ["test1.host.vm"],
      "TASKTRACKER": ["test2.host.vm"],
      "HISTORYSERVER": ["test3.host.vm"]
    }

    tricky_mock = MagicMock(side_effect=return_data)
    curl_mock.side_effect = tricky_mock

    # execute testing function
    upgradeHelper.get_mr1_mapping()

    self.assertEquals(write_mapping_mock.call_count, 1)
    self.assertEquals(expect_data, write_mapping_mock.call_args[0][0])

  @patch.object(upgradeHelper, "get_choice_string_input")
  def test_get_YN_input(self, get_choice_string_input_mock):
    yes = set(['yes', 'ye', 'y'])
    no = set(['no', 'n'])

    prompt = "test prompt"
    default = "default value"

    # execute testing function
    upgradeHelper.get_YN_input(prompt, default)

    expect_args = (prompt, default, yes, no)
    self.assertEquals(expect_args, get_choice_string_input_mock.call_args[0])

  @patch("__builtin__.raw_input")
  def test_get_choice_string_input(self, raw_input_mock):
    yes = set(['yes', 'ye', 'y'])
    no = set(['no', 'n'])
    input_answers = ["yes", "no", ""]
    tricky_mock = MagicMock(side_effect=input_answers)
    raw_input_mock.side_effect = tricky_mock
    default = "default value"

    expect_result = [True, False, default]
    actual_result = []
    for i in range(0, len(input_answers)):
      actual_result.append(upgradeHelper.get_choice_string_input("test prompt", default, yes, no))

    self.assertEquals(expect_result, actual_result)

  @patch.object(upgradeHelper, "get_YN_input")
  @patch.object(upgradeHelper, "read_mapping")
  @patch.object(upgradeHelper, "curl")
  def test_delete_mr(self, curl_mock, read_mapping_mock, get_YN_mock):
    COMPONENT_URL_FORMAT = upgradeHelper.Options.CLUSTER_URL + '/hosts/%s/host_components/%s'
    SERVICE_URL_FORMAT = upgradeHelper.Options.CLUSTER_URL + '/services/MAPREDUCE'
    NON_CLIENTS = ["JOBTRACKER", "TASKTRACKER", "HISTORYSERVER"]
    PUT_IN_DISABLED = {
      "HostRoles": {
        "state": "DISABLED"
      }
    }
    mr_mapping = {
      "MAPREDUCE_CLIENT": ["test.host.vm"],
      "JOBTRACKER": ["test1.host.vm"],
      "TASKTRACKER": ["test2.host.vm"],
      "HISTORYSERVER": ["test3.host.vm"]
    }
    expected_curl_exec_args = []
    for key, hosts in mr_mapping.items():
      if key in NON_CLIENTS:
        for host in hosts:
          expected_curl_exec_args.append(
            [
              (COMPONENT_URL_FORMAT % (host, key),),
              {
                "request_type": "PUT",
                "data": PUT_IN_DISABLED,
                "validate": True
              }
            ]
          )

    expected_curl_exec_args.append(
      [
        (SERVICE_URL_FORMAT,),
        {
          "request_type": "DELETE",
          "validate": True
        }
      ]
    )

    get_YN_mock.return_value = True
    read_mapping_mock.return_value = mr_mapping

    # execute testing function
    upgradeHelper.delete_mr()

    self.assertEqual(expected_curl_exec_args, curl_mock.call_args_list)

    pass

  @patch.object(upgradeHelper, "curl")
  def test_get_cluster_stackname(self, curl_mock):
    expected_result = "test version"
    actual_result = ""
    curl_mock.return_value = {
      "Clusters": {
        "version": expected_result
      }
    }

    # execute testing function
    actual_result = upgradeHelper.get_cluster_stackname()

    self.assertEqual(expected_result, actual_result)

  @patch.object(upgradeHelper, "curl")
  def test_has_component_in_stack_def(self, curl_mock):
    curl_mock.side_effect = MagicMock(side_effect=["", upgradeHelper.FatalException(1, "some reason")])

    # execute testing function
    result_ok = upgradeHelper.has_component_in_stack_def("-", "", "")
    result_fail = upgradeHelper.has_component_in_stack_def("-", "", "")

    self.assertEqual(True, result_ok)
    self.assertEqual(False, result_fail)

  @patch.object(upgradeHelper, "get_cluster_stackname")
  @patch.object(upgradeHelper, "has_component_in_stack_def")
  @patch.object(upgradeHelper, "read_mapping")
  @patch.object(upgradeHelper, "curl")
  def test_add_services(self, curl_mock, read_mapping_mock, has_component_mock, get_stack_name_mock):
    host_mapping = {
      "MAPREDUCE_CLIENT": ["test.host.vm"],
      "JOBTRACKER": ["test1.host.vm"],
      "TASKTRACKER": ["test2.host.vm"],
      "HISTORYSERVER": ["test3.host.vm"]
    }
    SERVICE_URL_FORMAT = upgradeHelper.Options.CLUSTER_URL + '/services/{0}'
    COMPONENT_URL_FORMAT = SERVICE_URL_FORMAT + '/components/{1}'
    HOST_COMPONENT_URL_FORMAT = upgradeHelper.Options.CLUSTER_URL + '/hosts/{0}/host_components/{1}'
    service_comp = {
      "YARN": ["NODEMANAGER", "RESOURCEMANAGER", "YARN_CLIENT"],
      "MAPREDUCE2": ["HISTORYSERVER", "MAPREDUCE2_CLIENT"]}
    new_old_host_map = {
      "NODEMANAGER": "TASKTRACKER",
      "HISTORYSERVER": "HISTORYSERVER",
      "RESOURCEMANAGER": "JOBTRACKER",
      "YARN_CLIENT": "MAPREDUCE_CLIENT",
      "MAPREDUCE2_CLIENT": "MAPREDUCE_CLIENT"}
    get_stack_name_mock.return_value = ""
    has_component_mock.return_value = False
    read_mapping_mock.return_value = host_mapping
    expected_curl_args = []

    for service in service_comp.keys():
      expected_curl_args.append([
        (SERVICE_URL_FORMAT.format(service),),
        {
          "validate": True,
          "request_type": "POST"
        }
      ])
      for component in service_comp[service]:
        expected_curl_args.append([
          (COMPONENT_URL_FORMAT.format(service, component),),
          {
            "validate": True,
            "request_type": "POST"
          }
        ])
        for host in host_mapping[new_old_host_map[component]]:
          expected_curl_args.append([
            (HOST_COMPONENT_URL_FORMAT.format(host, component),),
            {
              "validate": True,
              "request_type": "POST"
            }
          ])

    # execute testing function
    upgradeHelper.add_services()

    self.assertEqual(expected_curl_args, curl_mock.call_args_list)

  @patch.object(upgradeHelper, "get_config_resp_all")
  def test_coerce_tag(self, get_config_resp_all_mock):
    test_catalog = """
        {
      "version": "1.0",
      "stacks": [
        {
          "name": "HDP",
          "old-version": "1.0",
          "target-version": "1.1",
          "options": {
            "config-types":{
              "test": {
                "merged-copy": "yes"
              }
            }
          },
          "properties": {
             "test": {
               "test": "host1.com"
            }
          },
          "property-mapping": {
            "test":{
                "map-to": "test-arr",
                "coerce-to": "yaml-array"
           }
          }
        }
      ]
    }
    """
    old_opt = upgradeHelper.Options.OPTIONS
    options = lambda: ""
    options.from_stack = "1.0"
    options.to_stack = "1.1"
    options.upgrade_json = ""

    upgradeHelper.Options.OPTIONS = options
    upgradeHelper.Options.SERVICES = [self.required_service]
    get_config_resp_all_mock.return_value = {
      "test": {
        "properties": {}
      }
    }

    ucf = UpgradeCatalogFactoryMock(test_catalog)
    scf = upgradeHelper.ServerConfigFactory()

    cfg = scf.get_config("test")
    ucfg = ucf.get_catalog("1.0", "1.1")

    cfg.merge(ucfg)
    scf.process_mapping_transformations(ucfg)

    upgradeHelper.Options.OPTIONS = old_opt

    self.assertEqual(True, "test-arr" in cfg.properties)
    self.assertEqual("['host1.com']", cfg.properties["test-arr"])

  @patch.object(upgradeHelper, "get_config_resp_all")
  def test_override_tag(self, get_config_resp_all_mock):
    test_catalog = """
        {
      "version": "1.0",
      "stacks": [
        {
          "name": "HDP",
          "old-version": "1.0",
          "target-version": "1.1",
          "options": {
            "config-types":{
              "test": {
                "merged-copy": "yes"
              }
            }
          },
          "properties": {
             "test": {
               "test_property": {
                  "value": "host1.com",
                  "override": "no"
                }

            }
          },
          "property-mapping": {}
        }
      ]
    }
    """
    old_opt = upgradeHelper.Options.OPTIONS
    options = lambda: ""
    options.from_stack = "1.0"
    options.to_stack = "1.1"
    options.upgrade_json = ""

    upgradeHelper.Options.OPTIONS = options
    upgradeHelper.Options.SERVICES = [self.required_service]
    get_config_resp_all_mock.return_value = {
      "test": {
        "properties": {
          "test_property": "test host"
        }
      }
    }

    ucf = UpgradeCatalogFactoryMock(test_catalog)
    scf = upgradeHelper.ServerConfigFactory()

    cfg = scf.get_config("test")
    ucfg = ucf.get_catalog("1.0", "1.1")

    cfg.merge(ucfg)
    scf.process_mapping_transformations(ucfg)

    upgradeHelper.Options.OPTIONS = old_opt

    self.assertEqual(True, "test_property" in cfg.properties)
    self.assertEqual("test host", cfg.properties["test_property"])

  @patch.object(upgradeHelper, "get_config_resp_all")
  def test_replace_tag(self, get_config_resp_all_mock):
    test_catalog = """
        {
      "version": "1.0",
      "stacks": [
        {
          "name": "HDP",
          "old-version": "1.0",
          "target-version": "1.1",
          "options": {
            "config-types":{
              "test": {
                "merged-copy": "yes"
              }
            }
          },
          "properties": {
             "test": {
               "test": "host1.com"
            }
          },
          "property-mapping": {
            "test":{
                "map-to": "test-arr",
                "replace-from": "com",
                "replace-to": "org"
           }
          }
        }
      ]
    }
    """
    old_opt = upgradeHelper.Options.OPTIONS
    options = lambda: ""
    options.from_stack = "1.0"
    options.to_stack = "1.1"
    options.upgrade_json = ""

    upgradeHelper.Options.OPTIONS = options
    upgradeHelper.Options.SERVICES = [self.required_service]
    get_config_resp_all_mock.return_value = {
      "test": {
        "properties": {}
      }
    }

    ucf = UpgradeCatalogFactoryMock(test_catalog)
    scf = upgradeHelper.ServerConfigFactory()

    cfg = scf.get_config("test")
    ucfg = ucf.get_catalog("1.0", "1.1")

    cfg.merge(ucfg)
    scf.process_mapping_transformations(ucfg)

    upgradeHelper.Options.OPTIONS = old_opt

    self.assertEqual(True, "test-arr" in cfg.properties)
    self.assertEqual("host1.org", cfg.properties["test-arr"])

  @patch.object(upgradeHelper, "curl")
  @patch("time.time")
  def test_update_config(self, time_mock, curl_mock):
    time_pass = 2
    config_type = "test config"
    properties = {
      "test property": "test value"
    }
    attributes = {
      "test attribute": "attribute value"
    }
    expected_tag = "version" + str(int(time_pass * 1000))
    properties_payload = {"Clusters": {"desired_config": {"type": config_type, "tag": expected_tag, "properties": properties}}}
    time_mock.return_value = time_pass

    expected_simple_result = (
      (upgradeHelper.Options.CLUSTER_URL,),
      {
        "request_type": "PUT",
        "data": copy.deepcopy(properties_payload),
        "validate": True,
        "soft_validation": True
      }
    )

    properties_payload["Clusters"]["desired_config"]["properties_attributes"] = attributes
    expected_complex_result = (
      (upgradeHelper.Options.CLUSTER_URL,),
      {
        "request_type": "PUT",
        "data": copy.deepcopy(properties_payload),
        "validate": True,
        "soft_validation": True
      }
    )

    # execute testing function
    upgradeHelper.update_config(properties, config_type)
    simple_result = tuple(curl_mock.call_args)

    upgradeHelper.update_config(properties, config_type, attributes)
    complex_result = tuple(curl_mock.call_args)

    self.assertEqual(expected_simple_result, simple_result)
    self.assertEqual(expected_complex_result, complex_result)

  @patch.object(upgradeHelper, "curl")
  def test_get_zookeeper_quorum(self, curl_mock):
    zoo_def_port = "2181"
    return_curl_data = {
      "host_components": [
                           {
                             "HostRoles": {
                               "host_name": "test.host.vm"
                             }
                           },
                           {
                             "HostRoles": {
                               "host_name": "test.host.vm"
                             }
                           }
      ]
    }

    curl_mock.return_value = copy.deepcopy(return_curl_data)

    # build zookeeper quorum string from return_curl_data and remove trailing comas
    expected_result = reduce(
      lambda x, y: x + "%s:%s," % (y["HostRoles"]["host_name"], zoo_def_port),
      return_curl_data["host_components"],
      ''  # initializer
    ).rstrip(',')

    # execute testing function
    actual_result = upgradeHelper.get_zookeeper_quorum()

    self.assertEqual(expected_result, actual_result)

  @patch.object(upgradeHelper, "curl")
  def test_get_tez_history_url_base(self, curl_mock):
    return_curl_data = {
      'href': 'http://127.0.0.1:8080/api/v1/views/TEZ',
      'ViewInfo': {'view_name': 'TEZ'},
      'versions': [
        {
          'ViewVersionInfo': {
            'view_name': 'TEZ',
            'version': '0.7.0.2.3.0.0-1319'
          },
          'href': 'http://127.0.0.1:8080/api/v1/views/TEZ/versions/0.7.0.2.3.0.0-1319'
        }
      ]
    }

    curl_mock.return_value = copy.deepcopy(return_curl_data)

    # build zookeeper quorum string from return_curl_data and remove trailing comas
    expected_result = "http://127.0.0.1:8080/#/main/views/TEZ/0.7.0.2.3.0.0-1319/TEZ_CLUSTER_INSTANCE"

    # execute testing function
    actual_result = upgradeHelper.get_tez_history_url_base()

    self.assertEqual(expected_result, actual_result)

  @patch.object(upgradeHelper, "curl")
  def test_get_ranger_xaaudit_hdfs_destination_directory(self, curl_mock):
    return_curl_data = {
      "host_components": [
        {
          "HostRoles": {
            "host_name": "test.host.vm"
          }
        }
      ]
    }

    curl_mock.return_value = copy.deepcopy(return_curl_data)

    # build zookeeper quorum string from return_curl_data and remove trailing comas
    expected_result = "hdfs://test.host.vm:8020/ranger/audit"

    # execute testing function
    actual_result = upgradeHelper.get_ranger_xaaudit_hdfs_destination_directory()

    self.assertEqual(expected_result, actual_result)


  @patch.object(upgradeHelper, "curl")
  def test_get_config_resp_all(self, curl_mock):
    cfg_type = "my type"
    cfg_tag = "my tag"
    cfg_properties = {
      "my property": "property value"
    }
    curl_resp = [
      {
        'Clusters': {
          'desired_configs': {
            cfg_type: {
              "tag": cfg_tag
            }
          }
        }
      },
      {
        "items": [
          {
            "type": cfg_type,
            "tag": cfg_tag,
            "properties": cfg_properties
          }
        ]
      }
    ]

    expected_result = {
        cfg_type: {
          "properties": cfg_properties,
          "tag": cfg_tag
        }
      }
    curl_mock.side_effect = MagicMock(side_effect=curl_resp)

    # execute testing function
    actual_result = upgradeHelper.get_config_resp_all()

    self.assertEquals(expected_result, actual_result)
    pass

  @patch.object(upgradeHelper, "get_config_resp_all")
  @patch("os.mkdir")
  @patch("os.path.exists")
  @patch("__builtin__.open")
  def test_backup_configs(self, open_mock, os_path_exists_mock, mkdir_mock, get_config_resp_all_mock):
    data = {
      self.catalog_cfg_type: {
        "properties": {
          "test-property": "value"
        },
        "tag": "version1"
      }
    }
    os_path_exists_mock.return_value = False
    get_config_resp_all_mock.return_value = data
    expected = json.dumps(data[self.catalog_cfg_type]["properties"], indent=4)
    stream = StringIO()
    m = MagicMock()
    m.__enter__.return_value = stream
    open_mock.return_value = m

    # execute testing function
    upgradeHelper.backup_configs(self.catalog_cfg_type)

    self.assertEqual(expected, stream.getvalue())

  @patch.object(upgradeHelper, "curl")
  def test_install_services(self, curl_mock):
    expected_args = (
      (
        ('http://127.0.0.1:8080/api/v1/clusters/test1/services/MAPREDUCE2',),
        {
          'request_type': 'PUT',
          'data': {
            'RequestInfo': {
              'context': 'Install MapReduce2'
            },
            'Body': {
              'ServiceInfo': {
                'state': 'INSTALLED'
              }
            }
          },
          'validate': True
        }
      ),
      (
        ('http://127.0.0.1:8080/api/v1/clusters/test1/services/YARN',),
        {
          'request_type': 'PUT',
          'data': {
            'RequestInfo': {
              'context': 'Install YARN'
            },
            'Body': {
              'ServiceInfo': {
                'state': 'INSTALLED'
              }
            }
          },
          'validate': True
        }
      )
    )

    # execute testing function
    upgradeHelper.install_services()

    self.assertEqual(2, curl_mock.call_count)
    for i in range(0, 1):
      self.assertEqual(expected_args[i], tuple(curl_mock.call_args_list[i]))

  def test_configuration_diff_analyze(self):
    in_data = {
        self.catalog_cfg_type: [
          {
            'catalog_item': {
              'value': 'my value'
            },
            'property': 'my property',
            'actual_value': 'my value',
            'catalog_value': 'my value'
          }
        ]
    }

    expected_result = {
      'my type': {
        'fail': {
          'count': 0,
          'items': []
        },
        'total': {
          'count': 1,
          'items': []
        },
      'skipped': {
        'count': 0,
        'items': []
      },
        'ok': {
          'count': 1,
          'items': [
                    {
                      'catalog_item': {
                        'value': 'my value'
                      },
                      'property': 'my property',
                      'actual_value': 'my value',
                      'catalog_value': 'my value'
                    }
          ]
        }
      }
    }

    # execute testing function
    actual_result = upgradeHelper.configuration_diff_analyze(in_data)

    self.assertEqual(expected_result, actual_result)

  @patch.object(upgradeHelper, "UpgradeCatalogFactory", autospec=True)
  @patch.object(upgradeHelper, "get_config_resp_all")
  @patch.object(upgradeHelper, "configuration_item_diff")
  @patch.object(upgradeHelper, "configuration_diff_analyze")
  @patch("__builtin__.open")
  def test_verify_configuration(self, open_mock, configuration_diff_analyze_mock, configuration_item_diff_mock,
                                get_config_resp_all_mock, upgradecatalogfactory_mock):
    old_opt = upgradeHelper.Options.OPTIONS
    options = lambda: ""
    options.from_stack = self.catalog_from
    options.to_stack = self.catalog_to
    options.upgrade_json = ""

    upgradeHelper.Options.OPTIONS = options
    upgradeHelper.Options.SERVICES = [self.required_service]
    upgradecatalogfactory_mock.return_value = UpgradeCatalogFactoryMock(self.test_catalog)
    get_config_resp_all_mock.return_value = {
      self.catalog_cfg_type: {
        "properties": {}
      }
    }

    # execute testing function
    upgradeHelper.verify_configuration()

    upgradeHelper.Options.OPTIONS = old_opt

    self.assertEqual(1, get_config_resp_all_mock.call_count)
    self.assertEqual(1, configuration_item_diff_mock.call_count)
    self.assertEqual(1, configuration_diff_analyze_mock.call_count)
    self.assertEqual(1, open_mock.call_count)

  def test_report_formatter(self):
    file = StringIO()
    cfg_item = self.catalog_cfg_type
    analyzed_list = {
        'fail': {
          'count': 1,
          'items': [
            {
              'catalog_item': {
                'value': 'my value'
              },
              'property': 'my property',
              'actual_value': 'my value 1',
              'catalog_value': 'my value'
            }
          ]
        },
        'total': {
          'count': 1,
          'items': []
        },
        'skipped': {
          'count': 0,
          'items': []
        },
        'ok': {
          'count': 0,
          'items': []
        }
    }

    expected_output = "Configuration item my type: property \"my property\" is set to \"my value 1\", but should be set to \"my value\"\n"

    # execute testing function
    upgradeHelper.report_formatter(file, cfg_item, analyzed_list)

    self.assertEqual(expected_output, file.getvalue())

  @patch.object(upgradeHelper, "get_config_resp_all")
  def test_conditional_replace(self, get_config_resp_all_mock):
    test_catalog = """
        {
      "version": "1.0",
      "stacks": [
        {
          "name": "HDP",
          "old-version": "1.0",
          "target-version": "1.1",
          "options": {
            "config-types":{
              "test": {
                "merged-copy": "yes"
              }
            }
          },
          "properties": {
             "test": {
               "test": {
                 "value": "10",
                 "value-required": "-1"
               },
               "test2": {
                 "value": "10",
                 "value-required": "-2"
               }
            }
          },
          "property-mapping": {
          }
        }
      ]
    }
    """

    expected_properties = {"test":"10", "test2":"15"}

    old_opt = upgradeHelper.Options.OPTIONS
    options = lambda: ""
    options.from_stack = "1.0"
    options.to_stack = "1.1"
    options.upgrade_json = ""

    upgradeHelper.Options.OPTIONS = options
    upgradeHelper.Options.SERVICES = [self.required_service]
    get_config_resp_all_mock.return_value = {
      "test": {
        "properties": {"test":"-1", "test2":"15"}
      }
    }

    ucf = UpgradeCatalogFactoryMock(test_catalog)
    scf = upgradeHelper.ServerConfigFactory()

    cfg = scf.get_config("test")
    ucfg = ucf.get_catalog("1.0", "1.1")

    cfg.merge(ucfg)
    upgradeHelper.Options.OPTIONS = old_opt

    self.assertEqual(expected_properties, cfg.properties)

if __name__ == "__main__":
  unittest.main()
