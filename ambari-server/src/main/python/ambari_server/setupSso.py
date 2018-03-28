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
import ambari_simplejson as json
import base64
import logging
import re
import sys
import time
import urllib2

from ambari_commons.os_utils import is_root, run_os_command, copy_file, set_file_permissions, remove_file
from ambari_commons.exceptions import FatalException, NonFatalException
from ambari_commons.logging_utils import get_silent, print_info_msg
from ambari_server.userInput import get_validated_string_input, get_YN_input, get_multi_line_input
from ambari_server.serverUtils import is_server_runing, get_ambari_server_api_base, get_ambari_admin_username_password_pair, get_cluster_name, perform_changes_via_rest_api
from ambari_server.setupSecurity import REGEX_TRUE_FALSE
from ambari_server.serverConfiguration import get_ambari_properties, get_value_from_properties, update_properties, \
  store_password_file
from contextlib import closing

logger = logging.getLogger(__name__)

JWT_AUTH_ENBABLED = "authentication.jwt.enabled"
JWT_AUTH_PROVIDER_URL = "authentication.jwt.providerUrl"
JWT_PUBLIC_KEY = "authentication.jwt.publicKey"
JWT_AUDIENCES = "authentication.jwt.audiences"
JWT_COOKIE_NAME = "authentication.jwt.cookieName"
JWT_ORIGINAL_URL_QUERY_PARAM = "authentication.jwt.originalUrlParamName"

JWT_COOKIE_NAME_DEFAULT = "hadoop-jwt"
JWT_ORIGINAL_URL_QUERY_PARAM_DEFAULT = "originalUrl"
JWT_AUTH_PROVIDER_URL_DEFAULT = "http://example.com"

REGEX_ANYTHING = ".*"

JWT_PUBLIC_KEY_FILENAME = "jwt-cert.pem"
JWT_PUBLIC_KEY_HEADER = "-----BEGIN CERTIFICATE-----\n"
JWT_PUBLIC_KEY_FOOTER = "\n-----END CERTIFICATE-----\n"

REGEX_URL = "http[s]?://(?:[a-zA-Z]|[0-9]|[$-_@.&+]|[!*\(\),]|(?:%[0-9a-fA-F][0-9a-fA-F]))+\S*$"

SSO_MANAGE_SERVICES = "ambari.sso.manage_services"
SSO_ENABLED_SERVICES = "ambari.sso.enabled_services"
WILDCARD_FOR_ALL_SERVICES = "*"
SERVICE_NAME_AMBARI = 'Ambari'

URL_TO_FETCH_SERVICES_ELIGIBLE_FOR_SSO = "clusters/:CLUSTER_NAME/services?ServiceInfo/sso_integration_supported=true" ## :CLUSTER_NAME should be replaced
SETUP_SSO_CONFIG_URL = 'services/AMBARI/components/AMBARI_SERVER/configurations/sso-configuration'


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
      provider_url = get_value_from_properties(properties, JWT_AUTH_PROVIDER_URL, JWT_AUTH_PROVIDER_URL_DEFAULT)
      provider_url = get_validated_string_input("Provider URL [URL] ({0}):".format(provider_url), provider_url, REGEX_URL,
                                                "Invalid provider URL", False)
  else:
    provider_url = options.sso_provider_url

  properties.process_pair(JWT_AUTH_PROVIDER_URL, provider_url)


def populate_sso_public_cert(options, properties):
  if not options.sso_public_cert_file:
    cert_path = properties.get_property(JWT_PUBLIC_KEY)
    cert_string = get_multi_line_input("Public Certificate pem ({0})".format('stored' if cert_path else 'empty'))
    store_new_cert = False
    if cert_string is not None:
        store_new_cert = True
    if store_new_cert:
      full_cert = JWT_PUBLIC_KEY_HEADER + cert_string + JWT_PUBLIC_KEY_FOOTER
      cert_path = store_password_file(full_cert, JWT_PUBLIC_KEY_FILENAME)
  else:
    cert_path = options.sso_public_cert_file

  properties.process_pair(JWT_PUBLIC_KEY, cert_path)


def populate_jwt_cookie_name(options, properties):
  if not options.sso_jwt_cookie_name:
    cookie_name = get_value_from_properties(properties, JWT_COOKIE_NAME, JWT_COOKIE_NAME_DEFAULT)
    cookie_name = get_validated_string_input("JWT Cookie name ({0}):".format(cookie_name), cookie_name, REGEX_ANYTHING,
                                         "Invalid cookie name", False)
  else:
    cookie_name = options.sso_jwt_cookie_name

  properties.process_pair(JWT_COOKIE_NAME, cookie_name)


def populate_jwt_audiences(options, properties):
  if not options.sso_jwt_audience_list:
    audiences = properties.get_property(JWT_AUDIENCES)
    audiences = get_validated_string_input("JWT audiences list (comma-separated), empty for any ({0}):".format(audiences), audiences,
                                        REGEX_ANYTHING, "Invalid value", False)
  else:
    audiences = options.sso_jwt_audience_list

  properties.process_pair(JWT_AUDIENCES, audiences)
  
def get_eligible_services(properties, admin_login, admin_password, cluster_name):
  safe_cluster_name = urllib2.quote(cluster_name)
  url = get_ambari_server_api_base(properties) + URL_TO_FETCH_SERVICES_ELIGIBLE_FOR_SSO.replace(":CLUSTER_NAME", safe_cluster_name)
  admin_auth = base64.encodestring('%s:%s' % (admin_login, admin_password)).replace('\n', '')
  request = urllib2.Request(url)
  request.add_header('Authorization', 'Basic %s' % admin_auth)
  request.add_header('X-Requested-By', 'ambari')
  request.get_method = lambda: 'GET'

  services = [SERVICE_NAME_AMBARI]
  sys.stdout.write('\nFetching SSO enabled services')
  numOfTries = 0
  request_in_progress = True
  while request_in_progress:
    numOfTries += 1
    if (numOfTries == 60):
      raise FatalException(1, "Could not fetch eligible services within a minute; giving up!")
    sys.stdout.write('.')
    sys.stdout.flush()

    try:
      with closing(urllib2.urlopen(request)) as response:
        response_status_code = response.getcode()
        if response_status_code != 200:
          request_in_progress = False
          err = 'Error while fetching eligible services. Http status code - ' + str(response_status_code)
          raise FatalException(1, err)
        else:
            response_body = json.loads(response.read())
            items = response_body['items']
            if len(items) > 0:
              for item in items:
                services.append(item['ServiceInfo']['service_name'])
              if not services:
                time.sleep(1)
              else:
                request_in_progress = False
            else:
              request_in_progress = False

    except Exception as e:
      request_in_progress = False
      err = 'Error while fetching eligible services. Error details: %s' % e
      raise FatalException(1, err)
  if (len(services) == 0):
    sys.stdout.write('\nThere is no SSO enabled services found\n')
  else:
    sys.stdout.write('\nFound SSO enabled services: {0}\n'.format(', '.join(str(s) for s in services)))
  return services

def get_services_requires_sso(options, properties, admin_login, admin_password):
  if not options.sso_enabled_services:
    configure_for_all_services = get_YN_input("Use SSO for all services [y/n] (n): ", False)
    if configure_for_all_services:
      services = WILDCARD_FOR_ALL_SERVICES
    else:
      cluster_name = get_cluster_name(properties, admin_login, admin_password)
      eligible_services = get_eligible_services(properties, admin_login, admin_password, cluster_name)
      services = ''
      for service in eligible_services:
        question = "Use SSO for {0} [y/n] (y): ".format(service)
        if get_YN_input(question, True):
          if len(services) > 0:
            services = services + ", "
          services = services + service
  else:
    services = options.sso_enabled_services

  return services

def get_sso_property_from_db(properties, admin_login, admin_password, property_name):
  sso_property = None
  url = get_ambari_server_api_base(properties) + SETUP_SSO_CONFIG_URL
  admin_auth = base64.encodestring('%s:%s' % (admin_login, admin_password)).replace('\n', '')
  request = urllib2.Request(url)
  request.add_header('Authorization', 'Basic %s' % admin_auth)
  request.add_header('X-Requested-By', 'ambari')
  request.get_method = lambda: 'GET'
  request_in_progress = True

  sys.stdout.write('\nFetching SSO configuration from DB')
  numOfTries = 0
  while request_in_progress:
    numOfTries += 1
    if (numOfTries == 60):
      raise FatalException(1, "Could not fetch SSO configuration within a minute; giving up!")
    sys.stdout.write('.')
    sys.stdout.flush()

    try:
      with closing(urllib2.urlopen(request)) as response:
        response_status_code = response.getcode()
        if response_status_code != 200:
          request_in_progress = False
          if response_status_code == 404:
            ## This means that there is no SSO configuration in the database yet -> we can not fetch the property (but this is NOT an error)
            sso_property = None
          else:
            err = 'Error while fetching SSO configuration. Http status code - ' + str(response_status_code)
            raise FatalException(1, err)
        else:
            response_body = json.loads(response.read())
            sso_properties = response_body['Configuration']['properties']
            sso_property = sso_properties[property_name]
            if not sso_property:
              time.sleep(1)
            else:
              request_in_progress = False
    except Exception as e:
      request_in_progress = False
      err = 'Error while fetching SSO configuration. Error details: %s' % e
      raise FatalException(1, err)

  return sso_property

def update_sso_conf(properties, enable_sso, services, admin_login, admin_password):
  sso_configuration_properties = {}
  sso_configuration_properties[SSO_MANAGE_SERVICES] = "true" if enable_sso else "false"
  sso_configuration_properties[SSO_ENABLED_SERVICES] = services
  request_data = {
    "Configuration": {
      "category": "sso-configuration",
      "properties": {
      }
    }
  }
  request_data['Configuration']['properties'] = sso_configuration_properties
  perform_changes_via_rest_api(properties, admin_login, admin_password, SETUP_SSO_CONFIG_URL, 'PUT', request_data)


def setup_sso(options):
  logger.info("Setup SSO.")
  if not is_root():
    raise FatalException(4, 'ambari-server setup-sso should be run with root-level privileges')

  server_status, pid = is_server_runing()
  if not server_status:
    err = 'Ambari Server is not running.'
    raise FatalException(1, err)

  if not get_silent():
    validate_options(options)

    properties = get_ambari_properties()

    admin_login, admin_password = get_ambari_admin_username_password_pair(options)

    if not options.sso_enabled:
      sso_enabled_from_db = get_sso_property_from_db(properties, admin_login, admin_password, SSO_MANAGE_SERVICES)
      sso_enabled = sso_enabled_from_db == None or sso_enabled_from_db in ['true']
      print_info_msg("SSO is currently {0}".format("not configured" if sso_enabled_from_db == None else ("enabled" if sso_enabled else "disabled")), True)
      if sso_enabled:
        enable_sso = not get_YN_input("Do you want to disable SSO authentication [y/n] (n)? ", False)
      else:
        if get_YN_input("Do you want to configure SSO authentication [y/n] (y)? ", True):
          enable_sso = True
        else:
          return False
    else:
      enable_sso = options.sso_enabled == 'true'

    services = ''
    if enable_sso:
      populate_sso_provider_url(options, properties)
      populate_sso_public_cert(options, properties)
      populate_jwt_cookie_name(options, properties)
      populate_jwt_audiences(options, properties)
      services = get_services_requires_sso(options, properties, admin_login, admin_password)

    update_sso_conf(properties, enable_sso, services, admin_login, admin_password)

    enable_jwt_auth = WILDCARD_FOR_ALL_SERVICES == services or SERVICE_NAME_AMBARI in services
    properties.process_pair(JWT_AUTH_ENBABLED, "true" if enable_jwt_auth else "false")
    update_properties(properties)

    pass
  else:
    warning = "setup-sso is not enabled in silent mode."
    raise NonFatalException(warning)

  pass
