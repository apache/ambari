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
from ambari_client.model.base_model import BaseModel, ModelList
from ambari_client.model import service, host, paths, status, configuration, utils


LOG = logging.getLogger(__name__)


def _get_cluster(resource_root, cluster_name):
    """
    Lookup a cluster by cluster_name
    @param resource_root: The root Resource .
    @param cluster_name: cluster_name
    @return: A ClusterModel object
    """
    dic = resource_root.get("%s/%s" % (paths.CLUSTERS_PATH, cluster_name))
    return utils.ModelUtils.create_model(
        ClusterModel,
        dic,
        resource_root,
        "Clusters")


def _get_all_clusters(root_resource):
    """
    Get all clusters
    @param root_resource: The root Resource .
    @return: A list of ClusterModel objects.
    """
    dic = root_resource.get(paths.CLUSTERS_PATH)
    return utils.ModelUtils.get_model_list(
        ModelList,
        ClusterModel,
        dic,
        root_resource,
        "Clusters")


def _create_cluster(root_resource, cluster_name, version):
    """
    Create a cluster
    @param root_resource: The root Resource.
    @param cluster_name: Cluster cluster_name
    @param version: HDP version
    @return: An ClusterModel object
    """
    data = {"Clusters": {"version": str(version)}}
    path = paths.CLUSTERS_PATH + "/%s" % (cluster_name)
    resp = root_resource.post(path=path, payload=data)
    return utils.ModelUtils.create_model(
        status.StatusModel,
        resp,
        root_resource,
        "NO_KEY")


def _create_cluster_from_blueprint(root_resource, cluster_name, blueprint_name, 
                                   host_groups, configurations=None, 
                                   default_password=None):
    """
    Create a cluster
    @param root_resource: The root Resource.
    @param cluster_name: Cluster cluster_name
    @param blueprint_name: the name of the blueprint
    @param host_groups: an array of host_group information
    @param configurations: an array of configuration overrides
    @param default_password: the default password to use for all password-requiring services
    @return: An StatusModel object
    """
    data = {
      "blueprint" : blueprint_name,
      "host_groups" : host_groups,
    }
    if configurations is not None:
        data['configurations'] = configurations
    if default_password is not None:
        data['default_password'] = default_password

    path = paths.CLUSTERS_PATH + "/%s" % (cluster_name)
    resp = root_resource.post(path=path, payload=data)
    return utils.ModelUtils.create_model(
        status.StatusModel,
        resp,
        root_resource,
        "NO_KEY")


def _delete_cluster(root_resource, cluster_name):
    """
    Delete a cluster by name
    @param root_resource: The root Resource .
    @param name: Cluster name
    """
    resp = root_resource.delete("%s/%s" % (paths.CLUSTERS_PATH, cluster_name))
    return utils.ModelUtils.create_model(
        status.StatusModel,
        resp,
        root_resource,
        "NO_KEY")


def _install_all_services(root_resource, cluster_name):
    """
    Start all services
    @param root_resource: The root Resource .
    @param name: Cluster name
    """
    cpath = paths.CLUSTER_START_ALL_SERVICES % cluster_name
    data = {
        "RequestInfo": {
            "context": "Install Services"}, "Body": {
            "ServiceInfo": {
                "state": "INSTALLED"}}}
    resp = root_resource.put(path=cpath, payload=data)
    return utils.ModelUtils.create_model(
        status.StatusModel,
        resp,
        root_resource,
        "NO_KEY")


def _stop_all_services(root_resource, cluster_name):
    """
    Start all services
    @param root_resource: The root Resource .
    @param name: Cluster name
    """
    cpath = paths.CLUSTER_STOP_ALL_SERVICES % cluster_name
    data = {
        "RequestInfo": {
            "context": "Stop All Services"}, "Body": {
            "ServiceInfo": {
                "state": "INSTALLED"}}}
    resp = root_resource.put(path=cpath, payload=data)
    return utils.ModelUtils.create_model(
        status.StatusModel,
        resp,
        root_resource,
        "NO_KEY")


def _start_all_services(root_resource, cluster_name, run_smoke_test=False):
    """
    Start all services
    @param root_resource: The root Resource .
    @param name: Cluster name
    """
    cpath = paths.CLUSTER_START_ALL_SERVICES % cluster_name
    if run_smoke_test:
        cpath = "%s&%s" % (
            cpath, "params/run_smoke_test=true&params/reconfigure_client=false")
    data = {
        "RequestInfo": {
            "context": "Start All Services"}, "Body": {
            "ServiceInfo": {
                "state": "STARTED"}}}
    resp = root_resource.put(path=cpath, payload=data)
    if isinstance(resp, dict) and "Requests" in resp:
        resp = resp["Requests"]
    return utils.ModelUtils.create_model(
        status.StatusModel,
        resp,
        root_resource,
        "NO_KEY")


def _task_status(root_resource, cluster_name, requestid):
    cpath = paths.TASKS_PATH % (cluster_name, requestid)
    dic = root_resource.get(cpath)
    return utils.ModelUtils.get_model_list(
        ModelList,
        TaskModel,
        dic,
        root_resource,
        "Tasks")


class TaskModel(BaseModel):

    """
    The ClusterModel class
    """
    RW_ATTR = ('host_name', 'role', 'status')

    def __init__(self, resource_root, host_name, role, status):
        utils.retain_self_helper(BaseModel, **locals())

    def __str__(self):
        return "<<TaskModel>> host_name = %s; status = %s" % (
            self.host_name, self.status)


class ClusterModel(BaseModel):

    """
    The ClusterModel class
    """
    RW_ATTR = ('cluster_name', 'version')

    def __init__(self, resource_root, cluster_name, version):
        utils.retain_self_helper(BaseModel, **locals())

    def __str__(self):
        return "<<ClusterModel>> cluster_name = %s; version = %s" % (
            self.cluster_name, self.version)

    def _path(self):
        return "%s/%s" % (paths.CLUSTERS_PATH, self.cluster_name)

    def get_service(self, service_name):
        """
        Get a service by service_name.
        @param service_name: Service name
        @return: A ServiceModel object
        """
        return service._get_service(
            self._get_resource_root(),
            service_name,
            self.cluster_name)

    def get_all_services(self, detail=None):
        """
        Get all services in this cluster.
        @return: ModelList containing ServiceModel objects.
        """
        return service._get_all_services(
            self._get_resource_root(),
            self.cluster_name)

    def get_all_hosts(self, detail=None):
        """
        Get all hosts in this cluster.
        @return: ModelList containing HostModel objects.
        """
        return host._get_all_cluster_hosts(
            self._get_resource_root(),
            self.cluster_name)

    def get_host(self, hostname, detail=None):
        """
        Get a specific hosts in this cluster.
        @return: A HostModel object.
        """
        return host._get_cluster_host(
            self._get_resource_root(),
            self.cluster_name,
            hostname)

    def get_global_config(self, detail=None):
        """
        Get global configuration of  cluster.
        @return: A ConfigModel object.
        """
        return configuration._get_configuration(
            self._get_resource_root(),
            self.cluster_name,
            "global")

    def get_core_site_config(self, tag="version1", detail=None):
        """
        Get core-site configuration of  cluster.
        @return: A ConfigModel object or ModelList<ConfiObject>
        """
        if(detail == utils.ALL):
            return configuration._get_all_configuration(
                self._get_resource_root(),
                self.cluster_name,
                "core-site")
        else:
            return configuration._get_configuration(
                self._get_resource_root(),
                self.cluster_name,
                "core-site",
                tag)

    def get_hdfs_site_config(self, detail=None):
        """
        Get hdfs-site configuration of  cluster.
        @return: A ConfigModel object.
        """
        return configuration._get_configuration(
            self._get_resource_root(),
            self.cluster_name,
            "hdfs-site")

    def get_mapred_site_config(self, detail=None):
        """
        Get mapred-site configuration of  cluster.
        @return: A ConfigModel object.
        """
        return configuration._get_configuration(
            self._get_resource_root(),
            self.cluster_name,
            "mapred-site")

    def update_global_config(self, config_model, tag="version1", detail=None):
        """
        Updates the  global configuration of  cluster.
        @param config_model: The configModel object
        @return: A ConfigModel object.
        """
        return configuration._update_configuration(
            self._get_resource_root(),
            self.cluster_name,
            "global",
            tag,
            config_model)

    def update_core_site_config(
            self,
            config_model,
            tag="version1",
            detail=None):
        """
        Updates the  core-site configuration of  cluster.
        @param config_model: The configModel object
        @return: A ConfigModel object.
        """
        return configuration._update_configuration(
            self._get_resource_root(),
            self.cluster_name,
            "core-site",
            tag,
            config_model)

    def update_hdfs_site_config(
            self,
            config_model,
            tag="version1",
            detail=None):
        """
        Updates the  hdfs-site configuration of  cluster.
        @param config_model: The configModel object
        @return: A ConfigModel object.
        """
        return configuration._update_configuration(
            self._get_resource_root(),
            self.cluster_name,
            "hdfs-site",
            tag,
            config_model)

    def update_mapred_site_config(
            self,
            config_model,
            tag="version1",
            detail=None):
        """
        Updates the  mapred-site configuration of  cluster.
        @param config_model: The configModel object
        @return: A ConfigModel object.
        """
        return configuration._update_configuration(
            self._get_resource_root(),
            self.cluster_name,
            "mapred-site",
            tag,
            config_model)

    def create_services(self, services_list, detail=None):
        """
        Creates services.
        @param services_list: list of services
        @return: StatusModel.
        """
        return service._create_services(
            self._get_resource_root(),
            self.cluster_name,
            services_list)

    def create_service(self, service_name, detail=None):
        """
        Creates a single service
        @param service_name: service name
        @return: StatusModel.
        """
        return service._create_service(
            self._get_resource_root(),
            self.cluster_name,
            service_name)

    def create_service_components(self, version, service_name, detail=None):
        """
        Creates service with components
        @param version: version
        @param service_name: service_name
        @return: StatusModel.
        """
        return service._create_service_components(
            self._get_resource_root(),
            self.cluster_name,
            version,
            service_name)

    def create_service_component(
            self,
            version,
            service_name,
            component_name,
            detail=None):
        """
        Create service with component
        @param version: version
        @param service_name: service_name
        @return: StatusModel.
        """
        return service._create_service_component(
            self._get_resource_root(),
            self.cluster_name,
            version,
            service_name,
            component_name)

    def create_hosts(self, host_list, detail=None):
        """
        Creates hosts.
        @param host_list: list of HostModel
        @return: StatusModel.
        """
        return host._add_hosts(
            self._get_resource_root(),
            self.cluster_name,
            host_list)

    def create_host(
            self,
            host_name,
            ip,
            rack_info='/default-rack',
            detail=None):
        """
        Creates host.
        @param host_name: Host name
        @param ip: ip of Host
        @param rack_info: rack information
        @return: StatusModel.
        """
        return host._add_host(
            self._get_resource_root(),
            self.cluster_name,
            host_name,
            ip,
            rack_info)

    def delete_host(self, host_name, detail=None):
        """
        deletes a host.
        @param host_name: Host name
        @return: StatusModel.
        """
        return host._delete_cluster_host(
            self._get_resource_root(),
            self.cluster_name,
            host_name)

    def start_all_services(self, run_smoke_test=False, detail=None):
        """
        Start all the services.
        @return: StatusModel.
        """
        return _start_all_services(
            self._get_resource_root(),
            self.cluster_name,
            run_smoke_test)

    def stop_all_services(self, detail=None):
        """
        Stop all the services.
        @return: StatusModel.
        """
        return _stop_all_services(self._get_resource_root(), self.cluster_name)

    def install_all_services(self, detail=None):
        """
        INIT all the services.
        @return: StatusModel.
        """
        return _install_all_services(
            self._get_resource_root(),
            self.cluster_name)

    def add_config(self, type, tag, properties):
        """
        add configurations to the cluster
        @param type: the type of config
        @param tag: tag
        @param properties: a dict of properties
        @return: A StatusModel object
        """
        return configuration._add_config(
            self._get_resource_root(),
            self.cluster_name,
            type,
            tag,
            properties)

    def create_config(self, type, tag, properties):
        """
        create configurations to the cluster
        @param type: the type of config
        @param tag: tag
        @param properties: a dict of properties
        @return: A StatusModel object
        """
        return configuration._create_config(
            self._get_resource_root(),
            self.cluster_name,
            type,
            tag,
            properties)


class ClusterModelRef(BaseModel):

    """
    The ClusterModelRef class
      Some models need reference to cluster
    """
    RW_ATTR = ('cluster_name',)

    def __init__(self, resource_root, cluster_name=None):
        utils.retain_self_helper(BaseModel, **locals())
