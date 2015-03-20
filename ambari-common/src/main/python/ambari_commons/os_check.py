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

def _get_windows_version():
  """
  Get's the OS major and minor versions.  Returns a tuple of
  (OS_MAJOR, OS_MINOR).
  """
  import ctypes

  class _OSVERSIONINFOEXW(ctypes.Structure):
    _fields_ = [('dwOSVersionInfoSize', ctypes.c_ulong),
                ('dwMajorVersion', ctypes.c_ulong),
                ('dwMinorVersion', ctypes.c_ulong),
                ('dwBuildNumber', ctypes.c_ulong),
                ('dwPlatformId', ctypes.c_ulong),
                ('szCSDVersion', ctypes.c_wchar*128),
                ('wServicePackMajor', ctypes.c_ushort),
                ('wServicePackMinor', ctypes.c_ushort),
                ('wSuiteMask', ctypes.c_ushort),
                ('wProductType', ctypes.c_byte),
                ('wReserved', ctypes.c_byte)]

  os_version = _OSVERSIONINFOEXW()
  os_version.dwOSVersionInfoSize = ctypes.sizeof(os_version)
  retcode = ctypes.windll.Ntdll.RtlGetVersion(ctypes.byref(os_version))
  if retcode != 0:
    raise Exception("Failed to get OS version")

  return os_version.dwMajorVersion, os_version.dwMinorVersion, os_version.dwBuildNumber, os_version.wProductType

# path to resources dir
RESOURCES_DIR = os.path.join(os.path.dirname(os.path.realpath(__file__)), "resources")

# family JSON data
OSFAMILY_JSON_RESOURCE = "os_family.json"
JSON_OS_TYPE = "distro"
JSON_OS_VERSION = "versions"

#windows family constants
SYSTEM_WINDOWS = "Windows"
REL_2008 = "win2008server"
REL_2008R2 = "win2008serverr2"
REL_2012 = "win2012server"
REL_2012R2 = "win2012serverr2"

# windows machine types
VER_NT_WORKSTATION = 1
VER_NT_DOMAIN_CONTROLLER = 2
VER_NT_SERVER = 3

# Linux specific releases, caching them since they are execution invariants
_IS_ORACLE_LINUX = os.path.exists('/etc/oracle-release')
_IS_REDHAT_LINUX = os.path.exists('/etc/redhat-release')

def _is_oracle_linux():
  return _IS_ORACLE_LINUX

def _is_redhat_linux():
  return _IS_REDHAT_LINUX


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
      json_data = eval(f.read())
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
  def os_distribution():
    if platform.system() == SYSTEM_WINDOWS:
      # windows distribution
      major, minor, build, code = _get_windows_version()
      if code in (VER_NT_DOMAIN_CONTROLLER, VER_NT_SERVER):
        # we are on server os
        release = None
        if major == 6:
          if minor == 0:
            release = REL_2008
          elif minor == 1:
            release = REL_2008R2
          elif minor == 2:
            release = REL_2012
          elif minor == 3:
            release = REL_2012R2
        distribution = (release, "{0}.{1}".format(major,minor),"WindowsServer")
      else:
        # we are on unsupported desktop os
        distribution = ("", "","")
    else:
      # linux distribution
      PYTHON_VER = sys.version_info[0] * 10 + sys.version_info[1]

      if PYTHON_VER < 26:
        distribution = platform.dist()
      elif _is_redhat_linux():
        distribution = platform.dist()
      else:
        distribution = platform.linux_distribution()

    if distribution[0] == '' and platform.system().lower() == 'darwin':
      # mac - used for unit tests
      distribution = ("Darwin", "TestOnly", "1.1.1", "1.1.1", "1.1")
    
    return distribution

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
    dist = OSCheck.os_distribution()
    operatingSystem = dist[0].lower()

    # special cases
    if _is_oracle_linux():
      return 'oraclelinux'
    elif operatingSystem.startswith('suse linux enterprise server'):
      return 'sles'
    elif operatingSystem.startswith('red hat enterprise linux'):
      return 'redhat'
    elif operatingSystem.startswith('darwin'):
      return 'mac'

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
    dist = OSCheck.os_distribution()
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
    dist = OSCheck.os_distribution()
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

  @staticmethod
  def is_windows_family():
    """
     Return true if it is so or false if not

     This is safe check for winsrv , doesn't generate exception
    """
    try:
      return OSCheck.get_os_family() == OSConst.WINSRV_FAMILY
    except Exception:
      pass
    return False
