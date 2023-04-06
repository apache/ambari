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

from resource_management.core.logger import Logger
from resource_management.core.resources.system import Execute
from resource_management.libraries.functions.default import default
from resource_management.core.exceptions import Fail
import utils
from resource_management.libraries.functions.jmx import get_value_from_jmx
import namenode_ha_state
from namenode_ha_state import NAMENODE_STATE, NamenodeHAState
from utils import get_dfsadmin_base_command


def post_upgrade_check():
  """
  Ensure all journal nodes are up and quorum is established during Rolling Upgrade.
  :return:
  """
  import params
  Logger.info("Ensuring Journalnode quorum is established")

  if params.security_enabled:
    # We establish HDFS identity instead of JN Kerberos identity
    # since this is an administrative HDFS call that requires the HDFS administrator user to perform.
    Execute(params.hdfs_kinit_cmd, user=params.hdfs_user)

  time.sleep(5)
  hdfs_roll_edits()
  time.sleep(5)

  all_journal_node_hosts = default("/clusterHostInfo/journalnode_hosts", [])

  if len(all_journal_node_hosts) < 3:
    raise Fail("Need at least 3 Journalnodes to maintain a quorum")

  try:
    namenode_ha = namenode_ha_state.NamenodeHAState()
  except ValueError as err:
    raise Fail("Could not retrieve Namenode HA addresses. Error: " + str(err))

  Logger.info(str(namenode_ha))
  nn_address = namenode_ha.get_address(NAMENODE_STATE.ACTIVE)

  nn_data = utils.get_jmx_data(nn_address, 'org.apache.hadoop.hdfs.server.namenode.FSNamesystem', 'JournalTransactionInfo',
                         namenode_ha.is_encrypted(), params.security_enabled)
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
  Must kinit before calling this command.
  """
  import params

  # TODO, this will need to be doc'ed since existing clusters will need HDFS_CLIENT on all JOURNALNODE hosts
  dfsadmin_base_command = get_dfsadmin_base_command('hdfs')
  command = dfsadmin_base_command + ' -rollEdits'
  Execute(command, user=params.hdfs_user, tries=1)


def ensure_jns_have_new_txn(nodelist, last_txn_id):
  """
  :param nodelist: List of Journalnodes
  :param last_txn_id: Integer of last transaction id
  :return: Return true on success, false otherwise
  """
  import params

  jn_uri = default("/configurations/hdfs-site/dfs.namenode.shared.edits.dir", None)

  if jn_uri is None:
    raise Fail("No JournalNode URI found at hdfs-site/dfs.namenode.shared.edits.dir")

  nodes = []
  for node in nodelist:
    if node in jn_uri:
      nodes.append(node)

  num_of_jns = len(nodes)
  actual_txn_ids = {}
  jns_updated = 0

  if params.journalnode_address is None:
    raise Fail("Could not retrieve JournalNode address")

  if params.journalnode_port is None:
    raise Fail("Could not retrieve JournalNode port")

  time_out_secs = 3 * 60
  step_time_secs = 10
  iterations = int(time_out_secs/step_time_secs)

  protocol = "https" if params.https_only else "http"

  Logger.info("Checking if all JournalNodes are updated.")
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

      url = '%s://%s:%s' % (protocol, node, params.journalnode_port)
      data = utils.get_jmx_data(url, 'Journal-', 'LastWrittenTxId', params.https_only, params.security_enabled)
      if data:
        actual_txn_ids[node] = int(data)
        if actual_txn_ids[node] >= last_txn_id:
          Logger.info("JournalNode %s has a higher transaction id: %s" % (node, str(data)))
          jns_updated += 1
        else:
          Logger.info("JournalNode %s is still on transaction id: %s" % (node, str(data)))

    Logger.info("Sleeping for %d secs" % step_time_secs)
    time.sleep(step_time_secs)

  return jns_updated == num_of_jns