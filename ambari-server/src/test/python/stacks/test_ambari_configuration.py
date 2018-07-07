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

from unittest import TestCase

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
    self.assertIsNone(ambari_configuration.get_ambari_sso_configuration())

  def testMissingSSOConfiguration(self):
    services_json = {
      "ambari-server-configuration": {
      }
    }

    ambari_configuration = self.ambari_configuration_class(services_json)
    self.assertIsNone(ambari_configuration.get_ambari_sso_configuration())
    self.assertIsNone(ambari_configuration.get_ambari_sso_configuration())

    ambari_sso_details = ambari_configuration.get_ambari_sso_details()
    self.assertIsNotNone(ambari_sso_details)
    self.assertIsNone(ambari_sso_details.get_jwt_audiences())
    self.assertIsNone(ambari_sso_details.get_jwt_cookie_name())
    self.assertIsNone(ambari_sso_details.get_sso_provider_url())
    self.assertIsNone(ambari_sso_details.get_sso_provider_original_parameter_name())
    self.assertFalse(ambari_sso_details.should_enable_sso("AMBARI"))

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

    ambari_sso_details = ambari_configuration.get_ambari_sso_details()
    self.assertIsNotNone(ambari_sso_details)
    self.assertFalse(ambari_sso_details.is_managing_services())
    self.assertFalse(ambari_sso_details.should_enable_sso("AMBARI"))
    self.assertFalse(ambari_sso_details.should_disable_sso("AMBARI"))

    services_json = {
      "ambari-server-configuration": {
        "sso-configuration": {
          "ambari.sso.manage_services": "false",
          "ambari.sso.enabled_services": "AMBARI, RANGER"
        }
      }
    }

    ambari_configuration = self.ambari_configuration_class(services_json)
    self.assertIsNotNone(ambari_configuration.get_ambari_sso_configuration())

    ambari_sso_details = ambari_configuration.get_ambari_sso_details()
    self.assertIsNotNone(ambari_sso_details)
    self.assertFalse(ambari_sso_details.is_managing_services())
    self.assertFalse(ambari_sso_details.should_enable_sso("AMBARI"))
    self.assertFalse(ambari_sso_details.should_disable_sso("AMBARI"))
    self.assertFalse(ambari_sso_details.should_enable_sso("RANGER"))
    self.assertFalse(ambari_sso_details.should_disable_sso("RANGER"))

    services_json = {
      "ambari-server-configuration": {
        "sso-configuration": {
          "ambari.sso.manage_services": "false",
          "ambari.sso.enabled_services": "*"
        }
      }
    }

    ambari_configuration = self.ambari_configuration_class(services_json)
    self.assertIsNotNone(ambari_configuration.get_ambari_sso_configuration())

    ambari_sso_details = ambari_configuration.get_ambari_sso_details()
    self.assertIsNotNone(ambari_sso_details)
    self.assertFalse(ambari_sso_details.is_managing_services())
    self.assertFalse(ambari_sso_details.should_enable_sso("AMBARI"))
    self.assertFalse(ambari_sso_details.should_disable_sso("AMBARI"))
    self.assertFalse(ambari_sso_details.should_enable_sso("RANGER"))
    self.assertFalse(ambari_sso_details.should_disable_sso("RANGER"))

  def testAmbariSSOConfigurationManagingServices(self):
    services_json = {
      "ambari-server-configuration": {
        "sso-configuration": {
          "ambari.sso.manage_services": "true",
          "ambari.sso.enabled_services": "AMBARI"
        }
      }
    }

    ambari_configuration = self.ambari_configuration_class(services_json)
    self.assertIsNotNone(ambari_configuration.get_ambari_sso_configuration())

    ambari_sso_details = ambari_configuration.get_ambari_sso_details()
    self.assertIsNotNone(ambari_sso_details)
    self.assertTrue(ambari_sso_details.is_managing_services())
    self.assertTrue(ambari_sso_details.should_enable_sso("AMBARI"))
    self.assertFalse(ambari_sso_details.should_disable_sso("AMBARI"))
    self.assertFalse(ambari_sso_details.should_enable_sso("RANGER"))
    self.assertTrue(ambari_sso_details.should_disable_sso("RANGER"))

    services_json = {
      "ambari-server-configuration": {
        "sso-configuration": {
          "ambari.sso.manage_services": "true",
          "ambari.sso.enabled_services": "AMBARI, RANGER"
        }
      }
    }

    ambari_configuration = self.ambari_configuration_class(services_json)
    self.assertIsNotNone(ambari_configuration.get_ambari_sso_configuration())

    ambari_sso_details = ambari_configuration.get_ambari_sso_details()
    self.assertIsNotNone(ambari_sso_details)
    self.assertTrue(ambari_sso_details.is_managing_services())
    self.assertTrue(ambari_sso_details.should_enable_sso("AMBARI"))
    self.assertFalse(ambari_sso_details.should_disable_sso("AMBARI"))
    self.assertTrue(ambari_sso_details.should_enable_sso("RANGER"))
    self.assertFalse(ambari_sso_details.should_disable_sso("RANGER"))

    services_json = {
      "ambari-server-configuration": {
        "sso-configuration": {
          "ambari.sso.manage_services": "true",
          "ambari.sso.enabled_services": "*"
        }
      }
    }

    ambari_configuration = self.ambari_configuration_class(services_json)
    self.assertIsNotNone(ambari_configuration.get_ambari_sso_configuration())

    ambari_sso_details = ambari_configuration.get_ambari_sso_details()
    self.assertIsNotNone(ambari_sso_details)
    self.assertTrue(ambari_sso_details.is_managing_services())
    self.assertTrue(ambari_sso_details.should_enable_sso("AMBARI"))
    self.assertFalse(ambari_sso_details.should_disable_sso("AMBARI"))
    self.assertTrue(ambari_sso_details.should_enable_sso("RANGER"))
    self.assertFalse(ambari_sso_details.should_disable_sso("RANGER"))

  def testAmbariJWTProperties(self):
    services_json = {
      "ambari-server-configuration": {
        "sso-configuration": {
          "ambari.sso.provider.certificate": "-----BEGIN CERTIFICATE-----\nMIICVTCCAb6gAwIBAg...2G2Vhj8vTYptEVg==\n-----END CERTIFICATE-----",
          "ambari.sso.authentication.enabled": "true",
          "ambari.sso.provider.url": "https://knox.ambari.apache.org",
          "ambari.sso.jwt.cookieName": "hadoop-jwt",
          "ambari.sso.jwt.audiences": ""
        }
      }
    }

    ambari_configuration = self.ambari_configuration_class(services_json)
    self.assertIsNotNone(ambari_configuration.get_ambari_sso_configuration())

    ambari_sso_details = ambari_configuration.get_ambari_sso_details()
    self.assertIsNotNone(ambari_sso_details)
    self.assertEquals('', ambari_sso_details.get_jwt_audiences())
    self.assertEquals('hadoop-jwt', ambari_sso_details.get_jwt_cookie_name())
    self.assertEquals('https://knox.ambari.apache.org', ambari_sso_details.get_sso_provider_url())
    self.assertEquals('MIICVTCCAb6gAwIBAg...2G2Vhj8vTYptEVg==',
                      ambari_sso_details.get_sso_provider_certificate())

  def testCertWithHeaderAndFooter(self):
    services_json = {
      "ambari-server-configuration": {
        "sso-configuration": {
          "ambari.sso.provider.certificate": '-----BEGIN CERTIFICATE-----\n'
                                             'MIIE3DCCA8SgAwIBAgIJAKfbOMmFyOlNMA0GCSqGSIb3DQEBBQUAMIGkMQswCQYD\n'
                                             '................................................................\n'
                                             'dXRpbmcxFzAVBgNVBAMTDmNsb3VkYnJlYWstcmdsMSUwIwYJKoZIhvcNAQkBFhZy\n'
                                             '-----END CERTIFICATE-----\n'
        }
      }
    }

    ambari_configuration = self.ambari_configuration_class(services_json)
    ambari_sso_details = ambari_configuration.get_ambari_sso_details()

    self.assertEquals('-----BEGIN CERTIFICATE-----\n'
                      'MIIE3DCCA8SgAwIBAgIJAKfbOMmFyOlNMA0GCSqGSIb3DQEBBQUAMIGkMQswCQYD\n'
                      '................................................................\n'
                      'dXRpbmcxFzAVBgNVBAMTDmNsb3VkYnJlYWstcmdsMSUwIwYJKoZIhvcNAQkBFhZy\n'
                      '-----END CERTIFICATE-----',
                      ambari_sso_details.get_sso_provider_certificate(True, False))

    self.assertEquals('-----BEGIN CERTIFICATE-----'
                      'MIIE3DCCA8SgAwIBAgIJAKfbOMmFyOlNMA0GCSqGSIb3DQEBBQUAMIGkMQswCQYD'
                      '................................................................'
                      'dXRpbmcxFzAVBgNVBAMTDmNsb3VkYnJlYWstcmdsMSUwIwYJKoZIhvcNAQkBFhZy'
                      '-----END CERTIFICATE-----',
                      ambari_sso_details.get_sso_provider_certificate(True, True))

    self.assertEquals('MIIE3DCCA8SgAwIBAgIJAKfbOMmFyOlNMA0GCSqGSIb3DQEBBQUAMIGkMQswCQYD\n'
                      '................................................................\n'
                      'dXRpbmcxFzAVBgNVBAMTDmNsb3VkYnJlYWstcmdsMSUwIwYJKoZIhvcNAQkBFhZy',
                      ambari_sso_details.get_sso_provider_certificate(False, False))

    self.assertEquals('MIIE3DCCA8SgAwIBAgIJAKfbOMmFyOlNMA0GCSqGSIb3DQEBBQUAMIGkMQswCQYD'
                      '................................................................'
                      'dXRpbmcxFzAVBgNVBAMTDmNsb3VkYnJlYWstcmdsMSUwIwYJKoZIhvcNAQkBFhZy',
                      ambari_sso_details.get_sso_provider_certificate(False, True))

  def testCertWithoutHeaderAndFooter(self):
    services_json = {
      "ambari-server-configuration": {
        "sso-configuration": {
          "ambari.sso.provider.certificate": 'MIIE3DCCA8SgAwIBAgIJAKfbOMmFyOlNMA0GCSqGSIb3DQEBBQUAMIGkMQswCQYD\n'
                                             '................................................................\n'
                                             'dXRpbmcxFzAVBgNVBAMTDmNsb3VkYnJlYWstcmdsMSUwIwYJKoZIhvcNAQkBFhZy\n',
        }
      }
    }

    ambari_configuration = self.ambari_configuration_class(services_json)
    ambari_sso_details = ambari_configuration.get_ambari_sso_details()

    self.assertEquals('-----BEGIN CERTIFICATE-----\n'
                      'MIIE3DCCA8SgAwIBAgIJAKfbOMmFyOlNMA0GCSqGSIb3DQEBBQUAMIGkMQswCQYD\n'
                      '................................................................\n'
                      'dXRpbmcxFzAVBgNVBAMTDmNsb3VkYnJlYWstcmdsMSUwIwYJKoZIhvcNAQkBFhZy\n'
                      '-----END CERTIFICATE-----',
                      ambari_sso_details.get_sso_provider_certificate(True, False))

    self.assertEquals('-----BEGIN CERTIFICATE-----'
                      'MIIE3DCCA8SgAwIBAgIJAKfbOMmFyOlNMA0GCSqGSIb3DQEBBQUAMIGkMQswCQYD'
                      '................................................................'
                      'dXRpbmcxFzAVBgNVBAMTDmNsb3VkYnJlYWstcmdsMSUwIwYJKoZIhvcNAQkBFhZy'
                      '-----END CERTIFICATE-----',
                      ambari_sso_details.get_sso_provider_certificate(True, True))

    self.assertEquals('MIIE3DCCA8SgAwIBAgIJAKfbOMmFyOlNMA0GCSqGSIb3DQEBBQUAMIGkMQswCQYD\n'
                      '................................................................\n'
                      'dXRpbmcxFzAVBgNVBAMTDmNsb3VkYnJlYWstcmdsMSUwIwYJKoZIhvcNAQkBFhZy',
                      ambari_sso_details.get_sso_provider_certificate(False, False))

    self.assertEquals('MIIE3DCCA8SgAwIBAgIJAKfbOMmFyOlNMA0GCSqGSIb3DQEBBQUAMIGkMQswCQYD'
                      '................................................................'
                      'dXRpbmcxFzAVBgNVBAMTDmNsb3VkYnJlYWstcmdsMSUwIwYJKoZIhvcNAQkBFhZy',
                      ambari_sso_details.get_sso_provider_certificate(False, True))
