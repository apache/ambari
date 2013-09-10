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
from ambari_client.model.base_model import  BaseModel , ModelList
from ambari_client.model  import paths , utils , status



LOG = logging.getLogger(__name__)


def get_host_components(resource_root, cluster_name , host_name):
  path = paths.HOSTS_COMPONENTS_PATH % (cluster_name, host_name)
  dic = resource_root.get(path)
  return utils.ModelUtils.get_model_list(ModelList, ComponentModel, dic, resource_root , "HostRoles")


def get_host_component(resource_root, cluster_name , host_name , component_name):
  path = paths.HOSTS_COMPONENT_PATH % (cluster_name, host_name , component_name)
  dic = resource_root.get(path)
  comp_model = utils.ModelUtils.create_model(ComponentModel, dic, resource_root , "HostRoles" , status.StatusModel)
  #comp_model._setattr('host_name', dic["items"][0]['HostRoles']['host_name'])
  return comp_model


def _get_service_components(resource_root, cluster_name , service_name):
  path = paths.SERVICE_COMPONENTS_PATH % (cluster_name, service_name)
  dic = resource_root.get(path)
  return utils.ModelUtils.get_model_list(ModelList, ComponentModel, dic, resource_root , "ServiceComponentInfo")


def _get_service_component(resource_root, cluster_name , service_name , component_name):
  path = paths.SERVICE_COMPONENT_PATH % (cluster_name, service_name , component_name)
  dic = resource_root.get(path)
  return utils.ModelUtils.create_model(ComponentModel, dic, resource_root , "ServiceComponentInfo")








class ComponentModel(BaseModel):
  """
  The ComponentModel class
  """
  RO_ATTR = ('stack_id',)
  RW_ATTR = ('host_name', 'component_name', 'service_name', 'state')
  REF_ATTR = ('cluster_name',)
  
  def __init__(self, resource_root, component_name, host_name=None , service_name=None , state=None):
    utils.retain_self_helper(BaseModel, **locals())

  def __str__(self):
    return "<<ComponentModel>> component_name = %s; state = %s" % (self.component_name, self.state)
  
  def _get_cluster_name(self):
    if self.clusterRef:
      return self.clusterRef.cluster_name
    return None

  def _path(self):
    """
    Return the API path for this service.
    """
    if self._get_cluster_name():
      return paths.HOSTS_COMPONENTS_PATH % (self._get_cluster_name(), self.host_name)
    else:
      return ''

  def get_metrics(self, detail=None):
    """
    Gets the json containing all the metrics data
    @return: json.
    """
    if self.service_name:
      metricpath = paths.SERVICE_COMPONENT_PATH % (self._get_cluster_name(), self.service_name , self.component_name) + "?fields=metrics"
    elif self.host_name:
      metricpath = paths.HOSTS_COMPONENT_PATH % (self._get_cluster_name(), self.host_name , self.component_name) + "?fields=metrics"
    metricjson = self._get_resource_root().get(metricpath)
    return metricjson
