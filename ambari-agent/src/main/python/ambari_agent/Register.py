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
import json
from Hardware import Hardware
from ActionQueue import ActionQueue
from ServerStatus import ServerStatus
import socket
import time
import urllib2
import subprocess


firstContact = True
class Register:
  """ Registering with the server. Get the hardware profile and 
  declare success for now """
  def __init__(self):
    self.hardware = Hardware()

  def pfqdn(self):
    try:
      handle = urllib2.urlopen('http://169.254.169.254/latest/meta-data/public-hostname', '', 3)
      str = handle.read()
      handle.close()
      return str
    except Exception, e:
      return socket.getfqdn()

  def build(self, id='-1'):
    global clusterId, clusterDefinitionRevision, firstContact
    timestamp = int(time.time()*1000)
    register = { 'responseId'        : int(id),
                  'timestamp'         : timestamp,
                  'hostname'          : socket.getfqdn(),
                  'publicHostname'    : self.pfqdn(),
                  'hardwareProfile'   : self.hardware.get(),
                }
    return register

def doExec(vals, key, command, preLF=False):
  template = "{0}: {1} {2}"
  try:
    osStat = subprocess.Popen(command, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
    out, err = osStat.communicate()
    if 0 != osStat.returncode or 0 == len(out.strip()):
      print template.format(key, "UNAVAILABLE", "")
    else:
      if (preLF):
        print template.format(key, "ok,\n", out.strip())
      else:
        print template.format(key, "ok,", out.strip())
  except:
    print template.format(key, "UNAVAILABLE", "")
  

# Linux only
def machineInfo():
  vals = { }
  doExec(vals, 'hostname', ["hostname", "-f"])
  doExec(vals, 'ip', ["hostname", "-i"])
  doExec(vals, 'cpu', ["sh", "-c", "cat /proc/cpuinfo | grep 'model name' | awk -F': ' '{ print $2; }'"])
  doExec(vals, 'memory', ["sh", "-c", "cat /proc/meminfo | grep MemTotal | awk -F': ' '{ print $2/1024/1024 \" GB\"; }'"])
  doExec(vals, 'disks', ["df", "-h"], True)
  doExec(vals, 'os', ["sh", "-c", "cat /etc/issue.net | head -1"])
  doExec(vals, 'iptables', ["iptables", "-vnL"], True)
  doExec(vals, 'selinux', ["sh", "-c", "cat /etc/selinux/config | grep ^SELINUX"])

  rpm_req = { }
  for REQ in (["yum", "rpm", "openssl", "curl", "wget", "net-snmp", "net-snmp-utils", "ntpd"]):
   doExec(rpm_req, REQ, ["rpm", "-qa", REQ])
  vals["required_packages"] = rpm_req

  rpm_opt = { }
  for OPT in (["ruby", "puppet", "nagios", "ganglia", "passenger", "hadoop"]):
   doExec(rpm_opt, OPT, ["rpm", "-qa", OPT])
  vals["optional_packages"] = rpm_opt

  doExec(vals, "yum_repos", ["sh", "-c", "yum -C repolist enabled | egrep \"(AMBARI|HDP)\""], True)
  # for SUSE-based agents
  doExec(vals, "zypper_repos", ["sh", "-c", "zypper repos | egrep \"(AMBARI|HDP)\""], True)
  
  
def main(argv=None):
  if len(argv) == 1:
    register = Register()
    print json.dumps(register.build())
  else:
    machineInfo()

if __name__ == '__main__':
  main(sys.argv)
