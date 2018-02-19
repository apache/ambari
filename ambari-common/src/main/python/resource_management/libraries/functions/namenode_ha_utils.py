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
from resource_management.libraries.functions.is_empty import is_empty
from resource_management.libraries.functions.format import format
from resource_management.libraries.functions.jmx import get_value_from_jmx
from resource_management.core.base import Fail
from resource_management.core import shell
from resource_management.core.logger import Logger
from resource_management.libraries.functions.decorator import retry
from resource_management.libraries.functions.hdfs_utils import is_https_enabled_in_hdfs


__all__ = ["get_namenode_states", "get_active_namenode",
           "get_property_for_active_namenode", "get_nameservices"]

HDFS_NN_STATE_ACTIVE = 'active'
HDFS_NN_STATE_STANDBY = 'standby'

NAMENODE_HTTP_NON_HA = 'dfs.namenode.http-address'
NAMENODE_HTTPS_NON_HA = 'dfs.namenode.https-address'
DFS_HTTP_POLICY = "dfs.http.policy"

NAMENODE_HTTP_FRAGMENT = 'dfs.namenode.http-address.{0}.{1}'
NAMENODE_HTTPS_FRAGMENT = 'dfs.namenode.https-address.{0}.{1}'
NAMENODE_RPC_FRAGMENT = 'dfs.namenode.rpc-address.{0}.{1}'
NAMENODE_RPC_NON_HA = 'dfs.namenode.rpc-address'
JMX_URI_FRAGMENT = "{0}://{1}/jmx?qry=Hadoop:service=NameNode,name=FSNamesystem"
INADDR_ANY = '0.0.0.0'

def get_namenode_states(hdfs_site, security_enabled, run_user, times=10, sleep_time=1, backoff_factor=2):
  """
  return format [('nn1', 'hdfs://hostname1:port1'), ('nn2', 'hdfs://hostname2:port2')] , [....], [....]
  """
  @retry(times=times, sleep_time=sleep_time, backoff_factor=backoff_factor, err_class=Fail)
  def doRetries(hdfs_site, security_enabled, run_user):
    doRetries.attempt += 1
    active_namenodes, standby_namenodes, unknown_namenodes = get_namenode_states_noretries(hdfs_site, security_enabled, run_user, doRetries.attempt == times)
    Logger.info(
      "NameNode HA states: active_namenodes = {0}, standby_namenodes = {1}, unknown_namenodes = {2}".format(
        active_namenodes, standby_namenodes, unknown_namenodes))
    if active_namenodes:
      return active_namenodes, standby_namenodes, unknown_namenodes
    elif doRetries.attempt == times:
      Logger.warning("No active NameNode was found after {0} retries. Will return current NameNode HA states".format(times))
      return active_namenodes, standby_namenodes, unknown_namenodes
    raise Fail('No active NameNode was found.')

  doRetries.attempt = 0
  return doRetries(hdfs_site, security_enabled, run_user)


def get_namenode_states_noretries(hdfs_site, security_enabled, run_user, last_retry=True):
  """
  returns data for all name nodes of all name services
  """
  active_namenodes = []
  standby_namenodes = []
  unknown_namenodes = []

  name_services = get_nameservices(hdfs_site)
  for name_service in name_services:
    active, standby, unknown = _get_namenode_states_noretries_single_ns(hdfs_site, name_service, security_enabled, run_user, last_retry)
    active_namenodes += active
    standby_namenodes += standby
    unknown_namenodes += unknown
  return active_namenodes, standby_namenodes, unknown_namenodes


def _get_namenode_states_noretries_single_ns(hdfs_site, name_service, security_enabled, run_user, last_retry=True):
  """
  return format [('nn1', 'hdfs://hostname1:port1'), ('nn2', 'hdfs://hostname2:port2')] , [....], [....]
  """
  active_namenodes = []
  standby_namenodes = []
  unknown_namenodes = []

  nn_unique_ids_key = 'dfs.ha.namenodes.' + name_service

  # now we have something like 'nn1,nn2,nn3,nn4'
  # turn it into dfs.namenode.[property].[dfs.nameservices].[nn_unique_id]
  # ie dfs.namenode.http-address.hacluster.nn1
  nn_unique_ids = hdfs_site[nn_unique_ids_key].split(',')
  for nn_unique_id in nn_unique_ids:
    is_https_enabled = is_https_enabled_in_hdfs(hdfs_site['dfs.http.policy'], hdfs_site['dfs.https.enable'])

    rpc_key = NAMENODE_RPC_FRAGMENT.format(name_service,nn_unique_id)
    if not is_https_enabled:
      key = NAMENODE_HTTP_FRAGMENT.format(name_service,nn_unique_id)
      protocol = "http"
    else:
      key = NAMENODE_HTTPS_FRAGMENT.format(name_service,nn_unique_id)
      protocol = "https"

    if key in hdfs_site:
      # use str() to ensure that unicode strings do not have the u' in them
      value = str(hdfs_site[key])
      if INADDR_ANY in value and rpc_key in hdfs_site:
        rpc_value = str(hdfs_site[rpc_key])
        if INADDR_ANY not in rpc_value:
          rpc_host = rpc_value.split(":")[0]
          value = value.replace(INADDR_ANY, rpc_host)

      jmx_uri = JMX_URI_FRAGMENT.format(protocol, value)

      state = get_value_from_jmx(jmx_uri, 'tag.HAState', security_enabled, run_user, is_https_enabled, last_retry)
      # If JMX parsing failed
      if not state:
        check_service_cmd = "hdfs haadmin -ns {0} -getServiceState {1}".format(name_service, nn_unique_id)
        code, out = shell.call(check_service_cmd, logoutput=True, user=run_user)
        if code == 0 and out:
          if HDFS_NN_STATE_STANDBY in out:
            state = HDFS_NN_STATE_STANDBY
          elif HDFS_NN_STATE_ACTIVE in out:
            state = HDFS_NN_STATE_ACTIVE

      if state == HDFS_NN_STATE_ACTIVE:
        active_namenodes.append((nn_unique_id, value))
      elif state == HDFS_NN_STATE_STANDBY:
        standby_namenodes.append((nn_unique_id, value))
      else:
        unknown_namenodes.append((nn_unique_id, value))

  return active_namenodes, standby_namenodes, unknown_namenodes


def _is_ha_config(hdfs_site):
  """
  returns True if an HA config is used
  """
  name_services = hdfs_site.get('dfs.nameservices', None)
  if name_services:
    for ns in name_services.split(","):
      if hdfs_site.get('dfs.ha.namenodes.'+ns):
        return True
  return False


def get_active_namenode(hdfs_site, security_enabled, run_user):
  """
  return format is nn_unique_id and it's address ('nn1', 'hdfs://hostname1:port1')
  """
  active_namenodes = get_namenode_states(hdfs_site, security_enabled, run_user)[0]
  if active_namenodes:
    return active_namenodes[0]

  raise Fail('No active NameNode was found.')

def get_property_for_active_namenode(hdfs_site, property_name, security_enabled, run_user):
  """
  For dfs.namenode.rpc-address:
    - In non-ha mode it will return hdfs_site[dfs.namenode.rpc-address]
    - In ha-mode it will return hdfs_site[dfs.namenode.rpc-address.nnha.nn2], where nnha is the name of HA, and nn2 is id of active NN
    - In federated mode it fails since there is more than one active namenode
  """
  value = None
  rpc_key = None
  if _is_ha_config(hdfs_site):
    name_services = get_nameservices(hdfs_site)
    if len(name_services) > 1:
      raise Fail('Multiple name services not supported by this function')
    name_service = name_services(hdfs_site)[0]
    active_namenodes = get_namenode_states(hdfs_site, security_enabled, run_user)[0]

    if not len(active_namenodes):
      raise Fail("There is no active namenodes.")

    active_namenode_id = active_namenodes[0][0]
    value = hdfs_site[format("{property_name}.{name_service}.{active_namenode_id}")]
    rpc_key = NAMENODE_RPC_FRAGMENT.format(name_service,active_namenode_id)
  else:
    value = hdfs_site[property_name]
    rpc_key = NAMENODE_RPC_NON_HA

  if INADDR_ANY in value and rpc_key in hdfs_site:
    rpc_value = str(hdfs_site[rpc_key])
    if INADDR_ANY not in rpc_value:
      rpc_host = rpc_value.split(":")[0]
      value = value.replace(INADDR_ANY, rpc_host)

  return value

def get_all_namenode_addresses(hdfs_site):
  """
  - In non-ha mode it will return list of hdfs_site[dfs.namenode.http[s]-address]
  - In ha-mode it will return list of hdfs_site[dfs.namenode.http-address.NS.Uid], where NS is the name of HA, and Uid is id of NameNode
  - In federated mode it will return all namenodes for internal name services
  """
  nn_addresses = []
  name_services = get_nameservices(hdfs_site)
  if not name_services:
    name_services = [None] #fall back to config handling without name services
  for ns in name_services:
    nn_addresses += _get_all_namenode_addresses_single_ns(hdfs_site, ns)
  return nn_addresses

def _get_all_namenode_addresses_single_ns(hdfs_site, name_service):
  nn_addresses = []
  http_policy = 'HTTP_ONLY'

  if DFS_HTTP_POLICY in hdfs_site:
    http_policy = hdfs_site[DFS_HTTP_POLICY]

  if _is_ha_config(hdfs_site):
    nn_unique_ids_key = 'dfs.ha.namenodes.' + name_service
    nn_unique_ids = hdfs_site[nn_unique_ids_key].split(',')
    for nn_unique_id in nn_unique_ids:
      rpc_key = NAMENODE_RPC_FRAGMENT.format(name_service,nn_unique_id)
      if http_policy == 'HTTPS_ONLY':
        key = NAMENODE_HTTPS_FRAGMENT.format(name_service,nn_unique_id)
      else:
        key = NAMENODE_HTTP_FRAGMENT.format(name_service,nn_unique_id)
      if key in hdfs_site:
        # use str() to ensure that unicode strings do not have the u' in them
        value = str(hdfs_site[key])
        if INADDR_ANY in value and rpc_key in hdfs_site:
          rpc_value = str(hdfs_site[rpc_key])
          if INADDR_ANY not in rpc_value:
            rpc_host = rpc_value.split(":")[0]
            value = value.replace(INADDR_ANY, rpc_host)

        if not value in nn_addresses:
          nn_addresses.append(value)
  else:
    if http_policy == 'HTTPS_ONLY':
      if NAMENODE_HTTPS_NON_HA in hdfs_site:
        nn_addresses.append(hdfs_site[NAMENODE_HTTPS_NON_HA])
    else:
      if NAMENODE_HTTP_NON_HA in hdfs_site:
        nn_addresses.append(hdfs_site[NAMENODE_HTTP_NON_HA])

  return nn_addresses

def get_nameservices(hdfs_site):
  """
  Multiple nameservices can be configured for example to support seamless distcp
  between two HA clusters. The nameservices are defined as a comma separated
  list in hdfs_site['dfs.nameservices']. The parameter
  hdfs['dfs.internal.nameservices'] was introduced in Hadoop 2.6 to denote the
  nameservice for the current cluster (HDFS-6376).
  In federated mode multiple name services will be returned.

  This method uses hdfs['dfs.internal.nameservices'] to get the current
  nameservice(s), if that parameter is not available it tries to splits the value
  in hdfs_site['dfs.nameservices'] returning the first string or what is
  contained in hdfs_site['dfs.namenode.shared.edits.dir'].

  By default hdfs_site['dfs.nameservices'] is returned.
  :param hdfs_site:
  :return: list of string or an empty list
  """

  name_services_param = hdfs_site.get('dfs.internal.nameservices', None) #in Federated mode this can be a list
  if name_services_param:
    name_services = name_services_param.split(",")
    return name_services

  name_services_string = hdfs_site.get('dfs.nameservices', None)

  if name_services_string and ',' in name_services_string:
    import re
    for ns in name_services_string.split(","):
      if 'dfs.namenode.shared.edits.dir' in hdfs_site and re.match(r'.*%s$' % ns, hdfs_site['dfs.namenode.shared.edits.dir']): # better would be core_site['fs.defaultFS'] but it's not available
        return [ns]
    return [name_services_string.split(",")[0]] # default to return the first nameservice
  return []
