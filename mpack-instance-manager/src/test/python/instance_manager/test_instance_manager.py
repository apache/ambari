#!/usr/bin/env python
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
from unittest import TestCase

import instance_manager
import os
import shutil
import json

TMP_ROOT_FOLDER = "/tmp/instance_manager_test"
# set root directory to tmp location
instance_manager.ROOT_FOLDER_PATH = TMP_ROOT_FOLDER

MPACK_NAME = 'hdpcore'
MPACK_NAME2 = 'edw'
MPACK_VERSION_1 = '1.0.0-b1'
MPACK_VERSION_2 = '1.5.0-b1'
INSTANCE_NAME_1 = 'Production'
INSTANCE_NAME_2 = 'eCommerce'
SUBGROUP_NAME = 'default'
CLIENT_MODULE_NAME = 'hdfs-clients'
CLIENT_COMPONENT_NAME = 'hdfs_client'
SERVER_MODULE_NAME = 'hdfs'
SERVER_COMPONENT_NAME = 'hdfs_server'
MODULE_VERSION_MAPPING = {CLIENT_MODULE_NAME: '3.1.0.0-b1', SERVER_MODULE_NAME: '3.1.0.0-b1'}
MODULE_COMPONENT_MAPPING = {CLIENT_MODULE_NAME: CLIENT_COMPONENT_NAME, SERVER_MODULE_NAME: SERVER_COMPONENT_NAME}

MPACK_JSON = {"modules": [
  {"category": "CLIENT", "components": [{"id": CLIENT_COMPONENT_NAME}], "id": CLIENT_MODULE_NAME},
  {"category": "SERVER", "components": [{"id": SERVER_COMPONENT_NAME}], "id": SERVER_MODULE_NAME}]}


class TestInstanceManager(TestCase):
  def setUp(self):
    build_rpm_structure()

  def tearDown(self):
    remove_rpm_structure()

  def test_create_mpack_client_module(self):
    create_mpack_with_defaults(module_name=CLIENT_MODULE_NAME)
    current_link = os.path.join(TMP_ROOT_FOLDER, instance_manager.INSTANCES_FOLDER_NAME, MPACK_NAME,
                                INSTANCE_NAME_1, SUBGROUP_NAME, CLIENT_COMPONENT_NAME,
                                instance_manager.CURRENT_SOFTLINK_NAME)

    self.assertTrue(os.path.exists(current_link))
    self.assertEquals(os.readlink(current_link),
                      os.path.join(TMP_ROOT_FOLDER, instance_manager.MPACKS_FOLDER_NAME, MPACK_NAME,
                                   MPACK_VERSION_1, CLIENT_COMPONENT_NAME))

    self.assertTrue(os.path.exists(os.path.join(TMP_ROOT_FOLDER, instance_manager.INSTANCES_FOLDER_NAME, MPACK_NAME,
                                                instance_manager.DEFAULT_MPACK_INSTANCE_NAME)))

  def test_create_mpack_server_module_with_default_component_instance(self):
    create_mpack_with_defaults()
    current_link = os.path.join(TMP_ROOT_FOLDER, instance_manager.INSTANCES_FOLDER_NAME, MPACK_NAME,
                                INSTANCE_NAME_1, SUBGROUP_NAME, SERVER_MODULE_NAME, SERVER_COMPONENT_NAME,
                                instance_manager.DEFAULT_COMPONENT_INSTANCE_NAME,
                                instance_manager.CURRENT_SOFTLINK_NAME)

    self.assertTrue(os.path.exists(current_link))
    self.assertEquals(os.readlink(current_link),
                      os.path.join(TMP_ROOT_FOLDER, instance_manager.MPACKS_FOLDER_NAME, MPACK_NAME,
                                   MPACK_VERSION_1, SERVER_COMPONENT_NAME))

    self.assertTrue(os.path.exists(os.path.join(TMP_ROOT_FOLDER, instance_manager.INSTANCES_FOLDER_NAME, MPACK_NAME,
                                                instance_manager.DEFAULT_MPACK_INSTANCE_NAME)))

  def test_create_mpack_server_module_with_multiple_component_instances(self):
    create_mpack_with_defaults(components=None, components_map={SERVER_COMPONENT_NAME: ['server1', 'server2']})

    current_link_1 = os.path.join(TMP_ROOT_FOLDER, instance_manager.INSTANCES_FOLDER_NAME, MPACK_NAME,
                                  INSTANCE_NAME_1, SUBGROUP_NAME, SERVER_MODULE_NAME, SERVER_COMPONENT_NAME,
                                  'server1', instance_manager.CURRENT_SOFTLINK_NAME)
    self.assertTrue(os.path.exists(current_link_1))
    self.assertEqual(os.readlink(current_link_1),
                     os.path.join(TMP_ROOT_FOLDER, instance_manager.MPACKS_FOLDER_NAME, MPACK_NAME,
                                  MPACK_VERSION_1, SERVER_COMPONENT_NAME))

    current_link_2 = os.path.join(TMP_ROOT_FOLDER, instance_manager.INSTANCES_FOLDER_NAME, MPACK_NAME,
                                  INSTANCE_NAME_1, SUBGROUP_NAME, SERVER_MODULE_NAME, SERVER_COMPONENT_NAME,
                                  'server2', instance_manager.CURRENT_SOFTLINK_NAME)

    self.assertTrue(os.path.exists(current_link_2))
    self.assertEqual(os.readlink(current_link_2),
                     os.path.join(TMP_ROOT_FOLDER, instance_manager.MPACKS_FOLDER_NAME, MPACK_NAME,
                                  MPACK_VERSION_1, SERVER_COMPONENT_NAME))

  def test_set_version_server_module_asterisk(self):
    create_mpack_with_defaults()

    build_rpm_structure(mpack_version=MPACK_VERSION_2, remove_old_content=False, create_modules=False)

    instance_manager.set_mpack_instance(MPACK_NAME, MPACK_VERSION_2, INSTANCE_NAME_1, SUBGROUP_NAME,
                                        SERVER_MODULE_NAME, '*')

    current_link = os.path.join(TMP_ROOT_FOLDER, instance_manager.INSTANCES_FOLDER_NAME, MPACK_NAME,
                                INSTANCE_NAME_1, SUBGROUP_NAME, SERVER_MODULE_NAME, SERVER_COMPONENT_NAME,
                                instance_manager.DEFAULT_COMPONENT_INSTANCE_NAME,
                                instance_manager.CURRENT_SOFTLINK_NAME)

    self.assertEqual(os.readlink(current_link),
                     os.path.join(TMP_ROOT_FOLDER, instance_manager.MPACKS_FOLDER_NAME, MPACK_NAME,
                                  MPACK_VERSION_2, SERVER_COMPONENT_NAME))

  def test_set_version_client_module_asterisk(self):
    create_mpack_with_defaults(module_name=CLIENT_MODULE_NAME)

    build_rpm_structure(mpack_version=MPACK_VERSION_2, remove_old_content=False, create_modules=False)

    instance_manager.set_mpack_instance(MPACK_NAME, MPACK_VERSION_2, INSTANCE_NAME_1, SUBGROUP_NAME,
                                        CLIENT_MODULE_NAME, '*')

    current_link = os.path.join(TMP_ROOT_FOLDER, instance_manager.INSTANCES_FOLDER_NAME, MPACK_NAME,
                                INSTANCE_NAME_1, SUBGROUP_NAME, CLIENT_COMPONENT_NAME,
                                instance_manager.CURRENT_SOFTLINK_NAME)

    self.assertEqual(os.readlink(current_link),
                     os.path.join(TMP_ROOT_FOLDER, instance_manager.MPACKS_FOLDER_NAME, MPACK_NAME,
                                  MPACK_VERSION_2, CLIENT_COMPONENT_NAME))

  def test_set_version_for_one_of_two_component_instances(self):
    create_mpack_with_defaults(components=None, components_map={SERVER_COMPONENT_NAME: ['server1', 'server2']})

    build_rpm_structure(mpack_version=MPACK_VERSION_2, remove_old_content=False, create_modules=False)

    instance_manager.set_mpack_instance(MPACK_NAME, MPACK_VERSION_2, INSTANCE_NAME_1, SUBGROUP_NAME,
                                        SERVER_MODULE_NAME, None, {SERVER_COMPONENT_NAME: ['server2']})

    current_link_1 = os.path.join(TMP_ROOT_FOLDER, instance_manager.INSTANCES_FOLDER_NAME, MPACK_NAME,
                                  INSTANCE_NAME_1, SUBGROUP_NAME, SERVER_MODULE_NAME, SERVER_COMPONENT_NAME,
                                  'server1', instance_manager.CURRENT_SOFTLINK_NAME)
    self.assertEqual(os.readlink(current_link_1),
                     os.path.join(TMP_ROOT_FOLDER, instance_manager.MPACKS_FOLDER_NAME, MPACK_NAME,
                                  MPACK_VERSION_1, SERVER_COMPONENT_NAME))

    current_link_2 = os.path.join(TMP_ROOT_FOLDER, instance_manager.INSTANCES_FOLDER_NAME, MPACK_NAME,
                                  INSTANCE_NAME_1, SUBGROUP_NAME, SERVER_MODULE_NAME, SERVER_COMPONENT_NAME,
                                  'server2', instance_manager.CURRENT_SOFTLINK_NAME)

    self.assertEqual(os.readlink(current_link_2),
                     os.path.join(TMP_ROOT_FOLDER, instance_manager.MPACKS_FOLDER_NAME, MPACK_NAME,
                                  MPACK_VERSION_2, SERVER_COMPONENT_NAME))

  def test_get_conf_dir_all(self):
    create_mpack_with_defaults(module_name=CLIENT_MODULE_NAME)
    create_mpack_with_defaults(module_name=SERVER_MODULE_NAME, components=None,
                               components_map={SERVER_COMPONENT_NAME: ['server1']})

    conf_dir_json = instance_manager.get_conf_dir()

    expected_json = {
      "mpacks": {
        "hdpcore": {
          "mpack-instances": {
            "Production": {
              "name": "Production",
              "subgroups": {
                "default": {
                  "modules": {
                    "hdfs": {
                      "category": "SERVER",
                      "name": "hdfs",
                      "components": {
                        "hdfs_server": {
                          "component-instances": {
                            "server1": {
                              "config_dir": "/tmp/instance_manager_test/instances/hdpcore/Production/default/hdfs/hdfs_server/server1/conf",
                              "name": "server1"
                            }
                          }
                        }
                      }
                    },
                    "hdfs_client": {
                      "category": "CLIENT",
                      "component_instances": {
                        "default": {
                          "config_dir": "/tmp/instance_manager_test/instances/hdpcore/Production/default/hdfs_client/conf",
                          "name": "default"
                        }
                      },
                      "name": "hdfs_client"
                    }
                  }
                }
              }
            }
          }
        }
      }
    }
    self.assertEqual(conf_dir_json, expected_json)

  def test_list_instances_all(self):
    create_mpack_with_defaults(module_name=CLIENT_MODULE_NAME)
    create_mpack_with_defaults(module_name=SERVER_MODULE_NAME, components=None,
                               components_map={SERVER_COMPONENT_NAME: ['server1']})

    conf_dir_json = instance_manager.list_instances()

    expected_json = {
      "mpacks": {
        "hdpcore": {
          "mpack-instances": {
            "Production": {
              "name": "Production",
              "subgroups": {
                "default": {
                  "modules": {
                    "hdfs": {
                      "category": "SERVER",
                      "name": "hdfs",
                      "components": {
                        "hdfs_server": {
                          "component-instances": {
                            "server1": {
                              "path": "/tmp/instance_manager_test/modules/hdfs/3.1.0.0-b1",
                              "name": "server1"
                            }
                          }
                        }
                      }
                    },
                    "hdfs_client": {
                      "category": "CLIENT",
                      "component_instances": {
                        "default": {
                          "path": "/tmp/instance_manager_test/modules/hdfs-clients/3.1.0.0-b1",
                          "name": "default"
                        }
                      },
                      "name": "hdfs_client"
                    }
                  }
                }
              }
            }
          }
        }
      }
    }
    self.assertEqual(conf_dir_json, expected_json)

  def test_granularity(self):
    create_mpack_with_defaults()

    full_conf_dir_json = instance_manager.get_conf_dir()
    self.assertTrue('mpacks' in full_conf_dir_json)

    mpack_conf_dir_json = instance_manager.get_conf_dir(mpack=MPACK_NAME)
    self.assertTrue('mpack-instances' in mpack_conf_dir_json)

    instance_conf_dir_json = instance_manager.get_conf_dir(mpack=MPACK_NAME, mpack_instance=INSTANCE_NAME_1,
                                                           subgroup_name=None)
    self.assertTrue('subgroups' in instance_conf_dir_json)

    subgroup_conf_dir_json = instance_manager.get_conf_dir(mpack=MPACK_NAME, mpack_instance=INSTANCE_NAME_1,
                                                           subgroup_name=SUBGROUP_NAME)
    self.assertTrue('modules' in subgroup_conf_dir_json)

    module_conf_dir_json = instance_manager.get_conf_dir(mpack=MPACK_NAME, mpack_instance=INSTANCE_NAME_1,
                                                         subgroup_name=SUBGROUP_NAME, module_name=SERVER_MODULE_NAME)
    self.assertTrue('components' in module_conf_dir_json)

    # The mpack level filter not specified
    full_conf_dir_json = instance_manager.get_conf_dir(mpack_instance=INSTANCE_NAME_1, subgroup_name=SUBGROUP_NAME,
                                                       module_name=SERVER_MODULE_NAME)
    self.assertTrue('mpacks' in full_conf_dir_json)

    # The instance level filter not specified
    mpack_conf_dir_json = instance_manager.get_conf_dir(mpack=MPACK_NAME, subgroup_name=SUBGROUP_NAME,
                                                        module_name=SERVER_MODULE_NAME)
    self.assertTrue('mpack-instances' in mpack_conf_dir_json)

  def test_filtering(self):
    create_mpack_with_defaults(module_name=CLIENT_MODULE_NAME)
    create_mpack_with_defaults(module_name=SERVER_MODULE_NAME, components=None,
                               components_map={})

    create_mpack_with_defaults(module_name=CLIENT_MODULE_NAME, mpack_instance=INSTANCE_NAME_2)
    create_mpack_with_defaults(module_name=SERVER_MODULE_NAME, mpack_instance=INSTANCE_NAME_2, components=None,
                               components_map={SERVER_COMPONENT_NAME: ['server1']})

    build_rpm_structure(mpack_name=MPACK_NAME2, remove_old_content=False, create_modules=False)
    create_mpack_with_defaults(mpack_name=MPACK_NAME2, mpack_instance=INSTANCE_NAME_2,
                               module_name=CLIENT_MODULE_NAME)
    create_mpack_with_defaults(mpack_name=MPACK_NAME2, mpack_instance=INSTANCE_NAME_2,
                               module_name=SERVER_MODULE_NAME, components=None,
                               components_map={SERVER_COMPONENT_NAME: ['server2']})

    filter_by_module_json = instance_manager.list_instances(module_name=SERVER_MODULE_NAME)
    self.assertTrue(MPACK_NAME in filter_by_module_json['mpacks'])
    self.assertTrue(MPACK_NAME2 in filter_by_module_json['mpacks'])
    self.assertTrue(INSTANCE_NAME_2 in filter_by_module_json['mpacks'][MPACK_NAME]['mpack-instances'])
    self.assertTrue(INSTANCE_NAME_1 not in filter_by_module_json['mpacks'][MPACK_NAME]['mpack-instances'])

    filter_by_component_instance_name_json = instance_manager.list_instances(
      components_map={SERVER_COMPONENT_NAME: ['server2']})
    expected_filter_result = {'mpacks': {'edw': {'mpack-instances': {'eCommerce': {'name': 'eCommerce', 'subgroups': {
      'default': {'modules': {'hdfs': {'category': 'SERVER', 'name': 'hdfs', 'components': {'hdfs_server': {
        'component-instances': {
          'server2': {'path': '/tmp/instance_manager_test/modules/hdfs/3.1.0.0-b1', 'name': 'server2'}}}}}}}}}}}}}
    self.assertEquals(expected_filter_result, filter_by_component_instance_name_json)

  def test_validation(self):
    try:
      create_mpack_with_defaults(mpack_name=MPACK_NAME2)
      raise AssertionError("The previous call should have thrown exception")
    except ValueError as e:
      self.assertEquals(e.message, "Mpack {0} doesn't exist, please check mpack name.".format(MPACK_NAME2))

    try:
      create_mpack_with_defaults(mpack_version=MPACK_VERSION_2)
      raise AssertionError("The previous call should have thrown exception")
    except ValueError as e:
      self.assertEquals(e.message,
                        "Mpack version {0} doesn't exist for mpack {1}, please check mpack name and version".format(
                          MPACK_VERSION_2, MPACK_NAME))

    try:
      create_mpack_with_defaults(module_name=SERVER_MODULE_NAME + "broken")
      raise AssertionError("The previous call should have thrown exception")
    except ValueError as e:
      self.assertEquals(e.message,
                        "There is no module {0} for mpack {1} with version {2}."
                        " Please check mpack name, version and module name".format(
                          SERVER_MODULE_NAME + "broken", MPACK_NAME, MPACK_VERSION_1))

    try:
      create_mpack_with_defaults(components_map={SERVER_COMPONENT_NAME: "comp1"}, module_name=CLIENT_MODULE_NAME)
      raise AssertionError("The previous call should have thrown exception")
    except ValueError as e:
      self.assertEquals(e.message,
                        "There is no component {0} in module {1} for mpack {2} with version {3}."
                        " Please check mpack name, version, module name and component name".format(
                          SERVER_COMPONENT_NAME, CLIENT_MODULE_NAME, MPACK_NAME, MPACK_VERSION_1))

  def test_creating_existing_component_instance(self):
    create_mpack_with_defaults()
    try:
      create_mpack_with_defaults()
      raise AssertionError("The previous call should have thrown exception")
    except ValueError as e:
      self.assertEquals(e.message,
                        "The instance /tmp/instance_manager_test/instances/hdpcore/Production/default/hdfs/"
                        "hdfs_server/default already exist. To change the version use set-mpack-instance command")

  def test_set_non_existing_instance(self):
    try:
      instance_manager.set_mpack_instance(mpack=MPACK_NAME, mpack_version=MPACK_VERSION_1,
                                          mpack_instance=INSTANCE_NAME_1,
                                          subgroup_name=SUBGROUP_NAME, module_name=SERVER_MODULE_NAME,
                                          components_map={})
      raise AssertionError("The previous call should have thrown exception")
    except ValueError as e:
      self.assertEquals(e.message,
                        "There are no created instances. Use create-mpack-instance command to add them.")

    create_mpack_with_defaults()
    try:
      instance_manager.set_mpack_instance(mpack=MPACK_NAME, mpack_version=MPACK_VERSION_1,
                                          mpack_instance=INSTANCE_NAME_1,
                                          subgroup_name=SUBGROUP_NAME, module_name=SERVER_MODULE_NAME,
                                          components_map={SERVER_COMPONENT_NAME: ["non-existing-instance"]})
      raise AssertionError("The previous call should have thrown exception")
    except ValueError as e:
      self.assertEquals(e.message,
                        "Found no instances for the given filters.")


def create_mpack_with_defaults(mpack_name=MPACK_NAME, mpack_version=MPACK_VERSION_1, mpack_instance=INSTANCE_NAME_1,
                               subgroup_name=SUBGROUP_NAME, module_name=SERVER_MODULE_NAME, components='*',
                               components_map=None):
  instance_manager.create_mpack(mpack_name, mpack_version, mpack_instance,
                                subgroup_name, module_name, components, components_map)


def build_rpm_structure(mpack_name=MPACK_NAME, mpack_version=MPACK_VERSION_1, mpack_json=MPACK_JSON,
                        module_version_mapping=MODULE_VERSION_MAPPING,
                        module_component_mapping=MODULE_COMPONENT_MAPPING,
                        remove_old_content=True,
                        create_modules=True):
  if remove_old_content:
    remove_rpm_structure()

  mpack_path = os.path.join(TMP_ROOT_FOLDER, instance_manager.MPACKS_FOLDER_NAME, mpack_name, mpack_version)
  os.makedirs(mpack_path)
  with open(os.path.join(mpack_path, "mpack.json"), "w") as mpack_json_file:
    json.dump(mpack_json, mpack_json_file)

  modules_path = os.path.join(TMP_ROOT_FOLDER, instance_manager.MODULES_FOLDER_NAME)
  for module_name in module_version_mapping:
    if create_modules:
      os.makedirs(os.path.join(modules_path, module_name, module_version_mapping[module_name], 'bin'))

    os.symlink(os.path.join(modules_path, module_name, module_version_mapping[module_name]),
               os.path.join(mpack_path, module_component_mapping[module_name]))


def remove_rpm_structure():
  if os.path.exists(TMP_ROOT_FOLDER):
    shutil.rmtree(TMP_ROOT_FOLDER)
