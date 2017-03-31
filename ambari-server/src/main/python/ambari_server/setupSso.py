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
import logging

from ambari_commons.os_utils import is_root, run_os_command, copy_file, set_file_permissions, remove_file
from ambari_commons.exceptions import FatalException, NonFatalException
from ambari_commons.logging_utils import get_silent, print_warning_msg, print_error_msg
from ambari_server.userInput import get_validated_string_input, get_YN_input, get_multi_line_input

from ambari_server.serverConfiguration import get_ambari_properties, get_value_from_properties, update_properties, \
  store_password_file

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



def setup_sso(args):
  logger.info("Setup SSO.")
  if not is_root():
    err = 'ambari-server setup-sso should be run with ' \
          'root-level privileges'
    raise FatalException(4, err)
  if not get_silent():
    properties = get_ambari_properties()

    must_setup_params = False
    store_new_cert = False

    sso_enabled = properties.get_property(JWT_AUTH_ENBABLED).lower() in ['true']

    if sso_enabled:
      if get_YN_input("Do you want to disable SSO authentication [y/n] (n)?", False):
        properties.process_pair(JWT_AUTH_ENBABLED, "false")
    else:
      if get_YN_input("Do you want to configure SSO authentication [y/n] (y)?", True):
        properties.process_pair(JWT_AUTH_ENBABLED, "true")
        must_setup_params = True
      else:
        return False

    if must_setup_params:

      provider_url = get_value_from_properties(properties, JWT_AUTH_PROVIDER_URL, JWT_AUTH_PROVIDER_URL_DEFAULT)
      provider_url = get_validated_string_input("Provider URL [URL] ({0}):".format(provider_url),
                                                provider_url,
                                                REGEX_ANYTHING,
                                                "Invalid provider URL",
                                                False)
      properties.process_pair(JWT_AUTH_PROVIDER_URL, provider_url)

      cert_path = properties.get_property(JWT_PUBLIC_KEY)
      cert_string = get_multi_line_input("Public Certificate pem ({0})".format('stored' if cert_path else 'empty'))
      if cert_string is not None:
          store_new_cert = True

      if get_YN_input("Do you want to configure advanced properties [y/n] (n) ?", False):
        cookie_name = get_value_from_properties(properties, JWT_COOKIE_NAME, JWT_COOKIE_NAME_DEFAULT)
        cookie_name = get_validated_string_input("JWT Cookie name ({0}):".format(cookie_name),
                                                 cookie_name,
                                                 REGEX_ANYTHING,
                                                 "Invalid cookie name",
                                                 False)
        properties.process_pair(JWT_COOKIE_NAME, cookie_name)

        audiences = properties.get_property(JWT_AUDIENCES)
        audiences = get_validated_string_input("JWT audiences list (comma-separated), empty for any ({0}):".format(audiences),
                                               audiences,
                                               REGEX_ANYTHING,
                                               "Invalid value",
                                               False)
        properties.process_pair(JWT_AUDIENCES, audiences)

        # TODO not required for now as we support Knox only
        # orig_query_param = get_value_from_properties(JWT_ORIGINAL_URL_QUERY_PARAM, JWT_ORIGINAL_URL_QUERY_PARAM_DEFAULT)
        # orig_query_param = get_validated_string_input("Original URL query parameter name ({}):".format(orig_query_param),
        #                                               orig_query_param,
        #                                               REGEX_ANYTHING,
        #                                               "Invalid value",
        #                                               False)
        # properties.process_pair(JWT_ORIGINAL_URL_QUERY_PARAM, orig_query_param)

      if store_new_cert:
        full_cert = JWT_PUBLIC_KEY_HEADER + cert_string + JWT_PUBLIC_KEY_FOOTER
        cert_path = store_password_file(full_cert, JWT_PUBLIC_KEY_FILENAME)

      properties.process_pair(JWT_PUBLIC_KEY, cert_path)

    update_properties(properties)

    pass
  else:
    warning = "setup-sso is not enabled in silent mode."
    raise NonFatalException(warning)

  pass
