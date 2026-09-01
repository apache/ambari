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

import os

from ambari_commons.os_family_impl import OsFamilyFuncImpl, OsFamilyImpl
from resource_management.core.exceptions import Fail

from metrics_process import (
  check_ams_process_status,
  check_hbase_process_status,
  hbase_pid_file,
)


def get_collector_pid_files(status_params=None):
  if status_params is None:
    import status_params

  pid_files = [
    os.path.join(
      status_params.ams_collector_pid_dir, "ambari-metrics-collector.pid"
    ),
    hbase_pid_file(
      status_params.hbase_pid_dir, status_params.hbase_user, "master"
    ),
  ]
  if status_params.is_hbase_distributed:
    pid_files.append(
      hbase_pid_file(
        status_params.hbase_pid_dir,
        status_params.hbase_user,
        "regionserver",
      )
    )
  return pid_files


@OsFamilyFuncImpl(os_family=OsFamilyImpl.DEFAULT)
def check_service_status(env, name):
  import status_params

  env.set_params(status_params)

  if name == "collector":
    collector_pid_file = os.path.join(
      status_params.ams_collector_pid_dir, "ambari-metrics-collector.pid"
    )
    check_ams_process_status(
      collector_pid_file,
      status_params.ams_user,
      status_params.user_group,
      "collector",
    )
    check_hbase_process_status(
      hbase_pid_file(
        status_params.hbase_pid_dir, status_params.hbase_user, "master"
      ),
      status_params.hbase_user,
      status_params.user_group,
      "master",
    )
    if status_params.is_hbase_distributed:
      check_hbase_process_status(
        hbase_pid_file(
          status_params.hbase_pid_dir,
          status_params.hbase_user,
          "regionserver",
        ),
        status_params.hbase_user,
        status_params.user_group,
        "regionserver",
      )
  elif name == "monitor":
    check_ams_process_status(
      status_params.monitor_pid_file,
      status_params.ams_user,
      status_params.user_group,
      "monitor",
    )
  elif name == "grafana":
    check_ams_process_status(
      status_params.grafana_pid_file,
      status_params.ams_user,
      status_params.user_group,
      "grafana",
    )
  else:
    raise Fail(f"Unsupported Ambari Metrics status component: {name}")
