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


from ambari_client.model.base_model import BaseModel, ModelList
from ambari_client.model import paths, status, utils


def _get_configuration(resource_root, cluster_name, type, tag="version1"):
    """
    Get configuration of a cluster
    @param resource_root: The root Resource .
    @param cluster_name: cluster_name
    @param type: type of config
    @return: A ConfigModel object
    """
    dic = resource_root.get(
        paths.CONFIGURATION_PATH %
        (cluster_name, type, tag))

    if len(dic["items"]) == 0:
        return None

    config_model = utils.ModelUtils.create_model(
        ConfigModel,
        dic["items"][0],
        resource_root,
        "NO_KEY")
    ref_clss = utils.getREF_class_name("cluster_name")
    config_model._setattr(ref_clss, dic["items"][0]['Config']['cluster_name'])
    return config_model


def _get_all_configuration(resource_root, cluster_name, type):
    """
    Gets ALL configuration of a cluster of a given type
    @param resource_root: The root Resource .
    @param cluster_name: cluster_name
    @param type: type of config
    @return: A ConfigModel object
    """
    dic = resource_root.get(
        paths.CONFIGURATION_ALL_PATH %
        (cluster_name, type))

    if len(dic["items"]) == 0:
        return None

    objects = []
    for cfgm in dic["items"]:
        config_model = utils.ModelUtils.create_model(
            ConfigModel,
            cfgm,
            resource_root,
            "NO_KEY")
        ref_clss = utils.getREF_class_name("cluster_name")
        config_model._setattr(ref_clss, cfgm['Config']['cluster_name'])
        objects.append(config_model)
    return ModelList(objects)


def _update_configuration(
        resource_root,
        cluster_name,
        type,
        tag,
        config_model):
    """
    Update configuration of a cluster
    @param resource_root: The root Resource .
    @param cluster_name: cluster_name
    @param type: type of config
    @param config_model: config model object
    @return: A ConfigModel object
    """
    data = {
        "Clusters": {
            "desired_configs": {
                "type": type,
                "tag": tag,
                "properties": config_model.properties}}}
    resp = resource_root.put(
        path=paths.UPDATE_CONFIGURATION_PATH %
        cluster_name,
        payload=data)
    return utils.ModelUtils.create_model(
        status.StatusModel,
        resp,
        resource_root,
        "NO_KEY")


def _add_config(root_resource, cluster_name, type, tag, properties):
    """
    add configurations
    @param type: the type of config
    @param tag: tag
    @param properties: a dict of properties
    @return: A StatusModel object
    """
    cpath = paths.CLUSTERS_CONFIG_PATH % cluster_name
    data = {
        "Clusters": {
            "desired_configs": {
                "type": type,
                "tag": tag,
                "properties": properties}}}
    resp = root_resource.put(path=cpath, payload=data)
    return utils.ModelUtils.create_model(
        status.StatusModel,
        resp,
        root_resource,
        "NO_KEY")


def _create_config(root_resource, cluster_name, type, tag, properties):
    """
    create a new  configurations
    @param type: the type of config
    @param tag: tag
    @param properties: a dict of properties
    @return: A StatusModel object
    """
    cpath = paths.CLUSTERS_CONFIG_PATH % cluster_name
    data = {"type": type, "tag": tag, "properties": properties}
    resp = root_resource.put(path=cpath, payload=data)
    return utils.ModelUtils.create_model(
        status.StatusModel,
        resp,
        root_resource,
        "NO_KEY")


class ConfigModel(BaseModel):

    """
    The ConfigModel class
    """
    RO_ATTR = ('properties',)
    RW_ATTR = ('tag', 'type')
    REF_ATTR = ('cluster_name',)

    def __init__(self, resource_root, tag, type=None):
        utils.retain_self_helper(BaseModel, **locals())

    def __str__(self):
        return "<<ConfigModel>> tag = %s; type = %s" % (self.tag, self.type)

    def _get_cluster_name(self):
        if self.clusterRef:
            return self.clusterRef.cluster_name
        return None

    def __lt__(self, other):
        return self.tag < other.tag

    def _path(self):
        """
        Return the API path for this service.
        """
        if self._get_cluster_name():
            return paths.CONFIGURATION_PATH % (
                self._get_cluster_name(), self.type, self.tag)
        else:
            return ''
