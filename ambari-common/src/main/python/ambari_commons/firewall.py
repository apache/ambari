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

import subprocess
import shlex
from ambari_commons import OSCheck, OSConst


class Firewall(object):
  def __init__(self):
    # OS info
    self.OS_VERSION = OSCheck().get_os_major_version()
    self.OS_TYPE = OSCheck.get_os_type()
    self.OS_FAMILY = OSCheck.get_os_family()

  def getFirewallObject(self):
    if self.OS_TYPE == OSConst.OS_UBUNTU:
      return UbuntuFirewallChecks()
    elif self.OS_TYPE == OSConst.OS_FEDORA and int(self.OS_VERSION) >= 18:
      return Fedora18FirewallChecks()
    elif self.OS_FAMILY == OSConst.SUSE_FAMILY:
      return SuseFirewallChecks()
    else:
      return FirewallChecks()

class FirewallChecks(object):
  def __init__(self):
    self.FIREWALL_SERVICE_NAME = "iptables"
    self.SERVICE_SUBCMD = "status"
    # service cmd
    self.SERVICE_CMD = "/sbin/service"
    self.returncode = None
    self.stdoutdata = None
    self.stderrdata = None

  def get_command(self):
    return "%s %s %s" % (self.SERVICE_CMD, self.FIREWALL_SERVICE_NAME, self.SERVICE_SUBCMD)

  def check_result(self, retcode, out, err):
    result = False
    if retcode == 3:
      result = False
    elif retcode == 0:
      if "Table: filter" in out:
        result = True
    return result

  def check_iptables(self):
    try:
      retcode, out, err = self.run_os_command(self.get_command())
      return self.check_result(retcode, out, err)
    except OSError:
      return False

  def run_os_command(self, cmd):
    if type(cmd) == str:
      cmd = shlex.split(cmd)

    process = subprocess.Popen(cmd, stdout=subprocess.PIPE, stdin=subprocess.PIPE,
                               stderr=subprocess.PIPE)
    (stdoutdata, stderrdata) = process.communicate()
    self.returncode = process.returncode
    self.stdoutdata = stdoutdata
    self.stderrdata = stderrdata
    return self.returncode, self.stdoutdata, self.stderrdata



class UbuntuFirewallChecks(FirewallChecks):
  def __init__(self):
    super(UbuntuFirewallChecks, self).__init__()
    self.FIREWALL_SERVICE_NAME = "ufw"

  def get_command(self):
    return "%s %s" % (self.FIREWALL_SERVICE_NAME, self.SERVICE_SUBCMD)

  def check_result(self, retcode, out, err):
    # On ubuntu, the status command returns 0 whether running or not
    result = False
    if retcode == 0:
      if "Status: inactive" in out:
        result = False
      elif "Status: active" in out:
        result = True
    return result

class Fedora18FirewallChecks(FirewallChecks):
  def __init__(self):
    super(Fedora18FirewallChecks, self).__init__()

  def get_command(self):
    return "systemctl is-active %s" % (self.FIREWALL_SERVICE_NAME)

  def check_result(self, retcode, out, err):
    result = False
    if retcode == 0:
      if "active" in out:
        result = True
    return result

class SuseFirewallChecks(FirewallChecks):
  def __init__(self):
    super(SuseFirewallChecks, self).__init__()
    self.FIREWALL_SERVICE_NAME = "SuSEfirewall2"

  def get_command(self):
    return "%s %s" % (self.FIREWALL_SERVICE_NAME, self.SERVICE_SUBCMD)

  def check_result(self, retcode, out, err):
    result = False
    if retcode == 0:
      if "SuSEfirewall2 not active" in out:
        result = False
      elif "### iptables" in out:
        result = True
    return result
