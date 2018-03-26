#!/usr/bin/env ambari-python-wrap
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

CERTIFICATE_HEADER = "-----BEGIN CERTIFICATE-----"
CERTIFICATE_FOOTER = "-----END CERTIFICATE-----"


def _get_from_dictionary(dictionary, key):
  """
  Safely returns the value from a dictionary that has the given key.

  if the dictionary is None or does not contain the specified key, None is returned

  :return: a dictionary
  """
  if dictionary and key in dictionary:
    return dictionary[key]
  else:
    return None


class AmbariConfiguration(object):
  """
  AmbariConfiguration is a class the encapsulates the Ambari server configuration data.

  The Ambari server configurations are split into categories, where each category contains 0 or more
  properties. For example, the 'ldap-configuration' category contains the
  "ambari.ldap.authentication.enabled"
  property.

  ...
  "ambari-server-configuration" : {
    ...
    "ldap-configuration" : {
      ...
      "ambari.ldap.authentication.enabled" : "true"
      ...
    },
    ...
    "sso-configuration" : {
      ...
      "ambari.sso.enabled_services" : "ATLAS, AMBARI"
      ...
    },
    ...
  }
  ...
  """

  def __init__(self, services):
    self.services = services

  def get_ambari_server_configuration(self):
    """
    Safely returns the "ambari-server-configurations" dictionary from the services dictionary.

    if the services dictionary is None or does not contain "ambari-server-configuration",
    None is returned

    :return: a dictionary
    """
    return _get_from_dictionary(self.services, "ambari-server-configuration")

  def get_ambari_server_properties(self):
    """
    Safely returns the "ambari-server-properties" dictionary from the services dictionary.

    if the services dictionary is None or does not contain "ambari-server-configuration",
    None is returned

    :return: a dictionary
    """
    return _get_from_dictionary(self.services, "ambari-server-properties")

  def get_ambari_server_configuration_category(self, category):
    """
    Safely returns a dictionary of the properties for the requested category from the
    "ambari-server-configurations" dictionary.

    If the ambari-server-configurations dictionary is None or does not contain the
    request category name, None is returned

    :param category: the name of a category
    :return: a dictionary
    """
    return _get_from_dictionary(self.get_ambari_server_configuration(), category)

  def get_category_property_value(self, properties, property_name):
    """
    Safely gets the value of a property from a supplied dictionary of properties.

    :param properties: a dictionary of properties
    :param property_name: the name of a property to retrieve the value for
    :return: a value or None, if the property does not exist
    """
    return _get_from_dictionary(properties, property_name)

  def get_ambari_sso_configuration(self):
    """
    Safely gets a dictionary of properties for the "sso-configuration" category.

    :return: a dictionary or None, if "sso-configuration" is not available
    """
    return self.get_ambari_server_configuration_category("sso-configuration")

  def get_ambari_sso_configuration_value(self, property_name):
    """
    Safely gets a value for a "sso-configuration" property

    :param property_name: the name of a property to retrieve the value for
    :return: a value or None, if the property does not exist
    """
    return self.get_category_property_value(self.get_ambari_sso_configuration(), property_name)

  def get_services_to_enable(self):
    """
    Safely gets the list of services that Ambari should enabled for SSO.

    The returned value is a list of the relevant service names converted to lowercase.

    :return: a list of service names converted to lowercase
    """
    sso_enabled_services = self.get_ambari_sso_configuration_value("ambari.sso.enabled_services")

    return [x.strip().lower() for x in sso_enabled_services.strip().split(",")] \
      if sso_enabled_services \
      else []

  def should_enable_sso(self, service_name):
    """
    Tests the configuration data to determine if the specified service should be configured by
    Ambari to enable SSO integration.

    The relevant property is "sso-configuration/ambari.sso.enabled_services", which is expected
    to be a comma-delimited list of services to be enabled.

    :param service_name: the name of the service to test
    :return: True, if SSO should be enabled; False, otherwise
    """
    if "true" == self.get_ambari_sso_configuration_value("ambari.sso.manage_services"):
      services_to_enable = self.get_services_to_enable()
      return "*" in services_to_enable or service_name.lower() in services_to_enable
    else:
      return False

  def should_disable_sso(self, service_name):
    """
    Tests the configuration data to determine if the specified service should be configured by
    Ambari to disable SSO integration.

    The relevant property is "sso-configuration/ambari.sso.enabled_services", which is expected
    to be a comma-delimited list of services to be enabled.

    :param service_name: the name of the service to test
    :return: true, if SSO should be disabled; false, otherwise
    """
    if "true" == self.get_ambari_sso_configuration_value("ambari.sso.manage_services"):
      services_to_enable = self.get_services_to_enable()
      return "*" not in services_to_enable and service_name.lower() not in services_to_enable
    else:
      return False

  def get_ambari_sso_details(self):
    """
    Gets a dictionary of properties that may be used to configure a service for SSO integration.
    :return: a dictionary
    """
    return AmbariSSODetails(self.get_ambari_server_properties())


class AmbariSSODetails(object):
  """
  AmbariSSODetails encapsulates the SSO configiuration data specified in the ambari-server-properties
  """

  def __init__(self, ambari_server_properties):
    self.jwt_enabled = _get_from_dictionary(ambari_server_properties,
                                            'authentication.jwt.enabled')
    self.jwt_audiences = _get_from_dictionary(ambari_server_properties,
                                              'authentication.jwt.audiences')
    self.jwt_cookie_name = _get_from_dictionary(ambari_server_properties,
                                                'authentication.jwt.cookieName')
    self.jwt_provider_url = _get_from_dictionary(ambari_server_properties,
                                                 'authentication.jwt.providerUrl')
    self.jwt_public_key_file = _get_from_dictionary(ambari_server_properties,
                                                    'authentication.jwt.publicKey')

  def is_jwt_enabled(self):
    """
    Test is SSO/JWT authentication is enabled for Ambari
    :return: True if JWT authentication is enabled for Ambari; False, otherwise
    """
    return "true" == self.jwt_enabled

  def get_jwt_audiences(self):
    """
    Gets the configured JWT audiences list
    :return the configured JWT audiences list:
    """
    return self.jwt_audiences

  def get_jwt_cookie_name(self):
    """
    Gets the configured JWT cookie name
    :return: the configured JWT cookie name
    """
    return self.jwt_cookie_name

  def get_jwt_provider_url(self):
    """
    Gets the configured SSO provider URL
    :return: the configured SSO provider URL
    """
    return self.jwt_provider_url

  def get_jwt_public_key_file(self):
    """
    Gets the configured path to the public key file
    :return: the configured path to the public key file
    """
    return self.jwt_public_key_file

  def get_jwt_public_key(self, include_header_and_footer=False, remove_line_breaks=True):
    """
    Retrieves, formats, and returns the PEM data from the configured 509 certificate file.

    Attempts to read in the file specified by the value in ambari-server-properties/authentication.jwt.publicKey.
    If the file exists and is readable, the content is read.  If the header and foooter need to exists, and
    do not, the will be added. If they need to be removed, they will be removed if they exist.  Any line
    break characters will be leave alone unles the caller specifies them to be removed. Line break
    characters will not be added if missing.

    :param include_header_and_footer: True, to include the standard header and footer; False to remove
    the standard header and footer
    :param remove_line_breaks: True, remove and line breaks from PEM data; False to leave any existing line break as-is
    :return:  formats, and returns the PEM data from an x509 certificate file
    """
    public_cert = None

    if (self.jwt_public_key_file) and os.path.isfile(self.jwt_public_key_file):
      with open(self.jwt_public_key_file, "r") as f:
        public_cert = f.read()

    if public_cert:
      public_cert = public_cert.lstrip().rstrip()

      if include_header_and_footer:
        # Ensure the header and footer are in the string
        if not public_cert.startswith(CERTIFICATE_HEADER):
          public_cert = CERTIFICATE_HEADER + '\n' + public_cert

        if not public_cert.endswith(CERTIFICATE_FOOTER):
          public_cert = public_cert + '\n' + CERTIFICATE_FOOTER
      else:
        # Ensure the header and footer are not in the string
        if public_cert.startswith(CERTIFICATE_HEADER):
          public_cert = public_cert[len(CERTIFICATE_HEADER):]

        if public_cert.endswith(CERTIFICATE_FOOTER):
          public_cert = public_cert[:len(public_cert) - len(CERTIFICATE_FOOTER)]

      # Remove any leading and ending line breaks
      public_cert = public_cert.lstrip().rstrip()

      if remove_line_breaks:
        public_cert = public_cert.replace('\n', '')

    return public_cert
