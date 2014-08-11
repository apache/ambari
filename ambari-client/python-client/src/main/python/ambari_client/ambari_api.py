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
from ambari_client.core.rest_resource import RestResource
from ambari_client.model import blueprint, stack, cluster, host, status

__docformat__ = "epytext"

LOG = logging.getLogger(__name__)


API_VERSION = 1


class AmbariClient(RestResource):

    """
    AmbariClient top-level root resources.
    """

    def __init__(
            self,
            host_name,
            port=None,
            user_name="admin",
            password="admin",
            use_https=False,
            version=API_VERSION,
            client=None,
            http_header=None):
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

        if not http_header:
            http_header = {'X-Requested-By': 'pythonclient'}
        elif 'X-Requested-By' not in http_header.keys():
            http_header.update({'X-Requested-By': 'pythonclient'})
        else:
            pass

        host_url = "%s://%s:%s/api/v%s" % (protocol, host_name, port, version)
        if client is None:
            client = HttpClient(host_url, user_name, password)
            if http_header:
                client.set_headers(http_header)
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
        return cluster._get_all_clusters(self)

    def get_cluster(self, cluster_name):
        """
        Get a cluster by cluster_name.

        @param cluster_name : Cluster's cluster_name.
        @return : An ClusterModel.
        """
        return cluster._get_cluster(self, cluster_name)

    def get_host(self, host_name):
        """
        Lookup a host by name
        @param root_resource: The root Resource.
        @param host_name: Host name
        @return: A HostModel object
        """
        return host._get_host(self, host_name)

    def get_all_hosts(self):
        """
        Get all hosts in the Data Center
        @return: A ModelList of HostModel objects.
        """
        return host._get_all_hosts(self)

    def get_request_status(self, request_id):
        """
        Get request status
        @param request_id : request id for the request
        @return: A  StatusModel object.
        """
        return "TODO"

    def bootstrap_hosts(self, hosts_list, ssh_key=None, ssh_user=None):
        """
        Bootstrap hosts.
        @param hosts_list :list of host_names.
        @param ssh_key : ssh key for password-less access
        @return: A  StatusModel object.
        """
        return host._bootstrap_hosts(self, hosts_list, ssh_key, ssh_user)

    def create_cluster(self, cluster_name, version):
        """
        Create a new cluster.
        @param cluster_name: Cluster name.
        @param version : HDP version.
        @return  ClusterModel object.
        """
        return cluster._create_cluster(self, cluster_name, version)

    def create_cluster_from_blueprint(self, cluster_name, blueprint_name, 
                                      host_groups, configurations=None, 
                                      default_password=None):
        """
        Create a new cluster.
        @param cluster_name: Cluster cluster_name
        @param blueprint_name: the name of the blueprint
        @param host_groups: an array of host_group information
        @param configurations: an array of configuration overrides
        @param default_password: the default password to use for all password-requiring services
        @return  ClusterModel object.
        """
        return cluster._create_cluster_from_blueprint(self, cluster_name, 
            blueprint_name, host_groups, configurations=configurations, 
            default_password=default_password)

    def delete_cluster(self, cluster_name):
        """
        Delete a cluster
        @param cluster_name: Cluster to be deleted
        """
        return cluster._delete_cluster(self, cluster_name)

    def delete_host(self, host_name):
        """
        Delete a cluster
        @param host_name: host to be deleted
        """
        return host._delete_host(self, host_name)

    def get_config(self, version, service_name):
        """
        get configurations from stack
        @param version: The HDP version.
        @param service_name: service name
        @return: A ConfigModel object
        """
        return stack._get_configuration_from_stack(self, version, service_name)

    def get_components(self, version, service_name):
        """
        get components from stack
        @param version: The HDP version.
        @param service_name: service name
        @return: A ConfigModel object
        """
        return stack._get_components_from_stack(self, version, service_name)

    def get_stacks(self, view_all_versions=None):
        """
        get components from stack
        @param version: The HDP version.
        @param service_name: service name
        @return: A ConfigModel object
        """
        return stack._get_stacks(self, view_all_versions)

    def get_stack(self, version):
        """
        get components from stack
        @param version: The HDP version.
        @param service_name: service name
        @return: A ConfigModel object
        """
        return stack._get_stack(self, version)

    def get_stack_components(self, version, service_name):
        """
        get components from stack
        @param version: The HDP version.
        @param service_name: service name
        @return: A ConfigModel object
        """
        return stack._get_components_from_stack(self, version, service_name)

    def get_stack_configs(self, version, service_name):
        """
        get components from stack
        @param version: The HDP version.
        @param service_name: service name
        @return: A ConfigModel object
        """
        return stack._get_configuration_from_stack(self, version, service_name)

    def set_stacks(self, version, os_type, repo):
        """
        get components from stack
        @param version: The HDP version.
        @param service_name: service name
        @return: A ConfigModel object
        """
        return stack._put_stacks_and_repo(self, version, os_type, repo)

    def get_task_status(self, cluster_name, requestid=None):
        """
        get components from stack
        @param version: The HDP version.
        @param service_name: service name
        @return: A ConfigModel object
        """
        return cluster._task_status(self, cluster_name, requestid)

    def get_requests(self, cluster_name, noOfrequest=3):
        """
        get components from stack
        @param version: The HDP version.
        @param service_name: service name
        @return: A ConfigModel object
        """
        return status._get_N_requests(self, cluster_name, noOfrequest)

    def get_blueprint(self, blueprint_name):
        """
        get blueprint
        @param blueprint_name:blueprint_name name.
        @return: A BlueprintModel object
        """
        return blueprint.get_blueprint(self, blueprint_name)

    def get_cluster_blueprint(self, cluster_name):
        """
        get blueprint
        @param cluster_name:cluster_name name.
        @return: A BlueprintModel object
        """
        return blueprint.get_cluster_blueprint(self, cluster_name)

    def delete_blueprint(self, blueprint_name):
        """
        get blueprint
        @param blueprint_name:blueprint_name name.
        @return: A BlueprintModel object
        """
        return blueprint.delete_blueprint(self, blueprint_name)

    def create_blueprint(self, blueprint_name, blueprint_schema):
        """
        get blueprint
        @param blueprint_name:blueprint_name name.
        @return: A BlueprintModel object
        """
        return blueprint.create_blueprint(self, blueprint_name, blueprint_schema)


def get_root_resource(
        server_host,
        server_port=None,
        username="admin",
        password="admin",
        version=1):
    """
     AmbariClient.
    """
    return AmbariClient(server_host, server_port, username, password, version)
