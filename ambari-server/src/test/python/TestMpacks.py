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
import os
import platform
from mock.mock import patch, MagicMock, call
from unittest import TestCase
from ambari_commons.exceptions import FatalException

os.environ["ROOT"] = ""

from only_for_platform import get_platform, not_for_platform, only_for_platform, os_distro_value, PLATFORM_LINUX, PLATFORM_WINDOWS
from ambari_commons import os_utils

import shutil
project_dir = os.path.join(os.path.abspath(os.path.dirname(__file__)),os.path.normpath("../../../../"))
shutil.copyfile(project_dir+"/ambari-server/conf/unix/ambari.properties", "/tmp/ambari.properties")

# We have to use this import HACK because the filename contains a dash
_search_file = os_utils.search_file
os_utils.search_file = MagicMock(return_value="/tmp/ambari.properties")
with patch.object(platform, "linux_distribution", return_value = MagicMock(return_value=('Redhat', '6.4', 'Final'))):
  with patch("os.path.isdir", return_value = MagicMock(return_value=True)):
    with patch("os.access", return_value = MagicMock(return_value=True)):
      with patch.object(os_utils, "parse_log4j_file", return_value={'ambari.log.dir': '/var/log/ambari-server'}):
        with patch("platform.linux_distribution", return_value = os_distro_value):
          with patch("os.symlink"):
            with patch("glob.glob", return_value = ['/etc/init.d/postgresql-9.3']):
              _ambari_server_ = __import__('ambari-server')
              os_utils.search_file = _search_file
              with patch("__builtin__.open"):
                from ambari_commons.exceptions import FatalException, NonFatalException
                from ambari_server import serverConfiguration
                serverConfiguration.search_file = _search_file

from ambari_server.setupMpacks import install_mpack, upgrade_mpack, replay_mpack_logs, \
  purge_stacks_and_mpacks, STACK_DEFINITIONS_RESOURCE_NAME, SERVICE_DEFINITIONS_RESOURCE_NAME, \
  MPACKS_RESOURCE_NAME

with patch.object(os, "geteuid", new=MagicMock(return_value=0)):
  from resource_management.core import sudo
  reload(sudo)

def get_configs():
  test_directory = os.path.dirname(os.path.abspath(__file__))
  mpacks_directory = os.path.join(test_directory, "mpacks")
  configs = {
    serverConfiguration.STACK_LOCATION_KEY : "/var/lib/ambari-server/resources/stacks",
    serverConfiguration.COMMON_SERVICES_PATH_PROPERTY : "/var/lib/ambari-server/resources/common-services",
    serverConfiguration.MPACKS_STAGING_PATH_PROPERTY : mpacks_directory
  }
  return configs

configs = get_configs()

class TestMpacks(TestCase):

  def test_install_mpack_with_no_mpack_path(self):
    options = self._create_empty_options_mock()
    fail = False
    try:
      install_mpack(options)
    except FatalException as e:
      self.assertEquals("Management pack not specified!", e.reason)
      fail = True
    self.assertTrue(fail)

  @patch("ambari_server.setupMpacks.download_mpack")
  def test_install_mpack_with_invalid_mpack_path(self, download_mpack_mock):
    options = self._create_empty_options_mock()
    options.mpack_path = "/invalid_path/mpack.tar.gz"
    download_mpack_mock.return_value = None

    fail = False
    try:
      install_mpack(options)
    except FatalException as e:
      self.assertEquals("Management pack could not be downloaded!", e.reason)
      fail = True
    self.assertTrue(fail)

  @patch("os.path.exists")
  @patch("ambari_server.setupMpacks.get_ambari_properties")
  def test_purge_stacks_and_mpacks(self, get_ambari_version_mock, os_path_exists_mock):
    options = self._create_empty_options_mock()
    get_ambari_version_mock.return_value = configs
    stacks_directory = configs[serverConfiguration.STACK_LOCATION_KEY]
    common_services_directory = configs[serverConfiguration.COMMON_SERVICES_PATH_PROPERTY]
    mpacks_directory = configs[serverConfiguration.MPACKS_STAGING_PATH_PROPERTY]
    os_path_exists_mock.return_value = False

    purge_stacks_and_mpacks(None)
    os_path_exists_calls = []
    os_path_exists_mock.assert_has_calls(os_path_exists_calls)

    purge_stacks_and_mpacks(options.purge_list.split(","))
    os_path_exists_calls = [
      call(stacks_directory),
      call(mpacks_directory)
    ]
    os_path_exists_mock.assert_has_calls(os_path_exists_calls)

    options.purge_list = ",".join([STACK_DEFINITIONS_RESOURCE_NAME, SERVICE_DEFINITIONS_RESOURCE_NAME, MPACKS_RESOURCE_NAME])
    purge_stacks_and_mpacks(options.purge_list.split(","))
    os_path_exists_calls = [
      call(stacks_directory),
      call(common_services_directory),
      call(mpacks_directory)
    ]
    os_path_exists_mock.assert_has_calls(os_path_exists_calls)

    options.purge_list = ",".join([STACK_DEFINITIONS_RESOURCE_NAME, SERVICE_DEFINITIONS_RESOURCE_NAME, MPACKS_RESOURCE_NAME])
    options.replay_mode = True
    purge_stacks_and_mpacks(options.purge_list.split(","))
    os_path_exists_calls = [
      call(stacks_directory),
      call(common_services_directory)
    ]
    os_path_exists_mock.assert_has_calls(os_path_exists_calls)

  @patch("os.path.exists")
  @patch("ambari_server.setupMpacks.extract_archive")
  @patch("ambari_server.setupMpacks.get_archive_root_dir")
  @patch("ambari_server.setupMpacks.download_mpack")
  def test_install_mpack_with_malformed_mpack(self, download_mpack_mock, get_archive_root_dir_mock, extract_archive_mock, os_path_exists_mock):
    options = self._create_empty_options_mock()
    options.mpack_path = "/path/to/mpack.tar.gz"
    download_mpack_mock.return_value = "/tmp/mpack.tar.gz"
    os_path_exists_mock.return_value = True
    get_archive_root_dir_mock.return_value = None

    fail = False
    try:
      install_mpack(options)
    except FatalException as e:
      self.assertEquals("Malformed management pack. Root directory missing!", e.reason)
      fail = True
    self.assertTrue(fail)

    get_archive_root_dir_mock.return_value = "mpack"
    os_path_exists_mock.side_effect = [True, False, False]
    extract_archive_mock.return_value = None
    fail = False
    try:
      install_mpack(options)
    except FatalException as e:
      self.assertEquals("Malformed management pack. Failed to expand management pack!", e.reason)
      fail = True
    self.assertTrue(fail)

    get_archive_root_dir_mock.return_value = "mpack"
    os_path_exists_mock.side_effect = [True, False, True, False]
    extract_archive_mock.return_value = None
    fail = False
    try:
      install_mpack(options)
    except FatalException as e:
      self.assertEquals("Malformed management pack {0}. Metadata file missing!".format(options.mpack_path), e.reason)
      fail = True
    self.assertTrue(fail)

  @patch("os.path.exists")
  @patch("shutil.move")
  @patch("os.mkdir")
  @patch("ambari_server.setupMpacks.create_symlink")
  @patch("ambari_server.setupMpacks.get_ambari_version")
  @patch("ambari_server.setupMpacks.get_ambari_properties")
  @patch("ambari_server.setupMpacks.add_replay_log")
  @patch("ambari_server.setupMpacks.purge_stacks_and_mpacks")
  @patch("ambari_server.setupMpacks.expand_mpack")
  @patch("ambari_server.setupMpacks.download_mpack")
  def test_install_stack_mpack(self, download_mpack_mock, expand_mpack_mock, purge_stacks_and_mpacks_mock,
                                     add_replay_log_mock, get_ambari_properties_mock, get_ambari_version_mock,
                                     create_symlink_mock, os_mkdir_mock, shutil_move_mock, os_path_exists_mock):
    options = self._create_empty_options_mock()
    options.mpack_path = "/path/to/mystack.tar.gz"
    options.purge = True
    download_mpack_mock.return_value = "/tmp/mystack.tar.gz"
    expand_mpack_mock.return_value = "mpacks/mystack-ambari-mpack-1.0.0.0"
    get_ambari_version_mock.return_value = "2.4.0.0"
    """
    os_path_exists_calls = [call('/tmp/mystack.tar.gz'),
                            call('mpacks/mystack-ambari-mpack-1.0.0.0/mpack.json'),
                            call('/var/lib/ambari-server/resources/stacks'),
                            call('/var/lib/ambari-server/resources/common-services'),
                            call(mpacks_directory),
                            call(mpacks_directory + '/cache'),
                            call(mpacks_directory + '/mystack-ambari-mpack-1.0.0.0'),
                            call('/var/lib/ambari-server/resources/common-services/SERVICEA'),
                            call('/var/lib/ambari-server/resources/common-services/SERVICEB'),
                            call('/var/lib/ambari-server/resources/stacks/MYSTACK'),
                            call('/var/lib/ambari-server/resources/stacks/MYSTACK/1.0'),
                            call('/var/lib/ambari-server/resources/stacks/MYSTACK/1.0/services'),
                            call('/var/lib/ambari-server/resources/stacks/MYSTACK/1.1'),
                            call('/var/lib/ambari-server/resources/stacks/MYSTACK/1.1/services'),
                            call('/var/lib/ambari-server/resources/stacks/MYSTACK/2.0'),
                            call('/var/lib/ambari-server/resources/stacks/MYSTACK/2.0/services')]
   """
    os_path_exists_mock.side_effect = [True, True, False, False, False, False,
                                       False, False, False, False, False, False,
                                       False, False, False, False]
    get_ambari_properties_mock.return_value = configs
    shutil_move_mock.return_value = True

    install_mpack(options)

    stacks_directory = configs[serverConfiguration.STACK_LOCATION_KEY]
    common_services_directory = configs[serverConfiguration.COMMON_SERVICES_PATH_PROPERTY]
    mpacks_directory = configs[serverConfiguration.MPACKS_STAGING_PATH_PROPERTY]
    mpacks_staging_directory = os.path.join(mpacks_directory, "mystack-ambari-mpack-1.0.0.0")

    os_mkdir_calls = [
      call(stacks_directory),
      call(common_services_directory),
      call(mpacks_directory),
      call(mpacks_directory + '/cache'),
      call(os.path.join(common_services_directory, "SERVICEA")),
      call(os.path.join(common_services_directory, "SERVICEB")),
      call(os.path.join(stacks_directory, "MYSTACK")),
      call(os.path.join(stacks_directory, "MYSTACK/1.0")),
      call(os.path.join(stacks_directory, "MYSTACK/1.0/services")),
      call(os.path.join(stacks_directory, "MYSTACK/1.1")),
      call(os.path.join(stacks_directory, "MYSTACK/1.1/services")),
      call(os.path.join(stacks_directory, "MYSTACK/2.0")),
      call(os.path.join(stacks_directory, "MYSTACK/2.0/services"))
    ]
    create_symlink_calls = [
      call(os.path.join(mpacks_staging_directory, "common-services/SERVICEA"),
           os.path.join(common_services_directory, "SERVICEA"),
           "1.0", None),
      call(os.path.join(mpacks_staging_directory, "common-services/SERVICEA"),
           os.path.join(common_services_directory, "SERVICEA"),
           "2.0", None),
      call(os.path.join(mpacks_staging_directory, "common-services/SERVICEB"),
           os.path.join(common_services_directory, "SERVICEB"),
           "1.0.0", None),
      call(os.path.join(mpacks_staging_directory, "common-services/SERVICEB"),
           os.path.join(common_services_directory, "SERVICEB"),
           "2.0.0", None),
      call(os.path.join(mpacks_staging_directory, "stacks/MYSTACK/1.0"),
           os.path.join(stacks_directory, "MYSTACK/1.0"),
           "metainfo.xml", None),
      call(os.path.join(mpacks_staging_directory, "stacks/MYSTACK/1.0/services"),
           os.path.join(stacks_directory, "MYSTACK/1.0/services"),
           "SERVICEA", None),
      call(os.path.join(mpacks_staging_directory, "stacks/MYSTACK/1.1"),
           os.path.join(stacks_directory, "MYSTACK/1.1"),
           "metainfo.xml", None),
      call(os.path.join(mpacks_staging_directory, "stacks/MYSTACK/1.1/services"),
           os.path.join(stacks_directory, "MYSTACK/1.1/services"),
           "SERVICEA", None),
      call(os.path.join(mpacks_staging_directory, "stacks/MYSTACK/2.0"),
           os.path.join(stacks_directory, "MYSTACK/2.0"),
           "metainfo.xml", None),
      call(os.path.join(mpacks_staging_directory, "stacks/MYSTACK/2.0/services"),
           os.path.join(stacks_directory, "MYSTACK/2.0/services"),
           "SERVICEA", None),
      call(os.path.join(mpacks_staging_directory, "stacks/MYSTACK/2.0/services"),
           os.path.join(stacks_directory, "MYSTACK/2.0/services"),
           "SERVICEB", None)
    ]

    self.assertTrue(purge_stacks_and_mpacks_mock.called)
    os_mkdir_mock.assert_has_calls(os_mkdir_calls)
    create_symlink_mock.assert_has_calls(create_symlink_calls)
    self.assertTrue(add_replay_log_mock.called)

  @patch("os.path.exists")
  @patch("os.path.isdir")
  @patch("os.symlink")
  @patch("shutil.move")
  @patch("os.mkdir")
  @patch("ambari_server.setupMpacks.create_symlink")
  @patch("ambari_server.setupMpacks.get_ambari_version")
  @patch("ambari_server.setupMpacks.get_ambari_properties")
  @patch("ambari_server.setupMpacks.add_replay_log")
  @patch("ambari_server.setupMpacks.purge_stacks_and_mpacks")
  @patch("ambari_server.setupMpacks.expand_mpack")
  @patch("ambari_server.setupMpacks.download_mpack")
  def test_install_addon_service_mpack(self, download_mpack_mock, expand_mpack_mock, purge_stacks_and_mpacks_mock,
                                       add_replay_log_mock, get_ambari_properties_mock, get_ambari_version_mock,
                                       create_symlink_mock, os_mkdir_mock, shutil_move_mock,os_symlink_mock,
                                       os_path_isdir_mock, os_path_exists_mock):
    options = self._create_empty_options_mock()
    options.mpack_path = "/path/to/myservice.tar.gz"
    options.purge = False
    download_mpack_mock.return_value = "/tmp/myservice.tar.gz"
    expand_mpack_mock.return_value = "mpacks/myservice-ambari-mpack-1.0.0.0"
    get_ambari_version_mock.return_value = "2.4.0.0"
    """
    os_path_exists_calls = [call('/tmp/myservice.tar.gz'),
                            call('mpacks/myservice-ambari-mpack-1.0.0.0/mpack.json'),
                            call('/var/lib/ambari-server/resources/stacks/MYSTACK/1.0'),
                            call('/var/lib/ambari-server/resources/stacks'),
                            call('/var/lib/ambari-server/resources/common-services'),
                            call(mpacks_directory),
                            call(mpacks_directory + '/cache'),
                            call(mpacks_directory + '/myservice-ambari-mpack-1.0.0.0'),
                            call('/var/lib/ambari-server/resources/common-services/MYSERVICE'),
                            call('/var/lib/ambari-server/resources/stacks/MYSTACK'),
                            call('/var/lib/ambari-server/resources/stacks/MYSTACK/1.0'),
                            call('/var/lib/ambari-server/resources/stacks/MYSTACK/1.0/services'),
                            call('/var/lib/ambari-server/resources/stacks/MYSTACK'),
                            call('/var/lib/ambari-server/resources/stacks/MYSTACK/2.0'),
                            call('/var/lib/ambari-server/resources/stacks/MYSTACK/2.0/services')]
    """
    os_path_exists_mock.side_effect = [True, True, True, True, True,
                                       True, True, False, False, True,
                                       True, True, True, True, True]

    get_ambari_properties_mock.return_value = configs
    shutil_move_mock.return_value = True
    os_path_isdir_mock.return_value = True

    install_mpack(options)

    stacks_directory = configs[serverConfiguration.STACK_LOCATION_KEY]
    common_services_directory = configs[serverConfiguration.COMMON_SERVICES_PATH_PROPERTY]
    mpacks_directory = configs[serverConfiguration.MPACKS_STAGING_PATH_PROPERTY]
    mpacks_staging_directory = os.path.join(mpacks_directory, "myservice-ambari-mpack-1.0.0.0")

    os_mkdir_calls = [
      call(os.path.join(common_services_directory, "MYSERVICE"))
    ]
    os_symlink_calls = [
      call(os.path.join(mpacks_staging_directory, "common-services/MYSERVICE/1.0.0"),
           os.path.join(common_services_directory, "MYSERVICE/1.0.0")),
      call(os.path.join(mpacks_staging_directory, "custom-services/MYSERVICE/1.0.0"),
           os.path.join(stacks_directory, "MYSTACK/1.0/services/MYSERVICE")),
      call(os.path.join(mpacks_staging_directory, "custom-services/MYSERVICE/2.0.0"),
           os.path.join(stacks_directory, "MYSTACK/2.0/services/MYSERVICE"))
    ]

    self.assertFalse(purge_stacks_and_mpacks_mock.called)
    os_mkdir_mock.assert_has_calls(os_mkdir_calls)
    os_symlink_mock.assert_has_calls(os_symlink_calls)
    self.assertTrue(add_replay_log_mock.called)

  @patch("os.path.exists")
  @patch("shutil.move")
  @patch("os.mkdir")
  @patch("ambari_server.setupMpacks.create_symlink")
  @patch("ambari_server.setupMpacks.get_ambari_version")
  @patch("ambari_server.setupMpacks.get_ambari_properties")
  @patch("ambari_server.setupMpacks.add_replay_log")
  @patch("ambari_server.setupMpacks.uninstall_mpack")
  @patch("ambari_server.setupMpacks.purge_stacks_and_mpacks")
  @patch("ambari_server.setupMpacks.expand_mpack")
  @patch("ambari_server.setupMpacks.download_mpack")
  def test_upgrade_stack_mpack(self, download_mpack_mock, expand_mpack_mock, purge_stacks_and_mpacks_mock,
                               uninstall_mpack_mock, add_replay_log_mock, get_ambari_properties_mock,
                               get_ambari_version_mock, create_symlink_mock, os_mkdir_mock, shutil_move_mock,
                               os_path_exists_mock):
    options = self._create_empty_options_mock()
    options.mpack_path = "/path/to/mystack-1.0.0.1.tar.gz"
    download_mpack_mock.return_value = "/tmp/mystack-1.0.0.1.tar.gz"
    expand_mpack_mock.return_value = "mpacks/mystack-ambari-mpack-1.0.0.1"
    get_ambari_version_mock.return_value = "2.4.0.0"
    """
    os_path_exists_calls = [call('/tmp/mystack-1.0.0.1.tar.gz'),
                            call('mpacks/mystack-ambari-mpack-1.0.0.1/mpack.json'),
                            call('/var/lib/ambari-server/resources/stacks'),
                            call('/var/lib/ambari-server/resources/common-services'),
                            call(mpacks_directory),
                            call(mpacks_directory + '/cache'),
                            call(mpacks_directory + '/mystack-ambari-mpack-1.0.0.1'),
                            call('/var/lib/ambari-server/resources/common-services/SERVICEA'),
                            call('/var/lib/ambari-server/resources/common-services/SERVICEB'),
                            call('/var/lib/ambari-server/resources/common-services/SERVICEC'),
                            call('/var/lib/ambari-server/resources/stacks/MYSTACK'),
                            call('/var/lib/ambari-server/resources/stacks/MYSTACK/1.0'),
                            call('/var/lib/ambari-server/resources/stacks/MYSTACK/1.0/services'),
                            call('/var/lib/ambari-server/resources/stacks/MYSTACK/1.1'),
                            call('/var/lib/ambari-server/resources/stacks/MYSTACK/1.1/services'),
                            call('/var/lib/ambari-server/resources/stacks/MYSTACK/2.0'),
                            call('/var/lib/ambari-server/resources/stacks/MYSTACK/2.0/services')]
                            call('/var/lib/ambari-server/resources/stacks/MYSTACK/3.0'),
                            call('/var/lib/ambari-server/resources/stacks/MYSTACK/3.0/services'),
                            call(mpacks_directory),
                            call(mpacks_directory + '/myservice-ambari-mpack-1.0.0.0/mpack.json'),
                            call(mpacks_directory + '/mystack-ambari-mpack-1.0.0.0/mpack.json'),
                            call(mpacks_directory + '/mystack-ambari-mpack-1.0.0.1/mpack.json')]
   """
    os_path_exists_mock.side_effect = [True, True, True, True, True, True,
                                       False, True, True, False, True, True, True,
                                       True, True, True, True, False, False,
                                       True, True, True, True]
    get_ambari_properties_mock.return_value = configs
    shutil_move_mock.return_value = True

    upgrade_mpack(options)

    stacks_directory = configs[serverConfiguration.STACK_LOCATION_KEY]
    common_services_directory = configs[serverConfiguration.COMMON_SERVICES_PATH_PROPERTY]
    mpacks_directory = configs[serverConfiguration.MPACKS_STAGING_PATH_PROPERTY]
    mpacks_staging_directory = os.path.join(mpacks_directory, "mystack-ambari-mpack-1.0.0.1")


    os_mkdir_calls = [
      call(os.path.join(common_services_directory, "SERVICEC")),
      call(os.path.join(stacks_directory, "MYSTACK/3.0")),
      call(os.path.join(stacks_directory, "MYSTACK/3.0/services"))
    ]
    create_symlink_calls = [
      call(os.path.join(mpacks_staging_directory, "common-services/SERVICEA"),
           os.path.join(common_services_directory, "SERVICEA"),
           "1.0", True),
      call(os.path.join(mpacks_staging_directory, "common-services/SERVICEA"),
           os.path.join(common_services_directory, "SERVICEA"),
           "2.0", True),
      call(os.path.join(mpacks_staging_directory, "common-services/SERVICEB"),
           os.path.join(common_services_directory, "SERVICEB"),
           "1.0.0", True),
      call(os.path.join(mpacks_staging_directory, "common-services/SERVICEB"),
           os.path.join(common_services_directory, "SERVICEB"),
           "2.0.0", True),
      call(os.path.join(mpacks_staging_directory, "common-services/SERVICEC"),
           os.path.join(common_services_directory, "SERVICEC"),
           "1.0.0", True),
      call(os.path.join(mpacks_staging_directory, "common-services/SERVICEC"),
           os.path.join(common_services_directory, "SERVICEC"),
           "2.0.0", True),
      call(os.path.join(mpacks_staging_directory, "stacks/MYSTACK/1.0"),
           os.path.join(stacks_directory, "MYSTACK/1.0"),
           "metainfo.xml", True),
      call(os.path.join(mpacks_staging_directory, "stacks/MYSTACK/1.0/services"),
           os.path.join(stacks_directory, "MYSTACK/1.0/services"),
           "SERVICEA", True),
      call(os.path.join(mpacks_staging_directory, "stacks/MYSTACK/1.1"),
           os.path.join(stacks_directory, "MYSTACK/1.1"),
           "metainfo.xml", True),
      call(os.path.join(mpacks_staging_directory, "stacks/MYSTACK/1.1/services"),
           os.path.join(stacks_directory, "MYSTACK/1.1/services"),
           "SERVICEA", True),
      call(os.path.join(mpacks_staging_directory, "stacks/MYSTACK/2.0"),
           os.path.join(stacks_directory, "MYSTACK/2.0"),
           "metainfo.xml", True),
      call(os.path.join(mpacks_staging_directory, "stacks/MYSTACK/2.0/services"),
           os.path.join(stacks_directory, "MYSTACK/2.0/services"),
           "SERVICEA", True),
      call(os.path.join(mpacks_staging_directory, "stacks/MYSTACK/2.0/services"),
           os.path.join(stacks_directory, "MYSTACK/2.0/services"),
           "SERVICEB", True),
      call(os.path.join(mpacks_staging_directory, "stacks/MYSTACK/3.0"),
           os.path.join(stacks_directory, "MYSTACK/3.0"),
           "metainfo.xml", True),
      call(os.path.join(mpacks_staging_directory, "stacks/MYSTACK/3.0/services"),
           os.path.join(stacks_directory, "MYSTACK/3.0/services"),
           "SERVICEA", True),
      call(os.path.join(mpacks_staging_directory, "stacks/MYSTACK/3.0/services"),
           os.path.join(stacks_directory, "MYSTACK/3.0/services"),
           "SERVICEB", True),
      call(os.path.join(mpacks_staging_directory, "stacks/MYSTACK/3.0/services"),
           os.path.join(stacks_directory, "MYSTACK/3.0/services"),
           "SERVICEC", True)
    ]

    self.assertFalse(purge_stacks_and_mpacks_mock.called)
    os_mkdir_mock.assert_has_calls(os_mkdir_calls)
    create_symlink_mock.assert_has_calls(create_symlink_calls)
    uninstall_mpack_mock.assert_has_calls([call("mystack-ambari-mpack", "1.0.0.0")])
    self.assertTrue(add_replay_log_mock.called)


  @patch("os.path.exists")
  @patch("ambari_server.setupMpacks.get_replay_log_file")
  @patch("ambari_server.setupMpacks.upgrade_mpack")
  @patch("ambari_server.setupMpacks.install_mpack")
  def test_replay_mpack_logs(self, install_mpack_mock, upgrade_mpack_mock, get_replay_log_file_mock, os_path_exists_mock):
    test_directory = os.path.dirname(os.path.abspath(__file__))
    resources_directory = os.path.join(test_directory, os.pardir, "resources")
    get_replay_log_file_mock.return_value = os.path.join(resources_directory, "mpacks_replay.log")
    os_path_exists_mock.return_value = True

    replay_mpack_logs()

    install_replay_options = {
      'purge' : True,
      'mpack_command' : 'install-mpack',
      'mpack_path': '/var/lib/ambari-server/resources/mpacks/cache/hdp-1.0.0.0.tar.gz',
      'force': False,
      'verbose': True
    }

    upgrade_replay_options = {
      'purge' : False,
      'mpack_command' : 'upgrade-mpack',
      'mpack_path': '/var/lib/ambari-server/resources/mpacks/cache/hdp-1.0.0.1.tar.gz',
      'force': True,
      'verbose': True
    }

    install_mpack_mock.assert_has_calls([call(install_replay_options, replay_mode=True)])
    upgrade_mpack_mock.assert_has_calls([call(upgrade_replay_options, replay_mode=True)])

  def _create_empty_options_mock(self):
    options = MagicMock()
    options.mpack_path = None
    options.purge = None
    options.purge_list = ",".join([STACK_DEFINITIONS_RESOURCE_NAME, MPACKS_RESOURCE_NAME])
    options.force = None
    options.verbose = None
    options.replay_mode = False
    return options