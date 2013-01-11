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

import logging
import logging.handlers
import ConfigParser
import StringIO

config = ConfigParser.RawConfigParser()
content = """

[server]
hostname=localhost
url_port=8440
secured_url_port=8441

[agent]
prefix=/tmp/ambari-agent

[services]
serviceToPidMapFile=servicesToPidNames.dict
pidLookupPath=/var/run/

[stack]
installprefix=/tmp

[puppet]
puppetmodules=/var/lib/ambari-agent/puppet/
puppet_home=/root/workspace/puppet-install/puppet-2.7.9
facter_home=/root/workspace/puppet-install/facter-1.6.10

[command]
maxretries=2
sleepBetweenRetries=1

[security]
keysdir=/tmp/ambari-agent
server_crt=ca.crt
passphrase_env_var_name=AMBARI_PASSPHRASE
"""
s = StringIO.StringIO(content)
config.readfp(s)

class AmbariConfig:
  def getConfig(self):
    global config
    return config

def setConfig(customConfig):
  global config
  config = customConfig

def main():
  print config

if __name__ == "__main__":
  main()
