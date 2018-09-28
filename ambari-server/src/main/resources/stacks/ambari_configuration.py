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

  def get_ambari_sso_configuration(self):
    """
    Safely gets a dictionary of properties for the "sso-configuration" category.

    :return: a dictionary or None, if "sso-configuration" is not available
    """
    return self.get_ambari_server_configuration_category("sso-configuration")

  def get_ambari_sso_details(self):
    """
    Gets a dictionary of properties that may be used to configure a service for SSO integration.
    :return: a dictionary
    """
    return AmbariSSODetails(self.get_ambari_sso_configuration())


class AmbariSSODetails(object):
  """
  AmbariSSODetails encapsulates the SSO configuration data specified in the ambari-server-configuration data
  """

  def __init__(self, sso_properties):
    self.sso_properties = sso_properties

  def is_managing_services(self):
    """
    Tests the configuration data to determine if Ambari should be configuring servcies to enable SSO integration.

    The relevant property is "sso-configuration/ambari.sso.manage_services", which is expected
    to be a "true" or "false".

    :return: True, if Ambari should manage services' SSO configurations
    """
    return "true" == _get_from_dictionary(self.sso_properties, "ambari.sso.manage_services")

  def get_services_to_enable(self):
    """
    Safely gets the list of services that Ambari should enabled for SSO.

    The returned value is a list of the relevant service names converted to lowercase.

    :return: a list of service names converted to lowercase
    """
    sso_enabled_services = _get_from_dictionary(self.sso_properties, "ambari.sso.enabled_services")

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
    if self.is_managing_services():
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
    if self.is_managing_services():
      services_to_enable = self.get_services_to_enable()
      return "*" not in services_to_enable and service_name.lower() not in services_to_enable
    else:
      return False

  def get_jwt_audiences(self):
    """
    Gets the configured JWT audiences list

    The relevant property is "sso-configuration/ambari.sso.jwt.audiences", which is expected
    to be a comma-delimited list of audience names.

    :return the configured JWT audiences list:
    """
    return _get_from_dictionary(self.sso_properties, 'ambari.sso.jwt.audiences')

  def get_jwt_cookie_name(self):
    """
    Gets the configured JWT cookie name

    The relevant property is "sso-configuration/ambari.sso.jwt.cookieName", which is expected
    to be a string.

    :return: the configured JWT cookie name
    """
    return _get_from_dictionary(self.sso_properties, 'ambari.sso.jwt.cookieName')

  def get_sso_provider_url(self):
    """
    Gets the configured SSO provider URL

    The relevant property is "sso-configuration/ambari.sso.provider.url", which is expected
    to be a string.

    :return: the configured SSO provider URL
    """
    return _get_from_dictionary(self.sso_properties, 'ambari.sso.provider.url')

  def get_sso_provider_original_parameter_name(self):
    """
    Gets the configured SSO provider's original URL parameter name

    The relevant property is "sso-configuration/ambari.sso.provider.originalUrlParamName", which is
    expected to be a string.

    :return: the configured SSO provider's original URL parameter name
    """
    return _get_from_dictionary(self.sso_properties, 'ambari.sso.provider.originalUrlParamName')

  def get_sso_provider_certificate(self, include_header_and_footer=False, remove_line_breaks=True):
    """
    Retrieves, formats, and returns the PEM data from the stored 509 certificate.

    The relevant property is "sso-configuration/ambari.sso.provider.certificate", which is expected
    to be a PEM-encoded x509 certificate, including the header and footer.

    If the header and footer need to exist, and do not, the will be added. If they need to be removed,
    they will be removed if they exist.  Any line break characters will be left alone unless the
    caller specifies them to be removed. Line break characters will not be added if missing.

    :param include_header_and_footer: True, to include the standard header and footer; False to remove
    the standard header and footer
    :param remove_line_breaks: True, remove and line breaks from PEM data; False to leave any existing line break as-is
    :return:  formats, and returns the PEM data from an x509 certificate
    """
    public_cert = _get_from_dictionary(self.sso_properties, 'ambari.sso.provider.certificate')

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
