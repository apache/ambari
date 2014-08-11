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
from ambari_client.model.base_model import BaseModel, ModelList
from ambari_client.model import status, component, paths, utils


LOG = logging.getLogger(__name__)


def _get_host(root_resource, host_name):
    """
    Lookup up by host_name
    @param root_resource: The root Resource object.
    @param cluster_name: Cluster name
    @param host_name: Host name
    @return: A HostModel object
    """
    path = paths.HOST_PATH % (host_name)
    dic = root_resource.get(path)

    return utils.ModelUtils.create_model(
        HostModel,
        dic,
        root_resource,
        "Hosts")


def _get_cluster_host(root_resource, cluster_name, host_name):
    """
    Lookup cluster host up by host_name
    @param root_resource: The root Resource object.
    @param cluster_name: Cluster name
    @param host_name: Host name
    @return: A HostModel object
    """
    path = paths.CLUSTER_HOST_PATH % (cluster_name, host_name)
    dic = root_resource.get(path)
    return utils.ModelUtils.create_model(
        HostModel,
        dic,
        root_resource,
        "Hosts")


def _create_hosts(root_resource, host_list):
    """
    Create hosts from list
    @param root_resource: The root Resource.
    @param host_name: Host name
    @param ip: IP address
    @param rack_info: Rack id. Default None
    @return: An HostList object
    """

    data = [{"Hosts": {"host_name": x.host_name,
                       "ip": x.ip,
                       "rack_info": x.rack_info}} for x in host_list]
    resp = root_resource.post(paths.HOSTS_PATH, payload=data)
    return utils.ModelUtils.create_model(
        status.StatusModel,
        resp,
        root_resource,
        "NO_KEY")


def _create_host(root_resource, host_name, ip, rack_info=None):
    """
    Create a host
    @param root_resource: The root Resource.
    @param host_name: Host name
    @param ip: IP address
    @param rack_info: Rack id. Default None
    @return: An HostModel object
    """
    host_list = ModelList([HostModel(root_resource, host_name, ip, rack_info)])
    return _create_hosts(root_resource, host_list)


def _add_hosts(root_resource, cluster_name, host_list):
    """
    Adds a hosts to a cluster.
    @param root_resource: The root Resource object.
    @param cluster_name: Cluster name
    @param host_list: list of hosts
    @return: A StatusModel object
    """
    cpath = paths.HOSTS_CREATE_PATH % (cluster_name)
    data = [{"Hosts": {"host_name": x.host_name,
                       "ip": x.ip,
                       "rack_info": x.rack_info}} for x in host_list]
    resp = root_resource.post(path=cpath, payload=data)
    return utils.ModelUtils.create_model(
        status.StatusModel,
        resp,
        root_resource,
        "NO_KEY")


def _add_host(root_resource, cluster_name, host_name, ip, rack_info=None):
    """
    Adds a host to a cluster.
    @param host_name: Host name
    @param ip: ip of Host
    @param rack_info: rack information
    @return: StatusModel.
    """
    host_list = ModelList([HostModel(root_resource, host_name, ip, rack_info)])
    return _add_hosts(root_resource, cluster_name, host_list)


def _assign_role(root_resource, cluster_name, host_name, component_name):
    """
    Add a new component to a node
    @param root_resource: The root Resource object.
    @param cluster_name: Cluster name
    @param component_name : name of component.
    @param host_name: name of host
    @return: StatusModel
    """
    data = {"host_components": [
        {"HostRoles": {"component_name": component_name}}]}
    cpath = paths.HOSTS_ASSIGN_ROLE % (cluster_name, host_name)
    resp = root_resource.post(path=cpath, payload=data)
    return utils.ModelUtils.create_model(
        status.StatusModel,
        resp,
        root_resource,
        "NO_KEY")


def _get_all_hosts(root_resource):
    """
    Get all hosts
    @param root_resource: The root Resource.
    @return: A list of HostModel objects.
    """
    dic = root_resource.get(paths.HOSTS_PATH)
    return utils.ModelUtils.get_model_list(
        ModelList,
        HostModel,
        dic,
        root_resource,
        "Hosts")


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
    return utils.ModelUtils.get_model_list(
        ModelList,
        HostModel,
        dic,
        root_resource,
        "Hosts")


def _delete_host(root_resource, host_name):
    """
    Delete a host by id
    @param root_resource: The root Resource object.
    @param host_name: Host name
    @return: StatusModel object
    """
    resp = root_resource.delete(paths.HOST_PATH % (host_name))
    return utils.ModelUtils.create_model(
        status.StatusModel,
        resp,
        root_resource,
        "NO_KEY")


def _delete_cluster_host(root_resource, cluster_name, host_name):
    """
    Delete a host by id
    @param root_resource: The root Resource object.
    @param host_name: Host name
    @param cluster_name: cluster name
    @return: StatusModel object
    """
    path = paths.CLUSTER_HOST_PATH % (cluster_name, host_name)
    resp = root_resource.delete(path)
    return utils.ModelUtils.create_model(
        status.StatusModel,
        resp,
        root_resource,
        "NO_KEY")


def _bootstrap_hosts(root_resource, hosts_list, ssh_key, ssh_user):
    """
    Bootstrap hosts.
    @param hosts_list list of host_names.
    @return: A  StatusModel object.
    """
    payload_dic = {
        "verbose": True,
        "sshKey": ssh_key,
        "hosts": hosts_list,
        "user": ssh_user}
    resp = root_resource.post(
        paths.BOOTSTRAP_PATH,
        payload_dic,
        content_type="application/json")
    status_dict = _bootstrap_resp_to_status_dict(resp)
    return utils.ModelUtils.create_model(
        status.StatusModel,
        status_dict,
        root_resource,
        "NO_KEY")


def _bootstrap_resp_to_status_dict(resp):
    """
    Bootstrap response has a little odd format
    that's why we have to convert it to the normal
    format to handle it properly later.
    """

    # if we got other response, like an error 400 happened on higher level
    if isinstance(resp['status'], int):
        return resp

    new_resp = {}

    if resp['status'] == "OK":
        new_resp['status'] = 201
    else:  # ERROR
        new_resp['status'] = 500

    new_resp['message'] = resp['log']
    new_resp['requestId'] = resp['requestId']
    return new_resp


class HostModel(BaseModel):

    """
    The HostModel class
    """
    RO_ATTR = ('host_state', 'public_host_name')
    RW_ATTR = ('host_name', 'ip', 'rack_info')
    REF_ATTR = ('cluster_name',)

    def __init__(
            self,
            resource_root,
            host_name,
            ip=None,
            rack_info='/default-rack'):
        utils.retain_self_helper(BaseModel, **locals())

    def __str__(self):
        return "<<HostModel>> hostname = %s; ip = %s ; rack_info = %s" % (
            self.host_name, self.ip, self.rack_info)

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
        return component.get_host_components(
            self._get_resource_root(),
            self._get_cluster_name(),
            self.host_name)

    def get_host_component(self, component_name, detail=None):
        """
        Get a specific host's ,specific component.
        @param component_name : name of component.
        @return: A ComponentModel object.
        """
        return component.get_host_component(
            self._get_resource_root(),
            self._get_cluster_name(),
            self.host_name,
            component_name)

    def assign_role(self, component_name, detail=None):
        """
        Assign a component role to the host
        @param component_name : name of component.
        @return: StatusModel.
        """
        return _assign_role(
            self._get_resource_root(),
            self._get_cluster_name(),
            self.host_name,
            component_name)

    def install_all_components(self):
        root_resource = self._get_resource_root()
        path = paths.HOSTS_COMPONENTS_PATH % (self._get_cluster_name(), 
                                              self.host_name)
        data = {
            "RequestInfo": {
                "context" :"Install All Components",
            }, 
            "Body": {
                "HostRoles": {"state": "INSTALLED"},
            },
        }
        resp = root_resource.put(path=path, payload=data)
        return utils.ModelUtils.create_model(status.StatusModel, resp, 
                                             root_resource, "NO_KEY")

    def start_all_components(self):
        root_resource = self._get_resource_root()
        path = paths.HOSTS_COMPONENTS_PATH % (self._get_cluster_name(), 
                                              self.host_name)
        data = {
            "RequestInfo": {
                "context" :"Start All Components",
            }, 
            "Body": {
                "HostRoles": {"state": "STARTED"},
            },
        }
        resp = root_resource.put(path=path, payload=data)
        return utils.ModelUtils.create_model(status.StatusModel, resp, 
                                             root_resource, "NO_KEY")

    def stop_all_components(self):
        root_resource = self._get_resource_root()
        path = paths.HOSTS_COMPONENTS_PATH % (self._get_cluster_name(), 
                                              self.host_name)
        data = {
            "RequestInfo": {
                "context" :"Stop All Components",
            }, 
            "Body": {
                "HostRoles": {"state": "INSTALLED"},
            },
        }
        resp = root_resource.put(path=path, payload=data)
        return utils.ModelUtils.create_model(status.StatusModel, resp, 
                                             root_resource, "NO_KEY")

    def enable_maintenance_mode(self):
        root_resource = self._get_resource_root()
        path = paths.HOSTS_COMPONENTS_PATH % (self._get_cluster_name(), 
                                              self.host_name)
        data = {
            "RequestInfo": {
                "context" :"Start Maintanence Mode",
            }, 
            "Body": {
                "HostRoles": {"maintenance_state": "ON"},
            },
        }
        resp = root_resource.put(path=path, payload=data)
        return utils.ModelUtils.create_model(status.StatusModel, resp, 
                                             root_resource, "NO_KEY")

    def disable_maintenance_mode(self):
        root_resource = self._get_resource_root()
        path = paths.HOSTS_COMPONENTS_PATH % (self._get_cluster_name(), 
                                              self.host_name)
        data = {
            "RequestInfo": {
                "context" :"Stop Maintanence Mode",
            }, 
            "Body": {
                "HostRoles": {"maintenance_state": "OFF"},
            },
        }
        resp = root_resource.put(path=path, payload=data)
        return utils.ModelUtils.create_model(status.StatusModel, resp, 
                                             root_resource, "NO_KEY")
