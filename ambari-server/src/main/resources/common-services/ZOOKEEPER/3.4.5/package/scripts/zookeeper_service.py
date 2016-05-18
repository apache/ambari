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
from ambari_commons import OSConst
from ambari_commons.os_family_impl import OsFamilyFuncImpl, OsFamilyImpl
from resource_management.libraries.functions import conf_select
from resource_management.libraries.functions import stack_select
from resource_management.libraries.functions import StackFeature
from resource_management.libraries.functions.version import compare_versions, format_stack_version
from resource_management.libraries.functions.stack_features import check_stack_feature
from resource_management.libraries.functions.show_logs import show_logs

@OsFamilyFuncImpl(os_family=OsFamilyImpl.DEFAULT)
def zookeeper_service(action='start', upgrade_type=None):
  import params

  # This path may be missing after Ambari upgrade. We need to create it.
  if upgrade_type is None and not os.path.exists(os.path.join(params.stack_root,"/current/zookeeper-server")) and params.current_version \
    and check_stack_feature(StackFeature.ROLLING_UPGRADE, format_stack_version(params.version)):
    conf_select.select(params.stack_name, "zookeeper", params.current_version)
    stack_select.select("zookeeper-server", params.version)

  cmd = format("env ZOOCFGDIR={config_dir} ZOOCFG=zoo.cfg {zk_bin}/zkServer.sh")

  if action == 'start':
    daemon_cmd = format("source {config_dir}/zookeeper-env.sh ; {cmd} start")
    no_op_test = format("ls {zk_pid_file} >/dev/null 2>&1 && ps -p `cat {zk_pid_file}` >/dev/null 2>&1")
    
    try:
      Execute(daemon_cmd,
              not_if=no_op_test,
              user=params.zk_user
      )
    except:
      show_logs(params.zk_log_dir, params.zk_user)
      raise

    if params.security_enabled:
      kinit_cmd = format("{kinit_path_local} -kt {smoke_user_keytab} {smokeuser_principal};")

      Execute(kinit_cmd,
              user=params.smokeuser
      )

  elif action == 'stop':
    daemon_cmd = format("source {config_dir}/zookeeper-env.sh ; {cmd} stop")
    try:
      Execute(daemon_cmd,
              user=params.zk_user
      )
    except:
      show_logs(params.zk_log_dir, params.zk_user)
      raise
    File(params.zk_pid_file, action="delete")

@OsFamilyFuncImpl(os_family=OSConst.WINSRV_FAMILY)
def zookeeper_service(action='start', rolling_restart=False):
  import params
  if action == 'start':
    Service(params.zookeeper_win_service_name, action="start")
  elif action == 'stop':
    Service(params.zookeeper_win_service_name, action="stop")