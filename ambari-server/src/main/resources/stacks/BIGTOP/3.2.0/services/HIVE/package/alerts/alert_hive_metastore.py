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

import logging
import socket
import time
import traceback
from urllib.parse import urlparse

OK_MESSAGE = "Metastore TCP OK - connection completed in {0:.3f}s"
CRITICAL_MESSAGE = "Metastore on {0} failed ({1})"
HIVE_METASTORE_URIS_KEY = "{{hive-site/hive.metastore.uris}}"

CHECK_COMMAND_TIMEOUT_KEY = "check.command.timeout"
CHECK_COMMAND_TIMEOUT_DEFAULT = 60.0

logger = logging.getLogger("ambari_alerts")


def get_tokens():
  """
  Returns a tuple of tokens in the format {{site/property}} that will be used
  to build the dictionary passed into execute
  """
  return (
    HIVE_METASTORE_URIS_KEY,
  )


def execute(configurations={}, parameters={}, host_name=None):
  """
  Returns a tuple containing the result code and a pre-formatted result label

  Keyword arguments:
  configurations (dictionary): a mapping of configuration key to value
  parameters (dictionary): a mapping of script parameter key to value
  host_name (string): the name of this host where the alert is running
  """

  if configurations is None:
    return ("UNKNOWN", ["There were no configurations supplied to the script."])

  if HIVE_METASTORE_URIS_KEY not in configurations:
    return ("UNKNOWN", ["Hive metastore uris were not supplied to the script."])

  metastore_uris = str(configurations[HIVE_METASTORE_URIS_KEY]).split(",")
  try:
    check_command_timeout = float(
      parameters.get(CHECK_COMMAND_TIMEOUT_KEY, CHECK_COMMAND_TIMEOUT_DEFAULT)
    )
  except (TypeError, ValueError) as exception:
    return ("UNKNOWN", [f"Invalid Hive metastore alert timeout: {exception}"])
  if check_command_timeout <= 0:
    return ("UNKNOWN", ["Hive metastore alert timeout must be positive"])

  result_code = None

  try:
    if host_name is None:
      host_name = socket.getfqdn()

    port = None

    for uri in metastore_uris:
      try:
        parts = urlparse(uri.strip())
        if parts.scheme == "thrift" and parts.hostname == host_name:
          port = parts.port
      except ValueError:
        continue

    if port is None:
      return ("UNKNOWN", [f"No metastore URI matched host {host_name}"])

    start_time = time.time()
    try:
      with socket.create_connection(
        (host_name, port), timeout=check_command_timeout
      ):
        pass

      total_time = time.time() - start_time
      result_code = "OK"
      label = OK_MESSAGE.format(total_time)
    except Exception:
      result_code = "CRITICAL"
      label = CRITICAL_MESSAGE.format(host_name, traceback.format_exc())

  except Exception:
    label = traceback.format_exc()
    result_code = "UNKNOWN"

  return (result_code, [label])
