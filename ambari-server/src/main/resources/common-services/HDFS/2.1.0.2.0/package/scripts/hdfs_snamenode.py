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

from utils import service
from resource_management.core.resources.system import Directory, File
from resource_management.core.source import Template
from resource_management.libraries.functions.check_process_status import check_process_status
from ambari_commons.os_family_impl import OsFamilyImpl, OsFamilyFuncImpl
from ambari_commons import OSConst

@OsFamilyFuncImpl(os_family=OsFamilyImpl.DEFAULT)
def snamenode(action=None, format=False):
  if action == "configure":
    import params
    for fs_checkpoint_dir in params.fs_checkpoint_dirs:
      Directory(fs_checkpoint_dir,
                create_parents = True,
                cd_access="a",
                mode=0755,
                owner=params.hdfs_user,
                group=params.user_group)
    File(params.exclude_file_path,
         content=Template("exclude_hosts_list.j2"),
         owner=params.hdfs_user,
         group=params.user_group)
    if params.hdfs_include_file:
      File(params.include_file_path,
         content=Template("include_hosts_list.j2"),
         owner=params.hdfs_user,
         group=params.user_group)
      pass
  elif action == "start" or action == "stop":
    import params
    service(
      action=action,
      name="secondarynamenode",
      user=params.hdfs_user,
      create_pid_dir=True,
      create_log_dir=True
    )
  elif action == "status":
    import status_params
    check_process_status(status_params.snamenode_pid_file)


@OsFamilyFuncImpl(os_family=OSConst.WINSRV_FAMILY)
def snamenode(action=None, format=False):
  if action == "configure":
    pass
  elif action == "start" or action == "stop":
    import params
    Service(params.snamenode_win_service_name, action=action)
  elif action == "status":
    import status_params
    check_windows_service_status(status_params.snamenode_win_service_name)

