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
import win32service
import win32api
from win32serviceutil import RemoveService, SmartOpenService
import winerror

from ambari_commons.xml_utils import ConvertToXml
from ambari_metrics_collector.serviceConfiguration import EMBEDDED_HBASE_MASTER_SERVICE, EMBEDDED_HBASE_SUBDIR, \
  find_jdk, get_java_exe_path, build_jvm_args

MASTER_JVM_ARGS = '{0} ' \
  '"-XX:+UseConcMarkSweepGC" "-Djava.net.preferIPv4Stack=true" ' \
  '-Djava.library.path="{6}" ' \
  '-Dhadoop.home.dir="{1}" -Dhbase.log.dir="{2}" -Dhbase.log.file={3} -Dhbase.home.dir="{1}" -Dhbase.id.str="{4}" ' \
  '-Dhbase.root.logger="INFO,DRFA" -Dhbase.security.logger="INFO,RFAS" ' \
  '-classpath "{5}" org.apache.hadoop.hbase.master.HMaster start'

#-Xmx1000m "-XX:+UseConcMarkSweepGC" "-Djava.net.preferIPv4Stack=true"
# -Dhbase.log.dir="C:\test\ambari-metrics-timelineservice-2.0.0-SNAPSHOT\hbase\logs" -Dhbase.log.file="hbase.log" -Dhbase.home.dir="C:\test\ambari-metrics-timelineservice-2.0.0-SNAPSHOT\hbase" -Dhbase.id.str="Administrator"
# -XX:OnOutOfMemoryError="taskkill /F /PID p" -Dhbase.root.logger="INFO,console" -Dhbase.security.logger="INFO,DRFAS"
# -classpath "C:\test\ambari-metrics-timelineservice-2.0.0-SNAPSHOT\hbase\conf;C:\jdk1.7.0_67\lib\tools.jar;C:\test\ambari-metrics-timelineservice-2.0.0-SNAPSHOT\hbase;C:\test\ambari-metrics-timelineservice-2.0.0-SNAPSHOT\hbase\lib\*"
# org.apache.hadoop.hbase.master.HMaster start
def _build_master_java_args(username = None):
  hbase_home_dir = os.path.abspath(EMBEDDED_HBASE_SUBDIR)
  hbase_log_dir = os.path.join(os.sep, "var", "log", EMBEDDED_HBASE_MASTER_SERVICE)
  hbase_log_file = "hbase.log"
  hbase_user_id = username if username else "SYSTEM"
  java_library_path = os.path.join(hbase_home_dir, "bin")
  if not os.path.exists(hbase_log_dir):
    os.makedirs(hbase_log_dir)

  java_class_path = os.path.join(hbase_home_dir, "conf")
  java_class_path += os.pathsep + os.path.join(find_jdk(), "lib", "tools.jar")
  java_class_path += os.pathsep + hbase_home_dir
  java_class_path += os.pathsep + os.path.join(hbase_home_dir, "lib", "*")

  args = MASTER_JVM_ARGS.format(build_jvm_args(), hbase_home_dir, hbase_log_dir, hbase_log_file, hbase_user_id, java_class_path, java_library_path)

  return args


def _get_config_file_path():
  config_file_path = os.path.join(os.getcwd(), EMBEDDED_HBASE_SUBDIR, "bin", EMBEDDED_HBASE_MASTER_SERVICE + ".xml")
  return config_file_path

class EmbeddedHBaseService:
  _svc_name_ = EMBEDDED_HBASE_MASTER_SERVICE
  _svc_display_name_ = "Apache Ambari Metrics " + EMBEDDED_HBASE_MASTER_SERVICE
  _exe_name_ = os.path.join(EMBEDDED_HBASE_SUBDIR, "bin", EMBEDDED_HBASE_MASTER_SERVICE + ".exe")

  @classmethod
  def _get_start_type(cls, startupMode):
    if startupMode == "auto":
      startType = win32service.SERVICE_AUTO_START
    elif startupMode == "disabled":
      startType = win32service.SERVICE_DISABLED
    else:
      startType = win32service.SERVICE_DEMAND_START
    return startType

  @classmethod
  def Install(cls, startupMode = "auto", username = None, password = None):
    print "Installing service %s" % (cls._svc_name_)

    # Configure master.xml, which drives the java subprocess32
    java_path = get_java_exe_path()
    java_args = _build_master_java_args(username)

    config_file_path = _get_config_file_path()

    xmlFileContents = _MasterXml()
    xmlFileContents.service.id = EMBEDDED_HBASE_MASTER_SERVICE
    xmlFileContents.service.name = EMBEDDED_HBASE_MASTER_SERVICE
    xmlFileContents.service.description = "This service runs " + EMBEDDED_HBASE_MASTER_SERVICE
    xmlFileContents.service.executable = java_path
    xmlFileContents.service.arguments = java_args

    xmlFile = open(config_file_path, "w")
    xmlFile.write( str(xmlFileContents) )
    xmlFile.close()

    startType = cls._get_start_type(startupMode)
    serviceType = win32service.SERVICE_WIN32_OWN_PROCESS
    errorControl = win32service.SERVICE_ERROR_NORMAL

    commandLine = os.path.abspath(cls._exe_name_)
    hscm = win32service.OpenSCManager(None,None,win32service.SC_MANAGER_ALL_ACCESS)
    try:
      try:
        hs = win32service.CreateService(hscm,
                                        cls._svc_name_,
                                        cls._svc_display_name_,
                                        win32service.SERVICE_ALL_ACCESS,         # desired access
                                        serviceType,        # service type
                                        startType,
                                        errorControl,       # error control type
                                        commandLine,
                                        None,
                                        0,
                                        None,
                                        username,
                                        password)
        print "Service installed"
        win32service.CloseServiceHandle(hs)
      finally:
        win32service.CloseServiceHandle(hscm)
    except win32service.error, exc:
      if exc.winerror==winerror.ERROR_SERVICE_EXISTS:
        cls.Update(username, password)
      else:
        print "Error installing service: %s (%d)" % (exc.strerror, exc.winerror)
        err = exc.winerror
    except ValueError, msg: # Can be raised by custom option handler.
      print "Error installing service: %s" % str(msg)
      err = -1
      # xxx - maybe I should remove after _any_ failed install - however,
      # xxx - it may be useful to help debug to leave the service as it failed.
      # xxx - We really _must_ remove as per the comments above...
      # As we failed here, remove the service, so the next installation
      # attempt works.
      try:
        RemoveService(cls._svc_name_)
      except win32api.error:
        print "Warning - could not remove the partially installed service."

  @classmethod
  def Update(cls, startupMode = "auto", username = None, password = None):
    # Handle the default arguments.
    if startupMode is None:
      startType = win32service.SERVICE_NO_CHANGE
    else:
      startType = cls._get_start_type(startupMode)

    hscm = win32service.OpenSCManager(None,None,win32service.SC_MANAGER_ALL_ACCESS)
    serviceType = win32service.SERVICE_WIN32_OWN_PROCESS

    commandLine = os.path.abspath(cls._exe_name_)

    try:
      hs = SmartOpenService(hscm, cls._svc_name_, win32service.SERVICE_ALL_ACCESS)
      try:
        win32service.ChangeServiceConfig(hs,
                                         serviceType,  # service type
                                         startType,
                                         win32service.SERVICE_NO_CHANGE,       # error control type
                                         commandLine,
                                         None,
                                         0,
                                         None,
                                         username,
                                         password,
                                         cls._svc_display_name_)
        print "Service updated"
      finally:
        win32service.CloseServiceHandle(hs)
    finally:
      win32service.CloseServiceHandle(hscm)

class _MasterXml(ConvertToXml):
  service = ""  #Service entity

  def __init__(self):
    self.service = _ServiceXml()

  def __str__(self):
    result = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
    result += str(self.service)
    return result

class _ServiceXml(ConvertToXml):
  def __init__(self):
    self.id = ""
    self.name = ""
    self.description = ""
    self.executable = ""
    self.arguments = ""

  def __str__(self):
    result = "<service>\n"
    result += self.attributesToXml()
    result += "</service>"
    return result
