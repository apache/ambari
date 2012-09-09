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

import multiprocessing
import platform
import AmbariConfig
import os.path
import shell
import logging
import subprocess

logger = logging.getLogger()

class Hardware:
  def __init__(self):
    self.hardware = {}
    osdisks = self.osdisks()
    self.hardware['mounts'] = osdisks
    otherInfo = self.facterInfo()
    self.hardware.update(otherInfo)
    pass
  
  def osdisks(self):
    """ Run df to find out the disks on the host. Only works on linux 
    platforms. Note that this parser ignores any filesystems with spaces 
    and any mounts with spaces. """
    mounts = {}
    df = subprocess.Popen(["df", "-kP"], stdout=subprocess.PIPE)
    dfdata = df.communicate()[0]
    lines = dfdata.splitlines()
    for l in lines:
      split = l.split()
      """ this ignores any spaces in the filesystemname and mounts """
      if (len(split)) == 6:
        device, size, used, available, percent, mountpoint = split
        mountinfo = { 'size' : size,
             'used' : used,
             'available' : available,
             'percent' : percent,
             'mountpoint' : mountpoint}

        mounts[device ] = mountinfo
        pass
      pass
    return mounts
    
  def facterBin(self, facterHome):
    return facterHome + "/bin/facter"
    pass
  
  def facterLib(self, facterHome):
    return facterHome + "/lib/"
    pass
  
  def parseFacterOutput(self, facterOutput):
    retDict = {}
    allLines = facterOutput.splitlines()
    for line in allLines:
      keyValue = line.split("=>")
      if (len(keyValue) == 2):
        """Ignoring values that are just spaces or do not confirm to the 
        format"""
        retDict[keyValue[0].strip()] = keyValue[1].strip()
        pass
      pass
    return retDict
  
  def facterInfo(self):   
    facterHome = AmbariConfig.config.get("puppet", "facter_home")
    facterEnv = os.environ
    logger.info("Using facter home as: " + facterHome)
    facterInfo = {}
    if os.path.exists(facterHome):
      rubyLib = ""
      if os.environ.has_key("RUBYLIB"):
        rubyLib = os.environ["RUBYLIB"]
        logger.info("Ruby Lib env from Env " + rubyLib)
      rubyLib = rubyLib + ":" + self.facterLib(facterHome)
      facterEnv["RUBYLIB"] = rubyLib
      logger.info("Setting RUBYLIB as: " + rubyLib)
      facter = subprocess.Popen([self.facterBin(facterHome)],
                                 stdout=subprocess.PIPE,
                                 stderr=subprocess.PIPE,
                                 env=facterEnv)
      stderr_out = facter.communicate()
      if facter.returncode != 0:
        logging.error("Error getting facter info: " + stderr_out[1])
        pass
      facterOutput = stderr_out[0]
      infoDict = self.parseFacterOutput(facterOutput)
      facterInfo = infoDict
    else:
      pass
    return facterInfo
  
  def get(self):
    logger.info("Hardware Info for the agent: " + str(self.hardware))
    return self.hardware

def main(argv=None):
  hardware = Hardware()
  print hardware.get()

if __name__ == '__main__':
  main()
