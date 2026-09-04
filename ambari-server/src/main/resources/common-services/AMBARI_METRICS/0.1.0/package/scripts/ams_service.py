# !/usr/bin/env python
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

from resource_management.core.resources.system import Directory, Execute, File
from resource_management.core.resources.service import Service
from resource_management.libraries.functions.show_logs import show_logs
from resource_management.libraries.functions.format import format
from ambari_commons import OSConst
from ambari_commons.os_family_impl import OsFamilyFuncImpl, OsFamilyImpl
from hbase_service import hbase_service
import os

@OsFamilyFuncImpl(os_family=OSConst.WINSRV_FAMILY)
def ams_service(name, action):
  import params
  if name == 'collector':
    Service(params.ams_embedded_hbase_win_service_name, action=action)
    Service(params.ams_collector_win_service_name, action=action)
  elif name == 'monitor':
    Service(params.ams_monitor_win_service_name, action=action)

@OsFamilyFuncImpl(os_family=OsFamilyImpl.DEFAULT)
def ams_service(name, action):
  import params

  if name == 'collector':
    cmd = format("{ams_collector_script} --config {ams_collector_conf_dir}")
    pid_file = format("{ams_collector_pid_dir}/ambari-metrics-collector.pid")
    #no_op_test should be much more complex to work with cumulative status of collector
    #removing as startup script handle it also
    #no_op_test = format("ls {pid_file} >/dev/null 2>&1 && ps `cat {pid_file}` >/dev/null 2>&1")

    if params.is_hbase_distributed:
      if action == 'stop':
        hbase_service('regionserver', action=action)
        hbase_service('master', action=action)
      else:
        hbase_service('master', action=action)
        hbase_service('regionserver', action=action)
      cmd = format("{cmd} --distributed")
    else:
      # make sure no residual region server process is running in embedded mode
      if action == 'stop':
        hbase_service('regionserver', action=action)

    if action == 'start':
      Execute(format("{sudo} rm -rf {hbase_tmp_dir}/*.tmp")
      )

      if not params.is_hbase_distributed:
        File(format("{ams_collector_conf_dir}/core-site.xml"),
             action='delete',
             owner=params.ams_user)

        File(format("{ams_collector_conf_dir}/hdfs-site.xml"),
             action='delete',
             owner=params.ams_user)

      if params.security_enabled:
        kinit_cmd = format("{kinit_path_local} -kt {ams_collector_keytab_path} {ams_collector_jaas_princ};")
        daemon_cmd = format("{kinit_cmd} {cmd} start")
      else:
        daemon_cmd = format("{cmd} start")

      try:
        Execute(daemon_cmd,
                user=params.ams_user
        )
      except:
        show_logs(params.ams_collector_log_dir, params.ams_user)
        raise

      pass
    elif action == 'stop':
      daemon_cmd = format("{cmd} stop")
      Execute(daemon_cmd,
              user=params.ams_user
      )

      pass
    pass
  elif name == 'monitor':
    cmd = format("{ams_monitor_script} --config {ams_monitor_conf_dir}")
    pid_file = format("{ams_monitor_pid_dir}/ambari-metrics-monitor.pid")
    no_op_test = format("ls {pid_file} >/dev/null 2>&1 && ps `cat {pid_file}` >/dev/null 2>&1")

    if action == 'start':
      daemon_cmd = format("{cmd} start")
      
      try:
        Execute(daemon_cmd,
                user=params.ams_user
        )
      except:
        show_logs(params.ams_monitor_log_dir, params.ams_user)
        raise      

      pass
    elif action == 'stop':

      daemon_cmd = format("{cmd} stop")
      Execute(daemon_cmd,
              user=params.ams_user
      )

      pass
    pass
  pass
