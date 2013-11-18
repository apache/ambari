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

import os.path
import logging
import subprocess
import pprint
import traceback
import re

import AmbariConfig


logger = logging.getLogger()

class Hardware:
  SSH_KEY_PATTERN = 'ssh.*key'

  def __init__(self, config):
    self.config = config
    self.hardware = {}
    osdisks = self.osdisks()
    self.hardware['mounts'] = osdisks
    otherInfo = self.facterInfo()
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
        'size' : size,
        'used' : used,
        'available' : available,
        'percent' : percent,
        'mountpoint' : mountpoint,
        'type': type,
        'device' : device }
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

  def facterBin(self, facterHome):
    facterBin = facterHome + "/bin/facter"
    if (os.path.exists(facterBin)):
      return facterBin
    else:
      return "facter"
    pass
  
  def facterLib(self, facterHome):
    return facterHome + "/lib/"
    pass
  
  def configureEnviron(self, environ):
    if not self.config.has_option("puppet", "ruby_home"):
      return environ
    ruby_home = self.config.get("puppet", "ruby_home")
    if os.path.exists(ruby_home):
      """Only update ruby home if the config is configured"""
      path = os.environ["PATH"]
      if not ruby_home in path:
        environ["PATH"] = ruby_home + os.path.sep + "bin"  + ":"+environ["PATH"] 
      environ["MY_RUBY_HOME"] = ruby_home
    return environ
    
  def parseFacterOutput(self, facterOutput):
    retDict = {}
    compiled_pattern = re.compile(self.SSH_KEY_PATTERN)
    allLines = facterOutput.splitlines()
    for line in allLines:
      keyValue = line.split("=>")
      if (len(keyValue) == 2):
        """Ignoring values that are just spaces or do not confirm to the 
        format"""
        strippedKey = keyValue[0].strip()
        logger.info("Stripped key is " + strippedKey)
        if strippedKey in ["memoryfree", "memorysize", "memorytotal"]:
          value = keyValue[1].strip()
          """Convert to KB"""
          parts = value.split()
          if len(parts) == 2:
            mem_size = parts[1].upper()
            if mem_size in ["GB", "G"]:
              mem_in_kb = long(float(parts[0]) * 1024 * 1024)
            elif mem_size in ["MB", "M"]:
              mem_in_kb = long(float(parts[0]) * 1024)
            elif mem_size in ["KB", "K"]:
              mem_in_kb = long(float(parts[0]))
            else:
              mem_in_kb = long(float(parts[0]) / 1024)
          else:
            mem_in_kb = long(float(parts[0]) / 1024)
          retDict[strippedKey] = mem_in_kb
          pass
        else:
          if not compiled_pattern.match(strippedKey):
            retDict[strippedKey] = keyValue[1].strip()
          pass
        pass
      pass
    """ Convert the needed types to the true values """
    if 'physicalprocessorcount' in retDict.keys():
      retDict['physicalprocessorcount'] = int(retDict['physicalprocessorcount'])
      pass
    if 'is_virtual' in retDict.keys():
      retDict['is_virtual'] = ("true" == retDict['is_virtual'])
      pass
    
    logger.info("Facter info : \n" + pprint.pformat(retDict))
    return retDict  
  
  def facterInfo(self):
    facterHome = self.config.get("puppet", "facter_home")
    facterEnv = os.environ
    logger.info("Using facter home as: " + facterHome)
    facterInfo = {}
    try:
      if os.path.exists(facterHome):
        rubyLib = ""
        if os.environ.has_key("RUBYLIB"):
          rubyLib = os.environ["RUBYLIB"]
          logger.info("RUBYLIB from Env " + rubyLib)
        if not (self.facterLib(facterHome) in rubyLib):
          rubyLib = rubyLib + ":" + self.facterLib(facterHome)
        
        facterEnv["RUBYLIB"] = rubyLib
        facterEnv = self.configureEnviron(facterEnv)
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
        pass
      else:
        logger.error("Facter home at " + facterHome + " does not exist")
    except:
      logger.info("Traceback " + traceback.format_exc())
      pass
    return facterInfo
  
  def get(self):
    return self.hardware

def main(argv=None):
  hardware = Hardware(AmbariConfig.config)
  print hardware.get()

if __name__ == '__main__':
  main()
