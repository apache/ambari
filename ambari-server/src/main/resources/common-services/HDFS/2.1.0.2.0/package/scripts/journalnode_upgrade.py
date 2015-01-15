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

import time
import json

from resource_management.core.logger import Logger
from resource_management.core.resources.system import Execute
from resource_management.libraries.functions.format import format
from resource_management.libraries.functions.default import default
from resource_management.core.exceptions import Fail
from utils import get_jmx_data, get_port



def post_upgrade_check():
  """
  Ensure all journal nodes are up and quorum is established
  :return:
  """
  import params
  Logger.info("Ensuring Journalnode quorum is established")

  time.sleep(5)
  hdfs_roll_edits()
  time.sleep(5)

  all_journal_node_hosts = default("/clusterHostInfo/journalnode_hosts", [])

  if len(all_journal_node_hosts) < 3:
    raise Fail("Need at least 3 Journalnodes to maintain a quorum")

  # TODO, test with HTTPS
  policy = default("/configurations/hdfs-site/dfs.http.policy", None)
  if not policy:
    raise Fail("Could not retrieve dfs.http.policy")
  encrypted = policy.upper == "HTTPS_ONLY"

  nn_address = default("/configurations/hdfs-site/dfs.namenode.https-address", None) if encrypted else \
    default("/configurations/hdfs-site/dfs.namenode.http-address", None)

  if not nn_address:
    raise Fail("Could not retrieve dfs.namenode.http(s)-address for policy %s" % str(policy))

  nn_data = get_jmx_data(nn_address, 'org.apache.hadoop.hdfs.server.namenode.FSNamesystem', 'JournalTransactionInfo',
                         encrypted)
  if not nn_data:
    raise Fail("Could not retrieve JournalTransactionInfo from JMX")

  try:
    last_txn_id = int(nn_data['LastAppliedOrWrittenTxId'])
    success = ensure_jns_have_new_txn(all_journal_node_hosts, last_txn_id)

    if not success:
      raise Fail("Could not ensure that all Journal nodes have a new log transaction id")
  except KeyError:
    raise Fail("JournalTransactionInfo does not have key LastAppliedOrWrittenTxId from JMX info")


def hdfs_roll_edits():
  """
  HDFS_CLIENT needs to be a dependency of JOURNALNODE
  Roll the logs so that Namenode will be able to connect to the Journalnode.
  """
  import params

  # TODO, this will be to be doc'ed since existing HDP 2.2 clusters will needs HDFS_CLIENT on all JOURNALNODE hosts
  if params.security_enabled:
    Execute(params.dn_kinit_cmd, user=params.hdfs_user)

  command = 'hdfs dfsadmin -rollEdits'
  Execute(command, user=params.hdfs_user, tries=1)


def ensure_jns_have_new_txn(nodes, last_txn_id):
  """
  :param nodes: List of Journalnodes
  :param last_txn_id: Integer of last transaction id
  :return: Return true on success, false otherwise
  """
  import params

  num_of_jns = len(nodes)
  actual_txn_ids = {}
  jns_updated = 0
  protocol = 'http'

  journal_node_address = default("/configurations/hdfs-site/dfs.journalnode.https-address", None)
  if journal_node_address:
    protocol = "https"
  else:
    journal_node_address = default("/configurations/hdfs-site/dfs.journalnode.http-address", None)

  if not journal_node_address:
    raise Fail("Could not retrieve Journal node address")

  jn_port = get_port(journal_node_address)    # default is 8480, encrypted is 8481
  if not jn_port:
    raise Fail("Could not retrieve Journalnode port")

  time_out_secs = 3 * 60
  step_time_secs = 10
  iterations = int(time_out_secs/step_time_secs)

  Logger.info("Checking if all Journalnodes are updated.")
  for i in range(iterations):
    Logger.info('Try %d out of %d' % (i+1, iterations))
    for node in nodes:
      # if all JNS are updated break
      if jns_updated == num_of_jns:
        Logger.info("All journal nodes are updated")
        return True

      # JN already meets condition, skip it
      if node in actual_txn_ids and actual_txn_ids[node] and actual_txn_ids[node] >= last_txn_id:
        continue

      url = '%s://%s:%s' % (protocol, node, jn_port)
      data = get_jmx_data(url, 'Journal-', 'LastWrittenTxId')
      if data:
        actual_txn_ids[node] = int(data)
        if actual_txn_ids[node] >= last_txn_id:
          Logger.info("Journalnode %s has a higher transaction id: %s" + str(data))
          jns_updated += 1
        else:
          Logger.info("Journalnode %s is still on transaction id: %s" + str(data))

    Logger.info("Sleeping for %d secs" % step_time_secs)
    time.sleep(step_time_secs)

  return jns_updated == num_of_jns