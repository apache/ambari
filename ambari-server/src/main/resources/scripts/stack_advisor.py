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
import StringIO

import json
import os
import sys
import traceback

RECOMMEND_COMPONENT_LAYOUT_ACTION = 'recommend-component-layout'
VALIDATE_COMPONENT_LAYOUT_ACTION = 'validate-component-layout'
RECOMMEND_CONFIGURATIONS = 'recommend-configurations'
VALIDATE_CONFIGURATIONS = 'validate-configurations'

ALL_ACTIONS = [ RECOMMEND_COMPONENT_LAYOUT_ACTION, VALIDATE_COMPONENT_LAYOUT_ACTION, RECOMMEND_CONFIGURATIONS, VALIDATE_CONFIGURATIONS ]
USAGE = "Usage: <action> <hosts_file> <services_file>\nPossible actions are: {0}\n".format( str(ALL_ACTIONS) )

SCRIPT_DIRECTORY = os.path.dirname(os.path.abspath(__file__))
STACK_ADVISOR_PATH_TEMPLATE = os.path.join(SCRIPT_DIRECTORY, '../stacks/{0}/stack_advisor.py')
STACK_ADVISOR_IMPL_PATH_TEMPLATE = os.path.join(SCRIPT_DIRECTORY, './../stacks/{0}/{1}/services/stack_advisor.py')
STACK_ADVISOR_IMPL_CLASS_TEMPLATE = '{0}{1}StackAdvisor'


class StackAdvisorException(Exception):
  pass

def loadJson(path):
  try:
    with open(path, 'r') as f:
      return json.load(f)
  except Exception, err:
    raise StackAdvisorException("File not found at: {0}".format(hostsFile))


def dumpJson(json_object, dump_file):
  try:
    with open(dump_file, 'w') as out:
      json.dump(json_object, out, indent=1)
  except Exception, err:
    raise StackAdvisorException("Can not write to file {0} : {1}".format(dump_file, str(err)))


def main(argv=None):
  args = argv[1:]

  if len(args) < 3:
    sys.stderr.write(USAGE)
    sys.exit(2)
    pass

  action = args[0]
  if action not in ALL_ACTIONS:
    sys.stderr.write(USAGE)
    sys.exit(2)
    pass

  hostsFile = args[1]
  servicesFile = args[2]

  # Parse hostsFile and servicesFile
  hosts = loadJson(hostsFile)
  services = loadJson(servicesFile)

  # Instantiate StackAdvisor and call action related method
  stackName = services["Versions"]["stack_name"]
  stackVersion = services["Versions"]["stack_version"]
  parentVersions = []
  if "parent_stack_version" in services["Versions"] and \
      services["Versions"]["parent_stack_version"] is not None:
    parentVersions = [ services["Versions"]["parent_stack_version"] ]

  stackAdvisor = instantiateStackAdvisor(stackName, stackVersion, parentVersions)

  # Perform action
  actionDir = os.path.realpath(os.path.dirname(args[1]))
  result = {}
  result_file = "non_valid_result_file.json"

  if action == RECOMMEND_COMPONENT_LAYOUT_ACTION:
    result = stackAdvisor.recommendComponentLayout(services, hosts)
    result_file = os.path.join(actionDir, "component-layout.json")
  elif action == VALIDATE_COMPONENT_LAYOUT_ACTION:
    result = stackAdvisor.validateComponentLayout(services, hosts)
    result_file = os.path.join(actionDir, "component-layout-validation.json")
  elif action == RECOMMEND_CONFIGURATIONS:
    result = stackAdvisor.recommendConfigurations(services, hosts)
    result_file = os.path.join(actionDir, "configurations.json")
  else: # action == VALIDATE_CONFIGURATIONS
    result = stackAdvisor.validateConfigurations(services, hosts)
    result_file = os.path.join(actionDir, "configurations-validation.json")

  dumpJson(result, result_file)
  pass


def instantiateStackAdvisor(stackName, stackVersion, parentVersions):
  """Instantiates StackAdvisor implementation for the specified Stack"""
  import imp

  stackAdvisorPath = STACK_ADVISOR_PATH_TEMPLATE.format(stackName)
  with open(stackAdvisorPath, 'rb') as fp:
    stack_advisor = imp.load_module( 'stack_advisor', fp, stackAdvisorPath, ('.py', 'rb', imp.PY_SOURCE) )

  versions = [stackVersion]
  versions.extend(parentVersions)

  for version in reversed(versions):
    try:
      path = STACK_ADVISOR_IMPL_PATH_TEMPLATE.format(stackName, version)
      className = STACK_ADVISOR_IMPL_CLASS_TEMPLATE.format(stackName, version.replace('.', ''))

      with open(path, 'rb') as fp:
        stack_advisor_impl = imp.load_module('stack_advisor_impl', fp, path, ('.py', 'rb', imp.PY_SOURCE))
      print "StackAdvisor implementation for stack {0}, version {1} was loaded".format(stackName, version)
    except Exception, e:
      print "StackAdvisor implementation for stack {0}, version {1} was not found".format(stackName, version)

  try:
    clazz = getattr(stack_advisor_impl, className)
    return clazz()
  except Exception, e:
    print "Returning default implementation"
    return stack_advisor.StackAdvisor()


if __name__ == '__main__':
  try:
    main(sys.argv)
  except Exception, e:
    traceback.print_exc()
    print "Error occured in stack advisor.\nError details: {0}".format(str(e))
    sys.exit(1)

