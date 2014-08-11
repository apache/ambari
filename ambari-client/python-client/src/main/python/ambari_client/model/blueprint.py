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
import json
from ambari_client.model.base_model import BaseModel, ModelList
from ambari_client.model import paths, utils, status


LOG = logging.getLogger(__name__)


def get_blueprint(resource_root, blueprint_name):
    """
    Get all Blueprint
    @param root_resource: The root Resource .
    @param name: blueprint_name
    @return: A list of BlueprintModel objects.
    """
    if not blueprint_name:
        dic = resource_root.get(paths.BLUEPRINT_ALL_PATH)
        return utils.ModelUtils.get_model_list(
            ModelList,
            BlueprintModel,
            dic,
            resource_root,
            "Blueprints")
    else:
        dic = resource_root.get(paths.BLUEPRINT_PATH % blueprint_name)
        return utils.ModelUtils.create_model(
            BlueprintModel,
            dic,
            resource_root,
            "Blueprints")


def get_cluster_blueprint(resource_root, cluster_name):
    """
    Get all Blueprint
    @param root_resource: The root Resource .
    @param name: blueprint_name
    @return: A list of BlueprintModel objects.
    """
    resp = resource_root.get(paths.BLUEPRINT_CLUSTER_PATH % cluster_name)
    result = json.dumps(resp)
    resp_dictt = json.loads(result)
    objects = [
        utils.ModelUtils.create_model(
            BlueprintHostModel,
            x,
            resource_root,
            "NO_KEY") for x in resp_dictt['host_groups']]
    LOG.debug(objects)

    bluep = utils.ModelUtils.create_model(
        BlueprintModel,
        resp,
        resource_root,
        "Blueprints")

    return bluep, ModelList(objects)


def delete_blueprint(resource_root, blueprint_name):
    """
    Delete a blueprint by name
    @param root_resource: The root Resource .
    @param name: blueprint_name
    """
    resp = resource_root.delete(paths.BLUEPRINT_PATH % blueprint_name)
    return utils.ModelUtils.create_model(
        status.StatusModel,
        resp,
        resource_root,
        "NO_KEY")


def create_blueprint(resource_root, blueprint_name, blueprint_schema):
    """
    Create a blueprint
    @param root_resource: The root Resource.
    @param blueprint_name: blueprint_name
    @param blueprint_schema: blueprint  json
    @return: An ClusterModel object
    """
    path = paths.BLUEPRINT_PATH % blueprint_name
    resp = resource_root.post(path=path, payload=blueprint_schema)
    return utils.ModelUtils.create_model(
        status.StatusModel,
        resp,
        resource_root,
        "NO_KEY")


class BlueprintModel(BaseModel):

    """
    The BlueprintModel class
    """
    RO_ATTR = ('stack_name', 'stack_version')
    RW_ATTR = ('blueprint_name')
    REF_ATTR = ('cluster_name',)

    def __init__(self, resource_root, blueprint_name):
        utils.retain_self_helper(BaseModel, **locals())

    def __str__(self):
        return "<<BlueprintModel>> blueprint_name = %s " % (
            self.blueprint_name)

    def _get_cluster_name(self):
        if self.clusterRef:
            return self.clusterRef.cluster_name
        return None


class BlueprintHostModel(BaseModel):

    """
    The BlueprintHostModel class
    """
    RO_ATTR = ('blueprint_name',)
    RW_ATTR = ('name', 'components', 'cardinality')
    REF_ATTR = ()

    def __init__(self, resource_root, name, components, cardinality):
        utils.retain_self_helper(BaseModel, **locals())

    def __str__(self):
        return "<<BlueprintHostModel>> name = %s ,components =%s" % (
            self.name, str(self.components))

    def to_json(self):
        pass


class BlueprintConfigModel(BaseModel):

    """
    The BlueprintConfigModel class
    """
    RO_ATTR = ('stack_name', 'stack_version')
    RW_ATTR = ('blueprint_name')
    REF_ATTR = ('cluster_name',)

    RO_ATTR = ('stack_name', 'type', 'property_description')
    RW_ATTR = (
        'property_name',
        'property_value',
        'service_name',
        'stack_version')
    REF_ATTR = ()

    def __init__(
            self,
            resource_root,
            property_name,
            property_value=None,
            service_name=None,
            stack_version=None):
        utils.retain_self_helper(BaseModel, **locals())

    def __str__(self):
        return "<<BlueprintConfigModel>> property_name=%s; service_name= %s" % (
            self.property_name, self.service_name)
