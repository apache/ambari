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
import AmbariConfig

class Hardware:
  def __init__(self):
    facterHome = AmbariConfig.config.get('puppet', 'facter_home')
    
    self.hardware = { 'coreCount' : 4,
                      'cpuSpeed' : 4,
                      'cpuFlag' : 4,
                      'diskCount' : 3,
                      'netSpeed' : 3,
                      'ramSize' : 2
                    }

  def get(self):
    return self.hardware


def main(argv=None):
  hardware = Hardware()
  print hardware.get()

if __name__ == '__main__':
  main()
