#!/usr/bin/env python
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
import os
import sys
import inspect

LOG = logging.getLogger(__name__)
#------------------------------------------------------------------------------
'''
Function that searches for all plugins from a file
Input   : folder
Output  : dict
'''
#------------------------------------------------------------------------------


def get_plugins(module):
    logging.debug('[Module: %s]\n' % module.__name__)

    plugin_method_map = {}
    count = 0

    for name in dir(module):
        obj = getattr(module, name)
        if inspect.isclass(obj):
            count += 1
        elif (inspect.ismethod(obj) or inspect.isfunction(obj)):
            if obj.__name__.startswith("do_") or obj.__name__.startswith(
                    "help_") or obj.__name__.startswith("complete_") or obj.__name__.startswith("t_"):
                logging.debug("%s ,%s ", obj.__name__, obj)
                plugin_method_map.update({obj.__name__: obj})
                count += 1
            elif inspect.isbuiltin(obj):
                count += 1
    logging.debug(plugin_method_map)
    if count == 0:
        logging.debug('(No members)')

    return plugin_method_map


def import_modules(dirr):
    module_list = []
    for f in os.listdir(os.path.abspath(dirr)):
        module_name, ext = os.path.splitext(f)
        if ext == '.py' and module_name != "ambari_shell":
            logging.debug('imported module: %s' % (module_name))
            module = __import__(module_name)
            module_list.append(module)

    return module_list


def getPlugins(foldername):
    if os.path.isdir(foldername):
        sys.path.append(foldername)
        logging.debug('%s is a directory!' % (foldername))

    mod_list = import_modules(foldername)
    logging.debug(mod_list)

    plugin_method_map = {}
    for m in mod_list:
        dictt = get_plugins(m)
        if dictt:
            plugin_method_map.update(dictt)

    return plugin_method_map


def getPluginsFromModules(modulename):
    module = __import__(modulename)
    logging.debug(module)

    plugin_method_map = {}
    dictt = get_plugins(module)
    if dictt:
        plugin_method_map.update(dictt)

    return plugin_method_map

if __name__ == "__main__":
    print getPlugins("plug")
