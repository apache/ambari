#!/usr/bin/env python3
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

Ambari Agent

"""

from resource_management.core.exceptions import ComponentIsNotRunning, Fail
from resource_management.core.logger import Logger
from resource_management.libraries.functions.safe_process import read_running_process

__all__ = ["check_process_status"]


def check_process_status(pid_file, expected_user=None, expected_cmdline=None):
  """
  Function checks whether process is running.
  Process is considered running, if pid file exists, and process with
  a pid, mentioned in pid file is running
  If process is not running, will throw ComponentIsNotRunning exception

  @param pid_file: path to service pid file
  @param expected_user: optional operating system user that must own the process
  @param expected_cmdline: optional command line fragment or fragments that must match
  """
  try:
    identity = read_running_process(pid_file, expected_user, expected_cmdline)
  except Fail as error:
    Logger.info(f"Process in pid file {pid_file} failed validation: {error}")
    if expected_user is not None or expected_cmdline is not None:
      raise
    raise ComponentIsNotRunning() from error
  if identity is None:
    Logger.info(f"Pid file {str(pid_file)} is empty, stale, or does not exist")
    raise ComponentIsNotRunning()


def wait_process_stopped(pid_file):
  """
  Waits until component is actually stopped (check is performed using
  check_process_status() method.
  """
  import time

  component_is_stopped = False
  counter = 0
  while not component_is_stopped:
    try:
      if counter % 10 == 0:
        Logger.logger.info("Waiting for actual component stop")
      check_process_status(pid_file)
      time.sleep(1)
      counter += 1
    except ComponentIsNotRunning as e:
      Logger.logger.debug(" reports ComponentIsNotRunning")
      component_is_stopped = True
