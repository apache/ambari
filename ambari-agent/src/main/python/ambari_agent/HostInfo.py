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
import glob
import pwd
import subprocess
import AmbariConfig

class HostInfo:

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

  def rpmInfo(self, rpmList):
    config = AmbariConfig.config

    try:
      for rpmName in config.get('heartbeat', 'rpms').split(','):
        rpmName = rpmName.strip()
        rpm = { }
        rpm['name'] = rpmName

        try:
          osStat = subprocess.Popen(["rpm", "-q", rpmName], stdout=subprocess.PIPE, stderr=subprocess.PIPE)
          out, err = osStat.communicate()
          if (0 != osStat.returncode or 0 == len(out.strip())):
            rpm['installed'] = False
          else:
            rpm['installed'] = True
            rpm['version'] = out.strip()
        except:
          rpm['available'] = False

        rpmList.append(rpm)
    except:
      pass

  def hadoopVarRunCount(self):
    if not os.path.exists('/var/run/hadoop'):
      return 0
    pids = glob.glob('/var/run/hadoop/*/*.pid')
    return len(pids)

  def hadoopVarLogCount(self):
    if not os.path.exists('/var/log/hadoop'):
      return 0
    logs = glob.glob('/var/log/hadoop/*/*.log')
    return len(logs)
  
  def etcAlternativesConf(self, etcList):
    if not os.path.exists('/etc/alternatives'):
      return []
    confs = glob.glob('/etc/alternatives/*conf')

    for conf in confs:
      confinfo = { }
      realconf = conf
      if os.path.islink(conf):
        realconf = os.path.realpath(conf)
      confinfo['name'] = conf
      confinfo['target'] = realconf
      etcList.append(confinfo)

  def repos(self):
    # centos, redhat
    try:
      osStat = subprocess.Popen(["yum", "-C", "repolist"], stdout=subprocess.PIPE, stderr=subprocess.PIPE)
      out, err = osStat.communicate()
      return out
    except:
      pass
    # suse, only if above failed
    try:
      osStat = subprocess.Popen(["zypper", "repos"], stdout=subprocess.PIPE, stderr=subprocess.PIPE)
      out, err = osStat.communicate()
      return out
    except:
      pass

    # default, never return empty
    return "could_not_determine"
    

  def register(self, dict):
    dict['varLogHadoopLogCount'] = self.hadoopVarLogCount()
    dict['varRunHadoopPidCount'] = self.hadoopVarRunCount()
    
    etcs = []
    self.etcAlternativesConf(etcs)
    dict['etcAlternativesConf'] = etcs

    dirs = []
    config = AmbariConfig.config
    try:
      for dirName in config.get('heartbeat', 'dirs').split(','):
        obj = { }
        obj['type'] = self.dirType(dirName.strip())
        obj['name'] = dirName.strip()
        dirs.append(obj)
    except:
      pass

    dict['paths'] = dirs

    java = []
    self.javaProcs(java)
    dict['javaProcs'] = java

    rpms = []
    self.rpmInfo(rpms)
    dict['rpms'] = rpms

    dict['repoInfo'] = self.repos()
    
  def javaProcs(self, list):
    try:
      pids = [pid for pid in os.listdir('/proc') if pid.isdigit()]
      for pid in pids:
        cmd = open(os.path.join('/proc', pid, 'cmdline'), 'rb').read()
        cmd = cmd.replace('\0', ' ')
        if 'java' in cmd:
          dict = { }
          dict['pid'] = int(pid)
          dict['hadoop'] = True if 'hadoop' in cmd else False
          dict['command'] = cmd.strip()
          for line in open(os.path.join('/proc', pid, 'status')):
            if line.startswith('Uid:'):
              uid = int(line.split()[1])
              dict['user'] = pwd.getpwuid(uid).pw_name
          list.append(dict)
    except:
      pass
    pass

def main(argv=None):
  h = HostInfo()
  struct = { }
  h.register(struct)
  print struct

if __name__ == '__main__':
  main()
