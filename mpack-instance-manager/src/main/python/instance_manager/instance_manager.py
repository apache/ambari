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

ROOT_FOLDER_PATH = "/usr/hwx/"

INSTANCES_FOLDER_NAME = "instances"
MODULES_FOLDER_NAME = "modules"
MPACKS_FOLDER_NAME = "mpacks"
DEFAULT_COMPONENT_INSTANCE_NAME = 'default'
DEFAULT_MPACK_INSTANCE_NAME = 'default'
DEFAULT_SUBGROUP_NAME = 'default'
CLIENT_CATEGORY = "CLIENT"
SERVER_CATEGORY = "SERVER"


def create_mpack(mpack_name, mpack_version, mpack_instance, subgroup_name=DEFAULT_SUBGROUP_NAME, module_name=None,
                 components=None, components_map=None):
  """
  Use case 1: Creates an instance of mpack with a new subgroup, new module and one or more component(s)

  Use case 2: Creates/adds in an existing mpack either a : new subgroup and/or module and/or component instance.

  Components are provided as map with key as 'component type' and value as 'list of individual component instances
  names' OR empty map to create single instance of all components with name 'default'
  OR
  list of 'components_instances_name' to be created. (default) OR '*' for all components
  """
  mpack_name = mpack_name.lower()
  module_name = module_name.lower()

  validate_mpack_for_creation_or_changing(mpack_name, mpack_version, module_name, components, components_map)

  MpackInstance.create_mpack_instance(mpack_name, mpack_version, mpack_instance, subgroup_name, module_name,
                                      components, components_map)


def set_mpack_instance(mpack, mpack_version, mpack_instance, subgroup_name=DEFAULT_SUBGROUP_NAME, module_name=None,
                       components=None, components_map=None):
  """
  Use case: move a given component instances from one version to the next version by modifying the soft links.
  Eg: Moving hive_server instance 'HS-1' from version 1.0.0-b1 to 1.5.0-b1.

  Components are provided as map with key as 'component type' and value as 'list of individual component instances
  names' OR empty map to create single instance of all components with name 'default'
  OR
  list of 'components_instances_name' to be created. (default) OR '*' for all components
  """
  mpack = mpack.lower()
  module_name = module_name.lower()

  instances = MpackInstance.parse_instances_with_filtering(os.path.join(ROOT_FOLDER_PATH, INSTANCES_FOLDER_NAME), mpack,
                                                           mpack_instance, subgroup_name, module_name,
                                                           components, components_map)
  if not instances:
    raise ValueError("Found no instances for the given filters.")

  validate_mpack_for_creation_or_changing(mpack, mpack_version, module_name, components, components_map)

  for mpack in instances:
    for mpack_instance_name in instances[mpack]:
      instances[mpack][mpack_instance_name].set_new_version(mpack, mpack_version)


def get_conf_dir(mpack=None, mpack_instance=None, subgroup_name=DEFAULT_SUBGROUP_NAME, module_name=None,
                 components_map=None):
  """
  Use case: retrieve conf directory paths for a given component instances based on the granularity specified
            ranging from: mpack, mpack-instance, subgroup-name, module-name and map of component instance
            AND with a filtering on each level

  Granularity works only while names for all consecutive levels are specified.
  Note that subgroup has default value of 'default'
  Levels: mpack/instance/subgroup/module
  E.g If only mpack and subgroup names are specified, the granularity will work only on mpack level,
      though the subgroup fitler will be applied. But if the instance name is specified also, than only granular output
      of subgroup will be returned.

  Components are provided as map with key as 'component type' and value as 'list of individual component instances
  names' OR empty map for all component instances present
  """
  return build_granular_json_with_filtering(mpack, mpack_instance, subgroup_name, module_name, components_map,
                                            output_conf_dir=True)


def list_instances(mpack=None, mpack_instance=None, subgroup_name=DEFAULT_SUBGROUP_NAME, module_name=None,
                   components_map=None):
  """
  Use case: figure out the versions a given component instances based on the granularity specified
            ranging from: mpack, mpack-instance, subgroup-name, module-name and map of component instance
            AND with a filtering on each level

  Granularity works only while names for all consecutive levels are specified.
  Note that subgroup has default value of 'default'
  Levels: mpack/instance/subgroup/module
  E.g If only mpack and subgroup names are specified, the granularity will work only on mpack level,
      though the subgroup fitler will be applied. But if the instance name is specified also, than only granular output
      of subgroup will be returned.

  Components are provided as map with key as 'component type' and value as 'list of individual component instances
  names' OR empty map for all component instances present
  """
  return build_granular_json_with_filtering(mpack, mpack_instance, subgroup_name, module_name, components_map,
                                            output_path=True)


def build_granular_json_with_filtering(mpack_name_filter, instance_name_filter, subgroup_name_filter,
                                       module_name_filter, components_name_filter_map, output_conf_dir=False,
                                       output_path=False):
  """
  Builds the json that contains all instances specified in filters or all instances if filters are not specified.
  The level of granularity depends on the consecutive levels of specified filters
  Levels: mpack/instance/subgroup/module
  E.g If only mpack and subgroup names are specified, the granularity will work only on mpack level,
      though the subgroup fitler will be applied. But if the instance name is specified also, than only granular output
      of subgroup will be returned.

  The output_conf_dir or output_path for each component instance will be included in json depending on given parameters.
  """

  if mpack_name_filter:
    mpack_name_filter = mpack_name_filter.lower()
  if module_name_filter:
    module_name_filter = module_name_filter.lower()

  instances = MpackInstance.parse_instances_with_filtering(os.path.join(ROOT_FOLDER_PATH, INSTANCES_FOLDER_NAME),
                                                           mpack_name_filter,
                                                           instance_name_filter, subgroup_name_filter,
                                                           module_name_filter,
                                                           None,
                                                           components_name_filter_map)
  if not instances:
    raise ValueError("Found no instances for the given filters.")

  full_json_output = build_json_output(instances, output_conf_dir=output_conf_dir, output_path=output_path)

  granular_json_output = build_granular_output(full_json_output, mpack_name_filter, instance_name_filter,
                                               subgroup_name_filter,
                                               module_name_filter)

  return granular_json_output


def build_granular_output(json_output, mpack_name_filter, instance_name_filter, subgroup_name_filter,
                          module_name_filter):
  """
  Returns the part of original json using the granularity filters

  The level of granularity depends on the consecutive levels of specified filters
  Levels: mpack/instance/subgroup/module
  E.g If only mpack and subgroup names are specified, the granularity will work only on mpack level,
      But if the instance name is specified also, than only granular output of subgroup will be returned.
  """
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
  """
  Build the json from the dictionary of Instance objects.
  The plural_name is used to form the upper level of the json output.
  """
  result = {}
  for instance_name in instances_dict:
    result[instance_name] = instances_dict[instance_name].build_json_output(output_conf_dir, output_path)

  return {plural_name: result}


# Better use it with component instance path.
# Need to be careful with this method as it will return only the single meta mpack whilst the module component instances
# may be from different mpack versions and point to different mpack.json's
def find_link_to_current_in_path_recursive(root_path):
  if not os.path.lexists(os.path.join(root_path, CURRENT_SOFTLINK_NAME)):
    walk = os.walk(root_path)
    for folder_name in next(walk)[1]:
      folder_search_result = find_link_to_current_in_path_recursive(os.path.join(root_path, folder_name))
      if folder_search_result:
        return folder_search_result
      else:
        return None
  else:
    return os.path.join(root_path, CURRENT_SOFTLINK_NAME)


def get_module_meta_mpack(path, module_name):
  current_link_location = find_link_to_current_in_path_recursive(os.path.join(path, module_name))
  current_target = os.readlink(current_link_location)
  return MetaMpack.parse_mpack(os.path.dirname(current_target))


def build_json_output(instances, output_conf_dir=False, output_path=False):
  result = {}
  for mpack_name in instances.keys():
    result[mpack_name] = build_json_output_from_instances_dict(instances[mpack_name], MpackInstance.plural_name,
                                                               output_conf_dir, output_path)
  return {'mpacks': result}


def validate_mpack_for_creation_or_changing(mpack_name, mpack_version, module_name, components, components_map):
  mpack_root_path = os.path.join(ROOT_FOLDER_PATH, MPACKS_FOLDER_NAME, mpack_name)
  if not os.path.exists(mpack_root_path):
    raise ValueError("Mpack {0} doesn't exist, please check mpack name.".format(mpack_name))

  mpack_version_path = os.path.join(mpack_root_path, mpack_version)
  if not os.path.exists(mpack_version_path):
    raise ValueError(
      "Mpack version {0} doesn't exist for mpack {1}, please check mpack name and version".format(mpack_version,
                                                                                                  mpack_name))

  meta_mpack = MetaMpack.parse_mpack(mpack_version_path)

  if not module_name in meta_mpack.module_component_types_map:
    raise ValueError(
      "There is no module {0} for mpack {1} with version {2}. Please check mpack name, version and module name".format(
        module_name, mpack_name, mpack_version))

  if components and components != "*":
    for component in components:
      if not meta_mpack.get_component_category(component):
        raise ValueError(
          "There is no component {0} in module {1} for mpack {2} with version {3}."
          " Please check mpack name, version, module name and component name".format(
            component, module_name, mpack_name, mpack_version))

  if components_map:
    for component in components_map:
      if component not in meta_mpack.module_component_types_map[module_name]:
        raise ValueError(
          "There is no component {0} in module {1} for mpack {2} with version {3}."
          " Please check mpack name, version, module name and component name".format(
            component, module_name, mpack_name, mpack_version))


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
  def parse_mpack(path):
    # build components map from soft links
    component_module_map = {}
    for filename in os.listdir(path):
      if os.path.islink(os.path.join(path, filename)):
        component_module_map[filename] = os.path.realpath(os.path.join(path, filename))

    mpack_json_path = os.path.join(path, MPACK_JSON_FILE_NAME)
    if not os.path.exists(mpack_json_path):
      raise ValueError(
        "{0} file is missing. The exact location should be {1}".format(MPACK_JSON_FILE_NAME, mpack_json_path))

    with open(mpack_json_path, "r") as json_file:
      json_file_content = json_file.read()

    try:
      mpack_json = json.loads(json_file_content)
    except ValueError as e:
      raise ValueError("The {0} is invalid. Location: {1}. Error message: {2}".format(
        MPACK_JSON_FILE_NAME, mpack_json_path, e.message))

    mpack_version = os.path.basename(path)
    mpack_name = os.path.basename(os.path.dirname(path))
    return MetaMpack(name=mpack_name,
                     version=mpack_version,
                     component_module_map=component_module_map,
                     mpack_json=mpack_json)

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
      result[mpack_version] = MetaMpack.parse_mpack(path=os.path.join(path, mpack_version))
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
    if not os.path.exists(path):
      raise ValueError("There are no created instances. Use create-mpack-instance command to add them.")
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
    # The problem here is that for client component this folder name means the component type, while for server modules
    # it's module name
    for folder_name in next(walk)[1]:
      components_map = None

      # find out the module category and name
      meta_mpack = get_module_meta_mpack(path, folder_name)
      if folder_name in meta_mpack.module_categoty_map:
        module_category = meta_mpack.module_categoty_map[folder_name]
        module_name = folder_name
      else:
        module_category = meta_mpack.get_component_category(folder_name)
        module_name = os.path.basename(os.path.dirname(meta_mpack.component_module_map[folder_name]))

      if not module_name_filter or module_name == module_name_filter:
        if module_category == CLIENT_CATEGORY:
          if ((not components_filter and not components_name_filter_map) or
                (components_filter and (components_filter == '*' or folder_name in components_filter)) or
                (components_name_filter_map and folder_name in components_name_filter_map)):
            components_map = ComponentInstance(name=DEFAULT_COMPONENT_INSTANCE_NAME,
                                               component_path=os.path.join(path, folder_name),
                                               path_exec=os.path.realpath(
                                                 os.path.join(path, folder_name, CURRENT_SOFTLINK_NAME)))
        else:
          components_map = ComponentInstance.parse_into_components_dict(os.path.join(path, folder_name),
                                                                        components_filter,
                                                                        components_name_filter_map)
        if components_map:
          result[folder_name] = ModuleInstance(folder_name, components_map, module_category)
    return result

  @staticmethod
  def create_module_instance(mpack_name, mpack_version, mpack_instance, subgroup_name, module_name,
                             components,
                             components_map):
    meta_mpack = MetaMpack.parse_mpack(
      path=os.path.join(ROOT_FOLDER_PATH, MPACKS_FOLDER_NAME, mpack_name, mpack_version))

    is_client_module = meta_mpack.module_categoty_map[module_name] == CLIENT_CATEGORY
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
    if self.category == CLIENT_CATEGORY:
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
    if self.category == CLIENT_CATEGORY:
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

  def __init__(self, name, component_path, path_exec):
    self.name = name
    self.component_path = component_path
    self.path_exec = path_exec

  def set_new_version(self, mpack_name, mpack_version, component_type):
    mpack_path = os.path.join(ROOT_FOLDER_PATH, MPACKS_FOLDER_NAME, mpack_name, mpack_version, component_type)
    target_link = os.path.join(self.component_path, CURRENT_SOFTLINK_NAME)
    if os.path.lexists(target_link):
      if os.readlink(target_link) == mpack_path:
        print "\n{0} already points to {1}. Skipping.".format(target_link, mpack_path)
        return
      os.remove(target_link)

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
        elif component_type in components_filter:
          component_instance_dict = ComponentInstance.parse_into_component_instance_dict(
            os.path.join(path, component_type))
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

    if os.path.exists(component_path):
      raise ValueError(
        "The instance {0} already exist. To change the version use set-mpack-instance command".format(component_path))

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
