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

Ambari Agent

"""
import os
from resource_management import *
from resource_management.core.source import InlineTemplate
from ambari_commons.os_family_impl import OsFamilyFuncImpl, OsFamilyImpl

@OsFamilyFuncImpl(os_family=OsFamilyImpl.DEFAULT)
def titan():
    import params

    Directory(params.titan_conf_dir,
               create_parents = True,
               owner=params.titan_user,
               group=params.user_group
               )

    File(format("{params.titan_conf_dir}/titan-env.sh"),
             mode=0644,
             group=params.user_group,
             owner=params.titan_user,
             content=InlineTemplate(params.titan_env_props)
             )

    # titan-hbase-solr_properties is always set to a default even if it's not in the payload
    File(format("{params.titan_conf_dir}/titan-hbase-solr.properties"),
         mode=0644,
         group=params.user_group,
         owner=params.titan_user,
         content=InlineTemplate(params.titan_hbase_solr_props)
         )

    if (params.log4j_console_props != None):
        File(format("{params.titan_conf_dir}/log4j-console.properties"),
             mode=0644,
             group=params.user_group,
             owner=params.titan_user,
             content=InlineTemplate(params.log4j_console_props)
             )
    elif (os.path.exists(format("{params.titan_conf_dir}/log4j-console.properties"))):
        File(format("{params.titan_conf_dir}/log4j-console.properties"),
             mode=0644,
             group=params.user_group,
             owner=params.titan_user
             )
    # Change titan ext directory for multiple user access
    Directory(params.titan_ext_dir,
               create_parents = True,
               owner=params.titan_user,
               group=params.user_group,
               mode=0775
               )
