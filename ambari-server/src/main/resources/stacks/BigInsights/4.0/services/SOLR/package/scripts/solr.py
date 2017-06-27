#!/usr/bin/env python
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
from resource_management.libraries.functions import conf_select
import sys
import os

def solr(type = None, upgrade_type=None):
  import params

  if type == 'server':
    effective_version = params.iop_stack_version if upgrade_type is None else format_stack_version(params.version)

    params.HdfsResource(params.solr_hdfs_home_dir,
                         type="directory",
                         action="create_on_execute",
                         owner=params.solr_user,
                         mode=params.solr_hdfs_user_mode
    )
    params.HdfsResource(None, action="execute")

    Directory([params.log_dir,params.pid_dir,params.solr_conf_dir],
              mode=0755,
              cd_access='a',
              owner=params.solr_user,
              create_parents=True,
              group=params.user_group
      )

    XmlConfig("solr-site.xml",
              conf_dir=params.solr_conf_dir,
              configurations=params.solr_site,
              configuration_attributes=params.config['configuration_attributes']['solr-site'],
              owner=params.solr_user,
              group=params.user_group,
              mode=0644
    )

    File(format("{solr_conf_dir}/solr.in.sh"),
         content=InlineTemplate(params.solr_in_sh_template),
         owner=params.solr_user,
         group=params.user_group
    )

    File(format("{solr_conf_dir}/log4j.properties"),
           mode=0644,
           group=params.user_group,
           owner=params.solr_user,
           content=params.log4j_props
    )

    File(format("{solr_conf_dir}/log4j.properties"),
           mode=0644,
           group=params.user_group,
           owner=params.solr_user,
           content=params.log4j_props
    )

    Directory(params.lib_dir,
              mode=0755,
              cd_access='a',
              owner=params.solr_user,
              create_parents=True,
              group=params.user_group
    )

    if effective_version is not None and effective_version != "" and compare_versions(effective_version, '4.2.0.0') >= 0:
      File(format("{lib_dir}/solr.xml"),
              mode=0644,
              group=params.user_group,
              owner=params.solr_user,
              content=Template("solr.xml.j2")
      )
    else:
      Directory(format("{lib_dir}/data"),
              owner=params.solr_user,
              create_parents=True,
              group=params.user_group
      )

      File(format("{lib_dir}/data/solr.xml"),
              mode=0644,
              group=params.user_group,
              owner=params.solr_user,
              content=Template("solr.xml.j2")
      )

    #solr-webapp is temp dir, need to own by solr in order for it to wirte temp files into.
    Directory(format("{solr_home}/server/solr-webapp"),
              owner=params.solr_user,
              create_parents=True,
    )

    if params.security_enabled:
      File(format("{solr_jaas_file}"),
           content=Template("solr_jaas.conf.j2"),
           owner=params.solr_user)

  elif type == '4103':
    solr41_conf_dir = "/usr/iop/4.1.0.0/solr/conf"
    solr41_etc_dir="/etc/solr/4.1.0.0/0"
    if not os.path.exists(solr41_etc_dir):
      Execute("mkdir -p /etc/solr/4.1.0.0/0")

    content_path=solr41_conf_dir
    if not os.path.isfile("/usr/iop/4.1.0.0/solr/conf/solr.in.sh"):
      content_path = "/etc/solr/conf.backup"

    for each in os.listdir(content_path):
      File(os.path.join(solr41_etc_dir, each),
           owner=params.solr_user,
           content = StaticFile(os.path.join(content_path,each)))

    if not os.path.islink(solr41_conf_dir):
      Directory(solr41_conf_dir,
                action="delete",
                create_parents=True)

    if os.path.islink(solr41_conf_dir):
      os.unlink(solr41_conf_dir)

    if not os.path.islink(solr41_conf_dir):
      Link(solr41_conf_dir,
           to=solr41_etc_dir
      )
