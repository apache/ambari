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

Ambari Agent

"""

import json
import socket
import re
import time

from resource_management import *
from resource_management.core.shell import call, checked_call
from resource_management.core.exceptions import Fail
from resource_management.core.logger import Logger
from resource_management.libraries.functions.list_ambari_managed_repos import *


# TODO, HACK
def replace_variables(cmd, host_name, version):
  if cmd:
    cmd = cmd.replace("0.0.0.0", "{host_name}")
    cmd = cmd.replace("{{version}}", "{version}")
    cmd = format(cmd)
  return cmd


class ExecuteUpgradeTasks(Script):
  """
  This script is a part of Rolling Upgrade workflow and is described at
  appropriate design doc.

  It executes tasks used for rolling upgrades.
  """

  def actionexecute(self, env):
    # Parse parameters
    config = Script.get_config()

    # TODO HACK, should be retrieved from the command.
    host_name = socket.gethostname()
    version = "2.2.0.0"

    code, out = checked_call("hdp-select")
    if code == 0 and out:
      p = re.compile(r"(2\.2\.0\.0\-\d{4})")
      m = p.search(out)
      if m and len(m.groups()) == 1:
        version = m.group(1)

    tasks = json.loads(config['roleParams']['tasks'])
    if tasks:
      for t in tasks:
        Logger.info("Task: %s" % str(t))
        command = t["command"] if "command" in t else None
        first = t["first"] if "first" in t else None
        unless = t["unless"] if "unless" in t else None
        on_failure = t["onfailure"] if "onfailure" in t else None

        # Run at most x times
        upto = None
        try:
          upto = int(t["upto"]) if "upto" in t else None
        except ValueError, e:
          Logger.warning("Could not retrieve 'upto' value from task.")

        # If upto is set, repeat every x seconds
        every = int(t["every"]) if "every" in t and upto else 0
        if every < 0:
          every = 0
        effective_times = upto if upto else 1

        # Set of return codes to ignore
        ignore_return_codes = t["ignore"] if "ignore" in t else set()
        if ignore_return_codes:
          ignore_return_codes = set([int(e) for e in ignore_return_codes.split(",")])

        if command:
          command = replace_variables(command, host_name, version)
          first = replace_variables(first, host_name, version)
          unless = replace_variables(unless, host_name, version)

          if first:
            code, out = call(first)
            Logger.info("Pre-condition command. Code: %s, Out: %s" % (str(code), str(out)))
            if code != 0:
              break

          if unless:
            code, out = call(unless)
            Logger.info("Unless command. Code: %s, Out: %s" % (str(code), str(out)))
            if code == 0:
              break

          for i in range(1, effective_times+1):
            # TODO, Execute already has a tries and try_sleep, see hdfs_namenode.py for an example
            code, out = call(command)
            Logger.info("Command. Code: %s, Out: %s" % (str(code), str(out)))

            if code == 0 or code in ignore_return_codes:
              break

            if i == effective_times:
              err_msg = Logger.get_protected_text("Execution of '%s' returned %d. %s" % (command, code, out))
              try:
                if on_failure:
                  on_failure = replace_variables(on_failure, host_name, version)
                  code_failure_handler, out_failure_handler = call(on_failure)
                  Logger.error("Failure Handler. Code: %s, Out: %s" % (str(code_failure_handler), str(out_failure_handler)))
              except:
                pass
              raise Fail(err_msg)

            if upto:
              time.sleep(every)

if __name__ == "__main__":
  ExecuteUpgradeTasks().execute()
