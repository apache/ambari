#!/usr/bin/env python

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

import json
import glob
import os

from resource_management.core.exceptions import ComponentIsNotRunning
from resource_management.libraries.functions import check_process_status


def get_flume_status(flume_conf_directory, flume_run_directory):
  """
  Gets the sources, sink, and channel information for all expected flume
  agent processes.

  :param flume_conf_directory:  the configuration directory (ie /etc/flume/conf)
  :param flume_run_directory: the run directory (ie /var/run/flume)
  :return: a list of status information for each expected flume agent
  """
  meta_files = find_expected_agent_names(flume_conf_directory)
  pid_files = []

  for agent_name in meta_files:
    pid_files.append(os.path.join(flume_run_directory, agent_name + '.pid'))

  processes = []
  for pid_file in pid_files:
    processes.append(get_live_status(pid_file, flume_conf_directory))

  return processes


def find_expected_agent_names(flume_conf_directory):
  """
  Gets the names of the flume agents that Ambari is aware of.
  :param flume_conf_directory:  the configuration directory (ie /etc/flume/conf)
  :return: a list of names of expected flume agents
  """
  files = glob.glob(flume_conf_directory + os.sep + "*/ambari-meta.json")
  expected = []

  for f in files:
    expected.append(os.path.dirname(f).split(os.sep).pop())

  return expected


def is_flume_process_live(pid_file):
  """
  Gets whether the flume agent represented by the specified file is running.
  :param pid_file: the PID file of the agent to check
  :return: True if the agent is running, False otherwise
  """
  live = False

  try:
    check_process_status(pid_file)
    live = True
  except ComponentIsNotRunning:
    pass

  return live


def get_live_status(pid_file, flume_conf_directory):
  """
  Gets the status information of a flume agent, including source, sink, and
  channel counts.
  :param pid_file: the PID file of the agent to check
  :param flume_conf_directory:  the configuration directory (ie /etc/flume/conf)
  :return: a dictionary of information about the flume agent
  """
  pid_file_part = pid_file.split(os.sep).pop()

  res = {}
  res['name'] = pid_file_part

  if pid_file_part.endswith(".pid"):
    res['name'] = pid_file_part[:-4]

  res['status'] = 'RUNNING' if is_flume_process_live(pid_file) else 'NOT_RUNNING'
  res['sources_count'] = 0
  res['sinks_count'] = 0
  res['channels_count'] = 0

  flume_agent_conf_dir = flume_conf_directory + os.sep + res['name']
  flume_agent_meta_file = flume_agent_conf_dir + os.sep + 'ambari-meta.json'

  try:
    with open(flume_agent_meta_file) as fp:
      meta = json.load(fp)
      res['sources_count'] = meta['sources_count']
      res['sinks_count'] = meta['sinks_count']
      res['channels_count'] = meta['channels_count']
  except:
    pass

  return res