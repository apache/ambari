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
import os
import re

from ambari_commons.exceptions import FatalException, NonFatalException
from ambari_commons.logging_utils import get_silent, print_info_msg
from ambari_server.serverConfiguration import get_ambari_properties, find_properties_file
from ambari_server.setupSecurity import REGEX_TRUE_FALSE
from ambari_server.userInput import get_validated_string_input, get_YN_input
from ambari_commons.logging_utils import print_error_msg
from ambari_server.serverUtils import get_value_from_dictionary

TPROXY_SUPPORT_ENABLED = 'ambari.tproxy.authentication.enabled'
PROXYUSER_HOSTS = 'ambari.tproxy.proxyuser.{}.hosts'
PROXYUSER_USERS = 'ambari.tproxy.proxyuser.{}.users'
PROXYUSER_GROUPS = 'ambari.tproxy.proxyuser.{}.groups'

REGEX_ANYTHING = '.*'
WILDCARD_FOR_ALL = '*'

def populate_tproxy_configuration_property(properties, tproxy_user_name, property_name, question_text_qualifier):
  resolved_property_name = property_name.format(tproxy_user_name)
  resolved_property_value = get_value_from_dictionary(properties, resolved_property_name, WILDCARD_FOR_ALL)
  resolved_property_value = get_validated_string_input("Allowed {0} for {1} ({2})? ".format(question_text_qualifier, tproxy_user_name, resolved_property_value), resolved_property_value, REGEX_ANYTHING, "Invalid input", False)
  properties[resolved_property_name] = resolved_property_value

def add_new_trusted_proxy_config(properties):
  tproxy_user_name = get_validated_string_input("The proxy user's (local) username? ", None, REGEX_ANYTHING, "Invalid Trusted Proxy User Name", False, allowEmpty=False)
  populate_tproxy_configuration_property(properties, tproxy_user_name, PROXYUSER_HOSTS, "hosts")
  populate_tproxy_configuration_property(properties, tproxy_user_name, PROXYUSER_USERS, "users")
  populate_tproxy_configuration_property(properties, tproxy_user_name, PROXYUSER_GROUPS, "groups")
  return get_YN_input("Add another proxy user [y/n] (n)? ", False)

def parse_trusted_configuration_file(tproxy_configuration_file_path, result):
  with open(tproxy_configuration_file_path) as tproxy_configuration_file:
    tproxy_configurations = json.loads(tproxy_configuration_file.read())
  if tproxy_configurations:
    for tproxy_configuration in tproxy_configurations:
      tproxy_user_name = tproxy_configuration['proxyuser']
      result[PROXYUSER_HOSTS.format(tproxy_user_name)] = tproxy_configuration['hosts']
      result[PROXYUSER_USERS.format(tproxy_user_name)] = tproxy_configuration['users']
      result[PROXYUSER_GROUPS.format(tproxy_user_name)] = tproxy_configuration['groups']

def validate_options(options):
  errors = []
  if options.tproxy_enabled and not re.match(REGEX_TRUE_FALSE, options.tproxy_enabled):
    errors.append("--tproxy-enabled should be to either 'true' or 'false'")
  if options.tproxy_configuration_file_path and options.tproxy_configuration_file_path is not None:
    if not os.path.isfile(options.tproxy_configuration_file_path):
      errors.append("--tproxy-configuration-file-path is set to a non-existing file: {}".format(options.tproxy_configuration_file_path))
  if len(errors) > 0:
    error_msg = "The following errors occurred while processing your request: {0}"
    raise FatalException(1, error_msg.format(str(errors)))

def remove_existing_tproxy_properties(ambari_properties):
  for key in (each for each in ambari_properties.propertyNames() if each.startswith('ambari.tproxy.proxyuser')):
    ambari_properties.removeOldProp(key)

def save_ambari_properties(properties):
  conf_file = find_properties_file()
  try:
    with open(conf_file, 'w') as f:
      properties.store_ordered(f)
  except Exception, e:
    print_error_msg('Could not write ambari config file "%s": %s' % (conf_file, e))

def update_ambari_properties(ambari_properties, a_dict):
  for key, value in a_dict.items():
    ambari_properties.process_pair(key, value)

def input_tproxy_config(options):
  properties = {}
  if not options.tproxy_configuration_file_path:
    interactive_tproxy_input(properties)
  else:
    parse_trusted_configuration_file(options.tproxy_configuration_file_path, properties)
  return properties

def interactive_tproxy_input(properties):
  add_new_trusted_proxy = add_new_trusted_proxy_config(properties)
  while add_new_trusted_proxy:
    add_new_trusted_proxy = add_new_trusted_proxy_config(properties)

def setup_trusted_proxy(options):
  print_info_msg("Setup Trusted Proxy")
  if get_silent():
    raise NonFatalException('setup-trusted-proxy is not enabled in silent mode.')
  validate_options(options)
  ambari_properties = get_ambari_properties()

  if ambari_properties.get_property(TPROXY_SUPPORT_ENABLED) == 'true':
    print_info_msg('\nTrusted Proxy support is currently enabled.\n')
    if not get_YN_input('Do you want to disable Trusted Proxy support [y/n] (n)? ', False):
      return
    ambari_properties.process_pair(TPROXY_SUPPORT_ENABLED, 'false')
  else:
    print_info_msg('\nTrusted Proxy support is currently disabled.\n')
    if not get_YN_input('Do you want to configure Trusted Proxy Support [y/n] (y)? ', True):
      return
    remove_existing_tproxy_properties(ambari_properties)
    ambari_properties.process_pair(TPROXY_SUPPORT_ENABLED, 'true')
    update_ambari_properties(ambari_properties, input_tproxy_config(options))

  save_ambari_properties(ambari_properties)
