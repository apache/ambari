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

import os

from ambari_commons.exceptions import FatalException
from ambari_commons.logging_utils import print_info_msg
from ambari_commons.os_utils import search_file
from ambari_metrics_collector.properties import Properties


AMS_CONF_VAR = "AMS_CONF"
DEFAULT_CONF_DIR = "conf"
AMS_PROPERTIES_FILE = "ams.properties"

JAVA_HOME = "JAVA_HOME"

DEBUG_MODE_KEY = "ams.server.debug"
SUSPEND_START_MODE_KEY = "ams.server.debug.suspend.start"

SERVER_OUT_FILE_KEY = "ams.output.file.path"

DEFAULT_LIBS_DIR = "lib"

EMBEDDED_HBASE_MASTER_SERVICE = "ams_hbase_master"

EMBEDDED_HBASE_SUBDIR = "hbase"

JAVA_EXE_SUBPATH = "bin\\java.exe"

JAVA_HEAP_MAX_DEFAULT = "-Xmx1g"

HADOOP_HEAPSIZE = "HADOOP_HEAPSIZE"
HADOOP_HEAPSIZE_DEFAULT = "1024"

DEBUG_MODE = False
SUSPEND_START_MODE = False

OUT_DIR = "\\var\\log\\ambari-metrics-collector"
SERVER_OUT_FILE = OUT_DIR + "\\ambari-metrics-collector.out"
SERVER_LOG_FILE = OUT_DIR + "\\ambari-metrics-collector.log"

PID_DIR = "\\var\\run\\ambari-metrics-collector"
PID_OUT_FILE = PID_DIR + "\\ambari-metrics-collector.pid"
EXITCODE_OUT_FILE = PID_DIR + "\\ambari-metrics-collector.exitcode"

SERVICE_USERNAME_KEY = "TMP_AMC_USERNAME"
SERVICE_PASSWORD_KEY = "TMP_AMC_PASSWORD"

SETUP_ACTION = "setup"
START_ACTION = "start"
STOP_ACTION = "stop"
RESTART_ACTION = "restart"
STATUS_ACTION = "status"

def get_conf_dir():
  try:
    conf_dir = os.environ[AMS_CONF_VAR]
  except KeyError:
    conf_dir = DEFAULT_CONF_DIR
  return conf_dir

def find_properties_file():
  conf_file = search_file(AMS_PROPERTIES_FILE, get_conf_dir())
  if conf_file is None:
    err = 'File %s not found in search path $%s: %s' % (AMS_PROPERTIES_FILE,
                                                        AMS_CONF_VAR, get_conf_dir())
    print err
    raise FatalException(1, err)
  else:
    print_info_msg('Loading properties from ' + conf_file)
  return conf_file

# Load AMC properties and return dict with values
def get_properties():
  conf_file = find_properties_file()

  properties = None
  try:
    properties = Properties()
    properties.load(open(conf_file))
  except (Exception), e:
    print 'Could not read "%s": %s' % (conf_file, e)
    return -1
  return properties

def get_value_from_properties(properties, key, default=""):
  try:
    value = properties.get_property(key)
    if not value:
      value = default
  except:
    return default
  return value

def get_java_cp():
  conf_dir = get_conf_dir()
  conf_dir = os.path.abspath(conf_dir) + os.pathsep + os.path.join(os.path.abspath(DEFAULT_LIBS_DIR), "*")
  if conf_dir.find(' ') != -1:
    conf_dir = '"' + conf_dir + '"'
  return conf_dir

def find_jdk():
  try:
    java_home = os.environ[JAVA_HOME]
  except Exception:
    # No JAVA_HOME set
    err = "ERROR: JAVA_HOME is not set and could not be found."
    raise FatalException(1, err)

  if not os.path.isdir(java_home):
    err = "ERROR: JAVA_HOME {0} does not exist.".format(java_home)
    raise FatalException(1, err)

  java_exe = os.path.join(java_home, JAVA_EXE_SUBPATH)
  if not os.path.isfile(java_exe):
    err = "ERROR: {0} is not executable.".format(java_exe)
    raise FatalException(1, err)

  return java_home

def get_java_exe_path():
  jdk_path = find_jdk()
  java_exe = os.path.join(jdk_path, JAVA_EXE_SUBPATH)
  return java_exe

def build_jvm_args():
  try:
    # check envvars which might override default args
    hadoop_heapsize = os.environ[HADOOP_HEAPSIZE]
    java_heap_max = "-Xms{0}m".format(hadoop_heapsize)
  except Exception:
    java_heap_max = JAVA_HEAP_MAX_DEFAULT

  return java_heap_max
