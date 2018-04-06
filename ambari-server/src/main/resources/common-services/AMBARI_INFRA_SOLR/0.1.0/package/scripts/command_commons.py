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
import fnmatch
import json
import os
import params
import time
import traceback

from resource_management.core.exceptions import ExecutionFailed
from resource_management.core.logger import Logger
from resource_management.core.resources.system import Execute
from resource_management.libraries.functions.default import default
from resource_management.libraries.functions.format import format

index_helper_script = '/usr/lib/ambari-infra-solr-client/solrIndexHelper.sh'

# folder location which contains the snapshot/core folder
index_location = default("/commandParams/solr_index_location", None)

# index version (available index versions: 6.6.2 and 7.2.1, second one is used by default)
index_version = default("/commandParams/solr_index_version", '6.6.2')

# if this flag is false, skip upgrade if the version is proper, you can force to re-run the tool with setting the flag to true
force = default("/commandParams/solr_index_upgrade_force", False)

# if this flag is true, then it will generate specific folder for every backup with a hostname suffix
# where "." chars replaced with "_"(e.g.: /my/path/backup_locationc7301_ambari_apache_org), that can be useful if different
# hosts share the same filesystem where the backup is stored.
shared_fs = default("/commandParams/solr_shared_fs", False)

# set verbose log for index migration (default: true)
debug = default("/commandParams/solr_migrate_debug", True)

# used for filtering folders in backup location (like: if the filter is ranger, that will include snapshot.ranger folder but won't include snapshot.hadoop_logs)
core_filter = default("/commandParams/solr_core_filter", None)

# delete write.lock file at the start of lucene index migration process
delete_lock_on_start = default("/commandParams/solr_delete_lock_on_start", True)
# if it used, then core filter will be used with snapshot.* folder pattern
backup_mode = default("/commandParams/solr_migrate_backup", True)

log_output = default("/commandParams/solr_migrate_logoutput", True)
# Solr colleection name (used for DELETE/BACKUP/RESTORE)
collection = default("/commandParams/solr_collection", "ranger_audits")
# it will be used in the snapshot name, if it's ranger, the snapshot folder will be snapshot.ranger
backup_name = default("/commandParams/solr_backup_name", "ranger")

request_async = default("/commandParams/solr_request_async", False)
request_tries = int(default("/commandParams/solr_request_tries", 30))
request_time_interval = int(default("/commandParams/solr_request_time_interval", 5))

check_hosts_default = True if params.security_enabled else False
check_hosts = default("/commandParams/solr_check_hosts", check_hosts_default)

solr_protocol = "https" if params.infra_solr_ssl_enabled else "http"
solr_base_url = format("{solr_protocol}://{params.hostname}:{params.infra_solr_port}/solr")

if params.security_enabled:
  keytab = params.infra_solr_kerberos_keytab
  principal = params.infra_solr_kerberos_principal

hostname_suffix = params.hostname.replace(".", "_")

if shared_fs:
  index_location = format("{index_location}_{hostname_suffix}")


def get_files_by_pattern(directory, pattern):
  for root, dirs, files in os.walk(directory):
    for basename in files:
      try:
        matched = pattern.match(basename)
      except AttributeError:
        matched = fnmatch.fnmatch(basename, pattern)
      if matched:
        yield os.path.join(root, basename)

def create_solr_api_request_command(request_path, output=None):
  solr_url = format("{solr_base_url}/{request_path}")
  grep_cmd = " | grep 'solr_rs_status: 200'"
  api_cmd = format("kinit -kt {keytab} {principal} && curl -w'solr_rs_status: %{{http_code}}' -k --negotiate -u : '{solr_url}'") \
    if params.security_enabled else format("curl -w'solr_rs_status: %{{http_code}}' -k '{solr_url}'")
  if output is not None:
    api_cmd+=format(" -o {output}")
  api_cmd+=grep_cmd
  return api_cmd

def snapshot_status_check(request_cmd, json_output, snapshot_name, backup=True, log_output=True, tries=30, time_interval=5):
  """
  Check BACKUP/RESTORE status until the response status will be successful or failed.

  :param request_cmd: backup or restore api path
  :param json_output: json file which will store the response output
  :param snapshot_name: snapshot name, it will be used to check the proper status in the status response (backup: <snapshot_name>, restore: snapshot.<snapshot_name>)
  :param backup: this flag is true if the check is against backup, otherwise it will be restore
  :param log_output: print the output of the downloaded json file (backup/restore response)
  :param tries: number of tries of the requests - it stops after the response status is successful for backup/restore
  :param time_interval: time to wait in seconds between retries
  """
  failed = True
  num_tries = 0
  for i in range(tries):
    try:
      num_tries+=1
      if (num_tries > 1):
        Logger.info(format("Number of tries: {num_tries} ..."))
      Execute(request_cmd, user=params.infra_solr_user)
      with open(json_output) as json_file:
        json_data = json.load(json_file)
        if backup:
          details = json_data['details']
          backup_list = details['backup']
          if log_output:
            Logger.info(str(backup_list))

          if type(backup_list) == type(list()): # support map and list format as well
            backup_data = dict(backup_list[i:i+2] for i in range(0, len(backup_list), 2))
          else:
            backup_data = backup_list

          if (not 'snapshotName' in backup_data) or backup_data['snapshotName'] != snapshot_name:
            snapshot = backup_data['snapshotName']
            Logger.info(format("Snapshot name: {snapshot}, wait until {snapshot_name} will be available."))
            time.sleep(time_interval)
            continue

          if backup_data['status'] == 'success':
            Logger.info("Backup command status: success.")
            failed = False
          elif backup_data['status'] == 'failed':
            Logger.info("Backup command status: failed.")
          else:
            Logger.info(format("Backup command is in progress... Sleep for {time_interval} seconds."))
            time.sleep(time_interval)
            continue

        else:
          restorestatus_data = json_data['restorestatus']
          if log_output:
            Logger.info(str(restorestatus_data))

          if (not 'snapshotName' in restorestatus_data) or restorestatus_data['snapshotName'] != format("snapshot.{snapshot_name}"):
            snapshot = restorestatus_data['snapshotName']
            Logger.info(format("Snapshot name: {snapshot}, wait until snapshot.{snapshot_name} will be available."))
            time.sleep(time_interval)
            continue

          if restorestatus_data['status'] == 'success':
            Logger.info("Restore command successfully finished.")
            failed = False
          elif restorestatus_data['status'] == 'failed':
            Logger.info("Restore command failed.")
          else:
            Logger.info(format("Restore command is in progress... Sleep for {time_interval} seconds."))
            time.sleep(time_interval)
            continue

    except Exception:
      traceback.print_exc()
      time.sleep(time_interval)
      continue
    break

  if failed:
    raise Exception("Status Command failed.")
  else:
    Logger.info("Status command finished successfully.")

def __get_domain_name(url):
  spltAr = url.split("://")
  i = (0,1)[len(spltAr) > 1]
  dm = spltAr[i].split('/')[0].split(':')[0].lower()
  return dm

def __read_hosts_from_clusterstate_json(json_path):
  hosts = set()
  with open(json_path) as json_file:
    json_data = json.load(json_file)
    znode = json_data['znode']
    data = json.loads(znode['data'])
    collection_data = data[collection]
    shards = collection_data['shards']

    for shard in shards:
      Logger.info(format("Found shard: {shard}"))
      replicas = shards[shard]['replicas']
      for replica in replicas:
        core_data = replicas[replica]
        core = core_data['core']
        base_url = core_data['base_url']
        domain = __get_domain_name(base_url)
        hosts.add(domain)
        Logger.info(format("Found replica: {replica} (core '{core}') in {shard} on {domain}"))
    return hosts

def __get_hosts_for_collection():
  request_path = 'admin/zookeeper?wt=json&detail=true&path=%2Fclusterstate.json&view=graph'
  json_path = format("{index_location}/zk_state.json")
  api_request = create_solr_api_request_command(request_path, output=json_path)
  Execute(api_request, user=params.infra_solr_user)
  return __read_hosts_from_clusterstate_json(json_path)

def is_collection_available_on_host():
  if check_hosts:
    hosts_set = __get_hosts_for_collection()
    return params.hostname in hosts_set
  else:
    return True