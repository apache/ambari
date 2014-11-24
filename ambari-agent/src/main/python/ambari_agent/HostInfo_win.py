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
import logging
import time
import subprocess
from HostCheckReportFileHandler import HostCheckReportFileHandler
from shell import shellRunner
from ambari_commons.os_check import OSCheck, OSConst
from ambari_commons.os_windows import run_powershell_script, CHECK_FIREWALL_SCRIPT
import socket

logger = logging.getLogger()

# OS info
OS_VERSION = OSCheck().get_os_major_version()
OS_TYPE = OSCheck.get_os_type()
OS_FAMILY = OSCheck.get_os_family()

class HostInfo:
  # List of live services checked for on the host, takes a map of plan strings
  DEFAULT_LIVE_SERVICES = [
    {OSConst.WINSRV_FAMILY: "W32Time"}
  ]

  # Set of default users (need to be replaced with the configured user names)
  DEFAULT_USERS = [
    "hive", "ambari-qa", "oozie", "hbase", "hcat", "mapred",
    "hdfs", "rrdcached", "zookeeper", "flume", "sqoop", "sqoop2",
    "hue", "yarn"
  ]

  # Filters used to identify processed
  PROC_FILTER = [
    "hadoop", "zookeeper"
  ]

  RESULT_UNAVAILABLE = "unable_to_determine"

  SERVICE_STATUS_CMD = 'If ((Get-Service | Where-Object {{$_.Name -eq \'{0}\'}}).Status -eq \'Running\') {{echo "Running"; $host.SetShouldExit(0)}} Else {{echo "Stopped"; $host.SetShouldExit(1)}}'
  GET_USERS_CMD = '$accounts=(Get-WmiObject -Class Win32_UserAccount -Namespace "root\cimv2" -Filter "LocalAccount=\'$True\'" -ComputerName "LocalHost" -ErrorAction Stop); foreach ($acc in $accounts) {echo $acc.Name}'
  GET_JAVA_PROC_CMD = 'foreach ($process in (gwmi Win32_Process -Filter "name = \'java.exe\'")){echo $process.ProcessId;echo $process.CommandLine; echo $process.GetOwner().User}'

  current_umask = -1

  def __init__(self, config=None):
    self.reportFileHandler = HostCheckReportFileHandler(config)

  def dirType(self, path):
    if not os.path.exists(path):
      return 'not_exist'
    elif os.path.islink(path):
      return 'sym_link'
    elif os.path.isdir(path):
      return 'directory'
    elif os.path.isfile(path):
      return 'file'
    return 'unknown'

  def checkLiveServices(self, services, result):
    osType = OSCheck.get_os_family()
    for service in services:
      svcCheckResult = {}
      if isinstance(service, dict):
        serviceName = service[osType]
      else:
        serviceName = service

      service_check_live = ["powershell",'-noProfile', '-NonInteractive',  '-nologo', "-Command", self.SERVICE_STATUS_CMD.format(serviceName)]
      svcCheckResult['name'] = serviceName
      svcCheckResult['status'] = "UNKNOWN"
      svcCheckResult['desc'] = ""
      try:
        osStat = subprocess.Popen(service_check_live, stdout=subprocess.PIPE,
                                  stderr=subprocess.PIPE)
        out, err = osStat.communicate()
        if 0 != osStat.returncode:
          svcCheckResult['status'] = "Unhealthy"
          svcCheckResult['desc'] = out
          if len(out) == 0:
            svcCheckResult['desc'] = err
        else:
          svcCheckResult['status'] = "Healthy"
      except Exception, e:
        svcCheckResult['status'] = "Unhealthy"
        svcCheckResult['desc'] = repr(e)
      result.append(svcCheckResult)

  #TODO get user directory
  def checkUsers(self, users, results):
    get_users_cmd = ["powershell",'-noProfile', '-NonInteractive',  '-nologo', "-Command", self.GET_USERS_CMD]
    try:
      osStat = subprocess.Popen(get_users_cmd, stdout=subprocess.PIPE,                               stderr=subprocess.PIPE)
      out, err = osStat.communicate()
    except:
      raise Exception("Failed to get users.")
    for user in out.split(os.linesep):
      if user in users:
        result = {}
        result['name'] = user
        result['status'] = "Available"
        results.append(result)

  def javaProcs(self, list):
    try:
      runner = shellRunner()
      command_result = runner.run(["powershell",'-noProfile', '-NonInteractive',  '-nologo', "-Command", self.GET_JAVA_PROC_CMD])
      if command_result["exitCode"] == 0:
        splitted_output = command_result["output"].split(os.linesep)
        for i in [index for index in range(0,len(splitted_output)) if (index % 3)==0]:
          pid = splitted_output[i]
          cmd = splitted_output[i+1]
          user = splitted_output[i+2]
          if not 'AmbariServer' in cmd:
            if 'java' in cmd:
              dict = {}
              dict['pid'] = int(pid)
              dict['hadoop'] = False
              for filter in self.PROC_FILTER:
                if filter in cmd:
                  dict['hadoop'] = True
              dict['command'] = cmd.strip()
              dict['user'] = user
              list.append(dict)
    except Exception as e:
      pass
    pass

  def getUMask(self):
    if (self.current_umask == -1):
      self.current_umask = os.umask(self.current_umask)
      os.umask(self.current_umask)
      return self.current_umask
    else:
      return self.current_umask

  def checkIptables(self):
    out = run_powershell_script(CHECK_FIREWALL_SCRIPT)
    if out[0] != 0:
      logger.warn("Unable to check firewall status:{0}".format(out[2]))
      return False
    profiles_status = [i for i in out[1].split("\n") if not i == ""]
    if "1" in profiles_status:
      return True
    return False

  """ Return various details about the host
  componentsMapped: indicates if any components are mapped to this host
  commandsInProgress: indicates if any commands are in progress
  """
  def register(self, dict, componentsMapped=True, commandsInProgress=True):
    dict['hostHealth'] = {}

    java = []
    self.javaProcs(java)
    dict['hostHealth']['activeJavaProcs'] = java

    liveSvcs = []
    self.checkLiveServices(self.DEFAULT_LIVE_SERVICES, liveSvcs)
    dict['hostHealth']['liveServices'] = liveSvcs

    dict['umask'] = str(self.getUMask())

    dict['iptablesIsRunning'] = self.checkIptables()
    dict['reverseLookup'] = self.checkReverseLookup()
    # If commands are in progress or components are already mapped to this host
    # Then do not perform certain expensive host checks
    if componentsMapped or commandsInProgress:
      dict['existingRepos'] = [self.RESULT_UNAVAILABLE]
      dict['installedPackages'] = []
      dict['alternatives'] = []
      dict['stackFoldersAndFiles'] = []
      dict['existingUsers'] = []
    else:
      existingUsers = []
      self.checkUsers(self.DEFAULT_USERS, existingUsers)
      dict['existingUsers'] = existingUsers
      #TODO check HDP stack and folders here
      self.reportFileHandler.writeHostCheckFile(dict)
      pass

    # The time stamp must be recorded at the end
    dict['hostHealth']['agentTimeStampAtReporting'] = int(time.time() * 1000)

    pass

  def checkReverseLookup(self):
    """
    Check if host fqdn resolves to current host ip
    """
    try:
      host_name = socket.gethostname().lower()
      host_ip = socket.gethostbyname(host_name)
      host_fqdn = socket.getfqdn().lower()
      fqdn_ip = socket.gethostbyname(host_fqdn)
      return host_ip == fqdn_ip
    except socket.error:
      pass
    return False

def main(argv=None):
  h = HostInfo()
  struct = {}
  h.register(struct)
  print struct


if __name__ == '__main__':
  main()
