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
from resource_management.libraries.script.script import Script
from resource_management.libraries.resources.xml_config import XmlConfig
from resource_management.core.resources.service import ServiceConfig
from resource_management.libraries.functions.format import format
from resource_management.libraries.functions.get_config import get_config
from resource_management.libraries.resources.template_config import TemplateConfig
from resource_management.core.resources.system import File, Execute, Directory
from resource_management.core.shell import as_user
from resource_management.core.source import InlineTemplate

from ambari_commons import OSConst
from ambari_commons.os_family_impl import OsFamilyFuncImpl, OsFamilyImpl

from resource_management.libraries.functions.stack_features import check_stack_feature
from resource_management.libraries.functions import StackFeature

@OsFamilyFuncImpl(os_family=OSConst.WINSRV_FAMILY)
def knox():
  import params

  XmlConfig("gateway-site.xml",
            conf_dir=params.knox_conf_dir,
            configurations=params.config['configurations']['gateway-site'],
            configuration_attributes=params.config['configuration_attributes']['gateway-site'],
            owner=params.knox_user
  )

  # Manually overriding service logon user & password set by the installation package
  ServiceConfig(params.knox_gateway_win_service_name,
                action="change_user",
                username = params.knox_user,
                password = Script.get_password(params.knox_user))

  File(os.path.join(params.knox_conf_dir, "gateway-log4j.properties"),
       owner=params.knox_user,
       content=params.gateway_log4j
  )

  File(os.path.join(params.knox_conf_dir, "topologies", "default.xml"),
       group=params.knox_group,
       owner=params.knox_user,
       content=InlineTemplate(params.topology_template)
  )

  if params.admin_topology_template:
    File(os.path.join(params.knox_conf_dir, "topologies", "admin.xml"),
       group=params.knox_group,
       owner=params.knox_user,
       content=InlineTemplate(params.admin_topology_template)
    )

  if params.version_formatted and check_stack_feature(StackFeature.KNOX_SSO_TOPOLOGY, params.version_formatted):
    knoxsso_topology_template_content = get_config("knoxsso-topology")
    if knoxsso_topology_template_content:
      File(os.path.join(params.knox_conf_dir, "topologies", "knoxsso.xml"),
        group=params.knox_group,
        owner=params.knox_user,
        content=InlineTemplate(params.knoxsso_topology_template)
      )

  if params.security_enabled:
    TemplateConfig( os.path.join(params.knox_conf_dir, "krb5JAASLogin.conf"),
        owner = params.knox_user,
        template_tag = None
    )

  if not os.path.isfile(params.knox_master_secret_path):
    cmd = format('cmd /C {knox_client_bin} create-master --master {knox_master_secret!p}')
    Execute(cmd)
    cmd = format('cmd /C {knox_client_bin} create-cert --hostname {knox_host_name_in_cluster}')
    Execute(cmd)

@OsFamilyFuncImpl(os_family=OsFamilyImpl.DEFAULT)
def knox():
    import params
    Directory([params.knox_data_dir, params.knox_logs_dir, params.knox_pid_dir, params.knox_conf_dir, os.path.join(params.knox_conf_dir, "topologies")],
              owner = params.knox_user,
              group = params.knox_group,
              create_parents = True,
              cd_access = "a",
              mode = 0755,
              recursive_ownership = True,
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
         content=InlineTemplate(params.gateway_log4j)
    )

    File(format("{params.knox_conf_dir}/topologies/default.xml"),
         group=params.knox_group,
         owner=params.knox_user,
         content=InlineTemplate(params.topology_template)
    )

    if params.admin_topology_template:
      File(format("{params.knox_conf_dir}/topologies/admin.xml"),
           group=params.knox_group,
           owner=params.knox_user,
           content=InlineTemplate(params.admin_topology_template)
      )

    if params.version_formatted and check_stack_feature(StackFeature.KNOX_SSO_TOPOLOGY, params.version_formatted):
      knoxsso_topology_template_content = get_config("knoxsso-topology")
      if knoxsso_topology_template_content:
        File(os.path.join(params.knox_conf_dir, "topologies", "knoxsso.xml"),
            group=params.knox_group,
            owner=params.knox_user,
            content=InlineTemplate(params.knoxsso_topology_template)
        )

    if params.security_enabled:
      TemplateConfig( format("{knox_conf_dir}/krb5JAASLogin.conf"),
                      owner = params.knox_user,
                      template_tag = None
      )

    cmd = format('{knox_client_bin} create-master --master {knox_master_secret!p}')
    master_secret_exist = as_user(format('test -f {knox_master_secret_path}'), params.knox_user)

    Execute(cmd,
            user=params.knox_user,
            environment={'JAVA_HOME': params.java_home},
            not_if=master_secret_exist,
    )

    cmd = format('{knox_client_bin} create-cert --hostname {knox_host_name_in_cluster}')
    cert_store_exist = as_user(format('test -f {knox_cert_store_path}'), params.knox_user)

    Execute(cmd,
            user=params.knox_user,
            environment={'JAVA_HOME': params.java_home},
            not_if=cert_store_exist,
    )


@OsFamilyFuncImpl(os_family=OSConst.WINSRV_FAMILY)
def update_knox_folder_permissions():
  import params
  Directory(params.knox_logs_dir,
            owner = params.knox_user,
            group = params.knox_group
            )


@OsFamilyFuncImpl(os_family=OsFamilyImpl.DEFAULT)
def update_knox_logfolder_permissions():
  """
   Fix for the bug with rpm/deb packages. During installation of the package, they re-apply permissions to the
   folders below; such behaviour will affect installations with non-standard user name/group and will put
   cluster in non-working state
  """
  import params
  
  Directory(params.knox_logs_dir,
            owner = params.knox_user,
            group = params.knox_group,
            create_parents = True,
            cd_access = "a",
            mode = 0755,
            recursive_ownership = True,
  )
