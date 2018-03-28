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
import operator
import os
import platform
import sys
import unittest
import StringIO

from mock.mock import patch, MagicMock

from only_for_platform import os_distro_value
from ambari_commons import os_utils

import shutil
project_dir = os.path.join(os.path.abspath(os.path.dirname(__file__)),os.path.normpath("../../../../"))
shutil.copyfile(project_dir+"/ambari-server/conf/unix/ambari.properties", "/tmp/ambari.properties")

# We have to use this import HACK because the filename contains a dash
_search_file = os_utils.search_file

def search_file_proxy(filename, searchpatch, pathsep=os.pathsep):
  global _search_file
  if "ambari.properties" in filename:
    return "/tmp/ambari.properties"
  return _search_file(filename, searchpatch, pathsep)

os_utils.search_file = search_file_proxy

with patch.object(platform, "linux_distribution", return_value = MagicMock(return_value=('Redhat', '6.4', 'Final'))):
  with patch("os.path.isdir", return_value = MagicMock(return_value=True)):
    with patch("os.access", return_value = MagicMock(return_value=True)):
      with patch.object(os_utils, "parse_log4j_file", return_value={'ambari.log.dir': '/var/log/ambari-server'}):
        with patch("platform.linux_distribution", return_value = os_distro_value):
          with patch("os.symlink"):
            with patch("glob.glob", return_value = ['/etc/init.d/postgresql-9.3']):
              _ambari_server_ = __import__('ambari-server')
              with patch("__builtin__.open"):
                from ambari_commons.exceptions import FatalException, NonFatalException
                from ambari_server.properties import Properties
                from ambari_server.setupSso import setup_sso, JWT_AUTH_ENBABLED, JWT_AUTH_PROVIDER_URL, JWT_PUBLIC_KEY, JWT_COOKIE_NAME, JWT_AUDIENCES

class TestSetupSso(unittest.TestCase):

  @patch("ambari_server.setupSso.is_root")
  def test_non_root_user_should_not_be_able_to_setup_sso(self, is_root_mock):
    out = StringIO.StringIO()
    sys.stdout = out

    is_root_mock.return_value = False
    options = self._create_empty_options_mock()

    try:
      setup_sso(options)
      self.fail("Should fail with non-fatal exception")
    except FatalException as e:
      self.assertTrue("ambari-server setup-sso should be run with root-level privileges" in e.reason)
      pass

    sys.stdout = sys.__stdout__
    pass


  @patch("ambari_server.setupSso.is_server_runing")
  @patch("ambari_server.setupSso.is_root")
  def test_sso_setup_should_fail_if_server_is_not_running(self, is_root_mock, is_server_runing_mock):
    out = StringIO.StringIO()
    sys.stdout = out

    is_root_mock.return_value = True
    is_server_runing_mock.return_value = (False, 0)
    options = self._create_empty_options_mock()

    try:
      setup_sso(options)
      self.fail("Should fail with non-fatal exception")
    except FatalException as e:
      self.assertTrue("Ambari Server is not running" in e.reason)
      pass

    sys.stdout = sys.__stdout__
    pass

  @patch("ambari_server.setupSso.get_silent")
  @patch("ambari_server.setupSso.is_server_runing")
  @patch("ambari_server.setupSso.is_root")
  def test_silent_mode_is_not_allowed(self, is_root_mock, is_server_runing_mock, get_silent_mock):
    out = StringIO.StringIO()
    sys.stdout = out

    is_root_mock.return_value = True
    is_server_runing_mock.return_value = (True, 0)
    get_silent_mock.return_value = True
    options = self._create_empty_options_mock()

    try:
      setup_sso(options)
      self.fail("Should fail with fatal exception")
    except NonFatalException as e:
      self.assertTrue("setup-sso is not enabled in silent mode." in e.reason)
      pass

    sys.stdout = sys.__stdout__
    pass



  @patch("ambari_server.setupSso.get_silent")
  @patch("ambari_server.setupSso.is_server_runing")
  @patch("ambari_server.setupSso.is_root")
  def test_invalid_sso_enabled_cli_option_should_result_in_error(self, is_root_mock, is_server_runing_mock, get_silent_mock):
    out = StringIO.StringIO()
    sys.stdout = out

    is_root_mock.return_value = True
    is_server_runing_mock.return_value = (True, 0)
    get_silent_mock.return_value = False
    options = self._create_empty_options_mock()
    options.sso_enabled = 'not_true_or_false'

    try:
      setup_sso(options)
      self.fail("Should fail with fatal exception")
    except FatalException as e:
      self.assertTrue("--sso-enabled should be to either 'true' or 'false'" in e.reason)
      pass

    sys.stdout = sys.__stdout__
    pass



  @patch("ambari_server.setupSso.get_silent")
  @patch("ambari_server.setupSso.is_server_runing")
  @patch("ambari_server.setupSso.is_root")
  def test_missing_sso_provider_url_cli_option_when_enabling_sso_should_result_in_error(self, is_root_mock, is_server_runing_mock, get_silent_mock):
    out = StringIO.StringIO()
    sys.stdout = out

    is_root_mock.return_value = True
    is_server_runing_mock.return_value = (True, 0)
    get_silent_mock.return_value = False
    options = self._create_empty_options_mock()
    options.sso_enabled = 'true'
    options.sso_provider_url = ''

    try:
      setup_sso(options)
      self.fail("Should fail with fatal exception")
    except FatalException as e:
      self.assertTrue("Missing option: --sso-provider-url" in e.reason)
      pass

    sys.stdout = sys.__stdout__
    pass



  @patch("ambari_server.setupSso.get_silent")
  @patch("ambari_server.setupSso.is_server_runing")
  @patch("ambari_server.setupSso.is_root")
  def test_missing_sso_public_cert_file_cli_option_when_enabling_sso_should_result_in_error(self, is_root_mock, is_server_runing_mock, get_silent_mock):
    out = StringIO.StringIO()
    sys.stdout = out

    is_root_mock.return_value = True
    is_server_runing_mock.return_value = (True, 0)
    get_silent_mock.return_value = False
    options = self._create_empty_options_mock()
    options.sso_enabled = 'true'
    options.sso_public_cert_file = ''

    try:
      setup_sso(options)
      self.fail("Should fail with fatal exception")
    except FatalException as e:
      self.assertTrue("Missing option: --sso-public-cert-file" in e.reason)
      pass

    sys.stdout = sys.__stdout__
    pass



  @patch("ambari_server.setupSso.get_silent")
  @patch("ambari_server.setupSso.is_server_runing")
  @patch("ambari_server.setupSso.is_root")
  def test_invalid_sso_provider_url_cli_option_when_enabling_sso_should_result_in_error(self, is_root_mock, is_server_runing_mock, get_silent_mock):
    out = StringIO.StringIO()
    sys.stdout = out

    is_root_mock.return_value = True
    is_server_runing_mock.return_value = (True, 0)
    get_silent_mock.return_value = False
    options = self._create_empty_options_mock()
    options.sso_enabled = 'true'
    options.sso_provider_url = '!invalidHost:invalidPort'

    try:
      setup_sso(options)
      self.fail("Should fail with fatal exception")
    except FatalException as e:
      self.assertTrue("Invalid --sso-provider-url" in e.reason)
      pass

    options.sso_provider_url = 'The SSO provider URL is https://c7402.ambari.apache.org:8443/gateway/knoxsso/api/v1/websso'
    try:
      setup_sso(options)
      self.fail("Should fail with fatal exception")
    except FatalException as e:
      self.assertTrue("Invalid --sso-provider-url" in e.reason)
      pass

    options.sso_provider_url = 'https://c7402.ambari.apache.org:8443/gateway/knoxsso/api/v1/websso is the SSO provider URL'
    try:
      setup_sso(options)
      self.fail("Should fail with fatal exception")
    except FatalException as e:
      self.assertTrue("Invalid --sso-provider-url" in e.reason)
      pass

    sys.stdout = sys.__stdout__
    pass



  @patch("ambari_server.setupSso.perform_changes_via_rest_api")
  @patch("ambari_server.setupSso.update_properties")
  @patch("ambari_server.setupSso.get_ambari_properties")
  @patch("ambari_server.setupSso.get_silent")
  @patch("ambari_server.setupSso.is_server_runing")
  @patch("ambari_server.setupSso.is_root")
  def test_all_cli_options_are_collected_when_enabling_sso(self, is_root_mock, is_server_runing_mock, get_silent_mock, get_ambari_properties_mock, update_properties_mock, perform_changes_via_rest_api_mock):
    out = StringIO.StringIO()
    sys.stdout = out

    is_root_mock.return_value = True
    is_server_runing_mock.return_value = (True, 0)
    get_silent_mock.return_value = False

    properties = Properties();
    get_ambari_properties_mock.return_value = properties

    sso_enabled = 'true'
    sso_enabled_services = 'Ambari, SERVICE1, SERVICE2'
    sso_provider_url = 'https://c7402.ambari.apache.org:8443/gateway/knoxsso/api/v1/websso'
    sso_public_cert_file = '/test/file/path'
    sso_jwt_cookie_name = 'test_cookie'
    sso_jwt_audience_list = 'test, audience, list'
    options = self._create_empty_options_mock()
    options.sso_enabled = sso_enabled
    options.sso_provider_url = sso_provider_url
    options.sso_public_cert_file = sso_public_cert_file
    options.sso_jwt_cookie_name = sso_jwt_cookie_name
    options.sso_jwt_audience_list = sso_jwt_audience_list
    options.sso_enabled_services = sso_enabled_services

    setup_sso(options)

    self.assertTrue(update_properties_mock.called)
    self.assertEqual(properties.get_property(JWT_AUTH_ENBABLED), sso_enabled)
    self.assertEqual(properties.get_property(JWT_AUTH_PROVIDER_URL), sso_provider_url)
    self.assertEqual(properties.get_property(JWT_PUBLIC_KEY), sso_public_cert_file)
    self.assertEqual(properties.get_property(JWT_COOKIE_NAME), sso_jwt_cookie_name)
    self.assertEqual(properties.get_property(JWT_AUDIENCES), sso_jwt_audience_list)
    self.assertTrue(perform_changes_via_rest_api_mock.called)

    sys.stdout = sys.__stdout__
    pass


  @patch("urllib2.urlopen")
  @patch("ambari_server.setupSso.update_properties")
  @patch("ambari_server.setupSso.get_ambari_properties")
  @patch("ambari_server.setupSso.get_silent")
  @patch("ambari_server.setupSso.is_server_runing")
  @patch("ambari_server.setupSso.is_root")
  def test_only_sso_enabled_cli_option_is_collected_when_disabling_sso(self, is_root_mock, is_server_runing_mock, get_silent_mock, get_ambari_properties_mock, update_properties_mock, urlopen_mock):
    out = StringIO.StringIO()
    sys.stdout = out

    is_root_mock.return_value = True
    is_server_runing_mock.return_value = (True, 0)
    get_silent_mock.return_value = False

    properties = Properties();
    get_ambari_properties_mock.return_value = properties

    sso_enabled = 'false'
    sso_provider_url = 'http://testHost:8080'
    sso_public_cert_file = '/test/file/path'
    sso_jwt_cookie_name = 'test_cookie'
    sso_jwt_audience_list = 'test, audience, list'
    options = self._create_empty_options_mock()
    options.sso_enabled = sso_enabled
    options.sso_provider_url = sso_provider_url
    options.sso_public_cert_file = sso_public_cert_file
    options.sso_jwt_cookie_name = sso_jwt_cookie_name
    options.sso_jwt_audience_list = sso_jwt_audience_list

    response = MagicMock()
    response.getcode.return_value = 200
    urlopen_mock.return_value = response

    setup_sso(options)

    self.assertTrue(update_properties_mock.called)
    self.assertEqual(properties.get_property(JWT_AUTH_ENBABLED), sso_enabled)
    self.assertTrue(JWT_AUTH_PROVIDER_URL not in properties.propertyNames())
    self.assertTrue(JWT_PUBLIC_KEY not in properties.propertyNames())
    self.assertTrue(JWT_COOKIE_NAME not in properties.propertyNames())
    self.assertTrue(JWT_AUDIENCES not in properties.propertyNames())

    sys.stdout = sys.__stdout__
    pass


  @patch("ambari_server.setupSso.perform_changes_via_rest_api")
  @patch("ambari_server.setupSso.get_YN_input")
  @patch("ambari_server.setupSso.update_properties")
  @patch("ambari_server.setupSso.get_ambari_properties")
  @patch("ambari_server.setupSso.get_silent")
  @patch("ambari_server.setupSso.is_server_runing")
  @patch("ambari_server.setupSso.is_root")
  def test_sso_is_enabled_for_all_services_via_user_input(self, is_root_mock, is_server_runing_mock, get_silent_mock, get_ambari_properties_mock, update_properties_mock, get_YN_input_mock,
                                                             perform_changes_via_rest_api_mock):
    out = StringIO.StringIO()
    sys.stdout = out

    is_root_mock.return_value = True
    is_server_runing_mock.return_value = (True, 0)
    get_silent_mock.return_value = False
    get_ambari_properties_mock.return_value = Properties()

    def yn_input_side_effect(*args, **kwargs):
      if 'all services' in args[0]:
        return True
      else:
        raise Exception("ShouldNotBeInvoked") # only the 'Use SSO for all services' question should be asked for now

    get_YN_input_mock.side_effect = yn_input_side_effect

    options = self._create_empty_options_mock()
    options.sso_enabled = 'true'
    options.sso_provider_url = 'http://testHost:8080'
    options.sso_public_cert_file = '/test/file/path'
    options.sso_jwt_cookie_name = 'test_cookie'
    options.sso_jwt_audience_list = 'test, audience, list'

    setup_sso(options)

    requestCall = perform_changes_via_rest_api_mock.call_args_list[0]
    args, kwargs = requestCall
    requestData = args[5]
    self.assertTrue(isinstance(requestData, dict))
    ssoProperties = requestData['Configuration']['properties'];
    properties_updated_in_ambari_db = sorted(ssoProperties.iteritems(), key=operator.itemgetter(0))
    properties_should_be_updated_in_ambari_db = sorted({"ambari.sso.enabled_services": "*", "ambari.sso.manage_services": "true"}.iteritems(), key=operator.itemgetter(0))
    self.assertEqual(properties_should_be_updated_in_ambari_db, properties_updated_in_ambari_db)

    sys.stdout = sys.__stdout__
    pass


  @patch("urllib2.urlopen")
  @patch("ambari_server.setupSso.perform_changes_via_rest_api")
  @patch("ambari_server.setupSso.get_cluster_name")
  @patch("ambari_server.setupSso.get_YN_input")
  @patch("ambari_server.setupSso.update_properties")
  @patch("ambari_server.setupSso.get_ambari_properties")
  @patch("ambari_server.setupSso.get_silent")
  @patch("ambari_server.setupSso.is_server_runing")
  @patch("ambari_server.setupSso.is_root")
  def test_sso_enabled_services_are_collected_via_user_input(self, is_root_mock, is_server_runing_mock, get_silent_mock, get_ambari_properties_mock, update_properties_mock, get_YN_input_mock,
                                                             get_cluster_name_mock, perform_changes_via_rest_api_mock, urlopen_mock):
    out = StringIO.StringIO()
    sys.stdout = out

    is_root_mock.return_value = True
    is_server_runing_mock.return_value = (True, 0)
    get_silent_mock.return_value = False
    get_ambari_properties_mock.return_value = Properties()
    get_cluster_name_mock.return_value = 'cluster1'

    def yn_input_side_effect(*args, **kwargs):
      if 'all services' in args[0]:
        return False
      else:
        return True

    get_YN_input_mock.side_effect = yn_input_side_effect

    eligible_services = \
    """
        {
          "href": "http://c7401:8080/api/v1/clusters/cluster1/services?ServiceInfo/sso_integration_supported=true",
          "items": [
            {
                "href": "http://c7401:8080/api/v1/clusters/cluster1/services/HDFS",
                "ServiceInfo": {
                    "cluster_name": "cluster1",
                    "service_name": "HDFS"
                }
            },
            {
                "href": "http://c7401:8080/api/v1/clusters/cluster1/services/ZOOKEPER",
                "ServiceInfo": {
                    "cluster_name": "cluster1",
                    "service_name": "ZOOKEPER"
                }
            }
          ]
        }
    """

    response = MagicMock()
    response.getcode.return_value = 200
    response.read.return_value = eligible_services
    urlopen_mock.return_value = response

    options = self._create_empty_options_mock()
    options.sso_enabled = 'true'
    options.sso_provider_url = 'http://testHost:8080'
    options.sso_public_cert_file = '/test/file/path'
    options.sso_jwt_cookie_name = 'test_cookie'
    options.sso_jwt_audience_list = 'test, audience, list'

    setup_sso(options)

    requestCall = perform_changes_via_rest_api_mock.call_args_list[0]
    args, kwargs = requestCall
    requestData = args[5]
    self.assertTrue(isinstance(requestData, dict))
    ssoProperties = requestData['Configuration']['properties'];
    properties_updated_in_ambari_db = sorted(ssoProperties.iteritems(), key=operator.itemgetter(0))
    properties_should_be_updated_in_ambari_db = sorted({"ambari.sso.enabled_services": "Ambari, HDFS, ZOOKEPER", "ambari.sso.manage_services": "true"}.iteritems(), key=operator.itemgetter(0))
    self.assertEqual(properties_should_be_updated_in_ambari_db, properties_updated_in_ambari_db)

    sys.stdout = sys.__stdout__
    pass

  def _create_empty_options_mock(self):
    options = MagicMock()
    options.sso_enabled = None
    options.sso_enabled_services = None
    options.sso_provider_url = None
    options.sso_public_cert_file = None
    options.sso_jwt_cookie_name = None
    options.sso_jwt_audience_list = None
    return options
    