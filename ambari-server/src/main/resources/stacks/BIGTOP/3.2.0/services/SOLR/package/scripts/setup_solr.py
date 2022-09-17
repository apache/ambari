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
from resource_management.core.exceptions import ExecutionFailed, Fail
from resource_management.core.logger import Logger
from resource_management.core.resources.system import Execute, Directory, File
from resource_management.core.source import InlineTemplate, Template
from resource_management.libraries.functions import solr_cloud_util
from resource_management.libraries.functions.decorator import retry
from resource_management.libraries.functions.format import format

def setup_solr(name = None):
  import params

  if name == 'server':
    Directory([params.solr_log_dir, params.solr_piddir,
               params.solr_datadir, params.solr_data_resources_dir],
              mode=0755,
              cd_access='a',
              create_parents=True,
              owner=params.solr_user,
              group=params.user_group
              )

    Directory([params.solr_dir, params.solr_conf],
              mode=0755,
              cd_access='a',
              owner=params.solr_user,
              group=params.user_group,
              create_parents=True,
              recursive_ownership=True
              )

    File(params.solr_log,
         mode=0644,
         owner=params.solr_user,
         group=params.user_group,
         content=''
         )

    File(format("{solr_conf}/solr-env.sh"),
         content=InlineTemplate(params.solr_env_content),
         mode=0755,
         owner=params.solr_user,
         group=params.user_group
         )

    File(format("{solr_datadir}/solr.xml"),
         content=InlineTemplate(params.solr_xml_content),
         owner=params.solr_user,
         group=params.user_group
         )

    File(format("{solr_conf}/log4j2.xml"),
         content=InlineTemplate(params.solr_log4j_content),
         owner=params.solr_user,
         group=params.user_group
         )

    custom_security_json_location = format("{solr_conf}/custom-security.json")
    File(custom_security_json_location,
         content=InlineTemplate(params.solr_security_json_content),
         owner=params.solr_user,
         group=params.user_group,
         mode=0640
         )

    if params.security_enabled:
      File(format("{solr_jaas_file}"),
           content=Template("solr_jaas.conf.j2"),
           owner=params.solr_user)

      File(format("{solr_conf}/security.json"),
           content=Template("solr-security.json.j2"),
           owner=params.solr_user,
           group=params.user_group,
           mode=0640)
    if os.path.exists(params.limits_conf_dir):
      File(os.path.join(params.limits_conf_dir, 'solr.conf'),
           owner='root',
           group='root',
           mode=0644,
           content=Template("solr.conf.j2")
      )

  elif name == 'client':
    solr_cloud_util.setup_solr_client(params.config)

  else :
    raise Fail('Nor client or server were selected to install.')

def setup_solr_znode_env():
  """
  Setup SSL, ACL and authentication / authorization related Zookeeper settings for Solr (checkout: /clustersprops.json and /security.json)
  """
  import params

  custom_security_json_location = format("{solr_conf}/custom-security.json")
  jaas_file = params.solr_jaas_file if params.security_enabled else None
  java_opts = params.zk_security_opts if params.security_enabled else None
  url_scheme = 'https' if params.solr_ssl_enabled else 'http'

  security_json_file_location = custom_security_json_location \
    if params.solr_security_json_content and str(params.solr_security_json_content).strip() \
    else format("{solr_conf}/security.json") # security.json file to upload

  create_solr_znode(java_opts, jaas_file)

#   solr_cloud_util.set_cluster_prop(
#     zookeeper_quorum=params.zk_quorum,
#     solr_znode=params.solr_znode,
#     java64_home=params.java64_home,
#     prop_name="urlScheme",
#     prop_value=url_scheme,
#     jaas_file=jaas_file,
#     java_opts=java_opts
#   )
#   if not params.solr_security_manually_managed:
#     solr_cloud_util.setup_kerberos_plugin(
#       zookeeper_quorum=params.zk_quorum,
#       solr_znode=params.solr_znode,
#       jaas_file=jaas_file,
#       java64_home=params.java64_home,
#       secure=params.security_enabled,
#       security_json_location=security_json_file_location,
#       java_opts=java_opts
#     )

#   if params.security_enabled:
#     solr_cloud_util.secure_solr_znode(
#       zookeeper_quorum=params.zk_quorum,
#       solr_znode=params.solr_znode,
#       jaas_file=jaas_file,
#       java64_home=params.java64_home,
#       sasl_users_str=params.solr_sasl_user,
#       java_opts=java_opts
#     )

def create_solr_znode(java_opts, jaas_file):
  import params

  create_cmd = format('{solr_bindir}/solr zk mkroot {solr_znode} -z {zookeeper_quorum} -Dsolr.kerberos.name.rules=\'{solr_kerberos_name_rules}\' 2>&1') \
            if params.security_enabled else format('{solr_bindir}/solr zk mkroot {solr_znode} -z {zookeeper_quorum} 2>&1')

  try:
    Execute(
      create_cmd,
      environment={'SOLR_INCLUDE': format('{solr_conf}/solr-env.sh')},
      user=params.solr_user
    )
  except ExecutionFailed as e:
    if (format("NodeExists for {solr_znode}") in str(e)):
      Logger.info(format("Node {solr_znode} already exists."))
    else:
      raise e
  except Exception as e:
    raise e

  # solr_cloud_util.create_znode(
  #   zookeeper_quorum=params.zk_quorum,
  #   solr_znode=params.solr_znode,
  #   java64_home=params.java64_home,
  #   retry=30, interval=5, java_opts=java_opts, jaas_file=jaas_file)