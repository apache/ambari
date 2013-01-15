#!/usr/bin/env python2.6

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

class HostInfo:

  def dirType(self, path):
    if not os.path.exists(path):
      return 'not_exist'
    elif os.path.islink(path):
      return 'sym_link'
    elif os.path.isdir(path):
      return 'directory'
    return 'unknown'

  def hadoopDir(self):
    return self.dirType('/etc/hadoop')

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

  def dirHelper(self, dict, name, prefix):
    dict[name] = self.dirType(os.path.join(prefix, name))

  def register(self, dict):
    dict['varLogHadoopLogCount'] = self.hadoopVarLogCount()
    dict['varRunHadoopPidCount'] = self.hadoopVarRunCount()

    etcDirs = { }
    self.dirHelper(etcDirs, 'hadoop', '/etc')
    etcDirs['hadoop_conf'] = self.dirType('/etc/hadoop/conf')
    self.dirHelper(etcDirs, 'hbase', '/etc')
    self.dirHelper(etcDirs, 'hcatalog', '/etc')
    self.dirHelper(etcDirs, 'hive', '/etc')
    self.dirHelper(etcDirs, 'oozie', '/etc')
    self.dirHelper(etcDirs, 'sqoop', '/etc')
    self.dirHelper(etcDirs, 'ganglia', '/etc')
    self.dirHelper(etcDirs, 'nagios', '/etc')
    dict['etcDirs'] = etcDirs
    
    varRunDirs = { }
    self.dirHelper(varRunDirs, 'hadoop', '/var/run')
    self.dirHelper(varRunDirs, 'zookeeper', '/var/run')
    self.dirHelper(varRunDirs, 'hbase', '/var/run')
    self.dirHelper(varRunDirs, 'templeton', '/var/run')
    self.dirHelper(varRunDirs, 'oozie', '/var/run')
    dict['varRunDirs'] = varRunDirs

    varLogDirs = { }
    self.dirHelper(varLogDirs, 'hadoop', '/var/log')
    self.dirHelper(varLogDirs, 'zookeeper', '/var/log')
    self.dirHelper(varLogDirs, 'hbase', '/var/log')
    self.dirHelper(varLogDirs, 'hive', '/var/log')
    self.dirHelper(varLogDirs, 'templeton', '/var/log')
    self.dirHelper(varLogDirs, 'nagios', '/var/log')
    dict['varLogDirs'] = varLogDirs

    java = []
    self.hadoopJava(java)
    dict['hadoopJavaProcs'] = java
    
  def hadoopJava(self, list):
    try:
      pids = [pid for pid in os.listdir('/proc') if pid.isdigit()]
      for pid in pids:
        cmd = open(os.path.join('/proc', pid, 'cmdline'), 'rb').read()
        if 'java' in cmd and 'hadoop' in cmd:
          for line in open(os.path.join('/proc', pid, 'status')):
            if line.startswith('Uid:'):
              uid = int(line.split()[1])
              dict = { }
              dict['user'] = pwd.getpwuid(uid).pw_name
              dict['pid'] = int(pid)
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
