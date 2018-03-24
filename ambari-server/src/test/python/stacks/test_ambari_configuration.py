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

import os

from mock.mock import MagicMock, patch
from unittest import TestCase

# Mock classes for reading from a file
class MagicFile(object):
  def __init__(self, data):
    self.data = data

  def read(self):
    return self.data

  def __exit__(self, exc_type, exc_val, exc_tb):
    pass

  def __enter__(self):
    return self
pass

class TestAmbariConfiguration(TestCase):

  def setUp(self):
    import imp
    self.test_directory = os.path.dirname(os.path.abspath(__file__))

    relative_path = '../../../main/resources/stacks/ambari_configuration.py'
    ambari_configuration_path = os.path.abspath(os.path.join(self.test_directory, relative_path))
    class_name = 'AmbariConfiguration'

    with open(ambari_configuration_path, 'rb') as fp:
      ambari_configuration_impl = imp.load_module('ambari_configuration', fp,
                                                  ambari_configuration_path,
                                                  ('.py', 'rb', imp.PY_SOURCE))

    self.ambari_configuration_class = getattr(ambari_configuration_impl, class_name)

  def testMissingData(self):
    ambari_configuration = self.ambari_configuration_class('{}')
    self.assertIsNone(ambari_configuration.get_ambari_server_configuration())
    self.assertIsNone(ambari_configuration.get_ambari_server_properties())

  def testMissingSSOConfiguration(self):
    services_json = {
      "ambari-server-configuration": {
      }
    }

    ambari_configuration = self.ambari_configuration_class(services_json)
    self.assertIsNone(ambari_configuration.get_ambari_sso_configuration())
    self.assertIsNone(ambari_configuration.get_ambari_sso_configuration_value("ambari.sso.property"))
    self.assertFalse(ambari_configuration.should_enable_sso("AMBARI"))

  def testMissingAmbariProperties(self):
    services_json = {
      "ambari-server-configuration": {
      }
    }

    ambari_configuration = self.ambari_configuration_class(services_json)
    ambari_sso_details = ambari_configuration.get_ambari_sso_details()
    self.assertFalse(ambari_sso_details.is_jwt_enabled())
    self.assertIsNone(ambari_sso_details.get_jwt_audiences())
    self.assertIsNone(ambari_sso_details.get_jwt_cookie_name())
    self.assertIsNone(ambari_sso_details.get_jwt_provider_url())
    self.assertIsNone(ambari_sso_details.get_jwt_public_key_file())
    self.assertIsNone(ambari_sso_details.get_jwt_public_key())

  def testAmbariSSOConfigurationNotManagingServices(self):
    services_json = {
      "ambari-server-configuration": {
        "sso-configuration": {
          "ambari.sso.enabled_services": "AMBARI"
        }
      }
    }

    ambari_configuration = self.ambari_configuration_class(services_json)
    self.assertIsNotNone(ambari_configuration.get_ambari_sso_configuration())
    self.assertEquals("AMBARI", ambari_configuration.get_ambari_sso_configuration_value("ambari.sso.enabled_services"))
    self.assertFalse(ambari_configuration.should_enable_sso("AMBARI"))
    self.assertFalse(ambari_configuration.should_disable_sso("AMBARI"))

    services_json = {
      "ambari-server-configuration": {
        "sso-configuration": {
          "ambari.sso.manage_services" : "false",
          "ambari.sso.enabled_services" : "AMBARI, RANGER"
        }
      }
    }

    ambari_configuration = self.ambari_configuration_class(services_json)
    self.assertIsNotNone(ambari_configuration.get_ambari_sso_configuration())
    self.assertEquals("AMBARI, RANGER", ambari_configuration.get_ambari_sso_configuration_value("ambari.sso.enabled_services"))
    self.assertFalse(ambari_configuration.should_enable_sso("AMBARI"))
    self.assertFalse(ambari_configuration.should_disable_sso("AMBARI"))
    self.assertFalse(ambari_configuration.should_enable_sso("RANGER"))
    self.assertFalse(ambari_configuration.should_disable_sso("RANGER"))

    services_json = {
      "ambari-server-configuration": {
        "sso-configuration": {
          "ambari.sso.manage_services" : "false",
          "ambari.sso.enabled_services" : "*"
        }
      }
    }

    ambari_configuration = self.ambari_configuration_class(services_json)
    self.assertIsNotNone(ambari_configuration.get_ambari_sso_configuration())
    self.assertEquals("*", ambari_configuration.get_ambari_sso_configuration_value("ambari.sso.enabled_services"))
    self.assertFalse(ambari_configuration.should_enable_sso("AMBARI"))
    self.assertFalse(ambari_configuration.should_disable_sso("AMBARI"))
    self.assertFalse(ambari_configuration.should_enable_sso("RANGER"))
    self.assertFalse(ambari_configuration.should_disable_sso("RANGER"))

  def testAmbariSSOConfigurationManagingServices(self):
    services_json = {
      "ambari-server-configuration": {
        "sso-configuration": {
          "ambari.sso.manage_services" : "true",
          "ambari.sso.enabled_services": "AMBARI"
        }
      }
    }

    ambari_configuration = self.ambari_configuration_class(services_json)
    self.assertIsNotNone(ambari_configuration.get_ambari_sso_configuration())
    self.assertEquals("AMBARI", ambari_configuration.get_ambari_sso_configuration_value("ambari.sso.enabled_services"))
    self.assertTrue(ambari_configuration.should_enable_sso("AMBARI"))
    self.assertFalse(ambari_configuration.should_disable_sso("AMBARI"))
    self.assertFalse(ambari_configuration.should_enable_sso("RANGER"))
    self.assertTrue(ambari_configuration.should_disable_sso("RANGER"))

    services_json = {
      "ambari-server-configuration": {
        "sso-configuration": {
          "ambari.sso.manage_services" : "true",
          "ambari.sso.enabled_services" : "AMBARI, RANGER"
        }
      }
    }

    ambari_configuration = self.ambari_configuration_class(services_json)
    self.assertIsNotNone(ambari_configuration.get_ambari_sso_configuration())
    self.assertEquals("AMBARI, RANGER", ambari_configuration.get_ambari_sso_configuration_value("ambari.sso.enabled_services"))
    self.assertTrue(ambari_configuration.should_enable_sso("AMBARI"))
    self.assertFalse(ambari_configuration.should_disable_sso("AMBARI"))
    self.assertTrue(ambari_configuration.should_enable_sso("RANGER"))
    self.assertFalse(ambari_configuration.should_disable_sso("RANGER"))

    services_json = {
      "ambari-server-configuration": {
        "sso-configuration": {
          "ambari.sso.manage_services" : "true",
          "ambari.sso.enabled_services" : "*"
        }
      }
    }

    ambari_configuration = self.ambari_configuration_class(services_json)
    self.assertIsNotNone(ambari_configuration.get_ambari_sso_configuration())
    self.assertEquals("*", ambari_configuration.get_ambari_sso_configuration_value("ambari.sso.enabled_services"))
    self.assertTrue(ambari_configuration.should_enable_sso("AMBARI"))
    self.assertFalse(ambari_configuration.should_disable_sso("AMBARI"))
    self.assertTrue(ambari_configuration.should_enable_sso("RANGER"))
    self.assertFalse(ambari_configuration.should_disable_sso("RANGER"))

  def testAmbariJWTProperties(self):
    services_json = {
      "ambari-server-properties": {
        "authentication.jwt.publicKey": "/etc/ambari-server/conf/jwt-cert.pem",
        "authentication.jwt.enabled": "true",
        "authentication.jwt.providerUrl": "https://knox.ambari.apache.org",
        "authentication.jwt.cookieName": "hadoop-jwt",
        "authentication.jwt.audiences": ""
      },
      "ambari-server-configuration": {
      }
    }

    ambari_configuration = self.ambari_configuration_class(services_json)
    ambari_sso_details = ambari_configuration.get_ambari_sso_details()
    self.assertTrue(ambari_sso_details.is_jwt_enabled())
    self.assertEquals('', ambari_sso_details.get_jwt_audiences())
    self.assertEquals('hadoop-jwt', ambari_sso_details.get_jwt_cookie_name())
    self.assertEquals('https://knox.ambari.apache.org', ambari_sso_details.get_jwt_provider_url())
    self.assertEquals('/etc/ambari-server/conf/jwt-cert.pem', ambari_sso_details.get_jwt_public_key_file())
    self.assertIsNone(ambari_sso_details.get_jwt_public_key())  # This is none since the file does not exist for unit tests.


  @patch("os.path.isfile", new=MagicMock(return_value=True))
  @patch('__builtin__.open')
  def testReadCertFileWithHeaderAndFooter(self, open_mock):
    mock_file = MagicFile(
      '-----BEGIN CERTIFICATE-----\n'
      'MIIE3DCCA8SgAwIBAgIJAKfbOMmFyOlNMA0GCSqGSIb3DQEBBQUAMIGkMQswCQYD\n'
      '................................................................\n'
      'dXRpbmcxFzAVBgNVBAMTDmNsb3VkYnJlYWstcmdsMSUwIwYJKoZIhvcNAQkBFhZy\n'
      '-----END CERTIFICATE-----\n')
    open_mock.side_effect = [mock_file, mock_file, mock_file, mock_file]

    services_json = {
      "ambari-server-properties": {
        "authentication.jwt.publicKey": "/etc/ambari-server/conf/jwt-cert.pem",
        "authentication.jwt.enabled": "true",
        "authentication.jwt.providerUrl": "https://knox.ambari.apache.org",
        "authentication.jwt.cookieName": "hadoop-jwt",
        "authentication.jwt.audiences": ""
      },
      "ambari-server-configuration": {
      }
    }

    ambari_configuration = self.ambari_configuration_class(services_json)
    ambari_sso_details = ambari_configuration.get_ambari_sso_details()

    self.assertEquals('-----BEGIN CERTIFICATE-----\n'
                      'MIIE3DCCA8SgAwIBAgIJAKfbOMmFyOlNMA0GCSqGSIb3DQEBBQUAMIGkMQswCQYD\n'
                      '................................................................\n'
                      'dXRpbmcxFzAVBgNVBAMTDmNsb3VkYnJlYWstcmdsMSUwIwYJKoZIhvcNAQkBFhZy\n'
                      '-----END CERTIFICATE-----',
                      ambari_sso_details.get_jwt_public_key(True, False))

    self.assertEquals('-----BEGIN CERTIFICATE-----'
                      'MIIE3DCCA8SgAwIBAgIJAKfbOMmFyOlNMA0GCSqGSIb3DQEBBQUAMIGkMQswCQYD'
                      '................................................................'
                      'dXRpbmcxFzAVBgNVBAMTDmNsb3VkYnJlYWstcmdsMSUwIwYJKoZIhvcNAQkBFhZy'
                      '-----END CERTIFICATE-----',
                      ambari_sso_details.get_jwt_public_key(True, True))

    self.assertEquals('MIIE3DCCA8SgAwIBAgIJAKfbOMmFyOlNMA0GCSqGSIb3DQEBBQUAMIGkMQswCQYD\n'
                      '................................................................\n'
                      'dXRpbmcxFzAVBgNVBAMTDmNsb3VkYnJlYWstcmdsMSUwIwYJKoZIhvcNAQkBFhZy',
                      ambari_sso_details.get_jwt_public_key(False, False))

    self.assertEquals('MIIE3DCCA8SgAwIBAgIJAKfbOMmFyOlNMA0GCSqGSIb3DQEBBQUAMIGkMQswCQYD'
                      '................................................................'
                      'dXRpbmcxFzAVBgNVBAMTDmNsb3VkYnJlYWstcmdsMSUwIwYJKoZIhvcNAQkBFhZy',
                      ambari_sso_details.get_jwt_public_key(False, True))

  @patch("os.path.isfile", new=MagicMock(return_value=True))
  @patch('__builtin__.open')
  def testReadCertFileWithoutHeaderAndFooter(self, open_mock):
    mock_file = MagicFile(
      'MIIE3DCCA8SgAwIBAgIJAKfbOMmFyOlNMA0GCSqGSIb3DQEBBQUAMIGkMQswCQYD\n'
      '................................................................\n'
      'dXRpbmcxFzAVBgNVBAMTDmNsb3VkYnJlYWstcmdsMSUwIwYJKoZIhvcNAQkBFhZy\n')
    open_mock.side_effect = [mock_file, mock_file, mock_file, mock_file]

    services_json = {
      "ambari-server-properties": {
        "authentication.jwt.publicKey": "/etc/ambari-server/conf/jwt-cert.pem",
        "authentication.jwt.enabled": "true",
        "authentication.jwt.providerUrl": "https://knox.ambari.apache.org",
        "authentication.jwt.cookieName": "hadoop-jwt",
        "authentication.jwt.audiences": ""
      },
      "ambari-server-configuration": {
      }
    }

    ambari_configuration = self.ambari_configuration_class(services_json)
    ambari_sso_details = ambari_configuration.get_ambari_sso_details()

    self.assertEquals('-----BEGIN CERTIFICATE-----\n'
                      'MIIE3DCCA8SgAwIBAgIJAKfbOMmFyOlNMA0GCSqGSIb3DQEBBQUAMIGkMQswCQYD\n'
                      '................................................................\n'
                      'dXRpbmcxFzAVBgNVBAMTDmNsb3VkYnJlYWstcmdsMSUwIwYJKoZIhvcNAQkBFhZy\n'
                      '-----END CERTIFICATE-----',
                      ambari_sso_details.get_jwt_public_key(True, False))

    self.assertEquals('-----BEGIN CERTIFICATE-----'
                      'MIIE3DCCA8SgAwIBAgIJAKfbOMmFyOlNMA0GCSqGSIb3DQEBBQUAMIGkMQswCQYD'
                      '................................................................'
                      'dXRpbmcxFzAVBgNVBAMTDmNsb3VkYnJlYWstcmdsMSUwIwYJKoZIhvcNAQkBFhZy'
                      '-----END CERTIFICATE-----',
                      ambari_sso_details.get_jwt_public_key(True, True))

    self.assertEquals('MIIE3DCCA8SgAwIBAgIJAKfbOMmFyOlNMA0GCSqGSIb3DQEBBQUAMIGkMQswCQYD\n'
                      '................................................................\n'
                      'dXRpbmcxFzAVBgNVBAMTDmNsb3VkYnJlYWstcmdsMSUwIwYJKoZIhvcNAQkBFhZy',
                      ambari_sso_details.get_jwt_public_key(False, False))

    self.assertEquals('MIIE3DCCA8SgAwIBAgIJAKfbOMmFyOlNMA0GCSqGSIb3DQEBBQUAMIGkMQswCQYD'
                      '................................................................'
                      'dXRpbmcxFzAVBgNVBAMTDmNsb3VkYnJlYWstcmdsMSUwIwYJKoZIhvcNAQkBFhZy',
                      ambari_sso_details.get_jwt_public_key(False, True))

