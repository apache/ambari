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

from resource_management.core.logger import Logger
from resource_management.core.resources.system import Directory, Execute, File
from resource_management.libraries.functions.format import format

def backup_collection(env):
    """
    Backup collections using replication API (as Solr Cloud Backup API is not available in Solr 5)
    """
    import params, command_commons
    env.set_params(command_commons)

    Logger.info(format("Backup Solr Collection {collection} to {index_location}"))

    solr_request_path = format("{collection}/replication?command=BACKUP&location={index_location}&name={backup_name}&wt=json")
    backup_api_cmd = command_commons.create_solr_api_request_command(solr_request_path)

    Directory(command_commons.index_location,
              mode=0755,
              cd_access='a',
              owner=params.infra_solr_user,
              group=params.user_group
              )

    Execute(backup_api_cmd, user=params.infra_solr_user, logoutput=True)

def restore_collection(env):
    """
    Restore collections using replication API (as Solr Cloud Backup API is not available in Solr 5)
    """
    import params, command_commons
    env.set_params(command_commons)

    Logger.info(format("Remove write.lock files from folder '{index_location}'"))
    for write_lock_file in command_commons.get_files_by_pattern(format("{index_location}"), 'write.lock'):
      File(write_lock_file, action="delete")

    Logger.info(format("Restore Solr Collection {collection} from {index_location}"))

    solr_request_path = format("{collection}/replication?command=RESTORE&location={index_location}&name={backup_name}&wt=json")
    restore_api_cmd = command_commons.create_solr_api_request_command(solr_request_path)

    Execute(restore_api_cmd, user=params.infra_solr_user, logoutput=True)

def delete_collection(env):
    """
    Delete specific Solr collection - used on ranger_audits by default
    """
    import params, command_commons
    env.set_params(command_commons)

    Logger.info(format("Delete Solr Collection: {collection}"))

    solr_request_path = format("admin/collections?action=DELETE&name={collection}&wt=json")
    delete_api_cmd = command_commons.create_solr_api_request_command(solr_request_path)

    Execute(delete_api_cmd, user=params.infra_solr_user, logoutput=True)