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
import os.path
import re
from resource_management.core.shell import call

HAWQ_USER = 'gpadmin'
HAWQ_HOME='/usr/local/hawq'
HAWQ_GREENPLUM_PATH_FILE = "{0}/greenplum_path.sh".format(HAWQ_HOME)
HAWQ_SLAVES_FILE= "{0}/etc/slaves".format(HAWQ_HOME)
HAWQMASTER_PORT = '{{hawq-site/hawq_master_address_port}}'

RESULT_STATE_OK = 'OK'
RESULT_STATE_WARNING = 'WARNING'
RESULT_STATE_UNKNOWN = 'UNKNOWN'
RESULT_STATE_SKIPPED = 'SKIPPED'

logger = logging.getLogger('ambari_alerts')


def get_tokens():
  """
  Returns a tuple of tokens in the format {{site/property}} that will be used to build the dictionary passed into execute
  """
  return ([HAWQMASTER_PORT])


def execute(configurations={}, parameters={}, host_name=None):
  """
  Returns a tuple containing the result code and a pre-formatted result label

  Keyword arguments:
  configurations (dictionary): a mapping of configuration key to value
  parameters (dictionary): a mapping of script parameter key to value
  host_name (string): the name of this host where the alert is running
  """

  if configurations is None:
    logger.error("[Alert HAWQ] Configurations file is either not accessible or not present.")
    return (RESULT_STATE_UNKNOWN, ['There were no configurations supplied to the script.'])
  logger.debug("Configuration File found")

  if not os.path.isfile(HAWQ_SLAVES_FILE):
    logger.error("[Alert HAWQ] Slaves file is not present in {0}".format(HAWQ_SLAVES_FILE))
    return (RESULT_STATE_SKIPPED, ['Slaves file is not present in /usr/local/hawq/etc'])

  try:
    ambari_segment_list = get_segment_list_ambari()
    db_segment_list = get_segment_list_db(configurations[HAWQMASTER_PORT])
    # Replace any occurence of 'localhost' in segment_list with host_name
    hawq_segment_list = [host_name if name == 'localhost' else name for name in db_segment_list]

    #Converted to set to omit any duplicates inserted into slaves file
    segment_diff = (set(hawq_segment_list) ^ set(ambari_segment_list))
    segment_diff_len = len(segment_diff)
    #segment_diff_len cannot be negative since this diff is calculated two ways. (eg: "A - B" & "B - A")
    if not segment_diff_len :
      return (RESULT_STATE_OK, ['All HAWQ Segments are registered.'])

    msg = '{0} HAWQ Segments are not registered with HAWQ Master.'.format(segment_diff_len) if (segment_diff_len > 1) else '1 HAWQ Segment is not registered with HAWQ Master.'
    logger.error(" [Alert HAWQ] Segments Unregistered: {0} are unregistered/down.".format(list(segment_diff)))
    return (RESULT_STATE_WARNING, [msg + " Try restarting HAWQ service if a segment has been added/removed. Check the log file in /var/log/ambari-agent/ambari-alerts.log for more details on unregistered hosts."])

  except Exception, ex:
    logger.error('[Alert HAWQ]  Could not find HAWQ Segments registration status on {0}'.format(host_name))
    logger.exception(str(ex))

  # Registration status cannot be determined
  return (RESULT_STATE_UNKNOWN, ['HAWQ Segments Registration Status cannot be determined.'])


def get_segment_list_db(port):
  """
  Gets the Segment registrations count  from HAWQMASTER by running a SQL command.
  """
  logger.debug("Fetching segment list from HAWQ Master Database.")
  query = " SELECT hostname FROM gp_segment_configuration where role = 'p' and status = 'u' "
  cmd = "source {0} && psql -p {1} -t -d template1 -c \"{2};\"".format(HAWQ_GREENPLUM_PATH_FILE, port, query)
 
  returncode, command_output = call(cmd, user=HAWQ_USER, timeout=60)
  if returncode:
    raise

  return [segment.strip() for segment in command_output.split('\n')] if command_output else []


def get_segment_list_ambari():
  """
  Gets the Segment count from HAWQMASTER host from /usr/local/hawq/etc/slaves saved from ambari configurations file.
  """
  segment_list = []
  logger.debug("Fetching Slaves from Slaves file in {0}".format(HAWQ_SLAVES_FILE))
  try:
    #regex to read all not empty lines in a file.
    with open(HAWQ_SLAVES_FILE, "r") as slaves_file:
      slaves = slaves_file.read()
    segment_list = re.findall('\S+' , slaves)
    return segment_list
  except Exception as ex:
     logger.error("[Alert HAWQ] Get Segment list from Slaves : Could not read slaves from {0}".format(HAWQ_SLAVES_FILE))
     raise ex
