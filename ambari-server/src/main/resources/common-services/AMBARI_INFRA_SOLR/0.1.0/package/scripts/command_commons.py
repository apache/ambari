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
import os
import params

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

solr_protocol = "https" if params.infra_solr_ssl_enabled else "http"
solr_base_url = format("{solr_protocol}://{params.hostname}:{params.infra_solr_port}/solr")

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

def create_solr_api_request_command(request_path):
  solr_url = format("{solr_base_url}/{request_path}")
  api_cmd = format("kinit -kt {keytab} {principal} && curl -k --negotiate -u : '{solr_url}'") if params.security_enabled else format("curl -k '{solr_url}'")
  return api_cmd
