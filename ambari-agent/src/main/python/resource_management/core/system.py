#!/usr/bin/env python
"""
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

Ambari Agent

"""

__all__ = ["System"]

import os
import sys
import platform
from resource_management.core import shell
from resource_management.core.exceptions import Fail
from functools import wraps

def lazy_property(undecorated):
  name = '_' + undecorated.__name__

  @property
  @wraps(undecorated)
  def decorated(self):
    try:
      return getattr(self, name)
    except AttributeError:
      v = undecorated(self)
      setattr(self, name, v)
      return v

  return decorated

class System(object):
  @lazy_property
  def os(self):
    """
    Return values:
    linux, unknown
    
    In case cannot detect raises 'unknown'
    """
    platform = sys.platform
    if platform.startswith('linux'):
      return "linux"
    else:
      return "unknown"
    
  @lazy_property
  def os_version(self):
    """
    Example return value:
    "6.3" for "Centos 6.3"
    
    In case cannot detect raises 'unknown'
    """
    dist = platform.linux_distribution()
    if dist[1] != '':
      return dist[1]
    else:
      return 'unknown'
    
  @lazy_property
  def os_type(self):
    """
    Return values:
    redhat, fedora, centos, oraclelinux, ascendos,
    amazon, xenserver, oel, ovs, cloudlinux, slc, scientific, psbm,
    ubuntu, debian, sles, sled, opensuse, suse ... and others
    
    In case cannot detect raises exception.
    """
    dist = platform.linux_distribution()
    operatingSystem = dist[0].lower()

    # special cases
    if os.path.exists('/etc/oracle-release'):
      return 'oraclelinux'
    elif operatingSystem.startswith('suse linux enterprise server'):
      return 'sles'
    elif operatingSystem.startswith('red hat enterprise linux server'):
      return 'redhat'
    
    # in general
    if operatingSystem:
      return operatingSystem
    else:
      raise Fail("Cannot detect os type")
    
  @lazy_property
  def os_family(self):
    """
    Return values:
    redhat, debian, suse
    
    In case cannot detect raises exception
    """
    os_type = self.os_type
    if os_type in ['redhat', 'centos', 'fedora', 'oraclelinux', 'ascendos',
                     'amazon', 'xenserver', 'oel', 'ovs', 'cloudlinux',
                     'slc', 'scientific', 'psbm']:
      os_family = 'redhat'
    elif os_type in ['ubuntu', 'debian']:
      os_family = 'debian'
    elif os_type in ['sles', 'sled', 'opensuse', 'suse']:
      os_family = 'suse'
    else:
      raise Fail("Cannot detect os family for os: {0}".format(os_type))
      
    return os_family

  @lazy_property
  def ec2(self):
    if not os.path.exists("/proc/xen"):
      return False
    if os.path.exists("/etc/ec2_version"):
      return True
    return False

  @lazy_property
  def vm(self):
    if os.path.exists("/usr/bin/VBoxControl"):
      return "vbox"
    elif os.path.exists("/usr/bin/vmware-toolbox-cmd") or os.path.exists(
      "/usr/sbin/vmware-toolbox-cmd"):
      return "vmware"
    elif os.path.exists("/proc/xen"):
      return "xen"
    return None
  
  @lazy_property
  def arch(self):
    machine = self.machine
    if machine in ("i386", "i486", "i686"):
      return "x86_32"
    return machine

  @lazy_property
  def machine(self):
    code, out = shell.call(["/bin/uname", "-m"])
    return out.strip()

  @lazy_property
  def locales(self):
    code, out = shell.call("locale -a")
    return out.strip().split("\n")

  @classmethod
  def get_instance(cls):
    try:
      return cls._instance
    except AttributeError:
      cls._instance = cls()
    return cls._instance
  
  def unquote(self, val):
    if val[0] == '"':
      val = val[1:-1]
    return val
