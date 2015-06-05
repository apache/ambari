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
from resource_management.libraries.script import UnknownConfiguration
from resource_management.libraries.functions.is_empty import is_empty
from resource_management.libraries.functions.format import format
from resource_management.libraries.functions.jmx import get_value_from_jmx
from resource_management.core.base import Fail
__all__ = ["get_namenode_states", "get_active_namenode", "get_property_for_active_namenode"]

HDFS_NN_STATE_ACTIVE = 'active'
HDFS_NN_STATE_STANDBY = 'standby'

NAMENODE_HTTP_FRAGMENT = 'dfs.namenode.http-address.{0}.{1}'
NAMENODE_HTTPS_FRAGMENT = 'dfs.namenode.https-address.{0}.{1}'
JMX_URI_FRAGMENT = "{0}://{1}/jmx?qry=Hadoop:service=NameNode,name=NameNodeStatus"
  
def get_namenode_states(hdfs_site, security_enabled, run_user):
  """
  return format [('nn1', 'hdfs://hostname1:port1'), ('nn2', 'hdfs://hostname2:port2')] , [....], [....]
  """
  active_namenodes = []
  standby_namenodes = []
  unknown_namenodes = []
  
  name_service = hdfs_site['dfs.nameservices']
  nn_unique_ids_key = 'dfs.ha.namenodes.' + name_service

  # now we have something like 'nn1,nn2,nn3,nn4'
  # turn it into dfs.namenode.[property].[dfs.nameservices].[nn_unique_id]
  # ie dfs.namenode.http-address.hacluster.nn1
  nn_unique_ids = hdfs_site[nn_unique_ids_key].split(',')
  for nn_unique_id in nn_unique_ids:
    is_https_enabled = hdfs_site['dfs.https.enable'] if not is_empty(hdfs_site['dfs.https.enable']) else False
    
    if not is_https_enabled:
      key = NAMENODE_HTTP_FRAGMENT.format(name_service,nn_unique_id)
      protocol = "http"
    else:
      key = NAMENODE_HTTPS_FRAGMENT.format(name_service,nn_unique_id)
      protocol = "https"
    
    if key in hdfs_site:
      # use str() to ensure that unicode strings do not have the u' in them
      value = str(hdfs_site[key])

      jmx_uri = JMX_URI_FRAGMENT.format(protocol, value)
      
      state = get_value_from_jmx(jmx_uri, 'State', security_enabled, run_user, is_https_enabled)
      
      if state == HDFS_NN_STATE_ACTIVE:
        active_namenodes.append((nn_unique_id, value))
      elif state == HDFS_NN_STATE_STANDBY:
        standby_namenodes.append((nn_unique_id, value))
      else:
        unknown_namenodes.append((nn_unique_id, value))
        
  return active_namenodes, standby_namenodes, unknown_namenodes

def is_ha_enabled(hdfs_site):
  dfs_ha_nameservices = hdfs_site['dfs.nameservices']
  
  if is_empty(dfs_ha_nameservices):
    return False
  
  dfs_ha_namenode_ids = hdfs_site[format("dfs.ha.namenodes.{dfs_ha_nameservices}")]
  
  if not is_empty(dfs_ha_namenode_ids):
    dfs_ha_namemodes_ids_list = dfs_ha_namenode_ids.split(",")
    dfs_ha_namenode_ids_array_len = len(dfs_ha_namemodes_ids_list)
    if dfs_ha_namenode_ids_array_len > 1:
      return True
      
  return False

def get_active_namenode(hdfs_site, security_enabled, run_user):
  """
  return format is nn_unique_id and it's address ('nn1', 'hdfs://hostname1:port1')
  """
  active_namenodes = get_namenode_states(hdfs_site, security_enabled, run_user)[0]
  if active_namenodes:
    return active_namenodes[0]
  else:
    return UnknownConfiguration('fs_root')
  
def get_property_for_active_namenode(hdfs_site, property_name, security_enabled, run_user):
  """
  For dfs.namenode.rpc-address:
    - In non-ha mode it will return hdfs_site[dfs.namenode.rpc-address]
    - In ha-mode it will return hdfs_site[dfs.namenode.rpc-address.nnha.nn2], where nnha is the name of HA, and nn2 is id of active NN
  """
  if is_ha_enabled(hdfs_site):
    name_service = hdfs_site['dfs.nameservices']
    active_namenodes = get_namenode_states(hdfs_site, security_enabled, run_user)[0]
    
    if not len(active_namenodes):
      raise Fail("There is no active namenodes.")
    
    active_namenode_id = active_namenodes[0][0]
    
    return hdfs_site[format("{property_name}.{name_service}.{active_namenode_id}")]
  else:
    return hdfs_site[property_name]
  
