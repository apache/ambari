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
from resource_management.core.shell import call

HAWQ_USER = 'gpadmin'
HAWQ_GREENPLUM_PATH_FILE = '/usr/local/hawq/greenplum_path.sh'

HAWQMASTER_PORT = '{{hawq-site/hawq_master_address_port}}'
HAWQSTANDBY_ADDRESS = '{{hawq-site/hawq_standby_address_host}}'

RESULT_STATE_OK = 'OK'
RESULT_STATE_WARNING = 'WARNING'
RESULT_STATE_UNKNOWN = 'UNKNOWN'
RESULT_STATE_SKIPPED = 'SKIPPED'

logger = logging.getLogger('ambari_alerts')


def get_tokens():
  """
  Returns a tuple of tokens in the format {{site/property}} that will be used to build the dictionary passed into execute
  """
  return (HAWQMASTER_PORT, HAWQSTANDBY_ADDRESS)
  

def execute(configurations={}, parameters={}, host_name=None):
  """
  Returns a tuple containing the result code and a pre-formatted result label

  Keyword arguments:
  configurations (dictionary): a mapping of configuration key to value
  parameters (dictionary): a mapping of script parameter key to value
  host_name (string): the name of this host where the alert is running
  """

  if configurations is None:
    return (RESULT_STATE_UNKNOWN, ['There were no configurations supplied to the script.'])

  # If HAWQSTANDBY is not installed on the cluster
  if HAWQSTANDBY_ADDRESS not in configurations:
   return (RESULT_STATE_SKIPPED, ['HAWQSTANDBY is not installed.'])

  try:
    sync_status = get_sync_status(configurations[HAWQMASTER_PORT])
    if sync_status in ('Synchronized', 'Synchronizing'):
      return (RESULT_STATE_OK, ['HAWQSTANDBY is in sync with HAWQMASTER.'])
    elif sync_status == 'Not Synchronized':
      return (RESULT_STATE_WARNING, ['HAWQSTANDBY is not in sync with HAWQMASTER.'])
  except Exception, e:
    logger.exception('[Alert] Retrieving HAWQSTANDBY sync status from HAWQMASTER fails on host, {0}:'.format(host_name))
    logger.exception(str(e))

  # Sync status cannot be determined
  return (RESULT_STATE_UNKNOWN, ['Sync status cannot be determined.'])


def get_sync_status(port):
  """
  Gets the sync status of HAWQSTANDBY from HAWQMASTER by running a SQL command.
  summary_state can be of the following values: ('Synchronized', 'Synchronizing', 'Not Synchronized', 'None', 'Not Configured', 'Unknown')
  """
  query = "SELECT summary_state FROM gp_master_mirroring"
  cmd = "source {0} && psql -p {1} -t -d template1 -c \"{2};\"".format(HAWQ_GREENPLUM_PATH_FILE, port, query)

  returncode, output = call(cmd,
                            user=HAWQ_USER,
                            timeout=60)

  if returncode:
    raise

  return output.strip()
