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

"""

__all__ = ["create_mpack", "set_mpack_instance", "get_conf_dir", "list_instances"]

import sys
import os
import json

MPACK_JSON_FILE_NAME = 'mpack.json'
CURRENT_SOFTLINK_NAME = 'current'
CONFIGS_DIRECTORY_NAME = 'conf'

ROOT_FOLDER_PATH = "/opt/odp"

INSTANCES_FOLDER_NAME = "instances"
MODULES_FOLDER_NAME = "modules"
MPACKS_FOLDER_NAME = "mpacks"
DEFAULT_COMPONENT_INSTANCE_NAME = 'default'
DEFAULT_MPACK_INSTANCE_NAME = 'default'


def create_mpack(mpack_name, mpack_version, mpack_instance, subgroup_name, module_name,
                 components=None, components_map=None):
  """
  Use case 1: Creates an instance of mpack with a new subgroup, new module and one or more component(s)

  Use case 2: Creates/adds in an existing mpack either a : new subgroup and/or module and/or component instance.

  Components are provided as map with key as 'component type' and value as 'list of individual component instances
  names' OR empty map to create single instance of all components with name 'default'
  OR
  list of 'components_instances_name' to be created. (default) OR '*' for all components
  """
  MpackInstance.create_mpack_instance(mpack_name, mpack_version, mpack_instance, subgroup_name, module_name,
                                      components, components_map)


def set_mpack_instance(mpack, mpack_version, mpack_instance, subgroup_name, module_name,
                       components=None, components_map=None):
  """
  Use case: move a given component instances from one version to the next version by modifying the soft links.
  Eg: Moving hive_server instance 'HS-1' from version 1.0.0-b1 to 1.5.0-b1.

  Components are provided as map with key as 'component type' and value as 'list of individual component instances
  names' OR empty map to create single instance of all components with name 'default'
  OR
  list of 'components_instances_name' to be created. (default) OR '*' for all components
  """
  instances = MpackInstance.parse_instances_with_filtering(os.path.join(ROOT_FOLDER_PATH, INSTANCES_FOLDER_NAME), mpack,
                                                           mpack_instance, subgroup_name, module_name,
                                                           components, components_map)
  if not instances:
    print("\nFound no instances for the given filters.")
    sys.exit(0)

  for mpack in instances:
    for mpack_instance_name in instances[mpack]:
      instances[mpack][mpack_instance_name].set_new_version(mpack, mpack_version)


def get_conf_dir(mpack=None, mpack_instance=None, subgroup_name='default', module_name=None, components_map=None):
  """
  Use case: retrieve conf directory paths for a given component instances based on the granularity specified
            ranging from: mpack, mpack-instance, subgroup-name, module-name and map of component instance
            AND with a filtering on each level

  Granularity works only while names for all consecutive levels are specified.
  Levels: mpack/instance/subgroup/module
  E.g If only mpack and subgroup names are specified, the granularity will work only on mpack level,
      though the subgroup filer will be applied. But if the instance name is specified also, than only granular output
      of subgroup will be returned.

  Components are provided as map with key as 'component type' and value as 'list of individual component instances
  names' OR empty map for all component instances present
  """
  return parse_mpack_instances_with_filtering(mpack, mpack_instance, subgroup_name, module_name, components_map,
                                              output_conf_dir=True)


def list_instances(mpack=None, mpack_instance=None, subgroup_name='default', module_name=None, components_map=None):
  """
  Use case: figure out the versions a given component instances based on the granularity specified
            ranging from: mpack, mpack-instance, subgroup-name, module-name and map of component instance
            AND with a filtering on each level

  Granularity works only while names for all consecutive levels are specified.
  Levels: mpack/instance/subgroup/module
  E.g If only mpack and subgroup names are specified, the granularity will work only on mpack level,
      though the subgroup filer will be applied. But if the instance name is specified also, than only granular output
      of subgroup will be returned.

  Components are provided as map with key as 'component type' and value as 'list of individual component instances
  names' OR empty map for all component instances present
  """
  return parse_mpack_instances_with_filtering(mpack, mpack_instance, subgroup_name, module_name, components_map,
                                              output_path=True)


def parse_mpack_instances_with_filtering(mpack_name_filter, instance_name_filter, subgroup_name_filter,
                                         module_name_filter, components_name_filter_map, output_conf_dir=False,
                                         output_path=False):
  instances = MpackInstance.parse_instances_with_filtering(os.path.join(ROOT_FOLDER_PATH, INSTANCES_FOLDER_NAME),
                                                           mpack_name_filter,
                                                           instance_name_filter, subgroup_name_filter,
                                                           module_name_filter,
                                                           components_name_filter_map)
  full_json_output = build_json_output(instances, output_conf_dir=output_conf_dir, output_path=output_path)

  granular_json_output = build_granular_output(full_json_output, mpack_name_filter, instance_name_filter,
                                               subgroup_name_filter,
                                               module_name_filter)

  return granular_json_output


def build_granular_output(json_output, mpack_name_filter, instance_name_filter, subgroup_name_filter,
                          module_name_filter):
  if mpack_name_filter:
    json_output = json_output['mpacks'][mpack_name_filter]
    if instance_name_filter:
      json_output = json_output[MpackInstance.plural_name][instance_name_filter]
      if subgroup_name_filter:
        json_output = json_output['subgroups'][subgroup_name_filter]
        if module_name_filter:
          json_output = json_output[ModuleInstance.plural_name][module_name_filter]
  return json_output


def build_json_output_from_instances_dict(instances_dict, plural_name, output_conf_dir, output_path):
  result = {}
  for instance_name in instances_dict:
    result[instance_name] = instances_dict[instance_name].build_json_output(output_conf_dir, output_path)

  return {plural_name: result}


# FIXME use mpack.json to determine category
def find_module_category(path, module_name):
  walk = os.walk(os.path.join(path, module_name))
  if CONFIGS_DIRECTORY_NAME in next(walk)[1]:
    return "CLIENT"
  return "SERVER"


def build_json_output(instances, output_conf_dir=False, output_path=False):
  result = {}
  for mpack_name in instances.keys():
    result[mpack_name] = build_json_output_from_instances_dict(instances[mpack_name], MpackInstance.plural_name,
                                                               output_conf_dir, output_path)
  return {'mpacks': result}


class MetaMpack:
  def __init__(self, name, version, component_module_map, mpack_json):
    self.name = name
    self.version = version
    self.component_module_map = component_module_map
    self.mpack_json = mpack_json
    self.module_categoty_map = self.parse_mpack_json_into_module_category_map()
    self.module_component_types_map = self.parse_mpack_json_into_module_component_types_map()

  def parse_mpack_json_into_module_category_map(self):
    result = {}
    if not self.mpack_json:
      return None
    for module in self.mpack_json['modules']:
      result[module['id']] = module['category']

    return result

  def parse_mpack_json_into_module_component_types_map(self):
    result = {}
    if not self.mpack_json:
      return None
    for module in self.mpack_json['modules']:
      result[module['id']] = []
      for component in module['components']:
        result[module['id']].append(component['id'])

    return result

  def get_component_category(self, component_type):
    for module in self.module_component_types_map:
      if component_type in self.module_component_types_map[module]:
        return self.module_categoty_map[module]
    return None

  @staticmethod
  def parse_mpack(path, name, version):
    # build components map from soft links
    component_module_map = {}
    for filename in os.listdir(path):
      if os.path.islink(os.path.join(path, filename)):
        component_module_map[filename] = os.path.realpath(os.path.join(path, filename))

    return MetaMpack(name=name,
                     version=version,
                     component_module_map=component_module_map,
                     mpack_json=json.load(open(os.path.join(path, MPACK_JSON_FILE_NAME))))

  @staticmethod
  def parse_mpacks(path):
    result = {}
    walk = os.walk(path)
    for mpack_name in next(walk)[1]:
      result[mpack_name] = MetaMpack.parse_into_mpack_objects(os.path.join(path, mpack_name))
    return result

  @staticmethod
  def parse_into_mpack_objects(path):
    result = {}
    walk = os.walk(path)
    for mpack_version in next(walk)[1]:
      result[mpack_version] = MetaMpack.parse_mpack(path=os.path.join(path, mpack_version),
                                                    name=os.path.basename(path),
                                                    version=mpack_version)
    return result


class Instance:
  def build_json_output(self, output_conf_dir, output_path):
    raise NotImplementedError("Should have implemented this")


class MpackInstance(Instance):
  plural_name = "mpack-instances"

  def __init__(self, mpack_name, instance_name, groups_dict):
    self.mpack_name = mpack_name
    self.instance_name = instance_name
    self.groups_dict = groups_dict

  def build_json_output(self, output_conf_dir, output_path):
    result = {}
    for group in self.groups_dict.keys():
      result[group] = build_json_output_from_instances_dict(self.groups_dict[group], ModuleInstance.plural_name,
                                                            output_conf_dir, output_path)
    return {"subgroups": result, 'name': self.instance_name}

  def set_new_version(self, mpack_name, mpack_version):
    for subgroup_name in self.groups_dict:
      for module_name in self.groups_dict[subgroup_name]:
        self.groups_dict[subgroup_name][module_name].set_new_version(mpack_name, mpack_version)

  @staticmethod
  def parse_into_mpack_instance_dict(path, mpack_name, instance_name_filter, subgroup_name_filter,
                                     module_name_filter, components_filter, components_name_filter_map):
    result = {}
    walk = os.walk(os.path.join(path, mpack_name))
    for instance_name in next(walk)[1]:
      if (
            not instance_name_filter or instance_name_filter == instance_name) and instance_name != DEFAULT_MPACK_INSTANCE_NAME:
        mpack_instance_object = MpackInstance.parse_into_mpack_instance_object(path, mpack_name,
                                                                               instance_name,
                                                                               subgroup_name_filter,
                                                                               module_name_filter,
                                                                               components_filter,
                                                                               components_name_filter_map)
        if mpack_instance_object:
          result[instance_name] = mpack_instance_object
    return result

  @staticmethod
  def create_mpack_instance(mpack_name, mpack_version, mpack_instance, subgroup_name, module_name,
                            components,
                            components_map):
    ModuleInstance.create_module_instance(mpack_name, mpack_version, mpack_instance, subgroup_name,
                                          module_name, components, components_map)

    default_mpack_instance_symlink = os.path.join(ROOT_FOLDER_PATH, INSTANCES_FOLDER_NAME, mpack_name,
                                                  DEFAULT_MPACK_INSTANCE_NAME)
    if not os.path.lexists(default_mpack_instance_symlink):
      os.symlink(os.path.join(ROOT_FOLDER_PATH, INSTANCES_FOLDER_NAME, mpack_name, mpack_instance),
                 default_mpack_instance_symlink)

  @staticmethod
  def parse_instances_with_filtering(path, mpack_name_filter=None, instance_name_filter=None, subgroup_name_filter=None,
                                     module_name_filter=None, components_filter=None, components_name_filter_map=None):
    result = {}
    walk = os.walk(path)
    for mpack_name in next(walk)[1]:
      if not mpack_name_filter or mpack_name_filter == mpack_name:
        mpack_instance_dict = MpackInstance.parse_into_mpack_instance_dict(path, mpack_name,
                                                                           instance_name_filter,
                                                                           subgroup_name_filter,
                                                                           module_name_filter,
                                                                           components_filter,
                                                                           components_name_filter_map)
        if mpack_instance_dict:
          result[mpack_name] = mpack_instance_dict
    return result

  @staticmethod
  def parse_into_mpack_instance_object(root_path, mpack_name, instance_name, subgroup_name_filter=None,
                                       module_name_filter=None, components_filter=None,
                                       components_name_filter_map=None):
    full_path = os.path.join(root_path, mpack_name, instance_name)

    # return None if instance doesn't exist
    if not os.path.exists(full_path):
      return None

    # build groups dictionary
    groups_dict = {}
    walk = os.walk(full_path)
    for group_name in next(walk)[1]:
      if not subgroup_name_filter or subgroup_name_filter == group_name:
        module_instance_dict = ModuleInstance.parse_into_module_instance_dict(
          os.path.join(full_path, group_name), module_name_filter, components_filter, components_name_filter_map)
        if module_instance_dict:
          groups_dict[group_name] = module_instance_dict

    if not groups_dict:
      return None
    return MpackInstance(mpack_name, instance_name, groups_dict)


class ModuleInstance(Instance):
  plural_name = "modules"

  def __init__(self, module_name, components_map, category):
    self.module_name = module_name
    self.components_map = components_map
    self.category = category

  @staticmethod
  def parse_into_module_instance_dict(path, module_name_filter, components_filter, components_name_filter_map):
    result = {}
    walk = os.walk(path)
    for module_name in next(walk)[1]:
      if not module_name_filter or module_name_filter == module_name:

        module_category = find_module_category(path, module_name)

        if module_category == "CLIENT":
          components_map = ComponentInstance(name=DEFAULT_COMPONENT_INSTANCE_NAME,
                                             component_path=os.path.join(path, module_name),
                                             path_exec=os.path.realpath(
                                               os.path.join(path, module_name, CURRENT_SOFTLINK_NAME)), is_client=True)
        else:
          components_map = ComponentInstance.parse_into_components_dict(os.path.join(path, module_name),
                                                                        components_filter,
                                                                        components_name_filter_map)
        if components_map:
          result[module_name] = ModuleInstance(module_name, components_map, module_category)
    return result

  @staticmethod
  def create_module_instance(mpack_name, mpack_version, mpack_instance, subgroup_name, module_name,
                             components,
                             components_map):
    meta_mpack = MetaMpack.parse_mpack(
      path=os.path.join(ROOT_FOLDER_PATH, MPACKS_FOLDER_NAME, mpack_name, mpack_version),
      name=mpack_name,
      version=mpack_version)

    is_client_module = meta_mpack.module_categoty_map[module_name] == "CLIENT"
    if components:
      if components == '*':
        components = meta_mpack.module_component_types_map[module_name]
      for component_type in components:
        ComponentInstance.create_component_instance(mpack_name, mpack_version, mpack_instance, subgroup_name,
                                                    module_name, component_type, DEFAULT_COMPONENT_INSTANCE_NAME,
                                                    is_client_module)
    else:
      for component_type in components_map:
        for component_instance_name in components_map[component_type]:
          ComponentInstance.create_component_instance(mpack_name, mpack_version, mpack_instance, subgroup_name,
                                                      module_name, component_type, component_instance_name,
                                                      is_client_module)

  def set_new_version(self, mpack_name, mpack_version):
    if self.category == 'CLIENT':
      component_instance = self.components_map
      print("\nSetting new version for component : " + component_instance.component_path)
      component_instance.set_new_version(mpack_name, mpack_version, self.module_name)
    else:
      for component_type in self.components_map:
        for component_name in self.components_map[component_type]:
          component_instance = self.components_map[component_type][component_name]
          print("\nSetting new version for component : " + component_instance.component_path)
          component_instance.set_new_version(mpack_name, mpack_version, component_type)

  def build_json_output(self, output_conf_dir, output_path):
    result = {}
    if self.category == 'CLIENT':
      result['component_instances'] = {'default': self.components_map.build_json_output(output_conf_dir, output_path)}
    else:
      for component_type in self.components_map.keys():
        result[component_type] = build_json_output_from_instances_dict(self.components_map[component_type],
                                                                       ComponentInstance.plural_name,
                                                                       output_conf_dir, output_path)
      result = {'components': result}

    result['category'] = self.category
    result['name'] = self.module_name
    return result


class ComponentInstance(Instance):
  plural_name = "component-instances"

  def __init__(self, name, component_path, path_exec, is_client=False):
    self.name = name
    self.component_path = component_path
    self.path_exec = path_exec
    self.is_client = is_client

  def set_new_version(self, mpack_name, mpack_version, component_type):
    mpack_path = os.path.join(ROOT_FOLDER_PATH, MPACKS_FOLDER_NAME, mpack_name, mpack_version, component_type)
    target_link = os.path.join(self.component_path, CURRENT_SOFTLINK_NAME)
    if os.path.lexists(target_link):
      os.remove(target_link)
      print "\nRemoved old link " + target_link

    os.symlink(mpack_path, target_link)
    print "\nCreated new link " + target_link + " -> " + mpack_path

  @staticmethod
  def parse_into_component_instance_dict(path, component_names_filter=None):
    result = {}
    walk = os.walk(path)
    for component_instance_name in next(walk)[1]:
      if not component_names_filter or component_instance_name in component_names_filter:
        result[component_instance_name] = ComponentInstance(name=component_instance_name,
                                                            component_path=os.path.join(path, component_instance_name),
                                                            path_exec=os.path.realpath(
                                                              os.path.join(path, component_instance_name,
                                                                           CURRENT_SOFTLINK_NAME)))
    return result

  @staticmethod
  def parse_into_components_dict(path, components_filter, components_name_filter_map):
    result = {}
    walk = os.walk(path)
    for component_type in next(walk)[1]:
      if components_filter:
        if components_filter == '*':
          result[component_type] = ComponentInstance.parse_into_component_instance_dict(
            os.path.join(path, component_type))
        else:
          component_instance_dict = ComponentInstance.parse_into_component_instance_dict(
            os.path.join(path, component_type), components_filter)
          if component_instance_dict:
            result[component_type] = component_instance_dict
      elif not components_name_filter_map:
        result[component_type] = ComponentInstance.parse_into_component_instance_dict(
          os.path.join(path, component_type))
      elif component_type in components_name_filter_map.keys():
        component_instance_dict = ComponentInstance.parse_into_component_instance_dict(
          os.path.join(path, component_type), components_name_filter_map[component_type])
        if component_instance_dict:
          result[component_type] = component_instance_dict
    return result

  @staticmethod
  def create_component_instance(mpack_name, mpack_version, mpack_instance, subgroup_name, module_name,
                                component_type, component_instance_name, is_client_module):
    if is_client_module:
      component_path = os.path.join(ROOT_FOLDER_PATH, INSTANCES_FOLDER_NAME, mpack_name, mpack_instance, subgroup_name,
                                    component_type)
    else:
      component_path = os.path.join(ROOT_FOLDER_PATH, INSTANCES_FOLDER_NAME, mpack_name, mpack_instance, subgroup_name,
                                    module_name, component_type, component_instance_name)
    mpack_path = os.path.join(ROOT_FOLDER_PATH, MPACKS_FOLDER_NAME, mpack_name, mpack_version, component_type)

    if not os.path.lexists(mpack_path):
      raise ValueError("Path doesn't exist: " + mpack_path)

    os.makedirs(component_path)

    os.symlink(mpack_path, os.path.join(component_path, CURRENT_SOFTLINK_NAME))

    os.makedirs(os.path.join(component_path, CONFIGS_DIRECTORY_NAME))
    print "\n Created " + component_path

  def build_json_output(self, output_conf_dir, output_path):
    result = {'name': self.name}
    if output_conf_dir:
      result['config_dir'] = os.path.join(self.component_path, CONFIGS_DIRECTORY_NAME)
    if output_path:
      result['path'] = self.path_exec
    return result
