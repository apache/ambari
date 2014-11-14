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

import platform

import ConfigParser
import StringIO
import json
from NetUtil import NetUtil

SETUP_ACTION = "setup"
START_ACTION = "start"
STOP_ACTION = "stop"
RESET_ACTION = "reset"
STATUS_ACTION = "status"
DEBUG_ACTION = "debug"

IS_WINDOWS = platform.system() == "Windows"

if not IS_WINDOWS:
  from AgentConfig_linux import *
else:
  from AgentConfig_windows import *

config = ConfigParser.RawConfigParser()

s = StringIO.StringIO(content)
config.readfp(s)

class AmbariConfig:
  TWO_WAY_SSL_PROPERTY = "security.server.two_way_ssl"
  CONFIG_FILE = "/etc/ambari-agent/conf/ambari-agent.ini"
  SERVER_CONNECTION_INFO = "{0}/connection_info"
  CONNECTION_PROTOCOL = "https"

  config = None
  net = None

  def __init__(self):
    global content
    self.config = ConfigParser.RawConfigParser()
    self.net = NetUtil()
    self.config.readfp(StringIO.StringIO(content))

  def get(self, section, value, default=None):
    try:
      return self.config.get(section, value)
    except ConfigParser.Error, err:
      if default:
        return default
      raise err

  def set(self, section, option, value):
    self.config.set(section, option, value)

  def add_section(self, section):
    self.config.add_section(section)

  @staticmethod
  def getConfigFile():
    global configFile
    return configFile

  @staticmethod
  def getLogFile():
    global logfile
    return logfile

  @staticmethod
  def getOutFile():
    global outfile
    return outfile

  def setConfig(self, customConfig):
    self.config = customConfig

  def getConfig(self):
    return self.config

  def getImports(self):
    global imports
    return imports

  def getRolesToClass(self):
    global rolesToClass
    return rolesToClass

  def getServiceStates(self):
    global serviceStates
    return serviceStates

  def getServicesToPidNames(self):
    global servicesToPidNames
    return servicesToPidNames

  def pidPathVars(self):
    global pidPathVars
    return pidPathVars

  def has_option(self, section, option):
    return self.config.has_option(section, option)

  def remove_option(self, section, option):
    return self.config.remove_option(section, option)

  def load(self, data):
    self.config = ConfigParser.RawConfigParser(data)

  def read(self, filename):
    self.config.read(filename)

  def getServerOption(self, url, name, default=None):
    status, response = self.net.checkURL(url)
    if status is True:
      try:
        data = json.loads(response)
        if name in data:
          return data[name]
      except:
        pass
    return default

  def get_api_url(self):
    return "%s://%s:%s" % (self.CONNECTION_PROTOCOL,
                           self.get('server', 'hostname'),
                           self.get('server', 'url_port'))

  def isTwoWaySSLConnection(self):
    req_url = self.get_api_url()
    response = self.getServerOption(self.SERVER_CONNECTION_INFO.format(req_url), self.TWO_WAY_SSL_PROPERTY, 'false')
    if response is None:
      return False
    elif response.lower() == "true":
      return True
    else:
      return False


def main():
  print AmbariConfig().config

if __name__ == "__main__":
  main()
