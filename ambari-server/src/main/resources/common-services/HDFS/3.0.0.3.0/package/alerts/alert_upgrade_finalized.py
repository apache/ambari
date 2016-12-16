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

import urllib2
import ambari_simplejson as json # simplejson is much faster comparing to Python 2.6 json module and has the same functions set.
import logging
import traceback

from resource_management.libraries.functions.curl_krb_request import curl_krb_request
from resource_management.libraries.functions.curl_krb_request import DEFAULT_KERBEROS_KINIT_TIMER_MS
from resource_management.libraries.functions.curl_krb_request import KERBEROS_KINIT_TIMER_PARAMETER
from resource_management.libraries.functions.curl_krb_request import CONNECTION_TIMEOUT_DEFAULT
from resource_management.core.environment import Environment
from resource_management.libraries.functions.namenode_ha_utils import get_all_namenode_addresses

NN_HTTP_ADDRESS_KEY = '{{hdfs-site/dfs.namenode.http-address}}'
NN_HTTPS_ADDRESS_KEY = '{{hdfs-site/dfs.namenode.https-address}}'
NN_HTTP_POLICY_KEY = '{{hdfs-site/dfs.http.policy}}'

HDFS_SITE_KEY = '{{hdfs-site}}'
KERBEROS_KEYTAB = '{{hdfs-site/dfs.web.authentication.kerberos.keytab}}'
KERBEROS_PRINCIPAL = '{{hdfs-site/dfs.web.authentication.kerberos.principal}}'
SECURITY_ENABLED_KEY = '{{cluster-env/security_enabled}}'
SMOKEUSER_KEY = "{{cluster-env/smokeuser}}"
EXECUTABLE_SEARCH_PATHS = '{{kerberos-env/executable_search_paths}}'
logger = logging.getLogger('ambari_alerts')

def get_tokens():
  """
  Returns a tuple of tokens in the format {{site/property}} that will be used
  to build the dictionary passed into execute

  :rtype tuple
  """
  return (HDFS_SITE_KEY, NN_HTTP_ADDRESS_KEY, NN_HTTPS_ADDRESS_KEY, NN_HTTP_POLICY_KEY, EXECUTABLE_SEARCH_PATHS,
          KERBEROS_KEYTAB, KERBEROS_PRINCIPAL, SECURITY_ENABLED_KEY, SMOKEUSER_KEY)


def execute(configurations={}, parameters={}, host_name=None):
  """
  Returns a tuple containing the result code and a pre-formatted result label

  Keyword arguments:
  configurations : a mapping of configuration key to value
  parameters : a mapping of script parameter key to value
  host_name : the name of this host where the alert is running

  :type configurations dict
  :type parameters dict
  :type host_name str
  """

  if configurations is None:
    return (('UNKNOWN', ['There were no configurations supplied to the script.']))

  uri = None
  http_policy = 'HTTP_ONLY'

  # hdfs-site is required
  if not HDFS_SITE_KEY in configurations:
    return 'SKIPPED', ['{0} is a required parameter for the script'.format(HDFS_SITE_KEY)]

  if NN_HTTP_POLICY_KEY in configurations:
    http_policy = configurations[NN_HTTP_POLICY_KEY]

  if SMOKEUSER_KEY in configurations:
    smokeuser = configurations[SMOKEUSER_KEY]

  executable_paths = None
  if EXECUTABLE_SEARCH_PATHS in configurations:
    executable_paths = configurations[EXECUTABLE_SEARCH_PATHS]

  security_enabled = False
  if SECURITY_ENABLED_KEY in configurations:
    security_enabled = str(configurations[SECURITY_ENABLED_KEY]).upper() == 'TRUE'

  kerberos_keytab = None
  if KERBEROS_KEYTAB in configurations:
    kerberos_keytab = configurations[KERBEROS_KEYTAB]

  kerberos_principal = None
  if KERBEROS_PRINCIPAL in configurations:
    kerberos_principal = configurations[KERBEROS_PRINCIPAL]
    kerberos_principal = kerberos_principal.replace('_HOST', host_name)

  kinit_timer_ms = parameters.get(KERBEROS_KINIT_TIMER_PARAMETER, DEFAULT_KERBEROS_KINIT_TIMER_MS)

  # determine the right URI and whether to use SSL
  hdfs_site = configurations[HDFS_SITE_KEY]

  scheme = "https" if http_policy == "HTTPS_ONLY" else "http"

  nn_addresses = get_all_namenode_addresses(hdfs_site)
  for nn_address in nn_addresses:
    if nn_address.startswith(host_name + ":") or nn_address == host_name:
      uri = nn_address
      break
  if not uri:
    return 'SKIPPED', [
      'NameNode on host {0} not found (namenode adresses = {1})'.format(host_name, ', '.join(nn_addresses))]

  upgrade_finalized_qry = "{0}://{1}/jmx?qry=Hadoop:service=NameNode,name=NameNodeInfo".format(scheme, uri)

  # start out assuming an OK status
  label = None
  result_code = "OK"

  try:
    if kerberos_principal is not None and kerberos_keytab is not None and security_enabled:
      env = Environment.get_instance()

      last_checkpoint_time_response, error_msg, time_millis = curl_krb_request(
        env.tmp_dir, kerberos_keytab,
        kerberos_principal, upgrade_finalized_qry, "upgrade_finalized_state", executable_paths, False,
        "HDFS Upgrade Finalized State", smokeuser, kinit_timer_ms = kinit_timer_ms
       )

      upgrade_finalized_response_json = json.loads(last_checkpoint_time_response)
      upgrade_finalized = bool(upgrade_finalized_response_json["beans"][0]["UpgradeFinalized"])

    else:
      upgrade_finalized = bool(get_value_from_jmx(upgrade_finalized_qry,
                                                    "UpgradeFinalized"))

    if upgrade_finalized:
      label = "HDFS cluster is not in the upgrade state"
      result_code = 'OK'
    else:
      label = "HDFS cluster is not finalized"
      result_code = 'CRITICAL'

  except:
    label = traceback.format_exc()
    result_code = 'UNKNOWN'

  return ((result_code, [label]))

def get_value_from_jmx(query, jmx_property):
  """
   Read property from the jxm endpoint

  :param query: jmx uri path
  :param jmx_property: property name to read
  :return: jmx property value
  
  :type query str
  :type jmx_property str
  """
  response = None

  try:
    response = urllib2.urlopen(query, timeout=int(CONNECTION_TIMEOUT_DEFAULT))
    data = response.read()

    data_dict = json.loads(data)
    return data_dict["beans"][0][jmx_property]
  finally:
    if response is not None:
      try:
        response.close()
      except:
        pass
