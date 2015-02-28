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
from resource_management.core.environment import Environment
from resource_management.core.resources import Execute
from resource_management.core.shell import call
from resource_management.libraries.functions import format
from resource_management.libraries.functions import get_kinit_path
from resource_management.libraries.functions import get_klist_path
from ambari_commons.os_check import OSConst, OSCheck
from os import getpid, sep
from urlparse import urlparse

RESULT_CODE_OK = 'OK'
RESULT_CODE_CRITICAL = 'CRITICAL'
RESULT_CODE_UNKNOWN = 'UNKNOWN'

OOZIE_URL_KEY = '{{oozie-site/oozie.base.url}}'
SECURITY_ENABLED = '{{cluster-env/security_enabled}}'
OOZIE_PRINCIPAL = '{{oozie-site/oozie.authentication.kerberos.principal}}'
OOZIE_KEYTAB = '{{oozie-site/oozie.authentication.kerberos.keytab}}'

def get_tokens():
  """
  Returns a tuple of tokens in the format {{site/property}} that will be used
  to build the dictionary passed into execute
  """
  return (OOZIE_URL_KEY, OOZIE_PRINCIPAL, SECURITY_ENABLED, OOZIE_KEYTAB)

def execute(parameters=None, host_name=None):
  """
  Returns a tuple containing the result code and a pre-formatted result label

  Keyword arguments:
  parameters (dictionary): a mapping of parameter key to value
  host_name (string): the name of this host where the alert is running
  """

  if parameters is None:
    return (RESULT_CODE_UNKNOWN, ['There were no parameters supplied to the script.'])

  if not OOZIE_URL_KEY in parameters:
    return (RESULT_CODE_UNKNOWN, ['The Oozie URL is a required parameter.'])

  # use localhost on Windows, 0.0.0.0 on others; 0.0.0.0 means bind to all
  # interfaces, which doesn't work on Windows
  localhost_address = 'localhost' if OSCheck.get_os_family() == OSConst.WINSRV_FAMILY else '0.0.0.0'

  oozie_url = parameters[OOZIE_URL_KEY]
  oozie_url = oozie_url.replace(urlparse(oozie_url).hostname,localhost_address)

  security_enabled = False
  if SECURITY_ENABLED in parameters:
    security_enabled = str(parameters[SECURITY_ENABLED]).upper() == 'TRUE'

  command = format("source /etc/oozie/conf/oozie-env.sh ; oozie admin -oozie {oozie_url} -status")

  try:
    # kinit if security is enabled so that oozie-env.sh can make the web request
    kerberos_env = None

    if security_enabled:
      if OOZIE_KEYTAB in parameters and OOZIE_PRINCIPAL in parameters:
        oozie_keytab = parameters[OOZIE_KEYTAB]
        oozie_principal = parameters[OOZIE_PRINCIPAL]

        # substitute _HOST in kerberos principal with actual fqdn
        oozie_principal = oozie_principal.replace('_HOST', host_name)
      else:
        return (RESULT_CODE_UNKNOWN, ['The Oozie keytab and principal are required parameters when security is enabled.'])

      # Create the kerberos credentials cache (ccache) file and set it in the environment to use
      # when executing curl
      env = Environment.get_instance()
      ccache_file = "{0}{1}oozie_alert_cc_{2}".format(env.tmp_dir, sep, getpid())
      kerberos_env = {'KRB5CCNAME': ccache_file}

      klist_path_local = get_klist_path()
      klist_command = format("{klist_path_local} -s {ccache_file}")

      # Determine if we need to kinit by testing to see if the relevant cache exists and has
      # non-expired tickets.  Tickets are marked to expire after 5 minutes to help reduce the number
      # it kinits we do but recover quickly when keytabs are regenerated
      return_code, _ = call(klist_command)
      if return_code != 0:
        kinit_path_local = get_kinit_path()
        kinit_command = format("{kinit_path_local} -l 5m -kt {oozie_keytab} {oozie_principal}; ")

        # kinit
        Execute(kinit_command, environment=kerberos_env)

    # execute the command
    Execute(command, environment=kerberos_env)

    return (RESULT_CODE_OK, ["Successful connection to {0}".format(oozie_url)])

  except Exception, ex:
    return (RESULT_CODE_CRITICAL, [str(ex)])
