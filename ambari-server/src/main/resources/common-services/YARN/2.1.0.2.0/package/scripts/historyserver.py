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

from resource_management.libraries.script.script import Script
from resource_management.libraries.resources.hdfs_resource import HdfsResource
from resource_management.libraries.functions import stack_select
from resource_management.libraries.functions.constants import StackFeature
from resource_management.libraries.functions.stack_features import check_stack_feature
from resource_management.libraries.functions.check_process_status import check_process_status
from resource_management.libraries.functions.copy_tarball import copy_to_hdfs
from resource_management.libraries.functions.security_commons import build_expectations, \
  cached_kinit_executor, get_params_from_filesystem, validate_security_config_properties, \
  FILE_TYPE_XML
from resource_management.core.source import Template
from resource_management.core.logger import Logger

from install_jars import install_tez_jars
from yarn import yarn
from service import service
from ambari_commons import OSConst
from ambari_commons.os_family_impl import OsFamilyImpl


class HistoryServer(Script):
  def install(self, env):
    self.install_packages(env)

  def stop(self, env, upgrade_type=None):
    import params
    env.set_params(params)
    service('historyserver', action='stop', serviceName='mapreduce')

  def configure(self, env):
    import params
    env.set_params(params)
    yarn(name="historyserver")


@OsFamilyImpl(os_family=OSConst.WINSRV_FAMILY)
class HistoryserverWindows(HistoryServer):
  def start(self, env):
    import params
    env.set_params(params)
    self.configure(env)
    service('historyserver', action='start', serviceName='mapreduce')

  def status(self, env):
    service('historyserver', action='status')


@OsFamilyImpl(os_family=OsFamilyImpl.DEFAULT)
class HistoryServerDefault(HistoryServer):
  def pre_upgrade_restart(self, env, upgrade_type=None):
    Logger.info("Executing Stack Upgrade pre-restart")
    import params
    env.set_params(params)

    if params.version and check_stack_feature(StackFeature.ROLLING_UPGRADE, params.version):
      stack_select.select_packages(params.version)
      # MC Hammer said, "Can't touch this"
      copy_to_hdfs("mapreduce", params.user_group, params.hdfs_user, skip=params.sysprep_skip_copy_tarballs_hdfs)
      copy_to_hdfs("tez", params.user_group, params.hdfs_user, skip=params.sysprep_skip_copy_tarballs_hdfs)
      copy_to_hdfs("slider", params.user_group, params.hdfs_user, skip=params.sysprep_skip_copy_tarballs_hdfs)
      params.HdfsResource(None, action="execute")

  def start(self, env, upgrade_type=None):
    import params
    env.set_params(params)
    self.configure(env) # FOR SECURITY

    if check_stack_feature(StackFeature.COPY_TARBALL_TO_HDFS, params.version_for_stack_feature_checks):
      # MC Hammer said, "Can't touch this"
      resource_created = copy_to_hdfs(
        "mapreduce",
        params.user_group,
        params.hdfs_user,
        skip=params.sysprep_skip_copy_tarballs_hdfs)
      resource_created = copy_to_hdfs(
        "tez",
        params.user_group,
        params.hdfs_user,
        skip=params.sysprep_skip_copy_tarballs_hdfs) or resource_created
      resource_created = copy_to_hdfs(
        "slider",
        params.user_group,
        params.hdfs_user,
        skip=params.sysprep_skip_copy_tarballs_hdfs) or resource_created
      if resource_created:
        params.HdfsResource(None, action="execute")
    else:
      # In stack versions before copy_tarball_to_hdfs support tez.tar.gz was copied to a different folder in HDFS.
      install_tez_jars()

    service('historyserver', action='start', serviceName='mapreduce')

  def status(self, env):
    import status_params
    env.set_params(status_params)
    check_process_status(status_params.mapred_historyserver_pid_file)

  def get_log_folder(self):
    import params
    return params.mapred_log_dir

  def get_user(self):
    import params
    return params.mapred_user

  def get_pid_files(self):
    import status_params
    return [status_params.mapred_historyserver_pid_file]

if __name__ == "__main__":
  HistoryServer().execute()
