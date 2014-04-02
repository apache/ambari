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

__all__ = ["Script"]

import os
import sys
import json
import logging

from resource_management.core.environment import Environment
from resource_management.core.exceptions import Fail, ClientComponentHasNoStatus, ComponentIsNotRunning
from resource_management.core.resources.packaging import Package
from resource_management.libraries.script.config_dictionary import ConfigDictionary
from resource_management.libraries.script.repo_installer import RepoInstaller

USAGE = """Usage: {0} <COMMAND> <JSON_CONFIG> <BASEDIR> <STROUTPUT>

<COMMAND> command type (INSTALL/CONFIGURE/START/STOP/SERVICE_CHECK...)
<JSON_CONFIG> path to command json file. Ex: /var/lib/ambari-agent/data/command-2.json
<BASEDIR> path to service metadata dir. Ex: /var/lib/ambari-agent/cache/stacks/HDP/2.0.6/services/HDFS
<STROUTPUT> path to file with structured command output (file will be created). Ex:/tmp/my.txt
"""

class Script(object):
  """
  Executes a command for custom service. stdout and stderr are written to
  tmpoutfile and to tmperrfile respectively.
  Script instances share configuration as a class parameter and therefore
  different Script instances can not be used from different threads at
  the same time within a single python process

  Accepted command line arguments mapping:
  1 command type (START/STOP/...)
  2 path to command json file
  3 path to service metadata dir (Directory "package" inside service directory)
  4 path to file with structured command output (file will be created)
  """
  structuredOut = {}

  def put_structured_out(self, sout):
    Script.structuredOut.update(sout)
    try:
      structuredOut = json.dumps(Script.structuredOut)
      with open(self.stroutfile, 'w') as fp:
        json.dump(structuredOut, fp)
    except IOError:
      Script.structuredOut.update({"errMsg" : "Unable to write to " + self.stroutfile})

  def execute(self):
    """
    Sets up logging;
    Parses command parameters and executes method relevant to command type
    """
    # set up logging (two separate loggers for stderr and stdout with different loglevels)
    logger = logging.getLogger('resource_management')
    logger.setLevel(logging.DEBUG)
    formatter = logging.Formatter('%(asctime)s - %(message)s')
    chout = logging.StreamHandler(sys.stdout)
    chout.setLevel(logging.INFO)
    chout.setFormatter(formatter)
    cherr = logging.StreamHandler(sys.stderr)
    cherr.setLevel(logging.ERROR)
    cherr.setFormatter(formatter)
    logger.addHandler(cherr)
    logger.addHandler(chout)
    # parse arguments
    if len(sys.argv) < 5: 
     logger.error("Script expects at least 4 arguments")
     print USAGE.format(os.path.basename(sys.argv[0])) # print to stdout
     sys.exit(1)
      
    command_name = str.lower(sys.argv[1])
    # parse command parameters
    command_data_file = sys.argv[2]
    basedir = sys.argv[3]
    self.stroutfile = sys.argv[4]
    try:
      with open(command_data_file, "r") as f:
        pass
        Script.config = ConfigDictionary(json.load(f))
    except IOError:
      logger.exception("Can not read json file with command parameters: ")
      sys.exit(1)
    # Run class method depending on a command type
    try:
      method = self.choose_method_to_execute(command_name)
      with Environment(basedir) as env:
        method(env)
    except ClientComponentHasNoStatus or ComponentIsNotRunning:
      # Support of component status checks.
      # Non-zero exit code is interpreted as an INSTALLED status of a component
      sys.exit(1)
    except Fail:
      logger.exception("Error while executing command '{0}':".format(command_name))
      sys.exit(1)


  def choose_method_to_execute(self, command_name):
    """
    Returns a callable object that should be executed for a given command.
    """
    self_methods = dir(self)
    if not command_name in self_methods:
      raise Fail("Script '{0}' has no method '{1}'".format(sys.argv[0], command_name))
    method = getattr(self, command_name)
    return method


  @staticmethod
  def get_config():
    """
    HACK. Uses static field to store configuration. This is a workaround for
    "circular dependency" issue when importing params.py file and passing to
     it a configuration instance.
    """
    return Script.config


  def install(self, env):
    """
    Default implementation of install command is to install all packages
    from a list, received from the server.
    Feel free to override install() method with your implementation. It
    usually makes sense to call install_packages() manually in this case
    """
    self.install_packages(env)


  def install_packages(self, env):
    """
    List of packages that are required by service is received from the server
    as a command parameter. The method installs all packages
    from this list
    """
    config = self.get_config()
    RepoInstaller.install_repos(config)
    
    try:
      package_list_str = config['hostLevelParams']['package_list']
      if isinstance(package_list_str,basestring) and len(package_list_str) > 0:
        package_list = json.loads(package_list_str)
        for package in package_list:
          name = package['name']
          Package(name)
    except KeyError:
      pass # No reason to worry
    
    #RepoInstaller.remove_repos(config)



  def fail_with_error(self, message):
    """
    Prints error message and exits with non-zero exit code
    """
    print("Error: " + message)
    sys.stderr.write("Error: " + message)
    sys.exit(1)

  def start(self, env):
    """
    To be overridden by subclasses
    """
    self.fail_with_error('start method isn\'t implemented')

  def stop(self, env):
    """
    To be overridden by subclasses
    """
    self.fail_with_error('stop method isn\'t implemented')

  def restart(self, env):
    """
    Default implementation of restart command is to call stop and start methods
    Feel free to override restart() method with your implementation.
    For client components we call install
    """
    config = self.get_config()
    componentCategory = None
    try :
      componentCategory = config['roleParams']['component_category']
    except KeyError:
      pass

    if componentCategory and componentCategory.strip().lower() == 'CLIENT'.lower():
      self.install(env)
    else:
      self.stop(env)
      self.start(env)

  def configure(self, env):
    """
    To be overridden by subclasses
    """
    self.fail_with_error('configure method isn\'t implemented')
