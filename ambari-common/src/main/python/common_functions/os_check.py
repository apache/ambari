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
import sys
import platform


def linux_distribution():
  PYTHON_VER = sys.version_info[0] * 10 + sys.version_info[1]

  if PYTHON_VER < 26:
    linux_distribution = platform.dist()
  else:
    linux_distribution = platform.linux_distribution()

  return linux_distribution

class OSCheck:

  @staticmethod
  def get_os_type():
    """
    Return values:
    redhat, fedora, centos, oraclelinux, ascendos,
    amazon, xenserver, oel, ovs, cloudlinux, slc, scientific, psbm,
    ubuntu, debian, sles, sled, opensuse, suse ... and others

    In case cannot detect - exit.
    """
    # Read content from /etc/*-release file
    # Full release name
    dist = linux_distribution()
    operatingSystem = dist[0].lower()

    # special cases
    if os.path.exists('/etc/oracle-release'):
      return 'oraclelinux'
    elif operatingSystem.startswith('suse linux enterprise server'):
      return 'sles'
    elif operatingSystem.startswith('red hat enterprise linux server'):
      return 'redhat'

    if operatingSystem != '':
      return operatingSystem
    else:
      raise Exception("Cannot detect os type. Exiting...")

  @staticmethod
  def get_os_family():
    """
    Return values:
    redhat, debian, suse ... and others

    In case cannot detect raises exception( from self.get_operating_system_type() ).
    """
    os_family = OSCheck.get_os_type()
    if os_family in ['redhat', 'fedora', 'centos', 'oraclelinux', 'ascendos',
                     'amazon', 'xenserver', 'oel', 'ovs', 'cloudlinux',
                     'slc', 'scientific', 'psbm', 'centos linux']:
      os_family = 'RedHat'
    elif os_family in ['ubuntu', 'debian']:
      os_family = 'Debian'
    elif os_family in ['sles', 'sled', 'opensuse', 'suse']:
      os_family = 'Suse'
    #else:  os_family = OSCheck.get_os_type()
    return os_family.lower()

  @staticmethod
  def get_os_version():
    """
    Returns the OS version

    In case cannot detect raises exception.
    """
    # Read content from /etc/*-release file
    # Full release name
    dist = linux_distribution()
    dist = dist[1]

    if dist:
      return dist
    else:
      raise Exception("Cannot detect os version. Exiting...")

  @staticmethod
  def get_os_major_version():
    """
    Returns the main OS version like
    Centos 6.5 --> 6
    RedHat 1.2.3 --> 1
    """
    return OSCheck.get_os_version().split('.')[0]

  @staticmethod
  def get_os_release_name():
    """
    Returns the OS release name

    In case cannot detect raises exception.
    """
    dist = linux_distribution()
    dist = dist[2].lower()

    if dist:
      return dist
    else:
      raise Exception("Cannot detect os release name. Exiting...")



