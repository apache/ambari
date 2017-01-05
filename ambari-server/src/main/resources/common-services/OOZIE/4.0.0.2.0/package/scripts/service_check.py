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
import os
import glob

from resource_management.core.resources.system import Execute
from resource_management.core.resources import File
from resource_management.core.source import StaticFile
from resource_management.core.system import System
from resource_management.libraries.functions import format
from resource_management.libraries.script import Script
from ambari_commons.os_family_impl import OsFamilyImpl
from ambari_commons import OSConst

from resource_management.core.logger import Logger

NO_DOCS_FOLDER_MESSAGE = "Cannot find {oozie_examples_regex}. Possible reason is that /etc/yum.conf contains" \
" tsflags=nodocs which prevents this folder from being installed along with oozie-client package." \
" If this is the case, please fix /etc/yum.conf and re-install the package."

class OozieServiceCheck(Script):
  pass

@OsFamilyImpl(os_family=OsFamilyImpl.DEFAULT)
class OozieServiceCheckDefault(OozieServiceCheck):

  def service_check(self, env):
    import params
    env.set_params(params)

    # on HDP1 this file is different
    prepare_hdfs_file_name = 'prepareOozieHdfsDirectories.sh'
    smoke_test_file_name = 'oozieSmoke2.sh'

    OozieServiceCheckDefault.oozie_smoke_shell_file(smoke_test_file_name, prepare_hdfs_file_name)

  @staticmethod
  def oozie_smoke_shell_file(file_name, prepare_hdfs_file_name):
    import params

    File(format("{tmp_dir}/{file_name}"),
         content=StaticFile(file_name),
         mode=0755
    )
    File(format("{tmp_dir}/{prepare_hdfs_file_name}"),
         content=StaticFile(prepare_hdfs_file_name),
         mode=0755
    )

    os_family = System.get_instance().os_family
    oozie_examples_dir_regex_matches = glob.glob(params.oozie_examples_regex)
    if not oozie_examples_dir_regex_matches:
      raise Fail(format(NO_DOCS_FOLDER_MESSAGE))
    oozie_examples_dir = oozie_examples_dir_regex_matches[0]

    Execute((format("{tmp_dir}/{prepare_hdfs_file_name}"), params.conf_dir, oozie_examples_dir, params.hadoop_conf_dir, params.yarn_resourcemanager_address, params.fs_root, params.service_check_queue_name, params.service_check_job_name),
            tries=3,
            try_sleep=5,
            logoutput=True
    )

    params.HdfsResource(format("/user/{smokeuser}"),
        type="directory",
        action="create_on_execute",
        owner=params.smokeuser,
        mode=params.smoke_hdfs_user_mode,
        )

    examples_dir = format('/user/{smokeuser}/examples')
    params.HdfsResource(examples_dir,
                        action = "delete_on_execute",
                        type = "directory"
    )
    params.HdfsResource(examples_dir,
      action = "create_on_execute",
      type = "directory",
      source = format("{oozie_examples_dir}/examples"),
      owner = params.smokeuser,
      group = params.user_group
    )

    input_data_dir = format('/user/{smokeuser}/input-data')
    params.HdfsResource(input_data_dir,
                        action = "delete_on_execute",
                        type = "directory"
    )
    params.HdfsResource(input_data_dir,
      action = "create_on_execute",
      type = "directory",
      source = format("{oozie_examples_dir}/examples/input-data"),
      owner = params.smokeuser,
      group = params.user_group
    )
    params.HdfsResource(None, action="execute")

    if params.security_enabled:
      sh_cmd = format(
        "{tmp_dir}/{file_name} {os_family} {oozie_lib_dir} {conf_dir} {oozie_bin_dir} {oozie_base_url} {oozie_examples_dir} {hadoop_conf_dir} {hadoop_bin_dir} {smokeuser} {service_check_job_name} {security_enabled} {smokeuser_keytab} {kinit_path_local} {smokeuser_principal}")
    else:
      sh_cmd = format(
        "{tmp_dir}/{file_name} {os_family} {oozie_lib_dir} {conf_dir} {oozie_bin_dir} {oozie_base_url} {oozie_examples_dir} {hadoop_conf_dir} {hadoop_bin_dir} {smokeuser} {service_check_job_name} {security_enabled}")

    Execute(sh_cmd,
            path=params.execute_path,
            tries=3,
            try_sleep=5,
            logoutput=True
    )

@OsFamilyImpl(os_family=OSConst.WINSRV_FAMILY)
class OozieServiceCheckWindows(OozieServiceCheck):

  def service_check(self, env):
    import params

    env.set_params(params)
    smoke_cmd = os.path.join(params.stack_root, "Run-SmokeTests.cmd")
    service = "OOZIE"
    Execute(format("cmd /C {smoke_cmd} {service}"), logoutput=True)

if __name__ == "__main__":
  OozieServiceCheck().execute()

