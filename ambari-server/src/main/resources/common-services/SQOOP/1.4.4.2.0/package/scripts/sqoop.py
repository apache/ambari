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

from resource_management.core.source import InlineTemplate, DownloadSource
from resource_management.libraries.functions import format
from resource_management.core.resources.system import File, Link, Directory
from ambari_commons.os_family_impl import OsFamilyFuncImpl, OsFamilyImpl
from ambari_commons import OSConst
import os


@OsFamilyFuncImpl(os_family=OSConst.WINSRV_FAMILY)
def sqoop(type=None):
  import params
  File(os.path.join(params.sqoop_conf_dir, "sqoop-env.cmd"),
       content=InlineTemplate(params.sqoop_env_cmd_template)
  )

@OsFamilyFuncImpl(os_family=OsFamilyImpl.DEFAULT)
def sqoop(type=None):
  import params
  Link(params.sqoop_lib + "/mysql-connector-java.jar",
       to = '/usr/share/java/mysql-connector-java.jar'
  )

  jdbc_connector()
  
  Directory(params.sqoop_conf_dir,
            owner = params.sqoop_user,
            group = params.user_group,
            recursive = True
  )
  File(format("{sqoop_conf_dir}/sqoop-env.sh"),
    owner=params.sqoop_user,
    group = params.user_group,
    content=InlineTemplate(params.sqoop_env_sh_template)
  )
  update_config_permissions(["sqoop-env-template.sh",
                             "sqoop-site-template.xml",
                             "sqoop-site.xml"])
  pass

def update_config_permissions(names):
  import params
  for filename in names:
    full_filename = os.path.join(params.sqoop_conf_dir, filename)
    File(full_filename,
          owner = params.sqoop_user,
          group = params.user_group,
          only_if = format("test -e {full_filename}")
    )

def jdbc_connector():
  import params
  from urllib2 import HTTPError
  from resource_management import Fail
  for jar_name in params.sqoop_jdbc_drivers_dict:
    if 'mysql-connector-java.jar' in jar_name:
      continue
    downloaded_custom_connector = format("{sqoop_lib}/{jar_name}")
    jdbc_symlink_remote = params.sqoop_jdbc_drivers_dict[jar_name]
    jdbc_driver_label = params.sqoop_jdbc_drivers_name_dict[jar_name]
    driver_curl_source = format("{jdk_location}/{jdbc_symlink_remote}")
    environment = {
      "no_proxy": format("{ambari_server_hostname}")
    }
    try:
      File(downloaded_custom_connector,
           content = DownloadSource(driver_curl_source),
           mode = 0644,
      )
    except HTTPError:
      error_string = format("Could not download {driver_curl_source}\n\
                 Please upload jdbc driver to server by run command:\n\
                 ambari-server setup --jdbc-db={jdbc_driver_label} --jdbc-driver=<PATH TO DRIVER>\n\
                 at {ambari_server_hostname}") 
      raise Fail(error_string)
                 
