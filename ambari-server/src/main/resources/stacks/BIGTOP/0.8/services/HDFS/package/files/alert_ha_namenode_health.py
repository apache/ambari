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

from resource_management.libraries.functions.curl_krb_request import curl_krb_request
from resource_management.core.environment import Environment

RESULT_STATE_OK = 'OK'
RESULT_STATE_CRITICAL = 'CRITICAL'
RESULT_STATE_UNKNOWN = 'UNKNOWN'
RESULT_STATE_SKIPPED = 'SKIPPED'

HDFS_NN_STATE_ACTIVE = 'active'
HDFS_NN_STATE_STANDBY = 'standby'

HDFS_SITE_KEY = '{{hdfs-site}}'
NAMESERVICE_KEY = '{{hdfs-site/dfs.nameservices}}'
NN_HTTP_ADDRESS_KEY = '{{hdfs-site/dfs.namenode.http-address}}'
NN_HTTPS_ADDRESS_KEY = '{{hdfs-site/dfs.namenode.https-address}}'
DFS_POLICY_KEY = '{{hdfs-site/dfs.http.policy}}'

KERBEROS_KEYTAB = '{{hdfs-site/dfs.web.authentication.kerberos.keytab}}'
KERBEROS_PRINCIPAL = '{{hdfs-site/dfs.web.authentication.kerberos.principal}}'
SECURITY_ENABLED_KEY = '{{cluster-env/security_enabled}}'
SMOKEUSER_KEY = '{{cluster-env/smokeuser}}'

CONNECTION_TIMEOUT_KEY = 'connection.timeout'
CONNECTION_TIMEOUT_DEFAULT = 5.0

logger = logging.getLogger()

def get_tokens():
  """
  Returns a tuple of tokens in the format {{site/property}} that will be used
  to build the dictionary passed into execute
  """
  return (HDFS_SITE_KEY, NAMESERVICE_KEY, NN_HTTP_ADDRESS_KEY,
  NN_HTTPS_ADDRESS_KEY, DFS_POLICY_KEY, SMOKEUSER_KEY, KERBEROS_KEYTAB, KERBEROS_PRINCIPAL, SECURITY_ENABLED_KEY)
  

def execute(configurations={}, parameters={}, host_name=None):
  """
  Returns a tuple containing the result code and a pre-formatted result label

  Keyword arguments:
  configurations (dictionary): a mapping of configuration key to value
  parameters (dictionary): a mapping of script parameter key to value
  host_name (string): the name of this host where the alert is running
  """
  if configurations is None:
    return (RESULT_STATE_UNKNOWN, ['There were no configurations supplied to the script.'])

  # if not in HA mode, then SKIP
  if not NAMESERVICE_KEY in configurations:
    return (RESULT_STATE_SKIPPED, ['NameNode HA is not enabled'])

  # hdfs-site is required
  if not HDFS_SITE_KEY in configurations:
    return (RESULT_STATE_UNKNOWN, ['{0} is a required parameter for the script'.format(HDFS_SITE_KEY)])
  
  if SMOKEUSER_KEY in configurations:
    smokeuser = configurations[SMOKEUSER_KEY]

  # parse script arguments
  connection_timeout = CONNECTION_TIMEOUT_DEFAULT
  if CONNECTION_TIMEOUT_KEY in parameters:
    connection_timeout = float(parameters[CONNECTION_TIMEOUT_KEY])

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


  # determine whether or not SSL is enabled
  is_ssl_enabled = False
  if DFS_POLICY_KEY in configurations:
    dfs_policy = configurations[DFS_POLICY_KEY]
    if dfs_policy == "HTTPS_ONLY":
      is_ssl_enabled = True

  name_service = configurations[NAMESERVICE_KEY]
  hdfs_site = configurations[HDFS_SITE_KEY]

  # look for dfs.ha.namenodes.foo
  nn_unique_ids_key = 'dfs.ha.namenodes.' + name_service
  if not nn_unique_ids_key in hdfs_site:
    return (RESULT_STATE_UNKNOWN, ['Unable to find unique namenode alias key {0}'.format(nn_unique_ids_key)])

  namenode_http_fragment = 'dfs.namenode.http-address.{0}.{1}'
  jmx_uri_fragment = "http://{0}/jmx?qry=Hadoop:service=NameNode,name=*"

  if is_ssl_enabled:
    namenode_http_fragment = 'dfs.namenode.https-address.{0}.{1}'
    jmx_uri_fragment = "https://{0}/jmx?qry=Hadoop:service=NameNode,name=*"


  active_namenodes = []
  standby_namenodes = []
  unknown_namenodes = []

  # now we have something like 'nn1,nn2,nn3,nn4'
  # turn it into dfs.namenode.[property].[dfs.nameservices].[nn_unique_id]
  # ie dfs.namenode.http-address.hacluster.nn1
  nn_unique_ids = hdfs_site[nn_unique_ids_key].split(',')
  for nn_unique_id in nn_unique_ids:
    key = namenode_http_fragment.format(name_service,nn_unique_id)

    if key in hdfs_site:
      # use str() to ensure that unicode strings do not have the u' in them
      value = str(hdfs_site[key])

      try:
        jmx_uri = jmx_uri_fragment.format(value)
        if kerberos_principal is not None and kerberos_keytab is not None and security_enabled:
          env = Environment.get_instance()

          # curl requires an integer timeout
          curl_connection_timeout = int(connection_timeout)

          state_response, error_msg, time_millis  = curl_krb_request(env.tmp_dir,
            kerberos_keytab, kerberos_principal, jmx_uri,"ha_nn_health", None, False,
            "NameNode High Availability Health", smokeuser, connection_timeout=curl_connection_timeout)

          state = _get_ha_state_from_json(state_response)
        else:
          state_response = get_jmx(jmx_uri, connection_timeout)
          state = _get_ha_state_from_json(state_response)

        if state == HDFS_NN_STATE_ACTIVE:
          active_namenodes.append(value)
        elif state == HDFS_NN_STATE_STANDBY:
          standby_namenodes.append(value)
        else:
          unknown_namenodes.append(value)
      except:
        unknown_namenodes.append(value)

  # now that the request is done, determine if this host is the host that
  # should report the status of the HA topology
  is_active_namenode = False
  for active_namenode in active_namenodes:
    if active_namenode.startswith(host_name):
      is_active_namenode = True

  # there's only one scenario here; there is exactly 1 active and 1 standby
  is_topology_healthy = len(active_namenodes) == 1 and len(standby_namenodes) == 1

  result_label = 'Active{0}, Standby{1}, Unknown{2}'.format(str(active_namenodes),
    str(standby_namenodes), str(unknown_namenodes))

  # Healthy Topology:
  #   - Active NN reports the alert, standby does not
  #
  # Unhealthy Topology:
  #   - Report the alert if this is the first named host
  #   - Report the alert if not the first named host, but the other host
  #   could not report its status
  if is_topology_healthy:
    if is_active_namenode is True:
      return (RESULT_STATE_OK, [result_label])
    else:
      return (RESULT_STATE_SKIPPED, ['Another host will report this alert'])
  else:
    # dfs.namenode.rpc-address.service.alias is guaranteed in HA mode
    first_listed_host_key = 'dfs.namenode.rpc-address.{0}.{1}'.format(
      name_service, nn_unique_ids[0])

    first_listed_host = ''
    if first_listed_host_key in hdfs_site:
      first_listed_host = hdfs_site[first_listed_host_key]

    is_first_listed_host = False
    if first_listed_host.startswith(host_name):
      is_first_listed_host = True

    if is_first_listed_host:
      return (RESULT_STATE_CRITICAL, [result_label])
    else:
      # not the first listed host, but the first host might be in the unknown
      return (RESULT_STATE_SKIPPED, ['Another host will report this alert'])


def get_jmx(query, connection_timeout):
  response = None
  
  try:
    response = urllib2.urlopen(query, timeout=connection_timeout)
    json_data = response.read()
    return json_data
  finally:
    if response is not None:
      try:
        response.close()
      except:
        pass


def _get_ha_state_from_json(string_json):
  """
  Searches through the specified JSON string looking for either the HDP 2.0 or 2.1+ HA state
  enumerations.
  :param string_json: the string JSON
  :return:  the value of the HA state (active, standby, etc)
  """
  json_data = json.loads(string_json)
  jmx_beans = json_data["beans"]

  # look for HDP 2.1+ first
  for jmx_bean in jmx_beans:
    if "name" not in jmx_bean:
      continue

    jmx_bean_name = jmx_bean["name"]
    if jmx_bean_name == "Hadoop:service=NameNode,name=NameNodeStatus" and "State" in jmx_bean:
      return jmx_bean["State"]

  # look for HDP 2.0 last
  for jmx_bean in jmx_beans:
    if "name" not in jmx_bean:
      continue

    jmx_bean_name = jmx_bean["name"]
    if jmx_bean_name == "Hadoop:service=NameNode,name=FSNamesystem":
      return jmx_bean["tag.HAState"]
