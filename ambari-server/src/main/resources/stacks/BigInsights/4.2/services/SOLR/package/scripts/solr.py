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

    Directory([params.log_dir,params.pid_dir,params.solr_conf_dir,params.solr_data_dir],
              mode=0755,
              cd_access='a',
              owner=params.solr_user,
              create_parents = True,
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

    if effective_version is not None and effective_version != "" and compare_versions(effective_version, '4.2.0.0') >= 0:
      File(format("{solr_data_dir}/solr.xml"),
           mode=0644,
           group=params.user_group,
           owner=params.solr_user,
           content=Template("solr.xml.j2")
      )
    else:
      Directory(format("{solr_data_dir}/data"),
           owner=params.solr_user,
           create_parents = True,
           group=params.user_group
      )

      File(format("{solr_data_dir}/data/solr.xml"),
           mode=0644,
           group=params.user_group,
           owner=params.solr_user,
           content=Template("solr.xml.j2")
      )

    #solr-webapp is temp dir, need to own by solr in order for it to wirte temp files into.
    Directory(format("{solr_home}"),
              owner=params.solr_user,
              create_parents = True,
    )

    if params.security_enabled:
      File(format("{solr_jaas_file}"),
           content=Template("solr_jaas.conf.j2"),
           owner=params.solr_user)

