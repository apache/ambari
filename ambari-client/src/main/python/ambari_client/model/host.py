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

try:
  import json
except ImportError:
  import simplejson as json
import logging
from ambari_client.model.base_model import  BaseModel , ModelList
from ambari_client.model import status , component , paths , utils



LOG = logging.getLogger(__name__)


def _create_host(root_resource, host_name, ip, rack_info=None):
  """
  Create a host
  @param root_resource: The root Resource.
  @param host_name: Host name
  @param ip: IP address
  @param rack_info: Rack id. Default None
  @return: An HostModel object
  """
  host = HostModel(root_resource, host_name, ip, rack_info)
  host_list = ModelList([host])
  body = json.dumps(host_list.to_json_dict())
  resp = root_resource.post(paths.HOSTS_PATH, data=body)
  # The server returns a created hosts 
  return _get_host(root_resource, host_name)


def _get_host(root_resource, cluster_name , host_name):
  """
  Lookup up by host_name
  @param root_resource: The root Resource object.
  @param cluster_name: Cluster name
  @param host_name: Host name
  @return: A HostModel object
  """
  path = paths.CLUSTER_HOST_PATH % (cluster_name, host_name)
  dic = root_resource.get(path)
  return utils.ModelUtils.create_model(HostModel , dic, root_resource, "Hosts") 


def _add_hosts(root_resource, cluster_name , host_list):
  """
  Lookup up by host_name
  @param root_resource: The root Resource object.
  @param cluster_name: Cluster name
  @param host_list: list of hostnames
  @return: A StatusModel object
  """
  cpath = paths.HOSTS_CREATE_PATH % (cluster_name)
  data = [{"Hosts":{"host_name":x}} for x in host_list]
  resp = root_resource.post(path=cpath, payload=data)
  return utils.ModelUtils.create_model(status.StatusModel, resp, root_resource, "NO_KEY")


def _add_host(root_resource, cluster_name , host_name , ip, rack_info):
  """
  Creates host.
  @param host_name: Host name
  @param ip: ip of Host 
  @param rack_info: rack information
  @return: StatusModel.
  """
  cpath = paths.HOSTS_CREATE_PATH % (cluster_name)
  data = [{"Hosts":{"host_name":host_name , "ip":ip, "rack_info":rack_info}}]
  resp = root_resource.post(path=cpath, payload=data)
  return utils.ModelUtils.create_model(status.StatusModel, resp, root_resource, "NO_KEY")


def _assign_role(root_resource, cluster_name , host_name , component_name):
  """
  Lookup up by host_name
  @param root_resource: The root Resource object.
  @param cluster_name: Cluster name
  @param component_name : name of component.
  @param host_name: name of host
  @return: StatusModel
  """
  data = {"host_components":[{"HostRoles":{"component_name":component_name}}]}
  cpath = paths.HOSTS_ASSIGN_ROLE % (cluster_name, host_name)
  resp = root_resource.post(path=cpath, payload=data)
  return utils.ModelUtils.create_model(status.StatusModel, resp, root_resource, "NO_KEY")


def _get_all_hosts(root_resource):
  """
  Get all hosts
  @param root_resource: The root Resource.
  @return: A list of HostModel objects.
  """
  dic = root_resource.get(paths.HOSTS_PATH)
  return utils.ModelUtils.get_model_list(ModelList, HostModel, dic, root_resource , "Hosts")
  
  
def _get_all_cluster_hosts(root_resource, cluster_name):
  """
  Get all hosts in the cluster
  @param root_resource: The root Resource.
  @param cluster_name: The name of the cluster.
  @return: A list of HostModel objects.
  """
  path = paths.CLUSTER_HOSTS_PATH % (cluster_name)
  path = path + '?fields=*'
  dic = root_resource.get(path)
  return utils.ModelUtils.get_model_list(ModelList, HostModel, dic, root_resource , "Hosts")


def _delete_host(root_resource, host_name):
  """
  Delete a host by id
  @param root_resource: The root Resource object.
  @param host_name: Host name
  @return: StatusModel object
  """
  resp = root_resource.delete("%s/%s" % (paths.HOSTS_PATH, host_name))
  return utils.ModelUtils.create_model(status.StatusModel, resp, root_resource, "NO_KEY")
  

def _delete_cluster_host(root_resource, cluster_name , host_name):
  """
  Delete a host by id
  @param root_resource: The root Resource object.
  @param host_name: Host name
  @param cluster_name: cluster name
  @return: StatusModel object
  """
  path = paths.CLUSTER_HOST_PATH % (cluster_name, host_name)
  resp = root_resource.delete(path)
  return utils.ModelUtils.create_model(status.StatusModel, resp, root_resource, "NO_KEY")


def _bootstrap_hosts(root_resource , hosts_list, ssh_key):
  """
  Bootstrap hosts.
  @param hosts_list list of host_names.
  @return: A  StatusModel object.
  """
  #payload_dic = {'sshKey':ssh_key.encode('string_escape') , 'hosts':hosts_list}
  payload_dic = {'sshKey':ssh_key , 'hosts':hosts_list}
  resp = root_resource.post(paths.BOOTSTRAP_PATH, payload_dic , content_type="application/json")
  LOG.debug(resp)
  return utils.ModelUtils.create_model(status.StatusModel, resp, root_resource, "NO_KEY")


 


class HostModel(BaseModel):
  """
  The HostModel class
  """
  RO_ATTR = ('host_state', 'public_host_name')
  RW_ATTR = ('host_name', 'ip', 'rack_info')
  REF_ATTR = ('cluster_name',)
  
  def __init__(self, resource_root, host_name, ip=None , rack_info=None):
    utils.retain_self_helper(BaseModel, **locals())

  def __str__(self):
    return "<<HostModel>> hostname = %s; ip = %s ; rack_info = %s" % (self.host_name, self.ip, self.rack_info)

  def _get_cluster_name(self):
    if self.clusterRef:
      return self.clusterRef.cluster_name
    return None

  def _path(self):
    return paths.HOSTS_PATH + '/' + self.host_name

  def get_host_components(self, detail=None):
    """
    Get a specific host's components.
    @return: A ModelList containing ComponentModel objects.
    """
    return component.get_host_components(self._get_resource_root(), self._get_cluster_name(), self.host_name)

  def get_host_component(self, component_name , detail=None):
    """
    Get a specific host's ,specific component.
    @param component_name : name of component.
    @return: A ComponentModel object.
    """
    return component.get_host_component(self._get_resource_root(), self._get_cluster_name(), self.host_name , component_name)

  def assign_role(self, component_name , detail=None):
    """
    Assign a component role to the host
    @param component_name : name of component.
    @return: StatusModel.
    """
    return _assign_role(self._get_resource_root(), self._get_cluster_name(), self.host_name , component_name)

