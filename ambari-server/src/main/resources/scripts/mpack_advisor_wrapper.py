#!/usr/bin/env ambari-python-wrap

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

import ambari_simplejson as json
import os
import sys
import traceback

"""
This is entry point of Mpack Advisor module, either Ambari Server or cmd will invoke this module
"""

RECOMMEND_COMPONENT_LAYOUT_ACTION = 'recommend-component-layout'
VALIDATE_COMPONENT_LAYOUT_ACTION = 'validate-component-layout'
RECOMMEND_CONFIGURATIONS = 'recommend-configurations'
RECOMMEND_CONFIGURATIONS_FOR_SSO = 'recommend-configurations-for-sso'
RECOMMEND_CONFIGURATION_DEPENDENCIES = 'recommend-configuration-dependencies'
VALIDATE_CONFIGURATIONS = 'validate-configurations'

ALL_ACTIONS = [RECOMMEND_COMPONENT_LAYOUT_ACTION,
               VALIDATE_COMPONENT_LAYOUT_ACTION,
               RECOMMEND_CONFIGURATIONS,
               RECOMMEND_CONFIGURATIONS_FOR_SSO,
               RECOMMEND_CONFIGURATION_DEPENDENCIES,
               VALIDATE_CONFIGURATIONS]
USAGE = "Usage: <action> <hosts_file> <services_file>\nPossible actions are: {0}\n".format( str(ALL_ACTIONS) )

SCRIPT_DIRECTORY = os.path.dirname(os.path.abspath(__file__))
STACKS_DIRECTORY = os.path.join(SCRIPT_DIRECTORY, '../stacks')
MPACK_ADVISOR_PATH = os.path.join(STACKS_DIRECTORY, 'mpack_advisor.py')
AMBARI_CONFIGURATION_PATH = os.path.join(STACKS_DIRECTORY, 'ambari_configuration.py')

ADVISOR_CONTEXT = "advisor_context"
CALL_TYPE = "call_type"

class MpackAdvisorException(Exception):
  pass

def loadJson(path):
  try:
    with open(path, 'r') as f:
      return json.load(f)
  except Exception as err:
    traceback.print_exc()
    raise MpackAdvisorException("ERROR: loading file at: {0}".format(path))

def dumpJson(json_object, dump_file):
  try:
    with open(dump_file, 'w') as out:
      json.dump(json_object, out, indent=1)
  except Exception as err:
    traceback.print_exc()
    raise MpackAdvisorException("ERROR: writing to file {0} : {1}".format(dump_file, str(err)))

def main(argv=None):
  args = argv[1:]

  if len(args) < 3:
    sys.stderr.write(USAGE)
    sys.exit(2)

  action = args[0]
  if action not in ALL_ACTIONS:
    sys.stderr.write(USAGE)
    sys.exit(2)

  hostsFile = args[1]
  servicesFile = args[2]

  # Parse hostsFile and servicesFile
  hosts = loadJson(hostsFile)

  print("INFO: {0} has been loaded".format(hostsFile))
  services = loadJson(servicesFile)
  print("INFO: {0} has been loaded".format(servicesFile))

  # Instantiate Default MpackAdvisor and call action related method
  mpackAdvisor = instantiateMpackAdvisor()
  print("INFO: instantiate MpackAdvisor")

  # Preprocess services in mpackAdvisor
  mpackAdvisor.preprocessHostsAndServices(hosts, services)

  # Perform action
  actionDir = os.path.realpath(os.path.dirname(args[1]))

  if action == RECOMMEND_COMPONENT_LAYOUT_ACTION:
    services[ADVISOR_CONTEXT] = {CALL_TYPE : 'recommendComponentLayout'}
    result = mpackAdvisor.recommendComponentLayout()
    result_file = os.path.join(actionDir, "component-layout.json")
  elif action == VALIDATE_COMPONENT_LAYOUT_ACTION:
    services[ADVISOR_CONTEXT] = {CALL_TYPE : 'validateComponentLayout'}
    result = mpackAdvisor.validateComponentLayout()
    result_file = os.path.join(actionDir, "component-layout-validation.json")
  elif action == RECOMMEND_CONFIGURATIONS:
    services[ADVISOR_CONTEXT] = {CALL_TYPE : 'recommendConfigurations'}
    result = mpackAdvisor.recommendConfigurations()
    result_file = os.path.join(actionDir, "configurations.json")
  elif action == RECOMMEND_CONFIGURATIONS_FOR_SSO:
    services[ADVISOR_CONTEXT] = {CALL_TYPE : 'recommendConfigurationsForSSO'}
    result = mpackAdvisor.recommendConfigurationsForSSO()
    result_file = os.path.join(actionDir, "configurations.json")
  elif action == RECOMMEND_CONFIGURATION_DEPENDENCIES:
    services[ADVISOR_CONTEXT] = {CALL_TYPE : 'recommendConfigurationDependencies'}
    result = mpackAdvisor.recommendConfigurationDependencies(services, hosts)
    result_file = os.path.join(actionDir, "configurations.json")
  else:  # action == VALIDATE_CONFIGURATIONS
    services[ADVISOR_CONTEXT] = {CALL_TYPE: 'validateConfigurations'}
    result = mpackAdvisor.validateConfigurations()
    result_file = os.path.join(actionDir, "configurations-validation.json")

  dumpJson(result, result_file)


def instantiateMpackAdvisor():
  """Instantiates DefaultMpackAdvisor implementation"""
  import imp

  with open(AMBARI_CONFIGURATION_PATH, 'rb') as fp:
    imp.load_module('ambari_configuration', fp, AMBARI_CONFIGURATION_PATH, ('.py', 'rb', imp.PY_SOURCE))

  with open(MPACK_ADVISOR_PATH, 'rb') as fp:
    mpack_advisor = imp.load_module('mpack_advisor', fp, MPACK_ADVISOR_PATH, ('.py', 'rb', imp.PY_SOURCE))

  print "INFO: returning MpackAdvisor implementation"
  return mpack_advisor.MpackAdvisorImpl()


if __name__ == '__main__':
  try:
    main(sys.argv)
  except MpackAdvisorException as mpack_exception:
    traceback.print_exc()
    print "ERROR: occured in mpack advisor.\nError details: {0}".format(str(mpack_exception))
    sys.exit(1)
  except Exception as e:
    traceback.print_exc()
    print "ERROR: occured in mpack advisor.\nError details: {0}".format(str(e))
    sys.exit(2)

