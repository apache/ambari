#!/usr/bin/env python
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
from instance_manager import *

import optparse
import sys
import ast

CREATE_MPACK_INSTANCE_ACTION = 'create-mpack-instance'
SET_MPACK_INSTANCE_ACTION = 'set-mpack-instance'
GET_CONF_DIR_ACTION = 'get-conf-dir'
GET_LOG_DIR_ACTION = 'get-log-dir'
GET_RUN_DIR_ACTION = 'get-run-dir'
LIST_INSTANCES_ACTION = 'list-instances'


def init_action_parser(action, parser):
  action_parser_map = {
    CREATE_MPACK_INSTANCE_ACTION: init_create_parser_options,
    SET_MPACK_INSTANCE_ACTION: init_set_parser_options,
    GET_CONF_DIR_ACTION: init_get_parser_options,
    GET_LOG_DIR_ACTION: init_get_parser_options,
    GET_RUN_DIR_ACTION: init_get_parser_options,
    LIST_INSTANCES_ACTION: init_get_parser_options
  }
  try:
    action_parser_map[action](parser)
  except KeyError:
    parser.error(
      "Invalid action {0}. Possible options are: create-mpack-instance|set-mpack-instance|get-conf-dir|list-instances".format(
        action))


def check_required_options(options, action, parser):
  missing_options = []
  if action == CREATE_MPACK_INSTANCE_ACTION or action == SET_MPACK_INSTANCE_ACTION:
    if not options.mpack:
      missing_options.append("mpack")
    if not options.mpack_version:
      missing_options.append("mpack-version")
    if not options.mpack_instance:
      missing_options.append("mpack-instance")
    if not options.module_name:
      missing_options.append("module-name")

    if missing_options:
      parser.error("Missing following required command options: {0}".format(missing_options))

    if not options.components_map and not options.components:
      parser.error("Either components or components-map option must be specified.")

    if options.components_map and options.components:
      parser.error("Only components or components-map option could be specified. Can't use both.")


def init_create_parser_options(parser):
  parser.add_option('--mpack', default=None, help="selected 'mpack_name'. eg: edw, core", dest="mpack")
  parser.add_option('--mpack-version', default=None, help="selected 'mpack_version'. eg. 1.0.0.-b1",
                    dest="mpack_version")
  parser.add_option('--mpack-instance', default=None,
                    help="new/existing 'mpack_instance_name'. eg: Production, eCommerce", dest="mpack_instance")
  parser.add_option('--subgroup-name', default="default", help="new/existing 'subgroup_name' eg: hive-for-finance",
                    dest="subgroup_name")
  parser.add_option('--module-name', default=None,
                    help="selected 'service/client_module' eg: hive, hdfs, hive_client etc", dest="module_name")
  parser.add_option('--components', default=None,
                    help="list of 'components_instances_name' to be created. (default) OR '*' for all components",
                    dest="components")
  parser.add_option('--components-map', default=None,
                    help="map of 'component type' (eg: hive_server, metastore etc) as key and List of component instance name(s) to be given (eg: HS-1, finance_metastore) as value OR Empty map to create single instance of all components with name 'default'",
                    dest="components_map")


def init_set_parser_options(parser):
  parser.add_option('--mpack', default=None, help="selected 'mpack_name'. eg: edw, core", dest="mpack")
  parser.add_option('--mpack-version', default=None,
                    help="selected 'mpack_version' on which we want to move to. eg. 1.5.0.-b1", dest="mpack_version")
  parser.add_option('--mpack-instance', default=None, help="existing 'mpack_instance_name'. eg: Production, eCommerce",
                    dest="mpack_instance")
  parser.add_option('--subgroup-name', default="default", help="existing 'subgroup_name' eg: hive-for-finance",
                    dest="subgroup_name")
  parser.add_option('--module-name', default=None,
                    help="selected 'service/client_module' eg: hive, hdfs, hive_client etc", dest="module_name")
  parser.add_option('--components', default=None,
                    help="list of 'components_instances_name' to be updated. (default) OR '*' for all components",
                    dest="components")
  parser.add_option('--components-map', default=None,
                    help="map of 'component type' (eg: hive_server, metastore etc) as key and List of component instance name(s) to be given (eg: HS-1, finance_metastore) as value OR Empty map to update instance of all components with name 'default'",
                    dest="components_map")


def init_get_parser_options(parser):
  parser.add_option('--mpack', default=None,
                    help="'mpack_name' to which component instance belongs. eg: edw, core'. eg: edw, core",
                    dest="mpack")
  parser.add_option('--mpack-instance', default=None, help="'mpack_instance_name'. (eg: default)",
                    dest="mpack_instance")
  parser.add_option('--subgroup-name', default="default", help="'subgroup_name' eg: hive-for-finance",
                    dest="subgroup_name")
  parser.add_option('--module-name', default=None,
                    help="selected 'service/client_module' eg: hive, hdfs, hive_client etc", dest="module_name")
  parser.add_option('--components-map', default=None,
                    help="map of 'component type' (eg: hive_server, metastore etc) as key and List of component instance name(s) to be given (eg: HS-1, finance_metastore) as value OR Empty map for all component instances present",
                    dest="components_map")


def main(options, args):
  action = sys.argv[1]

  parsed_components = None
  parsed_components_map = None
  try:
    if hasattr(options, 'components') and options.components:
      if options.components == '*':
        parsed_components = '*'
      else:
        parsed_components = ast.literal_eval(options.components)
    if options.components_map:
      parsed_components_map = ast.literal_eval(options.components_map)
  except ValueError:
    raise ValueError("Components or components-map format is invalid.")

  if action == CREATE_MPACK_INSTANCE_ACTION:
    create_mpack(mpack_name=options.mpack, mpack_version=options.mpack_version,
                 mpack_instance=options.mpack_instance,
                 subgroup_name=options.subgroup_name, module_name=options.module_name,
                 components=parsed_components,
                 components_map=parsed_components_map)

  elif action == SET_MPACK_INSTANCE_ACTION:
    set_mpack_instance(mpack=options.mpack, mpack_version=options.mpack_version,
                       mpack_instance=options.mpack_instance,
                       subgroup_name=options.subgroup_name, module_name=options.module_name,
                       components=parsed_components,
                       components_map=parsed_components_map)

  elif action == GET_CONF_DIR_ACTION:
    print get_conf_dir(mpack=options.mpack, mpack_instance=options.mpack_instance,
                       subgroup_name=options.subgroup_name, module_name=options.module_name,
                       components_map=parsed_components_map)

  elif action == GET_LOG_DIR_ACTION:
    print get_log_dir(mpack=options.mpack, mpack_instance=options.mpack_instance,
                       subgroup_name=options.subgroup_name, module_name=options.module_name,
                       components_map=parsed_components_map)

  elif action == GET_RUN_DIR_ACTION:
    print get_run_dir(mpack=options.mpack, mpack_instance=options.mpack_instance,
                       subgroup_name=options.subgroup_name, module_name=options.module_name,
                       components_map=parsed_components_map)

  elif action == LIST_INSTANCES_ACTION:
    print list_instances(mpack=options.mpack, mpack_instance=options.mpack_instance,
                         subgroup_name=options.subgroup_name, module_name=options.module_name,
                         components_map=parsed_components_map)


if __name__ == "__main__":
  if len(sys.argv) < 2:
    print(
      "Missing the command. Possible options are: create-mpack-instance|set-mpack-instance|get-conf-dir|get-log-dir|get-run-dir|list-instances")
    sys.exit(1)

  usage = "Usage: %prog create-mpack-instance|set-mpack-instance|get-conf-dir|get-log-dir|get-run-dir|list-instances [options]"
  parser = optparse.OptionParser(usage=usage)
  action = sys.argv[1]
  init_action_parser(action, parser)
  (options, args) = parser.parse_args()
  check_required_options(options, action, parser)

  try:
    main(options, args)
  except (KeyboardInterrupt, EOFError):
    print("\nAborting ... Keyboard Interrupt.")
    sys.exit(1)
  except ValueError as e:
    print "ERROR: " + e.message
    sys.exit(1)
