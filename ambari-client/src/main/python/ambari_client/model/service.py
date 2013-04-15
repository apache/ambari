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
'''



'''
import logging

from ambari_client.model.base_model import  BaseModel 
from ambari_client.model.paths import SERVICES_PATH ,SERVICE_PATH 
from ambari_client.model.utils import ModelUtils ,retain_self_helper

LOG = logging.getLogger(__name__)


def get_all_services(resource_root, cluster_name="default"):
  path = SERVICES_PATH % (cluster_name,)
  path = path +'?fields=*'
  dic = resource_root.get(path)
  return ModelUtils.get_model_list(ServiceModel, dic, resource_root ,"ServiceInfo")


def get_service(resource_root, service_name, cluster_name="default"):
  path = "%s/%s" % (SERVICES_PATH % (cluster_name,), service_name)
  dic = resource_root.get(path)
  return ModelUtils.create_model(ServiceModel ,dic, resource_root,"ServiceInfo") 


    
    
    
class ServiceModel(BaseModel):
  RO_ATTR = ('state',  'cluster_name')
  RW_ATTR = ('service_name', 'type')
  REF_ATTR = ('cluster_name',)

  def __init__(self, resource_root, service_name):
    #BaseModel.__init__(self, **locals())
    retain_self_helper(**locals())

  def __str__(self):
    return "<<ServiceModel>> = %s (cluster_name = %s)" % (self.service_name, self._get_cluster_name())

  def _get_cluster_name(self):
    if self.clusterRef:
      return self.clusterRef.cluster_name
    return None

  def _path(self):
    """
    Return the API path for this service.
    """
    if self._get_cluster_name():
      return SERVICE_PATH % (self._get_cluster_name(), self.service_name)
    else:
      return ''


  def _action(self, data=None):
    path = self._path() 
    self._get_resource_root().put(path, payload=data)
    return None


  def start(self):
    """
    Start a service.
    """
    data={"ServiceInfo": {"state": "STARTED"}}
    return self._action(data)

  def stop(self):
    """
    Stop a service.
    """
    data={"ServiceInfo": {"state": "INSTALLED"}}
    return self._action(data)


