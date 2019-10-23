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

from resource_management.core import global_lock
from resource_management.core.resources import Execute
from resource_management.libraries.functions import format
from resource_management.core.signal_utils import TerminateStrategy
from resource_management.core.shell import quote_bash_args


def check_thrift_port_sasl(address, port, hive_auth="NOSASL", key=None, kinitcmd=None, smokeuser='ambari-qa',
                           hive_user='hive', transport_mode="binary", http_endpoint="cliservice",
                           ssl=False, ssl_keystore=None, ssl_password=None, check_command_timeout=30,
                           ldap_username="", ldap_password="", pam_username="", pam_password=""):
  """
  Hive thrift SASL port check
  """

  # check params to be correctly passed, if not - try to cast them
  if isinstance(port, str):
    port = int(port)

  if isinstance(ssl, str):
    ssl = bool(ssl)

  # to pass as beeline argument
  ssl_str = str(ssl).lower()
  beeline_url = ['jdbc:hive2://{address}:{port}/', "transportMode={transport_mode}"]

  # append url according to used transport
  if transport_mode == "http":
    beeline_url.append('httpPath={http_endpoint}')

  # append url according to used auth
  if hive_auth == "NOSASL":
    beeline_url.append('auth=noSasl')

  credential_str = "-n {hive_user}"

  # append username and password for LDAP
  if hive_auth == "LDAP":
    # password might contain special characters that need to be escaped
    quoted_ldap_password = quote_bash_args(ldap_password)
    credential_str = "-n {ldap_username} -p {quoted_ldap_password!p}"

  # append username and password for PAM
  if hive_auth == "PAM":
    # password might contain special characters that need to be escaped
    quoted_pam_password = quote_bash_args(pam_password)
    credential_str = "-n '{pam_username}' -p '{quoted_pam_password!p}'"

  # append url according to ssl configuration
  if ssl and ssl_keystore is not None and ssl_password is not None:
    beeline_url.extend(['ssl={ssl_str}', 'sslTrustStore={ssl_keystore}', 'trustStorePassword={ssl_password!p}'])

  # append url according to principal and execute kinit
  if kinitcmd and hive_auth != "LDAP":
    beeline_url.append('principal={key}')

    # prevent concurrent kinit
    kinit_lock = global_lock.get_lock(global_lock.LOCK_TYPE_KERBEROS)
    kinit_lock.acquire()
    try:
      Execute(kinitcmd, user=smokeuser)
    finally:
      kinit_lock.release()

  # -n the user to connect as (ignored when using the hive principal in the URL, can be different from the user running the beeline command)
  # -e ';' executes a SQL commmand of NOOP
  cmd = ("! (beeline -u '%s' %s -e ';' 2>&1 | awk '{print}' | grep -vz -i " + \
         "-e 'Connected to:' -e 'Transaction isolation:' -e 'inactive HS2 instance; use service discovery')") % \
        (format(";".join(beeline_url)), format(credential_str))

  Execute(cmd,
    user=smokeuser,
    path=["/bin/", "/usr/bin/", "/usr/lib/hive/bin/", "/usr/sbin/"],
    timeout=check_command_timeout,
    timeout_kill_strategy=TerminateStrategy.KILL_PROCESS_TREE,
  )
