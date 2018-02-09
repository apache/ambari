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

from resource_management.libraries.functions.default import default
from resource_management.core.resources.system import Directory, File
from resource_management.libraries.functions.format import format
from resource_management.core.source import InlineTemplate, Template
from resource_management.libraries.resources.properties_file import PropertiesFile
from resource_management.libraries.functions.security_commons import update_credential_provider_path, HADOOP_CREDENTIAL_PROVIDER_PROPERTY_NAME

def setup_logfeeder():
  import params

  Directory([params.logfeeder_log_dir, params.logfeeder_pid_dir, params.logfeeder_checkpoint_folder],
            mode=0755,
            cd_access='a',
            create_parents=True
            )

  Directory([params.logfeeder_dir, params.logsearch_logfeeder_conf],
            mode=0755,
            cd_access='a',
            create_parents=True,
            recursive_ownership=True
            )

  File(format("{logfeeder_log_dir}/{logfeeder_log}"),
       mode=0644,
       content=''
       )

  if params.credential_store_enabled:
    params.logfeeder_env_config = update_credential_provider_path(params.logfeeder_env_config,
                                                                 'logfeeder-env',
                                                                 params.logfeeder_env_jceks_file,
                                                                 params.logsearch_user,
                                                                 params.user_group
                                                                 )
    params.logfeeder_properties[HADOOP_CREDENTIAL_PROVIDER_PROPERTY_NAME] = 'jceks://file' + params.logfeeder_env_jceks_file
    File(format("{logsearch_logfeeder_keys_folder}/ks_pass.txt"),
         action="delete"
         )
    File(format("{logsearch_logfeeder_keys_folder}/ts_pass.txt"),
         action="delete"
         )
  else:
    Directory(params.logsearch_logfeeder_keys_folder,
              cd_access='a',
              mode=0755,
              owner=params.logsearch_user,
              group=params.user_group
              )
   
    File(format("{logsearch_logfeeder_keys_folder}/ks_pass.txt"),
         content=params.logfeeder_keystore_password,
         mode=0600,
         owner=params.logsearch_user,
         group=params.user_group
         )

    File(format("{logsearch_logfeeder_keys_folder}/ts_pass.txt"),
         content=params.logfeeder_truststore_password,
         mode=0600,
         owner=params.logsearch_user,
         group=params.user_group
         )
  
  PropertiesFile(format("{logsearch_logfeeder_conf}/logfeeder.properties"),
                 properties = params.logfeeder_properties
                 )

  File(format("{logsearch_logfeeder_conf}/logfeeder-env.sh"),
       content=InlineTemplate(params.logfeeder_env_content),
       mode=0755
       )

  File(format("{logsearch_logfeeder_conf}/log4j.xml"),
       content=InlineTemplate(params.logfeeder_log4j_content)
       )

  File(format("{logsearch_logfeeder_conf}/grok-patterns"),
       content=InlineTemplate(params.logfeeder_grok_patterns),
       encoding="utf-8"
       )

  File(format("{logsearch_logfeeder_conf}/global.config.json"),
       content=Template("global.config.json.j2")
       )

  File(format("{logsearch_logfeeder_conf}/input.config-ambari.json"),
       content=InlineTemplate(params.logfeeder_ambari_config_content),
       encoding="utf-8"
       )

  File(format("{logsearch_logfeeder_conf}/output.config.json"),
       content=InlineTemplate(params.logfeeder_output_config_content),
       encoding="utf-8"
       )

  if params.logfeeder_system_log_enabled:
    File(format("{logsearch_logfeeder_conf}/input.config-system_messages.json"),
         content=params.logfeeder_system_messages_content
         )
    File(format("{logsearch_logfeeder_conf}/input.config-secure_log.json"),
         content=params.logfeeder_secure_log_content
         )


  if params.logsearch_solr_kerberos_enabled:
    File(format("{logfeeder_jaas_file}"),
         content=Template("logfeeder_jaas.conf.j2")
         )

