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
import time
from ambari_client.model.base_model import  BaseModel 
from ambari_client.model.paths import CLUSTERS_PATH 
from ambari_client.model import service ,host
from ambari_client.model.utils import ModelUtils ,retain_self_helper


LOG = logging.getLogger(__name__)


def get_cluster(resource_root, cluster_name):
  """
  Lookup a cluster by cluster_name
  @param resource_root: The root Resource .
  @param cluster_name: cluster_name
  @return: A ClusterModel object
  """
  dic = resource_root.get("%s/%s" % (CLUSTERS_PATH, cluster_name))
  return ModelUtils.create_model(ClusterModel ,dic, resource_root,"Clusters")

def get_all_clusters(root_resource, details=None):
  """
  Get all clusters
  @param root_resource: The root Resource .
  @return: A list of ClusterModel objects.
  """
  dic = root_resource.get(CLUSTERS_PATH)
  return ModelUtils.get_model_list(ClusterModel, dic, root_resource ,"Clusters")



def create_cluster(root_resource, cluster_name, version):
  """
  Create a cluster
  @param root_resource: The root Resource.
  @param cluster_name: Cluster cluster_name
  @param version: HDP version
  @return: An ClusterModel object
  """
  data={"Clusters":{"version":str(version)}}
  cluster = ClusterModel(root_resource, cluster_name, version)
  path = CLUSTERS_PATH+"/%s" % (cluster_name)
  root_resource.post(path=path , payload=data)
  return get_cluster(root_resource, cluster_name)

def delete_cluster(root_resource, cluster_name):
  """
  Delete a cluster by name
  @param root_resource: The root Resource object.
  @param name: Cluster name
  """
  root_resource.delete("%s/%s" % (CLUSTERS_PATH, cluster_name))
  time.sleep(3)
  return None

    
class ClusterModel(BaseModel):

  RW_ATTR = ('cluster_name', 'version')
 
  
  def __init__(self, resource_root, cluster_name, version):
    retain_self_helper(**locals())

  def __str__(self):
    return "<<ClusterModel>> = %s; version = %s" % (self.cluster_name, self.version)

  def _path(self):
    return "%s/%s" % (CLUSTERS_PATH, self.cluster_name)

  def get_service(self, service_name):
    """
    Get a service by service_name.
    @param service_name: Service service_name
    @return: A ServiceModel object
    """
    return service.get_service(self._get_resource_root(), service_name, self.cluster_name)

  def get_all_services(self, detail = None):
    """
    Get all services in this cluster.
    @return: A list of ServiceModel objects.
    """
    return service.get_all_services(self._get_resource_root(), self.cluster_name)

  def get_all_hosts(self, detail = None):
    """
    Get all hosts in this cluster.
    @return: A list of HostModel objects.
    """
    return host.get_all_cluster_hosts(self._get_resource_root(), self.cluster_name)


  def get_host(self, hostname , detail = None):
    """
    Get a specific hosts in this cluster.
    @return: A HostModel object.
    """
    return host.get_host(self._get_resource_root(), self.cluster_name, hostname)

class ClusterModelRef(BaseModel):
  RW_ATTR = ('cluster_name',)
  def __init__(self, resource_root, cluster_name = None):
    retain_self_helper(**locals())
