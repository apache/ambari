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
from resource_management.core.resources.system import Execute, File
from resource_management.libraries.functions.format import format

def migrate_index(env):
  """
  Migrate lucene index in the background.
  """
  import params, command_commons
  env.set_params(command_commons)

  collection_available = command_commons.is_collection_available_on_host()
  if not collection_available:
    Logger.info(format("No any '{collection}' replica is used on {params.hostname} host"))
    return

  index_migrate_cmd = format("{index_helper_script} upgrade-index -d {index_location} -v {index_version}")

  if command_commons.force is True:
    index_migrate_cmd+=" -f"

  if command_commons.backup_mode is True:
    index_migrate_cmd+=" -b"

  if command_commons.debug is True:
    index_migrate_cmd+=" -g"

  if command_commons.core_filter is not None:
    index_migrate_cmd+=format(" -c {core_filter}")

  if command_commons.delete_lock_on_start:
    Logger.info(format("Remove write.lock files from folder '{index_location}'"))
    for write_lock_file in command_commons.get_files_by_pattern(format("{index_location}"), 'write.lock'):
      File(write_lock_file, action="delete")
  else:
    Logger.info("Skip removing write.lock files")

  Logger.info(format("Migrate index at location: {index_location}"))
  # It can generate a write.lock file
  Execute(index_migrate_cmd, user=params.infra_solr_user, environment={'JAVA_HOME': params.java64_home}, logoutput=command_commons.log_output)

