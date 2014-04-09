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

Ambari Agent

"""
import os
import time
from unittest import TestCase
from mock.mock import patch, MagicMock
from resource_management.core import Environment
from resource_management.core.system import System
from resource_management.libraries import XmlConfig


@patch.object(System, "os_family", new='redhat')
class TestXmlConfigResource(TestCase):
  """
  XmlConfig="resource_management.libraries.providers.xml_config.XmlConfigProvider",
  Testing XmlConfig(XmlConfigProvider) with different 'resource configurations'
  """

  @patch("resource_management.core.providers.system._ensure_metadata")
  @patch("__builtin__.open")
  @patch.object(os.path, "exists")
  @patch.object(os.path, "isdir")
  @patch.object(time, "asctime")
  def test_action_create_empty_xml_config(self,
                                          time_asctime_mock,
                                          os_path_isdir_mock,
                                          os_path_exists_mock,
                                          open_mock,
                                          ensure_mock):
    """
    Tests if 'create' action - creates new non existent xml file and write proper data
    where configurations={}
    """
    os_path_isdir_mock.side_effect = [False, True]
    os_path_exists_mock.return_value = False
    time_asctime_mock.return_value = 'Wed 2014-02'

    result_file = MagicMock()
    open_mock.return_value = result_file

    with Environment('/') as env:
      XmlConfig('file.xml',
                conf_dir='/dir/conf',
                configurations={}
      )

    open_mock.assert_called_with('/dir/conf/file.xml', 'wb')
    result_file.__enter__().write.assert_called_with('<!--Wed 2014-02-->\n    <configuration>\n    \n  </configuration>\n')


  @patch("resource_management.core.providers.system._ensure_metadata")
  @patch("__builtin__.open")
  @patch.object(os.path, "exists")
  @patch.object(os.path, "isdir")
  @patch.object(time, "asctime")
  def test_action_create_simple_xml_config(self,
                                           time_asctime_mock,
                                           os_path_isdir_mock,
                                           os_path_exists_mock,
                                           open_mock,
                                           ensure_mock):
    """
    Tests if 'create' action - creates new non existent xml file and write proper data
    where configurations={"Some conf":"Some value"}
    """
    os_path_isdir_mock.side_effect = [False, True]
    os_path_exists_mock.return_value = False
    time_asctime_mock.return_value = 'Wed 2014-02'

    result_file = MagicMock()
    open_mock.return_value = result_file

    with Environment('/') as env:
      XmlConfig('file.xml',
                conf_dir='/dir/conf',
                configurations={'property1': 'value1'}
      )

    open_mock.assert_called_with('/dir/conf/file.xml', 'wb')
    result_file.__enter__().write.assert_called_with('<!--Wed 2014-02-->\n    <configuration>\n    \n    <property>\n      <name>property1</name>\n      <value>value1</value>\n    </property>\n    \n  </configuration>\n')


  @patch("resource_management.core.providers.system._ensure_metadata")
  @patch("__builtin__.open")
  @patch.object(os.path, "exists")
  @patch.object(os.path, "isdir")
  @patch.object(time, "asctime")
  def test_action_create_xml_config_with_metacharacters(self,
                                                        time_asctime_mock,
                                                        os_path_isdir_mock,
                                                        os_path_exists_mock,
                                                        open_mock,
                                                        ensure_mock):
    """
    Tests if 'create' action - creates new non existent xml file and write proper data
    where configurations={"Some conf":"Some metacharacters"}
    """
    os_path_isdir_mock.side_effect = [False, True]
    os_path_exists_mock.return_value = False
    time_asctime_mock.return_value = 'Wed 2014-02'

    result_file = MagicMock()
    open_mock.return_value = result_file

    with Environment('/') as env:
      XmlConfig('file.xml',
                conf_dir='/dir/conf',
                configurations={"": "",
                                "prop.1": "'.'yyyy-MM-dd-HH",
                                "prop.3": "%d{ISO8601} %5p %c{1}:%L - %m%n",
                                "prop.2": "INFO, openjpa",
                                "prop.4": "${oozie.log.dir}/oozie.log",
                                "prop.empty": "",
                },
      )

    open_mock.assert_called_with('/dir/conf/file.xml', 'wb')
    result_file.__enter__().write.assert_called_with('<!--Wed 2014-02-->\n    <configuration>\n    \n    <property>\n      <name></name>\n      <value></value>\n    </property>\n    \n    <property>\n      <name>prop.empty</name>\n      <value></value>\n    </property>\n    \n    <property>\n      <name>prop.3</name>\n      <value>%d{ISO8601} %5p %c{1}:%L - %m%n</value>\n    </property>\n    \n    <property>\n      <name>prop.2</name>\n      <value>INFO, openjpa</value>\n    </property>\n    \n    <property>\n      <name>prop.1</name>\n      <value>&#39;.&#39;yyyy-MM-dd-HH</value>\n    </property>\n    \n    <property>\n      <name>prop.4</name>\n      <value>${oozie.log.dir}/oozie.log</value>\n    </property>\n    \n  </configuration>\n')
