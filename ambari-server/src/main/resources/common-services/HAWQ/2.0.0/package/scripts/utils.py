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
import subprocess

from resource_management.core.resources.system import Execute, Directory
from resource_management.core.exceptions import Fail
from resource_management.core.logger import Logger

import hawq_constants

def generate_hawq_process_status_cmd(component_name, port):
  """
  Generate a string of command to check if hawq postgres / gpsyncmaster process is running
  """
  process_name = hawq_constants.COMPONENT_ATTRIBUTES_MAP[component_name]['process_name']
  return "netstat -tupln | egrep ':{0}\s' | egrep {1}".format(port, process_name)


def create_dir_as_hawq_user(directory):
  """
  Creates directories with hawq_user and hawq_group (defaults to gpadmin:gpadmin)
  """
  Directory(
        directory,
        recursive=True,
        owner=hawq_constants.hawq_user,
        group=hawq_constants.hawq_group)


def exec_hawq_operation(operation, option, not_if=None, only_if=None, logoutput=True):
  """
  Sets up execution environment and runs a given command as HAWQ user
  """
  hawq_cmd = "source {0} && hawq {1} {2}".format(hawq_constants.hawq_greenplum_path_file, operation, option)
  Execute(
        hawq_cmd,
        user=hawq_constants.hawq_user,
        timeout=hawq_constants.hawq_operation_exec_timeout,
        not_if=not_if,
        only_if=only_if,
        logoutput=logoutput)


def read_file_to_dict(file_name):
  """ 
  Converts a file with key=value format to dictionary
  """
  with open(file_name, "r") as fh:
    lines = fh.readlines()
    lines = [item for item in lines if '=' in item]
    result_dict = dict(item.split("=") for item in lines)
  return result_dict


def write_dict_to_file(source_dict, dest_file):
  """
  Writes a dictionary into a file with key=value format
  """
  with open(dest_file, "w") as fh:
    for property_key, property_value in source_dict.items():
      if property_value is None:
        fh.write(property_key + "\n")
      else:
        fh.write("{0}={1}\n".format(property_key, property_value))


def exec_ssh_cmd(hostname, cmd):
  """
  Runs the command on the remote host as gpadmin user
  """
  # Only gpadmin should be allowed to run command via ssh, thus not exposing user as a parameter
  cmd = "su - {0} -c \"ssh -o StrictHostKeyChecking=no -o UserKnownHostsFile=/dev/null {1} \\\"{2} \\\" \"".format(hawq_constants.hawq_user, hostname, cmd)
  Logger.info("Command executed: {0}".format(cmd))
  process = subprocess.Popen(cmd, stdout=subprocess.PIPE, stdin=subprocess.PIPE, stderr=subprocess.PIPE, shell=True)
  (stdout, stderr) = process.communicate()
  return process.returncode, stdout, stderr


def exec_psql_cmd(command, host, port, db="template1", tuples_only=True):
  """
  Sets up execution environment and runs the HAWQ queries
  """
  src_cmd = "export PGPORT={0} && source {1}".format(port, hawq_constants.hawq_greenplum_path_file)
  if tuples_only:
    cmd = src_cmd + " && psql -d {0} -c \\\\\\\"{1};\\\\\\\"".format(db, command)
  else:
    cmd = src_cmd + " && psql -t -d {0} -c \\\\\\\"{1};\\\\\\\"".format(db, command)
  retcode, out, err = exec_ssh_cmd(host, cmd)
  if retcode:
    Logger.error("SQL command executed failed: {0}\nReturncode: {1}\nStdout: {2}\nStderr: {3}".format(cmd, retcode, out, err))
    raise Fail("SQL command executed failed.")

  Logger.info("Output:\n{0}".format(out))
  return retcode, out, err
