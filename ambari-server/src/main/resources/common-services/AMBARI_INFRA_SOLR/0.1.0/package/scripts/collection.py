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
from resource_management.core.resources.system import Directory, Execute, File
from resource_management.libraries.functions.format import format

def backup_collection(env):
    """
    Backup collections using replication API (as Solr Cloud Backup API is not available in Solr 5)
    """
    import params, command_commons
    env.set_params(command_commons)

    Directory(command_commons.index_location,
              mode=0755,
              cd_access='a',
              owner=params.infra_solr_user,
              group=params.user_group
              )
    collection_available = command_commons.is_collection_available_on_host()
    if not collection_available:
      Logger.info(format("No any '{collection}' replica is used on {params.hostname} host"))
      return

    Logger.info(format("Backup Solr Collection {collection} to {index_location}"))

    solr_request_path = format("{collection}/replication?command=BACKUP&location={index_location}&name={backup_name}&wt=json")
    backup_api_cmd = command_commons.create_solr_api_request_command(solr_request_path)

    Execute(backup_api_cmd, user=params.infra_solr_user, logoutput=True)

    if command_commons.request_async is False:
      Logger.info("Sleep 5 seconds to wait until the backup request is executed.")
      time.sleep(5)
      Logger.info("Check backup status ...")
      solr_status_request_path = format("{collection}/replication?command=details&wt=json")
      status_check_json_output = format("{index_location}/backup_status.json")
      status_check_cmd = command_commons.create_solr_api_request_command(solr_status_request_path, status_check_json_output)
      command_commons.snapshot_status_check(status_check_cmd, status_check_json_output, command_commons.backup_name, True,
        log_output=command_commons.log_output, tries=command_commons.request_tries, time_interval=command_commons.request_time_interval)

def restore_collection(env):
    """
    Restore collections using replication API (as Solr Cloud Backup API is not available in Solr 5)
    """
    import params, command_commons
    env.set_params(command_commons)

    collection_available = command_commons.is_collection_available_on_host()
    if command_commons.check_hosts and not collection_available:
      Logger.info(format("No any '{collection}' replica is used on {params.hostname} host"))
      return

    Logger.info(format("Remove write.lock files from folder '{index_location}'"))
    for write_lock_file in command_commons.get_files_by_pattern(format("{index_location}"), 'write.lock'):
      File(write_lock_file, action="delete")

    Logger.info(format("Restore Solr Collection {collection} from {index_location}"))

    solr_request_path = format("{collection}/replication?command=RESTORE&location={index_location}&name={backup_name}&wt=json")
    restore_api_cmd = command_commons.create_solr_api_request_command(solr_request_path)

    Execute(restore_api_cmd, user=params.infra_solr_user, logoutput=True)

    if command_commons.request_async is False:
      Logger.info("Sleep 5 seconds to wait until the restore request is executed.")
      time.sleep(5)
      Logger.info("Check restore status ...")
      solr_status_request_path = format("{collection}/replication?command=restorestatus&wt=json")
      status_check_json_output = format("{index_location}/restore_status.json")
      status_check_cmd = command_commons.create_solr_api_request_command(solr_status_request_path, status_check_json_output)
      command_commons.snapshot_status_check(status_check_cmd, status_check_json_output, command_commons.backup_name, False,
        log_output=command_commons.log_output, tries=command_commons.request_tries, time_interval=command_commons.request_time_interval)