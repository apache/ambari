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
from ambari_client.model.paths import HOSTS_PATH , CLUSTER_HOSTS_PATH , CLUSTER_HOST_PATH ,BOOTSTRAP_PATH
from ambari_client.model import service ,status
from ambari_client.model.utils import ModelUtils , retain_self_helper


LOG = logging.getLogger(__name__)

def create_host(root_resource, host_name, ip, rack_info=None):
  """
  Create a host
  @param root_resource: The root Resource object.
  @param host_name: Host name
  @param ip: IP address
  @param rack_info: Rack id. Default None
  @return: An HostModel object
  """
  host = HostModel(root_resource, host_name, ip, rack_info)
  host_list = ModelList([host])
  body = json.dumps(host_list.to_json_dict())
  resp = root_resource.post(HOSTS_PATH, data=body)
  # The server returns a created hosts 
  return get_host(root_resource, host_name)




def get_host(root_resource, cluster_name , host_name):
  """
  Lookup up by host_name
  @param root_resource: The root Resource object.
  @param cluster_name: Cluster name
  @param host_name: Host name
  @return: A HostModel object
  """
  path = CLUSTER_HOST_PATH % (cluster_name, host_name)
  dic = root_resource.get(path)
  return ModelUtils.create_model(HostModel , dic, root_resource, "Hosts") 



def get_all_hosts(root_resource):
  """
  Get all hosts
  @param root_resource: The root Resource.
  @return: A list of HostModel objects.
  """
  dic = root_resource.get(HOSTS_PATH)
  return ModelUtils.get_model_list(HostModel, dic, root_resource , "Hosts")
  
  
  
def get_all_cluster_hosts(root_resource, cluster_name):
  """
  Get all hosts in the cluster
  @param root_resource: The root Resource.
  @return: A list of HostModel objects.
  """
  path = "%s/%s" % (HOSTS_PATH, cluster_name)
  dic = root_resource.get(path)
  return ModelUtils.get_model_list(HostModel, dic, root_resource , "Hosts")



def delete_host(root_resource, host_id):
  """
  Delete a host by id
  @param root_resource: The root Resource object.
  @param host_id: Host id
  @return: The deleted HostModel object
  """
  resp = root_resource.delete("%s/%s" % (HOSTS_PATH, host_id))
  return HostModel.from_json_dict(resp, root_resource)
  
  
def bootstrap_hosts(root_resource , hosts_list,ssh_key):
  """
  Bootstrap hosts.
  @param hosts_list list of host_names.
  @return: A  StatusModel object.
  """
  payload_dic = {'sshKey':ssh_key.encode('string_escape') ,'hosts':hosts_list}
  resp = root_resource.post(BOOTSTRAP_PATH,payload_dic ,content_type="application/json")
  return ModelUtils.create_model(status.StatusModel, resp, root_resource, "NO_KEY")



 
class HostModel(BaseModel):
  RO_ATTR = ('host_state', 'public_host_name')
  RW_ATTR = ('host_name', 'ip', 'rack_info')
  REF_ATTR = ('cluster_name',)
  
  def __init__(self, resource_root, host_name, ip=None , rack_info=None):
    retain_self_helper(**locals())

  def __str__(self):
    return "<<HostModel>>: = %s; ip = %s" % (self.host_name, self.ip)

  def _path(self):
    return HOSTS_PATH + '/' + self.host_name

