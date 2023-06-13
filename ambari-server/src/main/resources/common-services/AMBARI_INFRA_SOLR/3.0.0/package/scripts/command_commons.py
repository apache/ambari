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
import socket
import time
import traceback
from resource_management.core.logger import Logger
from resource_management.core.resources.system import Execute, File
from resource_management.core.shell import call
from resource_management.libraries.functions.default import default
from resource_management.libraries.functions.format import format
from resource_management.libraries.resources.hdfs_resource import HdfsResource

import params

index_helper_script = '/usr/lib/ambari-infra-solr-client/solrIndexHelper.sh'

# folder location which contains the snapshot/core folder
index_location = default("/commandParams/solr_index_location", None)

# index version (available index versions: 6.6.2 and 7.7.0, first one is used by default)
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

# used to filer out comma separated cores - can be useful if backup/resotre failed in some point
skip_cores = default("/commandParams/solr_skip_cores", "").split(",")

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
solr_port = format("{params.infra_solr_port}")
solr_base_url = format("{solr_protocol}://{params.hostname}:{params.infra_solr_port}/solr")
solr_datadir = params.infra_solr_datadir

solr_keep_backup=default("/commandParams/solr_keep_backup", False)

solr_num_shards = int(default("/commandParams/solr_shards", "0"))

solr_hdfs_path=default("/commandParams/solr_hdfs_path", None)

solr_backup_host_cores_map = json.loads(default("/commandParams/solr_backup_host_cores_map", "{}"))
solr_backup_core_host_map = json.loads(default("/commandParams/solr_backup_core_host_map", "{}"))
solr_restore_host_cores_map = json.loads(default("/commandParams/solr_restore_host_cores_map", "{}"))
solr_restore_core_host_map = json.loads(default("/commandParams/solr_restore_core_host_map", "{}"))
solr_restore_core_data = json.loads(default("/commandParams/solr_restore_core_data", "{}"))
solr_restore_config_set = default("/commandParams/solr_restore_config_set", None)

keytab = None
principal = None
if params.security_enabled:
  keytab = params.infra_solr_kerberos_keytab
  principal = params.infra_solr_kerberos_principal

if solr_hdfs_path:

  import functools
  from resource_management.libraries.functions import conf_select
  from resource_management.libraries.functions import stack_select
  from resource_management.libraries.functions import get_klist_path
  from resource_management.libraries.functions import get_kinit_path
  from resource_management.libraries.functions.get_not_managed_resources import get_not_managed_resources

  klist_path_local = get_klist_path(default('/configurations/kerberos-env/executable_search_paths', None))
  kinit_path_local = get_kinit_path(default('/configurations/kerberos-env/executable_search_paths', None))

  # hadoop default parameters
  hdfs_user = params.config['configurations']['hadoop-env']['hdfs_user']
  hadoop_bin = stack_select.get_hadoop_dir("sbin")
  hadoop_bin_dir = stack_select.get_hadoop_dir("bin")
  hadoop_conf_dir = conf_select.get_hadoop_conf_dir()
  hadoop_conf_secure_dir = os.path.join(hadoop_conf_dir, "secure")
  hadoop_lib_home = stack_select.get_hadoop_dir("lib")
  hdfs_principal_name = default('/configurations/hadoop-env/hdfs_principal_name', None)
  hdfs_user_keytab = params.config['configurations']['hadoop-env']['hdfs_user_keytab']

  dfs_type = default("/clusterLevelParams/dfs_type", "")

  hdfs_site = params.config['configurations']['hdfs-site']
  default_fs = params.config['configurations']['core-site']['fs.defaultFS']
  #create partial functions with common arguments for every HdfsResource call
  #to create/delete/copyfromlocal hdfs directories/files we need to call params.HdfsResource in code
  HdfsResource = functools.partial(
    HdfsResource,
    user=params.infra_solr_user,
    hdfs_resource_ignore_file = "/var/lib/ambari-agent/data/.hdfs_resource_ignore",
    security_enabled = params.security_enabled,
    keytab = keytab,
    kinit_path_local = kinit_path_local,
    hadoop_bin_dir = hadoop_bin_dir,
    hadoop_conf_dir = hadoop_conf_dir,
    principal_name = principal,
    hdfs_site = hdfs_site,
    default_fs = default_fs,
    immutable_paths = get_not_managed_resources(),
    dfs_type = dfs_type
  )

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

def create_solr_api_request_command(request_path, output=None, override_solr_base_url=None):
  solr_url = format("{solr_base_url}/{request_path}") if override_solr_base_url is None else format("{override_solr_base_url}/{request_path}")
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
          if 'backup' in details:
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
            Logger.info("Backup data is not found yet in details JSON response...")
            time.sleep(time_interval)
            continue

        else:
          if 'restorestatus' in json_data:
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
          else:
            Logger.info("Restore status data is not found yet in details JSON response...")
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

def write_core_file(core, core_data):
  core_json_location = format("{index_location}/{core}.json")
  File(core_json_location, content=json.dumps(core_data))

def create_core_pairs(original_cores, new_cores):
  """
  Create core pairss from the original and new cores (backups -> restored ones), use alphabetic order
  """
  core_pairs_data=[]
  if len(new_cores) < len(original_cores):
    raise Exception("Old collection core size is: " + str(len(new_cores)) +
                    ". You will need at least: " + str(len(original_cores)))
  else:
    for index, core_data in enumerate(original_cores):
      value={}
      value['src_core']=core_data[0]
      value['src_host']=core_data[1]
      value['target_core']=new_cores[index][0]
      value['target_host']=new_cores[index][1]
      core_pairs_data.append(value)
    File(format("{index_location}/restore_core_pairs.json"), content=json.dumps(core_pairs_data))
    return core_pairs_data

def sort_core_host_pairs(host_core_map):
  """
  Sort host core map by key
  """
  core_host_pairs=[]
  for key in sorted(host_core_map):
    core_host_pairs.append((key, host_core_map[key]))
  return core_host_pairs

def is_ip(addr):
  try:
    socket.inet_aton(addr)
    return True
  except socket.error:
    return False

def resolve_ip_to_hostname(ip):
  try:
    host_name = socket.gethostbyaddr(ip)[0].lower()
    Logger.info(format("Resolved {ip} to {host_name}"))
    fqdn_name = socket.getaddrinfo(host_name, 0, 0, 0, 0, socket.AI_CANONNAME)[0][3].lower()
    return host_name if host_name == fqdn_name else fqdn_name
  except socket.error:
    pass
  return ip

def create_command(command):
  """
  Create hdfs command. Append kinit to the command if required.
  """
  kinit_cmd = "{0} -kt {1} {2};".format(kinit_path_local, params.infra_solr_kerberos_keytab, params.infra_solr_kerberos_principal) if params.security_enabled else ""
  return kinit_cmd + command

def execute_commad(command):
  """
  Run hdfs command by infra-solr user
  """
  return call(command, user=params.infra_solr_user, timeout=300)

def move_hdfs_folder(source_dir, target_dir):
  cmd=create_command(format('hdfs dfs -mv {source_dir} {target_dir}'))
  returncode, stdout = execute_commad(cmd)
  if returncode:
    raise Exception("Unable to move HDFS dir '{0}' to '{1}' (return code: {2})".format(source_dir, target_dir, str(returncode)))
  return stdout.strip()

def check_hdfs_folder_exists(hdfs_dir):
  """
  Check that hdfs folder exists or not
  """
  cmd=create_command(format("hdfs dfs -ls {hdfs_dir}"))
  returncode, stdout = execute_commad(cmd)
  if returncode:
    return False
  return True

def check_folder_exists(dir):
  """
  Check that folder exists or not
  """
  returncode, stdout = call(format("test -d {dir}"), user=params.infra_solr_user, timeout=300)
  if returncode:
    return False
  return True

def check_folder_until_size_not_changes(dir):
  """
  Call du -d 0 <folder> | cut -f 1 on specific directory until the size not changes (so copy operation has finished)
  """
  cmd = format("du -d 0 {dir} | cut -f 1")
  size_changed = True
  size_str = "-1"
  while size_changed:
    returncode, stdout = call(cmd, user=params.infra_solr_user, timeout=300)
    if stdout:
      actual_size_str = stdout.strip()
      if actual_size_str == size_str:
        size_changed = False
        continue
      else:
        Logger.info(format("Actual size of '{dir}' is {actual_size_str}, wait 5 sec and check again, to make sure no copy operation is in progress..."))
        time.sleep(5)
        size_str = actual_size_str