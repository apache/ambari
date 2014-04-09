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

from unittest import TestCase
from mock.mock import patch


utils = __import__('ambari_server.utils').utils


class TestUtils(TestCase):
  @patch('os.listdir')
  @patch('os.path.isdir')
  def test_get_ubuntu_pg_version(self, path_isdir_mock, os_listdir_mock):
    path_isdir_mock.return_value = True
    os_listdir_mock.return_value = ['8.4', '9.1']

    self.assertEqual('9.1', utils.get_ubuntu_pg_version())

  @patch('ambari_server.utils.get_ubuntu_pg_version')
  def test_get_postgre_hba_dir(self, get_ubuntu_pg_version_mock):
    utils.UBUNTU_PG_HBA_ROOT = '/tmp'
    utils.PG_HBA_ROOT_DEFAULT = '/redhat/postgre/data'
    get_ubuntu_pg_version_mock.return_value = '9.1'

    self.assertEqual('/tmp/9.1/main', utils.get_postgre_hba_dir('ubuntu'))
    self.assertEqual('/redhat/postgre/data', utils.get_postgre_hba_dir('redhat'))

  @patch('ambari_server.utils.get_ubuntu_pg_version')
  def test_get_postgre_running_status(self, get_ubuntu_pg_version_mock):
    utils.PG_STATUS_RUNNING_DEFAULT = "red_running"
    get_ubuntu_pg_version_mock.return_value = '9.1'

    self.assertEqual('9.1/main', utils.get_postgre_running_status('ubuntu'))
    self.assertEqual('red_running', utils.get_postgre_running_status('redhat'))

  @patch('os.path.isfile')
  def test_locate_file(self, isfile_mock):
    utils.ENV_PATH = ['/test']
    # File was found in the path
    isfile_mock.return_value = True
    self.assertEqual('/test/myfile', utils.locate_file('myfile'))
    # File not found in the path
    isfile_mock.return_value = False
    self.assertEqual('myfile', utils.locate_file('myfile'))
    # Testing default vaule
    isfile_mock.return_value = False
    self.assertEqual('/tmp/myfile', utils.locate_file('myfile', '/tmp'))
