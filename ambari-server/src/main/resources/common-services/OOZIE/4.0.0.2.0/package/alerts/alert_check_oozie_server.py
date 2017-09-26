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
import os
import re

from resource_management.core import global_lock
from resource_management.core.environment import Environment
from resource_management.core.resources import Execute
from resource_management.libraries.functions import format
from resource_management.libraries.functions import get_kinit_path
from resource_management.libraries.functions import get_klist_path
from resource_management.libraries.functions import stack_tools
from ambari_commons.os_check import OSConst, OSCheck
from ambari_commons.os_family_impl import OsFamilyFuncImpl, OsFamilyImpl
from urlparse import urlparse

STACK_ROOT_PATTERN = "{{ stack_root }}"
RESULT_CODE_OK = 'OK'
RESULT_CODE_CRITICAL = 'CRITICAL'
RESULT_CODE_UNKNOWN = 'UNKNOWN'

if OSCheck.is_windows_family():
  OOZIE_ENV_HTTPS_RE = r"set\s+OOZIE_HTTPS_PORT=(\d+)"
else:
  OOZIE_ENV_HTTPS_RE = r"export\s+OOZIE_HTTPS_PORT=(\d+)"

# The configured Kerberos executable search paths, if any
KERBEROS_EXECUTABLE_SEARCH_PATHS_KEY = '{{kerberos-env/executable_search_paths}}'

OOZIE_URL_KEY = '{{oozie-site/oozie.base.url}}'
SECURITY_ENABLED = '{{cluster-env/security_enabled}}'
OOZIE_USER = '{{oozie-env/oozie_user}}'
OOZIE_CONF_DIR = "{0}/current/oozie-server/conf".format(STACK_ROOT_PATTERN)
OOZIE_CONF_DIR_LEGACY = '/etc/oozie/conf'
OOZIE_HTTPS_PORT = '{{oozie-site/oozie.https.port}}'
OOZIE_ENV_CONTENT = '{{oozie-env/content}}'

USER_KEYTAB_KEY = '{{oozie-site/oozie.service.HadoopAccessorService.keytab.file}}'
USER_PRINCIPAL_KEY = '{{oozie-site/oozie.service.HadoopAccessorService.kerberos.principal}}'
USER_KEY = '{{oozie-env/oozie_user}}'

# default keytab location
USER_KEYTAB_SCRIPT_PARAM_KEY = 'default.oozie.keytab'
USER_KEYTAB_DEFAULT = '/etc/security/keytabs/oozie.headless.keytab'

# default user principal
USER_PRINCIPAL_SCRIPT_PARAM_KEY = 'default.oozie.principal'
USER_PRINCIPAL_DEFAULT = 'oozie@EXAMPLE.COM'

# default user
USER_DEFAULT = 'oozie'

STACK_NAME_KEY = '{{cluster-env/stack_name}}'
STACK_ROOT_KEY = '{{cluster-env/stack_root}}'
STACK_ROOT_DEFAULT = '/usr/hdp'

class KerberosPropertiesNotFound(Exception): pass

@OsFamilyFuncImpl(os_family=OSConst.WINSRV_FAMILY)
def get_tokens():
  """
  Returns a tuple of tokens in the format {{site/property}} that will be used
  to build the dictionary passed into execute
  """
  return (OOZIE_URL_KEY,)

@OsFamilyFuncImpl(os_family=OsFamilyImpl.DEFAULT)
def get_tokens():
  """
  Returns a tuple of tokens in the format {{site/property}} that will be used
  to build the dictionary passed into execute
  """
  return (OOZIE_URL_KEY, USER_PRINCIPAL_KEY, SECURITY_ENABLED, USER_KEYTAB_KEY, KERBEROS_EXECUTABLE_SEARCH_PATHS_KEY,
          USER_KEY, OOZIE_HTTPS_PORT, OOZIE_ENV_CONTENT, STACK_NAME_KEY, STACK_ROOT_KEY)

@OsFamilyFuncImpl(os_family=OSConst.WINSRV_FAMILY)
def get_check_command(oozie_url, host_name, configurations):
  from resource_management.libraries.functions import reload_windows_env
  reload_windows_env()
  oozie_home = os.environ['OOZIE_HOME']
  oozie_cmd = os.path.join(oozie_home, 'bin', 'oozie.cmd')
  command = format("cmd /c {oozie_cmd} admin -oozie {oozie_url} -status")
  return (command, None, None)

@OsFamilyFuncImpl(os_family=OsFamilyImpl.DEFAULT)
def get_check_command(oozie_url, host_name, configurations, parameters, only_kinit):
  kerberos_env = None

  user = USER_DEFAULT
  if USER_KEY in configurations:
    user = configurations[USER_KEY]

  if is_security_enabled(configurations):
    # defaults
    user_keytab = USER_KEYTAB_DEFAULT
    user_principal = USER_PRINCIPAL_DEFAULT

    # check script params
    if USER_PRINCIPAL_SCRIPT_PARAM_KEY in parameters:
      user_principal = parameters[USER_PRINCIPAL_SCRIPT_PARAM_KEY]
      user_principal = user_principal.replace('_HOST', host_name.lower())
    if USER_KEYTAB_SCRIPT_PARAM_KEY in parameters:
      user_keytab = parameters[USER_KEYTAB_SCRIPT_PARAM_KEY]

    # check configurations last as they should always take precedence
    if USER_PRINCIPAL_KEY in configurations:
      user_principal = configurations[USER_PRINCIPAL_KEY]
      user_principal = user_principal.replace('_HOST', host_name.lower())
    if USER_KEYTAB_KEY in configurations:
      user_keytab = configurations[USER_KEYTAB_KEY]

    # Create the kerberos credentials cache (ccache) file and set it in the environment to use
    # when executing curl
    env = Environment.get_instance()
    ccache_file = "{0}{1}oozie_alert_cc_{2}".format(env.tmp_dir, os.sep, os.getpid())
    kerberos_env = {'KRB5CCNAME': ccache_file}

    # Get the configured Kerberos executable search paths, if any
    kerberos_executable_search_paths = None
    if KERBEROS_EXECUTABLE_SEARCH_PATHS_KEY in configurations:
      kerberos_executable_search_paths = configurations[KERBEROS_EXECUTABLE_SEARCH_PATHS_KEY]

    klist_path_local = get_klist_path(kerberos_executable_search_paths)
    kinit_path_local = get_kinit_path(kerberos_executable_search_paths)
    kinit_part_command = format("{kinit_path_local} -l 5m20s -c {ccache_file} -kt {user_keytab} {user_principal}; ")

    # Determine if we need to kinit by testing to see if the relevant cache exists and has
    # non-expired tickets.  Tickets are marked to expire after 5 minutes to help reduce the number
    # it kinits we do but recover quickly when keytabs are regenerated

    if only_kinit:
      kinit_command = kinit_part_command
    else:
      kinit_command = "{0} -s {1} || ".format(klist_path_local, ccache_file) + kinit_part_command

    # prevent concurrent kinit
    kinit_lock = global_lock.get_lock(global_lock.LOCK_TYPE_KERBEROS)
    kinit_lock.acquire()
    try:
      Execute(kinit_command, environment=kerberos_env, user=user)
    finally:
      kinit_lock.release()

  # Configure stack root
  stack_root = STACK_ROOT_DEFAULT
  if STACK_NAME_KEY in configurations and STACK_ROOT_KEY in configurations:
    stack_root = stack_tools.get_stack_root(configurations[STACK_NAME_KEY], configurations[STACK_ROOT_KEY]).lower()

  # oozie configuration directory using a symlink
  oozie_config_directory = OOZIE_CONF_DIR.replace(STACK_ROOT_PATTERN, stack_root)
  if not os.path.exists(oozie_config_directory):
    oozie_config_directory = OOZIE_CONF_DIR_LEGACY

  command = "source {0}/oozie-env.sh ; oozie admin -oozie {1} -status".format(
    oozie_config_directory, oozie_url)

  return (command, kerberos_env, user)

def execute(configurations={}, parameters={}, host_name=None):
  """
  Returns a tuple containing the result code and a pre-formatted result label

  Keyword arguments:
  configurations (dictionary): a mapping of configuration key to value
  parameters (dictionary): a mapping of script parameter key to value
  host_name (string): the name of this host where the alert is running
  """

  if configurations is None:
    return (RESULT_CODE_UNKNOWN, ['There were no configurations supplied to the script.'])

  if not OOZIE_URL_KEY in configurations:
    return (RESULT_CODE_UNKNOWN, ['The Oozie URL is a required parameter.'])

  https_port = None
  # try to get https port form oozie-env content
  if OOZIE_ENV_CONTENT in configurations:
    for line in configurations[OOZIE_ENV_CONTENT].splitlines():
      result = re.match(OOZIE_ENV_HTTPS_RE, line)

      if result is not None:
        https_port = result.group(1)
  # or from oozie-site.xml
  if https_port is None and OOZIE_HTTPS_PORT in configurations:
    https_port = configurations[OOZIE_HTTPS_PORT]

  oozie_url = configurations[OOZIE_URL_KEY]

  # construct proper url for https
  if https_port is not None:
    parsed_url = urlparse(oozie_url)
    oozie_url = oozie_url.replace(parsed_url.scheme, "https")
    if parsed_url.port is None:
      oozie_url.replace(parsed_url.hostname, ":".join([parsed_url.hostname, str(https_port)]))
    else:
      oozie_url = oozie_url.replace(str(parsed_url.port), str(https_port))

  # https will not work with localhost address, we need put fqdn
  if https_port is None:
    oozie_url = oozie_url.replace(urlparse(oozie_url).hostname, host_name)

  (code, msg) = get_check_result(oozie_url, host_name, configurations, parameters, False)

  # sometimes real lifetime for ticket is less than we have set(5m20s aS of now)
  # so i've added this double check with rekinit command to be sure thaT it's not problem with ticket lifetime
  if is_security_enabled(configurations) and code == RESULT_CODE_CRITICAL:
    (code, msg) = get_check_result(oozie_url, host_name, configurations, parameters, True)

  return (code, msg)


def get_check_result(oozie_url, host_name, configurations, parameters, only_kinit):
  try:
    command, env, user = get_check_command(oozie_url, host_name, configurations, parameters, only_kinit)
    # execute the command
    Execute(command, environment=env, user=user)

    return (RESULT_CODE_OK, ["Successful connection to {0}".format(oozie_url)])
  except KerberosPropertiesNotFound, ex:
    return (RESULT_CODE_UNKNOWN, [str(ex)])
  except Exception, ex:
    return (RESULT_CODE_CRITICAL, [str(ex)])

def is_security_enabled(configurations):
  security_enabled = False
  if SECURITY_ENABLED in configurations:
    security_enabled = str(configurations[SECURITY_ENABLED]).upper() == 'TRUE'

  return security_enabled
