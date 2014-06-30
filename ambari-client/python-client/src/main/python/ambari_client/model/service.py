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
from ambari_client.model.base_model import BaseModel, ModelList
from ambari_client.model import component, paths, status, stack, utils


LOG = logging.getLogger(__name__)


def _get_all_services(resource_root, cluster_name):
    """
    Get all services in a cluster.
    @param cluster_name :Cluster name.
    @return: A  ModelList object.
    """
    path = paths.SERVICES_PATH % (cluster_name,)
    path = path + '?fields=*'
    dic = resource_root.get(path)
    return utils.ModelUtils.get_model_list(
        ModelList,
        ServiceModel,
        dic,
        resource_root,
        "ServiceInfo")


def _get_service(resource_root, service_name, cluster_name):
    """
    Get a specific services in a cluster.
    @param service_name :Service name.
    @param cluster_name :Cluster name.
    @return: A  ServiceModel object.
    """
    path = "%s/%s" % (paths.SERVICES_PATH % (cluster_name,), service_name)
    dic = resource_root.get(path)
    return utils.ModelUtils.create_model(
        ServiceModel,
        dic,
        resource_root,
        "ServiceInfo")


def _create_services(root_resource, cluster_name, service_names):
    """
    Create services
    @param root_resource: The root Resource object.
    @param service_names: list of service_names
    @param cluster_name: Cluster name
    @return: StatusModel
    """
    data = [{"ServiceInfo": {"service_name": x}} for x in service_names]
    cpath = paths.SERVICES_PATH % cluster_name
    resp = root_resource.post(path=cpath, payload=data)
    return utils.ModelUtils.create_model(
        status.StatusModel,
        resp,
        root_resource,
        "NO_KEY")


def _create_service(root_resource, cluster_name, service_name):
    """
    Create a single service
    @param root_resource: The root Resource object.
    @param service_name:  service_name
    @param cluster_name: Cluster name
    @return: StatusModel
    """
    data = {"ServiceInfo": {"service_name": service_name}}
    cpath = paths.SERVICES_PATH % cluster_name
    resp = root_resource.post(path=cpath, payload=data)
    return utils.ModelUtils.create_model(
        status.StatusModel,
        resp,
        root_resource,
        "NO_KEY")


def _create_service_components(
        root_resource,
        cluster_name,
        version,
        service_name):
    """
    Create service with components
    @param root_resource: The root Resource object.
    @param service_name:  service_names
    @param cluster_name: Cluster service_name
    @return: An ServiceModel object
    """
    components = stack._get_components_from_stack(
        root_resource,
        version,
        service_name)
    list_componnetinfo = [
        {"ServiceComponentInfo": {"component_name": x.component_name}} for x in components]
    data = {"components": list_componnetinfo}
    cpath = paths.SERVICE_CREATE_PATH % (cluster_name, service_name)
    resp = root_resource.post(path=cpath, payload=data)
    return utils.ModelUtils.create_model(
        status.StatusModel,
        resp,
        root_resource,
        "NO_KEY")


def _create_service_component(
        root_resource,
        cluster_name,
        version,
        service_name,
        component_name):
    """
    Create service with single component
    @param root_resource: The root Resource object.
    @param service_name:  service_names
    @param cluster_name: Cluster service_name
    @param component_name: name of component
    @return: An ServiceModel object
    """
    cpath = paths.SERVICE_COMPONENT_PATH % (
        cluster_name, service_name, component_name)
    resp = root_resource.post(path=cpath, payload=None)
    return utils.ModelUtils.create_model(
        status.StatusModel,
        resp,
        root_resource,
        "NO_KEY")


def _delete_service(root_resource, service_name, cluster_name):
    """
    Delete a service by service_name
    @param root_resource: The root Resource object.
    @param service_name: Service service_name
    @param cluster_name: Cluster service_name
    @return: The StatusModel object
    """
    resp = root_resource.delete(
        "%s/%s" %
        (paths.SERVICES_PATH %
         (cluster_name,), service_name))
    time.sleep(3)
    return utils.ModelUtils.create_model(
        status.StatusModel,
        resp,
        root_resource,
        "NO_KEY")


class ServiceModel(BaseModel):

    """
    The ServiceModel class
    """
    #RO_ATTR = ('state', 'cluster_name')
    RW_ATTR = ('service_name', 'state')
    REF_ATTR = ('cluster_name',)

    def __init__(self, resource_root, service_name, state):
        #BaseModel.__init__(self, **locals())
        utils.retain_self_helper(BaseModel, **locals())

    def __str__(self):
        return "<<ServiceModel>> = %s ;state = %s ; cluster_name = %s" % (
            self.service_name, self.state, self._get_cluster_name())

    def _get_cluster_name(self):
        if self.clusterRef:
            return self.clusterRef.cluster_name
        return None

    def _path(self):
        """
        Return the API path for this object.
        """
        if self._get_cluster_name():
            return paths.SERVICE_PATH % (
                self._get_cluster_name(), self.service_name)
        else:
            return ''

    def _action(self, data=None):
        path = self._path()
        resp = self._get_resource_root().put(path, payload=data)
        status_model = utils.ModelUtils.create_model(
            status.StatusModel,
            resp,
            self._get_resource_root(),
            "NO_KEY")
        if status_model._get_id() is not None:
            status_model.request_path = paths.REQUEST_PATH % (
                self._get_cluster_name(), status_model._get_id())
        else:
            status_model.request_path = None
        return status_model

    def start(self, message=None):
        """
        Start a service.
        """
        data = None
        if message:
            data = {
                "RequestInfo": {
                    "context": message}, "Body": {
                    "ServiceInfo": {
                        "state": "STARTED"}}}
        else:
            data = {"ServiceInfo": {"state": "STARTED"}}
        return self._action(data)

    def stop(self, message=None):
        """
        Stop a service.
        """
        data = None
        if message:
            data = {
                "RequestInfo": {
                    "context": message}, "Body": {
                    "ServiceInfo": {
                        "state": "INSTALLED"}}}
        else:
            data = {"ServiceInfo": {"state": "INSTALLED"}}
        return self._action(data)

    def install(self):
        """
        Install a service.
        """
        data = {"ServiceInfo": {"state": "INSTALLED"}}
        return self._action(data)

    def get_service_components(self, detail=None):
        """
        Get a specific services's components.
        @return: A ComponentModel object.
        """
        return component._get_service_components(
            self._get_resource_root(),
            self._get_cluster_name(),
            self.service_name)

    def get_service_component(self, component_name, detail=None):
        """
        Get a specific services's components.
        @return: A ComponentModel object.
        """
        return component._get_service_component(
            self._get_resource_root(),
            self._get_cluster_name(),
            self.service_name,
            component_name)
