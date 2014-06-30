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
from ambari_client.model import paths, utils, status


def _get_configuration_from_stack(
        resource_root,
        version,
        service_name,
        tag="version1"):
    """
    Get configuration from stack
    @param resource_root: The root Resource .
    @param cluster_name: cluster_name
    @param type: type of config
    @return: A ModelList of ConfigModel object
    """
    dic = resource_root.get(
        paths.STACK_SERVICES_CONFIG_PATH %
        (version, service_name))
    return utils.ModelUtils.get_model_list(
        ModelList,
        StackConfigModel,
        dic,
        resource_root,
        "StackConfigurations")


def _get_components_from_stack(
        resource_root,
        version,
        service_name,
        tag="version1"):
    """
    Get configuration from stack
    @param resource_root: The root Resource .
    @param cluster_name: cluster_name
    @param type: type of config
    @return: A ModelList of ConfigModel object
    """
    path = paths.STACK_SERVICES_COMPONENTS_PATH % (version, service_name)
    dic = resource_root.get(path)
    return utils.ModelUtils.get_model_list(
        ModelList,
        StackComponentModel,
        dic,
        resource_root,
        "StackServiceComponents")


def _get_stacks(resource_root, versions=False):
    """
    Get configuration from stack
    @param resource_root: The root Resource .
    @param cluster_name: cluster_name
    @param type: type of config
    @return: A ModelList of StackModel object
    """
    if(versions):
        path = paths.STACKS_VERSIONS_PATH
        dic = resource_root.get(path)
        return utils.ModelUtils.get_model_list(
            ModelList,
            StackModel,
            dic,
            resource_root,
            "Versions")
    else:
        path = paths.STACKS_PATH
        dic = resource_root.get(path)
        return utils.ModelUtils.get_model_list(
            ModelList,
            StackModel,
            dic,
            resource_root,
            "Stacks")


def _get_stack(resource_root, versions):
    """
    Get configuration from stack
    @param resource_root: The root Resource .
    @param cluster_name: cluster_name
    @param type: type of config
    @return: A ModelList of StackModel object
    """
    pass


def _put_stacks_and_repo(resource_root, version, os_type, repo):
    """
    Get configuration from stack
    @param resource_root: The root Resource .
    @param cluster_name: cluster_name
    @param type: type of config
    @return: A ModelList of StackModel object
    """
    path = (paths.STACKS_OS_REPO_PATH) % (version, os_type, version)
    data = '{"Repositories":{"base_url":' + repo + ',"verify_base_url":true}}'
    dictt = resource_root.put(path, payload=data)
    return utils.ModelUtils.create_model(
        status.StatusModel,
        dictt,
        resource_root,
        "NO_KEY")


class StackModel(BaseModel):

    """
    The StackModel class
    """
    RO_ATTR = ('stack_version')
    RW_ATTR = ('stack_name')
    REF_ATTR = ()

    def __init__(self, resource_root, stack_name):
        utils.retain_self_helper(BaseModel, **locals())

    def __str__(self):
        return "<<StackConfigModel>> stack_name= %s" % (self.stack_name)


class StackConfigModel(BaseModel):

    """
    The StackConfigModel class
    """
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
        return "<<StackConfigModel>> property_name=%s; service_name= %s" % (
            self.property_name, self.service_name)


class StackComponentModel(BaseModel):

    """
    The StackComponentModel class
    """
    RO_ATTR = ('stack_name', 'is_master', 'is_client', 'component_category')
    RW_ATTR = ('component_name', 'service_name', 'stack_version')
    REF_ATTR = ()

    def __init__(
            self,
            resource_root,
            component_name,
            service_name=None,
            stack_version=None):
        utils.retain_self_helper(BaseModel, **locals())

    def __str__(self):
        return "<<StackComponentModel>> component_name = %s; service_name = %s" % (
            self.component_name, self.service_name)
