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

from resource_management.libraries.functions.show_logs import show_logs
from resource_management.core.resources.system import Execute
from resource_management.core.logger import Logger
from ranger_process import (
  find_process,
  rollback_started_process,
  secure_started_process,
  stop_process,
)


def ranger_service(name, action=None):
  import params

  env_dict = {"JAVA_HOME": params.java_home}
  if params.db_flavor.lower() == "sqla":
    env_dict = {"JAVA_HOME": params.java_home, "LD_LIBRARY_PATH": params.ld_lib_path}

  component = {
    "ranger_admin": (
      params.ranger_admin_services_file,
      params.ranger_admin_pid_file,
      params.admin_log_dir,
    ),
    "ranger_usersync": (
      params.usersync_services_file,
      params.ranger_usersync_pid_file,
      params.usersync_log_dir,
    ),
    "ranger_tagsync": (
      params.tagsync_services_file,
      params.tagsync_pid_file,
      params.tagsync_log_dir,
    ),
  }.get(name)
  if component is None:
    raise ValueError(f"Unsupported Ranger component {name}")
  if name == "ranger_tagsync" and not params.stack_supports_ranger_tagsync:
    return

  service_file, pid_file, log_dir = component
  if action == "stop":
    stop_process(name, pid_file, params.unix_user, params.unix_group)
    return
  if action not in (None, "start"):
    raise ValueError(f"Unsupported Ranger service action {action}")
  if find_process(name, pid_file, params.unix_user, params.unix_group) is not None:
    return

  try:
    Execute(
      (service_file, "start"),
      environment=env_dict,
      user=params.unix_user,
      timeout=60,
    )
    secure_started_process(name, pid_file, params.unix_user, params.unix_group)
  except Exception:
    try:
      rollback_started_process(name, pid_file, params.unix_user)
    except Exception as cleanup_error:
      Logger.warning(f"Could not roll back failed {name} start: {cleanup_error}")
    show_logs(log_dir, params.unix_user)
    raise
