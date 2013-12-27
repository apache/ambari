#
#  Licensed to the Apache Software Foundation (ASF) under one
#  or more contributor license agreements.  See the NOTICE file
#  distributed with this work for additional information
#  regarding copyright ownership.  The ASF licenses this file
#  to you under the Apache License, Version 2.0 (the
#  "License"); you may not use this file except in compliance
#  with the License.  You may obtain a copy of the License at
# 
#      http://www.apache.org/licenses/LICENSE-2.0
# 
#  Unless required by applicable law or agreed to in writing, software
#  distributed under the License is distributed on an "AS IS" BASIS,
#  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#  See the License for the specific language governing permissions and
#  limitations under the License.

from ambari_client.model import  host
__docformat__ = "epytext"

def _add_hosts(root_resource, cluster_name , host_list):
  """
  Adds a hosts to a cluster.
  @param root_resource: The root Resource object.
  @param cluster_name: Cluster name
  @param host_list: list of hosts
  @return: A StatusModel object
  """
  return _add_hosts(root_resource, cluster_name , host_list)

def _add_host(root_resource, cluster_name , host_name , ip, rack_info=None):
  """
  Adds a host to a cluster.
  @param host_name: Host name
  @param ip: ip of Host 
  @param rack_info: rack information
  @return: StatusModel.
  """
  return _add_host(root_resource, cluster_name , host_name , ip, rack_info)


def _create_hosts(root_resource, host_list):
  """
  Create a host
  @param root_resource: The root Resource.
  @param host_list: ModelList list of hosts
  @return: A HostModel object
  """
  return host._create_hosts(root_resource, host_list)

def _create_host(root_resource, host_name, ip, rack_info=None):
  """
  Create a host
  @param root_resource: The root Resource.
  @param host_name: Host name
  @param ip: IP address
  @param rack_info: Rack id. Default None
  @return: A HostModel object
  """
  return host._create_host(root_resource, host_name, ip, rack_info)


def _get_host(root_resource, host_name):
  """
  Lookup a host by name
  @param root_resource: The root Resource.
  @param host_name: Host name
  @return: A HostModel object
  """
  return host._get_host(root_resource, host_name)

def _get_cluster_host(root_resource, cluster_name, host_name):
  """
  Lookup a host by name
  @param root_resource: The root Resource.
  @param host_name: Host name
  @return: A HostModel object
  """
  return host._get_cluster_host(root_resource, cluster_name, host_name)




def _get_all_hosts(root_resource):
  """
  Get all hosts
  @param root_resource: The root Resource.
  @return: A list of HostModel objects.
  """
  return host._get_all_hosts(root_resource)

def _get_all_cluster_hosts(root_resource, cluster_name):
  """
  Get all cluster hosts
  @param root_resource: The root Resource.
  @return: A list of HostModel objects.
  """
  return host._get_all_cluster_hosts(root_resource, cluster_name)


def _delete_host(root_resource, host_name):
  """
  Delete a host by id
  @param root_resource: The root Resource.
  @param host_name: Host name
  @return: The deleted HostModel object
  """
  return host._delete_host(root_resource, host_name)

def _delete_cluster_host(root_resource, cluster_name , host_name):
  """
  Delete a cluster host by id
  @param root_resource: The root Resource.
  @param host_name: Host name
  @return: The deleted HostModel object
  """
  return host._delete_cluster_host(root_resource, cluster_name , host_name)


def _bootstrap_hosts(root_resource , hosts_list , ssh_key):
  """
  Bootstrap hosts.
  @param hosts_list: list of host_names.
  @param ssh_key: ssh key for password-less access.
  @return: A  StatusModel object.
  """
  return host._bootstrap_hosts(root_resource, hosts_list , ssh_key)
