#!/usr/bin/env python3
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

from resource_management.core.resources.system import Execute
from resource_management.core.logger import Logger
from resource_management.libraries.functions.show_logs import show_logs
from kms_process import (
  find_process,
  rollback_started_process,
  secure_started_process,
  stop_process,
)


def kms_service(action="start", upgrade_type=None):
  import params

  env_dict = {"JAVA_HOME": params.java_home}
  if params.db_flavor.lower() == "sqla":
    env_dict = {
      "JAVA_HOME": params.java_home,
      "LD_LIBRARY_PATH": params.ld_library_path,
    }

  if action == "start":
    if find_process(
      params.ranger_kms_pid_file, params.kms_user, params.kms_group
    ) is not None:
      return
    try:
      Execute(
        (params.kms_home + "/ranger-kms", "start"),
        environment=env_dict,
        user=params.kms_user,
        timeout=60,
      )
      secure_started_process(
        params.ranger_kms_pid_file, params.kms_user, params.kms_group
      )
    except Exception:
      try:
        rollback_started_process(params.ranger_kms_pid_file, params.kms_user)
      except Exception as cleanup_error:
        Logger.warning(f"Could not roll back failed Ranger KMS start: {cleanup_error}")
      show_logs(params.kms_log_dir, params.kms_user)
      raise
  elif action == "stop":
    stop_process(params.ranger_kms_pid_file, params.kms_user, params.kms_group)
  else:
    raise ValueError(f"Unsupported Ranger KMS service action {action}")
