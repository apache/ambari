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

import os.path
import logging
import subprocess
from Facter import Facter

logger = logging.getLogger()

class Hardware:
  SSH_KEY_PATTERN = 'ssh.*key'

  def __init__(self):
    self.hardware = {}
    osdisks = self.osdisks()
    self.hardware['mounts'] = osdisks
    otherInfo = Facter().facterInfo()
    self.hardware.update(otherInfo)
    pass

  @staticmethod
  def extractMountInfo(outputLine):
    if outputLine == None or len(outputLine) == 0:
      return None

      """ this ignores any spaces in the filesystemname and mounts """
    split = outputLine.split()
    if (len(split)) == 7:
      device, type, size, used, available, percent, mountpoint = split
      mountinfo = {
        'size': size,
        'used': used,
        'available': available,
        'percent': percent,
        'mountpoint': mountpoint,
        'type': type,
        'device': device}
      return mountinfo
    else:
      return None

  @staticmethod
  def osdisks():
    """ Run df to find out the disks on the host. Only works on linux 
    platforms. Note that this parser ignores any filesystems with spaces 
    and any mounts with spaces. """
    mounts = []
    df = subprocess.Popen(["df", "-kPT"], stdout=subprocess.PIPE)
    dfdata = df.communicate()[0]
    lines = dfdata.splitlines()
    for l in lines:
      mountinfo = Hardware.extractMountInfo(l)
      if mountinfo != None and os.access(mountinfo['mountpoint'], os.W_OK):
        mounts.append(mountinfo)
      pass
    pass
    return mounts

  def get(self):
    return self.hardware

def main(argv=None):
  hardware = Hardware()
  print hardware.get()

if __name__ == '__main__':
  main()
