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
           "my property": "my value"
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
    self.test_catalog = self.test_catalog % (self.catalog_from, self.catalog_to, self.catalog_cfg_type, self.catalog_cfg_type)

    self.original_curl = upgradeHelper.curl
    upgradeHelper.curl = self.magic_curl

    # mock logging methods
    upgradeHelper.logging.getLogger = MagicMock()
    upgradeHelper.logging.FileHandler = MagicMock()

    self.out = StringIO()
    sys.stdout = self.out

  def magic_curl(self, *args, **kwargs):

    def ret_object():
      return ""

    def communicate():
      return "{}", ""

    ret_object.returncode = 0
    ret_object.communicate = communicate

    with patch("upgradeHelper.subprocess") as subprocess:
      subprocess.Popen.return_value = ret_object
      self.original_curl(*args, **kwargs)

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
      warnings = []
      printonly = False

    args = ["update-configs"]
    modify_action_mock.return_value = MagicMock()
    test_mock = MagicMock()
    test_mock.parse_args = lambda: (options, args)
    option_parser_mock.return_value = test_mock

    upgradeHelper.main()
    self.assertEqual(1, modify_action_mock.call_count)
    self.assertEqual({"user": options.user, "pass": options.password}, upgradeHelper.Options.API_TOKENS)
    self.assertEqual(options.clustername, upgradeHelper.Options.CLUSTER_NAME)

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
  @patch("os.remove")
  def test_write_config(self, remove_mock, isfile_mock, open_mock):
    test_data = {
      "test_field": "test_value"
    }
    test_result = json.dumps(test_data)
    test_cfgtype = 'cfg.me'
    test_tag = 'tag.test'
    test_filename = "%s_%s" % (test_cfgtype, test_tag)
    test_result = json.dumps(test_data)
    output = StringIO()
    isfile_mock.return_value = True
    open_mock.return_value = output

    # execute testing function
    upgradeHelper.write_config(test_data, test_cfgtype, test_tag)

    self.assertEquals(1, isfile_mock.call_count)
    self.assertEquals(1, remove_mock.call_count)
    self.assertEquals(1, open_mock.call_count)

    # check file name
    self.assertEquals(test_filename, isfile_mock.call_args[0][0])
    # check content
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
                "validate": True,
                "validate_expect_body": False
              }
            ]
          )

    expected_curl_exec_args.append(
      [
        (SERVICE_URL_FORMAT,),
        {
          "request_type": "DELETE",
          "validate": True,
          "validate_expect_body": False
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
          "validate_expect_body": False,
          "request_type": "POST"
        }
      ])
      for component in service_comp[service]:
        expected_curl_args.append([
          (COMPONENT_URL_FORMAT.format(service, component),),
          {
            "validate": True,
            "validate_expect_body": False,
            "request_type": "POST"
          }
        ])
        for host in host_mapping[new_old_host_map[component]]:
          expected_curl_args.append([
            (HOST_COMPONENT_URL_FORMAT.format(host, component),),
            {
              "validate": True,
              "validate_expect_body": False,
              "request_type": "POST"
            }
          ])

    # execute testing function
    upgradeHelper.add_services()

    self.assertEqual(expected_curl_args, curl_mock.call_args_list)

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
        "validate_expect_body": True
      }
    )

    properties_payload["Clusters"]["desired_config"]["properties_attributes"] = attributes
    expected_complex_result = (
      (upgradeHelper.Options.CLUSTER_URL,),
      {
        "request_type": "PUT",
        "data": copy.deepcopy(properties_payload),
        "validate": True,
        "validate_expect_body": True
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

  @patch.object(upgradeHelper, "get_config_resp")
  def test_get_config(self, get_config_resp_mock):
    config_type = "test type"
    tag_name = "my tag"
    properties = {
      "my property": "property value"
    }
    property_atributes = {
      "myproperty": "property value"
    }

    in_data = tag_name, {
      "items": [
        {
          "tag": tag_name,
          "type": config_type,
          "properties": properties,
          "properties_attributes": property_atributes
        }
      ]
    }

    get_config_resp_mock.return_value = in_data
    expected_data = (properties, property_atributes)

    # execute testing function
    actual_data = upgradeHelper.get_config(config_type)

    self.assertEqual(expected_data, actual_data)

  def test_parse_config_resp(self):
    cfg_type = "type 1"
    cfg_properties = {
      "my property": "property value"
    }

    in_data = {
      upgradeHelper.CatConst.ITEMS_TAG: [
        {
          upgradeHelper.CatConst.TYPE_TAG: cfg_type,
          upgradeHelper.CatConst.STACK_PROPERTIES: cfg_properties
        }
      ]
    }

    expected_result = [{
     "type": cfg_type,
     "properties": cfg_properties
    }]

    # execute testing function
    actual_result = upgradeHelper.parse_config_resp(in_data)

    self.assertEqual(expected_result, actual_result)

  @patch.object(upgradeHelper, "curl")
  @patch.object(upgradeHelper, "parse_config_resp")
  def test_get_config_resp(self, parse_config_resp_mock, curl_mock):
    cfg_type = "my type"
    cfg_tag = "my tag"
    cfg_data = "test"
    curl_responses = [
      {
        'Clusters': {
          'desired_configs': {
            cfg_type: {
              "tag": cfg_tag
            }
          }
        }
      },
      cfg_data
    ]
    curl_mock.side_effect = MagicMock(side_effect=curl_responses)

    # execute testing function
    actual_tag, actual_data = upgradeHelper.get_config_resp(cfg_type, False, False)

    self.assertEqual((cfg_tag, cfg_data), (actual_tag, actual_data))

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
      cfg_type: cfg_properties
    }
    curl_mock.side_effect = MagicMock(side_effect=curl_resp)

    # execute testing function
    actual_result = upgradeHelper.get_config_resp_all()

    self.assertEquals(expected_result, actual_result)
    pass

  @patch.object(upgradeHelper, "read_mapping")
  @patch.object(upgradeHelper, "get_config")
  @patch.object(upgradeHelper, "update_config_using_existing_properties")
  @patch.object(upgradeHelper, "update_config")
  @patch.object(upgradeHelper, "get_config_resp")
  def test_modify_config_item(self, get_config_resp_mock, upgrade_config_mock, update_config_using_existing_properties_mock,
                              get_config_mock, read_mapping_mock):
    catalog_factory = UpgradeCatalogFactoryMock(self.test_catalog)
    get_config_resp_mock.return_value = "", {}
    catalog = catalog_factory.get_catalog(self.catalog_from, self.catalog_to)
    cfg_type = self.catalog_cfg_type
    read_mapping_mock.return_value = {
      "MAPREDUCE_CLIENT": ["test.host.vm"],
      "JOBTRACKER": ["test1.host.vm"],
      "TASKTRACKER": ["test2.host.vm"],
      "HISTORYSERVER": ["test3.host.vm"]
    }
    get_config_mock.return_value = {"my replace property": "property value 2"}, {}
    expected_params = [
      cfg_type,
      {
        "my property": {
          "value": "my value"
        }
      },
      {
        "my property 2": "property value 2"
      },
    ]

    # execute testing function
    upgradeHelper.modify_config_item(cfg_type, catalog)

    actual_params = [
      update_config_using_existing_properties_mock.call_args[0][0],
      update_config_using_existing_properties_mock.call_args[0][1],
      update_config_using_existing_properties_mock.call_args[0][2]
    ]
    self.assertEquals(update_config_using_existing_properties_mock.call_count, 1)
    self.assertEqual(upgrade_config_mock.call_count, 0)

    self.assertEqual(expected_params, actual_params)

  @patch.object(upgradeHelper, "UpgradeCatalogFactory", autospec=True)
  @patch.object(upgradeHelper, "modify_config_item")
  def test_modify_configs(self, modify_config_item_mock, factory_mock):
    factory_mock.return_value = UpgradeCatalogFactoryMock(self.test_catalog)
    options = lambda: ""
    options.from_stack = self.catalog_from
    options.to_stack = self.catalog_to
    options.upgrade_json = ""

    upgradeHelper.Options.OPTIONS = options

    # execute testing function
    upgradeHelper.modify_configs()

    self.assertEqual(1, modify_config_item_mock.call_count)
    self.assertEqual(self.catalog_cfg_type, modify_config_item_mock.call_args[0][0])

  def test_rename_all_properties(self):
    in_data_properties = {
      "test property": "test value",
      "rename property": "test value 2"
    }
    in_data_mapping = {
      "rename property": "test property 2"
    }
    expect_properties = {
      "test property": "test value",
      "test property 2": "test value 2"
    }

    # execute testing function
    actual_properties = upgradeHelper.rename_all_properties(in_data_properties, in_data_mapping)

    self.assertEqual(expect_properties, actual_properties)

  @patch.object(upgradeHelper, "update_config")
  def test_update_config_using_existing_properties(self, update_config_mock):
    actual_property = {
      "actual property": "actual value"
    }
    actual_attrib = {
      self.catalog_cfg_type: {
        "attribute 1": "attribute value 1"
      }
    }
    catalog_factory = UpgradeCatalogFactoryMock(self.test_catalog)
    catalog = catalog_factory.get_catalog(self.catalog_from, self.catalog_to)

    # execute testing function
    upgradeHelper.update_config_using_existing_properties(self.catalog_cfg_type,
                                                          catalog.get_properties(self.catalog_cfg_type),
                                                          actual_property,
                                                          actual_attrib,
                                                          catalog
                                                          )
    expected_dict = {}
    expected_dict.update(actual_property)
    expected_dict.update(catalog.get_properties_as_dict(catalog.get_properties(self.catalog_cfg_type)))

    expected_args = (
      (expected_dict, self.catalog_cfg_type),
      {
        "attributes": actual_attrib
      }
    )

    self.assertEqual(1, update_config_mock.call_count)
    self.assertEqual(expected_args, tuple(update_config_mock.call_args))

  @patch.object(upgradeHelper, "backup_single_config_type")
  @patch.object(upgradeHelper, "curl")
  def test_backup_configs(self, curl_mock, backup_single_config_type_mock):
    curl_mock.return_value = {
      'Clusters': {
        'desired_configs': {
          self.catalog_cfg_type: {
            "tag": "my tag"
          }
        }
      }
    }

    expected_args = (self.catalog_cfg_type, True)

    # execute testing function
    upgradeHelper.backup_configs(self.catalog_cfg_type)

    self.assertEqual(1, backup_single_config_type_mock.call_count)
    self.assertEqual(expected_args, backup_single_config_type_mock.call_args[0])

  @patch.object(upgradeHelper, "get_config_resp")
  @patch.object(upgradeHelper, "write_config")
  def test_backup_single_config_type(self, write_config_mock, get_config_resp_mock):
    resp = {
      "property": "my data"
    }
    tag = "my tag"
    get_config_resp_mock.return_value = tag, resp
    expected_args = (resp, self.catalog_cfg_type, tag)

    # execute testing function
    upgradeHelper.backup_single_config_type(self.catalog_cfg_type, False)

    self.assertEqual(1, get_config_resp_mock.call_count)
    self.assertEqual(1, write_config_mock.call_count)
    self.assertEqual(expected_args, write_config_mock.call_args[0])

  @patch.object(upgradeHelper, "curl")
  def test_install_services(self, curl_mock):
    expected_args = (
      (
        ('http://127.0.0.1:8080/api/v1/clusters/test1/services/MAPREDUCE2',),
        {
          'validate_expect_body': True,
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
          'validate_expect_body': True,
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

  def test_validate_response(self):
    resp_in_data = [
      ["", False],
      ["", True],
      ["\"href\" : \"", True]
    ]
    resp_expected_results = [
      (0, ""),
      (1, ""),
      (0, "")
    ]

    # execute testing function
    for i in range(0, len(resp_in_data)):
      actual_code, actual_data = upgradeHelper.validate_response(resp_in_data[i][0], resp_in_data[i][1])
      self.assertEqual(resp_expected_results[i], (actual_code, actual_data))

  def test_configuration_item_diff(self):
    factory_mock = UpgradeCatalogFactoryMock(self.test_catalog)
    catalog = factory_mock.get_catalog(self.catalog_from, self.catalog_to)
    actual_properties = {
      self.catalog_cfg_type: {
        "my property": {
         "value": "my value"
        }
      }
    }

    expected_result = [
      {
        'catalog_item': {
          'value': u'my value'
        },
        'property': 'my property',
        'actual_value': {
          'value': 'my value'
        },
        'catalog_value': u'my value'
      }
    ]

    # execute testing function
    actual_result = upgradeHelper.configuration_item_diff(self.catalog_cfg_type, catalog, actual_properties)

    self.assertEqual(expected_result, actual_result)

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
    options = lambda: ""
    options.from_stack = self.catalog_from
    options.to_stack = self.catalog_to
    options.upgrade_json = ""

    upgradeHelper.Options.OPTIONS = options
    upgradecatalogfactory_mock.return_value = UpgradeCatalogFactoryMock(self.test_catalog)

    # execute testing function
    upgradeHelper.verify_configuration()

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

    expected_output = "Configuration item my type: property \"my property\" is set to \"my value 1\", but should be set to \"my value\""

    # execute testing function
    upgradeHelper.report_formatter(file, cfg_item, analyzed_list)

    self.assertEqual(expected_output, file.getvalue())

if __name__ == "__main__":
  unittest.main()
