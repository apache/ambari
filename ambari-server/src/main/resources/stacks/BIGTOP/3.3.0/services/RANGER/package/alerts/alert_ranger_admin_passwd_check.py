#!/usr/bin/env python3
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
import urllib.request, urllib.error, urllib.parse
import json
import logging
from resource_management.libraries.script import Script
from resource_management.libraries.functions.stack_features import check_stack_feature
from resource_management.libraries.functions import StackFeature

logger = logging.getLogger()
RANGER_ADMIN_URL = "{{admin-properties/policymgr_external_url}}"
ADMIN_USERNAME = "{{ranger-env/admin_username}}"
ADMIN_PASSWORD = "{{ranger-env/admin_password}}"
RANGER_ADMIN_USERNAME = "{{ranger-env/ranger_admin_username}}"
RANGER_ADMIN_PASSWORD = "{{ranger-env/ranger_admin_password}}"
SECURITY_ENABLED = "{{cluster-env/security_enabled}}"


def _strict_bool(value, name):
  if isinstance(value, bool):
    return value
  if isinstance(value, str):
    normalized = value.strip().lower()
    if normalized == "true":
      return True
    if normalized == "false":
      return False
  raise ValueError(f"{name} must be true or false")


def _validate_ranger_url(value):
  try:
    parsed_url = urllib.parse.urlsplit(value)
    hostname = parsed_url.hostname
    port = parsed_url.port
  except (TypeError, ValueError) as error:
    raise ValueError("Ranger Admin URL is invalid") from error
  if (
    parsed_url.scheme not in ("http", "https")
    or not hostname
    or parsed_url.username is not None
    or parsed_url.password is not None
    or parsed_url.query
    or parsed_url.fragment
    or (port is not None and not 1 <= port <= 65535)
  ):
    raise ValueError("Ranger Admin URL is invalid")
  return value.rstrip("/")


def get_tokens():
  """
  Returns a tuple of tokens in the format {{site/property}} that will be used
  to build the dictionary passed into execute

  :return tuple
  """
  return (
    RANGER_ADMIN_URL,
    ADMIN_USERNAME,
    ADMIN_PASSWORD,
    RANGER_ADMIN_USERNAME,
    RANGER_ADMIN_PASSWORD,
    SECURITY_ENABLED,
  )


def execute(configurations=None, parameters=None, host_name=None):
  """
  Returns a tuple containing the result code and a pre-formatted result label

  Keyword arguments:
  configurations (dictionary): a mapping of configuration key to value
  parameters (dictionary): a mapping of script parameter key to value
  host_name (string): the name of this host where the alert is running
  """

  if configurations is None:
    return ("UNKNOWN", ["There were no configurations supplied to the script."])

  ranger_link = None
  ranger_auth_link = None
  ranger_get_user = None
  admin_username = None
  admin_password = None
  ranger_admin_username = None
  ranger_admin_password = None
  security_enabled = False

  stack_version_formatted = Script.get_stack_version()
  stack_supports_ranger_kerberos = stack_version_formatted and check_stack_feature(
    StackFeature.RANGER_KERBEROS_SUPPORT, stack_version_formatted
  )

  if RANGER_ADMIN_URL in configurations:
    try:
      ranger_link = _validate_ranger_url(configurations[RANGER_ADMIN_URL])
    except ValueError as error:
      return ("UNKNOWN", [str(error)])
    ranger_auth_link = f"{ranger_link}/service/public/api/repository/count"
    ranger_get_user = f"{ranger_link}/service/xusers/users"

  if ADMIN_USERNAME in configurations:
    admin_username = configurations[ADMIN_USERNAME]

  if ADMIN_PASSWORD in configurations:
    admin_password = configurations[ADMIN_PASSWORD]

  if RANGER_ADMIN_USERNAME in configurations:
    ranger_admin_username = configurations[RANGER_ADMIN_USERNAME]

  if RANGER_ADMIN_PASSWORD in configurations:
    ranger_admin_password = configurations[RANGER_ADMIN_PASSWORD]

  if SECURITY_ENABLED in configurations:
    try:
      security_enabled = _strict_bool(
        configurations[SECURITY_ENABLED], "cluster-env/security_enabled"
      )
    except ValueError as error:
      return ("UNKNOWN", [str(error)])

  label = None
  result_code = "OK"

  try:
    if security_enabled and stack_supports_ranger_kerberos:
      result_code = "UNKNOWN"
      label = "This alert will get skipped for Ranger Admin on kerberos env"
    else:
      required_values = (
        ranger_auth_link,
        ranger_get_user,
        admin_username,
        admin_password,
        ranger_admin_username,
        ranger_admin_password,
      )
      if any(not isinstance(value, str) or not value for value in required_values):
        raise ValueError("Required Ranger alert configuration is missing")
      admin_http_code = check_ranger_login(
        ranger_auth_link, admin_username, admin_password
      )
      if admin_http_code == 200:
        get_user_code = get_ranger_user(
          ranger_get_user, admin_username, admin_password, ranger_admin_username
        )
        if get_user_code:
          user_http_code = check_ranger_login(
            ranger_auth_link, ranger_admin_username, ranger_admin_password
          )
          if user_http_code == 200:
            result_code = "OK"
            label = (
              f"Login Successful for users {admin_username} and {ranger_admin_username}"
            )
          elif user_http_code == 401:
            result_code = "CRITICAL"
            label = f"User:{ranger_admin_username} credentials on Ambari UI are not in sync with Ranger"
          else:
            result_code = "WARNING"
            label = "Ranger Admin service is not reachable, please restart the service"
        else:
          result_code = "OK"
          label = f"Login Successful for user: {admin_username}. User:{ranger_admin_username} user not yet synced with Ranger"
      elif admin_http_code == 401:
        result_code = "CRITICAL"
        label = (
          f"User:{admin_username} credentials on Ambari UI are not in sync with Ranger"
        )
      else:
        result_code = "WARNING"
        label = "Ranger Admin service is not reachable, please restart the service"

  except Exception:
    label = "Ranger credential check failed"
    result_code = "UNKNOWN"
    logger.exception(label)

  return (result_code, [label])


def check_ranger_login(ranger_auth_link, username, password):
  """
  params ranger_auth_link: ranger login url
  params username: user credentials
  params password: user credentials

  return response code
  """
  try:
    usernamepassword = f"{username}:{password}"
    base_64_string = (
      base64.b64encode(usernamepassword.encode()).decode().replace("\n", "")
    )
    request = urllib.request.Request(ranger_auth_link)
    request.add_header("Content-Type", "application/json")
    request.add_header("Accept", "application/json")
    request.add_header("Authorization", f"Basic {base_64_string}")
    with urllib.request.urlopen(request, timeout=20) as result:
      return result.getcode()
  except urllib.error.HTTPError as e:
    logger.exception(
      f"Error during Ranger service authentication. Http status code - {e.code}."
    )
    return e.code
  except urllib.error.URLError:
    logger.exception("Error connecting to Ranger during authentication")
    return None
  except (TypeError, ValueError):
    logger.exception("Invalid Ranger authentication request")
    return None


def get_ranger_user(ranger_get_user, username, password, user):
  """
  params ranger_get_user: ranger get user url
  params username: user credentials
  params password: user credentials
  params user: user to be search
  return Boolean if user exist or not
  """
  try:
    url = f"{ranger_get_user}?{urllib.parse.urlencode({'name': user})}"
    usernamepassword = f"{username}:{password}"
    base_64_string = (
      base64.b64encode(usernamepassword.encode()).decode().replace("\n", "")
    )
    request = urllib.request.Request(url)
    request.add_header("Content-Type", "application/json")
    request.add_header("Accept", "application/json")
    request.add_header("Authorization", f"Basic {base_64_string}")
    with urllib.request.urlopen(request, timeout=20) as result:
      response_code = result.getcode()
      response = json.loads(result.read().decode("utf-8"))
    users = response.get("vXUsers") if isinstance(response, dict) else None
    if response_code != 200 or not isinstance(users, list):
      return False
    return any(
      isinstance(xuser, dict) and xuser.get("name") == user for xuser in users
    )
  except urllib.error.HTTPError as e:
    logger.exception(
      f"Error getting user from Ranger service. Http status code - {e.code}."
    )
    return False
  except urllib.error.URLError:
    logger.exception("Error connecting to Ranger while looking up a user")
    return False
  except (TypeError, ValueError, UnicodeDecodeError, json.JSONDecodeError):
    logger.exception("Invalid Ranger user lookup response")
    return False
