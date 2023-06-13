#!/usr/bin/env python

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

import base64
import urllib2
import ambari_simplejson as json # simplejson is much faster comparing to Python 2.6 json module and has the same functions set.
import logging
from resource_management.core.environment import Environment
from resource_management.libraries.script import Script
from resource_management.libraries.functions.stack_features import check_stack_feature
from resource_management.libraries.functions import StackFeature

logger = logging.getLogger()
RANGER_ADMIN_URL = '{{admin-properties/policymgr_external_url}}'
ADMIN_USERNAME = '{{ranger-env/admin_username}}'
ADMIN_PASSWORD = '{{ranger-env/admin_password}}'
RANGER_ADMIN_USERNAME = '{{ranger-env/ranger_admin_username}}'
RANGER_ADMIN_PASSWORD = '{{ranger-env/ranger_admin_password}}'
SECURITY_ENABLED = '{{cluster-env/security_enabled}}'

def get_tokens():
  """
  Returns a tuple of tokens in the format {{site/property}} that will be used
  to build the dictionary passed into execute

  :return tuple
  """
  return (RANGER_ADMIN_URL, ADMIN_USERNAME, ADMIN_PASSWORD, RANGER_ADMIN_USERNAME, RANGER_ADMIN_PASSWORD, SECURITY_ENABLED)


def execute(configurations={}, parameters={}, host_name=None):
  """
  Returns a tuple containing the result code and a pre-formatted result label

  Keyword arguments:
  configurations (dictionary): a mapping of configuration key to value
  parameters (dictionary): a mapping of script parameter key to value
  host_name (string): the name of this host where the alert is running
  """

  if configurations is None:
    return (('UNKNOWN', ['There were no configurations supplied to the script.']))

  ranger_link = None
  ranger_auth_link = None
  ranger_get_user = None
  admin_username = None
  admin_password = None
  ranger_admin_username = None
  ranger_admin_password = None
  security_enabled = False

  stack_version_formatted = Script.get_stack_version()
  stack_supports_ranger_kerberos = stack_version_formatted and check_stack_feature(StackFeature.RANGER_KERBEROS_SUPPORT, stack_version_formatted)

  if RANGER_ADMIN_URL in configurations:
    ranger_link = configurations[RANGER_ADMIN_URL]
    if ranger_link.endswith('/'):
      ranger_link = ranger_link[:-1]
    ranger_auth_link = '{0}/{1}'.format(ranger_link, 'service/public/api/repository/count')
    ranger_get_user = '{0}/{1}'.format(ranger_link, 'service/xusers/users')

  if ADMIN_USERNAME in configurations:
    admin_username = configurations[ADMIN_USERNAME]

  if ADMIN_PASSWORD in configurations:
    admin_password = configurations[ADMIN_PASSWORD]

  if RANGER_ADMIN_USERNAME in configurations:
    ranger_admin_username = configurations[RANGER_ADMIN_USERNAME]

  if RANGER_ADMIN_PASSWORD in configurations:
    ranger_admin_password = configurations[RANGER_ADMIN_PASSWORD]

  if SECURITY_ENABLED in configurations:
    security_enabled = str(configurations[SECURITY_ENABLED]).upper() == 'TRUE'

  label = None
  result_code = 'OK'

  try:
    if security_enabled and stack_supports_ranger_kerberos:
      result_code = 'UNKNOWN'
      label = 'This alert will get skipped for Ranger Admin on kerberos env'
    else:
      admin_http_code = check_ranger_login(ranger_auth_link, admin_username, admin_password)
      if admin_http_code == 200:
        get_user_code = get_ranger_user(ranger_get_user, admin_username, admin_password, ranger_admin_username)
        if get_user_code:
          user_http_code = check_ranger_login(ranger_auth_link, ranger_admin_username, ranger_admin_password)
          if user_http_code == 200:
            result_code = 'OK'
            label = 'Login Successful for users {0} and {1}'.format(admin_username, ranger_admin_username)
          elif user_http_code == 401:
            result_code = 'CRITICAL'
            label = 'User:{0} credentials on Ambari UI are not in sync with Ranger'.format(ranger_admin_username)
          else:
            result_code = 'WARNING'
            label = 'Ranger Admin service is not reachable, please restart the service'
        else:
          result_code = 'OK'
          label = 'Login Successful for user: {0}. User:{1} user not yet synced with Ranger'.format(admin_username, ranger_admin_username)
      elif admin_http_code == 401:
        result_code = 'CRITICAL'
        label = 'User:{0} credentials on Ambari UI are not in sync with Ranger'.format(admin_username)
      else:
        result_code = 'WARNING'
        label = 'Ranger Admin service is not reachable, please restart the service'

  except Exception, e:
    label = str(e)
    result_code = 'UNKNOWN'
    logger.exception(label)

  return ((result_code, [label]))

def check_ranger_login(ranger_auth_link, username, password):
  """
  params ranger_auth_link: ranger login url
  params username: user credentials
  params password: user credentials

  return response code
  """
  try:
    usernamepassword = '{0}:{1}'.format(username, password)
    base_64_string = base64.encodestring(usernamepassword).replace('\n', '')
    request = urllib2.Request(ranger_auth_link)
    request.add_header("Content-Type", "application/json")
    request.add_header("Accept", "application/json")
    request.add_header("Authorization", "Basic {0}".format(base_64_string))
    result = urllib2.urlopen(request, timeout=20)
    response_code = result.getcode()
    if response_code == 200:
      response = json.loads(result.read())
    return response_code
  except urllib2.HTTPError, e:
    logger.exception("Error during Ranger service authentication. Http status code - {0}. {1}".format(e.code, e.read()))
    return e.code
  except urllib2.URLError, e:
    logger.exception("Error during Ranger service authentication. {0}".format(e.reason))
    return None
  except Exception, e:
    return 401

def get_ranger_user(ranger_get_user, username, password, user):
  """
  params ranger_get_user: ranger get user url
  params username: user credentials
  params password: user credentials
  params user: user to be search
  return Boolean if user exist or not
  """
  try:
    url = '{0}?name={1}'.format(ranger_get_user, user)
    usernamepassword = '{0}:{1}'.format(username, password)
    base_64_string = base64.encodestring(usernamepassword).replace('\n', '')
    request = urllib2.Request(url)
    request.add_header("Content-Type", "application/json")
    request.add_header("Accept", "application/json")
    request.add_header("Authorization", "Basic {0}".format(base_64_string))
    result = urllib2.urlopen(request, timeout=20)
    response_code = result.getcode()
    response = json.loads(result.read())
    if response_code == 200 and len(response['vXUsers']) > 0:
      for xuser in response['vXUsers']:
        if xuser['name'] == user:
          return True
    else:
      return False
  except urllib2.HTTPError, e:
    logger.exception("Error getting user from Ranger service. Http status code - {0}. {1}".format(e.code, e.read()))
    return False
  except urllib2.URLError, e:
    logger.exception("Error getting user from Ranger service. {0}".format(e.reason))
    return False
  except Exception, e:
    return False
