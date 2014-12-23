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

from datetime import datetime, timedelta
from resource_management import Execute
from tempfile import mkstemp
import os
import json


def validate_security_config_properties(params, configuration_rules):
  """
  Generic security configuration validation based on a set of rules and operations
  :param params: The structure where the config parameters are held
  :param configuration_rules: A structure containing rules and expectations,
  Three types of checks are currently supported by this method:
  1. value_checks - checks that a certain value must be set
  2. empty_checks - checks that the property values must not be empty
  3. read_checks - checks that the value represented by the property describes a readable file on the filesystem
  :return: Issues found - should be empty if all is good
  """

  issues = {}

  for config_file, rule_sets in configuration_rules.iteritems():
    # Each configuration rule set may have 0 or more of the following rule sets:
    # - value_checks
    # - empty_checks
    # - read_checks
    try:
      # Each rule set has at least a list of relevant property names to check in some way
      # The rule set for the operation of 'value_checks' is expected to be a dictionary of
      # property names to expected values

      actual_values = params[config_file] if config_file in params else {}

      # Process Value Checks
      # The rules are expected to be a dictionary of property names to expected values
      rules = rule_sets['value_checks'] if 'value_checks' in rule_sets else None
      if rules:
        for property_name, expected_value in rules.iteritems():
          actual_value = actual_values[property_name] if property_name in actual_values else ''
          if actual_value != expected_value:
            issues[config_file] = "Property " + property_name + ". Expected/Actual: " + \
                                  expected_value + "/" + actual_value

      # Process Empty Checks
      # The rules are expected to be a list of property names that should not have empty values
      rules = rule_sets['empty_checks'] if 'empty_checks' in rule_sets else None
      if rules:
        for property_name in rules:
          actual_value = actual_values[property_name] if property_name in actual_values else ''
          if not actual_value:
            issues[config_file] = "Property " + property_name + " must exist and must not be empty!"

      # Process Read Checks
      # The rules are expected to be a list of property names that resolve to files names and must
      # exist and be readable
      rules = rule_sets['read_checks'] if 'read_checks' in rule_sets else None
      if rules:
        for property_name in rules:
          actual_value = actual_values[property_name] if property_name in actual_values else None
          if not actual_value or not os.path.isfile(actual_value):
            issues[
              config_file] = "Property " + property_name + " points to an inaccessible file or parameter does not exist!"
    except Exception as e:
      issues[config_file] = "Exception occurred while validating the config file\nCauses: " + str(e)
  return issues


def build_expectations(config_file, value_checks, empty_checks, read_checks):
  """
  Helper method used to build the check expectations dict
  :return:
  """
  configs_expectations = {}
  configs_expectations[config_file] = {}
  if value_checks:
    configs_expectations[config_file]['value_checks'] = value_checks
  if empty_checks:
    configs_expectations[config_file]['empty_checks'] = empty_checks
  if read_checks:
    configs_expectations[config_file]['read_checks'] = read_checks
  return configs_expectations


def get_params_from_filesystem(conf_dir, config_files):
  """
  Used to retrieve properties from xml config files and build a dict
  :param conf_dir:  directory where the configuration files sit
  :param config_files: list of configuration file names
  :return:
  """
  result = {}
  from xml.etree import ElementTree as ET

  for config_file in config_files:
    configuration = ET.parse(conf_dir + os.sep + config_file)
    props = configuration.getroot().getchildren()
    config_file_id = config_file[:-4] if len(config_file) > 4 else config_file
    result[config_file_id] = {}
    for prop in props:
      result[config_file_id].update({prop[0].text: prop[1].text})
  return result


def cached_kinit_executor(kinit_path, exec_user, keytab_file, principal, hostname, temp_dir,
                          expiration_time):
  """
  Main cached kinit executor - Uses a temporary file on the FS to cache executions. Each command
  will have its own file and only one entry (last successful execution) will be stored
  :return:
  """
  key = str(hash("%s|%s" % (principal, keytab_file)))
  filename = key + "_tmp.txt"
  file_path = temp_dir + os.sep + "kinit_executor_cache"
  output = None

  # First execution scenario dir file existence check
  if not os.path.exists(file_path):
    os.makedirs(file_path)

  file_path += os.sep + filename

  # If the file does not exist create before read
  if not os.path.isfile(file_path):
    with open(file_path, 'w+') as new_file:
      new_file.write("{}")
  try:
    with open(file_path, 'r') as cache_file:
      output = json.load(cache_file)
  except:
    # In the extraordinary case the temporary file gets corrupted the cache should be reset to avoid error loop
    with open(file_path, 'w+') as cache_file:
      cache_file.write("{}")

  if (not output) or (key not in output) or ("last_successful_execution" not in output[key]):
    return new_cached_exec(key, file_path, kinit_path, exec_user, keytab_file, principal, hostname)
  else:
    last_run_time = output[key]["last_successful_execution"]
    now = datetime.now()
    if (now - datetime.strptime(last_run_time, "%Y-%m-%d %H:%M:%S.%f") > timedelta(
      minutes=expiration_time)):
      return new_cached_exec(key, file_path, kinit_path, exec_user, keytab_file, principal, hostname)
    else:
      return True


def new_cached_exec(key, file_path, kinit_path, exec_user, keytab_file, principal, hostname):
  """
  Entry point of an actual execution - triggered when timeout on the cache expired or on fresh execution
  """
  now = datetime.now()
  _, temp_kinit_cache_file = mkstemp()
  command = "su -s /bin/bash - %s -c '%s -c %s -kt %s %s'" % \
            (exec_user, kinit_path, temp_kinit_cache_file, keytab_file,
             principal.replace("_HOST", hostname))

  try:
    Execute(command)

    with open(file_path, 'w+') as cache_file:
      result = {key: {"last_successful_execution": str(now)}}
      json.dump(result, cache_file)
  finally:
    os.remove(temp_kinit_cache_file)

  return True
