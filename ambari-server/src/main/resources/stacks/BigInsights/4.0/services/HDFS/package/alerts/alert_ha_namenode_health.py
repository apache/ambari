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
import json

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

def get_tokens():
  """
  Returns a tuple of tokens in the format {{site/property}} that will be used
  to build the dictionary passed into execute
  """
  return (HDFS_SITE_KEY, NAMESERVICE_KEY, NN_HTTP_ADDRESS_KEY,
  NN_HTTPS_ADDRESS_KEY, DFS_POLICY_KEY)


def execute(parameters=None, host_name=None):
  """
  Returns a tuple containing the result code and a pre-formatted result label

  Keyword arguments:
  parameters (dictionary): a mapping of parameter key to value
  host_name (string): the name of this host where the alert is running
  """
  if parameters is None:
    return (RESULT_STATE_UNKNOWN, ['There were no parameters supplied to the script.'])

  # if not in HA mode, then SKIP
  if not NAMESERVICE_KEY in parameters:
    return (RESULT_STATE_SKIPPED, ['NameNode HA is not enabled'])

  # hdfs-site is required
  if not HDFS_SITE_KEY in parameters:
    return (RESULT_STATE_UNKNOWN, ['{0} is a required parameter for the script'.format(HDFS_SITE_KEY)])

  # determine whether or not SSL is enabled
  is_ssl_enabled = False
  if DFS_POLICY_KEY in parameters:
    dfs_policy = parameters[DFS_POLICY_KEY]
    if dfs_policy == "HTTPS_ONLY":
      is_ssl_enabled = True

  name_service = parameters[NAMESERVICE_KEY]
  hdfs_site = parameters[HDFS_SITE_KEY]

  # look for dfs.ha.namenodes.foo
  nn_unique_ids_key = 'dfs.ha.namenodes.' + name_service
  if not nn_unique_ids_key in hdfs_site:
    return (RESULT_STATE_UNKNOWN, ['Unable to find unique namenode alias key {0}'.format(nn_unique_ids_key)])

  namenode_http_fragment = 'dfs.namenode.http-address.{0}.{1}'
  jmx_uri_fragment = "http://{0}/jmx?qry=Hadoop:service=NameNode,name=NameNodeStatus"

  if is_ssl_enabled:
    namenode_http_fragment = 'dfs.namenode.https-address.{0}.{1}'
    jmx_uri_fragment = "https://{0}/jmx?qry=Hadoop:service=NameNode,name=NameNodeStatus"


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
        state = get_value_from_jmx(jmx_uri,'State')

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


def get_value_from_jmx(query, jmx_property):
  response = None

  try:
    response = urllib2.urlopen(query)
    data = response.read()

    data_dict = json.loads(data)
    return data_dict["beans"][0][jmx_property]
  finally:
    if response is not None:
      try:
        response.close()
      except:
        pass
