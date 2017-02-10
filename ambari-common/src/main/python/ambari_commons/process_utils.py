# !/usr/bin/env python

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

import subprocess
import time

check_time_delay = 0.1  # seconds between checks of process killed


def get_children(pid):
  PSCMD = ["ps", "-o", "pid", "--no-headers", "--ppid", str(pid)]
  ps_process = subprocess.Popen(PSCMD, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
  stdout, stderr = ps_process.communicate()
  if ps_process.returncode != 0:
    return []
  return stdout.split()


def get_flat_process_tree(pid):
  """
  :param pid: process id of parent process
  :return: list of child process pids. Resulting list also includes parent pid
  """
  res = [str(pid)]
  children = get_children(pid)
  for child in children:
    res += get_flat_process_tree(child)
  return res


def kill_pids(pids, signal):
  from resource_management.core.exceptions import Fail
  CMD = ["kill", "-" + str(signal)]
  CMD.extend(pids)
  process = subprocess.Popen(CMD, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
  stdout, stderr = process.communicate()
  if process.returncode != 0:
    raise Fail("Unable to kill PIDs {0} : {1}".format(str(pids),stderr))


def get_command_by_pid(pid):
  CMD = ["ps", "-p", str(pid), "-o", "command", "--no-headers"]
  process = subprocess.Popen(CMD, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
  stdout, stderr = process.communicate()
  if process.returncode != 0:
    return "NOT_FOUND[%s]" % pid
  return stdout


def wait_for_entire_process_tree_death(pids):
  for child in pids:
    wait_for_process_death(child)


def wait_for_process_death(pid, timeout=5):
  start = time.time()
  current_time = start
  while is_process_running(pid) and current_time < start + timeout:
    time.sleep(check_time_delay)
    current_time = time.time()


def is_process_running(pid):
  CMD = ["ps", "-p", str(pid), "-o", "pid", "--no-headers"]
  process = subprocess.Popen(CMD, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
  stdout, stderr = process.communicate()
  if process.returncode != 0:
    return False
  return pid in stdout



def get_processes_running(process_pids):
  """
  Checks what processes are still running
  :param process_pids: list of process pids
  :return: list of pids for processes that are still running
  """
  result = []
  for pid in process_pids:
    if is_process_running(pid):
      result.append(pid)
  return result
