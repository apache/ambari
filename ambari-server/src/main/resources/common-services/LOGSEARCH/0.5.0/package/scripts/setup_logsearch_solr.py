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

from resource_management.core.exceptions import Fail
from resource_management.core.source import InlineTemplate, Template
from resource_management.core.resources.system import Directory, Execute, File
from resource_management.libraries.functions.format import format
from resource_management.libraries.functions import solr_cloud_util


def setup_logsearch_solr(name = None):
  import params

  if name == 'server':
    Directory([params.solr_dir, params.logsearch_solr_log_dir, params.logsearch_solr_piddir, params.logsearch_solr_conf,
               params.logsearch_solr_datadir, params.logsearch_solr_data_resources_dir],
              mode=0755,
              cd_access='a',
              owner=params.logsearch_solr_user,
              group=params.logsearch_solr_group,
              create_parents=True
              )

    File(params.logsearch_solr_log,
         mode=0644,
         owner=params.logsearch_solr_user,
         group=params.logsearch_solr_group,
         content=''
         )

    File(format("{logsearch_solr_conf}/logsearch-solr-env.sh"),
         content=InlineTemplate(params.solr_env_content),
         mode=0755,
         owner=params.logsearch_solr_user
         )

    File(format("{logsearch_solr_datadir}/solr.xml"),
         content=InlineTemplate(params.solr_xml_content),
         owner=params.logsearch_solr_user
         )

    File(format("{logsearch_solr_conf}/log4j.properties"),
         content=InlineTemplate(params.solr_log4j_content),
         owner=params.logsearch_solr_user
         )

    File(format("{logsearch_solr_datadir}/zoo.cfg"),
         content=Template("zoo.cfg.j2"),
         owner=params.logsearch_solr_user
         )

    zk_cli_prefix = format('export JAVA_HOME={java64_home}; {cloud_scripts}/zkcli.sh -zkhost {zookeeper_hosts}')
    Execute(format('{zk_cli_prefix} -cmd makepath {logsearch_solr_znode}'),
            not_if=format("{zk_cli_prefix}{logsearch_solr_znode} -cmd list"),
            ignore_failures=True,
            user=params.logsearch_solr_user
            )
    if params.logsearch_solr_ssl_enabled:
      Execute(format('{zk_cli_prefix}{logsearch_solr_znode} -cmd clusterprop -name urlScheme -val https'),
              ignore_failures=True,
              user=params.logsearch_solr_user
              )
    else:
      Execute(format('{zk_cli_prefix}{logsearch_solr_znode} -cmd clusterprop -name urlScheme -val http'),
              ignore_failures=True,
              user=params.logsearch_solr_user
              )
  elif name == 'client':
    solr_cloud_util.setup_solr_client(params.config)

  else :
    raise Fail('Nor client or server were selected to install.')