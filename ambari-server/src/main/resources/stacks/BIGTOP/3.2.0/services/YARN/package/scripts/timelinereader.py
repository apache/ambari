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
from resource_management.libraries.functions import stack_select
from resource_management.libraries.functions.constants import StackFeature
from resource_management.libraries.functions.stack_features import check_stack_feature
from resource_management.libraries.functions.format import format
from resource_management.core.logger import Logger
from resource_management.core.exceptions import Fail

from yarn import yarn
from service import service
from ambari_commons.os_family_impl import OsFamilyImpl
from hbase_service import hbase, configure_hbase, rollback_hbase_roles
import yarn_process_utils


def _require_timeline_service_v2(params):
  if not params.atsv2_backend_enabled:
    raise Fail("TIMELINE_READER requires an enabled YARN Timeline Service v2")


class ApplicationTimelineReader(Script):
  def install(self, env):
    self.install_packages(env)

  def start(self, env, upgrade_type=None):
    import params

    env.set_params(params)
    self.configure(env)  # FOR SECURITY

    started_hbase_roles = ()
    if not params.use_external_hbase and not params.is_hbase_system_service_launch:
      started_hbase_roles = hbase(action="start")
    try:
      service("timelinereader", action="start")
    except Exception:
      cleanup_errors = rollback_hbase_roles(started_hbase_roles)
      if cleanup_errors:
        Logger.warning(
          "Could not roll back YARN ATS HBase after Timeline Reader start failure: "
          + "; ".join(cleanup_errors)
        )
      raise

  def stop(self, env, upgrade_type=None):
    import params

    env.set_params(params)
    _require_timeline_service_v2(params)
    cleanup_errors = []
    try:
      service("timelinereader", action="stop")
    except Exception as error:
      cleanup_errors.append(("timeline reader", error))
    if not params.use_external_hbase and not params.is_hbase_system_service_launch:
      try:
        hbase(action="stop")
      except Exception as error:
        cleanup_errors.append(("embedded HBase", error))
    if cleanup_errors:
      raise RuntimeError(
        "Failed to stop timeline service processes: "
        + "; ".join(f"{label}: {error}" for label, error in cleanup_errors)
      ) from cleanup_errors[0][1]

  def configure(self, env, action=None):
    import params

    env.set_params(params)
    _require_timeline_service_v2(params)
    yarn(name="apptimelinereader")
    if not params.use_external_hbase and not params.is_hbase_system_service_launch:
      configure_hbase(env)


@OsFamilyImpl(os_family=OsFamilyImpl.DEFAULT)
class ApplicationTimelineReaderDefault(ApplicationTimelineReader):
  def pre_upgrade_restart(self, env, upgrade_type=None):
    Logger.info("Executing Stack Upgrade pre-restart")
    import params

    env.set_params(params)

    if params.version and check_stack_feature(
      StackFeature.ROLLING_UPGRADE, params.version
    ):
      stack_select.select_packages(params.version)

  def status(self, env):
    import params

    env.set_params(params)
    _require_timeline_service_v2(params)
    processes = [
      (params.yarn_timelinereader_pid_file, params.yarn_user, "timelinereader")
    ]
    if not params.use_external_hbase and not params.is_hbase_system_service_launch:
      processes.extend(
        (
          (
            format("{yarn_hbase_pid_dir}/hbase-{yarn_hbase_user}-master.pid"),
            params.yarn_hbase_user,
            "master",
          ),
          (
            format("{yarn_hbase_pid_dir}/hbase-{yarn_hbase_user}-regionserver.pid"),
            params.yarn_hbase_user,
            "regionserver",
          ),
        )
      )
    for pid_file, expected_user, component in processes:
      yarn_process_utils.check_component_status(
        pid_file,
        expected_user,
        component,
        expected_user,
        params.user_group,
      )

  def get_log_folder(self):
    import params

    return params.yarn_log_dir

  def get_user(self):
    import params

    return params.yarn_user

  def get_pid_files(self):
    import params

    _require_timeline_service_v2(params)
    pid_files = []
    pid_files.append(format("{yarn_timelinereader_pid_file}"))
    if not params.use_external_hbase and not params.is_hbase_system_service_launch:
      pid_files.append(
        format("{yarn_hbase_pid_dir}/hbase-{yarn_hbase_user}-master.pid")
      )
      pid_files.append(
        format("{yarn_hbase_pid_dir}/hbase-{yarn_hbase_user}-regionserver.pid")
      )
    return pid_files


if __name__ == "__main__":
  ApplicationTimelineReader().execute()
