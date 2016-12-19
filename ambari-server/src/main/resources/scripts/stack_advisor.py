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

RECOMMEND_COMPONENT_LAYOUT_ACTION = 'recommend-component-layout'
VALIDATE_COMPONENT_LAYOUT_ACTION = 'validate-component-layout'
RECOMMEND_CONFIGURATIONS = 'recommend-configurations'
RECOMMEND_CONFIGURATION_DEPENDENCIES = 'recommend-configuration-dependencies'
VALIDATE_CONFIGURATIONS = 'validate-configurations'

ALL_ACTIONS = [RECOMMEND_COMPONENT_LAYOUT_ACTION,
               VALIDATE_COMPONENT_LAYOUT_ACTION,
               RECOMMEND_CONFIGURATIONS,
               RECOMMEND_CONFIGURATION_DEPENDENCIES,
               VALIDATE_CONFIGURATIONS]
USAGE = "Usage: <action> <hosts_file> <services_file>\nPossible actions are: {0}\n".format( str(ALL_ACTIONS) )

SCRIPT_DIRECTORY = os.path.dirname(os.path.abspath(__file__))
STACK_ADVISOR_PATH_TEMPLATE = os.path.join(SCRIPT_DIRECTORY, '../stacks/stack_advisor.py')
STACK_ADVISOR_DEFAULT_IMPL_CLASS = 'DefaultStackAdvisor'
STACK_ADVISOR_IMPL_PATH_TEMPLATE = os.path.join(SCRIPT_DIRECTORY, './../stacks/{0}/{1}/services/stack_advisor.py')
STACK_ADVISOR_IMPL_CLASS_TEMPLATE = '{0}{1}StackAdvisor'


class StackAdvisorException(Exception):
  pass

def loadJson(path):
  try:
    with open(path, 'r') as f:
      return json.load(f)
  except Exception as err:
    traceback.print_exc()
    raise StackAdvisorException("Error loading file at: {0}".format(path))


def dumpJson(json_object, dump_file):
  try:
    with open(dump_file, 'w') as out:
      json.dump(json_object, out, indent=1)
  except Exception as err:
    traceback.print_exc()
    raise StackAdvisorException("Error writing to file {0} : {1}".format(dump_file, str(err)))


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
  services = loadJson(servicesFile)

  # Instantiate StackAdvisor and call action related method
  stackName = services["Versions"]["stack_name"]
  stackVersion = services["Versions"]["stack_version"]
  parentVersions = []

  if "stack_hierarchy" in services["Versions"]:
    parentVersions = services["Versions"]["stack_hierarchy"]["stack_versions"]

  stackAdvisor = instantiateStackAdvisor(stackName, stackVersion, parentVersions)

  # Perform action
  actionDir = os.path.realpath(os.path.dirname(args[1]))

  # filter
  hosts = stackAdvisor.filterHostMounts(hosts, services)

  if action == RECOMMEND_COMPONENT_LAYOUT_ACTION:
    result = stackAdvisor.recommendComponentLayout(services, hosts)
    result_file = os.path.join(actionDir, "component-layout.json")
  elif action == VALIDATE_COMPONENT_LAYOUT_ACTION:
    result = stackAdvisor.validateComponentLayout(services, hosts)
    result_file = os.path.join(actionDir, "component-layout-validation.json")
  elif action == RECOMMEND_CONFIGURATIONS:
    result = stackAdvisor.recommendConfigurations(services, hosts)
    result_file = os.path.join(actionDir, "configurations.json")
  elif action == RECOMMEND_CONFIGURATION_DEPENDENCIES:
    result = stackAdvisor.recommendConfigurationDependencies(services, hosts)
    result_file = os.path.join(actionDir, "configurations.json")
  else:  # action == VALIDATE_CONFIGURATIONS
    result = stackAdvisor.validateConfigurations(services, hosts)
    result_file = os.path.join(actionDir, "configurations-validation.json")

  dumpJson(result, result_file)


def instantiateStackAdvisor(stackName, stackVersion, parentVersions):
  """Instantiates StackAdvisor implementation for the specified Stack"""
  import imp

  with open(STACK_ADVISOR_PATH_TEMPLATE, 'rb') as fp:
    default_stack_advisor = imp.load_module('stack_advisor', fp, STACK_ADVISOR_PATH_TEMPLATE, ('.py', 'rb', imp.PY_SOURCE))
  className = STACK_ADVISOR_DEFAULT_IMPL_CLASS
  stack_advisor = default_stack_advisor

  versions = [stackVersion]
  versions.extend(parentVersions)

  for version in reversed(versions):
    try:
      path = STACK_ADVISOR_IMPL_PATH_TEMPLATE.format(stackName, version)

      if os.path.isfile(path):
        with open(path, 'rb') as fp:
          stack_advisor = imp.load_module('stack_advisor_impl', fp, path, ('.py', 'rb', imp.PY_SOURCE))
        className = STACK_ADVISOR_IMPL_CLASS_TEMPLATE.format(stackName, version.replace('.', ''))
        print "StackAdvisor implementation for stack {0}, version {1} was loaded".format(stackName, version)
    except IOError: # file not found
      traceback.print_exc()
      print "StackAdvisor implementation for stack {0}, version {1} was not found".format(stackName, version)

  try:
    clazz = getattr(stack_advisor, className)
    print "Returning " + className + " implementation"
    return clazz()
  except Exception as e:
    traceback.print_exc()
    print "Returning default implementation"
    return default_stack_advisor.DefaultStackAdvisor()


if __name__ == '__main__':
  try:
    main(sys.argv)
  except StackAdvisorException as stack_exception:
    traceback.print_exc()
    print "Error occured in stack advisor.\nError details: {0}".format(str(stack_exception))
    sys.exit(1)
  except Exception as e:
    traceback.print_exc()
    print "Error occured in stack advisor.\nError details: {0}".format(str(e))
    sys.exit(2)

