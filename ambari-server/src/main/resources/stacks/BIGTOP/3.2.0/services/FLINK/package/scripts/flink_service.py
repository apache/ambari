#!/usr/bin/env python

'''
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
'''
import os
import shutil
import glob

from resource_management.libraries.script.script import Script
from resource_management.libraries.resources.hdfs_resource import HdfsResource
from resource_management.libraries.functions.copy_tarball import copy_to_hdfs, get_tarball_paths
from resource_management.libraries.functions import format
from resource_management.core.resources.system import File, Execute
from resource_management.libraries.functions.version import format_stack_version
from resource_management.libraries.functions.stack_features import check_stack_feature
from resource_management.libraries.functions.check_process_status import check_process_status
from resource_management.libraries.functions.constants import StackFeature
from resource_management.libraries.functions.show_logs import show_logs
from resource_management.core.shell import as_sudo
from resource_management.core.exceptions import ComponentIsNotRunning
from resource_management.core.logger import Logger

def flink_service(name, upgrade_type=None, action=None):
  import params

  if action == 'start':
    if name == 'historyserver':
      # create flink history directory
      params.HdfsResource(params.jobmanager_archive_fs_dir,
                          type="directory",
                          action="create_on_execute",
                          owner=params.flink_user,
                          group=params.user_group,
                          mode=0777,
                          recursive_chmod=True
                          )
      params.HdfsResource(None, action="execute")

      historyserver_no_op_test = as_sudo(["test", "-f", params.flink_history_server_pid_file]) + " && " + as_sudo(["pgrep", "-F", params.flink_history_server_pid_file])
      try:
        Execute(params.flink_history_server_start,
                user=params.flink_user,
                environment={'JAVA_HOME': params.java_home},
                not_if=historyserver_no_op_test)
      except:
        show_logs(params.flink_log_dir, user=params.flink_user)
        raise

  elif action == 'stop':
    if name == 'historyserver':
      try:
        Execute(format('{flink_history_server_stop}'),
                user=params.flink_user,
                environment={'JAVA_HOME': params.java_home}
        )
      except:
        show_logs(params.flink_log_dir, user=params.flink_user)
        raise

      File(params.flink_history_server_pid_file,
        action="delete"
      )