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

import logging
from ambari_client.core.http_client import HttpClient
from ambari_client.resources import  clusters , hosts , stacks
from ambari_client.core.rest_resource import RestResource

__docformat__ = "epytext"

LOG = logging.getLogger(__name__)


API_VERSION = 1




class AmbariClient(RestResource):
  """
  AmbariClient top-level root resources.
  """

  def __init__(self, host_name, port=None, user_name="admin", password="admin", use_https = False,
               version=API_VERSION , client=None):
    """
    Creates a RestResource object.

    @param host_name: The hostname  server.
    @param port: The port of the server. 
    @param user_name: Login name.
    @param password: Login password.
    @param version: API version.
    @return RestResource object referring to the root.
    """
    
    self._version = version
    
    if use_https:
      protocol = "https"
      if port is None: 
        port = 8443
    else:
      protocol = "http"
      if port is None: 
        port = 8080
      

      
    host_url = "%s://%s:%s/api/v%s" % (protocol, host_name, port, version)
    if client is None:
        client = HttpClient(host_url, user_name , password)
    RestResource.__init__(self, client)



  @property
  def version(self):
    """
    Returns the API version .
    """
    return self._version


  def get_all_clusters(self):
    """
    Get all clusters.
    @return : A ModelList of ClusterModel.
    """
    return clusters._get_all_clusters(self)


  def get_cluster(self, cluster_name):
    """
    Get a cluster by cluster_name.

    @param cluster_name : Cluster's cluster_name.
    @return : An ClusterModel.
    """
    return clusters._get_cluster(self, cluster_name)
  
  def get_host(self, host_name):
    """
    Lookup a host by name
    @param root_resource: The root Resource.
    @param host_name: Host name
    @return: A HostModel object
    """
    return hosts._get_host(self, host_name)


  def get_all_hosts(self):
    """
    Get all hosts in the Data Center
    @return: A ModelList of HostModel objects.
    """
    return hosts._get_all_hosts(self)


  def get_request_status(self , request_id):
    """
    Get request status
    @param request_id : request id for the request
    @return: A  StatusModel object.
    """
    return "TODO"


  def bootstrap_hosts(self , hosts_list , ssh_key=None):
    """
    Bootstrap hosts.
    @param hosts_list :list of host_names.
    @param ssh_key : ssh key for password-less access
    @return: A  StatusModel object.
    """
    return hosts._bootstrap_hosts(self, hosts_list , ssh_key)


  def create_cluster(self, cluster_name, version):
    """
    Create a new cluster.
    @param cluster_name: Cluster name.
    @param version : HDP version.
    @return  ClusterModel object.
    """
    return clusters._create_cluster(self, cluster_name, version)

  def delete_cluster(self , cluster_name):
    """
    Delete a cluster
    @param cluster_name: Cluster to be deleted
    """
    return clusters._delete_cluster(self, cluster_name)


  def delete_host(self , host_name):
    """
    Delete a cluster
    @param host_name: host to be deleted
    """
    return hosts._delete_host(self, host_name)


  def get_config(self, version, service_name):
    """
    get configurations from stack
    @param version: The HDP version.
    @param service_name: service name
    @return: A ConfigModel object
    """
    return stacks._get_config(self, version, service_name)


  def get_components(self, version, service_name):
    """
    get components from stack
    @param version: The HDP version.
    @param service_name: service name
    @return: A ConfigModel object
    """
    return stacks._get_components(self, version, service_name)




def get_root_resource(server_host, server_port=None, username="admin", password="admin",
                       version=1):
  """
   AmbariClient.
  """
  return AmbariClient(server_host, server_port, username, password, version)
