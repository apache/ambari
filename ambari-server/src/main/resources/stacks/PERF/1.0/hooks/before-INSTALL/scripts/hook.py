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
import os
from resource_management import ExecutionFailed
from resource_management.core.resources.system import Execute
from resource_management.core.shell import call
from resource_management.libraries.functions.default import default
from resource_management.libraries.script import Hook

AMBARI_AGENT_CACHE_DIR = 'AMBARI_AGENT_CACHE_DIR'
DEFAULT_AMBARI_AGENT_CACHE_DIR = '/var/lib/ambari-agent/cache/'

BEFORE_INSTALL_SCRIPTS = "hooks/before-INSTALL/scripts"
STACK = "PERF/1.0"
STACKS = "stacks"
DISTRO_SELECT_PY = os.path.join(STACKS, STACK, BEFORE_INSTALL_SCRIPTS, "distro-select.py")
CONF_SELECT_PY = os.path.join(STACKS, STACK, BEFORE_INSTALL_SCRIPTS, "conf-select.py")
DISTRO_SELECT_DEST = "/usr/bin/distro-select"
CONF_SELECT_DEST = "/usr/bin/conf-select"

class BeforeInstallHook(Hook):

  def hook(self, env):
    self.run_custom_hook('before-ANY')
    print "Before Install Hook"
    cache_dir = self.extrakt_var_from_pythonpath(AMBARI_AGENT_CACHE_DIR)

    # this happens if PythonExecutor.py.sed hack was not done.
    if not cache_dir:
      print "WARN: Cache dir for the agent could not be detected. Using default cache dir"
      cache_dir = DEFAULT_AMBARI_AGENT_CACHE_DIR

    conf_select = os.path.join(cache_dir, CONF_SELECT_PY)
    dist_select = os.path.join(cache_dir, DISTRO_SELECT_PY)
    try:
      Execute("cp -n %s %s" % (conf_select, CONF_SELECT_DEST), user="root")
      Execute("chmod a+x %s" % (CONF_SELECT_DEST), user="root")
    except ExecutionFailed:
      pass   # Due to concurrent execution, may produce error

    try:
      Execute("cp -n %s %s" % (dist_select, DISTRO_SELECT_DEST), user="root")
      Execute("chmod a+x %s" % (DISTRO_SELECT_DEST), user="root")
      stack_version_unformatted = str(default("/clusterLevelParams/stack_version", ""))
      call((DISTRO_SELECT_DEST, 'deploy_cluster', stack_version_unformatted))
    except ExecutionFailed:
      pass   # Due to concurrent execution, may produce error

  def extrakt_var_from_pythonpath(self, name):

    PATH = os.environ['PATH']
    paths = PATH.split(':')
    var = ''
    for item in paths:
      if item.startswith(name):
        var = item.replace(name, '')
        break
    return var

if __name__ == "__main__":
  BeforeInstallHook().execute()
