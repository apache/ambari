
#!/usr/bin/env python
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
from resource_management import *
from resource_management.core import shell
from resource_management.libraries.functions import stack_select
from resource_management.libraries.functions import StackFeature
from resource_management.libraries.functions.stack_features import check_stack_feature
from resource_management.libraries.functions.decorator import retry
from resource_management.libraries.functions import check_process_status

def prestart(env):
  import params

  if params.version and check_stack_feature(StackFeature.ROLLING_UPGRADE, params.version):
    stack_select.select_packages(params.version)

def post_regionserver(env):
  import params
  env.set_params(params)

  check_cmd = "echo 'status \"simple\"' | {0} shell".format(params.hbase_cmd)

  exec_cmd = "{0} {1}".format(params.kinit_cmd, check_cmd)
  call_and_match(exec_cmd, params.hbase_user, params.hostname + ":", re.IGNORECASE)


def is_region_server_process_running():
  try:
    pid_file = format("{pid_dir}/hbase-{hbase_user}-regionserver.pid")
    check_process_status(pid_file)
    return True
  except ComponentIsNotRunning:
    return False

@retry(times=30, sleep_time=30, err_class=Fail) # keep trying for 15 mins
def call_and_match(cmd, user, regex, regex_search_flags):

  if not is_region_server_process_running():
    Logger.info("RegionServer process is not running")
    raise Fail("RegionServer process is not running")

  code, out = shell.call(cmd, user=user)

  if not (out and re.search(regex, out, regex_search_flags)):
    raise Fail("Could not verify RS available")
