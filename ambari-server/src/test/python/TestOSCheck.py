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

from ambari_commons import os_check
import json
import os
import tempfile
import sys
from unittest import TestCase
from unittest.mock import patch
from unittest.mock import MagicMock

from only_for_platform import os_distro_value, os_distro_value_linux

from ambari_commons import os_utils

from ambari_commons import OSCheck, OSConst
import os_check_type

import shutil

project_dir = os.path.join(
  os.path.abspath(os.path.dirname(__file__)), os.path.normpath("../../../../")
)
shutil.copyfile(
  project_dir + "/ambari-server/conf/unix/ambari.properties", "/tmp/ambari.properties"
)

_search_file = os_utils.search_file
os_utils.search_file = MagicMock(return_value="/tmp/ambari.properties")
utils = __import__("ambari_server.utils").utils
# We have to use this import HACK because the filename contains a dash
with patch("os.path.isdir", return_value=MagicMock(return_value=True)):
  with patch("os.access", return_value=MagicMock(return_value=True)):
    with patch.object(
      os_utils,
      "parse_log4j_file",
      return_value={"ambari.log.dir": "/var/log/ambari-server"},
    ):
      with patch(
        "ambari_commons.os_check.linux_distribution",
        return_value=os_distro_value_linux,
      ):
        with patch.object(OSCheck, "os_distribution", return_value=os_distro_value):
          with patch.object(os_utils, "is_service_exist", return_value=True):
            with patch.object(utils, "get_postgre_hba_dir"):
              os.environ["ROOT"] = ""
              ambari_server = __import__("ambari-server")


class TestLinuxDistribution(TestCase):
  @patch("ambari_commons.os_check.distro.codename", return_value="Blue Onyx")
  @patch("ambari_commons.os_check.distro.version", return_value="9.5")
  @patch("ambari_commons.os_check.distro.id", return_value="rocky")
  def test_uses_supported_distro_api(self, id_mock, version_mock, codename_mock):
    self.assertEqual(
      os_check.linux_distribution(), ("rocky", "9.5", "Blue Onyx")
    )


class TestOSFamilyData(TestCase):
  def _write_resource(self, content):
    file_descriptor, path = tempfile.mkstemp()
    self.addCleanup(os.unlink, path)
    with os.fdopen(file_descriptor, "w", encoding="utf-8") as stream:
      stream.write(content)
    return path

  def test_loads_valid_json_resource(self):
    path = self._write_resource(
      '{"mapping": {"redhat": {"distro": ["rocky"]}}, "aliases": {}}'
    )

    self.assertEqual(
      os_check._load_os_family_data(path)["mapping"]["redhat"]["distro"],
      ["rocky"],
    )

  def test_rejects_malformed_json_resource(self):
    path = self._write_resource("{'mapping': {}}")

    with self.assertRaises(json.JSONDecodeError):
      os_check._load_os_family_data(path)

  def test_rejects_non_mapping_json_structures(self):
    invalid_resources = ("[]", '{"mapping": []}')

    for content in invalid_resources:
      with self.subTest(content=content):
        path = self._write_resource(content)
        with self.assertRaises(ValueError):
          os_check._load_os_family_data(path)


@patch.object(
  os_check, "linux_distribution", new=MagicMock(return_value=("Redhat", "6.4", "Final"))
)
class TestOSCheck(TestCase):
  @patch.object(OSCheck, "os_distribution")
  @patch("ambari_commons.os_check._is_oracle_linux")
  def test_get_os_type(self, mock_is_oracle_linux, mock_linux_distribution):
    # 1 - Any system
    mock_is_oracle_linux.return_value = False
    mock_linux_distribution.return_value = ("my_os", "2015.09", "")
    result = OSCheck.get_os_type()
    self.assertEqual(result, "my_os")

    # 2 - Negative case
    mock_linux_distribution.return_value = ("", "aaaa", "bbbbb")
    try:
      result = OSCheck.get_os_type()
      self.fail("Should throw exception in OSCheck.get_os_type()")
    except Exception as e:
      # Expected
      self.assertEqual("Cannot detect os type. Exiting...", str(e))
      pass

    # 3 - path exist: '/etc/oracle-release'
    mock_is_oracle_linux.return_value = True
    mock_linux_distribution.return_value = ("some_os", "1234", "")
    result = OSCheck.get_os_type()
    self.assertEqual(result, "oraclelinux")

    # 4 - Common system
    mock_is_oracle_linux.return_value = False
    mock_linux_distribution.return_value = ("CenToS", "4.56", "")
    result = OSCheck.get_os_type()
    self.assertEqual(result, "centos")

    # 5 - Red Hat Enterprise Linux
    mock_is_oracle_linux.return_value = False
    # Red Hat Enterprise Linux Server release 6.5 (Santiago)
    mock_linux_distribution.return_value = (
      "Red Hat Enterprise Linux Server",
      "6.5",
      "Santiago",
    )
    result = OSCheck.get_os_type()
    self.assertEqual(result, "redhat")

    # Red Hat Enterprise Linux Workstation release 6.4 (Santiago)
    mock_linux_distribution.return_value = (
      "Red Hat Enterprise Linux Workstation",
      "6.4",
      "Santiago",
    )
    result = OSCheck.get_os_type()
    self.assertEqual(result, "redhat")

    # Red Hat Enterprise Linux AS release 4 (Nahant Update 3)
    mock_linux_distribution.return_value = (
      "Red Hat Enterprise Linux AS",
      "4",
      "Nahant Update 3",
    )
    result = OSCheck.get_os_type()
    self.assertEqual(result, "redhat")

  @patch.object(OSCheck, "os_distribution")
  @patch("os.path.exists")
  def test_get_os_family(self, mock_exists, mock_linux_distribution):
    # 1 - Any system
    mock_exists.return_value = False
    mock_linux_distribution.return_value = ("MY_os", "5.6.7", "")
    result = OSCheck.get_os_family()
    self.assertEqual(result, "my_os")

    # 2 - Redhat
    mock_exists.return_value = False
    mock_linux_distribution.return_value = ("Centos Linux", "2.4", "")
    result = OSCheck.get_os_family()
    self.assertEqual(result, "redhat")

    # 3 - Ubuntu
    mock_exists.return_value = False
    mock_linux_distribution.return_value = ("Ubuntu", "14.04", "")
    result = OSCheck.get_os_family()
    self.assertEqual(result, "ubuntu")

    # 4 - Suse
    mock_exists.return_value = False
    mock_linux_distribution.return_value = ("suse linux enterprise server", "11.3", "")
    result = OSCheck.get_os_family()
    self.assertEqual(result, "suse")

    mock_exists.return_value = False
    mock_linux_distribution.return_value = ("SLED", "1.2.3.4.5", "")
    result = OSCheck.get_os_family()
    self.assertEqual(result, "suse")

    # 5 - Negative case
    mock_linux_distribution.return_value = ("", "111", "2222")
    try:
      result = OSCheck.get_os_family()
      self.fail("Should throw exception in OSCheck.get_os_family()")
    except Exception as e:
      # Expected
      self.assertEqual("Cannot detect os type. Exiting...", str(e))
      pass

  @patch.object(OSCheck, "os_distribution")
  def test_get_os_version(self, mock_linux_distribution):
    # 1 - Any system
    mock_linux_distribution.return_value = ("some_os", "123.45", "")
    result = OSCheck.get_os_version()
    self.assertEqual(result, "123.45")

    # 2 - Negative case
    mock_linux_distribution.return_value = ("ssss", "", "ddddd")
    try:
      result = OSCheck.get_os_version()
      self.fail("Should throw exception in OSCheck.get_os_version()")
    except Exception as e:
      # Expected
      self.assertEqual("Cannot detect os version. Exiting...", str(e))
      pass

  @patch.object(OSCheck, "os_distribution")
  def test_get_os_major_version(self, mock_linux_distribution):
    # 1
    mock_linux_distribution.return_value = ("abcd_os", "123.45.67", "")
    result = OSCheck.get_os_major_version()
    self.assertEqual(result, "123")

    # 2
    mock_linux_distribution.return_value = ("Suse", "11", "")
    result = OSCheck.get_os_major_version()
    self.assertEqual(result, "11")

  @patch.object(OSCheck, "os_distribution")
  def test_aliases(self, mock_linux_distribution):
    OSConst.OS_TYPE_ALIASES["qwerty_os123"] = "aliased_os5"
    OSConst.OS_FAMILY_COLLECTION.append(
      {"name": "aliased_os_family", "os_list": ["aliased_os"]}
    )

    mock_linux_distribution.return_value = ("qwerty_os", "123.45.67", "")

    self.assertEqual(OSCheck.get_os_type(), "aliased_os")
    self.assertEqual(OSCheck.get_os_major_version(), "5")
    self.assertEqual(OSCheck.get_os_version(), "5.45.67")
    self.assertEqual(OSCheck.get_os_family(), "aliased_os_family")

  @patch.object(OSCheck, "os_distribution")
  def test_get_os_release_name(self, mock_linux_distribution):
    # 1 - Any system
    mock_linux_distribution.return_value = ("", "", "MY_NEW_RELEASE")
    result = OSCheck.get_os_release_name()
    self.assertEqual(result, "my_new_release")

    # 2 - Negative case
    mock_linux_distribution.return_value = ("aaaa", "bbbb", "")
    try:
      result = OSCheck.get_os_release_name()
      self.fail("Should throw exception in OSCheck.get_os_release_name()")
    except Exception as e:
      # Expected
      self.assertEqual("Cannot detect os release name. Exiting...", str(e))
      pass

  @patch.object(OSCheck, "os_distribution")
  def test_os_type_check(self, mock_linux_distribution):
    # 1 - server and agent os compatible
    mock_linux_distribution.return_value = ("aaa", "11", "bb")
    base_args = ["os_check_type.py", "aaa11"]
    sys.argv = list(base_args)

    try:
      os_check_type.main()
    except SystemExit as e:
      # exit_code=0
      self.assertEqual("0", str(e))

    # 2 - server and agent os is not compatible
    mock_linux_distribution.return_value = ("ddd", "33", "bb")
    base_args = ["os_check_type.py", "zzz_x77"]
    sys.argv = list(base_args)

    try:
      os_check_type.main()
      self.fail("Must fail because os's not compatible.")
    except Exception as e:
      self.assertEqual(
        "Local OS is not compatible with cluster primary OS family. Please perform manual bootstrap on this host.",
        str(e),
      )
      pass

  @patch.object(OSCheck, "get_os_family")
  def is_ubuntu_family(self, get_os_family_mock):
    get_os_family_mock.return_value = "ubuntu"
    self.assertEqual(OSCheck.is_ubuntu_family(), True)

    get_os_family_mock.return_value = "troll_os"
    self.assertEqual(OSCheck.is_ubuntu_family(), False)

  @patch.object(OSCheck, "get_os_family")
  def test_is_suse_family(self, get_os_family_mock):
    get_os_family_mock.return_value = "suse"
    self.assertEqual(OSCheck.is_suse_family(), True)

    get_os_family_mock.return_value = "troll_os"
    self.assertEqual(OSCheck.is_suse_family(), False)

  @patch.object(OSCheck, "get_os_family")
  def test_is_redhat_family(self, get_os_family_mock):
    get_os_family_mock.return_value = "redhat"
    self.assertEqual(OSCheck.is_redhat_family(), True)

    get_os_family_mock.return_value = "troll_os"
    self.assertEqual(OSCheck.is_redhat_family(), False)
