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
    required-services - properties from json catalog would be processed only if desired services are present on the cluster
                        property level definition will always override catalog level definition.

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
            "merged-copy": "yes",
            "required-services": ["HDFS"]
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
           "template": "yes",
           "required-services": ["HDFS", "YARN"]
          }
        }
      },
      "property-mapping": {
        "old-property-name": "new-property-name", (short form, equal to "old-property-name": { "map-to": "new-property-name" })
        "old-property1-name": {    (usually key is an name of the property which need to be mapped, but in case of same
                                     property should be set to unique name and "map-from" option used instead)
          "map-from": "old property name", (optional, define property name which should be mapped)
          "map-to": "new_property1_name", (optional, new property name. If not set, would be used old property name)
          "from-catalog": "test",        (optional, require "to-catalog. Source of old-property1-name)
          "to-catalog": "test",          (optional, require "from-catalog. Target of new_property1_name)
          "default": "default value",    (optional, if set and old property not exists, new one would be created with default value)
          "template": "yes",             (optional, template parsing for default option)
          "required-services": ["YARN"]  (optional, process entry if services in the list existed on the cluster
      }
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
import os.path
import logging
import time
import base64
from urllib2 import HTTPPasswordMgrWithDefaultRealm, HTTPBasicAuthHandler, Request, build_opener, URLError, HTTPError

try:
  # try to import new simplejson version, which should be faster than outdated python 2.6 version
  import ambari_simplejson as json
except ImportError:
  import json


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


class CatalogNotFoundException(Exception):
  pass


class CatalogExistException(Exception):
  pass


class PropertyNotFoundException(Exception):
  pass


class MalformedPropertyDefinitionException(Exception):
  pass


# ==============================
#    Constant class definition
# ==============================
class Const(object):
  def __new__(cls, *args, **kwargs):
    raise Exception("Class couldn't be created")


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
  ZK_OPTIONS = "zoo.cfg"
  KAFKA_BROKER_CONF = "kafka-broker"
  RANGER_ADMIN = "admin-properties"
  KAFKA_PORT = "port"
  RANGER_EXTERNAL_URL = "ranger.externalurl"
  ZK_CLIENTPORT = "clientPort"
  DELETE_OLD_TAG = "DELETE_OLD"

  ZOOKEEPER_SERVER = "ZOOKEEPER_SERVER"
  KAFKA_BROKER = "KAFKA_BROKER"
  NAMENODE = "NAMENODE"

  MR_MAPPING = None
  logger = None
  server_config_factory = None
  """:type : ServerConfigFactory"""

  # Api constants
  ROOT_URL = None
  CLUSTER_URL = None
  COMPONENTS_FORMAT = None
  TEZ_VIEW_URL = None

  # Curl options
  CURL_PRINT_ONLY = None

  ARGS = None
  OPTIONS = None
  HOST = None
  CLUSTER_NAME = None

  # for verify action
  REPORT_FILE = None

  SERVICES = []

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
    cls.TEZ_VIEW_URL = cls.ROOT_URL + "/views/TEZ"
    if cls.CLUSTER_NAME is not None and cls.HOST is not None:
      cls.SERVICES = set(map(lambda x: x.upper(), get_cluster_services()))

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


class CatConst(Const):
  VERSION_TAG = "version"
  STACK_VERSION_OLD = "old-version"
  STACK_VERSION_TARGET = "target-version"
  STACK_STAGS_TAG = "stacks"
  STACK_NAME = "name"
  CONFIG_OPTIONS = "options"
  CONFIG_TYPES = "config-types"
  STACK_PROPERTIES = "properties"
  STACK_PROPERTIES_ATTRIBUTES = "properties_attributes"
  PROPERTY_VALUE_TAG = "value"
  PROPERTY_REMOVE_TAG = "remove"
  PROPERTY_MAP_TO = "map-to"
  PROPERTY_MAP_FROM = "map-from"
  PROPERTY_FROM_CATALOG = "from-catalog"
  PROPERTY_TO_CATALOG = "to-catalog"
  PROPERTY_DEFAULT = "default"
  MERGED_COPY_TAG = "merged-copy"
  REQUIRED_SERVICES = "required-services"
  ITEMS_TAG = "items"
  TYPE_TAG = "type"
  TRUE_TAG = "yes"
  STACK_PROPERTIES_MAPPING_LIST_TAG = "property-mapping"
  VALUE_TEMPLATE_TAG = "template"
  SEARCH_PATTERN = "(\{[^\{\}]+\})"  # {XXXXX}
  ACTION_COMMIT = "commit"
  ACTION_RELOAD = "reload"
  ACTION_RENAME_PROPERTY = "rename-property"
  TEMPLATE_HANDLER = "template_handler"


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
  _properties_map_catalog = None
  _version = None
  _search_pattern = None
  _catalog_options = None

  def __init__(self, catalog=None, version=None):
    self._handlers = {}
    self._json_catalog = catalog
    self._version = version
    self._search_pattern = re.compile(CatConst.SEARCH_PATTERN)

    if CatConst.STACK_PROPERTIES in catalog:
      self._properties_catalog = self._format_catalog_properties(catalog[CatConst.STACK_PROPERTIES])

    if CatConst.STACK_PROPERTIES_MAPPING_LIST_TAG in catalog:
      self._properties_map_catalog = PropertyMapping(catalog[CatConst.STACK_PROPERTIES_MAPPING_LIST_TAG])
    else:
      self._properties_map_catalog = PropertyMapping()

    if catalog is not None and CatConst.CONFIG_OPTIONS in catalog \
            and CatConst.CONFIG_TYPES in catalog[CatConst.CONFIG_OPTIONS]:
      self._catalog_options = catalog[CatConst.CONFIG_OPTIONS]

  def add_handler(self, name, handler):
    if name not in self._handlers:
      self._handlers[name] = handler

  def _format_catalog_properties(self, properties):
    """
    Transform properties from short form to normal one:
    "property": "text" => "property": { "value": "text" }
    :param properties: dict
    :return: dict
    """
    for config_item in properties:
      cfg_item = properties[config_item]
      properties[config_item] = dict(zip(
        cfg_item.keys(),
        map(lambda x: x if isinstance(x, dict) or isinstance(x, list) else {CatConst.PROPERTY_VALUE_TAG: x}, cfg_item.values())
      ))
    return properties

  @property
  def version(self):
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

  @property
  def name(self):
    if CatConst.STACK_NAME in self._json_catalog:
      return self._json_catalog[CatConst.STACK_NAME]
    return ""

  @property
  def mapping(self):
    return self._properties_map_catalog

  @property
  def items(self):
    return self._properties_catalog

  @property
  def options(self):
    if CatConst.CONFIG_TYPES in self._catalog_options:
      return self._catalog_options[CatConst.CONFIG_TYPES]
    return {}

  @property
  def action_handlers(self):
    return self._handlers

  @property
  def tag_search_pattern(self):
    return self._search_pattern

  def __handle_remove_tag(self, catalog_item_name, catalog_property_item, properties):
    """
    :type catalog_item_name str
    :type catalog_property_item dict
    :type properties dict
    """
    if CatConst.PROPERTY_REMOVE_TAG in catalog_property_item and \
                    catalog_property_item[CatConst.PROPERTY_REMOVE_TAG] == CatConst.TRUE_TAG and \
                    catalog_item_name in properties:
      del properties[catalog_item_name]

  def __handle_template_tag_sub(self, catalog_item_name, catalog_property_item):
    """
    :type catalog_item_name str
    :type catalog_property_item dict
    """
    if CatConst.TEMPLATE_HANDLER in self._handlers and self._handlers is not None and \
                    CatConst.VALUE_TEMPLATE_TAG in catalog_property_item and catalog_property_item[
      CatConst.VALUE_TEMPLATE_TAG] == CatConst.TRUE_TAG:
      parsed_value = self._handlers[CatConst.TEMPLATE_HANDLER](
        self,
        self._search_pattern.findall(catalog_property_item[CatConst.PROPERTY_VALUE_TAG]),
        catalog_property_item[CatConst.PROPERTY_VALUE_TAG]
      )
      catalog_property_item[CatConst.PROPERTY_VALUE_TAG] = parsed_value

  def __handle_add_new(self, catalog_item_name, catalog_property_item, properties):
    """
    :type catalog_item_name str
    :type catalog_property_item dict
    :type properties dict
    """
    catalog_property_item = dict(catalog_property_item)
    if CatConst.PROPERTY_VALUE_TAG in catalog_property_item and catalog_item_name not in properties:
      self.__handle_template_tag_sub(catalog_item_name, catalog_property_item)
      properties[catalog_item_name] = catalog_property_item[CatConst.PROPERTY_VALUE_TAG]

  def __handle_change_existing(self, catalog_item_name, catalog_property_item, properties):
    """
    :type catalog_item_name str
    :type catalog_property_item dict
    :type properties dict
    """
    catalog_property_item = dict(catalog_property_item)
    if CatConst.PROPERTY_VALUE_TAG in catalog_property_item and catalog_item_name in properties:
      self.__handle_template_tag_sub(catalog_item_name, catalog_property_item)
      properties[catalog_item_name] = catalog_property_item[CatConst.PROPERTY_VALUE_TAG]
    return properties

  def __can_handler_execute(self, catalog_options, property_item):
    """
    :type catalog_options dict
    :type property_item dict
    """
    can_process = True

    # process required services tag
    required_list = None

    if CatConst.REQUIRED_SERVICES in catalog_options and catalog_options[CatConst.REQUIRED_SERVICES] is not None and \
            isinstance(catalog_options[CatConst.REQUIRED_SERVICES], list):
      required_list = catalog_options[CatConst.REQUIRED_SERVICES]

    if CatConst.REQUIRED_SERVICES in property_item and property_item[CatConst.REQUIRED_SERVICES] is not None and\
            isinstance(property_item[CatConst.REQUIRED_SERVICES], list):
      required_list = property_item[CatConst.REQUIRED_SERVICES]

    if required_list is not None:
      can_process = can_process and is_services_exists(required_list)

    return can_process

  def process_simple_transformations(self, name, properties):
    """
    :type properties dict
    :type name str
    """
    tag_handlers = [
      self.__handle_add_new,
      self.__handle_change_existing,
      self.__handle_remove_tag
    ]
    # catalog has no update entries for this config group
    if name not in self._properties_catalog:
      return 0

    catalog_item = self._properties_catalog[name]
    for catalog_property_item in catalog_item.keys():
      catalog_options = self.options[name] if name in self.options else {}
      if self.__can_handler_execute(catalog_options, catalog_item[catalog_property_item]):
        for handler in tag_handlers:
          handler(catalog_property_item, catalog_item[catalog_property_item], properties)


class PropertyMapping(object):
  _mapping_list = {}

  def __init__(self, map_list=None):
    if map_list is not None:
      self._mapping_list = self._convert_list(map_list)

  def _convert_list(self, map_list):
    return dict(zip(
      map_list.keys(),
      map(lambda x: x if isinstance(x, dict) else {CatConst.PROPERTY_MAP_TO: x}, map_list.values())
    ))

  def get(self, old_property_name):
    """
    Get property mapping dict
    :old_property_name str
    :return dict
    """
    if old_property_name in self._mapping_list:
      return self._mapping_list[old_property_name]

    raise PropertyNotFoundException("Property %s from property mapping section not found" % old_property_name)

  def list(self):
    return self._mapping_list.keys()

  def get_mapped_name(self, old_property_name):
    if CatConst.PROPERTY_MAP_TO not in self.get(old_property_name):
      raise MalformedPropertyDefinitionException("%s option is not set for %s property" %
                                                 (CatConst.PROPERTY_MAP_TO, old_property_name))
    return self.get(old_property_name)[CatConst.PROPERTY_MAP_TO]

  def exists(self, old_property_name):
    return old_property_name in self._mapping_list


class ServerConfigFactory(object):
  _server_catalogs = {}

  def __init__(self):
    self.__observers = []
    self._load_configs()

  def subscribe(self, name, config_item):
    self.__observers.append((name, config_item))

  def _load_configs(self):
    Options.logger.info('Getting latest cluster configuration from the server...')
    new_configs = get_config_resp_all()
    for config_item in new_configs:
      if config_item in self._server_catalogs:
        self.notify_observer(config_item, CatConst.ACTION_RELOAD, new_configs[config_item])
      else:
        self._server_catalogs[config_item] = ServerConfig(self, config_item, new_configs[config_item])

  def notify_observers(self, action, arg=None):
    for name, config_item in self.__observers:
      if config_item is not None and name in self._server_catalogs:
        config_item.notify(action, arg)

  def notify_observer(self, _name, action, arg=None):
    for name, config_item in self.__observers:
      if config_item is not None and name == _name and name in self._server_catalogs:
        config_item.notify(action, arg)

  def get_config(self, name):
    """
    Get configuration item object
    :type name str
    :rtype: ServerConfig
    """
    if name in self._server_catalogs:
      return self._server_catalogs[name]

    raise CatalogNotFoundException("Server catalog item \"%s\" not found" % name)

  def create_config(self, name):
    if name not in self._server_catalogs:
      self._server_catalogs[name] = ServerConfig(self, name, {CatConst.STACK_PROPERTIES: {}})
    else:
      raise CatalogExistException("Config group \"%s\" already existed" % name)

  def items(self):
    return self._server_catalogs.keys()

  def reload(self):
    self._load_configs()

  def process_mapping_transformations(self, catalog):
    """
    :type catalog UpgradeCatalog
    """
    for map_item in catalog.mapping.list():
      self._process_single_map_transformation(catalog, map_item, catalog.mapping.get(map_item))

  def _process_default_template_map_replacement(self, catalog, item):
    """
    :type catalog: UpgradeCatalog
    :type item: dict
    """
    if CatConst.VALUE_TEMPLATE_TAG in item and CatConst.TEMPLATE_HANDLER in catalog.action_handlers and\
            CatConst.PROPERTY_DEFAULT in item and item[CatConst.VALUE_TEMPLATE_TAG] == CatConst.TRUE_TAG:

      parsed_value = catalog.action_handlers[CatConst.TEMPLATE_HANDLER](
        catalog,
        catalog.tag_search_pattern.findall(item[CatConst.PROPERTY_DEFAULT]),
        item[CatConst.PROPERTY_DEFAULT]
      )
      item[CatConst.PROPERTY_DEFAULT] = parsed_value

  def _process_single_map_transformation(self, catalog, map_item_name, map_property_item):
    """
    :type catalog UpgradeCatalog
    :type map_item_name str
    :type map_property_item dict
    """
    old_property_name = map_item_name

    # map-from item name could be re-defined via PROPERTY_MAP_FROM property to avoid duplicate entries
    if CatConst.PROPERTY_MAP_FROM in map_property_item and map_property_item[CatConst.PROPERTY_MAP_FROM] is not None:
      old_property_name = map_property_item[CatConst.PROPERTY_MAP_FROM]

    new_property_name = old_property_name

    if CatConst.PROPERTY_MAP_TO in map_property_item:
      new_property_name = map_property_item[CatConst.PROPERTY_MAP_TO]

    # process template tag
    self._process_default_template_map_replacement(catalog, map_property_item)

    source_cfg_group = map_property_item[CatConst.PROPERTY_FROM_CATALOG] if CatConst.PROPERTY_FROM_CATALOG in map_property_item and\
                                                                            map_property_item[CatConst.PROPERTY_FROM_CATALOG] != "" else None
    target_cfg_group = map_property_item[CatConst.PROPERTY_TO_CATALOG] if CatConst.PROPERTY_TO_CATALOG in map_property_item and \
                                                                          map_property_item[CatConst.PROPERTY_TO_CATALOG] != ""else None
    default_value = map_property_item[CatConst.PROPERTY_DEFAULT] if CatConst.PROPERTY_DEFAULT in map_property_item and \
                                                                    map_property_item[CatConst.PROPERTY_DEFAULT] != "" else None
    required_services = map_property_item[CatConst.REQUIRED_SERVICES] if CatConst.REQUIRED_SERVICES in map_property_item else None

    # process required-services tag
    if required_services is not None and not is_services_exists(required_services):
      return 0

    if source_cfg_group is None and target_cfg_group is None:  # global scope mapping renaming
      self.notify_observers(CatConst.ACTION_RENAME_PROPERTY, [old_property_name, new_property_name])
    elif source_cfg_group is not None and target_cfg_group is not None:  # group-to-group moving
      if source_cfg_group in self._server_catalogs and target_cfg_group in self._server_catalogs:
        old_cfg_group = self.get_config(source_cfg_group).properties
        new_cfg_group = self.get_config(target_cfg_group).properties

        if old_property_name in old_cfg_group:
          new_cfg_group[new_property_name] = old_cfg_group[old_property_name]
          del old_cfg_group[old_property_name]
        elif old_property_name not in old_cfg_group and default_value is not None:
          new_cfg_group[new_property_name] = default_value

  def commit(self):
    self.notify_observers(CatConst.ACTION_COMMIT)


class ServerConfig(object):
  def __init__(self, factory, name, initial_configs):
    """
     Initialize configuration item
     :factory ServerConfigFactory
    """
    factory.subscribe(name, self)
    self._configs = initial_configs
    self._hash = self._calculate_hash()
    self._name = name

  def _calculate_hash(self):
    return hash(str(self._configs))

  def notify(self, action, arg=None):
    if action == CatConst.ACTION_RELOAD:
      self._configs = arg
      self._hash = self._calculate_hash()
    elif action == CatConst.ACTION_COMMIT:
      self._commit()
    elif action == CatConst.ACTION_RENAME_PROPERTY and isinstance(arg, list) and len(arg) == 2:
      self._rename_property(arg[0], arg[1])

  def _rename_property(self, old_name, new_name):
    if old_name in self.properties:
      old_property_value = self.properties[old_name]
      self.properties[new_name] = old_property_value
      del self.properties[old_name]

  def is_attributes_exists(self):
    return CatConst.STACK_PROPERTIES_ATTRIBUTES in self._configs

  @property
  def properties(self):
    return self._configs[CatConst.STACK_PROPERTIES]

  @properties.setter
  def properties(self, value):
    self._configs[CatConst.STACK_PROPERTIES] = value

  @property
  def attributes(self):
    return self._configs[CatConst.STACK_PROPERTIES_ATTRIBUTES]

  @attributes.setter
  def attributes(self, value):
    self._configs[CatConst.STACK_PROPERTIES_ATTRIBUTES] = value

  def _commit(self):
    if self._hash != self._calculate_hash():
      Options.logger.info("Committing changes for \"%s\" configuration group ..." % self._name)
      if self.is_attributes_exists():
        update_config(self.properties, self._name, self.attributes)
      else:
        update_config(self.properties, self._name)

  def clear(self):
    self.properties = {}
    self.attributes = {}

  def merge(self, catalog_item):
    """
    :type catalog_item UpgradeCatalog
    """
    # handle "merged-copy" tag
    config_options = catalog_item.options[self._name] if self._name in catalog_item.options else {}
    clear_properties = not (CatConst.MERGED_COPY_TAG in config_options and
                            config_options[CatConst.MERGED_COPY_TAG] == CatConst.TRUE_TAG)
    if clear_properties:
      self.clear()
    Options.logger.info("Processing configuration group: %s", self._name)
    catalog_item.process_simple_transformations(self._name, self.properties)


def write_mapping(hostmapping):
  if os.path.isfile(Options.MR_MAPPING_FILE):
    os.remove(Options.MR_MAPPING_FILE)
  json.dump(hostmapping, open(Options.MR_MAPPING_FILE, 'w'))


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
    structured_resp = curl(GET_URL_FORMAT % component, parse=True, validate=True)

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
             validate=True)

  curl(SERVICE_URL_FORMAT, request_type="DELETE", validate=True)


def get_cluster_stackname():
  VERSION_URL_FORMAT = Options.CLUSTER_URL + '?fields=Clusters/version'

  structured_resp = curl(VERSION_URL_FORMAT, simulate=False, validate=True, parse=True)

  if 'Clusters' in structured_resp:
    if 'version' in structured_resp['Clusters']:
      return structured_resp['Clusters']['version']

  raise FatalException(-1, "Unable to get the cluster version")


def has_component_in_stack_def(stack_name, service_name, component_name):
  STACK_COMPONENT_URL_FORMAT = Options.ROOT_URL + '/stacks2/{0}/versions/{1}/stackServices/{2}/serviceComponents/{3}'
  stack, stack_version = stack_name.split('-')

  try:
    curl(STACK_COMPONENT_URL_FORMAT.format(stack, stack_version, service_name, component_name),
         validate=True, simulate=False)
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
    curl(SERVICE_URL_FORMAT.format(service), validate=True, request_type="POST")

    for component in service_comp[service]:
      curl(COMPONENT_URL_FORMAT.format(service, component),
           validate=True, request_type="POST")

      for host in hostmapping[new_old_host_map[component]]:
        curl(HOST_COMPONENT_URL_FORMAT.format(host, component),
             validate=True, request_type="POST")


def update_config(properties, config_type, attributes=None):
  tag = "version" + str(int(time.time() * 1000))
  properties_payload = {"Clusters": {"desired_config": {"type": config_type, "tag": tag, "properties": properties}}}
  if attributes is not None:
    properties_payload["Clusters"]["desired_config"]["properties_attributes"] = attributes

  expect_body = config_type != "cluster-env"  # ToDo: make exceptions more flexible

  curl(Options.CLUSTER_URL, request_type="PUT", data=properties_payload, validate=True, soft_validation=True)


def get_config_resp_all():
  desired_configs = {}
  config_all_properties_url = Options.CLUSTER_URL + "/configurations?fields=properties,properties_attributes"
  desired_configs_resp = curl(Options.CLUSTER_URL + "?fields=Clusters/desired_configs", validate=True, parse=True, simulate=False)
  all_options = curl(config_all_properties_url, validate=True, parse=True, simulate=False)

  if 'Clusters' in desired_configs_resp:
    if 'desired_configs' in desired_configs_resp['Clusters']:
      desired_configs_resp = desired_configs_resp['Clusters']['desired_configs']
    else:
      return None
  else:
    return None

  if CatConst.ITEMS_TAG in all_options:
    all_options = all_options[CatConst.ITEMS_TAG]
  else:
    return None

  all_options = filter(
    lambda x: x[CatConst.TYPE_TAG] in desired_configs_resp and x["tag"] == desired_configs_resp[x[CatConst.TYPE_TAG]][
      "tag"],
    all_options)

  for item in all_options:
    dc_item = {}

    if CatConst.STACK_PROPERTIES in item:  # config item could not contain any property
      dc_item[CatConst.STACK_PROPERTIES] = item[CatConst.STACK_PROPERTIES]
    else:
      dc_item[CatConst.STACK_PROPERTIES] = {}

    if CatConst.STACK_PROPERTIES_ATTRIBUTES in item:
      dc_item[CatConst.STACK_PROPERTIES_ATTRIBUTES] = item[CatConst.STACK_PROPERTIES_ATTRIBUTES]

    if "tag" in item:
      dc_item["tag"] = item["tag"]

    if dc_item != {}:
      desired_configs[item[CatConst.TYPE_TAG]] = dc_item

  return desired_configs


def is_services_exists(required_services):
  """
  return true, if required_services is a part of Options.SERVICES
  :param required_services: list
  :return: bool
  """
  # sets are equal
  if Options.SERVICES == set(required_services):
    return True

  return set(map(lambda x: x.upper(), required_services)) < Options.SERVICES


def get_cluster_services():
  services_url = Options.CLUSTER_URL + '/services'
  raw_services = curl(services_url, parse=True, simulate=False)

  # expected structure:
  # items: [ {"href":"...", "ServiceInfo":{"cluster_name":"..", "service_name":".."}}, ..., ... ]
  if raw_services is not None and "items" in raw_services and isinstance(raw_services["items"], list):
    return list(map(lambda item: item["ServiceInfo"]["service_name"], raw_services["items"]))

  Options.logger.warning("Failed to load services list, functionality that depends on them couldn't work")
  return []


def get_zookeeper_quorum():
  zoo_cfg = curl(Options.COMPONENTS_FORMAT.format(Options.ZOOKEEPER_SERVER), validate=False, simulate=False, parse=True)
  zoo_quorum = []
  zoo_def_port = "2181"
  if Options.server_config_factory is not None and Options.ZK_OPTIONS in Options.server_config_factory.items():
    props = Options.server_config_factory.get_config(Options.ZK_OPTIONS)
    if Options.ZK_CLIENTPORT in props.properties:
      zoo_def_port = props.properties[Options.ZK_CLIENTPORT]

  if "host_components" in zoo_cfg:
    for item in zoo_cfg["host_components"]:
      zoo_quorum.append("%s:%s" % (item["HostRoles"]["host_name"], zoo_def_port))

  return ",".join(zoo_quorum)

def get_tez_history_url_base():
  tez_view = curl(Options.TEZ_VIEW_URL, validate=False, simulate=False, parse=True)
  version = ""
  if "versions" in tez_view and \
    len(tez_view['versions']) > 0 and \
    "ViewVersionInfo" in tez_view['versions'][0] and \
    'version' in tez_view['versions'][0]['ViewVersionInfo']:
    version = tez_view['versions'][0]['ViewVersionInfo']['version']
  url = '{0}://{1}:{2}/#/main/views/TEZ/{3}/TEZ_CLUSTER_INSTANCE'.format(Options.API_PROTOCOL, Options.HOST, Options.API_PORT, version)
  return url

def get_kafka_listeners():
  kafka_host="localhost"
  kafka_port="6667"
  if Options.server_config_factory is not None and Options.KAFKA_BROKER_CONF in Options.server_config_factory.items():
    props = Options.server_config_factory.get_config(Options.KAFKA_BROKER_CONF)
    if Options.KAFKA_PORT in props.properties:
      kafka_port = props.properties[Options.KAFKA_PORT]

  # Default kafka listeners string
  kafka_listeners = ["PLAINTEXT://{0}:{1}".format(kafka_host, kafka_port)]

  # Get hosts where kafka_broker is installed
  kafka_cfg = curl(Options.COMPONENTS_FORMAT.format(Options.KAFKA_BROKER), validate=False, simulate=False, parse=True)
  if "host_components" in kafka_cfg:
    kafka_listeners = []
    for item in kafka_cfg["host_components"]:
      kafka_listeners.append("PLAINTEXT://{0}:{1}".format(item["HostRoles"]["host_name"], kafka_port))

  return ",".join(kafka_listeners)

def get_ranger_xaaudit_hdfs_destination_directory():
  namenode_hostname="localhost"
  namenode_cfg = curl(Options.COMPONENTS_FORMAT.format(Options.NAMENODE), validate=False, simulate=False, parse=True)
  if "host_components" in namenode_cfg:
    namenode_hostname = namenode_cfg["host_components"][0]["HostRoles"]["host_name"]

  return "hdfs://{0}:8020/ranger/audit".format(namenode_hostname)

def get_ranger_policymgr_external_url():
  url = "{{ranger_external_url}}"
  if Options.server_config_factory is not None and Options.RANGER_ADMIN in Options.server_config_factory.items():
    props = Options.server_config_factory.get_config(Options.RANGER_ADMIN)
    if Options.RANGER_EXTERNAL_URL in props.properties:
      url = props.properties[Options.RANGER_EXTERNAL_URL]
  return url

def get_jdbc_driver(config_name):
  driver = "{{jdbc_driver}}"
  if Options.server_config_factory is not None and config_name in Options.server_config_factory.items():
    props = Options.server_config_factory.get_config(config_name)
    if "XAAUDIT.DB.FLAVOUR" in props.properties:
      db = props.properties["XAAUDIT.DB.FLAVOUR"]

  if db == "mysql":
    driver = "com.mysql.jdbc.Driver"
  elif db == "oracle":
    driver = "oracle.jdbc.OracleDriver"
  return driver

def get_audit_jdbc_url(config_name):
  audit_jdbc_url = "{{audit_jdbc_url}}"
  if Options.server_config_factory is not None and config_name in Options.server_config_factory.items():
    props = Options.server_config_factory.get_config(config_name)
    if "XAAUDIT.DB.FLAVOUR" in props.properties:
      xa_audit_db_flavor = props.properties["XAAUDIT.DB.FLAVOUR"]
    if "XAAUDIT.DB.HOSTNAME" in props.properties:
      xa_db_host =  props.properties["XAAUDIT.DB.HOSTNAME"]
    if "XAAUDIT.DB.DATABASE_NAME" in props.properties:
      xa_audit_db_name = props.properties["XAAUDIT.DB.DATABASE_NAME"]

  if xa_audit_db_flavor == 'mysql':
    audit_jdbc_url = "jdbc:mysql://{0}/{1}".format(xa_db_host, xa_audit_db_name)
  elif xa_audit_db_flavor == 'oracle':
    audit_jdbc_url = "jdbc:oracle:thin:\@//{0}".format(xa_db_host)
  return audit_jdbc_url

def get_jt_host(catalog):
  """
  :type catalog: UpgradeCatalog
  :rtype str
  """
  if catalog.get_parsed_version()["from"] == 13:
    return read_mapping()["JOBTRACKER"][0]

  return ""


def get_jh_host(catalog):
  """
  :type catalog: UpgradeCatalog
  :rtype str
  """
  if catalog.get_parsed_version()["from"] == 13:
    return read_mapping()["HISTORYSERVER"][0]

  return ""

def get_ranger_host():
  ranger_config = curl(Options.COMPONENTS_FORMAT.format('RANGER_ADMIN'), validate=False, simulate=False, parse=True)
  ranger_host_list = []
  if "host_components" in ranger_config:
    for item in ranger_config["host_components"]:
      ranger_host_list.append(item["HostRoles"]["host_name"])
  return ranger_host_list[0]

def get_ranger_service_details():
  server_cfg_factory = Options.server_config_factory
  server_cfg_catalog = server_cfg_factory.get_config('admin-properties')
  properties_latest = server_cfg_catalog.properties
  data = {}

  if properties_latest['DB_FLAVOR'].lower() == 'mysql':
    data['RANGER_JDBC_DRIVER'] = 'com.mysql.jdbc.Driver'
    data['RANGER_JDBC_DIALECT'] = 'org.eclipse.persistence.platform.database.MySQLPlatform'
    data['RANGER_JDBC_URL'] = 'jdbc:mysql://{0}/{1}'.format(properties_latest['db_host'], properties_latest['db_name'])
    data['RANGER_AUDIT_JDBC_URL'] = 'jdbc:mysql://{0}/{1}'.format(properties_latest['db_host'], properties_latest['audit_db_name'])
  elif properties_latest['DB_FLAVOR'].lower() == 'oracle':
    data['RANGER_JDBC_DRIVER'] = 'oracle.jdbc.OracleDriver'
    data['RANGER_JDBC_DIALECT'] = 'org.eclipse.persistence.platform.database.OraclePlatform'
    data['RANGER_JDBC_URL'] = 'jdbc:oracle:thin:@//{0}'.format(properties_latest['db_host'])
    data['RANGER_AUDIT_JDBC_URL'] = 'jdbc:oracle:thin:@//{0}'.format(properties_latest['db_host'])

  return data

def _substitute_handler(upgrade_catalog, tokens, value):
  """
  Substitute handler
  :param upgrade_catalog: UpgradeCatalog
  :param tokens: list
  :param value: str
  :rtype str
  """
  for token in tokens:
    if token == "{JOBHISTORY_HOST}":
      value = value.replace(token, get_jh_host(upgrade_catalog))
    elif token == "{RESOURCEMANAGER_HOST}":
      value = value.replace(token, get_jt_host(upgrade_catalog))
    elif token == "{ZOOKEEPER_QUORUM}":
      value = value.replace(token, get_zookeeper_quorum())
    elif token == "{TEZ_HISTORY_URL_BASE}":
      value = value.replace(token, get_tez_history_url_base())
    elif token == "{RANGER_JDBC_DRIVER}":
      value = value.replace(token, get_ranger_service_details()['RANGER_JDBC_DRIVER'])
    elif token == "{RANGER_JDBC_URL}":
      value = value.replace(token, get_ranger_service_details()['RANGER_JDBC_URL'])
    elif token == "{RANGER_AUDIT_JDBC_URL}":
      value = value.replace(token, get_ranger_service_details()['RANGER_AUDIT_JDBC_URL'])
    elif token == "{RANGER_HOST}":
      value = value.replace(token, get_ranger_host())
    elif token == "{RANGER_JDBC_DIALECT}":
      value = value.replace(token, get_ranger_service_details()['RANGER_JDBC_DIALECT'])
    elif token == "{KAFKA_LISTENERS}":
      value = value.replace(token, get_kafka_listeners())
    elif token == "{RANGER_PLUGIN_HBASE_POLICY_CACHE_DIR}":
      value = value.replace(token, "/etc/ranger/{0}{1}/policycache".format(Options.CLUSTER_NAME, "_hbase"))
    elif token == "{RANGER_PLUGIN_HDFS_POLICY_CACHE_DIR}":
      value = value.replace(token, "/etc/ranger/{0}{1}/policycache".format(Options.CLUSTER_NAME, "_hdfs"))
    elif token == "{RANGER_PLUGIN_HIVE_POLICY_CACHE_DIR}":
      value = value.replace(token, "/etc/ranger/{0}{1}/policycache".format(Options.CLUSTER_NAME, "_hive"))
    elif token == "{RANGER_PLUGIN_KNOX_POLICY_CACHE_DIR}":
      value = value.replace(token, "/etc/ranger/{0}{1}/policycache".format(Options.CLUSTER_NAME, "_knox"))
    elif token == "{RANGER_PLUGIN_STORM_POLICY_CACHE_DIR}":
      value = value.replace(token, "/etc/ranger/{0}{1}/policycache".format(Options.CLUSTER_NAME, "_storm"))
    elif token == "{RANGER_HBASE_KEYSTORE_CREDENTIAL_FILE}":
      value = value.replace(token, "jceks://file/etc/ranger/{0}{1}/cred.jceks".format(Options.CLUSTER_NAME, "_hbase"))
    elif token == "{RANGER_HDFS_KEYSTORE_CREDENTIAL_FILE}":
      value = value.replace(token, "jceks://file/etc/ranger/{0}{1}/cred.jceks".format(Options.CLUSTER_NAME, "_hdfs"))
    elif token == "{RANGER_HIVE_KEYSTORE_CREDENTIAL_FILE}":
      value = value.replace(token, "jceks://file/etc/ranger/{0}{1}/cred.jceks".format(Options.CLUSTER_NAME, "_hive"))
    elif token == "{RANGER_KNOX_KEYSTORE_CREDENTIAL_FILE}":
      value = value.replace(token, "jceks://file/etc/ranger/{0}{1}/cred.jceks".format(Options.CLUSTER_NAME, "_knox"))
    elif token == "{RANGER_STORM_KEYSTORE_CREDENTIAL_FILE}":
      value = value.replace(token, "jceks://file/etc/ranger/{0}{1}/cred.jceks".format(Options.CLUSTER_NAME, "_storm"))

    elif token == "{XAAUDIT_HDFS_DESTINATION_DIRECTORY}":
      value = value.replace(token, get_ranger_xaaudit_hdfs_destination_directory())
    elif token == "{HBASE_RANGER_REPO_NAME}":
      value = value.replace(token, Options.CLUSTER_NAME+"_hbase")
    elif token == "{HDFS_RANGER_REPO_NAME}":
      value = value.replace(token, Options.CLUSTER_NAME+"_hdfs")
    elif token == "{HIVE_RANGER_REPO_NAME}":
      value = value.replace(token, Options.CLUSTER_NAME+"_hive")
    elif token == "{HNOX_RANGER_REPO_NAME}":
      value = value.replace(token, Options.CLUSTER_NAME+"_knox")
    elif token == "{STORM_RANGER_REPO_NAME}":
      value = value.replace(token, Options.CLUSTER_NAME+"_storm")
    elif token == "{POLICYMGR_MGR_URL}":
      value = value.replace(token, get_ranger_policymgr_external_url())
    elif token == "{HDFS_JDBC_DRIVER}":
      value = value.replace(token, get_jdbc_driver("ranger-hdfs-plugin-properties"))
    elif token == "{HBASE_JDBC_DRIVER}":
      value = value.replace(token, get_jdbc_driver("ranger-hbase-plugin-properties"))
    elif token == "{HIVE_JDBC_DRIVER}":
      value = value.replace(token, get_jdbc_driver("ranger-hive-plugin-properties"))
    elif token == "{KNOX_JDBC_DRIVER}":
      value = value.replace(token, get_jdbc_driver("ranger-knox-plugin-properties"))
    elif token == "{STORM_JDBC_DRIVER}":
      value = value.replace(token, get_jdbc_driver("ranger-storm-plugin-properties"))
    elif token == "{HDFS_AUDIT_JDBC_URL}":
      value = value.replace(token, get_audit_jdbc_url("ranger-hdfs-plugin-properties"))
    elif token == "{HBASE_AUDIT_JDBC_URL}":
      value = value.replace(token, get_audit_jdbc_url("ranger-hbase-plugin-properties"))
    elif token == "{HIVE_AUDIT_JDBC_URL}":
      value = value.replace(token, get_audit_jdbc_url("ranger-hive-plugin-properties"))
    elif token == "{KNOX_AUDIT_JDBC_URL}":
      value = value.replace(token, get_audit_jdbc_url("ranger-knox-plugin-properties"))
    elif token == "{STORM_AUDIT_JDBC_URL}":
      value = value.replace(token, get_audit_jdbc_url("ranger-storm-plugin-properties"))

  return value


def modify_config_item(config_type, catalog, server_config_factory):
  """
  Modify configuration item
  :type config_type str
  :type catalog UpgradeCatalog
  :type server_config_factory ServerConfigFactory
  """

  # if config group is absent on the server, we will create it
  if config_type not in server_config_factory.items():
    server_config_factory.create_config(config_type)

  server_config_catalog = server_config_factory.get_config(config_type)

  server_config_catalog.merge(catalog)


def modify_configs():
  if len(Options.ARGS) > 1:
    config_type = Options.ARGS[1]
  else:
    config_type = None

  catalog_farm = UpgradeCatalogFactory(Options.OPTIONS.upgrade_json)  # Load upgrade catalog
  catalog = catalog_farm.get_catalog(Options.OPTIONS.from_stack,
                                     Options.OPTIONS.to_stack)  # get desired version of catalog

  # load all desired configs from the server
  # ToDo: implement singleton for that class
  Options.server_config_factory = ServerConfigFactory()

  if catalog is None:
    raise FatalException(1, "Upgrade catalog for version %s-%s not found, no configs was modified"
                         % (Options.OPTIONS.from_stack, Options.OPTIONS.to_stack))

  # add user-defined template processing function
  catalog.add_handler(CatConst.TEMPLATE_HANDLER, _substitute_handler)

  if config_type is not None and config_type not in catalog.items:
    raise FatalException("Config type %s not exists, no configs was modified" % config_type)

  if config_type is not None:
    modify_config_item(config_type, catalog, Options.server_config_factory)
  else:
    for collection_name in catalog.items:
      modify_config_item(collection_name, catalog, Options.server_config_factory)

  Options.server_config_factory.process_mapping_transformations(catalog)

  # commit changes to server, if any will be found
  Options.server_config_factory.commit()


def backup_configs(conf_type=None):
  dir = "backups_%d" % time.time()
  file_pattern = "%s%s%s_%s.json"
  configs = get_config_resp_all()
  if configs is None:
    Options.logger.error("Unexpected response from the server")
    return -1

  if conf_type is not None and conf_type in configs:
    configs = {conf_type: configs[conf_type]}

  if not os.path.exists(dir):
    os.mkdir(dir)

  for item in configs:
    filename = file_pattern % (dir, os.path.sep, item, configs[item]["tag"])
    if os.path.exists(filename):
      os.remove(filename)

    try:
      with open(filename, "w") as f:
        f.write(json.dumps(configs[item][CatConst.STACK_PROPERTIES], indent=4))
      Options.logger.info("Catalog \"%s\" stored to %s", item, filename)
    except IOError as e:
      Options.logger.error("Unable to store \"%s\": %s", item, e)


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
      curl(SERVICE_URL_FORMAT.format(SERVICES[index]), validate=True, request_type="PUT", data=PUT_IN_INSTALLED[index])
    except FatalException as e:
      if not e.code == 0:
        err_retcode = e.code
        err_message = err_message + " Error while installing " + SERVICES[index] + ". Details: " + e.message + "."

  if err_retcode != 0:
    raise FatalException(err_retcode,
                         err_message + "(Services may already be installed or agents are not yet started.)")

  Options.OPTIONS.exit_message = "Requests has been submitted to install YARN and MAPREDUCE2. Use Ambari Web to monitor " \
                                 "the status of the install requests."


def generate_auth_header(user, password):
  token = "%s:%s" % (user, password)
  token = base64.encodestring(token)
  return {"Authorization": "Basic %s" % token.replace('\n', '')}


def curl(url, tokens=None, headers=None, request_type="GET", data=None, parse=False,
         simulate=None, validate=False, soft_validation=False):
  _headers = {}
  handler_chain = []
  post_req = ["POST", "PUT"]
  get_req = ["GET", "DELETE"]

  simulate_only = Options.CURL_PRINT_ONLY is not None or (simulate is not None and simulate is True)
  print_url = Options.CURL_PRINT_ONLY is not None and simulate is not None
  if request_type not in post_req + get_req:
    raise IOError("Wrong request type \"%s\" passed" % request_type)

  if data is not None and isinstance(data, dict):
    data = json.dumps(data)

  if tokens is not None:
    _headers.update(generate_auth_header(tokens["user"], tokens["pass"]))
  elif Options.API_TOKENS is not None:
    _headers.update(generate_auth_header(Options.API_TOKENS["user"], Options.API_TOKENS["pass"]))

  if request_type in post_req and data is not None:
    _headers["Content-Length"] = len(data)

  if headers is not None:
    _headers.update(headers)

  if Options.HEADERS is not None:
    _headers.update(Options.HEADERS)

  director = build_opener(*handler_chain)
  if request_type in post_req:
    _data = bytes(data)
    req = Request(url, headers=_headers, data=_data)
  else:
    req = Request(url, headers=_headers)

  req.get_method = lambda: request_type

  if print_url:
    Options.logger.info(url)

  code = 200
  if not simulate_only:
    try:
      resp = director.open(req)
      out = resp.read()
      if isinstance(out, bytes):
        out = out.decode("utf-8")
      code = resp.code
    except URLError as e:
      Options.logger.error(str(e))
      if isinstance(e, HTTPError):
        raise e
      else:
        raise FatalException(-1, str(e))
  else:
    if not print_url:
      Options.logger.info(url)
    out = "{}"

  if validate and not simulate_only and (code > 299 or code < 200):
    if soft_validation:
      Options.logger.warning("Response validation failed, please check previous action result manually.")
    else:
      raise FatalException(code, "Response validation failed, please check previous action result manually.")

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
  :param actual_properties_list
  :return:
  """

  verified_catalog = []
  catalog_properties = dict(catalog)
  actual_properties = dict(actual_properties_list)

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
      "catalog_value": catalog_properties[x][CatConst.PROPERTY_VALUE_TAG] if CatConst.PROPERTY_VALUE_TAG in
                                                                             catalog_properties[x] else None,
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
      if property_item["actual_value"] is None \
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
  catalog = catalog_farm.get_catalog(Options.OPTIONS.from_stack,
                                     Options.OPTIONS.to_stack)  # get desired version of catalog
  server_configs = ServerConfigFactory()

  if catalog is None:
    raise FatalException(1, "Upgrade catalog for version %s-%s not found"
                         % (Options.OPTIONS.from_stack, Options.OPTIONS.to_stack))

  if config_type is not None and config_type not in catalog.items.keys() and config_type not in server_configs.items():
    raise FatalException("Config type %s not exists" % config_type)

  # fetch from server all option at one time and filter only desired versions

  if config_type is not None:
    diff_list[config_type] = configuration_item_diff(config_type, catalog.items[config_type], server_configs.get_config(config_type).properties)
  else:
    for collection_name in catalog.items.keys():
      diff_list[collection_name] = configuration_item_diff(collection_name, catalog.items[collection_name], server_configs.get_config(collection_name).properties)

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
      report_file.write("%s: property \"%s\" is set to \"%s\", but should be set to \"%s\"\n" % (
        prefix, item["property"], item["actual_value"], item["catalog_value"]
      ))


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
