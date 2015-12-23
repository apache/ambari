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

from resource_management import *
import os


def groups_and_users():
  import params

def config():
  import params

  shell_cmds_dir = params.ganglia_shell_cmds_dir
  shell_files = ['checkGmond.sh', 'checkRrdcached.sh', 'gmetadLib.sh',
                 'gmondLib.sh', 'rrdcachedLib.sh',
                 'setupGanglia.sh', 'startGmetad.sh', 'startGmond.sh',
                 'startRrdcached.sh', 'stopGmetad.sh',
                 'stopGmond.sh', 'stopRrdcached.sh', 'teardownGanglia.sh']
  Directory(shell_cmds_dir,
            owner="root",
            group="root",
            create_parents = True
  )
  init_file("gmetad")
  init_file("gmond")
  for sh_file in shell_files:
    shell_file(sh_file)
  for conf_file in ['gangliaClusters.conf', 'gangliaEnv.sh', 'gangliaLib.sh']:
    ganglia_TemplateConfig(conf_file)


def init_file(name):
  import params

  File("/etc/init.d/hdp-" + name,
       content=StaticFile(name + ".init"),
       mode=0755
  )


def shell_file(name):
  import params

  File(params.ganglia_shell_cmds_dir + os.sep + name,
       content=StaticFile(name),
       mode=0755
  )


def ganglia_TemplateConfig(name, mode=0755, tag=None):
  import params

  TemplateConfig(format("{params.ganglia_shell_cmds_dir}/{name}"),
                 owner="root",
                 group="root",
                 template_tag=tag,
                 mode=mode
  )


def generate_daemon(ganglia_service,
                    name=None,
                    role=None,
                    owner=None,
                    group=None):
  import params

  cmd = ""
  if ganglia_service == "gmond":
    if role == "server":
      cmd = "{params.ganglia_shell_cmds_dir}/setupGanglia.sh -c {name} -m -o {owner} -g {group}"
    else:
      cmd = "{params.ganglia_shell_cmds_dir}/setupGanglia.sh -c {name} -o {owner} -g {group}"
  elif ganglia_service == "gmetad":
    cmd = "{params.ganglia_shell_cmds_dir}/setupGanglia.sh -t -o {owner} -g {group}"
  else:
    raise Fail("Unexpected ganglia service")
  Execute(format(cmd),
          path=[params.ganglia_shell_cmds_dir, "/usr/sbin",
                "/sbin:/usr/local/bin", "/bin", "/usr/bin"]
  )
