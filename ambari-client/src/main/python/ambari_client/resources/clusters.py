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

from ambari_client.model import  cluster
__docformat__ = "epytext"


def _get_cluster(resource_root, cluster_name):
  """
  Get a cluster by cluster_name
  @param resource_root: The root Resource.
  @param cluster_name: Cluster's name
  @return: ClusterModel object
  """
  return cluster._get_cluster(resource_root, cluster_name)


def _get_all_clusters(root_resource):
  """
  Get all clusters in Ambari.
  @param root_resource: The root Resource object.
  @return: A list of ClusterModel objects in ModelList.
  """
  return cluster._get_all_clusters(root_resource)

 
def _create_cluster(root_resource, cluster_name, version):
  """
  Create a cluster
  @param root_resource: The root Resource.
  @param cluster_name: Cluster cluster_name
  @param version: HDP version
  @return: An ClusterModel object
  """
  return cluster._create_cluster(root_resource, cluster_name, version)
  
  
def _delete_cluster(root_resource, cluster_name):
  """
  Create a cluster
  @param root_resource: The root Resource.
  @param cluster_name: Cluster cluster_name
  """
  return cluster._delete_cluster(root_resource, cluster_name)
