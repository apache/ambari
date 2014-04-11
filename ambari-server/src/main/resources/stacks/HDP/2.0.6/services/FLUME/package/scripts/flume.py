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

import glob
import os
from resource_management import *

def flume(action = None):
  import params

  flume_agents = {}
  if params.flume_conf_content is not None:
    flume_agents = build_flume_topology(params.flume_conf_content)

  if action == 'config':
    Directory(params.flume_conf_dir)
    Directory(params.flume_log_dir, owner=params.flume_user)

    for agent in flume_agents.keys():
      flume_agent_conf_dir = params.flume_conf_dir + os.sep + agent
      flume_agent_conf_file = flume_agent_conf_dir + os.sep + 'flume.conf'
      flume_agent_log4j_file = flume_agent_conf_dir + os.sep + 'log4j.properties'

      Directory(flume_agent_conf_dir)

      PropertiesFile(flume_agent_conf_file,
        properties=flume_agents[agent],
        mode = 0644)

      File(flume_agent_log4j_file,
        content=Template('log4j.properties.j2', agent_name = agent),
        mode = 0644)

  elif action == 'start':
    flume_base = format('env JAVA_HOME={java_home} /usr/bin/flume-ng agent '
      '--name {{0}} '
      '--conf {{1}} '
      '--conf-file {{2}} '
      '{{3}}')

    for agent in flume_agents.keys():
      flume_agent_conf_dir = params.flume_conf_dir + os.sep + agent
      flume_agent_conf_file = flume_agent_conf_dir + os.sep + "flume.conf"
      flume_agent_pid_file = params.flume_run_dir + os.sep + agent + ".pid"

      if not is_live(flume_agent_pid_file):
        # TODO someday make the ganglia ports configurable
        extra_args = ''
        if params.ganglia_server_host is not None:
          extra_args = '-Dflume.monitoring.type=ganglia -Dflume.monitoring.hosts={0}:{1}'
          extra_args = extra_args.format(params.ganglia_server_host, '8655')

        flume_cmd = flume_base.format(agent, flume_agent_conf_dir,
           flume_agent_conf_file, extra_args)

        Execute(flume_cmd, wait_for_finish=False)

        # sometimes startup spawns a couple of threads - so only the first line may count
        pid_cmd = format('pgrep -o -f {flume_agent_conf_file} > {flume_agent_pid_file}')

        Execute(pid_cmd, logoutput=True, tries=5, try_sleep=10)

    pass
  elif action == 'stop':
    pid_files = glob.glob(params.flume_run_dir + os.sep + "*.pid")

    if 0 == len(pid_files):
      return

    for pid_file in pid_files:
      pid = format('`cat {pid_file}` > /dev/null 2>&1')
      Execute(format('kill {pid}'), ignore_failures=True)

    for pid_file in pid_files:
      File(pid_file, action = 'delete')
    
    pass
  elif action == 'status':
    pass

# define a map of dictionaries, where the key is agent name
# and the dictionary is the name/value pair
def build_flume_topology(content):
  import ConfigParser
  import StringIO

  config = StringIO.StringIO()
  config.write('[dummy]\n')
  config.write(content)
  config.seek(0, os.SEEK_SET)

  cp = ConfigParser.ConfigParser()
  cp.readfp(config)

  result = {}
  agent_names = []

  for item in cp.items('dummy'):
    key = item[0]
    part0 = key.split('.')[0]
    if key.endswith(".sources"):
      agent_names.append(part0)

    if not result.has_key(part0):
      result[part0] = {}

    result[part0][key] = item[1]

  # trim out non-agents
  for k in result.keys():
    if not k in agent_names:
      del result[k]

  return result

def is_live(pid_file):
  live = False

  try:
    check_process_status(pid_file)
    live = True
  except ComponentIsNotRunning:
    pass

  return live

def live_status(pid_file):
  res = {}
  res['name'] = pid_file.split(os.sep).pop()
  res['status'] = 'RUNNING' if is_live(pid_file) else 'NOT_RUNNING'

  return res
  

def flume_status():
  import params
  
  procs = []

  pid_files = glob.glob(params.flume_run_dir + os.sep + "*.pid")

  if 0 != len(pid_files):
    for pid_file in pid_files:
      procs.append(live_status(pid_file))

  return procs

