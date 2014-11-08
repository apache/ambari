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


def knox():
    import params

    Directory(params.knox_conf_dir,
              owner = params.knox_user,
              group = params.knox_group,
              recursive = True
    )


    XmlConfig("gateway-site.xml",
              conf_dir=params.knox_conf_dir,
              configurations=params.config['configurations']['gateway-site'],
              configuration_attributes=params.config['configuration_attributes']['gateway-site'],
              owner=params.knox_user,
              group=params.knox_group,
    )

    File(format("{params.knox_conf_dir}/gateway-log4j.properties"),
         mode=0644,
         group=params.knox_group,
         owner=params.knox_user,
         content=params.gateway_log4j
    )

    File(format("{params.knox_conf_dir}/topologies/default.xml"),
         group=params.knox_group,
         owner=params.knox_user,
         content=InlineTemplate(params.topology_template)
    )
    if params.security_enabled:
      TemplateConfig( format("{knox_conf_dir}/krb5JAASLogin.conf"),
                      owner = params.knox_user,
                      template_tag = None
      )
    cmd = format('chown -R {knox_user}:{knox_group} {knox_data_dir} {knox_logs_dir} {knox_pid_dir} {knox_conf_dir}')
    Execute(cmd)

    cmd = format('{knox_client_bin} create-master --master {knox_master_secret!p}')
    Execute(cmd,
            user=params.knox_user,
            environment={'JAVA_HOME': params.java_home},
            not_if=format('test -f {knox_master_secret_path}')
    )

    cmd = format('{knox_client_bin} create-cert --hostname {knox_host_name_in_cluster}')
    Execute(cmd,
            user=params.knox_user,
            environment={'JAVA_HOME': params.java_home},
            not_if=format('test -f {knox_cert_store_path}')
    )

