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
from resource_management.core.resources.system import Directory, File
from resource_management.libraries.functions.decorator import retry
from resource_management.libraries.functions.format import format
from resource_management.libraries.functions import solr_cloud_util

def setup_infra_solr(name = None):
  import params

  if name == 'server':
    Directory([params.infra_solr_log_dir, params.infra_solr_piddir,
               params.infra_solr_datadir, params.infra_solr_data_resources_dir],
              mode=0755,
              cd_access='a',
              create_parents=True,
              owner=params.infra_solr_user,
              group=params.user_group
              )

    Directory([params.solr_dir, params.infra_solr_conf],
              mode=0755,
              cd_access='a',
              owner=params.infra_solr_user,
              group=params.user_group,
              create_parents=True,
              recursive_ownership=True
              )

    File(params.infra_solr_log,
         mode=0644,
         owner=params.infra_solr_user,
         group=params.user_group,
         content=''
         )

    File(format("{infra_solr_conf}/infra-solr-env.sh"),
         content=InlineTemplate(params.solr_env_content),
         mode=0755,
         owner=params.infra_solr_user,
         group=params.user_group
         )

    File(format("{infra_solr_datadir}/solr.xml"),
         content=InlineTemplate(params.solr_xml_content),
         owner=params.infra_solr_user,
         group=params.user_group
         )

    File(format("{infra_solr_conf}/log4j.properties"),
         content=InlineTemplate(params.solr_log4j_content),
         owner=params.infra_solr_user,
         group=params.user_group
         )

    custom_security_json_location = format("{infra_solr_conf}/custom-security.json")
    File(custom_security_json_location,
         content=InlineTemplate(params.infra_solr_security_json_content),
         owner=params.infra_solr_user,
         group=params.user_group,
         mode=0640
         )

    jaas_file = params.infra_solr_jaas_file if params.security_enabled else None
    url_scheme = 'https' if params.infra_solr_ssl_enabled else 'http'

    create_ambari_solr_znode()

    if params.has_logsearch:
      cleanup_logsearch_collections(params.logsearch_service_logs_collection, jaas_file)
      cleanup_logsearch_collections(params.logsearch_audit_logs_collection, jaas_file)
      cleanup_logsearch_collections('history', jaas_file)

    security_json_file_location = custom_security_json_location \
      if params.infra_solr_security_json_content and str(params.infra_solr_security_json_content).strip() \
      else format("{infra_solr_conf}/security.json") # security.json file to upload

    if params.security_enabled:
      File(format("{infra_solr_jaas_file}"),
           content=Template("infra_solr_jaas.conf.j2"),
           owner=params.infra_solr_user)

      File(format("{infra_solr_conf}/security.json"),
           content=Template("infra-solr-security.json.j2"),
           owner=params.infra_solr_user,
           group=params.user_group,
           mode=0640)

    solr_cloud_util.set_cluster_prop(
      zookeeper_quorum=params.zookeeper_quorum,
      solr_znode=params.infra_solr_znode,
      java64_home=params.java64_home,
      prop_name="urlScheme",
      prop_value=url_scheme,
      jaas_file=jaas_file
    )

    solr_cloud_util.setup_kerberos_plugin(
      zookeeper_quorum=params.zookeeper_quorum,
      solr_znode=params.infra_solr_znode,
      jaas_file=jaas_file,
      java64_home=params.java64_home,
      secure=params.security_enabled,
      security_json_location=security_json_file_location
    )

    if params.security_enabled:
      solr_cloud_util.secure_solr_znode(
        zookeeper_quorum=params.zookeeper_quorum,
        solr_znode=params.infra_solr_znode,
        jaas_file=jaas_file,
        java64_home=params.java64_home,
        sasl_users_str=params.infra_solr_sasl_user
      )


  elif name == 'client':
    solr_cloud_util.setup_solr_client(params.config)

  else :
    raise Fail('Nor client or server were selected to install.')

@retry(times=30, sleep_time=5, err_class=Fail)
def create_ambari_solr_znode():
  import params
  solr_cloud_util.create_znode(
    zookeeper_quorum=params.zookeeper_quorum,
    solr_znode=params.infra_solr_znode,
    java64_home=params.java64_home,
    retry=30, interval=5)

def cleanup_logsearch_collections(collection, jaas_file):
  import params
  solr_cloud_util.remove_admin_handlers(
    zookeeper_quorum=params.zookeeper_quorum,
    solr_znode=params.infra_solr_znode,
    java64_home=params.java64_home,
    jaas_file=jaas_file,
    collection=collection
  )