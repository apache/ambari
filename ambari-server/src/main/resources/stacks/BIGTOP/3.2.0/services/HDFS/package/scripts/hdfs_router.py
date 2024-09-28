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
import os.path
import time

from ambari_commons import constants

from resource_management.core import shell
from resource_management.core.source import Template
from resource_management.core.resources.system import File, Execute, Directory
from resource_management.core.resources.service import Service
from resource_management.libraries.functions.decorator import retry
from resource_management.libraries.functions.default import default
from resource_management.libraries.functions.format import format
from resource_management.libraries.functions.check_process_status import check_process_status
from resource_management.libraries.resources.execute_hadoop import ExecuteHadoop
from resource_management.libraries.functions import Direction, upgrade_summary
from resource_management.libraries.functions.namenode_ha_utils import get_name_service_by_hostname
from ambari_commons import OSCheck, OSConst
from ambari_commons.os_family_impl import OsFamilyImpl, OsFamilyFuncImpl
from utils import get_dfsrouteradmin_base_command
from utils import set_up_zkfc_security

if OSCheck.is_windows_family():
  from resource_management.libraries.functions.windows_service_utils import check_windows_service_status

from resource_management.core.exceptions import Fail
from resource_management.core.logger import Logger

from utils import service, safe_zkfc_op, is_previous_fs_image
from setup_ranger_hdfs import setup_ranger_hdfs, create_ranger_audit_hdfs_directories


@OsFamilyFuncImpl(os_family=OsFamilyImpl.DEFAULT)
def router(action=None, hdfs_binary=None, env=None):

  if action is None:
    raise Fail('"action" parameter is required for function router().')

  if action in ["start", "stop"] and hdfs_binary is None:
    raise Fail('"hdfs_binary" parameter is required for function router().')

  if action == "configure":
    import params
    set_up_zkfc_security(params)
  elif action == "start":
    import params
    service(
      action="start",
      name="dfsrouter",
      user=params.hdfs_user,
      create_pid_dir=True,
      create_log_dir=True
    )

    if params.security_enabled:
      Execute(format("{kinit_path_local} -kt {hdfs_user_keytab} {hdfs_principal_name}"),
              user = params.hdfs_user)

    name_service = get_name_service_by_hostname(params.hdfs_site, params.hostname)
    ensure_safemode_off = True

  elif action == "stop":
    import params
    service(
      action="stop", name="dfsrouter",
      user=params.hdfs_user
    )
  elif action == "status":
    import status_params
    check_process_status(status_params.router_pid_file)




