
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
import re
import socket

from resource_management.core import shell
from resource_management.core.exceptions import ComponentIsNotRunning
from resource_management.core.exceptions import Fail
from resource_management.core.logger import Logger
from resource_management.libraries.functions import conf_select, stack_select
from resource_management.libraries.functions.constants import StackFeature
from resource_management.libraries.functions.stack_features import check_stack_feature
from resource_management.libraries.functions.decorator import retry
from resource_management.libraries.functions.format import format
from resource_management.libraries.functions import check_process_status


def prestart(env, stack_component):
  import params

  if params.version and check_stack_feature(StackFeature.ROLLING_UPGRADE, params.version):
    conf_select.select(params.stack_name, "hbase", params.version)
    stack_select.select(stack_component, params.version)

def post_regionserver(env):
  import params
  env.set_params(params)

  check_cmd = "echo 'status \"simple\"' | {0} shell".format(params.hbase_cmd)

  exec_cmd = "{0} {1}".format(params.kinit_cmd, check_cmd)
  is_regionserver_registered(exec_cmd, params.hbase_user, params.hostname, re.IGNORECASE)


def is_region_server_process_running():
  try:
    pid_file = format("{pid_dir}/hbase-{hbase_user}-regionserver.pid")
    check_process_status(pid_file)
    return True
  except ComponentIsNotRunning:
    return False


@retry(times=30, sleep_time=30, err_class=Fail)
def is_regionserver_registered(cmd, user, hostname, regex_search_flags):
  """
  Queries HBase through the HBase shell to see which servers have successfully registered. This is
  useful in cases, such as upgrades, where we must ensure that a RegionServer has not only started,
  but also completed it's registration handshake before moving into upgrading the next RegionServer.

  The hbase shell is used along with the "show 'simple'" command in order to determine if the
  specified host has registered.
  :param cmd:
  :param user:
  :param hostname:
  :param regex_search_flags:
  :return:
  """
  if not is_region_server_process_running():
    Logger.info("RegionServer process is not running")
    raise Fail("RegionServer process is not running")

  # use hbase shell with "status 'simple'" command
  code, out = shell.call(cmd, user=user)

  # if we don't have ouput, then we can't check
  if not out:
    raise Fail("Unable to retrieve status information from the HBase shell")

  # try matching the hostname with a colon (which indicates a bound port)
  bound_hostname_to_match = hostname + ":"
  match = re.search(bound_hostname_to_match, out, regex_search_flags)

  # if there's no match, try again with the IP address
  if not match:
    try:
      ip_address = socket.gethostbyname(hostname)
      bound_ip_address_to_match = ip_address + ":"
      match = re.search(bound_ip_address_to_match, out, regex_search_flags)
    except socket.error:
      # this is merely a backup, so just log that it failed
      Logger.warning("Unable to lookup the IP address of {0}, reverse DNS lookup may not be working.".format(hostname))
      pass

  # failed with both a hostname and an IP address, so raise the Fail and let the function auto retry
  if not match:
    raise Fail(
      "The RegionServer named {0} has not yet registered with the HBase Master".format(hostname))
