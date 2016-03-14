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

import datetime
import glob
import os
import re
import shutil
import stat
import string
import sys
import tempfile

import ambari_server
from ambari_commons.logging_utils import print_info_msg
from resource_management.core.shell import quote_bash_args
AMBARI_CONF_VAR = "AMBARI_CONF_DIR"
SERVER_CLASSPATH_KEY = "SERVER_CLASSPATH"
LIBRARY_PATH_KEY = "LD_LIBRARY_PATH"
AMBARI_SERVER_LIB = "AMBARI_SERVER_LIB"
JDBC_DRIVER_PATH_PROPERTY = "server.jdbc.driver.path"



class ServerClassPath():

  properties = None
  options = None
  configDefaults = None


  def __init__(self, properties, options):
    self.properties = properties
    self.options = options
    self.configDefaults = ambari_server.serverConfiguration.ServerConfigDefaults()


  def _get_ambari_jars(self):
    try:
      conf_dir = os.environ[AMBARI_SERVER_LIB]
      return conf_dir
    except KeyError:
      default_jar_location = self.configDefaults.DEFAULT_LIBS_DIR
      print_info_msg(AMBARI_SERVER_LIB + " is not set, using default "
                     + default_jar_location)
      return default_jar_location

  def _get_jdbc_cp(self):
    jdbc_jar_path = ""
    if self.properties != -1:
      jdbc_jar_path = self.properties[JDBC_DRIVER_PATH_PROPERTY]
    return jdbc_jar_path

  def _get_ambari_classpath(self):
    ambari_class_path = os.path.abspath(self._get_ambari_jars() + os.sep + "*")

    # Add classpath from server.jdbc.driver.path property
    jdbc_cp = self._get_jdbc_cp()
    if len(jdbc_cp) > 0:
      ambari_class_path = ambari_class_path + os.pathsep + jdbc_cp

    # Add classpath from environment (SERVER_CLASSPATH)
    if SERVER_CLASSPATH_KEY in os.environ:
      ambari_class_path =  os.environ[SERVER_CLASSPATH_KEY] + os.pathsep + ambari_class_path

    # Add jdbc driver classpath
    if self.options:
      jdbc_driver_path = ambari_server.dbConfiguration.get_jdbc_driver_path(self.options, self.properties)
      if jdbc_driver_path not in ambari_class_path:
        ambari_class_path = ambari_class_path + os.pathsep + jdbc_driver_path

    # Add conf_dir to class_path
    conf_dir = ambari_server.serverConfiguration.get_conf_dir()
    ambari_class_path = conf_dir + os.pathsep + ambari_class_path

    return ambari_class_path

  def get_full_ambari_classpath_escaped_for_shell(self):
    class_path = self._get_ambari_classpath()

    # When classpath is required we should also set native libs os env variable
    # This is required for some jdbc (ex. sqlAnywhere)
    self.set_native_libs_path()

    return quote_bash_args(class_path)


  #
  # Set native libs os env
  #
  def set_native_libs_path(self):
    if self.options:
      native_libs_path = ambari_server.dbConfiguration.get_native_libs_path(self.options, self.properties)
      if native_libs_path is not None:
        if LIBRARY_PATH_KEY in os.environ:
          native_libs_path = os.environ[LIBRARY_PATH_KEY] + os.pathsep + native_libs_path
        os.environ[LIBRARY_PATH_KEY] = native_libs_path

