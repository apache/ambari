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
import sys
import platform


class OSCheck(object):
  def __init__(self):
    pass

  def get_os_type(self):
    """
    Return values:
    redhat, fedora, centos, oraclelinux, ascendos,
    amazon, xenserver, oel, ovs, cloudlinux, slc, scientific, psbm,
    ubuntu, debian, sles, sled, opensuse, suse ... and others

    In case cannot detect - exit.
    """
    # Read content from /etc/*-release file
    # Full release name
    dist = platform.linux_distribution()
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
      print "Cannot detect os type. Exiting..."
      sys.exit(1)


  def get_os_family(self):
    """
    Return values:
    redhat, debian, suse ... and others

    In case cannot detect raises exception( from self.get_operating_system_type() ).
    """
    os_family = self.get_os_type()
    if os_family in ['redhat', 'fedora', 'centos', 'oraclelinux', 'ascendos',
                     'amazon', 'xenserver', 'oel', 'ovs', 'cloudlinux',
                     'slc', 'scientific', 'psbm', 'centos linux']:
      os_family = 'RedHat'
    elif os_family in ['ubuntu', 'debian']:
      os_family = 'Debian'
    elif os_family in ['sles', 'sled', 'opensuse', 'suse']:
      os_family = 'Suse'
    #else:  os_family = self.get_os_type()
    return os_family.lower()


  def get_os_version(self):
    """
    Returns the OS version

    In case cannot detect raises exception.
    """
    # Read content from /etc/*-release file
    # Full release name
    dist = platform.linux_distribution()
    dist = dist[1]

    if dist:
      return dist
    else:
      print "Cannot detect os version. Exiting..."
      sys.exit(1)

  def get_os_major_version(self):
    """
    Returns the main OS version like
    Centos 6.5 --> 6
    RedHat 1.2.3 --> 1
    """
    return self.get_os_version().split('.')[0]

  def get_os_release_name(self):
    """
    Returns the OS release name

    In case cannot detect raises exception.
    """
    dist = platform.linux_distribution()
    dist = dist[2].lower()

    if dist:
      return dist
    else:
      print "Cannot detect os release name. Exiting..."
      sys.exit(1)


def main(argv=None):
  # Same logic that was in "os_type_check.sh"
  if len(sys.argv) != 2:
    print "Usage: <cluster_os>"
    sys.exit(2)
    pass

  cluster_os = sys.argv[1]
  current_os = OSCheck().get_os_family() + OSCheck().get_os_major_version()

  # If agent/server have the same {"family","main_version"} - then ok.
  print "Cluster primary/cluster OS type is %s and local/current OS type is %s" % (
    cluster_os, current_os)
  if current_os == cluster_os:
    sys.exit(0)
  else:
    print "Local OS is not compatible with cluster primary OS. Please perform manual bootstrap on this host."
    sys.exit(1)


if __name__ == "__main__":
  main()
