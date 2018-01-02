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

from resource_management.libraries.functions import format
from ambari_commons import OSConst
from ambari_commons.os_family_impl import OsFamilyFuncImpl, OsFamilyImpl
import os

def get_collector_pid_files():
  pid_files = []
  pid_files.append(format("{ams_collector_pid_dir}/ambari-metrics-collector.pid"))
  pid_files.append(format("{hbase_pid_dir}/hbase-{hbase_user}-master.pid"))
  if os.path.exists(format("{hbase_pid_dir}/distributed_mode")):
    pid_files.append(format("{hbase_pid_dir}/hbase-{hbase_user}-regionserver.pid"))
  return pid_files

@OsFamilyFuncImpl(os_family=OsFamilyImpl.DEFAULT)
def check_service_status(env, name):
  import status_params
  env.set_params(status_params)

  from resource_management.libraries.functions.check_process_status import check_process_status
  if name=='collector':
    for pid_file in get_collector_pid_files():
      check_process_status(pid_file)
  elif name == 'monitor':
    check_process_status(status_params.monitor_pid_file)
  elif name == 'grafana':
    check_process_status(status_params.grafana_pid_file)

@OsFamilyFuncImpl(os_family=OSConst.WINSRV_FAMILY)
def check_service_status(name):
  import service_mapping
  from resource_management.libraries.functions.windows_service_utils import check_windows_service_status
  if name=='collector':
    check_windows_service_status(service_mapping.ams_collector_win_service_name)
  elif name == 'monitor':
    check_windows_service_status(service_mapping.ams_monitor_win_service_name)
