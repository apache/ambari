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

"""
from resource_management.libraries.script import Script
from resource_management.core.logger import Logger
from resource_management.core.source import DownloadSource
from resource_management.core.resources import File, Package

rpm_location = 'http://104.196.87.250/msft-r/8.1/'
rpms = ['microsoft-r-server-mro-8.0.rpm',
        'microsoft-r-server-intel-mkl-8.0.rpm',
        'microsoft-r-server-packages-8.0.rpm',
        'microsoft-r-server-hadoop-8.0.rpm']

class MicrosoftR(Script):
  def install(self, env):
    Logger.info('Installing R Server Client...')
    tmp_dir = Script.tmp_dir
    Logger.debug('Using temp dir: {0}'.format(tmp_dir))
    Logger.info("Will download and install the following rpm's from {0}: {1}".format(rpm_location, rpms))

    for rpm in rpms:
      Logger.info('Downloading {0}'.format(rpm))
      rpmFile = '{0}/{1}'.format(tmp_dir, rpm)
      File(rpmFile, \
           content = DownloadSource(rpm_location + rpm), \
           mode = 0644)
      Logger.info('Installing {0}'.format(rpm))
      Package(rpmFile)

    Logger.info('Installed R Server')

  def configure(self, env):
    Logger.info('Configure R Server. Nothing to do.')

if __name__ == "__main__":
  MicrosoftR().execute()
