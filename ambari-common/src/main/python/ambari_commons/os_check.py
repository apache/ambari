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
import json
import platform

# path to resources dir
RESOURCES_DIR = os.path.join(os.path.dirname(os.path.realpath(__file__)), "resources")

# family JSON data
OSFAMILY_JSON_RESOURCE = "os_family.json"
JSON_OS_TYPE = "distro"
JSON_OS_VERSION = "versions"


def linux_distribution():
  PYTHON_VER = sys.version_info[0] * 10 + sys.version_info[1]

  if PYTHON_VER < 26:
    linux_distribution = platform.dist()
  elif os.path.exists('/etc/redhat-release'):
    linux_distribution = platform.dist()
  else:
    linux_distribution = platform.linux_distribution()

  return linux_distribution


class OS_CONST_TYPE(type):

  # Declare here os type mapping
  OS_FAMILY_COLLECTION = []
  # Would be generated from Family collection definition
  OS_COLLECTION = []
  FAMILY_COLLECTION = []

  def initialize_data(cls):
    """
      Initialize internal data structures from file
    """
    try:
      f = open(os.path.join(RESOURCES_DIR, OSFAMILY_JSON_RESOURCE))
      json_data = json.load(f)
      f.close()
      for family in json_data:
        cls.FAMILY_COLLECTION += [family]
        cls.OS_COLLECTION += json_data[family][JSON_OS_TYPE]
        cls.OS_FAMILY_COLLECTION += [{
          'name': family,
          'os_list': json_data[family][JSON_OS_TYPE]
        }]
    except:
      raise Exception("Couldn't load '%s' file" % OSFAMILY_JSON_RESOURCE)

  def __init__(cls, name, bases, dct):
    cls.initialize_data()

  def __getattr__(cls, name):
    """
      Added support of class.OS_<os_type> properties defined in OS_COLLECTION
      Example:
              OSConst.OS_CENTOS would return centos
              OSConst.OS_OTHEROS would triger an error, coz
               that os is not present in OS_FAMILY_COLLECTION map
    """
    name = name.lower()
    if "os_" in name and name[3:] in cls.OS_COLLECTION:
      return name[3:]
    if "_family" in name and name[:-7] in cls.FAMILY_COLLECTION:
      return name[:-7]
    raise Exception("Unknown class property '%s'" % name)


class OSConst:
  __metaclass__ = OS_CONST_TYPE


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
    elif operatingSystem.startswith('red hat enterprise linux'):
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
    for os_family_item in OSConst.OS_FAMILY_COLLECTION:
      if os_family in os_family_item['os_list']:
        os_family = os_family_item['name']
        break

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

  #  Exception safe family check functions

  @staticmethod
  def is_ubuntu_family():
    """
     Return true if it is so or false if not

     This is safe check for debian family, doesn't generate exception
    """
    try:
      if OSCheck.get_os_family() == OSConst.UBUNTU_FAMILY:
        return True
    except Exception:
      pass
    return False

  @staticmethod
  def is_suse_family():
    """
     Return true if it is so or false if not

     This is safe check for suse family, doesn't generate exception
    """
    try:
      if OSCheck.get_os_family() == OSConst.SUSE_FAMILY:
        return True
    except Exception:
      pass
    return False

  @staticmethod
  def is_redhat_family():
    """
     Return true if it is so or false if not

     This is safe check for redhat family, doesn't generate exception
    """
    try:
      if OSCheck.get_os_family() == OSConst.REDHAT_FAMILY:
        return True
    except Exception:
      pass
    return False

  @staticmethod
  def is_redhat7():
    """
     Return true if it is so or false if not

     This is safe check for redhat7 , doesn't generate exception
    """
    try:
      ostemp=OSCheck.get_os_family()+OSCheck().get_os_major_version()
      if ostemp == 'redhat7':
        return True
    except Exception:
      pass
    return False