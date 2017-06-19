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
import ambari_simplejson as json # simplejson is much faster comparing to Python 2.6 json module and has the same functions set.
import os
from resource_management.libraries.script.script import Script
from resource_management.core.resources.service import ServiceConfig
from resource_management.core.resources.system import Directory, Execute, File
from resource_management.core.exceptions import Fail
from resource_management.core import shell
from resource_management.core.shell import as_user, as_sudo
from resource_management.core.source import Template, InlineTemplate
from resource_management.libraries.functions.format import format
from resource_management.libraries.resources.properties_file import PropertiesFile
from resource_management.libraries.functions.flume_agent_helper import is_flume_process_live
from resource_management.libraries.functions.flume_agent_helper import find_expected_agent_names
from resource_management.libraries.functions.flume_agent_helper import await_flume_process_termination
from ambari_commons import OSConst
from ambari_commons.os_family_impl import OsFamilyFuncImpl, OsFamilyImpl
from resource_management.libraries.functions.show_logs import show_logs

@OsFamilyFuncImpl(os_family=OSConst.WINSRV_FAMILY)
def flume(action = None):
  import params

  from service_mapping import flume_win_service_name

  if action == 'config':
    ServiceConfig(flume_win_service_name,
                  action="configure",
                  start_type="manual")

    ServiceConfig(flume_win_service_name,
                  action="change_user",
                  username=params.flume_user,
                  password = Script.get_password(params.flume_user))

    # remove previously defined meta's
    for n in find_expected_agent_names(params.flume_conf_dir):
      os.unlink(os.path.join(params.flume_conf_dir, n, 'ambari-meta.json'))

    flume_agents = {}
    if params.flume_conf_content is not None:
      flume_agents = build_flume_topology(params.flume_conf_content)

    for agent in flume_agents.keys():
      flume_agent_conf_dir = os.path.join(params.flume_conf_dir, agent)
      flume_agent_conf_file = os.path.join(flume_agent_conf_dir, 'flume.conf')
      flume_agent_meta_file = os.path.join(flume_agent_conf_dir, 'ambari-meta.json')
      flume_agent_log4j_file = os.path.join(flume_agent_conf_dir, 'log4j.properties')
      flume_agent_env_file = os.path.join(flume_agent_conf_dir, 'flume-env.ps1')

      Directory(flume_agent_conf_dir
      )

      PropertiesFile(flume_agent_conf_file,
                     properties=flume_agents[agent])

      File(flume_agent_log4j_file,
           content=InlineTemplate(params.flume_log4j_content,agent_name=agent)),

      File(flume_agent_meta_file,
           content = json.dumps(ambari_meta(agent, flume_agents[agent])))

      File(flume_agent_env_file,
           owner=params.flume_user,
           content=InlineTemplate(params.flume_env_sh_template)
      )

      if params.has_metric_collector:
        File(os.path.join(flume_agent_conf_dir, "flume-metrics2.properties"),
             owner=params.flume_user,
             content=Template("flume-metrics2.properties.j2")
        )

@OsFamilyFuncImpl(os_family=OsFamilyImpl.DEFAULT)
def flume(action = None):
  import params

  if action == 'config':
    # remove previously defined meta's
    for n in find_expected_agent_names(params.flume_conf_dir):
      File(os.path.join(params.flume_conf_dir, n, 'ambari-meta.json'),
        action = "delete",
      )
      
    Directory(params.flume_run_dir,
              group=params.user_group,
              owner=params.flume_user,
    )

    Directory(params.flume_conf_dir,
              create_parents = True,
              owner=params.flume_user,
              )
    Directory(params.flume_log_dir,
              group=params.user_group,
              owner=params.flume_user,
              create_parents=True,
              cd_access="a",
              mode=0755,
    )

    flume_agents = {}
    if params.flume_conf_content is not None:
      flume_agents = build_flume_topology(params.flume_conf_content)

    for agent in flume_agents.keys():
      flume_agent_conf_dir = os.path.join(params.flume_conf_dir, agent)
      flume_agent_conf_file = os.path.join(flume_agent_conf_dir, 'flume.conf')
      flume_agent_meta_file = os.path.join(flume_agent_conf_dir, 'ambari-meta.json')
      flume_agent_log4j_file = os.path.join(flume_agent_conf_dir, 'log4j.properties')
      flume_agent_env_file = os.path.join(flume_agent_conf_dir, 'flume-env.sh')

      Directory(flume_agent_conf_dir,
                owner=params.flume_user,
                )

      PropertiesFile(flume_agent_conf_file,
        properties=flume_agents[agent],
        owner=params.flume_user,
        mode = 0644)

      File(flume_agent_log4j_file,
        content=InlineTemplate(params.flume_log4j_content,agent_name=agent),
        owner=params.flume_user,
        mode = 0644)

      File(flume_agent_meta_file,
        content = json.dumps(ambari_meta(agent, flume_agents[agent])),
        owner=params.flume_user,
        mode = 0644)

      File(flume_agent_env_file,
           owner=params.flume_user,
           content=InlineTemplate(params.flume_env_sh_template)
      )

      if params.has_metric_collector:
        File(os.path.join(flume_agent_conf_dir, "flume-metrics2.properties"),
             owner=params.flume_user,
             content=Template("flume-metrics2.properties.j2")
        )

  elif action == 'start':
    # desired state for service should be STARTED
    if len(params.flume_command_targets) == 0:
      _set_desired_state('STARTED')

    # It is important to run this command as a background process.

    flume_base = as_user(format("{flume_bin} agent --name {{0}} --conf {{1}} --conf-file {{2}} {{3}} > {flume_log_dir}/{{4}}.out 2>&1"), params.flume_user, env={'JAVA_HOME': params.java_home}) + " &"

    for agent in cmd_target_names():
      flume_agent_conf_dir = params.flume_conf_dir + os.sep + agent
      flume_agent_conf_file = flume_agent_conf_dir + os.sep + "flume.conf"
      flume_agent_pid_file = params.flume_run_dir + os.sep + agent + ".pid"

      if not os.path.isfile(flume_agent_conf_file):
        continue

      if not is_flume_process_live(flume_agent_pid_file):
        # TODO someday make the ganglia ports configurable
        extra_args = ''
        if params.ganglia_server_host is not None:
          extra_args = '-Dflume.monitoring.type=ganglia -Dflume.monitoring.hosts={0}:{1}'
          extra_args = extra_args.format(params.ganglia_server_host, '8655')
        if params.has_metric_collector:
          extra_args = '-Dflume.monitoring.type=org.apache.hadoop.metrics2.sink.flume.FlumeTimelineMetricsSink ' \
                       '-Dflume.monitoring.node={0}:{1}'
          # TODO check if this is used.
          extra_args = extra_args.format(params.metric_collector_host, params.metric_collector_port)

        flume_cmd = flume_base.format(agent, flume_agent_conf_dir,
           flume_agent_conf_file, extra_args, agent)

        Execute(flume_cmd, 
          wait_for_finish=False,
          environment={'JAVA_HOME': params.java_home}
        )
        # sometimes startup spawns a couple of threads - so only the first line may count
        pid_cmd = as_sudo(('pgrep', '-o', '-u', params.flume_user, '-f', format('^{java_home}.*{agent}.*'))) + \
        " | " + as_sudo(('tee', flume_agent_pid_file)) + "  && test ${PIPESTATUS[0]} -eq 0"
        
        try:
          Execute(pid_cmd,
                  logoutput=True,
                  tries=20,
                  try_sleep=10)
        except:
          show_logs(params.flume_log_dir, params.flume_user)
          raise

    pass
  elif action == 'stop':
    # desired state for service should be INSTALLED
    if len(params.flume_command_targets) == 0:
      _set_desired_state('INSTALLED')

    pid_files = glob.glob(params.flume_run_dir + os.sep + "*.pid")

    if 0 == len(pid_files):
      return

    agent_names = cmd_target_names()


    for agent in agent_names:
      pid_file = format("{flume_run_dir}/{agent}.pid")
      
      if is_flume_process_live(pid_file):
        pid = shell.checked_call(("cat", pid_file), sudo=True)[1].strip()
        Execute(("kill", "-15", pid), sudo=True)    # kill command has to be a tuple
        if not await_flume_process_termination(pid_file, try_count=30):
          Execute(("kill", "-9", pid), sudo=True)
      
      if not await_flume_process_termination(pid_file, try_count=10):
        show_logs(params.flume_log_dir, params.flume_user)
        raise Fail("Can't stop flume agent: {0}".format(agent))
        
      File(pid_file, action = 'delete')


def ambari_meta(agent_name, agent_conf):
  res = {}

  sources = agent_conf[agent_name + '.sources'].split(' ')
  res['sources_count'] = len(sources)

  sinks = agent_conf[agent_name + '.sinks'].split(' ')
  res['sinks_count'] = len(sinks)

  channels = agent_conf[agent_name + '.channels'].split(' ')
  res['channels_count'] = len(channels)

  return res


# define a map of dictionaries, where the key is agent name
# and the dictionary is the name/value pair
def build_flume_topology(content):

  result = {}
  agent_names = []

  for line in content.split('\n'):
    rline = line.strip()
    if 0 != len(rline) and not rline.startswith('#'):
      pair = rline.split('=')
      lhs = pair[0].strip()
      # workaround for properties that contain '='
      rhs = "=".join(pair[1:]).strip()

      part0 = lhs.split('.')[0]

      if lhs.endswith(".sources"):
        agent_names.append(part0)

      if not result.has_key(part0):
        result[part0] = {}

      result[part0][lhs] = rhs

  # trim out non-agents
  for k in result.keys():
    if not k in agent_names:
      del result[k]

  return result


def cmd_target_names():
  import params

  if len(params.flume_command_targets) > 0:
    return params.flume_command_targets
  else:
    return find_expected_agent_names(params.flume_conf_dir)


def _set_desired_state(state):
  import params
  File(params.ambari_state_file,
    content = state,
  )

def get_desired_state():
  import params
  from resource_management.core import sudo
  if os.path.exists(params.ambari_state_file):
    return sudo.read_file(params.ambari_state_file)
  else:
    return 'INSTALLED'
  
