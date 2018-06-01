"""
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

Ambari Agent

"""

import os
from instance_manager import create_mpack, set_mpack_instance, get_conf_dir, get_log_dir, get_run_dir, list_instances

CONFIG_DIR_KEY_NAME = 'config_dir'
LOG_DIR_KEY_NAME = 'log_dir'
RUN_DIR_KEY_NAME = 'run_dir'
PATH_KEY_NAME = 'path'
COMPONENTS_PLURAL_KEY_NAME = 'components'
COMPONENT_INSTANCES_PLURAL_KEY_NAME = 'component-instances'


def get_component_conf_path(mpack_name, instance_name, module_name, components_instance_type,
                            subgroup_name='default', component_instance_name='default'):
  """
  :returns the single string that contains the path to the configuration folder of given component instance
  :raises ValueError if the parameters doesn't match the mpack or instances structure
  """

  conf_json = get_conf_dir(mpack_name, instance_name, subgroup_name, module_name,
                           {components_instance_type: [component_instance_name]})

  return conf_json[COMPONENTS_PLURAL_KEY_NAME][components_instance_type.lower()][COMPONENT_INSTANCES_PLURAL_KEY_NAME][
    component_instance_name][CONFIG_DIR_KEY_NAME]

def get_component_log_path(mpack_name, instance_name, module_name, components_instance_type,
                            subgroup_name='default', component_instance_name='default'):
  """
  :returns the single string that contains the path to the log folder of given component instance
  :raises ValueError if the parameters doesn't match the mpack or instances structure
  """

  log_json = get_log_dir(mpack_name, instance_name, subgroup_name, module_name,
                           {components_instance_type: [component_instance_name]})

  return log_json[COMPONENTS_PLURAL_KEY_NAME][components_instance_type.lower()][COMPONENT_INSTANCES_PLURAL_KEY_NAME][
    component_instance_name][LOG_DIR_KEY_NAME]

def get_component_rundir_path(mpack_name, instance_name, module_name, components_instance_type,
                            subgroup_name='default', component_instance_name='default'):
  """
  :returns the single string that contains the path to the rundir folder of given component instance
  :raises ValueError if the parameters doesn't match the mpack or instances structure
  """

  run_json = get_run_dir(mpack_name, instance_name, subgroup_name, module_name,
                           {components_instance_type: [component_instance_name]})

  return run_json[COMPONENTS_PLURAL_KEY_NAME][components_instance_type.lower()][COMPONENT_INSTANCES_PLURAL_KEY_NAME][
    component_instance_name][RUN_DIR_KEY_NAME]

def get_component_target_path(mpack_name, instance_name, module_name, components_instance_type,
                              subgroup_name='default', component_instance_name='default'):
  """
  :returns the single string that contains the path to the mpack component folder of given component instance
  :raises ValueError if the parameters doesn't match the mpack or instances structure
  """

  instances_json = list_instances(mpack_name, instance_name, subgroup_name, module_name,
                                  {components_instance_type: [component_instance_name]})

  return instances_json[COMPONENTS_PLURAL_KEY_NAME][components_instance_type.lower()][
    COMPONENT_INSTANCES_PLURAL_KEY_NAME][component_instance_name][PATH_KEY_NAME]


def get_component_home_path(mpack_name, instance_name, module_name, components_instance_type,
                            subgroup_name='default', component_instance_name='default'):
  """
  :returns the single string that contains the path to the module component folder of given component instance
  :raises ValueError if the parameters doesn't match the mpack or instances structure
  """

  component_path = get_component_target_path(mpack_name=mpack_name, instance_name=instance_name,
                                             subgroup_name=subgroup_name,
                                             module_name=module_name, components_instance_type=components_instance_type,
                                             component_instance_name=component_instance_name)

  return os.readlink(component_path)


def create_component_instance(mpack_name, mpack_version, instance_name, module_name, components_instance_type,
                              subgroup_name='default', component_instance_name='default', fail_if_exists=False):
  """
  creates the single component instance according to the parameters
  :raises ValueError if the parameters doesn't match the mpack or instances structure
  """
  create_mpack(mpack_name, mpack_version, instance_name, subgroup_name, module_name,
               None, {components_instance_type: [component_instance_name]}, fail_if_exists)


def set_component_instance_version(mpack_name, mpack_version, instance_name, module_name, components_instance_type,
                                   subgroup_name='default', component_instance_name='default'):
  """
  changes the version of the single component instance according to the parameters
  :raises ValueError if the parameters doesn't match the mpack or instances structure
  """
  set_mpack_instance(mpack_name, mpack_version, instance_name, subgroup_name, module_name,
                     None, {components_instance_type: [component_instance_name]})
