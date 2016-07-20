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

from resource_management.core.resources.system import Execute, File
from resource_management.libraries.functions.format import format
from resource_management.libraries.functions.check_process_status import check_process_status
from resource_management.libraries.script.script import Script
from setup_logsearch_solr import setup_logsearch_solr
from logsearch_common import kill_process
from resource_management.core.logger import Logger
import sys

class LogsearchSolr(Script):
  def install(self, env):
    import params
    env.set_params(params)
    self.install_packages(env)

  def configure(self, env, upgrade_type=None):
    import params
    env.set_params(params)

    setup_logsearch_solr(name = 'server')

  def start(self, env, upgrade_type=None):
    import params
    env.set_params(params)
    self.configure(env)

    Execute(
      format('{solr_bindir}/solr start -cloud -noprompt -s {logsearch_solr_datadir} >> {logsearch_solr_log} 2>&1'),
      environment={'SOLR_INCLUDE': format('{logsearch_solr_conf}/logsearch-solr-env.sh')},
      user=params.logsearch_solr_user
    )

  def stop(self, env, upgrade_type=None):
    import params
    env.set_params(params)

    try:
      Execute(format('{solr_bindir}/solr stop -all >> {logsearch_solr_log}'),
              environment={'SOLR_INCLUDE': format('{logsearch_solr_conf}/logsearch-solr-env.sh')},
              user=params.logsearch_solr_user,
              only_if=format("test -f {logsearch_solr_pidfile}")
              )
      
      File(params.logsearch_solr_pidfile,
           action="delete"
           )
    except:
      Logger.warning("Could not stop solr:" + str(sys.exc_info()[1]) + "\n Trying to kill it")
      kill_process(params.logsearch_solr_pidfile, params.logsearch_solr_user, params.logsearch_solr_log_dir);

  def status(self, env):
    import status_params
    env.set_params(status_params)

    check_process_status(status_params.logsearch_solr_pidfile)


if __name__ == "__main__":
  LogsearchSolr().execute()
