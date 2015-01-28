#!/usr/bin/env python

'''
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
'''


"""
Upgrade catalog file format description:


Format version 1.0

Global section description:
  STACKNAME - name of stack, for example HDP
  OLDVERSION - version of stack from which upgrade should be done, used by fromStack script argument
  NEWVERSION - version of stack to which upgrade should be done, used by toStack script argument

Sub-section options:
  config-types - contains global per-config settings
    merged-copy - would merge latest server properties with properties defined in "properties" section,
                  without this option server properties would be rewritten by properties defined in "properties" section

Sub-section properties - Contains property definition
Sub-section property-mapping(optional) - contains mapping of property names in case, if some property changed their name in NEWVERSION

Example:

{
  "version": "1.0",
  "stacks": [
    {
      "name": "STACKNAME",
      "old-version": "OLDVERSION",
      "target-version": "NEWVERSION",
      "options": {
        "config-types": {
          "CONFIGTYPE1": {
            "merged-copy": "yes"
          }
        }
      },
      "properties": {
        "CONFIGTYPE1": {
          "some_property": "some property value",
          "some_second_property: {
             "remove": "yes"
          },
          "template_property": {
           "value": "{TEMPLATE_TAG}",
           "template": "yes"
          }
        }
      },
     "property-mapping": {
       "old-property-name": "new-property-name"
     }
    }
  ]
}

More examples available in ambari-server/src/main/resources/upgrade/catalog/
"""

import getpass
import optparse
from pprint import pprint
import re
import sys
import datetime
import os.path
import logging
import shutil
import json
import subprocess
import time


# ==============================
#    Error classes definition
# ==============================
class FatalException(Exception):
  def __init__(self, code, reason):
    self.code = code
    self.reason = reason

  def __str__(self):
    return repr("Fatal exception: %s, exit code %s" % (self.reason, self.code))

  def _get_message(self):
    return str(self)


class ReadOnlyPropertyException(Exception):
  def __str__(self):
    return "Property is read-only"

  def _get_message(self):
    return self.__str__()

class NotSupportedCatalogVersion(Exception):
  def __init__(self, catalog_version):
    self._version = catalog_version

  def __str__(self):
    return "Version %s of loaded catalog not supported" % self._version

  def _get_message(self):
    return self.__str__()

  message = property(__str__)


# ==============================
#    Constant class definition
# ==============================
class Const(object):
  def __new__(cls, *args, **kwargs):
    raise Exception("Class couldn't be created")


class CatConst(Const):
  VERSION_TAG = "version"
  STACK_VERSION_OLD = "old-version"
  STACK_VERSION_TARGET = "target-version"
  STACK_STAGS_TAG = "stacks"
  STACK_NAME = "name"
  CONFIG_OPTIONS = "options"
  CONFIG_TYPES = "config-types"
  STACK_PROPERTIES = "properties"
  PROPERTY_VALUE_TAG = "value"
  PROPERTY_REMOVE_TAG = "remove"
  MERGED_COPY_TAG = "merged-copy"
  ITEMS_TAG = "items"
  TYPE_TAG = "type"
  TRUE_TAG = "yes"
  STACK_PROPERTIES_MAPPING_LIST_TAG = "property-mapping"
  VALUE_TEMPLATE_TAG = "template"
  SEARCH_PATTERN = "(\{[^\{\}]+\})"  # {XXXXX}


class Options(Const):
  # action commands
  API_PROTOCOL = "http"
  API_PORT = "8080"

  GET_MR_MAPPING_ACTION = "save-mr-mapping"
  VERIFY_ACTION = "verify"
  DELETE_MR_ACTION = "delete-mr"
  ADD_YARN_MR2_ACTION = "add-yarn-mr2"
  MODIFY_CONFIG_ACTION = "update-configs"
  BACKUP_CONFIG_ACTION = "backup-configs"
  INSTALL_YARN_MR2_ACTION = "install-yarn-mr2"

  MR_MAPPING_FILE = "mr_mapping"
  CAPACITY_SCHEDULER_TAG = "capacity-scheduler"
  REPLACE_JH_HOST_NAME_TAG = "REPLACE_JH_HOST"
  REPLACE_RM_HOST_NAME_TAG = "REPLACE_RM_HOST"
  REPLACE_WITH_TAG = "REPLACE_WITH_"
  DELETE_OLD_TAG = "DELETE_OLD"

  ZOOKEEPER_SERVER = "ZOOKEEPER_SERVER"

  MR_MAPPING = None
  logger = None

  # Api constants
  ROOT_URL = None
  CLUSTER_URL = None
  COMPONENTS_FORMAT = None

  # Curl options
  POST_REQUESTS = ['PUT', 'POST']
  GET_REQUESTS = ['GET', 'DELETE']
  CURL_PRINT_ONLY = None

  ARGS = None
  OPTIONS = None
  HOST = None
  CLUSTER_NAME = None

  # for verify action
  REPORT_FILE = None

  API_TOKENS = {
    "user": None,
    "pass": None
  }

  HEADERS = {
    'X-Requested-By': 'upgradeHelper'
  }

  @classmethod
  def initialize(cls):
    cls.ROOT_URL = '%s://%s:%s/api/v1' % (cls.API_PROTOCOL, cls.HOST, cls.API_PORT)
    cls.CLUSTER_URL = cls.ROOT_URL + "/clusters/%s" % cls.CLUSTER_NAME
    cls.COMPONENTS_FORMAT = cls.CLUSTER_URL + "/components/{0}"

  @classmethod
  def initialize_logger(cls, filename=None):
    cls.logger = logging.getLogger('UpgradeHelper')
    cls.logger.setLevel(logging.DEBUG)

    if filename is not None:
      handler = logging.FileHandler(filename)
      handler.setFormatter(logging.Formatter('%(asctime)s %(levelname)s - %(message)s'))
      cls.logger.addHandler(handler)
      cls.logger.info("")
      cls.logger.info("Start new logging section")

    handler = logging.StreamHandler(sys.stdout)
    handler.setFormatter(logging.Formatter('%(levelname)s: %(message)s'))
    cls.logger.addHandler(handler)


# ==============================
#    Catalog classes definition
# ==============================
class UpgradeCatalogFactory(object):

   # versions of catalog which is currently supported
  _supported_catalog_versions = ["1.0"]

  # private variables
  _json_catalog = None

  def __init__(self, path):
    self._load(path)

  def _load(self, path):
    f = None
    try:
      f = open(path, 'r')
      json_string = f.read()
      self._json_catalog = json.loads(json_string)
      self._parse_upgrade_catalog()
    except IOError as e:
      raise FatalException(e.errno, "Couldn't open upgrade catalog file %s: %s" % (path, e.strerror))
    except NotSupportedCatalogVersion as e:
      raise FatalException(1, e.message)
    except ValueError as e:
      raise FatalException(1, "Malformed upgrade catalog: %s" % e.message)
    finally:
      try:
        if f is not None:
          f.close()
      except IOError as e:
        pass

  def _parse_upgrade_catalog(self):
    catalog_version = None
    if CatConst.VERSION_TAG in self._json_catalog:
      catalog_version = self._json_catalog[CatConst.VERSION_TAG]

    if catalog_version is None or catalog_version not in self._supported_catalog_versions:
      raise NotSupportedCatalogVersion(str(catalog_version))

  def get_catalog(self, from_version=None, to_version=None):
    search_version = {
      CatConst.STACK_VERSION_OLD: from_version,
      CatConst.STACK_VERSION_TARGET: to_version
    }

    for stack in self._json_catalog[CatConst.STACK_STAGS_TAG]:
      version = {
        CatConst.STACK_VERSION_OLD: stack[CatConst.STACK_VERSION_OLD],
        CatConst.STACK_VERSION_TARGET: stack[CatConst.STACK_VERSION_TARGET]
      }
      if version == search_version:
        return UpgradeCatalog(catalog=stack, version=version)

    return None


class UpgradeCatalog(object):

  # private variables
  _json_catalog = None
  _properties_catalog = None
  _properties_map_catalog = {}  # Initially should be assigned empty dictionary as default value
  _version = None
  _search_pattern = None

  """
   Substitute handler, should return replaced value, as param would be passed value and tokens to substitute
   Please, be aware! Token should be unique in context of one catalog

   Example:
    def _substitute(tokens, value):
      for token in tokens:
        if token == "{REPLACE_ME}":
          value = value.replace(token, "\"hello world\"")
      return value

    catalog.set_substitution_handler = _substitute

    After that, all properties with CatConst.VALUE_TEMPLATE_TAG  set to "yes" would be processed
  """
  _substitution_handler = None

  # public variable
  config_groups = None

  def __init__(self, catalog=None, version=None, substitution_handler=None):
    self._json_catalog = catalog
    self._version = version
    self._search_pattern = re.compile(CatConst.SEARCH_PATTERN)

    if CatConst.STACK_PROPERTIES in catalog:
      self._properties_catalog = catalog[CatConst.STACK_PROPERTIES]

    if CatConst.STACK_PROPERTIES_MAPPING_LIST_TAG in catalog:
      self._properties_map_catalog = catalog[CatConst.STACK_PROPERTIES_MAPPING_LIST_TAG]

    if catalog is not None and CatConst.CONFIG_OPTIONS in catalog \
                           and CatConst.CONFIG_TYPES in catalog[CatConst.CONFIG_OPTIONS]:

      self.config_groups = ConfigConst(catalog[CatConst.CONFIG_OPTIONS][CatConst.CONFIG_TYPES],
                                       properties_catalog=self._properties_catalog)

    if substitution_handler is not None:
      self.set_substitution_handler(substitution_handler)

  # deprecated, used for compatibility with old code
  def get_properties_as_dict(self, properties):
    target_dict = {}
    for key in properties:
      if CatConst.PROPERTY_VALUE_TAG in properties[key] and CatConst.PROPERTY_REMOVE_TAG not in properties[key]:
        target_dict[key] = properties[key][CatConst.PROPERTY_VALUE_TAG]

    return target_dict

  def set_substitution_handler(self, handler):
    self._substitution_handler = handler

  def _get_version(self):
    return "%s-%s" % (self._version[CatConst.STACK_VERSION_OLD], self._version[CatConst.STACK_VERSION_TARGET])

  def get_parsed_version(self):
    """
     Get numeric representation of the version for comparation purposes

     Example:
       1.3-2.1 will be represented as { from: 13, to: 21 }

    :return: Numeric version
    """
    v_from = self._version[CatConst.STACK_VERSION_OLD].split(".")
    v_to = self._version[CatConst.STACK_VERSION_TARGET].split(".")
    try:
      v_from = int(v_from[0]) * 10 + int(v_from[1])
      v_to = int(v_to[0]) * 10 + int(v_to[1])
    except ValueError:
      v_from = 0
      v_to = 0

    version = {
      "from": v_from,
      "to": v_to
    }

    return version

  def _get_name(self):
    if CatConst.STACK_NAME in self._json_catalog:
      return self._json_catalog[CatConst.STACK_NAME]
    return ""

  def _get_propoerty_mapping(self):
    return self._properties_map_catalog

  def get_properties(self, config_group):
    if config_group in self._properties_catalog:
      return self._filter_properties(config_group)
    return None

  def _filter_properties(self, config_group):
    def _property_filter_strings(value):
      if not isinstance(value, dict):
        return {CatConst.PROPERTY_VALUE_TAG: value}
      else:
        if self._substitution_handler is not None and CatConst.VALUE_TEMPLATE_TAG in value  \
          and CatConst.VALUE_TEMPLATE_TAG in value:  # value contains template

          parsed_value = self._substitution_handler(
            self._search_pattern.findall(value[CatConst.PROPERTY_VALUE_TAG]), value[CatConst.PROPERTY_VALUE_TAG]
          )
          if parsed_value is not None:  # Check if target function returns result
            value[CatConst.PROPERTY_VALUE_TAG] = parsed_value

      return value
    properties = self._properties_catalog[config_group].copy()  # pass to process only copy of data
    properties = dict(zip(properties, map(_property_filter_strings, properties.values())))
    return properties

  version = property(_get_version)
  name = property(_get_name)
  property_map_catalog = property(_get_propoerty_mapping)


class ConfigConst(object):
  _config_types_const_definition = {}
  _config_types_value_definition = {}

  def __init__(self, config_types_definition, properties_catalog=None):
    if properties_catalog is not None:  # compensate possibly undefined config groups in options from property definition
      for item in properties_catalog:
        if item not in config_types_definition:
          config_types_definition[item] = {}

    self._config_types_value_definition = config_types_definition
    for key in config_types_definition:
      self._config_types_const_definition[key.replace("-", "_").lower()] = key

  def list(self):
    return self._config_types_value_definition.keys()

  def get(self, name):
    if name in self._config_types_value_definition:
      return self._config_types_value_definition[name]
    raise Exception("No config group with name %s found" % name)

  def __getattr__(self, item):
    """
    Support for constant handling like "<name>_tag" which would return real config name.
    Base list loaded from section options\config-types of json.

    Example:
      self.hbase_env_tag will return hbase-env

    :param item: accessed attribute
    :return: attribute value if exists or None
    """
    item = item.lower()
    if "_tag" in item and item[:-4] in self._config_types_const_definition:
      return self._config_types_const_definition[item[:-4]]


# Copy file and save with file.# (timestamp)
def backup_file(filePath):
  if filePath is not None and os.path.exists(filePath):
    timestamp = datetime.datetime.now()
    format = '%Y%m%d%H%M%S'
    try:
      shutil.copyfile(filePath, filePath + "." + timestamp.strftime(format))
      os.remove(filePath)
    except Exception as e:
      Options.logger.warn('Could not backup file "%s": %s' % (filePath, str(e)))
  return 0


def write_mapping(hostmapping):
  if os.path.isfile(Options.MR_MAPPING_FILE):
    os.remove(Options.MR_MAPPING_FILE)
  json.dump(hostmapping, open(Options.MR_MAPPING_FILE, 'w'))


def write_config(config, cfg_type, tag):
  file_name = cfg_type + "_" + tag
  if os.path.isfile(file_name):
    os.remove(file_name)
  json.dump(config, open(file_name, 'w'))


def read_mapping():
  if os.path.isfile(Options.MR_MAPPING_FILE):
    if Options.MR_MAPPING is not None:
      return Options.MR_MAPPING
    else:
      Options.MR_MAPPING = json.load(open(Options.MR_MAPPING_FILE))
      return Options.MR_MAPPING
  else:
    raise FatalException(-1, "MAPREDUCE host mapping file, mr_mapping, is not available or badly formatted. Execute "
                             "action save-mr-mapping. Ensure the file is present in the directory where you are "
                             "executing this command.")


def get_mr1_mapping():
  components = ["MAPREDUCE_CLIENT", "JOBTRACKER", "TASKTRACKER", "HISTORYSERVER"]
  GET_URL_FORMAT = Options.CLUSTER_URL + '/services/MAPREDUCE/components/%s'
  hostmapping = {}
  for component in components:
    hostlist = []
    structured_resp = curl(GET_URL_FORMAT % component, parse=True, validate=True, validate_expect_body=True)

    if 'host_components' in structured_resp:
      for hostcomponent in structured_resp['host_components']:
        if 'HostRoles' in hostcomponent:
          if 'host_name' in hostcomponent['HostRoles']:
            hostlist.append(hostcomponent['HostRoles']['host_name'])

    hostmapping[component] = hostlist
  write_mapping(hostmapping)

  pprint("File mr_mapping contains the host mapping for mapreduce components. This file is critical for later "
         "steps.")


def get_YN_input(prompt, default):
  yes = set(['yes', 'ye', 'y'])
  no = set(['no', 'n'])
  return get_choice_string_input(prompt, default, yes, no)


def get_choice_string_input(prompt, default, firstChoice, secondChoice):
  choice = raw_input(prompt).lower()
  if choice in firstChoice:
    return True
  elif choice in secondChoice:
    return False
  elif choice is "":  # Just enter pressed
    return default
  else:
    print "input not recognized, please try again: "
    return get_choice_string_input(prompt, default, firstChoice, secondChoice)


def delete_mr():
  saved_mr_mapping = get_YN_input("Have you saved MR host mapping using action save-mr-mapping [y/n] (n)? ", False)
  if not saved_mr_mapping:
    raise FatalException(1, "Ensure MAPREDUCE host component mapping is saved before deleting it. Use action "
                            "save-mr-mapping.")

  SERVICE_URL_FORMAT = Options.CLUSTER_URL + '/services/MAPREDUCE'
  COMPONENT_URL_FORMAT = Options.CLUSTER_URL + '/hosts/%s/host_components/%s'
  NON_CLIENTS = ["JOBTRACKER", "TASKTRACKER", "HISTORYSERVER"]
  PUT_IN_DISABLED = {
    "HostRoles": {
      "state": "DISABLED"
    }
  }

  hostmapping = read_mapping()

  for key, value in hostmapping.items():
    if (key in NON_CLIENTS) and (len(value) > 0):
      for host in value:
        curl(COMPONENT_URL_FORMAT % (host, key), request_type="PUT", data=PUT_IN_DISABLED,
             validate=True, validate_expect_body=False)

  curl(SERVICE_URL_FORMAT, request_type="DELETE", validate=True, validate_expect_body=False)


def get_cluster_stackname():
  VERSION_URL_FORMAT = Options.CLUSTER_URL + '?fields=Clusters/version'

  structured_resp = curl(VERSION_URL_FORMAT, simulate=False, validate=True, validate_expect_body=True, parse=True)

  if 'Clusters' in structured_resp:
    if 'version' in structured_resp['Clusters']:
      return structured_resp['Clusters']['version']

  raise FatalException(-1, "Unable to get the cluster version")


def has_component_in_stack_def(stack_name, service_name, component_name):
  STACK_COMPONENT_URL_FORMAT = Options.ROOT_URL + '/stacks2/{0}/versions/{1}/stackServices/{2}/serviceComponents/{3}'
  stack, stack_version = stack_name.split('-')

  try:
    curl(STACK_COMPONENT_URL_FORMAT.format(stack,stack_version, service_name, component_name),
          validate=True, validate_expect_body=True, simulate=False)
    return True
  except FatalException:
    return False


def add_services():
  SERVICE_URL_FORMAT = Options.CLUSTER_URL + '/services/{0}'
  COMPONENT_URL_FORMAT = SERVICE_URL_FORMAT + '/components/{1}'
  HOST_COMPONENT_URL_FORMAT = Options.CLUSTER_URL + '/hosts/{0}/host_components/{1}'
  service_comp = {
    "YARN": ["NODEMANAGER", "RESOURCEMANAGER", "YARN_CLIENT"],
    "MAPREDUCE2": ["HISTORYSERVER", "MAPREDUCE2_CLIENT"]}
  new_old_host_map = {
    "NODEMANAGER": "TASKTRACKER",
    "HISTORYSERVER": "HISTORYSERVER",
    "RESOURCEMANAGER": "JOBTRACKER",
    "YARN_CLIENT": "MAPREDUCE_CLIENT",
    "MAPREDUCE2_CLIENT": "MAPREDUCE_CLIENT"}

  stack_name = get_cluster_stackname()
  stack_has_ats = has_component_in_stack_def(stack_name, "YARN", "APP_TIMELINE_SERVER")

  # if upgrading to stack > 2.1 (which has ats)
  if stack_has_ats:
    service_comp["YARN"].append("APP_TIMELINE_SERVER")
    new_old_host_map["APP_TIMELINE_SERVER"] = "JOBTRACKER"

  hostmapping = read_mapping()

  for service in service_comp.keys():
    curl(SERVICE_URL_FORMAT.format(service), validate=True, validate_expect_body=False, request_type="POST")

    for component in service_comp[service]:
      curl(COMPONENT_URL_FORMAT.format(service, component),
           validate=True, validate_expect_body=False, request_type="POST")

      for host in hostmapping[new_old_host_map[component]]:
        curl(HOST_COMPONENT_URL_FORMAT.format(host, component),
             validate=True, validate_expect_body=False, request_type="POST")


def update_config(properties, config_type, attributes=None):
  tag = "version" + str(int(time.time() * 1000))
  properties_payload = {"Clusters": {"desired_config": {"type": config_type, "tag": tag, "properties": properties}}}
  if attributes is not None:
    properties_payload["Clusters"]["desired_config"]["properties_attributes"] = attributes

  expect_body = config_type != "cluster-env"  # ToDo: make exceptions more flexible

  curl(Options.CLUSTER_URL, request_type="PUT", data=properties_payload, validate=True,
       validate_expect_body=expect_body)


def get_zookeeper_quorum():
  zoo_cfg = curl(Options.COMPONENTS_FORMAT.format(Options.ZOOKEEPER_SERVER), validate=False, simulate=False, parse=True)
  zoo_quorum = []
  zoo_def_port = "2181"
  if "host_components" in zoo_cfg:
    for item in zoo_cfg["host_components"]:
      zoo_quorum.append("%s:%s" % (item["HostRoles"]["host_name"], zoo_def_port))

  return ",".join(zoo_quorum)


def get_config(cfg_type):
  tag, structured_resp = get_config_resp(cfg_type)
  properties = None
  properties_attributes = None

  if 'items' in structured_resp:
    for item in structured_resp['items']:
      if (tag == item['tag']) or (cfg_type == item['type']):
        if 'properties' in item:
          properties = item['properties']
        if 'properties_attributes' in item:
          properties_attributes = item['properties_attributes']
        break
  if properties is None:
    raise FatalException(-1, "Unable to read configuration for type " + cfg_type + " and tag " + tag)

  return properties, properties_attributes


def parse_config_resp(resp):
  parsed_configs = []
  if CatConst.ITEMS_TAG in resp:
    for config_item in resp[CatConst.ITEMS_TAG]:
      parsed_configs.append({
        "type": config_item[CatConst.TYPE_TAG],
        "properties": config_item[CatConst.STACK_PROPERTIES]
      })
  return parsed_configs


def get_config_resp(cfg_type, error_if_na=True, parsed=False, tag=None):
  CONFIG_URL_FORMAT = Options.CLUSTER_URL + '/configurations?type={0}&tag={1}'

  # Read the config version
  if tag is None:
    structured_resp = curl(Options.CLUSTER_URL, validate=True, validate_expect_body=True, parse=True, simulate=False)

    if 'Clusters' in structured_resp:
      if 'desired_configs' in structured_resp['Clusters']:
        if cfg_type in structured_resp['Clusters']['desired_configs']:
          tag = structured_resp['Clusters']['desired_configs'][cfg_type]['tag']

  if tag is not None:
    # Get the config with the tag and return properties
    structured_resp = curl(CONFIG_URL_FORMAT.format(cfg_type, tag), parse=True, simulate=False,
                           validate=True, validate_expect_body=True)
    if parsed:
      return tag, parse_config_resp(structured_resp)
    else:
      return tag, structured_resp
  else:
    if error_if_na:
      raise FatalException(-1, "Unable to get the current version for config type " + cfg_type)
    else:
      return tag, None


def get_config_resp_all():
  desired_configs = {}
  CONFIG_ALL_PROPERTIES_URL = Options.CLUSTER_URL  + "/configurations?fields=properties"
  desired_configs_resp = curl(Options.CLUSTER_URL, validate=True, validate_expect_body=True, parse=True, simulate=False)
  all_options = curl(CONFIG_ALL_PROPERTIES_URL, validate=True, validate_expect_body=True, parse=True, simulate=False)

  if 'Clusters' in desired_configs_resp:
    if 'desired_configs' in desired_configs_resp['Clusters']:
      desired_configs_resp = desired_configs_resp['Clusters']['desired_configs']
    else:
      return None
  else:
    return None

  if CatConst.ITEMS_TAG in all_options:
    all_options = all_options["items"]
  else:
    return None

  all_options = filter(
    lambda x: x["type"] in desired_configs_resp and x["tag"] == desired_configs_resp[x["type"]]["tag"],
    all_options)

  for item in all_options:
    if CatConst.STACK_PROPERTIES in item:  # config item could not contain anu property
      desired_configs[item["type"]] = item["properties"]

  return desired_configs


def modify_config_item(config_type, catalog):
  #  here should be declared tokens for pattern replace
  if catalog.get_parsed_version()["from"] == 13:  # ToDo: introduce class for pre-defined tokens
    hostmapping = read_mapping()
    jt_host = hostmapping["JOBTRACKER"][0]
    jh_host = hostmapping["HISTORYSERVER"][0]
  else:
    jt_host = ""
    jh_host = ""

  def _substitute(tokens, value):
    for token in tokens:
      if token == "{JOBHISTORY_HOST}":
        value = value.replace(token, jh_host)
      elif token == "{RESOURCEMANAGER_HOST}":
        value = value.replace(token, jt_host)
      elif token == "{ZOOKEEPER_QUORUM}":
        value = value.replace(token, get_zookeeper_quorum())
    return value
  # Exit from function if was passed not suitable parameters
  catalog.set_substitution_handler(_substitute)

  try:
    properties_latest, properties_attributes_latest = get_config(config_type)
    properties_latest = rename_all_properties(properties_latest, catalog.property_map_catalog)
  except Exception as e:
    properties_latest = {}
    properties_attributes_latest = None

  properties_copy = catalog.get_properties(config_type)
  is_merged_copy = CatConst.MERGED_COPY_TAG in catalog.config_groups.get(config_type) \
   and catalog.config_groups.get(config_type)[CatConst.MERGED_COPY_TAG] == CatConst.TRUE_TAG

  # ToDo: implement property transfer from one catalog to other
  #   properties_to_move = [
  #     "dfs.namenode.checkpoint.edits.dir",
  #     "dfs.namenode.checkpoint.dir",
  #     "dfs.namenode.checkpoint.period"]
  Options.logger.info("Updating '%s' catalog item..." % config_type )
  if is_merged_copy:  # Append configs to existed ones
    tag, structured_resp = get_config_resp(config_type, False)
    if structured_resp is not None:
      update_config_using_existing_properties(config_type, properties_copy, properties_latest, properties_attributes_latest, catalog)
  else:  # Rewrite/create config items
    update_config(catalog.get_properties_as_dict(properties_copy), config_type)


def modify_configs():
  if len(Options.ARGS) > 1:
    config_type = Options.ARGS[1]
  else:
    config_type = None

  catalog_farm = UpgradeCatalogFactory(Options.OPTIONS.upgrade_json)  # Load upgrade catalog
  catalog = catalog_farm.get_catalog(Options.OPTIONS.from_stack, Options.OPTIONS.to_stack)  # get desired version of catalog

  if catalog is None:
    raise FatalException(1, "Upgrade catalog for version %s-%s not found, no configs was modified"
                         % (Options.OPTIONS.from_stack, Options.OPTIONS.to_stack))

  if config_type is not None and config_type not in catalog.config_groups.list():
    raise FatalException("Config type %s not exists, no configs was modified" % config_type)

  if config_type is not None:
    modify_config_item(config_type, catalog)
  else:
    for collection_name in catalog.config_groups.list():
      modify_config_item(collection_name, catalog)


def rename_all_properties(properties, name_mapping):
  for key, val in name_mapping.items():
    if (key in properties.keys()) and (val not in properties.keys()):
      properties[val] = properties[key]
      del properties[key]
  return properties


# properties template - passed as dict from UpgradeCatalog
def update_config_using_existing_properties(conf_type, properties_template,
                                            site_properties, properties_attributes_latest, catalog):
  keys_processed = []
  keys_to_delete = []
  properties_parsed = catalog.get_properties_as_dict(properties_template)

  for key in properties_template.keys():
    keys_processed.append(key)
    if CatConst.PROPERTY_REMOVE_TAG in properties_template and properties_template[CatConst.PROPERTY_REMOVE_TAG] == CatConst.TRUE_TAG:
      keys_to_delete.append(key)

  for key in site_properties.keys():
    if key not in keys_processed:
      properties_parsed[key] = site_properties[key]

  for key in keys_to_delete:
    del properties_parsed[key]

  # check property attributes list
  if properties_attributes_latest is not None:
    for key in properties_attributes_latest:
      properties_attributes_latest[key] = dict(filter(
        lambda (item_key, item_value): item_key not in keys_to_delete,
        zip(properties_attributes_latest[key].keys(), properties_attributes_latest[key].values())
      ))

  update_config(properties_parsed, conf_type, attributes=properties_attributes_latest)


def backup_configs(conf_type=None):
  DESIRED_CONFIGS_URL = Options.CLUSTER_URL + "?fields=Clusters/desired_configs"

  desired_configs = curl(DESIRED_CONFIGS_URL, validate=True, validate_expect_body=True, parse=True, simulate=False)

  if "Clusters" in desired_configs and "desired_configs" in desired_configs["Clusters"]:
    for conf_type in desired_configs["Clusters"]["desired_configs"].keys():
      backup_single_config_type(conf_type, True)


def backup_single_config_type(conf_type, error_if_na=True):
  tag, response = get_config_resp(conf_type, error_if_na)
  if response is not None:
    Options.logger.info("Saving config for type: " + conf_type + " and tag: " + tag)
    write_config(response, conf_type, tag)
  else:
    Options.logger.info("Unable to obtain config for type: " + conf_type)


def install_services():
  SERVICE_URL_FORMAT = Options.CLUSTER_URL + '/services/{0}'
  SERVICES = ["MAPREDUCE2", "YARN"]
  PUT_IN_INSTALLED = [
    {
      "RequestInfo": {
        "context": "Install MapReduce2"
      },
      "Body": {
        "ServiceInfo": {
          "state": "INSTALLED"
        }
      }
    },
    {
      "RequestInfo": {
        "context": "Install YARN"
      },
      "Body": {
        "ServiceInfo": {
          "state": "INSTALLED"
        }
      }
    }
  ]

  err_retcode = 0
  err_message = ""
  for index in [0, 1]:
    try:
      curl(SERVICE_URL_FORMAT.format(SERVICES[index]), validate=True,
           validate_expect_body=not Options.OPTIONS.printonly, request_type="PUT", data=PUT_IN_INSTALLED[index])
    except FatalException as e:
      if not e.code == 0:
        err_retcode = e.code
        err_message = err_message + " Error while installing " + SERVICES[index] + ". Details: " + e.message + "."

  if err_retcode != 0:
    raise FatalException(err_retcode, err_message + "(Services may already be installed or agents are not yet started.)")

  Options.OPTIONS.exit_message = "Requests has been submitted to install YARN and MAPREDUCE2. Use Ambari Web to monitor " \
                         "the status of the install requests."


def validate_response(response, expect_body):
  if expect_body:
    if "\"href\" : \"" not in response:
      return 1, response
    else:
      return 0, ""
  elif len(response) > 0:
    return 1, response
  else:
    return 0, ""


def curl(url, tokens=None, headers=None, request_type="GET", data=None, parse=False,
         simulate=None, validate=False, validate_expect_body=False):

  simulate_only = Options.CURL_PRINT_ONLY is not None or (simulate is not None and simulate is True)
  print_url = Options.CURL_PRINT_ONLY is not None and simulate is not None

  curl_path = '/usr/bin/curl'
  curl_list = [curl_path]

  curl_list.append('-X')
  curl_list.append(request_type)

  if tokens is not None:
    curl_list.append('-u')
    curl_list.append("%s:%s" % (tokens["user"], tokens["pass"]))
  elif Options.API_TOKENS is not None:
    curl_list.append('-u')
    curl_list.append("%s:%s" % (Options.API_TOKENS["user"], Options.API_TOKENS["pass"]))

  if request_type in Options.POST_REQUESTS:
    curl_list.append(url)

  if headers is None and Options.HEADERS is not None:
    headers = Options.HEADERS

  if headers is not None:
    for header in headers:
      curl_list.append('-H')
      curl_list.append("%s: %s" % (header, headers[header]))

  if data is not None and request_type in Options.POST_REQUESTS:
    curl_list.append('--data')
    curl_list.append(json.dumps(data))

  if request_type in Options.GET_REQUESTS:
    curl_list.append(url)

  if print_url:
    Options.logger.info(" ".join(curl_list))

  if not simulate_only:
    osStat = subprocess.Popen(
      curl_list,
      stderr=subprocess.PIPE,
      stdout=subprocess.PIPE)
    out, err = osStat.communicate()
    if 0 != osStat.returncode:
      error = "curl call failed. out: " + out + " err: " + err
      Options.logger.error(error)
      raise FatalException(osStat.returncode, error)
  else:
    if not print_url:
      Options.logger.info(" ".join(curl_list))
    out = "{}"

  if validate and not simulate_only:
    retcode, errdata = validate_response(out, validate_expect_body)
    if not retcode == 0:
      raise FatalException(retcode, errdata)

  if parse:
    return json.loads(out)
  else:
    return out


def configuration_item_diff(collection_name, catalog, actual_properties_list):
  """
  Merge catalog item with actual config item on the server
  Diff item response:
   {
     "property" : name,
     "catalog_item": value,
     "catalog_value": value,
     "actual_value": value
   }
  :param collection_name:
  :param catalog:
  :return:
  """

  verified_catalog = []
  catalog_properties = catalog.get_properties(collection_name)
  actual_properties = None

  if collection_name in actual_properties_list:
    actual_properties = actual_properties_list[collection_name]

  if actual_properties is None:
    verified_catalog = map(lambda x: {
      "property": x,
      "catalog_item": catalog_properties[x],
      "catalog_value": catalog_properties[x][CatConst.PROPERTY_VALUE_TAG],
      "actual_value": None
    }, catalog_properties.keys())
  else:
    # build list of properties according to actual properties
    verified_catalog = map(lambda x: {
      "property": x,
      "catalog_item": catalog_properties[x] if x in catalog_properties else None,
      "catalog_value": catalog_properties[x][CatConst.PROPERTY_VALUE_TAG] if x in catalog_properties else None,
      "actual_value": actual_properties[x]
    }, actual_properties.keys())

    # build list of properties according to catalog properties
    verified_catalog_catalog = map(lambda x: {
      "property": x,
      "catalog_item": catalog_properties[x],
      "catalog_value": catalog_properties[x][CatConst.PROPERTY_VALUE_TAG] if CatConst.PROPERTY_VALUE_TAG in catalog_properties[x] else None,
      "actual_value": actual_properties[x] if x in actual_properties else None,
    }, catalog_properties.keys())

    # append properties, which are listened in catalog but doesn't present in the actual configuration
    verified_catalog += filter(lambda x: x["property"] not in actual_properties, verified_catalog_catalog)

  return verified_catalog


def configuration_diff_analyze(diff_list):
  report = {}
  for item_key in diff_list.keys():
    property_diff_list = diff_list[item_key]
    item_stat = {
      "skipped": {"count": 0, "items": []},
      "ok": {"count": 0, "items": []},
      "fail": {"count": 0, "items": []},
      "total": {"count": len(property_diff_list), "items": []}
    }

    def push_status(status, _property_item):
      item_stat[status]["count"] += 1
      item_stat[status]["items"].append(_property_item)

    for property_item in property_diff_list:
      # process properties which can be absent

      # item was removed, from actual configs according to catalog instructions
      if property_item["actual_value"] is None and property_item["catalog_value"] is None \
        and CatConst.PROPERTY_REMOVE_TAG in property_item["catalog_item"] \
        and property_item["catalog_item"][CatConst.PROPERTY_REMOVE_TAG] == CatConst.TRUE_TAG:

        push_status("ok", property_item)

       # currently skip values with template tag, as there no filter implemented
       # ToDo: implement possibility to filter values without filter handler,
       # ToDo: currently filtering is possible only on update-configs stage
      elif property_item["actual_value"] is not None and property_item["catalog_value"] is not None \
        and CatConst.VALUE_TEMPLATE_TAG in property_item["catalog_item"] \
        and property_item["catalog_item"][CatConst.VALUE_TEMPLATE_TAG] == CatConst.TRUE_TAG:

        push_status("skipped", property_item)

      # item not present in actual config, but present in catalog and no remove tag is present
      elif property_item["actual_value"] is None and property_item["catalog_value"] is not None:
        push_status("fail", property_item)

      # property exists in actual configuration, but not described in catalog configuration
      elif property_item["actual_value"] is not None and property_item["catalog_value"] is None:
        push_status("skipped", property_item)

      # actual and catalog properties are equal
      elif property_item["catalog_value"] == property_item["actual_value"]:
        push_status("ok", property_item)
      elif property_item["catalog_value"] != property_item["actual_value"]:
        push_status("fail", property_item)

    report[item_key] = item_stat
  return report


def verify_configuration():
  diff_list = {}

  if len(Options.ARGS) > 1:
    config_type = Options.ARGS[1]
  else:
    config_type = None

  catalog_farm = UpgradeCatalogFactory(Options.OPTIONS.upgrade_json)  # Load upgrade catalog
  catalog = catalog_farm.get_catalog(Options.OPTIONS.from_stack, Options.OPTIONS.to_stack)  # get desired version of catalog

  if catalog is None:
    raise FatalException(1, "Upgrade catalog for version %s-%s not found"
                         % (Options.OPTIONS.from_stack, Options.OPTIONS.to_stack))

  if config_type is not None and config_type not in catalog.config_groups.list():
    raise FatalException("Config type %s not exists" % config_type)

  # fetch from server all option at one time and filter only desired versions
  actual_options = get_config_resp_all()

  if config_type is not None:
    diff_list[config_type] = configuration_item_diff(config_type, catalog, actual_options)
  else:
    for collection_name in catalog.config_groups.list():
      diff_list[collection_name] = configuration_item_diff(collection_name, catalog, actual_options)

  analyzed_list = configuration_diff_analyze(diff_list)

  report_file = None
  if Options.REPORT_FILE is not None:
    try:
      report_file = open(Options.REPORT_FILE, "w")
    except IOError as e:
      Options.logger.error("Report file open error: %s" % e.message)

  for config_item in analyzed_list:
    if analyzed_list[config_item]["fail"]["count"] != 0:
      Options.logger.info(
        "%s: %s missing configuration(s) - please look in the output file for the missing params" % (
         config_item, analyzed_list[config_item]["fail"]["count"]
        )
      )
      if report_file is not None:
        report_formatter(report_file, config_item, analyzed_list[config_item])
    else:
      Options.logger.info("%s: verified" % config_item)

  if report_file is not None:
    try:
      report_file.close()
    except IOError as e:
      Options.logger.error("Report file close error: %s" % e.message)


def report_formatter(report_file, config_item, analyzed_list_item):
  prefix = "Configuration item %s" % config_item
  if analyzed_list_item["fail"]["count"] > 0:
    for item in analyzed_list_item["fail"]["items"]:
      report_file.write("%s: property \"%s\" is set to \"%s\", but should be set to \"%s\"" % (
        prefix, item["property"], item["actual_value"], item["catalog_value"]
      ))


#
# Main.
#
def main():

  action_list = {  # list of supported actions
                   Options.GET_MR_MAPPING_ACTION: get_mr1_mapping,
                   Options.DELETE_MR_ACTION: delete_mr,
                   Options.ADD_YARN_MR2_ACTION: add_services,
                   Options.MODIFY_CONFIG_ACTION: modify_configs,
                   Options.INSTALL_YARN_MR2_ACTION: install_services,
                   Options.BACKUP_CONFIG_ACTION: backup_configs,
                   Options.VERIFY_ACTION: verify_configuration
  }

  parser = optparse.OptionParser(usage="usage: %prog [options] action\n  Valid actions: "
                                       + ", ".join(action_list.keys())
                                       + "\n  update-configs accepts type, e.g. hdfs-site to update specific configs")

  parser.add_option("-n", "--printonly",
                    action="store_true", dest="printonly", default=False,
                    help="Prints all the curl commands to be executed (only for write/update actions)")
  parser.add_option("-o", "--log", dest="logfile", default=None,
                    help="Log file")
  parser.add_option("--report", dest="report", default=None,
                    help="Report file output location")

  parser.add_option('--upgradeCatalog', default=None, help="Upgrade Catalog file full path", dest="upgrade_json")
  parser.add_option('--fromStack', default=None, help="stack version to upgrade from", dest="from_stack")
  parser.add_option('--toStack', default=None, help="stack version to upgrade to", dest="to_stack")

  parser.add_option('--hostname', default=None, help="Hostname for Ambari server", dest="hostname")
  parser.add_option('--user', default=None, help="Ambari admin user", dest="user")
  parser.add_option('--password', default=None, help="Ambari admin password", dest="password")
  parser.add_option('--clustername', default=None, help="Cluster name", dest="clustername")

  (options, args) = parser.parse_args()
  Options.initialize_logger(options.logfile)
  options.warnings = []

  if len(args) == 0:
    parser.error("No action entered")

  if options.user is None:
    options.warnings.append("User name must be provided (e.g. admin)")
  if options.hostname is None:
    options.warnings.append("Ambari server host name must be provided")
  if options.clustername is None:
    options.warnings.append("Cluster name must be provided")
  if options.password is None:
    options.password = getpass.getpass("Please enter Ambari admin password: ")
    if options.password == "":
      options.warnings.append("Ambari admin user's password name must be provided (e.g. admin)")
  action = args[0]

  # check params according to executed action
  if action == Options.MODIFY_CONFIG_ACTION or action == Options.VERIFY_ACTION:
    if options.upgrade_json is None:
      options.warnings.append("Upgrade catalog option need to be set")
    if options.from_stack is None:
      options.warnings.append("Should be provided fromStack option")
    if options.to_stack is None:
      options.warnings.append("Should be provided toStack option")

  if action == Options.VERIFY_ACTION:
    if options.report is None:
      options.warnings.append("Should be provided report option")

  if len(options.warnings) != 0:
    print parser.print_help()
    for warning in options.warnings:
      Options.logger.warn(warning)
    raise FatalException(1, "Not all required options was set")

  options.exit_message = "Upgrade action '%s' completed successfully." % action
  if options.printonly:
    Options.CURL_PRINT_ONLY = "yes"
    options.exit_message = "Simulated execution of action '%s'. Verify the list edit calls." % action

  Options.ARGS = args
  Options.OPTIONS = options
  Options.HOST = options.hostname
  Options.CLUSTER_NAME = options.clustername
  Options.API_TOKENS = {
    "user": options.user,
    "pass": options.password
  }
  Options.REPORT_FILE = options.report

  if action in action_list:
    Options.initialize()
    action_list[action]()
  else:
    parser.error("Invalid action")

  if options.exit_message is not None:
    Options.logger.info(options.exit_message)

if __name__ == "__main__":
  try:
    main()
  except (KeyboardInterrupt, EOFError):
    print("\nAborting ... Keyboard Interrupt.")
    sys.exit(1)
  except FatalException as e:
    if e.reason is not None:
      error = "Exiting with exit code {0}. Reason: {1}".format(e.code, e.reason)
      if Options.logger is not None:
        Options.logger.error(error)
      sys.exit(e.code)
