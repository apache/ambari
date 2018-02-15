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
MPACK_VERSION_1 = '1.0.0-b1'
MPACK_VERSION_2 = '1.5.0-b1'
INSTANCE_NAME_1 = 'Production'
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

  def test_set_version_asterisk(self):
    create_mpack_with_defaults()

    instance_manager.set_mpack_instance(MPACK_NAME, MPACK_VERSION_2, INSTANCE_NAME_1, SUBGROUP_NAME,
                                        SERVER_MODULE_NAME, '*')

    current_link = os.path.join(TMP_ROOT_FOLDER, instance_manager.INSTANCES_FOLDER_NAME, MPACK_NAME,
                                INSTANCE_NAME_1, SUBGROUP_NAME, SERVER_MODULE_NAME, SERVER_COMPONENT_NAME,
                                instance_manager.DEFAULT_COMPONENT_INSTANCE_NAME,
                                instance_manager.CURRENT_SOFTLINK_NAME)

    self.assertEqual(os.readlink(current_link),
                     os.path.join(TMP_ROOT_FOLDER, instance_manager.MPACKS_FOLDER_NAME, MPACK_NAME,
                                  MPACK_VERSION_2, SERVER_COMPONENT_NAME))

  def test_set_version_for_one_of_two_component_instances(self):
    create_mpack_with_defaults(components=None, components_map={SERVER_COMPONENT_NAME: ['server1', 'server2']})

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


def create_mpack_with_defaults(mpack_name=MPACK_NAME, mpack_version=MPACK_VERSION_1, mpack_instance=INSTANCE_NAME_1,
                               subgroup_name=SUBGROUP_NAME, module_name=SERVER_MODULE_NAME, components='*',
                               components_map=None):
  instance_manager.create_mpack(mpack_name, mpack_version, mpack_instance,
                                subgroup_name, module_name, components, components_map)


def build_rpm_structure():
  remove_rpm_structure()

  mpack_path = os.path.join(TMP_ROOT_FOLDER, instance_manager.MPACKS_FOLDER_NAME, MPACK_NAME, MPACK_VERSION_1)
  os.makedirs(mpack_path)
  with open(os.path.join(mpack_path, "mpack.json"), "w") as mpack_json:
    json.dump(MPACK_JSON, mpack_json)

  modules_path = os.path.join(TMP_ROOT_FOLDER, instance_manager.MODULES_FOLDER_NAME)
  for module_name in MODULE_VERSION_MAPPING:
    os.makedirs(os.path.join(modules_path, module_name, MODULE_VERSION_MAPPING[module_name], 'bin'))
    os.symlink(os.path.join(modules_path, module_name, MODULE_VERSION_MAPPING[module_name]),
               os.path.join(mpack_path, MODULE_COMPONENT_MAPPING[module_name]))


def remove_rpm_structure():
  if os.path.exists(TMP_ROOT_FOLDER):
    shutil.rmtree(TMP_ROOT_FOLDER)
