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

from shell import shellRunner
import multiprocessing
import platform

class Hardware:
  def __init__(self):
    self.scanOS()
    self.scanDisk()
    self.scanRam()
    self.scanCpu()
    self.scanNet()
    self.hardware = { 'coreCount' : self.cpuCount,
                      'cpuSpeed' : self.cpuSpeed,
                      'cpuFlag' : self.cpuFlag,
                      'diskCount' : self.diskCount,
                      'netSpeed' : self.netSpeed,
                      'ramSize' : self.ramSize
                    }

  def get(self):
    return self.hardware

  def scanDisk(self):
    self.diskCount = 0

  def scanRam(self):
    self.ramSize = 0

  def scanCpu(self):
    self.cpuCount = multiprocessing.cpu_count()
    self.cpuSpeed = 0
    self.cpuFlag = ""

  def scanNet(self):
    switches = {
                'Linux': self.ethtool,
                'Darwin': self.ifconfig
               }
    switches.get(self.os, self.ethtool)()

  def ethtool(self):
    sh = shellRunner()
    script = [ 'ethtool', 'eth0', '|', 'grep', 'Speed:', '|', 'sed', "'s/\s*Speed:\s*//'", '|', 'sed', "'s/Mb\/s//'" ]
    result = sh.run(script)
    if "ethtool: not found\n" in result['error']:
      # ethtool not installed, assume a speed of 0Mb/s
      self.netSpeed = 0
    else:
      self.netSpeed = int(result['output'].rstrip())

  def ifconfig(self):
    sh = shellRunner()
    script = [ 'ifconfig', 'en0', '|', 'grep', 'media:', '|', 'sed', "'s/.*(//'", '|', 'sed', "'s/ .*//'", '|', 'sed', "'s/baseT//'" ]
    result = sh.run(script)
    if "none" in result['output']:
      # No ethernet detected, detect airport
      script = [ '/System/Library/PrivateFrameworks/Apple80211.framework/Versions/A/Resources/airport', '-I', '|', 'grep', 'lastTxRate:', '|', 'sed', "'s/.*: //'", '|', 'sed', "'s/$//'"]
      result = sh.run(script)
    try:
      self.netSpeed = int(result['output'].rstrip())
    except Exception:
      self.netSpeed = 0

  def scanOS(self):
    self.arch = platform.processor()
    self.os = platform.system()

def main(argv=None):
  hardware = Hardware()
  print hardware.get()

if __name__ == '__main__':
  main()
