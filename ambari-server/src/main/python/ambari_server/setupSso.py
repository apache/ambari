#!/usr/bin/env python

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
import re
import urllib2

import sys
from ambari_commons.exceptions import FatalException, NonFatalException
from ambari_commons.logging_utils import get_silent, print_info_msg

from ambari_server.serverConfiguration import get_ambari_properties
from ambari_server.serverUtils import is_server_runing, get_ambari_admin_username_password_pair, \
  get_cluster_name, perform_changes_via_rest_api, get_json_via_rest_api
from ambari_server.setupSecurity import REGEX_TRUE_FALSE
from ambari_server.userInput import get_validated_string_input, get_YN_input, get_multi_line_input

AMBARI_JWT_AUTH_ENBABLED = "ambari.sso.authentication.enabled"
SSO_MANAGE_SERVICES = "ambari.sso.manage_services"
SSO_ENABLED_SERVICES = "ambari.sso.enabled_services"
SSO_PROVIDER_URL = "ambari.sso.provider.url"
SSO_CERTIFICATE = "ambari.sso.provider.certificate"
SSO_PROVIDER_ORIGINAL_URL_QUERY_PARAM = "ambari.sso.provider.originalUrlParamName"
JWT_AUDIENCES = "ambari.sso.jwt.audiences"
JWT_COOKIE_NAME = "ambari.sso.jwt.cookieName"

SSO_PROVIDER_ORIGINAL_URL_QUERY_PARAM_DEFAULT = "originalUrl"
SSO_PROVIDER_URL_DEFAULT = "https://knox.example.com:8443/gateway/knoxsso/api/v1/websso"
JWT_COOKIE_NAME_DEFAULT = "hadoop-jwt"
JWT_AUDIENCES_DEFAULT = ""

CERTIFICATE_HEADER = "-----BEGIN CERTIFICATE-----"
CERTIFICATE_FOOTER = "-----END CERTIFICATE-----"

REGEX_ANYTHING = ".*"
REGEX_URL = "http[s]?://(?:[a-zA-Z]|[0-9]|[$-_@.&+]|[!*\(\),]|(?:%[0-9a-fA-F][0-9a-fA-F]))+\S*$"

WILDCARD_FOR_ALL_SERVICES = "*"
SERVICE_NAME_AMBARI = 'AMBARI'

FETCH_SERVICES_FOR_SSO_ENTRYPOINT = "clusters/%s/services?ServiceInfo/sso_integration_supported=true"
SSO_CONFIG_API_ENTRYPOINT = 'services/AMBARI/components/AMBARI_SERVER/configurations/sso-configuration'


def validate_options(options):
  errors = []
  if options.sso_enabled and not re.match(REGEX_TRUE_FALSE, options.sso_enabled):
    errors.append("--sso-enabled should be to either 'true' or 'false'")

  if options.sso_enabled == 'true':
    if not options.sso_provider_url:
      errors.append("Missing option: --sso-provider-url")
    if not options.sso_public_cert_file:
      errors.append("Missing option: --sso-public-cert-file")
    if options.sso_provider_url and not re.match(REGEX_URL, options.sso_provider_url):
      errors.append("Invalid --sso-provider-url")

  if len(errors) > 0:
    error_msg = "The following errors occurred while processing your request: {0}"
    raise FatalException(1, error_msg.format(str(errors)))


def populate_sso_provider_url(options, properties):
  if not options.sso_provider_url:
      provider_url = get_value_from_dictionary(properties, SSO_PROVIDER_URL, SSO_PROVIDER_URL_DEFAULT)
      provider_url = get_validated_string_input("Provider URL ({0}):".format(provider_url), provider_url, REGEX_URL,
                                                "Invalid provider URL", False)
  else:
    provider_url = options.sso_provider_url

  properties[SSO_PROVIDER_URL] = provider_url

def populate_sso_public_cert(options, properties):
  if not options.sso_public_cert_file:
    cert = get_value_from_dictionary(properties, SSO_CERTIFICATE)
    get_cert = True if not cert else get_YN_input("The SSO provider's public certificate has already set. Do you want to change it [y/n] (n): ", False)

    if get_cert:
      cert_string = get_multi_line_input("Public Certificate PEM")
      properties[SSO_CERTIFICATE] = ensure_complete_cert(cert_string) if cert_string else ""
  else:
    cert_path = options.sso_public_cert_file
    with open(cert_path) as cert_file:
      cert_string = cert_file.read()
    properties[SSO_CERTIFICATE] = ensure_complete_cert(cert_string) if cert_string else ""


def populate_jwt_cookie_name(options, properties):
  if not options.sso_jwt_cookie_name and (not options.sso_provider_url or not options.sso_public_cert_file):
    cookie_name = get_value_from_dictionary(properties, JWT_COOKIE_NAME, JWT_COOKIE_NAME_DEFAULT)
    cookie_name = get_validated_string_input("JWT Cookie name ({0}):".format(cookie_name), cookie_name, REGEX_ANYTHING,
                                         "Invalid cookie name", False)
  else:
    cookie_name = options.sso_jwt_cookie_name if options.sso_jwt_cookie_name else JWT_COOKIE_NAME_DEFAULT

  properties[JWT_COOKIE_NAME] = cookie_name


def populate_jwt_audiences(options, properties):
  if options.sso_jwt_audience_list is None and (not options.sso_provider_url or not options.sso_public_cert_file):
    audiences = get_value_from_dictionary(properties, JWT_AUDIENCES, JWT_AUDIENCES_DEFAULT)
    audiences = get_validated_string_input("JWT audiences list (comma-separated), empty for any ({0}):".format(audiences), audiences,
                                        REGEX_ANYTHING, "Invalid value", False)
  else:
    audiences = options.sso_jwt_audience_list if options.sso_jwt_audience_list else JWT_AUDIENCES_DEFAULT

  properties[JWT_AUDIENCES] = audiences
  
def get_eligible_services(properties, admin_login, admin_password, cluster_name):
  print_info_msg("Fetching SSO enabled services")

  safe_cluster_name = urllib2.quote(cluster_name)

  response_code, json_data = get_json_via_rest_api(properties, admin_login, admin_password,
                                                   FETCH_SERVICES_FOR_SSO_ENTRYPOINT % safe_cluster_name)

  services = [SERVICE_NAME_AMBARI]

  if json_data and 'items' in json_data:
    items = json_data['items']
    if len(items) > 0:
      for item in items:
        services.append(item['ServiceInfo']['service_name'])

    print_info_msg('Found SSO enabled services: %s' % ', '.join(services))

  return services


def get_services_requires_sso(options, ambari_properties, admin_login, admin_password):
  if not options.sso_enabled_services:
    configure_for_all_services = get_YN_input("Use SSO for all services [y/n] (n): ", False)
    if configure_for_all_services:
      services = [WILDCARD_FOR_ALL_SERVICES]
    else:
      services = []
      cluster_name = get_cluster_name(ambari_properties, admin_login, admin_password)

      if cluster_name:
        eligible_services = get_eligible_services(ambari_properties, admin_login, admin_password, cluster_name)
        if eligible_services:
          for service in eligible_services:
            question = "Use SSO for {0} [y/n] (y): ".format(service)
            if get_YN_input(question, True):
              services.append(service)
  else:
    services = options.sso_enabled_services.upper().split(',') if options.sso_enabled_services else []

  return services

def get_sso_properties(properties, admin_login, admin_password):
  print_info_msg("Fetching SSO configuration from DB")

  try:
    response_code, json_data = get_json_via_rest_api(properties, admin_login, admin_password, SSO_CONFIG_API_ENTRYPOINT)
  except urllib2.HTTPError as http_error:
    if http_error.code == 404:
      # This means that there is no SSO configuration in the database yet -> we can not fetch the
      # property (but this is NOT an error)
      json_data = None
    else:
      raise http_error

  if json_data and 'Configuration' in json_data and 'properties' in json_data['Configuration']:
    return json_data['Configuration']['properties']
  else:
    return {}

def update_sso_conf(ambari_properties, sso_configuration_properties, admin_login, admin_password):
  request_data = {
    "Configuration": {
      "category": "sso-configuration",
      "properties": {
      }
    }
  }
  request_data['Configuration']['properties'] = sso_configuration_properties
  perform_changes_via_rest_api(ambari_properties, admin_login, admin_password, SSO_CONFIG_API_ENTRYPOINT, 'PUT', request_data)


def setup_sso(options):
  print_info_msg("Setup SSO.")

  server_status, pid = is_server_runing()
  if not server_status:
    err = 'Ambari Server is not running.'
    raise FatalException(1, err)

  if not get_silent():
    validate_options(options)

    ambari_properties = get_ambari_properties()

    admin_login, admin_password = get_ambari_admin_username_password_pair(options)
    properties = get_sso_properties(ambari_properties, admin_login, admin_password)

    if not options.sso_enabled:
      sso_enabled = get_value_from_dictionary(properties, SSO_MANAGE_SERVICES, None)
      if sso_enabled:
        sso_status = "enabled" if sso_enabled == "true" else "disabled"
      else:
        sso_status = "not configured"
      sys.stdout.write("\nSSO is currently %s\n" % sso_status)

      if sso_status == "enabled":
        enable_sso = not get_YN_input("Do you want to disable SSO authentication [y/n] (n)? ", False)
      else:
        if get_YN_input("Do you want to configure SSO authentication [y/n] (y)? ", True):
          enable_sso = True
        else:
          return False
    else:
      enable_sso = options.sso_enabled == 'true'

    services = None
    if enable_sso:
      populate_sso_provider_url(options, properties)
      populate_sso_public_cert(options, properties)
      populate_jwt_cookie_name(options, properties)
      populate_jwt_audiences(options, properties)
      services = get_services_requires_sso(options, ambari_properties, admin_login, admin_password)

    enable_jwt_auth = services and (WILDCARD_FOR_ALL_SERVICES in services or SERVICE_NAME_AMBARI in services)
    properties[AMBARI_JWT_AUTH_ENBABLED]  = "true" if enable_jwt_auth else "false"
    properties[SSO_MANAGE_SERVICES] = "true" if enable_sso else "false"
    properties[SSO_ENABLED_SERVICES] = ','.join(services) if services else ""

    update_sso_conf(ambari_properties, properties, admin_login, admin_password)
    pass
  else:
    warning = "setup-sso is not enabled in silent mode."
    raise NonFatalException(warning)
  pass


def ensure_complete_cert(cert_string):
  if cert_string:
    cert_string = cert_string.lstrip().rstrip()

    # Ensure the header and footer are in the string
    if not cert_string.startswith(CERTIFICATE_HEADER):
      cert_string = CERTIFICATE_HEADER + '\n' + cert_string

    if not cert_string.endswith(CERTIFICATE_FOOTER):
      cert_string = cert_string + '\n' + CERTIFICATE_FOOTER

  return cert_string

def get_value_from_dictionary(properties, key, default_value=None):
  return properties[key] if properties and key in properties else default_value
