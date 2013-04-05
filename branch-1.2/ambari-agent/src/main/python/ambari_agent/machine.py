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

import sys
import subprocess

# please keep compatible with Python 2.4 or greater
def doExec(key, command, preLF=False):
  try:
    osStat = subprocess.Popen(command, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
    out, err = osStat.communicate()
    if 0 != osStat.returncode or 0 == len(out.strip()):
      print "%s: UNAVAILABLE" % (key)
    else:
      if (preLF):
        print "%s: ok\n %s" % (key, out.strip())
      else:
        print "%s: ok %s" % (key, out.strip())
  except:
    print "%s: UNAVAILABLE" % (key)
  
def main(argv=None):
  doExec('hostname', ["hostname", "-f"])
  doExec('ip', ["hostname", "-i"])
  doExec('cpu', ["sh", "-c", "cat /proc/cpuinfo | grep 'model name' | awk -F': ' '{ print $2; }'"])
  doExec('memory', ["sh", "-c", "cat /proc/meminfo | grep MemTotal | awk -F': ' '{ print $2/1024/1024 \" GB\"; }'"])
  doExec('disks', ["df", "-h"], True)
  doExec('os', ["sh", "-c", "cat /etc/issue.net | head -1"])
  doExec('iptables', ["iptables", "-vnL"], True)
  doExec('selinux', ["sh", "-c", "cat /etc/selinux/config | grep ^SELINUX"])

  for REQ in (["yum", "rpm", "openssl", "curl", "wget", "net-snmp", "net-snmp-utils", "ntpd"]):
   doExec(REQ, ["rpm", "-qa", REQ])

  for OPT in (["ruby", "puppet", "nagios", "ganglia", "passenger", "hadoop"]):
   doExec(OPT, ["rpm", "-qa", OPT])

  doExec("yum_repos", ["sh", "-c", "yum -C repolist enabled | egrep \"(AMBARI|HDP)\""], True)
  # for SUSE-based agents
  doExec("zypper_repos", ["sh", "-c", "zypper repos | egrep \"(AMBARI|HDP)\""], True)

if __name__ == '__main__':
  main(sys.argv)
