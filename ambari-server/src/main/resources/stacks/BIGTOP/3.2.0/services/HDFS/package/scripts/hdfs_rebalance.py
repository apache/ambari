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

import sys
from resource_management.core.exceptions import Fail
from resource_management.libraries.resources.execute_hadoop import ExecuteHadoop


def is_balancer_running():
  import params

  check_balancer_command = "fs -test -e /system/balancer.id"
  does_hdfs_file_exist = False
  try:
    _print("Checking if the balancer is running ...")
    ExecuteHadoop(
      check_balancer_command,
      user=params.hdfs_user,
      logoutput=True,
      conf_dir=params.hadoop_conf_dir,
      bin_dir=params.hadoop_bin_dir,
    )

    does_hdfs_file_exist = True
    _print("Balancer is running. ")
  except Fail:
    pass

  return does_hdfs_file_exist


def _print(line):
  sys.stdout.write(line)
  sys.stdout.flush()
