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

import logging

RESULT_STATE_OK = 'OK'
RESULT_STATE_WARNING = 'WARNING'
RESULT_STATE_CRITICAL = 'CRITICAL'
RESULT_STATE_UNKNOWN = 'UNKNOWN'

OK_MESSAGE = 'Ok'

DFS_DATA_DIR = '{{hdfs-site/dfs.datanode.data.dir}}'
DATA_DIR_MOUNT_FILE = "/var/lib/ambari-agent/data/datanode/dfs_data_dir_mount.hist"

logger = logging.getLogger()

def get_tokens():
  """
  Returns a tuple of tokens in the format {{site/property}} that will be used
  to build the dictionary passed into execute
  """
  return (DFS_DATA_DIR, DATA_DIR_MOUNT_FILE)


def execute(configurations={}, parameters={}, host_name=None):
  """
  Returns a tuple containing the result code and a pre-formatted result label

  Keyword arguments:
  configurations (dictionary): a mapping of configuration key to value
  parameters (dictionary): a mapping of script parameter key to value
  host_name (string): the name of this host where the alert is running

  DataNode directories can be of the following formats and each needs to be supported:
    /grid/dn/archive0
    [SSD]/grid/dn/archive0
    [ARCHIVE]file:///grid/dn/archive0
  """
  warnings = []
  errors = []

  if configurations is None:
    return (RESULT_STATE_UNKNOWN, ['There were no configurations supplied to the script.'])

  # Check required properties
  if DFS_DATA_DIR not in configurations:
    return (RESULT_STATE_UNKNOWN, ['{0} is a required parameter for the script'.format(DFS_DATA_DIR)])

  dfs_data_dir = configurations[DFS_DATA_DIR]

  if dfs_data_dir is None:
    return (RESULT_STATE_UNKNOWN, ['{0} is a required parameter for the script and the value is null'.format(DFS_DATA_DIR)])

  result_code = RESULT_STATE_OK
  label = OK_MESSAGE
  return (result_code, [label])